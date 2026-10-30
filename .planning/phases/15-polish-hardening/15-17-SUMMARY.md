---
phase: 15-polish-hardening
plan: 17
subsystem: gate
tags: [gate, device-checklist, sweeps, release-build, state, tracer]
status: complete

requires: [15-01, 15-02, 15-03, 15-04, 15-05, 15-06, 15-07, 15-08, 15-09, 15-10, 15-11, 15-12, 15-13, 15-14, 15-15, 15-16, 15-18, 15-19, 15-20]
provides:
  - "15-DEVICE-CHECKLIST.md: one non-blocking checklist of 101 open rows (85 in ten flow tables, 16 system rows), a Decisions for the user table and a Gap list for Phase 16"
  - "STATE.md: 14 absorbed Phase 3-14 sub-items marked SUPERSEDED, the [Phase 15] DEFERRED (human) entry, [Phase 15] Open items and Hand-offs todos, the D-09 wording correction"
  - "15-VALIDATION.md signed off (status validated, wave_0_complete true, nyquist_compliant true) with the D-03 reading of SC-2 and the gate results"
  - "The green Phase 15 gate: 4316 tests per flavor, ktlint, three assembles, design rules, lint on both flavors, R8 assembleProdRelease"
affects: [phase-16, verify-work-15]

tech-stack:
  added: []
  patterns:
    - "Consolidated device checklist: one table per flow, eight fixed columns, cells only [ ] or n/a, absorbed originals marked SUPERSEDED in place"

key-files:
  created:
    - .planning/phases/15-polish-hardening/15-DEVICE-CHECKLIST.md
  modified:
    - .planning/STATE.md
    - .planning/phases/15-polish-hardening/15-VALIDATION.md
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/ChatThreadScreen.kt

key-decisions:
  - "The D-14 decision is the orchestrator-applied default jvm-audit-plus-gap (15-04 SUMMARY): its Phase 16 gap line (androidTest source set with enableAccessibilityChecks() on the four flows, tier 1) is in the checklist gap list and the STATE.md Phase 16 hand-off"
  - "Stripe-card and live-iOS-peer items were left in place in their original STATE.md entries (D-15) and listed as owned by Phase 16 in the gap list; partly absorbed bullets got the '(visual/system part SUPERSEDED ...)' form"
  - "STATE.md carried no sentence saying the signup OTP is mirrored to saved state; the only stale claim was the Phase 3 rotation line, corrected to say the code and passwords are no longer kept (D-09). The T-14-31 reversal for non-secret Tax fields is recorded in the [Phase 15] Open items todo"

patterns-established: []

requirements-completed: [POL-01, POL-02, POL-03, POL-04]

duration: ~1 h 15 min (dominated by Gradle gate runs)
completed: 2026-10-29
actuals:
  tokens: 62000
  tasks: 2
  commits: 3
plan_head_before: fe033b968b0726968f64e4a95a0c53d8aa251546
plan_head_after: 0b87f8cfed27b97816cd6d53f332771ff2e1c6d5
---

# Phase 15 Plan 17: Device checklist, paperwork close-out and phase gate Summary

Every deferred visual, accessibility and system check from Phases 3-15 now lives in one non-blocking 101-row device checklist with a Phase 16 gap list, STATE.md points to it with nothing dropped, and the Phase 15 gate is green (4316 tests per flavor, lint on both flavors, R8 release build) after one lint error from the chat plans was fixed.

## Tasks

| Task | Name | Commit |
| ---- | ---- | ------ |
| 1 (tracer) | Consolidated device checklist and STATE.md pointers | 518f3da6 |
| gate fix | `LocalResources` in `PeerArrivalAnnouncer` (lint error found by the gate) | de321fb5 |
| 2 | Validation map sign-off, gate and sweep results | 0b87f8cf |

## Checklist

`15-DEVICE-CHECKLIST.md` follows the UI-SPEC shape: header (Samsung SM-A075F, devDebug, production backend, the resizable-emulator setup from the `android-37.0` image, the D-03 reading of SC-2, the Phase 16 ownership note), one table per flow in the UI-SPEC order (Login and recovery 9 rows, Signup 6, Explore and Expert Profile 6, Booking 10, My Bookings and Cancel 8, Call 10, Chat and Report/Block 13, Notifications and Reviews 4, Expert side 9, Settings and Payouts 10), 16 system rows, a Decisions for the user table, and the Gap list for Phase 16. A script check confirmed all 101 rows have eight columns, every cell is `[ ]` or `n/a`, no `[x]` exists, and every first column names a source phase. No row is marked passed from automated results (RESEARCH Pitfall 9).

Rows carried from this phase: the four SC-4 TalkBack runs (L1, B1, C1, K1), A6 field speech (L3), call pill first (C2), focus on the status line, Request Sent, Booking Confirmed, Password Changed and Farewell headlines, Retry snackbars under TalkBack (Y14), Accessibility Scanner (Y12), portrait lock (Y1), large-window rows on the resizable emulator (Y2, B9, C4, C10), "Don't keep activities" through Signup, Booking, Chat and a call (Y3-Y6), the `am force-stop` process kill during a call landing on the booking detail with the one-time note (C7), dark-mode and font-scale recreation (Y7), R8 release restore (Y11), IME with the cutout (Y8), gesture versus 3-button navigation (Y9), animations off (Y10), scroll position after restore (Y15).

STATE.md absorption: 14 sub-items marked SUPERSEDED (full: P3 rotation line, P5 visual, P8 visual and system, P10 (f), P11 (e), P12 (e), P13 (g), P14 (f); partial: P3 SC-2, P3 release smoke, P4 visual bullet, P6 visual and system R8 smoke, P7 SC-5, P9 last bullet). Functional, Stripe-card, live-iOS-peer and release-checklist items were left in place.

