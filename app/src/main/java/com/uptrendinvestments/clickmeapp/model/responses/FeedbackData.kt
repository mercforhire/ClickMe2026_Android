package com.uptrendinvestments.clickmeapp.model.responses

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.time.Instant

/**
 * `GET /feedback/types` data. The type list comes from the server and is never hard-coded: the iOS preview samples
 * differ from the backend list.
 */
@JsonClass(generateAdapter = true)
data class FeedbackTypesData(
    val types: List<FeedbackTypeItem> = emptyList(),
)

/** One feedback type: the id sent as `type_id`, the label to show, and an optional hint. */
@JsonClass(generateAdapter = true)
data class FeedbackTypeItem(
    val id: String,
    val label: String,
    val description: String? = null,
)

/** `POST /feedback/submit` data (201): the stored submission. */
@JsonClass(generateAdapter = true)
data class FeedbackSubmissionData(
    @Json(name = "submission_id") val submissionId: String,
    @Json(name = "submitted_at") val submittedAt: Instant? = null,
)
