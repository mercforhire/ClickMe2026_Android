---
phase: 16-cross-platform-qa
plan: 06
subsystem: qa
tags: [qa, matrix, direction-2, payouts, account-deletion, live-money, tracer]
requires:
  - phase: 16-cross-platform-qa
    provides: versionCode 4 on Play, batch A fixes, the live-money plan (option-a)
provides:
  - Session B results on versionCode 4 from Google Play, 45 pass, 2 fail, 77 not run
  - Triaged bug rows BUG-24 to BUG-33 and the Fix batch B input list
  - Eight new Paid bookings created rows (ten in total), two captures to refund in Section 7
affects: [16-07, 16-08]
key-files:
  created:
    - .planning/phases/16-cross-platform-qa/16-06-SUMMARY.md
  modified:
    - .planning/phases/16-cross-platform-qa/16-MATRIX-RUNBOOK.md
    - .planning/phases/16-cross-platform-qa/16-BUG-LOG.md
key-decisions:
  - "D-14 deviation, user option A: the 70 Samsung setting-pass rows (TB., FS., PD., IM., PL., EM., FS.CP1) were not run in session B and move to the batch B re-test, which adds one account swap"
  - "User decided 2026-11-05 to leave the full Client Profile hidden without a booking (privacy rule): BUG-33 closed as won't do"
  - "User rated the iOS client's inability to answer an expert reschedule (BUG-25) critical; whether it blocks the Android release is still the user's call"
  - "Audio freezes (remote audio state=3 reason=1, 2-4 s) are log-only: the user did not hear them, no BUG row"
requirements-completed: []
completed: 2026-11-05
status: complete
commits: 9
plan_head_before: 4538f4dc6c892221913a213eaf83a95ad24b6013
plan_head_after: 260a018246b3eecbd76723b1776cb582efc04d9c
actuals:
  tokens: 23000
  tasks: 3
  commits: 9
---

# Phase 16 Plan 06: Session B Summary

**Direction 2 (iOS client, Android expert) works on Android: calls, chat, free and paid booking, cancel, reschedule proposal, notes, Client Profile and account deletion all passed on the Play build. The iPhone client, however, cannot answer an expert's reschedule (BUG-25, user rated critical, owner ios).** Two real captures were made (D1.25 and D2.12) and both still need a refund in Section 7. No Android bug blocks the release.

The plan counts "commits: 9" are the nine commits from `plan_head_before` to `plan_head_after`; the commit that adds this file follows them.

## Results

Build proof: installer `com.android.vending`, versionCode 4, lastUpdateTime 2026-11-03 20:25:37, backend `2ec82af`, crash count 0 at every row start. SW.4 and D2.20 re-ran H2 on 2026-11-04 and 2026-11-05: the build did not change.

| Group | pass | fail | not run |
|---|---|---|---|
| Carried Direction 1 rows (14) | 10 | 0 | 4 (D1.21, D1.22, D1.24, D1.RS5) |
| Switch SW.1-SW.4 (4) | 3 | 1 (SW.1, BUG-24) | 0 |
| Direction 2 D2.01-D2.20 and D2.RS, FV, PN, CP, NS, FB, PV (36) | 32 | 1 (D2.RS1, BUG-25) | 3 (D2.11, D2.14, D2.15) |
| Settings rows moved to batch B (70) | 0 | 0 | 70 |
| Total (124) | 45 | 2 | 77 |

The session log row reads 47 rows run, 2 fails.

Not run, and why:
- D1.21, D2.11: 3DS accept not run under P0.9 (no challenging card). D1.22: no failing card under P0.9. The D-02 outcome for the sign-off is therefore "not run" in both directions.
- D1.24 (recovery part) and D1.RS5: unreachable on a live account, see Runbook corrections. D1.24 part 1 (accept, decline with and without reason, free bookings) passed per the user on 2026-11-03.
- D2.14 and D2.15: the expert account is already onboarded, so Payouts shows the ready state with no Set up payouts entry, and a new Connect account is forbidden by D-05. **The Android half of the payout-link bar (Custom Tab opens, return deep link, refresh) was never exercised.** D2.16 (forged return intent) passed, but weakly: the account was already complete, so a wrong mark-complete could not have shown.

