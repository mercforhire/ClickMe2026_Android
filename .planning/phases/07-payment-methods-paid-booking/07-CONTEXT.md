# Phase 7: Payment Methods & Paid Booking - Context

**Gathered:** 2026-10-22
**Status:** Ready for planning

<domain>
## Phase Boundary

Two deliverables:

1. **Payment Methods screen** (PAY-01, PAY-02), ported from iOS `Settings/Payment Methods`:
   - List saved cards (`GET /me/payment-methods`).
   - Add a card through `POST /me/payment-methods/setup-intent` and Stripe PaymentSheet in setup mode.
   - Set a card as default (`/me/payment-methods/:id/default`) and delete one (`DELETE /me/payment-methods/:id`).
2. **The paid paths in Make a Booking** (PAY-03 to PAY-07). They replace Phase 6's `BookDisabledReason.PaidTopic` branch with the same layout:
   - **Short-lead.** `POST /bookings/payment-intent` (with `booking_attempt_id`), then the setup-intent, then PaymentSheet `FlowController` in setup mode against the SetupIntent, then read the `pm_…`, then `POST /bookings/confirm`.
   - **Long-lead.** `422 LONG_LEAD_REQUIRES_SAVED_CARD` from payment-intent leads to `/bookings/confirm` with no `payment_intent_id` and the preferred saved card.
   - **3DS.** `402 PAYMENT_REQUIRES_ACTION` leads to `PaymentLauncher` with `stripeAccountId = detail.stripe_account` (never saved), then a retry of confirm with the same `pm_…`.
   - The ~15 `handlePaidFlowError` codes.
   - Minor-unit price display, with JPY showing no decimals.
   - The "How does payment work?" sheet.
   - The paid payment-lifecycle card on Request Sent.

Out of scope:
- Promo codes (`promo_code` is always null, as on iOS).
- The client Profile hub (Phase 14).
- Payment history (DEF-4).
- Reschedule payment conversion (DEF-1 / Phase 8).
- Expert payouts (Phase 14).

</domain>

<decisions>
## Implementation Decisions

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

### Stripe test key & device testing
- **D-01: The user supplies the correct test key.** The user gets the publishable test key (`pk_test_…`) of the Stripe account behind the backend's secret key (the `51U8OTJ…` account, the same account as iOS prod's `pk_live`) from the Stripe dashboard. The dev flavor's `STRIPE_PUBLISHABLE_KEY` in `app/build.gradle.kts` is then swapped. This is the fix for the open Phase 2 UAT gap ("client secret does not match any associated SetupIntent on this account", 02-UAT tests 7–8).
- **D-02: Confirm the backend's Stripe mode first.** The user doesn't know whether the backend's Stripe account is in test or live mode. Phase 7 includes an early `checkpoint:human-action` (non-blocking):
  - Confirm the mode in the dashboard.
  - Provide the matching publishable key.
  - If the backend is **live**, device testing must not use real cards against it. Flag it and stop device tests until a test-mode path exists.
- **D-03: Code first, device test later.** Phase 7 is built and unit-tested without waiting on the key. Device checks (PaymentSheet add card, short-lead with the iOS expert receiving the request, long-lead, 3DS with `4000 0025 0000 3155`) are logged as open human-verification items, like Phases 3–6. No plan blocks on them.

### Payment Methods screen (PAY-01, PAY-02)
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

### Paid CTA & no-card gate
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

### Errors, 3DS & retry (PAY-05, PAY-06)
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

### Request Sent (paid)
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

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Backend contract (authoritative)
- `backend-reference/docs/ios/STRIPE-INTEGRATION.md`: Path A (setup-mode PaymentSheet plus server-side confirm), payment-intent and confirm bodies, the error codes, and the §2.4 saved-card 3DS flow with `detail.stripe_account`
- `backend-reference/docs/ios/RESCHEDULE-PAYMENT-CONVERSION.md`: long-lead (> 6 days) save-card-first and charge-at-T-24h branch, and `LONG_LEAD_REQUIRES_SAVED_CARD`
- `backend-reference/docs/ios/PRICING-LIMITS.md` §5: minor units, the `payment-intent` exception and JPY
- `backend-reference/src/controllers/booking.controller.js`: `createPaymentIntent` and `confirm` handlers (check the exact error codes and the `detail` shape)

