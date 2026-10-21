@file:OptIn(ExperimentalCoroutinesApi::class)

package com.uptrendinvestments.clickmeapp.screens.signup

import com.uptrendinvestments.clickmeapp.constants.Timezones
import com.uptrendinvestments.clickmeapp.model.requests.SetupExpertProfileRequest
import com.uptrendinvestments.clickmeapp.model.requests.SetupLocation
import com.uptrendinvestments.clickmeapp.model.requests.VerifyEmailCodeRequest
import com.uptrendinvestments.clickmeapp.model.responses.AvatarUploadData
import com.uptrendinvestments.clickmeapp.model.responses.EmailStatusData
import com.uptrendinvestments.clickmeapp.model.responses.ExpertLanguage
import com.uptrendinvestments.clickmeapp.model.responses.ExpertTag
import com.uptrendinvestments.clickmeapp.model.responses.SetupExpertProfileData
import com.uptrendinvestments.clickmeapp.model.responses.SuccessMessageResponse
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.SignupTestHarness
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import okhttp3.MultipartBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException

class SignupSetupRepositoryTest {
    @get:Rule
    val main = MainDispatcherRule()

    @get:Rule
    val tmp = TemporaryFolder()

    private fun harness(scope: TestScope) =
        SignupTestHarness(scope, tmp, main.testDispatcher).also {
            it.api.setupProfileHandler = { SetupExpertProfileData() }
        }

    private val freshDraft get() = SignupDraft(timezone = Timezones.detected().id)

    // ---- initial state and seeding ----

    @Test
    fun freshRepositoryHasDetectedTimezoneAndNoLanguages() =
        runTest(main.testDispatcher) {
            val h = harness(this)

            assertEquals(Timezones.detected().id, h.repository.draft.value.timezone)
            assertTrue(
                h.repository.draft.value.languages
                    .isEmpty(),
            )
            assertEquals(HydrationState.Idle, h.repository.hydration.value)
        }

    @Test
    fun seedFromSignupSetsOnlyEmailAndVerified() =
        runTest(main.testDispatcher) {
            val h = harness(this)

            h.repository.seedFromSignup("ada@b.co", false)

            assertEquals(freshDraft.copy(email = "ada@b.co", emailVerified = false), h.repository.draft.value)
        }

    // ---- hydration ----

    @Test
    fun hydrateFillsTheDraftFromProfileAndEmailStatus() =
        runTest(main.testDispatcher) {
            val h = harness(this)
            h.repository.seedFromSignup("seed@b.co", false)
            h.api.expertProfileHandler = {
                h.fullProfile(
                    languages = listOf(ExpertLanguage("en", "English"), ExpertLanguage(null, "Klingon")),
                    tags = listOf(ExpertTag("t2", "Audit"), ExpertTag("t1", "Tax")),
                )
            }
            h.api.emailStatusHandler = { EmailStatusData(verified = true) }

            val ok = h.repository.hydrate()

            val draft = h.repository.draft.value
            assertTrue(ok)
            assertEquals(HydrationState.Loaded, h.repository.hydration.value)
            assertEquals("Ada", draft.firstName)
            assertEquals("Toronto", draft.city)
            assertEquals("ON", draft.provinceState)
            assertEquals("CAN", draft.countryCode)
            assertEquals("America/Toronto", draft.timezone)
            assertEquals("https://cdn.example/avatar.jpg", draft.avatarUrl)
            assertEquals(listOf(h.language("en", "English")), draft.languages)
            assertEquals(listOf(h.tag("t2", "Audit"), h.tag("t1", "Tax")), draft.expertiseTags)
            assertTrue(draft.emailVerified)
            assertEquals("ada@b.co", draft.email)
            assertTrue(draft.publishBlockedSteps.isEmpty())
        }

    @Test
    fun hydrateKeepsDeviceTimezoneAndEmptyLanguagesWhenServerHasNone() =
        runTest(main.testDispatcher) {
            val h = harness(this)
            h.api.expertProfileHandler = { h.fullProfile(timezone = null, languages = emptyList()) }
            h.api.emailStatusHandler = { EmailStatusData(verified = false) }

            h.repository.hydrate()

            assertEquals(Timezones.detected().id, h.repository.draft.value.timezone)
            assertTrue(
                h.repository.draft.value.languages
                    .isEmpty(),
            )
        }

