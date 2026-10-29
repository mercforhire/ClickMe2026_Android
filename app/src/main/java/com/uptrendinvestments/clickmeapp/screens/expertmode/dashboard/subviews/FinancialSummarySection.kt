package com.uptrendinvestments.clickmeapp.screens.expertmode.dashboard.subviews

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.model.responses.PayoutSummaryData
import com.uptrendinvestments.clickmeapp.model.responses.WeeklyEarningsData
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.formatShortDate
import com.uptrendinvestments.clickmeapp.screens.expertmode.dashboard.PayoutOnboarding
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.extras
import com.uptrendinvestments.clickmeapp.util.Money
import java.time.LocalDate
import java.time.format.DateTimeParseException

/** Test tag on the payout onboarding card. */
const val DASHBOARD_PAYOUT_ONBOARDING_TAG = "dashboard_payout_onboarding"

/** Test tag on the weekly earnings card. */
const val DASHBOARD_EARNINGS_CARD_TAG = "dashboard_earnings_card"

private val TileSize = 48.dp
private val GlyphSize = 24.dp
private val SmallGlyphSize = 16.dp
private const val TILE_ALPHA = 0.12f
private const val CARD_FILL_ALPHA = 0.10f
private const val CARD_BORDER_ALPHA = 0.45f

/**
 * The Financial Summary block (UI-SPEC surface 1, block 5): the "Financial Summary" heading, the weekly earnings card
 * (always shown, D-10) and, while Stripe Connect setup is [PayoutOnboarding.Required], the payout onboarding card.
 * A null [weekly] or [payoutSummary] means that fetch failed and reads "—".
 */
@Composable
fun FinancialSummarySection(
    weekly: WeeklyEarningsData?,
    onboarding: PayoutOnboarding,
    payoutSummary: PayoutSummaryData?,
    isMinting: Boolean,
    onStartPayoutSetup: () -> Unit,
    onOpenPayouts: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.dashboard_financial_summary),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(Spacing.sm))
        EarningsCard(weekly = weekly, onboarding = onboarding, payoutSummary = payoutSummary, onClick = onOpenPayouts)
        if (onboarding is PayoutOnboarding.Required) {
            Spacer(Modifier.height(Spacing.md))
            PayoutOnboardingCard(isBusy = isMinting, onClick = onStartPayoutSetup)
        }
    }
}

/**
 * Weekly earnings card (D-10, UI-SPEC A4): "This week" with the net amount and the session count on the left, the
 * available payout on the right and, unless payout setup is still required, the next payout line. Amounts are integer
 * minor units formatted by [Money.format]; the gross amount is never shown. Tapping it opens Payouts (Phase 14). The
 * columns wrap at large font scales.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EarningsCard(
    weekly: WeeklyEarningsData?,
    onboarding: PayoutOnboarding,
    payoutSummary: PayoutSummaryData?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val openPayoutsLabel = stringResource(R.string.dashboard_open_payouts)
    val unavailable = stringResource(R.string.dashboard_amount_unavailable)
    val net = weekly?.let { Money.format(it.netAmount, it.currency) } ?: unavailable
    val sessions =
        weekly?.let { pluralStringResource(R.plurals.dashboard_sessions_count, it.sessionCount, it.sessionCount) }
    val earningsDescription = sessions?.let { stringResource(R.string.dashboard_earnings_a11y, net, it) }
    val available =
        if (onboarding == PayoutOnboarding.Onboarded && payoutSummary != null) {
            Money.format(payoutSummary.availableAmount, payoutSummary.currency)
        } else {
            unavailable
        }
    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .testTag(DASHBOARD_EARNINGS_CARD_TAG)
                .clip(MaterialTheme.shapes.medium)
                .clickable(onClickLabel = openPayoutsLabel, role = Role.Button, onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                Column(
                    modifier =
                        Modifier.semantics(mergeDescendants = true) {
                            earningsDescription?.let { contentDescription = it }
                        },
                ) {
                    Text(
                        text = stringResource(R.string.dashboard_this_week),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.extras.onSurfaceMuted,
                    )
                    Text(
                        text = net,
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    sessions?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(R.string.dashboard_available_payout),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.extras.onSurfaceMuted,
                    )
                    Text(
                        text = available,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            if (onboarding !is PayoutOnboarding.Required) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = Spacing.md),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
                NextPayoutRow(summary = payoutSummary.takeIf { onboarding == PayoutOnboarding.Onboarded })
            }
        }
    }
}

/** "Next payout on {date}", the manual-schedule line, or "Payout schedule unavailable" when there is no summary. */
@Composable
private fun NextPayoutRow(summary: PayoutSummaryData?) {
    val locale = LocalConfiguration.current.locales[0]
    val date = remember(summary?.nextPayoutDate) { parsePayoutDate(summary?.nextPayoutDate) }
    val text =
        when {
            summary == null -> stringResource(R.string.dashboard_payout_unavailable)
            date == null -> stringResource(R.string.dashboard_payout_manual)
            else -> stringResource(R.string.dashboard_next_payout, formatShortDate(date, locale))
        }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Icon(
            imageVector = Icons.Outlined.AccountBalance,
            contentDescription = null,
            modifier = Modifier.size(SmallGlyphSize),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** The server's `next_payout_date` as a calendar date, or null when absent or malformed (manual schedule). */
private fun parsePayoutDate(value: String?): LocalDate? =
    value?.let {
        try {
            LocalDate.parse(it)
        } catch (_: DateTimeParseException) {
            null
        }
    }

/**
 * "Complete payout setup" card (D-09): one button that mints a Stripe link and opens it in a Custom Tab. While the
 * link is minting it reads "Opening secure setup…" with a spinner, and repeat taps are ignored by the ViewModel.
 */
@Composable
fun PayoutOnboardingCard(
    isBusy: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tertiary = MaterialTheme.colorScheme.tertiary
    val busyDescription = stringResource(R.string.dashboard_payout_setup_busy_a11y)
    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .testTag(DASHBOARD_PAYOUT_ONBOARDING_TAG)
                .clickable(role = Role.Button, onClick = onClick)
                .semantics { if (isBusy) stateDescription = busyDescription },
        shape = MaterialTheme.shapes.medium,
        color = tertiary.copy(alpha = CARD_FILL_ALPHA),
        border = BorderStroke(1.dp, tertiary.copy(alpha = CARD_BORDER_ALPHA)),
    ) {
        Row(
            modifier = Modifier.padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(TileSize)
                        .background(tertiary.copy(alpha = TILE_ALPHA), MaterialTheme.shapes.small),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.CreditCard,
                    contentDescription = null,
                    modifier = Modifier.size(GlyphSize),
                    tint = tertiary,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text =
                        stringResource(
                            if (isBusy) R.string.dashboard_payout_setup_busy else R.string.dashboard_payout_setup_title,
                        ),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.dashboard_payout_setup_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (isBusy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(GlyphSize),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(GlyphSize),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
