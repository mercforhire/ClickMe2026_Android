# Phase 13: Push, Notifications & Internal Testing - Pattern Map

**Mapped:** 2026-10-28
**Files analyzed:** 27 (new/modified)
**Analogs found:** 25 / 27

All paths are relative to `app/src/main/java/com/uptrendinvestments/clickmeapp/` unless stated. All analogs below are git-tracked source.

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|---|---|---|---|---|
| `screens/push/PushLaunchRequest.kt` | store (hand-off) | event-driven | `screens/chat/ChatLaunchRequest.kt` | exact |
| `screens/push/ShellRouteRequest.kt` | store (hand-off) | event-driven | `screens/expertmode/ExpertLaunchRequest.kt` (`OneShotSlot`) | exact |
| `screens/push/PushEvent.kt` / `PushDestination.kt` (router) | utility (pure) | transform | `MainActivity.kt` `consumeExpandCallExtra` + `MainActivityIntentTest.kt` (parse + test style) | role-match |
| `screens/push/PushDestinationResolver.kt` | service | request-response | `screens/clientmode/mybookings/ClientBookingsRepository.kt` (`detail`) | partial |
| `screens/push/PushRoutingViewModel.kt` / `PushRoutingHost.kt` | viewmodel + root composable | event-driven | `screens/clientmode/mybookings/MyBookingsViewModel.kt` (consumes `BookingsLaunchRequest` in `init` and on emission) | role-match |
| `screens/push/PushPermissionRequester.kt` | component | event-driven | `screens/call/calling/MicPermissionRequester.kt` | exact |
| `managers/FcmPushTokenRegistrar.kt` | service | request-response | `managers/PushTokenRegistrar.kt` (base) | exact |
| `managers/PushTokenRegistrar.kt` (modify) | service | request-response | itself | exact |
| `managers/PushRegistrationManager.kt` | app-scoped manager | event-driven | `realtime/RealtimeConnectionManager.kt` + `realtime/ProcessForeground.kt` | exact |
| `managers/PushDeviceId.kt`, `PushPermissionGate.kt` | store | CRUD | `managers/SessionStore` (but NOT cleared on logout) | partial |
| `di/PushModule.kt` | config (Hilt) | n/a | `di/ChatModule.kt` | exact |
| `services/push/ClickMeMessagingService.kt` | service (Android) | event-driven | `services/call/CallService.kt` (`@AndroidEntryPoint`) | role-match |
| `services/push/PushNotificationFactory.kt`, `NotificationChannels.kt` | utility | transform | `services/call/CallNotificationFactory.kt` | exact |
| `services/push/PushMessageHandler.kt` | plain class | event-driven | `screens/chat/ChatSessionState.kt` (`openThreadId`) | partial |
| `MainActivity.kt` (modify) | activity | event-driven | itself (`onCreate`/`onNewIntent`/`consumeExpandCallExtra`) | exact |
| `navigation/Navigator.kt` (add `openRouted`) | utility | transform | `Navigator.openChatThread` / `navigateInTab` | exact |
| `network/ApiRequestUrls.kt`, `network/ClickMeApi.kt` (modify) | config/API | request-response | `CHATS_*`, `USER_REPORT` entries | exact |
| `model/requests/RegisterDeviceTokenRequest.kt` | model | request-response | `model/requests/ChatRequests.kt` (strict schema DTOs) | exact |
| `model/responses/NotificationItem(s)`, `ReviewContextData`, `SubmitReviewData` | model | request-response | `model/responses/ChatsPayload` / `ClientBookingDetail` | role-match |
| `screens/notifications/*` (Key, Screen, ViewModel, UiState) | screen | CRUD + pull-to-refresh | `screens/clientmode/mybookings/MyBookingsScreen/ViewModel` | role-match |
| `screens/clientmode/writereview/*` | screen | request-response | `screens/clientmode/bookingsummary/*` | role-match |
| `screens/clientmode/reviews/ReviewsRepository.kt` | repository | request-response | `ClientBookingsRepository.kt` | exact |
| `screens/clientmode/reviews/ReviewLaunchRequest.kt` | store | event-driven | `screens/clientmode/mybookings/BookingsLaunchRequest.kt` | exact |
| `PastBookingCard.kt`, `BookingSummaryScreen/UiState/ViewModel` (modify) | component | CRUD | themselves (carry "Phase 13" comments) | exact |
| `DashboardHeader.kt` / `ExpertDashboardScreen.kt`, `ExploreScreen.kt` (bell) | component | event-driven | themselves | exact |
| `app/build.gradle.kts`, `gradle/libs.versions.toml`, `AndroidManifest.xml` | config | n/a | RESEARCH.md "Installation" (verified probe) | no in-repo analog |
| `backend-reference/src/services/push.js` (cross-repo) | service | pub-sub | Phase 11 G-11-6 backend task | process analog |
| Tests (`*Test.kt`) | test | n/a | `ChatLaunchRequestTest`, `ChatsRepositoryTest`, `MainActivityIntentTest` | exact |

