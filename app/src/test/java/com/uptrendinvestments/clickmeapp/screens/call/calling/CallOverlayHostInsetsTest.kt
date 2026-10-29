package com.uptrendinvestments.clickmeapp.screens.call.calling

import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.uptrendinvestments.clickmeapp.managers.CallStatus
import com.uptrendinvestments.clickmeapp.managers.callTimerState
import com.uptrendinvestments.clickmeapp.model.enums.ConnectionType
import com.uptrendinvestments.clickmeapp.screens.call.calling.subviews.MEETING_CALL_PILL_TAG
import com.uptrendinvestments.clickmeapp.testing.cutoutInsets
import com.uptrendinvestments.clickmeapp.testing.dispatchInsets
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

/** UI-SPEC R5: the navigation root and the minimized pill clear a side display cutout. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class CallOverlayHostInsetsTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val start = Instant.parse("2026-10-24T10:00:00Z")
    private val end = start.plusSeconds(1800)

    private lateinit var view: View
    private var state by mutableStateOf<MeetingCallUiState?>(null)

    private fun inApp() =
        MeetingCallUiState(
            connectionType = ConnectionType.InAppVoice,
            peerName = "Alex Doe",
            peerAvatarUrl = null,
            myAvatarUrl = null,
            topic = "Career advice",
            scheduledStart = start,
            scheduledEnd = end,
            status = CallStatus.Connecting,
            timer = callTimerState(start, end, start.plusSeconds(300)),
            isMuted = false,
            isSpeakerOn = true,
            isEndingCall = false,
            externalJoinUrl = null,
            provider = ExternalMeetingProvider.Unknown,
            hasOpenedExternalLink = false,
            peerId = "p1",
        )

    private fun show() {
        rule.setContent {
            view = LocalView.current
            ClickMeTheme {
                CallOverlayContent(
                    state = state,
                    isMinimized = true,
                    dialog = null,
                    pendingPermission = null,
                    onMinimize = {},
                    onExpand = {},
                    onToggleMute = {},
                    onToggleSpeaker = {},
                    onEndCall = {},
                    onOpenExternal = {},
                    onDismissDialog = {},
                    onPermissionResult = { _, _ -> },
                ) {
                    Box(Modifier.fillMaxSize().testTag(CONTENT))
                }
            }
        }
    }

    private fun leftOf(tag: String): Float {
        val bounds = rule.onNodeWithTag(tag).getUnclippedBoundsInRoot()
        return bounds.left.value
    }

    @Test
    fun navigationContentClearsLeftCutout() {
        show()

        rule.dispatchInsets(view, cutoutInsets(leftPx = 40, rightPx = 0))

        assertEquals(40f, leftOf(CALL_OVERLAY_CONTENT_TAG), 0.5f)
        assertEquals(40f, leftOf(CONTENT), 0.5f)
    }

    @Test
    fun pillClearsLeftCutout() {
        show()
        state = inApp()
        rule.waitForIdle()

        rule.dispatchInsets(view, cutoutInsets(leftPx = 40, rightPx = 40))

        val pill = rule.onNodeWithTag(MEETING_CALL_PILL_TAG).getUnclippedBoundsInRoot()
        assertTrue(pill.left.value >= 40f)
        assertTrue(pill.right.value <= 360f - 40f)
    }

    private companion object {
        const val CONTENT = "inset_content"
    }
}
