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

    /** How long the "saved" confirmation stays visible before the flow advances. */
    const val SAVED_CONFIRMATION_HOLD_MS = 600

    /** Fade and expand duration when the password strength meter appears or disappears. */
    const val STRENGTH_METER_VISIBILITY_MS = 200

    /** Duration of a strength meter segment color change. */
    const val STRENGTH_SEGMENT_COLOR_MS = 300

    /** Half-cycle of the hero avatar ring pulse while connecting or waiting (UI-SPEC Motion). */
    const val CALL_RING_PULSE_MS = 900

    /** Half-cycle of the ambient glow behind the call hero (UI-SPEC Motion). */
    const val CALL_GLOW_PULSE_MS = 2200

    val pressEasing: Easing = EaseInOut
    val pushPopEasing: Easing = FastOutSlowInEasing
}
