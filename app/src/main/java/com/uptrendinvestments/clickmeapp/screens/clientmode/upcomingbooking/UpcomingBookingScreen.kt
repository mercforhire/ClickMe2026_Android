package com.uptrendinvestments.clickmeapp.screens.clientmode.upcomingbooking

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.EmptyState
import com.uptrendinvestments.clickmeapp.components.EmptyStateAction
import com.uptrendinvestments.clickmeapp.components.RescheduleOutcomeDialog
import com.uptrendinvestments.clickmeapp.components.RescheduleResponseCard
import com.uptrendinvestments.clickmeapp.components.SkeletonBlock
import com.uptrendinvestments.clickmeapp.components.SkeletonGroup
import com.uptrendinvestments.clickmeapp.components.buttonstyles.DestructiveOutlinedButton
import com.uptrendinvestments.clickmeapp.components.chat.ChatEntryErrorDialog
import com.uptrendinvestments.clickmeapp.components.chat.MessageButton
import com.uptrendinvestments.clickmeapp.managers.isWithinJoinWindow
import com.uptrendinvestments.clickmeapp.screens.chat.ChatEntryViewModel
import com.uptrendinvestments.clickmeapp.screens.chat.ChatPeer
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.rememberDeviceTimeFormat
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.RescheduleDialog
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.RescheduleNotice
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.RescheduleProposalUi
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.RescheduleUiState
import com.uptrendinvestments.clickmeapp.screens.clientmode.upcomingbooking.subviews.UpcomingBookingDateTimeCard
import com.uptrendinvestments.clickmeapp.screens.clientmode.upcomingbooking.subviews.UpcomingBookingExpertCard
import com.uptrendinvestments.clickmeapp.screens.clientmode.upcomingbooking.subviews.UpcomingBookingJoinCard
import com.uptrendinvestments.clickmeapp.screens.clientmode.upcomingbooking.subviews.UpcomingBookingMessageCard
import com.uptrendinvestments.clickmeapp.screens.clientmode.upcomingbooking.subviews.UpcomingBookingPaymentPill
import com.uptrendinvestments.clickmeapp.screens.clientmode.upcomingbooking.subviews.UpcomingBookingPendingBanner
import com.uptrendinvestments.clickmeapp.screens.clientmode.upcomingbooking.subviews.UpcomingBookingStatusRow
import com.uptrendinvestments.clickmeapp.screens.clientmode.upcomingbooking.subviews.UpcomingBookingTimezoneNote
import com.uptrendinvestments.clickmeapp.screens.clientmode.upcomingbooking.subviews.UpcomingBookingTopicCard
import com.uptrendinvestments.clickmeapp.screens.login.AuthTopAppBar
import com.uptrendinvestments.clickmeapp.screens.login.asString
import com.uptrendinvestments.clickmeapp.ui.theme.Motion
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

/** Test tag on the Cancel Booking button. */
const val UPCOMING_BOOKING_CANCEL_TAG = "upcoming_booking_cancel"

/** Test tag on the Message Expert button. */
const val MESSAGE_EXPERT_TAG = "message_expert"

private val CancelMinHeight = 56.dp
private val CancelIconSize = 24.dp
private val StatusSkeletonSize = 96.dp
private val StatusSkeletonHeight = 32.dp
private val IdSkeletonWidth = 80.dp
private val IdSkeletonHeight = 16.dp
private val ExpertSkeletonHeight = 104.dp
private val TopicSkeletonHeight = 144.dp
private val DateSkeletonHeight = 128.dp

/**
 * Upcoming Booking Details (BOOK-06), opened from a Today's Sessions or Upcoming card. Port of iOS
 * `UpcomingBookingView` without Reschedule and the private note (DEF-1, Phase 13). Message Expert is present (D-14): it
 * opens the chat with the booking's expert through [ChatEntryViewModel].
 *
 * D-01: a pending reschedule proposal shows the shared reschedule card first, which fixes the iOS dead end where the
 * client could not respond. D-14: the status, price and payment pill are real. D-15: Cancel is limited to confirmed,
 * pending approval and pending reschedule. The join card has a live Join Call (D-01) enabled by the shared join window
 * ([isWithinJoinWindow], ticking every 30 s, D-02 / D-03), and Cancel is blocked while this booking's call is live (D-13).
 *
 * The screen reloads quietly on resume and when it re-enters composition, so returning from Payment Methods (D-02)
 * shows fresh data.
 */
