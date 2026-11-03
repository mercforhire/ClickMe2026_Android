package com.uptrendinvestments.clickmeapp.stripe

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.stripe.android.paymentsheet.PaymentSheet
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.ui.theme.extras

const val CLICKME_MERCHANT_DISPLAY_NAME = "ClickMe"

private const val SHEET_CORNER_RADIUS_DP = 16f
private const val SHEET_BORDER_STROKE_DP = 1f
private const val PRIMARY_BUTTON_CORNER_RADIUS_DP = 28f

/**
 * Ports the iOS `MakeABookingStripePaymentSheet` configuration. One builder serves the add-card PaymentSheet
 * (07-09) and the booking FlowController (07-13). [primaryButtonLabel] applies to PaymentSheet only; FlowController
 * has no primary button, so callers there leave it null.
 */
fun clickMeSheetConfiguration(
    customerId: String,
    ephemeralKey: String,
    appearance: PaymentSheet.Appearance,
    primaryButtonLabel: String? = null,
): PaymentSheet.Configuration {
    val builder =
        PaymentSheet.Configuration
            .Builder(CLICKME_MERCHANT_DISPLAY_NAME)
            .customer(PaymentSheet.CustomerConfiguration(customerId, ephemeralKey))
            .allowsDelayedPaymentMethods(false)
            // BUG-07: Link can close the sheet without any callback (a stuck spinner) and its logout can drop the
            // chosen card, so ClickMe never offers it.
            .link(PaymentSheet.LinkConfiguration(PaymentSheet.LinkConfiguration.Display.Never))
            .appearance(appearance)
    if (primaryButtonLabel != null) builder.primaryButtonLabel(primaryButtonLabel)
    return builder.build()
}

/**
 * The app is dark only, and PaymentSheet has no "always dark" switch on Android. Stripe's documented way to pin one
 * palette is to pass the same [PaymentSheet.Colors] as both light and dark, so that is what this does. Every value
 * comes from the theme; there are no color literals here.
 */
fun clickMePaymentSheetAppearance(
    colorScheme: ColorScheme,
    mutedText: Color,
): PaymentSheet.Appearance {
    val colors =
        PaymentSheet.Colors(
            primary = colorScheme.primary,
            surface = colorScheme.surfaceContainerHigh,
            component = colorScheme.surfaceContainer,
            componentBorder = colorScheme.outlineVariant,
            componentDivider = colorScheme.outlineVariant,
            onComponent = colorScheme.onSurface,
            subtitle = mutedText,
            placeholderText = mutedText,
            onSurface = colorScheme.onSurface,
            appBarIcon = colorScheme.onSurface,
            error = colorScheme.error,
        )
    val buttonColors =
        PaymentSheet.PrimaryButtonColors(
            background = colorScheme.primary,
            onBackground = colorScheme.onPrimary,
            border = colorScheme.primary,
        )
    return PaymentSheet.Appearance
        .Builder()
        .colorsLight(colors)
        .colorsDark(colors)
        .shapes(
            PaymentSheet.Shapes(cornerRadiusDp = SHEET_CORNER_RADIUS_DP, borderStrokeWidthDp = SHEET_BORDER_STROKE_DP),
        ).typography(
            PaymentSheet.Typography
                .Builder()
                .fontResId(R.font.nunito)
                .build(),
        ).primaryButton(
            PaymentSheet.PrimaryButton(
                colorsLight = buttonColors,
                colorsDark = buttonColors,
                shape = PaymentSheet.PrimaryButtonShape(cornerRadiusDp = PRIMARY_BUTTON_CORNER_RADIUS_DP),
            ),
        ).build()
}

/** The ClickMe PaymentSheet appearance for the current theme, rebuilt only when the theme colors change. */
@Composable
fun rememberClickMePaymentSheetAppearance(): PaymentSheet.Appearance {
    val colorScheme = MaterialTheme.colorScheme
    val muted = MaterialTheme.extras.onSurfaceMuted
    return remember(colorScheme, muted) { clickMePaymentSheetAppearance(colorScheme, muted) }
}
