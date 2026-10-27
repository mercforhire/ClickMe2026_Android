---
phase: 12-chat-report-block
verified: 2026-10-27T00:00:00Z
status: gaps_found
score: 4/6 must-haves verified
covered_files:
  - ".planning/phases/12-chat-report-block/12-01-PLAN.md"
  - ".planning/phases/12-chat-report-block/12-01-SUMMARY.md"
  - ".planning/phases/12-chat-report-block/12-02-PLAN.md"
  - ".planning/phases/12-chat-report-block/12-02-SUMMARY.md"
  - ".planning/phases/12-chat-report-block/12-03-PLAN.md"
  - ".planning/phases/12-chat-report-block/12-03-SUMMARY.md"
  - ".planning/phases/12-chat-report-block/12-04-PLAN.md"
  - ".planning/phases/12-chat-report-block/12-04-SUMMARY.md"
  - ".planning/phases/12-chat-report-block/12-05-PLAN.md"
  - ".planning/phases/12-chat-report-block/12-05-SUMMARY.md"
  - ".planning/phases/12-chat-report-block/12-06-PLAN.md"
  - ".planning/phases/12-chat-report-block/12-06-SUMMARY.md"
  - ".planning/phases/12-chat-report-block/12-07-PLAN.md"
  - ".planning/phases/12-chat-report-block/12-07-SUMMARY.md"
  - ".planning/phases/12-chat-report-block/12-08-PLAN.md"
  - ".planning/phases/12-chat-report-block/12-08-SUMMARY.md"
  - ".planning/phases/12-chat-report-block/12-09-PLAN.md"
  - ".planning/phases/12-chat-report-block/12-09-SUMMARY.md"
  - ".planning/phases/12-chat-report-block/12-10-PLAN.md"
  - ".planning/phases/12-chat-report-block/12-10-SUMMARY.md"
  - ".planning/phases/12-chat-report-block/12-11-PLAN.md"
  - ".planning/phases/12-chat-report-block/12-11-SUMMARY.md"
  - ".planning/phases/12-chat-report-block/12-12-PLAN.md"
  - ".planning/phases/12-chat-report-block/12-12-SUMMARY.md"
  - ".planning/phases/12-chat-report-block/12-13-PLAN.md"
  - ".planning/phases/12-chat-report-block/12-13-SUMMARY.md"
  - ".planning/phases/12-chat-report-block/12-14-PLAN.md"
  - ".planning/phases/12-chat-report-block/12-14-SUMMARY.md"
  - ".planning/phases/12-chat-report-block/12-15-PLAN.md"
  - ".planning/phases/12-chat-report-block/12-15-SUMMARY.md"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/model/enums/UserReportReason.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/model/requests/ChatRequests.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/navigation/Navigator.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/BlockState.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/ChatLauncher.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/ChatSessionState.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/ChatsRepository.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/ChatThreadScreen.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/ChatThreadViewModel.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/ThreadItem.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/ThreadReducer.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/conversations/ConversationsViewModel.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/reportandblock/ReportAndBlockViewModel.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/expertprofile/ExpertProfileScreen.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/expertprofile/ProfileSafetyViewModel.kt"
