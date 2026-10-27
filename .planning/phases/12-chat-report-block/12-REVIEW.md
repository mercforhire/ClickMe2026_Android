---
phase: 12-chat-report-block
reviewed: 2026-10-27T00:00:00Z
depth: standard
files_reviewed: 87
files_reviewed_list:
  - app/src/main/java/com/uptrendinvestments/clickmeapp/components/chat/MessageButton.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/components/safety/BlockUserDialog.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/components/safety/ReportReasonGroup.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/components/safety/ReportUserSheet.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/di/ChatModule.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/managers/CurrentUserIdProvider.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/model/enums/ErrorCode.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/model/enums/UserReportReason.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/model/requests/ChatRequests.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/ChatPayloads.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/navigation/Navigator.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/navigation/StackOrigin.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/network/ApiRequestUrls.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/network/ClickMeApi.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/call/calling/CallOverlayHost.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/call/calling/MeetingCallScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/call/calling/MeetingCallUiState.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/call/calling/subviews/MeetingCallControls.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/BlockState.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/ChatEntries.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/ChatEntryViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/ChatLaunchRequest.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/ChatLaunchViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/ChatLauncher.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/ChatSessionState.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/ChatsRepository.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/ClientMsgIdGenerator.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/SafetyErrors.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/TextCap.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/ChatMessageMapping.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/ChatThreadKey.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/ChatThreadRoute.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/ChatThreadScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/ChatThreadUiState.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/ChatThreadViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/ThreadItem.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/ThreadReducer.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/ThreadRows.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/subviews/ChatComposer.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/subviews/ChatThreadStates.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/subviews/ComposerBlockedBanner.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/subviews/FailedMessageMenu.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/subviews/MessageBubble.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/subviews/SystemEventRow.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/subviews/ThreadListParts.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/subviews/ThreadOverflowMenu.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/conversations/ConversationTimeLabel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/conversations/ConversationsRoute.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/conversations/ConversationsScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/conversations/ConversationsUiState.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/conversations/ConversationsViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/conversations/subviews/ConversationRow.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/conversations/subviews/ConversationsSearchField.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/conversations/subviews/ConversationsStates.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/reportandblock/ChatReportBlockKey.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/reportandblock/ReportAndBlockRoute.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/reportandblock/ReportAndBlockScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/reportandblock/ReportAndBlockViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/reportandblock/subviews/BlockCard.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/reportandblock/subviews/ReportCard.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/bookingsummary/BookingSummaryKey.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/bookingsummary/BookingSummaryScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/bookingsummary/BookingSummaryUiState.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/expertprofile/ExpertProfileKey.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/expertprofile/ExpertProfileScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/expertprofile/ProfileSafetyViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/expertprofile/subviews/ExpertProfileActionButtons.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/home/HomeClientScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/MyBookingsScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/mybookings/subviews/UpcomingBookingCard.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/upcomingbooking/UpcomingBookingKey.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/upcomingbooking/UpcomingBookingScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/bookingsummary/ExpertBookingSummaryKey.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/bookingsummary/ExpertBookingSummaryScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/home/HomeExpertScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/requestdecision/RequestDecisionScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/requestdecision/RequestDecisionUiState.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/requestdecision/RequestDecisionViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/requestdecision/subviews/RequestActionFooter.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/upcomingbookings/ExpertBookingsScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/upcomingbookings/subviews/ExpertUpcomingCard.kt
  - app/src/main/res/values/strings_call.xml
  - app/src/main/res/values/strings_chat_conversations.xml
  - app/src/main/res/values/strings_chat_entry.xml
  - app/src/main/res/values/strings_chat_report_block.xml
  - app/src/main/res/values/strings_chat_thread.xml
  - app/src/main/res/values/strings_report_user.xml
findings:
  critical: 1
  warning: 6
  info: 5
  total: 12
status: issues_found
---

# Phase 12: Code Review Report

**Reviewed:** 2026-10-27
**Depth:** standard
**Files Reviewed:** 87
**Status:** issues_found

## Summary

