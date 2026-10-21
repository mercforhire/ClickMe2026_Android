@file:OptIn(ExperimentalCoroutinesApi::class)

package com.uptrendinvestments.clickmeapp.screens.signup.basicinfo

import androidx.lifecycle.SavedStateHandle
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.buttons.SaveState
import com.uptrendinvestments.clickmeapp.model.requests.SetupLocation
import com.uptrendinvestments.clickmeapp.model.responses.EmailStatusData
import com.uptrendinvestments.clickmeapp.model.responses.ExpertLanguage
import com.uptrendinvestments.clickmeapp.model.responses.LanguageItem
import com.uptrendinvestments.clickmeapp.model.responses.LanguagesPayload
import com.uptrendinvestments.clickmeapp.model.responses.SetupExpertProfileData
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.SignupTestHarness
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
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

class SignupBasicInfoViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    @get:Rule
    val tmp = TemporaryFolder()

    private val english = LanguageItem("en", "English")
    private val french = LanguageItem("fr", "French")
    private val german = LanguageItem("de", "German")

    private fun harness(scope: TestScope) =
        SignupTestHarness(scope, tmp, main.testDispatcher).also {
            it.api.setupProfileHandler = { SetupExpertProfileData() }
            it.api.languagesHandler = { LanguagesPayload(listOf(english, french, german)) }
        }

    private fun viewModel(
        h: SignupTestHarness,
        handle: SavedStateHandle = SavedStateHandle(),
    ) = SignupBasicInfoViewModel(h.repository, h.api, handle)

    /** A fresh signup: the hydrate succeeds and the server has no languages, so English is the default. */
    private suspend fun hydrateEmpty(h: SignupTestHarness) {
        h.api.expertProfileHandler = { h.emptyProfile() }
        h.api.emailStatusHandler = { EmailStatusData(verified = false) }
        h.repository.hydrate()
    }

    private suspend fun hydrateAda(h: SignupTestHarness) {
        h.api.expertProfileHandler = { h.fullProfile(languages = listOf(ExpertLanguage("fr", "French"))) }
        h.api.emailStatusHandler = { EmailStatusData(verified = false) }
        h.repository.hydrate()
    }

    @Test
    fun emptyDraftStartsBlankWithEnglishSelected() =
        runTest(main.testDispatcher) {
            val h = harness(this)
            hydrateEmpty(h)
            val vm = viewModel(h)

            val s = vm.uiState.value
            assertEquals("", s.firstName)
            assertEquals("", s.city)
            assertEquals("", s.countryCode)
            assertEquals(listOf(english), s.languages)
            assertNull(s.countryName)
        }

    @Test
    fun hydratedDraftPrefillsWithoutAddingEnglish() =
        runTest(main.testDispatcher) {
            val h = harness(this)
            hydrateAda(h)

            val s = viewModel(h).uiState.value

            assertEquals("Ada", s.firstName)
            assertEquals("Toronto", s.city)
            assertEquals("ON", s.provinceState)
            assertEquals("CAN", s.countryCode)
            assertEquals(listOf(french), s.languages)
        }

    @Test
    fun lateHydrateReseedsUntilTheFirstEdit() =
        runTest(main.testDispatcher) {
            val h = harness(this)
            val vm = viewModel(h)
            runCurrent()

            hydrateAda(h)
            runCurrent()
            assertEquals("Ada", vm.uiState.value.firstName)

            vm.onFirstNameChange("Grace")
            h.api.expertProfileHandler = { h.fullProfile(firstName = "Other") }
            h.repository.hydrate()
            runCurrent()

            assertEquals("Grace", vm.uiState.value.firstName)
        }

    @Test
    fun countrySheetSelectsFiltersAndCloses() =
        runTest(main.testDispatcher) {
            val vm = viewModel(harness(this))

            vm.openCountrySheet()
            assertTrue(vm.uiState.value.showCountrySheet)
            vm.onCountryQueryChange("can")
            val names =
                vm.uiState.value.visibleCountries
                    .map { it.name }
            assertTrue(names.isNotEmpty())
            assertTrue(names.all { it.contains("can", ignoreCase = true) })
            assertTrue("Canada" in names)

            vm.selectCountry("CAN")

            val s = vm.uiState.value
            assertEquals("CAN", s.countryCode)
            assertEquals("Canada", s.countryName)
            assertFalse(s.showCountrySheet)
        }

    @Test
    fun languageSheetLoadsOnceAndReloadsOnRetryAfterFailure() =
        runTest(main.testDispatcher) {
            val h = harness(this)
            val vm = viewModel(h)

            vm.openLanguageSheet()
            assertEquals(
                LanguageCatalog.Loading,
                vm.uiState.value.languageSheet
                    ?.catalog,
            )
            advanceUntilIdle()
            assertEquals(
                LanguageCatalog.Loaded(listOf(english, french, german)),
                vm.uiState.value.languageSheet
                    ?.catalog,
            )
            vm.dismissLanguageSheet()
            vm.openLanguageSheet()
            advanceUntilIdle()
            assertEquals(1, h.api.languagesCalls)
        }

    @Test
    fun languageLoadFailureThenRetryLoads() =
        runTest(main.testDispatcher) {
            val h = harness(this)
            h.api.languagesHandler = { throw IOException("offline") }
            val vm = viewModel(h)

            vm.openLanguageSheet()
            advanceUntilIdle()
            assertEquals(
                LanguageCatalog.Failed,
                vm.uiState.value.languageSheet
                    ?.catalog,
            )

            h.api.languagesHandler = { LanguagesPayload(listOf(english)) }
            vm.retryLanguages()
            advanceUntilIdle()

            assertEquals(
                LanguageCatalog.Loaded(listOf(english)),
                vm.uiState.value.languageSheet
                    ?.catalog,
            )
            assertEquals(2, h.api.languagesCalls)
        }

    @Test
    fun languageSelectionKeepsTapOrderAndConfirmApplies() =
        runTest(main.testDispatcher) {
            val h = harness(this)
            hydrateEmpty(h)
            val vm = viewModel(h)
            vm.openLanguageSheet()
            advanceUntilIdle()

            vm.toggleLanguage(french)
            vm.toggleLanguage(german)
            assertEquals(
                listOf(english, french, german),
                vm.uiState.value.languageSheet
                    ?.selection,
            )
            assertEquals(listOf(english), vm.uiState.value.languages)

            vm.toggleLanguage(english)
            assertEquals(
                listOf(french, german),
                vm.uiState.value.languageSheet
                    ?.selection,
            )
            vm.toggleLanguage(english)
            vm.confirmLanguageSheet()

            assertEquals(listOf(french, german, english), vm.uiState.value.languages)
            assertNull(vm.uiState.value.languageSheet)
        }

    @Test
    fun languageSearchFiltersAndDismissDiscardsSelection() =
        runTest(main.testDispatcher) {
            val h = harness(this)
            hydrateEmpty(h)
            val vm = viewModel(h)
            vm.openLanguageSheet()
            advanceUntilIdle()

            vm.onLanguageQueryChange("FR")
            assertEquals(
                listOf(french),
                vm.uiState.value.languageSheet
                    ?.visibleItems,
            )

            vm.toggleLanguage(french)
            vm.dismissLanguageSheet()

            assertEquals(listOf(english), vm.uiState.value.languages)
            assertNull(vm.uiState.value.languageSheet)
        }

    @Test
    fun removeLanguageDropsTheChip() =
        runTest(main.testDispatcher) {
            val h = harness(this)
            hydrateEmpty(h)
            val vm = viewModel(h)
            assertEquals(listOf(english), vm.uiState.value.languages)

            vm.removeLanguage("en")

            assertTrue(
                vm.uiState.value.languages
                    .isEmpty(),
            )
        }

    @Test
    fun savePatchesOnlyFilledFieldsAndHoldsSavedFor600ms() =
        runTest(main.testDispatcher) {
            val h = harness(this)
            hydrateEmpty(h)
            val vm = viewModel(h)
            vm.onFirstNameChange("Ada")
            vm.selectCountry("CAN")

            vm.save()
            assertEquals(SaveState.Saving, vm.uiState.value.saveState)
            runCurrent()

            assertEquals(1, h.api.setupProfileCalls)
            val request = h.api.setupProfileRequests.single()
            assertEquals("Ada", request.firstName)
            assertEquals(SetupLocation(countryCode = "CAN"), request.location)
            assertEquals(listOf("en"), request.languages)
            assertEquals(SaveState.Saved, vm.uiState.value.saveState)
            advanceTimeBy(599)
            assertFalse(vm.saved.value)
            advanceTimeBy(2)
            assertTrue(vm.saved.value)

            vm.onSavedConsumed()
            assertFalse(vm.saved.value)
            assertEquals(SaveState.Idle, vm.uiState.value.saveState)
        }

    @Test
    fun http422MapsEachFieldInline() =
        runTest(main.testDispatcher) {
            val h = harness(this)
            h.api.setupProfileHandler = {
                throw h.api.httpError(
                    422,
                    """{"errors":[{"field":"first_name","message":"Too long"},
                        {"field":"location.city","message":"Bad city"}]}""",
                )
            }
            // English is the default after an empty hydrate, so Save has something to send.
            hydrateEmpty(h)
            val vm = viewModel(h)

            vm.save()
            advanceUntilIdle()

            val s = vm.uiState.value
            assertEquals(FieldError.Text("Too long"), s.firstNameError)
            assertEquals(FieldError.Text("Bad city"), s.cityError)
            assertNull(s.countryError)
            assertNull(s.dialogError)
            assertEquals(SaveState.Idle, s.saveState)
        }

    @Test
    fun http422CountryFieldMapsToCountryError() =
        runTest(main.testDispatcher) {
            val h = harness(this)
            h.api.setupProfileHandler = {
                throw h.api.httpError(422, """{"errors":[{"field":"location.country_code","message":"Bad country"}]}""")
            }
            // English is the default after an empty hydrate, so Save has something to send.
            hydrateEmpty(h)
            val vm = viewModel(h)

            vm.save()
            advanceUntilIdle()

            assertEquals(FieldError.Text("Bad country"), vm.uiState.value.countryError)
            assertNull(vm.uiState.value.dialogError)
        }

    @Test
    fun http422UnknownFieldShowsDialog() =
        runTest(main.testDispatcher) {
            val h = harness(this)
            h.api.setupProfileHandler = {
                throw h.api.httpError(422, """{"errors":[{"field":"surprise","message":"Odd"}]}""")
            }
            // English is the default after an empty hydrate, so Save has something to send.
            hydrateEmpty(h)
            val vm = viewModel(h)

            vm.save()
            advanceUntilIdle()

            assertEquals(FieldError.Text("Odd"), vm.uiState.value.dialogError)
            assertEquals(SaveState.Idle, vm.uiState.value.saveState)
        }

    @Test
    fun networkFailureShowsNetworkDialog() =
        runTest(main.testDispatcher) {
            val h = harness(this)
            h.api.setupProfileHandler = { throw IOException("offline") }
            // English is the default after an empty hydrate, so Save has something to send.
            hydrateEmpty(h)
            val vm = viewModel(h)

            vm.save()
            advanceUntilIdle()

            assertEquals(FieldError.Res(R.string.login_error_network), vm.uiState.value.dialogError)
            assertEquals(SaveState.Idle, vm.uiState.value.saveState)
            vm.dismissDialog()
            assertNull(vm.uiState.value.dialogError)
        }

    @Test
    fun editingAFieldClearsItsServerError() =
        runTest(main.testDispatcher) {
            val h = harness(this)
            h.api.setupProfileHandler = {
                throw h.api.httpError(422, """{"errors":[{"field":"location.city","message":"Bad city"}]}""")
            }
            // English is the default after an empty hydrate, so Save has something to send.
            hydrateEmpty(h)
            val vm = viewModel(h)
            vm.save()
            advanceUntilIdle()

            vm.onCityChange("Toronto")

            assertNull(vm.uiState.value.cityError)
        }

    @Test
    fun secondSaveWhileSavingMakesNoSecondCall() =
        runTest(main.testDispatcher) {
            val h = harness(this)
            val gate = CompletableDeferred<Unit>()
            h.api.setupProfileHandler = {
                gate.await()
                SetupExpertProfileData()
            }
            // English is the default after an empty hydrate, so Save has something to send.
            hydrateEmpty(h)
            val vm = viewModel(h)

            vm.save()
            runCurrent()
            vm.save()
            runCurrent()
            gate.complete(Unit)
            advanceUntilIdle()

            assertEquals(1, h.api.setupProfileCalls)
        }

    @Test
    fun formIsRestoredFromSavedState() =
        runTest(main.testDispatcher) {
            val handle =
                SavedStateHandle(
                    mapOf(
                        "basic_first_name" to "Grace",
                        "basic_city" to "Paris",
                        "basic_province" to "IDF",
                        "basic_country" to "FRA",
                        "basic_language_ids" to arrayListOf("fr", "de"),
                        "basic_language_labels" to arrayListOf("French", "German"),
                        "basic_touched" to true,
                        "basic_languages_touched" to true,
                    ),
                )

            val s = viewModel(harness(this), handle).uiState.value

            assertEquals("Grace", s.firstName)
            assertEquals("Paris", s.city)
            assertEquals("IDF", s.provinceState)
            assertEquals("FRA", s.countryCode)
            assertEquals(listOf(french, german), s.languages)
        }

    // ---- WR-03: an untouched server language list is never replaced ----

    @Test
    fun failedHydrateShowsNoEnglishAndSaveOmitsLanguages() =
        runTest(main.testDispatcher) {
            val h = harness(this)
            h.api.expertProfileHandler = { throw IOException("offline") }
            h.api.emailStatusHandler = { throw IOException("offline") }
            h.repository.hydrate()
            val vm = viewModel(h)
            assertTrue(
                vm.uiState.value.languages
                    .isEmpty(),
            )

            vm.onFirstNameChange("Ada")
            vm.save()
            runCurrent()

            val request = h.api.setupProfileRequests.single()
            assertEquals("Ada", request.firstName)
            assertNull(request.languages)
        }

    @Test
    fun textEditBeforeALateHydrateStillShowsAndKeepsTheServerLanguages() =
        runTest(main.testDispatcher) {
            val h = harness(this)
            val vm = viewModel(h)
            runCurrent()
            vm.onFirstNameChange("Grace")

            hydrateAda(h)
            runCurrent()

            assertEquals("Grace", vm.uiState.value.firstName)
            assertEquals(listOf(french), vm.uiState.value.languages)

            vm.save()
            runCurrent()
            val request = h.api.setupProfileRequests.single()
            assertEquals("Grace", request.firstName)
            assertNull(request.languages)
            assertEquals(listOf(french), h.repository.draft.value.languages)
        }

    @Test
    fun untouchedHydratedLanguagesAreOmittedFromSave() =
        runTest(main.testDispatcher) {
            val h = harness(this)
            hydrateAda(h)
            val vm = viewModel(h)

            vm.save()
            runCurrent()

            assertNull(
                h.api.setupProfileRequests
                    .single()
                    .languages,
            )
        }

    @Test
    fun changedLanguagesAreSentInTapOrder() =
        runTest(main.testDispatcher) {
            val h = harness(this)
            hydrateAda(h)
            val vm = viewModel(h)
            vm.openLanguageSheet()
            advanceUntilIdle()
            vm.toggleLanguage(german)
            vm.confirmLanguageSheet()

            vm.save()
            runCurrent()

            assertEquals(
                listOf("fr", "de"),
                h.api.setupProfileRequests
                    .single()
                    .languages,
            )
        }

    @Test
    fun languageEditsSurviveALateHydrateButTextStillReseeds() =
        runTest(main.testDispatcher) {
            val h = harness(this)
            hydrateEmpty(h)
            val vm = viewModel(h)
            vm.removeLanguage("en")

            hydrateAda(h)
            runCurrent()

            assertEquals("Ada", vm.uiState.value.firstName)
            assertTrue(
                vm.uiState.value.languages
                    .isEmpty(),
            )
        }

    @Test
    fun textOnlySavedStateReseedsTheLanguagesFromTheDraft() =
        runTest(main.testDispatcher) {
            val h = harness(this)
            hydrateAda(h)
            val handle =
                SavedStateHandle(
                    mapOf(
                        "basic_first_name" to "Grace",
                        "basic_language_ids" to arrayListOf("en"),
                        "basic_language_labels" to arrayListOf("English"),
                        "basic_touched" to true,
                    ),
                )

            val s = viewModel(h, handle).uiState.value

            assertEquals("Grace", s.firstName)
            assertEquals(listOf(french), s.languages)
        }

    // ---- WR-04: a cleared optional field is cleared on the server ----

    @Test
    fun clearingTheProvinceSavesItAsEmptyAndItDoesNotComeBack() =
        runTest(main.testDispatcher) {
            val h = harness(this)
            hydrateAda(h)
            val vm = viewModel(h)
            assertEquals("ON", vm.uiState.value.provinceState)

            vm.onProvinceChange("")
            vm.save()
            runCurrent()

            assertEquals(
                "",
                h.api.setupProfileRequests
                    .single()
                    .location
                    ?.provinceState,
            )
            assertEquals("", viewModel(h).uiState.value.provinceState)
        }

    @Test
    fun aCityTheUserNeverSawIsNotClearedByALateHydrate() =
        runTest(main.testDispatcher) {
            val h = harness(this)
            val vm = viewModel(h)
            runCurrent()
            vm.onFirstNameChange("Grace")
            hydrateAda(h)
            runCurrent()

            vm.save()
            runCurrent()

            assertNull(
                h.api.setupProfileRequests
                    .single()
                    .location,
            )
            assertEquals("Toronto", h.repository.draft.value.city)
        }

    @Test
    fun editsAreMirroredToSavedState() =
        runTest(main.testDispatcher) {
            val handle = SavedStateHandle()
            val vm = viewModel(harness(this), handle)

            vm.onFirstNameChange("Grace")
            vm.removeLanguage("en")

            assertEquals("Grace", handle.get<String>("basic_first_name"))
            assertEquals(true, handle.get<Boolean>("basic_touched"))
            assertEquals(arrayListOf<String>(), handle.get<ArrayList<String>>("basic_language_ids"))
        }
}
