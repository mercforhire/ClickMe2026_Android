@file:OptIn(ExperimentalCoroutinesApi::class)

package com.uptrendinvestments.clickmeapp.screens.expertmode.availability

import androidx.lifecycle.SavedStateHandle
import com.uptrendinvestments.clickmeapp.components.buttons.SaveState
import com.uptrendinvestments.clickmeapp.managers.RestrictedAction
import com.uptrendinvestments.clickmeapp.model.requests.AvailabilityTimeRangeBody
import com.uptrendinvestments.clickmeapp.model.responses.SuccessMessageResponse
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.testing.FakeCallGuard
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.availabilityData
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
import java.time.DayOfWeek
import java.util.TimeZone

class AvailabilityViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val api = FakeClickMeApi()
    private val guard = FakeCallGuard()
    private val repository = AvailabilityRepository(api)
    private lateinit var originalZone: TimeZone

    @Before
    fun setUp() {
        originalZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"))
        api.updateMyAvailabilityHandler = { SuccessMessageResponse(status = "success") }
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(originalZone)
    }

    private fun viewModel(handle: SavedStateHandle = SavedStateHandle()) =
        AvailabilityViewModel(handle, repository, guard)

    private fun AvailabilityViewModel.day(day: DayOfWeek): DaySchedule = uiState.value.days.first { it.day == day }

    private fun firstEntryLabel(vm: AvailabilityViewModel): String {
        val entries = vm.uiState.value.timezoneEntries
        return entries[0].label
    }

    private fun serverNewYork() {
        api.myAvailabilityHandler = {
            availabilityData(
                timezoneId = "America/New_York",
                schedule = mapOf(DayOfWeek.MONDAY to listOf("09:00" to "17:00")),
            )
        }
    }

    private fun serverUtc(id: String = "UTC") {
        api.myAvailabilityHandler = { availabilityData(timezoneId = id, displayName = "(GMT+00:00) UTC") }
    }

    private fun loaded(
        scope: TestScope,
        handle: SavedStateHandle = SavedStateHandle(),
    ): AvailabilityViewModel {
        val vm = viewModel(handle)
        scope.runCurrent()
        return vm
    }

    // ---- Task 1: load, edits, timezone, restore ----

    @Test
    fun showsTheDefaultScheduleBeforeTheLoadCompletes() =
        runTest(main.testDispatcher) {
            serverNewYork()
            val vm = viewModel()

            assertEquals(AvailabilityLoad.Loading, vm.uiState.value.load)
            assertEquals(AvailabilityScheduleLogic.defaultSchedule(), vm.uiState.value.days)
        }

    @Test
    fun hydratesTheScheduleAndTimezoneFromTheServer() =
        runTest(main.testDispatcher) {
            serverNewYork()
            val vm = loaded(this)

            val state = vm.uiState.value
            assertEquals(AvailabilityLoad.Loaded, state.load)
            val monday = state.days.first { it.day == DayOfWeek.MONDAY }
            assertTrue(monday.enabled)
            assertEquals(listOf(TimeSlot(540, 1020)), monday.slots)
            assertTrue(state.days.filter { it.day != DayOfWeek.MONDAY }.none { it.enabled })
            assertEquals("America/New_York", state.timezoneId)
            assertEquals("(GMT-05:00) Eastern Time", state.timezoneLabel)
        }

    @Test
    fun anUnbundledServerZoneUsesTheServerDisplayName() =
        runTest(main.testDispatcher) {
            api.myAvailabilityHandler = {
                availabilityData(timezoneId = "America/Toronto", displayName = "(GMT-05:00) America/Toronto")
            }
            val vm = loaded(this)

            assertEquals("(GMT-05:00) America/Toronto", vm.uiState.value.timezoneLabel)
            assertEquals("(GMT-05:00) America/Toronto", firstEntryLabel(vm))
        }

    @Test
    fun loadFailureShowsTheServerMessageAndRetryRecovers() =
        runTest(main.testDispatcher) {
            api.myAvailabilityHandler = { throw api.httpError(500, """{"message":"Server down"}""") }
            val vm = loaded(this)

            assertEquals(AvailabilityLoad.Failed(FieldError.Text("Server down")), vm.uiState.value.load)

            serverNewYork()
            vm.retry()
            assertEquals(AvailabilityLoad.Loading, vm.uiState.value.load)
            runCurrent()
            assertEquals(AvailabilityLoad.Loaded, vm.uiState.value.load)
        }

    @Test
    fun refreshOverwritesLocalEditsWithTheServerCopy() =
        runTest(main.testDispatcher) {
            serverNewYork()
            val vm = loaded(this)
            vm.setDayEnabled(DayOfWeek.TUESDAY, true)

            vm.refresh()
            assertTrue(vm.uiState.value.isRefreshing)
            runCurrent()

            assertFalse(vm.uiState.value.isRefreshing)
            assertFalse(vm.day(DayOfWeek.TUESDAY).enabled)
        }

    @Test
    fun refreshFailureKeepsEditsAndFlagsTheFailureOnce() =
        runTest(main.testDispatcher) {
            serverNewYork()
            val vm = loaded(this)
            vm.setDayEnabled(DayOfWeek.TUESDAY, true)
            api.myAvailabilityHandler = { throw api.httpError(500, """{"message":"down"}""") }

            vm.refresh()
            runCurrent()

            assertFalse(vm.uiState.value.isRefreshing)
            assertTrue(vm.uiState.value.refreshFailed)
            assertTrue(vm.day(DayOfWeek.TUESDAY).enabled)
            vm.onRefreshFailedShown()
            assertFalse(vm.uiState.value.refreshFailed)
        }

    @Test
    fun editsUpdateLocalStateAndOverlapsBlockSave() =
        runTest(main.testDispatcher) {
            serverNewYork()
            val vm = loaded(this)

            vm.setDayEnabled(DayOfWeek.TUESDAY, true)
            assertEquals(
                listOf(TimeSlot(540, 1020)),
                vm.uiState.value.days
                    .first { it.day == DayOfWeek.TUESDAY }
                    .slots,
            )
            assertTrue(vm.uiState.value.canSave)

            vm.addHours(DayOfWeek.MONDAY)
            val monday = vm.day(DayOfWeek.MONDAY)
            assertEquals(TimeSlot(1050, 1110), monday.slots[1])

            vm.updateSlot(DayOfWeek.MONDAY, 1, TimeSlot(600, 700))
            assertEquals(listOf(DayOfWeek.MONDAY), vm.uiState.value.overlappingDays)
            assertFalse(vm.uiState.value.canSave)

            vm.removeSlot(DayOfWeek.MONDAY, 1)
            assertEquals(emptyList<DayOfWeek>(), vm.uiState.value.overlappingDays)
            assertTrue(vm.uiState.value.canSave)
        }

    @Test
    fun timezoneChangesStayLocalUntilSave() =
        runTest(main.testDispatcher) {
            serverNewYork()
            TimeZone.setDefault(TimeZone.getTimeZone("America/Chicago"))
            val vm = loaded(this)

            vm.selectTimezone("Europe/Paris")
            assertEquals("Europe/Paris", vm.uiState.value.timezoneId)
            vm.autoDetectTimezone()
            assertEquals("America/Chicago", vm.uiState.value.timezoneId)
            runCurrent()

            assertEquals(0, api.updateMyAvailabilityCalls)
        }

    @Test
    fun unsavedEditsSurviveProcessDeathAndRefreshOverwritesThem() =
        runTest(main.testDispatcher) {
            serverNewYork()
            val handle = SavedStateHandle()
            val first = loaded(this, handle)
            first.setDayEnabled(DayOfWeek.TUESDAY, true)
            first.selectTimezone("Europe/Paris")

            val second = viewModel(handle)
            assertEquals("Europe/Paris", second.uiState.value.timezoneId)
            runCurrent()

            val state = second.uiState.value
            assertEquals(AvailabilityLoad.Loaded, state.load)
            assertEquals("Europe/Paris", state.timezoneId)
            assertTrue(state.days.first { it.day == DayOfWeek.TUESDAY }.enabled)
            assertEquals("America/New_York", state.serverTimezoneId)

            second.refresh()
            runCurrent()
            assertEquals("America/New_York", second.uiState.value.timezoneId)
            assertFalse(second.day(DayOfWeek.TUESDAY).enabled)
        }

    // ---- Task 2: guarded Save and the silent UTC fill ----

    @Test
    fun saveDuringACallSendsNothingAndStaysIdle() =
        runTest(main.testDispatcher) {
            serverNewYork()
            val vm = loaded(this)
            guard.blocked = true

            vm.save()
            runCurrent()

            assertEquals(listOf(RestrictedAction.EditAvailability), guard.attempts)
            assertEquals(0, api.updateMyAvailabilityCalls)
            assertEquals(SaveState.Idle, vm.uiState.value.saveState)
        }

    @Test
    fun saveIsIgnoredBeforeLoadWithOverlapsAndWhileSaving() =
        runTest(main.testDispatcher) {
            serverNewYork()
            val vm = viewModel()
            vm.save()
            assertTrue(guard.attempts.isEmpty())
            runCurrent()

            vm.addHours(DayOfWeek.MONDAY)
            vm.updateSlot(DayOfWeek.MONDAY, 1, TimeSlot(600, 700))
            vm.save()
            runCurrent()
            assertEquals(0, api.updateMyAvailabilityCalls)

            vm.removeSlot(DayOfWeek.MONDAY, 1)
            val before = guard.attempts.size
            vm.save()
            vm.save()
            runCurrent()
            assertEquals(1, guard.attempts.size - before)
            assertEquals(1, api.updateMyAvailabilityCalls)
        }

    @Test
    fun saveSendsAllSevenDaysSortedAndShowsSavedForTwoSeconds() =
        runTest(main.testDispatcher) {
            api.myAvailabilityHandler = {
                availabilityData(
                    timezoneId = "America/New_York",
                    schedule = mapOf(DayOfWeek.MONDAY to listOf("13:00" to "14:00", "09:00" to "10:00")),
                )
            }
            val handle = SavedStateHandle()
            val vm = loaded(this, handle)
            vm.setDayEnabled(DayOfWeek.TUESDAY, true)
            vm.updateSlot(DayOfWeek.TUESDAY, 0, TimeSlot(1320, 1440))
            assertTrue(handle.contains("availability_schedule"))

            vm.save()
            assertEquals(SaveState.Saving, vm.uiState.value.saveState)
            runCurrent()

            val body = api.updateMyAvailabilityRequests.single()
            assertEquals("America/New_York", body.timezoneId)
            assertEquals(
                listOf(AvailabilityTimeRangeBody("09:00", "10:00"), AvailabilityTimeRangeBody("13:00", "14:00")),
                body.schedule.monday,
            )
            assertEquals(listOf(AvailabilityTimeRangeBody("22:00", "24:00")), body.schedule.tuesday)
            assertTrue(body.schedule.wednesday.isEmpty())
            assertTrue(body.schedule.sunday.isEmpty())
            assertEquals(SaveState.Saved, vm.uiState.value.saveState)
            assertFalse(handle.contains("availability_schedule"))

            advanceTimeBy(1999)
            runCurrent()
            assertEquals(SaveState.Saved, vm.uiState.value.saveState)
            advanceTimeBy(1)
            runCurrent()
            assertEquals(SaveState.Idle, vm.uiState.value.saveState)
        }

    @Test
    fun saveFailureShowsTheFieldMessageAndDismissClearsIt() =
        runTest(main.testDispatcher) {
            serverNewYork()
            val vm = loaded(this)
            api.updateMyAvailabilityHandler = {
                throw api.httpError(
                    422,
                    """{"status":"error","message":"Validation failed","errors":""" +
                        """[{"field":"schedule.monday","message":"end_time must align to a 15-minute boundary"}]}""",
                )
            }

            vm.save()
            runCurrent()

            assertEquals(SaveState.Idle, vm.uiState.value.saveState)
            assertEquals(
                FieldError.Text("end_time must align to a 15-minute boundary"),
                vm.uiState.value.dialog,
            )
            vm.dismissDialog()
            assertNull(vm.uiState.value.dialog)
        }

    @Test
    fun aUtcServerZoneIsReplacedByTheDeviceZoneWithOneSilentPut() =
        runTest(main.testDispatcher) {
            TimeZone.setDefault(TimeZone.getTimeZone("America/Toronto"))
            serverUtc()
            val vm = loaded(this)

            assertEquals("America/Toronto", vm.uiState.value.timezoneId)
            val body = api.updateMyAvailabilityRequests.single()
            assertEquals("America/Toronto", body.timezoneId)
            assertEquals(7, vm.uiState.value.days.size)
            assertEquals(SaveState.Idle, vm.uiState.value.saveState)
            assertTrue(guard.attempts.isEmpty())
            assertNull(vm.uiState.value.dialog)
        }

    @Test
    fun etcUtcInAnyCaseIsFilledToo() =
        runTest(main.testDispatcher) {
            TimeZone.setDefault(TimeZone.getTimeZone("America/Toronto"))
            serverUtc("etc/utc")
            val vm = loaded(this)

            assertEquals("America/Toronto", vm.uiState.value.timezoneId)
            assertEquals(1, api.updateMyAvailabilityCalls)
        }

    @Test
    fun noFillWhenTheDeviceIsOnUtcOrTheServerZoneIsReal() =
        runTest(main.testDispatcher) {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
            serverUtc()
            loaded(this)
            assertEquals(0, api.updateMyAvailabilityCalls)

            TimeZone.setDefault(TimeZone.getTimeZone("America/Toronto"))
            api.myAvailabilityHandler = { availabilityData(timezoneId = "Europe/Paris") }
            loaded(this)
            assertEquals(0, api.updateMyAvailabilityCalls)
        }

    @Test
    fun duringACallTheZoneIsFilledButNothingIsSent() =
        runTest(main.testDispatcher) {
            TimeZone.setDefault(TimeZone.getTimeZone("America/Toronto"))
            serverUtc()
            guard.isCallActive.value = true
            val vm = loaded(this)

            assertEquals("America/Toronto", vm.uiState.value.timezoneId)
            assertEquals(0, api.updateMyAvailabilityCalls)
            assertTrue(guard.attempts.isEmpty())
        }

    @Test
    fun hydratedOverlapsSuppressTheSilentPut() =
        runTest(main.testDispatcher) {
            TimeZone.setDefault(TimeZone.getTimeZone("America/Toronto"))
            api.myAvailabilityHandler = {
                availabilityData(
                    timezoneId = "UTC",
                    schedule = mapOf(DayOfWeek.MONDAY to listOf("09:00" to "12:00", "10:00" to "11:00")),
                )
            }
            val vm = loaded(this)

            assertEquals("America/Toronto", vm.uiState.value.timezoneId)
            assertEquals(0, api.updateMyAvailabilityCalls)
        }

    @Test
    fun aSilentPutIsSkippedWhenHydrateDroppedAServerSlot() =
        runTest(main.testDispatcher) {
            TimeZone.setDefault(TimeZone.getTimeZone("America/Toronto"))
            api.myAvailabilityHandler = {
                availabilityData(
                    timezoneId = "UTC",
                    schedule = mapOf(DayOfWeek.MONDAY to listOf("09:00" to "17:00", "12:00" to "11:00")),
                )
            }
            val vm = loaded(this)

            assertEquals("America/Toronto", vm.uiState.value.timezoneId)
            assertEquals(0, api.updateMyAvailabilityCalls)
            assertEquals(listOf(TimeSlot(540, 1020)), vm.day(DayOfWeek.MONDAY).slots)
            assertEquals(SaveState.Idle, vm.uiState.value.saveState)
            assertTrue(guard.attempts.isEmpty())
            assertNull(vm.uiState.value.dialog)
        }

    @Test
    fun aFailedSilentPutIsQuietAndNeverRepeatedInTheSameSession() =
        runTest(main.testDispatcher) {
            TimeZone.setDefault(TimeZone.getTimeZone("America/Toronto"))
            serverUtc()
            api.updateMyAvailabilityHandler = { throw api.httpError(500, """{"message":"down"}""") }
            val vm = loaded(this)

            assertEquals("America/Toronto", vm.uiState.value.timezoneId)
            assertNull(vm.uiState.value.dialog)
            assertEquals(SaveState.Idle, vm.uiState.value.saveState)
            assertEquals(1, api.updateMyAvailabilityCalls)

            vm.refresh()
            runCurrent()
            assertEquals(1, api.updateMyAvailabilityCalls)
        }

    @Test
    fun aRestoredViewModelNeverAutoFills() =
        runTest(main.testDispatcher) {
            TimeZone.setDefault(TimeZone.getTimeZone("America/Toronto"))
            serverUtc()
            val handle =
                SavedStateHandle(
                    mapOf(
                        "availability_schedule" to
                            AvailabilityScheduleLogic.encode(AvailabilityScheduleLogic.defaultSchedule(), "UTC"),
                    ),
                )
            val vm = loaded(this, handle)

            assertEquals("UTC", vm.uiState.value.timezoneId)
            assertEquals(0, api.updateMyAvailabilityCalls)
        }
}
