package com.uptrendinvestments.clickmeapp.screens.push

/**
 * Who the backend sends a push to (RESEARCH F-6). It drives the mode decision when a tap lands (D-05): [Either] means
 * the resolver probes the booking to find out which side the user is on.
 */
enum class PushAudience {
    Client,
    Expert,
    Either,
}

/** Where a tapped push should land. Port of the iOS `PushDestination`. */
sealed interface PushDestination {
    /** Booking detail; the booking-summary screen of the mode that matches [audience]. */
    data class BookingDetail(
        val bookingId: String,
        val audience: PushAudience,
    ) : PushDestination

    /** The expert's incoming requests hub; [requestId] is the booking id the push carried (D-08). */
    data class IncomingRequests(
        val requestId: String?,
    ) : PushDestination

    /** A chat thread. */
    data class ChatThread(
        val threadId: String,
    ) : PushDestination

    /** The leave-review flow, opened through the booking summary. */
    data class LeaveReview(
        val bookingId: String,
        val audience: PushAudience,
    ) : PushDestination

    /** The payouts / earnings screen. */
    data object Payouts : PushDestination

    /** The Stripe Connect KYC flow. */
    data object KycOnboarding : PushDestination

    /** Security settings. */
    data object AccountSecurity : PushDestination

    /** The in-app notifications list, the fallback for unrouted pushes. */
    data object NotificationsList : PushDestination
}

/**
 * Translates a [PushEvent] into a [PushDestination]. Port of the iOS `PushRouter.destination(for:)`.
 *
 * The audience of each booking and review destination comes from who the backend actually notifies (RESEARCH F-6).
 */
object PushRouter {
    /** The destination for [event], or null when the category is unknown or a required id is missing. */
    fun destination(event: PushEvent): PushDestination? =
        when (event.category) {
            "booking" -> {
                if (event.subCategory == "requests") {
                    PushDestination.IncomingRequests(event.bookingId)
                } else {
                    event.bookingId?.let { PushDestination.BookingDetail(it, bookingAudience(event.subCategory)) }
                }
            }

            else -> {
                null
            }
        }

    /** [destination], or [PushDestination.NotificationsList] when there is no event or no route (D-10). */
    fun destinationOrList(event: PushEvent?): PushDestination =
        event?.let(::destination) ?: PushDestination.NotificationsList

    private fun bookingAudience(subCategory: String?): PushAudience =
        when (subCategory) {
            "confirmations", "declines", "expirations", "refunds" -> PushAudience.Client
            else -> PushAudience.Either
        }
}