covered_digest: "v3:sha256:29bb5c458ed532b864096a2f572a3945354ec4c77a2d68079bac758c38f159e8"
behavior_unverified: 0
overrides_applied: 0
gaps:
  - truth: "Expert Profile report/block success shows the confirmation snackbars \"Thanks, we'll review this.\" and \"You blocked {name}\" (12-10 must_haves; SAFE-01, SAFE-02; roadmap SC-3 profile path)"
    status: failed
    reason: "ExpertProfileScreen.kt:113-124 keys LaunchedEffect on safetyState.snackbar, calls safety.consumeSnackbar() (which sets snackbar = null and so changes the key) and then suspends in snackbarHostState.showSnackbar(message) in the same coroutine. The next recomposition cancels the effect; Material 3 showSnackbar clears currentSnackbarData in a finally, so the snackbar is dismissed within about a frame. The Thread and Conversations screens in this same phase avoid the bug with scope.launch. ProfileSafetyViewModelTest only asserts the state flag, and no screen test asserts the snackbar is visible, so nothing caught it. Matches review CR-01 (confirmed independently from the code). The report and block requests themselves do go out correctly, but the profile block (user stays on the profile) gives no visible confirmation at all."
    artifacts:
      - path: "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/expertprofile/ExpertProfileScreen.kt"
        issue: "LaunchedEffect(safetyState.snackbar) consumes its own key before showSnackbar returns"
    missing:
      - "Show the snackbar via rememberCoroutineScope().launch { showSnackbar(...) } (or consume after showSnackbar returns), as ChatThreadScreen does"
      - "A ExpertProfileScreen Robolectric test asserting the \"Thanks, we'll review this.\" and \"You blocked {name}\" text is displayed after the state is set"
  - truth: "Unblocking a thread this session blocked clears the blocked composer state, including a thread absent from GET /chats (SAFE-03 note, CHAT-UNBLOCK section 6: unblock 200 sets is_blocked=false locally)"
    status: partial
    reason: "ReportAndBlockViewModel.unblock() clears the session mark and posts ThreadNotice.Unblocked, but ChatThreadViewModel.takeNoticeIfForThisThread() only stores the notice; block is only changed by refreshBlockState(). For a thread with no messages (absent from GET /chats, which is exactly the case the session-local blocked-by-me set exists for), findThread returns null and BlockState.applyScan(null, sessionBlocked = false) takes the `else -> this` branch, so the stale BlockedByMe state survives. The \"Unblocked\" snackbar shows but the composer banner stays locked and the menu still says Unblock or report until the user leaves and reopens the thread. A failed rescan has the same effect. Matches review WR-01 (confirmed from BlockState.kt:57-58 and ChatThreadViewModel.kt:441-473). Thread with messages is not affected (the scan finds it)."
    artifacts:
      - path: "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/ChatThreadViewModel.kt"
        issue: "Unblocked notice does not apply BlockState.onUnblockSucceeded() to the live thread state"
      - path: "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/BlockState.kt"
        issue: "applyScan(null, false) leaves a stale block in place"
    missing:
      - "When the thread VM takes a ThreadNotice.Unblocked for its thread, set block = block.onUnblockSucceeded() immediately"
      - "A ChatThreadViewModel test: empty-thread (findThread null) blocked-by-me, then Unblocked notice -> composerMode Normal"
deferred: []
behavior_unverified_items: []
human_verification:
  - test: "SC-1 on the Samsung SM-A075F (devDebug, production backend), iOS app as the other side: Conversations in both modes; Message from Expert Profile, client Upcoming Details, client Booking Summary, a My Bookings card, expert Request Decision (pending and expired), expert Booking Summary, an expert Upcoming card and the in-call Chat control"
    expected: "Every entry lands on the thread in the Chats tab and Back goes to the list; the in-call control keeps the call running in the pill"
    why_human: "Device plus live backend; navigation hand-off is unit-tested with fakes only (STATE [Phase 12] DEFERRED (a))"
  - test: "SC-2 with the iOS client: live messages both ways, Android send appears at once and reconciles without a duplicate, typing both ways, Read on Android bubbles, Load earlier pulls 30 more, booking events inline and live after iOS accepts or declines, airplane-mode failed bubble retries without a duplicate, background then foreground catches up"
    expected: "All behaviors hold across a real two-client socket"
    why_human: "Two-client realtime cannot be exercised by JVM tests (STATE DEFERRED (b))"
  - test: "SC-3 report from chat and from the profile (backend row stores the enum value), block from chat (returns to the list with \"Blocked {name}\") and from the profile (stays, \"You blocked {name}\"), the 10/hour report limit message"
    expected: "Confirmation snackbars are visible. Re-run the profile cases after the CR-01 fix: today the profile snackbars are expected to flash and vanish"
    why_human: "Live backend and visual behavior (STATE DEFERRED (c))"
  - test: "SC-4 the blocker sees \"You blocked this conversation\" and Unblock in Report & Block, the blocked side sees \"You can't reply to this conversation\" and no Unblock, an iOS send into the blocked thread fails, RECIPIENT_UNAVAILABLE with a suspended account, the session-local block after a profile block on a thread with no messages (then unblock it: see gap 2)"
    expected: "Matches CHAT-UNBLOCK section 5"
    why_human: "Two accounts and backend state (STATE DEFERRED (d))"
  - test: "Visual and system: 200% font scale on Conversations, a failed bubble, the blocked banner, Report & Block, the Report sheet and the Request Decision footer; TalkBack on bubble actions and the reason group; jump-to-latest; list move-to-top animation; composer IME behavior; signed prodRelease R8 smoke (needs the deferred Phase 2 keystore)"
    expected: "Nothing clips; accessibility labels read correctly"
    why_human: "Visual and device-only (STATE DEFERRED (e))"
---

# Phase 12: Chat, Report & Block Verification Report

**Phase Goal:** Clients and experts can message each other in real time, and either side can report, block or unblock safely.
**Verified:** 2026-10-27
**Status:** gaps_found
**Re-verification:** No, initial verification

