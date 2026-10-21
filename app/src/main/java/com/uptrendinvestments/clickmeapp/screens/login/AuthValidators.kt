package com.uptrendinvestments.clickmeapp.screens.login

import com.uptrendinvestments.clickmeapp.R

/**
 * Client-side validators ported from the iOS login, forgot-password and reset-password view models (D-01, D-08),
 * plus the signup rules, which follow the server schema rather than iOS (D-02).
 */
object AuthValidators {
    val EMAIL_REGEX = Regex("""^[^@\s]+@[^@\s]+\.[^@\s]+$""")
    private val DIGIT = Regex("[0-9]")
    private val LETTER = Regex("[A-Za-z]")
    private val SYMBOL = Regex("[^A-Za-z0-9]")
    private val SIX_DIGITS = Regex("""^\d{6}$""")

    private const val MIN_PASSWORD_LENGTH = 8
    private const val CODE_LENGTH = 6

    fun loginEmail(raw: String): FieldError? = emailError(raw, R.string.error_email_required)

    fun loginPassword(password: String): FieldError? =
        when {
            password.isEmpty() -> FieldError.Res(R.string.error_password_required)
            password.length < MIN_PASSWORD_LENGTH -> FieldError.Res(R.string.error_password_too_short)
            else -> null
        }

    /** Signup email: same regex and messages as login (D-02). */
    fun signupEmail(raw: String): FieldError? = emailError(raw, R.string.error_email_required)

    /** Signup password: the server rule, min 8 characters with at least one letter and one digit (D-02). */
    fun signupPassword(password: String): FieldError? =
        when {
            password.isEmpty() -> FieldError.Res(R.string.error_password_required)
            password.length < MIN_PASSWORD_LENGTH -> FieldError.Res(R.string.error_password_too_short)
            !LETTER.containsMatchIn(password) -> FieldError.Res(R.string.error_signup_password_letter)
            !DIGIT.containsMatchIn(password) -> FieldError.Res(R.string.error_signup_password_number)
            else -> null
        }

    fun forgotEmail(raw: String): FieldError? = emailError(raw, R.string.error_forgot_email_required)

    fun resetCode(raw: String): FieldError? {
        val code = raw.trim()
        return when {
            code.isEmpty() -> FieldError.Res(R.string.error_code_required)
            !SIX_DIGITS.matches(code) -> FieldError.Res(R.string.error_code_invalid)
            else -> null
        }
    }

    fun newPassword(password: String): FieldError? =
        when {
            password.isEmpty() -> FieldError.Res(R.string.error_new_password_required)
            password.length < MIN_PASSWORD_LENGTH -> FieldError.Res(R.string.error_password_too_short)
            !DIGIT.containsMatchIn(password) || !SYMBOL.containsMatchIn(password) ->
                FieldError.Res(R.string.error_new_password_rules)
            else -> null
        }

    fun confirmPassword(
        password: String,
        confirm: String,
    ): FieldError? =
        when {
            confirm.isEmpty() -> FieldError.Res(R.string.error_confirm_required)
            confirm != password -> FieldError.Res(R.string.error_confirm_mismatch)
            else -> null
        }

    /** Keeps ASCII digits only and at most six of them. */
    fun sanitizeCode(raw: String): String = raw.filter { it in '0'..'9' }.take(CODE_LENGTH)

    private fun emailError(
        raw: String,
        requiredId: Int,
    ): FieldError? {
        val email = raw.trim()
        return when {
            email.isEmpty() -> FieldError.Res(requiredId)
            !EMAIL_REGEX.matches(email) -> FieldError.Res(R.string.error_email_invalid)
            else -> null
        }
    }
}
