package com.trackit.app.util

import java.text.DecimalFormat

object NumberUtils {
    private val indonesianFormat = DecimalFormat("#,###")

    fun formatWithThousandSeparators(amount: String): String {
        val numericValue = amount.filter { it.isDigit() }
        if (numericValue.isEmpty()) return ""
        return try {
            indonesianFormat.format(numericValue.toLong())
        } catch (e: NumberFormatException) {
            numericValue
        }
    }

    fun parseToLong(formattedAmount: String): Long {
        return formattedAmount.filter { it.isDigit() }.toLongOrNull() ?: 0L
    }
}
