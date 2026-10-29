@file:OptIn(ExperimentalCoroutinesApi::class)

package com.uptrendinvestments.clickmeapp.screens.expertmode.taxprofile

import com.uptrendinvestments.clickmeapp.components.buttons.SaveState
import com.uptrendinvestments.clickmeapp.model.requests.SaveTaxProfileRequest
import com.uptrendinvestments.clickmeapp.model.requests.TaxAddressBody
import com.uptrendinvestments.clickmeapp.model.responses.SaveTaxProfileData
import com.uptrendinvestments.clickmeapp.model.responses.TaxProfileAddress
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.taxProfile
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
}
