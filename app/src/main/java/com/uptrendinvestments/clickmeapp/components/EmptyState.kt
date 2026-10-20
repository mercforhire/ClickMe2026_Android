package com.uptrendinvestments.clickmeapp.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.buttonstyles.PressScaleButton
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.extras

/** Optional call-to-action rendered under an [EmptyState]. */
data class EmptyStateAction(
    val label: String,
    val onClick: () -> Unit,
)

/** Centered icon, heading, body and optional action. Heading and body are required: no generic default copy. */
@Composable
fun EmptyState(
    icon: ImageVector,
    heading: String,
    body: String,
    modifier: Modifier = Modifier,
    action: EmptyStateAction? = null,
) {
    Column(
        modifier = modifier.padding(horizontal = Spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.extras.onSurfaceMuted,
        )
        Spacer(Modifier.height(Spacing.xl))
        Text(
            text = heading,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = body,
            modifier = Modifier.widthIn(max = 320.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.extras.onSurfaceMuted,
            textAlign = TextAlign.Center,
        )
        if (action != null) {
            Spacer(Modifier.height(Spacing.lg))
            PressScaleButton(onClick = action.onClick) { Text(action.label) }
        }
    }
}

/** Shared "Something went wrong" pattern with a retry action (used from Phase 15 onwards). */
@Composable
fun ErrorState(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    EmptyState(
        icon = Icons.Outlined.CloudOff,
        heading = stringResource(R.string.error_state_heading),
        body = stringResource(R.string.error_state_body),
        modifier = modifier,
        action = EmptyStateAction(stringResource(R.string.error_state_retry), onRetry),
    )
}

@Preview(name = "EmptyState")
@Composable
private fun EmptyStatePreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            EmptyState(
                icon = Icons.Outlined.Inbox,
                heading = "No bookings yet",
                body = "Book a session with an expert and it will show up here.",
                action = EmptyStateAction("Explore experts") {},
            )
        }
    }
}

@Preview(name = "ErrorState")
@Composable
private fun ErrorStatePreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            ErrorState(onRetry = {})
        }
    }
}
