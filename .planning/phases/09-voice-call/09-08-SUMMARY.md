---
phase: 09-voice-call
plan: 08
subsystem: call
tags: [compose, ui, call-screen, pill, accessibility]
requires:
  - phase: 09-voice-call
    provides: "09-02 CallTimerState and call timer labels, strings_call.xml; 09-04 CallStatus"
provides:
  - MeetingCallBackground
  - CallRingAvatar and MeetingCallAvatar (single / pair hero with pulse and glow)
  - ExternalSessionHero
  - MeetingCallInfo, MeetingCallTimer, callStatusText
  - MeetingCallPill
  - Motion.CALL_RING_PULSE_MS and Motion.CALL_GLOW_PULSE_MS
affects: [09-12, 09-13]
tech-stack:
  added: []
  patterns: [stateless call composables taking plain values, test tags exported as top-level consts]
key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/call/calling/subviews/MeetingCallBackground.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/call/calling/subviews/MeetingCallAvatar.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/call/calling/subviews/ExternalSessionHero.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/call/calling/subviews/MeetingCallInfo.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/call/calling/subviews/MeetingCallPill.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/call/calling/subviews/MeetingCallHeroTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/call/calling/subviews/MeetingCallPillTest.kt
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/ui/theme/Motion.kt
key-decisions:
  - "Pill leaves inner semantics intact (merged under one contentDescription, Button role, no live region) instead of clearAndSetSemantics, so the timer text stays testable and TalkBack still reads the description"
  - "Background wash drawn with drawBehind so the radial center can use the measured width (top-center)"
requirements-completed: [CALL-04, CALL-07]
duration: ~50 min (mostly Gradle wall time under parallel load)
completed: 2026-10-24
---

# Phase 9 Plan 08: Call hero, info block and minimized pill Summary

Stateless call UI building blocks: pulsing single hero and connected avatar pair with ambient glow, external-variant hero, info block with the schedule-anchored timer, final-stretch chip and polite status line, and a single-button minimized pill.

## Tasks

| Task | Name | Commit |
|------|------|--------|
| 1 | Background, hero avatar, external hero, info block | a5b762e |
| 2 | Minimized call pill | ff90484 |

## Verification

- `MeetingCallHeroTest` (14 tests) and `MeetingCallPillTest` (5 tests) pass; `ktlintCheck` clean.
- `scripts/check-design-rules.sh`: OK. No `fontSize` under `screens/call/`. `shadowElevation = 8.dp` count 1. Both Motion constants present.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Pill inner `clearAndSetSemantics {}` dropped**
- **Found during:** Task 2
- **Issue:** Clearing the inner row's semantics removes the timer text from the semantics tree, so the plan's own behavior ("shows timer text 01:05") cannot be asserted, and the avatar/timer add nothing for accessibility anyway.
- **Fix:** The merged node carries contentDescription "Return to call with {name}", `Role.Button` and a labelled onClick; the timer `Text` has no live region, so it never announces on tick. Tests assert the text on the merged node and the absence of a live region.
- **Files modified:** `MeetingCallPill.kt`
- **Commit:** ff90484

**2. [Rule 1 - Bug] Radial wash center**
- **Found during:** Task 1
- **Issue:** A background `Brush.radialGradient` cannot express "top-center" without knowing the width up front.
- **Fix:** Drawn in `drawBehind` with `Offset(size.width / 2, 0f)`, radius 600dp.
- **Files modified:** `MeetingCallBackground.kt`
- **Commit:** a5b762e

### Process notes

- Worktree HEAD (`d1217fb`) was behind the stated base; reset to `c22e86e` per the startup check before any work.
- The animator-scale-0 guard from the plan was not added: Robolectric tests with infinite transitions (ring pulse, glow) completed without hanging.
- Avatars use `contentDescription = null`; no `Avatar` allowed-size list change was needed (96, 160 and 32 are already allowed).

## Known Stubs

None.

## Threat Flags

None. T-09-28 and T-09-29 accepted as planned (plain `Text` with `maxLines`; existing Coil `Avatar`).

## Self-Check: PASSED
