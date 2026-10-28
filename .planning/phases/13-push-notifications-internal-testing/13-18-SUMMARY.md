---
phase: 13-push-notifications-internal-testing
plan: 18
subsystem: reviews
tags: [compose, reviews, my-bookings, past-card]
requires:
  - phase: 13-04
    provides: WriteReviewKey, ReviewOpener
  - phase: 13-09
    provides: ReviewLaunchRequest (per-opener slots, session reviewed-ids set)
  - phase: 13-13
    provides: review_thanks_snackbar string
  - phase: 13-15
    provides: HomeClientScreen summary lambda (left unchanged)
provides:
  - Leave a review pill on completed Past booking cards (D-18)
  - My Bookings consumer of the bookingsReviewed hand-off with quiet reload and thank-you snackbar (D-19)
affects: [13-17]
tech-stack:
  added: []
  patterns: [per-opener hand-off slot consumed in init and on each emission, session reviewed-id set hides a list affordance]
key-files:
  created: []
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/subviews/PastBookingCard.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/MyBookingsScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/MyBookingsViewModel.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/MyBookingsUiState.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/home/HomeClientScreen.kt
    - app/src/main/res/values/strings_my_bookings.xml
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/MyBookingsViewModelTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/MyBookingsScreenTest.kt
key-decisions:
  - "The card always exposes Leave a review as a custom accessibility action when the pill is present, in addition to the pill itself, so TalkBack reaches it either way"
  - "A hand-off posted before the ViewModel exists is consumed in init: it sets the thanks flag without a second load, since the first load is already fresh"
requirements-completed: [REV-01]
status: complete
commits: 2
plan_head_before: d86e7871401b73080533dbf3045bb328952c5339
plan_head_after: c48591c
actuals:
  tokens: 11000
  tasks: 2
  commits: 2
duration: ~25min
completed: 2026-10-28
---

# Phase 13 Plan 18: Past card Leave a review Summary

Completed Past booking cards now carry an outlined "Leave a review" pill that opens Write Review (opener MyBookings), and a submit from there returns to a quietly reloaded My Bookings with a Short "Thanks for your review" snackbar, with the pill hidden for ids reviewed this session.

## Tasks

1. Tracer: pill, hand-off consumer, thanks snackbar, `bookingsRoot` navigation (commit ae78587, tests in c48591c). The three test classes and `:app:assembleDevDebug` pass.
2. Reviewed-set visibility, TalkBack, large font: `reviewedBookingIds` is exposed by the ViewModel and collected by the screen; the card exposes a custom accessibility action; `largeFontPillWraps` passes at font scale 2.

## Verification

- `MyBookingsViewModelTest`, `MyBookingsScreenTest`, `*BookingSummary*`, `HomeShellsTest`: 241 tests in the run, 0 failures. `ktlintCheck` clean, `:app:assembleDevDebug` OK, `scripts/check-design-rules.sh` prints "Design rules: OK".
- New tests: 5 in `MyBookingsScreenTest` (`completedPastCardShowsPill`, `reviewThanksShowsSnackbar`, `noPillForCancelledMissedOrReviewed`, `pillAndCardAreSeparateActions`, `largeFontPillWraps`), 4 in `MyBookingsViewModelTest` (`reviewHandOffReloadsQuietlyAndThanks`, `aReviewHandOffPostedBeforeTheViewModelExistsThanksWithoutASecondLoad`, `summaryHandOffIsLeftAlone`, `reviewedIdsMirrorLaunchRequest`).
- T-13-45: no non-comment reference to `summaryReviewed` in `MyBookingsViewModel`; `summaryHandOffIsLeftAlone` asserts the slot stays pending, no extra load, no thanks flag.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Existing test forbade the "Leave a Review" text on Past**
- **Found during:** Task 1
- **Issue:** `content_neverOffersExcludedActions` listed "Leave a Review" as forbidden on cards; the pill is now intended (it is case-different, so the test would still have passed, but the entry was stale).
- **Fix:** removed that entry from the forbidden list.
- **Files modified:** MyBookingsScreenTest.kt
- **Commit:** c48591c

**2. Task split note.** The `reviewedBookingIds` exclusion and the accessibility action were written together with Task 1's sources, so the single source commit (ae78587) holds both tasks' production code; the tests for both tasks are in c48591c.

**3. Accessibility action is unconditional when the pill exists.** The plan made the custom action a fallback if Compose merged the pill into the card. `pillAndCardAreSeparateActions` asserts the pill and card are both clickable and that the custom action invokes `onLeaveReview` without opening the card. The action is kept as a belt-and-braces TalkBack path.

## Auth gates

None.

## Open items for human/device

- TalkBack walk-through of a Past card (card = "View summary", pill = "Leave a review", custom action list) on a device.
- End-to-end on a device: Past tab, tap pill, submit review, confirm return to My Bookings with the snackbar and no pill on that card.

## Known Stubs

None.

## Threat Flags

None.

## Self-Check: PASSED

Commits ae78587 and c48591c verified present; all modified files exist.
