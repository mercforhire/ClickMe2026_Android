package com.uptrendinvestments.clickmeapp.screens.settings.deleteaccount

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Whether the account-deleted Farewell is showing. A process-wide flag, set the instant `DELETE user/account`
 * returns and cleared only by the Farewell's "Back to Login".
 *
 * It is deliberately independent of `UserManager.isLoggedIn` (RESEARCH Pitfall 3): deleting revokes every session
 * server-side, so any later authenticated request can force a logout and swap the route to Login. The Farewell is
 * hosted at the root, above the route switch, and reads only this flag, so that teardown cannot remove it.
 */
@Singleton
class AccountDeletionState
    @Inject
    constructor() {
        private val _farewellVisible = MutableStateFlow(false)
        val farewellVisible: StateFlow<Boolean> = _farewellVisible.asStateFlow()

        /** The account is gone: show the Farewell. */
        fun markDeleted() {
            _farewellVisible.value = true
        }

        /** The user left the Farewell. */
        fun clear() {
            _farewellVisible.value = false
        }
    }
