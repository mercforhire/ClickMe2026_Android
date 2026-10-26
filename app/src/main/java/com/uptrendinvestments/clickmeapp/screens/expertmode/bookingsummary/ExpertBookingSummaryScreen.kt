package com.uptrendinvestments.clickmeapp.screens.expertmode.bookingsummary

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.EmptyState
import com.uptrendinvestments.clickmeapp.components.EmptyStateAction
import com.uptrendinvestments.clickmeapp.components.RescheduleOutcomeDialog
import com.uptrendinvestments.clickmeapp.components.RescheduleResponseCard
import com.uptrendinvestments.clickmeapp.components.RescheduleViewer
import com.uptrendinvestments.clickmeapp.components.SkeletonBlock
import com.uptrendinvestments.clickmeapp.components.SkeletonGroup
import com.uptrendinvestments.clickmeapp.components.buttonstyles.DestructiveOutlinedButton
import com.uptrendinvestments.clickmeapp.components.call.JoinCallButton
import com.uptrendinvestments.clickmeapp.managers.isWithinJoinWindow
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.rememberDeviceTimeFormat
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.RescheduleDialog
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.RescheduleNotice
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.RescheduleProposalUi
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.RescheduleUiState
import com.uptrendinvestments.clickmeapp.screens.expertmode.bookingsummary.subviews.ExpertMessageCard
import com.uptrendinvestments.clickmeapp.screens.expertmode.bookingsummary.subviews.ExpertSummaryCard
import com.uptrendinvestments.clickmeapp.screens.login.AuthTopAppBar
import com.uptrendinvestments.clickmeapp.screens.login.asString
import com.uptrendinvestments.clickmeapp.ui.theme.Motion
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

/** Test tag on the Join button. */
const val EXPERT_SUMMARY_JOIN_TAG = "expert_summary_join"

/** Test tag on the Cancel Booking button. */
const val EXPERT_SUMMARY_CANCEL_TAG = "expert_summary_cancel"

private val CancelMinHeight = 56.dp
private val CancelIconSize = 24.dp
private val SummarySkeletonHeight = 224.dp
private val ButtonSkeletonHeight = 56.dp

/**
 * Expert Booking Summary (EXP-08 / EXP-09), opened from the Bookings tab or the Dashboard. Port of iOS
 * `ExpertBookingSummaryView` without Message, Reschedule, the meeting-link row and the private note (DEF-1, Phase 12,
 * RESEARCH C11, Phase 8 D-16).
 *
 * [onCancel] runs only when the call guard allows it (D-17); the caller pushes the Cancel route.
 */
@Composable
fun ExpertBookingSummaryScreen(
    viewModel: ExpertBookingSummaryViewModel,
    onBack: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val joinNow by viewModel.joinNow.collectAsStateWithLifecycle()
    val rescheduleState by viewModel.rescheduleState.collectAsStateWithLifecycle()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshQuietly() }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { viewModel.onScreenHidden() }
    DisposableEffect(viewModel) { onDispose { viewModel.onScreenHidden() } }
    // Nav3 may not deliver ON_RESUME to a covered entry (Phase 7 A10), for example back from an aborted Cancel screen;
    // a coalesced call is a no-op on first launch.
    LaunchedEffect(Unit) { viewModel.refreshQuietly() }

    ExpertBookingSummaryContent(
        state = state,
        rescheduleState = rescheduleState,
        joinNow = joinNow,
        onJoin = viewModel::join,
        onBack = onBack,
        onRetry = viewModel::retry,
        onCancel = { if (viewModel.attemptCancel()) onCancel() },
        onAcceptReschedule = viewModel::acceptReschedule,
        onDeclineReschedule = viewModel::declineReschedule,
        onDismissRescheduleDialog = viewModel::dismissRescheduleDialog,
        onRescheduleNoticeConsumed = viewModel::consumeRescheduleNotice,
        modifier = modifier,
    )
}

/**
 * Stateless Expert Booking Summary: loading, failed (with Retry) and loaded states under an app bar. A reschedule
 * response outcome shows as a snackbar ([RescheduleNotice]) or a "Couldn't update booking" dialog; leaving on a
 * dialog's OK ([RescheduleDialog.Error.popOnDismiss]) calls [onBack].
 */
