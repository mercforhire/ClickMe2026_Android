---
phase: 07-payment-methods-paid-booking
fixed_at: 2026-10-23T18:30:00Z
review_path: .planning/phases/07-payment-methods-paid-booking/07-REVIEW.md
iteration: 1
findings_in_scope: 9
fixed: 6
skipped: 3
status: partial
---

# Phase 7: Code Review Fix Report

**Fixed at:** 2026-10-23T18:30:00Z
**Source review:** .planning/phases/07-payment-methods-paid-booking/07-REVIEW.md
**Iteration:** 1

**Summary:**
- Findings in scope: 9 (CR-01, WR-01 to WR-08; scope `critical_warning`, so IN-01 to IN-05 were not attempted)
- Fixed: 6 (WR-02 to WR-07)
- Skipped: 3 (CR-01, WR-01, WR-08), as the orchestrator directed

Every fix is a logic or state-handling change. Each one is unit-tested, but they are all marked **fixed: requires human verification** so a developer confirms the behavior before the phase moves on. WR-05 changes the Stripe host, which cannot run under Robolectric, so it also needs a rotation check on a device.

## Gate results

Run after the last fix, on `main` at `a320299`:

| Check | Result |
|-------|--------|
| `./gradlew testDevDebugUnitTest ktlintCheck assembleDevDebug` (with `JAVA_HOME` set to the Android Studio JBR) | BUILD SUCCESSFUL |
| Unit tests (dev debug) | 1556 tests, 0 failures, 0 errors, 0 skipped (118 suites); 21 more than the 1535 recorded in 07-14 |
| `bash scripts/check-design-rules.sh` | `Design rules: OK` |

Each fix also ran its targeted test classes and `ktlintCheck` before it was committed.

## Fixed Issues

### WR-02: An HTTP 5xx or gateway timeout on `/bookings/confirm` is not treated as an unknown outcome

**Status:** fixed: requires human verification
**Files modified:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/PaidFlowError.kt`, `app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/PaidFlowErrorTest.kt`, `app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingViewModelPaidTest.kt`
**Commit:** 07c3230
**Applied fix:**
- `toHttpPaidFailure` now receives `confirming`.
- On the confirm step, an unmapped code with status >= 500 sets `outcomeUnknown = true`. This covers the short-lead path (keeps the payment reference) and the long-lead path (no reference).
- The ViewModel then remembers the attempt id (D-16). A later `SLOT_UNAVAILABLE` on the same attempt shows the check-My-Bookings copy (D-19).
- Mapped codes are unchanged. For example, 503 `DIRECT_CHARGE_DISABLED` keeps its own row.
- Unmapped 4xx on confirm, and 5xx on create-intent, are not flagged as unknown.

**iOS follow-up:** iOS `handlePaidFlowError` probably has the same gap. Raise it with the iOS owner.

### WR-03: A late availability response can clear the selected slot and rotate `bookingAttemptId` in the middle of a paid booking

**Status:** fixed: requires human verification
**Files modified:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingViewModel.kt`, `app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingViewModelPaidTest.kt`
**Commit:** 843bfa4
**Applied fix:**
- While `isSubmitting` is true, `applyAvailability` only refreshes the slot list, the expert time zone and `now`. The selection, its `SavedStateHandle` copy and the attempt id are left alone. This covers both the free and paid flows, and the state after a success.
- Once the form is editable again (`resetPaidFlow`, and the free-path failure handler), `reconcileSlotAfterSubmission()` checks the held slot against the refreshed list. A slot that has gone is dropped now, silently, with the attempt id rotated. Without this, the next tap would have dropped it with the misleading "time has passed" notice.
- The D-16 attempt id is recorded before that reconcile runs, so it always matches the id that was sent.

**iOS follow-up:** iOS has the same availability race (stale responses can overwrite newer ones). This fix is Android-only hardening.

### WR-04: A 3DS challenge that completes after process death is dropped without notice

