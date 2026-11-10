---
phase: 17-play-store-release
plan: 10
subsystem: release
tags: [human-gate, production, smoke-test, close-out]
requires:
  - phase: 17-09
    provides: smoke run 1 passed on vc 6, gate not shown
provides:
  - ClickMe 1.0.0 (versionCode 6) live on Google Play, 100%, Canada and United States
  - smoke run 2 passed from the public listing
  - REL-04, REL-06, REL-07 and POL-02 complete; Phase 13 steps 7 and 9 closed
affects: [milestone v1.0]
key-files:
  created:
    - .planning/phases/17-play-store-release/17-10-SUMMARY.md
  modified:
    - .planning/phases/17-play-store-release/17-RELEASE-CHECKLIST.md
    - .planning/phases/16-cross-platform-qa/16-BUILD-LEDGER.md
    - .planning/phases/13-push-notifications-internal-testing/13-RELEASE-CHECKLIST.md
    - .planning/REQUIREMENTS.md
    - .planning/STATE.md
key-decisions:
  - "Promoted vc 6, the exact smoke-tested binary, at 100% to Canada and the US (D-11, D-12)"
  - "Used the existing Production draft (Promote was greyed out: 'Track already has a draft release') with vc 6 added from the library"
requirements-completed: [REL-04, REL-06, REL-07, POL-02]
completed: 2026-11-10
---

# Phase 17 Plan 10: Production release and smoke run 2 Summary

**ClickMe 1.0.0 (versionCode 6) is live on Google Play in Canada and the United States, and a fresh install from the public listing signed up, booked with the iOS expert and completed a call with no crashes.**

## Tasks

| Task | Result |
|------|--------|
| 1 Promotion gate | PASS on all five checks, promote vc 6 (6246b10b) |
| 2 Production | Submitted 2026-11-06 ~23:10 (100%, CA+US, managed publishing off); review passed; listing 200 for gl=CA and gl=US on 2026-11-10 17:23 (f04326ba) |
| 3 Smoke run 2 + close-out | S2.00-S2.05 pass, S2.06 skipped (25147a4f); requirements and checklists closed |

## Smoke run 2

| Row | Result |
|-----|--------|
| S2.00 | listing 200 for CA and US |
| S2.01 | public install, versionCode 6 / 1.0.0, installer com.android.vending, firstInstallTime 2026-11-10 17:29:47 |
| S2.02 | new client signup |
| S2.03 | free topic booked and accepted, pushes on both phones |
| S2.04 | two-way audio |
| S2.05 | 0 crash lines, 0 FATAL/ANR, CallService stopped, no ClickMe notification left |
| S2.06 | skipped by the user |

## Deviations from Plan

1. **Production draft.** Promote to Production was greyed out ("Track already has a draft release"), probably from the gate check on 2026-11-06. The user finished that draft with vc 6, the same bundle, so the smoke-tested binary still went out (T-17-27 holds: S2.01 shows vc 6).
2. **"Production live" date.** The user reported it without a date. 2026-11-10 is the date Claude confirmed the listing returned 200. Google's approval may have come earlier.
3. **POL-02 ticked on JVM evidence.** 17-01's three screens are covered by JVM tests. The real-kill device check stays deferred under the manual-testing policy (17-01 SUMMARY).
4. **S2.06 skipped.** The Samsung's Google account is not opted into Internal testing, and the smoke client still exists. Rejoin the program before the next fix build.
5. **Regex typo.** Task 1's check uses `production access (approved|: no gate)`, which needs a space before the colon. It was evaluated with `production access ?(...)` (see 17-09 SUMMARY).
6. **Reviewer re-seed before submission (section 14 step 1)** was not confirmed by the user. Review passed regardless.

## Known open after release

See checklist section 16: forced-logout push-token DELETE (backend), `delete_account_body` wording, deferred CONTEXT deviations, the iOS sync todo, optional removal of unused Stripe redirect activities, and leftover test bookings to cancel.
