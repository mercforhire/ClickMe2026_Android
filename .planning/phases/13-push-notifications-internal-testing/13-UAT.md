---
status: partial
phase: 13-push-notifications-internal-testing
source: [13-VERIFICATION.md]
started: 2026-10-28T18:30:00Z
updated: 2026-10-28T21:00:39.786Z
---

## Current Test

[testing complete]

## Tests

### 1. SC-2 / PUSH-04 tap routing from a killed and a backgrounded app (system-displayed notification)
expected: On the Samsung SM-A075F with a real google-services.json and the backend D-04 deploy: kill the app, send booking/chat/session pushes, tap each; repeat with the app backgrounded (not killed), and with an open chat thread on screen. Each tap routes to the right screen in the right tab, Back order correct, mode switches first when needed, and warm-tap extras reach MainActivity.onNewIntent.
result: issue
reported: "app not receiving push notification"
severity: major

### 2. REL-05 / SC-5 build with testers (release checklist steps 2-6)
expected: A signed AAB is on the Play internal testing track, shows "Available to internal testers", and the Play-installed build logs in and reaches Explore. D-22: one internal tester until Phase 16.
result: blocked
blocked_by: release-build
reason: "blocked" (upload keystore, keystore.properties, app/google-services.json and signed prodRelease AAB not yet created; release checklist steps 2-6 outstanding)

### 3. REL-04 / SC-5 drafts in Play Console (release checklist step 7)
expected: App content shows the Data safety form and the FOREGROUND_SERVICE_MICROPHONE declaration saved as draft or complete (pasted from 13-PLAY-CONSOLE-DRAFTS.md); the store listing has no empty required fields.
result: blocked
blocked_by: third-party
reason: "blocked" (Play Console drafts from release checklist step 7 not yet pasted)

### 4. PUSH-01 live token registration
expected: After the backend deploy and adding google-services.json: one device_tokens row with platform = android after login / foreground return, a re-register after token rotation, the row gone after logout, and no pushes for the previous user on that device.
result: blocked
blocked_by: server
reason: "blocked" (backend D-04 push commits not deployed: prod /health 3274175 vs master 92b4827; device_tokens row, rotation and logout not yet checked. Partial evidence 2026-10-28: on SM-A075F a foreground return sent POST /api/v1/notifications/tokens -> 200; installed build has Firebase project clickme-a006b)

### 5. PUSH-02 permission prompt and channels on Android 13+
expected: Prompt appears once after login (never again after logout/login); foreground pushes post on the five channels with the ClickMe icon and accent; background pushes land on the category channel after the 13-02 deploy (general before it); App info lists the five channels.
result: pass

### 6. PUSH-05 and REV-01 on device
expected: Notifications list from both bells grouped Today / Yesterday / Earlier, a foreground push reloads it, rows route like pushes, payout/account rows inert; Write Review from session/ended and review/nudge pushes, Booking Summary and the Past card; read-only state and thanks snackbar; 200% font and TalkBack pass.
result: pass

### 7. Lock-screen privacy of system-displayed background pushes (REVIEW WR-02)
expected: A decision on whether backgrounded pushes may show full title/body on the lock screen. If not, set lockscreenVisibility on the channels (permanent once shipped, so before the first internal upload) and/or in the backend payload.
result: pass
decision: "Keep Android default lock-screen visibility for system-displayed background pushes (WR-02 accepted); foreground pushes stay VISIBILITY_PRIVATE. Users can hide content via system lock-screen settings."

## Summary

total: 7
passed: 3
issues: 1
pending: 0
skipped: 0
blocked: 3

## Gaps

- gap_id: G-13-1
  truth: "On a real device with google-services.json and the backend D-04 deploy, booking/chat/session pushes arrive and each tap (killed and backgrounded app) routes to the right screen in the right tab"
  status: failed
  reason: "User reported: app not receiving push notification"
  evidence: "2026-10-28: token registration works (POST notifications/tokens -> 200 on device, Firebase project clickme-a006b); suspect backend FIREBASE_SERVICE_ACCOUNT_JSON project mismatch or undeployed D-04 commits"
  severity: major
  test: 1
  artifacts: []
  missing: []
