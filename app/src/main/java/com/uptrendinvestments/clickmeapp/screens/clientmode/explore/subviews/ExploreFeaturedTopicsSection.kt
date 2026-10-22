package com.uptrendinvestments.clickmeapp.screens.clientmode.explore.subviews

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.PriceText
import com.uptrendinvestments.clickmeapp.components.SectionError
import com.uptrendinvestments.clickmeapp.components.SectionHeader
import com.uptrendinvestments.clickmeapp.components.avatar.Avatar
import com.uptrendinvestments.clickmeapp.components.buttonstyles.pressScale
import com.uptrendinvestments.clickmeapp.screens.clientmode.explore.FeaturedTopicUi
import com.uptrendinvestments.clickmeapp.screens.clientmode.explore.SectionState
import com.uptrendinvestments.clickmeapp.screens.login.asString
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.extras
import com.uptrendinvestments.clickmeapp.util.PriceDisplay

private val TopicCardWidth = 224.dp
private val TopicCardMinHeight = 176.dp
private val AccentBarWidth = 4.dp
private val TopicAvatarSize = 32.dp

/**
 * Featured Topics (D-03, D-21). Absent while loading or empty (iOS shows no skeleton); an error keeps the heading
 * with Retry. The header has no action: "See all" waits for Search.
 */
@Composable
fun ExploreFeaturedTopicsSection(
    state: SectionState<List<FeaturedTopicUi>>,
    onTopicClick: (FeaturedTopicUi) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        SectionState.Loading -> return
        is SectionState.Loaded -> if (state.data.isEmpty()) return
        is SectionState.Error -> Unit
    }
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(title = stringResource(R.string.explore_section_featured_topics))
        Spacer(Modifier.height(Spacing.md))
        when (state) {
            is SectionState.Error -> SectionError(message = state.message.asString(), onRetry = onRetry)
            is SectionState.Loaded ->
                LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    items(state.data, key = { it.topicId }) { topic ->
                        FeaturedTopicCard(topic = topic, onClick = { onTopicClick(topic) })
                    }
                }
            SectionState.Loading -> Unit
        }
    }
}

@Composable
private fun FeaturedTopicCard(
    topic: FeaturedTopicUi,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.large
    val interactionSource = remember { MutableInteractionSource() }
    val expertName = topic.expertName ?: stringResource(R.string.explore_expert_name_fallback)
    val priceText =
        when (val price = topic.price) {
            PriceDisplay.Free -> stringResource(R.string.price_free)
            is PriceDisplay.Paid -> price.text
            PriceDisplay.Unknown -> stringResource(R.string.price_unknown)
        }
    val description = stringResource(R.string.explore_featured_semantics, topic.title, priceText, expertName)
    val barColor = colors.primary
    Column(
        modifier =
            Modifier
                .width(TopicCardWidth)
                .heightIn(min = TopicCardMinHeight)
                .pressScale(interactionSource)
                .clip(shape)
                .background(colors.surfaceContainerLow)
                .drawBehind { drawRect(color = barColor, size = Size(AccentBarWidth.toPx(), size.height)) }
                .border(1.dp, colors.outlineVariant, shape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    role = Role.Button,
                    onClick = onClick,
                ).semantics(mergeDescendants = true) { contentDescription = description }
                .padding(start = Spacing.md + AccentBarWidth, top = Spacing.md, end = Spacing.md, bottom = Spacing.md),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(
                text = topic.title,
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(Spacing.sm))
            PriceText(topic.price)
        }
        Spacer(Modifier.height(Spacing.md))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(url = topic.expertImageUrl, contentDescription = null, size = TopicAvatarSize)
            Spacer(Modifier.width(Spacing.sm))
            Text(
                text = expertName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.extras.onSurfaceMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Preview(name = "ExploreFeaturedTopicsSection")
@Composable
private fun ExploreFeaturedTopicsSectionPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            ExploreFeaturedTopicsSection(
                state =
                    SectionState.Loaded(
                        listOf(
                            FeaturedTopicUi("t1", "Intro to ML", PriceDisplay.Paid("$50.00"), "e1", "Ada", null),
                            FeaturedTopicUi("t2", "Career chat", PriceDisplay.Free, "e2", null, null),
                        ),
                    ),
                onTopicClick = {},
                onRetry = {},
                modifier = Modifier.padding(Spacing.md),
            )
        }
    }
}
