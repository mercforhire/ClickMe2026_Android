package com.uptrendinvestments.clickmeapp.services.call

import android.content.ComponentName
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import androidx.test.core.app.ApplicationProvider
import com.uptrendinvestments.clickmeapp.MainActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CallManifestTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val pm = context.packageManager

    private val service
        get() = pm.getServiceInfo(ComponentName(context, CallService::class.java), 0)

    @Test
    fun callService_isNotExported() {
        assertFalse(service.exported)
    }

    @Test
    fun callService_isMicrophoneTypeAndSurvivesTaskSwipe() {
        assertTrue(service.foregroundServiceType and ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE != 0)
        assertEquals(0, service.flags and ServiceInfo.FLAG_STOP_WITH_TASK)
    }

    @Test
    fun callPermissionsAreDeclared() {
        val requested =
            pm.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS).requestedPermissions.orEmpty()
        listOf(
            "android.permission.RECORD_AUDIO",
            "android.permission.MODIFY_AUDIO_SETTINGS",
            "android.permission.FOREGROUND_SERVICE",
            "android.permission.FOREGROUND_SERVICE_MICROPHONE",
            "android.permission.POST_NOTIFICATIONS",
            "android.permission.ACCESS_NETWORK_STATE",
        ).forEach { assertTrue("missing $it", requested.contains(it)) }
    }

    @Test
    fun mainActivity_isSingleTop() {
        val info = pm.getActivityInfo(ComponentName(context, MainActivity::class.java), 0)
        assertEquals(ActivityInfo.LAUNCH_SINGLE_TOP, info.launchMode)
    }
}
