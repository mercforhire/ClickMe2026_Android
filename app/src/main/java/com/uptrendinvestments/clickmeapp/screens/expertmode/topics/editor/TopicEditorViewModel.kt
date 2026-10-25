package com.uptrendinvestments.clickmeapp.screens.expertmode.topics.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uptrendinvestments.clickmeapp.managers.UserManager
import com.uptrendinvestments.clickmeapp.model.responses.ExpertiseTagItem
import com.uptrendinvestments.clickmeapp.model.responses.MeData
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.screens.expertmode.settings.ExpertiseTagsRepository
import com.uptrendinvestments.clickmeapp.screens.expertmode.topics.ExpertTopicsRepository
import com.uptrendinvestments.clickmeapp.screens.expertmode.topics.TopicEvents
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Topic Editor state (EXP-01, EXP-02). Port of iOS `TopicEditorViewModel` with the Android decisions of Phase 10.
 *
 * - D-07: Edit is seeded from the list item in [TopicEditorKey], so nothing is fetched.
 * - D-08: the profile tags load fresh, a selection that is not on the profile is dropped, at most
 *   [TopicEditorRules.MAX_TAGS] are kept, and tags saved in the Edit Expertise sheet arrive through
 *   [onExpertiseSaved].
 * - D-10: an existing topic keeps its own currency; a new topic follows the `pricing_limits` default currency, else
 *   CAD.
 * - D-12: the floor is read from the in-memory `/me` each time it is needed, never cached across launches. When the
 *   limits are missing at open, `/me` is refreshed once in the background and the form stays usable meanwhile.
 * - D-16: every edit is mirrored to [SavedStateHandle] so it survives rotation and process death. Restored values pass
 *   through the same caps and filters as typed ones. Nothing tracks dirty state.
 *
 * There is no call guard on topic save or delete (UI-SPEC Surface 7: only availability is guarded).
 */
