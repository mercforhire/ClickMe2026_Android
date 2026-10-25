# Phase 10: Expert Topics & Availability - Pattern Map

**Mapped:** 2026-10-24
**Files analyzed:** 30 new/modified
**Analogs found:** 28 / 30
**Note:** Line numbers are approximate, so locate excerpts by the named function or symbol. The root is `app/src/main/java/com/uptrendinvestments/clickmeapp/` (abbreviated `J/`). Tests are under `app/src/test/java/com/uptrendinvestments/clickmeapp/`.

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match |
|---|---|---|---|---|
| `J/managers/CallDialog.kt` (add `EditAvailability`) | enum | n/a | same file | exact |
| `J/screens/call/calling/CallDialogs.kt` (`restrictedActionText` branch) | component | n/a | same file | exact |
| `res/values/strings_call.xml` (`call_action_edit_availability`) | config | n/a | same file | exact |
| `J/network/ApiRequestUrls.kt` (+`EXPERT_TOPIC`, `EXPERT_MY_AVAILABILITY`) | config | n/a | same file | exact |
| `J/network/ClickMeApi.kt` (topic CRUD, availability GET/PUT, PATCH profile) | service (Retrofit) | request-response | same file (`getMyTopics`, `setupExpertProfile`) | exact |
| `J/network/NullPreservingAdapters.kt` + `MoshiProvider.kt` registration | utility | transform | `MoshiProvider.kt` / `InstantAdapter.kt` | role-match |
| `J/model/requests/ExpertTopicRequests.kt` | model | request | `model/requests/SignupRequests.kt` | role-match |
| `J/model/responses/ExpertTopicItem.kt`, edit `MyExpertTopicsPayload.kt` | model | request-response | `MyExpertTopicsPayload.kt`, `ExpertProfileData.kt` | exact |
| `J/model/responses/ExpertAvailabilityScheduleData.kt` | model | request-response | `model/responses/ExpertAvailabilityData.kt` | role-match |
| `J/screens/expertmode/topics/ExpertTopicsRepository.kt` | repository | CRUD | `screens/settings/paymentmethods/PaymentMethodsRepository.kt` | exact |
| `J/screens/expertmode/availability/AvailabilityRepository.kt` | repository | CRUD | `PaymentMethodsRepository.kt` | exact |
| `.../topics/TopicEvents.kt` | utility | event-driven | none (see No Analog) | none |
| `.../topics/TopicsViewModel.kt`, `TopicsUiState.kt` | viewmodel | CRUD | `PaymentMethodsViewModel.kt` / `PaymentMethodsUiState.kt` | exact |
| `.../topics/TopicsScreen.kt` + `subviews/` | component | CRUD + pull-to-refresh | `PaymentMethodsScreen.kt`, `MyBookingsScreen.kt` | role-match |
| `.../topics/editor/TopicEditorKey.kt` | route | n/a | `PaymentMethodsKey.kt`, `CancelBookingKey` | exact |
| `.../topics/editor/TopicEditorViewModel.kt` | viewmodel | request-response | `ClientCancellationViewModel.kt` (assisted) | exact |
| `.../topics/editor/TopicPrice.kt` | utility | transform | `util/Money.kt` | role-match |
| `.../topics/editor/TopicEditorScreen.kt` + subviews | component | form | `SignupTagsScreen.kt`, `ValidatedField.kt` | role-match |
| `.../editor/subviews/ExpertiseEditorSheet.kt` + VM | component/viewmodel | request-response | `SignupTagsViewModel.kt` / `SignupTagsScreen.kt` | role-match |
| `.../availability/AvailabilityKey.kt` | route | n/a | `PaymentMethodsKey.kt` | exact |
| `.../availability/AvailabilityScheduleLogic.kt` | utility | transform | `util/Money.kt` (pure object) | partial |
| `.../availability/AvailabilityViewModel.kt` | viewmodel | request-response | `PaymentMethodsViewModel.kt` (guard + load) | role-match |
| `.../availability/AvailabilityScreen.kt` + subviews (day row, time-range sheet, tz row) | component | form | `SignupTimezoneScreen.kt`, `PickerSheet.kt` | role-match |
| `J/components/SelectableChip.kt` (+`enabled`) | component | n/a | same file | exact |
| `J/screens/expertmode/home/HomeExpertScreen.kt` (wire Topics, Availability row) | route wiring | n/a | `HomeClientScreen.kt` (Payment Methods row + `detailEntries`) | exact |
| `res/values/strings_expert_topics.xml`, `strings_expert_availability.xml` | config | n/a | `strings_call.xml` | role-match |
| Tests: `ExpertTopicsViewModelTest`, `TopicPriceTest`, `AvailabilityScheduleLogicTest`, etc. | test | n/a | `PaymentMethodsViewModelTest.kt` | role-match |
| `test/.../testing/FakeClickMeApi.kt` (new handlers) | test | n/a | same file | exact |
| `AppViewModelTest.kt`, `UserManagerTest.kt` (update `MyExpertTopicsPayload(...)` call sites) | test | n/a | same files | exact |

