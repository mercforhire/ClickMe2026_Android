---
phase: 12-chat-report-block
plan: 04
subsystem: chat
tags: [chat-entry, launcher, message-button, expert-profile, tdd]
status: complete

requires:
  - phase: 12-chat-report-block
    provides: ChatsRepository.initiate (12-01), CurrentUserIdProvider (12-02), ChatThreadKey and ChatLaunchRequest.thread (12-03)
provides:
  - ChatLauncher (single "open chat thread" path, one in-flight initiate per peer) with ChatPeer, ChatOpenResult, toChatEntryError
  - ChatEntryViewModel (entry-scoped busy and error state, isSelf guard)
  - MessageButton (text and icon forms), MessageButtonVariant, ChatEntryErrorDialog, MESSAGE_BUTTON_SPINNER_TAG
  - strings_chat_entry.xml with every Message label, state description and entry error string
  - Expert Profile Message button below Book Session, hidden on the own profile
  - testChatEntryViewModel test support
affects: [12-11, 12-12, 12-13]

tech-stack:
  added: []
  patterns:
    - "Entry-scoped @HiltViewModel injected via hiltViewModel() default parameter; tests pass a Hilt-free instance"
    - "Single flight per peer via a synchronized lower-cased id set released in finally"

key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/ChatLauncher.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/ChatEntryViewModel.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/components/chat/MessageButton.kt
    - app/src/main/res/values/strings_chat_entry.xml
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/chat/ChatLauncherTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/chat/ChatEntryViewModelTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/components/chat/MessageButtonTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/testing/ChatEntryTestSupport.kt
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/expertprofile/ExpertProfileScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/expertprofile/subviews/ExpertProfileActionButtons.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/expertprofile/ExpertProfileScreenTest.kt

key-decisions:
  - "Single flight uses a synchronized set of lower-cased peer ids (non-suspending claim) instead of a Mutex; same semantics, simpler, released in finally so cancellation frees the peer"
  - "MessageButton busy keeps the button enabled and guards the click lambda, so accessibility still announces 'Opening chat' via stateDescription"
  - "Icon form puts contentDescription (label when null) on the button semantics, not the glyph, so it survives the busy spinner swap"

patterns-established:
  - "ChatEntryErrorDialog(error, onDismiss) is the one failure surface for every Message control"
  - "Existing ExpertProfileActionButtons/Content callers keep compiling: the new Message parameters default to hidden"

requirements-completed: [CHAT-05]

duration: ~40min
completed: 2026-10-27
commits: 3
plan_head_before: e0a298fc91d76d676bd22914ec24bae368e65356
plan_head_after: a649b08a017468d7624bec1f3b43862db9fe7839
actuals:
  tokens: 28000
  tasks: 3
  commits: 3
---

# Phase 12 Plan 04: Chat entry path and Expert Profile Message Summary

**Message on an Expert Profile calls chats/initiate once per peer and hands a ChatThreadKey to the Chats tab, through a reusable ChatLauncher, entry ViewModel and MessageButton (text and icon forms) that later entry points reuse unchanged.**

## Accomplishments

- `ChatLauncher.open(peer)`: initiate, build `ChatThreadKey(threadId, peerId, name, avatar, isExpert)`, post to `ChatLaunchRequest.thread`. Never checks block state (D-12). One in-flight initiate per peer (case-insensitive) returns `AlreadyOpening`; `ApiException` becomes `Failed(FieldError)`; cancellation propagates, posts nothing and releases the peer.
- `toChatEntryError`: 404 -> "This user is unavailable.", other HTTP -> server message else generic, network -> shared connection copy, decoding -> generic. No exception text reaches the dialog (T-12-12).
- `ChatEntryViewModel`: `busyPeerId`/`error` state, `isSelf` (case-insensitive), `open` ignores taps while busy and for the current user, runs in `viewModelScope` so leaving the screen cancels it, clears busy in `finally`.
- `MessageButton`: outlined 56 dp text form and `OutlinedIconButton` icon form (`iconTarget` 56/48 dp), spinner plus "Opening chat" state description and ignored taps while busy; `ChatEntryErrorDialog` wraps `AuthErrorDialog` with the "Couldn't open chat" title.
- Expert Profile: Message below Book Session (`expert_profile_message`), enabled even when Book Session is disabled, hidden on the own profile; the action skeleton shows a second 56 dp bar.
- `strings_chat_entry.xml` holds all entry copy (Message, Message Expert/Client/%1$s, Opening chat, error title, unavailable) so 12-11/12/13 add no strings.

## Task Commits

1. Task 1 (tracer): `eaaeed3` feat(12-04): Message on an Expert Profile initiates the chat and hands the thread to the Chats tab
2. Task 2: `6adb81f` feat(12-04): single flight per peer, busy state, entry error mapping and dialog
3. Task 3: `a649b08` feat(12-04): MessageButton icon form, busy semantics and the profile skeleton bar

## Verification

- `ChatLauncherTest` 4, `ChatEntryViewModelTest` 3, `MessageButtonTest` 6, `ExpertProfileScreenTest` 26: all green. `ktlintCheck` passes. `bash scripts/check-design-rules.sh` prints "Design rules: OK".
- Tracer feedback gate: Task 1 verify (ChatLauncherTest plus ExpertProfileScreenTest) passed end to end before Tasks 2 and 3 expanded; no `blocking-human` gate on the task.
- Acceptance greps: `thread.post` in ChatLauncher, `isExpert = true` in ExpertProfileScreen, `expert_profile_message` in ExpertProfileActionButtons, `AlreadyOpening` x2+, `chat_entry_error_unavailable`, `OutlinedIconButton`, `chat_entry_opening` all present.

## TDD Gate Compliance

Tests and implementation are committed together per task. The test files reference symbols that do not exist before the implementation (ChatLauncher, ChatPeer, EXPERT_PROFILE_MESSAGE_TAG, MessageButton), so a separate RED run is a compile failure, not an assertion failure (INVALID_RED). The plan is `type: execute`, so the plan-level RED/GREEN gate sequence does not apply. All tests were run and pass.

## Deviations from Plan

**1. [Process] No separate RED commits** (see TDD Gate Compliance).

**2. [Design] Mutex replaced by a synchronized set**
- The plan said "guard with a `Mutex`". The claim is a non-suspending add to a set, so `synchronized` gives the same guarantee with less machinery. Behavior and tests are as specified.

**3. [Design] MessageButton fully written in Task 1 text form, then rewritten in Task 3**
- Task 1 shipped the text form only (the Expert Profile needed it); Task 3 added the variant, icon form and shared glyph/spinner helpers as planned.

Otherwise the plan executed as written.

## Known Stubs

None.

## Threat Flags

None. T-12-11 mitigated (single flight in the launcher plus the busy guard; tests assert one request). T-12-12 mitigated (dialog shows `userMessage` or a string resource only). T-12-13 accepted (control hidden for self; server rejects with 422).

## Deferred / Open Items

- On-device check of the Message tap landing on the Chats tab thread needs the real thread screen (12-09); manual testing is deferred per project policy.

## Self-Check: PASSED

- Created files found: ChatLauncher.kt, ChatEntryViewModel.kt, MessageButton.kt, strings_chat_entry.xml, ChatLauncherTest.kt, ChatEntryViewModelTest.kt, MessageButtonTest.kt, ChatEntryTestSupport.kt.
- Commits eaaeed3, 6adb81f, a649b08 are ancestors of HEAD.
