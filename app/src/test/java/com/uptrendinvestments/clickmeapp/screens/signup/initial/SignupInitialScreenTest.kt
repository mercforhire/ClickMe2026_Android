package com.uptrendinvestments.clickmeapp.screens.signup.initial

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SignupInitialScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private var submitCount = 0
    private var backCount = 0
    private var dismissCount = 0

    private fun show(state: SignupInitialUiState) {
        rule.setContent {
            ClickMeTheme {
                SignupInitialContent(
                    state = state,
                    onEmailChange = {},
                    onPasswordChange = {},
                    onSubmit = { submitCount++ },
                    onBack = { backCount++ },
                    onDismissDialog = { dismissCount++ },
                )
            }
        }
    }

    @Test
    fun rendersHeadlineSubtitleFieldsHintAndContinue() {
        show(SignupInitialUiState())
        rule
            .onNodeWithText("Create Account")
            .assertExists()
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        rule.onNodeWithText("Start your journey with us.").assertExists()
        rule.onNodeWithText("Email").assertExists()
        rule.onNodeWithText("Password").assertExists()
        rule.onNodeWithText("At least 8 characters, with a letter and a number.").assertExists()
        rule.onNode(hasText("Continue") and hasClickAction()).assertExists()
    }

    @Test
    fun hasNoUsernamePhoneOrRoleControls() {
        show(SignupInitialUiState())
        rule.onAllNodes(hasText("Username", substring = true, ignoreCase = true)).assertCountEquals(0)
        rule.onAllNodes(hasText("Phone", substring = true, ignoreCase = true)).assertCountEquals(0)
        rule.onAllNodes(hasText("Role", substring = true, ignoreCase = true)).assertCountEquals(0)
    }

    @Test
    fun strengthMeterShowsOnlyOnceThePasswordIsNotEmpty() {
        show(SignupInitialUiState(password = ""))
        rule.onAllNodes(hasContentDescription("Password strength", substring = true)).assertCountEquals(0)
    }

    @Test
    fun strengthMeterIsShownForANonEmptyPassword() {
        show(SignupInitialUiState(password = "abcdefg1"))
        rule.onAllNodes(hasContentDescription("Password strength", substring = true)).assertCountEquals(1)
    }

    @Test
    fun passwordRuleErrorIsShownFromStringResource() {
        show(
            SignupInitialUiState(
                password = "abcdefgh",
                passwordError = FieldError.Res(R.string.error_signup_password_number),
            ),
        )
        rule.onNodeWithText("Password must include a number").assertExists()
    }

    @Test
    fun tappingContinueCallsOnSubmit() {
        show(SignupInitialUiState(email = "a@b.co", password = "abcdefg1"))
        rule.onNode(hasText("Continue") and hasClickAction()).performScrollTo().performClick()
        assertEquals(1, submitCount)
    }

    @Test
    fun continueStaysEnabledForAnInvalidForm() {
        show(SignupInitialUiState())
        rule.onNode(hasText("Continue") and hasClickAction()).assertIsEnabled()
    }

    @Test
    fun topBarBackCallsOnBack() {
        show(SignupInitialUiState())
        rule.onNodeWithContentDescription("Back").performClick()
        assertEquals(1, backCount)
    }

    @Test
    fun whileSubmittingBackIsDisabledAndContinueShowsSpinner() {
        show(SignupInitialUiState(email = "a@b.co", password = "abcdefg1", isSubmitting = true))
        rule.onNodeWithContentDescription("Back").assertIsNotEnabled()
        rule.onNodeWithText("Continue").assertDoesNotExist()
        rule.onNode(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Saving")).assertExists()
    }

    @Test
    fun dialogErrorShowsTitleMessageAndOkDismisses() {
        show(SignupInitialUiState(dialogError = FieldError.Text("Server unavailable")))
        rule.onNodeWithText("Couldn't create account").assertExists()
        rule.onNodeWithText("Server unavailable").assertExists()
        rule.onNodeWithText("OK").performClick()
        assertEquals(1, dismissCount)
    }
}
