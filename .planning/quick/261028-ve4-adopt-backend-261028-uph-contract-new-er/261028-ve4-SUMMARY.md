---
phase: quick-261028-ve4
plan: 01
subsystem: networking, bookings
tags: [error-codes, reschedule, paid-booking, backend-261028-uph]
requires: []
provides:
  - ErrorCode.LongLeadNotAvailable and ErrorCode.RescheduleTooFarAhead
  - client and expert accept-reschedule mapping for RESCHEDULE_TOO_FAR_AHEAD
  - paid-flow backstop for LONG_LEAD_NOT_AVAILABLE
affects: [BOOK-09, EXP-09, PAY-04]
tech-stack:
  added: []
  patterns: [Android-only block in ErrorCode keeps the iOS order]
key-files:
  created: []
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/model/enums/ErrorCode.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/RescheduleErrors.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/bookingsummary/ExpertRescheduleErrors.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/PaidFlowError.kt
    - app/src/main/res/values/strings_reschedule_response.xml
    - .planning/phases/07-payment-methods-paid-booking/07-LOCAL-STRIPE-TESTING.md
    - .planning/STATE.md
decisions:
  - New codes live in the Android-only block of ErrorCode (iOS lacks both), enum is now 68 entries
  - One shared fallback string for client and expert accept
status: complete
commits: 3
plan_head_before: 43274e4a7fbd59036cd925b5c05686fde830aad3
plan_head_after: 2e0e0c168ce94a93d972af358ad2f4fe22c661ce
actuals:
  tokens: 9000
  tasks: 3
  commits: 3
completed: 2026-10-28
---

# Quick 261028-ve4: Adopt backend 261028-uph contract Summary

Two new error codes (`LONG_LEAD_NOT_AVAILABLE`, `RESCHEDULE_TOO_FAR_AHEAD`) are parsed, mapped and tested on Android. A reschedule accept beyond the 6-day paid cap shows the server text, or a new Android 6-day string, and reloads the booking on both the client and expert side.

## Commits

- ead53d0 feat(quick-261028-ve4): adopt RESCHEDULE_TOO_FAR_AHEAD and LONG_LEAD_NOT_AVAILABLE codes, client accept mapping
- 27d5faa feat(quick-261028-ve4): map RESCHEDULE_TOO_FAR_AHEAD on expert accept and LONG_LEAD_NOT_AVAILABLE backstop
- 2e0e0c1 docs(quick-261028-ve4): runbook sandbox expert done, optional RESCHEDULE_TOO_FAR_AHEAD test case, STATE decisions

## Gate results

- ApiErrorParserTest 25/25, RescheduleErrorsTest (client) 19/19, ExpertRescheduleErrorsTest 15/15, PaidFlowErrorTest 25/25, 0 failures, 0 skipped.
- ktlintCheck: pass. `scripts/check-design-rules.sh`: "Design rules: OK".
- Task 3 secret checks: the uat password (17 chars) and full Connect account id (21 chars), read from the backend doc at run time, are absent from the runbook and STATE.md. No sk_/rk_/whsec_ pattern in the runbook. `DISABLE_CRONS=true` appears on 4 lines in the runbook.
- STATE.md diff check: every hunk is inside `### Decisions` (line 95-96) or `### Blockers/Concerns` (lines 207, 294-296).

## Deviations from Plan

- Per the dispatch instruction, runbook test case 9 (RESCHEDULE_TOO_FAR_AHEAD accept) is marked OPTIONAL, with the reason: after 261028-uph the code is normally unreachable because propose is gated too; only a proposal made before the change can hit it.
- [Rule 3 - Blocking] Two test lines exceeded the 120-column ktlint limit; the message literal was moved to its own line in RescheduleErrorsTest and ExpertRescheduleErrorsTest.

Otherwise none; the plan was executed as written.

## Open items

- Runbook test case 9 and the other device tests are not run (manual testing deferred); logged as open.
- iOS follow-up remains open (recorded in STATE.md).

## Known Stubs

None.

## Threat Flags

None.

## Self-Check: PASSED

All five source files and the two doc files exist, and commits ead53d0, 27d5faa and 2e0e0c1 are ancestors of HEAD.
