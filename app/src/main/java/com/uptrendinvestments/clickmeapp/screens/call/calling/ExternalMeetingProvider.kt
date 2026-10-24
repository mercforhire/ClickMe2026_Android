package com.uptrendinvestments.clickmeapp.screens.call.calling

import java.net.URI
import java.util.Locale

/**
 * Which external meeting service a `skype_zoom` URL is for. The backend collapses every non-Agora meeting
 * under one bucket, so the client sniffs the URL to pick the right verb and icon. Port of iOS
 * `ExternalMeetingProvider.detect`; anything unrecognised is [Unknown] so the UI shows generic "Open
 * Meeting" copy and never mislabels a link (D-05, T-09-09).
 *
 * Unlike iOS, hosts match on the provider's domain or a subdomain of it, never a substring, and there is no
 * raw-substring fallback: an expert-supplied link such as `zoom.us.attacker.example` or
 * `evil.example/?q=skype` must not get a trusted provider label (WR-08, iOS follow-up).
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
        private val SchemePattern = Regex("[a-z][a-z0-9+.-]*")

        /**
         * Case-insensitive. Native app scheme first, then the host's domain. Scheme and host come from [URI]
         * so this stays a plain JVM unit; a string that [URI] cannot parse (a space in the path, say) is read
         * leniently, the way `Uri.parse` would.
         */
        fun detect(url: String?): ExternalMeetingProvider {
            val trimmed = url?.trim().orEmpty()
            if (trimmed.isEmpty()) return Unknown
            val parsed = runCatching { URI(trimmed) }.getOrNull()
            val scheme = (parsed?.scheme ?: lenientScheme(trimmed)).lowercase(Locale.ROOT)
            val host =
                (parsed?.host ?: lenientHost(trimmed))
                    .lowercase(Locale.ROOT)
                    .removeSuffix(".")

            return fromScheme(scheme) ?: fromHost(host) ?: Unknown
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
                host.isOnDomain("skype.com") -> Skype
                host.isOnDomain("zoom.us") || host.isOnDomain("zoom.com") -> Zoom
                host.isOnDomain("meet.google.com") -> GoogleMeet
                host.isOnDomain("teams.microsoft.com") || host.isOnDomain("teams.live.com") -> MicrosoftTeams
                else -> null
            }

        /** True for [domain] itself or any subdomain of it; `notzoom.us` and `zoom.us.evil.example` are not. */
        private fun String.isOnDomain(domain: String): Boolean = this == domain || endsWith(".$domain")

        private fun lenientScheme(url: String): String {
            val scheme = url.substringBefore(':', missingDelimiterValue = "")
            return if (SchemePattern.matches(scheme.lowercase(Locale.ROOT))) scheme else ""
        }

        /** Host of a `scheme://[user@]host[:port]/...` string without user info or port; empty when there is none. */
        private fun lenientHost(url: String): String {
            val afterScheme = url.substringAfter("://", missingDelimiterValue = "")
            val authority = afterScheme.takeWhile { it != '/' && it != '?' && it != '#' }
            return authority.substringAfterLast('@').substringBefore(':')
        }
    }
}
