---
phase: 11-expert-requests-bookings
plan: 13
subsystem: ui
tags: [reschedule, booking-summary, call-guard, errors, compose, wave-3]

requires:
  - phase: 11-expert-requests-bookings
    provides: "11-01 ExpertBookingsRepository.acceptReschedule/declineReschedule + fixtures; 11-03 RescheduleViewer.Expert, RescheduleResponding, RestrictedAction.RespondToReschedule, expert strings; 11-08 ExpertBookingSummaryViewModel/Screen, reloadQuietly()"
provides:
  - "ApiException.toExpertRescheduleFailure(): the C9 expert error mapping incl. SLOT_UNAVAILABLE"
  - "ExpertRescheduleResponder : RescheduleResponding (no saved-card retry)"
  - "ExpertSummaryDetails.proposal: RescheduleProposalUi? built for pending_reschedule"
  - "ExpertBookingSummaryViewModel.rescheduleState / acceptReschedule() / declineReschedule(reason) / dismissRescheduleDialog() / consumeRescheduleNotice(), both responses behind CallGuard.attemptModifyBooking"
  - "Expert Booking Summary renders the expert RescheduleResponseCard first, with snackbar notices, the Couldn't update booking dialog, and Cancel disabled while a response is in flight"
affects: [11-15]

actuals:
  tokens: 21000
  tasks: 3
  commits: 4
commits: 4
plan_head_before: 12a38f1c33f95e9eb5883046a694c426121ff444
plan_head_after: 3acf43c6d2d3a79bfa098172e8716174df7faa9b

tech-stack:
  added: []
  patterns:
    - "Expert VM reuses the shared RescheduleResponseController over its own RescheduleResponding, so in-flight, reload-before-dialog and notice logic exists once"
    - "Guard first, then controller: callGuard.attemptModifyBooking(bookingId, RespondToReschedule) before any request"
    - "Card fade (200 ms) kept by remembering the last proposal, in an outer Column so a hidden card adds no spacing"

key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/bookingsummary/ExpertRescheduleErrors.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/bookingsummary/ExpertRescheduleResponder.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/bookingsummary/ExpertRescheduleErrorsTest.kt
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/bookingsummary/ExpertBookingSummaryUiState.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/bookingsummary/ExpertBookingSummaryViewModel.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/bookingsummary/ExpertBookingSummaryScreen.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/bookingsummary/ExpertBookingSummaryViewModelTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/bookingsummary/ExpertBookingSummaryScreenTest.kt

key-decisions:
  - "ExpertBookingSummaryViewModel takes ExpertRescheduleResponder as its third constructor argument (after the repository); 11-08 tests were updated to pass it"
  - "Outcome dialog reuses the shared RescheduleOutcomeDialog with a no-op onAddPaymentMethod: the expert responder never returns a payment outcome, so that branch is unreachable"
  - "Accept and decline share one guard attempt each; a blocked guard sends nothing and leaves the controller untouched"

requirements-completed: [EXP-09]

