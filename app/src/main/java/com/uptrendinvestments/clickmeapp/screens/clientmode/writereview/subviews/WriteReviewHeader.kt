package com.uptrendinvestments.clickmeapp.screens.clientmode.writereview.subviews

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.avatar.Avatar
import com.uptrendinvestments.clickmeapp.screens.clientmode.writereview.ReviewHeader
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.extras

/** Test tag on the already-submitted banner. */
const val WRITE_REVIEW_SUBMITTED_BANNER_TAG = "write_review_submitted_banner"

private const val HEADLINE_MAX_LINES = 3

/** Tells the user the booking already has a review, shown only in the read-only state. */
@Composable
fun AlreadySubmittedBanner(modifier: Modifier = Modifier) {
    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }
                .testTag(WRITE_REVIEW_SUBMITTED_BANNER_TAG),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(Modifier.padding(Spacing.md), verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = Icons.Outlined.TaskAlt,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(Spacing.md))
            Column {
                Text(
                    text = stringResource(R.string.write_review_submitted_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.write_review_submitted_hint),
                    modifier = Modifier.padding(top = Spacing.xs),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.extras.onSurfaceMuted,
                )
            }
        }
    }
}

/**
 * Who is reviewed: the 96 dp expert avatar (silhouette when there is no photo), the headline, the subtitle and the
 * optional session topic (iOS `WriteReviewHeader`, without the glow and ring).
 */
@Composable
fun WriteReviewHeader(
    header: ReviewHeader,
    modifier: Modifier = Modifier,
) {
    val name = header.expertName?.takeIf(String::isNotBlank) ?: stringResource(R.string.write_review_name_fallback)
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Avatar(
            url = header.expertAvatarUrl,
            contentDescription = null,
            size = 96.dp,
        )
        Spacer(Modifier.height(Spacing.lg))
        Text(
            text = stringResource(R.string.write_review_headline, name),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = HEADLINE_MAX_LINES,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = stringResource(R.string.write_review_subtitle),
            modifier = Modifier.padding(top = Spacing.sm),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        header.sessionTopic?.takeIf(String::isNotBlank)?.let { topic ->
            Text(
                text = topic,
                modifier = Modifier.padding(top = Spacing.xs),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.extras.onSurfaceMuted,
                textAlign = TextAlign.Center,
            )
        }
    }
}
