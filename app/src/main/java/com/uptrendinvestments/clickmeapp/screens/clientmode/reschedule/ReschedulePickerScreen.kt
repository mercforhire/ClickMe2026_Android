package com.uptrendinvestments.clickmeapp.screens.clientmode.reschedule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.RescheduleViewer
import com.uptrendinvestments.clickmeapp.components.SectionHeader
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.canGoToPreviousMonth
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.formatShortDate
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.rememberDeviceTimeFormat
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.subviews.MakeABookingCalendarCard
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.subviews.MakeABookingExpertCard
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.subviews.MakeABookingNotesCard
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.subviews.MakeABookingTimeSlotsCard
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.formatSessionTimeRange
import com.uptrendinvestments.clickmeapp.screens.clientmode.reschedule.subviews.CurrentSessionBanner
import com.uptrendinvestments.clickmeapp.screens.clientmode.reschedule.subviews.RescheduleSubmitButton
import com.uptrendinvestments.clickmeapp.screens.login.AuthTopAppBar
import com.uptrendinvestments.clickmeapp.ui.theme.Spacing
import com.uptrendinvestments.clickmeapp.ui.theme.contentMaxWidth
import java.time.Instant
import java.time.LocalDate
import java.util.Locale

const val RESCHEDULE_DAY_TAG_PREFIX = "reschedule_day_"
const val RESCHEDULE_SLOT_TAG_PREFIX = "reschedule_slot_"
const val RESCHEDULE_PAID_NOTE_TAG = "reschedule_paid_window_note"
const val RESCHEDULE_MESSAGE_TAG = "reschedule_message"
const val RESCHEDULE_SUBMIT_TAG = "reschedule_submit"
const val RESCHEDULE_BANNER_TAG = "reschedule_current_banner"
const val RESCHEDULE_DISCLOSURE_CONTINUE_TAG = "reschedule_disclosure_continue"

/**
 * The reschedule picker shared by the client and the expert (Phase 16.2, UI-SPEC Screen 1). Port of iOS
 * `RescheduleView`. It is stateless: it renders a [ReschedulePickerUiState] and reports user actions, so each role's
 * route wires its own ViewModel and tests can drive every state without Hilt.
 *
 * Block order follows iOS: booking card, calendar, Current banner, slots, message, Submit. The app bar Back stays
 * available in every state except while a send is in flight. `authorization_expires_at` is deliberately never passed
 * to any card: it only affects the payment disclosure, never which day or slot can be picked.
 */
@Suppress("LongParameterList")
@Composable
fun ReschedulePickerScreen(
    state: ReschedulePickerUiState,
    onBack: () -> Unit,
    onRetryLoad: () -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onSelectSlot: (Instant) -> Unit,
    onRetrySlots: () -> Unit,
    onMessageChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onConfirmDisclosure: () -> Unit,
    onDismissDialog: () -> Unit,
    onAddPaymentMethod: () -> Unit,
    modifier: Modifier = Modifier,
    timeFormat: java.text.DateFormat = rememberDeviceTimeFormat(),
    locale: Locale = LocalConfiguration.current.locales[0],
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            AuthTopAppBar(
                title = stringResource(R.string.reschedule_start_title),
                onBack = onBack,
                backEnabled = !state.isSubmitting,
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            val booking = state.booking
            if (booking != null) {
                LoadedContent(
                    state = state,
                    booking = booking,
                    onPreviousMonth = onPreviousMonth,
                    onNextMonth = onNextMonth,
                    onSelectDate = onSelectDate,
                    onSelectSlot = onSelectSlot,
                    onRetrySlots = onRetrySlots,
                    onMessageChange = onMessageChange,
                    onSubmit = onSubmit,
                    timeFormat = timeFormat,
                    locale = locale,
                )
            }
        }
    }
}

@Suppress("LongParameterList")
@Composable
private fun LoadedContent(
    state: ReschedulePickerUiState,
    booking: PickerBooking,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onSelectSlot: (Instant) -> Unit,
    onRetrySlots: () -> Unit,
    onMessageChange: (String) -> Unit,
    onSubmit: () -> Unit,
    timeFormat: java.text.DateFormat,
    locale: Locale,
) {
    val expertViewer = booking.viewer == RescheduleViewer.Expert
    val name =
        booking.counterpartName
            ?: stringResource(
                if (expertViewer) R.string.reschedule_start_client_fallback else R.string.explore_expert_name_fallback,
            )
    val title =
        booking.subtitle
            ?: if (expertViewer) stringResource(R.string.reschedule_start_session_fallback) else ""
    val detail =
        stringResource(
            R.string.reschedule_start_booking_detail,
            formatShortDate(booking.currentStart.atZone(state.zone).toLocalDate(), locale),
            formatSessionTimeRange(booking.currentStart, booking.currentEnd, state.zone, timeFormat),
        )
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState()),
    ) {
        Column(
            modifier =
                Modifier
                    .contentMaxWidth()
                    .padding(PaddingValues(start = Spacing.md, end = Spacing.md, top = Spacing.lg)),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            MakeABookingExpertCard(
                name = name,
                title = title,
                imageUrl = booking.avatarUrl,
                detail = detail,
            )
            MakeABookingCalendarCard(
                displayedMonth = state.displayedMonth,
                today = state.today,
                selectedDate = state.selectedDate,
                openDays = state.openDays,
                canGoToPreviousMonth = canGoToPreviousMonth(state.displayedMonth, state.today),
                onPreviousMonth = onPreviousMonth,
                onNextMonth = onNextMonth,
                onSelectDate = onSelectDate,
                enabled = !state.isSubmitting,
                tagPrefix = RESCHEDULE_DAY_TAG_PREFIX,
            )
            CurrentSessionBanner(booking.currentStart, state.zone, timeFormat, locale)
            MakeABookingTimeSlotsCard(
                selectedDate = state.selectedDate,
                availability = state.availability,
                now = state.now,
                selectedSlotStart = state.selectedSlotStart,
                expertTimezone = booking.slotsTimezone,
                onSelectSlot = onSelectSlot,
                onRetry = onRetrySlots,
                enabled = !state.isSubmitting,
                selectedDateLoaded = state.selectedDateLoaded,
                zone = state.zone,
                timeFormat = timeFormat,
                latestSelectableStart = state.latestSelectableStart,
                paidWindowNote = stringResource(R.string.reschedule_start_paid_window_note),
                paidWindowNoteTag = RESCHEDULE_PAID_NOTE_TAG,
                hidePastSlots = true,
                currentSlotStart = booking.currentStart,
                slotTagPrefix = RESCHEDULE_SLOT_TAG_PREFIX,
            )
            Column {
                SectionHeader(title = stringResource(R.string.reschedule_start_message_header))
                Spacer(Modifier.padding(top = Spacing.sm))
                MakeABookingNotesCard(
                    notes = state.message,
                    onNotesChange = onMessageChange,
                    readOnly = state.isSubmitting,
                    hint =
                        stringResource(
                            if (expertViewer) {
                                R.string.reschedule_start_message_hint_expert
                            } else {
                                R.string.reschedule_start_message_hint_client
                            },
                        ),
                    tag = RESCHEDULE_MESSAGE_TAG,
                )
            }
            RescheduleSubmitButton(
                enabled = state.selectedSlotStart != null,
                submitting = state.isSubmitting,
                onClick = onSubmit,
            )
            Spacer(Modifier.navigationBarsPadding().padding(bottom = Spacing.xl))
        }
    }
}
