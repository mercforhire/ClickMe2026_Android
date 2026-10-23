# Phase 7: Payment Methods & Paid Booking - Pattern Map

**Mapped:** 2026-10-22
**Files analyzed:** 38 (new and modified)
**Analogs found:** 36 / 38

All paths below are under `app/src/main/java/com/uptrendinvestments/clickmeapp/` unless prefixed `app/src/test/`, `app/`, `spikes/` or `scripts/`.

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|---|---|---|---|---|
| `network/ApiRequestUrls.kt` (modify: 6 consts) | config | request-response | same file, Phase 6 group (lines 37-40) | exact |
| `network/ClickMeApi.kt` (modify: 7 methods) | service (Retrofit) | request-response | same file, `requestBooking` / `getClientBookingDetail` (lines 174-184) | exact |
| `model/requests/PaymentRequests.kt` (`PaymentIntentRequest`, `ConfirmBookingRequest`) | model | request-response | `model/requests/BookingRequests.kt` | exact |
| `model/responses/PaymentIntentResponse.kt` (BARE, no envelope) | model | request-response | `model/responses/BookingRequestData.kt` | role-match (no envelope) |
| `model/responses/ConfirmBookingData.kt` | model | request-response | `model/responses/BookingRequestData.kt` | exact |
| `model/responses/PaymentMethodsData.kt`, `PaymentMethodItem`, `PaymentMethodSetupData` | model | CRUD | `model/responses/BookingRequestData.kt` + `SuccessDataResponse.kt` | exact |
| `screens/clientmode/makeabooking/BookingRepository.kt` (modify: paymentIntent, confirm) | service | request-response | same file | exact |
| `screens/settings/paymentmethods/PaymentMethodsRepository.kt` | service | CRUD | `screens/clientmode/expertprofile/ExpertProfileRepository.kt` | exact |
| `stripe/StripePaymentMethodReader.kt` (interface + Default impl) | service | request-response | `di/BookingModule.kt` (@Binds seam) + `CompletedBookingStore` interface | role-match |
| `di/StripeModule.kt` | config (DI) | n/a | `di/BookingModule.kt` | exact |
| `ClickMeApplication.kt` (modify: `PaymentConfiguration.init`) | config | n/a | `spikes/stripe/.../MainActivity.kt` line 45 | exact |
| `app/build.gradle.kts` (modify: add dependency, swap dev key) | config | n/a | existing `implementation(libs...)` lines + STRIPE_PUBLISHABLE_KEY lines 47, 58 | exact |
| `screens/clientmode/makeabooking/PaidFlowError.kt` (pure `handlePaidFlowError`) | utility | transform | `screens/clientmode/makeabooking/BookingErrors.kt` | exact |
| `screens/clientmode/makeabooking/PaidBookingFlow.kt` (step model, `StripeRequest`, `PaymentMethodsAvailability`) | model | event-driven | `MakeABookingUiState.kt` (sealed ifaces `TopicsState`, `AvailabilityState`) | role-match |
| `screens/clientmode/makeabooking/MakeABookingViewModel.kt` (modify: paid path) | viewmodel | request-response + event-driven | same file (`book()`, `handleBookingFailure`, `loadTopics`) | exact |
| `screens/clientmode/makeabooking/MakeABookingUiState.kt` (modify) | model | n/a | same file (`BookDisabledReason`, `bookDisabledReason`) | exact |
| `screens/clientmode/makeabooking/PaidFlowStripeHost.kt` | component (Compose host) | event-driven | `spikes/stripe/.../MainActivity.kt` (`SpikeScreen`) | exact |
| `screens/clientmode/makeabooking/MakeABookingScreen.kt` (modify) | component | request-response | same file (`LaunchedEffect(completedBookingId)`, `LifecycleEventEffect`) | exact |
| `screens/clientmode/makeabooking/subviews/MakeABookingBookButton.kt` (modify: CTA switch) | component | request-response | same file | exact |
| `screens/clientmode/makeabooking/subviews/PaymentInfoSheet.kt` | component | n/a | `components/PickerSheet.kt` / `subviews/MakeABookingTopicSheet.kt` (ModalBottomSheet) | role-match |
| `screens/clientmode/makeabooking/subviews/MakeABookingPaymentInfoLink.kt` | component | n/a | `subviews/MakeABookingSummarySection.kt` | role-match |
| `screens/clientmode/makeabooking/subviews/MakeABookingSummarySection.kt` (modify: price) | component | n/a | same file + `util/PriceDisplay.kt` / `Money.kt` | exact |
| `components/PaymentLifecycleCard.kt` (shared steps) | component | n/a | `screens/clientmode/requestsent/subviews/BookingRequestSentDetailsCard.kt` | role-match |
| `screens/clientmode/requestsent/BookingRequestSentScreen.kt` (modify: paid card) | component | request-response | same file (`LoadedBody` Column, `fadeSlideUp` blocks) | exact |
| `screens/clientmode/requestsent/subviews/BookingRequestSentHeadline.kt` (modify: paid headline) | component | n/a | same file | exact |
| `screens/clientmode/requestsent/BookingRequestSentUiState.kt` (modify: `isFree` is already carried) | model | n/a | same file | exact |
| `screens/settings/paymentmethods/PaymentMethodsKey.kt` | route key | n/a | `screens/clientmode/makeabooking/BookingKey.kt` | exact |
| `screens/settings/paymentmethods/PaymentMethodsUiState.kt` | model | CRUD | `makeabooking/MakeABookingUiState.kt` (`TopicsState` sealed) | role-match |
| `screens/settings/paymentmethods/PaymentMethodsViewModel.kt` | viewmodel | CRUD | `makeabooking/MakeABookingViewModel.kt` (`loadTopics`, `handleBookingFailure`) | role-match |
| `screens/settings/paymentmethods/PaymentMethodsScreen.kt` | component | CRUD | `requestsent/BookingRequestSentScreen.kt` (Scaffold + Loading/Failed/Loaded + `AuthTopAppBar`) | exact |
| `screens/settings/paymentmethods/subviews/PaymentMethodRow.kt` | component | CRUD | `makeabooking/subviews/MakeABookingBookButton.kt` (spinner/semantics idiom) | partial |
| `screens/settings/paymentmethods/subviews/PaymentMethodBrandDisplay.kt` (functions only) | utility | transform | `util/Money.kt` / `BookingFormatting.kt` (top-level fns) | partial |
| `screens/settings/paymentmethods/subviews/PaymentMethodRemoveDialog.kt` | component | n/a | `screens/login/AuthErrorDialog.kt` | role-match |
| `screens/clientmode/home/ClientBookingFlowEntries.kt` / `HomeClientScreen.kt` (modify: register key, Profile row) | route/config | n/a | same files | exact |
| `components/ProfileMenuRow.kt` | component | n/a | `components/PlaceholderTabContent.kt` footer slot | partial |
| `app/src/test/.../testing/FakeClickMeApi.kt` (modify) | test | n/a | same file | exact |
| `app/src/test/.../testing/FakeStripePaymentMethodReader.kt` | test | n/a | `testing/FakeCompletedBookingStore.kt` | exact |
| `app/src/test/.../makeabooking/PaidFlowErrorTest.kt` | test | transform | `makeabooking/BookingErrorsTest.kt` | exact |
| `app/src/test/.../makeabooking/MakeABookingViewModelPaidTest.kt`, `...ScaTest.kt` | test | request-response | `makeabooking/MakeABookingViewModelBookTest.kt` + `BookingTestFixtures.kt` | exact |
| `app/src/test/.../paymentmethods/PaymentMethodsViewModelTest.kt`, `PaymentMethodsScreenTest.kt` | test | CRUD | `requestsent/BookingRequestSentViewModelTest.kt`, `BookingRequestSentScreenTest.kt` | role-match |

