---
phase: 06-booking-flow-free-bookings
plan: 06
subsystem: booking
tags: [viewmodel, hilt-assisted, saved-state, coroutines, availability]
requires:
  - phase: 06-01
    provides: FakeClickMeApi availability and details handlers
  - phase: 06-03
    provides: capNotes, isPastDay, canGoToPreviousMonth
  - phase: 06-04
    provides: MakeABookingUiState, groupSlots, toSectionMessage, MeetingType
provides:
  - BookingRepository (availability, requestBooking)
  - MakeABookingViewModel load-and-selection state machine with SavedStateHandle persistence
  - Shared booking test fixtures (BookingTestFixtures.kt)
affects: [06-11, 06-12, 06-13]
tech-stack:
  added: []
  patterns:
    - "Assisted @HiltViewModel with a plain SavedStateHandle constructor parameter"
    - "Cancellable availabilityJob with coroutineScope { async } x3, silent neighbor months"
key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/BookingRepository.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingViewModel.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/BookingTestFixtures.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingViewModelTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/EntrySavedStateHandleTest.kt
  modified: []
key-decisions:
  - "SavedStateHandle works with assisted injection in a Nav3 entry; the rememberSaveable fallback is NOT needed"
  - "Saved slot is applied once, after the first successful availability load, and only if still present and selectable"
requirements-completed: [BOOK-01, BOOK-02]
duration: ~35 min
completed: 2026-10-22
---

# Phase 6 Plan 06: Booking ViewModel Summary

Assisted `MakeABookingViewModel` with D-02 topic default, three-month parallel availability load with latest-wins cancellation, D-06/D-04/D-07 navigation and selection rules, and SavedStateHandle persistence of the six selection values.

## Saved-state check (research Open Question 3)

Both checks passed, so SavedStateHandle is a plain constructor parameter and `restoreSelection` / the `rememberSaveable` fallback was not built:
- `EntrySavedStateHandleTest` (Robolectric, real `HomeClientShell`, private `ProbeKey` entry) creates a ViewModel with `createSavedStateHandle()` inside a Nav3 entry and renders.
- `:app:assembleDevDebug` succeeds with `SavedStateHandle` in the `@AssistedInject` constructor (Hilt graph validated).

06-12 therefore does not need to save the six values in the screen.

## Tasks

| Task | Commit | Content |
| ---- | ------ | ------- |
| 1 | 73e79c6 | BookingRepository, fixtures, topics load (D-02, restore, Retry), saved-state check |
| 2 | b9edf54 | Three-month load, month/day navigation, slot/meeting/notes rules, persistence |

## Verification

- `./gradlew :app:testDevDebugUnitTest --tests '*EntrySavedStateHandleTest' --tests '*MakeABookingViewModelTest' :app:assembleDevDebug ktlintCheck` exits 0.
- MakeABookingViewModelTest: 23 tests, 0 failures. EntrySavedStateHandleTest: 1 test, 0 failures.
- Acceptance greps: `availabilityJob?.cancel()` 1, `runCatching` 0, `groupSlots(` 1, `async` 4, `availableMeetingTypes` 0 (non-comment), `@AssistedFactory` 1, repository call `getExpertAvailability(expertId, month.toString(), null)` 1.

## Notes for downstream plans

- `loadAvailability(month, keepVisible)` is private; 06-11 adds `book()` in the same file and calls it with `keepVisible = true` after a 409.
- `pendingTopicId` and `pendingSlotMillis` hold restored values until applied; user actions that change the topic or clear the slot drop them.
- Corrupt restored date/month strings are ignored via a small `parseOrNull` helper (no `runCatching` in the file, per the acceptance grep).
- Test helpers in `BookingTestFixtures.kt`: `bookingKey`, `topicDto`, `detailsWith`, `slotDto`, `availabilityOf`, `NOW`, `newViewModel`.

## Deviations from Plan

None - plan executed exactly as written (the SavedStateHandle check passed, so no fallback).

## Known Stubs

None.

## Self-Check: PASSED

Created files exist; commits 73e79c6 and b9edf54 exist on branch worktree-agent-ab426a002b7f7295a.
