---
phase: 11-expert-requests-bookings
plan: 12
subsystem: ui
tags: [request-decision, accept, decline, sheets, errors, compose, viewmodel]

requires:
  - phase: 11-expert-requests-bookings
    provides: "11-01 accept/decline repository calls and strict bodies; 11-02 BookingConfirmedKey, ExpertLaunchRequest, Navigator.replaceTop; 11-05 Incoming Requests launch consumer; 11-06 Request Decision read side; 11-07 Booking Confirmed destination"
provides:
  - Pinned Decline / Accept footer (RequestActionFooter) for pending requests, hidden for expired ones
  - AcceptRequestSheet "Confirm Booking" with optional message (cap 1000, counter from 800)
  - DeclineRequestSheet with the four backend reason codes, optional note, reason required
  - DeclineReason enum, declineReasonText, acceptMessage
  - Single-flight accept and decline in RequestDecisionViewModel with one-shot events
  - RequestDecisionFailure mapping with the ALREADY_PROCESSED exit, "Couldn't update request" dialog
  - Navigation outcomes in RequestDecisionEntries (accept replaces with Confirmed, decline and processed pop)
affects: [11-15 follow-ups, Incoming Requests refresh, Booking Confirmed]

actuals:
  tokens: 22000
  tasks: 3
  commits: 3
plan_head_before: 12a38f1c33f95e9eb5883046a694c426121ff444
plan_head_after: 3048e1b4a8e377490e8c84045d0edd94a27e6ecf
commits: 3

tech-stack:
  added: []
  patterns:
    - "Decision actions as one-shot RequestDecisionEvent consumed by the screen (Accepted, Declined, ExitAfterProcessed)"
    - "Sheet open flag, reason and typed text kept in rememberSaveable in the stateless Content, so a failed call keeps the input"
    - "Stateless sheet bodies separate from ModalBottomSheet; sheet dismissal blocked via confirmValueChange while submitting"

key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/requestdecision/DeclineReason.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/requestdecision/RequestDecisionErrors.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/requestdecision/subviews/RequestActionFooter.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/requestdecision/subviews/AcceptRequestSheet.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/requestdecision/subviews/DeclineRequestSheet.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/requestdecision/DeclineReasonTest.kt
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/requestdecision/RequestDecisionUiState.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/requestdecision/RequestDecisionViewModel.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/requestdecision/RequestDecisionScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/requestdecision/RequestDecisionEntries.kt
    - app/src/main/res/values/strings_request_decision.xml
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/requestdecision/RequestDecisionViewModelTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/requestdecision/RequestDecisionScreenTest.kt

key-decisions:
  - "decline reason_text is the trimmed note, or the English reason label when the note is blank, capped at 1000 (D-02); never empty"
  - "A successful accept keeps submitting set so nothing is tappable while the screen is replaced; failures reset it"
  - "ALREADY_PROCESSED is a dialog then ExitAfterProcessed with a plain IncomingRequestsLaunch (reload, no Declined notice), on accept and on decline"
  - "Sheet state lives in the stateless RequestDecisionContent via rememberSaveable, so the error dialog leaves the sheet and its inputs untouched"

requirements-completed: [EXP-06]

