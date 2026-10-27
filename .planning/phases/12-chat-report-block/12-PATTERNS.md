# Phase 12: Chat, Report & Block - Pattern Map

**Mapped:** 2026-10-27
**Files analyzed:** 33 (new and modified)
**Analogs found:** 31 / 33

All paths below are relative to `app/src/main/java/com/uptrendinvestments/clickmeapp/` (main) or `app/src/test/java/com/uptrendinvestments/clickmeapp/` (test). Every analog was confirmed with `git ls-files` (tracked source only).

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match |
|---|---|---|---|---|
| `network/ApiRequestUrls.kt` (add CHATS_*, USER_REPORT) | config | request-response | same file, `EXPERT_BOOKING_REQUESTS` group (line ~75) | exact |
| `network/ClickMeApi.kt` (6 methods) | service (Retrofit) | request-response | same file, `acceptBookingRequest` etc. | exact |
| `model/requests/ChatRequests.kt` | model | request-response | `model/requests/ExpertBookingRequests.kt` | exact |
| `model/responses/ChatPayloads.kt` | model | request-response | `model/responses/BookingRequestsPayload.kt` | exact |
| `model/enums/UserReportReason.kt` | model | transform | `model/enums/ErrorCode.kt` (wire string enum) | role-match |
| `model/enums/ErrorCode.kt` (add `CannotChatWithSelf`) | model | transform | same file | exact |
| `screens/chat/ChatsRepository.kt` | service | request-response | `screens/expertmode/incomingrequests/BookingRequestsRepository.kt` | exact |
| `screens/chat/ChatLauncher.kt`, `ChatLaunchRequest.kt` | service | event-driven (one-shot) | `screens/expertmode/ExpertLaunchRequest.kt` (`OneShotSlot`) | exact |
| `screens/chat/ThreadReducer.kt`, `BlockState` | utility | transform (pure) | `screens/clientmode/mybookings` pure logic (`BookingBucketsTest` subject) | role-match |
| `screens/chat/conversations/ConversationsScreen.kt` + `ConversationsViewModel.kt` | component + viewmodel | CRUD + event-driven | `screens/clientmode/mybookings/MyBookingsScreen.kt` / `MyBookingsViewModel.kt` | exact |
| `screens/chat/chatting/ChatThreadScreen.kt` + `ChatThreadViewModel.kt` + `ChatThreadKey` | component + viewmodel | streaming + CRUD | `ExpertProfileViewModel.kt` (assisted key) + `MyBookingsViewModel.kt` (realtime) | role-match |
| `screens/chat/reportandblock/*` | component + viewmodel | request-response | `ExpertProfileViewModel.kt` (assisted key) | role-match |
| `components/safety/ReportUserSheet.kt`, `BlockUserDialog.kt` | component | request-response | `screens/login/AuthErrorDialog.kt` (dialog); a `ModalBottomSheet` usage in repo | role-match |
| `components/chat/MessageButton.kt` | component | request-response | existing PressScale/async buttons in `components/` | partial |
| `navigation/Navigator.kt` (add `openChatThread`) | utility | event-driven | same file, `navigateInTab` (lines 117-129) | exact |
| `screens/clientmode/home/HomeClientScreen.kt`, `screens/expertmode/home/HomeExpertScreen.kt` (chatsRoot slot, slot consumer) | component | event-driven | same files, `bookingsRoot` slot + `BookingsLaunchRequest` | exact |
| `screens/call/calling/subviews/MeetingCallControls.kt:183` + `MeetingCallViewModel.kt` + `MeetingCallUiState` (peerId) | component | event-driven | same files | exact |
| `screens/clientmode/expertprofile/ExpertProfileScreen.kt` / `ExpertProfileViewModel.kt` (Message, menu, report/block) | component | request-response | same files | exact |
| Message button wiring: client `BookingSummaryScreen`, `UpcomingBookingScreen`, My Bookings card; expert `RequestDecisionBlocks.kt`, `ExpertBookingSummaryScreen`, `ExpertUpcomingCard` | component | request-response | same files plus `MyBookingsScreen.kt` | role-match |
| `managers/SessionDataCleaner` binding (blocked-by-me set, if kept in a singleton) | service | batch | `managers/SessionDataCleaner.kt` | exact |
| Tests: `ChatsRepositoryTest`, `ConversationsViewModelTest`, `ThreadReducerTest`, `ChatThreadViewModel*Test`, `ReportAndBlockViewModelTest`, `ChatLauncherTest`, `NavigatorOpenChatThreadTest`, etc. | test | n/a | `screens/clientmode/mybookings/MyBookingsViewModelTest.kt`, `ClientBookingsRepositoryTest.kt`, `BookingsLaunchRequestTest.kt` | exact |
| `testing/FakeClickMeApi.kt`, `FakeRealtimeConnection.kt` (Wave 0), `testing/ChatFixtures.kt` (new) | test util | n/a | same files / `testing/ExpertBookingFixtures.kt` | exact |

