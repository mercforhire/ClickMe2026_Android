---
status: complete
phase: 02-networking-session-sdk-spikes
source: [02-01-SUMMARY.md, 02-02-SUMMARY.md, 02-03-SUMMARY.md, 02-04-SUMMARY.md, 02-05-SUMMARY.md, 02-06-SUMMARY.md, 02-07-SUMMARY.md, 02-08-SUMMARY.md, 02-10-SUMMARY.md, 02-11-SUMMARY.md, 02-13-SUMMARY.md, 02-14-SUMMARY.md]
started: 2026-10-21T22:15:00Z
updated: 2026-10-21T21:28:51Z
---

## Current Test
<!-- OVERWRITE each test - shows where we are -->

[testing complete]

## Tests

### 1. Log in against the production backend
expected: Install the devDebug build on the Samsung device. Log in on the Login screen with a real, fully set-up account. The splash clears in about 5 s or less and you land in the 5-tab layout for the account's mode. The avatar in the top bar shows the profile photo.
result: pass

### 2. Auto-login after relaunch
expected: Swipe the app away from Recents (or force-stop it) and open it again. The splash clears and you land straight in the same home and mode, with no Login screen. (This is the EncryptedSharedPreferences check that can't run in unit tests.)
result: pass

### 3. Forced token refresh keeps you logged in
expected: On the Profile tab, tap the debug "Force token refresh" button. The status shows success, you stay logged in, and switching tabs or pulling data still works. Relaunch the app afterwards: still logged in (the rotated token was persisted).
result: pass

### 4. Offline launch routes from cache
expected: Turn on airplane mode, force-stop and relaunch. Within about 5 s the splash clears into the cached home (no crash, no Login screen). Turn airplane mode off and bring the app to the foreground: it keeps working normally.
result: pass
note: "Tabs show the \"Nothing here yet\" placeholder; expected, since tab content arrives in Phases 5+."

### 5. Log out clears the session
expected: Tap Log out on the Profile tab. You return to the Login screen. Force-stop and relaunch: you are still on Login (tokens and cached profile cleared). Logging in again works.
result: pass

### 6. Suspended account is logged out with a message
expected: Log in with a suspended account (or have the backend suspend a logged-in test account and then use the app). You are sent to Login and a dialog explains the account is suspended, with a way to contact support. No crash.
result: skipped
reason: "No backend access right now to suspend a test account; covered by SuspensionInterceptorTest, NetworkStackTest and AppViewModelTest."

### 7. Stripe spike: save a card through PaymentSheet (SetupIntent)
expected: Create ~/.clickme/spike-inputs.properties, install the :spikes:stripe app, log in, tap the save-card action. PaymentSheet opens in setup mode; enter test card 4000 0025 0000 3155 (or 4242…), confirm. The card appears in the spike's card list (logcat `setup:` / `methods:` lines).
result: issue
reported: "After clicking the \"Add card\" button, the Stripe modal screen shows momentarily and disappears."
severity: major

### 8. Stripe spike: 3DS challenge on a connected account
expected: In the Stripe spike, pay for the test booking with the saved 3DS card. Confirm returns 402 PAYMENT_REQUIRES_ACTION, the 3DS challenge screen opens, you complete it, and the retried confirm succeeds (logcat `confirm: 402`, `3ds: Completed`, `confirm-retry: 201`).
result: issue
reported: "Says client secret does not match any associated setup intent on this account."
severity: major

### 9. Agora spike: background audio with an iOS peer
expected: With a confirmed booking id in the spike inputs and the iOS app joined as the expert, install :spikes:agora, log in, Join. You hear each other. Press Home and leave it in the background for 5+ minutes: audio keeps flowing both ways, the ongoing-call notification stays, and `heartbeat:` lines keep logging with importance 125. Leave ends the call cleanly.
result: skipped
reason: "User skipped remaining Phase 2 verification on 2026-10-21 to move on to Phase 5."

### 10. Signed release bundle builds
expected: With the upload keystore at ~/keys/clickme-upload.jks and keystore.properties in place, `./gradlew bundleProdRelease` succeeds and `jarsigner -verify` on the AAB reports "jar verified".
result: skipped
reason: "User skipped remaining Phase 2 verification on 2026-10-21 to move on to Phase 5."

### 11. Signed R8 release build cold-starts and logs in
expected: Install the signed prodRelease APK on the device. It cold-starts through the splash without a crash, you can log in, relaunch auto-logs in, and Log out works (R8 did not strip anything needed).
result: skipped
reason: "User skipped remaining Phase 2 verification on 2026-10-21 to move on to Phase 5."

### 12. Internal-track upload registers the package
expected: Upload the signed AAB to Play Console's internal testing track. Play Console accepts it, and the app shows the package com.uptrendinvestments.clickmeapp with Play App Signing enrolled.
result: skipped
reason: "User skipped remaining Phase 2 verification on 2026-10-21 to move on to Phase 5."

## Summary

total: 12
passed: 5
issues: 2
pending: 0
skipped: 5
blocked: 0

## Gaps

- truth: "Tapping Add card in the Stripe spike opens PaymentSheet in setup mode and lets the user save a test card, which then appears in the card list"
  status: failed
  reason: "User reported: After clicking the \"Add card\" button, the Stripe modal screen shows momentarily and disappears."
  severity: major
  test: 7
  root_cause: ""
  artifacts: []
  missing: []
  debug_session: ""
- truth: "The Stripe spike completes a paid booking with a 3DS challenge on a connected account and the retried confirm succeeds"
  status: failed
  reason: "User reported: Says client secret does not match any associated setup intent on this account."
  severity: major
  test: 8
  root_cause: ""
  artifacts: []
  missing: []
  debug_session: ""
  hypothesis: "Same cause as test 7: the spike's pk_test_51Tha0r... key belongs to a different Stripe account than the production backend's secret key (iOS pk_live is 51U8OTJ...), so SetupIntent client secrets don't resolve."
