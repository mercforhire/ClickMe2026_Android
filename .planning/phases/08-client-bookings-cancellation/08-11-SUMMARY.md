---
phase: 08-client-bookings-cancellation
plan: 11
subsystem: cancellation
tags: [compose, screen, cancellation, two-step]
requires: [08-03, 08-04, 08-06, 08-08]
provides:
  - ClientCancellationScreen (stateful) and ClientCancellationContent (stateless)
  - CancellationReasonStep, CancellationReason.label(), CANCEL_REASON_ROW_TAG_PREFIX, CANCEL_COMMENT_TAG
  - CancellationConfirmStep with the D-06 refund row, CANCEL_CONFIRM_TAG, CANCEL_KEEP_TAG
affects: [08-15]
tech-stack:
  added: []
  patterns: [two-step AnimatedContent, stacked BackHandlers for in-flight swallow, BoxWithConstraints centre-or-scroll]
key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/cancellation/ClientCancellationScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/cancellation/subviews/CancellationReasonStep.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/cancellation/subviews/CancellationConfirmStep.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/cancellation/ClientCancellationScreenTest.kt
  modified: []
key-decisions:
  - "System Back is tested through the real OnBackPressedDispatcher in Robolectric (outer BackHandler counts leaks), so no deferred device check is needed"
  - "Continue arrow uses Icons.AutoMirrored.Outlined.ArrowForward so it flips in RTL"
requirements-completed: [BOOK-07]
duration: ~35min
completed: 2026-10-23
---

# Phase 8 Plan 11: Cancel Booking screen Summary

Two-step full-screen Cancel Booking UI: a reason step (five radio reasons, optional 450-char comment), a confirm step (booking summary, D-06 refund line, Keep Booking filled / Confirm Cancellation error-outlined), the Back rules, the in-flight lock, the error dialog and the one-shot completion hand-off.

## Tasks

| Task | Name | Commit |
| ---- | ---- | ------ |
| 1 | Reason step and confirm step subviews | 47d110d |
| 2 | Screen scaffold, Back rules, error dialog and completion | 8647a5a |

## Verification

- `:app:testDevDebugUnitTest --tests '*ClientCancellationScreenTest'`: 30 tests, 0 failures (16 for Task 1, 14 for Task 2).
- `ktlintCheck` passes; `bash scripts/check-design-rules.sh` prints "Design rules: OK".
- Acceptance greps: `Role.RadioButton` = 1, `DestructiveOutlinedButton(` = 1 in the confirm step; `BackHandler(enabled = state.isSubmitting)` = 1, `backEnabled = !state.isSubmitting` = 1, `AnimatedContent(` = 1 in the screen.

## Behavior covered

- Reason labels on screen equal `wireLabel` for every entry (D-05).
- Refund line per variant: HoldReleased, Full (with the 3-5 day note, "CA$50.00"), NoRefundWithin24h, PolicyFallback, None (no row).
- Submitting disables both buttons and sets the confirm node `stateDescription` to "Cancelling".
- App-bar and system Back: step Confirm goes to `onBackToReason`, step Reason goes to `onBack` / navigation, in-flight swallows system Back (nothing reaches the outer handler) and disables the arrow.
- Keep Booking calls `onBack`; error dialog shows "Couldn't cancel booking" plus the message and OK dismisses; `isFinished` calls `onFinished` exactly once.

## Deviations from Plan

- **Commit granularity.** Task 1 tests and implementation share one commit (47d110d) rather than a separate RED commit, as in 08-03 and 08-08. Tests were written before the implementation and the first run passed all 16.
- Worktree base was behind the expected base; reset to 2aa7762 per the startup check (worktree was clean).
- Warning badge uses a `Box` and the summary-row helper takes an `AnnotatedString` so the Full-refund amount can be a `titleMedium` span; neither changes the specified behavior.

## Known Stubs

None.

## Threat Flags

None. T-08-37 (Back swallowed and arrow disabled while in flight, tested), T-08-38 (server message rendered as plain text through `AuthErrorDialog`) and T-08-39 (Keep Booking filled, Confirm error-outlined behind two steps) are implemented.

## Self-Check: PASSED

- All four created files exist; commits 47d110d and 8647a5a exist.
