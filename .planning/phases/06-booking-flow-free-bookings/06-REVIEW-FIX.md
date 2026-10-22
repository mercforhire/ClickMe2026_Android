---
phase: 06-booking-flow-free-bookings
fixed_at: 2026-10-22T21:51:47Z
review_path: .planning/phases/06-booking-flow-free-bookings/06-REVIEW.md
iteration: 4
findings_in_scope: 2
fixed: 2
skipped: 0
status: all_fixed
---

# Phase 6: Code Review Fix Report

**Fixed at:** 2026-10-22T21:51:47Z
**Source review:** .planning/phases/06-booking-flow-free-bookings/06-REVIEW.md (third review, `59917ea`)
**Iteration:** 4

This report replaces the previous one. Earlier reports are preserved in git: the first-review report at `8f2c919`
and the iteration-3 report (review-2 WR-01/WR-02) at `ef0cd10`.

**Summary:**
- Findings in scope: 2 (WR-01, WR-02; no Critical findings; Info IN-01..IN-10 are out of scope for
  `critical_warning`)
- Fixed: 2
- Skipped: 0

**Gate:** `./gradlew testDevDebugUnitTest ktlintCheck assembleDevDebug` (JDK 25 from the Android Studio JBR) passed
after both commits: 1340 unit tests, 0 failures, 0 errors. ktlint is clean and the dev debug APK builds.

## Fixed Issues

### WR-01: A restore whose attempt has no record silently reopens an editable form with the in-flight slot preselected

**Files modified:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/CompletedBookingStore.kt`, `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingViewModel.kt`, `app/src/main/res/values/strings_make_a_booking.xml`, `app/src/test/java/com/uptrendinvestments/clickmeapp/testing/FakeCompletedBookingStore.kt`, `app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/DataStoreCompletedBookingStoreTest.kt`, `app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingViewModelBookTest.kt`
**Commit:** 0f04b42
**Applied fix:** This follows the agreed approach. There is no server lookup of upcoming bookings.
- **`get()` result:** `CompletedBookingStore.get()` now returns a sealed `CompletedBookingLookup`: `Found(bookingId)`,
  `Absent` or `Unreadable`. A DataStore `IOException` on read becomes `Unreadable`, so a failed read is no longer
  reported as "no record".
- **Restore without a record:** in `recoverAttempt()`, `Found` still goes to Request Sent. `Absent` and `Unreadable`
  no longer reopen a ready-to-book form silently. The ViewModel:
  - removes `KEY_ATTEMPT_ID`, so the notice shows only once;
  - unlocks the form and loads it;
  - sets `dialogError` to the new string `make_a_booking_error_unconfirmed`: "We couldn't confirm your last booking
    request. It may already have been sent, so please check My Bookings before booking again."

  The notice uses the existing `AuthErrorDialog`. Once it is dismissed the form works normally, and the restored
  selection is kept.
- **Failed `put()`:** it stays best-effort. The comment now says that a restore with no record is covered by the
  notice.
- **Supporting guard:** `explainSlotPassed()` no longer replaces a dialog that is already on screen. Without this
  guard, the notice could be overwritten straight away by "That time has passed" when the restored slot had started
  in the meantime. This guard also resolves **IN-10**.

**Tests:**
- `aRequestLostWithTheProcessReopensTheFormBehindACheckYourBookingsNotice` replaces
  `aRequestLostWithTheProcessReopensTheRestoredForm`. After the process dies mid-request it checks that the notice
  shows and the attempt id is cleared, and that a second restore shows no notice. After dismissal, booking works.
- `anUnreadableRecordIsNotTreatedAsNoBooking`: a store read failure produces the notice.
- `aRestoredSlotThatHasStartedDoesNotReplaceTheCheckYourBookingsNotice`: the notice survives the slot-passed path.
- `aRecordFromAnotherAttemptNeverResurfaces`: a restore with a different attempt id does not show the other record,
  but now shows the notice. A fresh flow shows no dialog.
- `DataStoreCompletedBookingStoreTest`: the existing tests now assert `Found` and `Absent`. A new test,
  `aReadFailureIsUnreadableNotAbsent`, uses a DataStore that throws on read and write. It checks that a failed read
  returns `Unreadable` and that a failed `put` does not throw.

**Requires human verification:**
1. **Notice title.** The notice keeps the "Couldn't book" title, following the instruction to reuse the existing
   error dialog and add one string. Paired with "We couldn't confirm your last booking request…" it reads
   consistently, but a neutral title such as "Check My Bookings" is an option.
2. **False positive after a background failure.** The attempt id is also still in the saved state when the request
   *failed* after the activity saved its state, because the failure removes it only from the live handle. A restore
   then shows the notice even though nothing was booked. The notice says "may", so this errs on the safe side.

### WR-02: Ordinary taps now open a "Couldn't book" dialog, including when the tap replaces the slot anyway

**Files modified:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingViewModel.kt`, `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingUiState.kt`, `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingScreen.kt`, `app/src/main/res/values/strings_make_a_booking.xml`, `app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingViewModelBookTest.kt`, `app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingViewModelTest.kt`, `app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingScreenTest.kt`
**Commit:** 7bf8011
**Applied fix:**
- **Silent clock sync:** the old `refreshClock()` body is now `private fun syncClock(): Boolean`. It clears a held
  slot that is no longer selectable and returns true, but shows nothing.
