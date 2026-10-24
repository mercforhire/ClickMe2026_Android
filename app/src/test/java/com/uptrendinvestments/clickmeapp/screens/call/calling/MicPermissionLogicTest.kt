package com.uptrendinvestments.clickmeapp.screens.call.calling

import android.Manifest
import com.uptrendinvestments.clickmeapp.managers.MicPermissionResult
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class MicPermissionLogicTest {
    @Test
    fun preRequest_granted_isAlreadyGrantedWhateverTheRationaleFlag() {
        assertEquals(MicPreRequestStep.AlreadyGranted, micPreRequestStep(granted = true, shouldShowRationale = true))
        assertEquals(MicPreRequestStep.AlreadyGranted, micPreRequestStep(granted = true, shouldShowRationale = false))
    }

    @Test
    fun preRequest_notGrantedWithRationale_showsRationale() {
        assertEquals(MicPreRequestStep.ShowRationale, micPreRequestStep(granted = false, shouldShowRationale = true))
    }

    @Test
    fun preRequest_notGrantedWithoutRationale_launchesRequestDirectly() {
        assertEquals(MicPreRequestStep.LaunchRequest, micPreRequestStep(granted = false, shouldShowRationale = false))
    }

    @Test
    fun afterRequest_granted_isGranted() {
        assertEquals(MicPermissionResult.Granted, micResultAfterRequest(granted = true, shouldShowRationaleNow = true))
        assertEquals(MicPermissionResult.Granted, micResultAfterRequest(granted = true, shouldShowRationaleNow = false))
    }

    @Test
    fun afterRequest_deniedWithRationale_isDeniedOnce() {
        assertEquals(
            MicPermissionResult.DeniedOnce,
            micResultAfterRequest(granted = false, shouldShowRationaleNow = true),
        )
    }

    @Test
    fun afterRequest_deniedWithoutRationale_isDeniedPermanently() {
        assertEquals(
            MicPermissionResult.DeniedPermanently,
            micResultAfterRequest(granted = false, shouldShowRationaleNow = false),
        )
    }

    @Test
    fun permissions_api32_micOnly() {
        assertArrayEquals(arrayOf(Manifest.permission.RECORD_AUDIO), permissionsToRequest(32, false))
    }

    @Test
    fun permissions_api33_addsNotificationsWhenNotGranted() {
        assertArrayEquals(
            arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.POST_NOTIFICATIONS),
            permissionsToRequest(33, false),
        )
    }

    @Test
    fun permissions_api33_notificationsAlreadyGranted_micOnly() {
        assertArrayEquals(arrayOf(Manifest.permission.RECORD_AUDIO), permissionsToRequest(33, true))
    }
}
