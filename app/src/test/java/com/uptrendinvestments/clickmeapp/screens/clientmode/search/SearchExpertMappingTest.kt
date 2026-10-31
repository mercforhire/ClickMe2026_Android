package com.uptrendinvestments.clickmeapp.screens.clientmode.search

import com.uptrendinvestments.clickmeapp.testing.searchResultItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchExpertMappingTest {
    @Test
    fun fullRowMapsEveryField() {
        val ui =
            searchResultItem(
                id = "e1",
                fullName = "Ada Lovelace",
                headline = "Engineer",
                avatarUrl = "https://x/a.png",
                avgRating = 4.5,
                tags = listOf("AI", "Math"),
            ).toSearchExpertUi()

        assertEquals(
            SearchExpertUi("e1", "Ada Lovelace", "Engineer", "https://x/a.png", 4.5, listOf("AI", "Math"), bio = ""),
            ui,
        )
    }

    @Test
    fun nullRowAppliesIosFallbacks() {
        val ui =
            searchResultItem(
                fullName = null,
                headline = null,
                avatarUrl = null,
                avgRating = null,
                totalReviews = null,
                tags = null,
            ).toSearchExpertUi()

        assertEquals(null, ui.name)
        assertEquals("", ui.headline)
        assertEquals(null, ui.avatarUrl)
        assertEquals(0.0, ui.rating, 0.0)
        assertEquals(emptyList<String>(), ui.tags)
        assertEquals("", ui.bio)
    }

    @Test
    fun sortOrderAndApiValuesMatchIos() {
        assertEquals(listOf(SearchSort.MostPopular, SearchSort.HighestRated), SearchSort.entries)
        assertEquals("relevance", SearchSort.MostPopular.apiValue)
        assertEquals("rating", SearchSort.HighestRated.apiValue)
    }

    @Test
    fun defaultStateIsTheFirstVisit() {
        val state = SearchExpertUiState()

        assertEquals("", state.query)
        assertEquals("", ALL_CATEGORIES_SLUG)
        assertEquals(ALL_CATEGORIES_SLUG, state.selectedCategorySlug)
        assertEquals(SearchSort.MostPopular, state.sort)
        assertTrue(state.categories.isEmpty())
        assertEquals(SearchResults.Loading, state.results)
        assertFalse(state.isRefreshing)
        assertFalse(state.refreshFailed)
    }
}
