package com.uptrendinvestments.clickmeapp.screens.expertmode.payouts

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.uptrendinvestments.clickmeapp.testing.earningsTransaction
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.util.Money
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

    private fun show(state: TransactionsUiState) {
        rule.setContent {
            ClickMeTheme {
                TransactionsContent(
                    state = state,
                    zone = ZoneOffset.UTC,
                    locale = Locale.US,
                    onBack = {},
                    onRowVisible = {},
                    onRetry = {},
                    onRetryPage = {},
                    onRefresh = {},
                    onRefreshFailureConsumed = {},
                )
            }
        }
    }

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
}
