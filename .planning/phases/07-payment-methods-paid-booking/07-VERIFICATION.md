---
phase: 07-payment-methods-paid-booking
verified: 2026-10-23T16:30:00Z
status: human_needed
score: 4/5 roadmap success criteria verified in code; SC-4 UNCERTAIN (cross-platform design risk, human decision); SC-1..SC-5 device runs deferred
overrides_applied: 0
re_verification: false
human_verification:
  - test: "SC-4 / CR-01 decision: run a 3DS-always card (4000 0027 6000 3184) through a short-lead booking against a test-mode backend account and see whether the challenge opens and the retried confirm creates the booking"
    expected: "The 3DS challenge UI appears, completes, and /bookings/confirm returns 201. If instead the app shows 'Card verification failed', CR-01 is real and the 402 recovery contract must be redesigned with the backend owner (backend + iOS + Android)."
    why_human: "Depends on Stripe server-side PaymentIntent state after an off_session confirm with error_on_requires_action, which cannot be checked from code. The Phase 2 spike run that would have proved it failed on the key mismatch. The 4000 0025 0000 3155 card named in CONTEXT/ROADMAP may never reach the 402 path (see Gaps Summary)."
  - test: "SC-1 Payment Methods on device: add 4242 card via PaymentSheet setup mode, set default, delete"
    expected: "Card appears after add, Default badge moves on set-default, card disappears on delete after the confirm dialog"
    why_human: "Real Stripe PaymentSheet. Needs the correct pk_test_ key (open item D-01/D-02 from 07-05)."
  - test: "SC-2 Short-lead paid booking on device with the iOS app signed in as the expert; then force a failure and retry"
    expected: "iOS expert receives exactly one request; retry after a network failure reuses the same booking_attempt_id and creates no duplicate"
    why_human: "Real Stripe, real backend and a second device/app. Also check the WR-01 behavior (retry after a decline)."
  - test: "SC-3 Long-lead (more than 6 days out) with a saved card, and with no saved card"
    expected: "Saved card: booking confirms without a PaymentIntent. No card: CTA reads 'Add a Payment Method', opens Payment Methods, and after adding a card the client taps Book Now and finishes."
    why_human: "Real backend 422 branch and Stripe card save."
  - test: "SC-5 on device: JPY topic price shows no decimals; rotation while the sheet or challenge is open launches neither twice (A4); 200% font scale on Payment Methods, info sheet, paid Request Sent"
    expected: "As stated"
    why_human: "Visual and lifecycle behavior; JPY formatting itself is unit-tested."
  - test: "Swap the dev flavor STRIPE_PUBLISHABLE_KEY to the pk_test_51U8OTJ... key of the backend's Stripe account (D-01) and confirm backend mode (D-02)"
    expected: "No 'No such setupintent' error from FlowController, PaymentSheet or PaymentLauncher"
    why_human: "Needs the user to supply the key. Without it no Stripe flow can run in the dev flavor (WR-08). This precedes every other device check."
---

# Phase 7: Payment Methods & Paid Booking Verification Report

**Phase Goal:** A client can save cards and pay for a booking on every Stripe path, including 3DS.
**Verified:** 2026-10-23
**Status:** human_needed
**Re-verification:** No, initial verification

## Verdict

Every Android code path the phase promised exists, is wired, and is unit-tested. Nothing in the repo is a stub for PAY-01, 02, 03, 04, 06 or 07. PAY-05 (3DS) is built exactly the way iOS and the backend contract specify, but there is a credible argument (code review CR-01) that the contract itself does not work against Stripe. That is a backend/iOS/Android design question, not an Android porting defect, and it cannot be settled without a device run. I classify it UNCERTAIN, not FAILED, and ask for a human decision. All real-Stripe checks are deferred per the standing preference (D-03) and the unresolved Stripe key (07-05).

## CR-01 judgement (3DS reachability vs SC-4)

I read the backend and iOS code and compared them with the Android port.

**What the backend does** (`src/controllers/booking.controller.js:744-775`, `src/services/stripe.js:244-257`): for a saved card, `/bookings/confirm` calls `confirmPIOffSession` with `off_session: true` and `error_on_requires_action: true` (idempotency key `confirm-off-{pi}`). On `StripeCardError` with `authentication_required` it returns `402 PAYMENT_REQUIRES_ACTION` with `detail = { client_secret: err.payment_intent.client_secret, stripe_account }`. The `stripe.js` doc comment says outright that the flag is used "rather than leaving the PI in 'requires_action' status".

