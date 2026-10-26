package com.uptrendinvestments.clickmeapp.screens.expertmode.requestdecision

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.uptrendinvestments.clickmeapp.model.responses.FEE_SOURCE_EXPERT_DEFAULT
import com.uptrendinvestments.clickmeapp.model.responses.FEE_SOURCE_UNAVAILABLE
import com.uptrendinvestments.clickmeapp.screens.expertmode.requestdecision.subviews.REQUEST_EARNINGS_TAG
import com.uptrendinvestments.clickmeapp.screens.expertmode.requestdecision.subviews.RequestEarningsSection
import com.uptrendinvestments.clickmeapp.testing.breakdown
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
class RequestEarningsSectionTest {
    @get:Rule
    val rule = createComposeRule()

    private val us = Locale.US

    // region requestEarnings rules

    @Test
    fun bookingBreakdownIsAReceipt() {
        val result = requestEarnings(breakdown(), null, null, us)

        assertEquals(
            RequestEarningsUi.Receipt(
                clientPays = "$75.00",
                platformFee = "$11.25",
                commissionPctText = "15",
                net = "$63.75",
                estimated = false,
            ),
            result,
        )
    }

    @Test
    fun expertDefaultBreakdownIsAnEstimatedReceipt() {
        val result = requestEarnings(breakdown(feeSource = FEE_SOURCE_EXPERT_DEFAULT), null, null, us)

        assertEquals(true, (result as RequestEarningsUi.Receipt).estimated)
        assertEquals("$63.75", result.net)
    }

    @Test
    fun fractionalCommissionShowsOneDecimal() {
        val result = requestEarnings(breakdown(pct = 12.5), null, null, us) as RequestEarningsUi.Receipt

        assertEquals("12.5", result.commissionPctText)
    }

    @Test
    fun zeroCommissionIsARealReceiptNotThePendingBranch() {
        val result =
            requestEarnings(breakdown(fee = 0.0, net = 75.0, pct = 0.0), null, null, us) as RequestEarningsUi.Receipt

        assertNull(result.commissionPctText)
        assertEquals("$0.00", result.platformFee)
        assertEquals("$75.00", result.net)
    }

    @Test
    fun unavailableFeeSourceNeverShowsGrossAsEarnings() {
        val result =
            requestEarnings(
                breakdown(feeSource = FEE_SOURCE_UNAVAILABLE, fee = null, net = null, pct = null),
                null,
                null,
                us,
            )

        assertEquals(RequestEarningsUi.PricePending("$75.00"), result)
    }

    @Test
    fun nullNetWithABookingFeeSourceIsStillPending() {
        val result = requestEarnings(breakdown(net = null), null, null, us)

        assertEquals(RequestEarningsUi.PricePending("$75.00"), result)
    }

    @Test
    fun missingBreakdownFallsBackToTheListEarningsInTheListCurrency() {
        val result = requestEarnings(null, 40.0, "CAD", us)

        assertEquals(RequestEarningsUi.PricePending("CA$40.00"), result)
    }

    @Test
    fun zeroDecimalCurrencyHasNoDecimals() {
        val result =
            requestEarnings(
                breakdown(gross = 10_000.0, fee = 1_500.0, net = 8_500.0, currency = "JPY"),
                null,
                null,
                us,
            ) as RequestEarningsUi.Receipt

        assertEquals("¥10,000", result.clientPays)
        assertEquals("¥1,500", result.platformFee)
        assertEquals("¥8,500", result.net)
    }

    @Test
    fun noPriceAnywhereIsFree() {
        assertEquals(RequestEarningsUi.Free, requestEarnings(breakdown(gross = null, net = null), null, null, us))
        assertEquals(RequestEarningsUi.Free, requestEarnings(null, null, null, us))
    }

    @Test
    fun zeroGrossIsFree() {
        val result = requestEarnings(breakdown(gross = 0.0, fee = 0.0, net = 0.0, pct = 0.0), null, null, us)

        assertEquals(RequestEarningsUi.Free, result)
    }

    @Test
    fun commissionTextRules() {
        assertNull(formatCommissionPct(null, us))
        assertNull(formatCommissionPct(0.0, us))
        assertEquals("15", formatCommissionPct(15.0, us))
        assertEquals("12.5", formatCommissionPct(12.5, us))
    }

    // endregion

    // region RequestEarningsSection rendering

    private fun show(earnings: RequestEarningsUi) {
        rule.setContent { ClickMeTheme { RequestEarningsSection(earnings = earnings) } }
    }

    @Test
    fun receiptShowsTheThreeRows() {
        show(RequestEarningsUi.Receipt("$75.00", "$11.25", "15", "$63.75", estimated = false))

        listOf("Your Earnings", "Client pays", "$75.00", "Platform fee (15%)", "−$11.25", "You earn", "$63.75")
            .forEach { rule.onNodeWithText(it).assertExists() }
        rule.onNodeWithText("Estimated — fee applies when the booking is charged.").assertDoesNotExist()
        rule.onNodeWithTag(REQUEST_EARNINGS_TAG).assertExists()
    }

    @Test
    fun zeroCommissionReceiptReadsPlatformFeeWithoutAPercent() {
        show(RequestEarningsUi.Receipt("$75.00", "$0.00", null, "$75.00", estimated = false))

        rule.onNodeWithText("Platform fee").assertExists()
        rule.onNodeWithText("−$0.00").assertExists()
    }

    @Test
    fun estimatedReceiptPrefixesTheNetAndShowsTheCaption() {
        show(RequestEarningsUi.Receipt("$75.00", "$11.25", "15", "$63.75", estimated = true))

        rule.onNodeWithText("≈ $63.75").assertExists()
        rule.onNodeWithText("Estimated — fee applies when the booking is charged.").assertExists()
    }

    @Test
    fun pricePendingShowsSessionPriceAndNeverAnEarningsLabel() {
        show(RequestEarningsUi.PricePending("$75.00"))

        rule.onNodeWithText("Session price").assertExists()
        rule.onNodeWithText("$75.00").assertExists()
        rule.onNodeWithText("Payout amount pending").assertExists()
        rule.onNodeWithText("You earn").assertDoesNotExist()
    }

    @Test
    fun freeShowsFreeAndNoBreakdownRows() {
        show(RequestEarningsUi.Free)

        rule.onNodeWithText("Session price").assertExists()
        rule.onNodeWithText("Free").assertExists()
        rule.onNodeWithText("Client pays").assertDoesNotExist()
        rule.onNodeWithText("You earn").assertDoesNotExist()
        rule.onNodeWithText("Payout amount pending").assertDoesNotExist()
    }

    // endregion
}
