package com.uptrendinvestments.clickmeapp.screens.login

import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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

    private fun show(state: LoginUiState) {
        rule.setContent {
            ClickMeTheme {
                LoginContent(
                    state = state,
                    onEmailChange = {},
                    onPasswordChange = {},
                    onSubmit = { submitCount++ },
                )
            }
        }
    }

    @Test
    fun rendersFieldsAndSubmitButton() {
        show(LoginUiState())
        rule.onNodeWithText("Email").assertExists()
        rule.onNodeWithText("Password").assertExists()
        rule.onNode(hasText("Log in") and hasClickAction()).assertExists()
    }

    @Test
    fun tappingSubmitCallsOnSubmit() {
        show(LoginUiState(email = "a@b.co", password = "pw"))
        // The title and the button share the "Log in" text; the button is the clickable one.
        rule.onNode(hasText("Log in") and hasClickAction()).performClick()
        assertEquals(1, submitCount)
    }

    @Test
    fun missingFieldsErrorIsShown() {
        show(LoginUiState(error = LoginError.MissingFields))
        rule.onNodeWithText("Enter your email and password.").assertExists()
    }

    @Test
    fun serverErrorMessageIsShown() {
        show(LoginUiState(error = LoginError.Server("Invalid email or password")))
        rule.onNodeWithText("Invalid email or password").assertExists()
    }

    @Test
    fun noErrorTextWhenErrorIsNull() {
        show(LoginUiState())
        rule.onNodeWithText("Enter your email and password.").assertDoesNotExist()
    }
}
