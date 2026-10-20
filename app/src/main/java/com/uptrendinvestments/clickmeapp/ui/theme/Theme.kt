package com.uptrendinvestments.clickmeapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

internal val ClickMeDarkColorScheme =
    darkColorScheme(
        primary = ClickMePalette.primary,
        onPrimary = ClickMePalette.onPrimary,
        primaryContainer = ClickMePalette.primaryContainer,
        onPrimaryContainer = ClickMePalette.onPrimaryContainer,
        inversePrimary = ClickMePalette.inversePrimary,
        secondary = ClickMePalette.secondary,
        onSecondary = ClickMePalette.onSecondary,
        secondaryContainer = ClickMePalette.secondaryContainer,
        onSecondaryContainer = ClickMePalette.primary,
        tertiary = ClickMePalette.tertiary,
        onTertiary = ClickMePalette.onTertiary,
        tertiaryContainer = ClickMePalette.tertiaryContainer,
        onTertiaryContainer = ClickMePalette.onTertiaryContainer,
        background = ClickMePalette.surface,
        onBackground = ClickMePalette.onSurface,
        surface = ClickMePalette.surface,
        onSurface = ClickMePalette.onSurface,
        surfaceVariant = ClickMePalette.surfaceContainerHighest,
        onSurfaceVariant = ClickMePalette.onSurfaceVariant,
        surfaceTint = ClickMePalette.primary,
        inverseSurface = ClickMePalette.inverseSurface,
        inverseOnSurface = ClickMePalette.inverseOnSurface,
        error = ClickMePalette.error,
        onError = ClickMePalette.onError,
        errorContainer = ClickMePalette.errorContainer,
        onErrorContainer = ClickMePalette.onErrorContainer,
        outline = ClickMePalette.outline,
        outlineVariant = ClickMePalette.outlineVariant,
        scrim = ClickMePalette.scrim,
        surfaceBright = ClickMePalette.surfaceBright,
        surfaceDim = ClickMePalette.surface,
        surfaceContainer = ClickMePalette.surfaceContainer,
        surfaceContainerHigh = ClickMePalette.surfaceContainerHigh,
        surfaceContainerHighest = ClickMePalette.surfaceContainerHighest,
        surfaceContainerLow = ClickMePalette.surfaceContainerLow,
        surfaceContainerLowest = ClickMePalette.surfaceContainerLowest,
    )

/** Dark-only app theme: no light scheme, no dynamic color, no system-dark branch. */
@Composable
fun ClickMeTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalClickMeExtras provides ClickMeExtrasDark) {
        MaterialTheme(
            colorScheme = ClickMeDarkColorScheme,
            typography = ClickMeTypography,
            shapes = ClickMeShapes,
            content = content,
        )
    }
}
