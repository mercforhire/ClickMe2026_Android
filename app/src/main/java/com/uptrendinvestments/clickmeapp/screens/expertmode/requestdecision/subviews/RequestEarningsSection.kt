package com.uptrendinvestments.clickmeapp.screens.expertmode.requestdecision.subviews

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.screens.expertmode.requestdecision.RequestEarningsUi
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing

/** Test tag on the earnings card (receipt, price pending or free). */
const val REQUEST_EARNINGS_TAG = "request_earnings"

private val PendingIcon = 16.dp
private val CardBorderWidth = 1.dp
private const val RECEIPT_FILL_ALPHA = 0.12f
private const val RECEIPT_BORDER_ALPHA = 0.28f

/**
 * "Your Earnings": one card chosen by [earnings]. The receipt is the only `primary`-tinted card on the screen. The
 * fee line is plain `onSurfaceVariant`, not `error`, because a platform fee is not a failure. The price-pending and
 * free cards never use an earnings label for the client's price (EXP-06, EXPERT-EARNINGS-BREAKDOWN 3).
 */
@Composable
fun RequestEarningsSection(
    earnings: RequestEarningsUi,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(
            text = stringResource(R.string.request_decision_your_earnings),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        when (earnings) {
            is RequestEarningsUi.Receipt -> ReceiptCard(earnings)
            is RequestEarningsUi.PricePending -> PricePendingCard(earnings.sessionPrice)
            RequestEarningsUi.Free -> FreeCard()
        }
    }
}

@Composable
private fun ReceiptCard(receipt: RequestEarningsUi.Receipt) {
    val primary = MaterialTheme.colorScheme.primary
    Surface(
        modifier = Modifier.fillMaxWidth().testTag(REQUEST_EARNINGS_TAG),
        shape = MaterialTheme.shapes.medium,
        color = primary.copy(alpha = RECEIPT_FILL_ALPHA),
        border = BorderStroke(CardBorderWidth, primary.copy(alpha = RECEIPT_BORDER_ALPHA)),
    ) {
        Column(modifier = Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            AmountRow(
                label = stringResource(R.string.request_decision_client_pays),
                amount = receipt.clientPays,
                amountColor = MaterialTheme.colorScheme.onSurface,
            )
            val feeLabel =
                receipt.commissionPctText?.let { stringResource(R.string.request_decision_platform_fee_pct, it) }
                    ?: stringResource(R.string.request_decision_platform_fee)
            AmountRow(
                label = feeLabel,
                amount = stringResource(R.string.request_decision_fee_amount, receipt.platformFee),
                amountColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                Text(
                    text = stringResource(R.string.request_decision_you_earn),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text =
                        if (receipt.estimated) {
                            stringResource(R.string.request_decision_estimated_net, receipt.net)
                        } else {
                            receipt.net
                        },
                    style = MaterialTheme.typography.titleLarge,
                    color = primary,
                    textAlign = TextAlign.End,
                )
            }
            if (receipt.estimated) {
                Text(
                    text = stringResource(R.string.request_decision_estimated_caption),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun AmountRow(
    label: String,
    amount: String,
    amountColor: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = amount,
            style = MaterialTheme.typography.bodyLarge,
            color = amountColor,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun PricePendingCard(sessionPrice: String) {
    NeutralCard {
        SessionPriceRow(amount = sessionPrice, amountColor = MaterialTheme.colorScheme.onSurface)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.Schedule,
                contentDescription = null,
                modifier = Modifier.size(PendingIcon),
                tint = MaterialTheme.colorScheme.tertiary,
            )
            Spacer(Modifier.width(Spacing.sm))
            Text(
                text = stringResource(R.string.request_decision_payout_pending),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FreeCard() {
    NeutralCard {
        SessionPriceRow(amount = stringResource(R.string.price_free), amountColor = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun SessionPriceRow(
    amount: String,
    amountColor: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Text(
            text = stringResource(R.string.request_decision_session_price),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = amount,
            style = MaterialTheme.typography.titleLarge,
            color = amountColor,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun NeutralCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().testTag(REQUEST_EARNINGS_TAG),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(CardBorderWidth, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            content()
        }
    }
}
