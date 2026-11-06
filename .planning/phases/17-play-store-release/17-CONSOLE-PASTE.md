# Phase 17 Play Console Paste File

This file supersedes `.planning/phases/13-push-notifications-internal-testing/13-PLAY-CONSOLE-DRAFTS.md` as the single source for every Play Console App content task, the store listing and the release fields (REL-04, REL-06). Paste from the fenced blocks only. The sections follow the order of the Console's App content and release screens; the "Needs attention" tab on App content is the authority for which tasks exist, so use what Console lists.

This file contains no password, keystore value or token. The reviewer password is typed into Console by the account owner.

The answers that depended on the pre-submission checkpoint were confirmed by the user on 2026-11-06 (rows 1-13 of the table in `17-PAYMENTS-DECISION.md` section 2); each is marked "(confirmed 2026-11-06, row n)" where it appears below.

## 1. Privacy policy

Console path: Policy and programs > App content > Privacy policy

```
https://www.uptrendinvestments.net/clickme/privacy.html
```

This is the same string as `AppLinks.PRIVACY_POLICY` in the app, and the app links it from the profile hub. Google requires an active public URL (not a PDF) and a link inside the app. Publish the edits in `17-PRIVACY-POLICY-EDITS.md` (E1-E4) before submitting the Data safety form, so the policy and the form agree.

## 2. App access

Console path: Policy and programs > App content > App access

Choose "All or some functionality is restricted", then add one set of instructions (D-07):

- Instruction name: `Reviewer account (client and expert)`
- Username (email): `appreview@clickme.test`
- Credential field: the account owner types the reviewer-account credential into Console's own field, copying it from the backend's reviewer-account document. It is never stored in this repository or in any planning file.
- Any other information required: paste the block below.

```
Sign-in: email appreview@clickme.test, with the credential entered in the credentials field.
The account holds both roles (client and expert). Open the Bookings tab for three completed sessions, the Messages tab for a chat thread, and Settings for Delete Account.
Voice calls need two people at a scheduled time, so a live call cannot be reached with this account alone. The in-app call and its foreground service are shown in the foreground-service declaration video.
Payouts are not set up on this account, so paid topics cannot be booked from it.
```

Note for the user, not for pasting: re-run `npm run seed:app-review` in the backend repo immediately before every submission to review (the closed-track release and the production release). A reviewer may delete the account or book its free topic, and re-running creates a fresh user (RESEARCH Pitfall 7).

## 3. Ads

Console path: Policy and programs > App content > Ads

- Does your app contain ads? No.
- Advertising ID declaration: the app does not use the advertising ID. The merged manifest has no `AD_ID` permission and no ad SDK is in the app.

## 4. Content rating

Console path: Policy and programs > App content > Content rating

Category: "Social" or the closest communication category Console offers (users chat and talk to each other). Email for the rating certificate: `clickmeapp@uptrendinvestments.net`.

| Question topic | Answer |
|---|---|
| Users can interact or exchange content with each other | Yes (chat and voice between users) |
| Users can block other users or their content | Yes (chat Report & Block: block and unblock a user) |
| Users can report or flag other users or their content | Yes (chat Report & Block: report with a reason, reviewed on the backend) |
| Digital purchases or payments | Yes (paid sessions, charged through Stripe) |
| Shares the user's location with other users | No |
| Violence, blood, fear | No |
| Sexual content or nudity | No |
| Profanity or crude humour | No |
| Controlled substances (drugs, alcohol, tobacco) | No |
| Gambling or simulated gambling | No |

Expect an "Users interact" notice and a teen-or-higher rating. The exact question wording and categories are read in Console, not assumed here (RESEARCH A7); if the resulting rating looks wrong, use "Start new questionnaire" and answer again. This is flagged assumption A7: the 17-08 answer sheet has no row for it, so the check happens here, at the moment you fill in the questionnaire. Keep the "interact" and "digital purchases" answers Yes whatever the wording; never answer as if users could not interact.

## 5. Target audience and content

Console path: Policy and programs > App content > Target audience and content

- Target age group: 18 and over only. Do not select any younger group.
- Does the app appeal to children? No, the app is not designed to appeal to children.
- Families program: not part of it. Committed to Play Families Policy: No (as in section 6).
- Ads shown to children: not applicable, the app has no ads.

## 6. Data safety

Console path: Policy and programs > App content > Data safety

Overview answers:

