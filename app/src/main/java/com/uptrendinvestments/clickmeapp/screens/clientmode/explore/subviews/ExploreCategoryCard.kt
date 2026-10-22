package com.uptrendinvestments.clickmeapp.screens.clientmode.explore.subviews

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.buttonstyles.pressScale
import com.uptrendinvestments.clickmeapp.constants.CategoryIconMap
import com.uptrendinvestments.clickmeapp.screens.clientmode.explore.CategoryChipUi
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing

private val CardWidth = 112.dp
private val CardMinHeight = 104.dp
private val BadgeSize = 40.dp
private val CategoryIconSize = 24.dp
private const val COLOR_ANIMATION_MS = 200

/**
 * Trending category card (D-09, D-11): icon badge plus name. Selected cards get the secondaryContainer fill and a 2 dp
 * primary border; announced as a checkbox so TalkBack reads the toggle state. Never a fixed height (font scale).
 */
@Composable
fun ExploreCategoryCard(
    category: CategoryChipUi,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val animation = tween<androidx.compose.ui.graphics.Color>(durationMillis = COLOR_ANIMATION_MS)
    val container by animateColorAsState(
        targetValue = if (selected) colors.secondaryContainer else colors.surfaceContainerLow,
        animationSpec = animation,
        label = "categoryContainer",
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) colors.primary else colors.outlineVariant,
        animationSpec = animation,
        label = "categoryBorder",
    )
    val shape = MaterialTheme.shapes.medium
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier =
            modifier
                .width(CardWidth)
                .heightIn(min = CardMinHeight)
                .pressScale(interactionSource)
                .clip(shape)
                .background(container)
                .border(width = if (selected) 2.dp else 1.dp, color = borderColor, shape = shape)
                .toggleable(
                    value = selected,
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    role = Role.Checkbox,
                    onValueChange = { onClick() },
                ).padding(horizontal = Spacing.sm, vertical = Spacing.md),
    ) {
        Box(
            modifier =
                Modifier
                    .size(BadgeSize)
                    .clip(MaterialTheme.shapes.small)
                    .background(colors.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = CategoryIconMap.icon(category.slug),
                contentDescription = null,
                modifier = Modifier.size(CategoryIconSize),
                tint = colors.primary,
            )
        }
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = category.name ?: stringResource(R.string.explore_category_name_fallback),
            style = MaterialTheme.typography.labelMedium,
            color = colors.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Preview(name = "ExploreCategoryCard")
@Composable
private fun ExploreCategoryCardPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(Spacing.md)) {
                ExploreCategoryCard(CategoryChipUi("ai", "AI"), selected = true, onClick = {})
                Spacer(Modifier.height(Spacing.sm))
                ExploreCategoryCard(CategoryChipUi("design", "Design"), selected = false, onClick = {})
            }
        }
    }
}
