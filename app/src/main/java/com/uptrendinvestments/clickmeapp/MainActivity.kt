package com.uptrendinvestments.clickmeapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.uptrendinvestments.clickmeapp.managers.CallCenter
import com.uptrendinvestments.clickmeapp.screens.push.PushDestination
import com.uptrendinvestments.clickmeapp.screens.push.PushEvent
import com.uptrendinvestments.clickmeapp.screens.push.PushLaunchRequest
import com.uptrendinvestments.clickmeapp.screens.push.PushRouter
import com.uptrendinvestments.clickmeapp.services.call.CallNotificationFactory
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Single activity: OS splash hand-off to the Compose splash, edge-to-edge, and the [ClickMeApp] route switch. `singleTop`, so tapping the
 * ongoing-call notification or a push notification arrives in [onNewIntent] while running and in [onCreate] after a
 * cold start.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val appViewModel: AppViewModel by viewModels()

    @Inject
    lateinit var callCenter: CallCenter

    @Inject
    lateinit var pushLaunchRequest: PushLaunchRequest

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        // Flips at composition, not at first draw: see AppViewModel.keepSystemSplash.
        splash.setKeepOnScreenCondition { appViewModel.keepSystemSplash() }
        // No exit animation, so the API 31+ icon fade cannot stack on the Compose splash entrance (UI-SPEC).
        splash.setOnExitAnimationListener { it.remove() }
        // Rotation keeps the ViewModel and its running hold, so this is a no-op there; a process restore has a new
        // ViewModel and is treated as already served.
        if (savedInstanceState != null) appViewModel.onActivityRestored()
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        setContent {
            ClickMeTheme {
                ClickMeApp(appViewModel)
            }
        }
        // Each return to the foreground retries /me if the launch took the cached path (D-01).
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) { appViewModel.onAppForegrounded() }
        }
        // Cold start from the notification (process alive after a task swipe, D-14).
        if (intent.consumeExpandCallExtra()) callCenter.expand()
        // Cold start from a push tap, system-displayed or ours. Never on restore or a recents launch (Pitfall 1).
        if (shouldReadLaunchPush(savedInstanceState, intent)) {
            intent.consumePushDestination()?.let(pushLaunchRequest.destination::post)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Warm start from the notification (D-10). `expand()` is a no-op without a live call (D-15).
        if (intent.consumeExpandCallExtra()) callCenter.expand()
        // Warm start from a push tap (D-15): the newest tap wins in the pending slot.
        intent.consumePushDestination()?.let(pushLaunchRequest.destination::post)
    }
}

/**
 * True when this is the ongoing-call notification's tap intent. The extra is removed so a recreation of the
 * activity does not expand the call again. Nothing else is read from the intent (T-09-43).
 */
internal fun Intent?.consumeExpandCallExtra(): Boolean {
    if (this == null || !getBooleanExtra(CallNotificationFactory.EXTRA_EXPAND_CALL, false)) return false
    removeExtra(CallNotificationFactory.EXTRA_EXPAND_CALL)
    return true
}

/**
 * The destination a push-tap intent asks for, or null when this is not a push intent (a plain launcher open, the
 * ongoing-call tap). The push keys are removed so a recreation of the activity does not route again.
 *
 * The activity is exported, so any app can start it with arbitrary extras: only String values of
 * [PushEvent.PUSH_KEYS] are read (a non-String reads as null) and [PushEvent] validates every id (ASVS V5).
 */
internal fun Intent?.consumePushDestination(): PushDestination? {
    if (this == null) return null
    val data = PushEvent.PUSH_KEYS.associateWith { key -> extras?.getString(key) }
    val event = PushEvent.from(data) ?: return null
    PushEvent.PUSH_KEYS.forEach { removeExtra(it) }
    return PushRouter.destinationOrList(event)
}

/**
 * Whether `onCreate` should read the launch intent for a push. Rotation, process restore (non-null
 * [savedInstanceState]) and a launch from recents (FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) replay the original intent,
 * which would route the same tap again (Pitfall 1).
 */
internal fun shouldReadLaunchPush(
    savedInstanceState: Bundle?,
    intent: Intent?,
): Boolean =
    savedInstanceState == null &&
        intent != null &&
        intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY == 0
