---
phase: 07-payment-methods-paid-booking
plan: 10
subsystem: payments
tags: [viewmodel, paid-booking, stripe-flow, idempotency, error-handling]
requires:
  - phase: 07-01
    provides: StripePaymentMethodReader, FakeStripePaymentMethodReader
  - phase: 07-02
    provides: BookingRepository.paymentIntent/confirm, PaymentMethodsRepository.createSetupIntent, fake API handlers
  - phase: 07-03
    provides: PaidStep, StripeRequest, toPaidFailure, SheetOutcome, SETUP_INTENT_REUSE_WINDOW_MS, UTC formatters
  - phase: 07-07
    provides: paymentMethods availability and savedPaymentMethods in MakeABookingViewModel
provides:
  - Paid book() branch (short lead and long lead) in MakeABookingViewModel
  - onStripeRequestLaunched / onSheetOutcome for the Compose host (07-13)
  - In-memory booking attempt id, setup-intent reuse, unknown-outcome tracking
  - paidStep / stripeRequest / dialogPaymentReference in MakeABookingUiState
affects: [07-11, 07-12, 07-13]
tech-stack:
  added: []
  patterns:
    - "Stripe-free ViewModel: one-shot StripeRequest out, SheetOutcome in"
    - "Memory-only idempotency key and secrets, recovery id (not the attempt id) in SavedStateHandle"
key-files:
  created:
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingViewModelPaidTest.kt
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingViewModel.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingUiState.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/BookingTestFixtures.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingViewModelBookTest.kt
key-decisions:
  - "bookingAttemptId regenerates in selectTopic, selectSlot (new start), clearSlot (previous non-null) and applyAvailability (resolved slot differs); never in SavedStateHandle"
  - "confirmPaid writes a separate recovery UUID to KEY_ATTEMPT_ID before the request and records the success under it, as the free path"
  - "A cancelled sheet keeps the cached SetupIntent (same attempt id, within 10 minutes); Failed and Completed drop it"
patterns-established:
  - "PaidContext captured at tap time so a mid-flow availability reload cannot change what is sent"
requirements-completed: [PAY-03, PAY-04, PAY-06]
duration: 40min
completed: 2026-10-22
---

# Phase 7 Plan 10: Paid booking state machine Summary

**MakeABookingViewModel now runs the full paid booking flow (payment-intent, setup-intent, sheet request, pm_ read, confirm, long-lead fallback) with an in-memory idempotency key and the complete 07-03 failure table applied.**

## Accomplishments
- Short-lead path sends exactly one payment-intent (UTC date and slot, attempt id), one setup-intent, publishes a one-shot `StripeRequest.PresentSheet`, and after `SheetOutcome.Completed` reads the `pm_` and sends one `/bookings/confirm` (no `booking_attempt_id`).
- Long-lead path (422 LONG_LEAD_REQUIRES_SAVED_CARD) skips setup-intent and the sheet, confirming with no `payment_intent_id` and the default card, else the first.
- `isSubmitting` stays true for the whole flow; cancelled sheets reset silently, keep the slot and reuse the SetupIntent for 10 minutes on the same attempt id.
- Failures go through `applyPaidFailure`: dialog copy, new attempt id, slot clear and reload (list stays visible), meeting reset, payment reference on confirm failures, unknown outcome kept after a confirm network failure with no auto-retry, and the check-My-Bookings copy for a follow-up slot conflict on the same attempt id.
- 402 PAYMENT_REQUIRES_ACTION publishes `RunChallenge` and keeps the flow open (07-11 adds the outcome handling and loop guard).
- Paid success is recorded in `CompletedBookingStore` under a recovery id written before the request; removed on any failure.

## Task Commits
1. **Task 1 and Task 2 (implemented together, one commit)** - `332122d` (feat)

## Deviations from Plan

### Process
**1. Tasks committed together, tests written alongside the implementation**
- The two tasks edit the same ViewModel and test file and Task 2's failure handling (`applyPaidFailure`, recovery record, `dismissError`) was part of the same state machine, so the implementation was written in one pass and the 30 tests afterwards instead of a strict RED commit per task. All plan behaviors (13 + 12) are covered by `MakeABookingViewModelPaidTest`.

### Auto-fixed Issues
**2. [Rule 1 - Bug, test only] Wrong setup-intent count in my own test**
- A failed sheet drops the cached SetupIntent, so consecutive failed sheets mint each time; the first draft of the test expected reuse. Corrected the assertion (2 then 3 calls).

Otherwise the plan was executed as written. `pendingPaymentMethodId` is stored but not read yet; it is the hand-off for the 07-11 3DS retry.

## Known Stubs
None. `pendingPaymentMethodId` is intentionally unread until 07-11.

## Issues Encountered
- The worktree base was behind the expected base commit; reset to `1d1a386` per the startup check before any work.

## Verification
- `./gradlew testDevDebugUnitTest ktlintCheck assembleDevDebug` passes (Hilt graph compiles with the two new constructor dependencies).
- Acceptance greps: no saved-state writes of secrets or attempt id, no `if (!topic.isFree) return`, one `toUtcDateString()`, one `SETUP_INTENT_REUSE_WINDOW_MS` use, no Stripe imports, `runCatching` or `Log.`, one `toPaidFailure(`, `completedBookings.put(` twice.

## Self-Check: PASSED
- MakeABookingViewModelPaidTest.kt exists (30 tests, 0 failures); commit 332122d exists.