coverage:
  - id: D1
    description: "A client-proposed reschedule shows the expert card first; Accept confirms, runs the call guard, sends {} to accept-reschedule, reloads the booking and shows Reschedule accepted"
    requirement: EXP-09
    verification:
      - kind: unit
        ref: "ExpertBookingSummaryScreenTest#clientProposalAcceptEndToEnd, ExpertBookingSummaryViewModelTest#acceptSendsAnEmptyBodyGuardedAndReloads"
        status: pass
    human_judgment: false
  - id: D2
    description: "An expert-proposed reschedule is read-only with Awaiting client's response"
    requirement: EXP-09
    verification:
      - kind: unit
        ref: "ExpertBookingSummaryScreenTest#expertProposalIsReadOnly, ExpertBookingSummaryViewModelTest#expertProposalMapsToAReadOnlyProposal"
        status: pass
    human_judgment: false
  - id: D3
    description: "Decline sends the trimmed optional reason (blank sends null) and shows Reschedule declined"
    requirement: EXP-09
    verification:
      - kind: unit
        ref: "ExpertBookingSummaryViewModelTest#declineSendsTrimmedReasonAndReloads, #blankDeclineReasonSendsNull; ExpertBookingSummaryScreenTest#declineSendsTheTrimmedReasonAndShowsTheNotice"
        status: pass
    human_judgment: false
  - id: D4
    description: "Responses are guarded by CallGuard.attemptModifyBooking and single-flight; repeat idempotent 200 is success"
    requirement: EXP-09
    verification:
      - kind: unit
        ref: "ExpertBookingSummaryScreenTest#guardBlocksAccept; ExpertBookingSummaryViewModelTest#aLiveCallOnThisBookingBlocksBothResponses, #singleFlight, #idempotentRepeatIsSuccess"
        status: pass
    human_judgment: false
  - id: D5
    description: "SLOT_UNAVAILABLE shows the dedicated message, reloads, and never shows the reschedule as accepted"
    requirement: EXP-09
    verification:
      - kind: unit
        ref: "ExpertBookingSummaryViewModelTest#slotUnavailableShowsDialogAndReloadsWithoutAcceptingTheReschedule; ExpertBookingSummaryScreenTest#slotUnavailableShowsTheDialogAndKeepsTheProposal"
        status: pass
    human_judgment: false
  - id: D6
    description: "Every other backend outcome (403, INVALID_STATUS, VALIDATION_ERROR, CLIENT_UNAVAILABLE, RESCHEDULE_OUTSIDE_AUTH_WINDOW, 404, suspended, network, decoding) maps to the UI-SPEC copy with the right reload or pop"
    requirement: EXP-09
    verification:
      - kind: unit
        ref: "ExpertRescheduleErrorsTest (13 cases); ExpertBookingSummaryViewModelTest#invalidStatusShowsNotPendingAndReloads, #serverTextErrorsReload, #notFoundPopsAfterDismiss"
        status: pass
    human_judgment: false
  - id: D7
    description: "Cancel Booking is disabled while a response is in flight; OK on a pop error leaves the screen"
    requirement: EXP-09
    verification:
      - kind: unit
        ref: "ExpertBookingSummaryScreenTest#cancelDisabledWhileResponding, #errorDialogShowsTitleAndOkOnAPopErrorLeavesTheScreen"
        status: pass
    human_judgment: false
  - id: D8
    description: "Visual fit of the card above the summary, the 200 ms fade and the snackbar on a device"
    human_judgment: true
    rationale: "Layout and motion are not asserted by Robolectric tests; manual device check is deferred per the Phase 11 manual-testing note"

duration: 25min
completed: 2026-10-26
status: complete
---

# Phase 11 Plan 13: Expert Reschedule Response Summary

**Expert accepts or declines a client's reschedule from the Booking Summary through the shared controller and its own responder and error mapper (SLOT_UNAVAILABLE included), behind the call guard, never optimistic, with Cancel locked while a response is in flight.**

## Performance

- **Duration:** about 25 min
- **Tasks:** 3 (one tracer, two expansion)
- **Files:** 3 created, 5 modified

## Accomplishments

- Tracer: a `pending_reschedule` booking proposed by the client shows "Client proposed a new time" above the summary card; Accept opens the expert dialog (no payment text), runs `attemptModifyBooking(bookingId, RespondToReschedule)`, sends `POST expert/bookings/{id}/accept-reschedule` with `{}`, reloads the detail and shows the "Reschedule accepted" snackbar. The tracer verify (ScreenTest and ktlint) was re-run green before expanding.
- `toExpertRescheduleFailure()` implements the C9 mapping: SLOT_UNAVAILABLE gets its own copy and a reload; 403 and INVALID_STATUS reload with fixed copy; VALIDATION_ERROR, CLIENT_UNAVAILABLE and RESCHEDULE_OUTSIDE_AUTH_WINDOW show the server text and reload; 404 leaves the screen; suspended is silent. The client mapper is untouched.
- `ExpertRescheduleResponder` implements `RescheduleResponding` with no saved-card retry; a repeat answered with the idempotent 200 is success.
- The ViewModel adds `acceptReschedule()` and `declineReschedule(reason)` (both guarded), `rescheduleState`, `dismissRescheduleDialog()` and `consumeRescheduleNotice()`. Single-flight and reload-before-dialog come from the shared controller.
- The screen shows the card first (200 ms fade, last proposal remembered), a snackbar for each notice, the "Couldn't update booking" dialog (OK pops on a 404), and `expert_summary_cancel` disabled while submitting.

