package com.uptrendinvestments.clickmeapp.screens.login

import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.model.enums.ErrorCode
import com.uptrendinvestments.clickmeapp.model.responses.FieldErrorDto
import com.uptrendinvestments.clickmeapp.network.ApiException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.IOException

class AuthValidatorsTest {
    private fun res(id: Int) = FieldError.Res(id)

    private fun http(
        status: Int = 500,
        code: ErrorCode? = null,
        message: String? = null,
        fields: List<FieldErrorDto> = emptyList(),
    ) = ApiException.Http(status, code, code?.wire, message, fields, null)

    @Test
    fun loginEmail_blankIsRequired() {
        assertEquals(res(R.string.error_email_required), AuthValidators.loginEmail(""))
        assertEquals(res(R.string.error_email_required), AuthValidators.loginEmail("   "))
    }

    @Test
    fun loginEmail_malformedIsInvalid_andValidIsTrimmed() {
        assertEquals(res(R.string.error_email_invalid), AuthValidators.loginEmail("bad"))
        assertEquals(res(R.string.error_email_invalid), AuthValidators.loginEmail("a@b"))
        assertNull(AuthValidators.loginEmail("  a@b.co "))
    }

    @Test
    fun loginPassword_rules() {
        assertEquals(res(R.string.error_password_required), AuthValidators.loginPassword(""))
        assertEquals(res(R.string.error_password_too_short), AuthValidators.loginPassword("1234567"))
        assertNull(AuthValidators.loginPassword("12345678"))
    }

    @Test
    fun forgotEmail_rules() {
        assertEquals(res(R.string.error_forgot_email_required), AuthValidators.forgotEmail(""))
        assertEquals(res(R.string.error_email_invalid), AuthValidators.forgotEmail("x@y"))
        assertNull(AuthValidators.forgotEmail(" a@b.co "))
    }

    @Test
    fun resetCode_rules() {
        assertEquals(res(R.string.error_code_required), AuthValidators.resetCode(""))
        assertEquals(res(R.string.error_code_invalid), AuthValidators.resetCode("12345"))
        assertNull(AuthValidators.resetCode("123456"))
    }

    @Test
    fun newPassword_rules() {
        assertEquals(res(R.string.error_new_password_required), AuthValidators.newPassword(""))
        assertEquals(res(R.string.error_password_too_short), AuthValidators.newPassword("abc1!"))
        assertEquals(res(R.string.error_new_password_rules), AuthValidators.newPassword("abcdefgh1"))
        assertEquals(res(R.string.error_new_password_rules), AuthValidators.newPassword("abcdefg!"))
        assertNull(AuthValidators.newPassword("abcdefg1!"))
    }

    @Test
    fun confirmPassword_rules() {
        assertEquals(res(R.string.error_confirm_required), AuthValidators.confirmPassword("abcdefg1!", ""))
        assertEquals(res(R.string.error_confirm_mismatch), AuthValidators.confirmPassword("abcdefg1!", "abcdefg1?"))
        assertNull(AuthValidators.confirmPassword("abcdefg1!", "abcdefg1!"))
    }

    @Test
    fun sanitizeCode_keepsAsciiDigitsMaxSix() {
        assertEquals("123456", AuthValidators.sanitizeCode("12a3 4-5678"))
        assertEquals("", AuthValidators.sanitizeCode("١٢٣"))
    }

    @Test
    fun toDialogError_mapping() {
        assertNull(http(403, ErrorCode.AccountSuspended).toDialogError())
        assertEquals(FieldError.Text("Down"), http(message = "Down").toDialogError())
        assertEquals(res(R.string.login_error_unknown), http().toDialogError())
        assertEquals(res(R.string.login_error_network), ApiException.Network(IOException("x")).toDialogError())
        assertEquals(res(R.string.login_error_unknown), ApiException.Decoding(RuntimeException("x")).toDialogError())
    }

    @Test
    fun fieldMessage_matchesFirstListedField() {
        val e = http(422, ErrorCode.ValidationError, fields = listOf(FieldErrorDto("identity", "Bad")))
        assertEquals("Bad", e.fieldMessage("identity", "email"))
        assertNull(e.fieldMessage("password"))
    }
}
