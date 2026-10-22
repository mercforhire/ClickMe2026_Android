package com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.subviews

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.model.enums.MeetingType
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing

private val RowMinHeight = 56.dp

/**
 * Meeting type picker: one radio row per [MeetingType] in declaration order (In-app Voice first).
 *
 * Both types are always offered (D-08 as written): the card does not read the expert's available meeting types. A
 * `422 MEETING_LINK_UNAVAILABLE` answer is handled in the ViewModel, which snaps [selected] back to In-app Voice.
 * While [enabled] is false (a booking is in flight) the rows do not respond.
 */
@Composable
fun MakeABookingMeetingTypeCard(
    selected: MeetingType,
    onSelect: (MeetingType) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val types = MeetingType.entries
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.selectableGroup()) {
            types.forEachIndexed { index, type ->
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = RowMinHeight)
                            .selectable(
                                selected = type == selected,
                                enabled = enabled,
                                role = Role.RadioButton,
                                onClick = { onSelect(type) },
                            ).padding(horizontal = Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    RadioButton(selected = type == selected, onClick = null)
                    Text(
                        text = meetingTypeLabel(type),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                if (index < types.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = Spacing.md),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
            }
        }
    }
}

/** The display label for [type]; shared by the meeting type card and the summary. */
@Composable
fun meetingTypeLabel(type: MeetingType): String =
    stringResource(
        when (type) {
            MeetingType.InAppVoice -> R.string.make_a_booking_meeting_in_app
            MeetingType.SkypeZoom -> R.string.make_a_booking_meeting_skype_zoom
        },
    )

@Preview(name = "MakeABookingMeetingTypeCard")
@Composable
private fun MakeABookingMeetingTypeCardPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            MakeABookingMeetingTypeCard(
                selected = MeetingType.InAppVoice,
                onSelect = {},
                modifier = Modifier.padding(Spacing.md),
            )
        }
    }
}
