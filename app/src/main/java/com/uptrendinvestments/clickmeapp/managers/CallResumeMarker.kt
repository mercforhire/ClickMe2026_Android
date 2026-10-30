package com.uptrendinvestments.clickmeapp.managers

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.uptrendinvestments.clickmeapp.di.ApplicationScope
import com.uptrendinvestments.clickmeapp.di.CallResumePrefs
import com.uptrendinvestments.clickmeapp.model.enums.ConnectionType
import com.uptrendinvestments.clickmeapp.screens.push.PushAudience
import com.uptrendinvestments.clickmeapp.screens.push.PushDestination
import com.uptrendinvestments.clickmeapp.screens.push.PushLaunchRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.Clock
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists "an in-app call is live" so a process kill during a call lands the user back on that booking's detail
 * screen, where Join Call is the primary action (CONTEXT D-11, SC-2 "call").
 *
 * Chooses the landing screen only; never joins. Nothing here references the call engine, the call starter or the
 * microphone permission gate: the token and the permission are fetched again only when the user taps Join Call.
 *
 * The marker holds the booking id and the scheduled end and nothing else (no peer name, token, channel or user id;
 * T-15-19). It is written while an in-app session is live and cleared when the session ends, on logout (it is a
 * [SessionDataCleaner]) and once the routed destination has been taken by the screen shell. At [start], a marker with no live session is routed through
 * [PushLaunchRequest] as a `BookingDetail` with the Either audience, so the Phase 13 push routing resolves the
 * client's Upcoming Booking Details or the expert's Booking Summary; it is dropped without routing once the booking
 * is more than [CUTOFF_SECONDS] past its scheduled end (the CALL-07 cutoff). A live session at [start] (an Activity
 * recreation) never routes. [CallResumeNotice] flags the booking so the landing screen shows the one-time resume note.
 */
@Singleton
class CallResumeMarker
    @Inject
    constructor(
        @CallResumePrefs private val dataStore: DataStore<Preferences>,
        private val callCenter: CallCenter,
        private val pushLaunchRequest: PushLaunchRequest,
        private val notice: CallResumeNotice,
        private val clock: Clock,
        @ApplicationScope private val scope: CoroutineScope,
    ) : SessionDataCleaner {
        /** Resumes a stored marker once, then keeps the store in step with the live session. Call once at app start. */
        fun start() {
            scope.launch {
                resumeStoredMarker()
                callCenter.session
                    .map { session -> session?.toMarker() }
                    .distinctUntilChanged()
                    .collect { marker -> if (marker == null) clearStore() else write(marker) }
            }
        }

        override suspend fun clearSessionData() {
            clearStore()
        }

        private suspend fun resumeStoredMarker() {
            val prefs = readOrNull() ?: return
            val bookingId = prefs[KEY_BOOKING_ID]
            val scheduledEndMs = prefs[KEY_SCHEDULED_END_MS]
            if (bookingId == null && scheduledEndMs == null) return
            if (bookingId.isNullOrBlank() || scheduledEndMs == null || callCenter.session.value != null) {
                clearStore()
                return
            }
            val cutoff = Instant.ofEpochMilli(scheduledEndMs).plusSeconds(CUTOFF_SECONDS)
            if (!clock.instant().isBefore(cutoff)) {
                clearStore()
                return
            }
            // An explicit destination already waiting (a push tap) wins: the resume steps aside rather than hijack it.
            val destination = PushDestination.BookingDetail(bookingId, PushAudience.Either)
            if (!pushLaunchRequest.destination.postIfAbsent(destination)) {
                clearStore()
                return
            }
            notice.post(bookingId)
            // Consumed only once the screen shell has taken the destination (or something replaced it). A process
            // that was started for no UI (a push wake-up) and is killed again keeps the marker for the real launch.
            pushLaunchRequest.destination.pending.first { pending -> pending != destination }
            clearStore()
        }

        private fun CallSession.toMarker(): Marker? {
            if (connectionType != ConnectionType.InAppVoice || status == CallStatus.Ended) return null
            val end = scheduledEnd ?: scheduledStart.plus(FALLBACK_DURATION)
            return Marker(bookingId, end.toEpochMilli())
        }

        private suspend fun write(marker: Marker) {
            try {
                dataStore.edit {
                    it[KEY_BOOKING_ID] = marker.bookingId
                    it[KEY_SCHEDULED_END_MS] = marker.scheduledEndMs
                }
            } catch (_: IOException) {
                // Best effort: without the marker a killed call just restores the previous screen.
            }
        }

        private suspend fun clearStore() {
            try {
                dataStore.edit { it.clear() }
            } catch (_: IOException) {
                // Best effort.
            }
        }

        private suspend fun readOrNull(): Preferences? =
            try {
                dataStore.data.first()
            } catch (_: IOException) {
                null
            }

        private data class Marker(
            val bookingId: String,
            val scheduledEndMs: Long,
        )

        companion object {
            const val FILE_NAME = "call_resume"

            /** Seconds past the scheduled end after which a booking can no longer be rejoined (CALL-07). */
            const val CUTOFF_SECONDS = 3600L

            val KEY_BOOKING_ID = stringPreferencesKey("call_resume_booking_id")
            val KEY_SCHEDULED_END_MS = longPreferencesKey("call_resume_scheduled_end_ms")

            private val FALLBACK_DURATION: Duration = Duration.ofHours(2)
        }
    }
