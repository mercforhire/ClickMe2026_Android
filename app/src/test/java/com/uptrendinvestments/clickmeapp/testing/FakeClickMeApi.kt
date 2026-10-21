package com.uptrendinvestments.clickmeapp.testing

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

    private val _setupProfileRequests = mutableListOf<SetupExpertProfileRequest>()

    /** Every PATCH /expert/profile/setup body received, in call order. */
    val setupProfileRequests: List<SetupExpertProfileRequest> get() = _setupProfileRequests

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
