---
phase: 12-chat-report-block
plan: 09
subsystem: chat
tags: [chat-thread, compose, lazycolumn, system-events, menu]
status: complete
requires:
  - phase: 12-03
    provides: ChatThreadKey, openChatBooking / openChatPeerProfile / openChatReportBlock, chat_thread_title_fallback
  - phase: 12-06
    provides: ChatThreadViewModel (assisted Hilt), ChatThreadUiState, ThreadLoad
provides:
  - "ChatThreadScreen (Hilt binding, lifecycle) and stateless ChatThreadContent with a bottomBar slot"
  - "ThreadRow model and buildThreadRows (date headers, run grouping)"
  - "MessageBubble / MessageStatusLine, SystemEventRow, DateSeparator, LoadEarlierButton, TypingStrip, JumpToLatestButton"
  - "ChatThreadSkeleton / ChatThreadError / ChatEmptyThread, ThreadOverflowMenu"
  - "ChatThreadUiState.toThreadKey()"
affects: [12-14]
tech-stack:
  added: []
  patterns:
    - "reverseLayout LazyColumn over newest-first rows with per-row top padding for run grouping"
    - "Key-change LaunchedEffect for conditional auto-scroll to the newest message"
key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/ChatThreadScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/ThreadRows.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/subviews/MessageBubble.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/subviews/SystemEventRow.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/subviews/ThreadListParts.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/subviews/ChatThreadStates.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/subviews/ThreadOverflowMenu.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/ChatThreadScreenTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/ThreadRowsTest.kt
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/ChatThreadRoute.kt
    - app/src/main/res/values/strings_chat_thread.xml
key-decisions:
  - "Run grouping spacing is per-row top padding (xs when tightAbove, else sm) because LazyColumn arrangement spacing cannot vary per item"
  - "The route reads the live ViewModel state for View profile and Report & Block so a peer filled in from the scanned thread row is used, not the stale route key"
  - "Auto-scroll and the Jump to latest button use the same threshold: first visible index beyond the visible item count means more than a viewport away"
  - "Scaffold keeps its default insets (innerPadding includes the navigation bar), so 12-14's composer in the bottomBar slot must not add the navigation inset a second time unless it also changes the Scaffold insets"
patterns-established:
  - "ThreadOverflowMenu is self-hiding: it renders nothing when neither View profile nor the safety item applies"
requirements-completed: [CHAT-02, CHAT-04, SAFE-03]
metrics:
  tasks: 3
  commits: 3
  tests_added: 28
actuals:
  tokens: 18800
  tasks: 3
  commits: 3
commits: 3
plan_head_before: daa12dd1d9ff48e83c44a9ae369cb3bb3b9ce2c5
plan_head_after: 6872d7800821c8fa98d87ef8b8cbf13f23168019
completed: 2026-10-27
---

# Phase 12 Plan 09: Chat thread screen, read side Summary

**The thread opens at the newest message (reverseLayout LazyColumn) with the peer in a custom top bar, own and incoming bubbles with Sending / time / Read status, date separators and run grouping, tappable booking event rows, Load earlier, typing strip, jump to latest, loading / error / empty states, pull-to-refresh and a state-aware overflow menu.**

## What was built

- **Task 1 (tracer), `5fdbee8`:** `ChatThreadRoute` builds the assisted `ChatThreadViewModel` and wires `openChatBooking`, `openChatPeerProfile` and `openChatReportBlock`. `ChatThreadScreen` runs the lifecycle triad (ON_RESUME / ON_PAUSE / dispose / first composition) and `ChatThreadContent` renders the top bar (back arrow, 32dp avatar, peer name with the "Chat" fallback) and a `reverseLayout` list with stable `ThreadItem.key` keys. `MessageBubble` follows UI-SPEC (primary / surfaceContainerHigh, tail corners, 320dp and 75% width cap, 56dp opposite gap, selectable plain text) with `MessageStatusLine` (time, "Sending…", " · Read" with Read in primary, 150 ms fade).
- **Task 2, `d8a8c2e`:** `buildThreadRows` (local-day headers in the given zone, same-sender same-minute runs, avatar on the last incoming message of a run, events break runs), `SystemEventRow` (kind title and glyph, "{topic} · {EEE, MMM d} · {time}", topic, or server content; chevron and `Role.Button` with "View booking" only with a booking id, routing `isTerminal`), `DateSeparator` ("Today", "Yesterday", medium date) and `LoadEarlierButton` (disabled "Loading…" state, last item of the reversed list).
- **Task 3, `6872d78`:** `ChatThreadSkeleton`, `ChatThreadError`, `ChatEmptyThread`, `TypingStrip`, `JumpToLatestButton` (`SmallFloatingActionButton`), `ThreadOverflowMenu` (View profile for client mode and expert peers, disabled without a peer id; exactly one of Block or report / Unblock or report / Report; hidden on a self thread; button absent when empty), `PullToRefreshBox`, the refresh-failure snackbar, near-newest auto-scroll and a `bottomBar` slot for 12-14's composer.

