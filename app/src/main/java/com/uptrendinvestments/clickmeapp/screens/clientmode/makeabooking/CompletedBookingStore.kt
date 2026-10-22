package com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.uptrendinvestments.clickmeapp.di.ApplicationScope
import com.uptrendinvestments.clickmeapp.managers.SessionDataCleaner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Durable record of the free booking that the last booking attempt created (WR-01).
 *
 * `SavedStateHandle` reaches the system only when the activity saves its state, right after `onStop`. A booking
 * response that lands after that (the user left the app while the request was in flight) would be written to memory
 * only and lost if the process is then killed, and the restored form would let the user book a second slot. The
 * process is still alive when the response arrives, so [MakeABookingViewModel] writes the result here, to disk,
 * before it shows Request Sent.
 *
 * Records are keyed by a random attempt id that the ViewModel stores in its `SavedStateHandle` before the request
 * is sent (always while the screen is in the foreground, so always before the save). Only a ViewModel restored from
 * that saved state can read the record back, so a record can never resurface in a later, unrelated booking flow.
 * The record is discarded once Request Sent has been shown. iOS has no equivalent: it does not restore a booking
 * form after the app is terminated.
 */
interface CompletedBookingStore {
    /** Records that [attemptId] created [bookingId], replacing any older record. Returns once written; best effort. */
    suspend fun put(
        attemptId: String,
        bookingId: String,
    )

    /** The booking id created by [attemptId], or null when there is none (or it cannot be read). */
    suspend fun get(attemptId: String): String?

    /**
     * Forgets [attemptId]'s record if it is still the stored one. Fire and forget: the write must finish even though
     * the calling ViewModel is about to be cleared.
     */
    fun discard(attemptId: String)
}

/**
 * [CompletedBookingStore] in the session Preferences DataStore (`clickme_session`). One record at a time (only one
 * booking flow can be open). The session file is wiped by `SessionStore.clear()` at logout, and this store is also a
 * [SessionDataCleaner], so a record can never reach another account.
 */
@Singleton
class DataStoreCompletedBookingStore
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
        @ApplicationScope private val appScope: CoroutineScope,
    ) : CompletedBookingStore,
        SessionDataCleaner {
        override suspend fun put(
            attemptId: String,
            bookingId: String,
        ) {
            try {
                dataStore.edit {
                    it[ATTEMPT_KEY] = attemptId
                    it[BOOKING_KEY] = bookingId
                }
            } catch (_: IOException) {
                // Best effort: the in-memory and saved-state copies still cover every case except process death.
            }
        }

        override suspend fun get(attemptId: String): String? {
            val prefs =
                try {
                    dataStore.data.first()
                } catch (_: IOException) {
                    return null
                }
            return if (prefs[ATTEMPT_KEY] == attemptId) prefs[BOOKING_KEY] else null
        }

        override fun discard(attemptId: String) {
            appScope.launch {
                try {
                    dataStore.edit {
                        if (it[ATTEMPT_KEY] == attemptId) {
                            it.remove(ATTEMPT_KEY)
                            it.remove(BOOKING_KEY)
                        }
                    }
                } catch (_: IOException) {
                    // A record left behind is harmless: no later flow knows its attempt id, and logout wipes it.
                }
            }
        }

        override suspend fun clearSessionData() {
            dataStore.edit {
                it.remove(ATTEMPT_KEY)
                it.remove(BOOKING_KEY)
            }
        }

        private companion object {
            val ATTEMPT_KEY = stringPreferencesKey("booking_completed_attempt_id")
            val BOOKING_KEY = stringPreferencesKey("booking_completed_booking_id")
        }
    }
