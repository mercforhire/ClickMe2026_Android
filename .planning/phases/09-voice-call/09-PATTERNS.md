# Phase 9: Voice Call - Pattern Map

**Mapped:** 2026-10-24
**Files analyzed:** 34 new/modified
**Analogs found:** 31 / 34

Base path `B` = `app/src/main/java/com/uptrendinvestments/clickmeapp`. Test base `T` = `app/src/test/java/com/uptrendinvestments/clickmeapp`.

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|---|---|---|---|---|
| `B/managers/CallCenter.kt` | manager (singleton) | event-driven + request-response | `B/realtime/RealtimeConnectionManager.kt` (+ `B/managers/UserManager.kt`) | role-match |
| `B/managers/CallEngine.kt` (port + `EngineEvent`) | port/interface | event-driven | `B/realtime/ProcessForeground.kt` (seam interface) | role-match |
| `B/managers/AgoraManager.kt` | service (SDK wrapper) | streaming | `spikes/agora/.../CallService.kt` lines 57-187 | exact (absorb) |
| `B/managers/CallTimer.kt` | utility (pure) | transform | `B/managers/MonotonicClock.kt` (pure injected-clock) | partial |
| `B/managers/JoinWindow.kt` (+ `JoinClock`) | utility + singleton | transform / flow | `B/realtime/ProcessForeground.kt` (`stateIn` + `@ApplicationScope`) | role-match |
| `B/managers/ExternalLinkLauncher.kt` | utility | request-response (intent) | `B/components/web/CustomTabLauncher.kt` | exact |
| `B/services/call/CallService.kt` | service (FGS) | event-driven | `spikes/agora/.../CallService.kt` | exact (absorb) |
| `B/services/call/CallForegroundBridge.kt` | singleton bridge | event-driven | `B/realtime/ProcessForeground.kt` | partial |
| `B/services/call/CallNotificationFactory.kt` | utility | transform | spike `CallService.buildNotification` (lines 189-204) | role-match |
| `B/model/enums/ConnectionType.kt`, `EndReason.kt` | model enum | transform | `B/model/enums/MeetingType.kt` | exact |
| `B/model/requests/CallRequests.kt` (ConfirmConnection, EndCall) | model | CRUD | `B/model/requests/BookingActionRequests.kt` | exact |
| `B/model/responses/JoinCallResponse.kt`, `EndCallData.kt` | model | CRUD | `B/model/responses/BookingActionResponses.kt` | exact |
| `B/network/ApiRequestUrls.kt` + `ClickMeApi.kt` (3 endpoints) | config/api | request-response | `cancelClientBooking` in `ClickMeApi.kt` ~L237-244 | exact |
| `B/di/CallModule.kt` | config (Hilt) | - | `B/di/BookingModule.kt` | exact |
| `B/screens/call/calling/CallOverlayHost.kt` | component (root overlay) | event-driven | `B/ClickMeApp.kt` (suspended dialog host) | role-match |
| `B/screens/call/calling/MeetingCallScreen.kt` | component | request-response | `B/screens/clientmode/upcomingbooking/UpcomingBookingScreen.kt` | role-match |
| `B/screens/call/calling/MeetingCallViewModel.kt` | viewmodel (adapter) | event-driven | `B/screens/clientmode/upcomingbooking/UpcomingBookingViewModel.kt` | role-match |
| `B/screens/call/calling/CallDialogs.kt` | component | request-response | `B/screens/login/AuthErrorDialog` | exact |
| `B/screens/call/calling/subviews/*` (TopBar, Avatar, Info, Controls, EndButton, Pill, ExternalMeetingProvider) | component | request-response | `UpcomingBookingJoinCard.kt` (PressScaleButton, MeetingLinkRow) | role-match |
| `B/ClickMeApp.kt` (modify) | component | - | itself | exact |
| `B/MainActivity.kt` (modify: `onNewIntent`, expand extra) | activity | event-driven | itself | exact |
| `B/ClickMeApplication.kt` (modify: create channel) | config | - | itself | exact |
| `app/src/main/AndroidManifest.xml` (modify) | config | - | `spikes/agora/src/main/AndroidManifest.xml` | exact |
| 3 Join sites (modify): `UpcomingBookingJoinCard.kt`, `mybookings/subviews/UpcomingBookingCard.kt`, `explore/subviews/ExploreTodaysSessionsSection.kt` | component | request-response | each other | exact |
| Guard sites (modify): `settings/modeswitching/ModeSwitchScreen.kt`, `AppViewModel.logout`, `settings/paymentmethods/PaymentMethodsScreen.kt`/VM (`addCard`), `upcomingbooking/UpcomingBookingScreen.kt` (`onCancel`) | component/vm | request-response | each other | exact |
| `B/realtime/RealtimeConnectionManager.kt` (optional keep-alive) | manager | event-driven | itself L58-62 | exact |
| `res/values/strings_call.xml`, `res/drawable/ic_stat_call.xml` | resource | - | existing `strings_*.xml` | role-match |
| `settings.gradle.kts`, `.github/workflows/ci.yml`, `app/build.gradle.kts` | config | - | itself | exact |
| `T/testing/FakeCallEngine.kt`, `FakeCallForegroundController.kt`, `FakeCallPermissionGate.kt` | test fake | - | `T/testing/FakeProcessForeground.kt`, `FakeRealtimeConnection.kt` | role-match |
| `T/testing/FakeClickMeApi.kt` (extend) | test fake | - | `cancelClientBookingHandler` pattern | exact |
| `T/managers/CallCenter*Test.kt`, `CallTimerTest`, `JoinWindowTest` | test | - | existing `*Test` using `MainDispatcherRule` + `MutableClock` | role-match |

