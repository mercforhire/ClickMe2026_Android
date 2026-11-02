package com.uptrendinvestments.clickmeapp.screens.settings.notifications

import com.uptrendinvestments.clickmeapp.model.responses.NotificationPreferenceRow
import com.uptrendinvestments.clickmeapp.model.responses.NotificationPreferencesData
import com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.AutoSaveStatus
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.allNotificationRows
import com.uptrendinvestments.clickmeapp.testing.notificationRow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationSettingsViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val api = FakeClickMeApi()

    private fun TestScope.viewModel() =
        NotificationSettingsViewModel(NotificationSettingsRepository(api), backgroundScope)

    private fun TestScope.loadedViewModel(): NotificationSettingsViewModel {
        api.notificationPreferencesHandler = { NotificationPreferencesData(emptyList()) }
        val vm = viewModel()
        runCurrent()
        return vm
    }

    @Test
    fun loadsThenSavesOneFlipAfter800ms() =
        runTest(main.testDispatcher) {
            api.notificationPreferencesHandler = { NotificationPreferencesData(emptyList()) }
            val vm = viewModel()
            assertEquals(NotificationLoad.Loading, vm.uiState.value.load)
            runCurrent()
            assertEquals(NotificationLoad.Loaded, vm.uiState.value.load)

            vm.onToggle("booking|requests", false)
            advanceTimeBy(NOTIFICATION_DEBOUNCE_MS - 1)
            runCurrent()
            assertEquals(0, api.updateNotificationPreferencesCalls)

            advanceTimeBy(1)
            runCurrent()
            assertEquals(1, api.updateNotificationPreferencesCalls)
            val rows = api.updateNotificationPreferencesRequests.single().preferences
            assertEquals(13, rows.size)
            for (row in rows) {
                val expected = !(row.category == "booking" && row.subCategory == "requests")
                assertEquals(expected, row.pushEnabled)
                assertEquals(expected, row.emailEnabled)
                assertEquals(expected, row.inAppEnabled)
            }
        }

    @Test
    fun loadingSendsNoPatch() =
        runTest(main.testDispatcher) {
            val vm = loadedViewModel()
            advanceTimeBy(NOTIFICATION_DEBOUNCE_MS * 5)
            runCurrent()
            assertEquals(0, api.updateNotificationPreferencesCalls)
            assertEquals(AutoSaveStatus.Idle, vm.uiState.value.status)
        }

    private fun TestScope.viewModelWith(vararg rows: NotificationPreferenceRow): NotificationSettingsViewModel {
        api.notificationPreferencesHandler = { NotificationPreferencesData(rows.toList()) }
        val vm = viewModel()
        runCurrent()
        return vm
    }

    private fun TestScope.settleDebounce() {
        advanceTimeBy(NOTIFICATION_DEBOUNCE_MS)
        runCurrent()
    }

    @Test
    fun aRowIsOnIfAnyChannelIsOn() =
        runTest(main.testDispatcher) {
            val vm =
                viewModelWith(
                    notificationRow("booking", "requests", push = false, email = true, inApp = false),
                    notificationRow("booking", "declines", push = false, email = false, inApp = false),
                )
            assertEquals(true, vm.uiState.value.values["booking|requests"])
            assertEquals(false, vm.uiState.value.values["booking|declines"])
        }

    @Test
    fun aMissingRowDefaultsToOn() =
        runTest(main.testDispatcher) {
            val empty = viewModelWith()
            assertEquals(13, empty.uiState.value.values.size)
            assertTrue(
                empty.uiState.value.values.values
                    .all { it },
            )

            val one = viewModelWith(notificationRow("session", "ended", push = false, email = false, inApp = false))
            assertEquals(false, one.uiState.value.values["session|ended"])
            assertEquals(
                12,
                one.uiState.value.values
                    .count { it.value },
            )
        }

    @Test
    fun rowsOutsideTheGridAreIgnored() =
        runTest(main.testDispatcher) {
            val vm =
                viewModelWith(
                    notificationRow("promotions", "offers", push = false, email = false, inApp = false),
                    notificationRow("payout", "paid", push = false, email = false, inApp = false),
                    notificationRow("account", "password_changed", push = false, email = false, inApp = false),
                    notificationRow("booking", "payment_actions", push = false, email = false, inApp = false),
                )
            assertEquals(defaultNotificationValues().keys, vm.uiState.value.values.keys)

            vm.onToggle("booking|requests", false)
            settleDebounce()
            val sent = api.updateNotificationPreferencesRequests.single().preferences
            assertEquals(13, sent.size)
            assertTrue(sent.none { it.category in setOf("promotions", "payout", "account") })
            assertTrue(sent.none { it.subCategory == "payment_actions" })
        }

    @Test
    fun keysAreSingular() =
        runTest(main.testDispatcher) {
            val vm = viewModelWith(notificationRow("bookings", "requests", push = false, email = false, inApp = false))
            assertEquals(true, vm.uiState.value.values["booking|requests"])
            assertEquals(13, vm.uiState.value.values.size)
        }

    @Test
    fun rapidFlipsCoalesceToOnePatch() =
        runTest(main.testDispatcher) {
            val vm = loadedViewModel()
            vm.onToggle("booking|requests", false)
            advanceTimeBy(200)
            vm.onToggle("booking|declines", false)
            advanceTimeBy(200)
            vm.onToggle("session|ended", false)
            advanceTimeBy(200)
            vm.onToggle("review|nudge", false)
            advanceTimeBy(NOTIFICATION_DEBOUNCE_MS - 1)
            runCurrent()
            assertEquals(0, api.updateNotificationPreferencesCalls)

            advanceTimeBy(1)
            runCurrent()
            assertEquals(1, api.updateNotificationPreferencesCalls)
            val off =
                api.updateNotificationPreferencesRequests
                    .single()
                    .preferences
                    .filter { !it.pushEnabled }
                    .map { "${it.category}|${it.subCategory}" }
            assertEquals(setOf("booking|requests", "booking|declines", "session|ended", "review|nudge"), off.toSet())
        }

    @Test
    fun patchCarriesOnlyTheFiveKeysForEveryRow() =
        runTest(main.testDispatcher) {
            val vm = viewModelWith(*allNotificationRows().toTypedArray())
            vm.onToggle("message|new_message", false)
            settleDebounce()
            val sent = api.updateNotificationPreferencesRequests.single().preferences
            assertEquals(13, sent.size)
            assertTrue(sent.all { it.pushEnabled == it.emailEnabled && it.emailEnabled == it.inAppEnabled })
            assertEquals(
                NOTIFICATION_SECTIONS.flatMap { it.rows }.map { it.key },
                sent.map { "${it.category}|${it.subCategory}" },
            )
            assertTrue(sent.none { it.category in setOf("payout", "account", "promotions") })
            assertTrue(sent.none { it.subCategory == "payment_actions" })
        }

    @Test
    fun aFlipBackToTheSavedValueSendsNothing() =
        runTest(main.testDispatcher) {
            val vm = loadedViewModel()
            vm.onToggle("booking|requests", false)
            advanceTimeBy(300)
            vm.onToggle("booking|requests", true)
            settleDebounce()
            advanceTimeBy(NOTIFICATION_DEBOUNCE_MS)
            runCurrent()
            assertEquals(0, api.updateNotificationPreferencesCalls)
            assertEquals(AutoSaveStatus.Idle, vm.uiState.value.status)
        }

    @Test
    fun gridIsThirteenRowsInUiSpecOrder() {
        assertEquals(listOf(7, 3, 1, 2), NOTIFICATION_SECTIONS.map { it.rows.size })
        assertEquals(
            listOf("booking", "session", "message", "review"),
            NOTIFICATION_SECTIONS.map { it.rows.first().category },
        )
        assertEquals(
            listOf("reminder", "no_show", "ended"),
            NOTIFICATION_SECTIONS[1].rows.map { it.subCategory },
        )
        assertTrue(
            NOTIFICATION_SECTIONS
                .flatMap { it.rows }
                .map { it.key }
                .distinct()
                .size == 13,
        )
    }
}
