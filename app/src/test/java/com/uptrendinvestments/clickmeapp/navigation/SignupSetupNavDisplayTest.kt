package com.uptrendinvestments.clickmeapp.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import com.uptrendinvestments.clickmeapp.screens.signup.SetupStep
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private class StepClearTrackingViewModel(
    private val onCleared: () -> Unit,
) : ViewModel() {
    override fun onCleared() = onCleared.invoke()
}

@RunWith(RobolectricTestRunner::class)
class SignupSetupNavDisplayTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var setupNavigator: SignupSetupNavigator? = null
    private var tagsViewModelCleared = false

    private fun nav(): SignupSetupNavigator = checkNotNull(setupNavigator)

    private fun entry(key: NavKey): NavEntry<NavKey> =
        NavEntry(key) {
            when (key) {
                is HubKey -> Text("stub:hub")
                is TagsKey -> {
                    viewModel { StepClearTrackingViewModel { tagsViewModelCleared = true } }
                    Text("stub:tags")
                }
                is TimezoneKey -> Text("stub:timezone")
                is ReviewKey -> Text("stub:review")
                else -> Text("stub:other")
            }
        }

    private fun setContent() {
        composeRule.setContent {
            ClickMeTheme {
                val state = rememberNavigationState(HubKey, setOf(HubKey))
                val setup = remember(state) { SignupSetupNavigator(Navigator(state)) }
                setupNavigator = setup
                SignupSetupNavDisplay(
                    navigationState = state,
                    setupNavigator = setup,
                    entryProvider = ::entry,
                )
            }
        }
    }

    private fun backLikeNavDisplay() {
        composeRule.runOnIdle { nav().back() }
        composeRule.waitForIdle()
    }

    @Test
    fun showsHubFirstThenStepAndBackReturnsToHub() {
        setContent()
        composeRule.onNode(hasText("stub:hub")).assertExists()
        composeRule.runOnIdle { nav().openStep(SetupStep.Timezone) }
        composeRule.waitForIdle()
        composeRule.onNode(hasText("stub:timezone")).assertExists()
        backLikeNavDisplay()
        composeRule.onNode(hasText("stub:hub")).assertExists()
    }

    @Test
    fun atHubRootBackIsNoOpAndSingleEntryRemains() {
        setContent()
        backLikeNavDisplay()
        composeRule.onNode(hasText("stub:hub")).assertExists()
        val state = nav().navigator.state
        val hubStack = state.backStacks.getValue(HubKey)
        assertEquals(1, hubStack.size)
    }

    @Test
    fun poppedStepViewModelIsClearedAndEditFromReviewReturnsToReview() {
        setContent()
        composeRule.runOnIdle {
            nav().openReview()
            nav().openStep(SetupStep.Expertise)
        }
        composeRule.waitForIdle()
        composeRule.onNode(hasText("stub:tags")).assertExists()
        assertFalse(tagsViewModelCleared)
        backLikeNavDisplay()
        composeRule.onNode(hasText("stub:review")).assertExists()
        composeRule.onNode(hasText("stub:tags")).assertDoesNotExist()
        assertTrue("Tags step ViewModel was not cleared on pop", tagsViewModelCleared)
    }
}
