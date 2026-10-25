package com.uptrendinvestments.clickmeapp.screens.expertmode.settings

import com.uptrendinvestments.clickmeapp.model.requests.ExpertTagSelection
import com.uptrendinvestments.clickmeapp.model.requests.UpdateExpertTagsRequest
import com.uptrendinvestments.clickmeapp.model.responses.ExpertTag
import com.uptrendinvestments.clickmeapp.model.responses.ExpertiseTagItem
import com.uptrendinvestments.clickmeapp.network.ClickMeApi
import com.uptrendinvestments.clickmeapp.network.apiCall
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The expert's profile expertise tags and the tag catalog. Mirrors iOS `Expert Mode/Settings`; the later
 * expert settings phase reuses it. Every call throws [com.uptrendinvestments.clickmeapp.network.ApiException] only.
 *
 * Stateless by design: no per-user cache, since nothing clears singletons on logout (threat T-10-16).
 */
@Singleton
class ExpertiseTagsRepository
    @Inject
    constructor(
        private val api: ClickMeApi,
    ) {
        /** The tags currently on the expert's profile (`GET /expert/profile`), empty when the server sends none. */
        suspend fun profileTags(): List<ExpertTag> = apiCall { api.getExpertProfile() }.data.expertiseTags.orEmpty()

        /** The full tag catalog (`GET /meta/expertise-tags`), sorted case-insensitively by label. */
        suspend fun catalog(): List<ExpertiseTagItem> =
            apiCall { api.getExpertiseTags() }
                .data.tags
                .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })

        /**
         * `PATCH /expert/profile`: replaces the profile tags with [ordered]. The first tag is primary and ids are
         * lower-cased because server lookups are case-sensitive (T-10-18, as iOS does).
         */
        suspend fun saveProfileTags(ordered: List<ExpertiseTagItem>) {
            val selections =
                ordered.mapIndexed { index, tag ->
                    ExpertTagSelection(tagId = tag.id.lowercase(Locale.ROOT), isPrimary = index == 0)
                }
            apiCall { api.updateExpertProfileTags(UpdateExpertTagsRequest(selections)) }
        }
    }
