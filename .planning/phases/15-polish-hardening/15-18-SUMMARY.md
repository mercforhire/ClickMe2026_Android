---
phase: 15-polish-hardening
plan: 18
subsystem: booking-detail
tags: [booking-detail, call-resume, refresh, large-screen, tracer]
status: complete

requires:
  - phase: 15-01
    provides: RefreshFailedSnackbarEffect, ErrorState
  - phase: 15-02
    provides: contentMaxWidth
  - phase: 15-09
    provides: CallResumeNotice
provides:
  - "Upcoming Booking Details and expert Booking Summary: one-time call resume note above the join control"
  - "Failed realtime refetch / Retry keeps content, Retry snackbar, 4 dp LinearProgressIndicator under the app bar"
  - "404 first load is an Unavailable EmptyState with no Retry; other first-load failures use ErrorState"
  - "Both detail screens capped at 600 dp"
  - "Shared CallResumeNote composable and ApiException.isNotFound()"

commits: 4
plan_head_before: a102d624579499ca236a758404884ab43fc3c983
plan_head_after: 40e34d4c2524d34184a8c9133bf29b4c03b2a19f

actuals:
  tokens: 21000
  tasks: 2
  commits: 4

key-files:
  created:
    - app/src/main/res/values/strings_call_resume.xml
    - app/src/main/java/com/uptrendinvestments/clickmeapp/components/call/CallResumeNote.kt
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/upcomingbooking/UpcomingBookingViewModel.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/upcomingbooking/UpcomingBookingUiState.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/upcomingbooking/UpcomingBookingScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/bookingsummary/ExpertBookingSummaryViewModel.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/bookingsummary/ExpertBookingSummaryUiState.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/bookingsummary/ExpertBookingSummaryScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/BookingErrors.kt

key-decisions:
  - "The resume flag is consumed once in the ViewModel constructor and carried through every later Loaded state of that instance, so reloads do not hide the note and a new opening of the screen never shows it"
  - "Only realtime refetches and Retry flag a failure; the silent resume reload (D-02) stays silent as before"
  - "Expert flags live on ExpertBookingSummaryUiState (the wrapper), not on ExpertSummaryLoad.Loaded"

requirements-completed: [POL-01, POL-02, POL-03]

duration: ~2h wall (two sessions)
completed: 2026-10-29
---

# Phase 15 Plan 18: Booking detail resume note and refresh standard Summary

**Upcoming Booking Details and the expert Booking Summary show the one-time "You were on a call. Tap Join Call to rejoin." note above the join control, keep their content on a failed refresh with a Retry snackbar and 4 dp progress bar, show "unavailable" for a 404, and cap content at 600 dp.**

## Accomplishments

- `CallResumeNotice.consumeFor(bookingId)` is called once in each ViewModel constructor (grep count 1 each); the note is a polite live region, shown only for the posted booking and only once. Nothing references the call engine, starter or permission gate; Join Call stays the user's tap.
- `refresh()` keeps the loaded content, sets `isRefreshing`, and a failure sets `refreshFailed`; `onRefreshFailureConsumed()` clears it. Realtime `booking.update` for the booking flags failures the same way.
- Screens: `RefreshFailedSnackbarEffect(onRetry = refresh)`, `LinearProgressIndicator` (4 dp, primary on surfaceContainerHigh, tags `upcoming_booking_refresh_progress` / `expert_summary_refresh_progress`), `contentMaxWidth()` on the scroll column.
- First-load error: both screens already had a named heading and a Retry (client "Couldn't load booking", expert "Couldn't load booking"), so the existing copy and strings were kept (no new heading strings). They were converted to the shared `ErrorState` (top-aligned, polite live region). The plan's expert wording "Couldn't load session" was not adopted because the existing compliant heading is explicitly allowed.
- 404 first load: new `Unavailable` state (client) / `ExpertSummaryLoad.Unavailable` (expert) renders `EmptyState` "This booking isn't available" with no Retry. A 404 on a quiet reload keeps the content.

## TDD

- RED `b167c693` (client) and `3796e3eb` (expert): compiling skeletons (state fields, `CallResumeNotice` injection, no-op `refresh()`) plus 11 new cases each; 11 failed on assertions in each class set (note flag false, refresh not flagged, no progress bar, no unavailable state, no width cap).
- GREEN `da85e5f6` (client) and `40e34d4c` (expert). No REFACTOR commit.
- Tracer gate (Task 1): the verify command was re-run end to end before expanding to Task 2 and passed.

## Verification

- `:app:testDevDebugUnitTest` for `ExpertBookingSummaryViewModelTest` (37), `ExpertBookingSummaryScreenTest` (40), `UpcomingBookingViewModelTest` (36), `UpcomingBookingScreenTest` (53), `CallResumeMarkerTest` (8): 0 failures; `ktlintCheck` clean; `scripts/check-design-rules.sh` prints "Design rules: OK".
- Acceptance greps: `consumeFor(` == 1 in both ViewModels; `RefreshFailedSnackbarEffect(` == 1 in both screens; `contentMaxWidth()` present in both screens; resume string present once.

## Deviations from Plan

**1. [Rule 2 - Missing critical / consistency] Shared CallResumeNote composable (outside files_modified)**
- **Issue:** The note, its live-region semantics and tag would have been duplicated in two screens.
- **Fix:** Added `components/call/CallResumeNote.kt` (with `CALL_RESUME_NOTE_TAG = "call_resume_note"`), used by both screens. Kept as instructed.
- **Commits:** da85e5f6 (inline first), 40e34d4c (extracted)

**2. [Rule 3 - Blocking] 404 handling needed a shared helper**
- Added `ApiException.isNotFound()` to `makeabooking/BookingErrors.kt` (outside files_modified) rather than a second private copy per ViewModel.

**3. [Plan wording] Unavailable state strings and Unavailable UI state**
- `booking_detail_unavailable_heading/body` added to `strings_call_resume.xml`; the must-have "404 shows the unavailable EmptyState" required a new sealed state on both screens.

**4. Dispatch note:** the coordinator message said HEAD descends from 8829c7c1; this worktree's HEAD descends from the dispatch base a102d624 and is on `worktree-agent-a6720171449445940`. No reset or ref rewrite was performed.

## Open Items (device checks deferred by the user)

- On a device: kill the app during an in-app call, relaunch, and confirm the booking detail (client and expert) shows the note once above Join Call, with TalkBack announcing it, and that reopening the screen does not show it.
- After Retry succeeds on a detail screen, confirm TalkBack focus moves to the app bar title (the plan's backstop truth; the layout is replaced in place, no focus move is implemented or unit tested).
- Pull a realtime update while offline on both screens and confirm snackbar and progress bar behaviour on a large window (600 dp cap).

## Known Stubs

None.

## Threat Flags

None. T-15-38 is mitigated by `consumeFor(bookingId)` and the `noteShownOnceWhenNoticePosted` / `noNoteForOtherBooking` tests on both ViewModels.

## Self-Check: PASSED

Files exist (CallResumeNote.kt, strings_call_resume.xml, the six screen/ViewModel/state files); commits b167c693, da85e5f6, 3796e3eb, 40e34d4c are in the branch history.
