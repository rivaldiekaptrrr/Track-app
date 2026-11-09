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
            var selectRes = isoDep.transceive(ApduCommands.MANDIRI_SELECT_AID)
            Log.d(TAG, "parseMandiriEmoney: selectRes=${ApduCommands.toHexString(selectRes)}")
            if (!ApduCommands.isSuccess(selectRes)) {
                selectRes = isoDep.transceive(ApduCommands.MANDIRI_SELECT_AID_ALT)
                Log.d(TAG, "parseMandiriEmoney: selectResAlt=${ApduCommands.toHexString(selectRes)}")
            }
            if (!ApduCommands.isSuccess(selectRes)) return null

            var infoRes = isoDep.transceive(ApduCommands.MANDIRI_READ_INFO)
            Log.d(TAG, "parseMandiriEmoney: infoRes=${ApduCommands.toHexString(infoRes)}")
            if (!ApduCommands.isSuccess(infoRes) || infoRes.size < 8) {
                infoRes = isoDep.transceive(ApduCommands.MANDIRI_READ_INFO_RECORD)
                Log.d(TAG, "parseMandiriEmoney: infoResRecord=${ApduCommands.toHexString(infoRes)}")
            }
            if (!ApduCommands.isSuccess(infoRes) || infoRes.size < 8) return null

            val cardNumber = ApduCommands.toHexString(infoRes, 0, 8)
            val isG2 = if (infoRes.size > 18) (infoRes[18].toInt() and 0xFF) == 0x83 else false

            var balRes = isoDep.transceive(ApduCommands.MANDIRI_READ_BALANCE)
            Log.d(TAG, "parseMandiriEmoney: balRes=${ApduCommands.toHexString(balRes)}")
            if (!ApduCommands.isSuccess(balRes) || balRes.size < 4) {
                balRes = isoDep.transceive(ApduCommands.MANDIRI_READ_BALANCE_SV)
                Log.d(TAG, "parseMandiriEmoney: balResSV=${ApduCommands.toHexString(balRes)}")
            }
            if (!ApduCommands.isSuccess(balRes) || balRes.size < 4) return null

            var balance = 0L
            for (i in 0 until 4) {
                balance += ((balRes[i].toLong() and 0xFFL) shl (i * 8))
            }

            Log.i(TAG, "parseMandiriEmoney: success! card=$cardNumber, balance=$balance, isG2=$isG2")
            EmoneyCardResult(
                cardType = EmoneyCardType.MANDIRI_EMONEY,
                cardNumber = cardNumber,
                balance = balance,
                isChipG2 = isG2
            )
        } catch (e: Exception) {
            Log.w(TAG, "parseMandiriEmoney: exception: ${e.message}")
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
        if (!isoDep.isConnected) return null
        return try {
            // Coba AID ISO (CommandAPDU.qw BNI) terlebih dahulu, lalu DESFire 999999, lalu ALT
            var selectRes = isoDep.transceive(ApduCommands.TAPCASH_SELECT_AID_ISO)
            Log.d(TAG, "parseBniTapcash: select ISO res=${ApduCommands.toHexString(selectRes)}")
            if (!ApduCommands.isSuccess(selectRes)) {
                selectRes = isoDep.transceive(ApduCommands.TAPCASH_SELECT_APP)
                Log.d(TAG, "parseBniTapcash: select APP res=${ApduCommands.toHexString(selectRes)}")
                if (!ApduCommands.isSuccess(selectRes)) {
                    selectRes = isoDep.transceive(ApduCommands.TAPCASH_SELECT_AID_ISO_ALT)
                    Log.d(TAG, "parseBniTapcash: select ISO ALT res=${ApduCommands.toHexString(selectRes)}")
                }
            }
            if (!ApduCommands.isSuccess(selectRes)) return null

            val fileRes = isoDep.transceive(ApduCommands.TAPCASH_READ_FILE)
            Log.d(TAG, "parseBniTapcash: fileRes=${ApduCommands.toHexString(fileRes)}")
            if (!ApduCommands.isSuccess(fileRes) || fileRes.size < 16) return null

            val b2 = fileRes[2].toLong() and 0xFFL
            val b3 = fileRes[3].toLong() and 0xFFL
            val b4 = fileRes[4].toLong() and 0xFFL
            val balance = (b2 shl 16) or (b3 shl 8) or b4

            val cardNumber = ApduCommands.toHexString(fileRes, 8, 8)

            Log.i(TAG, "parseBniTapcash: success! card=$cardNumber, balance=$balance")
            EmoneyCardResult(
                cardType = EmoneyCardType.BNI_TAPCASH,
                cardNumber = cardNumber,
                balance = balance
            )
        } catch (e: Exception) {
            Log.w(TAG, "parseBniTapcash: exception: ${e.message}")
            if (!isoDep.isConnected) throw e
            null
        }
    }

    private fun parseBriBrizzi(isoDep: IsoDep): EmoneyCardResult? {
        if (!isoDep.isConnected) return null
        return try {
            // Coba App ID autentik 010000 (CommandAPDU.uu), lalu 100001, lalu legacy 112233
            var selectRes = isoDep.transceive(ApduCommands.BRIZZI_SELECT_APP)
            Log.d(TAG, "parseBriBrizzi: select APP res=${ApduCommands.toHexString(selectRes)}")
            if (!ApduCommands.isSuccess(selectRes)) {
                selectRes = isoDep.transceive(ApduCommands.BRIZZI_SELECT_APP_ALT)
                Log.d(TAG, "parseBriBrizzi: select APP ALT res=${ApduCommands.toHexString(selectRes)}")
                if (!ApduCommands.isSuccess(selectRes)) {
                    selectRes = isoDep.transceive(ApduCommands.BRIZZI_SELECT_APP_LEGACY)
                    Log.d(TAG, "parseBriBrizzi: select APP LEGACY res=${ApduCommands.toHexString(selectRes)}")
                }
            }
            if (!ApduCommands.isSuccess(selectRes)) return null

            var cardNumber = ""
            try {
                val infoRes = isoDep.transceive(ApduCommands.BRIZZI_READ_INFO)
                Log.d(TAG, "parseBriBrizzi: infoRes=${ApduCommands.toHexString(infoRes)}")
                if (ApduCommands.isSuccess(infoRes)) {
                    val hexInfo = ApduCommands.toHexString(infoRes)
                    if (hexInfo.length >= 22) {
                        cardNumber = hexInfo.substring(6, 22)
                    } else if (infoRes.size >= 11) {
                        cardNumber = ApduCommands.toHexString(infoRes, 3, 8)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "parseBriBrizzi: read info exception: ${e.message}")
            }

            var balance: Long? = null
            try {
                val balRes = isoDep.transceive(ApduCommands.BRIZZI_READ_BALANCE_GET_VALUE)
                Log.d(TAG, "parseBriBrizzi: balRes getValue=${ApduCommands.toHexString(balRes)}")
                if (ApduCommands.isSuccess(balRes) && balRes.size >= 4) {
                    var bal = 0L
                    for (i in 0 until 4) {
                        bal += ((balRes[i].toLong() and 0xFFL) shl (i * 8))
                    }
                    balance = bal
                }
            } catch (_: Exception) {}

            if (balance == null) {
                val balRes = isoDep.transceive(ApduCommands.BRIZZI_READ_BALANCE)
                Log.d(TAG, "parseBriBrizzi: balRes readData=${ApduCommands.toHexString(balRes)}")
                if (ApduCommands.isSuccess(balRes) && balRes.size >= 4) {
                    var bal = 0L
                    for (i in 0 until 4) {
                        bal += ((balRes[i].toLong() and 0xFFL) shl (i * 8))
                    }
                    balance = bal
                }
            }

            if (balance == null) return null

            Log.i(TAG, "parseBriBrizzi: success! card=$cardNumber, balance=$balance")
            EmoneyCardResult(
                cardType = EmoneyCardType.BRI_BRIZZI,
                cardNumber = cardNumber,
                balance = balance
            )
        } catch (e: Exception) {
            Log.w(TAG, "parseBriBrizzi: exception: ${e.message}")
            if (!isoDep.isConnected) throw e
            null
        }
    }

    private fun parseDkiJakcard(isoDep: IsoDep): EmoneyCardResult? {
        if (!isoDep.isConnected) return null
        return try {
            // Coba AID autentik (CommandAPDU.bttt A0000005714E4A43), jika gagal coba fallback ALT
            var isAuthentic = true
            var selectRes = isoDep.transceive(ApduCommands.JAKCARD_SELECT_APP)
            Log.d(TAG, "parseDkiJakcard: select authentic res=${ApduCommands.toHexString(selectRes)}")
            if (!ApduCommands.isSuccess(selectRes)) {
                isAuthentic = false
                selectRes = isoDep.transceive(ApduCommands.JAKCARD_SELECT_APP_ALT)
                Log.d(TAG, "parseDkiJakcard: select ALT res=${ApduCommands.toHexString(selectRes)}")
            }
            if (!ApduCommands.isSuccess(selectRes)) return null

            var cardNumber = ""
            val selectHex = ApduCommands.toHexString(selectRes)
            if (selectHex.length >= 32) {
                // Di Jcard.java eSaldo: strA.substring(16, 32) adalah nomor kartu
                cardNumber = selectHex.substring(16, 32)
            } else if (selectRes.size >= 16) {
                cardNumber = ApduCommands.toHexString(selectRes, 8, 8)
            }

            var balance: Long? = null
            if (isAuthentic) {
                // CommandAPDU.hhhh: 90 4C 00 00 04
                val balRes = isoDep.transceive(ApduCommands.JAKCARD_READ_BALANCE)
                Log.d(TAG, "parseDkiJakcard: balRes hhhh=${ApduCommands.toHexString(balRes)}")
                if (ApduCommands.isSuccess(balRes)) {
                    val hexBal = ApduCommands.toHexString(balRes)
                    if (hexBal.length >= 8) {
                        balance = hexBal.substring(0, 8).toLongOrNull(16)
                    }
                }
            }

            if (balance == null) {
                val balRes = isoDep.transceive(ApduCommands.JAKCARD_READ_BALANCE_ISO)
                Log.d(TAG, "parseDkiJakcard: balRes ISO=${ApduCommands.toHexString(balRes)}")
                if (ApduCommands.isSuccess(balRes) && balRes.size >= 4) {
                    val b0 = balRes[0].toLong() and 0xFFL
                    val b1 = balRes[1].toLong() and 0xFFL
                    val b2 = balRes[2].toLong() and 0xFFL
                    val b3 = balRes[3].toLong() and 0xFFL
                    balance = (b0 shl 24) or (b1 shl 16) or (b2 shl 8) or b3
                }
            }

            if (balance == null) return null

            Log.i(TAG, "parseDkiJakcard: success! card=$cardNumber, balance=$balance")
            EmoneyCardResult(
                cardType = EmoneyCardType.DKI_JAKCARD,
                cardNumber = cardNumber,
                balance = balance
            )
        } catch (e: Exception) {
            Log.w(TAG, "parseDkiJakcard: exception: ${e.message}")
            if (!isoDep.isConnected) throw e
            null
        }
    }

    private fun parseMegaCash(isoDep: IsoDep): EmoneyCardResult? {
        if (!isoDep.isConnected) return null
        return try {
            // Coba DESFire Select App autentik (CommandAPDU.mgaa 008800), lalu fallback ISO AID
            var isDesfire = true
            var selectRes = isoDep.transceive(ApduCommands.MEGACASH_SELECT_APP)
            Log.d(TAG, "parseMegaCash: select DESFire res=${ApduCommands.toHexString(selectRes)}")
            if (!ApduCommands.isSuccess(selectRes)) {
                isDesfire = false
                selectRes = isoDep.transceive(ApduCommands.MEGACASH_SELECT_AID_ISO)
                Log.d(TAG, "parseMegaCash: select ISO res=${ApduCommands.toHexString(selectRes)}")
            }
            if (!ApduCommands.isSuccess(selectRes)) return null

            var cardNumber = ""
            var balance: Long? = null

            if (isDesfire) {
                // CommandAPDU.mgbb (Read Info: 10 bytes)
                try {
                    val infoRes = isoDep.transceive(ApduCommands.MEGACASH_READ_INFO)
                    Log.d(TAG, "parseMegaCash: infoRes mgbb=${ApduCommands.toHexString(infoRes)}")
                    if (ApduCommands.isSuccess(infoRes)) {
                        val hexInfo = ApduCommands.toHexString(infoRes)
                        if (hexInfo.length > 8) {
                            cardNumber = hexInfo.substring(4, hexInfo.length - 4)
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "parseMegaCash: read info exception: ${e.message}")
                }

                // CommandAPDU.mgcc (Get Value)
                try {
                    val balRes = isoDep.transceive(ApduCommands.MEGACASH_READ_BALANCE)
                    Log.d(TAG, "parseMegaCash: balRes mgcc=${ApduCommands.toHexString(balRes)}")
                    if (ApduCommands.isSuccess(balRes) && balRes.size > 2) {
                        // Sesuai MegaCash.java: bytes sebelum status word di-reverse (little-endian ke big-endian)
                        val payloadLen = balRes.size - 2
                        val reversed = ByteArray(payloadLen)
                        val startIdx = balRes.size - 3
                        for (i in 0 until payloadLen) {
                            reversed[i] = balRes[startIdx - i]
                        }
                        val hexRev = ApduCommands.toHexString(reversed)
                        balance = hexRev.toLongOrNull(16)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "parseMegaCash: get balance exception: ${e.message}")
                }
            }

            if (balance == null) {
                val balRes = isoDep.transceive(ApduCommands.MEGACASH_READ_BALANCE_ISO)
                Log.d(TAG, "parseMegaCash: balRes ISO=${ApduCommands.toHexString(balRes)}")
                if (ApduCommands.isSuccess(balRes) && balRes.size >= 4) {
                    val b0 = balRes[0].toLong() and 0xFFL
                    val b1 = balRes[1].toLong() and 0xFFL
                    val b2 = balRes[2].toLong() and 0xFFL
                    val b3 = balRes[3].toLong() and 0xFFL
                    balance = (b0 shl 24) or (b1 shl 16) or (b2 shl 8) or b3
                }
            }

            if (balance == null) return null

            Log.i(TAG, "parseMegaCash: success! card=$cardNumber, balance=$balance")
            EmoneyCardResult(
                cardType = EmoneyCardType.MEGA_CASH,
                cardNumber = cardNumber,
                balance = balance
            )
        } catch (e: Exception) {
            Log.w(TAG, "parseMegaCash: exception: ${e.message}")
            if (!isoDep.isConnected) throw e
            null
        }
    }
}

