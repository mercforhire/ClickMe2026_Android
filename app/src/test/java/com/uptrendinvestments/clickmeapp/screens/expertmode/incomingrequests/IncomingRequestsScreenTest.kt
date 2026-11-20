package com.uptrendinvestments.clickmeapp.screens.expertmode.incomingrequests

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.uptrendinvestments.clickmeapp.model.responses.BookingRequestItem
import com.uptrendinvestments.clickmeapp.screens.expertmode.ExpertLaunchRequest
import com.uptrendinvestments.clickmeapp.screens.expertmode.IncomingRequestsNotice
import com.uptrendinvestments.clickmeapp.screens.expertmode.incomingrequests.subviews.INCOMING_REQUEST_CARD_TAG
import com.uptrendinvestments.clickmeapp.screens.expertmode.requestdecision.RequestDecisionKey
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.MutableClock
import com.uptrendinvestments.clickmeapp.testing.bookingRequestItem
import com.uptrendinvestments.clickmeapp.testing.breakdown
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException
import java.time.Duration
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class IncomingRequestsScreenTest {
    @get:Rule(order = 0)
    val main = MainDispatcherRule(UnconfinedTestDispatcher())

    @get:Rule(order = 1)
    val rule = createComposeRule()

    private val now = Instant.parse("2026-10-22T16:00:00Z")
    private val api = FakeClickMeApi()
    private val opened = mutableListOf<RequestDecisionKey>()

    private fun viewModel() =
        IncomingRequestsViewModel(
            repository = BookingRequestsRepository(api),
            launchRequest = ExpertLaunchRequest(),
            clock = MutableClock(now),
        )

    private fun showScreen(vararg rows: BookingRequestItem) {
        api.bookingRequestsHandler = { rows.toList() }
        rule.setContent {
            ClickMeTheme {
                IncomingRequestsScreen(
                    onBack = {},
                    onOpenRequest = { opened += it },
                    viewModel = viewModel(),
                )
            }
        }
    }

    private fun pending(expiresIn: Duration = Duration.ofHours(5)) =
        bookingRequestItem(
            id = "r1",
            start = now.plusSeconds(86_400),
            expiresAt = now.plus(expiresIn),
            earnings = 75.0,
            breakdown = breakdown(net = 63.75),
        )

    @Test
    fun requestCardsRenderFromTheApi() {
        showScreen(pending())

        rule.onNodeWithText("Ada Lovelace").assertExists()
        rule.onNodeWithText("Algorithms").assertExists()
        rule.onNodeWithText("Earnings").assertExists()
        rule.onNodeWithText("US\$63.75").assertExists()
        rule.onNodeWithText("Expires in 5h").assertExists()
    }

    @Test
    fun tapOpensDecisionWithRowData() {
        val row = pending()
        showScreen(row)

        rule.onNodeWithTag(INCOMING_REQUEST_CARD_TAG).performClick()

        assertEquals(
            listOf(
                RequestDecisionKey(
                    requestId = "r1",
                    isExpired = false,
                    expiresAtMillis = row.expiresAt!!.toEpochMilli(),
                    listEarningsMajor = 75.0,
                    listCurrency = "USD",
                ),
            ),
            opened,
        )
    }

    @Test
    fun expiredRowShowsBadgeAndOpensReadOnly() {
        showScreen(pending(expiresIn = Duration.ofHours(-1)))

        rule.onNodeWithText("Expired").assertExists()
        assertEquals(0, rule.onAllNodesWithText("Expires in", substring = true).fetchSemanticsNodes().size)
        assertEquals(1, rule.onAllNodesWithTag(INCOMING_REQUEST_CARD_TAG).fetchSemanticsNodes().size)

        rule.onNodeWithTag(INCOMING_REQUEST_CARD_TAG).performClick()

        assertEquals(true, opened.single().isExpired)
    }

    private fun showContent(
        state: IncomingRequestsUiState,
        calls: MutableList<String>,
    ) {
        rule.setContent {
            ClickMeTheme {
                IncomingRequestsContent(
                    state = state,
                    onBack = {},
                    onOpenRequest = {},
                    onRetry = { calls += "retry" },
                    onRefresh = { calls += "refresh" },
                    onNoticeConsumed = { calls += "notice" },
                    onRefreshFailureConsumed = { calls += "refreshFailure" },
                )
            }
        }
    }

    @Test
    fun theDeclinedNoticeShowsASnackbarAndIsConsumed() {
        val calls = mutableListOf<String>()
        showContent(IncomingRequestsUiState(notice = IncomingRequestsNotice.Declined), calls)

        rule.onNodeWithText("Request declined").assertExists()
        assertEquals(listOf("notice"), calls)
    }

    @Test
    fun aFailedPullShowsTheRefreshSnackbarAndIsConsumed() {
        val calls = mutableListOf<String>()
        showContent(IncomingRequestsUiState(refreshFailed = true), calls)

        rule.onNodeWithText("Couldn't refresh. Check your connection and try again.").assertExists()
        assertEquals(listOf("refreshFailure"), calls)
    }

    @Test
    fun refreshFailedShowsRetry() {
        val calls = mutableListOf<String>()
        showContent(
            IncomingRequestsUiState(refreshFailed = true, load = IncomingRequestsLoad.Loaded(emptyList())),
            calls,
        )

        rule.onNodeWithText("Couldn't refresh. Check your connection and try again.").assertExists()
        // Long duration: still up after the Short 4 s would have dismissed it.
        rule.mainClock.autoAdvance = false
        rule.mainClock.advanceTimeBy(5_000)
        rule.onNodeWithText("Retry").assertExists()
        rule.onNodeWithText("Retry").performClick()
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()

        assertEquals(listOf("refreshFailure", "refresh"), calls)
    }

    @Test
    fun listCells() {
        var state by mutableStateOf(IncomingRequestsUiState())
        val calls = mutableListOf<String>()
        rule.setContent {
            ClickMeTheme {
                IncomingRequestsContent(
                    state = state,
                    onBack = {},
                    onOpenRequest = {},
                    onRetry = { calls += "retry" },
                    onRefresh = {},
                    onNoticeConsumed = {},
                    onRefreshFailureConsumed = {},
                )
            }
        }
        rule.onNodeWithContentDescription("Loading").assertExists()

        state = state.copy(load = IncomingRequestsLoad.Failed(FieldError.Text("Boom")))
        rule.waitForIdle()
        rule.onNodeWithText("Couldn't load requests").assertExists()
        rule.onNodeWithText("Retry").performClick()
        assertEquals(listOf("retry"), calls)

        state = state.copy(load = IncomingRequestsLoad.Loaded(emptyList()))
        rule.waitForIdle()
        rule.onNodeWithText("No pending requests").assertExists()
    }

    @Test
    @Config(qualifiers = "w900dp-h600dp")
    fun capped() {
        showScreen(pending())

        val bounds = rule.onNodeWithTag(INCOMING_REQUEST_CARD_TAG).getUnclippedBoundsInRoot()
        assertTrue("width ${bounds.right - bounds.left}", (bounds.right - bounds.left).value <= 600f)
    }

    @Test
    fun emptyListShowsEmptyState() {
        showScreen()

        rule.onNodeWithText("No pending requests").assertExists()
        rule.onNodeWithText("New bookings will show up here.").assertExists()
        rule.onNodeWithText("Retry").assertDoesNotExist()
    }

    @Test
    fun failureShowsErrorWithRetry() {
        api.bookingRequestsHandler = { throw IOException("offline") }
        rule.setContent {
            ClickMeTheme {
                IncomingRequestsScreen(onBack = {}, onOpenRequest = { opened += it }, viewModel = viewModel())
            }
        }

        rule.onNodeWithText("Couldn't load requests").assertExists()
        rule.onNodeWithText("Check your connection and try again.").assertExists()

        api.bookingRequestsHandler = { listOf(pending()) }
        val callsBeforeRetry = api.bookingRequestsCalls
        rule.onNodeWithText("Retry").performClick()

        rule.onNodeWithText("Ada Lovelace").assertExists()
        assertEquals(callsBeforeRetry + 1, api.bookingRequestsCalls)
    }

    @Test
    fun firstLoadShowsSkeleton() {
        val gate = CompletableDeferred<List<BookingRequestItem>>()
        api.bookingRequestsHandler = { gate.await() }
        rule.setContent {
            ClickMeTheme {
                IncomingRequestsScreen(onBack = {}, onOpenRequest = { opened += it }, viewModel = viewModel())
            }
        }

        rule.onNodeWithContentDescription("Loading").assertExists()
        rule.onAllNodesWithTag(INCOMING_REQUEST_CARD_TAG).assertCountEquals(0)

        gate.complete(listOf(pending()))
        rule.onNodeWithText("Ada Lovelace").assertExists()
        rule.onNodeWithContentDescription("Loading").assertDoesNotExist()
    }
}
