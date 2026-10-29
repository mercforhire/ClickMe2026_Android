package com.uptrendinvestments.clickmeapp.screens.settings.deleteaccount

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Nav3 route to Delete Account (PROF-04). Registered by 14-14, which also links it from Account Security. The
 * flow's two forms (Warning, Reason) are steps inside one destination, not separate keys.
 */
@Serializable
data object DeleteAccountKey : NavKey
