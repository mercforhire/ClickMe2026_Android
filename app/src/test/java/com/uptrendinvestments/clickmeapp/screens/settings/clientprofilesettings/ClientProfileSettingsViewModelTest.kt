@file:OptIn(ExperimentalCoroutinesApi::class)

package com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings

import com.uptrendinvestments.clickmeapp.model.requests.ProfileBasicBody
import com.uptrendinvestments.clickmeapp.model.requests.UpdateUserProfileRequest
import com.uptrendinvestments.clickmeapp.model.responses.UserProfileData
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.SignupTestHarness
import com.uptrendinvestments.clickmeapp.testing.userProfileData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ClientProfileSettingsViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    @get:Rule
    val tmp = TemporaryFolder()

    private val seeded = userProfileData(firstName = "Ana", lastName = "Lee", bio = "Hi there")

    /** A signed-in harness whose cached profile is [profile] and whose PATCH answers with [after]. */
    private suspend fun TestScope.signedIn(
        profile: UserProfileData = seeded,
        after: UserProfileData = profile,
    ): SignupTestHarness {
        val h = SignupTestHarness(this, tmp, main.testDispatcher)
        h.api.userProfileHandler = { profile }
        h.signIn()
        runCurrent()
        h.userManager.refreshProfile()
        h.api.updateUserProfileHandler = { after }
        h.api.userProfileHandler = { after }
        return h
    }

    private fun TestScope.viewModel(h: SignupTestHarness) =
        ClientProfileSettingsViewModel(ProfileSettingsRepository(h.api), h.userManager, backgroundScope)

    @Test
    fun seedsFromTheCachedProfile() =
        runTest(main.testDispatcher) {
            val h = signedIn()
            val vm = viewModel(h)
            runCurrent()

            val state = vm.uiState.value
            assertEquals("Ana", state.snapshot?.firstName)
            assertEquals("Lee", state.snapshot?.lastName)
            assertEquals("Hi there", state.snapshot?.bio)
            assertEquals("ana@example.com", state.email)
            assertEquals(AutoSaveStatus.Idle, state.status)
        }

    @Test
    fun firstNameEditPatchesBasicGroupOnly() =
        runTest(main.testDispatcher) {
            val h = signedIn(after = userProfileData(firstName = "Anna", lastName = "Lee", bio = "Hi there"))
            val vm = viewModel(h)
            runCurrent()

            vm.onFieldChange(ProfileTextFieldId.FirstName, "Anna")
            advanceTimeBy(AUTO_SAVE_DEBOUNCE_MS - 1)
            runCurrent()
            assertEquals(0, h.api.updateUserProfileCalls)

            advanceTimeBy(1)
            runCurrent()

            assertEquals(
                listOf(UpdateUserProfileRequest(basic = ProfileBasicBody("Anna", "Lee", "Hi there"))),
                h.api.updateUserProfileRequests,
            )
            val cached = h.userManager.profile.value
            assertEquals("Anna", cached?.personalDetails?.firstName)
            assertEquals(AutoSaveStatus.Saved, vm.uiState.value.status)
        }

    @Test
    fun burstOfEditsSendsOnePatchWithTheFinalValue() =
        runTest(main.testDispatcher) {
            val h = signedIn()
            val vm = viewModel(h)
            runCurrent()

            vm.onFieldChange(ProfileTextFieldId.FirstName, "A")
            advanceTimeBy(400)
            vm.onFieldChange(ProfileTextFieldId.FirstName, "An")
            advanceTimeBy(400)
            vm.onFieldChange(ProfileTextFieldId.FirstName, "Anna")
            advanceTimeBy(AUTO_SAVE_DEBOUNCE_MS)
            runCurrent()

            assertEquals(1, h.api.updateUserProfileCalls)
            val sent = h.api.updateUserProfileRequests.single()
            assertEquals("Anna", sent.basic?.firstName)
        }
}
