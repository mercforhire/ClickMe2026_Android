---
phase: 12-chat-report-block
plan: 14
subsystem: chat-thread-ui
tags: [chat-thread, composer, failed-bubble, blocked-banner, compose]
status: complete
requires:
  - phase: 12-08
    provides: ChatThreadViewModel send/retry/delete/dismissSendError/onComposerChanged/consumeNotice
  - phase: 12-09
    provides: ChatThreadContent, MessageBubble, ChatEmptyThread, auto-scroll
provides:
  - ChatComposer (field, counter, Send button)
  - ComposerBlockedBanner (BlockedByMe / BlockedByPeer / Unavailable)
  - FailedMessageMenu and failed-bubble visuals, retry, delete, TalkBack actions
  - Send error dialog, Unblocked snackbar, empty-thread "Send a message" action
affects: [12-15]
tech-stack:
  added: []
  patterns:
    - "Composer keeps a local TextFieldValue mirrored from the ViewModel draft so the cursor stays stable"
    - "consumeWindowInsets(innerPadding) on the Scaffold content column so imePadding does not double-count the nav bar"
key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/subviews/ChatComposer.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/subviews/ComposerBlockedBanner.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/subviews/FailedMessageMenu.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/ChatComposerTest.kt
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/ChatThreadScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/subviews/MessageBubble.kt
    - app/src/main/res/values/strings_chat_thread.xml
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/ChatThreadScreenTest.kt
decisions:
  - "ChatThreadContent renders the composer itself and its bottomBar slot is removed, because the screen tests render ChatThreadContent directly and expect the composer"
  - "New ChatThreadContent callbacks are onRetryMessage/onDeleteMessage (the existing onRetry is the load-failure Retry)"
  - "Failed bubbles skip SelectionContainer so the long-press opens the Delete menu"
requirements-completed: [CHAT-03, SAFE-03, SAFE-04]
commits: 3
plan_head_before: 8777f8bd8ea9ec4536c735cc457291aa4bc818e2
plan_head_after: d2b4b19db3e9d94038694b1e24eabfddd24d1bab
actuals:
  tokens: 13000
  tasks: 3
  commits: 3
---

# Phase 12 Plan 14: Chat thread send side Summary

Chat thread send side finished: composer with counter and Send button, failed bubbles (tap to retry, long-press Delete, TalkBack actions), blocked/unavailable banners cross-faded in place of the composer, send error dialog, "Unblocked {name}" snackbar, and the empty thread's "Send a message" action, all bound to the 12-08 ViewModel.

## Tasks

| Task | Name | Commit |
|------|------|--------|
| 1 | Composer and Send wiring (tracer) | 47a2626 |
| 2 | Failed bubble retry/delete/accessibility | 42bff07 |
| 3 | Banners, error dialog, snackbar, counter, empty action | d2b4b19 |

## What was built

- `ChatComposer`: pinned bar (`surface`, 1dp top divider, `imePadding()`), `TextField` (`surfaceContainer`, `extraLarge`, 1dp `outlineVariant` / 2dp `primary` focused border, 1 to 5 lines, Sentences capitalization, tags `chat_input` / `chat_send`), 48dp circular Send that stays in layout. Counter "{n}/10000" from 9000 characters, `tertiary` at the cap, polite "Message length limit reached" description at the cap. Local over-cap pastes are truncated with the same `capText` the ViewModel uses.
- `MessageBubble`: Failed own bubble uses `primary` at 40%, `onSurface` text, 1dp `error` 50% border and the "Not sent. Tap to retry" line with an `ErrorOutline`; the bubble is a `combinedClickable` (`Role.Button`, click label "Retry sending") with custom actions "Retry sending" and "Delete message". Sent bubbles gain no click or custom action.
- `FailedMessageMenu`: one 48dp "Delete" item in `error`, no confirmation.
- `ComposerBlockedBanner`: three states per CHAT-UNBLOCK section 5; merged node with polite live region; `PersonOff` in `tertiary` for Unavailable.
- `ChatThreadContent`: `AnimatedContent` (200 ms fade) between the composer and the banner; `AuthErrorDialog` titled "Couldn't send message"; snackbar for `ThreadNotice.Unblocked` followed by `onNoticeConsumed`; empty thread passes the "Send a message" action that focuses the composer and shows the keyboard.

## Verification

- `./gradlew :app:testDevDebugUnitTest --tests '*ChatThreadScreenTest' --tests '*ChatComposerTest' --tests '*ChatThreadViewModelSendTest' --tests '*ThreadRowsTest' ktlintCheck` passes (37 + 7 + 24 + 6 tests); all `screens.chat*` unit tests pass.
- `bash scripts/check-design-rules.sh` prints "Design rules: OK".

## Deviations from Plan

### Auto-fixed / judgment calls

**1. [Rule 3 - Blocking] `bottomBar` slot replaced by built-in composer.** The plan said to fill `ChatThreadContent`'s `bottomBar` from `ChatThreadScreen`, but the plan's own tests render `ChatThreadContent` with hand-built states and expect the composer, banner, dialog and snackbar there. The composer, banner, dialog and snackbar are therefore rendered inside `ChatThreadContent`, and the unused `bottomBar` parameter was removed. Files: `ChatThreadScreen.kt`.

**2. [Rule 3 - Blocking] Callback names.** The plan's `onRetry: (String) -> Unit` collides with the existing load-failure `onRetry: () -> Unit`, so the per-message callbacks are `onRetryMessage` / `onDeleteMessage`.

**3. [Rule 1 - Bug avoidance] Insets.** The Scaffold content column now calls `consumeWindowInsets(innerPadding)` so the composer's `imePadding()` does not add the navigation-bar inset twice while the keyboard is open.

**4. Extra string** `chat_thread_unblocked_snackbar_nameless` ("Unblocked") for a notice with no known peer name.

**5. TDD commits.** Tests and implementation were committed together per task (the plan is `type: execute`, not a `type: tdd` plan), so there are no separate RED commits.

## Known Stubs

None.

## Deferred / open items (manual testing, per project policy)

- Real-device check of keyboard behavior: IME push-up of the composer, cursor stability with the local `TextFieldValue` mirror, and the long-press Delete menu on a physical Samsung device.
- TalkBack pass over the failed bubble actions and the banner live region.

## Threat Flags

None. T-12-41 mitigation (10000-unit cap) is applied both in the ViewModel (12-08) and in the field.

## Self-Check: PASSED

- ChatComposer.kt, ComposerBlockedBanner.kt, FailedMessageMenu.kt, ChatComposerTest.kt exist.
- Commits 47a2626, 42bff07, d2b4b19 are ancestors of HEAD.
