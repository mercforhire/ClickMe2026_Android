package com.uptrendinvestments.clickmeapp.network

import com.uptrendinvestments.clickmeapp.model.requests.ForgotPasswordRequest
import com.uptrendinvestments.clickmeapp.model.requests.LoginRequest
import com.uptrendinvestments.clickmeapp.model.requests.ResetPasswordRequest
import com.uptrendinvestments.clickmeapp.model.requests.SetupExpertProfileRequest
import com.uptrendinvestments.clickmeapp.model.requests.SignupRequest
import com.uptrendinvestments.clickmeapp.model.requests.VerifyEmailCodeRequest
import com.uptrendinvestments.clickmeapp.model.responses.AvatarUploadData
import com.uptrendinvestments.clickmeapp.model.responses.EmailStatusData
import com.uptrendinvestments.clickmeapp.model.responses.ExpertProfileData
import com.uptrendinvestments.clickmeapp.model.responses.ExpertiseTagsPayload
import com.uptrendinvestments.clickmeapp.model.responses.LanguagesPayload
import com.uptrendinvestments.clickmeapp.model.responses.LoginData
import com.uptrendinvestments.clickmeapp.model.responses.MeData
import com.uptrendinvestments.clickmeapp.model.responses.MyExpertTopicsPayload
import com.uptrendinvestments.clickmeapp.model.responses.SetupExpertProfileData
import com.uptrendinvestments.clickmeapp.model.responses.SignupData
import com.uptrendinvestments.clickmeapp.model.responses.SuccessDataResponse
import com.uptrendinvestments.clickmeapp.model.responses.SuccessMessageResponse
import com.uptrendinvestments.clickmeapp.model.responses.UserProfileData
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part

/**
 * Retrofit surface for the Phase 2 endpoints. Ports iOS ClickMeAPI+Auth, +Me and +UserProfile plus the two
 * expert lookups used by SplashViewModel and LoginView. It also covers the Phase 3 password-recovery
 * endpoints (iOS ClickMeAPI+Auth). Phase 4 adds signup, email verification, expert profile setup, meta
 * lookups and avatar upload/delete. Other API groups arrive with their features (ROADMAP Phase 2 planning
 * note).
 *
 * Paths are relative (no leading slash) so Retrofit resolves them under the `/api/v1/` base URL. Calls are
 * wrapped by `apiCall` at the repository layer, which maps failures to `ApiException`.
 */
interface ClickMeApi {
    @POST(ApiRequestUrls.LOGIN)
    suspend fun login(
        @Body body: LoginRequest,
    ): SuccessDataResponse<LoginData>

    @POST(ApiRequestUrls.FORGOT_PASSWORD)
    suspend fun forgotPassword(
        @Body body: ForgotPasswordRequest,
    ): SuccessMessageResponse

    @POST(ApiRequestUrls.RESET_PASSWORD)
    suspend fun resetPassword(
        @Body body: ResetPasswordRequest,
    ): SuccessMessageResponse

    @GET(ApiRequestUrls.ME)
    suspend fun getMe(): SuccessDataResponse<MeData>

    @GET(ApiRequestUrls.USER_PROFILE)
    suspend fun getUserProfile(): SuccessDataResponse<UserProfileData>

    @GET(ApiRequestUrls.EXPERT_PROFILE)
    suspend fun getExpertProfile(): SuccessDataResponse<ExpertProfileData>

    @GET(ApiRequestUrls.EXPERT_TOPICS)
    suspend fun getMyTopics(): SuccessDataResponse<MyExpertTopicsPayload>

    /** Ports iOS ClickMeAPI+Auth signup. Public path: no bearer. */
    @POST(ApiRequestUrls.SIGNUP)
    suspend fun signup(
        @Body body: SignupRequest,
    ): SuccessDataResponse<SignupData>

    /** Ports iOS ClickMeAPI+Auth resend. No body; uses the bearer issued at signup. */
    @POST(ApiRequestUrls.EMAIL_RESEND)
    suspend fun resendVerificationEmail(): SuccessMessageResponse

    /** Ports iOS ClickMeAPI+Auth verify. */
    @POST(ApiRequestUrls.EMAIL_VERIFY)
    suspend fun verifyEmailCode(
        @Body body: VerifyEmailCodeRequest,
    ): SuccessMessageResponse

    /** Ports iOS ClickMeAPI+Auth email status. */
    @GET(ApiRequestUrls.EMAIL_STATUS)
    suspend fun getEmailStatus(): SuccessDataResponse<EmailStatusData>

    /** Ports iOS ClickMeAPI+ExpertProfile setup. Partial update: null request fields are omitted. */
    @PATCH(ApiRequestUrls.EXPERT_PROFILE_SETUP)
    suspend fun setupExpertProfile(
        @Body body: SetupExpertProfileRequest,
    ): SuccessDataResponse<SetupExpertProfileData>

    /** Ports iOS ClickMeAPI+Meta languages. */
    @GET(ApiRequestUrls.META_LANGUAGES)
    suspend fun getLanguages(): SuccessDataResponse<LanguagesPayload>

    /** Ports iOS ClickMeAPI+Meta expertise tags. */
    @GET(ApiRequestUrls.META_EXPERTISE_TAGS)
    suspend fun getExpertiseTags(): SuccessDataResponse<ExpertiseTagsPayload>

    /** Ports iOS ClickMeAPI+Media avatar upload. The multipart field name is `image` (D-12). */
    @Multipart
    @POST(ApiRequestUrls.USER_AVATAR)
    suspend fun uploadAvatar(
        @Part image: MultipartBody.Part,
    ): SuccessDataResponse<AvatarUploadData>

    /** Ports iOS ClickMeAPI+UserProfile avatar delete. */
    @DELETE(ApiRequestUrls.USER_AVATAR)
    suspend fun deleteAvatar(): SuccessMessageResponse
}
