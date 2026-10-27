---
phase: "12"
slug: "chat-report-block"
status: verified
# threats_open = count of OPEN threats at or above workflow.security_block_on severity (the blocking gate)
threats_open: 0
asvs_level: 1
created: "2026-10-27"
---

# Phase 12 — Security

> Per-phase security contract: threat register, accepted risks, and audit trail.

---

## Trust Boundaries

| Boundary | Description | Data Crossing |
|----------|-------------|---------------|
| app -> backend | Message text, report reasons and details leave the device over the authenticated API | user content (sensitive) |
| backend -> app | Thread lists, message rows and free-form `event_data` are decoded from the server | server payloads (untrusted input) |
| realtime socket -> app | Typing, read and message events for any thread | events (spoofable thread/sender ids) |

---

## Threat Register

Main paths are relative to `app/src/main/java/com/uptrendinvestments/clickmeapp/`.

| Threat ID | Category | Component | Severity | Disposition | Mitigation | Status |
|-----------|----------|-----------|----------|-------------|------------|--------|
| T-12-01 | Information disclosure | Chat DTO / request toString | medium | mitigate | Redacting `toString()` in model/requests/ChatRequests.kt, model/responses/ChatPayloads.kt; `toStringNeverPrintsUserText` | closed |
| T-12-02 | Tampering | Request bodies vs `.strict()` zod | low | mitigate | Schema-only keys; `sendPostsOnlySchemaKeys` and wire tests in ChatsApiTest | closed |
| T-12-03 | Elevation of privilege | Thread id in paths (IDOR) | medium | accept | Server filters by participant (404) | closed |
| T-12-04 | Denial of service | Report spam | low | accept | Server `reportLimiter` 10/hour (429) | closed |
| T-12-05 | Information disclosure | ChatSessionState across logout/login | medium | mitigate | `@IntoSet SessionDataCleaner` (di/ChatModule.kt); `clearSessionDataResetsEverything` | closed |
| T-12-06 | Tampering | Stale `blocked_by_me` | medium | mitigate | `onNotBlocker` latch in BlockState.kt; `scanNeverReEnablesUnblockWhileStillBlocked` | closed |
| T-12-07 | Spoofing | "Mine" detection by sender id | low | accept | UI-only; server authorizes | closed |
| T-12-08 | Denial of service | Duplicate nav keys across stacks | medium | mitigate | `StackOrigin.Chats` (screens/chat/ChatEntries.kt); `chatsOriginKeyDiffersFromDefault` | closed |
| T-12-09 | Tampering | ChatLaunchRequest slots | low | accept | App-internal singleton | closed |
| T-12-10 | Information disclosure | Peer name/avatar in saved nav keys | low | accept | Ids and display seeds only | closed |
| T-12-11 | Denial of service | Repeated initiate taps | low | mitigate | ChatLauncher single flight + ChatEntryViewModel busy guard; tests assert one request | closed |
| T-12-12 | Information disclosure | Entry error dialog | low | mitigate | `toChatEntryError()` shows resource or `userMessage` only; `errorsMapToEntryCopy` | closed |
| T-12-13 | Elevation of privilege | Self-chat | low | accept | Control hidden; server 422 | closed |
| T-12-14 | Information disclosure | Previews in logs | low | mitigate | No logging in chat roots; preview redacted in `toString` | closed |
| T-12-15 | Denial of service | Event bursts for unknown threads | low | mitigate | `startLoad` cancels in-flight load (ConversationsViewModel.kt:215) | closed |
| T-12-16 | Tampering | Preview rendering | low | accept | Plain `Text` | closed |
| T-12-17 | Information disclosure | Read receipts from hidden thread | medium | mitigate | `screenShown && Connected` guard (ChatThreadViewModel.kt:392); `noReadReceiptsWhileHidden` | closed |
| T-12-18 | Denial of service | Receipt/reload storms | low | mitigate | `emittedReadIds`, latest-wins load; `peerMessagesAlreadyReadAreNotReEmitted` | closed |
| T-12-19 | Spoofing | Events for other threads / own id | low | mitigate | Thread and own-sender filters; `typingEventsForOtherThreadsOrFromMeAreIgnored` | closed |
| T-12-20 | Denial of service | Report double submit | low | mitigate | `reportSubmitting` guard, 429 copy; `submitWhileSubmittingIsIgnored` | closed |
| T-12-21 | Tampering | Stale Unblock for blocked side | high | mitigate | `unblock()` gated on `showsUnblock`, NOT_BLOCKER latch; `notBlockerLatchesUnblockOff`, `unblockIsIgnoredWhenTheCallerIsNotTheBlocker` | closed |
| T-12-22 | Information disclosure | Report details | medium | mitigate | No logging; request `toString` omits details | closed |
| T-12-23 | Elevation of privilege | Self-thread block/report | low | accept | `isSelfThread` guards; server 422 | closed |
| T-12-24 | Tampering | Duplicate messages on retry | low | mitigate | Retry reuses `client_msg_id`; `retryReusesSameIdAndSucceeds` | closed |
| T-12-25 | Information disclosure | Unsent message text | low | accept | Memory only, never logged | closed |
| T-12-26 | Elevation of privilege | Sending into a blocked thread | low | accept | Server THREAD_BLOCKED; client mirrors | closed |
| T-12-27 | Tampering | Message text rendering | low | mitigate | Plain `Text` in `SelectionContainer`; no link/HTML APIs | closed |
| T-12-28 | Elevation of privilege | View profile from thread | low | accept | Read-only public profile | closed |
| T-12-29 | Denial of service | Very long messages | low | accept | Bubble capped; server max 10000 | closed |
| T-12-30 | Denial of service | Report spam from profile | low | mitigate | ProfileSafetyViewModel in-flight guards; `repeatConfirmWhileInFlightIgnored`, `report429KeepsInputsAndSheet` | closed |
| T-12-31 | Tampering | Block via initiate creates empty thread | low | accept | Initiate idempotent; session mark | closed |
| T-12-32 | Information disclosure | Report details (profile) | medium | mitigate | No logging; `ReportUserRequest.toString` redacts | closed |
| T-12-33 | Spoofing | Peer id from booking DTOs | low | accept | Server-issued ids | closed |
| T-12-34 | Denial of service | Repeated Message taps on cards | low | mitigate | Shared `ChatEntryViewModel.open` + `MessageButton` guard; `busyCardShowsSpinnerOnThatCardOnly` | closed |
| T-12-35 | Spoofing | Peer id from request/booking DTOs | low | accept | Server-issued ids | closed |
| T-12-36 | Denial of service | Repeated Message taps (expert) | low | mitigate | Shared guard; `footerMessageIsDisabledWhileADecisionIsInFlight` | closed |
| T-12-37 | Denial of service | Chat taps during a call | low | mitigate | CallOverlayHost via `chatEntry.open`, minimize on Opened only; `chatBusyShowsSpinnerAndIgnoresTaps` | closed |
| T-12-38 | Spoofing | Peer id from call session | low | accept | Server booking id | closed |
| T-12-39 | Information disclosure | Composer draft in SavedStateHandle | low | accept | User's own draft, never logged | closed |
| T-12-40 | Tampering | Server error text in send dialog | low | accept | Plain text via AuthErrorDialog | closed |
| T-12-41 | Denial of service | Huge pastes | low | mitigate | 10,000 cap in VM and field; `composerCapTruncatesPaste` | closed |
| T-12-42 | Information disclosure | Chat text in logs (phase-wide) | medium | mitigate | `Log.`/Timber/println sweep over all Phase 12 files: 0 hits; no body logging interceptor | closed |
| T-12-43 | Denial of service | R8 stripping keys/adapters | medium | mitigate | R8 on; generated Moshi keep rules for Phase 12 DTOs; kotlinx-serialization rules for nav keys; `assembleProdRelease` green (12-15). Device smoke deferred (Phase 2 keystore) | closed |
| T-12-44 | Information disclosure | Kept unsent text | medium | mitigate | Owner-keyed, take-once, cleared on logout, memory only; `unsentTextIsPerOwnerAndThreadAndTakenOnce` | closed |
| T-12-45 | Denial of service | LazyColumn key collisions | medium | mitigate | `c:`/`s:` key scheme (ThreadItem.kt:76-80); `keysAreUniqueInAMixedThread` | closed |
| T-12-46 | Repudiation | Send completing after screen closed | low | accept | Server dedupes by client_msg_id | closed |
| T-12-47 | Tampering | Local block state after unblock | low | mitigate | `takeNoticeIfForThisThread` + scan restart; `staleScanInFlightCannotRelockAfterUnblock` | closed |
| T-12-48 | Repudiation | Profile report/block confirmation | low | mitigate | Snackbar shown then consumed once; ExpertProfileScreenTest | closed |
| T-12-49 | Information disclosure | Profile snackbar copy | low | accept | Display name or resource fallback only | closed |
| T-12-SC (x17) | Tampering | Package installs | low | accept | No dependency changes in Phase 12 (git log of version catalog/build files) | closed |

