package com.uptrendinvestments.clickmeapp.model.enums

/**
 * Call connection discriminator. Tells the join response whether the in-app voice config or the external
 * (Skype / Zoom) config is populated. Wire values match the backend. Mirrors iOS
 * `Model/Enums/ConnectionType.swift`.
 */
enum class ConnectionType(
    val wire: String,
) {
    InAppVoice("in_app_voice"),
    SkypeZoom("skype_zoom"),
    ;

    companion object {
        private val byWire: Map<String, ConnectionType> = entries.associateBy { it.wire }

        /** Returns the matching case, or null for an unknown or missing wire value. */
        fun fromWire(raw: String?): ConnectionType? = raw?.let(byWire::get)
    }
}
