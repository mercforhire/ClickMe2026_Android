# Phase 13: Push, Notifications & Internal Testing - Research

**Researched:** 2026-10-28
**Domain:** Android FCM push (registration, channels, foreground post, tap routing into per-tab Navigation 3 back stacks), notifications list, Write Review, Firebase / Play Console release setup
**Confidence:** HIGH on the code seams, backend contract and Firebase wiring (read in this session, plus a throwaway build probe); MEDIUM on Play Console policy items (cited, partly assumed)

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

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

Permission & channels (PUSH-01, PUSH-02)
- **D-01: Ask for `POST_NOTIFICATIONS` right after a successful login or signup**, matching iOS `requestPushPermissionIfNeeded`. It does nothing if the user has already answered and does nothing below API 33. There's no pre-prompt rationale screen.
- **D-02: If the user denies, stay quiet.** Never prompt again. The FCM token is still registered. The notifications list shows a small "Notifications are off — Turn on" row at the top, which opens the system app-notification settings (`Settings.ACTION_APP_NOTIFICATION_SETTINGS`). The row is hidden when notifications are enabled.
- **D-03: Per-category notification channels:** Bookings, Sessions, Messages, Account & payouts, and a General fallback. General is the manifest `default_notification_channel_id`. The existing `CallService` channel stays separate and unchanged. Exact channel ids, names and importance are Claude's discretion (Messages and Sessions at high importance).
- **D-04: Change the backend so channels apply in the background too.** `backend-reference/src/services/push.js` sets `android.notification.channel_id` from the push `category`: booking→Bookings, session and review→Sessions, message→Messages, payout/account→Account & payouts, anything else→General. (Amended 2026-10-27 during /gsd-ui-phase: the user confirmed review→Sessions. Channel ids are fixed in 13-UI-SPEC.md: `bookings`, `sessions`, `messages`, `account_payouts`, `general`.) The `apns` block and iOS behavior must not change. The change gets its own backend task, deployed and checked with `/health` the same way as the Phase 11 G-11-6 fix. The Android channel ids are the contract and must match exactly. — **Reversibility:** costly — the ids become a contract between the backend payload and the Android channels, and renaming a channel later orphans the user's per-channel settings.

