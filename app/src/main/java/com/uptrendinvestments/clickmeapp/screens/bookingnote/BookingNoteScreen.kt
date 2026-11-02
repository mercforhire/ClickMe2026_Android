package com.uptrendinvestments.clickmeapp.screens.bookingnote

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.ErrorState
import com.uptrendinvestments.clickmeapp.components.SkeletonBlock
import com.uptrendinvestments.clickmeapp.components.SkeletonGroup
import com.uptrendinvestments.clickmeapp.components.buttons.SaveState
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.formatShortDate
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.rememberDeviceTimeFormat
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.formatSessionTimeRange
import com.uptrendinvestments.clickmeapp.screens.login.AuthErrorDialog
import com.uptrendinvestments.clickmeapp.screens.login.AuthTopAppBar
import com.uptrendinvestments.clickmeapp.screens.login.asString
import com.uptrendinvestments.clickmeapp.ui.theme.Motion
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.contentMaxWidth
import com.uptrendinvestments.clickmeapp.ui.theme.extras
import java.text.DateFormat
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

/** Test tag on the note text field. */
const val BOOKING_NOTE_FIELD_TAG = "booking_note_field"

/** Test tag on the idle Save button in the app bar. */
const val BOOKING_NOTE_SAVE_TAG = "booking_note_save"

/** Test tag on the spinner that replaces Save while a save is in flight. */
const val BOOKING_NOTE_SAVING_TAG = "booking_note_saving"

/** Test tag on the check mark shown for a moment after a successful save. */
const val BOOKING_NOTE_SAVED_TAG = "booking_note_saved"

/** Test tag on the character counter under the field. */
const val BOOKING_NOTE_COUNTER_TAG = "booking_note_counter"

/** Test tag on the loading skeleton. */
const val BOOKING_NOTE_SKELETON_TAG = "booking_note_skeleton"

private val ActionBoxSize = 48.dp
private val ActionIconSize = 24.dp
private val SpinnerStroke = 2.dp
private val FieldMinHeight = 260.dp

/**
 * The note editor bound to its [viewModel]. [header] seeds the name, topic and date line, because the note endpoints
 * return none of them (D-05). Both backs ask the ViewModel first: [onBack] runs only when nothing is unsaved or after
 * the user confirms Discard (D-01).
 */
@Composable
fun BookingNoteScreen(
    viewModel: BookingNoteViewModel,
    header: BookingNoteHeader,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    BookingNoteContent(
        state = state,
        header = header,
        onLeave = { if (viewModel.requestLeave()) onBack() },
        onRetry = viewModel::retry,
        onTextChange = viewModel::onTextChange,
        onSave = viewModel::save,
        onDismissSaveError = viewModel::dismissSaveError,
        onKeepEditing = viewModel::dismissDiscardPrompt,
        onDiscard = {
            viewModel.dismissDiscardPrompt()
            onBack()
        },
        modifier = modifier,
    )
}

