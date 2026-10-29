@file:OptIn(ExperimentalCoroutinesApi::class)

package com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings

import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.model.requests.ProfileBasicBody
import com.uptrendinvestments.clickmeapp.model.requests.ProfileLocationBody
import com.uptrendinvestments.clickmeapp.model.requests.UpdateUserProfileRequest
import com.uptrendinvestments.clickmeapp.model.responses.UserProfileData
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.SignupTestHarness
import com.uptrendinvestments.clickmeapp.testing.userProfileData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException

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

    @Test
    fun cityAndStateSaveInLocationGroup() =
        runTest(main.testDispatcher) {
            val h = signedIn()
            val vm = viewModel(h)
            runCurrent()

            vm.onFieldChange(ProfileTextFieldId.City, "Paris")
            vm.onFieldChange(ProfileTextFieldId.StateProvince, "IDF")
            advanceTimeBy(AUTO_SAVE_DEBOUNCE_MS)
            runCurrent()

            assertEquals(
                listOf(UpdateUserProfileRequest(location = ProfileLocationBody("Paris", "IDF", "CAN"))),
                h.api.updateUserProfileRequests,
            )
        }

    @Test
    fun limitsClampInput() =
        runTest(main.testDispatcher) {
            val h = signedIn()
            val vm = viewModel(h)
            runCurrent()

            vm.onFieldChange(ProfileTextFieldId.FirstName, "a".repeat(120))
            vm.onFieldChange(ProfileTextFieldId.Bio, "b".repeat(600))
            vm.onFieldChange(ProfileTextFieldId.JobTitle, "c".repeat(250))

            val snapshot = vm.uiState.value.snapshot
            assertEquals(100, snapshot?.firstName?.length)
            assertEquals(500, snapshot?.bio?.length)
            assertEquals(200, snapshot?.jobTitle?.length)
        }

    @Test
    fun failureOpensDialogAndErrorIcon() =
        runTest(main.testDispatcher) {
            val h = signedIn()
            h.api.updateUserProfileHandler = { throw h.api.httpError(500, """{"status":"error","message":"Down"}""") }
            val vm = viewModel(h)
            runCurrent()

            vm.onFieldChange(ProfileTextFieldId.FirstName, "Anna")
            advanceTimeBy(AUTO_SAVE_DEBOUNCE_MS)
            runCurrent()

            assertEquals(AutoSaveStatus.Error, vm.uiState.value.status)
            assertEquals(FieldError.Text("Down"), vm.uiState.value.dialog)

            vm.dismissDialog()
            assertNull(vm.uiState.value.dialog)
        }

    @Test
    fun networkFailureUsesTheConnectionMessage() =
        runTest(main.testDispatcher) {
            val h = signedIn()
            h.api.updateUserProfileHandler = { throw IOException("offline") }
            val vm = viewModel(h)
            runCurrent()

            vm.onFieldChange(ProfileTextFieldId.FirstName, "Anna")
            advanceTimeBy(AUTO_SAVE_DEBOUNCE_MS)
            runCurrent()

            assertEquals(FieldError.Res(R.string.login_error_network), vm.uiState.value.dialog)
        }

    @Test
    fun retrySaveResendsAndSucceeds() =
        runTest(main.testDispatcher) {
            val h = signedIn(after = userProfileData(firstName = "Anna", lastName = "Lee", bio = "Hi there"))
            h.api.updateUserProfileHandler = { throw h.api.httpError(500, """{"status":"error","message":"Down"}""") }
            val vm = viewModel(h)
            runCurrent()
            vm.onFieldChange(ProfileTextFieldId.FirstName, "Anna")
            advanceTimeBy(AUTO_SAVE_DEBOUNCE_MS)
            runCurrent()
            assertEquals(AutoSaveStatus.Error, vm.uiState.value.status)

            h.api.updateUserProfileHandler = { userProfileData(firstName = "Anna", lastName = "Lee") }
            vm.retrySave()
            runCurrent()

            assertEquals(2, h.api.updateUserProfileCalls)
            assertEquals(AutoSaveStatus.Saved, vm.uiState.value.status)
        }

    @Test
    fun validationErrorMapsToField() =
        runTest(main.testDispatcher) {
            val h = signedIn()
            h.api.updateUserProfileHandler = {
                throw h.api.httpError(
                    422,
                    """{"status":"error","errors":[{"field":"basic.first_name","message":"Too long"}]}""",
                )
            }
            val vm = viewModel(h)
            runCurrent()

            vm.onFieldChange(ProfileTextFieldId.FirstName, "Anna")
            advanceTimeBy(AUTO_SAVE_DEBOUNCE_MS)
            runCurrent()

            val state = vm.uiState.value
            assertEquals(mapOf(ProfileTextFieldId.FirstName to FieldError.Text("Too long")), state.fieldErrors)
            assertNull(state.dialog)
            assertEquals(AutoSaveStatus.Error, state.status)

            vm.onFieldChange(ProfileTextFieldId.FirstName, "Ann")
            assertEquals(emptyMap<ProfileTextFieldId, FieldError>(), vm.uiState.value.fieldErrors)
        }

    @Test
    fun bareFieldNameAlsoMapsToField() =
        runTest(main.testDispatcher) {
            val h = signedIn()
            h.api.updateUserProfileHandler = {
                throw h.api.httpError(
                    422,
                    """{"status":"error","errors":[{"field":"city","message":"Unknown city"}]}""",
                )
            }
            val vm = viewModel(h)
            runCurrent()

            vm.onFieldChange(ProfileTextFieldId.City, "Atlantis")
            advanceTimeBy(AUTO_SAVE_DEBOUNCE_MS)
            runCurrent()

            assertEquals(FieldError.Text("Unknown city"), vm.uiState.value.fieldErrors[ProfileTextFieldId.City])
            assertNull(vm.uiState.value.dialog)
        }

    @Test
    fun unknownValidationFieldFallsBackToTheDialog() =
        runTest(main.testDispatcher) {
            val h = signedIn()
            h.api.updateUserProfileHandler = {
                throw h.api.httpError(
                    422,
                    """{"status":"error","message":"Bad language","errors":[{"field":"languages","message":"x"}]}""",
                )
            }
            val vm = viewModel(h)
            runCurrent()

            vm.onFieldChange(ProfileTextFieldId.City, "Rome")
            advanceTimeBy(AUTO_SAVE_DEBOUNCE_MS)
            runCurrent()

            assertEquals(FieldError.Text("Bad language"), vm.uiState.value.dialog)
            assertEquals(emptyMap<ProfileTextFieldId, FieldError>(), vm.uiState.value.fieldErrors)
        }

    @Test
    fun backFlushesPendingEdit() =
        runTest(main.testDispatcher) {
            val h = signedIn()
            val vm = viewModel(h)
            runCurrent()

            vm.onFieldChange(ProfileTextFieldId.LastName, "Leeson")
            advanceTimeBy(AUTO_SAVE_DEBOUNCE_MS / 2)
            assertEquals(0, h.api.updateUserProfileCalls)

            vm.flushPendingSave()
            runCurrent()

            val sent = h.api.updateUserProfileRequests.single()
            assertEquals("Leeson", sent.basic?.lastName)
        }
}
