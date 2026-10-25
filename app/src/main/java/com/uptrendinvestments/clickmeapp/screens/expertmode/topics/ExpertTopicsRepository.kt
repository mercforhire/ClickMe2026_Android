package com.uptrendinvestments.clickmeapp.screens.expertmode.topics

import com.uptrendinvestments.clickmeapp.model.requests.CreateExpertTopicRequest
import com.uptrendinvestments.clickmeapp.model.requests.UpdateExpertTopicRequest
import com.uptrendinvestments.clickmeapp.model.responses.ExpertTopicItem
import com.uptrendinvestments.clickmeapp.network.ClickMeApi
import com.uptrendinvestments.clickmeapp.network.apiCall
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The signed-in expert's own topics (`/expert/topics`). Mirrors iOS `Expert Mode/Topics`. Every call throws
 * [com.uptrendinvestments.clickmeapp.network.ApiException] only.
 *
 * Stateless by design: nothing is cached, because nothing clears singletons on logout and a cache would show
 * the previous expert's topics to the next one (threat T-10-16). The server scopes every call to the caller.
 */
@Singleton
class ExpertTopicsRepository
    @Inject
    constructor(
        private val api: ClickMeApi,
    ) {
        /** `GET /expert/topics`, in server order. */
        suspend fun list(): List<ExpertTopicItem> = apiCall { api.getMyTopics() }.data.topics

        /** `POST /expert/topics`: returns the created topic. */
        suspend fun create(body: CreateExpertTopicRequest): ExpertTopicItem =
            apiCall { api.createTopic(body) }.data.topic

        /** `PATCH /expert/topics/{id}`: returns the updated topic. */
        suspend fun update(
            topicId: String,
            body: UpdateExpertTopicRequest,
        ): ExpertTopicItem = apiCall { api.updateTopic(topicId, body) }.data.topic

        /** `DELETE /expert/topics/{id}`. */
        suspend fun delete(topicId: String) {
            apiCall { api.deleteTopic(topicId) }
        }
    }
