---
phase: 16-cross-platform-qa
plan: 03
subsystem: qa
tags: [qa, preflight, live-money, release-smoke]
requires:
  - phase: 16-cross-platform-qa
    provides: matrix runbook, bug log, build ledger, internal track
provides:
  - Preflight session on versionCode 3 from Google Play, 13 rows pass, 2 not run, 0 fails
  - Live-money plan on record in runbook P0.9 and STATE Decisions
affects: [16-04, 16-05, 16-06, 16-07, 16-08]
key-files:
  created:
    - .planning/phases/16-cross-platform-qa/16-03-SUMMARY.md
  modified:
    - .planning/phases/16-cross-platform-qa/16-MATRIX-RUNBOOK.md
    - .planning/phases/16-cross-platform-qa/16-BUG-LOG.md
    - .planning/phases/16-cross-platform-qa/16-BUILD-LEDGER.md
    - .planning/STATE.md
key-decisions:
  - "Money plan option-a stays; the user changed the 3DS route to accept not run on 2026-11-02"
  - "16.5 WR-01 fixed: Cancel is hidden after a confirmed or pending_reschedule session ends (deliberate iOS deviation)"
  - "Splash matches iOS type sizes; the system splash shows no icon because Android 12+ crops it and cannot show text"
requirements-completed: []
completed: 2026-11-02
status: complete
---

# Phase 16 Plan 03: Preflight and Live-Money Plan Summary

**The matrix can start: versionCode 3 is proven from Google Play on the Samsung, accounts, topics, availability and a card are ready, and the live-money plan is on record. No real charge was made.**

## Results

| Rows | Result |
|---|---|
| P0.1-P0.7, P0.9, P0.10 | pass |
| R1.1, R1.3, R1.4, R1.5 | pass (R1.5 read from dumpsys) |
| P0.8 | not run: skipped by the user, who states access works; A's Connect status was not read |
| R1.2 | not run: the release build logs no `state=` lines |

Build proof: installer `com.android.vending`, versionCode 3, lastUpdateTime 2026-11-02 20:43:25, crash count 0, backend `8d4450f`.

## Task Commits

- Task 1: `fa66325e` (vc 1), reopened on vc 3 in `9e0010fb`
- Task 2: `e4ebd9f0`; 3DS route updated by the user during Task 3
- Task 3: the commit that adds this file

## Deviations from Plan

1. **Preflight moved from vc 1 to vc 3.** The parity phases paused it after Task 2. The vc 1 results file was lost with the emptied archive, so Task 1 re-created it.
2. **Code changed during preflight, at the user's request.** WR-01 (`f2d6745c`), system splash (`2466fc09`), iOS splash sizes (`cb3b68c6`), then versionCode 3 (`5e776574`). The full B2 gate passed on `cb3b68c6` with 5238 tests per flavor.
3. **vc 2 was uploaded but superseded.** It reached the Samsung for about 16 minutes before vc 3, and no rows ran on it.
4. **Debug sideload removed.** A debug build on the Samsung was blocking Play updates and was uninstalled with the user's approval. The phone was also force-stopped and relaunched once at 19:27 for a splash capture, without asking first.
5. **P0.3 slot times were not reported**, so session A confirms a slot inside the join window.

## Issues Encountered

- `/Users/LeonChen/ClickMe-release/` was found empty, cause unknown. The vc 1 AAB, APK and mapping were restored from build outputs with matching hashes.
- BUG-01 and BUG-02 are verified; BUG-03 and BUG-04 are post-release.

## Open for Later Plans

- D1.21, D2.11 (3DS) and D1.22 (decline) will be not run. The sign-off must say the live 3DS criterion was not met, for the user to accept or reject.
- If D1.18 fails, read A's Connect status first (P0.8 was skipped).
- D1.PH5 and D1.SP1 check the WR-01 and splash fixes on the device.
- Open from 16-02: closed-testing recruitment, which the user has said to disregard as a deadline for now.

## Self-Check: PASSED
