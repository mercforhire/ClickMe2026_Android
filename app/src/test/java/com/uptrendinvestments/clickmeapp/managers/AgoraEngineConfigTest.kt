package com.uptrendinvestments.clickmeapp.managers

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
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

    @Test
    fun builderUsesCommunicationChannelProfile() {
        val handler = object : IRtcEngineEventHandler() {}

        val config = buildRtcEngineConfig(app, "app-id-sentinel", handler)

        assertEquals(Constants.CHANNEL_PROFILE_COMMUNICATION, config.mChannelProfile)
    }

    @Test
    fun channelMediaOptionsMirrorIosJoinOptions() {
        val options = buildChannelMediaOptions()

        assertEquals(Constants.CHANNEL_PROFILE_COMMUNICATION, options.channelProfile)
        assertEquals(Constants.CLIENT_ROLE_BROADCASTER, options.clientRoleType)
        assertEquals(true, options.publishMicrophoneTrack)
        assertEquals(true, options.autoSubscribeAudio)
        assertNull(options.token)
    }

    private fun frame() = StackTraceElement("com.example.Foo", "bar", "Foo.kt", 42)

    @Test
    fun describeFailureNamesClassAndTopFrameWithoutMessage() {
        val error = NullPointerException("sentinel-message")
        error.stackTrace = arrayOf(frame())

        assertEquals("NullPointerException at com.example.Foo.bar:42", describeEngineFailure(error))
    }

    @Test
    fun describeFailureWithoutFrameIsClassNameOnly() {
        val error = IllegalStateException("sentinel-message")
        error.stackTrace = emptyArray()

        assertEquals("IllegalStateException", describeEngineFailure(error))
    }

    @Test
    fun describeFailureForLinkageErrorOmitsMessage() {
        val error = UnsatisfiedLinkError("sentinel-message")
        error.stackTrace = arrayOf(frame())

        val text = describeEngineFailure(error)

        assertTrue(text, text.startsWith("UnsatisfiedLinkError at "))
        assertFalse(text, text.contains("sentinel-message"))
    }
}
