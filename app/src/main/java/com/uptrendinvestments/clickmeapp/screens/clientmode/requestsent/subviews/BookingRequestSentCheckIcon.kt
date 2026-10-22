package com.uptrendinvestments.clickmeapp.screens.clientmode.requestsent.subviews

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Decorative 128 dp check badge (D-14). No glow or blur (D-15). */
@Composable
fun BookingRequestSentCheckIcon(modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    Surface(
        modifier = modifier.size(128.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, primary.copy(alpha = 0.25f)),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = primary,
            )
        }
    }
}
