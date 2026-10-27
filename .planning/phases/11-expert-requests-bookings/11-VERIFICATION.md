---
phase: 11-expert-requests-bookings
verified: 2026-10-27T01:26:58Z
status: human_needed
score: 3/4 must-haves verified
covered_files:
  - .planning/phases/11-expert-requests-bookings/11-01-PLAN.md
  - .planning/phases/11-expert-requests-bookings/11-01-SUMMARY.md
  - .planning/phases/11-expert-requests-bookings/11-02-PLAN.md
  - .planning/phases/11-expert-requests-bookings/11-02-SUMMARY.md
  - .planning/phases/11-expert-requests-bookings/11-03-PLAN.md
  - .planning/phases/11-expert-requests-bookings/11-03-SUMMARY.md
  - .planning/phases/11-expert-requests-bookings/11-04-PLAN.md
  - .planning/phases/11-expert-requests-bookings/11-04-SUMMARY.md
  - .planning/phases/11-expert-requests-bookings/11-05-PLAN.md
  - .planning/phases/11-expert-requests-bookings/11-05-SUMMARY.md
  - .planning/phases/11-expert-requests-bookings/11-06-PLAN.md
  - .planning/phases/11-expert-requests-bookings/11-06-SUMMARY.md
  - .planning/phases/11-expert-requests-bookings/11-07-PLAN.md
  - .planning/phases/11-expert-requests-bookings/11-07-SUMMARY.md
  - .planning/phases/11-expert-requests-bookings/11-08-PLAN.md
  - .planning/phases/11-expert-requests-bookings/11-08-SUMMARY.md
  - .planning/phases/11-expert-requests-bookings/11-09-PLAN.md
  - .planning/phases/11-expert-requests-bookings/11-09-SUMMARY.md
  - .planning/phases/11-expert-requests-bookings/11-10-PLAN.md
  - .planning/phases/11-expert-requests-bookings/11-10-SUMMARY.md
  - .planning/phases/11-expert-requests-bookings/11-11-PLAN.md
  - .planning/phases/11-expert-requests-bookings/11-11-SUMMARY.md
  - .planning/phases/11-expert-requests-bookings/11-12-PLAN.md
  - .planning/phases/11-expert-requests-bookings/11-12-SUMMARY.md
  - .planning/phases/11-expert-requests-bookings/11-13-PLAN.md
  - .planning/phases/11-expert-requests-bookings/11-13-SUMMARY.md
  - .planning/phases/11-expert-requests-bookings/11-14-PLAN.md
  - .planning/phases/11-expert-requests-bookings/11-14-SUMMARY.md
  - .planning/phases/11-expert-requests-bookings/11-15-PLAN.md
  - .planning/phases/11-expert-requests-bookings/11-15-SUMMARY.md
  - .planning/phases/11-expert-requests-bookings/11-16-PLAN.md
  - .planning/phases/11-expert-requests-bookings/11-16-SUMMARY.md
  - .planning/phases/11-expert-requests-bookings/11-17-PLAN.md
  - .planning/phases/11-expert-requests-bookings/11-17-SUMMARY.md
  - app/build.gradle.kts
  - app/src/main/java/com/uptrendinvestments/clickmeapp/managers/AgoraEngineConfig.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/managers/AgoraManager.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/bookingsummary/ExpertBookingSummaryViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/cancellation/ExpertCancellationViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/confirmed/BookingConfirmedScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/confirmed/CalendarIntent.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/dashboard/ExpertDashboardScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/dashboard/ExpertDashboardViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/home/HomeExpertScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/incomingrequests/IncomingRequestsViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/requestdecision/RequestDecisionViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/expertmode/upcomingbookings/ExpertBookingsViewModel.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/managers/AgoraEngineConfigTest.kt
  - app/src/test/java/com/uptrendinvestments/clickmeapp/managers/AgoraManagerCreateEngineTest.kt
  - gradle/libs.versions.toml
covered_digest: "v3:sha256:009adcd31351e2bfa543222fcaad7c7430030c78b174d08b6bd112a8313d0bdc"
behavior_unverified: 1
overrides_applied: 0
re_verification:
  previous_status: human_needed
  previous_score: 3/4
  gaps_closed:
    - "Stale verification: code-review fixes WR-01 (ff49ad4) and WR-02 (754b8f5) changed AgoraManager.kt, AgoraManagerCreateEngineTest.kt, app/build.gradle.kts and libs.versions.toml after the previous report. Re-checked here; no must-have regressed."
    - "Prior advisory WR-01 (setup-failure cleanup untested) and WR-02 (destroy() not exception-safe) are fixed and test-covered."
  gaps_remaining:
    - "G-11-5 device confirmation: two-sided Android expert + iOS client call re-test has not been run. 11-UAT.md still lists G-11-5 as status: failed and is accurate until that run."
  regressions: []