## Pattern Assignments

### `B/managers/CallCenter.kt` (manager, event-driven)

**Analog:** `B/realtime/RealtimeConnectionManager.kt` (Hilt singleton, `@ApplicationScope`, constructor injection, policy via `combine`/`collectLatest`).

**Imports/DI pattern** (RealtimeConnectionManager L1-52):
```kotlin
@Singleton
class RealtimeConnectionManager
    @Inject
    constructor(
        private val connection: RealtimeConnection,
        private val userManager: UserManager,
        ...
        @ApplicationScope private val scope: CoroutineScope,
        @IoDispatcher private val io: CoroutineDispatcher,
    ) {
```
Notes: `@ApplicationScope` scope is `Dispatchers.Default`, so launch `scope.launch(Dispatchers.Main.immediate)` for state transitions (RESEARCH Pattern 1). Subscribe to `RealtimeConnection.events.filterIsInstance<RealtimeEvent.CallEnded>()`. Do NOT inject `UserManager` (Hilt cycle; Pitfall 8). Inject `java.time.Clock` (from `ClockModule.provideWallClock`), `ClickMeApi`, `ProcessForeground`.

**Session cleaner pattern** (`B/managers/SessionDataCleaner.kt` L10-13 + `B/di/BookingModule.kt`):
```kotlin
fun interface SessionDataCleaner { suspend fun clearSessionData() }
// module:
@Binds @IntoSet abstract fun bindCompletedBookingCleaner(impl: DataStoreCompletedBookingStore): SessionDataCleaner
```
`CallCenter` implements `SessionDataCleaner` as local teardown with no `/end`.

**Core logic:** copy from RESEARCH Patterns 1, 2, 3, 6 (deriveStatus, startCall order permission -> startForeground -> engine join, endCall order leave -> `/end` -> destroy -> stop). Mirror iOS `ios-reference/ClickMe2026/Managers/CallCenter.swift` for copy strings.

---

### `B/managers/AgoraManager.kt` (service/SDK wrapper, streaming) and `B/services/call/CallService.kt` (FGS)

**Analog:** `spikes/agora/src/main/java/com/uptrendinvestments/clickmeapp/spike/agora/CallService.kt`