## Pattern Assignments

### `network/ApiRequestUrls.kt` and `network/ClickMeApi.kt` (config / service, request-response)

**Analog:** same files, Phase 6 group.

Constants (ApiRequestUrls.kt lines 37-40): add a "Phase 7" block in the same shape.
```kotlin
// Phase 6: booking (availability, free booking request, client booking detail).
const val EXPERT_AVAILABILITY = "experts/{id}/availability"
const val BOOKING_REQUEST = "bookings/request"
const val CLIENT_BOOKING_DETAIL = "client/bookings/{id}"
```
New: `PAYMENT_INTENT = "bookings/payment-intent"`, `BOOKING_CONFIRM = "bookings/confirm"`, `PAYMENT_METHODS = "me/payment-methods"`, `PAYMENT_METHODS_SETUP_INTENT = "me/payment-methods/setup-intent"`, `PAYMENT_METHOD = "me/payment-methods/{id}"`, `PAYMENT_METHOD_DEFAULT = "me/payment-methods/{id}/default"`. Also update the KDoc header at lines 7-8.

API methods (ClickMeApi.kt lines 174-184):
```kotlin
/** Ports iOS ClickMeAPI+Booking requestBooking. Free path only; 201 on success. */
@POST(ApiRequestUrls.BOOKING_REQUEST)
suspend fun requestBooking(
    @Body body: BookingRequestBody,
): SuccessDataResponse<BookingRequestData>

@GET(ApiRequestUrls.CLIENT_BOOKING_DETAIL)
suspend fun getClientBookingDetail(
    @Path("id") bookingId: String,
): SuccessDataResponse<ClientBookingDetail>
```
Deviations to apply:
- `paymentIntent` returns the BARE `PaymentIntentResponse` (no `SuccessDataResponse`).
- `confirmBooking` returns `SuccessDataResponse<ConfirmBookingData>`.
- `createSetupIntent` and `setDefaultPaymentMethod` take `@Body body: Map<String, String> = emptyMap()` so the strict `{}` schema passes (RESEARCH Pitfall 13). `@PATCH` for default, `@DELETE` returns `SuccessMessageResponse`.
- Add the new methods to `FakeClickMeApi` (handlers + call counters + recorded bodies).

