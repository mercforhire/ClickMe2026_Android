package com.uptrendinvestments.clickmeapp.screens.login

import android.content.Context
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.testing.assertActionablesNamed
import com.uptrendinvestments.clickmeapp.testing.assertIsHeading
import com.uptrendinvestments.clickmeapp.testing.assertTouchTargets
import com.uptrendinvestments.clickmeapp.testing.assertTraversalOrder
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** SC-4 flow 1 (log in): the TalkBack contract of UI-SPEC "Flow 1", asserted on the semantics tree (POL-04). */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class LoginTalkBackFlowTest {
    @get:Rule
    val rule = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private var submitCount = 0
    private var signUpCount = 0

    private fun str(id: Int) = context.getString(id)

    private fun show(state: LoginUiState) {
        rule.setContent {
            ClickMeTheme {
                LoginContent(
                    state = state,
                    onEmailChange = {},
                    onPasswordChange = {},
                    onSubmit = { submitCount++ },
                    onForgotPassword = {},
                    onSignUp = { signUpCount++ },
                    onDismissDialog = {},
                )
            }
        }
    }

    @Test
    fun readingOrder() {
        show(LoginUiState())
        rule.assertTraversalOrder(
            str(R.string.login_wordmark),
            str(R.string.login_tagline),
            str(R.string.login_email_placeholder),
            str(R.string.login_password_placeholder),
            str(R.string.a11y_show_password),
            str(R.string.login_forgot_password),
            str(R.string.login_submit),
            "${str(R.string.login_no_account)} ${str(R.string.login_sign_up)}",
        )
    }

    @Test
    fun wordmarkIsHeading() {
        show(LoginUiState())
        rule.assertIsHeading(str(R.string.login_wordmark))
    }

    @Test
    fun signUpIsOneNode() {
        show(LoginUiState())
        val signUpNodes = rule.onAllNodes(hasClickAction() and hasText(str(R.string.login_sign_up)))
        assertEquals(1, signUpNodes.fetchSemanticsNodes().size)
        rule.onNode(hasClickAction() and hasText(str(R.string.login_no_account))).assertExists()
        rule.onNode(hasClickAction() and hasText(str(R.string.login_sign_up))).performScrollTo().performClick()
        assertEquals(1, signUpCount)
    }

    @Test
    fun everythingNamedAndLargeEnough() {
        show(LoginUiState())
        rule.assertActionablesNamed()
        rule.assertTouchTargets()
    }

    @Test
    fun busyIsInProgress() {
        show(LoginUiState(email = "a@b.co", password = "password1", isSubmitting = true))
        val busy =
            rule.onNode(
                SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, str(R.string.a11y_in_progress)),
            )
        busy.assertExists()
        busy.performClick()
        assertEquals(0, submitCount)
    }

    @Test
    fun wrongPasswordFocusesField() {
        show(LoginUiState(email = "a@b.co", password = "password1", passwordError = FieldError.Text("Wrong")))
        val field = rule.onNode(hasSetTextAction() and SemanticsMatcher.keyIsDefined(SemanticsProperties.Error))
        field.assertExists()
        field.assertIsFocused()
        rule.onNodeWithText("Wrong").assertExists()
    }
}
