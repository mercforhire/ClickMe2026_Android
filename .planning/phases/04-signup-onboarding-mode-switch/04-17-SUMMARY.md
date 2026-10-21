---
phase: 04-signup-onboarding-mode-switch
plan: 17
subsystem: signup
tags: [compose, review, publish, validation]
requires:
  - phase: 04-07
    provides: SignupSetupRepository (draft, publish, reset), SetupStep, SignupDraft, SignupTestHarness
  - phase: 04-03
    provides: AuthTopAppBar, AuthErrorDialog, Avatar
provides:
  - SignupReviewViewModel (rows, missing list, publish, 422 mapping, reset after publish)
  - SignupReviewScreen and stateless SignupReviewContent
  - strings_signup_review.xml
affects: [04-19]
key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/review/SignupReviewViewModel.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/review/SignupReviewScreen.kt
    - app/src/main/res/values/strings_signup_review.xml
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/signup/review/SignupReviewViewModelTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/signup/review/SignupReviewScreenTest.kt
key-decisions:
  - "The ViewModel only resets the repository and flips published; mode Client and completeSetup are done by AppViewModel.onSetupPublished (plan 04-19 wiring), which already calls userManager.completeSetup and routes to ClientHome"
  - "UI state follows the draft until publish succeeds, then freezes so the post-hold reset does not blank the screen"
requirements-completed: [SIGN-04]
completed: 2026-10-21
---

# Phase 4 Plan 17: Review and Publish Summary

Review and Publish screen (port of `SignupReviewView`) with Required markers, a D-03-gated Publish button, a 600 ms Published! hold followed by a repository reset, and 422 `missing_fields` mapped to hub step names.

## Accomplishments
- `SignupReviewViewModel`: maps the draft to `SignupReviewUiState` (null means Required; location omits an empty province; languages and skills in tap order), gates Publish on `draft.canPublish` (province and verified email never block), publishes once (second tap while Saving is ignored), holds `Motion.SAVED_CONFIRMATION_HOLD_MS`, then calls `repository.reset()` and sets `published`. A 422 with `missing_fields` yields `missingFieldsDialog` in hub order with duplicates collapsed; other failures yield `dialogError`. `dismissMissingFields()` sets `returnToHub`.
- `SignupReviewScreen`/`SignupReviewContent`: Profile Review top bar, four cards (Personal Details, Timezone, Profile Photo, Expertise) each with an "Edit {section}" icon button, `Required` with an error icon for missing values, pinned bar with the "Still needed: ..." line (`LiveRegionMode.Polite`) and `AsyncSaveButton` (Publish Profile / Published!), missing-fields and generic error dialogs titled "Couldn't publish profile". Back is swallowed while a publish is in flight.

## Task Commits
1. Task 1: ViewModel and tests - `c51d7ab`
2. Task 2: Screen, strings and tests - `a3a4689`

## Verification
- `SignupReviewViewModelTest` 11 tests and `SignupReviewScreenTest` 7 tests pass.
- Test covering the 04-07 note: `publishSendsOnceHoldsThenResetsTheRepositoryAndSignalsPublished` asserts the draft is still populated during the hold, blank after it, `published` true, and the screen state still showing the published values.
- `ktlintCheck` and `lintDevDebug` pass (ktlintFormat applied before the commits); `scripts/check-design-rules.sh` prints "Design rules: OK".

## Deviations from Plan
- Tests and implementation for each task were committed together (no separate RED commit).
- The orchestrator note mentioned setting mode Client / completing setup from the Review ViewModel via the 04-06 UserManager API. The plan's constructor is `SignupReviewViewModel(repository)` and `AppViewModel.onSetupPublished()` (04-06) already calls `userManager.completeSetup()` and routes to ClientHome, so the ViewModel stays repository-only and the handoff happens through `onPublished` in plan 04-19. Doing it in both places would double-call completeSetup.
- Compose rule: `joinToString` lambdas cannot call `stringResource`, so the lists are mapped first (compile fix only).

## Known Stubs
None.

## Threat Flags
None. T-04-46 mitigated (`repository.reset()` after the hold); T-04-45 accepted as planned (server remains the gate).

## Manual checks (deferred by project policy)
- On device: complete all hub steps, open Review, confirm values and Edit buttons return to the right step, Publish shows Published! then lands on client home in client mode.
- On device: force a 422 (remove the photo server-side) and confirm the dialog names the steps and OK returns to the hub with them incomplete.
- TalkBack: the Still needed line announces when the missing list changes.
