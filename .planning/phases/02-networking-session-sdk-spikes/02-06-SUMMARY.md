---
phase: 02-networking-session-sdk-spikes
plan: 06
subsystem: networking
tags: [okhttp, authenticator, refresh, interceptors, encrypted-shared-preferences, mockwebserver, tdd]

requires:
  - phase: 02-networking-session-sdk-spikes
    provides: Moshi DTOs and MoshiProvider (02-02); ErrorCode, ApiErrorParser, ApiRequestUrls (02-03)
provides:
  - SecureTokenStore interface, TokenPair, and EncryptedPrefsTokenStore (ESP-backed, guarded init, commit() writes, in-memory cache)
  - SessionEvent / SessionEvents bus (Refreshed, Rejected, Suspended)
  - AuthHeaderInterceptor (bearer on authed paths only)
  - SuspensionInterceptor (global ACCOUNT_SUSPENDED trap)
  - TokenRefresher (bare-client POST auth/refresh) and RefreshResult
  - TokenAuthenticator (single-flight, retry-once, reactive refresh)
  - FakeSecureTokenStore test helper
affects: [02-07, 02-10, 02-11, 02-15, 03]

tech-stack:
  added: []
  patterns: [Mutex single-flight Authenticator with sent-vs-current token comparison, interface plus in-memory fake for Keystore-backed storage, plain-Kotlin transport classes constructed by a factory]

key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/managers/SecureTokenStore.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/managers/EncryptedPrefsTokenStore.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/managers/SessionEvents.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/network/AuthHeaderInterceptor.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/network/SuspensionInterceptor.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/network/TokenRefresher.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/network/TokenAuthenticator.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/testing/FakeSecureTokenStore.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/network/AuthHeaderInterceptorTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/network/SuspensionInterceptorTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/network/TokenAuthenticatorTest.kt
  modified: []

key-decisions:
  - "Refresh is reactive only (D-04): TokenAuthenticator never reads expiresAt"
  - "A token-coded 401 (MISSING/INVALID/EXPIRED_TOKEN) on the retry after a successful refresh clears the session; code-less business 401s do not (resolved Q3)"
  - "rejectSession() always calls clear() but emits Rejected only if a session existed, so repeated 401s never duplicate the event"
  - "Transient refresh failures (IO, 429, 5xx, undecodable 2xx) keep tokens and surface the original 401 (D-03)"

patterns-established:
  - "Transport classes are plain Kotlin taking SecureTokenStore and SessionEvents, tested with MockWebServer and FakeSecureTokenStore, no Hilt"
  - "Emitters clear tokens synchronously before emitting, so token state never depends on event delivery"

requirements-completed: [FND-05, FND-06, FND-07]

duration: n/a
completed: 2026-10-20
---

# Phase 2 Plan 06: Authenticated Transport Summary

**Single-flight Mutex-based OkHttp Authenticator (12 concurrent 401s produce one refresh), bare-client TokenRefresher, ACCOUNT_SUSPENDED trap, bearer interceptor, and an EncryptedSharedPreferences-backed SecureTokenStore with guarded init.**

## Performance

- **Tasks:** 2 of 2 (both TDD, RED then GREEN)
- **Tests:** TokenAuthenticatorTest 13, SuspensionInterceptorTest 5, AuthHeaderInterceptorTest 9; all passing, stable over repeated runs; `ktlintCheck` passes

## Accomplishments

- 12 threads hitting /me with an expired token cause exactly one POST auth/refresh, 12 final 200s, one save and one `Refreshed` event. The server-side dispatcher asserts the rotated token is already in the store when each retry arrives.
- Late 401 (a request sent with a superseded token) is retried with the current token without a second refresh.
- Every failure branch is covered: refresh 401 (clear + `Rejected`), refresh 500 / 429 / connection refused (tokens untouched, no event, later request retries), missing refresh token (no refresh call), retried request 401 with `INVALID_TOKEN` (one refresh, one retry, session cleared), code-less `DELETE /user/account` 401 (new pair kept), login 401 (authenticator returns null), already-empty store (no duplicate `Rejected`).
- `SuspensionInterceptor` peeks the 403 body, clears tokens and emits `Suspended` once, on any path including `/auth/login`, while the caller still receives the 403 and full body. FORBIDDEN, HTML and 401 responses are ignored.
- `AuthHeaderInterceptor` uses `needAuthToken(encodedPath)`: public paths never carry (and have stripped) the bearer, including with a query string; a preset header on authed paths is left alone.
- `EncryptedPrefsTokenStore`: lazy ESP creation, delete-file plus delete-Keystore-alias retry once, in-memory fallback, `commit()` writes (no `apply()`), volatile cache. It is intentionally not exercised under Robolectric (KeyStoreException); device verification is plan 02-15.

## Task Commits

1. Task 1 RED: `71dedd6` (test) - interceptor tests and FakeSecureTokenStore
2. Task 1 GREEN: `c18042e` (feat) - store, events, bearer interceptor, suspension trap
3. Task 2 RED: `d1ea1cf` (test) - TokenAuthenticator MockWebServer tests
4. Task 2 GREEN: `07eb821` (feat) - TokenRefresher and TokenAuthenticator

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] ktlint formatting violations**
- **Found during:** Task 1 and 2 verification
- **Issue:** Multi-argument calls, import order and long lines in new files failed ktlint.
- **Fix:** Ran `ktlintFormat`; no behavior change. Included in the GREEN commits.

**2. [Note] Worktree base reset**
The worktree started at an older commit than the required base `346dfeb`; reset to it per the startup instructions before any work (tree was clean).

**3. [Note] FakeSecureTokenStore extras**
Added `setAccessOnly` and `setRefreshOnly` setup helpers (not in the artifact list) so tests can build the "access token but no refresh token" and "refresh token only" states.

Otherwise the plan was executed as written. Test counts exceed the acceptance minimums (14 >= 9, 13 >= 11).

## Issues Encountered

None. The benign Moshi codegen kapt deprecation warning (RESEARCH Pitfall 11) appears and was not chased.

## Known Stubs

None. `EncryptedPrefsTokenStore` has no JVM test by design; it is checked on the Samsung device in plan 02-15.

## Threat Flags

None. T-02-18 through T-02-23 are mitigated as planned (single-flight plus comparison, bearer allowlist, suspension trap, no over-eager logout, ESP at rest, guarded init) and covered by tests, except T-02-22/T-02-23 whose real-ESP behavior is device-verified in 02-15.

## Next Phase Readiness

Plan 02-10 (`NetworkFactory`) can wire `AuthHeaderInterceptor`, `SuspensionInterceptor` and `TokenAuthenticator(store, TokenRefresher(bareClient, BASE_URL, moshi), events)`. Plan 02-11 (UserManager) subscribes to `SessionEvents` eagerly. Note `SessionEvents` is `@Singleton @Inject` but its Hilt module binding for `SecureTokenStore` -> `EncryptedPrefsTokenStore` is not part of this plan.

## Self-Check: PASSED

- All 11 listed files exist; commits 71dedd6, c18042e, d1ea1cf, 07eb821 exist in history.
