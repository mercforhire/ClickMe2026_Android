---
phase: 07-payment-methods-paid-booking
reviewed: 2026-10-23T15:19:36Z
depth: standard
files_reviewed: 44
files_reviewed_list:
  - app/build.gradle.kts
  - app/src/main/java/com/uptrendinvestments/clickmeapp/ClickMeApplication.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/components/PaymentLifecycleCard.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/components/ProfileMenuRow.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/di/StripeModule.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/model/requests/PaymentRequests.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/ConfirmBookingData.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/PaymentIntentResponse.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/PaymentMethodsData.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/network/ApiRequestUrls.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/network/ClickMeApi.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/home/ClientBookingFlowEntries.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/home/HomeClientScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/BookingFormatting.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/BookingRepository.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingUiState.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/PaidBookingFlow.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/PaidFlowError.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/PaidFlowStripeHost.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/subviews/MakeABookingBookButton.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/subviews/MakeABookingPaymentInfoLink.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/subviews/PaymentInfoSheet.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/requestsent/BookingRequestSentScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/requestsent/BookingRequestSentUiState.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/requestsent/subviews/BookingRequestSentDetailsCard.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/requestsent/subviews/BookingRequestSentHeadline.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/login/AuthErrorDialog.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/paymentmethods/PaymentMethodsKey.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/paymentmethods/PaymentMethodsRepository.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/paymentmethods/PaymentMethodsScreen.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/paymentmethods/PaymentMethodsUiState.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/paymentmethods/PaymentMethodsViewModel.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/paymentmethods/subviews/PaymentMethodBrandDisplay.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/paymentmethods/subviews/PaymentMethodRemoveDialog.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/paymentmethods/subviews/PaymentMethodRow.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/stripe/ClickMePaymentSheetStyle.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/stripe/StripeOutcomes.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/stripe/StripePaymentMethodReader.kt
  - app/src/main/java/com/uptrendinvestments/clickmeapp/stripe/StripeResultMapping.kt
  - app/src/main/res/values/strings_make_a_booking.xml
  - app/src/main/res/values/strings_payment_lifecycle.xml
  - app/src/main/res/values/strings_payment_methods.xml
findings:
  critical: 1
  warning: 8
  info: 5
  total: 14
status: issues_found
---

# Phase 7: Code Review Report

**Reviewed:** 2026-10-23T15:19:36Z
**Depth:** standard
**Files Reviewed:** 44
**Status:** issues_found

## Narrative Findings (AI reviewer)

## Summary

I reviewed the 44 production and resource files of the Payment Methods screen and the paid booking flow. Where behavior depended on code outside those files, I traced it into:
- the Stripe Android SDK 23.24.0 sources: `rememberPaymentLauncher`, `DefaultFlowController`, `FlowControllerConfigurationHandler`, `PaymentLauncherViewModel` and `StripeIntentResult`
- the backend `booking.controller.js` (`createPaymentIntent`, `confirm`)
- the backend `services/stripe.js` (`confirmPIOffSession`) and `services/redis.js` (`holdSlotMulti`)

Several things are sound:
- **Single-flight booking.** `isSubmitting` is set synchronously in `book()`, and every Stripe outcome is gated on `paidStep` / `challengeAccount`.
- **One-shot `StripeRequest` consumption.** The request is cleared before launch.
- **Launcher keying.** The PaymentLauncher is keyed to the 402 account. `rememberPaymentLauncher` does `remember(publishableKey, stripeAccountId)`, so the launcher is rebuilt for the connected account.
- **Second-402 guard.** `scaAttempted` stops a second 402 from starting another challenge loop.
- **Secret redaction.** `toString()` is overridden on every type that holds a secret, HTTP logging is BASIC only, and nothing writes a secret to `SavedStateHandle`.

The main defect is in the 3DS recovery contract. The backend confirms the saved card with `off_session: true` / `error_on_requires_action: true`. Stripe then leaves the PaymentIntent in `requires_payment_method`, not `requires_action`. On a PaymentIntent in that state, the client's `handleNextActionForPaymentIntent` has no next action to run, and the SDK maps the status straight to `PaymentResult.Failed`. So PAY-05 cannot succeed as built.