behavior_unverified_items:
  - truth: "SC-3: the expert can join the call (Dashboard card, Upcoming card, Summary) and both sides end up in the same Agora call and see each other"
    test: "On the Samsung SM-A075F (-s R8YYC1QFA6H, devDebug, production backend) as the expert, with the iOS app as the client on ONE confirmed booking: (1) install a fresh APK built after the 11-17 merge and confirm `dumpsys package ... | grep lastUpdateTime` is later than the merge; (2) confirm the iOS client is in the same booking id; (3) pull agoraapi.log and look for `initializeEx` with `channelProfile:0`; (4) `adb logcat -s ClickMeCall CallService` for `remote joined elapsed=` then `remote audio state=2`; then two-way audio, mute, speaker, leave, rejoin."
    expected: "channelProfile:0 at initializeEx; a `remote joined` line; both UIs leave 'waiting for the other person'; audio is two-way; mute/speaker work; leave and rejoin are clean."
    why_human: "The profile fix is proven only up to the SDK factory boundary on the JVM (the config the factory receives has mChannelProfile == 0). Whether the native engine now sees the iOS peer needs a live iOS peer, microphone, foreground service and the Agora network. The fix was diagnosed at about 65% confidence; a failed device run reopens the debug session."
human_verification:
  - test: "G-11-5 two-sided device re-test (11-UAT test 5; also clears 11-UAT test 1 and 09-HUMAN-UAT items 1, 2, 5). Steps and deciding evidence are in behavior_unverified_items and in the STATE.md G-11-5 hand-off entry."
    expected: "Both sides in the same Agora call, remote presence logged, audio two-way, leave/rejoin clean."
    why_human: "Native Agora runtime, microphone, foreground service and a live iOS peer. Deferred and non-blocking by the user's rule."
  - test: "Residual UAT-passed items stay passed (11-UAT tests 2, 3, 4). No action unless a regression pass is wanted."
    expected: "Unchanged."
    why_human: "Device-only; already confirmed by the user in UAT."
---

# Phase 11: Expert Requests & Bookings Verification Report

**Phase Goal:** An Android expert can see their workload, accept requests, manage bookings and join sessions, so the expert side works end to end.
**Verified:** 2026-10-27T01:26:58Z
**Status:** human_needed
**Re-verification:** Yes. The previous report (human_needed, after 11-17) went stale when the code-review fixes WR-01/WR-02 landed. Truths were re-checked against HEAD `dfa470c`.

## Re-verification: effect of the code-review fixes (ff49ad4, 754b8f5)

Question: did the review fixes regress any must-have? Evidence checked directly in the code, not from 11-REVIEW-FIX.md.

