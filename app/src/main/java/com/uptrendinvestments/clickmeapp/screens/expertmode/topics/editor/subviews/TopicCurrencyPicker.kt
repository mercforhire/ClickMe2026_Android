package com.uptrendinvestments.clickmeapp.screens.expertmode.topics.editor.subviews

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing

/** Test tag of the currency picker column. Each segment is tagged [TOPIC_CURRENCY_TAG] + `_` + the code. */
const val TOPIC_CURRENCY_TAG = "topic_editor_currency"

/**
 * Topic currency: a Material 3 single-choice segmented row (CAD | USD) above the price field. Android half of iOS
 * EXP-17 (Phase 6 D-01), superseding Android 10-CONTEXT D-10 ("no currency picker").
 *
 * - D-02: [enabled] false (an existing paid topic) disables every segment; [showsLockCaption] adds the explanation.
 * - [showsUsdNotice] adds the Stripe conversion-fee notice while USD is selected (USD-TOPIC-PRICING.md section 4).
 * - Nothing shows when fewer than two [currencies] are pickable (an older backend without `allowed_currencies`).
 *
 * The ViewModel decides what [onSelect] does; this composable only displays [selected].
 */
@Composable
fun TopicCurrencyPicker(
    currencies: List<String>,
    selected: String,
    enabled: Boolean,
    showsLockCaption: Boolean,
    showsUsdNotice: Boolean,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (currencies.size < 2) return
    Column(modifier = modifier.fillMaxWidth().testTag(TOPIC_CURRENCY_TAG)) {
        Text(
            text = stringResource(R.string.topic_editor_currency_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.sm))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            currencies.forEachIndexed { index, code ->
                SegmentedButton(
                    selected = selected == code,
                    onClick = { onSelect(code) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = currencies.size),
                    enabled = enabled,
                    modifier = Modifier.testTag("${TOPIC_CURRENCY_TAG}_$code"),
                ) {
                    Text(code)
                }
            }
        }
        if (showsLockCaption) {
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = stringResource(R.string.topic_editor_currency_locked),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (showsUsdNotice) {
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = stringResource(R.string.topic_editor_usd_notice),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(name = "TopicCurrencyPicker")
@Composable
private fun TopicCurrencyPickerPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(Spacing.md)) {
                TopicCurrencyPicker(
                    currencies = listOf("CAD", "USD"),
                    selected = "USD",
                    enabled = true,
                    showsLockCaption = false,
                    showsUsdNotice = true,
                    onSelect = {},
                )
                Spacer(Modifier.height(Spacing.md))
                TopicCurrencyPicker(
                    currencies = listOf("CAD", "USD"),
                    selected = "USD",
                    enabled = false,
                    showsLockCaption = true,
                    showsUsdNotice = true,
                    onSelect = {},
                )
            }
        }
    }
}
