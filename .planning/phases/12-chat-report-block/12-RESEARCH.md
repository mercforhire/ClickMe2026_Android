# Phase 12: Chat, Report & Block - Research

**Researched:** 2026-10-27
**Domain:** Android Compose chat client (REST + Socket.IO), moderation (report / block / unblock), cross-tab navigation hand-offs
**Confidence:** HIGH on backend contracts and in-repo integration points (read this session). MEDIUM on Compose chat-list mechanics (training knowledge, flagged `[ASSUMED]`). Two backend gaps change the product behaviour (unread badge, empty-thread block state) and need user confirmation (see Open Questions).

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

Carried forward (not re-opened):
- iOS is the source of truth except where a backend spec says otherwise. Here CHAT-UNBLOCK.md and USER-REPORTING.md override iOS: iOS sends display labels and has no blocked state.
- Report reasons are the six backend enum values with the spec's suggested labels: `harassment` "Harassment or abuse", `spam` "Spam", `inappropriate_content` "Inappropriate content", `scam_or_fraud` "Scam or fraud", `impersonation` "Impersonation", `other` "Something else". iOS's `defaultReasons` labels ("Fake profile", "Hate speech", …) are NOT ported.
- The blocked-thread UI follows the CHAT-UNBLOCK.md §5 matrix exactly. Local state after each outcome follows §6. `blocked_by_me` decodes with a default of `false`.
- API pattern: `ApiRequestUrls` constants, `ClickMeApi` methods, Hilt `@Singleton` repository using `apiCall {}`, Moshi codegen DTOs with explicit `@Json(name=…)`. `chatAction` returns a status-only envelope.
- Error copy: the server `message` in a Material `AlertDialog`.
- Refresh triggers follow Phase 8 D-10: a quiet reload when the tab is shown again, plus pull-to-refresh.
- Phase 9 D-11 hand-off: enable the in-call Chat control (`// PHASE-12` in `MeetingCallControls.kt`). It calls `initiateChat`, minimizes the call, switches to Chats and pushes the thread.
- Hide Message / Report / Block when targeting yourself (`CANNOT_CHAT_WITH_SELF`, `CANNOT_REPORT_SELF`, `CANNOT_BLOCK_SELF`). On a self-thread (partner id == own id), hide Report and Block.

**Send behaviour (CHAT-03, SAFE-04)**
- **D-01: Optimistic send with a failed state.** The bubble appears immediately as "sending". On a network error or 5xx it stays in place, dimmed, with a red "!" and "Not sent. Tap to retry". A long-press offers Delete, which removes it locally. Retry reuses the **same `client_msg_id`** so the server dedupes. This deviates from iOS, which waits for the ack before showing the bubble.
- **D-02: No offline queue.** A send always tries right away. If it fails, the message goes to the D-01 failed state. Nothing persists across process death or app restart.
- **D-03: 403 `THREAD_BLOCKED` / `RECIPIENT_UNAVAILABLE` on send:** Remove the optimistic bubble and put its text back in the composer. Refetch `GET /chats` for this thread's block state (CHAT-UNBLOCK §6). Switch the composer to its disabled banner state: "You can't reply to this conversation" / "You blocked this conversation" for blocked threads, "This user is unavailable." for `RECIPIENT_UNAVAILABLE`.
- **D-04: The composer caps input at the backend send-schema maximum.** The researcher looks this up in `backend-reference`. Show a character counter near the limit. iOS has no cap.
- Reconciliation: the send ack (`ChatSendAckData`: message id, status, timestamp) replaces the optimistic bubble by `client_msg_id`. A `message.new` echo or page load with the same server id or `client_msg_id` must never create a duplicate.

**Report & Block surfaces (SAFE-01..04)**
- **D-05: Block on Expert Profile is a confirm dialog, not the combined screen.** The ⋯ menu "Block {name}" opens an `AlertDialog`: "Block {name}? They won't be able to message you." On confirm, call `initiateChat(peerId)` and then `chatAction(threadId, "block")`, with a progress indicator and repeat taps ignored. On success, stay on the profile and show a snackbar. This deviates from iOS, which bootstraps the thread and then opens the full Report & Block screen.
- **D-06: Report on Expert Profile** goes in a `ModalBottomSheet` (port of `ReportUserSheet`). It has the reason picker (required), optional details (max 1000, trimmed) and `POST /users/{id}/report`.
- **D-07: The profile ⋯ menu always shows "Block {name}".** There is no block-state lookup. Re-blocking is a 200 no-op. Unblocking happens in the chat.
- **D-08: Inside a chat thread, port the iOS combined "Report & Block" screen** (`ReportChatView`) as a full-screen destination pushed from the thread's top bar ⋯. The Report card sends `chatAction(threadId, "report", reason, details)`. The Block/Unblock card follows the §5 matrix: Block when not blocked, Unblock only when `blocked_by_me`, and no Unblock when blocked by the peer. Block asks for confirmation first, as iOS does. On `403 NOT_BLOCKER`, apply §6 and latch Unblock off (iOS `unblockDenied`). Block state comes from scanning `GET /chats` for the thread (there is no single-thread endpoint). It fails silent and defaults to showing Block.
- **D-09: A successful block from chat exits the thread.** Close the Report & Block screen and pop the thread, returning to the Conversations list (iOS-like). Unblock success returns to the thread, which is now unblocked.
- **D-10: A successful report shows "Thanks, we'll review this."** The profile sheet closes with a snackbar. In the combined screen the Report card switches to its submitted state and the Block card stays usable. `429 TOO_MANY_REQUESTS` → "Please try again later." `404` → neutral "This user is unavailable."
- **D-11: Experts report and block clients only from chat.** No Report/Block on Request Decision or the expert Booking Summary. Client Profile is DEF-3.
- **D-12: Message always opens the thread, even when it's blocked.** The blocked banner and Unblock live there. Entry points don't check block state.

