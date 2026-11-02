package com.uptrendinvestments.clickmeapp.network

import com.uptrendinvestments.clickmeapp.model.requests.AcceptBookingRequestBody
import com.uptrendinvestments.clickmeapp.model.requests.AcceptRescheduleRequest
import com.uptrendinvestments.clickmeapp.model.requests.AccountDeletionRequest
import com.uptrendinvestments.clickmeapp.model.requests.BookingNoteRequest
import com.uptrendinvestments.clickmeapp.model.requests.BookingRequestBody
import com.uptrendinvestments.clickmeapp.model.requests.CancelBookingRequest
import com.uptrendinvestments.clickmeapp.model.requests.ChatActionRequest
import com.uptrendinvestments.clickmeapp.model.requests.ConfirmBookingRequest
import com.uptrendinvestments.clickmeapp.model.requests.ConfirmConnectionRequest
import com.uptrendinvestments.clickmeapp.model.requests.CreateExpertTopicRequest
import com.uptrendinvestments.clickmeapp.model.requests.DeclineBookingRequestBody
import com.uptrendinvestments.clickmeapp.model.requests.DeclineRescheduleRequest
import com.uptrendinvestments.clickmeapp.model.requests.DeleteAccountRequest
import com.uptrendinvestments.clickmeapp.model.requests.EndCallRequest
import com.uptrendinvestments.clickmeapp.model.requests.ExpertRescheduleProposalRequest
import com.uptrendinvestments.clickmeapp.model.requests.ForgotPasswordRequest
import com.uptrendinvestments.clickmeapp.model.requests.InitiateChatRequest
import com.uptrendinvestments.clickmeapp.model.requests.LoginRequest
import com.uptrendinvestments.clickmeapp.model.requests.PaymentIntentRequest
import com.uptrendinvestments.clickmeapp.model.requests.RegisterDeviceTokenRequest
import com.uptrendinvestments.clickmeapp.model.requests.ReportUserRequest
import com.uptrendinvestments.clickmeapp.model.requests.RescheduleProposalRequest
import com.uptrendinvestments.clickmeapp.model.requests.ResetPasswordRequest
import com.uptrendinvestments.clickmeapp.model.requests.SaveTaxProfileRequest
import com.uptrendinvestments.clickmeapp.model.requests.SendMessageRequest
import com.uptrendinvestments.clickmeapp.model.requests.SetupExpertProfileRequest
import com.uptrendinvestments.clickmeapp.model.requests.SignupRequest
import com.uptrendinvestments.clickmeapp.model.requests.SubmitReviewRequest
import com.uptrendinvestments.clickmeapp.model.requests.UpdateExpertMeetingUrlRequest
import com.uptrendinvestments.clickmeapp.model.requests.UpdateExpertTagsRequest
import com.uptrendinvestments.clickmeapp.model.requests.UpdateExpertTopicRequest
import com.uptrendinvestments.clickmeapp.model.requests.UpdateMyAvailabilityRequest
import com.uptrendinvestments.clickmeapp.model.requests.UpdateNotificationPreferencesRequest
import com.uptrendinvestments.clickmeapp.model.requests.UpdatePasswordRequest
import com.uptrendinvestments.clickmeapp.model.requests.UpdateUserProfileRequest
import com.uptrendinvestments.clickmeapp.model.requests.VerifyEmailCodeRequest
import com.uptrendinvestments.clickmeapp.model.responses.AcceptBookingRequestData
import com.uptrendinvestments.clickmeapp.model.responses.AvatarUploadData
import com.uptrendinvestments.clickmeapp.model.responses.BookingNoteData
import com.uptrendinvestments.clickmeapp.model.responses.BookingRequestData
import com.uptrendinvestments.clickmeapp.model.responses.BookingRequestDetail
import com.uptrendinvestments.clickmeapp.model.responses.BookingRequestsPayload
import com.uptrendinvestments.clickmeapp.model.responses.CancelBookingData
import com.uptrendinvestments.clickmeapp.model.responses.CategoriesPayload
import com.uptrendinvestments.clickmeapp.model.responses.ChatMessagesPayload
import com.uptrendinvestments.clickmeapp.model.responses.ChatSendAckData
import com.uptrendinvestments.clickmeapp.model.responses.ChatsPayload
import com.uptrendinvestments.clickmeapp.model.responses.ClientBookingDetail
import com.uptrendinvestments.clickmeapp.model.responses.ClientBookingsPayload
import com.uptrendinvestments.clickmeapp.model.responses.ClientHomeData
import com.uptrendinvestments.clickmeapp.model.responses.ClientTransactionsData
import com.uptrendinvestments.clickmeapp.model.responses.ConfirmBookingData
import com.uptrendinvestments.clickmeapp.model.responses.ConfirmConnectionData
import com.uptrendinvestments.clickmeapp.model.responses.ConnectLinkData
import com.uptrendinvestments.clickmeapp.model.responses.ConnectStatusData
import com.uptrendinvestments.clickmeapp.model.responses.DeclineBookingRequestData
import com.uptrendinvestments.clickmeapp.model.responses.DeletionRequestData
import com.uptrendinvestments.clickmeapp.model.responses.EarningsTransactionsData
import com.uptrendinvestments.clickmeapp.model.responses.EmailStatusData
import com.uptrendinvestments.clickmeapp.model.responses.EndCallData
import com.uptrendinvestments.clickmeapp.model.responses.ExpertAvailabilityData
import com.uptrendinvestments.clickmeapp.model.responses.ExpertAvailabilityScheduleData
import com.uptrendinvestments.clickmeapp.model.responses.ExpertBookingDetail
import com.uptrendinvestments.clickmeapp.model.responses.ExpertBookingsPayload
import com.uptrendinvestments.clickmeapp.model.responses.ExpertClientProfileData
import com.uptrendinvestments.clickmeapp.model.responses.ExpertProfileData
import com.uptrendinvestments.clickmeapp.model.responses.ExpertReviewsPayload
import com.uptrendinvestments.clickmeapp.model.responses.ExpertiseTagsPayload
import com.uptrendinvestments.clickmeapp.model.responses.ExpertsSearchPayload
import com.uptrendinvestments.clickmeapp.model.responses.FaqsPayload
import com.uptrendinvestments.clickmeapp.model.responses.FavoritesPayload
import com.uptrendinvestments.clickmeapp.model.responses.InitiateChatData
import com.uptrendinvestments.clickmeapp.model.responses.JoinCallResponse
import com.uptrendinvestments.clickmeapp.model.responses.LanguagesPayload
import com.uptrendinvestments.clickmeapp.model.responses.LoginData
import com.uptrendinvestments.clickmeapp.model.responses.MeData
import com.uptrendinvestments.clickmeapp.model.responses.MyExpertTopicPayload
import com.uptrendinvestments.clickmeapp.model.responses.MyExpertTopicsPayload
import com.uptrendinvestments.clickmeapp.model.responses.NotificationPreferencesData
import com.uptrendinvestments.clickmeapp.model.responses.NotificationsPayload
import com.uptrendinvestments.clickmeapp.model.responses.PaymentIntentResponse
import com.uptrendinvestments.clickmeapp.model.responses.PaymentMethodSetupData
import com.uptrendinvestments.clickmeapp.model.responses.PaymentMethodsData
import com.uptrendinvestments.clickmeapp.model.responses.PayoutSummaryData
import com.uptrendinvestments.clickmeapp.model.responses.PublicProfileDetailsData
import com.uptrendinvestments.clickmeapp.model.responses.ReportUserData
import com.uptrendinvestments.clickmeapp.model.responses.RescheduleResponseData
import com.uptrendinvestments.clickmeapp.model.responses.ReviewContextData
import com.uptrendinvestments.clickmeapp.model.responses.SaveTaxProfileData
import com.uptrendinvestments.clickmeapp.model.responses.SetupExpertProfileData
import com.uptrendinvestments.clickmeapp.model.responses.SignupData
import com.uptrendinvestments.clickmeapp.model.responses.StatusOnlyResponse
import com.uptrendinvestments.clickmeapp.model.responses.SubmitReviewData
import com.uptrendinvestments.clickmeapp.model.responses.SuccessDataResponse
import com.uptrendinvestments.clickmeapp.model.responses.SuccessMessageResponse
import com.uptrendinvestments.clickmeapp.model.responses.TaxProfileData
import com.uptrendinvestments.clickmeapp.model.responses.ToggleFavoriteData
import com.uptrendinvestments.clickmeapp.model.responses.UserProfileData
import com.uptrendinvestments.clickmeapp.model.responses.WeeklyEarningsData
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit surface for the Phase 2 endpoints. Ports iOS ClickMeAPI+Auth, +Me and +UserProfile plus the two
 * expert lookups used by SplashViewModel and LoginView. It also covers the Phase 3 password-recovery
 * endpoints (iOS ClickMeAPI+Auth). Phase 4 adds signup, email verification, expert profile setup, meta
 * lookups and avatar upload/delete. Phase 5 adds the discovery group (client home, client bookings,
 * categories, expert details and reviews). Phase 6 adds the booking group (availability, free booking
 * request, client booking detail). Phase 7 adds the payment group (saved cards, setup and payment intents,
 * booking confirm). Phase 8 adds the client booking actions (cancel, accept and decline a reschedule).
 * Phase 9 adds the call group (join, confirm connection, end). Phase 10 adds expert topics (create, update, delete),
 * the expert's own weekly availability and the profile-tags update. Phase 14 adds nine endpoints: the profile
 * PATCH, change password, account deletion (request and confirm), the expert meeting link, the help FAQ list, the
 * earnings transactions ledger and the expert tax profile (get and save). Phase 16.2 adds the two
 * reschedule-propose PATCH calls. Phase 16.3 adds the three favorites calls. Phase 16.4 adds the four note calls and
 * the expert's client profile. Phase 16.5 adds the client payment history, the notification preferences and the
 * feedback calls. Other API groups arrive with their features (ROADMAP Phase 2 planning note).
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

    /**
     * Ports iOS ClickMeAPI+Discovery searchExperts. [q] is always sent: an empty string returns the default
     * browse set (the RPC treats it as no search). A null [category] omits the parameter (the All chip). [sort] is
     * `relevance` or `rating`. The backend schema is strict, so only these five parameters may ever be declared
     * (the old iOS rate filters were dropped server-side).
     */
    @GET(ApiRequestUrls.EXPERTS_SEARCH)
    suspend fun searchExperts(
        @Query("q") q: String,
        @Query("category") category: String?,
        @Query("sort") sort: String,
        @Query("page") page: Int,
        @Query("limit") limit: Int,
    ): SuccessDataResponse<ExpertsSearchPayload>

    /**
     * Ports iOS ClickMeAPI+Favorites getFavorites. Client-only (403 otherwise). The server default `limit` is 20 and
     * the cap is 100; the repository always asks for page 1 with 100 (one request, no paging).
     */
    @GET(ApiRequestUrls.CLIENT_FAVORITES)
    suspend fun getClientFavorites(
        @Query("page") page: Int,
        @Query("limit") limit: Int,
    ): SuccessDataResponse<FavoritesPayload>

    /**
     * Ports iOS ClickMeAPI+Favorites addFavorite. PUT on the backend (not POST); idempotent and client-only. No
     * `@Body`: the route has no body schema (same shape as [joinCall]), and Retrofit sends an empty body for a PUT.
     */
    @PUT(ApiRequestUrls.CLIENT_FAVORITE)
    suspend fun addFavorite(
        @Path("expertId") expertId: String,
    ): SuccessDataResponse<ToggleFavoriteData>

    /** Ports iOS ClickMeAPI+Favorites removeFavorite. Idempotent and client-only. */
    @DELETE(ApiRequestUrls.CLIENT_FAVORITE)
    suspend fun removeFavorite(
        @Path("expertId") expertId: String,
    ): SuccessDataResponse<ToggleFavoriteData>

    // Phase 16.4 note calls. Ports iOS getClientBookingNote / createClientBookingNote and the expert pair. A 404 is
    // uniform for a missing booking and for one that is not the caller's. The POST body is strict: `note` only.

    /** The client's private note on a booking. 404 when the booking is missing or not the caller's. */
    @GET(ApiRequestUrls.CLIENT_BOOKING_NOTE)
    suspend fun getClientBookingNote(
        @Path("id") id: String,
    ): SuccessDataResponse<BookingNoteData>

    /** Saves the client's private note; an empty string clears it. */
    @POST(ApiRequestUrls.CLIENT_BOOKING_NOTE)
    suspend fun saveClientBookingNote(
        @Path("id") id: String,
        @Body body: BookingNoteRequest,
    ): SuccessDataResponse<BookingNoteData>

    /** The expert's private note on a booking. 404 when the booking is missing or not the caller's. */
    @GET(ApiRequestUrls.BOOKING_NOTE)
    suspend fun getBookingNote(
        @Path("id") id: String,
    ): SuccessDataResponse<BookingNoteData>

    /** Saves the expert's private note; an empty string clears it. */
    @POST(ApiRequestUrls.BOOKING_NOTE)
    suspend fun saveBookingNote(
        @Path("id") id: String,
        @Body body: BookingNoteRequest,
    ): SuccessDataResponse<BookingNoteData>

    /**
     * Ports iOS getClientProfile: the expert's view of one client. 404 for a missing or deleted client, 403
     * `NO_RELATIONSHIP` when this expert has no booking with them.
     */
    @GET(ApiRequestUrls.EXPERT_CLIENT)
    suspend fun getExpertClient(
        @Path("id") id: String,
    ): SuccessDataResponse<ExpertClientProfileData>

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
     * backend narrows the grid to the topic duration when it is present. [excludeBookingId] null omits the
     * parameter; the reschedule pickers pass the booking being moved so its own cell and buffer cells are not
     * grayed (backend quick 260804-qo6; an iOS deviation recorded in 16.2 RESEARCH Open Question 3). Make a
     * Booking passes null.
     */
    @GET(ApiRequestUrls.EXPERT_AVAILABILITY)
    suspend fun getExpertAvailability(
        @Path("id") expertId: String,
        @Query("month") month: String,
        @Query("topic_id") topicId: String?,
        @Query("exclude_booking_id") excludeBookingId: String?,
    ): SuccessDataResponse<ExpertAvailabilityData>

    /** Ports iOS ClickMeAPI+Booking requestBooking. Free path only (the paid path is [confirmBooking]); 201 on success. */
    @POST(ApiRequestUrls.BOOKING_REQUEST)
    suspend fun requestBooking(
        @Body body: BookingRequestBody,
    ): SuccessDataResponse<BookingRequestData>

    /** Ports iOS ClickMeAPI+Booking getClientBookingDetail. */
    @GET(ApiRequestUrls.CLIENT_BOOKING_DETAIL)
    suspend fun getClientBookingDetail(
        @Path("id") bookingId: String,
    ): SuccessDataResponse<ClientBookingDetail>

    /** Ports iOS ClickMeAPI+PaymentMethods getPaymentMethods. */
    @GET(ApiRequestUrls.PAYMENT_METHODS)
    suspend fun getPaymentMethods(): SuccessDataResponse<PaymentMethodsData>

    /**
     * Ports iOS ClickMeAPI+PaymentMethods createSetupIntent. The backend schema is `z.object({}).strict()`
     * and Retrofit would otherwise send no body, so the `{}` body is explicit (RESEARCH Pitfall 13).
     */
    @POST(ApiRequestUrls.PAYMENT_METHODS_SETUP_INTENT)
    suspend fun createPaymentMethodSetupIntent(
        @Body body: Map<String, String> = emptyMap(),
    ): SuccessDataResponse<PaymentMethodSetupData>

    /** Ports iOS ClickMeAPI+PaymentMethods deletePaymentMethod. */
    @DELETE(ApiRequestUrls.PAYMENT_METHOD)
    suspend fun deletePaymentMethod(
        @Path("id") paymentMethodId: String,
    ): SuccessMessageResponse

    /** Ports iOS ClickMeAPI+PaymentMethods setDefaultPaymentMethod. Explicit `{}` body, as for setup-intent. */
    @PATCH(ApiRequestUrls.PAYMENT_METHOD_DEFAULT)
    suspend fun setDefaultPaymentMethod(
        @Path("id") paymentMethodId: String,
        @Body body: Map<String, String> = emptyMap(),
    ): SuccessMessageResponse

    /** Ports iOS ClickMeAPI+Booking createPaymentIntent. The response is a bare body, not enveloped. */
    @POST(ApiRequestUrls.PAYMENT_INTENT)
    suspend fun createPaymentIntent(
        @Body body: PaymentIntentRequest,
    ): PaymentIntentResponse

    /** Ports iOS ClickMeAPI+Booking confirmBooking. 201 on success; 402 PAYMENT_REQUIRES_ACTION carries 3DS detail. */
    @POST(ApiRequestUrls.BOOKING_CONFIRM)
    suspend fun confirmBooking(
        @Body body: ConfirmBookingRequest,
    ): SuccessDataResponse<ConfirmBookingData>

    /**
     * Ports iOS ClickMeAPI+Booking cancelClientBooking. The backend `cancelSchema` is `.strict()` with `reason` as
     * its only key, so the comment travels inside [CancelBookingRequest.reason]. The refund is in MAJOR units.
     */
    @POST(ApiRequestUrls.CLIENT_BOOKING_CANCEL)
    suspend fun cancelClientBooking(
        @Path("id") bookingId: String,
        @Body body: CancelBookingRequest,
    ): SuccessDataResponse<CancelBookingData>

    /**
     * Ports iOS ClickMeAPI+Booking acceptClientReschedule. The backend schema is `.strict()`. An explicit body is
     * always passed because Retrofit would otherwise send no body (Phase 7 RESEARCH Pitfall 13). A repeated accept
     * on an already confirmed booking is an idempotent 200.
     */
    @POST(ApiRequestUrls.CLIENT_BOOKING_ACCEPT_RESCHEDULE)
    suspend fun acceptClientReschedule(
        @Path("id") bookingId: String,
        @Body body: AcceptRescheduleRequest,
    ): SuccessDataResponse<RescheduleResponseData>

    /** Ports iOS ClickMeAPI+Booking declineClientReschedule. The backend schema is `.strict()` (`reason` only). */
    @POST(ApiRequestUrls.CLIENT_BOOKING_DECLINE_RESCHEDULE)
    suspend fun declineClientReschedule(
        @Path("id") bookingId: String,
        @Body body: DeclineRescheduleRequest,
    ): SuccessDataResponse<RescheduleResponseData>

    /**
     * Ports the iOS client propose-a-reschedule call. The backend `rescheduleSchema` is `.strict()` (unknown keys
     * are a 422), so [RescheduleProposalRequest] holds only the four schema keys. The response data is
     * `{booking_id, status}`.
     */
    @PATCH(ApiRequestUrls.CLIENT_BOOKING_RESCHEDULE)
    suspend fun rescheduleClientBooking(
        @Path("id") bookingId: String,
        @Body body: RescheduleProposalRequest,
    ): SuccessDataResponse<RescheduleResponseData>

    /**
     * Ports iOS ClickMeAPI+Call joinCall. No `@Body`: the backend join route has no schema and reads only the path
     * param, so the explicit-`{}` rule for strict bodies does not apply. The response decides the call variant
     * (`in_app_voice` with [JoinCallResponse.agoraConfig], `skype_zoom` with [JoinCallResponse.externalConfig]).
     */
    @POST(ApiRequestUrls.BOOKING_JOIN)
    suspend fun joinCall(
        @Path("id") bookingId: String,
    ): SuccessDataResponse<JoinCallResponse>

    /** Ports iOS ClickMeAPI+Call confirmCallConnection. The backend schema is `.strict()` (see the request KDoc). */
    @PATCH(ApiRequestUrls.BOOKING_CONFIRM_CONNECTION)
    suspend fun confirmCallConnection(
        @Path("id") bookingId: String,
        @Body body: ConfirmConnectionRequest,
    ): SuccessDataResponse<ConfirmConnectionData>

    /**
     * Ports iOS ClickMeAPI+Call endCall. The response status is always the unchanged booking status; callers never
     * branch on it (CALL-END-SEMANTICS).
     */
    @POST(ApiRequestUrls.BOOKING_END)
    suspend fun endCall(
        @Path("id") bookingId: String,
        @Body body: EndCallRequest,
    ): SuccessDataResponse<EndCallData>

    /** Ports iOS ClickMeAPI+ExpertTopics createTopic. 201 with `data.topic`; 422 `PRICE_BELOW_MINIMUM` on a low price. */
    @POST(ApiRequestUrls.EXPERT_TOPICS)
    suspend fun createTopic(
        @Body body: CreateExpertTopicRequest,
    ): SuccessDataResponse<MyExpertTopicPayload>

    /**
     * Ports iOS ClickMeAPI+ExpertTopics updateTopic. The body is serialized by `UpdateExpertTopicRequestAdapter` so a
     * cleared icon or description reaches the server as an explicit `null`.
     */
    @PATCH(ApiRequestUrls.EXPERT_TOPIC)
    suspend fun updateTopic(
        @Path("id") topicId: String,
        @Body body: UpdateExpertTopicRequest,
    ): SuccessDataResponse<MyExpertTopicPayload>

    /** Ports iOS ClickMeAPI+ExpertTopics deleteTopic. Soft delete; 404 when missing, no 409 today. */
    @DELETE(ApiRequestUrls.EXPERT_TOPIC)
    suspend fun deleteTopic(
        @Path("id") topicId: String,
    ): SuccessMessageResponse

    /** Ports iOS ClickMeAPI+Availability getMyAvailability: the expert's own schedule, not the public route. */
    @GET(ApiRequestUrls.EXPERT_MY_AVAILABILITY)
    suspend fun getMyAvailability(): SuccessDataResponse<ExpertAvailabilityScheduleData>

    /** Ports iOS ClickMeAPI+Availability updateMyAvailability. Full replace; the response `data.updated_days` is ignored. */
    @PUT(ApiRequestUrls.EXPERT_MY_AVAILABILITY)
    suspend fun updateMyAvailability(
        @Body body: UpdateMyAvailabilityRequest,
    ): SuccessMessageResponse

    /** Ports iOS ClickMeAPI+ExpertProfile updateExpertProfile (tags only): full replace of the profile tags (D-08). */
    @PATCH(ApiRequestUrls.EXPERT_PROFILE)
    suspend fun updateExpertProfileTags(
        @Body body: UpdateExpertTagsRequest,
    ): SuccessMessageResponse

    /**
     * Ports iOS ClickMeAPI+BookingRequests getBookingRequests. The list endpoint returns every pending request in one
     * response and ignores paging, so no `page` or `limit` is sent (RESEARCH C2).
     */
    @GET(ApiRequestUrls.EXPERT_BOOKING_REQUESTS)
    suspend fun getBookingRequests(): SuccessDataResponse<BookingRequestsPayload>

    /** Ports iOS ClickMeAPI+BookingRequests getBookingRequestDetail. 404 `NOT_FOUND` when the request is missing. */
    @GET(ApiRequestUrls.EXPERT_BOOKING_REQUEST)
    suspend fun getBookingRequest(
        @Path("id") requestId: String,
    ): SuccessDataResponse<BookingRequestDetail>

    /**
     * Ports iOS ClickMeAPI+BookingRequests acceptBookingRequest. The backend `acceptSchema` is `.strict()` and a
     * missing body fails validation, so an explicit [AcceptBookingRequestBody] is always passed (`{}` when empty).
     * 409 `ALREADY_PROCESSED` when the request is no longer pending.
     */
    @POST(ApiRequestUrls.EXPERT_BOOKING_REQUEST_ACCEPT)
    suspend fun acceptBookingRequest(
        @Path("id") requestId: String,
        @Body body: AcceptBookingRequestBody,
    ): SuccessDataResponse<AcceptBookingRequestData>

    /**
     * Ports iOS ClickMeAPI+BookingRequests declineBookingRequest. The backend `declineSchema` is `.strict()` and
     * requires both `reason_code` and `reason_text`. 409 `ALREADY_PROCESSED` when the request is no longer pending.
     */
    @POST(ApiRequestUrls.EXPERT_BOOKING_REQUEST_DECLINE)
    suspend fun declineBookingRequest(
        @Path("id") requestId: String,
        @Body body: DeclineBookingRequestBody,
    ): SuccessDataResponse<DeclineBookingRequestData>

    /**
     * Ports iOS ClickMeAPI+ExpertBookings getExpertBookings. `type` is `upcoming` or `past` and is required (422
     * otherwise); `limit` is 1 to 100.
     */
    @GET(ApiRequestUrls.EXPERT_BOOKINGS)
    suspend fun getExpertBookings(
        @Query("type") type: String,
        @Query("page") page: Int,
        @Query("limit") limit: Int,
    ): SuccessDataResponse<ExpertBookingsPayload>

    /**
     * Ports iOS ClickMeAPI+ExpertBookings getExpertBookingDetails. The route ends in `/details`; the backend has no
     * GET on the bare booking id (RESEARCH C1).
     */
    @GET(ApiRequestUrls.EXPERT_BOOKING_DETAILS)
    suspend fun getExpertBookingDetails(
        @Path("id") bookingId: String,
    ): SuccessDataResponse<ExpertBookingDetail>

    /**
     * Ports iOS ClickMeAPI+ExpertBookings cancelExpertBooking. The backend schema is `.strict()` with `reason` as
     * its only key, so an explicit body is always passed. A second cancel on a cancelled booking is 409
     * `INVALID_STATUS`; only the lost race is `ALREADY_CANCELLED` (RESEARCH C8). The refund is in MAJOR units.
     */
    @POST(ApiRequestUrls.EXPERT_BOOKING_CANCEL)
    suspend fun cancelExpertBooking(
        @Path("id") bookingId: String,
        @Body body: CancelBookingRequest,
    ): SuccessDataResponse<CancelBookingData>

    /**
     * Ports iOS ClickMeAPI+ExpertBookings acceptExpertReschedule. The backend schema is `.strict()`, so an explicit
     * `{}` body is always passed. A repeat on an already confirmed booking is an idempotent 200 (RESEARCH C9).
     */
    @POST(ApiRequestUrls.EXPERT_BOOKING_ACCEPT_RESCHEDULE)
    suspend fun acceptExpertReschedule(
        @Path("id") bookingId: String,
        @Body body: AcceptRescheduleRequest,
    ): SuccessDataResponse<RescheduleResponseData>

    /**
     * Ports iOS ClickMeAPI+ExpertBookings declineExpertReschedule. The backend schema is `.strict()` (`reason`
     * only). A repeat on an already resolved booking is an idempotent 200 (RESEARCH C9).
     */
    @POST(ApiRequestUrls.EXPERT_BOOKING_DECLINE_RESCHEDULE)
    suspend fun declineExpertReschedule(
        @Path("id") bookingId: String,
        @Body body: DeclineRescheduleRequest,
    ): SuccessDataResponse<RescheduleResponseData>

    /**
     * Ports the iOS expert propose-a-reschedule call. The backend `rescheduleSchema` is `.strict()` and
     * [ExpertRescheduleProposalRequest] has no card property (experts are never charged). The response data is
     * `{booking_id, status}`.
     */
    @PATCH(ApiRequestUrls.EXPERT_BOOKING_RESCHEDULE)
    suspend fun rescheduleExpertBooking(
        @Path("id") bookingId: String,
        @Body body: ExpertRescheduleProposalRequest,
    ): SuccessDataResponse<RescheduleResponseData>

    /**
     * Ports iOS ClickMeAPI+ExpertEarnings getWeeklyEarnings. Amounts are integer MINOR units and `net_amount` is never
     * null. The optional `week_start` query is not sent: the screen always shows the current week.
     */
    @GET(ApiRequestUrls.EXPERT_EARNINGS_WEEKLY)
    suspend fun getWeeklyEarnings(): SuccessDataResponse<WeeklyEarningsData>

    /** Ports iOS ClickMeAPI+ExpertPayouts getPayoutSummary. MINOR units; 400 `NO_PAYOUT_METHOD` or `KYC_INCOMPLETE`. */
    @GET(ApiRequestUrls.EXPERT_PAYOUTS_SUMMARY)
    suspend fun getPayoutSummary(): SuccessDataResponse<PayoutSummaryData>

    /** Ports iOS ClickMeAPI+ExpertConnect getConnectStatus. 404 `NOT_FOUND` when no connected account exists yet. */
    @GET(ApiRequestUrls.EXPERT_CONNECT_STATUS)
    suspend fun getConnectStatus(): SuccessDataResponse<ConnectStatusData>

    /**
     * Ports iOS ClickMeAPI+ExpertConnect startConnectOnboarding. No `@Body`: the backend route has no schema (like
     * [joinCall]). The returned URL is a single-use Stripe Account Link.
     */
    @POST(ApiRequestUrls.EXPERT_CONNECT_ONBOARD)
    suspend fun startConnectOnboarding(): SuccessDataResponse<ConnectLinkData>

    /** Ports iOS ClickMeAPI+ExpertConnect refreshConnectLink. No `@Body`; 404 `NOT_FOUND` without a connected account. */
    @POST(ApiRequestUrls.EXPERT_CONNECT_REFRESH_LINK)
    suspend fun refreshConnectLink(): SuccessDataResponse<ConnectLinkData>

    /**
     * Ports iOS ClickMeAPI+Chats initiateChat. The body is `{peer_id}` only (the deprecated `expert_id` is never
     * sent). 201 for a new thread, 200 for an existing one.
     */
    @POST(ApiRequestUrls.CHATS_INITIATE)
    suspend fun initiateChat(
        @Body body: InitiateChatRequest,
    ): SuccessDataResponse<InitiateChatData>

    /** Ports iOS ClickMeAPI+Chats getChats. `limit` is capped at 100 by the backend. */
    @GET(ApiRequestUrls.CHATS)
    suspend fun getChats(
        @Query("page") page: Int,
        @Query("limit") limit: Int,
    ): SuccessDataResponse<ChatsPayload>

    /** Ports iOS ClickMeAPI+Chats getChatMessages. */
    @GET(ApiRequestUrls.CHAT_MESSAGES)
    suspend fun getChatMessages(
        @Path("id") threadId: String,
        @Query("page") page: Int,
        @Query("limit") limit: Int,
    ): SuccessDataResponse<ChatMessagesPayload>

    /** Ports iOS ClickMeAPI+Chats sendChatMessage. 403 `THREAD_BLOCKED` or `RECIPIENT_UNAVAILABLE` when refused. */
    @POST(ApiRequestUrls.CHAT_SEND)
    suspend fun sendChatMessage(
        @Path("id") threadId: String,
        @Body body: SendMessageRequest,
    ): SuccessDataResponse<ChatSendAckData>

    /**
     * Ports iOS ClickMeAPI+Chats block, unblock and report. The body is `{action}` for block and unblock and
     * `{action, reason, details?}` for report. Answers `{status: "success"}` with no `data`. 403 `NOT_BLOCKER` when
     * the caller did not block the thread.
     */
    @POST(ApiRequestUrls.CHAT_ACTIONS)
    suspend fun chatAction(
        @Path("id") threadId: String,
        @Body body: ChatActionRequest,
    ): StatusOnlyResponse

    /** Ports iOS ClickMeAPI+Chats reportUser. 201; 429 `TOO_MANY_REQUESTS` past the report limiter. */
    @POST(ApiRequestUrls.USER_REPORT)
    suspend fun reportUser(
        @Path("id") userId: String,
        @Body body: ReportUserRequest,
    ): SuccessDataResponse<ReportUserData>

    /**
     * Ports iOS NotificationsListViewModel load; first page only (D-16). The server excludes `message` rows and
     * answers newest first.
     */
    @GET(ApiRequestUrls.NOTIFICATIONS)
    suspend fun getNotifications(
        @Query("page") page: Int,
        @Query("limit") limit: Int,
    ): SuccessDataResponse<NotificationsPayload>

    /**
     * Ports iOS push registration. The backend `registerTokenSchema` is `.strict()` (see
     * [RegisterDeviceTokenRequest]). It replaces every token row of the caller with this one.
     */
    @POST(ApiRequestUrls.NOTIFICATION_TOKENS)
    suspend fun registerDeviceToken(
        @Body body: RegisterDeviceTokenRequest,
    ): SuccessMessageResponse

    /** Ports iOS push unregistration. The device id travels in the path; answers `{status, message}`. */
    @DELETE(ApiRequestUrls.NOTIFICATION_TOKEN)
    suspend fun deleteDeviceToken(
        @Path("device_id") deviceId: String,
    ): SuccessMessageResponse

    /** Ports iOS ClickMeAPI+Reviews getReviewContext. 404 unknown booking, 403 non-participant. */
    @GET(ApiRequestUrls.BOOKING_REVIEW_CONTEXT)
    suspend fun getReviewContext(
        @Path("id") bookingId: String,
    ): SuccessDataResponse<ReviewContextData>

    /**
     * Ports iOS ClickMeAPI+Reviews submitReview. The backend `submitReviewSchema` is `.strict()` (see
     * [SubmitReviewRequest]). 201 on success; 404 when the booking is not found or not completed.
     */
    @POST(ApiRequestUrls.BOOKING_REVIEWS)
    suspend fun submitReview(
        @Path("id") bookingId: String,
        @Body body: SubmitReviewRequest,
    ): SuccessDataResponse<SubmitReviewData>

    /**
     * Ports iOS ClickMeAPI+Account changePassword. 403 when the current password is wrong, 422 with `errors[]` naming
     * `current_password` or `new_password` when validation fails.
     */
    @PATCH(ApiRequestUrls.USER_PASSWORD)
    suspend fun updatePassword(
        @Body body: UpdatePasswordRequest,
    ): SuccessMessageResponse

    /**
     * Ports iOS ClickMeAPI+Account requestAccountDeletion: step one of the deletion (CONTEXT D-05). Answers a
     * single-use `deletion_token`. 401 with no code means a wrong password; 409 `EXPERT_HAS_UPCOMING_BOOKINGS` blocks
     * an expert with upcoming sessions; 429 past the limiter.
     */
    @POST(ApiRequestUrls.USER_ACCOUNT_DELETE_REQUEST)
    suspend fun requestAccountDeletion(
        @Body body: AccountDeletionRequest,
    ): SuccessDataResponse<DeletionRequestData>

    /**
     * Ports iOS ClickMeAPI+Account deleteAccount: step two. An HTTP DELETE that carries a JSON body, which a plain
     * `@DELETE` cannot do, hence `@HTTP(hasBody = true)` (RESEARCH Pattern 1). 400 `INVALID_TOKEN` for a stale or
     * reused deletion token, 409 for the same expert block as step one, 429 past the limiter.
     */
    @HTTP(method = "DELETE", path = ApiRequestUrls.USER_ACCOUNT, hasBody = true)
    suspend fun deleteAccount(
        @Body body: DeleteAccountRequest,
    ): SuccessMessageResponse

    /**
     * Ports iOS ClickMeAPI+UserProfile updateUserProfile. Partial merge: only the non-null groups of
     * [UpdateUserProfileRequest] are sent, and `basic.email` is never sent (403). Answers the full profile.
     */
    @PATCH(ApiRequestUrls.USER_PROFILE)
    suspend fun updateUserProfile(
        @Body body: UpdateUserProfileRequest,
    ): SuccessDataResponse<UserProfileData>

    /**
     * Ports iOS ClickMeAPI+ExpertProfile updateExpertProfile (meeting link only). Same route as
     * [updateExpertProfileTags]; the body carries only `professional_details.meeting_url`.
     */
    @PATCH(ApiRequestUrls.EXPERT_PROFILE)
    suspend fun updateExpertMeetingUrl(
        @Body body: UpdateExpertMeetingUrlRequest,
    ): SuccessMessageResponse

    /** Ports iOS ClickMeAPI+Help getFaqs: categories with their featured articles. */
    @GET(ApiRequestUrls.HELP_FAQS)
    suspend fun getFaqs(): SuccessDataResponse<FaqsPayload>

    /**
     * Ports iOS ClickMeAPI+ExpertEarnings getEarningsTransactions. Amounts are MINOR units. The server clamps `limit`
     * to 1..100 and a garbage `page` to 1.
     */
    @GET(ApiRequestUrls.EXPERT_EARNINGS_TRANSACTIONS)
    suspend fun getEarningsTransactions(
        @Query("page") page: Int,
        @Query("limit") limit: Int,
    ): SuccessDataResponse<EarningsTransactionsData>

    /**
     * Ports iOS `getClientTransactions`: the client's own payment history, newest first, one row per booking. Amounts
     * are MINOR units. A page past the end is an HTTP 500, not an empty list, so callers page forward on `has_more`
     * only (CLIENT-TRANSACTIONS.md section 7).
     */
    @GET(ApiRequestUrls.CLIENT_TRANSACTIONS)
    suspend fun getClientTransactions(
        @Query("page") page: Int,
        @Query("limit") limit: Int,
    ): SuccessDataResponse<ClientTransactionsData>

    /** Ports iOS `getNotificationPreferences`: one row per category and sub-category with the three channel flags. */
    @GET(ApiRequestUrls.NOTIFICATION_PREFERENCES)
    suspend fun getNotificationPreferences(): SuccessDataResponse<NotificationPreferencesData>

    /**
     * Ports iOS `updateNotificationPreferences`. The backend schema is `.strict()` at both levels, so a row carries
     * exactly the five documented keys. Answers `{status, message}`.
     */
    @PATCH(ApiRequestUrls.NOTIFICATION_PREFERENCES)
    suspend fun updateNotificationPreferences(
        @Body body: UpdateNotificationPreferencesRequest,
    ): SuccessMessageResponse

    /** Ports iOS ClickMeAPI+TaxProfile getTaxProfile. The TIN is never returned, only `tin_on_file`. */
    @GET(ApiRequestUrls.EXPERT_TAX_PROFILE)
    suspend fun getTaxProfile(): SuccessDataResponse<TaxProfileData>

    /**
     * Ports iOS ClickMeAPI+TaxProfile saveTaxProfile. The backend schema is `.strict()`. A first save without a TIN
     * answers `saved=false, tin_required=true` and writes nothing.
     */
    @POST(ApiRequestUrls.EXPERT_TAX_PROFILE)
    suspend fun saveTaxProfile(
        @Body body: SaveTaxProfileRequest,
    ): SuccessDataResponse<SaveTaxProfileData>
}
