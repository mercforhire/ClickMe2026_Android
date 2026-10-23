# Phase 8: Client Bookings & Cancellation - Pattern Map

**Mapped:** 2026-10-23
**Files analyzed:** 33 new/modified
**Analogs found:** 31 / 33

All paths below are relative to `app/src/main/java/com/uptrendinvestments/clickmeapp/` unless prefixed `test/` (= `app/src/test/java/com/uptrendinvestments/clickmeapp/`).

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match |
|---|---|---|---|---|
| `network/ApiRequestUrls.kt` (+3 consts) | config | request-response | same file, Phase 6/7 blocks (lines 38-50) | exact |
| `network/ClickMeApi.kt` (+3 POSTs) | service (Retrofit) | request-response | `createPaymentMethodSetupIntent` / `getClientBookingDetail` (lines 140-190) | exact |
| `model/requests/BookingActionRequests.kt` (new) | model | request-response | `model/requests/PaymentRequests.kt` | exact |
| `model/responses/BookingActionResponses.kt` (new) | model | request-response | `model/responses/ClientBookingDetail.kt` | role-match |
| `model/responses/ClientBookingsPayload.kt` (modify) | model | CRUD read | itself | exact |
| `model/responses/ClientBookingDetail.kt` (modify) | model | CRUD read | itself | exact |
| `util/Money.kt` (+`formatMajor`) | utility | transform | `Money.format` | exact |
| `screens/clientmode/mybookings/ClientBookingsRepository.kt` (+3 methods) | service | request-response | itself + `PaymentMethodsRepository` | exact |
| `mybookings/BookingBuckets.kt` (new) | utility (pure) | transform | `mybookings/TodaysSessions.kt` | exact |
| `mybookings/MyBookingsViewModel.kt` + `MyBookingsUiState.kt` (new) | viewmodel | CRUD + event-driven (realtime) | `explore/ExploreViewModel.kt` (reload jobs, todaysSessions) + `makeabooking/MakeABookingViewModel.refreshPaymentMethods` | role-match |
| `mybookings/MyBookingsScreen.kt` + `subviews/*` (new) | component (screen) | CRUD read, pull-to-refresh | `explore/ExploreScreen.kt` (PullToRefreshBox, Scaffold, Snackbar) | role-match |
| `components/BookingStatusBadge.kt` (new) | component | transform | `components/PaymentLifecycleCard.kt` / `SelectableChip.kt` | partial |
| `components/RescheduleResponseCard.kt` (new) | component | request-response | `components/PaymentLifecycleCard.kt` | partial |
| `upcomingbooking/UpcomingBookingKey.kt` (+ cancel, summary keys) | route key | n/a | `requestsent/RequestSentKey.kt` | exact |
| `upcomingbooking/UpcomingBookingViewModel.kt` (new) | viewmodel | request-response + realtime | `requestsent/BookingRequestSentViewModel.kt` | exact |
| `upcomingbooking/UpcomingBookingScreen.kt` + `subviews/*` (new) | component | request-response | `requestsent/BookingRequestSentScreen.kt` | exact |
| `bookingsummary/BookingSummaryViewModel.kt` / `Screen.kt` (new) | viewmodel/screen | request-response | same as Upcoming (shares detail load + card) | exact |
| `mybookings/RescheduleErrors.kt` (new, accept/decline/cancel mappers) | utility | transform | `makeabooking/PaidFlowError.kt`, `BookingErrors.kt` | exact |
| accept-with-card-recovery logic (in Upcoming/Summary VM) | viewmodel | request-response | `PaymentMethodsRepository.list()` + RESEARCH "Accept with one-shot card recovery" | partial |
| `cancellation/CancelBookingViewModel.kt` (new) | viewmodel | request-response, multi-step | `makeabooking/MakeABookingViewModel.kt` (Assisted key + SavedStateHandle + isSubmitting) | role-match |
| `cancellation/CancelBookingScreen.kt` + `subviews/*` (new) | component | request-response | `makeabooking/MakeABookingScreen.kt` (BackHandler, AuthTopAppBar) | role-match |
| `cancellation/RefundPreview.kt`, `CancellationReason.kt` (new) | utility (pure) | transform | `mybookings/TodaysSessions.kt` + `makeabooking/BookingFormatting.capNotes` | role-match |
| `home/ClientBookingManagementEntries.kt` (new) | route/config | n/a | `home/ClientBookingFlowEntries.kt` | exact |
| `home/HomeClientScreen.kt` (modify: `bookingsRoot` slot + entries) | config/wiring | n/a | itself (`exploreRoot` slot, lines 82-130, 165-198) | exact |
| `explore/ExploreScreen.kt`, `subviews/ExploreTodaysSessionsSection.kt` (modify: `onSessionClick`) | component | event | itself | exact |
| `mybookings/BookingsLaunchRequest.kt` (new @Singleton holder) | service | event-driven (one-shot) | `makeabooking/CompletedBookingStore.kt` (loosely) | no real analog |
| `res/values/strings_*.xml` (5 new) | config | n/a | existing `strings_*` files (e.g. request sent / make a booking) | role-match |
| `test/testing/FakeClickMeApi.kt` (+3 handlers) | test util | n/a | itself (lines 60-80, 300-360) | exact |
| `test/testing/FakeRealtimeConnection.kt` (+`emit`) | test util | event | `emitFailure` (line 44) | exact |
| `test/.../network/ClickMeApiTest.kt`, `ApiRequestUrlsTest.kt` (modify) | test | request-response | themselves | exact |
| `test/.../mybookings/*Test.kt`, `cancellation/*Test.kt`, `upcomingbooking/*Test.kt` | test | n/a | `test/.../requestsent/BookingRequestSentViewModelTest.kt`, `makeabooking/PaidFlowErrorTest.kt` | exact |

