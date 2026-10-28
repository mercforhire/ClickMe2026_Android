---
phase: 02-networking-session-sdk-spikes
verified: 2026-10-28T23:00:00Z
status: gaps_found
score: 3/5 must-haves verified
covered_files:
  - ".planning/phases/02-networking-session-sdk-spikes/02-01-PLAN.md"
  - ".planning/phases/02-networking-session-sdk-spikes/02-01-SUMMARY.md"
  - ".planning/phases/02-networking-session-sdk-spikes/02-02-PLAN.md"
  - ".planning/phases/02-networking-session-sdk-spikes/02-02-SUMMARY.md"
  - ".planning/phases/02-networking-session-sdk-spikes/02-03-PLAN.md"
  - ".planning/phases/02-networking-session-sdk-spikes/02-03-SUMMARY.md"
  - ".planning/phases/02-networking-session-sdk-spikes/02-04-PLAN.md"
  - ".planning/phases/02-networking-session-sdk-spikes/02-04-SUMMARY.md"
  - ".planning/phases/02-networking-session-sdk-spikes/02-05-PLAN.md"
  - ".planning/phases/02-networking-session-sdk-spikes/02-05-SUMMARY.md"
  - ".planning/phases/02-networking-session-sdk-spikes/02-06-PLAN.md"
  - ".planning/phases/02-networking-session-sdk-spikes/02-06-SUMMARY.md"
  - ".planning/phases/02-networking-session-sdk-spikes/02-07-PLAN.md"
  - ".planning/phases/02-networking-session-sdk-spikes/02-07-SUMMARY.md"
  - ".planning/phases/02-networking-session-sdk-spikes/02-08-PLAN.md"
  - ".planning/phases/02-networking-session-sdk-spikes/02-08-SUMMARY.md"
  - ".planning/phases/02-networking-session-sdk-spikes/02-09-PLAN.md"
  - ".planning/phases/02-networking-session-sdk-spikes/02-10-PLAN.md"
  - ".planning/phases/02-networking-session-sdk-spikes/02-10-SUMMARY.md"
  - ".planning/phases/02-networking-session-sdk-spikes/02-11-PLAN.md"
  - ".planning/phases/02-networking-session-sdk-spikes/02-11-SUMMARY.md"
  - ".planning/phases/02-networking-session-sdk-spikes/02-12-PLAN.md"
  - ".planning/phases/02-networking-session-sdk-spikes/02-13-PLAN.md"
  - ".planning/phases/02-networking-session-sdk-spikes/02-13-SUMMARY.md"
  - ".planning/phases/02-networking-session-sdk-spikes/02-14-PLAN.md"
  - ".planning/phases/02-networking-session-sdk-spikes/02-14-SUMMARY.md"
  - ".planning/phases/02-networking-session-sdk-spikes/02-15-PLAN.md"
  - ".planning/phases/02-networking-session-sdk-spikes/02-16-PLAN.md"
  - "app/build.gradle.kts"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/AppViewModel.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/managers/EncryptedPrefsTokenStore.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/managers/FcmPushTokenRegistrar.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/managers/SecureTokenStore.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/managers/SessionEvents.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/managers/SessionStore.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/managers/UserManager.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/model/enums/ErrorCode.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/ApiErrorEnvelope.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/PaginationMeta.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/network/ApiErrorParser.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/network/ApiException.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/network/ApiRequestUrls.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/network/AuthHeaderInterceptor.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/network/InstantAdapter.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/network/MoshiProvider.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/network/NetworkFactory.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/network/SuspensionInterceptor.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/network/TokenAuthenticator.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/network/TokenRefresher.kt"
  - "spikes/stripe/src/main/java/com/uptrendinvestments/clickmeapp/spike/stripe/MainActivity.kt"
  - "spikes/stripe/src/main/java/com/uptrendinvestments/clickmeapp/spike/stripe/StripeSpikeViewModel.kt"
covered_digest: "v3:sha256:c0b169c54a80a274c53c033a6b96491efcc08eb878879e33c003e060aa158c53"
behavior_unverified: 1
overrides_applied: 0
behavior_unverified_items:
  - truth: "Agora audio keeps playing from a microphone-type foreground service while the app is in the background (SC-4 third part, FND-14)"
    test: "On the Samsung SM-A075F (devDebug, production backend) join a confirmed booking with the iOS app as the other side, press Home and leave the app backgrounded for 5+ minutes. Run `adb shell dumpsys activity services com.uptrendinvestments.clickmeapp` during the call. Repeat on the API 37 emulator with `adb shell cmd audio set-enable-hardening enable`."
    expected: "Audio keeps flowing both ways for the whole period. `CallService` shows as a foreground service with the microphone type and the ongoing-call notification stays. Leave ends the call cleanly."
    why_human: "The code is present and wired (CallService declares foregroundServiceType=microphone and calls ServiceCompat.startForeground with FOREGROUND_SERVICE_TYPE_MICROPHONE, and a two-sided call has been shown on device in Phase 11 UAT test 7), but no test or recorded run exercises 5+ minutes of backgrounded audio. Doze, Samsung battery management and Android 17 audio hardening are runtime behavior that grep cannot see."
