package com.uptrendinvestments.clickmeapp.screens.clientmode.expertprofile.subviews

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.RatingStars
import com.uptrendinvestments.clickmeapp.screens.clientmode.expertprofile.ReviewRowUi
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing

private const val MAX_REVIEWS = 3

/**
 * Recent Reviews preview (D-05, D-12): at most [MAX_REVIEWS] rows plus a "See all" header action that is always
 * present (D-13, D-14); with zero [reviews] the card is only the header strip. A null reviewer name reads
 * "Anonymous". The caller hides the card when the reviews load failed. Server text is plain [Text] only (T-05-29).
 */
@Composable
fun ExpertProfileReviewsCard(
    reviews: List<ReviewRowUi>,
    onSeeAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ExpertProfileSectionCard(
        title = stringResource(R.string.expert_profile_recent_reviews),
        modifier = modifier,
        hasContent = reviews.isNotEmpty(),
        headerAction = {
            TextButton(
                onClick = onSeeAll,
                modifier = Modifier.heightIn(min = 48.dp),
                contentPadding = PaddingValues(horizontal = Spacing.sm),
            ) {
                Text(
                    text = stringResource(R.string.expert_profile_see_all),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textDecoration = TextDecoration.Underline,
                )
            }
        },
    ) {
        reviews.take(MAX_REVIEWS).forEachIndexed { index, review ->
            if (index > 0) CardDivider()
            ReviewRow(review)
        }
    }
}

@Composable
private fun ReviewRow(review: ReviewRowUi) {
    val comment = review.comment?.takeIf { it.isNotBlank() }
    Column(Modifier.fillMaxWidth().padding(Spacing.md)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = review.author ?: stringResource(R.string.expert_profile_anonymous),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            RatingStars(rating = review.rating.toDouble(), size = 16.dp)
        }
        if (comment != null) {
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = comment,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Reviews placeholder: a header strip and two 72 dp rows (D-15). */
@Composable
fun ExpertProfileReviewsCardSkeleton(modifier: Modifier = Modifier) {
    ExpertProfileCardSkeleton(rows = 2, rowHeight = 72.dp, modifier = modifier)
}

@Preview(name = "ExpertProfileReviewsCard")
@Composable
private fun ExpertProfileReviewsCardPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            ExpertProfileReviewsCard(
                reviews =
                    listOf(
                        ReviewRowUi("1", "Grace", 5, "Very helpful session."),
                        ReviewRowUi("2", null, 4, null),
                    ),
                onSeeAll = {},
            )
        }
    }
}
