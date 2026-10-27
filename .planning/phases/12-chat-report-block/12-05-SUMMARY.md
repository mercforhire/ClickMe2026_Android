---
phase: 12-chat-report-block
plan: 05
subsystem: chat
tags: [conversations, viewmodel, compose, realtime, search]
status: complete
requires:
  - phase: 12-chat-report-block
    provides: ChatsRepository, ChatSessionState, CurrentUserIdProvider, ChatLaunchRequest, ChatThreadKey, ConversationsRoute signature, Wave 0 fakes and fixtures
provides:
  - "ConversationsScreen / ConversationsContent: the Chats tab root in both shells"
  - "ConversationsViewModel: load machine (Loud/Pull/Quiet), realtime fan-in, session-local unread, local search, block notice"
  - "displayUnread, filterConversations, sortConversations, ChatThreadItem.toConversationUi"
  - "relativeTimeLabel (today time, Yesterday, locale month-day)"
affects: [12-07, 12-09]
tech-stack:
  added: []
  patterns:
    - "Phase 8 My Bookings Loud/Quiet/Pull load machine reused for a list with realtime fan-in"
    - "Quiet-or-Loud helper so a quiet load never replaces a pending Loud load (failure would be swallowed)"
key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/conversations/ConversationsUiState.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/conversations/ConversationsViewModel.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/conversations/ConversationsScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/conversations/ConversationTimeLabel.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/conversations/subviews/ConversationRow.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/conversations/subviews/ConversationsSearchField.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/conversations/subviews/ConversationsStates.kt
    - app/src/main/res/values/strings_chat_conversations.xml
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/chat/conversations/ConversationsViewModelTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/chat/conversations/ConversationsScreenTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/chat/conversations/ConversationTimeLabelTest.kt
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/conversations/ConversationsRoute.kt
decisions:
  - "A message from the current user still refreshes the row preview and moves it to the top (iOS parity) but never adds to the local unread count; the plan text said 'ignore'"
  - "A failed non-Loud load over the skeleton becomes Failed instead of staying on Loading"
  - "Duplicate thread ids in one page are collapsed (distinctBy, case-insensitive) so the LazyColumn key can never repeat"
metrics:
  tasks: 3
  commits: 3
  tests_added: 39
commits: 3
plan_head_before: e0a298fc91d76d676bd22914ec24bae368e65356
plan_head_after: 31c4e08cc3922c5744d108359404ff6487d1d9b9
actuals:
  tokens: 26000
  tasks: 3
  commits: 3
requirements-completed: [CHAT-01, CHAT-04]
---

# Phase 12 Plan 05: Conversations list Summary

The Chats tab in both shells now lists the user's conversations from `GET /chats?page=1&limit=50`, keeps them live from realtime, and a row tap opens its thread on the Chats stack.

## What was built

- **Task 1 (tracer), `b0666f6`:** `ConversationsUiState` (row model, load state, `displayUnread`, `filterConversations`, stable `sortConversations`, `toConversationUi`), the `ConversationsViewModel` load machine copied from My Bookings, `relativeTimeLabel`, `ConversationsScreen` with the lifecycle triad, `ConversationRow` (avatar with presence dot, `UnreadBadge`, merged semantics, `onClickLabel`), and `ConversationsRoute` replacing the placeholder. Tracer re-verified end to end (rows, tap seed key, time labels) before expanding.
- **Task 2, `3c474ee`:** realtime fan-in (`message.new` moves a known row to the top and updates preview and time, an unknown thread reloads quietly; `user.status` flips presence; reconnect into Connected reloads quietly; the first state emission only seeds the tracker), session-local unread behind `displayUnread` (increments for peer messages unless the thread is open, clears when a thread opens or a row is tapped, survives reloads), the Blocked notice hand-off (before or after construction), and 18 ViewModel tests.
- **Task 3, `31c4e08`:** `ConversationsSearchField` (tag `chats_search`, clear button), `ConversationsSkeleton`, per-mode empty state, no-results, error with Retry, `PullToRefreshBox` wrapping every state, the "Blocked {name}" and refresh-failed snackbars, and screen plus time label tests.

## Verification

- `ConversationsViewModelTest` 18, `ConversationsScreenTest` 14, `ConversationTimeLabelTest` 7: all pass.
- `ktlintCheck`, `:app:assembleDevDebug` and `:app:lintDevDebug`: pass. `scripts/check-design-rules.sh`: "Design rules: OK".
- Acceptance greps: `fun displayUnread`, `ConversationsScreen(` in the route, both `filterIsInstance` calls, `openThreadId`, `chats_search`, `PullToRefreshBox`, `chat_conversations_empty_body_expert` all present; no `PlaceholderTabContent` in the route; zero `Log.` calls in the ViewModel; 18 `@Test` in the ViewModel test (>= 13).

## TDD Gate Compliance

The plan is `type: execute`. RED states were test files that name symbols not yet present (compile failure, not a meaningful assertion failure), so tests and implementation were committed together per task, as in 12-01.

## Deviations from Plan

**1. [Interpretation] Own messages still update the row**
- The plan said to ignore a `message.new` whose sender is the current user. The behaviour list only requires that it adds nothing to the unread count. Ignoring it fully would leave a stale preview if the user sent from another device, and iOS does not ignore it. The row is updated and moved to the top; only the unread increment is skipped. No test asserts otherwise.

**2. [Rule 1 - Bug] Quiet or Pull failure over the skeleton**
- A quiet or pull reload that cancelled a pending Loud load and then failed would have left the screen on Loading forever (the My Bookings template guards this only in its realtime handler). Realtime and reconnect triggers use `quietOrLoud()`, and any failure while the load is still Loading becomes Failed.

**3. [Rule 2 - Missing] Page de-duplication**
- Rows from a page are collapsed by lower-cased thread id so a repeated thread can never produce a duplicate LazyColumn key (edge probe "samePageTwiceKeepsOneRowPerThread").

**4. [Minor] Extra string and omitted string**
- Added `chat_conversations_blocked_snackbar_unnamed` ("Blocked") for the case where the peer name is unknown (`ConversationsNotice.Blocked(peerName: String?)`). Did not add `chat_conversations_loading`: the shared skeleton description `content_loading` already says "Loading", and an unused string would be dead resource.

**5. [Process] Task 1 pulled forward some Task 3 pieces**
- `ConversationRow` was written in full in Task 1 (presence dot, badge, semantics) so the row did not need rewriting; Task 1's screen used a minimal Loading/Failed body that Task 3 replaced with the real states.

## Known Stubs

None.

## Threat Flags

None. T-12-14: no `Log` calls in the ViewModel and DTOs redact previews. T-12-15: `startLoad` cancels the in-flight load, so an event burst for unknown threads ends in one reload.

## Deferred / Open Items

- Backstop truth "move-to-top animation with no flicker (verify on device with a second client)" is manual and deferred per project policy.
- Page 1 only (50 conversations) is the documented D-17 limit.
- At 200% font scale the row name and time wrap via `FlowRow`; not checked on device.

## Self-Check: PASSED

All eleven created files and the modified route exist; commits b0666f6, 3c474ee and 31c4e08 are ancestors of HEAD.
