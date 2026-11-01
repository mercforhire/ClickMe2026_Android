package com.uptrendinvestments.clickmeapp.screens.clientmode.search.subviews

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.ExpertTagChip
import com.uptrendinvestments.clickmeapp.components.avatar.Avatar
import com.uptrendinvestments.clickmeapp.components.buttonstyles.pressScale
import com.uptrendinvestments.clickmeapp.screens.clientmode.search.SearchExpertUi
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMePalette
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme

/** Test tag on every Search result card. */
const val SEARCH_EXPERT_CARD_TAG = "search_expert_card"

private const val MAX_TAGS = 3
private const val BIO_ALPHA = 0.75f
private const val BIO_MAX_LINES = 2
private val SearchAvatarSize = 80.dp
private val CardPadding = 16.dp
private val CardCorner = 16.dp
private val ColumnSpacing = 6.dp

/**
 * Port of iOS `SearchExpertCard`: 80 dp avatar, bold name with the star rating on the right, the headline, the bio
 * only when it is not blank, and at most three tag pills. The whole card is one button for TalkBack with a merged
 * description. Server text renders as plain [Text] only. No fixed height, so it grows with the font scale.
 *
 * The bio slot is intentional parity scaffolding: `/experts/search` rows carry no bio today (D-05), so
 * [SearchExpertUi.bio] is always empty and the bio line never renders. It mirrors the iOS card, which shows the bio
 * when non-empty, so it is kept rather than deleted. When a backend field appears, map it in `toSearchExpertUi` and add
 * the bio to the merged TalkBack description below.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchExpertCard(
    expert: SearchExpertUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(CardCorner)
    val interactionSource = remember { MutableInteractionSource() }
    val name = expert.name ?: stringResource(R.string.explore_expert_name_fallback)
    val headline = expert.headline.takeIf { it.isNotBlank() }
    val bio = expert.bio.takeIf { it.isNotBlank() }
    val visibleTags = expert.tags.filter { it.isNotBlank() }.take(MAX_TAGS)
    val ratingText = stringResource(R.string.rating_value, expert.rating)
    val ratingDescription = stringResource(R.string.search_card_rating_semantics, ratingText)
    val tagsDescription =
        if (visibleTags.isEmpty()) {
            null
        } else {
            stringResource(R.string.expert_card_tags_semantics, visibleTags.joinToString(", "))
        }
    val description = listOfNotNull(name, headline, ratingDescription, tagsDescription).joinToString(". ")

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .pressScale(interactionSource)
                .clip(shape)
                .background(colors.surfaceContainerLow)
                .border(1.dp, colors.outlineVariant, shape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    role = Role.Button,
                    onClick = onClick,
                ).semantics(mergeDescendants = true) { contentDescription = description }
                .testTag(SEARCH_EXPERT_CARD_TAG)
                .padding(CardPadding),
        verticalAlignment = Alignment.Top,
    ) {
        Avatar(
            url = expert.avatarUrl,
            contentDescription = null,
            size = SearchAvatarSize,
            fallbackColor = ClickMePalette.avatarFallback,
        )
        Spacer(Modifier.width(CardPadding))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(ColumnSpacing),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = name,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface,
                )
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = colors.tertiary,
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = ratingText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface,
                )
            }
            if (headline != null) {
                Text(
                    text = headline,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
            if (bio != null) {
                Text(
                    text = bio,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurface.copy(alpha = BIO_ALPHA),
                    maxLines = BIO_MAX_LINES,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (visibleTags.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.padding(top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(ColumnSpacing),
                    verticalArrangement = Arrangement.spacedBy(ColumnSpacing),
                ) {
                    visibleTags.forEach { tag -> ExpertTagChip(label = tag) }
                }
            }
        }
    }
}

@Preview(name = "SearchExpertCard")
@Composable
private fun SearchExpertCardPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            SearchExpertCard(
                expert =
                    SearchExpertUi(
                        expertId = "e1",
                        name = "Ada Lovelace",
                        headline = "Engineer",
                        avatarUrl = null,
                        rating = 4.5,
                        tags = listOf("AI", "Math", "Stats"),
                    ),
                onClick = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
