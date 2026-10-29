@file:OptIn(ExperimentalCoroutinesApi::class)

package com.uptrendinvestments.clickmeapp.screens.expertmode.payouts

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import com.uptrendinvestments.clickmeapp.model.responses.ConnectStatusData
import com.uptrendinvestments.clickmeapp.model.responses.ExpertBookingsPayload
import com.uptrendinvestments.clickmeapp.model.responses.PayoutSummaryData
import com.uptrendinvestments.clickmeapp.screens.expertmode.dashboard.ExpertFinancialRepository
import com.uptrendinvestments.clickmeapp.screens.expertmode.upcomingbookings.ExpertBookingsRepository
import com.uptrendinvestments.clickmeapp.testing.FakeCallGuard
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.SignupTestHarness
import com.uptrendinvestments.clickmeapp.testing.connectStatus
import com.uptrendinvestments.clickmeapp.testing.payoutSummary
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.util.Money
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** The real Payouts screen over the real ViewModel and repositories, with only the HTTP layer faked. */
@RunWith(RobolectricTestRunner::class)
class PayoutsScreenTest {
    @get:Rule(order = 0)
    val main = MainDispatcherRule(UnconfinedTestDispatcher())

    @get:Rule(order = 1)
    val tmp = TemporaryFolder()

    @get:Rule(order = 2)
    val rule = createComposeRule()

    private val guard = FakeCallGuard()

    private class Fixture(
        val vm: PayoutsViewModel,
        val api: FakeClickMeApi,
    )

    @Suppress("LongParameterList")
    private suspend fun TestScope.fixture(
        status: ConnectStatusData? = connectStatus(),
        summary: PayoutSummaryData? = payoutSummary(),
    ): Fixture {
        val h = SignupTestHarness(this, tmp, main.testDispatcher)
        h.signIn()
        h.api.connectStatusHandler = { status ?: throw h.api.httpError(404, """{"status":"error","message":"None"}""") }
        h.api.payoutSummaryHandler = {
            summary ?: throw h.api.httpError(400, """{"status":"error","code":"NO_PAYOUT_METHOD","message":"Set up"}""")
        }
        h.api.expertBookingsHandler = { ExpertBookingsPayload() }
        return Fixture(PayoutsViewModel(ExpertFinancialRepository(h.api), ExpertBookingsRepository(h.api), guard), h.api)
    }

    private fun setContent(f: Fixture) {
        rule.setContent {
            ClickMeTheme { PayoutsScreen(onBack = {}, viewModel = f.vm) }
        }
    }

    @Test
    fun heroShowsMinorUnitAmounts() =
        runTest(main.testDispatcher) {
            val f =
                fixture(
                    summary = payoutSummary(available = 12_500L, pending = 3_000L, nextPayoutDate = "2026-11-07"),
                )
            setContent(f)

            val available = Money.format(12_500L, "CAD")
            val pending = Money.format(3_000L, "CAD")
            rule.onNodeWithText("Available balance").assertExists()
            rule.onNodeWithText(available).assertExists()
            rule.onNodeWithText("$pending pending").assertExists()
            val description =
                rule
                    .onNodeWithTag(PAYOUTS_BALANCE_TAG)
                    .fetchSemanticsNode()
                    .config[SemanticsProperties.ContentDescription]
            assertEquals(listOf("Available balance $available, $pending pending"), description)
            rule.onNodeWithText("Next payout").performScrollTo().assertExists()
            rule.onNodeWithText("November 7, 2026").assertExists()
            rule.onNodeWithText("Payout method").assertExists()
            rule.onNodeWithText("Bank Transfer").assertExists()
            rule.onAllNodesWithText("Withdraw", substring = true, ignoreCase = true).assertCountEquals(0)
            rule.onAllNodesWithText("Cash out", substring = true, ignoreCase = true).assertCountEquals(0)
        }
}
