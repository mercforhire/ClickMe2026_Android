package com.uptrendinvestments.clickmeapp.screens.onboarding

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.uptrendinvestments.clickmeapp.R

/** One carousel page: bundled illustration plus its title and subtitle strings. */
data class OnboardingPage(
    @param:DrawableRes val imageRes: Int,
    @param:StringRes val titleRes: Int,
    @param:StringRes val subtitleRes: Int,
)

/** The four carousel pages in iOS order (port of OnboardingViewModel.pages). */
object OnboardingPages {
    val all: List<OnboardingPage> =
        listOf(
            OnboardingPage(
                imageRes = R.drawable.onboarding_welcome,
                titleRes = R.string.onboarding_welcome_title,
                subtitleRes = R.string.onboarding_welcome_subtitle,
            ),
            OnboardingPage(
                imageRes = R.drawable.onboarding_discover,
                titleRes = R.string.onboarding_discover_title,
                subtitleRes = R.string.onboarding_discover_subtitle,
            ),
            OnboardingPage(
                imageRes = R.drawable.onboarding_booking,
                titleRes = R.string.onboarding_booking_title,
                subtitleRes = R.string.onboarding_booking_subtitle,
            ),
            OnboardingPage(
                imageRes = R.drawable.onboarding_communication,
                titleRes = R.string.onboarding_communication_title,
                subtitleRes = R.string.onboarding_communication_subtitle,
            ),
        )
}
