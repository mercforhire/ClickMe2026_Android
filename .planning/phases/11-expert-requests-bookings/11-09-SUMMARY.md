---
phase: 11-expert-requests-bookings
plan: 09
subsystem: expert-cancellation
tags: [cancellation, refund-preview, viewmodel, compose, hilt, savedstate]

requires:
  - phase: 11-01
    provides: ExpertBookingsRepository.detail/cancel, ExpertBookingDetail, FakeClickMeApi expert handlers and fixtures
  - phase: 11-02
    provides: ExpertCancelKey, ExpertLaunchRequest.bookings, Navigator.showExpertBookings, expertCancelEntries slot
  - phase: 11-03
    provides: clientDisplayName
  - phase: 08-client-booking-management
    provides: CancelStep, CancelErrorDialog, CancelFailure, serverTextOrUnknown, REFUND_FULL_WINDOW, capCancelComment, BookingsLaunch
provides:
  - Expert two-step cancel flow (reason, confirm) with merged "label: comment" reason capped at 500
  - expertRefundPreview pure function with PaymentState, fed Held (paid) or None (free); Captured rows dormant but tested
  - ExpertCancellationViewModel with single-flight guarded confirm, ALREADY_CANCELLED as success, saved state
  - expertCancelEntries registration resetting to the Bookings tab
  - expert_cancel_result_refunded string for the Bookings screen notice (consumed by 11-04)
affects: [11-04, 11-15]

actuals:
  tokens: 21000
  tasks: 3
  commits: 3
plan_head_before: 2d62a572d451277a2d07d2d2715b4a38af23cb66
commits: 3

tech-stack:
  added: []
  patterns:
    - "Sibling expert step composables copied from the client cancel flow so the client flow stays untouched"
    - "Preview as a pure function fed a PaymentState; unknown backend state is encoded as dormant, tested branches"

key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/cancellation/ExpertCancelReason.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/cancellation/ExpertRefundPreview.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/cancellation/ExpertCancelErrors.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/cancellation/ExpertCancellationUiState.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/cancellation/ExpertCancellationViewModel.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/cancellation/ExpertCancellationScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/cancellation/subviews/ExpertCancelReasonStep.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/cancellation/subviews/ExpertCancelConfirmStep.kt
    - app/src/main/res/values/strings_expert_cancel.xml
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/cancellation/ExpertCancellationScreenTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/cancellation/ExpertRefundPreviewTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/cancellation/ExpertCancellationViewModelTest.kt
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/cancellation/ExpertCancelEntries.kt

key-decisions:
  - "Expert reason is nullable in state (nothing preselected, UI-SPEC A10); a saved step of Confirm with no valid saved reason restores to step 1"
  - "continueToConfirm and confirm both require a reason, so the disabled Continue button cannot be bypassed"
  - "The refund preview is computed at the moment the confirm step is reached (and on load when restored to step 2) with the injected Clock"
  - "Paid bookings always map to PaymentState.Held because the expert detail has no payment status and capture only follows session completion (RESEARCH C7, Open Question 1)"

patterns-established:
  - "Preview types for not-yet-reachable backend states stay in the sealed interface with unit and UI tests, labelled dormant in KDoc"

requirements-completed: [EXP-09]

