@file:OptIn(ExperimentalCoroutinesApi::class)

package com.uptrendinvestments.clickmeapp.realtime

import com.uptrendinvestments.clickmeapp.managers.PushTokenRegistrar
import com.uptrendinvestments.clickmeapp.managers.SessionEvent
import com.uptrendinvestments.clickmeapp.managers.SessionEvents
import com.uptrendinvestments.clickmeapp.managers.SessionStore
import com.uptrendinvestments.clickmeapp.managers.TokenPair
import com.uptrendinvestments.clickmeapp.managers.UserManager
import com.uptrendinvestments.clickmeapp.model.responses.AuthUser
import com.uptrendinvestments.clickmeapp.model.responses.LoginData
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.FakeProcessForeground
import com.uptrendinvestments.clickmeapp.testing.FakeRealtimeConnection
import com.uptrendinvestments.clickmeapp.testing.FakeSecureTokenStore
import com.uptrendinvestments.clickmeapp.testing.testPreferencesDataStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class RealtimeConnectionManagerTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private fun loginData(token: String = "access") =
        LoginData(
            token = token,
            user = AuthUser(id = "u1", firstName = "Ada", lastName = "Lovelace", roles = listOf("client")),
            refreshToken = "refresh",
            expiresAt = 1_900_000_000L,
            expiresIn = 3600L,
        )

    private inner class Harness(
        val scope: TestScope,
        foregroundInitially: Boolean = true,
    ) {
        private val dispatcher = StandardTestDispatcher(scope.testScheduler)
        val api = FakeClickMeApi()
        val tokens = FakeSecureTokenStore(null)
        val events = SessionEvents()
        val connection = FakeRealtimeConnection()
        val foreground = FakeProcessForeground(foregroundInitially)
        val userManager =
            UserManager(
                api,
                tokens,
                SessionStore(testPreferencesDataStore(scope.backgroundScope, tmp)),
                events,
                PushTokenRegistrar(),
                scope.backgroundScope,
                dispatcher,
            )
        val manager =
            RealtimeConnectionManager(
                connection,
                userManager,
                tokens,
                events,
                foreground,
                RealtimeConfig("http://localhost"),
                scope.backgroundScope,
                dispatcher,
            )

        init {
            api.loginHandler = { loginData() }
            api.userProfileHandler = { throw java.io.IOException("not needed") }
        }

        /** Starts the manager and logs in with token "access" (stubbed login). */
        suspend fun startAndLogin() {
            manager.start()
            scope.runCurrent()
            userManager.login("a@b.co", "pw")
            scope.runCurrent()
        }
    }

    @Test
    fun notLoggedInMakesNoConnectCall() =
        runTest {
            val h = Harness(this)

            h.manager.start()
            runCurrent()

            assertTrue(h.connection.connectCalls.isEmpty())
        }

    @Test
    fun loginInForegroundConnectsOnceWithStoredToken() =
        runTest {
            val h = Harness(this)

            h.startAndLogin()

            assertEquals(listOf("access"), h.connection.connectCalls)
        }

    @Test
    fun loginInBackgroundWaitsForForeground() =
        runTest {
            val h = Harness(this, foregroundInitially = false)

            h.startAndLogin()
            assertTrue(h.connection.connectCalls.isEmpty())

            h.foreground.isForeground.value = true
            runCurrent()
            assertEquals(listOf("access"), h.connection.connectCalls)
        }

    @Test
    fun goingToBackgroundDisconnects() =
        runTest {
            val h = Harness(this)
            h.startAndLogin()
            val before = h.connection.disconnectCalls

            h.foreground.isForeground.value = false
            runCurrent()

            assertEquals(before + 1, h.connection.disconnectCalls)
        }

    @Test
    fun logoutDisconnects() =
        runTest {
            val h = Harness(this)
            h.startAndLogin()
            val before = h.connection.disconnectCalls

            h.userManager.logout()
            runCurrent()

            assertEquals(before + 1, h.connection.disconnectCalls)
            assertFalse(h.userManager.isLoggedIn.value)
        }

    @Test
    fun sessionRejectedDisconnects() =
        runTest {
            val h = Harness(this)
            h.startAndLogin()
            val before = h.connection.disconnectCalls

            h.events.emit(SessionEvent.Rejected)
            runCurrent()

            assertEquals(before + 1, h.connection.disconnectCalls)
        }

    @Test
    fun sessionSuspendedDisconnects() =
        runTest {
            val h = Harness(this)
            h.startAndLogin()
            val before = h.connection.disconnectCalls

            h.events.emit(SessionEvent.Suspended)
            runCurrent()

            assertEquals(before + 1, h.connection.disconnectCalls)
            assertTrue(h.userManager.wasSuspended.value)
        }

    @Test
    fun refreshedReconnectsWithRotatedToken() =
        runTest {
            val h = Harness(this)
            h.startAndLogin()

            h.tokens.save(TokenPair("access-2", "refresh-2", 1_900_000_100L))
            h.events.emit(SessionEvent.Refreshed)
            runCurrent()

            assertEquals(listOf("access", "access-2"), h.connection.connectCalls)
        }

    @Test
    fun refreshedInBackgroundDoesNotConnect() =
        runTest {
            val h = Harness(this)
            h.startAndLogin()
            h.foreground.isForeground.value = false
            runCurrent()

            h.tokens.save(TokenPair("access-2", "refresh-2", 1_900_000_100L))
            h.events.emit(SessionEvent.Refreshed)
            runCurrent()

            assertEquals(listOf("access"), h.connection.connectCalls)
        }

    @Test
    fun missingStoredTokenDisconnectsInsteadOfConnecting() =
        runTest {
            val h = Harness(this, foregroundInitially = false)
            h.startAndLogin()
            h.tokens.clear()
            val before = h.connection.disconnectCalls

            h.foreground.isForeground.value = true
            runCurrent()

            assertTrue(h.connection.connectCalls.isEmpty())
            assertEquals(before + 1, h.connection.disconnectCalls)
        }

    @Test
    fun startTwiceStillConnectsOnce() =
        runTest {
            val h = Harness(this)
            h.manager.start()
            h.manager.start()
            runCurrent()
            h.userManager.login("a@b.co", "pw")
            runCurrent()

            assertEquals(listOf("access"), h.connection.connectCalls)
        }
}
