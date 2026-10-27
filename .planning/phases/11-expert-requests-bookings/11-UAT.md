---
status: complete
phase: 11-expert-requests-bookings
source: [11-VERIFICATION.md]
started: 2026-10-26T18:58:34Z
updated: 2026-10-27T04:08:33.247Z
---

## Current Test

[testing complete]

## Tests

### 1. SC-2 / SC-3 cross-client — iOS client books; Android expert accepts/declines; both join the call
expected: Request appears in Incoming Requests; Confirmed screen shows; the iOS client sees the decline; both sides are in the same Agora call (PRD Week 5 checkpoint).
result: issue
reported: "Android Version shows error: cannot join session."
severity: blocker

### 2. SC-4 cross-client — reschedule accept/decline, SLOT_UNAVAILABLE, cancel
expected: Accept/decline of an iOS-proposed reschedule succeed from the Summary; back-to-back bookings produce "That time is no longer available" and the booking stays; cancelling a free and a held paid booking lands on the Past tab with the snackbar.
result: pass

### 3. SC-1 device run — Dashboard, Stripe onboarding, calendar
expected: Dashboard shows pending count, reschedule banner, Today's Sessions with Join, weekly earnings and the payout onboarding card; the card opens a Stripe Custom Tab and the status refreshes on return; Add to Calendar opens the editor with the right title, times and description; no flicker on quiet reloads; 200% font scale and TalkBack usable.
result: pass

### 4. Prohibition review — no logging of client messages, reasons, client names or the Stripe onboarding URL
expected: No logging of that data on expert screens (grep pass is non-authoritative; human review recommended).
result: pass

### 5. Re-test after 11-16 (G-11-1 fix) — Android expert and iOS client join the same call
expected: Join from the Dashboard card, an Upcoming card and the Booking Summary opens the call with no "Couldn't join session" dialog; two-way audio with the iOS client; mute and speaker work; leave then re-join works. (Also clears 09-HUMAN-UAT items 1, 2 and 5.)
result: issue
reported: \"The call joined successfully, but it's stuck at \\"waiting for the other person.\\"\"
severity: major
note: "First attempt (2026-10-26 19:10) ran a stale build installed 17:09, before 11-16 merged at 18:34 — logcat showed the old 'engine create failed NullPointerException' line without a frame. Fixed APK installed 19:13; re-test pending."

### 6. Re-test after 11-17 (G-11-5 fix) — Android expert and iOS client see and hear each other
expected: Fresh APK installed (lastUpdateTime after the 11-17 / WR fix merges). Android expert and iOS client open the same booking's call inside the join window. Within a few seconds both sides leave "waiting for the other person" and show the active call; two-way audio works; mute and speaker work; leave then re-join reconnects both sides. logcat shows a remote-user-joined line and agoraapi.log shows channelProfile:0 at initializeEx. (Also clears 09-HUMAN-UAT items 1, 2 and 5.)
result: issue
reported: "Still stuck waiting for the other person."
severity: major
note: "First attempt (2026-10-26 ~21:35) ran a stale build installed 19:16, before 11-17 (channel profile fix) merged at 20:16 — user saw both phones stuck at 'waiting for the other person', which is the pre-fix G-11-5 symptom and does not test 11-17. Current HEAD APK (Gradle up-to-date, includes 11-17 + WR-01/WR-02) installed 21:39:38; second attempt on that build also failed."

### 7. Re-test after 11-18 (G-11-6 server fix) — Android expert and iOS client meet in the same call
expected: Backend live at 4c694e7, Android build installed 21:39:38 (after the last app commit 20:49:51). Same confirmed booking on both phones inside the join window: both leave "waiting for the other person" and show the active call within a few seconds; two-way audio, mute, speaker, leave and re-join work; logcat shows `remote joined elapsed=` and `remote audio state=2`. (Also clears 09-HUMAN-UAT items 1, 2 and 5.)
result: pass
evidence: "logcat 2026-10-27 00:02:23 — conn state=3 reason=1, remote joined elapsed=148, remote audio state=2 reason=6; user confirmed pass."

## Summary

total: 7
passed: 4
issues: 3
pending: 0
skipped: 0
blocked: 0

## Gaps

