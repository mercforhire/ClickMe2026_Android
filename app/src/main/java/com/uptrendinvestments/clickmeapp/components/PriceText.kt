package com.uptrendinvestments.clickmeapp.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.util.PriceDisplay

/** Renders a [PriceDisplay]: "Free" in the primary accent, prices and the unknown placeholder in onSurface. */
@Composable
fun PriceText(
    display: PriceDisplay,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.titleMedium,
) {
    val (text, color) =
        when (display) {
            PriceDisplay.Free -> stringResource(R.string.price_free) to MaterialTheme.colorScheme.primary
            is PriceDisplay.Paid -> display.text to MaterialTheme.colorScheme.onSurface
            PriceDisplay.Unknown -> stringResource(R.string.price_unknown) to MaterialTheme.colorScheme.onSurface
        }
    Text(text = text, modifier = modifier, style = style, color = color)
}

@Preview(name = "PriceText")
@Composable
private fun PriceTextPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Column {
                PriceText(PriceDisplay.Free)
                PriceText(PriceDisplay.Paid("$50.00"))
                PriceText(PriceDisplay.Unknown)
            }
        }
    }
}
