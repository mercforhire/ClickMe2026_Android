---
phase: 12-chat-report-block
plan: 02
subsystem: chat
tags: [pure-logic, reducer, state-machine, session, tdd, hilt]
status: complete
requires: []
provides:
  - ThreadItem model (Message, SystemEvent, Delivery, SystemEventKind)
  - ThreadReducer (merge, ack, fail, retry, remove, incoming, read rules)
  - BlockState machine (CHAT-UNBLOCK 5/6/7) with composerMode and menu entry
  - ChatSessionState (open thread id, blocked-by-me set) bound as SessionDataCleaner
  - CurrentUserIdProvider and ClientMsgIdGenerator seams, provided by ChatModule
affects: [12-chat-thread-viewmodel, 12-conversations-list, 12-report-block]
tech-stack:
  added: []
  patterns: [pure reducer over List<ThreadItem>, session cleaner @IntoSet binding, fun-interface seams]
key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/ThreadItem.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/ThreadReducer.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/BlockState.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/ChatSessionState.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/ClientMsgIdGenerator.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/managers/CurrentUserIdProvider.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/di/ChatModule.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/ThreadReducerTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/chat/BlockStateTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/chat/ChatSessionStateTest.kt
  modified: []
decisions:
  - "applyScan keeps the Unblock latch while the scan still says blocked (not only while blocked-by-peer), matching iOS where loadBlockState never touches unblockDenied and the plan test that a (true,true) scan after a latch keeps showsUnblock false"
  - "mergePage replaces a matched row with the page row (including its serverId casing); client_msg_id of the existing row is preserved so the LazyColumn key is stable"
metrics:
  tasks: 3
  commits: 6
  tests_added: 32
plan_head_before: 087ac37bf779d10dc8c32cfa04081e33339d1d67
plan_head_after: 19307181a73767527a725ace80a95a6d96fb356a
commits: 6
actuals:
  tokens: 20000
  tasks: 3
  commits: 6
---

# Phase 12 Plan 02: Chat reducer, block state machine and session seams Summary

Pure, test-first chat logic: a thread reducer that reconciles optimistic, acked and reloaded copies of an own message into one stable row, a CHAT-UNBLOCK block state machine, and session-local seams cleared on logout.

## What was built

- **ThreadItem / ThreadReducer**: `appendLocal`, `ack`, `mergePage`, `fail`, `retry`, `remove`, `onIncoming`, `markRead`, `unreadPeerIds`, `findMine`. Matching is server id first, then client_msg_id for own messages; Read never downgrades; the item key stays `c:<client_msg_id>` so the bubble is not recreated; server-backed rows are stable-sorted by createdAt, with local unsent bubbles after them. All id comparisons ignore case.
- **BlockState**: block/unblock/NOT_BLOCKER/THREAD_BLOCKED transitions, `applyScan` (a thread missing from `GET /chats` leaves state unchanged unless this session blocked it), `showsBlock`, `showsUnblock`, `menuEntry`, `ownComposerMode`, and top-level `composerMode(block, recipientUnavailable)` (blocked modes win over Unavailable).
- **ChatSessionState**: memory-only open thread id and blocked-by-me set, bound `@IntoSet` as a `SessionDataCleaner` in `ChatModule`, which also provides `CurrentUserIdProvider` (me id, falling back to auth user id) and `ClientMsgIdGenerator` (UUID).

## Verification

- `:app:testDevDebugUnitTest --tests '*ChatSessionStateTest' --tests '*ThreadReducerTest' --tests '*BlockStateTest'`, `ktlintCheck` and `:app:assembleDevDebug` all pass (ThreadReducerTest 14 tests, BlockStateTest 15, ChatSessionStateTest 3).
- `scripts/check-design-rules.sh` prints "Design rules: OK".

## TDD Gate Compliance

RED (test) commits precede GREEN (feat) commits for each task: 029d22b/53bc7ee, f2f8c1e/53d61bb, 1f1f798/1930718. RED states were test files that referenced types not yet present (compile failure); no refactor commits were needed.

## Deviations from Plan

None. Plan executed as written. Minor note: the Task 1 commit (53bc7ee) contains only the merge/ack part of the reducer; the Task 2 rules were added in 53d61bb.

## Known Stubs

None.

## Threat Flags

None. T-12-05 mitigated by the `@IntoSet SessionDataCleaner` binding and `clearSessionDataResetsEverything`; T-12-06 mitigated by the `onNotBlocker` latch and `scanNeverReEnablesUnblockWhileStillBlocked`.

## Deferred / Open Items

- The "duplicate echo suppression on device" truth (backstop) needs a manual two-client check once the thread screen exists; manual testing is deferred per project policy.

## Self-Check: PASSED

All ten files exist and all six commits are ancestors of HEAD.
