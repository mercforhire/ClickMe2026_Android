# Phase 9: Voice Call - Research

**Researched:** 2026-10-24
**Domain:** Android voice calling: Agora RTC in a `microphone` foreground service, an app-scoped call owner, a root Compose overlay, Android 14/17 foreground-service and background-audio rules
**Confidence:** HIGH on the iOS port, backend contract and local code. MEDIUM on Android 17 background-audio behaviour and Agora audio-focus behaviour (docs are thin, so these are flagged for the end-of-phase device check).

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

- **D-01: Wire all three `PHASE-9` Join placeholders to one `CallCenter.startCall(...)`.**
  - Upcoming Booking Details (`UpcomingBookingJoinCard`), whether it was opened from Explore or from My Bookings.
  - The My Bookings upcoming card (`mybookings/subviews/UpcomingBookingCard.kt`).
  - The Explore Today's Sessions card (`ExploreTodaysSessionsSection`).
  - This closes the iOS gap at `MyBookingsView.swift:273`, where the card passes an empty `onJoinCall`. Remove every `PHASE-9` marker.
- **D-02: Join is enabled by the iOS client-side rule** (`UpcomingBooking.isWithinJoinWindow`).
  - Never for `pending_approval`.
  - For `completed`, enabled while `endTime > now` (rejoin).
  - Otherwise enabled within ±3600s of `startTime`.
  - Don't use the server's `actions.canJoin`. iOS dropped it because its window (~5 min) is too tight. The server still has the final say on `POST /join`.
