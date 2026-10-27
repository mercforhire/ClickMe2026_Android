---
phase: 12-chat-report-block
plan: 08
subsystem: chat
tags: [chat-thread, optimistic-send, errors, blocked-send, viewmodel, tdd]

requires:
  - phase: 12-chat-report-block
    provides: "12-02 ThreadReducer/BlockState, 12-06 ChatThreadViewModel read side, 12-07 capText"
provides:
  - "ChatThreadViewModel.send/retry(clientMsgId)/delete/dismissSendError"
  - "CHAT_COMPOSER_MAX (10_000) and ChatThreadUiState.isComposerAtCap"
  - "THREAD_BLOCKED / RECIPIENT_UNAVAILABLE / other 4xx handling on send"
affects: [12-09 chat screen, 12-14 composer counter]

actuals:
  tokens: 8000
  tasks: 3
  commits: 3
plan_head_before: daa12dd1d9ff48e83c44a9ae369cb3bb3b9ce2c5
plan_head_after: 013af6af8228b2d2315bc5bb031d0c95c860bb1a

tech-stack:
  added: []
  patterns:
    - "Optimistic bubble keyed by client_msg_id; ack/fail/retry through ThreadReducer"
    - "4xx send refusal removes the bubble and restores the text ahead of the newer draft"

key-files:
  created:
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/ChatThreadViewModelSendTest.kt
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/ChatThreadViewModel.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/ChatThreadUiState.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/ChatThreadTestHarness.kt

key-decisions:
  - "Composer limit lives as top-level CHAT_COMPOSER_MAX in ChatThreadUiState.kt; ChatThreadViewModel.COMPOSER_MAX aliases it so both names resolve to one value"
  - "A 4xx send refusal removes the bubble and restores the text; only network, 5xx and decoding failures leave a Failed bubble"
  - "retry(clientMsgId) is ignored unless the bubble is Failed, not in flight, and the composer mode is Normal"

requirements-completed: [CHAT-03, SAFE-03, SAFE-04]

duration: 70min
completed: 2026-10-27
status: complete
---

# Phase 12 Plan 08: Chat send side Summary

Optimistic send in `ChatThreadViewModel`: a Sending bubble appears at once, the ack turns the same bubble into Sent, failures stay retryable with the same client_msg_id, and THREAD_BLOCKED / RECIPIENT_UNAVAILABLE / other 4xx put the text back in the composer.

## What was built

- `send()` trims the composer, appends a mine/Sending bubble with `clientMsgIds.next()`, clears the composer (state and `KEY_COMPOSER`), stops typing and launches `deliver`. It is guarded by `canSend` (Loaded, Normal mode, non-blank). Sends run independently; an `inFlight` set stops a second retry of the same id.
- `deliver` applies `ThreadReducer.ack` on success. Network, 5xx and decoding errors call `ThreadReducer.fail` (no dialog). Http status below 500 goes to `handleSendFailure`.
- `retry(clientMsgId)` resends a Failed bubble with the same id; `delete(clientMsgId)` removes it locally with no request.
- Composer cap: `onComposerChanged` runs `capText(text, COMPOSER_MAX)` first (UTF-16 units, no split surrogate pair). `isComposerAtCap` is added to the UI state for 12-14's counter.
- `handleSendFailure`: removes the bubble, `restoreToComposer` (failed text first, then newer draft on a new line, capped), then THREAD_BLOCKED sets `block.onThreadBlocked()` and calls `refreshBlockState()`; RECIPIENT_UNAVAILABLE sets `recipientUnavailable`; anything else sets `sendError` to the server message or `login_error_unknown`. `dismissSendError()` clears it.

## Verification

`ChatThreadViewModelSendTest` (24 tests), plus `ChatThreadViewModelTest`, `ChatThreadViewModelRealtimeTest`, `ThreadReducerTest`, `BlockStateTest` and `ktlintCheck` pass on `:app:testDevDebugUnitTest`.

## Task commits

1. Task 1 (tracer): `42bbab9` optimistic send and ack reconciliation
2. Task 2: `aac8a2b` failed send, retry, delete, composer cap
3. Task 3: `013af6a` blocked, unavailable and other 4xx outcomes

## Deviations from Plan

None - plan executed exactly as written. Public names and existing state fields are unchanged; the `onComposerChanged(text)` parameter name is kept for 12-09.

Notes: the tracer feedback gate was satisfied by the Task 1 `<verify>` command passing end-to-end (no human gate on this task); the machine was heavily loaded, so the first Gradle run took over ten minutes.

## Known Stubs

None.

## Threat Flags

None. No new network surface; T-12-24 (retry dedupe via same client_msg_id) is covered by `retryReusesSameIdAndSucceeds`, and message text is never logged (`Log.` count is 0).

## Self-Check: PASSED

- ChatThreadViewModelSendTest.kt present; commits 42bbab9, aac8a2b, 013af6a exist on the branch.
