# Phase 17 Release Checklist (human-only steps)

Every step below needs your accounts, your Mac or your phones, so none of it is done by code plans (manual steps never block code). Tick a box only after its "Check:" result is what you see. Each section is its own item, so an interrupted run resumes at the first unchecked box. Passwords and tokens stay in your password manager and in the backend reviewer document (`APP-REVIEW-ACCOUNT.md` in the backend repo's `docs/` folder); nothing secret is ever pasted into this repo. Accounts and phones are named by role: the "Android client on the Samsung" and the "iOS expert on the iPhone".

Short names used below: `APP` is `/Users/LeonChen/Documents/ClickMe_Android`; `BACKEND` is `/Users/LeonChen/Documents/ClickMe2026 Backend`; `RELEASE` is `/Users/LeonChen/ClickMe-release`; `ASSETS` is `/Users/LeonChen/ClickMe-release/store-assets`; `ADB` is `/Users/LeonChen/Library/Android/sdk/platform-tools/adb`. The contact email for users, the deletion page and Play is `clickmeapp@uptrendinvestments.net`.

## Timeline

Closed test day 0 to day 14, then Google's production-access review (usually seven days or less, flagged assumption A10), then the production review: roughly three to four weeks from day 0. Every day the closed test starts later moves the public release by one day (D-01). Sections 1 to 5 can start today and need no code, so the 14-day clock is never held up by the code plans.

## Branch

Section 1 decides it. Gate shown: run sections 10 to 13 (closed track, 14 days, apply). Gate not shown: skip sections 11 and 13, release vc 6 on Internal testing, run smoke run 1 on that Play install (the same AAB), then go to section 14.

Sections and the plans that run them: 1 to 5 start today; 6 is the 17-08 checkpoint; 7 to 10 are 17-08 and 17-09 work; 11 to 13 are 17-09; 14 to 16 are 17-10.

Readiness (17-08 preflight, 2026-11-06). Checked read-only before the Console session; the AAB check passed, so the session may proceed.
- READY: `/Users/LeonChen/ClickMe-release/clickme-1.0.0-6.aab` sha256 starts `9ec7495080f93805` and equals the 16-BUILD-LEDGER row 6 sum (full sum 9ec7495080f93805bfa9412d075bf6c252bd1bcd048b11f11325e6ddd651e81e); the APK and the mapping file are archived next to it.
- READY: `17-CONSOLE-PASTE.md` has its 11 numbered sections (1 privacy policy to 11 release) and no line pairing the word password with a value (two matches: the "no password" statement and the in-app delete path).
- READY: `17-DELETE-ACCOUNT-PAGE.html` parses (`xmllint --noout`).
- READY: `17-PAYMENTS-DECISION.md` has 13 numbered table rows.
- READY: five phone screenshots `01-explore.jpg` to `05-chat.jpg` pass `scripts/play-store-screenshot.sh check` (1080 x 1920, no alpha), more than the four required. Approval is still open: every "Approved at 17-08" cell is empty, and rows 1, 2, 4 and 5 carry third-party content (see 17-STORE-ASSETS.md).
- READY: `play-icon-512.png` is 512 x 512 with alpha.
- OPEN (user): feature graphic `ASSETS/feature-graphic.jpg` (or `.png`) does not exist yet (checklist section 5).
- OPEN (user): foreground-service video link is not recorded in 17-STORE-ASSETS.md row 9 (checklist section 4).
- OPEN (Task 2): 17-PAYMENTS-DECISION.md rows 1 to 13 still say "(pending 17-08)", and 17-CONSOLE-PASTE.md still carries five "(confirm at 17-08 #n)" markers plus the explanatory sentence in its intro; both clear when the answers are recorded.
- PENDING Task 3 (not a preflight failure): `https://www.uptrendinvestments.net/clickme/delete-account.html` returns 404 and the published `privacy.html` has no "Firebase Cloud Messaging" yet, because neither page is published until section 7.
- NOT RUN (device absent): the Samsung is not on ADB, so the installed versionCode check (H1 and H2) is deferred to Task 3. Ledger row 6 still reads "not uploaded".

## 1. Production-access gate (D-01)

Needs: Play Console, organization account. This is a 15-minute look, and it decides the branch above.

1. Open the Dashboard and look for "Apply for production" or "Get access to production" (also Test and release, Production: greyed out means the gate applies).
2. Settings, Developer account: note the account type.
3. Save a screenshot as `RELEASE/logs/phase17/gate-<date>.png` (`mkdir -p /Users/LeonChen/ClickMe-release/logs/phase17` first).

Note (flagged assumption A4): vendor pages claim organization accounts are exempt. Google's own page never says so and the Console showed the gate on 2026-10-30, so this plan does not rely on the claim. The Console decides.

Check: one line "gate shown: yes or no (date)". It becomes answer #12 at 17-08.

- [ ] (1) Gate answer recorded

## 2. Play support question (D-01)

Needs: Play Console (Help, Contact support). Send it and do not wait for the reply; the closed test does not depend on it.

1. Send this message:

```
Our developer account (Uptrend Investments Inc., a verified organization account) shows the closed-testing requirement (12 testers for 14 days) for our new app, package com.uptrendinvestments.clickmeapp. The published rule mentions personal accounts created after 13 November 2023. Does this requirement apply to our organization account? If it does not, please remove the production-access requirement for this app. Thank you.
```

Check: the support case number is recorded here: `case ______`.

- [ ] (2) Support question sent

## 3. Tester service (D-02, D-03)

Needs: a payment card. The choice and the payment are yours. Compare the services' current pages first, because prices and guarantees change; the table is vendor-stated and was fetched on 2026-11-05 (flagged assumption A3: vendor claims are unverified).

| Service | Price | Testers | 14-day terms | Questionnaire help | Guarantee |
|---|---|---|---|---|---|
| Testers Community Pro | CA$33 per app | 25 | "Full 14 days", delivery within 6 hours | Pre-filled production-access answers | "Approved, or your money back" |
| TestFi | $39.99 (20 testers for $49.99) | 16 | 14 days, "twelve survive" | Compliance report you can show Google | Twelve opted in on day 14, or a free re-run |
| PrimeTestLab | Starter $19.99 / Professional $59.99 | 12 / 20 | 14 days | Not stated | One free retest or refund if access is not granted |
| onTest | $19.99 | 12 | 14 days plus 1 day | Not stated | 3-day money-back only |
| 12RealTesters | $49, charged once progress shows | 14 | 14 days, daily screenshot per tester | Not stated | No guarantee of production access |

Recommendation: Testers Community Pro (25 testers, CA$33). Runner-up: TestFi (16 testers, $39.99). Avoid 12-tester packages: they have no buffer, which contradicts D-02. Buy at least 20 testers so that drop-outs do not break the 12 needed. Optionally add 2 or 3 testers of your own.

1. Buy the service. In writing, ask the vendor for: the countries their testers are in (needed for the closed-track country list, flagged assumption A2); a daily-use or feedback report per tester (it feeds Part 1 of the production-access application in section 13); and the email list or Google Group address they need from you.
2. Give the vendor the closed-track tester instructions exactly as they are in `17-CONSOLE-PASTE.md` section 11 (free topics only, never a real card). There is no guided task list (D-03). Check that the pasted text carries the contact email `clickmeapp@uptrendinvestments.net`.
3. Risk note (RESEARCH Pitfall 3): Google judges tester engagement, not only the count, and can answer "more testing required". Choose a vendor whose testers use the app daily. Flagged assumption A14: the vendor's testers follow the free-topics rule; section 11 watches for the opposite.

Check: the vendor confirms the order, the tester count (20 or more) and the delivery date.

- [ ] (3) Tester service chosen and paid

## 4. Foreground-service video (D-16)

Needs: the Samsung with the current Play install. Record now: the call flow is unchanged by the D-09 fixes.

1. Follow the five steps in `17-CONSOLE-PASTE.md` section 8, 30 to 60 seconds, screen recording on the Samsung.
2. Use your own client and expert accounts so that no third person is shown.
3. Upload to YouTube as Unlisted.

Check: the link opens and plays in a private browser window.

- [ ] (4) Foreground-service video recorded and linked

## 5. Feature graphic (D-16)

Needs: an image editor. Save as `ASSETS/feature-graphic.jpg` (or `.png`): 1024 x 500, JPEG or 24-bit PNG, no alpha, no fine detail near the edges.

Check: `sips -g pixelWidth -g pixelHeight -g hasAlpha /Users/LeonChen/ClickMe-release/store-assets/feature-graphic.jpg` prints 1024, 500 and `hasAlpha: no`.

- [ ] (5) Feature graphic saved

## 6. Decisions at the 17-08 checkpoint (D-05)

Needs: `17-PAYMENTS-DECISION.md`. Answer the 13 rows of its section 2 (payments path, card data, NFC, push content, privacy names, financial features, developer name, retention wording, email response time, the remember-login-email branch, call notification style, the gate answer from section 1, the tester service from section 3). "All recommended" is a valid answer. Also decide at this checkpoint whether to carry the in-app delete-account wording fix (the in-app copy promises more than the backend deletes) as a closed-track fix build under section 11.

Check: every row's Answer cell is filled.

- [ ] (6) Decisions answered

## 7. Publish the deletion page and privacy edits (D-06)

Needs: access to the uptrendinvestments.net site.

1. Upload `17-DELETE-ACCOUNT-PAGE.html` to the site as `/clickme/delete-account.html`, next to `privacy.html`.
2. Apply edits E1 to E5 from `17-PRIVACY-POLICY-EDITS.md` to `privacy.html`.

Check:
- `curl -s -o /dev/null -w '%{http_code}' https://www.uptrendinvestments.net/clickme/delete-account.html` prints 200.
- `curl -s https://www.uptrendinvestments.net/clickme/delete-account.html | grep -c clickmeapp@uptrendinvestments.net` prints at least 1.
- `curl -s https://www.uptrendinvestments.net/clickme/privacy.html | grep -c "Firebase Cloud Messaging"` prints at least 1.

- [ ] (7) Deletion page and privacy edits live

## 8. Console App content (REL-06, D-07, D-08)

Needs: Play Console, `17-CONSOLE-PASTE.md`, the backend reviewer document.

1. Re-seed the reviewer account before App access, so the account is fresh (RESEARCH Pitfall 7): `cd "/Users/LeonChen/Documents/ClickMe2026 Backend" && npm run seed:app-review`, against production as the backend reviewer document describes (it needs the demo cast to exist; never run the demo reset script). If it prints `INVARIANT VIOLATED`, stop and read that document.
2. Paste `17-CONSOLE-PASTE.md` sections 1 to 9 in order: privacy policy, App access, Ads, Content rating, Target audience, Data safety, Financial features, Foreground service permissions (with the video link from section 4), Other declarations.
3. Type the reviewer account credential straight into Console's own field, copying it from the backend reviewer document. It is never written here.

Check: Policy and programs, App content shows no item under "Needs attention". Save a screenshot to `RELEASE/logs/phase17/`. Flagged assumption A1: if "Review release" in section 10 lists more unfinished items, finish them before rollout.

- [ ] (8) App content complete

## 9. Store listing and graphics (REL-04, D-16)

Needs: the store assets (`17-STORE-ASSETS.md` from plan 17-05) and the feature graphic.

1. Paste `17-CONSOLE-PASTE.md` section 10 (name, short and full description, category, contact email, privacy policy URL).
2. Upload `ASSETS/play-icon-512.png`, the feature graphic, at least four phone screenshots whose row in `17-STORE-ASSETS.md` is approved, and the video link.
3. The in-call screenshot is captured last, during smoke run 1 (section 12, row S1.08), and added after.

Check: the main store listing saves with no field error.

- [ ] (9) Store listing saved

## 10. Upload vc 6 and open the closed track (D-10)

Needs: the AAB archived by plan 17-07 (`16-BUILD-LEDGER.md` B1 to B6).

1. Test and release, Testing, Closed testing: create the track.
2. Countries/regions: the countries the vendor stated, or all (production stays Canada and the US; flagged assumption A2). Testers: the vendor list or Google Group, plus the Samsung's Google account.
3. Create new release, upload `/Users/LeonChen/ClickMe-release/clickme-1.0.0-6.aab`. Release name `1.0.0 (6)`; release notes from `17-CONSOLE-PASTE.md` section 11.
4. Save, Review release (fix anything it lists), Start rollout. Optionally release the same AAB on Internal testing for an immediate proof on the Samsung.
5. Send the opt-in link to the vendor.
6. Ledger B8 on the Samsung: H1 and H2 of `16-MATRIX-RUNBOOK.md`. versionCode must be 6 and the installer `com.android.vending`.

Check: the closed release reads Available, and the Play status of ledger row 6 in `16-BUILD-LEDGER.md` is updated.

- [ ] (10) vc 6 on the closed track

## 11. Closed test, 14 days (D-01, D-04)

Needs: at least 12 opted-in testers every day (the extra testers are the buffer). Day 0 is the first day the Testers tab shows at least 12 opted-in testers. Fill one row per day.

| Day | Date | Opted-in testers | Notes |
|---|---|---|---|
| 0 | | | |
| 1 | | | |
| 2 | | | |
| 3 | | | |
| 4 | | | |
| 5 | | | |
| 6 | | | |
| 7 | | | |
| 8 | | | |
| 9 | | | |
| 10 | | | |
| 11 | | | |
| 12 | | | |
| 13 | | | |
| 14 | | | |

Rules:
1. A tester who opts out loses their streak and does not count; opting back in restarts their 14 days.
2. Uploading a fix build to the closed track does not reset the clock (RESEARCH, confirmed against Google's FAQ, D-04): only opt-outs matter. Fix builds follow the `16-BUILD-LEDGER.md` F1 to F8 procedure (vc 7, 8, and so on). The final binary gets a short smoke repeat (S1.01 to S1.06) before it is promoted.
3. Watch the Stripe dashboard (live mode) during the test. A payment by a tester is voided or refunded at once, and the vendor is reminded of the free-topics rule (flagged assumption A14). Testers are never to pay for sessions or enter real cards.
4. Collect feedback from Monitor and improve, Ratings and reviews, Testing feedback, and ask the vendor for the usage and feedback report.
5. Run section 12 (smoke run 1) during these days.

Check: the Testers tab shows at least 12 opted in on day 14, and every log row is filled.

- [ ] (11) 14 days completed

## 12. Smoke run 1 (D-13, D-14, D-15)

Needs: the Samsung and the iPhone. Who: a brand-new Android client on the Samsung (a new email you control) and your existing iOS expert on the iPhone, with a free topic and availability in the next few hours. The booking is free: no money moves (D-14). Run it on the closed-track Play install, the same binary that goes to production (D-15, part 1). On the gate-not-shown branch, run it on the Internal testing Play install of the same AAB.

Result notation (from `16-MATRIX-RUNBOOK.md`): `[ ]` not yet run; `[x] pass (vc N, YYYY-MM-DD)`; `[!] fail (BUG-NN, vc N)`; `[-] not run: reason`. A row passes only on the build that H2 proved for that run. Never put a password in this file.

| Row | Who and where | Steps | Expected | Result |
|---|---|---|---|---|
| S1.00 | iOS expert on the iPhone | Sign in as your iOS expert on the iPhone. Make sure it has one active free topic and a slot in the next few hours. Check the backend: `curl -s https://clickme-backend-production.up.railway.app/health`. | The iPhone shows expert mode with a bookable free topic; `/health` shows status success. | [ ] |
| S1.01 | Samsung | The Samsung's Google account is opted in to the closed track. Uninstall ClickMe, install it from Google Play, then run H1 and H2 (`16-MATRIX-RUNBOOK.md`). | versionCode equals the closed release (6 or the newest fix build), installer is `com.android.vending`. | [ ] |
| S1.02 | Android client on the Samsung | Open ClickMe and sign up as a new client with a new email you control. Finish the required steps. | Signup completes and lands on the client home. | [ ] |
| S1.03 | Android client on the Samsung | Book the iOS expert's free topic for a slot in the next few hours. | Request Sent shows Awaiting Approval; the iPhone receives the request push. | [ ] |
| S1.04 | iOS expert on the iPhone | Accept the request. | The Samsung gets the booking push; Upcoming Details shows Confirmed and a Join button when the window opens. | [ ] |
| S1.05 | Android client on the Samsung | BUG-34 re-test, before the iOS expert joins: set font size to maximum and screen zoom to largest, tap Join. On the Waiting screen swipe up. Then restore your usual font size and zoom. | Minimize, Chat, Mute and Speaker are all reachable above End Call. | [ ] |
| S1.06 | Both phones | Both join inside the join window. Test two-way audio, mute and speaker. Press Home on the Samsung. | Two-way audio works, mute and speaker work, Home shows the ongoing notification, tapping it returns to the call. | [ ] |
| S1.07 | Android client on the Samsung | BUG-22 re-test: in the last 5 minutes of the slot (stay in the call, or leave and rejoin within them), with the banner and the time-left chip showing at font scale 1.0. | The four controls (Minimize, Chat, Mute, Speaker) are fully visible above End Call without scrolling. | [ ] |
| S1.08 | Samsung, with Claude over ADB | While Connected, Claude runs `bash scripts/play-store-screenshot.sh prep`, then `capture 06-in-call`, then `restore`, while you hold the call. Approve the frame in `17-STORE-ASSETS.md`. | One in-call screenshot saved, with no real person's face; the Samsung display restored. | [ ] |
| S1.09 | Both phones | End the call on both phones. Claude reads the H4 crash count: `ADB -t T logcat -d -b crash` filtered for com.uptrendinvestments.clickmeapp. | The call ends on both sides; the H4 crash count is 0. | [ ] |
| S1.10 | Android client on the Samsung (optional) | Delete the smoke client in the app: Profile, Security and Password, Delete Account. This also checks the flow the deletion page describes. Then sign the Samsung back in to your expert account. | The farewell screen confirms deletion; signing in with the old email fails. | [ ] |

A failed row gets a row in `16-BUG-LOG.md` and a fix build (section 11 rule). If a later fix build replaces the smoke-tested binary, re-run S1.01 to S1.06 on it before section 14.

Check: every row S1.00 to S1.09 is `[x]`, S1.10 is `[x]` or `[-]`, and the in-call screenshot is approved.

- [ ] (12) Smoke run 1 passed

## 13. Apply for production (D-01)

Needs: day 14 reached with at least 12 opted in, and this section applies on the gate-shown branch only. Edit the drafts below to match what really happened before you submit; they must stay truthful.

1. Dashboard, Apply for production. Answer the three parts, then submit.
2. The result comes by email to the account owner, usually within seven days but it can take longer (flagged assumption A10).
3. If the answer is "more testing required", continue section 11 with better tester engagement and re-apply.

### Part 1

About the closed test: how easy was it to recruit, tester engagement, feedback summary.

```
We recruited our testers through <vendor name>, a paid testing service, plus <number> testers of our own. Recruiting 12 or more testers was easy through the service. <Number> testers stayed opted in for the full 14 days. Engagement: <summarize the vendor's usage report and what the Console shows; state numbers>.
Not every feature was used by testers. Testers were told to book free topics only, because the app takes live payments, so paid bookings and live paid voice calls were not exercised by testers. Those flows were covered by our own two-device QA with the iOS app and by our smoke test on the closed-track build.
Feedback: <summarize the Testing feedback entries and the vendor report; if there was little or none, say so>. We collected it through the Play Console testing feedback and the vendor's per-tester report.
```

### Part 2

About the app: audience, value, first-year installs.

```
Audience: adults aged 18 and over in Canada and the United States who want one-on-one advice from experts.
Value: users find an expert, book a live voice session, chat, pay securely and leave a review.
First-year installs: <choose the range you expect>.
```

### Part 3

Production readiness: changes made from testing and how readiness was determined.

```
Changes since testing: <list only what testers actually reported and what we changed; if testers reported nothing, say so>.
Separately, three fixes came from our internal QA before the test, not from tester feedback: saved form state after the system closes the app (POL-02), and call-screen controls that were hard to reach in the last five minutes of a session (BUG-22) and at the largest font and display sizes (BUG-34).
Readiness: internal builds 1 to 5, a two-device cross-platform QA matrix against the iOS app on the production backend, and a smoke test on the closed-track build (sign-up, booking, the call) that passed on <date>.
```

Check: the confirmation email arrives, and its date is recorded here: `______`.

- [ ] (13) Production access applied for

## 14. Production release (D-11, D-12)

Needs: production access granted (on the gate-not-shown branch, smoke run 1 on Internal testing passed). Rolling out to everyone is costly to reverse (D-11).

1. Re-run the reviewer seed (section 8, step 1), so the reviewer account is fresh for Google's review.
2. Test and release, Production, Countries/regions: Canada and United States only (D-12).
3. Promote the release from the closed track (the exact release smoke run 1 passed on) to Production. Rollout percentage 100%, no staged rollout (D-11).
4. Publishing overview: managed publishing off. Release notes from `17-CONSOLE-PASTE.md` section 11.
5. Save, Review release, Start rollout to Production, then wait for Google's review.

Rollback: "Halt rollout" in Console stops new installs. Installs already made are fixed only by a higher versionCode through the ledger procedure.

Check: both commands print 200:
- `curl -s -o /dev/null -w '%{http_code}' 'https://play.google.com/store/apps/details?id=com.uptrendinvestments.clickmeapp&hl=en&gl=CA'`
- `curl -s -o /dev/null -w '%{http_code}' 'https://play.google.com/store/apps/details?id=com.uptrendinvestments.clickmeapp&hl=en&gl=US'`

- [ ] (14) Production release live

## 15. Smoke run 2 (D-15)

Needs: the Samsung and the iPhone, same people as smoke run 1 (an Android client on the Samsung, the iOS expert on the iPhone), a free topic again. This is the short repeat from the public listing (D-15, part 2). Same result notation as section 12.

| Row | Who and where | Steps | Expected | Result |
|---|---|---|---|---|
| S2.00 | Mac | Run the two listing curl commands from section 14. | Both print 200. | [ ] |
| S2.01 | Samsung | Leave the closed and internal testing programs (the opt-out links on the Play opt-in pages), or use a Google account that is not a tester. Uninstall ClickMe, install from the public listing, run H1 and H2. | versionCode equals the production release, installer is `com.android.vending`. | [ ] |
| S2.02 | Android client on the Samsung | Sign up as a new client with a new email you control. | Signup completes and lands on the client home. | [ ] |
| S2.03 | Both phones | Book the iOS expert's free topic; the iOS expert accepts on the iPhone. | Request Sent, then Confirmed on the Samsung; pushes arrive. | [ ] |
| S2.04 | Both phones | Both join inside the window and talk. | Two-way audio works. | [ ] |
| S2.05 | Both phones | End the call; Claude reads the H4 crash count. | The call ends on both sides; the crash count is 0. | [ ] |
| S2.06 | Samsung (optional) | Rejoin the internal testing program for future builds, and delete the smoke client (Profile, Security and Password, Delete Account). | Opt-in restored; the smoke client is deleted. | [ ] |

Check: rows S2.00 to S2.05 are `[x]`.

- [ ] (15) Smoke run 2 passed

## 16. Close-out

Needs: the repo checkout.

1. Tick REL-04, REL-06 and REL-07 in `.planning/REQUIREMENTS.md` (POL-02 too, once 17-01 is verified).
2. Tick steps 7 and 9 in `13-RELEASE-CHECKLIST.md`, which sections 8, 9 and 6 of this checklist carry.
3. Record the dates (gate answer, day 0, application, production live) in STATE.
4. Optional cleanup: remove vendor-tester accounts through the admin portal Terminate action, only for accounts confirmed as vendor testers, never a real user.

Known open after release:
- The forced-logout push-token DELETE (milestone-audit warning 2) needs a backend change, so it is left for after release.
- The in-app `delete_account_body` wording promises more than the backend deletes ("all your data permanently removed"). If decided at section 6, it ships as a closed-track fix build; otherwise it stays open.
- Deferred deviations from CONTEXT: FND-13, PAY-05/CR-01, PAYOUT-02, POL-04, REL-05, BUG-04, BUG-18 to BUG-21, BUG-23, BUG-28.
- The iOS sync todo (Android bug fixes to iOS, including the overtime cutoff) stays a separate track.

Check: the requirement boxes are ticked in REQUIREMENTS.md.

- [ ] (16) Release closed out
