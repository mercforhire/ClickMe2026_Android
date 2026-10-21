package com.uptrendinvestments.clickmeapp.screens.signup.overview

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.buttonstyles.PressScaleButton
import com.uptrendinvestments.clickmeapp.screens.login.AuthTopAppBar
import com.uptrendinvestments.clickmeapp.screens.signup.SetupStep
import com.uptrendinvestments.clickmeapp.ui.theme.Motion
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.extras

private const val TOTAL_STEPS = 5
private val StatusCircleSize = 40.dp
private val IconSize = 24.dp

/**
 * Hilt-aware Overview hub (iOS `SignupOverviewView`). [onOpenStep] opens a step; [onNext] opens Review (D-05).
 * The hub is the root of the setup back stack, so it has no back arrow and does not intercept system Back, which exits.
 */
@Composable
fun SignupOverviewScreen(
    onOpenStep: (SetupStep) -> Unit,
    onNext: () -> Unit,
    viewModel: SignupOverviewViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.onShown() }

    SignupOverviewContent(
        state = state,
        onOpenStep = onOpenStep,
        onNext = onNext,
        onRefresh = viewModel::refresh,
        onRefreshErrorShown = viewModel::onRefreshErrorShown,
        onRequestLogout = viewModel::requestLogout,
        onDismissLogout = viewModel::dismissLogout,
        onConfirmLogout = viewModel::confirmLogout,
    )
}

/** Stateless hub: progress, five step rows and tips, with a pinned Next bar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignupOverviewContent(
    state: SignupOverviewUiState,
    onOpenStep: (SetupStep) -> Unit,
    onNext: () -> Unit,
    onRefresh: () -> Unit,
    onRefreshErrorShown: () -> Unit,
    onRequestLogout: () -> Unit,
    onDismissLogout: () -> Unit,
    onConfirmLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val failedMessage = stringResource(R.string.signup_overview_refresh_failed)
    val retryLabel = stringResource(R.string.signup_overview_retry)
    LaunchedEffect(state.refreshFailed) {
        if (state.refreshFailed) {
            val result =
                snackbarHostState.showSnackbar(
                    message = failedMessage,
                    actionLabel = retryLabel,
                    duration = SnackbarDuration.Indefinite,
                )
            if (result == SnackbarResult.ActionPerformed) onRefresh()
            onRefreshErrorShown()
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            Column {
                AuthTopAppBar(
                    title = stringResource(R.string.signup_overview_title),
                    onBack = null,
                    actions = { OverflowMenu(onLogOut = onRequestLogout) },
                )
                if (state.isRefreshing) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(4.dp))
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            PressScaleButton(
                onClick = onNext,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .padding(start = Spacing.lg, end = Spacing.lg, bottom = Spacing.lg),
            ) {
                Text(stringResource(R.string.signup_overview_next))
            }
        },
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = Spacing.lg),
            ) {
                Header()
                ProgressBlock(completedCount = state.completedCount)
                Column(
                    modifier = Modifier.padding(top = Spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    state.rows.forEach { row ->
                        StepRow(row = row, onClick = { onOpenStep(row.step) })
                    }
                }
                TipsCard()
                Spacer(Modifier.height(Spacing.xl))
            }
        }
    }

    if (state.showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = onDismissLogout,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = MaterialTheme.shapes.extraLarge,
            title = { Text(stringResource(R.string.signup_overview_logout_title)) },
            text = { Text(stringResource(R.string.signup_overview_logout_body)) },
            confirmButton = {
                TextButton(onClick = onConfirmLogout) {
                    Text(
                        text = stringResource(R.string.signup_overview_log_out),
                        color = MaterialTheme.extras.destructive,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissLogout) {
                    Text(stringResource(R.string.signup_overview_logout_cancel))
                }
            },
        )
    }
}

@Composable
private fun OverflowMenu(onLogOut: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Outlined.MoreVert,
                contentDescription = stringResource(R.string.signup_overview_more_options),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.signup_overview_log_out)) },
                leadingIcon = { Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null) },
                onClick = {
                    expanded = false
                    onLogOut()
                },
            )
        }
    }
}

@Composable
private fun Header() {
    Column(modifier = Modifier.padding(top = Spacing.lg)) {
        Text(
            text = stringResource(R.string.signup_overview_headline),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = stringResource(R.string.signup_overview_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.extras.onSurfaceMuted,
        )
    }
}

@Composable
private fun ProgressBlock(completedCount: Int) {
    val progress by animateFloatAsState(
        targetValue = completedCount.toFloat() / TOTAL_STEPS,
        animationSpec = tween(durationMillis = Motion.PUSH_POP_DURATION_MS, easing = FastOutSlowInEasing),
        label = "overviewProgress",
    )
    val a11y = stringResource(R.string.signup_overview_progress_a11y, completedCount)
    Column(
        modifier =
            Modifier
                .padding(top = Spacing.lg)
                .semantics(mergeDescendants = true) { contentDescription = a11y },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.signup_overview_progress_label),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.signup_overview_progress_value, completedCount),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.extras.onSurfaceMuted,
            )
        }
        Spacer(Modifier.height(Spacing.sm))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.extras.overlayWhite15,
            strokeCap = StrokeCap.Round,
            gapSize = 0.dp,
            drawStopIndicator = {},
        )
    }
}

@Composable
private fun StepRow(
    row: SetupStepRow,
    onClick: () -> Unit,
) {
    val stateRes =
        if (row.completed) R.string.signup_overview_state_completed else R.string.signup_overview_state_not_completed
    val stateText = stringResource(stateRes)
    val shape = MaterialTheme.shapes.medium
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
                .clickable(role = Role.Button, onClick = onClick)
                .semantics { stateDescription = stateText }
                .heightIn(min = 72.dp)
                .padding(Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        StatusCircle(completed = row.completed)
        Text(
            text = stringResource(row.step.titleRes),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.extras.onSurfaceMuted,
            modifier = Modifier.size(IconSize),
        )
    }
}

@Composable
private fun StatusCircle(completed: Boolean) {
    val fill by animateColorAsState(
        targetValue = if (completed) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = tween(durationMillis = Motion.STRENGTH_SEGMENT_COLOR_MS),
        label = "statusFill",
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .size(StatusCircleSize)
                .background(fill, CircleShape)
                .then(
                    if (completed) Modifier else Modifier.border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                ),
    ) {
        Icon(
            imageVector = if (completed) Icons.Outlined.Check else Icons.Outlined.StarBorder,
            contentDescription = null,
            tint = if (completed) MaterialTheme.colorScheme.onPrimary else MaterialTheme.extras.onSurfaceMuted,
            modifier = Modifier.size(IconSize),
        )
    }
}

@Composable
private fun TipsCard() {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.padding(top = Spacing.lg).fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lightbulb,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(IconSize),
                )
                Text(
                    text = stringResource(R.string.signup_overview_tips_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Tip(R.string.signup_overview_tip_bio_title, R.string.signup_overview_tip_bio_detail)
            Tip(R.string.signup_overview_tip_photo_title, R.string.signup_overview_tip_photo_detail)
            Tip(R.string.signup_overview_tip_expertise_title, R.string.signup_overview_tip_expertise_detail)
        }
    }
}

@Composable
private fun Tip(
    titleRes: Int,
    detailRes: Int,
) {
    Column {
        Text(
            text = stringResource(titleRes),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = stringResource(detailRes),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.extras.onSurfaceMuted,
        )
    }
}
