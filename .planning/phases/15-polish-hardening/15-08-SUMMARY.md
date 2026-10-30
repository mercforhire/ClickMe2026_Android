---
phase: 15-polish-hardening
plan: 08
subsystem: ui
tags: [call, talkback, traversal, recreation, large-screen, tracer]
status: complete

requires:
  - phase: 15-02
    provides: "Cutout padding on CallOverlayHost root and pill"
  - phase: 15-04
    provides: "A11yAudit (traversalLabels, assertTraversalOrder, assertActionablesNamed, assertTouchTargets)"
provides:
  - "Call layer TalkBack order: status, name, timer, Mute, Speaker, Chat, Minimize, End call (last)"
  - "Focus moves to the status line once when the expanded layer first shows"
  - "Pill host is its own traversal group at traversalIndex -1f"
  - "Tests that pin: composition and restoration never join, start a call or open the mic"
  - "Short-window (<480 dp) call layer: avatar floor 96 dp, tighter gaps, hero beside controls when wide"
affects: [15-17]

tech-stack:
  added: []
  patterns:
    - "traversalIndex only sorts the direct semantics children of a traversal group: AnimatedContent inserts an empty semantics node, so the variant content is its own group"
    - "Restoration tests swap a fresh ViewModel over the same app-scoped CallCenter before emulateSavedInstanceStateRestore"

key-files:
  created:
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/call/calling/CallTalkBackFlowTest.kt
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/call/calling/CallOverlayHost.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/call/calling/MeetingCallScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/call/calling/subviews/MeetingCallAvatar.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/call/calling/subviews/MeetingCallControls.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/call/calling/subviews/MeetingCallInfo.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/call/calling/CallOverlayHostTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/call/calling/MeetingCallScreenTest.kt

key-decisions:
  - "Chat 1, Minimize 2, End call 3 as explicit traversal indexes: the row lays Minimize out before Chat, and UI-SPEC wants Chat then Minimize"
  - "Variant content (InApp and External) gets its own isTraversalGroup; the root group stays"
  - "Short windows at least 560 dp wide place the hero beside the controls; narrower short windows stay a single compact column that scrolls"

patterns-established:
  - "Status line: FocusRequester keyed on whether the line exists, never on its text, so focus moves once on entry"

requirements-completed: [POL-02, POL-03, POL-04]

duration: ~3 h wall clock (cold Gradle builds on a loaded machine dominated; one 30 min build was killed by the background limit)
completed: 2026-10-29
actuals:
  tokens: 9000
  tasks: 3
  commits: 3
plan_head_before: e808da6b9c3dd62717fc017104c562ebca5b1350
plan_head_after: dcbe6bf9021e1a0235d13ce6208eff718aa8fa5d
---

# Phase 15 Plan 08: Call flow TalkBack order, no auto-join, short-window layout Summary

The expanded call layer now reads status first and End call last, takes focus on the status line when it opens, the minimized pill sorts before the screen underneath, and tests pin that composition and restoration never join a call or touch the microphone.

## Tasks

| Task | Name | Commit |
| ---- | ---- | ------ |
| 2 | Pill first in traversal; no auto-join pinned | a361e2a9 |
| 1 (tracer) | Status first, End call last, focus on entry (plus the short-window layout, same files) | 734683c7 |
| 3 | Short-window tests | dcbe6bf9 |

Tasks 1 and 3 share `MeetingCallScreen.kt`, so the short-window layout code landed in the Task 1 commit and Task 3's commit holds its tests.

## Verification

