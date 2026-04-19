package com.trackit.app.util

import com.trackit.app.data.local.entity.CategoryEntity
import java.util.Calendar

/**
 * Result from parsing voice input text.
 */
data class VoiceParseResult(
    val amount: Long?,           // Nominal in rupiah (e.g., 50000)
    val categoryName: String?,   // Matched category name (e.g., "Makanan")
    val description: String,     // Raw text / description
    val dateMillis: Long?,       // Parsed date (null = today)
    val isValid: Boolean         // Whether at least the amount was extracted
)

/**
 * Regex-based parser for extracting amount, category, and description
 * from Indonesian natural language voice input.
 *
 * Supports:
 * - Numeric amounts: "50 ribu", "1,5 juta", "2 juta 500 ribu"
 * - Word amounts: "lima puluh ribu", "seratus ribu", "dua ratus lima puluh ribu"
 * - Slang: "gocap", "cepek", "seceng", "gopek", "noban", "goban", "selawe"
 * - Category matching via keyword dictionary + custom keywords
 * - Date detection: "kemarin" (yesterday), "kemarin lusa" (2 days ago)
 */
object VoiceParser {

    // Default keyword mapping per category (matching DB names exactly)
    private val defaultKeywords: Map<String, List<String>> = mapOf(
        "Makanan" to listOf(
            "makan", "sayur", "buah", "nasi", "bakso", "mie", "kopi", "minum",
            "jajan", "snack", "gorengan", "soto", "sate", "ayam", "ikan", "tahu",
            "tempe", "roti", "susu", "es", "warung", "resto", "restoran", "cafe", "kantin"
        ),
        "Transportasi" to listOf(
            "bensin", "parkir", "tol", "gojek", "grab", "ojek", "ojol", "bus",
            "kereta", "angkot", "taksi", "taxi", "bbm", "solar", "pertalite"
        ),
        "Hiburan" to listOf(
            "nonton", "bioskop", "netflix", "game", "main", "spotify", "youtube",
            "konser", "wisata", "liburan", "rekreasi", "karaoke"
        ),
        "Tagihan" to listOf(
            "listrik", "air", "wifi", "internet", "pulsa", "kuota", "token", "pdam",
            "gas", "iuran", "sewa", "kos", "kontrakan", "cicilan", "kredit", "pajak", "asuransi"
        ),
        "Belanja" to listOf(
            "belanja", "baju", "celana", "sepatu", "tas", "online", "shopee",
            "tokopedia", "lazada", "beli", "fashion", "pakaian", "kosmetik", "skincare"
        ),
        "Kesehatan" to listOf(
            "obat", "dokter", "rumah sakit", "apotek", "farmasi", "vitamin",
            "klinik", "medis", "cek", "lab", "operasi"
        ),
        "Pendidikan" to listOf(
            "buku", "sekolah", "kuliah", "kursus", "les", "spp", "ukt",
            "semester", "tuition", "seminar"
        )
    )

    // Slang amounts recognized
    private val slangAmounts: Map<String, Long> = mapOf(
        "gocap" to 50_000L,
        "gopek" to 500L,
        "cepek" to 100_000L,
        "seceng" to 1_000L,
        "ceng" to 1_000L,
        "noban" to 20_000L,
        "goban" to 50_000L,
        "selawe" to 25_000L
    )

    // Number words mapping
    private val numberWords: Map<String, Int> = mapOf(
        "nol" to 0,
        "satu" to 1, "dua" to 2, "tiga" to 3, "empat" to 4,
        "lima" to 5, "enam" to 6, "tujuh" to 7, "delapan" to 8,
        "sembilan" to 9, "sepuluh" to 10, "sebelas" to 11
    )

    /**
     * Main parse function.
     * @param text Raw speech recognition result
     * @param categories List of all categories from DB (with customKeywords field)
     * @return VoiceParseResult with extracted data
     */
    fun parse(text: String, categories: List<CategoryEntity> = emptyList()): VoiceParseResult {
        val normalizedText = text.lowercase().trim()

        // Extract amount (try multiple strategies)
        val amount = extractAmount(normalizedText)

        // Detect category
        val categoryName = detectCategory(normalizedText, categories)

        // Detect date
        val dateMillis = detectDate(normalizedText)

        return VoiceParseResult(
            amount = amount,
            categoryName = categoryName,
            description = text.trim(),
            dateMillis = dateMillis,
            isValid = amount != null && amount > 0
        )
    }

