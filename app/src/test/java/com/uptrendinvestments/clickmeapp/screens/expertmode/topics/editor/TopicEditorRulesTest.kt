package com.uptrendinvestments.clickmeapp.screens.expertmode.topics.editor

import com.uptrendinvestments.clickmeapp.constants.CategoryIconMap
import com.uptrendinvestments.clickmeapp.model.responses.ExpertiseTagItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TopicEditorRulesTest {
    private fun tag(label: String) = ExpertiseTagItem(id = label.lowercase(), label = label)

    private val tax = tag("Tax")
    private val law = tag("Law")
    private val audit = tag("Audit")

    @Test
    fun curatedSlugs_areTheExpectedTwentyFour() {
        val expected =
            (
                "ai business career coaching consulting design education engineering finance fitness " +
                    "healthcare language legal marketing music photography productivity psychology science " +
                    "startup tech travel wellness writing"
            ).split(" ")
        assertEquals(expected, TopicEditorRules.CURATED_ICON_SLUGS)
        assertEquals(24, TopicEditorRules.CURATED_ICON_SLUGS.size)
        assertTrue(TopicEditorRules.CURATED_ICON_SLUGS.all { CategoryIconMap.slugs.contains(it) })
    }

    @Test
    fun iconOptions_prependsUnknownCurrentSlug() {
        val curated = TopicEditorRules.CURATED_ICON_SLUGS
        assertEquals(curated, TopicEditorRules.iconOptions(null))
        assertEquals(curated, TopicEditorRules.iconOptions("tech"))
        assertEquals(curated, TopicEditorRules.iconOptions(""))
        assertEquals(listOf("real-estate") + curated, TopicEditorRules.iconOptions("real-estate"))
    }

    @Test
    fun toggleIcon_clearsSelectedOnly() {
        assertEquals("tech", TopicEditorRules.toggleIcon(null, "tech"))
        assertNull(TopicEditorRules.toggleIcon("tech", "tech"))
        assertEquals("ai", TopicEditorRules.toggleIcon("tech", "ai"))
    }

    @Test
    fun iconLabel_readable() {
        assertEquals("AI", TopicEditorRules.iconLabel("ai"))
        assertEquals("Marketing", TopicEditorRules.iconLabel("marketing"))
        assertEquals("Real Estate", TopicEditorRules.iconLabel("real-estate"))
    }

    @Test
    fun seedDuration_roundsAndClamps() {
        assertEquals(60, TopicEditorRules.seedDuration(null))
        assertEquals(90, TopicEditorRules.seedDuration(90))
        assertEquals(30, TopicEditorRules.seedDuration(20))
        assertEquals(480, TopicEditorRules.seedDuration(1440))
        assertEquals(60, TopicEditorRules.seedDuration(45))
        assertEquals(30, TopicEditorRules.seedDuration(0))
    }

    @Test
    fun stepDuration_staysInRange() {
        assertEquals(90, TopicEditorRules.stepDuration(60, true))
        assertEquals(480, TopicEditorRules.stepDuration(480, true))
        assertEquals(30, TopicEditorRules.stepDuration(30, false))
        assertEquals(30, TopicEditorRules.stepDuration(60, false))
        assertFalse(TopicEditorRules.canDecrease(30))
        assertTrue(TopicEditorRules.canDecrease(60))
        assertFalse(TopicEditorRules.canIncrease(480))
        assertTrue(TopicEditorRules.canIncrease(450))
    }

    @Test
    fun capTitle_andDescription_areSurrogateSafe() {
        assertEquals(200, TopicEditorRules.capTitle("a".repeat(250)).length)
        assertEquals(2000, TopicEditorRules.capDescription("a".repeat(2500)).length)
        val emoji = "😀"
        val capped = TopicEditorRules.capTitle("a".repeat(199) + emoji)
        assertEquals(199, capped.length)
        assertFalse(Character.isHighSurrogate(capped.last()))
    }

    @Test
    fun isTitleValid_requiresNonBlank() {
        assertFalse(TopicEditorRules.isTitleValid("  "))
        assertTrue(TopicEditorRules.isTitleValid(" A "))
    }

    @Test
    fun sortedProfileTags_caseInsensitive() {
        val tags = listOf(tag("tax"), tag("Audit"), tag("Law"))
        assertEquals(listOf("Audit", "Law", "tax"), TopicEditorRules.sortedProfileTags(tags).map { it.label })
    }

    @Test
    fun visibleTags_selectedFirstAndKeptWhileFiltering() {
        val profile = listOf(tax, law, audit)
        assertEquals(listOf(law, audit, tax), TopicEditorRules.visibleTags(profile, listOf(law), ""))
        assertEquals(listOf(law, audit), TopicEditorRules.visibleTags(profile, listOf(law), "au"))
        assertEquals(listOf(audit), TopicEditorRules.visibleTags(profile, emptyList(), "AUD"))
    }

    @Test
    fun toggleTag_capsAtThree() {
        val extra = tag("Extra")
        var selected = emptyList<ExpertiseTagItem>()
        selected = TopicEditorRules.toggleTag(selected, tax)
        selected = TopicEditorRules.toggleTag(selected, law)
        selected = TopicEditorRules.toggleTag(selected, audit)
        assertEquals(listOf(tax, law, audit), selected)
        assertTrue(TopicEditorRules.isAtCap(selected))
        assertEquals(selected, TopicEditorRules.toggleTag(selected, extra))
        assertEquals(listOf(tax, audit), TopicEditorRules.toggleTag(selected, law))
        assertFalse(TopicEditorRules.isAtCap(listOf(tax, law)))
    }

    @Test
    fun applyProfileTagsUpdate_dropsRemovedTags() {
        val a = tag("A")
        val b = tag("B")
        val c = tag("C")
        assertEquals(listOf(b), TopicEditorRules.applyProfileTagsUpdate(listOf(a, b), listOf(b, c)))
    }

    @Test
    fun wireTagIds_areLowerCase() {
        assertEquals(listOf("abc-1"), TopicEditorRules.wireTagIds(listOf(ExpertiseTagItem("ABC-1", "x"))))
    }
}
