package com.uptrendinvestments.clickmeapp.screens.signup.overview

import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.uptrendinvestments.clickmeapp.screens.signup.SetupStep
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import androidx.compose.ui.test.assertCountEquals as assertCount

@RunWith(RobolectricTestRunner::class)
class SignupOverviewScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private fun rows(vararg completed: SetupStep) = SetupStep.entries.map { SetupStepRow(it, it in completed) }

    private fun state(
        completed: List<SetupStep> = emptyList(),
        isRefreshing: Boolean = false,
        refreshFailed: Boolean = false,
        showLogoutConfirm: Boolean = false,
    ) = SignupOverviewUiState(
        rows = rows(*completed.toTypedArray()),
        completedCount = completed.size,
        isRefreshing = isRefreshing,
        refreshFailed = refreshFailed,
        showLogoutConfirm = showLogoutConfirm,
    )

    private fun show(
        state: SignupOverviewUiState = state(),
        onOpenStep: (SetupStep) -> Unit = {},
        onNext: () -> Unit = {},
        onRefresh: () -> Unit = {},
        onRefreshErrorShown: () -> Unit = {},
        onRequestLogout: () -> Unit = {},
        onDismissLogout: () -> Unit = {},
        onConfirmLogout: () -> Unit = {},
    ) {
        rule.setContent {
            ClickMeTheme {
                SignupOverviewContent(
                    state = state,
                    onOpenStep = onOpenStep,
                    onNext = onNext,
                    onRefresh = onRefresh,
                    onRefreshErrorShown = onRefreshErrorShown,
                    onRequestLogout = onRequestLogout,
                    onDismissLogout = onDismissLogout,
                    onConfirmLogout = onConfirmLogout,
                )
            }
        }
    }

    private fun stateDescription(value: String) =
        SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, value)

    private val indeterminate =
        SemanticsMatcher.expectValue(SemanticsProperties.ProgressBarRangeInfo, ProgressBarRangeInfo.Indeterminate)

    @Test
    fun rendersTitleHeaderProgressRowsAndTipsWithoutBackArrow() {
        show(state(completed = listOf(SetupStep.PersonalDetails, SetupStep.Timezone)))

        rule.onNodeWithText("Profile Setup").assertExists()
        rule.onAllNodesWithContentDescription("Back").assertCount(0)
        rule.onNodeWithText("Complete Your Profile!").assertExists()
        rule.onNodeWithText("A complete profile helps you get more bookings.").assertExists()
        rule.onNodeWithText("Profile Completion", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("2 of 5", useUnmergedTree = true).assertExists()
        listOf("Personal details", "Confirm timezone", "Add profile picture", "Verify email", "Add expertise").forEach {
            rule.onNodeWithText(it).assertExists()
        }
        rule.onNodeWithText("Tips for a Great Profile").assertExists()
        rule.onNodeWithText("Craft a Compelling Bio").assertExists()
        rule.onNodeWithText("Choose a Professional Picture").assertExists()
        rule.onNodeWithText("Clearly Define Your Expertise").assertExists()
    }

    @Test
    fun progressBlockIsOneNodeWithACombinedDescription() {
        show(state(completed = listOf(SetupStep.PersonalDetails, SetupStep.Timezone)))

        rule.onAllNodesWithContentDescription("Profile completion, 2 of 5 steps").assertCount(1)
    }

    @Test
    fun rowsReportCompletedStateDescriptions() {
        show(state(completed = listOf(SetupStep.PersonalDetails)))

        rule.onNode(hasClickAction() and hasText("Personal details")).assert(stateDescription("Completed"))
        rule.onNode(hasClickAction() and hasText("Verify email")).assert(stateDescription("Not completed"))
    }

    @Test
    fun tappingARowOpensItsStep() {
        val opened = mutableListOf<SetupStep>()
        show(onOpenStep = { opened += it })

        rule.onNode(hasClickAction() and hasText("Add profile picture")).performScrollTo().performClick()

        assertEquals(listOf(SetupStep.ProfilePhoto), opened)
    }

    @Test
    fun nextIsEnabledWithNothingCompleteAndCallsOnNext() {
        var nexts = 0
        show(state(), onNext = { nexts++ })

        rule.onNode(hasClickAction() and hasText("Next")).assertIsEnabled().performClick()

        assertEquals(1, nexts)
    }

    @Test
    fun overflowMenuLogOutRequestsConfirmation() {
        var requests = 0
        show(onRequestLogout = { requests++ })

        rule.onNodeWithContentDescription("More options").performClick()
        rule.onNodeWithText("Log out").performClick()

        assertEquals(1, requests)
    }

    @Test
    fun logoutDialogShowsBodyAndConfirmCallsOnConfirmLogout() {
        var confirms = 0
        show(state(showLogoutConfirm = true), onConfirmLogout = { confirms++ })

        rule.onNodeWithText("Log out?").assertExists()
        rule
            .onNodeWithText(
                "You'll need to log in again to finish setting up your profile. Your saved progress is kept.",
            ).assertExists()
        rule.onNodeWithText("Log out").performClick()

        assertEquals(1, confirms)
    }

    @Test
    fun logoutDialogCancelCallsOnDismissLogout() {
        var dismisses = 0
        show(state(showLogoutConfirm = true), onDismissLogout = { dismisses++ })

        rule.onNodeWithText("Cancel").performClick()

        assertEquals(1, dismisses)
    }

    @Test
    fun refreshingShowsAnIndeterminateBarUnderTheAppBar() {
        show(state(isRefreshing = true))

        // The bar under the app bar plus the pull-to-refresh indicator PullToRefreshBox shows while refreshing.
        val nodes = rule.onAllNodes(indeterminate).fetchSemanticsNodes()
        assertTrue(nodes.isNotEmpty())
    }

    @Test
    fun notRefreshingShowsNoIndeterminateBar() {
        show(state(isRefreshing = false))

        rule.onAllNodes(indeterminate).assertCount(0)
    }

    @Test
    fun refreshFailureShowsSnackbarAndRetryCallsOnRefresh() {
        var refreshes = 0
        var shown = 0
        show(
            state(refreshFailed = true),
            onRefresh = { refreshes++ },
            onRefreshErrorShown = { shown++ },
        )

        rule.onNodeWithText("Couldn't refresh your profile. Pull down to retry.").assertExists()
        rule.onNodeWithText("Retry").performClick()
        rule.waitForIdle()

        assertEquals(1, refreshes)
        assertEquals(1, shown)
    }
}