Decisions for the user (section 12 of the checklist, also in the STATE.md Open items todo): (a) 15-08 height-adaptive call layout versus D-02, (b) 15-07 Book Session before About, (c) 15-19 Book Now last in the form versus pinned, (d) 15-12 empty-catalog copy, (e) 15-20 Farewell button full width on large windows, (f) 15-18 TalkBack focus after Retry not implemented, (g) 15-07 day cells about 45.7 dp on 360 dp phones, plus the D-14 decision, UI-SPEC A1, A2, A4, A7 and the portrait-lock review.

## Gate

| Step | Result |
|------|--------|
| `testDevDebugUnitTest` | 4316 tests, 0 failures, 0 errors, 0 skipped |
| `testProdDebugUnitTest` | 4316 tests, 0 failures, 0 errors, 0 skipped |
| `ktlintCheck`, `assembleDevDebug`, `assembleProdDebug`, `:spikes:stripe:assembleDebug` | BUILD SUCCESSFUL |
| `scripts/check-design-rules.sh` on app/src and on spikes | "Design rules: OK" both |
| `scripts/check-design-rules-selftest.sh` | "Design rules self-test: OK" |
| `lintDevDebug lintProdDebug` | First run: 1 error, rerun after the fix: 0 errors, BUILD SUCCESSFUL |
| `assembleProdRelease` (R8) | BUILD SUCCESSFUL, unsigned APK about 129 MB |

The combined gate was run as three Gradle invocations (tests plus ktlint plus assembles; lint; R8) to avoid the daemon OOM noted for the combined shape. After the lint fix, the chat tests (`*chat.chatting*`) and `ktlintCheck` were rerun green; the full suite was not rerun because the change is a one-line source swap in one composable.

## Sweeps (app/src/main)

| Sweep | Baseline | Now |
|-------|----------|-----|
| Action-less refresh snackbars (`showSnackbar(refreshFailedMessage`) | 12 | 0 |
| `"verify_code"` / `"reset_code"` literals | 2 | 0 |
| `ConnectivityManager` / `NetworkCallback` files (D-06) | 0 | 0 |
| `requestedOrientation` | 0 | 0 |
| `android:screenOrientation` in the manifest | 0 | 1 (MainActivity) |
| `// insets-ok:` with no reason | n/a | 0 (no opt-out is used in `app/src`; only the self-test fixtures use it) |
| Informative `onSurfaceMuted` text on `surfaceContainerHighest` / `surfaceBright` (UI-SPEC A9) | none found | none (the muted tint in `BookingRequestSentDetailsCard` is on a decorative icon; `ExploreCategoryCard`, `ExpertTagChip` and `SelectableChip` use onSurface / onSurfaceVariant) |

`*Screen.kt` files without a literal `contentMaxWidth()`: `DesignSystemGalleryScreen`, `HomeClientScreen`, `HomeExpertScreen`, `PhotoCropScreen` and `MeetingCallScreen` (the five exempt screens), plus `ClientProfileHomeScreen` and `ExpertProfileHomeScreen`, which are capped through `ProfileHubScaffold` in `ProfileHubComponents.kt` (named in the 15-20 SUMMARY, with the `hubCapped` test).

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Lint error in the chat peer announcer**
- **Found during:** Task 2 (`lintProdDebug`)
- **Issue:** `PeerArrivalAnnouncer` in `ChatThreadScreen.kt` (15-10) called `LocalContext.current.getString(...)`; lint rule `LocalContextGetResourceValueCall` flagged it as not configuration-aware. Earlier plans' unit-test runs do not cover lint.
- **Fix:** read the string through `LocalResources.current.getString(...)` (same behavior, configuration-aware).
- **Files modified:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/ChatThreadScreen.kt`
- **Commit:** de321fb5

**2. [Plan wording] `nyquist_compliant: true` count**
- The acceptance criterion expects exactly one match. The template's sign-off checkbox text contained the phrase and the frontmatter replace also touched a header comment; the comment was restored and the checkbox reworded ("`nyquist_compliant` set to true in frontmatter") so the count is 1.

No other deviations. No STATE.md edits beyond the plan's directions; ROADMAP.md untouched; no `phase.complete` or roadmap progress command was run.

## Edge-probe items flagged for the verifier

POL-02, POL-03 and POL-04 edge items were unclassified by the probe; they are covered only by the device rows above and are flagged for the verifier. Also unresolved on the JVM: the real rotate and resize behavior on sw600dp+ at targetSdk 37 (Robolectric proves `w900dp` only), TalkBack speech of a filled field (A6), and whether the call pill is first in TalkBack order (A8). The three UI Consideration backstops (state-transition jank, 200% font scale on every MVP screen, a TalkBack user completing the four flows) stay open device rows.

## Known Stubs

None.

## Threat Flags

None. T-15-36 holds: the checklist references accounts by role and the `uat.*@clickme.test` pattern, with no passwords, tokens or keys, and the design-rules Stripe-key rule passed. T-15-37 holds: every checklist cell starts `[ ]`. No package installs.

## Self-Check: PASSED

- `15-DEVICE-CHECKLIST.md` exists; `Gap list for Phase 16` count 1; no `[x]`.
- STATE.md: `SUPERSEDED by Phase 15 checklist` on 15 lines (>= 10), `[Phase 15] DEFERRED (human)` on 1 line.
- Commits 518f3da6, de321fb5 and 0b87f8cf are ancestors of HEAD.
- `grep -c 'nyquist_compliant: true'` on 15-VALIDATION.md is 1.
