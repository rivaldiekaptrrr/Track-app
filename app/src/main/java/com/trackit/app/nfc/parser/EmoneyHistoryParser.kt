package com.trackit.app.nfc.parser

import com.trackit.app.nfc.model.NfcCardTransaction
import com.trackit.app.nfc.model.NfcTransactionType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Utility murni untuk membedah dan memformat data riwayat transaksi kartu uang elektronik
 * (BCA Flazz, Mandiri e-Money, BNI TapCash, dan BRI Brizzi).
 */
object EmoneyHistoryParser {

    private val indonesianLocale = Locale("id", "ID")

    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy", indonesianLocale)
        sdf.timeZone = TimeZone.getTimeZone("GMT+7:00")
        return sdf.format(Date(timestamp))
    }

    fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm", indonesianLocale)
        sdf.timeZone = TimeZone.getTimeZone("GMT+7:00")
        return sdf.format(Date(timestamp))
    }

    /**
     * Parsing riwayat BCA Flazz dari hex dump SFI 4 / SFI 5 (blok 120 karakter hex / 60 bytes).
     * Format:
     * - [0..4]   : Tipe proses (0x0400 / 1024 = Payment/Pengeluaran, selain itu Top Up)
     * - [12..18] : Nominal transaksi dalam format hex (3 byte)
     * - [60..76] : Location ID / Merchant ID (16 karakter hex / 8 bytes ASCII)
     * - [76..84] : Timestamp dalam detik sejak 1 Jan 1980 00:00:00 (4 byte hex)
     */
    fun parseFlazzHistory(hexData: String): List<NfcCardTransaction> {
        val list = mutableListOf<NfcCardTransaction>()
        val blockSize = 120
        val count = hexData.length / blockSize
        for (i in 0 until count) {
            val block = hexData.substring(i * blockSize, (i + 1) * blockSize)
            try {
                // 1. Nominal (substring 12..18)
                val amountHex = block.substring(12, 18)
                val amount = amountHex.toLongOrNull(16) ?: 0L
                if (amount <= 0) continue

                // 2. Tipe proses (substring 0..4)
                val typeCode = block.substring(0, 4).toIntOrNull(16) ?: 0
                val type = if (typeCode == 0x0400 || typeCode == 1024) {
                    NfcTransactionType.PAYMENT
                } else {
                    NfcTransactionType.TOPUP
                }

                // 3. Timestamp detik sejak 1980 (substring 76..84)
                val secHex = block.substring(76, 84)
                val secSince1980 = secHex.toLongOrNull(16) ?: 0L
                val cal = Calendar.getInstance(TimeZone.getTimeZone("GMT+7:00"))
                cal.set(1980, Calendar.JANUARY, 1, 0, 0, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val timestamp = cal.timeInMillis + (secSince1980 * 1000L)

                // 4. Location ID (substring 60..76)
                val locHex = block.substring(60, 76)
                val locAscii = hexToAscii(locHex).trim()
                val locationId = if (locAscii.isNotBlank() && locAscii.any { it.isLetterOrDigit() }) {
                    locAscii
                } else {
                    locHex
                }

                list.add(
                    NfcCardTransaction(
                        type = type,
                        amount = amount,
                        timestamp = timestamp,
                        dateFormatted = formatDate(timestamp),
                        timeFormatted = formatTime(timestamp),
                        locationId = locationId
                    )
                )
            } catch (_: Exception) {}
        }
        return list.sortedByDescending { it.timestamp }
    }

    /**
     * Parsing riwayat Mandiri e-Money Chip G2 dari hex log (blok 48 karakter hex / 24 bytes).
     * Format:
     * - [0..12]  : Waktu (ddMMyyHHmmss)
     * - [12..20] : Location ID (8 hex chars)
     * - [28..32] : Tipe proses (100 = Top up, selain itu Payment)
     * - [32..40] : Nominal transaksi (4 byte little-endian)
     * - [40..48] : Saldo akhir (4 byte little-endian)
     */
    fun parseMandiriG2History(hexData: String): List<NfcCardTransaction> {
        val list = mutableListOf<NfcCardTransaction>()
        val blockSize = 48
        val count = hexData.length / blockSize
        for (i in 0 until count) {
            val block = hexData.substring(i * blockSize, (i + 1) * blockSize)
            try {
                val procCode = block.substring(28, 32).toIntOrNull() ?: 0
                val amount = parseLittleEndianLong(block.substring(32, 40))
                val balanceAfter = parseLittleEndianLong(block.substring(40, 48))
                if (procCode == 0 && amount == 0L && balanceAfter == 0L) continue

                // Tanggal (ddMMyyHHmmss)
                val dateStr = block.substring(0, 12)
                val day = dateStr.substring(0, 2).toIntOrNull() ?: 1
                val month = dateStr.substring(2, 4).toIntOrNull() ?: 1
                val year = 2000 + (dateStr.substring(4, 6).toIntOrNull() ?: 0)
                val hour = dateStr.substring(6, 8).toIntOrNull() ?: 0
                val min = dateStr.substring(8, 10).toIntOrNull() ?: 0
                val sec = dateStr.substring(10, 12).toIntOrNull() ?: 0

                val cal = Calendar.getInstance(TimeZone.getTimeZone("GMT+7:00"))
                cal.set(year, month - 1, day, hour, min, sec)
                cal.set(Calendar.MILLISECOND, 0)
                val timestamp = cal.timeInMillis

                val locationId = block.substring(12, 20)
                val type = if (procCode == 100) NfcTransactionType.TOPUP else NfcTransactionType.PAYMENT

                list.add(
                    NfcCardTransaction(
                        type = type,
                        amount = amount,
                        balanceAfter = balanceAfter,
                        timestamp = timestamp,
                        dateFormatted = formatDate(timestamp),
                        timeFormatted = formatTime(timestamp),
                        locationId = locationId
                    )
                )
            } catch (_: Exception) {}
        }
        return list.sortedByDescending { it.timestamp }
    }

    /**
     * Parsing riwayat BNI TapCash dari buffer byte transaksi (16 bytes per transaksi).
     * Format:
     * - [0]    : Tipe (1 = Payment, selain itu Top Up)
     * - [1..3] : Nominal (24-bit big-endian)
     * - [4..7] : Detik timestamp (32-bit big-endian + 788893200) * 1000
     */
    fun parseTapcashHistory(bytes: ByteArray): List<NfcCardTransaction> {
        val list = mutableListOf<NfcCardTransaction>()
        val recordSize = 16
        val count = bytes.size / recordSize
        for (i in 0 until count) {
            val offset = i * recordSize
            val b0 = bytes[offset].toInt() and 0xFF
            val b1 = bytes[offset + 1].toLong() and 0xFFL
            val b2 = bytes[offset + 2].toLong() and 0xFFL
            val b3 = bytes[offset + 3].toLong() and 0xFFL
            val amount = (b1 shl 16) or (b2 shl 8) or b3
            if (amount <= 0) continue

            val type = if (b0 == 1) NfcTransactionType.PAYMENT else NfcTransactionType.TOPUP

            val t0 = bytes[offset + 4].toLong() and 0xFFL
            val t1 = bytes[offset + 5].toLong() and 0xFFL
            val t2 = bytes[offset + 6].toLong() and 0xFFL
            val t3 = bytes[offset + 7].toLong() and 0xFFL
            val rawSec = (t0 shl 24) or (t1 shl 16) or (t2 shl 8) or t3
            val timestamp = (rawSec + 788893200L) * 1000L

            list.add(
                NfcCardTransaction(
                    type = type,
                    amount = amount,
                    timestamp = timestamp,
                    dateFormatted = formatDate(timestamp),
                    timeFormatted = formatTime(timestamp),
                    locationId = ""
                )
            )
        }
        return list.sortedByDescending { it.timestamp }
    }

    /**
     * Parsing riwayat BRI Brizzi dari data hex (64 karakter hex / 32 bytes per transaksi).
     * Format:
     * - [0..8]   : Nominal transaksi (4 byte little-endian)
     * - [32..38] : Tanggal (dd MM yy)
     * - [38..42] : Jam (HH mm)
     * - [44..46] : Tipe proses (1 = Payment, selain itu Top Up)
     */
    fun parseBrizziHistory(hexData: String): List<NfcCardTransaction> {
        val list = mutableListOf<NfcCardTransaction>()
        val blockSize = 64
        val count = hexData.length / blockSize
        for (i in 0 until count) {
            val block = hexData.substring(i * blockSize, (i + 1) * blockSize)
            try {
                val amount = parseLittleEndianLong(block.substring(0, 8))
                if (amount <= 0) continue

                val day = block.substring(32, 34).toIntOrNull() ?: 1
                val month = block.substring(34, 36).toIntOrNull() ?: 1
                val year = 2000 + (block.substring(36, 38).toIntOrNull() ?: 0)
                val hour = block.substring(38, 40).toIntOrNull() ?: 0
                val min = block.substring(40, 42).toIntOrNull() ?: 0

                val cal = Calendar.getInstance(TimeZone.getTimeZone("GMT+7:00"))
                cal.set(year, month - 1, day, hour, min, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val timestamp = cal.timeInMillis

                val procCode = block.substring(44, 46).toIntOrNull(16) ?: 0
                val type = if (procCode == 1 || procCode == 0x01) NfcTransactionType.PAYMENT else NfcTransactionType.TOPUP

                list.add(
                    NfcCardTransaction(
                        type = type,
                        amount = amount,
                        timestamp = timestamp,
                        dateFormatted = formatDate(timestamp),
                        timeFormatted = formatTime(timestamp),
                        locationId = ""
                    )
                )
            } catch (_: Exception) {}
        }
        return list.sortedByDescending { it.timestamp }
    }

    private fun parseLittleEndianLong(hex: String): Long {
        if (hex.length < 8) return 0L
        var res = 0L
        for (i in 0 until 4) {
            val byteVal = hex.substring(i * 2, (i + 1) * 2).toLongOrNull(16) ?: 0L
            res = res or (byteVal shl (i * 8))
        }
        return res
    }

    private fun hexToAscii(hex: String): String {
        val sb = StringBuilder()
        for (i in 0 until hex.length step 2) {
            if (i + 2 <= hex.length) {
                val byteVal = hex.substring(i, i + 2).toIntOrNull(16) ?: continue
                if (byteVal in 32..126) {
                    sb.append(byteVal.toChar())
                }
            }
        }
        return sb.toString()
    }
}
