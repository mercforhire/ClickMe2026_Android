---
phase: 11-expert-requests-bookings
verified: 2026-10-26T20:30:00Z
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
covered_digest: "v3:sha256:1c3a64395c46ce74a7040b5a3129999431e1d42630ea183476da7977760d1471"
behavior_unverified: 1
overrides_applied: 0
re_verification:
  previous_status: human_needed
  previous_score: 3/4
  gaps_closed:
    - "G-11-5 (code): Agora engine now created in the COMMUNICATION channel profile like iOS; per-join options built without a receiver scope; remote-presence logs added; engine cleanup on setup failure. The device effect is NOT yet confirmed (see behavior_unverified_items)."
  gaps_remaining:
    - "G-11-5 device confirmation: two-sided Android expert + iOS client call re-test has not been run. 11-UAT.md still lists G-11-5 as status: failed."
  regressions: []
behavior_unverified_items:
  - truth: "SC-3: the expert can join the call (Dashboard card, Upcoming card, Summary) and both sides end up in the same Agora call and see each other"
    test: "On the Samsung SM-A075F (-s R8YYC1QFA6H, devDebug, production backend) as the expert, with the iOS app as the client on ONE confirmed booking: (1) install a fresh APK built after the 11-17 merge and confirm `dumpsys package ... | grep lastUpdateTime` is later than the merge; (2) confirm the iOS client is in the same booking id; (3) pull agoraapi.log and look for `initializeEx` with `channelProfile:0`; (4) `adb logcat -s ClickMeCall CallService` for `remote joined elapsed=` then `remote audio state=2`; then two-way audio, mute, speaker, leave, rejoin."
    expected: "channelProfile:0 at initializeEx; a `remote joined` line; both UIs leave 'waiting for the other person'; audio is two-way; mute/speaker work; leave and rejoin are clean."
    why_human: "The profile fix is proven only up to the SDK factory boundary on the JVM (the config the factory receives has mChannelProfile == 0). Whether the native engine now sees the iOS peer needs a live iOS peer, microphone, foreground service and the Agora network. The fix was diagnosed at about 65% confidence; a failed device run reopens the debug session."
human_verification:
  - test: "G-11-5 two-sided device re-test (11-UAT test 5; also clears 11-UAT test 1 and 09-HUMAN-UAT items 1, 2, 5). Steps and deciding evidence are in behavior_unverified_items and in the STATE hand-off block of 11-17-SUMMARY.md."
    expected: "Both sides in the same Agora call, remote presence logged, audio two-way, leave/rejoin clean."
    why_human: "Native Agora runtime, microphone, foreground service and a live iOS peer. Deferred and non-blocking by the user's rule."
  - test: "Residual UAT-passed items stay passed (11-UAT tests 2, 3, 4). No action unless a regression pass is wanted."
    expected: "Unchanged."
    why_human: "Device-only; already confirmed by the user in UAT."
---

# Phase 11: Expert Requests & Bookings Verification Report

**Phase Goal:** An Android expert can see their workload, accept requests, manage bookings and join sessions, so the expert side works end to end.
**Verified:** 2026-10-26T20:30:00Z
**Status:** human_needed
**Re-verification:** Yes, after gap closure plan 11-17 (G-11-5). Previous verification followed 11-16 (G-11-1).

## Re-verification: G-11-5 (Android and iOS peers never meet)

After 11-16, the Android expert joined and reached CONNECTED, but both the Android expert and the iOS client stayed on "waiting for the other person" (11-UAT test 5, `status: failed`, major). The diagnosis (`.planning/debug/android-ios-call-peers-never-meet.md`, about 65% confidence) was that Android's engine ran the SDK default LIVE_BROADCASTING profile (1), while iOS creates its engine with `channelProfile = .communication`.

**The fix is present, wired and unit-tested in code. It is not confirmed on a device.** Evidence I checked directly, not from the SUMMARY:

