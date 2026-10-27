---
phase: 12-chat-report-block
verified: 2026-10-27T00:00:00Z
status: passed
score: 6/6 must-haves verified
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
  - ".planning/phases/12-chat-report-block/12-16-PLAN.md"
  - ".planning/phases/12-chat-report-block/12-16-SUMMARY.md"
  - ".planning/phases/12-chat-report-block/12-17-PLAN.md"
  - ".planning/phases/12-chat-report-block/12-17-SUMMARY.md"
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
covered_digest: "v3:sha256:875af1b89db02a852c4c554e836b0baa13f19cf15434a3732141e27918713fee"
behavior_unverified: 0
overrides_applied: 0
re_verification:
  previous_status: gaps_found
  previous_score: 4/6
  gaps_closed:
    - "Expert Profile report/block success shows the confirmation snackbars \"Thanks, we'll review this.\" and \"You blocked {name}\" (CR-01) - closed by 12-17 Task 1 (commit 236a45a)"
    - "Unblocking a thread this session blocked clears the blocked composer state, including a thread absent from GET /chats (WR-01) - closed by 12-16 Task 1 (commit 2de260c)"
  gaps_remaining: []
  regressions: []
gaps: []
deferred: []
behavior_unverified_items: []
human_verification:
  - test: "SC-1 on the Samsung SM-A075F (devDebug, production backend), iOS app as the other side: Conversations in both modes; Message from Expert Profile, client Upcoming Details, client Booking Summary, a My Bookings card, expert Request Decision (pending and expired), expert Booking Summary, an expert Upcoming card and the in-call Chat control"
    expected: "Every entry lands on the thread in the Chats tab and Back goes to the list; the in-call control keeps the call running in the pill"
    why_human: "Device plus live backend; navigation hand-off is unit-tested with fakes only (STATE [Phase 12] DEFERRED (a))"
  - test: "SC-2 with the iOS client: live messages both ways, Android send appears at once and reconciles without a duplicate, typing both ways, Read on Android bubbles, Load earlier pulls 30 more, booking events inline and live after iOS accepts or declines, airplane-mode failed bubble retries without a duplicate, background then foreground catches up. Also: Send then Back at once, then reopen (WR-03 fix: the message must not vanish; a quick reopen while the POST is in flight is the open GR-WR-02 case)"
    expected: "All behaviors hold across a real two-client socket"
    why_human: "Two-client realtime cannot be exercised by JVM tests (STATE DEFERRED (b))"
  - test: "SC-3 report from chat and from the profile (backend row stores the enum value), block from chat (returns to the list with \"Blocked {name}\") and from the profile (stays, \"You blocked {name}\"), the 10/hour report limit message"
    expected: "Profile report and block confirmation snackbars are visible for about 4 s (CR-01 fix) and not shown twice"
    why_human: "Live backend and visual behavior (STATE DEFERRED (c))"
  - test: "SC-4 the blocker sees \"You blocked this conversation\" and Unblock in Report & Block, the blocked side sees \"You can't reply to this conversation\" and no Unblock, an iOS send into the blocked thread fails, RECIPIENT_UNAVAILABLE with a suspended account. Also block from the profile on a thread with no messages, open the thread, Report & Block, Unblock, return"
    expected: "Matches CHAT-UNBLOCK section 5; after Unblock the composer is back even for the empty thread (WR-01 fix)"
    why_human: "Two accounts and backend state (STATE DEFERRED (d))"
  - test: "Visual and system: 200% font scale on Conversations, a failed bubble, the blocked banner, Report & Block, the Report sheet and the Request Decision footer; TalkBack on bubble actions and the reason group; jump-to-latest; list move-to-top animation; composer IME behavior; signed prodRelease R8 smoke (needs the deferred Phase 2 keystore)"
    expected: "Nothing clips; accessibility labels read correctly"
    why_human: "Visual and device-only (STATE DEFERRED (e))"
---

# Phase 12: Chat, Report & Block Verification Report

**Phase Goal:** Clients and experts can message each other in real time, and either side can report, block or unblock safely.
**Verified:** 2026-10-27
**Status:** human_needed
**Re-verification:** Yes, after gap closure (plans 12-16 and 12-17). Previous: gaps_found, 4/6.

Both previous gaps are closed in code and tests. The only items left are the device and two-client checks that the user's project policy defers (STATE "[Phase 12] DEFERRED (human)"). They are listed as human verification, not gaps. Open code-review warnings are listed below as warnings; none contradicts a must-have.

## Gap Closure Check

