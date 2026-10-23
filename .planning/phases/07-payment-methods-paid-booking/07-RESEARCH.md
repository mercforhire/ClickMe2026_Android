# Phase 7: Payment Methods & Paid Booking - Research

**Researched:** 2026-10-22
**Domain:** Stripe Android SDK (PaymentSheet setup mode, FlowController, PaymentLauncher) inside an MVVM Compose app, against a Path A (server-confirms) Connect direct-charge backend
**Confidence:** MEDIUM-HIGH (backend contract and iOS behavior HIGH; Stripe Android API surface HIGH for signatures, MEDIUM for runtime behavior, because nothing in this flow has ever run green on a device)

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

Carried forward (not re-opened):
- **iOS is the source of truth (DEC-ios-source-of-truth).** Material 3, dark only, Nunito. Pixel parity is out of scope.
- **Phase 6 decisions stand:**
  - The route is `BookingKey`.
  - Errors appear in an `AlertDialog` titled "Couldn't book" (D-10).
  - Request Sent replaces the form in the back stack (D-12).
  - Request Sent fetches `GET /client/bookings/:id` (D-14).
  - A tap during an in-flight request is ignored (D-11).
  - `Money.kt` is the shared formatter.
- **Phase 2 D-12 (spike-validated 3DS approach):**
  - Use `detail.client_secret` and `detail.stripe_account` from the 402.
  - Run `PaymentLauncher` with `stripeAccountId`, and never save that id.
- **Money contract (PROJECT.md):** amounts are minor units everywhere except `payment-intent` amounts. JPY has no decimal places.
- **API pattern:** `ApiRequestUrls` constants, `ClickMeApi` methods, and a Hilt `@Singleton` repository using `apiCall {}`. DTOs are Moshi codegen with explicit `@Json(name=…)`.

Stripe test key & device testing
- **D-01: The user supplies the correct test key.** The user gets the publishable test key (`pk_test_…`) of the Stripe account behind the backend's secret key (the `51U8OTJ…` account, the same account as iOS prod's `pk_live`) from the Stripe dashboard. The dev flavor's `STRIPE_PUBLISHABLE_KEY` in `app/build.gradle.kts` is then swapped. This is the fix for the open Phase 2 UAT gap ("client secret does not match any associated SetupIntent on this account", 02-UAT tests 7–8).
- **D-02: Confirm the backend's Stripe mode first.** The user doesn't know whether the backend's Stripe account is in test or live mode. Phase 7 includes an early `checkpoint:human-action` (non-blocking):
  - Confirm the mode in the dashboard.
  - Provide the matching publishable key.
  - If the backend is **live**, device testing must not use real cards against it. Flag it and stop device tests until a test-mode path exists.
- **D-03: Code first, device test later.** Phase 7 is built and unit-tested without waiting on the key. Device checks (PaymentSheet add card, short-lead with the iOS expert receiving the request, long-lead, 3DS with `4000 0025 0000 3155`) are logged as open human-verification items, like Phases 3–6. No plan blocks on them.

Payment Methods screen (PAY-01, PAY-02)
- **D-04: Two entry points.**
  - A "Payment Methods" row on the client Profile placeholder tab, next to Log out. Phase 14's Profile hub replaces this row.
  - The booking gate (D-08).
  - The screen lives in its own key and mirrors the iOS tree: `screens/settings/paymentmethods/`.
- **D-05: Row actions use a trailing ⋮ overflow `DropdownMenu`** with "Set as default" and "Remove" (the iOS `···` menu). The default card shows a "Default" badge. Rows show the brand, `•••• last4` and the expiry (iOS `PaymentMethodRow`).
- **D-06: Remove asks first.**
  - An `AlertDialog` asks "Remove this card?" before the DELETE.
  - The row shows a spinner during delete or set-default, and only one such operation runs at a time (iOS `deletingId` / `settingDefaultId`).
  - A failure shows a "Couldn't remove" or "Couldn't update" alert.
- **D-07: After a delete, reload and show the server's default.** The app never promotes another card to default by itself.
- Add card: tap Add, then `setup-intent`, then PaymentSheet in setup mode with the customer id and ephemeral key, so saved cards are listed. On completion, reload the list. Cancel does nothing. A failure shows an alert. The empty state and the load error with Retry follow iOS.

Paid CTA & no-card gate
- **D-08: Always gate on a saved card (iOS parity).**
  - Make a Booking loads `/me/payment-methods` along with topics and availability.
  - For **any** paid topic, if the client has no saved card (or the load failed, which iOS treats as no cards), the CTA reads "Add a Payment Method" and opens Payment Methods.
  - This covers short-lead too.
- **D-09: Return to the form, and the client taps again.**
  - Coming back from Payment Methods reloads the card list.
  - The form keeps every selection. The CTA becomes "Book Now", and the client taps it to start the paid flow.
  - Nothing continues automatically.
- **D-10: The default topic goes back to `topics.first` (iOS parity).** This replaces Phase 6 D-02 (first free topic). Changing the topic still clears the slot.
- **D-11: Regenerate `bookingAttemptId` on a topic or slot change (iOS `didSet`).** A network retry after a failure reuses the same id.
- **D-12: Price info follows iOS.**
  - The Summary shows the topic price through `Money.kt`.
  - Add a "How does payment work?" link that opens a `ModalBottomSheet` (platform swap for iOS `PaymentInfoSheet`). It contains the headline, the "You pay exactly the session price — no additional fees" line, the lifecycle steps in the upcoming state, and a "Got it" button.
  - Don't show the `payment-intent` summary total. iOS stores it and never displays it.
- PaymentSheet dismissed without picking a card, or a `FlowController` cancel: reset the paid flow silently and keep the form. The FlowController ports iOS `MakeABookingStripePaymentSheet`:
  - `merchantDisplayName = "ClickMe"`
  - `allowsDelayedPaymentMethods = false`
  - the customer config from the setup-intent response
  - read the `pm_…` from the confirmed SetupIntent

Errors, 3DS & retry (PAY-05, PAY-06)
- **D-13: Port the iOS `handlePaidFlowError` table**, with its copy and side effects, for these codes:
  - `PAYMENT_REQUIRES_ACTION`
  - `BOOKING_ATTEMPT_CONFLICT`: new attempt id, "Please try booking again."
  - `INTENT_EXPIRED` / `PI_CONTEXT_MISMATCH` / `PI_AMOUNT_MISMATCH`: new attempt id, the server message
  - `EXPERT_NOT_BOOKABLE`
  - `INVALID_PAYMENT_METHOD`
  - `DIRECT_CHARGE_DISABLED`
  - `PAYMENT_FAILED`
  - `DUPLICATE_BOOKING`
  - `AMOUNT_BELOW_MINIMUM`
  - `INVALID_PROMO`
  - `CANNOT_BOOK_SELF`

  Any other code shows the server `message`. A failure in the confirm step adds "\n\nPayment reference: {pi_…}".
- **D-14: `SLOT_UNAVAILABLE` and `MEETING_LINK_UNAVAILABLE` share the free path's handler (Phase 6 D-08, D-09).**
  - Slot taken: alert, clear the slot, reload the displayed month, and start a new attempt id.
  - Meeting link missing: the D-08 alert, and switch back to In-app Voice.
  - This deviates on purpose from the iOS paid copy, for consistency.
- **D-15: When 3DS is cancelled or fails, show an alert and keep the form.**
  - Cancel: "Payment wasn't completed. Your card wasn't charged."
  - Failure: the Stripe error message.
  - Either way, reset the paid flow and keep the slot and the same `bookingAttemptId`, so tapping Book Now again is an idempotent retry.
  - A missing or empty `detail.stripe_account` is a hard failure with an alert (iOS).
- **D-16: An unknown outcome on confirm shows the network copy and the payment reference.** A network error or timeout on `/bookings/confirm` shows the app's standard network-error copy plus "Payment reference: {pi_…}" when one exists. Keep the attempt id. No automatic retry.
- **D-17: `bookingAttemptId` lives in memory only (iOS parity).** It is not saved in `SavedStateHandle`, so it is regenerated if the process dies. The rest of the form state keeps Phase 6's `SavedStateHandle` handling.

Request Sent (paid)
- Request Sent gets the paid payment-lifecycle card (iOS `PaymentLifecycleCard` in the hold-placed state) and the paid headline when `BookingRequestSentDetails.isFree == false`. The Phase 6 hand-off closes here.