## Pattern Assignments

### `screens/push/PushLaunchRequest.kt` and `ShellRouteRequest.kt` (store, event-driven)

**Analog:** `screens/chat/ChatLaunchRequest.kt` (lines 1-48) and `screens/expertmode/ExpertLaunchRequest.kt` (lines 1-26, `OneShotSlot`).

```kotlin
// ExpertLaunchRequest.kt:13-26 - reuse this class, do not re-implement
class OneShotSlot<T : Any> {
    private val _pending = MutableStateFlow<T?>(null)
    val pending: StateFlow<T?> = _pending.asStateFlow()
    fun post(value: T) { _pending.value = value }          // newest wins (D-15)
    fun consume(): T? = _pending.getAndUpdate { null }
}
// ChatLaunchRequest.kt:41-47
@Singleton
class ChatLaunchRequest @Inject constructor() {
    val thread = OneShotSlot<ChatThreadKey>()
}
```
Import `com.uptrendinvestments.clickmeapp.screens.expertmode.OneShotSlot`. Make `PushLaunchRequest` also a `SessionDataCleaner` bound via `@Binds @IntoSet` (see `ChatModule`). Add an `arrivals` tick (MutableSharedFlow/StateFlow counter) for D-12.

### `di/PushModule.kt` (Hilt)

**Analog:** `di/ChatModule.kt` lines 1-40.
```kotlin
@Module @InstallIn(SingletonComponent::class)
abstract class ChatModule {
    @Binds @IntoSet
    abstract fun bindChatSessionCleaner(impl: ChatSessionState): SessionDataCleaner
    companion object {
        @Provides fun currentUserIdProvider(userManager: UserManager): CurrentUserIdProvider = ...
    }
}
```
Use `@Provides fun pushTokenRegistrar(impl: FcmPushTokenRegistrar): PushTokenRegistrar` in the companion, plus `@Binds @IntoSet` for `PushLaunchRequest` and the reviewed-ids set.

### `managers/PushTokenRegistrar.kt` (modify) and `FcmPushTokenRegistrar.kt`

**Analog:** `managers/PushTokenRegistrar.kt` lines 11-14 (whole file).
```kotlin
open class PushTokenRegistrar @Inject constructor() {
    open suspend fun unregister() {}
}
```
Per RESEARCH F-4: keep as the no-op base, DROP `@Inject` (bound through the module instead), add `open suspend fun register() {}`, and do NOT add constructor params (tests construct `PushTokenRegistrar()` and `RecordingRegistrar : PushTokenRegistrar()`). `FcmPushTokenRegistrar @Inject constructor(api: ClickMeApi, tokens: SecureTokenStore, ...) : PushTokenRegistrar()`. `UserManager.logout` already runs `runCatching { pushTokenRegistrar.unregister() }` before `tokens.clear()` (UserManager.kt:195).

### `managers/PushRegistrationManager.kt` (app-scoped, event-driven)

