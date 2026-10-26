package com.uptrendinvestments.clickmeapp.screens.expertmode.availability

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uptrendinvestments.clickmeapp.components.buttons.SaveState
import com.uptrendinvestments.clickmeapp.constants.Timezones
import com.uptrendinvestments.clickmeapp.managers.CallGuard
import com.uptrendinvestments.clickmeapp.managers.RestrictedAction
import com.uptrendinvestments.clickmeapp.model.requests.AvailabilityTimeRangeBody
import com.uptrendinvestments.clickmeapp.model.requests.UpdateMyAvailabilityRequest
import com.uptrendinvestments.clickmeapp.model.requests.WeeklyScheduleBody
import com.uptrendinvestments.clickmeapp.model.responses.AvailabilityTimeRangeDto
import com.uptrendinvestments.clickmeapp.model.responses.ExpertAvailabilityScheduleData
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.toSectionMessage
import com.uptrendinvestments.clickmeapp.screens.expertmode.toExpertDialogError
import com.uptrendinvestments.clickmeapp.ui.theme.Motion
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import javax.inject.Inject

/**
 * State holder for the Expert's own Availability screen (EXP-03, EXP-04). Port of iOS
 * `AvailabilitySettingsViewModel`.
 *
 * Guard rules: Save goes through `CallGuard.attempt(RestrictedAction.EditAvailability)` before any state change
 * (D-03, Phase 9 D-13), so during a call nothing is sent and the guard dialog is queued by CallCenter. The silent
 * UTC auto-fill (D-15) only reads `isCallActive`: calling `attempt()` there would queue the dialog just for
 * opening the screen.
 *
 * D-15 runs once per screen session, on the first successful hydrate, and never after a process-death restore or
 * a refresh. A failed silent PUT is only retried by a new screen session. It is also skipped when hydrate dropped
 * any malformed or inverted server slot (WR-04), because the PUT would delete those slots; the zone is then only
 * filled locally and goes out with the next explicit Save.
 *
 * Edits are mirrored into [SavedStateHandle] so unsaved work survives process death (D-16); there are no dirty
 * flags and leaving the screen discards edits without a prompt. Save shows the shared [SaveState]: Idle, Saving,
 * then Saved for the 2-second hold, then Idle.
 */
