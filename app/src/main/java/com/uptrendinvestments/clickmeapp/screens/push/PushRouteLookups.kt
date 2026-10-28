package com.uptrendinvestments.clickmeapp.screens.push

import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.screens.clientmode.mybookings.ClientBookingsRepository
import com.uptrendinvestments.clickmeapp.screens.expertmode.incomingrequests.BookingRequestsRepository
import com.uptrendinvestments.clickmeapp.screens.expertmode.requestdecision.RequestDecisionKey
import java.time.Clock
import javax.inject.Inject

/** What the client-booking probe learned about a booking id (RESEARCH F-6). */
sealed interface ClientBookingProbeResult {
    /** The caller is the client on the booking; [status] is its live wire status. */
    data class Client(
        val status: String,
    ) : ClientBookingProbeResult

    /** The server says this is not the caller's client booking, so the caller is its expert. */
    data object NotClient : ClientBookingProbeResult

    /** The probe could not tell (offline, 5xx, timeout). */
    data object Unknown : ClientBookingProbeResult
}

/**
 * Asks the backend which side of a booking the signed-in user is on, and the booking's live status. A seam so the
 * resolver stays pure and JVM-tested (D-06).
 */
fun interface ClientBookingProbe {
    suspend fun probe(bookingId: String): ClientBookingProbeResult
}

/**
 * Finds the pending Request Decision key for an expert push (D-08). Null means "not pending or not knowable": the
 * resolver then opens Incoming Requests instead.
 */
fun interface PendingRequestLookup {
    suspend fun decisionKeyFor(requestId: String): RequestDecisionKey?
}

/**
 * [ClientBookingProbe] over `GET /client/bookings/{id}`. The endpoint filters by `client_id` and answers 404 for
 * anyone else, and `roleCheck('client')` answers 403 without the client role (RESEARCH F-6). Self-booking is
 * rejected, so a booking is never both: 404 or 403 means the caller is the expert. Any other [ApiException] is
 * [ClientBookingProbeResult.Unknown]; coroutine cancellation propagates.
 */
class RepositoryClientBookingProbe
    @Inject
    constructor(
        private val repository: ClientBookingsRepository,
    ) : ClientBookingProbe {
        override suspend fun probe(bookingId: String): ClientBookingProbeResult =
            try {
                ClientBookingProbeResult.Client(repository.detail(bookingId).status)
            } catch (e: ApiException.Http) {
                if (e.status == HTTP_FORBIDDEN || e.status == HTTP_NOT_FOUND) {
                    ClientBookingProbeResult.NotClient
                } else {
                    ClientBookingProbeResult.Unknown
                }
            } catch (_: ApiException) {
                ClientBookingProbeResult.Unknown
            }

        private companion object {
            const val HTTP_FORBIDDEN = 403
            const val HTTP_NOT_FOUND = 404
        }
    }

/** Skeleton, filled in by the GREEN commit. */
class RepositoryPendingRequestLookup
    @Inject
    constructor(
        @Suppress("UnusedPrivateProperty") private val repository: BookingRequestsRepository,
        @Suppress("UnusedPrivateProperty") private val clock: Clock,
    ) : PendingRequestLookup {
        override suspend fun decisionKeyFor(requestId: String): RequestDecisionKey? = null
    }
