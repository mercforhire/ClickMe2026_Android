@file:OptIn(ExperimentalCoroutinesApi::class)

package com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.model.responses.UserProfileData
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.subviews.AUTO_SAVE_STATUS_TAG
import com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.subviews.PROFESSIONAL_COMPANY_TAG
import com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.subviews.PROFESSIONAL_JOB_TITLE_TAG
import com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.subviews.PROFILE_AVATAR_PROGRESS_TAG
import com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.subviews.PROFILE_AVATAR_TAG
import com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.subviews.PROFILE_BIO_COUNTER_TAG
import com.uptrendinvestments.clickmeapp.screens.settings.clientprofilesettings.subviews.ProfessionalDetailsBody
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.SignupTestHarness
import com.uptrendinvestments.clickmeapp.testing.userProfileData
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ClientProfileSettingsScreenTest {
    @get:Rule(order = 0)
    val main = MainDispatcherRule(UnconfinedTestDispatcher())

    @get:Rule(order = 1)
    val rule = createComposeRule()

    @get:Rule(order = 2)
    val tmp = TemporaryFolder()

    private val changes = mutableListOf<Pair<ProfileTextFieldId, String>>()
    private val retries = mutableListOf<Unit>()
    private val dismissals = mutableListOf<Unit>()
    private val backs = mutableListOf<Unit>()
    private val opens = mutableListOf<Unit>()
    private val applied = mutableListOf<Pair<String, String>>()
    private val discards = mutableListOf<Unit>()
    private val pickTaps = mutableListOf<Unit>()
    private val avatarDismissals = mutableListOf<Unit>()
    private val languageOpens = mutableListOf<Unit>()
    private val countryOpens = mutableListOf<Unit>()

    private fun state(
        bio: String = "",
        status: AutoSaveStatus = AutoSaveStatus.Idle,
        dialog: FieldError? = null,
        fieldErrors: Map<ProfileTextFieldId, FieldError> = emptyMap(),
        jobTitle: String? = null,
        company: String? = null,
        sheetOpen: Boolean = false,
        avatar: AvatarUiState = AvatarUiState(),
        languages: List<UserProfileData.LanguageEntry> =
            listOf(UserProfileData.LanguageEntry(id = "en", label = "English")),
        country: String? = "CAN",
    ) = ClientProfileSettingsUiState(
        avatar = avatar,
        snapshot =
            ProfileSnapshot.from(
                userProfileData(
                    bio = bio,
                    city = "Toronto",
                    stateProvince = "ON",
                    languages = languages,
                    country = country,
                    jobTitle = jobTitle,
                    company = company,
                ),
            ),
        professionalSheetOpen = sheetOpen,
        email = "ana@example.com",
        status = status,
        dialog = dialog,
        fieldErrors = fieldErrors,
    )

    private fun setContent(state: ClientProfileSettingsUiState) {
        rule.setContent {
            ClickMeTheme {
                ClientProfileSettingsContent(
                    state = state,
                    onBack = { backs += Unit },
                    onFieldChange = { field, value -> changes += field to value },
                    onRetrySave = { retries += Unit },
                    onDismissDialog = { dismissals += Unit },
                    onOpenProfessional = { opens += Unit },
                    onApplyProfessional = { jobTitle, company -> applied += jobTitle to company },
                    onDiscardProfessional = { discards += Unit },
                    onChangePhoto = { pickTaps += Unit },
                    onDismissAvatarError = { avatarDismissals += Unit },
                    onOpenLanguages = { languageOpens += Unit },
                    onLanguageQueryChange = {},
                    onToggleLanguage = {},
                    onRetryLanguages = {},
                    onDismissLanguages = {},
                    onApplyLanguages = {},
                    onOpenCountry = { countryOpens += Unit },
                    onCountryQueryChange = {},
                    onSelectCountry = {},
                    onDismissCountry = {},
                )
            }
        }
    }

    private fun top(text: String): Float =
        rule
            .onAllNodesWithText(text)
            .onFirst()
            .fetchSemanticsNode()
            .positionInRoot.y

    @Test
    fun fieldsInOrder() {
        setContent(state())

        rule.onNodeWithContentDescription("Email, ana@example.com, read only").assertExists()
        val tops =
            listOf("First Name", "Last Name", "Bio", "City", "State/Province").map { top(it) }
        assertEquals(tops.sorted(), tops)
        assertEquals(tops.distinct(), tops)
        rule.onNodeWithText("Save").assertDoesNotExist()
    }

    @Test
    fun avatarSectionAndChangePhoto() {
        setContent(state())

        rule.onNodeWithTag(PROFILE_AVATAR_TAG).assertIsDisplayed()
        rule.onNodeWithText("Change photo").assertIsDisplayed()
        rule.onNodeWithTag(PROFILE_AVATAR_PROGRESS_TAG).assertDoesNotExist()

        rule.onNodeWithText("Change photo").performClick()
        rule.onNodeWithTag(PROFILE_AVATAR_TAG).performClick()
        assertEquals(2, pickTaps.size)
    }

    @Test
    fun uploadingShowsAProgressNodeOverTheAvatar() {
        setContent(state(avatar = AvatarUiState(isUploading = true)))

        rule.onNodeWithTag(PROFILE_AVATAR_PROGRESS_TAG).assertIsDisplayed()
        rule.onNodeWithContentDescription("Uploading photo").assertExists()
    }

    @Test
    fun avatarErrorShowsTheUploadTitleAndMessage() {
        setContent(state(avatar = AvatarUiState(error = FieldError.Text("Too big"))))

        rule.onNodeWithText("Couldn't upload avatar").assertIsDisplayed()
        rule.onNodeWithText("Too big").assertIsDisplayed()
        rule.onNodeWithText("Got it").performClick()
        assertEquals(1, avatarDismissals.size)
    }

    @Test
    fun importErrorUsesTheOpenPhotoTitle() {
        setContent(
            state(
                avatar =
                    AvatarUiState(
                        error = FieldError.Res(R.string.signup_photo_open_error_body),
                        errorTitleRes = R.string.signup_photo_open_error_title,
                    ),
            ),
        )

        rule.onNodeWithText("Couldn't open photo").assertIsDisplayed()
        rule.onNodeWithText("Couldn't upload avatar").assertDoesNotExist()
    }

    @Test
    fun languageChipsAndEmptyHint() {
        setContent(
            state(
                languages =
                    listOf(
                        UserProfileData.LanguageEntry(id = "en", label = "English"),
                        UserProfileData.LanguageEntry(id = null, label = "Klingon"),
                    ),
            ),
        )

        rule.onNodeWithText("Spoken Languages").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("English").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Klingon").assertIsDisplayed()
        rule.onNodeWithText("Add the languages you speak").assertDoesNotExist()
        rule.onNodeWithText("Edit languages").performScrollTo().performClick()
        assertEquals(1, languageOpens.size)
    }

    @Test
    fun noLanguagesShowsTheEmptyHint() {
        setContent(state(languages = emptyList()))

        rule.onNodeWithText("Add the languages you speak").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Edit languages").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun manyLanguagesWrapInsteadOfClipping() {
        val many = List(12) { UserProfileData.LanguageEntry(id = "l$it", label = "Language number $it") }
        setContent(state(languages = many))

        rule.onNodeWithText("Language number 11").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Edit languages").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun fieldOrderComplete() {
        setContent(state())

        val tops =
            listOf(
                "Change photo",
                "Email",
                "First Name",
                "Last Name",
                "Bio",
                "Professional Details",
                "City",
                "State/Province",
                "Country",
                "Spoken Languages",
            ).map { top(it) }
        assertEquals(tops.sorted(), tops)
        assertEquals(tops.distinct(), tops)
    }

    @Test
    fun countryShowsItsNameAndOpensThePicker() {
        setContent(state(country = "CAN"))

        rule.onNodeWithText("Canada").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Select").assertDoesNotExist()
        rule.onNodeWithText("Canada").performClick()
        assertEquals(1, countryOpens.size)
    }

    @Test
    fun unsetCountryShowsSelect() {
        setContent(state(country = null))

        rule.onNodeWithText("Select").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun editingAFieldReportsTheNewValue() {
        setContent(state())

        rule
            .onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag(PROFILE_FIELD_TAG_PREFIX + "FirstName")))
            .performTextReplacement("Anna")

        assertEquals(listOf(ProfileTextFieldId.FirstName to "Anna"), changes)
    }

    @Test
    fun statusIconStates() {
        var status by mutableStateOf(AutoSaveStatus.Idle)
        rule.setContent {
            ClickMeTheme {
                ClientProfileSettingsContent(
                    state = state(status = status),
                    onBack = {},
                    onFieldChange = { _, _ -> },
                    onRetrySave = { retries += Unit },
                    onDismissDialog = {},
                    onOpenProfessional = {},
                    onApplyProfessional = { _, _ -> },
                    onDiscardProfessional = {},
                    onChangePhoto = {},
                    onDismissAvatarError = {},
                    onOpenLanguages = {},
                    onLanguageQueryChange = {},
                    onToggleLanguage = {},
                    onRetryLanguages = {},
                    onDismissLanguages = {},
                    onApplyLanguages = {},
                    onOpenCountry = { countryOpens += Unit },
                    onCountryQueryChange = {},
                    onSelectCountry = {},
                    onDismissCountry = {},
                )
            }
        }

        val saving = SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Saving")
        rule.onNodeWithTag(AUTO_SAVE_STATUS_TAG).assertExists()
        rule.onNode(saving).assertDoesNotExist()
        rule.onNodeWithContentDescription("Changes saved").assertDoesNotExist()

        status = AutoSaveStatus.Saving
        rule.waitForIdle()
        rule.onNode(saving).assertExists()

        status = AutoSaveStatus.Saved
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Changes saved").assertExists()
        rule.onNode(saving).assertDoesNotExist()

        status = AutoSaveStatus.Error
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Couldn't save changes. Tap to retry.").performClick()
        assertEquals(1, retries.size)
    }

    @Test
    fun bioCounterFrom400() {
        setContent(state(bio = "a".repeat(399)))
        rule.onNodeWithTag(PROFILE_BIO_COUNTER_TAG).assertDoesNotExist()
    }

    @Test
    fun bioCounterShowsAt400() {
        setContent(state(bio = "a".repeat(400)))
        rule.onNodeWithText("400/500").assertExists()
    }

    @Test
    fun saveFailureDialogShowsTitleMessageAndGotIt() {
        setContent(state(dialog = FieldError.Text("Down")))

        rule.onNodeWithText("Couldn't save changes").assertIsDisplayed()
        rule.onNodeWithText("Down").assertIsDisplayed()
        rule.onNodeWithText("Got it").performClick()
        assertEquals(1, dismissals.size)
    }

    @Test
    fun fieldErrorShowsUnderItsField() {
        setContent(state(fieldErrors = mapOf(ProfileTextFieldId.FirstName to FieldError.Text("Too long"))))

        rule.onAllNodesWithText("Too long").assertCountEquals(1)
    }

    @Test
    fun backArrowCallsOnBack() {
        setContent(state())

        rule.onNodeWithContentDescription("Back").performClick()

        assertEquals(1, backs.size)
    }

    @Test
    fun cardShowsDashForEmpty() {
        setContent(state())

        val card =
            rule.onNode(
                SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button) and hasText("Professional Details"),
            )
        card.assertExists()
        rule.onAllNodesWithText("—", useUnmergedTree = true).assertCountEquals(2)
        rule.onNodeWithText("Job Title").assertExists()
        rule.onNodeWithText("Company").assertExists()
        card.performScrollTo().performClick()
        assertEquals(1, opens.size)
    }

    @Test
    fun cardShowsValuesAndSitsBetweenBioAndCity() {
        setContent(state(jobTitle = "Coach", company = "Acme"))

        rule.onNodeWithText("Coach").assertExists()
        rule.onNodeWithText("Acme").assertExists()
        assertTrue(top("Bio") < top("Professional Details"))
        assertTrue(top("Professional Details") < top("City"))
    }

    @Test
    fun sheetShowsWhenOpen() {
        setContent(state(jobTitle = "Coach", company = "Acme", sheetOpen = true))

        rule.onNodeWithText("Edit Professional Details").assertExists()
        rule.onNodeWithText("Update details").assertExists()
        rule.onNodeWithText("Discard changes").assertExists()
    }

    @Test
    fun sheetUpdateAppliesTheTypedValues() {
        rule.setContent {
            ClickMeTheme {
                ProfessionalDetailsBody(
                    jobTitle = "",
                    company = "",
                    onUpdate = { jobTitle, company -> applied += jobTitle to company },
                    onDiscard = { discards += Unit },
                )
            }
        }

        rule
            .onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag(PROFESSIONAL_JOB_TITLE_TAG)))
            .performTextReplacement("Coach")
        rule
            .onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag(PROFESSIONAL_COMPANY_TAG)))
            .performTextReplacement("Acme")
        rule.onNodeWithText("Update details").performClick()

        assertEquals(listOf("Coach" to "Acme"), applied)
        assertEquals(0, discards.size)
    }

    @Test
    fun sheetDiscardAppliesNothing() {
        rule.setContent {
            ClickMeTheme {
                ProfessionalDetailsBody(
                    jobTitle = "Old",
                    company = "Co",
                    onUpdate = { jobTitle, company -> applied += jobTitle to company },
                    onDiscard = { discards += Unit },
                )
            }
        }

        rule
            .onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag(PROFESSIONAL_JOB_TITLE_TAG)))
            .performTextReplacement("Changed")
        rule.onNodeWithText("Discard changes").performClick()

        assertEquals(1, discards.size)
        assertTrue(applied.isEmpty())
    }

    @Test
    fun backFlushesPendingEditThenPops() =
        runTest(main.testDispatcher) {
            val h = SignupTestHarness(this, tmp, main.testDispatcher)
            val seeded = userProfileData(firstName = "Ana", lastName = "Lee")
            h.api.userProfileHandler = { seeded }
            h.signIn()
            runCurrent()
            h.userManager.refreshProfile()
            h.api.updateUserProfileHandler = { seeded }
            val vm =
                ClientProfileSettingsViewModel(
                    ProfileSettingsRepository(h.api),
                    h.userManager,
                    ProfileAvatarController(h.photo, ProfileSettingsRepository(h.api), h.userManager, backgroundScope),
                    backgroundScope,
                )
            runCurrent()

            rule.setContent {
                ClickMeTheme {
                    ClientProfileSettingsScreen(viewModel = vm, onBack = { backs += Unit }, onOpenCrop = {})
                }
            }
            rule
                .onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag(PROFILE_FIELD_TAG_PREFIX + "LastName")))
                .performTextReplacement("Leeson")
            assertEquals(0, h.api.updateUserProfileCalls)

            rule.onNodeWithContentDescription("Back").performClick()
            runCurrent()

            assertEquals(1, h.api.updateUserProfileCalls)
            val sent = h.api.updateUserProfileRequests.single()
            assertEquals("Leeson", sent.basic?.lastName)
            assertEquals(1, backs.size)
            val shown = vm.uiState.value.snapshot
            assertEquals("Leeson", shown?.lastName)
        }
}
