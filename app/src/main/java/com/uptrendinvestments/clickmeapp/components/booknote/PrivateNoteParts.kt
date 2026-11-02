package com.uptrendinvestments.clickmeapp.components.booknote

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing

/*
 * The private-note surfaces shared by the four booking cards (client Upcoming and Past, expert Upcoming and Past)
 * and the two summaries (client My Note card, expert equivalent). They live in one file so the surfaces stay
 * consistent, like the iOS note zone, preview footer and My Note card they port.
 *
 * Note text is user-written: it is shown as plain text (no autolink, HTML or markdown) and is never logged.
 * A blank preview or note counts as absent, stricter than the iOS `isEmpty`, because a note saved as whitespace from
 * iOS yields a preview of spaces.
 */

/** Test tag on the upcoming card's note zone, in both its preview and "add" forms. */
const val PRIVATE_NOTE_ZONE_TAG = "private_note_zone"

/** Test tag on the past card's "Private Note" action. */
const val PRIVATE_NOTE_ACTION_TAG = "private_note_action"

/** Test tag on the past card's note preview footer. */
const val PRIVATE_NOTE_FOOTER_TAG = "private_note_footer"

/** Test tag on the summary's My Note card. */
const val MY_NOTE_CARD_TAG = "my_note_card"

private val MinTarget = 48.dp
private val SmallIconSize = 16.dp
private val DashOn = 4.dp
private val DashOff = 3.dp
private val ZoneCornerRadius = 10.dp
private const val STRIP_FILL_ALPHA = 0.08f
private const val STRIP_BORDER_ALPHA = 0.25f
private const val CARD_FILL_ALPHA = 0.06f
private const val CARD_BORDER_ALPHA = 0.30f
private const val PREVIEW_MAX_LINES = 2

/**
 * Bottom-of-card affordance on an upcoming booking card (iOS `noteZone`): the note preview with a pencil when a
 * non-blank [preview] exists, otherwise a dashed "Add private note" row, so writing a note is one tap away.
 */
@Composable
fun PrivateNoteZone(
    preview: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (preview.isNullOrBlank()) {
        AddNoteRow(onClick = onClick, modifier = modifier)
    } else {
        NotePreviewStrip(
            preview = preview,
            showPencil = true,
            onClick = onClick,
            testTag = PRIVATE_NOTE_ZONE_TAG,
            modifier = modifier,
        )
    }
}

/** Past booking card's "Private Note" action with a pencil (iOS `actionsRow`). */
@Composable
fun PrivateNoteAction(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tint = MaterialTheme.colorScheme.primary
    Row(
        modifier =
            modifier
                .defaultMinSize(minWidth = MinTarget, minHeight = MinTarget)
                .clickable(
                    onClickLabel = stringResource(R.string.booking_note_open_a11y),
                    role = Role.Button,
                    onClick = onClick,
                ).padding(horizontal = Spacing.xs)
                .testTag(PRIVATE_NOTE_ACTION_TAG),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Text(
            text = stringResource(R.string.booking_note_private_note),
            style = MaterialTheme.typography.labelLarge,
            color = tint,
        )
        DecorativeIcon(Icons.Outlined.Edit, tint)
    }
}

/** Past booking card's preview strip under the actions row; a tap opens the editor (iOS `notePreviewFooter`). */
@Composable
fun NotePreviewFooter(
    preview: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NotePreviewStrip(
        preview = preview,
        showPencil = false,
        onClick = onClick,
        testTag = PRIVATE_NOTE_FOOTER_TAG,
        modifier = modifier,
    )
}

/**
 * Booking summary's My Note card (iOS `myNoteCard`): the whole note, or a prompt when [note] is null or blank. A tap
 * opens the editor.
 */
@Composable
fun MyNoteCard(
    note: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tint = MaterialTheme.colorScheme.primary
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = tint.copy(alpha = CARD_FILL_ALPHA),
        border = BorderStroke(1.dp, tint.copy(alpha = CARD_BORDER_ALPHA)),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(
                        onClickLabel = stringResource(R.string.booking_note_open_a11y),
                        role = Role.Button,
                        onClick = onClick,
                    ).padding(Spacing.md)
                    .testTag(MY_NOTE_CARD_TAG),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                DecorativeIcon(Icons.AutoMirrored.Outlined.Notes, tint)
                Text(
                    text = stringResource(R.string.booking_note_card_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                DecorativeIcon(Icons.Outlined.Edit, tint)
            }
            if (note.isNullOrBlank()) {
                Text(
                    text = stringResource(R.string.booking_note_card_prompt),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = note.trim(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

/**
 * The preview as shown in the two-line strip. Display only: the saved note is untouched. Edge whitespace is trimmed and
 * line breaks become single spaces, so a note that starts with blank lines still shows its first words instead of an
 * empty strip (the server preview is the first 140 characters as typed).
 */
internal fun notePreviewText(raw: String): String = raw.trim().replace(LineBreakRun, " ")

private val LineBreakRun = Regex("\\s*\\n\\s*")

@Composable
private fun NotePreviewStrip(
    preview: String,
    showPencil: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    val tint = MaterialTheme.colorScheme.primary
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = tint.copy(alpha = STRIP_FILL_ALPHA),
        border = BorderStroke(1.dp, tint.copy(alpha = STRIP_BORDER_ALPHA)),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = MinTarget)
                    .clickable(
                        onClickLabel = stringResource(R.string.booking_note_open_a11y),
                        role = Role.Button,
                        onClick = onClick,
                    ).padding(Spacing.sm)
                    .testTag(testTag),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            DecorativeIcon(Icons.AutoMirrored.Outlined.Notes, tint)
            Text(
                text = notePreviewText(preview),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = PREVIEW_MAX_LINES,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (showPencil) DecorativeIcon(Icons.Outlined.Edit, tint)
        }
    }
}

@Composable
private fun AddNoteRow(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dashColor = MaterialTheme.colorScheme.outlineVariant
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = MinTarget)
                .drawBehind {
                    drawRoundRect(
                        color = dashColor,
                        cornerRadius = CornerRadius(ZoneCornerRadius.toPx()),
                        style =
                            Stroke(
                                width = 1.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(DashOn.toPx(), DashOff.toPx())),
                            ),
                    )
                }.clickable(
                    onClickLabel = stringResource(R.string.booking_note_open_a11y),
                    role = Role.Button,
                    onClick = onClick,
                ).padding(horizontal = Spacing.sm, vertical = Spacing.sm)
                .testTag(PRIVATE_NOTE_ZONE_TAG),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DecorativeIcon(Icons.Outlined.Add, MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(Spacing.xs))
        Text(
            text = stringResource(R.string.booking_note_add_private_note),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DecorativeIcon(
    icon: ImageVector,
    tint: Color,
) {
    Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(SmallIconSize), tint = tint)
}

@Preview(name = "PrivateNoteParts")
@Composable
private fun PrivateNotePartsPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                PrivateNoteZone(preview = null, onClick = {})
                PrivateNoteZone(preview = "Ask about the pricing model before the call.", onClick = {})
                PrivateNoteAction(onClick = {})
                NotePreviewFooter(preview = "Ask about the pricing model before the call.", onClick = {})
                MyNoteCard(note = "Ask about the pricing model before the call.", onClick = {})
                MyNoteCard(note = null, onClick = {})
            }
        }
    }
}
