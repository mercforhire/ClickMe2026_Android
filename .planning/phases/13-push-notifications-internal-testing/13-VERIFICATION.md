---
phase: 13-push-notifications-internal-testing
verified: 2026-10-28T18:00:00Z
status: human_needed
score: 3/5 must-haves verified
covered_files:
  - ".planning/phases/13-push-notifications-internal-testing/13-01-PLAN.md"
  - ".planning/phases/13-push-notifications-internal-testing/13-01-SUMMARY.md"
  - ".planning/phases/13-push-notifications-internal-testing/13-02-PLAN.md"
  - ".planning/phases/13-push-notifications-internal-testing/13-02-SUMMARY.md"
  - ".planning/phases/13-push-notifications-internal-testing/13-03-PLAN.md"
  - ".planning/phases/13-push-notifications-internal-testing/13-03-SUMMARY.md"
  - ".planning/phases/13-push-notifications-internal-testing/13-04-PLAN.md"
  - ".planning/phases/13-push-notifications-internal-testing/13-04-SUMMARY.md"
  - ".planning/phases/13-push-notifications-internal-testing/13-05-PLAN.md"
  - ".planning/phases/13-push-notifications-internal-testing/13-05-SUMMARY.md"
  - ".planning/phases/13-push-notifications-internal-testing/13-06-PLAN.md"
  - ".planning/phases/13-push-notifications-internal-testing/13-06-SUMMARY.md"
  - ".planning/phases/13-push-notifications-internal-testing/13-07-PLAN.md"
  - ".planning/phases/13-push-notifications-internal-testing/13-07-SUMMARY.md"
  - ".planning/phases/13-push-notifications-internal-testing/13-08-PLAN.md"
  - ".planning/phases/13-push-notifications-internal-testing/13-08-SUMMARY.md"
  - ".planning/phases/13-push-notifications-internal-testing/13-09-PLAN.md"
  - ".planning/phases/13-push-notifications-internal-testing/13-09-SUMMARY.md"
  - ".planning/phases/13-push-notifications-internal-testing/13-10-PLAN.md"
  - ".planning/phases/13-push-notifications-internal-testing/13-10-SUMMARY.md"
  - ".planning/phases/13-push-notifications-internal-testing/13-11-PLAN.md"
  - ".planning/phases/13-push-notifications-internal-testing/13-11-SUMMARY.md"
  - ".planning/phases/13-push-notifications-internal-testing/13-12-PLAN.md"
  - ".planning/phases/13-push-notifications-internal-testing/13-12-SUMMARY.md"
  - ".planning/phases/13-push-notifications-internal-testing/13-13-PLAN.md"
  - ".planning/phases/13-push-notifications-internal-testing/13-13-SUMMARY.md"
  - ".planning/phases/13-push-notifications-internal-testing/13-14-PLAN.md"
  - ".planning/phases/13-push-notifications-internal-testing/13-14-SUMMARY.md"
  - ".planning/phases/13-push-notifications-internal-testing/13-15-PLAN.md"
  - ".planning/phases/13-push-notifications-internal-testing/13-15-SUMMARY.md"
  - ".planning/phases/13-push-notifications-internal-testing/13-16-PLAN.md"
  - ".planning/phases/13-push-notifications-internal-testing/13-16-SUMMARY.md"
  - ".planning/phases/13-push-notifications-internal-testing/13-17-PLAN.md"
  - ".planning/phases/13-push-notifications-internal-testing/13-17-SUMMARY.md"
  - ".planning/phases/13-push-notifications-internal-testing/13-18-PLAN.md"
  - ".planning/phases/13-push-notifications-internal-testing/13-18-SUMMARY.md"
  - "app/build.gradle.kts"
  - "app/src/main/AndroidManifest.xml"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/MainActivity.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/managers/FcmPushTokenRegistrar.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/managers/PushRegistrationManager.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/clientmode/writereview/WriteReviewViewModel.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/notifications/NotificationsRepository.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/push/PushDestination.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/push/PushDestinationResolver.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/screens/push/PushEvent.kt"
  - "app/src/main/java/com/uptrendinvestments/clickmeapp/services/push/ClickMeMessagingService.kt"
