package com.uptrendinvestments.clickmeapp.screens.expertmode.topics.editor.subviews

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.ValidatedField
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.extras

/** Test tag of the price text field. */
const val TOPIC_PRICE_TAG = "topic_editor_price"

/** Decimal keyboard, or the plain number keyboard for currencies without fraction digits. */
internal fun priceKeyboardType(decimalKeyboard: Boolean): KeyboardType =
    if (decimalKeyboard) KeyboardType.Decimal else KeyboardType.Number

/**
 * Price per session: a pill field with the ISO currency code as a trailing badge. [helperText] (the floor hint) shows
 * only when there is no [error] (the error line replaces it) and is null while the floor is unknown. The decimal
 * keyboard is used unless the currency has no fraction digits ([decimalKeyboard] false). Typed characters are
 * filtered by the ViewModel, so this composable only displays [text]. Pass a `FocusRequester` through [modifier] to
 * focus the field.
 */
@Composable
fun TopicPriceField(
    text: String,
    onTextChange: (String) -> Unit,
    currencyCode: String,
    helperText: String?,
    error: String?,
    decimalKeyboard: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.topic_editor_price_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.sm))
        ValidatedField(
            value = text,
            onValueChange = onTextChange,
            placeholder = stringResource(R.string.topic_editor_price_placeholder),
            leadingIcon = Icons.Outlined.Payments,
            error = error,
            modifier = modifier.testTag(TOPIC_PRICE_TAG),
            keyboardOptions =
                KeyboardOptions(
                    keyboardType = priceKeyboardType(decimalKeyboard),
                    imeAction = ImeAction.Done,
                ),
            enabled = enabled,
            trailingBadge = {
                Text(
                    text = currencyCode,
                    modifier = Modifier.padding(end = Spacing.xs),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
        )
        if (error == null && helperText != null) {
            // ValidatedField reserves Spacing.md below itself; pull the helper up to sit xs under the field.
            Text(
                text = helperText,
                modifier = Modifier.offset(y = -(Spacing.md - Spacing.xs)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.extras.onSurfaceMuted,
            )
        }
    }
}

@Preview(name = "TopicPriceField")
@Composable
private fun TopicPriceFieldPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(Spacing.md)) {
                TopicPriceField(
                    text = "12.50",
                    onTextChange = {},
                    currencyCode = "CAD",
                    helperText = "Leave blank or enter 0 for a free session — otherwise min $5.00",
                    error = null,
                    decimalKeyboard = true,
                    enabled = true,
                )
                TopicPriceField(
                    text = "2",
                    onTextChange = {},
                    currencyCode = "CAD",
                    helperText = "Leave blank or enter 0 for a free session — otherwise min $5.00",
                    error = "A paid session must be at least $5.00.",
                    decimalKeyboard = true,
                    enabled = true,
                )
            }
        }
    }
}
