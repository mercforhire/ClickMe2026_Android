---
phase: 12-chat-report-block
plan: 16
subsystem: chat-thread
tags: [gap-closure, chat-thread, unblock, lazy-keys, send-scope]
status: complete
requires:
  - phase: 12-chat-report-block
    provides: ChatThreadViewModel, ChatSessionState, ThreadItem, ReportAndBlockViewModel
provides:
  - Unblocked notice applies the CHAT-UNBLOCK section 6 transition in the thread ViewModel
  - Sender-aware LazyColumn key for thread messages
  - Sends that outlive the thread screen, with memory-only restore of text the server never took
affects: [12-17, 12-verification]
tech-stack:
  added: []
  patterns:
    - "Fire the request in the @ApplicationScope and await it in viewModelScope; on await cancellation register invokeOnCompletion to keep the unsent text"
    - "Owner-keyed, take-once, memory-only store with a keys-only change signal"
key-files:
  created: []
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/ChatThreadViewModel.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/ThreadItem.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/ChatSessionState.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/ChatThreadViewModelTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/ThreadReducerTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/ChatThreadScreenTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/ChatThreadTestHarness.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/ChatThreadViewModelSendTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/chat/ChatSessionStateTest.kt
key-decisions:
  - "The Unblocked notice is the authority for the unblocked state (section 6): the fix lives in the thread ViewModel, BlockState.kt and the applyScan not-found rule (A4) are untouched"
  - "Peer rows are keyed by server id; only own rows use the client_msg_id (backend uniqueness is per sender)"
  - "A send runs in @ApplicationScope; failed text is kept in memory per (user, thread) and restored once, never resent automatically (D-02)"
requirements-completed: [CHAT-02, CHAT-03, SAFE-03]
duration: 40 min
completed: 2026-10-27
commits: 3
plan_head_before: 66aad5d1deaac635c6f2cda08cd69ddfa414f2e5
plan_head_after: f7164deeca307fe055b6d081aa9bc7afe137aab3
actuals:
  tokens: 8400
  tasks: 3
  commits: 3
---

# Phase 12 Plan 16: Unblock unlock, peer key collision and send-outlives-screen Summary

Unblock from Report & Block now unlocks the thread composer at once (even for an empty session-blocked thread or a failed rescan), peer messages can no longer crash the thread with a duplicate LazyColumn key, and a send is no longer cancelled when the thread screen closes.

## Accomplishments

- **Gap 2 / WR-01 (SAFE-03):** `takeNoticeIfForThisThread()` applies `BlockState.onUnblockSucceeded()` in the same state update that sets the notice (exhaustive `when` over `ThreadNotice`), and restarts an in-flight block scan so a pre-unblock answer cannot re-lock the composer. A notice for another thread changes nothing.
- **IN-02 (CHAT-02, T-12-45):** `ThreadItem.Message.key` is `c:<client_msg_id>` only when `mine && clientMsgId != null`, otherwise `s:<server id>`. A peer reusing one of my client_msg_ids renders as a distinct row, and a peer socket row keeps its key when a reload adds a client_msg_id.
- **WR-03 (CHAT-03, T-12-44):** `deliver()` starts the POST with `sendScope.async` (`@ApplicationScope`) and awaits it in `viewModelScope`. If the await is cancelled (screen left) and the request does not succeed, `ChatSessionState.keepUnsentText` stores the text; the next `ChatThreadViewModel` for that thread and user restores it into the composer (also while open, via the keys-only `unsentKeys` signal). Live outcomes are unchanged (Failed bubble on network/5xx, bubble removed and text restored on 4xx).
- `ChatSessionState`: `keepUnsentText`, `takeUnsentText` (atomic take-once, null owner returns null), `unsentKeys`, and `clearSessionData()` also drops kept text.
- Test harness: `sendScope` constructor parameter and `clear(vm)` through the public `ViewModelStore` API.

## Task Commits

| Task | Commit | Description |
| ---- | ------ | ----------- |
| 1 | 2de260c | fix(12-16): unblock notice unlocks the thread composer at once |
| 2 | fa2e926 | fix(12-16): key peer messages by server id so a reused client_msg_id cannot crash the thread |
| 3 | f7164de | fix(12-16): a send outlives the thread screen; unsent text returns to the composer |

## Verification

- RED confirmed before each fix: Task 1 (tracer, failed-rescan, stale-scan tests failed at HEAD), Task 2 (three reducer tests and the screen test failed at HEAD), Task 3 (send-survives, network-after-leaving, refused-after-leaving and reopened-thread tests failed once the API existed but sends were still scoped to the ViewModel).
- Each task's `<automated>` command passed (ChatThreadViewModelTest, ReportAndBlockViewModelTest, BlockStateTest, ThreadReducerTest, ThreadRowsTest, ChatThreadScreenTest, ChatThreadViewModelSendTest, ChatThreadViewModelRealtimeTest, ChatSessionStateTest, `ktlintCheck`, `:app:assembleDevDebug` including the Hilt graph for the qualified scope). The whole `screens.chat.*` unit-test package also passes.
- `bash scripts/check-design-rules.sh`: "Design rules: OK".
- Prohibition gates: no commit touches BlockState.kt, ChatRequests.kt, ClickMeApi.kt, ApiRequestUrls.kt or ChatsRepository.kt; no `Log.`/`println(` in the changed files; no DataStore/SharedPreferences/File in ChatSessionState.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None. Manual device checks (Send then Back on a real device, Unblock round trip against the backend) are deferred per the project's manual-testing policy and remain open items.

## Known Stubs

None.

## Threat Flags

None. The new surface (kept unsent text) is the plan's T-12-44 and is mitigated as specified.

## Self-Check: PASSED

- All modified files exist; commits 2de260c, fa2e926, f7164de are ancestors of HEAD.
