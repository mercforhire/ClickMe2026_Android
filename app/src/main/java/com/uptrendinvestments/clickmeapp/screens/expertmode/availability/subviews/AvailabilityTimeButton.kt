package com.uptrendinvestments.clickmeapp.screens.expertmode.availability.subviews

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing

private val TimeButtonMinHeight = 48.dp

/**
 * Start or end time of one slot on a day card (UI-SPEC "Day card"). [text] is the visible time in the device 12/24
 * hour format; [contentDescription] is the full phrase ("Start time 09:00"). Tapping opens the time range sheet.
 */
@Composable
fun AvailabilityTimeButton(
    text: String,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = TimeButtonMinHeight)
                    .clickable(
                        onClickLabel = stringResource(R.string.availability_change_time_range),
                        role = Role.Button,
                        onClick = onClick,
                    ).semantics { this.contentDescription = contentDescription }
                    .padding(horizontal = Spacing.sm),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum"),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Preview(name = "AvailabilityTimeButton")
@Composable
private fun AvailabilityTimeButtonPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            AvailabilityTimeButton(text = "9:00 AM", contentDescription = "Start time 9:00 AM", onClick = {})
        }
    }
}
