---
phase: 06-booking-flow-free-bookings
fixed_at: 2026-10-22T21:26:02Z
review_path: .planning/phases/06-booking-flow-free-bookings/06-REVIEW.md
iteration: 3
findings_in_scope: 2
fixed: 2
skipped: 0
status: all_fixed
---

# Phase 6: Code Review Fix Report

**Fixed at:** 2026-10-22T21:26:02Z
**Source review:** .planning/phases/06-booking-flow-free-bookings/06-REVIEW.md (re-review after fix passes, `a153475`)
**Iteration:** 3

This report replaces the previous one. The previous report covered the first review (CR-01, WR-01..04 and
IN-01..11) and is preserved in git at `8f2c919`.

**Summary:**
- Findings in scope: 2 (WR-01, WR-02; the re-review has no Critical findings, and Info IN-01..IN-06 are out of
  scope for `critical_warning`)
- Fixed: 2
- Skipped: 0

**Gate:** `./gradlew testDevDebugUnitTest ktlintCheck assembleDevDebug` (JDK 25 from the Android Studio JBR) passed
after both commits. 1330 unit tests ran, with 0 failures, 0 errors and 0 skipped. ktlint is clean and the dev debug
APK builds.

## Fixed Issues

### WR-01: The completed booking id is not persisted when the success arrives in the background

**Files modified:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/CompletedBookingStore.kt` (new), `app/src/main/java/com/uptrendinvestments/clickmeapp/di/BookingModule.kt` (new), `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingViewModel.kt`, `app/src/test/java/com/uptrendinvestments/clickmeapp/testing/FakeCompletedBookingStore.kt` (new), `app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/DataStoreCompletedBookingStoreTest.kt` (new), `app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/BookingTestFixtures.kt`, `app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingViewModelBookTest.kt`
**Commit:** d9b2300
**Applied fix:** This combines the review's Option A (a durable write when the response arrives) with the attempt
marker from Option B. It does not add the server reconciliation from Option B.
- `book()` writes a random attempt id (`booking_attempt_id`) to the `SavedStateHandle` before it launches the
  request. The user has just tapped Book Now, so the activity is in the foreground and this always happens before
  `onSaveInstanceState`.
- On success the ViewModel first calls `CompletedBookingStore.put(attemptId, bookingId)`, a suspending DataStore
  edit that returns once the record is on disk. Then it writes the saved-state id and updates the UI state as
  before.
- `initialState()` and `init` handle a ViewModel restored with an attempt id but no completed id. The form starts
  locked (`isSubmitting = true`) and `recoverAttempt()` reads the record. If a record is found, the ViewModel goes to
  Request Sent exactly as for a saved-state completed id, with no form loads. If not, the attempt id is dropped, the
  form unlocks and loads, and the restored selection is applied as usual.
- The record is used up once. `consumeCompletedBooking()` removes the attempt id and calls `discard(attemptId)`. The
  discard runs on `@ApplicationScope`, so the write finishes even though the entry's ViewModel is cleared right
  after. A failed booking also removes the attempt id.
- The record cannot come back in a later flow. The store holds a single record, and it only answers for its own
  attempt id. A new booking flow has no attempt id in its saved state, so it never reads the store.
- The record cannot reach another account. It is stored in the existing Hilt-provided session Preferences DataStore
  (`clickme_session`), with no new dependency. `SessionStore.clear()` wipes that file at logout. The store is also
  bound as a `SessionDataCleaner` through `@IntoSet` in `BookingModule`, so it is cleared before login and signup
  as well.
- iOS (`/Users/LeonChen/Documents/ClickMe2026/.../MakeABookingViewModel.swift`) has nothing equivalent, because iOS
  does not restore a booking form after the app is terminated. This is an Android-only robustness addition, and the
  KDoc says so.

**Tests:**
- `aSuccessThatLandsAfterTheStateWasSavedSurvivesProcessDeath` models the failure the review describes. It takes a
  snapshot of the handle while the request is in flight (a copy of its contents, which is what the state save hands
  to the system), then completes the response. It asserts that the snapshot has no `booking_completed_id`, builds a
  fresh ViewModel from the snapshot with the same store, and checks that the ViewModel is locked at once, lands on
  `b1`, loads nothing and sends no second request. Consuming the id clears the record.
- With the `put` line removed, this test fails at the completed-id assertion. That run was done locally and
  reverted.
- `aRequestLostWithTheProcessReopensTheRestoredForm`: when there is no record, the form comes back editable with the
  restored slot.
- `aRecordFromAnotherAttemptNeverResurfaces`: a fresh flow, or a restore with a different attempt id, ignores an old
  record.
- `aFailedBookingPersistsNoCompletedId` now also checks that the attempt id and the record are cleared.
- `DataStoreCompletedBookingStoreTest` checks four things: a record is only returned for its own attempt; a record
  survives a second DataStore opened over the same file after the first one is shut down; `discard` only removes
  the matching attempt; and both the session cleaner and `SessionStore.clear()` (logout) remove the record.

**Residual (not in WR-01's scenario):** suppose the process is killed while the request is still in flight, so the
response is never processed. The restored form reopens, and the booking may still exist on the server. Covering
that needs Option B's server reconciliation, for example looking up upcoming bookings for this expert and start
time. That is a possible follow-up.

### WR-02: `refreshClock()` either strands a passed slot as selected or clears it silently

**Files modified:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingViewModel.kt`, `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingScreen.kt` (comment only), `app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingViewModelBookTest.kt`, `app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingViewModelTest.kt`
**Commit:** 3ac63e5
**Applied fix:**
- `refreshClock()` remembers the held slot. It stamps `now` on the same day, or runs the existing day, month and
  zone handling (moved into `moveClock()`) when the day or zone changed. Then it checks the slot in one place: if the
  held slot is no longer the selected slot and selectable at the new `now`, it calls `clearSlot()`, which also removes
  `KEY_SLOT_START`, and shows the existing IN-09 `make_a_booking_error_slot_passed` dialog.