covered_digest: "v3:sha256:bb50f3ed30405f24d0774ce6efb00aa34ed964dc8613b156d949c9a3e33bd1b6"
behavior_unverified: 1
overrides_applied: 0
behavior_unverified_items:
  - truth: "Tapping a push opens the right screen in the right tab's back stack from a killed app and while the app is running (SC-2, PUSH-04), specifically for a notification the OS displayed while the app was backgrounded"
    test: "On the Samsung SM-A075F with a real google-services.json and the backend D-04 deploy: (a) kill the app, send a booking, chat and session push, tap each from the shade; (b) background the app (do not kill it), send the same pushes, tap each; (c) repeat (b) with the app on a non-root screen such as an open chat thread."
    expected: "Each tap lands on the routed screen inside the correct tab (booking -> Bookings tab Booking Summary or Upcoming Booking; chat -> Chats tab thread; request -> Request Decision), Back unwinds through the tab root, and a client push in Expert mode switches mode first. On the warm tap the intent extras reach MainActivity (onNewIntent)."
    why_human: "In-app routing (PushEvent, PushRouter, resolver, Navigator.openRouted, pending slot, consumePushDestination, onNewIntent handler) is unit tested and wired. What no test can reach is whether Android/FCM delivers the launcher-intent extras to onNewIntent when a MAIN/LAUNCHER intent resumes an existing task. The 13-REVIEW explicitly leaves this unverified."
human_verification:
  - test: "REL-05 / SC-5 build with testers: complete release checklist steps 3 (keystore), 2 (Firebase + google-services.json), 4 (signed prodRelease), 5 (internal-track upload), 6 (testers + opt-in install)"
    expected: "A signed AAB is on the Play internal testing track, shows 'Available to internal testers', and the Play-installed build logs in and reaches Explore. D-22: one internal tester until Phase 16."
    why_human: "Needs the Play Console organization account, the upload keystore passwords, a Firebase console session and a physical device. No code can achieve this. Nothing in the repo or the checklist shows it done (all 10 checklist boxes are unchecked)."
  - test: "REL-04 / SC-5 drafts in Play Console: paste 13-PLAY-CONSOLE-DRAFTS.md into the store listing, Data safety form and the FOREGROUND_SERVICE_MICROPHONE declaration (checklist step 7)"
    expected: "App content shows the data safety form and foreground-service declaration saved as draft or complete; the store listing has no empty required fields."
    why_human: "The paste-ready text exists in the repo (13-PLAY-CONSOLE-DRAFTS.md, derived from the manifest and the REST contract), but REQ REL-04 says 'drafted in Play Console'. That is a Play Console action. The foreground-service video and graphics are user-supplied."
  - test: "PUSH-01 live: after deploying the backend and adding google-services.json, log in, check Supabase device_tokens, then log out"
    expected: "One row with platform = android after login/foreground return, a re-register after token rotation, row gone after logout (and no pushes for the previous user on that device)."
    why_human: "Needs real FCM, a Firebase config the repo deliberately lacks (app/google-services.json is gitignored and absent) and the live backend."
  - test: "PUSH-02 live: fresh install on Android 13+; deny and allow the notification permission; send a push per category while foregrounded and backgrounded"
    expected: "Prompt appears once after login (never again after logout/login); foreground pushes post on the five channels with the ClickMe icon and accent; background pushes land on the category channel after the backend 13-02 deploy (general before it); App info lists the five channels."
    why_human: "Runtime permission dialog, channel UI and FCM delivery are device behavior."
  - test: "PUSH-05 and REV-01 on device: notifications list from both bells, rows route like pushes; Write Review from session/ended and review/nudge pushes, Booking Summary and the Past card; read-only state and thanks snackbar; 200% font and TalkBack"
    expected: "List grouped Today / Yesterday / Earlier, a foreground push reloads it; payout/account rows are inert (PHASE-14 markers); a submitted review is read-only."
    why_human: "Visual grouping, accessibility and the end-to-end review flow against the live backend (STATE.md [Phase 13] DEFERRED (human) a to g)."
  - test: "Lock-screen privacy of system-displayed background pushes (REVIEW WR-02)"
    expected: "Decide whether a backgrounded push may show its full title/body on the lock screen. Foreground pushes use VISIBILITY_PRIVATE with a generic public version; FCM-displayed ones follow the channel default (public contents)."
    why_human: "Product/privacy decision. If privacy is wanted, set lockscreenVisibility on the channels (channel settings are permanent once shipped, so decide before the first internal upload) and/or in the backend payload."
