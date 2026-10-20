package com.uptrendinvestments.clickmeapp.screens.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.EmptyState
import com.uptrendinvestments.clickmeapp.components.EmptyStateAction
import com.uptrendinvestments.clickmeapp.components.ErrorState
import com.uptrendinvestments.clickmeapp.components.ValidatedField
import com.uptrendinvestments.clickmeapp.components.buttons.AsyncSaveButton
import com.uptrendinvestments.clickmeapp.components.buttons.SaveState
import com.uptrendinvestments.clickmeapp.components.buttonstyles.PressScaleButton
import com.uptrendinvestments.clickmeapp.components.buttonstyles.PressScaleButtonDefaults
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.extras
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val SAVING_MS = 1200L
private const val SAVED_MS = 1500L

/** Debug gallery section showing every interactive shared control in every state. */
@Composable
fun GalleryControlsSection(modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
        PressScaleBlock()
        AsyncSaveBlock()
        ValidatedFieldBlock()
        EmptyStateBlock()
    }
}

@Composable
private fun Heading(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
}

@Composable
private fun Caption(text: String) {
    Text(text = text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.extras.onSurfaceMuted)
}

@Composable
private fun PressScaleBlock() {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Heading(stringResource(R.string.gallery_heading_press_scale))
        PressScaleButton(onClick = {}) { Text(stringResource(R.string.gallery_sample_continue)) }
        Caption(stringResource(R.string.gallery_caption_filled))
        PressScaleButton(onClick = {}, colors = PressScaleButtonDefaults.tonalColors()) {
            Text(stringResource(R.string.gallery_sample_cancel))
        }
        Caption(stringResource(R.string.gallery_caption_tonal))
        PressScaleButton(onClick = {}, colors = PressScaleButtonDefaults.destructiveColors()) {
            Text(stringResource(R.string.gallery_sample_delete))
        }
        Caption(stringResource(R.string.gallery_caption_destructive))
        PressScaleButton(onClick = {}, enabled = false) { Text(stringResource(R.string.gallery_sample_continue)) }
        Caption(stringResource(R.string.gallery_caption_disabled))
    }
}

@Composable
private fun AsyncSaveBlock() {
    val title = stringResource(R.string.gallery_sample_save)
    var demoState by remember { mutableStateOf(SaveState.Idle) }
    val scope = rememberCoroutineScope()
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Heading(stringResource(R.string.gallery_heading_async_save))
        AsyncSaveButton(title = title, state = SaveState.Idle, onClick = {})
        Caption(stringResource(R.string.gallery_caption_idle))
        AsyncSaveButton(title = title, state = SaveState.Saving, onClick = {})
        Caption(stringResource(R.string.gallery_caption_saving))
        AsyncSaveButton(title = title, state = SaveState.Saved, onClick = {})
        Caption(stringResource(R.string.gallery_caption_saved))
        AsyncSaveButton(title = title, state = SaveState.Idle, onClick = {}, enabled = false)
        Caption(stringResource(R.string.gallery_caption_disabled))
        AsyncSaveButton(
            title = title,
            state = demoState,
            onClick = {
                scope.launch {
                    demoState = SaveState.Saving
                    delay(SAVING_MS)
                    demoState = SaveState.Saved
                    delay(SAVED_MS)
                    demoState = SaveState.Idle
                }
            },
        )
        Caption(stringResource(R.string.gallery_caption_interactive))
    }
}

@Composable
private fun ValidatedFieldBlock() {
    var email by rememberSaveable { mutableStateOf("") }
    var badEmail by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    Column {
        Heading(stringResource(R.string.gallery_heading_validated_field))
        ValidatedField(
            value = email,
            onValueChange = { email = it },
            placeholder = stringResource(R.string.gallery_sample_email),
            leadingIcon = Icons.Outlined.Email,
            error = null,
            modifier = Modifier.padding(top = Spacing.sm),
        )
        Caption(stringResource(R.string.gallery_caption_idle))
        ValidatedField(
            value = badEmail,
            onValueChange = { badEmail = it },
            placeholder = stringResource(R.string.gallery_sample_email),
            leadingIcon = Icons.Outlined.Email,
            error = stringResource(R.string.gallery_sample_email_error),
            modifier = Modifier.padding(top = Spacing.sm),
        )
        Caption(stringResource(R.string.gallery_caption_error))
        ValidatedField(
            value = password,
            onValueChange = { password = it },
            placeholder = stringResource(R.string.gallery_sample_password),
            leadingIcon = Icons.Outlined.Lock,
            error = null,
            isSecure = true,
            modifier = Modifier.padding(top = Spacing.sm),
        )
        Caption(stringResource(R.string.gallery_caption_secure))
    }
}

@Composable
private fun EmptyStateBlock() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Heading(stringResource(R.string.gallery_heading_empty_state))
        EmptyState(
            icon = Icons.Outlined.Inbox,
            heading = stringResource(R.string.gallery_sample_empty_heading),
            body = stringResource(R.string.gallery_sample_empty_body),
            modifier = Modifier.fillMaxWidth(),
            action = EmptyStateAction(stringResource(R.string.gallery_sample_empty_action)) {},
        )
        ErrorState(onRetry = {}, modifier = Modifier.fillMaxWidth())
        Caption(stringResource(R.string.gallery_caption_error_state))
    }
}

@Preview(name = "GalleryControlsSection")
@Composable
private fun GalleryControlsSectionPreview() {
    ClickMeTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            GalleryControlsSection(modifier = Modifier.padding(Spacing.md))
        }
    }
}
