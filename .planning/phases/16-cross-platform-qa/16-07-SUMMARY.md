---
phase: 16-cross-platform-qa
plan: 07
subsystem: qa
tags: [qa, bug-fix, regression-tests, release, versioncode, fix-batch, tracer]
requires:
  - phase: 16-cross-platform-qa
    provides: session B results and the Fix batch B input (16-06)
provides:
  - BUG-29, BUG-30, BUG-31 fixed with regression tests and verified on versionCode 5 from Google Play
  - Signed, archived versionCode 5 bundle, ledgered (Uploads row 5, Fix batches row B)
  - Batch B re-test: 34 of the 70 deferred settings rows run (33 pass, 1 fail), 36 skipped by user decision
  - New BUG-34 and the next-batch input list
affects: [16-08]
key-files:
  created:
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/signup/profilephoto/PhotoCropCircleRegressionTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/signup/SignupBottomBarInsetRegressionTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/BookingIdOnListRowsRegressionTest.kt
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/profilephoto/PhotoCropScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/overview/SignupOverviewScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/signup/review/SignupReviewScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/payments/ClientTransactionRow.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/upcomingbookings/subviews/ExpertUpcomingCard.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/upcomingbookings/subviews/ExpertPastCard.kt
    - app/build.gradle.kts
    - .planning/phases/16-cross-platform-qa/16-BUG-LOG.md
    - .planning/phases/16-cross-platform-qa/16-BUILD-LEDGER.md
    - .planning/phases/16-cross-platform-qa/16-MATRIX-RUNBOOK.md
key-decisions:
  - "Batch B fix-if-cheap budget (3 rows) went to BUG-31, BUG-30 and BUG-29 (BUG-29 was moved into batch B by the user on 2026-11-05); BUG-22 and BUG-23 are deferred to the next batch"
  - "BUG-28 Android half not started: no backend read exposes decline_reason_text yet, so the backend change is a cross-repo fix that comes first"
  - "User decision 2026-11-05: BUG-34 (call screen controls unreachable at font 200%) is not blocking; fix it with BUG-22 in the next batch"
  - "User decision 2026-11-05: 36 of the 70 D-14 rows skipped (all 23 TalkBack rows and 13 others); TalkBack (POL-04 / Phase 15 SC-4) stays unconfirmed on a device"
requirements-completed: []
completed: 2026-11-05
status: complete
commits: 10
plan_head_before: 10134dcb5fa427a0898832833708fecf54161050
plan_head_after: 0af047fd40951688518a9eea81a763fb36489aa8
actuals:
  tokens: 5900
  tasks: 3
  commits: 10
---

# Phase 16 Plan 07: Fix Batch B Summary

**Three fix-if-cheap Android bugs (photo crop circle, Profile Setup and Publish buttons behind the navigation bar, booking ID on list rows) were fixed with regression tests, shipped as versionCode 5 through the Play internal track, and verified on the Samsung. The smoke passed 5 of 5. One new bug (BUG-34) came out of the 200% font rows.**

The commit count is the ten commits from `plan_head_before` to `plan_head_after`; the commit that adds this file follows them. Work ran on branch `gsd/phase-16-plan-07` in the main checkout because the commit guard refuses `main`; the orchestrator merges it.

## What was fixed

No blocking Android bug was open after session B (BUG-25 was decided as not blocking Android on 2026-11-05).

| Bug | Fix | Commit | Regression test | Re-test (vc 5, 2026-11-05) |
|---|---|---|---|---|
| BUG-31 | Scrim is an even-odd path with the circle cut out; `BlendMode.Clear` in the shared layer had erased the photo | e928cc8e, 32a88358 | Robolectric native-graphics pixel test; failed before (near-black pixel inside the circle) | verified (D2.17) |
| BUG-30 | `navigationBarsPadding()` on the Profile Setup Next button and on the Review Publish bar | 5c3be4c4 | Dispatches a 48 dp nav-bar inset; both buttons failed before | verified (D2.17) |
| BUG-29 | `ID: #A1B2C3D4` on Payment History rows and expert Upcoming and Past cards, reusing the Booking Summary format and strings | 33951e9c | Three row tests; all failed before | verified (D2.RS3 expert, D1.PH1 client) |