gaps: []
deferred:
  - truth: "Payout, KYC and account pushes open the real Payouts / KYC / Security screens"
    addressed_in: "Phase 14"
    evidence: "ROADMAP Phase 14 hand-off: 'retarget the PHASE-14 markers in screens/push/PushDestination.kt and PushDestinationResolver.kt'. Phase 13 D-10 deliberately routes these to the notifications list."
  - truth: "A wider internal-tester group on a mix of devices and push from a killed app in the two-way matrix"
    addressed_in: "Phase 16"
    evidence: "REQUIREMENTS REL-05 note and STATE.md hand-off: 'Phase 16 adds a wider tester group (D-22)'."
---

# Phase 13: Push, Notifications & Internal Testing Verification Report

**Phase Goal:** Push notifications bring users to the right screen, reviews can be written, and a build is with real testers, so every MVP flow except payouts and settings works.
**Verified:** 2026-10-28T18:00:00Z
**Status:** human_needed
**Re-verification:** No, initial verification

## Bottom line

The code half of the goal is achieved. FCM registration, channels, the permission prompt, `PushRouter`, push-tap intake and per-tab back-stack routing, the notifications list and Write Review (all three entry points) exist, are substantive, wired and covered by passing tests. No code gap was found.

The "a build is with real testers" half of the goal is NOT achieved, and cannot be by code. There is no `app/google-services.json`, no keystore, no signed build, no Play upload, no tester, and no Play Console draft entered. All 10 steps in `13-RELEASE-CHECKLIST.md` are unchecked, the backend change is unpushed, and no device test has been run. Per project policy these are human items and not code gaps, but SC-5 (and with it the phase goal as written) is open until the user does them.

## Goal Achievement

### Observable Truths

| # | Truth (ROADMAP success criterion) | Status | Evidence |
|---|---|---|---|
| 1 | SC-1: FCM token registered with `platform: "android"` on every app start, unregistered on logout; Android 13+ permission prompt; defined channels | VERIFIED (code) | `PushRegistrationManager` combines `isLoggedIn` and `ProcessForeground` and calls `registrar.register()` (started in `ClickMeApplication.onCreate`). `FcmPushTokenRegistrar` posts `RegisterDeviceTokenRequest` (`PLATFORM_ANDROID`, `@Json device_id/app_version`, null omitted). `UserManager.logout()` calls `pushTokenRegistrar.unregister()` before `tokens.clear()` (DELETE `notifications/tokens/{device_id}` plus `deleteToken`). `ClickMeMessagingService.onNewToken` re-registers. `PushPermissionRequester` launches `POST_NOTIFICATIONS` once after login. Manifest declares the permission, the messaging service and default channel/icon/color meta-data. Tests: `FcmPushTokenRegistrarTest` 11, `PushRegistrationManagerTest` 5, `NotificationChannelsTest` 4, `PushManifestTest` 5, `PushPermissionRequesterTest` 6 pass. Real FCM delivery is a human item. |
| 2 | SC-2: Tapping a booking, chat or session push opens the right screen in the right tab's back stack, from a killed app and while running | PRESENT_BEHAVIOR_UNVERIFIED | Wired end to end: `MainActivity.onCreate` (guarded by `shouldReadLaunchPush`) and `onNewIntent` call `consumePushDestination()` -> `PushLaunchRequest.destination` -> `PushRoutingViewModel`/`PushRoutingHost` -> `PushDestinationResolver.resolve` -> `ShellRoute` -> `Navigator.openRouted` into the tab key chain (`ClientBookingsRoot`+`BookingSummaryKey`/`UpcomingBookingKey`, `ExpertBookingsRoot`+`ExpertBookingSummaryKey`, `DashboardRoot`+`IncomingRequestsKey`+`RequestDecisionKey`, `ChatThreadKey`). Mode switch, call guard, logged-out drop and 5 s lookup timeout are in code. Tests: `PushEventTest` 9, `PushRouterTest` 6, `PushDestinationResolverTest` 20, `PushRoutingViewModelTest` 11, `NavigatorOpenRoutedTest` 11, `MainActivityPushIntentTest` pass. Not exercised: OS delivery of extras for a system-displayed notification on a warm start (13-REVIEW "not verified here"), and every real-device tap. Routed to human verification, not scored. |
| 3 | SC-3: Notifications list groups items by date; tapping routes like a push | VERIFIED | `NotificationsRepository.recent()` calls `GET notifications` (page 1, limit 50, real `ClickMeApi` call); `NotificationsGrouping` buckets Today/Yesterday/Earlier; `NotificationsRoute`/`notificationsEntries` registered in both shells (`HomeClientScreen`, `HomeExpertScreen`); `NotificationBellButton` on Explore and the Expert Dashboard; rows build a `PushEvent` via `PushEvent.fromRow` and route through `PushRouter`/resolver. Tests: `NotificationsGroupingTest` 9, plus `NotificationDestinationTest`, `NotificationsViewModelTest`, `NotificationsRepositoryTest`, `NotificationsScreenTest` exist. Payout/account rows land on the list (PHASE-14 deferred). |
| 4 | SC-4: A user can write a review from `session/ended`, `review/nudge` pushes and Booking Summary | VERIFIED | `PushRouter` maps `session/ended` and `review/nudge` to `LeaveReview`; the resolver opens Booking Summary (user-confirmed D-07: the user taps Leave a Review there, iOS parity). `BookingSummaryScreen` shows `LeaveReviewButton` -> `viewModel.leaveReviewKey()` -> `navigator.navigate(WriteReviewKey)`; `PastBookingCard` pill and `MyBookingsScreen` also open it (13-18). `WriteReviewViewModel` loads context and submits via `ReviewsRepository` (`GET`/`POST bookings/:id/reviews`), hands off through `ReviewLaunchRequest`. `WriteReviewViewModelTest` 21 pass. Known edge: WR-04 (submit scoped to the entry ViewModel). |
| 5 | SC-5: A build is on the internal testing track with real testers; store listing, data safety and foreground-service declaration drafted in Play Console | UNCERTAIN (human, not achieved) | Repo artifacts exist: `13-RELEASE-CHECKLIST.md` (10 steps, all unchecked) and `13-PLAY-CONSOLE-DRAFTS.md` (listing, data safety, FGS text). Not done: `app/google-services.json` absent, no keystore/signed build evidenced, no upload, no tester, nothing entered in Play Console. STATE.md records these as `[Phase 13] OPEN (human, release)`. |

