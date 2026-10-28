# Phase 13: Push, Notifications & Internal Testing - Context

**Gathered:** 2026-10-27
**Status:** Ready for planning

<domain>
## Phase Boundary

Push notifications bring users to the right screen, clients can write reviews, and a signed build is on the Play internal testing track. This phase delivers:
- FCM token registration (`POST /notifications/tokens`, `platform: "android"`) on every app start and on token rotation. The token is unregistered (`DELETE /notifications/tokens/:device_id`) on logout through the existing `PushTokenRegistrar` hook.
- The `POST_NOTIFICATIONS` permission on Android 13+, notification channels, and foreground display.
- A port of `PushRouter` (category/sub_category, with the legacy `event_type` fallback, mapped to a `PushDestination`) and routing into the right mode, tab and back stack from a cold start and while the app is running.
- The notifications list (`GET /notifications`), grouped by date. Tapping an item routes through the same destination map.
- Write Review (`GET /bookings/:id/reviews/context`, `POST /bookings/:id/reviews`), reached from Booking Summary, the Past booking card, and the `session/ended` / `review/nudge` pushes.
- A human checklist covering Firebase setup, the upload keystore, the first signed upload, the internal track, and Play Console drafts (store listing, data safety, foreground-service declaration).

Requirements: PUSH-01..05, REV-01, REL-04, REL-05. Payout, KYC and security destinations get their real screens in Phase 14.

</domain>

<decisions>
## Implementation Decisions

Carried forward (not re-opened):
- iOS is the source of truth (`PushRouter.swift`, `ClickMe2026AppDelegate.swift`, `UserManager.requestPushPermissionIfNeeded`, `NotificationsListViewModel.swift`, `Write Review/`). Deviations are listed below.
- Routing keys from earlier phases:
  - `BookingSummaryKey(bookingId)` through `Navigator.openBookingSummary` (Phase 8 hand-off).
  - `RequestDecisionKey`, which must carry `isExpired` and the expiry, because the detail endpoint has no status (Phase 11, C3).
  - `ExpertBookingSummaryKey(bookingId, origin)` (Phase 11).
  - `ChatThreadKey(threadId)` posted to `ChatLaunchRequest.thread`, or `Navigator.openChatThread(chatsRootFor(mode), key)`. The thread fills peer seeds from `GET /chats` (Phase 12).
- Phase 8 D-16: Booking Summary's Leave a Review CTA and Feedback block were hidden and are restored here. The Past booking card also gains Review here.
- Cut order if the schedule slips: the notifications list (PUSH-05) is cut first, then Write Review (REV-01). Push deep links alone are enough.
- Manual and device testing doesn't block. Log it as open items in STATE.md.
- API pattern: `ApiRequestUrls` constants, `ClickMeApi` methods, a Hilt `@Singleton` repository using `apiCall {}`, and Moshi codegen DTOs with explicit `@Json(name=…)`. Error copy is the server `message` in an `AlertDialog`.
- Dark theme only, Material 3, Outlined icons, AutoMirrored back arrows.

### Permission & channels (PUSH-01, PUSH-02)
- **D-01: Ask for `POST_NOTIFICATIONS` right after a successful login or signup**, matching iOS `requestPushPermissionIfNeeded`. It does nothing if the user has already answered and does nothing below API 33. There's no pre-prompt rationale screen.
- **D-02: If the user denies, stay quiet.** Never prompt again. The FCM token is still registered. The notifications list shows a small "Notifications are off — Turn on" row at the top, which opens the system app-notification settings (`Settings.ACTION_APP_NOTIFICATION_SETTINGS`). The row is hidden when notifications are enabled.
- **D-03: Per-category notification channels:** Bookings, Sessions, Messages, Account & payouts, and a General fallback. General is the manifest `default_notification_channel_id`. The existing `CallService` channel stays separate and unchanged. Exact channel ids, names and importance are Claude's discretion (Messages and Sessions at high importance).
- **D-04: Change the backend so channels apply in the background too.** `backend-reference/src/services/push.js` sets `android.notification.channel_id` from the push `category`: booking→Bookings, session→Sessions, message→Messages, payout/account→Account & payouts, anything else→General. The `apns` block and iOS behavior must not change. The change gets its own backend task, deployed and checked with `/health` the same way as the Phase 11 G-11-6 fix. The Android channel ids are the contract and must match exactly. — **Reversibility:** costly — the ids become a contract between the backend payload and the Android channels, and renaming a channel later orphans the user's per-channel settings.