Other findings:
- Keeping the attempt id after a decline or a cancelled 3DS (D-15) assumes the retry is idempotent. That assumption fails against the backend's `confirm-off-{pi}` Stripe idempotency key.
- HTTP 5xx answers on `/bookings/confirm` are not treated as an unknown outcome.
- A late availability response can clear the slot and rotate the attempt id while a paid booking is in flight.
- A 3DS result that arrives after process death is dropped without notice, which can leave an orphaned authorization hold.
- A rotation while the FlowController is configuring shows a raw SDK developer error to the user.

## Critical Issues

### CR-01: The 3DS recovery calls `handleNextActionForPaymentIntent` on a PaymentIntent the backend has left in `requires_payment_method`, so the challenge never runs and PAY-05 always fails

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/PaidFlowStripeHost.kt:102-108` (with `MakeABookingViewModel.kt:582-644`, `PaidFlowError.kt:127-135`)

**Issue:** The short-lead card comes from a SetupIntent that attaches it to the customer. So `/bookings/confirm` takes backend Path 1, which calls `confirmPIOffSession`:

```js
// backend-reference/src/services/stripe.js:249-256
paymentIntents.confirm(pi, { payment_method, off_session: true, error_on_requires_action: true },
                       { idempotencyKey: `confirm-off-${pi}`, stripeAccount })
```

When an off-session confirm fails with `authentication_required` (with or without `error_on_requires_action`), Stripe moves the PaymentIntent to `requires_payment_method`. It does not move it to `requires_action`, and it sets no `next_action`. Stripe's documented recovery is an on-session confirm with `last_payment_error.payment_method`. The app instead calls `launcher.handleNextActionForPaymentIntent(clientSecret)`. In SDK 23.24.0 that call retrieves the intent and finds no next action, so `NoOpIntentNextActionHandler` runs. `StripeIntentResult.getOutcome` then maps `RequiresPaymentMethod -> Outcome.FAILED`.

Result:
- No challenge UI ever appears.
- `onChallengeOutcome(Failed)` shows "Card verification failed" (or Stripe's text).
- The booking can never complete for a card that needs SCA on the connected account.

Even if the client somehow got past this step, the retried confirm sees `requires_payment_method` again. It then reuses the same `confirm-off-{pi}` idempotency key and replays the cached 402.

The planned device check with `4000 0025 0000 3155` may never reach the 402 path. That card skips authentication for off-session payments once it has been set up, and the SetupIntent step sets it up. So the open human-verification item could pass without ever testing this code. Use a card that always requires authentication (`4000 0027 6000 3184`).

The same design is on iOS, but that does not make it correct. The Phase 2 "spike-validated" note rests on a UAT that failed on the key mismatch (02-UAT tests 7–8).

**Fix:** The client must confirm the PaymentIntent on-session on the connected account, which means revisiting the "Path A: never confirm the booking PI" rule for the 402 branch only. For example, have the backend add the cloned `payment_method` id to `detail` (or read the PaymentIntent with the secret and `stripeAccountId`), then:

```kotlin
// PaidFlowStripeHost, RunChallenge branch
launcher.confirm(
    ConfirmPaymentIntentParams.createWithPaymentMethodId(
        paymentMethodId = r.connectedPaymentMethodId, // last_payment_error.payment_method.id (connected-account clone)
        clientSecret = r.clientSecret,
    ),
)
```

After this, the PaymentIntent reaches `requires_capture`. The existing retry of `/bookings/confirm` then hits the backend's `requires_capture` skip branch and inserts the booking. Agree on this contract with the backend owner, and update the 3DS device check to use `4000 0027 6000 3184` against a test-mode account.

## Warnings

### WR-01: Keeping the attempt id after a decline or a cancelled or failed 3DS is not idempotent: the retry reuses a PaymentIntent whose off-session confirm idempotency key is already spent

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/PaidFlowError.kt:115-117`, `MakeABookingViewModel.kt:632-643`

