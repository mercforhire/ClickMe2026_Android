---
status: partial
phase: 09-voice-call
source: [09-VERIFICATION.md]
started: 2026-10-24T17:33:22Z
updated: 2026-10-24T17:33:22Z
---

## Current Test

[awaiting human testing]

## Tests

### 1. SC-1: Join against a live iOS expert, mic permission dialogs and the external link
Join from Upcoming Details (opened from Explore and from My Bookings), the My Bookings card and Today's Sessions. First mic prompt: deny once (rationale), then deny permanently (Open settings). A Skype/Zoom booking opens the native app or a Custom Tab and minimizes.
expected: Two-way audio with the iOS expert; both mic dialogs appear; the external link opens and the call minimizes to the pill.
result: [pending]

### 2. SC-2: 5+ minutes backgrounded with the screen off
Check the notification (title, Starts at / chronometer), the notification tap after a task swipe, and the notification End call action. Capture `adb logcat -s ClickMeCall` and `adb shell dumpsys audio` (Samsung SM-A075F, `-s R8YYC1QFA6H`).
expected: Audio continues both ways; the notification persists; tap expands the call; End call leaves, sends /end and tears down.
result: [pending]

### 3. Android 17 audio hardening (emulator Medium_Phone_API_37.0)
Run `adb shell cmd audio set-enable-hardening enable`; confirm the targetSdk 37 local-network permission does not block Agora.
expected: Background audio and focus still work; Agora joins.
result: [pending]

### 4. SC-3: Pill and guard
Pill across every tab; Client/Expert switch attempt, logout attempt, add-payment-method attempt and Cancel of the live booking each show the guard dialog; predictive back minimizes the call.
expected: Pill stays visible; guard text "End the call to ..."; Back minimizes and never ends the call.
result: [pending]

### 5. SC-4: Peer hang-up, rejoin, amber state, cutoff
iOS expert hangs up; then rejoin; then the final-5-minute amber state and banner; the 3600 s past scheduled-end cutoff can rely on the unit test.
expected: "Waiting for other person" without ending; rejoin restores Connected; timer and chip turn amber at 5:00 left.
result: [pending]

### 6. SC-5: Full paid session end to end
confirm-connection sent exactly once (backend logs). The paid part is blocked on the test-mode Stripe backend; a free booking covers the rest.
expected: One confirm-connection PATCH per call in the backend logs; session completes.
result: [blocked: test-mode Stripe backend]

### 7. Device, accessibility and OEM sweep
200% font scale on the Connected pair, banner and external variant; TalkBack order; POST_NOTIFICATIONS denied on API 33+; Bluetooth headset routing; incoming GSM call; Samsung battery/sleeping-apps; cold-start notification tap after a task swipe; pill position over gesture vs 3-button nav.
expected: Call still runs and the UI stays usable in each case.
result: [pending]

### 8. Product decision: CallStyle notification
The ongoing-call notification is plain and swipe-dismissible on Android 14+. Decide whether to switch to NotificationCompat.CallStyle.forOngoingCall (one-file change in CallNotificationFactory).
expected: Decision recorded before release.
result: [pending]

### 9. Review-fix device checks (09-REVIEW-FIX.md)
WR-05: force a refused foreground start on API 34+ (Samsung) and confirm the service stops without a crash. WR-06: tap the call notification while a Custom Tab is open and confirm a single MainActivity and the call expands. WR-07: a booking with an unopenable meeting link shows "Couldn't open meeting" and does not confirm. WR-09: a 90-minute booking can still be rejoined after start + 60 min.
expected: No crash; one activity; no confirm for an unopenable link; Join enabled until the scheduled end.
result: [pending]

## Summary

total: 9
passed: 0
issues: 0
pending: 8
skipped: 0
blocked: 1

## Gaps
