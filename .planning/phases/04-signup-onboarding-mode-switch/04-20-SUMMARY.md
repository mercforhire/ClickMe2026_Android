---
phase: 04-signup-onboarding-mode-switch
plan: 20
subsystem: verification
tags: [phase-gate, verification, deferred-human-verify, r8]
requires: [04-10, 04-19]
provides:
  - Green full CI-equivalent gate for Phase 4 (both flavors, R8)
  - Completed 04-VALIDATION.md (38 task rows, nyquist_compliant, wave_0_complete)
  - "[Phase 4] DEFERRED (human)" checklist and iOS-report todo in STATE.md
affects: [STATE.md, 04-VALIDATION.md]
tech-stack:
  added: []
  patterns: []
key-files:
  created: []
  modified:
    - .planning/STATE.md
    - .planning/phases/04-signup-onboarding-mode-switch/04-VALIDATION.md
key-decisions:
  - "Device checks for ROADMAP Phase 4 SC-1..SC-4 are deferred, non-blocking open items (manual-testing-deferred preference)"
requirements-completed: [SIGN-01, SIGN-02, SIGN-03, SIGN-04, SIGN-05, ONB-01, ONB-02]
metrics:
  duration: ~15 min
  completed: 2026-10-21
---

# Phase 4 Plan 20: Phase Gate Summary

Phase 4 passes the full automated gate on both flavors, including the unsigned R8 `assembleProdRelease`. The validation map is signed off and the on-device checks and iOS follow-ups are logged as open items in STATE.md.

## Gate results (Task 1)

Command: `./gradlew ktlintCheck lintDevDebug lintProdDebug testDevDebugUnitTest testProdDebugUnitTest assembleDevDebug assembleProdDebug assembleProdRelease` followed by `bash scripts/check-design-rules.sh`.

| Check | Result |
|-------|--------|
| Gradle gate (all tasks above) | BUILD SUCCESSFUL, exit 0 (208 actionable tasks, 2m 57s) |
| testDevDebugUnitTest | 809 tests, 0 failures, 0 errors, 0 skipped (Phase 3 baseline 421) |
| testProdDebugUnitTest | 809 tests, 0 failures, 0 errors, 0 skipped |
| ktlintCheck, lintDevDebug, lintProdDebug | pass |
| assembleDevDebug, assembleProdDebug | pass |
| assembleProdRelease (R8, minifyProdReleaseWithR8) | pass (T-04-52 mitigated) |
| scripts/check-design-rules.sh | `Design rules: OK` (T-04-53 mitigated) |

Grep sweeps (all printed nothing):
- `SignUpPlaceholderScreen|SignupSetupPlaceholder|debugSwitchScaffold` in `app/src`: none
- `fontSize` in `app/src/main/java` outside `ui/theme`: none
- `val password` in the `navigation` package: none
- `savedStateHandle[...password]` in `app/src/main/java`: none

04-VALIDATION.md: frontmatter set to `status: complete`, `nyquist_compliant: true`, `wave_0_complete: true`. A 38-row task map was added (`04-NN-T#` for plans 04-01..04-19, with wave, requirement, test patterns, file-exists, status green). Each referenced test class was confirmed to exist under `app/src/test`. Wave 0 and Sign-Off boxes are ticked, the requirement-level rows are green, and the Manual-Only rows are marked "deferred (non-blocking)". Approval recorded.

## Deferred device checklist (Task 2)

Added to STATE.md under Blockers/Concerns as `[Phase 4] DEFERRED (human)` (non-blocking), to run on the Samsung SM-A075F with the devDebug build against production:

- [ ] SC-1 Full signup to publish, with a real emailed code, ending on client home with the carousel
- [ ] SC-2 Verify Email behavior (nothing sent on open, 30 s resend cooldown, inline wrong-code error)
- [ ] SC-3 Resume after force-stop, logout from hub, offline-launch then foreground re-check (D-11)
- [ ] SC-4 Carousel shows once, Mode Switcher to Expert and back, expert mode persists across relaunch
- [ ] Photo picker on API 26-32, 200% font scale and keyboard checks on all 12 surfaces, ModalBottomSheet with IME, TalkBack crop actions
- [ ] Consequence to watch (D-17, D-23): client-only account tapping Expert Mode resumes into setup on every launch until it publishes or logs out

Pending Todo added: report to the iOS side the Phase 4 port findings (publish without photo fails, province required on iOS but optional on server, verified-email state lost on resume, alphabetical tags/languages make the primary tag accidental, country_code not normalized on publish, zoom slider does nothing).

STATE.md Current Position and Session Continuity now say Phase 4 execution is complete pending the deferred device checks. The existing Phase 2 and Phase 3 blocks are unchanged.

## Aggregated deviations from plans 04-01..04-19

Taken from the per-plan SUMMARY files (none were applied in this plan):
- 04-03 [Rule 3] Robolectric legacy graphics cannot create ImageBitmap (test workaround)
- 04-05 [Rule 1] Crop rect clamping: cap-side-then-shift to avoid non-square regions
- 04-06 [Rule 1] Cancel in-flight mode switch when the session ends
- 04-08 [Rule 2] Vertical scroll on each onboarding page for large font scales
- 04-15 [Rule 1] `KeyboardType.NumberPad` does not exist in Compose; used `KeyboardType.Number`
- 04-17 `joinToString` lambdas cannot call `stringResource` (compile fix only)
- 04-18 [Rule 3] Avatar click passed by name; KDoc avoids the word "Dialog" for an acceptance grep
- 04-19 No separate RED commit (plan type is `execute`, not `tdd`)
- 04-07 and 04-12: none

## Deviations from Plan

None - plan executed as written. The gate passed on the first run, so no `fix(04-20)` commits were needed.

## Auth gates

None.

## Known Stubs

None. The grep sweep confirms the placeholder screens and the debug mode-switch scaffold are gone.

## Threat Flags

None. This plan adds no code.

## Commits

- 413814b: docs(04-20): complete Phase 4 validation map and sign-off
- STATE.md deferred checklist and SUMMARY: committed together as the final docs commit

## Self-Check: PASSED

- 04-VALIDATION.md: `nyquist_compliant: true` count 1, `wave_0_complete: true` count 1, 38 task rows
- STATE.md: `[Phase 4] DEFERRED (human)` count 1, iOS todo count 1, four new SC lines
- Commit 413814b exists