**Issue:** D-15 and the iOS table keep `bookingAttemptId` after `PAYMENT_FAILED`, `INVALID_PAYMENT_METHOD`, a 3DS cancel and a 3DS failure. Here is what the retry does against the backend:
1. `createPaymentIntent` uses the Stripe idempotency key `booking:{attempt}:create_pi:{acct}`, so it returns the same PaymentIntent, which is now `requires_payment_method`.
2. The user picks a card. `confirm` runs `confirmPIOffSession` with the fixed key `confirm-off-{pi}`.

What the user then sees depends on the card:
- **Different card:** Stripe rejects the reused key with different parameters (`idempotency_error`, HTTP 400). The backend maps any `statusCode === 400` to `INTENT_EXPIRED`, so the user sees "unexpected payment state". Only the third attempt, with a new id, works.
- **Same card** (for example after topping up funds): Stripe replays the cached decline for 24 h, so the retry fails with `PAYMENT_FAILED` no matter what.

**Fix:** Either change the backend (include the cloned payment method id in the `confirm-off` key), or have the client set `newAttemptId = true` for `PaymentFailed`, `InvalidPaymentMethod`, and the 3DS Canceled/Failed outcomes. Record the change as an amendment to D-15, because the decision's premise ("idempotent retry") does not hold.

### WR-02: An HTTP 5xx or gateway timeout on `/bookings/confirm` is not treated as an unknown outcome, so D-16 and D-19 are bypassed

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/PaidFlowError.kt:69-83, 122`

**Issue:** `outcomeUnknown` is only set for `ApiException.Network` and `Decoding`. A Railway proxy 502/504, or a 500 raised after the backend already confirmed the PaymentIntent, arrives as `ApiException.Http` with no known code. It falls into the `else` branch, which shows the server text and the payment reference but never sets `unknownOutcomeAttemptId`. The booking (or authorization) may already exist. On retry, the server reports `SLOT_UNAVAILABLE` (the user's own pending booking) with the plain "slot was just taken" copy instead of the D-19 "check My Bookings" copy.

**Fix:**
```kotlin
else -> PaidFailure.Show(
    serverText,
    paymentReference = reference,
    outcomeUnknown = reference != null || status >= 500, // pass `confirming` in; a 5xx on confirm is unknown
)
```
Pass `confirming` into `toHttpPaidFailure` and set `outcomeUnknown = confirming && status >= 500`.

### WR-03: A late availability response can clear the selected slot and rotate `bookingAttemptId` in the middle of a paid booking

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingViewModel.kt:788-824` (and `323-344`, `570`)

**Issue:** `book()` does not cancel `availabilityJob`, and `applyAvailability` does not check `isSubmitting`. The typical trigger:
1. After a `SLOT_UNAVAILABLE`, `loadAvailability(keepVisible = true)` keeps the stale slots on screen.
2. The user picks one and taps Book Now before the reload lands.
3. If the fresh data no longer shows that slot, `applyAvailability` sets `selectedSlotStart = null` mid-flow and calls `newBookingAttempt()` (line 817).

Consequences:
- The Summary changes while the CTA spinner runs, which breaks the class invariant ("the request, the Summary and the 409/422 side effects all refer to the same slot").
- If confirm then ends with a D-16 unknown outcome, `unknownOutcomeAttemptId` records the rotated id rather than the one sent, so D-19 can never fire.
- After any failure the user has lost the slot.

**Fix:** Cancel `availabilityJob` when a paid or free submission starts, or make `applyAvailability` keep the selection and the attempt id while `isSubmitting`:
```kotlin
val locked = _uiState.value.isSubmitting
val slot = if (locked) s.selectedSlotStart else when { ... }
...
if (!locked && slot != previousSlot) newBookingAttempt()
```