Passes that rest on the user's word or lack one evidence item:
- D1.20: error text, payment reference and the no-duplicate check were not stated; airplane mode dropped wireless ADB, so the log is empty.
- D1.RS4: the Stripe status and copy seen were not stated. D1.29: the reports enum value was not sent. D1.PH3: which statuses were available was not stated.
- D1.25: the Railway confirm-connection count was not checked. The call dropped no audio the user reported, but dropouts were not stated; the log shows three short freezes.
- D2.04: the decline note is covered by BUG-28. D2.08: the Play build logs no `Realtime` lines, so live delivery rests on the user report. D2.13: the exact buffer outcome was not stated.
- D2.12: audibility during the 33 minutes (45 logged freezes) was not stated.
- D2.19: the exact login-rejected messages on the iPhone and the Samsung were not given. D2.FB1: the backend feedback row was not checked. D2.RS2: the booking id was never read (only PaymentIntent Hlqzg7).

Notes from the live rows:
- **D2.05 to D2.08, audio freezes.** The log shows `remote audio state=3 reason=1` freezes of 2-4 s: six in five minutes during D2.06 (starting before backgrounding), and continuing through D2.07. The user did not hear them (2026-11-04), so they are log-only with no BUG row; reopen if they become audible.
- **D2.06 notification.** Before the scheduled start the call notification shows "Starts at 11:00 p.m." with no chronometer; at 23:00:30 `showChronometer=true`. The runbook's "title and chronometer" holds only after the scheduled start. Not logged as a bug.
- **D2.05.** Both joined about 22 minutes before the start; `channelProfile:0`, `client_joined_at` and `expert_joined_at` both set, status `in_progress`.
- **D2.17 to D2.19.** Only the Throwaway was deleted; the user and Claude confirmed the signed-in email before each Delete tap. The 409 dialog appeared with the upcoming booking; after the expert cancelled it, the delete completed and both phones rejected the login. D2.CP5's 404 state was checked on the deleted Throwaway.
- **D2.PV1.** Run after the user cancelled the remaining free test bookings; the Past list opened (a cancel also lands on Past, per the user).

## Fix batch B input

**Blocking (android):** none.

**fix-if-cheap (android), status open:**
- BUG-30: Profile Setup Next button behind the navigation bar (`SignupOverviewScreen.kt`, add `navigationBarsPadding()`; sweeping other Scaffold bottomBar buttons may need more than 2 files, decide at the time).
- BUG-31: photo crop circle opaque black (`PhotoCropScreen.kt`, `BlendMode.Clear` shares the layer with the image).
- BUG-22: call controls hidden above End Call in the last 5 minutes (deferred from batch A).
- BUG-23: Past card offers Leave a review after an app restart (deferred from batch A).

Not fix work but still open for batch B: BUG-08 not exercised on a device; BUG-12's expert Dashboard label at the largest font was not read in Direction 2 either.

**Batch B re-test (not fixes):** the 70 settings rows moved by D-14, D1.24 and D1.RS5 recovery only if the user wants it despite the unreachable premise, and the Android client's response card on Upcoming Booking Details (to confirm Android handles the case that BUG-25 breaks on iOS; D2.RS2's iOS saved-card recovery could not be exercised for the same reason).

## Cross-repo fixes

None are confirmed "blocks Android release: yes". **BUG-25 (ios) is pending the user's decision** (see Questions). If the user says yes, the orchestrator edits the iOS repo sequentially without worktree isolation and commits (D-12).

## New bugs

