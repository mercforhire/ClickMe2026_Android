---
phase: 12-chat-report-block
plan: 01
subsystem: network
tags: [network, retrofit, moshi, chat, report, wave-0]
status: complete

requires:
  - phase: 02-networking
    provides: apiCall, ApiException, MoshiProvider, SuccessDataResponse, PaginationMeta
  - phase: 11-expert-requests
    provides: BookingRequestsRepository / MockWebServer test harness style
provides:
  - six chat and reporting endpoints (Retrofit) with strict-schema request DTOs
  - ChatsRepository (stateless @Singleton)
  - UserReportReason (six backend wire values)
  - ErrorCode.CannotChatWithSelf (Android-only, appended after the 65 iOS cases)
  - Wave 0 test seams (FakeClickMeApi chat handlers, recording FakeRealtimeConnection, ChatFixtures)
affects: [12-02, 12-03, 12-04, 12-05, 12-06, 12-07, 12-08, 12-09, 12-10]

tech-stack:
  added: []
  patterns:
    - "Request DTOs carry only backend zod schema keys; nulls omitted by the app Moshi (never serializeNulls)"
    - "Chat DTO and request toString() redact user text (ids, types, statuses only)"

key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/model/requests/ChatRequests.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/ChatPayloads.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/model/enums/UserReportReason.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/chat/ChatsRepository.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/network/ChatsApiTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/model/enums/UserReportReasonTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/testing/ChatFixtures.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/chat/ChatsRepositoryTest.kt
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/network/ApiRequestUrls.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/network/ClickMeApi.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/model/enums/ErrorCode.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/network/ApiErrorParserTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/testing/FakeClickMeApi.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/testing/FakeRealtimeConnection.kt

key-decisions:
  - "initiate sends {peer_id} only; the deprecated expert_id is never sent (iOS still does)"
  - "Report reasons go out as the six backend enum wire values, not the iOS display labels"
  - "findThread scans page 1 with limit=100 (backend cap) and matches threadId case-insensitively; no single-thread endpoint exists"
  - "chatAction returns StatusOnlyResponse because /chats/{id}/actions answers {status:success} with no data"

patterns-established:
  - "Wire tests assert exact request key sets (parsed via Moshi) and absence of null in the body"
  - "Fake recorders expose immutable snapshot lists guarded by synchronized"

requirements-completed: [CHAT-01, CHAT-02, CHAT-03, CHAT-05, SAFE-01, SAFE-02, SAFE-04]

duration: ~35min
completed: 2026-10-27
commits: 3
plan_head_before: 087ac37bf779d10dc8c32cfa04081e33339d1d67
plan_head_after: d459d71db9a6ceb353485b124f1df5d7a94022fc
actuals:
  tokens: 21000
  tasks: 3
  commits: 3
---

# Phase 12 Plan 01: Chat and report network contract Summary

**Retrofit/Moshi contract for chat initiate, list, messages, send, block/unblock/report and profile report, with backend-schema-valid bodies, a six-value report reason enum, a stateless ChatsRepository and the shared Wave 0 test seams.**

## Performance

- Duration: about 35 minutes
- Tasks: 3 of 3
- Files: 8 created, 6 modified

## Accomplishments

- Six endpoint constants and six `ClickMeApi` methods (`initiateChat`, `getChats`, `getChatMessages`, `sendChatMessage`, `chatAction`, `reportUser`); none added to `publicPaths`.
- DTOs for threads, messages (nullable sender, free-form `event_data`), send ack, initiate, report and the status-only action reply; `blocked_by_me` defaults to false when absent.
- `ChatsRepository` with `LIST_LIMIT = 50`, `SCAN_LIMIT = 100`, `PAGE_SIZE = 30`; report details are trimmed and blank details omitted.
- `UserReportReason` (harassment, spam, inappropriate_content, scam_or_fraud, impersonation, other) and `ErrorCode.CannotChatWithSelf`.
- Redacting `toString()` on every type holding message text, previews or report details (T-12-01).
- Test seams for later plans: `FakeClickMeApi` handlers and recorders for all six endpoints, `FakeRealtimeConnection.typingStarts/typingStops/messageReads`, `ChatFixtures.kt`.

