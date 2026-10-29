package com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.subviews

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.ExpertTagChip
import com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.ProfileLanguage
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing

private val EditMinHeight = 48.dp

/**
 * Spoken Languages: the current languages as read-only chips that wrap onto as many rows as needed (or a hint when
 * there are none) and an "Edit languages" button that opens the shared language sheet through [onEdit].
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileLanguagesField(
    languages: List<ProfileLanguage>,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().padding(bottom = Spacing.md)) {
        Text(
            text = stringResource(R.string.profile_settings_languages_title),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (languages.isEmpty()) {
            Text(
                text = stringResource(R.string.profile_settings_languages_empty),
                modifier = Modifier.padding(top = Spacing.sm),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            FlowRow(
                modifier = Modifier.padding(top = Spacing.sm),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                languages.forEach { language -> ExpertTagChip(label = language.label) }
            }
        }
        TextButton(onClick = onEdit, modifier = Modifier.heightIn(min = EditMinHeight)) {
            Text(
                text = stringResource(R.string.profile_settings_languages_edit),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
