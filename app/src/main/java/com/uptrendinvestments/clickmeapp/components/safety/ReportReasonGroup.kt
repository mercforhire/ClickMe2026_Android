package com.uptrendinvestments.clickmeapp.components.safety

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.model.enums.UserReportReason
import com.uptrendinvestments.clickmeapp.screens.chat.capText
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.extras

/** Report details are capped at this many UTF-16 code units (the backend zod max). */
const val REPORT_DETAILS_MAX = 1000

/** The "{n}/1000" counter shows from this many characters. */
const val REPORT_DETAILS_COUNTER_FROM = 800

/** Test tag of the details field. */
const val REPORT_DETAILS_FIELD_TAG = "report_details_field"

/** Row test tag prefix; the suffix is the [UserReportReason] enum name. */
const val REPORT_REASON_ROW_TAG_PREFIX = "report_reason_"

private val RowMinHeight = 56.dp
private val FieldMinHeight = 112.dp
private val CardBorderWidth = 1.dp
private const val FIELD_MAX_LINES = 6

/** The on-screen label resource of the reason (USER-REPORTING spec labels; the iOS default labels are not used). */
@StringRes
fun UserReportReason.labelRes(): Int =
    when (this) {
        UserReportReason.Harassment -> R.string.report_reason_harassment
        UserReportReason.Spam -> R.string.report_reason_spam
        UserReportReason.InappropriateContent -> R.string.report_reason_inappropriate_content
        UserReportReason.ScamOrFraud -> R.string.report_reason_scam_or_fraud
        UserReportReason.Impersonation -> R.string.report_reason_impersonation
        UserReportReason.Other -> R.string.report_reason_other
    }

/**
 * The "Reason for reporting *" label and a card of six radio rows in enum order. Nothing is preselected. Shared by the
 * chat Report & Block screen and the Expert Profile report sheet.
 */
@Composable
fun ReportReasonGroup(
    selected: UserReportReason?,
    enabled: Boolean,
    onSelect: (UserReportReason) -> Unit,
    modifier: Modifier = Modifier,
) {
    val requiredDescription = stringResource(R.string.report_reason_required_a11y)
    val errorColor = MaterialTheme.colorScheme.error
    val labelText = stringResource(R.string.report_reason_label)
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(
            text =
                buildAnnotatedString {
                    append(labelText)
                    append(" ")
                    withStyle(SpanStyle(color = errorColor)) { append("*") }
                },
            modifier = Modifier.semantics { contentDescription = requiredDescription },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(CardBorderWidth, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column(modifier = Modifier.selectableGroup()) {
                UserReportReason.entries.forEachIndexed { index, entry ->
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
    entry: UserReportReason,
    isSelected: Boolean,
    enabled: Boolean,
    onSelect: (UserReportReason) -> Unit,
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
                .testTag(REPORT_REASON_ROW_TAG_PREFIX + entry.name),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Text(
            text = stringResource(entry.labelRes()),
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

/**
 * The optional details box: label, a multi-line field capped at [REPORT_DETAILS_MAX] UTF-16 code units on input, and a
 * "{n}/1000" counter from [REPORT_DETAILS_COUNTER_FROM] characters.
 */
@Composable
fun ReportDetailsField(
    value: String,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(
            text = stringResource(R.string.report_details_label),
            style = MaterialTheme.typography.titleMedium,
            color = colorScheme.onSurface,
        )
        OutlinedTextField(
            value = value,
            onValueChange = { onValueChange(capText(it, REPORT_DETAILS_MAX)) },
            modifier = Modifier.fillMaxWidth().heightIn(min = FieldMinHeight).testTag(REPORT_DETAILS_FIELD_TAG),
            enabled = enabled,
            textStyle = MaterialTheme.typography.bodyLarge,
            placeholder = {
                Text(
                    text = stringResource(R.string.report_details_placeholder),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.extras.onSurfaceMuted,
                )
            },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            maxLines = FIELD_MAX_LINES,
            shape = MaterialTheme.shapes.medium,
            colors =
                OutlinedTextFieldDefaults.colors(
                    focusedTextColor = colorScheme.onSurface,
                    unfocusedTextColor = colorScheme.onSurface,
                    disabledTextColor = colorScheme.onSurfaceVariant,
                    focusedContainerColor = colorScheme.surfaceContainer,
                    unfocusedContainerColor = colorScheme.surfaceContainer,
                    disabledContainerColor = colorScheme.surfaceContainer,
                    focusedBorderColor = colorScheme.primary,
                    unfocusedBorderColor = colorScheme.outlineVariant,
                    disabledBorderColor = colorScheme.outlineVariant,
                    cursorColor = colorScheme.primary,
                ),
        )
        if (value.length >= REPORT_DETAILS_COUNTER_FROM) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                Text(
                    text = stringResource(R.string.report_details_counter, value.length),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.extras.onSurfaceMuted,
                )
            }
        }
    }
}
