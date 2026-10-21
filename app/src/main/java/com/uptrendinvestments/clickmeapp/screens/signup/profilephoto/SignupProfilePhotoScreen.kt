package com.uptrendinvestments.clickmeapp.screens.signup.profilephoto

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.Crop
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.avatar.Avatar
import com.uptrendinvestments.clickmeapp.components.buttons.AsyncSaveButton
import com.uptrendinvestments.clickmeapp.components.buttons.SaveState
import com.uptrendinvestments.clickmeapp.components.buttonstyles.PressScaleButton
import com.uptrendinvestments.clickmeapp.components.buttonstyles.PressScaleButtonDefaults
import com.uptrendinvestments.clickmeapp.screens.login.AuthErrorDialog
import com.uptrendinvestments.clickmeapp.screens.login.AuthTopAppBar
import com.uptrendinvestments.clickmeapp.screens.login.asString
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.extras

const val PHOTO_IMPORTING_TAG = "signup_photo_importing"

private val IconGap = Spacing.sm
private val ProgressSize = 18.dp

/**
 * Hilt-aware entry for the Profile Photo step (D-12). Upload Photo opens the system photo picker (no storage
 * permission); a picked image goes to the crop route through [onOpenCrop]; [onSaved] fires after the Saved hold.
 */
@Composable
fun SignupProfilePhotoScreen(
    onOpenCrop: () -> Unit,
    onSaved: () -> Unit,
    onBack: () -> Unit,
    viewModel: SignupProfilePhotoViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val openCrop by viewModel.openCrop.collectAsStateWithLifecycle()
    val saved by viewModel.saved.collectAsStateWithLifecycle()

    val pick =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            uri?.let(viewModel::onPicked)
        }

    // Runs on every entry, so the cropped preview appears when the crop route pops back here.
    LaunchedEffect(Unit) { viewModel.onShown() }
    LaunchedEffect(openCrop) {
        if (openCrop) {
            onOpenCrop()
            viewModel.onOpenCropConsumed()
        }
    }
    LaunchedEffect(saved) {
        if (saved) {
            onSaved()
            viewModel.onSavedConsumed()
        }
    }

    SignupProfilePhotoContent(
        state = state,
        onUpload = { pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        onAdjustCrop = onOpenCrop,
        onRemove = viewModel::requestRemove,
        onConfirmRemove = viewModel::confirmRemove,
        onDismissRemove = viewModel::dismissRemove,
        onSave = viewModel::save,
        onBack = onBack,
        onDismissDialog = viewModel::dismissDialog,
    )
}

/** Stateless Profile Photo step: header, 160 dp preview, Upload / Adjust / Remove actions, pinned Save. */
@Composable
fun SignupProfilePhotoContent(
    state: SignupProfilePhotoUiState,
    onUpload: () -> Unit,
    onAdjustCrop: () -> Unit,
    onRemove: () -> Unit,
    onConfirmRemove: () -> Unit,
    onDismissRemove: () -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
    onDismissDialog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val idle = state.saveState == SaveState.Idle
    // Leaving mid-upload would cancel viewModelScope before the Saved hold; swallow Back until it is done.
    BackHandler(enabled = !idle) {}

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            AuthTopAppBar(title = stringResource(R.string.signup_photo_title), onBack = onBack, backEnabled = idle)
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding),
        ) {
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = Spacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(Spacing.lg))
                Text(
                    text = stringResource(R.string.signup_photo_header),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { heading() },
                )
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    text = stringResource(R.string.signup_photo_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.extras.onSurfaceMuted,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(Spacing.xl))
                PhotoPreview(state)
                Spacer(Modifier.height(Spacing.xl))
                PhotoActions(
                    state = state,
                    onUpload = onUpload,
                    onAdjustCrop = onAdjustCrop,
                    onRemove = onRemove,
                )
                Spacer(Modifier.height(Spacing.xl))
            }
            AsyncSaveButton(
                title = stringResource(R.string.signup_save),
                savedTitle = stringResource(R.string.signup_saved),
                state = state.saveState,
                onClick = onSave,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(start = Spacing.lg, end = Spacing.lg, bottom = Spacing.lg),
            )
        }
    }

    if (state.showRemoveConfirm) {
        RemoveConfirmDialog(onConfirm = onConfirmRemove, onDismiss = onDismissRemove)
    }
    state.dialogError?.let { error ->
        val title = state.dialogTitleRes?.let { stringResource(it) }
        if (title != null) {
            AuthErrorDialog(message = error.asString(), onDismiss = onDismissDialog, title = title)
        } else {
            AuthErrorDialog(message = error.asString(), onDismiss = onDismissDialog)
        }
    }
}

@Composable
private fun PhotoPreview(state: SignupProfilePhotoUiState) {
    val description =
        stringResource(
            if (state.hasAnyPhoto) R.string.signup_photo_preview_a11y else R.string.signup_photo_none_a11y,
        )
    Box(
        modifier =
            Modifier
                .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                .padding(2.dp),
    ) {
        Avatar(
            url = state.avatarUrl,
            image = state.localPreview,
            size = 160.dp,
            contentDescription = description,
        )
    }
}

@Composable
private fun PhotoActions(
    state: SignupProfilePhotoUiState,
    onUpload: () -> Unit,
    onAdjustCrop: () -> Unit,
    onRemove: () -> Unit,
) {
    val idle = state.saveState == SaveState.Idle
    PressScaleButton(
        onClick = onUpload,
        enabled = !state.isImporting && idle,
        colors = PressScaleButtonDefaults.tonalColors(),
    ) {
        if (state.isImporting) {
            CircularProgressIndicator(
                modifier = Modifier.size(ProgressSize).testTag(PHOTO_IMPORTING_TAG),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onSurface,
            )
        } else {
            Icon(Icons.Outlined.AddAPhoto, contentDescription = null, modifier = Modifier.size(ProgressSize))
        }
        Spacer(Modifier.width(IconGap))
        Text(
            stringResource(
                if (state.hasAnyPhoto) R.string.signup_photo_choose_another else R.string.signup_photo_upload,
            ),
        )
    }
    if (state.hasUnsavedPhoto && state.hasSource) {
        Spacer(Modifier.height(Spacing.sm))
        PressScaleButton(
            onClick = onAdjustCrop,
            enabled = !state.isImporting && idle,
            colors = PressScaleButtonDefaults.tonalColors(),
        ) {
            Icon(Icons.Outlined.Crop, contentDescription = null, modifier = Modifier.size(ProgressSize))
            Spacer(Modifier.width(IconGap))
            Text(stringResource(R.string.signup_photo_adjust))
        }
    }
    if (state.hasAnyPhoto) {
        Spacer(Modifier.height(Spacing.sm))
        TextButton(
            onClick = onRemove,
            enabled = idle,
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.extras.destructive),
        ) {
            Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(ProgressSize))
            Spacer(Modifier.width(IconGap))
            Text(stringResource(R.string.signup_photo_remove), style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun RemoveConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.extraLarge,
        title = {
            Text(text = stringResource(R.string.signup_photo_remove_title), style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Text(
                text = stringResource(R.string.signup_photo_remove_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(R.string.signup_photo_remove_confirm),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.extras.destructive,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.signup_photo_remove_cancel),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
    )
}
