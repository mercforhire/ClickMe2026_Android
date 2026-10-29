# Phase 7: Stripe questions for the backend agent

**Written:** 2026-10-28 · **Status:** sent? (update when sent and answered) · **STATE.md entry:** "[Phase 7] Stripe key and mode"

## Findings (2026-10-28)

How iOS tested Stripe, and why Android can't:

- **Before 2026-10-17, prod ran in test mode.** Until the backend live cutover (backend quick task `261017-tm2`, commit `e85593f`), `clickme-backend-production` ran on the Stripe test-mode platform account `acct_1Tha…` ("Clickme 2026 app"). That is the account `pk_test_51Tha0…` belongs to, so the iOS Development/Staging builds matched the backend. All backend live-UAT ran then too, on the same Railway service and Supabase project, with test cards and test-mode Connect accounts (backend `.planning/LIVE-VERIFICATION.md`, `docs/UAT-ACCOUNTS.md`).
- **The cutover.** On 2026-10-17 prod moved to `sk_live` on a separate account, `acct_1U8O…`. `live-cutover-cleanup.js` cleared 7 `payout_accounts` and 5 customer IDs, deleted the clone cache, and cancelled 10 `requires_capture` test bookings. The cutover notes also record an open item: zero bookable experts in live.
- **Timing.** iOS TestFlight prep was on 2026-10-15, before the cutover. The Android Phase 2 Stripe spike ran on 2026-10-21, after it, hence 02-UAT tests 7–8 failing. iOS Development builds would fail the same way today.
- **3DS was never tested anywhere.** Backend Phase 18 (18-04-SUMMARY) and Phase 26 (26-06-SUMMARY) record that the off-session `authentication_required` → `402 PAYMENT_REQUIRES_ACTION` path is unit-covered but not reproducible in Stripe test mode. iOS's 3DS recovery has never run end to end. 07-REVIEW CR-01 still holds against backend HEAD `2a7cf19`: `stripe.js:249-257` and `booking.controller.js:765-776` are unchanged.
- **A new gap.** `POST /bookings/:id/recover-payment` (backend Phase 26 Plan 05) is the client-present SCA recovery for long-lead sweep failures. It has a 6-hour window, per `docs/ios/RESCHEDULE-PAYMENT-CONVERSION.md`. Neither the iOS nor the Android app calls it, and no requirement covers it.

User constraint (2026-10-28): no new Supabase project and no new Railway environment.

### Options

1. **Local test-mode backend (preferred if the backend agent says it's safe).**
   - Run `npm start` locally with the sandbox `sk_test_`, against the existing Supabase.
   - Connect the phone with `adb reverse tcp:3000 tcp:3000`.
   - Forward webhooks with `stripe listen --forward-to …`.
   - Android work: a configurable dev `BASE_URL` (local.properties) plus a debug-only cleartext network-security config. The key `pk_test_51Tha0…` stays.
   - Risk: test-mode IDs and bookings in the prod DB, and cron jobs on both sides acting on the wrong mode's rows.
2. **Live real-card smoke test at release.** Manual capture means a pre-session cancel only voids the hold. It needs a live bookable expert with real Connect KYC. It can't use test cards, declines or forced 3DS.
3. **Defer.** Rely on iOS's pre-cutover verification of the shared contract plus the Android unit tests, and do option 2 in Phase 16. 3DS stays unverified on both platforms.

## Message (paste as is; no secrets)

> **Context:** ClickMe Android port. Until the 2026-10-17 live cutover (`261017-tm2`), clients tested Stripe against prod in test mode (`acct_1Tha…`, `pk_test_51Tha0…`). Since then no client can run a Stripe flow without real cards, and the Android port hasn't been able to test payments on a device at all. We don't want a new Supabase project or Railway environment.
>
> **1. Local test-mode backend.** Can we safely run the backend locally (`npm start`) with the sandbox `sk_test_`, against the existing prod Supabase, for Android device testing?
> - Can the cron jobs be disabled locally (expiry, refund reconciliation, reminders, Sweep A, long-lead charge sweep)?
> - Will the live prod backend's jobs ignore test-mode bookings and PaymentIntents, or try to act on them with `sk_live`?
> - Which accounts should we use? The `uat.*@clickme.test` roster, with a test-mode Connect account re-created via `create-hero-connect.js`, since the cutover cleared `payout_accounts`?
> - How should rows created during testing be cleaned up afterwards (the `live-cutover-cleanup.js` approach)?
> - Which webhook secrets do we need for `stripe listen` forwarding?
> - Are any other env vars needed locally?
>
> If this isn't safe, say so, and we'll do a live real-card smoke test at release instead. That needs at least one live bookable expert. What's the plan for onboarding one?
>
> **2. 3DS recovery on a saved card (`402 PAYMENT_REQUIRES_ACTION`).** The off-session confirm (`stripe.js:249`, `error_on_requires_action: true`) leaves the PaymentIntent in `requires_payment_method` with no `next_action`. So the clients' `handleNextAction(client_secret)` has nothing to run, and the retried `/bookings/confirm` replays the cached 402 (idempotency key `confirm-off-{pi}`). Your Phase 18 and Phase 26 notes say this path was never reproduced, so iOS never exercised it either. Do you agree it's broken? Which fix do you prefer?
> - **(a)** Return the cloned `payment_method` id in `detail`. The client confirms on-session with `stripeAccount`, then retries confirm, and the backend skips the off-session confirm when the PaymentIntent is already `requires_capture`.
> - **(b)** The backend confirms on-session so the PaymentIntent reaches `requires_action` with a real `next_action`.
>
> **3. Retry after a decline.** The same `booking_attempt_id` returns the same PaymentIntent and the spent `confirm-off-{pi}` key, so retrying with a different card replays the old failure. Should the key include the payment method, or should clients rotate `booking_attempt_id` after a failure?
>
> **4. `POST /bookings/:id/recover-payment`.** `RESCHEDULE-PAYMENT-CONVERSION.md` says SCA failures in the long-lead sweep get a 6-hour recovery window through this endpoint. Neither the iOS nor the Android app calls it. Is it required for launch? What should trigger it in the app: a push, a booking status, a chat message?

## Answers

_(paste the backend agent's reply here)_

## Android follow-ups (once answered)

- **If option 1 is cleared:** make the dev `BASE_URL` configurable and add the debug cleartext config. Optionally add the 07-REVIEW WR-08 publishable-key guard. Then run 02-UAT tests 7–8 (closes FND-13), the Phase 7 SC-1..SC-5 checks, and the paid parts of Phases 8, 9 and 11.
- **If 3DS fix (a) or (b) is chosen:** update `PaidFlowStripeHost.kt` and `MakeABookingViewModel.kt` to match, and log the same change as an iOS follow-up.
- **If `recover-payment` is required for launch:** add a requirement and plan it (probably Phase 14 or 15), on both platforms.
