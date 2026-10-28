@file:OptIn(ExperimentalCoroutinesApi::class)

package com.uptrendinvestments.clickmeapp.managers

import com.uptrendinvestments.clickmeapp.model.responses.AuthUser
import com.uptrendinvestments.clickmeapp.model.responses.LoginData
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.FakeProcessForeground
import com.uptrendinvestments.clickmeapp.testing.FakeSecureTokenStore
import com.uptrendinvestments.clickmeapp.testing.testPreferencesDataStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException

class PushRegistrationManagerTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private class RecordingRegistrar : PushTokenRegistrar() {
        var registerCalls = 0
            private set

        override suspend fun register(token: String?) {
            registerCalls++
        }
    }

    private inner class Harness(
        val scope: TestScope,
        foregroundInitially: Boolean = true,
    ) {
        private val dispatcher = StandardTestDispatcher(scope.testScheduler)
        val api = FakeClickMeApi()
        val foreground = FakeProcessForeground(foregroundInitially)
        val registrar = RecordingRegistrar()
        val userManager =
            UserManager(
                api,
                FakeSecureTokenStore(null),
                SessionStore(testPreferencesDataStore(scope.backgroundScope, tmp)),
                SessionEvents(),
                PushTokenRegistrar(),
                scope.backgroundScope,
                dispatcher,
            )
        val manager = PushRegistrationManager(userManager, foreground, registrar, scope.backgroundScope)

        init {
            api.loginHandler = {
                LoginData(
                    token = "access",
                    user = AuthUser(id = "u1", firstName = "Ada", lastName = "Lovelace", roles = listOf("client")),
                    refreshToken = "refresh",
                    expiresAt = 1_900_000_000L,
                    expiresIn = 3600L,
                )
            }
            api.userProfileHandler = { throw IOException("not needed") }
        }

        suspend fun login() {
            userManager.login("a@b.co", "pw")
            scope.runCurrent()
        }
    }

    @Test
    fun registersWhenLoggedInAndForeground() =
        runTest {
            val h = Harness(this)
            h.manager.start()
            runCurrent()

            h.login()

            assertEquals(1, h.registrar.registerCalls)
        }

    @Test
    fun noRegistrationWhileLoggedOut() =
        runTest {
            val h = Harness(this)
            h.manager.start()
            runCurrent()

            h.foreground.isForeground.value = false
            runCurrent()
            h.foreground.isForeground.value = true
            runCurrent()

            assertEquals(0, h.registrar.registerCalls)
        }

    @Test
    fun registersOnEachReturnToForeground() =
        runTest {
            val h = Harness(this)
            h.manager.start()
            runCurrent()
            h.login()
            assertEquals(1, h.registrar.registerCalls)

            h.foreground.isForeground.value = false
            runCurrent()
            assertEquals(1, h.registrar.registerCalls)
            h.foreground.isForeground.value = true
            runCurrent()
            assertEquals(2, h.registrar.registerCalls)
        }

    @Test
    fun loginWhileBackgroundedWaitsForForeground() =
        runTest {
            val h = Harness(this, foregroundInitially = false)
            h.manager.start()
            runCurrent()

            h.login()
            assertEquals(0, h.registrar.registerCalls)

            h.foreground.isForeground.value = true
            runCurrent()
            assertEquals(1, h.registrar.registerCalls)
        }

    @Test
    fun startIsIdempotent() =
        runTest {
            val h = Harness(this)
            h.manager.start()
            h.manager.start()
            runCurrent()

            h.login()

            assertEquals(1, h.registrar.registerCalls)
        }
}
