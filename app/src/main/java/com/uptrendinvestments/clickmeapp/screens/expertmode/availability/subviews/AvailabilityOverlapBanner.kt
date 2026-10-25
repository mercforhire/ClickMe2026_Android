package com.uptrendinvestments.clickmeapp.screens.expertmode.availability.subviews

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing

private val BannerIconSize = 20.dp
private const val BANNER_FILL_ALPHA = 0.10f
private const val BANNER_BORDER_ALPHA = 0.45f

/** Banner naming the days whose slots overlap ([daysText], e.g. "Monday and Tuesday"). Polite live region. */
@Composable
fun AvailabilityOverlapBanner(
    daysText: String,
    modifier: Modifier = Modifier,
) {
    val error = MaterialTheme.colorScheme.error
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = error.copy(alpha = BANNER_FILL_ALPHA),
        border = BorderStroke(1.dp, error.copy(alpha = BANNER_BORDER_ALPHA)),
    ) {
        Row(
            modifier = Modifier.padding(Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                modifier = Modifier.size(BannerIconSize),
                tint = error,
            )
            Column(
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Text(
                    text = stringResource(R.string.availability_overlap_title),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.availability_overlap_body, daysText),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Preview(name = "AvailabilityOverlapBanner")
@Composable
private fun AvailabilityOverlapBannerPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            AvailabilityOverlapBanner(daysText = "Monday and Tuesday", modifier = Modifier.padding(Spacing.md))
        }
    }
}
