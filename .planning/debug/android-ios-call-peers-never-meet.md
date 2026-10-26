---
status: diagnosed
trigger: "Phase 11 UAT Test 5 / gap G-11-5: Android expert (devDebug, production backend) and iOS client both reach the call screen for the same booking and BOTH show 'waiting for the other person'; neither sees the other."
created: 2026-10-26T23:40:00Z
updated: 2026-10-27T00:20:00Z
goal: find_root_cause_only
---

## Current Focus

hypothesis: PROBABLE (about 65%). The Android engine joins in the LIVE_BROADCASTING channel profile while the iOS engine joins in COMMUNICATION. `buildRtcEngineConfig` never sets `RtcEngineConfig.mChannelProfile`, whose SDK default is 1 (LIVE_BROADCASTING); iOS sets `config.channelProfile = .communication` on its engine config. The only COMMUNICATION setting on Android is `ChannelMediaOptions.channelProfile` at join, and the device's Agora API log shows the engine and connection were created with profile 1 and never re-profiled. Peers in different profiles do not see each other (no onUserJoined on either side).
test: (done, partial) Device Agora API log (agoraapi.log pulled from the Samsung), Agora 4.6.4 bytecode (RtcEngineConfig default), iOS and backend source, native-lib disassembly of the join path. A two-sided run with `mChannelProfile = COMMUNICATION` is the deciding test and could not be run from here (needs the iOS peer).
expecting: With `config.mChannelProfile = CHANNEL_PROFILE_COMMUNICATION`, the Android API log shows `initializeEx ... channelProfile:0`, `rtc.channel_profile":0`, and a remote `onUserJoined`/UI Active appears when iOS joins the same booking.
next_action: none for this agent (diagnose-only). Hand to gap-closure planning: set engine-level COMMUNICATION, add remote-event logging, and run the two-sided re-test; first rule out that the iOS tester joined a different booking.

## Symptoms

expected: The Android expert and the iOS client join the same Agora voice call for a booking (30a789c1-...) and hear each other; both UIs go Active.
actual: Both apps reach the call screen at the same time and BOTH show "waiting for the other person". Neither sees the other.
errors: none. Android logcat (fixed build): CallService started, `conn state=2 reason=0`, `conn state=3 reason=1` (CONNECTED / JOIN_SUCCESS), no `joinChannel failed`, no `error code=`.
reproduction: UAT test 5 re-test after 11-16: join from the Android expert while the iOS client joins the same booking. Reproducible (3 Android joins at 19:17:51, 19:18:31, 19:19:13, none saw a peer).
started: First time Android and iOS were run against each other in the in-app voice path. Phase 2 test 9 (Agora spike with an iOS peer) was skipped, and Phase 9 device checks were pending, so Android<->iOS Agora interop had never been verified.

## Eliminated

- hypothesis: (1) Different Agora App ID between Android and iOS / backend token
  evidence: Android `AGORA_APP_ID` in both flavors (app/build.gradle.kts:52,63) is the same string as `AgoraAppId` in all three iOS environments (ios Environments.plist; `Current` = Production). Android joined and reached CONNECTED with a server-minted token for that App ID (a mismatched App ID or token yields INVALID_TOKEN, not CONNECTED), and the iOS side reached "waiting for the other person", which only happens after its join callback (MeetingCallViewModel.swift:286, AgoraManager.swift:158-170). Values were compared, not printed.
  timestamp: 2026-10-27T00:00:00Z
- hypothesis: (3) Client role mismatch / Android joined as audience
  evidence: Agora API log `doJoinChannel` options `clientRoleType:1` (broadcaster), `publishMicrophone:1`, `autoSubscribeAudio:1`; `onClientRoleChanged oldRole:2 newRole:1`; `onFirstLocalAudioFramePublished` at 19:17:52.302 (Android publishes audio). iOS also broadcaster with publishMicrophoneTrack (AgoraManager.swift:146-149). The backend token role is PUBLISHER for both sides (agora.js:99).
  timestamp: 2026-10-27T00:02:00Z
- hypothesis: (4) int uid vs user account mismatch
  evidence: Both sides call the int-uid join (Android `joinChannel(token, channel, uid, options)`, iOS `joinChannel(byToken:channelId:uid:mediaOptions:)` with `UInt(config.uid)`); the API log shows `localUid:14832` for Android, equal to the server-issued uid (uint16-range XOR fold, agora.js:30-38). Token and uid matched (join succeeded).
  timestamp: 2026-10-27T00:03:00Z
