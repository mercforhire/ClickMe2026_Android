package com.uptrendinvestments.clickmeapp.components.web

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMePalette

/**
 * Only web links may leave the app through the Custom Tab launcher. javascript:, file:, content:,
 * intent: and mailto: are rejected (mailto is handled by later phases via ACTION_SENDTO).
 */
internal fun isAllowedCustomTabUri(uri: Uri): Boolean {
    val scheme = uri.scheme?.lowercase()
    return (scheme == "https" || scheme == "http") && !uri.host.isNullOrBlank()
}

/**
 * Opens [url] in a dark Custom Tab (Android replacement for iOS `SafariView`). Falls back to a
 * plain ACTION_VIEW when no Custom Tabs provider exists and calls [onFailure] when the link is
 * not allowed or nothing can open it.
 */
fun Context.openCustomTab(
    url: Uri,
    onFailure: () -> Unit,
) {
    if (!isAllowedCustomTabUri(url)) {
        onFailure()
        return
    }
    val isActivity = findActivity() != null
    val barColor = ClickMePalette.surface.toArgb()
    val customTabsIntent =
        CustomTabsIntent
            .Builder()
            .setColorScheme(CustomTabsIntent.COLOR_SCHEME_DARK)
            .setDefaultColorSchemeParams(
                CustomTabColorSchemeParams
                    .Builder()
                    .setToolbarColor(barColor)
                    .setNavigationBarColor(barColor)
                    .build(),
            ).setShowTitle(true)
            .setUrlBarHidingEnabled(true)
            .build()
    if (!isActivity) customTabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    try {
        customTabsIntent.launchUrl(this, url)
    } catch (_: ActivityNotFoundException) {
        val viewIntent = Intent(Intent.ACTION_VIEW, url).addCategory(Intent.CATEGORY_BROWSABLE)
        if (!isActivity) viewIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            startActivity(viewIntent)
        } catch (_: ActivityNotFoundException) {
            onFailure()
        }
    }
}

/** Remembers a launcher for [openCustomTab]; callers show a Snackbar from [onFailure]. */
@Composable
fun rememberCustomTabLauncher(onFailure: () -> Unit): (Uri) -> Unit {
    val context = LocalContext.current
    val currentOnFailure by rememberUpdatedState(onFailure)
    return remember(context) {
        { uri -> context.openCustomTab(uri) { currentOnFailure() } }
    }
}

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