- **Callers:**
  - `refreshClock()` (ON_RESUME) is now `if (syncClock()) explainSlotPassed()`.
  - `book()` explains the drop and returns.
  - `selectDate`, `goToPreviousMonth`, `goToNextMonth` and `selectSlot` call `syncClock()` and ignore the result, so
    a tap clears a passed slot silently.
- **No second path to the dialog:** `applyAvailability()` has a new `explainPassedSlot` parameter. It is true for
  network loads, which covers the restored-slot-after-process-death case. It is false for the synchronous zone regroup
  inside `syncClock()`, where the caller decides. Without this, a tap after a zone change could still show the dialog
  through the regroup.
- **Neutral title:** `MakeABookingUiState` has a new field, `@StringRes dialogTitle`, which defaults to
  `make_a_booking_error_title`. The screen renders `stringResource(state.dialogTitle)`.
  - The slot-passed notice uses the new string `make_a_booking_notice_slot_passed_title`, "Time slot unavailable".
  - Booking failures and the WR-01 notice set "Couldn't book" explicitly.
  - `dismissError()` resets the title to the default.

**Tests:**
- **Tap tests (new):** these fail against the pre-fix code.
  - `aSlotTapAfterTheHeldSlotStartedReplacesItSilently`
  - `aDateTapAfterTheHeldSlotStartedClearsItSilently`
  - `aMonthTapAfterTheHeldSlotStartedClearsItSilently`
  - `aTapThatRegroupsAfterAZoneChangeDropsAPassedSlotSilently`
- **Resume and title tests (new):**
  - `aResumeAfterAZoneChangeDropsAPassedSlotWithOneNotice`
  - `aBookingFailureIsTitledCouldNotBookEvenAfterANotice`: after a notice is dismissed, a later booking failure is
    titled "Couldn't book" again.
- **Existing tests:** the resume, Book Now and restore tests now also assert the neutral title (via
  `assertSlotPassedNotice`), and so does `MakeABookingViewModelTest.aZoneChangeRegroupsTheLoadedSlotsWithoutARequest`.
  No existing test asserted a message on `selectDate` or `selectSlot`, so the new tap tests cover those paths.
- **Screen test:** `MakeABookingScreenTest.aSlotPassedNoticeHasANeutralTitle` checks that "Time slot unavailable" is
  rendered and "Couldn't book" is not.

**Requires human verification (logic change):** decide whether the rule (explain only on resume and Book Now, stay
silent on taps) matches the intended UX. Resume is the only case where a selection disappears without the user doing
anything.

## Info items touched along the way

- **IN-10 resolved:** this came from the WR-01 guard in `explainSlotPassed()`. An unread dialog, such as the 422
  meeting-type reset, is no longer replaced by the slot-passed notice.
- **IN-07 not resolved:** a zone change that moves a still-future slot to another day still says "That time has
  passed." The title is now the neutral "Time slot unavailable", which is closer, but the message is unchanged. The
  two tests named in IN-07 still assert that message.

---

_Fixed: 2026-10-22T21:51:47Z_
_Fixer: Claude (gsd-code-fixer)_
_Iteration: 4_
