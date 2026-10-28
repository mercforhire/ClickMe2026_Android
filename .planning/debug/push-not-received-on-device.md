---
status: diagnosed
trigger: "app not receiving push notification (UAT 13 Test 1, gap G-13-1)"
created: 2026-10-28T21:05:00Z
updated: 2026-10-28T21:25:00Z
goal: find_root_cause_only
---

## Current Focus

hypothesis: Test 1 was exercised while the installed APK carried no Firebase config (no FCM token, nothing registered) and prod ran the old backend. Both preconditions in Test 1's own `expected` text ("a real google-services.json and the backend D-04 deploy") are now met, and pushes now arrive. Not a code defect.
test: (done) device event log + APK contents + GMS c2dm log + backend push.js at 3274175 and master
expecting: n/a
next_action: Return diagnosis. Caller re-runs Test 1 against the current build and prod 2a7cf19 (steps in Resolution.suggested_next).

known_pattern_candidate: none (no .planning/debug/knowledge-base.md exists)
bug_class: Bohrbug-ish (environment/config state), not concurrency

## Symptoms

expected: Booking/chat/session pushes arrive on the Samsung SM-A075F (com.uptrendinvestments.clickmeapp) and each tap routes correctly (killed + backgrounded).
actual: User reported verbatim "app not receiving push notification".
errors: none reported
reproduction: 13-UAT.md Test 1
started: UAT 2026-10-28 (started 14:30 EDT, results committed 17:00:39 EDT)

## Eliminated

- hypothesis: Deployed backend (3274175) drops Android tokens or builds an APNs-only payload, so the undeployed D-04 commits are required for Android delivery.
  evidence: `git show 3274175:src/services/push.js` selects `token, device_id` with NO platform filter and sends to every row with `notification:{title,body}`, `data`, `android:{priority:'high'}`. Master's diff (6daf5c0/abde83a) only adds channelId, category/sub_category in data and visibility. Those affect channel mapping and tap routing, not delivery.
  timestamp: 2026-10-28

- hypothesis: Backend suppresses push when the recipient is online/on the socket, or for the sender, or for some event types.
  evidence: chats.controller.js (3274175) sends `sendPushToUser(recipientId)` on EVERY message ("offline gate removed" quick-260805-mrl, throttle removed quick-260805-phj). events.js dispatchPushForEvent covers request/confirmed/declined/cancelled/expired/rescheduled/completed. Only gate in push.js is `notification_preferences.push_enabled` (default on when no row).
  timestamp: 2026-10-28

- hypothesis: Firebase project mismatch between app and backend credential (messaging/mismatched-credential).
  evidence (indirect, not a direct read of the Railway var): app APK project_id clickme-a006b / sender 790325957439 == ios-reference/ClickMe2026/GoogleService-Info.plist PROJECT_ID/GCM_SENDER_ID (same values). Backend quick task 260812-x2z records a LIVE production FCM error "APNs device token is disabled." for an iOS token; FCM returns that only after it accepts the sending credential for the token's project, so Railway's FIREBASE_SERVICE_ACCOUNT_JSON authenticates against the project iOS uses. push.js also keeps the token on mismatched-credential (NEVER_STALE_CODES) and only logs it. Still needs the user's confirmation of the Railway var's project_id (cannot be read: .env hook).
  timestamp: 2026-10-28

- hypothesis: App-side wiring (manifest service, POST_NOTIFICATIONS, channels, stopped/standby/doze) blocks delivery.
  evidence: `dumpsys package` resolves com.google.firebase.MESSAGING_EVENT -> ClickMeMessagingService (exported=false, correct); POST_NOTIFICATIONS granted=true; stopped=false; `am get-standby-bucket` = 10 (ACTIVE); GMS 26.41.31 present; device clock correct (Oct 28 2026 EDT); five channels exist (Test 5 passed). 9 FCM messages were delivered to the app between 16:34:24 and 17:00:11 and rendered (foreground via PushNotificationFactory, background via the Firebase SDK).
  timestamp: 2026-10-28

## Evidence

- timestamp: 2026-10-28T17:0x EDT
  checked: Firebase config in the two dev-debug APKs (python zipfile scan of resources.arsc)
  found: app/build/outputs/apk/dev/debug/app-dev-debug.apk (built 13:21) has NO clickme-a006b, NO 790325957439, NO google_app_id/gcm_defaultSenderId. app/build/intermediates/apk/dev/debug/app-dev-debug.apk (built 16:21, the installed one: lastUpdateTime 16:21:15) HAS all of them. app/google-services.json mtime = 16:20:13; processDevDebugGoogleServices output = 16:20.
  implication: Until 16:21:15 the build on the device could not receive FCM. app/build.gradle.kts applies the google-services plugin only if the file exists, and FirebasePushTokenSource.firebaseAvailable() == false makes currentToken() null, so FcmPushTokenRegistrar.register() returns before any POST (no device_tokens row, nothing for the backend to push to). Matches Test 1's own precondition text.

- timestamp: 2026-10-28
  checked: `adb logcat -d -b events` (retained from 14:45) and `-b main` (from 16:26)
  found: No notification_enqueue / notify() by the app anywhere in 14:45 -> 16:34:24. First FCM delivery is 16:34:24. GMS registered the app with FCM at 16:30:23 and 16:31:35 (`c2dm [19,com.uptrendinvestments.clickmeapp:264131035]` then `[-19,SUCCESSFUL]`), token POSTs -> 200 from 16:26:39 (ClickMeHttp).
  implication: Clean cause/effect: no pushes while the build had no Firebase; first push 4 minutes after the first FCM registration on the Firebase build.

