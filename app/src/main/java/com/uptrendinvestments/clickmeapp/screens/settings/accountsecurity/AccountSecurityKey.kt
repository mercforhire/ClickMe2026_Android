package com.uptrendinvestments.clickmeapp.screens.settings.accountsecurity

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Nav3 route to Account Security (change password and the Delete Account entry). Reached from the
 * "Security & Password" row of both profile hubs; registered by the shell wiring plan (14-14).
 */
@Serializable
data object AccountSecurityKey : NavKey
