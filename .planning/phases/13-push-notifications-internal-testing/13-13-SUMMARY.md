---
phase: 13-push-notifications-internal-testing
plan: 13
subsystem: reviews
tags: [compose, reviews, ui, accessibility]
requires:
  - phase: 13-04
    provides: WriteReviewKey, WriteReviewRoute stub, strings_write_review.xml
  - phase: 13-09
    provides: WriteReviewViewModel (assisted) and WriteReviewUiState
provides:
  - Write Review screen (WriteReviewScreen, stateless WriteReviewContent) with header, star input, review field, footer, skeleton, load error, read-only state and submit error dialog
  - AuthErrorDialog confirmLabel parameter (default OK, 28 callers unchanged)
  - review_thanks_snackbar string for 13-15 and 13-18
affects: [13-15, 13-18]
tech-stack:
  added: []
  patterns: [stateless content composable over a bound screen, in-flight Back lock via BackHandler]
key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/writereview/WriteReviewScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/writereview/subviews/WriteReviewHeader.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/writereview/subviews/ReviewStarInput.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/writereview/subviews/ReviewTextField.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/writereview/subviews/WriteReviewFooter.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/writereview/WriteReviewScreenTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/login/AuthErrorDialogTest.kt
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/writereview/WriteReviewRoute.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/login/AuthErrorDialog.kt
    - app/src/main/res/values/strings_write_review.xml
key-decisions:
  - "The footer owns the keyboard inset; the scrolling form has no second imePadding (it would double-count the IME inside the Scaffold)"
  - "The in-flight Submit button stays enabled (taps are ignored in the onClick) so it keeps the primary fill under the spinner"
requirements-completed: [REV-01]
status: complete
commits: 3
plan_head_before: 86fbe6fb30a99c7498c4d9e01449e270f329f86c
plan_head_after: 2219dcd
actuals:
  tokens: 16000
  tasks: 3
  commits: 3
duration: ~50min
completed: 2026-10-28
---

# Phase 13 Plan 13: Write Review screen Summary

Write Review renders every UI-SPEC Surface 3 state from 13-09's ViewModel: 96dp avatar header, tertiary five-star radio input, optional 1000-character review field with counter, pinned Submit footer, skeleton, load error with Retry, already-submitted read-only form, in-flight lock and an "Edit review" submit error dialog; a successful submit pops back.

## Tasks

1. Tracer: route binds the assisted ViewModel; header, stars, field, footer; submit pops through `onSubmitted` then `onSubmittedHandled` (commit 683e437).
2. Radio-group star semantics with 150 ms color animation, counter from 800 (tertiary at cap), field semantics label, banner, read-only stars and comment, name and topic fallbacks, 200% font check (commit 4cf7fdd).
3. Skeleton, Failed state with Retry, in-flight lock (spinner with stateDescription Submitting, disabled stars/field/back arrow, swallowed system Back, ignored repeat taps), submit error dialog, `AuthErrorDialog.confirmLabel` (commit 2219dcd).

## Verification

- `WriteReviewScreenTest` 14 tests, `AuthErrorDialogTest` 2, plus `LoginScreenTest` (9) and `AuthComponentsTest` (10, existing AuthErrorDialog callers) green; `WriteReviewViewModelTest` green after Task 1.
- `ktlintCheck` clean; `:app:assembleDevDebug` exits 0; `scripts/check-design-rules.sh` prints "Design rules: OK"; no `fontSize` or `Color(0x` literals in the writereview package.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Dropped the second `imePadding()` on the scrolling form**
- **Found during:** Task 1
- **Issue:** The plan puts `imePadding()` on both the footer and the scroll column. Inside a Scaffold the footer's IME padding already grows the bottom bar and shrinks the content area, so a second `imePadding()` on the column would add the keyboard height twice and leave dead space above the keyboard.
- **Fix:** Only the footer applies `navigationBarsPadding().imePadding()`; the form area shrinks and scrolls with it. Read-only and failed states have no editable field, so they need no IME handling.
- **Files modified:** WriteReviewScreen.kt, WriteReviewFooter.kt
- **Commit:** 683e437

**2. [Rule 1 - Bug] In-flight Submit stays enabled instead of `enabled = false`**
- **Issue:** A disabled `PressScaleButton` switches to disabled colors, which would hide the `onPrimary` spinner contrast the spec requires.
- **Fix:** The button stays enabled; `onClick` ignores taps while `submitting` (the ViewModel also ignores them). Covered by `submittingLocksForm`.
- **Commit:** 2219dcd

Star radio semantics and the `review_star_label` plural were added in Task 1 (needed to click and assert stars in the tracer test) rather than Task 2; Task 2 added the read-only mode, animation and press scale.

## Open items for human/device

- TalkBack and on-device checks of Write Review (IME behavior with the pinned footer, 200% font scale on a real phone, predictive back while submitting) are deferred with the other Phase 13 device checks.

## Known Stubs

None.

## Threat Flags

None. T-13-34 (comment cap, plain-text rendering) and T-13-35 (in-flight lock, `submittingLocksForm`) are mitigated as planned.

## Self-Check: PASSED

Files verified present; commits 683e437, 4cf7fdd, 2219dcd are ancestors of HEAD.
