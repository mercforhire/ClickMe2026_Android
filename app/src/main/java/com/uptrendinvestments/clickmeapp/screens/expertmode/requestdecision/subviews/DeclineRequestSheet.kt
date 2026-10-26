package com.uptrendinvestments.clickmeapp.screens.expertmode.requestdecision.subviews

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.buttonstyles.DestructiveOutlinedButton
import com.uptrendinvestments.clickmeapp.components.buttonstyles.PressScaleButton
import com.uptrendinvestments.clickmeapp.components.buttonstyles.PressScaleButtonDefaults
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.formatShortDate
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.formatSessionTimeRange
import com.uptrendinvestments.clickmeapp.screens.expertmode.clientDisplayName
import com.uptrendinvestments.clickmeapp.screens.expertmode.requestdecision.DECLINE_NOTE_MAX_CHARS
import com.uptrendinvestments.clickmeapp.screens.expertmode.requestdecision.DeclineReason
import com.uptrendinvestments.clickmeapp.screens.expertmode.requestdecision.RequestDecisionDetails
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.extras
import java.time.ZoneId
import java.util.Locale

/** Test tags of the Decline sheet. */
const val DECLINE_NOTE_FIELD_TAG = "decline_note_field"
const val DECLINE_SHEET_CONFIRM_TAG = "decline_sheet_confirm"
const val DECLINE_SHEET_KEEP_TAG = "decline_sheet_keep"

/** Row test tag prefix; the suffix is the [DeclineReason] enum name. */
const val DECLINE_REASON_ROW_TAG_PREFIX = "decline_reason_"

private val ButtonMinHeight = 56.dp
private val RowMinHeight = 56.dp
private val SpinnerSize = 24.dp
private val SpinnerStroke = 2.dp
private val CardBorderWidth = 1.dp

/** The on-screen label of the reason, matching [DeclineReason.wireLabel] (tested). */
@Composable
fun DeclineReason.label(): String =
    stringResource(
        when (this) {
            DeclineReason.ScheduleConflict -> R.string.decline_reason_conflict
            DeclineReason.OutsideExpertise -> R.string.decline_reason_out_of_scope
            DeclineReason.NotAvailable -> R.string.decline_reason_unavailable
            DeclineReason.Other -> R.string.decline_reason_other
        },
    )

/**
 * "Decline Booking Request?" sheet (`ModalBottomSheet` swap for the iOS sheet). While [isSubmitting] the sheet cannot be
 * dismissed by swipe, scrim or Back (T-11-36); [onDismiss] closes it ("Keep Request" and every allowed dismissal). The
 * selected reason and the note live in the caller so they survive rotation and a failed decline.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeclineRequestSheet(
    details: RequestDecisionDetails,
    selected: DeclineReason?,
    onSelect: (DeclineReason) -> Unit,
    note: String,
    onNoteChange: (String) -> Unit,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    zone: ZoneId,
    timeFormat: java.text.DateFormat,
    locale: Locale,
) {
    val submitting by rememberUpdatedState(isSubmitting)
    ModalBottomSheet(
        onDismissRequest = { if (!submitting) onDismiss() },
        sheetState =
            rememberModalBottomSheetState(
                skipPartiallyExpanded = true,
                confirmValueChange = { !submitting || it != SheetValue.Hidden },
            ),
        shape = MaterialTheme.shapes.extraLarge.copy(bottomStart = ZeroCornerSize, bottomEnd = ZeroCornerSize),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        DeclineRequestSheetContent(
            details = details,
            selected = selected,
            onSelect = onSelect,
            note = note,
            onNoteChange = onNoteChange,
            isSubmitting = isSubmitting,
            onKeep = onDismiss,
            onConfirm = onConfirm,
            zone = zone,
            timeFormat = timeFormat,
            locale = locale,
        )
    }
}

/** Stateless sheet body, separate from the modal window so tests can render it directly. */
@Composable
fun DeclineRequestSheetContent(
    details: RequestDecisionDetails,
    selected: DeclineReason?,
    onSelect: (DeclineReason) -> Unit,
    note: String,
    onNoteChange: (String) -> Unit,
    isSubmitting: Boolean,
    onKeep: () -> Unit,
    onConfirm: () -> Unit,
    zone: ZoneId,
    timeFormat: java.text.DateFormat,
    locale: Locale,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(start = Spacing.md, end = Spacing.md, bottom = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(
                text = stringResource(R.string.decline_sheet_title),
                modifier = Modifier.fillMaxWidth().semantics { heading() },
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.decline_sheet_subtitle),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        DeclineSummary(details = details, zone = zone, timeFormat = timeFormat, locale = locale)
        ReasonSection(selected = selected, onSelect = onSelect, enabled = !isSubmitting)
        DecisionNoteField(
            label = stringResource(R.string.decline_sheet_note_label),
            placeholder = stringResource(R.string.decline_sheet_note_placeholder),
            value = note,
            maxChars = DECLINE_NOTE_MAX_CHARS,
            counterRes = R.string.decline_sheet_counter,
            onValueChange = onNoteChange,
            testTag = DECLINE_NOTE_FIELD_TAG,
        )
        DeclineButtons(
            canDecline = selected != null,
            isSubmitting = isSubmitting,
            onKeep = onKeep,
            onConfirm = onConfirm,
        )
    }
}