*Status: open · closed · open — below high threshold (non-blocking)*
*Severity: critical > high > medium > low — only open threats at or above workflow.security_block_on count toward threats_open*
*Disposition: mitigate (implementation required) · accept (documented risk) · transfer (third-party)*

---

## Accepted Risks Log

| Risk ID | Threat Ref | Rationale | Accepted By | Date |
|---------|------------|-----------|-------------|------|
| AR-12-01 | T-12-03 | IDOR on thread id; server filters by participant (404) | plan 12-01 | 2026-10-27 |
| AR-12-02 | T-12-04 | Report spam; server limiter 10/hour (429) | plan 12-01 | 2026-10-27 |
| AR-12-03 | T-12-07 | "Mine" detection is UI-only; server authorizes | plan 12-02 | 2026-10-27 |
| AR-12-04 | T-12-09, T-12-10 | App-internal launch slots; nav keys hold display seeds only | plan 12-03 | 2026-10-27 |
| AR-12-05 | T-12-13, T-12-23 | Self-chat/block/report; controls hidden, server 422 | plans 12-04, 12-07 | 2026-10-27 |
| AR-12-06 | T-12-16 | Preview rendered as plain Text | plan 12-05 | 2026-10-27 |
| AR-12-07 | T-12-25, T-12-39 | Unsent text and draft are the user's own, memory/saved-state only, never logged | plans 12-08, 12-14 | 2026-10-27 |
| AR-12-08 | T-12-26 | Server enforces THREAD_BLOCKED | plan 12-08 | 2026-10-27 |
| AR-12-09 | T-12-28, T-12-29 | Read-only public profile; long messages capped, server max 10000 | plan 12-09 | 2026-10-27 |
| AR-12-10 | T-12-31 | Initiate is idempotent; session mark covers empty thread | plan 12-10 | 2026-10-27 |
| AR-12-11 | T-12-33, T-12-35, T-12-38 | Peer ids are server-issued | plans 12-11, 12-12, 12-13 | 2026-10-27 |
| AR-12-12 | T-12-40, T-12-49 | Server error / snackbar copy shown as plain text | plans 12-14, 12-17 | 2026-10-27 |
| AR-12-13 | T-12-46 | Late/duplicate delivery of the same request; server dedupes | plan 12-16 | 2026-10-27 |
| AR-12-14 | T-12-SC | No package installs in any Phase 12 plan | plans 12-01..12-17 | 2026-10-27 |