- One combined run, all green: `CallTalkBackFlowTest` 6/6 (new), `CallOverlayHostTest` 17/17 (5 new), `MeetingCallScreenTest` 15/15 (3 new), `MeetingCallHeroTest` 14/14, `CallOverlayHostInsetsTest` 2/2, `MeetingCallPillTest`, `MeetingCallActionsTest`, `CallCenterStartTest`; `ktlintCheck` clean; `bash scripts/check-design-rules.sh` prints "Design rules: OK".
- Tracer gate: the Task 1 flow test passed end to end after the fix, before the expansion work was committed. Task 1 and 3 verifies were run together in one Gradle invocation, not per task, because cold builds took 5 to 30 min each on the loaded machine.
- Acceptance greps: `FocusRequester` in MeetingCallInfo.kt >= 1; `traversalIndex = -1f` in CallOverlayHost.kt == 1; `96.dp` in MeetingCallAvatar.kt >= 1.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Traversal indexes were ignored by the existing root group**
- **Found during:** Task 1 (flow test failed; printed the semantics tree)
- **Issue:** `AnimatedContent` puts an empty semantics node between the root `isTraversalGroup` column and the variant content, so name, timer, status and controls were not direct children of the group and sorted by geometry only.
- **Fix:** the InApp and External content roots are their own `isTraversalGroup`; the root group stays. `rootIsATraversalGroupAndTheNameIsAHeading` now checks the first of the (now 2) group nodes.
- **Files modified:** MeetingCallScreen.kt, MeetingCallScreenTest.kt
- **Commit:** 734683c7

**2. [Rule 3 - Blocking] MeetingCallControls.kt edited although not listed for Task 1**
- **Issue:** Chat and Minimize need explicit indexes (the row lays Minimize first), and the plan's files list for Task 1 had no controls file.
- **Fix:** `traversalAt` modifier on the Minimize (2) and Chat (1) cells; `CALL_END_TRAVERSAL_INDEX = 3f` exported for the End button.
- **Commit:** 734683c7

**3. [Rule 1 - Bug] Spacer reduction cannot fit the controls at h400dp**
- **Issue:** the stacked layout needs about 416 dp for info, controls and End call alone, before the 96 dp avatar and any gap. UI-SPEC "Large screens" and the plan's truth (controls on screen without scrolling) could not be met by shrinking the avatar and spacers.
- **Fix:** below 480 dp the gaps tighten; if the window is also at least 560 dp wide the hero moves beside the panel (Row), otherwise the single compact column scrolls (UI-SPEC allows scroll together on narrow short windows). D-02 forbids tablet-specific layouts; this is a height-driven adaptation of the one screen, not a tablet layout. Flag for review if that reading is too loose.
- **Files modified:** MeetingCallScreen.kt (new `InAppPanel`, `SideBySideMinWidth = 560.dp`), MeetingCallAvatar.kt (`ShortWindowHeight`, `MinAvatarSize`, glow scales with height)
- **Commit:** 734683c7

**4. [Rule 3 - Blocking] A11yAudit `traversalLabels` models groups only**
- The overlay root is not a traversal group, so the helper cannot show the pill first. `CallOverlayHostTest` uses a local reading-order helper that sorts siblings by index, then top, then left, and also asserts the pill host node's `IsTraversalGroup` and `TraversalIndex` directly. A11yAudit (owned by 15-04) was not changed.

### TDD note
RED was not captured as a separate failing run for Tasks 2 and 3 (cold builds took 5 to 30 min and a 30 min run was killed). Task 1's test did fail first (order and index assertions) and drove the fix in deviation 1.

## Auth Gates
None.

## Known Stubs
None.

## Threat Flags
None. T-15-16 is mitigated by `compositionNeverJoins`, `restorationNeverJoins` and `noSessionAfterProcessDeathShowsNoLayer` (join API calls, engine joins, permission requests and foreground starts all unchanged); no code path joined without a tap, so no production change was needed. T-15-17 is mitigated by `endCallIsLast`.

## Open Items (human/device, deferred per user, for the 15-17 checklist)
- TalkBack on a device: the status line is read first when the layer opens, the order is status, name, timer, Mute, Speaker, Chat, Minimize, End call, and the pill is the first node on screens underneath during a call (RESEARCH A4 / UI-SPEC A8; JVM tests prove the semantics order only). Fallback if the pill is not first: a focus request on the pill when a screen appears during a live call.
- Whether Compose focus on the status line makes TalkBack announce "Connecting" on entry without a double announcement from the live region.
- Resizable emulator: a 900x400 window shows Mute, Speaker and End call without scrolling; a narrow short window (split screen) scrolls; the phone layout is visually unchanged.
- Process death during a live call: relaunch shows the booking detail, never the call layer, and the mic stays closed until a tap.

## Self-Check: PASSED
- Created and modified files exist; commits a361e2a9, 734683c7, dcbe6bf9 are ancestors of HEAD.
