---
phase: 08-client-bookings-cancellation
plan: 05
subsystem: ui
tags: [reschedule, payment-recovery, viewmodel-delegate]
requires: [08-01, 08-02, 08-04]
provides:
  - "rescheduleDisclosure / ClientBookingDetail.toRescheduleProposalUi: pure disclosure flags and shared proposal card model"
  - "RescheduleResponder: accept with one-shot saved-card recovery (D-02), decline with capped reason (D-04)"
  - "RescheduleResponseController: in-flight guard, reload-first, dialog and notice state shared by both detail ViewModels (D-01)"
affects: [08-08, 08-09, 08-10, 08-11]
tech-stack:
  added: []
  patterns: ["stateless @Inject responder + plain controller class constructed in ViewModel init with viewModelScope"]
key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/RescheduleDisclosure.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/RescheduleResponse.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/RescheduleDisclosureTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/RescheduleResponseTest.kt
  modified: []
key-decisions:
  - "Exactly one retry on PAYMENT_METHOD_REQUIRED: default card, else first card; second 422 ends in CardUnusable"
  - "submitting is reset in a finally block after the reload so the card stays disabled until the booking refreshed"
  - "Silent failures (account suspended) leave the state untouched; Show(pop=true) skips the reload"
requirements-completed: [BOOK-09]
duration: 25min
completed: 2026-10-23
---

# Phase 8 Plan 05: Reschedule Response Logic Summary

Shared reschedule-response logic for Upcoming Booking Details and Booking Summary: a stateless responder with the D-02 one-shot card recovery, a controller owning in-flight/dialog/notice state, and pure disclosure and proposal mapping.

## Tasks

| Task | Name | Commit |
| ---- | ---- | ------ |
| 1 (RED) | Failing RescheduleDisclosureTest | ad07d23 |
| 1 (GREEN) | RescheduleDisclosure.kt | 0e61daf |
| 2 (RED) | Failing responder tests | ba97366 |
| 2 (GREEN) | RescheduleResponder | 5af5eb4 |
| 3 | RescheduleResponseController plus controller tests | 57e2bb4 |

## Verification

- `:app:testDevDebugUnitTest --tests '*RescheduleResponse*' --tests '*RescheduleDisclosureTest' ktlintCheck` passes: RescheduleDisclosureTest 9, RescheduleResponseTest 12, RescheduleResponseControllerTest 8, no failures.
- Grep gates: `if (_state.value.isSubmitting) return` appears 2 times; no `Log.`/`println` in RescheduleResponse.kt; `Duration.ofHours(48)` once; no `Instant.now()` in RescheduleDisclosure.kt.
- Threat mitigations covered: T-08-16 (in-flight guard test), T-08-17 (two accept requests max), T-08-18 (only ids from own card list), T-08-19 (no logging), T-08-20 (initiatedByExpert gate in the proposal model).

## Deviations from Plan

None - plan executed exactly as written, with two process notes:
- The worktree base was behind the expected commit and was reset to e682101 per the worktree branch check.
- Task 3 tests (controller) and implementation were committed together in one commit rather than a separate RED commit; the controller tests share `RescheduleResponseTest.kt` with the Task 2 responder tests.

## TDD Gate Compliance

Tasks 1 and 2 each have a `test(08-05)` RED commit (compile failure, types absent) followed by a `feat(08-05)` GREEN commit. Task 3 has a single `feat` commit containing both tests and implementation (see above).

## Known Stubs

None.

## Threat Flags

None.

## Self-Check: PASSED

All four files exist and commits ad07d23, 0e61daf, ba97366, 5af5eb4, 57e2bb4 are in the log.
