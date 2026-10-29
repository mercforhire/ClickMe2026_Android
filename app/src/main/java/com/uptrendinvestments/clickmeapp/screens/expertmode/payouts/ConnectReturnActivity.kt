package com.uptrendinvestments.clickmeapp.screens.expertmode.payouts

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.uptrendinvestments.clickmeapp.MainActivity
import com.uptrendinvestments.clickmeapp.screens.expertmode.ConnectReturn
import com.uptrendinvestments.clickmeapp.screens.expertmode.DashboardLaunch
import com.uptrendinvestments.clickmeapp.screens.expertmode.ExpertLaunchRequest
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import javax.inject.Inject

internal const val CONNECT_SCHEME = "clickme"
internal const val CONNECT_HOST = "connect"
internal const val CONNECT_RETURN_PATH = "/return"
internal const val CONNECT_REFRESH_PATH = "/refresh"

/**
 * Maps a deep-link [uri] to what it asks for.
 *
 * | URI | Result |
 * |-----|--------|
 * | `clickme://connect/return` | [ConnectReturn.Done] |
 * | `clickme://connect/refresh` | [ConnectReturn.LinkExpired] |
 * | anything else | `null` |
 *
 * The activity is exported, so the URI is untrusted input: only the scheme, the host and the exact path are read;
 * the query and the fragment are never read. Completion is only ever decided by `GET expert/connect/status` (backend
 * doc STRIPE-INTEGRATION 3.1), so the result is a "go re-check" signal and never proof that onboarding finished.
 */
internal fun connectReturnFrom(uri: Uri?): ConnectReturn? {
    if (uri == null) return null
    val scheme = uri.scheme?.lowercase(Locale.ROOT)
    val host = uri.host?.lowercase(Locale.ROOT)
    if (scheme != CONNECT_SCHEME || host != CONNECT_HOST) return null
    return when (uri.path) {
        CONNECT_RETURN_PATH -> ConnectReturn.Done
        CONNECT_REFRESH_PATH -> ConnectReturn.LinkExpired
        else -> null
    }
}

/** Posts the Dashboard and Payouts reload hand-offs for a recognised Stripe return URI. */
class ConnectReturnReceiver
    @Inject
    constructor(
        private val launchRequest: ExpertLaunchRequest,
    ) {
        /** Returns true and posts both slots for a recognised URI; returns false and posts nothing otherwise. */
        fun receive(uri: Uri?): Boolean {
            val result = connectReturnFrom(uri) ?: return false
            launchRequest.dashboard.post(DashboardLaunch)
            launchRequest.connectReturn.post(result)
            return true
        }
    }

/** Brings the existing [MainActivity] back on top: no data, no extras. */
internal fun mainActivityRelaunchIntent(context: Context): Intent =
    Intent(context, MainActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)

/**
 * Trampoline for the Stripe Connect return (AppAuth redirect-receiver pattern). A separate activity avoids stacking a
 * second `MainActivity` over the Custom Tab; `CLEAR_TOP` closes the Custom Tab above `MainActivity` and delivers
 * `onNewIntent`.
 *
 * Any app can fire this URI, so it is only a "go re-check" signal: it carries no data into app state and never marks
 * anything complete. A blocked browser redirect is covered by the Payouts and Dashboard ON_RESUME reloads.
 */
@AndroidEntryPoint
class ConnectReturnActivity : ComponentActivity() {
    @Inject
    lateinit var receiver: ConnectReturnReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (receiver.receive(intent?.data)) startActivity(mainActivityRelaunchIntent(this))
        finish()
    }
}