**Analog:** `realtime/RealtimeConnectionManager.kt` lines 36-70 (class header, `started` guard, `start()`), started from `ClickMeApplication.onCreate`; `ProcessForeground.isForeground` (ProcessForeground.kt:14-17).
```kotlin
@Singleton
class RealtimeConnectionManager @Inject constructor(
    private val userManager: UserManager,
    private val foreground: ProcessForeground,
    @ApplicationScope private val scope: CoroutineScope,
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    private val started = AtomicBoolean(false)
    fun start() { if (!started.compareAndSet(false, true)) return /* combine(...) collect */ }
}
```
Copy the idempotent `start()` + `@ApplicationScope` scope; use `combine(userManager.isLoggedIn, foreground.isForeground)`. No logging (runs in plain JVM tests).

### `services/push/NotificationChannels.kt` and `PushNotificationFactory.kt`

**Analog:** `services/call/CallNotificationFactory.kt` lines 28-47 and imports 1-14.
```kotlin
object CallNotificationFactory {
    const val CHANNEL_ID = "ongoing_call"
    fun createChannel(context: Context) {
        val channel = NotificationChannelCompat
            .Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_LOW)
            .setName(context.getString(R.string.call_notification_channel_name))
            .setDescription(context.getString(R.string.call_notification_channel_description))
            .build()
        NotificationManagerCompat.from(context).createNotificationChannel(channel)
    }
}
```
Use ids `bookings`, `sessions`, `messages`, `account_payouts`, `general` (fixed in D-04/UI-SPEC). Tap `PendingIntent`: `CLEAR_TOP | SINGLE_TOP` with `FLAG_IMMUTABLE` (as `openIntent` in this file), request code = stable notification id with `FLAG_UPDATE_CURRENT`. Strings go in `strings.xml` (names/descriptions), not hard-coded. Leave the `ongoing_call` channel untouched.

### `services/push/ClickMeMessagingService.kt`

**Analog:** `services/call/CallService.kt` lines 41-42 (`@AndroidEntryPoint` service injection works). Keep thin: map `RemoteMessage` to `PushMessageHandler`. For D-13 read `screens/chat/ChatSessionState.openThreadId` (line ~35; compare `ignoreCase = true` like `onThreadHidden`).

### `screens/push/PushPermissionRequester.kt` (composable)

**Analog:** `screens/call/calling/MicPermissionRequester.kt` lines 1-60 (imports; `rememberLauncherForActivityResult`, `ActivityResultContracts`, pure decision functions, `permissionsToRequest(sdkInt, notificationsGranted)` already references `POST_NOTIFICATIONS` at line ~60).
```kotlin
fun permissionsToRequest(sdkInt: Int, notificationsGranted: Boolean): Array<String> =
    if (sdkInt >= Build.VERSION_CODES.TIRAMISU && !notificationsGranted)
        arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.POST_NOTIFICATIONS) else ...
```
Mirror the "pure decision function + thin composable" split so the gate logic is JVM-testable. Host next to `CallOverlayHost` in `ClickMeApp`. Persist `asked` outside the session-cleared store (`SessionStore.clear()` wipes everything).

### `MainActivity.kt` (modify)

**Analog:** itself, lines 25-70.
```kotlin
override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    if (intent.consumeExpandCallExtra()) callCenter.expand()
}
internal fun Intent?.consumeExpandCallExtra(): Boolean {
    if (this == null || !getBooleanExtra(CallNotificationFactory.EXTRA_EXPAND_CALL, false)) return false
    removeExtra(CallNotificationFactory.EXTRA_EXPAND_CALL); return true
}
```
Add an `internal fun Intent?.toPushEvent()` extension of the same shape (pure, null-safe, removes extras after posting). Parse in `onCreate` only when `savedInstanceState == null` and not `FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY`; every `onNewIntent`. Inject `PushLaunchRequest` with `@Inject lateinit var` like `callCenter`. UUID-validate ids (activity is exported).

### `navigation/Navigator.kt` (add `openRouted`)

