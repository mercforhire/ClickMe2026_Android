package com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.subviews

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.PickerSheet
import com.uptrendinvestments.clickmeapp.components.PriceText
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.BookingTopicUi
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Motion
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.extras
import com.uptrendinvestments.clickmeapp.util.PriceDisplay

private val DurationIconSize = 16.dp
private val UnselectedBorderWidth = 1.dp
private val SelectedBorderWidth = 2.dp

/**
 * "Choose a Topic" bottom sheet (iOS `.sheet` becomes a `ModalBottomSheet` via [PickerSheet]). Lists every topic,
 * paid ones included (D-01). The caller closes the sheet after [onSelect]; selecting the already-selected topic only
 * closes it because the ViewModel ignores an equal id (D-02). Back, drag and scrim dismiss through [onDismiss].
 */
@Composable
fun MakeABookingTopicSheet(
    topics: List<BookingTopicUi>,
    selectedTopicId: String?,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    PickerSheet(
        title = stringResource(R.string.make_a_booking_topic_sheet_title),
        onDismiss = onDismiss,
    ) {
        makeABookingTopicItems(topics, selectedTopicId, onSelect)
    }
}

/** The sheet's list: one [MakeABookingTopicCard] per topic with `md` gaps, then an `xl` bottom spacer. */
fun LazyListScope.makeABookingTopicItems(
    topics: List<BookingTopicUi>,
    selectedTopicId: String?,
    onSelect: (String) -> Unit,
) {
    items(topics, key = { it.id }) { topic ->
        MakeABookingTopicCard(
            topic = topic,
            selected = topic.id == selectedTopicId,
            onClick = { onSelect(topic.id) },
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
        )
    }
    item(key = "bottom_spacer") { Spacer(Modifier.height(Spacing.xl)) }
}

/**
 * One topic as a radio card: title and price, duration, optional description. The whole card is a single merged
 * `Role.RadioButton` target; the selected state is shown by fill, border width and the radio mark, never color alone.
 * Server text is plain [Text] only (T-06-23).
 */
@Composable
fun MakeABookingTopicCard(
    topic: BookingTopicUi,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val animation = tween<Color>(Motion.CONTROL_STATE_DURATION_MS)
    val container by animateColorAsState(
        targetValue =
            if (selected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            },
        animationSpec = animation,
        label = "topicCardFill",
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        animationSpec = animation,
        label = "topicCardBorder",
    )
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = container,
        border = BorderStroke(if (selected) SelectedBorderWidth else UnselectedBorderWidth, borderColor),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
                    .padding(Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            RadioButton(selected = selected, onClick = null)
            TopicCardBody(topic)
        }
    }
}

@Composable
private fun TopicCardBody(topic: BookingTopicUi) {
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Top) {
            Text(
                text = topic.title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.width(Spacing.sm))
            PriceText(topic.price, style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(Spacing.sm))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.Schedule,
                contentDescription = null,
                modifier = Modifier.size(DurationIconSize),
                tint = MaterialTheme.extras.onSurfaceMuted,
            )
            Spacer(Modifier.width(Spacing.xs))
            Text(
                text = stringResource(R.string.make_a_booking_duration, topic.durationMins),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.extras.onSurfaceMuted,
            )
        }
        topic.description?.takeIf { it.isNotBlank() }?.let { description ->
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.extras.onSurfaceMuted,
            )
        }
    }
}

@Preview(name = "MakeABookingTopicCard")
@Composable
private fun MakeABookingTopicCardPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Column {
                MakeABookingTopicCard(
                    topic = BookingTopicUi("t1", "Intro to ML", 30, null, PriceDisplay.Free, true),
                    selected = true,
                    onClick = {},
                )
                MakeABookingTopicCard(
                    topic = BookingTopicUi("t2", "Deep dive", 60, "Long session", PriceDisplay.Paid("$50.00"), false),
                    selected = false,
                    onClick = {},
                )
            }
        }
    }
}
