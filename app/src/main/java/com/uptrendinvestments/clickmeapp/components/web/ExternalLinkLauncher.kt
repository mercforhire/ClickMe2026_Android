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

private val MeetingAppSchemes = setOf("skype", "zoommtg", "zoomus", "msteams")

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
 * [isAllowedMeetingUri] first; this never uses `Intent.parseUri` and never launches a server-built Intent.
 * The try/catch avoids needing `<queries>` package-visibility entries.
 */
fun Context.openExternalMeeting(
    uri: Uri,
    onFailure: () -> Unit,
    customTab: (Uri, () -> Unit) -> Unit = { u, f -> openCustomTab(u, f) },
) {
    if (!isAllowedMeetingUri(uri)) {
        onFailure()
        return
    }
    try {
        startActivity(buildExternalMeetingIntent(uri, fromActivity = findActivity() != null))
    } catch (_: ActivityNotFoundException) {
        if (isAllowedCustomTabUri(uri)) customTab(uri, onFailure) else onFailure()
    }
}

/** Remembers a launcher for [openExternalMeeting]; callers show a Snackbar from [onFailure]. */
@Composable
fun rememberExternalMeetingLauncher(onFailure: () -> Unit): (Uri) -> Unit {
    val context = LocalContext.current
    val currentOnFailure by rememberUpdatedState(onFailure)
    return remember(context) {
        { uri -> context.openExternalMeeting(uri, { currentOnFailure() }) }
    }
}
