package com.uptrendinvestments.clickmeapp.model.responses

import com.squareup.moshi.JsonClass

/**
 * `GET /expert/topics` payload. Phase 10 replaces the element type with `ExpertTopicItem`;
 * launch logic needs only `topics.isEmpty()` (iOS `expertHasTopics`).
 */
@JsonClass(generateAdapter = true)
data class MyExpertTopicsPayload(
    val topics: List<Map<String, Any?>>,
)