- gap_id: G-11-1
  truth: "Both sides join the same Agora call inside the join window (Dashboard card, Upcoming card, Summary) — Android expert joins an iOS client's booking"
  status: resolved
  resolved_by: 11-16-PLAN.md
  resolved_at: 2026-10-26
  reason: "User reported: Android Version shows error: cannot join session."
  severity: blocker
  test: 1
  root_cause: "AgoraManager.createEngine() throws NullPointerException on every in-app voice join (client and expert alike). Inside `RtcEngineConfig().apply { mContext = context.applicationContext }` the bare `context` resolves to RtcEngineConfig.getContext() (still-null mContext), not AgoraManager's injected @ApplicationContext — so the engine is never created, join() returns false, and CallCenter shows the generic 'Couldn't join session' dialog. Confirmed by device logcat (POST /join 200 → CallService started → 'engine create failed NullPointerException', twice) and compiled bytecode. Regression from the Phase 9 port of the Phase 2 spike; no test covers AgoraManager (CallCenter tests use a fake CallEngine)."
  artifacts:
    - path: "app/src/main/java/com/uptrendinvestments/clickmeapp/managers/AgoraManager.kt"
      issue: "Line 132: `context` shadowed by RtcEngineConfig.getContext() inside apply → NPE; catch blocks log only the exception class, hiding the cause; RtcEngine.create null return not handled explicitly"
    - path: "app/src/main/java/com/uptrendinvestments/clickmeapp/managers/CallCenter.kt"
      issue: "Lines 266-268 map join() == false to JoinError(Unknown) — working as designed, only surfaces the failure"
  missing:
    - "Assign mContext from AgoraManager's own context (e.g. capture appContext before apply, or this@AgoraManager.context)"
    - "Log exception class + top frame (no token/channel/uid) in createEngine/join catch blocks; log non-zero joinChannel return code"
    - "Treat a null RtcEngine.create result as an explicit failure"
    - "Regression test: extract engine config building into a testable function and assert mContext != null"
    - "Re-run 09-HUMAN-UAT items 1, 2, 5 and 11-UAT test 1 on device after the fix"
  debug_session: ".planning/debug/android-expert-cannot-join-session.md"

- gap_id: G-11-5
  truth: "Android expert and iOS client join the same Agora call and hear each other (re-test after 11-16)"
  status: resolved
  resolved_by: 11-17-PLAN.md
  resolved_at: 2026-10-26
  reason: "User reported: The call joined successfully, but it's stuck at \"waiting for the other person.\""
  severity: major
  test: 5
  evidence: "Fixed build (installed 19:13). logcat 19:17:51 and 19:18:31: CallService started -> conn state=2 reason=0 -> conn state=3 reason=1 (CONNECTED / JOIN_SUCCESS); no 'engine create' or 'joinChannel failed' line. 19:18:26 conn state=1 reason=5 (left, after ~35 s). onUserJoined/onUserOffline are not logged, so whether the remote uid ever arrived is not visible. User: the iOS client was in its call screen at the same time and also showed 'waiting for the other person' — both sides connected, neither sees the other."
  root_cause: "Probable (~65%): Android's Agora engine/connection runs LIVE_BROADCASTING because buildRtcEngineConfig never sets RtcEngineConfig.mChannelProfile (SDK default 1; device agoraapi.log shows initializeEx channelProfile:1, connection channelProfile:1 clientRoleType:2, rtc.channel_profile=1 once and never changed after joinChannel), while iOS sets config.channelProfile = .communication on its engine config (AgoraManager.swift:99). App ID, channel (= booking id), PUBLISHER token, broadcaster role and int uid are equivalent on both sides, and the Android SDK raised no remote-user callback across 3 joins (one 2m14s). Residual alternative (~25%): the iOS tester joined a different booking than 30a789c1-107b-48ee-b782-5e151b17751a."
  artifacts:
    - path: "app/src/main/java/com/uptrendinvestments/clickmeapp/managers/AgoraEngineConfig.kt"
      issue: "Lines 26-30: buildRtcEngineConfig sets mContext/mAppId/mEventHandler only; mChannelProfile stays at the SDK default 1 (LIVE_BROADCASTING) while iOS initializes the engine in COMMUNICATION (AgoraManager.swift:99)"
    - path: "app/src/main/java/com/uptrendinvestments/clickmeapp/managers/AgoraManager.kt"
      issue: "Line 126: COMMUNICATION set only in ChannelMediaOptions; device log shows no re-profile after joinChannel. Lines 83-95: onUserJoined/onUserOffline/onRemoteAudioStateChanged/onClientRoleChanged not logged, so remote presence is invisible in logcat"
    - path: "app/src/test/java/com/uptrendinvestments/clickmeapp/managers/AgoraEngineConfigTest.kt"
      issue: "Does not assert the channel profile, so the LIVE_BROADCASTING default could not be caught"
  missing:
    - "In buildRtcEngineConfig set config.mChannelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION (iOS parity); keep the per-join COMMUNICATION option"
    - "Extend AgoraEngineConfigTest to assert mChannelProfile == CHANNEL_PROFILE_COMMUNICATION"
    - "Log (no uid/token/channel values) onUserJoined, onUserOffline(reason), onRemoteAudioStateChanged(state, reason) and onClientRoleChanged in the AgoraManager handler"
    - "Confirm the iOS tester was in the same booking (iOS Xcode console, or bookings.client_joined_at / expert_joined_at)"
    - "Two-sided re-test of 11-UAT test 5 on a freshly installed APK (check lastUpdateTime); pull agoraapi.log and confirm channelProfile:0 at initializeEx plus a remote-join callback and Active status; then 09-HUMAN-UAT items 1, 2, 5"
  debug_session: ".planning/debug/android-ios-call-peers-never-meet.md"

