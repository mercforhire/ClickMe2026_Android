package com.uptrendinvestments.clickmeapp.screens.clientmode.reviews.subviews

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.RatingStars
import com.uptrendinvestments.clickmeapp.components.avatar.Avatar
import com.uptrendinvestments.clickmeapp.screens.clientmode.reviews.ExpertReviewUi
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMePalette
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.extras

private val AvatarSize = 48.dp
private val AvatarRingSize = 54.dp
private val AvatarRingWidth = 2.dp
private const val COMMENT_MAX_LINES = 2

/**
 * One review of the full Reviews list (iOS `ExpertReviewsCard`): avatar in a primary ring, name, stars, topic and date
 * label (each omitted when null) and the comment clamped to [COMMENT_MAX_LINES] lines. Server text is plain [Text]
 * only (T-16.3-16).
 */
@Composable
fun ExpertReviewsCard(
    review: ExpertReviewUi,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(Spacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                verticalAlignment = Alignment.Top,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(AvatarRingSize)
                            .border(AvatarRingWidth, MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Avatar(
                        url = review.avatarUrl,
                        contentDescription = null,
                        size = AvatarSize,
                        fallbackColor = ClickMePalette.avatarFallback,
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    Text(
                        text = review.reviewerName ?: stringResource(R.string.expert_profile_anonymous),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (review.topic != null) {
                        Text(
                            text = stringResource(R.string.expert_reviews_topic, review.topic),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (review.dateLabel != null) {
                        Text(
                            text = review.dateLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.extras.onSurfaceMuted,
                        )
                    }
                }
                RatingStars(rating = review.rating.toDouble(), size = 16.dp)
            }
            if (review.comment != null) {
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    text = review.comment,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = COMMENT_MAX_LINES,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Preview(name = "ExpertReviewsCard")
@Composable
private fun ExpertReviewsCardPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            ExpertReviewsCard(
                ExpertReviewUi("r1", "Sam Lee", null, 5, "Career advice", "Oct 12, 2026", "Great session"),
                Modifier.padding(Spacing.md),
            )
        }
    }
}