**Foreground start with type, before engine** (L149-161):
```kotlin
try {
    ServiceCompat.startForeground(this, NOTIFICATION_ID, buildNotification(),
        ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
} catch (e: SecurityException) {
    Log.e(TAG, "error: startForeground rejected ${e.javaClass.simpleName}")
    stopSelf(); return
}
```
**Engine config (iOS parity)** (L164-185): `RtcEngineConfig` + `setAudioProfile(AUDIO_PROFILE_SPEECH_STANDARD)`, `setAudioScenario(AUDIO_SCENARIO_DEFAULT)`, `setDefaultAudioRoutetoSpeakerphone(true)`, `setEnableSpeakerphone(true)`, `ChannelMediaOptions { channelProfile = CHANNEL_PROFILE_COMMUNICATION; clientRoleType = CLIENT_ROLE_BROADCASTER; publishMicrophoneTrack = true; autoSubscribeAudio = true }`, then `joinChannel(token, channel, uid, options)`. `mAppId = BuildConfig.AGORA_APP_ID` (resolve from `:app` BuildConfig).
**Event handler** (L57-112): `IRtcEngineEventHandler` overrides `onJoinChannelSuccess`, `onUserJoined`, `onUserOffline`, `onConnectionStateChanged`, `onError`. In `:app` convert them to `EngineEvent` via `trySend` into a `MutableSharedFlow`/`Channel` (SDK threads; do not touch state directly).
**Channel before startForeground** (L116-121) and **idempotent teardown** (L206-218): `ServiceCompat.stopForeground(this, STOP_FOREGROUND_REMOVE); stopSelf()`.
**Differences to apply:** engine lives in `AgoraManager` singleton, not in the service; remove the companion-object `MutableStateFlow` (spike-only); `@AndroidEntryPoint`; add `ACTION_END`; `START_NOT_STICKY` (L132, keep); no token or channel extras (L140-142 must NOT be copied); never log credentials; `RtcEngine.destroy()` off main after awaiting leave (Pitfall 10); do not override `onTaskRemoved`.

---

### `B/services/call/CallNotificationFactory.kt` (utility, transform)

**Analog:** spike `buildNotification()` L189-204 (`PendingIntent.FLAG_IMMUTABLE`, `NotificationCompat.Builder`, `setOngoing(true)`). Extend per RESEARCH "Notification builder" (chronometer, `CATEGORY_CALL`, public version, `PendingIntent.getService` End action, content intent to `MainActivity` with `EXTRA_EXPAND_CALL`). Replace `android.R.drawable.stat_sys_speakerphone` with new monochrome `ic_stat_call`. Create the channel (`IMPORTANCE_LOW`) in `ClickMeApplication.onCreate` and in `CallService.onCreate`.

---

### `B/managers/ExternalLinkLauncher.kt` (utility, intent)

**Analog:** `B/components/web/CustomTabLauncher.kt`

**Allow-list + fallback pattern** (L23-26, L33-69, L81-86):
```kotlin
internal fun isAllowedCustomTabUri(uri: Uri): Boolean {
    val scheme = uri.scheme?.lowercase()
    return (scheme == "https" || scheme == "http") && !uri.host.isNullOrBlank()
}
fun Context.openCustomTab(url: Uri, onFailure: () -> Unit) { ... }
private tailrec fun Context.findActivity(): Activity? = when (this) { is Activity -> this; is ContextWrapper -> baseContext.findActivity(); else -> null }
```
Fallback branch calls `context.openCustomTab(uri, onFailure)`. Add `FLAG_ACTIVITY_REQUIRE_NON_BROWSER` (API 30+) per RESEARCH Pattern 10; extend scheme allow-list to `skype`, `zoommtg`, `zoomus`, `msteams`. Use `if (!isActivity) addFlags(FLAG_ACTIVITY_NEW_TASK)` as in L56/L62. Note `findActivity` is `private` in that file; either widen it to `internal` or duplicate.

---

### `B/managers/JoinWindow.kt` + `JoinClock` (utility, transform / flow)

**Analog:** `B/realtime/ProcessForeground.kt` L20-48 for `@Singleton` + `@ApplicationScope scope` + `stateIn(scope, SharingStarted..., initial)`. Use `SharingStarted.WhileSubscribed(5_000)` and RESEARCH Pattern 9 code. Inject `java.time.Clock`.

---

### `B/model/enums/ConnectionType.kt`, `EndReason.kt` (enum)

