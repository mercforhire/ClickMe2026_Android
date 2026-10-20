package com.uptrendinvestments.clickmeapp.model.enums

/**
 * Account role, mirroring iOS `UserRole`.
 *
 * DTOs keep roles as raw strings because Moshi's enum adapter needs reflection on enum
 * fields, which R8 can rename; unknown roles then never fail decoding. Map through
 * [fromWire] instead.
 */
enum class UserRole(
    val wire: String,
) {
    Client("client"),
    Expert("expert"),
    ;

    companion object {
        private val byWire: Map<String, UserRole> = entries.associateBy { it.wire }

        fun fromWire(raw: String?): UserRole? = raw?.let { byWire[it] }
    }
}
