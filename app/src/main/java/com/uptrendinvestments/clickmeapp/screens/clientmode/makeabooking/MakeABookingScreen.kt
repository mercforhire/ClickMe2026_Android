package com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.SectionHeader
import com.uptrendinvestments.clickmeapp.model.enums.MeetingType
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.subviews.MakeABookingBookButton
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.subviews.MakeABookingCalendarCard
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.subviews.MakeABookingExpertCard
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.subviews.MakeABookingMeetingTypeCard
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.subviews.MakeABookingNotesCard
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.subviews.MakeABookingSummarySection
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.subviews.MakeABookingTimeSlotsCard
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.subviews.MakeABookingTopicPicker
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.subviews.MakeABookingTopicSheet
import com.uptrendinvestments.clickmeapp.screens.login.AuthErrorDialog
import com.uptrendinvestments.clickmeapp.screens.login.AuthTopAppBar
import com.uptrendinvestments.clickmeapp.screens.login.asString
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import java.time.Instant
import java.time.LocalDate

const val MAKE_A_BOOKING_SCROLL_TAG = "make_a_booking_scroll"

/**
 * Make a Booking, free path. Port of iOS `Make a Booking/MakeABookingView.swift`; it is the body of the
 * unchanged [BookingKey] route (Phase 5 D-04). The bottom bar is hidden by the route metadata, not by this screen.
 *
 * A completed booking is consumed once before [onBooked] runs (D-12), so a recomposition can never navigate to
 * Request Sent twice. The shell maps [onBooked] to `replaceTop(RequestSentKey(id))`.
 */
