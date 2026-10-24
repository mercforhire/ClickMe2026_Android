---
phase: 09-voice-call
plan: 01
subsystem: network
tags: [network, dto, retrofit, moshi, test-fakes]
requires: []
provides:
  - ClickMeApi.joinCall, confirmCallConnection, endCall
  - JoinCallResponse / AgoraConfig (redacting toString) / ExternalConfig
  - ConfirmConnectionRequest, EndCallRequest, CONNECTION_STATUS_CONNECTED
  - ConfirmConnectionData, EndCallData
  - FakeClickMeApi call handlers, counters and recorded requests
affects: [09-04, 09-05, 09-11]
tech-stack:
  added: []
  patterns: [dual-shape response with nullable configs, redacting toString for credentials]
key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/model/requests/CallRequests.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/JoinCallResponse.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/ConfirmConnectionData.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/EndCallData.kt
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/network/ApiRequestUrls.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/network/ClickMeApi.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/testing/FakeClickMeApi.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/network/ResponseDecodingTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/network/ClickMeApiTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/network/ApiRequestUrlsTest.kt
key-decisions:
  - "connectionType stays a raw String; ConnectionType.fromWire arrives in plan 09-04"
  - "joinCall has no @Body: the backend join route has no schema"
  - "EndCallRequest.actualDuration documented as >= 1 (backend .int().positive()); enforcement is plan 09-11"
requirements-completed: [CALL-01, CALL-05, CALL-08]
duration: ~25 min
completed: 2026-10-24
---

# Phase 9 Plan 01: Call network contract Summary

Three bearer-authenticated Retrofit call endpoints (POST join, PATCH confirm-connection, POST end) with the dual-shape join response, the two strict request bodies, and FakeClickMeApi handlers for the later CallCenter plans.

## Tasks

| Task | Commit | Result |
|------|--------|--------|
| 1. Call DTOs and decoding tests | 67deb40 | Join decodes both shapes, null join_url and unknown keys tolerated, `AgoraConfig.toString()` redacts token, channel and uid, exact strict request bodies asserted |
| 2. Endpoints, fakes, MockWebServer tests | aae2256 | `BOOKING_JOIN`, `BOOKING_CONFIRM_CONNECTION`, `BOOKING_END`, three Retrofit methods, Fake handlers/counters/recorded lists, verb/path/body tests, 409 surfaces as `ApiException.Http` |

Verification: `:app:testDevDebugUnitTest --tests '*ClickMeApiTest' '*ApiRequestUrlsTest' '*ResponseDecodingTest'` and `ktlintCheck` pass (62, 45 and 17 tests, 0 failures).

## Deviations from Plan

**1. [Rule 1 - Test contradiction] Redaction test values.** The plan's behavior line says `AgoraConfig("secret-token", 1, "chan").toString()` must not contain "chan", but the plan's own mandated redacted string contains "channelName", which contains "chan". The test uses distinctive values (`secret-token`, `987654`, `room-xyz`) and asserts none appear. The `toString()` text is exactly as the plan specifies.

**2. TDD ordering.** Task 1 tests and DTOs were committed together (one commit per task, as the plan's commit rule says) rather than a separate failing-test commit, since the tests cannot compile without the DTOs. Tests were written first.

**3. Worktree base.** The worktree started at an older commit than the specified base; it was reset to `770937d` per the startup check.

Otherwise the plan executed as written. No auth gates.

## Known Stubs

None.

## Threat Flags

None. Mitigations T-09-01, T-09-02 and T-09-04 are implemented and tested; no Log calls were added.

## Self-Check: PASSED

All 4 created files exist; commits 67deb40 and aae2256 exist.
