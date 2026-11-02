package com.uptrendinvestments.clickmeapp.screens.expertmode.clientprofile

import androidx.navigation3.runtime.NavKey
import com.uptrendinvestments.clickmeapp.screens.expertmode.bookingsummary.ExpertBookingOrigin
import com.uptrendinvestments.clickmeapp.screens.expertmode.bookingsummary.ExpertBookingSummaryKey
import kotlinx.serialization.Serializable

/**
 * Nav3 route to the read-only Client Profile an expert opens from a booking (iOS `HomeRoute.peerProfile` in expert
 * mode; the iOS name and avatar seeds are unused there, so the key carries only the id).
 *
 * [origin] keeps the route distinct across the Dashboard, Bookings and Chats stacks, because
 * `NavigationState.toDecoratedEntries` flattens the stacks in use and two equal keys would collide (D-13).
 */
@Serializable
data class ExpertClientProfileKey(
    val clientId: String,
    val origin: ExpertBookingOrigin,
) : NavKey

/** The Client Profile key for a booking summary; it copies the summary's origin so the profile lands on its stack. */
fun ExpertBookingSummaryKey.clientProfileKey(clientId: String): ExpertClientProfileKey =
    ExpertClientProfileKey(clientId, origin)
