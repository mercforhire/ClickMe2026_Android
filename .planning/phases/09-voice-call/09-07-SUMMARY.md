---
phase: 09-voice-call
plan: 07
subsystem: call-foreground-service
tags: [foreground-service, notification, manifest, audio-focus]
requires:
  - phase: 09-02
    provides: call_notification_* strings
  - phase: 09-04
    provides: CallForegroundController port and CallNotificationInfo
provides:
  - CallService thin microphone foreground service (ACTION_START / ACTION_END)
  - CallForegroundBridge singleton implementing CallForegroundController
  - CallNotificationFactory isolated notification builder and channel (single CallStyle swap point)
  - Manifest call permissions, non-exported CallService, MainActivity singleTop
affects: [09-11 CallModule binding, CallCenter, MainActivity EXTRA_EXPAND_CALL handling]
tech-stack:
  added: []
  patterns: [singleton bridge observed by service via StateFlow, CompletableDeferred start handshake]
key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/services/call/CallService.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/services/call/CallForegroundBridge.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/services/call/CallNotificationFactory.kt
    - app/src/main/res/drawable/ic_stat_call.xml
    - app/src/test/java/com/uptrendinvestments/clickmeapp/services/call/CallNotificationFactoryTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/services/call/CallManifestTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/services/call/CallForegroundBridgeTest.kt
  modified:
    - app/src/main/AndroidManifest.xml
    - app/src/main/java/com/uptrendinvestments/clickmeapp/ClickMeApplication.kt
key-decisions:
  - "Service follows CallForegroundBridge.active instead of being bound; stop() just flips the flag"
  - "Notification End call only requests a hang-up via the bridge; CallCenter owns leave -> /end -> stop"
  - "Plain ongoing notification kept behind CallNotificationFactory so CallStyle can be swapped later"
requirements-completed: [CALL-03]
duration: ~25 min
completed: 2026-10-24
---

# Phase 9 Plan 07: Call Foreground Service Summary

Thin non-exported microphone foreground service plus a singleton bridge (the production `CallForegroundController`) and an isolated notification factory, with the call permissions, service declaration and `singleTop` on `MainActivity`.

## Tasks

| Task | Commit | Result |
|------|--------|--------|
| 1. Notification factory, channel, icon, manifest | 8ea21ce | Factory, `ic_stat_call.xml`, manifest, channel in `ClickMeApplication`, `CallNotificationFactoryTest` and `CallManifestTest` pass |
| 2. Bridge and CallService | 3762582 | Bridge, full `@AndroidEntryPoint` service, `CallForegroundBridgeTest` pass; `assembleDevDebug` and `ktlintCheck` pass |

## Verification

- `:app:testDevDebugUnitTest --tests 'com.uptrendinvestments.clickmeapp.services.call.*' :app:assembleDevDebug ktlintCheck`: BUILD SUCCESSFUL.
- `scripts/check-design-rules.sh`: OK.
- Acceptance greps on `CallService.kt`: no `onTaskRemoved`, a single `START_NOT_STICKY`, a single `USAGE_VOICE_COMMUNICATION`, no token/channelName/EXTRA_UID.
- Manifest greps: one `foregroundServiceType="microphone"`, one `stopWithTask="false"`, one `FOREGROUND_SERVICE_MICROPHONE`; `FLAG_IMMUTABLE` appears at least twice in the factory.

## Behavior notes

- `startForeground` (microphone type) runs first in `start`; a rejected start reports `onServiceStarted(false)` and tears down. If `active` is already false afterwards the service is an orphan and tears down (T-09-25).
- Audio focus is requested (voice communication / speech) after `startForeground`; focus loss is ignored (no UI, no teardown).
- One delayed re-post at the scheduled start flips the notification from "Starts at {time}" to the chronometer, guarded by the `POST_NOTIFICATIONS` check on API 33+.
- Notification: IMPORTANCE_LOW `ongoing_call`, CATEGORY_CALL, private with a name-free public version, content intent opens `MainActivity` with `EXTRA_EXPAND_CALL`, exactly one End call action, no Mute; all PendingIntents immutable.

## Deviations from Plan

None - plan executed as written. Minor: `CallService` was created as a skeleton in Task 1 (the plan explicitly allows this) and completed in Task 2; the bridge's hang-up backing field is named `hangUpFlow` because ktlint rejected the `_hangUp` backing-property name.

## Open Items / Notes for later plans

- `MainActivity` does not yet read `EXTRA_EXPAND_CALL` (not in this plan's files); the call-pill/expand handling belongs to a later UI plan.
- The Hilt binding `CallForegroundController <- CallForegroundBridge` is added in 09-11 (`CallModule`).
- CallStyle swap (Android 14+ dismissibility) remains an open item per RESEARCH Pitfall 3; only `CallNotificationFactory` needs to change.
- Not verified on a device: real FGS start, audio focus behavior, task-swipe survival (manual testing deferred).
- Robolectric deprecation warnings for `ShadowPendingIntent.isServiceIntent/isActivityIntent` in `CallNotificationFactoryTest` (warnings only).

## Known Stubs

None.

## Threat Flags

None beyond the plan's threat model.

## Self-Check: PASSED

- Created files exist (factory, bridge, service, icon, three tests, SUMMARY).
- Commits 8ea21ce and 3762582 exist on the branch.
