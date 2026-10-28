package com.uptrendinvestments.clickmeapp.screens.clientmode.bookingsummary.subviews

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.buttonstyles.PressScaleButton
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing

/** Test tag on the Leave a review button. */
const val BOOKING_SUMMARY_LEAVE_REVIEW_TAG = "booking_summary_leave_review"

private val LeaveReviewMinHeight = 56.dp
private val LeaveReviewIconSize = 24.dp
private val OutlineWidth = 1.dp

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
