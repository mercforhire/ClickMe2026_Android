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
| preflight | 2026-11-02 | versionCode=3 | com.android.vending | 8d4450f | 1868 | 13 | 0 |
| session-A | 2026-11-02 to 2026-11-03 | versionCode=3 | com.android.vending | 8d4450f | 1868, 2525, 2827, 3079, 3080, 3242 | 53 | 4 |
| batch-A | 2026-11-03 | versionCode=4 | com.android.vending | 2ec82af | 4076 | 10 | 0 |
| session-B | 2026-11-03 to 2026-11-05 | versionCode=4 | com.android.vending | 2ec82af | 4076 | 47 | 2 |
| batch-B | 2026-11-05 | versionCode=5 | com.android.vending | 2ec82af (backend repo HEAD, /health not queried) | 4735, 8048 | 42 (3 fix re-tests, 5 smoke, 34 settings rows) | 1 (FS.C3, BUG-34) |
| close-out | 2026-11-05 | versionCode=5 | com.android.vending | 2ec82af | 8048 | 0 | 0 |

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
| P0.1 | D-03, P13 (a) | D1 | Account A signed in on the iPhone in expert mode; account B signed in on the Samsung in client mode. Choose (do not create yet) the Throwaway email. | Neither account is the store-review account; roles A and B each map to one real account of the user. | User confirms aloud; role map noted in the results file without emails or passwords. | [x] pass (vc 3, 2026-11-02) |
| P0.2 | RESEARCH Pitfall 1, WR-09 | D1 | A opens the Topic Editor on iOS or Android and reads the minimum the price field accepts. A has one paid 30-minute topic at that floor, one free 30-minute topic and one free 90-minute topic. | The floor reads CA$5.00 (A1); the three topics exist and are bookable. If the floor differs, P0.9 uses the live floor. | The floor value and the three topic titles. | [x] pass (vc 3, 2026-11-02) |
| P0.3 | RESEARCH Pitfall 9 | D1 | A's availability covers today and the next 6 days with slots in the next hours. | Paid bookings are possible up to 6 days ahead; at least one slot sits inside the join window for call rows. | The slot times seen from B on the Samsung. | [x] pass (vc 3, 2026-11-02) |
| P0.4 | D-02, RESEARCH A2 | D1 | On the Samsung as B: Payment Methods, Add Payment Method with the main live card, then with the 3DS candidate card and the decline candidate if the user has them. No charge is made (SetupIntent). Limit: 10 per 15 minutes per user. | Per card: saved, and whether a bank challenge appeared during setup. | Brand and last 4 only, challenge yes or no, per card. | [x] pass (vc 3, 2026-11-02) |
| P0.5 | RESEARCH A10, P13 (b) | D1 | B sends A a chat message from the Samsung. | The iPhone shows a push. If not, log owner ios and use realtime screens for iOS-side push rows. | Yes or no on the iPhone push; screenshot stays outside the repo. | [x] pass (vc 3, 2026-11-02) |
| P0.6 | RESEARCH Pitfall 7, P13 (a) | D1 | Supabase `device_tokens`: check A and B. | A has one row with platform ios; B has one row with platform android. | The two platform values read from the table. | [x] pass (vc 3, 2026-11-02) |
| P0.7 | RESEARCH Pitfall 10, 09-HUMAN-UAT 7 | D1 | Record the Samsung battery state (H7) before any call row; run with defaults (the real-user case). | ClickMe is not in Sleeping apps or Deep sleeping apps; bucket recorded. | The H7 outputs in `logs/preflight/P0.7.log`. | [x] pass (vc 3, 2026-11-02) |
| P0.8 | D-14, H10 | D1 | Confirm the user can open the Stripe dashboard for A's connected account in live mode, Railway logs and the Supabase table editor, or will read values aloud. | Each of the three is reachable, or the read-aloud fallback is agreed. | Which of the three the user can open. | [-] not run: skipped by the user 2026-11-02, who states access works; A Connect status not read |
| P0.9 | D-01, D-02, D-04 | D1 | Money plan: option-a; floor CA$5.00 (500 minor units, read at P0.2 2026-11-02); captures 2 (D1.25 and D2.12 only, each refunded by hand in Section 7); every other paid row is a void (cancel, decline, expiry); 3DS route: accept not run (user changed this 2026-11-02 from own challenging card after the Mastercard 2018 showed no setup challenge), so D1.21 and D2.11 are not run unless a live challenge appears; D1.22 not run, no failing card. Plan first confirmed 2026-10-30. | A written money plan the user confirmed before any charge. | The plan text in this cell. | [x] pass (vc 3, 2026-11-02) |
| P0.10 | D-03, P14 (d) | D2 | Throwaway plan for the deletion rows (D2.17, D2.18): a real email the user controls, signed up on Android in Direction 2. | The email exists and the user can read its inbox for the verification code. | Confirmed aloud; no address written here. | [x] pass (vc 3, 2026-11-02) |

## 1. Release smoke (Samsung, Play build)

| Row ID | Source row ID | Direction | Steps | Expected | Evidence to capture | Result |
|---|---|---|---|---|---|---|
| R1.1 | L1, P3 SC-1 | Samsung | Cold start from the Play install and log in as B. | Login lands on the client home; H4 crash count is 0. | H2 output, H4 count. | [x] pass (vc 3, 2026-11-02) |
| R1.2 | P3 lifecycle, P3 SC-3 | Samsung | With H5 running, watch `Realtime` after login, background the app, then return. | `state=Connecting` then `state=Connected`; backgrounding logs `Disconnected` about 1 s later; returning reconnects. | `Realtime` lines in the row log. | [-] not run: release build logs no state= lines (16-02 deviation); realtime proven by P0.5 and later chat rows |
| R1.3 | P13 (a) | Samsung | After login on Android 13+, note the notification permission prompt; log out and log in again. | The prompt appears once and not again after logout and login. | Yes or no, noted in the results file. | [x] pass (vc 3, 2026-11-02) |
| R1.4 | P13 (a), 13-RELEASE-CHECKLIST step 10 | Samsung | Read B's `device_tokens` row, background and return the app, read it again. | Platform is android and the row is refreshed after background and return. | Platform and updated-at read from Supabase. | [x] pass (vc 3, 2026-11-02) |
| R1.5 | P13 (c) | Samsung | Settings, Apps, ClickMe, Notifications. | The five ClickMe channels are listed. | Channel names read aloud or from a screenshot kept outside the repo. | [x] pass (vc 3, 2026-11-02) |

## 2. Direction 1 (QA-02): Android client B, iOS expert A

Samsung = B (client), iPhone = A (expert). Rows run in order. Amounts follow P0.9. Every paid row writes a `PAID` line (H9) and a row in "Paid bookings created".

### 2a Booking and push

| Row ID | Source row ID | Direction | Steps | Expected | Evidence to capture | Result |
|---|---|---|---|---|---|---|
| D1.01 | P6 SC-3 regression, P13 (b) | D1 | B books A's free 30-minute topic for a slot in the next hours. | Request Sent shows Awaiting Approval; the iPhone (A) receives the request push. | H5 log; iPhone push yes or no. | [x] pass (vc 3, 2026-11-02) |
| D1.02 | P13 (b) killed, P11 (b) mirror | D1 | Swipe ClickMe away (H6). A accepts on iOS. | The booking push arrives on the killed Samsung; tapping it opens that booking in the Bookings tab as Confirmed. | H6 `pidof` and `dumpsys notification` output. | [x] pass (vc 3, 2026-11-02) |
| D1.03 | P8 SC-1, P8 SC-2 | D1 | Keep My Bookings Upcoming open while A acts on iOS; then open Upcoming Details. | The list refreshes by realtime `booking.update` without a pull; Upcoming Details shows Confirmed, Join and Message. | `Realtime` lines in the H5 log. | [x] pass (vc 3, 2026-11-02) |
| D1.04 | P13 (b) running, P13 (c) | D1 | With the app open, A sends a chat message and then triggers a booking push. | Both post on the Messages and Bookings channels with the ClickMe icon; the open thread hides its own banner. | `dumpsys notification` channel names. | [x] pass (vc 3, 2026-11-02) |

### 2b Chat

| Row ID | Source row ID | Direction | Steps | Expected | Evidence to capture | Result |
|---|---|---|---|---|---|---|
| D1.05 | P12 (a), P12 (b) | D1 | Message from Upcoming Details. Exchange messages both ways; type on each side; use a thread with more than 30 messages for Load earlier; airplane mode and send; background then foreground. | Live both ways without duplicates; typing both ways; Read on Android bubbles; Load earlier pulls 30 more; the failed bubble retries once with no duplicate; catch-up after foreground. | `Realtime` lines (ids only) in the H5 log. | [!] fail (BUG-13, vc 3) |

### 2c Call on the confirmed free booking

| Row ID | Source row ID | Direction | Steps | Expected | Evidence to capture | Result |
|---|---|---|---|---|---|---|
| D1.06 | P9 SC-1 | D1 | Join inside the window from Upcoming Details opened from Explore Today's Sessions, from My Bookings and from the My Bookings card. At the first mic prompt: deny once, then deny permanently, then allow. | Each entry point reaches the call layer; deny once shows the rationale; deny permanently shows Open Settings; allow joins. | H5 `ClickMeCall` lines per entry point. | [x] pass (vc 3, 2026-11-02) |
| D1.07 | P9 SC-1, P11 (c) mirror, G-11-6 | D1 | A joins the same booking on iOS. Test two-way audio, mute and speaker. | `remote joined elapsed=` and `remote audio state=2` in the log; `channelProfile:0` in agoraapi.log; both UIs leave "waiting for the other person". | H5 log and the pulled agoraapi.log. | [x] pass (vc 3, 2026-11-02) |
| D1.08 | P9 SC-3 | D1 | In the call, minimize to the pill on every tab. Try Client/Expert switch, logout, Add Payment Method and Cancel of this booking. Press system Back in the call layer. | The pill shows on every tab; each of the four actions is refused by the call guard; predictive back minimizes. | Screenshots kept outside the repo. | [x] pass (vc 3, 2026-11-02) |
| D1.09 | P9 SC-2, FND-14 (replaced) | D1 | Press Home, turn the screen off for at least 5 minutes while talking. | Two-way audio throughout; the notification shows title and chronometer; the call is still connected after 5+ minutes. | H5 `dumpsys audio` and `dumpsys activity services` captured while backgrounded; H7 state. | [x] pass (vc 3, 2026-11-02) |
| D1.10 | P9 SC-4 | D1 | A hangs up on iOS, then rejoins; stay until the final 5 minutes of the booking. | Waiting for other person shows; audio resumes after rejoin (`remote offline reason=` then `remote joined`); the final 5 minutes show the amber state and banner. | `ClickMeCall` lines in the H5 log. | [x] pass (vc 4, 2026-11-03; was BUG-09) |
| D1.11 | P9 SC-2 notification, WR-06 | D1 | Open a Custom Tab (Help, Email Us or a meeting link), then tap the ongoing-call notification. Then tap End call in the notification. | A single MainActivity task; End call ends the call on both phones. | `dumpsys activity activities` task count in the log. | [x] pass (vc 3, 2026-11-02) |
| D1.12 | 09-HUMAN-UAT 7 | D1 | Bluetooth headset routing if a headset exists; an incoming GSM call to the Samsung during a call from another phone; audio after it ends. | Audio follows the headset; the call survives or recovers after the GSM call. Not run if the hardware is missing. | `dumpsys audio` snippet; `[-] not run: no headset` if so. | [-] not run: no headset |
| D1.13 | WR-05, WR-07, WR-09 | D1 | WR-07: book with a Skype or Zoom meeting type and an unopenable link. WR-09: on a free 90-minute booking, rejoin after start + 60 minutes. WR-05: note any refused foreground start on API 34+. | WR-07 shows "Couldn't open meeting" and nothing is confirmed; WR-09 rejoin works; WR-05 is recorded only if it happens. | H5 log lines for each. | [x] pass (vc 3, 2026-11-03) |
| D1.14 | 09-HUMAN-UAT 8 | D1 | Ask the user: keep the plain ongoing-call notification or switch to CallStyle. | The answer is recorded; a switch becomes a fix-if-cheap bug row. | The user's answer in the results file. | [x] pass (vc 3, 2026-11-02) |

### 2d Cards and the paid booking (amounts per P0.9)

| Row ID | Source row ID | Direction | Steps | Expected | Evidence to capture | Result |
|---|---|---|---|---|---|---|
| D1.15 | P7 SC-1 | D1 | Payment Methods lists the pre-flight cards. Set as default; Remove (confirm first); remove every card. | The default badge moves after the reload; Remove asks first; the list ends empty. | Brand and last 4 only. | [x] pass (vc 3, 2026-11-03) |
| D1.16 | P7 SC-3 | D1 | With no saved card, open A's paid topic. Add the main live card from the "Add a Payment Method" gate, press Back. Look at a slot more than 6 days out. | The gate shows; Back keeps every selection and shows Book Now; a slot more than 6 days out is dimmed with the 6-day note. | Screenshots kept outside the repo. | [x] pass (vc 3, 2026-11-03) |
| D1.17 | P7 SC-2 dismiss | D1 | Book Now on a paid slot within 6 days, dismiss the sheet with the preselected card. | Silent cancel: no booking and no PaymentIntent in requires_capture. | Stripe dashboard list (H10). | [x] pass (vc 3, 2026-11-03) |
| D1.18 | P7 SC-2 hold | D1 | Book Now, pick the saved card. | Request Sent with the hold card; A receives the request; Stripe shows a PaymentIntent in requires_capture for the floor amount. PAID line, expected end void. | Stripe dashboard (PI last 6, amount); booking id first 8. | [x] pass (vc 4, 2026-11-03; was BUG-05) |
| D1.19 | P7 SC-2, P8 SC-3, P8 SC-1 | D1 | B cancels that held booking with a comment. | Booking moves to the Past tab with a snackbar; A sees the cancellation; the Stripe PaymentIntent is canceled. Label void (no charge, no refund, no fee), never refund. | Stripe dashboard status canceled. | [x] pass (vc 4, 2026-11-03) |
| D1.20 | P7 SC-2 retry | D1 | Airplane mode during confirm, then Book Now again. If a server-created booking exists, the slot conflict points to My Bookings. Cancel anything created. | Network copy with the payment reference; no duplicate booking on retry; created bookings are cancelled (void). | PAID line for any booking created; Stripe status. | [x] pass (vc 4, 2026-11-03) |
| D1.21 | P7 SC-4, D-02 | D1 | Use the 3DS card from P0.4: book paid; pass the bank challenge. Then a second attempt that cancels the challenge. | The 402 challenge runs on A's connected account, passes, and the booking confirms (PAID line); the cancelled attempt shows "Payment wasn't completed. Your card wasn't charged." and Book Now retries. `[-] not run: no challenging card` only under the P0.9 plan. | Stripe status; booking id first 8. | [-] not run: 3DS accept not run per P0.9 (no challenging card) |
| D1.22 | D-02 decline | D1 | Book paid with the declining card. | A payment-failed message shows; another card retries with the same attempt. Or `[-] not run: no failing card`. | Error copy and code only. | [-] not run: no failing card per P0.9 |
| D1.23 | P7 SC-5 | D1 | Toggle dark mode (phones are portrait-locked, so this is the recreation) while the payment sheet and the challenge are open. | Neither launches twice. The JPY part is `[-] not run: no JPY topic in production`. | H5 log. | [x] pass (vc 4, 2026-11-03; was BUG-06; challenge and JPY parts not run) |
| D1.24 | P8 SC-4 | D1 | A proposes a reschedule on iOS. B accepts one booking and declines another, with and without a reason. Then trigger `PAYMENT_METHOD_REQUIRED` recovery with a saved card and with none. | The booking leaves pending_reschedule; recovery works with a saved card and with none. | Error code and outcome only. | [-] not run: part 1 (accept, decline with and without reason, free bookings) passed per user 2026-11-03; saved-card recovery unreachable live, runbook premise wrong (card always saved, clone cached), skipped by user 2026-11-04 |

