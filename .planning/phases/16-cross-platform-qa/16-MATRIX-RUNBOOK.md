# Phase 16 Matrix Runbook (blocking, D-09)

One ordered runbook for the QA-02 matrix (Direction 1: Android client, iOS expert), the QA-03 matrix (Direction 2: iOS client, Android expert) and the tier-1 rows of `15-DEVICE-CHECKLIST.md` (D-06). A person runs every row on the Play internal-track build; Claude captures logs and records results; nothing is ticked from automated test results. Accounts and directions switch once (D-14). Phase 16 cannot close, and Phase 17 cannot start, until the user confirms Section 8 (D-09).

## Header

- **Devices (D-13):** Samsung SM-A075F (Android 16, API 36) and the user's real iPhone with the production iOS app (its `Environments.plist` stays on Production, D-01). No simulators for matrix rows. The `Medium_Phone_API_37.0` emulator is used only for rows marked "emulator" (D-10).
- **Build under test:** the `prodRelease` installed from the Play internal testing track, versionCode as recorded in the Session log. Never `devDebug`, never a sideloaded build (D-08, D-15). Every session starts with Harness H1-H4.
- **Backend:** production, Stripe live mode, real cards, real money (D-01). Amounts follow the money plan the user confirms in 16-03 (recorded in P0.9).
- **Accounts by role only (D-03):** A = the user's real expert account whose live Connect account accepts charges. B = the user's second real account, used as the client. Throwaway = a new account created on Android in Direction 2 for the deletion rows. Never use the store-review account `appreview@clickme.test` for any row. Never paste a password, token, key or card number into this file; card references use the brand and last 4 digits only.
- **Money safety:** every paid booking is written into "Paid bookings created" when it is made. Stripe test cards are not used (D-02). Never run `scripts/live-cutover-cleanup.js` or any bulk script against production.
- **Columns:** `Row ID | Source row ID | Direction | Steps | Expected | Evidence to capture | Result`. Direction values: `D1` (Samsung = B client, iPhone = A expert), `D2` (Samsung = A expert, iPhone = B client), `Samsung` (single device), `emulator`.
- **Result notation:** `[ ]` not yet run; `[x] pass (vc N, YYYY-MM-DD)`; `[!] fail (BUG-NN, vc N)`; `[-] not run: reason`. A row passes only on the build proven by H2 for that session. A failed row's Result also gets a row in `16-BUG-LOG.md`.
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

Later plans add the sessions `preflight`, `session-A`, `batch-A`, `session-B`, `batch-B` and `close-out`.

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
| P0.9 | D-01, D-02, D-04 | D1 | Claude fills this cell in 16-03 Task 2: the money option the user chose, the live floor, the planned number of captures and the 3DS route. | A written money plan the user confirmed before any charge. | The plan text in this cell. | [ ] |
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

Every tier-1 row of `15-DEVICE-CHECKLIST.md`, grouped by the device setting so each setting is switched on and off once. Filled by Task 3.

## 6. Run if time allows (triaged by D-07, not blocking)

Large-window, tier 2-4 and other deferred rows. Failures are triaged by D-07 and do not block automatically. Filled by Task 3.

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

The user fills this at close-out. Filled by Task 3.
