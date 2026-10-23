package com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.subviews

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.PaymentLifecycleCard
import com.uptrendinvestments.clickmeapp.components.PaymentLifecycleStage
import com.uptrendinvestments.clickmeapp.components.PickerSheet
import com.uptrendinvestments.clickmeapp.components.PickerSheetContent
import com.uptrendinvestments.clickmeapp.components.buttonstyles.PressScaleButton
import com.uptrendinvestments.clickmeapp.components.buttonstyles.PressScaleButtonDefaults
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing

private val GotItMinHeight = 56.dp

/**
 * "How does payment work?" sheet. Platform swap for iOS `PaymentInfoSheet` (CLAUDE.md: `.sheet` becomes
 * `ModalBottomSheet`, via [PickerSheet]). Dismissed by drag, scrim, Back or "Got it". It explains when the card is
 * charged with the lifecycle card in the upcoming state and never shows the payment-intent total (D-12).
 */
@Composable
fun PaymentInfoSheet(onDismiss: () -> Unit) {
    PickerSheet(title = stringResource(R.string.payment_info_link), onDismiss = onDismiss) {
        paymentInfoItems(onGotIt = onDismiss)
    }
}

/** Stateless sheet body (same items as [PaymentInfoSheet]) so it can be tested without a modal window. */
@Composable
fun PaymentInfoSheetBody(onGotIt: () -> Unit) {
    PickerSheetContent(title = stringResource(R.string.payment_info_link)) {
        paymentInfoItems(onGotIt = onGotIt)
    }
}

private fun LazyListScope.paymentInfoItems(onGotIt: () -> Unit) {
    item {
        Text(
            text = stringResource(R.string.payment_info_intro),
            modifier = Modifier.padding(horizontal = Spacing.md),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    item {
        PaymentLifecycleCard(
            stage = PaymentLifecycleStage.Upcoming,
            sessionWhen = null,
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.md),
        )
    }
    item {
        PressScaleButton(
            onClick = onGotIt,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = GotItMinHeight)
                    .padding(start = Spacing.md, end = Spacing.md, bottom = Spacing.xl),
            colors = PressScaleButtonDefaults.filledColors(),
        ) {
            Text(text = stringResource(R.string.payment_info_got_it))
        }
    }
}

@Preview(name = "PaymentInfoSheetBody")
@Composable
private fun PaymentInfoSheetBodyPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            PaymentInfoSheetBody(onGotIt = {})
        }
    }
}
