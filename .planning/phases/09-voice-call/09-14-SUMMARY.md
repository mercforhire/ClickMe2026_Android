---
phase: 09-voice-call
plan: 14
subsystem: calls
tags: [join, bookings, guard, compose]
requires:
  - phase: 09-voice-call (plans 03, 04, 11)
    provides: JoinWindow / JoinClock / JoinCallButton, CallStarter / CallGuard contracts and fakes, CallModule Hilt bindings
provides:
  - Live Join Call on Upcoming Booking Details (both meeting types)
  - Live Join Call on the My Bookings upcoming card
  - Cancel Booking guard on Upcoming Booking Details (attemptModifyBooking)
affects: [09-15 (Today's Sessions Join), 09-16 (cancel flow confirm guard)]
tech-stack:
  added: []
  patterns:
    - "Join sites collect the shared JoinClock.now and compute isWithinJoinWindow in the composable; the ViewModel never re-checks the window at tap time"
key-files:
  created: []
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/upcomingbooking/UpcomingBookingUiState.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/upcomingbooking/UpcomingBookingViewModel.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/upcomingbooking/UpcomingBookingScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/upcomingbooking/subviews/UpcomingBookingJoinCard.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/subviews/UpcomingBookingCard.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/MyBookingsScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/MyBookingsViewModel.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/upcomingbooking/UpcomingBookingViewModelTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/upcomingbooking/UpcomingBookingScreenTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/MyBookingsViewModelTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/MyBookingsScreenTest.kt
key-decisions:
  - "Join is enabled in the composable via isWithinJoinWindow(details.status.raw, start, end, joinNow); the ViewModels call CallStarter.startCall without re-checking the window (server decides on /join, D-02)"
  - "The server can_join is not read anywhere in the new code (acceptance grep for canJoin is 0)"
  - "UpcomingBookingContent and MyBookingsContent take joinNow and onJoin as required parameters so the tests drive any time without a clock"
requirements-completed: [CALL-01, CALL-04, CALL-08]
metrics:
  tasks: 2
  files: 11
  completed: 2026-10-24
---

# Phase 9 Plan 14: Join on Upcoming Details and My Bookings, Cancel guard Summary

**Two of the three Join sites now start the call through `CallStarter`, enabled live by the shared 30 s join window, and Cancel Booking on Upcoming Details is blocked while that booking's call is live.**

## Accomplishments
- Upcoming Booking Details: `UpcomingBookingJoinCard` shows `JoinCallButton` (tag `upcoming_booking_join`) for in-app voice and for Skype/Zoom, where it sits below the unchanged meeting-link row. `UpcomingBookingDetails` gains `expertId`, mapped from `expert.id` (blank to null). The ViewModel gains `joinNow`, `join()` (no-op before load) and `attemptCancel()`. The screen wraps Cancel as `if (viewModel.attemptCancel()) onCancel()`.
- My Bookings: the upcoming card's disabled placeholder is replaced by `JoinCallButton` (tag `my_bookings_join`). `MyBookingsViewModel.join(item)` builds the request from the list item. The Join tap does not open the details (the card's click is not invoked). This closes the iOS gap where the card passes an empty `onJoinCall`.
- Rejoin: a completed booking whose end has not passed shows an enabled Join on both screens, through `isWithinJoinWindow`.
- All `PHASE-9` markers and the `DisabledJoinButton` / `JoinPlaceholder` composables are gone.

## Task Commits
1. **Task 1: Upcoming Booking Details live Join and cancel guard** - `5faa555` (feat)
2. **Task 2: My Bookings upcoming card live Join** - `ea406d0` (feat)

## Verification
- `:app:testDevDebugUnitTest` for the `mybookings.*` and `upcomingbooking.*` packages, `:app:assembleDevDebug` (Hilt graph resolves `CallStarter`, `CallGuard`, `JoinClock`) and `ktlintCheck`: BUILD SUCCESSFUL.
- `scripts/check-design-rules.sh`: OK.
- Acceptance greps: no `PHASE-9` in `upcomingbooking/` (0) or `PHASE-9|JoinPlaceholder` in `mybookings/` (0); `canJoin` in `UpcomingBookingViewModel.kt` 0; both tag constants present once.
- New tests: ViewModel join request mapping, join before load, no window re-check at tap, `attemptCancel` answer and recorded attempt, `joinNow`; screen tests for enabled/disabled Join with the hint, Skype/Zoom link plus Join below it, completed rejoin, pending banner without Join, Cancel allowed and Cancel blocked through the real ViewModel; My Bookings card Join enabled, not opening details, disabled with hint, rejoin, pending banner.

## Deviations from Plan

**1. [Rule 3 - Blocking] Worktree base reset.** HEAD was `d1217fb`, an ancestor of the required base `3150a62` (clean tree); ran the sanctioned `git reset --hard 3150a62` at startup.

**2. [Process] TDD red/green not committed separately.** As in earlier Phase 9 plans, implementation and tests were written together and committed per task. Tests were then run against the behavior lists.

**3. Environment.** The first Gradle run died with `OutOfMemoryError: Java heap space` during Kotlin body lowering (several daemons share the machine); the identical re-run succeeded. No code change.

Otherwise the plan was executed as written. No files outside `files_modified` were touched.

## Known Stubs
None.

## Threat Flags
None. T-09-47 (window is a UX gate only, no server-side `can_join` use), T-09-48 (`attemptModifyBooking` before the cancel flow opens, tested both ways) are mitigated as planned. T-09-49 accepted.

## Notes for later plans
- 09-16 must apply the second guard at the cancel flow's final confirm (RESEARCH Open Question 5); this plan guards only the entry from Upcoming Details.
- Today's Sessions Join remains plan 09-15.

## Self-Check: PASSED
All 11 modified files exist; commits `5faa555` and `ea406d0` exist on the branch.