gaps:
  - truth: "SC-4 (Stripe half, FND-13): a card is saved through PaymentSheet in SetupIntent mode, and a PaymentLauncher 3DS challenge on a connected account completes"
    status: failed
    reason: "The only device run (02-UAT tests 7 and 8, 2026-10-21) failed. PaymentSheet opened and then dismissed immediately, and the 3DS path stopped at 'client secret does not match any associated setup intent on this account'. Plan 02-09 was never completed (no SUMMARY, no `setup: Completed`, `3ds: Completed` or `confirm-retry: 201` evidence). The recorded root cause is configuration, not spike code: the Android dev flavor and the spike use a pk_test_ key (pk_test_51Tha0r...) from a different Stripe account than the backend, and the backend's Stripe account is in live mode (STATE.md 'Phase 2 UAT' note, confirmed 2026-10-23; Phase 7 07-REVIEW WR-08). So the spike code is unproven rather than shown broken, but the success criterion is not met."
    artifacts:
      - path: "spikes/stripe/src/main/java/com/uptrendinvestments/clickmeapp/spike/stripe/StripeSpikeViewModel.kt"
        issue: "Code for SetupIntent creation, PaymentSheet result handling, server-side confirm and the 402 PAYMENT_REQUIRES_ACTION to PaymentLauncher step exists, but it has never produced a passing device run."
      - path: "app/build.gradle.kts"
        issue: "Lines 55-60: dev flavor STRIPE_PUBLISHABLE_KEY is the mismatched pk_test_51Tha0r... key (the spike reads the same account via the spike inputs). Only the prod flavor (pk_live_51U8OTJ...) matches the backend account, and that needs live mode (D-02: no real cards)."
    missing:
      - "A test-mode Stripe path on the backend (or a pk_test_ key for the backend's actual account), then one passing run of the :spikes:stripe app (`setup: Completed`, `3ds: Completed`, `confirm-retry: 201`), OR"
      - "A user decision to accept Phase 7 SC-1 (PaymentSheet setup-mode card save in :app) and SC-4 (PaymentLauncher 3DS) as the stand-in evidence for FND-13 (see 'Gaps Summary'). Both are blocked on the same test-mode backend today."
deferred:
  - truth: "Agora audio keeps playing from the microphone foreground service for 5+ minutes in the background (02-12 spike run)"
    addressed_in: "Phase 9 deferred human list (plan 09-17, D-16)"
    evidence: "STATE.md: '[Phase 2] SUPERSEDED (2026-10-24, Phase 9 plan 09-17, D-16): 02-12 Agora spike device run. The spike `:spikes:agora` was deleted after its CallService findings were absorbed into `:app`; the same device check is now the [Phase 9] DEFERRED (human) list.' 02-VALIDATION.md Manual-Only table repeats this. This is a relocation of a still-open human check, not a pass."
  - truth: "Signed prodRelease AAB builds and is uploaded to the Play internal track, registering the package (REL-03)"
    addressed_in: "Phase 13 release checklist steps 3-5"
    evidence: "STATE.md '[Phase 13] OPEN (human, release)' steps (3) keystore, (4) signed bundleProdRelease + jarsigner, (5) 'Build uploaded to Play Internal testing with Play App Signing (closes REL-03)'. Phase 13 SC-5 (internal-track build) covers the same upload. Still open, so REL-03 stays Pending until the user does it."
