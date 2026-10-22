---
phase: 06-booking-flow-free-bookings
fixed_at: 2026-10-22T20:27:44Z
review_path: .planning/phases/06-booking-flow-free-bookings/06-REVIEW.md
iteration: 1
findings_in_scope: 5
fixed: 5
skipped: 0
status: all_fixed
---

# Phase 6: Code Review Fix Report

**Fixed at:** 2026-10-22T20:27:44Z
**Source review:** .planning/phases/06-booking-flow-free-bookings/06-REVIEW.md
**Iteration:** 1

**Summary:**
- Findings in scope: 5 (CR-01, WR-01 to WR-04; the 11 Info findings were out of scope)
- Fixed: 5
- Skipped: 0

**Gate after all fixes:** `./gradlew testDevDebugUnitTest ktlintCheck assembleDevDebug` passed: BUILD SUCCESSFUL,
1303 unit tests, 0 failures, 0 errors, 0 skipped. ktlint is clean and assembleDevDebug builds.

## Fixed Issues

### CR-01: Programmatic scrolls clear focus, so the notes field loses focus and the keyboard closes as soon as it is focused

**Status:** fixed
**Files modified:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingScreen.kt`, `app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingScreenTest.kt`
**Commit:** 6166b0b
**Applied fix:** Replaced the `LaunchedEffect(scrollState.isScrollInProgress)` with
`scrollState.interactionSource.collectIsDraggedAsState()`. Focus is now cleared only on a user drag, so the notes
field's bring-into-view and the `imePadding` viewport shrink no longer clear it. Added two Robolectric tests:
- `notesFieldKeepsFocusWhenTheViewportShrinks`: focus the notes field, shrink the host from 640 dp to 240 dp, then
  `assertIsFocused()`. This test was run against the old code and failed there, so it reproduces the bug.
- `aUserDragClearsNotesFocus`: a swipe still dismisses focus.

The on-device keyboard check is still among the deferred device checks.

### WR-01: Book Now is re-enabled between success and navigation, which leaves a gap in single-flight

**Status:** fixed: requires human verification
**Files modified:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingViewModel.kt`, `app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingViewModelBookTest.kt`
**Commit:** d5c2ed1
**Applied fix:** Success is now terminal. `isSubmitting` stays true after a successful request and after the id is
consumed, so the single-flight guard, Book Now and Back stay locked while the outgoing entry is still composed.
Updated `aSecondTapWhileInFlightIsIgnored` and `successExposesTheBookingIdOnceUntilConsumed` to match. Added
`aTapAfterSuccessAndConsumptionNeverSendsASecondRequest`.

### WR-02: A booking that succeeds while the app is in the background is lost if the process dies before the screen resumes

**Status:** fixed: requires human verification
**Files modified:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingViewModel.kt`, `app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingViewModelBookTest.kt`, `app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingScreenTest.kt`
**Commit:** 9089cb2
**Applied fix:**
- The id is saved in `SavedStateHandle` under `booking_completed_id` before the state update, and removed in
  `consumeCompletedBooking()`.
- `initialState` restores the id and sets `isSubmitting = true`, so the form stays locked.
- `init` skips the topic and availability loads when a completed id was restored.
- The screen's existing `LaunchedEffect` then navigates to Request Sent.

Tests added:
- `aCompletedBookingSurvivesProcessDeathUntilConsumed`: the id is restored, nothing is loaded, and no second
  request is sent.
- `aFailedBookingPersistsNoCompletedId`.
- `aBookingRestoredAfterProcessDeathNavigatesToRequestSentOnce`: a screen-level check that the restored id
  triggers `onBooked` once.

### WR-03: "Today" and the viewer's zone are frozen when the ViewModel is created and read separately by the UI

**Status:** fixed: requires human verification
**Files modified:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingUiState.kt`, `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingViewModel.kt`, `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingScreen.kt`, `app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingViewModelTest.kt`, `app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingScreenTest.kt`, `app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingUiStateTest.kt`
**Commit:** c590da5
**Applied fix:**
- `zone: ZoneId` is now a required field of `MakeABookingUiState` and is set in `initialState`.
- `MakeABookingContent` no longer has its own `zone = ZoneId.systemDefault()` parameter. It passes `state.zone`
  to the slot chips, the timezone note and the Summary.
- Slot grouping and the request body's `timezone` both use `state.zone`.
- New `refreshClock()` re-derives the zone and `today`. It runs on `ON_RESUME` (`LifecycleEventEffect`) and at the
  start of `selectDate`, both month moves, `selectSlot` and `book`:
  - A selected day that is now in the past moves to today.
  - A displayed month that is now in the past moves to the current month and reloads.
  - A new zone regroups the cached raw days of the last load, without a network request.
  - It does nothing while a request is in flight.

Tests added: zone is in the state, yesterday cannot be selected after midnight, a month rollover on resume (reload,
saved state, previous month blocked), a zone change regroups the slots with no request and drops a slot that moved
to another day, and the request sends the new zone.

**Notes for review:**
- The subviews `MakeABookingTimeSlotsCard` and `MakeABookingSummarySection` still default `zone` to
  `ZoneId.systemDefault()`. The only production caller now always passes `state.zone`, and the previews rely on
  the default.
- Commit c590da5 also includes a KDoc reflow of `book()` in the ViewModel. The KDoc edited in WR-01 had a
  136-character line.

### WR-04: The form stays editable while a booking is in flight, so the Summary can disagree with what is being booked

**Status:** fixed: requires human verification
**Files modified:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingViewModel.kt`, `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingScreen.kt`, `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/subviews/MakeABookingTopicPicker.kt`, `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/subviews/MakeABookingCalendarCard.kt`, `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/subviews/MakeABookingTimeSlotsCard.kt`, `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/subviews/MakeABookingMeetingTypeCard.kt`, `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/subviews/MakeABookingNotesCard.kt`, `app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingViewModelBookTest.kt`, `app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingScreenTest.kt`
**Commit:** 509297b
**Applied fix:**
- **ViewModel:** these setters now return early while `isSubmitting` is true: `selectTopic`, `selectDate`,
  `goToPreviousMonth`, `goToNextMonth`, `selectSlot`, `selectMeetingType` and `onNotesChange`.
- **UI:** each card gets an `enabled` flag (default `true`), and the screen passes `!state.isSubmitting`:
  - the topic picker `Surface`;
  - the month arrows and day cells;
  - the slot chips, through a separate `interactive` flag, so in-flight chips are not dimmed or announced as
    "unavailable";
  - the meeting-type rows.
- **Notes:** the notes field becomes `readOnly` while submitting, so it keeps focus but cannot be edited.
- **Topic sheet:** it cannot open while submitting.

Tests added:
- `selectionChangesAreIgnoredWhileInFlight`
- `aSlotTakenAfterATapDuringFlightClearsTheRejectedSlotOnly`
- `theFormIsLockedWhileSubmitting`: the controls report as disabled and no callback fires.
- `theFormIsEditableWhenIdle`

**Note for review:** nothing is visually dimmed while submitting. Only the Book Now spinner signals it, which
matches the previous look. Confirm on device that this is acceptable.

---

_Fixed: 2026-10-22T20:27:44Z_
_Fixer: Claude (gsd-code-fixer)_
_Iteration: 1_
