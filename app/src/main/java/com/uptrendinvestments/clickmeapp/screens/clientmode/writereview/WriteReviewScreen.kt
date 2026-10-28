package com.uptrendinvestments.clickmeapp.screens.clientmode.writereview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.screens.clientmode.writereview.subviews.AlreadySubmittedBanner
import com.uptrendinvestments.clickmeapp.screens.clientmode.writereview.subviews.ReadOnlyReviewComment
import com.uptrendinvestments.clickmeapp.screens.clientmode.writereview.subviews.ReviewStarInput
import com.uptrendinvestments.clickmeapp.screens.clientmode.writereview.subviews.ReviewTextField
import com.uptrendinvestments.clickmeapp.screens.clientmode.writereview.subviews.WriteReviewFooter
import com.uptrendinvestments.clickmeapp.screens.clientmode.writereview.subviews.WriteReviewHeader
import com.uptrendinvestments.clickmeapp.screens.login.AuthTopAppBar
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing

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
            if (ready) {
                // The footer owns the keyboard inset, so the scroll area shrinks with it (no second imePadding).
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
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
        }
    }
}
