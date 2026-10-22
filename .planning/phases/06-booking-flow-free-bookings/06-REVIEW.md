---
phase: 06-booking-flow-free-bookings
reviewed: 2026-10-22T21:33:17Z
depth: standard
files_reviewed: 67
files_reviewed_list:
  - app/src/main/java/com/uptrendinvestments/clickmeapp/di/BookingModule.kt
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
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/CompletedBookingStore.kt
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
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/DataStoreCompletedBookingStoreTest.kt
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
  - app/src/test/java/com/uptrendinvestments/clickmeapp/testing/FakeCompletedBookingStore.kt
findings:
  critical: 0
  warning: 2
  info: 10
  total: 12
status: issues_found
---

# Phase 6: Code Review Report (third review, after the review-2 fixes d9b2300 and 3ac63e5)

**Reviewed:** 2026-10-22T21:33:17Z
**Depth:** standard
**Files Reviewed:** 67
**Status:** issues_found

## Narrative Findings (AI reviewer)

## Summary

This review looks at the two review-2 fixes in `a153475..HEAD`:

- **WR-01** (`d9b2300`): the attempt id in the `SavedStateHandle`, `DataStoreCompletedBookingStore` and the `BookingModule` binding.
- **WR-02** (`3ac63e5`): the slot check in `refreshClock()` and the explanation in `applyAvailability()`.

For the 63 files that did not change, the review only re-checked the six open Info items from review 2.

**WR-01: the mechanism is sound for the scenario it targets.** Each step was traced:

- **Before the save:** the attempt id is written synchronously inside `book()`, from a user tap. The activity is in the foreground at that point, so the write always comes before `onSaveInstanceState`.
- **When the response lands:** `put` finishes before the saved-state write and the UI update.
- **When the screen does not run:** `collectAsStateWithLifecycle` stops collecting below STARTED, so `consumeCompletedBooking` and `discard` never run while the app is in the background.
- **Discard:** it runs inside `edit` and is conditional on the attempt id, so it cannot remove a newer attempt's record.
- **Hilt:** the `@Singleton` class is bound through two `@Binds` methods, so both bindings resolve to the same instance.
- **DataStore instance:** an unqualified `DataStore<Preferences>` is the `clickme_session` instance. The category cache is the only other `DataStore<Preferences>`, and it is qualified.
- **Account boundaries:** logout (`clearSessionData` and then `SessionStore.clear()`) wipes the record. Login and signup also run the cleaner first. `restore()` does not clear it, which is correct: the record has to survive the cold start it exists for.

**WR-01 is still incomplete in one case**, reported below as WR-01. The fix report already lists it as residual. The ViewModel now knows that a request was sent and that its outcome is unknown, but it still reopens the form as if nothing had been sent.

**WR-02: the slot checks are correct.** No path shows two dialogs, and none shows the dialog by mistake:

- **First resume and restore:** on the first resume `held` is null, because a restored slot sits in `pendingSlotMillis`, not in `selectedSlotStart`. While a booking is in flight or being recovered, `refreshClock()` returns early.
- **409:** the 409 path clears the slot before it reloads, so `applyAvailability` does not show the dialog a second time.
- **Zone change:** when a zone change regroups the slots, both `applyAvailability` and `refreshClock` may set the same `dialogError` value. That is still a single dialog.

The problem is *where* the dialog now appears. `refreshClock()` is also the first call in `selectDate`, `goToPrevious/NextMonth` and `selectSlot`. A modal dialog titled "Couldn't book" now interrupts ordinary taps, even when the tap would have replaced the slot anyway (WR-02).

**Review-2 Info items:** IN-01 to IN-06 are all still present, unchanged, and are carried forward below. None of the review-2 fixes has regressed.

There are no Critical findings.

## Warnings

