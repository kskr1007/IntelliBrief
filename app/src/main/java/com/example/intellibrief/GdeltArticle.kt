package com.example.intellibrief

import java.io.Serializable

/**
 * Data class representing a news article fetched from the GDELT Project.
 * Implements Serializable to allow passing between Activities via Intents.
 */
data class GdeltArticle(
    val title: String,
    val url: String,
    val socialImage: String?,
    val seenDate: String,
    val domain: String
) : Serializable
