package com.uptrendinvestments.clickmeapp.screens.expertmode

import com.uptrendinvestments.clickmeapp.model.enums.ErrorCode
import com.uptrendinvestments.clickmeapp.model.requests.AvailabilityTimeRangeBody
import com.uptrendinvestments.clickmeapp.model.requests.CreateExpertTopicRequest
import com.uptrendinvestments.clickmeapp.model.requests.ExpertTagSelection
import com.uptrendinvestments.clickmeapp.model.requests.TopicHourlyRateBody
import com.uptrendinvestments.clickmeapp.model.requests.UpdateExpertTagsRequest
import com.uptrendinvestments.clickmeapp.model.requests.UpdateExpertTopicRequest
import com.uptrendinvestments.clickmeapp.model.requests.UpdateMyAvailabilityRequest
import com.uptrendinvestments.clickmeapp.model.requests.WeeklyScheduleBody
import com.uptrendinvestments.clickmeapp.model.responses.ExpertProfileData
import com.uptrendinvestments.clickmeapp.model.responses.ExpertTag
import com.uptrendinvestments.clickmeapp.model.responses.ExpertiseTagItem
import com.uptrendinvestments.clickmeapp.model.responses.ExpertiseTagsPayload
import com.uptrendinvestments.clickmeapp.model.responses.MyExpertTopicPayload
import com.uptrendinvestments.clickmeapp.model.responses.MyExpertTopicsPayload
import com.uptrendinvestments.clickmeapp.model.responses.SuccessMessageResponse
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.screens.expertmode.availability.AvailabilityRepository
import com.uptrendinvestments.clickmeapp.screens.expertmode.settings.ExpertiseTagsRepository
import com.uptrendinvestments.clickmeapp.screens.expertmode.topics.ExpertTopicsRepository
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.availabilityData
import com.uptrendinvestments.clickmeapp.testing.topicItem
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ExpertRepositoriesTest {
    private val api = FakeClickMeApi()
    private val topics = ExpertTopicsRepository(api)
    private val tags = ExpertiseTagsRepository(api)
    private val availability = AvailabilityRepository(api)

    private val ok = SuccessMessageResponse(status = "success")

    private val createBody =
        CreateExpertTopicRequest(
            title = "Tax planning",
            description = null,
            durationMins = 60,
            isFree = false,
            hourlyRate = TopicHourlyRateBody(2500, "CAD"),
            iconSlug = "finance",
            expertiseTagIds = listOf("tax"),
        )
    private val updateBody =
        UpdateExpertTopicRequest(
            title = "Tax planning 2",
            description = null,
            durationMins = 30,
            isFree = true,
            hourlyRate = TopicHourlyRateBody(0, "CAD"),
            iconSlug = null,
            expertiseTagIds = emptyList(),
        )

    @Test
    fun listReturnsTopicsInServerOrder() =
        runTest {
            val items = listOf(topicItem("t2"), topicItem("t1"))
            api.topicsHandler = { MyExpertTopicsPayload(items) }

            assertEquals(items, topics.list())
            assertEquals(1, api.topicsCalls)
        }

    @Test
    fun listSurfacesHttpFailureAsApiException() =
        runTest {
            api.topicsHandler = { throw api.httpError(500, """{"message":"boom"}""") }

            try {
                topics.list()
                fail("expected ApiException")
            } catch (e: ApiException.Http) {
                assertEquals(500, e.status)
            }
        }

    @Test
    fun createPassesBodyThroughAndReturnsTopic() =
        runTest {
            val created = topicItem("t9")
            api.createTopicHandler = { MyExpertTopicPayload(created) }

            assertEquals(created, topics.create(createBody))
            assertEquals(listOf(createBody), api.createTopicRequests)
        }

    @Test
    fun updatePassesIdAndBodyThroughAndReturnsTopic() =
        runTest {
            val updated = topicItem("t1", title = "Tax planning 2")
            api.updateTopicHandler = { _, _ -> MyExpertTopicPayload(updated) }

            assertEquals(updated, topics.update("t1", updateBody))
            assertEquals(listOf("t1" to updateBody), api.updateTopicRequests)
        }

    @Test
    fun deleteCallsHandlerWithIdAndSurfacesNotFound() =
        runTest {
            api.deleteTopicHandler = { ok }
            topics.delete("t1")
            assertEquals(listOf("t1"), api.deletedTopicIds)

            api.deleteTopicHandler = {
                throw api.httpError(404, """{"code":"NOT_FOUND","message":"Topic not found"}""")
            }
            try {
                topics.delete("t2")
                fail("expected ApiException")
            } catch (e: ApiException.Http) {
                assertTrue(e.code == ErrorCode.NotFound || e.serverMessage == "Topic not found")
            }
        }

    @Test
    fun profileTagsReturnsServerTagsOrEmptyWhenNull() =
        runTest {
            val serverTags = listOf(ExpertTag("tax", "Tax", isPrimary = true))
            api.expertProfileHandler = { ExpertProfileData(setupCompleted = true, expertiseTags = serverTags) }
            assertEquals(serverTags, tags.profileTags())

            api.expertProfileHandler = { ExpertProfileData(setupCompleted = true, expertiseTags = null) }
            assertEquals(emptyList<ExpertTag>(), tags.profileTags())
        }

    @Test
    fun catalogIsSortedCaseInsensitivelyByLabel() =
        runTest {
            val law = ExpertiseTagItem("l", "Law")
            val audit = ExpertiseTagItem("a", "audit")
            val tax = ExpertiseTagItem("t", "tax")
            api.expertiseTagsHandler = { ExpertiseTagsPayload(listOf(tax, law, audit)) }

            assertEquals(listOf(audit, law, tax), tags.catalog())
        }

    @Test
    fun saveProfileTagsLowerCasesIdsAndMarksFirstPrimary() =
        runTest {
            api.updateExpertProfileTagsHandler = { ok }

            tags.saveProfileTags(listOf(ExpertiseTagItem("AB-1", "x"), ExpertiseTagItem("cd-2", "y")))

            val expected =
                UpdateExpertTagsRequest(
                    listOf(
                        ExpertTagSelection("ab-1", true),
                        ExpertTagSelection("cd-2", false),
                    ),
                )
            assertEquals(listOf(expected), api.updateExpertProfileTagsRequests)
        }

    @Test
    fun availabilityGetReturnsHandlerDataAndPutPassesBody() =
        runTest {
            val data = availabilityData()
            api.myAvailabilityHandler = { data }
            api.updateMyAvailabilityHandler = { ok }
            val day = listOf(AvailabilityTimeRangeBody("09:00", "17:00"))
            val body =
                UpdateMyAvailabilityRequest(
                    timezoneId = "America/New_York",
                    schedule =
                        WeeklyScheduleBody(
                            monday = day,
                            tuesday = emptyList(),
                            wednesday = emptyList(),
                            thursday = emptyList(),
                            friday = emptyList(),
                            saturday = emptyList(),
                            sunday = emptyList(),
                        ),
                )

            assertEquals(data, availability.get())
            availability.put(body)

            assertEquals(listOf(body), api.updateMyAvailabilityRequests)
        }
}
