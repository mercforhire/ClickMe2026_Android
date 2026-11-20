@file:OptIn(ExperimentalCoroutinesApi::class)

package com.uptrendinvestments.clickmeapp.screens.expertmode.settings

import androidx.lifecycle.SavedStateHandle
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.model.requests.MeetingUrlBody
import com.uptrendinvestments.clickmeapp.model.requests.ProfileBasicBody
import com.uptrendinvestments.clickmeapp.model.requests.ProfileLanguagesBody
import com.uptrendinvestments.clickmeapp.model.requests.ProfileLocationBody
import com.uptrendinvestments.clickmeapp.model.requests.ProfileProfessionalBody
import com.uptrendinvestments.clickmeapp.model.requests.UpdateExpertMeetingUrlRequest
import com.uptrendinvestments.clickmeapp.model.requests.UpdateUserProfileRequest
import com.uptrendinvestments.clickmeapp.model.responses.ExpertLanguage
import com.uptrendinvestments.clickmeapp.model.responses.ExpertLocationDetails
import com.uptrendinvestments.clickmeapp.model.responses.ExpertProfileData
import com.uptrendinvestments.clickmeapp.model.responses.ExpertTag
import com.uptrendinvestments.clickmeapp.model.responses.ExpertiseTagItem
import com.uptrendinvestments.clickmeapp.model.responses.LanguageItem
import com.uptrendinvestments.clickmeapp.model.responses.LanguagesPayload
import com.uptrendinvestments.clickmeapp.model.responses.SuccessMessageResponse
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.AUTO_SAVE_DEBOUNCE_MS
import com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.AutoSaveStatus
import com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.ProfileAvatarController
import com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.ProfileSettingsRepository
import com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.ProfileTextFieldId
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException

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

    private val serverDown = """{"status":"error","message":"Down"}"""

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

    private fun TestScope.viewModel(
        h: SignupTestHarness,
        handle: SavedStateHandle = SavedStateHandle(),
    ): ExpertProfileSettingsViewModel {
        val profileRepository = ProfileSettingsRepository(h.api)
        return ExpertProfileSettingsViewModel(
            ExpertProfileSettingsRepository(h.api),
            profileRepository,
            ProfileAvatarController(h.photo, profileRepository, h.userManager, backgroundScope),
            h.userManager,
            backgroundScope,
            handle,
        )
    }

    /** What the system hands the recreated ViewModel: the same entries in a new handle. */
    private fun copyOf(handle: SavedStateHandle) = SavedStateHandle(handle.keys().associateWith { handle.get<Any>(it) })

    private fun miaHandle() =
        SavedStateHandle(mapOf("expert_settings_first_name" to "Mia", "expert_settings_touched" to true))

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
            assertEquals(listOf("Tax", "Law"), state.expertiseTags)
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

    @Test
    fun rejectedMeetingLinkDoesNotFailLaterAutoSaves() =
        runTest(main.testDispatcher) {
            val h = signedIn()
            val vm = viewModel(h)
            runCurrent()
            h.api.updateExpertMeetingUrlHandler = {
                throw h.api.httpError(
                    422,
                    """{"status":"error","errors":[{"field":"meeting_url","message":"Not a meeting link"}]}""",
                )
            }

            vm.onMeetingUrlChange("https://bad.example.com/x")
            advanceTimeBy(AUTO_SAVE_DEBOUNCE_MS)
            runCurrent()

            assertEquals(FieldError.Text("Not a meeting link"), vm.uiState.value.meetingUrlError)
            assertEquals(AutoSaveStatus.Saved, vm.uiState.value.status)
            assertNull(vm.uiState.value.dialog)
            assertEquals(1, h.api.updateExpertMeetingUrlCalls)

            // An unrelated edit saves cleanly and does not send the rejected link again.
            vm.onFieldChange(ProfileTextFieldId.City, "Ottawa")
            advanceTimeBy(AUTO_SAVE_DEBOUNCE_MS)
            runCurrent()

            assertEquals(1, h.api.updateUserProfileCalls)
            assertEquals(1, h.api.updateExpertMeetingUrlCalls)
            assertEquals(AutoSaveStatus.Saved, vm.uiState.value.status)

            // A different link is sent again.
            h.api.updateExpertMeetingUrlHandler = { SuccessMessageResponse("ok") }
            vm.onMeetingUrlChange("https://meet.example.com/ok")
            advanceTimeBy(AUTO_SAVE_DEBOUNCE_MS)
            runCurrent()

            assertEquals(2, h.api.updateExpertMeetingUrlCalls)
            assertNull(vm.uiState.value.meetingUrlError)
        }

    @Test
    fun sharedFieldsGoToUserProfile() =
        runTest(main.testDispatcher) {
            val h = signedIn()
            val vm = viewModel(h)
            runCurrent()

            vm.onFieldChange(ProfileTextFieldId.Bio, "Tax lawyer")
            vm.onFieldChange(ProfileTextFieldId.City, "Paris")
            advanceTimeBy(AUTO_SAVE_DEBOUNCE_MS)
            runCurrent()

            assertEquals(
                listOf(
                    UpdateUserProfileRequest(
                        basic = ProfileBasicBody("Ana", "Lee", "Tax lawyer"),
                        location = ProfileLocationBody("Paris", "ON", "CAN"),
                    ),
                ),
                h.api.updateUserProfileRequests,
            )
            assertEquals(0, h.api.updateExpertMeetingUrlCalls)
        }

    @Test
    fun bothChangedSendsBoth() =
        runTest(main.testDispatcher) {
            val h = signedIn()
            val calls = mutableListOf<String>()
            h.api.updateUserProfileHandler = {
                calls += "profile"
                userProfileData()
            }
            h.api.updateExpertMeetingUrlHandler = {
                calls += "meeting"
                SuccessMessageResponse("ok")
            }
            val vm = viewModel(h)
            runCurrent()

            vm.onFieldChange(ProfileTextFieldId.FirstName, "Anna")
            vm.onMeetingUrlChange("https://meet.example.com/x")
            advanceTimeBy(AUTO_SAVE_DEBOUNCE_MS)
            runCurrent()

            assertEquals(listOf("profile", "meeting"), calls)
            assertEquals(AutoSaveStatus.Saved, vm.uiState.value.status)
        }

    @Test
    fun invalidLinkNotSent() =
        runTest(main.testDispatcher) {
            val h = signedIn()
            val vm = viewModel(h)
            runCurrent()
            val invalid = FieldError.Res(R.string.expert_settings_meeting_invalid)

            listOf("zoom.us/j/1", "javascript:alert(1)").forEach { typed ->
                vm.onMeetingUrlChange(typed)
                assertEquals(invalid, vm.uiState.value.meetingUrlError)
                assertEquals(typed, vm.uiState.value.meetingUrlText)
                advanceTimeBy(AUTO_SAVE_DEBOUNCE_MS)
                runCurrent()
            }

            assertEquals(0, h.api.updateExpertMeetingUrlCalls)
            assertEquals(0, h.api.updateUserProfileCalls)
            val kept = vm.uiState.value.snapshot
            assertEquals("https://zoom.us/j/1", kept?.meetingUrl)
        }

    @Test
    fun clearingTheSavedLinkSendsAnEmptyMeetingUrl() =
        runTest(main.testDispatcher) {
            val h = signedIn()
            val vm = viewModel(h)
            runCurrent()

            vm.onMeetingUrlChange("")
            assertNull(vm.uiState.value.meetingUrlError)
            advanceTimeBy(AUTO_SAVE_DEBOUNCE_MS)
            runCurrent()

            assertEquals(
                listOf(UpdateExpertMeetingUrlRequest(MeetingUrlBody(""))),
                h.api.updateExpertMeetingUrlRequests,
            )
            assertEquals(0, h.api.updateUserProfileCalls)
            assertEquals(
                "",
                vm.uiState.value.snapshot
                    ?.meetingUrl,
            )
            assertEquals(AutoSaveStatus.Saved, vm.uiState.value.status)

            // The clear is now what the server has: a later edit elsewhere does not send the link again.
            vm.onFieldChange(ProfileTextFieldId.Bio, "Tax lawyer")
            advanceTimeBy(AUTO_SAVE_DEBOUNCE_MS)
            runCurrent()
            assertEquals(1, h.api.updateExpertMeetingUrlCalls)
        }

    @Test
    fun aWhitespaceOnlyFieldClearsTheSavedLink() =
        runTest(main.testDispatcher) {
            val h = signedIn()
            val vm = viewModel(h)
            runCurrent()

            vm.onMeetingUrlChange("   ")
            advanceTimeBy(AUTO_SAVE_DEBOUNCE_MS)
            runCurrent()

            assertEquals(
                listOf(UpdateExpertMeetingUrlRequest(MeetingUrlBody(""))),
                h.api.updateExpertMeetingUrlRequests,
            )
        }

    @Test
    fun anExpertWithNoSavedLinkNeverSendsTheKey() =
        runTest(main.testDispatcher) {
            val h = signedIn(expert.copy(professionalDetails = null))
            val vm = viewModel(h)
            runCurrent()

            vm.onMeetingUrlChange("")
            vm.onFieldChange(ProfileTextFieldId.Bio, "Tax lawyer")
            advanceTimeBy(AUTO_SAVE_DEBOUNCE_MS)
            runCurrent()

            assertEquals(0, h.api.updateExpertMeetingUrlCalls)
            assertEquals(1, h.api.updateUserProfileCalls)
            assertEquals(
                "",
                vm.uiState.value.snapshot
                    ?.meetingUrl,
            )
        }

    @Test
    fun aNewLinkIsSentTrimmed() =
        runTest(main.testDispatcher) {
            val h = signedIn()
            val vm = viewModel(h)
            runCurrent()

            vm.onMeetingUrlChange("  https://zoom.us/j/2  ")
            advanceTimeBy(AUTO_SAVE_DEBOUNCE_MS)
            runCurrent()

            assertEquals(
                listOf(UpdateExpertMeetingUrlRequest(MeetingUrlBody("https://zoom.us/j/2"))),
                h.api.updateExpertMeetingUrlRequests,
            )
        }

    @Test
    fun aJavascriptLinkIsNeverSentEvenAfterAClear() =
        runTest(main.testDispatcher) {
            val h = signedIn()
            val vm = viewModel(h)
            runCurrent()

            vm.onMeetingUrlChange("")
            advanceTimeBy(AUTO_SAVE_DEBOUNCE_MS)
            runCurrent()
            vm.onMeetingUrlChange("javascript:alert(1)")
            advanceTimeBy(AUTO_SAVE_DEBOUNCE_MS)
            runCurrent()

            assertEquals(
                listOf(UpdateExpertMeetingUrlRequest(MeetingUrlBody(""))),
                h.api.updateExpertMeetingUrlRequests,
            )
        }

    @Test
    fun aLinkBrokenAfterBeingValidIsNotSaved() =
        runTest(main.testDispatcher) {
            val h = signedIn()
            val vm = viewModel(h)
            runCurrent()

            vm.onMeetingUrlChange("https://meet.example.com/x")
            vm.onMeetingUrlChange("https:/")
            advanceTimeBy(AUTO_SAVE_DEBOUNCE_MS)
            runCurrent()

            assertEquals(0, h.api.updateExpertMeetingUrlCalls)
        }

    @Test
    fun secondCallFailureRetriesBoth() =
        runTest(main.testDispatcher) {
            val h = signedIn()
            h.api.updateExpertMeetingUrlHandler = { throw h.api.httpError(500, serverDown) }
            val vm = viewModel(h)
            runCurrent()

            vm.onFieldChange(ProfileTextFieldId.FirstName, "Anna")
            vm.onMeetingUrlChange("https://meet.example.com/x")
            advanceTimeBy(AUTO_SAVE_DEBOUNCE_MS)
            runCurrent()
            assertEquals(AutoSaveStatus.Error, vm.uiState.value.status)
            assertEquals(FieldError.Text("Down"), vm.uiState.value.dialog)
            assertEquals(1, h.api.updateUserProfileCalls)

            h.api.updateExpertMeetingUrlHandler = { SuccessMessageResponse("ok") }
            vm.retrySave()
            runCurrent()

            assertEquals(2, h.api.updateUserProfileCalls)
            assertEquals(2, h.api.updateExpertMeetingUrlCalls)
            assertEquals(AutoSaveStatus.Saved, vm.uiState.value.status)
        }

    @Test
    fun failedLoadThenRetry() =
        runTest(main.testDispatcher) {
            val h = signedIn()
            h.api.expertProfileHandler = { throw h.api.httpError(500, serverDown) }
            val vm = viewModel(h)
            runCurrent()

            assertEquals(ExpertSettingsLoad.Failed(FieldError.Text("Down")), vm.uiState.value.load)

            h.api.expertProfileHandler = { expert }
            vm.retryLoad()
            runCurrent()

            assertEquals(ExpertSettingsLoad.Ready, vm.uiState.value.load)
            assertEquals(2, h.api.expertProfileCalls)
        }

    @Test
    fun failedLoadWithoutAServerMessageUsesTheConnectionCopy() =
        runTest(main.testDispatcher) {
            val h = signedIn()
            h.api.expertProfileHandler = { throw IOException("offline") }
            val vm = viewModel(h)
            runCurrent()

            assertEquals(
                ExpertSettingsLoad.Failed(FieldError.Res(R.string.login_error_network)),
                vm.uiState.value.load,
            )
        }

    @Test
    fun professionalDetailsApplyAndSave() =
        runTest(main.testDispatcher) {
            val h = signedIn()
            val vm = viewModel(h)
            runCurrent()

            vm.openProfessionalSheet()
            assertTrue(vm.uiState.value.professionalSheetOpen)
            vm.applyProfessional("Coach", "Acme")
            assertFalse(vm.uiState.value.professionalSheetOpen)
            advanceTimeBy(AUTO_SAVE_DEBOUNCE_MS)
            runCurrent()

            assertEquals(
                listOf(UpdateUserProfileRequest(professional = ProfileProfessionalBody("Coach", "Acme"))),
                h.api.updateUserProfileRequests,
            )

            vm.openProfessionalSheet()
            vm.discardProfessional()
            assertFalse(vm.uiState.value.professionalSheetOpen)
        }

    @Test
    fun countrySelectionSavesAlpha3() =
        runTest(main.testDispatcher) {
            val h = signedIn()
            val vm = viewModel(h)
            runCurrent()

            vm.openCountry()
            assertTrue(vm.uiState.value.countrySheetOpen)
            vm.onCountryQuery("united st")
            val visible = vm.uiState.value.visibleCountries
            assertEquals(listOf("USA"), visible.map { it.code })
            vm.selectCountry("USA")
            assertFalse(vm.uiState.value.countrySheetOpen)
            advanceTimeBy(AUTO_SAVE_DEBOUNCE_MS)
            runCurrent()

            assertEquals(
                listOf(UpdateUserProfileRequest(location = ProfileLocationBody("Toronto", "ON", "USA"))),
                h.api.updateUserProfileRequests,
            )
        }

    @Test
    fun languagesDoneSavesIdsAndKeepsCustoms() =
        runTest(main.testDispatcher) {
            val withCustom =
                expert.copy(
                    languages = listOf(ExpertLanguage("en", "English"), ExpertLanguage(null, "Klingon")),
                )
            val h = signedIn(withCustom)
            h.api.languagesHandler = {
                LanguagesPayload(listOf(LanguageItem("en", "English"), LanguageItem("fr", "French")))
            }
            val vm = viewModel(h)
            runCurrent()

            vm.openLanguages()
            runCurrent()
            vm.toggleLanguage(LanguageItem("fr", "French"))
            vm.applyLanguages()
            assertNull(vm.uiState.value.languageSheet)
            advanceTimeBy(AUTO_SAVE_DEBOUNCE_MS)
            runCurrent()

            assertEquals(
                listOf(
                    UpdateUserProfileRequest(languages = ProfileLanguagesBody(listOf("en", "fr"), listOf("Klingon"))),
                ),
                h.api.updateUserProfileRequests,
            )
        }

    @Test
    fun expertiseSaveDoesNotAutoSave() =
        runTest(main.testDispatcher) {
            val h = signedIn()
            val vm = viewModel(h)
            runCurrent()

            vm.onExpertiseSaved(listOf(ExpertiseTagItem("a", "Alpha"), ExpertiseTagItem("b", "Beta")))
            advanceTimeBy(AUTO_SAVE_DEBOUNCE_MS)
            runCurrent()

            assertEquals(listOf("Alpha", "Beta"), vm.uiState.value.expertiseTags)
            assertEquals(0, h.api.updateUserProfileCalls)
            assertEquals(0, h.api.updateExpertMeetingUrlCalls)
            assertEquals(AutoSaveStatus.Idle, vm.uiState.value.status)
        }

    // ---- process death and recreation (CONTEXT D-12) ----

    @Test
    fun typedFieldsRestored() =
        runTest(main.testDispatcher) {
            val h = signedIn()
            h.api.languagesHandler = {
                LanguagesPayload(listOf(LanguageItem("en", "English"), LanguageItem("fr", "French")))
            }
            val handle = SavedStateHandle()
            val first = viewModel(h, handle)
            runCurrent()
            first.onFieldChange(ProfileTextFieldId.FirstName, "Mia")
            first.onFieldChange(ProfileTextFieldId.LastName, "Roy")
            first.onFieldChange(ProfileTextFieldId.Bio, "Coach")
            first.applyProfessional("Advisor", "Acme")
            first.onFieldChange(ProfileTextFieldId.City, "Lyon")
            first.onMeetingUrlChange("https://meet.example.com/x")
            first.selectCountry("FR")
            first.openLanguages()
            runCurrent()
            first.toggleLanguage(LanguageItem("fr", "French"))
            first.applyLanguages()

            val second = viewModel(h, copyOf(handle))
            runCurrent()

            val state = second.uiState.value
            assertEquals(first.uiState.value.snapshot, state.snapshot)
            assertEquals("Mia", state.snapshot?.profile?.firstName)
            assertEquals("Advisor", state.snapshot?.profile?.jobTitle)
            assertEquals("FRA", state.snapshot?.profile?.countryCode)
            assertEquals(
                listOf("en", "fr"),
                state.snapshot
                    ?.profile
                    ?.languages
                    ?.map { it.id },
            )
            assertEquals("https://meet.example.com/x", state.meetingUrlText)
            assertEquals("https://meet.example.com/x", state.snapshot?.meetingUrl)
        }

    @Test
    fun restoredBeatsServer() =
        runTest(main.testDispatcher) {
            val h = signedIn()

            val vm = viewModel(h, miaHandle())
            runCurrent()

            val profile =
                vm.uiState.value.snapshot
                    ?.profile
            assertEquals("Mia", profile?.firstName)
            assertEquals("Lee", profile?.lastName)
            assertEquals("Toronto", profile?.city)
        }

    @Test
    fun untouchedSeedsFromServer() =
        runTest(main.testDispatcher) {
            val h = signedIn()

            val vm = viewModel(h, SavedStateHandle())
            runCurrent()
            advanceTimeBy(AUTO_SAVE_DEBOUNCE_MS)
            runCurrent()

            assertEquals(
                "Ana",
                vm.uiState.value.snapshot
                    ?.profile
                    ?.firstName,
            )
            assertEquals(0, h.api.updateUserProfileCalls)
            assertEquals(0, h.api.updateExpertMeetingUrlCalls)
        }

    @Test
    fun restoredEditIsAutoSaved() =
        runTest(main.testDispatcher) {
            val h = signedIn()

            viewModel(h, miaHandle())
            runCurrent()
            assertEquals(0, h.api.updateUserProfileCalls)
            advanceTimeBy(AUTO_SAVE_DEBOUNCE_MS)
            runCurrent()

            assertEquals(1, h.api.updateUserProfileCalls)
            assertEquals(
                "Mia",
                h.api.updateUserProfileRequests
                    .single()
                    .basic
                    ?.firstName,
            )
        }

    @Test
    fun onlyBundleTypes() =
        runTest(main.testDispatcher) {
            val h = signedIn()
            h.api.languagesHandler = { LanguagesPayload(listOf(LanguageItem("fr", "French"))) }
            val handle = SavedStateHandle()
            val vm = viewModel(h, handle)
            runCurrent()
            vm.onFieldChange(ProfileTextFieldId.Bio, "About me")
            vm.onMeetingUrlChange("https://meet.example.com/x")
            vm.applyProfessional("Coach", "Acme")
            vm.selectCountry("FR")
            vm.openLanguages()
            runCurrent()
            vm.toggleLanguage(LanguageItem("fr", "French"))
            vm.applyLanguages()

            assertTrue(handle.keys().isNotEmpty())
            for (key in handle.keys()) {
                val value = handle.get<Any>(key)
                val isStringList = value is ArrayList<*> && value.all { it is String }
                assertTrue("$key holds ${value?.javaClass}", value is String || value is Boolean || isStringList)
            }
        }

    @Test
    fun blankRestoredMeetingLinkOverASavedLinkIsAClear() =
        runTest(main.testDispatcher) {
            val h = signedIn()
            val handle =
                SavedStateHandle(
                    mapOf(
                        "expert_settings_meeting_url" to "   ",
                        "expert_settings_first_name" to "Mia",
                        "expert_settings_touched" to true,
                    ),
                )

            val vm = viewModel(h, handle)
            runCurrent()
            advanceTimeBy(AUTO_SAVE_DEBOUNCE_MS)
            runCurrent()

            assertEquals(
                "",
                vm.uiState.value.snapshot
                    ?.meetingUrl,
            )
            assertEquals(
                listOf(UpdateExpertMeetingUrlRequest(MeetingUrlBody(""))),
                h.api.updateExpertMeetingUrlRequests,
            )
            assertEquals(1, h.api.updateUserProfileCalls)
        }

    @Test
    fun anInvalidRestoredMeetingLinkKeepsTheServersLink() =
        runTest(main.testDispatcher) {
            val h = signedIn()
            val handle =
                SavedStateHandle(
                    mapOf(
                        "expert_settings_meeting_url" to "javascript:alert(1)",
                        "expert_settings_first_name" to "Mia",
                        "expert_settings_touched" to true,
                    ),
                )

            val vm = viewModel(h, handle)
            runCurrent()
            advanceTimeBy(AUTO_SAVE_DEBOUNCE_MS)
            runCurrent()

            assertEquals(
                "https://zoom.us/j/1",
                vm.uiState.value.snapshot
                    ?.meetingUrl,
            )
            assertEquals(0, h.api.updateExpertMeetingUrlCalls)
            assertEquals(1, h.api.updateUserProfileCalls)
        }
}
