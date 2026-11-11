package com.uptrendinvestments.clickmeapp.screens.clientmode.bookingsummary.subviews

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.BookingStatusBadge
import com.uptrendinvestments.clickmeapp.components.PriceText
import com.uptrendinvestments.clickmeapp.components.avatar.Avatar
import com.uptrendinvestments.clickmeapp.screens.clientmode.bookingsummary.BookAgainSeed
import com.uptrendinvestments.clickmeapp.screens.clientmode.bookingsummary.BookingSummaryDetails
import com.uptrendinvestments.clickmeapp.screens.clientmode.bookingsummary.SummaryPriceLabel
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.formatShortDate
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.rememberDeviceTimeFormat
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.formatSessionTimeRange
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.summaryStatusDisplay
import com.uptrendinvestments.clickmeapp.screens.clientmode.upcomingbooking.shortBookingId
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.extras
import com.uptrendinvestments.clickmeapp.util.PriceDisplay
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

/** Test tag on the Book Again button (the button itself lives in the screen, below this card). */
const val BOOKING_SUMMARY_BOOK_AGAIN_TAG = "booking_summary_book_again"

private val AvatarSize = 64.dp

/**
 * The summary card of Booking Summary (D-13): expert, status badge, topic, "{date}, {time range}" and the price
 * under the label that matches the payment (Price paid, Refunded or Session fee), then the short booking ID (BOOK-20,
 * the same "ID: #…" line as Upcoming details and the iOS summary). Times use [zone] and [timeFormat] so the viewer's
 * zone and 12/24 hour setting win. There is no note, review or feedback here (D-16).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BookingSummaryCard(
    details: BookingSummaryDetails,
    modifier: Modifier = Modifier,
    zone: ZoneId = ZoneId.systemDefault(),
    timeFormat: java.text.DateFormat = rememberDeviceTimeFormat(),
    locale: Locale = LocalConfiguration.current.locales[0],
) {
    val dateTime =
        stringResource(
            R.string.booking_summary_date_time,
            formatShortDate(details.start.atZone(zone).toLocalDate(), locale),
            formatSessionTimeRange(details.start, details.end, zone, timeFormat),
        )
    val priceLabel =
        stringResource(
            when (details.priceLabel) {
                SummaryPriceLabel.PricePaid -> R.string.booking_summary_price_paid
                SummaryPriceLabel.SessionFee -> R.string.booking_summary_session_fee
                SummaryPriceLabel.Refunded -> R.string.booking_summary_refunded
            },
        )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column {
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(Spacing.md),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Avatar(url = details.expertAvatarUrl, contentDescription = null, size = AvatarSize)
                    Column(modifier = Modifier.padding(start = Spacing.md)) {
                        Text(
                            text = details.expertName ?: stringResource(R.string.explore_expert_name_fallback),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        details.expertTitle?.let { title ->
                            Text(
                                text = title,
                                modifier = Modifier.padding(top = Spacing.xs),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.extras.onSurfaceMuted,
                            )
                        }
                    }
                }
                BookingStatusBadge(details.status)
            }
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = Spacing.md),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            Column(
                modifier = Modifier.fillMaxWidth().padding(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Text(
                    text = stringResource(R.string.booking_summary_topic),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.extras.onSurfaceMuted,
                )
                Text(
                    text = details.topicTitle ?: stringResource(R.string.explore_session_topic_fallback),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = dateTime,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = priceLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.extras.onSurfaceMuted,
                    )
                    PriceText(details.price)
                }
                Text(
                    text = stringResource(R.string.upcoming_booking_id, shortBookingId(details.bookingId)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.extras.onSurfaceMuted,
                )
            }
        }
    }
}

@Preview(name = "BookingSummaryCard")
@Composable
private fun BookingSummaryCardPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            BookingSummaryCard(
                details =
                    BookingSummaryDetails(
                        bookingId = "b1",
                        status = summaryStatusDisplay("completed"),
                        expertName = "Ada Lovelace",
                        expertTitle = "Engineer",
                        expertAvatarUrl = null,
                        topicTitle = "Algorithms",
                        start = Instant.parse("2026-10-20T18:00:00Z"),
                        end = Instant.parse("2026-10-20T19:00:00Z"),
                        price = PriceDisplay.Paid("$50.00"),
                        priceLabel = SummaryPriceLabel.PricePaid,
                        bookAgain = BookAgainSeed("e1", "Ada Lovelace", "Engineer", null),
                        reschedule = null,
                    ),
            )
        }
    }
}
