package com.uptrendinvestments.clickmeapp.screens.clientmode.explore

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.managers.CallStartRequest
import com.uptrendinvestments.clickmeapp.managers.CallStarter
import com.uptrendinvestments.clickmeapp.managers.JoinClock
import com.uptrendinvestments.clickmeapp.model.enums.MeetingType
import com.uptrendinvestments.clickmeapp.model.responses.ClientHomeData
import com.uptrendinvestments.clickmeapp.model.responses.FeaturedTopicDto
import com.uptrendinvestments.clickmeapp.model.responses.RecommendedExpertDto
import com.uptrendinvestments.clickmeapp.model.responses.TrendingCategoryDto
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.ClientBookingsRepository
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.todaysSessions
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.util.PriceDisplay
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

/**
 * Explore tab state machine (DISC-01). Port of iOS `ExploreClientViewModel` with the per-section errors (D-16),
 * single-select chip toggle (D-09) and sheet selection (D-10) decided here, not in the UI.
 *
 * Every launcher cancels its previous job first, so the newest request always wins and a slow response for an
 * older filter can never overwrite a newer one.
 */
@HiltViewModel
class ExploreViewModel
    @Inject
    constructor(
        private val repository: ExploreRepository,
        private val bookingsRepository: ClientBookingsRepository,
        private val clock: Clock,
        private val savedStateHandle: SavedStateHandle,
        private val callStarter: CallStarter,
        joinClock: JoinClock,
    ) : ViewModel() {
        private val _uiState =
            MutableStateFlow(
                ExploreUiState(
                    selection =
                        savedStateHandle
                            .get<ArrayList<String>>(KEY_SELECTION)
                            .orEmpty()
                            .toSet(),
                ),
            )
        val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()

        /** Shared 30 s wall clock the Join buttons and the Starting now badge are evaluated against (D-03). */
        val joinNow: StateFlow<Instant> = joinClock.now

        private var homeJob: Job? = null
        private var expertsJob: Job? = null
        private var todayJob: Job? = null

        init {
            loadHome(refresh = false)
            loadTodaysSessions()
        }

        /** Join tap on a Today's Sessions card (D-01): starts the call through the one shared entry point. */
        fun join(session: TodaysSessionUi) {
            callStarter.startCall(
                CallStartRequest(
                    bookingId = session.bookingId,
                    peerId = session.expertId,
                    peerName = session.expertName,
                    peerAvatarUrl = session.expertAvatarUrl,
                    topic = session.topic,
                    scheduledStart = session.startTime,
                    scheduledEnd = session.endTime,
                    meetingType = MeetingType.fromWire(session.meetingType),
                ),
            )
        }

        /** Pull-to-refresh: reloads home and Today's Sessions (D-16). */
        fun onRefresh() {
            loadHome(refresh = true)
            loadTodaysSessions()
        }

        /** Retry from a section error: re-sends the same `/client/home` request for the current selection. */
        fun onRetry() {
            val state = _uiState.value
            if (state.categories is SectionState.Error || state.featuredTopics is SectionState.Error) {
                loadHome(refresh = false)
            } else {
                loadExperts()
            }
        }

        /** Trending chip tap (D-09): selects exactly [slug], or clears the filter when it is already selected. */
        fun onChipTap(slug: String) {
            val next = if (slug in _uiState.value.selection) emptySet() else setOf(slug)
            setSelection(next)
            loadExperts()
        }

        /** Categories sheet Apply (D-10). An unchanged selection makes no request. */
        fun applySelection(slugs: Set<String>) {
            if (slugs == _uiState.value.selection) return
            setSelection(slugs)
            loadExperts()
        }

        /** "Clear filter" from the empty state (D-17). */
        fun clearFilter() = applySelection(emptySet())

        fun onRefreshFailureConsumed() {
            _uiState.update { it.copy(refreshFailed = false) }
        }

        private fun setSelection(slugs: Set<String>) {
            savedStateHandle[KEY_SELECTION] = ArrayList(slugs.sorted())
            _uiState.update { it.copy(selection = slugs) }
        }

        private fun loadHome(refresh: Boolean) {
            homeJob?.cancel()
            expertsJob?.cancel()
            val requested = _uiState.value.selection
            val keepData = refresh && _uiState.value.hasLoadedSection()
            _uiState.update {
                if (keepData) {
                    it.copy(isRefreshing = true)
                } else {
                    it.copy(
                        categories = SectionState.Loading,
                        featuredTopics = SectionState.Loading,
                        experts = SectionState.Loading,
                        isRefreshing = false,
                    )
                }
            }
            homeJob =
                viewModelScope.launch {
                    try {
                        val data = repository.home(requested)
                        _uiState.update { it.withHome(data, applyExperts = it.selection == requested) }
                    } catch (e: ApiException) {
                        val message = e.toSectionMessage()
                        _uiState.update { it.withHomeFailure(message, keepData, it.selection == requested) }
                    }
                }
        }

        private fun loadExperts() {
            expertsJob?.cancel()
            _uiState.update { it.copy(experts = SectionState.Loading) }
            val requested = _uiState.value.selection
            expertsJob =
                viewModelScope.launch {
                    try {
                        val data = repository.home(requested)
                        _uiState.update { state ->
                            if (state.selection != requested) {
                                state
                            } else {
                                val experts = SectionState.Loaded(data.recommendedExperts.map { it.toUi() })
                                // A filter never reshuffles a loaded strip; an Apply after a failed home load fills it.
                                state.copy(
                                    experts = experts,
                                    categories =
                                        state.categories.takeIf { it is SectionState.Loaded }
                                            ?: SectionState.Loaded(data.trendingCategories.map { it.toUi() }),
                                    featuredTopics =
                                        state.featuredTopics.takeIf { it is SectionState.Loaded }
                                            ?: SectionState.Loaded(data.featuredTopics.orEmpty().map { it.toUi() }),
                                )
                            }
                        }
                    } catch (e: ApiException) {
                        val message = e.toSectionMessage()
                        _uiState.update {
                            if (it.selection == requested) it.copy(experts = SectionState.Error(message)) else it
                        }
                    }
                }
        }

        private fun loadTodaysSessions() {
            todayJob?.cancel()
            todayJob =
                viewModelScope.launch {
                    val sessions =
                        try {
                            val zone = ZoneId.systemDefault()
                            bookingsRepository
                                .list(ClientBookingsRepository.TYPE_UPCOMING)
                                .bookings
                                .todaysSessions(clock, zone)
                                .map { booking ->
                                    TodaysSessionUi(
                                        bookingId = booking.bookingId,
                                        expertName = booking.expert.fullName,
                                        expertAvatarUrl = booking.expert.avatarUrl,
                                        topic = booking.topic,
                                        startTime = booking.startTime,
                                        endTime = booking.endTime,
                                        expertId = booking.expert.id,
                                        meetingType = booking.meetingType,
                                        status = booking.status,
                                    )
                                }
                        } catch (_: ApiException) {
                            // Silent on failure (D-01): the strip hides and the home sections are unaffected.
                            emptyList()
                        }
                    _uiState.update { it.copy(todaysSessions = sessions) }
                }
        }

        companion object {
            const val KEY_SELECTION = "explore_selection"
        }
    }

