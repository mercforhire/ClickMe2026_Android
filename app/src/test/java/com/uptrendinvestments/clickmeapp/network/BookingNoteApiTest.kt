package com.uptrendinvestments.clickmeapp.network

import com.uptrendinvestments.clickmeapp.model.requests.BookingNoteRequest
import com.uptrendinvestments.clickmeapp.model.responses.BookingNoteData
import com.uptrendinvestments.clickmeapp.screens.bookingnote.BookingNoteRepository
import com.uptrendinvestments.clickmeapp.screens.bookingnote.NoteRole
import com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking.isNotFound
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

/**
 * Exact-wire tests for the four private booking note calls (Phase 16.4). [NoteRole] alone picks the endpoint, the
 * POST body is exactly `{"note":"..."}` (the backend schema is strict), and an empty string is sent as such.
 */
class BookingNoteApiTest {
    private lateinit var server: MockWebServer
    private lateinit var repository: BookingNoteRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val api =
            Retrofit
                .Builder()
                .baseUrl(server.url("/api/v1/"))
                .client(OkHttpClient())
                .addConverterFactory(MoshiConverterFactory.create(MoshiProvider.moshi))
                .build()
                .create(ClickMeApi::class.java)
        repository = BookingNoteRepository(api)
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun enqueue(
        code: Int,
        body: String,
    ) {
        server.enqueue(
            MockResponse
                .Builder()
                .code(code)
                .addHeader("Content-Type", "application/json")
                .body(body)
                .build(),
        )
    }

    private fun noteBody(note: String?): String {
        val value = if (note == null) "null" else "\"$note\""
        return """{"status":"success","data":{"booking_id":"b1","note":$value}}"""
    }

    @Test
    fun clientGetCallsTheClientPath() =
        runTest {
            enqueue(200, noteBody("Call back"))

            val note = repository.get(NoteRole.Client, "b1")

            val request = server.takeRequest()
            assertEquals("GET", request.method)
            assertEquals("/api/v1/client/bookings/b1/note", request.url.encodedPath)
            assertEquals("Call back", note)
        }

    @Test
    fun expertGetCallsTheBookingsPath() =
        runTest {
            enqueue(200, noteBody("Prefers email"))

            val note = repository.get(NoteRole.Expert, "b1")

            val request = server.takeRequest()
            assertEquals("GET", request.method)
            assertEquals("/api/v1/bookings/b1/note", request.url.encodedPath)
            assertEquals("Prefers email", note)
        }

    @Test
    fun clientSaveSendsExactBody() =
        runTest {
            enqueue(200, noteBody("Call back"))

            val saved = repository.save(NoteRole.Client, "b1", "Call back")

            val request = server.takeRequest()
            assertEquals("POST", request.method)
            assertEquals("/api/v1/client/bookings/b1/note", request.url.encodedPath)
            assertEquals("""{"note":"Call back"}""", request.body?.utf8())
            assertEquals("Call back", saved)
        }

    @Test
    fun expertSaveSendsExactBody() =
        runTest {
            enqueue(200, noteBody("Prefers email"))

            val saved = repository.save(NoteRole.Expert, "b1", "Prefers email")

            val request = server.takeRequest()
            assertEquals("POST", request.method)
            assertEquals("/api/v1/bookings/b1/note", request.url.encodedPath)
            assertEquals("""{"note":"Prefers email"}""", request.body?.utf8())
            assertEquals("Prefers email", saved)
        }

    @Test
    fun emptyStringIsSentAsEmptyString() =
        runTest {
            enqueue(200, noteBody(null))

            val saved = repository.save(NoteRole.Client, "b1", "")

            val request = server.takeRequest()
            assertEquals("""{"note":""}""", request.body?.utf8())
            assertNull(saved)
        }

    @Test
    fun nullNoteDecodesToNull() =
        runTest {
            enqueue(200, noteBody(null))

            assertNull(repository.get(NoteRole.Expert, "b1"))
        }

    @Test
    fun notFoundSurfacesAsHttp404() =
        runTest {
            enqueue(404, """{"status":"error","code":"NOT_FOUND","message":"Booking not found."}""")

            try {
                repository.get(NoteRole.Client, "b1")
                fail("expected ApiException.Http")
            } catch (e: ApiException.Http) {
                assertEquals(404, e.status)
                assertTrue(e.isNotFound())
            }
        }

    @Test
    fun serverMessageSurvivesASaveFailure() =
        runTest {
            enqueue(422, """{"status":"error","code":"VALIDATION_ERROR","message":"Too long"}""")

            try {
                repository.save(NoteRole.Expert, "b1", "x")
                fail("expected ApiException.Http")
            } catch (e: ApiException.Http) {
                assertEquals("Too long", e.userMessage)
                assertFalse(e.isNotFound())
            }
        }

    @Test
    fun toStringNeverPrintsTheNote() {
        assertFalse(BookingNoteData("b1", "my secret").toString().contains("my secret"))
        assertFalse(BookingNoteRequest("my secret").toString().contains("my secret"))
    }
}