human_verification:
  - test: "FND-13 re-run: after a test-mode Stripe path exists, run `:spikes:stripe` on the Samsung (or run Phase 7 SC-1 and SC-4 in :app as the stand-in) and capture `adb logcat -d -s StripeSpike`"
    expected: "`setup: Completed` and a new card in the `methods:` list. A short-lead paid booking with 4000 0027 6000 3184 gives `confirm: 402`, `3ds: Completed`, `confirm-retry: 201`. No 'No such setupintent' error."
    why_human: "Needs a Stripe key that matches the backend account and a test-mode backend (never real cards on the live account, Phase 7 D-02). The user decides between re-running the spike and accepting the Phase 7 SC-1/SC-4 run as the stand-in."
  - test: "FND-14: 5+ minute backgrounded Agora call (the Phase 9 deferred list, also the API 37 emulator hardening check)"
    expected: "See behavior_unverified_items above."
    why_human: "Real native SDK, an iOS peer and process-lifecycle behavior."
  - test: "REL-03 build: create the upload keystore (`~/keys/clickme-upload.jks` plus `keystore.properties`), run `./gradlew bundleProdRelease`, then `jarsigner -verify` on the AAB"
    expected: "Build succeeds and jarsigner prints 'jar verified.'"
    why_human: "The keystore and its passwords are user-owned secrets (02-05 Task 2 was deferred). Nothing in the repo or on disk shows a signed bundle: no keystore.properties, no ~/keys directory, and the only release artifact is `app/build/outputs/apk/prod/release/app-prod-release-unsigned.apk` from 2026-10-20."
  - test: "REL-03 upload: upload the signed AAB to the Play Console internal testing track"
    expected: "Play Console accepts it and shows `com.uptrendinvestments.clickmeapp` with Play App Signing enrolled. (Org verification cleared 2026-10-20, so the 'log the blocker' branch of SC-5 no longer applies; the upload itself is what is missing.)"
    why_human: "External console and a user account. Same as Phase 13 checklist step 5."
  - test: "02-15 R8 cold start: install the signed prodRelease APK, cold-start through the splash, log in, relaunch (auto-login), force-refresh path where possible, log out"
    expected: "No crash. R8 stripped nothing needed by Moshi codegen, Retrofit, EncryptedSharedPreferences or the Authenticator."
    why_human: "Needs the signed build and a device. Unsigned R8 `assembleProdRelease` has built clean (STATE.md 2026-10-20, and the Phase 13 gate), but a cold start was never run."
  - test: "FND-06 on device: a suspended account logs in, or a logged-in test account is suspended on the backend"
    expected: "The app returns to Login and a dialog explains the suspension with a contact-support action. No crash."
    why_human: "02-UAT test 6 was skipped (no backend access to suspend an account). The code path is covered by SuspensionInterceptorTest, NetworkStackTest, UserManagerTest and AppViewModelTest."
  - test: "Regression re-smoke of the Phase 2 session flows on the current build (login, kill and relaunch, forced refresh, airplane-mode launch, logout)"
    expected: "Same results as 02-UAT tests 1-5."
    why_human: "Those five tests passed on 2026-10-21, before Phases 3-13 changed `UserManager`, `AppViewModel` and the token stack. Fold into the Phase 13 device smoke (checklist step 10)."
---

# Phase 2: Networking, Session & SDK Spikes Verification Report

**Phase Goal:** A user can log in against the real backend and stay logged in through token refreshes, and the Stripe and Agora SDKs are proven on a device before the features that depend on them.
**Verified:** 2026-10-28T23:00:00Z
**Status:** gaps_found
**Re-verification:** No, initial verification

## Bottom line

The first half of the goal is achieved. Login, auto-login, the single-flight refresh, the suspension trap, the parsing stack and logout all exist in the current code, are wired into the app, are covered by 325 passing tests, and five of them (login, relaunch, forced refresh, offline launch, logout) passed on a real device in 02-UAT.

The second half, "the Stripe and Agora SDKs are proven on a device", is not achieved:

- **Stripe (FND-13) is an actual failure.** The only device run failed (02-UAT tests 7 and 8), and plan 02-09 never finished. The recorded cause is a Stripe key/account mismatch (see below), not a defect found in the spike code. Still, no run has ever shown a saved card or a completed 3DS challenge.
- **Agora (FND-14) is unproven, not failed.** The `:spikes:agora` module was deleted (Phase 9, 09-17, D-16) and its findings moved into `:app`. A two-sided call has been shown on device, but nothing has shown 5+ minutes of backgrounded audio.
- **Release (REL-03) is not done.** There is no upload keystore, no signed bundle and no Play upload. That needs user-owned secrets, so it is a human item, not a code gap.

## Goal Achievement

