package com.uptrendinvestments.clickmeapp.screens.expertmode.requestdecision

import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.uptrendinvestments.clickmeapp.model.requests.AcceptBookingRequestBody
import com.uptrendinvestments.clickmeapp.model.requests.DeclineBookingRequestBody
import com.uptrendinvestments.clickmeapp.model.responses.AcceptBookingRequestData
import com.uptrendinvestments.clickmeapp.model.responses.DeclineBookingRequestData
import com.uptrendinvestments.clickmeapp.screens.expertmode.ExpertLaunchRequest
import com.uptrendinvestments.clickmeapp.screens.expertmode.IncomingRequestsLaunch
import com.uptrendinvestments.clickmeapp.screens.expertmode.IncomingRequestsNotice
import com.uptrendinvestments.clickmeapp.screens.expertmode.confirmed.BookingConfirmedKey
import com.uptrendinvestments.clickmeapp.screens.expertmode.incomingrequests.BookingRequestsRepository
import com.uptrendinvestments.clickmeapp.screens.expertmode.requestdecision.subviews.ACCEPT_MESSAGE_FIELD_TAG
import com.uptrendinvestments.clickmeapp.screens.expertmode.requestdecision.subviews.ACCEPT_SHEET_CANCEL_TAG
import com.uptrendinvestments.clickmeapp.screens.expertmode.requestdecision.subviews.ACCEPT_SHEET_CONFIRM_TAG
import com.uptrendinvestments.clickmeapp.screens.expertmode.requestdecision.subviews.DECLINE_NOTE_FIELD_TAG
import com.uptrendinvestments.clickmeapp.screens.expertmode.requestdecision.subviews.DECLINE_REASON_ROW_TAG_PREFIX
import com.uptrendinvestments.clickmeapp.screens.expertmode.requestdecision.subviews.DECLINE_SHEET_CONFIRM_TAG
import com.uptrendinvestments.clickmeapp.screens.expertmode.requestdecision.subviews.DECLINE_SHEET_KEEP_TAG
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
import java.time.Instant

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
    private var declinedCount = 0
    private var exitCount = 0

    private fun show(key: RequestDecisionKey) {
        val viewModel = RequestDecisionViewModel(key, BookingRequestsRepository(api), launch)
        rule.setContent {
            ClickMeTheme {
                RequestDecisionScreen(
                    viewModel = viewModel,
                    onBack = { backCount++ },
                    onAccepted = { accepted += it },
                    onDeclined = { declinedCount++ },
                    onExitAfterProcessed = { exitCount++ },
                )
            }
        }
        rule.waitForIdle()
    }

    private val details =
        RequestDecisionDetails(
            requestId = "r1",
            clientName = "Ada Lovelace",
            clientAvatarUrl = null,
            start = Instant.parse("2026-10-24T18:00:00Z"),
            end = Instant.parse("2026-10-24T19:00:00Z"),
            topic = "Algorithms",
            meetingType = "in_app_voice",
            clientMessage = null,
            earnings = RequestEarningsUi.Free,
        )

    /** Renders [state] under an outer Back handler like NavDisplay's and returns how often it fired on system Back. */
    private fun systemBackReachesNavigation(state: RequestDecisionUiState): Int {
        var navBacks = 0
        var dispatcher: OnBackPressedDispatcher? = null
        rule.setContent {
            ClickMeTheme {
                dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
                BackHandler { navBacks++ }
                RequestDecisionContent(
                    state = state,
                    onBack = { backCount++ },
                    onRetry = {},
                    onAccept = {},
                    onDecline = { _, _ -> },
                    onDismissDialog = {},
                )
            }
        }
        rule.runOnIdle { checkNotNull(dispatcher).onBackPressed() }
        rule.waitForIdle()
        return navBacks
    }

    private fun failAccept(
        status: Int,
        code: String,
        message: String,
    ) {
        api.acceptBookingRequestHandler = { _, _ ->
            throw api.httpError(status, """{"status":"error","code":"$code","message":"$message"}""")
        }
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
    fun declineDisabledUntilReasonChosen() {
        pendingRequest()
        show(RequestDecisionKey("r1"))
        rule.onNodeWithTag(REQUEST_DECLINE_TAG).performClick()

        rule.onNodeWithText("Decline Booking Request?").assertExists()
        rule.onNodeWithText("This action cannot be undone.").assertExists()
        DeclineReason.entries.forEach {
            rule
                .onNodeWithTag(
                    DECLINE_REASON_ROW_TAG_PREFIX + it.name,
                ).assertIsNotSelected()
        }
        rule
            .onNodeWithTag(DECLINE_SHEET_CONFIRM_TAG)
            .assertIsNotEnabled()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Choose a reason first"))

        rule
            .onNodeWithTag(
                DECLINE_REASON_ROW_TAG_PREFIX + DeclineReason.NotAvailable.name,
            ).performScrollTo()
            .performClick()

        rule.onNodeWithTag(DECLINE_REASON_ROW_TAG_PREFIX + DeclineReason.NotAvailable.name).assertIsSelected()
        rule.onNodeWithTag(DECLINE_SHEET_CONFIRM_TAG).assertIsEnabled()
    }

    @Test
    fun theFourReasonsReadInOrder() {
        pendingRequest()
        show(RequestDecisionKey("r1"))
        rule.onNodeWithTag(REQUEST_DECLINE_TAG).performClick()

        val labels = listOf("Schedule conflict", "Outside my expertise", "Not available at that time", "Other reason")
        DeclineReason.entries.zip(labels).forEach { (reason, label) ->
            rule
                .onNodeWithTag(DECLINE_REASON_ROW_TAG_PREFIX + reason.name)
                .assertTextContains(label)
        }
        rule.onNodeWithText("Reason for declining *").assertExists()
        rule.onNodeWithText("Optional note to the client").assertExists()
    }

    @Test
    fun keepRequestClosesTheSheetWithoutSending() {
        pendingRequest()
        show(RequestDecisionKey("r1"))
        rule.onNodeWithTag(REQUEST_DECLINE_TAG).performClick()

        rule.onNodeWithTag(DECLINE_SHEET_KEEP_TAG).performScrollTo().performClick()
        rule.waitForIdle()

        rule.onNodeWithText("Decline Booking Request?").assertDoesNotExist()
        assertEquals(0, api.declineBookingRequestCalls)
    }

    @Test
    fun declineSuccessCallsOnDeclined() {
        pendingRequest()
        api.declineBookingRequestHandler = { id, _ -> DeclineBookingRequestData(requestId = id) }
        show(RequestDecisionKey("r1"))

        rule.onNodeWithTag(REQUEST_DECLINE_TAG).performClick()
        rule
            .onNodeWithTag(
                DECLINE_REASON_ROW_TAG_PREFIX + DeclineReason.ScheduleConflict.name,
            ).performScrollTo()
            .performClick()
        rule.onNodeWithTag(DECLINE_SHEET_CONFIRM_TAG).performScrollTo().performClick()
        rule.waitForIdle()

        assertEquals(
            listOf("r1" to DeclineBookingRequestBody("CONFLICT", "Schedule conflict")),
            api.declineBookingRequestRequests,
        )
        assertEquals(1, declinedCount)
        assertEquals(0, accepted.size)
        assertEquals(
            IncomingRequestsLaunch(IncomingRequestsNotice.Declined),
            launch.incomingRequests.pending.value,
        )
    }

    @Test
    fun declineNoteCounterAppearsFrom800AndIsCapped() {
        pendingRequest()
        show(RequestDecisionKey("r1"))
        rule.onNodeWithTag(REQUEST_DECLINE_TAG).performClick()

        rule.onNodeWithTag(DECLINE_NOTE_FIELD_TAG).performTextInput("a".repeat(800))
        rule.onNodeWithText("800/1000").assertExists()
        rule.onNodeWithTag(DECLINE_NOTE_FIELD_TAG).performTextInput("b".repeat(400))
        rule.onNodeWithText("1000/1000").assertExists()
    }

    @Test
    fun backIsLockedWhileSubmitting() {
        val state =
            RequestDecisionUiState(
                load = RequestDecisionLoad.Loaded(details),
                submitting = DecisionSubmitting.Accepting,
            )

        assertEquals(0, systemBackReachesNavigation(state))
        rule.onNodeWithContentDescription("Back").assertIsNotEnabled()
    }

    @Test
    fun backWorksWhenNothingIsInFlight() {
        val state = RequestDecisionUiState(load = RequestDecisionLoad.Loaded(details))

        assertEquals(1, systemBackReachesNavigation(state))
        rule.onNodeWithContentDescription("Back").assertIsEnabled().performClick()
        assertEquals(1, backCount)
    }

    @Test
    fun errorDialogKeepsSheetInputs() {
        pendingRequest()
        failAccept(500, "INTERNAL_ERROR", "Boom")
        show(RequestDecisionKey("r1"))

        rule.onNodeWithTag(REQUEST_ACCEPT_TAG).performClick()
        rule.onNodeWithTag(ACCEPT_MESSAGE_FIELD_TAG).performTextInput("Hi there")
        rule.onNodeWithTag(ACCEPT_SHEET_CONFIRM_TAG).performClick()
        rule.waitForIdle()

        rule.onNodeWithText("Couldn't update request").assertExists()
        rule.onNodeWithText("Boom").assertExists()

        rule.onNodeWithText("OK").performClick()
        rule.waitForIdle()

        rule.onNodeWithText("Couldn't update request").assertDoesNotExist()
        rule.onNodeWithText("Confirm Booking").assertExists()
        rule.onNodeWithTag(ACCEPT_MESSAGE_FIELD_TAG).assertTextContains("Hi there")
        rule.onNodeWithTag(ACCEPT_SHEET_CONFIRM_TAG).assertIsEnabled()
        assertEquals(0, exitCount)
        assertEquals(0, accepted.size)
    }

    @Test
    fun alreadyProcessedExitsToARefreshedList() {
        pendingRequest()
        failAccept(409, "ALREADY_PROCESSED", "This request has already been handled.")
        show(RequestDecisionKey("r1"))

        rule.onNodeWithTag(REQUEST_ACCEPT_TAG).performClick()
        rule.onNodeWithTag(ACCEPT_SHEET_CONFIRM_TAG).performClick()
        rule.waitForIdle()

        rule.onNodeWithText("Couldn't update request").assertExists()
        rule.onNodeWithText("This request has already been handled.").assertExists()
        assertEquals(0, exitCount)

        rule.onNodeWithText("OK").performClick()
        rule.waitForIdle()

        assertEquals(1, exitCount)
        assertEquals(0, accepted.size)
        assertEquals(0, declinedCount)
        assertEquals(IncomingRequestsLaunch(), launch.incomingRequests.pending.value)
    }

    @Test
    fun alreadyProcessedOnDeclineNeverReportsADecline() {
        pendingRequest()
        api.declineBookingRequestHandler = { _, _ ->
            throw api.httpError(409, """{"status":"error","code":"ALREADY_PROCESSED","message":"Already handled."}""")
        }
        show(RequestDecisionKey("r1"))

        rule.onNodeWithTag(REQUEST_DECLINE_TAG).performClick()
        rule.onNodeWithTag(DECLINE_REASON_ROW_TAG_PREFIX + DeclineReason.Other.name).performScrollTo().performClick()
        rule.onNodeWithTag(DECLINE_SHEET_CONFIRM_TAG).performScrollTo().performClick()
        rule.waitForIdle()
        rule.onNodeWithText("OK").performClick()
        rule.waitForIdle()

        assertEquals(1, exitCount)
        assertEquals(0, declinedCount)
        assertEquals(IncomingRequestsLaunch(), launch.incomingRequests.pending.value)
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
