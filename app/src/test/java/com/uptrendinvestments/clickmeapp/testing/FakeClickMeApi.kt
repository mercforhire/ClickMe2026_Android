package com.uptrendinvestments.clickmeapp.testing

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
import com.uptrendinvestments.clickmeapp.network.ClickMeApi
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response

/**
 * Scriptable [ClickMeApi] for session tests. Each endpoint delegates to a `var` handler (default: throws
 * `IllegalStateException("not stubbed")`) and counts its calls. Handlers may throw (use [httpError] or an
 * `IOException`) or suspend.
 */
class FakeClickMeApi : ClickMeApi {
    var loginHandler: suspend (LoginRequest) -> LoginData = { notStubbed() }
    var forgotPasswordHandler: suspend (ForgotPasswordRequest) -> SuccessMessageResponse = { notStubbed() }
    var resetPasswordHandler: suspend (ResetPasswordRequest) -> SuccessMessageResponse = { notStubbed() }
    var meHandler: suspend () -> MeData = { notStubbed() }
    var userProfileHandler: suspend () -> UserProfileData = { notStubbed() }
    var expertProfileHandler: suspend () -> ExpertProfileData = { notStubbed() }
    var topicsHandler: suspend () -> MyExpertTopicsPayload = { notStubbed() }
    var signupHandler: suspend (SignupRequest) -> SignupData = { notStubbed() }
    var resendEmailHandler: suspend () -> SuccessMessageResponse = { notStubbed() }
    var verifyEmailHandler: suspend (VerifyEmailCodeRequest) -> SuccessMessageResponse = { notStubbed() }
    var emailStatusHandler: suspend () -> EmailStatusData = { notStubbed() }
    var setupProfileHandler: suspend (SetupExpertProfileRequest) -> SetupExpertProfileData = { notStubbed() }
    var languagesHandler: suspend () -> LanguagesPayload = { notStubbed() }
    var expertiseTagsHandler: suspend () -> ExpertiseTagsPayload = { notStubbed() }
    var uploadAvatarHandler: suspend (MultipartBody.Part) -> AvatarUploadData = { notStubbed() }
    var deleteAvatarHandler: suspend () -> SuccessMessageResponse = { notStubbed() }
    var clientHomeHandler: suspend (String?) -> ClientHomeData = { notStubbed() }
    var clientBookingsHandler: suspend (String) -> ClientBookingsPayload = { notStubbed() }
    var categoriesHandler: suspend () -> CategoriesPayload = { notStubbed() }
    var expertDetailsHandler: suspend (String) -> PublicProfileDetailsData = { notStubbed() }
    var expertReviewsHandler: suspend (String) -> ExpertReviewsPayload = { notStubbed() }
    var expertAvailabilityHandler: suspend (expertId: String, month: String) -> ExpertAvailabilityData =
        { _, _ -> notStubbed() }
    var requestBookingHandler: suspend (BookingRequestBody) -> BookingRequestData = { notStubbed() }
    var clientBookingDetailHandler: suspend (String) -> ClientBookingDetail = { notStubbed() }

    @Volatile var loginCalls = 0
        private set

    @Volatile var forgotPasswordCalls = 0
        private set

    @Volatile var resetPasswordCalls = 0
        private set

    @Volatile var meCalls = 0
        private set

    @Volatile var userProfileCalls = 0
        private set

    @Volatile var expertProfileCalls = 0
        private set

    @Volatile var topicsCalls = 0
        private set

    @Volatile var signupCalls = 0
        private set

    @Volatile var resendEmailCalls = 0
        private set

    @Volatile var verifyEmailCalls = 0
        private set

    @Volatile var emailStatusCalls = 0
        private set

    @Volatile var setupProfileCalls = 0
        private set

    @Volatile var languagesCalls = 0
        private set

    @Volatile var expertiseTagsCalls = 0
        private set

    @Volatile var uploadAvatarCalls = 0
        private set

    @Volatile var deleteAvatarCalls = 0
        private set

    @Volatile var clientHomeCalls = 0
        private set

    @Volatile var clientBookingsCalls = 0
        private set

    @Volatile var categoriesCalls = 0
        private set

    @Volatile var expertDetailsCalls = 0
        private set

    @Volatile var expertReviewsCalls = 0
        private set

    @Volatile var expertAvailabilityCalls = 0
        private set

    @Volatile var requestBookingCalls = 0
        private set

    @Volatile var clientBookingDetailCalls = 0
        private set

    private val _clientHomeCategories = mutableListOf<String?>()

    /** The `category` argument of every getClientHome call, in call order (null means unfiltered). */
    val clientHomeCategories: List<String?> get() = _clientHomeCategories

    private val _setupProfileRequests = mutableListOf<SetupExpertProfileRequest>()

    /** Every PATCH /expert/profile/setup body received, in call order. */
    val setupProfileRequests: List<SetupExpertProfileRequest> get() = _setupProfileRequests

    private val _availabilityMonths = mutableListOf<String>()

    /** The `month` argument of every getExpertAvailability call, in call order (recorded before the handler runs). */
    val availabilityMonths: List<String> get() = _availabilityMonths

    private val _availabilityTopicIds = mutableListOf<String?>()

    /** The `topicId` argument of every getExpertAvailability call, in call order (null means omitted). */
    val availabilityTopicIds: List<String?> get() = _availabilityTopicIds

