package com.uptrendinvestments.clickmeapp.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier

/** UI-SPEC Motion: optional sections fade and expand in over 200 ms. */
const val SECTION_APPEAR_MS = 200

/** Restores as already shown (current == target), so a restored section never animates again. */
private val AppearedSaver =
    Saver<MutableTransitionState<Boolean>, Boolean>(
        save = { it.targetState },
        restore = { MutableTransitionState(it) },
    )

/**
 * Fades and expands [content] into place the first time it composes (UI-SPEC Motion, 200 ms).
 *
 * The transition state is saveable, so inside a `LazyColumn` item (whose saveable state outlives the item while
 * it is scrolled off screen) the animation runs once. Scrolling the item back into view shows it at full height
 * at once instead of re-expanding from zero and shifting the content around it. Leaving composition for any
 * other reason (the section is removed, then added again) starts over and animates again.
 */
@Composable
fun SectionAppear(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val visibleState =
        rememberSaveable(saver = AppearedSaver) { MutableTransitionState(false).apply { targetState = true } }
    AnimatedVisibility(
        visibleState = visibleState,
        modifier = modifier,
        enter = fadeIn(tween(SECTION_APPEAR_MS)) + expandVertically(tween(SECTION_APPEAR_MS)),
    ) {
        content()
    }
}
