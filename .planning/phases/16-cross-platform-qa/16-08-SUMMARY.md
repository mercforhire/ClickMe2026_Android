---
phase: 16-cross-platform-qa
plan: 08
subsystem: qa
tags: [qa, close-out, write-back, sign-off, money-sweep, tracer]
requires:
  - phase: 16-cross-platform-qa
    provides: batch B re-test on versionCode 5 and the full matrix results (16-07)
provides:
  - Close-out record on the final Play build (versionCode 5, backend 2ec82af)
  - Runbook Section 8 with counts, 3DS outcome, money line, accepted gaps and the write-back table
  - Phase 16 results written into 15-DEVICE-CHECKLIST.md, 15-UAT.md and the STATE.md deferred items
  - Money sweep recorded (8 voids confirmed, 2 captures kept by user decision)
  - The user's sign-off (option-confirm, 2026-11-05)
affects: [phase 17, verify-work 16, next fix batch]
actuals:
  tokens: 52000
  tasks: 3
  commits: 3
plan_head_before: 59303f2db2277efda10c3454fd86586ea7d306c6
plan_head_after: febbe5bc707b21fe07596b2b72bcefcaaf1a1ccb
key-files:
  created: []
  modified:
    - .planning/phases/16-cross-platform-qa/16-MATRIX-RUNBOOK.md
    - .planning/phases/16-cross-platform-qa/16-BUG-LOG.md
    - .planning/phases/15-polish-hardening/15-DEVICE-CHECKLIST.md
    - .planning/phases/15-polish-hardening/15-UAT.md
    - .planning/STATE.md
key-decisions:
  - "User sign-off 2026-11-05: option-confirm (\"confirm\"). Phase 16 passes with the listed gaps accepted; Phase 17 may start (D-09)."
  - "D-04 deviation by user decision 2026-11-05: the 2 captures (C3eR3Z, See2oK, CA$5.00 each) were kept, not refunded, because the user owns both accounts and the platform. They stay in earnings, payout and revenue records; Stripe keeps its fee either way; the application fee was not refunded."
  - "TalkBack, POL-04 and Phase 15 SC-4 are not confirmed on a device (23 TalkBack rows skipped by the user 2026-11-05); stated as a gap, never as a pass."
  - "BUG-22, BUG-23, BUG-34 and BUG-28 (Android half) end as post-release."
requirements-completed: [QA-01, QA-02, QA-03]
completed: 2026-11-05
status: complete
commits: 3
---

# Phase 16 Plan 08: Close-out, money sweep and sign-off Summary

Phase 16 is closed on the user's explicit "confirm": every result is written back to its source, the live holds are settled, and the accepted gaps are on record for the verifier and for Phase 17.

## Accomplishments

