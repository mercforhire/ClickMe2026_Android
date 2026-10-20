package com.uptrendinvestments.clickmeapp.screens.login.forgetpassword

import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.buttons.SaveState
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ForgotPasswordScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private var sendCount = 0
    private var backCount = 0
    private var dismissCount = 0

    private fun show(state: ForgotPasswordUiState) {
        rule.setContent {
            ClickMeTheme {
                ForgotPasswordContent(
                    state = state,
                    onEmailChange = {},
                    onSend = { sendCount++ },
                    onBack = { backCount++ },
                    onDismissDialog = { dismissCount++ },
                )
            }
        }
    }

    @Test
    fun rendersAllCopy() {
        show(ForgotPasswordUiState())
        rule.onNodeWithText("Reset Password").assertExists()
        rule.onNodeWithText("Enter your email to receive a password reset code").assertExists()
        // The visible "Email" label is hidden from TalkBack (clearAndSetSemantics), so this is the field placeholder.
        rule.onNodeWithText("Email").assertExists()
        rule.onNodeWithText("Send Reset Code").assertExists()
        rule.onNodeWithText("Check your inbox", substring = true).assertExists()
        rule.onNodeWithText("Back to Login").assertExists()
    }

    @Test
    fun prefilledEmailIsShown() {
        show(ForgotPasswordUiState(email = "a@b.co"))
        rule.onNodeWithText("a@b.co").assertExists()
    }

    @Test
    fun tappingSendCallsOnSend() {
        show(ForgotPasswordUiState())
        rule.onNode(hasText("Send Reset Code") and hasClickAction()).performClick()
        assertEquals(1, sendCount)
    }

    @Test
    fun backToLoginAndTopBarBackCallOnBack() {
        show(ForgotPasswordUiState())
        rule.onNodeWithText("Back to Login").performClick()
        assertEquals(1, backCount)
        rule.onNodeWithContentDescription("Back").performClick()
        assertEquals(2, backCount)
    }

    @Test
    fun savedStateShowsCodeSent() {
        show(ForgotPasswordUiState(email = "a@b.co", saveState = SaveState.Saved))
        rule.onNodeWithText("Code Sent!").assertExists()
    }

    @Test
    fun emailErrorIsShownInline() {
        show(
            ForgotPasswordUiState(
                emailError = FieldError.Res(R.string.error_forgot_email_required),
                didAttemptSend = true,
            ),
        )
        rule.onNodeWithText("Please enter your email").assertExists()
    }

    @Test
    fun dialogErrorShowsAndOkDismisses() {
        show(ForgotPasswordUiState(dialogError = FieldError.Text("Validation failed")))
        rule.onNodeWithText("Validation failed").assertExists()
        rule.onNodeWithText("OK").performClick()
        assertEquals(1, dismissCount)
    }

    @Test
    fun noDialogWhenNoError() {
        show(ForgotPasswordUiState())
        rule.onNodeWithText("Something went wrong").assertDoesNotExist()
    }
}