- hypothesis: (6) The two apps hit different backend environments
  evidence: Android dev and prod flavors both use https://clickme-backend-production.up.railway.app/api/v1 (build.gradle.kts:46,57); all three iOS environments in Environments.plist use the same URL. Same backend, same Agora project.
  timestamp: 2026-10-27T00:04:00Z
- hypothesis: (5) Android "remote joined" UI state not wired to onUserJoined (one-sided UI bug)
  evidence: AgoraManager.kt:83-88 maps onUserJoined to `EngineEvent.RemoteJoined`; CallCenter.kt:315 sets `remote = Ready`; CallSession.withConnStates derives Active. More importantly the SDK itself never raised the callback: the Agora API log (agoraapi.log) lists every callback that fired and contains no user-joined, user-offline or remote-audio-state entry in any of the three joins (the third lasted 2 min 14 s). iOS independently shows the same "waiting" state, so a one-sided UI bug cannot explain it.
  timestamp: 2026-10-27T00:06:00Z
- hypothesis: Channel name differs by format (booking id vs a backend-provided name)
  evidence: Both platforms use the `channel_name` from POST /bookings/:id/join, which the backend sets to the booking id for both roles (agora.js:85, call.controller.js:106). API log shows Android `channelId` = 30a789c1-107b-48ee-b782-5e151b17751a, identical to the booking id in the POST URL. There is no client-side derivation on either platform. (Whether the iOS tester joined the same BOOKING is a separate, still open question, see Current candidates.)
  timestamp: 2026-10-27T00:08:00Z

## Evidence

- timestamp: 2026-10-27T00:00:00Z
  checked: Android join parameters on the device (Agora API log pulled from /sdcard/Android/data/com.uptrendinvestments.clickmeapp/files/agoraapi.log to /tmp/g115-agora-logs/; agorasdk.log is encrypted and unreadable)
  found: |
    19:17:51.371 RtcEngine::initializeEx(context:(... channelProfile:1 ...))
    19:17:51.572 RtcConnectionImpl::initializeEx(cfg:(... clientRoleType:2 ... channelProfile:1 ...))   <- connection created as LIVE_BROADCASTING / audience
    19:17:51.579 RtcEngine::setParametersInternal({"rtc.channel_profile":1})
    19:17:51.625 RtcEngine::joinChannel({channelId:"30a789c1-...", uid:14832, options})
    19:17:51.625 doJoinChannel options: clientRoleType:1, publishMicrophone:1, autoSubscribeAudio:1, channelProfile:0
    19:17:51.637 setUserRoleStatus(role:1, latency_level:2); onClientRoleChanged oldRole:2 newRole:1
    19:17:51.842 onJoinChannelSuccess (CONNECTED); 19:17:52.302 onFirstLocalAudioFramePublished
    `rtc.channel_profile` is set exactly once per engine (lines 29, 180, 315) and always to 1; nothing sets it to 0 at or after joinChannel.
  implication: The engine and its connection run with channelProfile 1 (LIVE_BROADCASTING). The per-join `channelProfile:0` is passed in the options but no profile change is logged after joinChannel.

- timestamp: 2026-10-27T00:02:00Z
  checked: Why the Android engine profile is 1 (Agora voice-sdk 4.6.4, javap of io.agora.rtc2.RtcEngineConfig, extracted by the earlier G-11-1 session)
  found: `RtcEngineConfig()` constructor sets `mChannelProfile = 1` (iconst_1 / putfield mChannelProfile). `buildRtcEngineConfig` (AgoraEngineConfig.kt:26-30) sets only mContext, mAppId and mEventHandler. `AgoraManager.join` sets COMMUNICATION only in `ChannelMediaOptions` (AgoraManager.kt:126).
  implication: Android engine profile = SDK default LIVE_BROADCASTING; COMMUNICATION exists only as a per-join option.

- timestamp: 2026-10-27T00:04:00Z
  checked: iOS reference (ios-reference/ClickMe2026/Managers/AgoraManager.swift)
  found: `config.channelProfile = .communication` on `AgoraRtcEngineConfig` (line 99) AND `options.channelProfile = .communication` at join (line 145). iOS therefore initializes its engine in COMMUNICATION and joins in COMMUNICATION. Remote presence = `didJoinedOfUid` (line 270) and `didOfflineOfUid` (line 276).
  implication: Android and iOS differ in the engine-level profile: Android 1 (LIVE_BROADCASTING), iOS 0 (COMMUNICATION). Everything else in the join is equivalent.