- timestamp: 2026-10-28
  checked: FCM deliveries in the log (GMS `c2dm [-7,...]`, AlarmManager `FcmRetry`, app `FirebaseMessaging: Unable to log event`)
  found: 16:34:24 five pushes in <1 s on five channels (bookings, sessions, messages, account_payouts, general; this is Test 5). 16:39:06/11/16 three pushes (session, review, payout; Test 6; tags push:session:<booking>, push:review:<booking>). 17:00:11 one background push with the screen off, rendered by the Firebase SDK as `FCM-Notification:368495175` on channel `messages` (Test 7 lock-screen check), visible in the shade at 17:00:26.
  implication: Device -> FCM -> app pipeline works in foreground and backgrounded (cached process) states. `channel=messages` for the background push means the payload carried android channelId, which only the D-04 backend code (6daf5c0+) emits.

- timestamp: 2026-10-28
  checked: Prod `/health` now vs. gap text
  found: `curl https://clickme-backend-production.up.railway.app/health` -> {"commit":"2a7cf19"} at 17:04 EDT. Backend master 2a7cf19 was committed 16:52:34, so prod was redeployed after that. The gap/Test 4 text (3274175 vs 92b4827) is stale.
  implication: All D-04 commits (channelId, category, visibility private) are live now.

- timestamp: 2026-10-28
  checked: Who sent the 16:34 and 16:39 pushes
  found: Their payloads carried channelId/category (D-04 code, >= 6daf5c0), but per the Test 4 text prod was still 3274175 then. So those were sent by local/scripted code with a clickme-a006b credential, not provably by Railway. 17:00:11 may be Railway 2a7cf19 (deployed 16:52-17:04) or a local script; the logs cannot tell.
  implication: Delivery from Railway itself is the one leg still unproven by direct observation (see remaining_unverified).

- timestamp: 2026-10-28
  checked: Killed-app and tap-routing coverage
  found: App process pid 15108 has run continuously since 16:21:15 (dumpsys activity: startUpTime -43m). No `notification_clicked` for the FCM-displayed notification. Clicks seen (16:34:53-16:35:52) were only on foreground-posted `push:*` notifications.
  implication: Test 1's literal scenarios (process killed, background tap routing, warm onNewIntent) have NOT been exercised on the Firebase build. They are untested, not failing.

- timestamp: 2026-10-28
  checked: Backend token model (notifications.controller.js registerToken, 3274175)
  found: One token row per account, last registration wins (delete-own + evict-token-from-others + insert). Android re-POSTs on every foreground return (16:35:53, 16:59:41, 17:00:24).
  implication: If the same account is also signed in on iOS, the two devices steal the row from each other; pushes then reach only the last registrant. Test-setup hazard, not a code bug.

## Resolution

root_cause: |
  No code defect found in the Android FCM path or in backend push.js (deployed 3274175 or master).
  The reported failure is explained by test-environment state: Test 1 was run (UAT started 14:30 EDT) while the installed APK was built without app/google-services.json (13:21 build; verified to contain no Firebase config), so FirebaseApp was never initialised, no FCM token was ever obtained or registered, and the backend had no Android device_tokens row to push to (and prod ran the pre-D-04 backend). The Firebase-enabled APK was installed at 16:21:15; from then on pushes are delivered (9 observed, foreground and screen-off background, correct channels).
  Confidence: high that no code fix is needed for "app not receiving push"; medium-high that the report predates 16:21 (the event log shows zero app notifications 14:45-16:34, but the exact time the user answered Test 1 is not recorded).
fix: (not applied - diagnose only)
verification: (n/a)
files_changed: []

remaining_unverified: |
  1. Railway-originated delivery (credential leg): not directly observed. Disfavoured mismatch hypothesis, see Eliminated.
  2. Process-killed delivery on Samsung One UI (deep sleep / "Put unused apps to sleep" / Settings Force stop all block FCM).
  3. Tap routing from the system-displayed notification (cold and warm).
  4. Same-account iOS/Android token stealing; notification_preferences push_enabled=false rows.

suggested_next: |
  No code change. Re-run Test 1 on the current build (installed 16:21:15) against prod 2a7cf19:
  a) Backgrounded: Home, trigger a REAL prod event for the Android user (chat message from another account), confirm arrival, tap, check route.
  b) Killed: swipe the app away in Recents (NOT Settings > Force stop), trigger again, tap.
  c) Open chat thread on screen: confirm no duplicate for that thread (D-13).
  While doing it run: adb logcat -b main | grep -E "c2dm|FirebaseMessaging|FCM-Notification|ClickMeHttp".
  Update 13-UAT.md: Test 1 -> re-test; Test 4 reason is stale (prod = 2a7cf19; POST 200 observed).
  If (a) or (b) fails, branch by evidence: nothing in logcat and no c2dm -7 -> Railway side (logs for "[push] send error (non-stale)" code messaging/mismatched-credential or "[push] fan-out init failed"; device_tokens row platform/owner; notification_preferences). c2dm -7 present but nothing shown -> app/OEM.
