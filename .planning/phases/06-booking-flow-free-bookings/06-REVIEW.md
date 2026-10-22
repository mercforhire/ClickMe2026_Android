---
phase: 06-booking-flow-free-bookings
reviewed: 2026-10-22T21:06:45Z
depth: standard
files_reviewed: 63
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
  - app/src/test/java/com/uptrendinvestments/clickmeapp/navigation/BookingFlowNavigationTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/navigation/NavigatorTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/network/ApiRequestUrlsTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/network/ClickMeApiTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/network/ResponseDecodingTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/HomeShellsTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/BookingCalendarTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/BookingErrorsTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/BookingFormattingTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/BookingSlotsTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/BookingTestFixtures.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/BookingTimezoneNoteTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/EntrySavedStateHandleTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingCalendarSlotsTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingFormCardsTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingScreenTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingTopicCardsTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingUiStateTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingViewModelBookTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingViewModelTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/requestsent/BookingRequestSentScreenTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/requestsent/BookingRequestSentViewModelTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/testing/FakeClickMeApi.kt
findings:
  critical: 0
  warning: 2
  info: 6
  total: 8
status: issues_found
---

# Phase 6: Code Review Report (re-review after fix passes)

**Reviewed:** 2026-10-22T21:06:45Z
**Depth:** standard
**Files Reviewed:** 63 (38 main source and resource files read in full; the 25 test files were read only to check the new fix tests and whether they are reliable)
**Status:** issues_found

## Summary

This is a re-review after the fix commits `6166b0b..62372f9`, which closed CR-01, WR-01..04 and IN-01..11 from the
first review (`3b635cd`). The fix code got the most attention: the post-success lock, the persisted completed
booking id, `refreshClock()` and the zone in state, the in-flight input lock, the drag-only focus clearing, and Back
to Home.

How each earlier fix holds up:

| Prior finding | Verdict |
|---|---|
| CR-01 drag-only focus clearing | Fixed. `collectIsDraggedAsState` on the scroll's interaction source ignores bring-into-view and `imePadding` scrolls. The new Robolectric tests cover both directions. |
| WR-01 post-success lock | Fixed. `isSubmitting` stays true after success and is never reset, `BackHandler`/`backEnabled` follow it, and a restored completed id starts locked. |
| WR-02 completed id across process death | **Incomplete. See WR-01 below.** The id is written to `SavedStateHandle` after the response arrives. In the scenario the finding described (the app is already in the background), that write happens after `onSaveInstanceState`, so it is never persisted. |
| WR-03 refreshClock / zone in state | The zone and `today` logic is correct. **It now conflicts with the IN-09 fix (see WR-02 below).** |
| WR-04 in-flight input lock | Fixed for user input. A small gap remains for background data loads (IN-03). |
| IN-10 Back to Home / Explore | Fixed, and matches iOS `returnToHome`. One documentation and edge-case note (IN-02). |
| IN-01..09, IN-11 | Fixed. IN-04's marker approach has one leftover edge case (IN-05). |

No new security issues: server text is still rendered only as plain `Text`, there are no secrets, and the request
body is built from typed fields.

## Warnings

### WR-01: The completed booking id is not persisted when the success arrives in the background, so the WR-02 fix does not cover its own scenario (prior WR-02 incomplete)

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingViewModel.kt:205-212`, `:396-412`
**Issue:** `SavedStateHandle` contents reach the system only when `SavedStateRegistry.performSave` runs from
`ComponentActivity.onSaveInstanceState`. On API 28+ that call happens right after `onStop`, and the bundle is handed
to the system at that moment. Values written to the handle later stay in memory until the next save, and a stopped
activity does not save again.

The prior WR-02 scenario was: tap Book Now, leave the app while the request is in flight, the response arrives in the
background, then the process is killed. In that order of events:

1. Book Now → `isSubmitting = true` (not persisted, which is fine).
2. Home → `onStop` → `onSaveInstanceState`. The bundle holds the topic, date and slot, but no `booking_completed_id`.
3. The response arrives → `savedStateHandle[KEY_COMPLETED_BOOKING_ID] = result.bookingId` (memory only).
4. The process is killed → the restore uses the bundle from step 2.

The restored ViewModel has no completed id and `isSubmitting = false`, so it reloads the form. The restored slot is
now `available: false` because it is the user's own booking, so `applyAvailability` drops it silently. The user is
back on an editable form with no sign that the booking exists, and may book a second slot. The fix only works if the
response lands before step 2, which is the case where nothing was lost in the first place.

The new tests can't catch this. `aCompletedBookingSurvivesProcessDeathUntilConsumed` reuses the same in-memory
`SavedStateHandle` object, and `entrySavedStateHandleSurvivesRecreationWithFreshViewModels` writes the value before
`emulateSavedInstanceStateRestore()`. Neither models a write after the save.

**Fix:** Record the result where it survives process death no matter when it is written. The process is still alive
when the response arrives, so a synchronous write to a small persistent store works. Alternatively, persist an
"attempt in flight" marker before the request is sent, which is always before `onSaveInstanceState`, and check it
against the server on restore:

```kotlin
// Option A: durable store written at response time (survives later process death)
val result = repository.requestBooking(body)
completedBookingStore.put(expertId, result.bookingId)   // SharedPreferences.commit() / DataStore
savedStateHandle[KEY_COMPLETED_BOOKING_ID] = result.bookingId
_uiState.update { it.copy(completedBookingId = result.bookingId) }
// initialState(): completedBookingId = savedStateHandle[...] ?: completedBookingStore.take(expertId)

