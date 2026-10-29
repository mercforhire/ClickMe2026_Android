@file:OptIn(ExperimentalCoroutinesApi::class)

package com.uptrendinvestments.clickmeapp.screens.expertmode.taxprofile

import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.buttons.SaveState
import com.uptrendinvestments.clickmeapp.model.requests.SaveTaxProfileRequest
import com.uptrendinvestments.clickmeapp.model.requests.TaxAddressBody
import com.uptrendinvestments.clickmeapp.model.responses.SaveTaxProfileData
import com.uptrendinvestments.clickmeapp.model.responses.TaxProfileAddress
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.taxProfile
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.time.LocalDate
import java.time.ZoneOffset

class ExpertTaxProfileViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val api = FakeClickMeApi()
    private val repository = TaxProfileRepository(api)

    private fun serverProfile(tinOnFile: Boolean = false) {
        api.taxProfileHandler = {
            taxProfile(
                tinOnFile = tinOnFile,
                legalName = "Ana Lee",
                dob = "1990-01-02",
                address =
                    TaxProfileAddress(
                        line1 = "1 Main",
                        city = "Toronto",
                        province = "ON",
                        postalCode = "M1M1M1",
                    ),
            )
        }
    }

    private fun loaded(scope: TestScope): ExpertTaxProfileViewModel {
        val vm = ExpertTaxProfileViewModel(repository)
        scope.runCurrent()
        return vm
    }

    @Test
    fun prefillFromServer() =
        runTest(main.testDispatcher) {
            serverProfile()
            val vm = loaded(this)

            val state = vm.uiState.value
            assertEquals(TaxProfileLoad.Ready, state.load)
            assertEquals("Ana Lee", state.form.legalName)
            assertEquals("1990-01-02", state.form.dob)
            assertEquals("1 Main", state.form.line1)
            assertEquals("Toronto", state.form.city)
            assertEquals("ON", state.form.region)
            assertEquals("M1M1M1", state.form.postalCode)
            assertEquals("CA", state.form.country)
            assertEquals("", state.form.tin)
            assertFalse(state.tinOnFile)
            assertEquals(1, api.taxProfileCalls)
        }

    @Test
    fun anEmptyServerProfileLeavesTheFormBlankWithTheDefaultCountry() =
        runTest(main.testDispatcher) {
            api.taxProfileHandler = { taxProfile() }
            val vm = loaded(this)

            assertEquals(TaxProfileLoad.Ready, vm.uiState.value.load)
            assertEquals(TaxProfileForm(), vm.uiState.value.form)
            assertFalse(vm.uiState.value.canSave)
        }

    @Test
    fun saveSendsStrictBodyAndClearsTin() =
        runTest(main.testDispatcher) {
            serverProfile()
            api.saveTaxProfileHandler = { SaveTaxProfileData(saved = true) }
            val vm = loaded(this)

            vm.onFieldChange(TaxProfileField.Tin, "123456789")
            vm.save()
            runCurrent()

            assertEquals(
                SaveTaxProfileRequest(
                    legalName = "Ana Lee",
                    address =
                        TaxAddressBody(
                            line1 = "1 Main",
                            line2 = null,
                            city = "Toronto",
                            region = "ON",
                            postalCode = "M1M1M1",
                            country = "CA",
                        ),
                    dob = "1990-01-02",
                    tin = "123456789",
                    financialAccountIdentifier = null,
                ),
                api.saveTaxProfileRequests.single(),
            )
            val state = vm.uiState.value
            assertEquals("", state.form.tin)
            assertTrue(state.tinOnFile)
            assertTrue(state.savedEvent)
            assertEquals(SaveState.Idle, state.saveState)
            assertNull(state.banner)
            assertNull(state.dialog)
        }

    @Test
    fun blankTinIsOmitted() =
        runTest(main.testDispatcher) {
            serverProfile()
            api.saveTaxProfileHandler = { SaveTaxProfileData(saved = false, tinRequired = true) }
            val vm = loaded(this)

            vm.onFieldChange(TaxProfileField.Tin, "   ")
            vm.save()
            runCurrent()

            assertNull(api.saveTaxProfileRequests.single().tin)
        }

    @Test
    fun trimsValuesAndSendsOptionalOnesOnlyWhenSet() =
        runTest(main.testDispatcher) {
            serverProfile()
            api.saveTaxProfileHandler = { SaveTaxProfileData(saved = true) }
            val vm = loaded(this)

            vm.onFieldChange(TaxProfileField.LegalName, "  Ana Lee  ")
            vm.onFieldChange(TaxProfileField.Line2, " Unit 4 ")
            vm.onFieldChange(TaxProfileField.FinancialAccount, " 000123 ")
            vm.save()
            runCurrent()

            val sent = api.saveTaxProfileRequests.single()
            assertEquals("Ana Lee", sent.legalName)
            assertEquals("Unit 4", sent.address.line2)
            assertEquals("000123", sent.financialAccountIdentifier)
        }

    @Test
    fun canSaveNeedsSevenFields() {
        val full =
            TaxProfileForm(
                legalName = "Ana Lee",
                dob = "1990-01-02",
                line1 = "1 Main",
                city = "Toronto",
                region = "ON",
                postalCode = "M1M1M1",
                country = "CA",
            )
        assertTrue(canSaveTaxProfile(full))

        val blanked =
            mapOf(
                "legalName" to full.copy(legalName = " "),
                "dob" to full.copy(dob = ""),
                "line1" to full.copy(line1 = ""),
                "city" to full.copy(city = "  "),
                "region" to full.copy(region = ""),
                "postalCode" to full.copy(postalCode = ""),
                "country" to full.copy(country = ""),
            )
        blanked.forEach { (name, form) -> assertFalse("$name blank must disable Save", canSaveTaxProfile(form)) }

        assertTrue(canSaveTaxProfile(full.copy(line2 = "", financialAccount = "", tin = "")))
    }

    @Test
    fun theTinNeverAppearsInThePrintedForm() {
        val form = TaxProfileForm(legalName = "Ana Lee", tin = "123456789", dob = "1990-01-02")

        assertFalse(form.toString().contains("123456789"))
        assertFalse(form.toString().contains("Ana Lee"))
    }

    // ---- Task 2: tin_required prompt, field errors, failures, locking, date of birth ----

    private fun filledViewModel(scope: TestScope): ExpertTaxProfileViewModel {
        serverProfile()
        return loaded(scope)
    }

    @Test
    fun tinRequiredIsAPrompt() =
        runTest(main.testDispatcher) {
            api.saveTaxProfileHandler = {
                SaveTaxProfileData(saved = false, tinRequired = true, message = "TIN required for payouts")
            }
            val vm = filledViewModel(this)
            val before = vm.uiState.value.form

            vm.save()
            runCurrent()

            val state = vm.uiState.value
            assertEquals(FieldError.Text("TIN required for payouts"), state.banner)
            assertNull(state.dialog)
            assertTrue(state.focusTinEvent)
            assertFalse(state.tinOnFile)
            assertFalse(state.savedEvent)
            assertEquals(before, state.form)
            assertEquals(SaveState.Idle, state.saveState)

            vm.consumeFocusTin()
            assertFalse(vm.uiState.value.focusTinEvent)
        }

    @Test
    fun tinRequiredWithoutAMessageUsesTheFallbackBanner() =
        runTest(main.testDispatcher) {
            api.saveTaxProfileHandler = { SaveTaxProfileData(saved = false, tinRequired = true) }
            val vm = filledViewModel(this)

            vm.save()
            runCurrent()

            assertEquals(FieldError.Res(R.string.tax_profile_tin_required_fallback), vm.uiState.value.banner)
        }

    @Test
    fun anUnsavedAnswerWithoutTinRequiredIsNotSilentlyIgnored() =
        runTest(main.testDispatcher) {
            api.saveTaxProfileHandler = { SaveTaxProfileData(saved = false, message = "Try later") }
            val vm = filledViewModel(this)

            vm.save()
            runCurrent()

            assertEquals(FieldError.Text("Try later"), vm.uiState.value.dialog)
            assertNull(vm.uiState.value.banner)
        }

    @Test
    fun validationErrorsInline() =
        runTest(main.testDispatcher) {
            val vm = filledViewModel(this)

            api.saveTaxProfileHandler = {
                throw api.httpError(422, """{"errors":[{"field":"tin","message":"TIN format is invalid"}]}""")
            }
            vm.save()
            runCurrent()
            assertEquals("TIN format is invalid", vm.uiState.value.fieldErrors[TaxProfileField.Tin])
            assertNull(vm.uiState.value.dialog)
            assertTrue(vm.uiState.value.focusTinEvent)

            api.saveTaxProfileHandler = {
                throw api.httpError(422, """{"errors":[{"field":"dob","message":"Date of birth is invalid"}]}""")
            }
            vm.save()
            runCurrent()
            assertEquals("Date of birth is invalid", vm.uiState.value.fieldErrors[TaxProfileField.Dob])
            assertNull(vm.uiState.value.fieldErrors[TaxProfileField.Tin])

            api.saveTaxProfileHandler = {
                throw api.httpError(
                    422,
                    """{"errors":[{"field":"address.postal_code","message":"Postal code is invalid"}]}""",
                )
            }
            vm.save()
            runCurrent()
            assertEquals("Postal code is invalid", vm.uiState.value.fieldErrors[TaxProfileField.PostalCode])
        }

    @Test
    fun editingAFieldClearsItsInlineError() =
        runTest(main.testDispatcher) {
            val vm = filledViewModel(this)
            api.saveTaxProfileHandler = {
                throw api.httpError(422, """{"errors":[{"field":"tin","message":"TIN format is invalid"}]}""")
            }
            vm.save()
            runCurrent()

            vm.onFieldChange(TaxProfileField.Tin, "1")

            assertNull(vm.uiState.value.fieldErrors[TaxProfileField.Tin])
        }

    @Test
    fun anUnknownFieldErrorOpensTheSaveDialogWithTheServerMessage() =
        runTest(main.testDispatcher) {
            val vm = filledViewModel(this)
            api.saveTaxProfileHandler = {
                throw api.httpError(422, """{"errors":[{"field":"surprise","message":"Something odd"}]}""")
            }

            vm.save()
            runCurrent()

            assertEquals(FieldError.Text("Something odd"), vm.uiState.value.dialog)
            assertEquals(emptyMap<TaxProfileField, String>(), vm.uiState.value.fieldErrors)
            assertEquals(SaveState.Idle, vm.uiState.value.saveState)

            vm.dismissDialog()
            assertNull(vm.uiState.value.dialog)
        }

    @Test
    fun aSaveNetworkFailureOpensTheDialogWithTheNetworkLine() =
        runTest(main.testDispatcher) {
            val vm = filledViewModel(this)
            api.saveTaxProfileHandler = { throw IOException("offline") }

            vm.save()
            runCurrent()

            assertEquals(FieldError.Res(R.string.login_error_network), vm.uiState.value.dialog)
            assertEquals("Ana Lee", vm.uiState.value.form.legalName)
        }

    @Test
    fun aFailedLoadShowsTheServerMessageAndRetryLoadsAgain() =
        runTest(main.testDispatcher) {
            api.taxProfileHandler = { throw api.httpError(500, """{"status":"error","message":"Boom"}""") }
            val vm = loaded(this)

            assertEquals(TaxProfileLoad.Failed(FieldError.Text("Boom")), vm.uiState.value.load)

            serverProfile()
            vm.retry()
            runCurrent()

            assertEquals(TaxProfileLoad.Ready, vm.uiState.value.load)
            assertEquals("Ana Lee", vm.uiState.value.form.legalName)
            assertEquals(2, api.taxProfileCalls)
        }

    @Test
    fun aLoadNetworkFailureUsesTheNetworkLine() =
        runTest(main.testDispatcher) {
            api.taxProfileHandler = { throw IOException("offline") }
            val vm = loaded(this)

            assertEquals(
                TaxProfileLoad.Failed(FieldError.Res(R.string.login_error_network)),
                vm.uiState.value.load,
            )
        }

    @Test
    fun doubleTapSendsOneRequestAndTheFormLocksWhileSaving() =
        runTest(main.testDispatcher) {
            val gate = CompletableDeferred<SaveTaxProfileData>()
            api.saveTaxProfileHandler = { gate.await() }
            val vm = filledViewModel(this)

            vm.save()
            vm.save()
            runCurrent()

            assertEquals(1, api.saveTaxProfileCalls)
            assertTrue(vm.uiState.value.saving)

            vm.onFieldChange(TaxProfileField.LegalName, "Someone Else")
            assertEquals("Ana Lee", vm.uiState.value.form.legalName)

            gate.complete(SaveTaxProfileData(saved = true))
            runCurrent()

            assertEquals(1, api.saveTaxProfileCalls)
            assertFalse(vm.uiState.value.saving)
        }

    @Test
    fun dobPickerProducesIso() =
        runTest(main.testDispatcher) {
            val vm = filledViewModel(this)
            val start = LocalDate.of(1985, 12, 31).atStartOfDay(ZoneOffset.UTC)

            vm.onDobPicked(start.toInstant().toEpochMilli())

            assertEquals("1985-12-31", vm.uiState.value.form.dob)
        }

    @Test
    fun theCountryIsUppercasedAndLimitedToTwoLetters() =
        runTest(main.testDispatcher) {
            val vm = filledViewModel(this)

            vm.onFieldChange(TaxProfileField.Country, "usa")

            assertEquals("US", vm.uiState.value.form.country)
        }
}
