package com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.model.enums.MeetingType
import com.uptrendinvestments.clickmeapp.model.requests.BookingRequestBody
import com.uptrendinvestments.clickmeapp.model.responses.AvailabilityDayDto
import com.uptrendinvestments.clickmeapp.model.responses.ExpertAvailabilityData
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.screens.clientmode.expertprofile.ExpertProfileRepository
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
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
 * Make a Booking state machine, free path. Port of iOS `MakeABookingViewModel`; `book()` is the free booking action (BOOK-03).
 *
 * - D-02: the first free topic is preselected, else the first topic, else none. Phase 7 may revert this to
 *   the plain first topic, and iOS regenerates its booking attempt id on a topic change, which Phase 7 owns.
 * - D-03: opens on today with no auto-jump to the first open day. D-06: no month before the current one and
 *   no past day. D-07: the displayed month plus both neighbours load in parallel and are regrouped by local
 *   date; changing the month or the day clears the slot, changing the meeting type or the notes does not.
 *   iOS lets a slow response for an old month overwrite the new one; here every load cancels the previous.
 * - D-08: both meeting types are always offered; the expert's `available_meeting_types` is never read.
 * - While a booking is in flight (and after it succeeds) every selection setter is ignored, so the request, the
 *   Summary and the 409/422 side effects all refer to the same slot, topic and meeting type.
 * - Selection state (topic, day, month, slot, meeting type, notes) is mirrored to [SavedStateHandle] so it
 *   survives process death. A restored topic or slot is only applied if it is still present in the freshly
 *   loaded data (and, for a slot, still selectable). A completed booking id is persisted too, until the screen
 *   consumes it. A success that lands after the activity saved its state (the app is in the background) is not in
 *   that saved state, so it is also written to the durable [CompletedBookingStore] under an attempt id that is put
 *   in the saved state before the request is sent (WR-01). A ViewModel restored with that attempt id looks the
 *   record up before it shows the form.
 */
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

        /** The raw days of the last applied availability load, kept so a zone change can regroup without a request. */
        private var loadedDays: List<AvailabilityDayDto> = emptyList()
        private var loadedExpertTimezone: String? = null

        init {
            val attemptId = savedStateHandle.get<String>(KEY_ATTEMPT_ID)
            when {
                // A restored completed booking navigates straight to Request Sent, so there is nothing to load.
                _uiState.value.completedBookingId != null -> Unit
                // A booking was in flight when the state was saved: it may have succeeded after the save (WR-01).
                attemptId != null -> recoverAttempt(attemptId)
                else -> loadForm()
            }
        }

        private fun loadForm() {
            loadTopics()
            loadAvailability(_uiState.value.displayedMonth, keepVisible = false)
        }

        /**
         * WR-01: the form stays locked while the durable store is read. A recorded success goes on to Request Sent
         * exactly like a completed id from the saved state; otherwise the attempt is forgotten and the form loads.
         */
        private fun recoverAttempt(attemptId: String) {
            viewModelScope.launch {
                val bookingId = completedBookings.get(attemptId)
                if (bookingId != null) {
                    savedStateHandle[KEY_COMPLETED_BOOKING_ID] = bookingId
                    _uiState.update { it.copy(completedBookingId = bookingId) }
                } else {
                    savedStateHandle.remove<String>(KEY_ATTEMPT_ID)
                    _uiState.update { it.copy(isSubmitting = false) }
                    loadForm()
                }
            }
        }

        fun retryTopics() = loadTopics()

        fun retryAvailability() = loadAvailability(_uiState.value.displayedMonth, keepVisible = false)

        /** Changing the topic clears the slot (D-02); re-selecting the current topic is a no-op. */
        fun selectTopic(topicId: String) {
            if (_uiState.value.isSubmitting || _uiState.value.selectedTopicId == topicId) return
            pendingTopicId = null
            _uiState.update { it.copy(selectedTopicId = topicId, now = clock.instant()) }
            persistTopic(topicId)
            clearSlot()
        }

        /** D-06: ignored on the current month. Clears the slot, keeps the day (D-07). */
        fun goToPreviousMonth() {
            if (_uiState.value.isSubmitting) return
            refreshClock()
            if (!_uiState.value.canGoToPreviousMonth) return
            moveToMonth(_uiState.value.displayedMonth.minusMonths(1))
        }

        fun goToNextMonth() {
            if (_uiState.value.isSubmitting) return
            refreshClock()
            moveToMonth(_uiState.value.displayedMonth.plusMonths(1))
        }

        private fun moveToMonth(month: YearMonth) {
            _uiState.update { it.copy(displayedMonth = month, now = clock.instant()) }
            savedStateHandle[KEY_DISPLAYED_MONTH] = month.toString()
            clearSlot()
            loadAvailability(month, keepVisible = false)
        }

        /** D-06: past days are ignored. The month does not change; the slot is cleared. */
        fun selectDate(date: LocalDate) {
            if (_uiState.value.isSubmitting) return
            refreshClock()
            if (isPastDay(date, _uiState.value.today)) return
            _uiState.update { it.copy(selectedDate = date, now = clock.instant()) }
            savedStateHandle[KEY_SELECTED_DATE] = date.toString()
            clearSlot()
        }

        /** D-04: only an available slot that starts after now can be chosen. */
        fun selectSlot(start: Instant) {
            if (_uiState.value.isSubmitting) return
            refreshClock()
            val now = clock.instant()
            val slot = _uiState.value.slotsForSelectedDate.firstOrNull { it.start == start }
            if (slot == null || !slot.isSelectable(now)) return
            pendingSlotMillis = null
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
         * Port of iOS `book()` + `performFreeBooking`, free topics only (BOOK-03). A tap while a request is in
         * flight, or after one has succeeded, is ignored (D-11 single flight), and a paid topic never reaches the
         * API: the paid path is Phase 7 (D-01), so there is no payment-methods gate here either. The clock and zone
         * are refreshed first (WR-03), which clears a slot that is no longer after now and says the time has passed
         * (IN-09, WR-02), so a past `scheduled_start`, which the server does not reject, is never sent. The request's
         * `timezone` is the zone the slots were grouped in.
         *
         * The body is the D-11 contract: `topic.durationMins` already defaults to 30 (06-04), notes are trimmed
         * and omitted when blank, and [Instant.toString] is ISO 8601 UTC ("2026-10-22T20:00:00Z"), accepted by
         * the backend `z.string().datetime({ offset: true })`.
         *
         * Failures are mapped by [toBookingFailure] and shown in one AlertDialog titled "Couldn't book" (the
         * screen, 06-12). The 422 MEETING_LINK_UNAVAILABLE cannot be prevented up front because Phase 6 keeps
         * D-08 as written: both meeting types are always offered and `available_meeting_types` is not read.
         */
        fun book() {
            if (_uiState.value.isSubmitting) return
            refreshClock()
            val s = _uiState.value
            val topic = s.selectedTopic ?: return
            if (!topic.isFree) return
            // refreshClock() has already cleared, and explained, a slot that time moved past (WR-02), so a selected
            // slot here is selectable at `s.now`.
            val slot = s.selectedSlot ?: return
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

        /** D-10 mapping with the D-08 meeting reset and the D-09 clear-and-reload side effects. */
        private fun handleBookingFailure(e: ApiException) {
            savedStateHandle.remove<String>(KEY_ATTEMPT_ID)
            when (val failure = e.toBookingFailure()) {
                BookingFailure.Silent -> _uiState.update { it.copy(isSubmitting = false) }
                is BookingFailure.Show -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            dialogError = failure.message,
                            meetingType = if (failure.resetMeetingType) MeetingType.InAppVoice else it.meetingType,
                            selectedSlotStart = if (failure.clearSlotAndReload) null else it.selectedSlotStart,
                        )
                    }
                    if (failure.resetMeetingType) savedStateHandle[KEY_MEETING_TYPE] = MeetingType.InAppVoice.wire
                    if (failure.clearSlotAndReload) {
                        pendingSlotMillis = null
                        savedStateHandle.remove<Long>(KEY_SLOT_START)
                        loadAvailability(_uiState.value.displayedMonth, keepVisible = true)
                    }
                }
            }
        }

        /** The error dialog's OK, Back and scrim all call this (D-10). */
        fun dismissError() {
            _uiState.update { it.copy(dialogError = null) }
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
                        applyAvailability(groupSlots(days, _uiState.value.zone), center.expertTimezone)
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
         * and selectable now (T-06-20); a held slot that has since become unavailable or past is dropped. A slot
         * dropped because it has already started (for example a selection restored after process death) is
         * explained with the time-has-passed dialog (WR-02); one dropped as unavailable is not, as before.
         */
        private fun applyAvailability(
            grouped: Map<LocalDate, List<BookingTimeSlot>>,
            expertTimezone: String?,
        ) {
            val now = clock.instant()
            val pending = pendingSlotMillis?.let(Instant::ofEpochMilli)
            val held = pending ?: _uiState.value.selectedSlotStart
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
            if (slot == null) {
                savedStateHandle.remove<Long>(KEY_SLOT_START)
                if (held != null && !held.isAfter(now)) explainSlotPassed()
            } else {
                savedStateHandle[KEY_SLOT_START] = slot.toEpochMilli()
            }
        }

        /**
         * Re-derives the viewer's zone and `today` (WR-03). The screen calls this on every resume and the
         * ViewModel at the start of each date, month, slot and book action, so a screen left open past midnight or
         * across a zone change (travel, or the user changing the setting) never keeps yesterday selectable (D-06)
         * nor labels slots in one zone while grouping them in another. A selected day that is now past moves to
         * today and a past month to the current one (clearing the slot, as any day or month change does); a new
         * zone regroups the slots already loaded, without a request. Ignored while a booking is in flight.
         *
         * WR-02: the selected slot is then reconciled in this one place. If it is no longer selectable (it started,
         * its day passed, or the new zone moved it to another day) it is cleared and the time-has-passed dialog
         * (IN-09) says why, rather than the slot vanishing silently or staying selected with Book Now disabled.
         */
        fun refreshClock() {
            val s = _uiState.value
            if (s.isSubmitting) return
            val zone = ZoneId.systemDefault()
            val now = clock.instant()
            val today = LocalDate.now(clock.withZone(zone))
            if (zone == s.zone && today == s.today) {
                _uiState.update { it.copy(now = now) }
            } else {
                moveClock(s, zone, today, now)
            }
            val held = s.selectedSlotStart ?: return
            val after = _uiState.value
            val stillSelectable = after.selectedSlotStart == held && after.selectedSlot?.isSelectable(after.now) == true
            if (!stillSelectable) {
                clearSlot()
                explainSlotPassed()
            }
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
                applyAvailability(groupSlots(loadedDays, zone), loadedExpertTimezone)
            }
        }

        /** IN-09 / WR-02: tells the user that the slot they had picked was dropped because its time has passed. */
        private fun explainSlotPassed() {
            _uiState.update { it.copy(dialogError = FieldError.Res(R.string.make_a_booking_error_slot_passed)) }
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
                            topics.firstOrNull { it.id == preferred }
                                ?: topics.firstOrNull { it.isFree }
                                ?: topics.firstOrNull()
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

        private companion object {
            const val KEY_TOPIC_ID = "booking_topic_id"
            const val KEY_SELECTED_DATE = "booking_selected_date"
            const val KEY_DISPLAYED_MONTH = "booking_displayed_month"
            const val KEY_SLOT_START = "booking_slot_start"
            const val KEY_MEETING_TYPE = "booking_meeting_type"
            const val KEY_NOTES = "booking_notes"
            const val KEY_COMPLETED_BOOKING_ID = "booking_completed_id"
            const val KEY_ATTEMPT_ID = "booking_attempt_id"
        }
    }

/** Restored saved-state strings may be corrupt; a bad value is ignored rather than crashing. */
private inline fun <T> parseOrNull(parse: () -> T): T? =
    try {
        parse()
    } catch (e: DateTimeParseException) {
        null
    }
