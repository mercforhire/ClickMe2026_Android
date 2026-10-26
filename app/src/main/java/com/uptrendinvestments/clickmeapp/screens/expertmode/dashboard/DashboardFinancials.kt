package com.uptrendinvestments.clickmeapp.screens.expertmode.dashboard

import com.uptrendinvestments.clickmeapp.model.responses.ConnectStatusData
import com.uptrendinvestments.clickmeapp.model.responses.PayoutSummaryData
import java.net.URI
import java.net.URISyntaxException
import java.util.Locale

/**
 * Whether the expert still has to set up payouts with Stripe Connect (D-09). Never persisted: it is recomputed from the
 * Connect status on every Dashboard load, so a Connect status the expert completes in the browser is picked up on the
 * next resume.
 */
sealed interface PayoutOnboarding {
    /**
     * Setup is needed. [useRefreshLink] is true when a connected account already exists (a refresh link resumes it)
     * and false when none exists yet (the first onboarding link creates it).
     */
    data class Required(
        val useRefreshLink: Boolean,
    ) : PayoutOnboarding

    /** Connect is fully set up, so the payout summary is meaningful. */
    data object Onboarded : PayoutOnboarding

    /** Neither the status nor the summary answered, so no card shows and the payout figures read "—". */
    data object Unknown : PayoutOnboarding
}

/**
 * Port of iOS `fetchPayoutSummary` and `hasIncompleteConnectAccount` (Pitfall 6). The Connect status is authoritative:
 * onboarding is required when charges are not enabled or something is currently due, and then a refresh link resumes
 * the existing account. Only when the status is unavailable does a 400 from the payout summary (`NO_PAYOUT_METHOD`,
 * `KYC_INCOMPLETE`) mean that no account exists yet, and then the first onboarding link is minted. A 400 summary never
 * overrides a status that loaded.
 */
fun resolvePayoutOnboarding(
    status: ConnectStatusData?,
    summary: PayoutSummaryData?,
    summaryWas400: Boolean,
): PayoutOnboarding =
    when {
        status != null ->
            if (!status.chargesEnabled || status.currentlyDue.isNotEmpty()) {
                PayoutOnboarding.Required(useRefreshLink = true)
            } else {
                PayoutOnboarding.Onboarded
            }
        summaryWas400 -> PayoutOnboarding.Required(useRefreshLink = false)
        summary != null -> PayoutOnboarding.Onboarded
        else -> PayoutOnboarding.Unknown
    }

/**
 * A minted Stripe Account Link may only leave the app when it is an https URL with a host (RESEARCH A4). This is
 * stricter than the generic Custom Tab launcher, which also accepts http. Anything that does not parse is rejected.
 */
fun isAllowedOnboardingUrl(url: String): Boolean =
    try {
        val uri = URI(url)
        uri.scheme?.lowercase(Locale.ROOT) == "https" && !uri.host.isNullOrBlank()
    } catch (_: URISyntaxException) {
        false
    }
