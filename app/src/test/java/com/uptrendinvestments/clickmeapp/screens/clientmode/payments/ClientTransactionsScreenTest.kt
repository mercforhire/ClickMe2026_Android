package com.uptrendinvestments.clickmeapp.screens.clientmode.payments

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.uptrendinvestments.clickmeapp.model.responses.ClientTransactionItem
import com.uptrendinvestments.clickmeapp.testing.clientTransaction
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.util.Money
import org.junit.Assert.assertEquals
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
}
