@file:OptIn(ExperimentalCoroutinesApi::class)

package com.uptrendinvestments.clickmeapp.screens.call.calling

import com.uptrendinvestments.clickmeapp.managers.CallCenterHarness
import com.uptrendinvestments.clickmeapp.managers.CallDialog
import com.uptrendinvestments.clickmeapp.managers.CallPermissionBroker
import com.uptrendinvestments.clickmeapp.managers.CallStatus
import com.uptrendinvestments.clickmeapp.managers.GuardMessage
import com.uptrendinvestments.clickmeapp.managers.MicPermissionResult
import com.uptrendinvestments.clickmeapp.managers.RestrictedAction
import com.uptrendinvestments.clickmeapp.model.enums.MeetingType
import com.uptrendinvestments.clickmeapp.model.responses.UserProfileData
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.SignupTestHarness
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.time.Duration

class MeetingCallViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    @get:Rule
    val tmp = TemporaryFolder()

    private class Fixture(
        scope: TestScope,
        tmp: TemporaryFolder,
        main: MainDispatcherRule,
    ) {
        val call = CallCenterHarness(scope.backgroundScope)
        val user = SignupTestHarness(scope, tmp, main.testDispatcher)
        val broker = CallPermissionBroker()
        val vm = MeetingCallViewModel(call.center, broker, user.userManager, call.clock)

        /** Keeps the ViewModel's WhileSubscribed flows hot, like a visible overlay would. */
        fun collectAll(scope: CoroutineScope) {
            scope.launch { vm.uiState.collect { } }
            scope.launch { vm.dialog.collect { } }
        }
    }

    @Test
    fun uiState_isNullWithoutASession() =
        runTest(main.testDispatcher) {
            val f = Fixture(this, tmp, main)
            f.collectAll(backgroundScope)
            runCurrent()

            assertNull(f.vm.uiState.value)
            assertNull(f.vm.dialog.value)
            assertFalse(f.vm.isMinimized.value)
        }

    @Test
    fun uiState_connectingThenTimerFollowsTheTick() =
        runTest(main.testDispatcher) {
            val f = Fixture(this, tmp, main)
            f.collectAll(backgroundScope)
            f.call.api.joinCallHandler = { f.call.inAppJoinResponse() }
            f.call.center.startCall(f.call.request())
            runCurrent()

            val first = f.vm.uiState.value
            assertNotNull(first)
            assertEquals(CallStatus.Connecting, first!!.status)
            assertEquals(0L, first.timer.elapsedSeconds)

            f.call.clock.advanceBy(Duration.ofSeconds(1))
            advanceTimeBy(UI_TICK_MS)
            runCurrent()

            val ticked = f.vm.uiState.value!!
            assertEquals(1L, ticked.timer.elapsedSeconds)
        }

    @Test
    fun uiState_takesMyAvatarFromTheCachedProfile() =
        runTest(main.testDispatcher) {
            val f = Fixture(this, tmp, main)
            f.user.api.userProfileHandler = {
                UserProfileData(
                    personalDetails = UserProfileData.PersonalDetails(avatarUrl = "https://cdn.example/me.jpg"),
                    professionalDetails = UserProfileData.ProfessionalDetails(),
                    location = UserProfileData.Location(),
                )
            }
            f.collectAll(backgroundScope)
            f.call.api.joinCallHandler = { f.call.inAppJoinResponse() }
            f.call.center.startCall(f.call.request())
            runCurrent()
            val before = f.vm.uiState.value!!
            assertNull(before.myAvatarUrl)

            f.user.userManager.refreshProfile()
            runCurrent()

            val after = f.vm.uiState.value!!
            assertEquals("https://cdn.example/me.jpg", after.myAvatarUrl)
        }

    @Test
    fun dialog_isTheQueueHeadAndDismissRemovesIt() =
        runTest(main.testDispatcher) {
            val f = Fixture(this, tmp, main)
            f.collectAll(backgroundScope)
            f.call.center.attempt(RestrictedAction.LogOut)
            // No call yet: the guard lets the action through and queues nothing.
            runCurrent()
            assertNull(f.vm.dialog.value)

            with(f.call) { startActiveInAppCall() }
            f.call.center.attempt(RestrictedAction.LogOut)
            runCurrent()

            val head = f.vm.dialog.value
            assertTrue(head is CallDialog.Guard && head.message is GuardMessage.EndCallTo)

            f.vm.dismissDialog()
            runCurrent()

            assertNull(f.vm.dialog.value)
        }

    @Test
    fun permission_resultCompletesTheBrokerRequestAndPendingMirrorsIt() =
        runTest(main.testDispatcher) {
            val f = Fixture(this, tmp, main)
            var result: MicPermissionResult? = null
            backgroundScope.launch { result = f.broker.ensureMicrophone() }
            runCurrent()

            val pending = f.vm.pendingPermission.value
            assertNotNull(pending)

            f.vm.onPermissionResult(pending!!.id, MicPermissionResult.Granted)
            runCurrent()

            assertEquals(MicPermissionResult.Granted, result)
            assertNull(f.vm.pendingPermission.value)
        }

    @Test
    fun minimizeAndExpand_delegateToTheCenter() =
        runTest(main.testDispatcher) {
            val f = Fixture(this, tmp, main)
            with(f.call) { startActiveInAppCall() }

            f.vm.minimize()
            assertTrue(f.call.center.isMinimized.value)
            assertTrue(f.vm.isMinimized.value)

            f.vm.expand()
            assertFalse(f.call.center.isMinimized.value)
        }

    @Test
    fun toggles_delegateToTheEngineThroughTheCenter() =
        runTest(main.testDispatcher) {
            val f = Fixture(this, tmp, main)
            with(f.call) { startActiveInAppCall() }

            f.vm.toggleMute()
            f.vm.toggleSpeaker()

            val session = f.call.center.session.value!!
            assertTrue(session.isMuted)
            assertFalse(session.isSpeakerOn)
        }

    @Test
    fun endCall_delegatesAndPostsEnd() =
        runTest(main.testDispatcher) {
            val f = Fixture(this, tmp, main)
            with(f.call) { startActiveInAppCall() }

            f.vm.endCall()
            runCurrent()

            assertEquals(1, f.call.api.endCallRequests.size)
            assertNull(f.call.center.session.value)
        }

    @Test
    fun onExternalLinkOpened_confirmsOnceAndMinimizes() =
        runTest(main.testDispatcher) {
            val f = Fixture(this, tmp, main)
            f.call.api.joinCallHandler = { f.call.externalJoinResponse("https://zoom.us/j/1") }
            f.call.center.startCall(
                f.call.request(meetingType = MeetingType.SkypeZoom),
            )
            runCurrent()

            f.vm.onExternalLinkOpened()
            runCurrent()

            assertTrue(f.call.center.isMinimized.value)
            val session = f.call.center.session.value!!
            assertTrue(session.hasOpenedExternalLink)
            assertEquals(1, f.call.api.confirmCallConnectionRequests.size)
        }
}
