package com.uptrendinvestments.clickmeapp.navigation

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.ui.unit.IntOffset
import com.uptrendinvestments.clickmeapp.ui.theme.Motion

/** Phase 1 push/pop transition: horizontal slide plus fade. Shared by the home shells and auth. */
internal fun slideTransition(forward: Boolean): ContentTransform {
    val spec = tween<Float>(Motion.PUSH_POP_DURATION_MS, easing = Motion.pushPopEasing)
    val offsetSpec = tween<IntOffset>(Motion.PUSH_POP_DURATION_MS, easing = Motion.pushPopEasing)
    val direction = if (forward) 1 else -1
    return (slideInHorizontally(offsetSpec) { it * direction } + fadeIn(spec)) togetherWith
        (slideOutHorizontally(offsetSpec) { -it * direction } + fadeOut(spec))
}