### Observable Truths (ROADMAP success criteria)

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | SC-1: A user logs in against the production backend and lands in the tab layout. After a relaunch they are logged in automatically. Logging out returns them to login with the session cleared. | VERIFIED | **Login:** `UserManager.login` calls `api.login`, then `startSession` saves the pair on the IO dispatcher inside `NonCancellable`, sets `isLoggedIn` and refreshes the profile. `ClickMeApp.kt:67` routes `AppRoute.Login -> AuthNavHost` (the throwaway login screen was replaced by the real one in Phase 3, a superset of the SC). **Auto-login:** `restore()` loads cached mode, onboarding and profile from `SessionStore`, returns `Login` when `tokens.hasSession()` is false, otherwise calls `/me`, and keeps the session and routes from cache on IO errors, 5xx and soft 401s (D-01/D-03). `AppViewModel` releases the splash at `SPLASH_HOLD_CAP_MS = 5_000`. **Logout:** `UserManager.logout` runs in `NonCancellable + io`, unregisters push, then `tokens.clear()`, `sessionStore.clear()` (removes profile, mode, onboarding flag and setup deferral in one edit) and resets in-memory state. Both Profile tabs show "Log out" (`HomeShellsTest.clientProfileShowsLogOutInReleaseAndInvokesCallback` and the expert twin). **Device:** 02-UAT tests 1, 2, 4 and 5 passed on the Samsung on 2026-10-21 (login, relaunch auto-login via EncryptedSharedPreferences, offline launch from cache, logout and relaunch to Login). `UserManagerTest.logoutClearsEverythingAndUnregistersPushFirst` asserts every store is empty after logout. |
| 2 | SC-2: When the access token expires, concurrent requests trigger exactly one refresh and then succeed. A failed refresh logs the user out instead of reusing the refresh token. | VERIFIED | `TokenAuthenticator` serializes callers with a `Mutex`, compares the token the failed request sent with the stored one (so a late 401 retries with the current token instead of refreshing again), persists the rotated pair with `tokens.save` before building the retry, retries once (`priorResponse != null` returns null), and calls `rejectSession()` (clear plus `SessionEvent.Rejected`) on a 401 from `/auth/refresh`, a missing refresh token, or a token-coded 401 on the retry. `TokenRefresher` uses a bare client with no authenticator, so it cannot recurse. `UserManager.onSessionEvent` turns `Rejected` into `logout()`. **Behavioral tests (run today, 0 failures):** `TokenAuthenticatorTest.twelve concurrent 401s cause exactly one refresh and all succeed`, `late 401 with a superseded token retries with the current token and does not refresh again`, `rotated pair is in the store before the retried request reaches the server`, `refresh answered 401 clears the store emits Rejected...`, 500/429/IO keep tokens, `retried request that 401s again with INVALID_TOKEN clears the session`, `code-less business 401 after a successful refresh keeps the new pair`. `NetworkStackTest.expiredTokenRefreshesOnceAndReturnsDecodedMe` runs the full OkHttp+Retrofit stack on MockWebServer. **Device:** 02-UAT test 3 (debug "Force token refresh") passed, including that the rotated token persisted across a relaunch. |
| 3 | SC-3: A `403 ACCOUNT_SUSPENDED` response logs the user out with a message. Backend errors surface as parsed `ErrorCode`s and unknown codes do not crash. Microsecond ISO-8601 dates decode correctly. | VERIFIED (device suspension run skipped) | **Suspension:** `SuspensionInterceptor` (application interceptor, after `AuthHeaderInterceptor`) peeks a 403 body, and on `ACCOUNT_SUSPENDED` clears the tokens and emits `SessionEvent.Suspended`; the response is returned untouched. `UserManager.handleAccountSuspended` sets `wasSuspended` once and logs out. `AppViewModel.showSuspendedDialog` mirrors the flag and `ClickMeApp.kt:83` shows `SuspendedAccountDialog`. Tests: `SuspensionInterceptorTest` (5, includes login 403, other-code 403, HTML 403), `UserManagerTest.suspendedEventSetsFlagOnceAndLogsOut`, `restoreReturnsLoginAndFlagsSuspensionWhenAccountSuspended`, `AppViewModelTest` (43). **Errors:** `ApiErrorEnvelope` is one all-optional DTO for the three backend shapes. `ApiErrorParser` never throws (blank, HTML or wrong-shape bodies give null). `ErrorCode.fromWire` returns null for unknown values; the enum has the 65 iOS cases plus one Android-only (`CANNOT_CHAT_WITH_SELF`), 66 in total. `ApiErrorParserTest` (24). **Dates:** `InstantAdapter` uses `OffsetDateTime.parse`; `InstantAdapterTest` covers `2026-10-20T16:17:09.983286+00:00`, nine-digit fractions with an offset, and rejects non-ISO strings. **Device:** 02-UAT test 6 skipped (see human_verification). |
| 4 | SC-4: The throwaway spike apps show (a) a card saved through PaymentSheet in SetupIntent mode, (b) a completed `PaymentLauncher` 3DS challenge on a connected account, and (c) Agora audio that keeps playing from a microphone foreground service in the background. | FAILED (a, b); PRESENT_BEHAVIOR_UNVERIFIED (c) | **(a), (b) Stripe:** `:spikes:stripe` exists and is in `settings.gradle.kts`. `StripeSpikeViewModel` and `MainActivity` implement `presentWithSetupIntent`, a server-side confirm, the 402 `PAYMENT_REQUIRES_ACTION` handling, and `rememberPaymentLauncher(key, stripeAccount)` driven from `LaunchedEffect`. The device run **failed**: 02-UAT test 7 ("the Stripe modal screen shows momentarily and disappears") and test 8 ("client secret does not match any associated setup intent on this account"). Plan 02-09 has no SUMMARY and never logged `setup: Completed`, `3ds: Completed` or `confirm-retry: 201`. Root cause (STATE.md, confirmed 2026-10-23; 07-REVIEW WR-08): the spike and the dev flavor use a `pk_test_51Tha0r...` key from a different Stripe account than the backend, whose Stripe account is in live mode. **(c) Agora:** the spike module was deleted (09-17, D-16). The equivalent is in `:app`: `AndroidManifest.xml:39-41` declares `.services.call.CallService` with `foregroundServiceType="microphone"` plus the `FOREGROUND_SERVICE_MICROPHONE` and `RECORD_AUDIO` permissions, and `CallService.kt:112-116` calls `ServiceCompat.startForeground(..., FOREGROUND_SERVICE_TYPE_MICROPHONE)`. Phase 11 UAT test 7 proved a two-sided call on device; no run covers 5+ minutes in the background. 02-12 has no SUMMARY. |
| 5 | SC-5: A signed release bundle of `com.uptrendinvestments.clickmeapp` builds. If org verification has cleared, it is uploaded to the internal track, which registers the package name; if not, the upload is logged as an open blocker in STATE.md. | UNCERTAIN (human, not achieved) | **In code:** `applicationId = "com.uptrendinvestments.clickmeapp"`, `versionCode = 1`, `versionName = "0.1.0"`; `signingConfigs.release` is created only when `keystore.properties` exists and `buildTypes.release` uses it, with R8 `optimization` enabled. `keystore.properties` is gitignored. **Not done:** there is no `keystore.properties`, no `~/keys` directory, no AAB, and the only release output is an unsigned APK from 2026-10-20. Org verification cleared 2026-10-20 (STATE.md Phase 1), so the "log a blocker" branch does not apply; the upload simply has not happened. STATE.md tracks it as `[Phase 2] DEFERRED (human)` and `[Phase 13] OPEN (human, release)` steps 3-5. 02-15 and 02-16 have no SUMMARY. |