### Claude's Discretion
- The Stripe Compose integration API:
  - `rememberPaymentSheet` / `PaymentSheet.FlowController.Builder` / `rememberPaymentLauncher`, or the Activity-scoped builders
  - how `PaymentConfiguration` is initialized from `BuildConfig`
  - the PaymentSheet `Appearance`, matched to the dark theme
- How the ViewModel exposes the paid step (`idle` / `creatingIntent` / `collectingPaymentMethod` / `confirming`) and one-shot events to the Compose layer that launches the Stripe UIs.
- Payment Methods skeleton and empty-state visuals, and the brand icons (iOS `PaymentMethodsBrand`).
- The exact copy for the empty state, the dialogs and the lifecycle steps. Port the iOS copy where it exists.
- Where the shared lifecycle-steps composable lives, so the info sheet and Request Sent share it.
- Whether the Payment Methods destination is registered once in the client shell and reached from both the Profile tab and the Explore stack.

### Deferred Ideas (OUT OF SCOPE)
- Auto-continuing the booking after a card is added (D-09 chose re-tap).
- Gating only long-lead bookings on a saved card (D-08 kept the iOS always-gate rule).
- Showing the server `payment-intent` total before paying (D-12).
- Saving `bookingAttemptId` across process death (D-17 chose memory only).
- Promo codes.

Out of scope for the phase (CONTEXT domain): promo codes (`promo_code` always null), the client Profile hub (Phase 14), payment history (DEF-4), reschedule payment conversion (DEF-1 / Phase 8), expert payouts (Phase 14).
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| PAY-01 | Client can view saved cards in Payment Methods, set a default card and delete a card. | `GET /me/payment-methods`, `PATCH /me/payment-methods/:id/default`, `DELETE /me/payment-methods/:id` shapes (STRIPE-INTEGRATION §4, controller read). Busy-rule, reload-not-optimistic pattern (D-06/D-07). Empty-body `{}` strict schema on PATCH. |
| PAY-02 | Client can add a card through a SetupIntent and Stripe PaymentSheet in setup mode. | `POST /me/payment-methods/setup-intent` (`{}` body, 10 per 15 min per user), `PaymentSheet.Builder(...).build()` + `presentWithSetupIntent`, `CustomerConfiguration`. Spike code in `spikes/stripe/` is the reusable base. |
| PAY-03 | Short-lead paid booking: `payment-intent` (with reusable `booking_attempt_id`), setup-intent, PaymentSheet `FlowController`, `confirm`. | iOS `beginPaidBooking` / `completePaidBooking`; Android FlowController API (`configureWithSetupIntent`, `presentPaymentOptions`, `confirm`); `pm_` read via `Stripe.retrieveSetupIntent`; exact request bodies (confirm has no `booking_attempt_id`). |
| PAY-04 | Long-lead paid booking: on `422 LONG_LEAD_REQUIRES_SAVED_CARD` confirm with the saved card, or send the client to Payment Methods when there is none. | Backend `createPaymentIntent` guard (> 6 days), `confirm()` fork on missing `payment_intent_id`; iOS `preferredPaymentMethodId` (default, else first). |
| PAY-05 | 3DS: on `402 PAYMENT_REQUIRES_ACTION` run `PaymentLauncher` with `stripeAccountId`, then retry confirm. | Backend 402 `detail` shape; `rememberPaymentLauncher(pk, stripeAccountId, cb)`; spike pattern (state first, launch from `LaunchedEffect`); consume-on-launch guard; one SCA round max. |
| PAY-06 | Each of the ~15 `handlePaidFlowError` codes shows the matching message or next action. | Full code to message to side-effect table (below); all 15 enum cases already exist in `ErrorCode.kt`. |
| PAY-07 | Prices and charges display correctly: minor units everywhere except `payment-intent` amounts; JPY has no decimals. | `Money.format` already handles JPY via `Currency.defaultFractionDigits`; `payment-intent` is bare JSON with decimal major-unit `total_amount` (do not declare it as `Long`; the app never displays it). |
</phase_requirements>

## Summary

