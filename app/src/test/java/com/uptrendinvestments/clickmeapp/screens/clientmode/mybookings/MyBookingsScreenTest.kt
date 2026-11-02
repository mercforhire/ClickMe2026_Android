package com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.components.booknote.PRIVATE_NOTE_FOOTER_TAG
import com.uptrendinvestments.clickmeapp.components.chat.MESSAGE_BUTTON_SPINNER_TAG
import com.uptrendinvestments.clickmeapp.model.responses.ClientBookingItem
import com.uptrendinvestments.clickmeapp.screens.clientmode.bookingnote.ClientBookingNoteKey
import com.uptrendinvestments.clickmeapp.screens.clientmode.bookingnote.noteKey
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.subviews.BOOKING_CARD_MESSAGE_TAG
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.subviews.BOOKING_CARD_RESCHEDULE_TAG
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.subviews.MY_BOOKINGS_JOIN_TAG
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.subviews.MyBookingsEmptyState
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.subviews.MyBookingsError
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.subviews.MyBookingsSkeleton
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.subviews.PAST_BOOKING_CARD_TAG
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.subviews.PAST_CARD_LEAVE_REVIEW_TAG
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.subviews.PastBookingCard
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.subviews.UPCOMING_BOOKING_CARD_TAG
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.subviews.UpcomingBookingCard
import com.uptrendinvestments.clickmeapp.screens.clientmode.writereview.ReviewOpener
import com.uptrendinvestments.clickmeapp.screens.clientmode.writereview.WriteReviewKey
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.testing.lineCountsOf
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
class MyBookingsScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private val zone: ZoneId = ZoneId.of("America/Toronto")
    private val locale: Locale = Locale.US

    private fun timeFormat() = SimpleDateFormat("h:mm a", Locale.US)

    private fun show(content: @Composable () -> Unit) {
        rule.setContent { ClickMeTheme { content() } }
    }

    private fun exists(text: String) = rule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()

    private fun existsTag(tag: String) = rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()

    private val defaultStart: Instant = Instant.parse("2026-10-24T18:00:00Z")

    // 2026-10-24T18:00Z is 2:00 PM to 3:00 PM in Toronto (EDT).
    private val dateText = "Oct 24, 2026"
    private val timeRange = "2:00 PM – 3:00 PM"

    /** Time text normalised for the narrow no-break space newer JDKs put before AM / PM. */
    private fun anyTextContains(fragment: String): Boolean =
        rule
            .onAllNodesWithText(fragment, substring = true)
            .fetchSemanticsNodes()
            .isNotEmpty()

    @Composable
    private fun UpcomingCard(
        item: ClientBookingItem,
        onClick: () -> Unit = {},
        joinEnabled: Boolean = false,
        onJoin: () -> Unit = {},
        messageBusy: Boolean = false,
        onMessage: () -> Unit = {},
        onReschedule: (() -> Unit)? = null,
    ) = UpcomingBookingCard(
        item,
        onClick,
        joinEnabled,
        onJoin,
        messageBusy = messageBusy,
        onMessage = onMessage,
        onReschedule = onReschedule,
        zone = zone,
        timeFormat = timeFormat(),
        locale = locale,
    )

    @Composable
    private fun PastCard(
        row: PastRow,
        onClick: () -> Unit = {},
        onPrivateNote: () -> Unit = {},
    ) = PastBookingCard(row, onClick, onPrivateNote, zone = zone, timeFormat = timeFormat(), locale = locale)

    private fun rescheduleItem(initiatedBy: String) =
        bookingItem(status = STATUS_PENDING_RESCHEDULE, reschedule = rescheduleDto(initiatedBy = initiatedBy))

    // Upcoming card

    @Test
    fun upcomingCard_showsExpertTopicDateAndTime() {
        show { UpcomingCard(bookingItem(status = "confirmed")) }
        rule.onNodeWithText("Ada Lovelace").assertExists()
        rule.onNodeWithText("Algorithms").assertExists()
        rule.onNodeWithText(dateText).assertExists()
        assertTrue(anyTextContains("2:00"))
        assertTrue(anyTextContains("3:00"))
    }

    @Test
    fun upcomingCard_outsideTheWindowShowsADisabledJoinWithTheHint() {
        show { UpcomingCard(bookingItem(status = "confirmed"), joinEnabled = false) }
        rule.onNodeWithTag(MY_BOOKINGS_JOIN_TAG).assertIsNotEnabled()
        val node = rule.onNodeWithTag(MY_BOOKINGS_JOIN_TAG).fetchSemanticsNode()
        assertEquals(
            "Available closer to the session start",
            node.config.getOrNull(SemanticsProperties.StateDescription),
        )
    }

    @Test
    fun upcomingCard_insideTheWindowShowsAnEnabledJoinThatDoesNotOpenTheDetails() {
        var joins = 0
        var clicks = 0
        show {
            UpcomingCard(
                bookingItem(status = "confirmed"),
                onClick = { clicks++ },
                joinEnabled = true,
                onJoin = { joins++ },
            )
        }
        rule.onNodeWithTag(MY_BOOKINGS_JOIN_TAG).assertIsEnabled().performClick()
        assertEquals(1, joins)
        assertEquals(0, clicks)
    }

    @Test
    fun upcomingCard_clickCallsOnClickOnce() {
        var clicks = 0
        show { UpcomingCard(bookingItem(), onClick = { clicks++ }) }
        rule.onNodeWithText("Algorithms").performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun upcomingCard_pendingApprovalShowsBannerAndNoJoin() {
        show { UpcomingCard(bookingItem(status = STATUS_PENDING_APPROVAL)) }
        rule.onNodeWithText("Waiting for expert to accept").assertExists()
        assertTrue(!existsTag(MY_BOOKINGS_JOIN_TAG))
    }

    @Test
    fun upcomingCard_expertRescheduleBadge() {
        show { UpcomingCard(rescheduleItem("expert")) }
        rule.onNodeWithText("Expert proposed a new time · Tap to review").assertExists()
    }

    @Test
    fun upcomingCard_clientRescheduleBadge() {
        show { UpcomingCard(rescheduleItem("client")) }
        rule.onNodeWithText("Reschedule pending expert's response").assertExists()
        assertTrue(!exists("Expert proposed a new time · Tap to review"))
    }

    @Test
    fun upcomingCard_noRescheduleShowsNoBadge() {
        show { UpcomingCard(bookingItem()) }
        assertTrue(!exists("Expert proposed a new time · Tap to review"))
        assertTrue(!exists("Reschedule pending expert's response"))
    }

    @Test
    fun upcomingCard_nullNameAndTopicFallBack() {
        show { UpcomingCard(bookingItem(expertName = null, topic = null)) }
        rule.onNodeWithText("Expert").assertExists()
        rule.onNodeWithText("Consultation").assertExists()
    }

    // Past card

    @Test
    fun pastCard_showsExpertTopicWhenAndBadge() {
        val row = PastRow(bookingItem(status = "declined"), pastStatusDisplay("declined")!!)
        show { PastCard(row) }
        rule.onNodeWithText("Ada Lovelace").assertExists()
        assertTrue(anyTextContains("Topic:"))
        assertTrue(anyTextContains("Algorithms"))
        assertTrue(anyTextContains("$dateText, 2:00"))
        rule.onNodeWithText("Cancelled").assertExists()
    }

    @Test
    fun pastCard_clickCallsOnClick() {
        var clicks = 0
        val row = PastRow(bookingItem(status = "completed"), pastStatusDisplay("completed")!!)
        show { PastCard(row, onClick = { clicks++ }) }
        rule.onNodeWithText("Ada Lovelace").performClick()
        assertEquals(1, clicks)
        rule.onNodeWithText("Completed").assertExists()
    }

    // States

    @Test
    fun emptyState_upcomingHasExploreAction() {
        var explored = 0
        show { MyBookingsEmptyState(BookingsTab.Upcoming, onExploreExperts = { explored++ }) }
        rule.onNodeWithText("No Bookings Yet!").assertExists()
        rule.onNodeWithText("Explore Experts").performClick()
        assertEquals(1, explored)
    }

    @Test
    fun emptyState_pastHasNoAction() {
        show { MyBookingsEmptyState(BookingsTab.Past, onExploreExperts = {}) }
        rule.onNodeWithText("No past bookings yet").assertExists()
        rule.onNodeWithText("Completed, cancelled and missed sessions will show up here.").assertExists()
        assertTrue(!exists("Explore Experts"))
    }

    @Test
    fun errorState_showsMessageAndRetry() {
        var retries = 0
        show { MyBookingsError(FieldError.Text("Boom"), onRetry = { retries++ }) }
        rule.onNodeWithText("Couldn't load bookings").assertExists()
        rule.onNodeWithText("Boom").assertExists()
        rule.onNodeWithText("Retry").performClick()
        assertEquals(1, retries)
    }

    @Test
    fun skeleton_exposesLoadingDescription() {
        show { MyBookingsSkeleton() }
        rule.onNodeWithContentDescription("Loading").assertExists()
    }

    @Test
    fun cards_exposeTags() {
        show {
            UpcomingCard(bookingItem())
            PastCard(PastRow(bookingItem(id = "b2", status = "missed"), pastStatusDisplay("missed")!!))
        }
        rule.onNodeWithTag(UPCOMING_BOOKING_CARD_TAG).assertExists()
        rule.onNodeWithTag(PAST_BOOKING_CARD_TAG).assertExists()
    }
    // Screen content

    private class Callbacks {
        var selectedTab: BookingsTab? = null
        var refreshes = 0
        var retries = 0
        var openedUpcoming: String? = null
        var openedPast: String? = null
        var explored = 0
        var noticeConsumed = 0
        var refreshFailureConsumed = 0
        var reviewThanksShown = 0
        var rescheduleSentShown = 0
        val reviewRequests = mutableListOf<WriteReviewKey>()
        val joined = mutableListOf<String>()
        val messaged = mutableListOf<String>()
        val rescheduled = mutableListOf<String>()
        val noteKeys = mutableListOf<ClientBookingNoteKey>()
    }

    @Composable
    private fun Content(
        state: MyBookingsUiState,
        calls: Callbacks = Callbacks(),
        joinNow: Instant = defaultStart.minusSeconds(600),
        messageBusyPeerId: String? = null,
        reviewedBookingIds: Set<String> = emptySet(),
    ) = MyBookingsContent(
        state = state,
        onSelectTab = { calls.selectedTab = it },
        onRefresh = { calls.refreshes++ },
        onRetry = { calls.retries++ },
        onOpenUpcoming = { calls.openedUpcoming = it },
        onOpenPast = { calls.openedPast = it },
        onExploreExperts = { calls.explored++ },
        onNoticeConsumed = { calls.noticeConsumed++ },
        onRefreshFailureConsumed = { calls.refreshFailureConsumed++ },
        joinNow = joinNow,
        onJoin = { calls.joined += it.bookingId },
        messageBusyPeerId = messageBusyPeerId,
        onMessage = { calls.messaged += it.bookingId },
        onLeaveReview = { calls.reviewRequests += it },
        reviewedBookingIds = reviewedBookingIds,
        onReviewThanksShown = { calls.reviewThanksShown++ },
        onReschedule = { calls.rescheduled += it },
        onPrivateNote = { calls.noteKeys += it },
        onRescheduleSentShown = { calls.rescheduleSentShown++ },
        zone = zone,
        timeFormat = timeFormat(),
        locale = locale,
    )

    private fun pastRow(
        id: String,
        status: String = "completed",
        preview: String? = null,
    ) = PastRow(bookingItem(id = id, status = status, privateNotePreview = preview), pastStatusDisplay(status)!!)

    private fun loaded(
        tab: BookingsTab = BookingsTab.Upcoming,
        upcoming: List<ClientBookingItem> = emptyList(),
        past: List<PastRow> = emptyList(),
        notice: BookingsNotice? = null,
        refreshFailed: Boolean = false,
        reviewThanks: Boolean = false,
        rescheduleSent: Boolean = false,
    ) = MyBookingsUiState(
        load = MyBookingsLoad.Loaded,
        lists = BookingLists(upcoming, past),
        selectedTab = tab,
        notice = notice,
        refreshFailed = refreshFailed,
        reviewThanks = reviewThanks,
        rescheduleSent = rescheduleSent,
    )

    @Test
    fun content_showsTitleAndBothTabs() {
        show { Content(loaded()) }
        rule.onNodeWithText("My Bookings").assertExists()
        rule.onNodeWithText("Upcoming").assertExists()
        rule.onNodeWithText("Past").assertExists()
        rule.onNodeWithText("Upcoming").assertIsSelected()
        rule.onNodeWithText("Past").assertIsNotSelected()
    }

    @Test
    fun content_tappingPastSelectsPastTab() {
        val calls = Callbacks()
        show { Content(loaded(), calls) }
        rule.onNodeWithText("Past").performClick()
        assertEquals(BookingsTab.Past, calls.selectedTab)
    }

    @Test
    fun content_loadingShowsSkeletonOnly() {
        show { Content(MyBookingsUiState()) }
        rule.onNodeWithContentDescription("Loading").assertExists()
        assertTrue(!existsTag(UPCOMING_BOOKING_CARD_TAG))
    }

    @Test
    @Config(qualifiers = "w411dp-h1200dp")
    fun content_upcomingTabShowsCardsAndOpensDetails() {
        val calls = Callbacks()
        show { Content(loaded(upcoming = listOf(bookingItem("b1"), bookingItem("b3"))), calls) }
        assertEquals(2, rule.onAllNodesWithTag(UPCOMING_BOOKING_CARD_TAG).fetchSemanticsNodes().size)
        rule.onAllNodesWithTag(UPCOMING_BOOKING_CARD_TAG)[0].performClick()
        assertEquals("b1", calls.openedUpcoming)
    }

    @Test
    fun content_pastTabShowsCardAndOpensSummary() {
        val calls = Callbacks()
        show { Content(loaded(tab = BookingsTab.Past, past = listOf(pastRow("b2"))), calls) }
        assertEquals(1, rule.onAllNodesWithTag(PAST_BOOKING_CARD_TAG).fetchSemanticsNodes().size)
        rule.onNodeWithTag(PAST_BOOKING_CARD_TAG).performClick()
        assertEquals("b2", calls.openedPast)
    }

    @Test
    fun content_emptyUpcomingOffersExplore() {
        val calls = Callbacks()
        show { Content(loaded(), calls) }
        rule.onNodeWithText("No Bookings Yet!").assertExists()
        rule.onNodeWithText("Explore Experts").performClick()
        assertEquals(1, calls.explored)
    }

    @Test
    fun content_emptyPastHasNoAction() {
        show { Content(loaded(tab = BookingsTab.Past)) }
        rule.onNodeWithText("No past bookings yet").assertExists()
        assertTrue(!exists("Explore Experts"))
    }

    @Test
    fun listCellsOneItemShowsTheRowWithoutEmptyState() {
        show { Content(loaded(upcoming = listOf(bookingItem("b1")))) }
        assertEquals(1, rule.onAllNodesWithTag(UPCOMING_BOOKING_CARD_TAG).fetchSemanticsNodes().size)
        assertTrue(!exists("No Bookings Yet!"))
    }

    @Test
    @Config(qualifiers = "w900dp-h1200dp")
    fun capped() {
        show { Content(loaded(upcoming = listOf(bookingItem("b1")))) }
        val card = rule.onNodeWithTag(UPCOMING_BOOKING_CARD_TAG).getUnclippedBoundsInRoot()
        assertTrue("card width ${card.right - card.left}", card.right - card.left <= 600.dp)
        // The list scroll area (and so pull-to-refresh) stays full width.
        val list = rule.onNodeWithTag(MY_BOOKINGS_LIST_TAG).getUnclippedBoundsInRoot()
        assertTrue(list.right - list.left > 600.dp)
    }

    @Test
    fun content_failedShowsErrorWithRetry() {
        val calls = Callbacks()
        show { Content(MyBookingsUiState(load = MyBookingsLoad.Failed(FieldError.Text("Boom"))), calls) }
        rule.onNodeWithText("Couldn't load bookings").assertExists()
        rule.onNodeWithText("Retry").performClick()
        assertEquals(1, calls.retries)
    }

    @Test
    fun content_refundNoticeShowsSnackbarAndIsConsumedOnce() {
        val calls = Callbacks()
        show { Content(loaded(notice = BookingsNotice.CancelledWithRefund("CA$12.50")), calls) }
        rule.onNodeWithText("Booking cancelled. CA$12.50 will be refunded").assertExists()
        assertEquals(1, calls.noticeConsumed)
    }

    @Test
    fun content_plainNoticeShowsCancelledSnackbar() {
        val calls = Callbacks()
        show { Content(loaded(notice = BookingsNotice.Cancelled), calls) }
        rule.onNodeWithText("Booking cancelled").assertExists()
        assertEquals(1, calls.noticeConsumed)
    }

    @Test
    fun content_refreshFailureShowsSnackbarAndIsConsumed() {
        val calls = Callbacks()
        show { Content(loaded(refreshFailed = true), calls) }
        rule.onNodeWithText("Couldn't refresh. Check your connection and try again.").assertExists()
        assertEquals(1, calls.refreshFailureConsumed)
    }

    @Test
    fun refreshFailureShowsRetryThatRefreshes() {
        val calls = Callbacks()
        show { Content(loaded(upcoming = listOf(bookingItem("b1")), refreshFailed = true), calls) }
        rule.onNodeWithText("Couldn't refresh. Check your connection and try again.").assertExists()
        rule.onNodeWithText("Retry").performClick()
        rule.waitForIdle()
        assertEquals(1, calls.refreshes)
        assertEquals(0, calls.retries)
        assertEquals(1, rule.onAllNodesWithTag(UPCOMING_BOOKING_CARD_TAG).fetchSemanticsNodes().size)
    }

    @Test
    fun content_offersOnlyTheIntendedActions() {
        val upcoming = listOf(bookingItem("b1"), bookingItem("b3", status = STATUS_PENDING_APPROVAL))
        val state = loaded(upcoming = upcoming, past = listOf(pastRow("b2")))
        var tab by mutableStateOf(BookingsTab.Upcoming)
        show { Content(state.copy(selectedTab = tab)) }
        // Reschedule and the note zone are offered on Upcoming cards (Phase 16.2 D-04 amended, Phase 16.4 D-07).
        assertTrue("Message Expert on Upcoming", !exists("Message Expert"))
        rule.runOnIdle { tab = BookingsTab.Past }
        rule.waitForIdle()
        // Private Note is on every Past card (D-07); Reschedule and Message never are.
        assertTrue("Private Note on Past", exists("Private Note"))
        listOf("Reschedule", "Message Expert", "Message").forEach {
            assertTrue("$it on Past", !exists(it))
        }
        assertTrue(!existsTag(BOOKING_CARD_MESSAGE_TAG))
    }

    // Join in the list (D-01 .. D-03)

    @Test
    fun content_aConfirmedCardTenMinutesBeforeStartHasAnEnabledJoinThatJoinsOnce() {
        val calls = Callbacks()
        show { Content(loaded(upcoming = listOf(bookingItem("b1"))), calls) }
        rule.onNodeWithTag(MY_BOOKINGS_JOIN_TAG).assertIsEnabled().performClick()
        assertEquals(listOf("b1"), calls.joined)
        assertEquals(null, calls.openedUpcoming)
    }

    @Test
    fun content_aConfirmedCardThreeHoursBeforeStartHasADisabledJoin() {
        show {
            Content(
                loaded(upcoming = listOf(bookingItem("b1"))),
                joinNow = defaultStart.minusSeconds(3 * 3600),
            )
        }
        rule.onNodeWithTag(MY_BOOKINGS_JOIN_TAG).assertIsNotEnabled()
    }

    @Test
    fun content_aRejoinableCompletedCardHasAnEnabledJoin() {
        val item =
            bookingItem(
                id = "r1",
                status = "completed",
                start = defaultStart.minusSeconds(2 * 3600),
                end = defaultStart.plusSeconds(300),
            )
        show { Content(loaded(upcoming = listOf(item)), joinNow = defaultStart) }
        rule.onNodeWithTag(MY_BOOKINGS_JOIN_TAG).assertIsEnabled()
    }

    @Test
    fun content_aPendingApprovalCardShowsTheBannerAndNoJoin() {
        show { Content(loaded(upcoming = listOf(bookingItem("b1", status = STATUS_PENDING_APPROVAL)))) }
        rule.onNodeWithText("Waiting for expert to accept").assertExists()
        assertTrue(!existsTag(MY_BOOKINGS_JOIN_TAG))
    }

    // Reschedule entry on the card (Phase 16.2 D-04, D-05, RS-C1)

    private val farOutStart: Instant = defaultStart.plusSeconds(3L * 24 * 3600)

    @Test
    @Config(qualifiers = "w411dp-h1200dp")
    fun farOutConfirmedCardShowsRescheduleRow() {
        show { Content(loaded(upcoming = listOf(bookingItem("b1", start = farOutStart)))) }
        rule.onNodeWithTag(MY_BOOKINGS_JOIN_TAG).assertExists()
        rule.onNodeWithContentDescription("Reschedule session with Ada Lovelace").assertExists()
        rule.onNodeWithTag(BOOKING_CARD_RESCHEDULE_TAG).assertExists()
        rule.onNodeWithText("Message").assertExists()
        val join = rule.onNodeWithTag(MY_BOOKINGS_JOIN_TAG).fetchSemanticsNode()
        val reschedule = rule.onNodeWithTag(BOOKING_CARD_RESCHEDULE_TAG).fetchSemanticsNode()
        val message = rule.onNodeWithTag(BOOKING_CARD_MESSAGE_TAG).fetchSemanticsNode()
        // Join is on its own, wider row; Reschedule sits at the start and Message at the end of the second row.
        assertTrue(join.boundsInRoot.bottom <= reschedule.boundsInRoot.top)
        assertTrue(join.size.width > reschedule.size.width)
        assertTrue(reschedule.boundsInRoot.right <= message.boundsInRoot.left)
        assertTrue(message.size.width > message.size.height)
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Config(qualifiers = "w384dp-h2400dp")
    fun rescheduleAndMessageFitOneLineAt384dp() {
        val confirmed = bookingItem("b1", start = farOutStart)
        val pending = bookingItem("b2", status = STATUS_PENDING_APPROVAL, start = farOutStart)
        show { Content(loaded(upcoming = listOf(confirmed, pending))) }

        assertEquals(listOf(1, 1), rule.lineCountsOf("Reschedule", BOOKING_CARD_RESCHEDULE_TAG))
        assertEquals(listOf(1, 1), rule.lineCountsOf("Message", BOOKING_CARD_MESSAGE_TAG))
        val reschedule = rule.onAllNodesWithTag(BOOKING_CARD_RESCHEDULE_TAG)[0].fetchSemanticsNode()
        val message = rule.onAllNodesWithTag(BOOKING_CARD_MESSAGE_TAG)[0].fetchSemanticsNode()
        assertEquals(reschedule.boundsInRoot.top, message.boundsInRoot.top, 1f)
        assertTrue(reschedule.boundsInRoot.right <= message.boundsInRoot.left)
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Config(qualifiers = "w360dp-h1200dp")
    fun rescheduleAndMessageFitOneLineAt360dp() {
        val confirmed = bookingItem("b1", start = farOutStart)
        val pending = bookingItem("b2", status = STATUS_PENDING_APPROVAL, start = farOutStart)
        show { Content(loaded(upcoming = listOf(confirmed, pending))) }

        assertEquals(listOf(1, 1), rule.lineCountsOf("Reschedule", BOOKING_CARD_RESCHEDULE_TAG))
        assertEquals(listOf(1, 1), rule.lineCountsOf("Message", BOOKING_CARD_MESSAGE_TAG))
        val reschedule = rule.onAllNodesWithTag(BOOKING_CARD_RESCHEDULE_TAG)[0].fetchSemanticsNode()
        val message = rule.onAllNodesWithTag(BOOKING_CARD_MESSAGE_TAG)[0].fetchSemanticsNode()
        assertTrue(reschedule.boundsInRoot.bottom <= message.boundsInRoot.top)
    }

    @Test
    fun rescheduleClickDoesNotOpenDetails() {
        val calls = Callbacks()
        show { Content(loaded(upcoming = listOf(bookingItem("b1", start = farOutStart))), calls) }
        rule.onNodeWithTag(BOOKING_CARD_RESCHEDULE_TAG).performScrollTo().performClick()
        assertEquals(listOf("b1"), calls.rescheduled)
        assertEquals(null, calls.openedUpcoming)
    }

    @Test
    fun keepsRescheduleWhenJoinClockPasses24h() {
        val firstNow = defaultStart.minusSeconds(600)
        val start = firstNow.plusSeconds(25L * 3600)
        var now by mutableStateOf(firstNow)
        show { Content(loaded(upcoming = listOf(bookingItem("b1", start = start))), joinNow = now) }
        rule.onNodeWithTag(BOOKING_CARD_RESCHEDULE_TAG).assertExists()

        now = start.minusSeconds(24L * 3600).plusSeconds(1)
        rule.waitForIdle()

        rule.onNodeWithTag(BOOKING_CARD_RESCHEDULE_TAG).assertExists()
        rule.onNodeWithTag(BOOKING_CARD_MESSAGE_TAG).assertExists()
    }

    @Test
    @Config(qualifiers = "w411dp-h1200dp")
    fun under24hCardShowsRescheduleRow() {
        val start = defaultStart.minusSeconds(600).plusSeconds(23L * 3600)
        show { Content(loaded(upcoming = listOf(bookingItem("b1", start = start)))) }
        val join = rule.onNodeWithTag(MY_BOOKINGS_JOIN_TAG).fetchSemanticsNode()
        val reschedule = rule.onNodeWithTag(BOOKING_CARD_RESCHEDULE_TAG).fetchSemanticsNode()
        val message = rule.onNodeWithTag(BOOKING_CARD_MESSAGE_TAG).fetchSemanticsNode()
        assertTrue(join.boundsInRoot.bottom <= reschedule.boundsInRoot.top)
        assertTrue(reschedule.boundsInRoot.right <= message.boundsInRoot.left)
        assertTrue(message.size.width > message.size.height)
    }

    @Test
    @Config(qualifiers = "w411dp-h1200dp")
    fun pendingApprovalCardShowsBannerThenRescheduleAndMessage() {
        val calls = Callbacks()
        val item = bookingItem("b1", status = STATUS_PENDING_APPROVAL, start = farOutStart)
        show { Content(loaded(upcoming = listOf(item)), calls) }
        rule.onNodeWithText("Waiting for expert to accept").assertExists()
        assertTrue(!existsTag(MY_BOOKINGS_JOIN_TAG))
        val banner = rule.onNodeWithText("Waiting for expert to accept", useUnmergedTree = true).fetchSemanticsNode()
        val reschedule = rule.onNodeWithTag(BOOKING_CARD_RESCHEDULE_TAG).fetchSemanticsNode()
        val message = rule.onNodeWithTag(BOOKING_CARD_MESSAGE_TAG).fetchSemanticsNode()
        assertTrue(banner.boundsInRoot.bottom <= reschedule.boundsInRoot.top)
        assertTrue(reschedule.boundsInRoot.right <= message.boundsInRoot.left)
        assertTrue(message.size.width > message.size.height)

        rule.onNodeWithTag(BOOKING_CARD_RESCHEDULE_TAG).performClick()
        assertEquals(listOf("b1"), calls.rescheduled)
        assertEquals(null, calls.openedUpcoming)
    }

    @Test
    fun pendingRescheduleCardHidesReschedule() {
        val item = rescheduleItem("expert").copy(startTime = farOutStart)
        show { Content(loaded(upcoming = listOf(item))) }
        assertTrue(!existsTag(BOOKING_CARD_RESCHEDULE_TAG))
        val message = rule.onNodeWithTag(BOOKING_CARD_MESSAGE_TAG).fetchSemanticsNode()
        assertEquals(message.size.width, message.size.height)
        rule.onNodeWithContentDescription("Message Ada Lovelace").assertExists()
    }

    @Test
    @Config(qualifiers = "w411dp-h1200dp")
    fun mixedListRendersBothLayouts() {
        val upcoming =
            listOf(
                bookingItem("b1", start = farOutStart),
                rescheduleItem("expert").copy(bookingId = "b2", startTime = farOutStart),
            )
        show { Content(loaded(upcoming = upcoming)) }
        assertEquals(1, rule.onAllNodesWithTag(BOOKING_CARD_RESCHEDULE_TAG).fetchSemanticsNodes().size)
        assertEquals(2, rule.onAllNodesWithTag(MY_BOOKINGS_JOIN_TAG).fetchSemanticsNodes().size)
    }

    @Test
    fun rescheduleSentShowsSnackbarOnce() {
        val calls = Callbacks()
        show { Content(loaded(rescheduleSent = true), calls) }
        rule.onNodeWithText("Reschedule request sent").assertExists()
        assertEquals(1, calls.rescheduleSentShown)
    }

    @Test
    fun largeFontStacksRescheduleAndMessage() {
        show {
            CompositionLocalProvider(LocalDensity provides Density(density = 1f, fontScale = 1.6f)) {
                Content(loaded(upcoming = listOf(bookingItem("b1", start = farOutStart))))
            }
        }
        val reschedule = rule.onNodeWithTag(BOOKING_CARD_RESCHEDULE_TAG).fetchSemanticsNode()
        val message = rule.onNodeWithTag(BOOKING_CARD_MESSAGE_TAG).fetchSemanticsNode()
        assertTrue(reschedule.boundsInRoot.bottom <= message.boundsInRoot.top)
    }

    // Message (D-14)

    @Test
    fun upcomingCardShowsMessageIconBesideJoin() {
        var messages = 0
        var clicks = 0
        show { UpcomingCard(bookingItem(), onClick = { clicks++ }, onMessage = { messages++ }) }
        rule.onNodeWithContentDescription("Message Ada Lovelace").assertExists()
        rule.onNodeWithTag(MY_BOOKINGS_JOIN_TAG).assertExists()
        rule.onNodeWithTag(BOOKING_CARD_MESSAGE_TAG).assertIsEnabled().performClick()
        assertEquals(1, messages)
        assertEquals(0, clicks)
    }

    @Test
    fun upcomingCardMessageIsAFixedSizeIconBesideAWeightedJoin() {
        show { UpcomingCard(bookingItem()) }
        val message = rule.onNodeWithTag(BOOKING_CARD_MESSAGE_TAG).fetchSemanticsNode()
        val join = rule.onNodeWithTag(MY_BOOKINGS_JOIN_TAG).fetchSemanticsNode()
        assertEquals(message.size.width, message.size.height)
        assertTrue(join.size.width > message.size.width)
        assertTrue(join.boundsInRoot.right <= message.boundsInRoot.left)
    }

    @Test
    fun pendingCardShowsTextMessage() {
        var messages = 0
        show { UpcomingCard(bookingItem(status = STATUS_PENDING_APPROVAL), onMessage = { messages++ }) }
        assertTrue(!existsTag(MY_BOOKINGS_JOIN_TAG))
        rule.onNodeWithText("Message").assertExists()
        rule.onNodeWithTag(BOOKING_CARD_MESSAGE_TAG).performClick()
        assertEquals(1, messages)
    }

    @Test
    @Config(qualifiers = "w411dp-h1200dp")
    fun busyCardShowsSpinnerOnThatCardOnly() {
        val upcoming = listOf(bookingItem("b1"), bookingItem("b3", expertId = "e2"))
        show { Content(loaded(upcoming = upcoming), messageBusyPeerId = "e2") }
        val spinners = rule.onAllNodesWithTag(MESSAGE_BUTTON_SPINNER_TAG, useUnmergedTree = true)
        assertEquals(1, spinners.fetchSemanticsNodes().size)
    }

    @Test
    @Config(qualifiers = "w411dp-h1200dp")
    fun contentMessageReportsTheTappedBooking() {
        val calls = Callbacks()
        show { Content(loaded(upcoming = listOf(bookingItem("b1"), bookingItem("b3"))), calls) }
        rule.onAllNodesWithTag(BOOKING_CARD_MESSAGE_TAG)[1].performScrollTo().performClick()
        assertEquals(listOf("b3"), calls.messaged)
        assertEquals(null, calls.openedUpcoming)
    }

    @Test
    fun pastCardsHaveNoMessage() {
        show { Content(loaded(tab = BookingsTab.Past, past = listOf(pastRow("b2")))) }
        assertTrue(!existsTag(BOOKING_CARD_MESSAGE_TAG))
        assertTrue(!exists("Message"))
    }

    // Past card Leave a review (D-18, D-19)

    private val pastTab = BookingsTab.Past

    @Test
    fun completedPastCardShowsPill() {
        val calls = Callbacks()
        show { Content(loaded(tab = pastTab, past = listOf(pastRow("b2"))), calls) }
        rule.onNodeWithTag(PAST_CARD_LEAVE_REVIEW_TAG).assertExists()
        rule.onNodeWithText("Leave a review").assertExists()

        rule.onNodeWithTag(PAST_CARD_LEAVE_REVIEW_TAG).performClick()

        assertEquals(
            listOf(WriteReviewKey("b2", "Ada Lovelace", null, ReviewOpener.MyBookings)),
            calls.reviewRequests,
        )
        assertEquals(null, calls.openedPast)

        rule.onNodeWithText("Ada Lovelace").performClick()
        assertEquals("b2", calls.openedPast)
        assertEquals(1, calls.reviewRequests.size)
    }

    @Test
    fun reviewThanksShowsSnackbar() {
        val calls = Callbacks()
        show { Content(loaded(tab = pastTab, past = listOf(pastRow("b2")), reviewThanks = true), calls) }
        rule.onNodeWithText("Thanks for your review").assertExists()
        assertEquals(1, calls.reviewThanksShown)
    }

    @Test
    fun noPillForCancelledMissedOrReviewed() {
        val calls = Callbacks()
        val past =
            listOf(
                pastRow("k1", "completed"),
                pastRow("r1", "completed"),
                pastRow("c1", "cancelled"),
                pastRow("m1", "missed"),
            )
        show { Content(loaded(tab = pastTab, past = past), calls, reviewedBookingIds = setOf("r1")) }
        assertEquals(1, rule.onAllNodesWithTag(PAST_CARD_LEAVE_REVIEW_TAG).fetchSemanticsNodes().size)
        // The one pill left belongs to k1 (the only completed row not reviewed).
        rule.onNodeWithTag(PAST_CARD_LEAVE_REVIEW_TAG).performClick()
        assertEquals(listOf("k1"), calls.reviewRequests.map { it.bookingId })
    }

    @Test
    fun pillAndCardAreSeparateActions() {
        var reviews = 0
        var opens = 0
        val row = pastRow("b2")
        show {
            PastBookingCard(
                row,
                onClick = { opens++ },
                onPrivateNote = {},
                onLeaveReview = { reviews++ },
                zone = zone,
                timeFormat = timeFormat(),
                locale = locale,
            )
        }
        val pill = rule.onNodeWithTag(PAST_CARD_LEAVE_REVIEW_TAG)
        pill.assertHasClickAction()
        rule.onNodeWithTag(PAST_BOOKING_CARD_TAG, useUnmergedTree = true).assertHasClickAction()
        val actions =
            rule
                .onNodeWithTag(PAST_BOOKING_CARD_TAG)
                .fetchSemanticsNode()
                .config
                .getOrNull(SemanticsActions.CustomActions)
                .orEmpty()
        val action = actions.single { it.label == "Leave a review" }
        rule.runOnUiThread { action.action() }
        assertEquals(1, reviews)
        assertEquals(0, opens)
    }

    @Test
    fun largeFontPillWraps() {
        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                ClickMeTheme {
                    PastBookingCard(
                        pastRow("b2"),
                        onClick = {},
                        onPrivateNote = {},
                        onLeaveReview = {},
                        zone = zone,
                        timeFormat = timeFormat(),
                        locale = locale,
                    )
                }
            }
        }
        rule.onNodeWithText("Leave a review").assertIsDisplayed()
        assertTrue(anyTextContains(dateText))
        rule.onNodeWithTag(PAST_CARD_LEAVE_REVIEW_TAG).assertIsDisplayed()
    }

    // Private note on the past card (Phase 16.4, D-07, D-08)

    @Test
    @Config(qualifiers = "w411dp-h2400dp")
    fun pastCardShowsPrivateNoteOnEveryStatus() {
        val statuses = listOf("completed", "cancelled", "declined", "missed", "expired")
        val past = statuses.map { pastRow("p-$it", it) }
        show { Content(loaded(tab = pastTab, past = past)) }
        assertEquals(statuses.size, rule.onAllNodesWithText("Private Note").fetchSemanticsNodes().size)
        // Leave a review stays completed-only.
        assertEquals(1, rule.onAllNodesWithText("Leave a review").fetchSemanticsNodes().size)
    }

    @Test
    fun pastNoteActionOpensTheNoteNotTheSummary() {
        val calls = Callbacks()
        val row = pastRow("b2", "cancelled")
        show { Content(loaded(tab = pastTab, past = listOf(row)), calls) }
        rule.onNodeWithText("Private Note").performClick()
        assertEquals(listOf(row.item.noteKey()), calls.noteKeys)
        assertEquals(null, calls.openedPast)

        rule.onNodeWithText("Ada Lovelace").performClick()
        assertEquals("b2", calls.openedPast)
        assertEquals(1, calls.noteKeys.size)
    }

    @Test
    fun pastFooterShowsTheServerPreview() {
        val calls = Callbacks()
        val row = pastRow("b2", preview = "Call back")
        show { Content(loaded(tab = pastTab, past = listOf(row)), calls) }
        rule.onNodeWithText("Call back").assertExists()
        rule.onNodeWithTag(PRIVATE_NOTE_FOOTER_TAG).performClick()
        assertEquals(listOf(row.item.noteKey()), calls.noteKeys)
        assertEquals(null, calls.openedPast)
    }

    @Test
    @Config(qualifiers = "w411dp-h1200dp")
    fun pastFooterIsHiddenForABlankPreview() {
        val past = listOf(pastRow("n1", preview = null), pastRow("n2", preview = "   "))
        show { Content(loaded(tab = pastTab, past = past)) }
        assertEquals(2, rule.onAllNodesWithTag(PAST_BOOKING_CARD_TAG).fetchSemanticsNodes().size)
        assertTrue(!existsTag(PRIVATE_NOTE_FOOTER_TAG))
    }

    @Test
    fun pastCardExposesPrivateNoteToTalkBack() {
        var notes = 0
        var opens = 0
        show { PastCard(pastRow("b2", "cancelled"), onClick = { opens++ }, onPrivateNote = { notes++ }) }
        val actions =
            rule
                .onNodeWithTag(PAST_BOOKING_CARD_TAG)
                .fetchSemanticsNode()
                .config
                .getOrNull(SemanticsActions.CustomActions)
                .orEmpty()
        val action = actions.single { it.label == "Private Note" }
        rule.runOnUiThread { action.action() }
        assertEquals(1, notes)
        assertEquals(0, opens)
    }

    @Test
    @Config(qualifiers = "w360dp-h1200dp")
    fun largeFontKeepsThePastControls() {
        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                ClickMeTheme {
                    PastBookingCard(
                        pastRow("b2"),
                        onClick = {},
                        onPrivateNote = {},
                        onLeaveReview = {},
                        zone = zone,
                        timeFormat = timeFormat(),
                        locale = locale,
                    )
                }
            }
        }
        rule.onNodeWithText("Private Note").assertIsDisplayed()
        rule.onNodeWithText("Leave a review").assertIsDisplayed()
        assertTrue(anyTextContains(dateText))
    }
}
