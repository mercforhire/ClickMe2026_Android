package com.uptrendinvestments.clickmeapp.screens.clientmode.writereview.subviews

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.SkeletonBlock
import com.uptrendinvestments.clickmeapp.components.SkeletonGroup
import com.uptrendinvestments.clickmeapp.components.buttonstyles.PressScaleButton
import com.uptrendinvestments.clickmeapp.ui.theme.Motion
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.contentMaxWidth

/** Test tag on the Submit review button. */
const val WRITE_REVIEW_SUBMIT_TAG = "write_review_submit"

private const val STAR_PLACEHOLDERS = 5

/**
 * Pinned Submit footer: `surface` fill, a 1 dp top border and one full-width filled button. It sits above the
 * navigation bar and the keyboard. While [submitting] the label is replaced by a spinner and taps do nothing; the
 * button keeps its fill (a disabled button would change color).
 */
@Composable
fun WriteReviewFooter(
    submitting: Boolean,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val submittingDescription = stringResource(R.string.write_review_submitting)
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
            onClick = { if (!submitting) onSubmit() },
            modifier =
                Modifier
                    .contentMaxWidth()
                    .padding(Spacing.md)
                    .heightIn(min = 56.dp)
                    .testTag(WRITE_REVIEW_SUBMIT_TAG)
                    .semantics { if (submitting) stateDescription = submittingDescription },
        ) {
            Crossfade(
                targetState = submitting,
                animationSpec = tween(Motion.CONTROL_STATE_DURATION_MS),
                label = "submitContent",
            ) { inFlight ->
                if (inFlight) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.write_review_submit),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

/** The Write Review first-load placeholder, one "Loading" node: avatar, headline bars, stars, field and footer. */
@Composable
fun WriteReviewSkeleton(modifier: Modifier = Modifier) {
    SkeletonGroup(modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize()) {
            Column(
                modifier =
                    Modifier
                        .contentMaxWidth()
                        .padding(horizontal = Spacing.md)
                        .padding(top = Spacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.lg),
            ) {
                SkeletonBlock(Modifier.size(96.dp), shape = CircleShape)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    SkeletonBlock(Modifier.width(240.dp).height(28.dp))
                    Spacer(Modifier.height(Spacing.sm))
                    SkeletonBlock(Modifier.width(280.dp).height(16.dp))
                }
                Box(Modifier.height(48.dp), contentAlignment = Alignment.Center) {
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        repeat(STAR_PLACEHOLDERS) { SkeletonBlock(Modifier.size(40.dp)) }
                    }
                }
                SkeletonBlock(Modifier.fillMaxWidth().height(160.dp), shape = MaterialTheme.shapes.medium)
            }
            SkeletonBlock(
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .contentMaxWidth()
                        .padding(Spacing.md)
                        .height(56.dp),
                shape = CircleShape,
            )
        }
    }
}
