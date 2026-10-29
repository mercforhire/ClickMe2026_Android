package com.uptrendinvestments.clickmeapp.ui.theme

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Widest a content column grows on a large or resized window (CONTEXT D-02). */
val ContentMaxWidth: Dp = 600.dp

/**
 * Content column cap for large windows (CONTEXT D-02, UI-SPEC "Large screens"): full width up to [ContentMaxWidth],
 * centered beyond it. App bars, the tab bar and full-bleed decoration stay outside it.
 */
fun Modifier.contentMaxWidth(): Modifier =
    this
        .fillMaxWidth()
        .wrapContentWidth(Alignment.CenterHorizontally)
        .widthIn(max = ContentMaxWidth)
        .fillMaxWidth()