### 2d-R Reschedule start (Phase 16.2)

Android client B proposes; iOS expert A responds. Paid rows end in a void (cancel), never a capture.

| Row ID | Source row ID | Direction | Steps | Expected | Evidence to capture | Result |
|---|---|---|---|---|---|---|
| D1.RS1 | PARITY #10, D-09 | D1 | Free booking. B taps Reschedule on Upcoming Booking Details, picks a new slot, adds a message and submits. A sees the proposal on iOS and accepts. | B is back on the details with "Reschedule request sent" and "Awaiting expert's response"; A sees the proposal; after the accept the booking shows the new time on both phones. | Outcome per step; booking id first 8. | [x] pass (vc 3, 2026-11-03) |
| D1.RS2 | D-05, D-09 | D1 | From a My Bookings card on another free booking, B proposes a new slot. A declines on iOS. | The request is sent from the card; after the decline the booking keeps its original time on both phones. | Outcome per step. | [x] pass (vc 3, 2026-11-03) |
| D1.RS3 | D-01, D-03 | D1 | Paid held booking (P0.9 amount). B opens the picker. Then B proposes a slot inside the 6-day window and inside the auth window; A accepts on iOS. Then B cancels the booking (void). | Days beyond now + 6 days show no dot and their chips are dimmed with "Paid sessions can be rescheduled up to 6 days ahead."; the in-window proposal is accepted; the cancel voids the hold. PAID line, expected end void. | Stripe dashboard status canceled; booking id first 8. | [x] pass (vc 4, 2026-11-04) |
| D1.RS4 | D-11, RESCHEDULE-PAYMENT-CONVERSION section 2 | D1 | A paid booking authorized at least one day earlier. B picks a slot whose session end is after the card hold expiry and within 6 days. Cancel at the dialog once and confirm nothing changed; propose again and Continue. A accepts on iOS. Then B cancels the converted booking (void). | The "Your card hold will be released" dialog shows before any request (with the sooner-than-24-hours line when under 48 h); Cancel changes nothing; Continue sends; on accept the booking converts and Stripe shows the original PaymentIntent canceled. PAID line, expected end void. Open item: the server's under-24 h conversion message says "choose a later time" while the app shows the fixed "cancel and book again" copy; record which text appears. | Stripe status of the original and any new PaymentIntent (last 6, status only); booking id first 8; the copy seen. | [x] pass (vc 4, 2026-11-04) |
| D1.RS5 | D-06, D-07, D-08, RESCHEDULE-PAYMENT-CONVERSION section 3.1 | D1 | A paid booking first paid with a one-off PaymentSheet card that was not saved, and B has no saved card. B proposes a reschedule that needs conversion. Follow the "Add a payment method" dialog to Payment Methods, add a card and return. Check that nothing is sent automatically. B taps Submit again. Then cancel the booking (void). | The "Add a payment method" dialog shows; "Add a Payment Method" opens Payment Methods; after returning nothing has been sent; the second Submit succeeds, or shows "The card on file can't be used for this reschedule…" on a second PAYMENT_METHOD_REQUIRED. PAID line, expected end void. | Error codes seen (PAYMENT_METHOD_REQUIRED once or twice) and the outcome; brand and last 4 at most; booking id first 8. | [-] not run: unreachable live: both apps always save the card and the booking stores it, so PAYMENT_METHOD_REQUIRED cannot be raised; runbook premise wrong |
| D1.RS6 | D-04 (amended 2026-11-01) | D1 | (a) On a free confirmed booking that starts in under 24 h, B taps Reschedule on the My Bookings card, picks a later slot and submits; A declines on iOS. (b) Look at a pending-approval booking and a booking already pending reschedule, on Upcoming Details and on the My Bookings card. | (a) Reschedule is present and the request is sent ('Reschedule request sent'); after A's decline the booking keeps its original time on both phones. (b) Reschedule is present on the pending-approval booking on both surfaces and absent on the pending-reschedule booking on both surfaces (iOS parity). | Yes or no per case and surface; outcome of (a); error code if any. | [x] pass (vc 3, 2026-11-03) |
| D1.RS7 | D-12 | D1 | Open the picker on a confirmed upcoming booking. | The picker opens on the current booking's day; the "Current" chip is disabled and the "Current:" banner shows; past slots are hidden. | Outcome per item. | [x] pass (vc 3, 2026-11-03) |
| D1.RS8 | RESEARCH Open Question 5 | D1 | A 60-minute booking: B proposes a slot inside a 30-minute gap; A accepts on iOS. | Record whether A's accept returns SLOT_UNAVAILABLE (known behavior, iOS parity). | Error code and outcome only. | [x] pass (vc 4, 2026-11-04; case is a slot exactly one hour before another booking, not a 30-minute gap) |

### 2e Capture

| Row ID | Source row ID | Direction | Steps | Expected | Evidence to capture | Result |
|---|---|---|---|---|---|---|
| D1.25 | P9 SC-5 | D1 | Make a paid booking inside the join window, A accepts, both join and finish the session. | Railway shows confirm-connection exactly once; about 15 minutes after the scheduled end Stripe shows the PaymentIntent succeeded on A's connected account. PAID line, expected end capture, refunded in Section 7. | Railway log line count; Stripe status; booking id first 8. | [x] pass (vc 4, 2026-11-04; Railway confirm-connection count not checked) |

### 2f After sessions

| Row ID | Source row ID | Direction | Steps | Expected | Evidence to capture | Result |
|---|---|---|---|---|---|---|
| D1.26 | P13 (e) | D1 | Write Review from the session ended push, the review nudge push, Booking Summary and a Past card. | Read-only after submit; the Feedback block and the thanks snackbar show. | Outcome per entry point. | [x] pass (vc 3, 2026-11-03) |
| D1.27 | P8 SC-1 Past | D1 | Open the Past tab. | Past labels are right; a completed-but-not-ended session follows the rejoin rule. | Labels noted. | [x] pass (vc 3, 2026-11-03) |
| D1.28 | P13 (d) | D1 | Open the notifications list from both bells; receive a foreground push; tap rows; turn the permission off and use the settings round trip. | Grouped by date; a foreground push reloads it; rows route like pushes; the permission-off row works. | Outcome per bullet. | [x] pass (vc 3, 2026-11-03) |
| D1.29 | P12 (c), P12 (d) | D1 | Run last in Direction 1. B reports A from chat (Supabase report row), blocks from chat ("Blocked {name}") and from the profile; the iPhone side tries to send; Unblock. | The report row stores the enum value; the iPhone side cannot send; Unblock restores the composer. | Supabase report row (enum value only). | [x] pass (vc 4, 2026-11-04) |

### 2f-F Saved Experts, favorite heart and full Reviews (Phase 16.3)

Samsung = B (client). No money moves in these rows.

| Row ID | Source row ID | Direction | Steps | Expected | Evidence to capture | Result |
|---|---|---|---|---|---|---|
| D1.FV1 | PARITY #2b, D-01..D-04 | D1 | B opens A's profile from Explore. Watch the heart; tap it; leave and reopen the profile; tap it again; then double tap quickly. | The outlined heart shows only after the profile loads; a tap fills it; after reopening it is still filled; the next tap outlines it; a fast double tap changes it once. | Outcome per step. | [x] pass (vc 3, 2026-11-03) |
| D1.FV2 | D-03 | D1 | With airplane mode on, tap the heart on A's profile. | The heart flips and rolls back with a snackbar (connection message or "Couldn't update favorites"). | Snackbar text seen. | [x] pass (vc 3, 2026-11-03) |
| D1.FV3 | PARITY #2, D-09, D-10, D-12 | D1 | Favorite A. Open Profile, Discovery, Saved Experts. Open A's profile from the card and unfavorite; press Back. Favorite A again; on the list tap A's heart; try Cancel, then Remove. | A is listed with photo, name, green headline and tags; after unfavoriting and Back, A is gone without a pull; the dialog reads "Remove {A} from favorites?" with Cancel and Remove; Cancel keeps A; Remove removes A and shows the empty state. | Outcome per step. | [x] pass (vc 3, 2026-11-03) |
| D1.FV4 | D-05..D-08 | D1 | On the Saved Experts empty state: read the Pro Tip; tap "Explore Experts"; go back to Profile; tap "View My History". | The Pro Tip names the heart icon; "Explore Experts" opens the Explore tab root; back on Profile, Saved Experts is still open; "View My History" opens Bookings with Past selected. | Outcome per step. | [x] pass (vc 3, 2026-11-03) |
| D1.FV5 | PARITY #3, D-13, D-14, D-16 | D1 | Tap "See all" on A's Recent Reviews from Explore, from Search, from a chat "View profile" and from Saved Experts. Switch tabs while two of these are open. | "{First}'s Reviews" shows the overall card and review cards; Back returns to the profile; switching tabs with two open causes no crash. | Outcome per origin; H4 crash count. | [x] pass (vc 3, 2026-11-03) |
| D1.FV6 | D-13 | D1 | Open the profile of an expert with 0 reviews; tap "See all". | The Recent Reviews card shows only its header and "See all"; "See all" shows "No reviews yet". | Outcome per step. | [x] pass (vc 3, 2026-11-03) |
| D1.FV7 | CR-01, D-05 | D1 | Favorite A. Open Profile, Discovery, Saved Experts, then A's profile. Book A's free topic to Request Sent; tap "View My Bookings"; tap the Profile tab. Book the same way again; tap "Back to home"; tap the Profile tab. As B, cancel both bookings. | "View My Bookings" opens Bookings and "Back to home" opens Explore; each time the Profile tab shows the hub with the bottom bar, not Request Sent and not Saved Experts. | Outcome per exit. | [x] pass (vc 3, 2026-11-03) |

### 2f-N Private notes, client side (Phase 16.4)

Samsung = B (client). No money moves in these rows.

| Row ID | Source row ID | Direction | Steps | Expected | Evidence to capture | Result |
|---|---|---|---|---|---|---|
| D1.PN1 | PARITY #7, D-03, D-05 | D1 | On a past card B taps "Private Note", types a note and taps Save. Then Back. | The header shows A's name, the topic and the date; the Save action shows a spinner, then a check for about 1.5 s; the screen stays; after Back the card shows the preview. | Outcome per step. | [x] pass (vc 3, 2026-11-03) |
| D1.PN2 | D-07, D-08 | D1 | On an upcoming card B taps "Add private note", saves, and returns. Tap the preview pencil. Open Upcoming Booking Details. | The card shows the preview with a pencil; tapping it shows the saved text; Upcoming Booking Details shows no note card. | Outcome per step. | [x] pass (vc 3, 2026-11-03) |
| D1.PN3 | D-06, D-08 | D1 | B opens Booking Summary from a past card, from a booking event in a chat thread and from a push. Tap "My Note", edit, go back. | "My Note" opens the editor from all three; after an edit the full note shows on return. | Outcome per entry point. | [x] pass (vc 3, 2026-11-03) |
| D1.PN4 | D-01, D-02, D-04 | D1 | With unsaved edits press the app bar arrow, then system back. Choose Keep editing, then Discard. With no edits press Back. Paste over 5,000 characters. Save only spaces. | Each back asks "Discard changes?"; Keep editing keeps the text; Discard leaves; with no edits no dialog shows; the paste is cut at 5,000 and the counter shows from 4,500; the spaces-only save clears the note and the card shows no preview. | Outcome per step. | [x] pass (vc 3, 2026-11-03) |
| D1.PN5 | PARITY #7, #8, RESEARCH notes are independent columns | D1 | On one booking B writes a client note on Android and A writes an expert note on iOS. Each then reads their own note on their own platform. | Each side reads only their own note; neither sees or overwrites the other's. | Yes or no per side; never the note text. | [x] pass (vc 3, 2026-11-03) |
| D1.PN6 | 16.4-06 manual backstop | D1 | A booking push opens Booking Summary on the Samsung (app killed, then backgrounded). Tap "My Note", save a note, press Back twice. | "My Note" opens the editor from the push-opened summary; the saved note shows on return; Back leaves the app stack in the order the push built. | Outcome per launch state. | [x] pass (vc 3, 2026-11-03) |
| D1.PN7 | 16.4-03 manual backstop | D1 | In the note editor look at the field border unfocused and focused, tap Save and watch the icon, open the keyboard and scroll a long note. | The border changes on focus; the Save spinner cross-fades to the check without a jump; the field, counter and Save stay reachable with the keyboard up and the text scrolls. | Note. | [x] pass (vc 3, 2026-11-03) |

### 2f-S Search tab and Explore See all (Phase 16.1)

Samsung = B (client). No money moves in these rows.