- **D-03: The enabled state updates live.** A lightweight clock flow (about every 30s, Claude picks the exact cadence) re-evaluates the window while a screen with a Join button is visible, so the button turns on without leaving the screen.
- **D-04: Join failures follow iOS `CallCenter.joinError`.**
  - Covers server refusal, network errors, and a missing `agora_config` or `join_url`.
  - The call container tears down immediately, with no `/end`.
  - One root-level Material `AlertDialog` shows the user message.
  - A mic-permission denial is a separate signal with its own dialog (CALL-02, Claude's discretion).
- **D-05: Port the iOS external-meeting surface. Don't just launch the link.**
  - A `skype_zoom` join uses the same call container and pill, in an external variant: peer info, the schedule-anchored timer, **Open in Skype/Zoom**, **Copy link** with a short "Link copied" confirmation, and Hang up.
  - The initial variant comes from the booking's `meeting_type` hint. The `POST /join` response overrides it.
  - `confirm-connection` (`method: "skype_zoom"`) fires once, only when the user taps Open. Copy doesn't count.
  - Open also minimizes the call to the pill.
  - Hang up sends `/end`, as on the Agora path.
- **D-06: Open the link with an app intent first and a Custom Tab as fallback.**
  - Try `ACTION_VIEW` first, so an installed Zoom, Skype or Teams app can claim the link.
  - If nothing resolves it, use the existing `components/web/CustomTabLauncher.kt`.
  - This refines CALL-01's "Custom Tab or intent".
- **D-07: The external variant gets no foreground service and no notification.**
  - There's no mic and no Agora, and a `microphone`-type foreground service without mic use would be a Play policy problem.
  - The 3600s cutoff is enforced only while the process is alive.
- **D-08: The call UI is a root composable overlay, not a separate Activity.**
  - A `CallOverlayHost` sits in `ClickMeApp` above the `AppRoute` switch, so it survives tab changes and the Client/Expert mode switch.
  - Expanded call: a full-screen layer that handles edge-to-edge insets itself. iOS used `fullScreenCover` because a ZStack overlay lost the safe area.
  - Minimized: a floating pill near the bottom trailing corner, above the bottom navigation, as on iOS.
  - Call state lives in an app-scoped singleton (`CallCenter`, provided by Hilt), not in a screen ViewModel. The view can then mount and unmount without tearing down Agora.
- **D-09: System Back and predictive back on the expanded call minimize it to the pill.** Only Hang up ends the call. This matches iOS `expandedBinding`.
- **D-10: The ongoing-call notification opens the call on tap and has a Hang up action.**
  - Tap opens `MainActivity` with the call expanded.
  - The Hang up action runs the same end flow as the in-app button: leave the channel, `POST /end`, tear down.
  - The notification shows the peer name and a timer.
  - No Mute action.
- **D-11: The in-call Chat button is shown disabled** until Phase 12. This keeps the iOS control-row layout (Minimize, Chat, Mute, Speaker). Phase 12 enables it (`initiateChat`, minimize, switch to Chats, push the thread).
- **D-12: Build the guard API now.**
  - Port `attempt(action)` ("End the call to {action}.") and `attemptModifyBooking(bookingId, action)` ("This session is in progress. End the call to {action}.").
  - One root `AlertDialog` titled "Can't do that during a call", with an OK button.
  - A second `startCall` during an active call shows "You're already on a call. End it before starting another."
- **D-13: Wire the guard at the call sites that exist today.**
  - Switch modes (`screens/settings/modeswitching/ModeSwitchScreen.kt`).
  - Log out (wherever the logout action lives now).
  - Add a payment method (Phase 7 Payment Methods).
  - Client Cancel on the booking in the call (`attemptModifyBooking`), plus client Reschedule if Android has that entry point.
  - Later phases wire their own call sites: Phase 10 (availability), Phase 11 (expert reschedule) and Phase 14 (change password, delete account, payouts). Add a note to those roadmap entries.
- **D-14: The call keeps going after a task swipe.** The foreground service outlives the task, and its notification reopens the app with the call expanded. Don't end the call in `onTaskRemoved`.
- **D-15: Don't restore a call after process death.**
  - If the process is killed anyway (an OEM battery killer, a crash), the call is gone, and the app persists no call state.
  - The booking stays live on the server, so the user just taps Join again (rejoin is allowed for the whole window).
- **D-16: Verify on a device only at phase end.**
  - The Agora spike's background-audio test (Phase 2 UAT #9) was never run.
  - Don't add a mid-phase device gate. Do one end-to-end check in UAT: Samsung client plus iOS expert, 5+ minutes in the background with the screen off, notification Hang up, rejoin, and a full paid session with `confirm-connection` sent once.
  - Following the project's deferred-manual-testing rule, log it as an open item if it can't run right away.
  - Delete `:spikes:agora` once its `CallService` findings are absorbed into `:app` (Phase 2 D-10).

**Behavior ported as-is from iOS (no discussion needed)**
- `callStatus` is derived from (my connection state, remote connection state):
  - local disconnected -> `ended`
  - remote disconnected, or local ready while remote is waiting -> `waitingForPeer`
  - both ready -> `active`, and run `confirmConnectionOnce()`
  - local waiting -> `connecting`
- A `call.ended` event for the same booking id moves the call to `waitingForPeer` and never ends it. Events for other bookings are ignored.
- `confirm-connection` uses a local once-per-call guard. It also fires on rejoin (it's idempotent on the server now). A 409 is ignored, and other errors are logged without interrupting the call.
- End reason: `completed` if confirm-connection was sent, otherwise `manual_exit`. `actual_duration` is seconds since the local join. Always send `/end`. Never branch on the `/end` response status (CALL-END-SEMANTICS).
- Timer: `elapsed = now - scheduledStart`. Before the start it shows "Starts in M:SS". Amber with "M:SS left" when 0 < remaining <= 300s. Past `scheduledEnd` it shows the "scheduled time complete" state and the call continues. At `scheduledEnd + 3600s` the call is force-ended, once (CALL-07; not the iOS `5 * 60` TEMP value).
- Audio defaults: as in the Phase 2 spike `CallService` (speech-standard profile, speakerphone on by default, mic unmuted).

### Claude's Discretion

- Mic permission UX (CALL-02): the rationale dialog before the request, the permanently-denied dialog with an "Open settings" app-details intent, and POST_NOTIFICATIONS on API 33+ (asked together or separately).
- Audio focus, audio routing (earpiece, speaker, Bluetooth) and interruptions such as an incoming phone call. Follow Agora and Android defaults sensibly.
- What happens on logout or session expiry during a call. Logout is guarded (D-13). If a forced logout (refresh 401) happens mid-call, end the call before clearing the session.
- Ticker cadence, pill visuals and animation, the notification channel's name and importance, and the exact copy of the external variant's status lines (port the iOS strings).
- How the service and `CallCenter` share state: bound service vs. a singleton the service observes.

### Deferred Ideas (OUT OF SCOPE)

- Enabling the in-call Chat button: Phase 12.
- Expert-side Join entry points: Phase 11.
- Guard call sites for availability (Phase 10), expert reschedule (Phase 11), and change password, delete account and payouts (Phase 14).
- A "Rejoin your call?" prompt after process death. Not planned (D-15).
- The Play Console foreground-service declaration and video: Phase 17.

Also out of scope (CONTEXT domain block): `Call/Ready` (`ReadyToStartCallView`), expert-side Join entry points, guard call sites on screens that do not exist yet, restoring a call after process death.
</user_constraints>

## Project Constraints (from CLAUDE.md)

- The Swift code under `ios-reference/` is the source of truth for behaviour. Deviate only for the Android platform swaps (`.alert` -> `AlertDialog`, `.sheet` -> `ModalBottomSheet`, `.task` -> `LaunchedEffect(Unit)`, back chevron -> `TopAppBar`).
- Stack is fixed: Jetpack Compose + Material 3, MVVM with Compose State, Kotlin coroutines (no RxJava), Hilt, Retrofit + Moshi, EncryptedSharedPreferences for the JWT, Firebase Messaging, Agora Android SDK for voice, minSdk 26 / targetSdk 37 / compileSdk 37.
- File organization mirrors the iOS folder tree (`screens/call/calling/MeetingCallScreen.kt` + `MeetingCallViewModel.kt`, `managers/CallCenter.kt`).
- Backend contract is identical to iOS. `backend-reference/docs/ios/CALL-END-SEMANTICS.md` is authoritative for hang-up behaviour.
- CI gates that every plan must keep green: `bash scripts/check-design-rules.sh` (no `Color(0x..)` literals, no `*Brand` types, no Stripe secret keys), `ktlintCheck`, `lintDevDebug lintProdDebug`, `testDevDebugUnitTest testProdDebugUnitTest`, `assembleDevDebug assembleProdDebug`.
- Memory notes that affect planning: build with `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"` (JDK 25); worktree executors need `local.properties` copied and must read iOS/backend sources via absolute paths (`/Users/LeonChen/Documents/ClickMe2026/`, `/Users/LeonChen/Documents/ClickMe2026 Backend/`); manual device checkpoints are logged as open items and never block; the combined test + lint + R8 gate can OOM at `-Xmx2048m` (run lint and R8 in a separate invocation).
- No `.claude/skills/` or `.agents/skills/` directories exist in this repo.

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| CALL-01 | Join from Upcoming Booking Details (also from My Bookings). `agoraConfig` starts an in-app call; `externalConfig.joinUrl` opens externally | Join dual-shape response, `JoinWindow` rule + `JoinClock`, three Join sites, `ExternalLinkLauncher` with `FLAG_ACTIVITY_REQUIRE_NON_BROWSER` then Custom Tab (Pitfall 4), server window mismatch (Pitfall 5) |
| CALL-02 | Mic permission with rationale; denial shows recovery dialog | Permission gate pattern (Pattern 4), `shouldShowRequestPermissionRationale` decision table, `ACTION_APPLICATION_DETAILS_SETTINGS`, POST_NOTIFICATIONS non-blocking |
| CALL-03 | `microphone` foreground service, audio focus, persistent notification, survives background | Foreground-service rules (Android 14 to 17), thin `CallService` + `CallForegroundBridge`, platform `AudioFocusRequest` from the service, notification design incl. dismissibility finding (Pitfall 3), manifest block |
| CALL-04 | Root overlay, minimize to pill across tabs and modes, in-call guard | `CallOverlayHost` in `ClickMeApp`, conditional `BackHandler` ordering (Pitfall 2), `attempt` / `attemptModifyBooking`, guard sites |
| CALL-05 | `confirm-connection` exactly once when both sides joined | Derived `callStatus` machine, per-session `confirmSent` guard, request body shape, 409 handling |
| CALL-06 | `call.ended` from peer shows "waiting for peer" and does not end the call | Model `call.ended` as `remote = Disconnected` (single source of truth), Agora `onUserOffline` is the primary signal in the background (Pitfall 6), echo race (Pitfall 7) |
| CALL-07 | Timer from scheduled start, amber in the last 5 min, 3600 s cutoff | Pure `CallTimer` helpers + injected `Clock`, cutoff job in `CallCenter` (not the UI ticker), Doze/uptime drift (Pitfall 11) |
| CALL-08 | Hang up leaves channel then `POST /end`; rejoin for the whole window | End-flow ordering, always-send `/end`, ignore the response, `completed` 409 handling, rejoin = a fresh `startCall` |
</phase_requirements>

## Summary

Phase 9 is a port of five iOS pieces (`CallCenter`, `MeetingCallViewModel`, `AgoraManager`, the call views, `CallOverlayHost`) plus one genuinely new Android concern: keeping an Agora call alive in a `microphone` foreground service across backgrounding, screen-off and task swipe. The iOS logic is small and well understood (state machine of two connection states, confirm-once, schedule-anchored timer, always-send-`/end`). The risk is entirely platform: when the foreground service may start, what Android 17 does to background audio, how the overlay interacts with Back and accessibility, and the notification being user-dismissible on Android 14+.

The central design recommendation is to keep **all call logic in a pure-Kotlin, Main-confined `CallCenter` singleton behind three small ports** (`CallEngine` over Agora, `CallForegroundController` over the service, `CallPermissionGate` fulfilled by the Compose host). The service becomes a thin holder of the notification and the `startForeground` call. That makes every CALL requirement except "audio actually flows in the background" testable in JVM/Robolectric unit tests with fakes, and keeps the single unavoidable device check (D-16) as small as possible. Everything else needed already exists: Agora 4.6.4 is in the version catalog (Maven Central's latest), the `RealtimeEvent.CallEnded` decoder, `CustomTabLauncher`, `AuthErrorDialog`, `PressScaleButton`, the `SessionDataCleaner` set, `@ApplicationScope`, and `FakeClickMeApi` / `FakeRealtimeConnection` / `MutableClock` test fakes. No new library other than adding `libs.agora.voice.sdk` to `:app` is needed.

Four findings change the plan versus a literal reading of CONTEXT/UI-SPEC and must reach the planner: (1) the backend's real join window in code is 120 min before to 120 min after (docs say 10 min / 0), and the token TTL is `end + 130 min`, so the 3600 s client cutoff is safe today but the window is flagged "tighten before launch"; (2) Android 14+ lets users swipe away an ordinary `setOngoing(true)` foreground notification, only `CallStyle` is exempt; (3) `ACTION_VIEW` on an `https` link always resolves to a browser, so "app first, Custom Tab fallback" needs `FLAG_ACTIVITY_REQUIRE_NON_BROWSER` (API 30+); (4) a Compose `BackHandler` registered early loses to handlers registered later, so the expanded layer's handler must be composed only while expanded.

**Primary recommendation:** Build `CallCenter` (+ `CallEngine`/`AgoraManager`, `CallForegroundController`/`CallService`, `CallPermissionGate`) as testable, port-based Kotlin first; start the foreground service only from the resumed, visible app after the mic grant and before creating the Agora engine; add `libs.agora.voice.sdk` to `:app`; and finish with one device UAT that proves background audio, notification hang-up and exactly-once `confirm-connection`.

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Call state machine, confirm-once, end flow, cutoff | App-scoped manager (`CallCenter`, Hilt singleton, Main-confined) | — | Must outlive any screen and the Activity (D-08, D-14). Pure Kotlin so it is unit-testable |
| Audio transport (join/leave/mute/speaker) | Agora SDK behind `CallEngine` port (`AgoraManager`) | Foreground service (keeps process eligible) | Engine lives in the process, not in the service, so the UI and service share it without binding |
| Background survival and notification | Android foreground service (`CallService`, type `microphone`) | `CallNotificationFactory` | Only a started FGS keeps mic capture and playback alive off-screen (Agora and Android docs) |
| Full-screen call UI, pill, dialogs | Compose root overlay (`CallOverlayHost` in `ClickMeApp`) | `MeetingCallViewModel` as a thin view-state adapter | D-08: a layer above `AppRoute`, not a nav destination or Activity |
| Mic and notification permission prompt | Activity-result launcher hosted by `CallOverlayHost` | `CallCenter` awaits a `CompletableDeferred` | Launchers need a composition/Activity; the manager only awaits the result |
| Join enablement (window + live clock) | Pure function `isWithinJoinWindow` + `JoinClock` singleton | Per-screen ViewModels | Same rule for 3 sites; one 30 s flow shared |
| Join/confirm/end REST | `ClickMeApi` (Retrofit) | `CallCenter` | Same contract as iOS; no new transport |
| `call.ended` signal | `RealtimeConnection.events` (Phase 3) | Agora `onUserOffline` (primary while backgrounded) | The socket disconnects when the process is not STARTED |
| External meeting open/copy | Android intents (`ExternalLinkLauncher`) + `CustomTabLauncher` | `ClipboardManager` | No Agora, no service (D-07) |
| Guard (`attempt`) | `CallCenter` API + one root dialog | Call sites in ViewModels/screens | One centralized message, as iOS |

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| `io.agora.rtc:voice-sdk` | 4.6.4 | RTC voice engine | Already in `gradle/libs.versions.toml` (`agora-voice-sdk`, used by `:spikes:agora`); 4.6.4 is the newest release on Maven Central (metadata `lastUpdated` 2026-09-01) [VERIFIED: repo1.maven.org maven-metadata.xml]. Add `implementation(libs.agora.voice.sdk)` to `:app` |
| AndroidX Core `NotificationCompat`, `ServiceCompat`, `ContextCompat`; platform `android.media.AudioFocusRequest` (API 26, equals minSdk) | core-ktx 1.19.1 (already in catalog) | Notification, foreground start, audio focus | Already on the classpath, no extra artifact [VERIFIED: libs.versions.toml] |
| androidx.browser Custom Tabs | 1.10.0 (existing) | External-link fallback | Existing `CustomTabLauncher.kt` |
| Hilt | 2.60.1 (existing) | `CallCenter` singleton, `@AndroidEntryPoint` service, `@IntoSet SessionDataCleaner` | Project standard |
| kotlinx-coroutines | 1.11.0 (existing) | Main-confined state, cutoff job, `CompletableDeferred` permission gate | Project standard |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| Retrofit + Moshi (codegen) | existing | Three new endpoints, `JoinCallResponse` dual shape | Same pattern as Phase 8 booking endpoints |
| Robolectric 4.17, `compose-ui-test-junit4`, `kotlinx-coroutines-test`, `mockwebserver3` | existing | Unit and Compose tests | Validation Architecture |
| Coil + `Avatar` | existing | Hero, pair and pill avatars | UI-SPEC |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Plain `NotificationCompat.Builder` ongoing notification (UI-SPEC) | `NotificationCompat.CallStyle.forOngoingCall(person, hangUpIntent)` | CallStyle is exempt from Android 14 swipe-dismissal and gives a system "Hang up" button, but changes the approved copy and layout (see Open Question 1) |
| Engine owned by the service (spike) | Engine owned by an app singleton, service only holds the notification | Singleton engine is shared with the UI with no binding and is testable. Spike put it in the service only because it had no `CallCenter` |
| Telecom `ConnectionService` / `CallStyle` self-managed | Not recommended | Heavy API for a voice-only booking app; Android 17 docs say VoIP apps "typically" meet the playback rules via Telecom, but a started FGS with while-in-use also satisfies them [CITED: developer.android.com/about/versions/17/changes/bg-audio] |
| `foregroundServiceType="microphone"` only (D-locked) | `microphone|mediaPlayback` (Agora's generic sample) | Agora docs suggest all three types for generic apps; CONTEXT locks `microphone` and an extra type adds a Play declaration. Keep `microphone`, verify on the API 37 emulator (see Pitfall 1) |

**Installation (`app/build.gradle.kts`):**
```kotlin
implementation(libs.agora.voice.sdk)
```
No other dependency change. Consumer R8 rules ship inside the AAR (`-keep class io.agora.**{*;}`, `-dontwarn io.agora.**`) and the project's `optimization.packageScope` is limited to `androidx.**`, `kotlin.**`, `kotlinx.**`, so Agora is not optimized away [VERIFIED: AAR `proguard.txt`, `app/build.gradle.kts`].

**Version verification:**
```bash
curl -s https://repo1.maven.org/maven2/io/agora/rtc/voice-sdk/maven-metadata.xml | tail -12   # 4.6.4 last stable
```
Native libs: the `arm64-v8a` `.so` files in 4.6.4 have `p_align = 0x4000` (16 KB), which satisfies the Play 16 KB page-size requirement for that ABI [VERIFIED: ELF program-header check of voice-rtc-basic-4.6.4.aar, this session]. Optional size trim for Phase 17: depend on `io.agora.rtc:voice-rtc-basic` instead of `voice-sdk` (the umbrella pulls ains, aiaec, spatial-audio, audio-beauty and full-voice-drive, none of which the app uses) [VERIFIED: voice-sdk-4.6.4.pom dependency list].

## Package Legitimacy Audit

| Package | Registry | Age | Downloads | Source Repo | slopcheck | Disposition |
|---------|----------|-----|-----------|-------------|-----------|-------------|
| `io.agora.rtc:voice-sdk` 4.6.4 | Maven Central | multi-year (66 published versions; first 4.x in 2022) | n/a (Maven) | agora.io (vendor SDK, named in CLAUDE.md and in the Phase 2 spike build) | not available (slopcheck is a PyPI tool; it could not be installed and does not cover Maven) | Approved: already in the project's version catalog and compiled in `:spikes:agora`; named by CLAUDE.md as the project's voice SDK. Treated as `[CITED: CLAUDE.md, docs.agora.io]` rather than `[VERIFIED]` because slopcheck did not run |

**Packages removed due to slopcheck [SLOP] verdict:** none
**Packages flagged as suspicious [SUS]:** none
No other external package is added by this phase. Everything else is AndroidX / Hilt / existing catalog entries.

*slopcheck was unavailable at research time. The only new coordinate is the Agora SDK the project already depends on in a spike, so no `checkpoint:human-verify` is needed beyond confirming the catalog entry is unchanged.*

## Architecture Patterns

### System Architecture Diagram

```
 Join tap (Upcoming Details | My Bookings card | Today's Sessions card)
        |   enabled only if isWithinJoinWindow(status, start, end, JoinClock.now)
        v
 CallCenter.startCall(args)  [Hilt singleton, Main-confined, StateFlow<CallSession?>]
        |-- session != null ----------------------> GuardDialog "You're already on a call..."
        v
 session = Connecting, isMinimized = false  ---------------> CallOverlayHost (in ClickMeApp)
        |                                                      |- expanded layer / pill
        v                                                      |- dialog queue (join error, mic, guard, cutoff)
 api.joinCall(id) --error--> abort(no /end) + JoinError dialog |- BackHandler only while expanded
        |
        |-- connection_type = skype_zoom ----------------------+
        |     external_config.join_url null -> JoinError       |
        |     else externalJoinUrl set, timer runs             v
        |     [user taps Open] -> confirmOnce(skype_zoom) -> ExternalLinkLauncher
        |                          (ACTION_VIEW REQUIRE_NON_BROWSER -> CustomTab) -> minimize
        |
        |-- connection_type = in_app_voice
        |     agora_config null -> JoinError
        |     CallPermissionGate.ensureMicrophone()  <---- host launcher (RECORD_AUDIO [+ POST_NOTIFICATIONS])
        |        denied -> abort(no /end) + Mic dialog
        |     visible-app check, CallForegroundController.startAndAwait()
        |        -> CallService.onStartCommand -> startForeground(MICROPHONE) -> bridge.onStarted
        |     CallEngine(AgoraManager).create + joinChannel(token, channel, uid)
        v
 Agora events (SDK thread) -> EngineEvent -> CallCenter (Main)
        local: Connecting/Ready/Failed          remote: Waiting/Ready/Disconnected
        (mine, remote) --derive--> callStatus: connecting | waitingForPeer | active | ended
        (Ready, Ready) --> confirmConnectionOnce --> PATCH confirm-connection (once per session)

 RealtimeConnection.events: CallEnded(booking == session.booking) -> remote = Disconnected -> waitingForPeer
 Cutoff job: wall clock >= scheduledEnd + 3600 s -> endCall(forced) -> TimeLimit dialog

 Hang up (in-app button | notification "End call" -> CallService ACTION_END -> CallCenter.endCall)
        isEnding=true -> engine.leave -> POST /end (ignore result, bounded timeout)
        -> engine.destroy -> CallForegroundController.stop -> session = null
 Forced logout / suspension -> CallCenter as SessionDataCleaner -> local teardown (no /end, tokens are gone)
```

### Recommended Project Structure
```
app/src/main/java/com/uptrendinvestments/clickmeapp/
├── managers/
│   ├── CallCenter.kt              # singleton; CallSession, CallStatus, CallDialog; startCall/endCall/minimize/expand/attempt*
│   ├── CallEngine.kt              # port: join/leave/destroy/setMuted/setSpeaker + Flow<EngineEvent>
│   ├── AgoraManager.kt            # RtcEngine implementation of CallEngine (ports iOS AgoraManager)
│   ├── CallTimer.kt               # pure: elapsed, remaining, final stretch, past end, cutoff instant, labels data
│   ├── JoinWindow.kt              # pure: isWithinJoinWindow(...) + JoinClock (30 s StateFlow)
│   └── ExternalLinkLauncher.kt    # scheme allow-list, ACTION_VIEW + REQUIRE_NON_BROWSER, Custom Tab fallback
├── services/call/
│   ├── CallService.kt             # @AndroidEntryPoint thin FGS (absorbs spike findings)
│   ├── CallForegroundBridge.kt    # @Singleton: start(info) / awaitStarted / stop; implements CallForegroundController
│   └── CallNotificationFactory.kt # channel + builder (channel created in Application.onCreate AND service.onCreate)
├── model/{enums/ConnectionType.kt, enums/EndReason.kt, requests/ConfirmConnectionRequest.kt,
│          requests/EndCallRequest.kt, responses/JoinCallResponse.kt, responses/EndCallData.kt}
├── network/ApiRequestUrls.kt + ClickMeApi.kt   # BOOKING_JOIN, BOOKING_CONFIRM_CONNECTION, BOOKING_END
├── di/CallModule.kt               # @Binds CallEngine, CallForegroundController; @IntoSet SessionDataCleaner for CallCenter
├── screens/call/calling/          # CallOverlayHost, MeetingCallScreen, MeetingCallViewModel, CallDialogs,
│   └── subviews/                  # ExternalMeetingProvider, subviews per UI-SPEC
└── (edits) ClickMeApp.kt, MainActivity.kt, ClickMeApplication.kt, AndroidManifest.xml,
    realtime/RealtimeConnectionManager.kt (optional keep-alive), the 3 Join sites, 4 guard sites, settings.gradle.kts (drop :spikes:agora)
```

### Pattern 1: Port-based `CallCenter` (testable core)
**What:** `CallCenter` depends on interfaces, never on Android or Agora types.
```kotlin
interface CallEngine {
    val events: Flow<EngineEvent>                       // thread-safe; Agora callbacks arrive on SDK threads
    fun join(appId: String, token: String, channel: String, uid: Int): Boolean
    suspend fun leave()                                 // leaveChannel, awaits onLeaveChannel (bounded)
    fun setMuted(muted: Boolean); fun setSpeakerOn(on: Boolean)
    suspend fun destroy()                               // off the main thread, never from a callback
}
sealed interface EngineEvent {
    data object LocalJoined : EngineEvent               // onJoinChannelSuccess / onRejoinChannelSuccess / CONNECTED
    data object LocalFailed : EngineEvent               // CONNECTION_STATE_FAILED, or DISCONNECTED w/ BANNED/INVALID_TOKEN/TOKEN_EXPIRED
    data class RemoteJoined(val uid: Int) : EngineEvent // onUserJoined
    data class RemoteOffline(val uid: Int, val reason: Int) : EngineEvent
    data class Error(val code: Int) : EngineEvent
}
interface CallForegroundController { suspend fun startAndAwait(info: CallNotificationInfo): Boolean; fun stop() }
fun interface CallPermissionGate { suspend fun ensureMicrophone(): MicResult }  // Granted | DeniedOnce | DeniedPermanently | Cancelled
```
**When to use:** always; this is what makes CALL-05/06/07/08 JVM-testable.
**Threading:** all `CallCenter` state transitions run on `Dispatchers.Main.immediate` (`@ApplicationScope` is `Dispatchers.Default`, so launch with `scope.launch(Dispatchers.Main.immediate)`; unit tests replace Main with `MainDispatcherRule`). `CallSession` is exposed as `StateFlow<CallSession?>`.

### Pattern 2: Derived `callStatus` (port of `wireAgoraStateSync`)
```kotlin
enum class ConnState { Waiting, Ready, Disconnected }
fun deriveStatus(mine: ConnState, remote: ConnState): CallStatus = when {
    mine == ConnState.Disconnected -> CallStatus.Ended
    remote == ConnState.Disconnected -> CallStatus.WaitingForPeer
    mine == ConnState.Ready && remote == ConnState.Ready -> CallStatus.Active
    mine == ConnState.Ready -> CallStatus.WaitingForPeer     // remote Waiting
    else -> CallStatus.Connecting                            // mine Waiting
}
```
The iOS `switch` order is preserved (local-disconnected first, then remote-disconnected). Once `Ended` the status is sticky (iOS `guard callStatus != .ended`). `Active` triggers `confirmConnectionOnce()`.

**Android mapping that differs from iOS (important):** the iOS code maps `didOfflineOfUid` with `uid == 0` to "local disconnected", which never occurs on a real callback, so iOS never reaches `Ended` from Agora. On Android map:
- `onJoinChannelSuccess` / `onRejoinChannelSuccess` / `onConnectionStateChanged(CONNECTED)` -> `mine = Ready`
- `onConnectionStateChanged(FAILED)` (SDK gives up after about 20 min of reconnecting) or `DISCONNECTED` with reason `BANNED_BY_SERVER`, `INVALID_TOKEN`, `TOKEN_EXPIRED` that was not caused by our own `leaveChannel` -> `mine = Disconnected` (status `Ended`, timer frozen, End button stays so the user can hang up and rejoin)
- `RECONNECTING` / `onConnectionLost` -> no status change (the SDK retries by itself)
- `onUserJoined(uid)` -> `remote = Ready`; `onUserOffline(uid, QUIT | DROPPED)` -> `remote = Disconnected` (both reasons mean the peer is gone for this purpose; `BECOME_AUDIENCE` cannot occur in `COMMUNICATION` profile)
[VERIFIED constants in agora-rtc-sdk.jar 4.6.4 via `javap`: `CONNECTION_STATE_*` 1..5, `CONNECTION_CHANGED_*`, `USER_OFFLINE_QUIT/DROPPED/BECOME_AUDIENCE`, `ERR_TOKEN_EXPIRED=109`, `ERR_INVALID_TOKEN=110`, callbacks `onTokenPrivilegeWillExpire`, `onRequestToken`, `onLeaveChannel`, `onRejoinChannelSuccess`.] Reconnect timings (4 s to RECONNECTING, 20 s for a remote to be declared offline, 20 min to FAILED) come from Agora's reconnection FAQ [CITED: docs.agora.io/en/api-reference/faq/integration/reconnection] (that page is written for the Web SDK, MEDIUM confidence for Android).

**`call.ended` handling:** model it as `remote = Disconnected` (not a separate sticky status override). Then a later `onUserJoined` naturally restores `Active`, matching iOS where `didJoinedOfUid` flips the remote back to ready. Filter by `bookingId == session.bookingId`; ignore otherwise.

### Pattern 3: `startCall` sequence and ordering rules
1. Second call: enqueue guard dialog, return (D-12).
2. Set `session` (Connecting, expanded). The layer appears instantly (UI-SPEC).
3. `api.joinCall(id)`; any `ApiException` -> `abortOnJoinError(message)` (no `/end`).
4. `in_app_voice`: `agora_config == null` -> error "This booking isn't set up for in-app voice."; then `permissionGate.ensureMicrophone()`; then, only if the process is still foreground (`ProcessForeground.isForeground`), `foreground.startAndAwait(info)`; only after it returns true, `engine.join(...)`. Order is load-bearing: **permission -> startForeground -> engine create/join** (spike finding; a microphone-type FGS throws `SecurityException` without the grant on API 34+).
5. `skype_zoom`: `external_config.join_url` null/blank -> error "This booking is missing a meeting link. Contact the other party."; else store URL; no service, no permission.
6. Record `joinStartedAt` after a successful local join (iOS sets it after `joinChannel`), start the cutoff job at `startCall` (iOS `ensureTimerRunning`).

### Pattern 4: Permission gate hosted by the overlay
`CallCenter` exposes `permissionRequests: StateFlow<PendingPermission?>` where `PendingPermission` carries a `CompletableDeferred<MicResult>`. `CallOverlayHost` collects it and runs the UI-SPEC logic:

| State when Join is processed | Action |
|---|---|
| `RECORD_AUDIO` granted | complete `Granted` (still request `POST_NOTIFICATIONS` on 33+ only if not granted and not asked yet; never block) |
| not granted and `shouldShowRequestPermissionRationale(RECORD_AUDIO)` is true (previous denial) | show rationale dialog: Continue launches the request, Not now completes `Cancelled` (teardown, no dialog) |
| not granted, rationale false | launch `RequestMultiplePermissions` directly (first ask, or already permanently denied) |
| launcher result: `RECORD_AUDIO` granted | `Granted` |
| launcher result: denied and rationale now true | `DeniedOnce` (teardown, no dialog) |
| launcher result: denied and rationale false | `DeniedPermanently` -> enqueue the "Open Settings" dialog (`ACTION_APPLICATION_DETAILS_SETTINGS`, `Uri.fromParts("package", packageName, null)`) |

`shouldShowRequestPermissionRationale` needs the Activity (`LocalContext` walked to `Activity`, as `CustomTabLauncher.findActivity` does). Hold dialog flags in `rememberSaveable`. A pending `CompletableDeferred` survives configuration change because `CallCenter` is a singleton; the launcher re-registers via `rememberLauncherForActivityResult`. Add a timeout (about 60 s) that completes `Cancelled` so a user who backgrounds the app during the prompt does not leave a half-started session.

### Pattern 5: Thin foreground service + bridge
- `CallService` is `@AndroidEntryPoint`, `onStartCommand` handles `ACTION_START` (extras: peer name, `scheduledStartEpochMs`, bookingId; **no token, no channel**) and `ACTION_END` (calls `callCenter.endCall()`), returns `START_NOT_STICKY`.
- First thing in `ACTION_START`: `ServiceCompat.startForeground(this, ID, notification, FOREGROUND_SERVICE_TYPE_MICROPHONE)` then `bridge.onStarted()` which completes the `CompletableDeferred` that `startAndAwait` is suspended on (timeout about 3 s, returns false on `SecurityException`, `IllegalStateException` such as `ForegroundServiceStartNotAllowedException`, or timeout).
- Defensive: in `onCreate`/`ACTION_START`, if `callCenter.session.value == null`, `stopSelf()` (an orphaned service must never keep a notification).
- Observe `callCenter.session` (service-owned scope cancelled in `onDestroy`) to stop itself when the session becomes null and to re-post the notification once when the scheduled start passes ("Starts at {time}" -> chronometer).
- Do not override `onTaskRemoved` (D-14). `android:stopWithTask` is false by default, so the service survives a swipe [ASSUMED: platform default, covered by the D-16 UAT task-swipe step].
- Idempotent teardown from the spike is kept: `ServiceCompat.stopForeground(STOP_FOREGROUND_REMOVE)` then `stopSelf()`.
- Engine creation stays out of the service, so the Agora `RtcEngine` is created by `AgoraManager` (singleton) after `startAndAwait` returns.

### Pattern 6: End flow (CALL-08)
```kotlin
suspend fun endCall(forced: Boolean = false) {
    val s = session.value ?: return
    if (s.isEndingCall) return                      // forced cutoff fires once; double taps ignored
    update { it.copy(isEndingCall = true, callStatus = CallStatus.Ended) }
    cancelTimers()
    val reason = if (s.confirmSent) EndReason.Completed else EndReason.ManualExit
    val secs = s.joinStartedAt?.let { max(0, Duration.between(it, clock.instant()).seconds) } ?: 0
    withTimeoutOrNull(LEAVE_TIMEOUT_MS) { engine.leave() }          // peer hears us go immediately
    withTimeoutOrNull(END_REQUEST_TIMEOUT_MS) {                      // keep the FGS alive until /end finishes
        runCatching { api.endCall(s.bookingId, EndCallRequest(secs, reason.wire)) }   // never branch on status
            .onFailure { if (it is CancellationException) throw it }
    }
    engine.destroy(); foreground.stop()
    session.value = null; isMinimized.value = false
    if (forced) enqueue(CallDialog.TimeLimit)
}
```
`/end` is sent for both variants. A `409 INVALID_STATUS` (a `completed` reason on a booking that is not `in_progress`, for example when confirm-connection failed) is expected and ignored [CITED: backend-reference .planning/clickme-api-reference.md section 6.3]. `rejoin` is simply a new `startCall`; the iOS `isRejoin` parameter is obsolete after CALL-END-SEMANTICS and should not be ported.

### Pattern 7: Overlay host composition (CALL-04)
In `ClickMeApp` wrap the existing `AnimatedContent` and the suspended dialog in a `Box`, composing `CallOverlayHost()` last. Back handling:
```kotlin
// Compose the handler ONLY while expanded so it registers after every content handler (see Pitfall 2).
if (expanded) BackHandler { callCenter.minimize() }
```
While expanded, the content subtree gets `Modifier.clearAndSetSemantics {}` (stable) or `semantics { invisibleToUser() }` (verify it is not experimental in the BOM 2026.09 compose-ui; if experimental use `clearAndSetSemantics`) and the layer swallows pointer input. The pill is `Alignment.BottomEnd` with `navigationBars` inset + 96 dp. Note that Compose `Dialog`/`ModalBottomSheet` windows draw above the activity's root, so the pill and layer cannot sit above an open sheet or dialog (UI-SPEC says the pill sits above sheets; accept this limit, none of the three Join sites is inside a sheet).

### Pattern 8: Notification tap and `MainActivity`
Set `android:launchMode="singleTop"` on `MainActivity`. The content `PendingIntent` is `Intent(context, MainActivity).addFlags(FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_SINGLE_TOP).putExtra(EXTRA_EXPAND_CALL, true)` with `FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE`. Handle it in `onCreate` (cold, process alive after task swipe) and `onNewIntent` (warm): `if (intent.getBooleanExtra(EXTRA_EXPAND_CALL, false)) callCenter.expand()` then `intent.removeExtra(...)` so recreation does not re-expand. Inject `CallCenter` via `@Inject lateinit var` (the activity is already `@AndroidEntryPoint`). If `session == null` (process death, D-15) `expand()` is a no-op and the tap just opens the app.

### Pattern 9: Join window and clock (D-02, D-03)
```kotlin
fun isWithinJoinWindow(status: String, start: Instant, end: Instant, now: Instant): Boolean = when (status) {
    "pending_approval" -> false
    "completed" -> end.isAfter(now)
    else -> Duration.between(now, start).abs().seconds <= 3600
}
@Singleton class JoinClock @Inject constructor(clock: Clock, @ApplicationScope scope: CoroutineScope) {
    val now: StateFlow<Instant> = flow { while (true) { emit(clock.instant()); delay(30_000) } }
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), clock.instant())
}
```
Sites collect with `collectAsStateWithLifecycle()` so the ticking stops when no Join site is visible. The same `now` drives the "Starting now" badge (`|start - now| <= 3600 s`).

### Pattern 10: External link launcher (D-06)
```kotlin
fun openExternalMeeting(context: Context, uri: Uri, onFailure: () -> Unit) {
    if (!isAllowedMeetingUri(uri)) return onFailure()                // allow-list below
    val view = Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK.takeIf { context !is Activity } ?: 0)
    if (Build.VERSION.SDK_INT >= 30) view.addFlags(Intent.FLAG_ACTIVITY_REQUIRE_NON_BROWSER)
    try { context.startActivity(view) }                              // a native app claimed it
    catch (_: ActivityNotFoundException) {
        if (uri.scheme in setOf("http", "https")) context.openCustomTab(uri, onFailure) else onFailure()
    }
}
```
`FLAG_ACTIVITY_REQUIRE_NON_BROWSER` is API 30+ [VERIFIED: SDK 37 `api-versions.xml` `since="30"`]. Below API 30 it is ignored, so an `https` link opens in the browser instead of a Custom Tab on API 26 to 29 (acceptable; document it). A try/catch on `startActivity` avoids needing `<queries>` package-visibility entries.

### Anti-Patterns to Avoid
- **Engine or call state in a screen ViewModel or in the service's static fields** (the spike's `companion object` `MutableStateFlow` is spike-only). It would not survive navigation and cannot be unit tested.
- **A separate `Activity` for the call** (D-08 forbids it; also loses the pill-over-navigation behaviour).
- **Ending the call on `call.ended`, on `onTaskRemoved`, or on Back** (D-09, D-14, CALL-06).
- **Branching on the `/end` response `status`** (CALL-END-SEMANTICS: it is always the unchanged status).
- **Per-second notification updates.** Use `setUsesChronometer(true)` + `setWhen(...)`; re-post only once when the scheduled start passes.
- **Logging the RTC token, channel or uid.** The debug OkHttp logger is `BASIC` (no bodies) [VERIFIED: `debug/.../DebugNetwork.kt`]; keep it that way and never add `Log` of `agora_config`.
- **Creating the Agora engine before `startForeground`** (spike finding).

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Real-time voice transport, reconnection, jitter | Anything over WebRTC/sockets | Agora `RtcEngine` | Already contracted by the backend (token minting, channel = booking id) |
| Foreground start and type | Manual `startForeground` API-level branching | `ServiceCompat.startForeground(..., FOREGROUND_SERVICE_TYPE_MICROPHONE)` + `ContextCompat.startForegroundService` | Handles pre-29 signatures (minSdk 26) |
| Notification chronometer | A coroutine posting every second | `setUsesChronometer(true)`, `setWhen(scheduledStartMs)`, `setShowWhen(true)` | System-driven, no wake-ups |
| Audio focus | Deprecated `AudioManager.requestAudioFocus(listener, stream, hint)` | `AudioFocusRequest` (USAGE_VOICE_COMMUNICATION, CONTENT_TYPE_SPEECH, AUDIOFOCUS_GAIN) | minSdk 26 means the request-object API is always available; abandon with the same object |
| Opening a custom tab | New Custom Tabs plumbing | Existing `CustomTabLauncher.openCustomTab` | Already themed and allow-listed |
| Clipboard | `ClipboardManager` legacy calls | `LocalClipboard.current.setClipEntry(ClipEntry(...))` as the Phase 8 `MeetingLinkRow` already does | Same pattern, plus Android 13+ auto confirmation |
| Single-button error dialogs | New dialog composable | `AuthErrorDialog` (join error, guard, cutoff) | UI-SPEC reuse |
| Press feedback | Custom scale animation | `PressScaleButton` | UI-SPEC reuse |
| Time formatting of the schedule | `DateTimeFormatter` patterns | `formatSessionTimeRange` + `rememberDeviceTimeFormat` | Honors the device 12/24 hour setting |
| Wall clock in logic | `Instant.now()` / `System.currentTimeMillis()` | Injected `java.time.Clock` (`ClockModule.provideWallClock`) | Test seam (`MutableClock` exists) |
| Per-session teardown on logout | An observer in a lazily created singleton | `SessionDataCleaner` `@IntoSet` binding | The established pattern in `UserManager.logout` (see Pitfall 8) |

**Key insight:** every hard part (transport, reconnect, token expiry, FGS plumbing, chronometer, custom tabs) already has a platform or project solution. The only genuinely new logic is the small state machine and its ordering rules, which is why it belongs in plain Kotlin behind ports.

## Common Pitfalls

### Pitfall 1: Foreground service started from the background (Android 14 / 17)
**What goes wrong:** `startForegroundService` for a `microphone` service throws `SecurityException` (API 34+) or `ForegroundServiceStartNotAllowedException` (API 31+) if the app is not visible, and on Android 17 a service without while-in-use (WIU) capability also silences playback and makes audio-focus requests return `AUDIOFOCUS_REQUEST_FAILED`.
**Why it happens:** the Join flow is `tap -> POST /join -> permission prompt -> start service`. If the user backgrounds the app during those hundreds of milliseconds to seconds, the start is no longer from a visible state. Microphone is one of the "while-in-use" types [CITED: developer.android.com/develop/background-work/services/fgs/restrictions-bg-start]. Android 17 background-audio hardening applies to every app on Android 17, and to targetSdk 37 it additionally requires a WIU FGS when backgrounded; a FGS gets WIU if started in a user-initiated operation or while the app is visible [CITED: developer.android.com/about/versions/17/changes/bg-audio].
**How to avoid:** (a) start the service only after the mic grant, immediately before the engine, and check `ProcessForeground.isForeground` first; (b) wrap the start in try/catch for `SecurityException` and `IllegalStateException` and treat failure as a join error with no `/end`; (c) never restart the service from the background (no "recreate on process revival"); (d) the notification "End call" action only stops, it never starts. A service started while visible keeps its WIU capability for its lifetime.
**Warning signs:** `ForegroundServiceStartNotAllowedException` in logcat; `AudioHardening ... level: full` in `adb shell dumpsys audio`; no remote audio after backgrounding on API 37.

### Pitfall 2: Back handler priority (D-09)
**What goes wrong:** with the usual `BackHandler(enabled = expanded)` kept permanently composed, a screen composed later (for example the Home shell after Splash resolves, or a nav3 `NavDisplay` back handler) registers after it and takes Back first, so Back pops a screen under the call layer instead of minimizing.
**Why it happens:** `OnBackPressedDispatcher` invokes the most recently added enabled callback; toggling `enabled` does not reorder [ASSUMED: documented dispatcher semantics, confirm with a test].
**How to avoid:** compose the handler conditionally (`if (expanded) BackHandler {...}`) so it is added at expansion time, after all existing content. Add a Robolectric Compose test: a content screen with its own `BackHandler`, expand the call, dispatch back, assert minimized.
**Warning signs:** Back from the expanded call navigates the screen behind; works only when the call was started right after launch.

### Pitfall 3: Foreground notification is swipe-dismissible on Android 14+
**What goes wrong:** the approved notification (`setOngoing(true)`, `CATEGORY_CALL`) can be swiped away by the user on Android 14+. The call and service continue, but the shade loses the End call action and the way back into the app.
**Why it happens:** Android 14 made `FLAG_ONGOING_EVENT` dismissible; exceptions are `CallStyle`, DPC, media, and the Search Selector package. Still non-dismissible when the phone is locked or on "Clear all" [CITED: developer.android.com/about/versions/14/behavior-changes-all].
**How to avoid:** either accept it (the pill and launcher icon still return to the app), or use `NotificationCompat.CallStyle.forOngoingCall(Person, hangUpPendingIntent)` which is exempt (see Open Question 1). Keep the notification construction isolated in `CallNotificationFactory` so the choice is a one-class swap. Do not add a re-post loop in `deleteIntent`.
**Warning signs:** on the Samsung Android 16 device the notification disappears after a swipe while the mic indicator stays on.

### Pitfall 4: `ACTION_VIEW` on an https link always finds a browser
**What goes wrong:** "try `ACTION_VIEW`, fall back to a Custom Tab if nothing resolves" never reaches the fallback for `https://zoom.us/...`, because the default browser always resolves it. The user gets a plain browser tab, not a Custom Tab.
**How to avoid:** add `FLAG_ACTIVITY_REQUIRE_NON_BROWSER` on API 30+ so only a native app (Zoom, Teams, Skype, Meet) can claim it; otherwise `ActivityNotFoundException` -> Custom Tab. Treat an exception from the first attempt as the signal, not `resolveActivity` (package-visibility filtering returns null on API 30+ without `<queries>`).
**Security (V5/V12):** the URL is server data set by the expert. Allow-list schemes before any `ACTION_VIEW`: `http`, `https`, `skype`, `zoommtg`, `zoomus`, `msteams` (the iOS detect list). Reject `intent:`, `javascript:`, `file:`, `content:`, `android-app:`. Never use `Intent.parseUri`. `CustomTabLauncher` already rejects non-http(s).

### Pitfall 5: Client join window vs server join window vs token TTL
**What goes wrong:** D-02 enables Join within ±3600 s of start (or until `end` for `completed`). The backend gate differs: the API reference says `[start-10 min, end_time]`, but the code in `src/services/agora.js` is `[start-120 min, end+120 min]` with the comment "Widened ... to ease manual/pre-launch testing - tighten before launch" [VERIFIED: backend-reference/src/services/agora.js]. If the backend is tightened to the documented values, a user inside the client window but outside the server window gets `403 SESSION_NOT_ACTIVE` ("Session join window is not active.") and sees the join-error dialog. The token expires at `end + JOIN_GRACE + 10 min` (130 min today).
**How to avoid:** do not widen or narrow the client rule (D-02 is locked, server has final say). Map `SESSION_NOT_ACTIVE` to a friendlier string only if the server message is unclear (Open Question 3). Do not implement token renewal for this phase: with today's TTL the token outlives the 3600 s cutoff. If the TTL ever shrinks below `end + 3600 s`, the SDK reports `CONNECTION_CHANGED_TOKEN_EXPIRED` / `onTokenPrivilegeWillExpire`; the local-failed mapping in Pattern 2 then yields `Ended` and the user rejoins (a fresh `/join` mints a new token).
**Warning signs:** join errors only in prod after a backend tightening; calls dropping about 10 minutes after the scheduled end.

### Pitfall 6: The realtime socket is off while the app is backgrounded
**What goes wrong:** `RealtimeConnectionManager` connects only while the process is STARTED (`ProcessLifecycleOwner`). During a backgrounded call the socket is down, so `call.ended` is not received, and Socket.IO has no replay.
**How to avoid:** treat Agora `onUserOffline` as the primary peer-left signal (the peer's client leaves the channel before calling `/end`, and `/end` does not remove anyone from the channel), and `call.ended` as a faster hint while foregrounded. Optionally OR a keep-alive into `RealtimeConnectionManager.isDesired()` (`|| callCenter.isCallActive`); the code and `ProcessForeground` KDoc already anticipate this ("Phase 9 ORs a keep-alive input here"). Recommended but not required for any CALL requirement; it is a one-line change plus a test in `RealtimeConnectionManagerTest`.

### Pitfall 7: `call.ended` echo and stale events
**What goes wrong:** `call.ended` is emitted to both participants' rooms, including the user who just hung up [CITED: backend SOCKET-CONTRACT.md section 4]. Because the local `session` is already null (booking filter fails), your own echo is ignored. The residual race: the peer hangs up, rejoins within about a second, and the delayed `call.ended` lands after Agora's `onUserJoined`, leaving "Waiting for other person..." although both are in the channel.
**How to avoid:** accept as a known low-probability race (iOS has the same), or add a cheap guard: ignore a `call.ended` that arrives within about 2 s after the latest `RemoteJoined`. Record the decision in the plan; do not build a heartbeat.

### Pitfall 8: Forced logout mid-call cannot POST `/end`
**What goes wrong:** CONTEXT says "end the call before clearing the session", but the transport clears tokens synchronously before `SessionEvent.Rejected` is emitted, and `UserManager.logout()` clears tokens before running `SessionDataCleaner`s. A `/end` at that point would 401.
**How to avoid:** register `CallCenter` as a `SessionDataCleaner` (`@Binds @IntoSet`, like `PhotoWorkingFilesCleaner`) whose `clearSessionData()` does a local teardown: leave channel, destroy engine, stop the service, clear the session, no `/end` and no dialog. The booking stays live on the server and the next account/session can rejoin. User-initiated logout is guarded (D-13) so it never reaches this path with a live call. Beware a Hilt cycle: `CallCenter` must not inject `UserManager`.

### Pitfall 9: `completed` end-reason 409 and the confirm timing
**What goes wrong:** `end_reason = completed` on a booking that is not `in_progress` returns `409 INVALID_STATUS`. This happens if `confirm-connection` failed (network) but the local `confirmSent` flag is true.
**How to avoid:** ignore every `/end` error (CALL-END-SEMANTICS says never branch on it). Set `confirmSent = true` before the request, as iOS does, but note it makes the later end-reason `completed`; this is accepted ported behaviour. The body is `{connection_status: "connected", timestamp: ISO-8601, method: "in_app_voice" | "skype_zoom"}`; the iOS timestamp has no fractional seconds, so use `clock.instant().truncatedTo(ChronoUnit.SECONDS).toString()`. `confirm-connection` is idempotent on `in_progress` now, so it also fires on rejoin (new session object = new once-guard).

### Pitfall 10: Agora engine lifecycle
**What goes wrong:** `RtcEngine.destroy()` is a static, synchronized call that can block; calling it from an SDK callback or the main thread risks jank or deadlock. A second `RtcEngine.create` before the first `destroy` completes shares one native singleton.
**How to avoid:** `destroy()` after `leaveChannel` completes (await `onLeaveChannel` with a short timeout), on `Dispatchers.IO`, never inside a callback. There is also `RtcEngine.destroy(IRtcEngineReleaseCallback)` [VERIFIED: javap 4.6.4] if an async release is preferred. D-12 guarantees one call at a time, so no second `create` can race. Agora callbacks arrive on SDK threads: only write to thread-safe holders (`MutableStateFlow`, `trySend` on a `Channel`/`MutableSharedFlow`), never touch Compose or `CallCenter` fields directly. Never use `uid == 0` logic from iOS.

### Pitfall 11: Wall-clock cutoff under Doze / screen-off
**What goes wrong:** coroutine `delay` is based on `uptimeMillis`, which does not advance in deep sleep, so a single `delay(cutoff - now)` can fire late with the screen off.
**How to avoid:** loop with a bounded sleep and re-read the injected `Clock`: `while (active) { val left = Duration.between(clock.instant(), cutoff); if (left <= ZERO) { endCall(forced = true); break }; delay(min(left.toMillis(), 30_000)) }`. An active microphone FGS with an audio stream normally keeps the CPU awake, and the Agora token plus the server completion cron bound the session regardless, so a few seconds of lateness is acceptable. The cutoff must live in `CallCenter`, never in the UI ticker (the ticker stops when nothing is composed).

### Pitfall 12: Mic permission edge cases
**What goes wrong:** treating the first-time and permanently-denied cases the same (`shouldShowRequestPermissionRationale` is false in both before the first request); handling the launcher result in a composable that has been recreated; the request result when the user ignores the dialog.
**How to avoid:** use the decision table in Pattern 4, evaluate `shouldShowRationale` again inside the result callback, and make only `RECORD_AUDIO` decisive (notification denial never blocks, `BLUETOOTH_CONNECT` is not requested, see Open Question 4). Denial paths never call `/end` and never show the join-error dialog (separate signal, D-04).

### Pitfall 13: Notification and manifest details
**What goes wrong:** missing `FOREGROUND_SERVICE_MICROPHONE` (SecurityException on API 34+ for targetSdk 34+), channel created after `startForeground` ("Bad notification for startForeground"), `PendingIntent` without `FLAG_IMMUTABLE` (crash on targetSdk 31+), and `POST_NOTIFICATIONS` denied hiding the notification.
**How to avoid:** channel `ongoing_call` (IMPORTANCE_LOW) created in `Application.onCreate` and again in `CallService.onCreate`; all `PendingIntent`s `FLAG_IMMUTABLE`; the "End call" action uses `PendingIntent.getService` (not `getForegroundService`) because the service is already foreground. On API 33+ with notifications denied the FGS still runs and shows only in the Task Manager; the pill remains the in-app control [ASSUMED: platform behaviour, covered by the D-16 notification-denial step].

### Pitfall 14: Process death and OEM killers
**What goes wrong:** Samsung/other OEMs may stop the process from the Task Manager "Stop" action or battery management; the call and notification vanish.
**How to avoid:** this is accepted (D-15). Make sure nothing persisted refers to a call, and `CallService` uses `START_NOT_STICKY` so the system never revives a service without a session. Log "battery optimization / sleeping apps" as a UAT observation, do not add a battery-exemption request.

### Pitfall 15: Server-provided data shape
**What goes wrong:** `join_url` can be null with a sibling `message` ("No meeting link configured for this session yet."); `agora_config` and `external_config` are always present and exactly one is non-null; ids are strings (Moshi has no UUID adapter, project convention); `uid` is a 32-bit positive int.
**How to avoid:** model `JoinCallResponse(bookingId: String, connectionType: String, agoraConfig: AgoraConfig?, externalConfig: ExternalConfig?)`, `AgoraConfig(rtcToken, uid: Int, channelName)`, `ExternalConfig(joinUrl: String?, message: String?)`. Derive the branch from `connectionType` (not from which object is non-null) and then require the matching config.

## Code Examples

Verified patterns from the spike and the SDK jar.

### Manifest block for `:app`
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MICROPHONE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

<activity android:name=".MainActivity" android:launchMode="singleTop" ... />
<service android:name=".services.call.CallService"
    android:exported="false"
    android:foregroundServiceType="microphone" />
```
(Source: `spikes/agora/src/main/AndroidManifest.xml` plus the `launchMode` change. Agora's AAR adds no permissions of its own [VERIFIED: merged spike manifest].)

### Engine configuration (iOS parity, compiled in the spike against 4.6.4)
```kotlin
// Source: spikes/agora/.../CallService.kt (compiled unchanged against Agora 4.6.4)
val rtc = RtcEngine.create(RtcEngineConfig().apply {
    mContext = applicationContext; mAppId = BuildConfig.AGORA_APP_ID; mEventHandler = handler })
rtc.setAudioProfile(Constants.AUDIO_PROFILE_SPEECH_STANDARD)
rtc.setAudioScenario(Constants.AUDIO_SCENARIO_DEFAULT)
rtc.setDefaultAudioRoutetoSpeakerphone(true)
rtc.setEnableSpeakerphone(true)
val options = ChannelMediaOptions().apply {
    channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION
    clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
    publishMicrophoneTrack = true
    autoSubscribeAudio = true
}
rtc.joinChannel(rtcToken, channelName, uid, options)      // token/channel/uid come only from POST /join
// mute:    rtc.muteLocalAudioStream(true|false)
// speaker: rtc.setEnableSpeakerphone(true|false)          // iOS also resets playback volume to 100, which is the default
```

### Audio focus from the service, after `startForeground`
```kotlin
// Source: platform android.media.AudioFocusRequest (developer.android.com/media/optimize/audio-focus)
val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
    .setAudioAttributes(AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
    .setOnAudioFocusChangeListener { /* log only: no teardown, no UI (UI-SPEC) */ }
    .build()
audioManager.requestAudioFocus(request)   // abandon with audioManager.abandonAudioFocusRequest(request) on teardown
```
Whether Agora requests or manages focus itself on Android is not documented in what could be fetched [ASSUMED]. Requesting focus explicitly satisfies CALL-03 ("with audio focus") and pauses other media. On Android 17 the request must come from a WIU service, which it is here.

### Notification builder (approved UI-SPEC variant)
```kotlin
NotificationCompat.Builder(context, CHANNEL_ID)
    .setSmallIcon(R.drawable.ic_stat_call)                   // monochrome; the existing launcher icon is not a valid small icon
    .setContentTitle(title)                                  // "Call with {name}" or "ClickMe call"
    .setOngoing(true).setOnlyAlertOnce(true)
    .setCategory(NotificationCompat.CATEGORY_CALL)
    .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
    .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
    .setPublicVersion(publicNotification)                    // "ClickMe call in progress"
    .setContentIntent(openCallPendingIntent)
    .addAction(0, endLabel, endCallPendingIntent)            // PendingIntent.getService(ACTION_END), FLAG_IMMUTABLE
    .apply { if (started) setUsesChronometer(true).setWhen(startMs).setShowWhen(true) else setContentText(startsAt) }
    .build()
```
(`ic_stat_call` is a new asset the plan must add; CONTEXT/UI-SPEC say "app notification icon". A monochrome vector is required for status bar rendering.)

### Moshi models and endpoints
```kotlin
// ApiRequestUrls: BOOKING_JOIN = "bookings/{id}/join", BOOKING_CONFIRM_CONNECTION = "bookings/{id}/confirm-connection", BOOKING_END = "bookings/{id}/end"
@POST(ApiRequestUrls.BOOKING_JOIN) suspend fun joinCall(@Path("id") id: String): SuccessDataResponse<JoinCallResponse>   // no body
@PATCH(ApiRequestUrls.BOOKING_CONFIRM_CONNECTION) suspend fun confirmCallConnection(@Path("id") id: String, @Body body: ConfirmConnectionRequest): SuccessDataResponse<ConfirmConnectionData>
@POST(ApiRequestUrls.BOOKING_END) suspend fun endCall(@Path("id") id: String, @Body body: EndCallRequest): SuccessDataResponse<EndCallData>
// ConfirmConnectionRequest(@Json(name="connection_status") "connected", timestamp: String, method: String?)
// EndCallRequest(@Json(name="actual_duration") Int, @Json(name="end_reason") String)  // completed | manual_exit | dropped
```
`POST /join` takes no body; the Phase 7 note about explicit `{}` bodies applies only to `.strict()` schemas with a body, confirm the backend join route (`call.js`) before choosing Retrofit's default no-body POST [VERIFIED: backend-reference/src/controllers/call.controller.js reads only path param].

### Existing realtime hook
```kotlin
// RealtimeEvent.CallEnded(bookingId, endReason) already decodes; collect in CallCenter:
realtime.events.filterIsInstance<RealtimeEvent.CallEnded>()
    .filter { it.bookingId == session.value?.bookingId }
    .collect { markRemoteDisconnected() }
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Foreground service types optional | Types mandatory with per-type permissions (`FOREGROUND_SERVICE_MICROPHONE`) | Android 14 (API 34) | Manifest block above is required |
| WIU service could start in background without access | Creating a WIU (mic) FGS from the background throws `SecurityException` | Android 14 | Start only from visible app (Pitfall 1) |
| `setOngoing(true)` notifications non-dismissible | Dismissible, except `CallStyle`/media/DPC | Android 14 | Pitfall 3, Open Question 1 |
| Apps could request audio focus / play from background | Background audio hardening: needs visible activity or non-`SHORT_SERVICE` FGS; targetSdk 37 needs WIU FGS | Android 17 (API 37) | Verify on `Medium_Phone_API_37.0` with `adb shell cmd audio set-enable-hardening enable` and `dumpsys audio` |
| Local network open by default | Local network permission enforced for targetSdk 37 | Android 17 | Agora relays through its servers; LAN P2P candidates are optional. Low risk, include in UAT [CITED: developer.android.com/privacy-and-security/local-network-permission] |
| `PredictiveBackHandler` | Compose Multiplatform deprecates it for Navigation Event; the Android docs page still presents `PredictiveBackHandler` | CMP 1.10.x | Use plain `BackHandler` now; progress-driven slide is polish (UI-SPEC asks for it), verify the API in activity-compose 1.13 before using it |

**Deprecated/outdated:**
- iOS `5 * 60` overtime cutoff (TEMP value): use 3600 (CONTEXT).
- iOS `isRejoin` skip logic: obsolete after the 2026-08-16 backend change; do not port.
- iOS `didOfflineOfUid` with `uid == 0` as "local disconnected": never fires; replaced by connection-state mapping.
- Backend doc text "Join window `[start-10 min, end]`" and "confirm-connection 409 on non-`confirmed`": the code is wider and confirm is idempotent now (Pitfall 5, 9).

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | `OnBackPressedDispatcher` runs the most recently added enabled callback, and toggling `enabled` does not reorder | Pitfall 2 | If wrong, the permanent `BackHandler(enabled=...)` would work and the conditional form is merely harmless. Covered by a Robolectric test |
| A2 | `android:stopWithTask` default (false) keeps the FGS alive after a task swipe | Pattern 5 | Call would end on swipe; the UAT task-swipe step (D-16) catches it. Set `stopWithTask="false"` explicitly to remove the doubt |
| A3 | With `POST_NOTIFICATIONS` denied on 33+, the FGS still runs with no shade entry | Pitfall 13 | Plan might need a blocking notification prompt; UAT notification-denial step catches it |
| A4 | Agora does not request audio focus itself on Android; explicit `AudioFocusRequest` is needed (or harmless) | Code Examples | Double focus handling is harmless. If Agora already does it, the explicit request is redundant but valid for CALL-03 |
| A5 | Agora fires `onUserJoined` for a peer already in the channel right after the local join | Pattern 2 | The call would stay `waitingForPeer` when the peer joined first. iOS relies on the same behaviour, so this is expected; the Phase 2 two-device test (if run) confirms |
| A6 | An incoming cellular call or other focus loss needs no custom handling because the channel stays connected (UI-SPEC) | Interaction Contract | Mic may stay muted after a GSM call on some devices; UAT item |
| A7 | `Modifier.semantics { invisibleToUser() }` may be experimental in the pinned compose-ui; `clearAndSetSemantics {}` is the stable fallback | Pattern 7 | Build error if experimental opt-in is not allowed; trivial swap |
| A8 | Backend join window will be tightened before launch ("tighten before launch" comment) | Pitfall 5 | If it stays 120/120 there are no mismatched taps; if tightened, 403s appear for early taps (UX only) |
| A9 | The exact Android 14 `FLAG_ONGOING_EVENT` dismissal exemption applies to `CallStyle` built through `NotificationCompat` on API 34+ and keeps the FGS behaviour intact | Open Question 1 | The alternative stays UI-SPEC's plain builder |
| A10 | Task Manager "Stop" and OEM killers terminate the process without service callbacks | Pitfall 14 | D-15 already accepts process death |

## Open Questions (RESOLVED)

1. **Ongoing notification: plain builder vs `CallStyle`?**
   - What we know: UI-SPEC approves a plain builder (title "Call with {name}", chronometer, one "End call" action, `CATEGORY_CALL`). On Android 14+ that notification is swipe-dismissible; only `CallStyle` is exempt, and `CallStyle.forOngoingCall` gives a system "Hang up" button and a chronometer [CITED: Android 14 behavior changes].
   - What's unclear: whether the user prefers the exact approved copy over non-dismissibility.
   - Recommendation: implement `CallNotificationFactory` to the approved UI-SPEC and isolate it so switching to `CallStyle` is a single-file change; raise the question once at plan time (not a blocker). If the user chooses `CallStyle`, the title becomes the `Person` name and the action label is system-provided.
   - RESOLVED: ship the approved plain builder, isolated in `CallNotificationFactory` (09-07); `CallStyle` logged as an open item in 09-17.

2. **Keep the realtime socket alive during a backgrounded call?**
   - What we know: the socket drops when the process is not STARTED; Agora covers peer-left detection; the manager has an explicit seam for a keep-alive.
   - Recommendation: add `|| callCenter.isCallActive` to `isDesired()` (small, tested) so `booking.update` and `call.ended` keep arriving. Not needed for any CALL requirement; skip if schedule is tight.
   - RESOLVED: adopted in 09-11 (keep-alive while a call is active, behind an interface to avoid a Hilt cycle).

3. **`SESSION_NOT_ACTIVE` message.**
   - The server message is "Session join window is not active." The join-error dialog shows the server message. Recommendation: keep the server text for now; if UAT shows early taps are common, add a string-resource mapping. Related: the guard call-site strings. iOS uses "cancel this session" / "reschedule this session", the UI-SPEC lists "cancel this booking" / "reschedule this booking". Recommendation: follow the approved UI-SPEC and treat the iOS wording as the tiebreaker only if the user objects.
   - RESOLVED: show the server message as-is; guard copy follows the UI-SPEC ("cancel this booking", 09-02). Revisit after UAT.

4. **Bluetooth headset routing on Android 12+.**
   - UI-SPEC excludes a route picker; the Agora manifest adds no Bluetooth permission. Android 12+ typically needs the runtime `BLUETOOTH_CONNECT` permission for an app to use a Bluetooth headset route [ASSUMED]. Recommendation: do not request it this phase; log "Bluetooth headset routing" as a UAT observation. If it fails, add `BLUETOOTH_CONNECT` (non-blocking) to the permission request.
   - RESOLVED: no `BLUETOOTH_CONNECT` this phase; Bluetooth routing logged as a D-16 UAT observation in 09-17.

5. **Cancel guard placement.**
   - iOS guards at the cancellation *confirm* button; the UI-SPEC says "Upcoming Details Cancel Booking and the Cancel flow entry". Recommendation: guard at the Cancel entry on Upcoming Details and also at the final confirm in the cancel flow (cheap, mirrors iOS), so a call that starts mid-flow is still blocked.
   - RESOLVED: guard at both the Cancel entry (09-14) and the final confirm (09-16).

## Runtime State Inventory

> Not a rename/refactor phase, but it does delete a module (`:spikes:agora`) and add persisted-manifest state. Only the relevant categories are listed.

| Category | Items Found | Action Required |
|----------|-------------|------------------|
| Stored data | None. D-15 forbids persisting call state; no DataStore key is added | None |
| Live service config | Backend join window and Agora token TTL are server configuration (`JOIN_LEAD_MS`/`JOIN_GRACE_MS`, currently 120/120 min) | None in this repo; track the "tighten before launch" item as an open risk (Pitfall 5) |
| OS-registered state | Notification channel `ongoing_call` registered in the device NotificationManager; foreground-service declaration in the Play Console (Phase 17) | Create channel in code; Play declaration is Phase 17 |
| Secrets / env vars | `BuildConfig.AGORA_APP_ID` already defined per flavor in `app/build.gradle.kts`; spike `~/.clickme/spike-inputs.properties` is a local dev file | None; do not copy spike inputs into `:app` |
| Build artifacts | `:spikes:agora` module, its entry in `settings.gradle.kts` and in `.github/workflows/ci.yml` (`:spikes:agora:assembleDebug`, `check-design-rules.sh spikes`) | Remove `include(":spikes:stripe", ":spikes:agora")` agora part and the CI task for it once absorbed (D-16); keep `:spikes:stripe` untouched |

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| JDK 25 (Android Studio JBR) | Gradle build | ✓ | 25.0.3 at `/Applications/Android Studio.app/Contents/jbr/Contents/Home` (system `java` is 18, unusable) | Use `JAVA_HOME` override |
| Android SDK platform 37 | compileSdk 37 | ✓ | `platforms/android-37.0` | — |
| Android emulator AVD | Android 17 audio-hardening check | ✓ | `Medium_Phone_API_37.0` | — |
| Physical device (Samsung SM-A075F, API 36) | D-16 background-audio and notification UAT | ✗ (no device attached at research time) | — | Log as open item per project rule; emulator covers API 37 hardening and UI |
| iOS expert counterpart (finished iOS app + expert account + a booking) | End-to-end paid session | unknown | — | Log as open item; the Phase 2 spike plan used the same counterpart |
| `ctx7` CLI / Context7 MCP | Library docs | ✗ | — | WebFetch/WebSearch + local jar inspection (used) |
| `slopcheck` | Package legitimacy | ✗ | — | Not applicable (only the already-vetted Agora coordinate) |
| Network to Railway backend | Join/confirm/end integration | not probed | — | Unit tests use MockWebServer |

**Missing dependencies with no fallback:** none for planning. The device and the iOS counterpart are required only for the deferred D-16 UAT.
**Missing dependencies with fallback:** physical device (logged as open item); emulator for API 37.

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 4.13.2 + Robolectric 4.17 + Compose UI test (`ui-test-junit4`, BOM 2026.09.00) + `kotlinx-coroutines-test` 1.11.0 + `mockwebserver3` 5.5.0 |
| Config file | `app/build.gradle.kts` (`testOptions.unitTests.isIncludeAndroidResources = true`); no `src/androidTest` exists |
| Quick run command | `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDevDebugUnitTest --tests "com.uptrendinvestments.clickmeapp.managers.CallCenter*" --tests "com.uptrendinvestments.clickmeapp.managers.CallTimerTest" --tests "com.uptrendinvestments.clickmeapp.managers.JoinWindowTest"` |
| Full suite command | `JAVA_HOME=... ./gradlew testDevDebugUnitTest testProdDebugUnitTest ktlintCheck assembleDevDebug assembleProdDebug` plus `bash scripts/check-design-rules.sh`; run `lintDevDebug lintProdDebug` and the R8 build separately after `./gradlew --stop` (OOM at `-Xmx2048m`, see memory) |

Existing reusable fixtures: `testing/FakeClickMeApi` (add `joinCallHandler`, `confirmCallConnectionHandler`, `endCallHandler`), `testing/FakeRealtimeConnection` (`emit(RealtimeEvent.CallEnded(...))`), `testing/MutableClock`, `testing/MainDispatcherRule`, `FakeProcessForeground`. New fakes: `FakeCallEngine`, `FakeCallForegroundController`, `FakeCallPermissionGate`.

### Phase Requirements -> Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| CALL-01 | `isWithinJoinWindow` table (pending_approval false; completed until end; ±3600 s inclusive) | unit | `--tests "*JoinWindowTest"` | ❌ Wave 0 |
| CALL-01 | Join response decoding both shapes (agora, skype_zoom with null url + message); request path/method; confirm and end bodies | unit (MockWebServer) | `--tests "*ClickMeApiTest" --tests "*ResponseDecodingTest"` (extend) | ✅ extend |
| CALL-01 | Three Join sites enabled/disabled by window, tap calls `startCall` once, markers gone (`grep -r PHASE-9 app/src` empty) | Compose Robolectric + grep | `--tests "*JoinSites*"`; `! grep -rn "PHASE-9" app/src` | ❌ Wave 0 |
| CALL-01 | `ExternalLinkLauncher`: scheme allow-list, `REQUIRE_NON_BROWSER` flag set on API 30+, ActivityNotFound -> Custom Tab, native schemes never go to Custom Tab | unit (Robolectric shadows) | `--tests "*ExternalLinkLauncherTest"` | ❌ Wave 0 |
| CALL-01 | `ExternalMeetingProvider.detect` table | unit | `--tests "*ExternalMeetingProviderTest"` | ❌ Wave 0 |
| CALL-02 | Permission decision table (granted, rationale, permanent, cancelled); denial tears down with no `/end` and no join-error dialog; notification denial never blocks | unit (`FakeCallPermissionGate`) + Compose test for the dialogs | `--tests "*CallCenterPermissionTest" --tests "*CallDialogsTest"` | ❌ Wave 0 |
| CALL-03 | Manifest: service `foregroundServiceType` includes microphone, permissions declared; notification: channel IMPORTANCE_LOW, ongoing, CATEGORY_CALL, End-call action is IMMUTABLE service intent, public version present | unit (Robolectric `PackageManager.getServiceInfo`, `NotificationCompat` inspection) | `--tests "*CallManifestTest" --tests "*CallNotificationFactoryTest"` | ❌ Wave 0 |
| CALL-03 | Start order: permission -> foreground start -> engine join; failed foreground start = join error with no engine | unit | `--tests "*CallCenterStartTest"` | ❌ Wave 0 |
| CALL-03 | Audio actually flows in the background with screen off, 5+ min | manual (device, D-16) | n/a | manual-only: needs a real device, two parties and the OS FGS policy |
| CALL-04 | Overlay: expanded layer vs pill; minimize/expand; Back minimizes even over a screen with its own `BackHandler`; pill visible across route change; content hidden from semantics while expanded | Compose Robolectric | `--tests "*CallOverlayHostTest"` | ❌ Wave 0 |
| CALL-04 | Guard: `attempt` and `attemptModifyBooking` (matching vs other booking), second `startCall`, dialog queue order, wired at ModeSwitch / logout / add payment method / cancel | unit + Compose | `--tests "*CallCenterGuardTest" --tests "*GuardSitesTest"` | ❌ Wave 0 |
| CALL-05 | `confirm-connection` exactly once across `(Ready,Ready)` flapping; fires again for a new session (rejoin); 409 ignored; other errors do not interrupt; external variant fires only on Open, not on Copy | unit | `--tests "*CallCenterConfirmTest"` | ❌ Wave 0 |
| CALL-06 | `call.ended` same booking -> `waitingForPeer`, other booking ignored; `RemoteOffline` same; peer rejoin -> `active`; the call is never ended | unit (`FakeRealtimeConnection`) | `--tests "*CallCenterPeerTest"` | ❌ Wave 0 |
| CALL-07 | Timer: "Starts in M:SS" before start, elapsed after, final stretch `0 < remaining <= 300`, past end, chip text; cutoff at end + 3600 s fires once, shows time-limit dialog; amber thresholds | unit (pure `CallTimer`, virtual time + `MutableClock`) | `--tests "*CallTimerTest" --tests "*CallCenterCutoffTest"` | ❌ Wave 0 |
| CALL-08 | End order (leave before `/end`), always POST `/end`, errors ignored, reason `completed` vs `manual_exit`, `actual_duration` from local join, session cleared; notification action routes to `endCall`; logout cleaner tears down locally with no `/end`; user can `startCall` again afterwards | unit | `--tests "*CallCenterEndTest"` | ❌ Wave 0 |
| CALL-08 | Full paid session with an iOS expert, `confirm-connection` exactly once on the server | manual (D-16 UAT) | n/a | manual-only: needs the iOS app and a real booking |
| (cross) | Keep-alive OR in `RealtimeConnectionManager` (if adopted) | unit | `--tests "*RealtimeConnectionManagerTest"` (extend) | ✅ extend |
| (cross) | Design rules: no hex literals, no `fontSize`, strings only in `strings_call.xml` | script | `bash scripts/check-design-rules.sh` | ✅ |

### Sampling Rate
- **Per task commit:** the quick command above (the tests for the files touched), plus `ktlintCheck` for Kotlin changes.
- **Per wave merge:** `./gradlew testDevDebugUnitTest ktlintCheck assembleDevDebug` and `bash scripts/check-design-rules.sh`.
- **Phase gate:** full suite for both flavors green, lint + R8 build in a separate invocation, `:spikes:agora` removed and CI still green, before `/gsd-verify-work`. The D-16 device UAT is logged as an open item if no device is available.

### Wave 0 Gaps
- [ ] `app/src/test/.../testing/FakeCallEngine.kt`, `FakeCallForegroundController.kt`, `FakeCallPermissionGate.kt` - scriptable ports
- [ ] `FakeClickMeApi` handlers for `joinCall`, `confirmCallConnection`, `endCall`
- [ ] `managers/CallTimerTest.kt`, `JoinWindowTest.kt`, `CallCenter*Test.kt` (Start, Confirm, Peer, End, Cutoff, Guard, Permission)
- [ ] `screens/call/calling/CallOverlayHostTest.kt`, `CallDialogsTest.kt`, `ExternalMeetingProviderTest.kt`
- [ ] `services/call/CallNotificationFactoryTest.kt`, `CallManifestTest.kt`
- [ ] `ExternalLinkLauncherTest.kt`
- [ ] No framework install needed (everything is in the catalog). No `androidTest` source set is required; do not add one for this phase.

## Security Domain

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | no (reuses existing JWT + refresh) | `/join`, `/confirm-connection`, `/end` go through the existing authenticated OkHttp client |
| V3 Session Management | yes | Forced logout/suspension must tear the call down (`SessionDataCleaner` binding, Pitfall 8); no call state persisted (D-15) |
| V4 Access Control | yes | Server enforces participant check and join window; client never uses `actions.canJoin` and never trusts its own window for authorization |
| V5 Input Validation | yes | Allow-list URI schemes for the server-provided `join_url` before `ACTION_VIEW`; reject `intent:`, `javascript:`, `file:`, `content:`; Moshi typed decoding of the join response; never `Intent.parseUri` |
| V6 Cryptography | no (no custom crypto) | RTC token is minted and signed by the backend; the app never handles the Agora certificate |
| V8 Data Protection | yes | RTC token, channel and uid never logged (debug OkHttp logging is BASIC, no bodies); not placed in intent extras to the service; `VISIBILITY_PRIVATE` with a public notification version so the peer name is hidden on the lock screen |
| V12 Files / external resources | yes | Custom Tab/intent launch limited to http(s) and the known meeting schemes |
| V13 API | yes | Exact request bodies per backend schema (`actual_duration`, `end_reason`, `connection_status`) |

### Known Threat Patterns for Android voice-call stack

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Malicious `join_url` (expert-controlled) launching an arbitrary intent | Tampering / Elevation | Scheme allow-list, `CATEGORY_BROWSABLE`, no `parseUri`, never `startActivity` with a server-built `Intent` |
| RTC token leakage in logs, crash reports or intent extras | Information disclosure | No logging of `agora_config`; pass only display data to the service; token held only in memory inside `AgoraManager` |
| Exported service or implicit PendingIntent hijack | Elevation / Tampering | `android:exported="false"` on `CallService`; all `PendingIntent`s explicit and `FLAG_IMMUTABLE` |
| Microphone left on after a failed teardown | Information disclosure | Idempotent teardown (leave, destroy, stop FGS) on every exit path including forced logout and the cutoff; `START_NOT_STICKY` |
| Session continuing for another account after logout | Spoofing / Info disclosure | `SessionDataCleaner` local teardown |
| Notification exposing the peer name on the lock screen | Information disclosure | `VISIBILITY_PRIVATE` + public version "ClickMe call in progress" |
| Play policy: microphone FGS without mic use (external variant) | Compliance | No service for `skype_zoom` (D-07); declaration and video in Phase 17 |

## Sources

### Primary (HIGH confidence)
- Local code: `ios-reference/ClickMe2026/Managers/CallCenter.swift`, `AgoraManager.swift`, `Views/Screens/Call/Calling/*` (`MeetingCallViewModel`, `CallOverlayHost`, `ExternalMeetingProvider`, subviews), `Network/ClickMeAPI+Call.swift`, `Model/Responses/JoinCallResponse.swift`, `EndCallData.swift`, `Model/UIModels/UpcomingBooking.swift` - behaviour source of truth
- Local code: `app/src/main/**` (`ClickMeApp.kt`, `MainActivity.kt`, `ClickMeApplication.kt`, `realtime/*`, `managers/UserManager.kt`, `SessionDataCleaner.kt`, `di/*`, `components/web/CustomTabLauncher.kt`, the three Join sites), `spikes/agora/**` (`CallService.kt`, manifest), `gradle/libs.versions.toml`, `app/build.gradle.kts`, `.github/workflows/ci.yml`, `scripts/check-design-rules.sh`, test fakes under `app/src/test/.../testing/`
- Backend: `backend-reference/docs/ios/CALL-END-SEMANTICS.md`, `docs/SOCKET-CONTRACT.md` (section 4 `call.ended`), `src/controllers/call.controller.js`, `src/services/agora.js` (window 120/120, token TTL), `.planning/clickme-api-reference.md` sections 6.1 to 6.3
- Agora SDK 4.6.4 jar/AAR inspection (`javap` of `IRtcEngineEventHandler`, `Constants`, `RtcEngine`; `proguard.txt`; ELF alignment; POM dependencies) and Maven Central `maven-metadata.xml`
- Android SDK 37 `api-versions.xml` (`FLAG_ACTIVITY_REQUIRE_NON_BROWSER` since 30, `ForegroundServiceStartNotAllowedException` since 31, `FOREGROUND_SERVICE_MICROPHONE` since 34, `Notification.CallStyle` since 31)
- https://developer.android.com/about/versions/17/changes/bg-audio - Android 17 background audio hardening, WIU, testing commands
- https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start - while-in-use types, exemptions, Android 14 `SecurityException`
- https://developer.android.com/about/versions/14/behavior-changes-all - dismissible foreground notifications and the `CallStyle` exemption
- https://developer.android.com/develop/background-work/services/fgs/changes and `/timeout` - FGS type/permission requirements; microphone is not subject to the 6 h timeout (only `dataSync`/`mediaProcessing`)

### Secondary (MEDIUM confidence)
- https://docs.agora.io/en/api-reference/faq/quality/android_background - Agora foreground-service guidance (suggests `microphone|camera|mediaPlayback`; CONTEXT keeps `microphone`)
- https://docs.agora.io/en/api-reference/faq/integration/reconnection.md - connection-state timings (Web SDK page; Android timings assumed equal)
- https://developer.android.com/privacy-and-security/local-network-permission - local network protection for targetSdk 37

### Tertiary (LOW confidence)
- Agora Android audio-focus behaviour, Bluetooth permission need on Android 12+, and `PredictiveBackHandler` status in activity-compose 1.13 (no authoritative page retrieved; flagged A4, Open Question 4, State of the Art)

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - no new libraries; Agora 4.6.4 verified on Maven Central and already compiled in the spike
- Architecture: HIGH for the port-based `CallCenter`, state machine, end flow and join-window logic (derived from the iOS code and the backend contract). MEDIUM for the service/permission/Back interplay (platform behaviour, covered by tests plus the UAT)
- Pitfalls: HIGH for backend/iOS/local-code findings (verified in source); MEDIUM for Android 17 hardening effects and OEM behaviour (documented rules, no device proof yet, Phase 2 UAT #9 never ran)

**Research date:** 2026-10-24
**Valid until:** 2026-11-24 (30 days; re-check the backend join window/TTL and the Android 17 stable behaviour before release)
