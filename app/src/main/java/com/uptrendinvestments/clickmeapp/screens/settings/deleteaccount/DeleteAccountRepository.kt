package com.uptrendinvestments.clickmeapp.screens.settings.deleteaccount

import com.uptrendinvestments.clickmeapp.di.IoDispatcher
import com.uptrendinvestments.clickmeapp.managers.SecureTokenStore
import com.uptrendinvestments.clickmeapp.model.requests.AccountDeletionRequest
import com.uptrendinvestments.clickmeapp.model.requests.DeleteAccountRequest
import com.uptrendinvestments.clickmeapp.network.ClickMeApi
import com.uptrendinvestments.clickmeapp.network.apiCall
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The two backend calls behind account deletion (CONTEXT D-05). Every call throws
 * [com.uptrendinvestments.clickmeapp.network.ApiException] only.
 *
 * The deletion token is single use and sensitive: [requestDeletion] returns it to the caller, and nothing here
 * stores, logs or caches it.
 */
@Singleton
class DeleteAccountRepository
    @Inject
    constructor(
        private val api: ClickMeApi,
        private val tokens: SecureTokenStore,
        @IoDispatcher private val io: CoroutineDispatcher,
    ) {
        /** `POST user/account/delete-request`: re-checks the [password] and answers the single-use token. */
        suspend fun requestDeletion(password: String): String =
            apiCall { api.requestAccountDeletion(AccountDeletionRequest(password)) }.data.deletionToken

        /**
         * `DELETE user/account`: removes the account and revokes every session. [feedback] is already trimmed and
         * null when blank. A 409 means the token is spent: the caller mints a new one to retry. On success the
         * remembered login email is forgotten before returning, so the Login shown after the Farewell (or after a
         * forced logout underneath it) starts empty. A thrown error leaves the email in place.
         */
        suspend fun deleteAccount(
            token: String,
            reason: DeleteAccountReason,
            feedback: String?,
        ) {
            apiCall { api.deleteAccount(DeleteAccountRequest(token, reason.wire, feedback)) }
            withContext(NonCancellable + io) { tokens.saveLastLoginEmail(null) }
        }
    }
