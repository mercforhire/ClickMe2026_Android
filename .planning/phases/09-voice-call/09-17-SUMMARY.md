---
phase: 09-voice-call
plan: 17
subsystem: voice-call
tags: [gate, release-build, spike-removal, state, follow-ups]
requires:
  - phase: 09-voice-call
    provides: plans 09-01..09-16 (call stack, UI, Join sites, guards)
provides:
  - Green Phase 9 gate on both flavors (tests, ktlint, lint, R8 with Agora, design rules)
  - ":spikes:agora removed (D-16); only :app and :spikes:stripe remain"
  - Deferred device UAT, open items, follow-ups and hand-offs recorded in STATE
affects: [phase-10, phase-11, phase-12, phase-13, phase-14, phase-17]
tech-stack:
  added: []
  patterns: []
key-files:
  created: []
  modified:
    - settings.gradle.kts
    - .github/workflows/ci.yml
    - .planning/STATE.md
    - .planning/ROADMAP.md
    - .planning/REQUIREMENTS.md
    - .planning/phases/09-voice-call/09-VALIDATION.md
  deleted:
    - spikes/agora/
key-decisions:
  - "Scope sweeps are kept literal (all print 0): comments and one log message that merely named a forbidden token were reworded rather than waived"
  - "Predictive back shipped in 09-13, so no polish open item is logged for it"
patterns-established: []
requirements-completed: [CALL-01, CALL-02, CALL-03, CALL-04, CALL-05, CALL-06, CALL-07, CALL-08]
duration: ~50 min
completed: 2026-10-24
---

# Phase 9 Plan 17: Phase gate and close-out Summary

**Absorbed `:spikes:agora` deleted, full gate green on both flavors (2206 tests each, lint, R8 prodRelease with Agora), and the D-16 device UAT plus open items and hand-offs logged as non-blocking items.**

## Performance
- **Duration:** ~50 min (most of it Gradle runs)
- **Completed:** 2026-10-24
- **Tasks:** 2/2
- **Files modified:** 6 tracked files plus the deleted `spikes/agora/` tree

## Accomplishments
- `git rm -r spikes/agora`, `include(":spikes:stripe")` only, and `:spikes:agora:assembleDebug` dropped from the CI Gradle step. The Stripe spike and the `check-design-rules.sh spikes` step are untouched. The `agora` entries in `gradle/libs.versions.toml` stay (used by `:app`).
- Gate, three invocations as required by the OOM note:
  1. `testDevDebugUnitTest testProdDebugUnitTest ktlintCheck assembleDevDebug assembleProdDebug :spikes:stripe:assembleDebug` (with `-Pkotlin.daemon.jvmargs=-Xmx3g` on the command line): BUILD SUCCESSFUL. Tests: dev 2206, prod 2206, 0 failures, 0 errors, 0 skipped. `check-design-rules.sh` and `check-design-rules.sh spikes`: OK.
  2. After `--stop`: `lintDevDebug lintProdDebug`: BUILD SUCCESSFUL.
  3. After `--stop`: `assembleProdRelease` (R8): BUILD SUCCESSFUL. Unsigned APK is 126,818,810 bytes (about 121 MB); `configuration.txt` carries 28 `io.agora` keep lines, so the AAR consumer rules hold (T-09-55). No pre-Agora release baseline was kept, so no size delta was measured; the Phase 7 note records Stripe at about +10.4 MB and the Phase 17 hand-off lists the optional `voice-rtc-basic` trim.