    /**
     * Extract amount from Rupiah-formatted text produced by Google STT.
     * Handles: "Rp.50.000", "Rp50.000", "Rp 50.000", "Rp1.500.000",
     *          "Rp. 50.000", "Rp.50,000", "Rp50000", etc.
     */
    private fun extractRupiahFormat(text: String): Long? {
        // Pattern: "rp" optionally followed by "." and/or spaces, then digits with dot/comma thousand separators
        val rupiahPattern = Regex("""rp\.?\s*(\d{1,3}(?:[.,]\d{3})*)""")
        val match = rupiahPattern.find(text) ?: return null
        val numberStr = match.groupValues[1].replace(".", "").replace(",", "")
        return numberStr.toLongOrNull()
    }

    /**
     * Extract monetary amount from text using multiple strategies.
     * Strategy priority: Slang → Numeric with multiplier → Compound (X juta Y ribu) → Word numbers → Plain number
     */
    private fun extractAmount(text: String): Long? {
        // Strategy 0: Rupiah format from STT (e.g., "Rp.50.000", "Rp50.000")
        val rupiahAmount = extractRupiahFormat(text)

        // Strategy 1: Check for slang terms ("gocap", "cepek", etc.)
        val slangAmount = extractSlangAmount(text)

        // Strategy 2: Compound pattern "X juta Y ribu" or "X juta Y ratus ribu"
        val compoundAmount = extractCompoundAmount(text)

        // Strategy 3: Numeric with multiplier: "50 ribu", "1,5 juta", "50ribu"
        val numericAmount = extractNumericAmount(text)

        // Strategy 4: Word numbers: "lima puluh ribu", "seratus ribu"
        val wordAmount = extractWordAmount(text)

        // Strategy 5: Plain number (just digits)
        val plainAmount = extractPlainNumber(text)

        // Priority: rupiah format > slang > compound > numeric > word > plain
        return rupiahAmount ?: slangAmount ?: compoundAmount ?: numericAmount ?: wordAmount ?: plainAmount
    }

    private fun extractSlangAmount(text: String): Long? {
        // Check slang terms - last match wins (override logic)
        var lastMatch: Long? = null
        for ((slang, value) in slangAmounts) {
            if (text.contains(slang)) {
                lastMatch = value
            }
        }
        return lastMatch
    }

    private fun extractCompoundAmount(text: String): Long? {
        // Pattern: "X juta Y ribu" or "X juta Y ratus ribu"
        val compoundPattern = Regex("""(\d+[.,]?\d*)\s*(?:juta|jt)\s+(\d+[.,]?\d*)\s*(?:ribu|rb|rebu)""")
        val match = compoundPattern.find(text)
        if (match != null) {
            val jutaPart = parseDecimal(match.groupValues[1])
            val ribuPart = parseDecimal(match.groupValues[2])
            return (jutaPart * 1_000_000 + ribuPart * 1_000).toLong()
        }
        return null
    }

    private fun extractNumericAmount(text: String): Long? {
        // Pattern: number (with optional decimal) followed by multiplier
        val patterns = listOf(
            Regex("""(\d+[.,]?\d*)\s*(?:juta|jt)""") to 1_000_000.0,
            Regex("""(\d+[.,]?\d*)\s*(?:ribu|rb|rebu)""") to 1_000.0,
            Regex("""(\d+[.,]?\d*)\s*(?:ratus)\s*(?:ribu|rb|rebu)""") to 100_000.0
        )

        // Also handle "setengah juta" = 500,000
        if (text.contains("setengah juta") || text.contains("setengah jt")) {
            return 500_000L
        }

        // Check each pattern - use the LAST valid match for override logic
        var lastResult: Long? = null
        for ((pattern, multiplier) in patterns) {
            val allMatches = pattern.findAll(text)
            for (m in allMatches) {
                val number = parseDecimal(m.groupValues[1])
                lastResult = (number * multiplier).toLong()
            }
        }

        return lastResult
    }

    private fun extractWordAmount(text: String): Long? {
        // Handle "satu setengah juta" = 1,500,000
        val setengahJutaPattern = Regex("""(se|satu|dua|tiga|empat|lima|enam|tujuh|delapan|sembilan)\s+setengah\s+(?:juta|jt)""")
        val setengahMatch = setengahJutaPattern.find(text)
        if (setengahMatch != null) {
            val baseNum = wordToNumber(setengahMatch.groupValues[1])
            return ((baseNum + 0.5) * 1_000_000).toLong()
        }

        // Handle complex word numbers with multiplier
        val result = parseWordNumberWithMultiplier(text)
        if (result != null && result > 0) return result

        // Handle "seribu" = 1000, "seratus" = 100
        if (text.contains("seribu")) return 1_000L
        if (text.contains("seratus ribu") || text.contains("seratus rb")) return 100_000L
        if (text.contains("seratus")) return 100L

        return null
    }

