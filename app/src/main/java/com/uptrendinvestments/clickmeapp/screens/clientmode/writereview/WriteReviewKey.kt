package com.uptrendinvestments.clickmeapp.screens.clientmode.writereview

import androidx.navigation3.runtime.NavKey
import com.uptrendinvestments.clickmeapp.navigation.StackOrigin
import kotlinx.serialization.Serializable

/** Which screen opened Write Review, and so which one consumes the post-submit hand-off. */
@Serializable
enum class ReviewOpener { BookingSummary, MyBookings }

/**
 * Nav3 route to Write Review (client only, D-19).
 *
 * [expertName] and [expertAvatarUrl] are a display seed so the header renders before the context call returns (iOS
 * `seedExpertName`, `seedExpertImageURL`). [opener] says which screen consumes the post-submit hand-off. [origin] is
 * passed through from `BookingSummaryKey.origin`, so a Chats-stack summary pushes a distinct review key.
 */
@Serializable
data class WriteReviewKey(
    val bookingId: String,
    val expertName: String? = null,
    val expertAvatarUrl: String? = null,
    val opener: ReviewOpener = ReviewOpener.BookingSummary,
    val origin: StackOrigin = StackOrigin.Default,
) : NavKey
