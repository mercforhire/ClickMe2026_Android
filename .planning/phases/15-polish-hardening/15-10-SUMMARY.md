---
phase: 15-polish-hardening
plan: 10
subsystem: chat
tags: [chat, talkback, composer, ime, saved-state, tracer]
status: complete
requires: [15-02, 15-04]
provides:
  - "Chat composer: Send explains why it is disabled, field keeps focus after a send, field keeps its name once filled, counter live only at the cap"
  - "MessageBubble reads as one node '{speaker}: {text}' + time + Sent/Read/Failed to send; announceStatus polite live region"
  - "Peer-arrival announcer in ChatThreadContent (once, never history)"
  - "Restored composer text capped at COMPOSER_MAX; restoration tests per thread"
  - "Chat list and composer content capped at 600 dp"
affects: [15-11, 15-17]
tech-stack:
  added: []
  patterns:
    - "1 dp live-region node whose contentDescription is set then cleared by a LaunchedEffect delay"
key-files:
  created:
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/ChatTalkBackFlowTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/ChatComposerInsetsTest.kt
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/subviews/ChatComposer.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/subviews/MessageBubble.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/ChatThreadScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/ChatThreadViewModel.kt
    - app/src/main/res/values/strings_chat_thread.xml
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/ChatComposerTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/ChatThreadScreenTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/ChatThreadViewModelTest.kt
key-decisions:
  - "Reused chat_thread_bubble_a11y '%1$s: %2$s' for both the bubble and the arrival announcement instead of adding chat_new_message_a11y (UI-SPEC A11: reuse beats adding)"
  - "The live region on the newest own bubble sits on the merged bubble node (child live regions propagate to the merged parent), so a Sent to Read change re-speaks the whole bubble, not only the status"
  - "Arrival announcement is cleared after 4 s so an identical later message is announced again"
requirements-completed: [POL-02, POL-03, POL-04]
plan_head_before: e808da6b9c3dd62717fc017104c562ebca5b1350
plan_head_after: 41ef86f898c9753829c203d64b2e4dbf2a136a45
commits: 2
actuals:
  tokens: 22000
  tasks: 2
  commits: 2
completed: 2026-10-29
---

# Phase 15 Plan 10: Chat thread TalkBack, composer restoration and IME Summary

SC-4 flow 4 (send a chat message) now meets the TalkBack contract in JVM tests: a disabled Send says why, the field keeps focus after a send, each bubble is one "{speaker}: {text}" node with time and status, peer arrivals are announced once, the restored draft is capped, and the composer sits above the IME and inside a 600 dp column.

## What was built

- **ChatComposer**: Send gets `stateDescription` "Type a message to send" when `!canSend`; the Send click calls `onSend()` then `focusRequester.requestFocus()` (keyboard stays open). The field sets `contentDescription = "Message"` once it holds text (the placeholder is the name only when empty, same pattern as 15-04). The character counter is a polite live region only at the cap, where it carries "Message length limit reached" (previously it was a live region from 9000 characters on, which would have spoken the count on every keystroke). The row and the counter use `contentMaxWidth()`; the surface still spans the window.
- **MessageBubble**: merged node description is "{You|name}: {text}" (`chat_thread_bubble_a11y`); failed bubbles append "Failed to send" and keep the Retry sending / Delete message custom actions. The status line reads "{time}, Sent" / "{time}, Read" on my messages and the bare time on theirs. New `announceStatus` parameter puts a polite live region on the bubble.
- **ChatThreadScreen**: passes `announceStatus = true` for the newest own message only; `PeerArrivalAnnouncer` is a 1 dp node with a polite live region that carries "{name}: {text}" for 4 s when a new newest peer message appears after the first loaded page (`rememberSaveable` flags, so recreation does not re-announce history); list items (date headers, entries, Load earlier) use `contentMaxWidth()`.
- **ChatThreadViewModel**: `composerText = capText(savedStateHandle.get<String>(KEY_COMPOSER).orEmpty(), COMPOSER_MAX)`. KDoc cites D-10. No disk persistence added.
- **Strings**: `chat_composer_send_disabled_a11y`, `chat_failed_status_a11y`, `chat_thread_sent_status_a11y`, `chat_thread_bubble_a11y`.

## Tests

All run in `:app:testDevDebugUnitTest --tests '*chat.chatting*'`, green; `ktlintCheck` and `scripts/check-design-rules.sh` clean.

