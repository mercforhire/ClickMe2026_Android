package com.uptrendinvestments.clickmeapp.testing

import com.uptrendinvestments.clickmeapp.model.requests.ForgotPasswordRequest
import com.uptrendinvestments.clickmeapp.model.requests.LoginRequest
import com.uptrendinvestments.clickmeapp.model.requests.ResetPasswordRequest
import com.uptrendinvestments.clickmeapp.model.responses.ExpertProfileData
import com.uptrendinvestments.clickmeapp.model.responses.LoginData
import com.uptrendinvestments.clickmeapp.model.responses.MeData
import com.uptrendinvestments.clickmeapp.model.responses.MyExpertTopicsPayload
import com.uptrendinvestments.clickmeapp.model.responses.SuccessDataResponse
import com.uptrendinvestments.clickmeapp.model.responses.SuccessMessageResponse
import com.uptrendinvestments.clickmeapp.model.responses.UserProfileData
import com.uptrendinvestments.clickmeapp.network.ClickMeApi
import okhttp3.MediaType.Companion.toMediaType
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