    @Test
    fun hydrateWithFailingStatusMergesProfileAndReportsFailure() =
        runTest(main.testDispatcher) {
            val h = harness(this)
            h.api.expertProfileHandler = { h.fullProfile() }
            h.api.emailStatusHandler = { throw IOException("offline") }

            val ok = h.repository.hydrate()

            assertFalse(ok)
            assertEquals(HydrationState.Failed, h.repository.hydration.value)
            assertEquals("Ada", h.repository.draft.value.firstName)
            assertFalse(h.repository.draft.value.emailVerified)
        }

    @Test
    fun hydrateWithBothFailingLeavesTheDraftUnchanged() =
        runTest(main.testDispatcher) {
            val h = harness(this)
            h.api.expertProfileHandler = { throw IOException("offline") }
            h.api.emailStatusHandler = { throw IOException("offline") }

            val ok = h.repository.hydrate()

            assertFalse(ok)
            assertEquals(HydrationState.Failed, h.repository.hydration.value)
            assertEquals(freshDraft, h.repository.draft.value)
        }

    @Test
    fun concurrentHydrateCallsShareOneProfileRequest() =
        runTest(main.testDispatcher) {
            val h = harness(this)
            val gate = CompletableDeferred<Unit>()
            h.api.expertProfileHandler = {
                gate.await()
                h.fullProfile()
            }
            h.api.emailStatusHandler = { EmailStatusData(verified = false) }

            val first = async { h.repository.hydrate() }
            val second = async { h.repository.hydrate() }
            runCurrent()
            gate.complete(Unit)

            assertTrue(first.await())
            assertTrue(second.await())
            assertEquals(1, h.api.expertProfileCalls)
        }

    @Test
    fun ensureHydratedOnlyHydratesFromIdle() =
        runTest(main.testDispatcher) {
            val h = harness(this)
            h.api.expertProfileHandler = { h.fullProfile() }
            h.api.emailStatusHandler = { EmailStatusData(verified = false) }

            h.repository.ensureHydrated()
            h.repository.ensureHydrated()

            assertEquals(1, h.api.expertProfileCalls)
            assertEquals(HydrationState.Loaded, h.repository.hydration.value)
        }

    // ---- per-step saves ----

    @Test
    fun saveBasicInfoSendsOnlyFilledFieldsAndKeepsTheOldCity() =
        runTest(main.testDispatcher) {
            val h = harness(this)
            h.api.expertProfileHandler = { h.fullProfile(firstName = "Old") }
            h.api.emailStatusHandler = { EmailStatusData(verified = false) }
            h.repository.hydrate()
            h.repository.saveBasicInfo("Ada", "", "", "ca", listOf(h.language("en", "English")))

            // The hydrated draft already has English, so the list is sent in tap order.
            val request = h.api.setupProfileRequests.single()
            assertEquals(
                SetupExpertProfileRequest(
                    firstName = "Ada",
                    location = SetupLocation(countryCode = "CAN"),
                    languages = listOf("en"),
                ),
                request,
            )
            val draft = h.repository.draft.value
            assertEquals("Ada", draft.firstName)
            assertEquals("CAN", draft.countryCode)
            assertEquals("Toronto", draft.city)
            assertEquals(listOf(h.language("en", "English")), draft.languages)
        }

    @Test
    fun saveBasicInfoWithNothingFilledMakesNoRequest() =
        runTest(main.testDispatcher) {
            val h = harness(this)

            h.repository.saveBasicInfo("  ", "", "", "", emptyList())

            assertEquals(0, h.api.setupProfileCalls)
            assertEquals(freshDraft, h.repository.draft.value)
        }

    @Test
    fun tagsFollowTheThreeCaseListRule() =
        runTest(main.testDispatcher) {
            val h = harness(this)

            // Empty selection on an empty draft: no request.
            h.repository.saveTags(emptyList())
            assertEquals(0, h.api.setupProfileCalls)

            // Non-empty selection: ids in tap order.
            h.repository.saveTags(listOf(h.tag("b", "B"), h.tag("a", "A")))
            assertEquals(
                listOf("b", "a"),
                h.api.setupProfileRequests
                    .last()
                    .expertiseTags,
            )
            assertEquals(listOf(h.tag("b", "B"), h.tag("a", "A")), h.repository.draft.value.expertiseTags)

            // Empty selection after a non-empty draft: [] clears on purpose.
            h.repository.saveTags(emptyList())
            assertEquals(
                emptyList<String>(),
                h.api.setupProfileRequests
                    .last()
                    .expertiseTags,
            )
            assertTrue(
                h.repository.draft.value.expertiseTags
                    .isEmpty(),
            )
            assertEquals(2, h.api.setupProfileCalls)
        }