**Score:** 3/5 roadmap success criteria verified (SC-4 failed, SC-5 human-only and not yet done; 1 behavior-dependent truth present but behavior-unverified inside SC-4).

### Deferred Items

Items not yet met but explicitly tracked in later phases. They are relocations of open human checks, not passes.

| # | Item | Addressed In | Evidence |
|---|------|-------------|----------|
| 1 | Backgrounded Agora audio, 5+ minutes (02-12) | Phase 9 deferred human list (09-17, D-16) | STATE.md `[Phase 2] SUPERSEDED`; `:spikes:agora` deleted; 02-VALIDATION.md Manual-Only table |
| 2 | Signed `bundleProdRelease` and Play internal-track upload (REL-03) | Phase 13 release checklist steps 3-5 | STATE.md `[Phase 13] OPEN (human, release)` (3), (4), (5) "closes REL-03" |

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `network/InstantAdapter.kt`, `MoshiProvider.kt` | ISO-8601 (microsecond) Instant decode, shared Moshi, no reflection | VERIFIED | 22 and 24 lines. Adapter registered in `MoshiProvider`; codegen DTOs carry explicit `@Json` names. |
| `model/responses/PaginationMeta.kt`, `ApiErrorEnvelope.kt` | Page meta, three-shape error DTO | VERIFIED | Used by Chat, ClientBookings, ExpertReviews and Notification payloads. |
| `model/enums/ErrorCode.kt` | 65 iOS codes plus lenient `fromWire` | VERIFIED | 66 cases (65 plus `CANNOT_CHAT_WITH_SELF`), `fromWire` returns null for unknown, `isTokenError` helper. |
| `network/ApiErrorParser.kt`, `ApiException.kt`, `ApiCall.kt` | Non-throwing parse, one exception type | VERIFIED | `apiCall` rethrows `CancellationException`, maps everything else to `ApiException`. |
| `network/ApiRequestUrls.kt`, `AuthHeaderInterceptor.kt` | `needAuthToken` allowlist, bearer on authed paths only | VERIFIED | Five public paths (`login`, `signup`, `refresh`, `password/forgot`, `password/reset`), matched on `encodedPath` so a query string cannot change the answer. |
| `network/TokenAuthenticator.kt`, `TokenRefresher.kt` | Single-flight refresh | VERIFIED | See truth 2. |
| `network/SuspensionInterceptor.kt` | 403 ACCOUNT_SUSPENDED trap | VERIFIED | See truth 3. |
| `network/NetworkFactory.kt` | Client/Retrofit builders, interceptor order | VERIFIED | `AuthHeaderInterceptor`, `SuspensionInterceptor`, debug network interceptors, `authenticator`; timeouts 15 s connect, 60 s read/write. Debug logging lives in the debug source set only. |
| `managers/SecureTokenStore.kt`, `EncryptedPrefsTokenStore.kt` | EncryptedSharedPreferences token store | VERIFIED | Lazy, guarded create with reset-and-retry, `commit()` on save and clear, cache in `@Volatile` fields. Cannot run under Robolectric, so device proof is 02-UAT tests 1, 2, 3 and 5. |
| `managers/SessionStore.kt`, `SessionEvents.kt` | DataStore profile, mode, onboarding flag; event bus | VERIFIED | `clear()` is a single `edit { clear() }`. |
| `managers/UserManager.kt` (552 lines) | login, restore, logout, suspension | VERIFIED | Substantive and wired (injected into `AppViewModel`, login/signup/home view models, realtime and push managers). |
| `AppViewModel.kt`, `ClickMeApp.kt` | Routing, 5 s splash cap, suspension dialog | VERIFIED | `AppRoute.Login`, `showSuspendedDialog`, `SPLASH_HOLD_CAP_MS`. |
| `spikes/stripe/*` (MainActivity, StripeSpikeViewModel, StripeSpikeApi) | Stripe spike | STUB-FREE BUT UNPROVEN | 511 lines of real flow code; no passing run. See truth 4. |
| `spikes/agora/*` | Agora spike | DELETED (superseded) | Removed in 09-17 (D-16). `CallService` lives in `:app`. |
| Signed `prodRelease` AAB | REL-03 | MISSING | No keystore, no AAB. |

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| `NetworkFactory.apiClient` | `AuthHeaderInterceptor`, `SuspensionInterceptor`, `TokenAuthenticator` | `addInterceptor` x2, `.authenticator` | WIRED | Order documented and tested in `NetworkStackTest`. |
| `TokenAuthenticator` | `SecureTokenStore.save` then `SessionEvent.Refreshed` / `Rejected` | `tokens.save`, `sessionEvents.emit` | WIRED | Save happens before the retry is built. |
| `SuspensionInterceptor` | `UserManager.handleAccountSuspended` | `SessionEvent.Suspended` collected in `init` with `UNDISPATCHED` start | WIRED | No-replay bus subscribed before any request can emit. |
| `UserManager.isLoggedIn` | `AppViewModel` route | state flow | WIRED | Logout flips to `AppRoute.Login`. |
| `AppViewModel.showSuspendedDialog` | `SuspendedAccountDialog` | `ClickMeApp.kt:83` | WIRED | |
| Profile "Log out" button (both modes) | `UserManager.logout` | `HomeClientViewModel.logout` / `HomeExpertViewModel.logout` (guarded by `callGuard`) | WIRED | |
| Debug "Force token refresh" | `UserManager.debugForceTokenRefresh` | `debugForceRefresh` in both home view models | WIRED | Debug builds only (`BuildConfig.DEBUG`). |
| `EncryptedPrefsTokenStore` | Hilt graph | session `NetworkModule` | WIRED | Same instance used by interceptors, authenticator and `UserManager`. |
| `:spikes:stripe` | Gradle build | `include(":spikes:stripe")` in `settings.gradle.kts` | WIRED | Builds; never produced a passing run. |

