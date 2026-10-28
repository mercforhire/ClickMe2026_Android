package com.uptrendinvestments.clickmeapp.screens.push

import androidx.navigation3.runtime.NavKey
import com.uptrendinvestments.clickmeapp.model.enums.AppMode
import com.uptrendinvestments.clickmeapp.screens.chat.chatting.ChatThreadKey
import com.uptrendinvestments.clickmeapp.screens.clientmode.bookingsummary.BookingSummaryKey
import com.uptrendinvestments.clickmeapp.screens.clientmode.home.subviews.ClientBookingsRoot
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.CANCELLABLE_BOOKING_STATUSES
import com.uptrendinvestments.clickmeapp.screens.clientmode.upcomingbooking.UpcomingBookingKey
import com.uptrendinvestments.clickmeapp.screens.expertmode.bookingsummary.ExpertBookingOrigin
import com.uptrendinvestments.clickmeapp.screens.expertmode.bookingsummary.ExpertBookingSummaryKey
import com.uptrendinvestments.clickmeapp.screens.expertmode.home.subviews.DashboardRoot
import com.uptrendinvestments.clickmeapp.screens.expertmode.home.subviews.ExpertBookingsRoot
import com.uptrendinvestments.clickmeapp.screens.expertmode.incomingrequests.IncomingRequestsKey
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
        /**
         * Resolves [destination] for the app state in [context]. The decision table, CONTEXT D-05..D-10:
         * - Booking and review pushes: an Expert-audience push goes to the expert Booking Summary without a probe. Any
         *   other audience probes `GET client/bookings/{id}` (5 s cap): a client booking opens Upcoming Booking
         *   Details for a live status (booking pushes only) or Booking Summary, a 404/403 means the user is the
         *   expert, and an unreadable answer keeps a Client push on the client Booking Summary and an Either push in
         *   the current mode. Reviews never open a review screen directly (D-07).
         * - Chat: the current mode's Chats tab, never a switch and never gated by a call (D-09).
         * - Incoming requests: Request Decision when the request is still in the pending list, else the list (D-08).
         * - Notifications list, Payouts, KYC and Account Security: the current mode's list (D-10, D-11).
         *
         * A route in the other mode is honoured only when [allowSwitch] says so (no call, expert role for Expert);
         * otherwise the current mode's notifications list opens. This resolver only decides: the caller (13-11)
         * performs the switch and posts the route.
         */
        suspend fun resolve(
            destination: PushDestination,
            context: PushRoutingContext,
        ): ResolvedPush {
            val wanted =
                when (destination) {
                    is PushDestination.BookingDetail -> {
                        resolveBooking(destination.bookingId, destination.audience, isReview = false, context)
                    }

                    is PushDestination.LeaveReview -> {
                        resolveBooking(destination.bookingId, destination.audience, isReview = true, context)
                    }

                    is PushDestination.ChatThread -> {
                        return ResolvedPush(context.currentMode, PushRoute.Chat(ChatThreadKey(destination.threadId)))
                    }

                    is PushDestination.IncomingRequests -> {
                        // Skip the list call when the switch to Expert is refused anyway.
                        if (!allowSwitch(context.currentMode, AppMode.Expert, context)) {
                            return notificationsList(context)
                        }
                        resolveIncomingRequest(destination.requestId)
                    }

                    // PHASE-14: Payouts, KycOnboarding and AccountSecurity get real routes in Phase 14 (D-10).
                    is PushDestination.Payouts,
                    is PushDestination.KycOnboarding,
                    is PushDestination.AccountSecurity,
                    is PushDestination.NotificationsList,
                    -> {
                        return notificationsList(context)
                    }
                }
            val allowed = allowSwitch(context.currentMode, wanted.targetMode, context)
            return if (allowed) wanted else notificationsList(context)
        }

        /**
         * A switch is allowed within a mode, or when no call is active and the target is Client or the user has the
         * expert role. Never switching a client-only account to Expert matters because that switch converts the
         * account and resumes into expert setup on every launch (RESEARCH F-5, Open Questions 2 and 4).
         */
        private fun allowSwitch(
            from: AppMode,
            to: AppMode,
            context: PushRoutingContext,
        ): Boolean = from == to || (!context.callActive && (to == AppMode.Client || context.hasExpertRole))

        /**
         * Request Decision with the pending row's expiry when [requestId] is still in the list, else Incoming Requests
         * alone: never a decidable Request Decision for a request that may already be processed (RESEARCH A10).
         */
        private suspend fun resolveIncomingRequest(requestId: String?): ResolvedPush {
            val decisionKey =
                requestId?.let { withTimeoutOrNull(LOOKUP_TIMEOUT_MS) { pendingRequests.decisionKeyFor(it) } }
            return if (decisionKey != null) {
                shell(AppMode.Expert, DashboardRoot, IncomingRequestsKey, decisionKey)
            } else {
                shell(AppMode.Expert, DashboardRoot, IncomingRequestsKey)
            }
        }

        private suspend fun resolveBooking(
            bookingId: String,
            audience: PushAudience,
            isReview: Boolean,
            context: PushRoutingContext,
        ): ResolvedPush {
            if (audience == PushAudience.Expert) return expertSummary(bookingId)
            return when (val result = probe(bookingId)) {
                is ClientBookingProbeResult.Client -> {
                    val key: NavKey =
                        if (!isReview && result.status in PUSH_UPCOMING_STATUSES) {
                            UpcomingBookingKey(bookingId)
                        } else {
                            BookingSummaryKey(bookingId)
                        }
                    shell(AppMode.Client, ClientBookingsRoot, key)
                }

                ClientBookingProbeResult.NotClient -> {
                    expertSummary(bookingId)
                }

                ClientBookingProbeResult.Unknown -> {
                    if (audience == PushAudience.Client || context.currentMode == AppMode.Client) {
                        shell(AppMode.Client, ClientBookingsRoot, BookingSummaryKey(bookingId))
                    } else {
                        expertSummary(bookingId)
                    }
                }
            }
        }

        private fun expertSummary(bookingId: String): ResolvedPush {
            val key = ExpertBookingSummaryKey(bookingId, ExpertBookingOrigin.Bookings)
            return shell(AppMode.Expert, ExpertBookingsRoot, key)
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
