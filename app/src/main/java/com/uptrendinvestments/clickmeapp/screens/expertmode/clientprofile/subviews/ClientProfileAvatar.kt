package com.uptrendinvestments.clickmeapp.screens.expertmode.clientprofile.subviews

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.components.avatar.Avatar
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMePalette
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme

private val BlockHeight = 200.dp
private val RingSize = 148.dp
private val RingWidth = 3.dp
private val PhotoSize = 142.dp
private val GlowRadius = 130.dp
private const val GLOW_ALPHA = 0.20f

/**
 * The Client Profile photo (iOS `ClientProfileAvatar`): a 200 dp block holding a primary ring around the photo over a
 * radial glow. The glow is static, with no infinite pulse and no blur (Phase 16.4 flagged assumption A5; the iOS one
 * pulses for 2.4 s and its ring shadow needs API 31+). The whole block is decorative because the name sits right
 * below it; the fallback disc uses the iOS dark green (UI-SPEC C5).
 */
@Composable
fun ClientProfileAvatar(
    avatarUrl: String?,
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(BlockHeight)
                .clearAndSetSemantics { }
                .drawBehind {
                    val radius = GlowRadius.toPx()
                    drawCircle(
                        brush =
                            Brush.radialGradient(
                                colors = listOf(primary.copy(alpha = GLOW_ALPHA), Color.Transparent),
                                center = center,
                                radius = radius,
                            ),
                        radius = radius,
                        center = center,
                    )
                },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier.size(RingSize).border(RingWidth, primary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Avatar(
                url = avatarUrl,
                contentDescription = null,
                size = PhotoSize,
                fallbackColor = ClickMePalette.avatarFallback,
            )
        }
    }
}

@Preview(name = "ClientProfileAvatar")
@Composable
private fun ClientProfileAvatarPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            ClientProfileAvatar(avatarUrl = null)
        }
    }
}
