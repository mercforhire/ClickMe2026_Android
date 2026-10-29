package com.uptrendinvestments.clickmeapp.screens.expertmode.payouts

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.uptrendinvestments.clickmeapp.MainActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ConnectReturnManifestTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun viewIntent(uri: String) =
        Intent(Intent.ACTION_VIEW, Uri.parse(uri))
            .addCategory(Intent.CATEGORY_BROWSABLE)
            .setPackage(context.packageName)

    private fun resolve(uri: String) = context.packageManager.queryIntentActivities(viewIntent(uri), 0)

    @Test
    fun connectReturnResolvesToTrampoline() {
        listOf("clickme://connect/return", "clickme://connect/refresh").forEach { uri ->
            val resolved = resolve(uri)
            assertEquals(uri, 1, resolved.size)
            assertEquals(ConnectReturnActivity::class.java.name, resolved.single().activityInfo.name)
        }
    }

    @Test
    fun trampolineIsExportedWithoutHistory() {
        val info =
            context.packageManager.getActivityInfo(
                ComponentName(context.packageName, ConnectReturnActivity::class.java.name),
                0,
            )
        assertTrue(info.exported)
        assertTrue(info.flags and ActivityInfo.FLAG_NO_HISTORY != 0)
        assertTrue(info.flags and ActivityInfo.FLAG_EXCLUDE_FROM_RECENTS != 0)
    }

    @Test
    fun mainActivityDoesNotHandleTheDeepLink() {
        listOf("clickme://connect/return", "clickme://connect/refresh").forEach { uri ->
            assertFalse(resolve(uri).any { it.activityInfo.name == MainActivity::class.java.name })
        }
    }

    @Test
    fun httpsDoesNotResolveToTrampoline() {
        assertFalse(
            resolve("https://example.com/connect/return").any {
                it.activityInfo.name == ConnectReturnActivity::class.java.name
            },
        )
    }

    @Test
    fun exactlyOneActivityDeclaresTheClickmeScheme() {
        val components =
            listOf("clickme://connect/return", "clickme://connect/refresh", "clickme://other/x")
                .flatMap { resolve(it) }
                .map { it.activityInfo.name }
                .toSet()
        assertEquals(setOf(ConnectReturnActivity::class.java.name), components)
    }
}
