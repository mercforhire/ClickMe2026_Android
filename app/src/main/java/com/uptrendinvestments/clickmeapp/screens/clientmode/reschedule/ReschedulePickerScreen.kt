package com.uptrendinvestments.clickmeapp.screens.clientmode.reschedule

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.EmptyState
import com.uptrendinvestments.clickmeapp.components.EmptyStateAction
import com.uptrendinvestments.clickmeapp.components.PaymentMethodDialog
import com.uptrendinvestments.clickmeapp.components.RescheduleViewer
import com.uptrendinvestments.clickmeapp.components.SectionHeader
import com.uptrendinvestments.clickmeapp.components.SkeletonBlock
import com.uptrendinvestments.clickmeapp.components.SkeletonGroup
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.canGoToPreviousMonth
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.formatShortDate
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.rememberDeviceTimeFormat
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.subviews.MakeABookingCalendarCard
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.subviews.MakeABookingExpertCard
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.subviews.MakeABookingNotesCard
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.subviews.MakeABookingTimeSlotsCard
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.formatSessionTimeRange
import com.uptrendinvestments.clickmeapp.screens.clientmode.reschedule.subviews.CurrentSessionBanner
import com.uptrendinvestments.clickmeapp.screens.clientmode.reschedule.subviews.RescheduleDisclosureDialog
import com.uptrendinvestments.clickmeapp.screens.clientmode.reschedule.subviews.RescheduleSubmitButton
import com.uptrendinvestments.clickmeapp.screens.login.AuthErrorDialog
import com.uptrendinvestments.clickmeapp.screens.login.AuthTopAppBar
import com.uptrendinvestments.clickmeapp.screens.login.asString
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

/** Booking card, calendar, slots card, message field and button, as the loading skeleton (UI-SPEC E1). */
private val SkeletonHeights = listOf(88.dp, 360.dp, 168.dp, 144.dp, 56.dp)

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
            when (val load = state.load) {
                PickerLoad.Loading -> LoadingContent()
                is PickerLoad.Failed -> FailedContent(message = load.message.asString(), onRetry = onRetryLoad)
                is PickerLoad.Loaded ->
                    LoadedContent(
                        state = state,
                        booking = load.booking,
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

    // System Back is ignored while a send (including the card retry) is in flight, like the arrow (UI-SPEC Navigation).
    BackHandler(enabled = state.isSubmitting) {}

    when (val dialog = state.dialog) {
        null -> Unit
        is ReschedulePickerDialog.Disclosure ->
            RescheduleDisclosureDialog(
                chargesSoon = dialog.chargesSoon,
                onContinue = onConfirmDisclosure,
                onDismiss = onDismissDialog,
            )
        ReschedulePickerDialog.PaymentMethodNeeded ->
            PaymentMethodDialog(
                message = stringResource(R.string.reschedule_start_payment_needed_message),
                onDismiss = onDismissDialog,
                onAddPaymentMethod = onAddPaymentMethod,
            )
        ReschedulePickerDialog.CardUnusable ->
            PaymentMethodDialog(
                message = stringResource(R.string.reschedule_card_unusable_message),
                onDismiss = onDismissDialog,
                onAddPaymentMethod = onAddPaymentMethod,
            )
        is ReschedulePickerDialog.Error ->
            AuthErrorDialog(
                message = dialog.message.asString(),
                onDismiss = onDismissDialog,
                title = stringResource(R.string.reschedule_start_error_title),
            )
    }
}

/**
 * The skeleton shown until the booking has loaded: it copies the booking card, calendar, slots card, message field and
 * button, so nothing renders half-loaded (UI-SPEC E1/E2 loading). The app bar stays outside it.
 */
@Composable
private fun LoadingContent() {
    SkeletonGroup(
        modifier =
            Modifier
                .contentMaxWidth()
                .padding(PaddingValues(start = Spacing.md, end = Spacing.md, top = Spacing.lg))
                .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            SkeletonHeights.forEach { height ->
                SkeletonBlock(Modifier.fillMaxWidth().height(height), MaterialTheme.shapes.medium)
            }
        }
    }
}

/** "Couldn't load booking" with the server or network reason and a Retry that re-runs the load (UI-SPEC E1 error). */
@Composable
private fun FailedContent(
    message: String,
    onRetry: () -> Unit,
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        EmptyState(
            icon = Icons.Outlined.ErrorOutline,
            heading = stringResource(R.string.reschedule_start_load_error_title),
            body = message,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            action = EmptyStateAction(stringResource(R.string.section_retry), onRetry),
        )
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
