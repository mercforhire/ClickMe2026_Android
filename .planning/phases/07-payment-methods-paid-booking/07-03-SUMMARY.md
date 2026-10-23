---
phase: 07-payment-methods-paid-booking
plan: 03
subsystem: payments
tags: [pure-kotlin, error-mapping, state-model, tdd, stripe]
requires: []
provides:
  - PaidStep, PaymentMethodsAvailability, StripeRequest (redacted toString)
  - Instant.toUtcDateString / toUtcTimeSlotString (Locale.ROOT, UTC)
  - SheetOutcome, ChallengeOutcome, SETUP_INTENT_REUSE_WINDOW_MS (SDK-free)
  - PaidPhase, PaidFailure, ApiException.toPaidFailure (pure handlePaidFlowError port)
  - 15 paid-flow error strings
affects: [07-09, 07-10, 07-11, 07-13]
tech-stack:
  added: []
  patterns: [pure error-mapping function with side-effect flags, one-shot request types]
key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/PaidBookingFlow.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/PaidFlowError.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/stripe/StripeOutcomes.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/PaidBookingFlowTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/PaidFlowErrorTest.kt
  modified:
    - app/src/main/res/values/strings_make_a_booking.xml
key-decisions:
  - "Confirm-phase network/decoding failures are flagged outcomeUnknown even with no pi_ (long-lead); the reference is only attached when a pi_ exists"
  - "A 402 with a client_secret but blank/missing stripe_account is a hard failure (sca_unavailable), never a platform-account fallback"
requirements-completed: [PAY-03, PAY-04, PAY-05, PAY-06]
duration: ~25min
completed: 2026-10-22
---

# Phase 7 Plan 03: Paid-flow core model and error mapping Summary

Android-free paid-flow core: step/request model, SDK-free Stripe outcome types, locale-proof UTC strings, and iOS `handlePaidFlowError` ported as a pure, table-tested `ApiException.toPaidFailure`.

## Tasks

| Task | Commit | Result |
|------|--------|--------|
| 1. State model, Stripe outcomes, UTC strings | 71b12af | PaidBookingFlowTest: 6 tests pass |
| 2. toPaidFailure table + strings | 92c1a58 | PaidFlowErrorTest: 20 tests pass; BookingErrorsTest and ktlintCheck pass |

## Deviations from Plan

**1. [Process - TDD order]** For both tasks the production files were written in the same step as the tests, so no separate failing-test (RED) commit exists; each task is a single `feat` commit with tests that pass. Behavior coverage is as specified.

**2. [Rule 1 - Lint] ktlint multi-line data class parameters** in `StripeOutcomes.kt` fixed before the Task 1 commit.

**3. Extra coverage beyond the plan:** `outcomeUnknown` is also set for a confirm network/decoding failure without a `pi_` (long-lead path), since the booking may still have been created. Tested.

Worktree base was behind the expected base commit and was fast-forwarded with the startup reset before any work.

## Known Stubs

None.

## Threat Flags

None. T-07-10 to T-07-14 mitigations are implemented and tested.

## Self-Check: PASSED

All five new files and the strings edit exist; commits 71b12af and 92c1a58 are in the log.