- **iOS source of truth.** `ClickMe2026/Managers/AgoraManager.swift` lines 91-150: `config.channelProfile = .communication` on the engine config, and `options.channelProfile = .communication`, `.broadcaster`, `publishMicrophoneTrack = true`, `autoSubscribeAudio = true` on the join.
- **Engine profile.** `AgoraEngineConfig.kt:37` has `config.mChannelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION`. `buildRtcEngineConfig` uses plain assignments, no receiver scope (G-11-1 lesson held). `AgoraManager.createEngine` calls it at line 158 with `appContext` read before the call.
- **Join options.** `buildChannelMediaOptions()` (`AgoraEngineConfig.kt:48-55`) assigns the same four values iOS uses, with no scope function, so the options' own `token` cannot shadow `join`'s parameter (11-16 IN-01). `AgoraManager.join` line 145 uses it; the `joinChannel` call and the `joinChannel failed code=` log are unchanged. Repo-wide, `RtcEngineConfig(` appears only in `AgoraEngineConfig.kt` and `io.agora` imports only in the three `managers` files, so there is no second copy.
- **Remote-presence logs.** The handler logs `remote joined elapsed=N`, `remote offline reason=N`, `remote audio state=N reason=N` and `client role old=N new=N` at INFO. `onUserJoined` and `onUserOffline` still emit `EngineEvent.RemoteJoined(uid)` and `mapUserOffline(uid, reason)` after the log, so `CallCenter` still leaves "waiting" once the peer arrives. The two new overrides only log. No `uid`, `token`, `channel` or `newRoleOptions` is interpolated. The privacy grep `Log\.[a-z]+\(.*\$\{?(token|channelName|channel|uid|rtcToken)` over `managers/*.kt` returns 0.
- **Setup-failure cleanup (11-16 WR-01).** `createEngine` records `created = rtc` before the four audio setup calls, and after both catch blocks calls `runCatching { RtcEngine.destroy() }` when `created != null`, then returns null. The success path assigns `engine` and returns from inside the `try`; the null-create path returns null early.
- **Tests.** I re-ran `*AgoraManagerCreateEngineTest`, `*AgoraEngineConfigTest`, `*CallCenter*` and `*AgoraEventMappingTest` on this checkout: BUILD SUCCESSFUL. Result files show AgoraEngineConfigTest 6, AgoraEventMappingTest 6, AgoraManagerCreateEngineTest 5, and the seven CallCenter classes 74, all with 0 failures, 0 errors, 0 skipped. `joinHandsFactoryACommunicationProfileConfig` drives `join()` to the factory and asserts `mChannelProfile == CHANNEL_PROFILE_COMMUNICATION` (0, versus the pre-fix default 1). `remotePresenceCallbacksLogCodesOnlyAndKeepEvents` asserts the exact four INFO lines in order, the two emitted events, no event for the audio-state and role callbacks, the only WARN being `engine create returned null`, and that the sentinel uid, token and channel appear in no ClickMeCall entry. The RED runs are in separate `test(11-17)` commits (`1a244b1`, `898fe3e`), then GREEN `324bc19`, `d76c540`, `519d72a`.
- **Scope of the change.** `git diff 6a79524..HEAD -- app/src` touches exactly four files: the two Agora sources and their two test classes. The expert screens, repositories and network layer verified earlier are unchanged, so truths 1, 2 and 4 are regression-clean by diff.
- **Plan hygiene.** No 11-17 commit touched `STATE.md` or `ROADMAP.md` (`git log bc97c5c..HEAD -- ...` shows only `6fccfe5`, the orchestrator's post-wave tracking commit). `11-VALIDATION.md` has the three G-11-5 rows and the 11-17 approval line.

**What is still unproven.** The JVM tests stop at the SDK factory boundary and at direct handler calls. They show the config handed to `RtcEngine.create` is COMMUNICATION and that the handler logs and emits correctly. They cannot show that the native engine now actually sees the iOS peer, that audio flows both ways, or that the diagnosis is right. The two-sided device re-test was not run and is not claimed here. The root cause was diagnosed at about 65%, with a residual 25% that the iOS tester was simply in a different booking. 11-UAT.md still carries G-11-5 as `status: failed` and test 5 as an open issue, and that is accurate until the device run.

## Goal Achievement

### Observable Truths

| # | Truth (ROADMAP success criterion) | Status | Evidence |
|---|-----------------------------------|--------|----------|
| 1 | Dashboard shows pending request count, upcoming sessions, weekly earnings text and a payout onboarding card | VERIFIED | Unchanged since the prior verification (no diff in `screens/expertmode/*`). `ExpertDashboardViewModel` (310 lines) loads upcoming bookings, pending requests, weekly earnings, payout summary and Connect status; `ExpertDashboardScreen` renders the banners, today's sessions, financial summary and payout card. UAT test 3 passed on device. |
| 2 | Expert sees an iOS client's request in Incoming Requests, accepts with an optional message or declines with a reason code, sees Confirmed, adds to calendar | VERIFIED | Unchanged. Incoming Requests, Request Decision and Booking Confirmed entries are registered in `HomeExpertScreen`; accept and decline hit real endpoints with schema-matching bodies; `CalendarIntent.kt` uses `ACTION_INSERT`. UAT test 3 passed. |
| 3 | Expert Bookings (Upcoming/Past) shows Join inside the join window and the expert can join the call | PRESENT_BEHAVIOR_UNVERIFIED | Join UI, gating and call start are wired (unchanged). G-11-1 (engine creation) is confirmed on device: the join reaches CONNECTED (UAT test 5 evidence). G-11-5 (peers never see each other) is fixed in code with the iOS-parity COMMUNICATION profile and JVM-tested, but the two-sided call has not run on a device since the fix. Whether both sides end up in the same call is unproven. |
| 4 | Expert can accept or decline a client's reschedule request, sees a clear message on `409 SLOT_UNAVAILABLE`, and can cancel | VERIFIED | Unchanged. `ExpertBookingSummaryViewModel` and `ExpertCancellationViewModel` use the guarded repositories and error mapping. UAT test 2 passed on device. |

**Score:** 3/4 truths verified (1 present, behavior-unverified: truth 3, the live two-sided Agora call).

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `managers/AgoraEngineConfig.kt` | COMMUNICATION engine profile, `buildChannelMediaOptions` | VERIFIED | Lines 28-55; assignments only, no scope functions. |
| `managers/AgoraManager.kt` | join on the options builder, redacted presence logs, setup-failure cleanup | VERIFIED | Lines 85-124 (handler), 145 (builder), 153-182 (cleanup). |
| `AgoraEngineConfigTest.kt` | profile and options assertions | VERIFIED | 6 tests pass. |
| `AgoraManagerCreateEngineTest.kt` | join()-to-factory profile check; presence log, event and redaction test | VERIFIED | 5 tests pass. |
| `11-VALIDATION.md` | G-11-5 rows and approval line | VERIFIED | Lines 78-80 and 124. |
| `screens/expertmode/*`, repositories, API | EXP-05 to EXP-09 | VERIFIED | Unchanged since the previous verification. |

### Key Link Verification

| From | To | Via | Status |
|------|----|-----|--------|
| `AgoraManager.createEngine` | `buildRtcEngineConfig` | factory receives a config with `mChannelProfile == COMMUNICATION` | WIRED (test-proven) |
| `AgoraManager.join` | `buildChannelMediaOptions()` | line 145 | WIRED |
| handler `onUserJoined` | `CallCenter` remote = Ready | `EngineEvent.RemoteJoined(uid)` still emitted after the log | WIRED (test-proven) |
| Join (Dashboard, Bookings, Summary) | `CallStarter.startCall` to `CallEngine.join` | single path | WIRED |

### Data-Flow Trace (Level 4)

Phase 11 data flows (Dashboard, Bookings, Summary) are unchanged and were traced to real repository calls in the previous verification. The Agora change adds no rendered data: the new handler lines are log-only.

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| G-11-5 and CallCenter regression | `./gradlew --offline :app:testDevDebugUnitTest --tests '*AgoraManagerCreateEngineTest' --tests '*AgoraEngineConfigTest' --tests '*CallCenter*' --tests '*AgoraEventMappingTest'` | BUILD SUCCESSFUL; 6 + 6 + 5 + 74 tests, 0 failures | PASS |
| Privacy grep | `grep -nE 'Log\.[a-z]+\(.*\$\{?(token\|channelName\|channel\|uid\|rtcToken)' managers/*.kt \| wc -l` | 0 | PASS |
| Full gate (`testDevDebugUnitTest`, `ktlintCheck`, `assembleDevDebug`; 11-17 reports 2958 tests per flavor, lint, R8) | not re-run here | Reported green by the caller (post-merge on main) and by 11-17-SUMMARY | NOT RE-RUN |
| Native Agora join, remote presence, audio | needs device and iOS peer | not run | SKIPPED (human) |

### Probe Execution

No probes declared by the phase. Skipped.

### Requirements Coverage

Every requirement ID in the PLAN frontmatter (EXP-05, EXP-06, EXP-07, EXP-08, EXP-09) appears in REQUIREMENTS.md (lines 123-127) and in its traceability table (lines 314-318, all `Phase 11 | Complete`). No ID is orphaned: REQUIREMENTS.md maps no other ID to Phase 11.

| Requirement | Source plans | Status | Evidence |
|-------------|--------------|--------|----------|
| EXP-05 | 11-01, 02, 03, 10, 14, 15 | SATISFIED | Truth 1 |
| EXP-06 | 11-01, 02, 03, 05, 06, 07, 12, 15 | SATISFIED | Truth 2. The backend discards the optional accept message (11-REVIEW WR-08). |
| EXP-07 | 11-01, 02, 07, 15 | SATISFIED | `CalendarIntent.kt` |
| EXP-08 | 11-01 to 04, 08, 10, 11, 15, 16, 17 | SATISFIED in code; live two-sided join pending device re-check | Truth 3. REQUIREMENTS.md already marks it `[x]` Complete; that is ahead of the evidence for the join half until the device re-test passes. |
| EXP-09 | 11-01, 02, 03, 08, 09, 13, 15 | SATISFIED | Truth 4 |

### Prohibitions (must_haves.prohibitions)

| Prohibition | Tier | Result |
|-------------|------|--------|
| 11-17: MUST NOT log the uid, RTC token, channel name or any exception message from AgoraManager or its helpers (EXP-08, privacy) | test | Resolved with enforcement evidence. `AgoraManagerCreateEngineTest` sentinel assertions (`assertNoSentinelsLogged`) run in all five tests, including the four new presence lines. `describeEngineFailure` omits message and cause. The grep guard returns 0. Not flagged. |
| 11-15: MUST NOT log client messages, reasons, names or the Stripe URL; MUST NOT expose deferred controls | test | Unchanged. Carried from the previous verification: grep pass is non-authoritative, UAT test 4 confirmed it on device. |

### Anti-Patterns Found

| File | Pattern | Severity | Impact |
|------|---------|----------|--------|
| Phase 11 sources | `TBD` / `FIXME` / `XXX` | None | Debt-marker gate clear for the four files changed by 11-17 and the earlier sources. |
| `AgoraManager.kt` | stubs, placeholders | None | The two new handler overrides are intentionally log-only, as the plan specifies. |

### Human Verification Required

1. **G-11-5 two-sided device re-test (SC-3, EXP-08).** Samsung SM-A075F `-s R8YYC1QFA6H`, devDebug, production backend, iOS app as the client on the same confirmed booking. Deciding evidence, in order: fresh APK with `lastUpdateTime` after the 11-17 merge; the iOS client in the same booking id; `agoraapi.log` showing `initializeEx` with `channelProfile:0`; logcat showing `remote joined elapsed=` then `remote audio state=2`; both UIs leaving "waiting for the other person"; two-way audio, mute, speaker, leave and rejoin. Result reading: profile 0 plus `remote joined` means G-11-5 is fixed. Profile 0, same booking, no `remote joined` means the profile hypothesis is wrong and the debug session reopens. This also clears 11-UAT tests 1 and 5 and 09-HUMAN-UAT items 1, 2, 5. It is deferred and non-blocking by the user's rule; it has not been run and nothing here claims it passed.
2. UAT tests 2, 3 and 4 passed on device and need no action.

### Gaps Summary

No gaps block the phase goal in code: the four roadmap success criteria and all five requirements are implemented, wired and test-covered, and G-11-5 is fixed in code to the diagnosed probable cause. The verdict is `human_needed`, not `passed`, because SC-3 ("both sides end up in the same call") has never been confirmed after the fix and the last device run (UAT test 5) failed on exactly this point.

**Bookkeeping to do before closing the phase:**

- The G-11-5 STATE hand-off (new entry plus two amendments in `11-17-SUMMARY.md`) has not been applied. `.planning/STATE.md` has no G-11-5 entry, and the "[Phase 11] DEFERRED (human)" item (c) and the G-11-1 lead line still lack their amendments. The deferred re-test is therefore not yet tracked in STATE.
- `11-UAT.md` still shows `status: diagnosed`, and test 5 and G-11-5 as open. Leave them until the device run; `/gsd-verify-work 11` reconciles through `gap_ids`.

**Advisory (not blocking).** From the incremental `11-REVIEW.md` (0 critical, 2 warnings, 3 info):

- **WR-01.** The new setup-failure cleanup (`created`, `RtcEngine.destroy()`) is covered only by grep and inspection. All five tests use a factory returning null or throwing, so `created` is always null there. Needs a `destroyRtcEngine` seam or an engine stub to test.
- **WR-02.** `destroy()` calls `RtcEngine.destroy()` bare. If it throws, `engine`, `isLeaving` and `leaveSignal` are never reset, and the next `join` would reuse a dead engine. Pre-existing, but inconsistent with the new defensive cleanup.
- **IN-01.** Return codes of the four audio setup calls are ignored.
- **IN-02.** The new INFO lines are emitted in release builds. They are redacted, but they are a debug aid for G-11-5. Gate on `BuildConfig.DEBUG` or remove them once G-11-5 closes.
- **IN-03.** iOS also calls `disableVideo()` and `enableAudioVolumeIndication`; Android omits both. Probably benign for voice-only. No success-path test for `joinChannel`.
- 11-16 IN-02 (the `describeEngineFailure` frame choice) is not addressed, as 11-17 states.

The eight earlier warnings from the first `11-REVIEW.md` (WR-01 to WR-08, notably WR-08: the backend discards the accept message) remain open in `11-REVIEW-DISPOSITION.md` and do not undermine a must-have.

---

_Verified: 2026-10-26T20:30:00Z_
_Verifier: Claude (gsd-verifier)_
