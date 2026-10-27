---
phase: 12-chat-report-block
plan: 13
subsystem: call
tags: [in-call, chat-entry, call-overlay, tdd]
status: complete

requires:
  - phase: 12-chat-report-block
    provides: ChatEntryViewModel, ChatPeer, ChatEntryErrorDialog, MESSAGE_BUTTON_SPINNER_TAG, chat_entry_opening (12-04)
provides:
  - In-call Chat control enabled end to end (peer id in the call UI state, click and busy state in both layouts, overlay wiring)
  - Call minimizes to the pill on a successful chat open (Phase 9 D-11 hand-off)
affects: [12-15]

tech-stack:
  added: []
  patterns:
    - "Stateless overlay content takes chat parameters with defaults; the Hilt host owns ChatEntryViewModel"
    - "MeetingCallControlButton busy flag swaps the glyph for a primary spinner and keeps the control enabled so the state description is announced"

key-files:
  created: []
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/call/calling/MeetingCallUiState.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/call/calling/subviews/MeetingCallControls.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/call/calling/MeetingCallScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/call/calling/CallOverlayHost.kt
    - app/src/main/res/values/strings_call.xml
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/call/calling/subviews/MeetingCallActionsTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/call/calling/MeetingCallScreenTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/call/calling/MeetingCallUiStateTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/call/calling/CallOverlayHostTest.kt

key-decisions:
  - "chatEnabled is derived from state.peerId != null inside MeetingCallScreen (both layouts), so callers pass only onChat and chatBusy"
  - "Busy keeps the Chat control enabled and guards the click lambda, matching MessageButton, so accessibility still announces 'Opening chat'"
  - "The busy spinner reuses MESSAGE_BUTTON_SPINNER_TAG from the shared Message control"

patterns-established:
  - "CallOverlayContent exposes chatBusy/onChat/chatError/onDismissChatError; the error dialog renders above the call layer so a failure never touches the call"

requirements-completed: [CHAT-05]

duration: ~45min
completed: 2026-10-27
commits: 2
plan_head_before: daa12dd1d9ff48e83c44a9ae369cb3bb3b9ce2c5
plan_head_after: 8efe537888870f848e9d865ae54e38a798c7b0f1
actuals:
  tokens: 9000
  tasks: 2
  commits: 2
---

# Phase 12 Plan 13: In-call Chat entry Summary

**Tapping Chat during a call initiates the chat with the session's peer, hands the thread to the Chats tab and minimizes the call to the pill (call keeps running), in both the in-app voice and the Skype/Zoom layouts.**

## Accomplishments

- `MeetingCallUiState.peerId` (defaulted, last) mapped from `CallSession.peerId`; null keeps Chat disabled with the existing disabled styling.
- `MeetingCallControls` gains `onChat`, `chatEnabled`, `chatBusy`. The placeholder, its PHASE-12 marker and the "Available soon" state description are gone; the `call_control_chat_unavailable` string is deleted (`call_control_chat` kept).
- `MeetingCallControlButton` gains `busy`: a 24 dp primary spinner replaces the glyph; Chat carries state description "Opening chat" and ignores taps while busy.
- `MeetingCallScreen` threads `onChat`/`chatBusy` into both `InAppContent` and `ExternalContent`.
- `CallOverlayHost` takes `chatEntry: ChatEntryViewModel = hiltViewModel()` and calls `chatEntry.open(ChatPeer(peerId, peerName, peerAvatarUrl, isExpert = null), onOpened = viewModel::minimize)`. `CallOverlayContent` renders `ChatEntryErrorDialog` above the call layer, so "Couldn't open chat" never disturbs the call. KDoc documents why `isExpert` is null.

## Task Commits

1. Task 1 (tracer): `45c5bb8` feat(12-13): in-call Chat opens the thread and minimizes the call
2. Task 2: `8efe537` feat(12-13): busy spinner on the in-call Chat control and error dialog tests

## Verification

- Task 1 gate (tracer): `MeetingCallActionsTest`, `MeetingCallUiStateTest`, `CallOverlayHostTest`, `MeetingCallScreenTest` plus `ktlintCheck` passed before Task 2 expanded.
- Task 2 gate: the five specified classes plus `MeetingCallUiStateTest` pass (12/20/9/8/12/5 tests, 0 failures) and `ktlintCheck` passes. `MeetingCallViewModelTest` and `MeetingCallPillTest` are unchanged.
- `bash scripts/check-design-rules.sh` prints "Design rules: OK".
- Acceptance greps: `PHASE-12` in MeetingCallControls = 0, `call_control_chat_unavailable` = 0, `onOpened = viewModel::minimize` = 1, `chat_entry_opening` in controls = 1, `chatEnabled` in MeetingCallScreen = 2.
- New tests: `peerIdMapsFromSession`, `chatControlEnabledAndClicks`, `chatControlIsNotEnabledWithoutAPeerAndIgnoresTaps`, `chatBusyShowsSpinnerAndIgnoresTaps`, `chatIdleShowsNoSpinner`, `chatTapCallsOnChat`, `chatErrorShowsDialogOverCall`, and three `MeetingCallScreenTest` cases (external enabled with peer, external disabled without peer, in-app busy).

## TDD Gate Compliance

Tests and implementation are committed together per task; the new tests reference parameters that do not exist before the implementation, so a separate RED run is a compile failure (INVALID_RED), not an assertion failure. The plan is `type: execute`, so the plan-level RED/GREEN gate sequence does not apply.

## Deviations from Plan

**1. [Design] Tests for the plan's `MeetingCallActionsTest` "disabled with Available soon" assertion replaced, external-controls assertion flipped**
- The existing `externalControlsHaveOnlyMinimizeAndChat` asserted Chat was not enabled; it now asserts enabled (the helper defaults `chatEnabled = true`).

**2. [Design] `MeetingCallScreen` chat parameters default (`chatBusy = false`, `onChat = {}`)**
- Keeps other callers compiling; `chatEnabled` is derived from `state.peerId` rather than being a separate parameter.

Otherwise the plan executed as written.

## Known Stubs

None.

## Threat Flags

None. T-12-37 mitigated: single flight plus busy guard in `ChatEntryViewModel` and a second click guard on the control; the call only minimizes after a successful open. T-12-38 accepted.

## Deferred / Open Items

- On-device check of tapping Chat during a live call (minimize to pill, thread on the Chats tab) depends on the thread screen (12-09) and is deferred per project policy on manual testing.

## Self-Check: PASSED

- Modified files present; commits 45c5bb8 and 8efe537 are ancestors of HEAD.
