@file:OptIn(ExperimentalCoroutinesApi::class)

package com.uptrendinvestments.clickmeapp.screens.signup.timezone

import androidx.lifecycle.SavedStateHandle
import com.uptrendinvestments.clickmeapp.components.buttons.SaveState
import com.uptrendinvestments.clickmeapp.constants.Timezones
import com.uptrendinvestments.clickmeapp.model.requests.SetupExpertProfileRequest
import com.uptrendinvestments.clickmeapp.model.responses.EmailStatusData
import com.uptrendinvestments.clickmeapp.model.responses.SetupExpertProfileData
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.SignupTestHarness
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
class SignupTimezoneViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    @get:Rule
    val tmp = TemporaryFolder()

    private lateinit var originalZone: TimeZone

    @Before
    fun setUp() {
        originalZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"))
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(originalZone)
    }

    private fun harness(scope: TestScope) =
        SignupTestHarness(scope, tmp, main.testDispatcher).also {
            it.api.setupProfileHandler = { SetupExpertProfileData() }
        }

    private fun viewModel(
        h: SignupTestHarness,
        handle: SavedStateHandle = SavedStateHandle(),
    ) = SignupTimezoneViewModel(h.repository, handle)

    @Test
    fun freshDraftPreselectsDeviceZoneWithTheTenBundledEntries() =
        runTest(main.testDispatcher) {
            val vm = viewModel(harness(this))
            runCurrent()

            assertEquals("America/New_York", vm.uiState.value.selectedId)
            assertEquals(Timezones.all, vm.uiState.value.entries)
            assertEquals("(GMT-05:00) Eastern Time", vm.uiState.value.selectedLabel)
        }

    @Test
    fun deviceZoneOutsideTheTenIsListedFirstAndSelected() =
        runTest(main.testDispatcher) {
            TimeZone.setDefault(TimeZone.getTimeZone("America/Toronto"))
            val vm = viewModel(harness(this))
            runCurrent()

            val entries = vm.uiState.value.entries
            assertEquals(11, entries.size)
            assertEquals("America/Toronto", entries.first().id)
            assertEquals("America/Toronto", vm.uiState.value.selectedId)
        }

    @Test
    fun hydratedServerZoneOutsideTheListIsShownFirstAndSelected() =
        runTest(main.testDispatcher) {
            val h = harness(this)
            h.api.expertProfileHandler = { h.fullProfile(timezone = "Asia/Tokyo") }
            h.api.emailStatusHandler = { EmailStatusData(false) }
            h.repository.hydrate()
            val vm = viewModel(h)
            runCurrent()

            assertEquals("Asia/Tokyo", vm.uiState.value.selectedId)
            assertEquals("Asia/Tokyo", vm.uiState.value.entries.first().id)
            assertEquals(Timezones.entryFor("Asia/Tokyo").label, vm.uiState.value.selectedLabel)
            assertEquals(11, vm.uiState.value.entries.size)
        }

    @Test
    fun selectSetsTheZoneAndClosesTheSheetAndAutoDetectReturnsToDevice() =
        runTest(main.testDispatcher) {
            val vm = viewModel(harness(this))
            runCurrent()

            vm.openSheet()
            assertTrue(vm.uiState.value.showSheet)
            vm.select("Europe/Paris")
            assertEquals("Europe/Paris", vm.uiState.value.selectedId)
            assertFalse(vm.uiState.value.showSheet)

            vm.autoDetect()
            assertEquals("America/New_York", vm.uiState.value.selectedId)
        }

    @Test
    fun saveSendsOnlyTheTimezoneAndHoldsSavedFor600ms() =
        runTest(main.testDispatcher) {
            val h = harness(this)
            val vm = viewModel(h)
            runCurrent()
            vm.select("Europe/Paris")

            vm.save()
            assertEquals(SaveState.Saving, vm.uiState.value.saveState)
            runCurrent()

            assertEquals(1, h.api.setupProfileRequests.size)
            assertEquals(
                SetupExpertProfileRequest(timezone = "Europe/Paris"),
                h.api.setupProfileRequests.single(),
            )
            assertEquals("Europe/Paris", h.repository.draft.value.timezone)
            assertEquals(SaveState.Saved, vm.uiState.value.saveState)
            assertFalse(vm.saved.value)

            advanceTimeBy(599)
            runCurrent()
            assertFalse(vm.saved.value)
            advanceTimeBy(2)
            runCurrent()
            assertTrue(vm.saved.value)

            vm.onSavedConsumed()
            assertFalse(vm.saved.value)
            assertEquals(SaveState.Idle, vm.uiState.value.saveState)
        }

    @Test
    fun a422OnTimezoneShowsInlineAndReturnsToIdle() =
        runTest(main.testDispatcher) {
            val h = harness(this)
            h.api.setupProfileHandler = {
                throw h.api.httpError(422, """{"errors":[{"field":"timezone","message":"Invalid timezone"}]}""")
            }
            val vm = viewModel(h)
            runCurrent()

            vm.save()
            runCurrent()

            assertEquals(FieldError.Text("Invalid timezone"), vm.uiState.value.timezoneError)
            assertNull(vm.uiState.value.dialogError)
            assertEquals(SaveState.Idle, vm.uiState.value.saveState)
            assertFalse(vm.saved.value)
        }

    @Test
    fun otherFailuresShowTheDialogAndAllowRetry() =
        runTest(main.testDispatcher) {
            val h = harness(this)
            h.api.setupProfileHandler = { throw IOException("offline") }
            val vm = viewModel(h)
            runCurrent()

            vm.save()
            runCurrent()

            assertTrue(vm.uiState.value.dialogError != null)
            assertNull(vm.uiState.value.timezoneError)
            assertEquals(SaveState.Idle, vm.uiState.value.saveState)

            vm.dismissDialog()
            assertNull(vm.uiState.value.dialogError)
        }

    @Test
    fun selectionIsRestoredFromSavedStateHandle() =
        runTest(main.testDispatcher) {
            val h = harness(this)
            val first = SavedStateHandle()
            val vm = viewModel(h, first)
            runCurrent()
            vm.select("Europe/London")

            val restored = viewModel(h, SavedStateHandle(mapOf("timezone_selected" to "Europe/London")))
            runCurrent()

            assertEquals("Europe/London", first.get<String>("timezone_selected"))
            assertEquals("Europe/London", restored.uiState.value.selectedId)
        }
}
