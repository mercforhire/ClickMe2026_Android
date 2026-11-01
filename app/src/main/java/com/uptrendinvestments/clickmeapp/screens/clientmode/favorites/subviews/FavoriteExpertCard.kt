package com.uptrendinvestments.clickmeapp.screens.clientmode.favorites.subviews

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.ExpertTagChip
import com.uptrendinvestments.clickmeapp.components.buttonstyles.pressScale
import com.uptrendinvestments.clickmeapp.screens.clientmode.favorites.FavoriteExpertUi
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing

/** Test tag on the tappable (open profile) area of every Saved Experts card. */
const val FAVORITE_EXPERT_CARD_TAG = "favorite_expert_card"

private const val NAME_MAX_LINES = 2
private const val HEADLINE_MAX_LINES = 2

/** The heart's 48 dp target plus its 4 dp inset, less the card padding, kept clear of the name column. */
private val HeartReserve = 36.dp
private val HeartInset = Spacing.xs
private val HeartIconSize = 24.dp

/** The heart is a full 48 dp target (UI-SPEC deviation from the iOS 24 dp icon). */
private val HeartTarget = 48.dp

/**
 * Port of iOS `FavoriteExpertCard`: 72 x 80 photo, name, headline in the primary color, a trailing filled heart and
 * the expertise tags wrapped below. There is no bio line (the endpoint carries none). No fixed height, so it grows
 * with the font scale.
 *
 * Two TalkBack stops per card (UI-SPEC): the card area is one merged button that opens the profile, and the heart is
 * its own button that asks to remove the expert, so the heart sits beside the card area rather than inside it. Server
 * text renders as plain [Text] only.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FavoriteExpertCard(
    expert: FavoriteExpertUi,
    onOpen: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    val name = expert.name ?: stringResource(R.string.explore_expert_name_fallback)
    val headline = expert.headline.takeIf { it.isNotBlank() }
    val tagsDescription =
        expert.tags
            .takeIf { it.isNotEmpty() }
            ?.let { stringResource(R.string.favorites_card_expertise_a11y, it.joinToString(", ")) }
    val description = listOfNotNull(name, headline, tagsDescription).joinToString(". ")
    val openLabel = stringResource(R.string.favorites_card_open_profile)

    Surface(
        modifier = modifier.fillMaxWidth().pressScale(interactionSource),
        shape = MaterialTheme.shapes.medium,
        color = colors.surfaceContainerLow,
        border = BorderStroke(1.dp, colors.outlineVariant),
    ) {
        Box {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = interactionSource,
                            indication = LocalIndication.current,
                            onClickLabel = openLabel,
                            role = Role.Button,
                            onClick = onOpen,
                        ).semantics(mergeDescendants = true) { contentDescription = description }
                        .testTag(FAVORITE_EXPERT_CARD_TAG)
                        .padding(Spacing.md),
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    FavoriteExpertPhoto(url = expert.avatarUrl)
                    Spacer(Modifier.size(Spacing.md))
                    Column(modifier = Modifier.weight(1f).padding(end = HeartReserve)) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.titleLarge,
                            color = colors.onSurface,
                            maxLines = NAME_MAX_LINES,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (headline != null) {
                            Spacer(Modifier.height(Spacing.xs))
                            Text(
                                text = headline,
                                style = MaterialTheme.typography.labelLarge,
                                color = colors.primary,
                                maxLines = HEADLINE_MAX_LINES,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                if (expert.tags.isNotEmpty()) {
                    Spacer(Modifier.height(Spacing.md))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        expert.tags.forEach { tag -> ExpertTagChip(label = tag) }
                    }
                }
            }
            IconButton(
                onClick = onRemove,
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = HeartInset, end = HeartInset)
                        .size(HeartTarget),
            ) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = stringResource(R.string.favorites_card_remove_a11y, name),
                    modifier = Modifier.size(HeartIconSize),
                    tint = colors.error,
                )
            }
        }
    }
}

@Preview(name = "FavoriteExpertCard")
@Composable
private fun FavoriteExpertCardPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            FavoriteExpertCard(
                expert =
                    FavoriteExpertUi(
                        expertId = "e1",
                        name = "Ada Lovelace",
                        headline = "Engineer",
                        tags = listOf("AI", "Math"),
                        avatarUrl = null,
                    ),
                onOpen = {},
                onRemove = {},
                modifier = Modifier.padding(Spacing.md),
            )
        }
    }
}
