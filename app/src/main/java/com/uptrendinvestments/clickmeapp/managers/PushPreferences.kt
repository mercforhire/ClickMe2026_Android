package com.uptrendinvestments.clickmeapp.managers

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.uptrendinvestments.clickmeapp.di.PushPrefs
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Push-related device state in the `clickme_push` DataStore: the stable device id sent to
 * `POST notifications/tokens` and `DELETE notifications/tokens/:device_id`.
 *
 * Deliberately not a `SessionDataCleaner` and not the session file, because the device id must match across
 * logins and restarts (RESEARCH Pattern 5). It is a random UUID; no hardware identifier is used.
 */
@Singleton
class PushPreferences
    @Inject
    constructor(
        @PushPrefs private val dataStore: DataStore<Preferences>,
    ) {
        /** The persisted device id; created once, on first use, inside a single atomic edit. */
        suspend fun deviceId(): String {
            dataStore.data.first()[DEVICE_ID]?.let { return it }
            var result = ""
            dataStore.edit { prefs ->
                result = prefs[DEVICE_ID] ?: UUID.randomUUID().toString().also { prefs[DEVICE_ID] = it }
            }
            return result
        }

        companion object {
            const val FILE_NAME = "clickme_push"
            private val DEVICE_ID = stringPreferencesKey("device_id")
        }
    }
