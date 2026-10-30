@file:OptIn(ExperimentalCoroutinesApi::class)

package com.uptrendinvestments.clickmeapp.screens.expertmode.taxprofile

import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uptrendinvestments.clickmeapp.R
import com.uptrendinvestments.clickmeapp.components.SKELETON_BLOCK_TAG
import com.uptrendinvestments.clickmeapp.components.buttons.SaveState
import com.uptrendinvestments.clickmeapp.model.responses.SaveTaxProfileData
import com.uptrendinvestments.clickmeapp.model.responses.TaxProfileAddress
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.testing.FakeClickMeApi
import com.uptrendinvestments.clickmeapp.testing.MainDispatcherRule
import com.uptrendinvestments.clickmeapp.testing.dispatchInsets
import com.uptrendinvestments.clickmeapp.testing.imeInsets
import com.uptrendinvestments.clickmeapp.testing.navigationBarInsets
import com.uptrendinvestments.clickmeapp.testing.taxProfile
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.ZoneOffset

@RunWith(RobolectricTestRunner::class)
class ExpertTaxProfileScreenTest {
    @get:Rule(order = 0)
    val main = MainDispatcherRule(UnconfinedTestDispatcher())

    @get:Rule(order = 1)
    val rule = createComposeRule()

    private val api = FakeClickMeApi()

    private fun showWithViewModel(): ExpertTaxProfileViewModel {
        val viewModel = ExpertTaxProfileViewModel(TaxProfileRepository(api), SavedStateHandle())
        rule.setContent {
            ClickMeTheme {
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                ExpertTaxProfileContent(
                    state = state,
                    actions =
                        TaxProfileActions(
                            onFieldChange = viewModel::onFieldChange,
                            onDobPicked = viewModel::onDobPicked,
                            save = viewModel::save,
                            retry = viewModel::retry,
                            dismissDialog = viewModel::dismissDialog,
                            consumeSaved = viewModel::consumeSaved,
                            consumeFocusTin = viewModel::consumeFocusTin,
                        ),
                    onBack = {},
                )
            }
        }
        return viewModel
    }

    private fun tinInput() = hasSetTextAction() and hasAnyAncestor(hasTestTag(TAX_TIN_TAG))

    /** The TIN text field whether or not it is enabled (a disabled field has no SetText action). */
    private fun tinField() =
        SemanticsMatcher.keyIsDefined(SemanticsProperties.EditableText) and hasAnyAncestor(hasTestTag(TAX_TIN_TAG))

    private fun utcMillis(
        year: Int,
        month: Int,
        day: Int,
    ): Long {
        val start = LocalDate.of(year, month, day).atStartOfDay(ZoneOffset.UTC)
        return start.toInstant().toEpochMilli()
    }

    private fun serverProfile() {
        api.taxProfileHandler = {
            taxProfile(
                legalName = "Ana Lee",
                dob = "1990-01-02",
                address =
                    TaxProfileAddress(
                        line1 = "1 Main",
                        city = "Toronto",
                        region = "ON",
                        postalCode = "M1M1M1",
                        country = "CA",
                    ),
            )
        }
    }

    @Test
    fun formAndSave() {
        serverProfile()
        api.saveTaxProfileHandler = { SaveTaxProfileData(saved = true) }
        showWithViewModel()
        rule.waitForIdle()

        rule.onNodeWithText("Tax & Payout Info").assertExists()
        rule.onNodeWithText("Required by Stripe", substring = true).assertExists()
        listOf("Legal identity", "Address", "Tax identification").forEach {
            rule.onNodeWithText(it).assertExists()
        }
        listOf(
            "Legal name",
            "Date of birth",
            "Street address",
            "Apt / suite (optional)",
            "City",
            "Province / region",
            "Postal code",
            "Country",
            "Financial account (optional)",
            "TIN (SIN / BN)",
        ).forEach { label -> rule.onAllNodesWithText(label).onFirst().assertExists() }
        rule.onNodeWithText("Jan 2, 1990").assertExists()
        rule.onNodeWithTag(TAX_SAVE_TAG).assertIsEnabled()

        rule.onNode(tinInput()).performScrollTo().performTextInput("123456789")
        rule.onNodeWithTag(TAX_SAVE_TAG).performClick()
        rule.waitForIdle()

        rule.onNodeWithText("Tax profile saved.").assertExists()
        rule.onNodeWithText("TIN on file").assertExists()
        rule.onAllNodesWithText("Replace TIN (optional)").onFirst().assertExists()
        rule.onNodeWithText("123456789").assertDoesNotExist()
    }

