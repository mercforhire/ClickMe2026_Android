package com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uptrendinvestments.clickmeapp.model.enums.MeetingType
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.screens.clientmode.expertprofile.ExpertProfileRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/**
 * Make a Booking state machine, free path. Port of iOS `MakeABookingViewModel`; `book()` is added by 06-11.
 *
 * - D-02: the first free topic is preselected, else the first topic, else none. Phase 7 may revert this to
 *   the plain first topic, and iOS regenerates its booking attempt id on a topic change, which Phase 7 owns.
 * - D-08: both meeting types are always offered; the expert's `available_meeting_types` is never read.
 * - Selection state (topic, day, month, slot, meeting type, notes) is mirrored to [SavedStateHandle] so it
 *   survives process death. A restored topic or slot is only applied if it is still present in the freshly
 *   loaded data (and, for a slot, still selectable).
 */
@HiltViewModel(assistedFactory = MakeABookingViewModel.Factory::class)
class MakeABookingViewModel
    @AssistedInject
    constructor(
        @Assisted key: BookingKey,
        private val savedStateHandle: SavedStateHandle,
        private val profiles: ExpertProfileRepository,
        private val repository: BookingRepository,
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

        private var topicsJob: Job? = null

        init {
            loadTopics()
        }

        fun retryTopics() = loadTopics()

        private fun initialState(key: BookingKey): MakeABookingUiState {
            val now = clock.instant()
            val today = LocalDate.now(clock.withZone(ZoneId.systemDefault()))
            val date =
                savedStateHandle
                    .get<String>(KEY_SELECTED_DATE)
                    ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                    ?.takeUnless { isPastDay(it, today) }
                    ?: today
            val currentMonth = YearMonth.from(today)
            val month =
                savedStateHandle
                    .get<String>(KEY_DISPLAYED_MONTH)
                    ?.let { runCatching { YearMonth.parse(it) }.getOrNull() }
                    ?.takeUnless { it.isBefore(currentMonth) }
                    ?: currentMonth
            return MakeABookingUiState(
                expertId = key.expertId,
                expertName = key.expertName,
                expertTitle = key.expertTitle,
                expertImageUrl = key.expertImageUrl,
                today = today,
                now = now,
                selectedDate = date,
                displayedMonth = month,
                meetingType =
                    MeetingType.fromWire(savedStateHandle.get<String>(KEY_MEETING_TYPE)) ?: MeetingType.InAppVoice,
                notes = capNotes(savedStateHandle.get<String>(KEY_NOTES).orEmpty()),
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
        }
    }