Device-only and two-client checks are deferred under project policy (STATE "[Phase 12] DEFERRED (human)") and are listed as human verification items, not gaps. The two gaps below are defects found in the code itself and are independent of device testing.

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | SC-1: A user can open their conversations list and start a chat from Expert Profile, a booking or a request | VERIFIED (device pass deferred) | `ConversationsRoute` is mounted as the Chats root in both `HomeClientScreen.kt:111` and `HomeExpertScreen.kt:105`; `chatEntries()` registers `ChatThreadKey` and `ChatReportBlockKey` in both shells. `ChatLauncher.open` calls `ChatsRepository.initiate` (body `{peer_id}` only) and posts a `ChatThreadKey` to `ChatLaunchRequest.thread`; both shells consume it via `Navigator.openChatThread`. `MessageButton` is used on Expert Profile, client Upcoming Details, client Booking Summary, My Bookings card, Request Decision (screen and footer), expert Booking Summary, expert Upcoming card; the in-call Chat control is wired in `CallOverlayHost` (`PHASE-12` marker gone). Self-chat hidden via `ChatEntryViewModel.isSelf`. |
| 2 | SC-2: Messages in real time; optimistic send reconciles without duplicates; typing and read live; 30-per-page earlier loading; booking events inline | VERIFIED (code and unit tests; live Android/iOS exchange deferred) | `ThreadReducer` merges by server id then `client_msg_id`, ack collapses a reload-added row into the bubble, Read never regresses. `ChatThreadViewModel`: optimistic `send()` with `ClientMsgIdGenerator`, `deliver()` with ack/fail/retry (same id), `MessageNew`/`MessageRead`/`TypingStart`/`TypingStop`/`BookingUpdate` collectors, `emitMessageRead` only when screen shown and socket Connected, reconnect catch-up reload, `loadEarlier` page size 30 (`ChatsRepository.PAGE_SIZE`), `toThreadItem` maps `type=event` rows to `SystemEvent`. Conversations list moves threads to top on `message.new` and reloads on reconnect. Scoped run `:app:testDevDebugUnitTest --tests '*screens.chat.*' --tests '*ProfileSafetyViewModelTest'`: 267 tests, 0 failures, 0 errors (including `ThreadReducerTest`, `ChatThreadViewModelSendTest`, `ChatThreadViewModelRealtimeTest`). Caveats: WR-03 and IN-02 (see Warnings). |
| 3 | SC-3: A user can report (enum value accepted) and block, from chat and from Expert Profile | VERIFIED for the requests; confirmation feedback is truth 5 | `UserReportReason` has exactly the six backend wire values (`harassment, spam, inappropriate_content, scam_or_fraud, impersonation, other`), matching `USER-REPORTING.md`; `ChatsRepository.reportInChat` and `reportUser` send `reason.wire`, never a label; DTOs carry only schema keys, null-omitting, redacted `toString`. Chat path: `ReportAndBlockViewModel.submitReport/confirmBlock` (single-flight, inputs kept on failure). Profile path: `ProfileSafetyViewModel` (initiate-then-block, session mark). Block from chat returns to the Conversations root with a "Blocked {name}" notice; block from the profile stays. |
| 4 | SC-4: Blocked thread disables composer and shows banner; only the blocker sees Unblock; blocked side sees "You can't reply to this conversation"; THREAD_BLOCKED and RECIPIENT_UNAVAILABLE give clear messages | VERIFIED for the main path (device pass deferred); exit edge is truth 6 | `BlockState` implements CHAT-UNBLOCK sections 5-7: `showsUnblock = isBlocked && blockedByMe && !unblockDenied`; legacy block (`is_blocked` without `blocked_by_me`) is treated as peer-blocked; `NOT_BLOCKER` latches Unblock off and keeps blocked-by-peer. `composerMode()` gives BlockedByMe / BlockedByPeer / Unavailable banners (`chat_banner_blocked_by_peer` = "You can't reply to this conversation"). `handleSendFailure` maps `THREAD_BLOCKED` (state to blocked, rescan) and `RECIPIENT_UNAVAILABLE` (unavailable banner) and restores the refused text to the composer. `SafetyErrors.kt` maps `NOT_BLOCKER`, 404, 429 and network to fixed copy. `BlockStateTest` (14) and the VM tests pass. |
| 5 | 12-10 plan truth: Expert Profile report success shows "Thanks, we'll review this." and block success shows "You blocked {name}" (SAFE-01, SAFE-02) | FAILED | See gap 1: `ExpertProfileScreen.kt:113-124` consumes the snackbar key before `showSnackbar` returns, so the effect is cancelled and the snackbar dismissed almost at once. Review CR-01 (critical) confirmed. |
| 6 | SAFE-03 / CHAT-UNBLOCK section 6: after a successful unblock the composer unlocks, including for a session-blocked empty thread | PARTIAL (FAILED for the empty-thread case) | See gap 2: review WR-01 confirmed in `BlockState.applyScan` and `ChatThreadViewModel`. Threads that have messages recover through the rescan. |

