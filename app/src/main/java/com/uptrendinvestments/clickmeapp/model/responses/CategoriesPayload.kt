package com.uptrendinvestments.clickmeapp.model.responses

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** `GET /categories` payload (public endpoint on the backend; the bearer is ignored there). */
@JsonClass(generateAdapter = true)
data class CategoriesPayload(
    val categories: List<CategoryItem>,
)

/** One category. Also the element type the category cache persists, so it must round-trip through Moshi. */
@JsonClass(generateAdapter = true)
data class CategoryItem(
    val id: String,
    val name: String,
    @Json(name = "icon_name") val iconName: String? = null,
    @Json(name = "color_accent") val colorAccent: String? = null,
    val description: String? = null,
)
