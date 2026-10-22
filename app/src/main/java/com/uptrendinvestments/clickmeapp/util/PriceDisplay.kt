package com.uptrendinvestments.clickmeapp.util

import java.util.Locale

/**
 * What a price surface should show (D-21).
 *
 * Featured topics pass the top-level `is_free` + `hourly_rate`; details topics pass `price.is_free` +
 * `price.amount/currency`.
 */
sealed interface PriceDisplay {
    data object Free : PriceDisplay

    data class Paid(
        val text: String,
    ) : PriceDisplay

    data object Unknown : PriceDisplay

    companion object {
        /**
         * An explicit `isFree == true` wins. A missing flag counts as not free only when both an amount and a
         * currency exist; anything else is [Unknown].
         */
        fun of(
            isFree: Boolean?,
            amount: Long?,
            currency: String?,
            locale: Locale = Locale.getDefault(),
        ): PriceDisplay =
            when {
                isFree == true -> Free
                amount != null && currency != null -> Paid(Money.format(amount, currency, locale))
                else -> Unknown
            }
    }
}
