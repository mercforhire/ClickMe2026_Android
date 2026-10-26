package com.uptrendinvestments.clickmeapp.screens.expertmode.dashboard

private val WHITESPACE = Regex("\\s+")

/**
 * The name the Dashboard hero greets: the first non-blank of [authFirstName] and [profileFirstName], trimmed, then
 * its first whitespace-delimited word ("Ada Marie" greets "Ada"). Null when both are blank, which the hero renders as
 * "Hello, there".
 */
fun dashboardFirstName(
    authFirstName: String?,
    profileFirstName: String?,
): String? =
    listOf(authFirstName, profileFirstName)
        .firstOrNull { !it.isNullOrBlank() }
        ?.trim()
        ?.split(WHITESPACE)
        ?.firstOrNull()
