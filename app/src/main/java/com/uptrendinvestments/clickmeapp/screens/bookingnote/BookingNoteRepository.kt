package com.uptrendinvestments.clickmeapp.screens.bookingnote

import com.uptrendinvestments.clickmeapp.model.requests.BookingNoteRequest
import com.uptrendinvestments.clickmeapp.network.ClickMeApi
import com.uptrendinvestments.clickmeapp.network.apiCall
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Whose private note this is. The client's and the expert's notes are independent columns behind different
 * endpoints: the role picks the endpoint and nothing else does (no role or owner id travels in a request).
 */
enum class NoteRole { Client, Expert }

/**
 * The private note on a booking (`/client/bookings/{id}/note` for [NoteRole.Client], `/bookings/{id}/note` for
 * [NoteRole.Expert]). Ports iOS `getClientBookingNote` / `createClientBookingNote` and the expert pair.
 *
 * Stateless. Throws ApiException only. The note text is never logged. The server answers a uniform 404 for a
 * missing booking and for one that is not the caller's: a GET 404 means "no note yet" to the caller, a POST 404
 * does not.
 */
@Singleton
class BookingNoteRepository
    @Inject
    constructor(
        private val api: ClickMeApi,
    ) {
        /** The saved note, or null when there is none. */
        suspend fun get(
            role: NoteRole,
            bookingId: String,
        ): String? =
            when (role) {
                NoteRole.Client -> apiCall { api.getClientBookingNote(bookingId) }
                NoteRole.Expert -> apiCall { api.getBookingNote(bookingId) }
            }.data.note

        /** Saves [note] (an empty string clears it) and returns the server's stored note, null when empty. */
        suspend fun save(
            role: NoteRole,
            bookingId: String,
            note: String,
        ): String? {
            val body = BookingNoteRequest(note)
            return when (role) {
                NoteRole.Client -> apiCall { api.saveClientBookingNote(bookingId, body) }
                NoteRole.Expert -> apiCall { api.saveBookingNote(bookingId, body) }
            }.data.note
        }
    }
