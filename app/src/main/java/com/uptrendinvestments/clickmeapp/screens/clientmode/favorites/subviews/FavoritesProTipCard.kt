package com.uptrendinvestments.clickmeapp.screens.clientmode.favorites.subviews

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing

private val TileSize = 44.dp
private val TipIconSize = 24.dp

/**
 * Port of iOS `FavoritesProTipCard`: a lightbulb tile and the tip text. Shown in the empty state only (D-08), and
 * the tip names the heart, not the star the iOS copy names (D-07). One merged TalkBack node; not interactive.
 */
@Composable
fun FavoritesProTipCard(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.fillMaxWidth().semantics(mergeDescendants = true) { },
        shape = MaterialTheme.shapes.medium,
        color = colors.surfaceContainerLow,
        border = BorderStroke(1.dp, colors.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(TileSize),
                shape = MaterialTheme.shapes.extraSmall,
                color = colors.surfaceContainerHigh,
                border = BorderStroke(1.dp, colors.outlineVariant),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Lightbulb,
                        contentDescription = null,
                        modifier = Modifier.size(TipIconSize),
                        tint = colors.primary,
                    )
                }
            }
            Spacer(Modifier.size(Spacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.favorites_pro_tip_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurface,
                )
                Text(
                    text = stringResource(R.string.favorites_pro_tip_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
        }
    }
}

@Preview(name = "FavoritesProTipCard")
@Composable
private fun FavoritesProTipCardPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            FavoritesProTipCard(modifier = Modifier.padding(Spacing.md))
        }
    }
}
