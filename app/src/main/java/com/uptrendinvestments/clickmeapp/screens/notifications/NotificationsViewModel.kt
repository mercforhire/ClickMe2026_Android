package com.uptrendinvestments.clickmeapp.screens.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.toSectionMessage
import com.uptrendinvestments.clickmeapp.screens.push.PushLaunchRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.ZoneId
import javax.inject.Inject

/**
 * The notifications list (PUSH-05). Port of iOS `NotificationsListViewModel` (D-16): loads the first 50 rows, no read
 * state, grouped by local day. The device time zone is read at load time.
 */
@HiltViewModel
class NotificationsViewModel
    @Inject
    constructor(
        private val repository: NotificationsRepository,
        private val launchRequest: PushLaunchRequest,
        private val clock: Clock,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(NotificationsUiState())
        val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

        init {
            startLoad()
        }

        private fun startLoad() {
            viewModelScope.launch {
                try {
                    val now = clock.instant()
                    val zone = ZoneId.systemDefault()
                    val rows = repository.recent().map { it.toRowModel(now, zone) }
                    _uiState.update { it.copy(load = NotificationsLoad.Loaded(groupNotifications(rows, now, zone))) }
                } catch (e: ApiException) {
                    _uiState.update { it.copy(load = NotificationsLoad.Failed(e.toSectionMessage())) }
                }
            }
        }
    }