Reviewed the chat thread, conversations, report/block, entry-point and call-overlay code against CHAT-UNBLOCK.md and USER-REPORTING.md. The reducer, send/ack reconciliation, single-flight guards and the request DTOs (strict schema keys, redacted `toString`) are largely sound. There is no logging of user text in any reviewed file.

I found one defect that makes a required user-facing confirmation effectively invisible (Expert Profile report/block snackbar). I found six robustness or state-machine problems: a stale block state after unblock on an empty thread, and a Report & Block screen that offers actions before its scan returns. The others are an optimistic send that dies with the screen, default `toString` on state classes that hold user text, a duplicate suspended dialog on the send path, and a navigation helper that tears down an open thread for a cosmetic key difference. Info items are cosmetic or minor.

## Critical Issues

### CR-01: Expert Profile report/block snackbar cancels itself and never shows reliably

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/expertprofile/ExpertProfileScreen.kt:113-124`
**Issue:** `LaunchedEffect(safetyState.snackbar)` calls `safety.consumeSnackbar()` and then `snackbarHostState.showSnackbar(message)` in the same coroutine. Consuming sets `snackbar = null`. That changes the effect key, so the effect coroutine is cancelled on the next recomposition. `SnackbarHostState.showSnackbar` clears `currentSnackbarData` in a `finally`, so the snackbar is dismissed within about a frame of appearing. The result is that "Thanks, we'll review this." (the USER-REPORTING section 1 required UX) and "You blocked {name}" are effectively never visible, and the user gets no confirmation that the report or block succeeded. The thread and conversations screens in this same phase avoid the problem with `scope.launch { showSnackbar(...) }` and a comment explaining why. This screen does not.
**Fix:**
```kotlin
val scope = rememberCoroutineScope()
LaunchedEffect(safetyState.snackbar) {
    val message = when (safetyState.snackbar) {
        ProfileSafetySnackbar.ReportThanks -> thanks
        is ProfileSafetySnackbar.Blocked -> blocked
        null -> null
    }
    if (message != null) {
        // Launched on the composition scope so consuming the flag does not cancel the Snackbar.
        scope.launch { snackbarHostState.showSnackbar(message) }
        safety.consumeSnackbar()
    }
}
```

## Warnings

### WR-01: Unblock can leave the thread composer locked (empty thread, or any failed rescan)

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/ChatThreadViewModel.kt:449-473` (with `BlockState.kt:57-58`)
**Issue:** After Report & Block unblocks a thread, the thread ViewModel (still on the back stack) learns of it only through `refreshBlockState()`. That scan is the only path that clears `block`. Take a thread with no messages: it is absent from `GET /chats` (A4), so the scan returns `found == null`. `ReportAndBlockViewModel.unblock()` has already called `sessionState.clearBlockedByMe`, so `applyScan(null, sessionBlocked=false)` takes the `else -> this` branch and the stale `BlockedByMe` state survives. Repro: block an expert from the profile, tap Message (thread opens empty and blocked-by-me through the session mark), then Unblock or report, then Unblock. The "Unblocked" snackbar shows but the composer banner stays and the menu still says "Unblock or report". A failed scan (network) gives the same result. CHAT-UNBLOCK section 6 says unblock 200 sets `is_blocked=false` locally.
**Fix:** When the thread ViewModel takes a `ThreadNotice.Unblocked` for its thread, apply the local transition directly:
```kotlin
launchRequest.threadNotice.consume()?.let { notice ->
    if (notice.threadId.equals(threadId, ignoreCase = true)) {
        _uiState.update {
            it.copy(
                notice = notice,
                block = if (notice is ThreadNotice.Unblocked) it.block.onUnblockSucceeded() else it.block,
            )
        }
    }
}
```