| Row ID | Source row ID | Direction | Steps | Expected | Evidence to capture | Result |
|---|---|---|---|---|---|---|
| D1.SE1 | PARITY #1, 16.1 UAT 1 | D1 | On the Search tab: first load; type a query; tap a chip; change the sort; open a result; press Back. | The first load shows 3 skeletons; typing waits about 300 ms before searching; a chip sends its slug; sort changes the order; a result opens Expert Profile with the bottom bar visible; Back returns to the same results. | Outcome per step. | [x] pass (vc 3, 2026-11-03) |
| D1.SE2 | 16.1 UAT 2 | D1 | Open Search side by side with iOS SearchExpertView. | Spacing, the 80 dp avatar with its fallback, rating alignment, tag pills, glow and the pulsing empty illustration match iOS. | Note per element. | [x] pass (vc 3, 2026-11-03) |
| D1.SE3 | D-01, 16.1 UAT 3 | D1 | Tap See all on both Explore headers. Type a query, switch tabs and return. Open a profile from results and re-tap the Search tab. | See all selects Search with no filter carried; the query and results survive a tab switch; re-tapping Search pops an open profile and keeps the query. | Outcome per step. | [x] pass (vc 3, 2026-11-03) |
| D1.SE4 | 16.1 UAT 4 | D1 | Pull to refresh on results, on the empty state and on the error state, with and without airplane mode. | Rows stay during refresh; the snackbar has Retry and sits above the keyboard. | Outcome per state. | [x] pass (vc 3, 2026-11-03) |
| D1.SE5 | 16.1 UAT 6 | D1 | Scroll the results, then change the query, a chip and the sort. Separately switch tabs and pull to refresh. Start a newer search while a snackbar shows. | A query, chip or sort change opens at the top; a tab switch and a pull keep the scroll position; a visible snackbar disappears when a newer search starts. | Outcome per step. | [x] pass (vc 3, 2026-11-03) |

### 2f-P Payment History, notification preferences, Feedback and the splash (Phase 16.5)

Samsung = B (client). Only D1.PH4 touches money: use a paid booking already due to be voided under P0.9 and write its `PAID` line.

| Row ID | Source row ID | Direction | Steps | Expected | Evidence to capture | Result |
|---|---|---|---|---|---|---|
| D1.PH1 | PARITY #4, D-04 | D1 | Open Profile, Account, Payment History. Pull to refresh. Open a row and press Back. | A skeleton shows first, then rows newest first; a held paid booking reads "Authorized (not yet charged)" and a captured one "Paid"; amounts match the Stripe dashboard; pull to refresh keeps rows; Back keeps the scroll position. | Outcome per step; amounts read against Stripe (H10). | [x] pass (vc 4, 2026-11-04); vc 5 2026-11-05: Payment History rows show ID: #XXXXXXXX (BUG-29 client half re-test) |
| D1.PH2 | CLIENT-TRANSACTIONS section 7 | D1 | On an account with over 20 rows scroll to the end. Repeat in airplane mode for the next page. If no account has 20 rows, mark the row [-] not run with the reason. | Pages load with no error footer at the end; in airplane mode the next page shows the footer and Retry works. | Outcome per step. | [x] pass (vc 4, 2026-11-04) |
| D1.PH3 | D-03, UI-SPEC A8 | D1 | Tap a row for a live, a completed, a cancelled and a failed booking. | Details open with the real status; no enabled Join and no Reschedule on the last three, including inside the join window. | Outcome per status. | [x] pass (vc 4, 2026-11-04) |
| D1.PH4 | D-03, 16.5-02 | D1 | Cancel a live paid booking from a Payment History row. | Bookings opens with the result notice; the Profile tab shows the hub, not Cancel; the hold is voided. | Outcome per step; PAID line (H9) and a Paid bookings created row. | [x] pass (vc 4, 2026-11-04) |
| D1.PH5 | 16.5 review WR-01 | D1 | Open a Payment History row for a confirmed booking whose session has ended. If no account has one, mark the row [-] not run with the reason. | No Cancel Booking button. Reschedule may still show (no time gate on confirmed, 16.2 D-04). Do not tap anything that changes the booking. | Screenshot or note. | [-] not run: no confirmed ended booking in Payment History on the client account, user 2026-11-03 |
| D1.NS1 | PARITY #5, D-01 | D1 | Open Profile, Preferences, Notifications. Flip three rows quickly. Reopen the screen and compare with iOS. | Four sections and 13 rows, nothing about quiet hours; opening sends no PATCH (Railway logs, H10); the three flips send one PATCH; the bar shows a spinner then a check; reopening shows the values; iOS shows the same values. | Outcome per step; PATCH count from Railway logs. | [x] pass (vc 3, 2026-11-03) |
| D1.NS2 | D-02 | D1 | In airplane mode flip a row. Reconnect and tap Retry. | An error icon and Retry show in the bar, the toggle stays, no dialog; after Retry the check shows. | Outcome per step. | [x] pass (vc 3, 2026-11-03) |
| D1.NS3 | backend push gate | D1 | Turn off Booking Confirmations. A accepts a request from iOS. Turn the row back on. | No push arrives on the Samsung while the row is off. | Yes or no on the push; row restored. | [x] pass (vc 3, 2026-11-03) |
| D1.FB1 | PARITY #6, D-05, D-06 | D1 | Open Profile, Support, Send Feedback. Enter 9 characters and tap Submit, then 10 characters and Submit. | The first server type is selected and the email is prefilled; 9 characters show the inline minimum-length error and send no request; 10 characters show the thanks dialog and OK returns to the hub; a backend row exists (H10). | Outcome per step; backend row yes or no. | [x] pass (vc 3, 2026-11-03) |
| D1.FB2 | carried from iOS | D1 | In airplane mode tap Submit on a valid form. Reconnect and tap Submit. | The failure dialog shows and the form is kept; after reconnecting Submit works. | Outcome per step. | [x] pass (vc 3, 2026-11-03) |
| D1.SP1 | PARITY #13, D-07, D-08, A4 | D1 | Force-stop the app and cold start it; repeat logged out. | The system splash is plain dark with no logo (note how long it feels on a cold start; iOS shows the logo and title at once, and Android 12+ cannot show text at this stage). It hands to the Compose splash with no color jump or flicker; the whole logo fades and scales, the 52 bold wordmark and tagline follow, the glow pulses; about 3 s later the right destination shows; logged out reaches Login; no notification prompt or suspended dialog shows over the splash. | Outcome per step. | [x] pass (vc 4, 2026-11-03; was BUG-11) |
| D1.SP2 | D-09, RESEARCH Question 2 | D1 | During a live call swipe the task away, then tap the ongoing-call notification. | The call shows above any splash and audio never stopped; the hold does not replay in the same process. | Outcome per step; H5 call proof lines. | [x] pass (vc 3, 2026-11-03) |
| D1.SP3 | D-09 | D1 | With the app killed (H6) tap a booking push. | The full splash shows, then the right screen in the right tab; nothing is lost. | Outcome per step. | [x] pass (vc 3, 2026-11-03) |
| D1.SP4 | A10, Pitfall 1 | D1 | Change the font size during the splash. Background and return. Repeat with animations off (Remove animations). | No replay on a font change; no replay on background and return; with animations off a static final frame shows for about 3 s; the app never sits on the OS splash. | Outcome per step. | [x] pass (vc 3, 2026-11-03) |

### 2g Payouts mirror (D-05, iOS half)

| Row ID | Source row ID | Direction | Steps | Expected | Evidence to capture | Result |
|---|---|---|---|---|---|---|
| D1.30 | P14 (a) on iOS, P11 (a) mirror | D1 | On the iPhone as A open the payout onboarding link and return. Then B on Android looks at A's paid topic. | The link opens and returns (record what Stripe shows for an already-complete account, RESEARCH A4); A's paid topic is still bookable on Android. Never finish a new Connect account (D-05). | What Stripe showed, one line. | [x] pass (vc 3, 2026-11-03) |

## 3. Direction switch

Log out on both phones and log in with the roles flipped, so accounts and directions switch once.

| Row ID | Source row ID | Direction | Steps | Expected | Evidence to capture | Result |
|---|---|---|---|---|---|---|
| SW.1 | P13 (a) | D1 | Log out on both phones. | B's android `device_tokens` row and A's ios row are gone. | Supabase `device_tokens` read. | [!] fail (BUG-24, vc 4) |
| SW.2 | RESEARCH Pitfall 7 | D2 | Log in A on the Samsung and B on the iPhone. | A has a platform android row and B has a platform ios row. | Supabase `device_tokens` read. | [x] pass (vc 4, 2026-11-04) |
| SW.3 | P11 (a) | D2 | A switches to Expert mode on the Samsung; B stays in client mode on iOS. | Samsung shows the expert Dashboard; iPhone shows the client home. | Screenshot kept outside the repo. | [x] pass (vc 4, 2026-11-04) |
| SW.4 | D-08, H2 | D2 | Run H2 again. | Same versionCode and installer as the Direction 1 session. | H2 output. | [x] pass (vc 4, 2026-11-04) |

## 4. Direction 2 (QA-03): iOS client B, Android expert A

Samsung = A (expert), iPhone = B (client). Rows run in order. Amounts follow P0.9. Every paid row writes a `PAID` line (H9) and a row in "Paid bookings created".

### 4a Requests

| Row ID | Source row ID | Direction | Steps | Expected | Evidence to capture | Result |
|---|---|---|---|---|---|---|
| D2.01 | P10 (d) | D2 | A edits availability on Android. B on iOS opens A's booking calendar. | The iOS client sees matching slots in A's timezone. | Slot times on both sides. | [x] pass (vc 4, 2026-11-04) |
| D2.02 | P13 (b) killed, P11 (b) | D2 | Swipe ClickMe away (H6). B books A's free topic on iOS. Tap the request push on the Samsung (mode switch first when in Client mode). | The request push arrives on the killed Samsung and opens Request Decision in Expert mode. | H6 `pidof` and `dumpsys notification` output. | [x] pass (vc 4, 2026-11-04) |
| D2.03 | P11 (a), P11 (b) | D2 | Dashboard pending count, Incoming Requests, accept with a message, Confirmed, Add to Calendar, Back, Back to Home. | The pending count is right; Confirmed shows; Add to Calendar opens the event editor; Back returns to Incoming Requests; Back to Home lands on Dashboard; iOS shows Confirmed. | Outcome per step. | [x] pass (vc 4, 2026-11-04) |
| D2.04 | P11 (b) | D2 | B sends four more free requests. Decline once per reason, with and without a note. Open an expired request. Accept a request B cancelled meanwhile. | iOS sees each decline; an expired request opens read-only (`[-] not run: none exists` if so); the cancelled one shows ALREADY_PROCESSED. | Error code and outcome per request. | [x] pass (vc 4, 2026-11-04; needed five requests; expired part not run, none exists) |

### 4b Call

| Row ID | Source row ID | Direction | Steps | Expected | Evidence to capture | Result |
|---|---|---|---|---|---|---|
| D2.05 | P11 (c), G-11-1, G-11-5, G-11-6 | D2 | Join from the Dashboard Today's Sessions card, an Upcoming card and Booking Summary with B on iOS. Test two-way audio, mute, speaker, leave and rejoin. | `remote joined elapsed=` and `remote audio state=2` in the log; two-way audio; the backend row has both joined_at values. | H5 log; Supabase booking row joined_at values. | [x] pass (vc 4, 2026-11-04) |
| D2.06 | P9 SC-2 in D2 | D2 | Background the Android expert with the screen off for at least 5 minutes while talking. | Two-way audio throughout; notification shows title and chronometer. | H5 `dumpsys audio` and services output while backgrounded. | [x] pass (vc 4, 2026-11-04) |
| D2.07 | P9 SC-4 in D2 | D2 | B hangs up on iOS and rejoins; stay to the final 5 minutes. | Waiting state, audio resumes after rejoin, the amber state and banner show on Android. | `ClickMeCall` lines in the H5 log. | [x] pass (vc 4, 2026-11-04) |

### 4c Chat

| Row ID | Source row ID | Direction | Steps | Expected | Evidence to capture | Result |
|---|---|---|---|---|---|---|
| D2.08 | P12 (a), P12 (b) expert side | D2 | Message from Request Decision, the expert Booking Summary, an expert Upcoming card and the in-call Chat control (the call stays in the pill). Exchange messages with iOS. | Every entry lands on the thread; live both ways with iOS; Back returns to the list. | `Realtime` lines (ids only). | [x] pass (vc 4, 2026-11-04; user report only, the Play build logs no Realtime lines) |

### 4d Paid

| Row ID | Source row ID | Direction | Steps | Expected | Evidence to capture | Result |
|---|---|---|---|---|---|---|
| D2.09 | P7 SC-2 on iOS, D-01 | D2 | B on iOS books A's paid topic within 6 days with a saved card. | The Android request shows the earnings card; Stripe shows requires_capture. PAID line, expected end void. | Stripe dashboard (PI last 6, amount); booking id first 8. | [x] pass (vc 4, 2026-11-04) |
| D2.10 | P11 (d) | D2 | A cancels that held booking from Android. | iOS sees the cancellation; the Stripe PaymentIntent is canceled. Label void (no charge, no refund, no fee). | Stripe dashboard status canceled. | [x] pass (vc 4, 2026-11-04) |
| D2.11 | P7 SC-4 on iOS, D-02 | D2 | B on iOS books paid with the 3DS card and passes the challenge. | The booking appears on Android as a request. Or `[-] not run` per P0.9. PAID line if made. | Stripe status; booking id first 8. | [-] not run: 3DS accept not run per P0.9 (no challenging card) |
| D2.12 | P9 SC-5 in D2 | D2 | Paid session end to end with the iOS client and the Android expert. | Confirm-connection is sent once; Stripe shows capture on A's connected account. PAID line expected capture, refunded in Section 7. Or `[-] not run` when the P0.9 plan keeps one capture only. | Railway log line count; Stripe status. | [x] pass (vc 4, 2026-11-05; confirm-connection sent once per app, two Railway lines) |

### 4e Reschedule

| Row ID | Source row ID | Direction | Steps | Expected | Evidence to capture | Result |
|---|---|---|---|---|---|---|
| D2.13 | P11 (d) | D2 | B proposes a reschedule on iOS; A accepts one and declines another from Booking Summary. Then create two back-to-back bookings inside the 30-minute buffer. | Accept and decline both work; the second back-to-back booking fails with SLOT_UNAVAILABLE. | Error code and outcome. | [x] pass (vc 4, 2026-11-04) |

### 4e-R Reschedule start (Phase 16.2)

Android expert A proposes; iOS client B responds. Paid rows end in a void (cancel), never a capture.

| Row ID | Source row ID | Direction | Steps | Expected | Evidence to capture | Result |
|---|---|---|---|---|---|---|
| D2.RS1 | PARITY #11, D-10 | D2 | A free booking whose Expert Booking Summary shows Reschedule (`can_reschedule` true). A proposes a new slot with a message. B accepts on iOS. | A is back on the summary with "Reschedule request sent" and "Awaiting client's response"; B accepts and the booking shows the new time on both phones. | Outcome per step; booking id first 8. | [!] fail (BUG-25, vc 4) |
| D2.RS2 | PARITY #11, D-10 | D2 | From an expert upcoming card on a paid held booking inside 6 days, A proposes a new slot. B accepts on iOS, which triggers the iOS client-side saved-card recovery. Then A cancels the booking (void). | The proposal is sent from the card; record what the iOS accept does and any error code. PAID line, expected end void. | Error codes and outcome only; Stripe status canceled; booking id first 8. | [x] pass (vc 4, 2026-11-05; iOS accept unreachable (BUG-25), saved-card recovery not exercised) |
| D2.RS3 | locked `can_reschedule` | D2 | Compare a booking where the server reports `can_reschedule` false (for example a completed past booking) with a confirmed upcoming one. | A's Reschedule button is absent on the first and present on the second. | Yes or no per booking. | [x] pass (vc 4, 2026-11-05); vc 5 2026-11-05: expert Upcoming and Past booking rows show ID: #XXXXXXXX (BUG-29 expert half re-test) |
| D2.RS4 | D-02 | D2 | On a paid booking A opens the reschedule calendar. | No dot and dimmed chips beyond now + 6 days, with the paid note. | Outcome one line. | [x] pass (vc 4, 2026-11-05) |

