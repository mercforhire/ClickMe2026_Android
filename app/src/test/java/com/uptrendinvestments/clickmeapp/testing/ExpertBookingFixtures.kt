package com.uptrendinvestments.clickmeapp.testing

import com.uptrendinvestments.clickmeapp.model.responses.BookingRequestClientDto
import com.uptrendinvestments.clickmeapp.model.responses.BookingRequestDetail
import com.uptrendinvestments.clickmeapp.model.responses.BookingRequestDetailClientDto
import com.uptrendinvestments.clickmeapp.model.responses.BookingRequestEconomicsDto
import com.uptrendinvestments.clickmeapp.model.responses.BookingRequestItem
import com.uptrendinvestments.clickmeapp.model.responses.BookingRequestSessionDto
import com.uptrendinvestments.clickmeapp.model.responses.ConsultationFeeDto
import com.uptrendinvestments.clickmeapp.model.responses.EarningsBreakdownDto
import com.uptrendinvestments.clickmeapp.model.responses.ExpertBookingActionsDto
import com.uptrendinvestments.clickmeapp.model.responses.ExpertBookingClientDto
import com.uptrendinvestments.clickmeapp.model.responses.ExpertBookingDetail
import com.uptrendinvestments.clickmeapp.model.responses.ExpertBookingDetailSessionDto
import com.uptrendinvestments.clickmeapp.model.responses.ExpertBookingItem
import com.uptrendinvestments.clickmeapp.model.responses.ExpertBookingSessionDto
import com.uptrendinvestments.clickmeapp.model.responses.ExpertStatusMeta
import com.uptrendinvestments.clickmeapp.model.responses.FEE_SOURCE_BOOKING
import com.uptrendinvestments.clickmeapp.model.responses.PreparationNotesDto
import com.uptrendinvestments.clickmeapp.model.responses.RescheduleDto
import com.uptrendinvestments.clickmeapp.model.responses.ScheduleDto
import com.uptrendinvestments.clickmeapp.model.responses.TimeSlotDto
import java.time.Instant

/** Shared Phase 11 expert request and booking fixtures for repository, ViewModel and screen tests. */
private val DEFAULT_START: Instant = Instant.parse("2026-10-24T18:00:00Z")

/** A paid-session earnings breakdown (75.00 gross, 15% commission) unless overridden. */
fun breakdown(
    feeSource: String? = FEE_SOURCE_BOOKING,
    gross: Double? = 75.0,
    fee: Double? = 11.25,
    net: Double? = 63.75,
    pct: Double? = 15.0,
    currency: String? = "USD",
): EarningsBreakdownDto =
    EarningsBreakdownDto(
        grossAmount = gross,
        platformFee = fee,
        netAmount = net,
        commissionPct = pct,
        currency = currency,
        feeSource = feeSource,
    )

@Suppress("LongParameterList")
fun expertBookingItem(
    id: String = "b1",
    statusCode: String = "confirmed",
    start: Instant = DEFAULT_START,
    end: Instant = start.plusSeconds(3600),
    clientName: String? = "Ada Lovelace",
    topic: String? = "Algorithms",
    meetingType: String? = "in_app_voice",
    fee: Double? = 75.0,
    currency: String? = "USD",
    reschedule: RescheduleDto? = null,
    breakdown: EarningsBreakdownDto? = breakdown(),
    canReschedule: Boolean = false,
): ExpertBookingItem =
    ExpertBookingItem(
        bookingId = id,
        client = ExpertBookingClientDto(id = "c1", name = clientName),
        session =
            ExpertBookingSessionDto(
                topic = topic,
                startTime = start,
                endTime = end,
                meetingType = meetingType,
                consultationFee = fee?.let { ConsultationFeeDto(amount = it, currency = currency, label = null) },
                earningsBreakdown = breakdown,
            ),
        status = ExpertStatusMeta(label = null, code = statusCode),
        reschedule = reschedule,
        actions = ExpertBookingActionsDto(canReschedule = canReschedule),
    )

@Suppress("LongParameterList")
fun expertBookingDetail(
    id: String = "b1",
    statusCode: String = "confirmed",
    start: Instant = DEFAULT_START,
    end: Instant = start.plusSeconds(3600),
    fee: Double? = 75.0,
    currency: String? = "USD",
    notes: String? = null,
    reschedule: RescheduleDto? = null,
    canCancel: Boolean = true,
    clientName: String? = "Ada Lovelace",
    topic: String? = "Algorithms",
    meetingType: String? = "in_app_voice",
    breakdown: EarningsBreakdownDto? = breakdown(),
    canReschedule: Boolean = false,
): ExpertBookingDetail =
    ExpertBookingDetail(
        bookingId = id,
        status = ExpertStatusMeta(label = null, code = statusCode),
        reschedule = reschedule,
        client = ExpertBookingClientDto(id = "c1", name = clientName),
        session =
            ExpertBookingDetailSessionDto(
                topicTitle = topic,
                consultationFee = fee?.let { ConsultationFeeDto(amount = it, currency = currency, label = null) },
                earningsBreakdown = breakdown,
                schedule = ScheduleDto(startTime = start, endTime = end),
                meetingType = meetingType,
            ),
        preparationNotes = notes?.let { PreparationNotesDto(text = it) },
        actions = ExpertBookingActionsDto(canCancel = canCancel, canReschedule = canReschedule),
    )

@Suppress("LongParameterList")
fun bookingRequestItem(
    id: String = "r1",
    start: Instant = DEFAULT_START,
    expiresAt: Instant? = start.minusSeconds(86_400),
    status: String? = "pending_approval",
    earnings: Double? = 75.0,
    breakdown: EarningsBreakdownDto? = breakdown(),
    clientName: String? = "Ada Lovelace",
    topic: String? = "Algorithms",
    currency: String? = "USD",
): BookingRequestItem =
    BookingRequestItem(
        requestId = id,
        client = BookingRequestClientDto(id = "c1", fullName = clientName),
        topic = topic,
        earnings = earnings,
        currency = currency,
        earningsBreakdown = breakdown,
        timeSlot = TimeSlotDto(startTime = start, endTime = start.plusSeconds(3600)),
        expiresAt = expiresAt,
        status = status,
    )

@Suppress("LongParameterList")
fun bookingRequestDetail(
    id: String = "r1",
    start: Instant = DEFAULT_START,
    end: Instant = start.plusSeconds(3600),
    topic: String? = "Algorithms",
    meetingType: String? = "in_app_voice",
    message: String? = null,
    breakdown: EarningsBreakdownDto? = breakdown(),
    clientName: String? = "Ada Lovelace",
    currency: String? = "USD",
): BookingRequestDetail =
    BookingRequestDetail(
        requestId = id,
        client = BookingRequestDetailClientDto(id = "c1", name = clientName),
        session =
            BookingRequestSessionDto(
                startTime = start,
                endTime = end,
                topic = topic,
                meetingType = meetingType,
                clientMessage = message,
            ),
        economics = BookingRequestEconomicsDto(currency = currency, earningsBreakdown = breakdown),
    )
