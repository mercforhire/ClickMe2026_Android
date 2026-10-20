package com.uptrendinvestments.clickmeapp.network

import com.squareup.moshi.Types
import com.uptrendinvestments.clickmeapp.model.enums.AppMode
import com.uptrendinvestments.clickmeapp.model.enums.UserRole
import com.uptrendinvestments.clickmeapp.model.requests.LoginRequest
import com.uptrendinvestments.clickmeapp.model.requests.RefreshTokenRequest
import com.uptrendinvestments.clickmeapp.model.responses.AuthUser
import com.uptrendinvestments.clickmeapp.model.responses.ExpertProfileData
import com.uptrendinvestments.clickmeapp.model.responses.LoginData
import com.uptrendinvestments.clickmeapp.model.responses.MeData
import com.uptrendinvestments.clickmeapp.model.responses.MyExpertTopicsPayload
import com.uptrendinvestments.clickmeapp.model.responses.PaginationMeta
import com.uptrendinvestments.clickmeapp.model.responses.RefreshTokenData
import com.uptrendinvestments.clickmeapp.model.responses.SuccessDataResponse
import com.uptrendinvestments.clickmeapp.model.responses.UserProfileData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

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
            moshi.adapter(MeData::class.java).fromJson(
                """{"id":"u","email":"e","roles":[],"pricing_limits":{"min_session_amount":""" +
                    """{"by_currency":{"USD":75},"default_minor_units":50,"default_currency":"CAD"}}}""",
            )!!.pricingLimits!!.minSessionAmount
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
        val two = """{"topics":[{"id":"1","title":"A"},{"id":"2","price":{"amount":5}}]}"""
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
        val meta = moshi.adapter(PaginationMeta::class.java)
            .fromJson("""{"current_page":1,"total_pages":3,"has_more":false}""")!!
        assertFalse(meta.hasMoreOrDerived)
    }
}