### iOS paid booking (source of truth)
- `ios-reference/ClickMe2026/Views/Screens/Client Mode/Make a Booking/MakeABookingViewModel.swift`: `PaymentMethodsAvailability`, `requiresPaymentMethodSetup`, `preferredPaymentMethodId`, `beginPaidBooking`, `performLongLeadBooking`, `completePaidBooking`, `handlePaidFlowError` (D-13), `retryConfirmAfterSCA` and `cancelSCA`, `bookingAttemptId` regeneration, and `utcDateString` / `utcTimeSlotString` for the payment-intent body
- `ios-reference/ClickMe2026/Views/Screens/Client Mode/Make a Booking/Subviews/MakeABookingStripePaymentSheet.swift`: FlowController setup-mode config, the confirm, reading the `pm_…` from the SetupIntent, and 3DS with `stripeAccount`
- `ios-reference/ClickMe2026/Views/Screens/Client Mode/Make a Booking/MakeABookingView.swift`: the CTA label switch, the Payment Methods navigation, the PaymentInfoSheet presentation, and the reload on return
- `ios-reference/ClickMe2026/Views/Screens/Client Mode/Make a Booking/Subviews/PaymentInfoSheet.swift` and `MakeABookingBookButton.swift`
- `ios-reference/ClickMe2026/Views/Screens/Client Mode/Request Sent/Subviews/PaymentLifecycleCard.swift`, `BookingRequestSentView.swift` and `BookingRequestSentViewModel.swift`: the paid card and headline

### iOS Payment Methods (source of truth)
- `ios-reference/ClickMe2026/Views/Screens/Settings/Payment Methods/PaymentMethodsView.swift`, `PaymentMethodsViewModel.swift`
- `ios-reference/ClickMe2026/Views/Screens/Settings/Payment Methods/Subviews/PaymentMethodRow.swift`, `PaymentMethodsBrand.swift`, `PaymentMethodsSetupSheet.swift`
- `ios-reference/ClickMe2026/Network/ClickMeAPI+PaymentMethods.swift`, `ClickMeAPI+Booking.swift` (`createPaymentIntent`, `confirmBooking`), `APIRequestURLs.swift` (lines ~247–248 and ~341–344)
- `ios-reference/ClickMe2026/Model/Responses/PaymentMethodsData.swift`, `PaymentIntentResponse.swift`, `PaymentMethodSetupData.swift`, `ConfirmBookingData.swift`, `Children/PaymentMethodItem.swift`
- `ios-reference/ClickMe2026/Model/Enums/ErrorCode.swift`

### Android spike & prior phases
- `spikes/stripe/src/main/java/com/uptrendinvestments/clickmeapp/spike/stripe/` (`MainActivity.kt`, `StripeSpikeViewModel.kt`, `StripeSpikeApi.kt`): the working PaymentSheet setup-mode and PaymentLauncher-with-`stripeAccountId` code to reuse
- `.planning/phases/02-networking-session-sdk-spikes/02-CONTEXT.md` D-12 and `02-UAT.md` tests 7–8 and Gaps: the key mismatch (D-01)
- `.planning/phases/06-booking-flow-free-bookings/06-CONTEXT.md`: D-01, D-02 (replaced by D-10), D-08 to D-12 and D-14
- `.planning/STATE.md` "[Phase 7 hand-off]" note

### Project
- `.planning/REQUIREMENTS.md`: PAY-01 to PAY-07
- `.planning/ROADMAP.md`: Phase 7 success criteria and planning notes (reuse the spike findings; `payment-intent` is the minor-unit exception; never cut)
- `CLAUDE.md`: platform swaps and file organization

