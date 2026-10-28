# Phase 13 Play Console Drafts (paste-ready)

Source for REL-04. Paste into Play Console (organization account), then confirm in checklist step 7. Derived from what the app actually requests and sends (manifest permissions, the REST contract shared with iOS, Phases 4 to 13). No secrets in this file. Items marked "user-supplied" or listed under "Decisions for the user" need your input.

## 1. Store listing (Grow users, Main store listing)

**App name:** ClickMe

**Short description** (limit 80 characters; this text is 68):

```
Book 1-on-1 voice sessions with experts. Chat, pay securely, review.
```

**Full description** (limit 4000 characters; this text is well under):

```
ClickMe connects you with real people who know their craft, through live one-on-one voice sessions.

Find an expert
Browse featured topics and trending categories on the Explore tab, search by expertise, language and category, and open any expert's profile to see their background, topics, prices and reviews from other clients.

Book a session
Pick a topic, choose a day and a time slot shown in your own time zone, add a note for the expert and send your request. Many topics are free. Paid sessions are paid securely through Stripe and you are told clearly what will be charged and when. You can follow every request from My Bookings, reschedule or cancel from the booking details, and see past sessions.

Talk by voice
Join your session at the scheduled time with in-app voice. The call keeps running when you leave the app or turn the screen off, with an ongoing notification that lets you return or end the call. Some experts also meet on Skype or Zoom, and ClickMe opens the link for you.

Chat before and after
Message an expert from their profile or from a booking, and keep the conversation in one place. You can report or block a conversation at any time.

Reviews you can trust
After a session you can leave a review so other clients know what to expect.

Share what you know
Switch to Expert Mode to build your profile, add your topics and prices, set your weekly availability and your time zone, and review incoming booking requests. Accept or decline with a reason, see your upcoming sessions on a dashboard, and message your clients.

Stay in the loop
Notifications tell you when a request is answered, a booking changes, a session is about to start or a message arrives. You choose which categories you want in your device's notification settings.

ClickMe works with the ClickMe app on iOS, so you can book and talk with experts on either platform.

Questions or feedback: support@clickme.app
```

**Category:** Business (alternative if Console rejects it: Education). **Tags:** pick the closest from Console's list (for example Education, Business).

**Contact details**
- Email: `support@clickme.app`
- Website (optional): `https://www.uptrendinvestments.net/clickme/`
- Privacy policy URL: `https://www.uptrendinvestments.net/clickme/privacy.html` (same value as `AppLinks.PRIVACY_POLICY` in the app)

**Graphics (all user-supplied):**
- [ ] App icon 512 x 512 PNG (32-bit, no alpha needed, max 1 MB)
- [ ] Feature graphic 1024 x 500 PNG or JPG
- [ ] At least 2 phone screenshots (suggested: Explore, an expert profile, Make a Booking, My Bookings, a chat thread, the in-call screen)

## 2. Data safety (App content, Data safety)

Not required for an internal-only app, but drafted now so the first production review is not blocked. Answer "Yes, the app collects or shares user data". No analytics, advertising or crash-reporting SDK is in the app; no location, contacts or SMS permission is requested.

| Data type (Console category) | What it is | Collected | Shared | Ephemeral | Required or optional | Purposes |
|---|---|---|---|---|---|---|
| Personal info: Name | Display name from signup | Yes | No | No | Required | App functionality, Account management |
| Personal info: Email address | Account email, login and recovery codes | Yes | No | No | Required | App functionality, Account management |
| Personal info: User IDs | Account id used on bookings, chats and reviews | Yes | No | No | Required | App functionality, Account management |
| Personal info: Other info | Bio, expertise tags, languages, country and province, time zone, expert availability, topic titles and prices | Yes | No | No | Required for experts; optional for clients | App functionality, Personalization |
| Photos and videos: Photos | Profile photo chosen with the system photo picker and uploaded | Yes | No | No | Required for experts; optional for clients | App functionality |
| Messages: Other in-app messages | Chat messages between clients and experts, report and block reasons | Yes | No | No | Optional | App functionality |
| Audio: Voice or sound recordings | Live voice during a session, streamed through Agora (a service provider); the app does not record or store audio on the device | Yes | No (Agora is a service provider acting for the app) | Yes (processed in real time, not stored by the app) | Optional (needed to take part in a call) | App functionality |
| Financial info: Other financial info (payment info) | Card details entered in Stripe's PaymentSheet; Stripe collects them, the app never reads card numbers. Booking amounts and payout status | Yes (conservative default) | No (Stripe is a service provider) | No | Optional (only for paid sessions and expert payouts) | App functionality |
| App activity: Other user-generated content | Reviews, booking notes, cancellation and decline reasons | Yes | No | No | Optional | App functionality |
| App activity: In-app actions / booking details | Booking requests, times, status | Yes | No | No | Required for bookings | App functionality |
| Device or other IDs | The Firebase Cloud Messaging push token and an app-generated random device id (not the advertising id, not the hardware id) | Yes | No (Google Firebase Cloud Messaging is a service provider that delivers the push) | No | Required for push notifications | App functionality (push notifications) |

