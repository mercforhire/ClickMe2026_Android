package com.uptrendinvestments.clickmeapp.screens.expertmode.topics.editor

import com.uptrendinvestments.clickmeapp.model.responses.ExpertiseTagItem
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import java.util.Locale

/** The error shown under the price field. */
sealed interface PriceFieldError {
    /** A paid price under the floor, shown live and on Save. */
    data class BelowFloor(
        val floorMinor: Int,
        val currency: String,
    ) : PriceFieldError

    /** Not a price this currency can represent; shown only after Save. */
    data object Invalid : PriceFieldError

    /** The server's `PRICE_BELOW_MINIMUM` message; cleared by the next keystroke. Rendered as plain text. */
    data class Server(
        val message: String?,
    ) : PriceFieldError
}

/** Load state of the expert's profile tags shown in the Expertise section. */
sealed interface TopicTagsLoad {
    data object Loading : TopicTagsLoad

    data object Failed : TopicTagsLoad

    data class Loaded(
        val profileTags: List<ExpertiseTagItem>,
    ) : TopicTagsLoad
}

/** Fields the screen can be asked to focus after a failed validation. */
enum class EditorField { Title, Price }

data class TopicEditorUiState(
    val isEdit: Boolean,
    val title: String = "",
    val titleError: FieldError? = null,
    val description: String = "",
    val durationMins: Int = TopicEditorRules.DEFAULT_DURATION,
    val priceText: String = "",
    val priceError: PriceFieldError? = null,
    val currency: String = TopicPrice.FALLBACK_CURRENCY,
    /** Floor for [currency] in minor units, or null when `/me` has no limits (the client check is skipped). */
    val floorMinor: Int? = null,
    val iconSlug: String? = null,
    val iconOptions: List<String> = TopicEditorRules.CURATED_ICON_SLUGS,
    val tags: TopicTagsLoad = TopicTagsLoad.Loading,
    val profileTags: List<ExpertiseTagItem> = emptyList(),
    val selectedTags: List<ExpertiseTagItem> = emptyList(),
    val tagFilter: String = "",
    val isSaving: Boolean = false,
    val dialog: FieldError? = null,
    val focusTarget: EditorField? = null,
    val finished: Boolean = false,
) {
    /** Selected tags first, then the rest of the profile tags, narrowed by [tagFilter]. */
    val visibleTags: List<ExpertiseTagItem>
        get() = TopicEditorRules.visibleTags(profileTags, selectedTags, tagFilter)

    /** True when no further tag can be selected. */
    val atCap: Boolean
        get() = TopicEditorRules.isAtCap(selectedTags)

    /** Lower-cased ids of the selected tags; compare with a lower-cased tag id. */
    val selectedIds: Set<String>
        get() = selectedTags.map { it.id.lowercase(Locale.ROOT) }.toSet()
}
