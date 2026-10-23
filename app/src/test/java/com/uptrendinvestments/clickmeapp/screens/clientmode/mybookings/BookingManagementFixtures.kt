package com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings

import com.uptrendinvestments.clickmeapp.model.responses.BookingDetailExpertDto
import com.uptrendinvestments.clickmeapp.model.responses.BookingDetailTopicDto
import com.uptrendinvestments.clickmeapp.model.responses.BookingExpertDto
import com.uptrendinvestments.clickmeapp.model.responses.ClientBookingDetail
import com.uptrendinvestments.clickmeapp.model.responses.ClientBookingItem
import com.uptrendinvestments.clickmeapp.model.responses.RescheduleDto
import com.uptrendinvestments.clickmeapp.model.responses.TopicPriceDto
import java.time.Instant

/** Shared Phase 8 booking fixtures for ViewModel, repository and screen tests. */
fun bookingItem(
    id: String = "b1",
    status: String = "confirmed",
    start: Instant = Instant.parse("2026-10-24T18:00:00Z"),
    end: Instant = start.plusSeconds(3600),
    expertName: String? = "Ada Lovelace",
    topic: String? = "Algorithms",
    meetingType: String? = "in_app_voice",
    reschedule: RescheduleDto? = null,
): ClientBookingItem =
    ClientBookingItem(
        bookingId = id,
        expert = BookingExpertDto(id = "e1", fullName = expertName),
        topic = topic,
        startTime = start,
        endTime = end,
        meetingType = meetingType,
        status = status,
        reschedule = reschedule,
    )

fun rescheduleDto(
    proposedStart: Instant? = Instant.parse("2026-10-30T18:00:00Z"),
    proposedEnd: Instant? = proposedStart?.plusSeconds(3600),
    initiatedBy: String? = "expert",
    note: String? = null,
): RescheduleDto =
    RescheduleDto(
        proposedStart = proposedStart,
        proposedEnd = proposedEnd,
        proposedTimezone = "America/Toronto",
        initiatedBy = initiatedBy,
        note = note,
        createdAt = Instant.parse("2026-10-23T12:00:00Z"),
    )

@Suppress("LongParameterList")
fun bookingDetail(
    id: String = "b1",
    status: String = "confirmed",
    start: Instant = Instant.parse("2026-10-24T18:00:00Z"),
    end: Instant? = start.plusSeconds(3600),
    isFree: Boolean = false,
    amount: Long? = 5000,
    currency: String? = "CAD",
    paymentStatus: String? = "held",
    expertId: String? = "e1",
    expertName: String? = "Ada Lovelace",
    expertTitle: String? = "Engineer",
    expertTimezone: String? = "America/Toronto",
    meetingType: String? = "in_app_voice",
    joinLink: String? = null,
    clientNotes: String? = null,
    authorizationExpiresAt: Instant? = null,
    reschedule: RescheduleDto? = null,
): ClientBookingDetail =
    ClientBookingDetail(
        bookingId = id,
        status = status,
        startTime = start,
        endTime = end,
        meetingType = meetingType,
        expertTimezone = expertTimezone,
        paymentStatus = paymentStatus,
        topic =
            BookingDetailTopicDto(
                id = "t1",
                title = "Algorithms",
                durationMins = 60,
                isFree = isFree,
                price = if (isFree) TopicPriceDto(null, null, true) else TopicPriceDto(amount, currency),
            ),
        expert = BookingDetailExpertDto(id = expertId, fullName = expertName, title = expertTitle),
        clientNotes = clientNotes,
        authorizationExpiresAt = authorizationExpiresAt,
        joinLink = joinLink,
        reschedule = reschedule,
    )
