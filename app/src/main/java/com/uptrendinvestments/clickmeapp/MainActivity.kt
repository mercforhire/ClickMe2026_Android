package com.uptrendinvestments.clickmeapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import dagger.hilt.android.AndroidEntryPoint

/** Single activity: splash hold, edge-to-edge, and the [ClickMeApp] route switch. */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val appViewModel: AppViewModel by viewModels()

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
    }
}
