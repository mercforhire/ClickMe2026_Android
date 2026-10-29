package com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings

import com.uptrendinvestments.clickmeapp.model.requests.ProfileBasicBody
import com.uptrendinvestments.clickmeapp.model.requests.ProfileLanguagesBody
import com.uptrendinvestments.clickmeapp.model.requests.ProfileLocationBody
import com.uptrendinvestments.clickmeapp.model.requests.ProfileProfessionalBody
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
        /** Nulls become blank; languages without a label are dropped. */
        fun from(profile: UserProfileData): ProfileSnapshot =
            ProfileSnapshot(
                firstName = profile.personalDetails.firstName.orEmpty(),
                lastName = profile.personalDetails.lastName.orEmpty(),
                bio = profile.personalDetails.bio.orEmpty(),
                jobTitle = profile.professionalDetails.jobTitle.orEmpty(),
                company = profile.professionalDetails.company.orEmpty(),
                city = profile.location.city.orEmpty(),
                stateProvince = profile.location.stateProvince.orEmpty(),
                countryCode = profile.location.country.orEmpty(),
                languages =
                    profile.languages
                        .map { ProfileLanguage(it.id, it.label.orEmpty()) }
                        .filter { it.label.isNotBlank() },
            )
    }
}

/**
 * The PATCH body for the groups that changed between [old] and [new], or null when nothing changed. A changed group is
 * sent whole with trimmed strings (an empty string clears a field, never null) so the server's per-key merge cannot
 * leave a stale half; groups are compared on their trimmed values, so trailing spaces alone never cause a request.
 * There is deliberately no email: the server answers 403.
 */
fun buildProfilePatch(
    old: ProfileSnapshot,
    new: ProfileSnapshot,
): UpdateUserProfileRequest? {
    val basic =
        ProfileBasicBody(new.firstName.trim(), new.lastName.trim(), new.bio.trim())
            .takeIf { it != ProfileBasicBody(old.firstName.trim(), old.lastName.trim(), old.bio.trim()) }
    val professional =
        ProfileProfessionalBody(new.jobTitle.trim(), new.company.trim())
            .takeIf { it != ProfileProfessionalBody(old.jobTitle.trim(), old.company.trim()) }
    val location =
        new
            .locationBody()
            .takeIf { it != old.locationBody() }
    val languages =
        new.languages
            .takeIf { it != old.languages }
            ?.let { list ->
                ProfileLanguagesBody(
                    languageIds = list.mapNotNull { language -> language.id?.takeIf { it.isNotBlank() } },
                    customLanguages = list.filter { it.id.isNullOrBlank() }.map { it.label },
                )
            }
    if (basic == null && professional == null && location == null && languages == null) return null
    return UpdateUserProfileRequest(
        basic = basic,
        location = location,
        professional = professional,
        languages = languages,
    )
}

private fun ProfileSnapshot.locationBody() =
    ProfileLocationBody(
        city = city.trim(),
        stateProvince = stateProvince.trim(),
        countryCode = countryCode.trim().ifBlank { null },
    )
