package com.uptrendinvestments.clickmeapp.managers

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.squareup.moshi.Types
import com.uptrendinvestments.clickmeapp.di.CategoryCache
import com.uptrendinvestments.clickmeapp.model.responses.CategoryItem
import com.uptrendinvestments.clickmeapp.network.ClickMeApi
import com.uptrendinvestments.clickmeapp.network.MoshiProvider
import com.uptrendinvestments.clickmeapp.network.apiCall
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Port of iOS `CategoryStore`: `GET /categories` behind a 7-day cache (D-07).
 *
 * [fetch] serves the in-memory copy, else the DataStore copy, while either is under [TTL_MS] old, and only
 * then calls the network. [refresh] always calls the network. A failed network call throws the
 * [com.uptrendinvestments.clickmeapp.network.ApiException] and leaves the existing cache untouched, so the
 * caller (the All Categories sheet, D-06) can show Retry and the old list still serves later.
 *
 * Freshness uses the injected wall [Clock] (the fetch time is persisted, so a monotonic clock would be wrong
 * after a reboot). A negative age, from the device clock moving backwards, counts as stale. Cached JSON that
 * no longer decodes counts as a miss. A [Mutex] makes concurrent callers share one network call.
 *
 * The list is public reference data shared by every user, so it is deliberately NOT cleared at logout and NOT
 * registered with `SessionDataCleaner` (D-08). It lives in its own `clickme_categories` DataStore file for the
 * same reason. The Explore chip row uses the trending categories from `/client/home` instead (D-09).
 */
@Singleton
class CategoryStore
    @Inject
    constructor(
        private val api: ClickMeApi,
        @CategoryCache private val dataStore: DataStore<Preferences>,
        private val clock: Clock,
    ) {
        private val adapter =
            MoshiProvider.moshi.adapter<List<CategoryItem>>(
                Types.newParameterizedType(List::class.java, CategoryItem::class.java),
            )
        private val mutex = Mutex()
        private var memory: List<CategoryItem>? = null
        private var memoryFetchedAt = 0L

        /** The categories, from cache while it is under 7 days old, otherwise from the network. */
        suspend fun fetch(): List<CategoryItem> = mutex.withLock { freshCache() ?: fetchAndSave() }

        /** Bypasses the cache and re-fetches, rewriting the cache on success. */
        suspend fun refresh(): List<CategoryItem> = mutex.withLock { fetchAndSave() }

        private fun isFresh(fetchedAt: Long): Boolean = (clock.millis() - fetchedAt) in 0 until TTL_MS

        private suspend fun freshCache(): List<CategoryItem>? {
            memory?.let { if (isFresh(memoryFetchedAt)) return it }
            val prefs = dataStore.data.first()
            val fetchedAt = prefs[FETCHED_AT_KEY] ?: return null
            if (!isFresh(fetchedAt)) return null
            val list = prefs[DATA_KEY]?.let { runCatching { adapter.fromJson(it) }.getOrNull() } ?: return null
            memory = list
            memoryFetchedAt = fetchedAt
            return list
        }

        private suspend fun fetchAndSave(): List<CategoryItem> {
            val list = apiCall { api.getCategories() }.data.categories
            val now = clock.millis()
            val json = adapter.toJson(list)
            dataStore.edit {
                it[DATA_KEY] = json
                it[FETCHED_AT_KEY] = now
            }
            memory = list
            memoryFetchedAt = now
            return list
        }

        companion object {
            const val FILE_NAME = "clickme_categories"

            /** Seven days, as iOS `staleAfter`. */
            const val TTL_MS = 604_800_000L

            private val DATA_KEY = stringPreferencesKey("categories_json")
            private val FETCHED_AT_KEY = longPreferencesKey("categories_fetched_at")
        }
    }