    @Test
    fun languagesFollowTheSameListRule() =
        runTest(main.testDispatcher) {
            val h = harness(this)
            h.repository.saveBasicInfo(
                "Ada",
                "",
                "",
                "",
                listOf(h.language("fr", "French"), h.language("en", "English")),
            )
            assertEquals(
                listOf("fr", "en"),
                h.api.setupProfileRequests
                    .last()
                    .languages,
            )

            h.repository.saveBasicInfo("Ada", "", "", "", emptyList())
            assertEquals(
                emptyList<String>(),
                h.api.setupProfileRequests
                    .last()
                    .languages,
            )
            assertTrue(
                h.repository.draft.value.languages
                    .isEmpty(),
            )
        }

    @Test
    fun listFieldForCoversTheThreeCases() {
        assertEquals(listOf("b", "a"), listFieldFor(listOf("b", "a"), listOf("x")))
        assertEquals(emptyList<String>(), listFieldFor(emptyList(), listOf("x")))
        assertNull(listFieldFor(emptyList(), emptyList()))
    }

    @Test
    fun saveTimezoneSendsOnlyTimezone() =
        runTest(main.testDispatcher) {
            val h = harness(this)

            h.repository.saveTimezone("Asia/Kolkata")

            assertEquals(SetupExpertProfileRequest(timezone = "Asia/Kolkata"), h.api.setupProfileRequests.single())
            assertEquals("Asia/Kolkata", h.repository.draft.value.timezone)
        }

    @Test
    fun uploadAvatarSendsOneJpegImagePartAndStoresTheUrl() =
        runTest(main.testDispatcher) {
            val h = harness(this)
            var captured: MultipartBody.Part? = null
            h.api.uploadAvatarHandler = {
                captured = it
                AvatarUploadData("https://cdn.example/new.jpg")
            }

            val url = h.repository.uploadAvatar(byteArrayOf(1, 2, 3))

            assertEquals("https://cdn.example/new.jpg", url)
            assertEquals(url, h.repository.draft.value.avatarUrl)
            assertEquals(1, h.api.uploadAvatarCalls)
            assertEquals("image/jpeg", captured?.body?.contentType().toString())
            assertTrue(captured?.headers?.get("Content-Disposition")?.contains("name=\"image\"") == true)
        }

    @Test
    fun removeAvatarCallsDeleteAndClearsTheUrl() =
        runTest(main.testDispatcher) {
            val h = harness(this)
            h.api.uploadAvatarHandler = { AvatarUploadData("u") }
            h.api.deleteAvatarHandler = { SuccessMessageResponse() }
            h.repository.uploadAvatar(byteArrayOf(1))

            h.repository.removeAvatar()

            assertEquals(1, h.api.deleteAvatarCalls)
            assertNull(h.repository.draft.value.avatarUrl)
        }

    @Test
    fun failedSavesThrowApiExceptionAndLeaveTheDraftUnchanged() =
        runTest(main.testDispatcher) {
            val h = harness(this)
            h.api.setupProfileHandler = { throw h.api.httpError(422, """{"message":"bad"}""") }

            try {
                h.repository.saveTimezone("Asia/Kolkata")
                fail("expected ApiException.Http")
            } catch (e: ApiException.Http) {
                assertEquals(422, e.status)
            }
            h.api.setupProfileHandler = { throw IOException("offline") }
            try {
                h.repository.saveTags(listOf(h.tag("a", "A")))
                fail("expected ApiException.Network")
            } catch (e: ApiException.Network) {
                // expected
            }

            assertEquals(freshDraft, h.repository.draft.value)
        }

    // ---- email verification ----

    @Test
    fun verifyCodeSendsTheCodeAndMarksTheEmailVerified() =
        runTest(main.testDispatcher) {
            val h = harness(this)
            var sent: VerifyEmailCodeRequest? = null
            h.api.verifyEmailHandler = {
                sent = it
                SuccessMessageResponse()
            }

            h.repository.verifyCode("123456")

            assertEquals(VerifyEmailCodeRequest("123456"), sent)
            assertTrue(h.repository.draft.value.emailVerified)
        }

