package com.uptrendinvestments.clickmeapp.screens.clientmode.requestsent.subviews

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.buttonstyles.PressScaleButton
import com.uptrendinvestments.clickmeapp.components.buttonstyles.PressScaleButtonDefaults
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing

/** "View My Bookings" (filled) and "Back to Home" (tonal). [enabled] is false until the entry animation ends. */
@Composable
fun BookingRequestSentActions(
    onViewBookings: () -> Unit,
    onBackToHome: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        PressScaleButton(
            onClick = onViewBookings,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            enabled = enabled,
        ) { Text(stringResource(R.string.request_sent_view_bookings)) }
        PressScaleButton(
            onClick = onBackToHome,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            enabled = enabled,
            colors = PressScaleButtonDefaults.tonalColors(),
        ) { Text(stringResource(R.string.request_sent_back_home)) }
    }
}
