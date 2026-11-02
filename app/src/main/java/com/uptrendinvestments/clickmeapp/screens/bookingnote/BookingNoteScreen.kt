package com.uptrendinvestments.clickmeapp.screens.bookingnote

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.formatShortDate
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.rememberDeviceTimeFormat
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.formatSessionTimeRange
import com.uptrendinvestments.clickmeapp.screens.login.AuthTopAppBar
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.contentMaxWidth
import com.uptrendinvestments.clickmeapp.ui.theme.extras
import java.text.DateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

/** Test tag on the note text field. */
const val BOOKING_NOTE_FIELD_TAG = "booking_note_field"

/** Test tag on the idle Save button in the app bar. */
const val BOOKING_NOTE_SAVE_TAG = "booking_note_save"

/**
 * The note editor bound to its [viewModel]. [header] seeds the name, topic and date line, because the note endpoints
 * return none of them (D-05).
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
        onLeave = onBack,
        onTextChange = viewModel::onTextChange,
        onSave = viewModel::save,
        modifier = modifier,
    )
}

/** Stateless note editor: app bar with Save, then the header and the text field. */
@Composable
fun BookingNoteContent(
    state: BookingNoteUiState,
    header: BookingNoteHeader,
    onLeave: () -> Unit,
    onTextChange: (String) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
    zone: ZoneId = ZoneId.systemDefault(),
    timeFormat: DateFormat = rememberDeviceTimeFormat(),
    locale: Locale = LocalConfiguration.current.locales[0],
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            AuthTopAppBar(
                title = stringResource(R.string.booking_note_title),
                onBack = onLeave,
                actions = {
                    TextButton(
                        onClick = onSave,
                        enabled = state.canSave,
                        modifier = Modifier.testTag(BOOKING_NOTE_SAVE_TAG),
                    ) {
                        Text(
                            text = stringResource(R.string.booking_note_save),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding).consumeWindowInsets(innerPadding)) {
            if (state.load == BookingNoteLoad.Loaded) {
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

/** The loaded body: header, then the field. It scrolls and follows the keyboard. */
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
        NoteField(value = state.noteText, onValueChange = onTextChange)
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
                .heightIn(min = 260.dp)
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