    @Test
    fun saveStaysOffUntilTheRequiredFieldsAreFilled() {
        api.taxProfileHandler = { taxProfile() }
        showWithViewModel()
        rule.waitForIdle()

        rule.onNodeWithTag(TAX_SAVE_TAG).assertIsNotEnabled()
    }

    // ---- Task 2: stateless content states ----

    private var retries = 0
    private var backs = 0
    private var dismissals = 0
    private var focusConsumed = 0
    private var saves = 0
    private val pickedDates = mutableListOf<Long>()
    private var backDispatcher: OnBackPressedDispatcher? = null

    private val readyForm =
        TaxProfileForm(
            legalName = "Ana Lee",
            dob = "1990-01-02",
            line1 = "1 Main",
            city = "Toronto",
            region = "ON",
            postalCode = "M1M1M1",
        )

    private val actions =
        TaxProfileActions(
            onFieldChange = { _, _ -> },
            onDobPicked = { pickedDates += it },
            save = { saves++ },
            retry = { retries++ },
            dismissDialog = { dismissals++ },
            consumeSaved = {},
            consumeFocusTin = { focusConsumed++ },
        )

    @Composable
    private fun Content(state: TaxProfileUiState) {
        backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
        ExpertTaxProfileContent(state = state, actions = actions, onBack = { backs++ })
    }

    private fun show(state: TaxProfileUiState) {
        rule.setContent { ClickMeTheme { Content(state) } }
    }

    private fun ready(
        saveState: SaveState = SaveState.Idle,
        banner: FieldError? = null,
        dialog: FieldError? = null,
        focusTin: Boolean = false,
        errors: Map<TaxProfileField, String> = emptyMap(),
    ) = TaxProfileUiState(
        load = TaxProfileLoad.Ready,
        form = readyForm,
        saveState = saveState,
        fieldErrors = errors,
        banner = banner,
        dialog = dialog,
        focusTinEvent = focusTin,
    )

    @Test
    fun tinRequiredShowsTheBannerNotADialogAndTheTinFieldTakesFocus() {
        show(ready(banner = FieldError.Text("TIN required for payouts"), focusTin = true))
        rule.waitForIdle()

        rule.onNodeWithText("TIN required for payouts").assertExists()
        rule.onNodeWithText("Couldn't save tax profile").assertDoesNotExist()
        rule.onNode(tinInput()).assertIsFocused()
        assertEquals(1, focusConsumed)
    }

    @Test
    fun theFallbackBannerCopyShowsWhenTheServerSentNoMessage() {
        show(ready(banner = FieldError.Res(R.string.tax_profile_tin_required_fallback)))

        rule.onNodeWithText("We need your TIN to finish saving. Enter it below and save again.").assertExists()
    }

    @Test
    fun inlineFieldErrorsShowUnderTheirFields() {
        show(
            ready(
                errors =
                    mapOf(
                        TaxProfileField.Tin to "TIN format is invalid",
                        TaxProfileField.Dob to "Date of birth is invalid",
                    ),
            ),
        )

        rule.onNodeWithText("TIN format is invalid").assertExists()
        rule.onNodeWithText("Date of birth is invalid").assertExists()
    }

    @Test
    fun theSaveErrorDialogShowsTheMessageAndDismisses() {
        show(ready(dialog = FieldError.Text("Check your connection and try again.")))

        rule.onNodeWithText("Couldn't save tax profile").assertExists()
        rule.onNodeWithText("Check your connection and try again.").assertExists()
        rule.onNodeWithText("Got it").performClick()
        assertEquals(1, dismissals)
    }

    @Test
    fun loadingShowsTheSkeleton() {
        show(TaxProfileUiState(load = TaxProfileLoad.Loading))

        // The skeleton group merges its blocks into one "Loading" node for TalkBack.
        rule.onNodeWithContentDescription("Loading").assertExists()
        rule.onAllNodesWithTag(SKELETON_BLOCK_TAG, useUnmergedTree = true).onFirst().assertExists()
        rule.onNodeWithTag(TAX_SAVE_TAG).assertDoesNotExist()
    }

