package com.uptrendinvestments.clickmeapp.screens.expertmode

import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.model.enums.ErrorCode
import com.uptrendinvestments.clickmeapp.model.responses.FieldErrorDto
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.IOException

class ExpertErrorsTest {
    private fun http(
        status: Int,
        code: ErrorCode? = null,
        message: String? = null,
        fieldErrors: List<FieldErrorDto> = emptyList(),
    ) = ApiException.Http(
        status = status,
        code = code,
        rawCode = code?.wire,
        serverMessage = message,
        fieldErrors = fieldErrors,
        detail = null,
    )

    @Test
    fun validation_envelope_shows_field_message_not_validation_failed() {
        val error =
            http(
                status = 422,
                code = ErrorCode.ValidationError,
                message = "Validation failed",
                fieldErrors = listOf(FieldErrorDto("schedule.monday", "end_time must be after start_time")),
            ).toExpertDialogError()
        assertEquals(FieldError.Text("end_time must be after start_time"), error)
    }

    @Test
    fun server_message_used_when_no_field_errors() {
        assertEquals(
            FieldError.Text("Topic not found"),
            http(status = 404, message = "Topic not found").toExpertDialogError(),
        )
    }

    @Test
    fun blank_messages_fall_back_to_generic() {
        assertEquals(
            FieldError.Res(R.string.expert_error_generic),
            http(status = 500, message = "  ").toExpertDialogError(),
        )
    }

    @Test
    fun no_message_falls_back_to_generic() {
        assertEquals(FieldError.Res(R.string.expert_error_generic), http(status = 500).toExpertDialogError())
    }

    @Test
    fun account_suspended_is_owned_by_global_dialog() {
        assertNull(http(status = 403, code = ErrorCode.AccountSuspended, message = "Suspended").toExpertDialogError())
    }

    @Test
    fun network_maps_to_network_copy() {
        assertEquals(
            FieldError.Res(R.string.login_error_network),
            ApiException.Network(IOException("offline")).toExpertDialogError(),
        )
    }

    @Test
    fun decoding_maps_to_generic() {
        assertEquals(
            FieldError.Res(R.string.expert_error_generic),
            ApiException.Decoding(IllegalStateException("bad json")).toExpertDialogError(),
        )
    }
}
