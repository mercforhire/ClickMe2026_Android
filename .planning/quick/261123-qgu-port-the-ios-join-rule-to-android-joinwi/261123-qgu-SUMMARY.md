---
phase: quick-261123-qgu
plan: 01
subsystem: calls
tags: [join-window, can_join, ios-parity, bookings]
requires: []
provides:
  - "isWithinJoinWindow(status, start, end, now, serverCanJoin) with the iOS and backend (1d18aac) three-tier rule"
  - "ExpertBookingActionsDto.canJoin from actions.can_join"
affects: [client Upcoming Booking Details, expert Upcoming card, expert dashboard Today's Sessions, expert Booking Summary, Explore Today's Sessions, My Bookings]
tech-stack:
  added: []
  patterns: ["explicit nullable server flag, no default, at every Join call site"]
key-files:
  created: []
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/managers/JoinWindow.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/ExpertBookingsPayload.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/upcomingbooking/UpcomingBookingUiState.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/upcomingbooking/UpcomingBookingScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/bookingsummary/ExpertBookingSummaryUiState.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/bookingsummary/ExpertBookingSummaryScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/upcomingbookings/subviews/ExpertUpcomingCard.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/dashboard/subviews/TodaysSessionsSection.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/explore/subviews/ExploreTodaysSessionsSection.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/MyBookingsScreen.kt
decisions:
  - "Declared actions.can_join on ExpertBookingActionsDto (additive Moshi field, no backend change) because the backend sends it on both expert endpoints and iOS passes it at all three expert Join sites"
  - "JOIN_ENABLED_BOOKING_STATUSES kept separate from the TodaysSessions.kt membership set so pending_reschedule still renders a disabled Join"
status: complete
commits: 3
plan_head_before: 3e478ba14ad070589815b23b9991774c62835226
plan_head_after: d22f4b83561aa1df290b915918527d95caa3b82f
actuals:
  tokens: 40000
  tasks: 3
  commits: 3
---

# Phase quick-261123-qgu Plan 01: Port the iOS join rule to Android Summary

Android `isWithinJoinWindow` now matches iOS `JoinWindow.isOpen` and the backend `evaluateJoinWindow` at 1d18aac. Only confirmed, in_progress and completed can enable Join. It opens 60 minutes before start, a first join runs through max(end, start + 60 min), and a reconnect runs through end + 60 min only when the server's `can_join` is true.

## Commits (local on main, not pushed)

- 7d540a33 `fix(quick-261123-qgu): port the iOS join rule to isWithinJoinWindow` (Task 1, tracer)
- fd5c700d `fix(quick-261123-qgu): enable expert Join reconnect from actions.can_join` (Task 2)
- d22f4b83 `fix(quick-261123-qgu): pass the join flag explicitly at every call site` (Task 3)

## What changed

- JoinWindow.kt: added `JOIN_ENABLED_BOOKING_STATUSES`, `JOIN_LEAD_SECONDS`, `JOIN_MIN_FIRST_SPAN_SECONDS`, `JOIN_RECONNECT_GRACE_SECONDS`. Removed the `completed` special branch and the private `isInLiveWindow`. The KDoc cites iOS JoinWindow.swift (plans 02-04, 02-14) and backend `evaluateJoinWindow` and `JOINABLE_BOOKING_STATUSES` at 1d18aac. `JOIN_WINDOW_SECONDS` is now documented as the Starting-now badge half-width.
- Client: `UpcomingBookingDetails.canJoin` mapped from `ClientBookingDetail.canJoin` and passed by Upcoming Booking Details.
- Expert: `ExpertBookingActionsDto.canJoin` (`actions.can_join`, default false) passed by the Upcoming card, dashboard Today's Sessions and Booking Summary (`ExpertSummaryDetails.canJoin`). Stale KDocs in ExpertBookingsPayload.kt updated.
- Explore Today's Sessions and My Bookings pass `serverCanJoin = null` (no flag in their payloads). The parameter has no default, so every caller chooses.
- Unchanged by design: `JOINABLE_BOOKING_STATUSES` (TodaysSessions.kt), `showsJoinCard`, `ExpertSummaryDetails.showsJoin`, buckets and the Today's Sessions filters.

## Tests

- New JoinWindowTest cases (edges one second either side, status and flag tables), UpcomingBookingScreenTest (3), UpcomingBookingViewModelTest mapper test, ExpertBookingsApiTest decode test, ExpertBookingSummaryScreenTest (2).
- Full `:app:testDevDebugUnitTest`: 5452 tests, 0 failures, 0 errors, 0 skipped.
- `ktlintCheck`: pass. `lintDevDebug`: pass.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] ktlint chain-method-continuation in the new decode test**
- **Found during:** Task 2
- **Fix:** Collected results into lists and mapped them, instead of long single-line chains.
- **Files modified:** ExpertBookingsApiTest.kt (same commit)

Legacy tests: the old-rule tests in JoinWindowTest (`inProgressAndPendingReschedule_followTheSameWindow`, `completed_isJoinableWhileEndIsInTheFuture`, `completed_ignoresTheStartWindow`) were replaced as the plan directed; the WR-09 group was reworked onto the new tiers. No other existing test needed an expectation change.

## Follow-ups (out of scope, not built)

1. Visibility still ends at the scheduled end for completed. `showsJoinCard` and `ExpertBookingSummaryUiState.showsJoin` hide the card for completed once end has passed, and `isRejoinableCompleted` and its expert counterpart move completed rows to Past at end. iOS `ExpertJoinVisibility` shows Join for completed while `isOpen` is true, so a completed booking past its end shows no Join on Android even when the server would allow a reconnect.
2. The iOS record `/Users/LeonChen/Documents/ClickMe2026/.planning/phases/02-shared-ios-plumbing/02-04-SUMMARY.md` still says the Android follow-up is "logged, not built". It belongs to the iOS repo and was not edited.
3. The STATE.md Phase 9 backend note about the join window (120 min, can_join ignored) is now stale.

## Known Stubs

None.

## Threat Flags

None. The new `can_join` fields are non-sensitive Booleans, non-null with a false default.

## Self-Check: PASSED

- JoinWindow.kt, ExpertBookingsPayload.kt and all six call sites found and edited.
- Commits 7d540a33, fd5c700d and d22f4b83 are in HEAD; 3 commits measured from the ledger base.
