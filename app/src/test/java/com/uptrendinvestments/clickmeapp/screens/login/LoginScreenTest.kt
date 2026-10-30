package com.uptrendinvestments.clickmeapp.screens.login

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LoginScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private var submitCount = 0
    private var forgotCount = 0
    private var signUpCount = 0
    private var dismissCount = 0

    private fun show(state: LoginUiState) {
        rule.setContent {
            ClickMeTheme {
                LoginContent(
                    state = state,
                    onEmailChange = {},
                    onPasswordChange = {},
                    onSubmit = { submitCount++ },
                    onForgotPassword = { forgotCount++ },
                    onSignUp = { signUpCount++ },
                    onDismissDialog = { dismissCount++ },
                )
            }
        }
    }

    @Test
    fun rendersHeaderFieldsLinksAndFooter() {
        show(LoginUiState())
        rule.onNodeWithText("ClickMe").assertExists()
        rule.onNodeWithText("Welcome back! Please enter your details.").assertExists()
        rule.onNodeWithText("Email").assertExists()
        rule.onNodeWithText("Password").assertExists()
        rule.onNodeWithText("Forgot Password?").assertExists()
        rule.onNode(hasText("Login") and hasClickAction()).assertExists()
        rule.onNodeWithText("Don't have an account?").assertExists()
        rule.onNodeWithText("Sign Up").assertExists()
    }

    @Test
    fun tappingLoginCallsOnSubmit() {
        show(LoginUiState(email = "a@b.co", password = "password1"))
        rule.onNode(hasText("Login") and hasClickAction()).performClick()
        assertEquals(1, submitCount)
    }

    @Test
    fun tappingForgotPasswordCallsCallbackOnce() {
        show(LoginUiState())
        rule.onNodeWithText("Forgot Password?").performClick()
        assertEquals(1, forgotCount)
        assertEquals(0, submitCount)
    }

    @Test
    fun tappingSignUpCallsCallbackOnce() {
        show(LoginUiState())
        // The footer sits below the fold of the small Robolectric screen.
        rule.onNodeWithText("Sign Up").performScrollTo().performClick()
        assertEquals(1, signUpCount)
    }

    @Test
    fun localEmailErrorIsShownFromStringResource() {
        show(LoginUiState(emailError = FieldError.Res(R.string.error_email_required)))
        rule.onNodeWithText("Email is required").assertExists()
    }

    @Test
    fun serverPasswordErrorIsShownInline() {
        show(LoginUiState(passwordError = FieldError.Text("Invalid email or password")))
        rule.onNodeWithText("Invalid email or password").assertExists()
    }

    @Test
    fun noErrorTextWhenStateIsClean() {
        show(LoginUiState())
        rule.onNodeWithText("Email is required").assertDoesNotExist()
        rule.onNodeWithText("Something went wrong").assertDoesNotExist()
    }

    @Test
    fun dialogErrorShowsTitleMessageAndOkDismisses() {
        show(LoginUiState(dialogError = FieldError.Text("Server unavailable")))
        rule.onNodeWithText("Something went wrong").assertExists()
        rule.onNodeWithText("Server unavailable").assertExists()
        rule.onNodeWithText("OK").performClick()
        assertEquals(1, dismissCount)
    }

    @Test
    fun whileSubmittingLinksAreDisabledAndLoginShowsSpinner() {
        show(LoginUiState(email = "a@b.co", password = "password1", isSubmitting = true))
        rule.onNodeWithText("Forgot Password?").assertIsNotEnabled()
        rule.onNodeWithText("Sign Up").assertIsNotEnabled()
        rule.onNodeWithText("Login").assertDoesNotExist()
        rule.onNode(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "In progress")).assertExists()
    }
}