## Pattern Assignments

### Call guard enum and string (modify 3 files)

**Analog:** `J/managers/CallDialog.kt`. The enum is `SwitchModes, LogOut, AddPaymentMethod, CancelBooking`. Add `EditAvailability`.

`J/screens/call/calling/CallDialogs.kt` has an exhaustive `when` in `restrictedActionText`. Add a branch or the build breaks:
```kotlin
RestrictedAction.CancelBooking -> R.string.call_action_cancel_booking
RestrictedAction.EditAvailability -> R.string.call_action_edit_availability
```
In `strings_call.xml`, next to `call_action_cancel_booking`, add `<string name="call_action_edit_availability">edit your availability</string>`. The dialog text is built as `"End the call to <action>."`, so the resulting copy is "End the call to edit your availability."

### `ClickMeApi.kt` and `ApiRequestUrls.kt` (modify)

**Analog:** the same files.
```kotlin
// ApiRequestUrls.kt: existing constants EXPERT_PROFILE = "expert/profile"; EXPERT_AVAILABILITY = "experts/{id}/availability" (public, do not reuse)
// ClickMeApi.kt
@GET(ApiRequestUrls.EXPERT_TOPICS)
suspend fun getMyTopics(): SuccessDataResponse<MyExpertTopicsPayload>

@PATCH(ApiRequestUrls.EXPERT_PROFILE_SETUP)
suspend fun setupExpertProfile(@Body body: SetupExpertProfileRequest): SuccessDataResponse<SetupExpertProfileData>
```
New methods follow this shape: `@POST/@PATCH/@DELETE(EXPERT_TOPIC) ... @Path("id")`, `@GET/@PUT(EXPERT_MY_AVAILABILITY)`, and `@PATCH(EXPERT_PROFILE)` for tags. Void-ish responses use `SuccessMessageResponse`, as the existing `resendVerificationEmail` and `verifyEmailCode` do. Every method added also needs a handler in `test/.../testing/FakeClickMeApi.kt`.

### DTOs (`model/responses/ExpertTopicItem.kt`, `ExpertAvailabilityScheduleData.kt`; `model/requests/ExpertTopicRequests.kt`)

**Analogs:** `MyExpertTopicsPayload.kt`, `ExpertProfileData.kt`, `ExpertAvailabilityData.kt`, `SignupRequests.kt`.
```kotlin
@JsonClass(generateAdapter = true)
data class MyExpertTopicsPayload(val topics: List<Map<String, Any?>>)   // change element to ExpertTopicItem
```
Use `@JsonClass(generateAdapter = true)` and an explicit `@Json(name = "...")` on every multi-word field. Make fields nullable or defaulted, as in `ExpertAvailabilityData`. The topic response has no `hourly_rate`; it has `price{amount,currency,is_free,label}` and `expertise_tags[{id,label,category_id}]` (RESEARCH Finding 3). `SignupRequests.kt` documents that the Moshi converter omits nulls. A PATCH that must send a literal null for `icon_slug` or `description` therefore needs the custom adapter.

