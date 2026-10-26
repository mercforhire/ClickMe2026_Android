package com.uptrendinvestments.clickmeapp.screens.expertmode.requestdecision

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.uptrendinvestments.clickmeapp.model.requests.AcceptBookingRequestBody
import com.uptrendinvestments.clickmeapp.model.responses.AcceptBookingRequestData
import com.uptrendinvestments.clickmeapp.screens.expertmode.ExpertLaunchRequest
import com.uptrendinvestments.clickmeapp.screens.expertmode.confirmed.BookingConfirmedKey
import com.uptrendinvestments.clickmeapp.screens.expertmode.incomingrequests.BookingRequestsRepository
import com.uptrendinvestments.clickmeapp.screens.expertmode.requestdecision.subviews.ACCEPT_MESSAGE_FIELD_TAG
import com.uptrendinvestments.clickmeapp.screens.expertmode.requestdecision.subviews.ACCEPT_SHEET_CANCEL_TAG
import com.uptrendinvestments.clickmeapp.screens.expertmode.requestdecision.subviews.ACCEPT_SHEET_CONFIRM_TAG
import com.uptrendinvestments.clickmeapp.screens.expertmode.requestdecision.subviews.REQUEST_ACCEPT_TAG
import com.uptrendinvestments.clickmeapp.screens.expertmode.requestdecision.subviews.REQUEST_DECLINE_TAG
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.bookingRequestDetail
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import kotlinx.coroutines.CompletableDeferred
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
    private val launch = ExpertLaunchRequest()
    private val accepted = mutableListOf<BookingConfirmedKey>()
    private var backCount = 0

    private fun show(key: RequestDecisionKey) {
        val viewModel = RequestDecisionViewModel(key, BookingRequestsRepository(api), launch)
        rule.setContent {
            ClickMeTheme {
                RequestDecisionScreen(
                    viewModel = viewModel,
                    onBack = { backCount++ },
                    onAccepted = { accepted += it },
                )
            }
        }
        rule.waitForIdle()
    }

    private fun pendingRequest() {
        api.bookingRequestDetailHandler = { bookingRequestDetail(id = it, topic = "Algorithms") }
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

    @Test
    fun pendingRequestShowsTheFooter() {
        pendingRequest()
        show(RequestDecisionKey("r1"))
        rule.onNodeWithTag(REQUEST_ACCEPT_TAG).assertExists()
        rule.onNodeWithTag(REQUEST_DECLINE_TAG).assertExists()
    }

    @Test
    fun expiredHasNoFooter() {
        pendingRequest()
        show(RequestDecisionKey("r1", isExpired = true))
        rule.onNodeWithText("This request has expired").assertExists()
        rule.onNodeWithTag(REQUEST_ACCEPT_TAG).assertDoesNotExist()
        rule.onNodeWithTag(REQUEST_DECLINE_TAG).assertDoesNotExist()
    }

    @Test
    fun acceptSheetOpensAndCancelCloses() {
        pendingRequest()
        show(RequestDecisionKey("r1"))

        rule.onNodeWithTag(REQUEST_ACCEPT_TAG).performClick()

        rule.onNodeWithText("Confirm Booking").assertExists()
        rule.onNodeWithText("Add a message (optional)").assertExists()
        rule.onNodeWithText("Accept Request").assertExists()

        rule.onNodeWithTag(ACCEPT_SHEET_CANCEL_TAG).performClick()
        rule.waitForIdle()

        rule.onNodeWithText("Confirm Booking").assertDoesNotExist()
        assertEquals(0, api.acceptBookingRequestCalls)
    }

    @Test
    fun acceptSendsTheMessageAndReplacesTheScreen() {
        pendingRequest()
        api.acceptBookingRequestHandler = { _, _ -> AcceptBookingRequestData(bookingId = "b9") }
        show(RequestDecisionKey("r1"))

        rule.onNodeWithTag(REQUEST_ACCEPT_TAG).performClick()
        rule.onNodeWithTag(ACCEPT_MESSAGE_FIELD_TAG).performTextInput("  See you soon  ")
        rule.onNodeWithTag(ACCEPT_SHEET_CONFIRM_TAG).performClick()
        rule.waitForIdle()

        assertEquals(listOf("r1" to AcceptBookingRequestBody("See you soon")), api.acceptBookingRequestRequests)
        assertEquals(listOf("b9"), accepted.map { it.bookingId })
        rule.onNodeWithText("Confirm Booking").assertDoesNotExist()
    }

    @Test
    fun acceptWithBlankMessageSendsNoMessage() {
        pendingRequest()
        api.acceptBookingRequestHandler = { _, _ -> AcceptBookingRequestData(bookingId = "b9") }
        show(RequestDecisionKey("r1"))

        rule.onNodeWithTag(REQUEST_ACCEPT_TAG).performClick()
        rule.onNodeWithTag(ACCEPT_MESSAGE_FIELD_TAG).performTextInput("   ")
        rule.onNodeWithTag(ACCEPT_SHEET_CONFIRM_TAG).performClick()
        rule.waitForIdle()

        assertEquals(listOf("r1" to AcceptBookingRequestBody(null)), api.acceptBookingRequestRequests)
    }

    @Test
    fun counterAppearsFrom800AndInputIsCappedAt1000() {
        pendingRequest()
        show(RequestDecisionKey("r1"))
        rule.onNodeWithTag(REQUEST_ACCEPT_TAG).performClick()

        rule.onNodeWithTag(ACCEPT_MESSAGE_FIELD_TAG).performTextInput("a".repeat(799))
        rule.onNodeWithText("799/1000").assertDoesNotExist()

        rule.onNodeWithTag(ACCEPT_MESSAGE_FIELD_TAG).performTextInput("b")
        rule.onNodeWithText("800/1000").assertExists()

        rule.onNodeWithTag(ACCEPT_MESSAGE_FIELD_TAG).performTextInput("c".repeat(500))
        rule.onNodeWithText("1000/1000").assertExists()
    }

    @Test
    fun theFooterIsDisabledWhileAcceptIsInFlight() {
        pendingRequest()
        val gate = CompletableDeferred<AcceptBookingRequestData>()
        api.acceptBookingRequestHandler = { _, _ -> gate.await() }
        show(RequestDecisionKey("r1"))

        rule.onNodeWithTag(REQUEST_ACCEPT_TAG).performClick()
        rule.onNodeWithTag(ACCEPT_SHEET_CONFIRM_TAG).performClick()
        rule.waitForIdle()

        rule.onNodeWithTag(REQUEST_ACCEPT_TAG).assertIsNotEnabled()
        rule.onNodeWithTag(REQUEST_DECLINE_TAG).assertIsNotEnabled()
        rule.onNodeWithTag(ACCEPT_SHEET_CONFIRM_TAG).assertIsNotEnabled()
        rule.onNodeWithTag(ACCEPT_SHEET_CANCEL_TAG).assertIsNotEnabled()
        assertEquals(1, api.acceptBookingRequestCalls)
    }
}
