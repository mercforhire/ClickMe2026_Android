---
status: partial
phase: 03-login-password-recovery-realtime
source: [03-VERIFICATION.md]
started: 2026-10-21T00:40:00Z
updated: 2026-10-21T00:40:00Z
---

## Current Test

[awaiting human testing]

## Tests

### 1. Login on device (Samsung SM-A075F)
expected: Wrong password shows the server message inline under the password field; a correct login routes to the right destination for account state and mode; offline shows the error dialog.
result: [pending]

### 2. Full password recovery loop with a real OTP email
expected: Forgot Password sends the code (600 ms "Code Sent" hold) → Set New Password accepts the emailed code (SMS/OTP autofill suggestion appears; focus moves to the first failing field on a bad submit) → Password Changed (glow, one-shot entrance, TalkBack focus on headline) → Go to Login pre-fills the email → login with the new password succeeds.
result: [pending]

### 3. Realtime events visible in debug Logcat
expected: With a debug build logged in, events triggered from iOS (message.*, typing.*, user.status, booking.update, call.ended) appear in `adb logcat -s Realtime` with no token in the output.
result: [pending]

### 4. Reconnect behavior
expected: After airplane mode on/off, the socket reconnects within ~2 s; after a forced token refresh it reconnects with the new token. Note: a graceful server close (e.g. Railway redeploy) may only be detected after the ~45 s ping timeout.
result: [pending]

### 5. Lifecycle, accessibility and release build
expected: Screens survive rotation and 200% font scale; socket disconnects on background and reconnects on foreground; a signed prodRelease (R8) build logs in and connects.
result: [pending]

## Summary

total: 5
passed: 0
issues: 0
pending: 5
skipped: 0
blocked: 0

## Gaps
