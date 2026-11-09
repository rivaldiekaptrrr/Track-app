package com.trackit.app.nfc.categorization

import com.trackit.app.data.local.entity.CategoryEntity
import java.util.Locale

object NfcKeywordMatcher {

    private val SMART_KEYWORDS_MAP = listOf(
        "stasiun" to "Stasiun",
        "tol" to "Tol",
        "halte" to "Halte",
        "terminal" to "Terminal",
        "bandara" to "Bandara",
        "airport" to "Bandara",
        "mrt" to "MRT",
        "lrt" to "LRT",
        "krl" to "KRL",
        "transjakarta" to "Transjakarta"
    )

    /**
     * Extracts a smart keyword from a location string (e.g. "Stasiun KRL Tangerang" -> "Stasiun",
     * "Gerbang Tol Pasteur" -> "Tol").
     * If no predefined transit keyword is found, falls back to the capitalized first word.
     */
    fun extractSmartKeyword(locationName: String): String {
        val trimmed = locationName.trim()
        if (trimmed.isEmpty()) return ""

        val lower = trimmed.lowercase(Locale.ROOT)

        // 1. Check known smart transit keywords via word boundary
        for ((pattern, canonical) in SMART_KEYWORDS_MAP) {
            val regex = Regex("\\b${Regex.escape(pattern)}\\b", RegexOption.IGNORE_CASE)
            if (regex.containsMatchIn(lower)) {
                return canonical
            }
        }

        // 2. Fallback to first non-empty word
        val words = trimmed.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.isNotEmpty()) {
            val firstWord = words[0].replace(Regex("[^a-zA-Z0-9-]"), "")
            if (firstWord.isNotEmpty()) {
                return firstWord.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
                }
            }
        }

        return ""
    }

    /**
     * Attempts to find a matching category based on custom keywords or category name.
     */
    fun matchCategory(locationName: String, categories: List<CategoryEntity>): CategoryEntity? {
        val lowerLocation = locationName.trim().lowercase(Locale.ROOT)
        if (lowerLocation.isEmpty()) return null

        // 1. Prioritize user custom keywords
        for (category in categories) {
            if (category.customKeywords.isNotBlank()) {
                val keywords = category.customKeywords
                    .split(",")
                    .map { it.trim().lowercase(Locale.ROOT) }
                    .filter { it.isNotEmpty() }

                for (kw in keywords) {
                    val regex = Regex("\\b${Regex.escape(kw)}\\b", RegexOption.IGNORE_CASE)
                    if (regex.containsMatchIn(lowerLocation)) {
                        return category
                    }
                }
            }
        }

        // 2. Check category name itself
        for (category in categories) {
            val catName = category.name.trim().lowercase(Locale.ROOT)
            if (catName.isNotEmpty()) {
                val regex = Regex("\\b${Regex.escape(catName)}\\b", RegexOption.IGNORE_CASE)
                if (regex.containsMatchIn(lowerLocation)) {
                    return category
                }
            }
        }

        return null
    }

    /**
     * Appends a new keyword to the category's comma-separated customKeywords field.
     * Pure and immutable; returns a new CategoryEntity instance.
     * Prevents case-insensitive duplicates.
     */
    fun appendKeywordToCategory(category: CategoryEntity, newKeyword: String): CategoryEntity {
        val trimmed = newKeyword.trim()
        if (trimmed.isEmpty()) return category

        val existing = if (category.customKeywords.isBlank()) {
            emptyList()
        } else {
            category.customKeywords
                .split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
        }

        if (existing.any { it.equals(trimmed, ignoreCase = true) }) {
            return category
        }

        val updatedKeywords = (existing + trimmed).joinToString(", ")
        return category.copy(customKeywords = updatedKeywords)
    }
}

