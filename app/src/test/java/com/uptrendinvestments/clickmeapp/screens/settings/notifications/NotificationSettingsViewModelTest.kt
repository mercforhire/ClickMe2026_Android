package com.uptrendinvestments.clickmeapp.screens.settings.notifications

import com.uptrendinvestments.clickmeapp.model.responses.NotificationPreferencesData
import com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.AutoSaveStatus
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
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
