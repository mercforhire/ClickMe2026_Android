package com.uptrendinvestments.clickmeapp.screens.clientmode.expertprofile.subviews

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.PriceText
import com.uptrendinvestments.clickmeapp.components.SkeletonBlock
import com.uptrendinvestments.clickmeapp.components.SkeletonGroup
import com.uptrendinvestments.clickmeapp.screens.clientmode.expertprofile.TopicRowUi
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.extras
import com.uptrendinvestments.clickmeapp.util.PriceDisplay

private val TopicRowMinHeight = 56.dp

/**
 * Topics of Discussion (D-21): one non-clickable row per topic with a trailing price ("Free" in the accent color, a
 * formatted amount, or a dash). The caller hides the card when [topics] is empty (D-12). Server text is plain [Text]
 * only (T-05-29).
 */
@Composable
fun ExpertProfileTopicsCard(
    topics: List<TopicRowUi>,
    modifier: Modifier = Modifier,
) {
    ExpertProfileSectionCard(
        title = stringResource(R.string.expert_profile_topics),
        modifier = modifier,
    ) {
        topics.forEachIndexed { index, topic ->
            if (index > 0) CardDivider()
            TopicRow(topic)
        }
    }
}

@Composable
private fun TopicRow(topic: TopicRowUi) {
    val duration = topic.durationMins?.let { stringResource(R.string.expert_profile_topic_duration, it) }
    val description = topic.description?.takeIf { it.isNotBlank() }
    val subtitle =
        if (duration != null && description != null) {
            stringResource(R.string.expert_profile_topic_line, duration, description)
        } else {
            duration ?: description
        }
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = TopicRowMinHeight).padding(Spacing.md),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = topic.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (subtitle != null) {
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.extras.onSurfaceMuted,
                )
            }
        }
        PriceText(topic.price)
    }
}

/**
 * Shared card shell for the Topics and Reviews sections: a `surfaceContainer` header strip with a heading and an
 * optional trailing [headerAction], then (when [hasContent]) a divider and [content]. With an action the header's end
 * and vertical padding shrink so the 48 dp action sets the strip height; without one the padding is `md` all round.
 */
@Composable
internal fun ExpertProfileSectionCard(
    title: String,
    modifier: Modifier = Modifier,
    headerAction: (@Composable RowScope.() -> Unit)? = null,
    hasContent: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(
                            if (headerAction == null) {
                                PaddingValues(Spacing.md)
                            } else {
                                PaddingValues(
                                    start = Spacing.md,
                                    end = Spacing.xs,
                                    top = Spacing.xs,
                                    bottom = Spacing.xs,
                                )
                            },
                        ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    modifier = Modifier.weight(1f).semantics { heading() },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                headerAction?.invoke(this)
            }
            if (hasContent) {
                CardDivider()
                content()
            }
        }
    }
}

@Composable
internal fun CardDivider() {
    HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
}

/** Card placeholder: a header strip plus [rows] skeleton rows of [rowHeight] (D-15). */
@Composable
fun ExpertProfileCardSkeleton(
    rows: Int,
    rowHeight: Dp,
    modifier: Modifier = Modifier,
) {
    SkeletonGroup(modifier = modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .padding(Spacing.md),
                ) {
                    SkeletonBlock(Modifier.width(160.dp).height(20.dp))
                }
                repeat(rows) {
                    CardDivider()
                    Box(Modifier.fillMaxWidth().height(rowHeight).padding(Spacing.md)) {
                        SkeletonBlock(Modifier.fillMaxSize())
                    }
                }
            }
        }
    }
}

@Preview(name = "ExpertProfileTopicsCard")
@Composable
private fun ExpertProfileTopicsCardPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            ExpertProfileTopicsCard(
                topics =
                    listOf(
                        TopicRowUi("1", "Career advice", 30, "Intro call", PriceDisplay.Free),
                        TopicRowUi("2", "Code review", 60, null, PriceDisplay.Paid("$50.00")),
                    ),
            )
        }
    }
}