</canonical_refs>

<code_context>
## Existing Code Insights

Paths below are under `app/src/main/java/com/uptrendinvestments/clickmeapp/` unless noted.

### Reusable Assets
- `screens/clientmode/makeabooking/`:
  - `MakeABookingViewModel.kt`: `book()` has the `!topic.isFree` early return to replace.
  - `MakeABookingUiState.kt`: `BookDisabledReason.PaidTopic`.
  - `BookingErrors.kt`: the free-path error mapping to extend (D-13, D-14).
  - `BookingRepository.kt`: add payment-intent and confirm.
  - `CompletedBookingStore.kt`
  - `subviews/MakeABookingBookButton.kt`: the coming-soon note goes, and the CTA label switch arrives.
- `screens/clientmode/requestsent/`: `BookingRequestSentScreen.kt` and its ViewModel and UI state, where the paid card goes.
- `model/enums/ErrorCode.kt`: already has `PaymentRequiresAction`, `PaymentFailed`, `InvalidPaymentMethod`, `AmountBelowMinimum`, `PaidTopicRequiresPayment`, `PaymentMethodRequired` and `LONG_LEAD_REQUIRES_SAVED_CARD`. Check it for missing codes such as `BOOKING_ATTEMPT_CONFLICT`, `INTENT_EXPIRED`, `PI_*_MISMATCH` and `DIRECT_CHARGE_DISABLED`.
- `config/AppConfig.kt` and `BuildConfig.STRIPE_PUBLISHABLE_KEY` (set per flavor in `app/build.gradle.kts`). `gradle/libs.versions.toml` has `stripe = "23.24.0"` and `stripe-paymentsheet`.
- `util/Money.kt`: the price formatter, with JPY handled.
- `components/PlaceholderTabContent.kt`: the client Profile placeholder that gets the Payment Methods row (D-04). `components/EmptyState.kt` has `EmptyState` and `ErrorState`.

### Established Patterns
- Assisted-injection ViewModels with `SavedStateHandle` for form state (Phase 6). `bookingAttemptId` stays out of it (D-17).
- `navigation/Navigator.kt` has `navigate`, replace-top (Phase 6 D-12) and the cross-tab switch. The client shell's `entryProvider` is in `screens/clientmode/home/HomeClientScreen.kt`.
- Errors use the `AlertDialog` "Couldn't book" pattern, and the error body is parsed into a code, a message and a `detail` (`detail` is needed for the 402).

### Integration Points
- `HomeClientScreen.kt`: register the Payment Methods key, reached from both the Explore stack (booking gate) and the Profile tab.
- New package `screens/settings/paymentmethods/` (mirrors iOS `Views/Screens/Settings/Payment Methods`).

</code_context>

<specifics>
## Specific Ideas

- Device verification, once D-01 and D-02 are resolved, with the iOS app signed in as the expert:
  - Add `4242…` in Payment Methods, set it as default, delete it.
  - Short-lead paid booking: the iOS expert gets the request.
  - Retry after a forced failure: no duplicate booking.
  - Long-lead (> 6 days out) confirms with the saved card. With no card, the CTA routes to Payment Methods, and the client finishes after adding one.
  - The 3DS card `4000 0025 0000 3155` triggers the challenge, and the booking confirms.
  - A JPY topic price shows no decimals.

</specifics>

<deferred>
## Deferred Ideas

- Auto-continuing the booking after a card is added (D-09 chose re-tap).
- Gating only long-lead bookings on a saved card (D-08 kept the iOS always-gate rule).
- Showing the server `payment-intent` total before paying (D-12).
- Saving `bookingAttemptId` across process death (D-17 chose memory only).
- Promo codes.

</deferred>

---

*Phase: 07-payment-methods-paid-booking*
*Context gathered: 2026-10-22*
