---
phase: 12-chat-report-block
reviewed: 2026-10-27T00:00:00Z
depth: standard
files_reviewed: 11
files_reviewed_list:
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/ChatSessionState.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/ChatThreadViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/ThreadItem.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/expertprofile/ExpertProfileScreen.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/chat/ChatSessionStateTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/ChatThreadScreenTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/ChatThreadTestHarness.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/ChatThreadViewModelSendTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/ChatThreadViewModelTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/ThreadReducerTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/expertprofile/ExpertProfileScreenTest.kt
findings:
  critical: 0
  warning: 3
  info: 3
  total: 6
status: issues_found
---

# Phase 12: Code Review Report (gap-closure re-review, plans 12-16 and 12-17)

**Reviewed:** 2026-10-27
**Depth:** standard (incremental: `git diff 7737143..HEAD`, 11 files)
**Files Reviewed:** 11
**Status:** issues_found

## Summary

All four targeted findings are fixed. The fixes introduce no crash or data-corruption bug. I found three warnings in the new application-scoped send path (WR-03 fix). All three are narrower than the original defect, and none is a regression of the other three fixes.

Verified safe:
- The `@ApplicationScope` provider is `SupervisorJob() + Dispatchers.Default` (`di/SessionModule.kt:44`), so a failed `async` cannot cancel the application scope.
- `apiCall` rethrows `CancellationException` and maps everything else to `ApiException`, so `await()` only surfaces `ApiException` or cancellation.
- `catch (ApiException)` precedes `catch (CancellationException)` and the two types are unrelated.
- If the VM is cleared after the request has failed but before the awaiting continuation runs, the continuation resumes with cancellation, so the text is kept. There is no double restore.
- The `unsentKeys` collector cannot loop: `takeUnsentText` on an absent key leaves an equal map, so the `StateFlow` does not re-emit.
- On the Unblocked notice, `ReportAndBlockViewModel.unblock()` calls `clearBlockedByMe` before `threadNotice.post`, so the restarted scan reads a cleared session mark.

## Prior findings re-checked

| ID | Verdict | Evidence |
|----|---------|----------|
| CR-01 | FIXED | `ExpertProfileScreen.kt:124-126`: `scope.launch { showSnackbar }` on the composition scope, then `consumeSnackbar()`. Consuming the flag now changes only the `LaunchedEffect` key, not the coroutine hosting the snackbar. Three new Robolectric tests (thanks, "You blocked Ada", fallback) assert the snackbar is displayed after idle. |
| WR-01 | FIXED | `ChatThreadViewModel.kt:484-499`: the Unblocked notice applies `onUnblockSucceeded()` directly, and an in-flight scan is restarted. The empty-thread, failed-rescan and stale-in-flight-scan cases are each covered by a test. The `when (notice)` is exhaustive over the sealed type and fails to compile if a notice type is added without a decision. |
| WR-03 | FIXED, with the caveats in GR-WR-01 to GR-WR-03 | The POST runs in `sendScope.async`, so Back no longer cancels it. On a post-close failure the text is kept, keyed by owner and thread, in memory only, and restored once. |
| IN-02 | FIXED | `ThreadItem.kt:74-79`: `c:` only when `mine && clientMsgId != null`, else `s:<serverId>`. A peer row can no longer take my key, so the duplicate-key crash is closed. Peer keys no longer flip on a reload that adds a `client_msg_id`. Three reducer tests and one screen test cover it. I found no new collision: `SystemEvent` and peer keys are both `s:` + a server id (disjoint id space), and mine rows are unique per (thread, sender). |

## Warnings

### GR-WR-01: A failed send that is restored after the thread closed loses its idempotency key, so an ambiguous failure can double-send

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/ChatThreadViewModel.kt:243-250` (with `ChatSessionState.kt:68-76`)
**Issue:** While the screen is alive, a network error or 5xx leaves a Failed bubble that keeps its `client_msg_id`. The retry is then server-deduplicated, which is the stated reason for D-01. On the new post-close path, any failure (`cause != null`) is turned into plain text. That includes `IOException` and 5xx, where the server may well have stored the message and only the response was lost, for example a read timeout after commit. The text returns to the composer with no id, the user re-sends it, and a second message is created with a fresh `client_msg_id`. The old design's dedupe protection is lost exactly in the ambiguous-failure cases.
**Fix:** Keep the pending id with the text for ambiguous failures (not 4xx refusals):
```kotlin
// ChatSessionState: store UnsentMessage(text, clientMsgId?) and, for network/5xx, restore a Failed bubble
// (Delivery.Failed with the same clientMsgId) instead of composer text.
```
Alternatively, restore ambiguous failures as a Failed bubble in the reopened thread and restore refusals (4xx) as composer text. At a minimum, document the double-send risk.

### GR-WR-02: A thread reopened while a send is still in flight does not show the message once the send succeeds

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/ChatThreadViewModel.kt:121-125, 232-235` (the success path exists only for the old VM)
**Issue:** After Back and a quick reopen, the new VM B loads page 1 while the old VM A's POST is still in flight. The socket never echoes my own message (documented in `ThreadReducer`). If B's load reaches the server before the POST commits, the message is absent from B, and nothing tells B when the POST succeeds. A's `await()` was cancelled, so nobody applies the ack. The message stays invisible until the next quiet reload (resume, socket reconnect, booking update). The composer was cleared at Send, so the user sees an empty composer and no sent message, and may type it again. This is a duplicate-send trigger created by the WR-03 fix.
**Fix:** Report successful post-close sends as well as failures. Use a small signal on `ChatSessionState` (for example `completedSends: SharedFlow<String /*threadId*/>`) that a live VM collects to call `reloadQuietly()`. Alternatively, have `sendScope` hand the ack to the session state so that a VM which opened later can append the bubble.

