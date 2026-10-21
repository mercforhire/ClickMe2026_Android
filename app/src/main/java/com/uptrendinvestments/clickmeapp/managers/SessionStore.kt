package com.uptrendinvestments.clickmeapp.managers

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.uptrendinvestments.clickmeapp.model.enums.AppMode
import com.uptrendinvestments.clickmeapp.model.responses.UserProfileData
import com.uptrendinvestments.clickmeapp.network.MoshiProvider
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Non-secret session state in Preferences DataStore: the cached [UserProfileData], the last chosen
 * [AppMode] and the has-seen-onboarding flag. Ports the three iOS UserDefaults keys (`cachedUserProfile`,
 * `currentMode`, `hasSeenOnboarding`).
 *
 * Tokens never go here (they live in the encrypted token store). `MeData.pricingLimits` is never stored
 * (PRICING-LIMITS.md). The profile is the full iOS `UserProfileData` mirror and is used to render home on an
 * offline launch (D-01).
 *
 * [storedMode] returns null when nothing was saved, so callers can tell a stored preference from the default
 * (iOS `hasStoredModePreference`).
 */
@Singleton
class SessionStore
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
    ) {
        private val profileAdapter = MoshiProvider.moshi.adapter(UserProfileData::class.java)

        suspend fun storedMode(): AppMode? = AppMode.fromWire(dataStore.data.first()[MODE_KEY])

        suspend fun setMode(mode: AppMode) {
            dataStore.edit { it[MODE_KEY] = mode.wire }
        }

        suspend fun hasSeenOnboarding(): Boolean = dataStore.data.first()[ONBOARDING_KEY] ?: false

        suspend fun setHasSeenOnboarding(seen: Boolean) {
            dataStore.edit { it[ONBOARDING_KEY] = seen }
        }

        /** Returns null when no profile is cached or the stored JSON cannot be decoded. */
        suspend fun cachedProfile(): UserProfileData? {
            val json = dataStore.data.first()[PROFILE_KEY] ?: return null
            return runCatching { profileAdapter.fromJson(json) }.getOrNull()
        }

        suspend fun setCachedProfile(profile: UserProfileData) {
            val json = profileAdapter.toJson(profile)
            dataStore.edit { it[PROFILE_KEY] = json }
        }

        /**
         * True after the user chose "Not now" on the setup hub (D-27): launch, login-restore and the foreground
         * re-check stop forcing setup until a switch to Expert, a publish or a logout clears it.
         */
        suspend fun isSetupDeferred(): Boolean = dataStore.data.first()[SETUP_DEFERRED_KEY] ?: false

        suspend fun setSetupDeferred(deferred: Boolean) {
            dataStore.edit { it[SETUP_DEFERRED_KEY] = deferred }
        }

        /**
         * Removes the profile, mode, onboarding flag and setup deferral in a single edit (logout / suspension,
         * FND-08, D-27).
         */
        suspend fun clear() {
            dataStore.edit { it.clear() }
        }

        companion object {
            const val FILE_NAME = "clickme_session"

            private val MODE_KEY = stringPreferencesKey("current_mode")
            private val ONBOARDING_KEY = booleanPreferencesKey("has_seen_onboarding")
            private val PROFILE_KEY = stringPreferencesKey("cached_profile_json")
            private val SETUP_DEFERRED_KEY = booleanPreferencesKey("setup_deferred")
        }
    }
