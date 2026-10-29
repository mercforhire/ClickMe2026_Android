package com.uptrendinvestments.clickmeapp

import android.content.ComponentName
import android.content.pm.ActivityInfo
import androidx.test.core.app.ApplicationProvider
import com.uptrendinvestments.clickmeapp.screens.expertmode.payouts.ConnectReturnActivity
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** The single portrait policy (CONTEXT D-01, D-03): declared once in the manifest, no recreation workarounds. */
@RunWith(RobolectricTestRunner::class)
class ManifestOrientationTest {
    private fun info(cls: Class<*>): ActivityInfo {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        return context.packageManager.getActivityInfo(ComponentName(context, cls), 0)
    }

    @Test
    fun mainActivityIsPortrait() {
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, info(MainActivity::class.java).screenOrientation)
    }

    @Test
    fun mainActivityHandlesNoConfigChanges() {
        val handled =
            ActivityInfo.CONFIG_ORIENTATION or
                ActivityInfo.CONFIG_SCREEN_SIZE or
                ActivityInfo.CONFIG_UI_MODE or
                ActivityInfo.CONFIG_FONT_SCALE
        assertEquals(0, info(MainActivity::class.java).configChanges and handled)
    }

    @Test
    fun connectReturnActivityHasNoOrientation() {
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
            info(ConnectReturnActivity::class.java).screenOrientation,
        )
    }
}