    /**
     * Parse word numbers like "lima puluh ribu", "dua ratus lima puluh ribu", etc.
     */
    private fun parseWordNumberWithMultiplier(text: String): Long? {
        // Build number from word tokens
        val tokens = text.split("\\s+".toRegex())

        var totalAmount = 0L
        var currentNumber = 0L
        var hasNumber = false
        var i = 0

        while (i < tokens.size) {
            val token = tokens[i]

            when {
                // Direct number word
                numberWords.containsKey(token) -> {
                    currentNumber += numberWords[token]!!
                    hasNumber = true
                }
                // "se" prefix handling (se = 1)
                token == "se" -> {
                    currentNumber = 1
                    hasNumber = true
                }
                // "belas" suffix: X belas = X + 10 (e.g., "dua belas" = 12)
                token == "belas" && hasNumber -> {
                    currentNumber += 10
                }
                // "puluh": X puluh = X * 10
                token == "puluh" && hasNumber -> {
                    currentNumber *= 10
                }
                // "ratus": X ratus = X * 100
                token == "ratus" && hasNumber -> {
                    currentNumber *= 100
                }
                // Multipliers
                token in listOf("ribu", "rb", "rebu") -> {
                    if (hasNumber) {
                        totalAmount += currentNumber * 1_000
                        currentNumber = 0
                    }
                }
                token in listOf("juta", "jt") -> {
                    if (hasNumber) {
                        totalAmount += currentNumber * 1_000_000
                        currentNumber = 0
                    }
                }
                else -> {
                    // If we had accumulated a number and hit a non-number token, keep it
                    if (hasNumber && currentNumber > 0) {
                        // Don't reset if next relevant tokens might follow
                    }
                }
            }
            i++
        }

        // Add any remaining number
        totalAmount += currentNumber

        return if (totalAmount > 0) totalAmount else null
    }

    private fun extractPlainNumber(text: String): Long? {
        // Find standalone numbers (not followed by multiplier keywords)
        val plainPattern = Regex("""(?<!\d)(\d{3,})(?!\d)""")
        val matches = plainPattern.findAll(text).toList()
        // Take the last match (override logic)
        return matches.lastOrNull()?.groupValues?.get(1)?.toLongOrNull()
    }

    private fun parseDecimal(value: String): Double {
        return value.replace(",", ".").toDoubleOrNull() ?: 0.0
    }

    private fun wordToNumber(word: String): Int {
        return when (word) {
            "se", "satu" -> 1
            "dua" -> 2
            "tiga" -> 3
            "empat" -> 4
            "lima" -> 5
            "enam" -> 6
            "tujuh" -> 7
            "delapan" -> 8
            "sembilan" -> 9
            else -> 0
        }
    }

    /**
     * Detect category from text using keyword matching.
     * Custom keywords from user take priority over default keywords.
     * If multiple categories match, the one whose keyword appears earliest wins.
     */
    private fun detectCategory(text: String, categories: List<CategoryEntity>): String? {
        data class Match(val categoryName: String, val position: Int, val isCustom: Boolean)

        val matches = mutableListOf<Match>()

        // Build merged keyword map: category name -> (keyword, isCustom)
        for (category in categories) {
            val categoryName = category.name

            // Check custom keywords first (higher priority)
            val customKeywords = category.customKeywords
                .split(",")
                .map { it.trim().lowercase() }
                .filter { it.isNotEmpty() }

            for (keyword in customKeywords) {
                val pos = text.indexOf(keyword)
                if (pos >= 0) {
                    matches.add(Match(categoryName, pos, isCustom = true))
                }
            }

            // Check default keywords
            val defaults = defaultKeywords[categoryName] ?: emptyList()
            for (keyword in defaults) {
                val pos = text.indexOf(keyword)
                if (pos >= 0) {
                    matches.add(Match(categoryName, pos, isCustom = false))
                }
            }
        }

        if (matches.isEmpty()) return null

        // Priority: custom keywords first, then by earliest position
        val bestMatch = matches
            .sortedWith(compareByDescending<Match> { it.isCustom }.thenBy { it.position })
            .firstOrNull()

        return bestMatch?.categoryName
    }

    /**
     * Detect date keywords in text.
     * - "kemarin lusa" → H-2
     * - "kemarin" → H-1
     * - No keyword → null (means today, caller decides)
     */
    private fun detectDate(text: String): Long? {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        return when {
            text.contains("kemarin lusa") || text.contains("kemaren lusa") -> {
                cal.add(Calendar.DAY_OF_YEAR, -2)
                cal.timeInMillis
            }
            text.contains("kemarin") || text.contains("kemaren") -> {
                cal.add(Calendar.DAY_OF_YEAR, -1)
                cal.timeInMillis
            }
            else -> null
        }
    }
}
