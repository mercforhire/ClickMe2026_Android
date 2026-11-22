@file:OptIn(ExperimentalCoroutinesApi::class)

package com.uptrendinvestments.clickmeapp.screens.settings.deleteaccount

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.model.enums.AppMode
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.screens.settings.deleteaccount.subviews.DELETE_BLOCKER_BOOKINGS_TAG
import com.uptrendinvestments.clickmeapp.screens.settings.deleteaccount.subviews.DELETE_BLOCKER_DISMISS_TAG
import com.uptrendinvestments.clickmeapp.screens.settings.deleteaccount.subviews.DELETE_COMMENTS_TAG
import com.uptrendinvestments.clickmeapp.screens.settings.deleteaccount.subviews.DELETE_CONFIRM_TAG
import com.uptrendinvestments.clickmeapp.screens.settings.deleteaccount.subviews.DELETE_KEEP_TAG
import com.uptrendinvestments.clickmeapp.testing.FakeCallGuard
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.FakeSecureTokenStore
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

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
        DeleteAccountViewModel(
            DeleteAccountRepository(FakeClickMeApi(), FakeSecureTokenStore(), main.testDispatcher),
            guard,
            AccountDeletionState(),
            CoroutineScope(main.testDispatcher),
        )

    private val bookings = mutableListOf<Unit>()
    private val dismissedBlockers = mutableListOf<Unit>()

    private fun setScreen(viewModel: DeleteAccountViewModel) {
        rule.setContent {
            ClickMeTheme {
                DeleteAccountScreen(
                    onBack = { backs += Unit },
                    mode = AppMode.Client,
                    onShowBookings = { bookings += Unit },
                    viewModel = viewModel,
                )
            }
        }
    }

    private fun setContent(
        state: DeleteAccountUiState,
        mode: AppMode = AppMode.Client,
    ) {
        rule.setContent {
            ClickMeTheme {
                DeleteAccountContent(
                    state = state,
                    mode = mode,
                    onBack = { backs += Unit },
                    onContinue = {},
                    onBackToWarning = { toWarning += Unit },
                    onSelectReason = {},
                    onFeedbackChange = {},
                    onPasswordChange = {},
                    onConfirm = {},
                    onDismissDialog = {},
                    onDismissBlocker = { dismissedBlockers += Unit },
                    onShowBookings = { bookings += Unit },
                )
            }
        }
    }

    private fun systemBack() {
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.waitForIdle()
    }

    @Test
    fun farewellHeadlineFocused() {
        rule.setContent { ClickMeTheme { DeleteAccountFarewell(onBackToLogin = {}) } }
        rule.waitForIdle()

        rule.onNodeWithText("We're sorry to see you go!").assertIsFocused()
    }

    @Test
    @Config(qualifiers = "w900dp-h1200dp")
    fun capped() {
        setContent(DeleteAccountUiState(step = DeleteStep.Reason))

        // The content column and the pinned footer buttons are capped and centered.
        val comments = rule.onNodeWithTag(DELETE_COMMENTS_TAG).performScrollTo().getUnclippedBoundsInRoot()
        val confirm = rule.onNodeWithTag(DELETE_CONFIRM_TAG).getUnclippedBoundsInRoot()
        val keep = rule.onNodeWithTag(DELETE_KEEP_TAG).getUnclippedBoundsInRoot()
        listOf(comments, confirm, keep).forEach { bounds ->
            assertTrue("width ${bounds.right - bounds.left}", bounds.right - bounds.left <= 600.dp)
            assertTrue("left ${bounds.left}", bounds.left >= 150.dp)
        }
        // The app bar stays full width: the back arrow sits at the window's leading edge, not in the column.
        val back = rule.onNodeWithContentDescription("Back").getUnclippedBoundsInRoot()
        assertTrue("back left ${back.left}", back.left < 100.dp)
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
    fun appBarArrowOnReasonReturnsToWarningLikeSystemBack() {
        setContent(DeleteAccountUiState(step = DeleteStep.Reason))

        rule.onNodeWithContentDescription("Back").performClick()

        assertEquals(1, toWarning.size)
        assertEquals(0, backs.size)
    }

    @Test
    fun appBarArrowOnWarningLeavesTheFlow() {
        setContent(DeleteAccountUiState())

        rule.onNodeWithContentDescription("Back").performClick()

        assertEquals(1, backs.size)
        assertEquals(0, toWarning.size)
    }

    @Test
    fun deletingSpinnerHasASpokenLabel() {
        setContent(
            DeleteAccountUiState(
                step = DeleteStep.Reason,
                reason = DeleteAccountReason.Other,
                password = "pw1",
                isDeleting = true,
            ),
        )

        rule.onNodeWithContentDescription("Deleting account").assertExists()
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

    private fun text(id: Int) = rule.activity.getString(id)

    @Test
    fun confirmationStepShowsTheTrueDisclosureAndTheSavedCardsLine() {
        setContent(DeleteAccountUiState(step = DeleteStep.Warning))

        listOf(
            R.string.delete_account_permanence,
            R.string.delete_account_bookings_first,
            R.string.delete_account_deleted_title,
            R.string.delete_account_deleted_body,
            R.string.delete_account_kept_anonymized_title,
            R.string.delete_account_kept_anonymized_body,
            R.string.delete_account_kept_title,
            R.string.delete_account_kept_body,
            R.string.delete_account_saved_cards_note,
        ).forEach { rule.onNodeWithText(text(it)).assertExists() }
    }

    @Test
    fun reasonStepKeepsOnlyThePermanenceLine() {
        setContent(DeleteAccountUiState(step = DeleteStep.Reason))

        rule.onNodeWithText(text(R.string.delete_account_permanence)).assertExists()
        listOf(
            R.string.delete_account_bookings_first,
            R.string.delete_account_deleted_title,
            R.string.delete_account_kept_anonymized_title,
            R.string.delete_account_kept_title,
            R.string.delete_account_saved_cards_note,
        ).forEach { rule.onAllNodesWithText(text(it)).assertCountEquals(0) }
    }

    @Test
    fun disclosureStringsAreTheIosWordsExactly() {
        // iOS AccountDeletionCopy, final after plan 08-13 (D-23, option-a).
        assertEquals(
            "Deleting your account is permanent and can't be undone.",
            text(R.string.delete_account_permanence),
        )
        assertEquals(
            "Cards you added to pay for sessions are not deleted with your account.",
            text(R.string.delete_account_saved_cards_note),
        )
        assertEquals(
            "Your past bookings stay on record, shown as from \"Deleted user\". " +
                "Your private notes on them are erased.",
            text(R.string.delete_account_kept_anonymized_body),
        )
        assertEquals(
            "Messages and reviews you've written stay visible to the other person, shown as from " +
                "\"Deleted user\". Payment records are kept.",
            text(R.string.delete_account_kept_body),
        )
    }

    @Test
    fun noDeleteAccountStringMakesAFalseRemovalClaim() {
        val all =
            R.string::class.java.fields
                .filter { it.name.startsWith("delete_account_") }
                .map { text(it.getInt(null)) }
        assertTrue("found ${all.size} delete-account strings", all.size > 20)

        val forbidden = listOf("payment details", "removed", "saved card")
        for (value in all) {
            for (phrase in forbidden) {
                assertFalse("\"$value\" contains \"$phrase\"", value.contains(phrase, ignoreCase = true))
            }
        }
        val joined = all.joinToString(" ")
        listOf("visible to the other person", "Deleted user", "upcoming").forEach {
            assertTrue("missing \"$it\"", joined.contains(it))
        }
    }

    @Test
    fun blockerDialogShowsTheTodoTextAndButtonsForEveryCodeAndMode() {
        data class Case(
            val blocker: AccountDeletionBlocker,
            val mode: AppMode,
            val message: String,
            val bookingsButton: Boolean,
        )
        val cases =
            listOf(
                Case(
                    AccountDeletionBlocker.ExpertUpcoming,
                    AppMode.Expert,
                    "You have upcoming bookings as an expert. Cancel or finish them, then try again.",
                    true,
                ),
                Case(
                    AccountDeletionBlocker.ExpertUpcoming,
                    AppMode.Client,
                    "You have upcoming bookings as an expert. Switch to Expert mode to cancel or finish them, " +
                        "then try again.",
                    false,
                ),
                Case(
                    AccountDeletionBlocker.ClientUpcoming,
                    AppMode.Client,
                    "You have upcoming sessions booked. Cancel them, then try again.",
                    true,
                ),
                Case(
                    AccountDeletionBlocker.ClientUpcoming,
                    AppMode.Expert,
                    "You have upcoming sessions booked as a client. Switch to Client mode to cancel them, " +
                        "then try again.",
                    false,
                ),
                Case(
                    AccountDeletionBlocker.SessionInProgress,
                    AppMode.Client,
                    "A session is in progress or starts within 15 minutes. Wait until it ends, then try again.",
                    true,
                ),
                Case(
                    AccountDeletionBlocker.SessionInProgress,
                    AppMode.Expert,
                    "A session is in progress or starts within 15 minutes. Wait until it ends, then try again.",
                    true,
                ),
            )
        val current = mutableStateOf(cases.first())
        rule.setContent {
            ClickMeTheme {
                DeleteAccountContent(
                    state = DeleteAccountUiState(step = DeleteStep.Reason, blocker = current.value.blocker),
                    mode = current.value.mode,
                    onBack = {},
                    onContinue = {},
                    onBackToWarning = {},
                    onSelectReason = {},
                    onFeedbackChange = {},
                    onPasswordChange = {},
                    onConfirm = {},
                    onDismissDialog = {},
                    onDismissBlocker = {},
                    onShowBookings = {},
                )
            }
        }
        for (case in cases) {
            val label = "${case.blocker} in ${case.mode}"
            rule.runOnUiThread { current.value = case }
            rule.waitForIdle()
            rule.onNodeWithText("You can't delete your account yet").assertExists(label)
            rule.onNodeWithText(case.message).assertExists(label)
            rule.onNodeWithTag(DELETE_BLOCKER_DISMISS_TAG).assertExists(label)
            rule
                .onAllNodes(hasTestTag(DELETE_BLOCKER_BOOKINGS_TAG))
                .assertCountEquals(if (case.bookingsButton) 1 else 0)
            rule.onAllNodesWithText("Couldn't delete account").assertCountEquals(0)
        }
    }

    @Test
    fun blockerButtonsCallTheirCallbacks() {
        setContent(DeleteAccountUiState(step = DeleteStep.Reason, blocker = AccountDeletionBlocker.ClientUpcoming))

        rule.onNodeWithText("View My Bookings").performClick()
        assertEquals(1, bookings.size)

        rule.onNodeWithText("Got it").performClick()
        assertEquals(1, dismissedBlockers.size)
    }

    @Test
    fun deleteConfirmIsTheOnlyConfirmControl() {
        setContent(DeleteAccountUiState(step = DeleteStep.Reason))
        rule.onAllNodes(hasTestTag("delete_confirm")).assertCountEquals(1)
        rule.onAllNodes(hasTestTag("delete_continue")).assertCountEquals(0)
    }
}