### 4e-F Expert self-view reviews (Phase 16.3)

Samsung = A (expert). No money moves in this row.

| Row ID | Source row ID | Direction | Steps | Expected | Evidence to capture | Result |
|---|---|---|---|---|---|---|
| D2.FV1 | D-15 | D2 | A opens "View My Public Profile" from the expert hub. Tap "See all" on Recent Reviews. | The self-view shows no heart; Recent Reviews shows "See all"; it opens A's own full Reviews list with no bottom bar; Back returns to the profile. | Outcome per step. | [x] pass (vc 4, 2026-11-05) |

### 4e-N Expert notes and Client Profile (Phase 16.4)

Samsung = A (expert). No money moves in these rows.

| Row ID | Source row ID | Direction | Steps | Expected | Evidence to capture | Result |
|---|---|---|---|---|---|---|
| D2.PN1 | PARITY #8, D-07, D-08 | D2 | A uses "Private Note" on a past card, the note zone on an upcoming card and "My Note" on Expert Booking Summary. | Each opens the editor; the saved note shows on return in each place. | Outcome per place. | [x] pass (vc 4, 2026-11-05) |
| D2.PN2 | PARITY #7, #8, independence the other way | D2 | On one booking A writes an expert note on Android and B writes a client note on iOS. Each reads their own note. | Each reads only their own note; neither overwrites the other's. | Yes or no per side; never the note text. | [x] pass (vc 4, 2026-11-05) |
| D2.CP1 | PARITY #9, D-09, D-10, D-11 | D2 | A taps "View Profile" on Expert Booking Summary. Pull to refresh. | Avatar, name, "Client" badge, About, "Your Sessions Together" and "Booking History" show in that order; a paid completed row shows A's net earnings, a free row shows no amount, a cancelled row shows no amount; rows are not tappable; pull to refresh works. | Outcome per element. | [x] pass (vc 4, 2026-11-05) |
| D2.CP2 | D-13 | D2 | In an expert-mode chat thread with a client open the menu and tap "View profile". Press Back. | The menu shows "View profile"; it opens the same Client Profile; Back returns to the thread. | Outcome per step. | [x] pass (vc 4, 2026-11-05) |
| D2.CP3 | D-14 | D2 | From an expert-mode chat with a person who has no booking with A tap "View profile". | "No bookings with this person yet" shows with no Retry and a working "Go back". | Outcome per step. | [x] pass (vc 4, 2026-11-05) |
| D2.PN3 | 16.4-09 manual backstop | D2 | A booking push opens Expert Booking Summary on the Samsung (app killed, then backgrounded). Tap "My Note", save, press Back twice. | "My Note" opens the editor from the push-opened summary; the saved note shows on return; Back follows the order the push built. | Outcome per launch state. | [x] pass (vc 4, 2026-11-05) |
| D2.CP4 | 16.4-09 manual backstop | D2 | From a booking event in an expert-mode chat thread open Expert Booking Summary, tap "View Profile", press Back twice. | Client Profile opens on the Chats stack; Back returns to the summary, then to the thread; the bottom bar stays hidden throughout. | Outcome per step. | [x] pass (vc 4, 2026-11-05) |
| D2.CP5 | 16.4-08 manual backstop | D2 | On a Client Profile look at the avatar ring and glow and the Upcoming row border. Reach the friendly 403 and 404 states (a chat peer with no booking; a removed account if one exists) and pull to refresh on each. | The ring and static glow match the iOS profile; the Upcoming row shows its border; each friendly state refreshes on pull and keeps Go back. | Note per item. | [x] pass (vc 4, 2026-11-05) |

### 4e-P Notification preferences, Feedback and View Past History, expert side (Phase 16.5)

Samsung = A (expert). No money moves in these rows.

| Row ID | Source row ID | Direction | Steps | Expected | Evidence to capture | Result |
|---|---|---|---|---|---|---|
| D2.NS1 | PARITY #5 | D2 | Open the expert hub, Preferences, Notifications. Flip a row. Open the same user on the iOS client account. | The same four sections show; the flip saves; iOS shows the value on its next load. | Outcome per step. | [x] pass (vc 4, 2026-11-05) |
| D2.FB1 | PARITY #6, D-06 | D2 | Open the expert hub, Support, Send Feedback and send a valid message. Tap OK. | The message sends; OK returns to the expert hub, not the client hub. | Outcome per step; backend row yes or no. | [x] pass (vc 4, 2026-11-05) |
| D2.PV1 | PARITY #12 | D2 | With no upcoming bookings tap View Past History. | The Past tab is selected. | Outcome. | [x] pass (vc 4, 2026-11-05) |

### 4f Payouts (D-05, Android half)

| Row ID | Source row ID | Direction | Steps | Expected | Evidence to capture | Result |
|---|---|---|---|---|---|---|
| D2.14 | P14 (a), P11 (a) | D2 | Payouts, Set up payouts. The Stripe page opens in a Custom Tab; leave or finish. Repeat from the Dashboard card. | `clickme://connect/return` brings Payouts back with the status refreshed. If Chrome does not follow the backend 302 into the app (RESEARCH A1), the ON_RESUME refresh still reloads: record it; it is blocking (D-07) only if it prevents payout setup entirely. | What Stripe showed; whether the app returned by deep link or by resume. | [-] not run: expert account already onboarded, Payouts shows the ready state with no Set up payouts entry; a new Connect account is forbidden (D-05) |
| D2.15 | P14 (a) refresh | D2 | Open the link, wait more than 5 minutes (or reopen a used link) and act on the page. | `/refresh` arrives and a fresh link opens. | Outcome one line. | [-] not run: no onboarding link reachable on the onboarded expert account, same reason as D2.14 |
| D2.16 | P14 (a) forged | D2 | Run H8 (forged intent). | The app only reloads; nothing is marked complete. | H8 output and a screenshot kept outside the repo. | [x] pass (vc 4, 2026-11-05; weak proof, the account was already complete) |

### 4g Account deletion (store-policy rows, D-07)

**Run the delete flow ONLY on the Throwaway account, never on A or B.**

| Row ID | Source row ID | Direction | Steps | Expected | Evidence to capture | Result |
|---|---|---|---|---|---|---|
| D2.17 | P4 SC-1 partial, P14 (d) | D2 | Log out A on the Samsung. Sign up the Throwaway on Android. Book A's free topic as Throwaway. Log back in as A and accept it. Log in as Throwaway again. | The Throwaway account exists with one upcoming booking. | Booking id first 8. | [x] pass (vc 4, 2026-11-05); vc 5 2026-11-05: photo crop circle shows the photo (was BUG-31), Next and Publish Profile clear the 3-button bar (was BUG-30) |
| D2.18 | P14 (d) 409 | D2 | As Throwaway with that upcoming booking, run Delete Account to the end. | The 409 "upcoming bookings" message appears and nothing is deleted. | Message text and code. | [x] pass (vc 4, 2026-11-05) |
| D2.19 | P14 (d) | D2 | A cancels the Throwaway's booking. As Throwaway: Delete, Warning, Reason, Delete, Farewell, Back to Login. Then try to log in as Throwaway on the iPhone and on a fresh Android login. | The Farewell shows; the iPhone and a fresh Android login both reject the Throwaway. | Outcome per device. | [x] pass (vc 4, 2026-11-05) |

### 4h Wrap up

| Row ID | Source row ID | Direction | Steps | Expected | Evidence to capture | Result |
|---|---|---|---|---|---|---|
| D2.20 | D-08, H2 | D2 | Log out the Throwaway on the Samsung and log A back in; run H2. | A is signed in; the build is unchanged. | H2 output. | [x] pass (vc 4, 2026-11-05) |

## 5. Samsung tier-1 setting passes (D-06)

Every tier-1 row of `15-DEVICE-CHECKLIST.md`, grouped by the device setting so each setting is switched on and off once. Samsung = B client with the iPhone as A for rows that need a peer (Direction `D1`); other rows are `Samsung`. Row ID = setting prefix plus the checklist ID; Source row ID = the checklist ID, plus a WR id where a 15-REVIEW-FIX device check is attached. Switch the setting on at the start of each subsection and off at its end (E7.6 restores everything at the close).

### 5.1 TalkBack (TB.)

Switch on TalkBack. The SC-4 runs are TB.L1, TB.B1, TB.C1 and TB.K1 (15-UAT test 1). Where a row fails with a known cause, log a fix-if-cheap bug with the fallback named in its Expected cell.

| Row ID | Source row ID | Direction | Steps | Expected | Evidence to capture | Result |
|---|---|---|---|---|---|---|
| TB.L1 | L1, 15-UAT test 1 | Samsung | Log in using only TalkBack (SC-4 run). | Login reads title as heading, email, password, Log in; one "Don't have an account? Sign Up" node; Log in says "In progress" while busy; lands on the right home. | Pass or fail note in the results file. | [-] not run: skipped by user decision (2026-11-05) |
| TB.L2 | L2 | Samsung | Enter a wrong password and submit. | The server message is spoken and focus lands on the password field. | Note. | [-] not run: skipped by user decision (2026-11-05) |
| TB.L3 | L3, WR-A03 | Samsung | On Login with email and password filled, listen to each field; also check one field on Signup and one on Settings. | TalkBack speaks the field name together with its value. If it fails, log fix-if-cheap with this fallback: add the name to the field semantics without hiding the value. | Note and which field. | [-] not run: skipped by user decision (2026-11-05) |
| TB.L7 | L7 | Samsung | Complete a password reset to Password Changed. | Focus lands on the "Password Changed" headline first; reading order sensible; Go to Login shows Login with the email pre-filled and an empty password. | Note. | [-] not run: skipped by user decision (2026-11-05) |
| TB.B1 | B1, 15-UAT test 1 | D1 | Book a free session using only TalkBack from Expert Profile to Request Sent (peer: iPhone A receives it; a person completes every step). | The whole booking completes by TalkBack alone. | Note. | [-] not run: skipped by user decision (2026-11-05) |
| TB.B3 | B3, WR-A04 | Samsung | On the booking form, move through the calendar and slots. Choose a different day with the same slot count. | Day cells read "Today, Selected, Available" or "No availability"; the month title is read before Previous and Next month; choosing a day announces how many times are free; the slots heading re-announces on a day change (WR-A04); Book Now says why it is disabled and "In progress" while busy. | Note. | [-] not run: skipped by user decision (2026-11-05) |
| TB.B4 | B4 | Samsung | Tap day cells on the 360 dp phone (about 45.7 dp wide). | Taps are reliable. If taps miss, log fix-if-cheap with this fallback: reduce the calendar card padding. | Note. | [-] not run: skipped by user decision (2026-11-05) |
| TB.B5 | B5 | Samsung | Open the topic sheet; close it; force a failed topic load. | Topic cards read as a radio group; closing returns focus to the Topic row; the failed load shows the named error and Retry inside the sheet. | Note. | [-] not run: skipped by user decision (2026-11-05) |
| TB.B8 | B8 | Samsung | Enter Request Sent. | TalkBack speaks "Request Sent!" first. | Note. | [-] not run: skipped by user decision (2026-11-05) |
| TB.C1 | C1, 15-UAT test 1 | D1 | Join a call from Upcoming Details using TalkBack (peer: iPhone A joins; SC-4 run). | The status line is read first; the order is status, name, timer, Mute, Speaker, Chat, Minimize, End call; "Connecting" is not announced twice. | Note; H5 log. | [-] not run: skipped by user decision (2026-11-05) |
| TB.C2 | C2 | D1 | During an active call, move across screens and tabs underneath. | The pill is the first TalkBack node on every screen. If it fails, log fix-if-cheap with this fallback: a focus request on the pill when a screen appears during a live call. | Note. | [-] not run: skipped by user decision (2026-11-05) |
| TB.C7 | C7 | D1 | After the force-stop and relaunch in PD.C7, listen to the booking detail. | TalkBack announces the one-time "You were on a call" note. | Note. | [-] not run: skipped by user decision (2026-11-05) |
| TB.K1 | K1, 15-UAT test 1 | D1 | Send a chat message using only TalkBack (peer: iPhone A; SC-4 run). | Conversations row, thread reading order, "You: {text}, {time}, Sent" or "Read"; the composer explains why Send is disabled; focus and the keyboard stay after Send. | Note. | [-] not run: skipped by user decision (2026-11-05) |
| TB.K2 | K2 | D1 | Focus a Conversations row. | One stop: "{name}, {last message}, {time}, {n} unread", child text not read again. | Note. | [-] not run: skipped by user decision (2026-11-05) |
| TB.K3 | K3 | D1 | Listen to a message bubble. | The bubble is not spoken twice. If it fails, log fix-if-cheap with this fallback: drop the Text from the merged bubble node. | Note. | [-] not run: skipped by user decision (2026-11-05) |
| TB.K4 | K4, WR-B02 | D1 | Peer (iPhone A) sends the same text twice; delete a failed send of mine. | A peer message arriving is announced once; identical back-to-back texts are re-announced; a newest own message from history is not announced on first composition; deleting a failed send of mine does not re-speak the peer message (WR-B02). | Note. | [-] not run: skipped by user decision (2026-11-05) |
| TB.K5 | K5 | D1 | On a failed bubble and the Report reason group, use TalkBack. | A failed bubble offers Retry and Delete; the Report reason group reads as a group. | Note. | [-] not run: skipped by user decision (2026-11-05) |
| TB.K10 | K10 | D1 | Open a blocked thread. | The blocked banner is announced. | Note. | [-] not run: skipped by user decision (2026-11-05) |
| TB.K11 | K11, WR-B01 | D1 | Network off, pull to refresh in Conversations and in a thread; toggle airplane mode inside a thread. | Rows and messages stay; one "Retry" snackbar; the reconnect failure path shows; the snackbar sits above the keyboard and the composer (WR-B01). | Note. | [-] not run: skipped by user decision (2026-11-05) |
| TB.Y12 | Y12 | Samsung | Run Accessibility Scanner over the four SC-4 flows (log in, book, call, chat). | Touch-target, contrast and label findings are logged as bug rows. | Finding count; screenshots outside the repo. | [-] not run: skipped by user decision (2026-11-05) |
| TB.Y14 | Y14 | Samsung | Trigger a "Retry" snackbar with TalkBack on. | The snackbar stays long enough to be reached and activated. | Note. | [-] not run: skipped by user decision (2026-11-05) |
| TB.SE1 | 16.1 UAT 5 | D1 | With TalkBack on, use the search field with text, the chips, the sort button and a result card. | Typed text is read back, chips announce selection, sort announces its value, a card reads as one button. | Note. | [-] not run: skipped by user decision (2026-11-05) |
| TB.PF1 | 16.5 UI-SPEC Accessibility Contract | D1 | With TalkBack on, open Payment History, Notifications, a save error, Send Feedback and the splash. | A Payment History row reads as one button with "Open booking"; a notification row reads once as a switch with title, subtitle and state; the save error is announced; Feedback labels precede their fields; the splash is one node announced once. | Note. | [-] not run: skipped by user decision (2026-11-05) |

