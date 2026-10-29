package com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings

import com.uptrendinvestments.clickmeapp.model.requests.UpdateUserProfileRequest
import com.uptrendinvestments.clickmeapp.model.responses.UserProfileData
import com.uptrendinvestments.clickmeapp.network.ClickMeApi
import com.uptrendinvestments.clickmeapp.network.apiCall
import javax.inject.Inject
import javax.inject.Singleton

/**
 * `PATCH /user/profile` for the Profile Settings screens (shared by the client and expert screens). Throws
 * [com.uptrendinvestments.clickmeapp.network.ApiException] only.
 */
@Singleton
class ProfileSettingsRepository
    @Inject
    constructor(
        private val api: ClickMeApi,
    ) {
        /** Sends [body] and returns the refreshed profile the server answers with. */
        suspend fun updateProfile(body: UpdateUserProfileRequest): UserProfileData =
            apiCall { api.updateUserProfile(body) }.data
    }
