package com.uptrendinvestments.clickmeapp.screens.clientmode.expertprofile.subviews

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.SkeletonBlock
import com.uptrendinvestments.clickmeapp.components.SkeletonGroup
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.extras

private val SkeletonCellHeight = 72.dp

/**
 * Experience and bookings cells. The Experience cell is dropped when [experienceYears] is unknown, so Bookings takes
 * the full width.
 */
@Composable
fun ExpertProfileStatsBar(
    experienceYears: Int?,
    totalBookings: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        if (experienceYears != null) {
            StatCell(
                label = stringResource(R.string.expert_profile_stat_experience),
                value = pluralStringResource(R.plurals.expert_profile_years, experienceYears, experienceYears),
                modifier = Modifier.weight(1f),
            )
        }
        StatCell(
            label = stringResource(R.string.expert_profile_stat_bookings),
            value = totalBookings.toString(),
            modifier = Modifier.weight(1f),
        )
    }
}

/** Two 72 dp skeleton cells shown while the details load (D-15). */
@Composable
fun ExpertProfileStatsBarSkeleton(modifier: Modifier = Modifier) {
    SkeletonGroup(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            SkeletonBlock(Modifier.weight(1f).height(SkeletonCellHeight), MaterialTheme.shapes.small)
            SkeletonBlock(Modifier.weight(1f).height(SkeletonCellHeight), MaterialTheme.shapes.small)
        }
    }
}

@Composable
private fun StatCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.extras.onSurfaceMuted,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Preview(name = "ExpertProfileStatsBar")
@Composable
private fun ExpertProfileStatsBarPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            ExpertProfileStatsBar(experienceYears = 7, totalBookings = 42)
        }
    }
}
