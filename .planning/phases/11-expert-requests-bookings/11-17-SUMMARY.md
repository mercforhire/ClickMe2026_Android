---
phase: 11-expert-requests-bookings
plan: 17
subsystem: voice-call
tags: [gap-closure, agora, call, channel-profile, ios-parity, diagnostics]
status: complete
gap_closure: true
gap_ids: [G-11-5]
requirements: [EXP-08]
requires:
  - phase: 11-expert-requests-bookings
    provides: "11-16 factory seam, buildRtcEngineConfig, describeEngineFailure"
provides:
  - "Agora engine created in COMMUNICATION profile (iOS parity)"
  - "buildChannelMediaOptions (no receiver scope)"
  - "Redacted remote-presence logcat lines"
  - "Cleanup of an engine whose setup fails"
affects: [voice-call, expert-bookings]
tech-stack:
  added: []
  patterns:
    - "SDK config/option objects are filled with explicit assignments, never a receiver scope (getter shadowing)"
key-files:
  created: []
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/managers/AgoraEngineConfig.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/managers/AgoraManager.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/managers/AgoraEngineConfigTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/managers/AgoraManagerCreateEngineTest.kt
    - .planning/phases/11-expert-requests-bookings/11-VALIDATION.md
key-decisions:
  - "Set RtcEngineConfig.mChannelProfile = COMMUNICATION explicitly; the SDK default 1 (LIVE_BROADCASTING) is the diagnosed probable cause of G-11-5"
  - "Remote-presence log lines carry codes and elapsed ms only, never uid, token, channel (T-09-02)"
commits: 6
plan_head_before: bc97c5cea844cb8b59a93d2ef934751fa7c5efd9
plan_head_after: 8b907d60415d4e8c17bc06bb678a0f6b65d0e700
actuals:
  tokens: 9000
  tasks: 3
  commits: 6
completed: 2026-10-26
---

# Phase 11 Plan 17: Agora Channel Profile Parity Summary

Android Agora engine now created in the COMMUNICATION channel profile like iOS, with JVM tests through `join()` to the SDK factory, codes-only remote-presence logging, and cleanup of an engine whose setup fails.

## What was done

- **Task 1 (tracer).** `buildRtcEngineConfig` sets `config.mChannelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION`. New `buildChannelMediaOptions()` builds the per-join options with explicit assignments (no receiver scope, 11-16-REVIEW IN-01); `AgoraManager.join` uses it and the unused `ChannelMediaOptions` import is gone.
- **Task 2.** The event handler logs `remote joined elapsed=N`, `remote offline reason=N`, `remote audio state=N reason=N`, `client role old=N new=N` (INFO, tag `ClickMeCall`). `RemoteJoined`/`RemoteOffline` events are emitted exactly as before. `createEngine` records the created engine and calls `runCatching { RtcEngine.destroy() }` before returning null when a setup call throws (11-16-REVIEW WR-01).
- **Task 3.** Full gate green; VALIDATION rows and approval line added.

## RED runs (TDD evidence)

1. Commit `1a244b1` `test(11-17): add failing G-11-5 channel profile tests through join()`: `builderUsesCommunicationChannelProfile` and `joinHandsFactoryACommunicationProfileConfig` both failed with `java.lang.AssertionError: expected:<0> but was:<1>` (SDK default LIVE_BROADCASTING reproduced on the JVM).
2. Commit `898fe3e` `test(11-17): add failing remote-presence logging test`: `remotePresenceCallbacksLogCodesOnlyAndKeepEvents` failed with expected the four log lines but was `[]` (events assertions already held).

GREEN commits: `324bc19`, `d76c540`, `519d72a`. Docs: `8b907d6`.

## Full gate

Run as separate invocations (daemon heap limit), all exit 0:

- `testDevDebugUnitTest testProdDebugUnitTest`: 2958 tests per flavor, 0 failures (baseline 2954 after 11-16)
- `ktlintCheck assembleDevDebug assembleProdDebug :spikes:stripe:assembleDebug`: OK
- `scripts/check-design-rules.sh` and `scripts/check-design-rules.sh spikes`: "Design rules: OK" both
- `lintDevDebug lintProdDebug`: OK
- `assembleProdRelease` (R8): OK

Acceptance greps all at expected values (profile assignment 1, `buildChannelMediaOptions` 1/1, no scope functions in `AgoraEngineConfig.kt` 0, `created = rtc` 1, `runCatching { RtcEngine.destroy() }` 1, non-comment `RtcEngine.destroy()` 2, privacy grep 0).

## Deviations from Plan

None - plan executed exactly as written. The WR-01 cleanup path has no JVM test (no mocking library, no package install); it is covered by grep criteria, as the plan specified. 11-16-REVIEW IN-02 (the `describeEngineFailure` frame choice) is not addressed by this plan.

