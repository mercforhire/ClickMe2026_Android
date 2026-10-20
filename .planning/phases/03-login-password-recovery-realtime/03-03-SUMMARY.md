---
phase: 03-login-password-recovery-realtime
plan: 03
subsystem: realtime
tags: [socket.io, realtime, moshi, gradle, contracts]
requires: []
provides:
  - socket.io-client 2.1.2 and lifecycle-process 2.11.0 on the app classpath
  - realtime.RealtimeConnection seam (events, state, handshakeFailures, connect, disconnect, emits)
  - realtime.RealtimeEvent sealed hierarchy and RealtimeEventNames
  - realtime.RealtimeEventDecoder (never throws)
  - realtime.RealtimeEmits payload builders
  - realtime.socketUrl(baseUrl)
affects: [03-08 socket service, 03-09 connection manager, 03-11 DI wiring, Phase 12 chat]
tech-stack:
  added: [io.socket:socket.io-client:2.1.2, androidx.lifecycle:lifecycle-process:2.11.0]
  patterns: [pure JVM-testable decode/emit logic, redacting toString for credentials and chat content]
key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/realtime/RealtimeConnection.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/realtime/RealtimeUrl.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/realtime/RealtimeEvents.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/realtime/RealtimeEventDecoder.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/realtime/RealtimeEmits.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/realtime/RealtimeConnectionContractsTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/realtime/RealtimeUrlTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/realtime/RealtimeEventDecoderTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/realtime/RealtimeEmitsTest.kt
  modified:
    - gradle/libs.versions.toml
    - app/build.gradle.kts
    - app/src/main/keepRules/rules.keep
key-decisions:
  - "Ids, type and status stay String; booking status keeps raw values such as rescheduled"
  - "HandshakeFailure.toString and MessageNew.toString redact the token and chat content"
patterns-established:
  - "Realtime logic that must run as plain JVM tests imports neither android.util.Log nor org.json"
requirements-completed: []
duration: ~35min
completed: 2026-10-20
---

# Phase 3 Plan 03: Realtime Foundation Summary

socket.io-client 2.1.2 is on the classpath, with the `RealtimeConnection` seam, a never-throwing Moshi event decoder, Moshi-built emit payloads and `/api/v1`-stripping URL derivation. All of it is covered by plain JVM tests.

## Dependency resolution (from `:app:dependencies --configuration devDebugRuntimeClasspath`)

- `com.squareup.okhttp3:okhttp:5.5.0` is the resolved version (engine.io-client's requests for 3.12.12 and 4.12.0 both show `-> 5.5.0`).
- `io.socket:socket.io-client:2.1.2`.
- `org.json:json` does not appear (0 matches).

## Tasks

| Task | Commit | Notes |
|------|--------|-------|
| 1 RED | a98a00a | failing URL and contract tests |
| 1 GREEN | f16436b | dependencies, keep rules, RealtimeConnection, RealtimeUrl, RealtimeEvents |
| 2 | 18318b8 | RealtimeEventDecoder and its test |
| 3 | 3608418 | RealtimeEmits and its test, ktlint-format fixes to the decoder |

Final verification: `:app:testDevDebugUnitTest --tests '*Realtime*' ktlintCheck lintDevDebug assembleDevDebug` exits 0. Tests: 5 URL, 5 contracts, 16 decoder, 5 emits.

## Deviations from Plan

**1. [Rule 3 - Blocking] RealtimeEvents.kt created in Task 1, not Task 2.**
`RealtimeConnection` references `RealtimeEvent`, so Task 1 could not compile without it. The file landed in the Task 1 commit (f16436b); Task 2 added the decoder and its test.

**2. [Rule 1 - Test correctness] `status: 5` does not return null.**
Moshi's built-in String adapter coerces a JSON number to a String, so `{"status":5}` decodes to `"5"`. The test now covers the failures Moshi really produces (`status: true`, `status: {}`, `message_ids` as a string, an unparseable timestamp). No production change: coercing a number to a string is harmless, and a strict adapter would have meant changing the shared `MoshiProvider`.

**3. [Minor] `@JsonClass(generateAdapter = true)` count in RealtimeEvents.kt is 6, not 7.**
The plan makes `TypingStart` and `TypingStop` plain data classes built from the one `TypingPayload` DTO, so 6 is correct. RealtimeEmits.kt adds 2 more.

**4. Test-first ordering.** Task 1 has separate RED and GREEN commits. Tasks 2 and 3 wrote test and implementation together before the first run, so there is no separate RED commit for them.

The decoder was also reformatted by ktlint and then restructured around a small reified `adapter<T>()` helper to keep it readable.

## Known Stubs

None. `RealtimeEmits` is not called by any code yet (by design); Phase 12 wires the chat UI.

## Deferred Items

None. There were no human or device checkpoints in this plan.

## Threat Flags

None beyond the plan's threat model. T-03-SC, T-03-08 to T-03-12 are mitigated as specified (URL keeps https, token and content redacted in `toString`, decoder catches every Exception, Moshi serializes emit ids).

## Notes for downstream plans

- `RealtimeEvent` subclasses are nested (`RealtimeEvent.MessageNew`, and so on).
- `TypingPayload`, `TypingEmit` and `MessageReadEmit` are `internal`.
- RT-01 and RT-03 are groundwork only (the service and manager come in 03-08 and 03-09), so the requirements are not marked complete here.

## Self-Check: PASSED

All nine source/test files and the three build files exist; commits a98a00a, f16436b, 18318b8 and 3608418 are in the log.
