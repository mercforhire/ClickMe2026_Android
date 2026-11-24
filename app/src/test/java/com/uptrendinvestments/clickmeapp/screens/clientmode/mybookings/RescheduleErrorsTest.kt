package com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings

import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.model.enums.ErrorCode
import com.uptrendinvestments.clickmeapp.network.ApiErrorParser
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class RescheduleErrorsTest {
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
    fun forbidden_reloads_with_fixed_copy_with_or_without_a_code() {
        val expected = RescheduleFailure.Show(res(R.string.reschedule_error_forbidden), reload = true)
        assertEquals(expected, http(403, ErrorCode.Forbidden).toRescheduleFailure())
        assertEquals(expected, http(403, null).toRescheduleFailure())
    }

    @Test
    fun invalid_status_reloads_with_not_pending_copy() {
        assertEquals(
            RescheduleFailure.Show(res(R.string.reschedule_error_not_pending), reload = true),
            http(409, ErrorCode.InvalidStatus).toRescheduleFailure(),
        )
    }

    @Test
    fun expert_unavailable_reloads_with_fixed_copy() {
        assertEquals(
            RescheduleFailure.Show(res(R.string.reschedule_error_expert_unavailable), reload = true),
            http(409, ErrorCode.ExpertUnavailable).toRescheduleFailure(),
        )
    }

    @Test
    fun validation_error_means_the_proposed_time_passed_and_reloads() {
        assertEquals(
            RescheduleFailure.Show(res(R.string.reschedule_error_time_passed), reload = true),
            http(422, ErrorCode.ValidationError, "Proposed time is no longer in the future").toRescheduleFailure(),
        )
    }

    @Test
    fun outside_auth_window_shows_the_server_message_and_reloads() {
        assertEquals(
            RescheduleFailure.Show(FieldError.Text("Direct charging is off"), reload = true),
            http(422, ErrorCode.RescheduleOutsideAuthWindow, "Direct charging is off").toRescheduleFailure(),
        )
    }

    @Test
    fun outside_auth_window_without_a_message_uses_the_android_copy() {
        assertEquals(
            RescheduleFailure.Show(res(R.string.reschedule_error_outside_window), reload = true),
            http(422, ErrorCode.RescheduleOutsideAuthWindow).toRescheduleFailure(),
        )
    }

    @Test
    fun too_far_ahead_from_the_backend_body_shows_the_server_message_and_reloads() {
        val message =
            "Paid sessions can be rescheduled up to 6 days ahead. Please choose a time within the next 6 days."
        val body = """{"status":"error","code":"RESCHEDULE_TOO_FAR_AHEAD","message":"$message"}"""
        val failure = ApiErrorParser.httpError(422, body).toRescheduleFailure()
        assertEquals(RescheduleFailure.Show(FieldError.Text(message), reload = true), failure)
        assertFalse((failure as RescheduleFailure.Show).pop)
    }

    @Test
    fun too_far_ahead_without_a_message_uses_the_android_copy() {
        assertEquals(
            RescheduleFailure.Show(res(R.string.reschedule_error_too_far_ahead), reload = true),
            http(422, ErrorCode.RescheduleTooFarAhead).toRescheduleFailure(),
        )
    }

    @Test
    fun invalid_payment_method_shows_the_server_message_without_reload() {
        val failure = http(422, ErrorCode.InvalidPaymentMethod, "Card not found").toRescheduleFailure()
        assertEquals(RescheduleFailure.Show(FieldError.Text("Card not found"), reload = false), failure)
        assertFalse((failure as RescheduleFailure.Show).reload)
    }

    @Test
    fun payment_method_required_defensively_maps_to_card_unusable_copy() {
        assertEquals(
            RescheduleFailure.Show(res(R.string.reschedule_card_unusable_message)),
            http(422, ErrorCode.PaymentMethodRequired, "No card").toRescheduleFailure(),
        )
    }

    @Test
    fun not_found_pops_with_fixed_copy_with_or_without_a_code() {
        val expected = RescheduleFailure.Show(res(R.string.reschedule_error_not_found), pop = true)
        assertEquals(expected, http(404, ErrorCode.NotFound).toRescheduleFailure())
        assertEquals(expected, http(404, null).toRescheduleFailure())
    }

    @Test
    fun account_suspended_is_silent() {
        assertEquals(RescheduleFailure.Silent, http(403, ErrorCode.AccountSuspended).toRescheduleFailure())
        assertEquals(CancelFailure.Silent, http(403, ErrorCode.AccountSuspended).toCancelFailure())
    }

    @Test
    fun other_http_errors_show_the_server_message_or_generic_copy() {
        assertEquals(
            RescheduleFailure.Show(FieldError.Text("Boom")),
            http(500, null, "Boom").toRescheduleFailure(),
        )
        assertEquals(
            RescheduleFailure.Show(res(R.string.login_error_unknown)),
            http(500, null, null).toRescheduleFailure(),
        )
    }

    @Test
    fun transport_failures_map_to_network_and_unknown_copy() {
        assertEquals(
            RescheduleFailure.Show(res(R.string.login_error_network)),
            ApiException.Network(IOException("offline")).toRescheduleFailure(),
        )
        assertEquals(
            RescheduleFailure.Show(res(R.string.login_error_unknown)),
            ApiException.Decoding(IllegalStateException("bad json")).toRescheduleFailure(),
        )
    }

    @Test
    fun cancel_not_found_returns_to_bookings() {
        val expected = CancelFailure.Show(res(R.string.cancel_booking_error_not_found), returnToBookings = true)
        assertEquals(expected, http(404, ErrorCode.NotFound).toCancelFailure())
        assertEquals(expected, http(404, null).toCancelFailure())
    }

    @Test
    fun cancel_invalid_status_returns_to_bookings() {
        assertEquals(
            CancelFailure.Show(res(R.string.cancel_booking_error_not_cancellable), returnToBookings = true),
            http(409, ErrorCode.InvalidStatus).toCancelFailure(),
        )
    }

    @Test
    fun cancel_other_errors_stay_on_the_confirm_step() {
        val failure = http(500, null, "Boom").toCancelFailure()
        assertEquals(CancelFailure.Show(FieldError.Text("Boom"), returnToBookings = false), failure)
        assertFalse((failure as CancelFailure.Show).returnToBookings)
        assertEquals(
            CancelFailure.Show(res(R.string.login_error_unknown)),
            http(500, null, null).toCancelFailure(),
        )
    }

    @Test
    fun cancel_session_already_ended_shows_the_server_message_and_stays() {
        val message = "This session has already ended, so it can't be cancelled."
        val error =
            ApiException.Http(
                status = 409,
                code = null,
                rawCode = "SESSION_ALREADY_ENDED",
                serverMessage = message,
                fieldErrors = emptyList(),
                detail = null,
            )
        assertEquals(CancelFailure.Show(FieldError.Text(message), returnToBookings = false), error.toCancelFailure())
    }

    @Test
    fun cancel_transport_failures_map_to_network_and_unknown_copy() {
        assertEquals(
            CancelFailure.Show(res(R.string.login_error_network)),
            ApiException.Network(IOException("offline")).toCancelFailure(),
        )
        assertEquals(
            CancelFailure.Show(res(R.string.login_error_unknown)),
            ApiException.Decoding(IllegalStateException("bad json")).toCancelFailure(),
        )
    }

    @Test
    fun reschedule_ui_state_defaults_and_submitting_flag() {
        val state = RescheduleUiState()
        assertEquals(RescheduleSubmitting.None, state.submitting)
        assertEquals(null, state.dialog)
        assertEquals(null, state.notice)
        assertFalse(state.isSubmitting)
        assertTrue(RescheduleUiState(submitting = RescheduleSubmitting.Accepting).isSubmitting)
        assertTrue(RescheduleUiState(submitting = RescheduleSubmitting.Declining).isSubmitting)
        assertEquals(500, DECLINE_REASON_MAX_CHARS)
    }
}