    @Test
    fun aFailedLoadShowsTheMessageAndRetry() {
        show(TaxProfileUiState(load = TaxProfileLoad.Failed(FieldError.Text("Boom"))))

        rule.onNodeWithText("Couldn't load tax profile").assertExists()
        rule.onNodeWithText("Boom").assertExists()
        rule.onNodeWithText("Retry").performClick()
        assertEquals(1, retries)
    }

    @Test
    fun savingLocksTheFormAndIgnoresBack() {
        show(ready(saveState = SaveState.Saving))
        rule.waitForIdle()

        rule.onNode(tinField()).assertIsNotEnabled()
        rule.onNodeWithContentDescription("Back").assertIsNotEnabled()
        rule.onNodeWithTag(TAX_SAVE_TAG).performClick()
        assertEquals(0, saves)
        rule.runOnIdle { checkNotNull(backDispatcher).onBackPressed() }
        assertEquals(0, backs)
    }

    @Test
    fun theDateOfBirthFieldOpensAPickerAndReportsTheChosenDay() {
        show(ready())

        rule.onNodeWithText("Jan 2, 1990").performClick()
        rule.onNodeWithText("Keep current date").assertExists()
        rule.onNodeWithText("Set date").performClick()

        assertEquals(listOf(utcMillis(1990, 1, 2)), pickedDates)
        rule.onNodeWithText("Keep current date").assertDoesNotExist()
    }

    @Test
    fun keepingTheCurrentDateReportsNothing() {
        show(ready())

        rule.onNodeWithText("Jan 2, 1990").performClick()
        rule.onNodeWithText("Keep current date").performClick()

        assertEquals(emptyList<Long>(), pickedDates)
        rule.onNodeWithText("Keep current date").assertDoesNotExist()
    }

    @Test
    fun theDateOfBirthRangeRunsFrom1900ToTheCurrentYear() {
        assertEquals(1900..2026, taxDobYearRange(2026))
        assertEquals(1900, taxDobYearRange().first)
    }

    @Test
    fun theDateOfBirthIsShownAsMonthDayYear() {
        assertEquals("Jan 2, 1990", formatTaxDob("1990-01-02"))
        assertNull(formatTaxDob("not a date"))
    }

    /** UI-SPEC R9: the zero-inset Scaffold's snackbar clears the navigation bar and the keyboard. */
    @Test
    @Config(qualifiers = "w360dp-h800dp")
    fun snackbarAboveNavBarAndIme() {
        lateinit var view: android.view.View
        // Frozen clock: the snackbar's 4 s timeout never elapses while the insets are measured.
        rule.mainClock.autoAdvance = false
        rule.setContent {
            view = LocalView.current
            ClickMeTheme { Content(ready().copy(savedEvent = true)) }
        }
        rule.mainClock.advanceTimeBy(300)

        rule.dispatchInsets(view, navigationBarInsets(bottomPx = 48))
        rule.mainClock.advanceTimeBy(300)
        val withNavBar =
            rule
                .onNodeWithText("Tax profile saved.")
                .getUnclippedBoundsInRoot()
                .bottom.value
        assertTrue("snackbar bottom $withNavBar dp must be above the navigation bar (752 dp)", withNavBar <= 752f)

        rule.dispatchInsets(view, imeInsets(bottomPx = 300))
        rule.mainClock.advanceTimeBy(300)
        val withIme =
            rule
                .onNodeWithText("Tax profile saved.")
                .getUnclippedBoundsInRoot()
                .bottom.value
        assertTrue("snackbar bottom $withIme dp must be above the keyboard (500 dp)", withIme <= 500f)
    }

    @Test
    @Config(qualifiers = "w900dp-h600dp")
    fun capped() {
        show(ready())

        val bounds = rule.onAllNodes(hasSetTextAction())[0].getUnclippedBoundsInRoot()
        val width = bounds.right.value - bounds.left.value
        assertTrue("field is $width dp wide, expected <= 600", width <= 600f)
    }
}