**Score:** 4/6 truths verified (0 present-but-behavior-unverified). Truths 1-4 are the roadmap Success Criteria; 5 and 6 are plan/requirement-level truths that the code contradicts.

### Requirements Coverage

All nine IDs are claimed by plan frontmatter (12-01..12-15) and appear in REQUIREMENTS.md with a "Phase 12 / Pending" traceability row. No orphaned requirements.

| Requirement | Source Plans | Status | Evidence |
|-------------|--------------|--------|----------|
| CHAT-01 conversations list | 12-01, 12-03, 12-05, 12-15 | SATISFIED | `ConversationsViewModel` (GET /chats page 1, limit 50, realtime move-to-top, session-local unread per the documented backend gap) mounted as Chats root in both shells |
| CHAT-02 30-per-page, newest first, inline booking events, text only | 12-01, 12-02, 12-06, 12-09, 12-15 | SATISFIED (live pass deferred) | `PAGE_SIZE = 30`, `loadEarlier`, `mergePage`, `SystemEvent` mapping; no attachments key in `SendMessageRequest`; no attachment code (sweep clean) |
| CHAT-03 optimistic send reconciled by `client_msg_id` | 12-01, 12-02, 12-08, 12-14, 12-15 | SATISFIED with warning | Reducer and VM verified and tested; WR-03 can silently lose a send when the user leaves the screen mid-flight |
| CHAT-04 read status and typing from realtime | 12-02, 12-05, 12-06, 12-09, 12-15 | SATISFIED (live pass deferred) | `MessageRead`/`Typing*` collectors, `emitMessageRead`, `emitTypingStart/Stop`, Read label on bubbles |
| CHAT-05 `initiateChat` from profile, booking and request | 12-01, 12-03, 12-04, 12-11, 12-12, 12-13, 12-15 | SATISFIED with warning | `ChatLauncher` plus ten `MessageButton` sites and the in-call control; WR-06 recreates an already-open thread from the in-call control |
| SAFE-01 report from chat and profile with enum value | 12-01, 12-07, 12-10, 12-15 | SATISFIED for the request; confirmation defective | Enum wire verified; profile "Thanks" snackbar defeated (gap 1) |
| SAFE-02 block from chat and profile | 12-01, 12-07, 12-10, 12-15 | SATISFIED for the request; confirmation defective | Block calls verified; profile "You blocked {name}" defeated (gap 1); WR-02 can record a false "blocked by me" |
| SAFE-03 blocked thread composer, banner, Unblock only for the blocker | 12-02, 12-06, 12-07, 12-08, 12-09, 12-14, 12-15 | PARTIAL | Main matrix verified; the empty-thread unblock exit leaves the composer locked (gap 2) |
| SAFE-04 THREAD_BLOCKED, RECIPIENT_UNAVAILABLE, NOT_BLOCKER clear messages | 12-01, 12-02, 12-07, 12-08, 12-10, 12-14, 12-15 | SATISFIED with warning | `SafetyErrors.kt`, banners; WR-05 double dialog for a suspended account on the send path |

### Required Artifacts and Key Links

| Artifact / Link | Status | Details |
|-----------------|--------|---------|
| `ClickMeApi` chat endpoints (initiate, list, messages, send, actions, user report) and `ApiRequestUrls` | VERIFIED | 6 endpoints present, strict-schema DTOs, repository wired in `ChatModule`/Hilt |
| `ChatsRepository`, `ThreadReducer`, `BlockState`, `ChatSessionState` | VERIFIED | Substantive and wired; `ChatSessionState` bound as a `SessionDataCleaner` (cleared on logout) |
| `ChatThreadViewModel`/`ChatThreadScreen`/`ChatComposer`/`MessageBubble`/`SystemEventRow` | VERIFIED | Substantive, `items(key = row.key)`, composer disabled by `canSend`/mode |
| `ConversationsViewModel`/`ConversationsScreen` | VERIFIED | Mounted in both shells |
| `ReportAndBlockViewModel`/`ReportAndBlockScreen`/`ChatReportBlockKey` | VERIFIED | Registered in `chatEntries`; overflow menu leads to it |
| `ProfileSafetyViewModel` to `ExpertProfileScreen` snackbar | NOT_WIRED correctly | State is set, display path defeated (gap 1) |
| `ThreadNotice.Unblocked` to `ChatThreadViewModel.block` | PARTIAL | Notice reaches the VM; block flags are not updated from it (gap 2) |
| Shell hand-off (`ChatLaunchRequest.thread` to `Navigator.openChatThread`) | WIRED | Both shells consume it |