**What iOS does** (`MakeABookingStripePaymentSheet.swift:172-227`, `MakeABookingViewModel.swift:573-590`, `666-675`): takes `client_secret` and `stripe_account`, scopes `STPAPIClient` to the connected account, calls `STPPaymentHandler.handleNextAction(forPayment:)`, on success calls `retryConfirmAfterSCA(paymentMethodId:)` which re-hits `/bookings/confirm`. The backend doc (`STRIPE-INTEGRATION.md` section 2.4) prescribes exactly this.

**What Android does** (`PaidFlowStripeHost.kt:102-108`, `MakeABookingViewModel.kt:582-644`): `rememberPaymentLauncher(stripeAccountId = challengeAccount)` then `handleNextActionForPaymentIntent(clientSecret)`, then on `Completed` one confirm retry with the same `pm_` and `pi_`. This is a faithful port of iOS, and the 402 parsing, missing-account hard failure (D-15) and single-challenge guard are present.

**Is the reviewer right?** The mechanism is plausible. With `error_on_requires_action: true` Stripe fails the confirm and, to my knowledge of Stripe's off-session behavior, leaves the PaymentIntent in `requires_payment_method` with `last_payment_error`, not in `requires_action` with a `next_action`. `handleNextAction` on such an intent has nothing to run. The backend's own comment points the same way. I could not run Stripe, so I cannot prove it. Two further facts raise the doubt:
- Nothing in the repo shows this path ever working end to end on any platform. The Android spike UAT (02-UAT tests 7-8) failed on the Stripe key mismatch before reaching 3DS, and 02-09 was never run.
- The backend's long-lead recovery endpoint (`recoverPayment`, line ~1199) assumes the PI "returned requires_action" after the same `confirmPIOffSession`, so the backend authors hold the opposite belief. One of them is wrong, and only a live Stripe run decides which.