- Does your app collect or share any of the required user data types? Yes.
- Is all of the user data collected by your app encrypted in transit? Yes (TLS to the API, the realtime socket, Stripe and Agora).
- Do you provide a way for users to request that their data is deleted? Yes.

Data types. Declare exactly these rows and no others. Columns follow the Console questions per data type.

| Data type (Console category) | What it is | Collected | Shared | Ephemeral | Required or optional | Purposes |
|---|---|---|---|---|---|---|
| Personal info: Name | Display name from signup | Yes | No | No | Required | App functionality, Account management |
| Personal info: Email address | Account email, login and recovery codes | Yes | No | No | Required | App functionality, Account management |
| Personal info: User IDs | Account id used on bookings, chats and reviews | Yes | No | No | Required | App functionality, Account management |
| Personal info: Other info | Bio, expertise tags, languages, country and province, time zone, expert availability, topic titles and prices | Yes | No | No | Required for experts; optional for clients | App functionality, Personalization |
| Photos and videos: Photos | Profile photo chosen with the system photo picker and uploaded | Yes | No | No | Required for experts; optional for clients | App functionality |
| Messages: Other in-app messages | Chat messages between clients and experts, report and block reasons. Notifications can carry a message preview of up to 140 characters delivered by Firebase Cloud Messaging (a service provider) (confirmed 2026-11-06, row 4) | Yes | No | No | Optional | App functionality |
| Audio: Voice or sound recordings | Live voice during a session, streamed through Agora (a service provider acting for the app); the app does not record or store audio | Yes | No (Agora is a service provider acting for the app) | Yes (processed in real time, not stored by the app) | Optional (needed to take part in a call) | App functionality |
| Financial info: User payment info | Card details typed into Stripe's PaymentSheet; Stripe's SDK sends them from the device, and the app never reads card numbers | Yes | No, Stripe is a service provider (confirmed 2026-11-06, row 2) | No | Optional (only for paid sessions) | App functionality |
| Financial info: Purchase history | Booking amounts and the Payment History screen | Yes | No | No | Optional | App functionality |
| Financial info: Other financial info | Expert earnings and payout status | Yes | No | No | Optional (experts only) | App functionality |
| App activity: Other user-generated content | Reviews, booking notes, cancellation and decline reasons | Yes | No | No | Optional | App functionality |
| App activity: In-app actions / booking details | Booking requests, times, status | Yes | No | No | Required for bookings | App functionality |
| Device or other IDs | The Firebase Cloud Messaging push token and an app-generated random device id (not the advertising id, not the hardware id) | Yes | No (Google Firebase Cloud Messaging is a service provider that delivers the push) | No | Required for push notifications | App functionality (push notifications) |

Security practices and deletion answers:

- Data is encrypted in transit: Yes.
- Do you provide a way for users to request that their data is deleted? Yes.
- In-app path (Console asks how users can request deletion): Profile > Security & Password > Delete Account (Phase 14).
- Web link (Console asks for a URL that users can use to request deletion without the app): `https://www.uptrendinvestments.net/clickme/delete-account.html` (D-06). Paste the same URL into the separate Console "Delete account URL" field, in Data safety or in App content, wherever Console shows it.

```
https://www.uptrendinvestments.net/clickme/delete-account.html
```

Notes:

- The NFC permission comes from Stripe's SDK (the app's own manifest has none) and needs no Data safety row (confirmed 2026-11-06, row 3).
- No advertising ID is used and no ad, analytics or crash-reporting SDK is in the app.
- Committed to Play Families Policy: No. Independent security review: No.
- The Data safety answers must match the published privacy policy after edits E1-E4 in `17-PRIVACY-POLICY-EDITS.md`. Publish those edits before submitting this form.
- Sign-in tokens are kept in encrypted storage and app data is excluded from cloud backup and device transfer. Console has no question for this; the note is for your own reference.

## 7. Financial features

Console path: Policy and programs > App content > Financial features

Select: "My app doesn't provide any financial features" (confirmed 2026-11-06, row 6).

Reason, for your own reference: Stripe takes payment for an expert's time and pays out experts. The app offers no banking, lending, investing, crypto, money transfer or wallet feature. This is flagged assumption A8: experts may teach finance topics, but the app gives no financial advice itself.

## 8. Foreground service permissions

Console path: Policy and programs > App content > Foreground service permissions

Service: `com.uptrendinvestments.clickmeapp.services.call.CallService`, declared with `android:foregroundServiceType="microphone"`, not exported. Permissions: `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MICROPHONE`, `RECORD_AUDIO`.

