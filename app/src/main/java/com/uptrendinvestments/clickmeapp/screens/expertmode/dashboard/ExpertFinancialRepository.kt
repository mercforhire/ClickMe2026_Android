package com.uptrendinvestments.clickmeapp.screens.expertmode.dashboard

import com.uptrendinvestments.clickmeapp.model.responses.ConnectStatusData
import com.uptrendinvestments.clickmeapp.model.responses.PayoutSummaryData
import com.uptrendinvestments.clickmeapp.model.responses.WeeklyEarningsData
import com.uptrendinvestments.clickmeapp.network.ClickMeApi
import com.uptrendinvestments.clickmeapp.network.apiCall
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The signed-in expert's weekly earnings, payout balance and Stripe Connect state. Mirrors iOS
 * `Expert Mode/Dashboard`. Every call throws [com.uptrendinvestments.clickmeapp.network.ApiException] only.
 *
 * Stateless by design: nothing is cached, because nothing clears singletons on logout. The onboarding URL is
 * returned to the caller and never stored, logged or persisted (threat T-11-44).
 */
@Singleton
class ExpertFinancialRepository
    @Inject
    constructor(
        private val api: ClickMeApi,
    ) {
        /** `GET /expert/earnings/weekly`: the current week, in MINOR units. */
        suspend fun weeklyEarnings(): WeeklyEarningsData = apiCall { api.getWeeklyEarnings() }.data

        /** `GET /expert/payouts/summary`: 400 `NO_PAYOUT_METHOD` or `KYC_INCOMPLETE` until Connect is set up. */
        suspend fun payoutSummary(): PayoutSummaryData = apiCall { api.getPayoutSummary() }.data

        /** `GET /expert/connect/status`: 404 when no connected account exists yet. */
        suspend fun connectStatus(): ConnectStatusData = apiCall { api.getConnectStatus() }.data

        /** A fresh single-use Stripe link: `refresh-link` when [refresh] is true, otherwise `onboard`. */
        suspend fun onboardingLink(refresh: Boolean): String =
            apiCall { if (refresh) api.refreshConnectLink() else api.startConnectOnboarding() }.data.url
    }
