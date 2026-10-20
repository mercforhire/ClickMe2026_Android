@file:Suppress("ktlint:standard:filename")

package com.uptrendinvestments.clickmeapp.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * The only place in the app where hex color literals may appear (enforced by
 * scripts/check-design-rules.sh). Values come from 01-UI-SPEC.md.
 */
object ClickMePalette {
    val surface = Color(0xFF131313)
    val surfaceContainerLowest = Color(0xFF0E0E0E)
    val surfaceContainerLow = Color(0xFF1C1B1B)
    val surfaceContainer = Color(0xFF201F1F)
    val surfaceContainerHigh = Color(0xFF2A2A2A)
    val surfaceContainerHighest = Color(0xFF353534)
    val surfaceBright = Color(0xFF3A3939)

    val onSurface = Color(0xFFE5E2E1)
    val onSurfaceVariant = Color(0xFFBACBBC)
    val outline = Color(0xFF859587)
    val outlineVariant = Color(0xFF3C4A3F)

    val primary = Color(0xFF44F697)
    val onPrimary = Color(0xFF00391D)
    val primaryContainer = Color(0xFF00D97E)
    val onPrimaryContainer = Color(0xFF005930)
    val inversePrimary = Color(0xFF006D3A)

    val secondary = Color(0xFFB7CCBA)
    val onSecondary = Color(0xFF233528)
    val secondaryContainer = Color(0xFF1E3A2B)

    val tertiary = Color(0xFFFFB833)
    val onTertiary = Color(0xFF3F2A00)
    val tertiaryContainer = Color(0xFF4A3300)
    val onTertiaryContainer = Color(0xFFFFDEA0)

    val error = Color(0xFFFFB4AB)
    val onError = Color(0xFF690005)
    val errorContainer = Color(0xFF93000A)
    val onErrorContainer = Color(0xFFFFDAD6)

    val inverseSurface = Color(0xFFE5E2E1)
    val inverseOnSurface = Color(0xFF313030)
    val scrim = Color(0xFF000000)

    val destructive = Color(0xFFFF4D4D)
    val onDestructive = Color(0xFF1A0000)
    val onSurfaceMuted = Color(0xFF999999)
    val onSurfaceFaint = Color(0xFF6E6E6E)

    val overlayWhite05 = Color.White.copy(alpha = 0.05f)
    val overlayWhite06 = Color.White.copy(alpha = 0.06f)
    val overlayWhite10 = Color.White.copy(alpha = 0.10f)
    val overlayWhite15 = Color.White.copy(alpha = 0.15f)
    val overlayWhite20 = Color.White.copy(alpha = 0.20f)
    val overlayWhite22 = Color.White.copy(alpha = 0.22f)
    val overlayWhite60 = Color.White.copy(alpha = 0.60f)
    val overlayWhite80 = Color.White.copy(alpha = 0.80f)
}