### Data-Flow Trace (Level 4)

| Artifact | Data Variable | Source | Produces Real Data | Status |
|----------|---------------|--------|--------------------|--------|
| Login result | `AuthUser`, `TokenPair` | `POST /auth/login` via Retrofit, then `tokens.save` | Yes (02-UAT test 1 on the production backend) | FLOWING |
| Cached home on offline launch | `UserProfileData` | DataStore `cached_profile_json`, written by `refreshProfile` from `GET /user/profile` | Yes (02-UAT test 4) | FLOWING |
| Suspended dialog | `wasSuspended` | `SuspensionInterceptor` -> `SessionEvent.Suspended` -> `UserManager` | Yes in tests, not yet seen on device | FLOWING (test-proven) |
| Stripe spike card list | `cards` | `GET /me/payment-methods` | Not reachable: SetupIntent never confirmed | DISCONNECTED (config mismatch) |

### Behavioral Spot-Checks

`:app:testDevDebugUnitTest` with 12 named classes, `BUILD SUCCESSFUL` (Gradle reported the task UP-TO-DATE, so the results are the XML from the 2026-10-28T22:46Z run; counts summed from the result XML, every file `failures=0 errors=0 skipped=0`):

| Class | Tests | Covers |
|-------|-------|--------|
| ResponseDecodingTest | 62 | snake_case, `{data}` wrapper, PaginationMeta, unknown keys (FND-03) |
| InstantAdapterTest | 9 | microsecond and offset dates (FND-03) |
| ApiErrorParserTest | 24 | three error shapes, unknown code, non-JSON (FND-04) |
| ApiRequestUrlsTest / AuthHeaderInterceptorTest | 21 / 9 | `needAuthToken` table, bearer rules (FND-05) |
| TokenAuthenticatorTest / NetworkStackTest | 13 / 6 | single-flight refresh, retry-once, failure modes (FND-05) |
| SuspensionInterceptorTest | 5 | 403 trap (FND-06) |
| UserManagerTest / SessionStoreTest | 69 / 8 | login, restore matrix, logout, suspension (FND-06/07/08) |
| AppViewModelTest | 43 | routing, 5 s cap, suspension dialog (FND-06/07) |
| HomeShellsTest | 56 | Log out buttons on both Profile tabs (FND-08) |
| **Total** | **325** | |

Not re-run here: `ktlintCheck`, lint, `assembleProdRelease` (R8) and the second flavor. The full gates are reported green by the orchestrator in later phases.

### Probe Execution

Step 7c: SKIPPED. No `probe-*.sh` is declared in any Phase 2 plan or SUMMARY, and none exists under `scripts/`.

