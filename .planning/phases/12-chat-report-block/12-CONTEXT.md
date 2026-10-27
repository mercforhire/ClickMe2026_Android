# Phase 12: Chat, Report & Block - Context

**Gathered:** 2026-10-27
**Status:** Ready for planning

<domain>
## Phase Boundary

Clients and experts can message each other in real time (text only), and either side can report, block and unblock safely. This phase delivers:
- The Conversations list (the Chats tab root, both modes)
- The chat thread: pages of 30, newest first, `loadEarlier`; inline booking system events; optimistic send reconciled by `client_msg_id`; typing and read indicators over realtime
- Chat entry points (`initiateChat`) from Expert Profile, bookings, requests and the in-call Chat control
- Report (six reason enum values) and Block/Unblock from chat and Expert Profile, with the CHAT-UNBLOCK blocked-thread states and the `THREAD_BLOCKED` / `RECIPIENT_UNAVAILABLE` / `NOT_BLOCKER` messages

Requirements: CHAT-01..05, SAFE-01..04. Images/attachments, push and the notifications list are out of scope (Phase 13).

</domain>

<decisions>
## Implementation Decisions

Carried forward (not re-opened):
- iOS is the source of truth except where a backend spec says otherwise. Here CHAT-UNBLOCK.md and USER-REPORTING.md override iOS: iOS sends display labels and has no blocked state.
- Report reasons are the six backend enum values with the spec's suggested labels: `harassment` "Harassment or abuse", `spam` "Spam", `inappropriate_content` "Inappropriate content", `scam_or_fraud` "Scam or fraud", `impersonation` "Impersonation", `other` "Something else". iOS's `defaultReasons` labels ("Fake profile", "Hate speech", …) are NOT ported.
- The blocked-thread UI follows the CHAT-UNBLOCK.md §5 matrix exactly. Local state after each outcome follows §6. `blocked_by_me` decodes with a default of `false`.
- API pattern: `ApiRequestUrls` constants, `ClickMeApi` methods, Hilt `@Singleton` repository using `apiCall {}`, Moshi codegen DTOs with explicit `@Json(name=…)`. `chatAction` returns a status-only envelope.
- Error copy: the server `message` in a Material `AlertDialog`.
- Refresh triggers follow Phase 8 D-10: a quiet reload when the tab is shown again, plus pull-to-refresh.
- Phase 9 D-11 hand-off: enable the in-call Chat control (`// PHASE-12` in `MeetingCallControls.kt`). It calls `initiateChat`, minimizes the call, switches to Chats and pushes the thread.
- Hide Message / Report / Block when targeting yourself (`CANNOT_CHAT_WITH_SELF`, `CANNOT_REPORT_SELF`, `CANNOT_BLOCK_SELF`). On a self-thread (partner id == own id), hide Report and Block.

### Send behaviour (CHAT-03, SAFE-04)
- **D-01: Optimistic send with a failed state.** The bubble appears immediately as "sending". On a network error or 5xx it stays in place, dimmed, with a red "!" and "Not sent. Tap to retry". A long-press offers Delete, which removes it locally. Retry reuses the **same `client_msg_id`** so the server dedupes. This deviates from iOS, which waits for the ack before showing the bubble.
- **D-02: No offline queue.** A send always tries right away. If it fails, the message goes to the D-01 failed state. Nothing persists across process death or app restart.
- **D-03: 403 `THREAD_BLOCKED` / `RECIPIENT_UNAVAILABLE` on send:**
  - Remove the optimistic bubble and put its text back in the composer.
  - Refetch `GET /chats` for this thread's block state (CHAT-UNBLOCK §6).
  - Switch the composer to its disabled banner state: "You can't reply to this conversation" / "You blocked this conversation" for blocked threads, "This user is unavailable." for `RECIPIENT_UNAVAILABLE`.
- **D-04: The composer caps input at the backend send-schema maximum.** The researcher looks this up in `backend-reference`. Show a character counter near the limit. iOS has no cap.
- Reconciliation: the send ack (`ChatSendAckData`: message id, status, timestamp) replaces the optimistic bubble by `client_msg_id`. A `message.new` echo or page load with the same server id or `client_msg_id` must never create a duplicate.

### Report & Block surfaces (SAFE-01..04)
- **D-05: Block on Expert Profile is a confirm dialog, not the combined screen.**
  - The ⋯ menu "Block {name}" opens an `AlertDialog`: "Block {name}? They won't be able to message you."
  - On confirm, call `initiateChat(peerId)` and then `chatAction(threadId, "block")`, with a progress indicator and repeat taps ignored.
  - On success, stay on the profile and show a snackbar.
  - This deviates from iOS, which bootstraps the thread and then opens the full Report & Block screen.
