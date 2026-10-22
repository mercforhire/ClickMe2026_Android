# Phase 6: Booking Flow & Free Bookings - Pattern Map

**Mapped:** 2026-10-22
**Files analyzed:** 30 (new or modified)
**Analogs found:** 28 / 30

All paths are under `app/src/main/java/com/uptrendinvestments/clickmeapp/` (main) or `app/src/test/java/com/uptrendinvestments/clickmeapp/` (test) unless noted.

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match |
|---|---|---|---|---|
| `network/ApiRequestUrls.kt` (modify: +3 consts) | config | request-response | same file, Phase 5 block (lines 30-35) | exact |
| `network/ClickMeApi.kt` (modify: +3 methods) | service (API iface) | request-response | same file, `getExpertDetails`/`getClientBookings` (lines ~136-156) | exact |
| `model/responses/ExpertAvailabilityData.kt` | model (DTO) | request-response | `model/responses/PublicProfileDetailsData` (Phase 5 DTOs) | exact |
| `model/responses/BookingRequestData.kt` | model (DTO) | request-response | same | exact |
| `model/responses/ClientBookingDetail.kt` | model (DTO) | request-response | `model/responses/ClientBookingsPayload` | exact |
| `model/requests/BookingRequestBody.kt` | model (request) | request-response | `SetupExpertProfileRequest` / `LoginRequest` in `model/requests` | role-match |
| `navigation/Navigator.kt` (modify: `replaceTop`, `resetAndSelect`) | utility (nav) | event-driven | same file, `reselect`/`navigateInTab`/`goBack` | exact |
| `screens/clientmode/makeabooking/BookingRepository.kt` | service | request-response | `screens/clientmode/expertprofile/ExpertProfileRepository.kt` | exact |
| `screens/clientmode/mybookings/ClientBookingsRepository.kt` (modify: +`detail`) | service | request-response | same file `list` | exact |
| `makeabooking/MakeABookingViewModel.kt` | viewmodel | CRUD + request-response | `expertprofile/ExpertProfileViewModel.kt` | exact (plus SavedStateHandle from `LoginViewModel`) |
| `makeabooking/MakeABookingUiState.kt` | model | state | `expertprofile/ExpertProfileUiState.kt` | exact |
| `makeabooking/MakeABookingScreen.kt` (rename from Stub) | component (screen) | request-response | `expertprofile/ExpertProfileScreen.kt` | exact |
| `makeabooking/subviews/*` (ExpertCard, TopicPicker + sheet, CalendarCard, TimeSlotsCard, MeetingTypeCard, NotesCard, SummarySection, BookButton) | component | transform | `expertprofile/subviews/*`, `components/PickerSheet.kt`, `SelectableChip.kt`, `SectionHeader.kt` | role-match |
| `makeabooking/BookingSlots.kt` (pure `groupSlots`) | utility | transform | `mybookings/TodaysSessions.kt` (pure fns taking `Clock`/`ZoneId`) | role-match |
| `makeabooking/BookingCalendar.kt` (pure `monthCells`) | utility | transform | `TodaysSessions.kt` | role-match |
| `makeabooking/BookingTimezoneNote.kt` | utility | transform | `TodaysSessions.formatSessionTimeRange` | partial |
| `makeabooking/BookingErrors.kt` (`toBookingFailure`) | utility | transform | `screens/login/FieldError.kt` `toDialogError` | exact |
| `requestsent/RequestSentKey.kt` | model (NavKey) | n/a | `makeabooking/BookingKey.kt` | exact |
| `requestsent/BookingRequestSentViewModel.kt` | viewmodel | request-response | `ExpertProfileViewModel.kt` (single load + retry) | exact |
| `requestsent/BookingRequestSentScreen.kt` + `subviews/` (CheckIcon, Headline, DetailsCard, Actions) | component | request-response | `ExpertProfileScreen.kt`, `components/EmptyState.kt` `ErrorState`, `SectionAppear.kt` | role-match |
| `screens/clientmode/home/HomeClientScreen.kt` (modify) | config (wiring) | event-driven | same file, `detailEntries` ExpertProfileKey entry (lines 80-92) | exact |
| `res/values/strings_booking.xml` | config | n/a | `res/values/strings_expert_profile.xml` | exact |
| test `testing/FakeClickMeApi.kt` (modify) | test util | n/a | same file | exact |
| test `network/ApiRequestUrlsTest.kt` (extend) | test | n/a | same file | exact |
| test `navigation/NavigatorTest.kt` (extend) | test | n/a | same file | exact |
| test `screens/HomeShellsTest.kt` (rewrite one test) | test | n/a | same file | exact |
| test `MakeABookingViewModelTest`, `BookingRequestSentViewModelTest` | test | n/a | `expertprofile/ExpertProfileViewModelTest.kt` | exact |
| test `BookingSlotsTest`, `BookingCalendarTest`, `BookingTimezoneNoteTest` | test | n/a | pure-fn tests (e.g. a `TodaysSessions` test; none read) | partial |
| test `MakeABookingScreenTest`, `BookingRequestSentScreenTest` | test | n/a | `expertprofile/ExpertProfileScreenTest.kt` | exact |
| delete `MakeABookingStubScreen.kt` + its test + `make_a_booking_coming_soon_*` strings | n/a | n/a | n/a | n/a |