---

### DTOs: `model/requests/PaymentRequests.kt`, `model/responses/{PaymentIntentResponse,ConfirmBookingData,PaymentMethods*}.kt` (model, request-response)

**Analog:** `model/requests/BookingRequests.kt` and `model/responses/BookingRequestData.kt`.

Request pattern (BookingRequests.kt lines 1-24): file starts with `@file:Suppress("ktlint:standard:filename")`, KDoc stating the backend schema is strict.
```kotlin
@JsonClass(generateAdapter = true)
data class BookingRequestBody(
    @Json(name = "expert_id") val expertId: String,
    ...
    @Json(name = "client_notes") val clientNotes: String? = null,
)
```
Rules to carry over: explicit `@Json(name=...)` on every snake_case key; nullable fields default `= null` because the converter omits nulls and the strict schema rejects explicit `null` (needed for `payment_intent_id` on the long-lead confirm). `ConfirmBookingRequest` must NOT contain `booking_attempt_id`.

Response pattern (BookingRequestData.kt):
```kotlin
@JsonClass(generateAdapter = true)
data class BookingRequestData(
    @Json(name = "booking_id") val bookingId: String,
    val status: String? = null,
    val message: String? = null,
)
```
`PaymentIntentResponse` must not declare `total_amount` / `summary.*` (decimal major units; a `Long` adapter would throw).

---

### Repositories: `BookingRepository.kt` (modify) and `PaymentMethodsRepository.kt` (new)

**Analog:** `screens/clientmode/makeabooking/BookingRepository.kt` lines 1-34 and `ExpertProfileRepository.kt`.
```kotlin
@Singleton
class BookingRepository
    @Inject
    constructor(
        private val api: ClickMeApi,
    ) {
        suspend fun requestBooking(body: BookingRequestBody): BookingRequestData =
            apiCall { api.requestBooking(body) }.data
    }
```
Copy: `@Singleton` + `@Inject constructor(api: ClickMeApi)`, every call wrapped in `apiCall { }`, `.data` unwrap, KDoc "throws ApiException only". Add `paymentIntent(req)` (no `.data`, bare body) and `confirm(req)`. Update the stale KDoc on `requestBooking` ("Phase 7 is later").

---

### `stripe/StripePaymentMethodReader.kt` + `di/StripeModule.kt` (service + DI)

**Analog for DI:** `di/BookingModule.kt`.
```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class BookingModule {
    @Binds
    abstract fun bindCompletedBookingStore(impl: DataStoreCompletedBookingStore): CompletedBookingStore
}
```
Copy as `abstract class StripeModule { @Binds abstract fun bindReader(impl: DefaultStripePaymentMethodReader): StripePaymentMethodReader }`. Interface + Default impl follows the `CompletedBookingStore` / `DataStoreCompletedBookingStore` split. Impl body: RESEARCH Pattern 4 (`Stripe(context, AppConfig.stripePublishableKey).retrieveSetupIntentSynchronous(secret).paymentMethodId` inside `withContext(Dispatchers.IO)`, catch `StripeException`/`IOException` returning null). Use `AppConfig.stripePublishableKey` (`config/AppConfig.kt` line 13), not `BuildConfig` directly.

Test fake analog: `app/src/test/.../testing/FakeCompletedBookingStore.kt`.

---

### `ClickMeApplication.kt` (modify) and `app/build.gradle.kts` (modify)

**Analog:** `spikes/stripe/.../MainActivity.kt` line 45: `PaymentConfiguration.init(applicationContext, BuildConfig.STRIPE_PUBLISHABLE_KEY)`.
Place in `ClickMeApplication.onCreate()` after `super.onCreate()` (before the realtime start), as `PaymentConfiguration.init(this, AppConfig.stripePublishableKey)`. Add `implementation(libs.stripe.paymentsheet)` to `:app` (the catalog entry exists, `:app` does not use it yet). Re-run `assembleProdRelease` (R8). The dev key lives in `app/build.gradle.kts` lines ~47 and ~58 (`STRIPE_PUBLISHABLE_KEY`), swapped when D-01 resolves.

