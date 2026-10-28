package com.uptrendinvestments.clickmeapp.screens.clientmode.bookingsummary.subviews

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.RatingStars
import com.uptrendinvestments.clickmeapp.components.SectionAppear
import com.uptrendinvestments.clickmeapp.components.buttonstyles.PressScaleButton
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing

/** Test tag on the Feedback card. */
const val BOOKING_SUMMARY_FEEDBACK_TAG = "booking_summary_feedback"

/** Test tag on the Leave a review button. */
const val BOOKING_SUMMARY_LEAVE_REVIEW_TAG = "booking_summary_leave_review"

private val LeaveReviewMinHeight = 56.dp
private val LeaveReviewIconSize = 24.dp
private val OutlineWidth = 1.dp
private val FeedbackStarSize = 24.dp

/**
 * "Leave a review" (D-18, UI-SPEC Surface 4): the outlined, neutral text button in the Message Expert style, never
 * filled, so Book Again stays the single filled CTA. [heightIn] grows with the font scale.
 */
@Composable
fun LeaveReviewButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PressScaleButton(
        onClick = onClick,
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = LeaveReviewMinHeight)
                .border(OutlineWidth, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                .testTag(BOOKING_SUMMARY_LEAVE_REVIEW_TAG),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
    ) {
        Icon(Icons.Outlined.StarBorder, contentDescription = null, modifier = Modifier.size(LeaveReviewIconSize))
        Spacer(Modifier.width(Spacing.sm))
        Text(stringResource(R.string.booking_summary_leave_review))
    }
}

/**
 * The review the user left (D-18, UI-SPEC Surface 4): a "Your feedback" header over a card with the stars and, when
 * [comment] is not blank, the comment. The card reads "Rated {n} out of 5" to TalkBack. Fades and expands in.
 */
@Composable
fun BookingSummaryFeedback(
    rating: Int,
    comment: String?,
    modifier: Modifier = Modifier,
) {
    val rated = stringResource(R.string.review_rated_out_of_five, rating)
    SectionAppear(modifier = modifier) {
        Column(modifier = Modifier.padding(top = Spacing.md)) {
            Text(
                text = stringResource(R.string.booking_summary_feedback_header),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.semantics { heading() },
            )
            Card(
                modifier =
                    Modifier
                        .padding(top = Spacing.sm)
                        .fillMaxWidth()
                        .testTag(BOOKING_SUMMARY_FEEDBACK_TAG),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                border = BorderStroke(OutlineWidth, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(modifier = Modifier.padding(Spacing.md)) {
                    RatingStars(
                        rating = rating.toDouble(),
                        size = FeedbackStarSize,
                        modifier = Modifier.semantics { contentDescription = rated },
                    )
                    if (!comment.isNullOrBlank()) {
                        SelectionContainer {
                            Text(
                                text = comment,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = Spacing.md),
                            )
                        }
                    }
                }
            }
        }
    }
}