## Pattern Assignments

### `model/requests/ChatRequests.kt` (model, request-response)
**Analog:** `model/requests/ExpertBookingRequests.kt` lines 1-30. Nullable-with-default fields are omitted by the app-wide Moshi, which matters because the backend zod `.optional()` rejects JSON null.
```kotlin
@JsonClass(generateAdapter = true)
data class AcceptBookingRequestBody(
    val message: String? = null,
)
@JsonClass(generateAdapter = true)
data class DeclineBookingRequestBody(
    @Json(name = "reason_code") val reasonCode: String,
    @Json(name = "reason_text") val reasonText: String,
)
```
Apply: `InitiateChatRequest(@Json(name="peer_id"))`, `SendMessageRequest(type, content, @Json(name="client_msg_id"))`, `ChatActionRequest(action, reason: String? = null, details: String? = null)`, `ReportUserRequest(reason, details: String? = null)`. Never use `serializeNulls`. Redact `content` in `toString()` (see `RealtimeEvents.kt` `MessageNew`).

### `model/responses/ChatPayloads.kt` (model)
**Analog:** `model/responses/BookingRequestsPayload.kt` and `model/responses/PaginationMeta.kt` (`hasMoreOrDerived`). DTO shapes are in RESEARCH "DTO notes" (`blocked_by_me` default `false`, `type`/`status`/`event_type` stay `String`). `StatusOnlyResponse(val status: String? = null)` for `chatAction`.

### `network/ApiRequestUrls.kt` + `ClickMeApi.kt` (config / service)
**Analog:** same files. Add a "Phase 12" constant group next to `EXPERT_BOOKING_REQUESTS = "expert/booking-requests"` (line 75). Add methods in the style of `@POST(ApiRequestUrls.EXPERT_BOOKING_DECLINE_RESCHEDULE)` (ClickMeApi line ~434). Method signatures are in RESEARCH "Retrofit surface". No `publicPaths` change.

### `screens/chat/ChatsRepository.kt` (service, request-response)
**Analog:** `screens/expertmode/incomingrequests/BookingRequestsRepository.kt` lines 1-52.
```kotlin
@Singleton
class BookingRequestsRepository
    @Inject
    constructor(
        private val api: ClickMeApi,
    ) {
        suspend fun list(): List<BookingRequestItem> = apiCall { api.getBookingRequests() }.data.requests
        suspend fun decline(requestId: String, reasonCode: String, reasonText: String): DeclineBookingRequestData =
            apiCall { api.declineBookingRequest(requestId, DeclineBookingRequestBody(reasonCode, reasonText)) }.data
    }
```
Copy the KDoc stance verbatim in spirit: stateless, throws `ApiException` only, user text never logged. Any session-local "blocked by me" set goes in a separate class with a `SessionDataCleaner` `@IntoSet` binding (`managers/SessionDataCleaner.kt`: `fun interface SessionDataCleaner { suspend fun clearSessionData() }`).

### `screens/chat/ChatLauncher.kt` / `ChatLaunchRequest.kt` (service, one-shot hand-off)
**Analog:** `screens/expertmode/ExpertLaunchRequest.kt` lines 12-60.
```kotlin
class OneShotSlot<T : Any> {
    private val _pending = MutableStateFlow<T?>(null)
    val pending: StateFlow<T?> = _pending.asStateFlow()
    fun post(value: T) { _pending.value = value }
    fun consume(): T? = _pending.getAndUpdate { null }
}
@Singleton
class ExpertLaunchRequest @Inject constructor() {
    val incomingRequests = OneShotSlot<IncomingRequestsLaunch>()
}
```
Reuse `OneShotSlot` (import it, do not copy). `ChatLaunchRequest` holds `val thread = OneShotSlot<ChatThreadKey>()`. Test analog: `BookingsLaunchRequestTest.kt` and the existing `ExpertLaunchRequestTest.kt`.

