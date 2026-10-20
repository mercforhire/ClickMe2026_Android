---
phase: 02-networking-session-sdk-spikes
plan: 14
subsystem: app-root
tags: [app-route, splash, auto-login, suspension-dialog, lifecycle, tdd]

requires:
  - phase: 02-networking-session-sdk-spikes
    provides: UserManager restore/offlineDestination/refreshIfStale/logout (02-11), LoginScreen and strings_session.xml (02-13)
provides:
  - AppViewModel driven by UserManager (auto-login with 5 s splash cap, logout/suspension routing)
  - Root route switch rendering Login, SignupSetup placeholder, both homes and the suspension dialog
  - Foreground /me retry hook in MainActivity
affects: [02-15, 03, 04]

tech-stack:
  added: []
  patterns: [restore in its own async with withTimeoutOrNull on the await only, isLoggedIn collector mapping session end to Login]

key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/login/SuspendedAccountDialog.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/SignupSetupPlaceholder.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/RootScreensTest.kt
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/AppViewModel.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/ClickMeApp.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/MainActivity.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/config/AppLinks.kt
    - app/src/main/res/values/strings_session.xml
    - app/src/test/java/com/uptrendinvestments/clickmeapp/AppViewModelTest.kt

key-decisions:
  - "The cap wraps only restoreJob.await(); the restore keeps running and its late result is ignored except through the isLoggedIn collector (T-02-48, T-02-50)"
  - "showSuspendedDialog is UserManager.wasSuspended directly, so the single-shot flag has one owner"

requirements-completed: [FND-06, FND-07, FND-08]

duration: ~20min
completed: 2026-10-20
---

# Phase 2 Plan 14: App Root Session Wiring Summary

**AppViewModel now resolves the launch route from UserManager with a 5 s splash cap that never cancels the restore, follows logout/rejection/suspension to Login, and ClickMeApp renders Login, a SignupSetup placeholder with Log out, and a Material suspension dialog.**

## Accomplishments
- `AppViewModel(userManager)`: `isLoggedIn` collector started before resolution; `restore()` runs in `viewModelScope.async`, awaited under `withTimeoutOrNull(SPLASH_HOLD_CAP_MS)`, falling back to `offlineDestination()`. Exposes `showSuspendedDialog`, `onLoggedIn`, `onSuspendedDialogDismissed`, `onAppForegrounded`, `logout`; `debugSwitchScaffold` unchanged.
- `ClickMeApp`: Splash -> empty surface, Login -> `LoginScreen`, SignupSetup -> `SignupSetupPlaceholder`, homes unchanged, suspension dialog after the `when` (mail intent via `runCatching`). No offline banner.
- `MainActivity`: `repeatOnLifecycle(STARTED)` calls `onAppForegrounded()`; splash keep-condition kept, no delay or Handler.
- `AppLinks.SUPPORT_EMAIL`, suspension and placeholder strings added.

## Task Commits
1. Task 1: AppViewModel on UserManager with the 5 s cap (tests and implementation together) - `54b86ff` (feat)
2. Task 2: root routes, suspension dialog, placeholder, foreground hook - `b2e5d70` (feat)

## Verification
- `AppViewModelTest`: 16 tests, 0 failures (cap at 4_999/5_001 ms on virtual time, /me not cancelled, rejection after cap, logout from ClientHome and SignupSetup, suspension mid-session and at launch, onLoggedIn mapping, foreground retry yes/no, debug toggle, route order).
- `RootScreensTest`: 5 tests, 0 failures.
- Full CI-equivalent command (`ktlintCheck lintDevDebug lintProdDebug testDevDebugUnitTest testProdDebugUnitTest assembleDevDebug assembleProdDebug :spikes:stripe:assembleDebug :spikes:agora:assembleDebug`) exits 0; both `check-design-rules.sh` invocations print "Design rules: OK".
- No device testing run (deferred by the user).

## Deviations from Plan
None - plan executed as written. Task 1's RED and GREEN landed in a single commit because the test and ViewModel were written back to back and the tests passed on the first run.

## Known Stubs
`SignupSetupPlaceholder` is the intentional D-05 placeholder until Phase 4 builds the signup flow; the temporary LoginScreen remains until Phase 3 (AUTH-01).

## Threat Flags
None beyond the plan's threat model (T-02-48, T-02-49, T-02-50 mitigations implemented and tested; T-02-51 accepted).

## Self-Check: PASSED
- Files exist: AppViewModel.kt, AppViewModelTest.kt, ClickMeApp.kt, MainActivity.kt, SuspendedAccountDialog.kt, SignupSetupPlaceholder.kt, AppLinks.kt, strings_session.xml, RootScreensTest.kt
- Commits 54b86ff and b2e5d70 exist on main
