# Phase 16 Matrix Runbook (blocking, D-09)

One ordered runbook for the QA-02 matrix (Direction 1: Android client, iOS expert), the QA-03 matrix (Direction 2: iOS client, Android expert) and the tier-1 rows of `15-DEVICE-CHECKLIST.md` (D-06). A person runs every row on the Play internal-track build; Claude captures logs and records results; nothing is ticked from automated test results. Accounts and directions switch once (D-14). Phase 16 cannot close, and Phase 17 cannot start, until the user confirms Section 8 (D-09).

## Header

- **Devices (D-13):** Samsung SM-A075F (Android 16, API 36) and the user's real iPhone with the production iOS app (its `Environments.plist` stays on Production, D-01). No simulators for matrix rows. The `Medium_Phone_API_37.0` emulator is used only for rows marked "emulator" (D-10).
- **Build under test:** the `prodRelease` installed from the Play internal testing track, versionCode as recorded in the Session log. Never `devDebug`, never a sideloaded build (D-08, D-15). Every session starts with Harness H1-H4.
- **Backend:** production, Stripe live mode, real cards, real money (D-01). Amounts follow the money plan the user confirms in 16-03 (recorded in P0.9).
- **Accounts by role only (D-03):** A = the user's real expert account whose live Connect account accepts charges. B = the user's second real account, used as the client. Throwaway = a new account created on Android in Direction 2 for the deletion rows. Never use the store-review account `appreview@clickme.test` for any row. Never paste a password, token, key or card number into this file; card references use the brand and last 4 digits only.
- **Money safety:** every paid booking is written into "Paid bookings created" when it is made. Stripe test cards are not used (D-02). Never run `scripts/live-cutover-cleanup.js` or any bulk script against production.
- **Columns:** `Row ID | Source row ID | Direction | Steps | Expected | Evidence to capture | Result`. Direction values: `D1` (Samsung = B client, iPhone = A expert), `D2` (Samsung = A expert, iPhone = B client), `Samsung` (single device), `emulator`.
- **Result notation:** `[ ]` not yet run; `[x]` then `pass (vc N, YYYY-MM-DD)` when it passed; `[!]` then `fail (BUG-NN, vc N)` when it failed; `[-] not run: reason`. A row passes only on the build proven by H2 for that session. A failed row's Result also gets a row in `16-BUG-LOG.md`.
- **Evidence root:** `/Users/LeonChen/ClickMe-release` (outside the repo): `logs/SESSION/ROW.log` and `sessions/SESSION-results.md`. Raw logs and screenshots never enter the repo; this file and the bug log carry ids and error codes only.

## Harness (Claude, every session)

1. **H1 transport.** `ADB=/Users/LeonChen/Library/Android/sdk/platform-tools/adb`; run `$ADB devices -l`; read `transport_id:N` on the `model:SM_A075F` line (it may be listed twice over wireless ADB). Always use `$ADB -t N`, never `-s` (`-s` hangs on wireless ADB). `T` below is that N.
2. **H2 prove the build.** `$ADB -t T shell dumpsys package com.uptrendinvestments.clickmeapp | grep -E "versionCode|lastUpdateTime|firstInstallTime"` and `$ADB -t T shell pm list packages -i com.uptrendinvestments.clickmeapp`. The versionCode must equal the newest row of `16-BUILD-LEDGER.md` whose Play status is available; the installer must be `com.android.vending`; lastUpdateTime must be later than that upload (Phase 11 stale-build lesson). Play can take minutes to update: open the Play listing, tap Update, then re-check.
3. **H3 backend.** `curl -s https://clickme-backend-production.up.railway.app/health` returns status success and a commit; record it in the Session log.
4. **H4 clean slate.** `$ADB -t T logcat -c`, then the crash buffer count `$ADB -t T logcat -d -b crash | grep -c com.uptrendinvestments.clickmeapp` (expect 0).
5. **H5 per-row capture.** `$ADB -t T logcat -v threadtime -s Realtime ClickMeCall CallService RtcEngine TokenStore AndroidRuntime > /Users/LeonChen/ClickMe-release/logs/SESSION/ROW.log`. Call proof lines: `remote joined elapsed=`, `remote audio state=2`, `remote offline reason=`. Also `$ADB -t T shell dumpsys audio`, `$ADB -t T shell dumpsys activity services com.uptrendinvestments.clickmeapp`, and `$ADB -t T pull /sdcard/Android/data/com.uptrendinvestments.clickmeapp/files/agoraapi.log` (expect `channelProfile:0`).
6. **H6 killed-app push.** Swipe ClickMe away in Recents (never `am force-stop`, which blocks FCM delivery). `$ADB -t T shell pidof com.uptrendinvestments.clickmeapp` must print nothing. Trigger the push from the iPhone, then `$ADB -t T shell dumpsys notification --noredact | grep -A25 "pkg=com.uptrendinvestments.clickmeapp"`.
7. **H7 battery state.** One UI Settings, Battery, Background usage limits: Sleeping apps and Deep sleeping apps must not list ClickMe. Then `$ADB -t T shell am get-standby-bucket com.uptrendinvestments.clickmeapp` and `$ADB -t T shell am get-inactive com.uptrendinvestments.clickmeapp`.
8. **H8 forged Connect return.** `$ADB -t T shell am start -a android.intent.action.VIEW -d clickme://connect/return`.
9. **H9 results file.** `/Users/LeonChen/ClickMe-release/sessions/SESSION-results.md`, one line per row: `ROW | pass or fail or not run | note`. Paid lines: `PAID | ROW | booking id first 8 | PaymentIntent last 6 | amount | expected end (void or capture)`. Free-form findings: `FREE | title | steps`.
10. **H10 API-side proof.** The release build has no HTTP logging, so API-side proof comes from Railway logs (confirm-connection, join, capture), the Stripe dashboard on A's connected account (live mode) and the Supabase `device_tokens` table. If the user cannot open one, they read the value aloud and Claude records it.

Commands may need adapting on One UI (RESEARCH A9); record the command actually used in the row's log.

## Session log

| Session | Date | Build (dumpsys versionCode) | Installer | Backend /health | Transport | Rows run | Fails |
|---|---|---|---|---|---|---|---|
| preflight | 2026-11-02 | versionCode=3 | com.android.vending | 8d4450f | 1868 | 0 | 0 |

Later plans add the sessions `preflight`, `session-A`, `batch-A`, `session-B`, `batch-B` and `close-out`.

Phase 16.2 adds rows D1.RS1-D1.RS8 and D2.RS1-D2.RS4; they run on the first Play build that contains Phase 16.2 (after Phase 16.5, per ROADMAP).

Phase 16.3 adds rows D1.FV1-D1.FV7, D2.FV1 and FS.FV1-FS.FV3; they run on the first Play build that contains Phase 16.3 (after Phase 16.5, per ROADMAP).

Phase 16.4 adds rows D1.PN1-D1.PN5, D2.PN1-D2.PN2, D2.CP1-D2.CP3, FS.PN1 and FS.CP1, plus the backstops D1.PN6-D1.PN7, D2.PN3 and D2.CP4-D2.CP5 reported by plans 16.4-03, 16.4-06, 16.4-08 and 16.4-09; they run on the first Play build that contains Phase 16.4 (after Phase 16.5, per ROADMAP).

Phase 16.1 adds rows D1.SE1-D1.SE5 and TB.SE1; they run on the first Play build that contains Phase 16.1 (after Phase 16.5, per ROADMAP).

Phase 16.5 adds rows D1.PH1-D1.PH5, D1.NS1-D1.NS3, D1.FB1-D1.FB2, D1.SP1-D1.SP4, D2.NS1, D2.FB1, D2.PV1, TB.PF1 and FS.PH1, FS.NS1, FS.FB1, FS.SP1; they run on the versionCode 3 Play build.

## 0. Pre-flight

Direction `D1` unless stated. Run before any money row.

