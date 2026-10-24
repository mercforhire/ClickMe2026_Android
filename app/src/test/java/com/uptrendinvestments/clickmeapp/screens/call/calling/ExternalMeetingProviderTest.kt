package com.uptrendinvestments.clickmeapp.screens.call.calling

import com.uptrendinvestments.clickmeapp.screens.call.calling.ExternalMeetingProvider.GoogleMeet
import com.uptrendinvestments.clickmeapp.screens.call.calling.ExternalMeetingProvider.MicrosoftTeams
import com.uptrendinvestments.clickmeapp.screens.call.calling.ExternalMeetingProvider.Skype
import com.uptrendinvestments.clickmeapp.screens.call.calling.ExternalMeetingProvider.Unknown
import com.uptrendinvestments.clickmeapp.screens.call.calling.ExternalMeetingProvider.Zoom
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExternalMeetingProviderTest {
    private fun assertDetects(
        expected: ExternalMeetingProvider,
        url: String?,
    ) = assertEquals("url=$url", expected, ExternalMeetingProvider.detect(url))

    @Test
    fun nativeSchemes_areTheStrongestSignal() {
        assertDetects(Skype, "skype:live:abc?call")
        assertDetects(Zoom, "zoommtg://zoom.us/join?confno=1")
        assertDetects(Zoom, "zoomus://x")
        assertDetects(MicrosoftTeams, "msteams://l/meetup-join/x")
    }

    @Test
    fun hosts_areMatchedBySubstring() {
        assertDetects(Skype, "https://join.skype.com/abc")
        assertDetects(Zoom, "https://us02web.zoom.us/j/1")
        assertDetects(Zoom, "https://zoom.com/j/1")
        assertDetects(GoogleMeet, "https://meet.google.com/abc-defg-hij")
        assertDetects(MicrosoftTeams, "https://teams.microsoft.com/l/x")
        assertDetects(MicrosoftTeams, "https://teams.live.com/meet/x")
    }

    @Test
    fun substringFallback_matchesIosBehavior() {
        assertDetects(Zoom, "https://example.com/zoom-room")
        assertDetects(Skype, "https://example.com/skype")
        assertDetects(GoogleMeet, "https://example.com/meet.google")
        assertDetects(MicrosoftTeams, "https://example.com/teams.x")
    }

    @Test
    fun unrecognised_isUnknown() {
        assertDetects(Unknown, "https://example.com/room")
        assertDetects(Unknown, null)
        assertDetects(Unknown, "")
        assertDetects(Unknown, "   ")
    }

    @Test
    fun detection_isCaseInsensitive() {
        assertDetects(GoogleMeet, "HTTPS://MEET.GOOGLE.COM/x")
        assertDetects(Zoom, "ZOOMMTG://X")
    }

    @Test
    fun unparseableUrl_stillRunsTheSubstringRules() {
        assertDetects(Zoom, "https://example.com/my zoom room")
        assertDetects(Unknown, "not a url at all")
    }

    @Test
    fun isKnown_isFalseOnlyForUnknown() {
        assertFalse(Unknown.isKnown)
        assertTrue(Skype.isKnown)
        assertTrue(Zoom.isKnown)
        assertTrue(GoogleMeet.isKnown)
        assertTrue(MicrosoftTeams.isKnown)
    }
}
