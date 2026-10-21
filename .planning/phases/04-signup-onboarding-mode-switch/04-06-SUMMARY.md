---
phase: 04-signup-onboarding-mode-switch
plan: 06
subsystem: session
tags: [session, routing, mode-switch, foreground, coroutines]
requires:
  - phase: 04-01
    provides: SignupRequest/SignupData, ClickMeApi.signup, FakeClickMeApi signup and expert-profile handlers
provides:
  - UserManager.signup, requestModeSwitch, recheckSetupIfPending, completeSetup
  - AppViewModel.switchMode, onSetupPublished, currentMode-following route, foreground setup re-check
affects: [04-07, 04-10, 04-18]
key-files:
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/managers/UserManager.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/AppViewModel.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/managers/UserManagerTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/AppViewModelTest.kt
key-decisions:
  - "Foreground re-check uses the existing MainActivity STARTED hook (onAppForegrounded) rather than ProcessForeground"
  - "Role guard in recheckSetupIfPending: no GET /expert/profile unless roles include expert or current mode is Expert"
  - "Expert is not persisted when setup is incomplete (switch routes to SignupSetup only)"
requirements-completed: [SIGN-01, SIGN-05, ONB-01, ONB-02]
duration: ~35 min
completed: 2026-10-21
---

# Phase 4 Plan 06: Session Brain for Signup, Mode Switch and Setup Re-check Summary

UserManager now signs up, gates the expert switch on setup status, retries failed or skipped setup checks on foreground (experts only), and AppViewModel follows the current mode while on a home route.

## Accomplishments
- `UserManager.signup` stores tokens like login, marks logged in, clears pending setup check.
- `setupCheckPending` flag set on failed lookup, `/me` failure and `offlineDestination` (splash cap); cleared by a successful re-check, the role guard, login, signup, `completeSetup`, logout.
- `requestModeSwitch`: Client is local only; Expert consults `GET /expert/profile`, routes to SignupSetup without persisting Expert when incomplete, failed lookup counts as complete.
- `completeSetup` persists Client and refreshes the profile; `logout` flips `isLoggedIn` before resetting mode and onboarding flag.
- `offlineDestination` loads persisted state first (no carousel flash on cap path).
- `AppViewModel`: second collector on `currentMode` (guarded by home route and `isLoggedIn`), `switchMode` (cancels the in-flight switch), `onSetupPublished`, and `onAppForegrounded` runs `refreshIfStale` then the setup re-check only while on a home route. `debugSwitchScaffold` and both constructors are unchanged.

## Task Commits
1. Task 1 (UserManager + tests, 20 new tests): `23bfdde`
2. Task 2 (AppViewModel + tests, 17 new tests): `93f0eff`

## Deviations from Plan

**[Rule 1 - Bug] Cancel in-flight mode switch when the session ends**
- **Issue:** A switch whose lookup finishes after logout would persist Expert into a logged-out store (route stayed Login but stored mode leaked to the next login).
- **Fix:** The `isLoggedIn` collector cancels `switchJob` when `loggedIn` becomes false.
- **Files:** AppViewModel.kt; covered by `switchCompletingAfterLogoutLeavesRouteOnLogin` (also asserts stored mode is null).
- **Commit:** `93f0eff`

Otherwise the plan was executed as written. Tests were written together with the implementation in each task commit (no separate RED commit).

## Verification
- `:app:testDevDebugUnitTest` (full dev suite) and `ktlintCheck` pass.
- Plan acceptance greps hold (single `switchMode`, `onSetupPublished`, `currentMode.collect`, `recheckSetupIfPending`; constructors unchanged).

## Manual checks deferred (project policy)
- On device: switch to Expert from Client with an incomplete expert profile lands on the setup hub; kill and relaunch after a successful Expert switch lands on ExpertHome; airplane-mode launch followed by reconnect and foreground moves an incomplete expert to SignupSetup.

## Known Stubs
None.

## Threat Flags
None.

## Self-Check: PASSED