/** Stateless note editor: app bar with the Save action, the body for the load state, and the two dialogs. */
@Suppress("LongParameterList")
@Composable
fun BookingNoteContent(
    state: BookingNoteUiState,
    header: BookingNoteHeader,
    onLeave: () -> Unit,
    onRetry: () -> Unit,
    onTextChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismissSaveError: () -> Unit,
    onKeepEditing: () -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier,
    zone: ZoneId = ZoneId.systemDefault(),
    timeFormat: DateFormat = rememberDeviceTimeFormat(),
    locale: Locale = LocalConfiguration.current.locales[0],
) {
    BackHandler(enabled = state.hasChanges) { onLeave() }
    // The later-registered enabled handler wins, so an in-flight save swallows Back (Phase 8 convention).
    BackHandler(enabled = state.isSaving) {}
    // The check shows only while the text is unchanged since the save, so an edit brings Save straight back.
    val saveMode =
        when {
            state.isSaving -> SaveState.Saving
            state.didSave && !state.hasChanges -> SaveState.Saved
            else -> SaveState.Idle
        }
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            AuthTopAppBar(
                title = stringResource(R.string.booking_note_title),
                onBack = onLeave,
                backEnabled = !state.isSaving,
                actions = { BookingNoteSaveAction(mode = saveMode, enabled = state.canSave, onSave = onSave) },
            )
        },
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding).consumeWindowInsets(innerPadding)) {
            Crossfade(
                targetState = state.load,
                modifier = Modifier.fillMaxSize(),
                animationSpec = tween(durationMillis = Motion.CONTROL_STATE_DURATION_MS),
                label = "bookingNoteBody",
            ) { load ->
                when (load) {
                    BookingNoteLoad.Loading -> NoteSkeleton()
                    is BookingNoteLoad.Failed ->
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            ErrorState(
                                onRetry = onRetry,
                                heading = stringResource(R.string.booking_note_load_error),
                                body = load.message.asString(),
                                retryLabel = stringResource(R.string.section_retry),
                            )
                        }
                    BookingNoteLoad.Loaded ->
                        NoteForm(
                            state = state,
                            header = header,
                            onTextChange = onTextChange,
                            zone = zone,
                            timeFormat = timeFormat,
                            locale = locale,
                        )
                }
            }
        }
    }
    state.saveError?.let { error ->
        AuthErrorDialog(
            message = error.asString(),
            onDismiss = onDismissSaveError,
            title = stringResource(R.string.booking_note_save_error_title),
        )
    }
    if (state.showDiscardPrompt) {
        DiscardChangesDialog(onKeepEditing = onKeepEditing, onDiscard = onDiscard)
    }
}

/**
 * The app bar Save action (UI-SPEC C2, mirroring the availability save): the Save text button, a spinner while saving
 * and a check once saved cross-fade inside 48 dp boxes so the bar never shifts.
 */
