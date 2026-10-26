package com.uptrendinvestments.clickmeapp.screens.expertmode.bookingsummary

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.uptrendinvestments.clickmeapp.components.SkeletonBlock
import com.uptrendinvestments.clickmeapp.components.SkeletonGroup
import com.uptrendinvestments.clickmeapp.components.buttonstyles.DestructiveOutlinedButton
import com.uptrendinvestments.clickmeapp.components.call.JoinCallButton
import com.uptrendinvestments.clickmeapp.managers.isWithinJoinWindow
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.rememberDeviceTimeFormat
import com.uptrendinvestments.clickmeapp.screens.expertmode.bookingsummary.subviews.ExpertMessageCard
import com.uptrendinvestments.clickmeapp.screens.expertmode.bookingsummary.subviews.ExpertSummaryCard
import com.uptrendinvestments.clickmeapp.screens.login.AuthTopAppBar
import com.uptrendinvestments.clickmeapp.screens.login.asString
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
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

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshQuietly() }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { viewModel.onScreenHidden() }
    DisposableEffect(viewModel) { onDispose { viewModel.onScreenHidden() } }
    // Nav3 may not deliver ON_RESUME to a covered entry (Phase 7 A10), for example back from an aborted Cancel screen;
    // a coalesced call is a no-op on first launch.
    LaunchedEffect(Unit) { viewModel.refreshQuietly() }

    ExpertBookingSummaryContent(
        state = state,
        joinNow = joinNow,
        onJoin = viewModel::join,
        onBack = onBack,
        onRetry = viewModel::retry,
        onCancel = { if (viewModel.attemptCancel()) onCancel() },
        modifier = modifier,
    )
}

/** Stateless Expert Booking Summary: loading, failed (with Retry) and loaded states under an app bar. */
@Composable
@Suppress("LongParameterList")
fun ExpertBookingSummaryContent(
    state: ExpertBookingSummaryUiState,
    joinNow: Instant,
    onJoin: () -> Unit,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    zone: ZoneId = ZoneId.systemDefault(),
    timeFormat: java.text.DateFormat = rememberDeviceTimeFormat(),
    locale: Locale = LocalConfiguration.current.locales[0],
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = { AuthTopAppBar(title = stringResource(R.string.expert_summary_title), onBack = onBack) },
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
                    joinNow = joinNow,
                    onJoin = onJoin,
                    onCancel = onCancel,
                    modifier = bodyModifier,
                    zone = zone,
                    timeFormat = timeFormat,
                    locale = locale,
                )
        }
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

@Suppress("LongParameterList")
@Composable
private fun LoadedBody(
    details: ExpertSummaryDetails,
    joinNow: Instant,
    onJoin: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier,
    zone: ZoneId,
    timeFormat: java.text.DateFormat,
    locale: Locale,
) {
    // The Scaffold insets already cover the navigation bar.
    Column(
        modifier =
            modifier
                .verticalScroll(rememberScrollState())
                .padding(start = Spacing.md, end = Spacing.md, top = Spacing.md, bottom = Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
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