- **Scope of the change.** `git diff da0ea0c..HEAD` over `app/` and `gradle/` touches exactly: `AgoraManager.kt` (+26/-10), `AgoraManagerCreateEngineTest.kt` (+92), `app/build.gradle.kts` (+1, `testImplementation(libs.mockito.core)`), `gradle/libs.versions.toml` (+1, mockito-core 5.24.0). The diff over `screens/` and `network/` is empty, so truths 1, 2 and 4 (Dashboard, Incoming Requests/Decision/Confirmed, reschedule/cancel) are regression-clean by diff. The Mockito dependency is `testImplementation` only and does not enter the app.
- **Seam and DI.** `AgoraManager.kt` line 44 adds the internal primary-constructor parameter `destroyRtcEngine: () -> Unit = { RtcEngine.destroy() }`, placed before `createRtcEngine`. The Hilt `@Inject` secondary constructor (lines 47-51) passes `{ RtcEngine.destroy() }` and `{ config -> RtcEngine.create(config) }`, so production behavior is unchanged. The user said the full gate passed on HEAD, which includes `assembleDevDebug`-level compilation of the Hilt graph via the unit-test build; I did not re-run it.
- **Join path unchanged.** `join` (lines 137-151) still calls `buildChannelMediaOptions()` and `rtc.joinChannel(...)` with the `joinChannel failed code=` log. `createEngine` (lines 154-183) still builds the COMMUNICATION config through `buildRtcEngineConfig` with `appContext` read before the call; the only change is line 181 now calls the `destroyRtcEngine` seam instead of the static `RtcEngine.destroy()` (same behavior in production, injectable in tests).
- **Remote-presence handler unchanged.** Lines 86-117: `onUserJoined` still logs `remote joined elapsed=` then emits `EngineEvent.RemoteJoined(uid)`; `onUserOffline` logs and emits `mapUserOffline`; the audio-state and role callbacks only log. So `CallCenter` still leaves "waiting" once a peer arrives.
- **WR-02 behavior.** `destroy()` (lines 218-238) now calls `destroyRtcEngine()` inside `try`, catches `Exception` and `LinkageError` with a redacted `engine destroy failed <class/top frame>` warning, and resets `engine`, `isLeaving` and `leaveSignal` in `finally`. A throwing release no longer leaves a dead engine for the next `join`, and no longer propagates into `CallCenter` teardown. This is a robustness improvement; it does not alter the normal leave/destroy/rejoin sequence.
- **Tests.** I re-ran `*AgoraManagerCreateEngineTest`, `*AgoraEngineConfigTest`, `*CallCenter*`, `*AgoraEventMappingTest` on this checkout: BUILD SUCCESSFUL. Result files: AgoraEngineConfigTest 6, AgoraEventMappingTest 6, AgoraManagerCreateEngineTest 8 (was 5), CallCenter classes Confirm 14, Cutoff 7, End 20, Guard 7, Peer 6, Permission 3, Start 17 (= 74), all with 0 failures, 0 errors, 0 skipped. The three new tests are real behavior checks: `setupFailureAfterCreationDestroysEngineAndAllowsRetry` (Mockito `RtcEngine` whose setup throws after creation: destroy ran once, `join` false, `joinChannel` never called, one redacted warning, second `join` re-creates and destroys again), `successfulSetupDoesNotDestroyEngine` (destroy count 0, `join` true), `destroyThatThrowsStillResetsEngineSoNextJoinRecreatesIt` (destroy throws after a good join: one redacted warning, sentinel-free logs, next `join` calls the factory a second time). This upgrades the previous advisory WR-01/WR-02 from inspection-only to test-proven.
- **Privacy.** `Log.*` lines interpolating token/channel/uid/rtcToken across `managers/*.kt`: 0. The new `engine destroy failed` line uses `describeEngineFailure` (class name and top frame only), and `assertNoSentinelsLogged()` runs in the new tests. Debt-marker grep (`TBD|FIXME|XXX`) over `managers/` and the changed test: 0.
- **Verdict.** No must-have regressed. The G-11-5 code fix from 11-17 (COMMUNICATION engine profile, per-join options, presence logs) is intact.

**What is still unproven.** Unchanged from before, and deliberately not upgraded: the JVM tests stop at the SDK factory boundary and at direct handler calls. They show the config handed to `RtcEngine.create` is COMMUNICATION and the handler logs and emits correctly. They cannot show that the native engine sees the iOS peer, that audio flows both ways, or that the ~65% diagnosis is right (residual ~25%: the iOS tester was in a different booking). The two-sided device re-test has not been run and is not claimed here. 11-UAT.md still carries G-11-5 as `status: failed` and test 5 as an open issue, which is accurate until that run.

## Goal Achievement

### Observable Truths

| # | Truth (ROADMAP success criterion) | Status | Evidence |
|---|-----------------------------------|--------|----------|
| 1 | Dashboard shows pending request count, upcoming sessions, weekly earnings text and a payout onboarding card | VERIFIED | No diff in `screens/expertmode/*` since the previous verification. `ExpertDashboardViewModel` loads upcoming bookings, pending requests, weekly earnings, payout summary and Connect status; `ExpertDashboardScreen` renders banners, today's sessions, financial summary and payout card. UAT test 3 passed on device. |
| 2 | Expert sees an iOS client's request in Incoming Requests, accepts with an optional message or declines with a reason code, sees Confirmed, adds to calendar | VERIFIED | Unchanged. Incoming Requests, Request Decision and Booking Confirmed entries are registered in `HomeExpertScreen`; accept and decline hit real endpoints; `CalendarIntent.kt` uses `ACTION_INSERT`. UAT test 3 passed. |
| 3 | Expert Bookings (Upcoming/Past) shows Join inside the join window and the expert can join the call | PRESENT_BEHAVIOR_UNVERIFIED | Join UI, gating and call start are wired (unchanged). G-11-1 (engine creation) is confirmed on device: the join reaches CONNECTED (UAT test 5 evidence). G-11-5 (peers never see each other) is fixed in code with the iOS-parity COMMUNICATION profile and JVM-tested, and the review fixes did not alter that path, but no two-sided call has run on a device since the fix. Whether both sides end up in the same call is unproven. |
| 4 | Expert can accept or decline a client's reschedule request, sees a clear message on `409 SLOT_UNAVAILABLE`, and can cancel | VERIFIED | Unchanged. `ExpertBookingSummaryViewModel` and `ExpertCancellationViewModel` use the guarded repositories and error mapping. UAT test 2 passed on device. |

