package com.uptrendinvestments.clickmeapp.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.extras

/** Test tag on the trailing check icon of a selected [PickerRow]. */
internal const val PICKER_ROW_CHECK_TAG = "picker_row_check"

/** Test tag on the optional leading icon of a [PickerRow]. */
internal const val PICKER_ROW_LEADING_ICON_TAG = "picker_row_leading_icon"

private val RowMinHeight = 48.dp
private val RowIconSize = 24.dp
private val LoadingIndicatorSize = 48.dp

/**
 * Bottom sheet for picking a country, language or timezone. Opens fully expanded (no half-open state) and the body
 * is IME-padded so the search field and list ride above the keyboard (RESEARCH Pitfall 13). All content lives in the
 * stateless [PickerSheetContent].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickerSheet(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    searchQuery: String? = null,
    searchPlaceholder: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    footer: (@Composable () -> Unit)? = null,
    content: LazyListScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape =
            MaterialTheme.shapes.extraLarge.copy(
                bottomStart = ZeroCornerSize,
                bottomEnd = ZeroCornerSize,
            ),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        PickerSheetContent(
            title = title,
            searchQuery = searchQuery,
            searchPlaceholder = searchPlaceholder,
            onSearchQueryChange = onSearchQueryChange,
            footer = footer,
            content = content,
        )
    }
}

/**
 * Stateless picker body: heading, optional search field (when [searchQuery] is non-null) with a clear button, the
 * list built by [content], and an optional [footer]. Separate from [PickerSheet] so it is testable without a window.
 */
@Composable
fun PickerSheetContent(
    title: String,
    modifier: Modifier = Modifier,
    searchQuery: String? = null,
    searchPlaceholder: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    footer: (@Composable () -> Unit)? = null,
    content: LazyListScope.() -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth().imePadding()) {
        Text(
            text = title,
            modifier =
                Modifier
                    .padding(horizontal = Spacing.lg, vertical = Spacing.md)
                    .semantics { heading() },
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (searchQuery != null) {
            ValidatedField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = searchPlaceholder,
                leadingIcon = Icons.Outlined.Search,
                error = null,
                modifier = Modifier.padding(horizontal = Spacing.lg),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                trailingBadge =
                    if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = stringResource(R.string.picker_clear_search),
                                    modifier = Modifier.size(RowIconSize),
                                    tint = MaterialTheme.extras.onSurfaceMuted,
                                )
                            }
                        }
                    } else {
                        null
                    },
            )
        }
        LazyColumn(modifier = Modifier.weight(1f, fill = false), content = content)
        if (footer != null) {
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = Spacing.lg)) {
                footer()
            }
        }
    }
}

/**
 * Selectable list row. Selected rows show an accent check and expose selected semantics to TalkBack; a 1 dp
 * outlineVariant divider separates rows. Timezone rows pass `minHeight = 56.dp`.
 *
 * [leadingIcon] adds a 24 dp primary-tinted decorative icon before the label. [role] defaults to
 * [Role.RadioButton] (single-select, `selectable`); pass [Role.Checkbox] for multi-select lists such as the All
 * Categories sheet, which switches the row to `toggleable` so TalkBack announces checked/unchecked.
 */
@Composable
fun PickerRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    minHeight: Dp = RowMinHeight,
    leadingIcon: ImageVector? = null,
    role: Role = Role.RadioButton,
) {
    val selectionModifier =
        if (role == Role.Checkbox) {
            Modifier.toggleable(value = selected, role = Role.Checkbox, onValueChange = { onClick() })
        } else {
            Modifier.selectable(selected = selected, role = role, onClick = onClick)
        }
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = minHeight)
                    .then(selectionModifier)
                    .padding(horizontal = Spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(RowIconSize).testTag(PICKER_ROW_LEADING_ICON_TAG),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (selected) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    modifier = Modifier.size(RowIconSize).testTag(PICKER_ROW_CHECK_TAG),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
    }
}

/** Centered loading state for a picker list: 48 dp primary spinner over a muted caption. */
@Composable
fun PickerLoading(
    text: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(LoadingIndicatorSize),
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.size(Spacing.md))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.extras.onSurfaceMuted,
        )
    }
}

@Preview(name = "PickerSheetContent")
@Composable
private fun PickerSheetContentPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            PickerSheetContent(
                title = "Select Country",
                searchQuery = "",
                searchPlaceholder = "Search countries...",
                content = {
                    item { PickerRow(label = "Canada", selected = true, onClick = {}) }
                    item { PickerRow(label = "France", selected = false, onClick = {}) }
                },
            )
        }
    }
}