### WR-01: A restore whose attempt has no record silently reopens an editable form with the in-flight slot preselected

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingViewModel.kt:108-120` (`recoverAttempt`), `:237` (`put`); `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/CompletedBookingStore.kt:61-83`
**Issue:** If `KEY_ATTEMPT_ID` is in the restored state but `completedBookings.get()` returns null, the ViewModel throws the attempt id away and loads the form. The restored slot, topic, meeting type and notes are applied as if no request had ever been sent. There are three ways to get here, and in each one the booking may already exist on the server:

1. **Process killed with the request in flight.** The response was never processed, but the server may have created the booking. The fix report lists this as a residual.
2. **The write failed.** `put()` swallows `IOException`, so a successful booking whose disk write failed produces no record.
3. **The read failed.** `get()` maps an `IOException` to null, so a read failure looks exactly like "no record".

In all three cases the user sees a fully filled form with Book Now enabled. Two things can go wrong from there:

- **Same slot:** the server answers 409 `SLOT_UNAVAILABLE`. The user's own pending booking now holds that slot, but the dialog says it was taken.
- **Different slot:** the free path has no idempotency key (`requestSchema` is `.strict()` and has no attempt field), so a second pending request to the same expert is created.

Before this fix the ViewModel could not tell this case apart from a clean restore. Now it can, through the attempt id and the missing record. So this is an incomplete fix, not a pre-existing limitation.
**Fix:** Treat "attempt present, no record" as an unknown outcome, not as "never sent":
```kotlin
// in book(): also persist what was sent, before launching
savedStateHandle[KEY_ATTEMPT_ID] = attemptId
savedStateHandle[KEY_ATTEMPT_START] = slot.start.toEpochMilli()

// recoverAttempt(): when get() returns null
val start = savedStateHandle.get<Long>(KEY_ATTEMPT_START)?.let(Instant::ofEpochMilli)
val existing = start?.let { runCatching {
    clientBookings.list(ClientBookingsRepository.TYPE_UPCOMING).bookings
        .firstOrNull { it.expert.id == expertId && it.startTime == start }   // ClientBookingItem fields
}.getOrNull() }
if (existing != null) { /* same as a found record: go to Request Sent with existing.bookingId */ }
else {
    savedStateHandle.remove<String>(KEY_ATTEMPT_ID)
    _uiState.update { it.copy(isSubmitting = false, dialogError = FieldError.Res(R.string.make_a_booking_error_unconfirmed)) }
    loadForm()
}
```
The `dialogError` would hold a new string such as "We couldn't confirm your last request. Check My Bookings before booking again." That notice is the minimum fix. Also make `get()` return a tri-state result (`Found`, `Absent`, `Unreadable`), so that a read failure is not treated as proof that no booking exists.

### WR-02: Ordinary taps now open a "Couldn't book" dialog, including when the tap replaces the slot anyway

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingViewModel.kt:138, 145, 159, 169` (callers) and `:399-405` (`refreshClock` slot check); `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingScreen.kt:244-249`; `app/src/main/res/values/strings_make_a_booking.xml:65,67`
**Issue:** WR-02 moved the "clear and explain" step into `refreshClock()`. But `selectDate`, `goToPreviousMonth`, `goToNextMonth` and `selectSlot` call `refreshClock()` first. Suppose the held slot has started while the screen sat idle:

- **`selectDate(d)` and the month arrows:** `refreshClock()` clears the slot and opens the modal dialog. Then the date or month change runs, and it clears the slot anyway (D-07). The user picked a different day and gets an error for a selection they were already leaving.
- **`selectSlot(B)`:** the dialog says "That time has passed. Please pick another." while B, the slot the user just picked, is shown as selected underneath it.
- **Every trigger, including resume:** the dialog is rendered with the `make_a_booking_error_title` title, "Couldn't book". On a resume or a calendar tap no booking was attempted, so the title says something that did not happen.

The fix report lists this as "Dialog before a new selection … deliberate". This review disagrees. The tap already answers "pick another", so the dialog is a spurious interruption with an incorrect title. Before this fix those paths cleared the slot silently, which was the right behaviour for them.
**Fix:** Have `refreshClock` return whether it dropped the slot, and let only resume and `book()` explain the drop:
```kotlin
/** Returns true when a held slot was dropped because time moved past it. */
private fun syncClock(): Boolean { /* current refreshClock body without explainSlotPassed() */ }

fun refreshClock() { if (syncClock()) explainSlotPassed() }   // ON_RESUME
fun book() { if (_uiState.value.isSubmitting) return; if (syncClock()) { explainSlotPassed(); return } ... }
fun selectDate(date: LocalDate) { if (_uiState.value.isSubmitting) return; syncClock(); ... }   // silent
// same for goToPreviousMonth / goToNextMonth / selectSlot
```
Also give this notice a title that does not say a booking failed. Either add a title to the dialog state (for example `dialogTitle: Int`), or use a separate `notice` field with its own title, such as "Time slot unavailable".

