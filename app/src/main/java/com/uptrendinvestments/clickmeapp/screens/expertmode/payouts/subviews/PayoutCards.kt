package com.uptrendinvestments.clickmeapp.screens.expertmode.payouts.subviews

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.SkeletonBlock
import com.uptrendinvestments.clickmeapp.components.SkeletonGroup
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
