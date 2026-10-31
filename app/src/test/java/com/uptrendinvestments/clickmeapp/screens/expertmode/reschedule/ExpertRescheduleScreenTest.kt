package com.uptrendinvestments.clickmeapp.screens.expertmode.reschedule

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.SavedStateHandle
import com.uptrendinvestments.clickmeapp.managers.CurrentUserIdProvider
import com.uptrendinvestments.clickmeapp.model.responses.RescheduleResponseData
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.BookingRepository
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.availabilityOf
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.slotDto
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.subviews.calendarDayTestTag
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.subviews.slotChipTestTag
import com.uptrendinvestments.clickmeapp.screens.clientmode.reschedule.RESCHEDULE_DAY_TAG_PREFIX
import com.uptrendinvestments.clickmeapp.screens.clientmode.reschedule.RESCHEDULE_PAID_NOTE_TAG
import com.uptrendinvestments.clickmeapp.screens.clientmode.reschedule.RESCHEDULE_SLOT_TAG_PREFIX
import com.uptrendinvestments.clickmeapp.screens.clientmode.reschedule.RESCHEDULE_SUBMIT_TAG
import com.uptrendinvestments.clickmeapp.screens.clientmode.reschedule.RescheduleOpener
import com.uptrendinvestments.clickmeapp.screens.clientmode.reschedule.RescheduleSentRequest
import com.uptrendinvestments.clickmeapp.screens.expertmode.bookingsummary.ExpertBookingOrigin
import com.uptrendinvestments.clickmeapp.screens.expertmode.upcomingbookings.ExpertBookingsRepository
import com.uptrendinvestments.clickmeapp.testing.FakeCallGuard
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.MutableClock
import com.uptrendinvestments.clickmeapp.testing.expertBookingDetail
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate
import java.util.TimeZone

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h3000dp")
class ExpertRescheduleScreenTest {
    @get:Rule(order = 0)
    val main = MainDispatcherRule(UnconfinedTestDispatcher())

    @get:Rule(order = 1)
    val rule = createComposeRule()

    private val api = FakeClickMeApi()
    private val clock = MutableClock(Instant.parse("2026-11-02T13:00:00Z"))
    private val slotNov5 = Instant.parse("2026-11-05T16:00:00Z")
    private lateinit var previousZone: TimeZone
    private var backCount = 0

    @Before
    fun setUp() {
        previousZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("America/Toronto"))
        val start = Instant.parse("2026-11-06T15:00:00Z")
        api.expertBookingDetailsHandler = { expertBookingDetail(start = start, fee = 0.0) }
        api.expertAvailabilityHandler = { _, _ -> availabilityOf(slotDto("2026-11-05T16:00:00Z")) }
        api.rescheduleExpertBookingHandler = { _, _ -> RescheduleResponseData(bookingId = "b1", status = "pending") }
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(previousZone)
    }

    private fun show() {
        val viewModel =
            ExpertRescheduleViewModel(
                ExpertRescheduleKey("b1", ExpertBookingOrigin.Bookings, RescheduleOpener.ExpertSummary),
                SavedStateHandle(),
                ExpertBookingsRepository(api),
                BookingRepository(api),
                ExpertRescheduleProposer(ExpertBookingsRepository(api)),
                RescheduleSentRequest(),
                FakeCallGuard(),
                CurrentUserIdProvider { "x1" },
                clock,
            )
        rule.setContent {
            ClickMeTheme {
                ExpertRescheduleScreen(viewModel = viewModel, onBack = { backCount++ })
            }
        }
        rule.waitForIdle()
    }

    private fun chooseSlotAndSubmit() {
        rule.onNodeWithTag(calendarDayTestTag(LocalDate.of(2026, 11, 5), RESCHEDULE_DAY_TAG_PREFIX)).performClick()
        rule.onNodeWithTag(slotChipTestTag(slotNov5, RESCHEDULE_SLOT_TAG_PREFIX)).performClick()
        rule.onNodeWithTag(RESCHEDULE_SUBMIT_TAG).performClick()
        rule.waitForIdle()
    }

    @Test
    fun selectSubmitPopsOnce() {
        show()
        rule.onNodeWithText("Let your client know why you'd like to move this session.").assertExists()
        rule.onNodeWithTag(RESCHEDULE_PAID_NOTE_TAG).assertDoesNotExist()

        chooseSlotAndSubmit()

        assertEquals(1, backCount)
        assertEquals(1, api.rescheduleExpertBookingCalls)
    }

    @Test
    fun noPaymentDialogsForExpert() {
        api.rescheduleExpertBookingHandler = { _, _ ->
            throw api.httpError(402, """{"code":"PAYMENT_METHOD_REQUIRED","message":"Server payment text"}""")
        }
        show()

        chooseSlotAndSubmit()

        rule.onNodeWithText("Couldn't reschedule booking").assertExists()
        rule.onNodeWithText("Server payment text").assertExists()
        rule.onNodeWithText("Add a payment method", substring = true).assertDoesNotExist()
        assertEquals(0, backCount)
        assertEquals(0, api.getPaymentMethodsCalls)
    }
}