### `network/NullPreservingAdapters.kt` and `MoshiProvider.kt` (new/modify)

**Analog:** `J/network/MoshiProvider.kt`:
```kotlin
val moshi: Moshi = Moshi.Builder().add(InstantAdapter()).build()
```
Register the custom adapter with `.add(UpdateExpertTopicRequestAdapter())`. See `InstantAdapter.kt` for the hand-written adapter style. The adapter body is in RESEARCH "Explicit-null PATCH adapter". Add a MockWebServer test that asserts `"icon_slug":null` is in the request body.

### Repositories (`ExpertTopicsRepository`, `AvailabilityRepository`)

**Analog:** `screens/settings/paymentmethods/PaymentMethodsRepository.kt`.
```kotlin
@Singleton
class PaymentMethodsRepository @Inject constructor(private val api: ClickMeApi) {
    suspend fun list(): List<PaymentMethodItem> = apiCall { api.getPaymentMethods() }.data.paymentMethods
    suspend fun delete(paymentMethodId: String) { apiCall { api.deletePaymentMethod(paymentMethodId) } }
}
```
Each call throws `ApiException` only. Keep the repository stateless (RESEARCH: no singleton topic cache). `profileTags()` calls `apiCall { api.getExpertProfile() }.data.expertiseTags`.

### `TopicsViewModel.kt` and `TopicsUiState.kt` (viewmodel, CRUD)

**Analog:** `PaymentMethodsViewModel.kt` and `PaymentMethodsUiState.kt` (sealed list state `Loading / Failed / Loaded`, `busy` operation, `dialog`).

Core load with a generation counter, so only the newest fetch applies:
```kotlin
private fun load(showLoading: Boolean) {
    loadJob?.cancel()
    val generation = ++listGeneration
    if (showLoading) _uiState.update { it.copy(list = PaymentMethodsListState.Loading) }
    loadJob = viewModelScope.launch {
        try {
            val cards = repository.list()
            if (generation == listGeneration) _uiState.update { it.copy(list = PaymentMethodsListState.Loaded(cards)) }
        } catch (e: ApiException) {
            if (generation != listGeneration) return@launch
            if (showLoading) _uiState.update { it.copy(list = PaymentMethodsListState.Failed(e.toSectionMessage())) }
            else if (e.toDialogError() != null) { /* dialog */ }
        }
    }
}
```
One operation at a time with a dialog on failure (copy for delete, with `busy` disabling every overflow menu):
```kotlin
private fun runCardOperation(operation: CardOperation, errorTitle: Int, call: suspend () -> Unit) {
    _uiState.update { it.copy(busy = operation) }
    viewModelScope.launch {
        try { call(); reloadAfter(operation); _uiState.update { it.copy(busy = null) } }
        catch (e: ApiException) {
            _uiState.update { s -> s.copy(busy = null, dialog = e.toDialogError()?.let { PaymentMethodsDialog(errorTitle, it) } ?: s.dialog) }
        }
    }
}
```
Imports to reuse: `screens.clientmode.makeabooking.toSectionMessage`, `screens.login.FieldError`, `screens.login.toDialogError`. Resume reload goes through `onScreenShown()` guarded by an in-flight flag, like `MyBookingsScreen`.

### `TopicsScreen.kt` and subviews (component, CRUD with pull-to-refresh)

