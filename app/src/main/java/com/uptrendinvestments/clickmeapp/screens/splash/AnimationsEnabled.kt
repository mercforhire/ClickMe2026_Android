package com.uptrendinvestments.clickmeapp.screens.splash

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Whether the system allows animations. No helper existed: the Android "Remove animations" setting (and the developer
 * option "Animator duration scale: off") zeroes `ANIMATOR_DURATION_SCALE`, and Compose animations would otherwise
 * ignore it. A missing setting counts as enabled. Shared so a later screen can follow the same switch.
 */
@Composable
fun rememberAnimationsEnabled(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember(resolver) {
        Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f
    }
}