**Analog:** `Navigator.openChatThread` (lines 143-155) and `navigateInTab` (122-130).
```kotlin
fun openChatThread(tab: NavKey, key: NavKey) {
    require(tab in state.backStacks) { "Unknown tab: $tab" }
    val stack = state.backStacks.getValue(tab)
    if (stack.lastOrNull() != key) {
        while (stack.size > 1) stack.removeAt(stack.lastIndex)
        stack.add(key)
    }
    state.lastTransition = if (tab == state.topLevelRoute) NavTransition.Push else NavTransition.TabSwitch
    state.topLevelRoute = tab
}
```
Copy the `require` validation-before-mutation, `lastTransition`, `topLevelRoute` shape. Add the Pitfall 7 dedupe (`StackOrigin.kt` KDoc) across `state.stacksInUse`. Test with the existing `navigation/` tests style.

### `network/ApiRequestUrls.kt` / `ClickMeApi.kt` (modify)

**Analog:** the Phase 12 block, `ApiRequestUrls.kt` (`CHATS_INITIATE`, `USER_REPORT = "users/{id}/report"`) and `ClickMeApi.kt:518-523`.
```kotlin
/** Ports iOS ClickMeAPI+Chats reportUser. 201; 429 ... */
@POST(ApiRequestUrls.USER_REPORT)
suspend fun reportUser(@Path("id") userId: String, @Body body: ReportUserRequest): SuccessDataResponse<ReportUserData>
```
Add a `// Phase 13` block: `notifications/tokens` (POST), `notifications/tokens/{device_id}` (DELETE), `notifications` (GET, page/limit), `bookings/{id}/reviews/context` (GET), `bookings/{id}/reviews` (POST). `FakeClickMeApi` in `testing/` must gain handlers (see test section).

### `model/requests/RegisterDeviceTokenRequest.kt`

**Analog:** `model/requests/ChatRequests.kt` lines 1-20.
```kotlin
// strict zod schemas: carry only schema keys, nullable fields default to null; Moshi omits nulls
@JsonClass(generateAdapter = true)
data class InitiateChatRequest(@Json(name = "peer_id") val peerId: String)
```
Fields exactly `device_id`, `token`, `platform = "android"`, optional `app_version` (omit, never null-serialize).

### `screens/clientmode/reviews/ReviewsRepository.kt` and `notifications` repository

**Analog:** `screens/clientmode/mybookings/ClientBookingsRepository.kt` lines 1-45.
```kotlin
@Singleton
class ClientBookingsRepository @Inject constructor(private val api: ClickMeApi) {
    suspend fun detail(bookingId: String): ClientBookingDetail =
        apiCall { api.getClientBookingDetail(bookingId) }.data
}
```
Same imports (`network.ClickMeApi`, `network.apiCall`, javax Inject/Singleton). The resolver reuses `ClientBookingsRepository.detail` for D-06/F-6 (200 = client + status; 404/403 = expert; other = unknown).

### `screens/clientmode/reviews/ReviewLaunchRequest.kt`

**Analog:** `screens/clientmode/mybookings/BookingsLaunchRequest.kt` (with `ExpertLaunchRequest`): `@Singleton` with `OneShotSlot<ReviewSubmitted(bookingId)>`. Consumed by `BookingSummaryViewModel` / `MyBookingsViewModel` (consume in `init` AND on each later `pending` emission, per the `MyBookingsViewModel` KDoc "D-07 hand-off").

### `screens/notifications/*` and `screens/clientmode/writereview/*`

**Analog:** `screens/clientmode/mybookings/MyBookingsScreen.kt` / `MyBookingsViewModel.kt` (`@HiltViewModel`, `SavedStateHandle`, repository, quiet reload on `onScreenShown`, pull-to-refresh via `PullToRefreshBox`, snackbar failure flag) and `screens/clientmode/bookingsummary/{Key,Screen,UiState,ViewModel}`.
```kotlin
// BookingSummaryKey.kt:14-17 - Nav3 key shape with origin
@Serializable
data class BookingSummaryKey(val bookingId: String, val origin: StackOrigin = StackOrigin.Default) : NavKey
```
`NotificationsKey(origin)` and `WriteReviewKey(bookingId, origin)` follow this. Shared `EmptyState`/`ErrorState`/`AsyncSaveButton`/`Avatar` from Phase 1. Error copy = server `message` in `AlertDialog`. Date grouping uses `java.time` with the `Clock` from `ClockModule` (`testing/MutableClock` in tests). No `fontSize`, no `Color(...)` literals (CI `scripts/check-design-rules.sh`).

