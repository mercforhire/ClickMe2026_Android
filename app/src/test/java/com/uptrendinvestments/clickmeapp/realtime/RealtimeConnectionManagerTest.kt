@file:OptIn(ExperimentalCoroutinesApi::class)

package com.uptrendinvestments.clickmeapp.realtime

import com.uptrendinvestments.clickmeapp.managers.PushTokenRegistrar
import com.uptrendinvestments.clickmeapp.managers.SecureTokenStore
import com.uptrendinvestments.clickmeapp.managers.SessionEvent
import com.uptrendinvestments.clickmeapp.managers.SessionEvents
import com.uptrendinvestments.clickmeapp.managers.SessionStore
import com.uptrendinvestments.clickmeapp.managers.TokenPair
import com.uptrendinvestments.clickmeapp.managers.UserManager
import com.uptrendinvestments.clickmeapp.model.enums.ErrorCode
import com.uptrendinvestments.clickmeapp.model.responses.AuthUser
import com.uptrendinvestments.clickmeapp.model.responses.LoginData
import com.uptrendinvestments.clickmeapp.model.responses.MeData
import com.uptrendinvestments.clickmeapp.testing.FakeCallGuard
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
import java.io.IOException

/**
 * Lets a test run code inside the manager's token read, i.e. while the read has freed the serial view.
 * The hook fires once and only for reads made by the manager (UserManager uses the delegate directly).
 */
