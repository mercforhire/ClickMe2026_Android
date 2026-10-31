package com.uptrendinvestments.clickmeapp.screens.clientmode.reschedule.subviews

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.formatProposalSlot
import com.uptrendinvestments.clickmeapp.screens.clientmode.reschedule.RESCHEDULE_BANNER_TAG
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

private const val FILL_ALPHA = 0.10f
private const val BORDER_ALPHA = 0.45f
private val IconSize = 16.dp

/**
 * "Current: Wed, Oct 4 · 3:00 PM": the booking's current time, shown above the slots so the user knows what they are
 * moving away from (D-12). The text wraps to two lines at large font scales. The time is taken in [zone] and follows
 * [timeFormat].
 */
@Composable
fun CurrentSessionBanner(
    currentStart: Instant,
    zone: ZoneId,
    timeFormat: java.text.DateFormat,
    locale: Locale,
    modifier: Modifier = Modifier,
) {
    val tertiary = MaterialTheme.colorScheme.tertiary
    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .testTag(RESCHEDULE_BANNER_TAG)
                .semantics(mergeDescendants = true) {},
        shape = MaterialTheme.shapes.small,
        color = tertiary.copy(alpha = FILL_ALPHA),
        border = BorderStroke(1.dp, tertiary.copy(alpha = BORDER_ALPHA)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Update,
                contentDescription = null,
                modifier = Modifier.size(IconSize),
                tint = tertiary,
            )
            Text(
                text =
                    stringResource(
                        R.string.reschedule_start_current_banner,
                        formatProposalSlot(currentStart, zone, locale, timeFormat),
                    ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