**Status:** fixed: requires human verification
**Files modified:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingViewModel.kt`, `app/src/main/res/values/strings_make_a_booking.xml`, `app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingViewModelScaTest.kt`
**Commit:** cffafb1
**Applied fix:**
- `startChallenge` writes one boolean marker (`booking_challenge_pending = true`) to `SavedStateHandle`. It contains no secret and no id, so D-17 holds and no client secret, `pi_`, `pm_` or account id is saved.
- Every challenge outcome and every `resetPaidFlow` clears the marker.
- A ViewModel restored with the marker loads the form behind a new "Couldn't book" notice (`make_a_booking_error_challenge_interrupted`): verification was interrupted, the card may show a temporary hold, and the user should check My Bookings before booking again.
- The late PaymentLauncher result is still ignored, because its context is gone.
- I did not take the reviewer's "persist the non-secret ids and resume the confirm" option. It would mean saving `pi_`/`pm_` ids, which the orchestrator ruled out.

**iOS follow-up:** None. This is Android-only (process death), and the copy is new on Android.

### WR-05: A rotation while the FlowController is configuring shows a spurious alert with raw SDK developer text

**Status:** fixed: requires human verification (device rotation check needed)
**Files modified:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/PaidFlowStripeHost.kt`, `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/PaidBookingFlow.kt`, `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingViewModel.kt`, `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingScreen.kt`, `app/src/main/java/com/uptrendinvestments/clickmeapp/stripe/StripeResultMapping.kt`, `app/src/main/java/com/uptrendinvestments/clickmeapp/stripe/StripeOutcomes.kt`, `app/src/test/java/com/uptrendinvestments/clickmeapp/stripe/StripeResultMappingTest.kt`, `app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingViewModelPaidTest.kt`
**Commit:** 486818b
**Applied fix:**
- **Present from the current composition.** The configure callback no longer touches the `flowController` it captured.
  - On success it calls a new `onSheetConfigured`. The ViewModel publishes a new secret-free one-shot, `StripeRequest.PresentOptions`, at most once per sheet request.
  - The host's `LaunchedEffect(request)` consumes that request and calls `presentPaymentOptions()` on the controller of whatever composition is current.
  - I checked the Stripe 23.24.0 sources. The configuration state (`FlowControllerViewModel`, and the `@Singleton` `FlowControllerConfigurationHandler` in its state component) is activity-scoped and found through a `rememberSaveable` key, so the rebuilt controller sees it.
- **No developer text.** A new `Throwable?.userFacingStripeMessage()` returns Stripe's text only for a `StripeException` whose `stripeError.type == "card_error"`. The Stripe SDK applies the same rule to its own error text. Every other failure maps to `null`, so the app shows its existing copy (`make_a_booking_error_sheet_failed`, `make_a_booking_error_sca_failed`, or `login_error_unknown` on Payment Methods). This covers:
  - "host activity is not in a valid state"
  - "FlowController must be successfully initialized"
  - API, network and invalid-request errors
- `toSheetOutcome`, `toChallengeOutcome` and the configure-failure path all use the new helper. The Payment Methods add-card sheet benefits too.

**iOS follow-up (small deviation):** D-15 says "Failure: the Stripe error message". Card-error messages still come through. Only non-card developer errors are replaced with app copy. iOS shows `localizedDescription` for every error, so the iOS owner may want the same filter.

**Device check:** rotate while the sheet is loading (after tapping Book Now on a paid short-lead slot). Expected: the sheet opens once after the rotation, with no "Couldn't book" alert.

### WR-06: `refreshPaymentMethods()` skips while an earlier load is running

**Status:** fixed: requires human verification
**Files modified:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingViewModel.kt`, `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingScreen.kt`, `app/src/test/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingViewModelTest.kt`
**Commit:** 7898ddf
**Applied fix:**
- The ViewModel tracks whether the running card load is still current. `loadPaymentMethods()` marks it current, and the new `onScreenHidden()` marks it stale.
- The screen calls `onScreenHidden()` on `ON_PAUSE` and when it leaves composition, through a `DisposableEffect`. That covers RESEARCH A10, where Nav3 may not deliver lifecycle events to the covered entry.
- `refreshPaymentMethods()` now skips only when the running load started after the screen was last hidden. So the duplicate `ON_RESUME` / `LaunchedEffect(Unit)` calls on first composition still collapse into one request, while a load that predates leaving for Payment Methods is restarted. `loadPaymentMethods()` cancels the old job.
- I chose this over the reviewer's "always restart" option because "always restart" would send up to three card requests every time the form opens.

**iOS follow-up:** None needed.

### WR-07: Payment Methods can show a stale list after a successful change, and an older load can overwrite a newer reload

**Status:** fixed: requires human verification
**Files modified:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/paymentmethods/PaymentMethodsViewModel.kt`, `app/src/main/res/values/strings_payment_methods.xml`, `app/src/test/java/com/uptrendinvestments/clickmeapp/screens/settings/paymentmethods/PaymentMethodsViewModelTest.kt`
**Commit:** a320299
**Applied fix:**
- Every list fetch carries a generation number. Fetches are the first load, Retry, the add-card reload and the new post-mutation `reloadAfter(operation)`, which replaces `reloadSilently()`.
- Only the newest fetch may apply its answer, and `reloadAfter` also cancels `loadJob`. So an older add-card reload cannot bring a removed card back, and a slow post-delete reload cannot overwrite a newer list.
- When the reload after a confirmed DELETE or set-default fails, the confirmed change is applied locally: the removed card disappears, or the badge moves to the new default. No other card is ever promoted (D-07). This matches iOS's local update.
- A failed reload after adding a card now shows "Couldn't load payment methods" with a new message: "Your card was saved, but the list couldn't be refreshed. Open Payment Methods again to see it." It is an `AlertDialog`, not an inline Retry state, which keeps the scope small.

**iOS follow-up:** The local fallback matches iOS. The add-card refresh-failure copy is Android-only.

## Skipped Issues