## Pattern Assignments

### `network/ApiRequestUrls.kt` + `network/ClickMeApi.kt`

**Analog:** same files. Add under a `// Phase 8:` comment next to line 41 (`CLIENT_BOOKING_DETAIL = "client/bookings/{id}"`).

**Retrofit POST with explicit body** (`ClickMeApi.kt` ~lines 199-206):
```kotlin
/** Ports iOS ClickMeAPI+PaymentMethods createSetupIntent. The backend schema is `z.object({}).strict()`
 *  and Retrofit would otherwise send no body, so the `{}` body is explicit (RESEARCH Pitfall 13). */
@POST(ApiRequestUrls.PAYMENT_METHODS_SETUP_INTENT)
suspend fun createPaymentMethodSetupIntent(
    @Body body: Map<String, String> = emptyMap(),
): SuccessDataResponse<PaymentMethodSetupData>
```
Use `@Path("id") id: String` as in `getClientBookingDetail` (line 188). Declarations are given verbatim in RESEARCH "Retrofit declarations". Each KDoc starts "Ports iOS ClickMeAPI+Booking ...".

### `model/requests/BookingActionRequests.kt` (strict bodies)

**Analog:** `model/requests/PaymentRequests.kt`. Header `@file:Suppress("ktlint:standard:filename")` (multi-class file), then:
```kotlin
@JsonClass(generateAdapter = true)
data class ConfirmBookingRequest(
    @Json(name = "payment_intent_id") val paymentIntentId: String? = null,
    ...
    @Json(name = "client_notes") val clientNotes: String? = null,
)
```
Null defaults are omitted by the converter (no `withNullSerialization`), so `AcceptRescheduleRequest(stripePaymentMethodId = null)` serializes to `{}`. Document the strict-schema reason in KDoc as this file does.

### `model/responses/ClientBookingsPayload.kt` and `ClientBookingDetail.kt` (extend)

