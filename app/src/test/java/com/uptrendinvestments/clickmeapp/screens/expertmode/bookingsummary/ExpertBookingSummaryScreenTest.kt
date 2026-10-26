package com.uptrendinvestments.clickmeapp.screens.expertmode.bookingsummary

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.uptrendinvestments.clickmeapp.managers.JoinClock
import com.uptrendinvestments.clickmeapp.model.responses.ConsultationFeeDto
import com.uptrendinvestments.clickmeapp.model.responses.ExpertBookingDetail
import com.uptrendinvestments.clickmeapp.screens.expertmode.upcomingbookings.ExpertBookingsRepository
import com.uptrendinvestments.clickmeapp.testing.FakeCallGuard
import com.uptrendinvestments.clickmeapp.testing.FakeCallStarter
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.FakeRealtimeConnection
import com.uptrendinvestments.clickmeapp.testing.MutableClock
import com.uptrendinvestments.clickmeapp.testing.expertBookingDetail
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
class ExpertBookingSummaryScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private val start = Instant.parse("2026-10-24T18:00:00Z")

    private val api = FakeClickMeApi()
    private val callStarter = FakeCallStarter()
    private val callGuard = FakeCallGuard()
    private val ids = mutableListOf<String>()
    private val cancels = mutableListOf<String>()

    /** The real screen over a real ViewModel, repository and fake API; the clock sits 30 minutes before [start]. */
    private fun showScreen(
        detail: ExpertBookingDetail,
        bookingId: String = "b1",
    ) {
        api.expertBookingDetailsHandler = {
            ids += it
            detail
        }
        val clock = MutableClock(start.minusSeconds(1800))
        val viewModel =
            ExpertBookingSummaryViewModel(
                ExpertBookingSummaryKey(bookingId, ExpertBookingOrigin.Bookings),
                ExpertBookingsRepository(api),
                FakeRealtimeConnection(),
                clock,
                callStarter,
                callGuard,
                JoinClock(clock, CoroutineScope(Job())),
            )
        rule.setContent {
            ClickMeTheme {
                ExpertBookingSummaryScreen(
                    viewModel = viewModel,
                    onBack = {},
                    onCancel = { cancels += bookingId },
                )
            }
        }
        rule.waitForIdle()
    }

    private fun withFee(
        detail: ExpertBookingDetail,
        fee: ConsultationFeeDto?,
    ): ExpertBookingDetail = detail.copy(session = detail.session.copy(consultationFee = fee))

    @Test
    fun summaryRendersFromDetails() {
        showScreen(expertBookingDetail(start = start, fee = 75.0, currency = "USD"))

        listOf(
            "Booking Summary",
            "Ada Lovelace",
            "Confirmed",
            "Date",
            "Time",
            "Duration",
            "1 hour",
            "Topic",
            "Algorithms",
            "Meeting Type",
            "In-App Voice",
            "Consultation Fee",
            "\$75.00",
        ).forEach { rule.onNodeWithText(it).assertExists() }
        assertEquals(listOf("b1"), ids)
    }

    @Test
    fun freeBookingShowsFree() {
        showScreen(expertBookingDetail(start = start, fee = 0.0))
        rule.onNodeWithText("Free").assertExists()
    }

    @Test
    fun noFeeAtAllShowsFree() {
        showScreen(expertBookingDetail(start = start, fee = null))
        rule.onNodeWithText("Free").assertExists()
    }

    @Test
    fun jpyFeeShowsWithoutDecimals() {
        showScreen(expertBookingDetail(start = start, fee = 7500.0, currency = "JPY"))
        rule.onNodeWithText("¥7,500").assertExists()
    }

    @Test
    fun feeWithoutCurrencyFallsBackToTheServerLabel() {
        val detail =
            withFee(
                expertBookingDetail(start = start),
                ConsultationFeeDto(amount = 75.0, currency = null, label = "\$75.00 / session"),
            )
        showScreen(detail)
        rule.onNodeWithText("\$75.00 / session").assertExists()
    }

    @Test
    fun shortIdIsFirstEightUppercase() {
        showScreen(expertBookingDetail(id = "a1b2c3d4-e5f6-7890-abcd-ef1234567890", start = start))
        rule.onNodeWithText("ID: #A1B2C3D4").assertExists()
    }
}
