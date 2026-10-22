package com.uptrendinvestments.clickmeapp.constants

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.Memory
import com.uptrendinvestments.clickmeapp.testing.MutableClock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import java.time.Duration
import java.time.Instant

class CategoryIconMapTest {
    @Test
    fun ai_isMemory_caseInsensitive() {
        assertSame(Icons.Outlined.Memory, CategoryIconMap.icon("ai"))
        assertSame(Icons.Outlined.Memory, CategoryIconMap.icon("AI"))
    }

    @Test
    fun realEstate_isHome() {
        assertSame(Icons.Outlined.Home, CategoryIconMap.icon("real-estate"))
    }

    @Test
    fun aliases_shareIcons() {
        assertSame(Icons.Outlined.Code, CategoryIconMap.icon("tech"))
        assertSame(Icons.Outlined.Code, CategoryIconMap.icon("technology"))
        assertSame(Icons.Outlined.MedicalServices, CategoryIconMap.icon("health"))
        assertSame(Icons.Outlined.MedicalServices, CategoryIconMap.icon("healthcare"))
    }

    @Test
    fun slugs_matchIosTable() {
        val expected =
            setOf(
                "ai",
                "business",
                "consulting",
                "career",
                "coaching",
                "design",
                "education",
                "engineering",
                "finance",
                "fitness",
                "food",
                "gaming",
                "healthcare",
                "health",
                "language",
                "languages",
                "legal",
                "marketing",
                "music",
                "photography",
                "productivity",
                "psychology",
                "real-estate",
                "science",
                "sports",
                "startup",
                "tech",
                "technology",
                "travel",
                "wellness",
                "writing",
            )
        assertEquals(31, expected.size)
        assertEquals(expected, CategoryIconMap.slugs)
    }

    @Test
    fun unknownSlug_usesGridViewFallback() {
        assertSame(CategoryIconMap.fallback, CategoryIconMap.icon("underwater-basket-weaving"))
        assertSame(Icons.Outlined.GridView, CategoryIconMap.fallback)
    }

    @Test
    fun mutableClock_advances() {
        val start = Instant.parse("2026-10-21T12:00:00Z")
        val clock = MutableClock(start)
        assertEquals(start.toEpochMilli(), clock.millis())
        clock.advanceBy(Duration.ofDays(8))
        assertEquals(start.plus(Duration.ofDays(8)), clock.instant())
    }
}
