package com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.managers.MonotonicClock
import com.uptrendinvestments.clickmeapp.model.enums.ErrorCode
import com.uptrendinvestments.clickmeapp.model.enums.MeetingType
import com.uptrendinvestments.clickmeapp.model.requests.BookingRequestBody
import com.uptrendinvestments.clickmeapp.model.requests.ConfirmBookingRequest
import com.uptrendinvestments.clickmeapp.model.requests.PaymentIntentRequest
import com.uptrendinvestments.clickmeapp.model.responses.AvailabilityDayDto
import com.uptrendinvestments.clickmeapp.model.responses.ExpertAvailabilityData
import com.uptrendinvestments.clickmeapp.model.responses.PaymentMethodItem
import com.uptrendinvestments.clickmeapp.model.responses.PaymentMethodSetupData
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.screens.clientmode.expertprofile.ExpertProfileRepository
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.screens.settings.paymentmethods.PaymentMethodsRepository
import com.uptrendinvestments.clickmeapp.stripe.ChallengeOutcome
import com.uptrendinvestments.clickmeapp.stripe.SETUP_INTENT_REUSE_WINDOW_MS
import com.uptrendinvestments.clickmeapp.stripe.SheetOutcome
import com.uptrendinvestments.clickmeapp.stripe.StripePaymentMethodReader
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeParseException
import java.util.UUID

/**
 * Make a Booking state machine. Port of iOS `MakeABookingViewModel`; `book()` is the free booking action (BOOK-03)
 * and the paid booking action (PAY-03, PAY-04): a port of iOS `beginPaidBooking`, `performLongLeadBooking`,
 * `completePaidBooking` and `handlePaidFlowError`. It is Path A: the app never confirms the booking PaymentIntent
 * itself, it only sends the `pm_` of a card to `/bookings/confirm`. The ViewModel holds no Stripe types: the Compose
 * host consumes [MakeABookingUiState.stripeRequest] and reports a [SheetOutcome] back.
 *
 * - D-10: the first topic is preselected, paid or free (iOS parity), else none.
 * - D-08: the client's saved cards load with the form. A paid topic is gated on them: nothing known yet disables
 *   the CTA, no cards (or a failed load) turns it into "Add a Payment Method", at least one card is "Book Now".
 *   D-09: [refreshPaymentMethods] reloads the cards silently on return from Payment Methods and never books.
 * - D-03: opens on today with no auto-jump to the first open day. D-06: no month before the current one and
 *   no past day. D-07: the displayed month plus both neighbours load in parallel and are regrouped by local
 *   date; changing the month or the day clears the slot, changing the meeting type or the notes does not.
 *   iOS lets a slow response for an old month overwrite the new one; here every load cancels the previous.
 * - D-08: both meeting types are always offered; the expert's `available_meeting_types` is never read.
 * - While a booking is in flight (and after it succeeds) every selection setter is ignored, and a late availability
 *   response cannot drop the slot either (WR-03), so the request, the Summary and the 409/422 side effects all refer
 *   to the same slot, topic and meeting type.
 * - Selection state (topic, day, month, slot, meeting type, notes) is mirrored to [SavedStateHandle] so it
 *   survives process death. A restored topic or slot is only applied if it is still present in the freshly
 *   loaded data (and, for a slot, still selectable). A completed booking id is persisted too, until the screen
 *   consumes it. A success that lands after the activity saved its state (the app is in the background) is not in
 *   that saved state, so it is also written to the durable [CompletedBookingStore] under an attempt id that is put
 *   in the saved state before the request is sent (WR-01). A ViewModel restored with that attempt id looks the
 *   record up before it shows the form; without a record it shows the form behind a check-My-Bookings notice.
 */
