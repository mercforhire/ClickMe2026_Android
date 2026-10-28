---
phase: 13-push-notifications-internal-testing
plan: 17
subsystem: release-gate
tags: [gate, release-build, sweeps, state, follow-ups]
requires:
  - phase: 13-01..13-16, 13-18
    provides: all Phase 13 code and plan SUMMARYs
provides:
  - Green automated gate for Phase 13 (both flavors, ktlint, lint, R8 release, design rules, backend suite at baseline)
  - 13-VALIDATION.md signed off (nyquist_compliant true, wave_0_complete true)
  - STATE entries: Phase 13 DEFERRED (human) checklist, backend notes, iOS follow-ups, open items, hand-offs
  - ROADMAP Phase 14 note and REQUIREMENTS PUSH-05 / REL-05 notes
affects: [14, 15, 16, 17]
tech-stack:
  added: []
  patterns: [three separate Gradle gate invocations with explicit jvmargs, no gradlew --stop]
key-files:
  created: []
  modified:
    - .planning/phases/13-push-notifications-internal-testing/13-VALIDATION.md
    - .planning/STATE.md
    - .planning/ROADMAP.md
    - .planning/REQUIREMENTS.md
key-decisions:
  - "Device, two-client and Play Console checks are logged as the non-blocking [Phase 13] DEFERRED (human) list next to the 13-14 release checklist entry; nothing blocks on them"
  - "The manual validation row (REL-04 / REL-05) is marked deferred (human) rather than green, because the Play Console outcome is not verifiable from the repo"
requirements-completed: [PUSH-01, PUSH-02, PUSH-03, PUSH-04, PUSH-05, REV-01, REL-04, REL-05]
status: complete
commits: 2
plan_head_before: f4ab548fb7db08931fd2107aad4987d37e598340
plan_head_after: 9a4d38d
actuals:
  tokens: 6000
  tasks: 2
  commits: 2
duration: ~30min
completed: 2026-10-28
---

# Phase 13 Plan 17: Phase close-out gate Summary

The full Phase 13 gate is green (3614 unit tests per flavor, ktlint, lint, R8 `assembleProdRelease`, design rules, Stripe spike build), the scope and privacy sweeps are clean, the backend suite is at its known baseline, and the validation map is signed off with every device and Play Console check logged as a non-blocking deferred list.

## Tasks

1. Task 1 (tracer): sweeps, three-invocation gate, backend suite, validation map. Commit `a14153c`.
2. Task 2: STATE deferred list, backend notes, iOS follow-ups, open items, hand-offs; ROADMAP Phase 14 note; REQUIREMENTS PUSH-05 and REL-05 notes. Commit `9a4d38d`.

## Verification

- Gate 1: `testDevDebugUnitTest testProdDebugUnitTest ktlintCheck assembleDevDebug assembleProdDebug :spikes:stripe:assembleDebug` with `-Dorg.gradle.jvmargs=-Xmx4g -Pkotlin.daemon.jvmargs=-Xmx3g --stacktrace`: BUILD SUCCESSFUL (4m 26s). Counts from the JUnit XML: devDebug 3614 tests, 0 failures, 0 errors, 0 skipped; prodDebug 3614 tests, 0 failures, 0 errors, 0 skipped.
- `bash scripts/check-design-rules.sh` and `bash scripts/check-design-rules.sh spikes`: both "Design rules: OK".
- Gate 2: `lintDevDebug lintProdDebug`: BUILD SUCCESSFUL (2m 11s), no lint errors.
- Gate 3: `assembleProdRelease` (R8): BUILD SUCCESSFUL (2m 35s). The unsigned APK's dex files contain `NotificationsKey`, `NotificationsOrigin`, `WriteReviewKey`, `ReviewOpener`, `ClickMeMessagingService`, `NotificationItemJsonAdapter` and `ReviewContextDataJsonAdapter` under their original names (R8 mapping.txt omits identity-mapped app classes, so the dex was checked directly).
- Backend `npm test -- --silent`: Test Suites 1 failed, 120 passed, 121 total; Tests 6 failed, 2436 passed, 2442 total. The only failing suite is `tests/services/redis.test.js` ("REDIS_URL is not set"), the known baseline.
- Sweeps over the Phase 13 roots (`screens/push`, `services/push`, `screens/notifications`, `screens/clientmode/writereview`, `screens/clientmode/reviews`, the five manager files, `NotificationBellButton.kt`, `BookingSummaryFeedback.kt`):
  - `Text("[A-Za-z]` and `contentDescription/stateDescription/onClickLabel = "`: no hits (so no `@Preview` literals to list).
  - `fontSize|Color(0x`: no hits.
  - `Log.x(` / `println(`: no hits (privacy prohibition, T-13-43).
  - `BadgedBox|markRead|mark_read|is_read|unread` in `screens/notifications` and `NotificationBellButton.kt`: one hit, a KDoc comment ("No badge and no dot: the list has no unread state (D-16)") at `NotificationBellButton.kt:20`. It is documentation, not code.
  - `"tags"` in `ReviewRequests.kt`: no hits. `AlertDialog` in `PushPermissionRequester.kt`: no hits.
  - `git ls-files app/google-services.json`: empty (never tracked).
  - `PHASE-14` in `screens/push`: two lines (`PushDestination.kt:57`, `PushDestinationResolver.kt:104`).
  - Backend `push.js` apns literal count: 1 (iOS message unchanged).
- Acceptance greps: `nyquist_compliant: true` count 1, `⬜ pending |` count 0 in 13-VALIDATION.md; all five Task 2 greps print 1; `DONE.*13-07` prints 3; `PHASE-14` in STATE prints 1; `D-22` in REQUIREMENTS prints 1.

## Deviations from Plan

None - plan executed exactly as written. Notes:

- No gate step failed, so no Rule 1-3 fixes were needed.
- All 40 test classes named in the validation map exist (checked by file name), so no row needed a corrected class name; the Status column went green and the File Exists column to "exists". The manual REL-04 / REL-05 row is marked "deferred (human)" instead of green.
- The plan's "commit on a gate branch" note is satisfied by the worktree branch. As the orchestrator authorized, only the plan-specified content edits were made to STATE, ROADMAP and REQUIREMENTS; STATE position, progress, session and status fields, and the ROADMAP progress table and phase checkboxes are untouched for the orchestrator.
- Three of the STATE hand-off entries that named Phase 13 (the booking routing note, the Phase 11 request/expert-booking note and the Phase 12 chat routing note) were marked DONE; the booking routing note also records that Leave a Review and the Feedback block are no longer hidden on Booking Summary.

## Known open items logged to STATE (none block this plan)

- Backend commits `6daf5c0`, `3ba94b9`, `92b4827` unpushed (13-02; release checklist step 1).
- `app/google-services.json` missing (13-05; release checklist step 2); the build warns and succeeds.
- 13-11 drops a push tap while on SignupSetup (RESEARCH A3), to confirm at UAT.
- Device checks (a) to (g) in the "[Phase 13] DEFERRED (human)" list, plus the release checklist entry from 13-14.

## Known Stubs

None. No code was changed in this plan.

## Threat Flags

None. No files introduce new security-relevant surface; T-13-43 (logging) and T-13-44 (R8 stripping) are mitigated by the clean sweep and the R8 build with the dex check.

## Self-Check: PASSED

- Commits `a14153c` and `9a4d38d` exist on this branch.
- `13-VALIDATION.md`, `STATE.md`, `ROADMAP.md`, `REQUIREMENTS.md` carry the planned content (acceptance greps above).
