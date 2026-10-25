package com.uptrendinvestments.clickmeapp.screens.expertmode.availability

import com.uptrendinvestments.clickmeapp.components.buttons.SaveState
import com.uptrendinvestments.clickmeapp.constants.TimezoneEntry
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import java.time.DayOfWeek

/** Load phase of the Availability screen. */
sealed interface AvailabilityLoad {
    data object Loading : AvailabilityLoad

    data class Failed(
        val message: FieldError,
    ) : AvailabilityLoad

    data object Loaded : AvailabilityLoad
}

/**
 * Everything the Availability screen renders. [days] shows the 17:00-21:00 default until the first load completes.
 * [dialog] is a save failure; [refreshFailed] is the one-shot flag behind the pull-to-refresh snackbar.
 */
data class AvailabilityUiState(
    val load: AvailabilityLoad = AvailabilityLoad.Loading,
    val days: List<DaySchedule> = AvailabilityScheduleLogic.defaultSchedule(),
    val timezoneId: String = "UTC",
    val serverTimezoneId: String? = null,
    val serverDisplayName: String? = null,
    val isRefreshing: Boolean = false,
    val saveState: SaveState = SaveState.Idle,
    val dialog: FieldError? = null,
    val refreshFailed: Boolean = false,
) {
    /** Enabled days whose slots overlap, in week order. */
    val overlappingDays: List<DayOfWeek> get() = AvailabilityScheduleLogic.overlappingDays(days)

    val timezoneLabel: String
        get() = AvailabilityTimezoneLogic.label(timezoneId, serverTimezoneId, serverDisplayName)

    val timezoneEntries: List<TimezoneEntry>
        get() = AvailabilityTimezoneLogic.pickerEntries(timezoneId, serverTimezoneId, serverDisplayName)

    /** Save is available once loaded, with no overlaps, and not already saving or showing "Saved". */
    val canSave: Boolean
        get() = load is AvailabilityLoad.Loaded && overlappingDays.isEmpty() && saveState == SaveState.Idle
}