- **Task 1, close-out record (52d4d076).** Harness H1-H4 on the final build: versionCode 5, installer com.android.vending, lastUpdateTime 2026-11-05 18:12:45, backend /health commit 2ec82af, crash buffer 0 (transport 8048). Session log row `close-out` added. 69 Section 6 rows ended as `not run in Phase 16: post-release (D-06)`. Section 8 filled. All 101 rows of 15-DEVICE-CHECKLIST.md carry a P16 result (17 pass, 5 partial, 1 fail, 78 not run). 15-UAT.md has a "Phase 16 device results" section for tests 1-7 (frontmatter unchanged, still `testing`). 80 STATE.md lines carry P16 outcomes; the D-10 line was added. Open Android fix-if-cheap bug rows (BUG-22, BUG-23, BUG-34, BUG-28) ended as `post-release`.
- **Task 2, money sweep (68693235).** All 8 voided holds confirmed canceled in the Stripe dashboard (WIUsEq, gAi5kj, BFQEmT, 53ka5k, b2m66n, lTnYmn, KJcrmX, Hlqzg7); 0 matrix PaymentIntents in requires_capture; no paid request pending; throwaway accounts deleted; the Samsung restored (font 1.0, density reset, animations 1.0, Don't keep activities off, TalkBack off, 3-button navigation, spike packages uninstalled). The two captures were not refunded (see Deviations).
- **Task 3, sign-off (febbe5bc).** Section 8 confirmation line reads `Confirmed by the user on 2026-11-05: option-confirm ("confirm")`; STATE.md carries `[Phase 16] Sign-off 2026-11-05: option-confirm`.

## Final Section 8 tally

| Scope | Pass | Fail | Not run |
|---|---|---|---|
| Direction 1 (QA-02) | 64 | 1 (D1.05, BUG-13 backend) | 6 |
| Direction 2 (QA-03) | 35 | 2 (SW.1 BUG-24 ios, D2.RS1 BUG-25 ios) | 3 |
| Tier-1 (Section 5) | 33 | 1 (FS.C3, BUG-34 android) | 36 |
| Pre-flight P0 | 9 | 0 | 1 |
| Smoke R1 | 4 | 0 | 1 |

Open android-owned blocking bugs at sign-off: 0. BUG-25 (ios, blocking severity, rated critical) stays open and reported to iOS; the user decided on 2026-11-05 that it does not block the Android release.

## Accepted gaps (accepted by the user's confirm)

- TalkBack, POL-04 and Phase 15 SC-4 are unconfirmed on a device. The 23 TalkBack rows, Accessibility Scanner (FS.Y12, TB.Y12) and 13 other tier-1 rows were skipped by the user on 2026-11-05; Phase 15 semantics are proven by Robolectric tests only (15-UAT tests 1, 3, 4 unconfirmed).
- No live 3DS challenge was passed on Android or iOS (D1.21, D2.11 not run; D1.22 not run). QA-02 and QA-03 "test cards including 3DS" is not met with a live challenge (D-02, P0.9 plan changed 2026-11-02).
- D-04 refund deviation: the 2 captures were kept (user decision).
- Never-run rows: payouts onboarding (D2.14, D2.15, expert already onboarded, D-05), large-window rows (Section 6.1), tier 2-4 rows (Sections 6.2 and 6.3), photo picker on API 26-32 (no AVD), JPY (no JPY topic), saved-card reschedule recovery (D1.24 part 2, D1.RS5: BUG-32 premise, BUG-27 backend), D1.PH5, D1.12 (no headset), EM.1 (Android 17 emulator row).
- D-10: the user is the only internal tester; the SC-5 and REL-05 "few real users on a mix of devices" part is accepted at release (carries Phase 13 D-22).
- Android post-release bugs: BUG-22 and BUG-34 (call controls at font 200%), BUG-23, BUG-28 (Android half), BUG-04, BUG-18, BUG-19, BUG-20, BUG-21.
- iOS and backend bugs travel on the STATE report list: BUG-13, 14, 15, 16, 17, 24, 25, 26, 27, 28.

## Next fix batch input (android)

- BUG-22 and BUG-34 together (both in `MeetingCallScreen.kt`: End Call pinned over a scrolling column, worse at font 200%).
- BUG-23 (Leave a review offered again after an app restart).
- The remember-login-email feature (pre-fill the last email on Login after any sign-out): branch `feature/remember-login-email`, Android commit 0c9ecc55, iOS commit b0629d9 (for the user to merge and ship). The temporary worktrees were removed; the branches remain in the two repos.
- The Android half of BUG-28 (show the expert's decline note) once the backend exposes `decline_reason_text`.
- Also open: BUG-08 not exercised on a device; the expert Dashboard tab label at the largest font not read.

## Deviations from Plan

### User-directed

**1. [D-04 deviation, user decision] Captures not refunded**
- **Found during:** Task 2
- **Issue:** The plan and D-04 require every captured matrix charge to be refunded in full in the Stripe dashboard.
- **Resolution:** The user decided on 2026-11-05 not to refund C3eR3Z (D1.25, booking 5449f1b7) and See2oK (D2.12, booking 43d0562b), CA$5.00 each, because they own both accounts and the platform. Recorded in Paid bookings created, E7.4, Section 7, the Section 8 money line and the accepted gaps. The charges remain in earnings, payout and revenue records; the application fee was not refunded.
- **Commit:** 68693235

### Auto-handled

**2. [Rule 3 - Blocking] Branch `gsd/phase-16-plan-08`**
- The commit guard refuses `main`, so the work was done on this branch in the same checkout, as the dispatch allowed. It is not merged.

**3. Runbook Section 8 sources and wording**
- Counts are by Result cell and a pass can carry a caveat; the caveats are listed in Section 8. Rows partly run (for example PD.C7, direct relaunch only) are counted as passes and flagged.

## Auth gates

None.

## Known Stubs

None. This plan changed planning documents only.

## Threat Flags

None. No code, endpoints or schema changed. No script touched production; the T-16-26 prohibition on bulk cleanup scripts held.

## Self-Check: PASSED

- Commits 52d4d076, 68693235 and febbe5bc exist on `gsd/phase-16-plan-08` (verified with `git merge-base --is-ancestor`).
- The three task verifies print CLOSEOUT-RECORDED, MONEY-SETTLED and SIGNOFF-RECORDED.
- 16-MATRIX-RUNBOOK.md, 16-BUG-LOG.md, 15-DEVICE-CHECKLIST.md, 15-UAT.md and STATE.md all exist.