## Pattern Assignments

### API plumbing: `ApiRequestUrls.kt` and `ClickMeApi.kt`

**Analog:** same files, Phase 5 additions.

`ApiRequestUrls.kt` lines 30-35: add a `// Phase 6` block in the same style. Relative paths, no leading slash, `{id}` placeholders:
```kotlin
// Phase 5: discovery (home, client bookings, categories, expert details/reviews).
const val CLIENT_BOOKINGS = "client/bookings"
const val EXPERT_DETAILS = "experts/{id}/details"
```
New: `EXPERT_AVAILABILITY = "experts/{id}/availability"`, `BOOKING_REQUEST = "bookings/request"`, `CLIENT_BOOKING_DETAIL = "client/bookings/{id}"`. Do NOT add to `publicPaths` (lines 42-49); they must carry the bearer. Update the KDoc header ("Phase 6 adds ...").

`ClickMeApi.kt` (end of file): KDoc "Ports iOS ClickMeAPI+X", `@GET(ApiRequestUrls.X)`, `suspend`, wrapped in `SuccessDataResponse<T>`:
```kotlin
/** Ports iOS ClickMeAPI+Discovery getExpertDetails. */
@GET(ApiRequestUrls.EXPERT_DETAILS)
suspend fun getExpertDetails(
    @Path("id") expertId: String,
): SuccessDataResponse<PublicProfileDetailsData>
```
POST bodies use `@Body body: XRequest` (see `login`, `setupExpertProfile`). Add optional `@Query("topic_id")` param on availability only if Open Question 2 is answered yes.

### `ExpertAvailabilityData.kt`, `BookingRequestData.kt`, `ClientBookingDetail.kt`, `BookingRequestBody.kt`

**Analog:** Phase 5 DTOs in `model/responses` (Moshi codegen, explicit `@Json(name=...)` on multi-word fields, `Instant` parsed by `network/InstantAdapter.kt`). Copy the DTO shapes in 06-RESEARCH.md "Availability and detail DTOs" and "Request body" verbatim. They already follow project convention. Reuse `TopicPriceDto` in `BookingDetailTopicDto`. Put the detail DTO in `model/responses` (Research Open Question 6), not `mybookings`.

### `BookingRepository.kt` (service, request-response)

**Analog:** `screens/clientmode/expertprofile/ExpertProfileRepository.kt` (lines 1-28).
```kotlin
@Singleton
class ExpertProfileRepository
    @Inject
    constructor(
        private val api: ClickMeApi,
    ) {
        suspend fun details(expertId: String): PublicProfileDetailsData =
            apiCall { api.getExpertDetails(expertId) }.data
    }
```
Apply: `availability(expertId, month: YearMonth) = apiCall { api.getExpertAvailability(expertId, month.toString()) }.data`; `requestBooking(body) = apiCall { api.requestBooking(body) }.data`. Topics come from the existing `ExpertProfileRepository.details(id)` (inject both). Add `detail(id)` to `ClientBookingsRepository` the same way as its `list` (line 23: `apiCall { api.getClientBookings(...) }.data`). `apiCall` throws `ApiException` only and rethrows `CancellationException`; callers catch `ApiException` (never `runCatching` around suspend calls).

### `MakeABookingViewModel.kt` and `BookingRequestSentViewModel.kt` (viewmodel)

**Analog:** `screens/clientmode/expertprofile/ExpertProfileViewModel.kt` lines 34-83.

Assisted Hilt shape:
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

        private val _uiState = MutableStateFlow(ExpertProfileUiState(expertId = key.expertId, ...))
        val uiState: StateFlow<ExpertProfileUiState> = _uiState.asStateFlow()
        private var detailsJob: Job? = null
        init { load() }
        fun retry() = load()
