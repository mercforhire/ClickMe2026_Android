package com.uptrendinvestments.clickmeapp.screens.clientmode.cancellation

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.screens.login.AuthTopAppBar
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.contentMaxWidth
import com.uptrendinvestments.clickmeapp.ui.theme.extras
import kotlinx.serialization.Serializable

/**
 * Nav3 route to the Cancellation Policy screen, pushed from the cancel confirmation step. It has no arguments, so it
 * is a data object. Back pops to the confirmation, where the cancel ViewModel (scoped to its own entry) still holds
 * the selected reason.
 */
@Serializable
data object CancellationPolicyKey : NavKey

private val CardIcon = 22.dp
private val StepBadge = 28.dp
private val CardBorderWidth = 1.5.dp
private const val CARD_BORDER_ALPHA = 0.5f

/**
 * Cancellation Policy (iOS `CancellationPoliciesView`, SET-07): three cards, Cancellation Deadlines, Refund
 * Eligibility and How to Initiate a Cancellation. The text is copied from iOS word for word and says what the
 * backend's `refund.js` does: a full refund only when the cancellation is strictly more than 24 hours before the
 * start, never a refund timing.
 */
@Composable
fun CancellationPolicyScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = { AuthTopAppBar(title = stringResource(R.string.cancel_policy_title), onBack = onBack) },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Column(
                modifier =
                    Modifier
                        .verticalScroll(rememberScrollState())
                        .contentMaxWidth()
                        .padding(horizontal = Spacing.md, vertical = Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                DeadlinesCard()
                RefundEligibilityCard()
                HowToInitiateCard()
            }
        }
    }
}

@Composable
private fun PolicyCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(CardBorderWidth, MaterialTheme.colorScheme.primary.copy(alpha = CARD_BORDER_ALPHA)),
    ) {
        Column(modifier = Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
            Text(
                text = title,
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            content()
        }
    }
}

@Composable
private fun DeadlinesCard() {
    PolicyCard(title = stringResource(R.string.cancel_policy_deadlines_title)) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            PolicyRow(
                icon = Icons.Outlined.AccessTime,
                tint = MaterialTheme.colorScheme.primary,
                title = R.string.cancel_policy_deadline_early_title,
                body = R.string.cancel_policy_deadline_early_body,
            )
            PolicyRow(
                icon = Icons.Outlined.AccessTime,
                tint = MaterialTheme.colorScheme.primary,
                title = R.string.cancel_policy_deadline_late_title,
                body = R.string.cancel_policy_deadline_late_body,
            )
            PolicyRow(
                icon = Icons.Outlined.AccessTime,
                tint = MaterialTheme.colorScheme.primary,
                title = R.string.cancel_policy_deadline_after_title,
                body = R.string.cancel_policy_deadline_after_body,
            )
        }
    }
}

@Composable
private fun RefundEligibilityCard() {
    PolicyCard(title = stringResource(R.string.cancel_policy_refund_title)) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            PolicyRow(
                icon = Icons.Outlined.Check,
                tint = MaterialTheme.colorScheme.primary,
                title = R.string.cancel_policy_refund_not_charged_title,
                body = R.string.cancel_policy_refund_not_charged_body,
            )
            PolicyRow(
                icon = Icons.Outlined.Check,
                tint = MaterialTheme.colorScheme.primary,
                title = R.string.cancel_policy_refund_full_title,
                body = R.string.cancel_policy_refund_full_body,
            )
            PolicyRow(
                icon = Icons.Outlined.Close,
                tint = MaterialTheme.colorScheme.error,
                title = R.string.cancel_policy_refund_none_title,
                body = R.string.cancel_policy_refund_none_body,
            )
        }
    }
}

@Composable
private fun HowToInitiateCard() {
    PolicyCard(title = stringResource(R.string.cancel_policy_how_title)) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Text(
                text = stringResource(R.string.cancel_policy_how_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.extras.onSurfaceMuted,
            )
            listOf(
                R.string.cancel_policy_how_step_1,
                R.string.cancel_policy_how_step_2,
                R.string.cancel_policy_how_step_3,
                R.string.cancel_policy_how_step_4,
            ).forEachIndexed { index, step -> StepRow(number = index + 1, text = stringResource(step)) }
        }
    }
}

@Composable
private fun PolicyRow(
    icon: ImageVector,
    tint: Color,
    @StringRes title: Int,
    @StringRes body: Int,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(CardIcon), tint = tint)
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(
                text = stringResource(title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.extras.onSurfaceMuted,
            )
        }
    }
}

@Composable
private fun StepRow(
    number: Int,
    text: String,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md), verticalAlignment = Alignment.CenterVertically) {
        Surface(
            modifier = Modifier.size(StepBadge),
            shape = CircleShape,
            color = Color.Transparent,
            border = BorderStroke(CardBorderWidth, MaterialTheme.colorScheme.primary),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = number.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                )
            }
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Preview(name = "CancellationPolicyScreen")
@Composable
private fun CancellationPolicyScreenPreview() {
    ClickMeTheme { CancellationPolicyScreen(onBack = {}) }
}
