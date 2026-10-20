package com.uptrendinvestments.clickmeapp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.uptrendinvestments.clickmeapp.R

/**
 * Nunito variable font (SIL OFL), Android stand-in for iOS SF Rounded. One bundled file,
 * two weights (400 and 600) selected through the variable font's wght axis (API 26+).
 */
val NunitoFamily =
    FontFamily(
        Font(
            R.font.nunito,
            FontWeight.Normal,
            variationSettings = FontVariation.Settings(FontVariation.weight(400)),
        ),
        Font(
            R.font.nunito,
            FontWeight.SemiBold,
            variationSettings = FontVariation.Settings(FontVariation.weight(600)),
        ),
    )

private fun nunito(
    size: Int,
    lineHeight: Int,
    weight: FontWeight,
) = TextStyle(
    fontFamily = NunitoFamily,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = 0.sp,
)

private val Body = nunito(16, 24, FontWeight.Normal)
private val Caption = nunito(12, 16, FontWeight.Normal)
private val Label = nunito(12, 16, FontWeight.SemiBold)
private val Strong = nunito(16, 24, FontWeight.SemiBold)
private val Heading = nunito(20, 24, FontWeight.SemiBold)
private val Display = nunito(28, 34, FontWeight.SemiBold)

/** Four sizes (12, 16, 20, 28 sp), two weights (400, 600). Never set fontSize elsewhere. */
val ClickMeTypography =
    Typography(
        displayLarge = Display,
        displayMedium = Display,
        displaySmall = Display,
        headlineLarge = Display,
        headlineMedium = Heading,
        headlineSmall = Heading,
        titleLarge = Heading,
        titleMedium = Strong,
        titleSmall = Strong,
        bodyLarge = Body,
        bodyMedium = Body,
        bodySmall = Caption,
        labelLarge = Strong,
        labelMedium = Label,
        labelSmall = Caption,
    )
