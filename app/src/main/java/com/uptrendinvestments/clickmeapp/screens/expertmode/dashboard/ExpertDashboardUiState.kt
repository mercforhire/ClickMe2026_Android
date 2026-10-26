package com.uptrendinvestments.clickmeapp.screens.expertmode.dashboard

import com.uptrendinvestments.clickmeapp.model.responses.ExpertBookingItem
import com.uptrendinvestments.clickmeapp.model.responses.PayoutSummaryData
import com.uptrendinvestments.clickmeapp.model.responses.WeeklyEarningsData
import com.uptrendinvestments.clickmeapp.screens.login.FieldError

/**
 * Load state of the Dashboard's workload half. Both fetches (upcoming bookings and pending requests) are hard: a
 * failure of either gives [Failed] (D-13 port of iOS `forceLoad`). The financial fetches added by plan 11-14 are
 * soft and never change this state.
 */
sealed interface DashboardLoad {
    data object Loading : DashboardLoad

    data class Failed(
        val message: FieldError,
    ) : DashboardLoad

    data object Loaded : DashboardLoad
}

/**
 * State of the expert Dashboard. [pendingCount] is the size of the pending-requests list (RESEARCH C2), [firstName] is
 * null when no name is known (the hero then reads "Hello, there"), and [rescheduleCount] counts only reschedules the
 * client proposed. [isRefreshing] drives the pull-to-refresh indicator and [refreshFailed] the one-shot snackbar.
 *
 * The financial fields are soft: [weekly] and [payoutSummary] are null when their fetch failed (the card then reads
 * "—"), and [onboarding] is [PayoutOnboarding.Unknown] until the Connect status or the payout summary answers.
 * [onboardingUrl] is the single-use Stripe link, held in memory only until the screen opens it (T-11-44).
 */
data class ExpertDashboardUiState(
    val load: DashboardLoad = DashboardLoad.Loading,
    val firstName: String? = null,
    val pendingCount: Int = 0,
    val rescheduleCount: Int = 0,
    val todaysSessions: List<ExpertBookingItem> = emptyList(),
    val isRefreshing: Boolean = false,
    val refreshFailed: Boolean = false,
    val weekly: WeeklyEarningsData? = null,
    val onboarding: PayoutOnboarding = PayoutOnboarding.Unknown,
    val payoutSummary: PayoutSummaryData? = null,
    val isMintingLink: Boolean = false,
    val onboardingUrl: String? = null,
    val onboardingError: FieldError? = null,
)
