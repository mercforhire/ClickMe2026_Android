package com.uptrendinvestments.clickmeapp.managers

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.uptrendinvestments.clickmeapp.model.enums.AppMode
import com.uptrendinvestments.clickmeapp.model.responses.UserProfileData
import com.uptrendinvestments.clickmeapp.testing.testPreferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SessionStoreTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private val sampleProfile =
        UserProfileData(
            personalDetails =
                UserProfileData.PersonalDetails(
                    firstName = "Ada",
                    lastName = "Lovelace",
                    email = "ada@example.com",
                    avatarUrl = "https://cdn/a.png",
                    bio = "Mathematician",
                ),
            professionalDetails =
                UserProfileData.ProfessionalDetails(jobTitle = "Engineer", company = "Analytical", education = "Home"),
            location =
                UserProfileData.Location(
                    city = "London",
                    stateProvince = null,
                    country = "GBR",
                    timezone = "Europe/London",
                ),
            languages = listOf(UserProfileData.LanguageEntry(id = "en", label = "English")),
        )

    private fun TestScope.newStore(): Pair<DataStore<Preferences>, SessionStore> {
        val dataStore = testPreferencesDataStore(backgroundScope, tmp)
        return dataStore to SessionStore(dataStore)
    }

    @Test
    fun freshStoreHasNoModeNoOnboardingNoProfile() =
        runTest {
            val (_, store) = newStore()
            assertNull(store.storedMode())
            assertFalse(store.hasSeenOnboarding())
            assertNull(store.cachedProfile())
        }

    @Test
    fun setModePersistsWireValue() =
        runTest {
            val (dataStore, store) = newStore()
            store.setMode(AppMode.Expert)
            assertEquals(AppMode.Expert, store.storedMode())
            assertEquals("expert", dataStore.data.first()[stringPreferencesKey("current_mode")])
        }

    @Test
    fun unknownStoredModeReadsAsNull() =
        runTest {
            val (dataStore, store) = newStore()
            dataStore.edit { it[stringPreferencesKey("current_mode")] = "admin" }
            assertNull(store.storedMode())
        }

    @Test
    fun onboardingFlagRoundTrips() =
        runTest {
            val (_, store) = newStore()
            store.setHasSeenOnboarding(true)
            assertTrue(store.hasSeenOnboarding())
            store.setHasSeenOnboarding(false)
            assertFalse(store.hasSeenOnboarding())
        }

    @Test
    fun cachedProfileRoundTripsFully() =
        runTest {
            val (_, store) = newStore()
            store.setCachedProfile(sampleProfile)
            assertEquals(sampleProfile, store.cachedProfile())
        }

    @Test
    fun garbageProfileJsonReadsAsNullInsteadOfThrowing() =
        runTest {
            val (dataStore, store) = newStore()
            dataStore.edit { it[stringPreferencesKey("cached_profile_json")] = "not json {{{" }
            assertNull(store.cachedProfile())
        }

    @Test
    fun clearRemovesModeOnboardingAndProfile() =
        runTest {
            val (_, store) = newStore()
            store.setMode(AppMode.Client)
            store.setHasSeenOnboarding(true)
            store.setCachedProfile(sampleProfile)

            store.clear()

            assertNull(store.storedMode())
            assertFalse(store.hasSeenOnboarding())
            assertNull(store.cachedProfile())
        }
}
