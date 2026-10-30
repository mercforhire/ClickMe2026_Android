package com.uptrendinvestments.clickmeapp.screens.expertmode.payouts

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.testing.earningsTransaction
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.util.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.ZoneOffset
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
class TransactionsScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private fun show(
        state: TransactionsUiState,
        onRetry: () -> Unit = {},
        onRetryPage: () -> Unit = {},
        onRefresh: () -> Unit = {},
    ) {
        rule.setContent {
            ClickMeTheme {
                TransactionsContent(
                    state = state,
                    zone = ZoneOffset.UTC,
                    locale = Locale.US,
                    onBack = {},
                    onRowVisible = {},
                    onRetry = onRetry,
                    onRetryPage = onRetryPage,
                    onRefresh = onRefresh,
                    onRefreshFailureConsumed = {},
                )
            }
        }
    }

    private fun rowCount() = rule.onAllNodesWithTag(EARNINGS_ROW_TAG).fetchSemanticsNodes().size

    @Test
    fun rowShowsNetFromMinorUnits() {
        show(
            TransactionsUiState(
                load = TransactionsLoad.Loaded,
                rows = listOf(earningsTransaction(net = 6375L, currency = "USD")),
            ),
        )

        rule.onNodeWithText(Money.format(6375L, "USD", Locale.US)).assertExists()
        rule.onNodeWithText("Career Advice").assertExists()
        rule.onNodeWithText("Liam Carter · Jul 30, 2026").assertExists()
    }

    @Test
    fun emptyState() {
        show(TransactionsUiState(load = TransactionsLoad.Empty))

        rule.onNodeWithText("No activity yet").assertExists()
        rule.onNodeWithText("Your completed sessions will appear here as they're processed.").assertExists()
        assertEquals(0, rowCount())
    }

    @Test
    fun singleRow() {
        show(TransactionsUiState(load = TransactionsLoad.Loaded, rows = listOf(earningsTransaction())))

        assertEquals(1, rowCount())
    }

    @Test
    fun failedFirstPageShowsMessageAndRetry() {
        var retried = 0
        show(
            TransactionsUiState(load = TransactionsLoad.Failed(FieldError.Text("Down"))),
            onRetry = { retried++ },
        )

        rule.onNodeWithText("Couldn't load activity").assertExists()
        rule.onNodeWithText("Down").assertExists()
        rule.onNodeWithText("Retry").performClick()
        assertEquals(1, retried)
    }

    @Test
    fun pagingFooter() {
        show(
            TransactionsUiState(
                load = TransactionsLoad.Loaded,
                rows = listOf(earningsTransaction()),
                isPaging = true,
            ),
        )

        rule.onNodeWithText("Loading more…").assertExists()
    }

    @Test
    fun pageErrorFooter() {
        var retried = 0
        show(
            TransactionsUiState(
                load = TransactionsLoad.Loaded,
                rows = listOf(earningsTransaction()),
                pageError = FieldError.Text("Server down"),
            ),
            onRetryPage = { retried++ },
        )

        rule.onNodeWithText("Server down").assertExists()
        assertEquals(1, rowCount())
        rule.onNodeWithText("Retry").performClick()
        assertEquals(1, retried)
    }

    @Test
    fun refreshFailedShowsRetry() {
        var refreshed = 0
        show(
            TransactionsUiState(
                load = TransactionsLoad.Loaded,
                rows = listOf(earningsTransaction("e1"), earningsTransaction("e2")),
                refreshFailed = true,
            ),
            onRefresh = { refreshed++ },
        )

        rule.onNodeWithText("Couldn't refresh. Check your connection and try again.").assertExists()
        // The rows stay under the snackbar.
        assertEquals(2, rowCount())
        rule.onNodeWithText("Retry").performClick()
        rule.waitForIdle()
        assertEquals(1, refreshed)
    }

    @Test
    fun listCells() {
        show(TransactionsUiState(load = TransactionsLoad.Loading))

        rule.onNodeWithContentDescription("Loading").assertExists()
        rule.onAllNodesWithText("No activity yet").assertCountEquals(0)
    }

    @Test
    fun listCellsOneRowHasNoEmptyHeading() {
        show(TransactionsUiState(load = TransactionsLoad.Loaded, rows = listOf(earningsTransaction())))

        assertEquals(1, rowCount())
        rule.onAllNodesWithText("No activity yet").assertCountEquals(0)
    }

    @Test
    @Config(qualifiers = "w900dp-h1200dp")
    fun capped() {
        show(TransactionsUiState(load = TransactionsLoad.Loaded, rows = listOf(earningsTransaction())))

        val row = rule.onAllNodesWithTag(EARNINGS_ROW_TAG)[0].getUnclippedBoundsInRoot()
        assertTrue("row width ${row.right - row.left}", row.right - row.left <= 600.dp)
        // Centered: the row starts well in from the 900 dp window edge.
        assertTrue("row left ${row.left}", row.left >= 150.dp)
        // The list scroll area itself stays full width.
        val list = rule.onNodeWithTag(TRANSACTIONS_LIST_TAG).getUnclippedBoundsInRoot()
        assertTrue(list.right - list.left > 600.dp)
    }
}