Existing item (ClientBookingsPayload.kt):
```kotlin
@JsonClass(generateAdapter = true)
data class ClientBookingItem(
    @Json(name = "booking_id") val bookingId: String,
    val expert: BookingExpertDto,
    val topic: String? = null,
    @Json(name = "start_time") val startTime: Instant,
    @Json(name = "end_time") val endTime: Instant,
    @Json(name = "meeting_type") val meetingType: String? = null,
    val status: String,
)
```
Add `val reschedule: RescheduleDto? = null`. Detail (ClientBookingDetail.kt) add `@Json(name = "authorization_expires_at") val authorizationExpiresAt: Instant? = null`, `@Json(name = "join_link") val joinLink: String? = null`, `@Json(name = "can_join") val canJoin: Boolean = false`, `val reschedule: RescheduleDto? = null`. One `RescheduleDto` with `proposedStart` required, others nullable (slim list vs full detail, RESEARCH Pitfall 5). All new fields need defaults so existing fixtures (`BookingRequestSentViewModelTest.detail()`) compile. Update the stale KDoc ("Phase 8 extends this class").

### `util/Money.kt` (+`formatMajor`)

Add next to `Money.format` (lines 22-46). Reuse its `Currency.getInstance(code)` try/catch and `DEFAULT_DIGITS` fallback; convert `BigDecimal.valueOf(major).movePointRight(digits).setScale(0, RoundingMode.HALF_UP).toLong()` then delegate to `format(minor, code, locale)`. Do not duplicate formatter logic.

### `ClientBookingsRepository.kt` (+cancel, acceptReschedule, declineReschedule)

```kotlin
suspend fun detail(bookingId: String): ClientBookingDetail =
    apiCall { api.getClientBookingDetail(bookingId) }.data
```
Follow the same one-liner `apiCall { api.xxx(...) }.data` shape. `apiCall` maps all failures to `ApiException`. Inject `PaymentMethodsRepository` into the VM (not this repository) for card recovery; its `list()` is `apiCall { api.getPaymentMethods() }.data.paymentMethods` (`screens/settings/paymentmethods/PaymentMethodsRepository.kt:25`).

### `mybookings/BookingBuckets.kt` (pure logic)

**Analog:** `mybookings/TodaysSessions.kt` lines 1-30: top-level `val JOINABLE_BOOKING_STATUSES: Set<String>`, extension functions on `List<ClientBookingItem>` with injected `Clock`/`ZoneId`, raw status strings, no Android imports. Put `buildBookingLists(upcoming, past, now)`, `isRejoinableCompleted`, and the label map there (RESEARCH "Rejoin rule and bucketing" is the body to copy). Test: copy `test/.../mybookings/TodaysSessionsFilterTest.kt` structure.

### `RescheduleErrors.kt` / cancel failure mappers

**Analog:** `makeabooking/BookingErrors.kt` lines 21-62 (and `PaidFlowError.kt` lines 60-110). Structure to copy:
```kotlin
fun ApiException.toBookingFailure(): BookingFailure =
    when (this) {
        is ApiException.Http ->
            when (code) {
                ErrorCode.AccountSuspended -> BookingFailure.Silent
                ErrorCode.SlotUnavailable -> BookingFailure.Show(FieldError.Res(R.string.make_a_booking_error_slot_taken), clearSlotAndReload = true)
                else -> BookingFailure.Show(userMessage?.let { FieldError.Text(it) } ?: FieldError.Res(R.string.login_error_unknown))
            }
        is ApiException.Network -> BookingFailure.Show(FieldError.Res(R.string.login_error_network))
        is ApiException.Decoding -> BookingFailure.Show(FieldError.Res(R.string.login_error_unknown))
    }
```
Define `sealed interface RescheduleFailure { Silent; Show(message, reload: Boolean, pop: Boolean) }` and a cancel equivalent. Branch on `ErrorCode` (`PaymentMethodRequired`, `InvalidPaymentMethod`, `RescheduleOutsideAuthWindow`, `ExpertUnavailable`, `InvalidStatus`, `Forbidden`, `ValidationError`, `NotFound`; all exist). `AccountSuspended -> Silent`. Server text only via `FieldError.Text` (T-06-13). Test: copy `PaidFlowErrorTest.kt` lines 10-45 (`http(status, code, message)` helper building `ApiException.Http(status, code, rawCode = code?.wire, serverMessage, fieldErrors = emptyList(), detail)`).

