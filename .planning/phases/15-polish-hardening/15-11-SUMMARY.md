---
phase: 15-polish-hardening
plan: 11
subsystem: chat
tags: [chat, conversations, refresh, report-block, sheets, tracer]
status: complete
requires: [15-01, 15-02, 15-04, 15-10]
provides:
  - "ConversationsViewModel.LoadMode.Realtime and ChatThreadViewModel.LoadMode.Realtime: reconnect, unknown-thread and booking.update reloads keep content and flag refreshFailed"
  - "RefreshFailedSnackbarEffect (shared common_refresh_failed copy, Retry runs the pull refresh) in ConversationsScreen and ChatThreadScreen"
  - "Conversation row: one node '{name}, {last message}, {time}, {n} unread', one-line visible preview"
  - "Report & Block content and Conversations rows capped at 600 dp"
  - "Report sheet: restoration, restored-details cap and above-IME proven by tests"
affects: [15-17]
tech-stack:
  added: []
  patterns:
    - "LoadMode.Realtime = Quiet that flags failure (same shape as MyBookings, 15-01)"
key-files:
  created: []
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/conversations/ConversationsViewModel.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/conversations/ConversationsScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/conversations/subviews/ConversationRow.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/ChatThreadViewModel.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/ChatThreadScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/reportandblock/ReportAndBlockScreen.kt
    - app/src/main/res/values/strings_chat_conversations.xml
    - app/src/main/res/values/strings_chat_thread.xml
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/chat/conversations/ConversationsViewModelTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/chat/conversations/ConversationsScreenTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/ChatThreadViewModelRealtimeTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/ChatThreadScreenTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/chat/reportandblock/ReportAndBlockScreenTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/components/safety/ReportUserSheetTest.kt
key-decisions:
  - "The Conversations unknown-thread message.new reload is also Realtime (the plan said 'any realtime-triggered reload'; D-05 says realtime-triggered refetch failures get the snackbar)"
  - "Dropped the per-screen strings chat_conversations_refresh_failed and chat_thread_refresh_failed (identical to common_refresh_failed, now unused)"
  - "ComposerBlockedBanner already had a polite live region from Phase 12; only the test was added"
  - "ReportUserSheet itself needed no code change: restoration and cap live in ProfileSafetyViewModel and the field, and the new tests prove them"
requirements-completed: [POL-01, POL-02, POL-03, POL-04]
plan_head_before: 0af6f760eba6f0b5aa7dadeeb36a695797310305
plan_head_after: e6e8c01a4cc744921163d2439d43e35833ccef1c
commits: 4
actuals:
  tokens: 21000
  tasks: 3
  commits: 4
completed: 2026-10-29
---

# Phase 15 Plan 11: Chat refresh standard, row semantics and Report sheet Summary

Both chat screens now keep their rows or messages and offer "Retry" on a failed pull, reconnect or booking-event reload (tab return and screen-shown reloads stay silent), conversation rows read as one full node, and the Report sheet's restoration, cap and keyboard behaviour are covered by tests.

## What was built

- **ConversationsViewModel**: new `LoadMode.Realtime`. The reconnect collector and the unknown-thread `message.new` reload use `realtimeOrLoud()` (Realtime when Loaded, Loud over the skeleton). Realtime keeps rows and sets `refreshFailed` on failure; `onScreenShown` stays Quiet and silent.
- **ConversationsScreen**: the action-less snackbar effect is replaced by `RefreshFailedSnackbarEffect(onRetry = onRefresh)`. The search field and every row use `contentMaxWidth()`; the pull-to-refresh area stays full width. The first-load error block ("Couldn't load conversations" plus Retry) was already compliant and is unchanged.
- **ChatThreadViewModel**: new `LoadMode.Realtime`; `reloadQuietly()` (booking.update, reconnect) is Loud while the first load runs, Realtime afterwards. Messages and failed/pending bubbles are untouched on failure.
- **ChatThreadScreen**: `RefreshFailedSnackbarEffect(onRetry = onRefresh)`. The thread already has `PullToRefreshBox` and `refresh() = startLoad(Pull)`, so no extra progress bar was needed. 15-10's `PeerArrivalAnnouncer`, `newestOwnKey` and composer behaviour are untouched.
- **ConversationRow**: description is now `name, last message, time` then `, n unread` then `, online`; the visible preview is `maxLines = 1` (was 2) while the node keeps the full text.
- **ReportAndBlockScreen**: the content column uses `contentMaxWidth()` (confirmed to be an actions screen with no first-load fetch).

## Tests

All green with `ktlintCheck` and `scripts/check-design-rules.sh` ("Design rules: OK"); a wider run of `*screens.chat*`, `*components.safety*`, `*expertprofile*` is green, including 15-10's ChatComposerTest, ChatTalkBackFlowTest, ChatThreadScreenTest and ChatThreadViewModelTest.

