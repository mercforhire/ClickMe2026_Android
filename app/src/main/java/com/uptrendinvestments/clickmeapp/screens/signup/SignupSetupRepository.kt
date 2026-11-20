package com.uptrendinvestments.clickmeapp.screens.signup

import com.uptrendinvestments.clickmeapp.constants.Countries
import com.uptrendinvestments.clickmeapp.constants.Timezones
import com.uptrendinvestments.clickmeapp.di.ApplicationScope
import com.uptrendinvestments.clickmeapp.di.IoDispatcher
import com.uptrendinvestments.clickmeapp.managers.UserManager
import com.uptrendinvestments.clickmeapp.model.requests.SetupExpertProfileRequest
import com.uptrendinvestments.clickmeapp.model.requests.SetupLocation
import com.uptrendinvestments.clickmeapp.model.requests.VerifyEmailCodeRequest
import com.uptrendinvestments.clickmeapp.model.responses.EmailStatusData
import com.uptrendinvestments.clickmeapp.model.responses.ExpertProfileData
import com.uptrendinvestments.clickmeapp.model.responses.ExpertiseTagItem
import com.uptrendinvestments.clickmeapp.model.responses.LanguageItem
import com.uptrendinvestments.clickmeapp.network.ApiException
import com.uptrendinvestments.clickmeapp.network.ClickMeApi
import com.uptrendinvestments.clickmeapp.network.apiCall
import com.uptrendinvestments.clickmeapp.screens.signup.profilephoto.PhotoProcessor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

/**
 * The Verify Email send state that must survive leaving and re-entering the step (success criterion 2).
 * [cooldownEndsAt] is an absolute end time on the caller's monotonic clock; this class never reads a clock.
 */
data class VerificationSendState(
    val codeSent: Boolean = false,
    val cooldownEndsAt: Long = 0L,
)

/**
 * D-25 / D-14 rule for the `languages` and `expertise_tags` PATCH fields. A non-empty selection is sent in tap order;
 * an empty selection sends `[]` only when it empties a list that was non-empty in the draft (the server replaces the
 * whole array); otherwise the field is omitted (null) so an untouched list is never cleared.
 */
internal fun listFieldFor(
    selectedIds: List<String>,
    savedIds: List<String>,
): List<String>? =
    when {
        selectedIds.isNotEmpty() -> selectedIds
        savedIds.isNotEmpty() -> emptyList()
        else -> null
    }

/**
 * App-scoped accumulator for the expert signup setup, ported from iOS `SignupAccumulator`. The hub, the five steps
 * and Review are separate navigation entries with entry-scoped ViewModels, so they share state through this
 * singleton. It is cleared when the session ends (logout) and on [reset] after publishing, so no draft, send state
 * or photo file reaches a later signup (T-04-18). The photo files are also wiped by `UserManager` through
 * `PhotoWorkingFilesCleaner` on every logout, login and signup, because this lazily created singleton (and so its
 * logout observer) may not exist in the process where the session ends (CR-03).
 *
 * Saves send only the fields the user filled (D-04) and update the draft only when the request succeeded. This class
 * never logs draft contents (T-04-21).
 */
