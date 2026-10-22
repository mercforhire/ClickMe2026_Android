package com.uptrendinvestments.clickmeapp.screens.clientmode.requestsent

import com.uptrendinvestments.clickmeapp.model.responses.BookingDetailExpertDto
import com.uptrendinvestments.clickmeapp.model.responses.BookingDetailTopicDto
import com.uptrendinvestments.clickmeapp.model.responses.ClientBookingDetail
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class BookingRequestSentViewModelTest {
    private val start = Instant.parse("2026-10-24T18:00:00Z")

    private fun detail(
        endTime: Instant? = Instant.parse("2026-10-24T19:00:00Z"),
        title: String = "Intro",
        durationMins: Int? = null,
        isFree: Boolean = true,
        fullName: String? = "Ada Lovelace",
        avatarUrl: String? = "https://x/a.jpg",
    ) = ClientBookingDetail(
        bookingId = "b1",
        status = "pending",
        startTime = start,
        endTime = endTime,
        topic = BookingDetailTopicDto(title = title, durationMins = durationMins, isFree = isFree),
        expert = BookingDetailExpertDto(fullName = fullName, avatarUrl = avatarUrl),
    )

    @Test
    fun mapsAllFieldsFromTheBooking() {
        assertEquals(
            BookingRequestSentDetails(
                expertName = "Ada Lovelace",
                expertAvatarUrl = "https://x/a.jpg",
                topicTitle = "Intro",
                start = start,
                end = Instant.parse("2026-10-24T19:00:00Z"),
                isFree = true,
            ),
            detail().toRequestSentDetails(),
        )
    }

    @Test
    fun missingEndTimeFallsBackToStartPlusTopicDuration() {
        val mapped = detail(endTime = null, durationMins = 60).toRequestSentDetails()
        assertEquals(Instant.parse("2026-10-24T19:00:00Z"), mapped.end)
    }

    @Test
    fun missingEndTimeAndDurationFallsBackToThirtyMinutes() {
        val mapped = detail(endTime = null, durationMins = null).toRequestSentDetails()
        assertEquals(Instant.parse("2026-10-24T18:30:00Z"), mapped.end)
    }

    @Test
    fun blankOrNullExpertNameMapsToNull() {
        assertEquals(null, detail(fullName = "   ").toRequestSentDetails().expertName)
        assertEquals(null, detail(fullName = null).toRequestSentDetails().expertName)
    }

    @Test
    fun paidTopicIsNotFree() {
        assertEquals(false, detail(isFree = false).toRequestSentDetails().isFree)
    }
}