@Composable
private fun DeclineSummary(
    details: RequestDecisionDetails,
    zone: ZoneId,
    timeFormat: java.text.DateFormat,
    locale: Locale,
) {
    val whenText =
        stringResource(
            R.string.request_decision_date_time_value,
            formatShortDate(details.start.atZone(zone).toLocalDate(), locale),
            formatSessionTimeRange(details.start, details.end, zone, timeFormat),
        )
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(modifier = Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            SummaryRow(stringResource(R.string.decline_sheet_from), clientDisplayName(details.clientName))
            SummaryRow(stringResource(R.string.request_decision_date_time), whenText)
            details.topic?.let { SummaryRow(stringResource(R.string.request_decision_topic), it) }
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.extras.onSurfaceMuted,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun ReasonSection(
    selected: DeclineReason?,
    onSelect: (DeclineReason) -> Unit,
    enabled: Boolean,
) {
    val requiredDescription = stringResource(R.string.decline_sheet_reason_required_a11y)
    val errorColor = MaterialTheme.colorScheme.error
    val labelText = stringResource(R.string.decline_sheet_reason_label)
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(
            text =
                buildAnnotatedString {
                    append(labelText)
                    append(" ")
                    withStyle(SpanStyle(color = errorColor)) { append("*") }
                },
            modifier = Modifier.semantics { contentDescription = requiredDescription },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.extras.onSurfaceMuted,
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(CardBorderWidth, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column(modifier = Modifier.selectableGroup()) {
                DeclineReason.entries.forEachIndexed { index, entry ->
                    if (index > 0) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = Spacing.md),
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                    }
                    ReasonRow(entry = entry, isSelected = entry == selected, enabled = enabled, onSelect = onSelect)
                }
            }
        }
    }
}

@Composable
private fun ReasonRow(
    entry: DeclineReason,
    isSelected: Boolean,
    enabled: Boolean,
    onSelect: (DeclineReason) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = RowMinHeight)
                .selectable(
                    selected = isSelected,
                    enabled = enabled,
                    role = Role.RadioButton,
                    onClick = { onSelect(entry) },
                ).padding(horizontal = Spacing.md)
                .testTag(DECLINE_REASON_ROW_TAG_PREFIX + entry.name),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Text(
            text = entry.label(),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        RadioButton(
            selected = isSelected,
            onClick = null,
            colors =
                RadioButtonDefaults.colors(
                    selectedColor = MaterialTheme.colorScheme.primary,
                    unselectedColor = MaterialTheme.colorScheme.outline,
                ),
        )
    }
}

@Composable
private fun DeclineButtons(
    canDecline: Boolean,
    isSubmitting: Boolean,
    onKeep: () -> Unit,
    onConfirm: () -> Unit,
) {
    val choose = stringResource(R.string.decline_sheet_choose_reason)
    val declining = stringResource(R.string.decline_sheet_declining)
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        PressScaleButton(
            onClick = onKeep,
            modifier = Modifier.fillMaxWidth().heightIn(min = ButtonMinHeight).testTag(DECLINE_SHEET_KEEP_TAG),
            enabled = !isSubmitting,
            colors = PressScaleButtonDefaults.filledColors(),
        ) {
            Text(text = stringResource(R.string.decline_sheet_keep), style = MaterialTheme.typography.labelLarge)
        }
        DestructiveOutlinedButton(
            onClick = onConfirm,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = ButtonMinHeight)
                    .testTag(DECLINE_SHEET_CONFIRM_TAG)
                    .semantics {
                        if (isSubmitting) {
                            stateDescription = declining
                        } else if (!canDecline) {
                            stateDescription = choose
                        }
                    },
            enabled = canDecline && !isSubmitting,
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(SpinnerSize),
                    color = MaterialTheme.colorScheme.error,
                    strokeWidth = SpinnerStroke,
                )
            } else {
                Text(text = stringResource(R.string.decline_sheet_confirm), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
