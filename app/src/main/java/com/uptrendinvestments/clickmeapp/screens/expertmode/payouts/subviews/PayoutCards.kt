package com.uptrendinvestments.clickmeapp.screens.expertmode.payouts.subviews

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.SkeletonBlock
import com.uptrendinvestments.clickmeapp.components.SkeletonGroup
import com.uptrendinvestments.clickmeapp.components.buttonstyles.PressScaleButton
import com.uptrendinvestments.clickmeapp.model.responses.PayoutSummaryData
import com.uptrendinvestments.clickmeapp.screens.expertmode.payouts.PayoutMethodLabel
import com.uptrendinvestments.clickmeapp.screens.expertmode.payouts.nextPayoutDateText
import com.uptrendinvestments.clickmeapp.screens.expertmode.payouts.payoutMethodLabel
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.util.Money

/** Test tag on the merged available-balance hero. */
const val PAYOUTS_BALANCE_TAG = "payouts_balance"

/** Test tag on the Set up payouts button. */
const val PAYOUTS_CONTINUE_TAG = "payouts_continue"

private val CaptionBarWidth = 120.dp
private val CaptionBarHeight = 12.dp
private val AmountBarWidth = 200.dp
private val AmountBarHeight = 28.dp
private val LineBarWidth = 160.dp
private val LineBarHeight = 16.dp
private val MetaCardHeight = 120.dp
private val TileSize = 64.dp
private val GlyphSize = 32.dp
private val ButtonHeight = 56.dp
private val SpinnerSize = 24.dp
private val PillMinHeight = 24.dp
private val PillGlyphSize = 16.dp
private const val TILE_ALPHA = 0.12f
private const val PILL_ALPHA = 0.14f

/**
 * The available balance (PAYOUT-01): caption, the amount in minor units through [Money.format] (a negative balance
 * reads as formatted, never clamped) and the pending amount, as one merged node for TalkBack.
 */
@Composable
fun PayoutBalanceHero(
    summary: PayoutSummaryData,
    modifier: Modifier = Modifier,
) {
    val available = Money.format(summary.availableAmount, summary.currency)
    val pending = Money.format(summary.pendingAmount, summary.currency)
    val description = stringResource(R.string.payouts_balance_a11y, available, pending)
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.md)
                .testTag(PAYOUTS_BALANCE_TAG)
                .semantics(mergeDescendants = true) { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Text(
            text = stringResource(R.string.payouts_available),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Text(
            text = available,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.payouts_pending, pending),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** "Eligible for next payout": shown only for a positive balance on a completed Connect account. */
@Composable
fun PayoutEligiblePill(modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    Row(
        modifier =
            modifier
                .heightIn(min = PillMinHeight)
                .background(primary.copy(alpha = PILL_ALPHA), CircleShape)
                .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Icon(
            imageVector = Icons.Outlined.Verified,
            contentDescription = null,
            modifier = Modifier.size(PillGlyphSize),
            tint = primary,
        )
        Text(
            text = stringResource(R.string.payouts_eligible),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** Which status card [PayoutStatusCard] draws. */
sealed interface PayoutStatusVariant {
    data object Setup : PayoutStatusVariant

    data class Hold(
        val heldCount: Int,
    ) : PayoutStatusVariant

    data object Ready : PayoutStatusVariant
}

/**
 * The centered status card for the states without a balance (UI-SPEC Screen 6): an icon tile, a title and a body.
 * Only [PayoutStatusVariant.Setup] has an action, the filled "Set up payouts" button whose label becomes a spinner
 * while the link is minting ([isMinting]).
 */
@Composable
fun PayoutStatusCard(
    variant: PayoutStatusVariant,
    onSetUp: () -> Unit,
    isMinting: Boolean,
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    val icon =
        when (variant) {
            PayoutStatusVariant.Setup -> Icons.Outlined.CreditCard
            is PayoutStatusVariant.Hold -> Icons.Outlined.Schedule
            PayoutStatusVariant.Ready -> Icons.Outlined.Payments
        }
    val title =
        stringResource(
            when (variant) {
                PayoutStatusVariant.Setup -> R.string.payouts_setup_title
                is PayoutStatusVariant.Hold -> R.string.payouts_hold_title
                PayoutStatusVariant.Ready -> R.string.payouts_ready_title
            },
        )
    val body =
        when (variant) {
            PayoutStatusVariant.Setup -> stringResource(R.string.payouts_setup_body)
            is PayoutStatusVariant.Hold ->
                pluralStringResource(R.plurals.payouts_hold_body, variant.heldCount, variant.heldCount)
            PayoutStatusVariant.Ready -> stringResource(R.string.payouts_ready_body)
        }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(TileSize)
                        .background(primary.copy(alpha = TILE_ALPHA), MaterialTheme.shapes.medium),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(GlyphSize),
                    tint = primary,
                )
            }
            Text(
                text = title,
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (variant == PayoutStatusVariant.Setup) {
                SetUpPayoutsButton(onClick = onSetUp, isMinting = isMinting)
            }
        }
    }
}

@Composable
private fun SetUpPayoutsButton(
    onClick: () -> Unit,
    isMinting: Boolean,
) {
    val busyDescription = stringResource(R.string.payouts_setup_busy_a11y)
    PressScaleButton(
        onClick = onClick,
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = ButtonHeight)
                .testTag(PAYOUTS_CONTINUE_TAG)
                .semantics { if (isMinting) stateDescription = busyDescription },
    ) {
        if (isMinting) {
            CircularProgressIndicator(
                modifier = Modifier.size(SpinnerSize),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        } else {
            Text(stringResource(R.string.payouts_setup_action))
        }
    }
}

/**
 * "Next payout" and "Payout method" rows. A missing or unreadable date reads "Manual schedule", `bank_transfer` reads
 * "Bank Transfer", another method is prettified and a missing one reads "—". Values wrap on large font scales.
 */
@Composable
fun PayoutMetaCard(
    summary: PayoutSummaryData?,
    modifier: Modifier = Modifier,
) {
    val locale = LocalConfiguration.current.locales[0]
    val nextPayout =
        nextPayoutDateText(summary?.nextPayoutDate, locale) ?: stringResource(R.string.payouts_manual_schedule)
    val method =
        when (val label = payoutMethodLabel(summary?.payoutMethod)) {
            PayoutMethodLabel.BankTransfer -> stringResource(R.string.payouts_bank_transfer)
            is PayoutMethodLabel.Raw -> label.text
            PayoutMethodLabel.None -> stringResource(R.string.payouts_none)
        }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            MetaRow(label = stringResource(R.string.payouts_next_payout), value = nextPayout)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            MetaRow(label = stringResource(R.string.payouts_method), value = method)
        }
    }
}

@Composable
private fun MetaRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
        )
    }
}

/** First-load placeholder (UI-SPEC Screen 6): caption, amount and line bars above a `medium` meta card. */
@Composable
fun PayoutsSkeleton(modifier: Modifier = Modifier) {
    SkeletonGroup(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.md).padding(top = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                SkeletonBlock(modifier = Modifier.width(CaptionBarWidth).height(CaptionBarHeight))
                SkeletonBlock(modifier = Modifier.width(AmountBarWidth).height(AmountBarHeight))
                SkeletonBlock(modifier = Modifier.width(LineBarWidth).height(LineBarHeight))
            }
            SkeletonBlock(
                modifier = Modifier.fillMaxWidth().height(MetaCardHeight),
                shape = MaterialTheme.shapes.medium,
            )
        }
    }
}