### Requirements Coverage

All nine IDs in the phase list appear in plan frontmatter (union of 02-01 to 02-16) and in REQUIREMENTS.md. REQUIREMENTS.md maps exactly FND-03 to FND-08, FND-13, FND-14 and REL-03 to Phase 2, so there are no orphans.

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|-------------|-------------|--------|----------|
| FND-03 | 02-02, 02-07 | snake_case JSON, ISO-8601 with microseconds, `{data}` wrappers, page-based `PaginationMeta` | SATISFIED | `InstantAdapter`, `MoshiProvider`, `PaginationMeta`; `ResponseDecodingTest` (62) and `InstantAdapterTest` (9) pass. |
| FND-04 | 02-03 | Three error shapes parsed into a ~65-code `ErrorCode`; unknown maps to null | SATISFIED | `ApiErrorEnvelope`, `ApiErrorParser`, `ErrorCode` (66 cases, `fromWire` null on unknown); `ApiErrorParserTest` (24). |
| FND-05 | 02-03, 02-06, 02-10, 02-15 | Bearer on authed paths only; single-flight refresh via `Authenticator` + `Mutex`; retry once; failed refresh logs out | SATISFIED (R8 release smoke open) | `ApiRequestUrls.needAuthToken`, `AuthHeaderInterceptor`, `TokenAuthenticator`, `TokenRefresher`; 49 tests across four classes; 02-UAT test 3 on device. The 02-15 R8 cold start was never run. |
| FND-06 | 02-06, 02-10, 02-11, 02-14 | `403 ACCOUNT_SUSPENDED` logs out and shows a message | SATISFIED in code and tests; device check skipped | `SuspensionInterceptor`, `UserManager.handleAccountSuspended`, `SuspendedAccountDialog`; 02-UAT test 6 skipped. |
| FND-07 | 02-01, 02-06, 02-07, 02-11, 02-13, 02-14, 02-15 | Tokens in EncryptedSharedPreferences; profile, `currentMode`, `hasSeenOnboarding` in DataStore; auto-login on launch | SATISFIED | `EncryptedPrefsTokenStore`, `SessionStore`, `UserManager.restore`; 02-UAT tests 1, 2 and 4 on device (the ESP round-trip that unit tests cannot run). |
| FND-08 | 02-11, 02-13, 02-14, 02-15 | Logout clears tokens, cached profile and the onboarding flag | SATISFIED (one warning) | `UserManager.logout`, `SessionStore.clear`; `logoutClearsEverythingAndUnregistersPushFirst`; 02-UAT test 5 on device. See the warning below. |
| FND-13 | 02-01, 02-04, 02-09 | Throwaway spike adds a card via PaymentSheet (SetupIntent) and completes a `PaymentLauncher` 3DS challenge on a connected account | **NOT SATISFIED** | Spike code present; the only device run failed (02-UAT 7, 8); 02-09 has no SUMMARY. Cause: Stripe key/account mismatch against a live-mode backend. |
| FND-14 | 02-01, 02-04, 02-08, 02-12 | Throwaway spike joins an Agora channel from a `microphone` foreground service and audio survives backgrounding | NOT PROVEN (human) | Join code and FGS exist (spike code was absorbed into `:app` `CallService`); a two-sided call works on device (Phase 11 UAT 7); the backgrounded 5+ minute run was never done. 02-12 superseded, not completed. |
| REL-03 | 02-01, 02-05, 02-15, 02-16 | First signed internal build uploaded so the package is registered in Play Console | NOT YET (human) | Conditional signing config in place; no keystore, no AAB, no upload. Tracks as Phase 13 checklist steps 3-5. |