| Previous gap | Resolution | Evidence |
|--------------|-----------|----------|
| 1. Expert Profile report/block snackbars self-cancel (CR-01) | CLOSED | `ExpertProfileScreen.kt:107-128`: `val scope = rememberCoroutineScope()`; inside `LaunchedEffect(safetyState.snackbar)` the message is shown with `scope.launch { snackbarHostState.showSnackbar(message) }` and then `safety.consumeSnackbar()`. Consuming the key now only ends the effect, not the coroutine that hosts the snackbar. Same shape as `ChatThreadScreen`. Three Robolectric tests on the real screen (`reportSuccessShowsThanksSnackbar`, `blockSuccessShowsYouBlockedName`, `blockSuccessWithoutNameShowsFallback`) assert the text is displayed and the state consumed; the SUMMARY records they failed at HEAD before the fix. I re-ran `ExpertProfileScreenTest`: 32 tests, 0 failures. `ProfileSafetyViewModel.kt` is unchanged (diff since 15655fd is empty for it). |
| 2. Unblock leaves an empty session-blocked thread's composer locked (WR-01) | CLOSED | `ChatThreadViewModel.takeNoticeIfForThisThread()` (lines ~484-499) sets `block = it.block.onUnblockSucceeded()` in the same update that stores the `ThreadNotice.Unblocked`, with an exhaustive `when` over the sealed notice, ignores a notice for another thread, and restarts an in-flight scan (`if (blockJob?.isActive == true) refreshBlockState()`). `ReportAndBlockViewModel.unblock()` clears the session mark before posting the notice, so the restarted scan with `applyScan(null, false)` keeps the unblocked state. Tests: `unblockFromReportAndBlockUnlocksAnEmptySessionBlockedThread`, `unblockedNoticeUnlocksWhenTheRescanFails`, `unblockedNoticeForAnotherThreadKeepsTheBlock`, `staleScanInFlightCannotRelockAfterUnblock`. `BlockState.kt` is unchanged since the previous report (plan prohibition holds). I re-ran `ChatThreadViewModelTest`: 27 tests, 0 failures. |
| Also fixed by the gap plans: IN-02 (duplicate LazyColumn key crash) | CLOSED | `ThreadItem.Message.key` is `c:<client_msg_id>` only when `mine && clientMsgId != null`, otherwise `s:<server id>`; `SystemEvent` keys are `s:` plus server id. Tests `peerRowSharingMyClientMsgIdKeepsADistinctKey`, `peerSocketRowKeepsItsKeyWhenAReloadAddsClientMsgId`, `keysAreUniqueInAMixedThread`, `peerAndOwnRowsSharingAClientMsgIdBothRender` (`ThreadReducerTest` 18, `ChatThreadScreenTest` 38, 0 failures). |
| Also fixed: WR-03 (Send then Back loses the message) | CLOSED, with caveats GR-WR-01..03 | `ChatThreadViewModel.deliver()` runs the POST in `sendScope.async` (`@ApplicationScope`) and awaits it in `viewModelScope`; if the await is cancelled and the request fails, `ChatSessionState.keepUnsentText(threadId, ownerId, text)` stores it in memory (owner-keyed, take-once, cleared by `clearSessionData`), and a later `ChatThreadViewModel` restores it via `takeUnsentText`. Live outcomes (Failed bubble on network/5xx, bubble removed and text restored on 4xx) unchanged. Tests in `ChatThreadViewModelSendTest` (29, 0 failures) and `ChatSessionStateTest` (7, 0 failures). |

Wire-contract prohibitions of 12-16 hold: `git diff 15655fd HEAD` shows no change to `BlockState.kt`, `ChatsRepository.kt`, `ChatRequests.kt`, `ClickMeApi.kt` or `ApiRequestUrls.kt`. 12-17 changes only `ExpertProfileScreen.kt` on the main side.

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | SC-1: A user can open their conversations list and start a chat from Expert Profile, a booking or a request | VERIFIED (device pass deferred) | Unchanged since the previous report: `ConversationsRoute` is the Chats root in both shells; `ChatLauncher.open` calls `ChatsRepository.initiate` and posts a `ChatThreadKey`; both shells consume it via `Navigator.openChatThread`; `MessageButton` on all entry points plus the in-call control. The only changed files in this area (`ExpertProfileScreen.kt`) still wire `chatEntry`. |
| 2 | SC-2: Messages in real time; optimistic send reconciles without duplicates; typing and read live; 30-per-page earlier loading; booking events inline | VERIFIED (code and unit tests; live exchange deferred) | `ThreadReducer`, `ChatThreadViewModel` collectors and `PAGE_SIZE = 30` unchanged in substance. The gap pass hardened it: peer rows can no longer collide with own keys (IN-02), and a send now survives leaving the screen (WR-03). Orchestrator full run: 3330 tests, 0 failures; my re-run of the six directly affected classes: 151 tests, 0 failures. Caveats GR-WR-01/02 (below) are edge cases of the post-close send path, not of the live path. |
| 3 | SC-3: A user can report (enum value accepted) and block, from chat and from Expert Profile | VERIFIED | `UserReportReason` wire values and DTOs unchanged and untouched by the gap pass. The profile confirmation feedback that was missing is now present (truth 5). |
| 4 | SC-4: Blocked thread disables composer and shows banner; only the blocker sees Unblock; blocked side sees "You can't reply to this conversation"; THREAD_BLOCKED and RECIPIENT_UNAVAILABLE give clear messages | VERIFIED (device pass deferred) | `BlockState` unchanged (matrix of CHAT-UNBLOCK section 5 intact); the exit edge is now truth 6. |
| 5 | 12-10 plan truth: Expert Profile report success shows "Thanks, we'll review this." and block success shows "You blocked {name}" (SAFE-01, SAFE-02) | VERIFIED | Gap 1 closed above; displayed-text assertions in `ExpertProfileScreenTest`, ViewModel untouched. |
| 6 | SAFE-03 / CHAT-UNBLOCK section 6: after a successful unblock the composer unlocks, including a session-blocked empty thread | VERIFIED | Gap 2 closed above; regression tests cover empty thread, failed rescan, other-thread notice and a stale in-flight scan. |

