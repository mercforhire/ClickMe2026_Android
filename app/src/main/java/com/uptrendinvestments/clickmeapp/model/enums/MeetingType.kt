package com.uptrendinvestments.clickmeapp.model.enums

/**
 * How a booked session is held. Wire values match the backend `MEETING_TYPES`; display labels are string
 * resources (`make_a_booking_meeting_*`), not enum properties. Declaration order is the picker order.
 * Mirrors iOS `Model/Enums/MeetingType.swift`.
 */
enum class MeetingType(
    val wire: String,
) {
    InAppVoice("in_app_voice"),
    SkypeZoom("skype_zoom"),
    ;

    companion object {
        // Plain map lookup, same lenient style as ErrorCode.fromWire: unknown values map to null.
        private val byWire: Map<String, MeetingType> = entries.associateBy { it.wire }

        /** Returns the matching case, or null for an unknown or missing wire value. */
        fun fromWire(raw: String?): MeetingType? = raw?.let(byWire::get)
    }
}
