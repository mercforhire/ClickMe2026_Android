---
phase: "12"
slug: "chat-report-block"
# status lifecycle: draft (seeded by plan-phase) → validated (set by validate-phase §6)
# audit-milestone §5.5 distinguishes NOT-VALIDATED (draft) from PARTIAL (validated + nyquist_compliant: false) (#2117)
status: complete
nyquist_compliant: true
wave_0_complete: true
created: "2026-10-27"
---

# Phase 12 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit4 + Robolectric + Compose ui-test-junit4 + kotlinx-coroutines-test + mockwebserver3 |
| **Config file** | app/build.gradle.kts (testOptions.unitTests.isIncludeAndroidResources = true) |
| **Quick run command** | `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:testDevDebugUnitTest --tests '*<TestClass>' ktlintCheck` |
| **Full suite command** | `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:testDevDebugUnitTest ktlintCheck lintDevDebug && bash scripts/check-design-rules.sh` |
| **Estimated runtime** | ~12 seconds |

---

## Sampling Rate

- **After every task commit:** Run the targeted quick run command for the files touched
- **After every plan wave:** Run the full suite command
- **Before `/gsd-verify-work`:** Full suite must be green
- **Max feedback latency:** 12 seconds

---

## Per-Task Verification Map

Targeted rows use the quick run command with the listed `--tests` filters plus `ktlintCheck` (for example `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:testDevDebugUnitTest --tests '*ChatsApiTest' ktlintCheck`). Each plan's `<verify>` block holds the exact command.

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 12-01-01 | 01 | 1 | CHAT-01, CHAT-02, CHAT-03, CHAT-05 | T-12-02 | Request bodies carry only schema keys, no JSON null | wire (MockWebServer) | `--tests` *ChatsApiTest | ✅ W0 | ✅ green |
| 12-01-02 | 01 | 1 | SAFE-01, SAFE-02, SAFE-04 | T-12-01 | toString never prints message text or report details; enum reasons only | wire + unit | `--tests` *ChatsApiTest *UserReportReasonTest *ApiErrorParserTest | ✅ W0 (ApiErrorParserTest exists) | ✅ green |
| 12-01-03 | 01 | 1 | CHAT-01..05, SAFE-01..04 | — | N/A (test seams) | unit | `--tests` *ChatsRepositoryTest *ChatsApiTest *RealtimeConnectionManagerTest | ✅ W0 | ✅ green |
| 12-02-01 | 02 | 1 | CHAT-03 | — | N/A | unit (pure) | `--tests` *ThreadReducerTest | ✅ W0 | ✅ green |
| 12-02-02 | 02 | 1 | CHAT-03, CHAT-04, SAFE-03 | T-12-06 | NOT_BLOCKER latches Unblock off; scan never re-enables it | unit (pure) | `--tests` *ThreadReducerTest *BlockStateTest | ✅ W0 | ✅ green |
| 12-02-03 | 02 | 1 | SAFE-03 | T-12-05 | Session chat state cleared on logout/login | unit + Hilt build | `--tests` *ChatSessionStateTest *ThreadReducerTest *BlockStateTest + :app:assembleDevDebug | ✅ W0 | ✅ green |
| 12-03-01 | 03 | 1 | CHAT-05 | T-12-09 | Hand-off consumed exactly once | Robolectric | `--tests` *HomeShellsTest *NavigatorTest | ✅ (extended) | ✅ green |
| 12-03-02 | 03 | 1 | CHAT-01, CHAT-05 | — | N/A | unit + Robolectric | `--tests` *NavigatorOpenChatThreadTest *ChatLaunchRequestTest *HomeShellsTest | ✅ W0 | ✅ green |
| 12-03-03 | 03 | 1 | CHAT-05 | T-12-08 | Chats-origin keys never equal start-stack keys | unit + build | `--tests` *ChatNavigationTest *NavigatorOpenChatThreadTest *HomeShellsTest *ExpertNavigationTest *BookingFlowNavigationTest + :app:assembleDevDebug | ✅ W0 | ✅ green |
| 12-04-01 | 04 | 2 | CHAT-05 | — | N/A | unit + Robolectric | `--tests` *ChatLauncherTest *ExpertProfileScreenTest | ✅ W0 | ✅ green |
| 12-04-02 | 04 | 2 | CHAT-05 | T-12-11, T-12-12 | One initiate in flight per peer; dialogs show userMessage or resources only | unit | `--tests` *ChatLauncherTest *ChatEntryViewModelTest *ExpertProfileScreenTest | ✅ W0 | ✅ green |
| 12-04-03 | 04 | 2 | CHAT-05 | T-12-13 | Message hidden on own profile | Robolectric | `--tests` *MessageButtonTest *ExpertProfileScreenTest *ChatEntryViewModelTest | ✅ W0 | ✅ green |
| 12-05-01 | 05 | 2 | CHAT-01 | — | N/A | Robolectric | `--tests` *ConversationsScreenTest | ✅ W0 | ✅ green |
| 12-05-02 | 05 | 2 | CHAT-01, CHAT-04 | T-12-14, T-12-15 | No logging of previews; latest-wins reloads | unit | `--tests` *ConversationsViewModelTest *ConversationsScreenTest | ✅ W0 | ✅ green |
| 12-05-03 | 05 | 2 | CHAT-01 | T-12-16 | Plain-text previews | Robolectric | `--tests` *ConversationsScreenTest *ConversationTimeLabelTest *ConversationsViewModelTest | ✅ W0 | ✅ green |
| 12-06-01 | 06 | 2 | CHAT-02 | — | N/A | unit | `--tests` *ChatMessageMappingTest *ChatThreadViewModelTest | ✅ W0 | ✅ green |
| 12-06-02 | 06 | 2 | CHAT-02, CHAT-04, SAFE-03 | T-12-19 | Events filtered by thread and sender | unit | `--tests` *ChatThreadViewModelTest *ChatThreadViewModelRealtimeTest *ChatMessageMappingTest | ✅ W0 | ✅ green |
| 12-06-03 | 06 | 2 | CHAT-04 | T-12-17, T-12-18 | Read receipts only while shown and Connected, never repeated | unit (virtual time) | `--tests` *ChatThreadViewModelRealtimeTest *ChatThreadViewModelTest *ChatMessageMappingTest *RealtimeEmitsTest | ✅ W0 | ✅ green |
| 12-07-01 | 07 | 2 | SAFE-01 | — | Reason is an enum wire value | Robolectric | `--tests` *ReportAndBlockScreenTest | ✅ W0 | ✅ green |
| 12-07-02 | 07 | 2 | SAFE-02, SAFE-03 | T-12-21 | NOT_BLOCKER latch; Unblock only for the blocker | unit + Robolectric | `--tests` *ReportAndBlockViewModelTest *BlockUserDialogTest *ReportAndBlockScreenTest | ✅ W0 | ✅ green |
| 12-07-03 | 07 | 2 | SAFE-04 | T-12-20, T-12-22, T-12-23 | In-flight lock; no logging; self-thread hidden | unit + Robolectric | `--tests` *SafetyErrorsTest *ReportAndBlockViewModelTest *ReportAndBlockScreenTest *BlockUserDialogTest | ✅ W0 | ✅ green |
| 12-08-01 | 08 | 3 | CHAT-03 | — | N/A | unit | `--tests` *ChatThreadViewModelSendTest *ChatThreadViewModelTest | ✅ W0 | ✅ green |
| 12-08-02 | 08 | 3 | CHAT-03 | T-12-24 | Retry reuses the same client_msg_id | unit | `--tests` *ChatThreadViewModelSendTest *ChatThreadViewModelTest *ThreadReducerTest | ✅ W0 | ✅ green |
| 12-08-03 | 08 | 3 | SAFE-03, SAFE-04 | T-12-26 | Text restored, never lost; server stays authoritative | unit | `--tests` *ChatThreadViewModelSendTest *ChatThreadViewModelTest *ChatThreadViewModelRealtimeTest *BlockStateTest | ✅ W0 | ✅ green |
| 12-09-01 | 09 | 3 | CHAT-02 | T-12-27 | Plain text, no link detection | Robolectric | `--tests` *ChatThreadScreenTest | ✅ W0 | ✅ green |
| 12-09-02 | 09 | 3 | CHAT-02 | — | N/A | unit + Robolectric | `--tests` *ThreadRowsTest *ChatThreadScreenTest | ✅ W0 | ✅ green |
| 12-09-03 | 09 | 3 | CHAT-04, SAFE-03 | T-12-28 | View profile client-mode only | Robolectric | `--tests` *ChatThreadScreenTest *ThreadRowsTest | ✅ W0 | ✅ green |
| 12-10-01 | 10 | 3 | SAFE-01 | — | Reason is an enum wire value | Robolectric + unit | `--tests` *ReportUserSheetTest *ExpertProfileScreenTest *ProfileSafetyViewModelTest | ✅ W0 | ✅ green |
| 12-10-02 | 10 | 3 | SAFE-02 | T-12-31 | Initiate then block; session mark | unit | `--tests` *ProfileSafetyViewModelTest *ExpertProfileScreenTest | ✅ W0 | ✅ green |
| 12-10-03 | 10 | 3 | SAFE-04 | T-12-30, T-12-32 | In-flight lock; no logging | unit + Robolectric | `--tests` *ProfileSafetyViewModelTest *ReportUserSheetTest *ExpertProfileScreenTest *ExpertProfileViewModelTest | ✅ W0 | ✅ green |
| 12-11-01 | 11 | 3 | CHAT-05 | T-12-33 | N/A | Robolectric | `--tests` *UpcomingBookingScreenTest | ✅ (extended) | ✅ green |
| 12-11-02 | 11 | 3 | CHAT-05 | — | N/A | unit + Robolectric | `--tests` *BookingSummaryScreenTest *BookingSummaryViewModelTest | ✅ (extended) | ✅ green |
| 12-11-03 | 11 | 3 | CHAT-05 | T-12-34 | Busy guard on cards | Robolectric | `--tests` *MyBookingsScreenTest *UpcomingBookingScreenTest *BookingSummaryScreenTest | ✅ (extended) | ✅ green |
| 12-12-01 | 12 | 3 | CHAT-05 | T-12-35 | No Report/Block on the expert summary (D-11) | Robolectric | `--tests` *ExpertBookingSummaryScreenTest | ✅ (extended) | ✅ green |
| 12-12-02 | 12 | 3 | CHAT-05 | T-12-36 | No Report/Block on Request Decision (D-11) | unit + Robolectric | `--tests` *RequestDecisionScreenTest *RequestDecisionViewModelTest | ✅ (extended) | ✅ green |
| 12-12-03 | 12 | 3 | CHAT-05 | — | N/A | Robolectric | `--tests` *ExpertBookingsScreenTest *ExpertBookingSummaryScreenTest *RequestDecisionScreenTest | ✅ (extended) | ✅ green |
| 12-13-01 | 13 | 3 | CHAT-05 | T-12-37, T-12-38 | Call keeps running; minimize only on success | Robolectric + unit | `--tests` *MeetingCallActionsTest *MeetingCallUiStateTest *CallOverlayHostTest | ✅ (extended) | ✅ green |
| 12-13-02 | 13 | 3 | CHAT-05 | — | N/A | Robolectric | `--tests` *MeetingCallActionsTest *MeetingCallScreenTest *CallOverlayHostTest *MeetingCallViewModelTest *MeetingCallPillTest | ✅ (extended) | ✅ green |
| 12-14-01 | 14 | 4 | CHAT-03 | — | N/A | Robolectric | `--tests` *ChatComposerTest *ChatThreadScreenTest | ✅ W0 | ✅ green |
| 12-14-02 | 14 | 4 | CHAT-03 | — | N/A | Robolectric | `--tests` *ChatThreadScreenTest *ChatComposerTest | ✅ W0 | ✅ green |
| 12-14-03 | 14 | 4 | SAFE-03, SAFE-04 | T-12-39, T-12-40, T-12-41 | Composer capped; error text plain | Robolectric | `--tests` *ChatThreadScreenTest *ChatComposerTest *ChatThreadViewModelSendTest *ThreadRowsTest | ✅ W0 | ✅ green |
| 12-15-01 | 15 | 5 | all | T-12-42, T-12-43 | No Log calls in Phase 12 code; R8 keeps keys and adapters | full gate | full suite both flavors + ktlint + assemble + lint (separate) + assembleProdRelease (separate) + design rules | ✅ | ✅ green |
| 12-15-02 | 15 | 5 | all | — | N/A | doc greps | grep STATE / REQUIREMENTS / ROADMAP markers | ✅ | ✅ green |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [x] `app/src/test/java/com/uptrendinvestments/clickmeapp/testing/FakeClickMeApi.kt` — handlers and recorders for `initiateChat`, `getChats`, `getChatMessages`, `sendChatMessage`, `chatAction`, `reportUser` (12-01 Task 3)
- [x] `app/src/test/java/com/uptrendinvestments/clickmeapp/testing/FakeRealtimeConnection.kt` — record `emitTypingStart` / `emitTypingStop` / `emitMessageRead` (12-01 Task 3)
- [x] `app/src/test/java/com/uptrendinvestments/clickmeapp/testing/ChatFixtures.kt` — thread, message, event, payload and ack builders (12-01 Task 3)
- [x] `app/src/test/java/com/uptrendinvestments/clickmeapp/testing/ChatEntryTestSupport.kt` — `testChatEntryViewModel(...)` for Hilt-aware screen tests (12-04 Task 2)
- [x] `app/src/test/java/com/uptrendinvestments/clickmeapp/screens/chat/chatting/ChatThreadTestHarness.kt` — thread ViewModel builder (12-06 Task 1)
- [x] `ApiErrorParserTest` count/order assertion updated for the Android-only `CANNOT_CHAT_WITH_SELF` (12-01 Task 2)
- No framework install needed (JUnit4, Robolectric, compose-ui-test, coroutines-test, mockwebserver3 already configured).

