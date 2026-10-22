---
phase: 06-booking-flow-free-bookings
fixed_at: 2026-10-22T20:57:19Z
review_path: .planning/phases/06-booking-flow-free-bookings/06-REVIEW.md
iteration: 2
findings_in_scope: 16
fixed: 16
skipped: 0
status: all_fixed
---

# Phase 6: Code Review Fix Report

**Fixed at:** 2026-10-22T20:57:19Z
**Source review:** .planning/phases/06-booking-flow-free-bookings/06-REVIEW.md
**Iteration:** 2

**Summary:**
- Findings in scope: 16 (CR-01 and WR-01 to WR-04 were fixed in iteration 1; IN-01 to IN-11 are fixed in this
  iteration)
- Fixed: 16
- Skipped: 0

**Gate after all iteration 2 fixes:** `./gradlew testDevDebugUnitTest ktlintCheck assembleDevDebug` passed:
BUILD SUCCESSFUL, 1316 unit tests, 0 failures, 0 errors, 0 skipped (iteration 1 ended at 1303). ktlint is clean and
assembleDevDebug builds.

Before each Info fix, the finding was re-checked against the code as it stands after iteration 1. All 11 still
applied. For IN-01, the ViewModel KDoc the review cited had already been reworded in iteration 1, but the stale
`/bookings/confirm` comments in `BookingErrors.kt` and `BookingRepository.kt` were still there.

## Fixed Issues

### CR-01: Programmatic scrolls clear focus, so the notes field loses focus and the keyboard closes as soon as it is focused

**Status:** fixed (iteration 1)
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

**Status:** fixed: requires human verification (iteration 1)
**Files modified:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingViewModel.kt`, `app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingViewModelBookTest.kt`
**Commit:** d5c2ed1
**Applied fix:** Success is now terminal. `isSubmitting` stays true after a successful request and after the id is
consumed, so the single-flight guard, Book Now and Back stay locked while the outgoing entry is still composed.
Updated `aSecondTapWhileInFlightIsIgnored` and `successExposesTheBookingIdOnceUntilConsumed` to match. Added
`aTapAfterSuccessAndConsumptionNeverSendsASecondRequest`.

### WR-02: A booking that succeeds while the app is in the background is lost if the process dies before the screen resumes

**Status:** fixed: requires human verification (iteration 1)
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

**Status:** fixed: requires human verification (iteration 1)
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

**Status:** fixed: requires human verification (iteration 1)
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

### IN-01: KDoc names the wrong endpoint (`/bookings/confirm`)

**Status:** fixed
**Files modified:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/BookingErrors.kt`, `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/BookingRepository.kt`
**Commit:** 940e2c3
**Applied fix:** `BookingFailure`, `toBookingFailure` and `requestBooking` now name `POST /bookings/request`.
`requestBooking` also notes that `POST /bookings/confirm` is the paid Stripe path (Phase 7). The repository KDoc now
says the server re-checks the duration when the booking is "requested" instead of "confirmed". The ViewModel KDoc
the review cited had already been reworded in iteration 1.

### IN-02: Two different `DEFAULT_DURATION_MINS` constants

**Status:** fixed
**Files modified:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/requestsent/BookingRequestSentUiState.kt`
**Commit:** f85189a
**Applied fix:** Removed the private `Long` copy. `toRequestSentDetails` imports the public
`makeabooking.DEFAULT_DURATION_MINS` and uses `(topic.durationMins ?: DEFAULT_DURATION_MINS).toLong()`.

### IN-03: Unused members

**Status:** fixed
**Files modified:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingUiState.kt`, `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/requestsent/BookingRequestSentUiState.kt`, `app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/ExpertAvailabilityData.kt`, `app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingUiStateTest.kt`, `app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingViewModelBookTest.kt`
**Commit:** 80597ef
**Applied fix:**
- `canBook` is removed. It duplicated what `MakeABookingBookButton` derives from `disabledReason` and
  `isSubmitting`. Its two UiState tests now assert `bookDisabledReason` only. The ViewModel test that checked it
  after success now asserts `isSubmitting` stays true.