@Composable
private fun BookingNoteSaveAction(
    mode: SaveState,
    enabled: Boolean,
    onSave: () -> Unit,
) {
    Crossfade(
        targetState = mode,
        animationSpec = tween(durationMillis = Motion.CONTROL_STATE_DURATION_MS),
        label = "bookingNoteSaveAction",
    ) { current ->
        when (current) {
            SaveState.Idle ->
                TextButton(
                    onClick = onSave,
                    enabled = enabled,
                    modifier = Modifier.testTag(BOOKING_NOTE_SAVE_TAG),
                ) {
                    Text(
                        text = stringResource(R.string.booking_note_save),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }

            SaveState.Saving -> {
                val savingLabel = stringResource(R.string.booking_note_saving_a11y)
                Box(
                    modifier =
                        Modifier
                            .size(ActionBoxSize)
                            .semantics { contentDescription = savingLabel }
                            .testTag(BOOKING_NOTE_SAVING_TAG),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(ActionIconSize),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = SpinnerStroke,
                    )
                }
            }

            SaveState.Saved -> {
                val savedLabel = stringResource(R.string.booking_note_saved_a11y)
                Box(
                    modifier =
                        Modifier
                            .size(ActionBoxSize)
                            .semantics {
                                contentDescription = savedLabel
                                liveRegion = LiveRegionMode.Polite
                            }.testTag(BOOKING_NOTE_SAVED_TAG),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(ActionIconSize),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

/** First-load placeholder: the header lines and a field-sized block, announced as one "Loading". */
@Composable
private fun NoteSkeleton() {
    SkeletonGroup(modifier = Modifier.fillMaxSize().testTag(BOOKING_NOTE_SKELETON_TAG)) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .contentMaxWidth()
                    .padding(horizontal = Spacing.md)
                    .padding(top = Spacing.md),
        ) {
            SkeletonBlock(Modifier.height(24.dp).width(160.dp))
            Spacer(Modifier.height(Spacing.xs))
            SkeletonBlock(Modifier.height(16.dp).width(120.dp))
            Spacer(Modifier.height(Spacing.lg))
            SkeletonBlock(Modifier.fillMaxWidth().height(FieldMinHeight), shape = MaterialTheme.shapes.medium)
        }
    }
}

/** The loaded body: header, then the field and its counter. It scrolls and follows the keyboard. */
@Composable
private fun NoteForm(
    state: BookingNoteUiState,
    header: BookingNoteHeader,
    onTextChange: (String) -> Unit,
    zone: ZoneId,
    timeFormat: DateFormat,
    locale: Locale,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .contentMaxWidth()
                .padding(horizontal = Spacing.md)
                .padding(top = Spacing.md, bottom = Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        NoteHeader(header = header, zone = zone, timeFormat = timeFormat, locale = locale)
        Column(Modifier.fillMaxWidth()) {
            NoteField(value = state.noteText, onValueChange = onTextChange)
            if (state.showCounter) {
                NoteCounter(length = state.noteText.length, atCap = state.atCap, locale = locale)
            }
        }
    }
}

/** Name, topic and the session's date line, read as one node. A null topic reads like the cards do (UI-SPEC C3). */
@Composable
private fun NoteHeader(
    header: BookingNoteHeader,
    zone: ZoneId,
    timeFormat: DateFormat,
    locale: Locale,
) {
    val topic = header.topic?.takeIf { it.isNotBlank() } ?: stringResource(R.string.explore_session_topic_fallback)
    val start = Instant.ofEpochMilli(header.startMillis)
    val end = Instant.ofEpochMilli(header.endMillis)
    val date = formatShortDate(start.atZone(zone).toLocalDate(), locale)
    val timeRange = formatSessionTimeRange(start, end, zone, timeFormat)
    val whenText = stringResource(R.string.my_bookings_date_time, date, timeRange)
    Column(
        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Text(
            text = header.name,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = topic,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = whenText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.extras.onSurfaceMuted,
        )
    }
}

/**
 * The multi-line note field, styled like the review field: a plain [TextField] on `surfaceContainer` with a 1 dp
 * resting and 2 dp focused border drawn around it (UI-SPEC C1). It grows with its content inside the scrolling
 * column. The cap is enforced by the ViewModel.
 */
@Composable
private fun NoteField(
    value: String,
    onValueChange: (String) -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = MaterialTheme.shapes.medium
    val borderColor =
        if (focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val fieldLabel = stringResource(R.string.booking_note_field_label)
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = FieldMinHeight)
                .onFocusChanged { focused = it.isFocused }
                .border(width = if (focused) 2.dp else 1.dp, color = borderColor, shape = shape)
                .semantics { contentDescription = fieldLabel }
                .testTag(BOOKING_NOTE_FIELD_TAG),
        textStyle = MaterialTheme.typography.bodyLarge,
        placeholder = {
            Text(
                text = stringResource(R.string.booking_note_hint),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.extras.onSurfaceMuted,
            )
        },
        shape = shape,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        colors =
            TextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = MaterialTheme.colorScheme.primary,
            ),
    )
}

/** "n / 5,000" under the field, `tertiary` at the cap (RESEARCH A7). */
@Composable
private fun NoteCounter(
    length: Int,
    atCap: Boolean,
    locale: Locale,
) {
    val format = NumberFormat.getIntegerInstance(locale)
    Text(
        text =
            stringResource(
                R.string.booking_note_counter,
                format.format(length),
                format.format(NOTE_MAX_LENGTH),
            ),
        modifier = Modifier.fillMaxWidth().padding(top = Spacing.xs).testTag(BOOKING_NOTE_COUNTER_TAG),
        style = MaterialTheme.typography.bodySmall,
        color = if (atCap) MaterialTheme.colorScheme.tertiary else MaterialTheme.extras.onSurfaceMuted,
        textAlign = TextAlign.End,
    )
}

/** Asks before leaving with unsaved changes (D-01). Holds no text field. Back and the scrim keep editing. */
@Composable
private fun DiscardChangesDialog(
    onKeepEditing: () -> Unit,
    onDiscard: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onKeepEditing,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.extraLarge,
        title = {
            Text(
                text = stringResource(R.string.booking_note_discard_title),
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Text(
                text = stringResource(R.string.booking_note_discard_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        dismissButton = {
            TextButton(onClick = onKeepEditing) {
                Text(
                    text = stringResource(R.string.booking_note_discard_keep),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDiscard) {
                Text(
                    text = stringResource(R.string.booking_note_discard_confirm),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
    )
}
