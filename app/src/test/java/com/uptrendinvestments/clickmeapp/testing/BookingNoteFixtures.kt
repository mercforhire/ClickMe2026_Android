package com.uptrendinvestments.clickmeapp.testing

import com.uptrendinvestments.clickmeapp.model.responses.BookingNoteData
import com.uptrendinvestments.clickmeapp.model.responses.ClientLocationDto
import com.uptrendinvestments.clickmeapp.model.responses.ClientPersonalDetailsDto
import com.uptrendinvestments.clickmeapp.model.responses.ClientRelationshipDto
import com.uptrendinvestments.clickmeapp.model.responses.ExpertClientProfileData
import java.time.Instant

/** A private booking note as the server returns it; pass `note = null` for "no note yet". */
fun bookingNoteData(
    bookingId: String = "b1",
    note: String? = "Call back",
): BookingNoteData = BookingNoteData(bookingId = bookingId, note = note)

/** The expert's view of a client, with every field filled by default. */
@Suppress("LongParameterList")
fun expertClientProfile(
    id: String = "c1",
    fullName: String? = "Sophia Carter",
    bio: String? = "Loves learning.",
    city: String? = "Toronto",
    stateProvince: String? = "ON",
    country: String? = "CAN",
    timezone: String? = "(GMT-05:00) Eastern Time",
    languages: List<String> = listOf("English", "Spanish"),
    memberSince: Instant? = Instant.parse("2025-03-10T12:00:00Z"),
    totalSessions: Int = 3,
    firstSessionDate: Instant? = Instant.parse("2026-02-01T15:00:00Z"),
    lastSessionDate: Instant? = Instant.parse("2026-10-01T15:00:00Z"),
): ExpertClientProfileData =
    ExpertClientProfileData(
        id = id,
        personalDetails = ClientPersonalDetailsDto(fullName = fullName, bio = bio),
        location =
            ClientLocationDto(
                city = city,
                stateProvince = stateProvince,
                country = country,
                timezone = timezone,
            ),
        languages = languages,
        memberSince = memberSince,
        relationship =
            ClientRelationshipDto(
                totalSessions = totalSessions,
                firstSessionDate = firstSessionDate,
                lastSessionDate = lastSessionDate,
            ),
    )
