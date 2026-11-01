package com.uptrendinvestments.clickmeapp.components.buttonstyles

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
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
 * Lays out up to two entry-point actions. Nothing renders when both are null; a single action fills the width; two
 * share a row with equal weights and [gap] between them, or stack in a column (start first, full width each) when the
 * font scale is 1.5 or more or the available width is under 320 dp, the same rule the reschedule response card uses.
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
            BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
                val stacked = LocalDensity.current.fontScale >= LARGE_FONT_SCALE || maxWidth < NarrowWidth
                if (stacked) {
                    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                        start(Modifier.fillMaxWidth())
                        end(Modifier.fillMaxWidth())
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                        start(Modifier.weight(1f))
                        end(Modifier.weight(1f))
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
