package com.uptrendinvestments.clickmeapp.screens.settings.help.subviews

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.ProfileMenuRow
import com.uptrendinvestments.clickmeapp.model.responses.FaqArticlePreview
import com.uptrendinvestments.clickmeapp.model.responses.FaqCategory
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Motion
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing

/** Test tag on every FAQ category card. */
const val HELP_FAQ_CARD_TAG = "help_faq_card"

/** Test tag on the "Email Us" row. */
const val HELP_EMAIL_US_TAG = "help_email_us"

private val HeaderMinHeight = 56.dp
private val ChevronSize = 24.dp
private const val EXPANDED_ROTATION = 180f

/**
 * One FAQ category (port of iOS `HelpFAQCard`): a header that expands in place to the category's questions and
 * snippets. The header is one button that announces "Expanded" or "Collapsed"; a missing snippet is left out.
 * Content is plain text: the server's copy is never parsed as markup.
 */
@Composable
fun HelpFaqCard(
    category: FaqCategory,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state =
        stringResource(if (expanded) R.string.help_expanded else R.string.help_collapsed)
    val rotation by animateFloatAsState(
        targetValue = if (expanded) EXPANDED_ROTATION else 0f,
        animationSpec = tween(Motion.STRENGTH_METER_VISIBILITY_MS),
        label = "helpChevron",
    )
    Surface(
        modifier = modifier.fillMaxWidth().testTag(HELP_FAQ_CARD_TAG),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = HeaderMinHeight)
                        .clickable(role = Role.Button, onClick = onToggle)
                        .semantics(mergeDescendants = true) { stateDescription = state }
                        .padding(Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = category.title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Icon(
                    imageVector = Icons.Outlined.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(ChevronSize).rotate(rotation),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            AnimatedVisibility(
                visible = expanded,
                enter =
                    fadeIn(tween(Motion.STRENGTH_METER_VISIBILITY_MS)) +
                        expandVertically(tween(Motion.STRENGTH_METER_VISIBILITY_MS)),
                exit =
                    fadeOut(tween(Motion.STRENGTH_METER_VISIBILITY_MS)) +
                        shrinkVertically(tween(Motion.STRENGTH_METER_VISIBILITY_MS)),
            ) {
                Column {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(Spacing.md),
                        verticalArrangement = Arrangement.spacedBy(Spacing.md),
                    ) {
                        category.featuredArticles.forEach { article ->
                            Column {
                                Text(
                                    text = article.question,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                article.snippet?.let { snippet ->
                                    Text(
                                        text = snippet,
                                        modifier = Modifier.padding(top = Spacing.xs),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** The "Email Us" row (port of iOS `HelpContactRow`): a [ProfileMenuRow] with a mail glyph. */
@Composable
fun HelpContactRow(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ProfileMenuRow(
        title = stringResource(R.string.help_email_us),
        icon = Icons.Outlined.MailOutline,
        onClick = onClick,
        modifier = modifier.testTag(HELP_EMAIL_US_TAG),
    )
}

@Preview(name = "HelpFaqCard expanded")
@Composable
private fun HelpFaqCardPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                HelpFaqCard(
                    category =
                        FaqCategory(
                            id = "c1",
                            title = "Payments",
                            featuredArticles =
                                listOf(
                                    FaqArticlePreview(
                                        id = "a1",
                                        question = "How do I get paid?",
                                        snippet = "Payouts arrive after each completed session.",
                                    ),
                                ),
                        ),
                    expanded = true,
                    onToggle = {},
                )
                HelpContactRow(onClick = {})
            }
        }
    }
}
