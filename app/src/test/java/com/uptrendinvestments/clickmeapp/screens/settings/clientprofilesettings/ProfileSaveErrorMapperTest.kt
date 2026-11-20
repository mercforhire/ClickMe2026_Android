package com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/** Ports iOS `ProfileSaveErrorMapperTests`: the 422 alert names fields in iOS-owned copy and screen order. */
class ProfileSaveErrorMapperTest {
    private val generic = "Please check the information you entered and try again."

    @Test
    fun productionBodyNamesFirstAndLastName() {
        val text = ProfileSaveErrorMapper.message(listOf("basic.first_name", "basic.last_name"))

        assertEquals("Check First Name and Last Name, then try again.", text)
        assertFalse(text.contains("basic."))
    }

    @Test
    fun oneFieldReadsAsIs() {
        assertEquals("Check Last Name, then try again.", ProfileSaveErrorMapper.message(listOf("basic.last_name")))
    }

    @Test
    fun fieldsComeBackInScreenOrderNotServerOrder() {
        val text = ProfileSaveErrorMapper.message(listOf("location.city", "basic.bio", "basic.first_name"))

        assertEquals("Check First Name, Bio and City, then try again.", text)
    }

    @Test
    fun arrayItemPathsCollapseToOneLabel() {
        val text = ProfileSaveErrorMapper.message(listOf("languages.language_ids.0", "languages.custom_languages"))

        assertEquals("Check Spoken Languages, then try again.", text)
    }

    @Test
    fun everyTableFieldHasItsOnScreenLabel() {
        val text =
            ProfileSaveErrorMapper.message(
                listOf(
                    "location.country_code",
                    "location.state_province",
                    "location.city",
                    "professional.company",
                    "professional.job_title",
                    "basic.bio",
                    "basic.last_name",
                    "basic.first_name",
                    "languages.language_ids",
                ),
            )

        assertEquals(
            "Check First Name, Last Name, Bio, Job Title, Company, City, State/Province, Country " +
                "and Spoken Languages, then try again.",
            text,
        )
    }

    @Test
    fun aPathThatOnlySharesAPrefixDoesNotMatch() {
        assertEquals(generic, ProfileSaveErrorMapper.message(listOf("basic.first_name_extra")))
    }

    @Test
    fun anUnknownFieldIsIgnoredAndKnownOnesStillNamed() {
        val text = ProfileSaveErrorMapper.message(listOf("location.timezone_id", "basic.bio"))

        assertEquals("Check Bio, then try again.", text)
    }

    @Test
    fun noKnownFieldGivesTheGenericSentence() {
        assertEquals(generic, ProfileSaveErrorMapper.message(listOf("location.timezone_id")))
        assertEquals(generic, ProfileSaveErrorMapper.message(emptyList()))
    }
}
