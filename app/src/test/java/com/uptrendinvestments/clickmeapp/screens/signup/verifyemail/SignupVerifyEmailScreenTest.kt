package com.uptrendinvestments.clickmeapp.screens.signup.verifyemail

import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// A tall screen so the whole centered column is on screen; the real screen scrolls.
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h1200dp-xhdpi")
class SignupVerifyEmailScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private fun show(
        state: SignupVerifyEmailUiState,
        onSend: () -> Unit = {},
        onResend: () -> Unit = {},
        onDone: () -> Unit = {},
    ) {
        rule.setContent {
            ClickMeTheme {
                SignupVerifyEmailContent(
                    state = state,
                    onSend = onSend,
                    onResend = onResend,
                    onCodeChange = {},
                    onVerify = {},
                    onDone = onDone,
                    onBack = {},
                    onDismissDialog = {},
                )
            }
        }
    }

    private val before = SignupVerifyEmailUiState(email = "ada@b.co")
    private val after = before.copy(codeSent = true, resendSecondsRemaining = 12)

    @Test
    fun beforeSend_showsCopyEmailAndSendButton_noCodeFieldNoResend() {
        var sends = 0
        show(before, onSend = { sends++ })

        rule.onNodeWithText("Verify Email").assertIsDisplayed()
        rule.onNodeWithText("Verify Your Email Address").assertIsDisplayed()
        rule
            .onNodeWithText("Tap the button below to send a 6-digit verification code to your email address.")
            .assertIsDisplayed()
        rule.onNodeWithText("ada@b.co").assertIsDisplayed()
        rule.onNodeWithText("Enter 6-digit code").assertDoesNotExist()
        rule.onNodeWithText("Resend code").assertDoesNotExist()
        rule.onNode(hasText("Send Verification Email") and hasClickAction()).performClick()
        assertEquals(1, sends)
    }

    @Test
    fun afterSend_showsCodeFieldVerifyAndDisabledCountdown() {
        show(after)

        rule
            .onNodeWithText("We've sent a 6-digit code to your email address. Enter it below to verify.")
            .assertIsDisplayed()
        rule.onNodeWithText("Enter 6-digit code").assertIsDisplayed()
        rule.onNode(hasText("Verify") and hasClickAction()).assertIsDisplayed()
        rule.onNodeWithText("Resend in 12s").assertIsNotEnabled()
        rule.onNodeWithText("Send Verification Email").assertDoesNotExist()
    }

    @Test
    fun atZero_resendIsEnabledAndCallsOnResend() {
        var resends = 0
        show(after.copy(resendSecondsRemaining = 0), onResend = { resends++ })

        rule.onNodeWithText("Resend code").assertIsEnabled().performClick()

        assertEquals(1, resends)
    }

    @Test
    fun atZero_announcesThatResendIsAvailable_politely() {
        show(after.copy(resendSecondsRemaining = 0))

        rule
            .onNode(
                hasText("You can resend the code") and
                    SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite),
            ).assertExists()
    }

    @Test
    fun whileCounting_noAnnouncementNode() {
        show(after)

        rule.onNodeWithText("You can resend the code").assertDoesNotExist()
    }

    @Test
    fun codeError_isShownUnderTheField() {
        show(after.copy(codeError = FieldError.Res(R.string.error_code_expired)))

        rule.onNodeWithText("Invalid or expired code. Please request a new one.").assertIsDisplayed()
    }

    @Test
    fun verified_showsDoneVariantWithoutSendOrCodeUi() {
        var dones = 0
        show(before.copy(verified = true), onDone = { dones++ })

        rule.onNodeWithText("Email Verified").assertIsDisplayed()
        rule.onNodeWithText("Your email address is confirmed.").assertIsDisplayed()
        rule.onNodeWithText("ada@b.co").assertIsDisplayed()
        rule.onNodeWithText("Send Verification Email").assertDoesNotExist()
        rule.onNodeWithText("Enter 6-digit code").assertDoesNotExist()
        rule.onNode(hasText("Done") and hasClickAction()).performClick()
        assertEquals(1, dones)
    }

    @Test
    fun sendError_showsDialogWithTheSendTitle() {
        show(
            before.copy(
                dialogError = FieldError.Text("Server down"),
                dialogTitleRes = R.string.signup_verify_error_send_title,
            ),
        )

        rule.onNodeWithText("Couldn't send code").assertIsDisplayed()
        rule.onNodeWithText("Server down").assertIsDisplayed()
    }
}
