package com.uptrendinvestments.clickmeapp.managers

import com.uptrendinvestments.clickmeapp.testing.testPreferencesDataStore
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.util.UUID

class PushPreferencesTest {
    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun deviceIdIsStableAcrossInstances() =
        runTest {
            val store = testPreferencesDataStore(backgroundScope, tmp, "push.preferences_pb")
            val first = PushPreferences(store).deviceId()

            assertEquals(first, PushPreferences(store).deviceId())
            assertEquals(first, PushPreferences(store).deviceId())
            // A valid random UUID.
            assertEquals(first, UUID.fromString(first).toString())
        }

    @Test
    fun differentFilesGetDifferentIds() =
        runTest {
            val a = PushPreferences(testPreferencesDataStore(backgroundScope, tmp, "a.preferences_pb")).deviceId()
            val b = PushPreferences(testPreferencesDataStore(backgroundScope, tmp, "b.preferences_pb")).deviceId()

            assertNotEquals(a, b)
        }

    @Test
    fun sessionStoreClearKeepsDeviceId() =
        runTest {
            val push = PushPreferences(testPreferencesDataStore(backgroundScope, tmp, "push.preferences_pb"))
            val session = SessionStore(testPreferencesDataStore(backgroundScope, tmp, "session.preferences_pb"))
            val before = push.deviceId()

            session.clear()

            assertEquals(before, push.deviceId())
        }
}