// Option B: marker persisted before sending, reconciled on restore
savedStateHandle[KEY_ATTEMPT] = "${topic.id}|${slot.start.toEpochMilli()}"   // before launch {}
// init: if KEY_ATTEMPT is present and there is no completed id, look up
// ClientBookingsRepository.list(TYPE_UPCOMING) for this expert and start time; navigate if found, else clear it
```

Add a test that captures the handle's saved bundle before the response completes (or simulates the save with
`SavedStateHandle.savedStateProvider().saveState()` at that point) and restores from that snapshot.

### WR-02: `refreshClock()` either strands a passed slot as selected or clears it silently, so the IN-09 "time has passed" dialog is unreachable in most real cases (IN-09 fix regressed by the WR-03 fix)

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingViewModel.kt:177-193, 350-377`; `MakeABookingScreen.kt:73`; `MakeABookingUiState.kt:117-123`
**Issue:** IN-09 was fixed by showing `make_a_booking_error_slot_passed` when `book()` finds that the selected slot
has started. `book()` now calls `refreshClock()` first, and `refreshClock()` runs on every `ON_RESUME`. That creates
three inconsistent outcomes:

1. **Same day, after a resume (the most common case).** `refreshClock()` only stamps `now` (lines 356-358). It does
   not reconcile `selectedSlotStart`. The chip still renders as selected: `isSelected` is true, so it gets the
   primary fill, even though it is no longer enabled. The Summary still shows the time. `bookDisabledReason` becomes
   `NoSlot` because `!slot.isSelectable(now)`, so Book Now is disabled and announces "Choose a time slot to
   continue". The user cannot tap Book, so the IN-09 dialog never appears. The stale slot also stays in
   `KEY_SLOT_START`.
2. **Crossing midnight.** `refreshClock()` moves the past `selectedDate` to today and calls `clearSlot()` (lines
   366-369). When this runs inside `book()`, `s.selectedSlot ?: return` (line 183) exits silently. Book Now does
   nothing, the day jumps and the selection vanishes with no explanation, which is exactly what IN-09 set out to
   remove.
3. **Zone change.** `applyAvailability(groupSlots(loadedDays, zone), …)` drops a slot that moved to another local day
   (this is asserted in `aZoneChangeRegroupsTheLoadedSlotsWithoutARequest`). Inside `book()` this is another silent
   no-op.

The only case where the dialog shows is the one the test uses: the clock passes the slot with no resume, and the user
taps Book.

**Fix:** Reconcile the slot in one place whenever `now`, `today` or `zone` moves, and tell the user each time a
selection is dropped because time moved:

```kotlin
fun refreshClock() {
    ...
    val hadSlot = _uiState.value.selectedSlotStart
    // existing zone/today/month handling ...
    val s2 = _uiState.value
    val stillOk = s2.selectedSlot?.isSelectable(s2.now) == true
    if (hadSlot != null && !stillOk) {
        clearSlot()
        _uiState.update { it.copy(dialogError = FieldError.Res(R.string.make_a_booking_error_slot_passed)) }
    }
}
```

Then `book()` only needs `val slot = s.selectedSlot ?: return`, because refreshClock already explained a dropped
slot. Add tests for: a resume after the slot started, a `book()` after midnight, and a `book()` after a zone change
that moves the slot's day.

