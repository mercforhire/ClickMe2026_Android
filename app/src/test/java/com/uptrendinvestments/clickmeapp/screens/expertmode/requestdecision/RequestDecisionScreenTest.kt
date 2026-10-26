package com.uptrendinvestments.clickmeapp.screens.expertmode.requestdecision

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.uptrendinvestments.clickmeapp.screens.expertmode.incomingrequests.BookingRequestsRepository
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.bookingRequestDetail
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class RequestDecisionScreenTest {
    @get:Rule(order = 0)
    val main = MainDispatcherRule(UnconfinedTestDispatcher())

    @get:Rule(order = 1)
    val rule = createComposeRule()

    private val api = FakeClickMeApi()

    private fun show(key: RequestDecisionKey) {
        val viewModel = RequestDecisionViewModel(key, BookingRequestsRepository(api))
        rule.setContent {
            ClickMeTheme { RequestDecisionScreen(viewModel = viewModel, onBack = {}) }
        }
        rule.waitForIdle()
    }

    @Test
    fun detailRendersFromTheApi() {
        api.bookingRequestDetailHandler = {
            bookingRequestDetail(
                id = it,
                topic = "Algorithms",
                meetingType = "skype_zoom",
                message = "  Looking forward  ",
            )
        }

        show(RequestDecisionKey("r1"))

        listOf(
            "Booking Details",
            "Ada Lovelace",
            "Date & Time",
            "Topic",
            "Algorithms",
            "Meeting Type",
            "Skype/Zoom",
            "Message",
            "Looking forward",
        ).forEach { rule.onNodeWithText(it).assertExists() }
        rule.onNodeWithText("This request has expired").assertDoesNotExist()
    }

    @Test
    fun blankMessageHidesTheMessageCard() {
        api.bookingRequestDetailHandler = { bookingRequestDetail(id = it, message = "   ") }
        show(RequestDecisionKey("r1"))
        rule.onNodeWithText("Algorithms").assertExists()
        rule.onNodeWithText("Message").assertDoesNotExist()
    }

    @Test
    fun nullMessageHidesTheMessageCard() {
        api.bookingRequestDetailHandler = { bookingRequestDetail(id = it, message = null) }
        show(RequestDecisionKey("r1"))
        rule.onNodeWithText("Algorithms").assertExists()
        rule.onNodeWithText("Message").assertDoesNotExist()
    }

    @Test
    fun expiredKeyShowsBanner() {
        api.bookingRequestDetailHandler = { bookingRequestDetail(id = it) }

        show(RequestDecisionKey("r1", isExpired = true))

        rule.onNodeWithText("This request has expired").assertExists()
        rule.onNodeWithText("You can no longer accept or decline this booking.").assertExists()
    }

    @Test
    fun loadFailureShowsRetry() {
        var calls = 0
        api.bookingRequestDetailHandler = {
            calls++
            if (calls == 1) throw IOException("offline")
            bookingRequestDetail(id = it)
        }

        show(RequestDecisionKey("r1"))

        rule.onNodeWithText("Couldn't load request").assertExists()
        rule.onNodeWithText("Retry").performClick()
        rule.waitForIdle()

        assertEquals(2, calls)
        rule.onNodeWithText("Ada Lovelace").assertExists()
    }
}
