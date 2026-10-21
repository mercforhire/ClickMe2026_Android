package com.uptrendinvestments.clickmeapp.screens.signup

import com.uptrendinvestments.clickmeapp.model.responses.ExpertiseTagItem
import com.uptrendinvestments.clickmeapp.model.responses.LanguageItem

/**
 * The expert-signup setup draft: the single app-scoped source of truth read by the hub, every step and Review.
 * Port of iOS `SignupAccumulator`'s fields, minus the credential (Android never keeps one here).
 *
 * Completion rules follow the server (D-03), not iOS: province is optional, a photo is required, and a verified
 * email counts toward progress but is not needed to publish.
 */
data class SignupDraft(
    val email: String = "",
    val emailVerified: Boolean = false,
    val firstName: String = "",
    val city: String = "",
    val provinceState: String = "",
    val countryCode: String = "",
    val languages: List<LanguageItem> = emptyList(),
    val timezone: String = "",
    val avatarUrl: String? = null,
    val expertiseTags: List<ExpertiseTagItem> = emptyList(),
    /** Steps the server rejected on publish (D-06); shown incomplete until the next hydrate. */
    val publishBlockedSteps: Set<SetupStep> = emptySet(),
) {
    fun isComplete(step: SetupStep): Boolean {
        if (step in publishBlockedSteps) return false
        return when (step) {
            SetupStep.PersonalDetails ->
                firstName.isNotBlank() && city.isNotBlank() && countryCode.isNotBlank() && languages.isNotEmpty()
            SetupStep.Timezone -> timezone.isNotBlank()
            SetupStep.ProfilePhoto -> !avatarUrl.isNullOrBlank()
            SetupStep.VerifyEmail -> emailVerified
            SetupStep.Expertise -> expertiseTags.isNotEmpty()
        }
    }

    /** Completed hub steps, 0..5. */
    val completedCount: Int get() = SetupStep.entries.count(::isComplete)

    /** Incomplete steps that block publishing, in hub order. Never contains [SetupStep.VerifyEmail] (D-03). */
    val missingForPublish: List<SetupStep>
        get() = SetupStep.entries.filter { it != SetupStep.VerifyEmail && !isComplete(it) }

    val canPublish: Boolean get() = missingForPublish.isEmpty()
}
