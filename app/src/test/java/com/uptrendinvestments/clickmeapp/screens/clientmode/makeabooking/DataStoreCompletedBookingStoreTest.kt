package com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.uptrendinvestments.clickmeapp.managers.SessionStore
import com.uptrendinvestments.clickmeapp.testing.testPreferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException

/** WR-01: the durable completed-booking record in the session DataStore. */
class DataStoreCompletedBookingStoreTest {
    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun aRecordIsReturnedOnlyForItsOwnAttempt() =
        runTest {
            val store = DataStoreCompletedBookingStore(testPreferencesDataStore(backgroundScope, tmp), backgroundScope)
            assertEquals(CompletedBookingLookup.Absent, store.get("a1"))
            store.put("a1", "b1")
            assertEquals(CompletedBookingLookup.Found("b1"), store.get("a1"))
            assertEquals(CompletedBookingLookup.Absent, store.get("a2"))
            // A newer attempt replaces the older record.
            store.put("a2", "b2")
            assertEquals(CompletedBookingLookup.Absent, store.get("a1"))
            assertEquals(CompletedBookingLookup.Found("b2"), store.get("a2"))
        }

    @Test
    fun aRecordSurvivesANewDataStoreOverTheSameFile() =
        runTest {
            val file = File(tmp.root, "clickme_session.preferences_pb")
            // The first "process": its DataStore is shut down once the record is written.
            val firstJob = Job()
            val first = PreferenceDataStoreFactory.create(scope = CoroutineScope(coroutineContext + firstJob)) { file }
            DataStoreCompletedBookingStore(first, backgroundScope).put("a1", "b1")
            firstJob.cancelAndJoin()

            // The restarted process opens the same file afresh.
            val second = PreferenceDataStoreFactory.create(scope = backgroundScope) { file }
            assertEquals(
                CompletedBookingLookup.Found("b1"),
                DataStoreCompletedBookingStore(second, backgroundScope).get("a1"),
            )
        }

    @Test
    fun discardRemovesOnlyTheMatchingRecord() =
        runTest {
            val dataStore = testPreferencesDataStore(backgroundScope, tmp)
            val store = DataStoreCompletedBookingStore(dataStore, backgroundScope)
            store.put("a1", "b1")

            store.discard("a2")
            runCurrent()
            // An edit queued after the discard, so the discard has been applied when it returns.
            dataStore.edit { }
            assertEquals(CompletedBookingLookup.Found("b1"), store.get("a1"))

            store.discard("a1")
            runCurrent()
            dataStore.edit { }
            assertEquals(CompletedBookingLookup.Absent, store.get("a1"))
        }

    @Test
    fun theSessionCleanerAndLogoutBothRemoveTheRecord() =
        runTest {
            val dataStore = testPreferencesDataStore(backgroundScope, tmp)
            val store = DataStoreCompletedBookingStore(dataStore, backgroundScope)
            store.put("a1", "b1")
            store.clearSessionData()
            assertEquals(CompletedBookingLookup.Absent, store.get("a1"))

            // Logout wipes the whole session file, which holds the record too.
            store.put("a1", "b1")
            SessionStore(dataStore).clear()
            assertEquals(CompletedBookingLookup.Absent, store.get("a1"))
        }

    @Test
    fun aReadFailureIsUnreadableNotAbsent() =
        runTest {
            val broken =
                object : DataStore<Preferences> {
                    override val data: Flow<Preferences> = flow { throw IOException("disk") }

                    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
                        throw IOException("disk")
                }
            val store = DataStoreCompletedBookingStore(broken, backgroundScope)
            // A failed read is not proof that the attempt created no booking (WR-01).
            assertEquals(CompletedBookingLookup.Unreadable, store.get("a1"))
            // A failed write stays best effort and does not throw.
            store.put("a1", "b1")
        }
}