- `BookingRequestSentDetails.isFree` is kept but documented as reserved for Phase 7. iOS hides the
  payment-lifecycle card behind `!isFree`, and Phase 6 has no such card yet.
- `ExpertAvailabilityData.clientTimezone` is kept for parity with the iOS DTO and documented as deliberately not
  read, because slots use the device zone.

### IN-04: The Request Sent headline crashes if a translation drops the `%1$s` placeholder

**Status:** fixed
**Files modified:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/requestsent/subviews/BookingRequestSentHeadline.kt`, `app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/requestsent/BookingRequestSentScreenTest.kt`
**Commit:** 7d7f09b
**Applied fix:** The body building moved into an `internal fun requestSentBody(template, expertName, nameStyle)`.
When the marker is missing (`index < 0`), it appends the template as is. `NAME_MARKER` is now `internal` so the
test can use it. Added `headlineBodyStylesOnlyTheExpertName` and
`headlineBodyWithoutTheNamePlaceholderFallsBackToTheTemplate`.

### IN-05: "Same zone" is decided by comparing IANA ids as strings

**Status:** fixed: requires human verification
**Files modified:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/BookingTimezoneNote.kt`, `app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/BookingTimezoneNoteTest.kt`
**Commit:** 4f34c84
**Applied fix:** A new `isSameZone(a, b)` returns true when any of these match: the ids, `normalized()`, or the
full `ZoneRules`. Aliases (`Asia/Calcutta`/`Asia/Kolkata`, `US/Eastern`/`America/New_York`) and the two spellings
of UTC (`UTC`/`Etc/UTC`) now give the single-zone note.

I chose full-rules equality over the review's "same offset at noon" option. With the offset check, Toronto and New
York would count as the same zone. They have different rule histories, so they stay "Different", and a test pins
this.

**To verify:** on device, java.time takes its rules from ICU, so alias equality there should be confirmed once.

### IN-06: Section errors show "check your connection" for server errors that have no message

**Status:** fixed
**Files modified:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/BookingErrors.kt`, `app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/BookingErrorsTest.kt`
**Commit:** 5329585
**Applied fix:** In `toSectionMessage`, an `Http` error with no `userMessage` now maps to `login_error_unknown`.
`Decoding` also maps to `login_error_unknown`, which matches `toBookingFailure`. Only `Network` still shows the
connection copy. The fallback test is split into a network-copy test and an unknown-copy test.
`ExploreViewModel` has its own private `toSectionMessage` and is not touched.

### IN-07: The "one odd value cannot break decoding" comment is not true for timestamps

**Status:** fixed
**Files modified:** `app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/ExpertAvailabilityData.kt`
**Commit:** 21d31c6
**Applied fix:** I corrected the comment and did not add a lenient adapter. iOS decodes `startUtc` as `Date?`,
which also throws on a malformed value, so failing strictly matches iOS. The KDoc now says that missing or null
fields are tolerated, but a malformed `start_utc`/`end_utc` still fails the whole month as
`ApiException.Decoding`. After IN-06, that failure shows the generic copy instead of the network copy.

### IN-08: Once the user moves to another month, the selected day can wrongly show "No available slots on this day"

**Status:** fixed: requires human verification
**Files modified:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/BookingCalendar.kt`, `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingUiState.kt`, `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingScreen.kt`, `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/subviews/MakeABookingTimeSlotsCard.kt`, `app/src/main/res/values/strings_make_a_booking.xml`, `app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/BookingCalendarTest.kt`, `app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingCalendarSlotsTest.kt`, `app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingUiStateTest.kt`
**Commit:** 5fd1f43
**Applied fix:**
- New `isInLoadedWindow(date, displayed)`: true when the date is in the displayed month or one of its two
  neighbours. `MakeABookingUiState.isSelectedDateLoaded` exposes it.
- The slots card has a new `selectedDateLoaded` parameter (default `true`, and the screen passes the state value).
  When it is false, the card shows "Pick a day to see its available times." (new string
  `make_a_booking_pick_a_day`) instead of the no-slots copy.
- Tests cover the window bounds, the UiState flag, and the card prompt.

