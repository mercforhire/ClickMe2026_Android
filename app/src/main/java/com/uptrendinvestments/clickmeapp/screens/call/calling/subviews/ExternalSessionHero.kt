package com.uptrendinvestments.clickmeapp.screens.call.calling.subviews

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.rememberDeviceTimeFormat
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.formatSessionTimeRange
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.extras
import java.text.DateFormat
import java.time.Instant
import java.time.ZoneId

/**
 * Hero for the external (Skype / Zoom / Meet / Teams) call variant: a single 96dp peer avatar with a static ring,
 * the peer name, the topic and the scheduled time range. No pulse, no timer (UI-SPEC surface 2).
 */
@Composable
fun ExternalSessionHero(
    peerName: String?,
    peerAvatarUrl: String?,
    topic: String?,
    scheduledStart: Instant,
    scheduledEnd: Instant?,
    modifier: Modifier = Modifier,
    zone: ZoneId = ZoneId.systemDefault(),
    timeFormat: DateFormat = rememberDeviceTimeFormat(),
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        CallRingAvatar(peerAvatarUrl, 96.dp, 3.dp, Spacing.xs, pulsing = false)
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = peerName?.takeIf { it.isNotBlank() } ?: stringResource(R.string.call_notification_title_fallback),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.semantics { heading() },
        )
        if (!topic.isNullOrBlank()) {
            Text(
                text = topic,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (scheduledEnd != null) {
            Text(
                text = formatSessionTimeRange(scheduledStart, scheduledEnd, zone, timeFormat),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.extras.onSurfaceMuted,
                textAlign = TextAlign.Center,
            )
        }
    }
}