@HiltViewModel(assistedFactory = MakeABookingViewModel.Factory::class)
class MakeABookingViewModel
    @AssistedInject
    constructor(
        @Assisted key: BookingKey,
        private val savedStateHandle: SavedStateHandle,
        private val profiles: ExpertProfileRepository,
        private val repository: BookingRepository,
        private val paymentMethodsRepository: PaymentMethodsRepository,
        private val completedBookings: CompletedBookingStore,
        private val clock: Clock,
        private val paymentMethodReader: StripePaymentMethodReader,
        private val monotonicClock: MonotonicClock,
    ) : ViewModel() {
        @AssistedFactory
        interface Factory {
            fun create(key: BookingKey): MakeABookingViewModel
        }

        private val expertId: String = key.expertId

        /** A restored topic id, applied once the topics arrive. */
        private var pendingTopicId: String? = savedStateHandle.get<String>(KEY_TOPIC_ID)

        private val _uiState = MutableStateFlow(initialState(key))
        val uiState: StateFlow<MakeABookingUiState> = _uiState.asStateFlow()

        /** A restored slot start (epoch millis), applied once the first availability load shows it is selectable. */
        private var pendingSlotMillis: Long? = savedStateHandle.get<Long>(KEY_SLOT_START)

        private var topicsJob: Job? = null
        private var availabilityJob: Job? = null
        private var paymentMethodsJob: Job? = null

        /** The cards of the last successful load, kept for the long-lead card choice; memory only. */
        private var savedPaymentMethods: List<PaymentMethodItem> = emptyList()

        /**
         * The idempotency key of `/bookings/payment-intent` (D-11). Memory only, never in [SavedStateHandle] (D-17).
         * Distinct from the WR-01 recovery id under [KEY_ATTEMPT_ID]. Regenerated by [newBookingAttempt] exactly
         * where iOS `didSet` fires (topic or selected slot changes) and when the server says the id is spent; kept
         * for a retry after a network failure or a cancelled sheet.
         */
        private var bookingAttemptId: String = UUID.randomUUID().toString()

        /** What the user tapped, captured at tap time so every request of one paid flow agrees (T-07-39). */
        private var paidContext: PaidContext? = null
        private var paymentIntentId: String? = null
        private var setupIntentClientSecret: String? = null

        /** A SetupIntent kept after a cancelled sheet, reused on the same attempt id (research correction 4). */
        private var reusableSetup: ReusableSetup? = null

        /** The `pm_` being confirmed; the 3DS retry sends the same one (T-07-43). */
        private var pendingPaymentMethodId: String? = null

        /**
         * True once a 3DS challenge has been started for the current confirm attempt. A second 402 after a completed
         * challenge is shown, not challenged again (RESEARCH Pitfall 10, T-07-41). Cleared by [resetPaidFlow] and
         * [book]. Memory only.
         */
        private var scaAttempted: Boolean = false

        /**
         * The attempt id whose `/bookings/confirm` ended without an answer (D-16). While it is still the current
         * [bookingAttemptId], a SLOT_UNAVAILABLE is most likely the user's own pending booking from that attempt, so
         * the dialog points to My Bookings (RESEARCH Pitfall 11a). Deviates from the iOS copy by user decision (D-19).
         */
        private var unknownOutcomeAttemptId: String? = null

        /** The raw days of the last applied availability load, kept so a zone change can regroup without a request. */
        private var loadedDays: List<AvailabilityDayDto> = emptyList()
        private var loadedExpertTimezone: String? = null

        init {
            val attemptId = savedStateHandle.get<String>(KEY_ATTEMPT_ID)
            val challengeInterrupted = savedStateHandle.remove<Boolean>(KEY_CHALLENGE_PENDING) == true
            when {
                // A restored completed booking navigates straight to Request Sent, so there is nothing to load.
                _uiState.value.completedBookingId != null -> Unit
                // A booking was in flight when the state was saved: it may have succeeded after the save (WR-01).
                attemptId != null -> recoverAttempt(attemptId)
                else -> {
                    loadForm()
                    if (challengeInterrupted) explainInterruptedChallenge()
                }
            }
        }

        /**
         * WR-04: the process died while a 3DS challenge was open (the bank app or browser was in front). Its context
         * (card, payment intent, account) lived in memory only, so the result Stripe still delivers cannot be acted
         * on and is ignored by [onChallengeOutcome]. If the client finished verifying, the payment intent may be
         * authorized with no booking, so instead of a silent idle form they are told their card may show a temporary
         * hold and to check My Bookings before booking again. No secret is ever saved for this (D-17): the marker is
         * a boolean.
         */
        private fun explainInterruptedChallenge() {
            _uiState.update {
                it.copy(
                    dialogError = FieldError.Res(R.string.make_a_booking_error_challenge_interrupted),
                    dialogTitle = R.string.make_a_booking_error_title,
                    dialogPaymentReference = null,
                )
            }
        }

        private fun loadForm() {
            loadTopics()
            loadAvailability(_uiState.value.displayedMonth, keepVisible = false)
            loadPaymentMethods()
        }

        /**
         * D-08: no cards, or a failed load, is [PaymentMethodsAvailability.None] (iOS parity; Payment Methods has its
         * own error state and Retry). The value is never reset to Unknown, so a silent refresh cannot flicker the CTA.
         */
        private fun loadPaymentMethods() {
            paymentMethodsJob?.cancel()
            paymentMethodsJob =
                viewModelScope.launch {
                    try {
                        val cards = paymentMethodsRepository.list()
                        savedPaymentMethods = cards
                        val availability =
                            if (cards.isEmpty()) {
                                PaymentMethodsAvailability.None
                            } else {
                                PaymentMethodsAvailability.Available
                            }
                        _uiState.update { it.copy(paymentMethods = availability) }
                    } catch (e: ApiException) {
                        savedPaymentMethods = emptyList()
                        _uiState.update { it.copy(paymentMethods = PaymentMethodsAvailability.None) }
                    }
                }
        }

        /**
         * D-09: the screen calls this on resume (so on return from Payment Methods or a Stripe activity). Ignored
         * while a booking is in flight or a load is already running, because ON_RESUME also fires on first
         * composition (RESEARCH Pitfall 9). It touches no selection and never books; the client taps again.
         * The screen's resume hook is wired by plan 07-12.
         */
        fun refreshPaymentMethods() {
            if (_uiState.value.isSubmitting || paymentMethodsJob?.isActive == true) return
            loadPaymentMethods()
        }

        /**
         * WR-01: the form stays locked while the durable store is read. A recorded success goes on to Request Sent
         * exactly like a completed id from the saved state.
         *
         * Without a record the outcome is unknown, not "never sent": the process may have died with the request in
         * flight (the server may still have created the booking), the record may have failed to write, or the store
         * may be unreadable. The free path has no idempotency key, so the form is not reopened as if nothing had been
         * sent: it loads behind a notice that asks the user to check My Bookings before booking again, and is usable
         * once the notice is dismissed. The attempt id is forgotten so the notice is shown only once.
         */
        private fun recoverAttempt(attemptId: String) {
            viewModelScope.launch {
                when (val lookup = completedBookings.get(attemptId)) {
                    is CompletedBookingLookup.Found -> {
                        savedStateHandle[KEY_COMPLETED_BOOKING_ID] = lookup.bookingId
                        _uiState.update { it.copy(completedBookingId = lookup.bookingId) }
                    }
                    CompletedBookingLookup.Absent, CompletedBookingLookup.Unreadable -> {
                        savedStateHandle.remove<String>(KEY_ATTEMPT_ID)
                        _uiState.update {
                            it.copy(
                                isSubmitting = false,
                                dialogError = FieldError.Res(R.string.make_a_booking_error_unconfirmed),
                                dialogTitle = R.string.make_a_booking_error_title,
                                dialogPaymentReference = null,
                            )
                        }
                        loadForm()
                    }
                }
            }
        }

        fun retryTopics() = loadTopics()

        fun retryAvailability() = loadAvailability(_uiState.value.displayedMonth, keepVisible = false)

        /** Changing the topic clears the slot (D-02); re-selecting the current topic is a no-op. */
        fun selectTopic(topicId: String) {
            if (_uiState.value.isSubmitting || _uiState.value.selectedTopicId == topicId) return
            pendingTopicId = null
            newBookingAttempt()
            _uiState.update { it.copy(selectedTopicId = topicId, now = clock.instant()) }
            persistTopic(topicId)
            clearSlot()
        }

        /**
         * D-06: ignored on the current month. Clears the slot, keeps the day (D-07). A slot that time has moved past
         * is cleared silently, since the month change clears it anyway (WR-02).
         */
        fun goToPreviousMonth() {
            if (_uiState.value.isSubmitting) return
            syncClock()
            if (!_uiState.value.canGoToPreviousMonth) return
            moveToMonth(_uiState.value.displayedMonth.minusMonths(1))
        }

        fun goToNextMonth() {
            if (_uiState.value.isSubmitting) return
            syncClock()
            moveToMonth(_uiState.value.displayedMonth.plusMonths(1))
        }

        private fun moveToMonth(month: YearMonth) {
            _uiState.update { it.copy(displayedMonth = month, now = clock.instant()) }
            savedStateHandle[KEY_DISPLAYED_MONTH] = month.toString()
            clearSlot()
            loadAvailability(month, keepVisible = false)
        }

        /**
         * D-06: past days are ignored. The month does not change; the slot is cleared (silently, even one that time
         * has moved past, WR-02).
         */
        fun selectDate(date: LocalDate) {
            if (_uiState.value.isSubmitting) return
            syncClock()
            if (isPastDay(date, _uiState.value.today)) return
            _uiState.update { it.copy(selectedDate = date, now = clock.instant()) }
            savedStateHandle[KEY_SELECTED_DATE] = date.toString()
            clearSlot()
        }

        /**
         * D-04: only an available slot that starts after now can be chosen. A previously selected slot that time
         * has moved past is replaced silently: the tap is already the new choice (WR-02).
         */
        fun selectSlot(start: Instant) {
            if (_uiState.value.isSubmitting) return
            syncClock()
            val now = clock.instant()
            val slot = _uiState.value.slotsForSelectedDate.firstOrNull { it.start == start }
            if (slot == null || !slot.isSelectable(now)) return
            pendingSlotMillis = null
            if (_uiState.value.selectedSlotStart != start) newBookingAttempt()
            _uiState.update { it.copy(selectedSlotStart = start, now = now) }
            savedStateHandle[KEY_SLOT_START] = start.toEpochMilli()
        }

        fun selectMeetingType(type: MeetingType) {
            if (_uiState.value.isSubmitting) return
            _uiState.update { it.copy(meetingType = type) }
            savedStateHandle[KEY_MEETING_TYPE] = type.wire
        }

        fun onNotesChange(text: String) {
            if (_uiState.value.isSubmitting) return
            val capped = capNotes(text)
            _uiState.update { it.copy(notes = capped) }
            savedStateHandle[KEY_NOTES] = capped
        }

        /**
         * Port of iOS `book()`. A tap while a request is in flight, or after one has succeeded, is ignored (D-11
         * single flight). The clock and zone are refreshed first (WR-03): a slot that is no longer after now is
         * cleared and the time-has-passed notice says why (IN-09, WR-02), so a past `scheduled_start`, which the
         * server does not reject, is never sent. A free topic goes to `/bookings/request` (BOOK-03); a paid topic
         * runs the paid flow (PAY-03, PAY-04) once the client has a saved card. With none the CTA routes to Payment
         * Methods instead (D-08) and this does nothing.
         *
         * Failures are mapped by [toBookingFailure] (free) and [toPaidFailure] (paid) and shown in one AlertDialog
         * titled "Couldn't book" (the screen, 06-12). The 422 MEETING_LINK_UNAVAILABLE cannot be prevented up front
         * because Phase 6 keeps D-08 as written: both meeting types are always offered.
         */
        fun book() {
            if (_uiState.value.isSubmitting) return
            if (syncClock()) {
                explainSlotPassed()
                return
            }
            val s = _uiState.value
            val topic = s.selectedTopic ?: return
            // syncClock() has already cleared a slot that time moved past (WR-02), so a selected slot here is
            // selectable at `s.now`.
            val slot = s.selectedSlot ?: return
            if (topic.isFree) {
                bookFree(s, topic, slot)
            } else {
                if (s.paymentMethods != PaymentMethodsAvailability.Available) return
                val context = PaidContext(topic.id, topic.durationMins, slot.start, s.meetingType, s.notes)
                paidContext = context
                scaAttempted = false
                _uiState.update { it.copy(isSubmitting = true, paidStep = PaidStep.CreatingIntent) }
                viewModelScope.launch { beginPaidBooking(context) }
            }
        }

        /**
         * Port of iOS `performFreeBooking`. The body is the D-11 contract: `topic.durationMins` already defaults to
         * 30 (06-04), notes are trimmed and omitted when blank, and [Instant.toString] is ISO 8601 UTC
         * ("2026-10-22T20:00:00Z"), accepted by the backend `z.string().datetime({ offset: true })`.
         */
        private fun bookFree(
            s: MakeABookingUiState,
            topic: BookingTopicUi,
            slot: BookingTimeSlot,
        ) {
            _uiState.update { it.copy(isSubmitting = true) }
            // Written while the screen is in the foreground, so always before the activity saves its state (WR-01).
            val attemptId = UUID.randomUUID().toString()
            savedStateHandle[KEY_ATTEMPT_ID] = attemptId
            val body =
                BookingRequestBody(
                    expertId = s.expertId,
                    topicId = topic.id,
                    scheduledStart = slot.start.toString(),
                    durationMinutes = topic.durationMins,
                    meetingType = s.meetingType.wire,
                    timezone = s.zone.id,
                    clientNotes = s.notes.trim().ifEmpty { null },
                )
            viewModelScope.launch {
                try {
                    val result = repository.requestBooking(body)
                    // Success is terminal: isSubmitting stays true, so Book Now, Back and the single-flight guard stay
                    // locked while the outgoing entry is still composed during the push to Request Sent. The id is
                    // persisted so a process death before the screen resumes still lands on Request Sent: on disk
                    // first, because the activity may already have saved its state (WR-01), then in the saved state.
                    completedBookings.put(attemptId, result.bookingId)
                    savedStateHandle[KEY_COMPLETED_BOOKING_ID] = result.bookingId
                    _uiState.update { it.copy(completedBookingId = result.bookingId) }
                } catch (e: ApiException) {
                    handleBookingFailure(e)
                }
            }
        }

        /** Port of iOS `beginPaidBooking`: payment-intent, then setup-intent, then the sheet request (PAY-03). */
        private suspend fun beginPaidBooking(context: PaidContext) {
            val attemptId = bookingAttemptId
            val intent =
                try {
                    repository.paymentIntent(
                        PaymentIntentRequest(
                            expertId = expertId,
                            topicId = context.topicId,
                            selectedDate = context.slotStart.toUtcDateString(),
                            timeSlot = context.slotStart.toUtcTimeSlotString(),
                            durationMinutes = context.durationMins,
                            bookingAttemptId = attemptId,
                        ),
                    )
                } catch (e: ApiException) {
                    if (e is ApiException.Http && e.code == ErrorCode.LongLeadRequiresSavedCard) {
                        performLongLeadBooking(context)
                    } else {
                        applyPaidFailure(e, PaidPhase.CreateIntent, null)
                    }
                    return
                }
            paymentIntentId = intent.paymentIntentId
            val setup =
                try {
                    reusableSetupFor(attemptId) ?: mintSetupIntent(attemptId)
                } catch (e: ApiException) {
                    // No auto-retry: setup-intent is limited to 10 calls per 15 minutes (research correction 4).
                    applyPaidFailure(e, PaidPhase.CreateIntent, null)
                    return
                }
            setupIntentClientSecret = setup.clientSecret
            _uiState.update {
                it.copy(
                    paidStep = PaidStep.CollectingPaymentMethod,
                    stripeRequest =
                        StripeRequest.PresentSheet(setup.clientSecret, setup.customerId, setup.ephemeralKey),
                )
            }
        }

        private fun reusableSetupFor(attemptId: String): PaymentMethodSetupData? {
            val cached = reusableSetup ?: return null
            val fresh = monotonicClock.nowMillis() - cached.mintedAt < SETUP_INTENT_REUSE_WINDOW_MS
            return cached.data.takeIf { cached.attemptId == attemptId && fresh }
        }

        private suspend fun mintSetupIntent(attemptId: String): PaymentMethodSetupData {
            reusableSetup = null
            val data = paymentMethodsRepository.createSetupIntent()
            reusableSetup = ReusableSetup(attemptId, data, monotonicClock.nowMillis())
            return data
        }

        /** The host has taken the request and is presenting the sheet; a recreation must not launch it twice. */
        fun onStripeRequestLaunched() {
            _uiState.update { it.copy(stripeRequest = null) }
        }

        /**
         * The payment sheet closed. Ignored unless a sheet is active. A cancel resets silently and keeps the slot and
         * the cached SetupIntent (D-12, D-18); a failure or a completed setup drops it, because a succeeded
         * SetupIntent cannot be reused. On completion the `pm_` is read from the SetupIntent (RESEARCH Pattern 4).
         */
        fun onSheetOutcome(outcome: SheetOutcome) {
            if (_uiState.value.paidStep != PaidStep.CollectingPaymentMethod) return
            when (outcome) {
                SheetOutcome.Canceled -> resetPaidFlow()
                is SheetOutcome.Failed -> {
                    reusableSetup = null
                    showPaidDialog(
                        outcome.message?.let(FieldError::Text)
                            ?: FieldError.Res(R.string.make_a_booking_error_sheet_failed),
                    )
                    resetPaidFlow()
                }
                SheetOutcome.Completed -> {
                    reusableSetup = null
                    val context = paidContext
                    val secret = setupIntentClientSecret
                    if (context == null || secret == null) {
                        resetPaidFlow()
                        return
                    }
                    _uiState.update { it.copy(paidStep = PaidStep.Confirming, stripeRequest = null) }
                    viewModelScope.launch {
                        val paymentMethodId = paymentMethodReader.paymentMethodIdOf(secret)
                        if (paymentMethodId == null) {
                            showPaidDialog(FieldError.Res(R.string.make_a_booking_error_pm_unreadable))
                            resetPaidFlow()
                        } else {
                            confirmPaid(context, paymentMethodId, paymentIntentId)
                        }
                    }
                }
            }
        }

        /**
         * Port of iOS `performLongLeadBooking` (PAY-04): a booking too far ahead for a PaymentIntent is confirmed
         * with a saved card and no `payment_intent_id`. The default card, else the first.
         */
        private suspend fun performLongLeadBooking(context: PaidContext) {
            val paymentMethodId = preferredPaymentMethodId()
            if (paymentMethodId == null) {
                showPaidDialog(FieldError.Res(R.string.make_a_booking_error_no_saved_card))
                resetPaidFlow()
                return
            }
            _uiState.update { it.copy(paidStep = PaidStep.Confirming) }
            confirmPaid(context, paymentMethodId, null)
        }

        private fun preferredPaymentMethodId(): String? =
            (savedPaymentMethods.firstOrNull { it.isDefault } ?: savedPaymentMethods.firstOrNull())?.id

        /**
         * `/bookings/confirm` with the `pm_` and, on the short-lead path, the `pi_`; never `booking_attempt_id`
         * (research correction 5). A fresh recovery id goes to [KEY_ATTEMPT_ID] before the request and the success is
         * recorded under it, exactly like the free path (WR-01, RESEARCH Pitfall 11b). The steps before this one
         * never write it, so a process death there returns an idle form (D-17, RESEARCH Pitfall 14).
         */
        private suspend fun confirmPaid(
            context: PaidContext,
            paymentMethodId: String,
            piId: String?,
        ) {
            pendingPaymentMethodId = paymentMethodId
            val recoveryId = UUID.randomUUID().toString()
            savedStateHandle[KEY_ATTEMPT_ID] = recoveryId
            try {
                val result =
                    repository.confirm(
                        ConfirmBookingRequest(
                            paymentIntentId = piId,
                            expertId = expertId,
                            topicId = context.topicId,
                            startTime = context.slotStart.toString(),
                            meetingType = context.meetingType.wire,
                            stripePaymentMethodId = paymentMethodId,
                            clientNotes = context.notes.trim().ifEmpty { null },
                        ),
                    )
                // Terminal, as the free path: isSubmitting stays true while the screen navigates on.
                completedBookings.put(recoveryId, result.bookingId)
                savedStateHandle[KEY_COMPLETED_BOOKING_ID] = result.bookingId
                clearPaidSecrets()
                _uiState.update {
                    it.copy(
                        paidStep = PaidStep.Idle,
                        stripeRequest = null,
                        challengeAccount = null,
                        completedBookingId = result.bookingId,
                    )
                }
            } catch (e: ApiException) {
                savedStateHandle.remove<String>(KEY_ATTEMPT_ID)
                applyPaidFailure(e, PaidPhase.Confirm, piId)
            }
        }

        /** Port of iOS `handlePaidFlowError` using the 07-03 table (D-13, D-14, D-16, D-19). */
        private fun applyPaidFailure(
            e: ApiException,
            phase: PaidPhase,
            piId: String?,
        ) {
            val afterUnknown = unknownOutcomeAttemptId != null && unknownOutcomeAttemptId == bookingAttemptId
            when (val failure = e.toPaidFailure(phase, piId, afterUnknownOutcome = afterUnknown)) {
                PaidFailure.Silent -> resetPaidFlow()
                is PaidFailure.StartChallenge -> startChallenge(e, failure)
                is PaidFailure.Show -> {
                    if (failure.newAttemptId) newBookingAttempt()
                    // D-16: the attempt id is kept so a retry is idempotent; nothing retries automatically.
                    if (failure.outcomeUnknown) unknownOutcomeAttemptId = bookingAttemptId
                    resetPaidFlow()
                    showPaidDialog(failure.message, failure.paymentReference)
                    if (failure.resetMeetingType) {
                        _uiState.update { it.copy(meetingType = MeetingType.InAppVoice) }
                        savedStateHandle[KEY_MEETING_TYPE] = MeetingType.InAppVoice.wire
                    }
                    if (failure.clearSlotAndReload) {
                        clearSlot()
                        loadAvailability(_uiState.value.displayedMonth, keepVisible = true)
                    }
                }
            }
        }

        /**
         * PAY-05: hands one 3DS challenge to the host in a single state update, so the request and the account id it
         * must be keyed to appear together. [toPaidFailure] only returns [PaidFailure.StartChallenge] for a non-blank
         * connected account; a missing one is already a hard failure there and never falls back to the platform
         * account (D-15). A second 402 in the same confirm attempt is shown instead of challenged again (T-07-41).
         */
        private fun startChallenge(
            e: ApiException,
            failure: PaidFailure.StartChallenge,
        ) {
            if (scaAttempted) {
                val message =
                    (e as? ApiException.Http)?.userMessage?.let(FieldError::Text)
                        ?: FieldError.Res(R.string.login_error_unknown)
                val reference = paymentIntentId
                resetPaidFlow()
                showPaidDialog(message, reference)
                return
            }
            scaAttempted = true
            // WR-04: a non-secret marker, so a process death during the challenge is explained on restore.
            savedStateHandle[KEY_CHALLENGE_PENDING] = true
            _uiState.update {
                it.copy(
                    paidStep = PaidStep.Confirming,
                    stripeRequest = StripeRequest.RunChallenge(failure.clientSecret, failure.stripeAccount),
                    challengeAccount = failure.stripeAccount,
                )
            }
        }

        /**
         * The 3DS challenge closed (iOS `retryConfirmAfterSCA` / `cancelSCA`). Ignored unless a challenge is active.
         *
         * - Completed: retries `/bookings/confirm` once with the same `pm_` and `pi_`; the server then sees
         *   `requires_capture` and only creates the booking. Without a `pm_` the flow ends with the "couldn't be
         *   resolved" notice and no request. That guard is defensive and has no unit test: [confirmPaid] always sets
         *   the `pm_` before a 402 can arrive.
         * - Canceled: an alert that the card wasn't charged (D-15; iOS `cancelSCA()` shows none).
         * - Failed: Stripe's message, else a generic one.
         *
         * Canceled and Failed reset the paid flow and keep the slot and the booking attempt id, so Book Now again is
         * an idempotent retry (D-15).
         */
        fun onChallengeOutcome(outcome: ChallengeOutcome) {
            if (_uiState.value.challengeAccount == null) return
            savedStateHandle.remove<Boolean>(KEY_CHALLENGE_PENDING)
            _uiState.update { it.copy(challengeAccount = null) }
            when (outcome) {
                ChallengeOutcome.Completed -> {
                    val context = paidContext
                    val paymentMethodId = pendingPaymentMethodId
                    if (context == null || paymentMethodId == null) {
                        showPaidDialog(FieldError.Res(R.string.make_a_booking_error_pm_unresolved))
                        resetPaidFlow()
                    } else {
                        viewModelScope.launch { confirmPaid(context, paymentMethodId, paymentIntentId) }
                    }
                }
                ChallengeOutcome.Canceled -> {
                    showPaidDialog(FieldError.Res(R.string.make_a_booking_error_sca_canceled))
                    resetPaidFlow()
                }
                is ChallengeOutcome.Failed -> {
                    showPaidDialog(
                        outcome.message?.let(FieldError::Text)
                            ?: FieldError.Res(R.string.make_a_booking_error_sca_failed),
                    )
                    resetPaidFlow()
                }
            }
        }

        /**
         * Back to an idle form, slot and attempt id untouched unless a refresh that landed during the flow no longer
         * offers the slot (WR-03). A cancelled sheet and every failure end here.
         */
        private fun resetPaidFlow() {
            clearPaidSecrets()
            scaAttempted = false
            savedStateHandle.remove<Boolean>(KEY_CHALLENGE_PENDING)
            _uiState.update {
                it.copy(paidStep = PaidStep.Idle, stripeRequest = null, challengeAccount = null, isSubmitting = false)
            }
            reconcileSlotAfterSubmission()
        }

        /**
         * WR-03: an availability response that landed while the booking was in flight refreshed only the list. Once
         * the form is editable again the held slot is checked against it, exactly as a fresh load would, so a slot
         * that has gone is dropped (and the attempt id rotated) now rather than on the next tap.
         */
        private fun reconcileSlotAfterSubmission() {
            val s = _uiState.value
            val loaded = s.availability as? AvailabilityState.Loaded ?: return
            if (s.selectedSlotStart == null) return
            applyAvailability(loaded.slotsByDate, s.expertTimezone, explainPassedSlot = false)
        }

        private fun clearPaidSecrets() {
            paidContext = null
            paymentIntentId = null
            setupIntentClientSecret = null
            pendingPaymentMethodId = null
        }

        private fun showPaidDialog(
            message: FieldError,
            paymentReference: String? = null,
        ) {
            _uiState.update {
                it.copy(
                    dialogError = message,
                    dialogTitle = R.string.make_a_booking_error_title,
                    dialogPaymentReference = paymentReference,
                )
            }
        }

        private fun newBookingAttempt() {
            bookingAttemptId = UUID.randomUUID().toString()
        }

        /** D-10 mapping with the D-08 meeting reset and the D-09 clear-and-reload side effects. */
        private fun handleBookingFailure(e: ApiException) {
            savedStateHandle.remove<String>(KEY_ATTEMPT_ID)
            when (val failure = e.toBookingFailure()) {
                BookingFailure.Silent -> {
                    _uiState.update { it.copy(isSubmitting = false) }
                    reconcileSlotAfterSubmission()
                }
                is BookingFailure.Show -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            dialogError = failure.message,
                            dialogTitle = R.string.make_a_booking_error_title,
                            dialogPaymentReference = null,
                            meetingType = if (failure.resetMeetingType) MeetingType.InAppVoice else it.meetingType,
                            selectedSlotStart = if (failure.clearSlotAndReload) null else it.selectedSlotStart,
                        )
                    }
                    if (failure.resetMeetingType) savedStateHandle[KEY_MEETING_TYPE] = MeetingType.InAppVoice.wire
                    if (failure.clearSlotAndReload) {
                        pendingSlotMillis = null
                        savedStateHandle.remove<Long>(KEY_SLOT_START)
                        loadAvailability(_uiState.value.displayedMonth, keepVisible = true)
                    } else {
                        reconcileSlotAfterSubmission()
                    }
                }
            }
        }

        /** The error dialog's OK, Back and scrim all call this (D-10). */
        fun dismissError() {
            _uiState.update {
                it.copy(
                    dialogError = null,
                    dialogTitle = R.string.make_a_booking_error_title,
                    dialogPaymentReference = null,
                )
            }
        }

        /**
         * The screen consumes the id, then calls `navigator.replaceTop(RequestSentKey(id))` (D-12); the entry and
         * this ViewModel are then removed. `isSubmitting` is left true, so nothing can be booked again in between.
         * The durable record is discarded too, so it can never send a later flow to Request Sent (WR-01).
         */
        fun consumeCompletedBooking() {
            _uiState.update { it.copy(completedBookingId = null) }
            savedStateHandle.remove<String>(KEY_COMPLETED_BOOKING_ID)
            savedStateHandle.remove<String>(KEY_ATTEMPT_ID)?.let(completedBookings::discard)
        }

        private fun clearSlot() {
            pendingSlotMillis = null
            if (_uiState.value.selectedSlotStart != null) newBookingAttempt()
            _uiState.update { it.copy(selectedSlotStart = null) }
            savedStateHandle.remove<Long>(KEY_SLOT_START)
        }

        /**
         * Loads [month] and both neighbours in parallel (D-07, iOS parity: `topic_id` is never sent). Only the
         * centre month can fail the section; a failing neighbour is silent. Slots of all three responses are
         * flattened and regrouped by the viewer's local date in one [groupSlots] call. The previous load is
         * cancelled first, so a stale response can never overwrite a newer month (iOS has that race).
         * [keepVisible] leaves the current slots on screen while loading (06-11 uses it after a 409).
         */
        private fun loadAvailability(
            month: YearMonth,
            keepVisible: Boolean,
        ) {
            availabilityJob?.cancel()
            if (!keepVisible) _uiState.update { it.copy(availability = AvailabilityState.Loading) }
            availabilityJob =
                viewModelScope.launch {
                    try {
                        val (previous, center, next) =
                            coroutineScope {
                                val centerCall = async { repository.availability(expertId, month) }
                                val previousCall = async { neighbor(month.minusMonths(1)) }
                                val nextCall = async { neighbor(month.plusMonths(1)) }
                                Triple(previousCall.await(), centerCall.await(), nextCall.await())
                            }
                        if (_uiState.value.displayedMonth != month) return@launch
                        val days = listOfNotNull(previous, center, next).flatMap { it.days }
                        loadedDays = days
                        loadedExpertTimezone = center.expertTimezone
                        applyAvailability(
                            groupSlots(days, _uiState.value.zone),
                            center.expertTimezone,
                            explainPassedSlot = true,
                        )
                    } catch (e: ApiException) {
                        if (_uiState.value.displayedMonth == month) {
                            _uiState.update { it.copy(availability = AvailabilityState.Failed(e.toSectionMessage())) }
                        }
                    }
                }
        }

        private suspend fun neighbor(month: YearMonth): ExpertAvailabilityData? =
            try {
                repository.availability(expertId, month)
            } catch (e: ApiException) {
                null
            }

        /**
         * Stores freshly grouped slots, then reconciles the slot: a restored slot is applied only if it is present
         * and selectable now (T-06-20); a held slot that has since become unavailable or past is dropped. With
         * [explainPassedSlot], a slot dropped because it has already started (for example a selection restored after
         * process death) is explained with the time-has-passed notice (WR-02); one dropped as unavailable is not, as
         * before. The synchronous regroup inside [syncClock] passes false and leaves the decision to its caller.
         *
         * WR-03: while a booking is in flight (or after it succeeded) a late response only refreshes the slot list.
         * The selection, its saved copy and the booking attempt id stay as they were, so the request, the Summary and
         * the D-16/D-19 bookkeeping keep referring to the slot that was sent. If that slot has really gone, the
         * server says so and the failure handling clears it.
         */
        private fun applyAvailability(
            grouped: Map<LocalDate, List<BookingTimeSlot>>,
            expertTimezone: String?,
            explainPassedSlot: Boolean,
        ) {
            val now = clock.instant()
            if (_uiState.value.isSubmitting) {
                _uiState.update {
                    it.copy(
                        availability = AvailabilityState.Loaded(grouped),
                        expertTimezone = expertTimezone,
                        now = now,
                    )
                }
                return
            }
            val pending = pendingSlotMillis?.let(Instant::ofEpochMilli)
            val previousSlot = _uiState.value.selectedSlotStart
            val held = pending ?: previousSlot
            _uiState.update { s ->
                val slots = grouped[s.selectedDate].orEmpty()
                val selectable = { start: Instant? ->
                    start != null && slots.any { it.start == start && it.isSelectable(now) }
                }
                val slot =
                    when {
                        selectable(pending) -> pending
                        selectable(s.selectedSlotStart) -> s.selectedSlotStart
                        else -> null
                    }
                s.copy(
                    availability = AvailabilityState.Loaded(grouped),
                    expertTimezone = expertTimezone,
                    now = now,
                    selectedSlotStart = slot,
                )
            }
            pendingSlotMillis = null
            val slot = _uiState.value.selectedSlotStart
            if (slot != previousSlot) newBookingAttempt()
            if (slot == null) {
                savedStateHandle.remove<Long>(KEY_SLOT_START)
                if (explainPassedSlot && held != null && !held.isAfter(now)) explainSlotPassed()
            } else {
                savedStateHandle[KEY_SLOT_START] = slot.toEpochMilli()
            }
        }

        /**
         * The screen calls this on every resume (WR-03). A selected slot that time has moved past is cleared by
         * [syncClock] and, since no tap has replaced it, the time-has-passed notice says why (IN-09, WR-02).
         */
        fun refreshClock() {
            if (syncClock()) explainSlotPassed()
        }

        /**
         * Re-derives the viewer's zone and `today` (WR-03). Runs on every resume ([refreshClock]) and at the start of
         * each date, month, slot and book action, so a screen left open past midnight or across a zone change
         * (travel, or the user changing the setting) never keeps yesterday selectable (D-06) nor labels slots in one
         * zone while grouping them in another. A selected day that is now past moves to today and a past month to
         * the current one (clearing the slot, as any day or month change does); a new zone regroups the slots already
         * loaded, without a request. Ignored while a booking is in flight.
         *
         * WR-02: the selected slot is then reconciled in this one place. If it is no longer selectable (it started,
         * its day passed, or the new zone moved it to another day) it is cleared and true is returned. Only resume
         * and Book Now explain the drop; a date, month or slot tap clears it silently, because the tap already
         * replaces the selection.
         */
        private fun syncClock(): Boolean {
            val s = _uiState.value
            if (s.isSubmitting) return false
            val zone = ZoneId.systemDefault()
            val now = clock.instant()
            val today = LocalDate.now(clock.withZone(zone))
            if (zone == s.zone && today == s.today) {
                _uiState.update { it.copy(now = now) }
            } else {
                moveClock(s, zone, today, now)
            }
            val held = s.selectedSlotStart ?: return false
            val after = _uiState.value
            val stillSelectable = after.selectedSlotStart == held && after.selectedSlot?.isSelectable(after.now) == true
            if (stillSelectable) return false
            clearSlot()
            return true
        }

        /** The day or zone changed: moves a past day or month forward and regroups the loaded slots (WR-03). */
        private fun moveClock(
            s: MakeABookingUiState,
            zone: ZoneId,
            today: LocalDate,
            now: Instant,
        ) {
            val currentMonth = YearMonth.from(today)
            val date = if (isPastDay(s.selectedDate, today)) today else s.selectedDate
            val month = if (s.displayedMonth.isBefore(currentMonth)) currentMonth else s.displayedMonth
            _uiState.update {
                it.copy(zone = zone, today = today, now = now, selectedDate = date, displayedMonth = month)
            }
            if (date != s.selectedDate) {
                savedStateHandle[KEY_SELECTED_DATE] = date.toString()
                clearSlot()
            }
            if (month != s.displayedMonth) {
                savedStateHandle[KEY_DISPLAYED_MONTH] = month.toString()
                clearSlot()
                loadAvailability(month, keepVisible = false)
            } else if (zone != s.zone && s.availability is AvailabilityState.Loaded) {
                // syncClock() reports this drop to its caller, which decides whether to explain it (WR-02).
                applyAvailability(groupSlots(loadedDays, zone), loadedExpertTimezone, explainPassedSlot = false)
            }
        }

        /**
         * IN-09 / WR-02: tells the user that the slot they had picked was dropped because its time has passed. No
         * booking was attempted, so the notice has a neutral title rather than "Couldn't book". A dialog already on
         * screen is kept, so an unread message (for example the WR-01 check-My-Bookings notice on a restore whose
         * slot has since started) is never replaced.
         */
        private fun explainSlotPassed() {
            _uiState.update {
                if (it.dialogError != null) {
                    it
                } else {
                    it.copy(
                        dialogError = FieldError.Res(R.string.make_a_booking_error_slot_passed),
                        dialogTitle = R.string.make_a_booking_notice_slot_passed_title,
                    )
                }
            }
        }

        private fun initialState(key: BookingKey): MakeABookingUiState {
            val now = clock.instant()
            val zone = ZoneId.systemDefault()
            val today = LocalDate.now(clock.withZone(zone))
            val date =
                savedStateHandle
                    .get<String>(KEY_SELECTED_DATE)
                    ?.let { parseOrNull { LocalDate.parse(it) } }
                    ?.takeUnless { isPastDay(it, today) }
                    ?: today
            val currentMonth = YearMonth.from(today)
            val month =
                savedStateHandle
                    .get<String>(KEY_DISPLAYED_MONTH)
                    ?.let { parseOrNull { YearMonth.parse(it) } }
                    ?.takeUnless { it.isBefore(currentMonth) }
                    ?: currentMonth
            val completedBookingId = savedStateHandle.get<String>(KEY_COMPLETED_BOOKING_ID)
            // A booking that was in flight when the state was saved: locked until recoverAttempt() has checked.
            val recoveringAttempt = completedBookingId == null && savedStateHandle.contains(KEY_ATTEMPT_ID)
            return MakeABookingUiState(
                expertId = key.expertId,
                expertName = key.expertName,
                expertTitle = key.expertTitle,
                expertImageUrl = key.expertImageUrl,
                zone = zone,
                today = today,
                now = now,
                selectedDate = date,
                displayedMonth = month,
                meetingType =
                    MeetingType.fromWire(savedStateHandle.get<String>(KEY_MEETING_TYPE)) ?: MeetingType.InAppVoice,
                notes = capNotes(savedStateHandle.get<String>(KEY_NOTES).orEmpty()),
                // A booking that succeeded before process death: stay locked and let the screen navigate on.
                isSubmitting = completedBookingId != null || recoveringAttempt,
                completedBookingId = completedBookingId,
            )
        }

        private fun loadTopics() {
            topicsJob?.cancel()
            _uiState.update { it.copy(topics = TopicsState.Loading) }
            topicsJob =
                viewModelScope.launch {
                    try {
                        val topics = profiles.details(expertId).topicsOfDiscussion.map { it.toBookingTopicUi() }
                        val preferred = pendingTopicId ?: _uiState.value.selectedTopicId
                        val chosen =
                            topics.firstOrNull { it.id == preferred } ?: topics.firstOrNull()
                        _uiState.update { it.copy(topics = TopicsState.Loaded(topics), selectedTopicId = chosen?.id) }
                        persistTopic(chosen?.id)
                    } catch (e: ApiException) {
                        _uiState.update { it.copy(topics = TopicsState.Failed(e.toSectionMessage())) }
                    }
                }
        }

        private fun persistTopic(id: String?) {
            if (id == null) savedStateHandle.remove<String>(KEY_TOPIC_ID) else savedStateHandle[KEY_TOPIC_ID] = id
        }

        /** What the user tapped for one paid flow (T-07-39). Memory only. */
        private data class PaidContext(
            val topicId: String,
            val durationMins: Int,
            val slotStart: Instant,
            val meetingType: MeetingType,
            val notes: String,
        )

        /** A SetupIntent minted for [attemptId] at [mintedAt] (monotonic millis). Memory only. */
        private class ReusableSetup(
            val attemptId: String,
            val data: PaymentMethodSetupData,
            val mintedAt: Long,
        )

        private companion object {
            const val KEY_TOPIC_ID = "booking_topic_id"
            const val KEY_SELECTED_DATE = "booking_selected_date"
            const val KEY_DISPLAYED_MONTH = "booking_displayed_month"
            const val KEY_SLOT_START = "booking_slot_start"
            const val KEY_MEETING_TYPE = "booking_meeting_type"
            const val KEY_NOTES = "booking_notes"
            const val KEY_COMPLETED_BOOKING_ID = "booking_completed_id"
            const val KEY_ATTEMPT_ID = "booking_attempt_id"
            const val KEY_CHALLENGE_PENDING = "booking_challenge_pending"
        }
    }

/** Restored saved-state strings may be corrupt; a bad value is ignored rather than crashing. */
private inline fun <T> parseOrNull(parse: () -> T): T? =
    try {
        parse()
    } catch (e: DateTimeParseException) {
        null
    }
