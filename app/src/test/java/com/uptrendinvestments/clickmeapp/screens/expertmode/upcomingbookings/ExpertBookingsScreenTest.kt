package com.uptrendinvestments.clickmeapp.screens.expertmode.upcomingbookings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.lifecycle.SavedStateHandle
import com.uptrendinvestments.clickmeapp.managers.CallStartRequest
import com.uptrendinvestments.clickmeapp.managers.JoinClock
import com.uptrendinvestments.clickmeapp.model.responses.ExpertBookingItem
import com.uptrendinvestments.clickmeapp.model.responses.ExpertBookingsPayload
import com.uptrendinvestments.clickmeapp.screens.chat.ChatLaunchRequest
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.formatShortDate
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.BookingsNotice
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.BookingsTab
import com.uptrendinvestments.clickmeapp.screens.clientmode.reschedule.RescheduleSentRequest
import com.uptrendinvestments.clickmeapp.screens.expertmode.ExpertLaunchRequest
import com.uptrendinvestments.clickmeapp.screens.expertmode.upcomingbookings.subviews.EXPERT_BOOKINGS_JOIN_TAG
import com.uptrendinvestments.clickmeapp.screens.expertmode.upcomingbookings.subviews.EXPERT_BOOKINGS_MESSAGE_TAG
import com.uptrendinvestments.clickmeapp.screens.expertmode.upcomingbookings.subviews.EXPERT_BOOKINGS_RESCHEDULE_TAG
import com.uptrendinvestments.clickmeapp.screens.expertmode.upcomingbookings.subviews.EXPERT_PAST_CARD_TAG
import com.uptrendinvestments.clickmeapp.screens.expertmode.upcomingbookings.subviews.EXPERT_UPCOMING_CARD_TAG
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.testing.FakeCallStarter
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.FakeRealtimeConnection
import com.uptrendinvestments.clickmeapp.testing.MutableClock
import com.uptrendinvestments.clickmeapp.testing.expertBookingItem
import com.uptrendinvestments.clickmeapp.testing.initiateData
import com.uptrendinvestments.clickmeapp.testing.lineCountsOf
import com.uptrendinvestments.clickmeapp.testing.testChatEntryViewModel
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.IOException
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h1200dp")
class ExpertBookingsScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private val now = Instant.parse("2026-10-24T17:00:00Z")
    private val clock = MutableClock(now)
    private val api = FakeClickMeApi()
    private val callStarter = FakeCallStarter()
    private val chatLaunch = ChatLaunchRequest()

    private fun serve(
        upcoming: List<ExpertBookingItem> = listOf(expertBookingItem(start = now.plusSeconds(1800))),
        past: List<ExpertBookingItem> = emptyList(),
    ) {
        api.expertBookingsHandler = { type ->
            ExpertBookingsPayload(bookings = if (type == "upcoming") upcoming else past)
        }
    }

    private fun viewModel() =
        ExpertBookingsViewModel(
            SavedStateHandle(),
            ExpertBookingsRepository(api),
            FakeRealtimeConnection(),
            clock,
            ExpertLaunchRequest(),
            callStarter,
            JoinClock(clock, CoroutineScope(Job())),
            RescheduleSentRequest(),
        )

    private fun show(
        onOpenBooking: (String) -> Unit = {},
        onReschedule: (String) -> Unit = {},
    ) {
        val vm = viewModel()
        val chatEntry = testChatEntryViewModel(api, chatLaunch)
        rule.setContent {
            ClickMeTheme {
                ExpertBookingsScreen(
                    onOpenBooking = onOpenBooking,
                    onUpdateAvailability = {},
                    onReschedule = onReschedule,
                    viewModel = vm,
                    chatEntry = chatEntry,
                )
            }
        }
        rule.waitForIdle()
    }

    @Test
    fun upcomingCardsRenderFromTheApi() {
        serve()
        show()
        rule.onNodeWithText("Ada Lovelace").assertExists()
        rule.onNodeWithText("Algorithms").assertExists()
        rule.onNodeWithText("Earnings").assertExists()
        rule.onNodeWithText("\$63.75").assertExists()
        rule.onNodeWithTag(EXPERT_BOOKINGS_JOIN_TAG).assertIsEnabled()
    }

    @Test
    fun upcomingCardShowsMessageIcon() {
        serve()
        api.initiateChatHandler = { initiateData("t1") }
        var opened = 0
        show(onOpenBooking = { opened++ })

        rule.onNodeWithContentDescription("Message Ada Lovelace").assertExists()
        rule.onNodeWithTag(EXPERT_BOOKINGS_MESSAGE_TAG).performClick()
        rule.waitForIdle()

        assertEquals(listOf("c1"), api.initiateChatRequests.map { it.peerId })
        val key = chatLaunch.thread.consume()
        assertEquals("c1", key?.peerId)
        assertEquals(false, key?.peerIsExpert)
        assertEquals(0, opened)
    }

    @Test
    fun flaggedCardShowsMessageThenReschedule() {
        serve(upcoming = listOf(expertBookingItem(start = now.plusSeconds(1800), canReschedule = true)))
        show()

        rule.onNodeWithTag(EXPERT_BOOKINGS_JOIN_TAG).assertExists()
        rule.onNodeWithTag(EXPERT_BOOKINGS_MESSAGE_TAG).assertExists()
        rule.onNodeWithText("Message").assertExists()
        rule.onNodeWithTag(EXPERT_BOOKINGS_RESCHEDULE_TAG).assertExists()
        rule.onNodeWithText("Reschedule").assertExists()
        rule.onNodeWithContentDescription("Reschedule session with Ada Lovelace").assertExists()

        val join = rule.onNodeWithTag(EXPERT_BOOKINGS_JOIN_TAG).getBoundsInRoot()
        val message = rule.onNodeWithTag(EXPERT_BOOKINGS_MESSAGE_TAG).getBoundsInRoot()
        val reschedule = rule.onNodeWithTag(EXPERT_BOOKINGS_RESCHEDULE_TAG).getBoundsInRoot()
        assertTrue("Message sits below Join", message.top >= join.bottom)
        assertTrue("Message is left of Reschedule", message.right <= reschedule.left)
        assertEquals(message.top, reschedule.top)
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Config(qualifiers = "w384dp-h1200dp")
    fun rescheduleAndMessageFitOneLineAt384dp() {
        serve(upcoming = listOf(expertBookingItem(start = now.plusSeconds(1800), canReschedule = true)))
        show()

        assertEquals(listOf(1), rule.lineCountsOf("Reschedule", EXPERT_BOOKINGS_RESCHEDULE_TAG))
        assertEquals(listOf(1), rule.lineCountsOf("Message", EXPERT_BOOKINGS_MESSAGE_TAG))
        val message = rule.onNodeWithTag(EXPERT_BOOKINGS_MESSAGE_TAG).getBoundsInRoot()
        val reschedule = rule.onNodeWithTag(EXPERT_BOOKINGS_RESCHEDULE_TAG).getBoundsInRoot()
        val topGap = kotlin.math.abs(message.top.value - reschedule.top.value)
        assertTrue("Message and Reschedule share a row", topGap <= 1f)
        assertTrue("Message is left of Reschedule", message.right <= reschedule.left)
    }

    @Test
    fun rescheduleClickDoesNotOpenSummary() {
        serve(upcoming = listOf(expertBookingItem(start = now.plusSeconds(1800), canReschedule = true)))
        val opened = mutableListOf<String>()
        val rescheduled = mutableListOf<String>()
        show(onOpenBooking = { opened += it }, onReschedule = { rescheduled += it })

        rule.onNodeWithTag(EXPERT_BOOKINGS_RESCHEDULE_TAG).performClick()

        assertEquals(listOf("b1"), rescheduled)
        assertEquals(emptyList<String>(), opened)
    }

    @Test
    fun unflaggedCardUnchanged() {
        serve(upcoming = listOf(expertBookingItem(start = now.plusSeconds(1800), canReschedule = false)))
        show()

        rule.onAllNodesWithTag(EXPERT_BOOKINGS_RESCHEDULE_TAG).assertCountEquals(0)
        rule.onNodeWithContentDescription("Message Ada Lovelace").assertExists()
        assertEquals(0, rule.onAllNodesWithText("Message").fetchSemanticsNodes().size)
    }

    @Test
    fun mixedListShowsOneReschedule() {
        serve(
            upcoming =
                listOf(
                    expertBookingItem(id = "b1", start = now.plusSeconds(1800), canReschedule = true),
                    expertBookingItem(id = "b2", start = now.plusSeconds(7200), canReschedule = false),
                ),
        )
        show()

        rule.onAllNodesWithTag(EXPERT_UPCOMING_CARD_TAG).assertCountEquals(2)
        rule.onAllNodesWithTag(EXPERT_BOOKINGS_RESCHEDULE_TAG).assertCountEquals(1)
    }

    @Test
    fun messageFailureShowsTheErrorDialog() {
        serve()
        api.initiateChatHandler = {
            throw api.httpError(404, """{"status":"error","code":"NOT_FOUND","message":"missing"}""")
        }
        show()

        rule.onNodeWithTag(EXPERT_BOOKINGS_MESSAGE_TAG).performClick()
        rule.waitForIdle()

        rule.onNodeWithText("Couldn't open chat").assertExists()
        assertEquals(null, chatLaunch.thread.consume())
    }

    @Test
    fun joinStartsTheCallWithTheClientAsPeer() {
        serve()
        show()
        rule.onNodeWithTag(EXPERT_BOOKINGS_JOIN_TAG).performClick()
        val request: CallStartRequest = callStarter.requests.single()
        assertEquals("b1", request.bookingId)
        assertEquals("c1", request.peerId)
        assertEquals("Ada Lovelace", request.peerName)
    }

    @Test
    fun cardTapOpensSummary() {
        serve()
        val opened = mutableListOf<String>()
        show(onOpenBooking = { opened += it })
        rule.onNodeWithTag(EXPERT_UPCOMING_CARD_TAG).performClick()
        assertEquals(listOf("b1"), opened)
    }

    @Test
    fun tabRowShowsUpcomingAndPast() {
        serve()
        show()
        rule.onNodeWithText("Upcoming").assertIsSelected()
        rule.onNodeWithText("Past").assertIsNotSelected()
        assertEquals(0, rule.onAllNodesWithText("Message").fetchSemanticsNodes().size)
    }

    @Test
    fun firstLoadShowsSkeleton() {
        api.expertBookingsHandler = { awaitCancellation() }
        show()
        rule.onNodeWithContentDescription("Loading").assertExists()
        assertTrue(rule.onAllNodesWithTag(EXPERT_UPCOMING_CARD_TAG).fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun hardFailureShowsTheServerMessageWithRetry() {
        api.expertBookingsHandler = { throw api.httpError(500, """{"status":"error","message":"Boom"}""") }
        show()
        rule.onNodeWithText("Couldn't load sessions").assertExists()
        rule.onNodeWithText("Boom").assertExists()
        val before = api.expertBookingsCalls
        rule.onNodeWithText("Retry").performClick()
        rule.waitForIdle()
        assertTrue(api.expertBookingsCalls > before)
    }

    @Test
    fun aNetworkFailureShowsTheConnectionMessage() {
        api.expertBookingsHandler = { throw IOException("offline") }
        show()
        rule.onNodeWithText("Couldn't load sessions").assertExists()
        rule.onNodeWithText("Check your connection and try again.").assertExists()
        rule.onNodeWithText("Retry").assertExists()
    }

    private fun showPast(
        past: List<ExpertBookingItem>,
        onOpenBooking: (String) -> Unit = {},
    ) {
        serve(upcoming = emptyList(), past = past)
        show(onOpenBooking = onOpenBooking)
        rule.onNodeWithText("Past").performClick()
        rule.waitForIdle()
    }

    private fun pastItem(
        id: String,
        status: String,
        hoursAgo: Long,
    ) = expertBookingItem(
        id = id,
        statusCode = status,
        start = now.minusSeconds(hoursAgo * 3600),
    )

    @Test
    fun pastTabShowsLabelledCards() {
        showPast(
            listOf(
                pastItem("p1", "completed", hoursAgo = 24),
                pastItem("p2", "declined", hoursAgo = 48),
                pastItem("p3", "expired", hoursAgo = 72),
            ),
        )
        assertEquals(3, rule.onAllNodesWithTag(EXPERT_PAST_CARD_TAG).fetchSemanticsNodes().size)
        rule.onNodeWithText("Completed").assertExists()
        rule.onNodeWithText("Cancelled").assertExists()
        rule.onNodeWithText("Missed").assertExists()
        assertEquals(3, rule.onAllNodesWithText("Topic: Algorithms").fetchSemanticsNodes().size)
        val date = formatShortDate(now.minusSeconds(24 * 3600).atZone(ZoneId.systemDefault()).toLocalDate(), Locale.US)
        assertTrue(rule.onAllNodesWithText(date, substring = true).fetchSemanticsNodes().isNotEmpty())
        assertTrue(rule.onAllNodesWithTag(EXPERT_BOOKINGS_JOIN_TAG).fetchSemanticsNodes().isEmpty())
        assertTrue(rule.onAllNodesWithTag(EXPERT_UPCOMING_CARD_TAG).fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun pastCardsAreNewestFirst() {
        showPast(
            listOf(
                pastItem("older", "completed", hoursAgo = 72),
                pastItem("newer", "cancelled", hoursAgo = 24),
            ),
        )
        val cards = rule.onAllNodesWithTag(EXPERT_PAST_CARD_TAG)
        cards[0].assertTextContains("Cancelled", substring = true)
        cards[1].assertTextContains("Completed", substring = true)
    }

    @Test
    fun pastCardOpensSummary() {
        val opened = mutableListOf<String>()
        showPast(listOf(pastItem("p1", "completed", hoursAgo = 24)), onOpenBooking = { opened += it })
        rule.onNodeWithTag(EXPERT_PAST_CARD_TAG).performClick()
        assertEquals(listOf("p1"), opened)
    }

    @Test
    fun pastFirstLoadShowsPastSkeleton() {
        api.expertBookingsHandler = { awaitCancellation() }
        show()
        rule.onNodeWithText("Past").performClick()
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Loading").assertExists()
        assertTrue(rule.onAllNodesWithTag(EXPERT_PAST_CARD_TAG).fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun emptyUpcomingOffersAvailabilityAndPast() {
        serve(upcoming = emptyList())
        var availability = 0
        val vm = viewModel()
        val chatEntry = testChatEntryViewModel(api, chatLaunch)
        rule.setContent {
            ClickMeTheme {
                ExpertBookingsScreen(
                    onOpenBooking = {},
                    onUpdateAvailability = { availability++ },
                    onReschedule = {},
                    viewModel = vm,
                    chatEntry = chatEntry,
                )
            }
        }
        rule.waitForIdle()
        rule.onNodeWithText("No Upcoming Sessions Yet").assertExists()
        val body =
            "Once a client books a session with you, it will appear here. " +
                "In the meantime, ensure your availability is up to date."
        rule.onNodeWithText(body).assertExists()
        rule.onNodeWithText("Update Availability").performClick()
        assertEquals(1, availability)
        rule.onNodeWithText("View Past History").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Past").assertIsSelected()
        rule.onNodeWithText("No past sessions yet").assertExists()
    }

    @Test
    fun pastCardsHaveNoMessage() {
        showPast(listOf(pastItem("p1", "completed", hoursAgo = 24)))
        assertEquals(1, rule.onAllNodesWithTag(EXPERT_PAST_CARD_TAG).fetchSemanticsNodes().size)
        rule.onAllNodesWithTag(EXPERT_BOOKINGS_MESSAGE_TAG).assertCountEquals(0)
        rule.onAllNodesWithContentDescription("Message Ada Lovelace").assertCountEquals(0)
    }

    @Test
    fun emptyPastShowsItsState() {
        showPast(emptyList())
        rule.onNodeWithText("No past sessions yet").assertExists()
        rule.onNodeWithText("Completed and cancelled sessions will appear here.").assertExists()
        assertTrue(rule.onAllNodesWithText("Update Availability").fetchSemanticsNodes().isEmpty())
        assertTrue(rule.onAllNodesWithText("View Past History").fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun emptyStatesStillPullToRefresh() {
        serve(upcoming = emptyList())
        show()
        val before = api.expertBookingsCalls
        rule.onNodeWithTag(EXPERT_BOOKINGS_STATE_TAG).assert(hasScrollAction())
        rule.onNodeWithTag(EXPERT_BOOKINGS_STATE_TAG).performTouchInput { swipeDown() }
        rule.waitForIdle()
        assertTrue(api.expertBookingsCalls > before)
    }

    private class NoticeCalls {
        var noticeConsumed = 0
        var refreshFailureConsumed = 0
        var refreshes = 0
        var retries = 0
        var rescheduleSentConsumed = 0
    }

    private fun showContent(
        state: ExpertBookingsUiState,
        calls: NoticeCalls = NoticeCalls(),
    ) {
        rule.setContent {
            ClickMeTheme {
                ExpertBookingsContent(
                    state = state,
                    joinNow = now,
                    onSelectTab = {},
                    onRefresh = { calls.refreshes++ },
                    onRetry = { calls.retries++ },
                    onOpenBooking = {},
                    onJoin = {},
                    onUpdateAvailability = {},
                    onNoticeConsumed = { calls.noticeConsumed++ },
                    onRefreshFailureConsumed = { calls.refreshFailureConsumed++ },
                    onRescheduleSentConsumed = { calls.rescheduleSentConsumed++ },
                )
            }
        }
        rule.waitForIdle()
    }

    private fun loadedState(
        notice: BookingsNotice? = null,
        refreshFailed: Boolean = false,
    ) = ExpertBookingsUiState(
        selectedTab = BookingsTab.Past,
        load = ExpertBookingsLoad.Loaded,
        notice = notice,
        refreshFailed = refreshFailed,
    )

    @Test
    fun sentNoticeShowsSnackbarOnce() {
        val calls = NoticeCalls()
        showContent(
            ExpertBookingsUiState(load = ExpertBookingsLoad.Loaded, rescheduleSent = true),
            calls,
        )
        rule.onNodeWithText("Reschedule request sent").assertExists()
        assertEquals(1, calls.rescheduleSentConsumed)
        assertEquals(0, calls.noticeConsumed)
        assertEquals(0, calls.refreshFailureConsumed)
    }

    @Test
    fun cancelNoticeShowsOnce() {
        val calls = NoticeCalls()
        showContent(loadedState(notice = BookingsNotice.Cancelled), calls)
        rule.onNodeWithText("Booking cancelled").assertExists()
        assertEquals(1, calls.noticeConsumed)
        assertEquals(0, calls.refreshFailureConsumed)
    }

    @Test
    fun refundNoticeShowsAmount() {
        val calls = NoticeCalls()
        showContent(loadedState(notice = BookingsNotice.CancelledWithRefund("\$75.00")), calls)
        rule.onNodeWithText("Booking cancelled. Your client will be refunded \$75.00.").assertExists()
        assertEquals(1, calls.noticeConsumed)
    }

    @Test
    fun refreshFailureShowsSnackbar() {
        val calls = NoticeCalls()
        showContent(loadedState(refreshFailed = true), calls)
        rule.onNodeWithText("Couldn't refresh. Check your connection and try again.").assertExists()
        assertEquals(1, calls.refreshFailureConsumed)
        assertEquals(0, calls.noticeConsumed)
    }

    @Test
    fun refreshFailedShowsRetry() {
        val calls = NoticeCalls()
        showContent(loadedState(refreshFailed = true), calls)
        rule.onNodeWithText("Couldn't refresh. Check your connection and try again.").assertExists()
        // Long duration: still up after the Short 4 s would have dismissed it.
        rule.mainClock.autoAdvance = false
        rule.mainClock.advanceTimeBy(5_000)
        rule.onNodeWithText("Retry").assertExists()
        rule.onNodeWithText("Retry").performClick()
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()
        assertEquals(1, calls.refreshes)
        assertEquals(0, calls.retries)
    }

    @Test
    fun listCells() {
        var state by mutableStateOf(ExpertBookingsUiState(load = ExpertBookingsLoad.Loading))
        val calls = NoticeCalls()
        rule.setContent {
            ClickMeTheme {
                ExpertBookingsContent(
                    state = state,
                    joinNow = now,
                    onSelectTab = {},
                    onRefresh = {},
                    onRetry = { calls.retries++ },
                    onOpenBooking = {},
                    onJoin = {},
                    onUpdateAvailability = {},
                    onNoticeConsumed = {},
                    onRefreshFailureConsumed = {},
                )
            }
        }
        rule.onNodeWithContentDescription("Loading").assertExists()

        state = state.copy(load = ExpertBookingsLoad.Failed(FieldError.Text("Boom")))
        rule.waitForIdle()
        rule.onNodeWithText("Couldn't load sessions").assertExists()
        rule.onNodeWithText("Retry").performClick()
        assertEquals(1, calls.retries)

        state = state.copy(load = ExpertBookingsLoad.Loaded, selectedTab = BookingsTab.Upcoming)
        rule.waitForIdle()
        rule.onNodeWithText("No Upcoming Sessions Yet").assertExists()

        state = state.copy(selectedTab = BookingsTab.Past)
        rule.waitForIdle()
        rule.onNodeWithText("No past sessions yet").assertExists()
    }

    @Test
    @Config(qualifiers = "w900dp-h600dp")
    fun capped() {
        serve()
        show()
        val bounds = rule.onNodeWithTag(EXPERT_UPCOMING_CARD_TAG).getUnclippedBoundsInRoot()
        assertTrue("width ${bounds.right - bounds.left}", (bounds.right - bounds.left).value <= 600f)
    }

    @Test
    fun noNoticeNoSnackbar() {
        val calls = NoticeCalls()
        showContent(loadedState(), calls)
        assertTrue(rule.onAllNodesWithText("Booking cancelled").fetchSemanticsNodes().isEmpty())
        assertEquals(0, calls.noticeConsumed)
        assertEquals(0, calls.refreshFailureConsumed)
    }
}
