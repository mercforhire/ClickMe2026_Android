package com.uptrendinvestments.clickmeapp.screens.signup.review

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.avatar.Avatar
import com.uptrendinvestments.clickmeapp.components.buttons.AsyncSaveButton
import com.uptrendinvestments.clickmeapp.components.buttons.SaveState
import com.uptrendinvestments.clickmeapp.screens.login.AuthErrorDialog
import com.uptrendinvestments.clickmeapp.screens.login.AuthTopAppBar
import com.uptrendinvestments.clickmeapp.screens.login.asString
import com.uptrendinvestments.clickmeapp.screens.signup.SetupStep
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.contentMaxWidth
import com.uptrendinvestments.clickmeapp.ui.theme.extras

private val RequiredIconSize = 20.dp
private val AvatarSize = 96.dp
private val ErrorIconGap = 4.dp

/**
 * Hilt-aware Review and Publish step (iOS `SignupReviewView`). [onEdit] opens the matching hub step,
 * [onPublished] fires once after the Published! hold (the app then lands on client home, D-22) and
 * [onReturnToHub] fires after the missing-fields dialog is dismissed (D-06).
 */
@Composable
fun SignupReviewScreen(
    onEdit: (SetupStep) -> Unit,
    onPublished: () -> Unit,
    onReturnToHub: () -> Unit,
    onBack: () -> Unit,
    viewModel: SignupReviewViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val published by viewModel.published.collectAsStateWithLifecycle()
    val returnToHub by viewModel.returnToHub.collectAsStateWithLifecycle()

    LaunchedEffect(published) {
        if (published) {
            onPublished()
            viewModel.onPublishedConsumed()
        }
    }
    LaunchedEffect(returnToHub) {
        if (returnToHub) {
            onReturnToHub()
            viewModel.onReturnToHubConsumed()
        }
    }

    SignupReviewContent(
        state = state,
        onEdit = onEdit,
        onPublish = viewModel::publish,
        onBack = onBack,
        onDismissMissingFields = viewModel::dismissMissingFields,
        onDismissDialog = viewModel::dismissDialog,
    )
}

/** Stateless Review screen: scrolling summary cards with the Publish bar pinned at the bottom. */
@Composable
fun SignupReviewContent(
    state: SignupReviewUiState,
    onEdit: (SetupStep) -> Unit,
    onPublish: () -> Unit,
    onBack: () -> Unit,
    onDismissMissingFields: () -> Unit,
    onDismissDialog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val busy = state.saveState != SaveState.Idle
    // Leaving mid-publish would cancel the ViewModel scope and drop the success handoff.
    BackHandler(enabled = busy) {}

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            AuthTopAppBar(
                title = stringResource(R.string.signup_review_title),
                onBack = onBack,
                backEnabled = !busy,
            )
        },
        bottomBar = { PublishBar(state, onPublish) },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .contentMaxWidth()
                    .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            ReviewCard(R.string.signup_review_card_personal, SetupStep.PersonalDetails, onEdit) {
                ReviewRow(R.string.signup_review_label_first_name, state.firstName)
                ReviewRow(R.string.signup_review_label_location, state.location)
                ReviewRow(R.string.signup_review_label_languages, state.languages)
            }
            ReviewCard(R.string.signup_review_card_timezone, SetupStep.Timezone, onEdit) {
                ReviewRow(R.string.signup_review_label_timezone, state.timezoneLabel)
            }
            ReviewCard(R.string.signup_review_card_photo, SetupStep.ProfilePhoto, onEdit) {
                if (state.avatarUrl == null) {
                    RequiredMarker()
                } else {
                    Avatar(
                        url = state.avatarUrl,
                        contentDescription = null,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        size = AvatarSize,
                    )
                }
            }
            ReviewCard(R.string.signup_review_card_expertise, SetupStep.Expertise, onEdit) {
                ReviewRow(R.string.signup_review_label_skills, state.skills)
            }
        }
    }

    state.missingFieldsDialog?.let { steps ->
        val names = steps.map { stringResource(it.titleRes) }.joinToString(", ")
        AuthErrorDialog(
            message = stringResource(R.string.signup_review_missing_body, names),
            onDismiss = onDismissMissingFields,
            title = stringResource(R.string.signup_review_error_title),
        )
    }
    state.publishRefusal?.let { steps ->
        val names = steps.map { stringResource(it.titleRes) }.joinToString(", ")
        AuthErrorDialog(
            message = stringResource(R.string.signup_review_finish_first, names),
            onDismiss = onDismissDialog,
            title = stringResource(R.string.signup_review_error_title),
        )
    }
    state.dialogError?.let { error ->
        AuthErrorDialog(
            message = error.asString(),
            onDismiss = onDismissDialog,
            title = stringResource(R.string.signup_review_error_title),
        )
    }
}

@Composable
private fun PublishBar(
    state: SignupReviewUiState,
    onPublish: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .contentMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        AsyncSaveButton(
            title = stringResource(R.string.signup_review_publish),
            savedTitle = stringResource(R.string.signup_review_published),
            state = state.saveState,
            onClick = onPublish,
        )
    }
}

@Composable
private fun ReviewCard(
    @StringRes titleRes: Int,
    step: SetupStep,
    onEdit: (SetupStep) -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val title = stringResource(titleRes)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(Spacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { onEdit(step) }) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = stringResource(R.string.signup_review_edit_a11y, title),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
                content = content,
            )
        }
    }
}

@Composable
private fun ReviewRow(
    @StringRes labelRes: Int,
    value: String?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (value == null) {
            RequiredMarker()
        } else {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.extras.onSurfaceMuted,
            )
        }
    }
}

@Composable
private fun RequiredMarker() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Outlined.ErrorOutline,
            contentDescription = null,
            modifier = Modifier.size(RequiredIconSize),
            tint = MaterialTheme.extras.destructive,
        )
        Spacer(Modifier.size(ErrorIconGap))
        Text(
            text = stringResource(R.string.signup_review_required),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.extras.destructive,
        )
    }
}
