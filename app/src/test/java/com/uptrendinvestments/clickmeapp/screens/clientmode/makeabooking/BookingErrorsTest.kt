package com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking

import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.model.enums.ErrorCode
import com.uptrendinvestments.clickmeapp.model.enums.MeetingType
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.IOException

class BookingErrorsTest {
    private fun http(
        status: Int,
        code: ErrorCode?,
        message: String? = null,
    ) = ApiException.Http(
        status = status,
        code = code,
        rawCode = code?.wire,
        serverMessage = message,
        fieldErrors = emptyList(),
        detail = null,
    )

    private fun res(id: Int) = FieldError.Res(id)

    @Test
    fun meeting_type_wire_values() {
        assertEquals("in_app_voice", MeetingType.InAppVoice.wire)
        assertEquals("skype_zoom", MeetingType.SkypeZoom.wire)
    }

    @Test
    fun meeting_type_from_wire_is_lenient() {
        assertEquals(MeetingType.SkypeZoom, MeetingType.fromWire("skype_zoom"))
        assertNull(MeetingType.fromWire("zoom"))
        assertNull(MeetingType.fromWire(null))
    }

    @Test
    fun meeting_type_entries_are_in_picker_order() {
        assertEquals(listOf(MeetingType.InAppVoice, MeetingType.SkypeZoom), MeetingType.entries)
    }

    @Test
    fun slot_unavailable_clears_slot_and_reloads() {
        val failure = http(409, ErrorCode.SlotUnavailable).toBookingFailure()
        assertEquals(
            BookingFailure.Show(res(R.string.make_a_booking_error_slot_taken), clearSlotAndReload = true),
            failure,
        )
    }

    @Test
    fun meeting_link_unavailable_resets_meeting_type() {
        val failure = http(422, ErrorCode.MeetingLinkUnavailable).toBookingFailure()
        assertEquals(
            BookingFailure.Show(res(R.string.make_a_booking_error_no_link), resetMeetingType = true),
            failure,
        )
    }

    @Test
    fun expert_not_bookable_message() {
        assertEquals(
            BookingFailure.Show(res(R.string.make_a_booking_error_not_bookable)),
            http(422, ErrorCode.ExpertNotBookable).toBookingFailure(),
        )
    }

    @Test
    fun cannot_book_self_message() {
        assertEquals(
            BookingFailure.Show(res(R.string.make_a_booking_error_self)),
            http(422, ErrorCode.CannotBookSelf).toBookingFailure(),
        )
    }

    @Test
    fun paid_topic_requires_payment_message() {
        assertEquals(
            BookingFailure.Show(res(R.string.make_a_booking_error_needs_payment)),
            http(402, ErrorCode.PaidTopicRequiresPayment).toBookingFailure(),
        )
    }

    @Test
    fun duplicate_booking_message() {
        assertEquals(
            BookingFailure.Show(res(R.string.make_a_booking_error_duplicate)),
            http(409, ErrorCode.DuplicateBooking).toBookingFailure(),
        )
    }

    @Test
    fun expert_unavailable_uses_server_message() {
        val failure = http(409, ErrorCode.ExpertUnavailable, "This expert is not available for booking.")
        assertEquals(
            BookingFailure.Show(FieldError.Text("This expert is not available for booking.")),
            failure.toBookingFailure(),
        )
    }

    @Test
    fun unknown_code_without_message_uses_unknown_copy() {
        assertEquals(
            BookingFailure.Show(res(R.string.login_error_unknown)),
            http(500, null).toBookingFailure(),
        )
    }

    @Test
    fun account_suspended_is_silent() {
        assertEquals(BookingFailure.Silent, http(403, ErrorCode.AccountSuspended).toBookingFailure())
    }

    @Test
    fun network_error_uses_network_copy() {
        assertEquals(
            BookingFailure.Show(res(R.string.login_error_network)),
            ApiException.Network(IOException()).toBookingFailure(),
        )
    }

    @Test
    fun decoding_error_uses_unknown_copy() {
        assertEquals(
            BookingFailure.Show(res(R.string.login_error_unknown)),
            ApiException.Decoding(RuntimeException()).toBookingFailure(),
        )
    }

    @Test
    fun section_message_uses_server_message() {
        assertEquals(
            FieldError.Text("Expert not found"),
            http(404, ErrorCode.ExpertNotFound, "Expert not found").toSectionMessage(),
        )
    }

    @Test
    fun section_message_falls_back_to_network_copy() {
        val network = res(R.string.login_error_network)
        assertEquals(network, http(500, null).toSectionMessage())
        assertEquals(network, ApiException.Network(IOException()).toSectionMessage())
        assertEquals(network, ApiException.Decoding(RuntimeException()).toSectionMessage())
    }
}