### `upcomingbooking/*Key.kt` (UpcomingBookingKey, CancelBookingKey, BookingSummaryKey)

**Analog:** `requestsent/RequestSentKey.kt` (whole file):
```kotlin
@Serializable
data class RequestSentKey(val bookingId: String) : NavKey
```
Place each in its own screen package, `import androidx.navigation3.runtime.NavKey` and `kotlinx.serialization.Serializable`.

### `UpcomingBookingViewModel` / `BookingSummaryViewModel` / `MyBookingsViewModel`

**Analog:** `requestsent/BookingRequestSentViewModel.kt` (full file): assisted Hilt VM, load-on-init, `loadJob?.cancel()` latest-wins, catch `ApiException`.
```kotlin
@HiltViewModel(assistedFactory = BookingRequestSentViewModel.Factory::class)
class BookingRequestSentViewModel @AssistedInject constructor(
    @Assisted key: RequestSentKey,
    private val repository: ClientBookingsRepository,
) : ViewModel() {
    @AssistedFactory interface Factory { fun create(key: RequestSentKey): BookingRequestSentViewModel }
    private val _uiState = MutableStateFlow<BookingRequestSentUiState>(BookingRequestSentUiState.Loading)
    val uiState: StateFlow<BookingRequestSentUiState> = _uiState.asStateFlow()
    private var loadJob: Job? = null
    init { load() }
    private fun load() {
        loadJob?.cancel()
        _uiState.value = BookingRequestSentUiState.Loading
        loadJob = viewModelScope.launch {
            _uiState.value = try { Loaded(repository.detail(bookingId)...) } catch (e: ApiException) { Failed }
        }
    }
}
```
Additions: a quiet variant that never sets Loading and never replaces Loaded with Failed; inject `Clock` (as `MakeABookingViewModel` does) and `RealtimeConnection`, collecting `events.filterIsInstance<RealtimeEvent.BookingUpdate>()` in `viewModelScope` (event class: `realtime/RealtimeEvents.kt:81`, fields `bookingId`, `status`). `isSubmitting` guard: return early if already true (`MakeABookingViewModel`, `if (_uiState.value.isSubmitting) return`).

**Resume-coalescing** (`MakeABookingViewModel.kt` ~lines 215-240):
```kotlin
fun refreshPaymentMethods() {
    if (_uiState.value.isSubmitting) return
    if (paymentMethodsJob?.isActive == true && paymentMethodsLoadIsCurrent) return
    loadPaymentMethods()
}
fun onScreenHidden() { paymentMethodsLoadIsCurrent = false }
```
Copy this idempotent pattern for `refreshQuietly()` / `onScreenHidden()` in My Bookings and Upcoming.

**Explore list reload** (`explore/ExploreViewModel.kt:169-196`): job field + `todayJob?.cancel()`, `try { bookingsRepository.list(ClientBookingsRepository.TYPE_UPCOMING)... } catch (_: ApiException) { ... }`. My Bookings `loadAll` uses `coroutineScope { async { list(TYPE_UPCOMING) }; async { list(TYPE_PAST) } }` with `limit = 20`.

### `cancellation/CancelBookingViewModel.kt`

**Analog:** `makeabooking/MakeABookingViewModel.kt` lines 58-100: `@HiltViewModel(assistedFactory = ...)`, `@Assisted key`, `SavedStateHandle` constructor arg, `private val clock: Clock`, `companion object { const val KEY_... }` string keys for persisted state (`step`, `selectedReasonIndex`, `comment`), `initialState(key)`. Completion signal idiom: `completedBookingId` + `consumeCompletedBooking()` (see `MakeABookingScreen.kt:107-111`: `LaunchedEffect(state.completedBookingId) { ... viewModel.consume...(); onBooked(id) }`).

### `CancelBookingScreen.kt` and other pushed screens

