---
phase: 11-expert-requests-bookings
plan: 03
subsystem: ui
tags: [components, reschedule, call-guard, strings, compose, wave-1]

requires:
  - phase: 08-client-booking-management
    provides: RescheduleResponseCard, RescheduleResponseController, RescheduleResponder, RescheduleErrors
  - phase: 09-call-experience
    provides: RestrictedAction, CallDialogs guard dialog
provides:
  - RescheduleViewer { Client, Expert } and the viewer parameter on RescheduleResponseCard
  - RescheduleResponding interface; RescheduleResponseController accepts any responder
  - RestrictedAction.SetUpPayouts and RestrictedAction.RespondToReschedule with guard copy
  - Shared expert display helpers (meetingTypeLabel, meetingTypeIcon, clientDisplayName)
  - ExpertRescheduleBadge composable and strings_expert_shared.xml
affects: [11-04, 11-06, 11-07, 11-08, 11-09, 11-10, 11-12, 11-13, 11-14]

actuals:
  tokens: 14500
  tasks: 3
  commits: 6
plan_head_before: f3a31401b9bcde5033bd4b1dc7f224291d2c72ae
plan_head_after: d183a31d992a335444b03328926bb676c5adcff4

tech-stack:
  added: []
  patterns:
    - "Viewer-parameterized shared card: actionable = proposal side differs from viewer; copy branches per viewer, tags unchanged"
    - "Controller depends on a narrow responder interface so client and expert flows share in-flight/dialog/notice logic"

key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/ExpertDisplay.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/components/ExpertRescheduleBadge.kt
    - app/src/main/res/values/strings_expert_shared.xml
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/expertmode/ExpertSharedUiTest.kt
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/components/RescheduleResponseCard.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/RescheduleResponse.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/RescheduleErrors.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/managers/CallDialog.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/call/calling/CallDialogs.kt
    - app/src/main/res/values/strings_reschedule_response.xml
    - app/src/main/res/values/strings_call.xml
    - app/src/test/java/com/uptrendinvestments/clickmeapp/components/RescheduleResponseCardTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/RescheduleResponseTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/call/calling/CallDialogsTest.kt

key-decisions:
  - "Expert accept dialog never renders the payment disclosure or charges-soon paragraphs, even if the proposal flags are set (D-18)"
  - "Card keeps the client string ids and test tags untouched; the expert viewer only adds string ids"
  - "serverTextOrUnknown is internal so the expert error mappers in 11-09, 11-12 and 11-13 reuse it"

patterns-established:
  - "Shared expert copy lives once in strings_expert_shared.xml with a header comment listing reused strings"

requirements-completed: [EXP-05, EXP-06, EXP-08, EXP-09]

coverage:
  - id: D1
    description: "Expert viewer of the reschedule card: client proposal is actionable with expert copy, own proposal is read-only, accept dialog has no payment text"
    requirement: EXP-08
    verification:
      - kind: automated_ui
        ref: "RescheduleResponseCardTest#expertViewer_clientProposal_isActionable, expertViewer_ownProposal_isReadOnly, expertViewer_acceptDialog_hasNoPaymentText, expertViewer_declineDialog_keepLabel"
        status: pass
    human_judgment: false
  - id: D2
    description: "Client behavior of the reschedule card and controller is unchanged"
    verification:
      - kind: unit
        ref: "RescheduleResponseCardTest (existing 17 client cases), RescheduleResponseControllerTest, RescheduleErrorsTest, UpcomingBookingViewModelTest, BookingSummaryViewModelTest"
        status: pass
    human_judgment: false
  - id: D3
    description: "RescheduleResponseController works over any RescheduleResponding"
    verification:
      - kind: unit
        ref: "RescheduleResponseControllerTest#controllerWorksWithAnyResponder"
        status: pass
    human_judgment: false
  - id: D4
    description: "SetUpPayouts and RespondToReschedule guard actions with call_action_* copy"
    requirement: EXP-09
    verification:
      - kind: automated_ui
        ref: "CallDialogsTest#guard_endCallToSetUpPayouts, guard_sessionInProgressRespondToReschedule"
        status: pass
    human_judgment: false
  - id: D5
    description: "Shared expert copy (meeting type, client name fallback) and the tinted reschedule badge"
    requirement: EXP-05
    verification:
      - kind: automated_ui
        ref: "ExpertSharedUiTest#meetingTypeLabels, clientNameFallback, rescheduleBadgeTexts, rescheduleBadgeExpertInitiated"
        status: pass
    human_judgment: true
    rationale: "Visual fit of the badge (tint, wrapping at 2 lines) is not asserted by tests; verified at the phase UAT on a device"

duration: 11min
completed: 2026-10-26
status: complete
---

# Phase 11 Plan 03: Shared Expert Reschedule Card, Guard Actions and Display Copy Summary

**Expert viewer of the Phase 8 reschedule card over an interface-typed controller, two new call-guard actions (SetUpPayouts, RespondToReschedule), and shared expert display copy with a tertiary-tinted reschedule badge**

## Performance

