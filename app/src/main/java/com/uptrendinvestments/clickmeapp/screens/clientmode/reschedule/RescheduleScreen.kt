package com.uptrendinvestments.clickmeapp.screens.clientmode.reschedule

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * The client Reschedule picker route (Phase 16.2): binds [ReschedulePickerScreen] to a [RescheduleViewModel].
 *
 * - D-09: the success hand-off is already posted to the opener's slot by the controller before `sent` is set, so
 *   popping here never loses it. [onBack] is also the pop for a booking that can no longer be rescheduled (`leave`).
 * - D-08: returning from Payment Methods only re-reads the clock ([RescheduleViewModel.refreshClock]); nothing is
 *   sent until the user taps Submit again.
 * - Back is blocked in the content while a send is in flight.
 *
 * [onOpenPaymentMethods] pushes Payment Methods on top of the picker; the saved-card dialog closes first so the
 * picker keeps its date, slot and message when the user comes back.
 */
@Composable
fun RescheduleScreen(
    viewModel: RescheduleViewModel,
    onBack: () -> Unit,
    onOpenPaymentMethods: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshClock() }

    LaunchedEffect(state.sent) {
        if (state.sent) {
            viewModel.consumeSent()
            onBack()
        }
    }
    LaunchedEffect(state.leave) {
        if (state.leave) {
            viewModel.consumeLeave()
            onBack()
        }
    }

    ReschedulePickerScreen(
        state = state,
        onBack = onBack,
        onRetryLoad = viewModel::retryLoad,
        onPreviousMonth = viewModel::previousMonth,
        onNextMonth = viewModel::nextMonth,
        onSelectDate = viewModel::selectDate,
        onSelectSlot = viewModel::selectSlot,
        onRetrySlots = viewModel::retrySlots,
        onMessageChange = viewModel::updateMessage,
        onSubmit = viewModel::submit,
        onConfirmDisclosure = viewModel::confirmDisclosure,
        onDismissDialog = viewModel::dismissDialog,
        onAddPaymentMethod = {
            viewModel.dismissDialog()
            onOpenPaymentMethods()
        },
        modifier = modifier,
    )
}