### CR-01: The 3DS recovery calls `handleNextActionForPaymentIntent` on a PaymentIntent left in `requires_payment_method`

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/PaidFlowStripeHost.kt:102-108`
**Reason:** skipped: cross-platform payment-contract decision, which the orchestrator ruled out of scope.
- The verifier (07-VERIFICATION.md, "CR-01 judgement") found that Android matches iOS (the source of truth) and the backend's `STRIPE-INTEGRATION.md` §2.4 line for line.
- The suggested fix would confirm the booking PaymentIntent on the client and needs a backend change: the cloned connected-account `payment_method` id is not in `detail`. That is not allowed in this pass.
- What is needed instead:
  1. A device run of a short-lead booking with card `4000 0027 6000 3184`, not `4000 0025 0000 3155`, against a test-mode backend account. This is blocked on the `pk_test_` key (WR-08).
  2. If the challenge never opens, a decision with the backend owner that covers backend, iOS and Android.
- I made no contract-neutral copy change. WR-05 already makes a 3DS `Failed` show Stripe's card-error text (for CR-01's scenario, the `last_payment_error` decline message) or the app's "Card verification failed." Any further copy would guess at a state the code cannot detect, namely "no challenge was shown".

**Original issue:** with `off_session: true` / `error_on_requires_action: true`, Stripe leaves the PaymentIntent in `requires_payment_method` with no `next_action`. `handleNextActionForPaymentIntent` has nothing to run, so it returns `Failed`, and PAY-05 can never succeed. The retried confirm would also replay the cached `confirm-off-{pi}` idempotency key.

### WR-01: Keeping the attempt id after a decline or a cancelled or failed 3DS is not idempotent

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/PaidFlowError.kt:115-117`, `MakeABookingViewModel.kt` (`onChallengeOutcome`)
**Reason:** skipped: the client-side fix contradicts locked decision D-15, and the real fix needs a backend change.
- The client-side fix is to rotate `bookingAttemptId` after `PAYMENT_FAILED`, `INVALID_PAYMENT_METHOD` and a 3DS cancel or failure. That directly contradicts D-15 (keep the attempt id after a decline or cancel), as the orchestrator stated it, and the iOS table that D-13 ports.
- I checked the backend:
  - The Redis slot hold is keyed per client (`holdSlotMulti(slotKeys, clientId, …)`), so rotating would not deadlock on the user's own hold. Rotating is technically feasible once the decision is amended.
  - The proper fix is on the backend: include the payment method id in the `confirm-off-{pi}` idempotency key.
- No copy change either:
  - The misleading case ("unexpected payment state") arrives as `INTENT_EXPIRED`. D-13 shows the server message for that code and already rotates the attempt id, so the next tap recovers.
  - Replacing it with local copy would contradict D-13. A "your card wasn't charged" line is not safe, because the backend also returns `INTENT_EXPIRED` for an already-captured PaymentIntent.
- Recommend amending D-15 together with the backend owner, as the review suggests.

**Original issue:** a retry with the kept attempt id reuses the same PaymentIntent. The backend's fixed `confirm-off-{pi}` Stripe idempotency key then gives:
- with a different card, a Stripe `idempotency_error`, which the backend maps to `INTENT_EXPIRED` ("unexpected payment state");
- with the same card, the cached decline replayed for 24 hours.

No duplicate booking results.

### WR-08: The dev flavor still ships the mismatched Stripe publishable key

**File:** `app/build.gradle.kts:41-51`
**Reason:** skipped: needs the user's `pk_test_` key for the backend's Stripe account (`51U8OTJ…`). This is the open item from 07-05 (D-01/D-02). As instructed, I did not edit the Stripe keys. Every real-Stripe device check, including the CR-01 run, is blocked until it is swapped.
**Original issue:** `pk_test_51Tha0r…` belongs to a different Stripe account than the backend's secret key. So FlowController, PaymentSheet, `retrieveSetupIntentSynchronous` and PaymentLauncher all fail with "No such setupintent" in the dev flavor.

## Notes

- **Where I worked.** All edits and commits were made directly on `main` in `/Users/LeonChen/Documents/ClickMe_Android`. The orchestrator said I was the only writer there, so I did not create a worktree or a recovery sentinel. The existing `.idea/` changes and `.planning/config.json` were never staged.
- **Commits.** Six atomic commits (`07c3230`, `843bfa4`, `cffafb1`, `486818b`, `7898ddf`, `a320299`). Each ends with the requested `Co-Authored-By` line, and normal hooks ran (no `--no-verify`).
- **New strings:**
  - `make_a_booking_error_challenge_interrupted` (WR-04)
  - `payment_methods_error_refresh_after_add` (WR-07)
- **New public API:**
  - `MakeABookingViewModel.onSheetConfigured()` and `StripeRequest.PresentOptions` (WR-05)
  - `MakeABookingViewModel.onScreenHidden()` (WR-06)
  - `Throwable?.userFacingStripeMessage()` (WR-05)
  - `PaidFlowStripeHost` gained an `onSheetConfigured` parameter.

---

_Fixed: 2026-10-23T18:30:00Z_
_Fixer: Claude (gsd-code-fixer)_
_Iteration: 1_