**Chat entry & navigation (CHAT-05)**
- **D-13: Every Message tap switches to the Chats tab and pushes the thread there**, using the shell "open chat thread" navigator hook noted in `Navigator.kt`. The original tab keeps its back stack. Back from the thread goes to the Conversations list. This matches iOS `openChatThread` and the in-call D-11 rule.
- **D-14: Message buttons appear on:** Expert Profile (next to Book Session, client mode, not on your own profile); Client Upcoming Booking Details and client Booking Summary (un-hides Phase 8's hidden Message Expert); Client My Bookings cards (un-hides the Phase 8 hand-off); Expert Request Decision footer and expert Booking Summary (Phase 11 hand-off); Expert Upcoming/Past booking cards, to match the client cards; The in-call Chat control.
- **D-15: While `initiateChat` is in flight, the Message button shows a spinner and ignores repeat taps.** On error, an `AlertDialog` shows the server message. A `404` → "This user is unavailable."

**Conversations list (CHAT-01)**
- **D-16: No unread badge on the Chats tab icon** (iOS parity). Each row keeps its unread count badge.
- **D-17: Loading (iOS):** `GET /chats?page=1&limit=50` only, with no paging. Realtime `message.new` moves the thread to the top and updates its preview and time. `user.status` updates presence. Pull-to-refresh, plus a quiet reload when the tab is shown again (and an iOS-style silent reload where iOS has one).
- **D-18: Keep the iOS search bar.** It filters the loaded threads locally by name or last message, case-insensitively.
- **D-19: Blocked threads look like any other row in the list** (iOS parity). The blocked state appears only inside the thread.

**Ported as-is from iOS (no discussion needed)**
- Thread: page size 30, `loadEarlier` prepends older pages (fails silently and stays retryable), booking system-event rows (`ChattingSystemEventRow`), an empty state, and the peer user id taken from the first peer message (never from admin broadcast rows).
- Typing: Outgoing `typing.start` goes out on the first non-empty edit after a quiet period, and a debounced `typing.stop` follows 3 s of idle. A send also emits stop. Incoming typing auto-clears after a timeout safety net. Own events are ignored.
- Read receipts: Emit `message.read` for visible unread peer messages on load and on each incoming `message.new` while the thread is open. Incoming `message.read` flips my bubbles to read. Own `message.new` echoes are deduped.

### Claude's Discretion
- Exact visual design of bubbles, failed state, banner, system-event rows and the Report & Block screen (Material 3, dark, Nunito, adapted from iOS; no pixel parity). The UI-SPEC phase may refine it.
- Where the Message button sits on each booking card and summary screen, and whether it's an icon or a text button.
- The relative time label format in the list (follow iOS `relativeTimeLabel` semantics with device locale).
- Whether the thread repository is per-thread or app-scoped, and how realtime events fan out to the list and the open thread.

### Deferred Ideas (OUT OF SCOPE)
- Unread badge on the Chats tab icon (rejected for MVP, iOS parity). Revisit after release.
- Paging the conversations list past 50 threads.
- A persistent offline send queue.
- A "Blocked" label on list rows.
- Report/Block from Client Profile on the expert side (Client Profile is DEF-3).
- Image attachments in chat (text only in the MVP).
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| CHAT-01 | View conversations list | `GET /chats` shape (below), `unread_count` is never maintained by the backend (Pitfall 1), thread exists only after first message (Pitfall 2), list VM fan-out from `RealtimeConnection.events` |
| CHAT-02 | Pages of 30, newest first, `loadEarlier`, inline system events, text only | `GET /chats/:id/messages` is offset-paginated, ascending within page; `event_type` / `event_data` shape; `reverseLayout` list; merge-by-id reducer (Pitfall 4) |
| CHAT-03 | Optimistic send reconciled by `client_msg_id` | `sendSchema` (content 1..10000, `client_msg_id` UUID, strict); server dedupes with 201; `message.new` is never echoed to the sender and carries no `client_msg_id` (Pitfall 3) |
| CHAT-04 | Read status + typing from realtime | Socket contract: `typing.*` needs `recipient_id`; `message.read` is socket-only; Android socket drops in background (Pitfall 5); existing `RealtimeEmits` / `RealtimeConnection` |
| CHAT-05 | `initiateChat` from Expert Profile, booking, request (and in-call) | `POST /chats/initiate` `{peer_id}` → 201 `{thread_id, created_at}`; `ChatLauncher` + one-shot hand-off to the shell (Architecture Pattern 3); peer ids available on every source DTO |
| SAFE-01 | Report from chat and Expert Profile with enum value | `POST /users/{id}/report` and chat `action: "report"`; `REPORT_REASONS`; 10/hour limiter; error table |
| SAFE-02 | Block from chat and Expert Profile | `block` is an idempotent 200 that keeps the first blocker; profile flow = `initiate` then `block` |
| SAFE-03 | Blocked-thread composer disabled, banner, Unblock only when `blocked_by_me` | CHAT-UNBLOCK §5/§6/§7 matrix; scan `GET /chats`; empty-thread gap (Pitfall 2) |
| SAFE-04 | `THREAD_BLOCKED`, `RECIPIENT_UNAVAILABLE`, `NOT_BLOCKER` clear messages | `ErrorCode` already has all but `CANNOT_CHAT_WITH_SELF`; error mapping table |
</phase_requirements>

## Summary

The backend is small and fully readable (`chats.controller.js`, `chat.schema.js`, `socket.js`), and almost everything Phase 12 needs on the Android side already exists: `RealtimeEvent.MessageNew/MessageRead/UserStatus/TypingStart/TypingStop`, `RealtimeEmits`, `RealtimeConnection.emitTypingStart/emitTypingStop/emitMessageRead`, `ErrorCode` entries for the chat/safety codes, the `OneShotSlot` one-shot hand-off store, Nav3 per-tab stacks with `Navigator.navigateInTab`, `PullToRefreshBox`, `ModalBottomSheet`, `AuthErrorDialog`, `PressScaleButton`. The new work is three screens (Conversations, Thread, Report & Block), a thread/list data layer with a pure merge reducer, the Message entry points, and the in-call hand-off.

Four backend facts materially shape the plan and are not visible in CONTEXT/UI-SPEC: (1) the backend **never increments `unread_count`** and `message.read` does not reset it, so the row badge the UI-SPEC specifies will always be 0 unless Android counts locally; (2) `GET /chats` **hides threads with no messages**, so a freshly initiated or freshly blocked empty thread is invisible to the block-state scan; (3) the server **does not echo `message.new` to the sender and the payload has no `client_msg_id`**, so "echo dedupe by `client_msg_id`" can only happen on page loads, never on the socket; (4) the Android **socket disconnects when the app is backgrounded**, so an open thread must catch up by REST on reconnect/resume.

**Primary recommendation:** Build one app-scoped `ChatsRepository` (stateless REST, `apiCall {}`) plus a pure, unit-tested `ThreadReducer` (merge by server id and `client_msg_id`), keep per-screen entry-scoped ViewModels that collect `RealtimeConnection.events`, route every Message tap and the in-call control through a single `@Singleton ChatLauncher` that does `initiateChat` and posts a `ChatThreadKey` to a `OneShotSlot` the active shell consumes with `navigateInTab`. Add `peerId` to `ChatThreadKey` (typing/read/profile need it from the first frame).

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Message persistence, dedupe by `client_msg_id`, block state, report storage | API / Backend | Database | Server enforces `THREAD_BLOCKED`, `NOT_BLOCKER`, rate limit; client never decides authorization |
| Optimistic bubble, failed/retry state, composer cap | Browser / Client (Compose VM) | — | D-01/D-02: memory-only, same `client_msg_id` on retry |
| Real-time delivery (message.new, typing, read, presence) | API / Backend (Socket.IO) | Client `RealtimeConnection` | Server relays; client only listens and emits typing/read |
| Unread counting | Client (session-local) | API (stale `unread_count`) | Backend never maintains it (Pitfall 1) |
| Cross-tab "open chat thread" | Client shell (Nav3) | App-scoped hand-off slot | In-call overlay lives above the shell (`CallOverlayHost` in `ClickMeApp.kt`), so a direct navigator call is impossible |
| Report / block / unblock | API / Backend | Client UI state per §6 | Client mirrors §6 local state and reconciles with `GET /chats` |
| Block-state lookup | API (`GET /chats` scan) | Client | No single-thread endpoint |

## Standard Stack

No new dependencies. Everything below is already in `gradle/libs.versions.toml` / the module. [VERIFIED: gradle/libs.versions.toml read this session]

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Compose BOM / Material 3 | `composeBom = "2026.09.00"` (UI-SPEC: material3 1.4.0 is the only version in the local cache) | `LazyColumn`, `PullToRefreshBox`, `ModalBottomSheet`, `DropdownMenu`, `SmallFloatingActionButton` | Project standard; UI-SPEC "New dependencies: none" |
| Navigation 3 | `navigation3 = "1.2.0"` | Per-tab stacks, `entry<Key>(metadata = BottomBar.hidden())` | Existing shells |
| Hilt + `hilt-lifecycle-viewmodel-compose` | `hilt = "2.60.1"`, `1.4.0` | `@HiltViewModel(assistedFactory = …)` keyed on the Nav key | Existing pattern (`ExpertProfileViewModel`) |
| Retrofit 3.0.0 / Moshi 1.15.2 (codegen) | as pinned | DTOs, `ClickMeApi` methods | Locked by CONTEXT |
| socket.io-client | `socketio = "2.1.2"` | Already wired in `RealtimeService` | Phase 3 |
| kotlinx-coroutines(-test) | `1.11.0` | VMs, `runTest` with `MainDispatcherRule` | Existing test pattern |
| JUnit4 + Robolectric + `compose-ui-test-junit4` + mockwebserver3 | per `app/build.gradle.kts` | Unit and Compose-screen tests under `testDevDebugUnitTest` | Existing |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Hand-written merge reducer | Room / Paging 3 | Room/Paging add a dependency and persistence that D-02 explicitly rejects (no offline); Paging 3 does not fit offset pages that shift under new messages. Keep the in-memory reducer |
| Per-thread repository | App-scoped repository | Recommended: **stateless** app-scoped repository (REST only); state lives in entry-scoped VMs. This avoids cross-account leaks (BookingRequestsRepository KDoc: "nothing clears singletons on logout") |

**Installation:** none.

## Package Legitimacy Audit

No external packages are added in this phase (UI-SPEC "New dependencies for UI: none"; stack above is already resolved in the project). Audit not applicable.

**Packages removed due to [SLOP] verdict:** none
**Packages flagged as suspicious [SUS]:** none

## Backend Contract (authoritative; read this session)

### Endpoints [VERIFIED: backend-reference/src/routes/chats.js, chats.controller.js, userReports.js]

| Method + path | Body (all `.strict()`) | Success | Notes |
|---|---|---|---|
| `POST /chats/initiate` | `{ peer_id: uuid }` (`expert_id` is a deprecated alias; send only `peer_id`) | `201 {status, data:{thread_id, created_at}}` | Idempotent. `422 CANNOT_CHAT_WITH_SELF`. `404 NOT_FOUND` only when creating a NEW thread with a suspended/terminated peer (existing threads still open) |
| `GET /chats?page&limit` | — | `200 data:{threads:[…], pagination:{total_count,total_pages,current_page}}` | `limit` is clamped to 100 (`Math.min(100, …)`), default 20. Threads with `last_msg_at` NULL are excluded |
| `GET /chats/:id/messages?page&limit` | — | `200 data:{messages:[…], pagination}` | `limit` clamped to 100, default 30. Page 1 = newest N, **ascending within the page**. Offset-based. Works on blocked threads. `404 NOT_FOUND` for non-participant |
| `POST /chats/:id/send` | `{type:'text'\|'image'\|'file', content: 1..10000, client_msg_id: uuid, attachments?: url[]}` | `201 data:{message_id, status:'sent', timestamp}` | Duplicate `client_msg_id` (per thread+sender) returns the same 201 (idempotent). `403 THREAD_BLOCKED` (checked first), `403 RECIPIENT_UNAVAILABLE`, `404 NOT_FOUND` |
| `POST /chats/:id/actions` | `{action:'block'\|'unblock'\|'report'\|'clear_history', reason?: ≤500, details?: ≤2000}` | `200 {status:'success'}` (no `data`) | Self-thread: `422 CANNOT_BLOCK_SELF` / `422 CANNOT_REPORT_SELF`. `unblock` by non-blocker: `403 NOT_BLOCKER`. Report missing reason stored as `other` |
| `POST /users/:id/report` | `{reason: enum, details?: trim ≤1000}` | `201 data:{report_id, status:'open', created_at}` | `404 NOT_FOUND`, `422 CANNOT_REPORT_SELF`, `422 VALIDATION_ERROR`, `429 TOO_MANY_REQUESTS` (code and handler) |

Verbatim schema quotes the tasks must match [VERIFIED: backend-reference/src/schemas/chat.schema.js:36-48]:

```js
const sendSchema = z.object({
  type:          z.enum(['text', 'image', 'file']),
  content:       z.string().min(1).max(10000),
  client_msg_id: z.string().uuid(),
  attachments:   z.array(z.string().url()).optional(),
}).strict();
```
```js
const actionsSchema = z.object({
  action:  z.enum(['block', 'unblock', 'report', 'clear_history']),
  reason:  z.string().max(500).optional(),
  details: z.string().max(2000).optional(),
}).strict();
```

Report reasons [VERIFIED: backend-reference/src/schemas/moderation.schema.js:9-16]:
```js
const REPORT_REASONS = [
  'harassment',
  'spam',
  'inappropriate_content',
  'scam_or_fraud',
  'impersonation',
  'other',
];
```
`reportUserSchema` is `{ reason: z.enum(REPORT_REASONS), details: z.string().trim().max(1000).optional() }.strict()`. Note `optional()` does **not** accept JSON `null`: the app-wide Moshi omits nulls (`MoshiProvider` comment: "The app-wide Moshi omits nulls"), so request DTOs must use nullable fields with default `null` and must NOT use `serializeNulls`. [VERIFIED: network/MoshiProvider.kt, network/NullPreservingAdapters.kt:1-12]

### `GET /chats` thread item [VERIFIED: chats.controller.js:213-224]
```js
thread_id:    thread.id,
partner_id:   partnerId,    // deprecated — retained one release; prefer partner.id
partner:      { ...buildDisplayInfo(partnerId, profile), roles: profile?.roles ?? [] },
last_message: thread.last_msg_text || null,
last_msg_at:  thread.last_msg_at || null,
unread_count: unreadCount,
is_online:    Boolean(online),
is_blocked:   Boolean(thread.is_blocked),
blocked_by_me: Boolean(thread.is_blocked) && isSameUser(thread.blocked_by, userId),
```
`partner` = `{id, name|null, avatar_url|null, roles:[…]}` (`displayInfo.js`). `last_message` is the server's 100-char preview. `blocked_by` is never serialized.

### `GET /chats/:id/messages` row [VERIFIED: chats.controller.js:291-320]
Columns `id, thread_id, sender_id, type, content, status, client_msg_id, event_type, event_data, created_at` plus `sender` = `{id,name,avatar_url}` or `null` for `type === 'event'`. `status ∈ sent|delivered|read` and `type ∈ text|image|file|event` [VERIFIED: supabase/migrations/20260609010000_create_messages.sql:20-35]. `event_type` values in use: `booking_request`, `BOOKING_CONFIRMED`, `BOOKING_DECLINED`, `BOOKING_CANCELLED`, `BOOKING_RESCHEDULED`, `BOOKING_COMPLETED`, `BOOKING_EXPIRED` [VERIFIED: iOS `MessageEventType.swift` and events.js `case` labels; the original CHECK lists only the first five and a later migration `20260614000000_extend_messages_event_type_completed_expired.sql` extends it]. `event_data` is free JSON: `{booking_id, client_id, expert_id, start_time, topic_title, duration_mins, end_time, [actions], [cancel_reason]}`; any enrichment field can be `null`. `sender_id` is `ON DELETE SET NULL`, so a `text` row can have a null sender (deleted account): branch on `type == "event"`, never on `sender_id == null`.

### Socket events [VERIFIED: backend-reference/docs/SOCKET-CONTRACT.md §3-§6, socket.js:263-330]
- Outbound `typing.start|stop` `{thread_id, recipient_id}`; `recipient_id` is required or the server drops it. `message.read` `{thread_id, message_ids[]}` (non-empty). Existing `RealtimeEmits` already builds these (ids lower-cased).
- Inbound `message.new` `{message_id, thread_id, sender_id, type, content, timestamp, status}` goes **only to the recipient's room** (`io.to(recipientId).emit`). No `client_msg_id`. The sender never receives it ("you do not receive your own message back over the socket — treat your successful 201 REST response as the authoritative echo").
- Inbound `message.read` `{thread_id, message_ids, read_by}`; `typing.*` `{thread_id, sender_id}`; `user.status {user_id, status}` (targeted to conversation partners).
- Booking system events do **not** arrive as `message.new` (they arrive as `booking.update`); an open thread only sees a new system row after a REST reload.

### Error code coverage in `ErrorCode.kt` [VERIFIED: model/enums/ErrorCode.kt:60-67 and the Account block read this session]
Present: `ThreadBlocked("THREAD_BLOCKED")`, `NotBlocker("NOT_BLOCKER")`, `CannotBlockSelf("CANNOT_BLOCK_SELF")`, `CannotReportSelf("CANNOT_REPORT_SELF")`, `RecipientUnavailable("RECIPIENT_UNAVAILABLE")`, `TooManyRequests("TOO_MANY_REQUESTS")`, `NotFound("NOT_FOUND")`, `ValidationError("VALIDATION_ERROR")`. **Missing: `CANNOT_CHAT_WITH_SELF`** (grep over `app/src/main` returns nothing). Add `CannotChatWithSelf("CANNOT_CHAT_WITH_SELF")`. The KDoc says "same 65 cases, same order" with iOS, so also update that comment (and any count assertion in the existing ErrorCode test).

## Architecture Patterns

### System Architecture Diagram

```
 Message buttons (profile, cards, summaries, request footer)    In-call Chat control (overlay, ABOVE the shell)
        |                                                              |
        v                                                              v
   ChatLauncher.open(peerId, name, avatar)  <--- one in-flight per peer, returns Result ---+
        |  POST /chats/initiate {peer_id}  (ChatsRepository, apiCall{})                   |
        |  ok -> ChatLaunchRequest.thread.post(ChatThreadKey)  (OneShotSlot)              |
        v                                                                                 |
   Shell (HomeClientScreen / HomeExpertScreen) collects slot --consume--> Navigator.openChatThread(
        ChatsRoot, key): select Chats tab; reuse if top==key else reset to root + push)  |
        v
 ChatsRoot = ConversationsScreen --row tap--> ChatThreadKey ---------> ChatThreadScreen
   |  ConversationsViewModel                                              | ChatThreadViewModel (entry-scoped)
   |   GET /chats?page=1&limit=50                                         |  GET /chats/{id}/messages?page&limit=30
   |   realtime: MessageNew (move to top, local unread++), UserStatus     |  POST /chats/{id}/send (optimistic, client_msg_id)
   |   quiet reload: ON_RESUME, return from thread, socket reconnect      |  realtime: MessageNew/MessageRead/Typing*
   v                                                                      |  emit: typing.start/stop, message.read
 RealtimeConnection.events (SharedFlow, replay 0, buffer 64 DROP_OLDEST) <-+  block state: GET /chats scan (limit 100)
                                                                          |
                              thread top bar menu --> ReportAndBlockScreen (ReportAndBlockViewModel)
                                  report: POST /chats/{id}/actions {report, reason, details}
                                  block/unblock: POST /chats/{id}/actions
                                  block ok -> pop 2 (screen + thread) -> Conversations snackbar
                                  unblock ok -> pop 1 -> thread (re-reads block state)
 Expert Profile: menu -> ReportUserSheet (POST /users/{id}/report) | BlockUserDialog (initiate -> block)
```

### Recommended Project Structure (mirrors iOS `Views/Screens/Chat/`)
```
app/src/main/java/com/uptrendinvestments/clickmeapp/
├── model/enums/UserReportReason.kt            # six enum values + wire strings
├── model/requests/ChatRequests.kt             # InitiateChatRequest, SendMessageRequest, ChatActionRequest, ReportUserRequest
├── model/responses/ChatPayloads.kt            # ChatThreadItem, ChatPartnerDto, ChatsPayload, ChatMessageItem, ChatMessagesPayload, ChatSendAckData, InitiateChatData, ReportUserData, StatusOnlyResponse
├── screens/chat/ChatsRepository.kt            # @Singleton, stateless
├── screens/chat/ChatLauncher.kt + ChatLaunchRequest.kt   # @Singleton, OneShotSlot<ChatThreadKey>
├── screens/chat/ThreadReducer.kt              # pure merge/ack/failure reducer (unit-tested)
├── screens/chat/conversations/…               # Screen, ViewModel, UiState, subviews/
├── screens/chat/chatting/…                    # ChatThreadKey, Screen, ViewModel, UiState, subviews/
├── screens/chat/reportandblock/…              # ChatReportBlockKey, Screen, ViewModel, subviews/
├── components/safety/ReportUserSheet.kt, BlockUserDialog.kt
└── components/chat/MessageButton.kt
```

### Pattern 1: Pure `ThreadReducer` + thin ViewModel
**What:** All message-list rules live in a pure function over `List<ThreadItem>` so reconcile/dedupe/ack/failure/paging are testable without coroutines or Compose.
```kotlin
// Source: design derived from backend contract above and iOS ChattingViewModel.swift
sealed interface ThreadItem {
    val key: String                              // LazyColumn key: server id, else client_msg_id
    data class Message(
        val serverId: String?, val clientMsgId: String?, val senderId: String?,
        val text: String, val createdAt: Instant,
        val mine: Boolean, val status: Delivery,  // Sending | Sent | Read | Failed
    ) : ThreadItem
    data class SystemEvent(val serverId: String, /* event_type, parsed event_data … */) : ThreadItem
}
// Rules (each is a unit test):
// merge(existing, incoming): match by serverId first, then by clientMsgId (a Sending/Failed local bubble
//   becomes the server row); never drop local Sending/Failed bubbles that have no server match; stable sort by createdAt
//   with local-pending bubbles last.
// ack(clientMsgId, serverId, timestamp): Sending|Failed -> Sent
// fail(clientMsgId): Sending -> Failed;  retry(clientMsgId): Failed -> Sending (same clientMsgId)
// onMessageNew(event): ignore if senderId == me (case-insensitive) or serverId already present; else append
// onMessageRead(ids): flip mine && serverId in ids -> Read
// prependOlder(page): merge by serverId (offset pages shift when new messages arrive, so duplicates are expected)
```

### Pattern 2: List built with `reverseLayout = true`
**What:** Keep the VM list oldest→newest; render `LazyColumn(reverseLayout = true)` over `items.asReversed()` with `key = { it.key }`. Index 0 is the newest, so "land on newest with no animation", "scroll to bottom on own send" (`animateScrollToItem(0)`) and "prepend older without moving the viewport" need no special code, and `animateItem()` still applies. Jump-to-latest visibility = `listState.firstVisibleItemIndex > 0` and distance. [ASSUMED: `reverseLayout` semantics are from training knowledge; the fetched Compose lists doc covers keys, `animateItem` and `animateScrollToItem` only. Keys + `animateItem` + `animateScrollToItem` are [CITED: developer.android.com/develop/ui/compose/lists]]
Stable keys are mandatory: `key = message.serverId ?: message.clientMsgId`. When an ack gives a Sending bubble a server id, the key changes; use `clientMsgId` as the key for any bubble that has one (own messages) so the item is not recreated and does not flash. For incoming rows use `serverId`.

### Pattern 3: One launcher, one slot, shell consumes
**What:** `ChatLauncher` (`@Singleton`) owns `initiateChat`; on success it posts `ChatThreadKey` to a `OneShotSlot<ChatThreadKey>` (`OneShotSlot` already exists: `screens/expertmode/ExpertLaunchRequest.kt`, "A memory-only, consume-on-read slot"). Each shell composition (inside `HomeClientScreen` / `HomeExpertScreen`, which are Hilt-aware) collects the slot and calls a new `Navigator.openChatThread(chatsRoot, key)`. Why not call `Navigator` directly from screens: the in-call control lives in `CallOverlayHost`, which `ClickMeApp.kt` mounts around the **route switch** (above both shells), and shells are intentionally Hilt-free (`HomeClientShell` KDoc "no Hilt or BuildConfig access"); a slot is the same mechanism Phase 8/11 used for exactly this reason (`BookingsLaunchRequest`, `ExpertLaunchRequest`).
Mode-specific roots: the real keys are `ClientChatsRoot` and `ExpertChatsRoot` (not a single `ChatsRoot`) [VERIFIED: HomeClientTab.kt:28-29 `data object ClientChatsRoot : NavKey`, HomeExpertTab.kt:28-29 `data object ExpertChatsRoot : NavKey`]. The consumer in each shell passes its own root. Both shells already render the Chats tab entry as `ClientTabRoot(ClientChatsRoot)` / `ExpertTabRoot(ExpertChatsRoot)` placeholders that Phase 12 replaces via a new `chatsRoot` slot (same pattern as `bookingsRoot`).
`Navigator.navigateInTab` today only does `state.backStacks.getValue(tab).add(route)` [VERIFIED: navigation/Navigator.kt:121-129]. It neither de-duplicates nor resets, so Message tap for thread B while thread A is on the Chats stack yields `[Root, A, B]` and Back lands on A, violating UI-SPEC "Back goes to ChatsRoot". Add `openChatThread(tab, key)`: if the Chats stack top equals `key` just select the tab; otherwise pop the Chats stack to its root then push `key`. (iOS `chatsPath.push` does not reset; this is a deliberate Android tightening that matches the UI-SPEC wording.)

### Pattern 4: Entry-scoped assisted ViewModels keyed on the Nav key
Same as `ExpertProfileViewModel` (`@HiltViewModel(assistedFactory = …)`, `hiltViewModel<VM, VM.Factory>(creationCallback = { it.create(key) })`) [VERIFIED: ExpertProfileViewModel.kt:34-43]. Persist composer text/search/reason via `SavedStateHandle` (precedent: `SignupBasicInfoViewModel`, `MakeABookingViewModel`).

### Pattern 5: Realtime collection and catch-up
VMs inject `RealtimeConnection` and collect `realtime.events` (precedent: `UpcomingBookingViewModel`, `MyBookingsViewModel`). The flow is `MutableSharedFlow(replay = 0, extraBufferCapacity = 64, onBufferOverflow = DROP_OLDEST)` [VERIFIED: RealtimeService.kt:44-50]; events are lost while nobody collects and while the socket is down. `RealtimeConnectionManager` disconnects whenever `loggedIn && (foreground || inCall)` is false [VERIFIED: RealtimeConnectionManager.kt `combine(...) { loggedIn, fg, inCall -> loggedIn && (fg || inCall) }`]. So both the list and the open thread must also collect `realtime.state` and run a **quiet reload** on each transition into `Connected` after having been not-`Connected` (skip the first emission), in addition to the `ON_RESUME` quiet reload.

### Anti-Patterns to Avoid
- **Deduping the socket "echo" by `client_msg_id`:** the payload has none and the sender never gets an echo. Dedupe `message.new` by `message_id` and ignore `sender_id == me`. Use `client_msg_id` only when merging REST pages.
- **Singleton message cache:** nothing clears `@Singleton`s on logout (BookingRequestsRepository KDoc). Keep message/thread state in VMs; the repository stays stateless. If a cache is added, register a `SessionDataCleaner` (`managers/SessionDataCleaner.kt`, bound `@IntoSet`).
- **Treating `sender_id == null` as "system event":** deleted-account text rows also have null senders. Branch on `type`.
- **Comparing UUIDs with `==`:** the backend compares case-insensitively (`isSameUser`); compare with `equals(ignoreCase = true)` for "mine" and "self-thread". `RealtimeEmits` lower-cases outgoing ids.
- **Logging chat text:** `MessageNew.toString()` already redacts content ("Chat content is user data: only ids are printed"); give every new DTO holding content (`ChatMessageItem`, send request) the same redacting `toString()`.
- **Blocking send on a previous send:** UI-SPEC says sends are independent; reducer must support multiple concurrent Sending bubbles.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| One-shot cross-screen hand-off | A new event bus | `OneShotSlot<T>` from `ExpertLaunchRequest.kt` | Already tested; consume-on-read semantics fit "open thread once" |
| Socket client / emits | New socket code | `RealtimeConnection.emitTypingStart/Stop/emitMessageRead`, `RealtimeEmits` | Handles lower-casing, only-while-Connected, JSON via Moshi |
| Error → UI text | Per-screen parsing | `ApiException.Http.code` / `.status` / `.userMessage` and `ApiException.Network` | Phase 2 pattern; ErrorCode parse is lenient |
| Pull to refresh, sheets, menus | Custom gestures | `PullToRefreshBox`, `ModalBottomSheet`, `DropdownMenu` | In use already (Explore, MyBookings) |
| Idempotent send | Client-side dedupe tables | Server UNIQUE(thread, sender, `client_msg_id`) + same id on retry | Backend returns the existing row with 201 |
| Debounce timers | Handler/Thread | `delay` in a cancellable `Job` with `runTest` virtual time | Matches iOS Task.sleep logic and is deterministic in tests |
| Destructive button, error dialog, empty/error states | New composables | `DestructiveOutlinedButton`, `AuthErrorDialog`, `EmptyState`, `SkeletonBlock` | UI-SPEC inventory |

**Key insight:** the only genuinely new logic is the merge/reconcile rules and the block-state state machine. Isolate both as pure functions so they can be proven without a device (manual two-client tests are deferred by project preference).

## Runtime State Inventory

Not a rename/refactor/migration phase. Omitted.

## Common Pitfalls

### Pitfall 1: The unread badge will always be 0 (backend never maintains `unread_count`)
**What goes wrong:** D-16/UI-SPEC render `UnreadBadge` from `unread_count`; the server value is always 0.
**Why:** `send` never increments it (controller comment: "Deferred to a future patch"; `void unreadField; // unread increment deferred`), and the socket `message.read` handler only updates `messages.status`, never `chats.unread_count_*` [VERIFIED: chats.controller.js:446-451, socket.js:283-327]. SOCKET-CONTRACT §6: "`unread_count` is not maintained… Compute unread counts client-side from message `status`". iOS's own VM comment says the same.
**How to avoid:** render `max(server unread_count, localUnread[threadId])`. Maintain `localUnread` in the list VM: increment on `MessageNew` where `sender != me` and the thread is not the open one; clear when the thread is opened. This is session-local (resets on process death) and cannot see messages received while the socket was down. Flag to the user (Assumption A1). Report the backend gap to the backend team (same bucket as the other pending iOS/backend follow-ups).
**Warning signs:** the badge never appears in a two-device test.

### Pitfall 2: Empty threads are invisible to `GET /chats`
**What goes wrong:** `GET /chats` has `.not('last_msg_at', 'is', null)`. A thread created by `initiate` (Message tap, or Block from Profile D-05) has no messages and is not listed. Consequences: (a) the Report & Block block-state scan and the thread's initial block-state fetch find nothing; (b) after "Block {name}" on a profile, tapping Message opens an empty thread with a normal composer; the first send gets `403 THREAD_BLOCKED`, the refetch still does not list the thread; (c) system events bump `last_msg_at` (events.js "bump the thread like a user send does"), so event-only threads are listed.
**How to avoid:** on `THREAD_BLOCKED` set `isBlocked = true` locally *even if the scan finds no row*; set `blockedByMe` from the scan if found, else keep the last known local value (the profile Block flow can record `blockedByMe = true` for that `threadId` in a small in-memory `Set` on `ChatsRepository`-adjacent session state, cleared on logout), else `false` (conservative: shows "You can't reply", no Unblock). A scan for a `threadId` with no row should leave the thread's state unchanged rather than "unblocked". Add a test for each branch.

### Pitfall 3: "Echo dedupe" is mostly a REST-merge problem
**What goes wrong:** Plans that wait for a `message.new` echo with `client_msg_id` will either never reconcile or double-render. **Reality:** ack (201) replaces the bubble; `message.new` only ever carries the *peer's* messages; the only place a own-message `client_msg_id` reappears is in `GET /messages` rows (pull-to-refresh, quiet reload, `loadEarlier`).
**Scenario that matters:** POST succeeded server-side but the response was lost (timeout) → bubble goes Failed → a reload returns the server row with the same `client_msg_id` → reducer must turn the Failed bubble into the Sent server row (not show both). Retry of a Failed bubble must reuse `client_msg_id` (server returns the existing row, 201).
**How to avoid:** `merge` matches `clientMsgId` for own messages; tests for "lost response then reload" and "retry after server already stored it".

### Pitfall 4: Offset pagination shifts under live traffic
**What goes wrong:** `loadEarlier` requests page N+1 by offset; messages received/sent after page 1 shift the window so page 2 re-includes rows already shown (never skips). iOS prepends blindly and would show duplicates.
**How to avoid:** `prependOlder` merges by server id (reducer rule). `hasMore = currentPage < totalPages` using the latest `pagination` (`PaginationMeta.hasMoreOrDerived` exists: `model/responses/PaginationMeta.kt`; note it requires `total_pages` and `current_page` ints, both present in the chat responses). A silent failure leaves `oldestLoadedPage` unchanged so the button stays retryable.

### Pitfall 5: The socket is down in the background; events replay is 0
**What goes wrong:** Peer messages sent while Android was backgrounded never arrive as events (push is Phase 13). An open thread/list silently misses them.
**How to avoid:** quiet reload on `ON_RESUME` (existing `LifecycleEventEffect(Lifecycle.Event.ON_RESUME)` pattern in `MyBookingsScreen`) **and** on `realtime.state` re-entering `Connected`. Also re-emit `message.read` for any newly loaded unread peer messages after each reload (reducer returns the ids that are peer && !read && have a server id; emit only ids not already emitted this session to avoid spamming).
**Related:** emits are dropped unless `Connected` ("Only while Connected: the library would otherwise buffer the emit and flush it stale later"), so `message.read` after a load that happens before the socket connects is lost; re-emit on the Connected transition too.

### Pitfall 6: Typing needs the peer id before the first message
`typing.*` requires `recipient_id`. iOS only knows `peerUserId` after seeing a peer message, so typing never works in an empty or own-messages-only thread, and "View profile" is dead. **How to avoid:** carry `peerId` in `ChatThreadKey` (every entry point calls `initiateChat(peerId)` so it is known; the list row has `partner.id`). Fall back to "first non-me sender among loaded text rows" only when the key has none (push routing in Phase 13). This deviates from the UI-SPEC key shape `ChatThreadKey(threadId, peerName, peerAvatarUrl)` by one field; no visual change.

### Pitfall 7: Duplicate Nav3 content keys across tab stacks
`NavigationState.toDecoratedEntries` flattens `stacksInUse` = start tab + selected tab [VERIFIED: NavigationState.kt `stacksInUse`, `toDecoratedEntries`], and existing KDoc says equal keys on two stacks collide (`AvailabilityOrigin`, `ExpertBookingOrigin`). The client start tab is Explore, the expert start tab is Dashboard; the Chats stack is flattened together with it. Pushing from the thread (system-event tap → `BookingSummaryKey`/`UpcomingBookingKey`/`ExpertBookingSummaryKey`, "View profile" → `ExpertProfileKey`) can collide with an equal key already on the Explore/Dashboard stack (e.g. Explore → profile of X → Message → thread → View profile of X; `ExpertProfileKey(expertId, name, title, imageUrl, …)` can be equal if seeded identically). **How to avoid:** add a defaulted origin/source field to every key the thread can push (`ExpertBookingOrigin.Chats` exists as an enum to extend; add `origin` with default to `ExpertProfileKey`, `BookingSummaryKey`, `UpcomingBookingKey`). Defaults keep existing call sites compiling. Also `ChatThreadKey` can only live on the Chats stack, so it cannot collide.

### Pitfall 8: Message buttons are not "hidden", they do not exist
STATE says "Message Expert is hidden", but the screens' KDocs say "without … Message Expert" [VERIFIED: grep of `BookingSummaryScreen.kt:76`, `UpcomingBookingScreen.kt:95`, `ExpertBookingSummaryScreen.kt:87`, `ExpertUpcomingCard.kt:83`, `RequestDecisionBlocks.kt:92`]. Each screen needs: peer id (all present: `BookingDetailExpertDto.id` is **nullable**; `BookingRequestDetailClientDto.id`, `ExpertBookingClientDto.id` and the My Bookings card DTO `BookingExpertDto.id` are non-null `String`s [VERIFIED: ClientBookingsPayload.kt `data class BookingExpertDto(val id: String, …)`, BookingRequestsPayload.kt, ExpertBookingsPayload.kt]), the peer name/avatar for the key, and the launcher state. Disable the control when the peer id is null.

### Pitfall 9: In-call control has no peer id in UI state
`MeetingCallUiState` has `peerName`, `peerAvatarUrl` but **no `peerId`** [VERIFIED: MeetingCallUiState.kt:12-30]; `CallSession.peerId` is nullable (`managers/CallSession.kt:42`). Add `peerId` to the UI state (or read `callCenter.session.value?.peerId`) and disable the control when null (UI-SPEC already says so). The overlay swallows touches when expanded, so success must call `callCenter.minimize()` after the launcher posts the key.

### Pitfall 10: `ConversationsScreen` quiet reload vs Nav3 re-show
Phase 8 noted "The Nav3 entry may not receive ON_RESUME when it is re-shown; it leaves and re-enters composition instead" (`MyBookingsScreen.kt:87`). Reuse the same `onScreenShown()/onScreenHidden()` pair so returning from a thread (pop) and tab re-selection both trigger a quiet reload.

### Pitfall 11: Lint errors not caught by unit tests
STATE: "The 11-15 gate found three lint errors that earlier plans' unit-test runs do not cover (`NonObservableLocale`, `ViewModelConstructorInComposable`); later plans should run `lintDevDebug` before the gate plan." Date/time formatting (`relativeTimeLabel`, system-event subtitle, date separators) must pass an explicit `Locale` from `LocalConfiguration`/`Locale.getDefault()` via the project's existing helpers, and test code must not construct ViewModels in composables.

## Code Examples

### Retrofit surface (new `ClickMeApi` methods) — matches existing style
```kotlin
// Constants in ApiRequestUrls (Phase 12 group)
const val CHATS_INITIATE = "chats/initiate"
const val CHATS = "chats"
const val CHAT_MESSAGES = "chats/{id}/messages"
const val CHAT_SEND = "chats/{id}/send"
const val CHAT_ACTIONS = "chats/{id}/actions"
const val USER_REPORT = "users/{id}/report"

@POST(ApiRequestUrls.CHATS_INITIATE)
suspend fun initiateChat(@Body body: InitiateChatRequest): SuccessDataResponse<InitiateChatData>

@GET(ApiRequestUrls.CHATS)
suspend fun getChats(@Query("page") page: Int, @Query("limit") limit: Int): SuccessDataResponse<ChatsPayload>

@GET(ApiRequestUrls.CHAT_MESSAGES)
suspend fun getChatMessages(@Path("id") threadId: String, @Query("page") page: Int, @Query("limit") limit: Int): SuccessDataResponse<ChatMessagesPayload>

@POST(ApiRequestUrls.CHAT_SEND)
suspend fun sendChatMessage(@Path("id") threadId: String, @Body body: SendMessageRequest): SuccessDataResponse<ChatSendAckData>

/** Returns `{status:"success"}` only (no `data`), so the envelope must be status-only. */
@POST(ApiRequestUrls.CHAT_ACTIONS)
suspend fun chatAction(@Path("id") threadId: String, @Body body: ChatActionRequest): StatusOnlyResponse

@POST(ApiRequestUrls.USER_REPORT)
suspend fun reportUser(@Path("id") userId: String, @Body body: ReportUserRequest): SuccessDataResponse<ReportUserData>
```
`needAuthToken` is an allowlist of public paths, so these need no change [VERIFIED: ApiRequestUrls.kt `publicPaths`].

### DTO notes
```kotlin
@JsonClass(generateAdapter = true)
data class ChatThreadItem(
    @Json(name = "thread_id") val threadId: String,
    val partner: ChatPartnerDto,
    @Json(name = "last_message") val lastMessage: String? = null,
    @Json(name = "last_msg_at") val lastMsgAt: Instant? = null,
    @Json(name = "unread_count") val unreadCount: Int = 0,
    @Json(name = "is_online") val isOnline: Boolean = false,
    @Json(name = "is_blocked") val isBlocked: Boolean = false,
    @Json(name = "blocked_by_me") val blockedByMe: Boolean = false,   // CHAT-UNBLOCK §9: absent -> false
)
// ChatPartnerDto(id: String, name: String?, @Json(name="avatar_url") avatarUrl: String?, roles: List<String> = emptyList())
// ChatMessageItem: id, @Json thread_id, @Json sender_id: String?, type: String, content: String?, status: String,
//   @Json client_msg_id: String?, @Json event_type: String?, @Json event_data: Map<String, Any?>?, @Json created_at: Instant, sender: SenderDto?
// StatusOnlyResponse(val status: String? = null)  (SuccessMessageResponse already tolerates absent keys; a dedicated type keeps intent clear)
```
Keep `type`, `status`, `event_type` as `String` (the repo's stated convention in `RealtimeEvents.kt`: "Ids, `type` and `status` stay [String]… a closed enum … would drop values"). `event_data` numbers decode as `Double` through Moshi's `Any?` handling (`duration_mins` may arrive as number or string; coerce both as iOS `extractInt` does).

### Typing / read emit rules (iOS parity, deterministic)
```kotlin
// First non-empty edit after quiet -> emitTypingStart once; each edit restarts a 3_000 ms stop timer.
// Empty text, send, or the 3 s timer -> emitTypingStop (only if we emitted start). Incoming typing.start sets
// peerTyping and (re)starts a 6_000 ms safety timer (iOS peerTypingTimeoutTask uses 6 s); typing.stop or a
// message.new from the peer clears it. Ignore events where threadId != this thread or senderId == me.
```
[VERIFIED: ios-reference ChattingViewModel.swift `typingIdleWindow` = 3 s and the 6 s `peerTypingTimeoutTask`.]

### Report/Block state machine (pure, per CHAT-UNBLOCK §5/§6)
```kotlin
data class BlockState(val isBlocked: Boolean = false, val blockedByMe: Boolean = false, val unblockDenied: Boolean = false)
// scan(found): isBlocked = found.isBlocked; blockedByMe = found.blockedByMe          (not found -> unchanged)
// block 200:         isBlocked = true,  blockedByMe = true
// unblock 200:       isBlocked = false, blockedByMe = false
// unblock 403 NOT_BLOCKER: isBlocked = true, blockedByMe = false, unblockDenied = true
// send 403 THREAD_BLOCKED: isBlocked = true (+ refetch; see Pitfall 2)
// Unblock button visible iff isBlocked && blockedByMe && !unblockDenied. Block button visible iff !isBlocked.
```

## Error Mapping (what each screen must show)

| Call | Condition | Copy (UI-SPEC) |
|---|---|---|
| send | network / 5xx | Failed bubble, no dialog |
| send | 403 `THREAD_BLOCKED` | remove bubble, restore text, refetch, banner |
| send | 403 `RECIPIENT_UNAVAILABLE` | remove bubble, restore text, "This user is unavailable." banner |
| send | other 4xx (`404`, `422`) | remove bubble, restore text, `AuthErrorDialog` "Couldn't send message" |
| initiate | `404` | "This user is unavailable." |
| initiate | `422 CANNOT_CHAT_WITH_SELF` | unreachable if buttons are hidden for self; fallback server message |
| report | `429` | "Please try again later." (server text is "Too many reports. Please try again later."; UI-SPEC pins the shorter copy) |
| report | `404` | "This user is unavailable." |
| block / unblock | `404` | "This conversation is unavailable." |
| unblock | 403 `NOT_BLOCKER` | server message, apply §6, latch off |
| any | `ApiException.Network` | existing `login_error_network` |
Map by `ApiException.Http.code` first, then `status` (`404`/`429` can arrive with a code or, for the rate limiter, `TOO_MANY_REQUESTS`). 403 `ACCOUNT_SUSPENDED` is handled globally by `SuspensionInterceptor` (Phase 2); do not special-case it.

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| iOS: wait for send ack, then show bubble | Optimistic bubble keyed by `client_msg_id` | CONTEXT D-01 | Needs the reducer and failed state |
| iOS: report reason display labels | Six enum values | 261016-m5y backend spec | `UserReportReason.wire` |
| iOS: no blocked state | `is_blocked` + `blocked_by_me` matrix | 261017-htx | Composer banner + Unblock rules |
| Legacy blocks (before 2026-10-17) | `blocked_by_me == false` for both sides | CHAT-UNBLOCK §7 | Neither side sees Unblock, even though the server would accept it; UI-SPEC says "You can't reply" |

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | The row unread badge should be `max(server unread_count, session-local count)` and is acceptable as session-local. The user's D-16 says "each row keeps its unread count badge" without knowing the backend never maintains it. | Pitfall 1 | If the user expects a persistent badge, only a backend fix delivers it; otherwise the badge is a lie after an app restart |
| A2 | `LazyColumn(reverseLayout = true)` over a reversed list is the right way to get "open at newest, prepend without jumping" | Pattern 2 | Wrong behaviour would be caught in the Compose screen test and by hand; fallback is `scrollToItem(last)` plus key-anchored prepends |
| A3 | Scanning `GET /chats` with `limit=100` (the backend cap) for the block-state read is acceptable instead of CONTEXT/iOS's 50 | Report & Block | Low: strictly fewer misses; list itself stays at 50 per D-17 |
| A4 | A session-local in-memory "blocked by me" set (for threads absent from `GET /chats`) is acceptable | Pitfall 2 | Without it, Block from profile then Message shows "You can't reply" with no Unblock until the thread has a message |
| A5 | Resetting the Chats stack to its root before pushing a thread (instead of iOS's plain push) matches intent | Pattern 3 | If the user wants iOS's stack-on-stack behaviour, Back goes to the previous thread |
| A6 | Booking system-event rows do not arrive live; an open thread refreshes them only via the quiet reload (and optionally on `booking.update` for that booking) | Backend Contract | Low: iOS has the same limit |
| A7 | `PullToRefreshBox` exists in the project's material3 (already imported in Explore/MyBookings/SignupOverview); version beyond "1.4.0 in cache" not confirmed from a lockfile | Standard Stack | None (already compiling in-repo) |

## Open Questions

1. **Unread badge truthfulness (A1).**
   - Known: server `unread_count` is always 0; `message.read` does not reset it; iOS has the same defect.
   - Unclear: whether the user prefers a session-local badge or no badge until the backend ships the fix.
   - Recommendation: implement the session-local count behind one function (`displayUnread`) so it can be swapped for the server value when fixed; log the backend gap in STATE "Backend notes".
2. **Empty-thread block state (Pitfall 2).** Recommend the in-memory handling above; also worth asking the backend to include empty threads (or expose `GET /chats/:id`).
3. **Peer role for the "View profile" menu item (UI-SPEC Open Item 3).** From the Conversations list the role is `partner.roles` (contains `"expert"`); from entry points it is known by construction (client booking/profile → expert). Recommend `ChatThreadKey.peerIsExpert: Boolean?` (null → hide the item), as UI-SPEC A4 already allows.
4. **Booking `booking.update` refresh of an open thread:** optional; recommend collecting `BookingUpdate` in the thread VM and quiet-reloading page 1 (cheap, gives live system-event rows). Planner's call.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| JDK (Android Studio JBR) | Gradle build/tests | ✓ | OpenJDK 25.0.3 at `/Applications/Android Studio.app/Contents/jbr/Contents/Home` | — |
| `./gradlew` | all commands | ✓ | wrapper present | — |
| Node | gsd-tools only | ✓ | v26.8.1 | — |
| Second client (iOS / second Android) | real two-way chat, typing, read, block interplay | ✗ in CI | — | Deferred manual checks (project preference: manual testing deferred); unit tests cover the logic |
| Live backend | manual verification | not probed | — | `backend-reference/` code read instead; contract-level tests use mockwebserver3 / `FakeClickMeApi` |

**Missing dependencies with no fallback:** none for automated work.

## Validation Architecture

> `workflow.nyquist_validation` is absent in `.planning/config.json` (only `_auto_chain_active`), so it is treated as enabled.

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit4 + Robolectric + Compose `ui-test-junit4` + `kotlinx-coroutines-test` + mockwebserver3 (all in `app/build.gradle.kts`) |
| Config file | none beyond `testOptions.unitTests.isIncludeAndroidResources = true` |
| Quick run command | `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:testDevDebugUnitTest --tests '*<TestClass>' ktlintCheck` |
| Full suite command | `JAVA_HOME="…" ./gradlew :app:testDevDebugUnitTest ktlintCheck lintDevDebug && bash scripts/check-design-rules.sh` |

Existing seams to reuse: `FakeClickMeApi` (`var …Handler`, call counters; add the six chat handlers), `FakeRealtimeConnection` (has `emit`, `setState`; **its `emitTypingStart/Stop/emitMessageRead` are `Unit` no-ops, so Wave 0 must make them record calls**), `MainDispatcherRule`, `FakeCallStarter`, `ExpertLaunchRequest`/`OneShotSlot` tests (`ExpertLaunchRequestTest.kt`).

### Phase Requirements → Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| CHAT-01 | `GET /chats` decodes (incl. null `last_message`, absent `blocked_by_me`); list sorts newest first; search filters name/last message case-insensitively; realtime `MessageNew` moves row to top and updates preview; `UserStatus` toggles presence; local unread increments except for the open thread; quiet reload on `Connected` transition; skeleton/empty/error/no-results states | unit + Robolectric | `… --tests '*ChatsRepositoryTest' --tests '*ConversationsViewModelTest' --tests '*ConversationsScreenTest' ktlintCheck` | ❌ Wave 0 |
| CHAT-02 | Page size 30; page 1 ascending; `loadEarlier` prepends, merges duplicates by id, silent failure stays retryable; `hasMore` from pagination; system-event mapping (7 event types, legacy `content`-only row, null enrichment, booking id extraction, terminal vs active routing); null-sender text row is not an event | unit (pure) + Robolectric | `… --tests '*ThreadReducerTest' --tests '*ChatMessageMappingTest' --tests '*ChatThreadViewModelTest' --tests '*ChatThreadScreenTest' ktlintCheck` | ❌ Wave 0 |
| CHAT-03 | Optimistic bubble immediately; ack replaces by `client_msg_id`; no duplicate on `MessageNew` with same server id; own `MessageNew` ignored (case-insensitive); network/5xx → Failed; retry reuses same `client_msg_id`; Delete removes; lost-response then reload converges to one Sent row; trimmed text; 10000 cap truncates paste; concurrent sends | unit (pure + VM) | `… --tests '*ThreadReducerTest' --tests '*ChatThreadViewModelSendTest' ktlintCheck` | ❌ Wave 0 |
| CHAT-04 | `typing.start` once on first non-empty edit, `typing.stop` after 3 s idle / on empty / on send (virtual time); incoming typing auto-clears at 6 s, on stop, on peer message; other thread's and own events ignored; `message.read` emitted for unread peer rows on load, on each peer `MessageNew`, and on reconnect; incoming `MessageRead` flips mine to Read | unit (VM + recording `FakeRealtimeConnection`) | `… --tests '*ChatThreadViewModelRealtimeTest' --tests '*RealtimeEventDecoderTest' ktlintCheck` | ❌ Wave 0 (decoder test file exists for realtime; extend) |
| CHAT-05 | `ChatLauncher`: one in-flight, success posts key, 404 → unavailable error, `CANNOT_CHAT_WITH_SELF` handled; shell consumes slot → `Navigator.openChatThread` selects Chats, resets to root, reuses equal top; every Message button disabled while busy / null peer id; in-call control disabled with null peer id, minimizes call on success; `ChatThreadKey` is Nav3-serializable | unit + Robolectric | `… --tests '*ChatLauncherTest' --tests '*NavigatorOpenChatThreadTest' --tests '*MessageButtonTest' --tests '*MeetingCallActionsTest' --tests '*MeetingCallScreenTest' --tests '*ChatKeysSerializationTest' ktlintCheck` | ❌ Wave 0 (`MeetingCallActionsTest` and `MeetingCallScreenTest` exist under `screens/call/calling`; extend them for the enabled Chat control) |
| SAFE-01 | Report sends enum wire value (never a label) from profile (`POST /users/{id}/report`) and chat (`action:"report"`); details trimmed, omitted when blank, ≤1000; Submit disabled until reason; 201 → submitted state/snackbar; 429/404/other mapping; request JSON contains no `null` keys (mockwebserver body assertion) | unit + Robolectric + mockwebserver | `… --tests '*UserReportReasonTest' --tests '*ReportUserSheetTest' --tests '*ReportAndBlockViewModelTest' --tests '*ChatRequestSerializationTest' ktlintCheck` | ❌ Wave 0 |
| SAFE-02 | Block from chat (confirm → `block` → pop 2); Block from profile (`initiate` then `block`, repeat taps ignored, stays on profile, snackbar); Block/Report hidden on own profile and self-thread | unit + Robolectric | `… --tests '*ReportAndBlockViewModelTest' --tests '*BlockUserDialogTest' --tests '*ExpertProfileViewModelTest' --tests '*ExpertProfileScreenTest' ktlintCheck` | ❌ Wave 0 (profile tests exist; extend) |
| SAFE-03 | §5 matrix: banner copy and composer disabled for each of the three states; Unblock only when `blocked_by_me`; §6 local state per outcome incl. `NOT_BLOCKER` latch; scan not-found leaves state unchanged; legacy block (both false) shows "You can't reply" | unit (pure `BlockState`) + Robolectric | `… --tests '*BlockStateTest' --tests '*ComposerBlockedBannerTest' --tests '*ReportAndBlockScreenTest' ktlintCheck` | ❌ Wave 0 |
| SAFE-04 | `THREAD_BLOCKED` / `RECIPIENT_UNAVAILABLE` on send restore text and show the right banner without a dialog; `NOT_BLOCKER` shows the server message; `CANNOT_CHAT_WITH_SELF` parses in `ErrorCode`; other 4xx shows `AuthErrorDialog` | unit + Robolectric | `… --tests '*ChatErrorsTest' --tests '*ChatThreadViewModelSendTest' --tests '*ApiErrorParserTest' ktlintCheck` | ❌ Wave 0 (`ApiErrorParserTest` exists under `network/`; extend it with the new code) |

### Sampling Rate
- **Per task commit:** the targeted `--tests '…' ktlintCheck` command for the files touched (< 60 s typical).
- **Per wave merge:** `./gradlew :app:testDevDebugUnitTest ktlintCheck` plus `bash scripts/check-design-rules.sh` (no color literals, no `*Brand` types).
- **Phase gate:** full suite + `lintDevDebug` (Pitfall 11) green before `/gsd-verify-work`. Never run `gradlew --stop` in parallel executors (memory: build-env-gotchas).

### Wave 0 Gaps
- [ ] `testing/FakeClickMeApi.kt` — add handlers/counters for `initiateChat`, `getChats`, `getChatMessages`, `sendChatMessage`, `chatAction`, `reportUser`
- [ ] `testing/FakeRealtimeConnection.kt` — record `emitTypingStart/Stop` and `emitMessageRead` (currently no-ops)
- [ ] `testing/ChatFixtures.kt` — builders for `ChatThreadItem`, `ChatMessageItem`, event rows (mirrors existing `bookingRequestDetail(...)` fixtures)
- [ ] New test files listed in the map above (all `app/src/test/java/com/uptrendinvestments/clickmeapp/...`)
- [ ] Update the `ErrorCode` KDoc ("same 65 cases, same order") when adding `CANNOT_CHAT_WITH_SELF`; check `ApiErrorParserTest` for any count/order assertion
- No framework install needed.

**Manual (deferred per project preference, log as open items):** two-way chat with an iOS client on the Samsung SM-A075F, typing/read indicators, block/unblock across both clients, a send into a blocked thread from the other client, 10/hour report limit, 200% font scale checks, jump-to-latest feel, list move-to-top animation.

## Security Domain

`security_enforcement` is not set to `false` in `.planning/config.json`, so this section applies.

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | no (reuses Phase 2 bearer + refresh; socket token via existing `RealtimeService`) | existing `AuthHeaderInterceptor` / `TokenAuthenticator` |
| V3 Session Management | yes (logout must not leak chat) | keep message state in entry-scoped VMs; stateless singletons; `SessionDataCleaner` for any in-memory set (A4) |
| V4 Access Control | yes | server-enforced (participant filter, `NOT_BLOCKER`, `CANNOT_*_SELF`); client hides controls but never relies on hiding |
| V5 Input Validation | yes | client caps content 10000 and details 1000, trims; server `.strict()` zod is the authority; send only schema keys |
| V6 Cryptography | no | TLS via existing OkHttp; nothing hand-rolled |
| V7/V8 Error handling, data protection | yes | no chat text or tokens in logs (`toString()` redaction); user-facing messages from `ApiException.userMessage` |

### Known Threat Patterns for this stack

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Chat content in logcat / crash text | Information disclosure | Redacting `toString()` on DTOs and events; no `Log` of bodies; `ApiException` message carries status/code only (T-02-08) |
| Cross-account leak via singleton cache | Information disclosure | No message cache in singletons; any session set cleared by `SessionDataCleaner` |
| Mass assignment / unknown keys | Tampering | Request DTOs contain only schema keys; omit nulls (zod `.optional()` rejects `null`) |
| IDOR on thread id | Elevation of privilege | Server filter `client_id/expert_id = caller` returns 404; client shows "conversation unavailable" |
| Report spam / double submit | DoS / Repudiation | In-flight lock + server 10/hour limiter (`429`); disable controls while in flight |
| Unsafe rendering of message text | Tampering | Plain `Text`; no link detection, no HTML (UI-SPEC "links are not detected") |
| Stale `blocked_by_me` leading to wrong Unblock | Tampering | Treat 403 `NOT_BLOCKER` as authoritative and latch off (§6) |

## Project Constraints (from CLAUDE.md)

- Port from iOS; Swift in `ios-reference/` is the behaviour source of truth; deviate only for Android conventions, and backend specs in `backend-reference/docs/ios/` override iOS (`CHAT-UNBLOCK.md`, `USER-REPORTING.md`).
- Stack: Jetpack Compose + Material 3, MVVM with Compose State, Kotlin coroutines (no RxJava), Hilt, Retrofit + Moshi, EncryptedSharedPreferences for JWT, Agora, Stripe, Firebase Messaging; minSdk 26, targetSdk/compileSdk 37.
- Platform swaps: `.sheet` → `ModalBottomSheet`; back chevron → `TopAppBar` with back arrow; `.alert` → Material `AlertDialog`; `.task` → `LaunchedEffect(Unit)`.
- File organization mirrors the iOS folder tree under `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/…` with `*Screen.kt` + `*ViewModel.kt`; package `com.uptrendinvestments.clickmeapp`.
- Same REST contract as iOS; no emojis in output; no hard-coded strings (UI-SPEC: new `strings_chat_*.xml`, `strings_report_user.xml`); no hex literals / `fontSize` / `*Brand` types (`scripts/check-design-rules.sh`).
- Memory notes: worktree executors need `local.properties` and absolute iOS paths; never `gradlew --stop` in parallel executors; manual device tests are deferred, log them as open items; commits may need a gate-plan branch when main is protected.

## Sources

### Primary (HIGH confidence)
- `backend-reference/docs/ios/CHAT-UNBLOCK.md`, `USER-REPORTING.md`, `backend-reference/docs/SOCKET-CONTRACT.md` (read in full)
- `backend-reference/src/controllers/chats.controller.js`, `src/routes/chats.js`, `src/routes/userReports.js`, `src/schemas/chat.schema.js`, `src/schemas/moderation.schema.js`, `src/middleware/rateLimiter.js` (reportLimiter), `src/services/socket.js`, `src/services/events.js`, `src/utils/displayInfo.js`, `supabase/migrations/20260609010000_create_messages.sql`
- iOS: `ClickMeAPI+Chats.swift`, `ChatThreadItem.swift`, `ChatMessageItem.swift`, `ChatSendAckData.swift`, `ChattingViewModel.swift`, `ChatConversationsViewModel.swift`, `ReportChatViewModel.swift`, `HomeExpertView.swift` (`openChatThread`), enums `MessageStatus/MessageEventType/MessageType`
- Android in-repo (read this session): `ErrorCode.kt`, `ApiException.kt`, `ApiCall.kt`, `ApiRequestUrls.kt`, `ClickMeApi.kt`, `MoshiProvider.kt`, `Navigator.kt`, `NavigationState.kt`, `HomeClientScreen.kt`, `HomeExpertScreen.kt`, `HomeClientTab.kt`, `HomeExpertTab.kt`, `RealtimeEvents.kt`, `RealtimeEmits.kt`, `RealtimeConnection.kt`, `RealtimeService.kt`, `RealtimeConnectionManager.kt`, `ExpertLaunchRequest.kt`, `MeetingCallControls.kt`, `MeetingCallUiState.kt`, `CallOverlayHost.kt`, `FakeRealtimeConnection.kt`, `FakeClickMeApi.kt`, `gradle/libs.versions.toml`, `scripts/check-design-rules.sh`
- Planning: `12-CONTEXT.md`, `12-UI-SPEC.md`, `REQUIREMENTS.md`, `STATE.md`

### Secondary (MEDIUM confidence)
- developer.android.com/develop/ui/compose/lists (keys, `animateItem`, `animateScrollToItem`) [CITED]
- developer.android.com/develop/ui/compose/components/pull-to-refresh (`PullToRefreshBox` signature) [CITED]

### Tertiary (LOW confidence)
- `LazyColumn(reverseLayout = true)` semantics (training knowledge, `[ASSUMED]` A2)

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH, no new dependencies; versions read from the version catalog.
- Backend contract: HIGH, read from controller/schema source rather than docs only.
- Architecture: MEDIUM-HIGH, patterns copy existing in-repo precedents; `reverseLayout` and the stack-reset behaviour are the assumed pieces.
- Pitfalls: HIGH for 1-5 and 7-9 (read from code), MEDIUM for 10-11 (from STATE/KDoc notes).

**Research date:** 2026-10-27
**Valid until:** 2026-11-26 (backend is under active change; re-check `chats.controller.js` for an unread-count fix and empty-thread listing before planning if more than two weeks pass)

## User Resolutions (2026-10-27, during /gsd-plan-phase 12)

These were confirmed by the user after research. They are binding for planning.

- **A1 / Open Question 1 (unread badge): session-local count.** Row badge = `max(server unread_count, session-local count)` behind one `displayUnread` function, so it can switch to the server value once the backend is fixed. Log the backend gap (unread_count never incremented, not reset by `message.read`) in STATE "Backend notes".
- **A4 / Open Question 2 (empty-thread block state): client fallback.** On `THREAD_BLOCKED`, mark the thread blocked even when it is absent from `GET /chats`, and keep a session-local in-memory "blocked by me" set. Log a backend request (list empty threads or add `GET /chats/:id`) in STATE "Backend notes".
- **A6 / Open Question 4 (`booking.update` in an open thread): yes.** The thread ViewModel collects `BookingUpdate` and quiet-reloads page 1, so booking system events appear live.
- Not asked; planner's call per research recommendation: A3 (scan `limit=100`), A5 (reset Chats stack to root before pushing a thread), Open Question 3 (`ChatThreadKey.peerIsExpert: Boolean?`, null hides "View profile").