The backend contract is mature and unambiguous: Path A (the app collects a `pm_…` through a SetupIntent and the server confirms the booking PaymentIntent on the expert's connected account), with one client-side step, 3DS through `PaymentLauncher` scoped to `detail.stripe_account`. iOS (`MakeABookingViewModel`, `MakeABookingStripePaymentSheet`, `PaymentMethods*`) is a complete reference and nearly all behavior ports one-to-one. The 15 error codes already exist in `ErrorCode.kt`, `Money.kt` already handles JPY, `ApiException.Http.detail` already carries the 402 payload, and the Phase 6 form has the single-flight/lock plumbing (`isSubmitting`) that the paid flow can reuse.

The real risk is not the logic; it is that **no part of the Android Stripe path has ever run successfully on a device**. Phase 2's spike (`spikes/stripe/`) failed UAT 7 and 8 on a publishable-key/account mismatch (D-01), and it only covers `PaymentSheet.presentWithSetupIntent` and `PaymentLauncher`. It contains **no FlowController code and no `pm_…` retrieval**, which are the two new Android-specific pieces this phase must write blind. Also, `:app` does **not yet depend on Stripe**: `libs.stripe.paymentsheet` is in the version catalog but absent from `app/build.gradle.kts` dependencies. R8 (`assembleProdRelease` is part of the gate) must be re-verified after adding it. Plans must therefore put every Stripe SDK touchpoint behind small interfaces so ViewModels are fully unit-testable, and treat the device checks as the only proof of the Stripe wiring.

Four items in the UI-SPEC/CONTEXT need correction before planning (details in Pitfalls and Standard Stack): (1) `PaymentSheet.UserInterfaceStyle.AlwaysDark` does not exist on Android PaymentSheet (force dark by setting `colorsLight == colorsDark`); (2) a type named `PaymentMethodsBrand` fails the CI design rule that forbids `*Brand` types; (3) the 3DS test card `4000 0025 0000 3155` will probably not produce the 402 (use `4000 0027 6000 3184`); (4) the `setup-intent` endpoint is rate-limited to 10 per 15 min per user and every short-lead attempt spends one.

**Primary recommendation:** Add `implementation(libs.stripe.paymentsheet)` (stay on the locked 23.24.0), initialize `PaymentConfiguration` in `ClickMeApplication.onCreate`, build the paid flow as a pure-Kotlin state machine in `MakeABookingViewModel` that talks to Stripe only through two injected interfaces (a `pm_` reader and nothing else; the Compose layer owns FlowController/PaymentSheet/PaymentLauncher and reports results back), and port `handlePaidFlowError` as a pure, table-tested function.

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Saved-card list, default, delete | Backend (Stripe Customer is source of truth) | Android ViewModel (reload, busy flags) | Server owns default; app never promotes a default (D-07) and reloads after every mutation |
| Card entry (PAN capture, SetupIntent confirm, 3DS during setup) | Stripe SDK UI (PaymentSheet / FlowController activities) | Compose host (launch + result routing) | PCI scope stays inside Stripe's UI; app never sees card data |
| Booking PI creation, authorization, confirm on connected account | Backend | — | Path A: the app must never confirm the booking PI itself |
| `pm_…` extraction after setup | Android data layer (`Stripe.retrieveSetupIntent`, publishable key only) | — | FlowController does not expose the id; mirrors iOS `retrieveSetupIntent` |
| 3DS challenge on connected-account PI | Stripe SDK UI (`PaymentLauncher` with `stripeAccountId`) | Android ViewModel (holds challenge in memory, single use) | Needs `acct_…` only on this step; never persisted |
| Paid-step state machine, attempt id, retry/idempotency | Android ViewModel | Backend (idempotency key from `booking_attempt_id`) | Same split as iOS `PaidStep` |
| Error to message/side-effect mapping | Android ViewModel (pure function) | UI (dialog) | Pure and unit-testable; copy in string resources |
| Gate CTA (no saved card) and navigation to Payment Methods | Android ViewModel (derived state) | Navigator / Nav3 | iOS `requiresPaymentMethodSetup` |
| Price display | Android util (`Money.format`) | — | Minor units only; `payment-intent` totals are never displayed |
| Paid lifecycle card (Request Sent, info sheet) | Android Compose (static copy) | — | No server data except slot time |

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| `com.stripe:paymentsheet` | 23.24.0 (locked in `libs.versions.toml`; 23.25.0 was published to Maven Central today, 2026-10-22) | PaymentSheet, FlowController, and (transitively) `payments-core` for `PaymentLauncher`, `Stripe`, `PaymentConfiguration` | Already pinned and built in the Phase 2 spike. Do not bump during this phase: 23.25.0 is hours old and only adds payment-method types |
| Existing stack | per `libs.versions.toml` | Retrofit 3.0.0 + Moshi 1.15.2 (codegen), Hilt, Navigation3 1.2.0, Compose BOM 2026.09.00, coroutines 1.11.0 | Project conventions (CLAUDE.md); no new libraries needed |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| Robolectric / `ui-test-junit4` / `coroutines-test` / `okhttp-mockwebserver3` | already in `testImplementation` | VM tests, Compose tests, ClickMeApi tests | All validation below; no new test libs |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| `com.stripe:paymentsheet` | `com.stripe:stripe-android` umbrella | Umbrella is what Stripe's docs name, but it pulls extra modules the app does not use. The spike proved `paymentsheet` alone resolves `PaymentSheet` and `PaymentLauncher` |
| FlowController + SetupIntent (iOS parity) | Deferred `IntentConfiguration` FlowController | Not applicable: the backend dictates a real SetupIntent client secret (`/me/payment-methods/setup-intent`) |
| `Stripe.retrieveSetupIntent` for the `pm_` | `GET /me/payment-methods` and pick the newest/default | The spike used the list endpoint because it never had the FlowController. The list cannot tell which saved card the user just chose, so it is wrong for the booking flow. Use `retrieveSetupIntent` (iOS parity) |

**Installation (one line in `app/build.gradle.kts`, no catalog change):**
```kotlin
implementation(libs.stripe.paymentsheet)
```

**Version verification:** Maven Central `com/stripe/paymentsheet/maven-metadata.xml` lists 23.22.0 to 23.25.0, `lastUpdated 2026-10-22`. [VERIFIED: repo1.maven.org]. 23.24.0 release notes (2026-10-19) note only a breaking move of `ApiConfigurationPreview` to `stripe-core` (not used here). [CITED: github.com/stripe/stripe-android/releases]

## Package Legitimacy Audit

No new package coordinates are introduced: `com.stripe:paymentsheet:23.24.0` is already in `gradle/libs.versions.toml` (Phase 2) and resolved and built in the `:spikes:stripe` module. `slopcheck` could not be run (no `pip` on this machine, and it targets npm/PyPI rather than Maven).

| Package | Registry | Age | Downloads | Source Repo | slopcheck | Disposition |
|---------|----------|-----|-----------|-------------|-----------|-------------|
| `com.stripe:paymentsheet` | Maven Central | years (Stripe first-party; 23.x line) | n/a | github.com/stripe/stripe-android | unavailable | Approved in Phase 2 human-verify checkpoint; [ASSUMED] per protocol because slopcheck did not run, but it is a first-party artifact already building in this repo. The planner does not need a new `checkpoint:human-verify` for it |

**Packages removed due to slopcheck [SLOP] verdict:** none
**Packages flagged as suspicious [SUS]:** none

## Architecture Patterns

### System Architecture Diagram

```
                        Make a Booking screen (Compose)
   Book Now tap ───────────────► MakeABookingViewModel.book()
                                       │  (isSubmitting=true, paidStep=CreatingIntent)
                                       ▼
   POST /bookings/payment-intent {…, booking_attempt_id}   (bare JSON response)
        │ 422 LONG_LEAD_REQUIRES_SAVED_CARD                │ 200 {payment_intent_id,…}
        ▼                                                   ▼
   preferredPm = default ?: first of loaded cards      POST /me/payment-methods/setup-intent {}
        │  (none ⇒ alert "Please add a payment method")     │  → {client_secret, customer_id, ephemeral_key}
        ▼                                                   ▼
   paidStep=Confirming                          paidStep=CollectingPaymentMethod
        │                                         VM publishes a one-shot "present" request
        │                                                   ▼
        │                              Compose host (PaidFlowStripeHost, in MakeABookingScreen)
        │                              FlowController.configureWithSetupIntent(secret, config{customer})
        │                                 → presentPaymentOptions()  ── Stripe activity ──┐
        │                              PaymentOptionResult(paymentOption, didCancel)       │
        │                                 null/cancel ⇒ VM.cancelPaidFlow() (silent)        │
        │                                 else flowController.confirm() ── Stripe activity ─┘
        │                              PaymentSheetResult: Completed / Canceled / Failed
        │                                                   ▼ Completed
        │                              VM → StripePaymentMethodReader.read(setupIntentSecret)   (retrieveSetupIntent → paymentMethodId)
        │                                                   ▼ pm_…  (null ⇒ "Couldn't read the selected payment method…")
        └──────────────────────────────►  POST /bookings/confirm {payment_intent_id?, expert_id, topic_id,
                                                               start_time, meeting_type, stripe_payment_method_id, client_notes?}
                                                   │ 201 {data.booking_id}      │ 402 PAYMENT_REQUIRES_ACTION
                                                   ▼                            ▼ detail{client_secret, stripe_account}
                                        persist completed id (WR-01)     VM holds Challenge in memory (single use)
                                        completedBookingId ⇒ Screen      Compose: rememberPaymentLauncher(pk, stripeAccount)
                                        → navigator.replaceTop(          → handleNextActionForPaymentIntent(client_secret)
                                          RequestSentKey(id))              Completed ⇒ re-POST confirm (same pm_, same PI)
                                                                           Canceled/Failed ⇒ alert, reset, same attemptId
   Any ApiException ⇒ handlePaidFlowError(code, phase) ⇒ dialog + side effects (table below)
```

### Recommended Project Structure
```
app/src/main/java/com/uptrendinvestments/clickmeapp/
├── network/                       # + ApiRequestUrls consts, ClickMeApi methods (5 payment-methods + 2 booking)
├── model/requests/                # PaymentIntentRequest, ConfirmBookingRequest
├── model/responses/               # PaymentIntentResponse (BARE), ConfirmBookingData, PaymentMethodsData/Item, PaymentMethodSetupData
├── stripe/                        # StripePaymentMethodReader (interface + Default impl), StripeAppearance builder (Composable-side)
├── di/StripeModule.kt             # @Binds reader; PaymentConfiguration.init stays in ClickMeApplication
├── components/                    # PaymentLifecycleCard.kt, ProfileMenuRow.kt (UI-SPEC)
├── screens/settings/paymentmethods/
│   ├── PaymentMethodsKey.kt  PaymentMethodsScreen.kt  PaymentMethodsViewModel.kt  PaymentMethodsUiState.kt
│   ├── PaymentMethodsRepository.kt            # @Singleton, shared with the booking VM
│   └── subviews/ PaymentMethodRow.kt  PaymentMethodBrandDisplay.kt (functions only, see Pitfall 3)  PaymentMethodRemoveDialog.kt
└── screens/clientmode/makeabooking/
    ├── PaidBookingFlow.kt / PaidFlowError.kt  # pure: step model + handlePaidFlowError table (no Android deps)
    ├── PaidFlowStripeHost.kt                  # Compose-only: FlowController + PaymentLauncher, forwards results to VM
    └── subviews/ PaymentInfoSheet.kt  MakeABookingPaymentInfoLink.kt  (BookButton gains the CTA switch)
```

### Pattern 1: Stripe stays out of the ViewModel; results flow back in as plain events
**What:** `MakeABookingViewModel` holds `paidStep` plus a `StripeRequest?` (a sealed type: `PresentSheet(setupSecret, customerId, ephemeralKey)` or `RunChallenge(clientSecret, stripeAccount)`). The Compose host launches it exactly once and reports `onSheetResult`, `onChallengeResult`. Only the `pm_` lookup is a suspend call inside the VM, through an injected `StripePaymentMethodReader`.
**When to use:** Always. FlowController and PaymentLauncher need an `ActivityResultRegistry`, so they must live in composition; the VM cannot own them.
**Why consume-on-launch:** state-based requests re-fire after rotation. The spike's `consumeSetupRequest()` pattern is correct: the host calls `vm.onStripeRequestLaunched()` (sets the request to null, keeps `paidStep`) right after launching. Do not use a `Channel` unless every consumer is collected under `repeatOnLifecycle`; a plain nullable state with explicit consume is simpler to test.

### Pattern 2: FlowController in setup mode (Android shape differs from iOS)
**What:** iOS has `FlowController.create(setupIntentClientSecret:)` and `confirm(from:)` completions. Android has a Compose builder with two callbacks and an async configure:
```kotlin
// Source: docs.stripe.com/payments/accept-a-payment-deferred?platform=android (FlowController Compose section) and
// PaymentSheet.kt in stripe-android master (configureWithSetupIntent, Builder constructors)
val flowController =
    remember {
        PaymentSheet.FlowController.Builder(
            resultCallback = { result -> vm.onSetupConfirmResult(result) },       // PaymentSheetResult
            paymentOptionResultCallback = { r ->                                  // PaymentOptionResult(paymentOption, didCancel)
                if (r.didCancel || r.paymentOption == null) vm.cancelPaidFlow() else flowController.confirm()
            },
        )
    }.build()   // @Composable build()

LaunchedEffect(request) {                       // request = vm.stripeRequest, a PresentSheet(...)
    val r = request as? StripeRequest.PresentSheet ?: return@LaunchedEffect
    vm.onStripeRequestLaunched()
    flowController.configureWithSetupIntent(
        setupIntentClientSecret = r.setupSecret,
        configuration = bookingSheetConfiguration(r.customerId, r.ephemeralKey, appearance),
    ) { success, error ->
        if (success) flowController.presentPaymentOptions() else vm.onStripeFailure(error?.message)
    }
}
```
Notes: `configureWithSetupIntent(setupIntentClientSecret, configuration, callback: ConfigCallback)`; `presentPaymentOptions()`, `confirm()` and `getPaymentOption()` take no arguments. [CITED: PaymentSheet.kt, stripe-android master]. Do not call `presentPaymentOptions()` or `confirm()` during configuration. [CITED: docs.stripe.com]. `flowController` referenced inside its own `remember` lambda needs a `lateinit`-style holder (declare a `var fc: PaymentSheet.FlowController? by remember { mutableStateOf(null) }` or a `val holder = remember { arrayOfNulls<…>(1) }`).

### Pattern 3: 3DS with a launcher keyed by the account id (from the spike)
```kotlin
// Source: spikes/stripe/.../MainActivity.kt (Phase 2) + PaymentLauncher.kt (stripe-android master)
val challenge = state.challenge                         // Challenge(clientSecret, stripeAccount)? held in the VM, memory only
val launcher = rememberPaymentLauncher(
    publishableKey = BuildConfig.STRIPE_PUBLISHABLE_KEY,  // via AppConfig.stripePublishableKey in screen-level code
    stripeAccountId = challenge?.stripeAccount,
    callback = { result -> vm.onChallengeResult(result) } // PaymentResult.Completed / Canceled / Failed(throwable)
)
LaunchedEffect(challenge) {
    challenge ?: return@LaunchedEffect
    vm.onChallengeLaunched()                             // consume FIRST so a recreation cannot start a second challenge
    launcher.handleNextActionForPaymentIntent(challenge.clientSecret)
}
```
`stripeAccountId` is a `remember` key inside `rememberPaymentLauncher`, so set the challenge in state and launch from `LaunchedEffect` after recomposition, never from the same handler that sets it. [CITED: 02-RESEARCH.md, `PaymentLauncher.kt`]

### Pattern 4: Reading the `pm_…` after a setup-mode confirm
FlowController exposes only `PaymentOption` (label and icon), never the id. iOS retrieves the SetupIntent. Android:
```kotlin
// Source: Stripe.kt in stripe-android master: retrieveSetupIntent(clientSecret, stripeAccountId, expand, callback: ApiResultCallback<SetupIntent>)
interface StripePaymentMethodReader { suspend fun paymentMethodIdOf(setupIntentClientSecret: String): String? }

class DefaultStripePaymentMethodReader @Inject constructor(@ApplicationContext private val context: Context) :
    StripePaymentMethodReader {
    override suspend fun paymentMethodIdOf(setupIntentClientSecret: String): String? =
        withContext(Dispatchers.IO) {
            try {
                Stripe(context, AppConfig.stripePublishableKey)
                    .retrieveSetupIntentSynchronous(setupIntentClientSecret)   // @WorkerThread, blocking
                    .paymentMethodId
            } catch (e: StripeException) { null } catch (e: IOException) { null }
        }
}
```
`SetupIntent.paymentMethodId: String?` exists. [CITED: SetupIntent.kt]. Use the synchronous variant inside `Dispatchers.IO` (there is no suspend variant). Never pass `stripeAccountId`: the SetupIntent lives on the platform account. A null result maps to "Couldn't read the selected payment method. Please try again." (UI-SPEC).

### Pattern 5: Pure error table (port of `handlePaidFlowError`)
A top-level function `fun ApiException.toPaidFailure(phase: PaidPhase, paymentIntentId: String?): PaidFailure` returning a data class (message `FieldError`, `newAttemptId`, `clearSlotAndReload`, `resetMeetingType`, `startChallenge`, `silent`). No Android types, table-tested like `BookingErrorsTest`. Table:

| Code | Message | Side effects (all also reset the paid flow) |
|---|---|---|
| `PAYMENT_REQUIRES_ACTION` with `detail.client_secret` | none | Stay in `Confirming`; set Challenge(clientSecret, stripeAccount?). Missing/empty account is a hard failure (D-15) |
| `PAYMENT_REQUIRES_ACTION` without `client_secret` | server message | reset |
| `BOOKING_ATTEMPT_CONFLICT` | "Please try booking again." | new attempt id |
| `INTENT_EXPIRED`, `PI_CONTEXT_MISMATCH`, `PI_AMOUNT_MISMATCH` | server message | new attempt id |
| `EXPERT_NOT_BOOKABLE` | existing `make_a_booking_error_not_bookable` | — |
| `SLOT_UNAVAILABLE` | existing slot-taken copy | clear slot, reload displayed month, new attempt id (D-14) |
| `MEETING_LINK_UNAVAILABLE` | existing no-link copy | meeting type back to In-app Voice (D-14) |
| `INVALID_PAYMENT_METHOD` | "This payment method can't be used for this booking. Please choose another card." | — |
| `DIRECT_CHARGE_DISABLED` | "Payments are temporarily unavailable. Please try again in a few minutes." | — |
| `PAYMENT_FAILED` | "Your card was declined. Please try another card." | — |
| `DUPLICATE_BOOKING` | existing duplicate copy | — |
| `AMOUNT_BELOW_MINIMUM` | "The session total is below the minimum charge amount." | — |
| `INVALID_PROMO` | "That promo code isn't valid." | — |
| `CANNOT_BOOK_SELF` | existing self copy | — |
| `ACCOUNT_SUSPENDED` | none (global dialog owns it, Phase 2 D-08) | reset (iOS would show the message; keep the Android rule) |
| `ApiException.Network` in confirm phase | network copy + payment reference (D-16) | keep attempt id |
| anything else | server message (+ reference when phase is confirm and a `pi_…` exists) | — |

That is 15 distinct codes (3 share a row). All exist in `ErrorCode.kt` today; nothing to add. [VERIFIED: codebase read]

### Anti-Patterns to Avoid
- **Confirming the booking PI client-side.** Path A forbids it; the app never calls `confirm` on `payment_intent_id`'s client secret. The only client-side PI call is `handleNextActionForPaymentIntent` after a 402. [CITED: STRIPE-INTEGRATION §1]
- **Sending `booking_attempt_id` on `/bookings/confirm`.** Confirm is `.strict()` with exactly: `payment_intent_id?`, `expert_id`, `topic_id`, `start_time`, `meeting_type`, `stripe_payment_method_id`, `client_notes?`, `promo_code?`. The spike's comment says the same. [VERIFIED: booking.schema.js]
- **Serializing nulls.** The converter omits nulls (no `withNullSerialization`). Keep it: `payment_intent_id = null` must be omitted for long-lead, not sent as `null`.
- **Persisting `paymentIntentId`, `pm_…`, `client_secret`, `ephemeral_key`, `stripe_account` or `bookingAttemptId` in `SavedStateHandle`** (D-17 and the secrets rule). Persisting any of them writes them to the system's saved-state bundle.
- **Optimistic UI on Payment Methods** (iOS does it; D-06/D-07 say reload from the server).
- **Auto-selecting the "new default" after a delete.** The server's answer wins (D-07).
- **Declaring `total_amount` / `summary.*` as `Long`.** They are decimal major units; Moshi's `Long` adapter throws on `42.0`. Do not declare them at all (nothing displays them). Unknown keys are ignored.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Card entry UI, SCA during setup, Link/wallet handling | Custom card form | `PaymentSheet` (add card) / `FlowController` (booking) | PCI, SCA, validation, localization |
| 3DS challenge | WebView to the issuer | `PaymentLauncher.handleNextActionForPaymentIntent` with `stripeAccountId` | Needs Stripe's authentication flow and the account header |
| Getting the `pm_` | Parsing PaymentSheet labels, or guessing from the card list | `Stripe.retrieveSetupIntentSynchronous(...).paymentMethodId` | Only reliable source of which card was just used |
| Currency formatting | New formatter | `Money.format(minorUnits, currency)` | Already handles JPY (0 digits) and unknown codes |
| Idempotency | Custom dedupe of booking creation | `booking_attempt_id` (server derives the Stripe idempotency key) plus the `DUPLICATE_BOOKING` / `SLOT_UNAVAILABLE` codes | Server enforces it |
| Error-body parsing | New parser | `ApiException.Http.code/serverMessage/detail` | `detail` is already a `Map<String, Any?>` |
| Pending-booking survival across process death | New persistence | `CompletedBookingStore` (Phase 6 WR-01) | Same failure mode on the paid path (see Pitfall 11) |

**Key insight:** every hard part is either Stripe's UI or the server's idempotency. The app's job is sequencing, single-flight, and mapping errors, all of which are pure and testable.

## Runtime State Inventory

Not a rename/refactor/migration phase. Omitted.

## Common Pitfalls

### Pitfall 1: `:app` has no Stripe dependency yet
**What goes wrong:** the catalog has `stripe-paymentsheet`, but `app/build.gradle.kts` `dependencies {}` does not use it (only `:spikes:stripe` does). Code that imports `com.stripe.android.*` will not compile.
**How to avoid:** first task adds `implementation(libs.stripe.paymentsheet)` and runs `assembleDevDebug` plus `assembleProdRelease`. Stripe pulls many transitive artifacts (Compose, Play Services wallet, etc.); check the merged manifest and APK size once.
**Warning signs:** unresolved reference on `PaymentSheet`.

### Pitfall 2: R8 and Stripe
**What goes wrong:** minified release builds with PaymentSheet have a history of R8 "missing class" failures for `com.stripe.android.pushProvisioning.*` (optional module). Flutter and RN wrappers hit it; no official Stripe fix confirmed. [CITED: github.com/flutter-stripe/flutter_stripe/issues/2077 (secondary)]
**How to avoid:** run `./gradlew assembleProdRelease` immediately after adding the dependency (it is part of the phase gate). If R8 fails, add `-dontwarn com.stripe.android.pushProvisioning.**` to `app/src/main/keepRules/rules.keep` and read the generated `missing_rules.txt`. Do not add it speculatively. [ASSUMED]

### Pitfall 3: CI design rule forbids `*Brand` types
**What goes wrong:** `scripts/check-design-rules.sh` fails on `(object|class|enum class|interface) +[A-Za-z0-9_]*Brand\b`. UI-SPEC names `PaymentMethodsBrand` (iOS's per-screen palette type). An `object PaymentMethodsBrand` would fail the gate.
**How to avoid:** implement the display-name and icon mapping as top-level functions in `PaymentMethodBrandDisplay.kt` (`fun brandDisplayName(raw: String?): String`). No type with `Brand` as a suffix. Also keep literals out of `Color(...)` (the rule matches `Color(0x`, `Color(red =`, `Color(<digit>`); `PaymentSheet.Colors(...)` is not matched.

### Pitfall 4: There is no `UserInterfaceStyle` on Android PaymentSheet
**What goes wrong:** UI-SPEC's `style = PaymentSheet.UserInterfaceStyle.AlwaysDark` does not exist for PaymentSheet (the only `ALWAYS_DARK` in the API dump is `LinkAppearance.Style`). On a phone in light mode the sheet would render light.
**How to avoid:** Stripe documents "effectively disable dark mode by setting `colorsDark` to the same value as `colorsLight`". Build one dark `PaymentSheet.Colors` from `MaterialTheme` tokens and pass it as both `colorsLight` and `colorsDark` (and for the primary button's light/dark colors). [CITED: docs.stripe.com/elements/appearance-api?platform=android]. `PaymentSheet.Colors` takes Compose `Color` values (11 colors: primary, surface, component, componentBorder, componentDivider, onComponent, subtitle, placeholderText, onSurface, appBarIcon, error), so `toArgb()` is unnecessary. `Typography.default.copy(fontResId = R.font.nunito)` works; `res/font/nunito.ttf` exists. Exact `PrimaryButton`/`PrimaryButtonColors` parameter names should be confirmed in the IDE. [ASSUMED]. `primaryButtonLabel` applies to PaymentSheet only (not FlowController).

### Pitfall 5: `setup-intent` rate limit (10 per 15 min per user)
**What goes wrong:** every short-lead paid attempt mints a SetupIntent. A tester (or a user who cancels the sheet repeatedly) is blocked after 10 with `429 TOO_MANY_REQUESTS` ("Too many setup-intent requests. Please try again in 15 minutes."). Add-card taps count too. [VERIFIED: rateLimiter.js]
**How to avoid:** show the server message (the default "other code" path does). Do not auto-retry. Reuse a minted SetupIntent within one attempt only. Document in the device-test checklist (budget: ~10 per 15 minutes).

### Pitfall 6: PaymentOption callback semantics with saved cards
**What goes wrong:** with a customer config, FlowController preselects a saved/default card, so after the user dismisses the sheet with Back, `paymentOption` is still non-null. iOS checks only `paymentOption != nil`, so iOS proceeds to authorize the preselected card after a dismissal. Android's callback gives `PaymentOptionResult(paymentOption, didCancel)`. [CITED: DefaultFlowController.kt]
**How to avoid:** proceed to `confirm()` only when `!didCancel && paymentOption != null`; treat `didCancel` as the silent cancel D-12 describes. This is a deliberate deviation from iOS behavior (safer: dismiss never authorizes a charge). Record as A1 for user confirmation. The callback is not invoked after `configure` finishes, so a stray auto-confirm on configuration is not expected, but still gate on an explicit "presented" flag in the host.

### Pitfall 7: Test card choice for 3DS
**What goes wrong:** CONTEXT lists `4000 0025 0000 3155` for the 3DS check. Stripe's description: it requires authentication for off-session payments *unless set up*; after setup, off-session payments no longer need authentication. [CITED: docs.stripe.com/testing]. Our flow sets the card up through the SetupIntent, then the server confirms off-session, so 3155 will likely succeed without a 402 and the 3DS path never triggers. (The server also clones the PM onto the connected account, which may change this; unverified, A2.)
**How to avoid:** use `4000 0027 6000 3184` ("always authenticate") for the PAY-05 check; the spike research reached the same conclusion. Keep 3155 as a second try.

### Pitfall 8: Recreation re-launches Stripe UIs
**What goes wrong:** state-based requests (`LaunchedEffect(request)`) re-run after rotation and start a second sheet or second 3DS.
**How to avoid:** consume-on-launch (Pattern 1/3). The result callbacks must go to the current ViewModel (same instance across rotation) and must not read state captured in the old composition (for example, the account id). Stripe's FlowController keeps its configured state in a ViewModel, so confirm after a recreation should work. [ASSUMED, A4]. Device-check "rotate while the sheet is open".

### Pitfall 9: ON_RESUME is not "returned from Payment Methods"
**What goes wrong:** UI-SPEC reloads cards on `LifecycleEventEffect(ON_RESUME)`. ON_RESUME also fires on first composition (a duplicate of the init load) and after every Stripe activity returns (mid-flow).
**How to avoid:** the VM method `refreshPaymentMethods()` only touches the card list and must be idempotent (cancel/skip when a load is in flight) and never touch `paidStep`. Whether the Nav3 entry below a pushed destination gets ON_RESUME on return should be confirmed with a Robolectric/Nav test or the device. [ASSUMED, A10]

### Pitfall 10: Second 3DS round loops
**What goes wrong:** retrying confirm after a successful challenge can return another 402. Handling it with the same code starts the challenge again, forever.
**How to avoid:** allow one challenge per confirm attempt (`scaAttempted` flag in the paid context). A second 402 falls to "server message" with reset.

### Pitfall 11: The ambiguous confirm outcome and the Phase 6 recovery machinery
**What goes wrong:** (a) D-16 keeps the same `bookingAttemptId` after a confirm timeout. If the server did create the booking, the next Book Now calls `payment-intent` with the same id and gets `409 SLOT_UNAVAILABLE` (the DB conflict check sees the user's own `pending_approval` booking), so the user is told the slot was "just taken" and the slot is cleared, which is misleading. (b) The Phase 6 free path persists a recovery id under `SavedStateHandle` key `"booking_attempt_id"` and writes the result to `CompletedBookingStore` (WR-01). That key name is unrelated to the paid `bookingAttemptId` (D-17, memory only) but collides conceptually.
**How to avoid:** (a) keep D-16's locked copy, but log the "check My Bookings" gap as an Open Question; the payment reference is already shown. (b) Reuse the recovery mechanism for the paid confirm: generate the recovery id (`KEY_ATTEMPT_ID`) immediately before each `/bookings/confirm` call (both short-lead and long-lead, including the post-3DS retry) and `completedBookings.put(...)` on 201, exactly like the free path, so a success that lands while the app is backgrounded is not lost. Name the paid idempotency field `bookingAttemptId` and the recovery constant differently in code and comments.

### Pitfall 11b: `isSubmitting` must cover the whole paid flow
`syncClock()`, selection setters and system Back all key off `isSubmitting`. Set it while `paidStep != Idle` (and across the 3DS wait) so a `ON_RESUME` after returning from Stripe's activity cannot clear the slot or move the clock mid-flow. After a silent cancel it returns to false.

### Pitfall 12: Locale-sensitive UTC strings
`payment-intent` needs `selected_date` (`yyyy-MM-dd`) and `time_slot` (`HH:mm`) **in UTC** derived from the slot instant (server builds the slot keys as UTC). Use `DateTimeFormatter.ISO_LOCAL_DATE` / `ofPattern("HH:mm", Locale.ROOT)` with `ZoneOffset.UTC`; a default-locale formatter can emit non-ASCII digits and the server regex `^\d{2}:\d{2}$` rejects them (iOS used `en_US_POSIX`). `start_time` for confirm is `slot.start.toString()` (ISO-8601 with `Z`), the same as the free path.

### Pitfall 13: Retrofit empty bodies
`POST /me/payment-methods/setup-intent` and `PATCH .../default` validate against `z.object({}).strict()`. Send an explicit `{}` (as iOS does with `[:]`) via `@Body body: Map<String, String> = emptyMap()` rather than relying on Retrofit's empty request body. `DELETE` has no body. [VERIFIED: paymentMethods.schema.js]

### Pitfall 14: Process death during Stripe UI
The paid step and `pm_` are memory only (D-17). If Android kills the process while the user is in a 3DS bank app, the form returns idle with a new attempt id; the PI stays `requires_action` server-side (it is held until its TTL; Redis slot hold is 15 min). This is accepted by D-17; list it in the manual-checks log, not as a bug.

### Pitfall 15: Stripe key and account facts for device testing
The dev publishable key (`pk_test_51Tha0r…`) belongs to a different account than the backend's (`51U8OTJ…`, same as `pk_live_51U8OTJ…`), which is exactly the Phase 2 failure ("client secret does not match any associated SetupIntent"). [VERIFIED: app/build.gradle.kts and 02-UAT.md]. The account id appears as the digits after `pk_test_`/`pk_live_`, so the user can eyeball that the new key starts `pk_test_51U8OTJENKN7x…`. [ASSUMED, A7]. The existing dev-flavor comment says the backend is in test mode; D-02 says the user does not know. Treat D-02's checkpoint as authoritative and update that comment when resolved.

### Pitfall 16: The Phase 2 spike is not proof
D-12 calls the 3DS approach "spike-validated", but 02-UAT tests 7 and 8 both failed (key mismatch), and 02-09 was never executed. The SDK calls are compile-verified only. Plans must not treat any Stripe call as device-proven.

## Code Examples

### Application init (once, before any Stripe UI)
```kotlin
// Source: spike MainActivity.kt; PaymentConfiguration.init(context, publishableKey)
override fun onCreate() {
    super.onCreate()
    PaymentConfiguration.init(this, AppConfig.stripePublishableKey)
    // …existing realtime start…
}
```

### DTOs and API (shapes verified against backend and iOS)
```kotlin
// POST /bookings/payment-intent  (strict; promo_code omitted)
@JsonClass(generateAdapter = true)
data class PaymentIntentRequest(
    @Json(name = "expert_id") val expertId: String,
    @Json(name = "topic_id") val topicId: String,
    @Json(name = "selected_date") val selectedDate: String,       // UTC yyyy-MM-dd
    @Json(name = "time_slot") val timeSlot: String,               // UTC HH:mm
    @Json(name = "duration_minutes") val durationMinutes: Int,    // multiple of 30
    @Json(name = "booking_attempt_id") val bookingAttemptId: String,
)

// BARE 200 body (no {status,data} envelope). total_amount/summary are decimal major units: do NOT declare them.
@JsonClass(generateAdapter = true)
data class PaymentIntentResponse(
    @Json(name = "payment_intent_id") val paymentIntentId: String,
    @Json(name = "client_secret") val clientSecret: String,       // booking PI secret: never used client-side (Path A)
    val currency: String? = null,
)

// POST /bookings/confirm  (strict; exactly these keys; NO booking_attempt_id)
@JsonClass(generateAdapter = true)
data class ConfirmBookingRequest(
    @Json(name = "payment_intent_id") val paymentIntentId: String? = null,   // null ⇒ omitted ⇒ long-lead branch
    @Json(name = "expert_id") val expertId: String,
    @Json(name = "topic_id") val topicId: String,
    @Json(name = "start_time") val startTime: String,
    @Json(name = "meeting_type") val meetingType: String,
    @Json(name = "stripe_payment_method_id") val stripePaymentMethodId: String,
    @Json(name = "client_notes") val clientNotes: String? = null,
)
// 201 → SuccessDataResponse<ConfirmBookingData(bookingId, status?, message?, estimatedConfirmationTime?)>

// /me/payment-methods
//   GET    → SuccessDataResponse<PaymentMethodsData(paymentMethods: List<PaymentMethodItem>)>
//            PaymentMethodItem(id, brand, last4, @Json("exp_month") expMonth, @Json("exp_year") expYear, @Json("is_default") isDefault)
//   POST   setup-intent {} → SuccessDataResponse<PaymentMethodSetupData(clientSecret, customerId, ephemeralKey)>
//   DELETE /{id}           → SuccessMessageResponse
//   PATCH  /{id}/default {} → SuccessMessageResponse
```
New `ApiRequestUrls` constants: `PAYMENT_INTENT = "bookings/payment-intent"`, `BOOKING_CONFIRM = "bookings/confirm"`, `PAYMENT_METHODS = "me/payment-methods"`, `PAYMENT_METHODS_SETUP_INTENT`, `PAYMENT_METHOD = "me/payment-methods/{id}"`, `PAYMENT_METHOD_DEFAULT`. All are authenticated (bearer allowlist needs no change). `last4`, `exp_*` and the setup-intent fields should be tolerant of nulls where the backend maps `pm.card && pm.card.last4`.

### Add-card sheet (PAY-02), reusing the spike
```kotlin
// Source: spike MainActivity.kt (PaymentSheet.Builder(callback).build() composable + presentWithSetupIntent)
val sheet = PaymentSheet.Builder { result -> vm.onAddCardResult(result) }.build()
LaunchedEffect(state.pendingSetup) {
    val s = state.pendingSetup ?: return@LaunchedEffect
    vm.onSetupLaunched()   // consume-on-launch
    sheet.presentWithSetupIntent(
        s.clientSecret,
        PaymentSheet.Configuration.Builder("ClickMe")
            .customer(PaymentSheet.CustomerConfiguration(s.customerId, s.ephemeralKey))
            .allowsDelayedPaymentMethods(false)
            .appearance(appearance)
            .primaryButtonLabel(stringResource(R.string.payment_methods_sheet_save))  // PaymentSheet only; UI-SPEC A3
            .build(),
    )
}
```
`onAddCardResult`: `Completed` ⇒ silent list reload; `Canceled` ⇒ nothing; `Failed` ⇒ "Couldn't add card" with `error.message`.

### UTC strings for payment-intent
```kotlin
private val UtcDate = DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneOffset.UTC)
private val UtcTime = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT).withZone(ZoneOffset.UTC)
fun Instant.utcDate(): String = UtcDate.format(this)
fun Instant.utcTime(): String = UtcTime.format(this)
```

### CTA derivation (iOS `canBook` / `requiresPaymentMethodSetup`)
```kotlin
enum class PaymentMethodsAvailability { Unknown, None, Available }   // load failure ⇒ None (iOS)
// paid topic:  Unknown → disabled ("Checking your payment methods"); None → "Add a Payment Method", enabled with only a topic;
//              Available → needs a selectable slot, label "Book Now"
// free topic:  unchanged from Phase 6
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `rememberPaymentSheet` / `PaymentSheet.Builder` + `rememberPaymentSheetFlowController`, `PaymentLauncher.createForCompose`, `rememberLauncher` | `PaymentSheet.Builder(cb).build()` and `PaymentSheet.FlowController.Builder(...).build()` (composable), `rememberPaymentLauncher(pk, stripeAccountId, cb)` | Deprecated in 23.x [CITED: PaymentLauncher.kt, 02-RESEARCH.md] | Use the new builders; the spike already does |
| Capture at expert accept | Authorize at confirm, capture ~15 min after session end (Phase 27) | 2026-08-11 | Copy: "hold now, charged after your session" (lifecycle card) |
| Required `payment_intent_id` on confirm | Optional; absent ⇒ long-lead save-then-charge-at-T-24h branch (Phase 26/27.1) | 2026-08 | PAY-04 path |
| `ApiConfigurationPreview` opt-in | Moved to `stripe-core` in 23.24.0 | 2026-10-19 | Not used here |

**Deprecated/outdated:**
- UI-SPEC's `PaymentSheet.UserInterfaceStyle.AlwaysDark` (does not exist on Android PaymentSheet); see Pitfall 4.
- iOS comment "TODO(backend): stripe_account" in `MakeABookingViewModel`: the backend ships `detail.stripe_account` today. [VERIFIED: booking.controller.js lines ~767-774]

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | Proceed to `confirm()` only when `!didCancel && paymentOption != null`, deviating from iOS (which proceeds whenever a preselected option exists) | Pitfall 6 | If the user expects iOS behavior, a dismissed sheet silently cancels instead of using the preselected card. Low harm; needs user confirmation |
| A2 | `4000 0025 0000 3155` will not produce the 402 in this flow (server off-session confirm after a setup); `…3184` will. The server's clone onto the connected account could change 3155's outcome | Pitfall 7 | Test plan could pick a card that never triggers 3DS. Mitigated by listing both |
| A3 | `PaymentResult.Completed` is returned by `handleNextActionForPaymentIntent` when the manual-capture PI lands on `requires_capture` | Pattern 3 | If it returns something else, the retry never fires. Device check |
| A4 | FlowController configured state survives Activity recreation (held in a ViewModel by Stripe) | Pitfall 8 | `confirm()` after rotation could fail with "not configured". Device check; fall back to reconfigure-then-confirm |
| A5 | `PaymentSheet.PrimaryButton` / `PrimaryButtonColors` parameter names (`colorsLight`/`colorsDark`, `background`/`onBackground`/`border`) | Pitfall 4 | Compile error only; fix in IDE |
| A6 | R8 may need `-dontwarn com.stripe.android.pushProvisioning.**` | Pitfall 2 | Release build fails until the rule is added |
| A7 | A publishable key embeds the account id after `pk_test_`/`pk_live_` | Pitfall 15 | User cannot eyeball the match; still verifiable in the dashboard |
| A8 | `SetupIntent.paymentMethodId` is populated once the setup-mode confirm completes (iOS depends on the same field) | Pattern 4 | `pm_` read returns null; fall back to the generic "Couldn't read…" message |
| A9 | `retrieveSetupIntentSynchronous` with only the publishable key works for a platform-account SetupIntent (no `stripeAccountId`) | Pattern 4 | Wrong account scoping; confirm on device |
| A10 | The Make a Booking nav entry receives ON_RESUME when Payment Methods is popped (Nav3 lifecycle decorator) | Pitfall 9 | Card list would not refresh on return; fall back to a nav-result or `onCleared`-style signal |
| A11 | `com.stripe:paymentsheet` coordinates are legitimate (slopcheck did not run) | Package Legitimacy | Negligible: first-party, already building |

## Open Questions

1. **Backend Stripe mode and the right test key (D-01, D-02).**
   - What we know: dev key and backend account differ; backend secret key is for the `51U8OTJ…` account.
   - What's unclear: test vs live mode; the correct `pk_test_…`.
   - Recommendation: early non-blocking `checkpoint:human-action`; code and unit tests proceed regardless (D-03).

2. **Long-lead Request Sent copy.** The lifecycle card says "A hold is placed on your card" and step 1 done, which is wrong for a > 6 day booking (card saved, charged ~T-24h). iOS has the same gap. UI-SPEC Open Item 3 says port iOS as is. Recommendation: port, log it for the iOS side.

3. **Misleading `SLOT_UNAVAILABLE` after an ambiguous confirm timeout (Pitfall 11a).** Locked copy (D-16) does not tell the user to check My Bookings. Recommendation: implement as locked; add a note to the manual-check list; consider adding "check My Bookings" copy later.

4. **Paid-path recovery record.** Recommendation in Pitfall 11b (reuse `CompletedBookingStore`). Planner decides whether this is in scope for Phase 7 or a documented gap.

5. **Active-call guard on Add Payment Method** is Phase 9's (UI-SPEC Open Item 4). No action.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| JDK 25 (Android Studio JBR) | Gradle/AGP builds | ✓ | OpenJDK 25.0.3 at `/Applications/Android Studio.app/Contents/jbr/Contents/Home` | — (system `java` is 18; always set `JAVA_HOME`) |
| Android SDK / emulator `Medium_Phone_API_37.0` | Builds, Robolectric | ✓ | compileSdk 37 | — |
| Samsung SM-A075F (Android 16) | Device checks | ✓ (per memory) | — | Deferred per D-03 |
| Correct dev `pk_test_…` | All device checks | ✗ | — | Code and unit tests proceed; device checks logged as open items (D-01/D-03) |
| Backend in a test-mode Stripe account | Device checks with real flows | Unknown | — | Hold device tests until D-02 resolves; never use real cards |
| iOS app signed in as the expert (`charges_enabled` in the same Stripe mode) | Short-lead acceptance check | Unknown | — | Deferred (D-03) |
| `ctx7` CLI | Docs lookup | ✗ | — | Used official docs and stripe-android source via WebFetch |
| `pip` / `slopcheck` | Package audit | ✗ | — | Marked `[ASSUMED]` (no new coordinates) |

**Missing dependencies with no fallback:** none for coding.
**Missing dependencies with fallback:** the test key and test-mode backend block device checks only.

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 4.13.2 + Robolectric + `kotlinx-coroutines-test` 1.11.0 + Compose `ui-test-junit4` + `okhttp-mockwebserver3` |
| Config file | none (`app/build.gradle.kts` `testOptions.unitTests.isIncludeAndroidResources = true`); fakes in `app/src/test/.../testing/` (`FakeClickMeApi`, `MainDispatcherRule`, `MutableClock`, `FakeCompletedBookingStore`) |
| Quick run command | `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:testDevDebugUnitTest --tests '<pattern>'` |
| Full suite command | `./gradlew ktlintCheck lintDevDebug lintProdDebug testDevDebugUnitTest testProdDebugUnitTest assembleDevDebug assembleProdDebug assembleProdRelease && bash scripts/check-design-rules.sh` |

### Phase Requirements → Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| PAY-01 | List loads; empty and failed states; set default and delete one at a time; spinner row; reload not optimistic; server default shown after delete; failure alerts with the three titles | unit (VM) + Compose (Robolectric) | `--tests '*PaymentMethodsViewModelTest' --tests '*PaymentMethodsScreenTest'` | ❌ Wave 0 |
| PAY-01 | Menu hides "Set as default" on the default card; Remove opens confirm dialog; other rows' menus disabled while busy | Compose | `--tests '*PaymentMethodRowTest'` | ❌ Wave 0 |
| PAY-02 | Add: setup-intent then sheet request; `Completed` reloads silently; `Canceled` no-op; `Failed` alert; double tap ignored while minting | unit (VM, fake Stripe results) | `--tests '*PaymentMethodsViewModelTest'` | ❌ Wave 0 |
| PAY-02/06 | New endpoints: paths, methods, `{}` bodies, bearer present, DTO decoding (`exp_month`, `is_default`, bare payment-intent with `total_amount: 42.00`) | MockWebServer + decoding | `--tests '*ClickMeApiTest' --tests '*ResponseDecodingTest' --tests '*ApiRequestUrlsTest'` | extend existing |
| PAY-03 | Short-lead order: payment-intent (body exact, UTC strings) → setup-intent → request published → pm read → confirm (body exact, six keys, no `booking_attempt_id`); attempt id regenerated on topic/slot change, reused after failure; single flight; silent cancel keeps form and slot | unit (VM) | `--tests '*MakeABookingViewModelPaidTest'` | ❌ Wave 0 |
| PAY-03 | `pm_` unreadable ⇒ "Couldn't read…" and reset | unit | same | ❌ Wave 0 |
| PAY-04 | 422 LONG_LEAD ⇒ confirm without `payment_intent_id`, default card else first; no cards ⇒ alert; gate: paid topic and no card/load failure ⇒ "Add a Payment Method", enabled with only a topic; returns to "Book Now" after reload | unit (VM + UiState) | `--tests '*MakeABookingViewModelPaidTest' --tests '*MakeABookingUiStateTest'` | ❌ Wave 0 / extend |
| PAY-05 | 402 with usable detail ⇒ challenge (memory only, consumed once) ⇒ Completed ⇒ confirm retry with same `pm_`/PI; cancel and failure copy; empty `stripe_account` hard failure; second 402 not looped; attempt id kept | unit (VM) | `--tests '*MakeABookingViewModelScaTest'` | ❌ Wave 0 |
| PAY-06 | Table test over all 15 codes plus unknown plus `ACCOUNT_SUSPENDED` plus network-in-confirm: message, new attempt id, slot clear/reload, meeting reset; payment reference only in confirm phase and when a `pi_…` exists | unit (pure) | `--tests '*PaidFlowErrorTest'` | ❌ Wave 0 |
| PAY-07 | `Money.format` for JPY (no decimals), USD, unknown code; Summary price for paid topics; lifecycle card `sessionWhen` formatting; Request Sent shows the card only when `!isFree` | unit + Compose | `--tests '*MoneyTest' --tests '*PaymentLifecycleCardTest' --tests '*BookingRequestSentScreenTest'` | MoneyTest ✅; others ❌ Wave 0 / extend |
| UI | CTA label switch and `stateDescription` table; info link only for paid topic; info sheet opens/closes | Compose | `--tests '*MakeABookingBookButtonTest' --tests '*PaymentInfoSheetTest'` | ❌ Wave 0 / extend `MakeABookingScreenTest` |
| Nav | `PaymentMethodsKey` registered in both stacks with bottom bar hidden; Profile row navigates | Compose/nav | `--tests '*HomeShellsTest'` (extend) | extend |
| Stripe wiring | Sheet, FlowController, 3DS, rotation, dark appearance, JPY topic, 200% font | **manual device** (needs D-01/D-02) | — | logged as open items (D-03) |

### Sampling Rate
- **Per task commit:** the targeted `--tests` pattern for the task (30-60 s)
- **Per wave merge:** `./gradlew testDevDebugUnitTest ktlintCheck && bash scripts/check-design-rules.sh`
- **Phase gate:** full suite green on both flavors including `assembleProdRelease` (R8 with Stripe) before `/gsd-verify-work`

### Wave 0 Gaps
- [ ] `FakeClickMeApi`: handlers and call counters for `paymentIntent`, `confirmBooking`, `getPaymentMethods`, `createSetupIntent`, `deletePaymentMethod`, `setDefaultPaymentMethod` (recording request bodies)
- [ ] `FakeStripePaymentMethodReader` (returns a scripted `pm_` or null) in `testing/`
- [ ] `BookingTestFixtures`: paid topic helpers, saved-card fixtures, `newViewModel` gaining the payment repository and reader parameters (all existing callers updated)
- [ ] Test files listed above (`PaymentMethodsViewModelTest`, `PaymentMethodsScreenTest`, `MakeABookingViewModelPaidTest`, `MakeABookingViewModelScaTest`, `PaidFlowErrorTest`, `PaymentLifecycleCardTest`)
- [ ] No new framework installs

## Security Domain

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | no (existing session; bearer via interceptor) | — |
| V3 Session Management | no change | existing `TokenAuthenticator` |
| V4 Access Control | yes (server-enforced) | Server returns 404 for another user's `pm_`, 422 `INVALID_PAYMENT_METHOD` for cross-customer; app sends only ids it received |
| V5 Input Validation | yes | Typed Moshi DTOs; UUID `booking_attempt_id` via `UUID.randomUUID()`; server strict schemas reject extra keys, so bodies must match exactly; treat server text as plain text (`FieldError.Text`) |
| V6 Cryptography | no (no app crypto) | Stripe SDK handles card data; never hand-roll |
| V8 Data Protection | yes | Never log or persist `client_secret`, `ephemeral_key`, `stripe_account`, `pm_`/`pi_` (except the user-visible payment reference); keep paid state out of `SavedStateHandle`; keep the debug logger at `BASIC` with `Authorization` redacted; data classes holding secrets must not be dumped by `toString()` into logs |
| V9 Communications | yes (inherent) | HTTPS only base URL; Stripe SDK TLS |

### Known Threat Patterns for this stack

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Client-side amount tampering | Tampering | App sends no amounts; server derives price, fee, currency from the topic |
| Double charge / duplicate booking on retry | Tampering | `booking_attempt_id` reuse on retry, new only on a genuinely new attempt; single-flight guard (D-11); `DUPLICATE_BOOKING` mapping |
| Leaking `stripe_account` or secrets | Information disclosure | Hold in memory only, consume once, no logging, not in saved state or analytics |
| Falling back to the platform account when `stripe_account` is missing | Elevation / integrity | Hard failure with alert (D-15); never construct the launcher without an account for the 3DS step |
| Confirming the booking PI client-side | Tampering | Path A: never; only `handleNextAction` on 402 |
| IDOR on delete or default | Elevation | Server 404; app only sends ids from its own list |
| Spoofed or phished payment UI | Spoofing | Only Stripe's SDK UIs collect card data; no custom WebView |
| Rate-limit lockout (setup-intent 429) | Denial of service (self-inflicted) | Show the server message; no auto-retry loops |

## Sources

### Primary (HIGH confidence)
- `backend-reference/docs/ios/STRIPE-INTEGRATION.md`: Path A, bodies, error table, §2.4 3DS, §4 saved cards
- `backend-reference/src/controllers/booking.controller.js`, `src/schemas/booking.schema.js`, `src/controllers/paymentMethods.controller.js`, `src/routes/paymentMethods.js`, `src/schemas/paymentMethods.schema.js`, `src/middleware/rateLimiter.js`, `src/services/redis.js`: exact strict schemas, long-lead guard, 402 detail, setup-intent rate limit, slot-hold re-acquisition by the same holder
- iOS: `MakeABookingViewModel.swift`, `MakeABookingStripePaymentSheet.swift`, `MakeABookingView.swift`, `PaymentMethods*`, `ClickMeAPI+PaymentMethods.swift`, `ClickMeAPI+Booking.swift`, `PaymentIntentResponse.swift`
- Android codebase: `MakeABookingViewModel.kt`, `MakeABookingUiState.kt`, `BookingErrors.kt`, `BookingRepository.kt`, `ErrorCode.kt`, `ApiException.kt`, `Money.kt`, `HomeClientScreen.kt`, `ClientBookingFlowEntries.kt`, `Navigator.kt`, `BookingRequestSent*`, `app/build.gradle.kts`, `scripts/check-design-rules.sh`, `spikes/stripe/**`
- stripe-android source (master): `PaymentSheet.kt` (FlowController API, Builder constructors, `CustomerConfiguration`), `Stripe.kt` (`retrieveSetupIntent*`), `PaymentLauncher.kt` (`rememberPaymentLauncher`), `SetupIntent.kt` (`paymentMethodId`), `DefaultFlowController.kt` (option callback semantics), `paymentsheet.api`
- docs.stripe.com: `accept-a-payment-deferred?platform=android` (FlowController Compose), `elements/appearance-api?platform=android` (colorsDark = colorsLight to disable dark mode), `testing` (3DS cards)
- Maven Central `maven-metadata.xml` for `com.stripe:paymentsheet` and `com.stripe:stripe-android` (23.25.0 latest, 2026-10-22)
- github.com/stripe/stripe-android/releases (23.20 to 23.25)

### Secondary (MEDIUM confidence)
- `.planning/phases/02-networking-session-sdk-spikes/02-RESEARCH.md`, `02-UAT.md`: spike findings and the key-mismatch failure
- flutter-stripe issue reports on R8 and `pushProvisioning` (secondary; no official fix found)

### Tertiary (LOW confidence)
- Behavior claims tagged `[ASSUMED]` in the Assumptions Log (A1 to A11), all to be resolved on a device

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH. One dependency line; versions verified on Maven Central; everything else exists.
- Architecture: MEDIUM-HIGH. State machine and error table are direct ports of iOS and the backend; the Compose host with FlowController is unproven on a device.
- Pitfalls: MEDIUM-HIGH. Backend and CI-rule pitfalls are verified in code; SDK runtime behavior (rotation, `PaymentResult` for `requires_capture`, `retrieveSetupIntent` timing) is assumed until a device run.

**Research date:** 2026-10-22
**Valid until:** 2026-11-05 (Stripe Android ships weekly; 23.25.0 already out; backend contract stable)
