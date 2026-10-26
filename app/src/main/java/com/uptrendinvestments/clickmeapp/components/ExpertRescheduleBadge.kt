package com.uptrendinvestments.clickmeapp.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing

/** Test tag on the reschedule badge of an expert card. */
const val EXPERT_RESCHEDULE_BADGE_TAG = "expert_reschedule_badge"

private const val BADGE_MAX_LINES = 2

/**
 * The tinted badge an expert booking card shows while a reschedule is pending (D-12, UI-SPEC surface 5). A proposal
 * from the client reads "Tap to review"; the expert's own proposal reads "pending client's response". It uses the
 * same tertiary tint as the reschedule response card so the two read as one thing.
 */
@Composable
fun ExpertRescheduleBadge(
    initiatedByClient: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.testTag(EXPERT_RESCHEDULE_BADGE_TAG),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.45f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Icon(
                imageVector = Icons.Outlined.Update,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.tertiary,
            )
            Text(
                text =
                    stringResource(
                        if (initiatedByClient) {
                            R.string.expert_reschedule_badge_client
                        } else {
                            R.string.expert_reschedule_badge_expert
                        },
                    ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = BADGE_MAX_LINES,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Preview(name = "ExpertRescheduleBadge client")
@Composable
private fun RescheduleBadgeClientPreview() {
    ClickMeTheme { ExpertRescheduleBadge(initiatedByClient = true, modifier = Modifier.padding(Spacing.md)) }
}

@Preview(name = "ExpertRescheduleBadge expert")
@Composable
private fun RescheduleBadgeExpertPreview() {
    ClickMeTheme { ExpertRescheduleBadge(initiatedByClient = false, modifier = Modifier.padding(Spacing.md)) }
}