@HiltViewModel(assistedFactory = TopicEditorViewModel.Factory::class)
class TopicEditorViewModel
    @AssistedInject
    constructor(
        @Assisted key: TopicEditorKey,
        private val savedStateHandle: SavedStateHandle,
        private val repository: ExpertTopicsRepository,
        private val tagsRepository: ExpertiseTagsRepository,
        private val userManager: UserManager,
        private val topicEvents: TopicEvents,
    ) : ViewModel() {
        @AssistedFactory
        interface Factory {
            fun create(key: TopicEditorKey): TopicEditorViewModel
        }

        private val seed: TopicEditorSeed? = key.topic
        private val isEdit = seed != null

        private val _uiState = MutableStateFlow(initialState())
        val uiState: StateFlow<TopicEditorUiState> = _uiState.asStateFlow()

        private var tagsJob: Job? = null

        init {
            viewModelScope.launch { userManager.me.collect(::onMeChanged) }
            if (userManager.me.value?.pricingLimits == null) {
                viewModelScope.launch {
                    try {
                        userManager.refreshMe()
                    } catch (_: ApiException) {
                        // The form stays usable without a floor; the server check still applies.
                    }
                }
            }
            loadTags()
        }

        // ---- init ----

        private fun defaultCurrencyOf(me: MeData?): String? = me?.pricingLimits?.minSessionAmount?.defaultCurrency

        private fun initialState(): TopicEditorUiState {
            val me = userManager.me.value
            val currency = TopicPrice.resolveCurrency(seed?.currency, defaultCurrencyOf(me))
            val title = savedStateHandle.get<String>(KEY_TITLE) ?: seed?.title.orEmpty()
            val description = savedStateHandle.get<String>(KEY_DESCRIPTION) ?: seed?.description.orEmpty()
            val duration =
                savedStateHandle.get<Int>(KEY_DURATION)?.let(TopicEditorRules::seedDuration)
                    ?: TopicEditorRules.seedDuration(seed?.durationMins)
            val priceText =
                TopicPrice.filterInput(
                    savedStateHandle.get<String>(KEY_PRICE)
                        ?: seed?.let { TopicPrice.fieldText(it.amountMinor, it.isFree, currency) }.orEmpty(),
                    currency,
                )
            val icon =
                if (savedStateHandle.get<Boolean>(KEY_ICON_SET) == true) {
                    savedStateHandle.get<String>(KEY_ICON)
                } else {
                    seed?.iconSlug
                }
            return TopicEditorUiState(
                isEdit = isEdit,
                title = TopicEditorRules.capTitle(title),
                description = TopicEditorRules.capDescription(description),
                durationMins = duration,
                priceText = priceText,
                currency = currency,
                floorMinor = TopicPrice.floorFor(me?.pricingLimits, currency),
                iconSlug = icon,
                iconOptions = TopicEditorRules.iconOptions(seed?.iconSlug),
                selectedTags = restoredTags(),
                tagFilter = savedStateHandle.get<String>(KEY_TAG_FILTER).orEmpty(),
            )
        }

        private fun restoredTags(): List<ExpertiseTagItem> {
            val ids = savedStateHandle.get<ArrayList<String>>(KEY_TAG_IDS)
            val labels = savedStateHandle.get<ArrayList<String>>(KEY_TAG_LABELS)
            val tags =
                if (ids != null && labels != null && ids.size == labels.size) {
                    ids.indices.map { ExpertiseTagItem(ids[it], labels[it]) }
                } else {
                    seed?.tags.orEmpty().map { ExpertiseTagItem(it.id, it.label) }
                }
            // Same cap and de-duplication as a tap, so a restored list can never exceed what the UI allows.
            return tags
                .distinctBy { it.id.lowercase(Locale.ROOT) }
                .take(TopicEditorRules.MAX_TAGS)
        }

        // ---- /me and the floor ----

        private fun onMeChanged(me: MeData?) {
            _uiState.update { s ->
                val currency =
                    if (isEdit) s.currency else TopicPrice.resolveCurrency(null, defaultCurrencyOf(me))
                val floor = TopicPrice.floorFor(me?.pricingLimits, currency)
                val text = TopicPrice.filterInput(s.priceText, currency)
                if (text != s.priceText) savedStateHandle[KEY_PRICE] = text
                s.copy(
                    currency = currency,
                    floorMinor = floor,
                    priceText = text,
                    priceError =
                        when (s.priceError) {
                            is PriceFieldError.Server, PriceFieldError.Invalid -> s.priceError
                            else -> liveError(text, currency, floor)
                        },
                )
            }
        }

        /** The error shown while typing: only a below-floor paid price (an invalid one shows after Save). */
        private fun liveError(
            text: String,
            currency: String,
            floor: Int?,
        ): PriceFieldError? {
            val parsed = TopicPrice.parse(text, currency) as? PriceParse.Ok ?: return null
            return if (floor != null && TopicPrice.isBelowFloor(parsed.minor, floor)) {
                PriceFieldError.BelowFloor(floor, currency)
            } else {
                null
            }
        }

        /** The floor for [currency] from the in-memory `/me` right now, not a snapshot taken at open. */
        private fun floorNow(currency: String): Int? =
            TopicPrice.floorFor(userManager.me.value?.pricingLimits, currency)

        // ---- edits ----

        private inline fun edit(block: (TopicEditorUiState) -> TopicEditorUiState) {
            if (_uiState.value.isSaving) return
            _uiState.update { block(it) }
        }

        fun onTitleChange(text: String) =
            edit { s ->
                val capped = TopicEditorRules.capTitle(text)
                savedStateHandle[KEY_TITLE] = capped
                s.copy(title = capped, titleError = null)
            }

        fun onDescriptionChange(text: String) =
            edit { s ->
                val capped = TopicEditorRules.capDescription(text)
                savedStateHandle[KEY_DESCRIPTION] = capped
                s.copy(description = capped)
            }

        fun decreaseDuration() = stepDuration(increase = false)

        fun increaseDuration() = stepDuration(increase = true)

        private fun stepDuration(increase: Boolean) =
            edit { s ->
                val next = TopicEditorRules.stepDuration(s.durationMins, increase)
                savedStateHandle[KEY_DURATION] = next
                s.copy(durationMins = next)
            }

        fun onPriceChange(raw: String) =
            edit { s ->
                val text = TopicPrice.filterInput(raw, s.currency)
                val floor = floorNow(s.currency)
                savedStateHandle[KEY_PRICE] = text
                s.copy(priceText = text, floorMinor = floor, priceError = liveError(text, s.currency, floor))
            }

        fun toggleIcon(slug: String) =
            edit { s ->
                val next = TopicEditorRules.toggleIcon(s.iconSlug, slug)
                savedStateHandle[KEY_ICON_SET] = true
                savedStateHandle[KEY_ICON] = next
                s.copy(iconSlug = next)
            }

        fun onTagFilterChange(text: String) =
            edit { s ->
                savedStateHandle[KEY_TAG_FILTER] = text
                s.copy(tagFilter = text)
            }

        fun toggleTag(tag: ExpertiseTagItem) =
            edit { s ->
                val next = TopicEditorRules.toggleTag(s.selectedTags, tag)
                mirrorTags(next)
                s.copy(selectedTags = next)
            }

        // ---- profile tags ----

        fun retryTags() = loadTags()

        private fun loadTags() {
            tagsJob?.cancel()
            _uiState.update { it.copy(tags = TopicTagsLoad.Loading) }
            tagsJob =
                viewModelScope.launch {
                    try {
                        val profile = tagsRepository.profileTags().map { ExpertiseTagItem(it.id, it.label) }
                        applyProfileTags(profile)
                    } catch (_: ApiException) {
                        _uiState.update { it.copy(tags = TopicTagsLoad.Failed) }
                    }
                }
        }

        /** Tags saved in the Edit Expertise sheet become the available tags; selections not on the profile drop. */
        fun onExpertiseSaved(tags: List<ExpertiseTagItem>) = applyProfileTags(tags)

        private fun applyProfileTags(tags: List<ExpertiseTagItem>) {
            _uiState.update { s ->
                val sorted = TopicEditorRules.sortedProfileTags(tags)
                val selected = TopicEditorRules.applyProfileTagsUpdate(s.selectedTags, tags)
                mirrorTags(selected)
                s.copy(
                    tags = TopicTagsLoad.Loaded(sorted),
                    profileTags = sorted,
                    selectedTags = selected,
                )
            }
        }

        private fun mirrorTags(selected: List<ExpertiseTagItem>) {
            savedStateHandle[KEY_TAG_IDS] = ArrayList(selected.map { it.id })
            savedStateHandle[KEY_TAG_LABELS] = ArrayList(selected.map { it.label })
        }

        // ---- save ----

        /** Validates and saves the topic. Implemented in the save-path task. */
        fun save() = Unit

        fun dismissDialog() {
            _uiState.update { it.copy(dialog = null) }
        }

        fun onFocusHandled() {
            _uiState.update { it.copy(focusTarget = null) }
        }

        companion object {
            const val KEY_TITLE = "topic_title"
            const val KEY_DESCRIPTION = "topic_description"
            const val KEY_DURATION = "topic_duration"
            const val KEY_PRICE = "topic_price"
            const val KEY_ICON_SET = "topic_icon_set"
            const val KEY_ICON = "topic_icon"
            const val KEY_TAG_IDS = "topic_tag_ids"
            const val KEY_TAG_LABELS = "topic_tag_labels"
            const val KEY_TAG_FILTER = "topic_tag_filter"
        }
    }