## Task Commits

1. Task 1 (tracer): `de3dc49` feat(12-01): chat initiate, list, messages and send through Retrofit and ChatsRepository
2. Task 2: `80211b0` feat(12-01): block, unblock and report bodies, six report reasons, CANNOT_CHAT_WITH_SELF
3. Task 3: `d459d71` test(12-01): chat fixtures, recording realtime emits and ChatsRepositoryTest

## Verification

- `ChatsApiTest` 10 tests, `ChatsRepositoryTest` 5, `UserReportReasonTest` 3, `ApiErrorParserTest` 10, `RealtimeConnectionManagerTest` 33 (unchanged): all green.
- `ktlintCheck` passes; `bash scripts/check-design-rules.sh` prints "Design rules: OK".
- Tracer feedback gate: the four tracer endpoints round-trip through Retrofit, Moshi and the repository in `ChatsApiTest` end to end before Task 2 expanded; no `blocking-human` gate on the task.
- Acceptance greps: `peer_id` present, `expert_id` absent in ChatRequests.kt; `"blocked_by_me"` present; `SCAN_LIMIT = 100`; `reason.wire` used; no `serializeNulls`; `CANNOT_CHAT_WITH_SELF` in ErrorCode.kt.

## Deviations from Plan

### TDD ordering

**1. [Process] Tests and implementation committed together per task, no separate RED commit**
- The test files name symbols (DTOs, repository, enum) that do not exist before the implementation, so a RED run is a compile failure (`build_failure`, INVALID_RED), not a meaningful assertion failure. The plan is `type: execute` (not `type: tdd`), so the plan-level RED/GREEN gate sequence does not apply. Each task commit contains its tests; all tests were run and pass.

### Auto-fixed Issues

**2. [Rule 3 - Blocking] FakeClickMeApi had to compile as each ClickMeApi method landed**
- **Found during:** Task 1 and Task 2
- **Issue:** `FakeClickMeApi` implements `ClickMeApi`, so new interface methods break test compilation until the fake overrides them. Plan assigned the handlers to Task 3.
- **Fix:** Added the real handlers and recorders in the same task commit as each method (four in Task 1, two in Task 2). Task 3 then only added the realtime recorders, fixtures and `ChatsRepositoryTest`. The final file set matches the plan.
- **Files modified:** `testing/FakeClickMeApi.kt`
- **Commits:** de3dc49, 80211b0

**3. [Rule 1 - Bug] Duplicate Phase 12 URL group**
- **Found during:** Task 1 compile (REDECLARATION)
- **Issue:** The URL constants were inserted twice into `ApiRequestUrls.kt` while editing.
- **Fix:** Removed the duplicate group before the first commit; no duplicates in the committed tree.

**Total deviations:** 3 (1 process note, 1 blocking, 1 self-inflicted bug fixed pre-commit). **Impact:** none on the delivered contract.

## Known Stubs

None.

## Threat Flags

None. No new network surface beyond the six endpoints in the plan's threat model.

## Issues Encountered

- The parallel-worktree ledger file under the shared `.git` directory could not be written by the sandbox; `plan_head_before` was taken from the known spawn base (`087ac37`), which the worktree base check confirmed.
- Pre-existing test warnings (unrelated files) were left alone.

## Next Phase Readiness

Plans 12-02 onward can script all six chat endpoints in `FakeClickMeApi`, assert realtime emits on `FakeRealtimeConnection`, and build DTOs from `ChatFixtures` without editing these shared files.

## Self-Check: PASSED

- Created files found: ChatRequests.kt, ChatPayloads.kt, UserReportReason.kt, ChatsRepository.kt, ChatsApiTest.kt, UserReportReasonTest.kt, ChatFixtures.kt, ChatsRepositoryTest.kt.
- Commits found in history: de3dc49, 80211b0, d459d71.