**Analog:** `requestsent/BookingRequestSentScreen.kt` lines 78-125: stateful wrapper `collectAsStateWithLifecycle()` + stateless `...Content(state, ...)` with defaulted `zone: ZoneId = ZoneId.systemDefault()` and `timeFormat = rememberDeviceTimeFormat()`:
```kotlin
Scaffold(
    modifier = modifier,
    containerColor = MaterialTheme.colorScheme.surface,
    topBar = { AuthTopAppBar(title = stringResource(R.string.request_sent_title), onBack = onBack) },
) { innerPadding -> when (state) { Loading -> ...; Failed -> FailedBody(onRetry, ...); is Loaded -> ... } }
```
Failed body uses `EmptyState(icon = Icons.Outlined.CloudOff, heading, body, action = EmptyStateAction(label, onClick))` (`components/EmptyState.kt:30-50`). Loading uses `CircularProgressIndicator` + `liveRegion` semantics, or `SkeletonGroup/SkeletonBlock` for list skeletons (D-11). Theme tokens only: `MaterialTheme.colorScheme.*`, `MaterialTheme.extras.onSurfaceMuted`, `Spacing.md`, never hard-coded colors or `fontSize` (`scripts/check-design-rules.sh`). Back-handling (`MakeABookingScreen.kt:164`): `BackHandler(enabled = state.isSubmitting) {}`; add a second `BackHandler(enabled = step == 2)` that returns to step 1 (order so in-flight wins). Tab refresh/resume: `LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refreshQuietly() }`, `LifecycleEventEffect(ON_PAUSE) { vm.onScreenHidden() }`, plus `LaunchedEffect(Unit)` (`MakeABookingScreen.kt:97-106`).

### `MyBookingsScreen.kt` (tabs, pull to refresh, snackbar)

**Analog:** `explore/ExploreScreen.kt` lines 152-170:
```kotlin
snackbarHost = { SnackbarHost(snackbarHostState) },
) { innerPadding ->
    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize().padding(innerPadding),
    ) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize().testTag(EXPLORE_LIST_TAG),
            contentPadding = PaddingValues(start = Spacing.md, end = Spacing.md, top = Spacing.lg, bottom = Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.xl)) { ... }
```
Add `PrimaryTabRow` above (no existing in-repo use; M3 API). `rememberSaveable` for the selected tab.

### Booking cards (Upcoming card with disabled Join, Past card)

**Analog:** `explore/subviews/ExploreTodaysSessionsSection.kt` lines 76-145 (`TodaysSessionCard`): `Surface(shape = MaterialTheme.shapes.medium, color = surfaceContainerLow, border = BorderStroke(1.dp, outlineVariant))`, `Avatar(url, size)`, then the disabled Join placeholder (copy exactly, keep `TODAYS_SESSION_JOIN_TAG` for the Explore card; use a new tag for My Bookings):
```kotlin
val joinState = stringResource(R.string.explore_join_call_state)
// PHASE-9: join window + click (D-02)
PressScaleButton(
    onClick = {}, enabled = false,
    modifier = Modifier.fillMaxWidth().heightIn(min = JoinButtonMinHeight)
        .testTag(TODAYS_SESSION_JOIN_TAG).semantics { stateDescription = joinState },
) { Icon(Icons.Outlined.Videocam, null, Modifier.size(IconSize)); Spacer(Modifier.width(Spacing.sm)); Text(stringResource(R.string.explore_join_call)) }
```
Time text: `sessionTimeRange` helper at line 151 uses `formatSessionTimeRange(start, end, zone, timeFormat)` (`mybookings/TodaysSessions.kt:33-43`); date lines use `formatShortDate`/`formatSessionWhen` (`makeabooking/BookingFormatting.kt:54,74`); expert tz note via `timezoneNote` (`BookingTimezoneNote.kt:31`).

### `explore` hand-off (modify `ExploreScreen`, `ExploreTodaysSessionsSection`)