### WR-04: A 3DS challenge that completes after process death (or with no active challenge) is dropped without notice, which can leave an orphaned authorization

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingViewModel.kt:543, 618-620`

**Issue:** During a 3DS challenge the app is often in the background (a bank app, or a browser redirect), so the process can be killed. Two things combine:
- `confirmPaid` removes `KEY_ATTEMPT_ID` on the 402 (line 543).
- `challengeAccount`, `paidContext`, `pendingPaymentMethodId` and `paymentIntentId` live only in memory.

After a restore, the form is idle. The PaymentLauncher result is still delivered (its activity-result registration is restored), but `onChallengeOutcome` returns immediately because `challengeAccount == null`. If the challenge succeeded (once CR-01 is fixed), the PaymentIntent is authorized with no booking. The user is told nothing, and their next tap mints a new attempt id and a second authorization hold. D-17 covers regenerating the attempt id; it does not cover silently discarding a completed authorization.

**Fix:**
- At minimum, show `make_a_booking_error_unconfirmed` (check My Bookings / contact support) when a challenge outcome arrives with no active challenge.
- Better, persist the non-secret ids (`pi_`, `pm_`, topic, slot start, meeting type) in `SavedStateHandle` for the 402 window and resume the confirm. Never persist the client secret or the connected account id.

### WR-05: A rotation while the FlowController is configuring shows a spurious "Couldn't book" alert containing raw SDK developer text

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/PaidFlowStripeHost.kt:83-92` (also `StripeResultMapping.kt:11,19`, `MakeABookingViewModel.kt:455-461, 636-641`)

**Issue:** `configureWithSetupIntent` runs in the Stripe FlowController ViewModel scope, so it survives recreation. Its callback, however, captures the `flowController` of the old composition. If the activity rotates during configuration (a network round trip), the sequence is:
1. The callback calls `presentPaymentOptions()` on the destroyed controller.
2. That controller's launcher was unregistered in `onDestroy`, so the SDK catches the `IllegalStateException` and reports `PaymentSheetResult.Failed("The host activity is not in a valid state (DESTROYED).")`.
3. `onSheetOutcome(Failed)` shows that English developer string in the "Couldn't book" dialog and resets the flow.

The same path surfaces "FlowController must be successfully initialized…" texts. `toSheetOutcome` / `toChallengeOutcome` pass `throwable.message` through without checking that it is user-facing.

**Fix:**
- Present from the current composition. For example, keep a `rememberSaveable` "pending present" flag set by the configure callback, and have a `LaunchedEffect` in the new composition call `presentPaymentOptions()` on the current controller. The second configure with the same request is skipped by `canSkip`.
- Only surface `StripeException.localizedMessage` for card errors. Map every other failure to `make_a_booking_error_sheet_failed` / `make_a_booking_error_sca_failed`.

### WR-06: `refreshPaymentMethods()` skips while an earlier load is running, so returning from Payment Methods can show a stale "Add a Payment Method"

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingViewModel.kt:193-196`

**Issue:** The guard `paymentMethodsJob?.isActive == true` exists to avoid a duplicate load on first composition. But it also drops the D-09 reload when the client returns while the initial (or previous) `GET /me/payment-methods` is still running:
1. On a slow network, the user taps "Add a Payment Method" before the first load returns.
2. The user adds a card and pops back.
3. The refresh is skipped, and the stale empty response lands as `None`.

The CTA stays "Add a Payment Method" until the next resume, which breaks D-09.

**Fix:** Restart instead of skipping (`loadPaymentMethods()` already cancels the previous job), or only skip when the active job was started after the last `ON_PAUSE`:
```kotlin
fun refreshPaymentMethods() {
    if (_uiState.value.isSubmitting) return
    loadPaymentMethods() // cancels any older in-flight load
}
```
To avoid the double request on first composition, have the screen call only one of `LaunchedEffect(Unit)` / `ON_RESUME`.

### WR-07: Payment Methods can show a stale list after a successful change, and an older load can overwrite a newer reload

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/settings/paymentmethods/PaymentMethodsViewModel.kt:66-74, 107-111, 159-165`

**Issue:** There are two gaps:
- **Failed reload is invisible.** After a successful DELETE or PATCH, `reloadSilently()` swallows a failure. The deleted card (or the old default) stays on screen with no indication. A second "Remove" on that card then 404s and shows "Couldn't remove", even though the first removal worked.
- **Reload race.** `reloadSilently()` is not tracked by `loadJob`. So an earlier `load(showLoading = false)`, started after an add-card, can finish after the post-delete reload and put the deleted card back. The add-card reload failure is likewise silent, so the new card never appears.

