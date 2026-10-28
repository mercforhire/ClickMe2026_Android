package com.uptrendinvestments.clickmeapp.managers

import com.uptrendinvestments.clickmeapp.di.ApplicationScope
import com.uptrendinvestments.clickmeapp.realtime.ProcessForeground
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App-scoped policy for FCM token registration: register whenever a session exists and the process comes to
 * the foreground (PUSH-01). One condition covers app start after restore, login, signup and every foreground
 * return, like iOS `applicationDidBecomeActive` plus `fetchAndUploadFCMToken`.
 *
 * A process FCM starts in the background is never foreground, so it does not register here; token rotation
 * is covered by `ClickMeMessagingService.onNewToken`. Deliberately has no logging (it runs in plain JVM tests).
 */
@Singleton
class PushRegistrationManager
    @Inject
    constructor(
        private val userManager: UserManager,
        private val foreground: ProcessForeground,
        private val registrar: PushTokenRegistrar,
        @ApplicationScope private val scope: CoroutineScope,
    ) {
        private val started = AtomicBoolean(false)

        /** Idempotent. Call once from application startup. */
        fun start() {
            if (!started.compareAndSet(false, true)) return
            scope.launch {
                combine(userManager.isLoggedIn, foreground.isForeground) { loggedIn, fg -> loggedIn && fg }
                    .distinctUntilChanged()
                    .collect { wanted -> if (wanted) registrar.register() }
            }
        }
    }
