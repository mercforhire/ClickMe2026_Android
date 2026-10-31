package com.uptrendinvestments.clickmeapp.screens.clientmode.search.subviews

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme

/** Test tag on the Search tab text field. */
const val SEARCH_FIELD_TAG = "search_expert_field"

/** Test tag on the Search tab clear button (present only while the field has text). */
const val SEARCH_CLEAR_TAG = "search_expert_clear"

private val FieldCornerRadius = 14.dp
private const val RESTING_BORDER_ALPHA = 0.55f

/**
 * Port of iOS `SearchExpertSearchBar`: a rounded field with a leading magnifier, the "Search for experts" placeholder
 * and a clear button that appears only while there is text. Autocorrect and auto-capitalization are off (iOS
 * `.disableAutocorrection(true)` / `.autocapitalization(.none)`) and the Search action just hides the keyboard, since
 * the ViewModel already debounces the query. Stateless: the ViewModel owns the text (RESEARCH Pitfall 6).
 */
@Composable
fun SearchExpertSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val colors = MaterialTheme.colorScheme
    val placeholderText = stringResource(R.string.search_field_placeholder)

    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier =
            modifier
                .fillMaxWidth()
                .testTag(SEARCH_FIELD_TAG)
                .semantics {
                    // The placeholder disappears once the user types, so keep it as the field's name (never the value).
                    if (query.isNotEmpty()) contentDescription = placeholderText
                },
        placeholder = {
            Text(
                text = placeholderText,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurfaceVariant,
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = colors.onSurfaceVariant,
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(
                    onClick = { onQueryChange("") },
                    modifier = Modifier.testTag(SEARCH_CLEAR_TAG),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Cancel,
                        contentDescription = stringResource(R.string.search_field_clear),
                        tint = colors.onSurfaceVariant,
                    )
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(FieldCornerRadius),
        keyboardOptions =
            KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                imeAction = ImeAction.Search,
            ),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        colors =
            OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.primary,
                unfocusedBorderColor = colors.primary.copy(alpha = RESTING_BORDER_ALPHA),
                cursorColor = colors.primary,
                focusedContainerColor = colors.surfaceContainerLowest,
                unfocusedContainerColor = colors.surfaceContainerLowest,
            ),
    )
}

@Preview(name = "SearchExpertSearchBar")
@Composable
private fun SearchExpertSearchBarPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            SearchExpertSearchBar(query = "Ada", onQueryChange = {})
        }
    }
}
