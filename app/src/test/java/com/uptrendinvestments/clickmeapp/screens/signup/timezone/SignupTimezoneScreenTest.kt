package com.uptrendinvestments.clickmeapp.screens.signup.timezone

import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasContentDescriptionExactly
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.uptrendinvestments.clickmeapp.components.PickerSheetContent
import com.uptrendinvestments.clickmeapp.components.buttons.SaveState
import com.uptrendinvestments.clickmeapp.constants.Timezones
import com.uptrendinvestments.clickmeapp.screens.login.FieldError
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SignupTimezoneScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private var openCount = 0
    private var autoDetectCount = 0
    private var saveCount = 0
    private var backCount = 0
    private var dismissDialogCount = 0
    private val selected = mutableListOf<String>()

    private val baseState =
        SignupTimezoneUiState(selectedId = "America/New_York", entries = Timezones.all)

    private fun show(state: SignupTimezoneUiState = baseState) {
        rule.setContent {
            ClickMeTheme {
                SignupTimezoneContent(
                    state = state,
                    onOpenSheet = { openCount++ },
                    onDismissSheet = {},
                    onSelect = { selected += it },
                    onAutoDetect = { autoDetectCount++ },
                    onSave = { saveCount++ },
                    onBack = { backCount++ },
                    onDismissDialog = { dismissDialogCount++ },
                )
            }
        }
    }

    @Test
    fun rendersCopyAndSelectedLabel() {
        show()

        rule.onNodeWithText("Timezone").assertIsDisplayed()
        rule.onNodeWithText("Confirm your timezone").assertIsDisplayed()
        rule.onNodeWithText("We auto-detected this from your device.\nChange it if it's wrong.").assertIsDisplayed()
        rule.onNodeWithText("(GMT-05:00) Eastern Time").assertIsDisplayed()
        rule.onNodeWithText("Auto-detect from device").assertIsDisplayed()
        rule
            .onNodeWithText(
                "We use this to convert booking times to your local time and to align payouts with your day.",
            ).assertIsDisplayed()
        rule.onNodeWithText("Save").assertIsDisplayed()
    }

    @Test
    fun tapsCallTheirHandlers() {
        show()

        rule.onNodeWithText("(GMT-05:00) Eastern Time").performClick()
        rule.onNodeWithText("Auto-detect from device").performClick()
        rule.onNodeWithText("Save").performClick()
        rule.onNodeWithContentDescription("Back").performClick()

        assertEquals(1, openCount)
        assertEquals(1, autoDetectCount)
        assertEquals(1, saveCount)
        assertEquals(1, backCount)
    }

    @Test
    fun sheetListsEntriesWithoutSearchAndSelectsTheTappedRow() {
        rule.setContent {
            ClickMeTheme {
                TimezoneSheetContent(
                    entries = Timezones.all,
                    selectedId = "America/New_York",
                    onSelect = { selected += it },
                )
            }
        }

        rule.onNodeWithText("Select Timezone").assertIsDisplayed()
        rule.onNodeWithText("(GMT-05:00) Eastern Time").assertIsDisplayed()
        rule.onNode(hasText("(GMT-05:00) Eastern Time")).assertIsSelected()
        rule.onAllNodesWithText("Search", substring = true).assertCountEquals(0)

        rule.onNode(hasScrollAction()).performScrollToNode(hasText("(GMT+01:00) Paris"))
        rule.onNodeWithText("(GMT+01:00) Paris").performClick()

        assertEquals(listOf("Europe/Paris"), selected)
    }

    @Test
    fun sheetContentCanBeComposedInsideThePickerBody() {
        rule.setContent {
            ClickMeTheme {
                PickerSheetContent(title = "Select Timezone") {
                    timezoneSheetRows(Timezones.all, "UTC") { selected += it }
                }
            }
        }

        rule.onNode(hasText("(GMT+00:00) UTC")).assertIsSelected()
    }

    @Test
    fun errorsShowInlineAndInTheDialog() {
        show(
            baseState.copy(
                timezoneError = FieldError.Text("Invalid timezone"),
                dialogError = FieldError.Text("Server exploded"),
            ),
        )

        rule.onNodeWithText("Invalid timezone").assertIsDisplayed()
        rule.onNodeWithText("Couldn't save").assertIsDisplayed()
        rule.onNodeWithText("Server exploded").assertIsDisplayed()

        rule.onNodeWithText("OK").performClick()
        assertEquals(1, dismissDialogCount)
    }

    /** Renders the step under an outer Back handler, like NavDisplay's, and returns how often it fired (WR-06). */
    private fun systemBackReachesNavigation(saveState: SaveState): Int {
        var navBacks = 0
        var dispatcher: OnBackPressedDispatcher? = null
        rule.setContent {
            ClickMeTheme {
                dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
                BackHandler { navBacks++ }
                SignupTimezoneContent(
                    state = baseState.copy(saveState = saveState),
                    onOpenSheet = {},
                    onDismissSheet = {},
                    onSelect = {},
                    onAutoDetect = {},
                    onSave = {},
                    onBack = { backCount++ },
                    onDismissDialog = {},
                )
            }
        }
        rule.runOnIdle { checkNotNull(dispatcher).onBackPressed() }
        rule.waitForIdle()
        return navBacks
    }

    @Test
    fun systemBackWhileIdleReachesNavigationAndTheArrowIsEnabled() {
        assertEquals(1, systemBackReachesNavigation(SaveState.Idle))
        rule.onNode(hasContentDescriptionExactly("Back")).assertIsEnabled()
    }

    @Test
    fun backIsBlockedWhileSaving() {
        assertEquals(0, systemBackReachesNavigation(SaveState.Saving))
        rule.onNode(hasContentDescriptionExactly("Back")).assertIsNotEnabled().performClick()
        assertEquals(0, backCount)
    }

    @Test
    fun backIsBlockedDuringTheSavedHold() {
        assertEquals(0, systemBackReachesNavigation(SaveState.Saved))
    }
}
