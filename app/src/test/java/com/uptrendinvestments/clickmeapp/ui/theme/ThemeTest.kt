package com.uptrendinvestments.clickmeapp.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeTest {
    private val scheme = ClickMeDarkColorScheme

    @Test
    fun surfaceTiersMatchUiSpec() {
        assertEquals(0xFF131313.toInt(), scheme.surface.toArgb())
        assertEquals(scheme.surface, scheme.background)
        assertEquals(scheme.surface, scheme.surfaceDim)
        assertEquals(0xFF0E0E0E.toInt(), scheme.surfaceContainerLowest.toArgb())
        assertEquals(0xFF1C1B1B.toInt(), scheme.surfaceContainerLow.toArgb())
        assertEquals(0xFF201F1F.toInt(), scheme.surfaceContainer.toArgb())
        assertEquals(0xFF2A2A2A.toInt(), scheme.surfaceContainerHigh.toArgb())
        assertEquals(0xFF353534.toInt(), scheme.surfaceContainerHighest.toArgb())
        assertEquals(scheme.surfaceContainerHighest, scheme.surfaceVariant)
        assertEquals(0xFF3A3939.toInt(), scheme.surfaceBright.toArgb())
    }

    @Test
    fun primaryFamilyMatchesUiSpec() {
        assertEquals(0xFF44F697.toInt(), scheme.primary.toArgb())
        assertEquals(scheme.primary, scheme.surfaceTint)
        assertEquals(0xFF00391D.toInt(), scheme.onPrimary.toArgb())
        assertEquals(0xFF00D97E.toInt(), scheme.primaryContainer.toArgb())
        assertEquals(0xFF005930.toInt(), scheme.onPrimaryContainer.toArgb())
        assertEquals(0xFF006D3A.toInt(), scheme.inversePrimary.toArgb())
    }

    @Test
    fun onSurfaceAndOutlineMatchUiSpec() {
        assertEquals(0xFFE5E2E1.toInt(), scheme.onSurface.toArgb())
        assertEquals(scheme.onSurface, scheme.onBackground)
        assertEquals(0xFFBACBBC.toInt(), scheme.onSurfaceVariant.toArgb())
        assertEquals(0xFF859587.toInt(), scheme.outline.toArgb())
        assertEquals(0xFF3C4A3F.toInt(), scheme.outlineVariant.toArgb())
    }

    @Test
    fun secondaryTertiaryAndErrorMatchUiSpec() {
        assertEquals(0xFF1E3A2B.toInt(), scheme.secondaryContainer.toArgb())
        assertEquals(scheme.primary, scheme.onSecondaryContainer)
        assertEquals(0xFFFFB833.toInt(), scheme.tertiary.toArgb())
        assertEquals(0xFFFFB4AB.toInt(), scheme.error.toArgb())
        assertEquals(0xFF690005.toInt(), scheme.onError.toArgb())
        assertEquals(0xFF000000.toInt(), scheme.scrim.toArgb())
    }

    @Test
    fun extrasMatchUiSpec() {
        val extras = ClickMeExtrasDark
        assertEquals(0xFFFF4D4D.toInt(), extras.destructive.toArgb())
        assertEquals(0xFF1A0000.toInt(), extras.onDestructive.toArgb())
        assertEquals(0xFF999999.toInt(), extras.onSurfaceMuted.toArgb())
        assertEquals(0xFF6E6E6E.toInt(), extras.onSurfaceFaint.toArgb())
        assertEquals(Color.White.copy(alpha = 0.10f), extras.overlayWhite10)
        assertEquals(Color.White.copy(alpha = 0.05f), extras.overlayWhite05)
        assertEquals(Color.White.copy(alpha = 0.80f), extras.overlayWhite80)
    }

    @Test
    fun shapesMatchUiSpec() {
        assertEquals(RoundedCornerShape(8.dp), ClickMeShapes.extraSmall)
        assertEquals(RoundedCornerShape(12.dp), ClickMeShapes.small)
        assertEquals(RoundedCornerShape(16.dp), ClickMeShapes.medium)
        assertEquals(RoundedCornerShape(20.dp), ClickMeShapes.large)
        assertEquals(RoundedCornerShape(28.dp), ClickMeShapes.extraLarge)
    }

    @Test
    fun spacingScaleMatchesUiSpec() {
        assertEquals(4.dp, Spacing.xs)
        assertEquals(8.dp, Spacing.sm)
        assertEquals(16.dp, Spacing.md)
        assertEquals(24.dp, Spacing.lg)
        assertEquals(32.dp, Spacing.xl)
        assertEquals(48.dp, Spacing.xxl)
        assertEquals(64.dp, Spacing.xxxl)
    }

    @Test
    fun motionConstantsMatchUiSpec() {
        assertEquals(120, Motion.PRESS_DURATION_MS)
        assertEquals(0.96f, Motion.PRESSED_SCALE, 0f)
        assertEquals(300, Motion.PUSH_POP_DURATION_MS)
    }
}
