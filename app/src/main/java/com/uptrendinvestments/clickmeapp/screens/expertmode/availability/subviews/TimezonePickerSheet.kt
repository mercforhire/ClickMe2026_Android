package com.uptrendinvestments.clickmeapp.screens.expertmode.availability.subviews

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.PickerSheet
import com.uptrendinvestments.clickmeapp.components.PickerSheetContent
import com.uptrendinvestments.clickmeapp.constants.TimezoneEntry
import com.uptrendinvestments.clickmeapp.screens.expertmode.availability.AvailabilityTimezoneLogic
import com.uptrendinvestments.clickmeapp.screens.signup.timezone.timezoneSheetRows
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme

/**
 * Select Timezone bottom sheet (D-13, D-14). [entries] come from [AvailabilityTimezoneLogic.pickerEntries], so a
 * server zone that is not bundled shows first with its server display name. The caller closes the sheet in
 * [onSelect]; the choice stays in local state until the next Save.
 */
@Composable
fun TimezonePickerSheet(
    entries: List<TimezoneEntry>,
    selectedId: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    PickerSheet(
        title = stringResource(R.string.availability_timezone_sheet_title),
        onDismiss = onDismiss,
    ) {
        timezoneSheetRows(entries, selectedId, onSelect)
    }
}

/** Sheet body without the window, so it can be composed and tested directly. */
@Composable
fun TimezonePickerContent(
    entries: List<TimezoneEntry>,
    selectedId: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    PickerSheetContent(
        title = stringResource(R.string.availability_timezone_sheet_title),
        modifier = modifier,
    ) {
        timezoneSheetRows(entries, selectedId, onSelect)
    }
}

@Preview(name = "TimezonePickerContent")
@Composable
private fun TimezonePickerContentPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            TimezonePickerContent(
                entries =
                    AvailabilityTimezoneLogic.pickerEntries(
                        selectedId = "America/Toronto",
                        serverId = "America/Toronto",
                        serverDisplayName = "(GMT-05:00) America/Toronto",
                    ),
                selectedId = "America/Toronto",
                onSelect = {},
            )
        }
    }
}
