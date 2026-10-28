---
phase: 13-push-notifications-internal-testing
plan: 16
subsystem: push
tags: [permissions, post-notifications, android-13, compose]
status: complete
requires:
  - phase: 13-06
    provides: PushPreferences (clickme_push DataStore)
  - phase: 13-11
    provides: PushRoutingHost mounted in ClickMeApp
provides:
  - UserManager.sessionStarts (logins and signups only)
  - PushPreferences.notificationPromptAsked / markNotificationPromptAsked
  - PushPermissionGate (needsPrompt, asked-once persistence)
  - notificationPromptStep decision table, PushPermissionViewModel, PushPermissionRequester
affects: [13-12 permission-off row, 13-18 release checklist]
tech-stack:
  added: []
  patterns: [pure decision table plus thin composable, gate with secondary @Inject constructor for testability]
key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/managers/PushPermissionGate.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/push/PushPermissionRequester.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/managers/PushPermissionGateTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/push/PushPermissionRequesterTest.kt
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/managers/UserManager.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/managers/PushPreferences.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/ClickMeApp.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/managers/PushPreferencesTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/managers/UserManagerTest.kt
key-decisions:
  - "PushPermissionGate takes a StateFlow<Int> in its primary constructor and a secondary @Inject constructor taking UserManager, so tests need no UserManager harness"
  - "The requester keeps a rememberSaveable launched guard so a route change while the system dialog is up (setup hub to home) cannot launch a second prompt"
requirements-completed: [PUSH-02]
commits: 2
plan_head_before: 80c805b58529d3fae57dbab878165329f0f36810
plan_head_after: 3633838f26629c667c31684c4bd371939681ae43
actuals:
  tokens: 14000
  tasks: 2
  commits: 2
---

# Phase 13 Plan 16: Notification permission prompt Summary

One-time Android 13+ POST_NOTIFICATIONS prompt shown on the landing screen right after login or signup, persisted as asked outside the session store so it never repeats.

## What was built

- `UserManager.sessionStarts`: incremented at the end of the `NonCancellable` block in `startSession` (login and signup); `restore()` and `logout()` leave it alone.
- `PushPreferences`: `notification_prompt_asked` boolean in the `clickme_push` DataStore, which logout does not clear.
- `PushPermissionGate`: raises `needsPrompt` for every `sessionStarts` increment while the asked flag is unset (handled counter starts at 0, so a login before the gate existed still counts). `onPromptLaunched` persists the flag before the dialog; `onAlreadyGranted` records it; `onNotApplicable` and `onAnswered` just clear.
- `PushPermissionRequester`: pure `notificationPromptStep(sdkInt, granted, needsPrompt, route, callActive)` table (Skip, Wait, RecordGranted, Launch), `PushPermissionViewModel`, and a non-visual composable mounted after `PushRoutingHost` in `ClickMeApp`. No rationale or pre-prompt UI, and no `shouldShowRequestPermissionRationale` use.

## Verification

- `:app:testDevDebugUnitTest` for PushPermissionGateTest (6), PushPermissionRequesterTest (6), UserManagerTest, AppViewModelTest, PushPreferencesTest, MicPermission*: green (run twice with `--rerun-tasks`, stable).
- `ktlintCheck` and `:app:assembleDevDebug`: pass. `scripts/check-design-rules.sh`: "Design rules: OK".
- Acceptance greps: `sessionStarts` x3 in UserManager, `notification_prompt_asked` x1, `PushPermissionRequester(` x1 in ClickMeApp, 0 rationale-query uses.

## Deviations from Plan

**1. [Rule 3 - Blocking/Design] Full decision table landed in Task 1.** The table in `notificationPromptStep` is small and the Task 1 composable already needed the API 33 check and the granted/not-applicable branches to be correct, so the Task 2 logic was written in the first commit. Task 2's commit therefore contains only the extended tests (decision table, call wait, setup hub once, already granted, API 32, prefs independence). Task 1's literal "Launch on ClientHome only" stub was never shipped.

**2. [Rule 1 - Bug prevention] `launched` guard added.** Without it, the route changing from `SignupSetup` to `ClientHome` while the system dialog is still up (needsPrompt stays true until answered) would re-run the effect and launch a second prompt. Covered by `waitsOnLoginThenLaunchesOnTheSetupHubOnce`.

**3. Gate constructor shape.** The plan lists `@Inject constructor(userManager, preferences, scope)`; the class has a primary constructor over `StateFlow<Int>` plus that `@Inject` secondary, so tests inject a plain `MutableStateFlow`. The Hilt-facing constructor matches the plan.

**4. Gate tests use real dispatchers.** DataStore file IO does not progress under `runTest` virtual time with `backgroundScope`, so `PushPermissionGateTest` uses `runBlocking` with `Dispatchers.Default` and awaits conditions with a 5 s timeout.

**5. Plan-head ledger.** The worktree guard blocked writing the `gsd-plan-head-before-13-16` ledger file under the shared `.git` dir; `plan_head_before` is the known worktree base (80c805b) and `commits: 2` was measured with `git rev-list --count 80c805b..HEAD`.

## Open items (human, deferred)

- On a real Android 13+ device (Samsung test device): fresh install, log in, confirm the system prompt appears once on home, deny, log out and in again, confirm no second prompt. Not run here (no device).
- Known edge (Assumptions): if the user denied the mic+notification prompt in Phase 9 first, this gate may ask once more after the next login; a second denial is permanent on Android.

## Known Stubs

None.

## Threat Flags

None. T-13-41 and T-13-42 mitigated and covered by `askedSurvivesLogoutAndANewGate` and `waitsDuringCallThenLaunches`.

## Self-Check: PASSED

Created files exist, commits 0b71295 and 3633838 are on the branch, all verification commands exit 0.
