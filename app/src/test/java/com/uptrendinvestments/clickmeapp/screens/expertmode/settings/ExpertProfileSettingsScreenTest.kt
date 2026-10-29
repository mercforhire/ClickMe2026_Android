package com.uptrendinvestments.clickmeapp.screens.expertmode.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import com.uptrendinvestments.clickmeapp.model.responses.UserProfileData
import com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.ProfileSnapshot
import com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.subviews.AUTO_SAVE_STATUS_TAG
import com.uptrendinvestments.clickmeapp.testing.userProfileData
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExpertProfileSettingsScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private fun readyState(
        meetingUrl: String = "https://zoom.us/j/1",
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
    )

    private fun setContent(
        state: ExpertProfileSettingsUiState,
        actions: ExpertSettingsActions = ExpertSettingsActions(),
    ) {
        rule.setContent { ClickMeTheme { ExpertProfileSettingsContent(state = state, actions = actions) } }
    }

    @Test
    fun loadedShowsMeetingField() {
        setContent(readyState())

        rule.onNodeWithText("Expert Settings").assertIsDisplayed()
        rule.onNodeWithText("Skype / Zoom Meeting Link").assertIsDisplayed()
        rule.onNodeWithText("https://zoom.us/j/1").performScrollTo().assertIsDisplayed()
        rule.onNodeWithTag(AUTO_SAVE_STATUS_TAG).assertIsDisplayed()
    }
}