Add `onSessionClick: (String) -> Unit` to `ExploreTodaysSessionsSection(sessions, modifier)` and `TodaysSessionCard`; make the `Surface` clickable (Surface `onClick` overload). `ExploreScreen.kt:170` call site `SectionAppear { ExploreTodaysSessionsSection(state.todaysSessions) }` becomes `(state.todaysSessions, onSessionClick)`. Thread through `HomeClientScreen.exploreRoot` (line 82-88: already passes `onOpenExpert = { navigator.navigate(it) }`, add `onOpenBooking = { navigator.navigate(UpcomingBookingKey(it)) }`). Update `test/.../explore/ExploreSectionsTest.kt`. Update the section's KDoc ("no tap action").

### `home/ClientBookingManagementEntries.kt`

**Analog:** `home/ClientBookingFlowEntries.kt` lines 29-70. Copy shape exactly: extension on `EntryProviderScope<NavKey>`, `navigator: Navigator`, composable slot lambdas receiving `(key, callbacks)`, `entry<K>(metadata = BottomBar.hidden()) { key -> slot(key, navigator::goBack, ...) }`. Navigation callbacks to wire:
```kotlin
{ navigator.navigate(PaymentMethodsKey) }                      // Add a Payment Method (D-02), as booking slot
{ navigator.resetAndSelect(ClientBookingsRoot, setOf(ExploreRoot, ClientBookingsRoot)) }  // after cancel (D-07), same as onViewBookings
```
`BookingKey` for Book Again: `BookingKey(expertId, expertName, expertTitle, expertImageUrl)` (`makeabooking/BookingKey.kt`). `PaymentMethodsKey` is already registered once by `clientPaymentMethodsEntry` (lines 63-68), do not re-register.

### `home/HomeClientScreen.kt` (modify)

Add a `bookingsRoot: @Composable (Navigator) -> Unit = { ClientTabRoot(ClientBookingsRoot) }` slot to `HomeClientShell` after `exploreRoot` (line 178), replace line 198 `entry<ClientBookingsRoot> { ClientTabRoot(ClientBookingsRoot) }` with `entry<ClientBookingsRoot> { bookingsRoot(navigator) }`. In `HomeClientScreen` supply `bookingsRoot = { MyBookingsScreen(viewModel = hiltViewModel(), ...) }` and register the new entries in `detailEntries`, using the Hilt pattern at lines 91-99:
```kotlin
val vm = hiltViewModel<BookingRequestSentViewModel, BookingRequestSentViewModel.Factory>(
    creationCallback = { it.create(key) },
)
```
Update KDoc at lines 165-167 and `ClientBookingFlowEntries` KDoc line 22 ("Bookings tab stays the placeholder"). Existing tests: `test/.../navigation` and `HomeShellsTest` drive the shell with fake content; extend them for the new slot.

### `BookingsLaunchRequest` (post-cancel "select Past + snackbar")

No analog. Closest idea: `makeabooking/CompletedBookingStore.kt` (a `@Singleton` one-shot handoff, with `test/testing/FakeCompletedBookingStore.kt` as its fake). Planner should follow RESEARCH Pattern 3: `@Singleton` class with `MutableStateFlow<Request?>` and consume-on-read, Hilt `@Inject constructor()`; provide a test double or use the real class (no Android deps).

### Tests and fakes

**`test/testing/FakeClickMeApi.kt`**: pattern lines 60-80 and 306-355:
```kotlin
var clientBookingDetailHandler: suspend (String) -> ClientBookingDetail = { notStubbed() }
override suspend fun getClientBookingDetail(bookingId: String): SuccessDataResponse<ClientBookingDetail> {
    clientBookingDetailCalls++
    return ok(clientBookingDetailHandler(bookingId))
}
```
Add `cancelClientBookingHandler`, `acceptClientRescheduleHandler`, `declineClientRescheduleHandler` plus call counters and recorded request lists (like `_requestBookingRequests`). Note `getClientBookings` handler receives only `type` (extend if tests need `limit`/`page`).

**`test/testing/FakeRealtimeConnection.kt`**: add beside `emitFailure` (line 44):
```kotlin
fun emit(event: RealtimeEvent) { check(_events.tryEmit(event)) { "event buffer full" } }
```
(Its `_events` field is private; confirm its name when editing.)