**Score:** 3/5 truths verified (1 present, behavior-unverified; 1 human-only and not yet done)

### Deferred Items

| # | Item | Addressed In | Evidence |
|---|------|-------------|----------|
| 1 | Payout/KYC/account pushes open real Payouts/KYC/Security screens (currently the notifications list; 2 `PHASE-14` markers remain) | Phase 14 | ROADMAP Phase 14 hand-off line; Phase 13 D-10 |
| 2 | Wider tester group; push from killed app in the two-way matrix | Phase 16 | REQUIREMENTS REL-05 note; STATE hand-offs |

### Required Artifacts

| Artifact | Expected | Status | Details |
|---|---|---|---|
| `screens/push/PushEvent.kt`, `PushDestination.kt` (`PushRouter`), `PushDestinationResolver.kt` | Parse with event_type fallback and UUID validation; category/sub_category -> destination; mode/tab resolution | VERIFIED | Substantive (124/132/193 lines), unit tested, used by `MainActivity`, notifications rows and `PushRoutingViewModel` |
| `managers/FcmPushTokenRegistrar.kt`, `PushRegistrationManager.kt`, `PushPreferences.kt` | Register/unregister, foreground policy | VERIFIED | Wired from `ClickMeApplication` and `UserManager.logout` |
| `services/push/ClickMeMessagingService.kt`, `PushMessageHandler.kt`, `PushNotificationFactory.kt`, `NotificationChannels.kt` | Foreground handling, five channels | VERIFIED | Service declared in manifest with `MESSAGING_EVENT`; channels created at app start |
| `screens/notifications/*` | List screen, grouping, repository, bells | VERIFIED | Registered in both shells; real API call |
| `screens/clientmode/writereview/*`, `reviews/ReviewsRepository.kt` | Write Review flow | VERIFIED | Registered via `writeReviewEntries` in `HomeClientScreen`; three entry points |
| Backend `src/services/push.js` (commits `6daf5c0`, `3ba94b9`, `92b4827`) | Android `channelId` and android-only `category`/`sub_category` in FCM data | VERIFIED in repo, NOT DEPLOYED | `channelIdFor`, `CHANNEL_BY_CATEGORY`, platform branch present; backend master is 4 commits ahead of origin/main |
| `app/google-services.json` | Firebase config for FCM | MISSING (by design, gitignored) | Required for any push at runtime; human checklist step 2 |
| `13-RELEASE-CHECKLIST.md`, `13-PLAY-CONSOLE-DRAFTS.md` | Human release steps and paste-ready text | VERIFIED (documents) | All checklist boxes unchecked |

