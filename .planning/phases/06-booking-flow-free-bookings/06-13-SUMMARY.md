---
phase: 06-booking-flow-free-bookings
plan: 13
subsystem: navigation
tags: [navigation3, shell-wiring, hilt, cleanup]
requires: [06-02, 06-10, 06-12]
provides:
  - clientBookingFlowEntries helper registering BookingKey and RequestSentKey once
  - Hilt-backed MakeABookingScreen and BookingRequestSentScreen reachable from the client shell
affects: [08-my-bookings]
key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/home/ClientBookingFlowEntries.kt
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/home/HomeClientScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/BookingKey.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingScreen.kt
    - app/src/main/res/values/strings_make_a_booking.xml
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/HomeShellsTest.kt
  deleted:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingStubScreen.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingStubScreenTest.kt
requirements-completed: [BOOK-03, BOOK-04]
completed: 2026-10-22
---

# Phase 6 Plan 13: Booking flow shell wiring Summary

Booking Session now reaches the real Make a Booking screen, success replaces it with Request Sent via `replaceTop` (D-12), and View My Bookings / Back to Home reset the Explore and Bookings stacks (D-13); the Phase 5 stub is removed.

## What was built
- `clientBookingFlowEntries(navigator, booking, requestSent)` (Hilt-free) registers `BookingKey` and `RequestSentKey` once, both with `BottomBar.hidden()`.
  - `onBooked` calls `replaceTop(RequestSentKey(id))`.
  - `onViewBookings` calls `resetAndSelect(ClientBookingsRoot, setOf(ExploreRoot, ClientBookingsRoot))`.
  - `onBackToHome` calls `reselect(ExploreRoot)`.
- `HomeClientScreen.detailEntries` supplies the assisted Hilt ViewModels (`MakeABookingViewModel.Factory`, `BookingRequestSentViewModel.Factory`) from each key. `HomeClientShell` no longer registers `BookingKey` itself, so the Nav3 duplicate-registration risk (T-06-43) is gone.
- HomeShellsTest: stub test replaced by `bookingRouteHidesBottomBarAndBackRestoresIt`, `bookingSuccessReplacesBookingWithRequestSent`, `viewMyBookingsResetsExploreAndSelectsBookings`, `backToHomeResetsExploreToRoot`.
- Removed the stub, its test and the two coming-soon strings.

## Commits
- a3efadc test(06-13): failing shell tests (RED)
- 48ae390 feat(06-13): wire booking flow routes into the client shell (GREEN)
- 865a37e chore(06-13): remove Make a Booking stub, its test and strings

## Verification
- `:app:testDevDebugUnitTest` (full dev suite), `ktlintCheck`, `:app:assembleDevDebug` and `scripts/check-design-rules.sh` all pass.
- Grep gates: one `entry<BookingKey>`, one `entry<RequestSentKey>`, zero stub / coming-soon references, `data class BookingKey` intact.

## Deleted files
- `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingStubScreen.kt`
- `app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingStubScreenTest.kt`

## Deviations from Plan

**1. [Rule 3 - Blocking/cleanliness] KDoc in MakeABookingScreen.kt**
- Its KDoc linked `[MakeABookingStubScreen]`, which would dangle after the deletion. Reworded it (comment-only change, file not in `files_modified`).

**2. Test fakes wrapped in Column**
- First test run failed because the fake buttons overlapped in the same Box and clicks hit the wrong button. Wrapped them in `Column`; no production impact.

The plan's verify command also names `*BookingFlowNavigationTest`, which does not exist in this repo; the full dev suite was run instead.

## Known Stubs
None. The Bookings tab remains the Phase 5 placeholder until Phase 8 (documented in the helper's KDoc).

## Self-Check: PASSED
