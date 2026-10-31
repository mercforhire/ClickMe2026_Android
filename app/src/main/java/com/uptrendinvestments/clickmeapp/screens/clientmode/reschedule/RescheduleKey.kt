package com.uptrendinvestments.clickmeapp.screens.clientmode.reschedule

import androidx.navigation3.runtime.NavKey
import com.uptrendinvestments.clickmeapp.navigation.StackOrigin
import kotlinx.serialization.Serializable

/**
 * Nav3 route to the client Reschedule picker (Phase 16.2). It carries only the booking id: the screen always loads its
 * own data. [opener] names the origin screen's one-shot slot that receives the success notice (D-09). [origin] only
 * keeps content keys distinct across stacks (see [StackOrigin]). Nothing here is a secret.
 */
@Serializable
data class RescheduleKey(
    val bookingId: String,
    val opener: RescheduleOpener,
    val origin: StackOrigin = StackOrigin.Default,
) : NavKey