- ConversationsViewModelTest (+4): `reconnectReloadFailureFlags`, `unknownThreadReloadFailureFlags`, `quietTabReturnFailureIsSilent`, `pullFailureStillFlags`.
- ConversationsScreenTest (+4): `refreshFailedShowsRetryThatRefreshes`, `firstLoadErrorNamedWithRetry`, `listCapped` (w900dp), `rowIsOneFullNode` (300-character message: description equals "Ada Lovelace, {300 chars}, 9:41 AM, 2 unread", one laid-out line).
- ChatThreadViewModelRealtimeTest (+4): `bookingUpdateReloadFailureFlags`, `reconnectReloadFailureFlags`, `quietReloadStillSilent`, `pullFailureStillFlags`.
- ChatThreadScreenTest (+3): `refreshFailedShowsRetry` (composer draft untouched), `firstLoadErrorNamed`, `blockedBannerIsPoliteLiveRegion`.
- ReportUserSheetTest (+3, now on `createAndroidComposeRule`): `detailsSurviveRestore` (real `ProfileSafetyViewModel` and `StateRestorationTester`), `restoredDetailsCapped` (1050 characters restored shows 1000), `submitAboveIme` (300 px IME on 360x800: details field and Submit bottoms at or below 500 dp after scrolling to them).
- ReportAndBlockScreenTest (+3): `inFlightAndErrorKept`, `blockInFlightLocksTheBlockButton`, `unblockInFlightIsAnnouncedAndLocked`, `capped` (w900dp).

## Deviations from Plan

**1. [Rule 2 - Missing critical functionality] Unknown-thread reload is Realtime too**
- **Found during:** Task 1. The plan named the reconnect collector; the Conversations `message.new` for an unknown thread also triggers a reload through the same helper and was silent on failure. D-05 covers it, so `quietOrLoud()` became `realtimeOrLoud()` for both. Test `unknownThreadReloadFailureFlags`.

**2. [Plan wording] ComposerBlockedBanner needed no code change**
- The banner already carried `liveRegion = LiveRegionMode.Polite` (Phase 12). The acceptance grep (== 1) holds; only `blockedBannerIsPoliteLiveRegion` was added.

**3. [Plan wording] `submitAboveIme` runs on the sheet content, not the sheet window**
- As the plan allowed, the test renders `ReportUserSheetContent` in the Activity and dispatches the IME inset to it; the `ModalBottomSheet` window is not exercised under Robolectric (see open items). Because the body scrolls inside the space above the keyboard, the test scrolls to the field and to Submit before measuring.

**4. [Plan wording] Report-in-flight does not lock the Block button**
- The first draft of `inFlightAndErrorKept` asserted the Block button disabled while a report submits. Existing behaviour only locks Block while Block or Unblock runs (Back is locked for all three). The test asserts what ships; no behaviour change.

**5. TDD shape**
- Task 1 has a separate failing-test commit (4 failures, then green). For Tasks 2 and 3 the VM, screen and test changes were small and written together, so RED and GREEN share one commit each; every new case passes against the final code.

**6. String cleanup**
- Removed `chat_conversations_refresh_failed` and `chat_thread_refresh_failed` (duplicates of `common_refresh_failed`, unused after the switch to the default message).

**Total deviations:** 1 auto-added (Rule 2), 5 wording or shape notes. **Impact:** none on scope.

## Known Stubs

None.

## Threat Flags

None. T-15-24 mitigated: reloads stay latest-wins in `startLoad`, and the snackbar helper stays silent while an identical one shows. T-15-25 mitigated: `capText` on restore (existing) and `restoredDetailsCapped`. T-15-SC accepted: no package installs.

## Open items (device checklist for 15-17; nothing blocks on a person)

- Samsung SM-A075F: kill the network, pull to refresh in Conversations and in a thread (snackbar with Retry, content kept), then toggle airplane mode during a thread to see the reconnect failure path.
- TalkBack: a Conversations row should read "{name}, {last message}, {time}, {n} unread" as one stop (check it does not also read the child Text again); the blocked banner announces when a blocked thread opens.
- Report sheet on hardware: the `ModalBottomSheet` window with the real keyboard (Robolectric tests exercised the sheet body, not the dialog window), and "Don't keep activities" restore of reason and typed details.
- Large-window check of Conversations rows and Report & Block at 600 dp on a tablet or foldable.

## Commits

- 95010fc0 test(15-11): failing tests for the Conversations refresh-failed Retry and the 600 dp list
- 3a437ed0 feat(15-11): a failed Conversations refresh or reconnect reload keeps the rows and offers Retry
- 04ade057 feat(15-11): a failed thread reload after a booking event or reconnect keeps the messages and offers Retry
- e6e8c01a feat(15-11): conversation rows read as one full node, Report and Block capped, Report sheet restoration and IME proven

## Self-Check: PASSED

All four commits are ancestors of HEAD; acceptance greps hold (Realtime 3 and 4, RefreshFailedSnackbarEffect 1 and 1, contentMaxWidth 1, LiveRegionMode.Polite 1); the three task verify commands and the wider chat run are green.
