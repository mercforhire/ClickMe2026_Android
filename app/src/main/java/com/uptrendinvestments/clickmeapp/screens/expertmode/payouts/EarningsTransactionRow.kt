package com.uptrendinvestments.clickmeapp.screens.expertmode.payouts

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.model.responses.EarningsTransactionItem
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.extras
import com.uptrendinvestments.clickmeapp.util.Money
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Test tag on every [EarningsTransactionRow]. */
const val EARNINGS_ROW_TAG = "earnings_row"

private const val ROW_MIN_HEIGHT_DP = 72
private const val PILL_MIN_HEIGHT_DP = 24
private const val PILL_FILL_ALPHA = 0.12f
private const val ROW_DATE_PATTERN = "MMM d, yyyy"

/**
 * The server's `payout_status`, as the pill shows it. The wire value is a raw string on purpose: `settled_direct` is
 * the normal terminal state for a direct-charge session, and an unknown or missing value must still render
 * ("Unknown") instead of breaking the page. `settled_direct` is never [Pending].
 */
enum class EarningsStatus(
    @StringRes val label: Int,
) {
    Pending(R.string.payouts_activity_status_pending),
    Processing(R.string.payouts_activity_status_processing),
    Settled(R.string.payouts_activity_status_settled),
    Processed(R.string.payouts_activity_status_processed),
    Failed(R.string.payouts_activity_status_failed),
    Unknown(R.string.payouts_activity_status_unknown),
}

/** Which theme color tints a status pill's 12% fill. The label is always `onSurface`; the text names the status. */
enum class EarningsPillTint { Tertiary, Primary, Error, Neutral }

/** The tint role of this status: Pending warns, Settled and Processed are accent, Failed is an error. */
val EarningsStatus.tint: EarningsPillTint
    get() =
        when (this) {
            EarningsStatus.Pending -> EarningsPillTint.Tertiary
            EarningsStatus.Settled, EarningsStatus.Processed -> EarningsPillTint.Primary
            EarningsStatus.Failed -> EarningsPillTint.Error
            EarningsStatus.Processing, EarningsStatus.Unknown -> EarningsPillTint.Neutral
        }

/** Maps the raw `payout_status` (trimmed, case-insensitive) to its [EarningsStatus]; anything else is Unknown. */
fun earningsStatusOf(raw: String?): EarningsStatus =
    when (raw?.trim()?.lowercase(Locale.ROOT)) {
        "pending" -> EarningsStatus.Pending
        "processing" -> EarningsStatus.Processing
        "settled_direct" -> EarningsStatus.Settled
        "paid" -> EarningsStatus.Processed
        "failed" -> EarningsStatus.Failed
        else -> EarningsStatus.Unknown
    }

/**
 * The row's date: the session date, else when the ledger row was created (an orphaned booking has no session date),
 * formatted "MMM d, yyyy" in [zone].
 */
fun earningsRowDate(
    item: EarningsTransactionItem,
    zone: ZoneId,
    locale: Locale,
): String =
    DateTimeFormatter
        .ofPattern(ROW_DATE_PATTERN, locale)
        .withZone(zone)
        .format(item.sessionDate ?: item.createdAt)

/**
 * One earnings-ledger row, shared by the Payouts recent-activity preview and the full Activity list (iOS
 * `EarningsTransactionRow`). The row is not tappable and reads to TalkBack as one node
 * "{topic}, {client}, {date}, {status}, {amount}".
 *
 * Transactions are MINOR units: the headline amount is `Money.format(netAmount, currency)`, with no division. That is
 * unlike the request `earnings_breakdown`, which carries MAJOR units and is handled only by `requestEarnings`.
 * Every missing text field renders as "—": the backend keeps rows whose booking, topic or client cannot be resolved.
 */
@Composable
fun EarningsTransactionRow(
    item: EarningsTransactionItem,
    modifier: Modifier = Modifier,
    zone: ZoneId = ZoneId.systemDefault(),
    locale: Locale = LocalConfiguration.current.locales[0],
) {
    val none = stringResource(R.string.payouts_activity_none)
    val noneSpoken = stringResource(R.string.payouts_activity_none_a11y)
    val topicText = item.topicTitle?.takeIf { it.isNotBlank() }
    val clientText = item.clientName?.takeIf { it.isNotBlank() }
    val topic = topicText ?: none
    val client = clientText ?: none
    val date = earningsRowDate(item, zone, locale)
    val status = earningsStatusOf(item.payoutStatus)
    val amount = Money.format(item.netAmount, item.currency, locale)
    val a11y =
        stringResource(
            R.string.payouts_activity_row_a11y,
            topicText ?: noneSpoken,
            clientText ?: noneSpoken,
            date,
            stringResource(status.label),
            amount,
        )

    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .testTag(EARNINGS_ROW_TAG)
                .semantics(mergeDescendants = true) { contentDescription = a11y },
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
                    text = topic,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.payouts_activity_row_line, client, date),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.extras.onSurfaceMuted,
                )
                EarningsStatusPill(status)
            }
            Text(
                text = amount,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/** The status pill: `labelMedium` `onSurface` text on a 12% tint, never color alone. Not interactive. */
@Composable
fun EarningsStatusPill(
    status: EarningsStatus,
    modifier: Modifier = Modifier,
) {
    val fill =
        when (status.tint) {
            EarningsPillTint.Tertiary -> MaterialTheme.colorScheme.tertiary.copy(alpha = PILL_FILL_ALPHA)
            EarningsPillTint.Primary -> MaterialTheme.colorScheme.primary.copy(alpha = PILL_FILL_ALPHA)
            EarningsPillTint.Error -> MaterialTheme.colorScheme.error.copy(alpha = PILL_FILL_ALPHA)
            EarningsPillTint.Neutral -> MaterialTheme.colorScheme.surfaceContainerHigh
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

@Preview(name = "EarningsTransactionRow")
@Composable
private fun EarningsTransactionRowPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.padding(Spacing.md)) {
                EarningsTransactionRow(
                    item =
                        EarningsTransactionItem(
                            id = "e1",
                            sessionDate = Instant.parse("2026-07-30T14:00:00Z"),
                            topicTitle = "Career Advice",
                            clientName = "Liam Carter",
                            grossAmount = 7500,
                            commissionAmount = 1125,
                            netAmount = 6375,
                            currency = "CAD",
                            payoutStatus = "pending",
                            createdAt = Instant.parse("2026-07-30T10:00:00Z"),
                        ),
                )
            }
        }
    }
}