---

## Manual-Only Verifications

All rows are deferred per the user's manual-testing preference and are logged as the non-blocking "[Phase 12] DEFERRED (human)" list in STATE by 12-15.

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Every Message entry point and the in-call Chat control land on the thread in the Chats tab | CHAT-05 | Real Hilt graph, real shells and a live call | STATE "[Phase 12] DEFERRED (human)" item (a) |
| Live two-way chat with iOS: arrival, optimistic send without duplicates, typing, Read, Load earlier, live booking events, failed-send retry, background catch-up | CHAT-02, CHAT-03, CHAT-04 | Needs a second client on the production backend | STATE item (b) |
| Report and block from chat and profile; report rate limit | SAFE-01, SAFE-02, SAFE-04 | Needs backend rows and a second account | STATE item (c) |
| Blocked thread on both sides, send into a blocked thread, RECIPIENT_UNAVAILABLE, empty-thread block | SAFE-03, SAFE-04 | Needs two accounts and a suspended user | STATE item (d) |
| 200% font scale, TalkBack, jump-to-latest feel, move-to-top animation, R8 smoke | all | Visual and device behavior | STATE item (e) |

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or Wave 0 dependencies
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 covers all MISSING references
- [x] No watch-mode flags
- [x] Feedback latency < 12s
- [x] `nyquist_compliant: true` set in frontmatter

**Approval:** approved 2026-10-27 (12-15 gate: 3310 tests per flavor, ktlint, lint, R8 assembleProdRelease, design rules green; manual rows deferred to STATE)
