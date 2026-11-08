package com.trackit.app.nfc.parser

object ApduCommands {
    // Mandiri e-Money
    val MANDIRI_SELECT_AID = byteArrayOf(
        0x00, 0xA4.toByte(), 0x04, 0x00, 0x08,
        0xA0.toByte(), 0x00, 0x00, 0x00, 0x03, 0x00, 0x00, 0x00
    )
    val MANDIRI_READ_INFO = byteArrayOf(0x00, 0xB0.toByte(), 0x00, 0x00, 0x1D)
    val MANDIRI_READ_BALANCE = byteArrayOf(0x00, 0xB0.toByte(), 0x00, 0x00, 0x04)

    // BCA Flazz
    val FLAZZ_SELECT_AID = byteArrayOf(
        0x00, 0xA4.toByte(), 0x04, 0x00, 0x08,
        0xA0.toByte(), 0x00, 0x00, 0x00, 0x18, 0x43, 0x41, 0x54
    )
    val FLAZZ_READ_TRACK2 = byteArrayOf(0x00, 0xB2.toByte(), 0x01, 0x0C, 0x00)
    val FLAZZ_GET_BALANCE = byteArrayOf(0x80.toByte(), 0x5C, 0x00, 0x02, 0x04)

    // BNI TapCash
    val TAPCASH_SELECT_APP = byteArrayOf(
        0x90.toByte(), 0x5A, 0x00, 0x00, 0x03,
        0x99.toByte(), 0x99.toByte(), 0x99.toByte(), 0x00
    )
    val TAPCASH_READ_FILE = byteArrayOf(
        0x90.toByte(), 0xBD.toByte(), 0x00, 0x00, 0x07,
        0x01, 0x00, 0x00, 0x00, 0x29, 0x00, 0x00, 0x00
    )

    // BRI Brizzi
    val BRIZZI_SELECT_APP = byteArrayOf(
        0x90.toByte(), 0x5A, 0x00, 0x00, 0x03, 0x11, 0x22, 0x33, 0x00
    )
    val BRIZZI_READ_INFO = byteArrayOf(
        0x90.toByte(), 0xBD.toByte(), 0x00, 0x00, 0x07,
        0x01, 0x00, 0x00, 0x00, 0x16, 0x00, 0x00, 0x00
    )
    val BRIZZI_READ_BALANCE = byteArrayOf(
        0x90.toByte(), 0xBD.toByte(), 0x00, 0x00, 0x07,
        0x02, 0x00, 0x00, 0x00, 0x04, 0x00, 0x00, 0x00
    )

    // Bank DKI JakCard
    val JAKCARD_SELECT_APP = byteArrayOf(
        0x00, 0xA4.toByte(), 0x04, 0x00, 0x07,
        0xA0.toByte(), 0x00, 0x00, 0x00, 0x03, 0x86.toByte(), 0x98.toByte()
    )
    val JAKCARD_READ_BALANCE = byteArrayOf(0x00, 0xB0.toByte(), 0x00, 0x00, 0x04)

    // Bank Mega MegaCash
    val MEGACASH_SELECT_AID = byteArrayOf(
        0x00, 0xA4.toByte(), 0x04, 0x00, 0x07,
        0xA0.toByte(), 0x00, 0x00, 0x03, 0x86.toByte(), 0x00, 0x01
    )
    val MEGACASH_READ_BALANCE = byteArrayOf(0x00, 0xB0.toByte(), 0x00, 0x00, 0x04)

    /**
     * Mengecek apakah respon APDU berstatus sukses (90 00 atau 91 00 pada DESFire)
     */
    fun isSuccess(response: ByteArray?): Boolean {
        if (response == null || response.size < 2) return false
        val sw1 = response[response.size - 2].toInt() and 0xFF
        val sw2 = response[response.size - 1].toInt() and 0xFF
        return (sw1 == 0x90 && sw2 == 0x00) || (sw1 == 0x91 && sw2 == 0x00)
    }

    /**
     * Konversi byte array ke hex string huruf besar
     */
    fun toHexString(bytes: ByteArray, offset: Int = 0, length: Int = bytes.size): String {
        val sb = StringBuilder()
        val end = (offset + length).coerceAtMost(bytes.size)
        for (i in offset until end) {
            sb.append(String.format("%02X", bytes[i]))
        }
        return sb.toString()
    }
}

