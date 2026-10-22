@file:OptIn(ExperimentalCoroutinesApi::class)

package com.uptrendinvestments.clickmeapp.managers

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.uptrendinvestments.clickmeapp.model.responses.CategoriesPayload
import com.uptrendinvestments.clickmeapp.model.responses.CategoryItem
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.MutableClock
import com.uptrendinvestments.clickmeapp.testing.testPreferencesDataStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException
import java.time.Duration
import java.time.Instant

class CategoryStoreTest {
    @get:Rule
    val main = MainDispatcherRule()

    @get:Rule
    val tmp = TemporaryFolder()

    private val start = Instant.parse("2026-10-21T12:00:00Z")
    private val listA = listOf(CategoryItem(id = "tech", name = "Tech", iconName = "laptop", colorAccent = "#00FF88"))
    private val listB = listOf(CategoryItem(id = "art", name = "Art"), CategoryItem(id = "music", name = "Music"))

    private class Setup(
        val api: FakeClickMeApi,
        val clock: MutableClock,
        val dataStore: DataStore<Preferences>,
        val store: CategoryStore,
    )

    private fun TestScope.setup(initial: List<CategoryItem> = listA): Setup {
        val api = FakeClickMeApi().also { it.categoriesHandler = { CategoriesPayload(initial) } }
        val clock = MutableClock(start)
        val dataStore = testPreferencesDataStore(backgroundScope, tmp)
        return Setup(api, clock, dataStore, CategoryStore(api, dataStore, clock))
    }

    @Test
    fun firstFetchCallsApiOnceAndReturnsList() =
        runTest(main.testDispatcher) {
            val s = setup()
            assertEquals(listA, s.store.fetch())
            assertEquals(1, s.api.categoriesCalls)
        }

    @Test
    fun secondFetchWithinTtlMakesNoNetworkCall() =
        runTest(main.testDispatcher) {
            val s = setup()
            s.store.fetch()
            assertEquals(listA, s.store.fetch())
            s.clock.advanceBy(Duration.ofDays(6).plusHours(23))
            assertEquals(listA, s.store.fetch())
            assertEquals(1, s.api.categoriesCalls)
        }

    @Test
    fun newInstanceOverSameDataStoreServesFromDiskWithinTtl() =
        runTest(main.testDispatcher) {
            val s = setup()
            s.store.fetch()
            val second = CategoryStore(s.api, s.dataStore, s.clock)
            assertEquals(listA, second.fetch())
            assertEquals(1, s.api.categoriesCalls)
        }

    @Test
    fun fetchAfterTtlRefetches() =
        runTest(main.testDispatcher) {
            val s = setup()
            s.store.fetch()
            s.api.categoriesHandler = { CategoriesPayload(listB) }
            s.clock.advanceBy(Duration.ofMillis(CategoryStore.TTL_MS + 1))
            assertEquals(listB, s.store.fetch())
            assertEquals(2, s.api.categoriesCalls)
            // The new copy is fresh again.
            assertEquals(listB, s.store.fetch())
            assertEquals(2, s.api.categoriesCalls)
        }

    @Test
    fun refreshAlwaysCallsApi() =
        runTest(main.testDispatcher) {
            val s = setup()
            s.store.fetch()
            s.api.categoriesHandler = { CategoriesPayload(listB) }
            assertEquals(listB, s.store.refresh())
            assertEquals(2, s.api.categoriesCalls)
            assertEquals(listB, s.store.fetch())
            assertEquals(2, s.api.categoriesCalls)
        }

    @Test
    fun failedRefreshThrowsAndKeepsOldCache() =
        runTest(main.testDispatcher) {
            val s = setup()
            s.store.fetch()
            s.api.categoriesHandler = { throw IOException("offline") }
            try {
                s.store.refresh()
                fail("expected ApiException.Network")
            } catch (_: ApiException.Network) {
            }
            assertEquals(2, s.api.categoriesCalls)
            assertEquals(listA, s.store.fetch())
            assertEquals(2, s.api.categoriesCalls)
            // Also true for a store that has to read the old copy from disk.
            assertEquals(listA, CategoryStore(s.api, s.dataStore, s.clock).fetch())
            assertEquals(2, s.api.categoriesCalls)
        }

    @Test
    fun failedFirstFetchThrowsAndCachesNothing() =
        runTest(main.testDispatcher) {
            val s = setup()
            s.api.categoriesHandler = { throw IOException("offline") }
            try {
                s.store.fetch()
                fail("expected ApiException.Network")
            } catch (_: ApiException.Network) {
            }
            s.api.categoriesHandler = { CategoriesPayload(listA) }
            assertEquals(listA, s.store.fetch())
            assertEquals(2, s.api.categoriesCalls)
        }

    @Test
    fun negativeCacheAgeIsStale() =
        runTest(main.testDispatcher) {
            val s = setup()
            s.store.fetch()
            s.clock.instant = start.minus(Duration.ofHours(1))
            s.api.categoriesHandler = { CategoriesPayload(listB) }
            assertEquals(listB, s.store.fetch())
            assertEquals(2, s.api.categoriesCalls)
        }

    @Test
    fun corruptCachedJsonIsAMiss() =
        runTest(main.testDispatcher) {
            val s = setup()
            s.dataStore.edit {
                it[stringPreferencesKey("categories_json")] = "not json"
                it[longPreferencesKey("categories_fetched_at")] = start.toEpochMilli()
            }
            assertEquals(listA, s.store.fetch())
            assertEquals(1, s.api.categoriesCalls)
        }

    @Test
    fun concurrentFetchesOnEmptyCacheShareOneNetworkCall() =
        runTest(main.testDispatcher) {
            val s = setup()
            val gate = CompletableDeferred<Unit>()
            s.api.categoriesHandler = {
                gate.await()
                CategoriesPayload(listA)
            }
            val first = async { s.store.fetch() }
            val second = async { s.store.fetch() }
            runCurrent()
            gate.complete(Unit)
            assertEquals(listA, first.await())
            assertEquals(listA, second.await())
            assertEquals(1, s.api.categoriesCalls)
        }

    @Test
    fun cacheSurvivesSessionStoreClear() =
        runTest(main.testDispatcher) {
            val s = setup()
            val sessionDataStore = testPreferencesDataStore(backgroundScope, tmp, "session.preferences_pb")
            val session = SessionStore(sessionDataStore)
            s.store.fetch()
            session.setHasSeenOnboarding(true)

            session.clear()

            assertEquals(false, session.hasSeenOnboarding())
            assertEquals(listA, CategoryStore(s.api, s.dataStore, s.clock).fetch())
            assertEquals(1, s.api.categoriesCalls)
        }
}