---

### `makeabooking/PaidFlowError.kt` (utility, transform)

**Analog:** `screens/clientmode/makeabooking/BookingErrors.kt` (whole file, 72 lines).

Sealed result + extension on `ApiException` + `when (code)` over `ErrorCode`:
```kotlin
sealed interface BookingFailure {
    data object Silent : BookingFailure
    data class Show(
        val message: FieldError,
        val clearSlotAndReload: Boolean = false,
        val resetMeetingType: Boolean = false,
    ) : BookingFailure
}

fun ApiException.toBookingFailure(): BookingFailure =
    when (this) {
        is ApiException.Http ->
            when (code) {
                ErrorCode.AccountSuspended -> BookingFailure.Silent
                ErrorCode.SlotUnavailable ->
                    BookingFailure.Show(
                        FieldError.Res(R.string.make_a_booking_error_slot_taken),
                        clearSlotAndReload = true,
                    )
                ErrorCode.MeetingLinkUnavailable -> BookingFailure.Show(FieldError.Res(R.string.make_a_booking_error_no_link), resetMeetingType = true)
                ErrorCode.ExpertNotBookable -> BookingFailure.Show(FieldError.Res(R.string.make_a_booking_error_not_bookable))
                ErrorCode.CannotBookSelf -> ...
                ErrorCode.DuplicateBooking -> ...
                else ->
                    BookingFailure.Show(
                        userMessage?.let { FieldError.Text(it) } ?: FieldError.Res(R.string.login_error_unknown),
                    )
            }
        is ApiException.Network -> BookingFailure.Show(FieldError.Res(R.string.login_error_network))
        is ApiException.Decoding -> BookingFailure.Show(FieldError.Res(R.string.login_error_unknown))
    }
```
Extend into `ApiException.toPaidFailure(phase, paymentIntentId)` with extra fields (`newAttemptId`, `startChallenge`, `paymentReference`). Table is in RESEARCH Pattern 5. Rules to copy: server text only via `FieldError.Text` (T-06-13, plain text); `AccountSuspended` -> Silent; reuse the existing `make_a_booking_error_*` string resources for slot taken, no link, not bookable, self and duplicate; the 402 `detail` map comes from `ApiException.Http.detail` (`network/ApiException.kt` line 20, `Map<String, Any?>?`). All 15 `ErrorCode` cases already exist (`model/enums/ErrorCode.kt` lines 58-92). D-19: add the My Bookings copy variant for `SlotUnavailable` after an unknown-confirm outcome with the same attempt id.

Test analog: `BookingErrorsTest.kt` lines 1-60: private `http(status, code, message)` helper building `ApiException.Http(... detail = null)`, `assertEquals(BookingFailure.Show(res(R.string...), clearSlotAndReload = true), failure)`. Copy the helper (add a `detail` parameter) for `PaidFlowErrorTest`.

---

### `MakeABookingViewModel.kt` (viewmodel, request-response + event-driven) - modify

**Analog:** same file. Key excerpts to extend:

Constructor (lines 54-64): add `PaymentMethodsRepository` and `StripePaymentMethodReader` (assisted-injection stays; update `BookingTestFixtures.newViewModel` lines 65-78 and every caller).
```kotlin
@HiltViewModel(assistedFactory = MakeABookingViewModel.Factory::class)
class MakeABookingViewModel
    @AssistedInject
    constructor(
        @Assisted key: BookingKey,
        private val savedStateHandle: SavedStateHandle,
        private val profiles: ExpertProfileRepository,
        private val repository: BookingRepository,
        private val completedBookings: CompletedBookingStore,
        private val clock: Clock,
    ) : ViewModel()
```