    @Test
    fun sendVerificationCodeCallsResendOnceAndChangesNothing() =
        runTest(main.testDispatcher) {
            val h = harness(this)
            h.api.resendEmailHandler = { SuccessMessageResponse() }

            h.repository.sendVerificationCode()

            assertEquals(1, h.api.resendEmailCalls)
            assertEquals(freshDraft, h.repository.draft.value)
        }

    @Test
    fun recordCodeSentStoresTheEndTimeAndResetClearsIt() =
        runTest(main.testDispatcher) {
            val h = harness(this)

            h.repository.recordCodeSent(42_000L)
            assertEquals(
                VerificationSendState(codeSent = true, cooldownEndsAt = 42_000L),
                h.repository.verificationSend.value,
            )

            h.repository.recordCodeSent(72_000L)
            assertEquals(72_000L, h.repository.verificationSend.value.cooldownEndsAt)

            h.repository.reset()
            assertEquals(VerificationSendState(), h.repository.verificationSend.value)
        }

    @Test
    fun refreshEmailStatusUpdatesTheVerifiedFlag() =
        runTest(main.testDispatcher) {
            val h = harness(this)
            h.api.emailStatusHandler = { EmailStatusData(verified = true) }

            h.repository.refreshEmailStatus()

            assertTrue(h.repository.draft.value.emailVerified)
        }

    // ---- publish ----

    @Test
    fun publishSendsTheFullNormalizedPayload() =
        runTest(main.testDispatcher) {
            val h = harness(this)
            h.api.expertProfileHandler = {
                h.fullProfile(firstName = " Ada ", city = " Toronto ", province = " ", countryCode = "ca")
            }
            h.api.emailStatusHandler = { EmailStatusData(verified = false) }
            h.repository.hydrate()

            h.repository.publish()

            assertEquals(
                SetupExpertProfileRequest(
                    firstName = "Ada",
                    location = SetupLocation(city = "Toronto", provinceState = null, countryCode = "CAN"),
                    timezone = "America/Toronto",
                    languages = listOf("en"),
                    expertiseTags = listOf("t1"),
                    setupCompleted = true,
                ),
                h.api.setupProfileRequests.single(),
            )
        }

    @Test
    fun publishWith422MissingFieldsBlocksTheMappedSteps() =
        runTest(main.testDispatcher) {
            val h = harness(this)
            h.api.expertProfileHandler = { h.fullProfile() }
            h.api.emailStatusHandler = { EmailStatusData(verified = false) }
            h.repository.hydrate()
            h.api.setupProfileHandler = {
                throw h.api.httpError(
                    422,
                    """{"message":"incomplete","missing_fields":["profile_image_url","expertise_tags"]}""",
                )
            }

            try {
                h.repository.publish()
                fail("expected ApiException.Http")
            } catch (e: ApiException.Http) {
                assertEquals(422, e.status)
            }

            assertEquals(
                setOf(SetupStep.ProfilePhoto, SetupStep.Expertise),
                h.repository.draft.value.publishBlockedSteps,
            )
            assertFalse(h.repository.draft.value.canPublish)

            // The next successful hydrate clears the blocked steps.
            h.repository.hydrate()
            assertTrue(
                h.repository.draft.value.publishBlockedSteps
                    .isEmpty(),
            )
        }

    /** Publishes against a 422 that names [fields], leaving those steps blocked (D-06). */
    private suspend fun blockOnPublish(
        h: SignupTestHarness,
        vararg fields: String,
    ) {
        val list = fields.joinToString(",") { "\"$it\"" }
        h.api.setupProfileHandler = {
            throw h.api.httpError(422, """{"message":"incomplete","missing_fields":[$list]}""")
        }
        try {
            h.repository.publish()
            fail("expected ApiException.Http")
        } catch (e: ApiException.Http) {
            assertEquals(422, e.status)
        }
        h.api.setupProfileHandler = { SetupExpertProfileData() }
    }

    @Test
    fun uploadingThePhotoLiftsOnlyThePhotoBlock() =
        runTest(main.testDispatcher) {
            val h = harness(this)
            h.api.expertProfileHandler = { h.fullProfile() }
            h.api.emailStatusHandler = { EmailStatusData(verified = false) }
            h.repository.hydrate()
            blockOnPublish(h, "profile_image_url", "expertise_tags")
            h.api.uploadAvatarHandler = { AvatarUploadData("https://cdn.example/new.jpg") }

            h.repository.uploadAvatar(byteArrayOf(1))

            val draft = h.repository.draft.value
            assertEquals(setOf(SetupStep.Expertise), draft.publishBlockedSteps)
            assertTrue(draft.isComplete(SetupStep.ProfilePhoto))
            assertFalse(draft.isComplete(SetupStep.Expertise))
            assertFalse(draft.canPublish)
        }

