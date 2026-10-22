---
phase: 06-booking-flow-free-bookings
reviewed: 2026-10-22T19:52:27Z
depth: standard
files_reviewed: 40
files_reviewed_list:
  - app/src/main/java/com/uptrendinvestments/clickmeapp/model/enums/MeetingType.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/model/requests/BookingRequests.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/BookingRequestData.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/ClientBookingDetail.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/ExpertAvailabilityData.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/navigation/Navigator.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/network/ApiRequestUrls.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/network/ClickMeApi.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/home/ClientBookingFlowEntries.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/home/HomeClientScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/BookingCalendar.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/BookingErrors.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/BookingFormatting.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/BookingKey.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/BookingRepository.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/BookingSlots.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/BookingTimezoneNote.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingUiState.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/subviews/MakeABookingBookButton.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/subviews/MakeABookingCalendarCard.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/subviews/MakeABookingExpertCard.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/subviews/MakeABookingMeetingTypeCard.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/subviews/MakeABookingNotesCard.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/subviews/MakeABookingSummarySection.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/subviews/MakeABookingTimeSlotsCard.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/subviews/MakeABookingTopicPicker.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/subviews/MakeABookingTopicSheet.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/ClientBookingsRepository.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/requestsent/BookingRequestSentScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/requestsent/BookingRequestSentUiState.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/requestsent/BookingRequestSentViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/requestsent/RequestSentKey.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/requestsent/subviews/BookingRequestSentActions.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/requestsent/subviews/BookingRequestSentCheckIcon.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/requestsent/subviews/BookingRequestSentDetailsCard.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/requestsent/subviews/BookingRequestSentHeadline.kt
  - app/src/main/res/values/strings_make_a_booking.xml
  - app/src/main/res/values/strings_request_sent.xml
findings:
  critical: 1
  warning: 4
  info: 11
  total: 16
status: issues_found
---

# Phase 6: Code Review Report

**Reviewed:** 2026-10-22T19:52:27Z
**Depth:** standard
**Files Reviewed:** 40 main-source files (the 23 test files were skimmed to check what they actually cover)
**Status:** issues_found

## Summary

I reviewed the Make a Booking free path, Request Sent, the booking/detail DTOs, the API surface and the
navigation wiring. I checked them against the iOS source (`MakeABookingViewModel.swift`, `MakeABookingView.swift`,
`HomeClientView.swift`) and the backend (`booking.controller.js#createRequest`, `booking.schema.js#requestSchema`,
`clientBooking.controller.js#getDetail`, `availability.controller.js#getClientCalendar`).

The core logic is solid. UTC-to-local slot grouping, latest-wins cancellation of availability loads, the D-10 error
mapping, the strict request body (null notes omitted) and SavedStateHandle restore are all correct, and the tests
cover them well. The wire contract matches the backend: `POST /bookings/request` with `scheduled_start`,
`duration_minutes`, `timezone`, and `start_time`/`end_time`/`topic.is_free`/`expert.full_name` on the detail
response.

The one blocker is a UI interaction: the "clear focus when a scroll starts" effect also fires on programmatic
scrolls. Focusing the notes field causes exactly such a scroll (its own `bringIntoView`, and the scroll container
keeping the focused child visible when `imePadding` shrinks the viewport). So the keyboard gets dismissed as soon as
it opens and the Message field can't be used on a device. Robolectric tests don't catch this, and the manual device
check was deferred.

The warnings are about single-flight and restore edge cases around a successful booking, the time context being
frozen when the ViewModel is created, and the form staying editable while a request is in flight.

## Critical Issues

