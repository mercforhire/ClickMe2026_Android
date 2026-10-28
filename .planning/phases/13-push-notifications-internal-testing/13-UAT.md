---
status: testing
phase: 13-push-notifications-internal-testing
source: [13-VERIFICATION.md]
started: 2026-10-28T18:30:00Z
updated: 2026-10-28T18:30:00Z
---

## Current Test

number: 1
name: SC-2 / PUSH-04 tap routing from a killed and a backgrounded app (system-displayed notification)
expected: |
  Each tap lands on the routed screen inside the correct tab (booking -> Bookings tab Booking Summary or
  Upcoming Booking; chat -> Chats tab thread; request -> Request Decision), Back unwinds through the tab root,
  and a client push in Expert mode switches mode first. On the warm tap the intent extras reach MainActivity
  (onNewIntent).
awaiting: user response

## Tests

### 1. SC-2 / PUSH-04 tap routing from a killed and a backgrounded app (system-displayed notification)
expected: On the Samsung SM-A075F with a real google-services.json and the backend D-04 deploy: kill the app, send booking/chat/session pushes, tap each; repeat with the app backgrounded (not killed), and with an open chat thread on screen. Each tap routes to the right screen in the right tab, Back order correct, mode switches first when needed, and warm-tap extras reach MainActivity.onNewIntent.
result: [pending]

### 2. REL-05 / SC-5 build with testers (release checklist steps 2-6)
expected: A signed AAB is on the Play internal testing track, shows "Available to internal testers", and the Play-installed build logs in and reaches Explore. D-22: one internal tester until Phase 16.
result: [pending]

### 3. REL-04 / SC-5 drafts in Play Console (release checklist step 7)
expected: App content shows the Data safety form and the FOREGROUND_SERVICE_MICROPHONE declaration saved as draft or complete (pasted from 13-PLAY-CONSOLE-DRAFTS.md); the store listing has no empty required fields.
result: [pending]

### 4. PUSH-01 live token registration
expected: After the backend deploy and adding google-services.json: one device_tokens row with platform = android after login / foreground return, a re-register after token rotation, the row gone after logout, and no pushes for the previous user on that device.
result: [pending]

### 5. PUSH-02 permission prompt and channels on Android 13+
expected: Prompt appears once after login (never again after logout/login); foreground pushes post on the five channels with the ClickMe icon and accent; background pushes land on the category channel after the 13-02 deploy (general before it); App info lists the five channels.
result: [pending]

### 6. PUSH-05 and REV-01 on device
expected: Notifications list from both bells grouped Today / Yesterday / Earlier, a foreground push reloads it, rows route like pushes, payout/account rows inert; Write Review from session/ended and review/nudge pushes, Booking Summary and the Past card; read-only state and thanks snackbar; 200% font and TalkBack pass.
result: [pending]

### 7. Lock-screen privacy of system-displayed background pushes (REVIEW WR-02)
expected: A decision on whether backgrounded pushes may show full title/body on the lock screen. If not, set lockscreenVisibility on the channels (permanent once shipped, so before the first internal upload) and/or in the backend payload.
result: [pending]

## Summary

total: 7
passed: 0
issues: 0
pending: 7
skipped: 0
blocked: 0

## Gaps