- This covers all three outcomes in the review: a same-day resume after the slot started (the slot is no longer left
  selected with Book Now disabled), crossing midnight, and a zone change that moves the slot to another local day.
- `book()` now relies on that check. It keeps only `val slot = s.selectedSlot ?: return` and drops its own past-slot
  branch, because the slot has already been cleared and explained.
- `applyAvailability()` also explains a held or restored slot that is dropped because it has already started, for
  example a selection restored after process death. Slots dropped because they are unavailable stay silent, as
  before. The 409 path clears the slot before it reloads, so it never shows two dialogs.

**Tests (MakeABookingViewModelBookTest):** these five fail against the pre-fix ViewModel:
- `aResumeAfterTheSlotStartedClearsItAndSaysWhy` (resume on the same day)
- `aBookAfterMidnightClearsTheSlotAndSaysWhy`
- `aBookAfterAZoneChangeThatMovesTheSlotsDayClearsItAndSaysWhy`
- `aResumeAfterAZoneChangeThatMovesTheSlotsDayClearsItAndSaysWhy`
- `aRestoredSlotThatHasStartedIsDroppedAndExplained`

These cases check that nothing is dropped when it shouldn't be:
- `aResumeBeforeTheSlotStartsKeepsItWithoutADialog`
- `aZoneChangeThatKeepsTheSlotsDayKeepsItWithoutADialog`

In `MakeABookingViewModelTest`:
- `aZoneChangeRegroupsTheLoadedSlotsWithoutARequest` now also asserts the dialog.
- `restoredSlotThatIsNoLongerAvailableIsDropped` now asserts that no dialog appears when the slot is unavailable
  rather than past.

**Requires human verification (logic change):**
1. **Copy on a zone change.** As instructed, a zone change that moves the slot to another day shows the existing
   "That time has passed. Please pick another." That is not literally true in this case, because the time has not
   passed. Decide whether a separate string is worth adding.
2. **Dialog before a new selection.** All clock-driven drops are explained, including when the user's next action
   (tap another chip, day or month) is what triggers `refreshClock()`. In that case the dialog appears just before
   the new selection is applied. This is deliberate: it keeps one place that decides, and never leaves a dropped slot
   unexplained.

---

_Fixed: 2026-10-22T21:26:02Z_
_Fixer: Claude (gsd-code-fixer)_
_Iteration: 3_