### WR-02: Report & Block offers actions before the block scan finishes, and can claim "blocked by me" falsely

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/reportandblock/ReportAndBlockViewModel.kt:94-134, 192-215`
**Issue:** The screen starts from `BlockState().applyScan(null, sessionBlocked)`, which is "not blocked" unless this session blocked the thread. It does not seed from the thread ViewModel's already-known state, which `ChatReportBlockKey` does not carry. There is no loading state, and a scan failure is silent. For a thread the peer blocked (or one blocked in an earlier session), the Block card and an enabled Block button show until the scan returns, and indefinitely if the scan fails. If the user confirms, the server answers a no-op 200 ("first blocker wins"). The code then calls `markBlockedByMe`, sets `blockedByMe=true`, posts the "Blocked {name}" notice and navigates away. That records a false session "blocked by me" mark, which for an empty thread overrides the real server state (`applyScan` prefers the session mark when `found == null`).
**Fix:** Pass the current `BlockState` (or `isBlocked` and `blockedByMe`) through `ChatReportBlockKey` as a seed, and disable Block and Unblock until the first scan completes or fails. After `block` 200, reconcile with a rescan before calling `markBlockedByMe` (CHAT-UNBLOCK section 6: "reconcile on the next GET /chats").

### WR-03: Optimistic send is tied to `viewModelScope`; leaving the thread cancels it and the message is lost silently

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/ChatThreadViewModel.kt:196-219`
**Issue:** `deliver()` launches the POST in `viewModelScope`. If the user taps Send and presses Back (or a hand-off such as `Navigator.openChatThread` pops the entry) before the response arrives, `onCleared` cancels the coroutine and the OkHttp call. The Sending bubble disappears with the ViewModel, the composer is already cleared, and the user has no signal that the message may not have been sent. The server may or may not have received it. This is the data-loss mode of an optimistic send, and D-01 only covers failures while the screen is alive.
**Fix:** Run the POST in an application-scoped scope that outlives the ViewModel, for example an injected `@ApplicationScope CoroutineScope` or a `NonCancellable` send. On failure while the screen is gone, drop the draft back into the saved composer text. At minimum, keep the draft in `SavedStateHandle` until the ack arrives.

### WR-04: State classes holding user text use the default data-class `toString`

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/ChatThreadUiState.kt:30`, `.../reportandblock/ReportAndBlockViewModel.kt:43`, `.../expertprofile/ProfileSafetyViewModel.kt:42`, `.../conversations/ConversationsUiState.kt:88`
**Issue:** The phase carefully redacts `toString` on `ChatMessageItem`, `ChatThreadItem`, `SendMessageRequest`, `ChatActionRequest`, `ReportUserRequest`, `ThreadItem.Message` and `ConversationUi`. The enclosing state classes do not. `ChatThreadUiState.composerText`, `ReportAndBlockUiState.details`, `ProfileSafetyState.details` and `ConversationsUiState.query` all print verbatim if any state is ever logged, attached to a crash report or dumped by tooling. That breaks the "message and report text never printed" invariant the rest of the phase enforces.
**Fix:** Override `toString` on these classes to print flags and lengths only (for example `composerLen=${composerText.length}`), matching the other classes.

### WR-05: Send path shows its own dialog for 403 ACCOUNT_SUSPENDED, and treats every 4xx as a refused message

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/ChatThreadViewModel.kt:208-211, 228-250`
**Issue:** The report, block and unblock paths suppress their dialog when `isAccountSuspended()` is true, because `SuspensionInterceptor` shows the global dialog. `handleSendFailure` has no such check. A suspended user sending a message gets the global suspended dialog plus a "Couldn't send message" dialog with the server text, and the message is also removed from the thread and restored to the composer. The blanket `status < 500` rule also routes 408 and 429 into the "refused, text returned to the composer" path. A 429 or 408 is transient and arguably should stay a retryable Failed bubble.
**Fix:** In the `else` branch, skip the dialog when `error.isAccountSuspended()`. Route 408 and 429 (and 401 if it ever surfaces) through the `ThreadReducer.fail` path.

