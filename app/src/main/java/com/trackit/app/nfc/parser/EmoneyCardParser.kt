package com.trackit.app.nfc.parser

import android.nfc.Tag
import android.nfc.tech.IsoDep
import com.trackit.app.nfc.model.EmoneyCardResult
import com.trackit.app.nfc.model.EmoneyCardType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object EmoneyCardParser {

    /**
     * Membaca kartu uang elektronik dari Android Tag secara otomatis menggunakan Sequential Probing.
     * Dijalankan pada Dispatchers.IO agar tidak memblokir UI thread.
     */
    suspend fun readCard(tag: Tag): Result<EmoneyCardResult> = withContext(Dispatchers.IO) {
        val isoDep = IsoDep.get(tag) ?: return@withContext Result.failure(
            IllegalStateException("Kartu tidak mendukung protokol ISO-DEP (ISO 14443-4).")
        )

        try {
            isoDep.connect()
            isoDep.timeout = 2000 // 2000 ms timeout per transceive

            // 1. Coba Mandiri e-Money
            parseMandiriEmoney(isoDep)?.let { return@withContext Result.success(it) }

            // 2. Coba BCA Flazz
            parseBcaFlazz(isoDep)?.let { return@withContext Result.success(it) }

            // 3. Coba BNI TapCash
            parseBniTapcash(isoDep)?.let { return@withContext Result.success(it) }

            // 4. Coba BRI Brizzi
            parseBriBrizzi(isoDep)?.let { return@withContext Result.success(it) }

            // 5. Coba Bank DKI JakCard
            parseDkiJakcard(isoDep)?.let { return@withContext Result.success(it) }

            // 6. Coba Bank Mega MegaCash
            parseMegaCash(isoDep)?.let { return@withContext Result.success(it) }

            Result.failure(Exception("Kartu terdeteksi namun jenisnya belum didukung."))
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            try {
                if (isoDep.isConnected) isoDep.close()
            } catch (_: Exception) {}
        }
    }

    private fun parseMandiriEmoney(isoDep: IsoDep): EmoneyCardResult? {
        return try {
            val selectRes = isoDep.transceive(ApduCommands.MANDIRI_SELECT_AID)
            if (!ApduCommands.isSuccess(selectRes)) return null

            val infoRes = isoDep.transceive(ApduCommands.MANDIRI_READ_INFO)
            if (!ApduCommands.isSuccess(infoRes) || infoRes.size < 8) return null

            val cardNumber = ApduCommands.toHexString(infoRes, 0, 8)
            val isG2 = if (infoRes.size > 18) (infoRes[18].toInt() and 0xFF) == 0x83 else false

            val balRes = isoDep.transceive(ApduCommands.MANDIRI_READ_BALANCE)
            if (!ApduCommands.isSuccess(balRes) || balRes.size < 4) return null

            var balance = 0L
            for (i in 0 until 4) {
                balance += ((balRes[i].toLong() and 0xFFL) shl (i * 8))
            }

            EmoneyCardResult(
                cardType = EmoneyCardType.MANDIRI_EMONEY,
                cardNumber = cardNumber,
                balance = balance,
                isChipG2 = isG2
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun parseBcaFlazz(isoDep: IsoDep): EmoneyCardResult? {
        return try {
            val selectRes = isoDep.transceive(ApduCommands.FLAZZ_SELECT_AID)
            if (!ApduCommands.isSuccess(selectRes)) return null

            var cardNumber = ""
            try {
                val track2Res = isoDep.transceive(ApduCommands.FLAZZ_READ_TRACK2)
                if (ApduCommands.isSuccess(track2Res)) {
                    val ascii = String(track2Res, Charsets.US_ASCII)
                    val start = ascii.indexOf(';')
                    val end = ascii.indexOf('=')
                    if (start != -1 && end != -1 && end > start) {
                        cardNumber = ascii.substring(start + 1, end)
                    }
                }
            } catch (_: Exception) {}

            val balRes = isoDep.transceive(ApduCommands.FLAZZ_GET_BALANCE)
            if (!ApduCommands.isSuccess(balRes) || balRes.size < 4) return null

            val hex = ApduCommands.toHexString(balRes)
            if (hex.length < 8) return null
            val balance = hex.substring(2, 8).toLong(16)

            EmoneyCardResult(
                cardType = EmoneyCardType.BCA_FLAZZ,
                cardNumber = cardNumber,
                balance = balance
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun parseBniTapcash(isoDep: IsoDep): EmoneyCardResult? {
        return try {
            val selectRes = isoDep.transceive(ApduCommands.TAPCASH_SELECT_APP)
            if (!ApduCommands.isSuccess(selectRes)) return null

            val fileRes = isoDep.transceive(ApduCommands.TAPCASH_READ_FILE)
            if (!ApduCommands.isSuccess(fileRes) || fileRes.size < 16) return null

            val b2 = fileRes[2].toLong() and 0xFFL
            val b3 = fileRes[3].toLong() and 0xFFL
            val b4 = fileRes[4].toLong() and 0xFFL
            val balance = (b2 shl 16) or (b3 shl 8) or b4

            val cardNumber = ApduCommands.toHexString(fileRes, 8, 8)

            EmoneyCardResult(
                cardType = EmoneyCardType.BNI_TAPCASH,
                cardNumber = cardNumber,
                balance = balance
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun parseBriBrizzi(isoDep: IsoDep): EmoneyCardResult? {
        return try {
            val selectRes = isoDep.transceive(ApduCommands.BRIZZI_SELECT_APP)
            if (!ApduCommands.isSuccess(selectRes)) return null

            var cardNumber = ""
            try {
                val infoRes = isoDep.transceive(ApduCommands.BRIZZI_READ_INFO)
                if (ApduCommands.isSuccess(infoRes) && infoRes.size >= 11) {
                    cardNumber = ApduCommands.toHexString(infoRes, 3, 8)
                }
            } catch (_: Exception) {}

            val balRes = isoDep.transceive(ApduCommands.BRIZZI_READ_BALANCE)
            if (!ApduCommands.isSuccess(balRes) || balRes.size < 4) return null

            var balance = 0L
            for (i in 0 until 4) {
                balance += ((balRes[i].toLong() and 0xFFL) shl (i * 8))
            }

            EmoneyCardResult(
                cardType = EmoneyCardType.BRI_BRIZZI,
                cardNumber = cardNumber,
                balance = balance
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun parseDkiJakcard(isoDep: IsoDep): EmoneyCardResult? {
        return try {
            val selectRes = isoDep.transceive(ApduCommands.JAKCARD_SELECT_APP)
            if (!ApduCommands.isSuccess(selectRes)) return null

            var cardNumber = ""
            if (selectRes.size >= 16) {
                cardNumber = ApduCommands.toHexString(selectRes, 8, 8)
            }

            val balRes = isoDep.transceive(ApduCommands.JAKCARD_READ_BALANCE)
            if (!ApduCommands.isSuccess(balRes) || balRes.size < 4) return null

            val b0 = balRes[0].toLong() and 0xFFL
            val b1 = balRes[1].toLong() and 0xFFL
            val b2 = balRes[2].toLong() and 0xFFL
            val b3 = balRes[3].toLong() and 0xFFL
            val balance = (b0 shl 24) or (b1 shl 16) or (b2 shl 8) or b3

            EmoneyCardResult(
                cardType = EmoneyCardType.DKI_JAKCARD,
                cardNumber = cardNumber,
                balance = balance
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun parseMegaCash(isoDep: IsoDep): EmoneyCardResult? {
        return try {
            val selectRes = isoDep.transceive(ApduCommands.MEGACASH_SELECT_AID)
            if (!ApduCommands.isSuccess(selectRes)) return null

            val balRes = isoDep.transceive(ApduCommands.MEGACASH_READ_BALANCE)
            if (!ApduCommands.isSuccess(balRes) || balRes.size < 4) return null

            val b0 = balRes[0].toLong() and 0xFFL
            val b1 = balRes[1].toLong() and 0xFFL
            val b2 = balRes[2].toLong() and 0xFFL
            val b3 = balRes[3].toLong() and 0xFFL
            val balance = (b0 shl 24) or (b1 shl 16) or (b2 shl 8) or b3

            EmoneyCardResult(
                cardType = EmoneyCardType.MEGA_CASH,
                cardNumber = "",
                balance = balance
            )
        } catch (_: Exception) {
            null
        }
    }
}

