package com.uptrendinvestments.clickmeapp.managers

import javax.inject.Inject

/**
 * Push-token registration hook (D-09). No-op until push registration lands (Phase 13, PUSH-01).
 *
 * [unregister] is called by `UserManager.logout` before the tokens are cleared, so the future
 * `DELETE` still has a bearer token, as on iOS. Open so tests and Phase 13 can substitute behavior.
 */
open class PushTokenRegistrar
    @Inject
    constructor() {
        open suspend fun unregister() {}
    }
