package com.uptrendinvestments.clickmeapp.screens.expertmode.topics.editor

import androidx.navigation3.runtime.NavKey
import com.uptrendinvestments.clickmeapp.model.responses.ExpertTopicItem
import kotlinx.serialization.Serializable

/**
 * Nav3 route to the Topic Editor. A null [topic] is Add; otherwise Edit, seeded from the list item the expert
 * tapped so the editor never fetches (there is no `GET /expert/topics/:id`). The seed holds only the expert's own
 * topic fields, no tokens or secrets, so it is safe in the serialized back stack.
 */
@Serializable
data class TopicEditorKey(
    val topic: TopicEditorSeed? = null,
) : NavKey {
    companion object {
        fun newTopic(): TopicEditorKey = TopicEditorKey()

        fun forTopic(item: ExpertTopicItem): TopicEditorKey =
            TopicEditorKey(
                TopicEditorSeed(
                    id = item.id,
                    title = item.title,
                    description = item.description,
                    durationMins = item.durationMins,
                    amountMinor = item.amountMinor,
                    isFree = item.isFree,
                    currency = item.storedCurrency,
                    iconSlug = item.iconSlug,
                    tags = item.expertiseTags.map { TopicEditorSeedTag(it.id, it.label) },
                ),
            )
    }
}

/** The topic being edited, copied from the list item. [currency] is null for a free topic stored without one. */
@Serializable
data class TopicEditorSeed(
    val id: String,
    val title: String,
    val description: String?,
    val durationMins: Int?,
    val amountMinor: Long,
    val isFree: Boolean,
    val currency: String?,
    val iconSlug: String?,
    val tags: List<TopicEditorSeedTag>,
)

@Serializable
data class TopicEditorSeedTag(
    val id: String,
    val label: String,
)
