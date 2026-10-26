package com.uptrendinvestments.clickmeapp.screens.expertmode.bookingsummary

import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.model.enums.ErrorCode
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.RescheduleFailure
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

class ExpertRescheduleErrorsTest {
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
    fun slotUnavailableShowsTheDedicatedMessageAndReloads() {
        assertEquals(
            RescheduleFailure.Show(
                res(R.string.expert_reschedule_error_slot_unavailable),
                reload = true,
                pop = false,
            ),
            http(409, ErrorCode.SlotUnavailable, "Slot taken").toExpertRescheduleFailure(),
        )
    }

    @Test
    fun forbiddenReloadsWithFixedCopyWithOrWithoutACode() {
        val expected = RescheduleFailure.Show(res(R.string.reschedule_error_forbidden), reload = true)
        assertEquals(expected, http(403, ErrorCode.Forbidden).toExpertRescheduleFailure())
        assertEquals(expected, http(403, null).toExpertRescheduleFailure())
    }

    @Test
    fun invalidStatusReloadsWithNotPendingCopy() {
        assertEquals(
            RescheduleFailure.Show(res(R.string.reschedule_error_not_pending), reload = true),
            http(409, ErrorCode.InvalidStatus).toExpertRescheduleFailure(),
        )
    }

    @Test
    fun validationErrorShowsTheServerMessageAndReloads() {
        assertEquals(
            RescheduleFailure.Show(FieldError.Text("x"), reload = true),
            http(422, ErrorCode.ValidationError, "x").toExpertRescheduleFailure(),
        )
    }

    @Test
    fun clientUnavailableShowsTheServerMessageAndReloads() {
        assertEquals(
            RescheduleFailure.Show(FieldError.Text("x"), reload = true),
            http(409, ErrorCode.ClientUnavailable, "x").toExpertRescheduleFailure(),
        )
    }

    @Test
    fun outsideAuthWindowShowsTheServerMessageAndReloads() {
        assertEquals(
            RescheduleFailure.Show(FieldError.Text("x"), reload = true),
            http(422, ErrorCode.RescheduleOutsideAuthWindow, "x").toExpertRescheduleFailure(),
        )
    }

    @Test
    fun serverCodesWithoutAMessageFallBackToTheUnknownCopyAndStillReload() {
        assertEquals(
            RescheduleFailure.Show(res(R.string.login_error_unknown), reload = true),
            http(422, ErrorCode.ValidationError).toExpertRescheduleFailure(),
        )
    }

    @Test
    fun notFoundLeavesTheScreenWithOrWithoutACode() {
        val expected = RescheduleFailure.Show(res(R.string.reschedule_error_not_found), pop = true)
        assertEquals(expected, http(404, ErrorCode.NotFound).toExpertRescheduleFailure())
        assertEquals(expected, http(404, null).toExpertRescheduleFailure())
    }

    @Test
    fun accountSuspendedIsSilent() {
        assertEquals(
            RescheduleFailure.Silent,
            http(403, ErrorCode.AccountSuspended).toExpertRescheduleFailure(),
        )
    }

    @Test
    fun unmappedServerErrorShowsTheMessageWithoutReload() {
        assertEquals(
            RescheduleFailure.Show(FieldError.Text("Boom")),
            http(500, null, "Boom").toExpertRescheduleFailure(),
        )
    }

    @Test
    fun unmappedServerErrorWithoutAMessageIsUnknown() {
        assertEquals(
            RescheduleFailure.Show(res(R.string.login_error_unknown)),
            http(500, null).toExpertRescheduleFailure(),
        )
    }

    @Test
    fun networkFailureShowsTheNetworkCopy() {
        assertEquals(
            RescheduleFailure.Show(res(R.string.login_error_network)),
            ApiException.Network(IOException("offline")).toExpertRescheduleFailure(),
        )
    }

    @Test
    fun decodingFailureShowsTheUnknownCopy() {
        assertEquals(
            RescheduleFailure.Show(res(R.string.login_error_unknown)),
            ApiException.Decoding(IllegalStateException("bad json")).toExpertRescheduleFailure(),
        )
    }
}
