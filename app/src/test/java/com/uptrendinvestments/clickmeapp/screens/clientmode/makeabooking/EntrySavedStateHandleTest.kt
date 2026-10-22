package com.uptrendinvestments.clickmeapp.screens.clientmode.makeabooking

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.uptrendinvestments.clickmeapp.screens.clientmode.home.HomeClientShell
import com.uptrendinvestments.clickmeapp.ui.theme.ClickMeTheme
import kotlinx.serialization.Serializable
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@Serializable
private data object ProbeKey : NavKey

private class ProbeViewModel(
    val handle: SavedStateHandle,
) : ViewModel()

/**
 * Research Open Question 3: a Nav3 entry's ViewModel can be created with a SavedStateHandle, which is what
 * `MakeABookingViewModel` needs for process-death restore. Uses the real client shell and its entry decorators.
 */
@RunWith(RobolectricTestRunner::class)
class EntrySavedStateHandleTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun navEntryCanCreateASavedStateHandle() {
        composeRule.setContent {
            ClickMeTheme {
                HomeClientShell(
                    avatarUrl = null,
                    showDebugAffordances = false,
                    onSwitchMode = {},
                    onLogout = {},
                    onDebugForceRefresh = {},
                    debugRefreshStatus = null,
                    exploreRoot = { navigator, _ ->
                        Button(onClick = { navigator.navigate(ProbeKey) }) { Text("open probe") }
                    },
                    detailEntries = {
                        entry<ProbeKey> {
                            val vm = viewModel { ProbeViewModel(createSavedStateHandle()) }
                            vm.handle["probe"] = "x"
                            Text("probe ready ${vm.handle.get<String>("probe")}")
                        }
                    },
                )
            }
        }
        composeRule.onNodeWithText("open probe").performClick()
        composeRule.onNodeWithText("probe ready x").assertExists()
    }
}
