package com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings

import com.uptrendinvestments.clickmeapp.model.requests.UpdateUserProfileRequest
import com.uptrendinvestments.clickmeapp.model.responses.UserProfileData

const val NAME_MAX = 100
const val BIO_MAX = 500
const val BIO_COUNTER_FROM = 400
const val PROFESSIONAL_MAX = 200
const val LOCATION_MAX = 100

/** One spoken language: a catalogue entry carries an [id]; a custom one has only a [label]. */
data class ProfileLanguage(
    val id: String?,
    val label: String,
)

/** Everything the client Profile Settings screen edits, compared as a unit by the auto-saver. */
data class ProfileSnapshot(
    val firstName: String,
    val lastName: String,
    val bio: String,
    val jobTitle: String,
    val company: String,
    val city: String,
    val stateProvince: String,
    val countryCode: String,
    val languages: List<ProfileLanguage>,
) {
    companion object {
        fun from(profile: UserProfileData): ProfileSnapshot = TODO("not implemented")
    }
}

/** The PATCH body for the groups that changed between [old] and [new], or null when nothing changed. */
fun buildProfilePatch(
    old: ProfileSnapshot,
    new: ProfileSnapshot,
): UpdateUserProfileRequest? = TODO("not implemented")
