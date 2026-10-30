package com.uptrendinvestments.clickmeapp.screens.clientmode.writereview

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.EmptyState
import com.uptrendinvestments.clickmeapp.components.EmptyStateAction
import com.uptrendinvestments.clickmeapp.screens.clientmode.writereview.subviews.AlreadySubmittedBanner
import com.uptrendinvestments.clickmeapp.screens.clientmode.writereview.subviews.ReadOnlyReviewComment
import com.uptrendinvestments.clickmeapp.screens.clientmode.writereview.subviews.ReviewStarInput
import com.uptrendinvestments.clickmeapp.screens.clientmode.writereview.subviews.ReviewTextField
import com.uptrendinvestments.clickmeapp.screens.clientmode.writereview.subviews.WriteReviewFooter
import com.uptrendinvestments.clickmeapp.screens.clientmode.writereview.subviews.WriteReviewHeader
import com.uptrendinvestments.clickmeapp.screens.clientmode.writereview.subviews.WriteReviewSkeleton
import com.uptrendinvestments.clickmeapp.screens.login.AuthErrorDialog
import com.uptrendinvestments.clickmeapp.screens.login.AuthTopAppBar
import com.uptrendinvestments.clickmeapp.screens.login.asString
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.contentMaxWidth

/**
 * Write Review bound to its [viewModel]. A successful submit calls [onSubmitted] once, then tells the ViewModel the
 * pop has been handled.
 */
@Composable
fun WriteReviewScreen(
    viewModel: WriteReviewViewModel,
    onBack: () -> Unit,
    onSubmitted: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.submitted) {
        if (state.submitted) {
            onSubmitted()
            viewModel.onSubmittedHandled()
        }
    }
    WriteReviewContent(
        state = state,
        onBack = onBack,
        onRetry = viewModel::retry,
        onRatingSelected = viewModel::onRatingSelected,
        onCommentChanged = viewModel::onCommentChanged,
        onSubmit = viewModel::submit,
        onDismissError = viewModel::dismissSubmitError,
    )
}

/** Stateless Write Review: app bar, the form (or its load state) and the pinned footer. */
@Composable
fun WriteReviewContent(
    state: WriteReviewUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onRatingSelected: (Int) -> Unit,
    onCommentChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    onDismissError: () -> Unit,
) {
    val ready = state.load is WriteReviewLoad.Ready
    // The later-registered enabled handler wins, so an in-flight submit swallows Back (Phase 8 convention).
    BackHandler(enabled = state.submitting) {}
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            AuthTopAppBar(
                title = stringResource(R.string.write_review_title),
                onBack = onBack,
                backEnabled = !state.submitting,
            )
        },
        bottomBar = {
            if (ready && !state.readOnly) {
                WriteReviewFooter(submitting = state.submitting, onSubmit = onSubmit)
            }
        },
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            when (val load = state.load) {
                WriteReviewLoad.Loading -> WriteReviewSkeleton()
                is WriteReviewLoad.Failed ->
                    EmptyState(
                        icon = Icons.Outlined.ErrorOutline,
                        heading = stringResource(R.string.write_review_load_error_title),
                        body = load.message.asString(),
                        modifier =
                            Modifier
                                .align(Alignment.Center)
                                .semantics { liveRegion = LiveRegionMode.Polite },
                        action = EmptyStateAction(stringResource(R.string.section_retry), onRetry),
                    )
                WriteReviewLoad.Ready ->
                    ReviewForm(
                        state = state,
                        onRatingSelected = onRatingSelected,
                        onCommentChanged = onCommentChanged,
                    )
            }
        }
    }
    state.submitError?.let { error ->
        AuthErrorDialog(
            message = error.asString(),
            onDismiss = onDismissError,
            title = stringResource(R.string.write_review_submit_error_title),
            confirmLabel = stringResource(R.string.write_review_edit_review),
        )
    }
}

/** The scrolling form. The footer owns the keyboard inset, so this area shrinks with it (no second imePadding). */
@Composable
private fun ReviewForm(
    state: WriteReviewUiState,
    onRatingSelected: (Int) -> Unit,
    onCommentChanged: (String) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .contentMaxWidth()
                .padding(horizontal = Spacing.md)
                .padding(top = Spacing.lg, bottom = Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        WriteReviewHeader(header = state.header)
        if (state.readOnly) AlreadySubmittedBanner()
        ReviewStarInput(
            rating = state.rating,
            readOnly = state.readOnly,
            enabled = !state.submitting,
            onSelect = onRatingSelected,
        )
        if (state.readOnly) {
            ReadOnlyReviewComment(comment = state.comment)
        } else {
            ReviewTextField(
                value = state.comment,
                onValueChange = onCommentChanged,
                enabled = !state.submitting,
                showCounter = state.showCounter,
                atCap = state.atCap,
            )
        }
    }
}
