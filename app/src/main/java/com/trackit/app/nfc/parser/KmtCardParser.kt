package com.trackit.app.nfc.parser

import android.nfc.Tag
import android.nfc.tech.NfcF
import android.util.Log
import com.trackit.app.nfc.model.EmoneyCardResult
import com.trackit.app.nfc.model.EmoneyCardType
import com.trackit.app.nfc.model.NfcCardTransaction
import com.trackit.app.nfc.model.NfcTransactionType

/**
 * Parser untuk Kartu Multi Trip (KMT) KAI Commuter berbasis Sony FeliCa (NFC-F).
 *
 * Struktur Layanan FeliCa KMT:
 * - Service Code: 0x90B7 (37047)
 * - Block 0: Nomor Kartu (16 byte ASCII string)
 * - Block 1: Saldo Kartu (4 byte integer Little-Endian pada byte [0..3])
 * - Block 2..N: Riwayat Transaksi (16 byte per blok):
 *     - byte [12]: Tipe transaksi (0 = Top Up, 1 = Payment / Gate Tap)
 *     - byte [4..7]: Nominal transaksi (4 byte integer Big-Endian)
 *     - byte [0..3]: Detik sejak 1 Januari 2000 07:00:00 WIB (4 byte integer Big-Endian)
 *     - byte [8..9]: Kode stasiun (2 byte integer Big-Endian)
 */
object KmtCardParser {

    private const val TAG = "KmtCardParser"

    const val KMT_SERVICE_CODE = 0x90B7
    const val KMT_BASE_EPOCH_MS = 946684800000L // 2000-01-01 07:00:00 Asia/Jakarta (GMT+7)

    /**
     * Membaca kartu KMT dari Android Tag (NfcF).
     * Mengembalikan EmoneyCardResult jika tag merupakan kartu KMT yang valid, atau null jika bukan.
     */
    fun parseKmtCard(tag: Tag): EmoneyCardResult? {
        val nfcF = NfcF.get(tag) ?: return null
        return parseKmtCard(nfcF)
    }

    /**
     * Membaca kartu KMT dari instance NfcF yang aktif.
     */
    fun parseKmtCard(nfcF: NfcF): EmoneyCardResult? {
        return try {
            if (!nfcF.isConnected) {
                nfcF.connect()
            }
            nfcF.timeout = 5000

            // 1. Polling FeliCa untuk mendapatkan IDm (8 byte)
            val pollCmd = byteArrayOf(0x06, 0x00, 0xFF.toByte(), 0xFF.toByte(), 0x01, 0x00)
            val pollRes = try {
                nfcF.transceive(pollCmd)
            } catch (e: Exception) {
                Log.d(TAG, "Polling failed, fallback to tag id: ${e.message}")
                null
            }

            val idm = if (pollRes != null && pollRes.size >= 10 && pollRes[1] == 0x01.toByte()) {
                pollRes.copyOfRange(2, 10)
            } else {
                nfcF.tag.id
            }

            if (idm == null || idm.size < 8) {
                Log.w(TAG, "IDm tidak ditemukan atau kurang dari 8 byte")
                return null
            }

            // 2. Baca Blok 0 (Nomor Kartu)
            val cmd0 = buildReadCommand(idm, KMT_SERVICE_CODE, 0)
            val res0 = nfcF.transceive(cmd0) ?: return null
            val block0 = extractBlockData(res0) ?: return null
            val cardNumber = parseCardNumber(block0)
            if (cardNumber.isEmpty()) {
                Log.w(TAG, "Nomor kartu KMT kosong atau tidak valid")
                return null
            }

            // 3. Baca Blok 1 (Saldo)
            val cmd1 = buildReadCommand(idm, KMT_SERVICE_CODE, 1)
            val res1 = nfcF.transceive(cmd1) ?: return null
            val block1 = extractBlockData(res1) ?: return null
            val balance = parseBalance(block1)

            // 4. Baca Riwayat Transaksi (Blok 2 s/d 9)
            var lastTx: NfcCardTransaction? = null
            for (b in 2..9) {
                try {
                    val cmd = buildReadCommand(idm, KMT_SERVICE_CODE, b)
                    val res = nfcF.transceive(cmd) ?: break
                    val block = extractBlockData(res) ?: break
                    val tx = parseHistoryBlock(block)
                    if (tx != null) {
                        if (lastTx == null) {
                            lastTx = tx
                        }
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "Selesai membaca blok riwayat pada indeks $b: ${e.message}")
                    break
                }
            }

            EmoneyCardResult(
                cardType = EmoneyCardType.KAI_KMT,
                cardNumber = cardNumber,
                balance = balance,
                isChipG2 = false,
                lastTransaction = lastTx
            )
        } catch (e: Exception) {
            Log.e(TAG, "Gagal memproses kartu KMT: ${e.message}", e)
            null
        }
    }

