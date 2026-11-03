package com.uptrendinvestments.clickmeapp.stripe

import androidx.compose.ui.graphics.Color
import com.stripe.android.paymentsheet.PaymentSheet
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.testing.FakeStripePaymentMethodReader
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeDarkColorScheme
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ClickMePaymentSheetStyleTest {
    private val scheme = ClickMeDarkColorScheme
    private val muted = Color.Gray

    private fun expectedColors() =
        PaymentSheet.Colors(
            primary = scheme.primary,
            surface = scheme.surfaceContainerHigh,
            component = scheme.surfaceContainer,
            componentBorder = scheme.outlineVariant,
            componentDivider = scheme.outlineVariant,
            onComponent = scheme.onSurface,
            subtitle = muted,
            placeholderText = muted,
            onSurface = scheme.onSurface,
            appBarIcon = scheme.onSurface,
            error = scheme.error,
        )

    private fun expectedAppearance(colors: PaymentSheet.Colors = expectedColors()): PaymentSheet.Appearance {
        val buttonColors = PaymentSheet.PrimaryButtonColors(scheme.primary, scheme.onPrimary, scheme.primary)
        return PaymentSheet.Appearance
            .Builder()
            .colorsLight(colors)
            .colorsDark(colors)
            .shapes(PaymentSheet.Shapes(cornerRadiusDp = 16f, borderStrokeWidthDp = 1f))
            .typography(
                PaymentSheet.Typography
                    .Builder()
                    .fontResId(R.font.nunito)
                    .build(),
            ).primaryButton(
                PaymentSheet.PrimaryButton(
                    colorsLight = buttonColors,
                    colorsDark = buttonColors,
                    shape = PaymentSheet.PrimaryButtonShape(cornerRadiusDp = 28f),
                ),
            ).build()
    }

    /** Stripe's Appearance and Configuration members are module-internal, so read them through their mangled getters. */
    private fun Any.member(name: String): Any? =
        javaClass.methods
            .first { it.name == "get$name" || it.name.startsWith("get$name$") }
            .invoke(this)

    @Test
    fun `configuration carries the ClickMe merchant, no delayed methods, no Link, the customer and no button label`() {
        val appearance = PaymentSheet.Appearance()

        val actual = clickMeSheetConfiguration("cus_1", "ek_1", appearance)

        val expected =
            PaymentSheet.Configuration
                .Builder("ClickMe")
                .customer(PaymentSheet.CustomerConfiguration("cus_1", "ek_1"))
                .allowsDelayedPaymentMethods(false)
                .link(PaymentSheet.LinkConfiguration(PaymentSheet.LinkConfiguration.Display.Never))
                .appearance(appearance)
                .build()
        assertEquals(expected, actual)
        assertEquals("ClickMe", CLICKME_MERCHANT_DISPLAY_NAME)
        assertEquals("ClickMe", actual.member("MerchantDisplayName"))
        assertEquals(false, actual.member("AllowsDelayedPaymentMethods"))
        assertNull(actual.member("PrimaryButtonLabel"))
    }

    /** BUG-07: Link can dismiss without a callback or drop the chosen card on logout, so the sheet never offers it. */
    @Test
    fun `configuration never shows Link`() {
        val link = clickMeSheetConfiguration("cus_1", "ek_1", PaymentSheet.Appearance()).member("Link")

        assertEquals(PaymentSheet.LinkConfiguration.Display.Never, link?.member("Display"))
    }

    @Test
    fun `configuration carries the primary button label when one is given`() {
        val actual = clickMeSheetConfiguration("cus_1", "ek_1", PaymentSheet.Appearance(), "Save card")

        assertEquals("Save card", actual.member("PrimaryButtonLabel"))
        assertNotEquals(clickMeSheetConfiguration("cus_1", "ek_1", PaymentSheet.Appearance()), actual)
    }

    @Test
    fun `appearance forces one palette by using the same colors for light and dark`() {
        val appearance = clickMePaymentSheetAppearance(scheme, muted)

        assertEquals(appearance.member("ColorsLight"), appearance.member("ColorsDark"))
    }

    @Test
    fun `appearance takes primary, surface and error from the theme`() {
        val appearance = clickMePaymentSheetAppearance(scheme, muted)

        assertEquals(expectedColors(), appearance.member("ColorsDark"))
        val recolored = clickMePaymentSheetAppearance(scheme.copy(primary = Color.Red), muted)
        assertNotEquals(appearance.member("ColorsDark"), recolored.member("ColorsDark"))
    }

    @Test
    fun `appearance uses 16dp corners, a 1dp border, Nunito and a 28dp primary button`() {
        assertEquals(expectedAppearance(), clickMePaymentSheetAppearance(scheme, muted))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `fake reader returns its script, records secrets and can be held in flight`() =
        runTest {
            val reader = FakeStripePaymentMethodReader()
            assertEquals("pm_test", reader.paymentMethodIdOf("seti_1_secret_a"))

            reader.result = null
            assertNull(reader.paymentMethodIdOf("seti_2_secret_b"))
            assertEquals(listOf("seti_1_secret_a", "seti_2_secret_b"), reader.requestedSecrets)

            reader.result = "pm_gated"
            val gate = CompletableDeferred<Unit>()
            reader.gate = gate
            val pending = async { reader.paymentMethodIdOf("seti_3_secret_c") }
            assertTrue(pending.isActive)
            gate.complete(Unit)
            assertEquals("pm_gated", pending.await())
        }
}