Single flight + clock sync + try/catch with `viewModelScope.launch` (lines 230-270): the paid path reuses this skeleton. The `if (!topic.isFree) return` at line 238 is the line to replace.
```kotlin
fun book() {
    if (_uiState.value.isSubmitting) return
    if (syncClock()) { explainSlotPassed(); return }
    val s = _uiState.value
    val topic = s.selectedTopic ?: return
    if (!topic.isFree) return
    val slot = s.selectedSlot ?: return
    _uiState.update { it.copy(isSubmitting = true) }
    val attemptId = UUID.randomUUID().toString()
    savedStateHandle[KEY_ATTEMPT_ID] = attemptId
    ...
    viewModelScope.launch {
        try {
            val result = repository.requestBooking(body)
            completedBookings.put(attemptId, result.bookingId)
            savedStateHandle[KEY_COMPLETED_BOOKING_ID] = result.bookingId
            _uiState.update { it.copy(completedBookingId = result.bookingId) }
        } catch (e: ApiException) {
            handleBookingFailure(e)
        }
    }
}
```
Naming caution (RESEARCH Pitfall 11b): the existing `KEY_ATTEMPT_ID = "booking_attempt_id"` (line 566) is the Phase 6 WR-01 recovery id. The new in-memory paid idempotency field must be a plain `private var bookingAttemptId` (never in `SavedStateHandle`, D-17). Use the recovery id (`KEY_ATTEMPT_ID` + `completedBookings.put`) around each `/bookings/confirm` call.

Failure handler with side effects (lines 273-295): extend to apply `newAttemptId`, `clearSlotAndReload` (calls `loadAvailability(displayedMonth, keepVisible = true)`), `resetMeetingType`.
```kotlin
_uiState.update {
    it.copy(
        isSubmitting = false,
        dialogError = failure.message,
        dialogTitle = R.string.make_a_booking_error_title,
        meetingType = if (failure.resetMeetingType) MeetingType.InAppVoice else it.meetingType,
        selectedSlotStart = if (failure.clearSlotAndReload) null else it.selectedSlotStart,
    )
}
```

Selection setters (lines 140-212) all start with `if (_uiState.value.isSubmitting) return`: add the D-11 "regenerate `bookingAttemptId`" in `selectTopic` (line 141) and `clearSlot()` / `selectSlot` (lines 190, 313). Keep `isSubmitting = true` while `paidStep != Idle` (Pitfall 11b).

Topic default (lines 541-546): change `topics.firstOrNull { it.isFree } ?: topics.firstOrNull()` to `topics.firstOrNull()` (D-10), and update the class KDoc D-02 text (lines 37-39).

Parallel load (lines 99-102, 335-341): `loadForm()` adds a payment-methods load; follow the `loadTopics()` pattern (lines 534-552: `Job` field, cancel previous, `try/catch ApiException`, load failure maps to `PaymentMethodsAvailability.None`). `refreshPaymentMethods()` must be idempotent (skip if a load is in flight), because `LifecycleEventEffect(ON_RESUME)` also fires on first composition and after Stripe activities (Pitfall 9).

Consume-on-launch event pattern (spike): see `PaidFlowStripeHost.kt` below. VM state field `stripeRequest: StripeRequest?` plus `onStripeRequestLaunched()`.

---

### `MakeABookingUiState.kt` (modify)

**Analog:** same file, lines 66-67 and 121-127.
```kotlin
enum class BookDisabledReason { NoTopic, PaidTopic, NoSlot }
...
val bookDisabledReason: BookDisabledReason?
    get() {
        val topic = selectedTopic ?: return BookDisabledReason.NoTopic
        if (!topic.isFree) return BookDisabledReason.PaidTopic
        val slot = selectedSlot
        return if (slot == null || !slot.isSelectable(now)) BookDisabledReason.NoSlot else null
    }
```
Replace `PaidTopic` with the availability-derived CTA (RESEARCH "CTA derivation": `PaymentMethodsAvailability { Unknown, None, Available }`; paid + Unknown -> disabled "Checking your payment methods", paid + None -> enabled "Add a Payment Method", paid + Available -> needs slot, "Book Now"). Add `paymentMethods: PaymentMethodsAvailability`, `paidStep`, `showPaymentInfo` fields beside `isSubmitting` (line 91). Sealed-state style for card data: copy `TopicsState` (lines 36-48). Test analog: `MakeABookingUiStateTest.kt`.

---

### `PaidFlowStripeHost.kt` (Compose host, event-driven)