**Judgement:**
- Not an Android code gap. Android matches iOS and the written backend contract line for line, and CLAUDE.md makes iOS the source of truth. Reworking the Android 402 branch alone (the review's `launcher.confirm(...)` suggestion) would diverge from iOS and would need a backend contract change (the cloned connected-account `payment_method` id is not in `detail`), so it is not something to patch in this phase unilaterally.
- If the device run shows the challenge never opens, SC-4/PAY-05 is unachievable on Android and iOS alike, and the fix is a cross-platform decision for the backend owner (for example return `payment_method` in `detail` and confirm on-session, or confirm without `error_on_requires_action`). That decision belongs to the user.
- Test-plan correction: ROADMAP, CONTEXT and the deferred checklist name card `4000 0025 0000 3155`. That card does not require authentication for off-session payments once set up, so it may skip the 402 branch and pass without exercising this code. Use `4000 0027 6000 3184` (07-14 already names it in the manual row; ROADMAP/CONTEXT should be aligned).

## Goal Achievement: Roadmap Success Criteria

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | In Payment Methods the client can add a card via PaymentSheet in setup mode, set default, delete | VERIFIED in code; device run deferred | `PaymentMethodsViewModel` (load, `setDefault`, `remove`, `addCard`, `onAddCardResult`); `PaymentMethodsScreen` with `AddCardSheetHost` calling `PaymentSheet.presentWithSetupIntent` with customer id + ephemeral key; endpoints GET/POST setup-intent/DELETE/PATCH default in `ClickMeApi.kt:193-217`; registered once in `HomeClientScreen`/`ClientBookingFlowEntries` and reachable from Profile row and booking gate. Unit tests for VM, screen, row, repo exist and pass. |
| 2 | Short-lead paid slot via FlowController; retry reuses `booking_attempt_id`, no duplicate | VERIFIED in code; iOS-expert receipt and real Stripe deferred; WR-01 caveat | `beginPaidBooking` sends payment-intent with `bookingAttemptId` (`PaymentIntentRequest.kt`), then setup-intent, then `StripeRequest.PresentSheet`; `PaidFlowStripeHost` runs `FlowController.configureWithSetupIntent` then `presentPaymentOptions` then `confirm`; pm read via `StripePaymentMethodReader`; `confirmPaid` posts `/bookings/confirm` with `pm_` and `pi_`, no attempt id. Attempt id regenerated only on topic/slot change or spent-id errors (`newBookingAttempt`), kept for network failure/cancel, memory only. Caveat: WR-01, the backend's fixed `confirm-off-{pi}` key makes a retry after a decline or cancelled 3DS fail with a misleading error. It does not create a duplicate booking. |
| 3 | Long-lead confirms with saved card; no card goes to Payment Methods and finishes after adding one | VERIFIED in code; device deferred | `beginPaidBooking` catches `LongLeadRequiresSavedCard` then `performLongLeadBooking` with default-else-first card and `payment_intent_id` omitted. `book()` returns unless `PaymentMethodsAvailability.Available`; CTA "Add a Payment Method" calls `onOpenPaymentMethods`; `refreshPaymentMethods` on resume keeps selections (D-09). Minor: WR-06 (a refresh can be skipped while an earlier load is still running). |
| 4 | 3DS test card triggers challenge via PaymentLauncher; booking confirms after it passes | UNCERTAIN (WARNING, human decision) | Code complete and iOS-faithful: 402 parsed in `PaidFailure.challengeOrFailure`, `RunChallenge` with `stripeAccountId` keyed launcher, `onChallengeOutcome` retries confirm once. Whether Stripe leaves the PI in a state `handleNextActionForPaymentIntent` can act on is unproven (CR-01, see judgement above). Also WR-04 (challenge result lost after process death). |
| 5 | ~15 paid-flow error codes show right message/action; prices correct in minor units incl. JPY | VERIFIED | `PaidFlowError.kt` maps AccountSuspended (silent), PaymentRequiresAction, BookingAttemptConflict, IntentExpired/PiContextMismatch/PiAmountMismatch, SlotUnavailable, MeetingLinkUnavailable, ExpertNotBookable, InvalidPaymentMethod, DirectChargeDisabled, PaymentFailed, DuplicateBooking, AmountBelowMinimum, InvalidPromo, CannotBookSelf (15) plus default-to-server-message with "Payment reference". Slot-taken and meeting-link copy deliberately follows the free path (D-14). `Money.format` uses `Currency.defaultFractionDigits` (JPY 0); `PaymentIntentResponse` does not declare `total_amount`/`summary`, so the major-unit exception never reaches the UI; JPY covered in `MoneyTest`. Minor: WR-02 (HTTP 5xx on confirm not flagged as unknown outcome). |

**Score:** 4/5 verified in code, 1 UNCERTAIN. No FAILED truths. Real-Stripe execution of all five is deferred.

## Requirements Coverage

All seven IDs are claimed by plan frontmatter (07-14 claims all seven) and match REQUIREMENTS.md lines 94-100; no orphaned requirement.

| Requirement | Source plans | Status | Evidence |
|-------------|--------------|--------|----------|
| PAY-01 saved cards, default, delete | 07-02, 04, 06, 09, 12, 14 | SATISFIED in code (device deferred) | SC-1 |
| PAY-02 add card via SetupIntent + PaymentSheet setup mode | 07-01, 02, 05, 06, 09, 14 | SATISFIED in code (device blocked on Stripe key) | SC-1 |
| PAY-03 short-lead sequence with reused `booking_attempt_id` | 07-01, 02, 03, 05, 07, 10, 13, 14 | SATISFIED in code (device deferred) | SC-2 |
| PAY-04 long-lead + no-card route | 07-02, 03, 07, 10, 12, 14 | SATISFIED in code (device deferred) | SC-3 |
| PAY-05 3DS via PaymentLauncher with `stripeAccountId`, retry confirm | 07-01, 03, 05, 11, 13, 14 | NEEDS HUMAN (CR-01) | SC-4 |
| PAY-06 ~15 `handlePaidFlowError` codes | 07-03, 10, 11, 13, 14 | SATISFIED | SC-5 |
| PAY-07 minor units, JPY no decimals | 07-02, 04, 07, 08, 13, 14 | SATISFIED | SC-5 |

REQUIREMENTS.md still shows PAY-01..07 "Pending". That is intentional per 07-14 (deferred is not done); update it after the device checks.

## Required Artifacts and Wiring

| Artifact | Status | Details |
|----------|--------|---------|
| `screens/settings/paymentmethods/*` (VM, Screen, Repository, Row, Remove dialog, Key) | VERIFIED | Substantive, Hilt-injected, registered in client shell, data from `/me/payment-methods` |
| `screens/clientmode/makeabooking/PaidFlowStripeHost.kt`, `PaidFlowError.kt`, `PaidBookingFlow.kt` | VERIFIED | Wired from `MakeABookingScreen` via `paidFlowHost`; requests consumed once |
| `MakeABookingViewModel` paid state machine | VERIFIED | `book()`, `beginPaidBooking`, `performLongLeadBooking`, `confirmPaid`, `applyPaidFailure`, `startChallenge`, `onChallengeOutcome` |
| `stripe/*` (appearance, outcomes, mapping, PM reader), `di/StripeModule.kt` | VERIFIED | Dark palette via same Colors for light/dark; reader catches only Stripe/IO exceptions (IN-04) |
| `PaymentLifecycleCard`, `PaymentInfoSheet`, `BookingRequestSentScreen` paid card | VERIFIED | Lifecycle card shared by info sheet and Request Sent |
| `ApiRequestUrls` / `ClickMeApi` payment group, request/response DTOs | VERIFIED | `ConfirmBookingRequest` has no attempt id; null `payment_intent_id` omitted for long-lead |

Level 4 data flow: card list from `GET /me/payment-methods` flows to `PaymentMethodsListState.Loaded`; Make a Booking `savedPaymentMethods` from the same repository; Request Sent paid card driven by `BookingRequestSentDetails.isFree`. No hollow props found.

## Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| Paid VM, SCA VM, paid error map, Payment Methods VM, Money (JPY) | `./gradlew --offline testDevDebugUnitTest --tests '*MakeABookingViewModelScaTest' '*PaidFlowErrorTest' '*PaymentMethodsViewModelTest' '*MakeABookingViewModelPaidTest' '*MoneyTest'` | exit 0 | PASS |
| Full-suite claim (1535 tests per flavor, R8 build) | not re-run; taken from 07-14-SUMMARY | n/a | SKIPPED |

Probe execution: none declared by the phase (SKIPPED). Debt markers (`TBD`/`FIXME`/`XXX`) in phase code: none found.

## Anti-Patterns and Open Warnings (none block the code goal)

| ID | Severity | Impact |
|----|----------|--------|
| CR-01 | WARNING (decision) | 3DS recovery may not work against Stripe's PI state; shared with iOS and backend |
| WR-01 | WARNING | Retry with the kept attempt id after decline/cancelled 3DS hits the spent `confirm-off-{pi}` key; misleading error, no duplicate booking. D-15's "idempotent retry" premise is wrong |
| WR-02 | WARNING | HTTP 5xx on confirm not treated as unknown outcome (D-16/D-19 copy bypassed) |
| WR-03 | WARNING | Late availability response can clear slot/rotate attempt id during a paid flow |
| WR-04 | WARNING | 3DS result after process death is dropped silently; possible orphaned authorization |
| WR-05 | WARNING | Rotation during FlowController configure can show raw SDK text |
| WR-06 | WARNING | `refreshPaymentMethods` can skip the D-09 reload while an earlier load runs |
| WR-07 | WARNING | Payment Methods stale list after failed silent reload; load race |
| WR-08 | WARNING (blocker for device testing only) | Dev flavor ships the mismatched `pk_test_51Tha0r...` key, so no Stripe flow can run until D-01 is resolved |
| IN-01..05 | INFO | See 07-REVIEW.md |

## Gaps Summary

No code gaps against the roadmap success criteria; `gaps:` is intentionally empty. The phase is `human_needed` for two reasons:

1. **SC-4 / CR-01 needs a user decision.** Run the 3DS-always card once the key is fixed. If the challenge opens and confirm succeeds, CR-01 is a false alarm and SC-4 closes. If not, open a cross-platform change with the backend owner; do not patch Android alone. Also fix the test-card guidance (3155 vs 3184).
2. **All real-Stripe device checks (SC-1..SC-5) are deferred** by standing preference and are blocked on the wrong dev publishable key (07-05 open item). They are logged in STATE.md and listed above.

Recommended follow-ups (separate from this verification): consider addressing WR-01 (client sets a new attempt id after `PaymentFailed`/`InvalidPaymentMethod`/3DS cancel-or-fail, or backend keys include the PM id), WR-02 and WR-04 before release; none is required for the roadmap criteria as written.

---

_Verified: 2026-10-23_
_Verifier: Claude (gsd-verifier)_