## Verification

- `ChatThreadScreenTest` 22 tests, `ThreadRowsTest` 6 tests: all green. `ktlintCheck` passes; `bash scripts/check-design-rules.sh` prints "Design rules: OK".
- Acceptance greps: `reverseLayout = true`, `creationCallback`-equivalent (`hiltViewModel<..., Factory>(creationCallback = ...)`), `SelectionContainer`, `fun buildThreadRows`, `isTerminal`, `chat_event_booking_completed`, `SmallFloatingActionButton`, `ThreadMenuEntry.UnblockOrReport`, `PullToRefreshBox` all present; `@Test` count 22 (>= 15).
- Tracer feedback gate: Task 1's slice was re-verified end to end (4 tests green, route compiles against the real ViewModel factory) before Tasks 2 and 3 expanded; no `blocking-human` gate on the task.
- RESEARCH A2 (reverseLayout auto-scroll behavior) is pinned by `jumpToLatestAppearsWhenScrolledAway`, `incomingMessageAutoScrollsWhenAtTheNewestEnd` and `incomingMessageDoesNotMoveAReaderOfOldHistory`; they pass under Robolectric.
- Backstop (device) items from the plan remain open: realtime arrival with a second client, typing / read receipts against the iOS client, long-locale subtitle wrapping.

## Deviations from Plan

### Auto-fixed Issues

**1. [Process] Tests and implementation committed together per task, no separate RED commit**
- A RED run is a compile failure (the symbols do not exist yet), not an assertion failure, so it is not valid RED evidence; the plan is `type: execute`. Same approach as 12-01 and 12-06. All tests were run green before each commit.

**2. [Rule 3 - Blocking] `ThreadRows.kt` filename lint**
- **Issue:** ktlint `standard:filename` wants a single-class file named after the class (`ThreadRow.kt`); the plan fixes the name `ThreadRows.kt`.
- **Fix:** `@file:Suppress("ktlint:standard:filename")`, the convention already used in `Color.kt` and `BookingSlots.kt`.
- **Commit:** d8a8c2e

**3. [Deviation] Ledger file not written**
- The worktree git-dir ledger could not be created by the sandboxed command forms; `plan_head_before` is the known spawn base `daa12dd`, confirmed by the worktree base check at start.

### Plan interpretation notes

- `MessageBubble` takes a precomputed `timeText` and merges to one node with `contentDescription` set to the speaker ("You" or the peer name), leaving the text, time and "Read" as merged text so TalkBack reads "{speaker}, {text}, {time}, Read" while test text matching stays exact.
- `Delivery.Failed` renders like a sent message (its time) here; the failed visuals, retry and delete are 12-14's.
- The "Unblocked {name}" thread notice (`ChatThreadUiState.notice`) is not shown by this plan; the plan does not list it and it belongs with 12-14's thread-level surfaces.

**Total deviations:** 3 (1 process, 1 lint, 1 environment). **Impact:** none on the planned contract.

## Known Stubs

None. `ChatEmptyThread` accepts an optional action that 12-14 supplies ("Send a message"); `ChatThreadContent.bottomBar` defaults to empty until 12-14 supplies the composer. Both are intentional extension seams.

## Threat Flags

None. T-12-27 is mitigated (plain `Text` in a `SelectionContainer`, no link detection or HTML); T-12-28, T-12-29 accepted as planned. The unused string `chat_thread_loading` is used by the skeleton's content description.

## Issues Encountered

- Gradle runs took 3 to 11 minutes under six-way contention; one early `ktlintFormat` + test run was lost to a missing test import (`assertDoesNotExist` is a member, not an import).

## Next Phase Readiness

12-14 can place the composer / blocked banner in `ChatThreadContent(bottomBar = ...)`, pass the "Send a message" `EmptyStateAction` into `ChatEmptyThread` (the empty state is currently rendered without an action in `ChatThreadContent`), and add the failed-bubble interaction on `MessageBubble`. Note the Scaffold inset: `innerPadding` already includes the navigation bar inset.

## Self-Check: PASSED

- Created files found: ChatThreadScreen.kt, ThreadRows.kt, MessageBubble.kt, SystemEventRow.kt, ThreadListParts.kt, ChatThreadStates.kt, ThreadOverflowMenu.kt, ChatThreadScreenTest.kt, ThreadRowsTest.kt.
- Commits in history: 5fdbee8, d8a8c2e, 6872d78.
