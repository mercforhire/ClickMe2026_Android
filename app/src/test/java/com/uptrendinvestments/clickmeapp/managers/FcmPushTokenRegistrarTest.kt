package com.uptrendinvestments.clickmeapp.managers

import com.uptrendinvestments.clickmeapp.BuildConfig
import com.uptrendinvestments.clickmeapp.model.requests.RegisterDeviceTokenRequest
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.FakePushTokenSource
import com.uptrendinvestments.clickmeapp.testing.FakeSecureTokenStore
import com.uptrendinvestments.clickmeapp.testing.testPreferencesDataStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import retrofit2.HttpException
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class FcmPushTokenRegistrarTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private class Harness(
        scope: TestScope,
        tmp: TemporaryFolder,
        session: TokenPair? = TokenPair("access-1", "refresh-1", 1_900_000_000L),
        token: String? = "fcm-token-1",
    ) {
        val api = FakeClickMeApi()
        val tokens = FakeSecureTokenStore(session)
        val source = FakePushTokenSource(token)
        val preferences = PushPreferences(testPreferencesDataStore(scope.backgroundScope, tmp, "push.preferences_pb"))
        val registrar =
            FcmPushTokenRegistrar(
                api,
                tokens,
                source,
                preferences,
                StandardTestDispatcher(scope.testScheduler),
            )
    }

    @Test
    fun registerPostsAndroidBody() =
        runTest {
            val h = Harness(this, tmp)

            h.registrar.register()

            assertEquals(
                listOf(
                    RegisterDeviceTokenRequest(
                        deviceId = h.preferences.deviceId(),
                        token = "fcm-token-1",
                        platform = "android",
                        appVersion = BuildConfig.VERSION_NAME,
                    ),
                ),
                h.api.registeredDeviceTokens,
            )
        }

    @Test
    fun registerUsesTheGivenToken() =
        runTest {
            val h = Harness(this, tmp)

            h.registrar.register("rotated")

            assertEquals(listOf("rotated"), h.api.registeredDeviceTokens.map { it.token })
            assertEquals(0, h.source.currentTokenCalls)
        }

    @Test
    fun noSessionSendsNothing() =
        runTest {
            val h = Harness(this, tmp, session = null)

            h.registrar.register()
            h.registrar.register("rotated")

            assertTrue(h.api.registeredDeviceTokens.isEmpty())
        }

    @Test
    fun nullOrBlankTokenSendsNothing() =
        runTest {
            val h = Harness(this, tmp, token = null)
            h.registrar.register()

            h.source.token = "  "
            h.registrar.register()
            h.registrar.register("")

            assertTrue(h.api.registeredDeviceTokens.isEmpty())
        }

    @Test
    fun registerTwiceSendsTheSameBody() =
        runTest {
            val h = Harness(this, tmp)

            h.registrar.register()
            h.registrar.register()

            assertEquals(2, h.api.registeredDeviceTokens.size)
            assertEquals(h.api.registeredDeviceTokens[0], h.api.registeredDeviceTokens[1])
        }

    @Test
    fun serverErrorIsSwallowed() =
        runTest {
            val h = Harness(this, tmp)
            val json = "application/json".toMediaType()
            val body = "{}".toResponseBody(json)
            h.api.registerDeviceTokenHandler = { throw HttpException(Response.error<Any>(500, body)) }

            h.registrar.register()

            assertEquals(1, h.api.registeredDeviceTokens.size)
        }

    @Test
    fun tokenNeverAppearsInToString() =
        runTest {
            val h = Harness(this, tmp)
            h.registrar.register()

            val request = h.api.registeredDeviceTokens.single()

            assertFalse(request.toString().contains("fcm-token-1"))
        }
}
