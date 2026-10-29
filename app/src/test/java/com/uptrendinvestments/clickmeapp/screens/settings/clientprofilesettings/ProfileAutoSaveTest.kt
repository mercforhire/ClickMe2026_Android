@file:OptIn(ExperimentalCoroutinesApi::class)

package com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings

import com.uptrendinvestments.clickmeapp.model.requests.ProfileBasicBody
import com.uptrendinvestments.clickmeapp.model.requests.ProfileLanguagesBody
import com.uptrendinvestments.clickmeapp.model.requests.ProfileLocationBody
import com.uptrendinvestments.clickmeapp.model.requests.ProfileProfessionalBody
import com.uptrendinvestments.clickmeapp.model.requests.UpdateUserProfileRequest
import com.uptrendinvestments.clickmeapp.model.responses.UserProfileData
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.testing.userProfileData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileAutoSaveTest {
    private val serverDown =
        ApiException.Http(
            status = 500,
            code = null,
            rawCode = null,
            serverMessage = "Down",
            fieldErrors = emptyList(),
            detail = null,
        )

    private class Harness(
        test: TestScope,
        val persistDelayMs: Long = 0L,
        sendsNothing: (String, String) -> Boolean = { _, _ -> false },
    ) {
        val mainJob = Job()
        val mainScope = CoroutineScope(mainJob + StandardTestDispatcher(test.testScheduler))
        val calls = mutableListOf<Pair<String, String>>()
        val failures = mutableListOf<ApiException>()
        var failNext: ApiException? = null
        var running = 0
        var maxRunning = 0

        val saver =
            AutoSaver(
                scope = mainScope,
                flushScope = test.backgroundScope,
                baseline = "a",
                persist = { old, new ->
                    running++
                    maxRunning = maxOf(maxRunning, running)
                    try {
                        calls += old to new
                        if (persistDelayMs > 0) delay(persistDelayMs)
                        failNext?.let { throw it }
                    } finally {
                        running--
                    }
                },
                onFailure = { failures += it },
                sendsNothing = sendsNothing,
            )
    }

    @Test
    fun debouncesBurstIntoOneSave() =
        runTest {
            val h = Harness(this, persistDelayMs = 100)
            h.saver.onEdited("b")
            advanceTimeBy(300)
            h.saver.onEdited("bc")
            advanceTimeBy(300)
            h.saver.onEdited("bcd")

            advanceTimeBy(AUTO_SAVE_DEBOUNCE_MS - 1)
            runCurrent()
            assertTrue(h.calls.isEmpty())
            assertEquals(AutoSaveStatus.Idle, h.saver.status.value)

            advanceTimeBy(1)
            runCurrent()
            assertEquals(listOf("a" to "bcd"), h.calls)
            assertEquals(AutoSaveStatus.Saving, h.saver.status.value)

            advanceTimeBy(100)
            runCurrent()
            assertEquals(AutoSaveStatus.Saved, h.saver.status.value)

            advanceTimeBy(SAVED_HOLD_MS - 1)
            runCurrent()
            assertEquals(AutoSaveStatus.Saved, h.saver.status.value)
            advanceTimeBy(1)
            runCurrent()
            assertEquals(AutoSaveStatus.Idle, h.saver.status.value)
            assertEquals(1, h.calls.size)
            h.mainJob.cancel()
        }

    @Test
    fun equalSnapshotIsSkipped() =
        runTest {
            val h = Harness(this)
            h.saver.onEdited("b")
            h.saver.onEdited("a")
            advanceTimeBy(AUTO_SAVE_DEBOUNCE_MS * 2)
            runCurrent()
            assertTrue(h.calls.isEmpty())
            assertEquals(AutoSaveStatus.Idle, h.saver.status.value)
            h.mainJob.cancel()
        }

    @Test
    fun editThatSendsNothingTakesTheBaselineWithoutASaveFlash() =
        runTest {
            val h = Harness(this, sendsNothing = { old, new -> old.trim() == new.trim() })
            h.saver.onEdited("a ")
            advanceTimeBy(AUTO_SAVE_DEBOUNCE_MS)
            runCurrent()

            assertTrue(h.calls.isEmpty())
            assertEquals(AutoSaveStatus.Idle, h.saver.status.value)

            // The baseline moved, so the next real change is diffed against the whitespace edit.
            h.saver.onEdited("b")
            advanceTimeBy(AUTO_SAVE_DEBOUNCE_MS)
            runCurrent()
            assertEquals(listOf("a " to "b"), h.calls)
            h.mainJob.cancel()
        }

    @Test
    fun failureKeepsBaselineAndRetrySavesLatest() =
        runTest {
            val h = Harness(this)
            h.failNext = serverDown
            h.saver.onEdited("b")
            advanceTimeBy(AUTO_SAVE_DEBOUNCE_MS)
            runCurrent()

            assertEquals(AutoSaveStatus.Error, h.saver.status.value)
            assertEquals(listOf<ApiException>(serverDown), h.failures)

            h.failNext = null
            h.saver.retry()
            runCurrent()

            assertEquals(listOf("a" to "b", "a" to "b"), h.calls)
            assertEquals(AutoSaveStatus.Saved, h.saver.status.value)
            h.mainJob.cancel()
        }

    @Test
    fun savedBaselineMovesOnlyAfterSuccess() =
        runTest {
            val h = Harness(this)
            h.saver.onEdited("b")
            advanceTimeBy(AUTO_SAVE_DEBOUNCE_MS)
            runCurrent()
            h.saver.onEdited("c")
            advanceTimeBy(AUTO_SAVE_DEBOUNCE_MS)
            runCurrent()

            assertEquals(listOf("a" to "b", "b" to "c"), h.calls)
            h.mainJob.cancel()
        }

    @Test
    fun savesNeverOverlap() =
        runTest {
            val h = Harness(this, persistDelayMs = 1_000)
            h.saver.onEdited("b")
            advanceTimeBy(AUTO_SAVE_DEBOUNCE_MS)
            runCurrent()
            assertEquals(AutoSaveStatus.Saving, h.saver.status.value)

            h.saver.onEdited("bc")
            advanceTimeBy(AUTO_SAVE_DEBOUNCE_MS)
            runCurrent()
            advanceTimeBy(5_000)
            runCurrent()

            assertEquals(listOf("a" to "b", "b" to "bc"), h.calls)
            assertEquals(1, h.maxRunning)
            h.mainJob.cancel()
        }

    @Test
    fun flushSavesPendingInFlushScope() =
        runTest {
            val h = Harness(this, persistDelayMs = 500)
            h.saver.onEdited("b")
            h.saver.flush()
            runCurrent()
            assertEquals(listOf("a" to "b"), h.calls)

            h.mainJob.cancel()
            advanceTimeBy(600)
            runCurrent()

            assertEquals(AutoSaveStatus.Saved, h.saver.status.value)
            advanceTimeBy(AUTO_SAVE_DEBOUNCE_MS * 2)
            runCurrent()
            assertEquals(1, h.calls.size)
        }

    @Test
    fun flushWithNothingPendingDoesNothing() =
        runTest {
            val h = Harness(this)
            h.saver.flush()
            runCurrent()
            assertTrue(h.calls.isEmpty())
            assertEquals(AutoSaveStatus.Idle, h.saver.status.value)
            h.mainJob.cancel()
        }

    // ---- buildProfilePatch ----

    private val base = ProfileSnapshot.from(userProfileData())

    @Test
    fun snapshotMapsNullsToBlankAndDropsBlankLanguages() {
        val profile =
            userProfileData(
                firstName = null,
                bio = null,
                country = null,
                languages =
                    listOf(
                        UserProfileData.LanguageEntry("en", "English"),
                        UserProfileData.LanguageEntry("xx", null),
                        UserProfileData.LanguageEntry(null, "Klingon"),
                    ),
            )
        val snapshot = ProfileSnapshot.from(profile)

        assertEquals("", snapshot.firstName)
        assertEquals("", snapshot.bio)
        assertEquals("", snapshot.countryCode)
        assertEquals(listOf(ProfileLanguage("en", "English"), ProfileLanguage(null, "Klingon")), snapshot.languages)
    }

    @Test
    fun noChangeGivesNoPatch() {
        assertNull(buildProfilePatch(base, base.copy()))
    }

    @Test
    fun buildPatchIncludesOnlyChangedGroups() {
        assertEquals(
            UpdateUserProfileRequest(location = ProfileLocationBody("Paris", "", "CAN")),
            buildProfilePatch(base, base.copy(city = "Paris")),
        )
        assertEquals(
            UpdateUserProfileRequest(basic = ProfileBasicBody("Anna", "Lee", "")),
            buildProfilePatch(base, base.copy(firstName = "Anna")),
        )
        assertEquals(
            UpdateUserProfileRequest(professional = ProfileProfessionalBody("Coach", "")),
            buildProfilePatch(base, base.copy(jobTitle = "Coach")),
        )
    }

    @Test
    fun languagesSplitIntoIdsAndCustomLabels() {
        val changed =
            base.copy(languages = listOf(ProfileLanguage("en", "English"), ProfileLanguage(null, "Klingon")))

        assertEquals(
            UpdateUserProfileRequest(languages = ProfileLanguagesBody(listOf("en"), listOf("Klingon"))),
            buildProfilePatch(base, changed),
        )
    }

    @Test
    fun removingEveryLanguageSendsBothListsEmpty() {
        assertEquals(
            UpdateUserProfileRequest(languages = ProfileLanguagesBody(emptyList(), emptyList())),
            buildProfilePatch(base, base.copy(languages = emptyList())),
        )
    }

    @Test
    fun blankCountryIsSentAsNullAndTextIsTrimmed() {
        val patch = buildProfilePatch(base, base.copy(countryCode = "", city = "  Rome "))

        assertEquals(UpdateUserProfileRequest(location = ProfileLocationBody("Rome", "", null)), patch)
    }

    @Test
    fun whitespaceOnlyChangeGivesNoPatch() {
        assertNull(buildProfilePatch(base, base.copy(firstName = "Ana ")))
    }
}
