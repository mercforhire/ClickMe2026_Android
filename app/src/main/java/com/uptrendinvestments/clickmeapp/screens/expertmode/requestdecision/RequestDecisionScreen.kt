package com.uptrendinvestments.clickmeapp.screens.expertmode.requestdecision

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.EmptyState
import com.uptrendinvestments.clickmeapp.components.EmptyStateAction
import com.uptrendinvestments.clickmeapp.components.SkeletonBlock
import com.uptrendinvestments.clickmeapp.components.SkeletonGroup
import com.uptrendinvestments.clickmeapp.components.chat.ChatEntryErrorDialog
import com.uptrendinvestments.clickmeapp.components.chat.MessageButton
import com.uptrendinvestments.clickmeapp.screens.chat.ChatEntryViewModel
import com.uptrendinvestments.clickmeapp.screens.chat.ChatPeer
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.rememberDeviceTimeFormat
import com.uptrendinvestments.clickmeapp.screens.expertmode.clientDisplayName
import com.uptrendinvestments.clickmeapp.screens.expertmode.confirmed.BookingConfirmedKey
import com.uptrendinvestments.clickmeapp.screens.expertmode.requestdecision.subviews.AcceptRequestSheet
import com.uptrendinvestments.clickmeapp.screens.expertmode.requestdecision.subviews.DeclineRequestSheet
import com.uptrendinvestments.clickmeapp.screens.expertmode.requestdecision.subviews.REQUEST_MESSAGE_TAG
import com.uptrendinvestments.clickmeapp.screens.expertmode.requestdecision.subviews.RequestActionFooter
import com.uptrendinvestments.clickmeapp.screens.expertmode.requestdecision.subviews.RequestClientHeader
import com.uptrendinvestments.clickmeapp.screens.expertmode.requestdecision.subviews.RequestDetailsCard
import com.uptrendinvestments.clickmeapp.screens.expertmode.requestdecision.subviews.RequestEarningsSection
import com.uptrendinvestments.clickmeapp.screens.expertmode.requestdecision.subviews.RequestExpiredBanner
import com.uptrendinvestments.clickmeapp.screens.expertmode.requestdecision.subviews.RequestMessageCard
import com.uptrendinvestments.clickmeapp.screens.login.AuthErrorDialog
import com.uptrendinvestments.clickmeapp.screens.login.AuthTopAppBar
import com.uptrendinvestments.clickmeapp.screens.login.asString
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import java.time.ZoneId
import java.util.Locale

/** Test tag on the scrolling content column of a loaded request. */
const val REQUEST_CONTENT_TAG = "request_content"

private val HeaderSkeletonAvatar = 72.dp
private val NameSkeletonWidth = 160.dp
private val NameSkeletonHeight = 24.dp
private val SubSkeletonWidth = 96.dp
private val SubSkeletonHeight = 16.dp
private val DetailsSkeletonHeight = 160.dp
private val MessageSkeletonHeight = 96.dp
private val EarningsSkeletonHeight = 128.dp

/**
 * Request Decision (EXP-06): the client, schedule, message and earnings of one pending request, with a pinned
 * Message / Decline / Accept footer and the two decision sheets. [onAccepted] replaces this screen with Booking
 * Confirmed (D-04). Message opens the chat with the requesting client (D-14); an expired request, which has no footer,
 * shows it at the bottom of the content. There is no Report or Block here (D-11).
 */
@Composable
fun RequestDecisionScreen(
    viewModel: RequestDecisionViewModel,
    onBack: () -> Unit,
    onAccepted: (BookingConfirmedKey) -> Unit,
    onDeclined: () -> Unit,
    onExitAfterProcessed: () -> Unit,
    modifier: Modifier = Modifier,
    chatEntry: ChatEntryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val entryState by chatEntry.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.event) {
        when (val event = state.event) {
            is RequestDecisionEvent.Accepted -> onAccepted(event.confirmed)
            RequestDecisionEvent.Declined -> onDeclined()
            RequestDecisionEvent.ExitAfterProcessed -> onExitAfterProcessed()
            null -> return@LaunchedEffect
        }
        viewModel.consumeEvent()
    }
    RequestDecisionContent(
        state = state,
        onBack = onBack,
        onRetry = viewModel::retry,
        onAccept = viewModel::accept,
        onDecline = viewModel::decline,
        onDismissDialog = viewModel::dismissDialog,
        modifier = modifier,
        messageBusy = entryState.busyPeerId != null,
        onMessage = { details ->
            chatEntry.open(ChatPeer(details.clientId, details.clientName, details.clientAvatarUrl, isExpert = false))
        },
    )
    ChatEntryErrorDialog(error = entryState.error, onDismiss = chatEntry::dismissError)
}

/**
 * Stateless Request Decision screen. [zone] and [timeFormat] are parameters so tests are deterministic. The sheets'
 * open flags and typed text are kept here with `rememberSaveable`, so a failed call or a rotation keeps the input.
 */
