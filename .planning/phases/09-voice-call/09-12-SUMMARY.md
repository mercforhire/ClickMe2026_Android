---
phase: 09-voice-call
plan: 12
subsystem: call
tags: [compose, ui, call-screen, accessibility, edge-to-edge]
requires: [09-02, 09-03, 09-04, 09-08, 09-09]
provides:
  - MeetingCallUiState and CallSession.toMeetingCallUiState(now, myAvatarUrl)
  - MeetingCallScreen (stateless, in-app and external variants), MEETING_CALL_SCREEN_TAG
affects: [09-13]
tech-stack:
  added: []
  patterns: [min-height-fill scroll column, self-applied edge-to-edge insets, AnimatedContent variant cross-fade]
key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/call/calling/MeetingCallUiState.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/call/calling/MeetingCallScreen.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/call/calling/MeetingCallUiStateTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/call/calling/MeetingCallScreenTest.kt
  modified: []
key-decisions:
  - "Timer is computed at endedAt once status is Ended (frozen), otherwise at the shared clock"
  - "Root is a single BoxWithConstraints (background plus scroll column) instead of Box wrapping BoxWithConstraints; same layout, one fewer node"
requirements-completed: [CALL-04, CALL-07, CALL-08]
duration: ~25 min
completed: 2026-10-24
---

# Phase 9 Plan 12: Expanded call screen Summary

Pure `MeetingCallUiState` projection of `CallSession` plus the stateless `MeetingCallScreen` assembling the Wave 2 subviews for the in-app and external variants, with its own safeDrawing/navigationBars insets, a min-height-fill scroll column and a 200 ms variant cross-fade.

## Tasks

| Task | Name | Commit |
|------|------|--------|
| 1 | MeetingCallUiState mapping | d0fcbc4 |
| 2 | MeetingCallScreen for both variants | 7003576 |

## Verification

- `:app:testDevDebugUnitTest --tests '*MeetingCallScreenTest' --tests '*MeetingCallUiStateTest' ktlintCheck`: BUILD SUCCESSFUL (7 state tests, 9 screen tests, 0 failures).
- `bash scripts/check-design-rules.sh`: OK.
- Acceptance greps: `endedAt ?: now` = 1, `WindowInsets.safeDrawing` = 2, `WindowInsets.navigationBars` = 1.

## Deviations from Plan

### Process notes

- Worktree HEAD (`d1217fb`) was behind the stated base; fast-forwarded to `2f612a4` per the startup check before any work.
- Both tasks are `tdd="true"`; tests and implementation were committed together per task (single commit each), not as separate RED/GREEN commits (same as 09-09).
- The weighted spacer sits once above the variant content (per plan), so the hero/info/controls/End group is bottom-anchored on a tall phone; the banner stays at the top.
- The external hero gets `Spacing.lg` horizontal padding from the screen because `ExternalSessionHero` does not pad itself; the other subviews pad themselves.
- External controls receive no-op mute/speaker callbacks since `showAudioControls = false` never renders them.

None of these required changes outside the plan's files.

## Known Stubs

None. Chat stays disabled by design until Phase 12 (D-11), owned by 09-09.

## Threat Flags

None. T-09-41 and T-09-42 accepted as planned (plain text with maxLines in the subviews; the join URL is only passed to buttons, never rendered as text).

## Self-Check: PASSED

All four created files exist; commits d0fcbc4 and 7003576 exist on the branch.