### CR-01: Programmatic scrolls clear focus, so the notes field loses focus and the keyboard closes as soon as it is focused

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingScreen.kt:122-124` (interacts with `:143` `imePadding()` and `subviews/MakeABookingNotesCard.kt:70-72`)

**Issue:**
```kotlin
LaunchedEffect(scrollState.isScrollInProgress) {
    if (scrollState.isScrollInProgress) focusManager.clearFocus()
}
```
`ScrollState.isScrollInProgress` is true for any scroll that runs through `ScrollableState.scroll {}`, not only
user drags. This is confirmed in compose-foundation 1.10.4: `DefaultScrollableState.scroll` sets
`isScrollingState = true` for the whole block, and `ContentInViewNode` (bring-into-view) scrolls through
`scrollingLogic.scroll(...)` → `scrollableState.scroll(...)`. Focusing the Message field starts two such
programmatic scrolls:

1. `MakeABookingNotesCard`'s own `LaunchedEffect(focused) { bringIntoViewRequester.bringIntoView() }`. The card is
   the sixth section, near the bottom, so this almost always scrolls.
2. When the IME opens, `imePadding()` (applied before `verticalScroll`) shrinks the scroll viewport.
   `ContentInViewNode.onRemeasured` sees the focused child clipped and starts an animated scroll to keep it visible.

Either scroll sets `isScrollInProgress = true`, the effect calls `clearFocus()`, and the keyboard is dismissed. The
user can't type a message, which breaks the Message section (UI-SPEC section 6, D-11 `client_notes`). No test covers
focus on this screen.

**Fix:** Only clear focus on a user drag. Watch the drag interactions of the scroll state's interaction source
instead of `isScrollInProgress`:
```kotlin
val isDragged by scrollState.interactionSource.collectIsDraggedAsState()
LaunchedEffect(isDragged) {
    if (isDragged) focusManager.clearFocus()
}
```
Alternatively, use a `NestedScrollConnection` that clears focus in `onPreScroll` only when
`source == NestedScrollSource.UserInput`. Add a Robolectric test that focuses `NOTES_FIELD_TAG` in a short viewport,
lets the bring-into-view animation run, and asserts the field is still focused (`assertIsFocused()`).

## Warnings

### WR-01: Book Now is re-enabled between success and navigation, which leaves a gap in single-flight

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingViewModel.kt:151,175` and `MakeABookingScreen.kt:69-73`

