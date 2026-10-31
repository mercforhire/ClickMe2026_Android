package com.uptrendinvestments.clickmeapp.screens.clientmode.search

import com.uptrendinvestments.clickmeapp.navigation.StackOrigin
import com.uptrendinvestments.clickmeapp.screens.clientmode.expertprofile.ExpertProfileKey
import com.uptrendinvestments.clickmeapp.testing.searchExpertUi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class SearchNavigationTest {
    @Test
    fun toProfileKeyCarriesTheSearchSeed() {
        val key =
            searchExpertUi(
                id = "e1",
                name = "Ada Lovelace",
                headline = "Engineer",
                avatarUrl = "u",
                rating = 4.5,
                tags = listOf("AI"),
            ).toProfileKey("Expert")

        assertEquals(
            ExpertProfileKey("e1", "Ada Lovelace", "Engineer", "u", 4.5, null, listOf("AI"), StackOrigin.Search),
            key,
        )
    }

    @Test
    fun nullNameUsesTheFallback() {
        assertEquals("Expert", searchExpertUi(name = null).toProfileKey("Expert").name)
    }

    @Test
    fun searchKeyNeverEqualsTheExploreKeyForTheSameExpert() {
        val key = searchExpertUi().toProfileKey("Expert")

        assertNotEquals(key, key.copy(origin = StackOrigin.Default))
    }

    @Test
    fun stackOriginAppendsSearchLast() {
        assertEquals(listOf(StackOrigin.Default, StackOrigin.Chats, StackOrigin.Search), StackOrigin.entries)
    }
}
