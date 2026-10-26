package com.uptrendinvestments.clickmeapp.screens.expertmode.cancellation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.EmptyState
import com.uptrendinvestments.clickmeapp.components.EmptyStateAction
import com.uptrendinvestments.clickmeapp.components.SkeletonBlock
import com.uptrendinvestments.clickmeapp.components.SkeletonGroup
import com.uptrendinvestments.clickmeapp.screens.clientmode.cancellation.CancelStep
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.rememberDeviceTimeFormat
import com.uptrendinvestments.clickmeapp.screens.expertmode.cancellation.subviews.ExpertCancelConfirmStep
import com.uptrendinvestments.clickmeapp.screens.expertmode.cancellation.subviews.ExpertCancelReasonStep
import com.uptrendinvestments.clickmeapp.screens.login.AuthTopAppBar
import com.uptrendinvestments.clickmeapp.screens.login.asString
import com.uptrendinvestments.clickmeapp.ui.theme.Motion
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import java.time.ZoneId
import java.util.Locale

private val HeadlineSkeleton = 240.dp
private val HeadlineSkeletonHeight = 28.dp
private val SubheadSkeletonHeight = 16.dp
private val RowSkeletonHeight = 56.dp
private val FieldSkeletonHeight = 112.dp
private const val REASON_ROW_SKELETONS = 4
private const val SLIDE_FRACTION_DIVISOR = 3

/**
 * Expert Cancel Booking screen (D-17): pushed full screen with two steps, reason then confirm. System and app-bar Back
 * on the confirm step return to the reason step; on the reason step they pop to the booking summary. Keep Booking
 * also pops one screen.
 *
 * When the cancel succeeds the ViewModel has already posted the Bookings launch request, so [onCancelled] only has to
 * reset to the Bookings tab. It runs once per success.
 */
@Composable
fun ExpertCancellationScreen(
    viewModel: ExpertCancellationViewModel,
    onBack: () -> Unit,
    onCancelled: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ExpertCancellationContent(
        state = state,
        onBack = onBack,
        onRetry = viewModel::retry,
        onSelectReason = viewModel::selectReason,
        onCommentChange = viewModel::updateComment,
        onContinue = viewModel::continueToConfirm,
        onBackToReason = viewModel::backToReason,
        onConfirm = viewModel::confirm,
        onFinished = {
            viewModel.consumeFinished()
            onCancelled()
        },
        modifier = modifier,
    )
}

/**
 * Stateless expert cancel screen. Back rules: on the confirm step the app-bar arrow and system Back call
 * [onBackToReason]; on the reason step they call [onBack]. While the cancel call runs
 * ([ExpertCancellationUiState.isSubmitting]) system Back is swallowed and the arrow is disabled, so the request is
 * not abandoned half way.
 */
@Composable
fun ExpertCancellationContent(
    state: ExpertCancellationUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onSelectReason: (ExpertCancelReason) -> Unit,
    onCommentChange: (String) -> Unit,
    onContinue: () -> Unit,
    onBackToReason: () -> Unit,
    onConfirm: () -> Unit,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    zone: ZoneId = ZoneId.systemDefault(),
    timeFormat: java.text.DateFormat = rememberDeviceTimeFormat(),
    locale: Locale = LocalConfiguration.current.locales[0],
) {
    // The later-registered enabled handler wins, so the in-flight one swallows Back.
    BackHandler(enabled = state.step == CancelStep.Confirm && !state.isSubmitting) { onBackToReason() }
    BackHandler(enabled = state.isSubmitting) {}

    LaunchedEffect(state.isFinished) {
        if (state.isFinished) onFinished()
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            AuthTopAppBar(
                title = stringResource(R.string.cancel_booking_title),
                onBack = { if (state.step == CancelStep.Confirm) onBackToReason() else onBack() },
                backEnabled = !state.isSubmitting,
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (val load = state.load) {
                ExpertCancelLoad.Loading -> LoadingBody()
                is ExpertCancelLoad.Failed -> FailedBody(message = load.message.asString(), onRetry = onRetry)
                is ExpertCancelLoad.Loaded ->
                    AnimatedContent(
                        targetState = state.step,
                        transitionSpec = { stepTransition(forward = targetState.ordinal > initialState.ordinal) },
                        label = "expertCancelStep",
                    ) { step ->
                        when (step) {
                            CancelStep.Reason ->
                                ExpertCancelReasonStep(
                                    reason = state.reason,
                                    comment = state.comment,
                                    onSelectReason = onSelectReason,
                                    onCommentChange = onCommentChange,
                                    onContinue = onContinue,
                                )
                            CancelStep.Confirm ->
                                ExpertCancelConfirmStep(
                                    summary = load.summary,
                                    refund = state.refund,
                                    isSubmitting = state.isSubmitting,
                                    onKeepBooking = onBack,
                                    onConfirm = onConfirm,
                                    zone = zone,
                                    timeFormat = timeFormat,
                                    locale = locale,
                                )
                        }
                    }
            }
        }
    }
}

/** 300 ms slide from the end plus fade going forward, reversed going back (UI-SPEC Motion). */
private fun stepTransition(forward: Boolean): ContentTransform {
    val sign = if (forward) 1 else -1
    val offsetSpec = tween<IntOffset>(Motion.PUSH_POP_DURATION_MS, easing = Motion.pushPopEasing)
    val fadeSpec = tween<Float>(Motion.PUSH_POP_DURATION_MS, easing = Motion.pushPopEasing)
    return (
        slideInHorizontally(offsetSpec) { sign * it / SLIDE_FRACTION_DIVISOR } + fadeIn(fadeSpec)
    ) togetherWith (
        slideOutHorizontally(offsetSpec) { -sign * it / SLIDE_FRACTION_DIVISOR } + fadeOut(fadeSpec)
    )
}

@Composable
private fun LoadingBody(modifier: Modifier = Modifier) {
    SkeletonGroup(modifier = modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }) {
        Column(
            modifier = Modifier.padding(start = Spacing.md, end = Spacing.md, top = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            SkeletonBlock(Modifier.width(HeadlineSkeleton).height(HeadlineSkeletonHeight))
            SkeletonBlock(Modifier.fillMaxWidth().height(SubheadSkeletonHeight))
            repeat(REASON_ROW_SKELETONS) {
                SkeletonBlock(Modifier.fillMaxWidth().height(RowSkeletonHeight))
            }
            SkeletonBlock(Modifier.fillMaxWidth().height(FieldSkeletonHeight))
        }
    }
}

@Composable
private fun FailedBody(
    message: String,
    onRetry: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize().semantics { liveRegion = LiveRegionMode.Polite },
        contentAlignment = Alignment.Center,
    ) {
        EmptyState(
            icon = Icons.Outlined.ErrorOutline,
            heading = stringResource(R.string.cancel_booking_load_error_title),
            body = message,
            action = EmptyStateAction(stringResource(R.string.section_retry), onRetry),
        )
    }
}
