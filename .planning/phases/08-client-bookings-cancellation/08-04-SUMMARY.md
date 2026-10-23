---
phase: 08-client-bookings-cancellation
plan: 04
subsystem: ui
tags: [copy, strings, error-mapping, ui-contract]
requires: []
provides:
  - "Five Phase 8 string files (117 strings) with the UI-SPEC Copywriting Contract text"
  - "RescheduleProposalUi / RescheduleSubmitting / RescheduleDialog / RescheduleNotice / RescheduleUiState shared reschedule UI contract"
  - "RescheduleFailure / CancelFailure and ApiException.toRescheduleFailure() / toCancelFailure() code-based mappers"
affects: [08-05, 08-06, 08-07, 08-08, 08-09, 08-10, 08-11]
tech-stack:
  added: []
  patterns: ["sealed failure + ApiException mapper branching on ErrorCode then HTTP status (copied from BookingErrors.kt)"]
key-files:
  created:
    - app/src/main/res/values/strings_my_bookings.xml
    - app/src/main/res/values/strings_upcoming_booking.xml
    - app/src/main/res/values/strings_reschedule_response.xml
    - app/src/main/res/values/strings_cancel_booking.xml
    - app/src/main/res/values/strings_booking_summary.xml
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/RescheduleUiState.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/RescheduleErrors.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/RescheduleErrorsTest.kt
  modified: []
key-decisions:
  - "Mappers branch on ErrorCode with status fallbacks (403, 404) when the code is null; message text is never parsed"
  - "PAYMENT_METHOD_REQUIRED reaching the mapper maps defensively to the card-unusable copy"
  - "ACCOUNT_SUSPENDED is Silent in both mappers (global dialog owns it)"
requirements-completed: [BOOK-07, BOOK-09]
duration: 20min
completed: 2026-10-23
---

# Phase 8 Plan 04: Copy Contract and Reschedule Contract Summary

All Phase 8 user-facing copy now lives in five string files, with the shared reschedule UI types and the code-based reschedule and cancel failure mappers in place for the later plans.

## Tasks

| Task | Name | Commit |
| ---- | ---- | ------ |
| 1 | Five Phase 8 string files | cf066ea |
| 2 (RED) | Failing RescheduleErrorsTest | acd056e |
| 2 (GREEN) | RescheduleUiState.kt and RescheduleErrors.kt | 50ffbef |

## Verification

- `processDevDebugResources` passes; string counts are 24 / 20 / 36 / 29 / 8 as required.
- `:app:testDevDebugUnitTest --tests '*RescheduleErrorsTest' ktlintCheck` passes (BUILD SUCCESSFUL).
- Grep gates: 5 qualified `ErrorCode.*` branch references; 0 message-parsing calls in the mapper.

## Deviations from Plan

None - plan executed exactly as written. (Startup: the worktree was behind the expected base commit and was reset to 3edeb7e per the worktree branch check.)

## TDD Gate Compliance

RED (`test(08-04)` acd056e) precedes GREEN (`feat(08-04)` 50ffbef). RED was confirmed as a compile failure (the types did not exist yet), not a run of failing assertions.

## Known Stubs

None.

## Threat Flags

None. T-08-13/14/15 mitigated: server text only via `FieldError.Text`, no message parsing, `AccountSuspended` is `Silent`.

## Self-Check: PASSED

All eight files exist and commits cf066ea, acd056e, 50ffbef are in the log.
