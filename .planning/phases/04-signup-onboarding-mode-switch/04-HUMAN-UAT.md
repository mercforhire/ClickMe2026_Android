---
status: partial
phase: 04-signup-onboarding-mode-switch
source: [04-VERIFICATION.md]
started: 2026-10-21T21:00:00Z
updated: 2026-10-21T21:00:00Z
---

## Current Test

[awaiting human testing — deferred per project policy (device checks don't block phases)]

## Tests

### 1. Full signup on the Samsung device
Create an account, fill all five hub steps in a non-default order, then Review and Publish.
expected: Lands on client home with the onboarding carousel. No username or phone field appears anywhere.
result: [pending]

### 2. Verify Email with a real emailed code
expected: Nothing is emailed when the step opens. A code arrives only after tapping Send. Resend shows a 30 s countdown and re-enables at zero.
result: [pending]

### 3. Profile Photo crop feel
Pick a photo, pinch-zoom, pan under the circular guide, then Use and Save.
expected: Gestures feel right, the output is square, and the upload succeeds.
result: [pending]

### 4. Resume an unfinished setup
Kill the app mid-setup and relaunch. Separately, log out and log in during an unfinished setup.
expected: Both resume into the hub with completed steps checked.
result: [pending]

### 5. "Not now" deferral (D-27)
Tap "Not now" on the hub, relaunch, then log out and log in.
expected: After the relaunch the app opens client home with no forced setup. After the re-login it resumes into setup. The app-bar "Not now" action fits at 200% font scale.
result: [pending]

### 6. Mode switch and carousel persistence
Switch client → expert → client from the top-bar avatar, then force-stop and relaunch.
expected: The last chosen mode is restored. The carousel does not reappear after being dismissed once. It reappears after logout and login.
result: [pending]

## Summary

total: 6
passed: 0
issues: 0
pending: 6
skipped: 0
blocked: 0

## Gaps