**Analogs:** `PaymentMethodsScreen.kt` (Scaffold, `AuthTopAppBar`, list state `when`, remove-confirm dialog with only the id saved, `AuthErrorDialog`), `MyBookingsScreen.kt` (PullToRefreshBox, SnackbarHost, resume effects).
```kotlin
// PaymentMethodsScreen.kt
var removeTargetId by rememberSaveable { mutableStateOf<String?>(null) }
Scaffold(containerColor = MaterialTheme.colorScheme.surface,
    topBar = { AuthTopAppBar(title = stringResource(...), onBack = onBack) }) { innerPadding ->
    when (val list = state.list) { Loading -> LoadingBody(...); is Failed -> FailedBody(...); is Loaded -> ... }
}
state.dialog?.let { AuthErrorDialog(message = it.message.asString(), onDismiss = onDismissDialog, title = stringResource(it.title)) }
```
```kotlin
// MyBookingsScreen.kt
LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onScreenShown() }
LaunchedEffect(Unit) { viewModel.onScreenShown() }   // Nav3 may not deliver ON_RESUME
val snackbarHostState = remember { SnackbarHostState() }; ... snackbarHost = { SnackbarHost(snackbarHostState) }
PullToRefreshBox(isRefreshing = state.isRefreshing, onRefresh = onRefresh, modifier = Modifier.fillMaxSize().padding(innerPadding)) { ... }
```
Use `components/EmptyState` and `EmptyStateAction` for the empty and error states. The overflow `DropdownMenu` follows the Payment Methods row (`screens/settings/paymentmethods/subviews/`). The Topics tab root needs an `AuthTopAppBar` variant with `backEnabled = false` and an `actions` slot for Availability.

### `TopicEditorKey.kt` and `AvailabilityKey.kt` (route)

**Analog:** `PaymentMethodsKey.kt`:
```kotlin
@Serializable
data object PaymentMethodsKey : NavKey
```
`AvailabilityKey` is a `data object`. `TopicEditorKey` is `@Serializable data class TopicEditorKey(val topicId: String?, val seed: TopicEditorSeed? = null) : NavKey`, and the seed must be `@Serializable` too. Precedent for keys carrying data: `ExpertProfileKey`, `CancelBookingKey`.

### `TopicEditorViewModel.kt` (assisted VM)

**Analog:** `screens/clientmode/cancellation/ClientCancellationViewModel.kt`. Its imports are `dagger.assisted.Assisted`, `AssistedFactory` and `AssistedInject`.
```kotlin
@HiltViewModel(assistedFactory = ClientCancellationViewModel.Factory::class)
class ClientCancellationViewModel @AssistedInject constructor(
    @Assisted key: CancelBookingKey,
    private val savedStateHandle: SavedStateHandle,
    private val repository: ClientBookingsRepository,
    ...
) : ViewModel() {
    @AssistedFactory interface Factory { fun create(key: CancelBookingKey): ClientCancellationViewModel }
```
Entry wiring in the shell (`HomeClientScreen.kt`, `detailEntries`):
```kotlin
entry<ExpertProfileKey> { key ->
    val vm = hiltViewModel<ExpertProfileViewModel, ExpertProfileViewModel.Factory>(creationCallback = { it.create(key) })
    ExpertProfileScreen(viewModel = vm, onBack = navigator::goBack, ...)
}
```
Mirror form state to `SavedStateHandle` as the cancellation VM does. Error mapping on save:
```kotlin
catch (e: ApiException.Http) {
    if (e.code == ErrorCode.PriceBelowMinimum) priceServerError = e.serverMessage
    else dialog = e.toDialogError()
}
```
Prefer `fieldErrors.firstOrNull()?.message ?: serverMessage` for generic failures, because `ApiException.Http.userMessage` is `serverMessage ?: firstFieldError`, which yields "Validation failed" on 422 envelopes.

### `TopicPrice.kt` and `AvailabilityScheduleLogic.kt` (pure utility)

**Analog:** `util/Money.kt` (a plain `object`, with currency resolved through `java.util.Currency` and `defaultFractionDigits`):
```kotlin
private fun currencyOrNull(code: String): Currency? =
    try { Currency.getInstance(code) } catch (_: IllegalArgumentException) { null }
private fun fractionDigits(currency: Currency?): Int =
    (currency?.defaultFractionDigits ?: DEFAULT_DIGITS).takeIf { it >= 0 } ?: DEFAULT_DIGITS
```
Floor lookup: `userManager.me.value?.pricingLimits?.minSessionAmount?.minMinorUnits(currency)` (already in `MeData.kt`). Code for `parsePrice`, `hydrateSlots`, `overlappingDays` and `minutesToHhmm` is in RESEARCH "Code Examples". Use `Locale.ROOT` for wire strings. Keep both files free of Android types so they can have JVM unit tests.

