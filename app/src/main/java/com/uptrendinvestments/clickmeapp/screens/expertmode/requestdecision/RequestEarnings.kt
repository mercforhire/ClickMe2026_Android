package com.uptrendinvestments.clickmeapp.screens.expertmode.requestdecision

import com.uptrendinvestments.clickmeapp.model.responses.EarningsBreakdownDto
import com.uptrendinvestments.clickmeapp.model.responses.FEE_SOURCE_BOOKING
import com.uptrendinvestments.clickmeapp.model.responses.FEE_SOURCE_EXPERT_DEFAULT
import com.uptrendinvestments.clickmeapp.util.Money
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

/** What the "Your Earnings" section shows for one request. Every amount is already formatted for display. */
sealed interface RequestEarningsUi {
    /**
     * The three-row receipt. [commissionPctText] is null when the commission is zero or unknown, so the row reads
     * "Platform fee" with a zero amount. [estimated] is true when the server applied the expert's default commission
     * (the fee is confirmed only when the booking is charged).
     */
    data class Receipt(
        val clientPays: String,
        val platformFee: String,
        val commissionPctText: String?,
        val net: String,
        val estimated: Boolean,
    ) : RequestEarningsUi

    /** The net payout is unknown: show the session price and "Payout amount pending", never an earnings label. */
    data class PricePending(
        val sessionPrice: String,
    ) : RequestEarningsUi

    /** A free request: "Free" and no breakdown rows. */
    data object Free : RequestEarningsUi
}

private const val FALLBACK_CURRENCY = "CAD"
private const val COMMISSION_MAX_FRACTION_DIGITS = 1

/**
 * Picks the earnings card for a request from `earnings_breakdown` (EXPERT-EARNINGS-BREAKDOWN 3).
 *
 * Money comes only from [breakdown] (major units, already scaled for the currency) or from the tapped list row's
 * [listEarningsMajor], never from the legacy hundredths figure on the request detail (C4). Free is checked first,
 * then the receipt (needs a net amount and a `booking` or `expert_default` fee source), otherwise the price is shown
 * with "Payout amount pending".
 *
 * The client's gross price is never returned as the expert's earnings: when the net is unavailable (fee source
 * `unavailable`, no breakdown or a null net) the result is [RequestEarningsUi.PricePending]. Money checks use
 * `== null`, never truthiness, because a zero commission and a zero fee are real values.
 */
fun requestEarnings(
    breakdown: EarningsBreakdownDto?,
    listEarningsMajor: Double?,
    listCurrency: String?,
    locale: Locale,
): RequestEarningsUi {
    val currency = breakdown?.currency ?: listCurrency ?: FALLBACK_CURRENCY
    val gross = breakdown?.grossAmount
    val grossIsFree = gross == null || gross == 0.0
    val listIsFree = listEarningsMajor == null || listEarningsMajor == 0.0
    if (grossIsFree && listIsFree) return RequestEarningsUi.Free

    val net = breakdown?.netAmount
    val hasReceiptSource =
        breakdown?.feeSource == FEE_SOURCE_BOOKING || breakdown?.feeSource == FEE_SOURCE_EXPERT_DEFAULT
    if (breakdown != null && net != null && gross != null && hasReceiptSource) {
        return RequestEarningsUi.Receipt(
            clientPays = Money.formatMajor(gross, currency, locale),
            platformFee = Money.formatMajor(breakdown.platformFee ?: 0.0, currency, locale),
            commissionPctText = formatCommissionPct(breakdown.commissionPct, locale),
            net = Money.formatMajor(net, currency, locale),
            estimated = breakdown.feeSource == FEE_SOURCE_EXPERT_DEFAULT,
        )
    }
    return RequestEarningsUi.PricePending(Money.formatMajor(gross ?: listEarningsMajor ?: 0.0, currency, locale))
}

/** "15" for 15.0, "12.5" for 12.5, null for a missing or zero commission. */
fun formatCommissionPct(
    pct: Double?,
    locale: Locale = Locale.getDefault(),
): String? {
    if (pct == null || pct == 0.0) return null
    return NumberFormat
        .getNumberInstance(locale)
        .apply {
            minimumFractionDigits = 0
            maximumFractionDigits = COMMISSION_MAX_FRACTION_DIGITS
            roundingMode = RoundingMode.HALF_UP
            isGroupingUsed = false
        }.format(pct)
}
