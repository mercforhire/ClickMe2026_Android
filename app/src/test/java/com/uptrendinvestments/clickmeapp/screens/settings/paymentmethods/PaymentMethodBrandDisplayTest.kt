package com.uptrendinvestments.clickmeapp.screens.settings.paymentmethods

import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.screens.settings.paymentmethods.subviews.brandDisplayNameRes
import com.uptrendinvestments.clickmeapp.screens.settings.paymentmethods.subviews.expiryParts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PaymentMethodBrandDisplayTest {
    @Test
    fun knownBrandsMapToTheirNames() {
        assertEquals(R.string.payment_methods_brand_visa, brandDisplayNameRes("visa"))
        assertEquals(R.string.payment_methods_brand_visa, brandDisplayNameRes("VISA"))
        assertEquals(R.string.payment_methods_brand_mastercard, brandDisplayNameRes("mastercard"))
        assertEquals(R.string.payment_methods_brand_amex, brandDisplayNameRes("amex"))
        assertEquals(R.string.payment_methods_brand_discover, brandDisplayNameRes("discover"))
        assertEquals(R.string.payment_methods_brand_diners, brandDisplayNameRes("diners"))
        assertEquals(R.string.payment_methods_brand_jcb, brandDisplayNameRes("jcb"))
        assertEquals(R.string.payment_methods_brand_unionpay, brandDisplayNameRes("unionpay"))
    }

    @Test
    fun unknownEmptyAndNullBrandsFallBackToCard() {
        assertEquals(R.string.payment_methods_brand_card, brandDisplayNameRes("unknown"))
        assertEquals(R.string.payment_methods_brand_card, brandDisplayNameRes("eftpos_au"))
        assertEquals(R.string.payment_methods_brand_card, brandDisplayNameRes(""))
        assertEquals(R.string.payment_methods_brand_card, brandDisplayNameRes(null))
    }

    @Test
    fun expiryIsZeroPaddedMonthAndTwoDigitYear() {
        assertEquals("09" to "28", expiryParts(9, 2028))
        assertEquals("12" to "31", expiryParts(12, 2031))
    }

    @Test
    fun expiryIsNullWhenEitherPartIsMissing() {
        assertNull(expiryParts(null, 2028))
        assertNull(expiryParts(9, null))
    }
}
