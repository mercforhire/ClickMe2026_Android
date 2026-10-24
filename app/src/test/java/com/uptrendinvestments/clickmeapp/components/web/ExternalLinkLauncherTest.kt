package com.uptrendinvestments.clickmeapp.components.web

import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ResolveInfo
import android.net.Uri
import androidx.core.net.toUri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class ExternalLinkLauncherTest {
    private val application = RuntimeEnvironment.getApplication()

    private val allowed =
        listOf(
            "https://zoom.us/j/1",
            "http://x.com",
            "skype:abc",
            "zoommtg://x",
            "zoomus://x",
            "msteams://x",
            "ZOOMMTG://x",
            "https://user@zoom.us:443/j/1?pwd=a#b",
        )

    private val rejected =
        listOf(
            "intent://x#Intent;end",
            "javascript:alert(1)",
            "file:///sdcard/a",
            "content://x/y",
            "android-app://com.x",
            "https:///nohost",
            "ftp://x.com",
            "mailto:a@b.c",
            "https:zoom.us",
            "zoom.us/j/1",
            " https://zoom.us/j/1",
        )

    @Test
    fun allowlist_admitsWebAndMeetingAppSchemes() {
        allowed.forEach { assertTrue(it, isAllowedMeetingUri(it.toUri())) }
    }

    @Test
    fun allowlist_rejectsEverythingElse() {
        rejected.forEach { assertFalse(it, isAllowedMeetingUri(it.toUri())) }
    }

    @Test
    fun stringAllowlist_agreesWithTheUriAllowlist() {
        allowed.forEach { assertTrue(it, isAllowedMeetingUrl(it)) }
        rejected.forEach { assertFalse(it, isAllowedMeetingUrl(it)) }
    }

    @Test
    fun intent_onApi35_requiresNonBrowser() {
        val uri = "https://zoom.us/j/1".toUri()
        val intent = buildExternalMeetingIntent(uri, fromActivity = false, sdkInt = 35)

        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals(uri, intent.data)
        assertTrue(intent.hasCategory(Intent.CATEGORY_BROWSABLE))
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_REQUIRE_NON_BROWSER != 0)
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }

    @Test
    fun intent_fromActivity_omitsNewTask() {
        val intent = buildExternalMeetingIntent("https://zoom.us/j/1".toUri(), fromActivity = true, sdkInt = 35)

        assertEquals(0, intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    @Test
    fun intent_belowApi30_omitsRequireNonBrowser() {
        val intent = buildExternalMeetingIntent("https://zoom.us/j/1".toUri(), fromActivity = false, sdkInt = 29)

        assertEquals(0, intent.flags and Intent.FLAG_ACTIVITY_REQUIRE_NON_BROWSER)
    }

    @Test
    @Config(sdk = [29])
    fun intent_onRobolectricSdk29_omitsRequireNonBrowser() {
        val intent = buildExternalMeetingIntent("https://zoom.us/j/1".toUri(), fromActivity = false)

        assertEquals(0, intent.flags and Intent.FLAG_ACTIVITY_REQUIRE_NON_BROWSER)
    }

    @Test
    fun open_disallowedUri_failsWithoutStartingAnything() {
        var failures = 0
        var customTabs = 0

        val opened =
            application.openExternalMeeting("javascript:alert(1)".toUri(), { failures++ }, { _, _ -> customTabs++ })

        assertFalse(opened)
        assertEquals(1, failures)
        assertEquals(0, customTabs)
        assertNull(shadowOf(application).nextStartedActivity)
    }

    @Test
    fun open_unresolvedHttps_fallsBackToCustomTabOnce() {
        shadowOf(application).checkActivities(true)
        val uri = "https://zoom.us/j/1".toUri()
        var failures = 0
        val tabbed = mutableListOf<Uri>()

        val opened = application.openExternalMeeting(uri, { failures++ }, { u, _ -> tabbed += u })

        assertTrue(opened)
        assertEquals(listOf(uri), tabbed)
        assertEquals(0, failures)
    }

    @Test
    fun open_unresolvedAppScheme_failsWithoutCustomTab() {
        shadowOf(application).checkActivities(true)
        var failures = 0
        var customTabs = 0

        val opened =
            application.openExternalMeeting("zoommtg://zoom.us/join".toUri(), { failures++ }, { _, _ -> customTabs++ })

        assertFalse(opened)
        assertEquals(1, failures)
        assertEquals(0, customTabs)
    }

    @Test
    fun open_resolvedApp_startsActivityAndSkipsFallbacks() {
        val uri = "zoommtg://zoom.us/join?confno=1".toUri()
        val resolveInfo =
            ResolveInfo().apply {
                activityInfo =
                    ActivityInfo().apply {
                        packageName = "us.zoom.videomeetings"
                        name = "us.zoom.videomeetings.LauncherActivity"
                    }
            }
        shadowOf(application.packageManager)
            .addResolveInfoForIntent(buildExternalMeetingIntent(uri, fromActivity = false), resolveInfo)
        shadowOf(application).checkActivities(true)
        var failures = 0
        var customTabs = 0

        val opened = application.openExternalMeeting(uri, { failures++ }, { _, _ -> customTabs++ })

        assertTrue(opened)
        assertEquals(0, failures)
        assertEquals(0, customTabs)
        val started = shadowOf(application).nextStartedActivity
        assertNotNull(started)
        assertEquals(Intent.ACTION_VIEW, started.action)
        assertEquals(uri, started.data)
    }

    @Test
    fun open_customTabThatFails_reportsNotOpened() {
        shadowOf(application).checkActivities(true)
        var failures = 0

        val opened =
            application.openExternalMeeting(
                "https://zoom.us/j/1".toUri(),
                { failures++ },
                { _, fail -> fail() },
            )

        assertFalse(opened)
        assertEquals(1, failures)
    }
}
