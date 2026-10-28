---
phase: 13-push-notifications-internal-testing
plan: 15
subsystem: ui
tags: [compose, reviews, booking-summary]
status: complete

requires:
  - phase: 13-04
    provides: WriteReviewKey and the Write Review route
  - phase: 13-09
    provides: ReviewLaunchRequest (summaryReviewed slot, reviewedBookingIds)
  - phase: 13-13
    provides: review_thanks_snackbar and review_rated_out_of_five strings
provides:
  - "Client Booking Summary Leave a review button (outlined, neutral) that pushes Write Review on the same stack"
  - "Feedback block (Your feedback, RatingStars, comment) from the quiet review-context load"
  - "Post-submit hand-off consumer: quiet reload, button fade-out, Short Thanks for your review snackbar"
affects: [13-18]

requirements-completed: [REV-01]

actuals:
  tokens: 8000
  tasks: 2
  commits: 2
plan_head_before: 80c805b58529d3fae57dbab878165329f0f36810
plan_head_after: df37947a65ff966ede6312d586cd8368b2f073cb

key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/bookingsummary/subviews/BookingSummaryFeedback.kt
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/bookingsummary/BookingSummaryUiState.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/bookingsummary/BookingSummaryViewModel.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/bookingsummary/BookingSummaryScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/home/ClientBookingManagementEntries.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/home/HomeClientScreen.kt
    - app/src/main/res/values/strings_booking_summary.xml
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/bookingsummary/BookingSummaryViewModelTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/bookingsummary/BookingSummaryScreenTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/HomeShellsTest.kt

key-decisions:
  - "A hand-off arriving while the summary is still in a loud load triggers no extra reload (that load is already fresh); only a Loaded summary reloads quietly"
  - "A review-context failure on a quiet reload keeps the previous SummaryReview rather than resetting to Unknown, to avoid flicker"
  - "Submitted.toString prints the rating only, so a comment never reaches logs"
  - "Content takes reviewedIds and derives visibility via showLeaveReview(reviewedIds); the Leave a review exit uses AnimatedVisibility (200 ms fade + shrink)"

duration: ~45min
completed: 2026-10-28
---

# Phase 13 Plan 15: Booking Summary Leave a review and Feedback Summary

**Booking Summary now offers Leave a review for completed bookings (pushing Write Review on the same stack), shows the user's submitted review as a Feedback block, and reloads and thanks the user after a submit.**

## Accomplishments

- Task 1 (tracer): `LeaveReviewButton` (outlined, 56 dp min, between Book Again and Message Expert), `onLeaveReview: (WriteReviewKey) -> Unit` through `clientBookingManagementEntries` and `HomeClientScreen`, `BookingSummaryViewModel.leaveReviewKey()`, the `summaryReviewed` consumer (init and later emissions, booking-id checked), `reviewThanks` plus `consumeReviewThanks()`, and the Short snackbar.
- Task 2: `SummaryReview` (Unknown / NotSubmitted / Submitted) on `Loaded`, quiet `reviews.context` call for completed bookings only, `showLeaveReview(reviewedIds)` and `showFeedback`, `BookingSummaryFeedback` (header with heading semantics, RatingStars 24 dp, "Rated n out of 5", selectable wrapping comment, `SectionAppear`).
- The hand-off arriving from a session/ended or review/nudge push lands on this summary (13-07), so the user is one tap from Write Review.

## Task Commits

1. Task 1 - `b01ba10` feat(13-15): Leave a review on Booking Summary opens Write Review and thanks after submit
2. Task 2 - `df37947` feat(13-15): Booking Summary review context, Feedback block and visibility rules

## Verification

- `:app:testDevDebugUnitTest` for `BookingSummaryViewModelTest`, `BookingSummaryScreenTest`, `HomeShellsTest`: all pass (0 failures, 0 skipped); `ktlintCheck` clean; `scripts/check-design-rules.sh` prints "Design rules: OK"; `:app:assembleDevDebug` exited 0 (Task 1 run).
- New tests: 8 ViewModel (key seed/origin, hand-off reload and thanks, hand-off in init, other-booking id left in slot, context load/prefill, blank comment and missing prefill, silent failure, no call for non-completed, visibility rule), 9 screen (feedback, stars only, button while Unknown, order + click, cancelled/missed, session-reviewed, snackbar, 2.0 font scale button and long comment), 1 HomeShells (`summaryLeaveReviewPushesWriteReview`).
- Acceptance greps: `onLeaveReview` in ClientBookingManagementEntries >= 2, `summaryReviewed` in the ViewModel, `booking_summary_leave_review` once in BookingSummaryFeedback.kt, `sealed interface SummaryReview` once, `BookingSummaryFeedback(` in the screen, no `fontSize` / `Color(0x` in the package.

## Deviations from Plan

None - plan executed as written. Notes:

- The old `phaseThirteenElementsDoNotRender` screen test asserted the absence of "Leave a Review"/"Feedback"; it was replaced by `privateNoteDoesNotRender` plus the new D-18 tests, as the behavior it guarded was deliberately reversed.
- `loadsTheBookingByIdOnceThenShowsLoaded` now expects `SummaryReview.NotSubmitted`, since the test class stubs the review context (`ReviewContextData()`) by default.
- A stale KDoc in `BookingSummaryKey.kt` still says "Phase 13 push routing reuses it"; it is accurate, so it was left alone.

## Known Stubs

None.

## Threat Flags

None. T-13-40 is mitigated by the booking-id check on the Booking Summary slot consumer and the `submittedHandOffReloadsAndThanks` / `aHandOffForAnotherBookingIsLeftInTheSlot` tests.

## Open Items (human)

- Device check: from a session/ended push on a Samsung device, tap Leave a review, submit, and confirm the summary shows the Feedback block, no button and the Thanks snackbar. Not run here (no device in the worktree).
- 13-18 (Wave 5) adds the Past card shortcut and its My Bookings hand-off; both plans edit `HomeClientScreen.kt`, which this plan touched only in the `summary` lambda.

## Self-Check: PASSED

- FOUND: BookingSummaryFeedback.kt, 13-15-SUMMARY.md
- FOUND commits: b01ba10, df37947