### WR-06: `openChatThread` treats a thread with different display seeds as a different entry and recreates it

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/navigation/Navigator.kt:143-155`
**Issue:** The "already on top" check is `stack.lastOrNull() != key`, which is full `ChatThreadKey` data-class equality including `peerName`, `peerAvatarUrl` and `peerIsExpert`. The in-call Chat control passes `isExpert = null` and booking cards pass `true`/`false`, while the Conversations row passes the partner roles. So pressing Chat in the call overlay while that very thread is open compares unequal. The stack is popped and re-pushed, giving a new ViewModel, a lost unsent draft (`SavedStateHandle` is per entry) and any in-flight send cancelled (see WR-03). The same applies when a profile opened from the thread has a Message button.
**Fix:** Compare by thread id:
```kotlin
val top = stack.lastOrNull()
val sameThread = top is ChatThreadKey && key is ChatThreadKey && top.threadId.equals(key.threadId, ignoreCase = true)
if (!sameThread) { while (stack.size > 1) stack.removeAt(stack.lastIndex); stack.add(key) }
```

## Info

### IN-01: First composition triggers `onScreenShown` three times

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/ChatThreadScreen.kt:125-128`
**Issue:** `LifecycleEventEffect(ON_RESUME)` fires on registration when already resumed, and `LaunchedEffect(Unit)` calls it again. Each call runs `refreshBlockState()`, which fetches up to 100 threads (the `init` block already did once), and, if the initial load has finished, a Quiet reload. Same pattern in `ConversationsScreen.kt:50-53`.
**Fix:** Keep the `LaunchedEffect(Unit)` plus `ON_RESUME` only for re-entry (skip the first resume), or de-duplicate in the ViewModel with an in-flight guard on `refreshBlockState`.

### IN-02: `ThreadItem.Message.key` flips from `s:` to `c:` when a socket message is reloaded

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/ThreadItem.kt:64-65`, `ThreadReducer.kt:197-199`
**Issue:** Socket messages have `clientMsgId = null` (key `s:<id>`). `GET /chats/{id}/messages` returns `client_msg_id` for peer messages as well. After the next quiet reload, `reconcile` adopts it and the key becomes `c:<id>`, so that LazyColumn item is disposed and recreated, `animateItem` replays, and `AutoScrollToNewest` sees a key change on the newest row. The key also ignores the sender. Two rows from different senders sharing a `client_msg_id` would crash `LazyColumn` with a duplicate key.
**Fix:** Key own messages by `c:` only when `mine`, and use `s:<serverId>` for peer messages (or prefix the key with `mine`).

### IN-03: Blank profile name yields "Report " / "Block " / "Why are you reporting ?"

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/components/safety/ReportUserSheet.kt` (title and subtitle), `ExpertProfileScreen.kt:128,195`
**Issue:** `BlockUserDialog` has a blank-name fallback. `ReportUserSheet` and the `profile_menu_block` menu item do not, and `ExpertProfileUiState.name` can be empty when the key had no name.
**Fix:** Add name-less variants of those strings (as `block_dialog_title_fallback` does) and pick them when `name.isBlank()`.

### IN-04: Double bottom inset on Report & Block, and a nav-bar-sized gap above the keyboard

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/reportandblock/ReportAndBlockScreen.kt:90-102`
**Issue:** The Scaffold `innerPadding` already carries the bottom system-bar inset. The content adds `WindowInsets.navigationBars` again, and `imePadding()` is applied without `consumeWindowInsets(innerPadding)`, so with the keyboard open the content sits a nav-bar height too high. `ChatThreadScreen` handles this correctly (its comment at line 240).
**Fix:** Apply `.consumeWindowInsets(innerPadding)` before `.imePadding()` and drop the extra `navigationBars` padding.

### IN-05: Smaller consistency items

**File:** `strings_chat_thread.xml` (`chat_composer_counter`), `ReportAndBlockViewModel.kt:149-164`, `ChatsRepository.kt:44-45`
**Issue:**
- The string `%1$d/10000` hardcodes the cap that `CHAT_COMPOSER_MAX` defines. Format it from the constant.
- `reportSubmitted` is memory only. After process death the restored reason and details re-enable Submit and a second report can be filed against the 10-per-hour limiter.
- `findThread` scans only the first 100 threads (known, RESEARCH A3). A blocked thread older than that is not found. It then looks Normal until a send returns THREAD_BLOCKED, after which it shows as blocked by the peer with no Unblock even if the caller was the blocker.
**Fix:** Format the counter from the constant, persist `reportSubmitted` in `SavedStateHandle`, and document or page the scan beyond 100.

---

_Reviewed: 2026-10-27_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