@Composable
fun UpcomingBookingScreen(
    viewModel: UpcomingBookingViewModel,
    onBack: () -> Unit,
    onCancel: () -> Unit,
    onOpenPaymentMethods: () -> Unit,
    modifier: Modifier = Modifier,
    chatEntry: ChatEntryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val rescheduleState by viewModel.rescheduleState.collectAsStateWithLifecycle()
    val joinNow by viewModel.joinNow.collectAsStateWithLifecycle()
    val entryState by chatEntry.state.collectAsStateWithLifecycle()
    ChatEntryErrorDialog(error = entryState.error, onDismiss = chatEntry::dismissError)

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshQuietly() }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { viewModel.onScreenHidden() }
    DisposableEffect(viewModel) { onDispose { viewModel.onScreenHidden() } }
    // Nav3 may not deliver ON_RESUME to a covered entry (Phase 7 A10); a coalesced call is a no-op on first launch.
    LaunchedEffect(Unit) { viewModel.refreshQuietly() }

    UpcomingBookingContent(
        state = state,
        rescheduleState = rescheduleState,
        joinNow = joinNow,
        onJoin = viewModel::join,
        onBack = onBack,
        onRetry = viewModel::retry,
        onRefresh = viewModel::refresh,
        onRefreshFailureConsumed = viewModel::onRefreshFailureConsumed,
        onCancel = { if (viewModel.attemptCancel()) onCancel() },
        onAccept = viewModel::accept,
        onDecline = viewModel::decline,
        onDismissRescheduleDialog = viewModel::dismissRescheduleDialog,
        onOpenPaymentMethods = onOpenPaymentMethods,
        onRescheduleNoticeConsumed = viewModel::consumeRescheduleNotice,
        modifier = modifier,
        messageBusy = entryState.busyPeerId != null,
        onMessage = {
            val details = (state as? UpcomingBookingUiState.Loaded)?.details
            details?.expertId?.let {
                chatEntry.open(ChatPeer(it, details.expertName, details.expertAvatarUrl, isExpert = true))
            }
        },
    )
}

/** Stateless Upcoming Booking Details: loading, failed (with Retry) and loaded states under an app bar. */
@Suppress("LongParameterList")
@Composable
fun UpcomingBookingContent(
    state: UpcomingBookingUiState,
    rescheduleState: RescheduleUiState,
    joinNow: Instant,
    onJoin: () -> Unit,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
    onAccept: () -> Unit,
    onDecline: (reason: String) -> Unit,
    onDismissRescheduleDialog: () -> Unit,
    onOpenPaymentMethods: () -> Unit,
    onRescheduleNoticeConsumed: () -> Unit,
    modifier: Modifier = Modifier,
    messageBusy: Boolean = false,
    onMessage: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onRefreshFailureConsumed: () -> Unit = {},
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
                if (notice ==
                    RescheduleNotice.Accepted
                ) {
                    acceptedMessage
                } else {
                    declinedMessage
                },
            )
        }
        onRescheduleNoticeConsumed()
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = { AuthTopAppBar(title = stringResource(R.string.upcoming_booking_title), onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        val bodyModifier = Modifier.fillMaxSize().padding(innerPadding)
        when (state) {
            UpcomingBookingUiState.Loading -> LoadingBody(bodyModifier)
            is UpcomingBookingUiState.Failed ->
                Column(
                    modifier = bodyModifier,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    EmptyState(
                        icon = Icons.Outlined.ErrorOutline,
                        heading = stringResource(R.string.upcoming_booking_load_error_title),
                        body = state.message.asString(),
                        action = EmptyStateAction(stringResource(R.string.section_retry), onRetry),
                    )
                }
            UpcomingBookingUiState.Unavailable -> Unit
            is UpcomingBookingUiState.Loaded ->
                LoadedBody(
                    details = state.details,
                    rescheduleState = rescheduleState,
                    joinNow = joinNow,
                    onJoin = onJoin,
                    onCancel = onCancel,
                    onAccept = onAccept,
                    onDecline = onDecline,
                    messageBusy = messageBusy,
                    onMessage = onMessage,
                    modifier = bodyModifier,
                    zone = zone,
                    timeFormat = timeFormat,
                    locale = locale,
                )
        }
    }

    rescheduleState.dialog?.let { dialog ->
        RescheduleOutcomeDialog(
            dialog = dialog,
            onDismiss = {
                onDismissRescheduleDialog()
                if ((dialog as? RescheduleDialog.Error)?.popOnDismiss == true) onBack()
            },
            onAddPaymentMethod = onOpenPaymentMethods,
        )
    }
}