**Fix:**
- Route every reload through `load()`, so `loadJob` cancels older loads.
- When a post-mutation reload fails, remove a deleted card locally. The server answer was a success, and this is not an optimistic update.
- Otherwise, show a non-blocking "Couldn't refresh" state with Retry.

### WR-08: The dev flavor still ships the mismatched Stripe publishable key, so every paid flow and add-card fails in day-to-day builds

**File:** `app/build.gradle.kts:41-51`

**Issue:** The comment itself says `pk_test_51Tha0r…` belongs to a different Stripe account than the backend's secret key. With that key, all of these fail with "No such setupintent":
- `FlowController.configureWithSetupIntent`
- `PaymentSheet.presentWithSetupIntent`
- `retrieveSetupIntentSynchronous`
- the PaymentLauncher

So the whole of Phase 7 cannot work in the `dev` flavor, and the only flavor that matches the backend account is `prod` with `pk_live`. D-01/D-02 track this as non-blocking, but until the swap happens the dev build is guaranteed broken for PAY-01 to PAY-06. If someone tests on the `prod` flavor instead, they risk real cards against a live account (D-02).

**Fix:** Swap in the `pk_test_` key of the backend's account (D-01) before any further device testing. Consider failing the dev build, or showing a debug banner, when the key's account prefix differs from the expected `51U8OTJ`.

## Info

### IN-01: `PaymentIntentResponse.clientSecret` is required but never used

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/model/responses/PaymentIntentResponse.kt:18-20`
**Issue:** Path A never uses the booking PaymentIntent secret. Declaring it non-null means a backend change that omits it turns every short-lead booking into a `Decoding` failure, and it keeps a secret in memory for no reason.
**Fix:** Drop the field (Moshi ignores unknown keys), or make it `String? = null`.

### IN-02: Long-lead uses a cached card list that is never refreshed after `INVALID_PAYMENT_METHOD`

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingViewModel.kt:489-501`
**Issue:** `preferredPaymentMethodId()` reads the cards loaded when the form opened. If the card was removed elsewhere (iOS, another device), every retry sends the same stale `pm_` until the next resume.
**Fix:** Call `loadPaymentMethods()` after an `InvalidPaymentMethod` failure, or re-list right before `performLongLeadBooking`.

### IN-03: Paid Request Sent tells long-lead bookings that "A hold is placed on your card", marked Done

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/requestsent/BookingRequestSentScreen.kt:229-240`
**Issue:** Long-lead bookings (more than 6 days out) get no authorization at confirm; the backend charges at T-24h. The `HoldPlaced` card is inaccurate for them. This matches iOS, so it is a copy and parity follow-up rather than a regression.
**Fix:** Raise it with the iOS owner. If changed, pass an `Upcoming`-style stage when `payment_status == "scheduled"`.

### IN-04: `DefaultStripePaymentMethodReader` only catches `StripeException` and `IOException`

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/stripe/StripePaymentMethodReader.kt:35-43`
**Issue:** `retrieveSetupIntentSynchronous` validates the secret format with `require` (`IllegalArgumentException`). Any `RuntimeException` there crashes the app through `viewModelScope`, which has no handler (`MakeABookingViewModel.kt:472-480`).
**Fix:** Also catch `IllegalArgumentException` (or `RuntimeException`, excluding `CancellationException`) and return null.

### IN-05: `MakeABookingViewModel` is over 1,000 lines, and its KDoc carries stale plan references

**File:** `app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/makeabooking/MakeABookingViewModel.kt:1-1008`
**Issue:** One class holds the free path, the paid state machine, the 3DS handling, the clock and zone sync, availability and WR-01 recovery. Comments such as "The screen's resume hook is wired by plan 07-12" (line 191) are already out of date. Mixing so many responsibilities is what allowed WR-03 (availability touching paid-flow state).
**Fix:** Extract the paid flow into its own state holder, for example `PaidBookingCoordinator`, with a narrow interface: start, outcome callbacks and failure mapping. Remove the plan references from the KDoc.

---

_Reviewed: 2026-10-23T15:19:36Z_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
