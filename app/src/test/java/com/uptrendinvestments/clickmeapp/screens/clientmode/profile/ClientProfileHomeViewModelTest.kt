@file:OptIn(ExperimentalCoroutinesApi::class)

package com.uptrendinvestments.clickmeapp.screens.clientmode.profile

import com.uptrendinvestments.clickmeapp.model.responses.AuthUser
import com.uptrendinvestments.clickmeapp.model.responses.MeData
import com.uptrendinvestments.clickmeapp.model.responses.UserProfileData
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.SignupTestHarness
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ClientProfileHomeViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    @get:Rule
    val tmp = TemporaryFolder()

    private fun profile(
        first: String? = "Ana",
        last: String? = "Lee",
        email: String? = "ana@example.com",
        avatar: String? = "https://cdn.example/ana.jpg",
    ) = UserProfileData(
        personalDetails =
            UserProfileData.PersonalDetails(
                firstName = first,
                lastName = last,
                email = email,
                avatarUrl = avatar,
            ),
        professionalDetails = UserProfileData.ProfessionalDetails(jobTitle = "Coach"),
        location = UserProfileData.Location(),
    )

    @Test
    fun cardMirrorsCachedProfile() =
        runTest(main.testDispatcher) {
            val h = SignupTestHarness(this, tmp, main.testDispatcher)
            h.api.userProfileHandler = { profile() }
            h.signIn()
            runCurrent()

            val vm = ClientProfileHomeViewModel(h.userManager)
            runCurrent()

            assertEquals(ProfileHubCard("Ana Lee", "ana@example.com", "https://cdn.example/ana.jpg"), vm.card.value)
        }

    @Test
    fun cardFallsBackToAuthUserThenNulls() =
        runTest(main.testDispatcher) {
            val h = SignupTestHarness(this, tmp, main.testDispatcher)
            val empty = ClientProfileHomeViewModel(h.userManager)
            runCurrent()
            assertEquals(ProfileHubCard(null, null, null), empty.card.value)

            // The harness fails the profile fetch, so only the login payload (Ada Lovelace) is known.
            h.signIn()
            runCurrent()
            assertEquals(ProfileHubCard("Ada Lovelace", null, null), empty.card.value)
        }

    @Test
    fun blankEmailBecomesNullAndMeSuppliesAMissingOne() {
        val blank = profileHubCard(profile(email = "   "), null, null)
        assertEquals(null, blank.email)

        val me = MeData(id = "u1", email = "me@example.com", roles = listOf("client"))
        assertEquals("me@example.com", profileHubCard(profile(email = null), null, me).email)
        assertEquals("me@example.com", profileHubCard(null, null, me).email)
    }

    @Test
    fun blankNamesAndAvatarsFallThroughToAuthUser() {
        val auth = AuthUser("u1", "Grace", "Hopper", listOf("client"), "https://cdn.example/g.jpg")
        val card = profileHubCard(profile(first = " ", last = "", avatar = ""), auth, null)
        assertEquals("Grace Hopper", card.name)
        assertEquals("https://cdn.example/g.jpg", card.avatarUrl)
    }
}
