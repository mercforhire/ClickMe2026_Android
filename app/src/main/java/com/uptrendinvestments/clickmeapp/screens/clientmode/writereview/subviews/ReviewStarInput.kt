package com.uptrendinvestments.clickmeapp.screens.clientmode.writereview.subviews

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.buttonstyles.pressScale
import com.uptrendinvestments.clickmeapp.ui.theme.Motion

/** Size of one star touch target. */
private val StarTarget = 48.dp

/** Size of the star glyph inside its target. */
private val StarGlyph = 40.dp

private const val STAR_COUNT = 5

/**
 * Five-star rating (iOS `ReviewStarRating`): stars up to [rating] are filled in tertiary, the rest outlined.
 *
 * Editable (`readOnly = false`): a radio group, each star labelled "{n} stars"; tapping star n calls [onSelect].
 * Read-only: display only, exposed as the single node "Rated {n} out of 5" with no click handling.
 */
@Composable
fun ReviewStarInput(
    rating: Int,
    readOnly: Boolean,
    enabled: Boolean,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val groupModifier =
        if (readOnly) {
            val label = stringResource(R.string.review_rated_out_of_five, rating)
            modifier.clearAndSetSemantics { contentDescription = label }
        } else {
            modifier.selectableGroup()
        }
    Row(
        modifier = groupModifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (n in 1..STAR_COUNT) {
            Star(
                n = n,
                rating = rating,
                readOnly = readOnly,
                enabled = enabled,
                onSelect = onSelect,
            )
        }
    }
}

@Composable
private fun Star(
    n: Int,
    rating: Int,
    readOnly: Boolean,
    enabled: Boolean,
    onSelect: (Int) -> Unit,
) {
    val filled = n <= rating
    val tint by animateColorAsState(
        targetValue =
            if (filled) {
                MaterialTheme.colorScheme.tertiary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        animationSpec = tween(Motion.CONTROL_STATE_DURATION_MS),
        label = "reviewStarTint",
    )
    val glyph: @Composable (Modifier) -> Unit = { glyphModifier ->
        Icon(
            imageVector = if (filled) Icons.Filled.Star else Icons.Outlined.StarBorder,
            contentDescription = null,
            modifier = glyphModifier.size(StarGlyph),
            tint = tint,
        )
    }
    if (readOnly) {
        Box(Modifier.size(StarTarget), contentAlignment = Alignment.Center) { glyph(Modifier) }
        return
    }
    val label = pluralStringResource(R.plurals.review_star_label, n, n)
    val source = remember { MutableInteractionSource() }
    Box(
        modifier =
            Modifier
                .size(StarTarget)
                .semantics { contentDescription = label }
                .selectable(
                    selected = n == rating,
                    enabled = enabled,
                    interactionSource = source,
                    indication = LocalIndication.current,
                    role = Role.RadioButton,
                    onClick = { onSelect(n) },
                ),
        contentAlignment = Alignment.Center,
    ) {
        glyph(if (n == rating) Modifier.pressScale(source) else Modifier)
    }
}
