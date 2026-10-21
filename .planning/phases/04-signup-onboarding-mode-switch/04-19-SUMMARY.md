---
phase: 04-signup-onboarding-mode-switch
plan: 19
subsystem: navigation
tags: [navigation3, back-stack, wiring, process-death, hilt]
requires: [04-06, 04-07, 04-09, 04-11, 04-12, 04-13, 04-14, 04-15, 04-16, 04-17, 04-18]
provides:
  - SignupSetupNavHost (hub root, five steps, crop, Review) rendered by AppRoute.SignupSetup
  - SignupSetupNavigator and eight data-less NavKeys
  - SignupSetupHostViewModel (draft hydration at the host)
affects: [04-20, signup flow end to end]
tech-stack:
  added: []
  patterns: [Hilt-free NavDisplay split for stub-entry tests (AuthNavDisplay precedent)]
key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/navigation/SignupSetupKeys.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/navigation/SignupSetupNavigator.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/navigation/SignupSetupNavHost.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/SignupSetupHostViewModel.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/navigation/SignupSetupNavigatorTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/navigation/SignupSetupNavDisplayTest.kt
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/ClickMeApp.kt
    - app/src/main/res/values/strings_session.xml
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/RootScreensTest.kt
  deleted:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/SignupSetupPlaceholder.kt
key-decisions:
  - "Hydration lives in SignupSetupNavHost via LaunchedEffect(Unit), not the hub, so a deep entry restored after process death still fills the draft"
  - "Hub is the stack root with no BackHandler; Navigator.goBack is a no-op there so system Back exits the app"
  - "onPublished is passed straight to appViewModel::onSetupPublished; the Review VM only resets the repository, so completeSetup runs once"
requirements-completed: [SIGN-02, SIGN-04, SIGN-05]
duration: ~25 min
completed: 2026-10-21
---

# Phase 4 Plan 19: Signup Setup Navigation Assembly Summary

Setup back stack (Overview hub root; Personal Details, Timezone, Profile Photo, Verify Email, Tags steps; Photo Crop; Review) is assembled under `AppRoute.SignupSetup`, replacing the Phase 2 placeholder.

## What was built

- **Keys:** eight `@Serializable data object` NavKeys (no fields, T-04-49).
- **SignupSetupNavigator:** `openStep`, `openReview`, `openCrop`, `back`, `returnToHub`, `keyFor`, with a same-key-type double-tap guard. Save from a step pops to where it was opened (hub or Review); Review Edit reuses the same step entries.
- **SignupSetupNavHost / SignupSetupNavDisplay:** host wires all eight entries to the W3 screens with the exact signatures from the plan. The Hilt-free display mirrors `AuthNavDisplay`.
- **Hydration:** `SignupSetupHostViewModel.ensureHydrated()` is called from `LaunchedEffect(Unit)` in the host (D-10, T-04-50).
- **ClickMeApp:** `AppRoute.SignupSetup -> SignupSetupNavHost(onPublished = appViewModel::onSetupPublished)` (D-22 handoff from 04-17 honored, no double `completeSetup`).
- **Handoffs honored:** 04-16 Profile Photo `onOpenCrop` pushes the PhotoCrop entry; Use Photo and Cancel pop back to the photo step. 04-11 hub has no BackHandler, system Back exits.
- **Removed:** `SignupSetupPlaceholder.kt`, the two `signup_setup_placeholder_*` strings, and the placeholder test (plus its now-unused imports and `logoutCount`).

## Tests

- `SignupSetupNavigatorTest`: 8 tests (stack shapes, key mapping, double-tap, Review edit return, crop push/pop, returnToHub, back at root).
- `SignupSetupNavDisplayTest`: 3 tests (hub first / step / back to hub; back at hub is a no-op with one entry; popped step's entry-scoped ViewModel is cleared and Edit-from-Review lands on Review).
- Full verification: `./gradlew testDevDebugUnitTest ktlintCheck lintDevDebug assembleDevDebug` green, `scripts/check-design-rules.sh` prints "Design rules: OK".

## Task commits

1. Task 1: `b0b2c50` feat(04-19): add setup nav keys and SignupSetupNavigator
2. Task 2: `71f34da` feat(04-19): wire signup setup nav host into ClickMeApp

## Deviations from Plan

None - plan executed as written. Implementation and tests for Task 1 were authored together and run green on first compile rather than via a separate RED commit (minor process note; test-first commit gate not enforced, plan type is `execute`). Two ktlint fixes (chained-call newline rule) were made in the new tests before commit.

## Environment note

The worktree started at an older commit (d1217fb) than the required base; it was reset to 6aee543 per the branch-check instructions before any work.

## Manual checks deferred (project policy)

- Device: fresh expert signup lands on the hub; each hub row opens its step; Save returns to hub; Review Edit then Save returns to Review.
- Device: Profile Photo -> pick -> crop -> Use Photo / Cancel returns to the photo step.
- Device: Publish success shows Published! hold then client home with the carousel; missing_fields OK returns to the hub.
- Device: kill the process on a deep step (developer options "Don't keep activities" / `adb shell am kill`), relaunch: app opens the hub with the draft pre-filled from the server.
- Device: system Back on the hub exits the app; predictive back animates step pops.

## Known Stubs

None.

## Threat Flags

None. T-04-49 (data-less keys, grep-verified), T-04-50 (host hydrates on first composition) and T-04-51 (auth stack leaves composition on root route change; the setup stack has no path to Signup Initial) are mitigated as planned.

## Self-Check: PASSED

All created files exist; commits b0b2c50 and 71f34da are in git log; acceptance greps verified (8 keys, 0 data classes, 8 entries, 1 onSetupPublished reference, no placeholder references).
