package com.example.intellibrief

// data class for gdelt events
data class GdeltArticle(
    val title: String,
    val url: String,
    val socialImage: String?,
    val seenDate: String,
    val domain: String,
    val sourceCountry: String?
)
