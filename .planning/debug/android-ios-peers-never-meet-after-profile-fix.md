---
status: diagnosed
trigger: "Phase 11 UAT Test 6 / gap G-11-6: after the 11-17 channel-profile fix (fresh APK, lastUpdateTime 21:39:38), Android expert (devDebug, production backend) and iOS client both join the same booking's call and BOTH still sit at 'waiting for the other person'. User: 'Still stuck waiting for the other person.'"
created: 2026-10-27T01:46:00Z
updated: 2026-10-27T02:10:00Z
goal: find_root_cause_only
---

## Current Focus

hypothesis: CONFIRMED at the code level (about 85-90%; one link needs a human observation, see Resolution). The two peers join DIFFERENT Agora channels because the channel name differs only in letter case. The backend join handler uses the raw URL path parameter `req.params.id` as the Agora channel name (and in the token), without canonicalizing it to the booking row's id. Android sends the booking id lowercase (as it arrives in JSON). iOS builds the URL with Swift `UUID.uuidString`, which is UPPERCASE, so the iOS channel is the uppercase form of the same string. Agora identifies a channel by its exact name, so both sides join successfully (each with a token minted for its own string), neither sees a peer, and both stay at "waiting for the other person".
test: (done) read both clients' join paths and the backend handler end to end; ran the real backend `join` handler offline with stubbed DB/sockets, once with a lowercase id (Android) and once with the uppercase id (iOS); compared the Android device logs for both builds.
expecting: Decisive human check (cannot be done from here, needs the iOS device): the iOS Xcode DEBUG console prints `-> POST .../bookings/<ID>/join` and `<- 200 ... {"channel_name":"<ID>"...}` (NetworkService.swift:238-262); the `<ID>` will be UPPERCASE. A two-sided re-test with the channel name made identical on both sides should show a remote-join line and Active status on both phones.
next_action: none for this agent (diagnose-only). Hand to gap-closure planning (/gsd-plan-phase 11 --gaps): canonicalize the channel name server-side (backend repo) and re-run the two-sided test.
bug_class: bohrbug
reasoning_checkpoint: null
tdd_checkpoint: null

## Symptoms

expected: Both sides join the same booking's call; within seconds both leave "waiting for the other person" and show the active call with two-way audio.
actual: Both phones (Android expert + iOS client) sit at "waiting for the other person".
errors: none. Android logcat: CallService started, `conn state=2 reason=0`, `conn state=3 reason=1` (CONNECTED / JOIN_SUCCESS) on every join; no `joinChannel failed`, no `error code=`; no `remote joined`, `remote offline` or `remote audio state` line ever (the build now logs all three).
reproduction: 11-UAT.md test 6, two real devices, production backend. Android joins: 21:40:24 (11 s) and 21:40:38 (still connected at 21:41:10, 30+ s) on the fresh build; 21:37:00 (2 m 10 s) on the stale pre-11-17 build. Same symptom as G-11-5 (earlier booking 30a789c1-..., 3 joins).
started: First time Android and iOS were run against each other in the in-app voice path (Phase 2 test 9, the Agora spike with an iOS peer, was skipped). Never worked.

## Eliminated

- hypothesis: Stale build (the test did not run the 11-17 fix)
  evidence: Given and re-checked in the logs. The 21:37:00 join is the old build (pid 16995, Agora API log `initializeEx channelProfile:1`, connection `clientRoleType:2`). The 21:40:24 and 21:40:38 joins are the fresh build (pid 13469): `initializeEx channelProfile:0`, connection `channelProfile:0 clientRoleType:1`, `setParametersInternal {"rtc.channel_profile":0}`. So 11-17 took effect on device and the peers still did not meet.
  timestamp: 2026-10-27T01:55:00Z
- hypothesis: LIVE_BROADCASTING vs COMMUNICATION channel-profile mismatch (the G-11-5 conclusion) is the cause
  evidence: Fixed and confirmed on device (above): Android is now COMMUNICATION at engine and connection level, matching iOS (`config.channelProfile = .communication`, AgoraManager.swift:99), and the symptom is unchanged in all fresh-build joins. The profile was a real difference but is not what keeps the peers apart. Keeping the 11-17 change is still right (iOS parity).
  timestamp: 2026-10-27T01:56:00Z
