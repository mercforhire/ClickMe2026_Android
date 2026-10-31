package com.uptrendinvestments.clickmeapp.screens.clientmode.reschedule

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.SavedStateHandle
import com.uptrendinvestments.clickmeapp.model.responses.PaymentMethodsData
import com.uptrendinvestments.clickmeapp.model.responses.RescheduleResponseData
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.BookingRepository
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.availabilityOf
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.slotDto
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.subviews.calendarDayTestTag
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.subviews.slotChipTestTag
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.ClientBookingsRepository
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.bookingDetail
import com.uptrendinvestments.clickmeapp.screens.settings.paymentmethods.PaymentMethodsRepository
import com.uptrendinvestments.clickmeapp.testing.FakeCallGuard
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.MutableClock
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
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

/**
 * The client picker route with the real [RescheduleViewModel] over [FakeClickMeApi] (Phase 16.2 plan 07): the
 * select-and-submit tracer, the saved-card recovery that opens Payment Methods without resending (D-08), and the
 * leave-after-OK pop.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h3000dp")
class RescheduleScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private val api = FakeClickMeApi()
    private val sentRequest = RescheduleSentRequest()
    private val bookingStart = Instant.parse("2026-11-06T15:00:00Z")
    private val slotNov5 = Instant.parse("2026-11-05T16:00:00Z")
    private val nov5 = LocalDate.of(2026, 11, 5)
    private var backCount = 0
    private var paymentMethodsCount = 0
    private lateinit var previousZone: TimeZone

    @Before
    fun setUp() {
        previousZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("America/Toronto"))
        api.clientBookingDetailHandler = { bookingDetail(start = bookingStart) }
        api.expertAvailabilityHandler = { _, _ ->
            availabilityOf(slotDto("2026-11-05T16:00:00Z"), slotDto("2026-11-06T15:00:00Z"))
        }
        api.rescheduleClientBookingHandler = { _, _ -> RescheduleResponseData(bookingId = "b1", status = "pending") }
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(previousZone)
    }

    private fun show(opener: RescheduleOpener = RescheduleOpener.UpcomingDetails) {
        val bookings = ClientBookingsRepository(api)
        // Built before setContent (lint: ViewModelConstructorInComposable).
        val viewModel =
            RescheduleViewModel(
                RescheduleKey("b1", opener),
                SavedStateHandle(),
                bookings,
                BookingRepository(api),
                ClientRescheduleProposer(bookings, PaymentMethodsRepository(api)),
                sentRequest,
                FakeCallGuard(),
                MutableClock(PickerNow),
            )
        rule.setContent {
            ClickMeTheme {
                RescheduleScreen(
                    viewModel = viewModel,
                    onBack = { backCount++ },
                    onOpenPaymentMethods = { paymentMethodsCount++ },
                )
            }
        }
    }

    /** Picks Nov 5 and its 16:00Z chip once the booking and the months have loaded. */
    private fun chooseNov5Slot() {
        val dayTag = calendarDayTestTag(nov5, RESCHEDULE_DAY_TAG_PREFIX)
        rule.waitUntil { runCatching { rule.onNodeWithTag(dayTag).assertIsEnabled() }.isSuccess }
        rule.onNodeWithTag(dayTag).performClick()
        val chipTag = slotChipTestTag(slotNov5, RESCHEDULE_SLOT_TAG_PREFIX)
        rule.waitUntil { runCatching { rule.onNodeWithTag(chipTag).assertIsEnabled() }.isSuccess }
        rule.onNodeWithTag(chipTag).performClick()
    }

    @Test
    fun selectSubmitPopsOnce() {
        show()
        chooseNov5Slot()
        rule.onNodeWithTag(RESCHEDULE_SUBMIT_TAG).performClick()
        rule.waitUntil { backCount > 0 }
        rule.waitForIdle()

        assertEquals(1, backCount)
        assertEquals(1, api.rescheduleClientBookingCalls)
        assertEquals(RescheduleSent("b1"), sentRequest.upcomingDetails.pending.value)
    }

    @Test
    fun addPaymentMethodNavigatesWithoutResending() {
        api.rescheduleClientBookingHandler = { _, _ ->
            throw api.httpError(422, """{"code":"PAYMENT_METHOD_REQUIRED","message":"Add a card"}""")
        }
        api.getPaymentMethodsHandler = { PaymentMethodsData(emptyList()) }
        show()
        chooseNov5Slot()
        rule.onNodeWithTag(RESCHEDULE_SUBMIT_TAG).performClick()
        rule.waitUntil { runCatching { rule.onNodeWithText("Add a Payment Method").assertExists() }.isSuccess }

        rule.onNodeWithText("Add a Payment Method").performClick()
        rule.waitForIdle()

        assertEquals(1, paymentMethodsCount)
        assertEquals(0, backCount)
        assertEquals(1, api.rescheduleClientBookingCalls)
        rule.onNodeWithText("Add a Payment Method").assertDoesNotExist()
        rule.onNodeWithTag(RESCHEDULE_SUBMIT_TAG).assertIsEnabled()
    }

    @Test
    fun leaveAfterInvalidStatus() {
        api.rescheduleClientBookingHandler = { _, _ ->
            throw api.httpError(409, """{"code":"INVALID_STATUS","message":"Not reschedulable"}""")
        }
        show()
        chooseNov5Slot()
        rule.onNodeWithTag(RESCHEDULE_SUBMIT_TAG).performClick()
        rule.waitUntil { runCatching { rule.onNodeWithText("OK").assertExists() }.isSuccess }
        assertEquals(0, backCount)

        rule.onNodeWithText("OK").performClick()
        rule.waitUntil { backCount > 0 }
        rule.waitForIdle()

        assertEquals(1, backCount)
        assertEquals(1, api.rescheduleClientBookingCalls)
    }
}
