package com.uptrendinvestments.clickmeapp.managers

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.agora.rtc2.IRtcEngineEventHandler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AgoraEngineConfigTest {
    private val app: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun builderCarriesContextAppIdAndHandler() {
        val handler = object : IRtcEngineEventHandler() {}

        val config = buildRtcEngineConfig(app, "app-id-sentinel", handler)

        assertSame(app, config.mContext)
        assertEquals("app-id-sentinel", config.mAppId)
        assertSame(handler, config.mEventHandler)
    }
}