### Data-Flow Trace (Level 4)

Conversations rows come from `repository.threads()` (GET /chats); thread rows from `repository.messages()` and the socket; block flags from `findThread`. No static or hardcoded-empty sources found. No `Log.`, `println`, persistence, attachment, clear-history or `PHASE-12`/TODO/FIXME markers in `screens/chat`, `components/chat`, `components/safety` or `ProfileSafetyViewModel.kt` (the 12-15 prohibitions hold).

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| Chat, block, reducer, report and profile-safety logic | `./gradlew :app:testDevDebugUnitTest --tests '*screens.chat.*' --tests '*ProfileSafetyViewModelTest'` | 267 tests, 0 failures, 0 skipped | PASS |
| Profile snackbar actually visible | none exists | No test covers it; defect found by reading the code | gap 1 |

Probe execution: no probes declared by this phase (Step 7c not applicable).

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| `ExpertProfileScreen.kt` | 113-124 | Effect key consumed inside its own `showSnackbar` coroutine | Blocker (gap 1) | Required confirmations invisible |
| `ChatThreadViewModel.kt` / `BlockState.kt` | 441-473 / 57-58 | Stale block survives unblock for empty threads | Blocker (gap 2) | Composer stays locked after Unblock |

### Warnings (not gaps; review findings bearing on must-haves)

- WR-03 (CHAT-03): `deliver()` runs in `viewModelScope`; Send then Back before the response cancels the POST and the bubble vanishes with no signal. Violates the spirit of "sent messages appear immediately and reconcile" on a data-loss path. Recommend an application-scoped send or restoring the draft.
- WR-02 (SAFE-02/SAFE-03): Report & Block does not seed from the thread's known block state and has no loading state. For a peer-blocked thread, Block stays actionable until the scan returns (indefinitely if it fails); confirming a no-op block 200 records a false session "blocked by me" that overrides server truth for empty threads.
- WR-05 (SAFE-04): the send path shows its own dialog in addition to the global ACCOUNT_SUSPENDED dialog; 408/429 are treated as refused (bubble removed, text restored) rather than a retryable Failed bubble. Not data-losing.
- WR-06 (CHAT-05): `Navigator.openChatThread` compares full `ChatThreadKey` (including display seeds), so the in-call Chat control with `isExpert = null` recreates an already-open thread, dropping the draft and cancelling an in-flight send (compounds WR-03).
- IN-02 raised to Warning (not Info): `ThreadItem.Message.key` is `c:<client_msg_id>` regardless of sender, and the backend returns `client_msg_id` on peer messages in `GET /chats/{id}/messages` (verified in `chats.controller.js:292`) with uniqueness only per (thread, sender, client_msg_id). A peer who reuses one of my client_msg_ids produces two LazyColumn rows with the same key, which throws on composition and crashes the thread screen on every open. Fix by keying on `mine` plus id.
- WR-04, IN-01, IN-03, IN-04, IN-05: robustness and polish, no must-have impact (WR-04 is a privacy-hygiene deviation from the phase's redaction invariant; no logging exists today).
- 12-REVIEW-DISPOSITION.md lists all 12 findings as `open`; none has been fixed.

### Gaps Summary

The chat data layer, reducer, realtime wiring, entry points, block state machine and error mapping are real, wired and green (267 scoped tests pass; the 12-15 close-out gate of 3310 tests per flavor was not re-run). Two defects keep the phase from being clean:

1. The Expert Profile report/block confirmation snackbars are defeated by a self-cancelling effect (CR-01). Report and block do execute, but the user sees no confirmation, and on the profile block nothing else changes on screen. This directly contradicts the 12-10 must-have text and weakens SAFE-01/SAFE-02 on a store-required feature.
2. Unblocking a session-blocked thread that has no messages leaves the composer locked (WR-01), the exact case SAFE-03's Phase 12 note introduces.

Both are small, localized fixes with known remedies in 12-REVIEW.md (`/gsd-code-review 12 --fix` or `/gsd-plan-phase 12 --gaps`). After they are fixed, the remaining status would be `human_needed` for the deferred device and two-client checks. The IN-02 duplicate-key crash and WR-03 silent send loss should be fixed in the same pass.

---

_Verified: 2026-10-27_
_Verifier: Claude (gsd-verifier)_
