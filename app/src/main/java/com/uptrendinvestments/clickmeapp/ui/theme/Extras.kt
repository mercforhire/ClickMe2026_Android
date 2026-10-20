package com.uptrendinvestments.clickmeapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Colors that Material 3 has no ColorScheme slot for. Read via [MaterialTheme.extras]. */
@Immutable
data class ClickMeExtras(
    val destructive: Color,
    val onDestructive: Color,
    val onSurfaceMuted: Color,
    val onSurfaceFaint: Color,
    val overlayWhite05: Color,
    val overlayWhite06: Color,
    val overlayWhite10: Color,
    val overlayWhite15: Color,
    val overlayWhite20: Color,
    val overlayWhite22: Color,
    val overlayWhite60: Color,
    val overlayWhite80: Color,
)

internal val ClickMeExtrasDark =
    ClickMeExtras(
        destructive = ClickMePalette.destructive,
        onDestructive = ClickMePalette.onDestructive,
        onSurfaceMuted = ClickMePalette.onSurfaceMuted,
        onSurfaceFaint = ClickMePalette.onSurfaceFaint,
        overlayWhite05 = ClickMePalette.overlayWhite05,
        overlayWhite06 = ClickMePalette.overlayWhite06,
        overlayWhite10 = ClickMePalette.overlayWhite10,
        overlayWhite15 = ClickMePalette.overlayWhite15,
        overlayWhite20 = ClickMePalette.overlayWhite20,
        overlayWhite22 = ClickMePalette.overlayWhite22,
        overlayWhite60 = ClickMePalette.overlayWhite60,
        overlayWhite80 = ClickMePalette.overlayWhite80,
    )

val LocalClickMeExtras = staticCompositionLocalOf { ClickMeExtrasDark }

val MaterialTheme.extras: ClickMeExtras
    @Composable
    @ReadOnlyComposable
    get() = LocalClickMeExtras.current
