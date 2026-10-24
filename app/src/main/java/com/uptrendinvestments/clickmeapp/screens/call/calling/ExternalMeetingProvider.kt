package com.uptrendinvestments.clickmeapp.screens.call.calling

import java.net.URI
import java.util.Locale

/**
 * Which external meeting service a `skype_zoom` URL is for. The backend collapses every non-Agora meeting
 * under one bucket, so the client sniffs the URL to pick the right verb and icon. Port of iOS
 * `ExternalMeetingProvider.detect`; anything unrecognised is [Unknown] so the UI shows generic "Open
 * Meeting" copy and never mislabels a link (D-05, T-09-09).
 *
 * Display labels live in string resources (`call_provider_*`) and are mapped in the UI, not here.
 */
enum class ExternalMeetingProvider {
    Skype,
    Zoom,
    GoogleMeet,
    MicrosoftTeams,
    Unknown,
    ;

    val isKnown: Boolean get() = this != Unknown

    companion object {
        /**
         * Case-insensitive. Order matters and mirrors iOS: native app scheme, then host, then a raw
         * substring fallback. Scheme and host come from [URI] so this stays a plain JVM unit; a string that
         * [URI] cannot parse leaves both empty and falls through to the substring rules.
         */
        fun detect(url: String?): ExternalMeetingProvider {
            val trimmed = url?.trim().orEmpty()
            if (trimmed.isEmpty()) return Unknown
            val parsed = runCatching { URI(trimmed) }.getOrNull()
            val scheme = parsed?.scheme?.lowercase(Locale.ROOT).orEmpty()
            val host = parsed?.host?.lowercase(Locale.ROOT).orEmpty()
            val raw = trimmed.lowercase(Locale.ROOT)

            return fromScheme(scheme) ?: fromHost(host) ?: fromRaw(raw) ?: Unknown
        }

        private fun fromScheme(scheme: String): ExternalMeetingProvider? =
            when (scheme) {
                "skype" -> Skype
                "zoommtg", "zoomus" -> Zoom
                "msteams" -> MicrosoftTeams
                else -> null
            }

        private fun fromHost(host: String): ExternalMeetingProvider? =
            when {
                host.contains("skype.com") -> Skype
                host.contains("zoom.us") || host.contains("zoom.com") -> Zoom
                host.contains("meet.google.com") -> GoogleMeet
                host.contains("teams.microsoft.com") || host.contains("teams.live.com") -> MicrosoftTeams
                else -> null
            }

        private fun fromRaw(raw: String): ExternalMeetingProvider? =
            when {
                raw.contains("skype") -> Skype
                raw.contains("zoom") -> Zoom
                raw.contains("meet.google") -> GoogleMeet
                raw.contains("teams.") -> MicrosoftTeams
                else -> null
            }
    }
}