| Row ID | Source row ID | Direction | Steps | Expected | Evidence to capture | Result |
|---|---|---|---|---|---|---|
| P0.1 | D-03, P13 (a) | D1 | Account A signed in on the iPhone in expert mode; account B signed in on the Samsung in client mode. Choose (do not create yet) the Throwaway email. | Neither account is the store-review account; roles A and B each map to one real account of the user. | User confirms aloud; role map noted in the results file without emails or passwords. | [ ] |
| P0.2 | RESEARCH Pitfall 1, WR-09 | D1 | A opens the Topic Editor on iOS or Android and reads the minimum the price field accepts. A has one paid 30-minute topic at that floor, one free 30-minute topic and one free 90-minute topic. | The floor reads CA$5.00 (A1); the three topics exist and are bookable. If the floor differs, P0.9 uses the live floor. | The floor value and the three topic titles. | [ ] |
| P0.3 | RESEARCH Pitfall 9 | D1 | A's availability covers today and the next 6 days with slots in the next hours. | Paid bookings are possible up to 6 days ahead; at least one slot sits inside the join window for call rows. | The slot times seen from B on the Samsung. | [ ] |
| P0.4 | D-02, RESEARCH A2 | D1 | On the Samsung as B: Payment Methods, Add Payment Method with the main live card, then with the 3DS candidate card and the decline candidate if the user has them. No charge is made (SetupIntent). Limit: 10 per 15 minutes per user. | Per card: saved, and whether a bank challenge appeared during setup. | Brand and last 4 only, challenge yes or no, per card. | [ ] |
| P0.5 | RESEARCH A10, P13 (b) | D1 | B sends A a chat message from the Samsung. | The iPhone shows a push. If not, log owner ios and use realtime screens for iOS-side push rows. | Yes or no on the iPhone push; screenshot stays outside the repo. | [ ] |
| P0.6 | RESEARCH Pitfall 7, P13 (a) | D1 | Supabase `device_tokens`: check A and B. | A has one row with platform ios; B has one row with platform android. | The two platform values read from the table. | [ ] |
| P0.7 | RESEARCH Pitfall 10, 09-HUMAN-UAT 7 | D1 | Record the Samsung battery state (H7) before any call row; run with defaults (the real-user case). | ClickMe is not in Sleeping apps or Deep sleeping apps; bucket recorded. | The H7 outputs in `logs/preflight/P0.7.log`. | [ ] |
| P0.8 | D-14, H10 | D1 | Confirm the user can open the Stripe dashboard for A's connected account in live mode, Railway logs and the Supabase table editor, or will read values aloud. | Each of the three is reachable, or the read-aloud fallback is agreed. | Which of the three the user can open. | [ ] |
| P0.9 | D-01, D-02, D-04 | D1 | Money plan confirmed by the user 2026-10-30 (16-03 Task 2): option-a; floor CA$5.00 default, final value is the P0.2 read (if it differs, this plan uses the live floor); captures 2 (D1.25 and D2.12 only, each refunded by hand in Section 7); every other paid row is a void (cancel, decline, expiry); 3DS route: own challenging card (the user's own card whose issuer challenges online payments; the P0.4 setup challenge is recorded, D1.21 and D2.11 confirm it, A2). | A written money plan the user confirmed before any charge. | The plan text in this cell. | [ ] |
| P0.10 | D-03, P14 (d) | D2 | Throwaway plan for the deletion rows (D2.17, D2.18): a real email the user controls, signed up on Android in Direction 2. | The email exists and the user can read its inbox for the verification code. | Confirmed aloud; no address written here. | [ ] |

## 1. Release smoke (Samsung, Play build)

| Row ID | Source row ID | Direction | Steps | Expected | Evidence to capture | Result |
|---|---|---|---|---|---|---|
| R1.1 | L1, P3 SC-1 | Samsung | Cold start from the Play install and log in as B. | Login lands on the client home; H4 crash count is 0. | H2 output, H4 count. | [ ] |
| R1.2 | P3 lifecycle, P3 SC-3 | Samsung | With H5 running, watch `Realtime` after login, background the app, then return. | `state=Connecting` then `state=Connected`; backgrounding logs `Disconnected` about 1 s later; returning reconnects. | `Realtime` lines in the row log. | [ ] |
| R1.3 | P13 (a) | Samsung | After login on Android 13+, note the notification permission prompt; log out and log in again. | The prompt appears once and not again after logout and login. | Yes or no, noted in the results file. | [ ] |
| R1.4 | P13 (a), 13-RELEASE-CHECKLIST step 10 | Samsung | Read B's `device_tokens` row, background and return the app, read it again. | Platform is android and the row is refreshed after background and return. | Platform and updated-at read from Supabase. | [ ] |
| R1.5 | P13 (c) | Samsung | Settings, Apps, ClickMe, Notifications. | The five ClickMe channels are listed. | Channel names read aloud or from a screenshot kept outside the repo. | [ ] |

## 2. Direction 1 (QA-02): Android client B, iOS expert A

Samsung = B (client), iPhone = A (expert). Rows run in order. Amounts follow P0.9. Every paid row writes a `PAID` line (H9) and a row in "Paid bookings created".

### 2a Booking and push

| Row ID | Source row ID | Direction | Steps | Expected | Evidence to capture | Result |
|---|---|---|---|---|---|---|
| D1.01 | P6 SC-3 regression, P13 (b) | D1 | B books A's free 30-minute topic for a slot in the next hours. | Request Sent shows Awaiting Approval; the iPhone (A) receives the request push. | H5 log; iPhone push yes or no. | [ ] |
| D1.02 | P13 (b) killed, P11 (b) mirror | D1 | Swipe ClickMe away (H6). A accepts on iOS. | The booking push arrives on the killed Samsung; tapping it opens that booking in the Bookings tab as Confirmed. | H6 `pidof` and `dumpsys notification` output. | [ ] |
| D1.03 | P8 SC-1, P8 SC-2 | D1 | Keep My Bookings Upcoming open while A acts on iOS; then open Upcoming Details. | The list refreshes by realtime `booking.update` without a pull; Upcoming Details shows Confirmed, Join and Message. | `Realtime` lines in the H5 log. | [ ] |
| D1.04 | P13 (b) running, P13 (c) | D1 | With the app open, A sends a chat message and then triggers a booking push. | Both post on the Messages and Bookings channels with the ClickMe icon; the open thread hides its own banner. | `dumpsys notification` channel names. | [ ] |

### 2b Chat

| Row ID | Source row ID | Direction | Steps | Expected | Evidence to capture | Result |
|---|---|---|---|---|---|---|
| D1.05 | P12 (a), P12 (b) | D1 | Message from Upcoming Details. Exchange messages both ways; type on each side; use a thread with more than 30 messages for Load earlier; airplane mode and send; background then foreground. | Live both ways without duplicates; typing both ways; Read on Android bubbles; Load earlier pulls 30 more; the failed bubble retries once with no duplicate; catch-up after foreground. | `Realtime` lines (ids only) in the H5 log. | [ ] |

### 2c Call on the confirmed free booking

| Row ID | Source row ID | Direction | Steps | Expected | Evidence to capture | Result |
|---|---|---|---|---|---|---|
| D1.06 | P9 SC-1 | D1 | Join inside the window from Upcoming Details opened from Explore Today's Sessions, from My Bookings and from the My Bookings card. At the first mic prompt: deny once, then deny permanently, then allow. | Each entry point reaches the call layer; deny once shows the rationale; deny permanently shows Open Settings; allow joins. | H5 `ClickMeCall` lines per entry point. | [ ] |
| D1.07 | P9 SC-1, P11 (c) mirror, G-11-6 | D1 | A joins the same booking on iOS. Test two-way audio, mute and speaker. | `remote joined elapsed=` and `remote audio state=2` in the log; `channelProfile:0` in agoraapi.log; both UIs leave "waiting for the other person". | H5 log and the pulled agoraapi.log. | [ ] |
| D1.08 | P9 SC-3 | D1 | In the call, minimize to the pill on every tab. Try Client/Expert switch, logout, Add Payment Method and Cancel of this booking. Press system Back in the call layer. | The pill shows on every tab; each of the four actions is refused by the call guard; predictive back minimizes. | Screenshots kept outside the repo. | [ ] |
| D1.09 | P9 SC-2, FND-14 (replaced) | D1 | Press Home, turn the screen off for at least 5 minutes while talking. | Two-way audio throughout; the notification shows title and chronometer; the call is still connected after 5+ minutes. | H5 `dumpsys audio` and `dumpsys activity services` captured while backgrounded; H7 state. | [ ] |
| D1.10 | P9 SC-4 | D1 | A hangs up on iOS, then rejoins; stay until the final 5 minutes of the booking. | Waiting for other person shows; audio resumes after rejoin (`remote offline reason=` then `remote joined`); the final 5 minutes show the amber state and banner. | `ClickMeCall` lines in the H5 log. | [ ] |
| D1.11 | P9 SC-2 notification, WR-06 | D1 | Open a Custom Tab (Help, Email Us or a meeting link), then tap the ongoing-call notification. Then tap End call in the notification. | A single MainActivity task; End call ends the call on both phones. | `dumpsys activity activities` task count in the log. | [ ] |
| D1.12 | 09-HUMAN-UAT 7 | D1 | Bluetooth headset routing if a headset exists; an incoming GSM call to the Samsung during a call from another phone; audio after it ends. | Audio follows the headset; the call survives or recovers after the GSM call. Not run if the hardware is missing. | `dumpsys audio` snippet; `[-] not run: no headset` if so. | [ ] |
| D1.13 | WR-05, WR-07, WR-09 | D1 | WR-07: book with a Skype or Zoom meeting type and an unopenable link. WR-09: on a free 90-minute booking, rejoin after start + 60 minutes. WR-05: note any refused foreground start on API 34+. | WR-07 shows "Couldn't open meeting" and nothing is confirmed; WR-09 rejoin works; WR-05 is recorded only if it happens. | H5 log lines for each. | [ ] |
| D1.14 | 09-HUMAN-UAT 8 | D1 | Ask the user: keep the plain ongoing-call notification or switch to CallStyle. | The answer is recorded; a switch becomes a fix-if-cheap bug row. | The user's answer in the results file. | [ ] |

### 2d Cards and the paid booking (amounts per P0.9)

| Row ID | Source row ID | Direction | Steps | Expected | Evidence to capture | Result |
|---|---|---|---|---|---|---|
| D1.15 | P7 SC-1 | D1 | Payment Methods lists the pre-flight cards. Set as default; Remove (confirm first); remove every card. | The default badge moves after the reload; Remove asks first; the list ends empty. | Brand and last 4 only. | [ ] |
| D1.16 | P7 SC-3 | D1 | With no saved card, open A's paid topic. Add the main live card from the "Add a Payment Method" gate, press Back. Look at a slot more than 6 days out. | The gate shows; Back keeps every selection and shows Book Now; a slot more than 6 days out is dimmed with the 6-day note. | Screenshots kept outside the repo. | [ ] |
| D1.17 | P7 SC-2 dismiss | D1 | Book Now on a paid slot within 6 days, dismiss the sheet with the preselected card. | Silent cancel: no booking and no PaymentIntent in requires_capture. | Stripe dashboard list (H10). | [ ] |
| D1.18 | P7 SC-2 hold | D1 | Book Now, pick the saved card. | Request Sent with the hold card; A receives the request; Stripe shows a PaymentIntent in requires_capture for the floor amount. PAID line, expected end void. | Stripe dashboard (PI last 6, amount); booking id first 8. | [ ] |
| D1.19 | P7 SC-2, P8 SC-3, P8 SC-1 | D1 | B cancels that held booking with a comment. | Booking moves to the Past tab with a snackbar; A sees the cancellation; the Stripe PaymentIntent is canceled. Label void (no charge, no refund, no fee), never refund. | Stripe dashboard status canceled. | [ ] |
| D1.20 | P7 SC-2 retry | D1 | Airplane mode during confirm, then Book Now again. If a server-created booking exists, the slot conflict points to My Bookings. Cancel anything created. | Network copy with the payment reference; no duplicate booking on retry; created bookings are cancelled (void). | PAID line for any booking created; Stripe status. | [ ] |
| D1.21 | P7 SC-4, D-02 | D1 | Use the 3DS card from P0.4: book paid; pass the bank challenge. Then a second attempt that cancels the challenge. | The 402 challenge runs on A's connected account, passes, and the booking confirms (PAID line); the cancelled attempt shows "Payment wasn't completed. Your card wasn't charged." and Book Now retries. `[-] not run: no challenging card` only under the P0.9 plan. | Stripe status; booking id first 8. | [ ] |
| D1.22 | D-02 decline | D1 | Book paid with the declining card. | A payment-failed message shows; another card retries with the same attempt. Or `[-] not run: no failing card`. | Error copy and code only. | [ ] |
| D1.23 | P7 SC-5 | D1 | Toggle dark mode (phones are portrait-locked, so this is the recreation) while the payment sheet and the challenge are open. | Neither launches twice. The JPY part is `[-] not run: no JPY topic in production`. | H5 log. | [ ] |
| D1.24 | P8 SC-4 | D1 | A proposes a reschedule on iOS. B accepts one booking and declines another, with and without a reason. Then trigger `PAYMENT_METHOD_REQUIRED` recovery with a saved card and with none. | The booking leaves pending_reschedule; recovery works with a saved card and with none. | Error code and outcome only. | [ ] |

### 2d-R Reschedule start (Phase 16.2)

Android client B proposes; iOS expert A responds. Paid rows end in a void (cancel), never a capture.

| Row ID | Source row ID | Direction | Steps | Expected | Evidence to capture | Result |
|---|---|---|---|---|---|---|
| D1.RS1 | PARITY #10, D-09 | D1 | Free booking. B taps Reschedule on Upcoming Booking Details, picks a new slot, adds a message and submits. A sees the proposal on iOS and accepts. | B is back on the details with "Reschedule request sent" and "Awaiting expert's response"; A sees the proposal; after the accept the booking shows the new time on both phones. | Outcome per step; booking id first 8. | [ ] |
| D1.RS2 | D-05, D-09 | D1 | From a My Bookings card on another free booking, B proposes a new slot. A declines on iOS. | The request is sent from the card; after the decline the booking keeps its original time on both phones. | Outcome per step. | [ ] |
| D1.RS3 | D-01, D-03 | D1 | Paid held booking (P0.9 amount). B opens the picker. Then B proposes a slot inside the 6-day window and inside the auth window; A accepts on iOS. Then B cancels the booking (void). | Days beyond now + 6 days show no dot and their chips are dimmed with "Paid sessions can be rescheduled up to 6 days ahead."; the in-window proposal is accepted; the cancel voids the hold. PAID line, expected end void. | Stripe dashboard status canceled; booking id first 8. | [ ] |
| D1.RS4 | D-11, RESCHEDULE-PAYMENT-CONVERSION section 2 | D1 | A paid booking authorized at least one day earlier. B picks a slot whose session end is after the card hold expiry and within 6 days. Cancel at the dialog once and confirm nothing changed; propose again and Continue. A accepts on iOS. Then B cancels the converted booking (void). | The "Your card hold will be released" dialog shows before any request (with the sooner-than-24-hours line when under 48 h); Cancel changes nothing; Continue sends; on accept the booking converts and Stripe shows the original PaymentIntent canceled. PAID line, expected end void. Open item: the server's under-24 h conversion message says "choose a later time" while the app shows the fixed "cancel and book again" copy; record which text appears. | Stripe status of the original and any new PaymentIntent (last 6, status only); booking id first 8; the copy seen. | [ ] |
| D1.RS5 | D-06, D-07, D-08, RESCHEDULE-PAYMENT-CONVERSION section 3.1 | D1 | A paid booking first paid with a one-off PaymentSheet card that was not saved, and B has no saved card. B proposes a reschedule that needs conversion. Follow the "Add a payment method" dialog to Payment Methods, add a card and return. Check that nothing is sent automatically. B taps Submit again. Then cancel the booking (void). | The "Add a payment method" dialog shows; "Add a Payment Method" opens Payment Methods; after returning nothing has been sent; the second Submit succeeds, or shows "The card on file can't be used for this reschedule…" on a second PAYMENT_METHOD_REQUIRED. PAID line, expected end void. | Error codes seen (PAYMENT_METHOD_REQUIRED once or twice) and the outcome; brand and last 4 at most; booking id first 8. | [ ] |
| D1.RS6 | D-04 (amended 2026-11-01) | D1 | (a) On a free confirmed booking that starts in under 24 h, B taps Reschedule on the My Bookings card, picks a later slot and submits; A declines on iOS. (b) Look at a pending-approval booking and a booking already pending reschedule, on Upcoming Details and on the My Bookings card. | (a) Reschedule is present and the request is sent ('Reschedule request sent'); after A's decline the booking keeps its original time on both phones. (b) Reschedule is present on the pending-approval booking on both surfaces and absent on the pending-reschedule booking on both surfaces (iOS parity). | Yes or no per case and surface; outcome of (a); error code if any. | [ ] |
| D1.RS7 | D-12 | D1 | Open the picker on a confirmed upcoming booking. | The picker opens on the current booking's day; the "Current" chip is disabled and the "Current:" banner shows; past slots are hidden. | Outcome per item. | [ ] |
| D1.RS8 | RESEARCH Open Question 5 | D1 | A 60-minute booking: B proposes a slot inside a 30-minute gap; A accepts on iOS. | Record whether A's accept returns SLOT_UNAVAILABLE (known behavior, iOS parity). | Error code and outcome only. | [ ] |

### 2e Capture

| Row ID | Source row ID | Direction | Steps | Expected | Evidence to capture | Result |
|---|---|---|---|---|---|---|
| D1.25 | P9 SC-5 | D1 | Make a paid booking inside the join window, A accepts, both join and finish the session. | Railway shows confirm-connection exactly once; about 15 minutes after the scheduled end Stripe shows the PaymentIntent succeeded on A's connected account. PAID line, expected end capture, refunded in Section 7. | Railway log line count; Stripe status; booking id first 8. | [ ] |

### 2f After sessions

| Row ID | Source row ID | Direction | Steps | Expected | Evidence to capture | Result |
|---|---|---|---|---|---|---|
| D1.26 | P13 (e) | D1 | Write Review from the session ended push, the review nudge push, Booking Summary and a Past card. | Read-only after submit; the Feedback block and the thanks snackbar show. | Outcome per entry point. | [ ] |
| D1.27 | P8 SC-1 Past | D1 | Open the Past tab. | Past labels are right; a completed-but-not-ended session follows the rejoin rule. | Labels noted. | [ ] |
| D1.28 | P13 (d) | D1 | Open the notifications list from both bells; receive a foreground push; tap rows; turn the permission off and use the settings round trip. | Grouped by date; a foreground push reloads it; rows route like pushes; the permission-off row works. | Outcome per bullet. | [ ] |
| D1.29 | P12 (c), P12 (d) | D1 | Run last in Direction 1. B reports A from chat (Supabase report row), blocks from chat ("Blocked {name}") and from the profile; the iPhone side tries to send; Unblock. | The report row stores the enum value; the iPhone side cannot send; Unblock restores the composer. | Supabase report row (enum value only). | [ ] |

### 2f-F Saved Experts, favorite heart and full Reviews (Phase 16.3)

Samsung = B (client). No money moves in these rows.

| Row ID | Source row ID | Direction | Steps | Expected | Evidence to capture | Result |
|---|---|---|---|---|---|---|
| D1.FV1 | PARITY #2b, D-01..D-04 | D1 | B opens A's profile from Explore. Watch the heart; tap it; leave and reopen the profile; tap it again; then double tap quickly. | The outlined heart shows only after the profile loads; a tap fills it; after reopening it is still filled; the next tap outlines it; a fast double tap changes it once. | Outcome per step. | [ ] |
| D1.FV2 | D-03 | D1 | With airplane mode on, tap the heart on A's profile. | The heart flips and rolls back with a snackbar (connection message or "Couldn't update favorites"). | Snackbar text seen. | [ ] |
| D1.FV3 | PARITY #2, D-09, D-10, D-12 | D1 | Favorite A. Open Profile, Discovery, Saved Experts. Open A's profile from the card and unfavorite; press Back. Favorite A again; on the list tap A's heart; try Cancel, then Remove. | A is listed with photo, name, green headline and tags; after unfavoriting and Back, A is gone without a pull; the dialog reads "Remove {A} from favorites?" with Cancel and Remove; Cancel keeps A; Remove removes A and shows the empty state. | Outcome per step. | [ ] |
| D1.FV4 | D-05..D-08 | D1 | On the Saved Experts empty state: read the Pro Tip; tap "Explore Experts"; go back to Profile; tap "View My History". | The Pro Tip names the heart icon; "Explore Experts" opens the Explore tab root; back on Profile, Saved Experts is still open; "View My History" opens Bookings with Past selected. | Outcome per step. | [ ] |
| D1.FV5 | PARITY #3, D-13, D-14, D-16 | D1 | Tap "See all" on A's Recent Reviews from Explore, from Search, from a chat "View profile" and from Saved Experts. Switch tabs while two of these are open. | "{First}'s Reviews" shows the overall card and review cards; Back returns to the profile; switching tabs with two open causes no crash. | Outcome per origin; H4 crash count. | [ ] |
| D1.FV6 | D-13 | D1 | Open the profile of an expert with 0 reviews; tap "See all". | The Recent Reviews card shows only its header and "See all"; "See all" shows "No reviews yet". | Outcome per step. | [ ] |
| D1.FV7 | CR-01, D-05 | D1 | Favorite A. Open Profile, Discovery, Saved Experts, then A's profile. Book A's free topic to Request Sent; tap "View My Bookings"; tap the Profile tab. Book the same way again; tap "Back to home"; tap the Profile tab. As B, cancel both bookings. | "View My Bookings" opens Bookings and "Back to home" opens Explore; each time the Profile tab shows the hub with the bottom bar, not Request Sent and not Saved Experts. | Outcome per exit. | [ ] |

### 2f-N Private notes, client side (Phase 16.4)

Samsung = B (client). No money moves in these rows.

| Row ID | Source row ID | Direction | Steps | Expected | Evidence to capture | Result |
|---|---|---|---|---|---|---|
| D1.PN1 | PARITY #7, D-03, D-05 | D1 | On a past card B taps "Private Note", types a note and taps Save. Then Back. | The header shows A's name, the topic and the date; the Save action shows a spinner, then a check for about 1.5 s; the screen stays; after Back the card shows the preview. | Outcome per step. | [ ] |
| D1.PN2 | D-07, D-08 | D1 | On an upcoming card B taps "Add private note", saves, and returns. Tap the preview pencil. Open Upcoming Booking Details. | The card shows the preview with a pencil; tapping it shows the saved text; Upcoming Booking Details shows no note card. | Outcome per step. | [ ] |
| D1.PN3 | D-06, D-08 | D1 | B opens Booking Summary from a past card, from a booking event in a chat thread and from a push. Tap "My Note", edit, go back. | "My Note" opens the editor from all three; after an edit the full note shows on return. | Outcome per entry point. | [ ] |
| D1.PN4 | D-01, D-02, D-04 | D1 | With unsaved edits press the app bar arrow, then system back. Choose Keep editing, then Discard. With no edits press Back. Paste over 5,000 characters. Save only spaces. | Each back asks "Discard changes?"; Keep editing keeps the text; Discard leaves; with no edits no dialog shows; the paste is cut at 5,000 and the counter shows from 4,500; the spaces-only save clears the note and the card shows no preview. | Outcome per step. | [ ] |
| D1.PN5 | PARITY #7, #8, RESEARCH notes are independent columns | D1 | On one booking B writes a client note on Android and A writes an expert note on iOS. Each then reads their own note on their own platform. | Each side reads only their own note; neither sees or overwrites the other's. | Yes or no per side; never the note text. | [ ] |
| D1.PN6 | 16.4-06 manual backstop | D1 | A booking push opens Booking Summary on the Samsung (app killed, then backgrounded). Tap "My Note", save a note, press Back twice. | "My Note" opens the editor from the push-opened summary; the saved note shows on return; Back leaves the app stack in the order the push built. | Outcome per launch state. | [ ] |
| D1.PN7 | 16.4-03 manual backstop | D1 | In the note editor look at the field border unfocused and focused, tap Save and watch the icon, open the keyboard and scroll a long note. | The border changes on focus; the Save spinner cross-fades to the check without a jump; the field, counter and Save stay reachable with the keyboard up and the text scrolls. | Note. | [ ] |

### 2f-S Search tab and Explore See all (Phase 16.1)

Samsung = B (client). No money moves in these rows.

| Row ID | Source row ID | Direction | Steps | Expected | Evidence to capture | Result |
|---|---|---|---|---|---|---|
| D1.SE1 | PARITY #1, 16.1 UAT 1 | D1 | On the Search tab: first load; type a query; tap a chip; change the sort; open a result; press Back. | The first load shows 3 skeletons; typing waits about 300 ms before searching; a chip sends its slug; sort changes the order; a result opens Expert Profile with the bottom bar visible; Back returns to the same results. | Outcome per step. | [ ] |
| D1.SE2 | 16.1 UAT 2 | D1 | Open Search side by side with iOS SearchExpertView. | Spacing, the 80 dp avatar with its fallback, rating alignment, tag pills, glow and the pulsing empty illustration match iOS. | Note per element. | [ ] |
| D1.SE3 | D-01, 16.1 UAT 3 | D1 | Tap See all on both Explore headers. Type a query, switch tabs and return. Open a profile from results and re-tap the Search tab. | See all selects Search with no filter carried; the query and results survive a tab switch; re-tapping Search pops an open profile and keeps the query. | Outcome per step. | [ ] |
| D1.SE4 | 16.1 UAT 4 | D1 | Pull to refresh on results, on the empty state and on the error state, with and without airplane mode. | Rows stay during refresh; the snackbar has Retry and sits above the keyboard. | Outcome per state. | [ ] |
| D1.SE5 | 16.1 UAT 6 | D1 | Scroll the results, then change the query, a chip and the sort. Separately switch tabs and pull to refresh. Start a newer search while a snackbar shows. | A query, chip or sort change opens at the top; a tab switch and a pull keep the scroll position; a visible snackbar disappears when a newer search starts. | Outcome per step. | [ ] |

### 2f-P Payment History, notification preferences, Feedback and the splash (Phase 16.5)

Samsung = B (client). Only D1.PH4 touches money: use a paid booking already due to be voided under P0.9 and write its `PAID` line.

| Row ID | Source row ID | Direction | Steps | Expected | Evidence to capture | Result |
|---|---|---|---|---|---|---|
| D1.PH1 | PARITY #4, D-04 | D1 | Open Profile, Account, Payment History. Pull to refresh. Open a row and press Back. | A skeleton shows first, then rows newest first; a held paid booking reads "Authorized (not yet charged)" and a captured one "Paid"; amounts match the Stripe dashboard; pull to refresh keeps rows; Back keeps the scroll position. | Outcome per step; amounts read against Stripe (H10). | [ ] |
| D1.PH2 | CLIENT-TRANSACTIONS section 7 | D1 | On an account with over 20 rows scroll to the end. Repeat in airplane mode for the next page. If no account has 20 rows, mark the row [-] not run with the reason. | Pages load with no error footer at the end; in airplane mode the next page shows the footer and Retry works. | Outcome per step. | [ ] |
| D1.PH3 | D-03, UI-SPEC A8 | D1 | Tap a row for a live, a completed, a cancelled and a failed booking. | Details open with the real status; no enabled Join and no Reschedule on the last three, including inside the join window. | Outcome per status. | [ ] |
| D1.PH4 | D-03, 16.5-02 | D1 | Cancel a live paid booking from a Payment History row. | Bookings opens with the result notice; the Profile tab shows the hub, not Cancel; the hold is voided. | Outcome per step; PAID line (H9) and a Paid bookings created row. | [ ] |
| D1.PH5 | 16.5 review WR-01 | D1 | Open a Payment History row for a confirmed booking whose session has ended. If no account has one, mark the row [-] not run with the reason. | No Cancel Booking button. Reschedule may still show (no time gate on confirmed, 16.2 D-04). Do not tap anything that changes the booking. | Screenshot or note. | [ ] |
| D1.NS1 | PARITY #5, D-01 | D1 | Open Profile, Preferences, Notifications. Flip three rows quickly. Reopen the screen and compare with iOS. | Four sections and 13 rows, nothing about quiet hours; opening sends no PATCH (Railway logs, H10); the three flips send one PATCH; the bar shows a spinner then a check; reopening shows the values; iOS shows the same values. | Outcome per step; PATCH count from Railway logs. | [ ] |
| D1.NS2 | D-02 | D1 | In airplane mode flip a row. Reconnect and tap Retry. | An error icon and Retry show in the bar, the toggle stays, no dialog; after Retry the check shows. | Outcome per step. | [ ] |
| D1.NS3 | backend push gate | D1 | Turn off Booking Confirmations. A accepts a request from iOS. Turn the row back on. | No push arrives on the Samsung while the row is off. | Yes or no on the push; row restored. | [ ] |
| D1.FB1 | PARITY #6, D-05, D-06 | D1 | Open Profile, Support, Send Feedback. Enter 9 characters and tap Submit, then 10 characters and Submit. | The first server type is selected and the email is prefilled; 9 characters show the inline minimum-length error and send no request; 10 characters show the thanks dialog and OK returns to the hub; a backend row exists (H10). | Outcome per step; backend row yes or no. | [ ] |
| D1.FB2 | carried from iOS | D1 | In airplane mode tap Submit on a valid form. Reconnect and tap Submit. | The failure dialog shows and the form is kept; after reconnecting Submit works. | Outcome per step. | [ ] |
| D1.SP1 | PARITY #13, D-07, D-08, A4 | D1 | Force-stop the app and cold start it; repeat logged out. | The system splash is plain dark with no logo (note how long it feels on a cold start; iOS shows the logo and title at once, and Android 12+ cannot show text at this stage). It hands to the Compose splash with no color jump or flicker; the whole logo fades and scales, the 52 bold wordmark and tagline follow, the glow pulses; about 3 s later the right destination shows; logged out reaches Login; no notification prompt or suspended dialog shows over the splash. | Outcome per step. | [ ] |
| D1.SP2 | D-09, RESEARCH Question 2 | D1 | During a live call swipe the task away, then tap the ongoing-call notification. | The call shows above any splash and audio never stopped; the hold does not replay in the same process. | Outcome per step; H5 call proof lines. | [ ] |
| D1.SP3 | D-09 | D1 | With the app killed (H6) tap a booking push. | The full splash shows, then the right screen in the right tab; nothing is lost. | Outcome per step. | [ ] |
| D1.SP4 | A10, Pitfall 1 | D1 | Change the font size during the splash. Background and return. Repeat with animations off (Remove animations). | No replay on a font change; no replay on background and return; with animations off a static final frame shows for about 3 s; the app never sits on the OS splash. | Outcome per step. | [ ] |

### 2g Payouts mirror (D-05, iOS half)

| Row ID | Source row ID | Direction | Steps | Expected | Evidence to capture | Result |
|---|---|---|---|---|---|---|
| D1.30 | P14 (a) on iOS, P11 (a) mirror | D1 | On the iPhone as A open the payout onboarding link and return. Then B on Android looks at A's paid topic. | The link opens and returns (record what Stripe shows for an already-complete account, RESEARCH A4); A's paid topic is still bookable on Android. Never finish a new Connect account (D-05). | What Stripe showed, one line. | [ ] |

## 3. Direction switch

Log out on both phones and log in with the roles flipped, so accounts and directions switch once.

| Row ID | Source row ID | Direction | Steps | Expected | Evidence to capture | Result |
|---|---|---|---|---|---|---|
| SW.1 | P13 (a) | D1 | Log out on both phones. | B's android `device_tokens` row and A's ios row are gone. | Supabase `device_tokens` read. | [ ] |
| SW.2 | RESEARCH Pitfall 7 | D2 | Log in A on the Samsung and B on the iPhone. | A has a platform android row and B has a platform ios row. | Supabase `device_tokens` read. | [ ] |
| SW.3 | P11 (a) | D2 | A switches to Expert mode on the Samsung; B stays in client mode on iOS. | Samsung shows the expert Dashboard; iPhone shows the client home. | Screenshot kept outside the repo. | [ ] |
| SW.4 | D-08, H2 | D2 | Run H2 again. | Same versionCode and installer as the Direction 1 session. | H2 output. | [ ] |

## 4. Direction 2 (QA-03): iOS client B, Android expert A

Samsung = A (expert), iPhone = B (client). Rows run in order. Amounts follow P0.9. Every paid row writes a `PAID` line (H9) and a row in "Paid bookings created".

### 4a Requests

| Row ID | Source row ID | Direction | Steps | Expected | Evidence to capture | Result |
|---|---|---|---|---|---|---|
| D2.01 | P10 (d) | D2 | A edits availability on Android. B on iOS opens A's booking calendar. | The iOS client sees matching slots in A's timezone. | Slot times on both sides. | [ ] |
| D2.02 | P13 (b) killed, P11 (b) | D2 | Swipe ClickMe away (H6). B books A's free topic on iOS. Tap the request push on the Samsung (mode switch first when in Client mode). | The request push arrives on the killed Samsung and opens Request Decision in Expert mode. | H6 `pidof` and `dumpsys notification` output. | [ ] |
| D2.03 | P11 (a), P11 (b) | D2 | Dashboard pending count, Incoming Requests, accept with a message, Confirmed, Add to Calendar, Back, Back to Home. | The pending count is right; Confirmed shows; Add to Calendar opens the event editor; Back returns to Incoming Requests; Back to Home lands on Dashboard; iOS shows Confirmed. | Outcome per step. | [ ] |
| D2.04 | P11 (b) | D2 | B sends four more free requests. Decline once per reason, with and without a note. Open an expired request. Accept a request B cancelled meanwhile. | iOS sees each decline; an expired request opens read-only (`[-] not run: none exists` if so); the cancelled one shows ALREADY_PROCESSED. | Error code and outcome per request. | [ ] |

### 4b Call

| Row ID | Source row ID | Direction | Steps | Expected | Evidence to capture | Result |
|---|---|---|---|---|---|---|
| D2.05 | P11 (c), G-11-1, G-11-5, G-11-6 | D2 | Join from the Dashboard Today's Sessions card, an Upcoming card and Booking Summary with B on iOS. Test two-way audio, mute, speaker, leave and rejoin. | `remote joined elapsed=` and `remote audio state=2` in the log; two-way audio; the backend row has both joined_at values. | H5 log; Supabase booking row joined_at values. | [ ] |
| D2.06 | P9 SC-2 in D2 | D2 | Background the Android expert with the screen off for at least 5 minutes while talking. | Two-way audio throughout; notification shows title and chronometer. | H5 `dumpsys audio` and services output while backgrounded. | [ ] |
| D2.07 | P9 SC-4 in D2 | D2 | B hangs up on iOS and rejoins; stay to the final 5 minutes. | Waiting state, audio resumes after rejoin, the amber state and banner show on Android. | `ClickMeCall` lines in the H5 log. | [ ] |

### 4c Chat

| Row ID | Source row ID | Direction | Steps | Expected | Evidence to capture | Result |
|---|---|---|---|---|---|---|
| D2.08 | P12 (a), P12 (b) expert side | D2 | Message from Request Decision, the expert Booking Summary, an expert Upcoming card and the in-call Chat control (the call stays in the pill). Exchange messages with iOS. | Every entry lands on the thread; live both ways with iOS; Back returns to the list. | `Realtime` lines (ids only). | [ ] |

### 4d Paid

| Row ID | Source row ID | Direction | Steps | Expected | Evidence to capture | Result |
|---|---|---|---|---|---|---|
| D2.09 | P7 SC-2 on iOS, D-01 | D2 | B on iOS books A's paid topic within 6 days with a saved card. | The Android request shows the earnings card; Stripe shows requires_capture. PAID line, expected end void. | Stripe dashboard (PI last 6, amount); booking id first 8. | [ ] |
| D2.10 | P11 (d) | D2 | A cancels that held booking from Android. | iOS sees the cancellation; the Stripe PaymentIntent is canceled. Label void (no charge, no refund, no fee). | Stripe dashboard status canceled. | [ ] |
| D2.11 | P7 SC-4 on iOS, D-02 | D2 | B on iOS books paid with the 3DS card and passes the challenge. | The booking appears on Android as a request. Or `[-] not run` per P0.9. PAID line if made. | Stripe status; booking id first 8. | [ ] |
| D2.12 | P9 SC-5 in D2 | D2 | Paid session end to end with the iOS client and the Android expert. | Confirm-connection is sent once; Stripe shows capture on A's connected account. PAID line expected capture, refunded in Section 7. Or `[-] not run` when the P0.9 plan keeps one capture only. | Railway log line count; Stripe status. | [ ] |

### 4e Reschedule

| Row ID | Source row ID | Direction | Steps | Expected | Evidence to capture | Result |
|---|---|---|---|---|---|---|
| D2.13 | P11 (d) | D2 | B proposes a reschedule on iOS; A accepts one and declines another from Booking Summary. Then create two back-to-back bookings inside the 30-minute buffer. | Accept and decline both work; the second back-to-back booking fails with SLOT_UNAVAILABLE. | Error code and outcome. | [ ] |

### 4e-R Reschedule start (Phase 16.2)

Android expert A proposes; iOS client B responds. Paid rows end in a void (cancel), never a capture.

| Row ID | Source row ID | Direction | Steps | Expected | Evidence to capture | Result |
|---|---|---|---|---|---|---|
| D2.RS1 | PARITY #11, D-10 | D2 | A free booking whose Expert Booking Summary shows Reschedule (`can_reschedule` true). A proposes a new slot with a message. B accepts on iOS. | A is back on the summary with "Reschedule request sent" and "Awaiting client's response"; B accepts and the booking shows the new time on both phones. | Outcome per step; booking id first 8. | [ ] |
| D2.RS2 | PARITY #11, D-10 | D2 | From an expert upcoming card on a paid held booking inside 6 days, A proposes a new slot. B accepts on iOS, which triggers the iOS client-side saved-card recovery. Then A cancels the booking (void). | The proposal is sent from the card; record what the iOS accept does and any error code. PAID line, expected end void. | Error codes and outcome only; Stripe status canceled; booking id first 8. | [ ] |
| D2.RS3 | locked `can_reschedule` | D2 | Compare a booking where the server reports `can_reschedule` false (for example a completed past booking) with a confirmed upcoming one. | A's Reschedule button is absent on the first and present on the second. | Yes or no per booking. | [ ] |
| D2.RS4 | D-02 | D2 | On a paid booking A opens the reschedule calendar. | No dot and dimmed chips beyond now + 6 days, with the paid note. | Outcome one line. | [ ] |

### 4e-F Expert self-view reviews (Phase 16.3)

Samsung = A (expert). No money moves in this row.

| Row ID | Source row ID | Direction | Steps | Expected | Evidence to capture | Result |
|---|---|---|---|---|---|---|
| D2.FV1 | D-15 | D2 | A opens "View My Public Profile" from the expert hub. Tap "See all" on Recent Reviews. | The self-view shows no heart; Recent Reviews shows "See all"; it opens A's own full Reviews list with no bottom bar; Back returns to the profile. | Outcome per step. | [ ] |

### 4e-N Expert notes and Client Profile (Phase 16.4)

Samsung = A (expert). No money moves in these rows.

| Row ID | Source row ID | Direction | Steps | Expected | Evidence to capture | Result |
|---|---|---|---|---|---|---|
| D2.PN1 | PARITY #8, D-07, D-08 | D2 | A uses "Private Note" on a past card, the note zone on an upcoming card and "My Note" on Expert Booking Summary. | Each opens the editor; the saved note shows on return in each place. | Outcome per place. | [ ] |
| D2.PN2 | PARITY #7, #8, independence the other way | D2 | On one booking A writes an expert note on Android and B writes a client note on iOS. Each reads their own note. | Each reads only their own note; neither overwrites the other's. | Yes or no per side; never the note text. | [ ] |
| D2.CP1 | PARITY #9, D-09, D-10, D-11 | D2 | A taps "View Profile" on Expert Booking Summary. Pull to refresh. | Avatar, name, "Client" badge, About, "Your Sessions Together" and "Booking History" show in that order; a paid completed row shows A's net earnings, a free row shows no amount, a cancelled row shows no amount; rows are not tappable; pull to refresh works. | Outcome per element. | [ ] |
| D2.CP2 | D-13 | D2 | In an expert-mode chat thread with a client open the menu and tap "View profile". Press Back. | The menu shows "View profile"; it opens the same Client Profile; Back returns to the thread. | Outcome per step. | [ ] |
| D2.CP3 | D-14 | D2 | From an expert-mode chat with a person who has no booking with A tap "View profile". | "No bookings with this person yet" shows with no Retry and a working "Go back". | Outcome per step. | [ ] |
| D2.PN3 | 16.4-09 manual backstop | D2 | A booking push opens Expert Booking Summary on the Samsung (app killed, then backgrounded). Tap "My Note", save, press Back twice. | "My Note" opens the editor from the push-opened summary; the saved note shows on return; Back follows the order the push built. | Outcome per launch state. | [ ] |
| D2.CP4 | 16.4-09 manual backstop | D2 | From a booking event in an expert-mode chat thread open Expert Booking Summary, tap "View Profile", press Back twice. | Client Profile opens on the Chats stack; Back returns to the summary, then to the thread; the bottom bar stays hidden throughout. | Outcome per step. | [ ] |
| D2.CP5 | 16.4-08 manual backstop | D2 | On a Client Profile look at the avatar ring and glow and the Upcoming row border. Reach the friendly 403 and 404 states (a chat peer with no booking; a removed account if one exists) and pull to refresh on each. | The ring and static glow match the iOS profile; the Upcoming row shows its border; each friendly state refreshes on pull and keeps Go back. | Note per item. | [ ] |

### 4e-P Notification preferences, Feedback and View Past History, expert side (Phase 16.5)

Samsung = A (expert). No money moves in these rows.

| Row ID | Source row ID | Direction | Steps | Expected | Evidence to capture | Result |
|---|---|---|---|---|---|---|
| D2.NS1 | PARITY #5 | D2 | Open the expert hub, Preferences, Notifications. Flip a row. Open the same user on the iOS client account. | The same four sections show; the flip saves; iOS shows the value on its next load. | Outcome per step. | [ ] |
| D2.FB1 | PARITY #6, D-06 | D2 | Open the expert hub, Support, Send Feedback and send a valid message. Tap OK. | The message sends; OK returns to the expert hub, not the client hub. | Outcome per step; backend row yes or no. | [ ] |
| D2.PV1 | PARITY #12 | D2 | With no upcoming bookings tap View Past History. | The Past tab is selected. | Outcome. | [ ] |

### 4f Payouts (D-05, Android half)

| Row ID | Source row ID | Direction | Steps | Expected | Evidence to capture | Result |
|---|---|---|---|---|---|---|
| D2.14 | P14 (a), P11 (a) | D2 | Payouts, Set up payouts. The Stripe page opens in a Custom Tab; leave or finish. Repeat from the Dashboard card. | `clickme://connect/return` brings Payouts back with the status refreshed. If Chrome does not follow the backend 302 into the app (RESEARCH A1), the ON_RESUME refresh still reloads: record it; it is blocking (D-07) only if it prevents payout setup entirely. | What Stripe showed; whether the app returned by deep link or by resume. | [ ] |
| D2.15 | P14 (a) refresh | D2 | Open the link, wait more than 5 minutes (or reopen a used link) and act on the page. | `/refresh` arrives and a fresh link opens. | Outcome one line. | [ ] |
| D2.16 | P14 (a) forged | D2 | Run H8 (forged intent). | The app only reloads; nothing is marked complete. | H8 output and a screenshot kept outside the repo. | [ ] |

### 4g Account deletion (store-policy rows, D-07)

**Run the delete flow ONLY on the Throwaway account, never on A or B.**

| Row ID | Source row ID | Direction | Steps | Expected | Evidence to capture | Result |
|---|---|---|---|---|---|---|
| D2.17 | P4 SC-1 partial, P14 (d) | D2 | Log out A on the Samsung. Sign up the Throwaway on Android. Book A's free topic as Throwaway. Log back in as A and accept it. Log in as Throwaway again. | The Throwaway account exists with one upcoming booking. | Booking id first 8. | [ ] |
| D2.18 | P14 (d) 409 | D2 | As Throwaway with that upcoming booking, run Delete Account to the end. | The 409 "upcoming bookings" message appears and nothing is deleted. | Message text and code. | [ ] |
| D2.19 | P14 (d) | D2 | A cancels the Throwaway's booking. As Throwaway: Delete, Warning, Reason, Delete, Farewell, Back to Login. Then try to log in as Throwaway on the iPhone and on a fresh Android login. | The Farewell shows; the iPhone and a fresh Android login both reject the Throwaway. | Outcome per device. | [ ] |

### 4h Wrap up

| Row ID | Source row ID | Direction | Steps | Expected | Evidence to capture | Result |
|---|---|---|---|---|---|---|
| D2.20 | D-08, H2 | D2 | Log out the Throwaway on the Samsung and log A back in; run H2. | A is signed in; the build is unchanged. | H2 output. | [ ] |

## 5. Samsung tier-1 setting passes (D-06)

Every tier-1 row of `15-DEVICE-CHECKLIST.md`, grouped by the device setting so each setting is switched on and off once. Samsung = B client with the iPhone as A for rows that need a peer (Direction `D1`); other rows are `Samsung`. Row ID = setting prefix plus the checklist ID; Source row ID = the checklist ID, plus a WR id where a 15-REVIEW-FIX device check is attached. Switch the setting on at the start of each subsection and off at its end (E7.6 restores everything at the close).

### 5.1 TalkBack (TB.)

Switch on TalkBack. The SC-4 runs are TB.L1, TB.B1, TB.C1 and TB.K1 (15-UAT test 1). Where a row fails with a known cause, log a fix-if-cheap bug with the fallback named in its Expected cell.

| Row ID | Source row ID | Direction | Steps | Expected | Evidence to capture | Result |
|---|---|---|---|---|---|---|
| TB.L1 | L1, 15-UAT test 1 | Samsung | Log in using only TalkBack (SC-4 run). | Login reads title as heading, email, password, Log in; one "Don't have an account? Sign Up" node; Log in says "In progress" while busy; lands on the right home. | Pass or fail note in the results file. | [ ] |
| TB.L2 | L2 | Samsung | Enter a wrong password and submit. | The server message is spoken and focus lands on the password field. | Note. | [ ] |
| TB.L3 | L3, WR-A03 | Samsung | On Login with email and password filled, listen to each field; also check one field on Signup and one on Settings. | TalkBack speaks the field name together with its value. If it fails, log fix-if-cheap with this fallback: add the name to the field semantics without hiding the value. | Note and which field. | [ ] |
| TB.L7 | L7 | Samsung | Complete a password reset to Password Changed. | Focus lands on the "Password Changed" headline first; reading order sensible; Go to Login shows Login with the email pre-filled and an empty password. | Note. | [ ] |
| TB.B1 | B1, 15-UAT test 1 | D1 | Book a free session using only TalkBack from Expert Profile to Request Sent (peer: iPhone A receives it; a person completes every step). | The whole booking completes by TalkBack alone. | Note. | [ ] |
| TB.B3 | B3, WR-A04 | Samsung | On the booking form, move through the calendar and slots. Choose a different day with the same slot count. | Day cells read "Today, Selected, Available" or "No availability"; the month title is read before Previous and Next month; choosing a day announces how many times are free; the slots heading re-announces on a day change (WR-A04); Book Now says why it is disabled and "In progress" while busy. | Note. | [ ] |
| TB.B4 | B4 | Samsung | Tap day cells on the 360 dp phone (about 45.7 dp wide). | Taps are reliable. If taps miss, log fix-if-cheap with this fallback: reduce the calendar card padding. | Note. | [ ] |
| TB.B5 | B5 | Samsung | Open the topic sheet; close it; force a failed topic load. | Topic cards read as a radio group; closing returns focus to the Topic row; the failed load shows the named error and Retry inside the sheet. | Note. | [ ] |
| TB.B8 | B8 | Samsung | Enter Request Sent. | TalkBack speaks "Request Sent!" first. | Note. | [ ] |
| TB.C1 | C1, 15-UAT test 1 | D1 | Join a call from Upcoming Details using TalkBack (peer: iPhone A joins; SC-4 run). | The status line is read first; the order is status, name, timer, Mute, Speaker, Chat, Minimize, End call; "Connecting" is not announced twice. | Note; H5 log. | [ ] |
| TB.C2 | C2 | D1 | During an active call, move across screens and tabs underneath. | The pill is the first TalkBack node on every screen. If it fails, log fix-if-cheap with this fallback: a focus request on the pill when a screen appears during a live call. | Note. | [ ] |
| TB.C7 | C7 | D1 | After the force-stop and relaunch in PD.C7, listen to the booking detail. | TalkBack announces the one-time "You were on a call" note. | Note. | [ ] |
| TB.K1 | K1, 15-UAT test 1 | D1 | Send a chat message using only TalkBack (peer: iPhone A; SC-4 run). | Conversations row, thread reading order, "You: {text}, {time}, Sent" or "Read"; the composer explains why Send is disabled; focus and the keyboard stay after Send. | Note. | [ ] |
| TB.K2 | K2 | D1 | Focus a Conversations row. | One stop: "{name}, {last message}, {time}, {n} unread", child text not read again. | Note. | [ ] |
| TB.K3 | K3 | D1 | Listen to a message bubble. | The bubble is not spoken twice. If it fails, log fix-if-cheap with this fallback: drop the Text from the merged bubble node. | Note. | [ ] |
| TB.K4 | K4, WR-B02 | D1 | Peer (iPhone A) sends the same text twice; delete a failed send of mine. | A peer message arriving is announced once; identical back-to-back texts are re-announced; a newest own message from history is not announced on first composition; deleting a failed send of mine does not re-speak the peer message (WR-B02). | Note. | [ ] |
| TB.K5 | K5 | D1 | On a failed bubble and the Report reason group, use TalkBack. | A failed bubble offers Retry and Delete; the Report reason group reads as a group. | Note. | [ ] |
| TB.K10 | K10 | D1 | Open a blocked thread. | The blocked banner is announced. | Note. | [ ] |
| TB.K11 | K11, WR-B01 | D1 | Network off, pull to refresh in Conversations and in a thread; toggle airplane mode inside a thread. | Rows and messages stay; one "Retry" snackbar; the reconnect failure path shows; the snackbar sits above the keyboard and the composer (WR-B01). | Note. | [ ] |
| TB.Y12 | Y12 | Samsung | Run Accessibility Scanner over the four SC-4 flows (log in, book, call, chat). | Touch-target, contrast and label findings are logged as bug rows. | Finding count; screenshots outside the repo. | [ ] |
| TB.Y14 | Y14 | Samsung | Trigger a "Retry" snackbar with TalkBack on. | The snackbar stays long enough to be reached and activated. | Note. | [ ] |
| TB.SE1 | 16.1 UAT 5 | D1 | With TalkBack on, use the search field with text, the chips, the sort button and a result card. | Typed text is read back, chips announce selection, sort announces its value, a card reads as one button. | Note. | [ ] |
| TB.PF1 | 16.5 UI-SPEC Accessibility Contract | D1 | With TalkBack on, open Payment History, Notifications, a save error, Send Feedback and the splash. | A Payment History row reads as one button with "Open booking"; a notification row reads once as a switch with title, subtitle and state; the save error is announced; Feedback labels precede their fields; the splash is one node announced once. | Note. | [ ] |

Switch TalkBack off.

### 5.2 Font 200% (FS.)

Switch on Settings, Display, Font size at the largest, then Display size at the largest.

| Row ID | Source row ID | Direction | Steps | Expected | Evidence to capture | Result |
|---|---|---|---|---|---|---|
| FS.L4 | L4 | Samsung | Login on the small phone at 200% font with the keyboard. | Nothing clips; IME Next and Done move email to password to submit; autofill offers the saved login; Log in stays reachable above the keyboard. | Note. | [ ] |
| FS.L5 | L5 | Samsung | Open Login, Forgot Password, Reset Code, Set New Password and Password Changed at 200% with the keyboard open. | Text wraps rather than clips. | Note. | [ ] |
| FS.B2 | B2 | Samsung | Open Make a Booking at 200%. | Topic row, calendar, slots and notes wrap; Book Now and its reason stay reachable. | Note. | [ ] |
| FS.B10 | B10 (re-scoped: live Payment Methods, its info sheet and a paid Request Sent, no sandbox) | D1 | Open Payment Methods and its info sheet; view the paid Request Sent from D1.18 at 200%. | All three reflow with no clipped text. | Note. | [ ] |
| FS.C3 | C3 | D1 | In a call at 200%, view the Connected pair, the final-5-minute banner and the Skype or Zoom external variant. | Names wrap to two lines; Mute, Speaker and End call never leave the screen. | Note. | [ ] |
| FS.K6 | K6 | D1 | At 200% open Conversations, a thread with a failed bubble, the blocked banner, Report & Block, the Report sheet and the Request Decision footer. | Row name and time wrap; bubbles and composer grow; footer buttons stack. | Note. | [ ] |
| FS.Y7 | Y7 | Samsung | Change the font scale while on a form in each flow. | Typed text kept, secrets empty, no crash. | Note; H4 crash count. | [ ] |
| FS.Y12 | Y12 | Samsung | Run Accessibility Scanner at 200% on the four SC-4 flows. | Findings logged. | Finding count. | [ ] |
| FS.FV1 | 16.3 UI-SPEC E3 backstop | D1 | At 200% open Profile, Saved Experts. | The Favorite Experts app bar title stays on one line with an ellipsis. | Note. | [ ] |
| FS.FV2 | 16.3 UI-SPEC E4 backstop | D1 | At 200% view a Saved Experts card with a long name and several tags. | The card grows with no clipping and the photo stays 72 x 80 dp. | Note. | [ ] |
| FS.FV3 | 16.3 UI-SPEC E5 backstop | D1 | At 200% tap the heart on a Saved Experts card whose expert has a long name. | The remove dialog title wraps without clipping and both buttons stay reachable. | Note. | [ ] |
| FS.PN1 | 16.4 D-02 | Samsung | At 200% open the note editor with the keyboard up. Also open Booking Summary as B and as A with a saved note. | The header, the field and the counter stay reachable and nothing clips; the Save action stays reachable; the My Note card wraps without clipping on both summaries. | Note. | [ ] |
| FS.CP1 | 16.4 D-09 | D2 | At 200% open a Client Profile with a long topic. Also view the View Profile link on Expert Booking Summary. | Stat cells wrap, the amount stays visible and nothing clips; the View Profile link wraps or stays one line without clipping and keeps a 48 dp target. | Note. | [ ] |
| FS.PH1 | 16.5 UI-SPEC E2 backstop | D1 | At 200% open Payment History with a row that has a long topic, a long expert name and "Authorized (not yet charged)". | The row wraps and the amount stays visible. | Note. | [ ] |
| FS.NS1 | 16.5 UI-SPEC E4 backstop | D1 | At 200% open Notifications. | Every notification title and subtitle wraps, the switch stays visible, and the row is one 48 dp target. | Note. | [ ] |
| FS.FB1 | 16.5 UI-SPEC E8 backstop | D1 | At 200% trigger the Feedback failure dialog with a long server message. | The dialog stays readable and OK is reachable. | Note. | [ ] |
| FS.SP1 | 16.5 UI-SPEC E9 backstop | Samsung | At 200% cold start the app on the Samsung. Then check landscape on the resizable emulator (the phone is portrait-locked). | The splash's mark, wordmark and tagline stay visible and centered on the Samsung and in landscape on the emulator. | Note. | [ ] |

Switch the font and display size back.

### 5.3 Process death and recreation (PD.)

Switch on Developer options, Don't keep activities. PD.C7 and PD.C8 carry the 15-REVIEW-FIX checks WR-A01 and WR-A02.

| Row ID | Source row ID | Direction | Steps | Expected | Evidence to capture | Result |
|---|---|---|---|---|---|---|
| PD.L8 | L8 | Samsung | On Forgot and Set New Password toggle dark mode, then background and return. | The typed email stays; the reset code and both passwords come back empty with no error text. | Note. | [ ] |
| PD.B5 | B5 | Samsung | Open the topic sheet, change configuration, background and return. | The open sheet survives. | Note. | [ ] |
| PD.B7 | B7 | Samsung | Background mid-booking and return. | Topic, day, month, slot, meeting type and notes kept; the Request Sent animation does not replay; system Back during an in-flight booking is ignored. | Note. | [ ] |
| PD.C6 | C6 | D1 | Dark mode toggle, font-scale change and Don't keep activities during a call. | The call continues in the pill or layer; nothing joins a second time or opens the mic by itself. | H5 log (one join only). | [ ] |
| PD.C7 | C7, WR-A01, WR-A02 | D1 | Start an in-app call, run `am force-stop` (not `am kill`), then tap a push before reopening the app. Separately, force-stop and relaunch directly. | The push destination wins and the "You were on a call" note does not cover it (WR-A01 and WR-A02); a direct relaunch shows the booking detail with Join Call, the one-time note, no auto-join and no microphone indicator; reopening does not show the note again. | H5 log; `pidof` after force-stop. | [ ] |
| PD.C8 | C8, WR-A01, WR-A02 | D1 | With a call running, swipe the task away and tap the ongoing-call notification. | The app starts through Splash and auth, then lands in the right place; a background FCM wake-up that dies before UI keeps the marker. | Note. | [ ] |
| PD.K8 | K8 | D1 | Type an unsent message in a thread, background and return. | The draft is back in the same thread and the keyboard does not reopen. | Note. | [ ] |
| PD.K9 | K9 | D1 | Fill the Report sheet, background and return. | Reason and details are restored. | Note. | [ ] |
| PD.Y1 | Y1 | Samsung | Rotate the Samsung through the app, including during a call. | It stays portrait on every screen. | Note. | [ ] |
| PD.Y3 | Y3 | Samsung | Walk the whole signup (as the Throwaway in D2.17 or a second throwaway), backgrounding at each step. | Same step and typed text kept; Verify Email returns with an empty code; password fields empty. | Note. | [ ] |
| PD.Y4 | Y4 | Samsung | Walk Expert Profile to Request Sent. | Selections kept (specifics in PD.B7). | Note. | [ ] |
| PD.Y5 | Y5 | D1 | Walk Conversations to a thread, type, background and return. | Draft kept (specifics in PD.K8). | Note. | [ ] |
| PD.Y6 | Y6 | D1 | Join a call, background and return; then the force-stop case in PD.C7. | Nothing re-joins. | H5 log. | [ ] |
| PD.Y7 | Y7 | Samsung | Toggle dark mode while on a form in each flow. | Typed text kept, secrets empty, no crash. | Note; H4 crash count. | [ ] |
| PD.Y11 | Y11 | Samsung | On the Play build, log in, then from a deep screen (booking detail, a chat thread) background and return. Confirm login and the realtime connection work under R8. | The deep screen is restored. If it lands on the start screen, log a bug (add a NavKey keep rule). | Note. | [ ] |

Switch Don't keep activities off.

### 5.4 IME and insets (IM.)

| Row ID | Source row ID | Direction | Steps | Expected | Evidence to capture | Result |
|---|---|---|---|---|---|---|
| IM.L2 | L2 | Samsung | Wrong password with the keyboard open. | Focus lands on the password field above the keyboard. | Note. | [ ] |
| IM.L4 | L4 | Samsung | Login with the keyboard open. | Log in stays reachable above the open keyboard. | Note. | [ ] |
| IM.L5 | L5 | Samsung | The four recovery screens with the keyboard open. | Nothing clips. | Note. | [ ] |
| IM.L6 | L6 | Samsung | Reset code with SMS or OTP autofill; submit with errors. | The 6-digit code autofills; focus moves to the first failing field. | Note. | [ ] |
| IM.B6 | B6 | Samsung | Type in the booking Notes field with the real keyboard. | Caret stays visible; tapping outside clears focus; a drag dismisses the keyboard; Book Now can be scrolled above the keyboard. | Note. | [ ] |
| IM.C5 | C5 | D1 | During a call, switch between gesture and 3-button navigation. | The pill sits correctly over both and clears a side display cutout. | Note. | [ ] |
| IM.K7 | K7, WR-B01 | D1 | Open a thread, show the keyboard, trigger a failed refresh. | The IME pushes the thread up with the cutout and 3-button navigation; the cursor is stable; the long-press Delete menu works; the failed refresh snackbar sits above the keyboard and the composer (WR-B01). | Note. | [ ] |
| IM.K8 | K8 | D1 | Type an unsent message and background (Don't keep activities is off now). | The draft is back and the keyboard does not reopen. | Note. | [ ] |
| IM.K9 | K9 | D1 | Open the Report sheet with the real keyboard. | The input stays visible. | Note. | [ ] |
| IM.Y8 | Y8 | Samsung | Keyboard open over the Samsung cutout. | No text or control sits under the status bar, a side cutout or the keyboard. | Note. | [ ] |
| IM.Y9 | Y9 | Samsung | Switch between gesture and 3-button navigation. | Snackbars, sticky footers and the call pill clear the bar in both. | Note. | [ ] |
| IM.Y16 | Y16 | Samsung | Walk the four SC-4 flows. | No text or control sits under the status bar, a side cutout or the navigation bar. | Note. | [ ] |

### 5.5 Plain (PL.)

| Row ID | Source row ID | Direction | Steps | Expected | Evidence to capture | Result |
|---|---|---|---|---|---|---|
| PL.C9 | C9 | D1 | Deny POST_NOTIFICATIONS on API 33+, then join a call. | The call still runs and the pill remains. | Note; H5 log. | [ ] |
| PL.K12 | K12 | D1 | Use the jump-to-latest control and watch the list move-to-top animation. | They feel right. | Note. | [ ] |

### 5.6 Emulator (EM., D-10)

| Row ID | Source row ID | Direction | Steps | Expected | Evidence to capture | Result |
|---|---|---|---|---|---|---|
| EM.1 | 09-HUMAN-UAT 3, P9 Android 17 | emulator | On `Medium_Phone_API_37.0` run `adb shell cmd audio set-enable-hardening enable`, join a call and background it. | Audio and focus continue; the targetSdk 37 local-network permission does not block Agora. | The adb output and call log lines. | [ ] |

## 6. Run if time allows (triaged by D-07, not blocking)

Large-window, tier 2-4 and other deferred rows. Failures are triaged by D-07 and do not block automatically. Rows left `[ ]` get "not run in Phase 16: post-release" at close-out. Compact form: the Source row ID names the original checklist or STATE item; the Check is one line.

### 6.1 Large window (emulator, outside the D-06 tier-1 variants)

Resizable AVD from the android-37.0 image (RESEARCH Open Question 3); confirm scope with the user at sign-off.

| Row ID | Source row ID | Check (one line) | Result |
|---|---|---|---|
| LW.L9 | L9 | Login and the four recovery screens are centered at 600 dp, app bars span the window, nothing clipped. | [ ] |
| LW.B9 | B9 | Make a Booking and Request Sent capped at 600 dp; resizing mid-booking keeps every selection and the notes. | [ ] |
| LW.C4 | C4 | At 900x400 Mute, Speaker and End call show without scrolling; a narrow short window scrolls; phone layout unchanged. | [ ] |
| LW.C10 | C10 | Resize the window during a call: no clipped controls, call state kept. | [ ] |
| LW.K13 | K13 | Conversations rows and Report & Block capped at 600 dp. | [ ] |
| LW.Y2 | Y2 | Resize through phone, 900x400 and 900 dp square: content never clipped, input kept, max width 600 dp. | [ ] |

### 6.2 Tier 2-4 checklist rows

| Row ID | Source row ID | Check (one line) | Result |
|---|---|---|---|
| T2.S1 | S1 | All 12 signup surfaces at 200% font with the keyboard open: no clipped text, buttons stack. | [ ] |
| T2.S2 | S2 | Country, language and expertise search sheets stay visible above the keyboard and the list scrolls. | [ ] |
| T2.S3 | S3 | TalkBack on the Profile Photo crop offers and performs the custom zoom and move actions. | [ ] |
| T2.S4 | S4 | Don't keep activities at each signup step: same step, typed text kept, Verify Email code empty. | [ ] |
| T2.S5 | S5 | Signup screens capped at 600 dp on a large window; app bars full width. | [ ] |
| T2.S6 | S6 | Review and Overview announce in-flight and error states; Publish reachable; retry works. | [ ] |
| T2.E1 | E1 | Explore, the View all sheet and Expert Profile at 200%: nothing clips, Book Session reachable. | [ ] |
| T2.E2 | E2 | Category sheet search and results stay usable with the keyboard open. | [ ] |
| T2.E3 | E3 | Pull to refresh feels normal; a failed refresh keeps all sections and shows one Retry snackbar. | [ ] |
| T2.E4 | E4 | With no categories the copy reads "No categories yet" and "Check back soon."; decide if the copy is right. | [ ] |
| T2.E5 | E5 | Expert Profile with TalkBack: "Book Session with {name}", sections in order, headings navigate. | [ ] |
| T2.E6 | E6 | Expert Profile on a large window: hero glow full width, sections capped at 600 dp. | [ ] |
| T2.M1 | M1 | My Bookings, Upcoming Details with a reschedule card, Cancel step 2 and Booking Summary at 200%. | [ ] |
| T2.M2 | M2 | Failed refresh shows exactly one "Couldn't refresh" snackbar with Retry; TalkBack announces it. | [ ] |
| T2.M3 | M3 | ErrorState and EmptyState headings navigate as headings and announce once; long headings wrap at 200%. | [ ] |
| T2.M4 | M4 | Kill the process on Cancel confirm and reopen: the confirm step shows and nothing cancels until Confirm. | [ ] |
| T2.M5 | M5 | A JPY booking cancel shows the refund snackbar with no decimals (no JPY topic in production: likely not run). | [ ] |
| T2.M6 | M6 | Decline-reschedule sheet keeps the reason, counter and Decline button visible with the keyboard; text survives a kill. | [ ] |
| T2.M7 | M7 | My Bookings, Upcoming Details, Booking Summary and Cancel capped at 600 dp on a large window. | [ ] |
| T2.M8 | M8 | After Retry succeeds on a detail screen, note where TalkBack focus lands (not implemented, log only). | [ ] |
| T2.N1 | N1 | Notifications list, Write Review, Booking Summary with Feedback and the Past card pill at 200%. | [ ] |
| T2.N2 | N2 | TalkBack reads the star group, the nested Past card action and the notification rows correctly. | [ ] |
| T2.N3 | N3 | Offline foreground push on Notifications: rows stay, one Retry snackbar, Retry reloads. | [ ] |
| T2.N4 | N4 | Notifications and Write Review on a large window: centered 600 dp column, footer background spans. | [ ] |
| T2.X1 | X1 | Topics, Topic Editor with an error, an overlapping day banner and the Time Range sheet at 200%. | [ ] |
| T2.X2 | X2 | TalkBack reads the Time Range sheet; the sheet keeps state after recreation and process death. | [ ] |
| T2.X3 | X3 | Topic Editor focus moves to the first invalid field; description stays above the keyboard; error survives rotation-equivalent. | [ ] |
| T2.X4 | X4 | Dashboard, request card, Request Decision, Decline sheet, Confirmed, Upcoming card and Cancel step 2 at 200%. | [ ] |
| T2.X5 | X5 | TalkBack on the Decline and Accept sheets; Booking Confirmed starts on its headline. | [ ] |
| T2.X6 | X6 | Tab return and realtime events reload without flicker; Confirmed with animations off shows its final state. | [ ] |
| T2.X7 | X7 | Decline and Accept sheets survive Don't keep activities with text kept and nothing sent. | [ ] |
| T2.X8 | X8 | Offline refresh on Dashboard, Bookings, Requests, Topics and Availability: one Retry snackbar each, above the bar and keyboard. | [ ] |
| T2.X9 | X9 | Expert screens capped and centered at 600 dp with full-width app bars on a large window. | [ ] |
| T2.T1 | T1 | Both hubs, settings, Account Security, Delete Account, Help, Payouts, Activity and Tax at 200%. | [ ] |
| T2.T2 | T2 | TalkBack announces the save status, password toggle state, FAQ state and selected delete reason. | [ ] |
| T2.T3 | T3 | The four forms survive Don't keep activities; TIN, account identifier and deletion password come back empty. | [ ] |
| T2.T4 | T4 | The Tax saved snackbar sits above the gesture bar and the keyboard. | [ ] |
| T2.T5 | T5 | Offline pull to refresh on Payouts, Activity and Help: content stays, one Retry snackbar. | [ ] |
| T2.T6 | T6 | A wrong current password keeps the cursor and keyboard; a server error dialog keeps all typed values. | [ ] |
| T2.T7 | T7 | The Payment Methods loading skeleton matches the loaded rows without a jump. | [ ] |
| T2.T8 | T8 | After deleting the Throwaway, TalkBack announces the Farewell headline first. | [ ] |
| T2.T9 | T9 | Log out and Delete dialogs survive Don't keep activities and never auto-confirm. | [ ] |
| T2.T10 | T10 | Settings screens centered at 600 dp with full-width app bars on a large window. | [ ] |
| T2.Y10 | Y10 | With animation scales off, Request Sent, Booking Confirmed and sheets show their final state at once. | [ ] |
| T2.Y13 | Y13 | On a throttled network, skeleton to content and error to retried content show no jump or flicker. | [ ] |
| T2.Y15 | Y15 | After Don't keep activities, scroll position is restored where data is available (best effort; note regressions). | [ ] |

### 6.3 Other deferred STATE items not in the matrix

| Row ID | Source row ID | Check (one line) | Result |
|---|---|---|---|
| OT.P3-2 | P3 SC-2 | Recovery flow on the Play build: Forgot Password to Password Changed, Back behavior, autofill. | [ ] |
| OT.P3-4 | P3 SC-4 | Airplane mode 10 s then off: `Realtime` returns to Connected within about 2 s of the network. | [ ] |
| OT.P4-1 | P4 SC-1 | Full signup with a new email (no username or phone) through Publish to the client home. | [ ] |
| OT.P4-2 | P4 SC-2 | Verify Email sends nothing on open; Resend disabled for 30 s; wrong code shows the inline error. | [ ] |
| OT.P4-3 | P4 SC-3 | Quit mid-setup and relaunch to the hub with completed steps checked. | [ ] |
| OT.P4-4 | P4 SC-4 | After publish: carousel shows once; mode switch to Expert and back keeps mode after relaunch. | [ ] |
| OT.P5-1 | P5 SC-1 | Explore hero, Trending Categories, Featured Topics, Today's Sessions, chip filter and View all. | [ ] |
| OT.P5-2 | P5 SC-2 | Category cache: View all shows from cache in airplane mode after a relaunch. | [ ] |
| OT.P5-3 | P5 SC-3 | Expert Profile hero, sections, Book Session and the unavailable-profile message. | [ ] |
| OT.P10-a | P10 (a) | Topics: create, edit, delete a paid topic; price below the floor shows the live error. | [ ] |
| OT.P10-b | P10 (b) | Availability: overlapping slots show the banner and disable Save; Save during a call is refused. | [ ] |
| OT.P10-c | P10 (c) | Timezone: a UTC-zone expert on a non-UTC device sees the device zone filled and stored. | [ ] |
| OT.P10-e | P10 (e) | French (Canada) locale: typing "12,50" saves 1250 minor units. | [ ] |
| OT.P10-g | P10 (g) | Edit Expertise offline shows the load error; a topic with 45 or 600 minutes keeps its duration after a title edit. | [ ] |
| OT.P14-b | P14 (b) | Activity amounts match the ledger, statuses, paging past 20 rows, preview and See all. | [ ] |
| OT.P14-c | P14 (c) | Hubs and settings auto-save; avatar pick and crop; languages and country persist; change password. | [ ] |
| OT.P14-e | P14 (e) | Tax & Payout Info saves without and with a TIN. | [ ] |
| OT.PP | P4 visual bullet, P14 (c) | Photo picker and avatar crop on API 26-32. `[-] not run: no API 26-32 AVD`. | [-] not run: no API 26-32 AVD |

## 7. End of run: money sweep and cleanup

Refund and hold checklist, run after the last row. Paid cancel, decline and expiry rows are voids (no charge, no refund, no fee); only completed paid sessions are captured, and those are refunded by hand (D-04).

| Row ID | Source row ID | Direction | Steps | Expected | Evidence to capture | Result |
|---|---|---|---|---|---|---|
| E7.1 | D-04 | D1 | Open "Paid bookings created" and fill the Actual end of every row. | Every row has an actual end state. | The completed table. | [ ] |
| E7.2 | D-04, RESEARCH Pitfall 2 | D1 | In the app, cancel or decline every leftover paid request (holds last up to about 7 days). | No paid booking is left in pending approval. | Booking ids first 8. | [ ] |
| E7.3 | D-04, H10 | D1 | Open the Stripe dashboard on A's connected account (live mode). | No matrix PaymentIntent is in requires_capture. | Count of requires_capture matrix PIs (expect 0). | [ ] |
| E7.4 | D-04, RESEARCH A3 | D1 | Refund each captured matrix charge in full in the dashboard on the connected account. | Each refund shows succeeded; Stripe keeps its processing fee; the application fee line was refunded too (RESEARCH A3) or the gap is noted. | Refund date per PI last 6, written in the Paid bookings table. | [ ] |
| E7.5 | P14 (d), D-03 | D1 | Check the accounts. | The Throwaway is deleted; A and B are active; B's saved live cards are kept or removed as the user prefers. | Confirmed aloud. | [ ] |
| E7.6 | D-13 | Samsung | Put the Samsung settings back to normal. | Font and display size default, TalkBack off, Don't keep activities off, animation scales on, navigation mode as before. | Confirmed aloud. | [ ] |
| E7.7 | D-04, T-16-02 | D1 | Restate the money rule. | Never run `scripts/live-cutover-cleanup.js`; never use a bulk refund script; refunds go through the Stripe dashboard. | None. | [ ] |

## Paid bookings created (D-04)

Rows are added from the `PAID` lines of each results file, at the moment the booking is made. Only completed sessions are captured; every other paid booking ends as a void.

| # | Row | Direction | Booking (first 8 of id) | PaymentIntent (last 6) | Amount | Expected end (void or capture) | Actual end | Refunded (date) |
|---|---|---|---|---|---|---|---|---|

## 8. Sign-off (D-09)

A fill-in template. Claude fills the counts at close-out; the user confirms. Phase 16 cannot close until the confirmation line below is filled (D-09).

- **Direction 1 (QA-02) counts:** pass ___ / fail ___ / not run ___
- **Direction 2 (QA-03) counts:** pass ___ / fail ___ / not run ___
- **Tier-1 counts (Section 5):** pass ___ / fail ___ / not run ___
- **Open blocking bugs (must be 0):** ___
- **3DS outcome (D-02):** a real live 3DS challenge passed in D1 and in D2, or not run with the reason: ___
- **Money result (D-04):** every hold voided ___, every capture refunded ___, Section 7 ticked ___
- **Accepted gaps (D-10):**
  - The user is the only internal tester, so the SC-5 and REL-05 "few real users on a mix of devices" part is accepted at release.
  - JPY is not runnable (no JPY topic in production).
  - Large-window rows are outside D-06 (Section 6.1).
  - Tier 2-4 rows not run (Section 6.2 and 6.3).
  - Photo picker on API 26-32 (no AVD).

Write-back table (left empty; close-out fills it so every deferred STATE item and checklist row has a result):

| Source item | Runbook rows | Result |
|---|---|---|

Confirmed by the user on YYYY-MM-DD: option ___ (accept the matrix and tier-1 results / accept with the listed gaps / do not accept, reason: ___)
