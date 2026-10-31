package com.uptrendinvestments.clickmeapp.screens.clientmode.reschedule

import androidx.lifecycle.SavedStateHandle
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.managers.CallGuard
import com.uptrendinvestments.clickmeapp.model.responses.AvailabilityDayDto
import com.uptrendinvestments.clickmeapp.model.responses.ExpertAvailabilityData
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.AvailabilityState
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.BookingTimeSlot
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.NOTES_MAX_CHARS
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.canGoToPreviousMonth
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.capNotes
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.groupSlots
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.isInLoadedWindow
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.isPastDay
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.toSectionMessage
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import kotlinx.coroutines.CoroutineScope
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

/**
 * The reschedule picker's state holder, shared by the client ViewModel and (plan 08) the expert ViewModel. A plain
 * class over an injected [scope] so it is tested without Android; the ViewModels differ only in the loaders,
 * [proposer] and [mapFailure] they hand it.
 *
 * Loads the booking, then the displayed month and both neighbours from [loadMonth] (Make a Booking's shape), applies
 * the selection rules, and sends through [proposer] exactly once per tap.
 */
@Suppress("LongParameterList", "TooManyFunctions")
class ReschedulePickerController(
    private val scope: CoroutineScope,
    private val savedState: SavedStateHandle,
    private val clock: Clock,
    private val zoneProvider: () -> ZoneId,
    private val bookingId: String,
    private val opener: RescheduleOpener,
    private val loadBooking: suspend () -> PickerBooking?,
    private val loadMonth: suspend (expertId: String, month: YearMonth) -> ExpertAvailabilityData,
    private val proposer: RescheduleProposing,
    @Suppress("unused") private val mapFailure: (ApiException) -> ProposeFailure,
    private val sentRequest: RescheduleSentRequest,
    @Suppress("unused") private val callGuard: CallGuard,
) {
    /** True until the calendar has been seeded from the booking, or restored from [savedState]. */
    private var needsSeed = true
    private val _state = MutableStateFlow(initialState())
    val state: StateFlow<ReschedulePickerUiState> = _state.asStateFlow()

    private var loadJob: Job? = null
    private var availabilityJob: Job? = null
    private var loadedDays: List<AvailabilityDayDto> = emptyList()

    /**
     * Restores month, date, slot and message after process death. Only these are ever saved (T-16.2-18): never
     * `isSubmitting`, a result dialog or a card id. A restored slot is re-validated by [applyAvailability].
     */
    private fun initialState(): ReschedulePickerUiState {
        val zone = zoneProvider()
        val now = clock.instant()
        val today = now.atZone(zone).toLocalDate()
        val savedDate =
            savedState
                .get<String>(KEY_SELECTED_DATE)
                ?.let { parseOrNull { LocalDate.parse(it) } }
                ?.takeUnless { isPastDay(it, today) }
        needsSeed = savedDate == null
        val date = savedDate ?: today
        val savedMonth =
            savedState
                .get<String>(KEY_DISPLAYED_MONTH)
                ?.let { parseOrNull { YearMonth.parse(it) } }
                ?.takeUnless { it.isBefore(YearMonth.from(today)) }
        val month = if (savedDate != null && savedMonth != null) savedMonth else YearMonth.from(date)
        return ReschedulePickerUiState(
            zone = zone,
            now = now,
            displayedMonth = month,
            selectedDate = date,
            selectedDateLoaded = isInLoadedWindow(date, month),
            selectedSlotStart = savedDate?.let { savedState.get<Long>(KEY_SLOT_START) }?.let(Instant::ofEpochMilli),
            message = capNotes(savedState.get<String>(KEY_MESSAGE).orEmpty(), NOTES_MAX_CHARS),
        )
    }

    /** Loads the booking, seeds the calendar from it, then loads the months. */
    fun load() {
        loadJob?.cancel()
        availabilityJob?.cancel()
        _state.update { it.copy(load = PickerLoad.Loading) }
        loadJob =
            scope.launch {
                val booking =
                    try {
                        loadBooking()
                    } catch (e: ApiException) {
                        _state.update { it.copy(load = PickerLoad.Failed(e.toSectionMessage())) }
                        return@launch
                    }
                if (booking == null) {
                    _state.update { it.copy(load = PickerLoad.Failed(FieldError.Res(R.string.login_error_unknown))) }
                    return@launch
                }
                val zone = zoneProvider()
                val now = clock.instant()
                if (needsSeed) {
                    needsSeed = false
                    val date = pickerSeedDate(booking.currentStart, zone, now.atZone(zone).toLocalDate())
                    val month = YearMonth.from(date)
                    _state.update {
                        it.copy(
                            zone = zone,
                            now = now,
                            load = PickerLoad.Loaded(booking),
                            selectedDate = date,
                            displayedMonth = month,
                            selectedDateLoaded = true,
                        )
                    }
                    savedState[KEY_SELECTED_DATE] = date.toString()
                    savedState[KEY_DISPLAYED_MONTH] = month.toString()
                } else {
                    _state.update { it.copy(zone = zone, now = now, load = PickerLoad.Loaded(booking)) }
                }
                loadMonths(_state.value.displayedMonth, keepVisible = false)
            }
    }

    fun retryLoad() = load()

    /** Reloads the displayed month and its neighbours after a section failure. */
    fun retrySlots() {
        if (_state.value.booking == null) return
        loadMonths(_state.value.displayedMonth, keepVisible = false)
    }

    /**
     * Loads [month] and both neighbours in parallel for the booking's expert. Only the centre month can fail the
     * section; a failing neighbour is silent. The previous load is cancelled first, so a stale response can never
     * overwrite a newer month. [keepVisible] leaves the slots on screen while loading.
     */
    private fun loadMonths(
        month: YearMonth,
        keepVisible: Boolean,
    ) {
        val expertId = _state.value.booking?.availabilityExpertId ?: return
        availabilityJob?.cancel()
        if (!keepVisible) _state.update { it.copy(availability = AvailabilityState.Loading) }
        availabilityJob =
            scope.launch {
                try {
                    val (previous, center, next) =
                        coroutineScope {
                            val centerCall = async { loadMonth(expertId, month) }
                            val previousCall = async { neighbor(expertId, month.minusMonths(1)) }
                            val nextCall = async { neighbor(expertId, month.plusMonths(1)) }
                            Triple(previousCall.await(), centerCall.await(), nextCall.await())
                        }
                    if (_state.value.displayedMonth != month) return@launch
                    loadedDays = listOfNotNull(previous, center, next).flatMap { it.days }
                    applyAvailability()
                } catch (e: ApiException) {
                    if (_state.value.displayedMonth == month) {
                        _state.update { it.copy(availability = AvailabilityState.Failed(e.toSectionMessage())) }
                    }
                }
            }
    }

    private suspend fun neighbor(
        expertId: String,
        month: YearMonth,
    ): ExpertAvailabilityData? =
        try {
            loadMonth(expertId, month)
        } catch (_: ApiException) {
            null
        }

    /**
     * Groups [loadedDays] by the viewer's zone and publishes them, then reconciles the selected slot: it stays only
     * if that loaded slot is still pickable now (a restored or carried-over slot that is gone, held, past or the
     * current one is dropped). While a send is in flight a late response only refreshes the list.
     */
    private fun applyAvailability() {
        val now = clock.instant()
        val grouped = groupSlots(loadedDays, _state.value.zone)
        if (_state.value.isSubmitting) {
            _state.update { it.copy(availability = AvailabilityState.Loaded(grouped), now = now) }
            return
        }
        _state.update { s ->
            val latest = s.booking?.let { latestPaidStart(it.isPaid, now) }
            val todays = grouped[s.selectedDate].orEmpty()
            val kept =
                s.selectedSlotStart?.takeIf { start ->
                    todays.any { it.start == start && it.isPickable(now, latest, s.currentStart) }
                }
            s.copy(
                availability = AvailabilityState.Loaded(grouped),
                now = now,
                selectedDateLoaded = isInLoadedWindow(s.selectedDate, s.displayedMonth),
                selectedSlotStart = kept,
            )
        }
        persistSlot()
    }

    private fun persistSlot() {
        val slot = _state.value.selectedSlotStart
        if (slot == null) savedState.remove<Long>(KEY_SLOT_START) else savedState[KEY_SLOT_START] = slot.toEpochMilli()
    }

    /** D-06: ignored on the current month. */
    fun previousMonth() {
        if (_state.value.isSubmitting) return
        _state.update { it.copy(now = clock.instant()) }
        val s = _state.value
        if (!canGoToPreviousMonth(s.displayedMonth, s.today)) return
        moveToMonth(s.displayedMonth.minusMonths(1))
    }

    fun nextMonth() {
        if (_state.value.isSubmitting) return
        _state.update { it.copy(now = clock.instant()) }
        moveToMonth(_state.value.displayedMonth.plusMonths(1))
    }

    /**
     * Moves the calendar. The selected date is kept however far the month moves. Its slot is kept only while the
     * date's month stays in the loaded window (and is re-validated when the months arrive); outside it there is
     * nothing on screen to confirm, so the slot is cleared and Submit stays disabled until a day is picked again.
     */
    private fun moveToMonth(month: YearMonth) {
        _state.update {
            val inWindow = isInLoadedWindow(it.selectedDate, month)
            it.copy(
                displayedMonth = month,
                selectedDateLoaded = inWindow,
                selectedSlotStart = it.selectedSlotStart.takeIf { inWindow },
            )
        }
        savedState[KEY_DISPLAYED_MONTH] = month.toString()
        persistSlot()
        loadMonths(month, keepVisible = false)
    }

    /**
     * Called on every resume. Re-reads the clock and the device zone, regroups the loaded days when the zone changed,
     * and clears a selected slot that has started meanwhile. It never re-evaluates the paid cap for a slot already
     * selected (the server backstop covers it) and never submits (D-08). Ignored while a send is in flight.
     */
    fun refreshClock() {
        if (_state.value.isSubmitting) return
        val zone = zoneProvider()
        val now = clock.instant()
        val zoneChanged = zone != _state.value.zone
        _state.update { it.copy(zone = zone, now = now) }
        if (zoneChanged && _state.value.availability is AvailabilityState.Loaded) applyAvailability()
        val held = _state.value.selectedSlotStart ?: return
        if (!held.isAfter(now)) {
            _state.update { it.copy(selectedSlotStart = null) }
            persistSlot()
        }
    }

    /** Picks the day whose slots are shown; the slot of the previous day is cleared. Past days are ignored. */
    fun selectDate(date: LocalDate) {
        if (_state.value.isSubmitting) return
        _state.update { it.copy(now = clock.instant()) }
        if (isPastDay(date, _state.value.today)) return
        _state.update {
            it.copy(
                selectedDate = date,
                selectedSlotStart = null,
                selectedDateLoaded = isInLoadedWindow(date, it.displayedMonth),
            )
        }
        savedState[KEY_SELECTED_DATE] = date.toString()
        persistSlot()
    }

    /** Selects [start] only when that loaded slot is pickable now (D-01, D-12). */
    fun selectSlot(start: Instant) {
        if (_state.value.isSubmitting) return
        _state.update { it.copy(now = clock.instant()) }
        val s = _state.value
        val slot: BookingTimeSlot = s.slotsForSelectedDate.firstOrNull { it.start == start } ?: return
        if (!slot.isPickable(s.now, s.latestSelectableStart, s.currentStart)) return
        _state.update { it.copy(selectedSlotStart = start) }
        persistSlot()
    }

    fun updateMessage(text: String) {
        val capped = capNotes(text, NOTES_MAX_CHARS)
        _state.update { it.copy(message = capped) }
        savedState[KEY_MESSAGE] = capped
    }

    /** Sends the proposal for the selected slot, once, then hands the success to the opener (D-09). */
    fun submit() {
        val s = _state.value
        if (s.isSubmitting) return
        val slotStart = s.selectedSlotStart ?: return
        _state.update { it.copy(isSubmitting = true) }
        val note = normalizeRescheduleNote(s.message)
        val zone = zoneProvider()
        scope.launch {
            try {
                if (proposer.propose(bookingId, slotStart, zone, note) is ProposeOutcome.Sent) {
                    sentRequest.onSent(opener, bookingId)
                    _state.update { it.copy(sent = true) }
                }
            } finally {
                _state.update { it.copy(isSubmitting = false) }
            }
        }
    }

    fun consumeSent() {
        _state.update { it.copy(sent = false) }
    }

    fun consumeLeave() {
        _state.update { it.copy(leave = false) }
    }

    companion object {
        const val KEY_DISPLAYED_MONTH = "reschedule_displayed_month"
        const val KEY_SELECTED_DATE = "reschedule_selected_date"
        const val KEY_SLOT_START = "reschedule_slot_start"
        const val KEY_MESSAGE = "reschedule_message"
        const val KEY_DISCLOSURE_OPEN = "reschedule_disclosure_open"
    }
}

private inline fun <T> parseOrNull(parse: () -> T): T? =
    try {
        parse()
    } catch (_: DateTimeParseException) {
        null
    }
