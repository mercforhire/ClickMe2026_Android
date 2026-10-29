# Phase 7: Local Stripe device-test runbook

**Written:** 2026-10-28 (quick 261028-to1) · **Based on:** `07-BACKEND-QUESTIONS.md` Answers §1 to §3 and the backend's quick 261028-snh (deployed `d9df858`) · **Updated:** 2026-10-28 (quick 261028-ve4) for backend 261028-uph (`2e790bf..8d4450f`).

The goal: run the Android dev build against a local backend that uses the Stripe sandbox (`acct_1Tha0rHGu8AnfRoU`), so cards, declines and 3DS can be tested on the Samsung. The backend is the real one, run with `npm start` on this Mac, against the shared production Supabase. That is why the safety rules come first.

No keys, webhook secrets, database credentials or account passwords go in this file or anywhere in this repo. Values below are placeholders.

## (a) Safety rules (read before anything else)

- Use only `uat.*@clickme.test` accounts. Never a real account, and never `appreview@clickme.test`: it transacts in live mode and the cleanup script protects it. The uat credentials are in the backend repo's `docs/UAT-ACCOUNTS.md`.
- `DISABLE_CRONS=true` on the local backend, always. Before any request, confirm the startup log line `[cron] DISABLE_CRONS=true: all 12 cron jobs are disabled`. Never set the flag on Railway. Without it, a local sandbox server would run the long-lead charge job on a live booking, and the recovery job would cancel the real booking 6 hours later.
- NEVER run `scripts/live-cutover-cleanup.js`. It is unscoped and would wipe the live accounts and every live customer.
- Keep testing windows short. While `uat.expert.preonboard@clickme.test` has its sandbox Connect account (b), prod lists that expert as bookable. Real users who try to book get a payment error (no money moves).
- Do expert-side actions on test-mode bookings through the local backend too. A prod-pointed app (for example the iOS app) would act on them with the live key and fail.
- Prod jobs (on the live key) pick up test rows and fail with "No such payment_intent". No money moves, but job results from this period cannot be trusted. The cleanup in (h) handles the rows.

## (b) Prerequisite (done 2026-10-28, backend 261028-uph): the sandbox Connect expert

