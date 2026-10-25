package com.uptrendinvestments.clickmeapp.screens.expertmode.topics.subviews

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
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

private val CardMinHeight = 96.dp
private const val SKELETON_CARD_COUNT = 3

/** Test tag on the [TopicsSkeleton] container. */
const val TOPICS_SKELETON_TAG = "topics_skeleton"

/** First-load placeholder for the Topics list: three card-shaped blocks (UI-SPEC Surface 1, Loading). */
@Composable
fun TopicsSkeleton(modifier: Modifier = Modifier) {
    SkeletonGroup(modifier = modifier.testTag(TOPICS_SKELETON_TAG)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.md).padding(top = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            repeat(SKELETON_CARD_COUNT) {
                SkeletonBlock(
                    modifier = Modifier.fillMaxWidth().heightIn(min = CardMinHeight),
                    shape = MaterialTheme.shapes.medium,
                )
            }
        }
    }
}

@Preview(name = "TopicsSkeleton")
@Composable
private fun TopicsSkeletonPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            TopicsSkeleton()
        }
    }
}
