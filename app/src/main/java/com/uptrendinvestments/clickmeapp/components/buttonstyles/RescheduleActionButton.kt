package com.uptrendinvestments.clickmeapp.components.buttonstyles

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing

private val ButtonMinHeight = 56.dp
private val GlyphSize = 24.dp
private val OutlineWidth = 1.dp
private val NarrowWidth = 320.dp
private const val LARGE_FONT_SCALE = 1.5f

/**
 * Shared sizing for any button placed in an [ActionButtonPair]. iOS gives these buttons no horizontal padding
 * (`UpcomingBookingsSessionCard.swift`, `UpcomingBookingCard.swift` secondaryButton), so the default 24 dp side padding
 * pushes "Reschedule" onto two lines in a half-width slot. 8 dp keeps the glyph inside the pill's rounded end; the
 * vertical padding stays 0, matching PressScaleButton's default (G-16.2-3).
 */
object ActionButtonPairDefaults {
    val ContentPadding: PaddingValues = PaddingValues(horizontal = Spacing.sm)
}

/**
 * The "Reschedule" control for every entry point (Phase 16.2, UI-SPEC "Reschedule action button"): a neutral outlined
 * 56 dp pill with the update glyph and the label "Reschedule". It is never accent-coloured, so it reads as a
 * secondary action beside Cancel or Message. [contentDescription], when given, replaces the spoken name (for example
 * "Reschedule session with Ada Lovelace" on a card that lists several bookings).
 */
@Composable
fun RescheduleActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String? = null,
) {
    PressScaleButton(
        onClick = onClick,
        enabled = enabled,
        contentPadding = ActionButtonPairDefaults.ContentPadding,
        modifier =
            modifier
                .heightIn(min = ButtonMinHeight)
                .border(OutlineWidth, MaterialTheme.colorScheme.outline, CircleShape)
                .then(
                    if (contentDescription != null) {
                        Modifier.semantics { this.contentDescription = contentDescription }
                    } else {
                        Modifier
                    },
                ),
        colors =
            ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
    ) {
        Icon(Icons.Outlined.Update, contentDescription = null, modifier = Modifier.size(GlyphSize))
        Spacer(Modifier.width(Spacing.sm))
        Text(stringResource(R.string.reschedule_start_action))
    }
}

/**
 * Lays out up to two entry-point actions. Nothing renders when both are null and a single action fills the width. Two
 * actions share a row of equal halves with [gap] between them, or stack in a column (start first, full width each)
 * when the font scale is 1.5 or more, the width is under 320 dp, or either button's single-line content is wider than
 * its half of the row (G-16.2-3), so no label ever breaks across lines.
 *
 * Contract: the slot content must support intrinsic measurement, because the fit check reads each slot's
 * `maxIntrinsicWidth`. That rules out subcomposition-based layouts (`SubcomposeLayout` and the composables built on
 * it) and lazy lists inside a slot. Every current slot is a Material button row.
 *
 * Used by Upcoming Booking Details (Reschedule + Cancel, `Spacing.md`), My Bookings and the expert cards
 * (Reschedule + Message, `Spacing.sm`) and Expert Booking Summary (UI-SPEC Entry points; D-05).
 */
@Composable
fun ActionButtonPair(
    start: (@Composable (Modifier) -> Unit)?,
    end: (@Composable (Modifier) -> Unit)?,
    modifier: Modifier = Modifier,
    gap: Dp = Spacing.md,
) {
    when {
        start == null && end == null -> Unit
        start == null || end == null -> {
            val only = start ?: end
            only?.invoke(modifier.fillMaxWidth())
        }
        else ->
            Layout(
                contents = listOf({ start(Modifier) }, { end(Modifier) }),
                modifier = modifier.fillMaxWidth(),
            ) { (startMeasurables, endMeasurables), constraints ->
                val startItem = startMeasurables.first()
                val endItem = endMeasurables.first()
                val gapPx = gap.roundToPx()
                val bounded = constraints.hasBoundedWidth
                val width = constraints.maxWidth
                val half = (width - gapPx) / 2
                val fits =
                    bounded &&
                        startItem.maxIntrinsicWidth(Constraints.Infinity) <= half &&
                        endItem.maxIntrinsicWidth(Constraints.Infinity) <= half
                val stacked = fontScale >= LARGE_FONT_SCALE || !bounded || width < NarrowWidth.roundToPx() || !fits
                val loose = constraints.copy(minWidth = 0, minHeight = 0)
                if (stacked) {
                    val slot = if (bounded) loose.copy(minWidth = width) else loose
                    val startPlaceable = startItem.measure(slot)
                    val endPlaceable = endItem.measure(slot)
                    val layoutWidth = if (bounded) width else maxOf(startPlaceable.width, endPlaceable.width)
                    val height = constraints.constrainHeight(startPlaceable.height + gapPx + endPlaceable.height)
                    layout(layoutWidth, height) {
                        startPlaceable.placeRelative(0, 0)
                        endPlaceable.placeRelative(0, startPlaceable.height + gapPx)
                    }
                } else {
                    val endWidth = width - half - gapPx
                    val startPlaceable = startItem.measure(loose.copy(minWidth = half, maxWidth = half))
                    val endPlaceable = endItem.measure(loose.copy(minWidth = endWidth, maxWidth = endWidth))
                    val height = constraints.constrainHeight(maxOf(startPlaceable.height, endPlaceable.height))
                    layout(width, height) {
                        startPlaceable.placeRelative(0, 0)
                        endPlaceable.placeRelative(half + gapPx, 0)
                    }
                }
            }
    }
}

@Preview(name = "RescheduleActionButton")
@Composable
private fun RescheduleActionButtonPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            ActionButtonPair(
                start = { RescheduleActionButton(onClick = {}, modifier = it) },
                end = { RescheduleActionButton(onClick = {}, modifier = it, enabled = false) },
            )
        }
    }
}