@HiltViewModel
class AvailabilityViewModel
    @Inject
    constructor(
        private val savedStateHandle: SavedStateHandle,
        private val repository: AvailabilityRepository,
        private val callGuard: CallGuard,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(AvailabilityUiState())
        val uiState: StateFlow<AvailabilityUiState> = _uiState.asStateFlow()

        private var loadJob: Job? = null

        /** Unsaved edits were restored after process death: the first load must not overwrite them. */
        private var restored = false
        private var didAutoDetect = savedStateHandle.get<Boolean>(KEY_DID_AUTO_DETECT) ?: false

        init {
            val saved = AvailabilityScheduleLogic.decode(savedStateHandle.get<String>(KEY_SCHEDULE))
            if (saved != null) {
                restored = true
                _uiState.update { it.copy(days = saved.first, timezoneId = saved.second) }
            }
            load(LoadMode.Initial)
        }

        private enum class LoadMode { Initial, Pull }

        fun retry() = load(LoadMode.Initial)

        /** Pull-to-refresh: overwrites local edits with the server copy (iOS parity); a failure keeps them. */
        fun refresh() = load(LoadMode.Pull)

        private fun load(mode: LoadMode) {
            loadJob?.cancel()
            _uiState.update {
                when (mode) {
                    LoadMode.Initial -> it.copy(load = AvailabilityLoad.Loading)
                    LoadMode.Pull -> it.copy(isRefreshing = true)
                }
            }
            loadJob =
                viewModelScope.launch {
                    try {
                        val data = repository.get()
                        when (mode) {
                            LoadMode.Initial -> applyInitial(data)
                            LoadMode.Pull -> applyPull(data)
                        }
                    } catch (e: ApiException) {
                        _uiState.update {
                            when (mode) {
                                LoadMode.Initial -> it.copy(load = AvailabilityLoad.Failed(e.toSectionMessage()))
                                LoadMode.Pull -> it.copy(isRefreshing = false, refreshFailed = true)
                            }
                        }
                    }
                }
        }

        private fun applyInitial(data: ExpertAvailabilityScheduleData) {
            val serverId = data.timezone?.id ?: DEFAULT_TIMEZONE
            val hydrated = hydrate(data)
            val fill =
                if (!restored && !didAutoDetect) {
                    didAutoDetect = true
                    savedStateHandle[KEY_DID_AUTO_DETECT] = true
                    AvailabilityTimezoneLogic.autoFillZone(serverId, Timezones.detected())
                } else {
                    null
                }
            _uiState.update { state ->
                val withServer =
                    state.copy(
                        load = AvailabilityLoad.Loaded,
                        serverTimezoneId = serverId,
                        serverDisplayName = data.timezone?.displayName,
                    )
                if (restored) withServer else withServer.copy(days = hydrated, timezoneId = fill ?: serverId)
            }
            // WR-04: a lossy hydrate means the PUT body would delete dropped server slots; keep the zone local only.
            if (fill != null && AvailabilityScheduleLogic.isLossless(rawRanges(data))) silentSaveIfNeeded()
        }

        private fun applyPull(data: ExpertAvailabilityScheduleData) {
            val serverId = data.timezone?.id ?: DEFAULT_TIMEZONE
            restored = false
            savedStateHandle.remove<String>(KEY_SCHEDULE)
            _uiState.update {
                it.copy(
                    load = AvailabilityLoad.Loaded,
                    days = hydrate(data),
                    timezoneId = serverId,
                    serverTimezoneId = serverId,
                    serverDisplayName = data.timezone?.displayName,
                    isRefreshing = false,
                )
            }
        }

        private fun hydrate(data: ExpertAvailabilityScheduleData): List<DaySchedule> =
            AvailabilityScheduleLogic.hydrate(rawRanges(data))

        /** The server's seven-day ranges as received, before [AvailabilityScheduleLogic.hydrate] drops anything. */
        private fun rawRanges(data: ExpertAvailabilityScheduleData): Map<DayOfWeek, List<Pair<String?, String?>>> {
            fun ranges(list: List<AvailabilityTimeRangeDto>?) = list.orEmpty().map { it.startTime to it.endTime }
            val schedule = data.recurringSchedule
            return mapOf(
                DayOfWeek.MONDAY to ranges(schedule?.monday),
                DayOfWeek.TUESDAY to ranges(schedule?.tuesday),
                DayOfWeek.WEDNESDAY to ranges(schedule?.wednesday),
                DayOfWeek.THURSDAY to ranges(schedule?.thursday),
                DayOfWeek.FRIDAY to ranges(schedule?.friday),
                DayOfWeek.SATURDAY to ranges(schedule?.saturday),
                DayOfWeek.SUNDAY to ranges(schedule?.sunday),
            )
        }

        fun setDayEnabled(
            day: DayOfWeek,
            enabled: Boolean,
        ) = edit { it.copy(days = AvailabilityScheduleLogic.setEnabled(it.days, day, enabled)) }

        fun addHours(day: DayOfWeek) = edit { it.copy(days = AvailabilityScheduleLogic.addHours(it.days, day)) }

        fun removeSlot(
            day: DayOfWeek,
            index: Int,
        ) = edit { it.copy(days = AvailabilityScheduleLogic.removeSlot(it.days, day, index)) }

        fun updateSlot(
            day: DayOfWeek,
            index: Int,
            slot: TimeSlot,
        ) = edit { it.copy(days = AvailabilityScheduleLogic.replaceSlot(it.days, day, index, slot)) }

        /** Local only: the zone is sent on the next Save (D-14). */
        fun selectTimezone(id: String) = edit { it.copy(timezoneId = id) }

        /** Local only, like [selectTimezone] (D-14). */
        fun autoDetectTimezone() = selectTimezone(Timezones.detected().id)

        private fun edit(transform: (AvailabilityUiState) -> AvailabilityUiState) {
            val next = _uiState.updateAndGet(transform)
            savedStateHandle[KEY_SCHEDULE] = AvailabilityScheduleLogic.encode(next.days, next.timezoneId)
        }

        /** Guarded Save (D-02, D-03). The guard runs before any state change: no spinner and no request in a call. */
        fun save() {
            val state = _uiState.value
            if (state.load !is AvailabilityLoad.Loaded || state.saveState != SaveState.Idle) return
            if (!callGuard.attempt(RestrictedAction.EditAvailability)) return
            if (state.overlappingDays.isNotEmpty()) return
            val request = buildRequest(state)
            _uiState.update { it.copy(saveState = SaveState.Saving) }
            viewModelScope.launch {
                try {
                    // Leaving the screen mid-save still lets the request finish; its result is dropped (UI-SPEC).
                    withContext(NonCancellable) { repository.put(request) }
                    _uiState.update { it.copy(saveState = SaveState.Saved) }
                    savedStateHandle.remove<String>(KEY_SCHEDULE)
                    delay(Motion.AVAILABILITY_SAVED_HOLD_MS.toLong())
                    _uiState.update { it.copy(saveState = SaveState.Idle) }
                } catch (e: ApiException) {
                    _uiState.update { it.copy(saveState = SaveState.Idle, dialog = e.toExpertDialogError()) }
                }
            }
        }

        /**
         * D-15: persists the auto-filled zone without any UI. Skipped while saving, with overlaps, or during a call;
         * the caller also skips it when hydrate dropped a server slot (WR-04). The body is the fully hydrated
         * schedule. Reads `isCallActive` instead of `attempt()` (Pitfall 7). Failures are swallowed.
         */
        private fun silentSaveIfNeeded() {
            val state = _uiState.value
            if (state.saveState == SaveState.Saving) return
            if (state.overlappingDays.isNotEmpty()) return
            if (callGuard.isCallActive.value) return
            val request = buildRequest(state)
            viewModelScope.launch {
                try {
                    withContext(NonCancellable) { repository.put(request) }
                } catch (_: ApiException) {
                    // Quiet by design; a new screen session tries again.
                }
            }
        }

        private fun buildRequest(state: AvailabilityUiState): UpdateMyAvailabilityRequest {
            fun ranges(day: DayOfWeek): List<AvailabilityTimeRangeBody> =
                AvailabilityScheduleLogic
                    .wireRanges(state.days.first { it.day == day })
                    .map { AvailabilityTimeRangeBody(it.first, it.second) }
            return UpdateMyAvailabilityRequest(
                timezoneId = state.timezoneId,
                schedule =
                    WeeklyScheduleBody(
                        monday = ranges(DayOfWeek.MONDAY),
                        tuesday = ranges(DayOfWeek.TUESDAY),
                        wednesday = ranges(DayOfWeek.WEDNESDAY),
                        thursday = ranges(DayOfWeek.THURSDAY),
                        friday = ranges(DayOfWeek.FRIDAY),
                        saturday = ranges(DayOfWeek.SATURDAY),
                        sunday = ranges(DayOfWeek.SUNDAY),
                    ),
            )
        }

        fun dismissDialog() {
            _uiState.update { it.copy(dialog = null) }
        }

        fun onRefreshFailedShown() {
            _uiState.update { it.copy(refreshFailed = false) }
        }

        private companion object {
            const val KEY_SCHEDULE = "availability_schedule"
            const val KEY_DID_AUTO_DETECT = "availability_did_auto_detect"
            const val DEFAULT_TIMEZONE = "UTC"
        }
    }
