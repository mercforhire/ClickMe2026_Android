# Phase 7: Stripe questions for the backend agent

**Written:** 2026-10-28 · **Status:** answered 2026-10-28 (backend HEAD `2a7cf19`); the backend fixes shipped in backend quick 261028-snh (deployed `d9df858`); Android follow-up quick 261028-to1 · **STATE.md entry:** "[Phase 7] Stripe key and mode"

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

## Answers (backend, 2026-10-28, HEAD `2a7cf19`)

### 1. Local test-mode backend against prod Supabase: not safe as is; workable with one small change

- **Cron jobs can't be disabled today.** `src/app.js:245-305` starts all 12 jobs on every boot. Worst case: a local `sk_test` server runs the long-lead T-24h charge job on a live booking. The job marks it attempted, the Stripe call fails on the live connected account, and the recovery-window job cancels the real booking 6 hours later. **Fix (backend):** a `DISABLE_CRONS=true` env flag that skips all job starts.
- **Prod's jobs won't ignore test rows.** No row records its Stripe mode. Prod jobs (on `sk_live`) pick up test bookings and fail with "No such payment_intent". No money moves, but job results can't be trusted.
  - Testable locally: payment-intent → confirm (short-lead, saved card, 3DS), SetupIntent and saved cards, Connect onboarding, expert accept/decline, cancels by either side.
  - **Not testable:** expiry void, completion capture, long-lead T-24h charge, `recover-payment`, auth-expiry capture, fee reconciliation.
- **Accounts:** only `uat.*@clickme.test`, never real ones. `profiles.stripe_customer_id` and `payout_accounts` hold one ID per user, so a test ID would overwrite a live one. The backend will check that `create-hero-connect.js` works with `sk_test`.
  - **Side effect:** while the uat expert has a test Connect account with charges enabled, prod shows that expert as bookable. Real users who try to book would get a payment error (no money moves). Keep testing windows short.
- **Cleanup:** **do NOT run `live-cutover-cleanup.js`.** It isn't limited to test rows and would wipe the live account (leonchn84) and every live customer. The backend will write a new cleanup script limited to the uat accounts.
- **Webhooks:** `stripe listen --forward-to localhost:3000/webhooks/stripe --forward-connect-to localhost:3000/webhooks/stripe-connect`. The paths are at the root, not under `/api/v1`. Set both `STRIPE_WEBHOOK_SECRET` and `STRIPE_CONNECT_WEBHOOK_SECRET` to the one `whsec_` it prints.
- **Local env:**
  - `STRIPE_SECRET_KEY`: a dashboard `sk_test_`, not the CLI's restricted key (it expires 2026-11-07).
  - The two webhook secrets above.
  - Prod `CONNECT_ONBOARD_RETURN_URL` and `CONNECT_ONBOARD_REFRESH_URL` are fine; they only 302 to `clickme://`.
  - `DISABLE_CRONS=true`.
  - A local `REDIS_URL` (`redis-server` is installed).
- **Key pairing confirmed:** `pk_test_51Tha0rH…` pairs with sandbox `acct_1Tha0rHGu8AnfRoU`.
- **Isolated alternative:** a local Supabase stack (`supabase start`) would make the job-driven flows testable too. It needs Docker, which isn't installed, and the 109 migrations haven't been replayed from scratch.
- **Release smoke test:** still worth doing. The only way to get a live bookable expert is a real live onboarding with KYC, and there's no plan for one yet.

### 2. Saved-card 3DS: confirmed broken, reproduced in the `51Tha0` sandbox

Tested with `pm_card_authenticationRequired`:
- Off-session confirm gives `authentication_required`, and the PaymentIntent goes to `requires_payment_method` with `next_action: null`.
- Retrying with the same idempotency key replays the cached 402.
- An on-session server confirm gives `requires_action` with a `use_stripe_sdk` next action.

**Backend recommends (b).** At `/bookings/confirm` the customer is present, so the backend confirms on-session. If the result is `requires_action`, it returns the existing 402 `{client_secret, stripe_account}`. **The client contract doesn't change:** `handleNextAction`, then retry confirm. The retry already skips the Stripe call when the PaymentIntent is `requires_capture` (`booking.controller.js:703`).

### 3. Retry after a decline: fixed on the backend; clients keep `booking_attempt_id`

