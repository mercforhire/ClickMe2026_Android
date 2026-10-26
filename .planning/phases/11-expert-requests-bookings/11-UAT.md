---
status: testing
phase: 11-expert-requests-bookings
source: [11-VERIFICATION.md]
started: 2026-10-26T18:58:34Z
updated: 2026-10-26T23:14:00Z
---

## Current Test

number: 5
name: Re-test after 11-16 (G-11-1 fix) — Android expert and iOS client join the same call
expected: |
  On the Samsung (devDebug, production backend) logged in as the expert, with an iOS client booking inside the join window: tap Join from the Dashboard card, an Upcoming card and the Booking Summary. Each time the call screen opens with no "Couldn't join session" dialog, the iOS client hears you and you hear them, mute and speaker work, and leaving then re-joining works.
awaiting: user response

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
result: [pending]
note: "First attempt (2026-10-26 19:10) ran a stale build installed 17:09, before 11-16 merged at 18:34 — logcat showed the old 'engine create failed NullPointerException' line without a frame. Fixed APK installed 19:13; re-test pending."

## Summary

total: 5
passed: 3
issues: 1
pending: 1
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

