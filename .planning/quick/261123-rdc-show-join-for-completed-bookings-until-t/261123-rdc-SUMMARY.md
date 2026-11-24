---
phase: quick-261123-rdc
plan: 01
subsystem: join-window
tags: [join, booking-details, ios-parity, reconnect-window]
requires: [261123-qgu]
provides:
  - "showsJoinCard(status, start, end, now, serverCanJoin): shared Join visibility rule, equal to iOS ExpertJoinVisibility.shows"
affects:
  - client Upcoming Booking Details
  - expert Booking Summary
key-files:
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/managers/JoinWindow.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/upcomingbooking/UpcomingBookingScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/upcomingbooking/UpcomingBookingUiState.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/bookingsummary/ExpertBookingSummaryScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/bookingsummary/ExpertBookingSummaryUiState.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/bookingsummary/ExpertBookingSummaryViewModel.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/upcomingbookings/ExpertBookingBuckets.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/BookingBuckets.kt
decisions:
  - "Client details keeps the Phase 16.5 D-03 hide (orchestrator decision): Join disappears once the reconnect window closes"
  - "List buckets and Today's Sessions strips stay unchanged, matching iOS; pinned by characterization tests"
status: complete
commits: 5
plan_head_before: e3adf7d07c04adf4689e5b0bb3d5e50da7e15903
plan_head_after: ec7a2cf6362dae2c7478adb358fd277f88c24fc8
actuals:
  tokens: 40000
  tasks: 3
  commits: 5
---

# Quick 261123-rdc: Show Join for completed bookings until the reconnect window closes

One shared rule, `showsJoinCard`, now shows Join on both detail screens for confirmed, in_progress and pending_reschedule at every instant. For any other status it shows Join exactly while `isWithinJoinWindow` is true, so a completed booking keeps an enabled Join through the server-approved reconnect tier (end + 60 min) and loses it at the edge with no reload.

## What changed

- `JoinWindow.kt`: `showsJoinCard` takes `start` and `serverCanJoin`. Its body is `JOINABLE_BOOKING_STATUSES` membership or `isWithinJoinWindow(...)`. `isWithinJoinWindow` and the status sets are unchanged.
- Client Upcoming Booking Details: passes `start` and `details.canJoin`, evaluated against the live `joinNow`.
- Expert Booking Summary: visibility is computed on screen at `joinNow`. `ExpertSummaryDetails.showsJoin` is removed and `toSummaryDetails` takes only a `Locale`. `ExpertBookingSummaryViewModel` no longer takes a `Clock` (its only use fed that mapping; `JoinClock` still carries the live time).
- Bucket KDocs in `ExpertBookingBuckets.kt` and `BookingBuckets.kt` record why the lists keep the end-based edge.

## Per-surface record

| Surface | iOS rule (source) | Android before | Android after |
|---------|-------------------|----------------|---------------|
| Client Upcoming Booking Details | every status but pending_approval shows the card, `isOpen` enables it (UpcomingBookingViewModel.swift:203) | live statuses, or completed until end | live statuses, or isWithinJoinWindow (completed visible exactly while enabled); D-03 hide kept for terminal statuses |
| Expert Booking Summary | ExpertJoinVisibility.shows: alwaysShown or `isOpen`, every 60 s | live statuses, or completed until end, fixed at load | live statuses, or isWithinJoinWindow, at joinNow (exact iOS match) |
| Client My Bookings buckets | completed stays Upcoming while end > now (MyBookingsViewModel.swift:213) | same | unchanged (iOS drops at end) |
| Expert Upcoming buckets | completed stays Upcoming while end > now (UpcomingBookingsViewModel.swift:208) | same | unchanged (iOS drops at end) |
| Explore and expert-dashboard Today's Sessions | completed excluded (isJoinable) | completed excluded | unchanged |

## D-03 deviation on client details

iOS client details shows a (disabled) Join card for every status except pending_approval. Android keeps Phase 16.5 D-03 because Payment History opens this screen for any booking: a completed booking shows Join only while it can be enabled, and terminal, unknown and pending_approval statuses show none. Per the orchestrator, a completed booking's Join therefore disappears once the reconnect window closes, where iOS would show a disabled card.

## Side effect

A completed booking more than 60 minutes before its start is now hidden (matches iOS `ExpertJoinVisibility`). The old rule showed it. This cannot happen in practice.

## Legacy tests replaced or deleted

- `JoinWindowTest.showsJoinCardTruthTable`: replaced by six `showsJoinCard_*` tests (old signature and old end-only rule).
- `UpcomingBookingScreenTest.completedAfterEndShowsNoJoinCard`: replaced by four tests, because the end is now inclusive and the card shows at exactly the end.
- `ExpertBookingSummaryViewModelTest.showsJoinFollowsTheStatusAndTheEndTime`: deleted (tested the removed property; its cases live in the `showsJoinCard` tests). The `showsJoin` assertion in `mapsTheDetailForDisplay` was dropped for the same reason.
- All `toSummaryDetails(now, Locale.US)` call sites in the expert tests now pass `Locale.US` only, and the `clock` argument was dropped from the two manual ViewModel constructions.

## New tests

- `JoinWindowTest`: six `showsJoinCard_*` tests, including a per-second sweep (session lengths 1800, 3600, 5400 s; can_join true, false, null) proving completed visibility equals `isWithinJoinWindow`.
- `UpcomingBookingScreenTest`: four completed-booking tests, including the live `joinNow` flip.
- `ExpertBookingSummaryScreenTest`: four tests through the real ViewModel and `ExpertBookingSummaryContent`, including the live flip.
- `ExpertBookingBucketsTest` and `BookingBucketsTest`: one characterization test each (list keeps the end edge, whatever can_join says).

## Deviations from plan

None in behavior. Task 2 RED was a compile failure on the new `toSummaryDetails(Locale)` signature rather than a runtime assertion failure, which the plan accepts for signature changes.

## Gate results

- Full `:app:testDevDebugUnitTest`: 5465 tests, 0 failures, 0 errors, 0 skipped.
- `ktlintCheck`: passed.
- `:app:lintDevDebug`: passed.

## Follow-ups

- Follow-up 1 of the qgu SUMMARY (completed-booking Join visibility ending at the scheduled end) is resolved by this task.
- iOS and backend repos were read-only references; no change is needed there.

## Commits

All local on main, none pushed:

- 8cc0fc40 test: client details completed Join tests
- 48d48e4b fix: shared `showsJoinCard` and client details wiring
- 1f079e45 test: expert summary completed Join tests
- 66a85fae fix: expert summary evaluates visibility live, drops load-time flag and ViewModel Clock
- ec7a2cf6 test: bucket characterization tests and KDoc

## Self-Check: PASSED

All five commits are ancestors of HEAD, and the modified files exist.