@Composable
private fun LoadingBody(modifier: Modifier = Modifier) {
    SkeletonGroup(modifier = modifier.padding(horizontal = Spacing.md, vertical = Spacing.md)) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SkeletonBlock(Modifier.size(width = StatusSkeletonSize, height = StatusSkeletonHeight))
                SkeletonBlock(Modifier.size(width = IdSkeletonWidth, height = IdSkeletonHeight))
            }
            Spacer(Modifier.height(Spacing.md))
            SkeletonBlock(
                Modifier.fillMaxWidth().height(ExpertSkeletonHeight),
                MaterialTheme.shapes.medium,
            )
            Spacer(Modifier.height(Spacing.md))
            SkeletonBlock(
                Modifier.fillMaxWidth().height(TopicSkeletonHeight),
                MaterialTheme.shapes.medium,
            )
            Spacer(Modifier.height(Spacing.md))
            SkeletonBlock(
                Modifier.fillMaxWidth().height(DateSkeletonHeight),
                MaterialTheme.shapes.medium,
            )
        }
    }
}

@Suppress("LongParameterList", "LongMethod")
@Composable
private fun LoadedBody(
    details: UpcomingBookingDetails,
    rescheduleState: RescheduleUiState,
    joinNow: Instant,
    onJoin: () -> Unit,
    onCancel: () -> Unit,
    onAccept: () -> Unit,
    onDecline: (reason: String) -> Unit,
    messageBusy: Boolean,
    onMessage: () -> Unit,
    modifier: Modifier,
    zone: ZoneId,
    timeFormat: java.text.DateFormat,
    locale: Locale,
) {
    // The Scaffold insets already cover the navigation bar.
    // Keep the last proposal so the card can fade out after the reload cleared it (UI-SPEC Motion, 200 ms).
    var lastProposal by remember { mutableStateOf<RescheduleProposalUi?>(null) }
    if (details.reschedule != null && details.reschedule != lastProposal) lastProposal = details.reschedule

    Column(
        modifier =
            modifier
                .verticalScroll(rememberScrollState())
                .padding(start = Spacing.md, end = Spacing.md, top = Spacing.md, bottom = Spacing.xl),
    ) {
        AnimatedVisibility(
            visible = details.reschedule != null,
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
                    onAccept = onAccept,
                    onDecline = onDecline,
                    modifier = Modifier.padding(bottom = Spacing.md),
                    zone = zone,
                    timeFormat = timeFormat,
                    locale = locale,
                )
            }
        }
        UpcomingBookingStatusRow(details.status, details.shortId)
        Spacer(Modifier.height(Spacing.md))
        UpcomingBookingExpertCard(details.expertName, details.expertTitle, details.expertAvatarUrl)
        Spacer(Modifier.height(Spacing.md))
        UpcomingBookingTopicCard(details.topicTitle, details.price)
        Spacer(Modifier.height(Spacing.md))
        UpcomingBookingDateTimeCard(details.start, details.end, zone, timeFormat, locale)
        Spacer(Modifier.height(Spacing.sm))
        UpcomingBookingTimezoneNote(details.expertTimezone, details.start.atZone(zone).toLocalDate(), locale)
        details.paymentPill?.let {
            Spacer(Modifier.height(Spacing.md))
            UpcomingBookingPaymentPill(it)
        }
        Spacer(Modifier.height(Spacing.md))
        if (details.showsPendingBanner) {
            UpcomingBookingPendingBanner(details.expertName)
        } else {
            UpcomingBookingJoinCard(
                meetingType = details.meetingType,
                joinLink = details.joinLink,
                joinEnabled = isWithinJoinWindow(details.status.raw, details.start, details.end, joinNow),
                onJoin = onJoin,
            )
        }
        details.clientNotes?.let {
            Spacer(Modifier.height(Spacing.md))
            UpcomingBookingMessageCard(it)
        }
        Spacer(Modifier.height(Spacing.md))
        MessageButton(
            label = stringResource(R.string.chat_entry_message_expert),
            onClick = onMessage,
            busy = messageBusy,
            enabled = details.expertId != null,
            modifier = Modifier.fillMaxWidth().testTag(MESSAGE_EXPERT_TAG),
        )
        if (details.canCancel) {
            Spacer(Modifier.height(Spacing.lg))
            DestructiveOutlinedButton(
                onClick = onCancel,
                enabled = !rescheduleState.isSubmitting,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = CancelMinHeight)
                        .testTag(UPCOMING_BOOKING_CANCEL_TAG),
            ) {
                Icon(Icons.Outlined.Cancel, contentDescription = null, modifier = Modifier.size(CancelIconSize))
                Spacer(Modifier.width(Spacing.sm))
                Text(stringResource(R.string.upcoming_booking_cancel))
            }
        }
    }
}
