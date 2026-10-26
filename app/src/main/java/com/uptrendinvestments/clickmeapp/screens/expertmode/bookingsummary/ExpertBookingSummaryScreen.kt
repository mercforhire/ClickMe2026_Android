package com.uptrendinvestments.clickmeapp.screens.expertmode.bookingsummary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.EmptyState
import com.uptrendinvestments.clickmeapp.components.EmptyStateAction
import com.uptrendinvestments.clickmeapp.components.SkeletonBlock
import com.uptrendinvestments.clickmeapp.components.SkeletonGroup
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.rememberDeviceTimeFormat
import com.uptrendinvestments.clickmeapp.screens.expertmode.bookingsummary.subviews.ExpertSummaryCard
import com.uptrendinvestments.clickmeapp.screens.login.AuthTopAppBar
import com.uptrendinvestments.clickmeapp.screens.login.asString
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import java.time.ZoneId
import java.util.Locale

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

    ExpertBookingSummaryContent(
        state = state,
        onBack = onBack,
        onRetry = viewModel::retry,
        onCancel = onCancel,
        modifier = modifier,
    )
}

/** Stateless Expert Booking Summary: loading, failed (with Retry) and loaded states under an app bar. */
@Composable
fun ExpertBookingSummaryContent(
    state: ExpertBookingSummaryUiState,
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

@Composable
private fun LoadedBody(
    details: ExpertSummaryDetails,
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
    }
}