### `navigation/Navigator.kt` (add `openChatThread`)
**Analog:** `navigateInTab`, lines 117-129.
```kotlin
fun navigateInTab(tab: NavKey, route: NavKey) {
    require(tab in state.backStacks) { "Unknown tab: $tab" }
    state.backStacks.getValue(tab).add(route)
    state.lastTransition = NavTransition.TabSwitch
    state.topLevelRoute = tab
}
```
New method: same `require`; if the stack top equals `key`, only select the tab; else trim the stack to size 1 (copy the `while (stack.size > 1) stack.removeAt(stack.lastIndex)` loop from the reset function at lines ~105-113), then add. Test: `NavigatorOpenChatThreadTest` next to existing Navigator tests in `navigation/`.

### Shells `HomeClientScreen.kt` / `HomeExpertScreen.kt`
**Analog:** the existing `bookingsRoot` slot and the `BookingsLaunchRequest` consumption in each file. Add a `chatsRoot` slot replacing the `ClientTabRoot(ClientChatsRoot)` / `ExpertTabRoot(ExpertChatsRoot)` placeholders, and collect `chatLaunchRequest.thread.pending` then `consume()` and call `navigator.openChatThread(ClientChatsRoot|ExpertChatsRoot, key)`. Shells themselves are Hilt-free, so do the collection in the Hilt-aware `HomeClientScreen` / `HomeExpertScreen`. Executor should read those two files for the exact slot wiring before editing.

### `screens/chat/conversations/ConversationsScreen.kt` + `ConversationsViewModel.kt`
**Analog:** `screens/clientmode/mybookings/MyBookingsScreen.kt` (lines 75-100) and `MyBookingsViewModel.kt` (lines 60-102, 139-149).
Screen shown/hidden triad (copy exactly):
```kotlin
val state by viewModel.uiState.collectAsStateWithLifecycle()
LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onScreenShown() }
LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { viewModel.onScreenHidden() }
DisposableEffect(viewModel) { onDispose { viewModel.onScreenHidden() } }
// The Nav3 entry may not receive ON_RESUME when it is re-shown; it leaves and re-enters composition instead.
LaunchedEffect(Unit) { viewModel.onScreenShown() }
```
ViewModel realtime collection (copy, switch event type to `MessageNew` / `UserStatus`; also collect `realtime.state` for the Connected transition quiet reload):
```kotlin
viewModelScope.launch {
    realtime.events.filterIsInstance<RealtimeEvent.BookingUpdate>().collect {
        startLoad(if (_uiState.value.load is MyBookingsLoad.Loading) LoadMode.Loud else LoadMode.Quiet)
    }
}
```
Also reuse: `loadJob` cancel-and-restart (latest wins), `LoadMode.Loud/Quiet`, `PullToRefreshBox`, `EmptyState`, `SkeletonBlock`, `SectionError`. Add `displayUnread = max(server, local)` as one function.

### `screens/chat/chatting/ChatThreadViewModel.kt` / `ChatThreadScreen.kt` / `ChatThreadKey`
**Analog:** `screens/clientmode/expertprofile/ExpertProfileViewModel.kt` lines 34-70.
```kotlin
@HiltViewModel(assistedFactory = ExpertProfileViewModel.Factory::class)
class ExpertProfileViewModel
    @AssistedInject
    constructor(
        @Assisted key: ExpertProfileKey,
        private val repository: ExpertProfileRepository,
    ) : ViewModel() {
        @AssistedFactory
        interface Factory { fun create(key: ExpertProfileKey): ExpertProfileViewModel }
        private val _uiState = MutableStateFlow(ExpertProfileUiState(/* seeded from key */))
        val uiState: StateFlow<ExpertProfileUiState> = _uiState.asStateFlow()
        private var detailsJob: Job? = null
        init { load() }
```
Composable side: `hiltViewModel<VM, VM.Factory>(creationCallback = { it.create(key) })`. Add `SavedStateHandle` for composer text (precedent `SignupBasicInfoViewModel`, `MakeABookingViewModel`). Inject `RealtimeConnection`, `CallStarter`-style fakes for tests. Realtime pattern as above. Key shape `ChatThreadKey(threadId, peerId, peerName, peerAvatarUrl, peerIsExpert: Boolean?)`, `@Serializable` NavKey.

