package com.uptrendinvestments.clickmeapp.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Nav keys of the auth back stack that lives under `AppRoute.Login`. The email travels in the keys
 * (not a shared ViewModel) so process death restores both the stack and each screen's input. No
 * password or OTP is ever placed in a key.
 */
@Serializable
data object LoginKey : NavKey

/** Forgot Password, seeded with the email typed on Login (D-07). */
@Serializable
data class ForgotPasswordKey(
    val email: String,
) : NavKey

/** Set New Password (OTP + new password) for [email]. */
@Serializable
data class SetNewPasswordKey(
    val email: String,
) : NavKey

/** Password Changed confirmation; [email] is pre-filled on Login afterwards (D-10). */
@Serializable
data class PasswordChangedKey(
    val email: String,
) : NavKey

/** Signup Initial step (SIGN-01). */
@Serializable
data object SignUpKey : NavKey