**Issue:** On success the ViewModel sets `isSubmitting = false` and `completedBookingId = id` together. The screen
then consumes the id and calls `replaceTop`. After consumption the state is `isSubmitting = false`,
`completedBookingId = null`, a free topic and a selectable slot. That makes `bookDisabledReason == null`, so Book Now,
the slots and the app-bar Back are all enabled again. Nav3 keeps the outgoing entry composed and interactive during
the push transition (`Navigator.navigate`'s own KDoc says so), and the entry's ViewModel is still alive until the
entry leaves composition. A tap during that window calls `book()`, which passes the `isSubmitting` guard and sends a
second `POST /bookings/request`. The same slot gets a 409 (an error dialog flashes on a screen that is leaving). If
the user picked a different slot in that window, a second real booking is created. The success-path test
(`successExposesTheBookingIdOnceUntilConsumed`) even asserts `isSubmitting == false` after success.

**Fix:** Make success terminal for this ViewModel. Keep `isSubmitting = true` on success, or add a `bookingCompleted`
flag that `book()` and `canBook`/`bookDisabledReason` respect:
```kotlin
val result = repository.requestBooking(body)
_uiState.update { it.copy(completedBookingId = result.bookingId) } // leave isSubmitting = true
```
and in `book()`: `if (s.isSubmitting || s.completedBookingId != null || bookingCompleted) return`.

### WR-02: A booking that succeeds while the app is in the background is lost if the process dies before the screen resumes

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingViewModel.kt:175, 214-216`

**Issue:** `completedBookingId` lives only in the `MutableStateFlow`. The screen collects with
`collectAsStateWithLifecycle` (STARTED), so if the response arrives while the app is backgrounded, the id waits in
memory until the user returns. If the process is killed in that window (a common case: the user taps Book and
switches apps), the restored entry is Make a Booking again. The restored slot is dropped because it is now
unavailable (`applyAvailability`), and nothing tells the user that a booking exists. Trying again gives a misleading
409 "That time slot was just taken". iOS has no process-death restore, so this gap is specific to the Android
restore feature added in this phase.

**Fix:** Persist the id when it is set (`savedStateHandle[KEY_COMPLETED_BOOKING_ID] = result.bookingId`), restore it
in `initialState`, and remove it in `consumeCompletedBooking()`. The screen's `LaunchedEffect` then navigates to
Request Sent after restore.

### WR-03: "Today" and the viewer's zone are frozen when the ViewModel is created and read separately by the UI

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingViewModel.kt:249, 305`; `MakeABookingScreen.kt:114`; `subviews/MakeABookingTimeSlotsCard.kt:89`; `subviews/MakeABookingSummarySection.kt:54`

**Issue:**
- `today` is computed once in `initialState` and never refreshed, although `now` is refreshed on every action.
  If the screen stays open past midnight, yesterday stays selectable (`isPastDay`), and after a month rollover
  `canGoToPreviousMonth` allows a past month. Both violate D-06.
- Slots are grouped with `ZoneId.systemDefault()` captured at load time (line 249). The composables independently
  evaluate `ZoneId.systemDefault()` as a default parameter on every recomposition. Android updates the default zone
  in-process on `ACTION_TIMEZONE_CHANGED`. After a zone change (travel, or the user changing the setting), chips are
  filed under a date key from the old zone but labelled in the new zone. A chip can then show a time that belongs to a
  different local day than the selected date, and the timezone note disagrees with the grouping.

**Fix:** Make the zone part of the state. Store `zone: ZoneId` in `MakeABookingUiState`, set it in `initialState`,
and pass `state.zone` to every formatter. Re-derive `today`, `zone` and the grouping when the zone or date changes,
e.g. refresh both in each user action alongside `now`, and on `ON_RESUME` call a `vm.refreshClock()` that recomputes
`today` and regroups when `ZoneId.systemDefault()` has changed.

### WR-04: The form stays editable while a booking is in flight, so the Summary can disagree with what is being booked

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingScreen.kt:150-199`; `MakeABookingViewModel.kt:83-133, 196-199`

**Issue:** Only Back and Book Now are gated on `isSubmitting`. The topic picker, calendar, slot chips, meeting-type
radios and notes stay live, and the ViewModel setters don't check `isSubmitting`. `book()` builds the request from a
snapshot, so during the request the Summary can show a different slot, topic or meeting type than the one being
booked. Then:
- on a 409, `clearSlotAndReload` clears the new slot the user picked during the request, not the one that was
  rejected;
- on `MEETING_LINK_UNAVAILABLE`, an In-app/Skype choice made during the request is overwritten.

**Fix:** Ignore selection changes while submitting (`if (_uiState.value.isSubmitting) return` in `selectTopic`,
`selectDate`, `selectSlot`, `selectMeetingType`, `goTo*Month`). Also pass `enabled = !state.isSubmitting` to the
interactive cards, or wrap the content in a `Modifier.semantics { disabled() }` + pointer-input blocker.

## Info

### IN-01: KDoc names the wrong endpoint (`/bookings/confirm`)

**File:** `MakeABookingViewModel.kt:136-147`, `BookingErrors.kt:8,26`, `BookingRepository.kt:31`

**Issue:** The free path calls `POST /bookings/request` (`ApiRequestUrls.BOOKING_REQUEST`). `/bookings/confirm` is the
paid Stripe endpoint with a different strict schema. The comments also say the body uses the
`z.string().datetime` rule "of confirm". This will confuse whoever wires up Phase 7.

**Fix:** Replace `/bookings/confirm` with `/bookings/request` in these comments.

### IN-02: Two different `DEFAULT_DURATION_MINS` constants

**File:** `makeabooking/MakeABookingUiState.kt:12` (public `Int`), `requestsent/BookingRequestSentUiState.kt:7` (private `Long`)

**Issue:** The same business default (30) is defined twice with different types, so the two copies can drift apart.

**Fix:** Reuse the public constant (`DEFAULT_DURATION_MINS.toLong()`).

### IN-03: Unused members

**File:** `MakeABookingUiState.kt:117-118` (`canBook`, only used by tests), `BookingRequestSentUiState.kt:20` (`isFree`, never read), `ExpertAvailabilityData.kt:17` (`clientTimezone`)

**Fix:** Remove them, or mark them as intentionally reserved for Phase 7. `canBook` also duplicates the logic that
`MakeABookingBookButton` builds itself from `disabledReason`/`isSubmitting`.

### IN-04: The Request Sent headline crashes if a translation drops the `%1$s` placeholder

**File:** `requestsent/subviews/BookingRequestSentHeadline.kt:36-42`

**Issue:** `template.indexOf(NAME_MARKER)` returns -1 when the placeholder is missing, and `substring(0, -1)` then
throws `StringIndexOutOfBoundsException`.

**Fix:** `if (index < 0) append(template) else { ... }`.

### IN-05: "Same zone" is decided by comparing IANA ids as strings

**File:** `makeabooking/BookingTimezoneNote.kt:48`

**Issue:** Alias ids (`Asia/Calcutta` vs `Asia/Kolkata`, `US/Eastern` vs `America/New_York`) produce a "Different"
note with identical abbreviations.

**Fix:** Compare `ZoneId.normalized()` and the rules at noon of `on` (`client.rules.getOffset(instant) == expert.rules.getOffset(instant)`) as well as the id, or canonicalise with `TimeZone.getCanonicalID`.

### IN-06: Section errors show "check your connection" for server errors that have no message

**File:** `makeabooking/BookingErrors.kt:68`

**Issue:** An HTTP 5xx with no `message` maps to `login_error_network`, which is misleading for a server fault.

**Fix:** Use `login_error_unknown` for an `Http` with a null `userMessage`.

### IN-07: The "one odd value cannot break decoding" comment is not true for timestamps

**File:** `model/responses/ExpertAvailabilityData.kt:10-12, 43-44`

**Issue:** `InstantAdapter.fromJson` calls `OffsetDateTime.parse`. A single malformed `start_utc` throws, which fails
the whole month as `Decoding`, and that is shown as the network-error copy.

**Fix:** Correct the comment, or make slot decoding lenient (a tolerant adapter that returns null).

### IN-08: Once the user moves to another month, the selected day can wrongly show "No available slots on this day"

**File:** `MakeABookingViewModel.kt:99-104`, `subviews/MakeABookingTimeSlotsCard.kt:132-139`

**Issue:** Moving the month keeps `selectedDate`, but the loaded window only covers displayed month ±1. Two or more
months away, the selected day is outside the window and the card states it has no slots, even if it does. iOS
behaves the same way (parity), but the message is false.

**Fix:** When `selectedDate` is outside the loaded window, show a "Pick a day" prompt instead of the no-slots copy.

### IN-09: A slot that has expired is cleared silently on Book Now

**File:** `MakeABookingViewModel.kt:156-160`

**Issue:** If the selected slot started while the screen was idle, tapping Book Now just deselects it with no
explanation. The dimming state (`now`) is only refreshed on actions.

**Fix:** Show a short dialog or snackbar ("That time has passed. Please pick another.").

### IN-10: "Back to Home" doesn't select the Explore tab (iOS `returnToHome` sets `selectedTab = .explore`)

**File:** `screens/clientmode/home/ClientBookingFlowEntries.kt:47`; `navigation/Navigator.kt:74-79`

**Issue:** `reselect(ExploreRoot)` pops the Explore stack without selecting it. It only works today because the
flow can only start from Explore. Any future entry point from another tab (Search, Chats) would leave Request Sent on
screen.

**Fix:** Use `resetAndSelect(ExploreRoot, setOf(ExploreRoot))`. This mirrors iOS exactly.

### IN-11: Test coverage gaps and a tautological test

**File:** `app/src/test/.../makeabooking/MakeABookingScreenTest.kt:211-216`, `EntrySavedStateHandleTest.kt:37-63`, `navigation/BookingFlowNavigationTest.kt:77-80`

**Issue:**
- `backIsDisabledWhileSubmittingAndEnabledOtherwise` only clicks the app-bar arrow. The system-Back
  `BackHandler(enabled = state.isSubmitting)` is untested, and the "EnabledOtherwise" half of the name isn't
  asserted.
- `EntrySavedStateHandleTest` only proves that a SavedStateHandle can be created, not that values survive entry
  recreation (state save/restore through the Nav3 decorators).
- `requestSentKeyCarriesOnlyTheBookingId` asserts that a data class equals itself.
- No test covers notes focus (see CR-01).

**Fix:** Add a `pressBack()` (or `OnBackPressedDispatcher.onBackPressed()`) test while submitting. Add a
`StateRestorationTester`-based restore test for the entry. Delete the tautological test.

---

_Reviewed: 2026-10-22T19:52:27Z_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
