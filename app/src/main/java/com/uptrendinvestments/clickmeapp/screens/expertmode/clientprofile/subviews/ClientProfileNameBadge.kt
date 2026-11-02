package com.uptrendinvestments.clickmeapp.screens.expertmode.clientprofile.subviews

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing

/**
 * The client's name and the non-interactive "Client" pill (iOS `ClientProfileNameBadge`). The name is the screen's
 * second focal point after the avatar: `displaySmall`, centered, wrapping, and a heading for TalkBack (UI-SPEC C4).
 * Name and pill are one merged node, so the pill is read with the name. [name] is untrusted server text shown as plain
 * [Text].
 */
@Composable
fun ClientProfileNameBadge(
    name: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().semantics(mergeDescendants = true) { },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Text(
            text = name,
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = CircleShape,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Text(
                text = stringResource(R.string.client_profile_badge),
                modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(name = "ClientProfileNameBadge")
@Composable
private fun ClientProfileNameBadgePreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            ClientProfileNameBadge("Sophia Carter", Modifier.padding(Spacing.md))
        }
    }
}
