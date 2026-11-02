package com.uptrendinvestments.clickmeapp.screens.clientmode.payments

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import com.uptrendinvestments.clickmeapp.model.responses.ClientTransactionItem
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.testing.clientTransaction
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.util.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.ZoneOffset
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
class ClientTransactionsScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private fun show(
        state: ClientTransactionsUiState,
        onRowVisible: (Int) -> Unit = {},
        onRetry: () -> Unit = {},
        onRetryPage: () -> Unit = {},
        onRefresh: () -> Unit = {},
        onRefreshFailureConsumed: () -> Unit = {},
        onOpenBooking: (String) -> Unit = {},
    ) {
        rule.setContent {
            ClickMeTheme {
                ClientTransactionsContent(
                    state = state,
                    zone = ZoneOffset.UTC,
                    locale = Locale.US,
                    onBack = {},
                    onRowVisible = onRowVisible,
                    onRetry = onRetry,
                    onRetryPage = onRetryPage,
                    onRefresh = onRefresh,
                    onRefreshFailureConsumed = onRefreshFailureConsumed,
                    onOpenBooking = onOpenBooking,
                )
            }
        }
    }

    private fun loaded(vararg rows: ClientTransactionItem) =
        ClientTransactionsUiState(load = ClientTransactionsLoad.Loaded, rows = rows.toList())

    private fun rowCount() = rule.onAllNodesWithTag(CLIENT_TRANSACTION_ROW_TAG).fetchSemanticsNodes().size

    @Test
    fun titleAndRowsShow() {
        show(loaded(clientTransaction()))

        rule.onNodeWithText("Payment History").assertExists()
        rule.onNodeWithText("Algorithms").assertExists()
        rule.onNodeWithText("Ada Lovelace · Aug 17, 2026").assertExists()
        rule.onNodeWithText("Paid").assertExists()
        rule.onNodeWithText(Money.format(7500L, "CAD", Locale.US)).assertExists()
    }

    @Test
    fun rowTapReportsTheBookingForEveryStatus() {
        val statuses = listOf("captured", "canceled", "failed", "something_new")
        val tapped = mutableListOf<String>()
        val rows = statuses.mapIndexed { i, status -> clientTransaction("b$i", topic = "T$i", status = status) }
        show(loaded(*rows.toTypedArray()), onOpenBooking = { tapped += it })

        statuses.indices.forEach { i ->
            rule.onNodeWithTag(CLIENT_TRANSACTIONS_LIST_TAG).performScrollToNode(hasText("T$i"))
            rule.onNodeWithText("T$i").performClick()
        }

        assertEquals(listOf("b0", "b1", "b2", "b3"), tapped)
    }

    @Test
    fun rowsKeepServerOrder() {
        show(
            loaded(
                clientTransaction(bookingId = "b1", topic = "First"),
                clientTransaction(bookingId = "b2", topic = "Second"),
                clientTransaction(bookingId = "b3", topic = "Third"),
            ),
        )

        val tops =
            listOf("First", "Second", "Third").map {
                rule
                    .onNodeWithText(it)
                    .fetchSemanticsNode()
                    .boundsInRoot.top
            }
        assertEquals(tops.sorted(), tops)
    }

    @Test
    fun oneRowAndManyRowsUseTheSameRow() {
        show(loaded(clientTransaction(bookingId = "b1")))
        assertEquals(1, rowCount())
    }

    @Test
    fun manyRowsUseTheSameRow() {
        show(
            loaded(
                clientTransaction(bookingId = "b1"),
                clientTransaction(bookingId = "b2"),
                clientTransaction(bookingId = "b3"),
            ),
        )
        assertEquals(3, rowCount())
    }

    @Test
    fun loadingShowsTheSkeleton() {
        show(ClientTransactionsUiState(load = ClientTransactionsLoad.Loading))

        rule.onAllNodesWithContentDescription("Loading").assertCountEquals(1)
        assertEquals(0, rowCount())
        rule.onAllNodesWithText("No payments yet").assertCountEquals(0)
    }

    @Test
    fun emptyShowsNoPaymentsYet() {
        show(ClientTransactionsUiState(load = ClientTransactionsLoad.Empty))

        rule.onNodeWithText("No payments yet").assertExists()
        rule.onNodeWithText("Your paid sessions will appear here once you've booked with an expert.").assertExists()
        rule.onAllNodesWithText("Retry").assertCountEquals(0)
        assertEquals(0, rowCount())
    }

    @Test
    fun failedShowsRetry() {
        var retried = 0
        show(
            ClientTransactionsUiState(load = ClientTransactionsLoad.Failed(FieldError.Text("Down"))),
            onRetry = { retried++ },
        )

        rule.onNodeWithText("Couldn't load payments").assertExists()
        rule.onNodeWithText("Down").assertExists()
        rule.onNodeWithText("Retry").performClick()
        assertEquals(1, retried)
    }

    @Test
    fun pullWorksFromEmpty() {
        var refreshes = 0
        show(ClientTransactionsUiState(load = ClientTransactionsLoad.Empty), onRefresh = { refreshes++ })
        rule.onNode(hasScrollAction()).performTouchInput { swipeDown() }
        rule.waitForIdle()
        assertTrue("empty pull", refreshes > 0)
    }

    @Test
    fun pullWorksFromFailed() {
        var refreshes = 0
        show(
            ClientTransactionsUiState(load = ClientTransactionsLoad.Failed(FieldError.Text("Down"))),
            onRefresh = { refreshes++ },
        )
        rule.onNode(hasScrollAction()).performTouchInput { swipeDown() }
        rule.waitForIdle()
        assertTrue("failed pull", refreshes > 0)
    }

    @Test
    fun refreshFailedShowsTheSnackbar() {
        var consumed = 0
        var refreshed = 0
        show(
            loaded(clientTransaction("b1"), clientTransaction("b2")).copy(refreshFailed = true),
            onRefresh = { refreshed++ },
            onRefreshFailureConsumed = { consumed++ },
        )

        rule.onNodeWithText("Couldn't refresh. Check your connection and try again.").assertExists()
        assertEquals(2, rowCount())
        rule.onNodeWithText("Retry").performClick()
        rule.waitForIdle()
        assertEquals(1, refreshed)
        assertTrue("consumed", consumed >= 1)
    }

    @Test
    fun refreshingKeepsTheRows() {
        show(loaded(clientTransaction("b1")).copy(isRefreshing = true))

        assertEquals(1, rowCount())
    }

    @Test
    fun pagingShowsTheFooter() {
        show(loaded(clientTransaction()).copy(isPaging = true))

        rule.onNodeWithText("Loading more…").assertExists()
    }

    @Test
    fun pageErrorShowsTheFooter() {
        var retried = 0
        show(
            loaded(clientTransaction()).copy(pageError = FieldError.Text("Server down")),
            onRetryPage = { retried++ },
        )

        rule.onNodeWithText("Server down").assertExists()
        assertEquals(1, rowCount())
        rule.onNodeWithText("Retry").performClick()
        assertEquals(1, retried)
    }

    @Test
    fun noFooterAfterTheEndOfTheList() {
        show(loaded(clientTransaction()))

        rule.onAllNodesWithText("Loading more…").assertCountEquals(0)
        rule.onAllNodesWithText("Retry").assertCountEquals(0)
        assertEquals(1, rowCount())
    }

    @Test
    fun scrollingNearTheEndReportsTheIndex() {
        val seen = mutableListOf<Int>()
        val rows = (0 until 12).map { clientTransaction("b$it", topic = "Row $it") }
        show(loaded(*rows.toTypedArray()), onRowVisible = { seen += it })

        rule.onNodeWithTag(CLIENT_TRANSACTIONS_LIST_TAG).performScrollToNode(hasText("Row 11"))
        rule.waitForIdle()

        assertTrue("seen $seen", seen.any { it >= rows.size - 3 })
    }
}
