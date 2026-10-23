---
status: partial
phase: 07-payment-methods-paid-booking
source: [07-VERIFICATION.md]
started: 2026-10-23T16:40:00Z
updated: 2026-10-23T16:40:00Z
---

## Current Test

[awaiting human testing — test 1 (Stripe key) must come first]

## Tests

### 1. Swap the dev flavor STRIPE_PUBLISHABLE_KEY to the backend account's pk_test_51U8OTJ… key and confirm backend mode (D-01/D-02, WR-08)
expected: No "No such setupintent" error from FlowController, PaymentSheet or PaymentLauncher. Apply per 07-05 Task 3 (pattern-check the key; prod pk_live_ untouched; live mode = device-test stop).
result: [pending]

### 2. SC-4 / CR-01 decision: 3DS-always card 4000 0027 6000 3184 through a short-lead booking against a test-mode backend
expected: The 3DS challenge appears, completes, and /bookings/confirm returns 201. If the app instead shows "Card verification failed", CR-01 is real and the 402 recovery contract must be redesigned with the backend owner (backend + iOS + Android). Do not rely on 4000 0025 0000 3155 — it may never reach the 402 path.
result: [pending]

### 3. SC-1 Payment Methods on device: add 4242 card via PaymentSheet setup mode, set default, delete
expected: Card appears after add, Default badge moves on set-default, card disappears on delete after the confirm dialog.
result: [pending]

### 4. SC-2 Short-lead paid booking with the iOS app signed in as the expert; force a failure and retry
expected: The iOS expert receives exactly one request; a retry after a network failure reuses the same booking_attempt_id and creates no duplicate. Also observe WR-01 (retry after a decline).
result: [pending]

### 5. SC-3 Long-lead (more than 6 days out) with a saved card, and with no saved card
expected: Saved card: booking confirms without a PaymentIntent. No card: CTA reads "Add a Payment Method", opens Payment Methods, and after adding a card the client taps Book Now and finishes.
result: [pending]

### 6. SC-5 on device: JPY no decimals; rotation while sheet/challenge is open launches neither twice (A4); 200% font scale on Payment Methods, info sheet, paid Request Sent
expected: As stated.
result: [pending]

## Summary

total: 6
passed: 0
issues: 0
pending: 6
skipped: 0
blocked: 0

## Gaps
