package com.uptrendinvestments.clickmeapp.network

import com.uptrendinvestments.clickmeapp.model.requests.LoginRequest
import com.uptrendinvestments.clickmeapp.model.responses.ExpertProfileData
import com.uptrendinvestments.clickmeapp.model.responses.LoginData
import com.uptrendinvestments.clickmeapp.model.responses.MeData
import com.uptrendinvestments.clickmeapp.model.responses.MyExpertTopicsPayload
import com.uptrendinvestments.clickmeapp.model.responses.SuccessDataResponse
import com.uptrendinvestments.clickmeapp.model.responses.UserProfileData
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Retrofit surface for the Phase 2 endpoints. Ports iOS ClickMeAPI+Auth, +Me and +UserProfile plus the two
 * expert lookups used by SplashViewModel and LoginView. Other API groups arrive with their features
 * (ROADMAP Phase 2 planning note).
 *
 * Paths are relative (no leading slash) so Retrofit resolves them under the `/api/v1/` base URL. Calls are
 * wrapped by `apiCall` at the repository layer, which maps failures to `ApiException`.
 */
interface ClickMeApi {
    @POST(ApiRequestUrls.LOGIN)
    suspend fun login(
        @Body body: LoginRequest,
    ): SuccessDataResponse<LoginData>

    @GET(ApiRequestUrls.ME)
    suspend fun getMe(): SuccessDataResponse<MeData>

    @GET(ApiRequestUrls.USER_PROFILE)
    suspend fun getUserProfile(): SuccessDataResponse<UserProfileData>

    @GET(ApiRequestUrls.EXPERT_PROFILE)
    suspend fun getExpertProfile(): SuccessDataResponse<ExpertProfileData>

    @GET(ApiRequestUrls.EXPERT_TOPICS)
    suspend fun getMyTopics(): SuccessDataResponse<MyExpertTopicsPayload>
}
