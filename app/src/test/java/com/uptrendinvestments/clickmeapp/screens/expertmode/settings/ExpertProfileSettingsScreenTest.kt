@file:OptIn(ExperimentalCoroutinesApi::class)

package com.uptrendinvestments.clickmeapp.screens.expertmode.settings

import android.net.Uri
import androidx.compose.material3.Text
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.components.SKELETON_BLOCK_TAG
import com.uptrendinvestments.clickmeapp.model.responses.ExpertiseTagItem
import com.uptrendinvestments.clickmeapp.model.responses.UserProfileData
import com.uptrendinvestments.clickmeapp.navigation.SettingsOrigin
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.ProfileAvatarController
import com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.ProfileSettingsRepository
import com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.ProfileSnapshot
import com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.subviews.AUTO_SAVE_STATUS_TAG
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.SignupTestHarness
import com.uptrendinvestments.clickmeapp.testing.expertProfileData
import com.uptrendinvestments.clickmeapp.testing.userProfileData
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExpertProfileSettingsScreenTest {
    @get:Rule(order = 0)
    val main = MainDispatcherRule(UnconfinedTestDispatcher())

    @get:Rule(order = 1)
    val rule = createComposeRule()

    @get:Rule(order = 2)
    val tmp = TemporaryFolder()

    private val retries = mutableListOf<Unit>()

    private fun readyState(
        meetingUrl: String = "https://zoom.us/j/1",
        tags: List<String> = listOf("Tax", "Law"),
        languages: List<UserProfileData.LanguageEntry> =
            listOf(UserProfileData.LanguageEntry(id = "en", label = "English")),
    ) = ExpertProfileSettingsUiState(
        load = ExpertSettingsLoad.Ready,
        snapshot =
            ExpertSettingsSnapshot(
                profile =
                    ProfileSnapshot.from(
                        userProfileData(bio = "", city = "Toronto", stateProvince = "ON", languages = languages),
                    ),
                meetingUrl = meetingUrl,
            ),
        meetingUrlText = meetingUrl,
        email = "ana@example.com",
        expertiseTags = tags,
    )

    private fun setContent(
        state: ExpertProfileSettingsUiState,
        actions: ExpertSettingsActions = ExpertSettingsActions(),
    ) {
        rule.setContent { ClickMeTheme { ExpertProfileSettingsContent(state = state, actions = actions) } }
    }

    private fun top(text: String): Float =
        rule
            .onAllNodesWithText(text)
            .onFirst()
            .fetchSemanticsNode()
            .positionInRoot.y

    @Test
    fun loadedShowsMeetingField() {
        setContent(readyState())

        rule.onNodeWithText("Expert Settings").assertIsDisplayed()
        rule.onNodeWithText("Skype / Zoom Meeting Link").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("https://zoom.us/j/1").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag(AUTO_SAVE_STATUS_TAG).assertIsDisplayed()
    }

    @Test
    fun fieldOrder() {
        setContent(readyState())

        val emailTop =
            rule
                .onNodeWithContentDescription("Email, ana@example.com, read only")
                .fetchSemanticsNode()
                .positionInRoot.y
        val tops =
            listOf(top("Change photo"), emailTop) +
                listOf(
                    "First Name",
                    "Last Name",
                    "Bio",
                    "Professional Details",
                    "Skype / Zoom Meeting Link",
                    "Expertise",
                    "City",
                    "State/Province",
                    "Country",
                    "Spoken Languages",
                ).map { top(it) }
        assertEquals(tops.sorted(), tops)
        assertEquals(tops.distinct(), tops)
        rule.onNodeWithText("Save").assertDoesNotExist()
        rule.onNodeWithText("reset onboarding", substring = true, ignoreCase = true).assertDoesNotExist()
    }

    @Test
    fun invalidLinkShowsTheInlineMessage() {
        setContent(
            readyState().copy(
                meetingUrlText = "zoom.us/j/1",
                meetingUrlError = FieldError.Text("Enter a link that starts with https://"),
            ),
        )

        rule.onNodeWithText("Enter a link that starts with https://").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun loadingShowsTheSkeleton() {
        setContent(ExpertProfileSettingsUiState())

        rule.onAllNodesWithTag(SKELETON_BLOCK_TAG, useUnmergedTree = true).assertCountEquals(7)
        rule.onNodeWithText("Skype / Zoom Meeting Link").assertDoesNotExist()
    }

    @Test
    fun failedLoadShowsMessageAndRetry() {
        setContent(
            ExpertProfileSettingsUiState(load = ExpertSettingsLoad.Failed(FieldError.Text("Down"))),
            ExpertSettingsActions(onRetryLoad = { retries += Unit }),
        )

        rule.onNodeWithText("Couldn't load profile").assertIsDisplayed()
        rule.onNodeWithText("Down").assertIsDisplayed()
        rule.onNodeWithText("Retry").performClick()
        assertEquals(1, retries.size)
    }

    @Test
    fun avatarUsesOwnOrigin() =
        runTest(main.testDispatcher) {
            val h = SignupTestHarness(this, tmp, main.testDispatcher)
            h.api.userProfileHandler = { userProfileData() }
            h.signIn()
            runCurrent()
            h.api.expertProfileHandler = { expertProfileData() }
            val profileRepository = ProfileSettingsRepository(h.api)
            val vm =
                ExpertProfileSettingsViewModel(
                    ExpertProfileSettingsRepository(h.api),
                    profileRepository,
                    ProfileAvatarController(h.photo, profileRepository, h.userManager, backgroundScope),
                    h.userManager,
                    backgroundScope,
                )
            runCurrent()
            vm.onPhotoPicked(Uri.parse("content://photo/1"))
            runCurrent()
            val opened = mutableListOf<SettingsOrigin>()

            rule.setContent {
                ClickMeTheme {
                    ExpertProfileSettingsScreen(
                        viewModel = vm,
                        origin = SettingsOrigin.Dashboard,
                        onBack = {},
                        onOpenCrop = { opened += it },
                    )
                }
            }
            rule.waitForIdle()

            assertEquals(listOf(SettingsOrigin.Dashboard), opened)
        }

    @Test
    fun expertiseChipsFromProfile() {
        setContent(readyState())

        rule.onNodeWithText("Expertise").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Tax").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Law").assertIsDisplayed()
        rule
            .onNodeWithText("Edit expertise")
            .performScrollTo()
            .assertIsDisplayed()
            .assertHeightIsAtLeast(56.dp)
    }

    @Test
    fun editExpertiseOpensSheetSlot() {
        var state by mutableStateOf(readyState())
        var opened = 0
        var saved: (List<ExpertiseTagItem>) -> Unit = {}
        rule.setContent {
            ClickMeTheme {
                ExpertProfileSettingsContent(
                    state = state,
                    actions = ExpertSettingsActions(onExpertiseSaved = { state = state.copy(expertiseTags = it.map { t -> t.label }) }),
                    expertiseSheet = { _, onSaved ->
                        SideEffect { opened++ }
                        saved = onSaved
                        Text("fake expertise sheet")
                    },
                )
            }
        }

        rule.onNodeWithText("fake expertise sheet").assertDoesNotExist()
        rule.onNodeWithText("Edit expertise").performScrollTo().performClick()
        rule.onNodeWithText("fake expertise sheet").assertIsDisplayed()
        assertEquals(1, opened)

        rule.runOnIdle {
            saved(listOf(ExpertiseTagItem("a", "Alpha"), ExpertiseTagItem("b", "Beta"), ExpertiseTagItem("c", "Gamma")))
        }

        rule.onNodeWithText("fake expertise sheet").assertDoesNotExist()
        listOf("Alpha", "Beta", "Gamma").forEach { rule.onNodeWithText(it).performScrollTo().assertIsDisplayed() }
        rule.onNodeWithText("Tax").assertDoesNotExist()
    }
}
