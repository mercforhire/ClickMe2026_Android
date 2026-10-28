package com.uptrendinvestments.clickmeapp.screens.clientmode.writereview.subviews

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R

/** Size of one star touch target. */
private val StarTarget = 48.dp

/** Size of the star glyph inside its target. */
private val StarGlyph = 40.dp

private const val STAR_COUNT = 5

/**
 * Five-star rating input (iOS `ReviewStarRating`): stars up to [rating] are filled in tertiary, the rest outlined.
 * Tapping star n calls [onSelect] with n. The row is a radio group; each star is labelled "{n} stars".
 */
@Composable
fun ReviewStarInput(
    rating: Int,
    enabled: Boolean,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.selectableGroup(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (n in 1..STAR_COUNT) {
            val filled = n <= rating
            val label = pluralStringResource(R.plurals.review_star_label, n, n)
            Box(
                modifier =
                    Modifier
                        .size(StarTarget)
                        .semantics { contentDescription = label }
                        .selectable(
                            selected = n == rating,
                            enabled = enabled,
                            role = Role.RadioButton,
                            onClick = { onSelect(n) },
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (filled) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = null,
                    modifier = Modifier.size(StarGlyph),
                    tint =
                        if (filled) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                )
            }
        }
    }
}
