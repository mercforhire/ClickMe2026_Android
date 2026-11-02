package com.uptrendinvestments.clickmeapp.screens.expertmode

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.model.enums.MeetingType
import com.uptrendinvestments.clickmeapp.model.responses.ExpertBookingItem
import com.uptrendinvestments.clickmeapp.util.Money
import java.util.Locale

/*
 * Display helpers shared by the Phase 11 expert surfaces (D-12): requests, upcoming cards, dashboard and summary.
 *
 * Client names and topics come from the server and are untrusted plain text. Render them only with `Text`: no
 * autolink, no HTML, no markdown.
 */

/** "In-App Voice" or "Skype/Zoom" for a wire value. A missing or unknown value reads as in-app, like iOS. */
@Composable
fun meetingTypeLabel(wire: String?): String =
    stringResource(
        when (MeetingType.fromWire(wire)) {
            MeetingType.SkypeZoom -> R.string.expert_meeting_skype_zoom
            MeetingType.InAppVoice, null -> R.string.expert_meeting_in_app_voice
        },
    )

/** The icon that goes with [meetingTypeLabel]. Decorative: the label text always sits next to it. */
fun meetingTypeIcon(wire: String?): ImageVector =
    when (MeetingType.fromWire(wire)) {
        MeetingType.SkypeZoom -> Icons.Outlined.Groups
        MeetingType.InAppVoice, null -> Icons.Outlined.Videocam
    }

/** The client's trimmed name, or "Client" when the server sent none. */
@Composable
fun clientDisplayName(name: String?): String {
    val fallback = stringResource(R.string.expert_client_fallback_name)
    return name?.trim()?.takeIf { it.isNotEmpty() } ?: fallback
}

/**
 * The net amount the expert earns from [item], formatted, or null when the breakdown has no net amount or no
 * currency. Gross never stands in for net under an "Earnings" label (RESEARCH C4), and a currency is never guessed.
 */
internal fun ExpertBookingItem.netEarningsText(locale: Locale): String? {
    val breakdown = session.earningsBreakdown ?: return null
    val net = breakdown.netAmount ?: return null
    val currency = breakdown.currency ?: session.consultationFee?.currency ?: return null
    return Money.formatMajor(net, currency, locale)
}
