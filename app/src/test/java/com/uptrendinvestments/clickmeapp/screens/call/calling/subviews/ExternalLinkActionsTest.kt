package com.uptrendinvestments.clickmeapp.screens.call.calling.subviews

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.uptrendinvestments.clickmeapp.screens.call.calling.ExternalMeetingProvider
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
class ExternalLinkActionsTest {
    @get:Rule
    val rule = createComposeRule()

    private val url = "https://zoom.us/j/1"

    private fun show(content: @Composable () -> Unit) {
        rule.setContent { ClickMeTheme { content() } }
    }

    private fun clipboardText(): String? {
        val manager =
            ApplicationProvider.getApplicationContext<Context>().getSystemService(Context.CLIPBOARD_SERVICE)
                as ClipboardManager
        return manager.primaryClip
            ?.getItemAt(0)
            ?.text
            ?.toString()
    }

    @Test
    fun openLabelNamesTheProviderOrFallsBackToOpenMeeting() {
        val labels = mutableMapOf<ExternalMeetingProvider, String>()
        show {
            ExternalMeetingProvider.entries.forEach { labels[it] = externalOpenLabel(it) }
        }
        rule.waitForIdle()
        assertEquals("Open in Skype", labels[ExternalMeetingProvider.Skype])
        assertEquals("Open in Zoom", labels[ExternalMeetingProvider.Zoom])
        assertEquals("Open in Google Meet", labels[ExternalMeetingProvider.GoogleMeet])
        assertEquals("Open in Microsoft Teams", labels[ExternalMeetingProvider.MicrosoftTeams])
        assertEquals("Open Meeting", labels[ExternalMeetingProvider.Unknown])
    }

    @Test
    fun openShowsTheProviderLabelAndCallsOnOpenOnce() {
        var opened = 0
        show { ExternalLinkActions(ExternalMeetingProvider.Zoom, url, onOpen = { opened++ }) }
        rule.onNodeWithTag(EXTERNAL_OPEN_TAG).assertIsEnabled().assertTextEquals("Open in Zoom")
        rule.onNodeWithTag(EXTERNAL_COPY_TAG).assertIsEnabled()
        rule.onNodeWithTag(EXTERNAL_OPEN_TAG).performClick()
        assertEquals(1, opened)
    }

    @Test
    fun unknownProviderShowsOpenMeeting() {
        show { ExternalLinkActions(ExternalMeetingProvider.Unknown, url, onOpen = {}) }
        rule.onNodeWithTag(EXTERNAL_OPEN_TAG).assertTextEquals("Open Meeting")
    }

    @Test
    fun copyPutsTheUrlOnTheClipboardShowsTheChipAndNeverCallsOnOpen() {
        var opened = 0
        show { ExternalLinkActions(ExternalMeetingProvider.Zoom, url, onOpen = { opened++ }) }
        rule.mainClock.autoAdvance = false
        rule.onNodeWithTag(EXTERNAL_COPY_TAG).performClick()
        rule.mainClock.advanceTimeBy(300)

        assertEquals(url, clipboardText())
        rule
            .onNodeWithTag(EXTERNAL_COPIED_TAG)
            .assertTextEquals("Link copied")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        assertEquals(0, opened)
    }

    @Test
    fun theCopiedChipDisappearsAfterTheHold() {
        show { ExternalLinkActions(ExternalMeetingProvider.Zoom, url, onOpen = {}) }
        rule.mainClock.autoAdvance = false
        rule.onNodeWithTag(EXTERNAL_COPY_TAG).performClick()
        rule.mainClock.advanceTimeBy(300)
        rule.onNodeWithTag(EXTERNAL_COPIED_TAG).assertExists()

        rule.mainClock.advanceTimeBy(1_600)
        rule.mainClock.advanceTimeBy(400)
        rule.onNodeWithTag(EXTERNAL_COPIED_TAG).assertDoesNotExist()
    }

    @Test
    fun bothButtonsAreDisabledWithoutAJoinUrl() {
        show { ExternalLinkActions(ExternalMeetingProvider.Zoom, null, onOpen = {}) }
        rule.onNodeWithTag(EXTERNAL_OPEN_TAG).assertIsNotEnabled()
        rule.onNodeWithTag(EXTERNAL_COPY_TAG).assertIsNotEnabled()
    }

    // ---- ExternalStatusText ----

    private val fixed12h = SimpleDateFormat("h:mm a", Locale.US)
    private val tenThirty = Instant.parse("2026-10-24T10:30:00Z")

    private fun status(
        hasOpened: Boolean,
        end: Instant?,
    ) = show {
        ExternalStatusText(
            hasOpened = hasOpened,
            scheduledEnd = end,
            zone = ZoneOffset.UTC,
            timeFormat = fixed12h,
        )
    }

    private fun assertStatus(expected: String) {
        rule
            .onNodeWithTag(EXTERNAL_STATUS_TAG)
            .assertTextEquals(expected)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
    }

    @Test
    fun statusBeforeOpenAsksToOpenTheLink() {
        status(hasOpened = false, end = tenThirty)
        assertStatus("Open the meeting link to start the session.")
    }

    @Test
    fun statusAfterOpenNamesTheScheduledEnd() {
        status(hasOpened = true, end = tenThirty)
        assertStatus(
            "Meeting in progress. The session is scheduled to end at 10:30 AM. Tap End Session when you're done.",
        )
    }

    @Test
    fun statusAfterOpenWithoutAnEndFallsBack() {
        status(hasOpened = true, end = null)
        assertStatus(
            "Meeting in progress — tap End Session when you're done, or leave the session to end automatically.",
        )
    }
}