```
Job cancel/relaunch + try/catch of `ApiException` (lines 74-98), copy for the latest-wins availability load (Research Pattern 2):
```kotlin
detailsJob?.cancel()
_uiState.update { it.copy(details = DetailsState.Loading) }
detailsJob = viewModelScope.launch { loadDetails(expertId) }
...
} catch (e: ApiException) { _uiState.update { it.copy(details = failed) } }
```
Mapping a `TopicOfDiscussionDto` to UI with `PriceDisplay.of(price.isFree, price.amount, price.currency)` (lines 141-148); reuse it for topic rows.

SavedStateHandle (Research A1, unverified with `@AssistedInject`): the only in-repo precedent for state persistence is `screens/login/LoginViewModel.kt` (`private companion object { const val EMAIL_KEY = "login_email" }`). Persist primitives only (topic id, `LocalDate.toString()`, `YearMonth.toString()`, slot epoch millis, meeting-type wire string, notes). First task should compile this; fallback is `rememberSaveable`. Inject `Clock` (see `ClockModule`, `MutableClock` in tests).

Error mapping: dialog error flows through `_uiState.update { it.copy(dialogError = e.toDialogError()) }` (LoginViewModel line 159). Phase 6 replaces this with `toBookingFailure()` (see below).

Single-flight: `if (state.isSubmitting) return` at the top of `book()`. Success is exposed as a one-shot (`bookingId`) consumed by the screen's `onBooked`.

### `MakeABookingUiState.kt`
**Analog:** `expertprofile/ExpertProfileUiState.kt`. Sealed per-section load states (`Loading`/`Loaded`/`Failed`), as `DetailsState`/`ReviewsState`. Use `AvailabilityState { Loading, Loaded(map), Failed(message) }`.

### `BookingErrors.kt`
**Analog:** `screens/login/FieldError.kt` lines 11-45 (`FieldError.Res` / `FieldError.Text`, `ApiException.toDialogError()` returns null for `ACCOUNT_SUSPENDED`, which the global dialog owns):
```kotlin
fun ApiException.toDialogError(): FieldError? =
    when (this) {
        is ApiException.Http ->
            when {
                code == ErrorCode.AccountSuspended -> null
                else -> userMessage?.let { FieldError.Text(it) } ?: FieldError.Res(R.string.login_error_unknown)
```
Copy the `toBookingFailure()` body from 06-RESEARCH.md "Error mapping with side effects". `ErrorCode.SlotUnavailable` and `MeetingLinkUnavailable` exist in `model/enums/ErrorCode.kt` (lines 38, 49). Verify `ExpertNotBookable`, `CannotBookSelf`, `PaidTopicRequiresPayment`, `DuplicateBooking` exist there before use (Research says they do). Reuse `login_error_network` / `login_error_unknown` strings.

### `MakeABookingScreen.kt` (screen) and Request Sent screen

**Analog:** `expertprofile/ExpertProfileScreen.kt` lines 70-150.
- Hilt-aware wrapper + stateless content split: `ExpertProfileScreen(viewModel, onBack, onBook)` collects `uiState.collectAsStateWithLifecycle()` and calls `ExpertProfileContent(state, onBack, ...)`. Keep this split so Compose tests drive `...Content` directly.
- `Scaffold` + `TopAppBar` with back arrow: `Icons.AutoMirrored.Filled.ArrowBack` inside `IconButton(onClick = onBack)`, content description "Back" (HomeShellsTest finds it via `onNodeWithContentDescription("Back")`).
- Error alert: `AuthErrorDialog(message, onDismiss, title = "Couldn't book")` from `screens/login/AuthErrorDialog.kt` (lines 17-51), with `FieldError.asString()` for the message. Lift to `components/` only if the UI-SPEC requires.
- Request Sent loading/error: `components/EmptyState.kt` `ErrorState` (line 79) with Retry; `SectionAppear.kt` and `Crossfade(tween)` for transitions.
- Past-slot and time labels: `formatSessionTimeRange(start, end, zone, DateFormat.getTimeFormat(context))` from `mybookings/TodaysSessions.kt` lines 33-45; don't write a new formatter.

Reusable components (no analog needed, use as-is): `components/PickerSheet.kt` (topic sheet), `SelectableChip.kt`, `SectionHeader.kt`, `SkeletonBlock.kt`/`SkeletonGroup`, `SectionError.kt`, `EmptyState.kt`, `PriceText.kt`, `avatar/Avatar.kt`, `buttons/` PressScaleButton.

### Subviews
One composable per file, named per 06-UI-SPEC, in `makeabooking/subviews/` and `requestsent/subviews/`. Follow `expertprofile/subviews/ExpertProfile*Card.kt` naming, including a `...Skeleton` sibling when the card has a loading shape (see `ExpertProfileAboutCardSkeleton`). Calendar: `Column` of `Row`s, never `LazyVerticalGrid`.

### Pure logic files (`BookingSlots.kt`, `BookingCalendar.kt`, `BookingTimezoneNote.kt`)
**Analog:** `mybookings/TodaysSessions.kt` (top-level pure functions taking `Clock` and `ZoneId` as parameters, no `ZoneId.systemDefault()` inside):
```kotlin
fun List<ClientBookingItem>.todaysSessions(clock: Clock, zone: ZoneId): List<ClientBookingItem> {
    val today = LocalDate.now(clock.withZone(zone))
    return filter { ... it.startTime.atZone(zone).toLocalDate() == today }.sortedBy { it.startTime }
}
```
Copy `groupSlots`, `monthCells`, `timezoneNote` bodies from 06-RESEARCH.md Patterns 1, 3 and "Timezone note".

### `RequestSentKey.kt`
**Analog:** `makeabooking/BookingKey.kt` lines 11-17:
```kotlin
@Serializable
data class BookingKey(val expertId: String, ...) : NavKey
```
`@Serializable data class RequestSentKey(val bookingId: String) : NavKey`.

### `Navigator.kt` (`replaceTop`, `resetAndSelect`)
**Analog:** same file. Mutations always set `state.lastTransition` then edit the stack (`goBack` lines 54-63, `reselect` 71-76, `navigateInTab` 83-91). `require(tab in state.backStacks)` for validation. Copy the bodies in 06-RESEARCH.md Pattern 5. "Back to Home" reuses existing `navigator.reselect(ExploreRoot)`.

### `HomeClientScreen.kt` (wiring)
**Analog:** lines 80-92, the `detailEntries` ExpertProfileKey entry:
```kotlin
detailEntries = { navigator ->
    entry<ExpertProfileKey> { key ->
        val profileViewModel =
            hiltViewModel<ExpertProfileViewModel, ExpertProfileViewModel.Factory>(
                creationCallback = { factory -> factory.create(key) },
            )
        ExpertProfileScreen(viewModel = profileViewModel, onBack = navigator::goBack, onBook = { navigator.navigate(it) })
    }
},
```
Changes: delete `entry<BookingKey>(metadata = BottomBar.hidden()) { MakeABookingStubScreen(...) }` at lines 194-196 and the stub import (line 45); update the KDoc at lines 125-129 (it says the booking stub is registered in the shell). Register `entry<BookingKey>(metadata = BottomBar.hidden())` and `entry<RequestSentKey>(metadata = BottomBar.hidden())` inside `detailEntries`, each with an assisted `hiltViewModel`. `onBooked = { navigator.replaceTop(RequestSentKey(it)) }`; Request Sent: `onViewBookings = { navigator.resetAndSelect(ClientBookingsRoot, setOf(ExploreRoot, ClientBookingsRoot)) }`, `onHome = { navigator.reselect(ExploreRoot) }`. Registering `BookingKey` twice is an error.

### Strings
**Analog:** `res/values/strings_expert_profile.xml`. New file `strings_booking.xml`; all copy in resources (no hard-coded text, no `fontSize`, no `Color(...)` literals; `scripts/check-design-rules.sh` enforces).

### Test patterns

**`FakeClickMeApi.kt`** (lines 40-60, 135-137, 225-250): add per-endpoint handler var, `@Volatile var xCalls = 0; private set`, and the override. Adding `ClickMeApi` methods breaks this fake until done.
```kotlin
var expertDetailsHandler: suspend (String) -> PublicProfileDetailsData = { notStubbed() }
@Volatile var expertDetailsCalls = 0
    private set
override suspend fun getExpertDetails(expertId: String): SuccessDataResponse<PublicProfileDetailsData> {
    expertDetailsCalls++
    return ok(expertDetailsHandler(expertId))
}
```
Record request bodies/queries in a list (like `_setupProfileRequests`) for `requestBookingRequests` and `availabilityMonths`. Errors: `api.httpError(409, """{"status":"error","code":"SLOT_UNAVAILABLE","message":"..."}""")`.

**ViewModel tests** (`ExpertProfileViewModelTest.kt`): `@get:Rule val main = MainDispatcherRule()`, `runTest(main.testDispatcher)`, construct the VM directly (`ExpertProfileViewModel(key, ExpertProfileRepository(api))`), hold calls open with `CompletableDeferred<T>().await()` plus `runCurrent()` (lines 83-130). Use this for the stale-month-cancel test. Add `MutableClock` from `testing/`. For SavedStateHandle, build it by hand: `SavedStateHandle()`.

**`ApiRequestUrlsTest.kt`**: add new paths to the `otherPaths_needAuthToken` list (`"/api/v1/experts/e1/availability"`, `"/api/v1/bookings/request"`, `"/api/v1/client/bookings/b1"`) and a `phaseSixConstants_haveExactRelativeValues` test, same style as `phaseFourConstants_...`.

**`NavigatorTest.kt`**: `newNavigator()` builds `NavigationState` with Explore/Bookings/Chats stacks and `Navigator.stack(tab)` helper (lines 31-48); add `replaceTop` (swaps top, `lastTransition == Push`, throws at root) and `resetAndSelect` tests.

**`HomeShellsTest.kt`** lines 296-309: `bookingRouteShowsStubWithoutBottomBarAndBackRestoresIt` asserts "Book a Session" / "Booking is coming soon". Rewrite it via the existing `setClientWithExploreButton(..., detail = { entry<BookingKey>(metadata = BottomBar.hidden()) { ... } })` hook (lines 266-285), keeping the "no bottom bar, Back restores it" assertions. Add a similar one for `RequestSentKey`.

## Shared Patterns

### Assisted ViewModel creation in Nav3 entry
**Source:** `HomeClientScreen.kt` lines 81-91. **Apply to:** BookingKey and RequestSentKey entries.

### Error handling (ApiException only)
**Source:** `ExpertProfileViewModel.kt` lines 85-97, `FieldError.kt`. **Apply to:** all Phase 6 ViewModels. Catch `ApiException`, never `Throwable`/`runCatching` around suspend calls. `ACCOUNT_SUSPENDED` stays silent.

### Time formatting
**Source:** `TodaysSessions.kt` `formatSessionTimeRange`. **Apply to:** slot chips, summary, Request Sent details card.

### Prices
**Source:** `util/PriceDisplay.kt` (`PriceDisplay.of(isFree, amount, currency)`), `components/PriceText.kt`. **Apply to:** topic picker rows, summary.

### Loading/error UI
**Source:** `SkeletonBlock.kt`, `SectionError.kt`, `EmptyState.kt`. **Apply to:** topics, slots, Request Sent.

### Error dialog
**Source:** `screens/login/AuthErrorDialog.kt`. **Apply to:** booking failures (title "Couldn't book").

## No Analog Found

| File | Role | Data Flow | Reason |
|---|---|---|---|
| `makeabooking/BookingCalendar.kt` month grid composable (`CalendarCard`) | component | transform | No calendar exists; use Research Pattern 3 (`monthCells`) and a `Column`/`Row` layout |
| `requestsent/` entry animation | component | event-driven | No staggered entry animation exists; use the Research "Entry animation" sketch; closest is `components/SectionAppear.kt` |
| `Navigator.replaceTop` / `resetAndSelect` semantics | utility | event-driven | New operations; closest is `reselect`/`navigateInTab` |
| Pure-fn golden tests (`BookingSlotsTest` etc.) | test | transform | Closest is any existing `TodaysSessions` test (not read); use the zone matrix in Research Validation Architecture |

## Notes for the planner

- D-08 premise is stale: `PublicProfileDetailsData.availableMeetingTypes` already exists (Research Open Question 1). Decision pending; keep the 422 handler either way.
- Research A1 (`SavedStateHandle` with `@AssistedInject`) is unverified; make it Wave 0 / first ViewModel task.
- `HomeClientScreen.kt` KDoc and the stub's strings/tests must be cleaned up when the stub is deleted.

## Metadata

**Analog search scope:** `navigation/`, `network/`, `screens/clientmode/{expertprofile,home,makeabooking,mybookings,explore}`, `screens/login/`, `components/`, `util/`, and the matching test and `testing/` dirs.
**Files scanned:** about 25 read in full or in part.
**Pattern extraction date:** 2026-10-22
