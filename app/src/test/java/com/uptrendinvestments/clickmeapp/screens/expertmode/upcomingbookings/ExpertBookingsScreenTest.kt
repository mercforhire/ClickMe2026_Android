package com.uptrendinvestments.clickmeapp.screens.expertmode.upcomingbookings

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.SavedStateHandle
import com.uptrendinvestments.clickmeapp.managers.CallStartRequest
import com.uptrendinvestments.clickmeapp.managers.JoinClock
import com.uptrendinvestments.clickmeapp.model.responses.ExpertBookingItem
import com.uptrendinvestments.clickmeapp.model.responses.ExpertBookingsPayload
import com.uptrendinvestments.clickmeapp.screens.expertmode.ExpertLaunchRequest
import com.uptrendinvestments.clickmeapp.screens.expertmode.upcomingbookings.subviews.EXPERT_BOOKINGS_JOIN_TAG
import com.uptrendinvestments.clickmeapp.screens.expertmode.upcomingbookings.subviews.EXPERT_UPCOMING_CARD_TAG
import com.uptrendinvestments.clickmeapp.testing.FakeCallStarter
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.FakeRealtimeConnection
import com.uptrendinvestments.clickmeapp.testing.MutableClock
import com.uptrendinvestments.clickmeapp.testing.expertBookingItem
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
class ExpertBookingsScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private val now = Instant.parse("2026-10-24T17:00:00Z")
    private val clock = MutableClock(now)
    private val api = FakeClickMeApi()
    private val callStarter = FakeCallStarter()

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
        )

    private fun show(onOpenBooking: (String) -> Unit = {}) {
        val vm = viewModel()
        rule.setContent {
            ClickMeTheme {
                ExpertBookingsScreen(onOpenBooking = onOpenBooking, onUpdateAvailability = {}, viewModel = vm)
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
}
