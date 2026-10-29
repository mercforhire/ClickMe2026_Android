package com.uptrendinvestments.clickmeapp.testing

import com.uptrendinvestments.clickmeapp.model.requests.AcceptBookingRequestBody
import com.uptrendinvestments.clickmeapp.model.requests.AcceptRescheduleRequest
import com.uptrendinvestments.clickmeapp.model.requests.AccountDeletionRequest
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
import com.uptrendinvestments.clickmeapp.model.requests.ForgotPasswordRequest
import com.uptrendinvestments.clickmeapp.model.requests.InitiateChatRequest
import com.uptrendinvestments.clickmeapp.model.requests.LoginRequest
import com.uptrendinvestments.clickmeapp.model.requests.PaymentIntentRequest
import com.uptrendinvestments.clickmeapp.model.requests.RegisterDeviceTokenRequest
import com.uptrendinvestments.clickmeapp.model.requests.ReportUserRequest
import com.uptrendinvestments.clickmeapp.model.requests.ResetPasswordRequest
import com.uptrendinvestments.clickmeapp.model.requests.SendMessageRequest
import com.uptrendinvestments.clickmeapp.model.requests.SetupExpertProfileRequest
import com.uptrendinvestments.clickmeapp.model.requests.SignupRequest
import com.uptrendinvestments.clickmeapp.model.requests.SubmitReviewRequest
import com.uptrendinvestments.clickmeapp.model.requests.UpdateExpertTagsRequest
import com.uptrendinvestments.clickmeapp.model.requests.UpdateExpertTopicRequest
import com.uptrendinvestments.clickmeapp.model.requests.UpdateMyAvailabilityRequest
import com.uptrendinvestments.clickmeapp.model.requests.UpdatePasswordRequest
import com.uptrendinvestments.clickmeapp.model.requests.VerifyEmailCodeRequest
import com.uptrendinvestments.clickmeapp.model.responses.AcceptBookingRequestData
import com.uptrendinvestments.clickmeapp.model.responses.AvatarUploadData
import com.uptrendinvestments.clickmeapp.model.responses.BookingRequestData
import com.uptrendinvestments.clickmeapp.model.responses.BookingRequestDetail
import com.uptrendinvestments.clickmeapp.model.responses.BookingRequestItem
import com.uptrendinvestments.clickmeapp.model.responses.BookingRequestsPayload
import com.uptrendinvestments.clickmeapp.model.responses.CancelBookingData
import com.uptrendinvestments.clickmeapp.model.responses.CategoriesPayload
import com.uptrendinvestments.clickmeapp.model.responses.ChatMessagesPayload
import com.uptrendinvestments.clickmeapp.model.responses.ChatSendAckData
import com.uptrendinvestments.clickmeapp.model.responses.ChatsPayload
import com.uptrendinvestments.clickmeapp.model.responses.ClientBookingDetail
import com.uptrendinvestments.clickmeapp.model.responses.ClientBookingsPayload
import com.uptrendinvestments.clickmeapp.model.responses.ClientHomeData
import com.uptrendinvestments.clickmeapp.model.responses.ConfirmBookingData
import com.uptrendinvestments.clickmeapp.model.responses.ConfirmConnectionData
import com.uptrendinvestments.clickmeapp.model.responses.ConnectLinkData
import com.uptrendinvestments.clickmeapp.model.responses.ConnectStatusData
import com.uptrendinvestments.clickmeapp.model.responses.DeclineBookingRequestData
import com.uptrendinvestments.clickmeapp.model.responses.DeletionRequestData
import com.uptrendinvestments.clickmeapp.model.responses.EmailStatusData
import com.uptrendinvestments.clickmeapp.model.responses.EndCallData
import com.uptrendinvestments.clickmeapp.model.responses.ExpertAvailabilityData
import com.uptrendinvestments.clickmeapp.model.responses.ExpertAvailabilityScheduleData
import com.uptrendinvestments.clickmeapp.model.responses.ExpertBookingDetail
import com.uptrendinvestments.clickmeapp.model.responses.ExpertBookingsPayload
import com.uptrendinvestments.clickmeapp.model.responses.ExpertProfileData
import com.uptrendinvestments.clickmeapp.model.responses.ExpertReviewsPayload
import com.uptrendinvestments.clickmeapp.model.responses.ExpertiseTagsPayload
import com.uptrendinvestments.clickmeapp.model.responses.InitiateChatData
import com.uptrendinvestments.clickmeapp.model.responses.JoinCallResponse
import com.uptrendinvestments.clickmeapp.model.responses.LanguagesPayload
import com.uptrendinvestments.clickmeapp.model.responses.LoginData
import com.uptrendinvestments.clickmeapp.model.responses.MeData
import com.uptrendinvestments.clickmeapp.model.responses.MyExpertTopicPayload
import com.uptrendinvestments.clickmeapp.model.responses.MyExpertTopicsPayload
import com.uptrendinvestments.clickmeapp.model.responses.NotificationsPayload
import com.uptrendinvestments.clickmeapp.model.responses.PaymentIntentResponse
import com.uptrendinvestments.clickmeapp.model.responses.PaymentMethodSetupData
import com.uptrendinvestments.clickmeapp.model.responses.PaymentMethodsData
import com.uptrendinvestments.clickmeapp.model.responses.PayoutSummaryData
import com.uptrendinvestments.clickmeapp.model.responses.PublicProfileDetailsData
import com.uptrendinvestments.clickmeapp.model.responses.ReportUserData
import com.uptrendinvestments.clickmeapp.model.responses.RescheduleResponseData
import com.uptrendinvestments.clickmeapp.model.responses.ReviewContextData
import com.uptrendinvestments.clickmeapp.model.responses.SetupExpertProfileData
import com.uptrendinvestments.clickmeapp.model.responses.SignupData
import com.uptrendinvestments.clickmeapp.model.responses.StatusOnlyResponse
import com.uptrendinvestments.clickmeapp.model.responses.SubmitReviewData
import com.uptrendinvestments.clickmeapp.model.responses.SuccessDataResponse
import com.uptrendinvestments.clickmeapp.model.responses.SuccessMessageResponse
import com.uptrendinvestments.clickmeapp.model.responses.UserProfileData
import com.uptrendinvestments.clickmeapp.model.responses.WeeklyEarningsData
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
    var getPaymentMethodsHandler: suspend () -> PaymentMethodsData = { notStubbed() }
    var setupIntentHandler: suspend () -> PaymentMethodSetupData = { notStubbed() }
    var deletePaymentMethodHandler: suspend (String) -> SuccessMessageResponse = { notStubbed() }
    var setDefaultPaymentMethodHandler: suspend (String) -> SuccessMessageResponse = { notStubbed() }
    var paymentIntentHandler: suspend (PaymentIntentRequest) -> PaymentIntentResponse = { notStubbed() }
    var confirmBookingHandler: suspend (ConfirmBookingRequest) -> ConfirmBookingData = { notStubbed() }
    var cancelClientBookingHandler: suspend (String, CancelBookingRequest) -> CancelBookingData =
        { _, _ -> notStubbed() }
    var acceptClientRescheduleHandler: suspend (String, AcceptRescheduleRequest) -> RescheduleResponseData =
        { _, _ -> notStubbed() }
    var declineClientRescheduleHandler: suspend (String, DeclineRescheduleRequest) -> RescheduleResponseData =
        { _, _ -> notStubbed() }
    var joinCallHandler: suspend (String) -> JoinCallResponse = { notStubbed() }
    var confirmCallConnectionHandler: suspend (String, ConfirmConnectionRequest) -> ConfirmConnectionData =
        { _, _ -> notStubbed() }
    var endCallHandler: suspend (String, EndCallRequest) -> EndCallData = { _, _ -> notStubbed() }
    var createTopicHandler: suspend (CreateExpertTopicRequest) -> MyExpertTopicPayload = { notStubbed() }
    var updateTopicHandler: suspend (String, UpdateExpertTopicRequest) -> MyExpertTopicPayload =
        { _, _ -> notStubbed() }
    var deleteTopicHandler: suspend (String) -> SuccessMessageResponse = { notStubbed() }
    var myAvailabilityHandler: suspend () -> ExpertAvailabilityScheduleData = { notStubbed() }
    var updateMyAvailabilityHandler: suspend (UpdateMyAvailabilityRequest) -> SuccessMessageResponse =
        { notStubbed() }
    var updateExpertProfileTagsHandler: suspend (UpdateExpertTagsRequest) -> SuccessMessageResponse =
        { notStubbed() }
    var bookingRequestsHandler: suspend () -> List<BookingRequestItem> = { notStubbed() }
    var bookingRequestDetailHandler: suspend (String) -> BookingRequestDetail = { notStubbed() }
    var acceptBookingRequestHandler: suspend (String, AcceptBookingRequestBody) -> AcceptBookingRequestData =
        { _, _ -> notStubbed() }
    var declineBookingRequestHandler: suspend (String, DeclineBookingRequestBody) -> DeclineBookingRequestData =
        { _, _ -> notStubbed() }
    var expertBookingsHandler: suspend (type: String) -> ExpertBookingsPayload = { notStubbed() }
    var expertBookingDetailsHandler: suspend (String) -> ExpertBookingDetail = { notStubbed() }
    var cancelExpertBookingHandler: suspend (String, CancelBookingRequest) -> CancelBookingData =
        { _, _ -> notStubbed() }
    var acceptExpertRescheduleHandler: suspend (String, AcceptRescheduleRequest) -> RescheduleResponseData =
        { _, _ -> notStubbed() }
    var declineExpertRescheduleHandler: suspend (String, DeclineRescheduleRequest) -> RescheduleResponseData =
        { _, _ -> notStubbed() }
    var weeklyEarningsHandler: suspend () -> WeeklyEarningsData = { notStubbed() }
    var payoutSummaryHandler: suspend () -> PayoutSummaryData = { notStubbed() }
    var connectStatusHandler: suspend () -> ConnectStatusData = { notStubbed() }
    var connectOnboardHandler: suspend () -> ConnectLinkData = { notStubbed() }
    var connectRefreshLinkHandler: suspend () -> ConnectLinkData = { notStubbed() }
    var initiateChatHandler: suspend (InitiateChatRequest) -> InitiateChatData = { notStubbed() }
    var chatsHandler: suspend (page: Int, limit: Int) -> ChatsPayload = { _, _ -> notStubbed() }
    var chatMessagesHandler: suspend (threadId: String, page: Int, limit: Int) -> ChatMessagesPayload =
        { _, _, _ -> notStubbed() }
    var sendChatMessageHandler: suspend (threadId: String, body: SendMessageRequest) -> ChatSendAckData =
        { _, _ -> notStubbed() }
    var chatActionHandler: suspend (threadId: String, body: ChatActionRequest) -> StatusOnlyResponse =
        { _, _ -> StatusOnlyResponse("success") }
    var reportUserHandler: suspend (userId: String, body: ReportUserRequest) -> ReportUserData =
        { _, _ -> notStubbed() }
    var notificationsHandler: suspend (page: Int, limit: Int) -> NotificationsPayload = { _, _ -> notStubbed() }
    var registerDeviceTokenHandler: suspend (RegisterDeviceTokenRequest) -> SuccessMessageResponse =
        { SuccessMessageResponse("success") }
    var deleteDeviceTokenHandler: suspend (String) -> SuccessMessageResponse = { SuccessMessageResponse("success") }
    var reviewContextHandler: suspend (bookingId: String) -> ReviewContextData = { notStubbed() }
    var submitReviewHandler: suspend (bookingId: String, body: SubmitReviewRequest) -> SubmitReviewData =
        { _, _ -> notStubbed() }

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

    @Volatile var getPaymentMethodsCalls = 0
        private set

    @Volatile var setupIntentCalls = 0
        private set

    @Volatile var deletePaymentMethodCalls = 0
        private set

    @Volatile var setDefaultPaymentMethodCalls = 0
        private set

    @Volatile var paymentIntentCalls = 0
        private set

    @Volatile var confirmBookingCalls = 0
        private set

    @Volatile var cancelClientBookingCalls = 0
        private set

    @Volatile var acceptClientRescheduleCalls = 0
        private set

    @Volatile var declineClientRescheduleCalls = 0
        private set

    @Volatile var joinCallCalls = 0
        private set

    @Volatile var confirmCallConnectionCalls = 0
        private set

    @Volatile var endCallCalls = 0
        private set

    @Volatile var createTopicCalls = 0
        private set

    @Volatile var updateTopicCalls = 0
        private set

    @Volatile var deleteTopicCalls = 0
        private set

    @Volatile var myAvailabilityCalls = 0
        private set

    @Volatile var updateMyAvailabilityCalls = 0
        private set

    @Volatile var updateExpertProfileTagsCalls = 0
        private set

    @Volatile var bookingRequestsCalls = 0
        private set

    @Volatile var bookingRequestDetailCalls = 0
        private set

    @Volatile var acceptBookingRequestCalls = 0
        private set

    @Volatile var declineBookingRequestCalls = 0
        private set

    @Volatile var expertBookingsCalls = 0
        private set

    @Volatile var expertBookingDetailsCalls = 0
        private set

    @Volatile var cancelExpertBookingCalls = 0
        private set

    @Volatile var acceptExpertRescheduleCalls = 0
        private set

    @Volatile var declineExpertRescheduleCalls = 0
        private set

    @Volatile var weeklyEarningsCalls = 0
        private set

    @Volatile var payoutSummaryCalls = 0
        private set

    @Volatile var connectStatusCalls = 0
        private set

    @Volatile var connectOnboardCalls = 0
        private set

    @Volatile var connectRefreshLinkCalls = 0
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

    private val _paymentIntentRequests = mutableListOf<PaymentIntentRequest>()

    /** Every POST /bookings/payment-intent body received, in call order. */
    val paymentIntentRequests: List<PaymentIntentRequest> get() = _paymentIntentRequests

    private val _confirmBookingRequests = mutableListOf<ConfirmBookingRequest>()

    /** Every POST /bookings/confirm body received, in call order. */
    val confirmBookingRequests: List<ConfirmBookingRequest> get() = _confirmBookingRequests

    private val _cancelClientBookingRequests = mutableListOf<Pair<String, CancelBookingRequest>>()

    /** Every POST /client/bookings/{id}/cancel call (id, body), in call order. */
    val cancelClientBookingRequests: List<Pair<String, CancelBookingRequest>>
        get() = synchronized(_cancelClientBookingRequests) { _cancelClientBookingRequests.toList() }

    private val _acceptClientRescheduleRequests = mutableListOf<Pair<String, AcceptRescheduleRequest>>()

    /** Every POST /client/bookings/{id}/accept-reschedule call (id, body), in call order. */
    val acceptClientRescheduleRequests: List<Pair<String, AcceptRescheduleRequest>>
        get() = synchronized(_acceptClientRescheduleRequests) { _acceptClientRescheduleRequests.toList() }

    private val _declineClientRescheduleRequests = mutableListOf<Pair<String, DeclineRescheduleRequest>>()

    /** Every POST /client/bookings/{id}/decline-reschedule call (id, body), in call order. */
    val declineClientRescheduleRequests: List<Pair<String, DeclineRescheduleRequest>>
        get() = synchronized(_declineClientRescheduleRequests) { _declineClientRescheduleRequests.toList() }

    private val _joinCallRequests = mutableListOf<String>()

    /** Every POST /bookings/{id}/join booking id, in call order. */
    val joinCallRequests: List<String>
        get() = synchronized(_joinCallRequests) { _joinCallRequests.toList() }

    private val _confirmCallConnectionRequests = mutableListOf<Pair<String, ConfirmConnectionRequest>>()

    /** Every PATCH /bookings/{id}/confirm-connection call (id, body), in call order. */
    val confirmCallConnectionRequests: List<Pair<String, ConfirmConnectionRequest>>
        get() = synchronized(_confirmCallConnectionRequests) { _confirmCallConnectionRequests.toList() }

    private val _endCallRequests = mutableListOf<Pair<String, EndCallRequest>>()

    /** Every POST /bookings/{id}/end call (id, body), in call order. */
    val endCallRequests: List<Pair<String, EndCallRequest>>
        get() = synchronized(_endCallRequests) { _endCallRequests.toList() }

    private val _createTopicRequests = mutableListOf<CreateExpertTopicRequest>()

    /** Every POST /expert/topics body received, in call order. */
    val createTopicRequests: List<CreateExpertTopicRequest>
        get() = synchronized(_createTopicRequests) { _createTopicRequests.toList() }

    private val _updateTopicRequests = mutableListOf<Pair<String, UpdateExpertTopicRequest>>()

    /** Every PATCH /expert/topics/{id} call (id, body), in call order. */
    val updateTopicRequests: List<Pair<String, UpdateExpertTopicRequest>>
        get() = synchronized(_updateTopicRequests) { _updateTopicRequests.toList() }

    private val _deletedTopicIds = mutableListOf<String>()

    /** The id of every DELETE /expert/topics/{id} call, in call order. */
    val deletedTopicIds: List<String>
        get() = synchronized(_deletedTopicIds) { _deletedTopicIds.toList() }

    private val _updateMyAvailabilityRequests = mutableListOf<UpdateMyAvailabilityRequest>()

    /** Every PUT /expert/availability body received, in call order. */
    val updateMyAvailabilityRequests: List<UpdateMyAvailabilityRequest>
        get() = synchronized(_updateMyAvailabilityRequests) { _updateMyAvailabilityRequests.toList() }

    private val _updateExpertProfileTagsRequests = mutableListOf<UpdateExpertTagsRequest>()

    /** Every PATCH /expert/profile tags body received, in call order. */
    val updateExpertProfileTagsRequests: List<UpdateExpertTagsRequest>
        get() = synchronized(_updateExpertProfileTagsRequests) { _updateExpertProfileTagsRequests.toList() }

    private val _acceptBookingRequestRequests = mutableListOf<Pair<String, AcceptBookingRequestBody>>()

    /** Every POST /expert/booking-requests/{id}/accept call (id, body), in call order. */
    val acceptBookingRequestRequests: List<Pair<String, AcceptBookingRequestBody>>
        get() = synchronized(_acceptBookingRequestRequests) { _acceptBookingRequestRequests.toList() }

    private val _declineBookingRequestRequests = mutableListOf<Pair<String, DeclineBookingRequestBody>>()

    /** Every POST /expert/booking-requests/{id}/decline call (id, body), in call order. */
    val declineBookingRequestRequests: List<Pair<String, DeclineBookingRequestBody>>
        get() = synchronized(_declineBookingRequestRequests) { _declineBookingRequestRequests.toList() }

    private val _expertBookingsQueries = mutableListOf<Triple<String, Int, Int>>()

    /** The (type, page, limit) of every getExpertBookings call, in call order. */
    val expertBookingsQueries: List<Triple<String, Int, Int>>
        get() = synchronized(_expertBookingsQueries) { _expertBookingsQueries.toList() }

    private val _cancelExpertBookingRequests = mutableListOf<Pair<String, CancelBookingRequest>>()

    /** Every POST /expert/bookings/{id}/cancel call (id, body), in call order. */
    val cancelExpertBookingRequests: List<Pair<String, CancelBookingRequest>>
        get() = synchronized(_cancelExpertBookingRequests) { _cancelExpertBookingRequests.toList() }

    private val _acceptExpertRescheduleRequests = mutableListOf<Pair<String, AcceptRescheduleRequest>>()

    /** Every POST /expert/bookings/{id}/accept-reschedule call (id, body), in call order. */
    val acceptExpertRescheduleRequests: List<Pair<String, AcceptRescheduleRequest>>
        get() = synchronized(_acceptExpertRescheduleRequests) { _acceptExpertRescheduleRequests.toList() }

    private val _declineExpertRescheduleRequests = mutableListOf<Pair<String, DeclineRescheduleRequest>>()

    /** Every POST /expert/bookings/{id}/decline-reschedule call (id, body), in call order. */
    val declineExpertRescheduleRequests: List<Pair<String, DeclineRescheduleRequest>>
        get() = synchronized(_declineExpertRescheduleRequests) { _declineExpertRescheduleRequests.toList() }

    private val _initiateChatRequests = mutableListOf<InitiateChatRequest>()

    /** Every POST /chats/initiate body received, in call order. */
    val initiateChatRequests: List<InitiateChatRequest>
        get() = synchronized(_initiateChatRequests) { _initiateChatRequests.toList() }

    private val _chatsQueries = mutableListOf<Pair<Int, Int>>()

    /** The (page, limit) of every getChats call, in call order. */
    val chatsQueries: List<Pair<Int, Int>>
        get() = synchronized(_chatsQueries) { _chatsQueries.toList() }

    private val _chatMessagesQueries = mutableListOf<Triple<String, Int, Int>>()

    /** The (threadId, page, limit) of every getChatMessages call, in call order. */
    val chatMessagesQueries: List<Triple<String, Int, Int>>
        get() = synchronized(_chatMessagesQueries) { _chatMessagesQueries.toList() }

    private val _sentChatMessages = mutableListOf<Pair<String, SendMessageRequest>>()

    /** Every POST /chats/{id}/send call (threadId, body), in call order. */
    val sentChatMessages: List<Pair<String, SendMessageRequest>>
        get() = synchronized(_sentChatMessages) { _sentChatMessages.toList() }

    private val _chatActions = mutableListOf<Pair<String, ChatActionRequest>>()

    /** Every POST /chats/{id}/actions call (threadId, body), in call order. */
    val chatActions: List<Pair<String, ChatActionRequest>>
        get() = synchronized(_chatActions) { _chatActions.toList() }

    private val _reportUserRequests = mutableListOf<Pair<String, ReportUserRequest>>()

    /** Every POST /users/{id}/report call (userId, body), in call order. */
    val reportUserRequests: List<Pair<String, ReportUserRequest>>
        get() = synchronized(_reportUserRequests) { _reportUserRequests.toList() }

    private val _notificationsQueries = mutableListOf<Pair<Int, Int>>()

    /** The (page, limit) of every getNotifications call, in call order. */
    val notificationsQueries: List<Pair<Int, Int>>
        get() = synchronized(_notificationsQueries) { _notificationsQueries.toList() }

    private val _registeredDeviceTokens = mutableListOf<RegisterDeviceTokenRequest>()

    /** Every POST /notifications/tokens body received, in call order. */
    val registeredDeviceTokens: List<RegisterDeviceTokenRequest>
        get() = synchronized(_registeredDeviceTokens) { _registeredDeviceTokens.toList() }

    private val _deletedDeviceTokens = mutableListOf<String>()

    /** The device id of every DELETE /notifications/tokens/{device_id} call, in call order. */
    val deletedDeviceTokens: List<String>
        get() = synchronized(_deletedDeviceTokens) { _deletedDeviceTokens.toList() }

    private val _reviewContextRequests = mutableListOf<String>()

    /** The booking id of every getReviewContext call, in call order. */
    val reviewContextRequests: List<String>
        get() = synchronized(_reviewContextRequests) { _reviewContextRequests.toList() }

    private val _submittedReviews = mutableListOf<Pair<String, SubmitReviewRequest>>()

    /** Every POST /bookings/{id}/reviews call (bookingId, body), in call order. */
    val submittedReviews: List<Pair<String, SubmitReviewRequest>>
        get() = synchronized(_submittedReviews) { _submittedReviews.toList() }

    private val _clientBookingsQueries = mutableListOf<Triple<String, Int, Int>>()

    /** The (type, page, limit) of every getClientBookings call, in call order. */
    val clientBookingsQueries: List<Triple<String, Int, Int>>
        get() = synchronized(_clientBookingsQueries) { _clientBookingsQueries.toList() }

    private val _deletedPaymentMethodIds = mutableListOf<String>()

    /** The id of every DELETE /me/payment-methods/{id} call, in call order. */
    val deletedPaymentMethodIds: List<String> get() = _deletedPaymentMethodIds

    private val _defaultedPaymentMethodIds = mutableListOf<String>()

    /** The id of every PATCH /me/payment-methods/{id}/default call, in call order. */
    val defaultedPaymentMethodIds: List<String> get() = _defaultedPaymentMethodIds

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
        synchronized(_clientBookingsQueries) { _clientBookingsQueries += Triple(type, page, limit) }
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

    override suspend fun getPaymentMethods(): SuccessDataResponse<PaymentMethodsData> {
        getPaymentMethodsCalls++
        return ok(getPaymentMethodsHandler())
    }

    override suspend fun createPaymentMethodSetupIntent(
        body: Map<String, String>,
    ): SuccessDataResponse<PaymentMethodSetupData> {
        setupIntentCalls++
        return ok(setupIntentHandler())
    }

    override suspend fun deletePaymentMethod(paymentMethodId: String): SuccessMessageResponse {
        deletePaymentMethodCalls++
        _deletedPaymentMethodIds += paymentMethodId
        return deletePaymentMethodHandler(paymentMethodId)
    }

    override suspend fun setDefaultPaymentMethod(
        paymentMethodId: String,
        body: Map<String, String>,
    ): SuccessMessageResponse {
        setDefaultPaymentMethodCalls++
        _defaultedPaymentMethodIds += paymentMethodId
        return setDefaultPaymentMethodHandler(paymentMethodId)
    }

    /** Bare body: returns the handler result without the success envelope. */
    override suspend fun createPaymentIntent(body: PaymentIntentRequest): PaymentIntentResponse {
        paymentIntentCalls++
        _paymentIntentRequests += body
        return paymentIntentHandler(body)
    }

    override suspend fun confirmBooking(body: ConfirmBookingRequest): SuccessDataResponse<ConfirmBookingData> {
        confirmBookingCalls++
        _confirmBookingRequests += body
        return ok(confirmBookingHandler(body))
    }

    override suspend fun cancelClientBooking(
        bookingId: String,
        body: CancelBookingRequest,
    ): SuccessDataResponse<CancelBookingData> {
        cancelClientBookingCalls++
        synchronized(_cancelClientBookingRequests) { _cancelClientBookingRequests += bookingId to body }
        return ok(cancelClientBookingHandler(bookingId, body))
    }

    override suspend fun acceptClientReschedule(
        bookingId: String,
        body: AcceptRescheduleRequest,
    ): SuccessDataResponse<RescheduleResponseData> {
        acceptClientRescheduleCalls++
        synchronized(_acceptClientRescheduleRequests) { _acceptClientRescheduleRequests += bookingId to body }
        return ok(acceptClientRescheduleHandler(bookingId, body))
    }

    override suspend fun declineClientReschedule(
        bookingId: String,
        body: DeclineRescheduleRequest,
    ): SuccessDataResponse<RescheduleResponseData> {
        declineClientRescheduleCalls++
        synchronized(_declineClientRescheduleRequests) { _declineClientRescheduleRequests += bookingId to body }
        return ok(declineClientRescheduleHandler(bookingId, body))
    }

    override suspend fun joinCall(bookingId: String): SuccessDataResponse<JoinCallResponse> {
        joinCallCalls++
        synchronized(_joinCallRequests) { _joinCallRequests += bookingId }
        return ok(joinCallHandler(bookingId))
    }

    override suspend fun confirmCallConnection(
        bookingId: String,
        body: ConfirmConnectionRequest,
    ): SuccessDataResponse<ConfirmConnectionData> {
        confirmCallConnectionCalls++
        synchronized(_confirmCallConnectionRequests) { _confirmCallConnectionRequests += bookingId to body }
        return ok(confirmCallConnectionHandler(bookingId, body))
    }

    override suspend fun endCall(
        bookingId: String,
        body: EndCallRequest,
    ): SuccessDataResponse<EndCallData> {
        endCallCalls++
        synchronized(_endCallRequests) { _endCallRequests += bookingId to body }
        return ok(endCallHandler(bookingId, body))
    }

    override suspend fun createTopic(body: CreateExpertTopicRequest): SuccessDataResponse<MyExpertTopicPayload> {
        createTopicCalls++
        synchronized(_createTopicRequests) { _createTopicRequests += body }
        return ok(createTopicHandler(body))
    }

    override suspend fun updateTopic(
        topicId: String,
        body: UpdateExpertTopicRequest,
    ): SuccessDataResponse<MyExpertTopicPayload> {
        updateTopicCalls++
        synchronized(_updateTopicRequests) { _updateTopicRequests += topicId to body }
        return ok(updateTopicHandler(topicId, body))
    }

    override suspend fun deleteTopic(topicId: String): SuccessMessageResponse {
        deleteTopicCalls++
        synchronized(_deletedTopicIds) { _deletedTopicIds += topicId }
        return deleteTopicHandler(topicId)
    }

    override suspend fun getMyAvailability(): SuccessDataResponse<ExpertAvailabilityScheduleData> {
        myAvailabilityCalls++
        return ok(myAvailabilityHandler())
    }

    override suspend fun updateMyAvailability(body: UpdateMyAvailabilityRequest): SuccessMessageResponse {
        updateMyAvailabilityCalls++
        synchronized(_updateMyAvailabilityRequests) { _updateMyAvailabilityRequests += body }
        return updateMyAvailabilityHandler(body)
    }

    override suspend fun updateExpertProfileTags(body: UpdateExpertTagsRequest): SuccessMessageResponse {
        updateExpertProfileTagsCalls++
        synchronized(_updateExpertProfileTagsRequests) { _updateExpertProfileTagsRequests += body }
        return updateExpertProfileTagsHandler(body)
    }

    override suspend fun getBookingRequests(): SuccessDataResponse<BookingRequestsPayload> {
        bookingRequestsCalls++
        return ok(BookingRequestsPayload(requests = bookingRequestsHandler()))
    }

    override suspend fun getBookingRequest(requestId: String): SuccessDataResponse<BookingRequestDetail> {
        bookingRequestDetailCalls++
        return ok(bookingRequestDetailHandler(requestId))
    }

    override suspend fun acceptBookingRequest(
        requestId: String,
        body: AcceptBookingRequestBody,
    ): SuccessDataResponse<AcceptBookingRequestData> {
        acceptBookingRequestCalls++
        synchronized(_acceptBookingRequestRequests) { _acceptBookingRequestRequests += requestId to body }
        return ok(acceptBookingRequestHandler(requestId, body))
    }

    override suspend fun declineBookingRequest(
        requestId: String,
        body: DeclineBookingRequestBody,
    ): SuccessDataResponse<DeclineBookingRequestData> {
        declineBookingRequestCalls++
        synchronized(_declineBookingRequestRequests) { _declineBookingRequestRequests += requestId to body }
        return ok(declineBookingRequestHandler(requestId, body))
    }

    override suspend fun getExpertBookings(
        type: String,
        page: Int,
        limit: Int,
    ): SuccessDataResponse<ExpertBookingsPayload> {
        expertBookingsCalls++
        synchronized(_expertBookingsQueries) { _expertBookingsQueries += Triple(type, page, limit) }
        return ok(expertBookingsHandler(type))
    }

    override suspend fun getExpertBookingDetails(bookingId: String): SuccessDataResponse<ExpertBookingDetail> {
        expertBookingDetailsCalls++
        return ok(expertBookingDetailsHandler(bookingId))
    }

    override suspend fun cancelExpertBooking(
        bookingId: String,
        body: CancelBookingRequest,
    ): SuccessDataResponse<CancelBookingData> {
        cancelExpertBookingCalls++
        synchronized(_cancelExpertBookingRequests) { _cancelExpertBookingRequests += bookingId to body }
        return ok(cancelExpertBookingHandler(bookingId, body))
    }

    override suspend fun acceptExpertReschedule(
        bookingId: String,
        body: AcceptRescheduleRequest,
    ): SuccessDataResponse<RescheduleResponseData> {
        acceptExpertRescheduleCalls++
        synchronized(_acceptExpertRescheduleRequests) { _acceptExpertRescheduleRequests += bookingId to body }
        return ok(acceptExpertRescheduleHandler(bookingId, body))
    }

    override suspend fun declineExpertReschedule(
        bookingId: String,
        body: DeclineRescheduleRequest,
    ): SuccessDataResponse<RescheduleResponseData> {
        declineExpertRescheduleCalls++
        synchronized(_declineExpertRescheduleRequests) { _declineExpertRescheduleRequests += bookingId to body }
        return ok(declineExpertRescheduleHandler(bookingId, body))
    }

    override suspend fun getWeeklyEarnings(): SuccessDataResponse<WeeklyEarningsData> {
        weeklyEarningsCalls++
        return ok(weeklyEarningsHandler())
    }

    override suspend fun getPayoutSummary(): SuccessDataResponse<PayoutSummaryData> {
        payoutSummaryCalls++
        return ok(payoutSummaryHandler())
    }

    override suspend fun getConnectStatus(): SuccessDataResponse<ConnectStatusData> {
        connectStatusCalls++
        return ok(connectStatusHandler())
    }

    override suspend fun startConnectOnboarding(): SuccessDataResponse<ConnectLinkData> {
        connectOnboardCalls++
        return ok(connectOnboardHandler())
    }

    override suspend fun refreshConnectLink(): SuccessDataResponse<ConnectLinkData> {
        connectRefreshLinkCalls++
        return ok(connectRefreshLinkHandler())
    }

    override suspend fun initiateChat(body: InitiateChatRequest): SuccessDataResponse<InitiateChatData> {
        synchronized(_initiateChatRequests) { _initiateChatRequests += body }
        return ok(initiateChatHandler(body))
    }

    override suspend fun getChats(
        page: Int,
        limit: Int,
    ): SuccessDataResponse<ChatsPayload> {
        synchronized(_chatsQueries) { _chatsQueries += page to limit }
        return ok(chatsHandler(page, limit))
    }

    override suspend fun getChatMessages(
        threadId: String,
        page: Int,
        limit: Int,
    ): SuccessDataResponse<ChatMessagesPayload> {
        synchronized(_chatMessagesQueries) { _chatMessagesQueries += Triple(threadId, page, limit) }
        return ok(chatMessagesHandler(threadId, page, limit))
    }

    override suspend fun sendChatMessage(
        threadId: String,
        body: SendMessageRequest,
    ): SuccessDataResponse<ChatSendAckData> {
        synchronized(_sentChatMessages) { _sentChatMessages += threadId to body }
        return ok(sendChatMessageHandler(threadId, body))
    }

    override suspend fun chatAction(
        threadId: String,
        body: ChatActionRequest,
    ): StatusOnlyResponse {
        synchronized(_chatActions) { _chatActions += threadId to body }
        return chatActionHandler(threadId, body)
    }

    override suspend fun reportUser(
        userId: String,
        body: ReportUserRequest,
    ): SuccessDataResponse<ReportUserData> {
        synchronized(_reportUserRequests) { _reportUserRequests += userId to body }
        return ok(reportUserHandler(userId, body))
    }

    override suspend fun getNotifications(
        page: Int,
        limit: Int,
    ): SuccessDataResponse<NotificationsPayload> {
        synchronized(_notificationsQueries) { _notificationsQueries += page to limit }
        return ok(notificationsHandler(page, limit))
    }

    override suspend fun registerDeviceToken(body: RegisterDeviceTokenRequest): SuccessMessageResponse {
        synchronized(_registeredDeviceTokens) { _registeredDeviceTokens += body }
        return registerDeviceTokenHandler(body)
    }

    override suspend fun deleteDeviceToken(deviceId: String): SuccessMessageResponse {
        synchronized(_deletedDeviceTokens) { _deletedDeviceTokens += deviceId }
        return deleteDeviceTokenHandler(deviceId)
    }

    override suspend fun getReviewContext(bookingId: String): SuccessDataResponse<ReviewContextData> {
        synchronized(_reviewContextRequests) { _reviewContextRequests += bookingId }
        return ok(reviewContextHandler(bookingId))
    }

    override suspend fun submitReview(
        bookingId: String,
        body: SubmitReviewRequest,
    ): SuccessDataResponse<SubmitReviewData> {
        synchronized(_submittedReviews) { _submittedReviews += bookingId to body }
        return ok(submitReviewHandler(bookingId, body))
    }

    // Phase 14: account security and deletion.
    var updatePasswordHandler: suspend (UpdatePasswordRequest) -> SuccessMessageResponse = { notStubbed() }
    var requestAccountDeletionHandler: suspend (AccountDeletionRequest) -> DeletionRequestData = { notStubbed() }
    var deleteAccountHandler: suspend (DeleteAccountRequest) -> SuccessMessageResponse = { notStubbed() }

    @Volatile var updatePasswordCalls = 0
        private set

    @Volatile var requestAccountDeletionCalls = 0
        private set

    @Volatile var deleteAccountCalls = 0
        private set

    private val _updatePasswordRequests = mutableListOf<UpdatePasswordRequest>()
    private val _accountDeletionRequests = mutableListOf<AccountDeletionRequest>()
    private val _deleteAccountRequests = mutableListOf<DeleteAccountRequest>()

    val updatePasswordRequests: List<UpdatePasswordRequest>
        get() = synchronized(_updatePasswordRequests) { _updatePasswordRequests.toList() }
    val accountDeletionRequests: List<AccountDeletionRequest>
        get() = synchronized(_accountDeletionRequests) { _accountDeletionRequests.toList() }
    val deleteAccountRequests: List<DeleteAccountRequest>
        get() = synchronized(_deleteAccountRequests) { _deleteAccountRequests.toList() }

    override suspend fun updatePassword(body: UpdatePasswordRequest): SuccessMessageResponse {
        updatePasswordCalls++
        synchronized(_updatePasswordRequests) { _updatePasswordRequests += body }
        return updatePasswordHandler(body)
    }

    override suspend fun requestAccountDeletion(
        body: AccountDeletionRequest,
    ): SuccessDataResponse<DeletionRequestData> {
        requestAccountDeletionCalls++
        synchronized(_accountDeletionRequests) { _accountDeletionRequests += body }
        return ok(requestAccountDeletionHandler(body))
    }

    override suspend fun deleteAccount(body: DeleteAccountRequest): SuccessMessageResponse {
        deleteAccountCalls++
        synchronized(_deleteAccountRequests) { _deleteAccountRequests += body }
        return deleteAccountHandler(body)
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