**ViewModel test shell:** `test/.../requestsent/BookingRequestSentViewModelTest.kt` lines 1-45: `@get:Rule val main = MainDispatcherRule()`, `private val api = FakeClickMeApi()`, `ClientBookingsRepository(api)`, `runTest`/`runCurrent`, `CompletableDeferred` for in-flight assertions, a local `detail(...)` fixture builder. Use `test/testing/MutableClock.kt` for `Clock`. Screen tests: copy `BookingRequestSentScreenTest.kt` (Robolectric Compose). Wire-body assertions go in `ClickMeApiTest.kt` (`{}`, `{"stripe_payment_method_id":"pm_x"}`, `{"reason":"..."}`); URL consts in `ApiRequestUrlsTest.kt`.

## Shared Patterns

### Error handling and copy
**Source:** `makeabooking/BookingErrors.kt`, `PaidFlowError.kt`. **Apply to:** all three VMs and mappers. `ApiException.{Http,Network,Decoding}` exhaustive `when`; `ErrorCode.AccountSuspended` is `Silent` (global dialog owns it); server text only as `FieldError.Text`; fallbacks `R.string.login_error_network` / `login_error_unknown`. Dialog: `AuthErrorDialog` (e.g. `screens/settings/paymentmethods/PaymentMethodsScreen.kt`).

### Time and money display
**Source:** `util/Money.kt`, `makeabooking/BookingFormatting.kt`, `BookingTimezoneNote.kt`, `mybookings/TodaysSessions.kt`. Device zone + `rememberDeviceTimeFormat()`, `zoneCity/zoneAbbreviation` for the "Expert timezone: X" note, `capNotes(text, max)` for the 500-char cap (`BookingFormatting.kt:26`, never `take`). Fee: minor units via `Money.format(price.amount, price.currency)`; cancel response uses new `Money.formatMajor`.

### Design rules
Material 3 tokens only (`MaterialTheme.colorScheme`, `MaterialTheme.extras`, `Spacing`, `MaterialTheme.typography`); gate is `testDevDebugUnitTest ktlintCheck assembleDevDebug` plus `scripts/check-design-rules.sh`. ktlint: trailing commas, constructor wrapping as in the excerpts above.

### Navigation
**Source:** `ClientBookingFlowEntries.kt`. Pushed detail screens use `metadata = BottomBar.hidden()`; cross-tab jumps use `Navigator.resetAndSelect(root, setOf(...))`; `Navigator` has `navigate`, `replaceTop`, `goBack`, `resetAndSelect`, `navigateInTab`.

### Hilt ViewModel creation
Assisted factory for key-bearing VMs (`hiltViewModel<VM, VM.Factory>(creationCallback = { it.create(key) })`); plain `hiltViewModel()` for `MyBookingsViewModel` (no key).

## No Analog Found

| File | Role | Data Flow | Reason |
|---|---|---|---|
| `mybookings/BookingsLaunchRequest.kt` | service | one-shot cross-stack event | No existing cross-tab launch-argument holder; use RESEARCH Pattern 3 |
| `PrimaryTabRow` usage in `MyBookingsScreen` | component | n/a | No tab row exists in the repo; use the M3 API directly (verified in the Gradle cache per RESEARCH) |

(`BookingStatusBadge` and `RescheduleResponseCard` have only partial analogs; follow the UI-SPEC and the `PaymentLifecycleCard` conventions: internal test-tag consts at the top, `Surface` card, `stateDescription` semantics, `@Preview` with `ClickMeTheme`.)

## Metadata

**Analog search scope:** `screens/clientmode/{mybookings,home,requestsent,makeabooking,explore}`, `network/`, `model/{requests,responses}`, `components/`, `util/`, `test/.../{testing,screens/clientmode,network}`.
**Files scanned:** about 30 read or grepped. RESEARCH.md was read to line 487 of 637; the remainder was not needed for analog mapping.
**Pattern extraction date:** 2026-10-23
