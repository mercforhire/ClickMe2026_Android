package com.uptrendinvestments.clickmeapp.screens.signup

import androidx.annotation.StringRes
import com.uptrendinvestments.clickmeapp.R

/**
 * One row of the signup setup hub, in hub order. [titleRes] is the row title; the Review publish refusal
 * ("Finish these first: ...") names missing steps by these same titles, as iOS does.
 */
enum class SetupStep(
    @StringRes val titleRes: Int,
) {
    PersonalDetails(R.string.signup_step_personal_details),
    Timezone(R.string.signup_step_timezone),
    ProfilePhoto(R.string.signup_step_profile_photo),
    VerifyEmail(R.string.signup_step_verify_email),
    Expertise(R.string.signup_step_expertise),
    ;

    companion object {
        /**
         * Maps a `missing_fields` value from a 422 on `PATCH /expert/profile/setup` to the hub step that fixes it
         * (D-06). The values come from `computeMissing` in the backend's `onboarding.controller.js`. Unknown
         * values return null so a newer server never crashes the client.
         */
        fun fromMissingField(field: String): SetupStep? =
            when (field) {
                "profile_image_url" -> ProfilePhoto
                "first_name", "location", "languages" -> PersonalDetails
                "timezone" -> Timezone
                "expertise_tags" -> Expertise
                else -> null
            }
    }
}

/** Progress of [SignupSetupRepository.hydrate]. */
enum class HydrationState { Idle, Loading, Loaded, Failed }