### Key Link Verification

| From | To | Via | Status | Details |
|---|---|---|---|---|
| `MainActivity` | `PushLaunchRequest` | `consumePushDestination()` in `onCreate` and `onNewIntent` | WIRED | Restore/recents guarded (`shouldReadLaunchPush`); keys removed after read |
| `PushLaunchRequest` | resolver -> shell | `PushRoutingHost`/`PushRoutingViewModel` -> `ShellRoute` -> `Navigator.openRouted` | WIRED | Hosted in `ClickMeApp` |
| `UserManager.logout` | `FcmPushTokenRegistrar.unregister` | `pushTokenRegistrar.unregister()` before `tokens.clear()` | WIRED | |
| `ClickMeApplication` | `PushRegistrationManager.start` | `onCreate` | WIRED | |
| `ClickMeMessagingService` | `PushTokenRegistrar` / `PushMessageHandler` | Hilt injection | WIRED | |
| Booking Summary / Past card | `WriteReviewKey` | `onLeaveReview` -> `navigator.navigate` | WIRED | `HomeClientScreen` lines 117, 192-211 |
| Explore / Dashboard bell | `NotificationsKey` | `NotificationBellButton` | WIRED | |
| `ReviewLaunchRequest.onSubmitted` | Booking Summary / My Bookings reload | hand-off | WIRED | |

### Data-Flow Trace (Level 4)

| Artifact | Data | Source | Real data | Status |
|---|---|---|---|---|
| Notifications list | `recent()` | `ClickMeApi.getNotifications(page=1, limit=50)` -> `/notifications` | Yes (backend route returns persisted rows) | FLOWING |
| Write Review | review context / submit | `ReviewsRepository` -> `GET`/`POST bookings/:id/reviews` | Yes | FLOWING |
| Token registration | FCM token | `PushTokenSource` (Firebase) | Only with `google-services.json`; returns null without Firebase | FLOWING once configured |

### Behavioral Spot-Checks

Ran one Gradle invocation with named test classes only (`:app:testDevDebugUnitTest --tests ...`), `BUILD SUCCESSFUL`, all `failures=0 errors=0`:

| Class | Tests |
|---|---|
| PushRouterTest / PushEventTest | 6 / 9 |
| PushDestinationResolverTest | 20 |
| PushRoutingViewModelTest / NavigatorOpenRoutedTest | 11 / 11 |
| FcmPushTokenRegistrarTest / PushRegistrationManagerTest | 11 / 5 |
| PushManifestTest / NotificationChannelsTest | 5 / 4 |
| PushPermissionRequesterTest / PushMessageHandlerTest / PushNotificationFactoryTest | 6 / 5 / 7 |
| WriteReviewViewModelTest / NotificationsGroupingTest | 21 / 9 |

The orchestrator-reported full gate (3614 tests per flavor, ktlint, lint, R8 `assembleProdRelease`) was not re-run.

### Probe Execution

SKIPPED: no probe scripts declared by the phase plans.

### Requirements Coverage

All 8 phase requirement IDs appear in plan frontmatter (union across 13-01..13-18; 13-17 lists all eight) and in REQUIREMENTS.md. No orphans: the traceability table maps exactly PUSH-01..05, REV-01, REL-04, REL-05 to Phase 13.

