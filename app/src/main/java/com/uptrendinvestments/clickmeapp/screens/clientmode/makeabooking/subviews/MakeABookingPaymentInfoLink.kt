package com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.subviews

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing

const val PAYMENT_INFO_LINK_TAG = "make_a_booking_payment_info_link"

private val LinkMinHeight = 48.dp
private val LinkIconSize = 16.dp

/**
 * "How does payment work?" text button. Shown only for a paid topic, directly above the Book Now CTA (wired in
 * 07-13). Neutral colour: the accent is reserved for primary actions (UI-SPEC Color).
 */
@Composable
fun MakeABookingPaymentInfoLink(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = LinkMinHeight)
                .clickable(role = Role.Button, onClick = onClick)
                .semantics(mergeDescendants = true) { }
                .testTag(PAYMENT_INFO_LINK_TAG),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = null,
            modifier = Modifier.size(LinkIconSize),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.payment_info_link),
            modifier = Modifier.padding(start = Spacing.sm),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview(name = "MakeABookingPaymentInfoLink")
@Composable
private fun MakeABookingPaymentInfoLinkPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            MakeABookingPaymentInfoLink(onClick = {})
        }
    }
}
