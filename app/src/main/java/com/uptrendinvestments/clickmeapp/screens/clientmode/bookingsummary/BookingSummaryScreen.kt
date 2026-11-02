package com.uptrendinvestments.clickmeapp.screens.clientmode.bookingsummary

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
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
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
import com.uptrendinvestments.clickmeapp.components.booknote.MyNoteCard
import com.uptrendinvestments.clickmeapp.components.buttonstyles.PressScaleButton
import com.uptrendinvestments.clickmeapp.components.chat.ChatEntryErrorDialog
import com.uptrendinvestments.clickmeapp.components.chat.MessageButton
import com.uptrendinvestments.clickmeapp.screens.chat.ChatEntryViewModel
import com.uptrendinvestments.clickmeapp.screens.chat.ChatPeer
import com.uptrendinvestments.clickmeapp.screens.clientmode.bookingnote.ClientBookingNoteKey
import com.uptrendinvestments.clickmeapp.screens.clientmode.bookingsummary.subviews.BOOKING_SUMMARY_BOOK_AGAIN_TAG
import com.uptrendinvestments.clickmeapp.screens.clientmode.bookingsummary.subviews.BookingSummaryCard
import com.uptrendinvestments.clickmeapp.screens.clientmode.bookingsummary.subviews.BookingSummaryFeedback
import com.uptrendinvestments.clickmeapp.screens.clientmode.bookingsummary.subviews.LeaveReviewButton
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.BookingKey
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.rememberDeviceTimeFormat
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.RescheduleDialog
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.RescheduleNotice
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.RescheduleProposalUi
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.RescheduleUiState
import com.uptrendinvestments.clickmeapp.screens.clientmode.writereview.WriteReviewKey
import com.uptrendinvestments.clickmeapp.screens.login.AuthTopAppBar
import com.uptrendinvestments.clickmeapp.screens.login.asString
import com.uptrendinvestments.clickmeapp.ui.theme.Motion
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.contentMaxWidth
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.util.Locale

/** Test tag on the Message Expert button. */
const val BOOKING_SUMMARY_MESSAGE_TAG = "message_expert"

private val SkeletonCardHeight = 192.dp
private val BookAgainMinHeight = 56.dp
private val BookAgainIconSize = 24.dp

/**
 * Booking Summary (BOOK-08, D-13), opened from a Past card or a push. Port of iOS `BookingSummaryView`. Leave a
 * review pushes Write Review ([onLeaveReview], D-18); after a submit the summary reloads and thanks the user (D-19).
 * Message Expert opens the chat with the booking's expert (D-14). A pending reschedule shows the shared reschedule
 * card first (D-01). The summary reloads quietly on resume, so returning from Payment Methods shows fresh data
 * (D-02).
 *
 * The My Note card opens the note editor ([onOpenNote]) from every route into the summary (Phase 16.4, D-06; a
 * deliberate fix, iOS only wires it from My Bookings). The quiet reload shows a saved note on return (D-08).
 */
