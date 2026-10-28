@file:Suppress("ktlint:standard:filename")

package com.uptrendinvestments.clickmeapp.model.requests

import com.squareup.moshi.JsonClass

/**
 * `POST /bookings/{id}/reviews` body. The backend `submitReviewSchema` is `.strict()`: `rating` 1 to 5, optional
 * `comment` of 1 to 1000 characters. `tags` exist server side but are out of scope (UI-SPEC), so no tags key is
 * ever sent, and a null [comment] is omitted. [comment] is user text, so [toString] prints the rating only.
 */
@JsonClass(generateAdapter = true)
data class SubmitReviewRequest(
    val rating: Int,
    val comment: String? = null,
) {
    override fun toString(): String = "SubmitReviewRequest(rating=$rating)"
}