    private val _requestBookingRequests = mutableListOf<BookingRequestBody>()

    /** Every POST /bookings/request body received, in call order. */
    val requestBookingRequests: List<BookingRequestBody> get() = _requestBookingRequests

    override suspend fun login(body: LoginRequest): SuccessDataResponse<LoginData> {
        loginCalls++
        return ok(loginHandler(body))
    }

    override suspend fun forgotPassword(body: ForgotPasswordRequest): SuccessMessageResponse {
        forgotPasswordCalls++
        return forgotPasswordHandler(body)
    }

    override suspend fun resetPassword(body: ResetPasswordRequest): SuccessMessageResponse {
        resetPasswordCalls++
        return resetPasswordHandler(body)
    }

    override suspend fun getMe(): SuccessDataResponse<MeData> {
        meCalls++
        return ok(meHandler())
    }

    override suspend fun getUserProfile(): SuccessDataResponse<UserProfileData> {
        userProfileCalls++
        return ok(userProfileHandler())
    }

    override suspend fun getExpertProfile(): SuccessDataResponse<ExpertProfileData> {
        expertProfileCalls++
        return ok(expertProfileHandler())
    }

    override suspend fun getMyTopics(): SuccessDataResponse<MyExpertTopicsPayload> {
        topicsCalls++
        return ok(topicsHandler())
    }

    override suspend fun signup(body: SignupRequest): SuccessDataResponse<SignupData> {
        signupCalls++
        return ok(signupHandler(body))
    }

    override suspend fun resendVerificationEmail(): SuccessMessageResponse {
        resendEmailCalls++
        return resendEmailHandler()
    }

    override suspend fun verifyEmailCode(body: VerifyEmailCodeRequest): SuccessMessageResponse {
        verifyEmailCalls++
        return verifyEmailHandler(body)
    }

    override suspend fun getEmailStatus(): SuccessDataResponse<EmailStatusData> {
        emailStatusCalls++
        return ok(emailStatusHandler())
    }

    override suspend fun setupExpertProfile(
        body: SetupExpertProfileRequest,
    ): SuccessDataResponse<SetupExpertProfileData> {
        setupProfileCalls++
        _setupProfileRequests += body
        return ok(setupProfileHandler(body))
    }

    override suspend fun getLanguages(): SuccessDataResponse<LanguagesPayload> {
        languagesCalls++
        return ok(languagesHandler())
    }

    override suspend fun getExpertiseTags(): SuccessDataResponse<ExpertiseTagsPayload> {
        expertiseTagsCalls++
        return ok(expertiseTagsHandler())
    }

    override suspend fun uploadAvatar(image: MultipartBody.Part): SuccessDataResponse<AvatarUploadData> {
        uploadAvatarCalls++
        return ok(uploadAvatarHandler(image))
    }

    override suspend fun deleteAvatar(): SuccessMessageResponse {
        deleteAvatarCalls++
        return deleteAvatarHandler()
    }

    override suspend fun getClientHome(category: String?): SuccessDataResponse<ClientHomeData> {
        clientHomeCalls++
        _clientHomeCategories += category
        return ok(clientHomeHandler(category))
    }

    override suspend fun getClientBookings(
        type: String,
        page: Int,
        limit: Int,
    ): SuccessDataResponse<ClientBookingsPayload> {
        clientBookingsCalls++
        return ok(clientBookingsHandler(type))
    }

    override suspend fun getCategories(): SuccessDataResponse<CategoriesPayload> {
        categoriesCalls++
        return ok(categoriesHandler())
    }

    override suspend fun getExpertDetails(expertId: String): SuccessDataResponse<PublicProfileDetailsData> {
        expertDetailsCalls++
        return ok(expertDetailsHandler(expertId))
    }

    override suspend fun getExpertReviews(
        expertId: String,
        page: Int,
        limit: Int,
    ): SuccessDataResponse<ExpertReviewsPayload> {
        expertReviewsCalls++
        return ok(expertReviewsHandler(expertId))
    }

    override suspend fun getExpertAvailability(
        expertId: String,
        month: String,
        topicId: String?,
    ): SuccessDataResponse<ExpertAvailabilityData> {
        expertAvailabilityCalls++
        _availabilityMonths += month
        _availabilityTopicIds += topicId
        return ok(expertAvailabilityHandler(expertId, month))
    }

    override suspend fun requestBooking(body: BookingRequestBody): SuccessDataResponse<BookingRequestData> {
        requestBookingCalls++
        _requestBookingRequests += body
        return ok(requestBookingHandler(body))
    }

    override suspend fun getClientBookingDetail(bookingId: String): SuccessDataResponse<ClientBookingDetail> {
        clientBookingDetailCalls++
        return ok(clientBookingDetailHandler(bookingId))
    }

    /** Builds the exception Retrofit throws for a non-2xx response carrying [json] as the body. */
    fun httpError(
        status: Int,
        json: String,
    ): HttpException =
        HttpException(
            Response.error<Any>(status, json.toResponseBody("application/json".toMediaType())),
        )

    private fun <T> ok(data: T) = SuccessDataResponse(status = "success", data = data)

    private fun notStubbed(): Nothing = throw IllegalStateException("not stubbed")
}
