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

Samsung = B (client), iPhone = A (expert). Rows run in order. Filled by Task 2.

## 3. Direction switch

Log out on both phones and log in with the roles flipped, so accounts and directions switch once. Filled by Task 2.

## 4. Direction 2 (QA-03): iOS client B, Android expert A

Samsung = A (expert), iPhone = B (client). Rows run in order. Filled by Task 2.

## 5. Samsung tier-1 setting passes (D-06)

Every tier-1 row of `15-DEVICE-CHECKLIST.md`, grouped by the device setting so each setting is switched on and off once. Filled by Task 3.

## 6. Run if time allows (triaged by D-07, not blocking)

Large-window, tier 2-4 and other deferred rows. Failures are triaged by D-07 and do not block automatically. Filled by Task 3.

## 7. End of run: money sweep and cleanup

Refund and hold checklist, run after the last row. Filled by Task 2.

## Paid bookings created (D-04)

Every paid booking is listed here when it is made. Filled by Task 2.

## 8. Sign-off (D-09)

The user fills this at close-out. Filled by Task 3.
