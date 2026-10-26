---
phase: 11-expert-requests-bookings
plan: 08
subsystem: ui
tags: [booking-summary, join, viewmodel, compose, wave-2]

requires:
  - phase: 11-expert-requests-bookings
    provides: "11-01 ExpertBookingsRepository.detail + fixtures; 11-02 ExpertBookingSummaryKey / ExpertCancelKey / registration stub; 11-03 meetingTypeLabel, meetingTypeIcon, clientDisplayName"
provides:
  - "ExpertBookingSummaryViewModel (assisted by key): load from /details, quiet reloads, join, guarded cancel entry, reloadQuietly()"
  - "ExpertSummaryDetails display model, SummaryFee (Free / Amount / ServerLabel) and ExpertBookingDetail.toSummaryDetails(now, locale)"
  - "ExpertBookingSummaryScreen / ExpertBookingSummaryContent, ExpertSummaryCard, ExpertMessageCard"
  - "Route registration with BottomBar.hidden(); Cancel pushes ExpertCancelKey with the same origin"
affects: [11-10, 11-13, 11-15]

actuals:
  tokens: 13500
  tasks: 3
  commits: 3
commits: 3
plan_head_before: 2d62a572d451277a2d07d2d2715b4a38af23cb66
plan_head_after: 64039b49c28d12120d0ad50169122e32839d5bcb

tech-stack:
  added: []
  patterns:
    - "Key-assisted summary VM with latest-wins startLoad(quiet) and own-id realtime filter (same as UpcomingBookingViewModel)"
    - "Screen wraps onCancel with viewModel.attemptCancel() so the guard runs before any navigation"
    - "Fee formatted on the device from amount + currency; server label only when currency is missing"

key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/bookingsummary/ExpertBookingSummaryUiState.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/bookingsummary/ExpertBookingSummaryViewModel.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/bookingsummary/ExpertBookingSummaryScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/bookingsummary/subviews/ExpertSummaryCard.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/bookingsummary/subviews/ExpertMessageCard.kt
    - app/src/main/res/values/strings_expert_booking_summary.xml
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/bookingsummary/ExpertBookingSummaryScreenTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/bookingsummary/ExpertBookingSummaryViewModelTest.kt
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/bookingsummary/ExpertBookingSummaryEntries.kt

key-decisions:
  - "ViewModel exposes callGuard as a public val and loadedDetail as a read-only property so 11-13 can add the reschedule controller without reshaping the constructor"
  - "Loading and Failed screen states were built in Task 1 (needed for a coherent screen); their tests landed in Task 2 as planned"
  - "Screen reloads quietly on first composition, resume and after a hidden period, so the details endpoint may be hit more than once on open; the test asserts every request carries this booking id"

patterns-established:
  - "Expert summary test harness: real VM over FakeClickMeApi + ExpertBookingsRepository, MutableClock 30 minutes before start, JoinClock on a detached scope"

requirements-completed: [EXP-08, EXP-09]

coverage:
  - id: D1
    description: "Summary loads GET expert/bookings/{id}/details and shows client header, Date, Time, Duration, Topic, Meeting Type and Consultation Fee"
    requirement: "EXP-08"
    verification:
      - kind: unit
        ref: "ExpertBookingSummaryScreenTest#summaryRendersFromDetails"
        status: pass
      - kind: unit
        ref: "ExpertBookingSummaryViewModelTest#loadsDetailsForKey"
        status: pass
    human_judgment: false
  - id: D2
    description: "Fee formatted client-side: Free for 0 or none, JPY without decimals, server label only when currency missing; short id is first 8 uppercase"
    requirement: "EXP-08"
    verification:
      - kind: unit
        ref: "ExpertBookingSummaryScreenTest#freeBookingShowsFree, #noFeeAtAllShowsFree, #jpyFeeShowsWithoutDecimals, #feeWithoutCurrencyFallsBackToTheServerLabel, #shortIdIsFirstEightUppercase"
        status: pass
    human_judgment: false
  - id: D3
    description: "Client's message card only for non-blank notes; no meeting-link, Message, Reschedule or note controls"
    requirement: "EXP-09"
    verification:
      - kind: unit
        ref: "ExpertBookingSummaryScreenTest#messageCardOnlyWhenNotesPresent, #noMessageCardForNullNotes, #noMessageCardForBlankNotes, #noMessageRescheduleOrNoteControls"
        status: pass
    human_judgment: false
  - id: D4
    description: "Join shown for confirmed, in_progress, pending_reschedule and rejoinable completed; enabled by the join window; starts the call with the client as peer"
    requirement: "EXP-09"
    verification:
      - kind: unit
        ref: "ExpertBookingSummaryScreenTest#joinShownForConfirmedAndStartsCall, #joinHiddenFor*, #joinHiddenForACompletedBookingThatHasEnded, #joinShownForACompletedBookingThatEndsInTheFuture"
        status: pass
      - kind: unit
        ref: "ExpertBookingSummaryViewModelTest#showsJoinFollowsTheStatusAndTheEndTime, #joinStartsTheCallWithTheClientAsPeer, #joinBeforeLoadIsNoOp"
        status: pass
    human_judgment: false
  - id: D5
    description: "Cancel Booking shows only when actions.can_cancel and opens the cancel route only when CallGuard.attemptModifyBooking allows it"
    requirement: "EXP-09"
    verification:
      - kind: unit
        ref: "ExpertBookingSummaryScreenTest#cancelHiddenWhenNotAllowed, #cancelOpensTheCancelFlowWhenTheGuardAllows, #cancelIsBlockedWhileThisBookingsCallIsLive"
        status: pass
    human_judgment: false
  - id: D6
    description: "Quiet reloads on resume and on realtime booking.update for this booking id only; quiet failure keeps the loaded state"
    requirement: "EXP-08"
    verification:
      - kind: unit
        ref: "ExpertBookingSummaryViewModelTest#realtimeForThisBookingReloadsQuietly, #realtimeForAnotherBookingDoesNotReload, #refreshQuietly*, #quietFailureKeepsLoaded, #reloadQuietlyWaitsForTheReload"
        status: pass
    human_judgment: false
  - id: D7
    description: "Visual fit of the summary card, wrapping at large font scales and the Join / Cancel button layout on a device"
    human_judgment: true
    rationale: "Pixel layout and large-text wrapping are not asserted by Robolectric tests; manual device check is deferred per the Phase 11 manual-testing note"

