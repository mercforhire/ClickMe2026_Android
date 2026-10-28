# API Coverage — Firebase Cloud Messaging (Android SDK and the backend sender) and the ClickMe notification / review endpoints (Phase 13)

> Full coverage by default. Opt-outs are explicit, reasoned decisions.
> Surface enumerated from `backend-reference/src/routes/{notifications,notificationHistory,reviews}.js`, `backend-reference/src/services/push.js` (read 2026-10-28, 13-RESEARCH.md F-1, F-8), the `firebase-messaging` 26.0.1 public API used by Android apps, and the FCM v1 `android` message block the backend sends.

| capability | decision | reason |
|---|---|---|
| POST notifications/tokens (register the device token, platform android) | INTEGRATE | |
| DELETE notifications/tokens/:device_id (unregister on logout) | INTEGRATE | |
| GET notifications (page 1, limit 50) | INTEGRATE | |
| GET notifications paging beyond page 1 | OPT-OUT | not needed yet: first 50 only is iOS parity and a deferred idea (CONTEXT D-16, Deferred Ideas) |
| GET notifications?category= filter | OPT-OUT | not needed: the list shows every category, as on iOS |
| GET notifications/preferences and PATCH notifications/preferences | OPT-OUT | explicitly out of scope: in-app notification preferences are not in the MVP (CONTEXT Deferred Ideas) |
| GET bookings/:id/reviews/context | INTEGRATE | |
| POST bookings/:id/reviews with rating and comment | INTEGRATE | |
| POST bookings/:id/reviews with tags | OPT-OUT | explicitly out of scope: review tags are excluded by the UI-SPEC |
| Re-submitting (upserting) an existing review | OPT-OUT | explicitly out of scope: a submitted review is read-only (CONTEXT D-19) |
| GET experts/:id/reviews | OPT-OUT | not needed: no Phase 13 screen lists an expert's reviews (the Expert Profile reviews shipped in Phase 5) |
| FirebaseMessaging.getToken | INTEGRATE | |
| FirebaseMessagingService.onNewToken (token rotation) | INTEGRATE | |
| FirebaseMessagingService.onMessageReceived (foreground notification messages) | INTEGRATE | |
| System-displayed background notification and launcher-intent data extras on tap | INTEGRATE | |
| FirebaseMessaging.deleteToken on logout | INTEGRATE | |
| Manifest default_notification_channel_id, default_notification_icon, default_notification_color | INTEGRATE | |
| Server-chosen android.notification.channel_id (backend push.js) | INTEGRATE | |
| Android-only category and sub_category in FCM data (backend push.js) | INTEGRATE | |
| Topic messaging (subscribeToTopic / unsubscribeFromTopic) | OPT-OUT | not needed: the backend addresses device tokens, never topics |
| Data-only messages, onDeletedMessages, upstream send (onMessageSent / onSendError) | OPT-OUT | not needed: the backend sends notification messages only and the app sends nothing upstream |
| setAutoInitEnabled / delivery metrics export / Analytics integration | OPT-OUT | not needed: registration is gated on a session in app code, and no analytics SDK is in the MVP |
| FCM AndroidConfigV2 / RemoteNotification callbacks | OPT-OUT | not needed yet: the backend sends v1 AndroidConfig messages (RESEARCH State of the Art) |
| Backend android collapse_key, ttl, tag, notification count, per-message priority beyond high | OPT-OUT | not needed: the app updates repeats in place itself (stable tag per entity) and the backend already sends priority high |
| iOS payload changes (category in data for iOS tokens) | OPT-OUT | explicitly out of scope: D-04 forbids changing iOS behaviour; logged as an iOS follow-up for the user (13-17) |
