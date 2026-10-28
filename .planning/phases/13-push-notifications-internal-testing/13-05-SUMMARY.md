---
phase: 13-push-notifications-internal-testing
plan: 05
subsystem: push
tags: [firebase, fcm, gradle, manifest, notification-channels]
status: complete
requires: []
provides:
  - Firebase BoM 35.1.0 + firebase-messaging + coroutines-play-services on the app classpath
  - Guarded google-services plugin (applied only when app/google-services.json exists)
  - FCM manifest defaults (channel general, ic_stat_clickme icon, notification_accent color)
  - NotificationChannels (five contract channels, channelIdFor)
affects: [13-06, 13-10, 13-14, 13-16]
tech-stack:
  added: [firebase-bom 35.1.0, firebase-messaging, kotlinx-coroutines-play-services 1.11.0, google-services plugin 4.5.0]
  patterns: [conditional plugin apply on gitignored config file, idempotent channel creation at Application.onCreate]
key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/services/push/NotificationChannels.kt
    - app/src/main/res/drawable/ic_stat_clickme.xml
    - app/src/main/res/values/strings_push.xml
    - app/src/test/java/com/uptrendinvestments/clickmeapp/services/push/PushManifestTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/services/push/NotificationChannelsTest.kt
  modified:
    - gradle/libs.versions.toml
    - build.gradle.kts
    - app/build.gradle.kts
    - app/src/main/AndroidManifest.xml
    - app/src/main/res/values/colors.xml
    - app/src/main/java/com/uptrendinvestments/clickmeapp/ClickMeApplication.kt
key-decisions:
  - "Channel ids bookings/sessions/messages/account_payouts/general are permanent (backend contract, D-04)"
  - "ic_stat_clickme is a Material bell vector (launcher mark is a PNG; UI-SPEC Open Item 5 fallback)"
requirements-completed: [PUSH-02, REL-05]
duration: ~30 min (mostly a 15 min cold Gradle build)
completed: 2026-10-28
commits: 4
plan_head_before: 1c9c187606c03a3bdbc76c63f8635978b6b8849d
plan_head_after: a171209b71623e5ab759255ced95772a250fce9d
actuals:
  tokens: 11000
  tasks: 2
  commits: 4
---

# Phase 13 Plan 05: Firebase build wiring and notification channels Summary

Firebase Messaging is in the build behind a guarded google-services plugin (CI and worktrees build without the gitignored config), the manifest sets the FCM default channel, icon and accent, and `NotificationChannels` creates the five backend-contract channels at app start.

## Accomplishments

- Version catalog, root and app Gradle wiring: BoM 35.1.0, `firebase-messaging` (no Kotlin-extension artifact), `kotlinx-coroutines-play-services` 1.11.0, google-services 4.5.0 applied only if `app/google-services.json` exists; otherwise one `logger.warn` (T-13-12).
- Manifest meta-data for `default_notification_channel_id` (`general`), `default_notification_icon` (`ic_stat_clickme`) and `default_notification_color` (`notification_accent` = `#FF44F697`); `POST_NOTIFICATIONS` still declared exactly once.
- `NotificationChannels`: `BOOKINGS`, `SESSIONS`, `MESSAGES`, `ACCOUNT_PAYOUTS` at High, `GENERAL` at Default, names and descriptions from `strings_push.xml`, created idempotently via `createNotificationChannelsCompat` from `ClickMeApplication.onCreate`. `channelIdFor` mirrors the D-04 mapping (trimmed, case-insensitive, unknown or null to `general`). The `ongoing_call` channel is untouched (test asserts it stays Low).

## Task Commits

| Task | Commit | Type |
| ---- | ------ | ---- |
| 1 RED | c178260 | test: PushManifestTest |
| 1 GREEN | 15c39d7 | feat: Firebase wiring, manifest, icon, color, general channel |
| 2 RED | 3488c5c | test: NotificationChannelsTest |
| 2 GREEN | a171209 | feat: five channels and channelIdFor |

## Verification

- Task 1 verify (PushManifestTest, CallManifestTest, HomeShellsTest, ktlintCheck, assembleDevDebug without `google-services.json`): BUILD SUCCESSFUL.
- Task 2 verify (NotificationChannelsTest 4/4, PushManifestTest, CallNotificationFactoryTest, ktlintCheck): BUILD SUCCESSFUL.
- `scripts/check-design-rules.sh`: "Design rules: OK". `git ls-files app/google-services.json`: empty.
- Tracer gate (Task 1): verify passed end-to-end, expansion continued.

## Deviations from Plan

None in code. One plan-criterion inconsistency: Task 1 acceptance expects `grep -c 'default_notification_channel_id' AndroidManifest.xml` == 1, but the plan's own meta-data spec (name line plus `@string/default_notification_channel_id` value line) yields 2 matching lines. The manifest content is exactly as specified; the count is a grep-by-line artifact, not a defect.

The RED commits are compile-failure REDs (the tests reference types and resources that did not exist yet); they were not run in isolation to avoid a 15 minute cold Gradle run.

## Open Items (human)

- A real `app/google-services.json` is needed for push to function at runtime (Firebase console, 13-14 release checklist); this plan builds without it by design.
- On-device confirmation that a background push shows the bell icon with the green accent on the `general` channel is deferred to device testing.

## Known Stubs

None.

## Threat Flags

None.

## Self-Check: PASSED

Created files exist; commits c178260, 15c39d7, 3488c5c, a171209 are on HEAD.