private fun ExploreUiState.hasLoadedSection(): Boolean =
    categories is SectionState.Loaded || featuredTopics is SectionState.Loaded || experts is SectionState.Loaded

private fun ExploreUiState.withHome(
    data: ClientHomeData,
    applyExperts: Boolean,
): ExploreUiState =
    copy(
        categories = SectionState.Loaded(data.trendingCategories.map { it.toUi() }),
        featuredTopics = SectionState.Loaded(data.featuredTopics.orEmpty().map { it.toUi() }),
        experts =
            if (applyExperts) SectionState.Loaded(data.recommendedExperts.map { it.toUi() }) else experts,
        isRefreshing = false,
    )

/**
 * A refresh with data on screen keeps it and raises [ExploreUiState.refreshFailed]; sections that were not loaded
 * still show the error. Otherwise every section still waiting on this request gets the error (D-16). A section that
 * is Loaded now is always kept: on a non-refresh load every section started as Loading, so a Loaded one was filled by
 * a concurrent experts request (an Apply during the initial load) and must not be replaced by this failure. The
 * experts section is left to its own request when the selection changed while this one was in flight.
 */
private fun ExploreUiState.withHomeFailure(
    message: FieldError,
    keepData: Boolean,
    selectionUnchanged: Boolean,
): ExploreUiState {
    fun <T> SectionState<T>.orError(): SectionState<T> =
        if (this is SectionState.Loaded) this else SectionState.Error(message)
    return copy(
        categories = categories.orError(),
        featuredTopics = featuredTopics.orError(),
        experts = if (selectionUnchanged) experts.orError() else experts,
        isRefreshing = false,
        refreshFailed = refreshFailed || keepData,
    )
}

private fun ApiException.toSectionMessage(): FieldError =
    (this as? ApiException.Http)?.userMessage?.let { FieldError.Text(it) } ?: FieldError.Res(R.string.error_state_body)

private fun TrendingCategoryDto.toUi() = CategoryChipUi(slug = id, name = name)

private fun RecommendedExpertDto.toUi() =
    ExpertCardUi(
        expertId = expertId,
        name = fullName,
        title = title,
        imageUrl = profileImageUrl,
        rating = rating ?: 0.0,
        reviewCount = reviewCount ?: 0,
        tags = expertiseTags.orEmpty(),
    )

private fun FeaturedTopicDto.toUi() =
    FeaturedTopicUi(
        topicId = topicId,
        title = title,
        price = PriceDisplay.of(isFree, hourlyRate?.amount, hourlyRate?.currency),
        expertId = expert.expertId,
        expertName = expert.fullName,
        expertImageUrl = expert.profileImageUrl,
    )