duration: 40min
completed: 2026-10-26
status: complete
---

# Phase 11 Plan 08: Expert Booking Summary Summary

**Expert Booking Summary loaded from GET expert/bookings/{id}/details with a device-formatted fee, client's message card, Join through the shared call layer and a call-guarded Cancel entry, quietly reloading on resume and on its own booking's realtime updates.**

## Performance

- **Duration:** about 40 min (dominated by a cold 12-minute first Gradle build)
- **Tasks:** 3 (one tracer, two expansion)
- **Files:** 8 created, 1 modified

## Accomplishments

- Tracer: the booking opens from the registered route, loads `/details` through `ExpertBookingsRepository.detail`, maps to `ExpertSummaryDetails` and renders the summary card (64dp avatar, name, `ID: #XXXXXXXX`, status badge, Date, Time, Duration, Topic, Meeting Type, Consultation Fee). The tracer verify was re-run green before expanding.
- Fee rules (C5): `Free` for a zero or missing amount, `Money.formatMajor` when a currency exists (JPY renders `¥7,500`), server label only for a paid fee without a currency, `CAD` formatting as the last fallback.
- Join (D-14) with the client as peer, gated by `JOINABLE_BOOKING_STATUSES` or a `completed` booking that has not ended, enabled by `isWithinJoinWindow` over `JoinClock`.
- Cancel Booking only when `actions.can_cancel`; the screen runs `attemptCancel()` (`CallGuard.attemptModifyBooking(bookingId, CancelBooking)`) before `onCancel`, and the entry pushes `ExpertCancelKey(key.bookingId, key.origin)`.
- Quiet reloads: `startLoad(quiet)` latest-wins, `refreshQuietly()`, `onScreenHidden()`, own-id realtime filter, plus `suspend reloadQuietly()` for 11-13.

## Task Commits

1. **Task 1: Summary loaded from /details to the registered route (tracer)** - `1566844` (feat)
2. **Task 2: Join, client message card, guarded Cancel, skeleton and error states** - `ed4c88b` (feat)
3. **Task 3: Quiet reloads on resume and own realtime updates** - `64039b4` (feat)

## Verification

- `./gradlew :app:testDevDebugUnitTest --tests '*ExpertBookingSummaryViewModelTest' --tests '*ExpertBookingSummaryScreenTest' ktlintCheck` exits 0: ScreenTest 22/22, ViewModelTest 15/15.
- `bash scripts/check-design-rules.sh` prints `Design rules: OK`.
- Acceptance greps: `ExpertCancelKey(key.bookingId, key.origin)` in Entries = 1; `Meeting link|meeting_link` in Screen = 0; `RestrictedAction.CancelBooking` in ViewModel = 1; `expert_summary_join` tag constant used in Screen (`EXPERT_SUMMARY_JOIN_TAG` x2); `it.bookingId == bookingId` in ViewModel = 1; `Log.x(` in ViewModel = 0.

## Deviations from Plan

None - plan executed exactly as written, apart from two small ordering/scoping notes that do not change behavior:

- The Loading skeleton and Failed `EmptyState` composables were written with the Task 1 screen (a screen with no non-loaded states would not compile coherently); their tests are in Task 2 as planned.
- `summaryRendersFromDetails` asserts that every detail request carries `"b1"` rather than exactly one request, because the screen deliberately reloads quietly on first composition/resume (Task 3 behavior from the plan). Task 3 verification confirmed this is the only effect.
- TDD was followed as tests-with-implementation per task (one commit per task, as 11-01 did), not separate RED/GREEN commits.

## Authentication Gates

None.

## Known Stubs

None. Every field renders real server data. The reschedule response card is intentionally absent and is added on top by plan 11-13.

## Threat Flags

None. No new network surface: the screen uses the existing `/details` endpoint. Preparation notes render as selectable plain `Text` and are never logged (T-11-25); the private note is not declared on the DTO and never rendered (T-11-26); Cancel runs through the call guard before navigation (T-11-23).

## Notes for downstream plans

- 11-13: `callGuard` is a public `val` on the ViewModel, `loadedDetail` is readable, `reloadQuietly()` exists, and `ExpertBookingSummaryUiState` currently holds only `load`; extend it with the reschedule block.
- 11-15 iOS follow-up: iOS has no Join on the expert Booking Summary; Android adds it (D-14).

## Self-Check: PASSED

- All 8 created files and the modified Entries file exist on disk (verified via the passing test run and clean `git status` after commits).
- Commits `1566844`, `ed4c88b`, `64039b4` are ancestors of HEAD; `git rev-list --count 2d62a57..HEAD` = 3 before this summary commit.
