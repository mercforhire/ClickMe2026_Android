package com.uptrendinvestments.clickmeapp.network

import com.squareup.moshi.Types
import com.uptrendinvestments.clickmeapp.model.enums.AppMode
import com.uptrendinvestments.clickmeapp.model.enums.UserRole
import com.uptrendinvestments.clickmeapp.model.requests.AcceptRescheduleRequest
import com.uptrendinvestments.clickmeapp.model.requests.BookingRequestBody
import com.uptrendinvestments.clickmeapp.model.requests.CancelBookingRequest
import com.uptrendinvestments.clickmeapp.model.requests.ConfirmBookingRequest
import com.uptrendinvestments.clickmeapp.model.requests.ConfirmConnectionRequest
import com.uptrendinvestments.clickmeapp.model.requests.DeclineRescheduleRequest
import com.uptrendinvestments.clickmeapp.model.requests.EndCallRequest
import com.uptrendinvestments.clickmeapp.model.requests.LoginRequest
import com.uptrendinvestments.clickmeapp.model.requests.PaymentIntentRequest
import com.uptrendinvestments.clickmeapp.model.requests.RefreshTokenRequest
import com.uptrendinvestments.clickmeapp.model.requests.SignupRequest
import com.uptrendinvestments.clickmeapp.model.responses.AgoraConfig
import com.uptrendinvestments.clickmeapp.model.responses.AuthUser
import com.uptrendinvestments.clickmeapp.model.responses.AvailabilitySlotDto
import com.uptrendinvestments.clickmeapp.model.responses.AvatarUploadData
import com.uptrendinvestments.clickmeapp.model.responses.BookingRequestData
import com.uptrendinvestments.clickmeapp.model.responses.CancelBookingData
import com.uptrendinvestments.clickmeapp.model.responses.CategoriesPayload
import com.uptrendinvestments.clickmeapp.model.responses.CategoryItem
import com.uptrendinvestments.clickmeapp.model.responses.ClientBookingDetail
import com.uptrendinvestments.clickmeapp.model.responses.ClientBookingsPayload
import com.uptrendinvestments.clickmeapp.model.responses.ClientHomeData
import com.uptrendinvestments.clickmeapp.model.responses.ConfirmBookingData
import com.uptrendinvestments.clickmeapp.model.responses.ConfirmConnectionData
import com.uptrendinvestments.clickmeapp.model.responses.EmailStatusData
import com.uptrendinvestments.clickmeapp.model.responses.EndCallData
import com.uptrendinvestments.clickmeapp.model.responses.ExpertAvailabilityData
import com.uptrendinvestments.clickmeapp.model.responses.ExpertProfileData
import com.uptrendinvestments.clickmeapp.model.responses.ExpertReviewsPayload
import com.uptrendinvestments.clickmeapp.model.responses.ExpertiseTagsPayload
import com.uptrendinvestments.clickmeapp.model.responses.JoinCallResponse
import com.uptrendinvestments.clickmeapp.model.responses.LanguagesPayload
import com.uptrendinvestments.clickmeapp.model.responses.LoginData
import com.uptrendinvestments.clickmeapp.model.responses.MeData
import com.uptrendinvestments.clickmeapp.model.responses.MyExpertTopicsPayload
import com.uptrendinvestments.clickmeapp.model.responses.PaginationMeta
import com.uptrendinvestments.clickmeapp.model.responses.PaymentIntentResponse
import com.uptrendinvestments.clickmeapp.model.responses.PaymentMethodSetupData
import com.uptrendinvestments.clickmeapp.model.responses.PaymentMethodsData
import com.uptrendinvestments.clickmeapp.model.responses.PublicProfileDetailsData
import com.uptrendinvestments.clickmeapp.model.responses.RefreshTokenData
import com.uptrendinvestments.clickmeapp.model.responses.RescheduleResponseData
import com.uptrendinvestments.clickmeapp.model.responses.SetupExpertProfileData
import com.uptrendinvestments.clickmeapp.model.responses.SignupData
import com.uptrendinvestments.clickmeapp.model.responses.SuccessDataResponse
import com.uptrendinvestments.clickmeapp.model.responses.UserProfileData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class ResponseDecodingTest {
    private val moshi = MoshiProvider.moshi

    private fun <T> envelope(data: Class<T>) =
        moshi.adapter<SuccessDataResponse<T>>(
            Types.newParameterizedType(SuccessDataResponse::class.java, data),
        )

    @Test
    fun loginDataDecodesFromEnvelopeAndIgnoresUnknownKeys() {
        val json =
            """
            {"status":"success","unexpected":{"a":1},"data":{
              "token":"jwt","refresh_token":"rt","expires_at":1792513029,"expires_in":900,
              "user":{"id":"7f1b8a52-0000-4000-8000-000000000001","first_name":"Ada","last_name":"Lovelace",
                      "roles":["client","expert"],"avatar_url":"https://cdn/a.png","extra":true}
            }}
            """.trimIndent()
        val response = envelope(LoginData::class.java).fromJson(json)!!
        assertEquals("success", response.status)
        val data = response.data
        assertEquals("jwt", data.token)
        assertEquals("rt", data.refreshToken)
        assertEquals(1_792_513_029L, data.expiresAt)
        assertEquals(900L, data.expiresIn)
        assertEquals("7f1b8a52-0000-4000-8000-000000000001", data.user.id)
        assertEquals("Ada", data.user.firstName)
        assertEquals("Lovelace", data.user.lastName)
        assertEquals("https://cdn/a.png", data.user.avatarUrl)
        assertEquals(listOf("client", "expert"), data.user.roles)
    }

    @Test
    fun authUserAvatarMayBeAbsent() {
        val json = """{"id":"x","first_name":"A","last_name":"B","roles":["client"]}"""
        val user = moshi.adapter(AuthUser::class.java).fromJson(json)!!
        assertNull(user.avatarUrl)
    }

    @Test
    fun refreshTokenDataDecodesRotatingTrio() {
        val json =
            """{"status":"success","data":{"token":"t2","refresh_token":"r2","expires_at":1792513999,"expires_in":900}}"""
        val data = envelope(RefreshTokenData::class.java).fromJson(json)!!.data
        assertEquals("t2", data.token)
        assertEquals("r2", data.refreshToken)
        assertEquals(1_792_513_999L, data.expiresAt)
        assertEquals(900L, data.expiresIn)
    }

    @Test
    fun userRolesDropsUnknownRolesWithoutThrowing() {
        val user = AuthUser("id", "A", "B", listOf("client", "expert", "admin"))
        assertEquals(setOf(UserRole.Client, UserRole.Expert), user.userRoles)
    }

    @Test
    fun userRoleFromWire() {
        assertEquals(UserRole.Expert, UserRole.fromWire("expert"))
        assertEquals(UserRole.Client, UserRole.fromWire("client"))
        assertNull(UserRole.fromWire("nope"))
        assertNull(UserRole.fromWire(null))
    }

    @Test
    fun appModeFromWire() {
        assertEquals(AppMode.Client, AppMode.fromWire("client"))
        assertEquals(AppMode.Expert, AppMode.fromWire("expert"))
        assertNull(AppMode.fromWire("other"))
        assertNull(AppMode.fromWire(null))
    }

    @Test
    fun loginRequestEncodes() {
        val json = moshi.adapter(LoginRequest::class.java).toJson(LoginRequest("a@b.com", "pw"))
        assertEquals("""{"email":"a@b.com","password":"pw"}""", json)
    }

    @Test
    fun refreshTokenRequestEncodes() {
        val json = moshi.adapter(RefreshTokenRequest::class.java).toJson(RefreshTokenRequest("rt"))
        assertEquals("""{"refresh_token":"rt"}""", json)
    }

    @Test
    fun meDataDecodesPricingLimits() {
        val json =
            """
            {"status":"success","data":{"id":"u1","email":"a@b.com","roles":["client","expert"],
              "pricing_limits":{"min_session_amount":{
                "by_currency":{"USD":50,"JPY":50},"default_minor_units":50,"default_currency":"CAD"}}}}
            """.trimIndent()
        val data = envelope(MeData::class.java).fromJson(json)!!.data
        assertEquals("u1", data.id)
        assertEquals("a@b.com", data.email)
        assertEquals(setOf(UserRole.Client, UserRole.Expert), data.userRoles)
        val min = data.pricingLimits!!.minSessionAmount
        assertEquals(mapOf("USD" to 50, "JPY" to 50), min.byCurrency)
        assertEquals(50, min.defaultMinorUnits)
        assertEquals("CAD", min.defaultCurrency)
    }

    @Test
    fun meDataWithoutPricingLimitsDecodesNull() {
        val json = """{"status":"success","data":{"id":"u1","email":"a@b.com","roles":["client"]}}"""
        assertNull(envelope(MeData::class.java).fromJson(json)!!.data.pricingLimits)
    }

    @Test
    fun minMinorUnitsUppercasesAndFallsBack() {
        val min =
            moshi
                .adapter(MeData::class.java)
                .fromJson(
                    """{"id":"u","email":"e","roles":[],"pricing_limits":{"min_session_amount":""" +
                        """{"by_currency":{"USD":75},"default_minor_units":50,"default_currency":"CAD"}}}""",
                )!!
                .pricingLimits!!
                .minSessionAmount
        assertEquals(75, min.minMinorUnits("usd"))
        assertEquals(50, min.minMinorUnits("XYZ"))
    }

    private val fullProfileJson =
        """
        {"status":"success","data":{
          "personal_details":{"first_name":"Ada","last_name":"Lovelace","email":"a@b.com",
                              "avatar_url":"https://cdn/a.png","bio":"Hi"},
          "professional_details":{"job_title":"Engineer","company":"Analytical","education":"Self"},
          "location":{"city":"London","state_province":"ENG","country":"USA","timezone":"Europe/London"},
          "languages":[{"id":"en","label":"English"},{"id":"fr","label":"French"}]}}
        """.trimIndent()

    @Test
    fun userProfileDecodesFullSample() {
        val data = envelope(UserProfileData::class.java).fromJson(fullProfileJson)!!.data
        assertEquals("Ada", data.personalDetails.firstName)
        assertEquals("https://cdn/a.png", data.personalDetails.avatarUrl)
        assertEquals("Hi", data.personalDetails.bio)
        assertEquals("Engineer", data.professionalDetails.jobTitle)
        assertEquals("ENG", data.location.stateProvince)
        assertEquals("USA", data.location.country)
        assertEquals(2, data.languages.size)
        assertEquals("French", data.languages[1].label)
    }

    @Test
    fun userProfileWithAllNestedValuesNullOrMissingDecodes() {
        val json =
            """{"personal_details":{"first_name":null},"professional_details":{},"location":{"city":null}}"""
        val data = moshi.adapter(UserProfileData::class.java).fromJson(json)!!
        assertNull(data.personalDetails.firstName)
        assertNull(data.professionalDetails.jobTitle)
        assertNull(data.location.country)
        assertTrue(data.languages.isEmpty())
    }

    @Test
    fun userProfileRoundTrips() {
        val adapter = moshi.adapter(UserProfileData::class.java)
        val original = envelope(UserProfileData::class.java).fromJson(fullProfileJson)!!.data
        assertEquals(original, adapter.fromJson(adapter.toJson(original)))
    }

    @Test
    fun expertProfileReadsSetupCompletedAndIgnoresOtherKeys() {
        val json =
            """{"status":"success","data":{"id":"e1","headline":"Coach","setup_completed":false,"topics_count":3}}"""
        assertFalse(envelope(ExpertProfileData::class.java).fromJson(json)!!.data.setupCompleted)
    }

    @Test
    fun myExpertTopicsDecodesSizes() {
        val adapter = moshi.adapter(MyExpertTopicsPayload::class.java)
        val two = """{"topics":[{"id":"1","title":"A"},{"id":"2","title":"B","price":{"amount":5}}]}"""
        assertEquals(2, adapter.fromJson(two)!!.topics.size)
        assertEquals(0, adapter.fromJson("""{"topics":[]}""")!!.topics.size)
    }

    @Test
    fun paginationMetaDecodesAllKeys() {
        val json = """{"current_page":2,"total_pages":5,"total_count":93,"has_more":true}"""
        val meta = moshi.adapter(PaginationMeta::class.java).fromJson(json)!!
        assertEquals(2, meta.currentPage)
        assertEquals(5, meta.totalPages)
        assertEquals(93, meta.totalCount)
        assertEquals(true, meta.hasMore)
        assertTrue(meta.hasMoreOrDerived)
    }

    @Test
    fun paginationMetaDerivesHasMoreWhenAbsent() {
        val adapter = moshi.adapter(PaginationMeta::class.java)
        assertTrue(adapter.fromJson("""{"current_page":1,"total_pages":3}""")!!.hasMoreOrDerived)
        assertFalse(adapter.fromJson("""{"current_page":3,"total_pages":3}""")!!.hasMoreOrDerived)
    }

    @Test
    fun paginationMetaPrefersServerFlag() {
        val meta =
            moshi
                .adapter(PaginationMeta::class.java)
                .fromJson("""{"current_page":1,"total_pages":3,"has_more":false}""")!!
        assertFalse(meta.hasMoreOrDerived)
    }

    @Test
    fun fullExpertProfileDecodesEveryField() {
        val json =
            """
            {"status":"success","data":{"profile_id":"p1","avatar_url":"https://x/a.jpg",
            "personal_info":{"first_name":"Ada","last_name":"","email":"ada@b.co","bio":null},
            "professional_details":{"job_title":null},
            "location_details":{"city":"Toronto","state_province":"ON","country_code":"CAN",
            "timezone":"America/Toronto"},
            "languages":[{"id":"en","label":"English"},{"id":null,"label":"Klingon"}],
            "expertise_tags":[{"id":"t1","label":"Tax","category_id":"c1","is_primary":true}],
            "setup_completed":false}}
            """.trimIndent()
        val data = envelope(ExpertProfileData::class.java).fromJson(json)!!.data
        assertEquals("p1", data.profileId)
        assertEquals("https://x/a.jpg", data.avatarUrl)
        assertEquals("Ada", data.personalInfo!!.firstName)
        assertEquals("", data.personalInfo.lastName)
        assertEquals("ada@b.co", data.personalInfo.email)
        assertNull(data.personalInfo.bio)
        assertEquals("Toronto", data.locationDetails!!.city)
        assertEquals("ON", data.locationDetails.stateProvince)
        assertEquals("CAN", data.locationDetails.countryCode)
        assertEquals("America/Toronto", data.locationDetails.timezone)
        assertEquals("en", data.languages!![0].id)
        assertNull(data.languages[1].id)
        assertEquals("Klingon", data.languages[1].label)
        assertEquals("Tax", data.expertiseTags!!.single().label)
        assertTrue(data.expertiseTags.single().isPrimary)
        assertFalse(data.setupCompleted)
    }

    @Test
    fun setupCompletedOnlyExpertProfileStillDecodes() {
        val json = """{"status":"success","data":{"setup_completed":true}}"""
        val data = envelope(ExpertProfileData::class.java).fromJson(json)!!.data
        assertTrue(data.setupCompleted)
        assertNull(data.profileId)
        assertNull(data.avatarUrl)
        assertNull(data.personalInfo)
        assertNull(data.locationDetails)
        assertNull(data.languages)
        assertNull(data.expertiseTags)
    }

    @Test
    fun signupDataDecodesAndRedactsTokens() {
        val json =
            """
            {"status":"success","data":{"token":"secret-jwt","refresh_token":"secret-rt","expires_at":1792513029,
            "expires_in":900,"email_verified":false,
            "user":{"id":"u1","first_name":"","last_name":"","roles":["expert"]}}}
            """.trimIndent()
        val data = envelope(SignupData::class.java).fromJson(json)!!.data
        assertEquals("secret-jwt", data.token)
        assertEquals("secret-rt", data.refreshToken)
        assertEquals(1_792_513_029L, data.expiresAt)
        assertEquals(900L, data.expiresIn)
        assertFalse(data.emailVerified)
        assertEquals("", data.user.firstName)
        assertFalse(data.toString().contains("secret-jwt"))
        assertFalse(data.toString().contains("secret-rt"))
    }

    @Test
    fun signupDataWithoutEmailVerifiedDefaultsToFalse() {
        val json =
            """
            {"token":"t","refresh_token":"r","expires_at":1,"expires_in":2,
            "user":{"id":"u1","first_name":"A","last_name":"B","roles":["expert"]}}
            """.trimIndent()
        assertFalse(moshi.adapter(SignupData::class.java).fromJson(json)!!.emailVerified)
    }

    @Test
    fun signupRequestToStringRedactsCredentials() {
        val text = SignupRequest("a@b.co", "abcdefg1").toString()
        assertFalse(text.contains("a@b.co"))
        assertFalse(text.contains("abcdefg1"))
    }

    @Test
    fun setupExpertProfileDataDecodes() {
        val json =
            """
            {"profile_id":"p1","completion_percentage":70,"missing_fields":["profile_image_url"],
            "next_recommended_step":"photo","setup_completed":false}
            """.trimIndent()
        val data = moshi.adapter(SetupExpertProfileData::class.java).fromJson(json)!!
        assertEquals("p1", data.profileId)
        assertEquals(70, data.completionPercentage)
        assertEquals(listOf("profile_image_url"), data.missingFields)
        assertEquals("photo", data.nextRecommendedStep)
        assertEquals(false, data.setupCompleted)
    }

    @Test
    fun smallPayloadsDecode() {
        assertTrue(moshi.adapter(EmailStatusData::class.java).fromJson("""{"verified":true}""")!!.verified)
        assertEquals(
            "https://x/a.jpg",
            moshi.adapter(AvatarUploadData::class.java).fromJson("""{"avatar_url":"https://x/a.jpg"}""")!!.avatarUrl,
        )
        val languages =
            moshi
                .adapter(LanguagesPayload::class.java)
                .fromJson("""{"languages":[{"id":"en","label":"English"}]}""")!!
        assertEquals("English", languages.languages.single().label)
        val tags =
            moshi
                .adapter(ExpertiseTagsPayload::class.java)
                .fromJson("""{"tags":[{"id":"u1","label":"Tax","category_id":"c1"}]}""")!!
        assertEquals("u1", tags.tags.single().id)
    }

    @Test
    fun clientHomeDecodesFeaturedFreeTopicAndLenientExperts() {
        val json =
            """
            {"status":"success","data":{
            "trending_categories":[{"id":"ai","name":"AI","icon_name":"cpu","color_accent":null}],
            "recommended_experts":[{"expert_id":"e1","full_name":"Ada Lovelace","title":"Engineer",
              "profile_image_url":"https://x/a.jpg","rating":4.5,"review_count":12,"years_experience":7,
              "expertise_tags":["AI","Math"],"is_favorite":false}],
            "featured_topics":[
              {"topic_id":"t1","title":"Intro to ML","duration_mins":30,
               "hourly_rate":{"amount":5000,"currency":"CAD"},"is_free":false,
               "expert":{"expert_id":"e1","full_name":"Ada Lovelace","profile_image_url":null},
               "rating":4.5,"is_favorite":false},
              {"topic_id":"t2","title":"Free chat","duration_mins":15,
               "hourly_rate":{"amount":null,"currency":null},"is_free":true,
               "expert":{"expert_id":"e2","full_name":null,"profile_image_url":null},"rating":0.0}]}}
            """.trimIndent()
        val data = envelope(ClientHomeData::class.java).fromJson(json)!!.data
        assertEquals("ai", data.trendingCategories.single().id)
        assertEquals("cpu", data.trendingCategories.single().iconName)
        assertNull(data.trendingCategories.single().colorAccent)
        val expert = data.recommendedExperts.single()
        assertEquals("e1", expert.expertId)
        assertEquals(4.5, expert.rating!!, 0.0)
        assertEquals(12, expert.reviewCount)
        assertEquals(listOf("AI", "Math"), expert.expertiseTags)
        val topics = data.featuredTopics!!
        assertEquals(5000L, topics[0].hourlyRate!!.amount)
        assertEquals("CAD", topics[0].hourlyRate!!.currency)
        assertEquals(false, topics[0].isFree)
        assertEquals(true, topics[1].isFree)
        assertNull(topics[1].hourlyRate!!.amount)
        assertNull(topics[1].hourlyRate!!.currency)
        assertNull(topics[1].expert.fullName)
    }

    @Test
    fun clientHomeWithoutFeaturedTopicsDecodesNullAndSparseExpert() {
        val json = """{"trending_categories":[],"recommended_experts":[{"expert_id":"e9"}]}"""
        val data = moshi.adapter(ClientHomeData::class.java).fromJson(json)!!
        assertNull(data.featuredTopics)
        val expert = data.recommendedExperts.single()
        assertEquals("e9", expert.expertId)
        assertNull(expert.rating)
        assertNull(expert.reviewCount)
        assertNull(expert.title)
        assertNull(expert.expertiseTags)
    }

    @Test
    fun categoriesDecodeAndRoundTrip() {
        val json =
            """{"categories":[{"id":"ai","name":"AI","icon_name":"cpu","color_accent":null,"description":null}]}"""
        val payload = moshi.adapter(CategoriesPayload::class.java).fromJson(json)!!
        val item = payload.categories.single()
        assertEquals("ai", item.id)
        assertEquals("AI", item.name)
        assertEquals("cpu", item.iconName)
        assertNull(item.colorAccent)
        assertNull(item.description)

        val listAdapter =
            moshi.adapter<List<CategoryItem>>(Types.newParameterizedType(List::class.java, CategoryItem::class.java))
        assertEquals(payload.categories, listAdapter.fromJson(listAdapter.toJson(payload.categories)))
    }

    private val detailsJson =
        """
        {"is_favorite":false,"is_bookable":false,"available_meeting_types":["in_app_voice"],
        "profile":{"name":"Ada Lovelace","title":null,"bio":"Hi",
          "stats":{"experience_years":null,"total_bookings":3,"avg_response_time":null}},
        "topics_of_discussion":[
          {"id":"t1","title":"Intro","duration_mins":30,"description":null,"icon_slug":null,
           "price":{"amount":null,"currency":null,"is_free":true},"expertise_tags":[]},
          {"id":"t2","title":"Deep dive","duration_mins":60,"description":"Long","icon_slug":null,
           "price":{"amount":5000,"currency":"USD","is_free":false},"expertise_tags":[]}]}
        """.trimIndent()

    @Test
    fun expertDetailsDecodesFreeAndPaidTopics() {
        val data = moshi.adapter(PublicProfileDetailsData::class.java).fromJson(detailsJson)!!
        assertEquals(false, data.isBookable)
        assertEquals(listOf("in_app_voice"), data.availableMeetingTypes)
        assertEquals("Ada Lovelace", data.profile.name)
        assertNull(data.profile.title)
        assertNull(data.profile.stats.experienceYears)
        assertEquals(3, data.profile.stats.totalBookings)
        val (free, paid) = data.topicsOfDiscussion
        assertTrue(free.price.isFree)
        assertNull(free.price.amount)
        assertFalse(paid.price.isFree)
        assertEquals(5000L, paid.price.amount)
        assertEquals("USD", paid.price.currency)
        assertEquals(60, paid.durationMins)
    }

    @Test
    fun expertDetailsWithoutIsBookableDecodesNull() {
        val json = """{"profile":{"name":"Ada"},"topics_of_discussion":[]}"""
        val data = moshi.adapter(PublicProfileDetailsData::class.java).fromJson(json)!!
        assertNull(data.isBookable)
        assertNull(data.availableMeetingTypes)
        assertEquals(0, data.profile.stats.totalBookings)
    }

    @Test
    fun expertReviewsDecodeNullRatingAndEmptyList() {
        val json =
            """
            {"summary":{"expert_id":"e1","overall_rating":null,"total_reviews":0,"rating_distribution":{"1":0}},
            "reviews":[],"pagination":{"current_page":1,"total_pages":0,"has_more":false}}
            """.trimIndent()
        val data = moshi.adapter(ExpertReviewsPayload::class.java).fromJson(json)!!
        assertNull(data.summary.overallRating)
        assertEquals(0, data.summary.totalReviews)
        assertTrue(data.reviews.isEmpty())
        assertFalse(data.pagination!!.hasMoreOrDerived)
    }

    @Test
    fun expertReviewsDecodeOneReviewWithNullReviewerName() {
        val json =
            """
            {"summary":{"expert_id":"e1","overall_rating":4.8,"total_reviews":1},
            "reviews":[{"id":"r1","reviewer":{"name":null,"avatar_url":null},
              "session_context":{"topic":"Intro","date":"2026-10-01T15:00:00.000000Z","date_label":"Oct 1"},
              "rating":5,"comment":"Great","created_at":"2026-10-02T10:00:00.123456Z","verified_booking":true}],
            "pagination":{"current_page":1,"total_pages":1,"has_more":false}}
            """.trimIndent()
        val data = moshi.adapter(ExpertReviewsPayload::class.java).fromJson(json)!!
        assertEquals(4.8, data.summary.overallRating!!, 0.0)
        assertEquals(1, data.summary.totalReviews)
        val review = data.reviews.single()
        assertEquals("r1", review.id)
        assertNull(review.reviewer.name)
        assertEquals(5, review.rating)
        assertEquals("Great", review.comment)
    }

    @Test
    fun clientBookingsDecodeInstantsAndIgnoreUnknownKeys() {
        val json =
            """
            {"bookings":[{"booking_id":"b1","expert":{"id":"e1","full_name":"Ada Lovelace","avatar_url":null},
              "topic":null,"start_time":"2026-10-21T19:00:00.000000Z","end_time":"2026-10-21T20:00:00.000000Z",
              "meeting_type":"in_app_voice","status":"confirmed","private_note_preview":null,"reschedule":null}],
            "pagination":{"current_page":1,"total_pages":1,"total_count":1}}
            """.trimIndent()
        val data = moshi.adapter(ClientBookingsPayload::class.java).fromJson(json)!!
        val booking = data.bookings.single()
        assertEquals("b1", booking.bookingId)
        assertEquals("e1", booking.expert.id)
        assertEquals(Instant.parse("2026-10-21T19:00:00Z"), booking.startTime)
        assertEquals(Instant.parse("2026-10-21T20:00:00Z"), booking.endTime)
        assertEquals("confirmed", booking.status)
        assertNull(booking.topic)
        // An explicit null preview is the same as an absent one.
        assertNull(booking.privateNotePreview)
    }

    @Test
    fun expertAvailabilityDecodesBackendShapedGridWithInstantSlots() {
        val json =
            """
            {"status":"success","data":{"expert_timezone":"Pacific/Auckland","client_timezone":"UTC",
            "month":"2026-11","days":[{"date":"2026-11-01","slots":[
            {"start_utc":"2026-10-31T20:00:00.000Z","end_utc":"2026-10-31T20:30:00.000Z",
            "start_local":"8:00 PM","end_local":"8:30 PM","available":true},
            {"start_utc":"2026-10-31T20:30:00.000Z","end_utc":"2026-10-31T21:00:00.000Z",
            "start_local":"8:30 PM","end_local":"9:00 PM","available":false}]},
            {"date":"2026-11-02","slots":null}]}}
            """.trimIndent()
        val data = envelope(ExpertAvailabilityData::class.java).fromJson(json)!!.data
        assertEquals("Pacific/Auckland", data.expertTimezone)
        assertEquals("UTC", data.clientTimezone)
        assertEquals("2026-11", data.month)
        assertEquals(2, data.days.size)
        val slots = data.days[0].slots!!
        assertEquals(Instant.parse("2026-10-31T20:00:00Z"), slots[0].startUtc)
        assertEquals(Instant.parse("2026-10-31T20:30:00Z"), slots[0].endUtc)
        assertEquals(true, slots[0].available)
        assertNull(slots[0].held)
        assertEquals(false, slots[1].available)
        assertNull(data.days[1].slots)
    }

    @Test
    fun availabilitySlotDecodesHeldFlagAndMissingEnd() {
        val json = """{"start_utc":"2026-10-31T21:00:00.000Z","available":true,"held":true}"""
        val slot = moshi.adapter(AvailabilitySlotDto::class.java).fromJson(json)!!
        assertEquals(Instant.parse("2026-10-31T21:00:00Z"), slot.startUtc)
        assertEquals(true, slot.held)
        assertNull(slot.endUtc)
    }

    @Test
    fun availabilityDataDecodesFromEmptyObject() {
        val data = moshi.adapter(ExpertAvailabilityData::class.java).fromJson("{}")!!
        assertNull(data.expertTimezone)
        assertTrue(data.days.isEmpty())
    }

    @Test
    fun bookingRequestResponseDecodes() {
        val json =
            """
            {"status":"success","data":{"booking_id":"b1","status":"pending_approval",
            "message":"Your booking request has been sent for review."}}
            """.trimIndent()
        val data = envelope(BookingRequestData::class.java).fromJson(json)!!.data
        assertEquals("b1", data.bookingId)
        assertEquals("pending_approval", data.status)
    }

    @Test
    fun clientBookingDetailDecodesOffsetInstantsAndTopicIsFree() {
        val json =
            """
            {"booking_id":"b1","status":"pending_approval","start_time":"2026-10-24T18:00:00+00:00",
            "end_time":"2026-10-24T19:00:00+00:00","meeting_type":"in_app_voice",
            "expert_timezone":"America/Toronto","payment_status":"none",
            "topic":{"id":"t1","title":"Intro","duration_mins":60,"is_free":true,
            "price":{"amount":null,"currency":null}},
            "expert":{"id":"e1","full_name":"Ada Lovelace","avatar_url":null,"title":"Engineer"},
            "join_link":null,"can_join":false,"client_notes":null,"private_note":null,"reschedule":null,
            "authorization_expires_at":null}
            """.trimIndent()
        val detail = moshi.adapter(ClientBookingDetail::class.java).fromJson(json)!!
        assertEquals("b1", detail.bookingId)
        assertEquals(Instant.parse("2026-10-24T18:00:00Z"), detail.startTime)
        assertEquals(Instant.parse("2026-10-24T19:00:00Z"), detail.endTime)
        assertTrue(detail.topic.isFree)
        assertEquals(60, detail.topic.durationMins)
        assertEquals("Ada Lovelace", detail.expert.fullName)
        assertNull(detail.clientNotes)
    }

    @Test
    fun clientBookingDetailDecodesPaidTopicWithoutEndTime() {
        val json =
            """
            {"booking_id":"b2","status":"confirmed","start_time":"2026-10-24T18:00:00+00:00",
            "topic":{"id":"t2","title":"Deep dive","duration_mins":30,"is_free":false,
            "price":{"amount":5000,"currency":"CAD"}},
            "expert":{"id":"e1","full_name":"Ada Lovelace"}}
            """.trimIndent()
        val detail = moshi.adapter(ClientBookingDetail::class.java).fromJson(json)!!
        assertNull(detail.endTime)
        assertFalse(detail.topic.isFree)
        assertEquals(5000L, detail.topic.price!!.amount)
        assertEquals("CAD", detail.topic.price.currency)
    }

    @Test
    fun bookingRequestBodySerializesExactKeysInOrderWithNotes() {
        val body =
            BookingRequestBody(
                expertId = "e1",
                topicId = "t1",
                scheduledStart = "2026-10-24T14:00:00Z",
                durationMinutes = 30,
                meetingType = "in_app_voice",
                timezone = "America/Toronto",
                clientNotes = "Hi",
            )
        assertEquals(
            """{"expert_id":"e1","topic_id":"t1","scheduled_start":"2026-10-24T14:00:00Z",""" +
                """"duration_minutes":30,"meeting_type":"in_app_voice","timezone":"America/Toronto",""" +
                """"client_notes":"Hi"}""",
            moshi.adapter(BookingRequestBody::class.java).toJson(body),
        )
    }

    @Test
    fun bookingRequestBodyOmitsClientNotesWhenNull() {
        val body =
            BookingRequestBody(
                expertId = "e1",
                topicId = "t1",
                scheduledStart = "2026-10-24T14:00:00Z",
                durationMinutes = 30,
                meetingType = "in_app_voice",
                timezone = "America/Toronto",
            )
        assertEquals(
            """{"expert_id":"e1","topic_id":"t1","scheduled_start":"2026-10-24T14:00:00Z",""" +
                """"duration_minutes":30,"meeting_type":"in_app_voice","timezone":"America/Toronto"}""",
            moshi.adapter(BookingRequestBody::class.java).toJson(body),
        )
    }

    @Test
    fun paymentIntentRequestSerializesExactKeysAndNoPromoCode() {
        val body =
            PaymentIntentRequest(
                expertId = "e1",
                topicId = "t2",
                selectedDate = "2026-10-23",
                timeSlot = "01:00",
                durationMinutes = 60,
                bookingAttemptId = "6f1c0000-0000-4000-8000-000000000001",
            )
        assertEquals(
            """{"expert_id":"e1","topic_id":"t2","selected_date":"2026-10-23","time_slot":"01:00",""" +
                """"duration_minutes":60,"booking_attempt_id":"6f1c0000-0000-4000-8000-000000000001"}""",
            moshi.adapter(PaymentIntentRequest::class.java).toJson(body),
        )
    }

    @Test
    fun confirmBookingRequestOmitsNullNotesAndNeverCarriesBookingAttemptId() {
        val body =
            ConfirmBookingRequest(
                paymentIntentId = "pi_1",
                expertId = "e1",
                topicId = "t2",
                startTime = "2026-10-23T01:00:00Z",
                meetingType = "in_app_voice",
                stripePaymentMethodId = "pm_1",
            )
        val json = moshi.adapter(ConfirmBookingRequest::class.java).toJson(body)
        assertEquals(
            """{"payment_intent_id":"pi_1","expert_id":"e1","topic_id":"t2","start_time":"2026-10-23T01:00:00Z",""" +
                """"meeting_type":"in_app_voice","stripe_payment_method_id":"pm_1"}""",
            json,
        )
        assertFalse(json, json.contains("client_notes"))
        assertFalse(json, json.contains("booking_attempt_id"))
    }

    @Test
    fun confirmBookingRequestOmitsPaymentIntentIdForLongLead() {
        val body =
            ConfirmBookingRequest(
                expertId = "e1",
                topicId = "t2",
                startTime = "2026-12-23T01:00:00Z",
                meetingType = "in_app_voice",
                stripePaymentMethodId = "pm_1",
                clientNotes = "Hi",
            )
        val json = moshi.adapter(ConfirmBookingRequest::class.java).toJson(body)
        assertFalse(json, json.contains("payment_intent_id"))
        assertTrue(json, json.contains(""""client_notes":"Hi""""))
        assertFalse(json, json.contains("booking_attempt_id"))
    }

    @Test
    fun paymentIntentBareBodyDecodesWithoutAmountAndRedactsSecret() {
        val json =
            """
            {"payment_intent_id":"pi_1","client_secret":"pi_1_secret_x","customer_id":"cus_1","ephemeral_key":"ek_1",
            "total_amount":42.5,"summary":{"subtotal":42.5,"discount":0,"total":42.5},"currency":"cad"}
            """.trimIndent()
        val response = moshi.adapter(PaymentIntentResponse::class.java).fromJson(json)!!
        assertEquals(PaymentIntentResponse("pi_1", "pi_1_secret_x", "cad"), response)
        assertFalse(response.toString(), response.toString().contains("pi_1_secret_x"))
    }

    @Test
    fun paymentMethodsListDecodesInServerOrder() {
        val json =
            """
            {"status":"success","data":{"payment_methods":[
              {"id":"pm_1","brand":"visa","last4":"4242","exp_month":9,"exp_year":2028,"is_default":true},
              {"id":"pm_2","brand":"unknown","last4":null,"is_default":false}]}}
            """.trimIndent()
        val items = envelope(PaymentMethodsData::class.java).fromJson(json)!!.data.paymentMethods
        assertEquals(listOf("pm_1", "pm_2"), items.map { it.id })
        assertEquals(9, items[0].expMonth)
        assertEquals(2028, items[0].expYear)
        assertTrue(items[0].isDefault)
        assertNull(items[1].last4)
        assertNull(items[1].expMonth)
        assertNull(items[1].expYear)
        assertFalse(items[1].isDefault)
    }

    @Test
    fun paymentMethodsEmptyListDecodes() {
        val json = """{"status":"success","data":{"payment_methods":[]}}"""
        val data = envelope(PaymentMethodsData::class.java).fromJson(json)!!.data
        assertTrue(data.paymentMethods.isEmpty())
    }

    @Test
    fun setupIntentDecodesAndRedactsSecrets() {
        val json =
            """{"status":"success","data":{"client_secret":"seti_1_secret_x","customer_id":"cus_1","ephemeral_key":"ek_1"}}"""
        val data = envelope(PaymentMethodSetupData::class.java).fromJson(json)!!.data
        assertEquals(PaymentMethodSetupData("seti_1_secret_x", "cus_1", "ek_1"), data)
        val text = data.toString()
        assertFalse(text, text.contains("seti_1_secret_x"))
        assertFalse(text, text.contains("ek_1"))
    }

    @Test
    fun confirmBookingEnvelopeDecodes() {
        val json =
            """
            {"status":"success","data":{"booking_id":"b9","status":"pending_approval","message":"Sent",
            "estimated_confirmation_time":"24 hours"}}
            """.trimIndent()
        val data = envelope(ConfirmBookingData::class.java).fromJson(json)!!.data
        assertEquals(ConfirmBookingData("b9", "pending_approval", "Sent", "24 hours"), data)
    }

    @Test
    fun clientBookingRowDecodesSlimReschedule() {
        val json =
            """
            {"bookings":[{"booking_id":"b1","expert":{"id":"e1"},"start_time":"2026-10-24T18:00:00Z",
            "end_time":"2026-10-24T19:00:00Z","status":"pending_reschedule",
            "reschedule":{"proposed_start":"2026-10-30T18:00:00Z","initiated_by":"expert"}}]}
            """.trimIndent()
        val reschedule =
            moshi
                .adapter(ClientBookingsPayload::class.java)
                .fromJson(json)!!
                .bookings
                .single()
                .reschedule!!
        assertEquals(Instant.parse("2026-10-30T18:00:00Z"), reschedule.proposedStart)
        assertEquals("expert", reschedule.initiatedBy)
        assertNull(reschedule.proposedEnd)
        assertNull(reschedule.proposedTimezone)
        assertNull(reschedule.note)
        assertNull(reschedule.createdAt)
    }

    @Test
    fun clientBookingRowWithNullRescheduleAndPrivateNotePreviewDecodes() {
        val json =
            """
            {"bookings":[{"booking_id":"b1","expert":{"id":"e1"},"start_time":"2026-10-24T18:00:00Z",
            "end_time":"2026-10-24T19:00:00Z","status":"confirmed","reschedule":null,
            "private_note_preview":"x"}]}
            """.trimIndent()
        val payload = moshi.adapter(ClientBookingsPayload::class.java).fromJson(json)!!
        assertNull(payload.bookings.single().reschedule)
        assertEquals("x", payload.bookings.single().privateNotePreview)
    }

    @Test
    fun clientBookingRowWithoutThePreviewKeyDecodesToNull() {
        val json =
            """
            {"bookings":[{"booking_id":"b1","expert":{"id":"e1"},"start_time":"2026-10-24T18:00:00Z",
            "end_time":"2026-10-24T19:00:00Z","status":"confirmed"}]}
            """.trimIndent()
        val payload = moshi.adapter(ClientBookingsPayload::class.java).fromJson(json)!!
        assertNull(payload.bookings.single().privateNotePreview)
    }

    @Test
    fun clientBookingRowToStringOmitsThePreview() {
        val json =
            """
            {"bookings":[{"booking_id":"b1","expert":{"id":"e1"},"start_time":"2026-10-24T18:00:00Z",
            "end_time":"2026-10-24T19:00:00Z","status":"confirmed","private_note_preview":"secret plan"}]}
            """.trimIndent()
        val row =
            moshi
                .adapter(ClientBookingsPayload::class.java)
                .fromJson(json)!!
                .bookings
                .single()
        assertEquals("secret plan", row.privateNotePreview)
        assertTrue(!row.toString().contains("secret plan"))
        assertEquals("ClientBookingItem(bookingId=b1, status=confirmed)", row.toString())
    }

    @Test
    fun clientBookingDetailDecodesFullRescheduleAuthorizationAndJoin() {
        val json =
            """
            {"booking_id":"b1","status":"pending_reschedule","start_time":"2026-10-24T18:00:00Z",
            "payment_status":"held","topic":{"id":"t1","title":"Intro"},"expert":{"id":"e1"},
            "authorization_expires_at":"2026-10-31T00:00:00Z","join_link":"https://zoom.us/j/1","can_join":true,
            "private_note":"secret",
            "reschedule":{"proposed_start":"2026-10-30T18:00:00Z","proposed_end":"2026-10-30T19:00:00Z",
            "proposed_timezone":"America/Toronto","initiated_by":"expert","note":"Can we move?",
            "created_at":"2026-10-23T12:00:00Z"}}
            """.trimIndent()
        val detail = moshi.adapter(ClientBookingDetail::class.java).fromJson(json)!!
        assertEquals("held", detail.paymentStatus)
        assertEquals(Instant.parse("2026-10-31T00:00:00Z"), detail.authorizationExpiresAt)
        assertEquals("https://zoom.us/j/1", detail.joinLink)
        assertTrue(detail.canJoin)
        val reschedule = detail.reschedule!!
        assertEquals(Instant.parse("2026-10-30T18:00:00Z"), reschedule.proposedStart)
        assertEquals(Instant.parse("2026-10-30T19:00:00Z"), reschedule.proposedEnd)
        assertEquals("America/Toronto", reschedule.proposedTimezone)
        assertEquals("expert", reschedule.initiatedBy)
        assertEquals("Can we move?", reschedule.note)
        assertEquals(Instant.parse("2026-10-23T12:00:00Z"), reschedule.createdAt)
    }

    @Test
    fun cancelBookingResponseDecodesMajorUnitDecimalAndIdempotentMessage() {
        val cancelled =
            """{"status":"success","message":"Booking cancelled.","data":{"refund_amount":12.5,"currency":"CAD"}}"""
        assertEquals(
            CancelBookingData(12.5, "CAD"),
            envelope(CancelBookingData::class.java).fromJson(cancelled)!!.data,
        )
        val already =
            """
            {"status":"success","message":"Booking is already cancelled.",
            "data":{"refund_amount":0,"currency":"JPY"}}
            """.trimIndent()
        assertEquals(
            CancelBookingData(0.0, "JPY"),
            envelope(CancelBookingData::class.java).fromJson(already)!!.data,
        )
    }

    @Test
    fun rescheduleResponseDecodes() {
        val json = """{"status":"success","data":{"booking_id":"b1","status":"confirmed"}}"""
        assertEquals(
            RescheduleResponseData("b1", "confirmed"),
            envelope(RescheduleResponseData::class.java).fromJson(json)!!.data,
        )
    }

    @Test
    fun bookingActionRequestsEncodeExactStrictBodies() {
        assertEquals(
            """{"reason":"Change of plans"}""",
            moshi.adapter(CancelBookingRequest::class.java).toJson(CancelBookingRequest("Change of plans")),
        )
        val accept = moshi.adapter(AcceptRescheduleRequest::class.java)
        assertEquals("{}", accept.toJson(AcceptRescheduleRequest()))
        assertEquals("""{"stripe_payment_method_id":"pm_x"}""", accept.toJson(AcceptRescheduleRequest("pm_x")))
        assertEquals(
            "{}",
            moshi.adapter(DeclineRescheduleRequest::class.java).toJson(DeclineRescheduleRequest()),
        )
    }

    @Test
    fun joinCallDecodesInAppVoiceShape() {
        val json =
            """
            {"status":"success","data":{"booking_id":"b1","connection_type":"in_app_voice",
            "agora_config":{"rtc_token":"tok","uid":12345,"channel_name":"b1"},"external_config":null}}
            """.trimIndent()
        assertEquals(
            JoinCallResponse("b1", "in_app_voice", AgoraConfig("tok", 12345, "b1"), null),
            envelope(JoinCallResponse::class.java).fromJson(json)!!.data,
        )
    }

    @Test
    fun joinCallDecodesSkypeZoomShapeWithNullJoinUrl() {
        val json =
            """
            {"status":"success","data":{"booking_id":"b1","connection_type":"skype_zoom","agora_config":null,
            "external_config":{"join_url":null,"message":"No meeting link configured for this session yet."}}}
            """.trimIndent()
        val data = envelope(JoinCallResponse::class.java).fromJson(json)!!.data
        assertNull(data.agoraConfig)
        assertNull(data.externalConfig!!.joinUrl)
        assertEquals("No meeting link configured for this session yet.", data.externalConfig.message)
    }

    @Test
    fun joinCallDecodesExternalConfigWithoutMessageAndIgnoresUnknownKeys() {
        val json =
            """
            {"status":"success","data":{"booking_id":"b1","connection_type":"skype_zoom","extra":1,
            "external_config":{"join_url":"https://zoom.us/j/1"}}}
            """.trimIndent()
        val data = envelope(JoinCallResponse::class.java).fromJson(json)!!.data
        assertEquals("https://zoom.us/j/1", data.externalConfig!!.joinUrl)
        assertNull(data.externalConfig.message)
        assertNull(data.agoraConfig)
    }

    @Test
    fun agoraConfigToStringNeverRevealsTheTokenOrChannel() {
        val text = AgoraConfig("secret-token", 987654, "room-xyz").toString()
        assertFalse(text.contains("secret-token"))
        assertFalse(text.contains("room-xyz"))
        assertFalse(text.contains("987654"))
    }

    @Test
    fun callRequestsEncodeExactStrictBodies() {
        val confirm = moshi.adapter(ConfirmConnectionRequest::class.java)
        assertEquals(
            """{"connection_status":"connected","timestamp":"2026-10-24T14:00:00Z","method":"in_app_voice"}""",
            confirm.toJson(ConfirmConnectionRequest(timestamp = "2026-10-24T14:00:00Z", method = "in_app_voice")),
        )
        assertEquals(
            """{"connection_status":"connected","timestamp":"2026-10-24T14:00:00Z"}""",
            confirm.toJson(ConfirmConnectionRequest(timestamp = "2026-10-24T14:00:00Z")),
        )
        assertEquals(
            """{"actual_duration":42,"end_reason":"manual_exit"}""",
            moshi.adapter(EndCallRequest::class.java).toJson(EndCallRequest(42, "manual_exit")),
        )
    }

    @Test
    fun endCallAndConfirmConnectionDataDecode() {
        val end =
            """
            {"status":"success","data":{"booking_id":"b1","status":"in_progress","actual_duration_secs":42,
            "end_reason":"completed","review_seam_triggered":false}}
            """.trimIndent()
        assertEquals(
            EndCallData("b1", "in_progress", 42, "completed", false),
            envelope(EndCallData::class.java).fromJson(end)!!.data,
        )
        val confirm =
            """
            {"status":"success","data":{"booking_id":"b1","status":"in_progress","connection_status":"connected",
            "method":null}}
            """.trimIndent()
        assertEquals(
            ConfirmConnectionData("b1", "in_progress", "connected", null),
            envelope(ConfirmConnectionData::class.java).fromJson(confirm)!!.data,
        )
    }
}