coverage:
  - id: D1
    description: "Expert cancels in two steps: nothing preselected, Continue disabled until a reason, optional comment capped at 450, merged reason capped at 500, Keep Booking and Confirm Cancellation on step 2"
    requirement: "EXP-09"
    verification:
      - kind: unit
        ref: "app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/cancellation/ExpertCancellationScreenTest.kt#cancelEndToEnd"
        status: pass
      - kind: unit
        ref: "ExpertCancellationScreenTest#labelOnlyWhenCommentBlank, #reasonsAreTheFourIosLabels"
        status: pass
      - kind: unit
        ref: "ExpertCancellationViewModelTest#theMergedReasonIsCappedAt500, #continueNeedsAReasonAndTheCommentIsCappedAt450"
        status: pass
    human_judgment: false
  - id: D2
    description: "Refund line: paid shows the client's payment hold will be released, free shows none, never tells the expert the client won't be refunded for a paid hold; captured rows dormant but tested"
    requirement: "EXP-09"
    verification:
      - kind: unit
        ref: "ExpertCancellationScreenTest#paidBookingShowsHoldReleaseLine, #freeBookingShowsNoRefundLine, #dormant*"
        status: pass
      - kind: unit
        ref: "ExpertRefundPreviewTest (10 tests, 24 h boundary strictly greater)"
        status: pass
    human_judgment: false
  - id: D3
    description: "Confirm is single flight and guarded by attemptModifyBooking(CancelBooking); edits and Back are blocked while in flight"
    requirement: "EXP-09"
    verification:
      - kind: unit
        ref: "ExpertCancellationViewModelTest#singleFlight, #guardBlocksTheCall, #editsAreIgnoredWhileSubmitting"
        status: pass
    human_judgment: false
  - id: D4
    description: "Success and 409 ALREADY_CANCELLED post the Past-tab hand-off and finish; 404 and INVALID_STATUS return to the refreshed bookings; other errors stay on step 2; server refund_amount becomes the notice"
    requirement: "EXP-09"
    verification:
      - kind: unit
        ref: "ExpertCancellationViewModelTest#alreadyCancelledIsSuccess, #invalidStatusReturnsToBookings, #notFoundBehavesTheSameWithItsOwnCopy, #otherErrorStaysOnConfirm, #aSuspendedAccountShowsNothing, #aServerReportedRefundBecomesTheNotice"
        status: pass
    human_judgment: false
  - id: D5
    description: "Step, reason and comment survive rotation and process death via SavedStateHandle; unknown values fall back"
    requirement: "EXP-09"
    verification:
      - kind: unit
        ref: "ExpertCancellationViewModelTest#savedStateRestoresStepReasonComment, #anUnknownSavedReasonRestoresToNoSelection"
        status: pass
    human_judgment: false
  - id: D6
    description: "Visual fidelity of the two steps against the UI-SPEC (spacing, avatar, warning badge) and navigation reset landing on Bookings with a Past tab snackbar"
    requirement: "EXP-09"
    verification: []
    human_judgment: true
    rationale: "Layout and the cross-tab navigation outcome are not asserted pixel for pixel; the Bookings-side notice is wired by plan 11-04. Manual testing is deferred per project policy."

duration: 75min
completed: 2026-10-26
status: complete
---

# Phase 11 Plan 09: Expert Cancellation Summary

**Two-step expert cancel flow (four iOS reasons, nothing preselected) that sends "label: comment" capped at 500, previews the client's payment-hold release through a D-16 pure function, treats ALREADY_CANCELLED as success and hands the server result to the Bookings Past tab.**

## Performance

- **Duration:** about 75 min (mostly Gradle builds in a shared parallel environment)
- **Tasks:** 3 (one tracer, two expansion)
- **Files:** 13 (12 created, 1 modified)

## Accomplishments

- Tracer: reason step, confirm step, guarded single-flight POST cancel, Past-tab hand-off and `showExpertBookings` registration proven end to end by `ExpertCancellationScreenTest` over a real ViewModel and `FakeClickMeApi`. Tracer verify re-run green before expanding.
- Refund preview: `expertRefundPreview(payment, amountMajor, currency, start, now)` encodes the D-16 table; paid bookings are fed `Held` ("The client's payment hold will be released."), free bookings `None` (no row). Captured rows (full refund strictly over 24 h, no refund within, policy fallback) are dormant and covered by 10 unit tests plus UI tests of the confirm step.
- Errors: `toExpertCancelFailure` (404 / INVALID_STATUS return to bookings, others stay, suspended is silent), `isAlreadyCancelled` as success, `dismissError` posts an Upcoming launch for gone bookings.
- Saved state: `expert_cancel_step`, `expert_cancel_reason`, `expert_cancel_comment`; the confirm step needs a reason, so a lost reason restores to step 1.

