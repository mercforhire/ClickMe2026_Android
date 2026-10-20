package com.uptrendinvestments.clickmeapp.screens.debug

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeExtras
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.extras

private val SwatchSize = 40.dp

/** Debug caption describing a typography slot, e.g. "titleLarge 20sp/600". */
private data class TypeSlot(
    val name: String,
    val caption: String,
    val style: @Composable () -> TextStyle,
)

private val typeSlots =
    listOf(
        TypeSlot("displayLarge", "displayLarge 28sp/600") { MaterialTheme.typography.displayLarge },
        TypeSlot("displayMedium", "displayMedium 28sp/600") { MaterialTheme.typography.displayMedium },
        TypeSlot("displaySmall", "displaySmall 28sp/600") { MaterialTheme.typography.displaySmall },
        TypeSlot("headlineLarge", "headlineLarge 28sp/600") { MaterialTheme.typography.headlineLarge },
        TypeSlot("headlineMedium", "headlineMedium 20sp/600") { MaterialTheme.typography.headlineMedium },
        TypeSlot("headlineSmall", "headlineSmall 20sp/600") { MaterialTheme.typography.headlineSmall },
        TypeSlot("titleLarge", "titleLarge 20sp/600") { MaterialTheme.typography.titleLarge },
        TypeSlot("titleMedium", "titleMedium 16sp/600") { MaterialTheme.typography.titleMedium },
        TypeSlot("titleSmall", "titleSmall 16sp/600") { MaterialTheme.typography.titleSmall },
        TypeSlot("bodyLarge", "bodyLarge 16sp/400") { MaterialTheme.typography.bodyLarge },
        TypeSlot("bodyMedium", "bodyMedium 16sp/400") { MaterialTheme.typography.bodyMedium },
        TypeSlot("bodySmall", "bodySmall 12sp/400") { MaterialTheme.typography.bodySmall },
        TypeSlot("labelLarge", "labelLarge 16sp/600") { MaterialTheme.typography.labelLarge },
        TypeSlot("labelMedium", "labelMedium 12sp/600") { MaterialTheme.typography.labelMedium },
        TypeSlot("labelSmall", "labelSmall 12sp/400") { MaterialTheme.typography.labelSmall },
    )

private data class ColorToken(
    val name: String,
    val color: (ColorScheme, ClickMeExtras) -> Color,
)

private val colorTokens =
    listOf(
        ColorToken("surface") { s, _ -> s.surface },
        ColorToken("surfaceContainerLowest") { s, _ -> s.surfaceContainerLowest },
        ColorToken("surfaceContainerLow") { s, _ -> s.surfaceContainerLow },
        ColorToken("surfaceContainer") { s, _ -> s.surfaceContainer },
        ColorToken("surfaceContainerHigh") { s, _ -> s.surfaceContainerHigh },
        ColorToken("surfaceContainerHighest") { s, _ -> s.surfaceContainerHighest },
        ColorToken("surfaceBright") { s, _ -> s.surfaceBright },
        ColorToken("onSurface") { s, _ -> s.onSurface },
        ColorToken("onSurfaceVariant") { s, _ -> s.onSurfaceVariant },
        ColorToken("outline") { s, _ -> s.outline },
        ColorToken("outlineVariant") { s, _ -> s.outlineVariant },
        ColorToken("primary") { s, _ -> s.primary },
        ColorToken("onPrimary") { s, _ -> s.onPrimary },
        ColorToken("primaryContainer") { s, _ -> s.primaryContainer },
        ColorToken("onPrimaryContainer") { s, _ -> s.onPrimaryContainer },
        ColorToken("inversePrimary") { s, _ -> s.inversePrimary },
        ColorToken("secondary") { s, _ -> s.secondary },
        ColorToken("onSecondary") { s, _ -> s.onSecondary },
        ColorToken("secondaryContainer") { s, _ -> s.secondaryContainer },
        ColorToken("onSecondaryContainer") { s, _ -> s.onSecondaryContainer },
        ColorToken("tertiary") { s, _ -> s.tertiary },
        ColorToken("onTertiary") { s, _ -> s.onTertiary },
        ColorToken("tertiaryContainer") { s, _ -> s.tertiaryContainer },
        ColorToken("onTertiaryContainer") { s, _ -> s.onTertiaryContainer },
        ColorToken("error") { s, _ -> s.error },
        ColorToken("onError") { s, _ -> s.onError },
        ColorToken("errorContainer") { s, _ -> s.errorContainer },
        ColorToken("onErrorContainer") { s, _ -> s.onErrorContainer },
        ColorToken("inverseSurface") { s, _ -> s.inverseSurface },
        ColorToken("inverseOnSurface") { s, _ -> s.inverseOnSurface },
        ColorToken("scrim") { s, _ -> s.scrim },
        ColorToken("extras.destructive") { _, e -> e.destructive },
        ColorToken("extras.onDestructive") { _, e -> e.onDestructive },
        ColorToken("extras.onSurfaceMuted") { _, e -> e.onSurfaceMuted },
        ColorToken("extras.onSurfaceFaint") { _, e -> e.onSurfaceFaint },
        ColorToken("extras.overlayWhite05") { _, e -> e.overlayWhite05 },
        ColorToken("extras.overlayWhite06") { _, e -> e.overlayWhite06 },
        ColorToken("extras.overlayWhite10") { _, e -> e.overlayWhite10 },
        ColorToken("extras.overlayWhite15") { _, e -> e.overlayWhite15 },
        ColorToken("extras.overlayWhite20") { _, e -> e.overlayWhite20 },
        ColorToken("extras.overlayWhite22") { _, e -> e.overlayWhite22 },
        ColorToken("extras.overlayWhite60") { _, e -> e.overlayWhite60 },
        ColorToken("extras.overlayWhite80") { _, e -> e.overlayWhite80 },
    )

/** Debug design-system gallery section: every typography slot and every color token. */
@Composable
fun GalleryThemeSection(modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Text(
                text = stringResource(R.string.gallery_heading_typography),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            typeSlots.forEach { slot ->
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Text(
                        text = stringResource(R.string.gallery_type_sample),
                        style = slot.style(),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = slot.caption,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.extras.onSurfaceMuted,
                    )
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text(
                text = stringResource(R.string.gallery_heading_colors),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            val scheme = MaterialTheme.colorScheme
            val extras = MaterialTheme.extras
            colorTokens.forEach { token ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(SwatchSize)
                                .background(token.color(scheme, extras), MaterialTheme.shapes.small)
                                .border(
                                    BorderStroke(1.dp, scheme.outlineVariant),
                                    MaterialTheme.shapes.small,
                                ),
                    )
                    Text(
                        text = token.name,
                        style = MaterialTheme.typography.labelMedium,
                        color = extras.onSurfaceMuted,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun GalleryThemeSectionPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            GalleryThemeSection(modifier = Modifier.padding(Spacing.md))
        }
    }
}
