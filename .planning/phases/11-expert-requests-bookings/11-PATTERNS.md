# Phase 11: Expert Requests & Bookings - Pattern Map

**Mapped:** 2026-10-25
**Files analyzed:** ~45 new/modified (grouped by slice)
**Analogs found:** all groups have an in-repo analog; Calendar intent has none (use RESEARCH Code Examples)

All paths below are under `app/src/main/java/com/uptrendinvestments/clickmeapp/` (abbrev `MAIN/`) unless prefixed `app/src/test/...`. All analog paths verified git-tracked (`git ls-files`). Contract corrections C1-C13 and the "Open Question Resolutions" in 11-RESEARCH.md override CONTEXT/UI-SPEC wherever they differ (notably: detail URL is `expert/bookings/{id}/details`; expert `status` is an object `{label, code, color_accent}`; no pagination on booking-requests; `isExpired` travels in `RequestDecisionKey`; refund preview fed `Held`/`None`).

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match |
|---|---|---|---|---|
| `network/ApiRequestUrls.kt` (+14 consts) | config | request-response | same file, `CLIENT_BOOKING_*` lines 35-56 | exact |
| `network/ClickMeApi.kt` (+14 methods) | service (Retrofit iface) | request-response | same file, `getClientBookings` L162-168, `cancelClientBooking`/`acceptClientReschedule`/`declineClientReschedule` L251-276 | exact |
| `model/responses/BookingRequestsPayload.kt`, `ExpertBookingsPayload.kt`, `ExpertFinancialData.kt` | model (Moshi DTO) | transform | `model/responses/ClientBookingsPayload.kt` | role-match (status is object, C6) |
| `model/requests/ExpertBookingRequests.kt` (Accept/Decline bodies) | model | request-response | `model/requests` `CancelBookingRequest`, `DeclineRescheduleRequest` | exact |
| `screens/expertmode/.../BookingRequestsRepository`, `ExpertBookingsRepository`, `ExpertFinancialRepository` | service (Hilt singleton) | CRUD | `screens/clientmode/mybookings/ClientBookingsRepository.kt` | exact |
| `screens/expertmode/ExpertLaunchRequest.kt` | store (singleton hand-off) | event-driven | `screens/clientmode/mybookings/BookingsLaunchRequest.kt` | exact |
| `screens/expertmode/ExpertErrors.kt` (add `toExpertRescheduleFailure`, `toExpertCancelFailure`, `toRequestFailure`) | utility | transform | `screens/clientmode/mybookings/RescheduleErrors.kt` + `screens/expertmode/ExpertErrors.kt` | exact |
| `screens/expertmode/upcomingbookings/ExpertBookingBuckets.kt` | utility (pure) | transform | `screens/clientmode/mybookings/BookingBuckets.kt` | exact |
| `.../upcomingbookings/ExpertBookingsViewModel.kt`, `ExpertBookingsScreen.kt`, `subviews/*Card.kt` | VM + screen | CRUD + realtime | `screens/clientmode/mybookings/MyBookingsViewModel.kt`, `MyBookingsScreen.kt`, `subviews/UpcomingBookingCard.kt`, `PastBookingCard.kt`, `MyBookingsStates.kt` | exact |
| `.../dashboard/ExpertDashboardViewModel.kt`, `ExpertDashboardScreen.kt`, `subviews/*` | VM + screen | CRUD + realtime | `MyBookingsViewModel.kt` (state machine); `screens/clientmode/explore/subviews/ExploreTodaysSessionsSection.kt` + `mybookings/TodaysSessions.kt` (Today's Sessions) | role-match |
| `.../incomingrequests/IncomingRequestsViewModel.kt`, `Screen`, `Key`, `subviews/IncomingRequestCard.kt` | VM + screen | CRUD | `MyBookingsViewModel.kt` + `UpcomingBookingCard.kt` | role-match |
| `.../requestdecision/RequestDecisionViewModel.kt`, `Screen`, `Key`, sheets | VM + screen | request-response | `screens/clientmode/cancellation/ClientCancellationViewModel.kt` (assisted key VM, single-flight submit, error dialog) | role-match |
| `.../confirmed/BookingConfirmedScreen.kt`, `Key`, `CalendarIntent.kt` | screen | event-driven | `screens/clientmode/requestsent/*` + `subviews/BookingRequestSentCheckIcon.kt` | role-match; calendar intent: none |
| `.../bookingsummary/ExpertBookingSummaryViewModel.kt`, `Screen`, `Key`, `subviews` | VM + screen | CRUD | `screens/clientmode/bookingsummary/*` and `screens/clientmode/upcomingbooking/UpcomingBookingViewModel.kt` (guard + reschedule) | role-match |
| `.../cancellation/ExpertCancellationViewModel.kt`, `Screen`, `ExpertCancelReason.kt`, `ExpertRefundPreview.kt`, `ExpertCancelKey.kt` | VM + screen | request-response | `screens/clientmode/cancellation/*` (all files) | exact |
| `components/RescheduleResponseCard.kt` (expert variant) | component | event-driven | same file | exact (modify) |
| `screens/clientmode/mybookings/RescheduleResponse.kt` (controller/responder) | service | request-response | same file; add `ExpertRescheduleResponder` | exact (extend) |
| `managers/CallDialog.kt` + `screens/call/calling/CallDialogs.kt` (+ strings) | config | event-driven | `RestrictedAction.EditAvailability` addition (Phase 10) | exact |
| `screens/expertmode/availability/AvailabilityKey.kt` (+`Dashboard`, `Bookings`) | config | - | same file L8 | exact |
| `screens/expertmode/home/HomeExpertScreen.kt` (slots + entries) | route/shell | request-response | same file L56-91, L101-180; `screens/clientmode/home/ClientBookingManagementEntries.kt` | exact |
| tests (VM, repo, buckets, refund, wire, shell) | test | - | `app/src/test/.../mybookings/MyBookingsViewModelTest.kt`, `ClientBookingsRepositoryTest.kt`, `cancellation/ClientCancellationViewModelTest.kt`, `RefundPreviewTest.kt`, `network/ApiRequestUrlsTest.kt`, `screens/HomeShellsTest.kt`, `components/RescheduleResponseCardTest.kt`, `testing/FakeClickMeApi.kt` | exact |

## Pattern Assignments

### Data layer: `ApiRequestUrls` / `ClickMeApi` / repositories

**Analog:** `network/ClickMeApi.kt` L162-168, L251-276; `ClientBookingsRepository.kt` L23-61.

URL style: no leading slash, `{id}` placeholders (`CLIENT_BOOKING_CANCEL = "client/bookings/{id}/cancel"`). Add per RESEARCH list incl. `EXPERT_BOOKING_DETAILS = "expert/bookings/{id}/details"` (C1). Note existing `EXPERT_MY_AVAILABILITY = "expert/availability"` L70: do not collide.

Retrofit method pattern (always explicit `@Body` for strict schemas so Moshi emits `{}`):
```kotlin
@POST(ApiRequestUrls.CLIENT_BOOKING_ACCEPT_RESCHEDULE)
suspend fun acceptClientReschedule(
    @Path("id") bookingId: String,
    @Body body: AcceptRescheduleRequest,
): SuccessDataResponse<RescheduleResponseData>
```
Expert accept-reschedule can reuse `AcceptRescheduleRequest(null)` and `DeclineRescheduleRequest(reason)`, `CancelBookingRequest(reason)`/`CancelBookingData` (refund in MAJOR units). Accept request body: `AcceptRequestBody(message: String?)` serializes `{}` when null (required). Decline: both keys always. Connect `onboard`/`refresh-link`: no body.

Repository pattern (copy shape, `apiCall { }.data`, "reason text never logged"):
```kotlin
@Singleton
class ClientBookingsRepository @Inject constructor(private val api: ClickMeApi) {
    suspend fun list(type: String, page: Int = 1, limit: Int = 20): ClientBookingsPayload =
        apiCall { api.getClientBookings(type, page, limit) }.data
    suspend fun cancel(bookingId: String, reason: String): CancelBookingData =
        apiCall { api.cancelClientBooking(bookingId, CancelBookingRequest(reason)) }.data
    companion object { const val TYPE_UPCOMING = "upcoming"; const val TYPE_PAST = "past" }
}
```
Expert list `limit` is 1..100, `type` required.

**DTO pattern** (`ClientBookingsPayload.kt` L5-60): `@JsonClass(generateAdapter = true)`, explicit `@Json(name=...)`, `Instant` fields, defaults for nullable/lists. Deviations: expert `status` is `ExpertStatusMeta(label, code, @Json(name="color_accent") colorAccent)` (C6); booking-requests list has NO `pagination` (C2); money in `earnings_breakdown`/`consultation_fee` is MAJOR units -> `Money.formatMajor`, weekly/payout is minor -> `Money.format` (`util/Money.kt` L34, L45).

---

### `ExpertLaunchRequest.kt` (store, event-driven)

**Analog:** `screens/clientmode/mybookings/BookingsLaunchRequest.kt` L24-54 (copy whole shape):
```kotlin
@Singleton
class BookingsLaunchRequest @Inject constructor() {
    private val _pending = MutableStateFlow<BookingsLaunch?>(null)
    val pending: StateFlow<BookingsLaunch?> = _pending.asStateFlow()
    fun post(launch: BookingsLaunch) { _pending.value = launch }
    fun consume(): BookingsLaunch? = _pending.getAndUpdate { null }
}
```
Use sealed/typed launches for: Incoming Requests (reload + "Request declined" snackbar), Dashboard (reload after Back to Home), Expert Bookings (select tab Upcoming/Past + cancel notice). Reuse `BookingsTab` enum or a sibling. Test analog: `BookingsLaunchRequestTest.kt`.

---

### Expert Bookings / Dashboard / Incoming Requests ViewModels (CRUD + realtime)

**Analog:** `screens/clientmode/mybookings/MyBookingsViewModel.kt` (whole file, 220 lines). Copy:
- `LoadMode { Loud, Quiet, Pull }` (L66-75), `loadJob?.cancel()` + `loadIsCurrent` (L178-206).
- Realtime trigger (L96-100): `realtime.events.filterIsInstance<RealtimeEvent.BookingUpdate>().collect { startLoad(Quiet or Loud) }`.
- `onScreenShown()` / `onScreenHidden()` (L139-151) wired from the screen via `LifecycleEventEffect(ON_RESUME/ON_PAUSE)` (`MyBookingsScreen.kt` L84-85).
- Launch-request consume in `init` then collect `pending.filterNotNull()` (L103-111).
- `joinNow: StateFlow<Instant> = joinClock.now` (L88) and `join(item)` building `CallStartRequest` (L115-128): for expert use `peerId = item.client.id`, `peerName = item.client.name`.
- Failure handling: `catch (e: ApiException)` -> `e.toSectionMessage()` (Loud) / `refreshFailed=true` (Pull) / swallow (Quiet).
- Parallel buckets: `coroutineScope { async {...}; async {...} }` (L208-213). For Dashboard: bookings + requests are HARD (fail -> "Couldn't load dashboard"); weekly/connect/payouts are SOFT (individual try/catch, never fail state).
- Screen scaffolding: `PullToRefreshBox`, `SnackbarHost` for refresh-failed and arrival notices (`MyBookingsScreen.kt` L134-177, L216); notice shown on composition scope so consuming does not cancel it (L149-150).

Dashboard extras: re-fetch Connect status on `ON_RESUME`; connect state machine per RESEARCH Pitfall 6; `CallGuard.attempt(RestrictedAction.SetUpPayouts)` before minting; open URL via `rememberCustomTabLauncher { onFailure }` (`components/web/CustomTabLauncher.kt` L73-79; https/http only). Never log or persist the URL.

Today's Sessions: `mybookings/TodaysSessions.kt` L13-27 filter is typed to `ClientBookingItem`; duplicate the 4-line filter for the expert item (use `status.code in JOINABLE_BOOKING_STATUSES`, reuse `JOINABLE_BOOKING_STATUSES`, `formatSessionTimeRange` L34-42). Card analog: `screens/clientmode/explore/subviews/ExploreTodaysSessionsSection.kt`. Join: `JoinCallButton(enabled = isWithinJoinWindow(status.code, start, end, now), onClick=...)` (`components/call/JoinCallButton.kt`; `managers/JoinWindow.kt` L45 `isWithinJoinWindow(status, start, end, now)`, L72 `isStartingNow(start, now)`). Ignore `actions.can_join` (Pitfall 4).

---

### `ExpertBookingBuckets.kt` (utility, transform)

**Analog:** `mybookings/BookingBuckets.kt` L40-57 (`buildBookingLists`) and `BookingStatusDisplay.kt` (`pastStatusDisplay(rawString)`, takes raw codes so pass `status.code`). Adapted body is already in RESEARCH Pattern 1 (filter Upcoming to `confirmed/pending_reschedule/in_progress` with `endTime > now`, merge rejoinable `completed`, `distinctBy bookingId`). Test: copy structure of existing `BookingBuckets` test under `app/src/test/.../mybookings/`.

---

### Expert reschedule: `RescheduleResponseCard` expert variant + responder + mapper

**Analogs:** `components/RescheduleResponseCard.kt` (tags L77-80: `RESCHEDULE_CARD_TAG/ACCEPT/DECLINE/DECLINE_REASON` keep unchanged; imports `RescheduleProposalUi`, `RescheduleSubmitting`, `RescheduleDialog`, `AuthErrorDialog`, `DestructiveOutlinedButton`, `PressScaleButton`); `mybookings/RescheduleResponse.kt` (controller + `RescheduleResponder` client-only); `mybookings/RescheduleErrors.kt` L1-113.

Approach (RESEARCH Pattern 5): add a viewer enum (`Client|Expert`) / `actionable` + string set to the card; keep `RescheduleResponseCardTest` green. Expert actionable when `initiatedBy == "client"`; `showsPaymentDisclosure=false`, `chargesSoon=false`. New `ExpertRescheduleResponder` (no saved-card retry). Mapper pattern to copy for `toExpertRescheduleFailure()` (do NOT reuse `toRescheduleFailure`, C9):
```kotlin
fun ApiException.toRescheduleFailure(): RescheduleFailure =
    when (this) {
        is ApiException.Http -> when (code) {
            ErrorCode.AccountSuspended -> RescheduleFailure.Silent
            ErrorCode.InvalidStatus ->
                RescheduleFailure.Show(FieldError.Res(R.string.reschedule_error_not_pending), reload = true)
            ErrorCode.NotFound -> notFound()   // Show(..., pop = true)
            else -> when (status) { 403 -> forbidden(); 404 -> notFound(); else -> RescheduleFailure.Show(serverTextOrUnknown()) }
        }
        is ApiException.Network -> RescheduleFailure.Show(FieldError.Res(R.string.login_error_network))
        is ApiException.Decoding -> RescheduleFailure.Show(FieldError.Res(R.string.login_error_unknown))
    }
```
Add branches: `ErrorCode.SlotUnavailable` -> new expert string + `reload=true`; `ClientUnavailable`, `ValidationError`, `RescheduleOutsideAuthWindow` -> server message + reload. Reuse `RescheduleFailure`/`CancelFailure` sealed types. Server text only via `FieldError.Text` (plain text). Expert cancel mapper: copy `toCancelFailure()` (L90-113) and add `ErrorCode.AlreadyCancelled` -> success (C8); `InvalidStatus`/404 -> `returnToBookings = true`.

Guard usage: `callGuard.attemptModifyBooking(bookingId, RestrictedAction.X)` (see `UpcomingBookingViewModel.kt` L141).

---

### Expert cancellation (screen, VM, reasons, refund preview)

**Analog:** `screens/clientmode/cancellation/` (exact: `ClientCancellationViewModel.kt`, `ClientCancellationScreen.kt`, `ClientCancellationUiState.kt`, `CancelBookingKey.kt`, `CancellationReason.kt`, `RefundPreview.kt`, `subviews/CancellationReasonStep.kt`, `CancellationConfirmStep.kt`).

Key: pattern (`CancelBookingKey.kt` L11-14): `@Serializable data class CancelBookingKey(val bookingId: String) : NavKey`. Expert: `ExpertCancelKey(bookingId, origin)`.

Assisted VM (copy L47-65): `@HiltViewModel(assistedFactory = X.Factory::class)`, `@AssistedInject constructor(@Assisted key, savedStateHandle, repository, clock, launchRequest, callGuard)`; state mirrored into `SavedStateHandle` (KEY_STEP/REASON/COMMENT); `confirm()` single flight:
```kotlin
if (current.isSubmitting || current.load !is CancellationLoad.Loaded) return
if (!callGuard.attemptModifyBooking(bookingId, RestrictedAction.CancelBooking)) return
_uiState.update { it.copy(isSubmitting = true) }
viewModelScope.launch {
    try {
        val data = repository.cancel(bookingId, cancelReason(current.reason, current.comment))
        val notice = if (data.refundAmount > 0.0 && data.currency != null)
            CancelledWithRefund(Money.formatMajor(data.refundAmount, data.currency)) else Cancelled
        launchRequest.post(...Past, notice); _uiState.update { it.copy(isFinished = true) }
    } catch (e: ApiException) { when (val f = e.toCancelFailure()) { Silent -> ...; is Show -> ... } }
}
```
Reasons: new `ExpertCancelReason` enum (4 labels: Personal Emergency, Scheduling Conflict, Technical Issues, Other; no default selection) instead of editing `CancellationReason`; reuse `cancelReason()`-style merge with `capNotes` and `CANCEL_REASON_MAX_CHARS`(500)/`CANCEL_COMMENT_MAX_CHARS`(450) (`CancellationReason.kt` L22-45) - make `cancelReason` generic over a label string.

Refund preview (`RefundPreview.kt`): keep sealed interface; write pure `expertRefundPreview(paymentState: PaymentState {None, Held, Captured}, feeAmountMajor, currency, start, now)`; feed `Held` when `consultation_fee.amount > 0`, `None` when free (Open Question Resolution 1: paid -> "payment hold will be released", free -> no line; Captured rows dormant but unit tested). 24h rule: `Duration.between(now, start) > REFUND_FULL_WINDOW` (L29, L52). Test analog: `RefundPreviewTest.kt`.

---

### Request Decision / Incoming Requests (key, VM, sheets)

**Analog:** `ClientCancellationViewModel` (assisted key, single flight, `isSubmitting`, error dialog + `dismissError()` that can finish the screen, `consumeFinished()`). `RequestDecisionKey(requestId, isExpired, expiresAtMillis?, earningsMajor?, currency?)` per C3/C4. Accept success -> post to `ExpertLaunchRequest` and `navigator.replaceTop(BookingConfirmedKey(...))` (`Navigator.kt` L88; stack size > 1). Decline success -> post launch + `goBack()`. `ErrorCode.AlreadyProcessed` -> dialog; OK -> post reload launch + `goBack()`. Backend bodies per D-01/D-02/D-08 (decline always sends both keys; fallback `reason_text` = reason label; caps 1000). Sheets: `ModalBottomSheet` (existing use: `PaymentInfoSheet.kt`); while submitting ignore dismiss/back. Error dialog: `AuthErrorDialog(message, onDismiss, title)`.

`IncomingRequestsViewModel`: list from `repository.list()`; expiry = `status == "expired" || expiresAt <= now`; sort pending by `expiresAt` ascending, expired last; count = `requests.size` (C2).

---

### Booking Confirmed + Calendar intent

**Analog:** `screens/clientmode/requestsent/` (Screen/ViewModel/UiState/Key) and `subviews/BookingRequestSentCheckIcon.kt` L15-35 (reuse as-is, 128dp surfaceContainer circle, 25% primary border, `Outlined.CheckCircle`; per C13 do not re-skin; generalize/move only if shared). `BookingConfirmedKey` carries id, client name/avatar, topic, start/end, meetingType, client message; no network.

No in-repo analog for the calendar intent: use RESEARCH "Code Examples > Calendar insert (EXP-07)" (lines 437-454 of 11-RESEARCH.md): `Intent(Intent.ACTION_INSERT).setData(CalendarContract.Events.CONTENT_URI)` + `EXTRA_EVENT_BEGIN_TIME/END_TIME`; wrap `startActivity` in `try/catch ActivityNotFoundException` (no `resolveActivity`, no `<queries>`); show snackbar "No calendar app found". Keep a pure `buildCalendarIntentSpec(...)` for unit tests.

---

### Shell wiring: `HomeExpertScreen.kt`, keys, `AvailabilityKey`

**Analog:** `HomeExpertScreen.kt` L56-91 (Hilt entry with slot lambdas `topicsRoot`, `detailEntries`) and L101-180 (stateless `HomeExpertShell`). Add default-placeholder slots `dashboardRoot` / `bookingsRoot` mirroring `topicsRoot: @Composable (Navigator) -> Unit = { ExpertTabRoot(TopicsRoot) }` (L110) so `HomeShellsTest` still compiles; replace `entry<DashboardRoot>`/`entry<ExpertBookingsRoot>` (L123-129) to call the slots (Dashboard keeps `TopBarAvatarButton` action). Detail entries, all `BottomBar.hidden()`:
```kotlin
entry<TopicEditorKey>(metadata = BottomBar.hidden()) { key ->
    TopicEditorScreen(
        viewModel = hiltViewModel<TopicEditorViewModel, TopicEditorViewModel.Factory>(
            creationCallback = { factory -> factory.create(key) }),
        onBack = navigator::goBack)
}
```
Hilt-free registration helper pattern: `screens/clientmode/home/ClientBookingManagementEntries.kt` (`EntryProviderScope<NavKey>.clientBookingManagementEntries(navigator, upcoming=..., cancel=..., summary=...)` taking composable lambdas so Robolectric tests can pass fakes) and `Navigator` extension helpers (`showMyBookings() = resetAndSelect(ClientBookingsRoot, setOf(ExploreRoot, ClientBookingsRoot))`). Create expert equivalents: `showExpertBookings()`, `showDashboard() = resetAndSelect(DashboardRoot, setOf(DashboardRoot))`.

Keys that may sit in two stacks carry `origin` (`AvailabilityKey.kt` L7-18): add `Dashboard`, `Bookings` to `AvailabilityOrigin` and give `ExpertBookingSummaryKey(bookingId, origin)`/`ExpertCancelKey(bookingId, origin)` the same trick.

---

### `RestrictedAction` additions

**Analog:** `managers/CallDialog.kt` L10-16 enum plus exhaustive `when` in `screens/call/calling/CallDialogs.kt` ~L45-50 (`RestrictedAction.CancelBooking -> R.string.call_action_cancel_booking`). Add `SetUpPayouts`, `RespondToReschedule` + `call_action_*` strings; missing a `when` branch breaks the build.

## Shared Patterns

### Error dialog and server text
**Source:** `screens/expertmode/ExpertErrors.kt` L17-30 `toExpertDialogError()` (suspended -> null; first field error > server message > generic) and `screens/login` `AuthErrorDialog`/`FieldError`. **Apply to:** Dashboard onboarding failure, Accept/Decline errors, Summary reschedule errors. Server text rendered only as plain `Text`.

### Quiet/loud load + section failure
**Source:** `MyBookingsViewModel.kt` L178-206 and `toSectionMessage()` (`screens/clientmode/makeabooking`). **Apply to:** all list/dashboard/summary VMs. Failed quiet reload shows nothing; failed pull -> snackbar "Couldn't refresh...".

### Join
**Source:** `components/call/JoinCallButton.kt`, `managers/JoinWindow.kt` L45/L72/L83, `CallStarter.startCall(CallStartRequest(bookingId, peerId, peerName, peerAvatarUrl, topic, scheduledStart, scheduledEnd, meetingType = MeetingType.fromWire(...)))`. **Apply to:** Dashboard Today card, Upcoming card, expert Summary (D-14).

### Guard
**Source:** `ClientCancellationViewModel.kt` L154, `UpcomingBookingViewModel.kt` L141 (`attemptModifyBooking`); `CallGuard.attempt(action)` L15 for non-booking actions. **Apply to:** cancel, accept/decline reschedule, payouts setup.

### One-shot cross-screen hand-off
**Source:** `BookingsLaunchRequest.kt`. **Apply to:** decline/accept -> Incoming Requests, Back to Home -> Dashboard, cancel -> Bookings(Past).

### Money
**Source:** `util/Money.kt` `format(Long minor, currency)` L45 and `formatMajor(Double, currency)` L34. Weekly/payout = minor; breakdown/fee/list earnings/cancel refund = major.

### Test harness
**Source:** `app/src/test/.../testing/FakeClickMeApi.kt` (add handler properties + call counters for each new endpoint), `MyBookingsViewModelTest.kt` (VM with `MainDispatcherRule`, `MutableClock`, `FakeRealtimeConnection`, `FakeCallGuard`, `FakeCallStarter`), `ClientBookingsRepositoryTest.kt` (MockWebServer wire JSON asserts; add `/expert/bookings/abc/details` path test per Pitfall 1), `network/ApiRequestUrlsTest.kt` (extend), `HomeShellsTest.kt` (shell with new slots).

## No Analog Found

| File | Role | Data Flow | Reason |
|---|---|---|---|
| `confirmed/CalendarIntent.kt` | utility | event-driven | No `ACTION_INSERT`/CalendarContract use in repo; use RESEARCH Code Examples |
| Dashboard Connect/payout card logic (`ExpertDashboardViewModel` financial part) | VM logic | request-response | No Connect code exists; follow RESEARCH Pitfall 6 + iOS `ExpertDashboardViewModel`; only Custom Tab launcher is reusable |
| `EarningsBreakdown` section rendering | component | transform | New; follow RESEARCH "Earnings breakdown rendering rule" (lines ~512-522) and UI-SPEC surface 3 block 5 |

## Metadata

**Analog search scope:** `MAIN/screens/clientmode/{mybookings,cancellation,requestsent,home,bookingsummary,upcomingbooking,explore}`, `MAIN/screens/expertmode/{home,availability,topics}`, `MAIN/components`, `MAIN/managers`, `MAIN/network`, `app/src/test`
**Files scanned:** ~30 read/listed
**Pattern extraction date:** 2026-10-25