**Analog:** `B/model/enums/MeetingType.kt` (entire file):
```kotlin
enum class MeetingType(val wire: String) {
    InAppVoice("in_app_voice"), SkypeZoom("skype_zoom");
    companion object {
        private val byWire: Map<String, MeetingType> = entries.associateBy { it.wire }
        fun fromWire(raw: String?): MeetingType? = raw?.let(byWire::get)
    }
}
```
`ConnectionType` can reuse `MeetingType` wire values (`in_app_voice`, `skype_zoom`); `EndReason`: `completed`, `manual_exit`, `dropped`.

---

### `B/model/requests/CallRequests.kt` and `B/model/responses/JoinCallResponse.kt` / `EndCallData.kt`

**Analogs:** `B/model/requests/BookingActionRequests.kt` and `B/model/responses/BookingActionResponses.kt`. Both start with `@file:Suppress("ktlint:standard:filename")` when multi-class.
```kotlin
@JsonClass(generateAdapter = true)
data class AcceptRescheduleRequest(
    @Json(name = "stripe_payment_method_id") val stripePaymentMethodId: String? = null,
)
```
Apply `@Json(name = "connection_status"/"actual_duration"/"end_reason"/"agora_config"/"external_config"/"join_url"/"rtc_token"/"channel_name")`. Ids are `String`. Null is omitted by the converter. KDoc each class with the endpoint, as here.

---

### `B/network/ClickMeApi.kt` + `ApiRequestUrls.kt`

**Analog:** `cancelClientBooking` (`ClickMeApi.kt` ~L236-244) and constants (`ApiRequestUrls.kt` L53-55):
```kotlin
/** Ports iOS ClickMeAPI+Booking cancelClientBooking. ... */
@POST(ApiRequestUrls.CLIENT_BOOKING_CANCEL)
suspend fun cancelClientBooking(
    @Path("id") bookingId: String,
    @Body body: CancelBookingRequest,
): SuccessDataResponse<CancelBookingData>
```
```kotlin
const val CLIENT_BOOKING_CANCEL = "client/bookings/{id}/cancel"
```
Add `BOOKING_JOIN = "bookings/{id}/join"`, `BOOKING_CONFIRM_CONNECTION` (`@PATCH`), `BOOKING_END`. `joinCall` has no `@Body`. Update the header KDoc (L57-60) to mention the Phase 9 call group. Extend `FakeClickMeApi` in the same shape (handler `var`, call counter, request list, override at ~L441-448).

---

### `B/di/CallModule.kt` (Hilt config)

**Analog:** `B/di/BookingModule.kt` (abstract `@Module @InstallIn(SingletonComponent::class)` with `@Binds`, `@Binds @IntoSet` for `SessionDataCleaner`). Bind `CallEngine <- AgoraManager`, `CallForegroundController <- CallForegroundBridge`, `SessionDataCleaner <- CallCenter`.

---

### `B/screens/call/calling/CallOverlayHost.kt` + `B/ClickMeApp.kt` (modify)

**Analog:** `B/ClickMeApp.kt` L31-81. Current structure is a bare `AnimatedContent(...)` followed by `if (showSuspendedDialog) SuspendedAccountDialog(...)`. Wrap both in a `Box(Modifier.fillMaxSize())` and compose `CallOverlayHost()` last (RESEARCH Pattern 7). State collection pattern to copy:
```kotlin
val route by appViewModel.route.collectAsStateWithLifecycle()
val showSuspendedDialog by appViewModel.showSuspendedDialog.collectAsStateWithLifecycle()
val context = LocalContext.current
```
Hosting dialogs: same root-level `if (flag) Dialog(...)` approach as the suspended dialog. Compose `BackHandler` only inside `if (expanded)`. Mic launcher: `rememberLauncherForActivityResult(RequestMultiplePermissions())`.

### `B/MainActivity.kt` (modify)

**Analog:** itself (L18-41), `@AndroidEntryPoint`, `ComponentActivity`. Add `@Inject lateinit var callCenter: CallCenter`, override `onNewIntent`, and handle `EXTRA_EXPAND_CALL` in `onCreate` and `onNewIntent` (RESEARCH Pattern 8). Manifest gets `android:launchMode="singleTop"` (current `<activity>` has none).