*Accepted risks do not resurface in future audit runs.*

### Non-blocking notes (from audit; tied to open review items)

- **N1 (T-12-44 / GR-WR-03):** a send failing after logout's `clearSessionData()` can re-populate kept unsent text for the signed-out user (memory only, owner-keyed). Epoch fix proposed in 12-REVIEW, still open.
- **N2 (T-12-24 / T-12-46 / GR-WR-01, GR-WR-02):** post-close failure restores text without its `client_msg_id`, so a user re-send after an ambiguous failure can duplicate; a reopened thread is not told when an in-flight send succeeds. Low.
- **N3 (WR-04):** `ChatThreadUiState.composerText`, `ReportAndBlockUiState.details`, `ProfileSafetyState.details` keep default `toString`. No logging touches them; defence in depth only.
- **N4 (T-12-43):** the R8 `assembleProdRelease` pass is documentation-sourced (12-15-SUMMARY); local mapping outputs predate Phase 12. Re-run with the release build once the Phase 2 keystore exists.

---

## Security Audit Trail

| Audit Date | Threats Total | Closed | Open | Run By |
|------------|---------------|--------|------|--------|
| 2026-10-27 | 66 | 66 | 0 | gsd-security-auditor (ASVS L1, block_on high) |

---

## Sign-Off

- [x] All threats have a disposition (mitigate / accept / transfer)
- [x] Accepted risks documented in Accepted Risks Log
- [x] `threats_open: 0` confirmed
- [x] `status: verified` set in frontmatter

**Approval:** verified 2026-10-27
