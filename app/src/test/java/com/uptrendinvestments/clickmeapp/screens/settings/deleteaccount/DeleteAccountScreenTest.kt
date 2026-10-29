@file:OptIn(ExperimentalCoroutinesApi::class)

package com.uptrendinvestments.clickmeapp.screens.settings.deleteaccount

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.testing.FakeCallGuard
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DeleteAccountScreenTest {
    @get:Rule(order = 0)
    val main = MainDispatcherRule(UnconfinedTestDispatcher())

    @get:Rule(order = 1)
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val guard = FakeCallGuard()
    private val backs = mutableListOf<Unit>()
    private val toWarning = mutableListOf<Unit>()

    private fun viewModel() =
        DeleteAccountViewModel(DeleteAccountRepository(FakeClickMeApi()), guard, AccountDeletionState())

    private fun setScreen(viewModel: DeleteAccountViewModel) {
        rule.setContent {
            ClickMeTheme { DeleteAccountScreen(onBack = { backs += Unit }, viewModel = viewModel) }
        }
    }

    private fun setContent(state: DeleteAccountUiState) {
        rule.setContent {
            ClickMeTheme {
                DeleteAccountContent(
                    state = state,
                    onBack = { backs += Unit },
                    onContinue = {},
                    onBackToWarning = { toWarning += Unit },
                    onSelectReason = {},
                    onFeedbackChange = {},
                    onPasswordChange = {},
                    onConfirm = {},
                    onDismissDialog = {},
                )
            }
        }
    }

    private fun systemBack() {
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.waitForIdle()
    }

    @Test
    fun warningShowsNoPauseCardAndGuardsContinue() {
        val vm = viewModel()
        setScreen(vm)

        rule.onNodeWithText("Are you sure you want to delete your account?").assertExists()
        rule.onNodeWithTag("delete_keep").assertExists()
        rule.onNodeWithTag("delete_continue").assertExists()
        rule.onAllNodesWithText("Pause Account").assertCountEquals(0)
        rule.onAllNodesWithText("Help us improve").assertCountEquals(0)

        rule.onNodeWithTag("delete_continue").performClick()

        rule.onNodeWithText("Help us improve").assertExists()
        rule.onNodeWithText("What is your primary reason for leaving?").assertExists()
        rule.onAllNodesWithText("Pause Account").assertCountEquals(0)
    }

    @Test
    fun blockedGuardKeepsTheWarningForm() {
        val vm = viewModel()
        guard.blocked = true
        setScreen(vm)

        rule.onNodeWithTag("delete_continue").performClick()

        rule.onAllNodesWithText("Help us improve").assertCountEquals(0)
        assertEquals(DeleteStep.Warning, vm.uiState.value.step)
    }

    @Test
    fun reasonFormEnablesDeleteOnlyWhenComplete() {
        val vm = viewModel()
        setScreen(vm)
        rule.onNodeWithTag("delete_continue").performClick()

        rule.onNodeWithTag("delete_confirm").assertIsNotEnabled()

        rule.onNodeWithTag("delete_reason_other").performScrollTo().performClick()
        rule.onNodeWithTag("delete_confirm").assertIsNotEnabled()

        vm.onPasswordChange("pw1")
        rule.waitForIdle()
        rule.onNodeWithTag("delete_confirm").assertIsEnabled()
    }

    @Test
    fun reasonRowsAreRadioButtonsWithNoneSelected() {
        val vm = viewModel()
        setScreen(vm)
        rule.onNodeWithTag("delete_continue").performClick()

        listOf("too_expensive", "found_alternative", "privacy_concerns", "other").forEach { wire ->
            rule
                .onNodeWithTag("delete_reason_$wire")
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, false))
        }

        rule.onNodeWithTag("delete_reason_privacy_concerns").performScrollTo().performClick()

        rule.onNodeWithTag("delete_reason_privacy_concerns").assertIsSelected()
        rule
            .onNodeWithTag("delete_reason_other")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, false))
    }

    @Test
    fun keepMyAccountOnWarningCallsOnBack() {
        setContent(DeleteAccountUiState())

        rule.onNodeWithTag("delete_keep").performClick()

        assertEquals(1, backs.size)
    }

    @Test
    fun backIgnoredInFlight() {
        setContent(
            DeleteAccountUiState(
                step = DeleteStep.Reason,
                reason = DeleteAccountReason.Other,
                password = "pw1",
                isDeleting = true,
            ),
        )

        rule.onNodeWithContentDescription("Back").assertIsNotEnabled()
        rule.onNodeWithTag("delete_confirm").assertIsNotEnabled()
        rule.onNodeWithTag("delete_keep").assertIsNotEnabled()

        systemBack()

        assertEquals(0, backs.size)
        assertEquals(0, toWarning.size)
    }

    @Test
    fun systemBackOnReasonReturnsToWarning() {
        setContent(DeleteAccountUiState(step = DeleteStep.Reason))

        systemBack()

        assertEquals(1, toWarning.size)
        assertEquals(0, backs.size)
    }

    @Test
    fun counterAppearsFrom800() {
        setContent(DeleteAccountUiState(step = DeleteStep.Reason, feedback = "a".repeat(799)))
        rule.onAllNodesWithText("799/1000").assertCountEquals(0)
        rule.onAllNodesWithText("800/1000").assertCountEquals(0)
    }

    @Test
    fun counterShowsAtEightHundred() {
        setContent(DeleteAccountUiState(step = DeleteStep.Reason, feedback = "a".repeat(800)))
        rule.onNodeWithText("800/1000").assertExists()
    }

    @Test
    fun errorsRenderUnderPasswordAndInDialog() {
        setContent(
            DeleteAccountUiState(
                step = DeleteStep.Reason,
                passwordError = FieldError.Text("Invalid password."),
                dialog = FieldError.Text("Finish your sessions first."),
            ),
        )

        rule.onNodeWithText("Invalid password.").assertExists()
        rule.onNodeWithText("Couldn't delete account").assertExists()
        rule.onNodeWithText("Finish your sessions first.").assertExists()
        rule.onNodeWithText("Got it").assertExists()
    }

    @Test
    fun deleteConfirmIsTheOnlyConfirmControl() {
        setContent(DeleteAccountUiState(step = DeleteStep.Reason))
        rule.onAllNodes(hasTestTag("delete_confirm")).assertCountEquals(1)
        rule.onAllNodes(hasTestTag("delete_continue")).assertCountEquals(0)
    }
}