| Requirement | Source Plans | Description | Status | Evidence |
|---|---|---|---|---|
| PUSH-01 | 13-03, 13-06, 13-17 | Register token on every start with `platform: android`, unregister on logout | SATISFIED in code; live check human | Registrar, manager, logout hook, tests |
| PUSH-02 | 13-02, 13-05, 13-10, 13-16, 13-17 | Notification channels; Android 13+ permission | SATISFIED in code; device check human; WR-02 privacy note | Channels, manifest, requester, backend channel map (undeployed) |
| PUSH-03 | 13-01, 13-02, 13-17 | `PushRouter` maps category/sub_category with event_type fallback | SATISFIED | `PushEvent` + `PushRouter` + tests |
| PUSH-04 | 13-01, 13-04, 13-07, 13-10, 13-11, 13-17 | Right screen in right tab back stack, cold and warm | SATISFIED in code; OS warm-start delivery human | See truth 2 |
| PUSH-05 | 13-03, 13-04, 13-08, 13-12, 13-17 | Notifications list by date, routes through destination map (first 50, no read state) | SATISFIED | See truth 3 |
| REV-01 | 13-03, 13-04, 13-09, 13-13, 13-15, 13-17, 13-18 | Write review from `session/ended`, `review/nudge`, Booking Summary | SATISFIED | See truth 4 |
| REL-04 | 13-14, 13-17 | Store listing, data safety, FGS declaration drafted in Play Console | NOT YET (human) | Text drafted in repo only; checklist step 7 unchecked |
| REL-05 | 13-05, 13-14, 13-17 | Build on internal testing track with real users | NOT YET (human) | Checklist steps 2-6 unchecked; no `google-services.json` |

Bookkeeping note: REQUIREMENTS.md still shows these eight as `[ ]` / "Pending". Tick PUSH-01..05 and REV-01 when the phase is accepted; keep REL-04 and REL-05 pending until the human steps are done.

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|---|---|---|---|---|
| `screens/push/PushDestination.kt`, `PushDestinationResolver.kt` | n/a | 2 `PHASE-14` markers | Info | Intentional, tracked hand-off (deferred) |
| `app/build.gradle.kts` | 12-18 | Missing `google-services.json` only logs a warning, so `bundleProdRelease` can ship a build with no push (WR-03) | Warning | Release safeguard is only the checklist (step 4 `aapt2 ... google_app_id` check). Consider failing release tasks without it |
| `managers/FcmPushTokenRegistrar.kt` | 46-85 | Mutex held across unbounded `getToken()`/`deleteToken()` awaits; logout can block behind a slow foreground registration (WR-01). Confirmed in code: only the DELETE is inside `withTimeoutOrNull` | Warning | Logout could appear to hang offline. Not a goal blocker |
| `services/push/NotificationChannels.kt` / `PushNotificationFactory.kt` | n/a | Lock-screen privacy only applied to app-posted (foreground) pushes (WR-02) | Warning | Backgrounded FCM-displayed pushes show full title/body on the lock screen. Fix before channels ship, because channel settings are permanent |
| `WriteReviewViewModel.kt` | 122-137 | Submit runs in `viewModelScope`; a push-driven stack change can cancel it after the server accepted the review (WR-04). Confirmed in code | Warning | Rare race; result lost, second attempt hits "already reviewed" |
| TBD/FIXME/XXX | n/a | None found by the 13-17 sweeps | n/a | n/a |

The four review warnings are open in `13-REVIEW-DISPOSITION.md` (all 10 findings "open"). None blocks a success criterion on its own; WR-02 is the one with a product-privacy consequence and should be decided before the first internal upload.

### Human Verification Required

See the `human_verification` and `behavior_unverified_items` blocks in the frontmatter. In priority order:

1. **Release steps 1-6 (REL-05):** backend push, Firebase + `google-services.json`, keystore, signed build, Play upload, tester opt-in. This is the only part of the phase goal that is flatly not achieved today.
2. **Play Console drafts (REL-04):** paste `13-PLAY-CONSOLE-DRAFTS.md` (checklist step 7).
3. **Device smoke (checklist step 10):** token row lifecycle, permission prompt, channels, and especially the system-displayed-notification tap on a warm start (13-REVIEW's open concern). If extras are not delivered to `onNewIntent` for a backgrounded app, PUSH-04 SC-2 fails in the common real-world path; the in-app fix would be small (e.g. an FCM `click_action` with an explicit intent-filter, or a `PendingIntent` posted by the app for all pushes).
4. **WR-02 decision:** lock-screen visibility of background pushes.

### Gaps Summary

No code gaps. The status is `human_needed` rather than `passed` because:
- SC-5 (internal-track build with testers, Play Console drafts) has not been done and is human-only, and
- SC-2's cold/warm tap from an OS-displayed notification is wired and unit tested, but the Android delivery behavior has never been exercised on a device.

Until the release checklist is done and the device smoke passes, the claim "every MVP flow except payouts and settings works with real testers" is not yet demonstrated, only enabled by the code.

---

_Verified: 2026-10-28T18:00:00Z_
_Verifier: Claude (gsd-verifier)_
