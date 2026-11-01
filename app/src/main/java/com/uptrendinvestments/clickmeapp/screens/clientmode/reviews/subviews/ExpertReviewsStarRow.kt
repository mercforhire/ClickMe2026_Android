package com.uptrendinvestments.clickmeapp.screens.clientmode.reviews.subviews

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.StarHalf
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.extras

private const val STAR_COUNT = 5
private const val HALF = 0.5

/** How one star of [ExpertReviewsStarRow] is drawn. */
enum class StarKind { Full, Half, Empty }

/**
 * The five star kinds for [rating]: star `i` is [StarKind.Full] when `i <= rating`, [StarKind.Half] when
 * `i - 0.5 <= rating`, else [StarKind.Empty] (iOS `ExpertReviewsStarRow`), so 3.5 gives 3 full, 1 half and 1 empty.
 */
fun starKinds(rating: Double): List<StarKind> =
    (1..STAR_COUNT).map { i ->
        when {
            i <= rating -> StarKind.Full
            i - HALF <= rating -> StarKind.Half
            else -> StarKind.Empty
        }
    }

/** Five stars with half-star support. Decorative: the parent card supplies the spoken rating. */
@Composable
fun ExpertReviewsStarRow(
    rating: Double,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.clearAndSetSemantics {},
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        starKinds(rating).forEach { kind ->
            when (kind) {
                StarKind.Full ->
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        modifier = Modifier.size(size),
                        tint = MaterialTheme.colorScheme.tertiary,
                    )
                StarKind.Half ->
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.StarHalf,
                        contentDescription = null,
                        modifier = Modifier.size(size),
                        tint = MaterialTheme.colorScheme.tertiary,
                    )
                StarKind.Empty ->
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        modifier = Modifier.size(size),
                        tint = MaterialTheme.extras.overlayWhite20,
                    )
            }
        }
    }
}

@Preview(name = "ExpertReviewsStarRow")
@Composable
private fun ExpertReviewsStarRowPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            ExpertReviewsStarRow(rating = 3.5, size = 24.dp)
        }
    }
}
