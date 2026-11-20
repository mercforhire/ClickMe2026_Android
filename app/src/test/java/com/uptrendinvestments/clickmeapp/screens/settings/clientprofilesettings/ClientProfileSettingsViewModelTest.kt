@file:OptIn(ExperimentalCoroutinesApi::class)

package com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings

import androidx.lifecycle.SavedStateHandle
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.model.requests.ProfileBasicBody
import com.uptrendinvestments.clickmeapp.model.requests.ProfileLanguagesBody
import com.uptrendinvestments.clickmeapp.model.requests.ProfileLocationBody
import com.uptrendinvestments.clickmeapp.model.requests.ProfileProfessionalBody
import com.uptrendinvestments.clickmeapp.model.requests.UpdateUserProfileRequest
import com.uptrendinvestments.clickmeapp.model.responses.LanguageItem
import com.uptrendinvestments.clickmeapp.model.responses.LanguagesPayload
import com.uptrendinvestments.clickmeapp.model.responses.UserProfileData
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.screens.signup.basicinfo.LanguageCatalog
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.SignupTestHarness
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

    private fun TestScope.viewModel(
        h: SignupTestHarness,
        handle: SavedStateHandle = SavedStateHandle(),
    ) = ClientProfileSettingsViewModel(
        ProfileSettingsRepository(h.api),
        h.userManager,
        ProfileAvatarController(h.photo, ProfileSettingsRepository(h.api), h.userManager, backgroundScope),
        backgroundScope,
        handle,
    )

    /** What the system hands the recreated ViewModel: the same entries in a new handle. */
    private fun copyOf(handle: SavedStateHandle) = SavedStateHandle(handle.keys().associateWith { handle.get<Any>(it) })

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
    fun failedFirstLoadShowsRetryStateThenSeedsOnRetry() =
        runTest(main.testDispatcher) {
            val h = SignupTestHarness(this, tmp, main.testDispatcher)
            h.signIn()
            runCurrent()
            h.api.userProfileHandler = { throw IOException("offline") }
            val vm = viewModel(h)
            runCurrent()

            assertTrue(vm.uiState.value.loadFailed)
            assertNull(vm.uiState.value.snapshot)

            h.api.userProfileHandler = { seeded }
            vm.retryLoad()
            runCurrent()

            assertFalse(vm.uiState.value.loadFailed)
            assertEquals(
                "Ana",
                vm.uiState.value.snapshot
                    ?.firstName,
            )
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
    fun whitespaceOnlyEditSendsNothingAndShowsNoSavedFlash() =
        runTest(main.testDispatcher) {
            val h = signedIn()
            val vm = viewModel(h)
            runCurrent()

            vm.onFieldChange(ProfileTextFieldId.FirstName, "Ana ")
            advanceTimeBy(AUTO_SAVE_DEBOUNCE_MS)
            runCurrent()

            assertEquals(0, h.api.updateUserProfileCalls)
            assertEquals(AutoSaveStatus.Idle, vm.uiState.value.status)
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

    private val rawText = "Too small: expected string to have >=1 characters"

    private fun validationBody(
        vararg fields: String,
        code: String? = "VALIDATION_ERROR",
    ): String {
        val errors = fields.joinToString(",") { """{"field":"$it","message":"$rawText"}""" }
        val codePart = code?.let { ""","code":"$it"""" }.orEmpty()
        return """{"status":"error","message":"Please check the information you entered and try again.",""" +
            """"errors":[$errors]$codePart}"""
    }

    @Test
    fun validationErrorNamesTheFieldsAndNeverShowsServerText() =
        runTest(main.testDispatcher) {
            val h = signedIn()
            h.api.updateUserProfileHandler = {
                throw h.api.httpError(422, validationBody("basic.first_name", "basic.last_name"))
            }
            val vm = viewModel(h)
            runCurrent()

            vm.onFieldChange(ProfileTextFieldId.FirstName, "Anna")
            advanceTimeBy(AUTO_SAVE_DEBOUNCE_MS)
            runCurrent()

            val state = vm.uiState.value
            assertEquals(FieldError.Text("Check First Name and Last Name, then try again."), state.dialog)
            assertEquals(emptyMap<ProfileTextFieldId, FieldError>(), state.fieldErrors)
            assertEquals(AutoSaveStatus.Error, state.status)
        }

    @Test
    fun validationErrorFieldsComeInScreenOrder() =
        runTest(main.testDispatcher) {
            val h = signedIn()
            h.api.updateUserProfileHandler = {
                throw h.api.httpError(422, validationBody("location.city", "basic.bio"))
            }
            val vm = viewModel(h)
            runCurrent()

            vm.onFieldChange(ProfileTextFieldId.City, "Atlantis")
            advanceTimeBy(AUTO_SAVE_DEBOUNCE_MS)
            runCurrent()

            assertEquals(FieldError.Text("Check Bio and City, then try again."), vm.uiState.value.dialog)
        }

    @Test
    fun validationErrorWithOnlyUnknownFieldsGivesTheGenericSentence() =
        runTest(main.testDispatcher) {
            val h = signedIn()
            h.api.updateUserProfileHandler = {
                throw h.api.httpError(422, validationBody("location.timezone_id"))
            }
            val vm = viewModel(h)
            runCurrent()

            vm.onFieldChange(ProfileTextFieldId.City, "Rome")
            advanceTimeBy(AUTO_SAVE_DEBOUNCE_MS)
            runCurrent()

            val expected = FieldError.Text("Please check the information you entered and try again.")
            assertEquals(expected, vm.uiState.value.dialog)
            assertEquals(emptyMap<ProfileTextFieldId, FieldError>(), vm.uiState.value.fieldErrors)
        }

    @Test
    fun aValidationErrorWithoutTheCodeKeepsTheServerMessage() =
        runTest(main.testDispatcher) {
            val h = signedIn()
            h.api.updateUserProfileHandler = {
                throw h.api.httpError(422, validationBody("basic.first_name", code = null))
            }
            val vm = viewModel(h)
            runCurrent()

            vm.onFieldChange(ProfileTextFieldId.FirstName, "Anna")
            advanceTimeBy(AUTO_SAVE_DEBOUNCE_MS)
            runCurrent()

            val expected = FieldError.Text("Please check the information you entered and try again.")
            assertEquals(expected, vm.uiState.value.dialog)
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

    @Test
    fun updateDetailsAppliesAndSaves() =
        runTest(main.testDispatcher) {
            val h = signedIn()
            val vm = viewModel(h)
            runCurrent()

            vm.openProfessionalSheet()
            assertTrue(vm.uiState.value.professionalSheetOpen)
            vm.applyProfessional("Coach", "Acme")

            val state = vm.uiState.value
            assertFalse(state.professionalSheetOpen)
            assertEquals("Coach", state.snapshot?.jobTitle)
            assertEquals("Acme", state.snapshot?.company)
            assertEquals(0, h.api.updateUserProfileCalls)

            advanceTimeBy(AUTO_SAVE_DEBOUNCE_MS)
            runCurrent()

            assertEquals(
                listOf(UpdateUserProfileRequest(professional = ProfileProfessionalBody("Coach", "Acme"))),
                h.api.updateUserProfileRequests,
            )
        }

    @Test
    fun applyProfessionalClampsToTheLimit() =
        runTest(main.testDispatcher) {
            val h = signedIn()
            val vm = viewModel(h)
            runCurrent()

            vm.applyProfessional("t".repeat(250), "c".repeat(250))

            val snapshot = vm.uiState.value.snapshot
            assertEquals(200, snapshot?.jobTitle?.length)
            assertEquals(200, snapshot?.company?.length)
        }

    @Test
    fun discardChangesLeavesValues() =
        runTest(main.testDispatcher) {
            val h = signedIn(profile = userProfileData(jobTitle = "Old", company = "Co"))
            val vm = viewModel(h)
            runCurrent()

            vm.openProfessionalSheet()
            vm.discardProfessional()
            advanceTimeBy(AUTO_SAVE_DEBOUNCE_MS * 2)
            runCurrent()

            val state = vm.uiState.value
            assertFalse(state.professionalSheetOpen)
            assertEquals("Old", state.snapshot?.jobTitle)
            assertEquals("Co", state.snapshot?.company)
            assertEquals(0, h.api.updateUserProfileCalls)
        }

    private val mixedLanguages =
        userProfileData(
            languages =
                listOf(
                    UserProfileData.LanguageEntry(id = "en", label = "English"),
                    UserProfileData.LanguageEntry(id = null, label = "Klingon"),
                ),
        )

    private val catalogue =
        LanguagesPayload(
            listOf(LanguageItem("en", "English"), LanguageItem("fr", "French"), LanguageItem("de", "German")),
        )

    @Test
    fun languagesSheetLoadsCatalogOnOpen() =
        runTest(main.testDispatcher) {
            val h = signedIn(mixedLanguages)
            h.api.languagesHandler = { catalogue }
            val vm = viewModel(h)
            runCurrent()

            vm.openLanguages()
            val opened = vm.uiState.value.languageSheet
            assertEquals(LanguageCatalog.Loading, opened?.catalog)
            assertEquals(listOf(LanguageItem("en", "English")), opened?.selection)

            runCurrent()
            val loaded = vm.uiState.value.languageSheet
            assertEquals(LanguageCatalog.Loaded(catalogue.languages), loaded?.catalog)
        }

    @Test
    fun languagesFailureShowsFailedAndRetryRefetches() =
        runTest(main.testDispatcher) {
            val h = signedIn(mixedLanguages)
            h.api.languagesHandler = { throw IOException("offline") }
            val vm = viewModel(h)
            runCurrent()

            vm.openLanguages()
            runCurrent()
            assertEquals(
                LanguageCatalog.Failed,
                vm.uiState.value.languageSheet
                    ?.catalog,
            )

            h.api.languagesHandler = { catalogue }
            vm.retryLanguages()
            runCurrent()
            assertEquals(
                LanguageCatalog.Loaded(catalogue.languages),
                vm.uiState.value.languageSheet
                    ?.catalog,
            )
        }

    @Test
    fun catalogIsLoadedOncePerViewModel() =
        runTest(main.testDispatcher) {
            val h = signedIn(mixedLanguages)
            var calls = 0
            h.api.languagesHandler = {
                calls++
                catalogue
            }
            val vm = viewModel(h)
            runCurrent()

            vm.openLanguages()
            runCurrent()
            vm.dismissLanguages()
            vm.openLanguages()
            runCurrent()

            assertEquals(1, calls)
            assertEquals(
                LanguageCatalog.Loaded(catalogue.languages),
                vm.uiState.value.languageSheet
                    ?.catalog,
            )
        }

    @Test
    fun doneSavesIdsAndKeepsCustoms() =
        runTest(main.testDispatcher) {
            val h = signedIn(mixedLanguages)
            h.api.languagesHandler = { catalogue }
            val vm = viewModel(h)
            runCurrent()

            vm.openLanguages()
            runCurrent()
            vm.toggleLanguage(LanguageItem("fr", "French"))
            assertEquals(0, h.api.updateUserProfileCalls)
            vm.applyLanguages()
            assertNull(vm.uiState.value.languageSheet)
            advanceTimeBy(AUTO_SAVE_DEBOUNCE_MS - 1)
            runCurrent()
            assertEquals(0, h.api.updateUserProfileCalls)
            advanceTimeBy(1)
            runCurrent()

            assertEquals(
                listOf(
                    UpdateUserProfileRequest(
                        languages = ProfileLanguagesBody(listOf("en", "fr"), listOf("Klingon")),
                    ),
                ),
                h.api.updateUserProfileRequests,
            )
        }

    @Test
    fun dismissingTheLanguageSheetSendsNothing() =
        runTest(main.testDispatcher) {
            val h = signedIn(mixedLanguages)
            h.api.languagesHandler = { catalogue }
            val vm = viewModel(h)
            runCurrent()

            vm.openLanguages()
            runCurrent()
            vm.toggleLanguage(LanguageItem("fr", "French"))
            vm.dismissLanguages()
            advanceTimeBy(AUTO_SAVE_DEBOUNCE_MS * 2)
            runCurrent()

            assertNull(vm.uiState.value.languageSheet)
            assertEquals(0, h.api.updateUserProfileCalls)
            assertEquals(
                2,
                vm.uiState.value.snapshot
                    ?.languages
                    ?.size,
            )
        }

    @Test
    fun doneWithNothingSelectedKeepsTheListAndExplains() =
        runTest(main.testDispatcher) {
            val h = signedIn(userProfileData())
            h.api.languagesHandler = { catalogue }
            val vm = viewModel(h)
            runCurrent()

            vm.openLanguages()
            runCurrent()
            vm.toggleLanguage(LanguageItem("en", "English"))
            vm.applyLanguages()
            advanceTimeBy(AUTO_SAVE_DEBOUNCE_MS * 2)
            runCurrent()

            val state = vm.uiState.value
            assertNull(state.languageSheet)
            assertEquals(listOf(ProfileLanguage("en", "English")), state.snapshot?.languages)
            assertEquals(FieldError.Res(R.string.profile_settings_languages_keep_one), state.dialog)
            assertEquals(0, h.api.updateUserProfileCalls)
        }

    @Test
    fun doneWithNothingSelectedOnAProfileWithNoLanguagesIsSilent() =
        runTest(main.testDispatcher) {
            val h = signedIn(userProfileData(languages = emptyList()))
            h.api.languagesHandler = { catalogue }
            val vm = viewModel(h)
            runCurrent()

            vm.openLanguages()
            runCurrent()
            vm.applyLanguages()
            advanceTimeBy(AUTO_SAVE_DEBOUNCE_MS * 2)
            runCurrent()

            val state = vm.uiState.value
            assertNull(state.languageSheet)
            assertNull(state.dialog)
            assertEquals(0, h.api.updateUserProfileCalls)
        }

    @Test
    fun deselectingEveryCatalogueLanguageKeepsTheCustomsAndSaves() =
        runTest(main.testDispatcher) {
            val h = signedIn(mixedLanguages)
            h.api.languagesHandler = { catalogue }
            val vm = viewModel(h)
            runCurrent()

            vm.openLanguages()
            runCurrent()
            vm.toggleLanguage(LanguageItem("en", "English"))
            vm.applyLanguages()
            advanceTimeBy(AUTO_SAVE_DEBOUNCE_MS)
            runCurrent()

            assertNull(vm.uiState.value.dialog)
            assertEquals(
                listOf(
                    UpdateUserProfileRequest(languages = ProfileLanguagesBody(emptyList(), listOf("Klingon"))),
                ),
                h.api.updateUserProfileRequests,
            )
        }

    @Test
    fun languageQueryFiltersTheSheet() =
        runTest(main.testDispatcher) {
            val h = signedIn(mixedLanguages)
            h.api.languagesHandler = { catalogue }
            val vm = viewModel(h)
            runCurrent()

            vm.openLanguages()
            runCurrent()
            vm.onLanguageQuery("fre")

            assertEquals(
                listOf(LanguageItem("fr", "French")),
                vm.uiState.value.languageSheet
                    ?.visibleItems,
            )
        }

    @Test
    fun countrySelectionSavesAlpha3() =
        runTest(main.testDispatcher) {
            val h = signedIn(userProfileData(city = "Toronto", stateProvince = "ON", country = "CA"))
            val vm = viewModel(h)
            runCurrent()
            assertEquals(
                "CAN",
                vm.uiState.value.snapshot
                    ?.countryCode,
            )

            vm.openCountry()
            assertTrue(vm.uiState.value.countrySheetOpen)
            vm.onCountryQuery("united st")
            assertEquals(
                listOf("USA"),
                vm.uiState.value.visibleCountries
                    .map { it.code },
            )

            vm.selectCountry("USA")
            assertFalse(vm.uiState.value.countrySheetOpen)
            assertEquals("", vm.uiState.value.countryQuery)
            advanceTimeBy(AUTO_SAVE_DEBOUNCE_MS - 1)
            runCurrent()
            assertEquals(0, h.api.updateUserProfileCalls)
            advanceTimeBy(1)
            runCurrent()

            assertEquals(
                listOf(UpdateUserProfileRequest(location = ProfileLocationBody("Toronto", "ON", "USA"))),
                h.api.updateUserProfileRequests,
            )
        }

    @Test
    fun unsetCountrySeedsEmptyAndSelectingSaves() =
        runTest(main.testDispatcher) {
            val h = signedIn(userProfileData(country = null))
            val vm = viewModel(h)
            runCurrent()
            assertEquals(
                "",
                vm.uiState.value.snapshot
                    ?.countryCode,
            )

            vm.selectCountry("CAN")
            advanceTimeBy(AUTO_SAVE_DEBOUNCE_MS)
            runCurrent()

            assertEquals(
                "CAN",
                h.api.updateUserProfileRequests
                    .single()
                    .location
                    ?.countryCode,
            )
        }

    @Test
    fun dismissingTheCountrySheetSendsNothing() =
        runTest(main.testDispatcher) {
            val h = signedIn()
            val vm = viewModel(h)
            runCurrent()

            vm.openCountry()
            vm.dismissCountry()
            advanceTimeBy(AUTO_SAVE_DEBOUNCE_MS * 2)
            runCurrent()

            assertFalse(vm.uiState.value.countrySheetOpen)
            assertEquals(0, h.api.updateUserProfileCalls)
        }

    @Test
    fun legacyAlpha2CountryIsSentAsAlpha3WithOtherLocationEdits() =
        runTest(main.testDispatcher) {
            val h = signedIn(userProfileData(country = "CA"))
            val vm = viewModel(h)
            runCurrent()

            vm.onFieldChange(ProfileTextFieldId.City, "Paris")
            advanceTimeBy(AUTO_SAVE_DEBOUNCE_MS)
            runCurrent()

            assertEquals(
                "CAN",
                h.api.updateUserProfileRequests
                    .single()
                    .location
                    ?.countryCode,
            )
        }

    // ---- process death and recreation (CONTEXT D-12) ----

    private fun miaHandle() =
        SavedStateHandle(mapOf("client_settings_first_name" to "Mia", "client_settings_touched" to true))

    @Test
    fun typedFieldsRestored() =
        runTest(main.testDispatcher) {
            val h = signedIn()
            h.api.languagesHandler = { catalogue }
            val handle = SavedStateHandle()
            val first = viewModel(h, handle)
            runCurrent()
            first.onFieldChange(ProfileTextFieldId.FirstName, "Mia")
            first.onFieldChange(ProfileTextFieldId.City, "Lyon")
            first.selectCountry("FR")
            first.openLanguages()
            runCurrent()
            first.toggleLanguage(LanguageItem("fr", "French"))
            first.toggleLanguage(LanguageItem("de", "German"))
            first.applyLanguages()

            val second = viewModel(h, copyOf(handle))
            runCurrent()

            val restored = second.uiState.value.snapshot
            assertEquals("Mia", restored?.firstName)
            assertEquals("Lyon", restored?.city)
            assertEquals("FRA", restored?.countryCode)
            assertEquals(listOf("en", "fr", "de"), restored?.languages?.map { it.id })
            assertEquals(first.uiState.value.snapshot, restored)
        }

    @Test
    fun restoredBeatsServer() =
        runTest(main.testDispatcher) {
            val h = SignupTestHarness(this, tmp, main.testDispatcher)
            h.signIn()
            runCurrent()
            h.api.userProfileHandler = { seeded }
            val handle = miaHandle()

            val vm = viewModel(h, handle)
            runCurrent()

            val snapshot = vm.uiState.value.snapshot
            assertEquals("Mia", snapshot?.firstName)
            assertEquals("Lee", snapshot?.lastName)
        }

    @Test
    fun untouchedSeedsFromServer() =
        runTest(main.testDispatcher) {
            val h = SignupTestHarness(this, tmp, main.testDispatcher)
            h.signIn()
            runCurrent()
            h.api.userProfileHandler = { seeded }

            val vm = viewModel(h, SavedStateHandle())
            runCurrent()

            assertEquals(
                "Ana",
                vm.uiState.value.snapshot
                    ?.firstName,
            )
            assertEquals(0, h.api.updateUserProfileCalls)
        }

    @Test
    fun restoredEditIsAutoSaved() =
        runTest(main.testDispatcher) {
            val h = signedIn(after = userProfileData(firstName = "Mia", lastName = "Lee", bio = "Hi there"))
            val handle = miaHandle()

            viewModel(h, handle)
            runCurrent()
            assertEquals(0, h.api.updateUserProfileCalls)
            advanceTimeBy(AUTO_SAVE_DEBOUNCE_MS)
            runCurrent()

            assertEquals(
                listOf(UpdateUserProfileRequest(basic = ProfileBasicBody("Mia", "Lee", "Hi there"))),
                h.api.updateUserProfileRequests,
            )
        }

    @Test
    fun onlyBundleTypes() =
        runTest(main.testDispatcher) {
            val h = signedIn()
            h.api.languagesHandler = { catalogue }
            val handle = SavedStateHandle()
            val vm = viewModel(h, handle)
            runCurrent()
            vm.onFieldChange(ProfileTextFieldId.Bio, "About me")
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

    // ---- G-05-6: never send an empty name ----

    private val firstNameRequired = FieldError.Res(R.string.profile_settings_first_name_required)
    private val lastNameRequired = FieldError.Res(R.string.profile_settings_last_name_required)

    @Test
    fun bioEditOnAnEmptyLastNameAccountSendsNoLastNameKey() =
        runTest(main.testDispatcher) {
            val h = signedIn(userProfileData(firstName = "Ana", lastName = "", bio = ""))
            val vm = viewModel(h)
            runCurrent()

            vm.onFieldChange(ProfileTextFieldId.Bio, "Hello")
            advanceTimeBy(AUTO_SAVE_DEBOUNCE_MS)
            runCurrent()

            assertEquals(
                listOf(UpdateUserProfileRequest(basic = ProfileBasicBody("Ana", null, "Hello"))),
                h.api.updateUserProfileRequests,
            )
            assertEquals(emptyMap<ProfileTextFieldId, FieldError>(), vm.uiState.value.fieldErrors)
            assertNull(vm.uiState.value.dialog)
        }

    @Test
    fun firstNameEditOnAnEmptyLastNameAccountSendsFirstNameOnly() =
        runTest(main.testDispatcher) {
            val h = signedIn(userProfileData(firstName = "Ana", lastName = "", bio = ""))
            val vm = viewModel(h)
            runCurrent()

            vm.onFieldChange(ProfileTextFieldId.FirstName, "Ada")
            advanceTimeBy(AUTO_SAVE_DEBOUNCE_MS)
            runCurrent()

            assertEquals(
                listOf(UpdateUserProfileRequest(basic = ProfileBasicBody("Ada", null, ""))),
                h.api.updateUserProfileRequests,
            )
        }

    @Test
    fun clearingASetFirstNameHoldsTheServerValueAndShowsTheRequiredMessage() =
        runTest(main.testDispatcher) {
            val h = signedIn()
            val vm = viewModel(h)
            runCurrent()

            vm.onFieldChange(ProfileTextFieldId.FirstName, "")
            advanceTimeBy(AUTO_SAVE_DEBOUNCE_MS)
            runCurrent()

            assertEquals(0, h.api.updateUserProfileCalls)
            assertEquals(mapOf(ProfileTextFieldId.FirstName to firstNameRequired), vm.uiState.value.fieldErrors)
            assertEquals(AutoSaveStatus.Idle, vm.uiState.value.status)

            // Another edit while the name is cleared goes out without the name key; the message stays.
            vm.onFieldChange(ProfileTextFieldId.Bio, "New bio")
            advanceTimeBy(AUTO_SAVE_DEBOUNCE_MS)
            runCurrent()
            assertEquals(
                listOf(UpdateUserProfileRequest(basic = ProfileBasicBody(null, "Lee", "New bio"))),
                h.api.updateUserProfileRequests,
            )
            assertEquals(mapOf(ProfileTextFieldId.FirstName to firstNameRequired), vm.uiState.value.fieldErrors)

            // Typing a name again clears the message and sends the name.
            vm.onFieldChange(ProfileTextFieldId.FirstName, "Anna")
            assertEquals(emptyMap<ProfileTextFieldId, FieldError>(), vm.uiState.value.fieldErrors)
            advanceTimeBy(AUTO_SAVE_DEBOUNCE_MS)
            runCurrent()
            val lastRequest = h.api.updateUserProfileRequests.last()
            assertEquals(ProfileBasicBody("Anna", "Lee", "New bio"), lastRequest.basic)
        }

    @Test
    fun clearingASetLastNameHoldsTheServerValueAndShowsTheRequiredMessage() =
        runTest(main.testDispatcher) {
            val h = signedIn()
            val vm = viewModel(h)
            runCurrent()

            vm.onFieldChange(ProfileTextFieldId.LastName, "  ")
            advanceTimeBy(AUTO_SAVE_DEBOUNCE_MS)
            runCurrent()

            assertEquals(0, h.api.updateUserProfileCalls)
            assertEquals(mapOf(ProfileTextFieldId.LastName to lastNameRequired), vm.uiState.value.fieldErrors)
        }

    @Test
    fun anEmptyNameOnAnEmptyAccountShowsNoMessage() =
        runTest(main.testDispatcher) {
            val h = signedIn(userProfileData(firstName = "Ana", lastName = "", bio = ""))
            val vm = viewModel(h)
            runCurrent()

            vm.onFieldChange(ProfileTextFieldId.LastName, "")
            vm.onFieldChange(ProfileTextFieldId.LastName, "Lee")
            vm.onFieldChange(ProfileTextFieldId.LastName, "")

            assertEquals(emptyMap<ProfileTextFieldId, FieldError>(), vm.uiState.value.fieldErrors)
        }

    @Test
    fun aClearedNameRestoredAfterRecreationShowsTheMessageAndSendsNothing() =
        runTest(main.testDispatcher) {
            val h = signedIn()
            val handle = SavedStateHandle(mapOf("client_settings_first_name" to "", "client_settings_touched" to true))

            val vm = viewModel(h, handle)
            runCurrent()
            advanceTimeBy(AUTO_SAVE_DEBOUNCE_MS)
            runCurrent()

            assertEquals(mapOf(ProfileTextFieldId.FirstName to firstNameRequired), vm.uiState.value.fieldErrors)
            assertEquals(0, h.api.updateUserProfileCalls)
        }
}
