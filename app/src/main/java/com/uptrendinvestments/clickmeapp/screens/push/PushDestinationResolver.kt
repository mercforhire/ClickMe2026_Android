package com.uptrendinvestments.clickmeapp.screens.push

import androidx.navigation3.runtime.NavKey
import com.uptrendinvestments.clickmeapp.model.enums.AppMode
import com.uptrendinvestments.clickmeapp.screens.chat.chatting.ChatThreadKey
import com.uptrendinvestments.clickmeapp.screens.clientmode.bookingsummary.BookingSummaryKey
import com.uptrendinvestments.clickmeapp.screens.clientmode.home.subviews.ClientBookingsRoot
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.CANCELLABLE_BOOKING_STATUSES
import com.uptrendinvestments.clickmeapp.screens.clientmode.upcomingbooking.UpcomingBookingKey
import com.uptrendinvestments.clickmeapp.screens.notifications.notificationsKeyFor
import com.uptrendinvestments.clickmeapp.screens.notifications.notificationsRootFor
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

/** Upper bound on each lookup a tap triggers, so a slow network never leaves a tap pending for long (D-06). */
const val LOOKUP_TIMEOUT_MS = 5_000L

/**
 * Client booking statuses that open Upcoming Booking Details: the cancellable ones plus `in_progress` (D-06). Every
 * other status, including unknown future ones, opens Booking Summary.
 */
val PUSH_UPCOMING_STATUSES: Set<String> = CANCELLABLE_BOOKING_STATUSES + "in_progress"

/** What the resolver needs to know about the app right now. */
data class PushRoutingContext(
    val currentMode: AppMode,
    val hasExpertRole: Boolean,
    val callActive: Boolean,
)

/** A resolved route: a key chain for a shell, or a chat thread that Phase 12 opens on the Chats tab. */
sealed interface PushRoute {
    data class Shell(
        val route: ShellRoute,
    ) : PushRoute

    data class Chat(
        val key: ChatThreadKey,
    ) : PushRoute
}

/** The mode the shell must be in and the route to open there. */
data class ResolvedPush(
    val targetMode: AppMode,
    val route: PushRoute,
)

/**
 * Turns a tapped [PushDestination] into a concrete route: which mode, which tab, which key chain. Pure decision logic
 * behind two small lookup seams, so the root host only orchestrates (PUSH-04).
 */
class PushDestinationResolver
    @Inject
    constructor(
        private val bookingProbe: ClientBookingProbe,
        private val pendingRequests: PendingRequestLookup,
    ) {
        /** Resolves [destination] for the app state in [context]. */
        suspend fun resolve(
            destination: PushDestination,
            context: PushRoutingContext,
        ): ResolvedPush =
            when (destination) {
                is PushDestination.BookingDetail -> {
                    resolveBooking(destination.bookingId, destination.audience, isReview = false, context)
                }

                else -> {
                    notificationsList(context)
                }
            }

        private suspend fun resolveBooking(
            bookingId: String,
            @Suppress("UnusedParameter") audience: PushAudience,
            @Suppress("UnusedParameter") isReview: Boolean,
            context: PushRoutingContext,
        ): ResolvedPush {
            val result = probe(bookingId)
            return if (result is ClientBookingProbeResult.Client) {
                val key: NavKey =
                    if (result.status in PUSH_UPCOMING_STATUSES) {
                        UpcomingBookingKey(bookingId)
                    } else {
                        BookingSummaryKey(bookingId)
                    }
                shell(AppMode.Client, ClientBookingsRoot, key)
            } else {
                notificationsList(context)
            }
        }

        /** The probe bounded by [LOOKUP_TIMEOUT_MS]; a timeout counts as [ClientBookingProbeResult.Unknown]. */
        private suspend fun probe(bookingId: String): ClientBookingProbeResult =
            withTimeoutOrNull(LOOKUP_TIMEOUT_MS) { bookingProbe.probe(bookingId) } ?: ClientBookingProbeResult.Unknown

        private fun notificationsList(context: PushRoutingContext): ResolvedPush {
            val mode = context.currentMode
            return shell(mode, notificationsRootFor(mode), notificationsKeyFor(mode))
        }

        private fun shell(
            mode: AppMode,
            tab: NavKey,
            vararg keys: NavKey,
        ): ResolvedPush = ResolvedPush(mode, PushRoute.Shell(ShellRoute(mode, tab, keys.toList())))
    }