private class HookedTokenStore(
    private val delegate: FakeSecureTokenStore,
) : SecureTokenStore by delegate {
    @Volatile
    var onNextAccessRead: (() -> Unit)? = null

    override val accessToken: String?
        get() {
            onNextAccessRead?.let {
                onNextAccessRead = null
                it()
            }
            return delegate.accessToken
        }
}

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
        val managerTokens = HookedTokenStore(tokens)
        val events = SessionEvents()
        val connection = FakeRealtimeConnection()
        val foreground = FakeProcessForeground(foregroundInitially)
        val callGuard = FakeCallGuard()
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
                managerTokens,
                events,
                foreground,
                callGuard,
                RealtimeConfig("http://localhost"),
                scope.backgroundScope,
                dispatcher,
            )

        init {
            api.loginHandler = { loginData() }
            api.userProfileHandler = { throw IOException("not needed") }
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
    fun activeCallInBackgroundConnectsWithStoredToken() =
        runTest {
            val h = Harness(this, foregroundInitially = false)
            h.callGuard.isCallActive.value = true

            h.startAndLogin()

            assertEquals(listOf("access"), h.connection.connectCalls)
        }

    @Test
    fun callEndingInBackgroundDisconnects() =
        runTest {
            val h = Harness(this, foregroundInitially = false)
            h.callGuard.isCallActive.value = true
            h.startAndLogin()
            val before = h.connection.disconnectCalls

            h.callGuard.isCallActive.value = false
            runCurrent()

            assertEquals(before + 1, h.connection.disconnectCalls)
        }

    @Test
    fun callEndingInForegroundKeepsTheSocket() =
        runTest {
            val h = Harness(this)
            h.callGuard.isCallActive.value = true
            h.startAndLogin()
            val before = h.connection.disconnectCalls

            h.callGuard.isCallActive.value = false
            runCurrent()

            assertEquals(before, h.connection.disconnectCalls)
            assertEquals(listOf("access"), h.connection.connectCalls)
        }

    @Test
    fun loggedOutWithCallFlagDoesNotConnect() =
        runTest {
            val h = Harness(this, foregroundInitially = false)
            h.callGuard.isCallActive.value = true

            h.manager.start()
            runCurrent()

            assertTrue(h.connection.connectCalls.isEmpty())
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
    fun refreshedThatGoesToBackgroundDuringTokenReadDoesNotConnect() =
        runTest {
            val h = Harness(this)
            h.startAndLogin()
            h.tokens.save(TokenPair("access-2", "refresh-2", 1_900_000_100L))
            h.managerTokens.onNextAccessRead = { h.foreground.isForeground.value = false }

            h.events.emit(SessionEvent.Refreshed)
            runCurrent()

            assertEquals(listOf("access"), h.connection.connectCalls)
            assertEquals(ConnectionState.Disconnected, h.connection.state.value)
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

    // ---- Task 2: handshake rejection policy (D-15) ----

    private val me = MeData(id = "u1", email = "a@b.co", roles = listOf("client"))

    private fun failure(
        reason: HandshakeFailureReason,
        token: String = "access",
    ) = HandshakeFailure(reason, token)

    private suspend fun TestScope.advanceBy(ms: Long) {
        advanceTimeBy(ms)
        runCurrent()
    }

    @Test
    fun missingTokenDisconnectsWithoutProbeOrRetry() =
        runTest {
            val h = Harness(this)
            h.startAndLogin()
            val before = h.connection.disconnectCalls

            h.connection.emitFailure(failure(HandshakeFailureReason.MissingToken))
            runCurrent()
            advanceBy(60_000)

            assertEquals(before + 1, h.connection.disconnectCalls)
            assertEquals(0, h.api.meCalls)
            assertEquals(listOf("access"), h.connection.connectCalls)
        }

    @Test
    fun invalidTokenProbesOnceThenRetriesAfterTwoSeconds() =
        runTest {
            val h = Harness(this)
            h.api.meHandler = { me }
            h.startAndLogin()

            h.connection.emitFailure(failure(HandshakeFailureReason.InvalidToken))
            runCurrent()
            assertEquals(1, h.api.meCalls)
            advanceBy(1_900)
            assertEquals(listOf("access"), h.connection.connectCalls)
            advanceBy(200)
            assertEquals(listOf("access", "access"), h.connection.connectCalls)
        }

    @Test
    fun repeatedRejectionOfSameTokenNeverProbesAgainAndBacksOffToCap() =
        runTest {
            val h = Harness(this)
            h.api.meHandler = { me }
            h.startAndLogin()
            h.connection.emitFailure(failure(HandshakeFailureReason.InvalidToken))
            runCurrent()
            advanceBy(2_001)
            var connects = h.connection.connectCalls.size

            for (delayMs in listOf(4_000L, 8_000L, 16_000L, 30_000L, 30_000L)) {
                h.connection.emitFailure(failure(HandshakeFailureReason.InvalidToken))
                runCurrent()
                advanceBy(delayMs - 1)
                assertEquals("no connect before $delayMs", connects, h.connection.connectCalls.size)
                advanceBy(2)
                connects++
                assertEquals("connect at $delayMs", connects, h.connection.connectCalls.size)
            }
            assertEquals(1, h.api.meCalls)
        }

    @Test
    fun invalidTokenAlreadyRotatedReconnectsImmediatelyWithoutProbe() =
        runTest {
            val h = Harness(this)
            h.startAndLogin()
            h.tokens.save(TokenPair("access-2", "refresh-2", 1_900_000_100L))

            h.connection.emitFailure(failure(HandshakeFailureReason.InvalidToken, token = "old"))
            runCurrent()

            assertEquals(listOf("access", "access-2"), h.connection.connectCalls)
            assertEquals(0, h.api.meCalls)
        }

    @Test
    fun probeThatRefreshesReconnectsWithNewTokenOnly() =
        runTest {
            val h = Harness(this)
            h.api.meHandler = {
                h.tokens.save(TokenPair("access-2", "refresh-2", 1_900_000_100L))
                h.events.emit(SessionEvent.Refreshed)
                me
            }
            h.startAndLogin()

            h.connection.emitFailure(failure(HandshakeFailureReason.InvalidToken))
            runCurrent()
            advanceBy(30_000)

            assertEquals(1, h.api.meCalls)
            assertTrue(
                h.connection.connectCalls
                    .drop(1)
                    .all { it == "access-2" },
            )
            assertEquals("access-2", h.connection.connectCalls.last())
        }

    @Test
    fun probeWithRejectedRefreshLogsOutAndDisconnects() =
        runTest {
            val h = Harness(this)
            h.api.meHandler = {
                h.tokens.clear()
                h.events.emit(SessionEvent.Rejected)
                throw h.api.httpError(401, """{"code":"TOKEN_EXPIRED"}""")
            }
            h.startAndLogin()
            val before = h.connection.disconnectCalls

            h.connection.emitFailure(failure(HandshakeFailureReason.InvalidToken))
            runCurrent()
            advanceBy(60_000)

            assertFalse(h.userManager.isLoggedIn.value)
            assertTrue(h.connection.disconnectCalls > before)
            assertEquals(listOf("access"), h.connection.connectCalls)
        }

    @Test
    fun accountSuspendedProbeTripsSuspensionAndDisconnects() =
        runTest {
            val h = Harness(this)
            h.api.meHandler = {
                h.tokens.clear()
                h.events.emit(SessionEvent.Suspended)
                throw h.api.httpError(403, """{"code":"${ErrorCode.AccountSuspended.wire}"}""")
            }
            h.startAndLogin()
            val before = h.connection.disconnectCalls

            h.connection.emitFailure(failure(HandshakeFailureReason.AccountSuspended))
            runCurrent()
            advanceBy(60_000)

            assertEquals(1, h.api.meCalls)
            assertTrue(h.userManager.wasSuspended.value)
            assertFalse(h.userManager.isLoggedIn.value)
            assertTrue(h.connection.disconnectCalls > before)
            assertEquals(listOf("access"), h.connection.connectCalls)
        }

    @Test
    fun softProbeFailureRetriesWithSameToken() =
        runTest {
            val h = Harness(this)
            h.api.meHandler = { throw IOException("offline") }
            h.startAndLogin()

            h.connection.emitFailure(failure(HandshakeFailureReason.InvalidToken))
            runCurrent()
            assertEquals(1, h.api.meCalls)
            assertEquals(listOf("access"), h.connection.connectCalls)
            advanceBy(2_001)

            assertEquals(listOf("access", "access"), h.connection.connectCalls)
        }

    @Test
    fun serverDisconnectedRetriesWithoutProbe() =
        runTest {
            val h = Harness(this)
            h.startAndLogin()

            h.connection.emitFailure(failure(HandshakeFailureReason.ServerDisconnected))
            runCurrent()
            advanceBy(1_900)
            assertEquals(1, h.connection.connectCalls.size)
            advanceBy(200)

            assertEquals(2, h.connection.connectCalls.size)
            assertEquals(0, h.api.meCalls)
        }

    @Test
    fun otherFailureRetriesWithoutProbe() =
        runTest {
            val h = Harness(this)
            h.startAndLogin()

            h.connection.emitFailure(failure(HandshakeFailureReason.Other))
            runCurrent()
            advanceBy(2_001)

            assertEquals(2, h.connection.connectCalls.size)
            assertEquals(0, h.api.meCalls)
        }

    @Test
    fun connectedStateResetsBackoff() =
        runTest {
            val h = Harness(this)
            h.startAndLogin()
            h.connection.emitFailure(failure(HandshakeFailureReason.ServerDisconnected))
            runCurrent()
            advanceBy(2_001)
            h.connection.emitFailure(failure(HandshakeFailureReason.ServerDisconnected))
            runCurrent()
            advanceBy(4_001)
            assertEquals(3, h.connection.connectCalls.size)

            h.connection.setState(ConnectionState.Connected)
            runCurrent()
            h.connection.emitFailure(failure(HandshakeFailureReason.ServerDisconnected))
            runCurrent()
            advanceBy(2_001)

            assertEquals(4, h.connection.connectCalls.size)
        }

    @Test
    fun backgroundCancelsPendingRetry() =
        runTest {
            val h = Harness(this)
            h.startAndLogin()
            h.connection.emitFailure(failure(HandshakeFailureReason.ServerDisconnected))
            runCurrent()

            h.foreground.isForeground.value = false
            runCurrent()
            advanceBy(60_000)

            assertEquals(1, h.connection.connectCalls.size)
        }

    @Test
    fun retryThatGoesToBackgroundDuringTokenReadDoesNotConnect() =
        runTest {
            val h = Harness(this)
            h.startAndLogin()
            h.connection.emitFailure(failure(HandshakeFailureReason.ServerDisconnected))
            runCurrent()
            h.managerTokens.onNextAccessRead = { h.foreground.isForeground.value = false }

            advanceBy(2_001)
            advanceBy(60_000)

            assertEquals(listOf("access"), h.connection.connectCalls)
            assertEquals(ConnectionState.Disconnected, h.connection.state.value)
        }

    @Test
    fun rotatedTokenRejectionQueuedBeforeBackgroundDoesNotReconnect() =
        runTest {
            val h = Harness(this)
            h.startAndLogin()
            h.tokens.save(TokenPair("access-2", "refresh-2", 1_900_000_100L))
            h.foreground.isForeground.value = false
            runCurrent()

            h.connection.emitFailure(failure(HandshakeFailureReason.InvalidToken, token = "old"))
            runCurrent()
            advanceBy(60_000)

            assertEquals(listOf("access"), h.connection.connectCalls)
            assertEquals(ConnectionState.Disconnected, h.connection.state.value)
        }

    @Test
    fun tokenRejectionQueuedBeforeBackgroundDoesNotProbe() =
        runTest {
            val h = Harness(this)
            h.api.meHandler = { me }
            h.startAndLogin()
            h.foreground.isForeground.value = false
            runCurrent()

            h.connection.emitFailure(failure(HandshakeFailureReason.InvalidToken))
            runCurrent()
            advanceBy(60_000)

            assertEquals(0, h.api.meCalls)
            assertEquals(listOf("access"), h.connection.connectCalls)
        }

    @Test
    fun tokenRejectionThatGoesToBackgroundDuringTokenReadDoesNotProbe() =
        runTest {
            val h = Harness(this)
            h.api.meHandler = { me }
            h.startAndLogin()
            h.managerTokens.onNextAccessRead = { h.foreground.isForeground.value = false }

            h.connection.emitFailure(failure(HandshakeFailureReason.InvalidToken))
            runCurrent()
            advanceBy(60_000)

            assertEquals(0, h.api.meCalls)
            assertEquals(listOf("access"), h.connection.connectCalls)
        }

    @Test
    fun logoutAndFreshLoginWithSameTokenProbesAgain() =
        runTest {
            val h = Harness(this)
            h.api.meHandler = { me }
            h.startAndLogin()
            h.connection.emitFailure(failure(HandshakeFailureReason.InvalidToken))
            runCurrent()
            assertEquals(1, h.api.meCalls)

            h.userManager.logout()
            runCurrent()
            h.userManager.login("a@b.co", "pw")
            runCurrent()
            h.connection.emitFailure(failure(HandshakeFailureReason.InvalidToken))
            runCurrent()

            assertEquals(2, h.api.meCalls)
        }
}
