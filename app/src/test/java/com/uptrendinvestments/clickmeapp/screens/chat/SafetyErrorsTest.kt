package com.uptrendinvestments.clickmeapp.screens.chat

import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.model.enums.ErrorCode
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class SafetyErrorsTest {
    private fun http(
        status: Int,
        code: ErrorCode? = null,
        message: String? = null,
    ) = ApiException.Http(
        status = status,
        code = code,
        rawCode = code?.name,
        serverMessage = message,
        fieldErrors = emptyList(),
        detail = null,
    )

    private val network = ApiException.Network(IOException("offline"))
    private val decoding = ApiException.Decoding(IllegalStateException("bad"))

    @Test
    fun reportRateLimitedWithAndWithoutACode() {
        val limited = FieldError.Res(R.string.report_error_rate_limited)

        assertEquals(limited, http(429, ErrorCode.TooManyRequests, "Too many reports.").toReportFailure())
        assertEquals(limited, http(429).toReportFailure())
    }

    @Test
    fun reportNotFoundIsUnavailable() {
        assertEquals(FieldError.Res(R.string.report_error_unavailable), http(404).toReportFailure())
    }

    @Test
    fun reportOtherClientErrorShowsTheServerMessage() {
        assertEquals(FieldError.Text("x"), http(422, message = "x").toReportFailure())
    }

    @Test
    fun blockNotFoundIsConversationUnavailable() {
        val unavailable = FieldError.Res(R.string.block_error_conversation_unavailable)

        assertEquals(unavailable, http(404).toBlockFailure())
        assertEquals(unavailable, http(404).toUnblockFailure())
    }

    @Test
    fun unblockNotBlockerShowsTheServerMessageOrTheFixedCopy() {
        assertEquals(
            FieldError.Text("Only the blocker can unblock."),
            http(403, ErrorCode.NotBlocker, "Only the blocker can unblock.").toUnblockFailure(),
        )
        assertEquals(
            FieldError.Res(R.string.unblock_error_not_blocker),
            http(403, ErrorCode.NotBlocker).toUnblockFailure(),
        )
    }

    @Test
    fun networkAndDecodingAndBareServerErrors() {
        val offline = FieldError.Res(R.string.login_error_network)
        val unknown = FieldError.Res(R.string.login_error_unknown)

        listOf(network.toReportFailure(), network.toBlockFailure(), network.toUnblockFailure()).forEach {
            assertEquals(offline, it)
        }
        listOf(decoding.toReportFailure(), decoding.toBlockFailure(), decoding.toUnblockFailure()).forEach {
            assertEquals(unknown, it)
        }
        assertEquals(unknown, http(500).toReportFailure())
        assertEquals(unknown, http(500).toBlockFailure())
        assertEquals(unknown, http(500).toUnblockFailure())
    }

    @Test
    fun accountSuspendedIsRecognisedSoTheGlobalDialogOwnsIt() {
        assertTrue(http(403, ErrorCode.AccountSuspended).isAccountSuspended())
        assertFalse(http(403, ErrorCode.NotBlocker).isAccountSuspended())
        assertFalse(network.isAccountSuspended())
    }
}
