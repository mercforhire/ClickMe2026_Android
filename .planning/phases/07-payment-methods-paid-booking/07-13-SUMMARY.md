---
phase: 07-payment-methods-paid-booking
plan: 13
subsystem: payments
tags: [compose, stripe, flowcontroller, payment-launcher, 3ds]
requires:
  - phase: 07-01
    provides: clickMeSheetConfiguration, rememberClickMePaymentSheetAppearance, AppConfig.stripePublishableKey
  - phase: 07-04
    provides: MakeABookingPaymentInfoLink, PaymentInfoSheet
  - phase: 07-09
    provides: toSheetOutcome, toChallengeOutcome, shouldConfirmSelection
  - phase: 07-11
    provides: onChallengeOutcome, challengeAccount
  - phase: 07-12
    provides: MakeABookingScreen onOpenPaymentMethods wiring
provides:
  - PaidFlowStripeHost (FlowController + PaymentLauncher host)
  - MakeABookingScreen paidFlowHost slot (swappable in tests)
  - Paid-only "How does payment work?" link and info sheet on the booking form
  - Payment reference appended to the booking error dialog, selectable
  - AuthErrorDialog selectable option
affects: [07-14]
tech-stack:
  added: []
  patterns:
    - "Consume-before-launch host: onRequestLaunched() runs before configureWithSetupIntent / handleNextActionForPaymentIntent"
    - "Holder object so the option-result callback can confirm() on the controller it belongs to"
    - "Screen takes a composable host slot with a Stripe default; tests inject a recording fake"
key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/PaidFlowStripeHost.kt
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingScreen.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/login/AuthErrorDialog.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingScreenTest.kt
key-decisions:
  - "23.24.0 PaymentOptionResult exposes didCancel and paymentOption (verified with javap), so D-18 is honored and no blocker was recorded"
  - "The RunChallenge launch additionally requires challengeAccount == request.stripeAccount, so the launcher in that composition is the one keyed to the 402's account"
requirements-completed: [PAY-03, PAY-05, PAY-06, PAY-07]
duration: ~45min
completed: 2026-10-22
---

# Phase 7 Plan 13: Stripe host, payment info link and payment reference Summary

**The booking screen now drives Stripe's FlowController sheet and the 3DS PaymentLauncher from ViewModel state exactly once per request, with D-18 dismissal semantics, plus the paid-only "How does payment work?" sheet and a copyable payment reference in paid error dialogs.**

## Accomplishments
- `PaidFlowStripeHost` runs `PaymentSheet.FlowController` in setup mode: `configureWithSetupIntent` with the SetupIntent secret, the ClickMe configuration and the dark appearance, then `presentPaymentOptions()` from the configure callback. After the sheet, `shouldConfirmSelection(didCancel, paymentOption != null)` gates `confirm()`; a dismissal reports `SheetOutcome.Canceled` even with a preselected card (D-18). A configure failure reports `SheetOutcome.Failed`.
- The 3DS step uses `rememberPaymentLauncher(publishableKey, stripeAccountId = challengeAccount, ...)` and `handleNextActionForPaymentIntent`, launched only when `challengeAccount` equals the request's `stripeAccount` (never the platform account).
- Both requests are consumed (`onRequestLaunched()`) before launching; results flow through `rememberUpdatedState`.
- `MakeABookingScreen` gained `paidFlowHost` (default: the real host) called once outside `MakeABookingContent`. `BackHandler(enabled = isSubmitting)` and the app-bar `backEnabled` are untouched and cover the whole paid flow.
- Content: paid-only `MakeABookingPaymentInfoLink` directly above the CTA (`Spacing.md` gap, `Spacing.lg` below the Summary), `PaymentInfoSheet` behind `rememberSaveable` state. The dialog appends "\n\nPayment reference: pi_..." when `dialogPaymentReference` exists and is selectable only then.
- `AuthErrorDialog(selectable = false)` wraps the message in `SelectionContainer` when true; other callers are unchanged.

## Task Commits
1. Task 1 RED: `b40b8ee` (test)
2. Task 1 GREEN: `b17bb19` (feat)
3. Task 2 RED: `6f7e0c9` (test)
4. Task 2 GREEN: `9042744` (feat)

## Verification
- `./gradlew testDevDebugUnitTest ktlintCheck assembleDevDebug` passes; `--tests '*MakeABooking*' --tests '*Login*' ktlintCheck lintDevDebug` passes; `scripts/check-design-rules.sh` reports OK.
- `MakeABookingScreenTest`: 36 tests (4 new host-slot behaviors, 6 new link/sheet/dialog behaviors, all prior cases) pass.
- Acceptance greps on `PaidFlowStripeHost.kt` (comments excluded): `configureWithSetupIntent` x1, `shouldConfirmSelection(` x1, `stripeAccountId = challengeAccount` x1, `confirmPayment|presentWithPaymentIntent|Log.` x0. `PaymentInfoSheet(` x1 and `make_a_booking_payment_reference` x1 in the screen; `SelectionContainer` x2 and `selectable: Boolean = false` x1 in `AuthErrorDialog.kt`. `onRequestLaunched()` precedes both the configure and the challenge call.

## Deviations from Plan

**1. [Rule 1 - Test only] Info sheet test scrolls to "Got it"**
- The sheet body is a lazy list, so "Got it" is not composed until scrolled. The test scrolls the sheet's list (excluding the form's scroll container) before asserting. No production change.

**2. [Process] Plan-critical D-18 check done with javap**
- Read the 23.24.0 `PaymentOptionResult` / `FlowController.Builder` signatures from the Gradle cache before writing the host; `didCancel` exists, so the "stop and record a blocker" branch did not apply.

Otherwise the plan was executed as written. The worktree started at a stale base and was reset to `3b56784` per the startup check.

## Open Items (deferred device checks, D-03)
Needs the 07-05 publishable key and the iOS app signed in as the expert. None of this runs in Robolectric (the host is replaced by a fake there):
- Short-lead paid slot -> FlowController lists saved cards -> pick a card -> Request Sent with the hold-placed card -> iOS expert receives the request.
- Dismiss the sheet with a preselected card -> no charge, form kept (D-18).
- 3DS with 4000 0027 6000 3184 (second try 4000 0025 0000 3155) -> challenge passes -> booking confirms; cancel the challenge -> "Payment wasn't completed..." and Book Now retries.
- Rotate while the sheet and while the challenge are open -> no second sheet or challenge (A4, FlowController state across rotation).
- A3: `PaymentResult.Completed` for a `requires_capture` PaymentIntent.
- PaymentSheet renders dark on a light-mode phone.

## Known Stubs
None.

## Threat Flags
None. T-07-46..50 are mitigated as planned (consume-before-launch tested with a fake host, `shouldConfirmSelection` gate, account match before launching, no `confirmPayment` / `presentWithPaymentIntent` / logging in the host); T-07-51 accepted.

## Self-Check: PASSED
- `PaidFlowStripeHost.kt` exists; commits b40b8ee, b17bb19, 6f7e0c9, 9042744 exist.
