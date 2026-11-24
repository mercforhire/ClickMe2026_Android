package com.uptrendinvestments.clickmeapp.screens.expertmode.bookingsummary

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.width
import com.uptrendinvestments.clickmeapp.components.RESCHEDULE_ACCEPT_TAG
import com.uptrendinvestments.clickmeapp.components.RESCHEDULE_DECLINE_REASON_TAG
import com.uptrendinvestments.clickmeapp.components.RESCHEDULE_DECLINE_TAG
import com.uptrendinvestments.clickmeapp.components.booknote.MY_NOTE_CARD_TAG
import com.uptrendinvestments.clickmeapp.managers.CallResumeNotice
import com.uptrendinvestments.clickmeapp.managers.JoinClock
import com.uptrendinvestments.clickmeapp.managers.RestrictedAction
import com.uptrendinvestments.clickmeapp.model.enums.MeetingType
import com.uptrendinvestments.clickmeapp.model.requests.DeclineRescheduleRequest
import com.uptrendinvestments.clickmeapp.model.responses.ConsultationFeeDto
import com.uptrendinvestments.clickmeapp.model.responses.ExpertBookingDetail
import com.uptrendinvestments.clickmeapp.model.responses.RescheduleDto
import com.uptrendinvestments.clickmeapp.model.responses.RescheduleResponseData
import com.uptrendinvestments.clickmeapp.screens.chat.ChatLaunchRequest
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.RescheduleUiState
import com.uptrendinvestments.clickmeapp.screens.clientmode.reschedule.RescheduleSentRequest
import com.uptrendinvestments.clickmeapp.screens.expertmode.bookingnote.ExpertBookingNoteKey
import com.uptrendinvestments.clickmeapp.screens.expertmode.upcomingbookings.ExpertBookingsRepository
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.testing.FakeCallGuard
import com.uptrendinvestments.clickmeapp.testing.FakeCallStarter
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.FakeRealtimeConnection
import com.uptrendinvestments.clickmeapp.testing.MutableClock
import com.uptrendinvestments.clickmeapp.testing.assertActionablesNamed
import com.uptrendinvestments.clickmeapp.testing.assertTouchTargets
import com.uptrendinvestments.clickmeapp.testing.expertBookingDetail
import com.uptrendinvestments.clickmeapp.testing.initiateData
import com.uptrendinvestments.clickmeapp.testing.lineCountsOf
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
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.IOException
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

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
    private val reschedules = mutableListOf<String>()
    private val noteKeys = mutableListOf<ExpertBookingNoteKey>()
    private val profileIds = mutableListOf<String>()
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
                callStarter,
                callGuard,
                JoinClock(clock, CoroutineScope(Job())),
                CallResumeNotice(),
                RescheduleSentRequest(),
            )
        val chatEntry = testChatEntryViewModel(api, launch)
        rule.setContent {
            ClickMeTheme {
                ExpertBookingSummaryScreen(
                    viewModel = viewModel,
                    onBack = { backs++ },
                    onCancel = { cancels += bookingId },
                    onReschedule = { reschedules += bookingId },
                    onOpenNote = { noteKeys += it },
                    onViewProfile = { profileIds += it },
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
            "US\$75.00",
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

    // ---- call resume note, refresh standard, unavailable and width cap (Phase 15) ----

    private fun summaryDetails() = expertBookingDetail(start = start).toSummaryDetails(Locale.US)

    private fun loadedState(
        showCallResumeNote: Boolean = false,
        isRefreshing: Boolean = false,
        refreshFailed: Boolean = false,
    ) = ExpertBookingSummaryUiState(
        load = ExpertSummaryLoad.Loaded(summaryDetails()),
        showCallResumeNote = showCallResumeNote,
        isRefreshing = isRefreshing,
        refreshFailed = refreshFailed,
    )

    private fun showContent(
        state: ExpertBookingSummaryUiState,
        events: MutableList<String> = mutableListOf(),
        fontScale: Float = 1f,
        joinNow: Instant = now,
    ): MutableList<String> {
        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                ClickMeTheme {
                    ExpertBookingSummaryContent(
                        state = state,
                        rescheduleState = RescheduleUiState(),
                        joinNow = joinNow,
                        onJoin = {},
                        onBack = {},
                        onRetry = { events += "retry" },
                        onCancel = {},
                        onAcceptReschedule = {},
                        onDeclineReschedule = {},
                        onDismissRescheduleDialog = {},
                        onRescheduleNoticeConsumed = {},
                        onOpenNote = { events += "note" },
                        onViewProfile = { events += "profile:$it" },
                        onRefresh = { events += "refresh" },
                        onRefreshFailureConsumed = { events += "refreshFailureConsumed" },
                        zone = ZoneId.of("America/Toronto"),
                        timeFormat = SimpleDateFormat("h:mm a", Locale.US),
                        locale = Locale.US,
                    )
                }
            }
        }
        rule.waitForIdle()
        return events
    }

    @Test
    fun noteIsPoliteLiveRegionAboveJoin() {
        showContent(loadedState(showCallResumeNote = true))
        rule
            .onNodeWithText("You were on a call. Tap Join Call to rejoin.")
            .assertExists()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        val top = { tag: String ->
            val node = rule.onNodeWithTag(tag).fetchSemanticsNode()
            node.positionInRoot.y
        }
        assertTrue(top("call_resume_note") < top(EXPERT_SUMMARY_JOIN_TAG))
    }

    @Test
    fun noNoteWhenNotFlagged() {
        showContent(loadedState())
        rule.onAllNodesWithText("You were on a call. Tap Join Call to rejoin.").assertCountEquals(0)
    }

    private fun stateFor(statusCode: String) =
        ExpertBookingSummaryUiState(
            load =
                ExpertSummaryLoad.Loaded(
                    expertBookingDetail(statusCode = statusCode, start = start).toSummaryDetails(Locale.US),
                ),
        )

    @Test
    fun addToCalendarShowsForBookedSessionNotYetEnded() {
        showContent(stateFor("confirmed"))
        rule.onNodeWithTag(EXPERT_SUMMARY_ADD_TO_CALENDAR_TAG).assertExists()
        rule.onNodeWithText("Add to Calendar").assertExists()
    }

    @Test
    fun addToCalendarIsHiddenForACompletedBooking() {
        showContent(stateFor("completed"))
        rule.onAllNodesWithTag(EXPERT_SUMMARY_ADD_TO_CALENDAR_TAG).assertCountEquals(0)
    }

    @Test
    fun addToCalendarIsHiddenOnceTheSessionHasEnded() {
        showContent(stateFor("confirmed"), joinNow = start.plusSeconds(3600))
        rule.onAllNodesWithTag(EXPERT_SUMMARY_ADD_TO_CALENDAR_TAG).assertCountEquals(0)
    }

    @Test
    fun refreshFailedShowsRetry() {
        val events = showContent(loadedState(refreshFailed = true))
        rule.waitForIdle()
        rule.onNodeWithText("Ada Lovelace").assertExists()
        assertEquals(listOf("refreshFailureConsumed"), events)
        rule.onNodeWithText("Retry").performClick()
        rule.waitForIdle()
        assertEquals(listOf("refreshFailureConsumed", "refresh"), events)
        rule.onNodeWithText("Ada Lovelace").assertExists()
    }

    @Test
    fun refreshingShowsLinearIndicator() {
        showContent(loadedState(isRefreshing = true))
        rule.onNodeWithTag("expert_summary_refresh_progress").assertExists()
        rule.onAllNodesWithContentDescription("Loading").assertCountEquals(0)
        rule.onNodeWithText("Ada Lovelace").assertExists()
    }

    @Test
    fun noIndicatorWhenNotRefreshing() {
        showContent(loadedState())
        rule.onAllNodesWithTag("expert_summary_refresh_progress").assertCountEquals(0)
    }

    @Test
    fun firstLoadErrorNamed() {
        val events = showContent(ExpertBookingSummaryUiState(ExpertSummaryLoad.Failed(FieldError.Text("Boom"))))
        rule
            .onNodeWithText("Couldn't load booking")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        rule.onNodeWithText("Boom").assertExists()
        rule.onNodeWithText("Retry").performClick()
        assertEquals(listOf("retry"), events)
    }

    @Test
    fun unavailableShowsNoRetry() {
        showContent(ExpertBookingSummaryUiState(ExpertSummaryLoad.Unavailable))
        rule.onNodeWithText("This booking isn't available").assertExists()
        rule.onNodeWithText("It may have been removed. Go back and try another.").assertExists()
        rule.onAllNodesWithText("Retry").assertCountEquals(0)
    }

    /** D-02: on a large window the content column is capped at 600 dp and centered. */
    @Test
    @Config(qualifiers = "w900dp-h600dp")
    fun contentCapped() {
        showContent(loadedState())
        val message = rule.onNodeWithTag(MESSAGE_CLIENT_TAG).getUnclippedBoundsInRoot()
        assertTrue("message button spans ${message.left} to ${message.right}", message.right <= 750.dp)
        assertTrue("message button spans ${message.left} to ${message.right}", message.left >= 150.dp)
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

    /** 30 minutes past the first-join edge (`max(end, start + 60 min)`), still inside end + 60 min. */
    private fun pastTheFirstJoinEdge(canJoin: Boolean) =
        expertBookingDetail(
            start = now.minusSeconds(5400),
            end = now.minusSeconds(1800),
            statusCode = "in_progress",
            canJoin = canJoin,
        )

    @Test
    fun joinReconnectsPastTheEndWhenTheServerAllows() {
        showScreen(pastTheFirstJoinEdge(canJoin = true))
        rule.onNodeWithTag(EXPERT_SUMMARY_JOIN_TAG).performScrollTo().assertIsEnabled()
    }

    @Test
    fun joinIsDisabledPastTheEndWithoutTheServerFlag() {
        showScreen(pastTheFirstJoinEdge(canJoin = false))
        rule.onNodeWithTag(EXPERT_SUMMARY_JOIN_TAG).performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun joinShownForACompletedBookingInTheReconnectTier() {
        showScreen(
            expertBookingDetail(
                start = now.minusSeconds(5400),
                end = now.minusSeconds(1800),
                statusCode = "completed",
                canJoin = true,
            ),
        )
        rule.onNodeWithTag(EXPERT_SUMMARY_JOIN_TAG).performScrollTo().assertIsEnabled()
    }

    @Test
    fun joinHiddenForACompletedBookingPastTheEndWithoutTheServerFlag() {
        showScreen(
            expertBookingDetail(
                start = now.minusSeconds(5400),
                end = now.minusSeconds(1800),
                statusCode = "completed",
                canJoin = false,
            ),
        )
        rule.onAllNodesWithTag(EXPERT_SUMMARY_JOIN_TAG).assertCountEquals(0)
    }

    @Test
    fun joinHiddenForACompletedBookingAfterTheReconnectWindow() {
        showScreen(
            expertBookingDetail(
                start = now.minusSeconds(7201),
                end = now.minusSeconds(3601),
                statusCode = "completed",
                canJoin = true,
            ),
        )
        rule.onAllNodesWithTag(EXPERT_SUMMARY_JOIN_TAG).assertCountEquals(0)
    }

    @Test
    fun completedJoinLeavesWhenTheJoinClockPassesTheReconnectWindow() {
        var joinNow by mutableStateOf(start.plusSeconds(7200))
        val state =
            ExpertBookingSummaryUiState(
                load =
                    ExpertSummaryLoad.Loaded(
                        expertBookingDetail(
                            start = start,
                            end = start.plusSeconds(3600),
                            statusCode = "completed",
                            canJoin = true,
                        ).toSummaryDetails(Locale.US),
                    ),
            )
        rule.setContent {
            ClickMeTheme {
                ExpertBookingSummaryContent(
                    state = state,
                    rescheduleState = RescheduleUiState(),
                    joinNow = joinNow,
                    onJoin = {},
                    onBack = {},
                    onRetry = {},
                    onCancel = {},
                    onAcceptReschedule = {},
                    onDeclineReschedule = {},
                    onDismissRescheduleDialog = {},
                    onRescheduleNoticeConsumed = {},
                    onOpenNote = {},
                    onViewProfile = {},
                    onRefresh = {},
                    onRefreshFailureConsumed = {},
                    zone = ZoneId.of("America/Toronto"),
                    timeFormat = SimpleDateFormat("h:mm a", Locale.US),
                    locale = Locale.US,
                )
            }
        }
        rule.waitForIdle()
        rule.onNodeWithTag(EXPERT_SUMMARY_JOIN_TAG).performScrollTo().assertIsEnabled()
        joinNow = start.plusSeconds(7201)
        rule.waitForIdle()
        rule.onAllNodesWithTag(EXPERT_SUMMARY_JOIN_TAG).assertCountEquals(0)
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
    fun noRescheduleReportBlockOrMeetingLinkControls() {
        showScreen(expertBookingDetail(start = start, notes = "Hello"))
        listOf("Reschedule", "Meeting link", "Report", "Block").forEach {
            rule.onAllNodesWithText(it, substring = true).assertCountEquals(0)
        }
    }

    // ---- My Note (Phase 16.4, D-05, D-07, D-08) ----

    private val notePrompt = "Tap to jot down your thoughts — only you can see this."

    @Test
    fun myNoteCardShowsTheFullNote() {
        showScreen(expertBookingDetail(start = start, privateNote = "Call back"))
        rule.onNodeWithText("My Note").assertExists()
        rule.onNodeWithText("Call back").assertExists()
        rule.onAllNodesWithText(notePrompt).assertCountEquals(0)
    }

    @Test
    fun myNoteCardShowsThePromptWhenThereIsNoNote() {
        showScreen(expertBookingDetail(start = start, privateNote = null))
        rule.onNodeWithText("My Note").assertExists()
        rule.onNodeWithText(notePrompt).assertExists()
    }

    @Test
    fun myNoteSitsAfterTheClientMessageAndBeforeMessageClient() {
        showScreen(expertBookingDetail(start = start, notes = "Please review my CV"))

        fun top(node: SemanticsNodeInteraction) = node.fetchSemanticsNode().positionInRoot.y
        val message = top(rule.onNodeWithText("Client's message"))
        val note = top(rule.onNodeWithTag(MY_NOTE_CARD_TAG))
        val messageClient = top(rule.onNodeWithTag(MESSAGE_CLIENT_TAG))
        assertTrue("message $message note $note", message < note)
        assertTrue("note $note messageClient $messageClient", note < messageClient)
    }

    @Test
    fun myNoteSitsBeforeJoin() {
        showScreen(expertBookingDetail(start = start, statusCode = "confirmed"))
        val note =
            rule
                .onNodeWithTag(MY_NOTE_CARD_TAG)
                .fetchSemanticsNode()
                .positionInRoot.y
        val join =
            rule
                .onNodeWithTag(EXPERT_SUMMARY_JOIN_TAG)
                .fetchSemanticsNode()
                .positionInRoot.y
        assertTrue("note $note join $join", note < join)
    }

    @Test
    fun tappingMyNoteOpensTheNoteForThisBooking() {
        showScreen(expertBookingDetail(start = start))
        rule.onNodeWithTag(MY_NOTE_CARD_TAG).performScrollTo().performClick()

        assertEquals(
            listOf(
                ExpertBookingNoteKey(
                    bookingId = "b1",
                    clientName = "Ada Lovelace",
                    topic = "Algorithms",
                    startMillis = start.toEpochMilli(),
                    endMillis = start.plusSeconds(3600).toEpochMilli(),
                    origin = ExpertBookingOrigin.Bookings,
                ),
            ),
            noteKeys,
        )
    }

    @Test
    fun myNoteShowsOnEveryStatus() {
        var state by mutableStateOf(loadedState())
        rule.setContent {
            ClickMeTheme {
                ExpertBookingSummaryContent(
                    state = state,
                    rescheduleState = RescheduleUiState(),
                    joinNow = now,
                    onJoin = {},
                    onBack = {},
                    onRetry = {},
                    onCancel = {},
                    onAcceptReschedule = {},
                    onDeclineReschedule = {},
                    onDismissRescheduleDialog = {},
                    onRescheduleNoticeConsumed = {},
                    zone = ZoneId.of("America/Toronto"),
                    timeFormat = SimpleDateFormat("h:mm a", Locale.US),
                    locale = Locale.US,
                )
            }
        }
        listOf("confirmed", "completed", "cancelled").forEach { status ->
            state =
                ExpertBookingSummaryUiState(
                    ExpertSummaryLoad.Loaded(
                        expertBookingDetail(start = start, statusCode = status).toSummaryDetails(Locale.US),
                    ),
                )
            rule.waitForIdle()
            rule.onNodeWithTag(MY_NOTE_CARD_TAG).assertExists()
            rule.onNodeWithText("My Note").assertExists()
        }
    }

    @Test
    @Suppress("MagicNumber")
    fun largeFontKeepsMyNote() {
        val long = "A long private note about the session. ".repeat(12).trim()
        val details = expertBookingDetail(start = start, privateNote = long).toSummaryDetails(Locale.US)
        showContent(ExpertBookingSummaryUiState(ExpertSummaryLoad.Loaded(details)), fontScale = 2f)
        rule.onNodeWithTag(MY_NOTE_CARD_TAG).performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("My Note").assertIsDisplayed()
        rule.onNodeWithText(long).assertIsDisplayed()
    }

    // ---- View Profile (Phase 16.4, D-13) ----

    @Test
    fun viewProfileLinkSitsUnderTheClientName() {
        showScreen(expertBookingDetail(start = start))
        rule.onNodeWithText("View Profile").assertExists()

        fun top(node: SemanticsNodeInteraction) = node.fetchSemanticsNode().positionInRoot.y
        val name = top(rule.onNodeWithText("Ada Lovelace"))
        val link = top(rule.onNodeWithTag(EXPERT_SUMMARY_VIEW_PROFILE_TAG))
        val id = top(rule.onNodeWithText("ID: #B1"))
        assertTrue("name $name link $link", name < link)
        assertTrue("link $link id $id", link < id)
    }

    @Test
    fun viewProfileReportsTheClientId() {
        showScreen(expertBookingDetail(start = start))
        rule.onNodeWithTag(EXPERT_SUMMARY_VIEW_PROFILE_TAG).performScrollTo().performClick()

        assertEquals(listOf("c1"), profileIds)
        assertTrue(noteKeys.isEmpty())
    }

    @Test
    fun viewProfileMeetsTheTargetAudit() {
        showContent(loadedState())
        rule.assertActionablesNamed()
        rule.assertTouchTargets()
    }

    @Test
    fun largeFontKeepsTheLink() {
        showContent(loadedState(), fontScale = 2f)
        rule.onNodeWithText("Ada Lovelace").assertIsDisplayed()
        rule.onNodeWithTag(EXPERT_SUMMARY_VIEW_PROFILE_TAG).assertIsDisplayed()
        rule.onNodeWithText("View Profile").assertIsDisplayed()
        rule.onNodeWithText("ID: #B1").assertIsDisplayed()
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

    private fun pendingReschedule(
        initiatedBy: String,
        canReschedule: Boolean = false,
    ) = expertBookingDetail(
        start = start,
        statusCode = "pending_reschedule",
        reschedule = proposal(initiatedBy),
        canReschedule = canReschedule,
    )

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

    // ---- Reschedule entry point (Phase 16.2, RS-E1) ----

    // The default 320 dp Robolectric screen is narrower than the pair's 320 dp content threshold and would stack.
    @Config(qualifiers = "w411dp-h891dp")
    @Test
    fun bothFlagsShowRescheduleThenCancel() {
        showScreen(expertBookingDetail(start = start, canReschedule = true, canCancel = true))

        val reschedule = rule.onNodeWithTag(EXPERT_SUMMARY_RESCHEDULE_TAG).performScrollTo()
        reschedule.assertExists()
        rule.onNodeWithText("Reschedule").assertExists()
        rule.onNodeWithTag(EXPERT_SUMMARY_CANCEL_TAG).assertExists()
        val rescheduleNode = rule.onNodeWithTag(EXPERT_SUMMARY_RESCHEDULE_TAG).fetchSemanticsNode()
        val cancelNode = rule.onNodeWithTag(EXPERT_SUMMARY_CANCEL_TAG).fetchSemanticsNode()
        // One row: same top, Reschedule on the left (iOS order).
        assertEquals(rescheduleNode.positionInRoot.y, cancelNode.positionInRoot.y, 1f)
        assertTrue(rescheduleNode.positionInRoot.x < cancelNode.positionInRoot.x)

        reschedule.performClick()
        assertEquals(listOf("b1"), reschedules)
    }

    @Config(qualifiers = "w411dp-h891dp")
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Test
    fun rescheduleAndCancelFitOneLineAt411dp() {
        showScreen(expertBookingDetail(start = start, canReschedule = true, canCancel = true))
        rule.onNodeWithTag(EXPERT_SUMMARY_RESCHEDULE_TAG).performScrollTo()

        assertEquals(listOf(1), rule.lineCountsOf("Reschedule", EXPERT_SUMMARY_RESCHEDULE_TAG))
        assertEquals(listOf(1), rule.lineCountsOf("Cancel Booking", EXPERT_SUMMARY_CANCEL_TAG))
        val reschedule = rule.onNodeWithTag(EXPERT_SUMMARY_RESCHEDULE_TAG).fetchSemanticsNode()
        val cancel = rule.onNodeWithTag(EXPERT_SUMMARY_CANCEL_TAG).fetchSemanticsNode()
        assertEquals(reschedule.positionInRoot.y, cancel.positionInRoot.y, 1f)
    }

    @Config(qualifiers = "w360dp-h891dp")
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Test
    fun labelsFitOneLineAt360dp() {
        showScreen(expertBookingDetail(start = start, canReschedule = true, canCancel = true))
        rule.onNodeWithTag(EXPERT_SUMMARY_RESCHEDULE_TAG).performScrollTo()

        assertEquals(listOf(1), rule.lineCountsOf("Reschedule", EXPERT_SUMMARY_RESCHEDULE_TAG))
        assertEquals(listOf(1), rule.lineCountsOf("Cancel Booking", EXPERT_SUMMARY_CANCEL_TAG))
        val reschedule = rule.onNodeWithTag(EXPERT_SUMMARY_RESCHEDULE_TAG).fetchSemanticsNode()
        val cancel = rule.onNodeWithTag(EXPERT_SUMMARY_CANCEL_TAG).fetchSemanticsNode()
        assertTrue(cancel.positionInRoot.y > reschedule.positionInRoot.y)
    }

    @Config(qualifiers = "w384dp-h891dp")
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Test
    fun labelsFitOneLineAt384dp() {
        showScreen(expertBookingDetail(start = start, canReschedule = true, canCancel = true))
        rule.onNodeWithTag(EXPERT_SUMMARY_RESCHEDULE_TAG).performScrollTo()

        assertEquals(listOf(1), rule.lineCountsOf("Reschedule", EXPERT_SUMMARY_RESCHEDULE_TAG))
        assertEquals(listOf(1), rule.lineCountsOf("Cancel Booking", EXPERT_SUMMARY_CANCEL_TAG))
    }

    @Test
    fun onlyRescheduleFillsWidth() {
        showScreen(expertBookingDetail(start = start, canReschedule = true, canCancel = false))

        rule.onAllNodesWithTag(EXPERT_SUMMARY_CANCEL_TAG).assertCountEquals(0)
        val reschedule = rule.onNodeWithTag(EXPERT_SUMMARY_RESCHEDULE_TAG).performScrollTo().getUnclippedBoundsInRoot()
        val message = rule.onNodeWithTag(MESSAGE_CLIENT_TAG).getUnclippedBoundsInRoot()
        assertEquals(message.width.value, reschedule.width.value, 1f)
    }

    @Test
    fun onlyCancelUnchanged() {
        showScreen(expertBookingDetail(start = start, canReschedule = false, canCancel = true))

        rule.onAllNodesWithTag(EXPERT_SUMMARY_RESCHEDULE_TAG).assertCountEquals(0)
        val cancel = rule.onNodeWithTag(EXPERT_SUMMARY_CANCEL_TAG).performScrollTo().getUnclippedBoundsInRoot()
        val message = rule.onNodeWithTag(MESSAGE_CLIENT_TAG).getUnclippedBoundsInRoot()
        assertEquals(message.width.value, cancel.width.value, 1f)
    }

    @Test
    fun neitherFlagNoRow() {
        showScreen(expertBookingDetail(start = start, canReschedule = false, canCancel = false))

        rule.onAllNodesWithTag(EXPERT_SUMMARY_RESCHEDULE_TAG).assertCountEquals(0)
        rule.onAllNodesWithTag(EXPERT_SUMMARY_CANCEL_TAG).assertCountEquals(0)
    }

    @Test
    fun noClientTimeRulesForExpert() {
        // 30 minutes before the start and awaiting approval: the client rules would hide it, the server flag decides.
        showScreen(expertBookingDetail(start = start, statusCode = "pending_approval", canReschedule = true))

        rule.onNodeWithTag(EXPERT_SUMMARY_RESCHEDULE_TAG).performScrollTo().assertIsEnabled()
    }

    @Test
    fun disabledWhileResponseInFlight() {
        val gate = CompletableDeferred<RescheduleResponseData>()
        api.acceptExpertRescheduleHandler = { _, _ -> gate.await() }
        showScreen(pendingReschedule("client", canReschedule = true))
        rule.onNodeWithTag(EXPERT_SUMMARY_RESCHEDULE_TAG).performScrollTo().assertIsEnabled()

        rule.onNodeWithTag(RESCHEDULE_ACCEPT_TAG).performScrollTo().performClick()
        rule.onNode(inDialog("Accept")).performClick()
        rule.waitForIdle()

        rule.onNodeWithTag(EXPERT_SUMMARY_RESCHEDULE_TAG).performScrollTo().assertIsNotEnabled()
        gate.complete(RescheduleResponseData(bookingId = "b1", status = "confirmed"))
        rule.waitForIdle()
    }

    @Test
    fun sentNoticeShowsSnackbarOnce() {
        var consumed = 0
        val details = expertBookingDetail(start = start, canReschedule = true).toSummaryDetails(Locale.US)
        rule.setContent {
            ClickMeTheme {
                ExpertBookingSummaryContent(
                    state = ExpertBookingSummaryUiState(ExpertSummaryLoad.Loaded(details)),
                    rescheduleState = RescheduleUiState(),
                    joinNow = now,
                    onJoin = {},
                    onBack = {},
                    onRetry = {},
                    onCancel = {},
                    onAcceptReschedule = {},
                    onDeclineReschedule = {},
                    onDismissRescheduleDialog = {},
                    onRescheduleNoticeConsumed = {},
                    rescheduleSent = true,
                    onRescheduleSentConsumed = { consumed++ },
                )
            }
        }
        rule.waitForIdle()

        rule.onNodeWithText("Reschedule request sent").assertExists()
        assertEquals(1, consumed)
    }
}
