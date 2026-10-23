package com.uptrendinvestments.clickmeapp.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.extras

/** Test tag on the [PaymentLifecycleCard] container. */
internal const val PAYMENT_LIFECYCLE_CARD_TAG = "payment_lifecycle_card"

/** Test tag on lifecycle step [index] (0 = hold, 1 = session, 2 = charge). */
internal fun paymentLifecycleStepTag(index: Int) = "payment_lifecycle_step_$index"

/** Which point of the payment lifecycle the card represents. */
enum class PaymentLifecycleStage {
    /** Pre-payment: nothing has happened yet, so no step is done. */
    Upcoming,

    /** Post-payment: the authorization hold exists, so step 1 is done. */
    HoldPlaced,
}

private val BadgeSize = 32.dp
private val BadgeIconSize = 16.dp
private val ConnectorWidth = 2.dp
private val ConnectorHeight = 16.dp
private const val DONE_BADGE_ALPHA = 0.18f
private const val DONE_CONNECTOR_ALPHA = 0.35f
private const val FUTURE_BADGE_ALPHA = 0.38f
private const val STEP_COUNT = 3

/**
 * Port of iOS `PaymentLifecycleCard`: a three-step "What happens next" timeline (hold placed, session runs, card
 * charged after). Used by the "How does payment work?" sheet ([PaymentLifecycleStage.Upcoming]) and by paid Request
 * Sent ([PaymentLifecycleStage.HoldPlaced]). Static copy only: it never shows amounts or fees.
 *
 * [sessionWhen] replaces the generic step 2 title with the scheduled time when it is not blank.
 */
@Composable
fun PaymentLifecycleCard(
    stage: PaymentLifecycleStage,
    sessionWhen: String?,
    modifier: Modifier = Modifier,
) {
    val doneStepCount = if (stage == PaymentLifecycleStage.HoldPlaced) 1 else 0
    val steps =
        listOf(
            LifecycleStep(
                Icons.Outlined.Lock,
                stringResource(R.string.payment_lifecycle_step_hold_title),
                stringResource(R.string.payment_lifecycle_step_hold_subtitle),
            ),
            LifecycleStep(
                Icons.Outlined.CalendarMonth,
                if (sessionWhen.isNullOrBlank()) {
                    stringResource(R.string.payment_lifecycle_step_session_title_fallback)
                } else {
                    stringResource(R.string.payment_lifecycle_step_session_title, sessionWhen)
                },
                stringResource(R.string.payment_lifecycle_step_session_subtitle),
            ),
            LifecycleStep(
                Icons.Outlined.Verified,
                stringResource(R.string.payment_lifecycle_step_charge_title),
                stringResource(R.string.payment_lifecycle_step_charge_subtitle),
            ),
        )
    Surface(
        modifier = modifier.fillMaxWidth().testTag(PAYMENT_LIFECYCLE_CARD_TAG),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Text(
                text = stringResource(R.string.payment_lifecycle_header),
                modifier = Modifier.padding(bottom = Spacing.md).semantics { heading() },
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            steps.forEachIndexed { index, step ->
                StepRow(step = step, index = index, done = index < doneStepCount, isLast = index == STEP_COUNT - 1)
            }
        }
    }
}

private data class LifecycleStep(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
)

@Composable
private fun StepRow(
    step: LifecycleStep,
    index: Int,
    done: Boolean,
    isLast: Boolean,
) {
    val doneLabel = stringResource(R.string.payment_lifecycle_state_done)
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag(paymentLifecycleStepTag(index))
                .semantics(mergeDescendants = true) { if (done) stateDescription = doneLabel },
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Badge(icon = step.icon, done = done)
            Spacer(Modifier.width(Spacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = step.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    text = step.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.extras.onSurfaceMuted,
                )
            }
        }
        if (!isLast) {
            Box(modifier = Modifier.width(BadgeSize), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .width(ConnectorWidth)
                        .height(ConnectorHeight)
                        .background(
                            if (done) {
                                MaterialTheme.colorScheme.primary.copy(alpha = DONE_CONNECTOR_ALPHA)
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            },
                        ),
                )
            }
        }
    }
}

@Composable
private fun Badge(
    icon: ImageVector,
    done: Boolean,
) {
    val fill =
        if (done) {
            MaterialTheme.colorScheme.primary.copy(alpha = DONE_BADGE_ALPHA)
        } else {
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = FUTURE_BADGE_ALPHA)
        }
    Box(
        modifier = Modifier.size(BadgeSize).clip(CircleShape).background(fill),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (done) Icons.Outlined.Check else icon,
            contentDescription = null,
            modifier = Modifier.size(BadgeIconSize),
            tint = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.extras.onSurfaceMuted,
        )
    }
}

@Preview(name = "PaymentLifecycleCard")
@Composable
private fun PaymentLifecycleCardPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Column(
                modifier = Modifier.padding(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                PaymentLifecycleCard(stage = PaymentLifecycleStage.Upcoming, sessionWhen = null)
                PaymentLifecycleCard(stage = PaymentLifecycleStage.HoldPlaced, sessionWhen = "Oct 24, 2:00 PM")
            }
        }
    }
}