Switch TalkBack off.

### 5.2 Font 200% (FS.)

Switch on Settings, Display, Font size at the largest, then Display size at the largest.

| Row ID | Source row ID | Direction | Steps | Expected | Evidence to capture | Result |
|---|---|---|---|---|---|---|
| FS.L4 | L4 | Samsung | Login on the small phone at 200% font with the keyboard. | Nothing clips; IME Next and Done move email to password to submit; autofill offers the saved login; Log in stays reachable above the keyboard. | Note. | [x] pass (vc 5, 2026-11-05) |
| FS.L5 | L5 | Samsung | Open Login, Forgot Password, Reset Code, Set New Password and Password Changed at 200% with the keyboard open. | Text wraps rather than clips. | Note. | [x] pass (vc 5, 2026-11-05) |
| FS.B2 | B2 | Samsung | Open Make a Booking at 200%. | Topic row, calendar, slots and notes wrap; Book Now and its reason stay reachable. | Note. | [x] pass (vc 5, 2026-11-05) |
| FS.B10 | B10 (re-scoped: live Payment Methods, its info sheet and a paid Request Sent, no sandbox) | D1 | Open Payment Methods and its info sheet; view the paid Request Sent from D1.18 at 200%. | All three reflow with no clipped text. | Note. | [x] pass (vc 5, 2026-11-05); whether a paid Request Sent was also viewed: not stated |
| FS.C3 | C3 | D1 | In a call at 200%, view the Connected pair, the final-5-minute banner and the Skype or Zoom external variant. | Names wrap to two lines; Mute, Speaker and End call never leave the screen. | Note. | [!] fail (BUG-34, vc 5): pre-join call screen at font 200% does not scroll; Mute, Speaker, Minimize and Chat unreachable behind End Call. Connected pair, final-5-minute banner and external variant not viewed |
| FS.K6 | K6 | D1 | At 200% open Conversations, a thread with a failed bubble, the blocked banner, Report & Block, the Report sheet and the Request Decision footer. | Row name and time wrap; bubbles and composer grow; footer buttons stack. | Note. | [x] pass (vc 5, 2026-11-05); blocked banner and Request Decision footer not viewed |
| FS.Y7 | Y7 | Samsung | Change the font scale while on a form in each flow. | Typed text kept, secrets empty, no crash. | Note; H4 crash count. | [x] pass (vc 5, 2026-11-05) |
| FS.Y12 | Y12 | Samsung | Run Accessibility Scanner at 200% on the four SC-4 flows. | Findings logged. | Finding count. | [-] not run: skipped by user decision (2026-11-05) |
| FS.FV1 | 16.3 UI-SPEC E3 backstop | D1 | At 200% open Profile, Saved Experts. | The Favorite Experts app bar title stays on one line with an ellipsis. | Note. | [x] pass (vc 5, 2026-11-05) |
| FS.FV2 | 16.3 UI-SPEC E4 backstop | D1 | At 200% view a Saved Experts card with a long name and several tags. | The card grows with no clipping and the photo stays 72 x 80 dp. | Note. | [x] pass (vc 5, 2026-11-05) |
| FS.FV3 | 16.3 UI-SPEC E5 backstop | D1 | At 200% tap the heart on a Saved Experts card whose expert has a long name. | The remove dialog title wraps without clipping and both buttons stay reachable. | Note. | [x] pass (vc 5, 2026-11-05) |
| FS.PN1 | 16.4 D-02 | Samsung | At 200% open the note editor with the keyboard up. Also open Booking Summary as B and as A with a saved note. | The header, the field and the counter stay reachable and nothing clips; the Save action stays reachable; the My Note card wraps without clipping on both summaries. | Note. | [x] pass (vc 5, 2026-11-05); client side only, expert side checked at FS.CP1 |
| FS.CP1 | 16.4 D-09 | D2 | At 200% open a Client Profile with a long topic. Also view the View Profile link on Expert Booking Summary. | Stat cells wrap, the amount stays visible and nothing clips; the View Profile link wraps or stays one line without clipping and keeps a 48 dp target. | Note. | [x] pass (vc 5, 2026-11-05) |
| FS.PH1 | 16.5 UI-SPEC E2 backstop | D1 | At 200% open Payment History with a row that has a long topic, a long expert name and "Authorized (not yet charged)". | The row wraps and the amount stays visible. | Note. | [x] pass (vc 5, 2026-11-05) |
| FS.NS1 | 16.5 UI-SPEC E4 backstop | D1 | At 200% open Notifications. | Every notification title and subtitle wraps, the switch stays visible, and the row is one 48 dp target. | Note. | [x] pass (vc 5, 2026-11-05) |
| FS.FB1 | 16.5 UI-SPEC E8 backstop | D1 | At 200% trigger the Feedback failure dialog with a long server message. | The dialog stays readable and OK is reachable. | Note. | [-] not run: skipped by user decision (2026-11-05) |
| FS.SP1 | 16.5 UI-SPEC E9 backstop | Samsung | At 200% cold start the app on the Samsung. Then check landscape on the resizable emulator (the phone is portrait-locked). | The splash's mark, wordmark and tagline stay visible and centered on the Samsung and in landscape on the emulator. | Note. | [x] pass (vc 5, 2026-11-05) on the Samsung; emulator landscape half not run |

Switch the font and display size back.

### 5.3 Process death and recreation (PD.)

Switch on Developer options, Don't keep activities. PD.C7 and PD.C8 carry the 15-REVIEW-FIX checks WR-A01 and WR-A02.

| Row ID | Source row ID | Direction | Steps | Expected | Evidence to capture | Result |
|---|---|---|---|---|---|---|
| PD.L8 | L8 | Samsung | On Forgot and Set New Password toggle dark mode, then background and return. | The typed email stays; the reset code and both passwords come back empty with no error text. | Note. | [x] pass (vc 5, 2026-11-05) |
| PD.B5 | B5 | Samsung | Open the topic sheet, change configuration, background and return. | The open sheet survives. | Note. | [x] pass (vc 5, 2026-11-05) |
| PD.B7 | B7 | Samsung | Background mid-booking and return. | Topic, day, month, slot, meeting type and notes kept; the Request Sent animation does not replay; system Back during an in-flight booking is ignored. | Note. | [x] pass (vc 5, 2026-11-05) |
| PD.C6 | C6 | D1 | Dark mode toggle, font-scale change and Don't keep activities during a call. | The call continues in the pill or layer; nothing joins a second time or opens the mic by itself. | H5 log (one join only). | [x] pass (vc 5, 2026-11-05) |
| PD.C7 | C7, WR-A01, WR-A02 | D1 | Start an in-app call, run `am force-stop` (not `am kill`), then tap a push before reopening the app. Separately, force-stop and relaunch directly. | The push destination wins and the "You were on a call" note does not cover it (WR-A01 and WR-A02); a direct relaunch shows the booking detail with Join Call, the one-time note, no auto-join and no microphone indicator; reopening does not show the note again. | H5 log; `pidof` after force-stop. | [x] pass (vc 5, 2026-11-05) on the direct-relaunch half; push half not testable on Android (force-stopped app: GCM broadcast result=CANCELLED, platform behavior; the swiped-away push path is proven by the batch-B smoke H6) |
| PD.C8 | C8, WR-A01, WR-A02 | D1 | With a call running, swipe the task away and tap the ongoing-call notification. | The app starts through Splash and auth, then lands in the right place; a background FCM wake-up that dies before UI keeps the marker. | Note. | [x] pass (vc 5, 2026-11-05); FCM wake-up-dies-before-UI marker sub-case not exercised |
| PD.K8 | K8 | D1 | Type an unsent message in a thread, background and return. | The draft is back in the same thread and the keyboard does not reopen. | Note. | [x] pass (vc 5, 2026-11-05) |
| PD.K9 | K9 | D1 | Fill the Report sheet, background and return. | Reason and details are restored. | Note. | [x] pass (vc 5, 2026-11-05) |
| PD.Y1 | Y1 | Samsung | Rotate the Samsung through the app, including during a call. | It stays portrait on every screen. | Note. | [x] pass (vc 5, 2026-11-05) |
| PD.Y3 | Y3 | Samsung | Walk the whole signup (as the Throwaway in D2.17 or a second throwaway), backgrounding at each step. | Same step and typed text kept; Verify Email returns with an empty code; password fields empty. | Note. | [-] not run: skipped by user decision (2026-11-05) |
| PD.Y4 | Y4 | Samsung | Walk Expert Profile to Request Sent. | Selections kept (specifics in PD.B7). | Note. | [x] pass (vc 5, 2026-11-05) |
| PD.Y5 | Y5 | D1 | Walk Conversations to a thread, type, background and return. | Draft kept (specifics in PD.K8). | Note. | [x] pass (vc 5, 2026-11-05) |
| PD.Y6 | Y6 | D1 | Join a call, background and return; then the force-stop case in PD.C7. | Nothing re-joins. | H5 log. | [x] pass (vc 5, 2026-11-05) |
| PD.Y7 | Y7 | Samsung | Toggle dark mode while on a form in each flow. | Typed text kept, secrets empty, no crash. | Note; H4 crash count. | [x] pass (vc 5, 2026-11-05) |
| PD.Y11 | Y11 | Samsung | On the Play build, log in, then from a deep screen (booking detail, a chat thread) background and return. Confirm login and the realtime connection work under R8. | The deep screen is restored. If it lands on the start screen, log a bug (add a NavKey keep rule). | Note. | [x] pass (vc 5, 2026-11-05) |

Switch Don't keep activities off.

### 5.4 IME and insets (IM.)

