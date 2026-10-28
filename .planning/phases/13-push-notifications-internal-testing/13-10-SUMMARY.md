---
phase: 13-push-notifications-internal-testing
plan: 10
subsystem: push-notifications
tags: [fcm, foreground, notification, intent, main-activity]
status: complete
requires:
  - phase: 13-01
    provides: PushEvent, PushRouter, PushLaunchRequest
  - phase: 13-05
    provides: NotificationChannels, ic_stat_clickme, strings_push.xml
  - phase: 13-06
    provides: ClickMeMessagingService, PushModule
  - phase: 13-08
    provides: stripBoldMarkers
provides:
  - "Foreground push handling: arrivals tick, D-13 chat suppression, system notification post"
  - "Tap intake in MainActivity (cold start and onNewIntent) into PushLaunchRequest.destination"
affects: [13-11]
tech-stack:
  added: []
  patterns:
    - "Poster seam (PushNotificationPoster) so the handler is a plain JVM test"
    - "Tap intent carries only PushEvent.PUSH_KEYS as String extras; extras removed after consumption"
key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/services/push/PushMessageHandler.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/services/push/PushNotificationFactory.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/services/push/PushMessageHandlerTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/services/push/PushNotificationFactoryTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/MainActivityPushIntentTest.kt
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/services/push/ClickMeMessagingService.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/MainActivity.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/di/PushModule.kt
    - app/src/main/res/values/strings_push.xml
key-decisions:
  - "D-13: a message push for the open thread (ids compared ignoring case, both non-null) ticks arrivals but is not posted"
  - "Permission guard sits inline in SystemPushNotificationPoster.post, so lint MissingPermission passes with no suppression"
  - "Notification tag is push:<category>:<entity id> (thread id for message, else booking, payout, review id); no entity gives a random tag"
requirements-completed: [PUSH-02, PUSH-04]
actuals:
  tokens: 21000
  tasks: 3
  commits: 3
plan_head_before: 86fbe6fb30a99c7498c4d9e01449e270f329f86c
plan_head_after: 71f7a5ca8517d519087ad08bd8cde26ef007761e
completed: 2026-10-28
---

# Phase 13 Plan 10: Foreground push handling and tap intake Summary

**A foreground FCM push now ticks the list, posts a private system notification on the right channel (silent for the chat on screen), and every notification tap, cold or warm, hands one validated destination to the pending slot.**

## Accomplishments
- `ClickMeMessagingService.onMessageReceived` builds a `ForegroundPush` and calls `PushMessageHandler.onMessage`, which ticks `PushLaunchRequest.arrivals` first (D-12), drops a `message` push whose thread is `ChatSessionState.openThreadId` (D-13), and otherwise posts through `PushNotificationPoster`.
- `PushNotificationFactory`: FCM channel id honored only when one of the five contract ids, else `channelIdFor(category)`; title/body stripped of `**` markers; BigTextStyle; E8 fallbacks (missing title or body shows the other, neither shows "ClickMe"); CATEGORY_MESSAGE/EVENT/STATUS mapping; `VISIBILITY_PRIVATE` with a generic public version; stable tag for in-place updates; `FLAG_IMMUTABLE` content intent (explicit `MainActivity`, `NEW_TASK|CLEAR_TOP|SINGLE_TOP`, request code from the tag) carrying only `PushEvent.PUSH_KEYS`.
- `SystemPushNotificationPoster` posts nothing when notifications are disabled or, on API 33+, `POST_NOTIFICATIONS` is not granted. Bound in `PushModule`.
- `MainActivity`: `consumePushDestination()` (String extras only, UUID validation via `PushEvent`, keys removed after use, call extra untouched) and `shouldReadLaunchPush()` (skips restore and `LAUNCHED_FROM_HISTORY`); read in `onCreate` (guarded) and every `onNewIntent`.

## Verification
- `PushMessageHandlerTest` (5), `PushNotificationFactoryTest` (7), `MainActivityPushIntentTest` (7), `MainActivityIntentTest`, `CallNotificationFactoryTest`: all green.
- `ktlintCheck`, `:app:assembleDevDebug`, `lintDevDebug` (own invocation): exit 0. `scripts/check-design-rules.sh`: "Design rules: OK".
- No `Log.*`/`println` in `services/push` (privacy prohibition).

## Task Commits
1. Task 1 (tracer): `f2e1a58` - handler, poster, service, MainActivity intake, module binding
2. Task 2: `16efab8` - content, channel choice, privacy, stable tag, permission guard
3. Task 3: `71f7a5c` - D-13 suppression and launch-intent guard tests

## Deviations from Plan
None - plan executed as written. Task 1 shipped a minimal factory (random tag, category channel) which Task 2 replaced, as the plan describes. One small hardening beyond the written D-13 text: suppression requires both the push thread id and the open thread id to be non-null, so a thread-less chat push is never swallowed.

## Known Stubs
None.

## Threat Flags
None. The only new surface (exported MainActivity extras, notification PendingIntent, lock-screen content) is covered by T-13-25..28 and each is mitigated and tested.

## Open Items (manual, deferred)
- UI-SPEC E8 populated row on the Samsung test device: foreground posts on all five channels with the accent icon, heads-up on High channels and none on General, and tap routing from a cold start and while running. Needs a device and a real `google-services.json` (the build currently warns it is missing; see 13-14 release checklist).
- The "Thanks for your review" snackbar half of E8 long-text is in 13-15 and 13-18, not here.

## Self-Check: PASSED
All created files exist and commits f2e1a58, 16efab8, 71f7a5c are ancestors of HEAD.
