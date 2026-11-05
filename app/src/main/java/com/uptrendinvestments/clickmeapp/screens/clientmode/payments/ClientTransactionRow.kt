package com.uptrendinvestments.clickmeapp.screens.clientmode.payments

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.model.responses.ClientTransactionItem
import com.uptrendinvestments.clickmeapp.screens.clientmode.upcomingbooking.shortBookingId
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.extras
import java.time.ZoneId
import java.util.Locale

/** Test tag on every [ClientTransactionRow]. */
const val CLIENT_TRANSACTION_ROW_TAG = "client_transaction_row"

private const val ROW_MIN_HEIGHT_DP = 72
private const val PILL_MIN_HEIGHT_DP = 24
private const val PILL_FILL_ALPHA = 0.12f

/**
 * One Payment History row (iOS `ClientTransactionRow`). The whole row is one button that reports the booking, whatever
 * its payment status (D-03), and reads to TalkBack as a single node
 * "{topic}, {expert}, {date}, {status}, {amount}" (plus ", Refunded {amount}" after a partial refund).
 *
 * Amounts are MINOR units formatted in the row's own currency. A missing topic, expert or amount shows "—" and is
 * spoken as "unavailable": the backend keeps rows it cannot fully resolve, and a null amount must never read as $0.00.
 * Text wraps with no line limit, so nothing is truncated at large font scales. Server text is plain [Text] only.
 */
@Composable
fun ClientTransactionRow(
    item: ClientTransactionItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    zone: ZoneId = ZoneId.systemDefault(),
    locale: Locale = LocalConfiguration.current.locales[0],
) {
    val none = stringResource(R.string.payouts_activity_none)
    val noneSpoken = stringResource(R.string.payouts_activity_none_a11y)
    val topicText = item.topicTitle?.takeIf { it.isNotBlank() }
    val expertText = item.expertName?.takeIf { it.isNotBlank() }
    val date = clientRowDate(item, zone, locale)
    val status = clientPaymentStatusOf(item.paymentStatus, item.amount)
    val amountText = clientAmountText(item.amount, item.currency, locale)
    val refundText = if (isPartialRefund(item)) clientAmountText(item.refundedAmount, item.currency, locale) else null
    val statusSpoken = if (status == ClientPaymentStatus.Unknown) noneSpoken else stringResource(status.label)
    val base =
        stringResource(
            R.string.payment_history_row_a11y,
            topicText ?: noneSpoken,
            expertText ?: noneSpoken,
            date,
            statusSpoken,
            amountText ?: noneSpoken,
        )
    val refundLine = refundText?.let { stringResource(R.string.payment_history_row_refunded, it) }
    val a11y = refundLine?.let { stringResource(R.string.payment_history_row_a11y_refund, base, it) } ?: base

    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .testTag(CLIENT_TRANSACTION_ROW_TAG)
                .clickable(
                    role = Role.Button,
                    onClickLabel = stringResource(R.string.payment_history_row_open),
                    onClick = onClick,
                ).semantics(mergeDescendants = true) { contentDescription = a11y },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.heightIn(min = ROW_MIN_HEIGHT_DP.dp).padding(Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Text(
                    text = topicText ?: none,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.payment_history_row_line, expertText ?: none, date),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.extras.onSurfaceMuted,
                )
                Text(
                    text = stringResource(R.string.upcoming_booking_id, shortBookingId(item.bookingId)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.extras.onSurfaceMuted,
                )
                refundLine?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.extras.onSurfaceMuted,
                    )
                }
                ClientPaymentStatusPill(status)
            }
            Text(
                text = amountText ?: none,
                style = MaterialTheme.typography.titleMedium,
                color =
                    if (amountIsMuted(item.paymentStatus)) {
                        MaterialTheme.extras.onSurfaceMuted
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
            )
        }
    }
}

/** The status pill: `labelMedium` `onSurface` text on a 12% tint, never color alone. Not interactive. */
@Composable
fun ClientPaymentStatusPill(
    status: ClientPaymentStatus,
    modifier: Modifier = Modifier,
) {
    val fill =
        when (status.tint) {
            ClientPaymentPillTint.Primary -> MaterialTheme.colorScheme.primary.copy(alpha = PILL_FILL_ALPHA)
            ClientPaymentPillTint.Tertiary -> MaterialTheme.colorScheme.tertiary.copy(alpha = PILL_FILL_ALPHA)
            ClientPaymentPillTint.Error -> MaterialTheme.colorScheme.error.copy(alpha = PILL_FILL_ALPHA)
            ClientPaymentPillTint.Neutral -> MaterialTheme.colorScheme.surfaceContainerHigh
        }
    Box(
        modifier =
            modifier
                .heightIn(min = PILL_MIN_HEIGHT_DP.dp)
                .clip(CircleShape)
                .background(fill)
                .padding(horizontal = Spacing.sm),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(status.label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