- **Duration:** 11 min
- **Started:** 2026-10-26T17:12:54Z
- **Completed:** 2026-10-26T17:23:16Z
- **Tasks:** 3
- **Files modified:** 14 (4 created, 10 modified)

## Accomplishments
- `RescheduleResponseCard` takes `viewer: RescheduleViewer = Client`. For the Expert viewer a client proposal shows Decline and Accept with expert copy; the expert's own proposal is read-only with "Awaiting client's response"; the accept dialog never shows payment text. Client strings, ids and test tags are unchanged.
- `RescheduleResponding` interface added; `RescheduleResponder` implements it and `RescheduleResponseController` is typed to it, so the expert responder (11-13) reuses the in-flight, dialog and notice logic. Existing client ViewModels compile unchanged.
- `RestrictedAction.SetUpPayouts` and `RespondToReschedule` with exhaustive-`when` branches and `call_action_*` strings.
- `ExpertDisplay.kt`, `ExpertRescheduleBadge.kt` and `strings_expert_shared.xml` so Wave 2 plans do not collide on shared files.

## Task Commits

1. **Task 1: Expert viewer + interface-typed controller (tracer)** - `b25b32b` (test, RED), `06f88a7` (feat)
2. **Task 2: Guard actions** - `f9cc0d1` (test, RED), `cb9ba8f` (feat)
3. **Task 3: Shared display copy and badge** - `c6a5fc2` (test, RED), `d183a31` (feat)

**Plan metadata:** docs commit following this file.

## Files Created/Modified
- `components/RescheduleResponseCard.kt` - `RescheduleViewer`, viewer-aware header, note, awaiting pill, accept and decline dialogs
- `screens/clientmode/mybookings/RescheduleResponse.kt` - `RescheduleResponding`; controller takes the interface
- `screens/clientmode/mybookings/RescheduleErrors.kt` - `serverTextOrUnknown` is `internal`
- `managers/CallDialog.kt`, `screens/call/calling/CallDialogs.kt`, `strings_call.xml` - two new guard actions and copy
- `screens/expertmode/ExpertDisplay.kt` - `meetingTypeLabel`, `meetingTypeIcon`, `clientDisplayName`
- `components/ExpertRescheduleBadge.kt` - badge with `expert_reschedule_badge` tag
- `strings_reschedule_response.xml`, `strings_expert_shared.xml` - new copy
- Tests: `RescheduleResponseCardTest` (+4), `RescheduleResponseTest` (+1, fake `RescheduleResponding`), `CallDialogsTest` (+2), `ExpertSharedUiTest` (new, 4)

## Decisions Made
- The accept dialog suppresses disclosure and charges-soon text for the Expert viewer regardless of the proposal flags (D-18).
- Preview functions in `ExpertRescheduleBadge.kt` are named `RescheduleBadge*Preview` so the plan's `grep -c 'fun ExpertRescheduleBadge'` acceptance check prints 1.

## Deviations from Plan

None - plan executed exactly as written. Minor process notes: the RED test commits for Tasks 2 and 3 were committed without a separate failing Gradle run (the new symbols did not exist, so compilation necessarily failed); Task 1's RED was run and failed to compile as expected. Tasks 2 and 3 were verified in one combined Gradle invocation, then committed per task.

**Total deviations:** 0 auto-fixed.

## Issues Encountered
- The per-plan commit ledger file under the shared `.git/worktrees/` directory could not be written from the isolated worktree, so `plan_head_before` was taken from the worktree base SHA given in the prompt (`f3a3140`), and `commits: 6` was measured with `git rev-list --count f3a3140..HEAD` before the docs commit.

## User Setup Required

None - no external service configuration required.

## Known Stubs

None.

## Threat Flags

None. T-11-08 (exhaustive `when` plus tests), T-11-09 (viewer-based `actionable`, tested) and T-11-10 (client names and notes rendered only as plain `Text`) are mitigated as planned.

## Next Phase Readiness
- Wave 2 plans can use `RescheduleViewer.Expert`, `RescheduleResponding`, `RestrictedAction.SetUpPayouts` / `RespondToReschedule`, `ExpertRescheduleBadge`, and the shared display helpers.
- `requirements-completed` lists EXP-05, EXP-06, EXP-08, EXP-09 verbatim from the plan; the shared-ID gate (other plans in the phase declare them too) is left to the orchestrator, and REQUIREMENTS.md was not touched here.

## Self-Check: PASSED

- Created files exist: ExpertDisplay.kt, ExpertRescheduleBadge.kt, strings_expert_shared.xml, ExpertSharedUiTest.kt (all FOUND)
- Commits b25b32b, 06f88a7, f9cc0d1, cb9ba8f, c6a5fc2, d183a31 are all ancestors of HEAD
- Task verifies: RescheduleResponseCardTest 21/21, RescheduleResponseControllerTest 9/9, CallDialogsTest 21/21, ExpertSharedUiTest 4/4, ktlintCheck and `scripts/check-design-rules.sh` ("Design rules: OK") pass

---
*Phase: 11-expert-requests-bookings*
*Completed: 2026-10-26*
