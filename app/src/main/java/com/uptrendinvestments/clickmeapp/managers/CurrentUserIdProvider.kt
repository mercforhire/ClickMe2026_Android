package com.uptrendinvestments.clickmeapp.managers

/**
 * The signed-in user's id, for "mine" message classification and self-target checks. Compare it ignoring case
 * because the backend compares UUIDs case-insensitively. Null when nobody is signed in.
 */
fun interface CurrentUserIdProvider {
    fun currentUserId(): String?
}
