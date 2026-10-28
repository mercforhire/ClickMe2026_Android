package com.uptrendinvestments.clickmeapp.screens.notifications

import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.notificationItem
import com.uptrendinvestments.clickmeapp.testing.notificationsPayload
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationsRepositoryTest {
    private val api = FakeClickMeApi()
    private val repository = NotificationsRepository(api)

    @Test
    fun recentUsesPageOneLimitFifty() =
        runTest {
            api.notificationsHandler = { _, _ -> notificationsPayload(notificationItem()) }

            val rows = repository.recent()

            assertEquals(listOf(1 to 50), api.notificationsQueries)
            assertEquals("n1", rows.single().id)
        }
}
