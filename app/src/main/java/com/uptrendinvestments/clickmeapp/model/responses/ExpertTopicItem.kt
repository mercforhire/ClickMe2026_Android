package com.uptrendinvestments.clickmeapp.model.responses

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * One of the signed-in expert's own topics (`GET /expert/topics`, `data.topics[]`; `POST` and `PATCH`
 * return it as `data.topic`). Ports iOS `ExpertTopicItem`.
 *
 * The server sends `price` only; [hourlyRate] is decoded for tolerance and wins over [price] when a
 * future server adds it. A free topic created without a currency carries `price.currency == ""`, so
 * [storedCurrency] skips blank values.
 */
@JsonClass(generateAdapter = true)
data class ExpertTopicItem(
    val id: String,
    val title: String,
    val description: String? = null,
    @Json(name = "duration_mins") val durationMins: Int? = null,
    @Json(name = "icon_slug") val iconSlug: String? = null,
    val price: TopicPriceDto = TopicPriceDto(),
    @Json(name = "hourly_rate") val hourlyRate: TopicHourlyRateDto? = null,
    @Json(name = "expertise_tags") val expertiseTags: List<ExpertiseTagItem> = emptyList(),
) {
    /** True when the server marks the topic free. */
    val isFree: Boolean get() = price.isFree

    /** Price in minor units; `hourly_rate` wins over `price`, 0 when neither is present. */
    val amountMinor: Long get() = hourlyRate?.amount ?: price.amount ?: 0L

    /** First non-blank trimmed currency from `hourly_rate` then `price`, or null (free topics). */
    val storedCurrency: String?
        get() =
            listOf(hourlyRate?.currency, price.currency)
                .firstNotNullOfOrNull { it?.trim()?.takeIf { value -> value.isNotEmpty() } }
}

/** Optional `hourly_rate` object on a topic: amount in minor units plus ISO currency. */
@JsonClass(generateAdapter = true)
data class TopicHourlyRateDto(
    val amount: Long? = null,
    val currency: String? = null,
)

/** `data` of `POST /expert/topics` (201) and `PATCH /expert/topics/{id}` (200). */
@JsonClass(generateAdapter = true)
data class MyExpertTopicPayload(
    val topic: ExpertTopicItem,
)
