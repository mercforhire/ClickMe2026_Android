package com.uptrendinvestments.clickmeapp.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.extras
import kotlin.math.roundToInt

/** Test tag on each filled star of [RatingStars]. */
internal const val RATING_STAR_FILLED_TAG = "rating_star_filled"

/** Test tag on each empty star of [RatingStars]. */
internal const val RATING_STAR_EMPTY_TAG = "rating_star_empty"

private const val STAR_COUNT = 5

/**
 * Five-star rating display. `round(rating)` (clamped to 0..5) stars are filled in tertiary, the rest use the muted
 * overlay. Decorative: the parent supplies the "Rated X out of 5" description for TalkBack.
 */
@Composable
fun RatingStars(
    rating: Double,
    modifier: Modifier = Modifier,
    size: Dp = 16.dp,
) {
    val filled = rating.coerceIn(0.0, STAR_COUNT.toDouble()).roundToInt()
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(STAR_COUNT) { index ->
            val isFilled = index < filled
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                modifier =
                    Modifier
                        .size(size)
                        .testTag(if (isFilled) RATING_STAR_FILLED_TAG else RATING_STAR_EMPTY_TAG),
                tint = if (isFilled) MaterialTheme.colorScheme.tertiary else MaterialTheme.extras.overlayWhite20,
            )
        }
    }
}

@Preview(name = "RatingStars")
@Composable
private fun RatingStarsPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            RatingStars(rating = 3.4)
        }
    }
}
