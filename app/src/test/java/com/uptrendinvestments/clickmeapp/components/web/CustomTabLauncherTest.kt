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

@RunWith(RobolectricTestRunner::class)
class CustomTabLauncherTest {
    private val application = RuntimeEnvironment.getApplication()

    @Test
    fun allowlist_admitsHttpAndHttpsWithHost() {
        assertTrue(isAllowedCustomTabUri("https://example.com/a".toUri()))
        assertTrue(isAllowedCustomTabUri("http://example.com".toUri()))
        assertTrue(isAllowedCustomTabUri("HTTPS://EXAMPLE.COM".toUri()))
    }

    @Test
    fun allowlist_rejectsOtherSchemesAndMissingHost() {
        assertFalse(isAllowedCustomTabUri("javascript:alert(1)".toUri()))
        assertFalse(isAllowedCustomTabUri("file:///sdcard/x.html".toUri()))
        assertFalse(isAllowedCustomTabUri("content://x/y".toUri()))
        assertFalse(isAllowedCustomTabUri("intent://x#Intent;end".toUri()))
        assertFalse(isAllowedCustomTabUri("mailto:a@b.c".toUri()))
        assertFalse(isAllowedCustomTabUri("https:///nohost".toUri()))
    }

    @Test
    fun openCustomTab_blockedScheme_failsWithoutStartingActivity() {
        var failures = 0
        application.openCustomTab("javascript:alert(1)".toUri()) { failures++ }

        assertEquals(1, failures)
        assertNull(shadowOf(application).nextStartedActivity)
    }

    @Test
    fun openCustomTab_noResolvingActivity_callsOnFailureOnce() {
        shadowOf(application).checkActivities(true)
        var failures = 0
        application.openCustomTab("https://example.com".toUri()) { failures++ }

        assertEquals(1, failures)
        assertNull(shadowOf(application).nextStartedActivity)
    }

    @Test
    fun openCustomTab_browserRegistered_startsViewIntent() {
        val uri: Uri = "https://example.com".toUri()
        val resolveInfo =
            ResolveInfo().apply {
                activityInfo =
                    ActivityInfo().apply {
                        packageName = "com.example.browser"
                        name = "com.example.browser.BrowserActivity"
                    }
            }
        shadowOf(application.packageManager)
            .addResolveInfoForIntent(Intent(Intent.ACTION_VIEW, uri), resolveInfo)
        shadowOf(application).checkActivities(true)
        var failures = 0

        application.openCustomTab(uri) { failures++ }

        assertEquals(0, failures)
        val started = shadowOf(application).nextStartedActivity
        assertNotNull(started)
        assertEquals(Intent.ACTION_VIEW, started.action)
        assertEquals(uri, started.data)
    }
}