- Sweeps (all print 0 after the fix below): `PHASE-9`, `onTaskRemoved` (D-14), persisted call state in `CallCenter` / `services/call` (D-15), `parseUri`, the iOS `5 * 60` value, `fontSize` in `screens/call`, `canJoin` outside the DTO (D-02), and credential logging in managers / services (T-09-02).
- STATE: "[Phase 9] DEFERRED (human)" block with checkbox items for SC-1..SC-5, the Android 17 check and the accessibility / Bluetooth / GSM / Samsung battery checks; 02-12 marked SUPERSEDED; open items (CallStyle, Bluetooth, Android 17), backend notes (join window 120/120 vs docs 10/0, `SESSION_NOT_ACTIVE`, `/end` duration >= 1), iOS follow-ups, and hand-offs for Phases 10, 11, 12, 14 and 17. The two stale Phase 9 hand-off lines were marked DONE.
- ROADMAP planning notes on Phases 10, 11, 12 and 14; REQUIREMENTS CALL-01 note for D-05 / D-06; `09-VALIDATION.md` all rows green, `nyquist_compliant: true`, `wave_0_complete: true`, sign-off ticked.

## Task Commits
1. **Task 1: Remove :spikes:agora, sweeps and gate** - `06ca612` (chore)
2. **Task 2: Deferred UAT, open items, follow-ups, hand-offs, validation sign-off** - `7c95dfd` (docs)

## Files Created/Modified
- `settings.gradle.kts`, `.github/workflows/ci.yml`: spike module and CI task removed.
- `spikes/agora/`: deleted.
- `ExternalLinkLauncher.kt`, `CallTimer.kt`, `JoinWindow.kt`, `EncryptedPrefsTokenStore.kt`: comment / log-message wording only (see deviations).
- `.planning/STATE.md`, `ROADMAP.md`, `REQUIREMENTS.md`, `09-VALIDATION.md`: close-out records.

## Decisions Made
- Reworded the comment and log text instead of accepting sweep matches, so the plan's literal "every sweep prints 0" criterion holds and future re-runs of the sweeps stay meaningful.
- No `PredictiveBackHandler` follow-up item: 09-13 used it, with no BackHandler fallback.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Sweeps matched documentation text, not code**
- **Found during:** Task 1 sweeps
- **Issue:** Four sweeps printed non-zero for text that only describes a prohibition: `parseUri` (KDoc in `ExternalLinkLauncher.kt` saying it is never used), `5 * 60` (three KDoc mentions in `CallTimer.kt` of the iOS TEMP value), `canJoin` (KDoc in `JoinWindow.kt`), and a `Log.w` message in `EncryptedPrefsTokenStore.kt` containing the word "Token" (no credential value logged).
- **Fix:** Reworded the comments and the log message; no behavior change. All sweeps now print 0.
- **Files modified:** `ExternalLinkLauncher.kt`, `CallTimer.kt`, `JoinWindow.kt`, `EncryptedPrefsTokenStore.kt`
- **Commit:** `06ca612`

### Other notes
- The first gate invocation used `-Pkotlin.daemon.jvmargs=-Xmx3g` pre-emptively (command line only; `gradle.properties` untouched). No OOM occurred in any invocation.
- No real defects found by the gate; no `fix(09-17)` commits.

## Deferred / Open Items
All logged in STATE.md. Headlines: the D-16 device UAT (non-blocking, "[Phase 9] DEFERRED (human)"), CallStyle ongoing notification decision, Bluetooth routing, Android 17 audio hardening check, paid-session confirm-connection check blocked on the test-mode Stripe backend, backend join-window tightening, Phase 2 02-12 superseded.

## Known Stubs
None. The in-call Chat control is intentionally disabled until Phase 12 (`// PHASE-12`, D-11) and is handed off in STATE and ROADMAP.

## Threat Flags
None. T-09-55 (R8 build with Agora, 28 keep lines), T-09-56 (credential-log sweep 0) and T-09-58 (spike removed, no references outside `.planning/`) are mitigated; T-09-57 accepted and tracked as the deferred UAT.

## Self-Check: PASSED
- `spikes/agora` absent; `settings.gradle.kts` includes only `:spikes:stripe`; CI has no `spikes:agora`.
- Commits `06ca612` and `7c95dfd` exist.
- Greps: `[Phase 9] DEFERRED (human)`, `CallStyle`, `SUPERSEDED` in STATE; `nyquist_compliant: true` in 09-VALIDATION; `D-05 / D-06` in REQUIREMENTS; 4 `CallGuard` / `RestrictedAction` mentions in ROADMAP.