- Same key and same card replays the 402. Same key and a different card gets `idempotency_error`, which the backend reports as `INTENT_EXPIRED`.
- **Fix:** drop the confirm idempotency key. The PaymentIntent status check and the one-booking-per-PaymentIntent database rule already prevent a double charge. (`latest_charge` stays null after `authentication_required`, so a smarter key isn't possible.)
- **Clients must not rotate the ID**; rotating leaves orphaned PaymentIntents behind.

### 4. `recover-payment`: needed for launch if long-lead paid bookings are allowed; nothing triggers it today

- When the T-24h charge needs SCA, the job leaves the booking scheduled and sends no push. `GET /client/bookings` still shows it as held, so the app gets no signal. The endpoint also has the §2 dead end: it returns the secret of a PaymentIntent with no next action.
- **Proposed backend work:**
  - On SCA, send a push with a new booking sub-category, plus an in-app notification row.
  - Add a `payment_action_required` flag (with a deadline) to client booking reads.
  - Make `recover-payment` confirm on-session before returning the secret.
- **App side:** open the booking from either signal, call `recover-payment`, then `handleNextAction`.
- Without it, these bookings are auto-cancelled 6 hours later with no warning. **The alternative** is to block long-lead paid bookings until it's built.

### 5. Further bug (iOS too): a new card needing 3DS on the short-lead path creates an unpaid booking

The server confirm leaves the PaymentIntent in `requires_action`. The code doesn't check the result (`booking.controller.js:793`), saves the booking as `requires_capture` and returns 201. The §2 fix covers this path too.

## Impact on Android (checked 2026-10-28)

- **§2:** no Android code change needed. `PaidFlowError.kt:141-146` turns the 402 `{client_secret, stripe_account}` into `StartChallenge`. `PaidFlowStripeHost` runs `PaymentLauncher` with `stripeAccountId` and `handleNextActionForPaymentIntent`, then retries confirm with the same attempt ID. Once the backend ships (b), 07-REVIEW CR-01 is resolved by the backend. Keep the device check with `4000 0027 6000 3184`.
- **§3:** matches the current Android behaviour (D-15 keeps `booking_attempt_id`). 07-REVIEW WR-01 is resolved by the backend fix; no Android change needed.
- **§5:** Android always confirms with a saved `pm_` (`MakeABookingViewModel.kt:584`, cards go through SetupIntent first), so it takes Path 1, not the new-card Path 3. It's an iOS and backend issue.
- **§4:** a new cross-platform requirement, if long-lead paid bookings stay in launch scope. **User decision needed.** Android work: `PushRouter` mapping for the new sub-category, the `payment_action_required` flag and deadline in the client booking models and UI, and a `recover-payment` call plus `PaymentLauncher` challenge (reusing the PAY-05 host).
- **§1:** blocked on the backend shipping `DISABLE_CRONS` and the uat-scoped cleanup script. The Android work (configurable dev `BASE_URL`, debug cleartext, socket host) can be done now.
  - Test scope locally: Phase 7 SC-1..SC-4, 02-UAT tests 7–8 (FND-13), the paid parts of Phase 8 cancel and Phase 11 accept/decline.
  - Untestable locally (job-driven): Phase 8 completion/void, long-lead T-24h charge.
  - Never run `live-cutover-cleanup.js`.

## Decision (user, 2026-10-28): block new long-lead paid bookings for launch (option B)

- **Android behavior (quick 261028-to1):**
  - For a paid topic only slots starting at most 6 days after now are selectable, with a short note on the time slots card.
  - A `422 LONG_LEAD_REQUIRES_SAVED_CARD` is shown as an error and resets the paid flow. The confirm-without-payment-intent path is removed.
  - The `booking` / `payment_actions` push (and `event_type` `PAYMENT_ACTION_REQUIRED`) routes to the client booking detail.
  - No recover-payment call or UI, and no `payment_action_required` fields in the booking models.
- **Accepted gap:** reschedule conversion. `RESCHEDULE-PAYMENT-CONVERSION.md` §1 forbids gating the reschedule picker, so an accepted reschedule that converts a booking to long-lead can still hit the SCA auto-cancel.
- **iOS follow-up:** iOS still creates long-lead paid bookings and handles neither the `payment_actions` push nor `recover-payment`. It should adopt the same 6-day gate or build recovery.
- **Backend follow-ups:** an optional server-side long-lead gate for launch, and a sandbox Connect account for a uat expert (`create-hero-connect.js` covers the demo expert only).
- **Runbook:** `07-LOCAL-STRIPE-TESTING.md`.

## Android follow-ups (once answered)

- **Option 1 is cleared and done by quick 261028-to1** (configurable dev `BASE_URL` and debug cleartext config). Still to do: optionally add the 07-REVIEW WR-08 publishable-key guard, then the device runs: 02-UAT tests 7–8 (closes FND-13), the Phase 7 SC-1..SC-5 checks, and the paid parts of Phases 8, 9 and 11, following `07-LOCAL-STRIPE-TESTING.md`.
- **If 3DS fix (a) or (b) is chosen:** update `PaidFlowStripeHost.kt` and `MakeABookingViewModel.kt` to match, and log the same change as an iOS follow-up.
- **`recover-payment` is not required for launch under option B** (new long-lead paid bookings are blocked). If long-lead returns, add a requirement and plan it (probably Phase 14 or 15), on both platforms.