- **D-06: Report on Expert Profile** goes in a `ModalBottomSheet` (port of `ReportUserSheet`). It has the reason picker (required), optional details (max 1000, trimmed) and `POST /users/{id}/report`.
- **D-07: The profile ⋯ menu always shows "Block {name}".** There is no block-state lookup. Re-blocking is a 200 no-op. Unblocking happens in the chat.
- **D-08: Inside a chat thread, port the iOS combined "Report & Block" screen** (`ReportChatView`) as a full-screen destination pushed from the thread's top bar ⋯.
  - The Report card sends `chatAction(threadId, "report", reason, details)`.
  - The Block/Unblock card follows the §5 matrix: Block when not blocked, Unblock only when `blocked_by_me`, and no Unblock when blocked by the peer.
  - Block asks for confirmation first, as iOS does.
  - On `403 NOT_BLOCKER`, apply §6 and latch Unblock off (iOS `unblockDenied`).
  - Block state comes from scanning `GET /chats` for the thread (there is no single-thread endpoint). It fails silent and defaults to showing Block.
- **D-09: A successful block from chat exits the thread.** Close the Report & Block screen and pop the thread, returning to the Conversations list (iOS-like). Unblock success returns to the thread, which is now unblocked.
- **D-10: A successful report shows "Thanks, we'll review this."** The profile sheet closes with a snackbar. In the combined screen the Report card switches to its submitted state and the Block card stays usable.
  - `429 TOO_MANY_REQUESTS` → "Please try again later."
  - `404` → neutral "This user is unavailable."
- **D-11: Experts report and block clients only from chat.** No Report/Block on Request Decision or the expert Booking Summary. Client Profile is DEF-3.
- **D-12: Message always opens the thread, even when it's blocked.** The blocked banner and Unblock live there. Entry points don't check block state.

