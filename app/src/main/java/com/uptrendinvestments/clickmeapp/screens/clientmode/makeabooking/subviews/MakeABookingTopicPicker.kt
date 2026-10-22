package com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.subviews

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.PriceText
import com.uptrendinvestments.clickmeapp.components.SectionError
import com.uptrendinvestments.clickmeapp.components.SkeletonBlock
import com.uptrendinvestments.clickmeapp.components.SkeletonGroup
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.BookingTopicUi
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.TopicsState
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.screens.login.asString
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.extras
import com.uptrendinvestments.clickmeapp.util.PriceDisplay

/** Test tag on the topic picker row in every state. */
const val TOPIC_PICKER_TAG = "make_a_booking_topic_picker"

private val PickerMinHeight = 56.dp
private val ChevronSize = 24.dp
private const val TITLE_MAX_LINES = 2
private const val SKELETON_PRIMARY_BAR_FRACTION = 0.6f
private const val SKELETON_SECONDARY_BAR_FRACTION = 0.25f

/**
 * The "Topic of Discussion" row: a button showing the selected topic (or the placeholder) that opens the topic sheet.
 * States: skeleton while [topics] loads, the button when loaded, a non-clickable "no topics" card when the list is
 * empty, and a [SectionError] with Retry on failure. Server text is plain [Text] only (T-06-23).
 */
@Composable
fun MakeABookingTopicPicker(
    topics: TopicsState,
    selectedTopic: BookingTopicUi?,
    onOpen: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (topics) {
        TopicsState.Loading -> PickerSkeleton(modifier)
        is TopicsState.Loaded ->
            if (topics.topics.isEmpty()) {
                StaticCard(modifier) {
                    Text(
                        text = stringResource(R.string.make_a_booking_no_topics),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.extras.onSurfaceMuted,
                    )
                }
            } else {
                LoadedPicker(selectedTopic = selectedTopic, onOpen = onOpen, modifier = modifier)
            }
        is TopicsState.Failed ->
            StaticCard(modifier) {
                SectionError(message = topics.message.asString(), onRetry = onRetry)
            }
    }
}

@Composable
private fun PickerSkeleton(modifier: Modifier) {
    SkeletonGroup(modifier = modifier.fillMaxWidth().testTag(TOPIC_PICKER_TAG)) {
        SkeletonBlock(
            modifier = Modifier.fillMaxWidth().height(PickerMinHeight),
            shape = MaterialTheme.shapes.medium,
        )
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            SkeletonBlock(Modifier.fillMaxWidth(SKELETON_PRIMARY_BAR_FRACTION).height(Spacing.md))
            SkeletonBlock(Modifier.fillMaxWidth(SKELETON_SECONDARY_BAR_FRACTION).height(Spacing.sm))
        }
    }
}

@Composable
private fun StaticCard(
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth().testTag(TOPIC_PICKER_TAG),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().heightIn(min = PickerMinHeight).padding(Spacing.md),
            contentAlignment = Alignment.CenterStart,
        ) {
            content()
        }
    }
}

@Composable
private fun LoadedPicker(
    selectedTopic: BookingTopicUi?,
    onOpen: () -> Unit,
    modifier: Modifier,
) {
    val description = selectedTopic?.let { topicDescription(it) }
    Surface(
        onClick = onOpen,
        modifier =
            modifier
                .fillMaxWidth()
                .testTag(TOPIC_PICKER_TAG)
                .semantics {
                    role = Role.Button
                    if (description != null) contentDescription = description
                },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = PickerMinHeight).padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = selectedTopic?.title ?: stringResource(R.string.make_a_booking_topic_placeholder),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = TITLE_MAX_LINES,
                    overflow = TextOverflow.Ellipsis,
                )
                if (selectedTopic != null) {
                    Spacer(Modifier.height(Spacing.xs))
                    TopicSubLine(selectedTopic)
                }
            }
            Spacer(Modifier.width(Spacing.sm))
            Icon(
                imageVector = Icons.Outlined.ExpandMore,
                contentDescription = null,
                modifier = Modifier.size(ChevronSize),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TopicSubLine(topic: BookingTopicUi) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(R.string.make_a_booking_duration, topic.durationMins),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.extras.onSurfaceMuted,
        )
        Text(
            text = SEPARATOR,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.extras.onSurfaceMuted,
        )
        PriceText(topic.price, style = MaterialTheme.typography.bodySmall)
    }
}

private const val SEPARATOR = " · "

@Composable
private fun topicDescription(topic: BookingTopicUi): String {
    val minutes = pluralStringResource(R.plurals.make_a_booking_minutes_a11y, topic.durationMins, topic.durationMins)
    val price =
        when (val display = topic.price) {
            PriceDisplay.Free -> stringResource(R.string.price_free)
            is PriceDisplay.Paid -> display.text
            PriceDisplay.Unknown -> stringResource(R.string.price_unknown)
        }
    return stringResource(R.string.make_a_booking_topic_a11y, topic.title, minutes, price)
}

@Preview(name = "MakeABookingTopicPicker")
@Composable
private fun MakeABookingTopicPickerPreview() {
    val topic = BookingTopicUi("t1", "Intro to ML", 60, null, PriceDisplay.Free, true)
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                MakeABookingTopicPicker(TopicsState.Loaded(listOf(topic)), topic, onOpen = {}, onRetry = {})
                MakeABookingTopicPicker(TopicsState.Loaded(emptyList()), null, onOpen = {}, onRetry = {})
                MakeABookingTopicPicker(TopicsState.Failed(FieldError.Text("Offline")), null, onOpen = {}, onRetry = {})
            }
        }
    }
}
