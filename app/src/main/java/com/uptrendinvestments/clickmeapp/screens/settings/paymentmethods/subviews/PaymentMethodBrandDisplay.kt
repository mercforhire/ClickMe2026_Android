package com.uptrendinvestments.clickmeapp.screens.settings.paymentmethods.subviews

import androidx.annotation.StringRes
import com.uptrendinvestments.clickmeapp.R
import java.util.Locale

private const val YEAR_DIGITS = 2
private const val YEAR_MODULUS = 100

/**
 * Display name for the backend's normalized lowercase card brand. Unknown brands, empty and null fall back to
 * "Card". Top-level functions only: the iOS brand palette collapses into theme tokens.
 */
@StringRes
fun brandDisplayNameRes(raw: String?): Int =
    when (raw?.lowercase(Locale.ROOT)) {
        "visa" -> R.string.payment_methods_brand_visa
        "mastercard" -> R.string.payment_methods_brand_mastercard
        "amex" -> R.string.payment_methods_brand_amex
        "discover" -> R.string.payment_methods_brand_discover
        "diners" -> R.string.payment_methods_brand_diners
        "jcb" -> R.string.payment_methods_brand_jcb
        "unionpay" -> R.string.payment_methods_brand_unionpay
        else -> R.string.payment_methods_brand_card
    }

/** Zero-padded month and two-digit year, or null when either part is missing. */
fun expiryParts(
    expMonth: Int?,
    expYear: Int?,
): Pair<String, String>? {
    if (expMonth == null || expYear == null) return null
    val month = String.format(Locale.ROOT, "%02d", expMonth)
    val year = String.format(Locale.ROOT, "%0${YEAR_DIGITS}d", expYear % YEAR_MODULUS)
    return month to year
}
