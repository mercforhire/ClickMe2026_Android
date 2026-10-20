---
phase: 02-networking-session-sdk-spikes
plan: 13
subsystem: auth
tags: [compose, login, logout, hilt-viewmodel, robolectric, tdd]

requires:
  - phase: 02-networking-session-sdk-spikes
    provides: UserManager login/logout/resolvePostAuthDestination/debugForceTokenRefresh (02-11), ApiException (02-03)
provides:
  - Temporary Material 3 login screen (LoginScreen/LoginContent/LoginViewModel) with iOS-equivalent post-login routing
  - Always-visible Log out button on both shells' Profile tabs
  - Debug-only "Force token refresh" action with status text
  - Top-bar avatar fed from the cached UserProfileData
affects: [02-14, 03]

tech-stack:
  added: []
  patterns: [Hilt wrapper + stateless content split for the login screen, destination StateFlow consumed by LaunchedEffect then cleared]

key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/login/LoginViewModel.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/login/LoginScreen.kt
    - app/src/main/res/values/strings_session.xml
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/login/LoginViewModelTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/login/LoginScreenTest.kt
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/home/HomeClientScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/home/HomeClientViewModel.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/home/HomeExpertScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/home/HomeExpertViewModel.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/HomeShellsTest.kt

key-decisions:
  - "LoginViewModel maps ApiException.Http via userMessage (covers both the 401 message and the first 422 field error); ACCOUNT_SUSPENDED yields no inline error (D-08)"
  - "Log out sits before the showDebugAffordances block in each Profile footer so it ships in release builds; Force token refresh sits inside the gate (T-02-45)"

patterns-established:
  - "ViewModel tests over a real UserManager: FakeClickMeApi + FakeSecureTokenStore + test DataStore, sharing MainDispatcherRule's dispatcher as the IO dispatcher"

requirements-completed: [FND-07, FND-08]

duration: ~25min
completed: 2026-10-20
---

# Phase 2 Plan 13: Login Screen and Log Out Summary

**Temporary Material 3 login screen routed through UserManager.resolvePostAuthDestination, plus Log out on both shells' Profile tabs and a debug-only force-token-refresh check.**

## Accomplishments
- `LoginViewModel` trims the email, rejects blank fields locally (MissingFields, no API call), guards against double submit, and maps Http/Network/Decoding errors to one inline error; suspended accounts show nothing.
- `LoginScreen` (Hilt wrapper) hands the `LaunchDestination` to `onLoggedIn` then consumes it; `LoginContent` is a stateless form with two `ValidatedField`s, an inline error line and an `AsyncSaveButton`.
- Both shells gained `onLogout`, `onDebugForceRefresh` and `debugRefreshStatus`; the Home ViewModels now inject `UserManager`, expose `avatarUrl` from `userManager.profile`, `logout()` and `debugForceRefresh()`.
- Strings live in `strings_session.xml`; `debug_force_token_refresh` is `translatable="false"`.

## Task Commits
1. Task 1 RED: failing login tests and strings - `9517456` (test)
2. Task 1 GREEN: login screen and ViewModel - `5b9e8e5` (feat)
3. Task 2: Log out and debug force-refresh in both shells, with HomeShellsTest updates - `a523dce` (feat)

## Verification
`./gradlew :app:testDevDebugUnitTest --tests '*HomeShellsTest' --tests '*LoginViewModelTest' --tests '*LoginScreenTest' ktlintCheck lintDevDebug assembleDevDebug` exits 0; `scripts/check-design-rules.sh` prints "Design rules: OK". LoginViewModelTest 13 tests, LoginScreenTest 5, HomeShellsTest 13 (9 existing + 4 new), 0 failures. assembleDevDebug proves the Hilt graph resolves. No on-device testing was run (deferred by the user).

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Force-refresh button off-screen in the test viewport**
- **Found during:** Task 2 first test run
- **Issue:** The two debug-refresh tests clicked a button below the fold of the scrollable placeholder column, so the tap never landed.
- **Fix:** Added `performScrollTo()` before the click in the tests (same approach as the existing gallery tests). No production change.
- **Files modified:** HomeShellsTest.kt
- **Commit:** a523dce

Otherwise the plan was executed as written. The RED commit has tests that do not compile until the GREEN commit (expected for new types).

## Known Stubs
None. The Profile tab Log out / debug buttons are the planned placeholder until the real settings screen (D-09); the login screen is the planned temporary screen until Phase 3 (AUTH-01).

## Threat Flags
None beyond the plan's threat model. T-02-44 (password only in memory, masked field), T-02-45 (debug button gated, test asserts absence), T-02-47 (isSubmitting guard, test covers second submit) are implemented.

## Self-Check: PASSED
- Files exist: LoginViewModel.kt, LoginScreen.kt, strings_session.xml, LoginViewModelTest.kt, LoginScreenTest.kt, updated shell and ViewModel files
- Commits 9517456, 5b9e8e5, a523dce exist on main