## Task Commits

1. **Task 1 (tracer): RED** - `3dd16e8` (test); **GREEN** - `1fabc72` (feat)
2. **Task 2: Error mapping tests** - `e26e2c3` (test; the mapper itself landed in Task 1 as the plan directed)
3. **Task 3: Decline, dialogs, idempotency, Cancel lock tests** - `3acf43c` (test; production behavior already present from Task 1)

**Plan metadata:** docs commit following this file.

## Verification

- `./gradlew :app:testDevDebugUnitTest --tests '*ExpertBookingSummaryViewModelTest' --tests '*ExpertBookingSummaryScreenTest' --tests '*ExpertRescheduleErrorsTest' --tests '*RescheduleResponseCardTest' --tests '*RescheduleResponseTest' ktlintCheck` exits 0: ScreenTest 29, ViewModelTest 29, ExpertRescheduleErrorsTest 13, RescheduleResponseCardTest 21, RescheduleResponseTest 12, no failures. `RescheduleErrorsTest` (client mapper) also passes unchanged.
- `bash scripts/check-design-rules.sh` prints `Design rules: OK`.
- Acceptance greps: `RescheduleViewer.Expert` in the Screen = 1; `RestrictedAction.RespondToReschedule` in the ViewModel = 2; `toRescheduleFailure` in the responder = 0; `ErrorCode.SlotUnavailable` in the mapper = 1; `@Test` in ExpertRescheduleErrorsTest = 13; `isSubmitting` in the Screen = 1; `Log.x(` in the responder = 0.

## Deviations from Plan

None - plan executed exactly as written. Process notes that do not change behavior:

- Task 1 carried the full C9 mapper (as its action step instructed), so Task 2 and Task 3 are test-only commits that pin behavior already in place; they passed on first run after import-order fixes.
- The per-plan commit ledger under the shared git dir was not used from the isolated worktree; `plan_head_before` is the worktree base SHA from the prompt and `commits: 4` was measured with `git rev-list --count 12a38f1..HEAD` before the docs commit.

**Total deviations:** 0 auto-fixed.

## Authentication Gates

None.

## Known Stubs

None.

## Threat Flags

None. T-11-40 (guard before the request, tested), T-11-41 (controller single-flight plus idempotent server 200, tested), T-11-42 (card actionable only when the client proposed; 403 maps to a reload) and T-11-43 (no optimistic update; reload before the dialog; SLOT_UNAVAILABLE tests assert the booking still shows the original time and proposal) are mitigated as planned. No new network surface; the decline reason is user text that is never logged.

## Next Phase Readiness

- EXP-09 reschedule half is complete; the Cancel half is in 11-10/11-15. 11-15 (iOS follow-ups) can note that the expert Summary now offers Join and the reschedule response card.
- `ExpertBookingSummaryViewModel`'s constructor gained `ExpertRescheduleResponder`; Hilt supplies it through `@Inject`.

## Self-Check: PASSED

- Created files exist: ExpertRescheduleErrors.kt, ExpertRescheduleResponder.kt, ExpertRescheduleErrorsTest.kt (verified by the passing test run and clean `git status`)
- Commits `3dd16e8`, `1fabc72`, `e26e2c3`, `3acf43c` are ancestors of HEAD; `git rev-list --count 12a38f1..HEAD` = 4 before this summary commit

---
*Phase: 11-expert-requests-bookings*
*Completed: 2026-10-26*