@Composable
fun MakeABookingScreen(
    viewModel: MakeABookingViewModel,
    onBack: () -> Unit,
    onBooked: (bookingId: String) -> Unit,
    modifier: Modifier = Modifier,
    timeFormat: java.text.DateFormat = rememberDeviceTimeFormat(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // WR-03: "today" and the viewer's zone are re-derived whenever the screen comes back, e.g. after midnight or
    // after the user changed the time zone setting.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshClock() }
    LaunchedEffect(state.completedBookingId) {
        val id = state.completedBookingId ?: return@LaunchedEffect
        viewModel.consumeCompletedBooking()
        onBooked(id)
    }
    MakeABookingContent(
        state = state,
        onBack = onBack,
        onRetryTopics = viewModel::retryTopics,
        onSelectTopic = viewModel::selectTopic,
        onPreviousMonth = viewModel::goToPreviousMonth,
        onNextMonth = viewModel::goToNextMonth,
        onSelectDate = viewModel::selectDate,
        onSelectSlot = viewModel::selectSlot,
        onRetryAvailability = viewModel::retryAvailability,
        onSelectMeetingType = viewModel::selectMeetingType,
        onNotesChange = viewModel::onNotesChange,
        onBook = viewModel::book,
        onDismissError = viewModel::dismissError,
        modifier = modifier,
        timeFormat = timeFormat,
    )
}

/**
 * Stateless Make a Booking content: the eight sections in a fixed order inside one scrolling column (a Column of
 * Rows calendar, so no nested lazy layout). While a booking request is in flight both the app-bar Back and system
 * Back are blocked, because cancelling the request scope could hide a booking that already exists, and the topic,
 * calendar, slots, meeting type and notes are locked, so the Summary always shows what is being booked.
 */
@Composable
fun MakeABookingContent(
    state: MakeABookingUiState,
    onBack: () -> Unit,
    onRetryTopics: () -> Unit,
    onSelectTopic: (String) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onSelectSlot: (Instant) -> Unit,
    onRetryAvailability: () -> Unit,
    onSelectMeetingType: (MeetingType) -> Unit,
    onNotesChange: (String) -> Unit,
    onBook: () -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier,
    timeFormat: java.text.DateFormat = rememberDeviceTimeFormat(),
) {
    var showTopicSheet by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    BackHandler(enabled = state.isSubmitting) {}
    val editable = !state.isSubmitting
    // Only a user drag dismisses the keyboard. `isScrollInProgress` is also true for programmatic scrolls, and
    // focusing the notes field starts two of them (its own bring-into-view, and keeping the focused child visible
    // when `imePadding` shrinks the viewport), which would clear focus as soon as the keyboard opens.
    val isDragged by scrollState.interactionSource.collectIsDraggedAsState()
    LaunchedEffect(isDragged) {
        if (isDragged) focusManager.clearFocus()
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            AuthTopAppBar(
                title = stringResource(R.string.make_a_booking_title),
                onBack = onBack,
                backEnabled = !state.isSubmitting,
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding()
                    .pointerInput(Unit) { detectTapGestures(onTap = { focusManager.clearFocus() }) }
                    .verticalScroll(scrollState)
                    .testTag(MAKE_A_BOOKING_SCROLL_TAG)
                    .padding(PaddingValues(start = Spacing.md, end = Spacing.md, top = Spacing.lg)),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            Section(R.string.make_a_booking_section_expert) {
                MakeABookingExpertCard(state.expertName, state.expertTitle, state.expertImageUrl)
            }
            Section(R.string.make_a_booking_section_topic) {
                MakeABookingTopicPicker(
                    topics = state.topics,
                    selectedTopic = state.selectedTopic,
                    onOpen = { showTopicSheet = true },
                    onRetry = onRetryTopics,
                    enabled = editable,
                )
            }
            Section(R.string.make_a_booking_section_time) {
                MakeABookingCalendarCard(
                    displayedMonth = state.displayedMonth,
                    today = state.today,
                    selectedDate = state.selectedDate,
                    openDays = state.openDays,
                    canGoToPreviousMonth = state.canGoToPreviousMonth,
                    onPreviousMonth = onPreviousMonth,
                    onNextMonth = onNextMonth,
                    onSelectDate = onSelectDate,
                    enabled = editable,
                )
            }
            MakeABookingTimeSlotsCard(
                selectedDate = state.selectedDate,
                availability = state.availability,
                now = state.now,
                selectedSlotStart = state.selectedSlotStart,
                expertTimezone = state.expertTimezone,
                onSelectSlot = onSelectSlot,
                onRetry = onRetryAvailability,
                enabled = editable,
                selectedDateLoaded = state.isSelectedDateLoaded,
                zone = state.zone,
                timeFormat = timeFormat,
            )
            Section(R.string.make_a_booking_section_meeting_type) {
                MakeABookingMeetingTypeCard(
                    selected = state.meetingType,
                    onSelect = onSelectMeetingType,
                    enabled = editable,
                )
            }
            Section(R.string.make_a_booking_section_message) {
                MakeABookingNotesCard(
                    notes = state.notes,
                    onNotesChange = onNotesChange,
                    readOnly = !editable,
                )
            }
            Section(R.string.make_a_booking_section_summary) {
                MakeABookingSummarySection(
                    topic = state.selectedTopic,
                    selectedDate = state.selectedDate,
                    selectedSlotStart = state.selectedSlotStart,
                    meetingType = state.meetingType,
                    zone = state.zone,
                    timeFormat = timeFormat,
                )
            }
            MakeABookingBookButton(
                disabledReason = state.bookDisabledReason,
                isSubmitting = state.isSubmitting,
                onClick = onBook,
            )
            Spacer(Modifier.navigationBarsPadding().padding(bottom = Spacing.xl))
        }
    }

    if (showTopicSheet && editable && state.topicList.isNotEmpty()) {
        MakeABookingTopicSheet(
            topics = state.topicList,
            selectedTopicId = state.selectedTopicId,
            onSelect = {
                onSelectTopic(it)
                showTopicSheet = false
            },
            onDismiss = { showTopicSheet = false },
        )
    }

    state.dialogError?.let { error ->
        AuthErrorDialog(
            message = error.asString(),
            onDismiss = onDismissError,
            title = stringResource(R.string.make_a_booking_error_title),
        )
    }
}

/** A heading followed by `Spacing.sm` and its card. */
@Composable
private fun Section(
    @StringRes titleRes: Int,
    content: @Composable () -> Unit,
) {
    Column {
        SectionHeader(title = stringResource(titleRes))
        Spacer(Modifier.padding(top = Spacing.sm))
        content()
    }
}