### `screens/chat/ReportAndBlock` and `components/safety/*`
**Analog (error dialog):** `screens/login/AuthErrorDialog.kt` lines 20-40: `AuthErrorDialog(message, onDismiss, title = ..., selectable = false)`. Use it for server-message errors and "Couldn't send message". The Block confirm dialog follows the same `AlertDialog` styling (`containerColor = surfaceContainerHigh`, `shape = extraLarge`). VM state machine follows the pure `BlockState` in RESEARCH; assisted key as above.

### `MeetingCallControls.kt` (modify)
**Analog/site:** lines 175-188. Replace:
```kotlin
onClick = {},
// PHASE-12: enable once in-call chat exists (D-11).
enabled = false,
stateDescription = stringResource(R.string.call_control_chat_unavailable),
```
with an `onChat` parameter threaded from `MeetingCallViewModel`, `enabled = peerId != null && !busy`. Add `peerId` to `MeetingCallUiState` (lines 12-30), and call `callCenter.minimize()` after the launcher succeeds. Extend the existing `MeetingCallActionsTest` / `MeetingCallScreenTest`.

### Entry-point screens (Message buttons)
**Analog:** each target screen itself (KDocs say "without Message Expert"); card wiring follows `MyBookingsScreen`'s `onJoin: (ClientBookingItem) -> Unit` callback pattern passed from VM. Peer id sources: `BookingDetailExpertDto.id` (nullable, disable when null), `BookingRequestDetailClientDto.id`, `ExpertBookingClientDto.id`, `BookingExpertDto.id`. Add defaulted `origin` to `ExpertProfileKey`, `BookingSummaryKey`, `UpcomingBookingKey` (Pitfall 7).

### Tests
**Analog:** `screens/clientmode/mybookings/MyBookingsViewModelTest.kt` lines 1-40.
```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class MyBookingsViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val api = FakeClickMeApi()
    private val realtime = FakeRealtimeConnection()
```
Use `runTest`, `runCurrent`, `CompletableDeferred` for in-flight control, `FakeClickMeApi` handler vars, `FakeRealtimeConnection.emit/setState`. Repository tests: `ClientBookingsRepositoryTest.kt`. Screen tests: `MyBookingsScreenTest.kt` (Robolectric Compose; render `*Content` stateless composables, never construct ViewModels in composables, lint rule). Fixtures: `testing/ExpertBookingFixtures.kt` is the model for `ChatFixtures.kt`.

## Shared Patterns

### API call + errors
**Source:** `network/apiCall`, `ApiException.Http.code/status/userMessage`, `ApiException.Network` (login_error_network). Apply to all repositories and VMs. Map by `code` then `status` (RESEARCH Error Mapping table).

### Quiet reload / realtime catch-up
**Source:** `MyBookingsScreen.kt` 75-100 and `MyBookingsViewModel.kt` 90-149. Apply to Conversations and Thread VMs, plus re-run on `realtime.state` re-entering `Connected`.

### Stateless singletons
**Source:** `BookingRequestsRepository` KDoc. No message cache in `@Singleton`s; anything session-scoped registers a `SessionDataCleaner`.

### One-shot hand-offs
**Source:** `OneShotSlot` in `screens/expertmode/ExpertLaunchRequest.kt`.

### Logging
Redact content in `toString()` on DTOs holding message text (model after `RealtimeEvent.MessageNew`).

## No Analog Found

| File | Role | Data Flow | Reason |
|---|---|---|---|
| `screens/chat/ThreadReducer.kt` (merge/ack/fail by `client_msg_id`) | utility | transform | No existing list-reconciliation reducer; use RESEARCH Pattern 1 and test with plain JUnit |
| Chat bubble list with `LazyColumn(reverseLayout = true)` (`ChatThreadScreen` body) | component | streaming | No existing reverse-layout list; use RESEARCH Pattern 2 (assumption A2) |

(`components/chat/MessageButton.kt` is a partial match only; compose from the existing press-scale / async-save button in `components/`, which the executor should locate with `git ls-files app/src/main | grep components/`.)

## Metadata

**Analog search scope:** `app/src/main/.../{network,model,navigation,realtime,managers,screens/{clientmode,expertmode,call,login},components}`, `app/src/test/.../{screens/clientmode/mybookings,testing}`
**Files scanned:** about 25 read in part; tracked status verified via `git ls-files`
**Pattern extraction date:** 2026-10-27
