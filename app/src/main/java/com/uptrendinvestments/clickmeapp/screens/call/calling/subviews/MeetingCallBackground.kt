package com.uptrendinvestments.clickmeapp.screens.call.calling.subviews

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp

/** Full-bleed call backdrop: the surface color with a soft top wash (iOS `vignetteTop`). Decorative. */
@Composable
fun MeetingCallBackground(modifier: Modifier = Modifier) {
    val surface = MaterialTheme.colorScheme.surface
    val wash = MaterialTheme.colorScheme.surfaceContainerLow
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(surface)
                .drawBehind {
                    drawRect(
                        Brush.radialGradient(
                            colors = listOf(wash, surface),
                            center = Offset(x = size.width / 2f, y = 0f),
                            radius = 600.dp.toPx(),
                        ),
                    )
                },
    )
}
