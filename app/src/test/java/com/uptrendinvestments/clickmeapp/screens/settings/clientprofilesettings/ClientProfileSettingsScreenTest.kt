@file:OptIn(ExperimentalCoroutinesApi::class)

package com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.subviews.AUTO_SAVE_STATUS_TAG
import com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.subviews.PROFILE_BIO_COUNTER_TAG
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.SignupTestHarness
import com.uptrendinvestments.clickmeapp.testing.userProfileData
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ClientProfileSettingsScreenTest {
    @get:Rule(order = 0)
    val main = MainDispatcherRule(UnconfinedTestDispatcher())

    @get:Rule(order = 1)
    val rule = createComposeRule()

    @get:Rule(order = 2)
    val tmp = TemporaryFolder()

    private val changes = mutableListOf<Pair<ProfileTextFieldId, String>>()
    private val retries = mutableListOf<Unit>()
    private val dismissals = mutableListOf<Unit>()
    private val backs = mutableListOf<Unit>()

    private fun state(
        bio: String = "",
        status: AutoSaveStatus = AutoSaveStatus.Idle,
        dialog: FieldError? = null,
        fieldErrors: Map<ProfileTextFieldId, FieldError> = emptyMap(),
    ) = ClientProfileSettingsUiState(
        snapshot = ProfileSnapshot.from(userProfileData(bio = bio, city = "Toronto", stateProvince = "ON")),
        email = "ana@example.com",
        status = status,
        dialog = dialog,
        fieldErrors = fieldErrors,
    )

    private fun setContent(state: ClientProfileSettingsUiState) {
        rule.setContent {
            ClickMeTheme {
                ClientProfileSettingsContent(
                    state = state,
                    onBack = { backs += Unit },
                    onFieldChange = { field, value -> changes += field to value },
                    onRetrySave = { retries += Unit },
                    onDismissDialog = { dismissals += Unit },
                )
            }
        }
    }

    private fun top(text: String): Float =
        rule
            .onAllNodesWithText(text)
            .onFirst()
            .fetchSemanticsNode()
            .boundsInRoot.top

    @Test
    fun fieldsInOrder() {
        setContent(state())

        rule.onNodeWithContentDescription("Email, ana@example.com, read only").assertExists()
        val tops =
            listOf("First Name", "Last Name", "Bio", "City", "State/Province").map { top(it) }
        assertEquals(tops.sorted(), tops)
        assertEquals(tops.distinct(), tops)
        rule.onNodeWithText("Save").assertDoesNotExist()
    }

    @Test
    fun editingAFieldReportsTheNewValue() {
        setContent(state())

        rule
            .onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag(PROFILE_FIELD_TAG_PREFIX + "FirstName")))
            .performTextReplacement("Anna")

        assertEquals(listOf(ProfileTextFieldId.FirstName to "Anna"), changes)
    }

    @Test
    fun statusIconStates() {
        var status by mutableStateOf(AutoSaveStatus.Idle)
        rule.setContent {
            ClickMeTheme {
                ClientProfileSettingsContent(
                    state = state(status = status),
                    onBack = {},
                    onFieldChange = { _, _ -> },
                    onRetrySave = { retries += Unit },
                    onDismissDialog = {},
                )
            }
        }

        val saving = SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Saving")
        rule.onNodeWithTag(AUTO_SAVE_STATUS_TAG).assertExists()
        rule.onNode(saving).assertDoesNotExist()
        rule.onNodeWithContentDescription("Changes saved").assertDoesNotExist()

        status = AutoSaveStatus.Saving
        rule.waitForIdle()
        rule.onNode(saving).assertExists()

        status = AutoSaveStatus.Saved
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Changes saved").assertExists()
        rule.onNode(saving).assertDoesNotExist()

        status = AutoSaveStatus.Error
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Couldn't save changes. Tap to retry.").performClick()
        assertEquals(1, retries.size)
    }

    @Test
    fun bioCounterFrom400() {
        setContent(state(bio = "a".repeat(399)))
        rule.onNodeWithTag(PROFILE_BIO_COUNTER_TAG).assertDoesNotExist()
    }

    @Test
    fun bioCounterShowsAt400() {
        setContent(state(bio = "a".repeat(400)))
        rule.onNodeWithText("400/500").assertExists()
    }

    @Test
    fun saveFailureDialogShowsTitleMessageAndGotIt() {
        setContent(state(dialog = FieldError.Text("Down")))

        rule.onNodeWithText("Couldn't save changes").assertIsDisplayed()
        rule.onNodeWithText("Down").assertIsDisplayed()
        rule.onNodeWithText("Got it").performClick()
        assertEquals(1, dismissals.size)
    }

    @Test
    fun fieldErrorShowsUnderItsField() {
        setContent(state(fieldErrors = mapOf(ProfileTextFieldId.FirstName to FieldError.Text("Too long"))))

        rule.onAllNodesWithText("Too long").assertCountEquals(1)
    }

    @Test
    fun backArrowCallsOnBack() {
        setContent(state())

        rule.onNodeWithContentDescription("Back").performClick()

        assertEquals(1, backs.size)
    }

    @Test
    fun backFlushesPendingEditThenPops() =
        runTest(main.testDispatcher) {
            val h = SignupTestHarness(this, tmp, main.testDispatcher)
            val seeded = userProfileData(firstName = "Ana", lastName = "Lee")
            h.api.userProfileHandler = { seeded }
            h.signIn()
            runCurrent()
            h.userManager.refreshProfile()
            h.api.updateUserProfileHandler = { seeded }
            val vm = ClientProfileSettingsViewModel(ProfileSettingsRepository(h.api), h.userManager, backgroundScope)
            runCurrent()

            rule.setContent { ClickMeTheme { ClientProfileSettingsScreen(viewModel = vm, onBack = { backs += Unit }) } }
            rule
                .onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag(PROFILE_FIELD_TAG_PREFIX + "LastName")))
                .performTextReplacement("Leeson")
            assertEquals(0, h.api.updateUserProfileCalls)

            rule.onNodeWithContentDescription("Back").performClick()
            runCurrent()

            assertEquals(1, h.api.updateUserProfileCalls)
            assertEquals("Leeson", h.api.updateUserProfileRequests.single().basic?.lastName)
            assertEquals(1, backs.size)
            assertTrue(vm.uiState.value.snapshot?.lastName == "Leeson")
        }
}
