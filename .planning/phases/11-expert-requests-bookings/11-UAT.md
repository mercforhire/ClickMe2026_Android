---
status: complete
phase: 11-expert-requests-bookings
source: [11-VERIFICATION.md]
started: 2026-10-26T18:58:34Z
updated: 2026-10-26T21:26:23.190Z
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

## Summary

total: 4
passed: 3
issues: 1
pending: 0
skipped: 0
blocked: 0

## Gaps

- gap_id: G-11-1
  truth: "Both sides join the same Agora call inside the join window (Dashboard card, Upcoming card, Summary) — Android expert joins an iOS client's booking"
  status: failed
  reason: "User reported: Android Version shows error: cannot join session."
  severity: blocker
  test: 1
  artifacts: []
  missing: []
