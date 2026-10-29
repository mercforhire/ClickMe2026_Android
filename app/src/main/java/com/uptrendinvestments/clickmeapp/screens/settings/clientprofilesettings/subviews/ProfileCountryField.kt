package com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.subviews

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.PickerField
import com.uptrendinvestments.clickmeapp.constants.Countries
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing

/**
 * "Country" label above a picker field showing the country's name for the alpha-3 [countryCode], or "Select" when
 * none is set. Tapping opens the shared country sheet through [onClick].
 */
@Composable
fun ProfileCountryField(
    countryCode: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.profile_settings_country_label),
            modifier = Modifier.padding(bottom = Spacing.xs),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        PickerField(
            value = countryCode.takeIf { it.isNotBlank() }?.let(Countries::displayName),
            placeholder = stringResource(R.string.profile_settings_country_select),
            leadingIcon = Icons.Outlined.Language,
            onClick = onClick,
        )
    }
}