### `AvailabilityViewModel.kt` (guard, hydrate, silent save)

**Analog:** `PaymentMethodsViewModel.addCard()`. The guard comes first, before any state change:
```kotlin
fun addCard() {
    if (!callGuard.attempt(RestrictedAction.AddPaymentMethod)) return
    ...
}
```
For Availability, `save()` begins with `if (!callGuard.attempt(RestrictedAction.EditAvailability)) return`. The silent UTC auto-save must instead check `callGuard.isCallActive.value` (a `StateFlow<Boolean>` on `managers/CallGuard.kt`), never `attempt()`. Wrap it in `runCatching`. Load and retry reuse the `PaymentMethodsViewModel` load pattern above.

### `AvailabilityScreen.kt`, time-range sheet, timezone row/sheet

**Analogs:** `screens/signup/timezone/SignupTimezoneScreen.kt` and `SignupTimezoneViewModel.kt`, plus `components/PickerSheet.kt`.
```kotlin
// SignupTimezoneViewModel.kt
get() = entries.firstOrNull { it.id == selectedId }?.label ?: Timezones.entryFor(selectedId).label
val base = Timezones.pickerEntries()
// SignupTimezoneScreen.kt: timezoneSheetRows(entries, selectedId, onSelect) used inside PickerSheet
```
For the server zone, show `Timezones.all.firstOrNull { it.id == serverId }?.label ?: server display_name` (RESEARCH Finding 9, which overrides plain `entryFor()`). Use `Timezones.detected()` for Auto-detect and the UTC auto-fill. `PickerSheet(title, onDismiss, content = LazyListScope.() -> Unit)` is a `ModalBottomSheet` with `skipPartiallyExpanded = true`. For the time-range sheet use a `ModalBottomSheet` with `rememberModalBottomSheetState(skipPartiallyExpanded = true)` and `shape` and `containerColor` copied from `PickerSheet`. Use `rememberDeviceTimeFormat()` from `screens/clientmode/makeabooking/BookingFormatting.kt` for display. The "Saved" confirmation can reuse the 600 ms hold idea in `SignupTimezoneScreen`, here as a ~2 s `delay`.

### `ExpertiseEditorSheet.kt` and VM

**Analog:** `screens/signup/tags/SignupTagsScreen.kt` and `SignupTagsViewModel.kt`. The VM is bound to `SignupSetupRepository`, so it is not reusable. Extract the list, search and chip body into a shared composable (`SelectableChip`, `PickerLoading`, `EmptyState`) and write a new VM calling `PATCH /expert/profile` with `{expertise_tags:[{tag_id,is_primary}]}`. Keep the iOS "at least one tag" guard.

### `TopicEditorScreen.kt` (form)

**Analogs:** `components/ValidatedField.kt` and `SignupTagsScreen.kt`.
```kotlin
ValidatedField(value, onValueChange, placeholder, leadingIcon, error: String?, keyboardOptions = ..., trailingBadge = { Text(currencyCode) })
```
Price keyboard is decimal. The filter must accept both `.` and `,` (RESEARCH Pitfall 3). The server price error goes in `error`. Save is a TopAppBar action with a spinner. For the icon picker use `CategoryIconMap` (all 24 slugs are present) and `SelectableChip` for tags; add `enabled: Boolean = true` to `SelectableChip`.

### `HomeExpertScreen.kt` (modify)

