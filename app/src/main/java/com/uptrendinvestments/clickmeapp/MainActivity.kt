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
import com.uptrendinvestments.clickmeapp.services.call.CallNotificationFactory
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Single activity: splash hold, edge-to-edge, and the [ClickMeApp] route switch. `singleTop`, so tapping the
 * ongoing-call notification arrives in [onNewIntent] while running and in [onCreate] after a cold start.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val appViewModel: AppViewModel by viewModels()

    @Inject
    lateinit var callCenter: CallCenter

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        // Hold the OS splash only until the root route resolves; no fixed delay.
        splash.setKeepOnScreenCondition { !appViewModel.destinationResolved.value }
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
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Warm start from the notification (D-10). `expand()` is a no-op without a live call (D-15).
        if (intent.consumeExpandCallExtra()) callCenter.expand()
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
