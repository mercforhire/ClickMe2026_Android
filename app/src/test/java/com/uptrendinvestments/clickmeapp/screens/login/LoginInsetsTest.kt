package com.uptrendinvestments.clickmeapp.screens.login

import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.performScrollTo
import com.uptrendinvestments.clickmeapp.testing.dispatchInsets
import com.uptrendinvestments.clickmeapp.testing.imeInsets
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** POL-03: the Log in button stays above the keyboard, and the form column is capped on a large window (D-02). */
@RunWith(RobolectricTestRunner::class)
class LoginInsetsTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var view: View

    private fun show() {
        rule.setContent {
            view = LocalView.current
            ClickMeTheme {
                LoginContent(
                    state = LoginUiState(),
                    onEmailChange = {},
                    onPasswordChange = {},
                    onSubmit = {},
                    onForgotPassword = {},
                    onSignUp = {},
                    onDismissDialog = {},
                )
            }
        }
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp")
    fun logInButtonStaysAboveIme() {
        show()
        rule.dispatchInsets(view, imeInsets(bottomPx = 300))
        val button = rule.onNode(hasText("Login") and hasClickAction())
        // The form scrolls inside the IME-shrunk viewport; scroll the button into it, then measure.
        button.performScrollTo()
        val bottom = button.getUnclippedBoundsInRoot().bottom.value
        assertTrue("Log in bottom $bottom dp must be at or above the IME top (500 dp)", bottom <= 500f)
    }

    @Test
    @Config(qualifiers = "w900dp-h600dp")
    fun contentCappedOnLargeWindow() {
        show()
        val bounds = rule.onAllNodes(hasSetTextAction())[0].getUnclippedBoundsInRoot()
        val width = bounds.right.value - bounds.left.value
        assertTrue("Email field is $width dp wide, expected <= 600", width <= 600f)
    }
}
