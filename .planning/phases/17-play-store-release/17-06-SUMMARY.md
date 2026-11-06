---
phase: 17-play-store-release
plan: 06
subsystem: release
tags: [release-checklist, closed-test, production-access, smoke-test, play-console, docs]
requires:
  - phase: 17-03
    provides: deletion page, privacy edits and the 17-08 answer sheet
  - phase: 17-04
    provides: 17-CONSOLE-PASTE.md
provides:
  - 17-RELEASE-CHECKLIST.md, the ordered 16-section human runbook from the gate check to the public release
  - pointers from Phase 13 checklist steps 7 and 9 to the Phase 17 sections
affects: [17-08, 17-09, 17-10]
tech-stack:
  added: []
  patterns: [Phase 13 checklist format (Needs, steps, Check, checkbox), result tables with the Phase 16 notation]
key-files:
  created:
    - .planning/phases/17-play-store-release/17-RELEASE-CHECKLIST.md
  modified:
    - .planning/phases/13-push-notifications-internal-testing/13-RELEASE-CHECKLIST.md
key-decisions:
  - "Gate branch up front: gate shown runs the closed test; not shown releases vc 6 on Internal testing and smoke-tests that install"
  - "Contact email is clickmeapp@uptrendinvestments.net everywhere (orchestrator correction); support@clickme.app does not appear"
  - "Step 9 pointer in the Phase 13 checklist also names 17-RELEASE-CHECKLIST.md section 6 so both pointers resolve to this file"
requirements-completed: [REL-04, REL-06, REL-07]
duration: 25min
completed: 2026-11-06
status: complete
commits: 3
plan_head_before: 33417d340c73fbb3a2062b61d16e930aa02ee122
plan_head_after: b5d62a9e3d99ee280cbe97e0b2e44e15aac59461
estimate:
  tokens: 60000
  tasks: 3
actuals:
  tokens: 6500
  tasks: 3
  commits: 3
---

# Phase 17 Plan 06: Release Checklist Summary

One ordered 16-section human runbook (gate check, same-day starters, Console paste, vc 6 closed-track upload, 14-day log, two smoke tables, truthful production-access drafts, 100% CA/US rollout, close-out) that starts the 14-day clock's prerequisites today and never waits on code.

## What was built

- `17-RELEASE-CHECKLIST.md`: sections 1 to 16 with Needs, steps, a Check line and a checkbox each. Sections 1 to 5 (gate, Play support question, tester service, foreground-service video, feature graphic) need no code. Section 11 carries the day 0 to 14 log table and the rule that a fix build does not reset the 14 days while a tester opting out breaks only that tester's streak (D-04). Sections 12 and 15 hold smoke run 1 (S1.00 to S1.10, including the BUG-34 and BUG-22 re-tests and the in-call screenshot) and smoke run 2 (S2.00 to S2.06) with Result cells. Section 13 holds the Part 1, 2 and 3 application drafts. Section 14 has the 100% Canada and US promotion with the CA and US listing curl checks and the rollback note.
- `13-RELEASE-CHECKLIST.md`: a "Carried by Phase 17" line under step 7 and under step 9. No box ticked.
- Accounts and phones are named by role only. No password, token or keystore value or field name is in the file.

## Flagged assumptions and where they are written

| Id | Where |
|----|-------|
| A1 (which Console items block a closed rollout) | section 8 Check note; section 10 step 4 |
| A2 (per-track countries) | section 3 step 1 (ask the vendor for countries); section 10 step 2 |
| A3 (vendor claims) | section 3 intro |
| A4 (organization accounts exempt, not relied on) | section 1 note |
| A10 (review times) | Timeline; section 13 step 2 |
| A14 (testers follow the free-topics rule) | section 3 step 3; section 11 rule 3 |

## Verification

The three plan verify gates pass (SKELETON-OK, STEPS-OK, RUNBOOK-OK, run from a script in the scratchpad because the shell here refuses long compound commands). `git status --porcelain app/` is empty. `grep -c support@clickme` on the checklist is 0.

## Deviations from Plan

### Auto-fixed or adjusted

**1. [Rule 3 - Blocking] Phase 13 step 9 pointer wording**
- **Found during:** Task 3 verify
- **Issue:** the plan's step 9 pointer text does not contain `17-RELEASE-CHECKLIST.md`, but the acceptance criterion requires two files-name matches.
- **Fix:** appended "(`17-RELEASE-CHECKLIST.md` section 6)" to the step 9 pointer.
- **Files modified:** 13-RELEASE-CHECKLIST.md
- **Commit:** b5d62a9e

**2. [Orchestrator correction] Contact email**
- The deletion-page check greps for `clickmeapp@uptrendinvestments.net`, not `support@clickme.app`. The open item "confirm support@clickme.app is monitored" is dropped.
- Section 3 step 2 tells the user to check that the tester-instructions text pasted from `17-CONSOLE-PASTE.md` section 11 carries the right contact email, because the paste file in this plan's base still said `support@clickme.app` (the orchestrator is fixing it on main).

**3. Added to the section 6 decision step and section 16 known-open list:** the in-app `delete_account_body` over-promise (open item from 17-03) as a candidate closed-track fix build.

## Known Stubs

None.

## Threat Flags

None. Docs only; no new endpoints or code.

## Self-Check: PASSED

- 17-RELEASE-CHECKLIST.md present; 16 sections, 16 checkboxes, 16 Check lines, S1 rows 11, S2 rows 7, Parts 1 to 3 present.
- Commits e0f9578a, cdd42cf2, b5d62a9e are on the branch.
