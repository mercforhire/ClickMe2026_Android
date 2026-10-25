package com.uptrendinvestments.clickmeapp.screens.expertmode.availability.subviews

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.components.SkeletonBlock
import com.uptrendinvestments.clickmeapp.components.SkeletonGroup
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing

/** Test tag on the [AvailabilitySkeleton] root. */
internal const val AVAILABILITY_SKELETON_TAG = "availability_skeleton"

private val TimezonePillHeight = 56.dp
private val DayCardMinHeight = 72.dp
private val PillShape = RoundedCornerShape(percent = 50)
private const val SKELETON_DAY_CARDS = 4

/** First-load placeholder: a timezone pill and four day cards. */
@Composable
fun AvailabilitySkeleton(modifier: Modifier = Modifier) {
    SkeletonGroup(modifier = modifier.testTag(AVAILABILITY_SKELETON_TAG)) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SkeletonBlock(
                modifier = Modifier.fillMaxWidth().height(TimezonePillHeight),
                shape = PillShape,
            )
            Spacer(modifier = Modifier.height(Spacing.lg))
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                repeat(SKELETON_DAY_CARDS) {
                    SkeletonBlock(
                        modifier = Modifier.fillMaxWidth().heightIn(min = DayCardMinHeight),
                        shape = MaterialTheme.shapes.medium,
                    )
                }
            }
        }
    }
}

@Preview(name = "AvailabilitySkeleton")
@Composable
private fun AvailabilitySkeletonPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            AvailabilitySkeleton(modifier = Modifier.padding(Spacing.md))
        }
    }
}