**Analog:** `spikes/stripe/src/main/java/com/uptrendinvestments/clickmeapp/spike/stripe/MainActivity.kt` lines 55-87 (`SpikeScreen`).
```kotlin
val sheet = remember { PaymentSheet.Builder(viewModel::onSetupResult) }.build()

// stripeAccountId is a remember key inside rememberPaymentLauncher, so the challenge is set in state first and the
// launcher is driven from LaunchedEffect, never from the same click handler.
val launcher =
    rememberPaymentLauncher(
        BuildConfig.STRIPE_PUBLISHABLE_KEY,
        viewModel.challenge?.stripeAccount,
        viewModel::onPaymentResult,
    )

LaunchedEffect(viewModel.setupRequest) {
    viewModel.setupRequest?.let { request ->
        sheet.presentWithSetupIntent(
            request.clientSecret,
            PaymentSheet.Configuration.Builder("ClickMe")
                .customer(PaymentSheet.CustomerConfiguration(request.customerId, request.ephemeralKey))
                .build(),
        )
        viewModel.consumeSetupRequest()
    }
}

LaunchedEffect(viewModel.challenge) {
    viewModel.challenge?.let { launcher.handleNextActionForPaymentIntent(it.clientSecret) }
}
```
Adaptations (RESEARCH Patterns 1-3): call the consume function BEFORE launching (rotation safety); replace `PaymentSheet.Builder` with `PaymentSheet.FlowController.Builder(resultCallback, paymentOptionResultCallback).build()` plus `configureWithSetupIntent` then `presentPaymentOptions()`; proceed to `confirm()` only when `!didCancel && paymentOption != null` (D-18); `merchantDisplayName = "ClickMe"`, `allowsDelayedPaymentMethods(false)`; Appearance with `colorsLight == colorsDark` (no `UserInterfaceStyle.AlwaysDark` on Android). Use `AppConfig.stripePublishableKey`. Place the host inside `MakeABookingScreen`. FlowController and the AddCard `PaymentSheet` both have no analog in `:app`; the spike has only PaymentSheet and PaymentLauncher.

The Add-card `PaymentSheet` in `PaymentMethodsScreen` copies lines 56-70 of this spike nearly verbatim.

---

### `MakeABookingScreen.kt` (modify)

**Analog:** same file lines 62-100.
```kotlin
val state by viewModel.uiState.collectAsStateWithLifecycle()
LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshClock() }
LaunchedEffect(state.completedBookingId) {
    val id = state.completedBookingId ?: return@LaunchedEffect
    viewModel.consumeCompletedBooking()
    onBooked(id)
}
MakeABookingContent(state = state, ..., onBook = viewModel::book, onDismissError = viewModel::dismissError, ...)
```
Add: `viewModel::refreshPaymentMethods` in the ON_RESUME effect (idempotent), `onOpenPaymentMethods: () -> Unit` parameter (screen to shell, like `onBooked`), the `PaidFlowStripeHost`, and the info-sheet state. Stateless `MakeABookingContent` keeps taking plain lambdas. Keep `BackHandler(enabled = state.isSubmitting) {}` (line 123). The "How does payment work?" sheet visibility follows `var showTopicSheet by rememberSaveable { mutableStateOf(false) }` (line 122). Errors use the existing `AuthErrorDialog` (title from `state.dialogTitle`).

---

### `subviews/MakeABookingBookButton.kt` (modify, CTA switch)

**Analog:** same file lines 41-100. Keep the structure (`PressScaleButton`, `testTag(BOOK_NOW_TAG)`, `stateDescription` semantics, progress spinner while submitting). Change:
- The `enabled = disabledReason == null` logic to the CTA enum.
- The label `Text(stringResource(R.string.make_a_booking_book_now))` to switch among "Book Now" / "Add a Payment Method".
- Remove the `PaidTopic` coming-soon `Text` block (lines 85-97) and the `make_a_booking_state_paid` / `make_a_booking_paid_note` strings.
- Update the `@Preview` (lines 103-124) and `MakeABookingScreenTest` / `MakeABookingFormCardsTest` that assert the note.

---

### `subviews/PaymentInfoSheet.kt` (ModalBottomSheet)

**Analog:** `components/PickerSheet.kt` (imports `ModalBottomSheet` at line 29, `rememberModalBottomSheetState` at line 32) and `subviews/MakeABookingTopicSheet.kt` (uses it via `PickerSheet`). Copy the sheet-state + `onDismissRequest` idiom; content is the shared lifecycle steps composable plus the "Got it" button. Apply D-12 copy.

---

### `components/PaymentLifecycleCard.kt` (shared by info sheet and Request Sent)

**Analog:** `screens/clientmode/requestsent/subviews/BookingRequestSentDetailsCard.kt` (card styling, `zone` / `timeFormat` params for formatting a slot time). Put in `components/` (discretion: shared). Request Sent insertion point: `BookingRequestSentScreen.kt` `LoadedBody` Column (lines 174-207), add `if (!details.isFree) PaymentLifecycleCard(...)` between the details card and the actions, either inside the existing `CARD_INDEX` block or as an extra animated block (`BLOCK_COUNT`, `BLOCK_DELAYS_MS` lines 214-218). Paid headline: add an `isFree` argument to `BookingRequestSentHeadline` (the `isFree` field is already on `BookingRequestSentDetails`, whose KDoc says "Reserved for Phase 7"). Remove that KDoc note and the "never mentions payment" note on the headline.