### `AndroidManifest.xml` (modify)
**Analog:** `spikes/agora/src/main/AndroidManifest.xml` (permissions + `<service ... foregroundServiceType="microphone"/>`). Current `:app` manifest has only `INTERNET`. Use RESEARCH "Manifest block"; add `android:stopWithTask="false"` explicitly.

### `B/ClickMeApplication.kt` (modify)
**Analog:** itself (L19-37): `@Inject lateinit var` fields and `onCreate` setup. Add the notification channel creation here.

---

### `B/screens/call/calling/MeetingCallViewModel.kt` (viewmodel)

**Analog:** `B/screens/clientmode/upcomingbooking/UpcomingBookingViewModel.kt` L33-70: `@HiltViewModel`, `MutableStateFlow` + `asStateFlow()`, injected `Clock`, `RealtimeConnection`. Here it should be a thin adapter over `CallCenter.session` (no logic duplication); expose timer ticks via a UI ticker using `Clock` (the cutoff stays in `CallCenter`). Event filter pattern in the analog: `realtime.events.filterIsInstance<RealtimeEvent...>().filter { it.bookingId == ... }`.

### `B/screens/call/calling/CallDialogs.kt`

**Analog:** `AuthErrorDialog(message, onDismiss, title, selectable)` in `B/screens/login/` (single OK button, `surfaceContainerHigh`, `extraLarge` shape). Reuse it directly for join error, guard and cutoff dialogs. Mic-denied "Open Settings" and rationale dialogs: copy its `AlertDialog` shape with two buttons.

### Subviews (pill, controls, etc.)

**Analog:** `UpcomingBookingJoinCard.kt` L75-140: `Surface` with `MaterialTheme.shapes.medium`, `PressScaleButton(onClick, enabled, modifier...)` with `heightIn(min = JoinButtonMinHeight)`, `Icon` + `Spacer(Spacing.sm)` + `Text(stringResource(...))`; `MeetingLinkRow` uses `LocalClipboard.current` + `rememberCoroutineScope` + `copied` flag reset via `LaunchedEffect(copied) { delay(COPIED_RESET_MS) }` (reuse for "Link copied"). Rules: theme colors only (no hex), `Spacing.*`, typography from `MaterialTheme.typography`, strings in `strings_call.xml`.

---

### The three Join sites (modify) - D-01

All three currently contain the same disabled button with a `PHASE-9` marker; replace `onClick = {}` / `enabled = false`, remove the marker, add `isWithinJoinWindow(...)` from `JoinClock.now`, and call `callCenter.startCall(...)`:

- `screens/clientmode/upcomingbooking/subviews/UpcomingBookingJoinCard.kt` L91-109 (`DisabledJoinButton`, marker L93). Keep the `MeetingLinkRow` branch only if D-05 does not replace it; the card currently shows the link row for `SkypeZoom`.
- `screens/clientmode/mybookings/subviews/UpcomingBookingCard.kt` L220-245 (`JoinPlaceholder`, marker L222).
- `screens/clientmode/explore/subviews/ExploreTodaysSessionsSection.kt` L137-150 (inline, marker L138).
- Doc comment at `UpcomingBookingScreen.kt` L97 mentions Phase 9; update it.

Common excerpt to modify:
```kotlin
val joinState = stringResource(R.string.explore_join_call_state)
// PHASE-9: join window + click (D-02, D-12)
PressScaleButton(
    onClick = {},
    enabled = false,
    modifier = Modifier.fillMaxWidth().heightIn(min = JoinButtonMinHeight)
        .testTag(MY_BOOKINGS_JOIN_TAG).semantics { stateDescription = joinState },
)
```
Keep test tags (`UPCOMING_BOOKING_JOIN_TAG`, `MY_BOOKINGS_JOIN_TAG`, `TODAYS_SESSION_JOIN_TAG`). Inject `CallCenter` into the owning ViewModel (not into the composable) and pass `onJoin` down, matching how `onCancel` is threaded in `UpcomingBookingScreen.kt` L106-124, L260, L327.

