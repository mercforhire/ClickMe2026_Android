package com.uptrendinvestments.clickmeapp.screens.expertmode.availability

import com.uptrendinvestments.clickmeapp.constants.TimezoneEntry
import com.uptrendinvestments.clickmeapp.constants.Timezones
import java.util.Locale

/**
 * Pure timezone rules for Expert availability (EXP-04).
 *
 * Port of `AvailabilityTimezoneRow.swift` and the timezone handling in `AvailabilitySettingsViewModel.swift`.
 * The bundled list and runtime labels come from [Timezones]; the device zone is passed in as [TimezoneEntry]
 * so this object never reads the system zone itself. Must stay free of Android types.
 */
object AvailabilityTimezoneLogic {
    private val UTC_IDS = setOf("utc", "etc/utc")

    /** True for "UTC" and "Etc/UTC" in any case. */
    fun isUtc(id: String?): Boolean = id?.lowercase(Locale.ROOT) in UTC_IDS

    /** The detected device zone id when the server still holds UTC and the device is not on UTC; else null. */
    fun autoFillZone(
        serverId: String?,
        detected: TimezoneEntry,
    ): String? = if (isUtc(serverId) && !isUtc(detected.id)) detected.id else null

    /**
     * Display label: bundled label first, then the server's non-blank display_name for the server's own zone,
     * then a runtime `(GMT+hh:mm) City` label.
     */
    fun label(
        id: String,
        serverId: String?,
        serverDisplayName: String?,
    ): String {
        Timezones.all.firstOrNull { it.id == id }?.let { return it.label }
        if (id == serverId && !serverDisplayName.isNullOrBlank()) return serverDisplayName
        return Timezones.entryFor(id).label
    }

    /**
     * Timezone sheet rows: the selected zone and the server zone when they are not bundled (selected first,
     * no duplicates), followed by the bundled list.
     */
    fun pickerEntries(
        selectedId: String,
        serverId: String?,
        serverDisplayName: String?,
    ): List<TimezoneEntry> {
        val extras = mutableListOf<TimezoneEntry>()
        if (Timezones.all.none { it.id == selectedId }) {
            extras += TimezoneEntry(selectedId, label(selectedId, serverId, serverDisplayName))
        }
        if (serverId != null && serverId != selectedId && Timezones.all.none { it.id == serverId }) {
            extras += TimezoneEntry(serverId, label(serverId, serverId, serverDisplayName))
        }
        return extras + Timezones.all
    }
}