- timestamp: 2026-10-27T00:06:00Z
  checked: No remote-peer event on Android in any of 3 joins
  found: The set of callbacks in agoraapi.log is only onJoinChannelSuccess, onConnectionStateChanged (connecting/disconnected), onClientRoleChanged, onAudioPublishStateChanged, onLocalAudioTrackStateChanged, onFirstLocalAudioFramePublished, onAudioRoutingChanged, onLeaveChannel. No onUserJoined, onUserOffline or remote audio state. Android does publish its mic (publish state 3, first frame published) and was muted/unmuted at 19:18:09-10.
  implication: The Android SDK never saw a remote user, even in a 2 min 14 s session. Combined with the iOS report, this is mutual invisibility, not a one-sided display bug.

- timestamp: 2026-10-27T00:08:00Z
  checked: Backend join (backend src/services/agora.js, src/controllers/call.controller.js)
  found: One handler for both roles. `channelName = bookingId` (agora.js:85), `uid = uuidToUid32(userId)` (agora.js:87), `RtcRole.PUBLISHER` for both (agora.js:99), the same AGORA_APP_ID/certificate. No role gate, no per-platform branching. The response carries `rtc_token`, `uid`, `channel_name` only.
  implication: The backend cannot cause a split by role or platform. Any split comes from the clients (profile) or from the tester joining a different booking.

- timestamp: 2026-10-27T00:10:00Z
  checked: Realtime echo in the Android logcat (/tmp/clickme-logcat2.txt)
  found: `19:18:27.380 D/Realtime CallEnded(bookingId=30a789c1-..., endReason=manual_exit)` is the echo of Android's own POST /end at 19:18:26.962 (emitCallEnded targets BOTH rooms, events.js:489-499). No CallEnded from the iOS side is visible in the window. No confirm-connection request was sent (never Active).
  implication: Nothing in the logcat shows the iOS client acting on this booking. It neither confirms nor refutes that iOS joined 30a789c1.

- timestamp: 2026-10-27T00:12:00Z
  checked: Native-library disassembly of libagora-rtc-sdk.so (arm64, 4.6.4) join path (llvm-objdump; byte-offset analysis, secondary evidence)
  found: The only code that sets the connection channel profile is the vtable method at 0x64d0cc (logs "uid:%u set channel profile %d", stores the profile at this+0x134, calls the helper that issues `setParametersInternal rtc.channel_profile`). It has no direct callers; its vtable slot (offset 0x240) is invoked from RtcEngine::initializeEx (0x7a5a44-0x7a5c00) and is NOT invoked anywhere in RtcEngine::doJoinChannel (0x7ac920-0x7ad4c8) or doJoinChannelEx. I found no code that reads `ChannelMediaOptions.channelProfile` other than the copy constructor and the JSON formatter. This is consistent with the API log (no second `rtc.channel_profile` line at join) but is not conclusive: another indirect path could exist.
  implication: Supports "the per-join COMMUNICATION option does not re-profile the engine in this SDK build". Treat as supporting, not proof.

- timestamp: 2026-10-27T00:14:00Z
  checked: Project history for any prior Android<->iOS Agora interop proof
  found: 02-UAT.md test 9 ("Agora spike: background audio with an iOS peer") was `skipped` on 2026-10-21. The spike used the same engine config (no mChannelProfile) and the same per-join COMMUNICATION option. 09-RESEARCH.md copied that config as "iOS parity". The code comment at AgoraEventMapping.kt:31 ("cannot occur in the COMMUNICATION profile") shows the design assumed COMMUNICATION, which the engine does not actually run.
  implication: The profile difference was never exercised against a real iOS peer; the config was carried from an unverified spike.

- timestamp: 2026-10-27T00:16:00Z
  checked: External documentation (web)
  found: Agora documents that COMMUNICATION and LIVE_BROADCASTING are different channel models and recommends all users in a channel use the same profile; the Channel Management REST API reference states all users in a channel must use the same channel profile. I found no Agora statement that a mixed-profile channel is supported, and none that explicitly says users cannot see each other. Agora's own Android quickstart sets COMMUNICATION only through ChannelMediaOptions, which argues the per-join option is meant to be honored, so the "ignored per-join option" premise is the weakest link.
  implication: Profile mismatch is the leading explanation but not documentation-proven. The decisive proof is the one-line change plus a two-sided re-run.

