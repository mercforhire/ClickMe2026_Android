package com.uptrendinvestments.clickmeapp.navigation

import androidx.navigation3.runtime.NavKey

/**
 * Auth-flow navigation actions over [Navigator]. The auth stack is a single stack rooted at
 * [LoginKey]; recovery screens and the Sign Up placeholder are pushed on it.
 */
class AuthNavigator(
    val navigator: Navigator,
) {
    /** The key currently on top of the auth stack. */
    val top: NavKey
        get() {
            val loginStack = navigator.state.backStacks.getValue(LoginKey)
            return loginStack.last()
        }

    fun openForgotPassword(email: String) = pushUnlessOnTop(ForgotPasswordKey(email))

    fun openSetNewPassword(email: String) = pushUnlessOnTop(SetNewPasswordKey(email))

    fun openPasswordChanged(email: String) = pushUnlessOnTop(PasswordChangedKey(email))

    fun openSignUp() = pushUnlessOnTop(SignUpKey)

    fun back() = navigator.goBack()

    /** Pop every recovery screen so a used OTP form is unreachable (D-10, D-11). */
    fun returnToLogin() = navigator.reselect(LoginKey)

    /**
     * System Back. On Password Changed it behaves like "Go to Login" (D-11): clear to Login and
     * report the reset email through [onReturnToLogin]. Elsewhere it pops one entry; at the Login
     * root it is a no-op and NavDisplay lets the activity exit.
     */
    fun handleBack(onReturnToLogin: (String) -> Unit) {
        val current = top
        if (current is PasswordChangedKey) {
            returnToLogin()
            onReturnToLogin(current.email)
        } else {
            back()
        }
    }

    /** Double-tap guard: no-op when the same key type is already on top. */
    private fun pushUnlessOnTop(key: NavKey) {
        if (top::class != key::class) navigator.navigate(key)
    }
}
