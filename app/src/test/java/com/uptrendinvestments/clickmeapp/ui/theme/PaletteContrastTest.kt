package com.uptrendinvestments.clickmeapp.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

/**
 * Pins the UI-SPEC contrast table (Phase 15 POL-04) with the WCAG 2.x relative-luminance formula. The
 * "banned" pairs document why muted text must not sit on the two brightest surfaces.
 */
class PaletteContrastTest {
    private val aa = 4.5

    private fun channel(value: Float): Double {
        val v = value.toDouble()
        return if (v <= 0.03928) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
    }

    private fun luminance(c: Color): Double =
        0.2126 * channel(c.red) + 0.7152 * channel(c.green) + 0.0722 * channel(c.blue)

    private fun contrast(
        a: Color,
        b: Color,
    ): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
    }

    private val p = ClickMePalette

    private fun assertAa(
        fg: Color,
        bg: Color,
        name: String,
    ) = assertTrue("$name contrast ${contrast(fg, bg)} should be >= $aa", contrast(fg, bg) >= aa)

    private fun assertBelowAa(
        fg: Color,
        bg: Color,
        name: String,
    ) = assertTrue("$name contrast ${contrast(fg, bg)} should be < $aa", contrast(fg, bg) < aa)

    @Test
    fun onSurfaceMuted_passesOnSurface() = assertAa(p.onSurfaceMuted, p.surface, "muted on surface")

    @Test
    fun onSurfaceMuted_passesOnSurfaceContainer() =
        assertAa(p.onSurfaceMuted, p.surfaceContainer, "muted on surfaceContainer")

    @Test
    fun onSurfaceMuted_passesOnSurfaceContainerHigh() =
        assertAa(p.onSurfaceMuted, p.surfaceContainerHigh, "muted on surfaceContainerHigh")

    @Test
    fun onSurfaceMuted_failsOnSurfaceContainerHighest() =
        assertBelowAa(p.onSurfaceMuted, p.surfaceContainerHighest, "muted on surfaceContainerHighest")

    @Test
    fun onSurfaceMuted_failsOnSurfaceBright() =
        assertBelowAa(p.onSurfaceMuted, p.surfaceBright, "muted on surfaceBright")

    @Test
    fun destructive_passesOnSurface() = assertAa(p.destructive, p.surface, "destructive on surface")

    @Test
    fun destructive_passesOnSurfaceContainer() =
        assertAa(p.destructive, p.surfaceContainer, "destructive on surfaceContainer")

    @Test
    fun onSurface_passesOnEverySurfaceToken() {
        listOf(
            "surface" to p.surface,
            "surfaceContainerLowest" to p.surfaceContainerLowest,
            "surfaceContainerLow" to p.surfaceContainerLow,
            "surfaceContainer" to p.surfaceContainer,
            "surfaceContainerHigh" to p.surfaceContainerHigh,
            "surfaceContainerHighest" to p.surfaceContainerHighest,
            "surfaceBright" to p.surfaceBright,
        ).forEach { (name, bg) -> assertAa(p.onSurface, bg, "onSurface on $name") }
    }

    @Test
    fun inverseTokens_passOnInverseSurface() {
        assertAa(p.inverseOnSurface, p.inverseSurface, "inverseOnSurface on inverseSurface")
        assertAa(p.inversePrimary, p.inverseSurface, "inversePrimary on inverseSurface")
    }
}