## Resolution

root_cause: |
  PROBABLE (about 65%), not yet confirmed by a two-sided run.
  The Android engine and connection run in the LIVE_BROADCASTING channel profile while the iOS engine runs in COMMUNICATION, so the two peers are never in the same logical channel and neither receives onUserJoined.
  Android: `buildRtcEngineConfig` (AgoraEngineConfig.kt:26-30) never sets `RtcEngineConfig.mChannelProfile`, whose SDK default is 1 = LIVE_BROADCASTING (verified in the 4.6.4 bytecode and in the device log: `initializeEx channelProfile:1`, connection `channelProfile:1 clientRoleType:2`, `rtc.channel_profile":1`, never changed). COMMUNICATION is requested only through `ChannelMediaOptions.channelProfile` at join (AgoraManager.kt:126), and no profile change is logged after joinChannel.
  iOS: `AgoraManager.swift:99` sets `config.channelProfile = .communication` on the engine config (and again in the join options, line 145).
  Backend and everything else are equivalent (same App ID, channel name = booking id, PUBLISHER token, broadcaster, int uid), so the engine-level profile is the single code-level difference found.
  Residual alternative (about 25%): the iOS tester joined a different booking than 30a789c1-107b-48ee-b782-5e151b17751a (an earlier UAT used b0761385-...), which produces the identical symptom. About 10% is an unknown cause.
  AND-gate: could be two conditions (profile mismatch AND a booking mismatch), but a booking mismatch alone would also reproduce the symptom, so the cheap booking check must be done first or in parallel.
fix: (not applied - diagnose-only)
verification: (not applied)
files_changed: []

suggested_fix_direction: |
  Non-binding.
  1. AgoraEngineConfig.kt `buildRtcEngineConfig`: add `config.mChannelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION` (iOS parity, AgoraManager.swift:99). Keep the per-join COMMUNICATION option (iOS sets both). Extend AgoraEngineConfigTest to assert mChannelProfile == CHANNEL_PROFILE_COMMUNICATION so the default cannot return.
  2. Make remote presence observable: in the AgoraManager handler log (no uid/token/channel values) onUserJoined, onUserOffline(reason), onRemoteAudioStateChanged(state, reason), onClientRoleChanged and the join `elapsed`, so the next failure is visible in `adb logcat -s ClickMeCall` instead of only in the on-device agoraapi.log.
  3. Before or alongside the code change, rule out a booking mismatch: confirm the iOS client joined booking 30a789c1-... (iOS DEBUG build prints Agora info logs because `engine.setLogFilter(.info)`; or query bookings.client_joined_at / expert_joined_at for that id, which call.controller.js:72-78 stamps on POST /join).
  4. Re-test with both devices and capture on Android: `adb -t 1 pull /sdcard/Android/data/com.uptrendinvestments.clickmeapp/files/agoraapi.log` and confirm `channelProfile:0` at `initializeEx`, plus an `onUserJoined`-class callback once iOS joins.

confidence: medium (about 65% for the profile mismatch as the cause). High confidence (verified) on the facts that Android's effective engine profile is LIVE_BROADCASTING, iOS's is COMMUNICATION, the App ID/channel/role/uid/backend are equivalent, and the Android SDK never saw a remote user. The causal link (profile mismatch makes the peers invisible to each other) and "the per-join COMMUNICATION option does not take effect" are inferences. Deciding evidence: either the booking check (iOS never joined 30a789c1) or the two-sided re-run after the one-line change.

residual_risks: |
  - If the one-line change does not make the peers meet and the booking check shows the iOS client was in 30a789c1, look at the iOS side next: capture the iOS Agora info log (DEBUG) for the join and `didJoinedOfUid`.
  - After the profile fix, nothing past remote join has been exercised on a device: two-way audio, mute, speaker route, leave/rejoin, confirm-connection (09-HUMAN-UAT items 1, 2, 5).
  - Install-time NativeLibraryHelper "Failed to punch uncompressed elf" warnings are benign (the engine loaded and joined).
