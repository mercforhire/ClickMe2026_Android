package com.uptrendinvestments.clickmeapp.screens.call.calling.subviews

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.managers.CallTimerState
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Motion
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing

/** Test tag on the ending-soon banner. */
const val ENDING_SOON_BANNER_TAG = "call_ending_soon_banner"

private val BannerIconSize = 20.dp
private const val BANNER_MAX_LINES = 3

private fun <T> visibilitySpec() = tween<T>(Motion.STRENGTH_METER_VISIBILITY_MS)

/**
 * Amber warning row shown in the last 300 s of the session and after the scheduled end (CALL-07). The call itself keeps
 * running until the overtime cutoff, so past the end the copy invites wrapping up instead of announcing a hang-up.
 * Announced politely when it appears. Applies to both the in-app and the external variant.
 */
@Composable
fun EndingSoonBanner(
    timer: CallTimerState,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = timer.isWarning,
        modifier = modifier,
        enter = expandVertically(visibilitySpec()) + fadeIn(visibilitySpec()),
        exit = shrinkVertically(visibilitySpec()) + fadeOut(visibilitySpec()),
    ) {
        val tertiary = MaterialTheme.colorScheme.tertiary
        Surface(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg)
                    .testTag(ENDING_SOON_BANNER_TAG)
                    .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
            shape = MaterialTheme.shapes.small,
            color = tertiary.copy(alpha = 0.1f),
            border = BorderStroke(1.dp, tertiary.copy(alpha = 0.45f)),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.HourglassEmpty,
                    contentDescription = null,
                    modifier = Modifier.size(BannerIconSize),
                    tint = tertiary,
                )
                Spacer(Modifier.width(Spacing.sm))
                Text(
                    text =
                        stringResource(
                            if (timer.isPastScheduledEnd) {
                                R.string.call_banner_time_complete
                            } else {
                                R.string.call_banner_ending_soon
                            },
                        ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = BANNER_MAX_LINES,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Preview(name = "EndingSoonBanner")
@Composable
private fun EndingSoonBannerPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            EndingSoonBanner(CallTimerState(elapsedSeconds = 1500, remainingSeconds = 120))
        }
    }
}
