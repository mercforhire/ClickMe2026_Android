package com.uptrendinvestments.clickmeapp.testing

import com.uptrendinvestments.clickmeapp.managers.PushTokenRegistrar
import com.uptrendinvestments.clickmeapp.managers.SessionEvents
import com.uptrendinvestments.clickmeapp.managers.SessionStore
import com.uptrendinvestments.clickmeapp.managers.TokenPair
import com.uptrendinvestments.clickmeapp.managers.UserManager
import com.uptrendinvestments.clickmeapp.model.responses.AuthUser
import com.uptrendinvestments.clickmeapp.model.responses.ExpertLanguage
import com.uptrendinvestments.clickmeapp.model.responses.ExpertLocationDetails
import com.uptrendinvestments.clickmeapp.model.responses.ExpertPersonalInfo
import com.uptrendinvestments.clickmeapp.model.responses.ExpertProfileData
import com.uptrendinvestments.clickmeapp.model.responses.ExpertTag
import com.uptrendinvestments.clickmeapp.model.responses.ExpertiseTagItem
import com.uptrendinvestments.clickmeapp.model.responses.LanguageItem
import com.uptrendinvestments.clickmeapp.model.responses.LoginData
import com.uptrendinvestments.clickmeapp.screens.signup.SignupSetupRepository
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import org.junit.rules.TemporaryFolder
import java.io.IOException

/**
 * Shared wiring for signup repository and step ViewModel tests: a real [UserManager] and
 * [SignupSetupRepository] over fakes. [signIn] goes through the existing `userManager.login`, never `signup`.
 */
class SignupTestHarness(
    scope: TestScope,
    tmp: TemporaryFolder,
    dispatcher: TestDispatcher,
) {
    val api = FakeClickMeApi()
    val tokens = FakeSecureTokenStore(TokenPair("access-1", "refresh-1", 1_900_000_000L))
    val sessionStore = SessionStore(testPreferencesDataStore(scope.backgroundScope, tmp))
    val userManager =
        UserManager(
            api,
            tokens,
            sessionStore,
            SessionEvents(),
            PushTokenRegistrar(),
            scope.backgroundScope,
            dispatcher,
        )
    val photo = FakePhotoProcessor()
    val repository = SignupSetupRepository(api, userManager, photo, scope.backgroundScope, dispatcher)

    init {
        // The background profile refresh after login is best effort; fail it quietly.
        api.userProfileHandler = { throw IOException("not needed") }
    }

    /** Logs in as an expert through the Phase 2 login path, so `userManager.isLoggedIn` becomes true. */
    suspend fun signIn() {
        api.loginHandler = {
            LoginData(
                token = "access-2",
                user = AuthUser(id = "u1", firstName = "Ada", lastName = "Lovelace", roles = listOf("expert")),
                refreshToken = "refresh-2",
                expiresAt = 1_900_000_100L,
                expiresIn = 3600L,
            )
        }
        userManager.login("ada@b.co", "abcdefg1")
    }

    fun fullProfile(
        firstName: String = "Ada",
        email: String? = "ada@b.co",
        city: String = "Toronto",
        province: String? = "ON",
        countryCode: String = "CAN",
        timezone: String? = "America/Toronto",
        avatarUrl: String? = "https://cdn.example/avatar.jpg",
        languages: List<ExpertLanguage> = listOf(ExpertLanguage("en", "English")),
        tags: List<ExpertTag> = listOf(ExpertTag("t1", "Tax")),
    ) = ExpertProfileData(
        setupCompleted = false,
        profileId = "p1",
        avatarUrl = avatarUrl,
        personalInfo = ExpertPersonalInfo(firstName = firstName, lastName = "Lovelace", email = email),
        locationDetails =
            ExpertLocationDetails(
                city = city,
                stateProvince = province,
                countryCode = countryCode,
                timezone = timezone,
            ),
        languages = languages,
        expertiseTags = tags,
    )

    fun emptyProfile() = ExpertProfileData(setupCompleted = false)

    fun tag(
        id: String,
        label: String,
    ) = ExpertiseTagItem(id, label)

    fun language(
        id: String,
        label: String,
    ) = LanguageItem(id, label)
}