### Routing (PUSH-03, PUSH-04)
- **D-05: Mode mismatch: switch mode, then route.** If the destination belongs to the other mode, run the existing Phase 4 mode switch (AppViewModel / UserManager), then push the destination into the right tab of the new mode. Examples are an expert-only request or booking while the user is in client mode, or a client booking while in expert mode. This deviates from iOS, which routes into whichever shell is showing. The researcher works out how to tell the role: payload fields first (check `backend-reference/src/services/events.js` for what `data` carries per event), otherwise a lightweight fetch. If the role can't be determined, route in the current mode.
- **D-06: A client's booking push lands by booking status.** This applies to `booking/*` (except `requests`), `session/reminder`, `session/no_show` and `review/received`. Fetch the booking first. An upcoming, pending or confirmed booking opens Upcoming Booking details (`UpcomingBookingKey`). A completed, cancelled, declined or past booking opens `BookingSummaryKey`. Experts get `ExpertBookingSummaryKey(bookingId, origin)` (iOS parity).
- **D-07: `session/ended` and `review/nudge` (leaveReview).** For a client, open `BookingSummaryKey` on the Bookings tab, and the user taps Leave a Review from there (iOS). For an expert, open `ExpertBookingSummaryKey` (iOS: experts don't leave reviews).
- **D-08: `booking/requests` for an expert.** If the payload has a request or booking id, open `RequestDecisionKey` directly, with `isExpired` and the expiry taken from the incoming-requests list (`GET /expert/booking-requests`). Without an id, open the incoming-requests list on the Dashboard stack. This deviates from iOS, which always opens the requests list. A client shell that gets this push falls back to the notifications list (iOS).
- **D-09: `message/new_message`.** Switch to the Chats tab of the current mode and post `ChatThreadKey(threadId)` (Phase 12 hand-off).
- **D-10: `payout/*` and `account/*` open the notifications list for now.** Treat them as unrouted until Phase 14 adds Payouts and Security. Leave a clear marker (e.g. `// PHASE-14`) in the destination mapping so Phase 14 can retarget them. Unknown categories, and pushes missing their id, also open the notifications list (iOS).
- **D-11: The notifications list opens on the current mode's Explore stack (client) or Dashboard stack (expert)**, the same stack the bell pushes onto.
- **D-12: Foreground arrival: post a system heads-up notification** on the push's channel, matching the iOS banner plus list behavior. Tapping it routes the same way as a background tap. Every foreground arrival also sends a refresh signal so an open notifications list silently reloads (iOS `newNotificationTick`).
- **D-13: Hide a chat push when that thread is on screen.** If `message/new_message` arrives in the foreground for the thread that's currently visible, don't post it. The realtime message already shows up. Other threads still notify. This deviates from iOS.
- **D-14: A tap while logged out or with an expired session is dropped.** Show Login only. Don't hold the destination for after login.
- **D-15: One pending destination at a time.** A newer tap overwrites an unconsumed one (iOS). It must work from a cold start (the launcher activity's intent extras from a notification the system displayed) and while the app is running (`onNewIntent`), and it must survive the splash → session-restore → home sequence before it's consumed.

### Notifications list (PUSH-05)
- **D-16: iOS parity.**
  - Load `GET /notifications?page=1&limit=50` (first 50 only, no paging).
  - Group into Today / Yesterday / Earlier.
  - Pull-to-refresh, plus a silent reload on a push arrival (D-12).
  - An empty state.
  - No read or unread state (the backend has no mark-read endpoint).
  - Tapping a row routes through the same destination map, using ids from `meta_data` (the iOS key aliases: `threadId`/`thread_id`/`chatThreadId`/`chat_thread_id`, plus the booking-id equivalents).
- **D-17: Entry points.** The bell on the client Explore top bar and the bell on the expert Dashboard header (`DashboardHeader.kt` already has the `NotificationsNone` icon) each push the list onto their own tab stack.

### Write Review (REV-01)
- **D-18: Entry points.** Restore Booking Summary's "Leave a Review" CTA and the Feedback block, which shows the submitted review (reverses the Phase 8 D-16 hide). Add a "Leave a review" affordance on the Past booking card for completed bookings that have no review yet. Pushes reach review through Booking Summary (D-07).
- **D-19: Presentation and flow.**
  - A full-screen destination pushed onto the same tab stack (port of `WriteReviewView`: brand, expert avatar, headline, star rating, text field, submit).
  - It loads `GET /bookings/:id/reviews/context`. If the user has already submitted, it is read-only with the prefilled rating and comment and an already-submitted banner (iOS).
  - On a successful submit it pops back to Booking Summary, which reloads to show the Feedback block, and shows a "Thanks for your review" snackbar.
  - Write Review is client-only.

### Internal testing & Play Console (REL-04, REL-05)
- **D-20: Firebase uses the same project as iOS.** The user adds the Android app (`com.uptrendinvestments.clickmeapp`) to the existing iOS Firebase project and puts `google-services.json` in `app/`. Both flavors share the package id, so one file covers both. The backend's firebase-admin credential is unchanged. The plan provides exact step-by-step instructions, including the debug and upload SHA-1 if the console asks. Code plans should build and pass CI without the real file, using either a guarded plugin or a committed placeholder file. Claude decides which, as long as CI is green.
- **D-21: Human-only steps go in one non-blocking checklist plan.** The steps:
  - Create the upload keystore and `keystore.properties` (the Phase 2 02-05 deferral).
  - Run a signed `bundleProdRelease` and upload it to the internal track. This also closes REL-03.
  - Add testers.
  - Paste the pre-written Play Console drafts: the store listing copy, the data safety form answers (derived from what the app actually collects and sends: account and profile data, voice audio through Agora, payments through Stripe, FCM token, chat content), and the foreground-service (microphone) declaration text for `CallService`.
  - Re-check the closed-testing requirement in Play Console.
  - Check the Google Play Payments policy for paying for live expert calls through Stripe (the STATE.md "before release (Phase 13)" item).
  
  Code plans don't wait on any of these. Each one is logged in STATE.md as an open item until the user confirms it's done.
- **D-22: The user is the only internal tester for now.** This partly falls short of SC-5's "a few real users on a mix of devices". Record the gap in VERIFICATION and STATE so a wider tester group is added by Phase 16 cross-platform QA.

### Claude's Discretion
- Channel ids, display names, descriptions and importance levels (D-03), as long as the backend mapping in D-04 uses the same ids.
- Small icon and accent color for notifications. Notification content comes from the FCM `notification` title and body as the backend sends it.
- How the "which thread is visible" signal is exposed to the messaging service (D-13).
- Device id source for token registration and deregistration. It must stay stable across app restarts, and must match what `DELETE /notifications/tokens/:device_id` expects. Check the backend `registerTokenSchema`.
- The holder for the pending destination (a `@Singleton` with a `OneShotSlot`, like `ChatLaunchRequest` and `ExpertLaunchRequest`, or similar) and how each home shell consumes it.
- How the Booking Summary Feedback block shows rating and comment, and the exact affordance on the Past card.
- Whether to use a placeholder `google-services.json` or a guarded plugin for CI (D-20).

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Push routing & registration (iOS source of truth)
- `ios-reference/ClickMe2026/Managers/PushRouter.swift`: the `PushDestination` enum and the category/sub_category mapping table
- `ios-reference/ClickMe2026/ClickMe2026AppDelegate.swift`: FCM delegate, token handling, `willPresent` (foreground) and `didReceive` (tap), and `PushEvent` parsing including the legacy `event_type` fallback
- `ios-reference/ClickMe2026/Managers/UserManager.swift` (around lines 170–215, 470–500): `requestPushPermissionIfNeeded` call sites (login and signup), token register and unregister
- `ios-reference/ClickMe2026/Views/Screens/Client Mode/Home/HomeClientView.swift` (`handlePushDestination`): client shell routing
- `ios-reference/ClickMe2026/Views/Screens/Expert Mode/Home/HomeExpertView.swift` (`handlePushDestination`): expert shell routing
- `ios-reference/ClickMe2026/Network/APIRequestURLs.swift`: notification and review endpoint paths

### Notifications list & Write Review
- `ios-reference/ClickMe2026/Views/Screens/Notification/NotificationsListView.swift` and `NotificationsListViewModel.swift`: date grouping, `meta_data` id aliases, `destination(for:)`
- `ios-reference/ClickMe2026/Views/Screens/Client Mode/Write Review/` (WriteReviewView, WriteReviewViewModel, Subviews/): review UI, context load, submit
- `ios-reference/ClickMe2026/Views/Screens/Client Mode/Booking Summary/BookingSummaryView.swift`: Leave a Review CTA and the submitted-review block
- `ios-reference/ClickMe2026/Views/Screens/Client Mode/My Bookings/MyBookingsView.swift`: the Write Review entry from My Bookings

### Backend contract
- `backend-reference/src/services/push.js`: FCM message shape (`notification` + string-only `data`, `android.priority: high`). This is the target of the D-04 `channel_id` change.
- `backend-reference/src/services/events.js`: which `data` fields (category, sub_category, booking_id, thread_id, role hints) each event sends, which D-05/D-08 need
- `backend-reference/src/routes/notifications.js` and `backend-reference/src/schemas/notification.schema.js`: `POST /notifications/tokens`, `DELETE /notifications/tokens/:device_id`, `registerTokenSchema`
- `backend-reference/src/routes/notificationHistory.js`: `GET /notifications` (and the preferences endpoints, which are not in scope)
- `backend-reference/docs/ios/`: authoritative specs. Check for any push, notification or review doc before relying on iOS behavior.

### Project
- `.planning/ROADMAP.md` § Phase 13: goal, success criteria, planning notes and hand-offs
- `.planning/REQUIREMENTS.md`: PUSH-01..05, REV-01, REL-03..06, and the cut order
- `.planning/STATE.md`: Phase 8/11/12 hand-offs, the Play Payments policy check, the Phase 2 keystore and REL-03 deferrals, and the closed-test re-check

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `managers/PushTokenRegistrar.kt`: an open no-op hook. `UserManager.logout` already calls `unregister()` before clearing tokens. Phase 13 fills it in and adds `register()`.
- `screens/chat/ChatLaunchRequest.kt` (`OneShotSlot<ChatThreadKey>`), `screens/expertmode/ExpertLaunchRequest.kt` and `screens/clientmode/mybookings/BookingsLaunchRequest.kt`: the established cross-tab launch-request pattern for push routing.
- `navigation/Navigator.kt`: `openBookingSummary`, `openChatThread`, tab selection and per-tab back stacks (Nav3).
- `screens/expertmode/requestdecision/RequestDecisionKey.kt` and `incomingrequests/IncomingRequestsLogic.kt`: building the key with `isExpired` and the expiry.
- `screens/clientmode/upcomingbooking/UpcomingBookingKey.kt` and `bookingsummary/BookingSummaryKey.kt`: client booking destinations (D-06).
- `screens/settings/modeswitching/` and `AppViewModel.kt`: the existing mode switch for D-05.
- `services/call/CallNotificationFactory.kt`: the existing channel and notification construction pattern, and the call channel that stays separate.
- `screens/call/calling/MicPermissionRequester.kt`: the runtime-permission request pattern to mirror for `POST_NOTIFICATIONS`.
- `screens/expertmode/dashboard/subviews/DashboardHeader.kt`: the bell icon already exists, with its handler to wire up.
- Shared `EmptyState` / `ErrorState`, `AsyncSaveButton`, `Avatar` (Phase 1).

### Established Patterns
- Hilt `@Singleton` repositories with `apiCall {}`, Moshi DTOs, `ApiRequestUrls`.
- Origin-carrying Nav3 keys keep the same screen distinct across tab stacks (Phase 10/11 Pitfall 7).
- Refresh: a quiet reload when the screen is shown again, plus pull-to-refresh (Phase 8 D-10).
- `BookingSummaryScreen.kt`, `BookingSummaryUiState.kt` and `PastBookingCard.kt` carry "Phase 13" comments where Review and Feedback plug in.

### Integration Points
- `MainActivity`: notification-tap intent extras on cold start and `onNewIntent` → the pending destination → consumed once a home shell is up after session restore.
- A new `FirebaseMessagingService`: `onNewToken` → register, and `onMessageReceived` → foreground post (D-12/D-13) plus the list refresh signal.
- `UserManager` login and signup success → permission request (D-01) and token register.
- Manifest: `POST_NOTIFICATIONS` permission, messaging service, and `default_notification_channel_id` meta-data. Gradle: google-services plugin and firebase-messaging (BoM).
- Backend repo (`backend-reference`): the `push.js` `channel_id` mapping (D-04).

</code_context>

<specifics>
## Specific Ideas

- The denied-permission hint row reads "Notifications are off — Turn on".
- The post-submit snackbar reads "Thanks for your review".
- Channel set: Bookings, Sessions, Messages, Account & payouts, General.

</specifics>

<deferred>
## Deferred Ideas

- Notifications list paging beyond 50 and read/unread state. This needs a backend mark-read endpoint, so it's a future item.
- Holding a tapped push across login (D-14 chose to drop it). Revisit if testers complain.
- A wider internal tester group on a mix of devices. Do this by Phase 16 cross-platform QA (D-22).
- Real Payouts, KYC and Security destinations for `payout/*` and `account/*` pushes: Phase 14 (D-10 marker).
- In-app notification preferences (`/notifications/preferences`): not in the MVP scope.

</deferred>

---

*Phase: 13-push-notifications-internal-testing*
*Context gathered: 2026-10-27*
