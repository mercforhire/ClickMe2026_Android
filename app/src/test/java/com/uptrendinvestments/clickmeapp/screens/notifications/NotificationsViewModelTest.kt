package com.uptrendinvestments.clickmeapp.screens.notifications

import com.uptrendinvestments.clickmeapp.screens.push.PushLaunchRequest
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.MutableClock
import com.uptrendinvestments.clickmeapp.testing.notificationItem
import com.uptrendinvestments.clickmeapp.testing.notificationsPayload
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.util.TimeZone

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationsViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val api = FakeClickMeApi()
    private val launchRequest = PushLaunchRequest()

    // 2026-10-28 15:00 in America/Toronto (EDT, UTC-4).
    private val clock = MutableClock(Instant.parse("2026-10-28T19:00:00Z"))
    private var savedZone: TimeZone = TimeZone.getDefault()

    @Before
    fun pinZone() {
        savedZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("America/Toronto"))
    }

    @After
    fun restoreZone() {
        TimeZone.setDefault(savedZone)
    }

    private fun vm() = NotificationsViewModel(NotificationsRepository(api), launchRequest, clock)

    @Test
    fun firstLoadGroupsBySection() =
        runTest(main.testDispatcher) {
            api.notificationsHandler = { _, _ ->
                notificationsPayload(
                    notificationItem(id = "t1", createdAt = Instant.parse("2026-10-28T18:00:00Z")),
                    notificationItem(id = "t2", createdAt = Instant.parse("2026-10-28T13:00:00Z")),
                    notificationItem(id = "y1", createdAt = Instant.parse("2026-10-28T00:00:00Z")),
                    notificationItem(id = "e1", createdAt = Instant.parse("2026-10-23T16:00:00Z")),
                )
            }
            val vm = vm()
            runCurrent()
            val load = vm.uiState.value.load as NotificationsLoad.Loaded
            assertEquals(
                listOf(NotificationDay.Today, NotificationDay.Yesterday, NotificationDay.Earlier),
                load.sections.map { it.day },
            )
            assertEquals(listOf(2, 1, 1), load.sections.map { it.rows.size })
            assertEquals(listOf("t1", "t2"), load.sections[0].rows.map { it.id })
            assertEquals(listOf(1 to 50), api.notificationsQueries)
        }
}
