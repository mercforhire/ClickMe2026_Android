package com.uptrendinvestments.clickmeapp.network

import com.uptrendinvestments.clickmeapp.model.requests.BookingRequestBody
import com.uptrendinvestments.clickmeapp.model.requests.ForgotPasswordRequest
import com.uptrendinvestments.clickmeapp.model.requests.LoginRequest
import com.uptrendinvestments.clickmeapp.model.requests.ResetPasswordRequest
import com.uptrendinvestments.clickmeapp.model.requests.SetupExpertProfileRequest
import com.uptrendinvestments.clickmeapp.model.requests.SignupRequest
import com.uptrendinvestments.clickmeapp.model.requests.VerifyEmailCodeRequest
import com.uptrendinvestments.clickmeapp.model.responses.AvatarUploadData
import com.uptrendinvestments.clickmeapp.model.responses.BookingRequestData
import com.uptrendinvestments.clickmeapp.model.responses.CategoriesPayload
import com.uptrendinvestments.clickmeapp.model.responses.ClientBookingDetail
import com.uptrendinvestments.clickmeapp.model.responses.ClientBookingsPayload
import com.uptrendinvestments.clickmeapp.model.responses.ClientHomeData
import com.uptrendinvestments.clickmeapp.model.responses.EmailStatusData
import com.uptrendinvestments.clickmeapp.model.responses.ExpertAvailabilityData
import com.uptrendinvestments.clickmeapp.model.responses.ExpertProfileData
import com.uptrendinvestments.clickmeapp.model.responses.ExpertReviewsPayload
import com.uptrendinvestments.clickmeapp.model.responses.ExpertiseTagsPayload
import com.uptrendinvestments.clickmeapp.model.responses.LanguagesPayload
import com.uptrendinvestments.clickmeapp.model.responses.LoginData
import com.uptrendinvestments.clickmeapp.model.responses.MeData
import com.uptrendinvestments.clickmeapp.model.responses.MyExpertTopicsPayload
import com.uptrendinvestments.clickmeapp.model.responses.PublicProfileDetailsData
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
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit surface for the Phase 2 endpoints. Ports iOS ClickMeAPI+Auth, +Me and +UserProfile plus the two
 * expert lookups used by SplashViewModel and LoginView. It also covers the Phase 3 password-recovery
 * endpoints (iOS ClickMeAPI+Auth). Phase 4 adds signup, email verification, expert profile setup, meta
 * lookups and avatar upload/delete. Phase 5 adds the discovery group (client home, client bookings,
 * categories, expert details and reviews). Phase 6 adds the booking group (availability, free booking
 * request, client booking detail). Other API groups arrive with their features (ROADMAP Phase 2 planning
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

    /**
     * Ports iOS ClickMeAPI+Discovery getClientHome. [category] is one comma-joined list of slugs; null omits
     * the parameter. Never send page or limit: the schema ignores them and the server defaults p_limit to 10.
     */
    @GET(ApiRequestUrls.CLIENT_HOME)
    suspend fun getClientHome(
        @Query("category") category: String?,
    ): SuccessDataResponse<ClientHomeData>

    /** Ports iOS ClickMeAPI+ClientBookings list. [type] is the server filter, e.g. `upcoming`. */
    @GET(ApiRequestUrls.CLIENT_BOOKINGS)
    suspend fun getClientBookings(
        @Query("type") type: String,
        @Query("page") page: Int,
        @Query("limit") limit: Int,
    ): SuccessDataResponse<ClientBookingsPayload>

    /** Ports iOS ClickMeAPI+Discovery categories. Public on the backend; the bearer is ignored there. */
    @GET(ApiRequestUrls.CATEGORIES)
    suspend fun getCategories(): SuccessDataResponse<CategoriesPayload>

    /** Ports iOS ClickMeAPI+Discovery getExpertDetails. */
    @GET(ApiRequestUrls.EXPERT_DETAILS)
    suspend fun getExpertDetails(
        @Path("id") expertId: String,
    ): SuccessDataResponse<PublicProfileDetailsData>

    /** Ports iOS ClickMeAPI+Discovery getExpertReviews. */
    @GET(ApiRequestUrls.EXPERT_REVIEWS)
    suspend fun getExpertReviews(
        @Path("id") expertId: String,
        @Query("page") page: Int,
        @Query("limit") limit: Int,
    ): SuccessDataResponse<ExpertReviewsPayload>

    /**
     * Ports iOS ClickMeAPI+Availability getExpertAvailability. [month] is `YearMonth.toString()`
     * (`yyyy-MM`). [topicId] null omits the parameter; Phase 6 always passes null (D-07 iOS parity). The
     * backend narrows the grid to the topic duration when it is present.
     */
    @GET(ApiRequestUrls.EXPERT_AVAILABILITY)
    suspend fun getExpertAvailability(
        @Path("id") expertId: String,
        @Query("month") month: String,
        @Query("topic_id") topicId: String?,
    ): SuccessDataResponse<ExpertAvailabilityData>

    /** Ports iOS ClickMeAPI+Booking requestBooking. Free path only; 201 on success. */
    @POST(ApiRequestUrls.BOOKING_REQUEST)
    suspend fun requestBooking(
        @Body body: BookingRequestBody,
    ): SuccessDataResponse<BookingRequestData>

    /** Ports iOS ClickMeAPI+Booking getClientBookingDetail. */
    @GET(ApiRequestUrls.CLIENT_BOOKING_DETAIL)
    suspend fun getClientBookingDetail(
        @Path("id") bookingId: String,
    ): SuccessDataResponse<ClientBookingDetail>
}
