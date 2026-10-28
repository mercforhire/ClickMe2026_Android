package com.uptrendinvestments.clickmeapp.managers

import com.uptrendinvestments.clickmeapp.BuildConfig
import com.uptrendinvestments.clickmeapp.di.IoDispatcher
import com.uptrendinvestments.clickmeapp.model.requests.RegisterDeviceTokenRequest
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.network.ClickMeApi
import com.uptrendinvestments.clickmeapp.network.apiCall
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Registers the device's FCM token with the backend (PUSH-01) and detaches it on logout.
 *
 * Both calls are best effort, like iOS: an API failure never reaches the caller. Registration needs a
 * session, a non-blank token and a Firebase app (the [PushTokenSource] returns null without one). The backend
 * keeps one row per account, so registering the same token twice is harmless. Nothing here logs: the token
 * and device id never leave the authenticated register request.
 *
 * [unregister] gives the `DELETE` [UNREGISTER_TIMEOUT_MS] so logout never hangs offline, then deletes the FCM
 * token so a missed `DELETE` cannot keep delivering the previous user's pushes on a shared device (RESEARCH
 * Pitfall 11). A [register] that runs after [unregister] in the same session (same access token) is skipped,
 * so a foreground registration racing the logout cannot re-bind the device to the account that just left;
 * both calls share one [Mutex] so they cannot interleave.
 */
@Singleton
class FcmPushTokenRegistrar
    @Inject
    constructor(
        private val api: ClickMeApi,
        private val tokens: SecureTokenStore,
        private val tokenSource: PushTokenSource,
        private val preferences: PushPreferences,
        @IoDispatcher private val io: CoroutineDispatcher,
    ) : PushTokenRegistrar() {
        private val lock = Mutex()

        @Volatile
        private var unregisteredAccessToken: String? = null

        override suspend fun register(token: String?) {
            withContext(io) {
                lock.withLock {
                    if (!tokens.hasSession()) return@withLock
                    val access = tokens.accessToken
                    if (access != null && access == unregisteredAccessToken) return@withLock
                    val value = token ?: tokenSource.currentToken()
                    if (value.isNullOrBlank()) return@withLock
                    try {
                        apiCall {
                            api.registerDeviceToken(
                                RegisterDeviceTokenRequest(
                                    deviceId = preferences.deviceId(),
                                    token = value,
                                    appVersion = BuildConfig.VERSION_NAME,
                                ),
                            )
                        }
                    } catch (_: ApiException) {
                        // Best effort: the next foreground return registers again.
                    }
                }
            }
        }

        override suspend fun unregister() {
            withContext(io) {
                lock.withLock {
                    unregisteredAccessToken = tokens.accessToken
                    withTimeoutOrNull(UNREGISTER_TIMEOUT_MS) {
                        try {
                            apiCall { api.deleteDeviceToken(preferences.deviceId()) }
                        } catch (_: ApiException) {
                            // Best effort: the token deletion below still detaches this device.
                        }
                    }
                    tokenSource.deleteToken()
                }
            }
        }

        companion object {
            const val UNREGISTER_TIMEOUT_MS = 3_000L
        }
    }