coverage:
  - id: D1
    description: "Pending request shows a pinned Decline / Accept footer; expired request shows the banner and no footer"
    requirement: EXP-06
    verification:
      - kind: automated_ui
        ref: "RequestDecisionScreenTest#pendingRequestShowsTheFooter, expiredHasNoFooter"
        status: pass
    human_judgment: false
  - id: D2
    description: "Accept sheet sends a trimmed message or {} for a blank one, caps input at 1000 with a counter from 800, and Booking Confirmed replaces the screen with a refreshed list behind it"
    requirement: EXP-06
    verification:
      - kind: unit
        ref: "RequestDecisionViewModelTest#acceptWithoutMessageSendsNull, acceptTrimsMessage, acceptSuccessEmitsConfirmedKey"
        status: pass
      - kind: automated_ui
        ref: "RequestDecisionScreenTest#acceptSendsTheMessageAndReplacesTheScreen, acceptWithBlankMessageSendsNoMessage, counterAppearsFrom800AndInputIsCappedAt1000"
        status: pass
    human_judgment: false
  - id: D3
    description: "Decline sheet offers CONFLICT, OUT_OF_SCOPE, UNAVAILABLE, OTHER with none preselected, Decline disabled until chosen, blank note sends the reason label, success pops with the Declined notice"
    requirement: EXP-06
    verification:
      - kind: unit
        ref: "DeclineReasonTest (9 tests); RequestDecisionViewModelTest#declineSendsCodeAndFallbackText, declineSendsTheTrimmedNote"
        status: pass
      - kind: automated_ui
        ref: "RequestDecisionScreenTest#declineDisabledUntilReasonChosen, theFourReasonsReadInOrder, declineSuccessCallsOnDeclined, declineNoteCounterAppearsFrom800AndIsCapped"
        status: pass
    human_judgment: false
  - id: D4
    description: "Single flight: while a call runs both actions and the sheet buttons are disabled, a second call is ignored, Back and the app bar arrow are locked"
    requirement: EXP-06
    verification:
      - kind: unit
        ref: "RequestDecisionViewModelTest#acceptIsSingleFlight, declineIsSingleFlightAndBlocksAccept, expiredRequestCannotAccept, expiredRequestCannotDecline"
        status: pass
      - kind: automated_ui
        ref: "RequestDecisionScreenTest#theFooterIsDisabledWhileAcceptIsInFlight, backIsLockedWhileSubmitting, backWorksWhenNothingIsInFlight"
        status: pass
    human_judgment: false
  - id: D5
    description: "409 ALREADY_PROCESSED is never reported as done: server message dialog, then a refreshed Incoming Requests list, no Confirmed screen and no Request declined snackbar; other errors keep the sheet inputs"
    requirement: EXP-06
    verification:
      - kind: unit
        ref: "RequestDecisionViewModelTest#alreadyProcessedShowsDialogThenExits, alreadyProcessedOnDeclineIsNeverReportedAsDeclined, otherErrorStays, networkErrorShowsTheConnectionCopy, accountSuspendedShowsNothing"
        status: pass
      - kind: automated_ui
        ref: "RequestDecisionScreenTest#alreadyProcessedExitsToARefreshedList, alreadyProcessedOnDeclineNeverReportsADecline, errorDialogKeepsSheetInputs"
        status: pass
    human_judgment: false
  - id: D6
    description: "Visual fidelity of the footer and the two sheets (stacking at 200% font scale, IME behaviour, drag handle, spinner placement) on a real device"
    requirement: EXP-06
    verification: []
    human_judgment: true
    rationale: "Robolectric asserts structure and behaviour only; layout at large font scale, keyboard insets and sheet gestures need a device pass (manual testing is deferred per project memory)"

duration: 55min
completed: 2026-10-26
status: complete
---

# Phase 11 Plan 12: Request Decision Accept and Decline Summary

**Accept (optional message, Confirm Booking sheet) and decline (four backend reason codes, label fallback for reason_text) on Request Decision, with single-flight calls, an ALREADY_PROCESSED exit to a refreshed list, and Confirmed replacing the screen on accept.**

## Performance

- **Duration:** about 55 min (Gradle runs dominate)
- **Started:** 2026-10-26T17:30:00Z (approx)
- **Completed:** 2026-10-26T18:26:00Z
- **Tasks:** 3 (one tracer, two expansion)
- **Files:** 6 created, 7 modified (plus this summary)

## Accomplishments

- Tracer: the expert accepts with or without a message and `Navigator.replaceTop(BookingConfirmedKey)` shows Booking Confirmed in place of the decision screen. The tracer verify was automated-only with `HUMAN_VERIFY_MODE` at `end-of-phase`, so it was re-run (26 tests plus ktlint) and expansion continued.
- Decline sends the backend's four codes (`CONFLICT`, `OUT_OF_SCOPE`, `UNAVAILABLE`, `OTHER`) and always a non-empty `reason_text` (note, else the reason label), fixing the two iOS defects that make decline fail with 422 (D-01, D-02).
- Errors: `RequestDecisionFailure` (Silent, AlreadyProcessed, Show). ALREADY_PROCESSED shows the server text in "Couldn't update request", then posts a plain reload and pops; any other error keeps the screen and sheet inputs.
- Locks: footer, sheet buttons, sheet dismissal, app bar arrow and system Back are all disabled while a call is in flight.
- 57 tests in the three Request Decision classes (26 ViewModel, 22 screen, 9 DeclineReason), plus the unchanged earnings (16) and Incoming Requests ViewModel (12) classes green.

## Task Commits

1. **Task 1: Expert accepts a request with an optional message and lands on Booking Confirmed (tracer)** - `8d0aac8` (feat)
2. **Task 2: Expert declines with one of the four backend reasons and returns to the list** - `f5e63d7` (feat)
3. **Task 3: ALREADY_PROCESSED exit, other errors, Back and sheet locks, read-only expired screen** - `3048e1b` (feat)