- gap_id: G-11-6
  truth: "Android expert and iOS client see and hear each other in the same Agora call (re-test after 11-17, fresh APK installed 21:39:38)"
  status: resolved
  resolved_by: 11-18-PLAN.md
  resolved_at: 2026-10-27
  reason: "User reported: Still stuck waiting for the other person."
  severity: major
  test: 6
  evidence: "Fresh build (installed 21:39:38) confirmed channelProfile:0 / clientRoleType:1 in agoraapi.log and conn state=3 reason=1 on the lowercase booking channel; the new remote-presence logging (onUserJoined/onUserOffline/onRemoteAudioStateChanged) never fired across 30+ s alone in the channel. User: two iOS devices meet fine; Android<->iOS never does."
  root_cause: "Probable (~85-90%): Android and iOS join different Agora channels whose names differ only by letter case. Backend call.controller.js join uses the raw URL param (bookingId = req.params.id) as the token channel and the returned channel_name (agora.js channelName = bookingId). iOS builds the join URL with UUID.uuidString (UPPERCASE in Swift, ClickMeAPI.swift url(_:id:)); Android uses the lowercase id from JSON. Postgres matches uuids case-insensitively, so both joins authorize and succeed with valid tokens for their own string. iOS<->iOS and Android<->Android each share one casing, which is why only the cross-platform pair fails. Also the real cause of G-11-5; the 11-17 profile change is correct and stays."
  artifacts:
    - path: "/Users/LeonChen/Documents/ClickMe2026 Backend/src/controllers/call.controller.js"
      issue: "join: bookingId = req.params.id (raw URL casing) passed to generateToken instead of the loaded booking.id"
    - path: "/Users/LeonChen/Documents/ClickMe2026 Backend/src/services/agora.js"
      issue: "channelName = bookingId verbatim"
    - path: "/Users/LeonChen/Documents/ClickMe2026/ClickMe2026/Network/ClickMeAPI.swift"
      issue: "url(_:id:) uses id.uuidString (uppercase) for join/confirm/end"
    - path: "app/src/main/java/com/uptrendinvestments/clickmeapp/network/ (bookings join call)"
      issue: "Sends the lowercase booking id, so its channel_name differs in case from iOS's"
  missing:
    - "Make both platforms land on one channel string: backend canonicalizes the channel to booking.id / lowercase (fixes installed iOS builds, needs Railway deploy + backend test), and/or Android sends the booking id uppercased in the call endpoints' path to match iOS (no backend deploy; Android<->Android also stays consistent). Never re-case channel_name locally: the token is bound to the exact signed string."
    - "Optional: log a no-value boolean (channel has uppercase) at join"
    - "Two-sided re-test of 11-UAT test 6 on a fresh APK; expect a 'remote joined' logcat line and Active on both phones; then 09-HUMAN-UAT items 1, 2, 5"
  debug_session: ".planning/debug/android-ios-peers-never-meet-after-profile-fix.md"
