@file:OptIn(ExperimentalCoroutinesApi::class)

package com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking

import androidx.lifecycle.SavedStateHandle
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.model.enums.MeetingType
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.util.TimeZone

class MakeABookingViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val api = FakeClickMeApi()
    private lateinit var previousZone: TimeZone

    private val notFound = """{"status":"error","code":"NOT_FOUND","message":"Expert not found"}"""

    @Before
    fun setUp() {
        previousZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("America/Toronto"))
        api.expertDetailsHandler = { detailsWith(topicDto("t1", isFree = true)) }
        api.expertAvailabilityHandler = { _, _ -> availabilityOf() }
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(previousZone)
    }

    private fun vm(savedState: SavedStateHandle = SavedStateHandle()) = newViewModel(api, savedState)

    // ---- Task 1: topics and initial state ----

    @Test
    fun initialStateComesFromTheKeyAndTheClock() =
        runTest(main.testDispatcher) {
            val vm = vm()
            val s = vm.uiState.value
            assertEquals("e1", s.expertId)
            assertEquals("Ada Lovelace", s.expertName)
            assertEquals("Engineer", s.expertTitle)
            assertNull(s.expertImageUrl)
            assertEquals(LocalDate.of(2026, 10, 22), s.today)
            assertEquals(LocalDate.of(2026, 10, 22), s.selectedDate)
            assertEquals(YearMonth.of(2026, 10), s.displayedMonth)
            assertEquals(MeetingType.InAppVoice, s.meetingType)
            assertEquals("", s.notes)
            assertEquals(Instant.parse(NOW), s.now)
        }

    @Test
    fun firstFreeTopicIsPreselected() =
        runTest(main.testDispatcher) {
            api.expertDetailsHandler = {
                detailsWith(
                    topicDto("t1", isFree = false, amount = 5000, currency = "CAD"),
                    topicDto("t2", isFree = true),
                    topicDto("t3", isFree = true),
                )
            }
            val vm = vm()
            runCurrent()
            val s = vm.uiState.value
            assertEquals(3, s.topicList.size)
            assertEquals("t2", s.selectedTopicId)
        }

    @Test
    fun firstTopicIsTheFallbackWhenNoneIsFree() =
        runTest(main.testDispatcher) {
            api.expertDetailsHandler = {
                detailsWith(
                    topicDto("t1", isFree = false, amount = 5000, currency = "CAD"),
                    topicDto("t2", isFree = false, amount = 6000, currency = "CAD"),
                )
            }
            val vm = vm()
            runCurrent()
            assertEquals("t1", vm.uiState.value.selectedTopicId)
        }

    @Test
    fun noTopicsLeavesNothingSelected() =
        runTest(main.testDispatcher) {
            api.expertDetailsHandler = { detailsWith() }
            val vm = vm()
            runCurrent()
            val s = vm.uiState.value
            assertEquals(TopicsState.Loaded(emptyList()), s.topics)
            assertNull(s.selectedTopicId)
            assertEquals(BookDisabledReason.NoTopic, s.bookDisabledReason)
        }

    @Test
    fun topicsFailureShowsTheServerMessageAndRetryRecovers() =
        runTest(main.testDispatcher) {
            api.expertDetailsHandler = { throw api.httpError(404, notFound) }
            val vm = vm()
            runCurrent()
            assertEquals(TopicsState.Failed(FieldError.Text("Expert not found")), vm.uiState.value.topics)

            api.expertDetailsHandler = { detailsWith(topicDto("t1", isFree = true)) }
            vm.retryTopics()
            assertEquals(TopicsState.Loading, vm.uiState.value.topics)
            runCurrent()
            assertTrue(vm.uiState.value.topics is TopicsState.Loaded)
            assertEquals(2, api.expertDetailsCalls)
        }

    @Test
    fun topicsNetworkFailureShowsTheNetworkCopy() =
        runTest(main.testDispatcher) {
            api.expertDetailsHandler = { throw IOException("offline") }
            val vm = vm()
            runCurrent()
            assertEquals(
                TopicsState.Failed(FieldError.Res(R.string.login_error_network)),
                vm.uiState.value.topics,
            )
        }

    @Test
    fun savedTopicOverridesTheDefaultWhenPresent() =
        runTest(main.testDispatcher) {
            api.expertDetailsHandler = {
                detailsWith(topicDto("t1", isFree = true), topicDto("t2", isFree = true), topicDto("t3", isFree = true))
            }
            val vm = vm(SavedStateHandle(mapOf("booking_topic_id" to "t3")))
            runCurrent()
            assertEquals("t3", vm.uiState.value.selectedTopicId)
        }

    @Test
    fun unknownSavedTopicFallsBackToTheDefault() =
        runTest(main.testDispatcher) {
            api.expertDetailsHandler = {
                detailsWith(
                    topicDto("t1", isFree = false, amount = 100, currency = "CAD"),
                    topicDto("t2", isFree = true),
                )
            }
            val vm = vm(SavedStateHandle(mapOf("booking_topic_id" to "gone")))
            runCurrent()
            assertEquals("t2", vm.uiState.value.selectedTopicId)
        }
}