**Score:** 3/4 truths verified (1 present, behavior-unverified: truth 3, the live two-sided Agora call).

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `managers/AgoraEngineConfig.kt` | COMMUNICATION engine profile, `buildChannelMediaOptions` | VERIFIED | Unchanged by the review fixes; assignments only, no scope functions. 6 tests pass. |
| `managers/AgoraManager.kt` | join on options builder, redacted presence logs, setup-failure cleanup, exception-safe destroy | VERIFIED | Handler lines 86-117, join 137-151, createEngine 154-183, destroy 218-238. |
| `AgoraManagerCreateEngineTest.kt` | profile check, presence log/event/redaction, setup-failure cleanup, destroy-throws | VERIFIED | 8 tests pass (5 prior, 3 new). |
| `app/build.gradle.kts`, `libs.versions.toml` | `mockito-core` test dependency | VERIFIED | `testImplementation` only; resolves and compiles (tests ran). |
| `11-VALIDATION.md` | G-11-5 rows and approval line | VERIFIED | Unchanged since the previous verification. |
| `screens/expertmode/*`, repositories, API | EXP-05 to EXP-09 | VERIFIED | Unchanged (empty diff since `da0ea0c`). |

### Key Link Verification

| From | To | Via | Status |
|------|----|-----|--------|
| `AgoraManager.createEngine` | `buildRtcEngineConfig` | factory receives config with `mChannelProfile == COMMUNICATION` | WIRED (test-proven) |
| `AgoraManager.createEngine` | `destroyRtcEngine` seam | line 181, when setup fails after creation | WIRED (test-proven) |
| `AgoraManager.destroy` | `destroyRtcEngine` seam | line 225, state reset in `finally` | WIRED (test-proven) |
| `AgoraManager.join` | `buildChannelMediaOptions()` | line 146 | WIRED |
| handler `onUserJoined` | `CallCenter` remote = Ready | `EngineEvent.RemoteJoined(uid)` still emitted after the log | WIRED (test-proven) |
| Hilt `@Inject` constructor | `RtcEngine.create` / `RtcEngine.destroy` | lines 47-51 | WIRED |
| Join (Dashboard, Bookings, Summary) | `CallStarter.startCall` to `CallEngine.join` | single path | WIRED |

### Data-Flow Trace (Level 4)

Phase 11 data flows (Dashboard, Bookings, Summary) are unchanged and were traced to real repository calls in the earlier verification. The review fixes add no rendered data: the new `destroy` handling is log-only plus state reset.

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| Agora and CallCenter regression | `./gradlew --offline :app:testDevDebugUnitTest --tests '*AgoraManagerCreateEngineTest' --tests '*AgoraEngineConfigTest' --tests '*CallCenter*' --tests '*AgoraEventMappingTest'` | BUILD SUCCESSFUL; 6 + 6 + 8 + 74 tests, 0 failures/errors/skips | PASS |
| Privacy grep | `grep -nE 'Log\.[a-z]+\(.*\$\{?(token\|channelName\|channel\|uid\|rtcToken)' managers/*.kt \| wc -l` | 0 | PASS |
| Debt-marker grep | `grep -rnE 'TBD\|FIXME\|XXX'` over `managers/` and the changed test | 0 | PASS |
| Full regression gate (`testDevDebugUnitTest ktlintCheck`) | run by the caller on HEAD | Reported passed by the caller | NOT RE-RUN HERE (caller-reported) |
| Native Agora join, remote presence, audio | needs device and iOS peer | not run | SKIPPED (human) |

### Probe Execution

No probes declared by the phase. Skipped.

### Requirements Coverage

Every requirement ID in the PLAN frontmatter (EXP-05, EXP-06, EXP-07, EXP-08, EXP-09; declared across plans 11-01 to 11-17) appears in REQUIREMENTS.md (lines 123-127) and in its traceability table (lines 314-318, all `Phase 11 | Complete`). No ID is orphaned: REQUIREMENTS.md maps no other ID to Phase 11.

| Requirement | Source plans | Status | Evidence |
|-------------|--------------|--------|----------|
| EXP-05 | 11-01, 02, 03, 10, 14, 15 | SATISFIED | Truth 1 |
| EXP-06 | 11-01, 02, 03, 05, 06, 07, 12, 15 | SATISFIED | Truth 2. The backend discards the optional accept message (11-REVIEW WR-08, open). |
| EXP-07 | 11-01, 02, 07, 15 | SATISFIED | `CalendarIntent.kt` |
| EXP-08 | 11-01 to 04, 08, 10, 11, 15, 16, 17 | SATISFIED in code; live two-sided join pending device re-check | Truth 3. REQUIREMENTS.md already marks it `[x]` Complete; that is ahead of the evidence for the join half until the device re-test passes. |
| EXP-09 | 11-01, 02, 03, 08, 09, 13, 15 | SATISFIED | Truth 4 |