Security practices:
- Data is encrypted in transit: Yes (TLS to the API, the realtime socket and Stripe/Agora).
- Users can request that data is deleted: Yes. In-app account deletion arrives in Phase 14; the web deletion-request URL Console asks for is a Phase 17 item (REL-06). Until then the support email `support@clickme.app` handles requests.
- On-device storage: Console has no question for it. For your own reference, sign-in tokens are kept in encrypted storage and app data is excluded from cloud backup and device transfer.
- Committed to the Play Families Policy: No. Independent security review: No.

Decisions for the user:
- [ ] Card data: the conservative default above declares financial info as collected because the user types it into a screen the app shows. If you decide that Stripe's PaymentSheet means the app does not collect it, change the row and keep a note why. Do not remove it without checking Play's "payments and third-party SDK" guidance.
- [ ] NFC: Stripe's SDK adds the `NFC` permission (Phase 7 note). Justify it as optional card scanning inside PaymentSheet; the app never reads NFC tags itself. No data safety row is needed for it, but expect a question if Console lists the permission.
- [ ] Push content: pushes carry short booking and chat text through Google's FCM; the "Device or other IDs" row covers the token. If a push carries chat message text, say so in the privacy policy.
- [ ] Confirm the privacy policy text names Agora, Stripe and Firebase Cloud Messaging as service providers before submitting.

## 3. Foreground service permission declaration (App content, Foreground service permissions)

Service: `com.uptrendinvestments.clickmeapp.services.call.CallService`, declared with `android:foregroundServiceType="microphone"`; permission `FOREGROUND_SERVICE_MICROPHONE` (plus `FOREGROUND_SERVICE`, `RECORD_AUDIO`).

**Foreground service type:** Microphone (`FOREGROUND_SERVICE_MICROPHONE`)

**Functionality description:**

```
ClickMe is a marketplace for live one-on-one voice sessions between a client and an expert. When a user joins a booked session, CallService starts as a microphone foreground service so the two-way voice call continues while the user leaves the app (for example to check a note, a calendar or a message) or turns the screen off. The service shows an ongoing notification with the expert's name, the elapsed time and an "End call" action, and tapping it returns the user to the call. The service starts only after the user taps Join on a booking and stops as soon as the call ends or the user taps End call. No audio is recorded or stored on the device.
```

**User impact if the foreground service is deferred or interrupted:**

```
The call is a live, scheduled and often paid session with a fixed time. If Android stops audio capture when the app moves to the background or the screen turns off, the microphone is cut mid-session: the other person can no longer hear the user, the session drops, and both people lose paid time with no way to get it back. Users would have to keep the screen on and the app in the foreground for the whole session. A deferred or interrupted service therefore breaks the core purpose of the app.
```

**Use case:** Background audio access (choose the closest category Console offers for an ongoing voice call; if it lists "Phone call" or "Communication", use that).

**Video link:** [user-supplied] screen recording, 30 to 60 seconds, unlisted link. Steps to record:
1. Open the app signed in as a client and go to Upcoming Booking Details for a confirmed session inside the join window (a free test booking is enough).
2. Tap Join. Accept the microphone prompt. Show the call screen connected.
3. Press Home so the app leaves the foreground; pull down the shade and show the ongoing ClickMe notification (expert name, timer, End call).
4. Turn the screen off and on again; show the call is still running.
5. Tap the notification to return to the call, then tap End call (or the in-app leave button) and show the notification disappears.

**Notes for the reviewer:** the type is declared in the manifest and the service is not exported. No other foreground service exists in the app. Push messages use FCM and need no foreground service.