### Chat entry & navigation (CHAT-05)
- **D-13: Every Message tap switches to the Chats tab and pushes the thread there**, using the shell "open chat thread" navigator hook noted in `Navigator.kt`. The original tab keeps its back stack. Back from the thread goes to the Conversations list. This matches iOS `openChatThread` and the in-call D-11 rule.
- **D-14: Message buttons appear on:**
  - Expert Profile, next to Book Session (client mode, not on your own profile)
  - Client Upcoming Booking Details and client Booking Summary (un-hides Phase 8's hidden Message Expert)
  - Client My Bookings cards (un-hides the Phase 8 hand-off)
  - Expert Request Decision footer and expert Booking Summary (Phase 11 hand-off)
  - Expert Upcoming/Past booking cards, to match the client cards
  - The in-call Chat control
- **D-15: While `initiateChat` is in flight, the Message button shows a spinner and ignores repeat taps.** On error, an `AlertDialog` shows the server message. A `404` → "This user is unavailable."

### Conversations list (CHAT-01)
- **D-16: No unread badge on the Chats tab icon** (iOS parity). Each row keeps its unread count badge.
- **D-17: Loading (iOS):**
  - `GET /chats?page=1&limit=50` only, with no paging.
  - Realtime `message.new` moves the thread to the top and updates its preview and time. `user.status` updates presence.
  - Pull-to-refresh, plus a quiet reload when the tab is shown again (and an iOS-style silent reload where iOS has one).
- **D-18: Keep the iOS search bar.** It filters the loaded threads locally by name or last message, case-insensitively.
- **D-19: Blocked threads look like any other row in the list** (iOS parity). The blocked state appears only inside the thread.

### Ported as-is from iOS (no discussion needed)
- Thread: page size 30, `loadEarlier` prepends older pages (fails silently and stays retryable), booking system-event rows (`ChattingSystemEventRow`), an empty state, and the peer user id taken from the first peer message (never from admin broadcast rows).
- Typing:
  - Outgoing `typing.start` goes out on the first non-empty edit after a quiet period, and a debounced `typing.stop` follows 3 s of idle. A send also emits stop.
  - Incoming typing auto-clears after a timeout safety net.
  - Own events are ignored.
- Read receipts:
  - Emit `message.read` for visible unread peer messages on load and on each incoming `message.new` while the thread is open.
  - Incoming `message.read` flips my bubbles to read.
  - Own `message.new` echoes are deduped.

### Claude's Discretion
- Exact visual design of bubbles, failed state, banner, system-event rows and the Report & Block screen (Material 3, dark, Nunito, adapted from iOS; no pixel parity). The UI-SPEC phase may refine it.
- Where the Message button sits on each booking card and summary screen, and whether it's an icon or a text button.
- The relative time label format in the list (follow iOS `relativeTimeLabel` semantics with device locale).
- Whether the thread repository is per-thread or app-scoped, and how realtime events fan out to the list and the open thread.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Backend contracts (authoritative over iOS)
- `backend-reference/docs/ios/CHAT-UNBLOCK.md` — unblock action, `blocked_by_me`, the §5 UI matrix, §6 local state, `NOT_BLOCKER`/`THREAD_BLOCKED`, legacy blocks
- `backend-reference/docs/ios/USER-REPORTING.md` — `POST /users/{id}/report`, the six reason enum values and labels, chat report action, the self-targeting guards (§3), `RECIPIENT_UNAVAILABLE` / `404` on initiate (§5), the 10/hour report limit
- `backend-reference/` chat routes and validation schemas — send message max length (D-04), `client_msg_id` dedupe, the `GET /chats` / messages payload shape (researcher to locate)

### iOS reference
- `ios-reference/ClickMe2026/Network/ClickMeAPI+Chats.swift` — initiate, list, messages, send, actions
- `ios-reference/ClickMe2026/Views/Screens/Chat/Conversations/` — list VM (limit 50, search, realtime updates) and views
- `ios-reference/ClickMe2026/Views/Screens/Chat/Chatting/` — thread VM (page 30, typing, read, send, system events) and subviews
- `ios-reference/ClickMe2026/Views/Screens/Chat/Report and Block/` — the combined Report & Block screen and VM (block state sync, `unblockDenied`)
- `ios-reference/ClickMe2026/Views/Screens/Client Mode/Expert Profile/ExpertProfileView.swift` and `Subviews/ReportUserSheet.swift` — profile ⋯ menu, report sheet, Message button
- `ios-reference/ClickMe2026/Model/Responses/Children/ChatThreadItem.swift`, `ChatMessageItem.swift`, `ChatSendAckData.swift`; `Model/UIModels/ChatDisplayModels.swift`; `Model/Enums/UserReportReason.swift`; `Model/Requests/ReportUserRequest.swift`

### Planning
- `.planning/REQUIREMENTS.md` — CHAT-01..05, SAFE-01..04
- `.planning/ROADMAP.md` — Phase 12 success criteria and planning notes
- `.planning/STATE.md` — Phase 8, 9 and 11 hand-offs to Phase 12

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `realtime/RealtimeEvents.kt` — `MessageNew`, `MessageRead`, `UserStatus`, `TypingStart`, `TypingStop` already exist. `RealtimeEmits.kt` / `RealtimeService.kt` handle outgoing emits, and `RealtimeEventDecoder.kt` decodes incoming events (Phase 3 RT-01..03, event `SharedFlow`).
- `navigation/Navigator.kt` — already has a note (around line 119) about the iOS "message jumps to Chats" hook that Phase 12 entry points use.
- `managers/CallGuard.kt` and `CallStarter` — the call is app-scoped. The in-call Chat control minimizes it to the pill.
- `model/enums/ErrorCode.kt` — add `THREAD_BLOCKED`, `RECIPIENT_UNAVAILABLE`, `NOT_BLOCKER`, `CANNOT_*_SELF`, `TOO_MANY_REQUESTS` if missing.
- Shared components from Phase 1/5: avatar, press-scale button, async save button, validated text field, skeletons, `SectionError`.

### Established Patterns
- Hilt `@Singleton` repositories with `apiCall {}`, and ViewModels exposing Compose State.
- Per-tab back stacks in the client and expert 5-tab shells. The Chats tab exists in both shells as a placeholder root.
- Quiet reload when the tab is shown again, plus pull-to-refresh (Phase 8 D-10).

### Integration Points
- `screens/call/calling/subviews/MeetingCallControls.kt:183` — `// PHASE-12` disabled Chat control
- `screens/clientmode/expertprofile/ExpertProfileScreen.kt` — Message button, ⋯ Report/Block menu
- `screens/clientmode/bookingsummary/BookingSummaryScreen.kt`, client Upcoming Booking Details, My Bookings cards — un-hide Message
- `screens/expertmode/requestdecision/RequestDecisionScreen.kt`, `screens/expertmode/bookingsummary/ExpertBookingSummaryScreen.kt`, expert booking cards — Message
- New: `screens/chat/conversations/`, `screens/chat/chatting/`, `screens/chat/reportandblock/` (mirroring the iOS `Views/Screens/Chat/` tree)

</code_context>

<specifics>
## Specific Ideas

- Failed-bubble copy: "Not sent. Tap to retry".
- Profile block dialog: "Block {name}? They won't be able to message you."
- Report success: "Thanks, we'll review this." 429: "Please try again later." Unavailable: "This user is unavailable."
- Report back to the iOS side (not Android work): iOS send waits for the ack (no optimistic bubble). iOS report reasons are labels that don't match the enum (already logged).

</specifics>

<deferred>
## Deferred Ideas

- Unread badge on the Chats tab icon (rejected for MVP, iOS parity). Revisit after release.
- Paging the conversations list past 50 threads.
- A persistent offline send queue.
- A "Blocked" label on list rows.
- Report/Block from Client Profile on the expert side (Client Profile is DEF-3).
- Image attachments in chat (text only in the MVP).

</deferred>

---

*Phase: 12-chat-report-block*
*Context gathered: 2026-10-27*
