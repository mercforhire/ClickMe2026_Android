package com.uptrendinvestments.clickmeapp.screens.clientmode.payments

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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

    private fun showRow(
        item: ClientTransactionItem,
        width: Dp = 360.dp,
    ) {
        rule.setContent {
            ClickMeTheme {
                Box(Modifier.width(width)) {
                    ClientTransactionRow(item = item, onClick = {}, zone = ZoneOffset.UTC, locale = Locale.US)
                }
            }
        }
    }

    private fun money(minor: Long) = Money.format(minor, "CAD", Locale.US)

    private fun rowTexts(
        text: String,
        substring: Boolean = false,
    ) = rule.onAllNodesWithText(text, substring = substring, useUnmergedTree = true)

    @Test
    fun nullFieldsShowDashesAndTheRowStays() {
        showRow(clientTransaction(topic = null, expert = null, amount = null))

        assertEquals(1, rowCount())
        // The topic and the amount each show a dash; the expert's dash sits in the "{expert} · {date}" line.
        rowTexts("—").assertCountEquals(2)
        rowTexts("— · Aug 17, 2026").assertCountEquals(1)
        rowTexts(money(0), substring = true).assertCountEquals(0)
    }

    @Test
    fun zeroAmountIsNotADash() {
        showRow(clientTransaction(amount = 0L, status = "captured"))

        rowTexts(money(0)).assertCountEquals(1)
        rowTexts("—").assertCountEquals(0)
    }

    @Test
    fun partialRefundShowsItsLine() {
        showRow(clientTransaction(amount = 7500L, status = "partially_refunded", refunded = 2500L))

        rowTexts("Refunded ${money(2500)}").assertCountEquals(1)
    }

    @Test
    fun fullRefundAndNoRefundHaveNoRefundLine() {
        showRow(clientTransaction(amount = 7500L, status = "refunded", refunded = 7500L))
        rowTexts("Refunded ${money(7500)}").assertCountEquals(0)
        // The pill still says Refunded.
        rowTexts("Refunded").assertCountEquals(1)
    }

    @Test
    fun noRefundHasNoRefundLine() {
        showRow(clientTransaction(amount = 7500L, status = "captured", refunded = 0L))

        rowTexts("Refunded", substring = true).assertCountEquals(0)
    }

    @Test
    fun unknownStatusShowsANeutralDashPill() {
        showRow(clientTransaction(status = "something_new"))

        assertEquals(1, rowCount())
        rowTexts("—").assertCountEquals(1)
    }

    @Test
    fun everyPillNamesItsStatus() {
        val cases =
            listOf(
                Triple("captured", 7500L, "Paid"),
                Triple("requires_capture", 7500L, "Authorized (not yet charged)"),
                Triple("scheduled", 7500L, "Scheduled"),
                Triple("processing", 7500L, "Processing"),
                Triple("requires_payment_method", 7500L, "Payment needed"),
                Triple("refunded", 7500L, "Refunded"),
                Triple("partially_refunded", 7500L, "Partially refunded"),
                Triple("failed", 7500L, "Payment failed"),
                Triple("canceled", 7500L, "Canceled"),
                Triple("auth_expired", 7500L, "Authorization expired"),
                Triple("none", 7500L, "Processed"),
                Triple("none", 0L, "No payment"),
            )
        rule.setContent {
            ClickMeTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    cases.forEachIndexed { i, (status, amount, _) ->
                        ClientTransactionRow(
                            item = clientTransaction("b$i", status = status, amount = amount),
                            onClick = {},
                            zone = ZoneOffset.UTC,
                            locale = Locale.US,
                        )
                    }
                }
            }
        }

        cases.forEach { (_, _, label) -> rowTexts(label).assertCountEquals(1) }
    }

    @Test
    fun rowIsOneMergedButtonWithItsClickLabel() {
        showRow(clientTransaction())

        val config = rule.onNodeWithTag(CLIENT_TRANSACTION_ROW_TAG).fetchSemanticsNode().config
        assertEquals(
            listOf("Algorithms, Ada Lovelace, Aug 17, 2026, Paid, ${money(7500)}"),
            config[SemanticsProperties.ContentDescription],
        )
        assertEquals(Role.Button, config[SemanticsProperties.Role])
        assertEquals("Open booking", config[SemanticsActions.OnClick].label)
    }

    @Test
    fun partialRefundContinuesTheDescription() {
        showRow(clientTransaction(amount = 7500L, status = "partially_refunded", refunded = 2500L))

        val config = rule.onNodeWithTag(CLIENT_TRANSACTION_ROW_TAG).fetchSemanticsNode().config
        assertEquals(
            listOf(
                "Algorithms, Ada Lovelace, Aug 17, 2026, Partially refunded, ${money(7500)}, Refunded ${money(2500)}",
            ),
            config[SemanticsProperties.ContentDescription],
        )
    }

    @Test
    fun nullPartsReadUnavailable() {
        showRow(clientTransaction(topic = null, expert = null, amount = null))

        val description =
            rule
                .onNodeWithTag(CLIENT_TRANSACTION_ROW_TAG)
                .fetchSemanticsNode()
                .config[SemanticsProperties.ContentDescription]
                .single()
        assertEquals("unavailable, unavailable, Aug 17, 2026, Paid, unavailable", description)
        assertTrue(!description.contains("—"))
    }

    @Test
    fun longTextWrapsAndTheAmountStays() {
        showRow(
            clientTransaction(
                topic = "A very long session topic title that has to wrap over several lines in a narrow row",
                expert = "Professor Maximilian Alexander Featherstonehaugh-Cholmondeley",
                status = "requires_capture",
            ),
        )

        val row = rule.onNodeWithTag(CLIENT_TRANSACTION_ROW_TAG).getUnclippedBoundsInRoot()
        val amount = rowTexts(money(7500)).onFirst().getUnclippedBoundsInRoot()
        assertTrue("amount $amount outside row $row", amount.left >= row.left && amount.right <= row.right)
        assertTrue("amount has no width", amount.right - amount.left > 0.dp)
        // The label wraps in the left column rather than being cut off.
        rowTexts("Authorized (not yet charged)").assertCountEquals(1)
    }
}
