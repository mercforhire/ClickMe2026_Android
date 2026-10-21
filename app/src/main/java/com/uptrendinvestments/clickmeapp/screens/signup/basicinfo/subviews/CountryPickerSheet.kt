package com.uptrendinvestments.clickmeapp.screens.signup.basicinfo.subviews

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.PickerRow
import com.uptrendinvestments.clickmeapp.components.PickerSheet
import com.uptrendinvestments.clickmeapp.components.PickerSheetContent
import com.uptrendinvestments.clickmeapp.constants.Country

/** Searchable single-select sheet over the static 50-country list (D-16). */
@Composable
fun CountryPickerSheet(
    countries: List<Country>,
    selectedCode: String,
    query: String,
    onQueryChange: (String) -> Unit,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    PickerSheet(
        title = stringResource(R.string.signup_basic_info_country_sheet_title),
        onDismiss = onDismiss,
        searchQuery = query,
        searchPlaceholder = stringResource(R.string.signup_basic_info_country_search),
        onSearchQueryChange = onQueryChange,
        content = countryRows(countries, selectedCode, onSelect),
    )
}

/** Stateless body of [CountryPickerSheet], testable without a window. */
@Composable
fun CountryPickerSheetContent(
    countries: List<Country>,
    selectedCode: String,
    query: String,
    onQueryChange: (String) -> Unit,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    PickerSheetContent(
        title = stringResource(R.string.signup_basic_info_country_sheet_title),
        modifier = modifier,
        searchQuery = query,
        searchPlaceholder = stringResource(R.string.signup_basic_info_country_search),
        onSearchQueryChange = onQueryChange,
        content = countryRows(countries, selectedCode, onSelect),
    )
}

private fun countryRows(
    countries: List<Country>,
    selectedCode: String,
    onSelect: (String) -> Unit,
): LazyListScope.() -> Unit =
    {
        items(countries, key = { it.code }) { country ->
            PickerRow(
                label = country.name,
                selected = country.code == selectedCode,
                onClick = { onSelect(country.code) },
            )
        }
    }