@Composable
@Suppress("LongParameterList", "LongMethod")
fun RequestDecisionContent(
    state: RequestDecisionUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onAccept: (String) -> Unit,
    onDecline: (DeclineReason, String) -> Unit,
    onDismissDialog: () -> Unit,
    modifier: Modifier = Modifier,
    zone: ZoneId = ZoneId.systemDefault(),
    timeFormat: java.text.DateFormat = rememberDeviceTimeFormat(),
    locale: Locale = LocalConfiguration.current.locales[0],
    messageBusy: Boolean = false,
    onMessage: (RequestDecisionDetails) -> Unit = {},
) {
    var acceptOpen by rememberSaveable { mutableStateOf(false) }
    var acceptText by rememberSaveable { mutableStateOf("") }
    var declineOpen by rememberSaveable { mutableStateOf(false) }
    var declineReasonName by rememberSaveable { mutableStateOf<String?>(null) }
    var declineNote by rememberSaveable { mutableStateOf("") }
    val declineReason = DeclineReason.entries.firstOrNull { it.name == declineReasonName }
    val submitting = state.submitting != DecisionSubmitting.None
    val canDecide = state.load is RequestDecisionLoad.Loaded && !state.isExpired
    // A finished decision closes its sheet; the screen is replaced or popped right after.
    LaunchedEffect(state.event) {
        if (state.event != null) {
            acceptOpen = false
            declineOpen = false
        }
    }
    // The call keeps running in the ViewModel, so Back must not leave while it is in flight.
    BackHandler(enabled = submitting) {}

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            AuthTopAppBar(
                title = stringResource(R.string.request_decision_title),
                onBack = onBack,
                backEnabled = !submitting,
            )
        },
        bottomBar = {
            val loadedDetails = (state.load as? RequestDecisionLoad.Loaded)?.details
            if (canDecide && loadedDetails != null) {
                RequestActionFooter(
                    enabled = !submitting,
                    onDecline = { declineOpen = true },
                    onAccept = { acceptOpen = true },
                    clientName = loadedDetails.clientName,
                    messageBusy = messageBusy,
                    onMessage = { onMessage(loadedDetails) },
                )
            }
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (val load = state.load) {
                RequestDecisionLoad.Loading -> LoadingBody()
                is RequestDecisionLoad.Failed -> FailedBody(message = load.message.asString(), onRetry = onRetry)
                is RequestDecisionLoad.Loaded ->
                    LoadedBody(
                        details = load.details,
                        isExpired = state.isExpired,
                        zone = zone,
                        timeFormat = timeFormat,
                        locale = locale,
                        messageBusy = messageBusy,
                        onMessage = { onMessage(load.details) },
                    )
            }
        }
    }

    if (acceptOpen && canDecide) {
        AcceptRequestSheet(
            message = acceptText,
            onMessageChange = { acceptText = it },
            isSubmitting = state.submitting == DecisionSubmitting.Accepting,
            onDismiss = { acceptOpen = false },
            onConfirm = { onAccept(acceptText) },
        )
    }

    val loaded = state.load as? RequestDecisionLoad.Loaded
    if (declineOpen && canDecide && loaded != null) {
        DeclineRequestSheet(
            details = loaded.details,
            selected = declineReason,
            onSelect = { declineReasonName = it.name },
            note = declineNote,
            onNoteChange = { declineNote = it },
            isSubmitting = state.submitting == DecisionSubmitting.Declining,
            onDismiss = { declineOpen = false },
            onConfirm = { declineReason?.let { onDecline(it, declineNote) } },
            zone = zone,
            timeFormat = timeFormat,
            locale = locale,
        )
    }

    // Drawn last so it sits above an open sheet; a non-exit error leaves the sheet and its inputs as they were.
    state.dialog?.let { dialog ->
        AuthErrorDialog(
            message = dialog.message.asString(),
            onDismiss = onDismissDialog,
            title = stringResource(R.string.request_decision_error_title),
        )
    }
}

@Composable
private fun LoadedBody(
    details: RequestDecisionDetails,
    isExpired: Boolean,
    zone: ZoneId,
    timeFormat: java.text.DateFormat,
    locale: Locale,
    messageBusy: Boolean,
    onMessage: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .testTag(REQUEST_CONTENT_TAG)
                .verticalScroll(rememberScrollState())
                .padding(start = Spacing.md, end = Spacing.md, top = Spacing.md, bottom = Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        if (isExpired) RequestExpiredBanner()
        RequestClientHeader(name = details.clientName, avatarUrl = details.clientAvatarUrl)
        RequestDetailsCard(
            start = details.start,
            end = details.end,
            topic = details.topic,
            meetingType = details.meetingType,
            zone = zone,
            timeFormat = timeFormat,
            locale = locale,
        )
        details.clientMessage?.let { RequestMessageCard(message = it) }
        RequestEarningsSection(earnings = details.earnings)
        // An expired request has no footer, so Message sits at the end of the content instead.
        if (isExpired) {
            MessageButton(
                label = stringResource(R.string.chat_entry_message_name, clientDisplayName(details.clientName)),
                onClick = onMessage,
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.lg).testTag(REQUEST_MESSAGE_TAG),
                busy = messageBusy,
            )
        }
    }
}

@Composable
private fun LoadingBody() {
    SkeletonGroup(modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }) {
        Column(
            modifier = Modifier.padding(start = Spacing.md, end = Spacing.md, top = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                SkeletonBlock(Modifier.size(HeaderSkeletonAvatar), shape = CircleShape)
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    SkeletonBlock(Modifier.width(NameSkeletonWidth).height(NameSkeletonHeight))
                    SkeletonBlock(Modifier.width(SubSkeletonWidth).height(SubSkeletonHeight))
                }
            }
            SkeletonBlock(Modifier.fillMaxWidth().height(DetailsSkeletonHeight))
            SkeletonBlock(Modifier.fillMaxWidth().height(MessageSkeletonHeight))
            SkeletonBlock(Modifier.fillMaxWidth().height(EarningsSkeletonHeight))
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
            heading = stringResource(R.string.request_decision_load_error),
            body = message,
            action = EmptyStateAction(stringResource(R.string.expert_retry), onRetry),
        )
    }
}
