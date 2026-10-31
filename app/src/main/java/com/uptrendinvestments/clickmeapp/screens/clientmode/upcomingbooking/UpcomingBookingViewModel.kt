package com.uptrendinvestments.clickmeapp.screens.clientmode.upcomingbooking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uptrendinvestments.clickmeapp.managers.CallGuard
import com.uptrendinvestments.clickmeapp.managers.CallResumeNotice
import com.uptrendinvestments.clickmeapp.managers.CallStartRequest
import com.uptrendinvestments.clickmeapp.managers.CallStarter
import com.uptrendinvestments.clickmeapp.managers.JoinClock
import com.uptrendinvestments.clickmeapp.managers.RestrictedAction
import com.uptrendinvestments.clickmeapp.model.enums.MeetingType
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.realtime.RealtimeConnection
import com.uptrendinvestments.clickmeapp.realtime.RealtimeEvent
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.isNotFound
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.toSectionMessage
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.ClientBookingsRepository
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.RescheduleResponder
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.RescheduleResponseController
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.RescheduleUiState
import com.uptrendinvestments.clickmeapp.screens.clientmode.reschedule.RescheduleOpener
import com.uptrendinvestments.clickmeapp.screens.clientmode.reschedule.RescheduleSentRequest
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant

/**
 * Upcoming Booking Details state machine (BOOK-06). Port of iOS `UpcomingBookingViewModel` with the D-14 fixes
 * (real status, Money price, payment pill).
 *
 * The route carries only the booking id, so the screen loads its own booking whichever list opened it (RESEARCH
 * Pitfall 12). Loads are latest-wins. Quiet reloads never show Loading and a failed one keeps the details already
 * on screen. They are triggered by [refreshQuietly] (screen shown again, for example back from Payment Methods,
 * D-02) and by a realtime `booking.update` for this booking id only.
 *
 * Reschedule response (BOOK-09, D-01): accept and decline are delegated to the shared [RescheduleResponseController],
 * which reloads the booking before it shows the outcome. Nothing is optimistic. A response that completes after
 * the user left is dropped with `viewModelScope`. The iOS follow-up for the missing response card is logged in
 * plan 08-15.
 *
 * Join Call (CALL-01, D-01): [join] starts the call through the one [CallStarter]. The window only enables the button
 * ([joinNow] ticks every 30 s); the server has the final say on `/join`, so [join] does not re-check it (D-02).
 * Cancel is guarded while this booking's call is live ([attemptCancel], D-13).
 *
 * Phase 15: a failed realtime refetch or Retry with content showing keeps the details and raises `refreshFailed` for
 * the refresh-failed snackbar (D-05); [refresh] is that Retry. The resume note (D-11) is consumed once at construction
 * from [CallResumeNotice]; nothing here touches the call engine, starter or permission gate. A 404 first load is
 * [UpcomingBookingUiState.Unavailable], not a failure.
 *
 * Phase 16.2 D-09: when the client Reschedule picker sends a request it posts to [RescheduleSentRequest.upcomingDetails]
 * and pops back here. The notice is consumed in `init` (this ViewModel was recreated) and while alive (the screen was
 * only covered); either way [rescheduleSent] is raised once and a covered screen reloads quietly. Nothing is
 * optimistic: the pending state comes from the server's booking.
 */