- hypothesis: Different Agora App ID / project between Android and iOS
  evidence: Android `AGORA_APP_ID` (app/build.gradle.kts:52 and :63, both flavors) and iOS `AgoraAppId` (Environments.plist, all three environments; `Current` = Production; read via AppEnvironment.current.agoraAppId, MeetingCallViewModel.swift:426) are the same 32-character value (compared by eye and by grep, not recorded here). The backend mints tokens for one `AGORA_APP_ID` env var for both roles (agora.js:57-59). Both sides reached CONNECTED, which a mismatched App ID/token would not allow.
  timestamp: 2026-10-27T01:57:00Z
- hypothesis: Different backend environment (dev vs prod)
  evidence: Android dev and prod flavors and all three iOS environments use the same production Railway URL (build.gradle.kts:46,57; Environments.plist).
  timestamp: 2026-10-27T01:57:30Z
- hypothesis: Token / uid / API contract differs between platforms (string uid vs int uid, role, joinChannelWithUserAccount)
  evidence: One backend handler serves both roles and platforms (call.controller.js:join); same `buildTokenWithUid`, `RtcRole.PUBLISHER`, int uid (uuidToUid32 of the caller's JWT user id, agora.js:87,99). Both clients use the int-uid join (Android `joinChannel(token, channel, uid, options)`, iOS `joinChannel(byToken:channelId:uid:mediaOptions:)`). The only per-request input that reaches the token is the URL id string, which is where the real difference is (see Evidence).
  timestamp: 2026-10-27T01:58:00Z
- hypothesis: Android leaving / POST /bookings/:id/end tears the session down, or iOS ending does
  evidence: /end records metadata only and never changes status; the Agora channel is untouched (call.controller.js end(), docs/ios/CALL-END-SEMANTICS.md). In the 21:40:38 join Android never left, and it was alone in the channel for 30+ s.
  timestamp: 2026-10-27T01:59:00Z
- hypothesis: Android UI state machine hides a real remote join
  evidence: The Agora API log (agoraapi.log), which lists every SDK callback that fires, has no user-joined, user-offline or remote-audio-state callback in any of the 3 sessions, and the app's own `remote joined` / `remote audio state` logs (AgoraManager.kt:81-101) never appear. The SDK itself never saw a remote user.
  timestamp: 2026-10-27T02:00:00Z
- hypothesis: iOS tester joined a different booking than Android (G-11-5 residual alternative)
  evidence: Not excluded by data, but now unlikely as the explanation: booking 28fac6fe-... is the booking the iOS client had just created and the expert accepted at 21:36:50 (logcat `POST .../booking-requests/28fac6fe-.../accept`), and the failure repeats across two different bookings (30a789c1-... earlier, 28fac6fe-... now) with the same symptom. The channel-name casing difference explains both without needing a wrong-booking mistake twice.
  timestamp: 2026-10-27T02:01:00Z
- hypothesis: Region / area code mismatch
  evidence: Android `areaCode:4294967295` (global, agoraapi.log initializeEx). iOS sets no area code (default global). No difference.
  timestamp: 2026-10-27T02:02:00Z

## Evidence

- timestamp: 2026-10-27T01:50:00Z
  checked: What channel string the Android client actually joins (g-11-6-agoraapi.log, g-11-6-logcat-app.txt)
  found: The Android channel id in `RtcEngine::joinChannel` and `onJoinChannelSuccess` is the booking id in LOWERCASE (28fac6fe-...), identical to the lowercase id in the Android request line `POST /api/v1/bookings/28fac6fe-.../join`. Android takes the id as it arrives in JSON (lowercase) and uses the response `channel_name` verbatim (AgoraManager.join passes it straight to joinChannel).
  implication: Android is in a lowercase-named channel.

- timestamp: 2026-10-27T01:52:00Z
  checked: Backend join handler (/Users/LeonChen/Documents/ClickMe2026 Backend/src/controllers/call.controller.js:join, src/services/agora.js:generateToken, src/routes/call.js)
  found: `const bookingId = req.params.id;` then `generateToken(bookingId, userId, sessionEnd)`; inside, `const channelName = bookingId;` and `RtcTokenBuilder.buildTokenWithUid(appId, cert, channelName, uid, PUBLISHER, ...)`. The response `channel_name` is that same string. The route `router.post('/bookings/:id/join', join)` has no param validation, no `router.param`, no lowercasing. The booking row is loaded with `.eq('id', bookingId)` (Postgres compares uuid case-insensitively, so an uppercase id still finds the row and the participant check, which compares JWT user id to DB columns, still passes), but the canonical `booking.id` from the row is never used for the channel.
  implication: The server echoes whatever casing the client put in the URL as the Agora channel name and signs the token for exactly that string. Two clients that send different casing get two different channels and two valid tokens.

- timestamp: 2026-10-27T01:54:00Z
  checked: iOS request path (ios: Network/ClickMeAPI+Call.swift:13, Network/ClickMeAPI.swift:112-114, Views/Screens/Call/Calling/MeetingCallViewModel.swift:~367-385 and :410-428, Managers/AgoraManager.swift:176-182, Model/Responses/JoinCallResponse.swift)
  found: `joinCall(id: UUID)` builds the URL with `endpoint.path.replacingOccurrences(of: ":id", with: id.uuidString)`. Swift `UUID.uuidString` returns uppercase hex. The bookingId is a `UUID`, so the iOS request is `POST /bookings/28FAC6FE-.../join`. iOS then joins with `config.channelName` from the response, unchanged (`joinChannel(using:)`). The iOS codebase already documents this exact trap elsewhere: ExpertiseEditorViewModel.swift:182 ("Swift's `.uuidString` returns uppercase but the backend looks tags up case-sensitively") and explicit `.uuidString.lowercased()` in ClickMeAPI+Booking.swift:51-56, RealtimeService.swift:191-210. The join/confirm/end endpoints do NOT lowercase. The backend also has uppercase-UUID handling (sameUser.js, quick 261016-o1a) because clients do send uppercase ids.
  implication: iOS is in an UPPERCASE-named channel.

- timestamp: 2026-10-27T02:03:00Z
  checked: Offline run of the real backend join handler (probe in /tmp, DB/socket/event stubs, dummy Agora creds, no network, no production data): called `join` once with the booking id lowercase (Android) and once with the same id uppercased (iOS)
  found: android `channel_name` == lowercase id: true; ios `channel_name` == UPPERCASE id: true; channel names identical (case-sensitive): FALSE; identical ignoring case: true.
  implication: The backend, as written, hands the two platforms channel names that differ only by case. This is the mechanism, reproduced against the real handler.

- timestamp: 2026-10-27T02:04:00Z
  checked: Why neither side shows an error
  found: Each token is minted for the exact channel string that client sent, so both joins succeed (Android `conn state=3 reason=1`; iOS reaches `didJoinChannel`/join completion, which sets myConnectionState = .ready and shows "waiting for the other person", MeetingCallViewModel.swift case (.ready, .waiting)). Two users in two different channels get no onUserJoined on either side. This also explains G-11-5 (booking 30a789c1-...) and the 11-17-independent failure here.
  implication: Symptom on both phones, no errors, no remote callbacks: fully explained.

- timestamp: 2026-10-27T02:05:00Z
  checked: Why same-platform pairs never hit this
  found: iOS client + iOS expert both send uppercase, Android + Android both send lowercase, so each same-platform pair lands in one channel. Only a mixed Android/iOS pair splits. No Android<->iOS call had ever been run before Phase 11 UAT.
  implication: Consistent with the history: the backend and iOS were validated iOS-to-iOS only.

- timestamp: 2026-10-27T02:06:00Z
  checked: Post-fix Android session shape (agoraapi.log, 21:40:38-21:41:10)
  found: Android in COMMUNICATION, broadcaster, `publishMicrophone:1`, `autoSubscribeAudio:1`, first local audio frame published, mute/unmute and speaker toggles work, no remote callback in 30+ s alone in the channel. Android's side of the join is correct and now identical to iOS in profile/role/options.
  implication: Nothing left to fix in the Android SDK configuration for this symptom; the divergence is the channel string.

## Resolution

root_cause: |
  The Agora channel name is case-divergent between the two platforms, so the peers are in different channels.
  Backend: `call.controller.js:join` takes `bookingId = req.params.id` (raw URL text) and passes it to `generateToken`, which uses it verbatim as `channelName` for both the token and the `channel_name` response field (agora.js `const channelName = bookingId`). The canonical `booking.id` from the loaded row is not used, and nothing normalizes case.
  iOS: `ClickMeAPI.url(_:id:)` uses `UUID.uuidString`, which is uppercase, so the iOS client joins channel "<BOOKING-ID-UPPERCASE>".
  Android: the booking id from JSON is lowercase, so Android joins channel "<booking-id-lowercase>".
  Agora identifies a channel by its exact name, so the two clients are in two distinct channels, each with a valid token, and neither ever receives onUserJoined.
  The earlier channel-profile finding (G-11-5) was a real Android/iOS difference and 11-17's fix is correct and confirmed on device, but it was not the cause of this symptom.
  Confidence: about 85-90%. Verified directly: Android channel is lowercase (device log); backend echoes URL casing (code + offline run of the real handler); iOS builds the URL from `uuidString` (source). Not directly observed: the iOS device's actual request/response (needs the iOS console) and Agora's byte-exact channel-name comparison (Agora docs say a channel is identified by its name; they do not state case sensitivity in the pages found; token binding to the exact channel string and standard behavior make it the expected result). Either human check below settles it.
  AND-gate: single cause is sufficient to explain G-11-5 and G-11-6 on its own. Candidate causes across categories were: code (backend echo, iOS uppercase, Android profile), config (App ID, flavor/environment, area code), environment (stale build, wrong device state), data (wrong booking). Config/environment/data candidates were eliminated above; the earlier code candidate (profile) is fixed and did not change the outcome. If the re-test after canonicalizing the channel still fails, look next at the iOS-side Agora info log (iOS DEBUG enables setLogFilter(.info)).
fix: (not applied - diagnose-only)
verification: (not applied)
oracle_type: (not applicable - diagnose-only)
files_changed: []

suggested_fix_direction: |
  Non-binding. The fix must make every client's channel string identical, and it must cover iOS builds already in the field, which send uppercase.
  1. Backend (authoritative, recommended; separate repo "ClickMe2026 Backend", needs a Railway deploy): in `call.controller.js:join` use the loaded row's canonical `booking.id` (or `bookingId.toLowerCase()`) when calling `generateToken`, and return that canonical value as `booking_id`/`channel_name`. Add a test in tests/call.test.js: join with an UPPERCASE `:id` returns the lowercase `channel_name`, equal to the one returned for the lowercase id. This one change fixes Android+iOS (and any older iOS build) without touching either app. The same raw-param pattern exists in `confirmConnection` and `end`, which only use it for DB lookups and are not affected.
  2. Optional belt and braces on iOS (the codebase is otherwise "done"): lowercase the id in `ClickMeAPI.url(_:id:)` or at least for the call endpoints, matching the existing `.uuidString.lowercased()` precedent. Not sufficient alone for older installed iOS builds.
  3. Android: no change needed for this bug. Keep using the response `channel_name` verbatim (do NOT lowercase or uppercase it locally: the token is bound to the exact string the server signed). Keep the 11-17 COMMUNICATION engine profile and the new remote-presence logging. Optionally log a boolean such as `channel has uppercase=<true|false>` (no value) at join so a future device log shows which casing the server returned without exposing the channel name.
  4. Re-test: two-sided 11-UAT test 6 on a fresh APK; expect a `remote joined` line in logcat tagged ClickMeCall, `remote audio state` lines, and Active status on both phones. Then 09-HUMAN-UAT items 1, 2, 5 (two-way audio, mute, speaker, leave/rejoin).
  Decisive human checks to confirm before or alongside the fix (any one is enough):
  a. iOS Xcode console (DEBUG build) on the next iOS join: the `-> POST .../bookings/<ID>/join` line and the `<- 200 ...` body show `channel_name`; expect uppercase.
  b. Or hit the join route for a test booking with both casings from a test account and compare `channel_name`.
  c. Or, with the backend unchanged, temporarily make Android request the join with the uppercase booking id and re-run the two-sided test: if the peers then meet, the casing is confirmed.

confidence: medium-high (about 85-90%).

residual_risks: |
  - If the peers still do not meet after the channel strings are identical, the iOS side is next (capture its Agora info log for the join and `didJoinedOfUid`); the Android side is already shown correct.
  - After the channel fix nothing past remote join has been exercised on a device: two-way audio, mute, speaker route, leave/rejoin, confirm-connection (09-HUMAN-UAT items 1, 2, 5).
  - The backend fix lives in another repo and another deploy pipeline; plan it as a cross-repo item with a deploy gate before the Android re-test.
  - Raw evidence in .planning/debug/evidence/ (agoraapi.log, agorasdk.log, logcat) pairs channel names with uids and was left uncommitted on purpose; redact before committing any of it.
