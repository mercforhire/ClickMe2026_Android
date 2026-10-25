package com.uptrendinvestments.clickmeapp.network

import com.squareup.moshi.Types
import com.uptrendinvestments.clickmeapp.model.responses.ExpertAvailabilityScheduleData
import com.uptrendinvestments.clickmeapp.model.responses.MyExpertTopicPayload
import com.uptrendinvestments.clickmeapp.model.responses.MyExpertTopicsPayload
import com.uptrendinvestments.clickmeapp.model.responses.SuccessDataResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpertTopicDtoTest {
    private val moshi = MoshiProvider.moshi

    private fun <T> envelope(data: Class<T>) =
        moshi.adapter<SuccessDataResponse<T>>(
            Types.newParameterizedType(SuccessDataResponse::class.java, data),
        )

    @Test
    fun listEnvelopeDecodesPaidAndFreeTopics() {
        val json =
            """
            {"status":"success","data":{"topics":[
              {"id":"t1","title":"Tax planning","description":"Plan","duration_mins":60,"icon_slug":"finance",
               "price":{"amount":2500,"currency":"CAD","is_free":false,"label":"${'$'}25 / session"},
               "expertise_tags":[{"id":"tag-1","label":"Tax","category_id":"c1"}]},
              {"id":"t2","title":"Intro chat","description":null,"duration_mins":30,"icon_slug":null,
               "price":{"amount":0,"currency":"","is_free":true,"label":"Free"},"expertise_tags":[]}
            ]}}
            """.trimIndent()
        val topics = envelope(MyExpertTopicsPayload::class.java).fromJson(json)!!.data.topics
        val paid = topics[0]
        assertEquals(2500L, paid.amountMinor)
        assertEquals("CAD", paid.storedCurrency)
        assertFalse(paid.isFree)
        assertEquals("finance", paid.iconSlug)
        assertEquals("tag-1", paid.expertiseTags[0].id)
        val free = topics[1]
        assertTrue(free.isFree)
        assertNull(free.storedCurrency)
        assertNull(free.hourlyRate)
        assertNull(free.iconSlug)
        assertNull(free.description)
    }

    @Test
    fun hourlyRateWinsOverPriceAndCurrencyIsTrimmed() {
        val json =
            """
            {"status":"success","data":{"topic":
              {"id":"t3","title":"X","duration_mins":45,
               "price":{"amount":1000,"currency":"CAD","is_free":false},
               "hourly_rate":{"amount":3000,"currency":" usd "}}}}
            """.trimIndent()
        val topic = envelope(MyExpertTopicPayload::class.java).fromJson(json)!!.data.topic
        assertEquals(3000L, topic.amountMinor)
        assertEquals("usd", topic.storedCurrency)
    }

    @Test
    fun availabilityDecodesTimezoneAndSparseDays() {
        val json =
            """
            {"status":"success","data":{
              "timezone":{"id":"UTC","display_name":"(GMT+00:00) UTC","utc_offset":0},
              "recurring_schedule":{"monday":[{"start_time":"09:00","end_time":"17:00"}],"tuesday":[]}
            }}
            """.trimIndent()
        val data = envelope(ExpertAvailabilityScheduleData::class.java).fromJson(json)!!.data
        assertEquals("UTC", data.timezone?.id)
        assertEquals("(GMT+00:00) UTC", data.timezone?.displayName)
        val schedule = data.recurringSchedule!!
        assertEquals("09:00", schedule.monday!![0].startTime)
        assertEquals("17:00", schedule.monday!![0].endTime)
        assertTrue(schedule.tuesday!!.isEmpty())
        assertNull(schedule.wednesday)
    }
}
