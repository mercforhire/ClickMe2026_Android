package com.uptrendinvestments.clickmeapp.testing

import com.uptrendinvestments.clickmeapp.model.responses.AvailabilityTimeRangeDto
import com.uptrendinvestments.clickmeapp.model.responses.AvailabilityTimezoneDto
import com.uptrendinvestments.clickmeapp.model.responses.ExpertAvailabilityScheduleData
import com.uptrendinvestments.clickmeapp.model.responses.ExpertTopicItem
import com.uptrendinvestments.clickmeapp.model.responses.ExpertiseTagItem
import com.uptrendinvestments.clickmeapp.model.responses.RecurringScheduleDto
import com.uptrendinvestments.clickmeapp.model.responses.TopicPriceDto
import java.time.DayOfWeek

/** A topic as the server returns it (price only). Free topics get amount 0 and an empty currency. */
fun topicItem(
    id: String = "t1",
    title: String = "Tax planning",
    amount: Long = 2500,
    currency: String = "CAD",
    isFree: Boolean = false,
    durationMins: Int? = 60,
    iconSlug: String? = "finance",
    description: String? = null,
    tags: List<ExpertiseTagItem> = emptyList(),
): ExpertTopicItem =
    ExpertTopicItem(
        id = id,
        title = title,
        description = description,
        durationMins = durationMins,
        iconSlug = iconSlug,
        price =
            if (isFree) {
                TopicPriceDto(amount = 0, currency = "", isFree = true)
            } else {
                TopicPriceDto(amount = amount, currency = currency, isFree = false)
            },
        expertiseTags = tags,
    )

/** The expert's own schedule; days absent from [schedule] are empty lists. */
fun availabilityData(
    timezoneId: String = "America/New_York",
    displayName: String = "(GMT-05:00) America/New_York",
    schedule: Map<DayOfWeek, List<Pair<String, String>>> = emptyMap(),
): ExpertAvailabilityScheduleData {
    fun day(d: DayOfWeek) = schedule[d].orEmpty().map { AvailabilityTimeRangeDto(it.first, it.second) }
    return ExpertAvailabilityScheduleData(
        timezone = AvailabilityTimezoneDto(id = timezoneId, displayName = displayName, utcOffset = null),
        recurringSchedule =
            RecurringScheduleDto(
                monday = day(DayOfWeek.MONDAY),
                tuesday = day(DayOfWeek.TUESDAY),
                wednesday = day(DayOfWeek.WEDNESDAY),
                thursday = day(DayOfWeek.THURSDAY),
                friday = day(DayOfWeek.FRIDAY),
                saturday = day(DayOfWeek.SATURDAY),
                sunday = day(DayOfWeek.SUNDAY),
            ),
    )
}