---

### `screens/settings/paymentmethods/*` (new package)

**Key:** analog `makeabooking/BookingKey.kt`.
```kotlin
@Serializable
data class BookingKey(...) : NavKey
```
Use `@Serializable data object PaymentMethodsKey : NavKey`.

**Screen:** analog `requestsent/BookingRequestSentScreen.kt` lines 73-136: thin stateful wrapper (`collectAsStateWithLifecycle`) over a stateless `...Content` with `Scaffold(containerColor = surface, topBar = AuthTopAppBar(title, onBack))` and a `when (state)` over Loading / Failed / Loaded. Failed uses `EmptyState` with `EmptyStateAction(stringResource(R.string.section_retry), onRetry)` (lines 118-128); loading uses a `CircularProgressIndicator` + `liveRegion = LiveRegionMode.Polite` (lines 100-115). Empty list state uses `components/EmptyState.kt`.

**ViewModel:** analog `MakeABookingViewModel.loadTopics()` (lines 534-552) for load/Retry, and `handleBookingFailure` for error to dialog mapping. State holder style: `MutableStateFlow` + `asStateFlow()` + `.update { it.copy(...) }`. Busy rule: single `busyId` (iOS `deletingId` / `settingDefaultId`); reload from the server after each mutation, never optimistic (D-06, D-07). Hilt: `@HiltViewModel` + `@Inject constructor` (no assisted factory needed; there is no per-instance key). Add-card consume-on-launch `pendingSetup` mirrors the spike's `consumeSetupRequest()`.

**Dialogs:** analog `screens/login/AuthErrorDialog.kt` for "Couldn't remove" / "Couldn't update" / "Couldn't add card"; the Remove confirm is a second Material `AlertDialog` (copy the AuthErrorDialog structure).

**Brand display:** top-level functions only, in `PaymentMethodBrandDisplay.kt` (`fun brandDisplayName(raw: String?): String`). Do NOT declare `PaymentMethodsBrand` (CI rule in `scripts/check-design-rules.sh` forbids `(object|class|enum class|interface) +\w*Brand\b`). No `Color(0x...)` literals.

**Row menu:** no `DropdownMenu` analog found in the codebase (see below); use Material `IconButton` + `DropdownMenu`. Spinner idiom from `MakeABookingBookButton.kt` lines 68-74: `CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)`.

---

### Navigation: `ClientBookingFlowEntries.kt` / `HomeClientScreen.kt` (modify)

**Analog:** `ClientBookingFlowEntries.kt` lines 33-51.
```kotlin
fun EntryProviderScope<NavKey>.clientBookingFlowEntries(
    navigator: Navigator,
    booking: @Composable (key: BookingKey, onBack: () -> Unit, onBooked: (bookingId: String) -> Unit) -> Unit,
    ...
) {
    entry<BookingKey>(metadata = BottomBar.hidden()) { key ->
        booking(key, navigator::goBack) { id -> navigator.replaceTop(RequestSentKey(id)) }
    }
```
Add `onPaymentMethods = { navigator.navigate(PaymentMethodsKey) }` to the `booking` lambda and register `entry<PaymentMethodsKey>(metadata = BottomBar.hidden()) { paymentMethods(navigator::goBack) }` once, so both the Explore stack and the Profile tab reach it (D-04). It stays Hilt-free: the Hilt screen arrives via a lambda param so `HomeShellsTest` can drive it with fakes.

Profile row: `HomeClientScreen.kt` lines 185-205 (`entry<ClientProfileRoot>` -> `ClientTabRoot(key, footer = { OutlinedButton(onClick = onLogout) ... })`). Add the "Payment Methods" `ProfileMenuRow` before the Log out button in `footer`, with `navigator.navigate(PaymentMethodsKey)`. The Profile tab uses `PlaceholderTabContent` (`components/PlaceholderTabContent.kt`, `footer: @Composable ColumnScope.() -> Unit`), so no layout work is needed.

---

### Tests

- `testing/FakeClickMeApi.kt`: pattern `var xHandler: suspend (Req) -> Resp = { notStubbed() }` plus `@Volatile var xCalls = 0 private set` (lines 46-80). Add handlers for the 7 new endpoints.
- `BookingTestFixtures.kt` lines 65-78: `newViewModel(api, savedState, clock, completedBookings)`. Add `paymentMethods` and `reader` parameters with defaults so existing callers compile.
- `MakeABookingViewModelBookTest.kt` is the analog for `MakeABookingViewModelPaidTest` / `...ScaTest` (MainDispatcherRule, MutableClock, fake api). Use `MutableClock` and `MainDispatcherRule` from `testing/`.
- Compose tests: `MakeABookingScreenTest.kt`, `BookingRequestSentScreenTest.kt` (Robolectric + `ui-test-junit4`).

