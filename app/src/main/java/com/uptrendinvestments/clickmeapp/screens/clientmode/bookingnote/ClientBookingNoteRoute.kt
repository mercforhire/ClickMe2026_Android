package com.uptrendinvestments.clickmeapp.screens.clientmode.bookingnote

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.screens.bookingnote.BookingNoteArgs
import com.uptrendinvestments.clickmeapp.screens.bookingnote.BookingNoteHeader
import com.uptrendinvestments.clickmeapp.screens.bookingnote.BookingNoteScreen
import com.uptrendinvestments.clickmeapp.screens.bookingnote.BookingNoteViewModel
import com.uptrendinvestments.clickmeapp.screens.bookingnote.NoteRole

/**
 * The client's private note route: binds the shared editor to the client role and to [key]. The role is fixed here,
 * never chosen by the screen, so a client note can only touch the client endpoints (T-16.4-12). The shell registers
 * this through `clientBookingNoteEntry`.
 */
@Composable
fun ClientBookingNoteRoute(
    key: ClientBookingNoteKey,
    onBack: () -> Unit,
) {
    val viewModel =
        hiltViewModel<BookingNoteViewModel, BookingNoteViewModel.Factory>(
            creationCallback = { factory -> factory.create(BookingNoteArgs(NoteRole.Client, key.bookingId)) },
        )
    val header =
        BookingNoteHeader(
            name = key.expertName?.takeIf(String::isNotBlank) ?: stringResource(R.string.explore_expert_name_fallback),
            topic = key.topic,
            startMillis = key.startMillis,
            endMillis = key.endMillis,
        )
    BookingNoteScreen(viewModel = viewModel, header = header, onBack = onBack)
}
