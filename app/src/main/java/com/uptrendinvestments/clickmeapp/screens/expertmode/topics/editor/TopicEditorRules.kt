package com.uptrendinvestments.clickmeapp.screens.expertmode.topics.editor

import com.uptrendinvestments.clickmeapp.model.responses.ExpertiseTagItem
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.capNotes
import java.util.Locale

/**
 * Pure form rules for the topic editor, ported from iOS `TopicEditorViewModel` (CONTEXT ported-as-is).
 *
 * Icon slugs are the 24 curated iOS slugs (all in the backend `ICON_SLUGS` enum and in `CategoryIconMap`). Duration
 * is 30..480 minutes in 30 minute steps (the server allows up to 1440, iOS caps at 480). Title and description caps
 * match the server's zod limits. Tags come from the expert's own profile only, and are compared case-insensitively
 * and sent lower-cased.
 */
object TopicEditorRules {
    val CURATED_ICON_SLUGS: List<String> =
        listOf(
            "ai",
            "business",
            "career",
            "coaching",
            "consulting",
            "design",
            "education",
            "engineering",
            "finance",
            "fitness",
            "healthcare",
            "language",
            "legal",
            "marketing",
            "music",
            "photography",
            "productivity",
            "psychology",
            "science",
            "startup",
            "tech",
            "travel",
            "wellness",
            "writing",
        )

    const val MIN_DURATION = 30
    const val MAX_DURATION = 480
    const val DURATION_STEP = 30
    const val DEFAULT_DURATION = 60
    const val MAX_TAGS = 3
    const val TITLE_MAX = 200
    const val DESCRIPTION_MAX = 2000

    /** Curated slugs, preceded by the topic's current slug when it is non-blank and not curated. */
    fun iconOptions(currentSlug: String?): List<String> {
        val current = currentSlug?.takeIf { it.isNotBlank() }
        return if (current != null && current !in CURATED_ICON_SLUGS) {
            listOf(current) + CURATED_ICON_SLUGS
        } else {
            CURATED_ICON_SLUGS
        }
    }

    /** Tapping the selected icon clears it; tapping another selects it. */
    fun toggleIcon(
        current: String?,
        tapped: String,
    ): String? = if (current == tapped) null else tapped

    /** Accessibility name used in "{label} icon". */
    fun iconLabel(slug: String): String =
        if (slug == "ai") {
            "AI"
        } else {
            slug.split('-').filter { it.isNotEmpty() }.joinToString(" ") { word ->
                word.replaceFirstChar { it.titlecase(Locale.ROOT) }
            }
        }

    /** Seeds the duration from a stored value: nearest step (ties up), clamped to the range; null is the default. */
    fun seedDuration(mins: Int?): Int {
        if (mins == null) return DEFAULT_DURATION
        val rounded = (mins + DURATION_STEP / 2) / DURATION_STEP * DURATION_STEP
        return rounded.coerceIn(MIN_DURATION, MAX_DURATION)
    }

    fun stepDuration(
        current: Int,
        increase: Boolean,
    ): Int = (if (increase) current + DURATION_STEP else current - DURATION_STEP).coerceIn(MIN_DURATION, MAX_DURATION)

    fun canDecrease(current: Int): Boolean = current > MIN_DURATION

    fun canIncrease(current: Int): Boolean = current < MAX_DURATION

    fun capTitle(text: String): String = capNotes(text, TITLE_MAX)

    fun capDescription(text: String): String = capNotes(text, DESCRIPTION_MAX)

    fun isTitleValid(title: String): Boolean = title.isNotBlank()

    fun sortedProfileTags(tags: List<ExpertiseTagItem>): List<ExpertiseTagItem> =
        tags.sortedBy { it.label.lowercase(Locale.ROOT) }

    /**
     * Selected tags first (selection order), then the remaining profile tags sorted by label. A non-blank [filter]
     * is a case-insensitive substring match on the label; selected tags stay visible while filtering.
     */
    fun visibleTags(
        profileTags: List<ExpertiseTagItem>,
        selected: List<ExpertiseTagItem>,
        filter: String,
    ): List<ExpertiseTagItem> {
        val query = filter.trim().lowercase(Locale.ROOT)
        val rest =
            sortedProfileTags(profileTags).filter { tag ->
                !contains(selected, tag) && (query.isEmpty() || tag.label.lowercase(Locale.ROOT).contains(query))
            }
        return selected + rest
    }

    /** Adds in tap order up to [MAX_TAGS] (a further add is ignored); toggling a selected tag removes it. */
    fun toggleTag(
        selected: List<ExpertiseTagItem>,
        tag: ExpertiseTagItem,
    ): List<ExpertiseTagItem> =
        when {
            contains(selected, tag) -> selected.filterNot { sameId(it, tag) }
            selected.size >= MAX_TAGS -> selected
            else -> selected + tag
        }

    fun isAtCap(selected: List<ExpertiseTagItem>): Boolean = selected.size >= MAX_TAGS

    /** Drops selected tags that are no longer on the expert's profile. */
    fun applyProfileTagsUpdate(
        selected: List<ExpertiseTagItem>,
        profileTags: List<ExpertiseTagItem>,
    ): List<ExpertiseTagItem> = selected.filter { contains(profileTags, it) }

    /** Tag ids as sent on the wire: lower-case (the server compares stored lower-case strings). */
    fun wireTagIds(selected: List<ExpertiseTagItem>): List<String> = selected.map { it.id.lowercase(Locale.ROOT) }

    private fun sameId(
        a: ExpertiseTagItem,
        b: ExpertiseTagItem,
    ): Boolean = a.id.equals(b.id, ignoreCase = true)

    private fun contains(
        tags: List<ExpertiseTagItem>,
        tag: ExpertiseTagItem,
    ): Boolean = tags.any { sameId(it, tag) }
}
