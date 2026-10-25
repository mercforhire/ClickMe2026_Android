package com.uptrendinvestments.clickmeapp.model.responses

import com.squareup.moshi.JsonClass

/**
 * `GET /expert/topics` payload. Phase 10 typed the element as [ExpertTopicItem]; launch logic still
 * only needs `topics.isEmpty()` (iOS `expertHasTopics`). A malformed topic now fails decoding, which
 * `expertHasTopics` already treats as "no topics".
 */
@JsonClass(generateAdapter = true)
data class MyExpertTopicsPayload(
    val topics: List<ExpertTopicItem> = emptyList(),
)