**Score:** 6/6 truths verified (0 present-but-behavior-unverified).

### Required Artifacts and Key Links

| Artifact / Link | Status | Details |
|-----------------|--------|---------|
| `ChatThreadViewModel.kt` (`onUnblockSucceeded` on notice, `sendScope.async`, `keepUnsentText`/`takeUnsentText` restore) | VERIFIED | Substantive, wired. `sendScope` is an `@ApplicationScope` constructor parameter; orchestrator `assembleDevDebug` proves the Hilt graph. |
| `ThreadItem.kt` sender-aware key | VERIFIED | Used by `ThreadRow.Entry.key` and `items(key = ...)` in `ChatThreadScreen`. |
| `ChatSessionState.kt` unsent-text store | VERIFIED | `keepUnsentText`, `takeUnsentText` (atomic take-once), keys-only `unsentKeys` flow, cleared in `clearSessionData()`. Memory only. |
| `ExpertProfileScreen.kt` snackbar effect | VERIFIED | `scope.launch` before `consumeSnackbar()`; `SnackbarHost(snackbarHostState)` rendered in `ExpertProfileContent`. |
| Link `ReportAndBlockViewModel.unblock` to `ChatThreadViewModel.takeNoticeIfForThisThread` to `BlockState.onUnblockSucceeded` | WIRED | Notice collected in thread VM `init` and in `onScreenShown()`; block applied in the same update. |
| Link `deliver` to `sendScope.async` and to `keepUnsentText` on await cancellation | WIRED | `request.invokeOnCompletion { if (cause != null) keepUnsentText(...) }` inside the `CancellationException` branch. |
| All earlier-phase artifacts (API endpoints, `ChatsRepository`, `ThreadReducer`, `BlockState`, conversations, report and block screens, `ProfileSafetyViewModel`) | VERIFIED, unchanged | `git diff 15655fd HEAD --stat -- app` lists only the 11 gap-plan files (4 main: `ThreadItem`, `ChatSessionState`, `ChatThreadViewModel`, `ExpertProfileScreen`; 7 test files); nothing else moved, so no regression surface outside them. |

### Data-Flow Trace (Level 4)

No new rendered-data source. The profile snackbar message comes from `ProfileSafetyViewModel.state.snackbar`, set by real `submitReport`/`confirmBlock` success paths. Kept unsent text comes from the failed request's own text and returns only to the composer for the same owner and thread.

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| Gap-fix tests on the touched classes | `./gradlew :app:testDevDebugUnitTest --tests '*ExpertProfileScreenTest' '*ChatThreadViewModelTest' '*ThreadReducerTest' '*ChatThreadViewModelSendTest' '*ChatSessionStateTest' '*ChatThreadScreenTest'` | 32 + 27 + 18 + 29 + 7 + 38 tests, 0 failures, 0 errors | PASS |
| Full gate (orchestrator-run, on main after merge) | `./gradlew :app:testDevDebugUnitTest ktlintCheck :app:assembleDevDebug` | BUILD SUCCESSFUL, 3330 tests, 0 failures, 0 errors | PASS (reported) |

Probe execution: no probes declared by this phase (Step 7c not applicable).

### Requirements Coverage

All nine IDs are claimed in plan frontmatter (12-01..12-17) and appear in REQUIREMENTS.md; no orphans. REQUIREMENTS.md traceability rows still say "Pending" (status bookkeeping, not an implementation gap; the orchestrator updates it on phase completion).

