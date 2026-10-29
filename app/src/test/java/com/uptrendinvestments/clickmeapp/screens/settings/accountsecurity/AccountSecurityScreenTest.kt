package com.uptrendinvestments.clickmeapp.screens.settings.accountsecurity

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.uptrendinvestments.clickmeapp.model.responses.SuccessMessageResponse
import com.uptrendinvestments.clickmeapp.screens.settings.accountsecurity.subviews.ACCOUNT_SECURITY_UPDATE_TAG
import com.uptrendinvestments.clickmeapp.testing.FakeCallGuard
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class AccountSecurityScreenTest {
    @get:Rule(order = 0)
    val main = MainDispatcherRule(UnconfinedTestDispatcher())

    @get:Rule(order = 1)
    val rule = createComposeRule()

    private val api = FakeClickMeApi()
    private val guard = FakeCallGuard()

    init {
        api.updatePasswordHandler = { SuccessMessageResponse(status = "success") }
    }

    private fun show() {
        val vm = AccountSecurityViewModel(AccountSecurityRepository(api), guard)
        rule.setContent {
            ClickMeTheme {
                val state by vm.uiState.collectAsState()
                AccountSecurityContent(
                    state = state,
                    onCurrentChange = vm::onCurrentChange,
                    onNewChange = vm::onNewChange,
                    onConfirmChange = vm::onConfirmChange,
                    onSubmit = vm::submit,
                    onDeleteAccount = {},
                    onDismissDialog = vm::dismissDialog,
                    onBack = {},
                )
            }
        }
    }

    @Test
    fun successDialogShowsAndDismisses() {
        show()
        rule.onNodeWithText("Enter your current password").performTextInput("old1pass")
        rule.onNodeWithText("Enter your new password").performTextInput("new2pass")
        rule.onNodeWithText("Confirm your new password").performTextInput("new2pass")

        rule.onNodeWithTag(ACCOUNT_SECURITY_UPDATE_TAG).performScrollTo().performClick()

        rule.onNodeWithText("Password updated").assertIsDisplayed()
        rule.onNodeWithText("Your password has been changed.").assertIsDisplayed()
        assertEquals(1, api.updatePasswordCalls)

        rule.onNodeWithText("Got it").performClick()

        assertTrue(rule.onAllNodesWithText("Password updated").fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun screenShowsBothCardsInOrder() {
        show()

        fun top(text: String) =
            rule
                .onAllNodesWithText(text, useUnmergedTree = true)[0]
                .fetchSemanticsNode()
                .positionInRoot.y

        val order =
            listOf(
                top("Change Password"),
                top("Your new password must be at least 8 characters long and include numbers."),
                top("Current Password"),
                top("New Password"),
                top("Confirm New Password"),
                top("Delete Account"),
            )
        assertEquals(order.sorted(), order)
        rule.onNodeWithTag(ACCOUNT_SECURITY_UPDATE_TAG).assertExists()
        rule.onNodeWithText("Once you delete your account, there is no going back. Please be certain.").assertExists()
        assertEquals(2, rule.onAllNodesWithText("Delete Account").fetchSemanticsNodes().size)
    }
}
