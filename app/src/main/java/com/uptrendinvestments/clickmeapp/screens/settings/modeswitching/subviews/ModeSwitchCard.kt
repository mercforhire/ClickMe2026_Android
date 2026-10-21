package com.uptrendinvestments.clickmeapp.screens.settings.modeswitching.subviews

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.buttonstyles.pressScale
import com.uptrendinvestments.clickmeapp.model.enums.AppMode
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.extras

private const val COLOR_ANIM_MS = 200
private val MinCardHeight = 120.dp
private val IllustrationSize = 88.dp
private val BadgeIconSize = 20.dp
private val IdleBorderWidth = 1.dp
private val ActiveBorderWidth = 2.dp

/**
 * One selectable mode card (iOS ModeSwitchCard) exposed as a radio button. The illustration is a
 * bundled drawable (D-19) so it never flashes and works offline. When [isActive], an "Active" badge
 * sits at the top trailing corner.
 */
@Composable
fun ModeSwitchCard(
    mode: AppMode,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val fill by animateColorAsState(
        targetValue = if (isActive) colors.secondaryContainer else colors.surfaceContainerLow,
        animationSpec = tween(COLOR_ANIM_MS),
        label = "modeCardFill",
    )
    val borderColor by animateColorAsState(
        targetValue = if (isActive) colors.primary else colors.outlineVariant,
        animationSpec = tween(COLOR_ANIM_MS),
        label = "modeCardBorder",
    )
    val title = stringResource(modeTitle(mode))
    val subtitle = stringResource(modeSubtitle(mode))
    val description = stringResource(modeDescription(mode))
    val a11yRes = if (isActive) R.string.mode_switch_card_a11y_active else R.string.mode_switch_card_a11y
    val label = stringResource(a11yRes, title, subtitle)
    val source = remember { MutableInteractionSource() }
    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .pressScale(source)
                .heightIn(min = MinCardHeight)
                .selectable(
                    selected = isActive,
                    interactionSource = source,
                    indication = null,
                    role = Role.RadioButton,
                    onClick = onClick,
                ).semantics { contentDescription = label },
        shape = MaterialTheme.shapes.medium,
        color = fill,
        border = BorderStroke(if (isActive) ActiveBorderWidth else IdleBorderWidth, borderColor),
    ) {
        Box {
            Row(modifier = Modifier.padding(Spacing.md)) {
                Image(
                    painter = painterResource(modeImage(mode)),
                    contentDescription = null,
                    modifier = Modifier.size(IllustrationSize).clip(MaterialTheme.shapes.small),
                    contentScale = ContentScale.Crop,
                )
                Spacer(Modifier.width(Spacing.md))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, style = MaterialTheme.typography.titleLarge, color = colors.onSurface)
                    Spacer(Modifier.size(Spacing.xs))
                    Text(text = subtitle, style = MaterialTheme.typography.bodyLarge, color = colors.onSurface)
                    Spacer(Modifier.size(Spacing.xs))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.extras.onSurfaceMuted,
                    )
                }
            }
            if (isActive) {
                ActiveBadge(Modifier.align(Alignment.TopEnd).padding(Spacing.md))
            }
        }
    }
}

@DrawableRes
private fun modeImage(mode: AppMode): Int =
    if (mode == AppMode.Client) R.drawable.mode_client else R.drawable.mode_expert

@StringRes
private fun modeTitle(mode: AppMode): Int =
    if (mode == AppMode.Client) R.string.mode_switch_client_title else R.string.mode_switch_expert_title

@StringRes
private fun modeSubtitle(mode: AppMode): Int =
    if (mode == AppMode.Client) R.string.mode_switch_client_subtitle else R.string.mode_switch_expert_subtitle

@StringRes
private fun modeDescription(mode: AppMode): Int =
    if (mode == AppMode.Client) R.string.mode_switch_client_description else R.string.mode_switch_expert_description

@Composable
private fun ActiveBadge(modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(BadgeIconSize),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.width(Spacing.xs))
        Text(
            text = stringResource(R.string.mode_switch_active),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
