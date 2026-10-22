package com.uptrendinvestments.clickmeapp.constants

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.DesignServices
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Engineering
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.SportsBasketball
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.Work
import androidx.compose.ui.graphics.vector.ImageVector
import java.util.Locale

/**
 * Port of iOS `CategoryIconMap` (Constants/CategoryIconMap.swift): `GET /categories` slug -> Material icon.
 *
 * The server's `icon_name` (Lucide names) is deliberately not used (iOS parity). Unknown slugs get [fallback].
 * Call sites tint the icon with the primary color.
 */
object CategoryIconMap {
    /** Generic taxonomy tile for slugs the client does not know yet. */
    val fallback: ImageVector = Icons.Outlined.GridView

    private val table: Map<String, ImageVector> =
        mapOf(
            "ai" to Icons.Outlined.Memory,
            "business" to Icons.Outlined.Work,
            "consulting" to Icons.Outlined.Lightbulb,
            "career" to Icons.Outlined.PersonAdd,
            "coaching" to Icons.Outlined.RecordVoiceOver,
            "design" to Icons.Outlined.DesignServices,
            "education" to Icons.Outlined.School,
            "engineering" to Icons.Outlined.Engineering,
            "finance" to Icons.Outlined.ShowChart,
            "fitness" to Icons.Outlined.FitnessCenter,
            "food" to Icons.Outlined.Restaurant,
            "gaming" to Icons.Outlined.SportsEsports,
            "healthcare" to Icons.Outlined.MedicalServices,
            "health" to Icons.Outlined.MedicalServices,
            "language" to Icons.Outlined.Translate,
            "languages" to Icons.Outlined.Translate,
            "legal" to Icons.Outlined.Gavel,
            "marketing" to Icons.Outlined.Campaign,
            "music" to Icons.Outlined.MusicNote,
            "photography" to Icons.Outlined.CameraAlt,
            "productivity" to Icons.Outlined.Speed,
            "psychology" to Icons.Outlined.Psychology,
            "real-estate" to Icons.Outlined.Home,
            "science" to Icons.Outlined.Science,
            "sports" to Icons.Outlined.SportsBasketball,
            "startup" to Icons.Outlined.RocketLaunch,
            "tech" to Icons.Outlined.Code,
            "technology" to Icons.Outlined.Code,
            "travel" to Icons.Outlined.Flight,
            "wellness" to Icons.Outlined.Spa,
            "writing" to Icons.Outlined.Edit,
        )

    /** The 31 slugs ported from iOS (lower-cased). */
    val slugs: Set<String> get() = table.keys

    fun icon(slug: String): ImageVector = table[slug.lowercase(Locale.ROOT)] ?: fallback
}
