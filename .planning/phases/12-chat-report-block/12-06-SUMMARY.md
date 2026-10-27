---
phase: 12-chat-report-block
plan: 06
subsystem: chat
tags: [chat-thread, viewmodel, paging, realtime, typing, read-receipts]
status: complete
requires:
  - phase: 12-01
    provides: ChatsRepository (messages, findThread), chat DTOs, FakeClickMeApi chat handlers, FakeRealtimeConnection recorders, ChatFixtures
  - phase: 12-02
    provides: ThreadItem, ThreadReducer, BlockState, ChatSessionState, CurrentUserIdProvider, ClientMsgIdGenerator
  - phase: 12-03
    provides: ChatThreadKey, ChatLaunchRequest.threadNotice
provides:
  - ChatThreadUiState (full thread state incl. send-related fields for 12-08) and ThreadLoad
  - ChatMessageMapping (ChatMessageItem and MessageNew to ThreadItem, firstPeerSenderId, toScannedBlock)
  - ChatThreadViewModel (assisted Hilt; load, paging, reloads, realtime, typing, read receipts, block-state read, unblock notice)
  - ChatThreadTestHarness for 12-08
affects: [12-08, 12-09, 12-14]
tech-stack:
  added: []
  patterns:
    - "LoadMode Loud/Quiet/Pull with latest-wins loadJob (MyBookings pattern) reused for the thread"
    - "Emitted-id set records read receipts only when actually sent (socket drops emits unless Connected)"
key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/ChatThreadUiState.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/ChatMessageMapping.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/ChatThreadViewModel.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/ChatThreadTestHarness.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/ChatMessageMappingTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/ChatThreadViewModelTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/ChatThreadViewModelRealtimeTest.kt
  modified: []
key-decisions:
  - "Reconnect reload fires on a Connected transition only after the ViewModel has already seen Connected once, so the first Connected never reloads (page 1 was just fetched) while Connected, Disconnected, Connected reloads once"
  - "booking.update and reconnect reloads are Loud while the first load is still Loading (so a failure stays visible) and Quiet otherwise"
  - "Block-state scan is latest-wins (blockJob), so the init scan and the immediate onScreenShown scan coalesce into one request"
  - "No typing.start for a self thread or without a known peer id"
  - "ChatThreadTestHarness takes an optional unused scope parameter so the plan's harness(scope) shape stays source compatible for 12-08"
metrics:
  tasks: 3
  commits: 3
  tests_added: 52
actuals:
  tokens: 16000
  tasks: 3
  commits: 3
commits: 3
plan_head_before: e0a298fc91d76d676bd22914ec24bae368e65356
plan_head_after: ca7c9b1a8622bf9ee62603a0149f59a64ce62ee7
requirements-completed: [CHAT-02, CHAT-04, SAFE-03]
completed: 2026-10-27
---

# Phase 12 Plan 06: Chat thread ViewModel, read side Summary

**One assisted-injection `ChatThreadViewModel` that loads page 1 (limit 30) with inline booking events, pages backwards, stays current through booking.update, reconnect, resume and pull, runs typing and read receipts for the open thread only, and reads block state from the GET chats scan.**

## What was built

- **Task 1 (tracer), `2bc2e94`:** `ChatThreadUiState` carries every field 12-08, 12-09 and 12-14 need (`composerMode`, `menuEntry`, `canSend` derived). `ChatMessageMapping` decides event versus message only by `type == "event"` (a text row with a null sender stays a message), parses `event_data` defensively and passes text through verbatim. The ViewModel seeds from the key, loads page 1 through `repository.messages`, merges with `ThreadReducer.mergePage`, falls back to the first non-me sender for the peer id, and shows `Failed` with a retry on a loud failure.
- **Task 2, `677e2fe`:** `loadEarlier` (silent, retryable failure, zero-row page ends history), quiet reloads on screen shown, booking.update and reconnect, pull refresh with `refreshFailed`, incoming `message.new` (peer only, deduped, clears typing) and `message.read` flips, block-state read via `findThread` with seed fill, and the thread-scoped unblock notice (peek then consume, matched ignoring case).
- **Task 3, `ca7c9b1`:** typing start once per burst and stop after `TYPING_IDLE_MS = 3_000L`, on an emptied composer and on hide; incoming typing cleared by stop, a peer message or `PEER_TYPING_TIMEOUT_MS = 6_000L`; `emitPendingReads` only while shown and Connected with an emitted-id set, called after every merge, incoming peer message, screen shown and Connected transition; draft text kept under `KEY_COMPOSER`; `internal fun stopTypingNow()` for 12-08.

