---
status: partial
phase: 07-payment-methods-paid-booking
source: [07-VERIFICATION.md]
started: 2026-10-23T16:40:00Z
updated: 2026-10-23T19:00:00Z
---

## Current Test

[blocked — backend Stripe account is live-mode; all card checks wait for a test-mode backend path (user decision 2026-10-23: keep deferred)]

## Tests

### 1. Test-mode Stripe path for device testing (D-01/D-02, WR-08)
expected: The dev flavor points at a backend whose Stripe secret key is from the same sandbox as its pk_test_ key; no "No such setupintent" error from FlowController, PaymentSheet or PaymentLauncher.
result: blocked
blocked_by: The only backend (clickme-backend-production) runs Stripe in live mode; the dev pk_test_51Tha0r… key is a sandbox. No pk_test_51U8OTJ… key exists. Needs a test-mode backend environment (backend owner) or a real-card smoke test with the live key.
reason: User chose to keep deferred on 2026-10-23.

### 2. SC-4 / CR-01 decision: 3DS-always card 4000 0027 6000 3184 through a short-lead booking against a test-mode backend
expected: The 3DS challenge appears, completes, and /bookings/confirm returns 201. If the app instead shows "Card verification failed", CR-01 is real and the 402 recovery contract must be redesigned with the backend owner (backend + iOS + Android). Do not rely on 4000 0025 0000 3155 — it may never reach the 402 path.
result: blocked
blocked_by: test 1 (no test-mode Stripe path)

### 3. SC-1 Payment Methods on device: add 4242 card via PaymentSheet setup mode, set default, delete
expected: Card appears after add, Default badge moves on set-default, card disappears on delete after the confirm dialog.
result: blocked
blocked_by: test 1 (no test-mode Stripe path)

### 4. SC-2 Short-lead paid booking with the iOS app signed in as the expert; force a failure and retry
expected: The iOS expert receives exactly one request; a retry after a network failure reuses the same booking_attempt_id and creates no duplicate. Also observe WR-01 (retry after a decline).
result: blocked
blocked_by: test 1 (no test-mode Stripe path)

### 5. SC-3 Long-lead (more than 6 days out) with a saved card, and with no saved card
expected: Saved card: booking confirms without a PaymentIntent. No card: CTA reads "Add a Payment Method", opens Payment Methods, and after adding a card the client taps Book Now and finishes.
result: blocked
blocked_by: test 1 (no test-mode Stripe path)

### 6. SC-5 on device: JPY no decimals; rotation while sheet/challenge is open launches neither twice (A4); 200% font scale on Payment Methods, info sheet, paid Request Sent
expected: As stated.
result: blocked
blocked_by: test 1 (no test-mode Stripe path)

## Summary

total: 6
passed: 0
issues: 0
pending: 0
skipped: 0
blocked: 6

## Gaps
