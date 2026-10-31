package com.uptrendinvestments.clickmeapp.screens.expertmode.reschedule

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uptrendinvestments.clickmeapp.screens.clientmode.reschedule.ReschedulePickerScreen

/**
 * Route for the expert Reschedule picker (PARITY-AUDIT #11): collects the ViewModel state and hands it to the shared
 * [ReschedulePickerScreen].
 *
 * A sent request (the ViewModel has already posted the hand-off to the opener's slot, D-10) and a booking that can no
 * longer be moved both pop once to the screen that opened the picker. Resuming re-reads the clock so a slot that
 * started meanwhile is dropped; it never sends (D-08). The expert state never carries a payment dialog or the
 * disclosure (RESCHEDULE-PAYMENT-CONVERSION.md section 6), so the Payment Methods callback only closes a dialog.
 */
@Composable
fun ExpertRescheduleScreen(
    viewModel: ExpertRescheduleViewModel,
    onBack: () -> Unit,
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
        onAddPaymentMethod = viewModel::dismissDialog,
        modifier = modifier,
    )
}