## Verification

- `ChatMessageMappingTest` 11, `ChatThreadViewModelTest` 23, `ChatThreadViewModelRealtimeTest` 18, `RealtimeEmitsTest` 5 (unchanged): all green. `ktlintCheck` passes. `bash scripts/check-design-rules.sh` prints "Design rules: OK".
- Acceptance greps: `ThreadReducer.mergePage`, `MESSAGE_TYPE_EVENT`, `interface Factory`, `filterIsInstance<RealtimeEvent.BookingUpdate>`, `applyScan`, `onThreadShown`, `TYPING_IDLE_MS = 3_000L`, `PEER_TYPING_TIMEOUT_MS = 6_000L` and `emitMessageRead` all present; zero `Log.x(` calls in the ViewModel.
- Prohibition (no typing or read receipts for another thread, or from a hidden or disconnected thread) is covered by `noReadReceiptsWhileHidden`, `pendingReadReceiptsGoOutAfterTheReconnect`, `typingEventsForOtherThreadsOrFromMeAreIgnored` and `noTypingWithoutPeerId`.
- Tracer feedback gate: the load slice round-tripped through the real `ChatsRepository` over the fake API (Task 1 tests green) before Task 2 expanded; no `blocking-human` gate on the task.

## Deviations from Plan

### TDD ordering

**1. [Process] Tests and implementation committed together per task, no separate RED commit**
- Test files name symbols (UiState, ViewModel, mapping) that do not exist before the implementation, so a RED run is a compile failure (INVALID_RED), not an assertion failure. The plan is `type: execute`, so the plan-level gate sequence does not apply. All tests were run and pass; same approach as 12-01.

### Auto-fixed Issues

**2. [Rule 1 - Bug] Loading state could stick when a quiet reload cancelled the first load**
- **Found during:** Task 2 design
- **Issue:** a booking.update or reconnect reload that cancels the in-flight first (Loud) load, if started as Quiet, would leave `load = Loading` forever when it failed.
- **Fix:** `reloadQuietly()` starts Loud while `load` is `Loading`, Quiet otherwise (as My Bookings does).
- **Files modified:** ChatThreadViewModel.kt
- **Commit:** 677e2fe

**3. [Rule 2 - Missing critical] Self threads emit no typing events**
- **Issue:** the plan only required "no typing without a peer id"; typing to oneself is meaningless and would leak a typing event to the user's own sockets.
- **Fix:** `onComposerChanged` skips `typing.start` when `isSelfThread`.
- **Commit:** ca7c9b1

**Total deviations:** 3 (1 process, 1 bug, 1 safety addition). **Impact:** none on the planned contract.

## Known Stubs

None. `clientMsgIds` and the send-related UI state fields (`recipientUnavailable`, `sendError`) are intentionally unused here; 12-08 writes them (the constructor already takes the generator so that plan adds no parameter).

## Threat Flags

None. T-12-17 (hidden-thread read receipts), T-12-18 (receipt and reload storms) and T-12-19 (event spoofing) are mitigated and tested as described above.

## Issues Encountered

- The shared `.git/worktrees` ledger file could not be written by the sandbox; `plan_head_before` is the known spawn base `e0a298f`, confirmed by the worktree base check at start.
- Flagged assumption left as planned: typing and read indicators are best-effort over the socket; a typing indicator missed while disconnected is not recovered.

## Next Phase Readiness

12-08 can add the send flow on top of the same constructor, state fields and harness; 12-09 and 12-14 can render `ChatThreadUiState` without further data work.

## Self-Check: PASSED

- Created files found: ChatThreadUiState.kt, ChatMessageMapping.kt, ChatThreadViewModel.kt, ChatThreadTestHarness.kt, ChatMessageMappingTest.kt, ChatThreadViewModelTest.kt, ChatThreadViewModelRealtimeTest.kt.
- Commits in history: 2bc2e94, 677e2fe, ca7c9b1.