| Row ID | Source row ID | Direction | Steps | Expected | Evidence to capture | Result |
|---|---|---|---|---|---|---|
| IM.L2 | L2 | Samsung | Wrong password with the keyboard open. | Focus lands on the password field above the keyboard. | Note. | [x] pass (vc 5, 2026-11-05) |
| IM.L4 | L4 | Samsung | Login with the keyboard open. | Log in stays reachable above the open keyboard. | Note. | [x] pass (vc 5, 2026-11-05) |
| IM.L5 | L5 | Samsung | The four recovery screens with the keyboard open. | Nothing clips. | Note. | [x] pass (vc 5, 2026-11-05) |
| IM.L6 | L6 | Samsung | Reset code with SMS or OTP autofill; submit with errors. | The 6-digit code autofills; focus moves to the first failing field. | Note. | [x] pass (vc 5, 2026-11-05); whether the 6-digit code was offered as an autofill suggestion: not stated |
| IM.B6 | B6 | Samsung | Type in the booking Notes field with the real keyboard. | Caret stays visible; tapping outside clears focus; a drag dismisses the keyboard; Book Now can be scrolled above the keyboard. | Note. | [x] pass (vc 5, 2026-11-05) |
| IM.C5 | C5 | D1 | During a call, switch between gesture and 3-button navigation. | The pill sits correctly over both and clears a side display cutout. | Note. | [-] not run: skipped by user decision (2026-11-05) |
| IM.K7 | K7, WR-B01 | D1 | Open a thread, show the keyboard, trigger a failed refresh. | The IME pushes the thread up with the cutout and 3-button navigation; the cursor is stable; the long-press Delete menu works; the failed refresh snackbar sits above the keyboard and the composer (WR-B01). | Note. | [-] not run: skipped by user decision (2026-11-05) |
| IM.K8 | K8 | D1 | Type an unsent message and background (Don't keep activities is off now). | The draft is back and the keyboard does not reopen. | Note. | [-] not run: skipped by user decision (2026-11-05) |
| IM.K9 | K9 | D1 | Open the Report sheet with the real keyboard. | The input stays visible. | Note. | [-] not run: skipped by user decision (2026-11-05) |
| IM.Y8 | Y8 | Samsung | Keyboard open over the Samsung cutout. | No text or control sits under the status bar, a side cutout or the keyboard. | Note. | [-] not run: skipped by user decision (2026-11-05) |
| IM.Y9 | Y9 | Samsung | Switch between gesture and 3-button navigation. | Snackbars, sticky footers and the call pill clear the bar in both. | Note. | [-] not run: skipped by user decision (2026-11-05) |
| IM.Y16 | Y16 | Samsung | Walk the four SC-4 flows. | No text or control sits under the status bar, a side cutout or the navigation bar. | Note. | [-] not run: skipped by user decision (2026-11-05) |

### 5.5 Plain (PL.)

| Row ID | Source row ID | Direction | Steps | Expected | Evidence to capture | Result |
|---|---|---|---|---|---|---|
| PL.C9 | C9 | D1 | Deny POST_NOTIFICATIONS on API 33+, then join a call. | The call still runs and the pill remains. | Note; H5 log. | [-] not run: skipped by user decision (2026-11-05) |
| PL.K12 | K12 | D1 | Use the jump-to-latest control and watch the list move-to-top animation. | They feel right. | Note. | [-] not run: skipped by user decision (2026-11-05) |

### 5.6 Emulator (EM., D-10)

| Row ID | Source row ID | Direction | Steps | Expected | Evidence to capture | Result |
|---|---|---|---|---|---|---|
| EM.1 | 09-HUMAN-UAT 3, P9 Android 17 | emulator | On `Medium_Phone_API_37.0` run `adb shell cmd audio set-enable-hardening enable`, join a call and background it. | Audio and focus continue; the targetSdk 37 local-network permission does not block Agora. | The adb output and call log lines. | [-] not run: skipped by user decision (2026-11-05) |

## 6. Run if time allows (triaged by D-07, not blocking)

Large-window, tier 2-4 and other deferred rows. Failures are triaged by D-07 and do not block automatically. Rows left `[ ]` get "not run in Phase 16: post-release" at close-out. Compact form: the Source row ID names the original checklist or STATE item; the Check is one line.

### 6.1 Large window (emulator, outside the D-06 tier-1 variants)

Resizable AVD from the android-37.0 image (RESEARCH Open Question 3); confirm scope with the user at sign-off.

| Row ID | Source row ID | Check (one line) | Result |
|---|---|---|---|
| LW.L9 | L9 | Login and the four recovery screens are centered at 600 dp, app bars span the window, nothing clipped. | [-] not run in Phase 16: post-release (D-06) |
| LW.B9 | B9 | Make a Booking and Request Sent capped at 600 dp; resizing mid-booking keeps every selection and the notes. | [-] not run in Phase 16: post-release (D-06) |
| LW.C4 | C4 | At 900x400 Mute, Speaker and End call show without scrolling; a narrow short window scrolls; phone layout unchanged. | [-] not run in Phase 16: post-release (D-06) |
| LW.C10 | C10 | Resize the window during a call: no clipped controls, call state kept. | [-] not run in Phase 16: post-release (D-06) |
| LW.K13 | K13 | Conversations rows and Report & Block capped at 600 dp. | [-] not run in Phase 16: post-release (D-06) |
| LW.Y2 | Y2 | Resize through phone, 900x400 and 900 dp square: content never clipped, input kept, max width 600 dp. | [-] not run in Phase 16: post-release (D-06) |

### 6.2 Tier 2-4 checklist rows

| Row ID | Source row ID | Check (one line) | Result |
|---|---|---|---|
| T2.S1 | S1 | All 12 signup surfaces at 200% font with the keyboard open: no clipped text, buttons stack. | [-] not run in Phase 16: post-release (D-06) |
| T2.S2 | S2 | Country, language and expertise search sheets stay visible above the keyboard and the list scrolls. | [-] not run in Phase 16: post-release (D-06) |
| T2.S3 | S3 | TalkBack on the Profile Photo crop offers and performs the custom zoom and move actions. | [-] not run in Phase 16: post-release (D-06) |
| T2.S4 | S4 | Don't keep activities at each signup step: same step, typed text kept, Verify Email code empty. | [-] not run in Phase 16: post-release (D-06) |
| T2.S5 | S5 | Signup screens capped at 600 dp on a large window; app bars full width. | [-] not run in Phase 16: post-release (D-06) |
| T2.S6 | S6 | Review and Overview announce in-flight and error states; Publish reachable; retry works. | [-] not run in Phase 16: post-release (D-06) |
| T2.E1 | E1 | Explore, the View all sheet and Expert Profile at 200%: nothing clips, Book Session reachable. | [-] not run in Phase 16: post-release (D-06) |
| T2.E2 | E2 | Category sheet search and results stay usable with the keyboard open. | [-] not run in Phase 16: post-release (D-06) |
| T2.E3 | E3 | Pull to refresh feels normal; a failed refresh keeps all sections and shows one Retry snackbar. | [-] not run in Phase 16: post-release (D-06) |
| T2.E4 | E4 | With no categories the copy reads "No categories yet" and "Check back soon."; decide if the copy is right. | [-] not run in Phase 16: post-release (D-06) |
| T2.E5 | E5 | Expert Profile with TalkBack: "Book Session with {name}", sections in order, headings navigate. | [-] not run in Phase 16: post-release (D-06) |
| T2.E6 | E6 | Expert Profile on a large window: hero glow full width, sections capped at 600 dp. | [-] not run in Phase 16: post-release (D-06) |
| T2.M1 | M1 | My Bookings, Upcoming Details with a reschedule card, Cancel step 2 and Booking Summary at 200%. | [-] not run in Phase 16: post-release (D-06) |
| T2.M2 | M2 | Failed refresh shows exactly one "Couldn't refresh" snackbar with Retry; TalkBack announces it. | [-] not run in Phase 16: post-release (D-06) |
| T2.M3 | M3 | ErrorState and EmptyState headings navigate as headings and announce once; long headings wrap at 200%. | [-] not run in Phase 16: post-release (D-06) |
| T2.M4 | M4 | Kill the process on Cancel confirm and reopen: the confirm step shows and nothing cancels until Confirm. | [-] not run in Phase 16: post-release (D-06) |
| T2.M5 | M5 | A JPY booking cancel shows the refund snackbar with no decimals (no JPY topic in production: likely not run). | [-] not run in Phase 16: post-release (D-06) |
| T2.M6 | M6 | Decline-reschedule sheet keeps the reason, counter and Decline button visible with the keyboard; text survives a kill. | [-] not run in Phase 16: post-release (D-06) |
| T2.M7 | M7 | My Bookings, Upcoming Details, Booking Summary and Cancel capped at 600 dp on a large window. | [-] not run in Phase 16: post-release (D-06) |
| T2.M8 | M8 | After Retry succeeds on a detail screen, note where TalkBack focus lands (not implemented, log only). | [-] not run in Phase 16: post-release (D-06) |
| T2.N1 | N1 | Notifications list, Write Review, Booking Summary with Feedback and the Past card pill at 200%. | [-] not run in Phase 16: post-release (D-06) |
| T2.N2 | N2 | TalkBack reads the star group, the nested Past card action and the notification rows correctly. | [-] not run in Phase 16: post-release (D-06) |
| T2.N3 | N3 | Offline foreground push on Notifications: rows stay, one Retry snackbar, Retry reloads. | [-] not run in Phase 16: post-release (D-06) |
| T2.N4 | N4 | Notifications and Write Review on a large window: centered 600 dp column, footer background spans. | [-] not run in Phase 16: post-release (D-06) |
| T2.X1 | X1 | Topics, Topic Editor with an error, an overlapping day banner and the Time Range sheet at 200%. | [-] not run in Phase 16: post-release (D-06) |
| T2.X2 | X2 | TalkBack reads the Time Range sheet; the sheet keeps state after recreation and process death. | [-] not run in Phase 16: post-release (D-06) |
| T2.X3 | X3 | Topic Editor focus moves to the first invalid field; description stays above the keyboard; error survives rotation-equivalent. | [-] not run in Phase 16: post-release (D-06) |
| T2.X4 | X4 | Dashboard, request card, Request Decision, Decline sheet, Confirmed, Upcoming card and Cancel step 2 at 200%. | [-] not run in Phase 16: post-release (D-06) |
| T2.X5 | X5 | TalkBack on the Decline and Accept sheets; Booking Confirmed starts on its headline. | [-] not run in Phase 16: post-release (D-06) |
| T2.X6 | X6 | Tab return and realtime events reload without flicker; Confirmed with animations off shows its final state. | [-] not run in Phase 16: post-release (D-06) |
| T2.X7 | X7 | Decline and Accept sheets survive Don't keep activities with text kept and nothing sent. | [-] not run in Phase 16: post-release (D-06) |
| T2.X8 | X8 | Offline refresh on Dashboard, Bookings, Requests, Topics and Availability: one Retry snackbar each, above the bar and keyboard. | [-] not run in Phase 16: post-release (D-06) |
| T2.X9 | X9 | Expert screens capped and centered at 600 dp with full-width app bars on a large window. | [-] not run in Phase 16: post-release (D-06) |
| T2.T1 | T1 | Both hubs, settings, Account Security, Delete Account, Help, Payouts, Activity and Tax at 200%. | [-] not run in Phase 16: post-release (D-06) |
| T2.T2 | T2 | TalkBack announces the save status, password toggle state, FAQ state and selected delete reason. | [-] not run in Phase 16: post-release (D-06) |
| T2.T3 | T3 | The four forms survive Don't keep activities; TIN, account identifier and deletion password come back empty. | [-] not run in Phase 16: post-release (D-06) |
| T2.T4 | T4 | The Tax saved snackbar sits above the gesture bar and the keyboard. | [-] not run in Phase 16: post-release (D-06) |
| T2.T5 | T5 | Offline pull to refresh on Payouts, Activity and Help: content stays, one Retry snackbar. | [-] not run in Phase 16: post-release (D-06) |
| T2.T6 | T6 | A wrong current password keeps the cursor and keyboard; a server error dialog keeps all typed values. | [-] not run in Phase 16: post-release (D-06) |
| T2.T7 | T7 | The Payment Methods loading skeleton matches the loaded rows without a jump. | [-] not run in Phase 16: post-release (D-06) |
| T2.T8 | T8 | After deleting the Throwaway, TalkBack announces the Farewell headline first. | [-] not run in Phase 16: post-release (D-06) |
| T2.T9 | T9 | Log out and Delete dialogs survive Don't keep activities and never auto-confirm. | [-] not run in Phase 16: post-release (D-06) |
| T2.T10 | T10 | Settings screens centered at 600 dp with full-width app bars on a large window. | [-] not run in Phase 16: post-release (D-06) |
| T2.Y10 | Y10 | With animation scales off, Request Sent, Booking Confirmed and sheets show their final state at once. | [-] not run in Phase 16: post-release (D-06) |
| T2.Y13 | Y13 | On a throttled network, skeleton to content and error to retried content show no jump or flicker. | [-] not run in Phase 16: post-release (D-06) |
| T2.Y15 | Y15 | After Don't keep activities, scroll position is restored where data is available (best effort; note regressions). | [-] not run in Phase 16: post-release (D-06) |

### 6.3 Other deferred STATE items not in the matrix

| Row ID | Source row ID | Check (one line) | Result |
|---|---|---|---|
| OT.P3-2 | P3 SC-2 | Recovery flow on the Play build: Forgot Password to Password Changed, Back behavior, autofill. | [-] not run in Phase 16: post-release (D-06) |
| OT.P3-4 | P3 SC-4 | Airplane mode 10 s then off: `Realtime` returns to Connected within about 2 s of the network. | [-] not run in Phase 16: post-release (D-06) |
| OT.P4-1 | P4 SC-1 | Full signup with a new email (no username or phone) through Publish to the client home. | [-] not run in Phase 16: post-release (D-06) |
| OT.P4-2 | P4 SC-2 | Verify Email sends nothing on open; Resend disabled for 30 s; wrong code shows the inline error. | [-] not run in Phase 16: post-release (D-06) |
| OT.P4-3 | P4 SC-3 | Quit mid-setup and relaunch to the hub with completed steps checked. | [-] not run in Phase 16: post-release (D-06) |
| OT.P4-4 | P4 SC-4 | After publish: carousel shows once; mode switch to Expert and back keeps mode after relaunch. | [-] not run in Phase 16: post-release (D-06) |
| OT.P5-1 | P5 SC-1 | Explore hero, Trending Categories, Featured Topics, Today's Sessions, chip filter and View all. | [-] not run in Phase 16: post-release (D-06) |
| OT.P5-2 | P5 SC-2 | Category cache: View all shows from cache in airplane mode after a relaunch. | [-] not run in Phase 16: post-release (D-06) |
| OT.P5-3 | P5 SC-3 | Expert Profile hero, sections, Book Session and the unavailable-profile message. | [-] not run in Phase 16: post-release (D-06) |
| OT.P10-a | P10 (a) | Topics: create, edit, delete a paid topic; price below the floor shows the live error. | [-] not run in Phase 16: post-release (D-06) |
| OT.P10-b | P10 (b) | Availability: overlapping slots show the banner and disable Save; Save during a call is refused. | [-] not run in Phase 16: post-release (D-06) |
| OT.P10-c | P10 (c) | Timezone: a UTC-zone expert on a non-UTC device sees the device zone filled and stored. | [-] not run in Phase 16: post-release (D-06) |
| OT.P10-e | P10 (e) | French (Canada) locale: typing "12,50" saves 1250 minor units. | [-] not run in Phase 16: post-release (D-06) |
| OT.P10-g | P10 (g) | Edit Expertise offline shows the load error; a topic with 45 or 600 minutes keeps its duration after a title edit. | [-] not run in Phase 16: post-release (D-06) |
| OT.P14-b | P14 (b) | Activity amounts match the ledger, statuses, paging past 20 rows, preview and See all. | [-] not run in Phase 16: post-release (D-06) |
| OT.P14-c | P14 (c) | Hubs and settings auto-save; avatar pick and crop; languages and country persist; change password. | [-] not run in Phase 16: post-release (D-06) |
| OT.P14-e | P14 (e) | Tax & Payout Info saves without and with a TIN. | [-] not run in Phase 16: post-release (D-06) |
| OT.PP | P4 visual bullet, P14 (c) | Photo picker and avatar crop on API 26-32. `[-] not run: no API 26-32 AVD`. | [-] not run: no API 26-32 AVD |

## 7. End of run: money sweep and cleanup

Refund and hold checklist, run after the last row. Paid cancel, decline and expiry rows are voids (no charge, no refund, no fee); only completed paid sessions are captured, and those are refunded by hand (D-04).

**Deviation from D-04 (user decision 2026-11-05):** the two captures (D1.25 booking 5449f1b7, PaymentIntent C3eR3Z; D2.12 booking 43d0562b, PaymentIntent See2oK; CA$5.00 each) were NOT refunded. The user owns both the client and expert accounts and the platform, so the money stays with them. Stripe keeps its processing fee either way, and the application fee was not refunded. These two charges remain in the earnings, payout and revenue records of the expert account and the platform.

| Row ID | Source row ID | Direction | Steps | Expected | Evidence to capture | Result |
|---|---|---|---|---|---|---|
| E7.1 | D-04 | D1 | Open "Paid bookings created" and fill the Actual end of every row. | Every row has an actual end state. | The completed table. | [x] pass (2026-11-05): every Paid bookings row has an actual end: 8 voids, 2 captures |
| E7.2 | D-04, RESEARCH Pitfall 2 | D1 | In the app, cancel or decline every leftover paid request (holds last up to about 7 days). | No paid booking is left in pending approval. | Booking ids first 8. | [x] pass (2026-11-05): no paid request pending in the app, user |
| E7.3 | D-04, H10 | D1 | Open the Stripe dashboard on A's connected account (live mode). | No matrix PaymentIntent is in requires_capture. | Count of requires_capture matrix PIs (expect 0). | [x] pass (2026-11-05): 0 matrix PaymentIntents in requires_capture; WIUsEq, gAi5kj, BFQEmT, 53ka5k, b2m66n, lTnYmn, KJcrmX, Hlqzg7 read as canceled in the Stripe dashboard by the user |
| E7.4 | D-04, RESEARCH A3 | D1 | Refund each captured matrix charge in full in the dashboard on the connected account. | Each refund shows succeeded; Stripe keeps its processing fee; the application fee line was refunded too (RESEARCH A3) or the gap is noted. | Refund date per PI last 6, written in the Paid bookings table. | [-] not done by user decision 2026-11-05: the 2 captures (C3eR3Z, See2oK, CA$5.00 each) are kept, not refunded, because the user owns both the client and expert accounts and the platform; Stripe keeps its processing fee either way; application fee not refunded. Deviation from D-04 |
| E7.5 | P14 (d), D-03 | D1 | Check the accounts. | The Throwaway is deleted; A and B are active; B's saved live cards are kept or removed as the user prefers. | Confirmed aloud. | [x] pass (2026-11-05): the first Throwaway was deleted at D2.19; a second throwaway created for the batch-B D2.17 re-test (BUG-30, BUG-31) was deleted by the user via Delete Account (TalkBack not used, T2.T8 not run); client and expert accounts logged in repeatedly during batch B (no separate login check after the deletion); the user keeps the client account's saved live cards |
| E7.6 | D-13 | Samsung | Put the Samsung settings back to normal. | Font and display size default, TalkBack off, Don't keep activities off, animation scales on, navigation mode as before. | Confirmed aloud. | [x] pass (2026-11-05): font 1.0, display density reset, animation scales 1.0, Don't keep activities off, TalkBack off, 3-button navigation is the user's normal mode; the spike stripe and agora packages were uninstalled at the user's request, only com.uptrendinvestments.clickmeapp (installer com.android.vending) remains |
| E7.7 | D-04, T-16-02 | D1 | Restate the money rule. | Never run `scripts/live-cutover-cleanup.js`; never use a bulk refund script; refunds go through the Stripe dashboard. | None. | [x] pass (2026-11-05): no live-cutover or bulk script was run; voids and any refund decisions went through the Stripe dashboard |

## Paid bookings created (D-04)

Rows are added from the `PAID` lines of each results file, at the moment the booking is made. Only completed sessions are captured; every other paid booking ends as a void.

| # | Row | Direction | Booking (first 8 of id) | PaymentIntent (last 6) | Amount | Expected end (void or capture) | Actual end | Refunded (date) |
|---|---|---|---|---|---|---|---|---|
| 1 | D1.18 | D1 | efd9a2e1 | WIUsEq | CA$5.00 expected, amount not read | void | void: Stripe read as cancelled by the user, 2026-11-03, Uncaptured before | not applicable |
| 2 | D1.23 | D1 | not read | gAi5kj | CA$5.00 expected, amount not read | void | void: Stripe read as cancelled by the user, 2026-11-03 | not applicable |
| 3 | D1.20 | D1 | 5e6f4274 | BFQEmT | CA$5.00 expected, amount not read | void | void: reported cancelled by the user, 2026-11-03; Section 7 confirms in Stripe | not applicable |
| 4 | D1.RS4 (hold 1) | D1 | 3d5b80b2 | 53ka5k | CA$5.00 expected, amount not read | void | void: 53ka5k read as canceled by the user, 2026-11-04; the moved booking was then cancelled before its T-24h charge (payment_status canceled) | not applicable |
| 5 | D1.24 part 2 (hold 2) | D1 | e8ba8e6a | b2m66n | CA$5.00 expected, amount not read | void | void: cancelled 2026-11-04 by a wrong-booking tap (user slip), payment_status canceled; D1.24 part 2 not tested on it | not applicable |
| 6 | D1.24 part 2, D1.RS5 (hold 3; reused for D1.RS3, D1.PH1, D1.PH4) | D1 | 165e4b0f | lTnYmn | CA$5.00 expected, amount not read | void | void: lTnYmn read as canceled in Stripe by the user, 2026-11-04 (D1.PH4) | not applicable |
| 7 | D1.25 | D1 | 5449f1b7 | C3eR3Z | CA$5.00 (captured, read by the user) | capture | captured CA$5.00 by 2026-11-04 17:15; Payment History reads Paid | not refunded: kept by user decision 2026-11-05 (user owns both accounts) |
| 8 | D2.09 | D2 | 277cd5ac | KJcrmX | CA$5.00 expected, amount not read | void | void: canceled in Stripe, no charge, refund or fee, per the user 2026-11-04 (D2.10) | not applicable |
| 9 | D2.12 | D2 | 43d0562b | See2oK | CAD 500 (Stripe object read 2026-11-05), application fee 50 | capture | succeeded CAD 500 on 2026-11-05, direct charge on the expert's connected account | not refunded: kept by user decision 2026-11-05 (user owns both accounts) |
| 10 | D2.RS2, D2.RS4 | D2 | not given (never read) | Hlqzg7 | CA$5.00 expected, amount not read | void | void: canceled in Stripe per the user, 2026-11-05 | not applicable |

## 8. Sign-off (D-09)

Claude filled the counts at close-out (2026-11-05); the user confirms. Phase 16 cannot close until the confirmation line below is filled (D-09). Counts are by Result cell; a pass can carry a caveat, listed after the counts.

- **Direction 1 (QA-02) counts:** pass 64 / fail 1 / not run 6 (D1.* rows, 71). Fail: D1.05 (BUG-13, backend, the Read label does not update live). Not run: D1.12, D1.21, D1.22, D1.24 (part 1 passed), D1.RS5, D1.PH5.
- **Direction 2 (QA-03) counts:** pass 35 / fail 2 / not run 3 (SW.* and D2.* rows, 40). Fail: SW.1 (BUG-24, ios: logout leaves the push token row) and D2.RS1 (BUG-25, ios: an iOS client cannot answer an expert reschedule, rated critical by the user). Not run: D2.11, D2.14, D2.15.
- **Tier-1 counts (Section 5):** pass 33 / fail 1 / not run 36 (70 rows). Fail: FS.C3 (BUG-34, android, fix-if-cheap). Not run: 36 rows skipped by user decision 2026-11-05 (the 23 TalkBack rows, FS.FB1, FS.Y12, PD.Y3, IM.K7, IM.K8, IM.K9, IM.C5, IM.Y8, IM.Y9, IM.Y16, PL.C9, PL.K12, EM.1).
- **Pre-flight and release smoke (Sections 0 and 1):** P0.* pass 9 / fail 0 / not run 1 (P0.8); R1.* pass 4 / fail 0 / not run 1 (R1.2).
- **Passes that carry a caveat:** D1.23 (challenge and JPY parts not run), D1.25 (Railway confirm-connection count not checked), D2.04 (expired-request part not run, none exists), D2.08 (user report only, the Play build logs no Realtime lines), D2.16 (weak proof, the account was already complete), D2.RS2 (the iOS accept was unreachable because of BUG-25), PD.C7 (direct relaunch only; the push half is not testable on Android), PD.C8 (FCM wake-up-dies-before-UI sub-case not exercised), FS.B10 (a paid Request Sent was not confirmed), FS.K6 (blocked banner and Request Decision footer not viewed), FS.SP1 (Samsung half only), IM.L6 (autofill suggestion not stated), D1.RS8 (the case tested was a slot exactly one hour before another booking).
- **Open blocking bugs (must be 0):** android-owned blocking bugs open: 0 (BUG-22, BUG-23 and BUG-34 are fix-if-cheap and end as post-release). One ios-owned bug of blocking severity is open: BUG-25 (a proposed reschedule cannot be answered on the iOS client Upcoming Booking screen), reported to iOS as critical; user decision 2026-11-05: it does not block the Android release (Android ships on its own). Cross-repo fixes verified on the device: none were needed.
- **3DS outcome (D-02):** not run: no live 3DS challenge was passed on Android or iOS. The user's cards showed no setup challenge (the Mastercard 2018 gave none), so the P0.9 plan, changed by the user on 2026-11-02, accepted not running D1.21 and D2.11; D1.22 (declining card) was also not run for lack of a failing card. QA-02 and QA-03 "test cards including 3DS" is therefore not met with a live challenge. Only the confirm-time Stripe paths that need no challenge were exercised (D1.18-D1.20, D1.23, D2.09-D2.12).
- **Money result (D-04):** all 8 holds voided; 2 captures (CA$5.00 each) kept, not refunded, by user decision 2026-11-05; application fee not refunded. This deviates from D-04 (every capture refunded): the user owns both accounts and the platform, so the money stays with them; the two charges remain in earnings, payout and revenue records, and Stripe keeps its processing fee either way. 0 matrix PaymentIntents are in requires_capture. Section 7 ticked, with E7.4 recorded as not done by user decision.
- **TalkBack (POL-04, Phase 15 SC-4):** not confirmed on a device. The user skipped all 23 TalkBack rows on 2026-11-05 (option 1 of 3 offered); Phase 15 semantics are proven by Robolectric tests only, and 15-UAT tests 1, 3 and 4 stay unconfirmed. The accessibility-scanner pass (Y12) was skipped with them.
- **Accepted gaps (D-10):**
  - The user is the only internal tester, so the SC-5 and REL-05 "few real users on a mix of devices" part is accepted at release (carries Phase 13 D-22). The emulator covered the Android 17 row only, and EM.1 was not run.
  - JPY is not runnable (no JPY topic in production).
  - Large-window rows are outside D-06 (Section 6.1); they were not run.
  - Tier 2-4 rows not run (Section 6.2 and 6.3), each marked post-release.
  - Photo picker on API 26-32 (no AVD).
  - No live 3DS challenge (above); no declining-card run.
  - TalkBack and Accessibility Scanner not run (above).
  - D-04 deviation: the 2 captures (C3eR3Z, See2oK) were kept, not refunded, by user decision 2026-11-05 (the user owns both accounts and the platform); they remain in earnings, payout and revenue records.
  - Payouts onboarding (D2.14, D2.15) not run: the expert account was already onboarded and a new Connect account is forbidden (D-05).
  - Saved-card recovery for a reschedule (D1.24 part 2, D1.RS5) is unreachable live (BUG-32 runbook premise, BUG-27 backend).
  - Open iOS and backend bugs travel on the STATE report list: BUG-13, BUG-14, BUG-15, BUG-16, BUG-17, BUG-24, BUG-25, BUG-26, BUG-27, BUG-28 (Pending Todos).
  - Android post-release items: BUG-22 and BUG-34 (call controls at font 200%), BUG-23 (review pill after restart), BUG-04, BUG-20, BUG-21, BUG-18, BUG-19 and the BUG-28 Android half.
- **Open must-pass rows (Sections 0-5 not a pass), with result and reason:**
  - 23 TalkBack rows (TB.L1, TB.L2, TB.L3, TB.L7, TB.B1, TB.B3, TB.B4, TB.B5, TB.B8, TB.C1, TB.C2, TB.C7, TB.K1, TB.K2, TB.K3, TB.K4, TB.K5, TB.K10, TB.K11, TB.Y12, TB.Y14, TB.SE1, TB.PF1): not run, skipped by user decision 2026-11-05. POL-04 and Phase 15 SC-4 are not confirmed on a device.
  - P0.8: not run: skipped by the user 2026-11-02, who states access works; A Connect status not read
  - R1.2: not run: release build logs no state= lines (16-02 deviation); realtime proven by P0.5 and later chat rows
  - D1.05: fail (BUG-13, vc 3)
  - D1.12: not run: no headset
  - D1.21: not run: 3DS accept not run per P0.9 (no challenging card)
  - D1.22: not run: no failing card per P0.9
  - D1.24: not run: part 1 (accept, decline with and without reason, free bookings) passed per user 2026-11-03; saved-card recovery unreachable live, runbook premise wrong (card always saved, clone cached), skipped by user 2026-11-04
  - D1.RS5: not run: unreachable live: both apps always save the card and the booking stores it, so PAYMENT_METHOD_REQUIRED cannot be raised; runbook premise wrong
  - D1.PH5: not run: no confirmed ended booking in Payment History on the client account, user 2026-11-03
  - SW.1: fail (BUG-24, vc 4)
  - D2.11: not run: 3DS accept not run per P0.9 (no challenging card)
  - D2.RS1: fail (BUG-25, vc 4)
  - D2.14: not run: expert account already onboarded, Payouts shows the ready state with no Set up payouts entry; a new Connect account is forbidden (D-05)
  - D2.15: not run: no onboarding link reachable on the onboarded expert account, same reason as D2.14
  - FS.C3: fail (BUG-34, vc 5): pre-join call screen at font 200% does not scroll; Mute, Speaker, Minimize and Chat unreachable behind End Call. Connected pair, final-5-minute banner and external variant not viewed
  - FS.Y12: not run: skipped by user decision 2026-11-05
  - FS.FB1: not run: skipped by user decision 2026-11-05
  - PD.Y3: not run: skipped by user decision 2026-11-05
  - IM.C5: not run: skipped by user decision 2026-11-05
  - IM.K7: not run: skipped by user decision 2026-11-05
  - IM.K8: not run: skipped by user decision 2026-11-05
  - IM.K9: not run: skipped by user decision 2026-11-05
  - IM.Y8: not run: skipped by user decision 2026-11-05
  - IM.Y9: not run: skipped by user decision 2026-11-05
  - IM.Y16: not run: skipped by user decision 2026-11-05
  - PL.C9: not run: skipped by user decision 2026-11-05
  - PL.K12: not run: skipped by user decision 2026-11-05
  - EM.1: not run: skipped by user decision 2026-11-05

Write-back table (every deferred STATE item and checklist source has a result; "vc N" is the build the row passed on):

| Source item | Runbook rows | Result |
|---|---|---|
| 02 spike inputs file | Sections 1-5 | superseded by Phase 16 (D-08); the matrix ran on the Play build with no spike inputs |
| 02-09 Stripe spike | D1.15-D1.20, D1.23; D1.21, D1.22, D2.11 | superseded by Phase 16 (D-08): card save, hold, cancel and retry rows pass vc 3-4; live 3DS not run (D1.21, D2.11: no challenging card per P0.9), D1.22 not run |
| 02-15, 02-16 | 16-02, R1.1 | closed by 16-02: signed vc 1 R8 cold start ok, Play internal-track upload (REL-03); login on the Play build pass vc 3; forced refresh and the ESP round-trip not separately run |
| P3 SC-1 Login | R1.1, IM.L2 | pass vc 3 and vc 5; the airplane-mode dialog was not separately run |
| P3 SC-2 Recovery | FS.L5, IM.L5, IM.L6, OT.P3-2, TB.L7 | partial: recovery screens with the keyboard at 200% and code-entry focus pass vc 5; the full recovery flow (OT.P3-2) and the TalkBack order (TB.L7) not run, post-release |
| P3 rotation line | FS.L4, FS.L5, IM.L4, PD.L8 | pass vc 5 |
| P3 SC-3 Events | R1.2, D1.03, D2.08 | R1.2 not run as written (the release build logs no state= lines); realtime exercised functionally: D1.03 pass vc 3, D2.08 pass vc 4; D1.05 fail BUG-13 (backend, Read label not live) |
| P3 SC-4 Reconnect | OT.P3-4 | not run, post-release |
| P3 Lifecycle | R1.2 | not run (the release build logs no state= lines), post-release |
| P3 Release smoke | R1.1, PD.Y11 | pass vc 3 and vc 5 (login and realtime on the signed Play build under R8; deep screen restored) |
| P4 SC-1 Signup | OT.P4-1, D2.17 | OT.P4-1 not run, post-release; the Throwaway signup through Profile Setup to Publish in D2.17 passed vc 4 and was re-checked vc 5 (BUG-30, BUG-31 verified) |
| P4 SC-2 Verify Email | OT.P4-2 | not run, post-release |
| P4 SC-3 Resume | OT.P4-3 | not run, post-release |
| P4 SC-4 After publish | OT.P4-4 | not run, post-release |
| P4 photo picker and visual bullets | OT.PP, T2.S1-T2.S3, D2.17 | photo picker on API 26-32 not run (no AVD); T2.S1-T2.S3 not run, post-release; the crop preview fix (BUG-31) verified vc 5 |
| P5 SC-1 Explore | OT.P5-1 | not run, post-release (Explore used in D1.01 and D1.SE1-D1.SE5, pass vc 3) |
| P5 SC-2 Category cache | OT.P5-2 | not run, post-release |
| P5 SC-3 Expert Profile | OT.P5-3 | not run, post-release (Expert Profile used in D1.01, D1.16 and D1.FV1, pass vc 3) |
| P5 visual and accessibility | T2.E1-T2.E6 | not run, post-release |
| P7 Stripe key and mode | P0.9, Section 7 | ran live with real cards on the Play build under the D-01 money plan (option-a, 2 captures); the sandbox path was not used; the money result is in 16-MATRIX-RUNBOOK.md Section 8 |
| P7 SC-1 Payment Methods | P0.4, D1.15 | pass vc 3 |
| P7 SC-2 Short-lead | D1.17, D1.18, D1.19, D1.20 | pass vc 3 (D1.17) and vc 4 (D1.18-D1.20, was BUG-05) |
| P7 SC-3 Long-lead and gate | D1.16 | pass vc 3 (the long-lead half is descoped) |
| P7 SC-4 3DS | D1.21, D2.11 | not run: no challenging card (P0.9 plan, user 2026-11-02); no live 3DS challenge was passed, accepted gap |
| P7 SC-5 Errors and money | D1.23, FS.B10, D1.18 | partial: rotation with the sheet open pass vc 4 (was BUG-06), Payment Methods at 200% pass vc 5, release-build booking with Stripe pass vc 4; the JPY and 3DS-challenge parts not run |
| P8 SC-1 My Bookings | D1.03, D1.27 | pass vc 3 |
| P8 SC-2 Upcoming Booking Details | D1.03, D1.06, D1.13 | pass vc 3 |
| P8 SC-3 Cancel | D1.19, D1.20 | pass vc 4 (held paid booking cancelled, Stripe canceled) |
| P8 SC-4 Reschedule response | D1.24 part 1, D1.RS5, BUG-25 | partial: accept and decline with and without a reason passed per the user (vc 3-4); PAYMENT_METHOD_REQUIRED recovery not run (unreachable live, BUG-32 and BUG-27 backend); Book Again not recorded; Android client answering an expert reschedule user-reported working vc 5 |
| P8 visual and system | T2.M1, T2.M4, T2.M5 | not run, post-release |
| P9 SC-1 Join | D1.06, D1.07, D1.13 | pass vc 3 (native-app open for Skype or Zoom not separately recorded; the unopenable-link case WR-07 passed) |
| P9 SC-2 Background 5+ min | D1.09, D1.11, D2.06 | pass vc 3 and vc 4 |
| P9 SC-3 Pill and guard | D1.08 | pass vc 3 |
| P9 SC-4 Hang-up and rejoin | D1.10, D2.07 | pass vc 4 (was BUG-09) |
| P9 SC-5 Paid session end to end | D1.25, D2.12 | pass vc 4 (D1.25; Railway confirm-connection count not checked) and vc 4 (D2.12; confirm-connection sent once per app, two Railway lines); captures refunded in Section 7 |
| P9 Android 17 audio hardening | EM.1 | not run: skipped by user decision 2026-11-05, post-release |
| P9 device and OEM sweep | FS.C3, TB.C1, TB.C2, PL.C9, IM.C5, D1.12, P0.7, PD.C8 | partial: FS.C3 fail BUG-34 (vc 5, same cause as BUG-22); TalkBack order, notification-permission-denied, pill over gesture and 3-button navigation not run; Bluetooth and GSM not run (D1.12: no headset); Samsung battery state P0.7 pass vc 3; cold-start notification tap PD.C8 pass vc 5 |
| P10 (a) Topics | OT.P10-a | not run, post-release |
| P10 (b) Availability | OT.P10-b | not run, post-release |
| P10 (c) Timezone | OT.P10-c | not run, post-release |
| P10 (d) cross-client | D2.01, D1.29, OT.P10-a | partial: slots match in the expert's timezone (D2.01) and Unblock restores the composer (D1.29) pass vc 4; the new-topic listing part not run (OT.P10-a) |
| P10 (e) French locale | OT.P10-e | not run, post-release |
| P10 (f) 200% and TalkBack | T2.X1, T2.X2, T2.X3 | not run, post-release |
| P10 (g) Gap closure | OT.P10-g | not run, post-release |
| P11 (a) Dashboard | D2.03, SW.3, D2.14 | partial: pending count, Confirmed and the expert Dashboard pass vc 4; the payout card and Custom Tab return not run (D2.14: expert account already onboarded) |
| P11 (b) Requests | D2.02, D2.03, D2.04 | pass vc 4 (expired-request part not run: none exists) |
| P11 (c) Join | D2.05 | pass vc 4 |
| P11 (d) Reschedule and cancel | D2.13, D2.10 | pass vc 4 (SLOT_UNAVAILABLE seen; held paid booking cancelled, Stripe canceled) |
| P11 (e) 200% and TalkBack | FS.CP1, T2.X4, T2.X5, T2.X6 | partial: Client Profile and Booking Summary at 200% pass vc 5 (FS.CP1); T2.X4-T2.X6 not run, post-release |
| G-11-1 11-UAT test 1 | D2.03, D2.05 | pass vc 4 (accept, both sides in the same call) |
| G-11-1 09-HUMAN-UAT item 1 | D1.06, D1.07, D1.13 | pass vc 3 |
| G-11-1 09-HUMAN-UAT item 2 | D1.09, D2.06 | pass vc 3 and vc 4 |
| G-11-1 09-HUMAN-UAT item 5 | D1.10, D2.07 | pass vc 4 |
| G-11-5 (1) fresh build | SW.4, D2.20, H2 | pass: H2 each session proved the Play build (vc 3 to vc 5), installer com.android.vending |
| G-11-5 (2) same booking | D2.05 | pass vc 4 (backend row has both joined_at values) |
| G-11-5 (3) engine profile | D1.07, D2.05 | pass vc 3 and vc 4 (channelProfile:0 in agoraapi.log) |
| G-11-5 (4) remote peer | D1.07, D2.05 | pass vc 3 and vc 4 (remote joined, remote audio state=2) |
| G-11-5 (5) the four checks | see G-11-1 rows | pass (the four G-11-1 boxes above) |
| G-11-6 (5) | D1.06, D1.09, D1.10, D2.06, D2.07 | pass vc 3 and vc 4 |
| P12 (a) Message entry points | D2.08, D1.05 | partial: expert-side entries pass vc 4 (D2.08, user report only); D1.05 fail BUG-13 (backend, Read label not live) |
| P12 (b) cross-client chat | D1.05, D2.08 | D1.05 fail BUG-13 (backend: the Read label does not update live; the server saved it); D2.08 pass vc 4 |
| P12 (c) Report and block | D1.29 | pass vc 4 (the 10/hour report limit message not run) |
| P12 (d) Blocked states | D1.29 | pass vc 4 (the RECIPIENT_UNAVAILABLE suspended-account case not run) |
| P12 (e) 200%, TalkBack, IME | FS.K6, PD.K8, PD.K9, TB.K1-TB.K11, IM.K7-IM.K9, PL.K12 | partial: FS.K6, PD.K8, PD.K9 pass vc 5; the TalkBack rows, IM.K7-IM.K9 and PL.K12 not run (skipped by user decision 2026-11-05) |
| P13 (a) Permission and token | P0.6, R1.3, R1.4, SW.2, SW.1 | Android half pass vc 3-4 (prompt once, android token row present after start, refreshed on return); SW.1 fail BUG-24 (ios: the iPhone keeps its push token row after logout) |
| P13 (b) Push routing | P0.5, D1.01, D1.02, D1.04, D2.02, batch-B smoke H6 | pass vc 3, vc 4 and vc 5 (push from a killed app opens the right screen) |
| P13 (c) Channels | R1.5, D1.04 | pass vc 3 (BUG-04 post-release: the channel list sits behind a category entry on One UI) |
| P13 (d) Notifications list | D1.28 | pass vc 3 |
| P13 (e) Write Review | D1.26 | pass vc 3 |
| P13 (f) Internal-track install | D-10 | installed from Play on the Samsung (vc 1 to vc 5); the wider tester group is an accepted gap (D-10, single tester) |
| P13 (g) 200% and TalkBack | T2.N1, T2.N2 | not run, post-release |
| P13 release checklist (10) | R1.3, R1.4, R1.5, D1.02, D1.04, D2.02 | pass vc 3 and vc 4 (iOS logout leftover is BUG-24, reported to ios) |
| P14 (a) Payouts | D2.14, D2.15, D2.16, D1.30 | D2.14 and D2.15 not run (expert account already onboarded; a new Connect account is forbidden, D-05); D2.16 forged intent pass vc 4 (weak proof); D1.30 iOS half pass vc 3 |
| P14 (b) Activity | OT.P14-b | not run, post-release |
| P14 (c) Settings | OT.P14-c | not run, post-release |
| P14 (d) Delete account | D2.17, D2.18, D2.19 | pass vc 4 (409 shown, then deleted; the iPhone and a fresh Android login both reject it); Help FAQ and Email Us not run |
| P14 (e) Tax | OT.P14-e | not run, post-release |
| P14 (f) 200% and TalkBack | T2.T1, T2.T2 | not run, post-release |
| P15 checklist (15-DEVICE-CHECKLIST.md) | all 101 rows | 17 pass, 5 partial, 1 fail (C3, BUG-34), 78 not run; results in the Result cells |
| 09-HUMAN-UAT 1 (SC-1 join) | D1.06, D1.07, D1.13 | pass vc 3 |
| 09-HUMAN-UAT 2 (SC-2 background) | D1.09, D1.11, D2.06 | pass vc 3 and vc 4 |
| 09-HUMAN-UAT 3 (Android 17 audio hardening) | EM.1 | not run: skipped by user decision 2026-11-05, post-release |
| 09-HUMAN-UAT 4 (SC-3 pill and guard) | D1.08 | pass vc 3 |
| 09-HUMAN-UAT 5 (SC-4 hang-up and rejoin) | D1.10, D2.07 | pass vc 4 (was BUG-09) |
| 09-HUMAN-UAT 6 (SC-5 paid session) | D1.25, D2.12 | pass vc 4; captures refunded in Section 7 |
| 09-HUMAN-UAT 7 (device and OEM sweep) | FS.C3, TB.C1, TB.C2, PL.C9, IM.C5, D1.12, P0.7 | partial: battery state P0.7 pass vc 3; FS.C3 fail BUG-34; Bluetooth and GSM not run (no headset); TalkBack, notification-denied and navigation-mode rows not run |
| 09-HUMAN-UAT 8 (CallStyle decision) | D1.14 | answered by the user 2026-11-02 (recorded in the results file) |
| 09-HUMAN-UAT 9 (review-fix device checks) | D1.11, D1.13 | WR-06, WR-07 and WR-09 pass vc 3; WR-05 is recorded only if it happens, none recorded |
| 15-UAT test 1 (SC-4 TalkBack runs) | TB.L1, TB.B1, TB.C1, TB.K1 | not run: skipped by user decision 2026-11-05; POL-04 and Phase 15 SC-4 not confirmed on a device |
| 15-UAT test 2 (recreation and process death) | PD.Y1, PD.Y3-PD.Y7, PD.Y11 | 6 of 7 pass vc 5; PD.Y3 not run (skipped by user decision) |
| 15-UAT test 3 (large window, cutout, navigation) | LW.Y2, LW.C4, IM.Y8, IM.Y9, IM.Y16, IM.C5 | not run (large window post-release; IM rows skipped by user decision) |
| 15-UAT test 4 (Accessibility Scanner) | TB.Y12, FS.Y12 | not run: skipped by user decision 2026-11-05 |
| 15-UAT test 5 (Week 7 loading, empty and error states) | T2.E3, T2.M2, T2.M3, T2.N3, T2.T5, T2.T7, T2.Y13 | not run, post-release (D-06) |
| 15-UAT test 6 (M8 focus after Retry) | T2.M8 | not run, post-release (D-06) |
| 15-UAT test 7 (B4 day-cell taps) | TB.B4 | not run: skipped by user decision 2026-11-05 |
| 15 gap list: androidTest accessibility suite | none | not run, post-release (JVM audit stays the substitute) |
| 15 gap list: Stripe-card rows | D1.15-D1.23, D1.25, D2.09-D2.12 | pass except D1.21, D1.22 and D2.11 (not run, no challenging or failing card) |
| 15 gap list: live-call rows with the iOS peer | D1.06-D1.13, D2.05-D2.07 | pass except D1.12 (no headset) |
| 15 gap list: cross-device chat, booking and request rows | D1.01-D1.05, D1.29, D2.01-D2.04, D2.08, D2.13 | pass except D1.05 (BUG-13, backend) |
| 15 gap list: photo picker on API 26-32 | OT.PP | not run: no API 26-32 AVD |
| 15 gap list: 45.7 dp day cells | TB.B4 | not run: skipped by user decision 2026-11-05 |
| 15 gap list: focus after Retry on detail screens | T2.M8 | not run, post-release |
| 15 gap list: call pill first (conditional fix) | TB.C2 | not run: skipped by user decision 2026-11-05 |
| 15 gap list: filled field speech (conditional fix) | TB.L3 | not run: skipped by user decision 2026-11-05 |
| 15 gap list: bubble speech (conditional fix) | TB.K3 | not run: skipped by user decision 2026-11-05 |
| 15 gap list: Book Now scroll footer under 480 dp | none | not applicable: phones are locked to portrait (PD.Y1 pass vc 5) |
| 15 gap list: cap the Farewell button | T2.T8, T2.T10 | not run, post-release |
| 15 gap list: wider tester group | none | accepted gap (D-10, single tester) |
| 15 gap list: portrait lock review | PD.Y1 | lock confirmed on the Samsung vc 5; the Play pre-launch review needs Play data |
| FND-14 (Phase 9 background audio) | D1.09, D2.06 | replaced and pass vc 3 and vc 4 |

Confirmed by the user on 2026-11-05: option-confirm ("confirm")
