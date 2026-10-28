---
phase: 13-push-notifications-internal-testing
plan: 06
subsystem: push
tags: [fcm, token-registration, hilt, datastore, lifecycle]

requires:
  - phase: 13-03
    provides: ClickMeApi.registerDeviceToken / deleteDeviceToken, RegisterDeviceTokenRequest, FakeClickMeApi recorders
  - phase: 13-05
    provides: firebase-messaging and coroutines-play-services on the classpath, push manifest meta-data
  - phase: 13-01
    provides: PushModule
provides:
  - FcmPushTokenRegistrar bound to PushTokenRegistrar (register and unregister against the backend)
  - PushTokenSource seam (FirebasePushTokenSource) and FakePushTokenSource
  - PushPreferences stable device id in the clickme_push DataStore (@PushPrefs)
  - PushRegistrationManager (logged-in x foreground policy), started from ClickMeApplication
  - ClickMeMessagingService with onNewToken (13-10 adds onMessageReceived)
affects: [13-10, 13-11, 13-14]

actuals:
  tokens: 15000
  tasks: 3
  commits: 3

tech-stack:
  added: []
  patterns:
    - "Firebase behind a seam (PushTokenSource) so JVM tests never touch Firebase"
    - "Policy manager mirrors RealtimeConnectionManager: combine + distinctUntilChanged, AtomicBoolean start guard"
    - "Shared Mutex plus same-access-token guard to stop a register racing a logout"

key-files:
  created:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/managers/FcmPushTokenRegistrar.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/managers/PushTokenSource.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/managers/PushPreferences.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/managers/PushRegistrationManager.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/services/push/ClickMeMessagingService.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/managers/FcmPushTokenRegistrarTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/managers/PushRegistrationManagerTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/managers/PushPreferencesTest.kt
    - app/src/test/java/com/uptrendinvestments/clickmeapp/testing/FakePushTokenSource.kt
  modified:
    - app/src/main/java/com/uptrendinvestments/clickmeapp/managers/PushTokenRegistrar.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/di/PushModule.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/di/Qualifiers.kt
    - app/src/main/java/com/uptrendinvestments/clickmeapp/ClickMeApplication.kt
    - app/src/main/AndroidManifest.xml
    - app/src/test/java/com/uptrendinvestments/clickmeapp/services/push/PushManifestTest.kt

key-decisions:
  - "PushTokenRegistrar stays a no-arg open class (Hilt @Inject removed); the app binds FcmPushTokenRegistrar via @Binds so UserManager and its tests are untouched"
  - "Device id is a random UUID in its own clickme_push DataStore, not a SessionDataCleaner, so it survives logout and matches DELETE"
  - "unregister DELETEs with a 3 s cap, then deletes the FCM token; register is skipped for the access token that was unregistered"

requirements-completed: [PUSH-01]

coverage:
  - id: D1
    description: "A logged-in device posts its Android FCM token (platform android, app version, stable device id) to POST notifications/tokens"
    requirement: PUSH-01
    verification:
      - kind: unit
        ref: "FcmPushTokenRegistrarTest#registerPostsAndroidBody"
        status: pass
    human_judgment: false
  - id: D2
    description: "Registration happens on every foreground return while logged in, never while logged out"
    requirement: PUSH-01
    verification:
      - kind: unit
        ref: "PushRegistrationManagerTest (5 tests)"
        status: pass
    human_judgment: false
  - id: D3
    description: "Logout unregisters within 3 s, deletes the FCM token, and a late register cannot re-bind the old account"
    requirement: PUSH-01
    verification:
      - kind: unit
        ref: "FcmPushTokenRegistrarTest#unregisterGivesUpAfterThreeSeconds, registerAfterUnregisterInSameSessionIsSkipped"
        status: pass
    human_judgment: false
  - id: D4
    description: "Token rotation re-registers through a private FirebaseMessagingService declared in the manifest"
    requirement: PUSH-01
    verification:
      - kind: unit
        ref: "PushManifestTest#messagingServiceIsPrivateAndHandlesMessagingEvent"
        status: pass
    human_judgment: false
  - id: D5
    description: "A real device receives a registered token on the backend and loses it on logout"
    verification: []
    human_judgment: true
    rationale: "Needs a real google-services.json, a device and the backend; no Firebase app exists in this build"

plan_head_before: 6fa01f099efa3257b758f859ff491cd82e307b64
plan_head_after: 6fc330eeacb3419089d2de01d55ec251af9374f9
duration: 35min
completed: 2026-10-28
status: complete
---

# Phase 13 Plan 06: FCM token registration Summary

**FCM token registration for a logged-in, foregrounded device (POST notifications/tokens, platform android, stable UUID device id), re-registration on token rotation, and a 3 s-capped unregister plus FCM token deletion on logout, all behind a Firebase seam.**

## Performance

- **Duration:** about 35 min
- **Tasks:** 3 of 3
- **Commits:** 3 task commits (the SUMMARY commit follows and is not counted in `commits`)

## Accomplishments

- `PushTokenRegistrar` is unchanged for callers (no-arg open base) but gained `open suspend fun register(token: String? = null)`. `PushModule` binds `FcmPushTokenRegistrar`, so UserManagerTest, AppViewModelTest, RealtimeConnectionManagerTest, LoginViewModelTest and TopicEditorViewModelTest compile and pass unchanged.
- `FirebasePushTokenSource` is the only class touching Firebase for tokens and is a quiet no-op when `FirebaseApp.getApps` is empty (this build has no `google-services.json`).
- `PushRegistrationManager` registers on every false-to-true transition of `isLoggedIn && isForeground`; started right after `realtimeConnectionManager.start()`.
- `unregister` runs under a shared `Mutex`, records the access token, DELETEs within 3 s (virtual-time tested), then deletes the FCM token; `register` skips the unregistered access token until a new session token exists.
- `ClickMeMessagingService.onNewToken` re-registers the rotated token; manifest entry is private with the `MESSAGING_EVENT` filter.

## Verification

- Task 1 gate (7 test classes, ktlintCheck, `:app:assembleDevDebug` incl. Hilt graph): passed.
- Task 2 and Task 3 gates (`FcmPushTokenRegistrarTest` 11 tests, `PushManifestTest` 5, `PushRegistrationManagerTest` 5, `UserManagerTest`, ktlintCheck, `assembleDevDebug`): passed.
- Acceptance greps (timeout constant, onNewToken, manifest entries, zero `Log.`/`println` in the registrar, source and service): all as specified. `scripts/check-design-rules.sh`: "Design rules: OK".

## Deviations from Plan

None - plan executed as written. Small notes:
- `PushPreferences.deviceId()` reads first and only enters `dataStore.edit` when the key is missing, then re-checks inside the edit, so the common path does not write.
- Added `PushPreferencesTest.differentFilesGetDifferentIds` and `FcmPushTokenRegistrarTest.tokenNeverAppearsInToString` as extra cheap guards for the privacy prohibition.
- The per-plan commit ledger file under the git dir could not be written by the Write tool in the worktree sandbox, so `plan_head_before` is the worktree base commit given in the dispatch (`6fa01f0`), which equals the HEAD at start.

## Open Items (human / device)

- With a real `google-services.json` on a device: confirm one row for the account in the backend notification tokens table after login and after a foreground return, and that it disappears after logout (D5 above). Covered by the 13-14 release checklist.

## Known Stubs

None.

## Threat Flags

None. The new service is `exported="false"`; the register/DELETE calls were already in the threat model (T-13-14 to T-13-17).

## Self-Check: PASSED

- All created files present; commits `1f72f08`, `91080e6`, `6fc330e` are ancestors of HEAD.
