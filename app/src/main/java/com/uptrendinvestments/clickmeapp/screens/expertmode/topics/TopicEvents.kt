package com.uptrendinvestments.clickmeapp.screens.expertmode.topics

import com.uptrendinvestments.clickmeapp.managers.SessionDataCleaner
import com.uptrendinvestments.clickmeapp.model.responses.ExpertTopicItem
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Whether a saved topic is new to the list or replaces an existing row. */
enum class TopicSavedKind { Added, Updated }

/** The topic the signed-in expert just saved in the editor. */
data class TopicSavedEvent(
    val kind: TopicSavedKind,
    val topic: ExpertTopicItem,
)

/**
 * One-shot channel that carries a saved topic from the Topic Editor back to the Topics list so the list updates
 * in place (D-07). It replaces a navigation result: the Topics ViewModel under the editor is the only collector.
 *
 * It holds only the topic the signed-in expert just saved. As a [SessionDataCleaner] it is drained when the
 * session ends, so an unconsumed event can never surface in the next expert's session (T-10-16).
 */
@Singleton
class TopicEvents
    @Inject
    constructor() : SessionDataCleaner {
        private val channel = Channel<TopicSavedEvent>(Channel.BUFFERED)

        /** Each event is delivered to exactly one collector, once, in emit order. */
        val events: Flow<TopicSavedEvent> = channel.receiveAsFlow()

        /** Never suspends. */
        fun emit(event: TopicSavedEvent) {
            channel.trySend(event)
        }

        override suspend fun clearSessionData() {
            while (channel.tryReceive().isSuccess) {
                // Drain until empty.
            }
        }
    }
