package com.uptrendinvestments.clickmeapp.ui.theme

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing

object Motion {
    const val PRESS_DURATION_MS = 120
    const val PRESSED_SCALE = 0.96f
    const val CONTROL_STATE_DURATION_MS = 150
    const val FIELD_ERROR_DURATION_MS = 200
    const val FIELD_ERROR_OFFSET_DP = 4
    const val TAB_FADE_IN_MS = 150
    const val TAB_FADE_OUT_MS = 90
    const val PUSH_POP_DURATION_MS = 300

    val pressEasing: Easing = EaseInOut
    val pushPopEasing: Easing = FastOutSlowInEasing
}
