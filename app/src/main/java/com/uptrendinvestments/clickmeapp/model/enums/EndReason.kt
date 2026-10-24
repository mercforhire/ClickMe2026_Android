package com.uptrendinvestments.clickmeapp.model.enums

/** Why a call session ended, sent with `POST /end`. Mirrors iOS `Model/Enums/EndReason.swift`. */
enum class EndReason(
    val wire: String,
) {
    Completed("completed"),
    Dropped("dropped"),
    ManualExit("manual_exit"),
    ;

    companion object {
        private val byWire: Map<String, EndReason> = entries.associateBy { it.wire }

        /** Returns the matching case, or null for an unknown or missing wire value. */
        fun fromWire(raw: String?): EndReason? = raw?.let(byWire::get)
    }
}
