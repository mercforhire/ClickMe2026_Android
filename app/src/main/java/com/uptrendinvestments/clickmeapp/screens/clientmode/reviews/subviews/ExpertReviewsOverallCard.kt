package com.uptrendinvestments.clickmeapp.screens.clientmode.reviews.subviews

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.extras

private val StarSize = 24.dp

/**
 * Summary card at the top of the Reviews list (iOS `ExpertReviewsOverallCard`): the rating to one decimal, a half-star
 * row and "{n} Total Reviews" ("1 Total Review" for one). One TalkBack node that reads the whole sentence.
 */
@Composable
fun ExpertReviewsOverallCard(
    overallRating: Double,
    totalReviews: Int,
    modifier: Modifier = Modifier,
) {
    val ratingText = stringResource(R.string.rating_value, overallRating)
    val totalText = pluralStringResource(R.plurals.expert_reviews_total, totalReviews, totalReviews)
    val description = stringResource(R.string.expert_reviews_overall_a11y, ratingText, totalText)
    Surface(
        modifier = modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = description },
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Text(
                text = stringResource(R.string.expert_reviews_overall_label),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = ratingText,
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    ExpertReviewsStarRow(rating = overallRating, size = StarSize)
                    Text(
                        text = totalText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.extras.onSurfaceMuted,
                    )
                }
            }
        }
    }
}

@Preview(name = "ExpertReviewsOverallCard")
@Composable
private fun ExpertReviewsOverallCardPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            ExpertReviewsOverallCard(4.5, 12, Modifier.padding(Spacing.md))
        }
    }
}
