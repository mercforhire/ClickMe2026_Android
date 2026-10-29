package com.uptrendinvestments.clickmeapp.screens.expertmode.payouts

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.testing.earningsTransaction
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
class TransactionsScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private fun show(
        state: TransactionsUiState,
        onRetry: () -> Unit = {},
        onRetryPage: () -> Unit = {},
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
                    onRefresh = {},
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
}