### Modified existing UI (Phase 13 hook points)

- `screens/clientmode/bookingsummary/BookingSummaryUiState.kt:39` (D-16 field), `BookingSummaryScreen.kt:85` (Feedback + Leave a Review), `screens/clientmode/mybookings/subviews/PastBookingCard.kt:51` (Review affordance). Grep `Phase 13` for exact spots.
- Bells: `screens/expertmode/dashboard/ExpertDashboardScreen.kt` (~188-191 `AuthTopAppBar` actions next to `TopBarAvatarButton`) and `screens/clientmode/explore/ExploreScreen.kt` (~151-153 `TopAppBar` actions).
- Both home shells already take `pendingChatThread` / `onConsumeChatThread`; mirror for `pendingShellRoute` with a mode check (F-7).

## Shared Patterns

### Session-cleared singleton state
**Source:** `di/ChatModule.kt` (`@Binds @IntoSet SessionDataCleaner`). **Apply to:** `PushLaunchRequest`, `ShellRouteRequest`, reviewed-ids set. NOT to device id / asked flag (must survive logout).

### API call + errors
**Source:** `ClientBookingsRepository` `apiCall { api.x() }.data`; Moshi DTOs with explicit `@Json(name=...)`; strict-schema omit-nulls rule from `ChatRequests.kt`.

### Hand-off consumed once
**Source:** `OneShotSlot` (`ExpertLaunchRequest.kt:13-26`); consume in a `LaunchedEffect` in the shell, never imperatively from the root; check pending mode equals the shell's mode.

### Test style
- Slot/store: `src/test/.../screens/chat/ChatLaunchRequestTest.kt` (pure JVM, `post`/`consume`/`assertNull`).
- Repository: `ChatsRepositoryTest.kt` (`FakeClickMeApi` with `xxxHandler` lambdas plus recorded call lists; `runTest`; helpers in `testing/`). Add handlers for the new endpoints to `FakeClickMeApi`.
- Intent parsing: `MainActivityIntentTest.kt` (`@RunWith(RobolectricTestRunner::class)`, build `Intent().putExtra(...)`, assert extras removed).

## No Analog Found

| File | Role | Data Flow | Reason |
|---|---|---|---|
| `google-services` plugin / Firebase deps / manifest service + meta-data | config | n/a | No Firebase in repo yet. Use RESEARCH.md "Installation (verified shape)" (guarded `if (file("google-services.json").exists()) apply(plugin = ...)`). |
| `PushDestinationResolver` (role probe + status + request expiry) | service | request-response | No existing multi-step resolver; compose `ClientBookingsRepository.detail`, `BookingRequestsRepository` (incoming list for expiry), `UserManager.requestModeSwitch` (check `me.userRoles` contains Expert and no active call, F-5). Hide lookups behind small interfaces for fakes. |
| Backend `push.js` change (D-04) | service | pub-sub | Cross-repo; reference the Phase 11 G-11-6 backend task for the plan/deploy/`/health` flow. Include android-only `category`/`sub_category` in `data` (RESEARCH F-1) plus `android.notification.channel_id`; leave `apns` byte-identical. |
| Human checklist (keystore, Play Console drafts) | docs | n/a | Non-code; follow D-21 and RESEARCH F-9. |

## Metadata

**Analog search scope:** `app/src/main/java/.../{managers,realtime,di,navigation,network,services/call,screens/chat,screens/clientmode,screens/expertmode,screens/call}`, `app/src/test/...`
**Files read:** ~14 (CONTEXT, RESEARCH lines 1-397 only; the remaining RESEARCH pages were not read)
**Pattern extraction date:** 2026-10-28
