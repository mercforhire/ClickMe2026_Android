---
phase: 06-booking-flow-free-bookings
plan: 12
subsystem: booking
tags: [compose, screen-assembly, bottom-sheet, alert-dialog, back-handling, integration-test]
requires:
  - phase: 06-06
    provides: MakeABookingViewModel with SavedStateHandle persistence
  - phase: 06-07
    provides: expert card, topic picker, topic sheet
  - phase: 06-08
    provides: calendar card, time slots card
  - phase: 06-09
    provides: meeting type, notes, summary, Book Now
  - phase: 06-11
    provides: book(), dismissError(), consumeCompletedBooking()
provides:
  - MakeABookingScreen (ViewModel wrapper) and stateless MakeABookingContent
  - MAKE_A_BOOKING_SCROLL_TAG
affects: [06-13, 06-14]
tech-stack:
  added: []
  patterns:
    - "Wrapper/content split with method references for every callback"
    - "One-shot success callback: consume completedBookingId before onBooked"
    - "BackHandler plus disabled app-bar Back while a request is in flight"
key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingScreen.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingScreenTest.kt
  modified: []
key-decisions:
  - "Back is blocked (not allowed) while submitting, per research Open Question 4"
  - "Scaffold contentWindowInsets is zero; imePadding on the column and navigationBarsPadding on a trailing spacer avoid double-counting insets"
  - "No rememberSaveable fallback: 06-06 confirmed SavedStateHandle works with assisted injection"
requirements-completed: [BOOK-01, BOOK-02, BOOK-03]
duration: ~30 min
completed: 2026-10-22
---

# Phase 6 Plan 12: Make a Booking Screen Summary

`MakeABookingContent` assembles the eight sections (Expert, Topic of Discussion, Select a Time, Available Time Slots, Meeting Type, Message, Summary, Book Now) under a "Book a Session" app bar, with the topic ModalBottomSheet, the "Couldn't book" AlertDialog, Back blocking during a request, tap/scroll focus clearing and ime insets. `MakeABookingScreen` wraps it with the ViewModel and turns `completedBookingId` into a single `onBooked(id)` call.

## Tasks

| Task | Name | Commit |
| ---- | ---- | ------ |
| 1 | MakeABookingContent and the ViewModel wrapper | c8f0534 |
| 2 | Real-ViewModel integration tests of the free path | 281ceac |

## Verification

- `./gradlew :app:testDevDebugUnitTest :app:assembleDevDebug ktlintCheck` exits 0 (whole unit suite green).
- `MakeABookingScreenTest`: 10 tests, 0 failures (7 content tests, 3 real-ViewModel integration tests).
- `bash scripts/check-design-rules.sh` prints "Design rules: OK".
- Acceptance greps: `BackHandler(enabled = state.isSubmitting)` 1, `backEnabled = !state.isSubmitting` 1, `consumeCompletedBooking()` 1, `imePadding()` 1, non-comment `LazyVerticalGrid|LazyColumn` 0, `UnconfinedTestDispatcher` in the test 2, `SLOT_UNAVAILABLE` 1.
- The Compose clock and the Unconfined Main dispatcher interleaved correctly, so the integration tests run through the real `MakeABookingScreen`; the planned fallback to `MakeABookingContent` was not needed.

## Deviations from Plan

None - plan executed as written.

Notes:
- The worktree started behind the expected base commit and was reset to it per the startup check (clean tree).
- Tests and implementation for Task 1 were committed together (the tests cannot compile before the screen exists), the same precedent as 06-04 and 06-07. Task 2 adds tests only.
- Section order is asserted with `positionInRoot.y`, because `boundsInRoot` is clipped to 0 for children outside the Robolectric viewport.

## Known Stubs

None.

## Threat Flags

None. T-06-39 (Back blocked while submitting, tested via the disabled app-bar Back), T-06-40 (server text rendered only as plain `Text` via `AuthErrorDialog`) and T-06-41 (`completedBookingId` consumed before `onBooked`, integration test asserts exactly one call and a null id afterwards) are mitigated.

## Open items

- System Back swallowing (`BackHandler(enabled = isSubmitting)`) is not exercised by a JVM test; it is a one-line guard. Device check stays with the deferred manual testing (Phase 2 policy).
- Rotation survival of the topic sheet (`rememberSaveable`) is not asserted in the JVM; covered by the deferred device check.

## Self-Check: PASSED

MakeABookingScreen.kt and MakeABookingScreenTest.kt exist; commit c8f0534 and the test(06-12) commit exist on branch worktree-agent-af20e301dc988179f. STATE.md and ROADMAP.md were not modified.
