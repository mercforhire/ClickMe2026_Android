package com.uptrendinvestments.clickmeapp.screens.settings.accountsecurity

import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasContentDescriptionExactly
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.model.responses.SuccessMessageResponse
import com.uptrendinvestments.clickmeapp.screens.settings.accountsecurity.subviews.ACCOUNT_SECURITY_DELETE_TAG
import com.uptrendinvestments.clickmeapp.screens.settings.accountsecurity.subviews.ACCOUNT_SECURITY_UPDATE_TAG
import com.uptrendinvestments.clickmeapp.testing.FakeCallGuard
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class AccountSecurityScreenTest {
    @get:Rule(order = 0)
    val main = MainDispatcherRule(UnconfinedTestDispatcher())

    @get:Rule(order = 1)
    val rule = createComposeRule()

    private val api = FakeClickMeApi()
    private val guard = FakeCallGuard()
    private var backs = 0
    private var navBacks = 0
    private var deleteFlowOpens = 0
    private var dispatcher: OnBackPressedDispatcher? = null

    init {
        api.updatePasswordHandler = { SuccessMessageResponse(status = "success") }
    }

    private fun show() {
        val vm = AccountSecurityViewModel(AccountSecurityRepository(api), guard)
        rule.setContent {
            ClickMeTheme {
                dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
                // An outer handler stands in for NavDisplay's: it fires only when the screen does not swallow Back.
                BackHandler { navBacks++ }
                val state by vm.uiState.collectAsState()
                AccountSecurityContent(
                    state = state,
                    onCurrentChange = vm::onCurrentChange,
                    onNewChange = vm::onNewChange,
                    onConfirmChange = vm::onConfirmChange,
                    onSubmit = vm::submit,
                    onDeleteAccount = { if (vm.requestDeleteAccount()) deleteFlowOpens++ },
                    onDismissDialog = vm::dismissDialog,
                    onBack = { backs++ },
                )
            }
        }
    }

    private fun fillValidForm(new: String = "new2pass") {
        rule.onNodeWithText("Enter your current password").performTextInput("old1pass")
        rule.onNodeWithText("Enter your new password").performTextInput(new)
        rule.onNodeWithText("Confirm your new password").performTextInput(new)
    }

    @Test
    fun successDialogShowsAndDismisses() {
        show()
        fillValidForm()

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

    @Test
    fun deleteButtonOpensFlowOnlyWhenAllowed() {
        show()
        rule.onNodeWithTag(ACCOUNT_SECURITY_DELETE_TAG).performScrollTo().performClick()
        assertEquals(1, deleteFlowOpens)

        guard.blocked = true
        rule.onNodeWithTag(ACCOUNT_SECURITY_DELETE_TAG).performClick()
        assertEquals(1, deleteFlowOpens)
        assertEquals(2, guard.attempts.size)
    }

    @Test
    fun validationMessageShowsUnderItsField() {
        show()
        rule.onNodeWithTag(ACCOUNT_SECURITY_UPDATE_TAG).performScrollTo().performClick()

        rule.onNodeWithText("Please enter your current password.").assertExists()
        assertEquals(0, api.updatePasswordCalls)
    }

    @Test
    fun backIsIgnoredWhileSavingAndRepeatTapsSendNothing() {
        val gate = CompletableDeferred<SuccessMessageResponse>()
        api.updatePasswordHandler = { gate.await() }
        show()
        fillValidForm()

        rule.onNodeWithTag(ACCOUNT_SECURITY_UPDATE_TAG).performScrollTo().performClick()

        rule
            .onNodeWithTag(ACCOUNT_SECURITY_UPDATE_TAG)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Saving"))
        rule.onNodeWithTag(ACCOUNT_SECURITY_UPDATE_TAG).performClick()
        assertEquals(1, api.updatePasswordCalls)

        rule.onNode(hasContentDescriptionExactly("Back")).assertIsNotEnabled().performClick()
        rule.runOnIdle { checkNotNull(dispatcher).onBackPressed() }
        rule.waitForIdle()
        assertEquals(0, backs)
        assertEquals(0, navBacks)

        gate.complete(SuccessMessageResponse(status = "success"))
        rule.waitForIdle()
        rule.onNodeWithText("Password updated").assertIsDisplayed()
    }

    @Test
    fun systemBackReachesNavigationWhenIdle() {
        show()

        rule.runOnIdle { checkNotNull(dispatcher).onBackPressed() }
        rule.waitForIdle()

        assertEquals(1, navBacks)
    }

    @Test
    fun strengthMeterFollowsNewPassword() {
        show()
        rule.onAllNodesWithContentDescription("Password strength", substring = true).assertCountEquals(0)

        rule.onNodeWithText("Enter your new password").performTextInput("abcdefgh1A!")
        rule.onNodeWithContentDescription("Password strength: Strong").assertIsDisplayed()

        // The placeholder is gone once the field has text, so address the second text field by position.
        rule.onAllNodes(hasSetTextAction())[1].performTextClearance()
        rule.waitForIdle()
        rule.onAllNodesWithContentDescription("Password strength", substring = true).assertCountEquals(0)
    }

    @Test
    fun passwordFieldsOfferShowAndHideToggles() {
        show()

        rule.onAllNodesWithContentDescription("Show password").assertCountEquals(3)
        rule.onAllNodesWithContentDescription("Show password")[0].performClick()
        rule.onAllNodesWithContentDescription("Hide password").assertCountEquals(1)
    }

    @Test
    fun copyMatchesIos() {
        val app = RuntimeEnvironment.getApplication()
        val expected =
            mapOf(
                R.string.account_security_error_current_empty to "Please enter your current password.",
                R.string.account_security_error_new_empty to "Please enter a new password.",
                R.string.account_security_error_new_short to "Password must be at least 8 characters.",
                R.string.account_security_error_new_no_number to "Password must include a number.",
                R.string.account_security_error_new_same to
                    "New password must be different from your current password.",
                R.string.account_security_error_confirm_empty to "Please confirm your new password.",
                R.string.account_security_error_confirm_mismatch to "Passwords do not match.",
                R.string.account_security_wrong_current to "Current password is incorrect.",
                R.string.account_security_too_many to "Too many attempts. Please try again in a moment.",
                R.string.account_security_failed_title to "Couldn't update password",
                R.string.account_security_failed_body to "Couldn't update your password. Please try again.",
            )
        expected.forEach { (id, text) -> assertEquals(text, app.getString(id)) }
    }
}