- ChatComposerTest 14 (7 new): send disabled reason, no reason when enabled, focus after send, field named empty and filled, counter not live below the cap, limit announced once.
- ChatTalkBackFlowTest 5 (new): reading order Ada -> "Ada: Hi" -> "You: Hello" -> Message -> Send message, bubble status and failed actions, Sent spoken, only the newest own bubble is polite, actionables named and 48 dp.
- ChatThreadViewModelTest 31 (4 new): restored from a handle copy, per thread, capped at COMPOSER_MAX, failed bubble not restored and text not in the handle.
- ChatThreadScreenTest 45 (5 new): empty thread keeps the composer, arrival announced once then cleared, first message of an empty thread announced, earlier page and own messages not announced, composer survives `StateRestorationTester` recreation with a real ViewModel and is not focused.
- ChatComposerInsetsTest 2 (new): Send and field end at or above 500 dp with a 300 px IME on 360x800 and the list still scrolls; composer width and own bubble stay within the 600 dp column on 900x600.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Character counter was a live region on every keystroke**
- **Found during:** Task 1 (`counterIsNotLiveUntilLimit`)
- **Issue:** `CharacterCounter` set `liveRegion = Polite` unconditionally from 9000 characters, contradicting UI-SPEC ("not a live region; limit announced once").
- **Fix:** live region and the limit description only when `atCap`.
- **Files modified:** `ChatComposer.kt`
- **Commit:** 57341847

**2. [Rule 3 - Blocking] Existing test pinned the old Sent status description**
- **Found during:** Task 1
- **Issue:** `sentStatusLineExposesTimeOnly` asserted a bare time; UI-SPEC now requires "Sent".
- **Fix:** renamed to `sentStatusLineExposesTimeAndSent`, asserts "{time}, Sent".
- **Files modified:** `ChatThreadScreenTest.kt`
- **Commit:** 57341847

**3. Plan-wording adjustments (no behavior change)**
- Custom action label stays "Delete message" (existing string and test); the flow test asserts the label starts with "Delete".
- The single `announceStatus` call-site in `ChatThreadScreen.kt` was made in Task 1 (the plan allowed either task) so the Task 1 tracer test `newestOwnStatusIsPolite` passes at the tracer commit.
- No separate `chat_new_message_a11y` string: `chat_thread_bubble_a11y` is the same "%1$s: %2$s" format.
- TDD RED run: the first RED run for Task 1 was killed by a 40 minute wait on a machine loaded by parallel executors (load average above 40), so RED and GREEN for each task share one commit instead of a separate failing `test(...)` commit. Tests were written before the implementation; every new case passes against the final code.

**Total deviations:** 2 auto-fixed (1 bug, 1 blocking), 1 TDD commit-shape note. **Impact:** none on scope.

## Known Stubs

None.

## Threat Flags

None. T-15-21 mitigated by `capText` on restore and `restoredComposerIsCapped`; T-15-22 and T-15-23 accepted as in the plan (no disk persistence; `grep DataStore|SharedPreferences` over the chat thread package is 0).

## Open items (device checklist for 15-17; nothing blocks on a person)

- TalkBack on the Samsung SM-A075F for flow 4: reading order, "You: {text}, {time}, Sent/Read", failed bubble wording and actions, disabled Send reason, focus and keyboard staying after Send. Semantics tests prove exposure, not speech (RESEARCH Pitfall 9).
- Whether TalkBack speaks both the merged contentDescription and the child Text of a bubble (possible duplicate "You: Hello" then "Hello"); if it does, drop the Text from the merged node.
- Whether a live region on a bubble that first appears (history newest own message) is announced on first composition; the arrival announcer is guarded against this, the `announceStatus` live region is not.
- IME on real hardware with the display cutout and 3-button navigation; "Don't keep activities" restore of a typed draft in a thread (keyboard must not reopen).
- Wave 3 note for 15-11: `ChatThreadContent` now has `PeerArrivalAnnouncer(state, peerLabel)` right after the `PullToRefreshBox`, and `ThreadList` computes `newestOwnKey`; the refresh-failed `LaunchedEffect` was not touched.

## Commits

- 57341847 feat(15-10): TalkBack contract for sending a chat message
- 41ef86f8 feat(15-10): composer restoration, arrival announcements and 600 dp chat column

## Self-Check: PASSED

Both created test files exist, both commits are ancestors of HEAD, acceptance greps and verify commands pass.
