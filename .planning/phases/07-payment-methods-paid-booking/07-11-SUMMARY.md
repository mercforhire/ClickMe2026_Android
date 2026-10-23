---
phase: 07-payment-methods-paid-booking
plan: 11
subsystem: payments
tags: [viewmodel, 3ds, sca, payment-launcher]
requires:
  - phase: 07-03
    provides: ChallengeOutcome, toPaidFailure (402 detail parsing), sca/pm_unresolved strings
  - phase: 07-10
    provides: paid book() state machine, applyPaidFailure, pendingPaymentMethodId hand-off
provides:
  - MakeABookingViewModel.onChallengeOutcome(ChallengeOutcome) for the Compose host (07-13)
  - MakeABookingUiState.challengeAccount (launcher stripeAccountId key while a challenge runs)
  - One-round 3DS guard (scaAttempted) and single confirm retry with the same pm_ and pi_
affects: [07-13]
tech-stack:
  added: []
  patterns:
    - "Stripe-free ViewModel: RunChallenge out (with challengeAccount), ChallengeOutcome in"
    - "Per-confirm-attempt loop guard cleared by resetPaidFlow and book()"
key-files:
  created:
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingViewModelScaTest.kt
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingViewModel.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingUiState.kt
key-decisions:
  - "challengeAccount is cleared as soon as an outcome is accepted, so a duplicate outcome for one challenge is ignored"
  - "A second 402 uses e.userMessage (else login_error_unknown) with the pi_ as dialogPaymentReference, read before resetPaidFlow clears it"
patterns-established:
  - "startChallenge publishes RunChallenge and challengeAccount in one state update"
requirements-completed: [PAY-05, PAY-06]
duration: 25min
completed: 2026-10-22
---

# Phase 7 Plan 11: 3DS challenge hand-off and retry Summary

**MakeABookingViewModel now hands a single 3DS challenge to the host on the connected account (kept in memory as `challengeAccount`), retries `/bookings/confirm` once with the same `pm_` and `pi_` after a completed challenge, and maps cancel, failure and a repeated 402 to the D-15 alerts without ever looping.**

## Accomplishments
- 402 PAYMENT_REQUIRES_ACTION publishes one `RunChallenge(client_secret, stripe_account)` and `challengeAccount` in one state update; the CTA spinner and Back lock stay (`paidStep = Confirming`, `isSubmitting = true`).
- `onChallengeOutcome(Completed)` retries confirm with the identical request body; success sets `completedBookingId` and clears `challengeAccount`.
- `scaAttempted` allows one challenge round per confirm attempt; a second 402 shows the server message with the payment reference and resets (RESEARCH Pitfall 10).
- `Canceled` shows "Payment wasn't completed. Your card wasn't charged." and `Failed` shows Stripe's message or "Card verification failed."; both reset and keep the slot and the same booking attempt id (next Book Now sends the same `booking_attempt_id`).
- A blank or missing `stripe_account` never starts a challenge (already a `Show(sca_unavailable)` from `toPaidFailure`), asserted end to end here.
- `Completed` with no `pm_` shows the "couldn't be resolved" notice and sends nothing (defensive, untested, documented in KDoc).
- No secret (`acct_`, PI client secret, `pi_`, `pm_`) is written to `SavedStateHandle`; checked by a value scan across all three outcomes.

## Task Commits
1. Task 1 RED: `198215f` (test)
2. Task 1 GREEN: `f54072b` (feat)
3. Task 2 RED: `25c6008` (test)
4. Task 2 GREEN: `7b6129c` (feat)

## Deviations from Plan

None - plan executed as written. The blank-account and no-secret tests passed at the Task 2 RED step because that behavior already existed (07-03 `toPaidFailure`, Task 1 state handling); only the cancel and failure tests were red.

## Known Stubs
None.

## Threat Flags
None. T-07-40..43 are mitigated as planned: account only from the 402 detail with blank as a hard failure, one round per attempt, memory-only secrets, retry reuses `pendingPaymentMethodId` and the same `pi_` (equal request bodies asserted).

## Verification
- `./gradlew testDevDebugUnitTest ktlintCheck assembleDevDebug` passes.
- `MakeABookingViewModelScaTest` (11 tests) and all `MakeABookingViewModel*` tests pass.
- Greps: `fun onChallengeOutcome` x1, `scaAttempted` x5, no saved-state writes of the account or secrets, `sca_canceled` x1, `pm_unresolved` x1.

## Issues Encountered
- Worktree base was behind the expected base commit; reset to `1e2b770` per the startup check before any work.

## Self-Check: PASSED
- MakeABookingViewModelScaTest.kt exists; commits 198215f, f54072b, 25c6008, 7b6129c exist.
