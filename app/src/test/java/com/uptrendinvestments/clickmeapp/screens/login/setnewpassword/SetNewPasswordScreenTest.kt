package com.uptrendinvestments.clickmeapp.screens.login.setnewpassword

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescriptionExactly
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
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
class SetNewPasswordScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private fun show(
        state: SetNewPasswordUiState = SetNewPasswordUiState(email = "a@b.co"),
        onSubmit: () -> Unit = {},
        onBack: () -> Unit = {},
        onDismissDialog: () -> Unit = {},
    ) {
        rule.setContent {
            ClickMeTheme {
                SetNewPasswordContent(
                    state = state,
                    onCodeChange = {},
                    onNewPasswordChange = {},
                    onConfirmChange = {},
                    onSubmit = onSubmit,
                    onBack = onBack,
                    onDismissDialog = onDismissDialog,
                )
            }
        }
    }

    @Test
    fun rendersLabelsPlaceholdersAndSubmitButton() {
        show()
        rule.onNodeWithText("Verification Code", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Enter 6-digit code").assertIsDisplayed()
        rule.onNodeWithText("New Password", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Enter your new password").assertIsDisplayed()
        rule.onNodeWithText("Confirm New Password", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Confirm your new password").assertIsDisplayed()
        // Title in the top bar plus the button label.
        rule.onAllNodesWithText("Set New Password").assertCountEquals(2)
    }

    @Test
    fun strengthMeter_hiddenWhenPasswordEmpty() {
        show(SetNewPasswordUiState(newPassword = ""))
        rule.onAllNodesWithContentDescription("Password strength", substring = true).assertCountEquals(0)
    }

    @Test
    fun strengthMeter_showsLevelForNonEmptyPassword() {
        show(SetNewPasswordUiState(newPassword = "Abcdefgh1!"))
        rule.onNodeWithContentDescription("Password strength: Strong").assertIsDisplayed()
    }

    @Test
    fun matchBadge_shownOnlyWhenPasswordsMatch() {
        show(SetNewPasswordUiState(newPassword = "abcdefg1!", confirmPassword = "abcdefg1!"))
        rule.onNodeWithContentDescription("Passwords match").assertExists()
    }

    @Test
    fun matchBadge_absentWhenPasswordsDiffer() {
        show(SetNewPasswordUiState(newPassword = "abcdefg1!", confirmPassword = "abcdefg2!"))
        rule.onAllNodesWithContentDescription("Passwords match").assertCountEquals(0)
    }

    @Test
    fun eachPasswordFieldHasItsOwnToggle() {
        show()
        rule.onAllNodesWithContentDescription("Show password").assertCountEquals(2)
    }

    @Test
    fun expiredCodeError_isShownInline() {
        show(SetNewPasswordUiState(codeError = FieldError.Res(R.string.error_code_expired)))
        rule.onNodeWithText("Invalid or expired code. Please request a new one.").assertIsDisplayed()
    }

    @Test
    fun savedState_showsPasswordUpdated() {
        show(SetNewPasswordUiState(saveState = SaveState.Saved))
        rule.onNodeWithText("Password Updated!").assertIsDisplayed()
    }

    @Test
    fun tappingButton_callsSubmit() {
        var submits = 0
        show(onSubmit = { submits++ })
        rule.onNode(hasText("Set New Password") and hasClickAction()).performClick()
        assertEquals(1, submits)
    }

    @Test
    fun topBarBack_callsOnBack() {
        var backs = 0
        show(onBack = { backs++ })
        rule.onNode(hasContentDescriptionExactly("Back")).performClick()
        assertEquals(1, backs)
    }

    @Test
    fun dialogError_showsDialogAndOkDismisses() {
        var dismissed = 0
        show(
            SetNewPasswordUiState(dialogError = FieldError.Text("Email is invalid")),
            onDismissDialog = { dismissed++ },
        )
        rule.onNodeWithText("Email is invalid").assertIsDisplayed()
        rule.onNodeWithText("OK").performClick()
        assertEquals(1, dismissed)
    }
}