    /**
     * Membangun paket perintah FeliCa "Read Without Encryption" (0x06).
     * Format:
     * - [0]       : Panjang paket (16 byte = 0x10)
     * - [1]       : Kode perintah (0x06)
     * - [2..9]    : IDm (8 byte)
     * - [10]      : Jumlah layanan (1 byte = 0x01)
     * - [11..12]  : Service Code little-endian (0xB7, 0x90 untuk 0x90B7)
     * - [13]      : Jumlah blok (1 byte = 0x01)
     * - [14..15]  : Elemen blok list format 2-byte (0x80, blockNumber)
     */
    fun buildReadCommand(idm: ByteArray, serviceCode: Int, blockNumber: Int): ByteArray {
        val buffer = ByteArray(16)
        buffer[0] = 16.toByte()
        buffer[1] = 0x06.toByte()
        System.arraycopy(idm, 0, buffer, 2, 8)
        buffer[10] = 0x01.toByte()
        buffer[11] = (serviceCode and 0xFF).toByte()
        buffer[12] = ((serviceCode shr 8) and 0xFF).toByte()
        buffer[13] = 0x01.toByte()
        buffer[14] = 0x80.toByte() // 2-byte block list element format (service 0)
        buffer[15] = (blockNumber and 0xFF).toByte()
        return buffer
    }

    /**
     * Mengekstrak 16 byte data blok dari respon FeliCa Read Without Encryption (0x07).
     * Memeriksa Status Flag 1 (offset 10) == 0 (sukses).
     */
    fun extractBlockData(response: ByteArray): ByteArray? {
        if (response.size < 29) return null
        if (response[1] != 0x07.toByte()) return null // Bukan Read Without Encryption response
        if (response[10] != 0.toByte()) return null   // Status Flag 1 error
        return response.copyOfRange(13, 29)
    }

    /**
     * Mengekstrak 16 karakter nomor kartu KMT dari blok 0 (ASCII).
     */
    fun parseCardNumber(block0: ByteArray): String {
        if (block0.isEmpty()) return ""
        val ascii = String(block0, Charsets.US_ASCII).trim()
        return ascii.filter { it.isLetterOrDigit() }
    }

    /**
     * Mengekstrak nilai saldo KMT dari blok 1.
     * 4 byte integer little-endian pada offset 0..3.
     */
    fun parseBalance(block1: ByteArray): Long {
        if (block1.size < 4) return 0L
        return (block1[0].toLong() and 0xFFL) or
                ((block1[1].toLong() and 0xFFL) shl 8) or
                ((block1[2].toLong() and 0xFFL) shl 16) or
                ((block1[3].toLong() and 0xFFL) shl 24)
    }

    /**
     * Mengekstrak riwayat transaksi KMT dari blok 16 byte.
     */
    fun parseHistoryBlock(block: ByteArray): NfcCardTransaction? {
        if (block.size < 16) return null
        if (block.all { it == 0.toByte() }) return null

        val typeByte = block[12].toInt() and 0xFF
        val type = when (typeByte) {
            0 -> NfcTransactionType.TOPUP
            1 -> NfcTransactionType.PAYMENT
            else -> NfcTransactionType.UNKNOWN
        }

        // Nominal: 4 byte integer big-endian pada byte [4..7]
        val amount = ((block[4].toLong() and 0xFFL) shl 24) or
                ((block[5].toLong() and 0xFFL) shl 16) or
                ((block[6].toLong() and 0xFFL) shl 8) or
                (block[7].toLong() and 0xFFL)

        if (amount <= 0 && typeByte != 0) return null

        // Waktu: Detik sejak 2000-01-01 07:00:00 GMT+7 pada byte [0..3]
        val seconds = ((block[0].toLong() and 0xFFL) shl 24) or
                ((block[1].toLong() and 0xFFL) shl 16) or
                ((block[2].toLong() and 0xFFL) shl 8) or
                (block[3].toLong() and 0xFFL)

        val timestamp = if (seconds > 0) {
            KMT_BASE_EPOCH_MS + (seconds * 1000L)
        } else {
            System.currentTimeMillis()
        }

        // Kode stasiun: 2 byte integer big-endian pada byte [8..9]
        val stationId = ((block[8].toInt() and 0xFF) shl 8) or (block[9].toInt() and 0xFF)
        val stationName = KmtStationData.getStationName(stationId) ?: "Stasiun $stationId"

        val terminalId = "KMT-$stationId"

        return NfcCardTransaction(
            type = type,
            amount = amount,
            balanceAfter = null,
            timestamp = timestamp,
            dateFormatted = EmoneyHistoryParser.formatDate(timestamp),
            timeFormatted = EmoneyHistoryParser.formatTime(timestamp),
            locationId = terminalId,
            rawDescription = stationName
        )
    }
}

