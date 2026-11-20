package com.uptrendinvestments.clickmeapp.screens.expertmode.payouts

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.uptrendinvestments.clickmeapp.model.responses.EarningsBreakdownDto
import com.uptrendinvestments.clickmeapp.model.responses.FEE_SOURCE_BOOKING
import com.uptrendinvestments.clickmeapp.model.responses.FEE_SOURCE_UNAVAILABLE
import com.uptrendinvestments.clickmeapp.screens.expertmode.requestdecision.RequestEarningsUi
import com.uptrendinvestments.clickmeapp.screens.expertmode.requestdecision.requestEarnings
import com.uptrendinvestments.clickmeapp.testing.earningsTransaction
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

/**
 * PAYOUT-03 guard: the two money conventions side by side. Ledger transactions are MINOR units (6375 is US$63.75);
 * the request `earnings_breakdown` is MAJOR units and a null net never turns into a figure (RESEARCH Pitfall 6).
 */
@RunWith(RobolectricTestRunner::class)
class EarningsUnitsRegressionTest {
    @get:Rule
    val rule = createComposeRule()

    private val us = Locale.US

    @Test
    fun transactionsAreMinorUnits() {
        assertEquals("US$63.75", Money.format(6375L, "USD", us))

        rule.setContent {
            ClickMeTheme {
                EarningsTransactionRow(
                    item = earningsTransaction(net = 6375L, currency = "USD"),
                    zone = ZoneOffset.UTC,
                    locale = us,
                )
            }
        }

        rule.onNodeWithText("US$63.75").assertExists()
    }

    @Test
    fun breakdownNullNetIsPricePending() {
        val unavailable =
            EarningsBreakdownDto(
                grossAmount = 75.0,
                netAmount = null,
                feeSource = FEE_SOURCE_UNAVAILABLE,
                currency = "USD",
            )

        assertEquals(RequestEarningsUi.PricePending("US$75.00"), requestEarnings(unavailable, null, null, us))
    }

    @Test
    fun breakdownNetIsMajorUnits() {
        val booked =
            EarningsBreakdownDto(
                grossAmount = 75.0,
                platformFee = 11.25,
                netAmount = 63.75,
                commissionPct = 15.0,
                currency = "USD",
                feeSource = FEE_SOURCE_BOOKING,
            )

        val result = requestEarnings(booked, null, null, us)

        assertTrue(result is RequestEarningsUi.Receipt)
        assertEquals("US$63.75", (result as RequestEarningsUi.Receipt).net)
    }
}