## Decisions Made

- `AcceptBookingRequestData.bookingId` is non-null (11-01), so the Confirmed key uses `data.bookingId` directly instead of the plan's `?: requestId` fallback.
- `RequestDecisionContent` gained `onAccept`, `onDecline` and `onDismissDialog` parameters, and `RequestDecisionScreen` gained `onAccepted`, `onDeclined`, `onExitAfterProcessed`; sheet open flags, the chosen reason and typed text are `rememberSaveable` inside Content.
- The shared multi-line field (`DecisionNoteField`, internal) lives in `AcceptRequestSheet.kt` and is reused by the Decline sheet.
- The counter threshold is the shared `NOTE_COUNTER_FROM = 800` in `DeclineReason.kt`.

## Deviations from Plan

### Process notes

**1. [Process] Back lock landed in Task 1 instead of Task 3**
- The `BackHandler(enabled = submitting)` and `backEnabled = !submitting` were added with the tracer so an in-flight accept was never abandonable. Tests for the lock are in Task 3 as planned.

**2. [Process] RED not committed separately**
- The tasks are `tdd="true"` but the plan is `type: execute`, so no plan-level gate applies (same as 11-07). Tests and implementation were written together and each task committed once after its verify went green.

**3. [Rule 3 - Blocking] Test viewport is smaller than the Decline sheet**
- **Found during:** Task 2. In Robolectric the Keep, Decline and reason rows sit below the window, so plain `performClick` landed outside it.
- **Fix:** the screen tests call `performScrollTo()` first, which also proves the sheet scrolls. No product code changed.

**4. [Rule 1 - Acceptance wording] KDoc reworded**
- The acceptance grep forbids the iOS code spellings in `DeclineReason.kt`, so the KDoc describes them without naming them.

**Total deviations:** 1 minor blocking (test viewport), 3 process notes. **Impact:** none on behavior or scope.

## Issues Encountered

None beyond the Robolectric viewport note above.

## Known Stubs

None. The `RequestActionFooter`, both sheets and the error dialog are fully wired to the ViewModel.

## Threat Flags

None. No new network surface (the accept and decline calls come from 11-01). T-11-36 (single flight, locks), T-11-37 (409 never success, prohibition tested at ViewModel and screen level), T-11-38 (strict bodies, `reason_text` never empty and capped) and T-11-39 (no `Log.` calls, grep gate = 0) are mitigated as planned.

## Follow-ups for 11-15 (recorded, not changed here)

- Backend `acceptSchema` validates `message` (max 1000) but the accept handler never reads it, so the expert's message is sent for iOS parity but not delivered (RESEARCH C10). Log in STATE.
- iOS sends decline codes and a "Pricing mismatch" reason the strict backend schema rejects with 422 (D-01); iOS fix is a follow-up.

## Open Items (device-only, not blocking)

- Check the footer stacking at 200% font scale and below 320dp, IME behaviour with both sheets, drag handle and swipe-lock while submitting, and the spinner placement on a Samsung device.

## Next Phase Readiness

Request Decision is complete for EXP-06. Back from Booking Confirmed and every exit path land on a refreshed Incoming Requests list (`IncomingRequestsLaunch` is consumed by 11-05's ViewModel; the decline path also carries the "Request declined" notice).

## Self-Check: PASSED

- Created files exist: DeclineReason.kt, RequestDecisionErrors.kt, subviews/RequestActionFooter.kt, AcceptRequestSheet.kt, DeclineRequestSheet.kt, DeclineReasonTest.kt (all compiled and tested).
- Commits `8d0aac8`, `f5e63d7`, `3048e1b` are ancestors of HEAD; `git rev-list --count 12a38f1..HEAD` was 3 before this summary commit.
- Acceptance greps: `navigator.replaceTop(it)` = 1; `BookingConfirmedKey(` in the ViewModel = 1; wire codes in DeclineReason.kt = 4; iOS code spellings = 0; `IncomingRequestsNotice.Declined` in the ViewModel = 1; `ErrorCode.AlreadyProcessed` in Errors = 1; `Log.x(` in the ViewModel = 0; `@Test` in the ViewModel test = 26 (needed 12).
- Plan verification: `testDevDebugUnitTest` for the five classes plus `ktlintCheck` BUILD SUCCESSFUL; `scripts/check-design-rules.sh` prints "Design rules: OK".
- STATE.md and ROADMAP.md untouched (the orchestrator owns them).