The client's own Upcoming and Past cards were not named in BUG-29 and are unchanged.

Not fixed, and why:
- **BUG-22 and BUG-23:** deferred to the next batch (budget of 3 fix-if-cheap rows used).
- **BUG-28 Android half:** the backend (2ec82af) exposes `decline_reason_text` on no client booking read.

## Release

- Gate B2 green on 85423656: 5276 tests in the dev flavor, 0 failures; prod unit tests, ktlint, `assembleDevDebug`, `lintDevDebug`, `lintProdDebug` (at -Xmx4g), design rules and the self-test. The design-rules script caught a color literal in my first BUG-31 test; the follow-up 32a88358 re-ran only the photo tests, ktlint and design rules, because it is test-only.
- `chore(release): versionCode 5` is d5fd82d9 and changes only that line. The bundle was built from it and passes B4: jar verified, apksigner v2, certificate equal to the pin, versionCode 5, `google_app_id` present, 0 tracked secrets.
- Archive: `/Users/LeonChen/ClickMe-release/clickme-0.1.0-5.aab` (sha256 `5da687b5bfeeef81...`), `.apk` and `mapping-0.1.0-5.txt`.
- Uploaded 2026-11-05, Play status available. Samsung proof (H1-H2): SM-A075F versionCode 5, installer `com.android.vending`, lastUpdateTime 2026-11-05 18:12:45, after the 17:59 archive.
- Archive incident 2: the vc 1-4 aab, apk and mapping files were gone from the archive folder; the user deleted them to save disk space. The vc 4 mapping can no longer retrace session B stack traces.

## Re-test results

Source: `/Users/LeonChen/ClickMe-release/sessions/batch-B-results.md`; logs under `/Users/LeonChen/ClickMe-release/logs/batch-B/` have 0 FATAL EXCEPTION lines.

- **Smoke, 5 of 5 pass:** R1.1 login; one free booking (booking id not read); one chat message; one call join (log: conn state=3 at 19:32:27.796, remote joined, remote audio decoding, left channel 19:32:40); one push to a swiped-away app (H6: notification arrived, tap opened the chat).
- **BUG-25 Android half:** user-reported pass. The Android client answered an expert reschedule on Upcoming Booking Details. There is no log evidence (the release build logs no reschedule API lines), and accept versus decline was not stated.
- **70 D-14 settings rows:** 34 run, 36 skipped by user decision.

| Group | pass | fail | not run |
|---|---|---|---|
| TB (TalkBack, 23) | 0 | 0 | 23 |
| FS (200% font, 17) | 14 | 1 (FS.C3, BUG-34) | 2 (FS.FB1, FS.Y12) |
| PD (process death, 15) | 14 | 0 | 1 (PD.Y3) |
| IM (IME, 12) | 5 | 0 | 7 (IM.K7, K8, K9, C5, Y8, Y9, Y16) |
| PL and EM (3) | 0 | 0 | 3 (PL.C9, PL.K12, EM.1) |
| Total (70) | 33 | 1 | 36 |

Partial passes, noted in the runbook Result cells:
- **PD.C7:** pass on the direct-relaunch half. The push half is not testable on Android: a force-stopped app gets `result=CANCELLED` on the GCM broadcast (21:48:31.679), a platform behavior, not a ClickMe bug. The swiped-away push path is proven by the smoke.
- **FS.SP1:** pass on the Samsung; the emulator landscape half was not run (left with EM.1).
- **FS.B10:** whether a paid Request Sent was also viewed was not stated.
- **FS.K6:** the blocked banner and Request Decision footer were not viewed.
- **PD.C8:** the FCM wake-up-dies-before-UI marker sub-case was not exercised.
- **IM.L6:** whether the 6-digit code was offered as an autofill suggestion was not stated.
- **FS.PN1:** client side only; the expert side was checked at FS.CP1.
- **FS.C3:** fail. At font 200% with the largest screen zoom the pre-join call screen does not scroll, and Mute, Speaker, Minimize and Chat are unreachable behind the pinned End Call. The Connected pair, final-5-minute banner and external variant were not viewed.

**TalkBack (POL-04 / Phase 15 SC-4) stays unconfirmed on a device.** The user skipped all 23 TB rows on 2026-11-05 (option 1 of 3); the semantics are covered by Robolectric tests only. FS.Y12 (Accessibility Scanner) was declined with them.

