package com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.subviews

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.ValidatedField
import com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.AutoSaveStatus
import com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.BIO_COUNTER_FROM
import com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.BIO_MAX
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Motion
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.extras

/** Test tag on the app-bar save indicator slot (present in every state, empty when idle). */
const val AUTO_SAVE_STATUS_TAG = "settings_autosave_status"

/** Test tag on the bio text field. */
const val PROFILE_BIO_FIELD_TAG = "profile_settings_bio"

/** Test tag on the bio character counter. */
const val PROFILE_BIO_COUNTER_TAG = "profile_settings_bio_counter"

/** Test tags on the sheet's Job Title and Company fields. */
const val PROFESSIONAL_JOB_TITLE_TAG = "profile_professional_job_title"
const val PROFESSIONAL_COMPANY_TAG = "profile_professional_company"

private val StatusSlotSize = 48.dp
private val StatusGlyphSize = 24.dp
private val StatusSpinnerStroke = 2.dp
private val LockIconSize = 20.dp
private val ReadOnlyMinHeight = 56.dp
private val BioMinHeight = 120.dp
private const val BIO_MAX_LINES = 8
private val RestBorder = 1.dp
private val FocusedBorder = 2.dp

/**
 * The save indicator in the app bar: a fixed 48 dp slot so the bar does not shift. Idle is empty, Saving a spinner,
 * Saved a check (announced politely) and Error an error glyph that re-runs the save when tapped. States cross-fade
 * over 150 ms.
 */
@Composable
fun AutoSaveStatusIcon(
    status: AutoSaveStatus,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val savingText = stringResource(R.string.profile_settings_status_saving)
    val savedText = stringResource(R.string.profile_settings_status_saved)
    val errorText = stringResource(R.string.profile_settings_status_error)
    Box(
        modifier = modifier.size(StatusSlotSize).testTag(AUTO_SAVE_STATUS_TAG),
        contentAlignment = Alignment.Center,
    ) {
        Crossfade(
            targetState = status,
            animationSpec = tween(Motion.CONTROL_STATE_DURATION_MS),
            label = "autoSaveStatus",
        ) { shown ->
            when (shown) {
                AutoSaveStatus.Idle -> Unit
                AutoSaveStatus.Saving ->
                    CircularProgressIndicator(
                        modifier = Modifier.size(StatusGlyphSize).semantics { stateDescription = savingText },
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = StatusSpinnerStroke,
                    )
                AutoSaveStatus.Saved ->
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = savedText,
                        modifier = Modifier.size(StatusGlyphSize).semantics { liveRegion = LiveRegionMode.Polite },
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                AutoSaveStatus.Error ->
                    IconButton(onClick = onRetry) {
                        Icon(
                            imageVector = Icons.Outlined.ErrorOutline,
                            contentDescription = errorText,
                            modifier = Modifier.size(StatusGlyphSize),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
            }
        }
    }
}

/** A label above a single-line [ValidatedField]; input is cut to [maxLength]. */
@Composable
fun ProfileTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    error: String? = null,
    maxLength: Int = Int.MAX_VALUE,
    keyboardOptions: KeyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            modifier = Modifier.padding(bottom = Spacing.xs),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        ValidatedField(
            value = value,
            onValueChange = { onValueChange(it.take(maxLength)) },
            placeholder = label,
            leadingIcon = icon,
            error = error,
            keyboardOptions = keyboardOptions,
        )
    }
}

/**
 * A display-only row (the email): not focusable, announced as "{label}, {value}, read only", with a muted lock.
 */
@Composable
fun ProfileReadOnlyField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(R.string.profile_settings_read_only_a11y, label, value)
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(bottom = Spacing.md)
                .clearAndSetSemantics { contentDescription = description },
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(bottom = Spacing.xs),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = ReadOnlyMinHeight)
                    .background(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.shapes.medium)
                    .border(RestBorder, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
                    .padding(horizontal = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Text(
                text = value,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = null,
                modifier = Modifier.size(LockIconSize),
                tint = MaterialTheme.extras.onSurfaceMuted,
            )
        }
    }
}

/**
 * Multi-line bio: at least 120 dp tall, at most [BIO_MAX_LINES] lines, with a "{n}/500" counter that appears from 400
 * characters and turns `tertiary` at the cap.
 */
@Composable
fun ProfileBioField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
) {
    val colorScheme = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.medium
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val borderWidth by animateDpAsState(
        targetValue = if (focused || error != null) FocusedBorder else RestBorder,
        animationSpec = tween(durationMillis = Motion.CONTROL_STATE_DURATION_MS),
        label = "bioBorderWidth",
    )
    val borderColor =
        when {
            error != null -> MaterialTheme.extras.destructive
            focused -> colorScheme.primary
            else -> colorScheme.outline
        }
    Column(modifier = modifier.fillMaxWidth().padding(bottom = Spacing.md)) {
        Text(
            text = label,
            modifier = Modifier.padding(bottom = Spacing.xs),
            style = MaterialTheme.typography.labelLarge,
            color = colorScheme.onSurface,
        )
        BasicTextField(
            value = value,
            onValueChange = { onValueChange(it.take(BIO_MAX)) },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = BioMinHeight)
                    .background(colorScheme.surfaceContainer, shape)
                    .border(borderWidth, borderColor, shape)
                    .testTag(PROFILE_BIO_FIELD_TAG)
                    .semantics { if (error != null) error(error) },
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = colorScheme.onSurface),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            maxLines = BIO_MAX_LINES,
            interactionSource = interactionSource,
            cursorBrush = SolidColor(colorScheme.primary),
        ) { innerTextField ->
            Box(modifier = Modifier.padding(Spacing.md)) {
                if (value.isEmpty()) {
                    Text(
                        text = stringResource(R.string.profile_settings_bio_placeholder),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.extras.onSurfaceMuted,
                    )
                }
                innerTextField()
            }
        }
        if (error != null) {
            Text(
                text = error,
                modifier = Modifier.padding(start = Spacing.xs, top = Spacing.xs),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.extras.destructive,
            )
        }
        if (value.length >= BIO_COUNTER_FROM) {
            Box(modifier = Modifier.fillMaxWidth().padding(top = Spacing.xs), contentAlignment = Alignment.CenterEnd) {
                Text(
                    text = stringResource(R.string.profile_settings_counter, value.length, BIO_MAX),
                    modifier = Modifier.testTag(PROFILE_BIO_COUNTER_TAG),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (value.length >= BIO_MAX) colorScheme.tertiary else MaterialTheme.extras.onSurfaceMuted,
                )
            }
        }
    }
}

/** Professional Details card (stub, implemented in the GREEN step). */
@Composable
fun ProfileProfessionalCard(
    jobTitle: String,
    company: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) = Unit

/** Body of the Professional Details sheet (stub, implemented in the GREEN step). */
@Composable
fun ProfessionalDetailsSheetContent(
    jobTitle: String,
    company: String,
    onUpdate: (jobTitle: String, company: String) -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier,
) = Unit

@Preview(name = "ProfileFields")
@Composable
private fun ProfileFieldsPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(Spacing.md)) {
                ProfileReadOnlyField(label = "Email", value = "ana@example.com")
                ProfileBioField(label = "Bio", value = "", onValueChange = {})
            }
        }
    }
}
