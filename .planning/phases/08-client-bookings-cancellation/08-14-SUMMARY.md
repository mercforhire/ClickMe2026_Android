---
phase: 08-client-bookings-cancellation
plan: 14
subsystem: navigation
tags: [navigation, nav3, shell, explore-handoff]
requires: [08-09, 08-11, 08-12, 08-13]
provides:
  - "clientBookingManagementEntries: Upcoming, Cancel and Booking Summary routes registered once, bottom bar hidden"
  - "Navigator helpers openUpcomingBooking, openBookingSummary, showMyBookings, showExplore"
  - "HomeClientShell bookingsRoot slot; the Bookings tab renders My Bookings"
  - "Today's Sessions card is one click target (onSessionClick) that opens Upcoming Booking Details"
affects: [08-15]
tech-stack:
  added: []
  patterns: ["Hilt-free entry registration with composable content slots so HomeShellsTest drives the real routes", "Single showMyBookings reset of both Explore and Bookings stacks for every cancel origin"]
key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/home/ClientBookingManagementEntries.kt
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/explore/subviews/ExploreTodaysSessionsSection.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/explore/ExploreScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/home/ClientBookingFlowEntries.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/home/HomeClientScreen.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/explore/ExploreSectionsTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/explore/ExploreScreenTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/HomeShellsTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/navigation/BookingFlowNavigationTest.kt
key-decisions:
  - "A successful cancel (or a 404/409 after OK) calls Navigator.showMyBookings, which resets both the Explore and Bookings stacks, so no details or cancel screen of a cancelled booking is reachable by Back from either origin"
  - "PaymentMethodsKey stays registered once by clientPaymentMethodsEntry; the three new routes only navigate to it"
requirements-completed: [BOOK-05, BOOK-06, BOOK-07, BOOK-08, BOOK-09]
duration: 6min
completed: 2026-10-24
---

# Phase 8 Plan 14: Client Shell Wiring Summary

The Bookings tab now renders My Bookings, the Upcoming, Cancel and Booking Summary routes are registered with the bottom bar hidden, and every Phase 8 navigation callback is connected. The Explore Today's Sessions card opens Upcoming Booking Details.

## Tasks

| Task | Name | Commit |
| ---- | ---- | ------ |
| 1 | Today's Sessions card opens Upcoming Booking Details (tests and implementation together) | 7a105d8 |
| 2 | Booking-management entries, Navigator helpers, shell wiring (navigation tests included) | fe928de |
| 3 | HomeShellsTest coverage for the new routes (10 new tests) | 2ce87b6 |

## Verification

- `testDevDebugUnitTest ktlintCheck assembleDevDebug` passes (full dev unit suite, 0 failures). HomeShellsTest has 38 tests, 10 new. `scripts/check-design-rules.sh` prints "Design rules: OK".
- Acceptance greps: `metadata = BottomBar.hidden()` in the management entries 3; `entry<ClientBookingsRoot> { bookingsRoot(navigator) }` 1; `entry<PaymentMethodsKey>` in the management entries 0; "stays the placeholder" 0; `clientBookingManagementEntries(` in HomeClientScreen 1; "no tap action" 0; `TODAYS_SESSION_JOIN_TAG = "todays_session_join"` unchanged.
- Threats: T-08-47 mitigated and tested (post-cancel reset from Bookings and Explore origins, in BookingFlowNavigationTest and HomeShellsTest). T-08-48 mitigated and tested (bar hidden on all three routes). T-08-49 accepted.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Existing test read the time text from the merged tree**
- **Found during:** Task 1
- **Issue:** Making the card clickable merges its descendants, so `ExploreSectionsTest.sessionTimeText()` (tag `todays_session_time`) no longer found the node in the merged tree, failing two existing tests.
- **Fix:** The helper reads the tag with `useUnmergedTree = true`. The production tag and the text are unchanged.
- **Also:** the old `assertHasNoClickAction()` assertion on the expert name was removed, since the card is now intentionally clickable.
- **Files modified:** ExploreSectionsTest.kt
- **Commit:** 7a105d8

**2. Task ordering note.** Task 1 changed the `ExploreScreen` signature, so `HomeClientScreen` got a temporary `onOpenBooking = {}` in the Task 1 commit to keep the build green. Task 2 replaced it with `navigator::openUpcomingBooking`.

Process note: RED steps were compile failures (new parameters and symbols absent); tests and implementation were committed together per task.

## Known Stubs

None added. The Join Call buttons on the Today's Sessions card and the upcoming join card remain disabled by design until Phase 9 (`PHASE-9` markers kept).

## Threat Flags

None.

## Self-Check: PASSED

- ClientBookingManagementEntries.kt exists; commits 7a105d8, fe928de, 2ce87b6 exist in git log.