**Analog:** `screens/clientmode/home/HomeClientScreen.kt`. Placeholder row in the profile footer:
```kotlin
// Throwaway entry point, replaced by the Phase 14 Profile hub (D-04).
ProfileMenuRow(
    title = stringResource(R.string.payment_methods_title),
    icon = Icons.Outlined.CreditCard,
    onClick = { navigator.navigate(PaymentMethodsKey) },
    modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.md),
)
Spacer(Modifier.height(Spacing.lg))
OutlinedButton(onClick = onLogout) { ... }
```
In the expert shell, replace `entry<TopicsRoot> { ExpertTabRoot(TopicsRoot) }` with the real `TopicsScreen`. Add `entry<TopicEditorKey>(metadata = BottomBar.hidden()) { key -> ... assisted hiltViewModel ... }` and `entry<AvailabilityKey>(metadata = BottomBar.hidden())`, matching the existing `entry<ModeSwitchKey>(metadata = BottomBar.hidden())`. Push with `navigator.navigate(...)` and pop with `navigator::goBack`. `HomeExpertShell` is deliberately Hilt-free for Robolectric tests, so hiltViewModel calls go in the `HomeExpertScreen` layer or in injected composable slots. Keep that constraint in mind when wiring.

### Tests

**Analog:** `test/.../screens/settings/paymentmethods/PaymentMethodsViewModelTest.kt` and `PaymentMethodsScreenTest.kt` (Robolectric Compose) and `PaymentRepositoriesTest.kt` (MockWebServer-style repository tests). `FakeClickMeApi` (`test/.../testing/FakeClickMeApi.kt`) must gain a handler for every new Retrofit method. Update the `MyExpertTopicsPayload(listOf(mapOf("id" to "t1")))` call sites in `AppViewModelTest` (3) and `UserManagerTest` (5).

## Shared Patterns

### Error display
**Source:** `screens/login/AuthErrorDialog`, `toDialogError()`, `FieldError`, and `toSectionMessage()` from `makeabooking`.
**Apply to:** every ViewModel and screen in this phase. Section-level failures render `Failed` + Retry. Operation failures render `AuthErrorDialog(message, onDismiss, title)`.

### Call guard
**Source:** `managers/CallGuard.kt`. `attempt(action)` is for user-initiated Save. `isCallActive.value` is for silent paths.

### Repository and API
**Source:** `PaymentMethodsRepository.kt`. A `@Singleton` class with an `@Inject` constructor, wrapping `apiCall { api.x() }.data`.

### Money
**Source:** `util/Money.kt` (`Money.format(Long, code)`). Amounts are minor units. The floor comes from `MinSessionAmount.minMinorUnits`. `pricing_limits` is read from `UserManager.me.value` and never persisted. Refresh once with `runCatching { userManager.refreshMe() }` if it is null.

### Navigation
**Source:** `Navigator`, `BottomBar.hidden()`, Nav3 `NavKey` data objects and classes marked `@Serializable`.

### Design gate
`scripts/check-design-rules.sh` forbids `Color(0x...)` outside `ui/theme/Color.kt`. Use `MaterialTheme.colorScheme`, `Spacing` and `MaterialTheme.extras`.

## No Analog Found

| File | Role | Data Flow | Reason |
|---|---|---|---|
| `topics/TopicEvents.kt` | utility | event-driven (Channel) | No singleton one-shot channel exists. Use RESEARCH Pattern 3 (`Channel<TopicSavedEvent>(BUFFERED)`). |
| `NullPreservingAdapters.kt` | utility | transform | `InstantAdapter` is the only custom adapter, and it is not a request writer. Use the RESEARCH adapter example. |

Partial matches (written from RESEARCH code): the scroll-to-center two-column time picker (no existing analog), and the SavedStateHandle string encoding for the schedule (Pitfall 10).

## Metadata

**Analog search scope:** `managers/`, `network/`, `model/`, `components/`, `constants/`, `util/`, `screens/settings/paymentmethods`, `screens/clientmode/{home,cancellation,mybookings}`, `screens/signup/{tags,timezone}`, `screens/expertmode/home`
**Files scanned:** about 30
**Pattern extraction date:** 2026-10-24