@Composable
fun BookingSummaryScreen(
    viewModel: BookingSummaryViewModel,
    onBack: () -> Unit,
    onBookAgain: (BookingKey) -> Unit,
    onOpenPaymentMethods: () -> Unit,
    onLeaveReview: (WriteReviewKey) -> Unit,
    onOpenNote: (ClientBookingNoteKey) -> Unit,
    modifier: Modifier = Modifier,
    chatEntry: ChatEntryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val rescheduleState by viewModel.rescheduleState.collectAsStateWithLifecycle()
    val entryState by chatEntry.state.collectAsStateWithLifecycle()
    val reviewThanks by viewModel.reviewThanks.collectAsStateWithLifecycle()
    val reviewedIds by viewModel.reviewedBookingIds.collectAsStateWithLifecycle()
    ChatEntryErrorDialog(error = entryState.error, onDismiss = chatEntry::dismissError)

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshQuietly() }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { viewModel.onScreenHidden() }
    DisposableEffect(viewModel) { onDispose { viewModel.onScreenHidden() } }
    // Nav3 may not deliver ON_RESUME to a covered entry (Phase 7 A10); a coalesced call is a no-op on first launch.
    LaunchedEffect(Unit) { viewModel.refreshQuietly() }

    BookingSummaryContent(
        state = state,
        rescheduleState = rescheduleState,
        onBack = onBack,
        onRetry = viewModel::retry,
        onBookAgain = onBookAgain,
        onAccept = viewModel::accept,
        onDecline = viewModel::decline,
        onDismissRescheduleDialog = viewModel::dismissRescheduleDialog,
        onOpenPaymentMethods = onOpenPaymentMethods,
        onRescheduleNoticeConsumed = viewModel::consumeRescheduleNotice,
        modifier = modifier,
        reviewedIds = reviewedIds,
        reviewThanks = reviewThanks,
        onReviewThanksConsumed = viewModel::consumeReviewThanks,
        onLeaveReview = { viewModel.leaveReviewKey()?.let(onLeaveReview) },
        onOpenNote = { viewModel.noteKey()?.let(onOpenNote) },
        messageBusy = entryState.busyPeerId != null,
        onMessage = {
            val details = (state as? BookingSummaryUiState.Loaded)?.details
            details?.expertId?.let {
                chatEntry.open(ChatPeer(it, details.expertName, details.expertAvatarUrl, isExpert = true))
            }
        },
    )
}

