package com.uptrendinvestments.clickmeapp.managers

/**
 * Push-token registration hook (D-09). This base class is the no-op used by tests; the app binds
 * [FcmPushTokenRegistrar] to it in `PushModule`. It has no Hilt annotation and must never gain constructor
 * parameters: existing tests build `PushTokenRegistrar()` and subclass it (RESEARCH F-4).
 *
 * [unregister] is called by `UserManager.logout` before the tokens are cleared, so the `DELETE` still has a
 * bearer token, as on iOS. Open so tests and the FCM implementation can substitute behavior.
 */
open class PushTokenRegistrar {
    /** Registers [token], or the current FCM token when null. */
    open suspend fun register(token: String? = null) {}

    open suspend fun unregister() {}
}