### Prohibitions (must_haves.prohibitions)

| Prohibition | Tier | Result |
|-------------|------|--------|
| 11-17: MUST NOT log the uid, RTC token, channel name or any exception message from AgoraManager or its helpers (EXP-08, privacy) | test | Resolved with enforcement evidence. `AgoraManagerCreateEngineTest` sentinel assertions (`assertNoSentinelsLogged`) run across the tests, including the three added by the review fixes (the new `engine destroy failed` line is redacted). `describeEngineFailure` omits message and cause. Grep guard returns 0. Not flagged. |
| 11-15: MUST NOT log client messages, reasons, names or the Stripe URL; MUST NOT expose deferred controls | test | Unchanged (no `screens/` diff). Grep pass is non-authoritative; UAT test 4 confirmed it on device. |

### Anti-Patterns Found

| File | Pattern | Severity | Impact |
|------|---------|----------|--------|
| Phase 11 sources and changed tests | `TBD` / `FIXME` / `XXX` | None | Debt-marker gate clear. |
| `AgoraManager.kt` | stubs, placeholders | None | New presence overrides are intentionally log-only. The `destroy()` catch blocks log and swallow by design (documented at line 222-223). |

### Human Verification Required

1. **G-11-5 two-sided device re-test (SC-3, EXP-08).** Samsung SM-A075F `-s R8YYC1QFA6H`, devDebug, production backend, iOS app as the client on the same confirmed booking. Deciding evidence, in order: fresh APK with `lastUpdateTime` after the 11-17 merge (and now after the review fixes if a current build is wanted); the iOS client in the same booking id; `agoraapi.log` showing `initializeEx` with `channelProfile:0`; logcat showing `remote joined elapsed=` then `remote audio state=2`; both UIs leaving "waiting for the other person"; two-way audio, mute, speaker, leave and rejoin. Reading the result: profile 0 plus `remote joined` means G-11-5 is fixed. Profile 0, same booking, no `remote joined` means the profile hypothesis is wrong and the debug session reopens. This also clears 11-UAT tests 1 and 5 and 09-HUMAN-UAT items 1, 2, 5. It is deferred and non-blocking by the user's rule; it has not been run and nothing here claims it passed.
2. UAT tests 2, 3 and 4 passed on device and need no action.

### Gaps Summary

No gaps block the phase goal in code: the four roadmap success criteria and all five requirements are implemented, wired and test-covered, and the code-review fixes did not regress any of them. The verdict stays `human_needed`, not `passed`, because SC-3 ("both sides end up in the same call") has never been confirmed after the G-11-5 fix, and the last device run (UAT test 5) failed on exactly this point.

**Bookkeeping status.**

- The G-11-5 hand-off is now tracked in `.planning/STATE.md` (G-11-5 entry at line 227 and the amended "DEFERRED (human)" item (c) at line 219 and G-11-1 entry at line 222). This resolves the open bookkeeping item from the previous report.
- `11-UAT.md` still shows `status: diagnosed`, with test 5 and G-11-5 open. Leave them until the device run; `/gsd-verify-work 11` reconciles through `gap_ids`.

**Advisory (not blocking).** From the incremental `11-REVIEW.md` (0 critical): WR-01 and WR-02 are now fixed (`ff49ad4`, `754b8f5`) and recorded as such in `11-REVIEW-DISPOSITION.md`. Still open:

- **IN-01.** Return codes of the four audio setup calls are ignored.
- **IN-02.** The new INFO presence lines are emitted in release builds. They are redacted, but they are a G-11-5 debug aid; gate on `BuildConfig.DEBUG` or remove once G-11-5 closes.
- **IN-03.** iOS also calls `disableVideo()` and `enableAudioVolumeIndication`; Android omits both. Probably benign for voice-only. No success-path test for `joinChannel` against a real engine.
- 11-16 IN-02 (the `describeEngineFailure` frame choice) remains unaddressed.

The eight warnings from the first `11-REVIEW.md` (WR-01 to WR-08, notably WR-08: the backend discards the accept message) remain open in `11-REVIEW-DISPOSITION.md` and do not undermine a must-have.

---

_Verified: 2026-10-27T01:26:58Z_
_Verifier: Claude (gsd-verifier)_
