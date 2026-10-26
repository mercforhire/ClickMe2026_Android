package com.uptrendinvestments.clickmeapp.network

import com.uptrendinvestments.clickmeapp.model.requests.AvailabilityTimeRangeBody
import com.uptrendinvestments.clickmeapp.model.requests.CreateExpertTopicRequest
import com.uptrendinvestments.clickmeapp.model.requests.ExpertTagSelection
import com.uptrendinvestments.clickmeapp.model.requests.TopicHourlyRateBody
import com.uptrendinvestments.clickmeapp.model.requests.UpdateExpertTagsRequest
import com.uptrendinvestments.clickmeapp.model.requests.UpdateExpertTopicRequest
import com.uptrendinvestments.clickmeapp.model.requests.UpdateMyAvailabilityRequest
import com.uptrendinvestments.clickmeapp.model.requests.WeeklyScheduleBody
import org.junit.Assert.assertEquals
import org.junit.Test

class ExpertTopicRequestJsonTest {
    private val moshi = MoshiProvider.moshi

    private fun update(
        description: String? = null,
        iconSlug: String? = null,
        durationMins: Int? = 60,
    ) = UpdateExpertTopicRequest(
        title = "T",
        description = description,
        durationMins = durationMins,
        isFree = false,
        hourlyRate = TopicHourlyRateBody(2500, "CAD"),
        iconSlug = iconSlug,
        expertiseTagIds = listOf("a1"),
    )

    @Test
    fun patchBodyWritesExplicitNullsForClearedFields() {
        assertEquals(
            """{"title":"T","description":null,"duration_mins":60,"is_free":false,""" +
                """"hourly_rate":{"amount":2500,"currency":"CAD"},"icon_slug":null,"expertise_tag_ids":["a1"]}""",
            moshi.adapter(UpdateExpertTopicRequest::class.java).toJson(update()),
        )
    }

    @Test
    fun patchBodyWritesProvidedStrings() {
        assertEquals(
            """{"title":"T","description":"D","duration_mins":60,"is_free":false,""" +
                """"hourly_rate":{"amount":2500,"currency":"CAD"},"icon_slug":"tech","expertise_tag_ids":["a1"]}""",
            moshi.adapter(UpdateExpertTopicRequest::class.java).toJson(update("D", "tech")),
        )
    }

    @Test
    fun patchBodyOmitsAnUnchangedDuration() {
        assertEquals(
            """{"title":"T","description":null,"is_free":false,""" +
                """"hourly_rate":{"amount":2500,"currency":"CAD"},"icon_slug":null,"expertise_tag_ids":["a1"]}""",
            moshi.adapter(UpdateExpertTopicRequest::class.java).toJson(update(durationMins = null)),
        )
    }

    @Test
    fun createBodyOmitsNullDescriptionAndIcon() {
        val body =
            CreateExpertTopicRequest(
                title = "T",
                durationMins = 60,
                isFree = true,
                hourlyRate = TopicHourlyRateBody(500, "CAD"),
                expertiseTagIds = emptyList(),
            )
        assertEquals(
            """{"title":"T","duration_mins":60,"is_free":true,""" +
                """"hourly_rate":{"amount":500,"currency":"CAD"},"expertise_tag_ids":[]}""",
            moshi.adapter(CreateExpertTopicRequest::class.java).toJson(body),
        )
    }

    @Test
    fun availabilityBodyAlwaysWritesAllSevenDays() {
        val empty = emptyList<AvailabilityTimeRangeBody>()
        val body =
            UpdateMyAvailabilityRequest(
                timezoneId = "America/Toronto",
                schedule =
                    WeeklyScheduleBody(
                        monday = listOf(AvailabilityTimeRangeBody("09:00", "17:00")),
                        tuesday = empty,
                        wednesday = empty,
                        thursday = empty,
                        friday = empty,
                        saturday = empty,
                        sunday = empty,
                    ),
            )
        assertEquals(
            """{"timezone_id":"America/Toronto","schedule":{"monday":[{"start_time":"09:00","end_time":"17:00"}],""" +
                """"tuesday":[],"wednesday":[],"thursday":[],"friday":[],"saturday":[],"sunday":[]}}""",
            moshi.adapter(UpdateMyAvailabilityRequest::class.java).toJson(body),
        )
    }

    @Test
    fun expertiseTagsBodyShape() {
        val body =
            UpdateExpertTagsRequest(
                listOf(ExpertTagSelection("a1", true), ExpertTagSelection("b2", false)),
            )
        assertEquals(
            """{"expertise_tags":[{"tag_id":"a1","is_primary":true},{"tag_id":"b2","is_primary":false}]}""",
            moshi.adapter(UpdateExpertTagsRequest::class.java).toJson(body),
        )
    }
}
