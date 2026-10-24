package com.uptrendinvestments.clickmeapp.components.web

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

private val MeetingAppSchemes = setOf("skype", "zoommtg", "zoomus", "msteams")

private val SchemePattern = Regex("[a-z][a-z0-9+.-]*")

/**
 * Only http(s) links with a host and the meeting-app schemes skype, zoommtg, zoomus and msteams may be
 * launched. intent:, javascript:, file:, content: and android-app: are rejected (T-09-07, T-09-08).
 */
internal fun isAllowedMeetingUri(uri: Uri): Boolean {
    val scheme = uri.scheme?.lowercase() ?: return false
    return when (scheme) {
        "http", "https" -> !uri.host.isNullOrBlank()
        else -> scheme in MeetingAppSchemes
    }
}

/**
 * String twin of [isAllowedMeetingUri] for code without an android [Uri]: `CallCenter` checks the server's
 * `join_url` before it enables Open (WR-07). Same rules, read leniently like [Uri.parse]: http(s) with a
 * non-blank host, or one of the meeting-app schemes.
 */
fun isAllowedMeetingUrl(url: String): Boolean {
    val colon = url.indexOf(':')
    if (colon <= 0) return false
    val scheme = url.substring(0, colon).lowercase(Locale.ROOT)
    if (!SchemePattern.matches(scheme)) return false
    return when (scheme) {
        "http", "https" -> webHost(url.substring(colon + 1)).isNotBlank()
        else -> scheme in MeetingAppSchemes
    }
}

/** Host of a hierarchical `//authority/...` part, without user info or port; empty when there is none. */
private fun webHost(schemeSpecific: String): String {
    if (!schemeSpecific.startsWith("//")) return ""
    val authority = schemeSpecific.substring(2).takeWhile { it != '/' && it != '?' && it != '#' }
    val hostAndPort = authority.substringAfterLast('@')
    return if (hostAndPort.startsWith("[")) {
        hostAndPort.substringBefore(']').removePrefix("[")
    } else {
        hostAndPort.substringBefore(':')
    }
}

/**
 * Builds the implicit ACTION_VIEW for a meeting link. `FLAG_ACTIVITY_REQUIRE_NON_BROWSER` (API 30+) makes
 * an https link resolve only to an installed non-browser app, otherwise `startActivity` throws and the
 * caller falls back. Below API 30 the flag does not exist and the link opens in the default browser
 * (accepted). `FLAG_ACTIVITY_NEW_TASK` is added only when the context is not an Activity.
 */
internal fun buildExternalMeetingIntent(
    uri: Uri,
    fromActivity: Boolean,
    sdkInt: Int = Build.VERSION.SDK_INT,
): Intent =
    Intent(Intent.ACTION_VIEW, uri).apply {
        addCategory(Intent.CATEGORY_BROWSABLE)
        if (!fromActivity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (sdkInt >= Build.VERSION_CODES.R) addFlags(Intent.FLAG_ACTIVITY_REQUIRE_NON_BROWSER)
    }

/**
 * Opens an expert-supplied meeting link app-first (D-06): an installed Zoom/Skype/Teams app claims it,
 * otherwise http(s) falls back to [customTab] (the Custom Tab launcher) and any other scheme calls
 * [onFailure]. The URL is expert-controlled server data (T-09-07), so it is checked against
 * [isAllowedMeetingUri] first; this never parses an Intent from a URI string and never launches a server-built Intent.
 * The try/catch avoids needing `<queries>` package-visibility entries.
 *
 * Returns true when something opened the link, false when [onFailure] ran (WR-07).
 */
fun Context.openExternalMeeting(
    uri: Uri,
    onFailure: () -> Unit,
    customTab: (Uri, () -> Unit) -> Unit = { u, f -> openCustomTab(u, f) },
): Boolean {
    var failed = false
    val fail = {
        failed = true
        onFailure()
    }
    if (!isAllowedMeetingUri(uri)) {
        fail()
        return false
    }
    try {
        startActivity(buildExternalMeetingIntent(uri, fromActivity = findActivity() != null))
    } catch (_: ActivityNotFoundException) {
        if (isAllowedCustomTabUri(uri)) customTab(uri, fail) else fail()
    }
    return !failed
}

/**
 * Remembers a launcher for [openExternalMeeting]. It returns true when the link opened; callers give feedback
 * from [onFailure].
 */
@Composable
fun rememberExternalMeetingLauncher(onFailure: () -> Unit): (Uri) -> Boolean {
    val context = LocalContext.current
    val currentOnFailure by rememberUpdatedState(onFailure)
    return remember(context) {
        { uri -> context.openExternalMeeting(uri, { currentOnFailure() }) }
    }
}