| Bug | Source | Owner | Severity | Status |
|---|---|---|---|---|
| BUG-24 | SW.1: iOS logout leaves the push token | ios | fix-if-cheap, blocks Android release: no | reported (ios) |
| BUG-25 | D2.RS1: iOS client cannot respond to an expert reschedule | ios | blocking (prevents a booked session or call from happening), blocks Android release: user decision pending | reported (ios) |
| BUG-26 | iOS expert Upcoming card Reschedule does nothing | ios | fix-if-cheap, no | reported (ios) |
| BUG-27 | a deleted card may still be charged after a move (code reading only) | backend | fix-if-cheap, no | reported (backend) |
| BUG-28 | the expert's decline note never reaches the client | backend, ios, android | post-release, no | post-release |
| BUG-29 | booking ID missing on Payment History and expert bookings list rows | android | post-release | post-release |
| BUG-30 | Profile Setup Next behind the navigation bar | android | fix-if-cheap | open |
| BUG-31 | photo crop circle opaque black | android | fix-if-cheap | open |
| BUG-32 | runbook premise: no unsaved one-off card | runbook | post-release | post-release (no code change) |
| BUG-33 | show the full Client Profile without a booking | backend, android, ios | post-release | closed (won't do, user decision 2026-11-05) |

BUG-25 is classed against the rubric as "prevents a booked session or call from happening" because a proposed reschedule can never be answered and the booking sits in pending_reschedule. The rubric does not settle whether it blocks the Android release: the defect is in the live iOS app and equally affects an iOS expert's proposals, but Android experts add a new way to hit it. Not checked: whether the original slot can still be joined while the booking is pending_reschedule.

## Runbook corrections

These premises were wrong or unreachable. The Result cells carry a short note; the Steps and Expected text of the rows is not rewritten.

- **D1.RS5 and D1.24 recovery:** no unsaved one-off card exists. Both apps vault every card entered in the booking sheet, the booking row stores the card id, and a cached clone in `connected_pm_clones` is used with no Stripe call, so `PAYMENT_METHOD_REQUIRED` cannot be raised live (BUG-32, with BUG-27 for the deletion side effect).
- **D1.RS8:** the case is a slot exactly one hour before another booking, not a 30-minute gap. The iPhone expert's accept showed "Couldn't update booking, That time slot is no longer available" and the booking stayed Pending Reschedule, matching the known behavior (RESEARCH Open Question 5).
- **D2.04:** it needs five requests, not four (the fifth is the one the client cancels).
- **D2.08:** the Play build logs no `Realtime` lines in either session, so the "Evidence to capture" cannot be obtained there.
- **D2.12:** the Expected text "confirm-connection is sent once" should read once per app. Railway showed two confirm-connection lines, both 200, at 15:56:30 UTC, one with an uppercase booking id and one lowercase (one per app). Confirmed in code: Android sends it once per session (`CallCenter.confirmConnectionOnce`), and `call.controller.js` accepts `confirmed` and `in_progress`, so a second participant's call is an idempotent no-op write.
- **D2.14 and D2.15:** they cannot run on an already onboarded expert account, and D-05 forbids a new Connect account. They need an account that is not onboarded.
- **D2.16:** weak proof when the account is already complete.
- **D2.06:** the chronometer shows only after the scheduled start.

## Deviations from Plan

**1. [D-14 deviation, user decision] Settings rows moved to batch B.** The 70 Samsung setting-pass rows (TB.B1 and the rest of the TB., FS., PD., IM., PL. and EM. rows, plus FS.CP1) that Task 1 listed as "carried to session B" were not run in session B. The user chose option A: they run in the batch B re-test on the bug-fix build, which adds one extra account swap (a deviation from the single switch of D-14). Their Result cells now read `[-] not run: moved to batch B re-test (D-14 deviation, user option A, 2026-11-03)`. The dispatch for this step quoted the date as 2026-11-04; the results file and the user's choice record 2026-11-03, so the cell uses 2026-11-03.

**2. [Order] The session ran across four days (2026-11-03 to 2026-11-05) and was paused six times.** Direction 1 carried rows ran before the switch as planned. D2.14-D2.15 were attempted last and could not run. D2.PV1 was deferred until after D2.19, when the expert had no upcoming bookings.

**3. [Log format] BUG-32 has no app owner.** The log's Owner column has no value for a runbook premise, so the row reads `runbook (no app owner)` and `post-release (no code change)`.

**4. STATE.md not edited.** The plan lists STATE report-to-iOS lines as part of Task 3. The orchestrator owns STATE.md for this run, so the lines are returned below instead.

## Questions for the user

1. **BUG-25:** does the iOS client's inability to answer an expert reschedule block the Android release? If yes, the orchestrator fixes the iOS repo in this phase.
2. **BUG-24:** logging out on the iPhone leaves the push token, so the account can keep receiving pushes there. It was classed fix-if-cheap, not blocking, because it matches no D-07 clause. Is a privacy leak of that kind blocking to you?
3. **BUG-27:** a card the client deleted may still be charged at T-24h after a move. Only read in code. Do you want a live check, or leave it fix-if-cheap? (Charging the wrong amount is a blocking clause if it is proven.)
4. **Payout bar (D2.14, D2.15):** the Custom Tab opening and the return/refresh links were never exercised on Android. Accept this as a gap in Section 8, or arrange an account that is not onboarded?
5. **BUG-29 and BUG-28:** both are enhancement-style and left post-release; say so if you want either in batch B.

## Open money for Section 7

- Captured, refund by hand in the Stripe dashboard on the expert's connected account: D1.25 booking `5449f1b7`, PaymentIntent ending C3eR3Z, CA$5.00; D2.12 booking `43d0562b`, PaymentIntent ending See2oK, succeeded CAD 500, application fee 50.
- Voids to confirm in Stripe: BFQEmT, 53ka5k, b2m66n, lTnYmn, KJcrmX, Hlqzg7 (all reported canceled by the user). The D2.RS2 and D2.RS4 booking's first 8 characters were never read.

## STATE lines for the orchestrator

Add to Pending Todos, report-to-iOS list:

```
- [Phase 16] BUG-24 (ios): logout leaves the device_tokens push row (SW.1); blocks Android release: no
- [Phase 16] BUG-25 (ios): client cannot respond to an expert reschedule from the Upcoming Booking screen (D2.RS1, user rated critical); blocks Android release: user decision pending
- [Phase 16] BUG-26 (ios): expert Upcoming card Reschedule does nothing; blocks Android release: no
- [Phase 16] BUG-27 (backend): a deleted card may still be charged after a move (code reading, not proven live); blocks Android release: no
- [Phase 16] BUG-28 (backend, ios, android): the expert's decline note never reaches the client; blocks Android release: no
```

## Known Stubs

None. This plan changed only planning documents.

## Threat Flags

None. No code or endpoint was added. T-16-17 (forged `clickme://connect/return`) was exercised by D2.16 with the weak-proof caveat above; T-16-18 held (only the Throwaway was deleted, email confirmed before each Delete tap); T-16-19 is closed by the Paid bookings created table and the Section 7 list above.

## Task Commits

- Task 1: `ac0bc4c2` (session B opened on versionCode 4)
- Task 2: pause notes `f59925ba`, `7660a470`, `bf2c2d07`, `540db58f`, `d01b12c3`, `c2142e2e`; the results live outside the repo
- Task 3: `260a0182` (session B results and triage), then the commit that adds this file

## Self-Check: PASSED

- Verify block printed SESSION-B-RECORDED.
- `16-06-SUMMARY.md`, the runbook and the bug log exist; commit `260a0182` is an ancestor of HEAD.
- 124 results-file verdict lines map to 124 rewritten runbook Result cells; no row reads `[ ]` or "carried to session B"; the two `fail` cells cite BUG-24 and BUG-25, both present in the bug log.
- Eight new Paid bookings created rows for the eight PAID lines (ten in total with the earlier D1.18 and D1.23 rows).