    @Test
    fun eachStepSaveLiftsItsOwnBlockSoPublishIsEnabledAgain() =
        runTest(main.testDispatcher) {
            val h = harness(this)
            h.api.expertProfileHandler = { h.fullProfile() }
            h.api.emailStatusHandler = { EmailStatusData(verified = false) }
            h.repository.hydrate()
            blockOnPublish(h, "first_name", "timezone", "expertise_tags")
            assertEquals(
                setOf(SetupStep.PersonalDetails, SetupStep.Timezone, SetupStep.Expertise),
                h.repository.draft.value.publishBlockedSteps,
            )

            h.repository.saveBasicInfo("Ada", "", "", "", listOf(h.language("en", "English")))
            assertEquals(
                setOf(SetupStep.Timezone, SetupStep.Expertise),
                h.repository.draft.value.publishBlockedSteps,
            )
            h.repository.saveTimezone("America/Toronto")
            assertEquals(setOf(SetupStep.Expertise), h.repository.draft.value.publishBlockedSteps)
            h.repository.saveTags(listOf(h.tag("t1", "Tax")))

            assertTrue(
                h.repository.draft.value.publishBlockedSteps
                    .isEmpty(),
            )
            assertTrue(h.repository.draft.value.canPublish)
        }

    @Test
    fun aFailedSaveKeepsTheBlock() =
        runTest(main.testDispatcher) {
            val h = harness(this)
            h.api.expertProfileHandler = { h.fullProfile() }
            h.api.emailStatusHandler = { EmailStatusData(verified = false) }
            h.repository.hydrate()
            blockOnPublish(h, "timezone")
            h.api.setupProfileHandler = { throw IOException("offline") }

            try {
                h.repository.saveTimezone("America/Toronto")
                fail("expected ApiException.Network")
            } catch (e: ApiException.Network) {
                // expected
            }

            assertEquals(setOf(SetupStep.Timezone), h.repository.draft.value.publishBlockedSteps)
        }

    // ---- reset and session end ----

    @Test
    fun logoutResetsDraftHydrationSendStateAndPhotoFiles() =
        runTest(main.testDispatcher) {
            val h = harness(this)
            h.signIn()
            runCurrent()
            h.repository.seedFromSignup("ada@b.co", false)
            h.repository.recordCodeSent(30_000L)

            h.userManager.logout()
            runCurrent()

            assertEquals(freshDraft, h.repository.draft.value)
            assertEquals(HydrationState.Idle, h.repository.hydration.value)
            assertEquals(VerificationSendState(), h.repository.verificationSend.value)
            assertEquals(1, h.photo.clearCalls)
        }

    @Test
    fun constructionWhileLoggedOutDoesNotResetALaterSeed() =
        runTest(main.testDispatcher) {
            val h = harness(this)
            runCurrent()

            h.repository.seedFromSignup("ada@b.co", false)
            runCurrent()

            assertEquals("ada@b.co", h.repository.draft.value.email)
            assertEquals(0, h.photo.clearCalls)
        }

    @Test
    fun resetClearsTheDraftAndPhotoFiles() =
        runTest(main.testDispatcher) {
            val h = harness(this)
            h.repository.seedFromSignup("ada@b.co", true)
            h.repository.saveTimezone("Asia/Kolkata")

            h.repository.reset()

            assertEquals(freshDraft, h.repository.draft.value)
            assertEquals(1, h.photo.clearCalls)
        }

    @Test
    fun hydrateStillInFlightWhenResetRunsDoesNotWriteIntoTheNewDraft() =
        runTest(main.testDispatcher) {
            val h = harness(this)
            val gate = CompletableDeferred<Unit>()
            h.api.expertProfileHandler = {
                gate.await()
                h.fullProfile()
            }
            h.api.emailStatusHandler = { EmailStatusData(verified = true) }
            val pending = async { h.repository.hydrate() }
            runCurrent()

            h.repository.reset()
            gate.complete(Unit)
            runCurrent()

            assertFalse(pending.await())
            assertEquals(freshDraft, h.repository.draft.value)
            assertEquals(HydrationState.Idle, h.repository.hydration.value)
        }
}
