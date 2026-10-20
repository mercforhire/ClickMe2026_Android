package com.uptrendinvestments.clickmeapp.network

import com.squareup.moshi.Types
import com.uptrendinvestments.clickmeapp.model.enums.AppMode
import com.uptrendinvestments.clickmeapp.model.enums.UserRole
import com.uptrendinvestments.clickmeapp.model.requests.LoginRequest
import com.uptrendinvestments.clickmeapp.model.requests.RefreshTokenRequest
import com.uptrendinvestments.clickmeapp.model.responses.AuthUser
import com.uptrendinvestments.clickmeapp.model.responses.LoginData
import com.uptrendinvestments.clickmeapp.model.responses.RefreshTokenData
import com.uptrendinvestments.clickmeapp.model.responses.SuccessDataResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
}
