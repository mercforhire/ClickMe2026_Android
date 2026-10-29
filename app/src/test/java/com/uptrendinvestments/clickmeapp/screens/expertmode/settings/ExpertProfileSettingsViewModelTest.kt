@file:OptIn(ExperimentalCoroutinesApi::class)

package com.uptrendinvestments.clickmeapp.screens.expertmode.settings

import com.uptrendinvestments.clickmeapp.model.requests.MeetingUrlBody
import com.uptrendinvestments.clickmeapp.model.requests.UpdateExpertMeetingUrlRequest
import com.uptrendinvestments.clickmeapp.model.responses.ExpertLanguage
import com.uptrendinvestments.clickmeapp.model.responses.ExpertLocationDetails
import com.uptrendinvestments.clickmeapp.model.responses.ExpertProfileData
import com.uptrendinvestments.clickmeapp.model.responses.ExpertTag
import com.uptrendinvestments.clickmeapp.model.responses.SuccessMessageResponse
import com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.AUTO_SAVE_DEBOUNCE_MS
import com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.ProfileAvatarController
import com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.ProfileSettingsRepository
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.SignupTestHarness
import com.uptrendinvestments.clickmeapp.testing.expertProfileData
import com.uptrendinvestments.clickmeapp.testing.userProfileData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ExpertProfileSettingsViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    @get:Rule
    val tmp = TemporaryFolder()

    /** The fake expert profile: Ana in Toronto (CAN) with a Zoom link, one language and two tags. */
    private val expert: ExpertProfileData =
        expertProfileData(
            meetingUrl = "https://zoom.us/j/1",
            tags = listOf(ExpertTag("tax", "Tax", true), ExpertTag("law", "Law")),
        ).let {
            it.copy(
                locationDetails = ExpertLocationDetails(city = "Toronto", stateProvince = "ON", countryCode = "CAN"),
                languages = listOf(ExpertLanguage(id = "en", label = "English")),
            )
        }

    private suspend fun TestScope.signedIn(profile: ExpertProfileData = expert): SignupTestHarness {
        val h = SignupTestHarness(this, tmp, main.testDispatcher)
        h.api.userProfileHandler = { userProfileData() }
        h.signIn()
        runCurrent()
        h.userManager.refreshProfile()
        h.api.expertProfileHandler = { profile }
        h.api.updateUserProfileHandler = { userProfileData() }
        h.api.updateExpertMeetingUrlHandler = { SuccessMessageResponse("ok") }
        return h
    }

    private fun TestScope.viewModel(h: SignupTestHarness): ExpertProfileSettingsViewModel {
        val profileRepository = ProfileSettingsRepository(h.api)
        return ExpertProfileSettingsViewModel(
            ExpertProfileSettingsRepository(h.api),
            profileRepository,
            ProfileAvatarController(h.photo, profileRepository, h.userManager, backgroundScope),
            h.userManager,
            backgroundScope,
        )
    }

    @Test
    fun loadsAndSeedsFromExpertProfile() =
        runTest(main.testDispatcher) {
            val h = signedIn()
            val vm = viewModel(h)
            runCurrent()

            val state = vm.uiState.value
            assertEquals(ExpertSettingsLoad.Ready, state.load)
            assertEquals("Ana", state.snapshot?.profile?.firstName)
            assertEquals("Toronto", state.snapshot?.profile?.city)
            assertEquals("CAN", state.snapshot?.profile?.countryCode)
            assertEquals("https://zoom.us/j/1", state.snapshot?.meetingUrl)
            assertEquals("https://zoom.us/j/1", state.meetingUrlText)
            assertEquals("ana@example.com", state.email)
            val languages = state.snapshot?.profile?.languages
            assertEquals(1, languages?.size)
            assertEquals(1, h.api.expertProfileCalls)
        }

    @Test
    fun meetingLinkSavesToExpertProfileOnly() =
        runTest(main.testDispatcher) {
            val h = signedIn()
            val vm = viewModel(h)
            runCurrent()

            vm.onMeetingUrlChange("https://meet.example.com/x")
            advanceTimeBy(AUTO_SAVE_DEBOUNCE_MS)
            runCurrent()

            assertEquals(
                listOf(UpdateExpertMeetingUrlRequest(MeetingUrlBody("https://meet.example.com/x"))),
                h.api.updateExpertMeetingUrlRequests,
            )
            assertEquals(0, h.api.updateUserProfileCalls)
            assertTrue(vm.uiState.value.meetingUrlError == null)
        }
}