/** Stateless Booking Summary: loading, failed (with Retry) and loaded states under a "Session Summary" bar. */
@Suppress("LongParameterList")
@Composable
fun BookingSummaryContent(
    state: BookingSummaryUiState,
    rescheduleState: RescheduleUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onBookAgain: (BookingKey) -> Unit,
    onAccept: () -> Unit,
    onDecline: (reason: String) -> Unit,
    onDismissRescheduleDialog: () -> Unit,
    onOpenPaymentMethods: () -> Unit,
    onRescheduleNoticeConsumed: () -> Unit,
    modifier: Modifier = Modifier,
    reviewedIds: Set<String> = emptySet(),
    onLeaveReview: () -> Unit = {},
    onOpenNote: () -> Unit = {},
    reviewThanks: Boolean = false,
    onReviewThanksConsumed: () -> Unit = {},
    messageBusy: Boolean = false,
    onMessage: () -> Unit = {},
    zone: ZoneId = ZoneId.systemDefault(),
    timeFormat: java.text.DateFormat = rememberDeviceTimeFormat(),
    locale: Locale = LocalConfiguration.current.locales[0],
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val acceptedMessage = stringResource(R.string.reschedule_accepted)
    val declinedMessage = stringResource(R.string.reschedule_declined)
    val thanksMessage = stringResource(R.string.review_thanks_snackbar)

    LaunchedEffect(reviewThanks) {
        if (!reviewThanks) return@LaunchedEffect
        // Launched on the composition scope so consuming the flag does not cancel the Snackbar (D-19, Short).
        scope.launch {
            snackbarHostState.showSnackbar(thanksMessage, duration = SnackbarDuration.Short)
        }
        onReviewThanksConsumed()
    }

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
        topBar = { AuthTopAppBar(title = stringResource(R.string.booking_summary_title), onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        val bodyModifier = Modifier.fillMaxSize().padding(innerPadding)
        when (state) {
            BookingSummaryUiState.Loading -> LoadingBody(bodyModifier)
            is BookingSummaryUiState.Failed ->
                Column(
                    modifier = bodyModifier,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    EmptyState(
                        icon = Icons.Outlined.ErrorOutline,
                        heading = stringResource(R.string.booking_summary_load_error_title),
                        body = state.message.asString(),
                        action = EmptyStateAction(stringResource(R.string.section_retry), onRetry),
                    )
                }
            is BookingSummaryUiState.Loaded ->
                LoadedBody(
                    details = state.details,
                    rescheduleState = rescheduleState,
                    onBookAgain = onBookAgain,
                    onAccept = onAccept,
                    onDecline = onDecline,
                    review = state.review,
                    showLeaveReview = state.showLeaveReview(reviewedIds),
                    onLeaveReview = onLeaveReview,
                    onOpenNote = onOpenNote,
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
    SkeletonGroup(modifier = modifier.contentMaxWidth().padding(horizontal = Spacing.md, vertical = Spacing.md)) {
        Column {
            SkeletonBlock(
                modifier = Modifier.fillMaxWidth().heightIn(min = SkeletonCardHeight),
                shape = MaterialTheme.shapes.medium,
            )
            Spacer(Modifier.height(Spacing.lg))
            SkeletonBlock(
                modifier = Modifier.fillMaxWidth().heightIn(min = BookAgainMinHeight),
                shape = MaterialTheme.shapes.extraLarge,
            )
        }
    }
}

@Suppress("LongParameterList")
@Composable
private fun LoadedBody(
    details: BookingSummaryDetails,
    rescheduleState: RescheduleUiState,
    onBookAgain: (BookingKey) -> Unit,
    onAccept: () -> Unit,
    onDecline: (reason: String) -> Unit,
    review: SummaryReview,
    showLeaveReview: Boolean,
    onLeaveReview: () -> Unit,
    onOpenNote: () -> Unit,
    messageBusy: Boolean,
    onMessage: () -> Unit,
    modifier: Modifier,
    zone: ZoneId,
    timeFormat: java.text.DateFormat,
    locale: Locale,
) {
    // Keep the last proposal so the card can fade out after the reload cleared it (UI-SPEC Motion, 200 ms).
    var lastProposal by remember { mutableStateOf<RescheduleProposalUi?>(null) }
    if (details.reschedule != null && details.reschedule != lastProposal) lastProposal = details.reschedule
    val expertFallback = stringResource(R.string.explore_expert_name_fallback)

    Column(
        modifier =
            modifier
                .verticalScroll(rememberScrollState())
                .contentMaxWidth()
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
        BookingSummaryCard(details = details, zone = zone, timeFormat = timeFormat, locale = locale)
        (review as? SummaryReview.Submitted)?.let { BookingSummaryFeedback(it.rating, it.comment) }
        MyNoteCard(note = details.privateNote, onClick = onOpenNote, modifier = Modifier.padding(top = Spacing.lg))
        details.bookAgain?.let { seed ->
            PressScaleButton(
                onClick = { onBookAgain(seed.toBookingKey(expertFallback)) },
                modifier =
                    Modifier
                        .padding(top = Spacing.lg)
                        .fillMaxWidth()
                        .heightIn(min = BookAgainMinHeight)
                        .testTag(BOOKING_SUMMARY_BOOK_AGAIN_TAG),
            ) {
                Icon(Icons.Outlined.Replay, contentDescription = null, modifier = Modifier.size(BookAgainIconSize))
                Spacer(Modifier.width(Spacing.sm))
                Text(stringResource(R.string.booking_summary_book_again))
            }
        }
        // 200 ms fade and shrink once a submitted review hides the button (UI-SPEC Motion).
        AnimatedVisibility(
            visible = showLeaveReview,
            exit =
                fadeOut(tween(Motion.STRENGTH_METER_VISIBILITY_MS)) +
                    shrinkVertically(tween(Motion.STRENGTH_METER_VISIBILITY_MS)),
        ) {
            LeaveReviewButton(onClick = onLeaveReview, modifier = Modifier.padding(top = Spacing.md))
        }
        MessageButton(
            label = stringResource(R.string.chat_entry_message_expert),
            onClick = onMessage,
            busy = messageBusy,
            enabled = details.expertId != null,
            modifier =
                Modifier
                    .padding(top = Spacing.md)
                    .fillMaxWidth()
                    .testTag(BOOKING_SUMMARY_MESSAGE_TAG),
        )
    }
}
