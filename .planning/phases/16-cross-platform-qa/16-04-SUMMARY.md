---
phase: 16-cross-platform-qa
plan: 04
subsystem: qa
tags: [qa, matrix, direction-1, tier-1, live-money]
requires:
  - phase: 16-cross-platform-qa
    provides: preflight on versionCode 3, live-money plan
provides:
  - Session A results on versionCode 3 from Google Play, 49 pass, 4 fail, 2 not run, 86 carried to session B
  - Triaged bug rows BUG-05 to BUG-21 and the Fix batch A input list
affects: [16-05, 16-06, 16-07, 16-08]
key-files:
  created:
    - .planning/phases/16-cross-platform-qa/16-04-SUMMARY.md
  modified:
    - .planning/phases/16-cross-platform-qa/16-MATRIX-RUNBOOK.md
    - .planning/phases/16-cross-platform-qa/16-BUG-LOG.md
    - .planning/STATE.md
key-decisions:
  - "Session A stopped at D1.30: paid booking cannot complete on vc 3 (BUG-05), so paid rows and Section 5 carry to session B on the fix build"
  - "User kept the plain ongoing-call notification (D1.14)"
  - "User skipped the account check on the 17:09 expert screen and accepted the D1.SP3 pass"
requirements-completed: []
completed: 2026-11-03
status: complete
---

# Phase 16 Plan 04: Session A Summary

**Session A stopped at D1.30 after 55 of 141 rows: free flows mostly work across Android and iOS, and paid booking cannot complete on Android (BUG-05, blocking).** No money moved: no hold, no capture and no PAID line. The user read A's connected account after both D1.18 attempts and saw no PaymentIntent in `requires_capture`.

## Results

| Result | Rows |
|---|---|
| pass | 49 |
| fail | D1.05 (BUG-13), D1.10 (BUG-09), D1.18 (BUG-05), D1.SP1 (BUG-11) |
| not run | D1.12 (no headset), D1.PH5 (no such booking; re-run once a paid booking exists) |
| carried to session B | 86: D1.19-D1.25, D1.RS3-RS5, D1.RS8, D1.29, D1.PH1-PH4, and all 70 Section 5 rows |

Build proof: installer `com.android.vending`, versionCode 3, lastUpdateTime 2026-11-02 20:43:25, backend `8d4450f`, crash count 0 at every row start.

D1.18 is a fail from reproduction and source reading. BUG-05's cause is not proven on a device until the fix build completes a paid booking.

Several passes were reported without one of their evidence items; the results file says which (for example D1.17 Stripe check, D1.NS1 PATCH count, D1.FB1 backend row, D1.26 nudge push, D1.30 Stripe text). D1.PN6 and D1.SP3 did not confirm the killed state with `pidof`. D1.SP3's signed-in account is unconfirmed; the user declined the repeat and accepted the pass.

## Fix batch A input

**Blocking (android):** BUG-05.

**fix-if-cheap (android):** BUG-06, BUG-07, BUG-08, BUG-09, BUG-10, BUG-11, BUG-12. The user approved this list on 2026-11-03.

**Cross-repo fixes:** none. No ios or backend row blocks the Android release.

BUG-05 already has a fix with regression tests on branch `worktree-agent-a9a263da2ef1d7f1f` (commit `f94d6bbf`), not yet merged at the time of this summary.

## Reported to iOS and backend

BUG-13 to BUG-19 are in the STATE Pending Todos report-to-iOS list. BUG-14, BUG-15 and BUG-16 are for the iOS sync after Android is final.

## Deviations

- **Claude force-stopped a live call.** At 17:32:45 on 2026-11-03 Claude force-stopped ClickMe for an account check while the user was in a rejoin that began 5 seconds earlier. The check of the call service printed its result but did not stop the command. Free booking, no money, no crash. It did show the app reopening on the booking with the rejoin prompt. Later relaunches stop if a call service is running.
- **Run order.** Free rows ran ahead of the blocked paid rows, by the user's choice, rather than strictly top to bottom. D1.28 and D1.30 ran before D1.29, which stays last in Direction 1.
- **Runbook labels.** The user said A and B were unclear, so an earlier answer about which account was signed in is unreliable (BUG-20). Messages to the user now say client account and expert account.
- **D1.11 Expected wording is out of date:** hanging up leaves the call for one person only (CALL-08). The row passed on that reading; the text is not corrected yet.

## Task Commits

- Task 1: `a6fee637`
- Task 2: `b7d35f55` (pause note); results are outside the repo
- Task 3: the commit that adds this file
