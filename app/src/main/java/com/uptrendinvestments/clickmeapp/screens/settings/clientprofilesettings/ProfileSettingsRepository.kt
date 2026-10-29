package com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings

import com.uptrendinvestments.clickmeapp.model.requests.UpdateUserProfileRequest
import com.uptrendinvestments.clickmeapp.model.responses.LanguageItem
import com.uptrendinvestments.clickmeapp.model.responses.UserProfileData
import com.uptrendinvestments.clickmeapp.network.ClickMeApi
import com.uptrendinvestments.clickmeapp.network.apiCall
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

/**
 * `PATCH /user/profile`, the avatar upload and the language catalogue for the Profile Settings screens (shared by the
 * client and expert screens). Throws [com.uptrendinvestments.clickmeapp.network.ApiException] only.
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

        /** Uploads the cropped JPEG as multipart field `image` (same contract as signup) and returns the avatar URL. */
        suspend fun uploadAvatar(jpeg: ByteArray): String {
            val body = jpeg.toRequestBody("image/jpeg".toMediaType())
            val part = MultipartBody.Part.createFormData("image", "avatar.jpg", body)
            return apiCall { api.uploadAvatar(part) }.data.avatarUrl
        }

        /** The language catalogue (`GET /meta/languages`). */
        suspend fun languages(): List<LanguageItem> = apiCall { api.getLanguages() }.data.languages
    }
