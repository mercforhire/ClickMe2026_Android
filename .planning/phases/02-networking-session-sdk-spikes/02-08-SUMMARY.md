---
phase: 02-networking-session-sdk-spikes
plan: 08
subsystem: voice-spike
tags: [spike, agora, foreground-service, microphone, background-audio]

requires:
  - phase: 02-networking-session-sdk-spikes
    provides: ":spikes:agora module skeleton with AgoraSpikeApi and login form (plan 02-04)"
provides:
  - "CallService: microphone-type foreground service hosting the Agora RtcEngine with evidence logging"
  - "AgoraSpikeViewModel: in-memory login and POST /bookings/:id/join credential fetch"
  - "MainActivity: permission flow, Join and Leave buttons, resumed-only service start"
affects: [02-12, 09-voice]

tech-stack:
  added: []
  patterns:
    - "Service start collected inside lifecycle.repeatOnLifecycle(RESUMED) so a microphone FGS is only started from the visible activity"
    - "Process importance in a 10 s heartbeat log proves audio kept flowing while backgrounded"

key-files:
  created:
    - spikes/agora/src/main/java/com/uptrendinvestments/clickmeapp/spike/agora/CallService.kt
    - spikes/agora/src/main/java/com/uptrendinvestments/clickmeapp/spike/agora/AgoraSpikeViewModel.kt
  modified:
    - spikes/agora/build.gradle.kts
    - spikes/agora/src/main/AndroidManifest.xml
    - spikes/agora/src/main/java/com/uptrendinvestments/clickmeapp/spike/agora/MainActivity.kt

key-decisions:
  - "Join config handed to the activity as observable state (pendingJoin) and consumed after the service start, instead of a SharedFlow, so it survives until the activity is resumed"
  - "The spike never calls confirm-connection or end: leaving is not ending (CALL-END-SEMANTICS)"

requirements-completed: []
requirements-enabled: [FND-14]  # code only; device proof is plan 02-12

duration: n/a
completed: 2026-10-20
---

# Phase 2 Plan 08: Agora Microphone Foreground Service Spike Summary

**Agora RtcEngine hosted in a microphone foreground service, joined with backend-issued credentials from POST /bookings/:id/join, with logcat evidence (join, peer, conn, heartbeat with process importance) for the background-audio device run.**

## Tasks

1. **Task 1: Microphone foreground service hosting the Agora engine** - `cecf045` (feat)
2. **Task 2: Join flow in the activity** - `bbac259` (feat)

## Accomplishments

- Manifest declares INTERNET, RECORD_AUDIO, MODIFY_AUDIO_SETTINGS, ACCESS_NETWORK_STATE, FOREGROUND_SERVICE, FOREGROUND_SERVICE_MICROPHONE, POST_NOTIFICATIONS and `.CallService` with `foregroundServiceType="microphone"`.
- `CallService` creates its notification channel in `onCreate`, calls `ServiceCompat.startForeground(..., FOREGROUND_SERVICE_TYPE_MICROPHONE)` before creating the engine, ports the iOS defaults (speech-standard profile, default scenario, speakerphone default route and on, volume indication every 2 s), and joins with a communication/broadcaster `ChannelMediaOptions` publishing the mic with audio auto-subscribe. `ACTION_STOP` and `onDestroy` run one idempotent teardown: `leaveChannel`, `RtcEngine.destroy`, remove the foreground state, `stopSelf`.
- Log evidence under tag `AgoraSpike`: `join:`, `peer:`, `conn:`, `error:`, `heartbeat:` (every 10 s: seconds since join, local volume, max remote volume, process importance where 100 = foreground and 125 = foreground service), `leave:`. No log statement contains the RTC credential.
- `AgoraSpikeViewModel` holds the auth token in memory only, prefills the booking id from `BuildConfig.BOOKING_ID`, parses `agora_config` (uid decoded via `Number.toInt()`), and logs `join-api: 200 connection_type=...`, `join-api: <status> <code>` (error body parsed for either `code` or `error.code`), or `join-api: no agora_config`.
- `MainActivity` requests RECORD_AUDIO (plus POST_NOTIFICATIONS on API 33+) via `RequestMultiplePermissions`, shows "Microphone permission is required" and stops on denial, otherwise calls `join()`. The service is started with `ContextCompat.startForegroundService` only while the activity is RESUMED. Leave sends `ACTION_STOP`.

## SDK name check (A6)

The Agora 4.6.4 names from the plan compiled unchanged: `RtcEngine.create(RtcEngineConfig)` with `mContext`/`mAppId`/`mEventHandler`, `setAudioProfile(AUDIO_PROFILE_SPEECH_STANDARD)`, `setAudioScenario(AUDIO_SCENARIO_DEFAULT)`, `setDefaultAudioRoutetoSpeakerphone`, `setEnableSpeakerphone`, `enableAudioVolumeIndication(Int, Int, Boolean)`, `joinChannel(token, channel, uid, ChannelMediaOptions)`, `leaveChannel`, `RtcEngine.destroy()`, and the `IRtcEngineEventHandler` callbacks. No renames were needed.

## Verification

- `./gradlew :spikes:agora:assembleDebug ktlintCheck` exits 0; `bash scripts/check-design-rules.sh spikes` prints "Design rules: OK".
- Acceptance greps pass: foregroundServiceType microphone = 1, FOREGROUND_SERVICE_MICROPHONE = 1, RECORD_AUDIO = 1, no token in Log lines, RequestMultiplePermissions and startForegroundService present in MainActivity, `agora_config` present in the ViewModel, no confirm-connection or end calls in code, no `project(":app")`.
- Not run on a device: two-way audio, 5+ minutes backgrounded and `dumpsys` FGS type are plan 02-12 and blocked on the deferred spike inputs file.

## Deviations from Plan

None in behavior. Minor: the heartbeat line and an error-body decode were split into locals purely to satisfy ktlint's 120-column limit.

## Deferred Items

- The `~/.clickme/spike-inputs.properties` file does not exist, so `BOOKING_ID` and `CLIENT_EMAIL` are empty by default; both are editable on screen.
- The service stops itself on a failed `startForeground` (SecurityException) and on missing extras, setting state Error.

## Known Stubs

None.

## Threat Flags

None beyond the plan's threat model. T-02-27 (no logging interceptor, no credentials in Log), T-02-28 (runtime RECORD_AUDIO, ongoing notification, Leave action), T-02-29 (separate applicationId, no `:app` dependency) and T-02-30 (start only from the resumed activity after the permission grant, channel before startForeground) are applied.

## Self-Check: PASSED

- CallService.kt, AgoraSpikeViewModel.kt, MainActivity.kt, manifest and build file exist and are committed.