## Shared Patterns

### API call wrapping and error type
**Source:** `network/ApiCall.kt` (`apiCall {}`), `network/ApiException.kt` lines 14-44
**Apply to:** every new repository method and every ViewModel catch block. Repositories throw only `ApiException`; ViewModels `catch (e: ApiException)` (never broader). `ApiException.Http` exposes `status`, `code: ErrorCode?`, `rawCode`, `serverMessage`, `detail: Map<String, Any?>?`, `userMessage`.

### Error text to UI
**Source:** `screens/login/FieldError.kt` (`FieldError.Res(id)` / `FieldError.Text(string)`), `screens/login/AuthErrorDialog.kt`, `BookingErrors.kt` lines 62-72 (`toSectionMessage()` for inline load failures)
**Apply to:** Payment Methods load error, all alerts, paid flow failures. Server text is wrapped in `FieldError.Text` only (rendered as plain text).

### Single flight and lock
**Source:** `MakeABookingViewModel.kt` lines 141-143, 154, 163, 178, 191, 202, 208, 231
**Apply to:** paid `book()`, Payment Methods add/delete/set-default. First line of every action: `if (_uiState.value.isSubmitting) return`.

### Money display
**Source:** `util/Money.kt` `Money.format(minorUnits, currencyCode)` and `util/PriceDisplay.kt`
**Apply to:** Summary price, Payment Methods N/A. Never pass `payment-intent` totals (decimal major units).

### Secrets hygiene
**Apply to:** the entire paid flow. Never put `client_secret`, `ephemeral_key`, `stripe_account`, `pm_`, `pi_` or `bookingAttemptId` in `SavedStateHandle` or logs (the Phase 6 `SavedStateHandle` keys at `MakeABookingViewModel.kt` lines 559-566 are the only persisted keys).

### Compose conventions
**Source:** `BookButton.kt`, `PlaceholderTabContent.kt`
**Apply to:** all new composables: `Spacing` tokens from `ui/theme/Spacing`, `MaterialTheme.colorScheme` / `MaterialTheme.extras.onSurfaceMuted`, stateless content composable + `@Preview` wrapped in `ClickMeTheme`, user-facing strings in `res/values/strings.xml`, `testTag` constants as top-level `const val`. CI gate `scripts/check-design-rules.sh` (no `*Brand` types, no raw `Color(0x...)`) plus ktlint.

## No Analog Found

| File | Role | Data Flow | Reason |
|---|---|---|---|
| `PaymentSheet.FlowController` usage in `PaidFlowStripeHost.kt` | component | event-driven | The spike has only `PaymentSheet.presentWithSetupIntent` and `PaymentLauncher`; no FlowController or `configureWithSetupIntent` code exists. Use RESEARCH Pattern 2 (device-unproven). |
| `DefaultStripePaymentMethodReader` (`retrieveSetupIntentSynchronous`) | service | request-response | Spike reads `pm_` from the card list, not the SetupIntent. Use RESEARCH Pattern 4. |
| `PaymentMethodRow` overflow `DropdownMenu` | component | CRUD | No `DropdownMenu` / overflow menu exists in `components/` or `screens/` (only `PickerField` / `PickerSheet`). Use the standard Material 3 API. |
| PaymentSheet `Appearance` from theme | config | transform | Nothing maps `MaterialTheme` tokens to Stripe colors. RESEARCH Pitfall 4 (`colorsLight == colorsDark`, `Typography.default.copy(fontResId = R.font.nunito)`). |
| `ProfileMenuRow` | component | n/a | Only a footer `OutlinedButton` exists; see UI-SPEC for the row spec. |

## Metadata

**Analog search scope:** `app/src/main/java/com/uptrendinvestments/clickmeapp/{network,model,di,components,navigation,screens/clientmode,screens/settings,screens/login,util}`, `app/src/test/.../{testing,screens/clientmode}`, `spikes/stripe/`.
**Files scanned:** ~45 listed, ~20 read in full or in part.
**Pattern extraction date:** 2026-10-22
**Not read (planner should read before writing the plan):** `07-UI-SPEC.md` (not loaded by this mapper, visual contract and copy), `components/PickerSheet.kt` body, `BookingRequestSentDetailsCard.kt` body, `AuthErrorDialog.kt` body, `StripeSpikeViewModel.kt` (only `consumeSetupRequest` / `onPaymentResult` located at lines 115 and 210).
