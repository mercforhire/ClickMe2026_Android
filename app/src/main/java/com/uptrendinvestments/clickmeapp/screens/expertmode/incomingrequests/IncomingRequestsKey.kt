package com.uptrendinvestments.clickmeapp.screens.expertmode.incomingrequests

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Nav3 route to Incoming Requests. Pushed on the Dashboard stack from the pending banner or the Pending Requests
 * tool (D-11); it carries no data because the screen loads the list itself.
 */
@Serializable
data object IncomingRequestsKey : NavKey
