package com.uptrendinvestments.clickmeapp.screens

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.pastStatusDisplay
import com.uptrendinvestments.clickmeapp.screens.clientmode.payments.ClientTransactionRow
import com.uptrendinvestments.clickmeapp.screens.expertmode.upcomingbookings.ExpertPastRow
import com.uptrendinvestments.clickmeapp.screens.expertmode.upcomingbookings.subviews.ExpertPastCard
import com.uptrendinvestments.clickmeapp.screens.expertmode.upcomingbookings.subviews.ExpertUpcomingCard
import com.uptrendinvestments.clickmeapp.testing.clientTransaction
import com.uptrendinvestments.clickmeapp.testing.expertBookingItem
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale

/**
 * BUG-29: Payment History rows and the expert bookings list rows did not show the booking ID, so a booking could only
 * be found by opening each row; only Booking Summary showed it. Every row now carries "ID: #" and the first eight
 * characters of the booking id, upper-cased, the same form as Booking Summary and Upcoming Booking Details.
 */
@RunWith(RobolectricTestRunner::class)
class BookingIdOnListRowsRegressionTest {
    @get:Rule
    val rule = createComposeRule()

    private val bookingId = "a1b2c3d4-e5f6-4789-a012-b3c4d5e6f708"
    private val expectedId = "ID: #A1B2C3D4"
    private val now = Instant.parse("2026-10-23T12:00:00Z")

    @Test
    fun paymentHistoryRowShowsTheBookingId() {
        rule.setContent {
            ClickMeTheme {
                ClientTransactionRow(
                    item = clientTransaction(bookingId = bookingId),
                    onClick = {},
                    zone = ZoneOffset.UTC,
                    locale = Locale.US,
                )
            }
        }
        rule.onNodeWithText(expectedId, useUnmergedTree = true).assertExists()
    }

    @Test
    fun expertUpcomingCardShowsTheBookingId() {
        rule.setContent {
            ClickMeTheme {
                ExpertUpcomingCard(
                    item = expertBookingItem(id = bookingId, start = now.plusSeconds(7200)),
                    now = now,
                    onOpen = {},
                    onJoin = {},
                    messageBusy = false,
                    onMessage = {},
                    onPrivateNote = {},
                    zone = ZoneOffset.UTC,
                    locale = Locale.US,
                )
            }
        }
        rule.onNodeWithText(expectedId).assertExists()
    }

    @Test
    fun expertPastCardShowsTheBookingId() {
        val row =
            ExpertPastRow(
                item = expertBookingItem(id = bookingId, statusCode = "completed", start = now.minusSeconds(86400)),
                status = requireNotNull(pastStatusDisplay("completed")),
            )
        rule.setContent {
            ClickMeTheme {
                ExpertPastCard(row = row, onOpen = {}, onPrivateNote = {}, zone = ZoneOffset.UTC, locale = Locale.US)
            }
        }
        rule.onNodeWithText(expectedId).assertExists()
    }
}
