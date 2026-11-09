package com.trackit.app.nfc.parser

import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.util.Log
import com.trackit.app.nfc.model.EmoneyCardResult
import com.trackit.app.nfc.model.EmoneyCardType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object EmoneyCardParser {

    private const val TAG = "EmoneyCardParser"

    /**
     * Membaca kartu uang elektronik dari Android Tag secara otomatis menggunakan Sequential Probing.
     * Dijalankan pada Dispatchers.IO agar tidak memblokir UI thread.
     */
    suspend fun readCard(tag: Tag): Result<EmoneyCardResult> = withContext(Dispatchers.IO) {
        val isoDep = IsoDep.get(tag) ?: return@withContext Result.failure(
            IllegalStateException("Kartu tidak mendukung protokol ISO-DEP (ISO 14443-4).")
        )

        Log.d(TAG, "readCard: Tag detected, techList=${tag.techList.joinToString()}")

        try {
            isoDep.connect()
            isoDep.timeout = 10000 // 10000 ms timeout per transceive untuk keandalan pembacaan

            Log.d(TAG, "readCard: IsoDep connected, starting sequential probing...")

            // 1. Coba Mandiri e-Money
            parseMandiriEmoney(isoDep)?.let {
                Log.i(TAG, "readCard: Mandiri e-Money detected")
                return@withContext Result.success(it)
            }

            // 2. Coba BCA Flazz (Mendukung Flazz Gen 2 CAT & PBOC AID)
            parseBcaFlazz(isoDep)?.let {
                Log.i(TAG, "readCard: BCA Flazz detected")
                return@withContext Result.success(it)
            }

            // 3. Coba BNI TapCash
            parseBniTapcash(isoDep)?.let {
                Log.i(TAG, "readCard: BNI TapCash detected")
                return@withContext Result.success(it)
            }

            // 4. Coba BRI Brizzi
            parseBriBrizzi(isoDep)?.let {
                Log.i(TAG, "readCard: BRI Brizzi detected")
                return@withContext Result.success(it)
            }

            // 5. Coba Bank DKI JakCard
            parseDkiJakcard(isoDep)?.let {
                Log.i(TAG, "readCard: DKI JakCard detected")
                return@withContext Result.success(it)
            }

            // 6. Coba Bank Mega MegaCash
            parseMegaCash(isoDep)?.let {
                Log.i(TAG, "readCard: Mega Cash detected")
                return@withContext Result.success(it)
            }

            Log.w(TAG, "readCard: All card parsers returned null. Card not recognized.")
            Result.failure(Exception("Kartu terdeteksi namun jenisnya belum didukung."))
        } catch (e: Exception) {
            Log.e(TAG, "readCard error: ${e.message}", e)
            Result.failure(e)
        } finally {
            try {
                if (isoDep.isConnected) isoDep.close()
            } catch (_: Exception) {}
        }
    }

    private fun parseMandiriEmoney(isoDep: IsoDep): EmoneyCardResult? {
        if (!isoDep.isConnected) return null
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
        } catch (e: Exception) {
            if (!isoDep.isConnected) throw e
            null
        }
    }

    private fun parseBcaFlazz(isoDep: IsoDep): EmoneyCardResult? {
        if (!isoDep.isConnected) return null
        return try {
            // Coba AID autentik (Gen 2) terlebih dahulu, jika gagal coba fallback AID CAT & PBOC
            var isGen2 = true
            var selectRes = isoDep.transceive(ApduCommands.FLAZZ_SELECT_AID)
            Log.d(TAG, "parseBcaFlazz: select AID Gen 2 res=${ApduCommands.toHexString(selectRes)}")
            if (!ApduCommands.isSuccess(selectRes)) {
                isGen2 = false
                selectRes = isoDep.transceive(ApduCommands.FLAZZ_SELECT_AID_CAT)
                Log.d(TAG, "parseBcaFlazz: select CAT res=${ApduCommands.toHexString(selectRes)}")
                if (!ApduCommands.isSuccess(selectRes)) {
                    selectRes = isoDep.transceive(ApduCommands.FLAZZ_SELECT_AID_PBOC)
                    Log.d(TAG, "parseBcaFlazz: select PBOC res=${ApduCommands.toHexString(selectRes)}")
                }
            }
            if (!ApduCommands.isSuccess(selectRes)) return null

            var cardNumber = ""
            var balance: Long? = null

            if (isGen2) {
                // Urutan APDU Gen 2 autentik (dari native card-lib eSaldo Info):
                // 1. Select EF 0200
                try {
                    val selectFileRes = isoDep.transceive(ApduCommands.FLAZZ_SELECT_FILE)
                    Log.d(TAG, "parseBcaFlazz: select File res=${ApduCommands.toHexString(selectFileRes)}")
                } catch (e: Exception) {
                    Log.w(TAG, "parseBcaFlazz: select file exception: ${e.message}")
                }

                // 2. Read Binary / Record untuk nomor kartu (Track 2)
                try {
                    val readRecordRes = isoDep.transceive(ApduCommands.FLAZZ_READ_RECORD)
                    Log.d(TAG, "parseBcaFlazz: read Record res=${ApduCommands.toHexString(readRecordRes)}")
                    if (ApduCommands.isSuccess(readRecordRes)) {
                        val ascii = String(readRecordRes, Charsets.US_ASCII)
                        val start = ascii.indexOf(';')
                        val end = ascii.indexOf('=')
                        if (start != -1 && end != -1 && end > start) {
                            cardNumber = ascii.substring(start + 1, end)
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "parseBcaFlazz: read record exception: ${e.message}")
                }

                // 3. Read Balance
                try {
                    val balRes = isoDep.transceive(ApduCommands.FLAZZ_GET_BALANCE)
                    Log.d(TAG, "parseBcaFlazz: balRes=${ApduCommands.toHexString(balRes)}")
                    if (ApduCommands.isSuccess(balRes)) {
                        val hex = ApduCommands.toHexString(balRes)
                        if (hex.length >= 8) {
                            balance = hex.substring(2, 8).toLongOrNull(16)
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "parseBcaFlazz: get balance exception: ${e.message}")
                }
            } else {
                // Fallback untuk kartu legacy (Gen 1 / CAT / PBOC)
                try {
                    val track2Res = isoDep.transceive(ApduCommands.FLAZZ_LEGACY_READ_TRACK2)
                    Log.d(TAG, "parseBcaFlazz: legacy track2Res=${ApduCommands.toHexString(track2Res)}")
                    if (ApduCommands.isSuccess(track2Res)) {
                        val ascii = String(track2Res, Charsets.US_ASCII)
                        val start = ascii.indexOf(';')
                        val end = ascii.indexOf('=')
                        if (start != -1 && end != -1 && end > start) {
                            cardNumber = ascii.substring(start + 1, end)
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "parseBcaFlazz: legacy read track2 exception: ${e.message}")
                }

                try {
                    val balRes = isoDep.transceive(ApduCommands.FLAZZ_LEGACY_GET_BALANCE)
                    Log.d(TAG, "parseBcaFlazz: legacy balRes=${ApduCommands.toHexString(balRes)}")
                    if (ApduCommands.isSuccess(balRes) && balRes.size >= 4) {
                        val hex = ApduCommands.toHexString(balRes)
                        if (hex.length >= 8) {
                            balance = hex.substring(2, 8).toLongOrNull(16)
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "parseBcaFlazz: legacy get balance exception: ${e.message}")
                }
            }

            if (balance == null) return null

            Log.i(TAG, "parseBcaFlazz: success! card=$cardNumber, balance=$balance")
            EmoneyCardResult(
                cardType = EmoneyCardType.BCA_FLAZZ,
                cardNumber = cardNumber,
                balance = balance
            )
        } catch (e: Exception) {
            Log.w(TAG, "parseBcaFlazz: exception: ${e.message}")
            if (!isoDep.isConnected) throw e
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
        } catch (e: Exception) {
            if (!isoDep.isConnected) throw e
            null
        }
    }

    private fun parseBriBrizzi(isoDep: IsoDep): EmoneyCardResult? {
        if (!isoDep.isConnected) return null
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
        } catch (e: Exception) {
            if (!isoDep.isConnected) throw e
            null
        }
    }

    private fun parseDkiJakcard(isoDep: IsoDep): EmoneyCardResult? {
        if (!isoDep.isConnected) return null
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
        } catch (e: Exception) {
            if (!isoDep.isConnected) throw e
            null
        }
    }

    private fun parseMegaCash(isoDep: IsoDep): EmoneyCardResult? {
        if (!isoDep.isConnected) return null
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
        } catch (e: Exception) {
            if (!isoDep.isConnected) throw e
            null
        }
    }
}

