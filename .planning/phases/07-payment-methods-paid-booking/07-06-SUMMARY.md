---
phase: 07-payment-methods-paid-booking
plan: 06
subsystem: payments
tags: [viewmodel, payment-methods, busy-rule, setup-intent, hilt, compose-state]
requires:
  - phase: 07-02
    provides: PaymentMethodsRepository, PaymentMethodItem, PaymentMethodSetupData, FakeClickMeApi handlers
  - phase: 07-03
    provides: SheetOutcome, SETUP_INTENT_REUSE_WINDOW_MS
provides:
  - PaymentMethodsKey NavKey
  - PaymentMethodsUiState and supporting types (list state, CardOperation, AddCardRequest, PaymentMethodsDialog)
  - PaymentMethodsViewModel (load/retry, setDefault, remove, addCard, onAddCardLaunched, onAddCardResult, dismissDialog)
  - strings_payment_methods.xml (all Payment Methods copy incl. brand names)
affects: [07-09, 07-12]
tech-stack:
  added: []
  patterns:
    - "Single busy operation guard; silent server-truth reload after mutation"
    - "SDK-free ViewModel; Stripe host consumes one-shot addCardRequest and reports SheetOutcome"
key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/paymentmethods/PaymentMethodsKey.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/paymentmethods/PaymentMethodsUiState.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/paymentmethods/PaymentMethodsViewModel.kt
    - app/src/main/res/values/strings_payment_methods.xml
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/settings/paymentmethods/PaymentMethodsViewModelTest.kt
  modified: []
key-decisions:
  - "No optimistic updates (unlike iOS): list always rendered as the server returned it (D-06, D-07)"
  - "A SetupIntent from a cancelled sheet is reused for 10 minutes using MonotonicClock; cleared on Completed/Failed"
  - "setDefault/remove only accept ids present in the loaded list (T-07-21)"
requirements-completed: [PAY-01, PAY-02]
duration: 25min
completed: 2026-10-22
---

# Phase 7 Plan 06: Payment Methods ViewModel Summary

**SDK-free Payment Methods ViewModel with a single-busy-operation rule, server-truth reloads and a SetupIntent add-card flow that reuses a cancelled intent for 10 minutes.**

## Accomplishments
- Route key, UI state model and all 36 Payment Methods strings (iOS copy verbatim, brand names included).
- `PaymentMethodsViewModel`: load/retry with Loading/Failed/Loaded (empty list is Loaded); `setDefault`/`remove` guarded by `busy`, loaded-list ids and already-default check; silent reload before `busy` clears; failures map to "Couldn't update"/"Couldn't remove"/"Couldn't add card" via `toDialogError()` (ACCOUNT_SUSPENDED silent).
- Add card: single mint, one-shot `addCardRequest`, outcome handling for Completed/Canceled/Failed, 10-minute reuse after cancel, no auto-retry on 429.
- 15 unit tests (covering the 12 plan behaviors plus split cases) pass; ktlint, full unit suite and `assembleDevDebug` pass.

## Task Commits
1. Task 1: route key, state, strings - `22c90c6`
2. Task 2 RED: failing tests - `515b23a`
3. Task 2 GREEN: ViewModel (+ ktlint-clean tests) - `37c749f`

## Deviations from Plan
None - plan executed as written. Worktree base was behind the expected base (`d882eb5`) and was fast-forwarded with `git reset --hard` per the startup check.

## Known Stubs
None.

## Threat Flags
None. T-07-20..T-07-24 mitigations implemented and tested (busy guard, known-id guard, no optimistic default, no auto-retry/reuse, redacted `AddCardRequest.toString()`).

## Self-Check: PASSED
All five files exist; commits 22c90c6, 515b23a, 37c749f exist.