- `uat.expert.preonboard@clickme.test` has a sandbox (test-mode) Connect account with charges enabled (`acct_1UVj…`; the full id and the sign-in details are in the backend's `docs/UAT-ACCOUNTS.md`), made by `scripts/uat-create-sandbox-connect.js`.
- Paid topic "260809-uch direct-charge UAT", 500 CAD / 30 min, weekly availability already set (America/Toronto). Always use a slot within the next 6 days: Android dims paid slots further out, and the backend refuses them.
- It can be booked and charged only through the local sandbox backend. Production lists it as bookable, but no money can move (see (a)).
- Re-create or repair, in the backend repo (idempotent; pass a dashboard sandbox secret key inline and never store it): `STRIPE_TEST_SECRET_KEY=sk_test_... npm run uat:sandbox-connect -- --email uat.expert.preonboard@clickme.test`. It prints the account id, the topic and a bookable sample slot inside the next 6 days.
- Undo: `node scripts/uat-testmode-cleanup.js --only uat.expert.preonboard@clickme.test` is a dry run. Add `--apply` only with the user's explicit consent. That resets the Connect link and deactivates a "Sandbox Test Session" topic.
- Push tokens: this account and `uat.expert@clickme.test` share one device token. Signing in as one on a device leaves the other with no push token until its app registers again.

## (c) Backend environment (set locally, never committed)

In the backend repo's local environment (not `.env.production`):

- `STRIPE_SECRET_KEY`: `<dashboard sandbox secret key>`. Use a dashboard sandbox secret key, not the Stripe CLI's restricted key, which expires 2026-11-07.
- `DISABLE_CRONS=true`
- `REDIS_URL=redis://localhost:6379`, with `redis-server` running.
- `STRIPE_WEBHOOK_SECRET` and `STRIPE_CONNECT_WEBHOOK_SECRET`: both set to the one `<webhook signing secret printed by stripe listen>`.
- The production `CONNECT_ONBOARD_RETURN_URL` and `CONNECT_ONBOARD_REFRESH_URL` are fine as they are (they only redirect to `clickme://`).
- The existing Supabase settings (the shared database, by user decision: no new Supabase project or Railway environment).

Then run `npm start` and check:

```
curl -s http://localhost:3000/health
```

Also check the startup log for the `DISABLE_CRONS=true` line from (a).

## (d) Webhooks

```
stripe listen --forward-to localhost:3000/webhooks/stripe --forward-connect-to localhost:3000/webhooks/stripe-connect
```

The paths are at the root, not under `/api/v1`. Copy the printed signing secret into both variables from (c), then restart the backend. Sandbox webhook events write rows into the shared `stripe_events` table; the cleanup script handles them.

## (e) Phone

1. Forward the port (add `-t <transport>` when the Samsung is attached twice):
   ```
   ~/Library/Android/sdk/platform-tools/adb reverse tcp:3000 tcp:3000
   ```
2. In the Android repo's `local.properties` (gitignored) add:
   ```
   clickme.devBaseUrl=http://localhost:3000/api/v1
   ```
   Look for the Gradle warning "The dev BASE_URL comes from clickme.devBaseUrl in local.properties".
3. Build and install: `./gradlew assembleDevDebug`, then `adb install -r app/build/outputs/apk/dev/debug/app-dev-debug.apk`.
4. Sign in as `uat.client@clickme.test`.

Notes:

- The dev flavor already carries the matching `pk_test_51Tha0...` publishable key (sandbox `acct_1Tha0rHGu8AnfRoU`).
- The debug build allows cleartext only to `localhost` and `127.0.0.1`. A release build cannot reach the local backend, and the prod flavor ignores `clickme.devBaseUrl`.
- If requests fail, check logcat for a cleartext error first, then confirm `adb reverse --list` shows port 3000.

## (f) Test cases

Each with its expected result.

1. **Save a card (Phase 7 SC-1).** Payment Methods -> Add Payment Method -> 4242 4242 4242 4242. Expect the card listed; Set as default moves the badge; Remove asks first.
2. **Short-lead paid booking (SC-2).** Book uat.expert.preonboard's paid topic "260809-uch direct-charge UAT" on a slot within 6 days with a saved card. Expect the sheet with saved cards, then Request Sent, and the request arrives on the expert side (through the local backend).
3. **Paid window (quick 261028-to1).** For a paid topic, slots more than 6 days out are dimmed, their days have no dot, and the note "Paid sessions can be booked up to 6 days ahead." shows. A free topic is unaffected. Optional 422 check: set the phone clock one day ahead with automatic time off, pick a paid slot about 6 days ahead on the phone, and expect the "Paid sessions can be booked up to 6 days ahead" dialog with nothing booked. Restore automatic time afterwards.
4. **3DS on a saved card.** Save 4000 0027 6000 3184 and book with it. Expect a challenge on the connected account, then the confirm retry creates the booking (backend fix (b) from Answers §2). Cancelling the challenge shows "Payment wasn't completed. Your card wasn't charged."
5. **Decline, then retry with another card.** Save 4000 0000 0000 0341 (it attaches, then charges fail) and book: expect "Your card was declined". Then press Book Now again and choose 4242. The same `booking_attempt_id` is kept (Android D-15), the booking is created, and the Stripe sandbox dashboard shows one PaymentIntent for the attempt (Answers §3).
6. **409 PAYMENT_IN_PROGRESS.** Android has no dedicated mapping: `PaidFlowError.kt` falls through to its default row, so the dialog shows the server message, and Book Now retries with the same attempt id. If it appears (for example a retry while a confirm is still settling), record what you see.
7. **payment_actions push routing.** It cannot be triggered locally because the sender is cron driven. Simulate the tap:
   ```
   adb shell am start -n com.uptrendinvestments.clickmeapp/.MainActivity --es category booking --es sub_category payment_actions --es booking_id <uuid of a uat client booking>
   ```
   Expect the client booking detail to open. The activity is `.MainActivity` (exported) in `app/src/main/AndroidManifest.xml`.
8. **Other paid checks.** The paid parts of Phase 8 (cancel) and Phase 11 (accept/decline), plus 02-UAT tests 7-8 (FND-13). Do the expert-side actions through the local backend too.
9. **Paid reschedule beyond 6 days (RESCHEDULE_TOO_FAR_AHEAD, backend 261028-uph). OPTIONAL.** Skip this unless you want the extra coverage. After 261028-uph the code is normally unreachable: propose and accept are both capped at 6 days from now, and time only brings a pending proposal closer, so normal use cannot produce it. Only a proposal made before 261028-uph can hit it, because accept re-checks against the server clock. Unit tests already cover the mapping (server text, fallback copy, reload), so this case only proves the dialog on a device.
   - Setup: (1) Have a confirmed paid uat.client booking with uat.expert.preonboard (test case 2, expert actions through the local backend). (2) Stop the local backend. In a separate checkout of the backend at `d9df858` (the last commit before 261028-uph; it already has `DISABLE_CRONS`), for example `git worktree add ../backend-pre-uph d9df858`, start it with the same environment as (c), and confirm the `DISABLE_CRONS=true` startup line. (3) As the expert, propose a start more than 6 days ahead: get a token with `node scripts/uat-lib.js uat.expert.preonboard@clickme.test`, then call `PATCH http://localhost:3000/api/v1/expert/bookings/<booking id>/reschedule` with an `Authorization: Bearer <token>` header and the JSON body field `proposed_start` (an ISO time with offset, more than 6 days ahead). If this pre-change propose fails, record what you see and stop. (4) Stop it, start the current backend again, and confirm the `DISABLE_CRONS=true` line again.
   - Expected on the phone, signed in as uat.client: Upcoming Booking Details -> Accept the reschedule -> the "Couldn't update booking" dialog shows the server text "Paid sessions can be rescheduled up to 6 days ahead. Please choose a time within the next 6 days.". The detail reloads, the booking keeps its original time and the proposal is still pending. Decline still works. The Android fallback copy "Ask for a time within the next 6 days." appears only if the server sends no message, and unit tests cover that.
   - Optional expert variant: sign the phone in as uat.expert.preonboard (expert mode; mind the push-token note in (b)), have the client propose on the pre-change backend with a token from `node scripts/uat-lib.js uat.client@clickme.test` and `PATCH .../api/v1/client/bookings/<booking id>/reschedule`, then accept on the expert Booking Summary. Expect the same dialog and reload.
   - Afterwards: `git worktree remove ../backend-pre-uph`, then run the cleanup in (h).

## (g) Not testable locally (cron or job driven)

Expiry void, completion capture, the long-lead T-24h charge, recover-payment, auth-expiry capture and fee reconciliation. These need the job runner (disabled here on purpose) or a local Supabase stack, which needs Docker (not installed).

## (h) Cleanup (every session)

1. Stop `stripe listen`.
2. Remove the `clickme.devBaseUrl` line from `local.properties` and rebuild.
3. `adb reverse --remove tcp:3000`.
4. In the backend repo run the scoped, dry-run-first cleanup:
   ```
   node scripts/uat-testmode-cleanup.js
   ```
   Read its output. Run it again with `--apply` (optionally `--only uat.client@clickme.test`) only with the user's explicit consent.
5. Never `live-cutover-cleanup.js`.