## Known Stubs

None.

## Threat Flags

None. No new network endpoints, auth paths or trust-boundary schema. T-11-57 mitigated by the sentinel redaction assertion.

## Deferred (human) item

The two-sided device re-test is non-blocking and deferred to the user; see the STATE hand-off below.

## STATE hand-off (orchestrator writes STATE.md)

The orchestrator applies this to `.planning/STATE.md` under "### Blockers/Concerns":
- Insert the new entry directly after the "[Phase 11] G-11-1 fixed in code by 11-16" entry and its four checkboxes.
- Make the two one-line amendments.
- Edit no other STATE section.

```markdown
NEW ENTRY (insert after the G-11-1 entry and its four checkboxes):

- [Phase 11] G-11-5 fix applied in code by 11-17; DEFERRED (human), non-blocking. Symptom: after 11-16 the Android expert and the iOS client both connect to the booking's call and both stay at "waiting for the other person"; the Android SDK raised no remote-user callback in 3 joins. Probable cause (about 65%, `.planning/debug/android-ios-call-peers-never-meet.md`): the Android engine ran the SDK default LIVE_BROADCASTING profile while iOS runs COMMUNICATION. 11-17 creates the engine in COMMUNICATION (iOS parity), keeps the per-join COMMUNICATION option, and logs remote presence (`remote joined`, `remote offline`, `remote audio state`, `client role`; codes only, no uid, token or channel). Residual alternative (about 25%): the iOS tester joined a different booking. Run on the Samsung SM-A075F (serial R8YYC1QFA6H; devDebug, production backend) as the expert, with the iOS app as the client on ONE confirmed booking. adb: `~/Library/Android/sdk/platform-tools/adb -t <transport>` (transport id from `adb devices -l`). Deciding evidence, in order:
  - [ ] (1) Fresh APK: on main after the 11-17 merge, build `assembleDevDebug`, run `adb -t <transport> install -r app/build/outputs/apk/dev/debug/app-dev-debug.apk`, then `adb -t <transport> shell dumpsys package com.uptrendinvestments.clickmeapp | grep lastUpdateTime` must be later than the 11-17 merge time (`git log --format='%ci %s' --grep='11-17' -5` on main; use the `merge:` line). The first G-11-1 re-test ran a stale build, so never skip this.
  - [ ] (2) Same booking: before judging the result, confirm the iOS client joined the same booking id as the Android expert: the iOS DEBUG Xcode console (the Agora info log shows the channelId, which is the booking id), or the backend row for that booking with both `client_joined_at` and `expert_joined_at` set (POST /bookings/:id/join stamps them). A different booking reproduces the same symptom.
  - [ ] (3) Engine profile: after the join, `adb -t <transport> pull /sdcard/Android/data/com.uptrendinvestments.clickmeapp/files/agoraapi.log /tmp/g115/`; in the lines after the join time expect `initializeEx` with `channelProfile:0` and `"rtc.channel_profile":0`. `channelProfile:1` there means the installed build does not contain 11-17 (go back to step 1).
  - [ ] (4) Remote peer: `adb -t <transport> logcat -s ClickMeCall CallService` shows `remote joined elapsed=` when the iOS client joins, then `remote audio state=2` (decoding); both UIs leave "waiting for the other person". Then two-way audio, mute, speaker, leave and rejoin (11-UAT test 5).
  - [ ] (5) Then run the four checks under the G-11-1 entry above (11-UAT test 1; 09-HUMAN-UAT items 1, 2 and 5).
  - Reading the result: profile 0 and `remote joined` -> G-11-5 fixed (`/gsd-verify-work 11` resolves it). Profile 0, same booking confirmed, and no `remote joined` -> the profile hypothesis is wrong; capture the iOS Agora info log for the join and `didJoinedOfUid` next and reopen the debug session. `remote offline reason=2` (became audience) should never appear in COMMUNICATION.

AMENDMENT 1 (on the existing "[Phase 11] DEFERRED (human)" item (c), append to the end of the line):
 Blocked by G-11-5 until the 11-17 two-sided re-test; see the G-11-5 entry below.

AMENDMENT 2 (on the existing "[Phase 11] G-11-1 fixed in code by 11-16" lead line, append to the end of the line):
 Engine creation confirmed on device 2026-10-26 (the join reaches CONNECTED); the peers-never-meet follow-up is G-11-5 below.
```

Note: `plan_head_after` and `commits` measure the work up to the VALIDATION commit (`8b907d6`); this SUMMARY is committed after it.

## Self-Check: PASSED

All five modified files exist; commits `1a244b1`, `324bc19`, `898fe3e`, `d76c540`, `519d72a`, `8b907d6` are ancestors of HEAD. STATE.md and ROADMAP.md untouched.
