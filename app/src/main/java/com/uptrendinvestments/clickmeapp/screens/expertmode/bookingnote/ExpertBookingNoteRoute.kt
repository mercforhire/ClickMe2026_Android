package com.uptrendinvestments.clickmeapp.screens.expertmode.bookingnote

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.uptrendinvestments.clickmeapp.screens.bookingnote.BookingNoteArgs
import com.uptrendinvestments.clickmeapp.screens.bookingnote.BookingNoteHeader
import com.uptrendinvestments.clickmeapp.screens.bookingnote.BookingNoteScreen
import com.uptrendinvestments.clickmeapp.screens.bookingnote.BookingNoteViewModel
import com.uptrendinvestments.clickmeapp.screens.bookingnote.NoteRole
import com.uptrendinvestments.clickmeapp.screens.expertmode.clientDisplayName

/**
 * The expert's private note route: binds the shared editor to the expert role and to [key]. The role is fixed here,
 * never chosen by the screen, so an expert note can only touch the expert endpoints (T-16.4-12). The expert shell
 * registers this through `expertBookingNoteEntry`.
 */
@Composable
fun ExpertBookingNoteRoute(
    key: ExpertBookingNoteKey,
    onBack: () -> Unit,
) {
    val viewModel =
        hiltViewModel<BookingNoteViewModel, BookingNoteViewModel.Factory>(
            creationCallback = { factory -> factory.create(BookingNoteArgs(NoteRole.Expert, key.bookingId)) },
        )
    val header =
        BookingNoteHeader(
            name = clientDisplayName(key.clientName),
            topic = key.topic,
            startMillis = key.startMillis,
            endMillis = key.endMillis,
        )
    BookingNoteScreen(viewModel = viewModel, header = header, onBack = onBack)
}
