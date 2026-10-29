package com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings

import androidx.navigation3.runtime.NavKey
import com.uptrendinvestments.clickmeapp.navigation.SettingsOrigin
import kotlinx.serialization.Serializable

/**
 * Nav3 route to the shared photo crop screen, pushed from Profile Settings (client or expert). The expert screen pushes
 * it with its own [origin] so the Dashboard and Profile stacks never hold equal keys (see [SettingsOrigin]).
 */
@Serializable
data class ProfilePhotoCropKey(
    val origin: SettingsOrigin,
) : NavKey
