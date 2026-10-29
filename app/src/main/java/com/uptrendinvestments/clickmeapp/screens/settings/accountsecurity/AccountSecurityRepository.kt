package com.uptrendinvestments.clickmeapp.screens.settings.accountsecurity

import com.uptrendinvestments.clickmeapp.model.requests.UpdatePasswordRequest
import com.uptrendinvestments.clickmeapp.network.ClickMeApi
import com.uptrendinvestments.clickmeapp.network.apiCall
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Account Security network access. Mirrors iOS `Settings/Account Security`. Every call throws
 * [com.uptrendinvestments.clickmeapp.network.ApiException] only.
 */
@Singleton
class AccountSecurityRepository
    @Inject
    constructor(
        private val api: ClickMeApi,
    ) {
        /** `PATCH /user/password`. A wrong current password is a 403, a weak new one a 422. The session stays valid. */
        suspend fun updatePassword(
            current: String,
            new: String,
        ) {
            apiCall { api.updatePassword(UpdatePasswordRequest(current, new)) }
        }
    }
