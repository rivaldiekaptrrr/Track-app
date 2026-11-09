package com.trackit.app.nfc.parser

object ApduCommands {
    // -------------------------------------------------------------
    // 1. Mandiri e-Money (ISO 14443-4 / Calypso)
    // -------------------------------------------------------------
    val MANDIRI_SELECT_AID = byteArrayOf(
        0x00, 0xA4.toByte(), 0x04, 0x00, 0x08,
        0xA0.toByte(), 0x00, 0x00, 0x00, 0x03, 0x00, 0x00, 0x00
    )
    val MANDIRI_SELECT_AID_ALT = byteArrayOf(
        0x00, 0xA4.toByte(), 0x04, 0x00, 0x08,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01
    )
    val MANDIRI_READ_INFO = byteArrayOf(0x00, 0xB0.toByte(), 0x00, 0x00, 0x1D)
    val MANDIRI_READ_INFO_RECORD = byteArrayOf(0x00, 0xB3.toByte(), 0x00, 0x00, 0x3F)
    val MANDIRI_READ_BALANCE = byteArrayOf(0x00, 0xB0.toByte(), 0x00, 0x00, 0x04)
    val MANDIRI_READ_BALANCE_SV = byteArrayOf(0x00, 0xB5.toByte(), 0x00, 0x00, 0x0A)

    // -------------------------------------------------------------
    // 2. BCA Flazz (ISO 7816 / PBOC / Flazz Gen 2)
    // -------------------------------------------------------------
    // Authentic APDU sequence from eSaldo Info native card-lib (Flazz Gen 2):
    // AID: A0 00 00 00 18 0F 00 00 01 80 01 (11 bytes, CLA=00 INS=A4 P1=04 P2=00 Lc=0B)
    val FLAZZ_SELECT_AID = byteArrayOf(
        0x00, 0xA4.toByte(), 0x04, 0x00, 0x0B,
        0xA0.toByte(), 0x00, 0x00, 0x00, 0x18, 0x0F, 0x00, 0x00, 0x01, 0x80.toByte(), 0x01
    )
    // Select Elementary File 0200 (CLA=00 INS=A4 P1=01 P2=00 Lc=02 Data=02 00)
    val FLAZZ_SELECT_FILE = byteArrayOf(
        0x00, 0xA4.toByte(), 0x01, 0x00, 0x02, 0x02, 0x00
    )
    // Read Binary / Record (CLA=00 INS=B0 P1=81 P2=00 Le=8E) -> returns ASCII with ';' and '='
    val FLAZZ_READ_RECORD = byteArrayOf(
        0x00, 0xB0.toByte(), 0x81.toByte(), 0x00, 0x8E.toByte()
    )
    // Get Balance (CLA=80 INS=32 P1=00 P2=03 Lc=04 Data=00 00 00 00) -> returns balance at substring(2, 8)
    val FLAZZ_GET_BALANCE = byteArrayOf(
        0x80.toByte(), 0x32, 0x00, 0x03, 0x04, 0x00, 0x00, 0x00, 0x00
    )

    // Fallback Flazz commands (Gen 1 / CAT / PBOC)
    val FLAZZ_SELECT_AID_CAT = byteArrayOf(
        0x00, 0xA4.toByte(), 0x04, 0x00, 0x08,
        0xA0.toByte(), 0x00, 0x00, 0x00, 0x18, 0x43, 0x41, 0x54
    )
    val FLAZZ_SELECT_AID_PBOC = byteArrayOf(
        0x00, 0xA4.toByte(), 0x04, 0x00, 0x07,
        0xA0.toByte(), 0x00, 0x00, 0x00, 0x03, 0x10, 0x10
    )
    val FLAZZ_LEGACY_READ_TRACK2 = byteArrayOf(0x00, 0xB2.toByte(), 0x01, 0x0C, 0x00)
    val FLAZZ_LEGACY_GET_BALANCE = byteArrayOf(0x80.toByte(), 0x5C, 0x00, 0x02, 0x04)

    // -------------------------------------------------------------
    // 3. BNI TapCash (MIFARE DESFire EV1)
    // -------------------------------------------------------------
    // Authentic AID ISO 7816: A0 00 42 4E 49 10 00 01 (CommandAPDU.qw, 42 4E 49 = "BNI")
    val TAPCASH_SELECT_AID_ISO = byteArrayOf(
        0x00, 0xA4.toByte(), 0x04, 0x00, 0x08,
        0xA0.toByte(), 0x00, 0x42, 0x4E, 0x49, 0x10, 0x00, 0x01
    )
    val TAPCASH_SELECT_AID_ISO_ALT = byteArrayOf(
        0x00, 0xA4.toByte(), 0x04, 0x00, 0x08,
        0xA0.toByte(), 0x00, 0x42, 0x4E, 0x49, 0x99.toByte(), 0x99.toByte(), 0x99.toByte()
    )
    // DESFire Native Select Application: 99 99 99
    val TAPCASH_SELECT_APP = byteArrayOf(
        0x90.toByte(), 0x5A, 0x00, 0x00, 0x03,
        0x99.toByte(), 0x99.toByte(), 0x99.toByte(), 0x00
    )
    val TAPCASH_READ_FILE = byteArrayOf(
        0x90.toByte(), 0xBD.toByte(), 0x00, 0x00, 0x07,
        0x01, 0x00, 0x00, 0x00, 0x29, 0x00, 0x00, 0x00
    )

