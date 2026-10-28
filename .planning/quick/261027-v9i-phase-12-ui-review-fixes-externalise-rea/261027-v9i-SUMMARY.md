---
phase: quick-261027-v9i
plan: 01
subsystem: chat
tags: [compose, a11y, strings, icons, ui-review]
status: complete
requirements: [CHAT-01, CHAT-02, CHAT-03, CHAT-04, SAFE-01, SAFE-02, SAFE-03]
key-files:
  modified:
    - app/src/main/res/values/strings_chat_thread.xml
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/subviews/MessageBubble.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/ChatThreadScreenTest.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/subviews/ChatComposer.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/subviews/FailedMessageMenu.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/subviews/ThreadListParts.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/subviews/ComposerBlockedBanner.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/reportandblock/subviews/BlockCard.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/reportandblock/subviews/ReportCard.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/conversations/subviews/ConversationsSearchField.kt
decisions:
  - "Thread back arrow stays AutoMirrored.Filled.ArrowBack (matches the other 4 app back arrows); deliberate exception to UI-SPEC line 40"
  - "Read-receipt TalkBack phrase is '{time}, Read' via clearAndSetSemantics (not a merge boundary), not an annotated-span single string"
actuals:
  tokens: 9000
  tasks: 3
  commits: 3
plan_head_before: ded38b21ebd66982d1c460759505ca4b6d2cb0d1
plan_head_after: 8b7bf316a5f7c132a45b65de3681913420dacd21
---

# Quick 261027-v9i: Phase 12 UI review fixes Summary

Read-receipt separator moved to string resources with a one-phrase TalkBack status ("{time}, Read"), and 11 Filled icon references across 7 chat files switched to Outlined.

## Commits
- a98b1da: Task 1, strings + MessageStatusLine semantics + 2 new Compose tests
- 2873604: Task 2, Outlined icons in thread subviews (Send, Delete, ArrowUpward/Downward, Block, PersonOff)
- 8b7bf31: Task 3, Outlined icons in BlockCard, ReportCard, ConversationsSearchField

## Verification
- `./gradlew :app:testDevDebugUnitTest ktlintCheck :app:assembleDevDebug`: BUILD SUCCESSFUL (ChatThreadScreenTest: 40 tests, 0 failures, 0 skipped)
- `scripts/check-design-rules.sh`: Design rules: OK
- Directory-wide Filled gate prints exactly one line: the ChatThreadScreen back arrow
- MessageBubble.kt has 0 `Text(text = "` literals (baseline 1)

## Deviations from Plan
None in behavior. Minor: the plan's test import list included `onAllNodes`, but it is a member of the compose rule, not a top-level import, so it was not imported.

## Decisions
- The thread back arrow in ChatThreadScreen.kt stays `Icons.AutoMirrored.Filled.ArrowBack`, as all 5 TopAppBar back arrows in the app do. A deliberate exception to UI-SPEC line 40. If Outlined back arrows are wanted, switch all 5 call sites together in the Phase 15 polish pass.
- The review's "annotated span" idea was not adopted, because the fade applies to "Read" only.

## Open items
- On-device TalkBack check (deferred, fold into Phase 15 POL-04): an own read bubble announces "{time}, Read" as one phrase, with no "dot" fragment and still one focus stop.

## Known Stubs
None.

## Self-Check: PASSED
Commits a98b1da, 2873604, 8b7bf31 are in history; modified files exist.
