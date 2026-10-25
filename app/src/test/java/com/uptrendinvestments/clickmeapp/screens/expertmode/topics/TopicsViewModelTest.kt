package com.uptrendinvestments.clickmeapp.screens.expertmode.topics

import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.model.responses.MyExpertTopicsPayload
import com.uptrendinvestments.clickmeapp.model.responses.SuccessMessageResponse
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.topicItem
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class TopicsViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val api = FakeClickMeApi()
    private val events = TopicEvents()
    private val t1 = topicItem("t1", title = "Tax planning")
    private val t2 = topicItem("t2", title = "Fundraising")
    private val ok = SuccessMessageResponse(status = "success")

    init {
        api.topicsHandler = { MyExpertTopicsPayload(listOf(t1, t2)) }
        api.deleteTopicHandler = { ok }
    }

    private fun vm() = TopicsViewModel(ExpertTopicsRepository(api), events)

    private fun TopicsViewModel.topics() = (uiState.value.list as TopicsListState.Loaded).topics

    private fun serverError(
        status: Int,
        message: String,
    ) = api.httpError(status, """{"status":"error","message":"$message"}""")

    @Test
    fun initLoadsSkeletonThenList() =
        runTest(main.testDispatcher) {
            val vm = vm()
            assertEquals(TopicsListState.Loading, vm.uiState.value.list)
            runCurrent()

            assertEquals(listOf(t1, t2), vm.topics())
            assertEquals(1, api.topicsCalls)
        }

    @Test
    fun emptyServerListIsLoaded() =
        runTest(main.testDispatcher) {
            api.topicsHandler = { MyExpertTopicsPayload(emptyList()) }
            val vm = vm()
            runCurrent()

            assertEquals(TopicsListState.Loaded(emptyList()), vm.uiState.value.list)
        }

    @Test
    fun loadFailureShowsServerMessageOrNetworkCopy() =
        runTest(main.testDispatcher) {
            api.topicsHandler = { throw serverError(500, "Server down") }
            val vm = vm()
            runCurrent()
            assertEquals(TopicsListState.Failed(FieldError.Text("Server down")), vm.uiState.value.list)

            api.topicsHandler = { throw IOException("offline") }
            vm.retry()
            runCurrent()
            assertEquals(TopicsListState.Failed(FieldError.Res(R.string.login_error_network)), vm.uiState.value.list)
        }

    @Test
    fun retryAfterFailureShowsLoadingThenLoaded() =
        runTest(main.testDispatcher) {
            api.topicsHandler = { throw serverError(500, "Server down") }
            val vm = vm()
            runCurrent()

            api.topicsHandler = { MyExpertTopicsPayload(listOf(t1)) }
            vm.retry()
            assertEquals(TopicsListState.Loading, vm.uiState.value.list)
            runCurrent()
            assertEquals(listOf(t1), vm.topics())
        }

    @Test
    fun refreshTogglesIndicatorAndReplacesList() =
        runTest(main.testDispatcher) {
            val vm = vm()
            runCurrent()
            api.topicsHandler = { MyExpertTopicsPayload(listOf(t2)) }

            vm.refresh()
            assertTrue(vm.uiState.value.isRefreshing)
            runCurrent()

            assertFalse(vm.uiState.value.isRefreshing)
            assertEquals(listOf(t2), vm.topics())
        }

    @Test
    fun failedRefreshKeepsListAndRaisesSnackbar() =
        runTest(main.testDispatcher) {
            val vm = vm()
            runCurrent()
            api.topicsHandler = { throw IOException("offline") }

            vm.refresh()
            runCurrent()

            assertFalse(vm.uiState.value.isRefreshing)
            assertEquals(listOf(t1, t2), vm.topics())
            assertEquals(TopicsSnackbar.RefreshFailed, vm.uiState.value.snackbar)
            vm.onSnackbarShown()
            assertNull(vm.uiState.value.snackbar)
        }

    @Test
    fun onScreenShownReloadsQuietlyWithoutSkeleton() =
        runTest(main.testDispatcher) {
            val vm = vm()
            runCurrent()
            api.topicsHandler = { MyExpertTopicsPayload(listOf(t1, t2, topicItem("t3"))) }

            vm.onScreenShown()
            assertTrue(vm.uiState.value.list is TopicsListState.Loaded)
            runCurrent()

            assertEquals(2, api.topicsCalls)
            assertEquals(3, vm.topics().size)
        }

    @Test
    fun onScreenShownDuringInitialLoadDoesNotLoadTwice() =
        runTest(main.testDispatcher) {
            val vm = vm()
            vm.onScreenShown()
            runCurrent()
            vm.onScreenShown()
            runCurrent()

            // The initial load, plus one quiet reload for the second call once the list is Loaded.
            assertEquals(2, api.topicsCalls)
        }

    @Test
    fun failedQuietReloadChangesNothingAndShowsNoSnackbar() =
        runTest(main.testDispatcher) {
            val vm = vm()
            runCurrent()
            val before = vm.uiState.value
            api.topicsHandler = { throw serverError(500, "Server down") }

            vm.onScreenShown()
            runCurrent()

            assertEquals(before, vm.uiState.value)
            assertNull(vm.uiState.value.snackbar)
        }

    @Test
    fun updatedEventReplacesInPlaceAndReloads() =
        runTest(main.testDispatcher) {
            val vm = vm()
            runCurrent()
            val renamed = t1.copy(title = "Tax planning v2")
            api.topicsHandler = { MyExpertTopicsPayload(listOf(renamed, t2)) }

            events.emit(TopicSavedEvent(TopicSavedKind.Updated, renamed))
            runCurrent()

            assertEquals(listOf(renamed, t2), vm.topics())
            assertEquals(TopicsSnackbar.Updated, vm.uiState.value.snackbar)
            assertEquals(2, api.topicsCalls)
        }

    @Test
    fun updatedEventMergesBeforeTheReloadReturns() =
        runTest(main.testDispatcher) {
            val vm = vm()
            runCurrent()
            val renamed = t1.copy(title = "Tax planning v2")
            val gate = CompletableDeferred<Unit>()
            api.topicsHandler = {
                gate.await()
                MyExpertTopicsPayload(listOf(renamed, t2))
            }

            events.emit(TopicSavedEvent(TopicSavedKind.Updated, renamed))
            runCurrent()

            assertEquals(listOf(renamed, t2), vm.topics())
            gate.complete(Unit)
            runCurrent()
            assertEquals(listOf(renamed, t2), vm.topics())
        }

    @Test
    fun addedEventAppendsAndReloads() =
        runTest(main.testDispatcher) {
            val vm = vm()
            runCurrent()
            val t3 = topicItem("t3", title = "Hiring")
            api.topicsHandler = { MyExpertTopicsPayload(listOf(t1, t2, t3)) }

            events.emit(TopicSavedEvent(TopicSavedKind.Added, t3))
            runCurrent()

            assertEquals(listOf(t1, t2, t3), vm.topics())
            assertEquals(TopicsSnackbar.Added, vm.uiState.value.snackbar)
            assertEquals(2, api.topicsCalls)
        }

    @Test
    fun deleteShowsSpinnerIgnoresSecondDeleteAndRemovesOnSuccess() =
        runTest(main.testDispatcher) {
            val vm = vm()
            runCurrent()
            val gate = CompletableDeferred<Unit>()
            api.deleteTopicHandler = {
                gate.await()
                ok
            }

            vm.delete("t1")
            runCurrent()
            assertEquals("t1", vm.uiState.value.deletingId)

            vm.delete("t2")
            runCurrent()
            assertEquals(1, api.deleteTopicCalls)

            gate.complete(Unit)
            runCurrent()

            assertEquals(listOf(t2), vm.topics())
            assertNull(vm.uiState.value.deletingId)
            assertEquals(TopicsSnackbar.Deleted, vm.uiState.value.snackbar)
        }

    @Test
    fun reloadInFlightCannotBringADeletedTopicBack() =
        runTest(main.testDispatcher) {
            val vm = vm()
            runCurrent()
            val deleteGate = CompletableDeferred<Unit>()
            val reloadGate = CompletableDeferred<Unit>()
            api.deleteTopicHandler = {
                deleteGate.await()
                ok
            }
            api.topicsHandler = {
                reloadGate.await()
                MyExpertTopicsPayload(listOf(t1, t2))
            }

            vm.delete("t1")
            vm.onScreenShown()
            runCurrent()
            deleteGate.complete(Unit)
            runCurrent()
            reloadGate.complete(Unit)
            runCurrent()

            assertEquals(listOf(t2), vm.topics())
        }

    @Test
    fun deleteFailureShowsDialogAndLeavesListUnchanged() =
        runTest(main.testDispatcher) {
            val vm = vm()
            runCurrent()
            api.deleteTopicHandler = { throw serverError(404, "Topic not found") }

            vm.delete("t1")
            runCurrent()

            assertEquals(listOf(t1, t2), vm.topics())
            assertNull(vm.uiState.value.deletingId)
            assertNull(vm.uiState.value.snackbar)
            assertEquals(
                TopicsDialog(R.string.topics_delete_error_title, FieldError.Text("Topic not found")),
                vm.uiState.value.dialog,
            )
            vm.dismissDialog()
            assertNull(vm.uiState.value.dialog)
        }
}
