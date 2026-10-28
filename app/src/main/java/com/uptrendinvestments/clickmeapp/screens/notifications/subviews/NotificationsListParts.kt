package com.uptrendinvestments.clickmeapp.screens.notifications.subviews

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.screens.notifications.NotificationDay

/** The "Today" / "Yesterday" / "Earlier" header of a date section. */
@Composable
fun NotificationSectionHeader(
    day: NotificationDay,
    modifier: Modifier = Modifier,
) {
    val label =
        stringResource(
            when (day) {
                NotificationDay.Today -> R.string.notifications_section_today
                NotificationDay.Yesterday -> R.string.notifications_section_yesterday
                NotificationDay.Earlier -> R.string.notifications_section_earlier
            },
        )
    Text(
        text = label,
        modifier = modifier.semantics { heading() },
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