Routing (PUSH-03, PUSH-04)
- **D-05: Mode mismatch: switch mode, then route.** If the destination belongs to the other mode, run the existing Phase 4 mode switch (AppViewModel / UserManager), then push the destination into the right tab of the new mode. Examples are an expert-only request or booking while the user is in client mode, or a client booking while in expert mode. This deviates from iOS, which routes into whichever shell is showing. The researcher works out how to tell the role: payload fields first (check `backend-reference/src/services/events.js` for what `data` carries per event), otherwise a lightweight fetch. If the role can't be determined, route in the current mode.
- **D-06: A client's booking push lands by booking status.** This applies to `booking/*` (except `requests`), `session/reminder`, `session/no_show` and `review/received`. Fetch the booking first. An upcoming, pending or confirmed booking opens Upcoming Booking details (`UpcomingBookingKey`). A completed, cancelled, declined or past booking opens `BookingSummaryKey`. Experts get `ExpertBookingSummaryKey(bookingId, origin)` (iOS parity). If the status fetch fails, for example offline, route to `BookingSummaryKey`, which has its own error and Retry. No extra UI is shown while the fetch runs (added 2026-10-27 during /gsd-ui-phase, user-confirmed).
- **D-07: `session/ended` and `review/nudge` (leaveReview).** For a client, open `BookingSummaryKey` on the Bookings tab, and the user taps Leave a Review from there (iOS). For an expert, open `ExpertBookingSummaryKey` (iOS: experts don't leave reviews).
- **D-08: `booking/requests` for an expert.** If the payload has a request or booking id, open `RequestDecisionKey` directly, with `isExpired` and the expiry taken from the incoming-requests list (`GET /expert/booking-requests`). Without an id, open the incoming-requests list on the Dashboard stack. This deviates from iOS, which always opens the requests list. A client shell that gets this push falls back to the notifications list (iOS).
- **D-09: `message/new_message`.** Switch to the Chats tab of the current mode and post `ChatThreadKey(threadId)` (Phase 12 hand-off).
- **D-10: `payout/*` and `account/*` open the notifications list for now.** Treat them as unrouted until Phase 14 adds Payouts and Security. Leave a clear marker (e.g. `// PHASE-14`) in the destination mapping so Phase 14 can retarget them. Unknown categories, and pushes missing their id, also open the notifications list (iOS).
- **D-11: The notifications list opens on the current mode's Explore stack (client) or Dashboard stack (expert)**, the same stack the bell pushes onto.
- **D-12: Foreground arrival: post a system heads-up notification** on the push's channel, matching the iOS banner plus list behavior. Tapping it routes the same way as a background tap. Every foreground arrival also sends a refresh signal so an open notifications list silently reloads (iOS `newNotificationTick`).
- **D-13: Hide a chat push when that thread is on screen.** If `message/new_message` arrives in the foreground for the thread that's currently visible, don't post it. The realtime message already shows up. Other threads still notify. This deviates from iOS.
- **D-14: A tap while logged out or with an expired session is dropped.** Show Login only. Don't hold the destination for after login.
- **D-15: One pending destination at a time.** A newer tap overwrites an unconsumed one (iOS). It must work from a cold start (the launcher activity's intent extras from a notification the system displayed) and while the app is running (`onNewIntent`), and it must survive the splash → session-restore → home sequence before it's consumed.

Notifications list (PUSH-05)
- **D-16: iOS parity.**
  - Load `GET /notifications?page=1&limit=50` (first 50 only, no paging).
  - Group into Today / Yesterday / Earlier.
  - Pull-to-refresh, plus a silent reload on a push arrival (D-12).
  - An empty state.
  - No read or unread state (the backend has no mark-read endpoint).
  - Tapping a row routes through the same destination map, using ids from `meta_data` (the iOS key aliases: `threadId`/`thread_id`/`chatThreadId`/`chat_thread_id`, plus the booking-id equivalents).
- **D-17: Entry points.** The bell on the client Explore top bar and the bell on the expert Dashboard header (`DashboardHeader.kt` already has the `NotificationsNone` icon) each push the list onto their own tab stack.

Write Review (REV-01)
- **D-18: Entry points.** Restore Booking Summary's "Leave a Review" CTA and the Feedback block, which shows the submitted review (reverses the Phase 8 D-16 hide). Add a "Leave a review" affordance on the Past booking card for completed bookings that have no review yet. Pushes reach review through Booking Summary (D-07).
- **D-19: Presentation and flow.**
  - A full-screen destination pushed onto the same tab stack (port of `WriteReviewView`: brand, expert avatar, headline, star rating, text field, submit).
  - It loads `GET /bookings/:id/reviews/context`. If the user has already submitted, it is read-only with the prefilled rating and comment and an already-submitted banner (iOS).
  - On a successful submit it pops back to Booking Summary, which reloads to show the Feedback block, and shows a "Thanks for your review" snackbar.
  - Write Review is client-only.

Internal testing & Play Console (REL-04, REL-05)
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

### Deferred Ideas (OUT OF SCOPE)
- Notifications list paging beyond 50 and read/unread state. This needs a backend mark-read endpoint, so it's a future item.
- Holding a tapped push across login (D-14 chose to drop it). Revisit if testers complain.
- A wider internal tester group on a mix of devices. Do this by Phase 16 cross-platform QA (D-22).
- Real Payouts, KYC and Security destinations for `payout/*` and `account/*` pushes: Phase 14 (D-10 marker).
- In-app notification preferences (`/notifications/preferences`): not in the MVP scope.
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| PUSH-01 | FCM token registered via `POST /notifications/tokens` with `platform: "android"` on every app start; unregistered on logout | Critical Findings F-4, F-14; `PushRegistrationManager` + `FcmPushTokenRegistrar` pattern; device-id recommendation; `registerTokenSchema` is `.strict()` |
| PUSH-02 | Notifications post to defined channels; Android 13+ asks `POST_NOTIFICATIONS` | UI-SPEC channel table; backend `channelId` contract (D-04); `PushPermissionGate` pattern; Pitfalls 6, 9 |
| PUSH-03 | Ported `PushRouter` maps category/sub_category with legacy `event_type` fallback to a `PushDestination` | Critical Finding F-1 (FCM data has no category today); full event_type fallback table; iOS mapping table |
| PUSH-04 | Tap opens the right screen in the right tab's back stack, cold start and running | F-2 (launcher-intent extras, CLEAR_TOP verified); routing pipeline; `Navigator.openRouted`; role and status resolution (F-5, F-6); Pitfalls 1-5 |
| PUSH-05 | Notifications list grouped by date; tap routes like a push | Backend list contract (F-8), grouping/parsing pure functions, UI-SPEC Surface 1 |
| REV-01 | Write a review from `session/ended`, `review/nudge` pushes and Booking Summary | Review endpoints/DTOs (read), D-07 landing, Booking Summary/Past card hooks, hand-off slot pattern |
| REL-04 | Store listing, data safety, FGS declaration drafted in Play Console | Play policy facts (F-9), data-safety derivation, FGS declaration requirements |
| REL-05 | Build on internal testing track with real users | Guarded `google-services` plugin (F-3), human checklist, internal-track facts |
</phase_requirements>

## Project Constraints (from CLAUDE.md)

- iOS code at `ios-reference/` is the source of truth for feature behavior; deviate only for Android platform conventions. Backend contract is the same REST API; `backend-reference/docs/ios/` holds the authoritative specs.
- Architecture is fixed: Jetpack Compose + Material 3, MVVM with Compose State, Kotlin coroutines (no RxJava), Hilt, Retrofit + Moshi, `EncryptedSharedPreferences` for JWT, Firebase Messaging for push, Agora, Stripe PaymentSheet. minSdk 26, targetSdk 37, compileSdk 37.
- Platform swaps: `.sheet` to `ModalBottomSheet`, back chevron to `TopAppBar` back arrow, `.alert` to Material `AlertDialog`, `.task` to `LaunchedEffect(Unit)`.
- File organization mirrors the iOS tree under `app/src/main/java/com/uptrendinvestments/clickmeapp/...` (ViewModel next to Screen).
- Memory-file constraints that bind execution: JDK 25 via Android Studio JBR (`JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"`); worktree executors need `local.properties` copied and absolute iOS/backend paths (the symlinks do not resolve in worktrees); never run `gradlew --stop` in parallel executors; manual and device testing never blocks, log it as an open item in STATE.md.
- Design rules enforced in CI (`scripts/check-design-rules.sh`): no `Color(...)` literals outside `ui/theme/Color.kt`, no `*Brand` types, no Stripe secret keys. No `fontSize` in composables (UI-SPEC).

## Summary

The phase splits into five independent seams: (1) Firebase wiring and registration, (2) a small pure routing core (`PushEvent` parse, `PushRouter` mapping, resolver), (3) the hand-off and navigation insertion into the two per-mode shells, (4) the notifications list and Write Review UI, and (5) human-only release setup. Everything in 2 and 4 can be built and tested on the JVM with Robolectric and fakes; 1 is verified to build under AGP 9.4.1 (see F-3); 5 is a checklist.

**The single most important finding changes D-04.** Today the backend never puts `category` or `sub_category` into the FCM `data` map. `sendPushToUser` sends `data: safeData` where `safeData` is only what the caller passed (`booking_id`, `event_type`, `thread_id`...), and the cron pushes (session reminder, no-show, review nudge) send `data: { booking_id }` and nothing else (F-1). A ported `PushRouter` that keys on `category/sub_category` therefore cannot tell `session/reminder` from `review/nudge` from a cold-start intent. The D-04 backend task must also add `category` and `sub_category` to the `data` of Android tokens (gated by `platform === 'android'` so iOS payloads stay byte-identical, which honours D-04's "iOS behavior must not change"). Android should still carry the `event_type` fallback table because it is the only discriminator on booking pushes if the backend deploy lags. A side effect worth logging for the iOS owners: iOS has the same gap, so today iOS routes only chat and `booking_request` pushes; every other push parse-fails and does nothing on tap.

Everything else follows established seams: `OneShotSlot` hand-offs (Phase 11/12), `Navigator` per-tab stacks with origin-carrying keys (Pitfall 7), `CallNotificationFactory` for notification construction, `ChatSessionState.openThreadId` for the D-13 signal, and `PushTokenRegistrar` (already wired into `UserManager.logout`). The riskiest area (deep links into per-tab stacks) is mostly a duplicate-key and ordering problem, not an API problem: the new `Navigator.openRouted` must dedupe keys across the stacks in use, the pending destination must be resolved at the app root (it may need a mode switch) and only then handed to the matching shell, and the extras must be consumed exactly once.

**Primary recommendation:** Build a pure `PushEvent`/`PushRouter`/`PushDestinationResolver` core first (fully JVM-tested, including the iOS table, the event_type fallback and the role/status fallbacks), add the backend D-04 task (channel_id + android-only category/sub_category) as its own cross-repo plan with a human push, wire Firebase with a guarded `google-services` plugin (probe-verified), then attach it to `MainActivity`/`FirebaseMessagingService` and the shells through two `OneShotSlot`s (`PushDestination` at the root, resolved `ShellRoute` per mode).

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Push payload shape, `channel_id`, `category`/`sub_category` in data | API / Backend (`push.js`) | — | Only the server can set `android.notification.channel_id` for system-displayed (background) notifications; the client cannot restyle those |
| Background notification display and tap intent | OS / FCM SDK (system tray) | Client (manifest meta-data) | FCM displays `notification` messages itself when the app is not foreground; the tap launches the launcher activity with data extras |
| Foreground notification post, channels, permission | Client (Android app) | — | `onMessageReceived` only fires in foreground; channels and the runtime permission are client concerns |
| Token registration / unregistration | Client (app-scoped manager) | API / Backend (`device_tokens`) | Client owns the FCM token; backend enforces one token per account, last registration wins |
| Role of the push recipient (client vs expert side) | Client (static table + booking probe) | API / Backend | Not in the payload; the client infers it from category or probes the booking endpoints (F-6) |
| Booking status for D-06 routing | API / Backend (`GET /client/bookings/:id`) | Client (status classification) | Status is server data; classification into Upcoming vs Summary is a pure client function |
| Pending destination, mode switch, per-tab stack insertion | Client (root coordinator + shells) | — | Navigation state is local; the mode switch is `UserManager.requestModeSwitch` |
| Notifications history list | API / Backend (`GET /notifications`) | Client (grouping, parsing, rendering) | Server returns newest-first rows; Today/Yesterday/Earlier is a local-calendar grouping |
| Review context and submit | API / Backend (`/bookings/:id/reviews*`) | Client (form, read-only state) | Backend owns eligibility (completed only) and upsert semantics |
| Play Console listing / data safety / FGS declaration | Human (Play Console) | Docs in planning | No code; drafts derive from what the app collects |

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| `com.google.firebase:firebase-bom` | 35.1.0 | Aligns Firebase artifact versions | Official Firebase setup snippet pins 35.1.0 [CITED: firebase.google.com/docs/android/setup] |
| `com.google.firebase:firebase-messaging` (no `-ktx`) | 26.0.1 (from the BoM) | FCM client, `FirebaseMessagingService`, `RemoteMessage` | The BoM lists `firebase-messaging:26.0.1` [VERIFIED: dl.google.com/dl/android/maven2/com/google/firebase/firebase-bom/35.1.0/firebase-bom-35.1.0.pom]; the `-ktx` artifacts are no longer needed with the BoM 34+ line [ASSUMED] |
| `com.google.gms.google-services` Gradle plugin | 4.5.0 | Turns `google-services.json` into resources | Official snippet `id("com.google.gms.google-services") version "4.5.0" apply false` [CITED: firebase.google.com/docs/android/setup]. **Verified in a throwaway worktree on this repo's AGP 9.4.1 / Gradle 9.6 / configuration cache: `:app:processDevDebugGoogleServices` succeeded with a placeholder json** [VERIFIED: probe build, 2026-10-28] |
| `org.jetbrains.kotlinx:kotlinx-coroutines-play-services` | 1.11.0 (same as `kotlinx-coroutines-android`) | `Task<String>.await()` for `FirebaseMessaging.token` | JetBrains module, "Awaits for completion of the Task (cancellable)" [CITED: github.com/Kotlin/kotlinx.coroutines/tree/master/integration/kotlinx-coroutines-play-services]. The probe's dependency tree already resolves it transitively at 1.11.0, so adding it explicitly changes nothing [VERIFIED: probe `dependencies` output] |

### Supporting (already in the project, reuse)
| Library | Purpose | Notes |
|---------|---------|-------|
| `androidx.core` (`NotificationCompat`, `NotificationChannelCompat`, `NotificationManagerCompat`) | Channels and foreground posts | Same API surface as `CallNotificationFactory` [VERIFIED: services/call/CallNotificationFactory.kt:28-47] |
| Moshi codegen, Retrofit, `apiCall {}` | DTOs, endpoints | Project pattern; `GET /notifications` meta_data decodes as `Map<String, Any?>` |
| Material 3 `PullToRefreshBox`, `SnackbarHost` | List UI | Already used in 5 screens (ExploreScreen, MyBookingsScreen, ConversationsScreen...) [VERIFIED: grep of `PullToRefreshBox`] |
| Robolectric 4.17, Compose ui-test, MockWebServer3, coroutines-test | Tests | Already on the test classpath [VERIFIED: app/build.gradle.kts dependencies block] |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Guarded `google-services` plugin | Committed placeholder `app/google-services.json` | `google-services.json` is in `.gitignore` line 22, so a placeholder needs a force-add and risks shipping a fake config [VERIFIED: .gitignore:22]. The guarded plugin is probe-verified and needs nothing committed. **Use the guarded plugin.** |
| `FirebaseMessaging.deleteToken()` on logout | DELETE only | Defence in depth for shared devices when the DELETE fails offline; optional, see Pitfall 11 |
| FCM `AndroidConfigV2` / `RemoteNotification` callbacks | Legacy v1 `onMessageReceived` | The receive-messages page now documents a v2 model; it states the foreground/background split "applies only to legacy `AndroidConfig` (v1) messages" and the backend sends v1 [CITED: firebase.google.com/docs/cloud-messaging/android/receive-messages]. Out of scope; note only |

**Installation (verified shape):**
```toml
# gradle/libs.versions.toml
[versions]            # add:
firebaseBom = "35.1.0"
googleServices = "4.5.0"
[libraries]           # add:
firebase-bom = { group = "com.google.firebase", name = "firebase-bom", version.ref = "firebaseBom" }
firebase-messaging = { group = "com.google.firebase", name = "firebase-messaging" }
kotlinx-coroutines-play-services = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-play-services", version = "1.11.0" }
[plugins]             # add:
google-services = { id = "com.google.gms.google-services", version.ref = "googleServices" }
```
```kotlin
// build.gradle.kts (root): add  alias(libs.plugins.google.services) apply false
// app/build.gradle.kts, directly after the plugins { } block:
// Applies only when the real file exists, so CI and worktrees (no google-services.json, it is gitignored) still build.
if (file("google-services.json").exists()) apply(plugin = "com.google.gms.google-services")
// dependencies { }
implementation(platform(libs.firebase.bom))
implementation(libs.firebase.messaging)
implementation(libs.kotlinx.coroutines.play.services)
```
Probe results: with the file present `processDevDebugGoogleServices` ran and generated `build/generated/res/processDevDebugGoogleServices/values/values.xml`; with the file absent the build configured, `processDevDebugMainManifest` succeeded and `gradlew tasks --all` listed zero `GoogleServices` tasks (the plugin was not applied); the merged manifest gained `FirebaseMessagingService`, `FirebaseInstanceIdReceiver`, `FirebaseInitProvider` and kept the app's own `POST_NOTIFICATIONS` [VERIFIED: probe, 2026-10-28].

**Version verification:** `firebase-bom` latest 35.1.0 (lastUpdated 2026-10-21), `google-services` latest 4.5.0 (2026-06-16), `firebase-messaging` latest 26.0.1, `kotlinx-coroutines-play-services` latest 1.11.0 [VERIFIED: maven-metadata.xml from dl.google.com and repo1.maven.org, fetched 2026-10-28].

## Package Legitimacy Audit

The `package-legitimacy` seam supports only npm, pypi and crates (`Usage: gsd-tools package-legitimacy check --ecosystem <npm|pypi|crates>`, run 2026-10-28), so it cannot grade Maven coordinates. All four coordinates come from official first-party documentation (Firebase setup docs, the kotlinx.coroutines repo), exist in Google Maven / Maven Central metadata, and resolved in a real Gradle build against this project. No `[SUS]`/`[SLOP]` signals; no postinstall concept applies to Maven.

| Package | Registry | Age | Downloads | Source Repo | Verdict | Disposition |
|---------|----------|-----|-----------|-------------|---------|-------------|
| com.google.firebase:firebase-bom / firebase-messaging | Google Maven | years (first-party Google) | n/a | github.com/firebase/firebase-android-sdk | not graded by seam (Maven unsupported); official docs + resolved in probe | Approved |
| com.google.gms:google-services (plugin 4.5.0) | Google Maven | years (first-party Google) | n/a | github.com/google/play-services-plugins | not graded; official docs + ran in probe | Approved |
| org.jetbrains.kotlinx:kotlinx-coroutines-play-services | Maven Central | years (JetBrains) | n/a | github.com/Kotlin/kotlinx.coroutines | not graded; same version train as `kotlinx-coroutines-android` already used | Approved |

**Packages removed due to [SLOP] verdict:** none
**Packages flagged as suspicious [SUS]:** none. The planner needs no `checkpoint:human-verify` before dependency installs.

## Architecture Patterns

### Critical Findings (read first)

**F-1. FCM `data` has no `category` / `sub_category` (backend change needed beyond `channel_id`).**
`sendPushToUser(userId, { title, body, data, category, sub_category, ... })` uses `category`/`sub_category` only for the preference gate and the `notifications` row; the FCM message is
`notification: { title, body }, data: safeData, apns: { payload: { aps: { sound: 'default' } } }, android: { priority: 'high' }` and `safeData` is only the caller's `data` coerced to strings [VERIFIED: backend-reference/src/services/push.js:248-253]. What callers put in `data`:

| Source | `data` sent | Verified at |
|---|---|---|
| Booking lifecycle (`events.js`) | `{ booking_id: booking.id, event_type: eventType }` where `eventType` is `booking_request`, `BOOKING_CONFIRMED`, `BOOKING_DECLINED`, `BOOKING_CANCELLED` (plus `cancel_reason`), `BOOKING_RESCHEDULED`, `BOOKING_EXPIRED`, `BOOKING_COMPLETED` | events.js:262, 273, 284, 309, 336, 343, 354, 370 |
| Chat message | `{ thread_id: chatId, sender_id: userId, message_id, event_type: 'NEW_MESSAGE' }` | chats.controller.js:489-492 |
| Review received | `{ review_id, booking_id, event_type: 'REVIEW_SUBMITTED' }` | reviews.controller.js (push block) |
| Cron reminder / no-show / review nudge | `data:           { booking_id: booking.id }` only (no `event_type`) | cron/reminderNotification.js:91,101; cron/noShowNotification.js:98; cron/reviewNudgeNotification.js:110 |
| Refund | `{ booking_id, refund_amount, event_type: 'REFUND_ISSUED' }` | webhooks.controller.js (refund block) |
| Payout / KYC / dispute | `{ payout_id, event_type: 'PAYOUT_PAID' / 'PAYOUT_FAILED' }`, `{ event_type: 'KYC_ACTION_REQUIRED', kyc_status }`, `{ booking_id, event_type: 'DISPUTE_CREATED' }` | connectWebhooks.controller.js |
| Account | `{ event_type: 'DELETION_REQUESTED' }`, `{ event_type: 'PASSWORD_CHANGED' }` | account.controller.js |

Consequence: `session/reminder`, `session/no_show` and `review/nudge` arrive as `{ booking_id }` only and cannot be told apart on the device. D-07's nudge landing and D-06's reminder landing both reduce to "open the booking by status" for a client (a nudged booking is `completed`, so D-06's status routing already lands on `BookingSummaryKey`), but a clean mapping needs the category.
**Required backend change (extends the D-04 task):** in the Android branch of the fan-out add `category` and `sub_category` to `data`. iOS must stay unchanged, so select `platform` with the tokens (`.select('token, device_id, platform')`; the column is written by `registerToken`, `platform` is `z.enum(['ios','android'])` [VERIFIED: notification.schema.js:16-21]) and build the android message only for `platform === 'android'`. `category` and `sub_category` are not FCM reserved keys (reserved: `from` and anything starting with `google.`) [VERIFIED: firebase-admin messaging-api.d.ts:580-590].
Android keeps the `event_type` fallback anyway (resilience while the backend deploy lags): see "event_type fallback table" below.
**iOS follow-up to log in STATE (found while porting):** iOS `PushEvent` needs `category` or one of the `NEW_MESSAGE` / `NEW_BOOKING_REQUEST` / `BOOKING_REQUEST` / `REQUEST_CREATED` event types, so on iOS every other push logs "PushEvent parse FAILED" at tap and does nothing [VERIFIED: ios-reference/ClickMe2026/Model/Push/PushEvent.swift fallback switch]. Adding `category` for all platforms would fix iOS but change iOS behavior, so it is a user decision, not part of D-04.

**F-2. How a tap reaches the app (verified by decompiling `firebase-messaging-26.0.1`).**
- Foreground: every message goes to `onMessageReceived`. Background `notification` messages are displayed by the SDK and are not delivered there; "the data payload is delivered in the extras of the intent of your launcher Activity" [CITED: firebase.google.com/docs/cloud-messaging/android/receive-messages].
- The tap `PendingIntent` is the package launch intent plus `addFlags(67108864)` (= `FLAG_ACTIVITY_CLEAR_TOP`) plus `putExtras(paramsWithReservedKeysRemoved())`, with `gcm.n.click_action` honoured if the server sets one [VERIFIED: javap of `CommonNotificationBuilder.createContentIntent`/`createTargetIntent`, firebase-messaging-26.0.1]. So the data keys arrive as plain String extras (`booking_id`, `event_type`, `thread_id`, and `category`/`sub_category` after F-1).
- `MainActivity` is `android:launchMode="singleTop"` [VERIFIED: AndroidManifest.xml:28]. With `CLEAR_TOP` on a `singleTop` activity the live instance gets `onNewIntent`; a dead process gets `onCreate` with the extras. No `click_action` is needed.
- The same intent flags are used by our own foreground-post tap: `CLEAR_TOP | SINGLE_TOP` with `FLAG_IMMUTABLE` [VERIFIED: CallNotificationFactory.kt openIntent].

**F-3. Firebase wiring builds in CI without the real file (D-20 resolved).** See Standard Stack. At runtime without `google-services.json` the Firebase init provider cannot create a default app, so any `FirebaseMessaging.getInstance()` call would throw `IllegalStateException` [ASSUMED: standard Firebase behavior, not exercised here]. Guard every Firebase call with `FirebaseApp.getApps(context).isNotEmpty()` and make "no Firebase" a quiet no-op. A release built without the file compiles and silently has no push; the human checklist must verify the built APK contains `google_app_id` (`aapt2 dump resources app-prod-release.apk | grep google_app_id`).

**F-4. `PushTokenRegistrar` is constructed bare in many tests.** `open class PushTokenRegistrar @Inject constructor()` with `open suspend fun unregister() {}` [VERIFIED: managers/PushTokenRegistrar.kt:11-14]; `UserManager.logout` calls `runCatching { pushTokenRegistrar.unregister() }` before `tokens.clear()` [VERIFIED: UserManager.kt:195]; tests build `PushTokenRegistrar()` (AppViewModelTest 4 refs, UserManagerTest 3 refs including `RecordingRegistrar : PushTokenRegistrar()`). Do not add constructor parameters to this class. Keep it as the no-op base, drop `@Inject` from it, and add `class FcmPushTokenRegistrar @Inject constructor(...) : PushTokenRegistrar()` overriding `unregister()` and a new `open suspend fun register()`, bound with `@Provides fun pushTokenRegistrar(impl: FcmPushTokenRegistrar): PushTokenRegistrar` in a Hilt module. Existing tests keep compiling untouched.

**F-5. Mode switching has two traps.** (a) `AppViewModel.switchMode` is blocked by `callGuard.attempt(RestrictedAction.SwitchModes)` during a call and shows the guard dialog [VERIFIED: AppViewModel.kt switchMode]. (b) `UserManager.requestModeSwitch(Expert)` for an account without the expert role converts it to an expert and resumes into setup on every launch (Phase 4 D-17/D-23, logged in STATE). So a push-driven switch to Expert is allowed only when `userManager.me.value?.userRoles` contains `UserRole.Expert`; a switch is skipped while a call is active; both cases fall back to routing in the current mode (D-05's own fallback). Also `requestModeSwitch(Expert)` can answer `SignupSetup` (incomplete expert profile); then the pending route must be dropped, not held.

**F-6. The recipient's role is not in the payload; use a static table, then a probe.**
- Static by category (backend facts): `booking/requests` is sent to the expert only [events.js:256-262]; `booking/confirmations`, `declines`, `expirations` and refunds go to the client [events.js:267-284]; `review/nudge` goes to the client only (cron comment "Notify ONLY the client") [reviewNudgeNotification.js:106]; `review/received` goes to the reviewed expert (`reviewer_role === 'client'` only) [reviews.controller.js]; `payout/*` is `is_expert_only: true`.
- Ambiguous (either party): `booking/cancellations`, `booking/reschedules`, all `session/*`, `account/*`, `message/*`. For `message/*` do not switch (D-09). For the booking ones probe `GET /client/bookings/{id}`: it filters `.eq('client_id', clientId)` and answers 404 `NOT_FOUND` otherwise [VERIFIED: clientBooking.controller.js getDetail lines ~991-997]; a 200 means client (and returns `status`, which D-06 needs anyway); a 404 or 403 (`roleCheck('client')` answers 403 `FORBIDDEN` when the JWT lacks the client role [VERIFIED: middleware/roleCheck.js:25-36]) means expert side; anything else (network, 5xx) means unknown, so route in the current mode (D-06 fallback). When the user is currently in Expert mode and the target may be a client booking, probing the client endpoint first is still correct because a booking is never both (self-booking is rejected server-side, `rejectSelfBooking`).
- `request_id` in the incoming-requests list is the booking id (`request_id: row.id`) [VERIFIED: bookingRequests.controller.js:104], so `booking_id` from a `booking_request` push is the `RequestDecisionKey.requestId`.

**F-7. The two shells are separate composable subtrees with their own `NavigationState`.** `ClickMeApp` switches `HomeClientScreen`/`HomeExpertScreen` inside an `AnimatedContent` on `AppViewModel.route` [VERIFIED: ClickMeApp.kt `when (targetRoute)`]; each shell calls `rememberNavigationState(startTab, tabs)` itself [VERIFIED: HomeClientScreen.kt/HomeExpertScreen.kt]. After a mode switch the target shell is a fresh composition, so a destination cannot be pushed until that shell exists. The chat hand-off already solves "consume in the shell with a `LaunchedEffect`" (`pendingChatThread`, `onConsumeChatThread` params on both shells). Mirror it. The outgoing shell stays composed during the cross-fade, so every consumer must check that the pending route's mode equals its own mode before consuming.

**F-8. Backend facts for the list.** `GET /notifications` selects `id, user_id, category, sub_category, title, body, meta_data, expires_at, is_expert_only, created_at`, orders newest first, supports `limit` up to 100, and excludes `message` rows (`query.neq('category', 'message')`) [VERIFIED: notificationPrefs.controller.js:70-82; openapi/clickme-api.yaml `maximum: 100`]. So message pushes never appear in the list (the thread-id aliases in D-16 are harmless but unreachable), and list rows have a real `category`/`sub_category` (no fallback needed there). No backend string contains `**` markers today (grep of `body:` literals), so the bold parser is for forward-compatibility only. `meta_data` is the same `data` map as the push, so it carries `booking_id`, `event_type`, `thread_id` etc.

**F-9. Play Console facts (for REL-04/05).** The Data safety form is not required for an app that is only on the internal testing track [CITED: support.google.com/googleplay/android-developer/answer/10787469 and /answer/9845334]. Foreground service declarations live under Play Console, App content, are required per type when targeting Android 14+, and need a functionality description, the user impact if deferred or interrupted, a video link demonstrating how the user triggers the feature, and a use case (microphone: "Background Audio Access") [CITED: support.google.com/googleplay/android-developer/answer/13392821]. The 12-testers-for-14-days closed-test rule is stated for personal accounts created after 2023-11-13 [CITED: support.google.com/googleplay/android-developer/answer/14151465]; the page is silent on organization accounts, so exemption is [ASSUMED] (STATE already says to re-check in Console). Internal testing: up to 100 testers per app, tester lists under Test and release, Testing, Internal testing; the opt-in link appears only when the status is Published; the first link can take hours [CITED: support.google.com/googleplay/android-developer/answer/9845334]. The Payments policy requires Play billing for in-app features, services or content except physical goods/services, bills, peer-to-peer payments and a few others [CITED: support.google.com/googleplay/android-developer/answer/9858738]; whether paid live expert calls fall under an exception is a policy judgment this research cannot settle, see Open Question 3.

**F-10. UI-SPEC Open Item 5 is moot.** `scripts/check-design-rules.sh` greps only `--include='*.kt'` for `Color(` literals [VERIFIED: check-design-rules.sh:10]; `res/values/colors.xml` already holds a hex (`<color name="surface">#FF131313</color>`) [VERIFIED: colors.xml]. Adding `notification_accent` there needs no script change. Open Item 1 is confirmed: the real Dashboard bell goes in the `AuthTopAppBar` `actions` of `ExpertDashboardScreen.kt` next to `TopBarAvatarButton` (line ~188-191) [VERIFIED: grep]; the client one goes in `ExploreScreen.kt`'s `TopAppBar` `actions` (line ~151-153) [VERIFIED: grep].

### System Architecture Diagram

```
                         BACKEND (push.js, D-04 task)
   events / chats / cron / webhooks -> sendPushToUser(userId, {title, body, data, category, sub_category})
                                        |  per device token
                         platform=ios  |  platform=android
                    (payload UNCHANGED) |  data += {category, sub_category}
                                        |  android.notification.channel_id = channelFor(category)
                                        v
                                       FCM
                                        |
        +-------------------------------+--------------------------------+
        | app NOT foreground (any process state)                          | app foreground
        v                                                                 v
 System tray shows notification on channel_id             ClickMeMessagingService.onMessageReceived
 (fallback: manifest default channel "general")             |-> PushLaunchRequest.arrivals.tick()   (D-12 list refresh)
        | user taps                                         |-> skip if chat thread == ChatSessionState.openThreadId (D-13)
        v                                                   '-> PushNotificationFactory.post(channel, stable id, tap PendingIntent)
 launcher Intent (CLEAR_TOP) + data extras                              | user taps
        +----------------------------+----------------------------------+
                                     v
                MainActivity.onCreate (cold, savedInstanceState == null, not LAUNCHED_FROM_HISTORY)
                MainActivity.onNewIntent (running)
                       Intent.extras --parse+validate--> PushEvent --PushRouter--> PushDestination
                                     |  PushLaunchRequest.destination.post()   (one slot, newest wins, D-15)
                                     v
        PushRoutingHost (root composable, ClickMeApp, next to CallOverlayHost)  [PushRoutingViewModel]
          wait until route in {ClientHome, ExpertHome}  (Splash/restore holds it)
          route == Login -> drop (D-14) ;  logout -> slot cleared (SessionDataCleaner)
          resolve: role (static table -> booking probe), status (D-06), request expiry (D-08)
          needs other mode? -> (not in call, expert role present) appViewModel.switchMode(target) -> await route
          post ShellRoute(mode, tab, key chain) to ShellRouteRequest
                                     v
        HomeClientShell / HomeExpertShell  LaunchedEffect(pendingShellRoute)
          if pending.mode == this shell's mode -> consume -> Navigator.openRouted(tab, keys)
                                     v
        per-tab Navigation 3 back stack:  [TabRoot, ...destination]   (dedupe equal keys across stacks in use)
```

### Recommended Project Structure
```
app/src/main/java/com/uptrendinvestments/clickmeapp/
├── services/push/
│   ├── ClickMeMessagingService.kt     # @AndroidEntryPoint FirebaseMessagingService: onNewToken, onMessageReceived (thin)
│   ├── PushMessageHandler.kt          # plain class: tick, D-13 suppression, post (JVM-testable)
│   ├── PushNotificationFactory.kt     # builds the foreground notification (mirrors CallNotificationFactory)
│   └── NotificationChannels.kt        # ids, importance, ensureCreated(context), channelIdFor(category)
├── managers/
│   ├── PushTokenRegistrar.kt          # stays the no-op base (F-4); add `open suspend fun register()`
│   ├── FcmPushTokenRegistrar.kt       # real register/unregister, Firebase-guarded
│   ├── PushRegistrationManager.kt     # app-scoped: logged-in x foreground -> register (like RealtimeConnectionManager)
│   ├── PushDeviceId.kt                # stable id in a small non-session store
│   └── PushPermissionGate.kt          # asked-once flag + "needs prompt after login/signup" signal
├── screens/push/
│   ├── PushEvent.kt                   # parse Map<String,String> / Bundle, event_type fallback, UUID validation
│   ├── PushDestination.kt             # sealed interface (iOS enum) + PushRouter.destination(event)
│   ├── PushLaunchRequest.kt           # @Singleton: OneShotSlot<PushDestination>, arrivals tick; SessionDataCleaner
│   ├── ShellRouteRequest.kt           # OneShotSlot<ShellRoute> per resolved route
│   ├── PushDestinationResolver.kt     # suspend: role + status + expiry -> ShellRoute (all lookups behind small interfaces)
│   ├── PushRoutingViewModel.kt, PushRoutingHost.kt
│   └── PushPermissionRequester.kt     # composable, mirrors MicPermissionRequester
├── screens/notifications/             # NotificationsKey(origin), Screen, ViewModel, UiState, subviews/ (UI-SPEC)
├── screens/clientmode/writereview/    # WriteReviewKey, Screen, ViewModel, UiState, subviews/ (UI-SPEC)
├── screens/clientmode/reviews/        # ReviewsRepository, ReviewLaunchRequest (post-submit hand-off), reviewed-ids set
├── navigation/Navigator.kt            # + openRouted(...)
└── model/responses|requests           # NotificationItem(s), ReviewContextData, SubmitReviewData, RegisterDeviceTokenRequest
```

### Pattern 1: Pure routing core (port of `PushRouter`)
**What:** `PushEvent` (category, subCategory, bookingId, threadId, reviewId, payoutId, eventType) parsed from a `Map<String,String>` built from `RemoteMessage.data`, an `Intent.extras` Bundle, or a list row (`category` and `sub_category` columns plus `meta_data`). `PushRouter.destination(event): PushDestination?` ports the iOS table:

| category / sub_category | iOS `PushDestination` | Android note |
|---|---|---|
| `booking/requests` | incomingRequests | D-08: carries `requestId` (= `booking_id`) when present |
| `booking/*` other | bookingDetail(bookingId) | D-06 by-status for clients |
| `session/ended` | leaveReview(bookingId) | D-07 |
| `session/*` other (reminder, no_show) | bookingDetail | D-06 |
| `message/new_message` | chatThread(threadId) | D-09 |
| `review/nudge` | leaveReview | D-07 |
| `review/*` other (received) | bookingDetail | D-06 |
| `payout/kyc_action` | kycOnboarding | D-10: resolves to NotificationsList, mark `// PHASE-14` |
| `payout/*` other | payouts | D-10 same |
| `account/*` | accountSecurity | D-10 same |
| anything else, or missing id | null, then notificationsList | D-10 / iOS |
[VERIFIED: ios-reference/ClickMe2026/Managers/PushRouter.swift `destination(for:)`; list variant NotificationsListViewModel.swift `destination(for:)` is the same table over `meta_data`]

**event_type fallback table (Android superset of iOS; used only when `category` is absent):** compare `eventType.uppercase()` (iOS uppercases): `NEW_MESSAGE` to `message/new_message`; `NEW_BOOKING_REQUEST`, `BOOKING_REQUEST`, `REQUEST_CREATED` to `booking/requests`; plus the values this backend actually emits [VERIFIED: events.js, chats.controller.js, reviews.controller.js, webhooks/connectWebhooks/account controllers]: `BOOKING_CONFIRMED` to `booking/confirmations`, `BOOKING_DECLINED` to `booking/declines`, `BOOKING_CANCELLED` to `booking/cancellations`, `BOOKING_RESCHEDULED` to `booking/reschedules`, `BOOKING_EXPIRED` to `booking/expirations`, `BOOKING_COMPLETED` to `session/ended`, `REVIEW_SUBMITTED` to `review/received`, `REFUND_ISSUED` to `booking/refunds`, `PAYOUT_PAID`/`PAYOUT_FAILED` to `payout/paid`/`payout/failed`, `KYC_ACTION_REQUIRED` to `payout/kyc_action`, `DISPUTE_CREATED` to `payout/dispute`, `PASSWORD_CHANGED` to `account/password_changed`, `DELETION_REQUESTED` to `account/deletion_requested`. A payload with neither `category` nor a known `event_type` but a valid `booking_id` is treated as `booking/*` (covers the cron pushes if the backend deploy lags); with neither, the event is unparseable and the tap opens the notifications list (iOS: no event means nothing; D-10 says unknown opens the list, so open the list).

### Pattern 2: Two-stage hand-off (root resolves, shell consumes)
**What:** Stage 1 `PushLaunchRequest.destination: OneShotSlot<PushDestination>` (D-15). Stage 2 `ShellRouteRequest.route: OneShotSlot<ShellRoute>` where `ShellRoute(mode: AppMode, tab: NavKey, keys: List<NavKey>)`. `OneShotSlot` is the established memory-only consume-on-read holder (`post` replaces an unconsumed value) [VERIFIED: screens/expertmode/ExpertLaunchRequest.kt:12-26]. Make `PushLaunchRequest` a `SessionDataCleaner` (as `ChatSessionState` is) so a logout clears a pending destination [VERIFIED: di/ChatModule.kt:25 binding pattern].
**Why two stages:** the resolver needs suspend lookups, `UserManager`, `CallGuard` and `AppViewModel.switchMode`, none of which a stateless shell has; and the shell only exists after the mode switch.
**Root host:** `PushRoutingHost(route = appViewModel.route, onSwitchMode = appViewModel::switchMode)` in `ClickMeApp`, a `LaunchedEffect` that collects `combine(route, destination.pending)`. Do not extend `AppViewModel`'s constructor (three test constructions in `AppViewModelTest`); use a separate `@HiltViewModel PushRoutingViewModel`.
**Gating rules:** act only when `route in {ClientHome, ExpertHome}` and logged in; `route == Login` (or logged out) clears the slot (D-14); `SignupSetup` also drops it (assumption A3). After `switchMode(target)`, await `route == target.homeRoute()` with a timeout (use ~5 s, the splash cap order of magnitude); on timeout or `SignupSetup` drop.

### Pattern 3: `Navigator.openRouted` (per-tab insertion with dedupe)
`Navigator.navigateInTab` only appends [VERIFIED: Navigator.kt `navigateInTab`], and `openChatThread` resets the target stack first [VERIFIED: Navigator.kt:143-155]. Add one method with these rules:
1. If an equal key already exists in a stack in use (`state.stacksInUse` = start stack plus the selected one), do not push a duplicate: truncate that stack to that key and select its tab. Equal keys in two stacks in use produce duplicate content keys (Pitfall 7, `StackOrigin.kt` KDoc) [VERIFIED: StackOrigin.kt:1-14].
2. Else, if the target tab is not the selected tab, pop the target stack to its root, then push the keys in order, then select the tab (TabSwitch transition). If the target tab is the selected tab, push on top (so a tap from the Notifications list on the same tab keeps Back to the list).
3. Always set `lastTransition`. Back from the destination then lands on the tab root, and Back from the root selects the start tab (existing `goBack` behavior).
Key chains: expert request = `[IncomingRequestsKey, RequestDecisionKey(...)]` on the Dashboard tab so Back lands on the list and Accept's `replaceTop` still works; client by-status = `UpcomingBookingKey(id)` or `BookingSummaryKey(id)` on the Bookings tab (`ClientBookingsRoot`); expert booking = `ExpertBookingSummaryKey(id, ExpertBookingOrigin.Bookings)` on `ExpertBookingsRoot`; chat = existing `Navigator.openChatThread(chatsRootFor(mode), ChatThreadKey(threadId))`; list = `NotificationsKey(origin)` on `ExploreRoot` or `DashboardRoot` (D-11).

### Pattern 4: Cold-start / warm intent handling in `MainActivity`
- Parse in `onCreate` only when `savedInstanceState == null` and the intent flags do not include `FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY`; parse in every `onNewIntent`. After posting, remove the push extras from the intent (as `consumeExpandCallExtra` does) [VERIFIED: MainActivity.kt consumeExpandCallExtra]. Process death re-creates the activity with the original intent, so `savedInstanceState != null` must skip parsing or an old push re-routes on restore.
- Validate ids as UUIDs before posting (the activity is `exported="true"`, so any app can fire an intent with arbitrary extras). The destinations are read-only screens that re-fetch with the user's own token, so the exposure is low; UUID validation keeps junk out of the URL path.
- Tests: extend the `MainActivityIntentTest` style (pure `Intent?.toPushEvent()` function, Robolectric) [VERIFIED: MainActivityIntentTest.kt].

### Pattern 5: Token registration (PUSH-01)
- `PushRegistrationManager` (app-scoped, started from `ClickMeApplication.onCreate` like `realtimeConnectionManager.start()`): `combine(userManager.isLoggedIn, foreground.isForeground)` and, on each transition into `loggedIn && foreground`, call `registrar.register()`. That covers app start, login/signup, restore and "every foreground" (iOS `applicationDidBecomeActive`) with the existing `ProcessForeground` seam [VERIFIED: realtime/ProcessForeground.kt, RealtimeConnectionManager.start()].
- `FcmPushTokenRegistrar.register()`: no-op unless Firebase is initialised and `tokens.hasSession()`; `FirebaseMessaging.getInstance().token.await()`; `POST notifications/tokens` with `RegisterDeviceTokenRequest(deviceId, token, platform = "android", appVersion = BuildConfig.VERSION_NAME)`. The request class must be `@JsonClass` with exactly `device_id`, `token`, `platform`, `app_version`; the schema is `.strict()` and `app_version` is `optional()` (not nullable), so omit the field instead of sending null (Moshi omits nulls by default).
- `onNewToken(token)` in the service: register with that token if a session exists (works from a background-started process; the authenticated Retrofit stack handles refresh).
- `unregister()`: `DELETE notifications/tokens/{device_id}` (best effort, short timeout, called before the tokens are cleared per the existing order); optionally also `FirebaseMessaging.deleteToken()`.
- Device id: `registerToken` deletes all of the caller's rows and inserts one fresh row [VERIFIED: notifications.controller.js registerToken steps 1-3], so only `DELETE` needs the id to match. Persist a random UUID in a small SharedPreferences/DataStore file that logout does NOT clear (not `clickme_session`, whose `SessionStore.clear()` wipes everything [VERIFIED: SessionStore.kt clear()]). `Settings.Secure.ANDROID_ID` is not needed and trips the HardwareIds lint [ASSUMED].

### Pattern 6: Foreground handling (`onMessageReceived`)
`@AndroidEntryPoint` services work in this project (`CallService`) [VERIFIED: CallService.kt:41-42]. Keep the service thin: map `RemoteMessage` to `(data, title, body, channelId)` and call `PushMessageHandler`. Handler steps: emit the arrivals tick always (D-12); if `message/new_message` and `thread_id` equals `ChatSessionState.openThreadId` ignoring case, return (D-13) [VERIFIED: ChatSessionState.kt:35 `openThreadId`; `onThreadHidden` already compares `ignoreCase = true`]; if notifications are disabled (`NotificationManagerCompat.areNotificationsEnabled()`), return; ensure channels exist; build and `notify` using the channel from `RemoteMessage.Notification.channelId` falling back to `NotificationChannels.channelIdFor(category)` then `general`. Notification id: stable hash of `(category, entityId)` so a repeat updates in place (UI-SPEC); PendingIntent request code = the same id with `FLAG_UPDATE_CURRENT | FLAG_IMMUTABLE` so extras are not shared between notifications.

### Pattern 7: Permission request (D-01)
`PushPermissionGate` (singleton): `needsPrompt` set true by `UserManager.startSession` (login and signup both go through it [VERIFIED: UserManager.kt login()/signup() call startSession]) only when API >= 33, permission not granted and the persisted `asked` flag is false. A composable `PushPermissionRequester` hosted next to `CallOverlayHost` launches `ActivityResultContracts.RequestPermission` when `needsPrompt && route is not Login/Splash && !callActive`, sets `asked = true` before launching, and clears `needsPrompt` on the result. Persist `asked` outside the session-cleared store (otherwise every login re-prompts, defeating "never prompt again"). Phase 9's `MicPermissionRequester` can also prompt for notifications at the first call [VERIFIED: MicPermissionRequester.kt permissionsToRequest], so also set `asked` when the permission is observed granted; a user who denies there may be asked once more here (Android allows a second prompt; accepted edge).

### Pattern 8: Post-submit hand-off (Write Review)
Follow the Phase 8 launch-argument pattern the UI-SPEC names: a `@Singleton ReviewLaunchRequest` with `OneShotSlot<ReviewSubmitted(bookingId)>`; Booking Summary's and My Bookings' ViewModels consume it on resume to reload quietly and raise the "Thanks for your review" snackbar. An in-memory `reviewedBookingIds` set (a `SessionDataCleaner`) lets Past cards hide "Leave a review" for the session. Give `WriteReviewKey` an `origin: StackOrigin = Default` passed through from `BookingSummaryKey.origin` (Chats-origin summaries then push a distinct review key), consistent with Phase 12.

### Anti-Patterns to Avoid
- **Routing from `onMessageReceived` or the service.** The service has no navigation; it only ticks and posts. Routing happens from the tap intent.
- **Pushing into a shell that does not exist yet.** Consume in the shell via `LaunchedEffect`, never imperatively from the root.
- **Switching to Expert without checking `me.userRoles`.** Triggers the client-to-expert conversion trap (F-5).
- **Holding the destination across login** (D-14 says drop).
- **Adding constructor params to `PushTokenRegistrar`** (breaks ~7 test constructions, F-4).
- **Parsing the launch intent on every `onCreate`.** Re-routes on process restore and recents (Pattern 4).
- **A placeholder `google-services.json` committed with `git add -f`.** Use the guarded plugin.
- **Hard-coding channel ids in two unrelated places.** One `NotificationChannels` object on Android; one `CHANNEL_BY_CATEGORY` map in backend `push.js`; one jest test and one JVM test assert the same five literals.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| FCM token fetch / rotation callback | Custom Play Services bridge | `FirebaseMessaging.getInstance().token` + `FirebaseMessagingService.onNewToken` | Official API; `Task.await()` from `kotlinx-coroutines-play-services` |
| System-tray display and tap intent for background pushes | A data-only message plus a custom notification builder | Keep the `notification` message (D-04) | The SDK already builds the tap `PendingIntent` with the data extras and CLEAR_TOP (F-2) |
| Notification channel creation and compat | `NotificationChannel` with `Build.VERSION` checks | `NotificationChannelCompat` / `NotificationManagerCompat` (already used by `CallNotificationFactory`) | Idempotent, API-safe |
| One-shot cross-screen hand-off | New event-bus or result APIs | `OneShotSlot<T>` in a `@Singleton` | Established project pattern (ChatLaunchRequest, ExpertLaunchRequest) |
| Per-tab back stack manipulation | Ad-hoc `backStacks[...]` edits in screens | One `Navigator.openRouted` | Centralises the Pitfall 7 dedupe |
| Runtime permission request | Manual `requestPermissions` / `onRequestPermissionsResult` | `rememberLauncherForActivityResult(RequestPermission())` | Matches `MicPermissionRequester`; survives recomposition |
| Date bucketing, relative time | Hand-rolled epoch math | `java.time` (`LocalDate`, `ZoneId`, `Clock` from `ClockModule`) | Local-calendar day semantics, testable with `MutableClock` (exists in `testing/`) |
| System notification-settings screen | Custom settings UI | `Settings.ACTION_APP_NOTIFICATION_SETTINGS` + `EXTRA_APP_PACKAGE`, fallback `ACTION_APPLICATION_DETAILS_SETTINGS` | UI-SPEC E3 error row |
| Firebase option wiring | Manual `FirebaseOptions` from BuildConfig | google-services plugin + `google-services.json` | Standard; verified building |

**Key insight:** every hard part of this phase is already a seam in the repo. The new code is mapping tables, a thin service, one Navigator method, and screens.

## Runtime State Inventory

Not a rename/refactor phase. One adjacent item: the only runtime state that survives uninstall-free upgrades is the new persisted device id and the notification-permission `asked` flag (both new in this phase, nothing to migrate), and the five notification channel ids, which become permanent user-visible state once shipped (renaming orphans per-channel user settings, D-04 reversibility).

## Common Pitfalls

### Pitfall 1: Parsing the launch intent on every `onCreate`
**What goes wrong:** rotation, process-death restore or "open from recents" re-delivers the original notification intent and re-routes to a stale booking.
**How to avoid:** `savedInstanceState == null` guard, ignore `FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY`, and remove the extras after posting. **Warning signs:** the app jumps to an old booking after returning from recents.

### Pitfall 2: Duplicate keys across stacks in use
**What goes wrong:** pushing `UpcomingBookingKey(b)` onto Bookings while Explore (the start stack) already holds the same key (Today's Sessions opens it there) produces equal content keys in the flattened list and crashes or mis-restores state.
**How to avoid:** the dedupe-first rule in `Navigator.openRouted` (Pattern 3). Add a Robolectric navigation test with the same key in two stacks.

### Pitfall 3: Consuming the route in the wrong shell
**What goes wrong:** during the Client/Expert cross-fade both shells are composed; the outgoing one consumes the slot and the push is lost.
**How to avoid:** consumers check `route.mode == myMode` before `consume()`; only the matching shell consumes.

### Pitfall 4: A push-driven mode switch hits the call guard or the expert-conversion trap
See F-5. **How to avoid:** skip the switch during a call (route in the current mode, or open the current mode's notifications list), never switch to Expert without the role, drop on `SignupSetup`.

### Pitfall 5: The slot is consumed before Splash/restore finishes, or never consumed
**What goes wrong:** consuming at `onCreate` time with `route == Splash` loses it; consuming too late (after Login) violates D-14.
**How to avoid:** the host acts only on a home route and clears on Login. The OS splash is held by `destinationResolved`, and the 5 s cap path keeps `route` on a home route using cached state, so the host still runs; lookups then fail offline and take the D-06 fallbacks.

### Pitfall 6: `areNotificationsEnabled()` vs the runtime permission
`areNotificationsEnabled()` is false when the user denied, when they turned notifications off in settings, or (API 33+) when the permission is not yet granted. Use it for the list hint row and for the foreground-post guard; use the persisted `asked` flag, not `shouldShowRequestPermissionRationale`, to decide whether to prompt (the latter is false both before the first ask and after a permanent denial, the same trap Phase 9 hit [VERIFIED: MicPermissionRequester.kt KDoc "RESEARCH Pitfall 12"]).

### Pitfall 7: Background notifications ignore `channel_id` until the backend change ships
With today's backend, background pushes land on the manifest default channel (`general`). Channel behavior per category only exists after D-04 is deployed; the app is correct either way. FCM falls back to the manifest default when the sent channel does not exist [VERIFIED: firebase-admin messaging-api.d.ts `channelId` docs lines ~453-457]. Create the `general` channel at `Application.onCreate` so the manifest default always exists.

### Pitfall 8: `data` values must be strings and the schema is strict
Backend `safeData` coerces values to strings; Android intent extras are strings. Never read `Boolean`/`Int` extras. `registerTokenSchema` and the `.strict()` rule mean any extra JSON field in the register body is a 422 [VERIFIED: notification.schema.js:16-21].

### Pitfall 9: `POST_NOTIFICATIONS` is already in the manifest
The app declares it for `CallService` [VERIFIED: AndroidManifest.xml:10] and the FCM SDK also adds it. Do not add a second `<uses-permission>` entry or a `tools:node="remove"`.

### Pitfall 10: Release builds silently lose push without the json
See F-3. A `bundleProdRelease` made on a machine without `app/google-services.json` builds green. Add a build-time `logger.warn` for release configurations when the file is missing and put the `aapt2 ... google_app_id` check in the human checklist. Do not fail the build: phase gates and worktree executors run `assembleProdRelease` without the (gitignored) file.

### Pitfall 11: Logout while offline leaves the token bound
`UserManager.logout` swallows an unregister failure. The next login's POST wipes the account's rows and evicts the token from other accounts, so it self-heals, but until then the device can receive the previous user's pushes. Mitigate with a short timeout on `unregister()` (logout must not hang) plus optional `FirebaseMessaging.deleteToken()` after the DELETE, which makes the old token dead at FCM and lets the backend's stale-token reaper remove it [VERIFIED: push.js isStaleTokenError handles `registration-token-not-registered`]. Recommend including it (A5).

### Pitfall 12: Notification list scope vs `message` rows
The server never returns `message` rows (F-8). Do not design list UI or tests that expect chat rows; keep the thread aliases only for parity.

### Pitfall 13: D-08 request no longer in the list
`GET /expert/booking-requests` returns only pending requests; a push for a request already accepted, declined or expired-and-flipped is absent. Building `RequestDecisionKey` with default `isExpired = false` would show a decidable request and end in 409 `ALREADY_PROCESSED` [VERIFIED: RequestDecisionKey.kt KDoc]. Recommend: id not found in the list means open `IncomingRequestsKey` instead (Open Question 2).

## Code Examples

### Backend: channel id + android-only category/sub_category (extends D-04)
```javascript
// Source: backend-reference/src/services/push.js (current lines 217-253), proposed change
const CHANNEL_BY_CATEGORY = {            // the Android contract (UI-SPEC "Channels")
  booking: 'bookings', session: 'sessions', review: 'sessions',
  message: 'messages', payout: 'account_payouts', account: 'account_payouts',
};
const channelIdFor = (category) => CHANNEL_BY_CATEGORY[category] || 'general';

// .select('token, device_id, platform')
for (const { token, device_id, platform } of tokens) {
  const isAndroid = platform === 'android';
  await messaging.send({
    token,
    notification: { title, body },
    data: isAndroid ? { ...safeData, category: String(category || ''), sub_category: String(sub_category || '') } : safeData,
    apns: { payload: { aps: { sound: 'default' } } },                       // unchanged
    android: isAndroid
      ? { priority: 'high', notification: { channelId: channelIdFor(category) } }
      : { priority: 'high' },
  });
}
```
The iOS message stays deep-equal to today's. `tests/push.test.js` mocks `messaging().send` and the token chain (`select` returns `mockReturnThis`); extend it with: android token gets `channelId` + data keys, ios token's `send` argument equals the pre-change shape, unknown category maps to `general`, `module.exports` exposes `channelIdFor` for a literal-contract test. Backend process rules from the Phase 11 G-11-6 plan apply: absolute path `/Users/LeonChen/Documents/ClickMe2026 Backend/` (quote it, it contains a space), commit only explicit paths on `master` (tracks `origin/main`), the executor never pushes or deploys, the human pushes and confirms with `curl -s https://clickme-backend-production.up.railway.app/health` returning the deployed `commit`, and `npm test -- --silent tests/push.test.js` runs one file [CITED: .planning/phases/11-expert-requests-bookings/11-18-PLAN.md cross-repo rules]. Pre-existing failing suite: `tests/services/redis.test.js` (needs `REDIS_URL`).

### Android: manifest additions
```xml
<!-- Source: https://firebase.google.com/docs/cloud-messaging/android/get-started (meta-data names) -->
<service android:name=".services.push.ClickMeMessagingService" android:exported="false">
    <intent-filter><action android:name="com.google.firebase.MESSAGING_EVENT" /></intent-filter>
</service>
<meta-data android:name="com.google.firebase.messaging.default_notification_channel_id" android:value="general" />
<meta-data android:name="com.google.firebase.messaging.default_notification_icon" android:resource="@drawable/ic_stat_clickme" />
<meta-data android:name="com.google.firebase.messaging.default_notification_color" android:resource="@color/notification_accent" />
```
Add `notification_accent` (`#44F697`, mirrors `ClickMePalette.primary`) to the existing `res/values/colors.xml`. Extend the `CallManifestTest` pattern (`services/call/CallManifestTest.kt`) with a `PushManifestTest` asserting the service, the three meta-data entries and that `POST_NOTIFICATIONS` is declared exactly once.

### Android: pure intent parsing
```kotlin
// Source: pattern from MainActivity.consumeExpandCallExtra (MainActivity.kt) + F-2 extras
internal fun Intent?.toPushEvent(): PushEvent? {
    val extras = this?.extras ?: return null
    val map = extras.keySet().mapNotNull { k -> (extras.get(k) as? String)?.let { k to it } }.toMap()
    return PushEvent.from(map)            // category/sub_category, event_type fallback, UUID-validated ids
}
// MainActivity: if (savedInstanceState == null && !launchedFromHistory(intent)) postPush(intent); onNewIntent: postPush(intent)
```

### Android: registrar override (keeps existing tests compiling)
```kotlin
// Source: managers/PushTokenRegistrar.kt (open class, no-arg) + UserManagerTest RecordingRegistrar pattern
open class PushTokenRegistrar {            // no @Inject, no parameters
    open suspend fun register() {}
    open suspend fun unregister() {}
}
class FcmPushTokenRegistrar @Inject constructor(/* api, tokens, deviceId, @ApplicationContext */) : PushTokenRegistrar() {
    override suspend fun register() { /* guard Firebase + session, token.await(), POST */ }
    override suspend fun unregister() { /* DELETE, best effort, short timeout */ }
}
```

### Android: D-06 status classification (pure)
```kotlin
// Statuses verified in screens/clientmode/mybookings/BookingStatusDisplay.kt:
// "confirmed", STATUS_PENDING_APPROVAL = "pending_approval", STATUS_PENDING_RESCHEDULE = "pending_reschedule",
// "in_progress" are upcoming; STATUS_COMPLETED = "completed", "cancelled", "declined", "missed", "expired" are past.
fun isUpcomingStatus(status: String) = status in setOf("confirmed", "pending_approval", "pending_reschedule", "in_progress")
// upcoming -> UpcomingBookingKey(id) ; everything else, including unknown -> BookingSummaryKey(id)
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `firebase-messaging-ktx` | `firebase-messaging` (main artifact has the Kotlin APIs via the BoM 34+ line) | BoM 34 (2025) [ASSUMED] | Do not add `-ktx` |
| Implicit permission prompt on channel creation (targeting 32-) | Explicit `POST_NOTIFICATIONS` request, app controls timing (targeting 33+) | Android 13 | D-01 request is ours [CITED: developer.android.com/develop/ui/views/notifications/notification-permission] |
| FCM legacy `AndroidConfig` (v1) callbacks | `AndroidConfigV2` with `RemoteNotification` / `onNotificationReceived` | documented 2026 [CITED: firebase.google.com/docs/cloud-messaging/send/v2-api via search summary] | Backend uses v1; no change this phase |
| Play data safety for all tracks | Internal-only apps exempt | current policy | REL-04 is a pre-release draft, not an internal-track blocker |

**Deprecated/outdated:** `FirebaseInstanceId`/`getInstanceId` (use `FirebaseMessaging.token`); `google-services.json` committed with real values is a per-team choice, this repo gitignores it.

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | `firebase-messaging` main artifact needs no `-ktx` with BoM 35 | Standard Stack | A missing Kotlin extension; trivially fixed by adding `-ktx`. The probe resolved and the merged manifest built, but no Kotlin code was compiled against it |
| A2 | Without `google-services.json` at runtime, `FirebaseMessaging.getInstance()` throws, so every call must be guarded by `FirebaseApp.getApps(...)` | F-3 | If it does not throw, the guard is merely redundant |
| A3 | A push tapped while the app is on `SignupSetup` is dropped like Login (D-14 only names logged-out/expired sessions) | Pattern 2 | A resuming incomplete expert loses a tap; low impact. Confirm with the user |
| A4 | Android 13+ treats a second "Don't allow" as permanent (no further system prompt) | Pattern 7 | If wrong, a second prompt could appear; the persisted `asked` flag prevents our own re-prompt anyway |
| A5 | Adding `FirebaseMessaging.deleteToken()` after the logout DELETE is desirable | Pitfall 11 | Slightly more logout work; user may prefer iOS parity (DELETE only) |
| A6 | Play Console may require the foreground-service declaration before an internal-track rollout | F-9 / checklist | If it blocks, complete a minimal declaration first; if not required, the item simply moves to REL-04 drafting |
| A7 | Organization developer accounts are exempt from the 12 testers / 14 days closed-test rule (the cited page is silent on them) | F-9 | If not exempt, production access is delayed by 14+ days and Phase 17 slips; STATE already schedules a Console re-check |
| A8 | `Settings.Secure.ANDROID_ID` trips the HardwareIds lint and is unnecessary | Pattern 5 | None; a persisted UUID works either way |
| A9 | Whether paying for live 1-on-1 expert calls through Stripe satisfies the Google Play Payments policy | F-9 / Open Question 3 | A policy rejection at review would block release; needs a user/legal decision before Phase 17 |
| A10 | A booking push for a request that is no longer pending should open the Incoming Requests list rather than a decidable Request Decision | Pitfall 13 | If the user wants Request Decision regardless, expired/processed cases end in the 409 dialog |

## Open Questions

1. **Backend scope of D-04.**
   - What we know: `channel_id` alone is not enough; `category`/`sub_category` are missing from the FCM data (F-1). Doing it for Android tokens only keeps iOS identical.
   - What's unclear: whether the user also wants iOS to receive `category` (which would activate iOS routing for pushes it currently drops).
   - Recommendation: plan the Android-only change; record the iOS gap as a STATE follow-up for the user to decide.
2. **D-08 fallback when the request is not in the list (Pitfall 13) and D-08/D-05 overlap.**
   - D-08 says a client shell falls back to the notifications list; D-05 says switch modes. Recommendation: switch to Expert when the role is verified and no call is active, else open the current mode's notifications list; if the request id is missing from the pending list, open `IncomingRequestsKey`.
3. **Google Play Payments policy for Stripe-paid live calls (STATE "before release (Phase 13)" item).**
   - What we know: Play requires Play billing for in-app features/services/content except listed exceptions (physical goods/services, peer-to-peer payments, and others) and offers alternative-billing / external-offer programs only in eligible regions [CITED: Play Payments policy, answer/9858738].
   - What's unclear: classification of paid live voice sessions between users. This is a policy interpretation, not a code question.
   - Recommendation: keep it a human-checklist item with the policy text attached; Phase 17 needs the answer before submission.
4. **Mode switch during a live call.** Skip the switch and route in the current mode (recommended), or queue the switch until the call ends? The simplest compliant behavior is skip-and-fallback; confirm.
5. **Re-prompt edge.** Phase 9's call permission flow may already have asked for notifications. Accept a possible second ask here, or have the gate treat "Phase 9 asked" as answered (needs a shared flag)? Recommendation: share the flag via `PushPermissionGate` if it is a one-line change in `MicPermissionRequester`, otherwise accept.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| JDK 25 (Android Studio JBR) | All Gradle work | yes | `/Applications/Android Studio.app/Contents/jbr/Contents/Home` | — |
| Gradle / AGP | Build | yes (AGP 9.4.1, Gradle 9.6, config cache on) | per `gradle/libs.versions.toml` | — |
| Google Maven / Maven Central reachability | New deps | yes (probe resolved all four coordinates) | — | — |
| Node + npm (backend jest) | D-04 backend task | yes (backend repo has `node_modules`, firebase-admin 13.10.0) | Node 22 per backend `.nvmrc` | — |
| Backend git repo `/Users/LeonChen/Documents/ClickMe2026 Backend/` (master tracks origin/main) | D-04 task | yes (per Phase 11 plan; `backend-reference` symlink resolves in the main checkout) | — | executors in worktrees must use the absolute path |
| `adb` | Device checks | yes at `~/Library/Android/sdk/platform-tools/adb` (not on PATH) | — | emulator `Medium_Phone_API_37.0` |
| Samsung SM-A075F test device (Android 16) | Deferred device checks | yes (per memory notes) | API 36 | emulator |
| `google-services.json` for the Android app | Real FCM | **no** (user must create it in Firebase console, D-20) | — | guarded plugin: everything builds and tests pass without it; push is dead until supplied |
| Upload keystore `~/keys/clickme-upload.jks` + `keystore.properties` | Signed bundle | **no** (Phase 2 02-05 deferral, human) | — | unsigned `bundleProdRelease` builds; Play upload needs the signed one |
| Firebase project access (iOS project) and Play Console org account | REL-04/05 | user-side; Play org verified 2026-10-20 per STATE | — | non-blocking checklist |
| `timeout` command | n/a | no on macOS (not needed) | — | use the tool's timeout |

**Missing dependencies with no fallback:** none for code plans.
**Missing dependencies with fallback:** `google-services.json`, keystore: guarded plugin and unsigned builds keep CI and gates green; they gate only the human steps.

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 4.13.2 + Robolectric 4.17 + Compose ui-test (`createComposeRule`) + MockWebServer3 + kotlinx-coroutines-test; ~275 existing test files, fakes in `app/src/test/java/.../testing/` (`FakeClickMeApi`, `FakeProcessForeground`, `MutableClock`, `MainDispatcherRule`, `FakeCallGuard`) |
| Config file | none beyond `app/build.gradle.kts` (`unitTests.isIncludeAndroidResources = true`); backend: `jest.config.js` |
| Quick run command | `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:testDevDebugUnitTest --tests '*PushRouterTest' --tests '*PushEventTest' ktlintCheck` |
| Full suite command | `JAVA_HOME=... ./gradlew testDevDebugUnitTest testProdDebugUnitTest ktlintCheck -Dorg.gradle.jvmargs=-Xmx4g -Pkotlin.daemon.jvmargs=-Xmx3g`, then in a separate invocation `lintDevDebug lintProdDebug assembleProdRelease`, plus `bash scripts/check-design-rules.sh` (STATE: the combined gate OOMs at the default 2 GiB daemon; lint errors such as `NonObservableLocale` and `RememberInComposition` are only caught by `lint`, so run `lintDevDebug` before the gate plan) |
| Backend | `cd "/Users/LeonChen/Documents/ClickMe2026 Backend" && npm test -- --silent tests/push.test.js` (full: `npm test`; known pre-existing failure `tests/services/redis.test.js`) |

### Phase Requirements to Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| PUSH-01 | Register body is exactly `{device_id, token, platform:"android", app_version}`; no-op without session or Firebase; unregister issues `DELETE notifications/tokens/{id}`; device id stable across calls | unit (MockWebServer / FakeClickMeApi) | `./gradlew :app:testDevDebugUnitTest --tests '*FcmPushTokenRegistrarTest'` | ❌ Wave 0 |
| PUSH-01 | Registration triggered on logged-in x foreground transitions, not when logged out | unit (FakeProcessForeground) | `--tests '*PushRegistrationManagerTest'` | ❌ Wave 0 |
| PUSH-01 | Logout still unregisters before tokens clear (existing) | unit | `--tests '*UserManagerTest'` | ✅ |
| PUSH-02 | Five channel ids/importances equal the contract; `channelIdFor` mapping incl. review to sessions and default to general; creation idempotent | unit (Robolectric) | `--tests '*NotificationChannelsTest'` | ❌ Wave 0 |
| PUSH-02 | Manifest: service, 3 meta-data, single `POST_NOTIFICATIONS` | unit (Robolectric, CallManifestTest pattern) | `--tests '*PushManifestTest'` | ❌ Wave 0 |
| PUSH-02 | Permission decision table (API <33, granted, asked) | unit | `--tests '*PushPermissionGateTest'` | ❌ Wave 0 |
| PUSH-02 | Backend `channelId` per category; iOS message unchanged; android data gets category | jest | `npm test -- --silent tests/push.test.js` (backend) | ❌ Wave 0 (backend) |
| PUSH-03 | iOS table, every row; event_type fallback (iOS three plus this backend's values); cron `{booking_id}`-only; missing ids; non-UUID ids rejected | unit | `--tests '*PushEventTest' --tests '*PushRouterTest'` | ❌ Wave 0 |
| PUSH-04 | Intent to event: cold, warm, `LAUNCHED_FROM_HISTORY`, extras removed after use | unit (Robolectric) | `--tests '*MainActivityPushIntentTest'` | ❌ Wave 0 |
| PUSH-04 | Resolver: role table, client probe 200/404/403/network, D-06 status split, D-07, D-08 incl. absent-from-list, D-09, D-10 markers, call-active and no-expert-role fallbacks | unit (fakes) | `--tests '*PushDestinationResolverTest'` | ❌ Wave 0 |
| PUSH-04 | `Navigator.openRouted`: tab select, reset vs push-on-top, dedupe across stacks, key chain order, transition | unit | `--tests '*NavigatorOpenRoutedTest'` | ❌ Wave 0 |
| PUSH-04 | Host: waits for home route, drops on Login/logout, mode switch then post, timeout drop; shell consumes only own-mode route; survives Splash | unit + Compose (HomeShellsTest style) | `--tests '*PushRoutingViewModelTest' --tests '*HomeShellsTest'` | ❌ Wave 0 / ✅ extend |
| PUSH-04 | Foreground handler: always ticks; D-13 suppression (case-insensitive thread id); disabled notifications; stable ids; channel fallback | unit (Robolectric) | `--tests '*PushMessageHandlerTest'` | ❌ Wave 0 |
| PUSH-05 | Date grouping at midnight/DST edges with `MutableClock` + zone; relative time strings; `**bold**` parser incl. unmatched markers; meta_data alias extraction; row-to-destination incl. inert rows | unit | `--tests '*NotificationsGroupingTest' --tests '*NotificationBodyParserTest' --tests '*NotificationDestinationTest'` | ❌ Wave 0 |
| PUSH-05 | ViewModel states: loading, loaded, empty, failed, quiet reload on tick/resume, refresh-failure snackbar flag | unit | `--tests '*NotificationsViewModelTest'` | ❌ Wave 0 |
| PUSH-05 | Screen: sections, hint row visibility, inert vs button semantics, end note, bell tags | Compose (Robolectric) | `--tests '*NotificationsScreenTest'` | ❌ Wave 0 |
| REV-01 | Repository: context and submit bodies (`rating`, optional trimmed `comment`, no extra keys), 201/404/422 mapping | unit (MockWebServer) | `--tests '*ReviewsRepositoryTest'` | ❌ Wave 0 |
| REV-01 | Write Review VM: default 5, 1000 cap/truncate, trim/omit blank, read-only prefill, in-flight lock, error dialog, success hand-off, SavedStateHandle restore | unit | `--tests '*WriteReviewViewModelTest'` | ❌ Wave 0 |
| REV-01 | Booking Summary: Feedback block vs Leave a review visibility by status and review state; Past card pill hidden for reviewed ids; nested click | Compose | `--tests '*BookingSummary*' --tests '*PastBookingCard*'` | ✅ extend |
| REL-04 / REL-05 | Store listing, data safety, FGS declaration drafts; signed bundle uploaded; tester added; `google_app_id` present in the release APK | manual-only (Play Console / Firebase console are not scriptable; justified in the human checklist plan) | `./gradlew :app:bundleProdRelease` (build only) | n/a |

### Sampling Rate
- **Per task commit:** the quick command with the task's test classes plus `ktlintCheck`.
- **Per wave merge:** `testDevDebugUnitTest` (and `testProdDebugUnitTest` once) with `-Xmx4g`, then `bash scripts/check-design-rules.sh`.
- **Phase gate:** full suite both flavors, `lintDevDebug lintProdDebug`, `assembleProdRelease` (separate invocation), backend `npm test`, all green before `/gsd-verify-work`.

### Wave 0 Gaps
- [ ] All test files in the map above (none exist for push, notifications or reviews).
- [ ] Test fakes: `FakeClickMeApi` additions for the three new endpoints, a `FakeBookingProbe` / resolver lookup interfaces, a fake `FirebaseMessaging` seam (an interface `PushTokenSource` so JVM tests never touch Firebase).
- [ ] Backend: new cases in `tests/push.test.js` (file exists, extend).
- [ ] No framework install needed.

## Security Domain

`security_enforcement` is not set in `.planning/config.json`, so it is enabled.

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | yes (token registration needs the bearer; D-14 drops taps when logged out) | Existing `AuthHeaderInterceptor` / `TokenAuthenticator`; registration and routing both gate on `tokens.hasSession()` / `isLoggedIn` |
| V3 Session Management | yes | Logout unregisters the device token before clearing tokens (existing order); pending destination cleared via `SessionDataCleaner`; optional `deleteToken()` |
| V4 Access Control | yes | Server scopes every booking/review/notification call to the caller (404/403); the client never trusts the payload for authorization; mode switch to Expert only with the expert role |
| V5 Input Validation | yes | UUID-validate every id taken from `Intent.extras` (exported activity) and from `meta_data` before it enters a route or URL path; ignore non-String extras; cap review comment at 1000 and trim; Moshi codegen DTOs |
| V6 Cryptography | no new use | Tokens stay in `EncryptedSharedPreferences`; no custom crypto; FCM token is not logged in full |
| V8 Data Protection | yes | Notification content can contain names/amounts: `VISIBILITY_PRIVATE` with a generic public version (UI-SPEC); never log message bodies, booking details or the full FCM token; no push content stored on device |
| V13 API / Web Service | yes | `.strict()` register schema; no extra fields |

### Known Threat Patterns for Android push

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Another app sends an intent to the exported `MainActivity` with forged `booking_id`/`category` extras | Spoofing / Tampering | UUID validation; destinations are read-only and re-fetched with the user's own token; server enforces ownership; no secrets in extras |
| Notification `PendingIntent` hijack | Tampering | `FLAG_IMMUTABLE`, explicit `MainActivity` component, extras set at creation (project precedent) |
| Stale or shared-device token delivers a previous user's pushes | Information disclosure | Unregister on logout, backend one-token-per-account rule, optional `deleteToken()`; short timeout so logout never hangs |
| Sensitive content on a locked screen | Information disclosure | `VISIBILITY_PRIVATE` + generic public notification |
| Replay of an old notification re-routing the app | Repudiation / Tampering | Extras removed after use; no routing from `savedInstanceState != null` or history launches |
| Mode-switch side effect (client becomes expert) | Elevation of privilege (accidental) | Check `me.userRoles` before any push-driven switch to Expert |
| Over-collection in the Play data safety form | Compliance | Derive from actual data: account/profile (name, email, avatar), user IDs, voice audio (Agora, in transit), payments handled by Stripe (card data collected directly by Stripe, so generally not declared if the app never accesses it [CITED: answer/10787469]), FCM token / device id, chat messages, crash/diagnostics only if added; encryption in transit yes (TLS), deletion mechanism yes (Phase 14/17 account deletion) |

## Sources

### Primary (HIGH confidence)
- Repo files read this session: `13-CONTEXT.md`, `13-UI-SPEC.md`, `STATE.md`, `REQUIREMENTS.md`, `CLAUDE.md`; Android: `MainActivity.kt`, `ClickMeApplication.kt`, `ClickMeApp.kt`, `AppViewModel.kt`, `UserManager.kt` (login/logout/requestModeSwitch), `PushTokenRegistrar.kt`, `SessionStore.kt`, `Navigator.kt`, `NavigationState.kt`, `StackOrigin.kt`, `HomeClientScreen.kt`, `HomeExpertScreen.kt`, `ChatEntries.kt`, `ChatSessionState.kt`, `ChatLaunchRequest.kt`, `ExpertLaunchRequest.kt`, `BookingsLaunchRequest.kt`, `RequestDecisionKey.kt`, `ExpertBookingSummaryKey.kt`, `UpcomingBookingKey.kt`, `BookingSummaryKey.kt`, `ChatThreadKey.kt`, `BookingStatusDisplay.kt`, `ClientBookingDetail.kt`, `IncomingRequestsLogic.kt`, `CallNotificationFactory.kt`, `MicPermissionRequester.kt`, `RealtimeConnectionManager.kt`, `ProcessForeground.kt`, `AndroidManifest.xml`, `app/build.gradle.kts`, `gradle/libs.versions.toml`, `.gitignore`, `scripts/check-design-rules.sh`, `.github/workflows/ci.yml`.
- iOS: `PushRouter.swift`, `ClickMe2026AppDelegate.swift`, `PushEvent.swift`, `UserManager.swift` (push sections), `NotificationsListViewModel.swift`, `HomeClientView.swift`/`HomeExpertView.swift` `handlePushDestination`, `WriteReviewViewModel.swift`, `ReviewContextData.swift`, `ClickMeAPI+Reviews.swift`.
- Backend: `services/push.js`, `services/events.js`, `controllers/notifications.controller.js`, `controllers/notificationPrefs.controller.js`, `schemas/notification.schema.js`, `schemas/notificationPrefs.schema.js`, `controllers/reviews.controller.js`, `schemas/reviews.schema.js`, `controllers/chats.controller.js` (push block), cron notification jobs, account/connect/webhook push calls, `middleware/roleCheck.js`, `routes/*`, `openapi/clickme-api.yaml` (`/notifications`), `node_modules/firebase-admin` typings.
- Local probe builds (throwaway worktree, removed): google-services 4.5.0 + BoM 35.1.0 on AGP 9.4.1, with and without `google-services.json`; javap of `firebase-messaging-26.0.1` `CommonNotificationBuilder`.
- Maven metadata: dl.google.com (BoM, google-services, firebase-messaging), repo1.maven.org (coroutines-play-services).

### Secondary (MEDIUM confidence)
- https://firebase.google.com/docs/android/setup (plugin and BoM versions)
- https://firebase.google.com/docs/cloud-messaging/android/get-started (meta-data keys, POST_NOTIFICATIONS, onNewToken)
- https://firebase.google.com/docs/cloud-messaging/android/receive-messages (background tap extras, v1 vs v2)
- https://developer.android.com/develop/ui/views/notifications/notification-permission
- https://support.google.com/googleplay/android-developer/answer/9845334 (internal testing), /answer/10787469 (data safety), /answer/13392821 (foreground service declaration), /answer/14151465 (closed-test requirement), /answer/9858738 (Payments policy)
- https://github.com/Kotlin/kotlinx.coroutines/tree/master/integration/kotlinx-coroutines-play-services

### Tertiary (LOW confidence)
- Web-search summary of the FCM `AndroidConfigV2` migration page (noted only as State of the Art; no decision depends on it).

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH. Versions read from the registries and a real build resolved them under this project's AGP, Gradle and config cache.
- Architecture (routing, hand-offs, Navigator): HIGH for the seams (all read in source); MEDIUM for exact Nav3 behavior on duplicate keys (taken from the project's own Pitfall 7 KDoc, not re-tested here).
- Backend contract and the F-1 gap: HIGH. Read from the controllers and the sender.
- Pitfalls: HIGH for code-derived ones, MEDIUM for platform-behavior ones tagged `[ASSUMED]`.
- Play Console policy: MEDIUM. Cited pages are explicit on data safety, FGS and internal testing; silent on org-account exemption and on the Payments classification.

**Research date:** 2026-10-28
**Valid until:** 2026-11-27 (stable stack); re-check Play Console policy pages and the Firebase BoM at execution time.