## Task Commits

1. **Task 1: Expert cancels a booking (tracer)** - `ea3c3a0` (feat)
2. **Task 2: Refund preview per D-16 as resolved** - `9ef57f7` (feat)
3. **Task 3: Cancel errors, ALREADY_CANCELLED as success, saved state** - `c303d4b` (feat)

## Verification

- Task 1: `*ExpertCancellationScreenTest` + ktlintCheck passed.
- Task 2: `*ExpertRefundPreviewTest` (10), `*ExpertCancellationScreenTest`, `*RefundPreviewTest` (client, 6) + ktlintCheck passed.
- Task 3 / plan level: `*ExpertCancellationViewModelTest` (18), `*ExpertCancellationScreenTest` (11), `*ExpertRefundPreviewTest` (10), `*ClientCancellationViewModelTest` (24, unchanged) + ktlintCheck passed; `:app:assembleDevDebug` passed (Hilt graph builds); `bash scripts/check-design-rules.sh` prints "Design rules: OK".
- Acceptance greps: `AlreadyCancelled` in ExpertCancelErrors = 1; `attemptModifyBooking(bookingId, RestrictedAction.CancelBooking)` = 1; `Log.x(` in the ViewModel = 0; client `CancellationReason.kt` has 0 mentions of `ExpertCancelReason`; four wire labels = 4; `PaymentState.Held` = 2; `REFUND_FULL_WINDOW` = 2 (reused); "payment hold will be released" in strings = 1; `@Test` counts 10 / 18.

## Deviations from Plan

None in behavior. Process notes:

- Task 1 acceptance criterion `grep -c 'showExpertBookings' ExpertCancelEntries.kt` is stated as "prints 1", but the file has three matching lines (import, KDoc mention, call). The intent (the reference exists, baseline 0) is met.
- Tasks are marked `tdd="true"` but each introduces new symbols, so a RED run would be a compile failure (INVALID_RED). Tests and implementation were committed together per task, and each task's tests were run green before commit (same approach as 11-01 and 11-02). The plan's task structure (no separate test/feat commits) was followed.
- In Task 1 the preview types were limited to `HoldReleased` / `None` and `PaymentState { None, Held }`, and Task 2 added the dormant Captured rows, so no stub or placeholder branch was committed.
- The per-plan commit ledger file in the git dir could not be written by the worktree guard, so `plan_head_before` is the known spawn base `2d62a57`; `git rev-list --count 2d62a57..HEAD` was 3 before this SUMMARY commit.

## Authentication Gates

None.

## Known Stubs

None. `expert_cancel_result_refunded` is defined here for the Bookings screen notice but is rendered by plan 11-04 (which owns the Bookings Past-tab snackbar); until then the notice text comes from whatever that plan wires.

## Notes for later plans

- 11-04 (Bookings tab) must consume `ExpertLaunchRequest.bookings` (one consumer) and show `BookingsNotice.Cancelled` as "Booking cancelled" and `CancelledWithRefund(amount)` with `expert_cancel_result_refunded`.
- Backend follow-up (for 11-15): the expert booking detail exposes no payment status, so the Captured preview rows stay dormant until it does. The "a late expert cancel penalizes the client" note was dropped per Open Question Resolution 1.

## Threat Flags

None. T-11-27 (single flight, Back and Keep disabled), T-11-28 (`attemptModifyBooking` before the request), T-11-29 (paid preview is the hold-release line, notice from server `refund_amount`, prohibition covered by `paidBookingShowsHoldReleaseLine`) and T-11-30 (no logging, grep gate) are all implemented and tested.

## Self-Check: PASSED

All 12 created files and the modified `ExpertCancelEntries.kt` exist on disk (committed; `git status` clean apart from the gitignored `local.properties`). Commits `ea3c3a0`, `9ef57f7` and `c303d4b` are ancestors of HEAD.