## Cross-repo fixes

None block the Android release. BUG-28 needs a backend change first: expose `decline_reason_text` on the client booking read. That runs sequentially without a worktree with the orchestrator committing (D-12); the iOS half stays reported.

## New bug

| Bug | Source | Owner | Severity | Status |
|---|---|---|---|---|
| BUG-34 | FS.C3: call screen controls unreachable at font 200% | android | fix-if-cheap (user decision 2026-11-05: not blocking) | open |

BUG-34 shares its cause with BUG-22 (End Call pinned over a scrolling column with a 200 dp avatar, `MeetingCallScreen.kt`), and is worse because the screen does not scroll at all. Evidence: `logs/batch-B/FS.C3-200pct.png`.

## Next batch input

- BUG-22 and BUG-34, together (`MeetingCallScreen.kt`).
- BUG-23 (Past card offers Leave a review after an app restart).
- The Android half of BUG-28 once the backend exposes `decline_reason_text`.
- Feature request, not a bug: pre-fill the last email on Login after any sign-out (user request 2026-11-05). It is committed on branch `feature/remember-login-email`: Android `0c9ecc55` (worktree `/Users/LeonChen/Documents/ClickMe_Android-remember-email`, gate green, 5292 tests) and iOS `b0629d9` (worktree `/Users/LeonChen/Documents/ClickMe2026-remember-email`, for the user to merge and ship to TestFlight).
- Still open and not fixes: BUG-08 not exercised on a device; the D2.14 and D2.15 payout-link gap (accepted as untested); the expert Dashboard label at the largest font.

## Deviations from Plan

**1. [Scope, user decision] BUG-29 counted against the fix-if-cheap budget and touched 3 files.** The plan caps fix-if-cheap rows at 2 files each. The user moved BUG-29 into batch B, so I accepted the third file.

**2. [Plan text] The Fix batches row is `B`, not `A`.** The plan text says row A in several places; batch A already exists.

**3. [Rule 3] The design-rules gate rejected a `Color(0x...)` literal in the BUG-31 test.** Fixed in 32a88358 with a platform color constant.

**4. [Process] Batch B ran on branch `gsd/phase-16-plan-07`** because the commit guard refuses `main`.

**5. [Ledger] The runbook Session log "Backend /health" cell for batch-B reads `2ec82af (backend repo HEAD, /health not queried)`.** The batch-B re-test did not query /health.

STATE.md and ROADMAP.md were not edited; the orchestrator owns them for this run.

## Known Stubs

None. The changes add display text and layout insets; no placeholder data.

## Threat Flags

None. No endpoint, auth path or schema was added. T-16-20 held: the bundle was built from the main checkout after the `chore(release)` commit, with the certificate, versionCode and `google_app_id` checks passing. T-16-21 held: every verified bug was re-tested on the Play-installed build proven by H2. T-16-22 held: no iOS or backend repo was edited by this plan.

## Task Commits

- Task 1: `e928cc8e` (BUG-31 fix and test), `f595e3aa` (bug-log row)
- Task 2: `5c3be4c4` (BUG-30), `33951e9c` (BUG-29), `32a88358` (test color constant), `fe460703` and `85423656` (bug-log), `d5fd82d9` (versionCode 5), `5ed70422` (ledger)
- Task 3: `0af047fd` (batch B re-test results), then the commit that adds this file

## Self-Check: PASSED

- The Task 3 verify printed BATCH-B-RETESTED (SM-A075F transport 8048: versionCode 5, installer `com.android.vending`; ledger row 5 available; no `fixed (pending re-test)` rows; `batch-B` Session log row present). Tasks 1 and 2 verifies printed FIRST-FIX-B-GREEN and BATCH-B-BUILT.
- `grep -c 'moved to batch B re-test'` and `grep -c 'carried to batch B re-test'` on the runbook both print 0; the 70 rewritten Result cells map to the 70 verdicts in the results file.
- `git ls-files | grep -cE '\.jks$|keystore\.properties'` prints 0.
- The fix commits e928cc8e, 5c3be4c4, 33951e9c and the release commit d5fd82d9 are ancestors of HEAD.
