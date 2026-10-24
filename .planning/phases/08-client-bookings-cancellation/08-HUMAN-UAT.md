---
status: partial
phase: 08-client-bookings-cancellation
source: [08-VERIFICATION.md]
started: 2026-10-24T01:50:00Z
updated: 2026-10-24T01:50:00Z
---

## Current Test

[awaiting human testing]

## Tests

### 1. SC-1 My Bookings on the Samsung SM-A075F (devDebug, client login)
expected: Upcoming and Past tabs; a completed-but-not-ended session stays in Upcoming; Past labels read Completed, Cancelled or Missed; pull-to-refresh works; an iOS action refreshes the list through booking.update; a quiet reload happens on tab return.
result: [pending]

### 2. SC-2 Upcoming Booking Details
expected: pending_approval shows the "Waiting for expert to accept" banner; confirmed shows the inert Join placeholder; a Skype or Zoom link is shown and copied; tapping a Today's Sessions card on Explore opens the details.
result: [pending]

### 3. SC-3 Cancel, free booking and held paid booking, with a comment
expected: The booking moves to Past as Cancelled, the result snackbar shows, and the iOS expert sees the cancellation. The paid case is blocked on the test-mode Stripe backend.
result: [pending]

### 4. SC-4 Reschedule response, with the iOS expert proposing
expected: Android accepts and declines (with and without a reason) and the booking leaves pending_reschedule. PAYMENT_METHOD_REQUIRED recovery with a saved card and with none (blocked on the test-mode Stripe backend). Booking Summary Book Again.
result: [pending]

### 5. Visual and system checks
expected: 200% font scale on My Bookings, Upcoming Details with a reschedule card, Cancel step 2 and Summary. Rotation and process death on Cancel step 2. Back during an in-flight cancel. JPY refund snackbar formatting.
result: [pending]

## Summary

total: 5
passed: 0
issues: 0
pending: 5
skipped: 0
blocked: 0

## Gaps
