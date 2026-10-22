package com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking

import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.model.enums.ErrorCode
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.screens.login.FieldError

/** What the booking screen does with a failed `POST /bookings/request` (the free path). */
sealed interface BookingFailure {
    /** Nothing to show here: the global suspended-account dialog owns ACCOUNT_SUSPENDED (Phase 2 D-08). */
    data object Silent : BookingFailure

    /**
     * Show [message] in the error dialog.
     *
     * @property clearSlotAndReload D-09: drop the selected slot and reload availability (409 SLOT_UNAVAILABLE).
     * @property resetMeetingType D-08: switch the meeting type back to In-app Voice (422 MEETING_LINK_UNAVAILABLE).
     */
    data class Show(
        val message: FieldError,
        val clearSlotAndReload: Boolean = false,
        val resetMeetingType: Boolean = false,
    ) : BookingFailure
}

/**
 * D-10 error mapping for the `POST /bookings/request` call. Server text is only ever wrapped in
 * [FieldError.Text], which is rendered as plain text (T-06-13).
 */
fun ApiException.toBookingFailure(): BookingFailure =
    when (this) {
        is ApiException.Http ->
            when (code) {
                ErrorCode.AccountSuspended -> BookingFailure.Silent
                ErrorCode.SlotUnavailable ->
                    BookingFailure.Show(
                        FieldError.Res(R.string.make_a_booking_error_slot_taken),
                        clearSlotAndReload = true,
                    )
                ErrorCode.MeetingLinkUnavailable ->
                    BookingFailure.Show(
                        FieldError.Res(R.string.make_a_booking_error_no_link),
                        resetMeetingType = true,
                    )
                ErrorCode.ExpertNotBookable ->
                    BookingFailure.Show(FieldError.Res(R.string.make_a_booking_error_not_bookable))
                ErrorCode.CannotBookSelf -> BookingFailure.Show(FieldError.Res(R.string.make_a_booking_error_self))
                // Defensive: the D-01 gate prevents paid bookings from reaching the server.
                ErrorCode.PaidTopicRequiresPayment ->
                    BookingFailure.Show(FieldError.Res(R.string.make_a_booking_error_needs_payment))
                ErrorCode.DuplicateBooking ->
                    BookingFailure.Show(FieldError.Res(R.string.make_a_booking_error_duplicate))
                else ->
                    BookingFailure.Show(
                        userMessage?.let { FieldError.Text(it) } ?: FieldError.Res(R.string.login_error_unknown),
                    )
            }
        is ApiException.Network -> BookingFailure.Show(FieldError.Res(R.string.login_error_network))
        is ApiException.Decoding -> BookingFailure.Show(FieldError.Res(R.string.login_error_unknown))
    }

/**
 * Message for an inline section failure (topics or availability load): the server message when there is
 * one, else the standard network copy.
 */
fun ApiException.toSectionMessage(): FieldError =
    when (this) {
        is ApiException.Http -> userMessage?.let { FieldError.Text(it) } ?: FieldError.Res(R.string.login_error_network)
        is ApiException.Network, is ApiException.Decoding -> FieldError.Res(R.string.login_error_network)
    }