## Info

### IN-01: `replaceTop` replaces the top of whichever tab is selected, not the booking entry that completed (carried forward from review 2, IN-01)

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/navigation/Navigator.kt:88-94`; `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/home/ClientBookingFlowEntries.kt:41`
**Issue:** Unchanged. `onBooked` calls `navigator.replaceTop(RequestSentKey(id))`, which acts on `currentStack()`. That is safe today only because the bottom bar is hidden on `BookingKey`. Once Phase 12/13 push routing (`navigateInTab`) can switch tabs while the form is open, the replace would hit another tab's stack. It would then either remove an unrelated screen or crash in `check(stack.size > 1)`.
**Fix:** Target the replacement at the entry: `replace(from = key, to = RequestSentKey(id))`. It should find the stack that contains `key` and do nothing if `key` is gone.

### IN-02: "Back to Home" leaves the flow stacked on another tab, although the KDoc says that case is handled (carried forward from review 2, IN-02)

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/home/ClientBookingFlowEntries.kt:21-22, 48`; `app/src/test/java/com/uptrendinvestments/clickmeapp/navigation/BookingFlowNavigationTest.kt` (`backToHomeFromAnotherTabSelectsExplore`)
**Issue:** Unchanged. `resetAndSelect(ExploreRoot, setOf(ExploreRoot))` resets only Explore. The KDoc says the flow lands correctly "even if a future entry point starts the flow from another tab". In that case the other tab keeps a stale Expert Profile → Request Sent on its stack. The test asserts only the Explore stack.
**Fix:** Either reword the KDoc to "resets Explore only (iOS `returnToHome` parity)", or also reset the tab that owns the Request Sent entry and assert that tab's stack in the test.

### IN-03: The in-flight lock does not cover background availability loads (carried forward from review 2, IN-03)

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingViewModel.kt:44-45, 207-244, 261-265, 341-374`
**Issue:** Unchanged. After a 409, the reload runs with `keepVisible = true` while the form is editable again. If the user re-selects a slot that is still visible and taps Book before the reload returns, `applyAvailability` can set `selectedSlotStart = null` while the request is in flight. That contradicts the class KDoc ("the request, the Summary and the 409/422 side effects all refer to the same slot"). Since WR-02, `applyAvailability` can also call `explainSlotPassed()` during that window. In practice that needs the slot to start during the reload, so it is unlikely.
**Fix:** In `applyAvailability`, keep `selectedSlotStart` unchanged and show no dialog while `isSubmitting` (store only the grouped slots). Or cancel `availabilityJob` when `book()` starts.

### IN-04: `isInLoadedWindow` treats the outer edge days of the window as fully loaded (carried forward from review 2, IN-04)

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/BookingCalendar.kt:36-46`
**Issue:** Unchanged. The three loaded months are expert-local, but the slots are regrouped by the viewer's local date. So the viewer's first day of month−1 and last day of month+1 can be missing slots from months that were never loaded. Those days still report `isSelectedDateLoaded == true` and can show "No available slots".
**Fix:** Treat the first and last day of the window as not loaded (shrink the window by one day on each side), or document the limitation in the KDoc.

### IN-05: The headline marker is replaced only once (carried forward from review 2, IN-05)

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/requestsent/subviews/BookingRequestSentHeadline.kt:65-79`
**Issue:** Unchanged. `requestSentBody` replaces only the first `indexOf(NAME_MARKER)`. A translation that uses `%1$s` twice would show a raw U+0000 where the second name belongs.
**Fix:** Split on `NAME_MARKER` and append the styled name between each pair of parts.

### IN-06: Formatter composables still default `zone` to `ZoneId.systemDefault()`, against the UiState rule (carried forward from review 2, IN-06)

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/subviews/MakeABookingTimeSlotsCard.kt:96`; `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/subviews/MakeABookingSummarySection.kt:54`
**Issue:** Unchanged. The `MakeABookingUiState.zone` KDoc (`MakeABookingUiState.kt:67`) says every formatter must use the state's zone and never `systemDefault()`. Both composables still default to `systemDefault()`, so a future call site that leaves out `zone` would bring the WR-03 mismatch back.
**Fix:** Make `zone` a required parameter. The previews can pass `ZoneId.of("America/Toronto")`.

