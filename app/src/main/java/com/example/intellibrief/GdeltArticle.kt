package com.example.intellibrief

import java.io.Serializable

// Data class representing a news article fetched from the GDELT Project.

data class GdeltArticle(
    val title: String = "",
    val url: String = "",
    val socialImage: String? = null,
    val seenDate: String = "",
    val domain: String = "",
    val lat: Double? = null,
    val lon: Double? = null
) : Serializable