### Guard sites (modify) - D-13
- `screens/settings/modeswitching/ModeSwitchScreen.kt` L36, L59: `if (mode != currentMode) onSwitchMode(mode)` -> wrap with `callCenter.attempt("switch modes") { ... }`.
- `AppViewModel.kt` L154-156: `fun logout() { viewModelScope.launch { userManager.logout() } }` -> guard.
- `screens/settings/paymentmethods/PaymentMethodsScreen.kt` L101: `onAddCard = viewModel::addCard` -> guard in the ViewModel `addCard`.
- `UpcomingBookingScreen.kt` `onCancel` (L106, L327) -> `attemptModifyBooking(bookingId, "cancel this booking")`; also the cancel flow's final confirm (RESEARCH Open Question 5). Use the UI-SPEC wording.

### `RealtimeConnectionManager.kt` (optional keep-alive)
L58-62 `combine(userManager.isLoggedIn, foreground.isForeground) { loggedIn, fg -> loggedIn && fg }`: OR in `callCenter.isCallActive` (beware cyclic injection with `CallCenter`; use a small `CallKeepAlive` flow interface instead of the class).

## Shared Patterns

### Hilt singleton + application scope
**Source:** `B/realtime/ProcessForeground.kt` L20-48, `B/di/Qualifiers.kt` (`@ApplicationScope`, `@IoDispatcher`). **Apply to:** `CallCenter`, `JoinClock`, `AgoraManager`, `CallForegroundBridge`.

### Injected wall clock
**Source:** `B/di/ClockModule.kt` `provideWallClock(): Clock = Clock.systemUTC()`; tests use `T/testing/MutableClock`. **Apply to:** `CallCenter`, `CallTimer`, `JoinClock`, `MeetingCallViewModel`. Never call `Instant.now()`.

### Session-scoped cleanup
**Source:** `SessionDataCleaner` + `@Binds @IntoSet` (`B/di/BookingModule.kt`). **Apply to:** `CallCenter` (local teardown, no `/end`).

### Error dialog
**Source:** `AuthErrorDialog` (`B/screens/login/`). **Apply to:** join error, guard, time-limit dialogs.

### API errors
**Source:** `B/network/ApiException` (used in `RealtimeConnectionManager` and `UpcomingBookingViewModel` via `toSectionMessage()` in `screens/clientmode/makeabooking`). **Apply to:** `joinCall` failure -> message for the join-error dialog; ignore all `/end` errors; ignore 409 on confirm-connection.

### Test infrastructure
**Source:** `T/testing/MainDispatcherRule.kt`, `MutableClock.kt`, `FakeClickMeApi.kt` (handler-var + call counter + request list), `FakeRealtimeConnection.kt` (`emit(RealtimeEvent...)`), `FakeProcessForeground.kt`. **Apply to:** all new `CallCenter*Test`; new `FakeCallEngine` etc. follow the same scriptable-fake shape.

### CI design rules
No `Color(0x...)` literals, no `*Brand` types, no `fontSize` literals; strings in `strings_call.xml`; ktlint (trailing commas, multiline constructor style as in the analogs).

## No Analog Found

| File | Role | Data Flow | Reason |
|---|---|---|---|
| `CallForegroundBridge.kt` (start/awaitStarted) | bridge | event-driven | No `CompletableDeferred` service bridge exists; use RESEARCH Pattern 5 |
| `CallOverlayHost` permission gate (`CompletableDeferred<MicResult>` + launcher) | component | event-driven | No runtime-permission flow exists in `:app`; use RESEARCH Pattern 4 |
| `CallTimer.kt` formatting helpers | utility | transform | No call timer; use RESEARCH Pattern 11 plus `formatSessionTimeRange`/`rememberDeviceTimeFormat` for schedule text |

## Metadata

**Analog search scope:** `app/src/main/java/.../clickmeapp/**`, `app/src/test/**`, `spikes/agora/**`.
**Files scanned:** ~35 read or grepped.
**Pattern extraction date:** 2026-10-24
