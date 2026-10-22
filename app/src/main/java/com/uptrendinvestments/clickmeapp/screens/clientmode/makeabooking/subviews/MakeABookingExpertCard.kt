package com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.subviews

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.components.avatar.Avatar
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.extras

private val ExpertAvatarSize = 56.dp
private const val EXPERT_TEXT_MAX_LINES = 2

/**
 * The expert being booked: avatar, name and (when not blank) title, all taken from the route key so there is no
 * loading state. Not clickable. Server text is plain [Text] only (T-06-23).
 */
@Composable
fun MakeABookingExpertCard(
    name: String,
    title: String,
    imageUrl: String?,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Avatar(url = imageUrl, contentDescription = null, size = ExpertAvatarSize)
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = EXPERT_TEXT_MAX_LINES,
                    overflow = TextOverflow.Ellipsis,
                )
                if (title.isNotBlank()) {
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.extras.onSurfaceMuted,
                        maxLines = EXPERT_TEXT_MAX_LINES,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Preview(name = "MakeABookingExpertCard")
@Composable
private fun MakeABookingExpertCardPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            MakeABookingExpertCard(name = "Ada Lovelace", title = "Engineer", imageUrl = null)
        }
    }
}
