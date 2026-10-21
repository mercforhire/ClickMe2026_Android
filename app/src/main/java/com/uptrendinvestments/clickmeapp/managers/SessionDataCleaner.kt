package com.uptrendinvestments.clickmeapp.managers

/**
 * Device-local data that belongs to one session, such as the signup photo working files (T-04-18).
 *
 * [UserManager] runs every bound cleaner when a session ends (logout) and before a new one starts (login, signup).
 * The cleanup is tied to the session itself rather than to an observer inside a lazily created feature singleton,
 * which would not exist in a process that never opened that feature. Bound into a set with Hilt `@IntoSet`.
 */
fun interface SessionDataCleaner {
    /** Deletes this session's local data. May throw; [UserManager] treats a failure as best effort. */
    suspend fun clearSessionData()
}
