package com.uptrendinvestments.clickmeapp.managers

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.uptrendinvestments.clickmeapp.di.ApplicationScope
import com.uptrendinvestments.clickmeapp.di.CallResumePrefs
import com.uptrendinvestments.clickmeapp.screens.push.PushLaunchRequest
import kotlinx.coroutines.CoroutineScope
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

/** RED skeleton: the real behaviour lands in the GREEN commit. */
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
        fun start() = Unit

        override suspend fun clearSessionData() = Unit

        companion object {
            const val FILE_NAME = "call_resume"
            const val CUTOFF_SECONDS = 3600L
            val KEY_BOOKING_ID = stringPreferencesKey("call_resume_booking_id")
            val KEY_SCHEDULED_END_MS = longPreferencesKey("call_resume_scheduled_end_ms")
        }
    }
