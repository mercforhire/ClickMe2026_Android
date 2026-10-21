package com.uptrendinvestments.clickmeapp.screens.signup

import com.uptrendinvestments.clickmeapp.model.responses.ExpertiseTagItem
import com.uptrendinvestments.clickmeapp.model.responses.LanguageItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SignupDraftTest {
    private val full =
        SignupDraft(
            firstName = "Ada",
            city = "Toronto",
            countryCode = "CAN",
            languages = listOf(LanguageItem("en", "English")),
            timezone = "America/Toronto",
            avatarUrl = "https://cdn/x.jpg",
            expertiseTags = listOf(ExpertiseTagItem("t1", "Tax")),
        )

    @Test
    fun defaultDraftHasNothingComplete() {
        val draft = SignupDraft()
        assertEquals(0, draft.completedCount)
        assertFalse(draft.canPublish)
        assertEquals(
            listOf(SetupStep.PersonalDetails, SetupStep.Timezone, SetupStep.ProfilePhoto, SetupStep.Expertise),
            draft.missingForPublish,
        )
    }

    @Test
    fun personalDetailsNeedsNameCityCountryAndLanguageButNotProvince() {
        assertTrue(full.copy(provinceState = "").isComplete(SetupStep.PersonalDetails))
        assertFalse(full.copy(firstName = " ").isComplete(SetupStep.PersonalDetails))
        assertFalse(full.copy(city = "").isComplete(SetupStep.PersonalDetails))
        assertFalse(full.copy(countryCode = "").isComplete(SetupStep.PersonalDetails))
        assertFalse(full.copy(languages = emptyList()).isComplete(SetupStep.PersonalDetails))
    }

    @Test
    fun otherStepsFollowTheirOwnField() {
        assertFalse(SignupDraft().isComplete(SetupStep.Timezone))
        assertTrue(SignupDraft(timezone = "UTC").isComplete(SetupStep.Timezone))
        assertFalse(SignupDraft(avatarUrl = " ").isComplete(SetupStep.ProfilePhoto))
        assertTrue(SignupDraft(avatarUrl = "u").isComplete(SetupStep.ProfilePhoto))
        assertTrue(SignupDraft(emailVerified = true).isComplete(SetupStep.VerifyEmail))
        assertTrue(full.isComplete(SetupStep.Expertise))
        assertFalse(full.copy(expertiseTags = emptyList()).isComplete(SetupStep.Expertise))
    }

    @Test
    fun verifiedEmailCountsTowardProgressButNotPublish() {
        assertTrue(full.canPublish)
        assertEquals(4, full.completedCount)
        assertEquals(5, full.copy(emailVerified = true).completedCount)
        assertTrue(full.missingForPublish.isEmpty())
    }

    @Test
    fun unverifiedEmailIsNeverListedAsMissingForPublish() {
        assertFalse(SetupStep.VerifyEmail in SignupDraft().missingForPublish)
    }

    @Test
    fun blockedStepIsIncompleteAndMissingEvenWithData() {
        val draft = full.copy(publishBlockedSteps = setOf(SetupStep.ProfilePhoto))
        assertFalse(draft.isComplete(SetupStep.ProfilePhoto))
        assertEquals(listOf(SetupStep.ProfilePhoto), draft.missingForPublish)
        assertFalse(draft.canPublish)
    }

    @Test
    fun fromMissingFieldMapsServerValues() {
        assertEquals(SetupStep.ProfilePhoto, SetupStep.fromMissingField("profile_image_url"))
        assertEquals(SetupStep.PersonalDetails, SetupStep.fromMissingField("first_name"))
        assertEquals(SetupStep.PersonalDetails, SetupStep.fromMissingField("location"))
        assertEquals(SetupStep.PersonalDetails, SetupStep.fromMissingField("languages"))
        assertEquals(SetupStep.Timezone, SetupStep.fromMissingField("timezone"))
        assertEquals(SetupStep.Expertise, SetupStep.fromMissingField("expertise_tags"))
        assertNull(SetupStep.fromMissingField("bogus"))
    }

    @Test
    fun stepsAreInHubOrder() {
        assertEquals(
            listOf(
                SetupStep.PersonalDetails,
                SetupStep.Timezone,
                SetupStep.ProfilePhoto,
                SetupStep.VerifyEmail,
                SetupStep.Expertise,
            ),
            SetupStep.entries.toList(),
        )
    }
}