**Foreground service type:** Microphone (`FOREGROUND_SERVICE_MICROPHONE`)

**Use case:** Background Audio Access

Do not choose a calling-type use case (Phone call, Telecom, VoIP or Cellular/VoIP APIs). Those options require `MANAGE_OWN_CALLS` or ConnectionService, which the app does not use; ClickMe carries its voice with the Agora SDK.

**Functionality description:**

```
ClickMe is a marketplace for live one-on-one voice sessions between a client and an expert. When a user joins a booked session, CallService starts as a microphone foreground service so the two-way voice call continues while the user leaves the app (for example to check a note, a calendar or a message) or turns the screen off. The service shows an ongoing notification with the expert's name, the elapsed time and an "End call" action, and tapping it returns the user to the call. The service starts only after the user taps Join on a booking and stops as soon as the call ends or the user taps End call. No audio is recorded or stored on the device.
```

**Impact if deferred:**

```
The call is a live, scheduled and often paid session with a fixed time. If Android deferred the foreground service, the microphone would not start capturing when the user joins with the screen about to lock or the app leaving the foreground, so the other person cannot hear the user from the start of the session. The user would have to keep the screen on and the app in the foreground for the whole session, and a deferred service breaks the core purpose of the app.
```

**Impact if interrupted:**

```
The call is a live, scheduled and often paid session with a fixed time. If the foreground service is interrupted while the app is in the background or the screen is off, audio capture is cut mid-session: the other person can no longer hear the user, the session drops, and both people lose paid time with no way to get it back.
```

**Video link:** (user-supplied, unlisted, 30-60 s). Steps to record:

