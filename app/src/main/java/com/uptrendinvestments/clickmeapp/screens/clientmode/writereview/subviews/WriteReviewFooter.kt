package com.uptrendinvestments.clickmeapp.screens.clientmode.writereview.subviews

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
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

/** Test tag on the Submit review button. */
const val WRITE_REVIEW_SUBMIT_TAG = "write_review_submit"

/**
 * Pinned Submit footer: `surface` fill, a 1 dp top border and one full-width filled button. It sits above the
 * navigation bar and the keyboard. [submitting] blocks repeat taps.
 */
@Composable
fun WriteReviewFooter(
    submitting: Boolean,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .navigationBarsPadding()
                .imePadding(),
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        PressScaleButton(
            onClick = onSubmit,
            enabled = !submitting,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(Spacing.md)
                    .heightIn(min = 56.dp)
                    .testTag(WRITE_REVIEW_SUBMIT_TAG),
        ) {
            Text(
                text = stringResource(R.string.write_review_submit),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}