| Requirement | Source Plans | Status | Evidence |
|-------------|--------------|--------|----------|
| CHAT-01 conversations list | 12-01, 12-03, 12-05, 12-15 | SATISFIED | Unchanged; mounted in both shells |
| CHAT-02 30 per page, newest first, inline booking events, text only | 12-01, 12-02, 12-06, 12-09, 12-15, 12-16 | SATISFIED (live pass deferred) | Plus IN-02: thread cannot crash on a peer-reused client_msg_id |
| CHAT-03 optimistic send reconciled by `client_msg_id` | 12-01, 12-02, 12-08, 12-14, 12-15, 12-16 | SATISFIED with warnings | WR-03 data-loss path closed; GR-WR-01/02 are residual edge cases of the post-close path |
| CHAT-04 read status and typing from realtime | 12-02, 12-05, 12-06, 12-09, 12-15 | SATISFIED (live pass deferred) | Unchanged |
| CHAT-05 `initiateChat` from profile, booking and request | 12-01, 12-03, 12-04, 12-11, 12-12, 12-13, 12-15 | SATISFIED with warning | WR-06 still open |
| SAFE-01 report from chat and profile with enum value | 12-01, 12-07, 12-10, 12-15, 12-17 | SATISFIED | Confirmation now visible (CR-01 closed) |
| SAFE-02 block from chat and profile | 12-01, 12-07, 12-10, 12-15, 12-17 | SATISFIED with warning | Confirmation now visible; WR-02 still open |
| SAFE-03 blocked composer, banner, Unblock only for the blocker | 12-02, 12-06, 12-07, 12-08, 12-09, 12-14, 12-15, 12-16 | SATISFIED | Empty-thread unblock exit closed (WR-01); phase-12 note on session-blocked absent threads holds |
| SAFE-04 THREAD_BLOCKED, RECIPIENT_UNAVAILABLE, NOT_BLOCKER clear messages | 12-01, 12-02, 12-07, 12-08, 12-10, 12-14, 12-15 | SATISFIED with warning | WR-05 still open |

### Anti-Patterns Found

Scan of the 11 files changed by the gap pass: no `TODO`, `FIXME`, `XXX`, `TBD`, `Log.` or `println` in the changed main files (the plan's prohibition gates also report none), no persistence in `ChatSessionState` (memory-only `MutableStateFlow`). No blockers.

### Warnings (not gaps; open in 12-REVIEW-DISPOSITION.md)

None contradicts a must-have; each is an edge case or hygiene item.

New from the gap-pass re-review, all in the application-scoped send path added for WR-03:
- GR-WR-01 (CHAT-03): a send that fails after the thread closed is restored as plain composer text and loses its `client_msg_id`; for an ambiguous failure (IOException or 5xx where the server may have stored it) a user re-send can duplicate. The live path still keeps the id on a Failed bubble.
- GR-WR-02 (CHAT-03): a thread reopened while the old send is still in flight is not told when it succeeds; the message appears only on the next quiet reload and the user may re-type it.
- GR-WR-03 (SAFE-04 / hygiene): post-close failures such as THREAD_BLOCKED or ACCOUNT_SUSPENDED are parked as unexplained composer text; a failure landing after logout can re-populate the cleared in-memory store (owner-keyed, so another account never sees it).
- GR-IN-01..03: a Failed bubble on screen at Back is still lost; joined kept texts are cut by the composer cap; missing tests for logout mid-send, success while reopened, null owner.

Carried from the first pass (unchanged, not in scope of the gap pass): WR-02 (Report & Block actionable before the scan finishes; can record a false session "blocked by me"), WR-04 (default `toString` on UI state with user text; no logging exists today), WR-05 (send path double dialog for ACCOUNT_SUSPENDED; 408/429 treated as refused), WR-06 (`openChatThread` compares display seeds and recreates an open thread), IN-01, IN-03, IN-04, IN-05. All are carried in STATE as a pending todo.

Recommended before the Phase 12 device pass or in a small follow-up: GR-WR-01 and GR-WR-02 (duplicate or invisible message) and WR-02.

### Human Verification Required

See the `human_verification` list in the frontmatter. Five device and two-client checks, deferred by the user's project policy; SC-2, SC-3 and SC-4 items now include the re-checks for the gap fixes (Send then Back, profile confirmation snackbars, empty-thread Unblock).

### Gaps Summary

No gaps. Both previous gaps (profile confirmation snackbars; empty-thread unblock composer lock) are closed in code, covered by tests that exercise the real screen and ViewModel, and passed in the orchestrator's full run (3330 tests, 0 failures) and in my re-run of the directly affected test classes. The gap plans also closed IN-02 and WR-03 and respected their prohibitions (no wire-contract or `BlockState` change). Status is `human_needed` solely because the device and live two-client checks are outstanding by project policy.

---

_Verified: 2026-10-27_
_Verifier: Claude (gsd-verifier)_
