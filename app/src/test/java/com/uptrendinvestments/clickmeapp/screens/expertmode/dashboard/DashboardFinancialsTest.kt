package com.uptrendinvestments.clickmeapp.screens.expertmode.dashboard

import com.uptrendinvestments.clickmeapp.model.responses.ConnectStatusData
import com.uptrendinvestments.clickmeapp.model.responses.PayoutSummaryData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardFinancialsTest {
    private val complete = ConnectStatusData(chargesEnabled = true, currentlyDue = emptyList())
    private val summary = PayoutSummaryData(availableAmount = 5000, currency = "CAD")

    @Test
    fun completeStatusIsOnboarded() {
        assertEquals(PayoutOnboarding.Onboarded, resolvePayoutOnboarding(complete, null, summaryWas400 = false))
    }

    @Test
    fun chargesDisabledNeedsARefreshLink() {
        val status = ConnectStatusData(chargesEnabled = false, currentlyDue = emptyList())
        assertEquals(
            PayoutOnboarding.Required(useRefreshLink = true),
            resolvePayoutOnboarding(status, null, summaryWas400 = false),
        )
    }

    @Test
    fun currentlyDueNeedsARefreshLink() {
        val status = ConnectStatusData(chargesEnabled = true, currentlyDue = listOf("external_account"))
        assertEquals(
            PayoutOnboarding.Required(useRefreshLink = true),
            resolvePayoutOnboarding(status, null, summaryWas400 = false),
        )
    }

    @Test
    fun noStatusAnd400SummaryNeedsFirstOnboarding() {
        assertEquals(
            PayoutOnboarding.Required(useRefreshLink = false),
            resolvePayoutOnboarding(null, null, summaryWas400 = true),
        )
    }

    @Test
    fun noStatusButASummaryIsOnboarded() {
        assertEquals(PayoutOnboarding.Onboarded, resolvePayoutOnboarding(null, summary, summaryWas400 = false))
    }

    @Test
    fun noStatusNoSummaryNot400IsUnknown() {
        assertEquals(PayoutOnboarding.Unknown, resolvePayoutOnboarding(null, null, summaryWas400 = false))
    }

    @Test
    fun a400SummaryNeverOverridesACompleteStatus() {
        assertEquals(PayoutOnboarding.Onboarded, resolvePayoutOnboarding(complete, null, summaryWas400 = true))
    }

    @Test
    fun anIncompleteStatusWinsOverALoadedSummary() {
        val status = ConnectStatusData(chargesEnabled = false, currentlyDue = listOf("x"))
        assertEquals(
            PayoutOnboarding.Required(useRefreshLink = true),
            resolvePayoutOnboarding(status, summary, summaryWas400 = false),
        )
    }

    @Test
    fun httpsWithAHostIsAllowed() {
        assertTrue(isAllowedOnboardingUrl("https://connect.stripe.com/setup/x"))
        assertTrue(isAllowedOnboardingUrl("HTTPS://connect.stripe.com/setup/x"))
    }

    @Test
    fun plainHttpIsRejected() {
        assertFalse(isAllowedOnboardingUrl("http://connect.stripe.com/setup/x"))
    }

    @Test
    fun scriptAndIntentSchemesAreRejected() {
        assertFalse(isAllowedOnboardingUrl("javascript:alert(1)"))
        assertFalse(isAllowedOnboardingUrl("intent://scan/#Intent;scheme=zxing;end"))
        assertFalse(isAllowedOnboardingUrl("file:///sdcard/x"))
    }

    @Test
    fun anHttpsUrlWithoutAHostIsRejected() {
        assertFalse(isAllowedOnboardingUrl("https://"))
        assertFalse(isAllowedOnboardingUrl("https:///path"))
    }

    @Test
    fun malformedStringsAreRejected() {
        assertFalse(isAllowedOnboardingUrl("not a url at all"))
        assertFalse(isAllowedOnboardingUrl(""))
    }
}
