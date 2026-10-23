package com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.realtime.RealtimeConnection
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.toSectionMessage
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.ClientBookingsRepository.Companion.TYPE_PAST
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.ClientBookingsRepository.Companion.TYPE_UPCOMING
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
import javax.inject.Inject

/**
 * My Bookings state machine (BOOK-05). Port of iOS `MyBookingsViewModel`.
 *
 * Every load fetches both buckets (`upcoming` and `past`, page 1, limit 20, D-09) and derives both tabs from the
 * pair with one combined load state, because the rejoin rule moves rows from the server's Past bucket into
 * Upcoming (RESEARCH Finding 1). iOS calls the two buckets sequentially only for Swift Sendable reasons; here they
 * run in parallel. Page 1 only is a known limit, logged in STATE by plan 08-15.
 *
 * Switching sub-tabs never fetches, since both buckets are always loaded together.
 */
@HiltViewModel
class MyBookingsViewModel
    @Inject
    constructor(
        private val savedStateHandle: SavedStateHandle,
        private val repository: ClientBookingsRepository,
        private val realtime: RealtimeConnection,
        private val clock: Clock,
        private val launchRequest: BookingsLaunchRequest,
    ) : ViewModel() {
        private enum class LoadMode {
            /** Shows the spinner and a failure message. */
            Loud,

            /** Silent: keeps the previous lists and swallows a failure. */
            Quiet,

            /** Pull-to-refresh: shows the refresh indicator and flags a failure. */
            Pull,
        }

        private val _uiState =
            MutableStateFlow(
                MyBookingsUiState(
                    selectedTab =
                        BookingsTab.entries.firstOrNull { it.name == savedStateHandle.get<String>(KEY_SELECTED_TAB) }
                            ?: BookingsTab.Upcoming,
                ),
            )
        val uiState: StateFlow<MyBookingsUiState> = _uiState.asStateFlow()

        private var loadJob: Job? = null

        init {
            startLoad(LoadMode.Loud)
        }

        fun retry() = startLoad(LoadMode.Loud)

        fun refresh() = startLoad(LoadMode.Pull)

        /** Selects [tab]; no fetch, both buckets are already loaded. Survives rotation and process death. */
        fun selectTab(tab: BookingsTab) {
            savedStateHandle[KEY_SELECTED_TAB] = tab.name
            _uiState.update { it.copy(selectedTab = tab) }
        }

        fun consumeRefreshFailure() {
            _uiState.update { it.copy(refreshFailed = false) }
        }

        private fun startLoad(mode: LoadMode) {
            loadJob?.cancel()
            _uiState.update {
                when (mode) {
                    LoadMode.Loud -> it.copy(load = MyBookingsLoad.Loading)
                    LoadMode.Pull -> it.copy(isRefreshing = true)
                    LoadMode.Quiet -> it
                }
            }
            loadJob =
                viewModelScope.launch {
                    try {
                        val lists = loadAll()
                        _uiState.update {
                            it.copy(lists = lists, load = MyBookingsLoad.Loaded, isRefreshing = false)
                        }
                    } catch (e: ApiException) {
                        _uiState.update {
                            when (mode) {
                                LoadMode.Loud ->
                                    it.copy(load = MyBookingsLoad.Failed(e.toSectionMessage()), isRefreshing = false)
                                LoadMode.Pull -> it.copy(refreshFailed = true, isRefreshing = false)
                                LoadMode.Quiet -> it.copy(isRefreshing = false)
                            }
                        }
                    }
                }
        }

        private suspend fun loadAll(): BookingLists =
            coroutineScope {
                val upcoming = async { repository.list(TYPE_UPCOMING, PAGE, PAGE_SIZE) }
                val past = async { repository.list(TYPE_PAST, PAGE, PAGE_SIZE) }
                buildBookingLists(upcoming.await().bookings, past.await().bookings, clock.instant())
            }

        companion object {
            const val KEY_SELECTED_TAB = "my_bookings_selected_tab"
            const val PAGE = 1
            const val PAGE_SIZE = 20
        }
    }