@Composable
@Suppress("LongParameterList", "LongMethod")
fun ExpertBookingSummaryContent(
    state: ExpertBookingSummaryUiState,
    rescheduleState: RescheduleUiState,
    joinNow: Instant,
    onJoin: () -> Unit,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
    onAcceptReschedule: () -> Unit,
    onDeclineReschedule: (reason: String) -> Unit,
    onDismissRescheduleDialog: () -> Unit,
    onRescheduleNoticeConsumed: () -> Unit,
    modifier: Modifier = Modifier,
    zone: ZoneId = ZoneId.systemDefault(),
    timeFormat: java.text.DateFormat = rememberDeviceTimeFormat(),
    locale: Locale = LocalConfiguration.current.locales[0],
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val acceptedMessage = stringResource(R.string.reschedule_accepted)
    val declinedMessage = stringResource(R.string.reschedule_declined)

    LaunchedEffect(rescheduleState.notice) {
        val notice = rescheduleState.notice ?: return@LaunchedEffect
        // Launched on the composition scope so consuming the notice does not cancel the Snackbar.
        scope.launch {
            snackbarHostState.showSnackbar(
                if (notice == RescheduleNotice.Accepted) acceptedMessage else declinedMessage,
            )
        }
        onRescheduleNoticeConsumed()
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = { AuthTopAppBar(title = stringResource(R.string.expert_summary_title), onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        val bodyModifier = Modifier.fillMaxSize().padding(innerPadding)
        when (val load = state.load) {
            ExpertSummaryLoad.Loading -> LoadingBody(bodyModifier)
            is ExpertSummaryLoad.Failed ->
                Column(
                    modifier = bodyModifier,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    EmptyState(
                        icon = Icons.Outlined.ErrorOutline,
                        heading = stringResource(R.string.expert_summary_load_error),
                        body = load.message.asString(),
                        action = EmptyStateAction(stringResource(R.string.expert_retry), onRetry),
                    )
                }
            is ExpertSummaryLoad.Loaded ->
                LoadedBody(
                    details = load.details,
                    rescheduleState = rescheduleState,
                    joinNow = joinNow,
                    onJoin = onJoin,
                    onCancel = onCancel,
                    onAcceptReschedule = onAcceptReschedule,
                    onDeclineReschedule = onDeclineReschedule,
                    modifier = bodyModifier,
                    zone = zone,
                    timeFormat = timeFormat,
                    locale = locale,
                )
        }
    }

    // The expert has no payment step, so only the error dialog can appear; the payment action is never used.
    rescheduleState.dialog?.let { dialog ->
        RescheduleOutcomeDialog(
            dialog = dialog,
            onDismiss = {
                onDismissRescheduleDialog()
                if ((dialog as? RescheduleDialog.Error)?.popOnDismiss == true) onBack()
            },
            onAddPaymentMethod = {},
        )
    }
}

@Composable
private fun LoadingBody(modifier: Modifier = Modifier) {
    SkeletonGroup(modifier = modifier.padding(horizontal = Spacing.md, vertical = Spacing.md)) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            SkeletonBlock(Modifier.fillMaxWidth().height(SummarySkeletonHeight), MaterialTheme.shapes.medium)
            SkeletonBlock(Modifier.fillMaxWidth().height(ButtonSkeletonHeight), MaterialTheme.shapes.medium)
        }
    }
}

@Suppress("LongParameterList", "LongMethod")
@Composable
private fun LoadedBody(
    details: ExpertSummaryDetails,
    rescheduleState: RescheduleUiState,
    joinNow: Instant,
    onJoin: () -> Unit,
    onCancel: () -> Unit,
    onAcceptReschedule: () -> Unit,
    onDeclineReschedule: (reason: String) -> Unit,
    modifier: Modifier,
    zone: ZoneId,
    timeFormat: java.text.DateFormat,
    locale: Locale,
) {
    // Keep the last proposal so the card can fade out after the reload cleared it (UI-SPEC Motion, 200 ms).
    var lastProposal by remember { mutableStateOf<RescheduleProposalUi?>(null) }
    if (details.proposal != null && details.proposal != lastProposal) lastProposal = details.proposal

    // The Scaffold insets already cover the navigation bar.
    Column(
        modifier =
            modifier
                .verticalScroll(rememberScrollState())
                .padding(start = Spacing.md, end = Spacing.md, top = Spacing.md, bottom = Spacing.xl),
    ) {
        AnimatedVisibility(
            visible = details.proposal != null,
            enter =
                fadeIn(tween(Motion.STRENGTH_METER_VISIBILITY_MS)) +
                    expandVertically(tween(Motion.STRENGTH_METER_VISIBILITY_MS)),
            exit =
                fadeOut(tween(Motion.STRENGTH_METER_VISIBILITY_MS)) +
                    shrinkVertically(tween(Motion.STRENGTH_METER_VISIBILITY_MS)),
        ) {
            lastProposal?.let { proposal ->
                RescheduleResponseCard(
                    proposal = proposal,
                    submitting = rescheduleState.submitting,
                    onAccept = onAcceptReschedule,
                    onDecline = onDeclineReschedule,
                    modifier = Modifier.padding(bottom = Spacing.md),
                    viewer = RescheduleViewer.Expert,
                    zone = zone,
                    timeFormat = timeFormat,
                    locale = locale,
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            ExpertSummaryCard(details = details, zone = zone, timeFormat = timeFormat, locale = locale)
            details.preparationNotes?.let { ExpertMessageCard(it) }
            if (details.showsJoin) {
                JoinCallButton(
                    enabled = isWithinJoinWindow(details.statusCode, details.start, details.end, joinNow),
                    onClick = onJoin,
                    modifier = Modifier.testTag(EXPERT_SUMMARY_JOIN_TAG),
                )
            }
            if (details.canCancel) {
                DestructiveOutlinedButton(
                    onClick = onCancel,
                    enabled = !rescheduleState.isSubmitting,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = CancelMinHeight)
                            .testTag(EXPERT_SUMMARY_CANCEL_TAG),
                ) {
                    Icon(Icons.Outlined.Cancel, contentDescription = null, modifier = Modifier.size(CancelIconSize))
                    Spacer(Modifier.width(Spacing.sm))
                    Text(stringResource(R.string.expert_summary_cancel))
                }
            }
        }
    }
}
