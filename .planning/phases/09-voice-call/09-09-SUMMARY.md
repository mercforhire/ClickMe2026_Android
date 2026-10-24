---
phase: 09-voice-call
plan: 09
subsystem: call
tags: [compose, ui, controls, external-meeting, accessibility]
requires: [09-02, 09-03, 09-04]
provides:
  - EndingSoonBanner (ENDING_SOON_BANNER_TAG)
  - MeetingCallControls + MeetingCallControlButton (CALL_CONTROL_*_TAG)
  - MeetingCallEndButton (CALL_END_BUTTON_TAG)
  - ExternalLinkActions, ExternalStatusText, externalOpenLabel (EXTERNAL_*_TAG)
affects: [09-12, 09-13]
tech-stack:
  added: []
  patterns: [stateless call composables, merged-semantics toggle controls, clipboard copy with timed chip]
key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/call/calling/subviews/EndingSoonBanner.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/call/calling/subviews/MeetingCallControls.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/call/calling/subviews/MeetingCallEndButton.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/call/calling/subviews/ExternalLinkActions.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/call/calling/subviews/MeetingCallActionsTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/call/calling/subviews/ExternalLinkActionsTest.kt
  modified: []
key-decisions:
  - "Copy Link has no path to onOpen, so copying never counts as engagement (D-05, T-09-32); tested"
  - "Outlined Copy Link button drawn with Modifier.border on PressScaleButton, since PressScaleButton has no border parameter"
requirements-completed: [CALL-01, CALL-07, CALL-08]
duration: ~50 min
completed: 2026-10-24
---

# Phase 9 Plan 09: Call action composables Summary

Stateless ending-soon banner, in-app and external control rows (Chat disabled, D-11), a filled-destructive End Call / End Session button with an ending spinner, and the external Open / Copy Link actions with the 1500 ms "Link copied" chip and the three-state status text.

## Tasks

| Task | Name | Commit |
|------|------|--------|
| 1 | Ending-soon banner, control row (both variants), End button | 6acd670 |
| 2 | External link actions, Link copied chip, status text | 4d577f8 |

Both tasks are `tdd="true"`; tests and implementation were committed together per task (single commit each), not as separate RED/GREEN commits.

## Verification

- `:app:testDevDebugUnitTest --tests '*MeetingCallActionsTest' --tests '*ExternalLinkActionsTest' ktlintCheck`: BUILD SUCCESSFUL (25 tests).
- `bash scripts/check-design-rules.sh`: OK.
- Acceptance greps: `enabled = false` in MeetingCallControls.kt = 1, `extras.destructive` in MeetingCallEndButton.kt = 1, `LINK_COPIED_HOLD_MS = 1_500L` = 1.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] PressScaleButton has no `border` parameter**
- **Found during:** Task 2
- **Issue:** The outlined Copy Link style needs a 1dp border; `PressScaleButton` does not expose one, and changing it is outside this plan's files.
- **Fix:** Applied `Modifier.border(1.dp, outlineVariant, CircleShape)` (38% alpha when disabled) on the button modifier with transparent container colors.
- **Files modified:** ExternalLinkActions.kt
- **Commit:** 4d577f8

**2. [Rule 1 - Bug] Deprecated Volume icons**
- **Found during:** Task 1
- **Issue:** `Icons.Outlined.VolumeUp/VolumeDown` are deprecated in favor of the AutoMirrored versions.
- **Fix:** Use `Icons.AutoMirrored.Outlined.VolumeUp/VolumeDown`.
- **Files modified:** MeetingCallControls.kt
- **Commit:** 6acd670

**3. [Rule 2 - Accessibility] Merged semantics on live-region chip and banner**
- **Found during:** Task 2 test run
- **Issue:** The live-region container node did not carry its text, so a screen reader and tests saw an empty live region.
- **Fix:** `semantics(mergeDescendants = true)` on the banner and the Link copied chip.
- **Commit:** 6acd670, 4d577f8

### Plan notes

- The worktree HEAD was behind the stated base (d1217fb vs c22e86e); reset to the base per the startup check before any work.
- The "Link copied" disappearance test advances the clock by 1600 + 400 ms (hold 1500 ms plus the 200 ms fade-out) rather than 1600 ms alone, because the node stays in the tree during the fade-out.

## Known Stubs

None. The Chat control is intentionally disabled until Phase 12 (D-11) and is marked `// PHASE-12` in MeetingCallControls.kt.

## Threat Flags

None. T-09-30 accepted (explicit tap, same as MeetingLinkRow); T-09-31 mitigated (unknown provider shows "Open Meeting" with the generic icon, tested); T-09-32 mitigated (Copy never calls onOpen, tested).

## Self-Check: PASSED

All six created files exist; commits 6acd670 and 4d577f8 exist on the branch.