### GR-WR-03: Post-close restore ignores the refusal classes, and unsent text can outlive logout

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/ChatThreadViewModel.kt:243-250`, `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/ChatSessionState.kt:101-105`
**Issue:**
1. Any failure, including 403 `ACCOUNT_SUSPENDED` and 403 `THREAD_BLOCKED`, is parked as composer text. The block and suspension handling in `handleSendFailure` is skipped. The user later reopens the thread, sees their old text in the composer, and gets no explanation of why it was not sent. A blocked thread is still handled by the next block scan.
2. `clearSessionData()` empties `unsentText`, but the `@ApplicationScope` send keeps running after logout. A failure that lands after the clear calls `keepUnsentText` and re-populates the singleton with the signed-out user's message text, held in memory until process death. The owner key prevents another account from seeing it. It still contradicts the class contract ("one account never sees another account's state") and holds user text longer than intended. It is also untested.
**Fix:** For (2), have `keepUnsentText` take a session generation or epoch captured at send time and ignore the call when the epoch has changed:
```kotlin
// ChatSessionState
private val epoch = AtomicInteger()
fun currentEpoch() = epoch.get()
fun keepUnsentText(threadId: String, ownerId: String, text: String, epoch: Int) {
    if (epoch != this.epoch.get()) return
    ...
}
override suspend fun clearSessionData() { epoch.incrementAndGet(); ... }
```
For (1), keep the refused statuses out of the kept text, or tag the kept item with the error code so the thread can show the matching state on reopen.

## Info

### GR-IN-01: A Failed bubble still on screen when the user leaves is lost, which is inconsistent with the new guarantee

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/ChatThreadViewModel.kt:379-383`
**Issue:** The WR-03 fix covers only sends that fail after the screen closed. A bubble that already failed (5xx or network) and is shown as Failed at Back time is dropped with the VM, and its text is neither kept nor restored. The disposition note ("failed or refused text returns to the thread's composer") reads as if every failed text returns.
**Fix:** In `onCleared`, call `keepUnsentText` for each `Delivery.Failed` mine bubble that has no server id, or document the limit in the disposition.

### GR-IN-02: Kept texts are joined and then truncated by the composer cap, silently dropping the tail

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/ChatSessionState.kt:72-76`, `ChatThreadViewModel.kt:292-297`
**Issue:** `keepUnsentText` concatenates every failed text with a newline, and `restoreToComposer` caps the result at `COMPOSER_MAX`. After a few failed 2000-character messages, the later ones are cut off without notice, which contradicts "a refused message is never lost". `restoreToComposer` already had this property for the draft, but the new accumulation makes it reachable. The map also holds text for threads that are never reopened, with no bound.
**Fix:** Keep a list per key and restore as much as fits, or accept the limit and note it. Consider a small per-key cap.

### GR-IN-03: Missing tests for the new edge cases

**File:** `app/src/test/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/ChatThreadViewModelSendTest.kt:310-415`
**Issue:** The new tests cover survive, network failure, refusal and a reopened thread. There is no test for logout during an in-flight send (GR-WR-03), for a send that succeeds while a reopened VM is loading (GR-WR-02), or for a null `ownerId` (a send started while signed out: the CancellationException path silently keeps nothing). `sendScope` in the harness is a `Dispatchers.Main` scope that is never cancelled, which is fine for tests but hides ordering differences from `Dispatchers.Default` in production, where `invokeOnCompletion` runs on a background thread. `keepUnsentText` is thread-safe via `update`, so this is not a bug today.
**Fix:** Add the three cases above.

---

_Reviewed: 2026-10-27_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
