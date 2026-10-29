package com.uptrendinvestments.clickmeapp.testing

import android.view.View
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Inset helpers for Robolectric Compose tests (CONTEXT D-17). Use with `createAndroidComposeRule<ComponentActivity>()`
 * and `@Config(qualifiers = "w360dp-h800dp")` so dp == px; capture `LocalView.current` inside `setContent` and pass
 * it to [dispatchInsets]. Assert on `getUnclippedBoundsInRoot()` of the footer or composer.
 */
fun imeInsets(bottomPx: Int): WindowInsetsCompat {
    val ime = WindowInsetsCompat.Type.ime()
    return WindowInsetsCompat
        .Builder()
        .setInsets(ime, Insets.of(0, 0, 0, bottomPx))
        .setVisible(ime, bottomPx > 0)
        .build()
}

/** A visible navigation bar [bottomPx] tall. */
fun navigationBarInsets(bottomPx: Int): WindowInsetsCompat {
    val bars = WindowInsetsCompat.Type.navigationBars()
    return WindowInsetsCompat
        .Builder()
        .setInsets(bars, Insets.of(0, 0, 0, bottomPx))
        .setVisible(bars, bottomPx > 0)
        .build()
}

/** A display cutout intruding [leftPx] and [rightPx] from the sides. */
fun cutoutInsets(
    leftPx: Int,
    rightPx: Int,
): WindowInsetsCompat {
    val cutout = WindowInsetsCompat.Type.displayCutout()
    return WindowInsetsCompat
        .Builder()
        .setInsets(cutout, Insets.of(leftPx, 0, rightPx, 0))
        .build()
}

private val InsetTypes =
    intArrayOf(
        WindowInsetsCompat.Type.statusBars(),
        WindowInsetsCompat.Type.navigationBars(),
        WindowInsetsCompat.Type.ime(),
        WindowInsetsCompat.Type.displayCutout(),
    )

/** Merges every inset type of [parts] into one value; later parts win a type both set. */
fun combinedInsets(vararg parts: WindowInsetsCompat): WindowInsetsCompat {
    val builder = WindowInsetsCompat.Builder()
    for (part in parts) {
        for (type in InsetTypes) {
            val insets = part.getInsets(type)
            if (insets != Insets.NONE) {
                builder.setInsets(type, insets)
                builder.setVisible(type, part.isVisible(type))
            }
        }
    }
    return builder.build()
}

/**
 * Dispatches [insets] to the window root of [view] (the Activity decor view) and to [view] itself, where [view] is the
 * `LocalView` captured in `setContent`.
 */
fun ComposeContentTestRule.dispatchInsets(
    view: View,
    insets: WindowInsetsCompat,
) {
    runOnUiThread {
        ViewCompat.dispatchApplyWindowInsets(view.rootView, insets)
        ViewCompat.dispatchApplyWindowInsets(view, insets)
    }
    waitForIdle()
}