    // -------------------------------------------------------------
    // 4. BRI Brizzi (MIFARE DESFire EV1)
    // -------------------------------------------------------------
    // Authentic DESFire Select Application dari eSaldo card-lib (CommandAPDU.uu): App ID 01 00 00
    val BRIZZI_SELECT_APP = byteArrayOf(
        0x90.toByte(), 0x5A, 0x00, 0x00, 0x03, 0x01, 0x00, 0x00, 0x00
    )
    val BRIZZI_SELECT_APP_ALT = byteArrayOf(
        0x90.toByte(), 0x5A, 0x00, 0x00, 0x03, 0x10, 0x00, 0x01, 0x00
    )
    val BRIZZI_SELECT_APP_LEGACY = byteArrayOf(
        0x90.toByte(), 0x5A, 0x00, 0x00, 0x03, 0x11, 0x22, 0x33, 0x00
    )
    val BRIZZI_READ_INFO = byteArrayOf(
        0x90.toByte(), 0xBD.toByte(), 0x00, 0x00, 0x07,
        0x01, 0x00, 0x00, 0x00, 0x16, 0x00, 0x00, 0x00
    )
    val BRIZZI_READ_BALANCE_GET_VALUE = byteArrayOf(
        0x90.toByte(), 0x6C, 0x00, 0x00, 0x00, 0x00, 0x00
    )
    val BRIZZI_READ_BALANCE = byteArrayOf(
        0x90.toByte(), 0xBD.toByte(), 0x00, 0x00, 0x07,
        0x02, 0x00, 0x00, 0x00, 0x04, 0x00, 0x00, 0x00
    )

    // -------------------------------------------------------------
    // 5. Bank DKI JakCard (ISO 14443-4 Type A)
    // -------------------------------------------------------------
    // Authentic AID dari eSaldo card-lib (CommandAPDU.bttt): A0 00 00 05 71 4E 4A 43 (ASCII "NJC")
    val JAKCARD_SELECT_APP = byteArrayOf(
        0x00, 0xA4.toByte(), 0x04, 0x00, 0x08,
        0xA0.toByte(), 0x00, 0x00, 0x05, 0x71, 0x4E, 0x4A, 0x43
    )
    val JAKCARD_SELECT_APP_ALT = byteArrayOf(
        0x00, 0xA4.toByte(), 0x04, 0x00, 0x07,
        0xA0.toByte(), 0x00, 0x00, 0x00, 0x03, 0x86.toByte(), 0x98.toByte()
    )
    // Authentic Get Balance (CommandAPDU.hhhh): 90 4C 00 00 04
    val JAKCARD_READ_BALANCE = byteArrayOf(
        0x90.toByte(), 0x4C, 0x00, 0x00, 0x04
    )
    val JAKCARD_READ_BALANCE_ISO = byteArrayOf(0x00, 0xB0.toByte(), 0x00, 0x00, 0x04)

    // -------------------------------------------------------------
    // 6. Bank Mega MegaCash (MIFARE DESFire)
    // -------------------------------------------------------------
    // Authentic DESFire Select Application dari eSaldo card-lib (CommandAPDU.mgaa): App ID 00 88 00
    val MEGACASH_SELECT_APP = byteArrayOf(
        0x90.toByte(), 0x5A, 0x00, 0x00, 0x03, 0x00, 0x88.toByte(), 0x00, 0x00
    )
    // Read Info: CommandAPDU.mgbb (Read Data file 01, len 10)
    val MEGACASH_READ_INFO = byteArrayOf(
        0x90.toByte(), 0xBD.toByte(), 0x00, 0x00, 0x07,
        0x01, 0x00, 0x00, 0x00, 0x0A, 0x00, 0x00, 0x00
    )
    // Read Balance: CommandAPDU.mgcc (Get Value)
    val MEGACASH_READ_BALANCE = byteArrayOf(
        0x90.toByte(), 0x6C, 0x00, 0x00, 0x00, 0x00, 0x00
    )
    val MEGACASH_SELECT_AID_ISO = byteArrayOf(
        0x00, 0xA4.toByte(), 0x04, 0x00, 0x07,
        0xA0.toByte(), 0x00, 0x00, 0x03, 0x86.toByte(), 0x00, 0x01
    )
    val MEGACASH_READ_BALANCE_ISO = byteArrayOf(0x00, 0xB0.toByte(), 0x00, 0x00, 0x04)

    /**
     * Mengecek apakah respon APDU berstatus sukses:
     * - 90 00 (Standard ISO 7816-4 Success)
     * - 91 00 (NXP MIFARE DESFire Success)
     * - 61 XX (ISO 7816-4 Normal: Command completed successfully, XX bytes available)
     */
    fun isSuccess(response: ByteArray?): Boolean {
        if (response == null || response.size < 2) return false
        val sw1 = response[response.size - 2].toInt() and 0xFF
        val sw2 = response[response.size - 1].toInt() and 0xFF
        return (sw1 == 0x90 && sw2 == 0x00) || (sw1 == 0x91 && sw2 == 0x00) || (sw1 == 0x61)
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