## Info

### IN-01: `replaceTop` replaces whatever is on top of the currently selected tab, not the booking entry that completed

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/navigation/Navigator.kt:88-94`; `screens/clientmode/home/ClientBookingFlowEntries.kt:41`
**Issue:** `onBooked` calls `navigator.replaceTop(RequestSentKey(id))`, which works on `currentStack()`. That is
correct today because the bottom bar is hidden on `BookingKey`. Once Phase 12/13 `navigateInTab` (push routing) can
switch tabs while the form is open, a success that lands during the outgoing transition would act on the new tab's
stack. It would either replace an unrelated screen or crash in `check(stack.size > 1)` on a root-only tab.
**Fix:** Give the entry its own key and make the replacement targeted, e.g. `replace(from = key, to = RequestSentKey(id))`,
which finds the stack that contains `key` and is a no-op if the key is gone.

### IN-02: "Back to Home" leaves the flow stacked on another tab, although the KDoc says it handles that case

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/home/ClientBookingFlowEntries.kt:21-22, 48`; `app/src/test/.../navigation/BookingFlowNavigationTest.kt` (`backToHomeFromAnotherTabSelectsExplore`)
**Issue:** `resetAndSelect(ExploreRoot, setOf(ExploreRoot))` only resets Explore. That matches iOS `returnToHome`.
But if a future entry point starts the flow from another tab, which the KDoc explicitly says is covered, that tab
keeps Expert Profile → Request Sent on its stack, and the user meets a stale Request Sent the next time they open
the tab. The new test asserts only the Explore stack, so it passes with the stale entry still in place.
**Fix:** Either change the KDoc to say only Explore is reset (iOS parity), or reset the tab that owns the Request
Sent entry as well, and assert that tab's stack in the test.

### IN-03: The in-flight lock does not cover background availability loads

**File:** `MakeABookingViewModel.kt:43-44, 233-237, 309-332`
**Issue:** After a 409, `loadAvailability(..., keepVisible = true)` runs while the form is editable again. If the
user picks a slot that is still visible and taps Book before that reload returns, `applyAvailability` can set
`selectedSlotStart = null` mid-flight. The Summary and chips then change while `isSubmitting` is true. This
contradicts the class KDoc ("the request, the Summary and the 409/422 side effects all refer to the same slot"). The
impact is cosmetic, because the request body was captured before launch.
**Fix:** In `applyAvailability`, keep `selectedSlotStart` unchanged while `isSubmitting` (store the grouped slots
only), or cancel `availabilityJob` when `book()` starts.

### IN-04: `isInLoadedWindow` treats the outer edge days of the window as fully loaded

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/BookingCalendar.kt:36-46`
**Issue:** The three loaded months are expert-local, and slots are regrouped by viewer-local date. When the viewer
is behind the expert, the last viewer day of month+1 can include slots from the expert's month+2, which was never
loaded. When the viewer is ahead, the same happens on the first day of month−1. Those days show a partial list, or
"No available slots", even though `isSelectedDateLoaded` is true. This is only reachable after a day is picked and
the month is then moved by one, so it is rare.
**Fix:** Treat the first and last day of the window as unknown (shrink the window by one day on each side), or
mention the limitation in the KDoc.

### IN-05: The headline marker is replaced only once

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/requestsent/subviews/BookingRequestSentHeadline.kt:65-79`
**Issue:** A translation that uses `%1$s` twice ends up with a raw U+0000 where the second name should be, because
`requestSentBody` replaces only the first `indexOf(NAME_MARKER)`.
**Fix:** Loop over every occurrence of the marker, or split on `NAME_MARKER` and add a styled name between each pair
of parts.

### IN-06: Formatter composables still default `zone` to `ZoneId.systemDefault()`, against the new UiState rule

**File:** `subviews/MakeABookingTimeSlotsCard.kt:96`, `subviews/MakeABookingSummarySection.kt:54`
**Issue:** The `MakeABookingUiState.zone` KDoc says every formatter must use the state zone and never
`ZoneId.systemDefault()` directly. Both cards still have a `systemDefault()` default, so a future call site that
leaves out `zone` would quietly bring back the WR-03 mismatch. The screen passes `state.zone` today.
**Fix:** Make `zone` a required parameter (previews can pass `ZoneId.of("America/Toronto")`).

---

_Reviewed: 2026-10-22T21:06:45Z_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