**Limitation:** if a neighbour month fails to load (that failure is silent), a selected day in that month still
shows the no-slots copy. The new copy is new UI text that iOS does not have.

### IN-09: A slot that has expired is cleared silently on Book Now

**Status:** fixed: requires human verification
**Files modified:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingViewModel.kt`, `app/src/main/res/values/strings_make_a_booking.xml`, `app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingViewModelBookTest.kt`
**Commit:** 6b5e9b5
**Applied fix:** When `book()` finds that the selected slot has already started, it still clears the slot and sends
nothing. It now also sets `dialogError` to the new string `make_a_booking_error_slot_passed` ("That time has passed.
Please pick another."). The message appears in the existing "Couldn't book" dialog. `aSlotThatHasStartedIsClearedAndNotSent`
now asserts the dialog and that dismissing it clears it. iOS has no past-slot check, so this whole branch is
Android-only.

### IN-10: "Back to Home" doesn't select the Explore tab (iOS `returnToHome` sets `selectedTab = .explore`)

**Status:** fixed: requires human verification
**Files modified:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/home/ClientBookingFlowEntries.kt`, `app/src/main/java/com/uptrendinvestments/clickmeapp/navigation/Navigator.kt`, `app/src/test/java/com/uptrendinvestments/clickmeapp/navigation/BookingFlowNavigationTest.kt`, `app/src/test/java/com/uptrendinvestments/clickmeapp/navigation/NavigatorTest.kt`
**Commit:** 3f40a32
**Applied fix:** I checked the iOS source: `HomeClientView.swift` `returnToHome` runs `explorePath.reset()` and
then `selectedTab = .explore`. `onBackToHome` now calls `navigator.resetAndSelect(ExploreRoot, setOf(ExploreRoot))`,
which matches that exactly.

`resetAndSelect` now records a `Pop` transition when the target tab is already selected, because only its stack
changes. It records `TabSwitch` otherwise. This keeps today's Explore-to-Explore "Back to Home" on the pop
animation it had with `reselect`. The `reselect` KDoc no longer describes Back to Home.

Tests added:
- `backToHomeFromAnotherTabSelectsExplore`
- `resetAndSelectOfTheSelectedTabAnimatesAsAPop`

### IN-11: Test coverage gaps and a tautological test

**Status:** fixed
**Files modified:** `app/src/test/java/com/uptrendinvestments/clickmeapp/navigation/BookingFlowNavigationTest.kt`, `app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/EntrySavedStateHandleTest.kt`, `app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingScreenTest.kt`
**Commit:** 62372f9
**Applied fix:**
- **Tautological test:** deleted `requestSentKeyCarriesOnlyTheBookingId`.
- **Back:** the old `backIsDisabledWhileSubmittingAndEnabledOtherwise` is split into `backArrowIsDisabledWhileSubmitting`
  and `backArrowIsEnabledWhenIdle`, so the "enabled otherwise" half is now asserted. Added
  `systemBackIsSwallowedWhileSubmitting` and `systemBackReachesNavigationWhenIdle`. They send a real
  `OnBackPressedDispatcher.onBackPressed()` under an outer `BackHandler` that stands in for NavDisplay, the same
  pattern as the signup tests.
- **Restore:** added `entrySavedStateHandleSurvivesRecreationWithFreshViewModels`. It uses `StateRestorationTester`
  with the real client shell and its Nav3 decorators, and a fresh `ViewModelStoreOwner` to stand in for process
  death. A value written to the entry ViewModel's `SavedStateHandle` comes back in a newly created ViewModel
  (`created == 2`). For this, `ProbeKey` became `internal` instead of `private`, because kotlinx.serialization
  cannot reflect a private object when it saves the back stack.
- **Notes focus:** already covered in iteration 1 (CR-01).

**Note:** the test still passes with the owner swap disabled, because Nav3 already recreates the entry ViewModel
when the composition is restored. The swap is kept so the test keeps emulating process death if that changes.

---

_Fixed: 2026-10-22T20:57:19Z_
_Fixer: Claude (gsd-code-fixer)_
_Iteration: 2_
