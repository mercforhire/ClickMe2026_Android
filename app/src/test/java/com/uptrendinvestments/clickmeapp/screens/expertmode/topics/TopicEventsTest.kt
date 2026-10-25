package com.uptrendinvestments.clickmeapp.screens.expertmode.topics

import com.uptrendinvestments.clickmeapp.testing.topicItem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TopicEventsTest {
    private val topicEvents = TopicEvents()

    @Test
    fun emittedEventIsDeliveredOnceAndConsumed() =
        runTest {
            val event = TopicSavedEvent(TopicSavedKind.Added, topicItem("t1"))
            topicEvents.emit(event)

            assertEquals(event, topicEvents.events.first())
            assertNull(withTimeoutOrNull(100) { topicEvents.events.first() })
        }

    @Test
    fun eventsEmittedBeforeCollectionAreBufferedInOrder() =
        runTest {
            val added = TopicSavedEvent(TopicSavedKind.Added, topicItem("t1"))
            val updated = TopicSavedEvent(TopicSavedKind.Updated, topicItem("t2"))
            topicEvents.emit(added)
            topicEvents.emit(updated)

            assertEquals(listOf(added, updated), topicEvents.events.take(2).toList())
        }

    @Test
    fun clearSessionDataDrainsUnconsumedEvents() =
        runTest {
            topicEvents.emit(TopicSavedEvent(TopicSavedKind.Added, topicItem("t1")))
            topicEvents.emit(TopicSavedEvent(TopicSavedKind.Updated, topicItem("t2")))

            topicEvents.clearSessionData()

            assertNull(withTimeoutOrNull(100) { topicEvents.events.first() })
        }
}
