package com.uptrendinvestments.clickmeapp.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing

/** Test tag on every [SkeletonBlock]. */
internal const val SKELETON_BLOCK_TAG = "skeleton_block"

private const val PULSE_MIN_ALPHA = 0.5f
private const val PULSE_MAX_ALPHA = 1f
private const val PULSE_DURATION_MS = 1000

/**
 * Placeholder block that pulses its alpha between 0.5 and 1.0 over 1000 ms. Size it with [modifier]. Decorative: wrap
 * a group of blocks in [SkeletonGroup] so TalkBack announces a single "Loading".
 */
@Composable
fun SkeletonBlock(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.small,
) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = PULSE_MIN_ALPHA,
        targetValue = PULSE_MAX_ALPHA,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = PULSE_DURATION_MS, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "skeletonAlpha",
    )
    Box(
        modifier =
            modifier
                .testTag(SKELETON_BLOCK_TAG)
                .alpha(alpha)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
    )
}

/** Groups skeleton blocks under one merged "Loading" content description. */
@Composable
fun SkeletonGroup(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val loading = stringResource(R.string.content_loading)
    Box(
        modifier =
            modifier.semantics(mergeDescendants = true) {
                contentDescription = loading
            },
    ) {
        content()
    }
}

@Preview(name = "SkeletonGroup")
@Composable
private fun SkeletonGroupPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            SkeletonGroup {
                Column {
                    SkeletonBlock(Modifier.size(64.dp))
                    Spacer(Modifier.height(Spacing.sm))
                    SkeletonBlock(Modifier.fillMaxWidth().height(16.dp))
                }
            }
        }
    }
}
