package com.uptrendinvestments.clickmeapp.navigation

import androidx.compose.runtime.mutableStateOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthNavigatorTest {
    private fun newAuthNavigator(): AuthNavigator {
        val state =
            NavigationState(
                startRoute = LoginKey,
                topLevelRoute = mutableStateOf<NavKey>(LoginKey),
                backStacks = mapOf(LoginKey to NavBackStack<NavKey>(LoginKey)),
            )
        return AuthNavigator(Navigator(state))
    }

    private fun AuthNavigator.stack(): List<NavKey> = navigator.state.backStacks.getValue(LoginKey).toList()

    @Test
    fun openForgotPasswordPushesKeyCarryingEmail() {
        val auth = newAuthNavigator()
        auth.openForgotPassword("a@b.co")
        assertEquals(listOf<NavKey>(LoginKey, ForgotPasswordKey("a@b.co")), auth.stack())
    }

    @Test
    fun openForgotPasswordTwiceIsDoubleTapGuarded() {
        val auth = newAuthNavigator()
        auth.openForgotPassword("a@b.co")
        auth.openForgotPassword("a@b.co")
        assertEquals(listOf<NavKey>(LoginKey, ForgotPasswordKey("a@b.co")), auth.stack())
    }

    @Test
    fun recoveryFlowPushesEveryStep() {
        val auth = newAuthNavigator()
        auth.openForgotPassword("a@b.co")
        auth.openSetNewPassword("a@b.co")
        auth.openPasswordChanged("a@b.co")
        assertEquals(
            listOf<NavKey>(
                LoginKey,
                ForgotPasswordKey("a@b.co"),
                SetNewPasswordKey("a@b.co"),
                PasswordChangedKey("a@b.co"),
            ),
            auth.stack(),
        )
        assertEquals(PasswordChangedKey("a@b.co"), auth.top)
    }

    @Test
    fun returnToLoginClearsEveryRecoveryScreen() {
        val auth = newAuthNavigator()
        auth.openForgotPassword("a@b.co")
        auth.openSetNewPassword("a@b.co")
        auth.openPasswordChanged("a@b.co")
        auth.returnToLogin()
        assertEquals(listOf<NavKey>(LoginKey), auth.stack())
    }

    @Test
    fun handleBackOnPasswordChangedReturnsToLoginWithPrefill() {
        val auth = newAuthNavigator()
        auth.openForgotPassword("a@b.co")
        auth.openSetNewPassword("a@b.co")
        auth.openPasswordChanged("a@b.co")
        var prefill: String? = null
        auth.handleBack { prefill = it }
        assertEquals(listOf<NavKey>(LoginKey), auth.stack())
        assertEquals("a@b.co", prefill)
    }

    @Test
    fun handleBackOnSetNewPasswordPopsToForgotWithEmailKept() {
        val auth = newAuthNavigator()
        auth.openForgotPassword("a@b.co")
        auth.openSetNewPassword("a@b.co")
        var prefill: String? = null
        auth.handleBack { prefill = it }
        assertEquals(listOf<NavKey>(LoginKey, ForgotPasswordKey("a@b.co")), auth.stack())
        assertNull(prefill)
    }

    @Test
    fun handleBackOnForgotPopsToLogin() {
        val auth = newAuthNavigator()
        auth.openForgotPassword("a@b.co")
        auth.handleBack { }
        assertEquals(listOf<NavKey>(LoginKey), auth.stack())
    }

    @Test
    fun handleBackAtLoginRootIsNoOp() {
        val auth = newAuthNavigator()
        var prefill: String? = null
        auth.handleBack { prefill = it }
        assertEquals(listOf<NavKey>(LoginKey), auth.stack())
        assertNull(prefill)
    }

    @Test
    fun signUpPushesAndBackReturnsToLogin() {
        val auth = newAuthNavigator()
        auth.openSignUp()
        assertEquals(listOf<NavKey>(LoginKey, SignUpKey), auth.stack())
        auth.back()
        assertEquals(listOf<NavKey>(LoginKey), auth.stack())
    }
}
