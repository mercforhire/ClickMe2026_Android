---
phase: 02-networking-session-sdk-spikes
plan: 10
subsystem: networking
tags: [okhttp, retrofit, hilt, logging, integration-test, tdd]

requires:
  - phase: 02-networking-session-sdk-spikes
    provides: Transport classes and SecureTokenStore (02-06), ClickMeApi and SessionStore (02-07)
provides:
  - NetworkFactory (bareClient, apiClient, retrofit) shared by Hilt and tests
  - DebugNetwork debug/release source-set split (BASIC logging, Authorization redacted; release stub empty)
  - Hilt NetworkModule providing SecureTokenStore, bare client, TokenRefresher, API client, Retrofit, ClickMeApi as singletons
  - NetworkStackTest full-stack proof and DebugNetworkTest (testDebug source set)
affects: [02-11, 02-13, 02-14, 02-15]

tech-stack:
  added: []
  patterns: [Factory object shared by Hilt module and tests, debug-only interceptors injected as a list so main never references the logging class, src/debug and src/release same-signature object split]

key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/network/NetworkFactory.kt
    - app/src/debug/java/com/uptrendinvestments/clickmeapp/network/DebugNetwork.kt
    - app/src/release/java/com/uptrendinvestments/clickmeapp/network/DebugNetwork.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/di/NetworkModule.kt
    - app/src/testDebug/java/com/uptrendinvestments/clickmeapp/network/DebugNetworkTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/network/NetworkStackTest.kt
  modified: []

key-decisions:
  - "Debug interceptors are added with addNetworkInterceptor on both clients so every attempt, including the Authenticator retry and the refresh call, is logged once"
  - "The unqualified OkHttpClient binding is the authenticated client; the refresh-only client is @Named(\"bare\")"
  - "NetworkModule never references UserManager (avoids the Hilt cycle: transport depends only on SecureTokenStore and SessionEvents)"

patterns-established:
  - "Full-stack tests drive Retrofit ClickMeApi through NetworkFactory against a path-routing MockWebServer Dispatcher, with a recording SessionEvents collector on backgroundScope"

requirements-completed: [FND-05, FND-06]

duration: n/a
completed: 2026-10-20
---

# Phase 2 Plan 10: Network Stack Wiring Summary

**NetworkFactory builds the bare and authenticated OkHttp clients (15 s connect, 60 s read/write), debug-only BASIC logging with Authorization redacted, and a Hilt NetworkModule exposing an injectable ClickMeApi; the assembled stack is proven end to end against MockWebServer.**

## Accomplishments

- `NetworkFactory`: `bareClient` (no authenticator, no bearer interceptor), `apiClient` (AuthHeaderInterceptor, then SuspensionInterceptor as application interceptors; TokenAuthenticator; debug interceptors as network interceptors), `retrofit` (appends the trailing slash when missing, Moshi converter).
- `DebugNetwork` in `src/debug` returns one `HttpLoggingInterceptor` at `Level.BASIC` logging to tag `ClickMeHttp` with `redactHeader("Authorization")`; `src/release` returns `emptyList()` and compiles without the logging dependency. Main code never references `HttpLoggingInterceptor`.
- `NetworkModule` (Hilt, singleton scope) delegates to the factory with `AppConfig.baseUrl`; `UserManager` is not in the graph.
- `NetworkStackTest` (5 tests) through real Retrofit: expired token on `/me` triggers exactly one refresh and decodes `MeData` (store ends "new"/"r2"); `/auth/login` carries no bearer although the store holds a token; 403 ACCOUNT_SUSPENDED throws `HttpException`, parses to `ErrorCode.AccountSuspended`, clears the store and emits `Suspended` with no refresh; refresh answered 503 surfaces 401 and keeps the original pair with no events; a token-coded 401 after a successful refresh clears the store and emits `Refreshed` then `Rejected`.
- `DebugNetworkTest` (4 tests, testDebug source set): BASIC level, bare client shape, API client interceptor order/authenticator/timeouts, Retrofit base URL normalization.

## Task Commits

1. Task 1 RED: `421f1f9` (test) - DebugNetwork and NetworkFactory shape tests
2. Task 1 GREEN: `dd85b0e` (feat) - NetworkFactory and DebugNetwork debug/release split
3. Task 2 test: `1a62640` (test) - NetworkStackTest
4. Task 2 GREEN: `180ae29` (feat) - Hilt NetworkModule (plus ktlint line-length fixes in the test)

## Verification

- `./gradlew :app:testDevDebugUnitTest --tests '*NetworkStackTest' --tests '*DebugNetworkTest' ktlintCheck assembleDevDebug assembleProdDebug :app:compileProdReleaseKotlin` exits 0: NetworkStackTest 5/5, DebugNetworkTest 4/4, ktlint clean, Hilt/KSP processed the module, release stub compiles.
- Acceptance greps: `Level.BASIC` 1, non-comment `Level.BODY` 0, `redactHeader("Authorization")` 1, `HttpLoggingInterceptor` in main/release (non-comment) 0, `addNetworkInterceptor` 2, `@InstallIn(SingletonComponent::class)` 1, `EncryptedPrefsTokenStore(` 1, `Named("bare")` 3, non-comment `UserManager` in NetworkModule 0.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] ktlint max line length in NetworkStackTest**
- **Found during:** Task 2 verification
- **Fix:** Split two long JSON string literals. No behavior change. Included in the Task 2 GREEN commit.

**2. [Note] Worktree base reset**
The worktree started at an older commit (d1217fb, an ancestor) than the required base 7d4fab9; reset to it per the startup instructions before any work (tree was clean).

**3. [Note] TDD ordering for Task 2**
NetworkStackTest exercises only already-built units via NetworkFactory, so it passes before NetworkModule exists (5/5 on first run). It was committed ahead of the module; the module itself is validated by Hilt/KSP during `assembleDevDebug`/`assembleProdDebug`.

Otherwise the plan was executed as written.

## Issues Encountered

None. The benign Moshi codegen kapt deprecation warning (RESEARCH Pitfall 11) appears and was not chased.

## Known Stubs

None. The release `DebugNetwork` returning an empty list is the intended behavior, not a stub.

## Threat Flags

None. T-02-34 (debug-only BASIC logging, Authorization redacted, release stub empty), T-02-35 (refresh on the bare client with no authenticator) and T-02-36 (suspension trap as an application interceptor; 403 on /me covered by NetworkStackTest) are mitigated as planned.

## Self-Check: PASSED

- All six plan files exist; commits 421f1f9, dd85b0e, 1a62640, 180ae29 exist in history.
