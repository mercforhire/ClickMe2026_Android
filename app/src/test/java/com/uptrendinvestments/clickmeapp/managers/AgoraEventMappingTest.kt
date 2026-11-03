package com.uptrendinvestments.clickmeapp.managers

import io.agora.rtc2.Constants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AgoraEventMappingTest {
    @Test
    fun connectedMapsToLocalJoined() {
        assertEquals(
            EngineEvent.LocalJoined,
            mapConnectionStateChanged(Constants.CONNECTION_STATE_CONNECTED, 0, false),
        )
    }

    @Test
    fun failedMapsToLocalFailedUnlessLeaving() {
        assertEquals(
            EngineEvent.LocalFailed,
            mapConnectionStateChanged(Constants.CONNECTION_STATE_FAILED, 0, false),
        )
        assertNull(mapConnectionStateChanged(Constants.CONNECTION_STATE_FAILED, 0, true))
    }

    @Test
    fun terminalDisconnectReasonsMapToLocalFailed() {
        listOf(
            Constants.CONNECTION_CHANGED_BANNED_BY_SERVER,
            Constants.CONNECTION_CHANGED_INVALID_TOKEN,
            Constants.CONNECTION_CHANGED_TOKEN_EXPIRED,
        ).forEach { reason ->
            assertEquals(
                EngineEvent.LocalFailed,
                mapConnectionStateChanged(Constants.CONNECTION_STATE_DISCONNECTED, reason, false),
            )
        }
    }

    @Test
    fun disconnectCausedByOurOwnLeaveIsIgnored() {
        assertNull(
            mapConnectionStateChanged(
                Constants.CONNECTION_STATE_DISCONNECTED,
                Constants.CONNECTION_CHANGED_LEAVE_CHANNEL,
                false,
            ),
        )
        assertNull(
            mapConnectionStateChanged(
                Constants.CONNECTION_STATE_DISCONNECTED,
                Constants.CONNECTION_CHANGED_TOKEN_EXPIRED,
                true,
            ),
        )
    }

    @Test
    fun reconnectingAndConnectingChangeNothing() {
        assertNull(mapConnectionStateChanged(Constants.CONNECTION_STATE_RECONNECTING, 0, false))
        assertNull(mapConnectionStateChanged(Constants.CONNECTION_STATE_CONNECTING, 0, false))
    }

    @Test
    fun userOfflineAlwaysMapsToRemoteOffline() {
        listOf(
            Constants.USER_OFFLINE_QUIT,
            Constants.USER_OFFLINE_DROPPED,
            Constants.USER_OFFLINE_BECOME_AUDIENCE,
        ).forEach { reason ->
            assertEquals(EngineEvent.RemoteOffline(9, reason), mapUserOffline(9, reason))
        }
    }

    /** BUG-09: decoding remote audio is the only audio state that proves the peer is present and talking. */
    @Test
    fun remoteAudioDecodingMapsToRemoteJoined() {
        assertEquals(
            EngineEvent.RemoteJoined(9),
            mapRemoteAudioStateChanged(9, Constants.REMOTE_AUDIO_STATE_DECODING),
        )
    }

    @Test
    fun everyOtherRemoteAudioStateMapsToNothing() {
        listOf(
            Constants.REMOTE_AUDIO_STATE_STOPPED,
            Constants.REMOTE_AUDIO_STATE_STARTING,
            Constants.REMOTE_AUDIO_STATE_FROZEN,
            Constants.REMOTE_AUDIO_STATE_FAILED,
        ).forEach { state -> assertNull("state $state", mapRemoteAudioStateChanged(9, state)) }
    }
}
