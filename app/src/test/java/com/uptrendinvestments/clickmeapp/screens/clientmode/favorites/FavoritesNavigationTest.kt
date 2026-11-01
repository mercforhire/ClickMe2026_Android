package com.uptrendinvestments.clickmeapp.screens.clientmode.favorites

import com.uptrendinvestments.clickmeapp.navigation.StackOrigin
import com.uptrendinvestments.clickmeapp.screens.clientmode.expertprofile.ExpertProfileKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class FavoritesNavigationTest {
    private fun row(name: String? = "Ada Lovelace") =
        FavoriteExpertUi(
            expertId = "e1",
            name = name,
            headline = "Engineer",
            tags = listOf("AI"),
            avatarUrl = "u",
        )

    @Test
    fun toProfileKeyCarriesTheProfileSeed() {
        assertEquals(
            ExpertProfileKey("e1", "Ada Lovelace", "Engineer", "u", null, null, listOf("AI"), StackOrigin.Profile),
            row().toProfileKey("Expert"),
        )
    }

    @Test
    fun nullNameUsesTheFallback() {
        assertEquals("Expert", row(name = null).toProfileKey("Expert").name)
    }

    @Test
    fun profileKeyNeverEqualsTheExploreOrSearchKey() {
        val key = row().toProfileKey("Expert")

        assertNotEquals(key, key.copy(origin = StackOrigin.Default))
        assertNotEquals(key, key.copy(origin = StackOrigin.Search))
    }
}
