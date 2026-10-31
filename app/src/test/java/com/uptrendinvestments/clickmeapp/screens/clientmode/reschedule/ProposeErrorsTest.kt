package com.uptrendinvestments.clickmeapp.screens.clientmode.reschedule

import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.model.enums.ErrorCode
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

class ProposeErrorsTest {
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

    // ---- client ----

    @Test
    fun clientOutsideAuthWindowUsesTheFixedCopyEvenWithAServerMessage() {
        val expected = ProposeFailure.Show(res(R.string.reschedule_start_error_outside_window_client))
        assertEquals(expected, http(422, ErrorCode.RescheduleOutsideAuthWindow, "Server text").toClientProposeFailure())
        assertEquals(expected, http(422, ErrorCode.RescheduleOutsideAuthWindow).toClientProposeFailure())
    }

    @Test
    fun clientTooFarAheadShowsServerTextElseFallbackAndClearsTheSlot() {
        assertEquals(
            ProposeFailure.Show(FieldError.Text("M"), clearSlot = true),
            http(422, ErrorCode.RescheduleTooFarAhead, "M").toClientProposeFailure(),
        )
        assertEquals(
            ProposeFailure.Show(res(R.string.reschedule_start_error_too_far_ahead), clearSlot = true),
            http(422, ErrorCode.RescheduleTooFarAhead).toClientProposeFailure(),
        )
    }

    @Test
    fun clientValidationErrorShowsServerTextElseTimePassedAndClearsTheSlot() {
        assertEquals(
            ProposeFailure.Show(FieldError.Text("Must be in the future"), clearSlot = true),
            http(422, ErrorCode.ValidationError, "Must be in the future").toClientProposeFailure(),
        )
        assertEquals(
            ProposeFailure.Show(res(R.string.reschedule_start_error_time_passed), clearSlot = true),
            http(422, ErrorCode.ValidationError).toClientProposeFailure(),
        )
    }

    @Test
    fun clientInvalidPaymentMethodShowsServerTextAndStays() {
        assertEquals(
            ProposeFailure.Show(FieldError.Text("Card belongs to another customer")),
            http(422, ErrorCode.InvalidPaymentMethod, "Card belongs to another customer").toClientProposeFailure(),
        )
    }

    @Test
    fun clientPaymentMethodRequiredShowsTheCardUnusableCopy() {
        assertEquals(
            ProposeFailure.Show(res(R.string.reschedule_card_unusable_message)),
            http(422, ErrorCode.PaymentMethodRequired, "Needs card").toClientProposeFailure(),
        )
    }

    @Test
    fun clientSlotTakenClearsTheSlotAndReloads() {
        val expected =
            ProposeFailure.Show(
                res(R.string.reschedule_error_expert_unavailable),
                clearSlot = true,
                reloadSlots = true,
            )
        assertEquals(expected, http(409, ErrorCode.ExpertUnavailable, "x").toClientProposeFailure())
        assertEquals(expected, http(409, ErrorCode.SlotUnavailable, "x").toClientProposeFailure())
    }

    @Test
    fun clientInvalidStatusAndMissingBookingLeaveThePicker() {
        assertEquals(
            ProposeFailure.Show(res(R.string.reschedule_start_error_invalid_status), leave = true),
            http(409, ErrorCode.InvalidStatus).toClientProposeFailure(),
        )
        val notFound = ProposeFailure.Show(res(R.string.reschedule_error_not_found), leave = true)
        assertEquals(notFound, http(404, ErrorCode.NotFound).toClientProposeFailure())
        assertEquals(notFound, http(404, null).toClientProposeFailure())
    }

    @Test
    fun clientSuspendedIsSilentAndOtherCodesShowTheServerMessage() {
        assertEquals(ProposeFailure.Silent, http(403, ErrorCode.AccountSuspended).toClientProposeFailure())
        assertEquals(
            ProposeFailure.Show(FieldError.Text("X")),
            http(500, null, "X").toClientProposeFailure(),
        )
        assertEquals(
            ProposeFailure.Show(res(R.string.login_error_unknown)),
            http(500, null).toClientProposeFailure(),
        )
    }

    @Test
    fun clientNetworkAndDecodingFailuresUseTheGenericCopy() {
        assertEquals(
            ProposeFailure.Show(res(R.string.login_error_network)),
            ApiException.Network(IOException("offline")).toClientProposeFailure(),
        )
        assertEquals(
            ProposeFailure.Show(res(R.string.login_error_unknown)),
            ApiException.Decoding(IllegalStateException("bad")).toClientProposeFailure(),
        )
    }

    // ---- expert ----

    @Test
    fun expertOutsideAuthWindowUsesTheExpertFixedCopy() {
        assertEquals(
            ProposeFailure.Show(res(R.string.reschedule_start_error_outside_window_expert)),
            http(422, ErrorCode.RescheduleOutsideAuthWindow, "Server text").toExpertProposeFailure(),
        )
    }

    @Test
    fun expertSlotTakenUsesTheExpertCopyClearsTheSlotAndReloads() {
        val expected =
            ProposeFailure.Show(
                res(R.string.expert_reschedule_error_slot_unavailable),
                clearSlot = true,
                reloadSlots = true,
            )
        assertEquals(expected, http(409, ErrorCode.SlotUnavailable).toExpertProposeFailure())
        assertEquals(expected, http(409, ErrorCode.ExpertUnavailable).toExpertProposeFailure())
    }

    @Test
    fun expertPaymentMethodRequiredShowsTheServerTextNotCardCopy() {
        assertEquals(
            ProposeFailure.Show(FieldError.Text("Needs card")),
            http(422, ErrorCode.PaymentMethodRequired, "Needs card").toExpertProposeFailure(),
        )
    }

    @Test
    fun expertSharedRowsMatchTheClient() {
        assertEquals(
            ProposeFailure.Show(FieldError.Text("M"), clearSlot = true),
            http(422, ErrorCode.RescheduleTooFarAhead, "M").toExpertProposeFailure(),
        )
        assertEquals(
            ProposeFailure.Show(res(R.string.reschedule_start_error_time_passed), clearSlot = true),
            http(422, ErrorCode.ValidationError).toExpertProposeFailure(),
        )
        assertEquals(
            ProposeFailure.Show(res(R.string.reschedule_start_error_invalid_status), leave = true),
            http(409, ErrorCode.InvalidStatus).toExpertProposeFailure(),
        )
        assertEquals(
            ProposeFailure.Show(res(R.string.reschedule_error_not_found), leave = true),
            http(404, null).toExpertProposeFailure(),
        )
        assertEquals(ProposeFailure.Silent, http(403, ErrorCode.AccountSuspended).toExpertProposeFailure())
        assertEquals(
            ProposeFailure.Show(res(R.string.login_error_network)),
            ApiException.Network(IOException("offline")).toExpertProposeFailure(),
        )
    }
}
