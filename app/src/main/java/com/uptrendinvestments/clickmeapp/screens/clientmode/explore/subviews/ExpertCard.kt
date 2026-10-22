package com.uptrendinvestments.clickmeapp.screens.clientmode.explore.subviews

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.ExpertTagChip
import com.uptrendinvestments.clickmeapp.components.RatingStars
import com.uptrendinvestments.clickmeapp.components.avatar.Avatar
import com.uptrendinvestments.clickmeapp.components.buttonstyles.pressScale
import com.uptrendinvestments.clickmeapp.screens.clientmode.explore.ExpertCardUi
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.extras

private const val MAX_TAGS = 3
private val ExpertAvatarSize = 96.dp

/**
 * Recommended Experts card (D-12): avatar, name, rating, title and up to three tags. The whole card is one button
 * for TalkBack with a merged description. No fixed height, so it grows with the font scale.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExpertCard(
    expert: ExpertCardUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.medium
    val interactionSource = remember { MutableInteractionSource() }
    val name = expert.name ?: stringResource(R.string.explore_expert_name_fallback)
    val title = expert.title?.takeIf { it.isNotBlank() }
    val visibleTags = expert.tags.filter { it.isNotBlank() }.take(MAX_TAGS)
    val hasReviews = expert.reviewCount > 0
    val ratingText = stringResource(R.string.rating_value, expert.rating)
    val ratingDescription =
        if (hasReviews) {
            pluralStringResource(R.plurals.rating_semantics, expert.reviewCount, ratingText, expert.reviewCount)
        } else {
            stringResource(R.string.rating_no_reviews)
        }
    val tagsDescription =
        if (visibleTags.isEmpty()) {
            null
        } else {
            stringResource(R.string.expert_card_tags_semantics, visibleTags.joinToString(", "))
        }
    val description = listOfNotNull(name, title, ratingDescription, tagsDescription).joinToString(". ")

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
                .padding(Spacing.md),
    ) {
        Avatar(url = expert.imageUrl, contentDescription = null, size = ExpertAvatarSize)
        Spacer(Modifier.width(Spacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(Spacing.xs))
            if (hasReviews) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RatingStars(rating = expert.rating, size = 16.dp)
                    Spacer(Modifier.width(Spacing.xs))
                    Text(
                        text = ratingText,
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onSurface,
                    )
                    Spacer(Modifier.width(Spacing.xs))
                    Text(
                        text =
                            pluralStringResource(
                                R.plurals.rating_card_review_count,
                                expert.reviewCount,
                                expert.reviewCount,
                            ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.extras.onSurfaceMuted,
                    )
                }
            } else {
                Text(
                    text = stringResource(R.string.rating_no_reviews),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.extras.onSurfaceMuted,
                )
            }
            if (title != null) {
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.extras.onSurfaceMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (visibleTags.isNotEmpty()) {
                Spacer(Modifier.height(Spacing.sm))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    visibleTags.forEach { tag -> ExpertTagChip(label = tag) }
                }
            }
        }
    }
}

@Preview(name = "ExpertCard")
@Composable
private fun ExpertCardPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            ExpertCard(
                expert =
                    ExpertCardUi(
                        expertId = "e1",
                        name = "Ada Lovelace",
                        title = "Engineer",
                        imageUrl = null,
                        rating = 4.5,
                        reviewCount = 12,
                        tags = listOf("AI", "Math", "Stats"),
                    ),
                onClick = {},
                modifier = Modifier.padding(Spacing.md),
            )
        }
    }
}
