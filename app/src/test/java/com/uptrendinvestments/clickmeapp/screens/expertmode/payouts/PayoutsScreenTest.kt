@file:OptIn(ExperimentalCoroutinesApi::class)

package com.uptrendinvestments.clickmeapp.screens.expertmode.payouts

import android.app.Application
import android.content.Intent
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.uptrendinvestments.clickmeapp.managers.RestrictedAction
import com.uptrendinvestments.clickmeapp.model.responses.ConnectLinkData
import com.uptrendinvestments.clickmeapp.model.responses.ConnectStatusData
import com.uptrendinvestments.clickmeapp.model.responses.ExpertBookingItem
import com.uptrendinvestments.clickmeapp.model.responses.ExpertBookingsPayload
import com.uptrendinvestments.clickmeapp.model.responses.PayoutSummaryData
import com.uptrendinvestments.clickmeapp.screens.expertmode.dashboard.ExpertFinancialRepository
import com.uptrendinvestments.clickmeapp.screens.expertmode.payouts.subviews.PAYOUTS_BALANCE_TAG
import com.uptrendinvestments.clickmeapp.screens.expertmode.payouts.subviews.PAYOUTS_CONTINUE_TAG
import com.uptrendinvestments.clickmeapp.screens.expertmode.upcomingbookings.ExpertBookingsRepository
import com.uptrendinvestments.clickmeapp.testing.FakeCallGuard
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.SignupTestHarness
import com.uptrendinvestments.clickmeapp.testing.connectStatus
import com.uptrendinvestments.clickmeapp.testing.expertBookingItem
import com.uptrendinvestments.clickmeapp.testing.payoutSummary
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.util.Money
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

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
        bookings: List<ExpertBookingItem> = emptyList(),
        hardFail: Boolean = false,
    ): Fixture {
        val h = SignupTestHarness(this, tmp, main.testDispatcher)
        h.signIn()
        h.api.connectStatusHandler = {
            if (hardFail) throw h.api.httpError(500, """{"status":"error","message":"Down"}""")
            status ?: throw h.api.httpError(404, """{"status":"error","message":"None"}""")
        }
        h.api.payoutSummaryHandler = {
            if (hardFail) throw h.api.httpError(500, """{"status":"error","message":"Down"}""")
            summary ?: throw h.api.httpError(400, """{"status":"error","code":"NO_PAYOUT_METHOD","message":"Set up"}""")
        }
        h.api.expertBookingsHandler = { ExpertBookingsPayload(bookings = bookings) }
        val vm = PayoutsViewModel(ExpertFinancialRepository(h.api), ExpertBookingsRepository(h.api), guard)
        return Fixture(vm, h.api)
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

    @Test
    fun setupShowsOnlyTheSetupCard() =
        runTest(main.testDispatcher) {
            val f = fixture(status = connectStatus(chargesEnabled = false, currentlyDue = listOf("external_account")))
            setContent(f)

            rule.onNodeWithText("Complete payout setup").assertExists()
            rule
                .onNodeWithText("Verify your identity and connect a bank account to start receiving your earnings.")
                .assertExists()
            rule.onNodeWithTag(PAYOUTS_CONTINUE_TAG).assertExists()
            rule.onNodeWithText("Set up payouts").assertExists()
            rule.onAllNodesWithText("Available balance").assertCountEquals(0)
            rule.onAllNodesWithText("Next payout").assertCountEquals(0)
            rule.onAllNodesWithText("Eligible for next payout").assertCountEquals(0)
        }

    @Test
    fun setupButtonOpensCustomTabOnce() =
        runTest(main.testDispatcher) {
            val f = fixture(status = connectStatus(chargesEnabled = false, currentlyDue = listOf("external_account")))
            f.api.connectRefreshLinkHandler = { ConnectLinkData("https://connect.stripe.com/setup/abc") }
            setContent(f)
            val application = ApplicationProvider.getApplicationContext<Application>()

            rule.onNodeWithTag(PAYOUTS_CONTINUE_TAG).performClick()
            rule.waitForIdle()

            val started: Intent? = shadowOf(application).nextStartedActivity
            assertNotNull(started)
            assertEquals("https://connect.stripe.com/setup/abc", started?.data?.toString())
            assertNull(shadowOf(application).nextStartedActivity)
            assertNull(f.vm.uiState.value.onboardingUrl)
            assertEquals(1, f.api.connectRefreshLinkCalls)
            assertEquals(listOf(RestrictedAction.SetUpPayouts), guard.attempts)
        }

    @Test
    fun mintFailureShowsDialogWithServerMessage() =
        runTest(main.testDispatcher) {
            val f = fixture(status = null, summary = null)
            f.api.connectOnboardHandler = {
                throw f.api.httpError(
                    403,
                    """{"status":"error","code":"CONNECT_NOT_RATIFIED","message":"Payouts are not open yet."}""",
                )
            }
            setContent(f)

            rule.onNodeWithTag(PAYOUTS_CONTINUE_TAG).performClick()

            rule.onNodeWithText("Couldn't open payout setup").assertExists()
            rule.onNodeWithText("Payouts are not open yet.").assertExists()
            rule.onNodeWithText("Got it").performClick()
            rule.onAllNodesWithText("Couldn't open payout setup").assertCountEquals(0)
        }

    @Test
    fun holdAndReadyCards() =
        runTest(main.testDispatcher) {
            val empty = payoutSummary(available = 0L, pending = 0L)
            val one = fixture(summary = empty, bookings = listOf(expertBookingItem("b1")))
            setContent(one)
            rule.onNodeWithText("Payment on hold").assertExists()
            rule.onNodeWithText("1 upcoming session. You'll be paid once your session completes.").assertExists()
            rule.onNodeWithText("Next payout").assertExists()
            rule.onNodeWithText("Payout method").assertExists()
            rule.onAllNodesWithText("Your available balance", substring = true).assertCountEquals(0)
        }

    @Test
    fun holdPluralCard() =
        runTest(main.testDispatcher) {
            val empty = payoutSummary(available = 0L, pending = 0L)
            val three = fixture(summary = empty, bookings = (1..3).map { expertBookingItem("b$it") })
            setContent(three)
            rule.onNodeWithText("3 upcoming sessions. You'll be paid once your sessions complete.").assertExists()
        }

    @Test
    fun readyCard() =
        runTest(main.testDispatcher) {
            val f = fixture(summary = payoutSummary(available = 0L, pending = 0L))
            setContent(f)
            rule.onNodeWithText("You're ready to receive payouts").assertExists()
            rule.onNodeWithText("Next payout").assertExists()
            rule.onAllNodesWithText("Payment on hold").assertCountEquals(0)
        }

    @Test
    fun negativeBalanceAndManualSchedule() =
        runTest(main.testDispatcher) {
            val f = fixture(summary = payoutSummary(available = -500L, pending = 0L, payoutMethod = null))
            setContent(f)

            rule.onNodeWithText(Money.format(-500L, "CAD")).assertExists()
            rule.onAllNodesWithText("Eligible for next payout").assertCountEquals(0)
            rule.onNodeWithText("Manual schedule").performScrollTo().assertExists()
            rule.onNodeWithText("—").assertExists()
        }

    @Test
    fun eligiblePillShowsForPositiveBalance() =
        runTest(main.testDispatcher) {
            val f = fixture(summary = payoutSummary(available = 100L))
            setContent(f)

            rule.onNodeWithText("Eligible for next payout").assertExists()
        }

    @Test
    fun failedShowsRetry() =
        runTest(main.testDispatcher) {
            val f = fixture(hardFail = true)
            setContent(f)

            rule.onNodeWithText("Couldn't load payouts").assertExists()
            rule.onNodeWithText("Down").assertExists()
            rule.onNodeWithText("Retry").assertExists()
        }
}
