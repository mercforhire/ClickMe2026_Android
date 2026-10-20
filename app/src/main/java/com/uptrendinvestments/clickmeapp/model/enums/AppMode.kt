package com.uptrendinvestments.clickmeapp.model.enums

/**
 * Which side of the app the user is currently in, mirroring iOS `AppMode`
 * ("client" / "expert"). The wire value is what gets persisted in DataStore.
 *
 * Looked up through [fromWire] rather than a reflection-based enum adapter (R8-safe).
 */
enum class AppMode(
    val wire: String,
) {
    Client("client"),
    Expert("expert"),
    ;

    companion object {
        private val byWire: Map<String, AppMode> = entries.associateBy { it.wire }

        fun fromWire(raw: String?): AppMode? = raw?.let { byWire[it] }
    }
}