@HiltViewModel(assistedFactory = UpcomingBookingViewModel.Factory::class)
class UpcomingBookingViewModel
    @AssistedInject
    constructor(
        @Assisted key: UpcomingBookingKey,
        private val repository: ClientBookingsRepository,
        responder: RescheduleResponder,
        private val realtime: RealtimeConnection,
        private val clock: Clock,
        private val callStarter: CallStarter,
        private val callGuard: CallGuard,
        joinClock: JoinClock,
        resumeNotice: CallResumeNotice,
        private val sentRequest: RescheduleSentRequest,
    ) : ViewModel() {
        @AssistedFactory
        interface Factory {
            fun create(key: UpcomingBookingKey): UpcomingBookingViewModel
        }

        private val bookingId = key.bookingId

        private val _uiState = MutableStateFlow<UpcomingBookingUiState>(UpcomingBookingUiState.Loading)
        val uiState: StateFlow<UpcomingBookingUiState> = _uiState.asStateFlow()

        private var loadJob: Job? = null
        private var loadIsCurrent = false

        // Consumed once here, so only the screen the relaunch landed on shows the note (D-11, T-15-38).
        private val showCallResumeNote = resumeNotice.consumeFor(bookingId)

        private val controller =
            RescheduleResponseController(
                scope = viewModelScope,
                responder = responder,
                bookingId = bookingId,
                reload = { startLoad(quiet = true).join() },
            )

        val rescheduleState: StateFlow<RescheduleUiState> = controller.state

        /** Shared 30 s wall clock that turns the Join button on without leaving the screen (D-03). */
        val joinNow: StateFlow<Instant> = joinClock.now

        private val _rescheduleSent = MutableStateFlow(false)

        /** True once after the client picker sent a request for this booking (D-09); the screen shows the snackbar. */
        val rescheduleSent: StateFlow<Boolean> = _rescheduleSent.asStateFlow()

        init {
            startLoad(quiet = false)
            // D-09: a request sent before this ViewModel existed is consumed here, and the first load is already fresh.
            // One posted while the screen is covered by the picker forces a quiet reload so "Awaiting expert's
            // response" shows. Only this booking's notice is taken (a details screen for another booking leaves it).
            if (sentRequest.consumeFor(RescheduleOpener.UpcomingDetails, bookingId)) {
                _rescheduleSent.value = true
            }
            viewModelScope.launch {
                sentRequest.upcomingDetails.pending.filterNotNull().collect {
                    if (sentRequest.consumeFor(RescheduleOpener.UpcomingDetails, bookingId)) {
                        _rescheduleSent.value = true
                        // Not optimistic: nothing on screen changes until the server's booking arrives.
                        startLoad(quiet = _uiState.value is UpcomingBookingUiState.Loaded, flagFailure = true)
                    }
                }
            }
            // Only this booking's updates reload (T-08-35); startLoad cancels the previous load, so a burst of
            // events ends with one latest-wins result.
            viewModelScope.launch {
                realtime.events
                    .filterIsInstance<RealtimeEvent.BookingUpdate>()
                    .filter { it.bookingId == bookingId }
                    .collect { startLoad(quiet = _uiState.value is UpcomingBookingUiState.Loaded, flagFailure = true) }
            }
        }

        fun retry() {
            startLoad(quiet = false)
        }

        /** Retry of a failed refresh with content showing (D-05): the details stay and only a progress bar runs. */
        fun refresh() {
            val current = _uiState.value
            if (current !is UpcomingBookingUiState.Loaded) {
                retry()
                return
            }
            _uiState.value = current.copy(isRefreshing = true, refreshFailed = false)
            startLoad(quiet = true, flagFailure = true)
        }

        /** The screen showed the refresh-failed snackbar, so the flag is spent. */
        fun onRefreshFailureConsumed() {
            _uiState.update { if (it is UpcomingBookingUiState.Loaded) it.copy(refreshFailed = false) else it }
        }

        /**
         * The screen is shown again (`ON_RESUME`, first composition). A call while a current load runs does nothing,
         * so first composition never double-loads (RESEARCH Findings 5-6); a Loaded screen reloads quietly.
         */
        fun refreshQuietly() {
            if (loadJob?.isActive == true && loadIsCurrent) return
            // A user's Retry still running must not be cancelled by a silent reload that would not flag its failure.
            if ((_uiState.value as? UpcomingBookingUiState.Loaded)?.isRefreshing == true) return
            startLoad(quiet = _uiState.value is UpcomingBookingUiState.Loaded)
        }

        /**
         * The screen was hidden or disposed. A load started before this may predate a change made while hidden (for
         * example a card added in Payment Methods), so the next [refreshQuietly] restarts it.
         */
        fun onScreenHidden() {
            loadIsCurrent = false
        }

        /** Starts the call for the loaded booking. Does nothing before the booking has loaded. */
        fun join() {
            val details = (_uiState.value as? UpcomingBookingUiState.Loaded)?.details ?: return
            callStarter.startCall(
                CallStartRequest(
                    bookingId = details.bookingId,
                    peerId = details.expertId,
                    peerName = details.expertName,
                    peerAvatarUrl = details.expertAvatarUrl,
                    topic = details.topicTitle,
                    scheduledStart = details.start,
                    scheduledEnd = details.end,
                    meetingType = MeetingType.fromWire(details.meetingType),
                ),
            )
        }

        /** True when Cancel may open; false (with the guard dialog queued) while this booking's call is live (D-13). */
        fun attemptCancel(): Boolean = callGuard.attemptModifyBooking(bookingId, RestrictedAction.CancelBooking)

        fun accept() = controller.accept()

        fun decline(reason: String) = controller.decline(reason)

        fun dismissRescheduleDialog() = controller.dismissDialog()

        fun consumeRescheduleNotice() = controller.consumeNotice()

        /** The screen showed the "Reschedule request sent" snackbar, so the one-shot is spent. */
        fun consumeRescheduleSent() {
            _rescheduleSent.value = false
        }

        /**
         * [flagFailure] makes a failed quiet load raise `refreshFailed` for the snackbar (realtime refetch and
         * Retry). The silent resume reload (D-02) leaves it off.
         */
        private fun startLoad(
            quiet: Boolean,
            flagFailure: Boolean = false,
        ): Job {
            loadJob?.cancel()
            loadIsCurrent = true
            if (!quiet) _uiState.value = UpcomingBookingUiState.Loading
            val job =
                viewModelScope.launch {
                    try {
                        val details = repository.detail(bookingId).toUpcomingDetails(clock.instant())
                        _uiState.value = UpcomingBookingUiState.Loaded(details, showCallResumeNote = showCallResumeNote)
                    } catch (e: ApiException) {
                        if (e.isNotFound()) {
                            // Gone for good, also while the details show: a Retry would only loop on the 404.
                            _uiState.value = UpcomingBookingUiState.Unavailable
                        } else if (quiet) {
                            _uiState.update {
                                if (it is UpcomingBookingUiState.Loaded) {
                                    it.copy(isRefreshing = false, refreshFailed = it.refreshFailed || flagFailure)
                                } else {
                                    it
                                }
                            }
                        } else {
                            _uiState.value = UpcomingBookingUiState.Failed(e.toSectionMessage())
                        }
                    }
                }
            loadJob = job
            return job
        }
    }