@Singleton
class SignupSetupRepository
    @Inject
    constructor(
        private val api: ClickMeApi,
        userManager: UserManager,
        private val photoProcessor: PhotoProcessor,
        @ApplicationScope private val appScope: CoroutineScope,
        @IoDispatcher private val io: CoroutineDispatcher,
    ) {
        private val _draft = MutableStateFlow(freshDraft())
        val draft: StateFlow<SignupDraft> = _draft.asStateFlow()

        private val _hydration = MutableStateFlow(HydrationState.Idle)
        val hydration: StateFlow<HydrationState> = _hydration.asStateFlow()

        private val _verificationSend = MutableStateFlow(VerificationSendState())
        val verificationSend: StateFlow<VerificationSendState> = _verificationSend.asStateFlow()

        private val lock = Any()
        private var hydrateInFlight: Deferred<Boolean>? = null

        /** Bumped by every [reset] so a response from the previous session never lands in the new draft. */
        @Volatile
        private var generation = 0

        /**
         * Bumped (under [lock]) by every successful step write, so a hydrate whose snapshot was read before a save
         * landed refetches instead of reverting the save (WR-02).
         */
        private var writes = 0

        init {
            // Reset only on a session end, never for the initial value (Pitfall 5).
            appScope.launch(start = CoroutineStart.UNDISPATCHED) {
                userManager.isLoggedIn
                    .drop(1)
                    .filter { !it }
                    .collect { reset() }
            }
        }

        /** Seeds the draft right after account creation. English is deliberately not seeded (Pitfall 6). */
        fun seedFromSignup(
            email: String,
            emailVerified: Boolean,
        ) {
            _draft.update { it.copy(email = email, emailVerified = emailVerified) }
        }

        // ---- hydration (D-09, D-10) ----

        /**
         * Loads `GET /expert/profile` and `GET /auth/email/status` concurrently and merges them into the draft.
         * Single-flight: concurrent callers share one run, which lives in the app scope so a screen leaving
         * composition does not cancel it. Returns true only when both requests succeeded.
         */
        suspend fun hydrate(): Boolean {
            val run =
                synchronized(lock) {
                    hydrateInFlight?.takeIf { it.isActive }
                        ?: appScope.async { runHydrate() }.also { hydrateInFlight = it }
                }
            return try {
                run.await()
            } catch (e: CancellationException) {
                // A reset cancelled the shared run; only rethrow when the caller itself was cancelled.
                currentCoroutineContext().ensureActive()
                false
            }
        }

        /** Hydrates once: only from [HydrationState.Idle]. */
        suspend fun ensureHydrated() {
            if (_hydration.value == HydrationState.Idle) hydrate()
        }

        /**
         * Fetches and merges. The server snapshot wins over the draft, so a snapshot read before a step save landed
         * would revert that save: when [writes] moved during the fetch the result is dropped and fetched again (the
         * new snapshot includes the save). If saves keep landing for [MAX_HYDRATE_ATTEMPTS] fetches, the newer draft is
         * kept and the run reports failure so the hub offers a retry. The generation check and the writes happen
         * together under [lock], so a [reset] can never be overwritten (WR-02).
         */
        private suspend fun runHydrate(): Boolean {
            val startGeneration = generation
            _hydration.value = HydrationState.Loading
            repeat(MAX_HYDRATE_ATTEMPTS) {
                val startWrites = synchronized(lock) { writes }
                val (profile, status) =
                    coroutineScope {
                        val profileCall = async { attempt { api.getExpertProfile().data } }
                        val statusCall = async { attempt { api.getEmailStatus().data } }
                        profileCall.await() to statusCall.await()
                    }
                synchronized(lock) {
                    if (startGeneration != generation) return false
                    if (startWrites == writes) {
                        if (profile != null || status != null) {
                            _draft.update { current ->
                                var merged = current
                                if (profile != null) {
                                    merged = merged.mergeProfile(profile).copy(publishBlockedSteps = emptySet())
                                }
                                if (status != null) merged = merged.mergeStatus(status)
                                merged
                            }
                        }
                        val ok = profile != null && status != null
                        _hydration.value = if (ok) HydrationState.Loaded else HydrationState.Failed
                        return ok
                    }
                }
            }
            synchronized(lock) {
                if (startGeneration == generation) _hydration.value = HydrationState.Failed
            }
            return false
        }

        private suspend fun <T> attempt(block: suspend () -> T): T? =
            try {
                apiCall(block)
            } catch (e: ApiException) {
                null
            }

        /**
         * Re-reads the verified flag on a hub reappearance. The endpoint only seeds the flag (iOS 05-11, SIGN-02,
         * `SignupAccumulator.refreshEmailVerified`): a draft that is already verified is never downgraded and costs
         * no request, and an unverified draft becomes verified only on a `verified: true` answer. Errors from the
         * request propagate to the caller.
         */
        suspend fun refreshEmailStatus() {
            val startGeneration = generation
            if (_draft.value.emailVerified) return
            val status = apiCall { api.getEmailStatus() }.data
            if (startGeneration == generation) _draft.update { it.mergeStatus(status) }
        }

        // ---- per-step saves (D-04, D-14, D-25) ----

        /**
         * Basic Info save (D-04). [languages] is null when the user did not change the selection, so the field is
         * omitted and the server's list (which the PATCH would fully replace) is left alone (D-25, WR-03).
         *
         * A blank city or province is omitted, unless [clearCity] / [clearProvince] says the user emptied the field:
         * then, if the draft still has a value, `""` is sent so the server clears it too and the step does not
         * re-seed the old value (WR-04). The server accepts `""` for both; first name cannot be cleared (`min(1)`).
         */
        suspend fun saveBasicInfo(
            firstName: String,
            city: String,
            provinceState: String,
            countryCode: String,
            languages: List<LanguageItem>?,
            clearCity: Boolean = false,
            clearProvince: Boolean = false,
        ) {
            val current = _draft.value
            val name = firstName.trim().ifBlank { null }
            val cityValue = city.trim().ifBlank { null } ?: "".takeIf { clearCity && current.city.isNotBlank() }
            val provinceValue =
                provinceState.trim().ifBlank { null }
                    ?: "".takeIf { clearProvince && current.provinceState.isNotBlank() }
            val country = Countries.normalize(countryCode)
            val location =
                if (cityValue == null && provinceValue == null && country == null) {
                    null
                } else {
                    SetupLocation(city = cityValue, provinceState = provinceValue, countryCode = country)
                }
            val languageIds =
                languages?.let { selected -> listFieldFor(selected.map { it.id }, current.languages.map { it.id }) }
            val request = SetupExpertProfileRequest(firstName = name, location = location, languages = languageIds)
            patch(request, SetupStep.PersonalDetails) { draft ->
                draft.copy(
                    firstName = name ?: draft.firstName,
                    city = cityValue ?: draft.city,
                    provinceState = provinceValue ?: draft.provinceState,
                    countryCode = country ?: draft.countryCode,
                    languages = if (languageIds != null && languages != null) languages else draft.languages,
                    // Sending `languages` replaced the server's whole list, custom entries included (WR-07).
                    customLanguages = if (languageIds != null) emptyList() else draft.customLanguages,
                )
            }
        }

        suspend fun saveTimezone(timezone: String) {
            patch(SetupExpertProfileRequest(timezone = timezone), SetupStep.Timezone) { it.copy(timezone = timezone) }
        }

        suspend fun saveTags(tags: List<ExpertiseTagItem>) {
            val tagIds = listFieldFor(tags.map { it.id }, _draft.value.expertiseTags.map { it.id })
            patch(SetupExpertProfileRequest(expertiseTags = tagIds), SetupStep.Expertise) { draft ->
                if (tagIds != null) draft.copy(expertiseTags = tags) else draft
            }
        }

        /**
         * Sends [request] when it carries any field, then applies [apply] to the draft unless the session ended. A
         * successful save also lifts [step]'s D-06 publish block, so the row re-derives its state from the saved fields
         * (the server re-checks everything on the next publish anyway).
         */
        private suspend fun patch(
            request: SetupExpertProfileRequest,
            step: SetupStep,
            apply: (SignupDraft) -> SignupDraft,
        ) {
            if (!request.hasAnyField()) return
            val startGeneration = generation
            apiCall { api.setupExpertProfile(request) }
            applyWrite(startGeneration) { apply(it).unblock(step) }
        }

        /** Applies a successful step write to the draft unless the session ended, and counts it for [runHydrate]. */
        private fun applyWrite(
            startGeneration: Int,
            transform: (SignupDraft) -> SignupDraft,
        ) {
            synchronized(lock) {
                if (startGeneration != generation) return
                writes++
                _draft.update(transform)
            }
        }

        // ---- avatar ----

        /** Uploads the cropped JPEG as multipart field `image` (D-12) and returns the stored avatar URL. */
        suspend fun uploadAvatar(jpeg: ByteArray): String {
            val startGeneration = generation
            val body = jpeg.toRequestBody("image/jpeg".toMediaType())
            val part = MultipartBody.Part.createFormData("image", "avatar.jpg", body)
            val url = apiCall { api.uploadAvatar(part) }.data.avatarUrl
            applyWrite(startGeneration) { it.copy(avatarUrl = url).unblock(SetupStep.ProfilePhoto) }
            return url
        }

        suspend fun removeAvatar() {
            val startGeneration = generation
            apiCall { api.deleteAvatar() }
            applyWrite(startGeneration) { it.copy(avatarUrl = null) }
        }

        // ---- email verification (D-07) ----

        suspend fun sendVerificationCode() {
            apiCall { api.resendVerificationEmail() }
        }

        suspend fun verifyCode(code: String) {
            val startGeneration = generation
            apiCall { api.verifyEmailCode(VerifyEmailCodeRequest(code)) }
            applyWrite(startGeneration) { it.copy(emailVerified = true).unblock(SetupStep.VerifyEmail) }
        }

        /**
         * Records a code send (or a 429 rate limit) so the resend cooldown survives leaving the step.
         * [cooldownEndsAt] is stored as given, on the caller's monotonic clock.
         */
        fun recordCodeSent(cooldownEndsAt: Long) {
            _verificationSend.value = VerificationSendState(codeSent = true, cooldownEndsAt = cooldownEndsAt)
        }

        // ---- publish (D-05, D-06) ----

        /**
         * Re-sends the full payload with `setup_completed = true` and a normalized `country_code`. A 422 carrying
         * `missing_fields` marks the matching steps blocked until that step saves again or the next hydrate, then
         * rethrows.
         */
        suspend fun publish() {
            val startGeneration = generation
            val current = _draft.value
            val request =
                SetupExpertProfileRequest(
                    firstName = current.firstName.trim(),
                    location =
                        SetupLocation(
                            city = current.city.trim(),
                            provinceState = current.provinceState.trim().ifBlank { null },
                            countryCode = Countries.normalize(current.countryCode),
                        ),
                    timezone = current.timezone,
                    // WR-07: with custom languages on the server the catalogue list is unchanged since the hydrate (any
                    // language save clears them), so it is omitted rather than replaced, which would delete them.
                    languages = current.languages.map { it.id }.takeIf { current.customLanguages.isEmpty() },
                    expertiseTags = current.expertiseTags.map { it.id },
                    setupCompleted = true,
                )
            try {
                apiCall { api.setupExpertProfile(request) }
            } catch (e: ApiException.Http) {
                if (e.status == UNPROCESSABLE && e.missingFields.isNotEmpty() && startGeneration == generation) {
                    val blocked = e.missingFields.mapNotNull(SetupStep::fromMissingField).toSet()
                    _draft.update { it.copy(publishBlockedSteps = blocked) }
                }
                throw e
            }
        }

        // ---- reset (T-04-13, T-04-18) ----

        /** Clears the draft, hydration and verification state, and deletes the photo working files. */
        suspend fun reset() {
            synchronized(lock) {
                generation++
                hydrateInFlight?.cancel()
                hydrateInFlight = null
            }
            _draft.value = freshDraft()
            _hydration.value = HydrationState.Idle
            _verificationSend.value = VerificationSendState()
            withContext(NonCancellable + io) { photoProcessor.clearWorkingFiles() }
        }

        /** Lifts the D-06 publish block on [step] after a successful save of that step. */
        private fun SignupDraft.unblock(step: SetupStep): SignupDraft =
            if (step in publishBlockedSteps) copy(publishBlockedSteps = publishBlockedSteps - step) else this

        /**
         * The status endpoint only seeds the flag (iOS 05-11, SIGN-02): a `verified: true` answer ticks the row, a
         * `false` one never unticks a local success such as a just-verified code.
         */
        private fun SignupDraft.mergeStatus(status: EmailStatusData): SignupDraft =
            copy(emailVerified = emailVerified || status.verified)

        private fun SignupDraft.mergeProfile(profile: ExpertProfileData): SignupDraft {
            val info = profile.personalInfo
            val location = profile.locationDetails
            return copy(
                email = info?.email.takeUnless { it.isNullOrBlank() } ?: email,
                firstName = info?.firstName.takeUnless { it.isNullOrBlank() } ?: firstName,
                city = location?.city.takeUnless { it.isNullOrBlank() } ?: city,
                provinceState = location?.stateProvince.takeUnless { it.isNullOrBlank() } ?: provinceState,
                countryCode = location?.countryCode.takeUnless { it.isNullOrBlank() } ?: countryCode,
                timezone = location?.timezone.takeUnless { it.isNullOrBlank() } ?: timezone,
                avatarUrl = profile.avatarUrl,
                languages =
                    profile.languages?.mapNotNull { lang -> lang.id?.let { LanguageItem(it, lang.label ?: it) } }
                        ?: languages,
                customLanguages =
                    profile.languages?.filter { it.id == null }?.map { it.label.orEmpty() } ?: customLanguages,
                expertiseTags = profile.expertiseTags?.map { ExpertiseTagItem(it.id, it.label) } ?: expertiseTags,
            )
        }

        private fun SetupExpertProfileRequest.hasAnyField(): Boolean =
            firstName != null ||
                location != null ||
                timezone != null ||
                languages != null ||
                expertiseTags != null ||
                setupCompleted != null

        private companion object {
            const val UNPROCESSABLE = 422

            /** Fetches a hydrate makes before giving up on a snapshot that no save raced (WR-02). */
            const val MAX_HYDRATE_ATTEMPTS = 3

            /** The device zone is the Timezone step's default, so a fresh draft already counts it (D-09). */
            fun freshDraft() = SignupDraft(timezone = Timezones.detected().id)
        }
    }
