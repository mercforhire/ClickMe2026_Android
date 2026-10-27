package com.uptrendinvestments.clickmeapp.screens.expertmode.bookingsummary

import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.uptrendinvestments.clickmeapp.components.RESCHEDULE_ACCEPT_TAG
import com.uptrendinvestments.clickmeapp.components.RESCHEDULE_DECLINE_REASON_TAG
import com.uptrendinvestments.clickmeapp.components.RESCHEDULE_DECLINE_TAG
import com.uptrendinvestments.clickmeapp.managers.JoinClock
import com.uptrendinvestments.clickmeapp.managers.RestrictedAction
import com.uptrendinvestments.clickmeapp.model.enums.MeetingType
import com.uptrendinvestments.clickmeapp.model.requests.DeclineRescheduleRequest
import com.uptrendinvestments.clickmeapp.model.responses.ConsultationFeeDto
import com.uptrendinvestments.clickmeapp.model.responses.ExpertBookingDetail
import com.uptrendinvestments.clickmeapp.model.responses.RescheduleDto
import com.uptrendinvestments.clickmeapp.model.responses.RescheduleResponseData
import com.uptrendinvestments.clickmeapp.screens.chat.ChatLaunchRequest
import com.uptrendinvestments.clickmeapp.screens.expertmode.upcomingbookings.ExpertBookingsRepository
import com.uptrendinvestments.clickmeapp.testing.FakeCallGuard
import com.uptrendinvestments.clickmeapp.testing.FakeCallStarter
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.FakeRealtimeConnection
import com.uptrendinvestments.clickmeapp.testing.MutableClock
import com.uptrendinvestments.clickmeapp.testing.expertBookingDetail
import com.uptrendinvestments.clickmeapp.testing.initiateData
import com.uptrendinvestments.clickmeapp.testing.testChatEntryViewModel
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
class ExpertBookingSummaryScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private val start = Instant.parse("2026-10-24T18:00:00Z")

    private val api = FakeClickMeApi()
    private val launch = ChatLaunchRequest()
    private val callStarter = FakeCallStarter()
    private val callGuard = FakeCallGuard()
    private val ids = mutableListOf<String>()
    private val cancels = mutableListOf<String>()
    private var backs = 0

    /** The clock sits 30 minutes before [start], so a session starting at [start] is inside the join window. */
    private val now: Instant = start.minusSeconds(1800)

    /** The real screen over a real ViewModel, repository and fake API. */
    private fun showScreen(
        detail: ExpertBookingDetail,
        bookingId: String = "b1",
    ) = showScreen(bookingId) {
        ids += it
        detail
    }

    private fun showScreen(
        bookingId: String = "b1",
        handler: suspend (String) -> ExpertBookingDetail,
    ) {
        api.expertBookingDetailsHandler = handler
        val clock = MutableClock(now)
        val repository = ExpertBookingsRepository(api)
        val viewModel =
            ExpertBookingSummaryViewModel(
                ExpertBookingSummaryKey(bookingId, ExpertBookingOrigin.Bookings),
                repository,
                ExpertRescheduleResponder(repository),
                FakeRealtimeConnection(),
                clock,
                callStarter,
                callGuard,
                JoinClock(clock, CoroutineScope(Job())),
            )
        val chatEntry = testChatEntryViewModel(api, launch)
        rule.setContent {
            ClickMeTheme {
                ExpertBookingSummaryScreen(
                    viewModel = viewModel,
                    onBack = { backs++ },
                    onCancel = { cancels += bookingId },
                    chatEntry = chatEntry,
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
        // The screen reloads quietly on resume, so the id may be requested more than once; it is always this booking.
        assertTrue(ids.isNotEmpty() && ids.all { it == "b1" })
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

    // ---- Join, message card, Cancel, skeleton and error ----

    @Test
    fun joinShownForConfirmedAndStartsCall() {
        showScreen(expertBookingDetail(start = start, statusCode = "confirmed"))

        val join = rule.onNodeWithTag(EXPERT_SUMMARY_JOIN_TAG)
        join.performScrollTo()
        join.assertIsEnabled()
        join.performClick()

        val request = callStarter.requests.single()
        assertEquals("b1", request.bookingId)
        assertEquals("c1", request.peerId)
        assertEquals("Ada Lovelace", request.peerName)
        assertEquals("Algorithms", request.topic)
        assertEquals(MeetingType.InAppVoice, request.meetingType)
    }

    private fun assertNoJoinFor(code: String) {
        showScreen(expertBookingDetail(start = start, statusCode = code))
        rule.onAllNodesWithTag(EXPERT_SUMMARY_JOIN_TAG).assertCountEquals(0)
    }

    @Test
    fun joinHiddenForCancelled() = assertNoJoinFor("cancelled")

    @Test
    fun joinHiddenForDeclined() = assertNoJoinFor("declined")

    @Test
    fun joinHiddenForMissed() = assertNoJoinFor("missed")

    @Test
    fun joinHiddenForExpired() = assertNoJoinFor("expired")

    @Test
    fun joinHiddenForACompletedBookingThatHasEnded() {
        showScreen(
            expertBookingDetail(
                start = now.minusSeconds(7200),
                end = now.minusSeconds(3600),
                statusCode = "completed",
            ),
        )
        rule.onAllNodesWithTag(EXPERT_SUMMARY_JOIN_TAG).assertCountEquals(0)
    }

    @Test
    fun joinShownForACompletedBookingThatEndsInTheFuture() {
        showScreen(
            expertBookingDetail(
                start = now.minusSeconds(600),
                end = now.plusSeconds(3000),
                statusCode = "completed",
            ),
        )
        rule.onNodeWithTag(EXPERT_SUMMARY_JOIN_TAG).assertExists()
    }

    @Test
    fun messageCardOnlyWhenNotesPresent() {
        showScreen(expertBookingDetail(start = start, notes = "Please review my CV"))
        rule.onNodeWithText("Client's message").assertExists()
        rule.onNodeWithText("Please review my CV").assertExists()
    }

    @Test
    fun noMessageCardForNullNotes() {
        showScreen(expertBookingDetail(start = start, notes = null))
        rule.onAllNodesWithText("Client's message").assertCountEquals(0)
    }

    @Test
    fun noMessageCardForBlankNotes() {
        showScreen(expertBookingDetail(start = start, notes = "   "))
        rule.onAllNodesWithText("Client's message").assertCountEquals(0)
    }

    @Test
    fun cancelHiddenWhenNotAllowed() {
        showScreen(expertBookingDetail(start = start, canCancel = false))
        rule.onAllNodesWithTag(EXPERT_SUMMARY_CANCEL_TAG).assertCountEquals(0)
    }

    @Test
    fun cancelOpensTheCancelFlowWhenTheGuardAllows() {
        showScreen(expertBookingDetail(start = start, canCancel = true))
        rule.onNodeWithTag(EXPERT_SUMMARY_CANCEL_TAG).performScrollTo().performClick()

        assertEquals(listOf("b1" to RestrictedAction.CancelBooking), callGuard.modifyAttempts)
        assertEquals(listOf("b1"), cancels)
    }

    @Test
    fun cancelIsBlockedWhileThisBookingsCallIsLive() {
        callGuard.blocked = true
        showScreen(expertBookingDetail(start = start, canCancel = true))
        rule.onNodeWithTag(EXPERT_SUMMARY_CANCEL_TAG).performScrollTo().performClick()

        assertEquals(listOf("b1" to RestrictedAction.CancelBooking), callGuard.modifyAttempts)
        assertTrue(cancels.isEmpty())
    }

    @Test
    fun noRescheduleNoteReportOrBlockControls() {
        showScreen(expertBookingDetail(start = start, notes = "Hello"))
        listOf("Reschedule", "Private Note", "My Note", "Meeting link", "Report", "Block").forEach {
            rule.onAllNodesWithText(it, substring = true).assertCountEquals(0)
        }
    }

    @Test
    fun messageClientOpensChat() {
        api.initiateChatHandler = { initiateData("t1") }
        showScreen(expertBookingDetail(start = start, statusCode = "confirmed", canCancel = true))
        rule.onNodeWithText("Message Client").assertExists()

        // Message Client sits after Join Call and before Cancel Booking.
        val top = { tag: String ->
            val node = rule.onNodeWithTag(tag).fetchSemanticsNode()
            node.positionInRoot.y
        }
        assertTrue(top(EXPERT_SUMMARY_JOIN_TAG) < top(MESSAGE_CLIENT_TAG))
        assertTrue(top(MESSAGE_CLIENT_TAG) < top(EXPERT_SUMMARY_CANCEL_TAG))

        rule.onNodeWithTag(MESSAGE_CLIENT_TAG).performScrollTo().performClick()
        rule.waitForIdle()

        assertEquals(listOf("c1"), api.initiateChatRequests.map { it.peerId })
        val key = launch.thread.consume()
        assertEquals("t1", key?.threadId)
        assertEquals("c1", key?.peerId)
        assertEquals(false, key?.peerIsExpert)
    }

    @Test
    fun messageClientShowsWithoutJoinOrCancel() {
        showScreen(expertBookingDetail(start = start, statusCode = "cancelled", canCancel = false))
        rule.onNodeWithTag(MESSAGE_CLIENT_TAG).assertExists()
    }

    @Test
    fun messageClientFailureShowsTheErrorDialog() {
        api.initiateChatHandler = {
            throw api.httpError(404, """{"status":"error","code":"NOT_FOUND","message":"missing"}""")
        }
        showScreen(expertBookingDetail(start = start))
        rule.onNodeWithTag(MESSAGE_CLIENT_TAG).performScrollTo().performClick()
        rule.waitForIdle()

        rule.onNodeWithText("Couldn't open chat").assertExists()
        assertEquals(null, launch.thread.consume())
    }

    @Test
    fun skeletonThenErrorWithRetry() {
        val gate = CompletableDeferred<ExpertBookingDetail>()
        showScreen { gate.await() }
        rule.onNodeWithContentDescription("Loading").assertExists()
    }

    @Test
    fun failureShowsTheErrorAndRetry() {
        showScreen { throw IOException("offline") }
        rule.onNodeWithText("Couldn't load booking").assertExists()
        rule.onNodeWithText("Retry").assertExists()
    }

    // ---- Reschedule response (11-13) ----

    private fun proposal(initiatedBy: String) =
        RescheduleDto(
            proposedStart = start.plusSeconds(86_400),
            proposedEnd = start.plusSeconds(86_400 + 3600),
            initiatedBy = initiatedBy,
        )

    private fun pendingReschedule(initiatedBy: String) =
        expertBookingDetail(start = start, statusCode = "pending_reschedule", reschedule = proposal(initiatedBy))

    private fun inDialog(text: String) = hasText(text) and hasAnyAncestor(isDialog())

    @Test
    fun clientProposalAcceptEndToEnd() {
        var current = pendingReschedule("client")
        var detailCalls = 0
        api.acceptExpertRescheduleHandler = { id, _ ->
            current = expertBookingDetail(start = start, statusCode = "confirmed")
            RescheduleResponseData(bookingId = id, status = "confirmed")
        }
        showScreen {
            detailCalls++
            current
        }
        rule.onNodeWithText("Client proposed a new time").assertExists()

        val callsBefore = detailCalls
        rule.onNodeWithTag(RESCHEDULE_ACCEPT_TAG).performScrollTo().performClick()
        rule.onNodeWithText("Accept the proposed reschedule?").assertExists()
        rule.onNode(inDialog("Accept")).performClick()
        rule.waitForIdle()

        assertEquals(listOf("b1" to RestrictedAction.RespondToReschedule), callGuard.modifyAttempts)
        assertEquals(1, api.acceptExpertRescheduleRequests.size)
        assertEquals("b1", api.acceptExpertRescheduleRequests.single().first)
        assertTrue(detailCalls > callsBefore)
        rule.onNodeWithText("Reschedule accepted").assertExists()
        rule.onAllNodesWithTag(RESCHEDULE_ACCEPT_TAG).assertCountEquals(0)
    }

    @Test
    fun expertProposalIsReadOnly() {
        showScreen(pendingReschedule("expert"))
        rule.onNodeWithText("You proposed a new time").assertExists()
        rule.onNodeWithText("Awaiting client's response").assertExists()
        rule.onAllNodesWithTag(RESCHEDULE_ACCEPT_TAG).assertCountEquals(0)
    }

    @Test
    fun guardBlocksAccept() {
        callGuard.blocked = true
        showScreen(pendingReschedule("client"))
        rule.onNodeWithTag(RESCHEDULE_ACCEPT_TAG).performScrollTo().performClick()
        rule.onNode(inDialog("Accept")).performClick()
        rule.waitForIdle()

        assertEquals(listOf("b1" to RestrictedAction.RespondToReschedule), callGuard.modifyAttempts)
        assertEquals(0, api.acceptExpertRescheduleCalls)
    }

    @Test
    fun declineSendsTheTrimmedReasonAndShowsTheNotice() {
        var current = pendingReschedule("client")
        api.declineExpertRescheduleHandler = { id, _ ->
            current = expertBookingDetail(start = start, statusCode = "confirmed")
            RescheduleResponseData(bookingId = id, status = "confirmed")
        }
        showScreen { current }

        rule.onNodeWithTag(RESCHEDULE_DECLINE_TAG).performScrollTo().performClick()
        rule.onNode(inDialog("Keep session as-is")).assertExists()
        rule.onNodeWithTag(RESCHEDULE_DECLINE_REASON_TAG).performTextInput("  Busy  ")
        rule.onNode(inDialog("Decline")).performClick()
        rule.waitForIdle()

        assertEquals(listOf("b1" to DeclineRescheduleRequest("Busy")), api.declineExpertRescheduleRequests)
        rule.onNodeWithText("Reschedule declined").assertExists()
    }

    @Test
    fun cancelDisabledWhileResponding() {
        val gate = CompletableDeferred<RescheduleResponseData>()
        api.acceptExpertRescheduleHandler = { _, _ -> gate.await() }
        showScreen(pendingReschedule("client"))
        rule.onNodeWithTag(EXPERT_SUMMARY_CANCEL_TAG).performScrollTo().assertIsEnabled()

        rule.onNodeWithTag(RESCHEDULE_ACCEPT_TAG).performScrollTo().performClick()
        rule.onNode(inDialog("Accept")).performClick()
        rule.waitForIdle()

        rule.onNodeWithTag(EXPERT_SUMMARY_CANCEL_TAG).performScrollTo().assertIsNotEnabled()
        gate.complete(RescheduleResponseData(bookingId = "b1", status = "confirmed"))
        rule.waitForIdle()
        rule.onNodeWithTag(EXPERT_SUMMARY_CANCEL_TAG).performScrollTo().assertIsEnabled()
    }

    @Test
    fun slotUnavailableShowsTheDialogAndKeepsTheProposal() {
        api.acceptExpertRescheduleHandler = { _, _ ->
            throw api.httpError(409, """{"status":"error","code":"SLOT_UNAVAILABLE","message":"nope"}""")
        }
        showScreen(pendingReschedule("client"))

        rule.onNodeWithTag(RESCHEDULE_ACCEPT_TAG).performScrollTo().performClick()
        rule.onNode(inDialog("Accept")).performClick()
        rule.waitForIdle()

        rule.onNodeWithText("Couldn't update booking").assertExists()
        rule.onNodeWithText("That time is no longer available — the booking stays at its original time.").assertExists()
        rule.onAllNodesWithText("Reschedule accepted").assertCountEquals(0)
        // Reloaded from the server: the proposal is still pending and nothing moved.
        rule.onNodeWithTag(RESCHEDULE_ACCEPT_TAG).assertExists()
    }

    @Test
    fun errorDialogShowsTitleAndOkOnAPopErrorLeavesTheScreen() {
        api.acceptExpertRescheduleHandler = { _, _ ->
            throw api.httpError(404, """{"status":"error","code":"NOT_FOUND","message":"gone"}""")
        }
        showScreen(pendingReschedule("client"))

        rule.onNodeWithTag(RESCHEDULE_ACCEPT_TAG).performScrollTo().performClick()
        rule.onNode(inDialog("Accept")).performClick()
        rule.waitForIdle()

        rule.onNodeWithText("Couldn't update booking").assertExists()
        rule.onNodeWithText("This booking could not be found.").assertExists()
        assertEquals(0, backs)
        rule.onNodeWithText("OK").performClick()
        rule.waitForIdle()
        assertEquals(1, backs)
    }
}
