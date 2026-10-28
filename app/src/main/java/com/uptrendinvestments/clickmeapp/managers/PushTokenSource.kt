package com.uptrendinvestments.clickmeapp.managers

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The seam to the FCM token. [FirebasePushTokenSource] is the only class that touches Firebase for tokens,
 * so JVM tests use `FakePushTokenSource` and never fake Firebase.
 */
interface PushTokenSource {
    /** The current FCM token, or null when Firebase is unavailable or the lookup failed. */
    suspend fun currentToken(): String?

    /** Deletes the FCM token so the previous user's pushes stop reaching this device. Best effort. */
    suspend fun deleteToken()
}

/**
 * Firebase-backed [PushTokenSource]. Without a Firebase app (no `google-services.json`, RESEARCH F-3) every
 * call is a quiet no-op, so no Firebase exception can reach the app.
 */
@Singleton
class FirebasePushTokenSource
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : PushTokenSource {
        private fun firebaseAvailable(): Boolean = FirebaseApp.getApps(context).isNotEmpty()

        @Suppress("TooGenericExceptionCaught")
        override suspend fun currentToken(): String? {
            if (!firebaseAvailable()) return null
            return try {
                FirebaseMessaging.getInstance().token.await()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }
        }

        @Suppress("TooGenericExceptionCaught")
        override suspend fun deleteToken() {
            if (!firebaseAvailable()) return
            try {
                FirebaseMessaging.getInstance().deleteToken().await()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Best effort: a failed delete must never block logout.
            }
        }
    }
