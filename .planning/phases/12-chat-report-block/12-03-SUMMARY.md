---
phase: 12-chat-report-block
plan: 03
subsystem: navigation
tags: [navigation, nav3, shells, hand-off, keys]
status: complete
requires: []
provides:
  - "ChatThreadKey / ChatReportBlockKey Nav3 routes"
  - "ChatLaunchRequest singleton (thread, conversationsNotice, threadNotice one-shot slots)"
  - "Navigator.openChatThread(tab, key)"
  - "chatEntries(navigator, mode), chatsRootFor(mode), openChatBooking / openChatPeerProfile / openChatReportBlock / returnToConversations"
  - "chatsRoot slot in both shells fed by ConversationsRoute; placeholder ChatThreadRoute / ReportAndBlockRoute / ConversationsRoute"
  - "StackOrigin { Default, Chats } and ExpertBookingOrigin.Chats"
affects: [12-05, 12-07, 12-09, 13]
tech-stack:
  added: []
  patterns:
    - "App-scoped OneShotSlot hand-off consumed by the Hilt-aware screen and passed into the Hilt-free shell"
    - "Defaulted origin field on shared keys to keep them distinct across flattened stacks"
key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/ChatLaunchRequest.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/ChatLaunchViewModel.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/ChatEntries.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/ChatThreadKey.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/ChatThreadRoute.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/reportandblock/ChatReportBlockKey.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/reportandblock/ReportAndBlockRoute.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/conversations/ConversationsRoute.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/navigation/StackOrigin.kt
    - app/src/main/res/values/strings_chat_thread.xml
    - app/src/main/res/values/strings_chat_report_block.xml
    - app/src/test/java/com/uptrendinvestments/clickmeapp/navigation/NavigatorOpenChatThreadTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/chat/ChatNavigationTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/chat/ChatLaunchRequestTest.kt
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/navigation/Navigator.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/home/HomeClientScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/home/HomeExpertScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/expertprofile/ExpertProfileKey.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/bookingsummary/BookingSummaryKey.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/upcomingbooking/UpcomingBookingKey.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/bookingsummary/ExpertBookingSummaryKey.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/HomeShellsTest.kt
decisions:
  - "openChatThread pops the Chats stack to its root before pushing a different thread (tightening of iOS chatsPath.push) so Back always lands on Conversations"
  - "ChatThreadKey carries peerId (one-field deviation from the UI-SPEC key) so typing and View profile work before the first peer message"
  - "Shared keys get a defaulted StackOrigin rather than stack-scoped content keys; BookingKey / CancelBookingKey carry no origin (accepted debt)"
metrics:
  tasks: 3
  commits: 4
  plan_head_before: 087ac37bf779d10dc8c32cfa04081e33339d1d67
  plan_head_after: 1c07c25f82af4624b64898689fa8b82029a828b9
actuals:
  tokens: 10000
  tasks: 3
  commits: 4
commits: 4
plan_head_before: 087ac37bf779d10dc8c32cfa04081e33339d1d67
plan_head_after: 1c07c25f82af4624b64898689fa8b82029a828b9
requirements-completed: [CHAT-01, CHAT-05]
---

# Phase 12 Plan 03: Chat navigation skeleton Summary

A ChatThreadKey posted to an app-scoped one-shot slot opens that thread on the Chats tab of whichever shell is showing (bottom bar hidden), Back lands on the Conversations root, and both shells now take their Chats root and all chat routes from the chat package.

## What was built

- **Task 1 (tracer):** `ChatThreadKey`, `ChatLaunchRequest` (thread / conversationsNotice / threadNotice slots), `ChatLaunchViewModel`, `Navigator.openChatThread`, placeholder `ChatThreadRoute`, `chatEntries`, and shell wiring (`pendingChatThread` + `onConsumeChatThread`, a `LaunchedEffect` that consumes the slot and calls `openChatThread`). `HomeShellsTest` drives the hand-off in both shells with a stand-in thread route (the real route will get a Hilt ViewModel). Commits: `7eec9c8` (RED), `4b1175e`.
- **Task 2:** `chatsRoot` slot in both shells fed by a placeholder `ConversationsRoute`; `ChatReportBlockKey` + placeholder `ReportAndBlockRoute` registered with `BottomBar.hidden()` in `chatEntries`; `NavigatorOpenChatThreadTest` (5 cases) and `ChatLaunchRequestTest`. Commit `4925cac`.
- **Task 3:** `StackOrigin`, defaulted `origin` on `ExpertProfileKey` / `BookingSummaryKey` / `UpcomingBookingKey`, `ExpertBookingOrigin.Chats`, and the helpers `openChatBooking`, `openChatPeerProfile`, `openChatReportBlock`, `returnToConversations`; `ChatNavigationTest` including the invariant that a Chats-origin key differs from the default one. Commit `1c07c25`.

## Verification

- `:app:testDevDebugUnitTest` for HomeShellsTest, NavigatorTest, NavigatorOpenChatThreadTest, ChatLaunchRequestTest, ChatNavigationTest, ExpertNavigationTest, BookingFlowNavigationTest: pass.
- `ktlintCheck`: pass. `:app:assembleDevDebug`: pass. `scripts/check-design-rules.sh`: "Design rules: OK".

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] ktlint chain-method-continuation in HomeShellsTest**
- **Found during:** Task 1 verify
- **Issue:** the optional-chained `chatLaunch?.thread?.pending?.collectAsState()?.value` and `.collectAsState().value` expressions violated ktlint.
- **Fix:** extracted a `@Composable pendingThreadOf(launch)` helper in the test.
- **Files modified:** HomeShellsTest.kt
- **Commit:** 4b1175e

**2. [Process] Task 3 test written alongside the implementation**
- The RED step for Task 3 was a compile failure (StackOrigin did not exist) and was not committed separately; Tasks 1 (RED commit `7eec9c8`) and 2 (tests pin already-built stack rules) are as planned.

Otherwise the plan executed as written. The `HomeShellsTest` `setClient` / `setExpert` helpers also capture the back dispatcher (reusing the existing `expertBackDispatcher` field) so system-Back tests work for both shells.

## Known Stubs

`ChatThreadRoute`, `ReportAndBlockRoute` and `ConversationsRoute` have intentionally placeholder bodies (empty scaffold / the shared `PlaceholderTabContent`), to be replaced by 12-09, 12-07 and 12-05 with the signatures unchanged. These do not block this plan's goal (navigation skeleton).

## Threat Flags

None. T-12-08 (duplicate keys across stacks) is mitigated by the Chats origins and the `chatsOriginKeyDiffersFromDefault` test; T-12-09 / T-12-10 accepted as planned.

## Self-Check: PASSED

All created files exist and commits 7eec9c8, 4b1175e, 4925cac, 1c07c25 are ancestors of HEAD.
