package com.uptrendinvestments.clickmeapp.screens.call.calling

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.managers.CallCenterHarness
import com.uptrendinvestments.clickmeapp.managers.CallDialog
import com.uptrendinvestments.clickmeapp.managers.CallPermissionBroker
import com.uptrendinvestments.clickmeapp.managers.CallStatus
import com.uptrendinvestments.clickmeapp.managers.GuardMessage
import com.uptrendinvestments.clickmeapp.managers.MicPermissionResult
import com.uptrendinvestments.clickmeapp.managers.PendingMicPermission
import com.uptrendinvestments.clickmeapp.managers.RestrictedAction
import com.uptrendinvestments.clickmeapp.managers.callTimerState
import com.uptrendinvestments.clickmeapp.model.enums.ConnectionType
import com.uptrendinvestments.clickmeapp.screens.call.calling.subviews.CALL_CONTROL_CHAT_TAG
import com.uptrendinvestments.clickmeapp.screens.call.calling.subviews.CALL_CONTROL_MUTE_TAG
import com.uptrendinvestments.clickmeapp.screens.call.calling.subviews.CALL_CONTROL_SPEAKER_TAG
import com.uptrendinvestments.clickmeapp.screens.call.calling.subviews.CALL_END_BUTTON_TAG
import com.uptrendinvestments.clickmeapp.screens.call.calling.subviews.EXTERNAL_OPEN_TAG
import com.uptrendinvestments.clickmeapp.screens.call.calling.subviews.MEETING_CALL_PILL_TAG
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.screens.splash.SPLASH_SCREEN_TAG
import com.uptrendinvestments.clickmeapp.screens.splash.SplashScreen
import com.uptrendinvestments.clickmeapp.testing.SignupTestHarness
import com.uptrendinvestments.clickmeapp.testing.testChatEntryViewModel
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class CallOverlayHostTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val tmp = TemporaryFolder()

    private var callFixture: CallFixture? = null

    @After
    fun tearDownCallFixture() {
        callFixture?.close()
    }

    private val start = Instant.parse("2026-10-24T10:00:00Z")
    private val end = start.plusSeconds(1800)

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

    private fun external() =
        inApp().copy(
            connectionType = ConnectionType.SkypeZoom,
            externalJoinUrl = "https://zoom.us/j/123",
            provider = ExternalMeetingProvider.Zoom,
        )

    /** Recording state for one test: what the overlay is shown with and what the callbacks did. */
    private class Probe {
        var state by mutableStateOf<MeetingCallUiState?>(null)
        var minimized by mutableStateOf(false)
        var dialog by mutableStateOf<CallDialog?>(null)
        var contentLabel by mutableStateOf("Underlying content")
        var minimizeCalls = 0
        var expandCalls = 0
        var endCalls = 0
        var openExternalCalls = 0
        var dismissCalls = 0
        var innerBack = 0
        var contentClicks = 0
        var permissionResults = mutableListOf<Pair<Long, MicPermissionResult>>()
        var pending by mutableStateOf<PendingMicPermission?>(null)
        var chatCalls = 0
        var chatError by mutableStateOf<FieldError?>(null)
        var chatErrorDismissals = 0
    }

    private fun show(probe: Probe) {
        rule.setContent {
            ClickMeTheme {
                CallOverlayContent(
                    state = probe.state,
                    isMinimized = probe.minimized,
                    dialog = probe.dialog,
                    pendingPermission = probe.pending,
                    onMinimize = {
                        probe.minimizeCalls++
                        probe.minimized = true
                    },
                    onExpand = {
                        probe.expandCalls++
                        probe.minimized = false
                    },
                    onToggleMute = {},
                    onToggleSpeaker = {},
                    onEndCall = { probe.endCalls++ },
                    onOpenExternal = { probe.openExternalCalls++ },
                    onDismissDialog = {
                        probe.dismissCalls++
                        probe.dialog = null
                    },
                    onPermissionResult = { id, result -> probe.permissionResults += id to result },
                    onChat = { probe.chatCalls++ },
                    chatError = probe.chatError,
                    onDismissChatError = {
                        probe.chatErrorDismissals++
                        probe.chatError = null
                    },
                ) {
                    BackHandler { probe.innerBack++ }
                    Box(Modifier.fillMaxSize().clickable { probe.contentClicks++ }) {
                        Text(probe.contentLabel)
                    }
                }
            }
        }
    }

    private fun pressBack() {
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.waitForIdle()
    }

    @Test
    fun noCall_showsOnlyTheContent() {
        val probe = Probe()
        show(probe)

        rule.onNodeWithText("Underlying content").assertIsDisplayed()
        rule.onNodeWithTag(MEETING_CALL_PILL_TAG).assertDoesNotExist()
        rule.onNodeWithTag(CALL_OVERLAY_LAYER_TAG).assertDoesNotExist()
        rule.onNodeWithTag(MEETING_CALL_SCREEN_TAG).assertDoesNotExist()
    }

    @Test
    fun expanded_showsTheLayerAndHidesTheContentFromAccessibility() {
        val probe = Probe()
        show(probe)

        probe.state = inApp()
        rule.waitForIdle()

        rule.onNodeWithTag(CALL_OVERLAY_LAYER_TAG).assertIsDisplayed()
        rule.onNodeWithTag(MEETING_CALL_SCREEN_TAG).assertIsDisplayed()
        rule.onNodeWithText("Underlying content").assertDoesNotExist()
        rule.onNodeWithTag(MEETING_CALL_PILL_TAG).assertDoesNotExist()
    }

    @Test
    fun expandedCallDrawsAboveTheSplash() {
        val state = mutableStateOf<MeetingCallUiState?>(null)
        rule.setContent {
            ClickMeTheme {
                CallOverlayContent(
                    state = state.value,
                    isMinimized = false,
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
                    onChat = {},
                    chatError = null,
                    onDismissChatError = {},
                ) {
                    SplashScreen(onComposed = {})
                }
            }
        }
        rule.onNodeWithText("Book time with the best minds.", useUnmergedTree = true).assertExists()

        state.value = inApp()
        rule.waitForIdle()

        rule.onNodeWithTag(CALL_OVERLAY_LAYER_TAG).assertIsDisplayed()
        rule.onNodeWithTag(MEETING_CALL_SCREEN_TAG).assertIsDisplayed()
        rule.onNodeWithTag(SPLASH_SCREEN_TAG).assertDoesNotExist()
    }

    @Test
    fun expanded_layerSwallowsTouchesAimedAtTheContent() {
        val probe = Probe()
        show(probe)
        probe.state = inApp()
        rule.waitForIdle()

        rule.onNodeWithTag(CALL_OVERLAY_LAYER_TAG).performTouchInput { click(center) }
        rule.waitForIdle()

        assertEquals(0, probe.contentClicks)
        assertEquals(0, probe.endCalls)
    }

    @Test
    fun back_whileExpanded_minimizesBeforeAnyContentHandlerAndNeverEndsTheCall() {
        val probe = Probe()
        show(probe)
        // The content composed its own BackHandler first; the call expands afterwards (Pitfall 2).
        probe.state = inApp()
        rule.waitForIdle()

        pressBack()

        assertEquals(1, probe.minimizeCalls)
        assertEquals(0, probe.innerBack)
        assertEquals(0, probe.endCalls)
        rule.onNodeWithTag(MEETING_CALL_PILL_TAG).assertIsDisplayed()
    }

    @Test
    fun back_whileMinimized_belongsToTheContent() {
        val probe = Probe()
        show(probe)
        probe.state = inApp()
        probe.minimized = true
        rule.waitForIdle()

        pressBack()

        assertEquals(1, probe.innerBack)
        assertEquals(0, probe.minimizeCalls)
        assertEquals(0, probe.endCalls)
    }

    @Test
    fun minimized_showsThePillAndTheAccessibleContent_andTapExpands() {
        val probe = Probe()
        show(probe)
        probe.state = inApp()
        probe.minimized = true
        rule.waitForIdle()

        rule.onNodeWithTag(MEETING_CALL_PILL_TAG).assertIsDisplayed()
        rule.onNodeWithText("Underlying content").assertIsDisplayed()
        rule.onNodeWithTag(CALL_OVERLAY_LAYER_TAG).assertDoesNotExist()

        rule.onNodeWithTag(MEETING_CALL_PILL_TAG).performClick()
        rule.waitForIdle()

        assertEquals(1, probe.expandCalls)
        rule.onNodeWithTag(CALL_OVERLAY_LAYER_TAG).assertIsDisplayed()
    }

    @Test
    fun pill_survivesAContentSwap() {
        val probe = Probe()
        show(probe)
        probe.state = inApp()
        probe.minimized = true
        rule.waitForIdle()

        probe.contentLabel = "Other tab"
        rule.waitForIdle()

        rule.onNodeWithText("Other tab").assertIsDisplayed()
        rule.onNodeWithTag(MEETING_CALL_PILL_TAG).assertIsDisplayed()
    }

    @Test
    fun guardDialog_showsTheCopyAndOkDismisses() {
        val probe = Probe()
        show(probe)
        probe.state = inApp()
        probe.dialog = CallDialog.Guard(GuardMessage.EndCallTo(RestrictedAction.LogOut))
        rule.waitForIdle()

        rule.onNodeWithText("End the call to log out.").assertIsDisplayed()
        // The dialog is drawn above the expanded layer, which stays composed beneath it.
        rule.onNodeWithTag(CALL_OVERLAY_LAYER_TAG).assertExists()

        rule.onNodeWithText("OK").performClick()
        rule.waitForIdle()

        assertEquals(1, probe.dismissCalls)
        assertNull(probe.dialog)
    }

    @Test
    fun externalOpen_callsTheCallback() {
        val probe = Probe()
        show(probe)
        probe.state = external()
        rule.waitForIdle()

        rule.onNodeWithTag(EXTERNAL_OPEN_TAG).performClick()

        assertEquals(1, probe.openExternalCalls)
    }

    @Test
    fun endCallButton_callsOnEndCall() {
        val probe = Probe()
        show(probe)
        probe.state = inApp()
        rule.waitForIdle()

        rule.onNodeWithTag(CALL_END_BUTTON_TAG).performClick()

        assertEquals(1, probe.endCalls)
    }

    @Test
    fun chatTapCallsOnChat() {
        val probe = Probe()
        show(probe)
        probe.state = inApp()
        rule.waitForIdle()

        rule.onNodeWithTag(CALL_CONTROL_CHAT_TAG).performClick()

        assertEquals(1, probe.chatCalls)
    }

    @Test
    fun chatErrorShowsDialogOverCall() {
        val probe = Probe()
        show(probe)
        probe.state = inApp()
        rule.waitForIdle()

        probe.chatError = FieldError.Res(R.string.chat_entry_error_unavailable)
        rule.waitForIdle()

        rule.onNodeWithText("Couldn't open chat").assertExists()
        rule.onNodeWithText("This user is unavailable.").assertExists()
        rule.onNodeWithTag(MEETING_CALL_SCREEN_TAG).assertExists()
        rule.onNodeWithText("OK").performClick()
        rule.waitForIdle()

        assertEquals(1, probe.chatErrorDismissals)
        rule.onNodeWithText("Couldn't open chat").assertDoesNotExist()
    }

    // Phase 15 plan 08: pill traversal and the no-auto-join guarantee (UI-SPEC Flow 3, CONTEXT D-11).

    /** The merged tree in reading order: siblings sort by traversal index, then top, then left. */
    private fun readingOrder(): List<String> {
        val labels = mutableListOf<String>()

        fun name(node: SemanticsNode): String {
            val descriptions = node.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty()
            val texts =
                node.config
                    .getOrNull(SemanticsProperties.Text)
                    .orEmpty()
                    .map { it.text }
            return (descriptions + texts).filter { it.isNotBlank() }.joinToString(" ")
        }

        fun visit(node: SemanticsNode) {
            name(node).takeIf { it.isNotEmpty() }?.let(labels::add)
            node.children
                .sortedWith(
                    compareBy<SemanticsNode>(
                        { it.config.getOrNull(SemanticsProperties.TraversalIndex) ?: 0f },
                        { it.boundsInRoot.top },
                        { it.boundsInRoot.left },
                    ),
                ).forEach(::visit)
        }
        visit(rule.onRoot().fetchSemanticsNode())
        return labels
    }

    private fun showMinimizedOverThreeNodes() {
        val ada = inApp().copy(peerName = "Ada")
        rule.setContent {
            ClickMeTheme {
                CallOverlayContent(
                    state = ada,
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
                    Column(Modifier.fillMaxSize()) {
                        Text("Alpha")
                        Text("Bravo")
                        Text("Charlie")
                    }
                }
            }
        }
        rule.waitForIdle()
    }

    @Test
    fun pillIsFirstInTraversal() {
        showMinimizedOverThreeNodes()

        val order = readingOrder()
        assertTrue("pill must come first in $order", order.first().contains("Return to call with Ada"))
        assertTrue(order.indexOf("Alpha") < order.indexOf("Bravo") && order.indexOf("Bravo") < order.indexOf("Charlie"))

        val host = rule.onNodeWithTag(MEETING_CALL_PILL_TAG).fetchSemanticsNode().parent
        assertEquals(true, host?.config?.getOrNull(SemanticsProperties.IsTraversalGroup))
        assertEquals(-1f, host?.config?.getOrNull(SemanticsProperties.TraversalIndex))
    }

    @Test
    fun pillKeepsMergedButton() {
        showMinimizedOverThreeNodes()

        val pill = rule.onNodeWithTag(MEETING_CALL_PILL_TAG)
        pill.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        pill.assertTextContains("05:00")
    }

    /** One app-scoped call center on an eager dispatcher, plus ViewModels created over it (as Hilt would). */
    @OptIn(ExperimentalCoroutinesApi::class)
    private class CallFixture(
        tmp: TemporaryFolder,
    ) {
        private val dispatcher = UnconfinedTestDispatcher()
        val scope = TestScope(dispatcher)

        init {
            Dispatchers.setMain(dispatcher)
        }

        val call = CallCenterHarness(scope.backgroundScope)
        private val user = SignupTestHarness(scope, tmp, dispatcher)

        fun newViewModel() = MeetingCallViewModel(call.center, CallPermissionBroker(), user.userManager, call.clock)

        fun close() = Dispatchers.resetMain()

        /** Everything that could start or join a call or open the microphone. */
        fun sideEffects() =
            listOf(
                call.api.joinCallCalls,
                call.engine.joins.size,
                call.permission.calls,
                call.foreground.starts.size,
            )
    }

    private fun fixture() = CallFixture(tmp).also { callFixture = it }

    @Test
    fun compositionNeverJoins() {
        val f = fixture()
        with(f.call) { f.scope.startActiveInAppCall() }
        val before = f.sideEffects()
        val vm = f.newViewModel()
        val chat = testChatEntryViewModel()

        rule.setContent {
            ClickMeTheme { CallOverlayHost(viewModel = vm, chatEntry = chat) { Text("Underlying content") } }
        }
        rule.waitForIdle()

        rule.onNodeWithTag(MEETING_CALL_SCREEN_TAG).assertIsDisplayed()
        assertEquals(before, f.sideEffects())
    }

    @Test
    fun restorationNeverJoins() {
        val f = fixture()
        with(f.call) { f.scope.startActiveInAppCall() }
        f.call.center.toggleMute()
        f.call.center.toggleSpeaker()
        val speakerState =
            if (f.call.center.session.value
                    ?.isSpeakerOn == true
            ) {
                "On"
            } else {
                "Off"
            }
        var vm = f.newViewModel()
        val chat = testChatEntryViewModel()
        val tester = StateRestorationTester(rule)
        // Read during composition only, so swapping it does not recompose before the save.
        tester.setContent {
            ClickMeTheme { CallOverlayHost(viewModel = vm, chatEntry = chat) { Text("Underlying content") } }
        }
        rule.waitForIdle()
        val before = f.sideEffects()
        rule.onNodeWithText("Connected").assertExists()

        // A recreated Activity gets a fresh ViewModel over the same app-scoped call center.
        vm = f.newViewModel()
        tester.emulateSavedInstanceStateRestore()
        rule.waitForIdle()

        rule.onNodeWithText("Connected").assertExists()
        rule.onNodeWithText("Connecting", substring = true).assertDoesNotExist()
        rule
            .onNodeWithTag(CALL_CONTROL_MUTE_TAG)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Muted"))
        rule
            .onNodeWithTag(CALL_CONTROL_SPEAKER_TAG)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, speakerState))
        assertEquals(before, f.sideEffects())
    }

    @Test
    fun noSessionAfterProcessDeathShowsNoLayer() {
        val f = fixture()
        val vm = f.newViewModel()
        val chat = testChatEntryViewModel()

        rule.setContent {
            ClickMeTheme { CallOverlayHost(viewModel = vm, chatEntry = chat) { Text("Underlying content") } }
        }
        rule.waitForIdle()

        rule.onNodeWithText("Underlying content").assertIsDisplayed()
        rule.onNodeWithTag(CALL_OVERLAY_LAYER_TAG).assertDoesNotExist()
        rule.onNodeWithTag(MEETING_CALL_PILL_TAG).assertDoesNotExist()
        assertEquals(listOf(0, 0, 0, 0), f.sideEffects())
    }
}
