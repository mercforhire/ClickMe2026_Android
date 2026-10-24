package com.uptrendinvestments.clickmeapp.screens.clientmode.upcomingbooking.subviews

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Undo
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.color
import com.uptrendinvestments.clickmeapp.components.labelText
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.BookingStatusDisplay
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.PaymentPill
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.detailStatusDisplay
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.extras

/** Test tag on the status pill of Upcoming Booking Details. */
const val UPCOMING_BOOKING_STATUS_TAG = "upcoming_booking_status"

/** Test tag on the payment pill (paid bookings only). */
const val UPCOMING_BOOKING_PAYMENT_PILL_TAG = "upcoming_booking_payment_pill"

private val StatusDotSize = 8.dp
private val PaymentIconSize = 16.dp

/**
 * Block 2 of Upcoming Booking Details: the real status pill at the start and "ID: #XXXXXXXX" at the end (D-14; iOS
 * hardcodes "Confirmed"). The wording carries the meaning, not only the tone colour.
 */
@Composable
fun UpcomingBookingStatusRow(
    status: BookingStatusDisplay,
    shortId: String,
    modifier: Modifier = Modifier,
) {
    val tone = status.tone.color()
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.testTag(UPCOMING_BOOKING_STATUS_TAG),
            shape = CircleShape,
            color = tone.copy(alpha = 0.1f),
            border = BorderStroke(1.dp, tone.copy(alpha = 0.3f)),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(Modifier.size(StatusDotSize).background(tone, CircleShape))
                Spacer(Modifier.width(Spacing.sm))
                Text(
                    text = status.labelText(),
                    style = MaterialTheme.typography.labelMedium,
                    color = tone,
                )
            }
        }
        Text(
            text = stringResource(R.string.upcoming_booking_id, shortId),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.extras.onSurfaceMuted,
        )
    }
}

/**
 * Money-state pill for a paid booking (D-15): card hold placed, paid or refunded. Static, never clickable. Free and
 * unknown money states pass no pill and nothing renders.
 */
@Composable
fun UpcomingBookingPaymentPill(
    pill: PaymentPill,
    modifier: Modifier = Modifier,
) {
    val (icon, text, color) = pill.visuals()
    Surface(
        modifier = modifier.testTag(UPCOMING_BOOKING_PAYMENT_PILL_TAG),
        shape = CircleShape,
        color = color.copy(alpha = 0.12f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(PaymentIconSize), tint = color)
            Spacer(Modifier.width(Spacing.xs))
            Text(text = text, style = MaterialTheme.typography.labelMedium, color = color)
        }
    }
}

@Composable
private fun PaymentPill.visuals(): Triple<ImageVector, String, Color> =
    when (this) {
        PaymentPill.HoldPlaced ->
            Triple(
                Icons.Outlined.Lock,
                stringResource(R.string.upcoming_booking_payment_held),
                MaterialTheme.colorScheme.tertiary,
            )
        PaymentPill.Paid ->
            Triple(
                Icons.Outlined.Verified,
                stringResource(R.string.upcoming_booking_payment_paid),
                MaterialTheme.colorScheme.primary,
            )
        PaymentPill.Refunded ->
            Triple(
                Icons.Outlined.Undo,
                stringResource(R.string.upcoming_booking_payment_refunded),
                MaterialTheme.colorScheme.onSurfaceVariant,
            )
    }

@Preview(name = "UpcomingBookingStatusRow")
@Composable
private fun UpcomingBookingStatusRowPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            UpcomingBookingStatusRow(detailStatusDisplay("confirmed"), "#AB12CD34", Modifier.padding(Spacing.md))
        }
    }
}
