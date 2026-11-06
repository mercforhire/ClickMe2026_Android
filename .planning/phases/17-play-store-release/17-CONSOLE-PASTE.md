# Phase 17 Play Console Paste File

This file supersedes `.planning/phases/13-push-notifications-internal-testing/13-PLAY-CONSOLE-DRAFTS.md` as the single source for every Play Console App content task, the store listing and the release fields (REL-04, REL-06). Paste from the fenced blocks only. The sections follow the order of the Console's App content and release screens; the "Needs attention" tab on App content is the authority for which tasks exist, so use what Console lists.

This file contains no password, keystore value or token. The reviewer password is typed into Console by the account owner.

Answers that depend on the 17-08 checkpoint carry "(confirm at 17-08 #n)", where n is the row number of the table in `17-PAYMENTS-DECISION.md` section 2. Do not submit those answers until 17-08 has confirmed them.

## 1. Privacy policy

Console path: Policy and programs > App content > Privacy policy

(stub: replaced in task 2)

## 2. App access

Console path: Policy and programs > App content > App access

(stub: replaced in task 2)

## 3. Ads

Console path: Policy and programs > App content > Ads

(stub: replaced in task 2)

## 4. Content rating

Console path: Policy and programs > App content > Content rating

(stub: replaced in task 2)

## 5. Target audience and content

Console path: Policy and programs > App content > Target audience and content

(stub: replaced in task 2)

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
| Messages: Other in-app messages | Chat messages between clients and experts, report and block reasons. Notifications can carry a message preview of up to 140 characters delivered by Firebase Cloud Messaging (a service provider) (confirm at 17-08 #4) | Yes | No | No | Optional | App functionality |
| Audio: Voice or sound recordings | Live voice during a session, streamed through Agora (a service provider acting for the app); the app does not record or store audio | Yes | No (Agora is a service provider acting for the app) | Yes (processed in real time, not stored by the app) | Optional (needed to take part in a call) | App functionality |
| Financial info: User payment info | Card details typed into Stripe's PaymentSheet; Stripe's SDK sends them from the device, and the app never reads card numbers | Yes | No, Stripe is a service provider (confirm at 17-08 #2) | No | Optional (only for paid sessions) | App functionality |
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

- The NFC permission comes from Stripe's SDK (the app's own manifest has none) and needs no Data safety row (confirm at 17-08 #3).
- No advertising ID is used and no ad, analytics or crash-reporting SDK is in the app.
- Committed to Play Families Policy: No. Independent security review: No.
- The Data safety answers must match the published privacy policy after edits E1-E4 in `17-PRIVACY-POLICY-EDITS.md`. Publish those edits before submitting this form.
- Sign-in tokens are kept in encrypted storage and app data is excluded from cloud backup and device transfer. Console has no question for this; the note is for your own reference.

## 7. Financial features

Console path: Policy and programs > App content > Financial features

(stub: replaced in task 2)

## 8. Foreground service permissions

Console path: Policy and programs > App content > Foreground service permissions

(stub: replaced in task 2)

## 9. Other declarations

Console path: Policy and programs > App content (the remaining tasks Console lists)

(stub: replaced in task 2)

## 10. Store listing

Console path: Grow users > Store presence > Main store listing

(stub: replaced in task 3)

## 11. Release

Console path: Test and release > Testing > Closed testing, and Test and release > Production

(stub: replaced in task 3)