1. Open the app signed in as a client and go to Upcoming Booking Details for a confirmed session inside the join window (a free test booking is enough).
2. Tap Join and show the microphone prompt appearing; tap Allow so the user is seen starting the microphone (Google's Background Audio Access rule: show the user initiating the microphone). Show the call screen connected.
3. Press Home so the app leaves the foreground; pull down the shade and show the ongoing ClickMe notification (expert name, timer, End call).
4. Turn the screen off and on again; show the call is still running.
5. Tap the notification to return to the call, then tap End call (or the in-app leave button) and show the notification disappears.

If the app already holds the microphone permission on the recording device, revoke it in system settings first so the prompt in step 2 is visible.

**Notes for the reviewer:**

```
The type is declared in the manifest and the service is not exported. It starts only from a foreground Join tap and no audio is recorded or stored. No other foreground service exists in the app. Push messages use Firebase Cloud Messaging and need no foreground service.
```

## 9. Other declarations

Console path: Policy and programs > App content (the remaining tasks Console lists)

| Declaration | Answer |
|---|---|
| News app | No |
| COVID-19 contact tracing or status | No |
| Government app | No |
| Health apps | No (or "not applicable", as Console phrases it) |
| Data safety | See section 6 |

Use what Console lists; answer No to any other program declaration the app does not belong to. The NFC permission comes from Stripe's SDK; Console is not expected to ask about it (flagged assumption A12, confirmed 2026-11-06, row 3: no Console action). If it does, answer that NFC is optional card-tap input inside Stripe's PaymentSheet and the app never reads NFC tags itself.

## 10. Store listing

Console path: Grow users > Store presence > Main store listing

**App name:** `ClickMe`

**Short description** (limit 80 characters; this text is 68):

```
Book 1-on-1 voice sessions with experts. Chat, pay securely, review.
```

**Full description** (limit 4,000 characters; this text is 2120 characters). Every sentence describes a shipped screen (feature-truth pass against Phases 16.1 to 16.5). Nothing about recordings, replays, digital goods, tips or credits, and no ranking or promotional wording:

```
ClickMe connects you with real people who know their craft, through live one-on-one voice sessions.

Find an expert
Browse featured topics and trending categories on the Explore tab, or use the Search tab to search by expertise, language and category. Open any expert's profile to see their background, topics, prices and reviews from other clients, and save experts to come back to them later.

Book a session
Pick a topic, choose a day and a time slot shown in your own time zone, add a note for the expert and send your request. Many topics are free. Paid sessions are paid securely through Stripe and you are told clearly what will be charged and when. You can follow every request from My Bookings, reschedule or cancel from the booking details, and see past sessions. Add a private note to any booking that only you can read, and review your charges in Payment History.

Talk by voice
Join your session at the scheduled time with in-app voice. The call keeps running when you leave the app or turn the screen off, with an ongoing notification that lets you return or end the call. Some experts also meet on Skype or Zoom, and ClickMe opens the link for you.

Chat before and after
Message an expert from their profile or from a booking, and keep the conversation in one place. You can report or block a conversation at any time.

Reviews you can trust
After a session you can leave a review so other clients know what to expect.

Share what you know
Switch to Expert Mode to build your profile, add your topics and prices, set your weekly availability and your time zone, and review incoming booking requests. Accept or decline with a reason, see your upcoming sessions on a dashboard, and message your clients.

Stay in the loop
Notifications tell you when a request is answered, a booking changes, a session is about to start or a message arrives. You choose which categories you want in the app's Notification Preferences and in your device's notification settings.

ClickMe works with the ClickMe app on iOS, so you can book and talk with experts on either platform.

Questions or feedback: clickmeapp@uptrendinvestments.net
```

**Category:** Business (fallback if Console rejects it: Education). **Tags:** pick the closest from Console's list (for example Education, Business).

**Contact details**

- Email: `clickmeapp@uptrendinvestments.net`
- Website: `https://www.uptrendinvestments.net/clickme/`
- Privacy policy URL: `https://www.uptrendinvestments.net/clickme/privacy.html` (as section 1)

**Graphics.** Exact specs (Google Play Console Help, store listing assets, answer 9866151):

| Asset | Spec | Who makes it | Where it is |
|---|---|---|---|
| App icon | 512 x 512, 32-bit PNG with alpha, at most 1024 KB; no text claims, no ranking, price or category text | Executor, plan 17-05 | `/Users/LeonChen/ClickMe-release/store-assets/play-icon-512.png` |
| Feature graphic | 1024 x 500, JPEG or 24-bit PNG with no alpha; avoid fine detail and anything near the edges, which can be cut off | The user (D-16) | The user's own folder; no repo path |
| Phone screenshots (2 to 8) | JPEG or 24-bit PNG with no alpha; each side 320-3840 px; longest side at most twice the shortest; at least four at 1080 x 1920 (9:16) to be eligible for featuring; real app UI only, no device frames, a clean status bar, no third person's name, photo or messages | Executor, plan 17-05; the in-call screenshot is added after smoke run 1 | `/Users/LeonChen/ClickMe-release/store-assets/`, manifest `17-STORE-ASSETS.md` |
| Foreground-service video | 30-60 s screen recording, unlisted YouTube link; steps in section 8 | The user (D-16) | Unlisted YouTube link pasted in section 8 |

Check a graphic before upload (the icon must report `hasAlpha: yes`; the feature graphic and every screenshot must report `hasAlpha: no`):

```
sips -g pixelWidth -g pixelHeight -g hasAlpha <file>
```

## 11. Release

Console path: Test and release > Testing > Closed testing, and Test and release > Production

**Track (row 12, 2026-11-06):** the production-access gate is not shown, so vc 6 goes to **Internal testing** first (Test and release > Testing > Internal testing), then to Production after smoke run 1. The closed-testing text below is not needed and is kept only in case the gate appears later.

**Countries**

- Production: Canada and United States only (D-12). More can be added in Console later.
- Closed testing track: its own country list, because every track has its own setting and paid tester pools are worldwide (RESEARCH Pitfall 6, A2). Use the countries the tester service states, or all countries. Do not copy the Production list onto the closed track, or the testers cannot see or install the app.
- minSdk is 26, so testers need Android 8.0 or newer.

**Release name:** `1.0.0 (6)`. Every later upload to any track is the newest ledger versionCode plus 1 (D-10), for example `1.0.0 (7)` for the first closed-track fix build.

**Release notes** (every track; limit 500 characters; this text is 116 characters):

```
First public release of ClickMe: find experts, book one-on-one voice sessions, chat, pay securely and leave reviews.
```

**Closed-track tester instructions** (no guided task list, D-03). Paste into the tester message or hand to the tester service:

```
Thanks for testing ClickMe.
1. Install the app from the opt-in link and stay opted in for the whole 14 days.
2. Sign up as a client with your own email address.
3. Use the app now and then as you like: browse experts, open profiles, use chat and settings.
4. Book free topics only. Never enter a real card: payments in this build are live.
5. Report problems with Google Play's testing feedback or by email to clickmeapp@uptrendinvestments.net.
```