Bookkeeping: REQUIREMENTS.md still shows all nine as `[ ]` / Pending. Tick FND-03 to FND-08 when this phase is accepted; keep FND-13, FND-14 and REL-03 pending.

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| `managers/UserManager.kt` / `managers/FcmPushTokenRegistrar.kt` | UserManager 131-137 (`onSessionEvent`, `logout`), Registrar 71-85 (`unregister`) | On forced logout (`SessionEvent.Rejected` or `Suspended`) the transport has already cleared the tokens, so `logout()` -> `unregister()` sends `DELETE notifications/tokens/{device_id}` with no bearer. `AuthHeaderInterceptor` adds none (the access token is null), the call gets a 401 and `apiCall` swallows it. The `logout()` comment ("Called first so the push-token DELETE still has a bearer token") is only true for a user-initiated logout, and the only test (`tokenPresentAtCall == true`) covers just that path. | Warning (FND-08, not a blocker) | The backend `device_tokens` row survives a forced logout. Mitigated because `tokenSource.deleteToken()` still runs locally and invalidates the FCM token, so pushes to it fail rather than reach the next user. Also noted in the v1.0 milestone audit. Check "row gone after forced logout" in Phase 13 step 10, or send the DELETE with the pre-clear token. |
| `.planning/ROADMAP.md` | Phase 2 plan list | `[x] 02-09-PLAN.md ... (completed 2026-10-23)` contradicts STATE.md (`[Phase 2] DEFERRED (human): 02-09 ... plan not executed`), the missing 02-09-SUMMARY.md, and the failed UAT. The same section's "12/16 plans executed" is consistent with the SUMMARY count, so only the checkbox is wrong. | Warning (bookkeeping) | Misreports FND-13 as done. Untick it. |
| `02-UAT.md` | Gaps section | Both Stripe gaps have `root_cause: ""` and `debug_session: ""` although STATE.md records the confirmed cause (2026-10-23). | Info | Fill in when the gap is closed. |
| `app/build.gradle.kts` | 55-60 | Dev flavor ships the mismatched `pk_test_51Tha0r...` publishable key | Warning (the FND-13 root cause; Phase 7 WR-08) | Every Stripe flow in the dev flavor fails with "No such setupintent". |
| `managers/EncryptedPrefsTokenStore.kt` | class-level | `@Suppress("DEPRECATION")` for deprecated EncryptedSharedPreferences | Info | Locked decision (DEC-jwt-encrypted-sharedprefs); revisit on purpose. |
| `app/build/outputs/apk/prod/release/app-prod-release-unsigned.apk` | n/a | Stale unsigned artifact dated 2026-10-20 | Info | Not evidence of a signed build. |
| TBD/FIXME/XXX/TODO/HACK | n/a | None in `network/`, the session managers, `AppViewModel.kt`, `spikes/stripe/src` or `app/build.gradle.kts` | n/a | Debt-marker gate clear. |

### Human Verification Required

See the `human_verification` and `behavior_unverified_items` blocks in the frontmatter. In priority order:

1. **FND-13 decision and re-run.** This is the one real failure. Either re-run the Stripe spike once a test-mode Stripe path exists, or accept the Phase 7 stand-in (below).
2. **REL-03 keystore, signed AAB and Play upload.** Needs the user (Phase 13 checklist steps 3-5).
3. **FND-14 backgrounded Agora audio.** Phase 9 deferred list, plus the API 37 hardening check.
4. **R8 cold start of the signed build (02-15)** and a regression re-smoke of the Phase 2 session flows, folded into Phase 13 step 10.
5. **FND-06 suspension on device** (02-UAT 6 skipped).

### Gaps Summary

**One gap: FND-13 (SC-4, Stripe half).** The 2026-10-21 device run is an actual failure and is recorded as one. The evidence points at configuration rather than the spike: the key in the dev flavor and the spike belongs to a different Stripe account than the backend, and the backend's account is in live mode (STATE.md, confirmed 2026-10-23; 07-REVIEW WR-08). No run exists that would tell us the spike code works, so the criterion cannot be marked met.

**The Phase 7 stand-in option is the user's call.** Phase 7 SC-1 (add a card through PaymentSheet in setup mode, in `:app`) and SC-4 (a 3DS card through `PaymentLauncher`) test the same SDK paths in the real app. STATE.md already says "the user decides whether 02-09 still needs its own run." Two cautions if the user takes this route:

- Those Phase 7 device checks are **blocked on the same test-mode backend path** (STATE.md "[Phase 7] Stripe key and mode"), so the stand-in does not unblock anything sooner.
- 07-VERIFICATION already flags SC-4 as UNCERTAIN (CR-01: the 402 recovery contract depends on Stripe's server-side PaymentIntent state after an off-session confirm), and the Phase 2 spike run was the evidence meant to settle that. A pass in Phase 7 would settle it just as well.

If the user chooses the stand-in, add an override (not applied here, because it needs the user's name and the Phase 7 results):

```yaml
overrides:
  - must_have: "SC-4 Stripe half: a card saved through PaymentSheet in SetupIntent mode and a completed PaymentLauncher 3DS challenge on a connected account"
    reason: "Proven in :app by Phase 7 SC-1 and SC-4 device runs instead of the throwaway spike (02-09 not run; key/account mismatch)"
    accepted_by: "{name}"
    accepted_at: "{ISO timestamp, after the Phase 7 runs pass}"
```

**Not gaps, but not done:**

- **FND-14.** The literal SC-4 wording ("throwaway spike apps") can no longer be met for Agora, because `:spikes:agora` was deleted on purpose (D-16). The intent (backgrounded microphone-FGS audio on a device) lives on as a Phase 9 deferred human check. An override for the wording is reasonable once that check passes.
- **REL-03 and plans 02-15/02-16.** These wait on user-owned secrets and a console. Code is ready (conditional signing, versionName 0.1.0, R8 on).

**Warning to carry:** the forced-logout push-token DELETE without a bearer (FND-08, table above). It does not defeat FND-08's stated behavior (tokens, profile and onboarding flag are all cleared), so it is not a gap.

---

_Verified: 2026-10-28T23:00:00Z_
_Verifier: Claude (gsd-verifier)_
