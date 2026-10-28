package com.uptrendinvestments.clickmeapp.managers

import com.uptrendinvestments.clickmeapp.BuildConfig
import com.uptrendinvestments.clickmeapp.di.IoDispatcher
import com.uptrendinvestments.clickmeapp.model.requests.RegisterDeviceTokenRequest
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.network.ClickMeApi
import com.uptrendinvestments.clickmeapp.network.apiCall
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Registers the device's FCM token with the backend (PUSH-01) and detaches it on logout.
 *
 * Both calls are best effort, like iOS: an API failure never reaches the caller. Registration needs a
 * session, a non-blank token and a Firebase app (the [PushTokenSource] returns null without one). The backend
 * keeps one row per account, so registering the same token twice is harmless. Nothing here logs: the token
 * and device id never leave the authenticated register request.
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
        override suspend fun register(token: String?) {
            withContext(io) {
                if (!tokens.hasSession()) return@withContext
                val value = token ?: tokenSource.currentToken()
                if (value.isNullOrBlank()) return@withContext
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
