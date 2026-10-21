package com.uptrendinvestments.clickmeapp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.uptrendinvestments.clickmeapp.screens.signup.SignupSetupHostViewModel
import com.uptrendinvestments.clickmeapp.screens.signup.basicinfo.SignupBasicInfoScreen
import com.uptrendinvestments.clickmeapp.screens.signup.overview.SignupOverviewScreen
import com.uptrendinvestments.clickmeapp.screens.signup.profilephoto.PhotoCropScreen
import com.uptrendinvestments.clickmeapp.screens.signup.profilephoto.SignupProfilePhotoScreen
import com.uptrendinvestments.clickmeapp.screens.signup.review.SignupReviewScreen
import com.uptrendinvestments.clickmeapp.screens.signup.tags.SignupTagsScreen
import com.uptrendinvestments.clickmeapp.screens.signup.timezone.SignupTimezoneScreen
import com.uptrendinvestments.clickmeapp.screens.signup.verifyemail.SignupVerifyEmailScreen

/**
 * The setup back stack that lives under `AppRoute.SignupSetup`: the Overview hub at the root with the five
 * steps, the photo crop and Review pushed on it. Resume at launch or login always opens the hub (D-10), and
 * the draft hydrates here, at the host, so a deeper entry restored after process death is filled too.
 * Publish success leaves through [onPublished] to AppViewModel (D-22); the hub's "Not now" leaves through [onNotNow]
 * (D-27).
 */
@Composable
fun SignupSetupNavHost(
    onPublished: () -> Unit,
    onNotNow: () -> Unit,
    modifier: Modifier = Modifier,
    hostViewModel: SignupSetupHostViewModel = hiltViewModel(),
) {
    val state = rememberNavigationState(HubKey, setOf(HubKey))
    val navigator = remember(state) { Navigator(state) }
    val setupNavigator = remember(navigator) { SignupSetupNavigator(navigator) }

    LaunchedEffect(Unit) { hostViewModel.ensureHydrated() }

    SignupSetupNavDisplay(
        navigationState = state,
        setupNavigator = setupNavigator,
        modifier = modifier,
        entryProvider =
            entryProvider<NavKey> {
                entry<HubKey> {
                    SignupOverviewScreen(
                        onOpenStep = setupNavigator::openStep,
                        onNext = setupNavigator::openReview,
                        onNotNow = onNotNow,
                    )
                }
                entry<PersonalDetailsKey> {
                    SignupBasicInfoScreen(onSaved = setupNavigator::back, onBack = setupNavigator::back)
                }
                entry<TimezoneKey> {
                    SignupTimezoneScreen(onSaved = setupNavigator::back, onBack = setupNavigator::back)
                }
                entry<ProfilePhotoKey> {
                    SignupProfilePhotoScreen(
                        onOpenCrop = setupNavigator::openCrop,
                        onSaved = setupNavigator::back,
                        onBack = setupNavigator::back,
                    )
                }
                entry<PhotoCropKey> {
                    PhotoCropScreen(onDone = setupNavigator::back, onCancel = setupNavigator::back)
                }
                entry<VerifyEmailKey> {
                    SignupVerifyEmailScreen(onDone = setupNavigator::back, onBack = setupNavigator::back)
                }
                entry<TagsKey> {
                    SignupTagsScreen(onSaved = setupNavigator::back, onBack = setupNavigator::back)
                }
                entry<ReviewKey> {
                    SignupReviewScreen(
                        onEdit = setupNavigator::openStep,
                        onPublished = onPublished,
                        onReturnToHub = setupNavigator::returnToHub,
                        onBack = setupNavigator::back,
                    )
                }
            },
    )
}

/**
 * Hilt-free NavDisplay over the setup stack so tests can drive it with stub entries. At the hub root
 * [SignupSetupNavigator.back] is a no-op and NavDisplay has a single entry, so system Back exits the app.
 */
@Composable
fun SignupSetupNavDisplay(
    navigationState: NavigationState,
    setupNavigator: SignupSetupNavigator,
    entryProvider: (NavKey) -> NavEntry<NavKey>,
    modifier: Modifier = Modifier,
) {
    val entries = navigationState.toDecoratedEntries(entryProvider)
    NavDisplay(
        entries = entries,
        onBack = { setupNavigator.back() },
        modifier = modifier,
        transitionSpec = { slideTransition(forward = true) },
        popTransitionSpec = { slideTransition(forward = false) },
        predictivePopTransitionSpec = { slideTransition(forward = false) },
    )
}
