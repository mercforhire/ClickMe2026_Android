package com.uptrendinvestments.clickmeapp.screens.chat.conversations.subviews

import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.extras

/** Test tag on the Conversations search field. */
const val CHATS_SEARCH_TAG = "chats_search"

private val SearchFieldMinHeight = 56.dp
private val RestingBorder = 1.dp
private val FocusedBorder = 2.dp
private val ClearButtonSize = 48.dp

/**
 * The Conversations search field (D-18): `surfaceContainer` fill, `extraLarge` shape, a 1 dp `outlineVariant` border
 * that becomes 2 dp `primary` when focused, a leading search icon and a trailing clear button once there is text. The
 * search action hides the keyboard. Filtering is local, in the ViewModel.
 */
@Composable
fun ConversationsSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val shape = MaterialTheme.shapes.extraLarge
    val borderColor = if (focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val clearLabel = stringResource(R.string.chat_conversations_clear_search)
    val container = MaterialTheme.colorScheme.surfaceContainer

    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = SearchFieldMinHeight)
                .border(if (focused) FocusedBorder else RestingBorder, borderColor, shape)
                .testTag(CHATS_SEARCH_TAG),
        placeholder = {
            Text(
                text = stringResource(R.string.chat_conversations_search),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.extras.onSurfaceMuted,
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }, modifier = Modifier.size(ClearButtonSize)) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = clearLabel,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        singleLine = true,
        shape = shape,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions =
            KeyboardActions(
                onSearch = {
                    keyboard?.hide()
                    focusManager.clearFocus()
                },
            ),
        interactionSource = interactionSource,
        colors =
            TextFieldDefaults.colors(
                focusedContainerColor = container,
                unfocusedContainerColor = container,
                disabledContainerColor = container,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
            ),
    )
}

@Preview(name = "ConversationsSearchField")
@Composable
private fun ConversationsSearchFieldPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            ConversationsSearchField(query = "Ada", onQueryChange = {})
        }
    }
}