### IN-07: A zone change that moves the slot to another day is reported as "That time has passed"

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingViewModel.kt:399-405`; `app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingViewModelTest.kt:525`; `MakeABookingViewModelBookTest.kt` (`aBookAfterAZoneChangeThatMovesTheSlotsDayClearsItAndSaysWhy`, `aResumeAfterAZoneChangeThatMovesTheSlotsDayClearsItAndSaysWhy`)
**Issue:** The `!stillSelectable` branch shows `make_a_booking_error_slot_passed` for every kind of drop. When a zone change moves a slot that is still in the future to another local day, the message is false: the time has not passed. Two tests now assert this false message, so they will resist a later fix. The fixer flagged this for human review.
**Fix:** Either use a separate string ("Your time zone changed. Please pick the time again."), or skip the drop altogether: set `selectedDate` to `held.atZone(zone).toLocalDate()` and keep the slot, since it is still bookable.

### IN-08: The completed-booking record shares `SessionStore`'s file, and the `SessionStore` KDoc no longer describes what that file holds

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/CompletedBookingStore.kt:54-59`; `app/src/main/java/com/uptrendinvestments/clickmeapp/managers/SessionStore.kt:16-22, 68-71`; `app/src/main/java/com/uptrendinvestments/clickmeapp/di/SessionModule.kt:28-36`
**Issue:** Functionally this works. `SessionStore.clear()` wipes the whole file, and the cleaner is registered. The design has two weak points, though:

- **Implicit file choice.** The session file is reached only because it is the unqualified `DataStore<Preferences>` binding. Any future class that injects an unqualified `DataStore<Preferences>` would silently write into the session file too. The category cache uses `@CategoryCache`; the session store has no qualifier.
- **Outdated KDoc.** `SessionStore`'s KDoc still says the file holds the profile, mode, onboarding flag and setup deferral, and that `clear()` removes "the profile, mode, onboarding flag and setup deferral". The file now has a second owner whose keys it does not list.

**Fix:** Add a `@SessionPreferences` qualifier to the session DataStore and use it in both `SessionStore` and `DataStoreCompletedBookingStore`. Update the `SessionStore` KDoc to say that `clear()` also removes the booking record (WR-01).

### IN-09: `KEY_ATTEMPT_ID` / "attempt id" clashes with the Phase 7 booking attempt id

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingViewModel.kt:37-38, 218-219, 510`
**Issue:** The class KDoc (lines 37-38) already reserves "booking attempt id" for the iOS/Phase 7 concept. That id is regenerated on a topic change, kept across retries, and used as the backend's Stripe idempotency key (`booking.schema.js:45-56`). The new `"booking_attempt_id"` key has a different lifetime: a new one on every tap, and it is removed on failure. If Phase 7 reuses the name or the key, retries would get a fresh idempotency key on every tap, which defeats the idempotency.
**Fix:** Rename it now, for example `KEY_INFLIGHT_REQUEST_ID = "booking_inflight_request_id"`, and update the `CompletedBookingStore` parameter names to match.

### IN-10: `explainSlotPassed()` overwrites any dialog already on screen

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingViewModel.kt:435-437`
**Issue:** `explainSlotPassed` replaces `dialogError` without checking whether a dialog is already showing. Example: the user leaves the app while the 422 `MEETING_LINK_UNAVAILABLE` dialog is open ("…switched to In-app Voice"). The held slot starts while the app is away. On resume, that dialog is replaced by "That time has passed". The user is never told that the meeting type was reset.
**Fix:** Only set the slot-passed message when `dialogError == null`, or queue dialogs, so an unread failure message is not lost.

---

_Reviewed: 2026-10-22T21:33:17Z_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
