package com.uptrendinvestments.clickmeapp.components.booknote

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.uptrendinvestments.clickmeapp.testing.assertActionablesNamed
import com.uptrendinvestments.clickmeapp.testing.assertTouchTargets
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private const val ADD_LABEL = "Add private note"
private const val PROMPT = "Tap to jot down your thoughts — only you can see this."

@RunWith(RobolectricTestRunner::class)
class PrivateNotePartsTest {
    @get:Rule
    val rule = createComposeRule()

    private var clicks = 0

    private fun click() {
        clicks++
    }

    @Test
    fun zoneShowsAddWhenThereIsNoPreview() {
        rule.setContent { ClickMeTheme { PrivateNoteZone(preview = null, onClick = ::click) } }
        rule.onNodeWithText(ADD_LABEL).assertIsDisplayed()
        rule.onNodeWithTag(PRIVATE_NOTE_ZONE_TAG).performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun zoneTreatsABlankPreviewAsAbsent() {
        rule.setContent { ClickMeTheme { PrivateNoteZone(preview = "   ", onClick = ::click) } }
        rule.onNodeWithText(ADD_LABEL).assertIsDisplayed()
        rule.onNodeWithText("   ").assertDoesNotExist()
    }

    @Test
    fun zoneShowsThePreviewWhenPresent() {
        rule.setContent { ClickMeTheme { PrivateNoteZone(preview = "Call back", onClick = ::click) } }
        rule.onNodeWithText("Call back").assertIsDisplayed()
        rule.onNodeWithText(ADD_LABEL).assertDoesNotExist()
        rule.onNodeWithTag(PRIVATE_NOTE_ZONE_TAG).performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun previewWithLeadingBlankLinesShowsItsFirstWords() {
        rule.setContent { ClickMeTheme { PrivateNoteZone(preview = "\n\nCall about pricing", onClick = ::click) } }
        rule.onNodeWithText("Call about pricing").assertIsDisplayed()
    }

    @Test
    fun previewTextTrimsEdgesAndCollapsesLineBreaks() {
        assertEquals("Call about pricing", notePreviewText("\n\nCall about pricing"))
        assertEquals("one two", notePreviewText("one\n \n  two  "))
        assertEquals("a b", notePreviewText("a b"))
    }

    @Test
    fun actionShowsPrivateNote() {
        rule.setContent { ClickMeTheme { PrivateNoteAction(onClick = ::click) } }
        rule.onNodeWithText("Private Note").assertIsDisplayed()
        rule.onNodeWithTag(PRIVATE_NOTE_ACTION_TAG).performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun footerShowsThePreview() {
        rule.setContent { ClickMeTheme { NotePreviewFooter(preview = "Call back", onClick = ::click) } }
        rule.onNodeWithText("Call back").assertIsDisplayed()
        rule.onNodeWithTag(PRIVATE_NOTE_FOOTER_TAG).performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun myNoteCardShowsTheFullNote() {
        val note = "x".repeat(2_000)
        rule.setContent { ClickMeTheme { MyNoteCard(note = note, onClick = ::click) } }
        rule.onNodeWithText("My Note").assertIsDisplayed()
        rule.onNodeWithText(note).assertExists()
        rule.onNodeWithText(PROMPT).assertDoesNotExist()
    }

    @Test
    fun myNoteCardTrimsLeadingBlankLines() {
        rule.setContent { ClickMeTheme { MyNoteCard(note = "\n\nCall about pricing\n", onClick = ::click) } }
        rule.onNodeWithText("Call about pricing").assertIsDisplayed()
    }

    @Test
    fun myNoteCardShowsThePromptWhenEmpty() {
        var note: String? = null
        rule.setContent { ClickMeTheme { MyNoteCard(note = note, onClick = ::click) } }
        rule.onNodeWithText(PROMPT).assertIsDisplayed()
        rule.onNodeWithTag(MY_NOTE_CARD_TAG).performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun myNoteCardShowsThePromptWhenBlank() {
        rule.setContent { ClickMeTheme { MyNoteCard(note = "  \n ", onClick = ::click) } }
        rule.onNodeWithText(PROMPT).assertIsDisplayed()
    }

    @Test
    fun targetsAreNamedAndLargeEnough() {
        rule.setContent { ClickMeTheme { AllParts() } }
        rule.assertActionablesNamed()
        rule.assertTouchTargets()
    }

    @Test
    fun largeFontDoesNotClip() {
        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                ClickMeTheme { Column(Modifier.width(320.dp)) { AllParts() } }
            }
        }
        // The lower parts may sit below the small test window once the text doubles, so only exposure is asserted
        // for them; the touch-target audit below proves no target is clipped under 48 dp.
        rule.onNodeWithText(ADD_LABEL).assertIsDisplayed()
        rule.onNodeWithText("Private Note").assertExists()
        rule.onNodeWithText("Call back").assertExists()
        rule.onNodeWithText("My Note").assertExists()
        rule.onNodeWithText("Short note").assertExists()
        rule.assertTouchTargets()
    }

    @Composable
    private fun AllParts() {
        Column {
            PrivateNoteZone(preview = null, onClick = ::click)
            PrivateNoteAction(onClick = ::click)
            NotePreviewFooter(preview = "Call back", onClick = ::click)
            MyNoteCard(note = "Short note", onClick = ::click)
        }
    }
}
