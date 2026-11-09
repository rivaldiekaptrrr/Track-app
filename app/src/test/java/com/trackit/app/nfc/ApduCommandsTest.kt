package com.trackit.app.nfc

import com.trackit.app.nfc.parser.ApduCommands
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ApduCommandsTest {

    @Test
    fun testIsSuccess_standard9000() {
        val successResponse = byteArrayOf(0x01, 0x02, 0x90.toByte(), 0x00)
        assertTrue(ApduCommands.isSuccess(successResponse))
    }

    @Test
    fun testIsSuccess_desfire9100() {
        val desfireResponse = byteArrayOf(0x10, 0x20, 0x91.toByte(), 0x00)
        assertTrue(ApduCommands.isSuccess(desfireResponse))
    }

    @Test
    fun testIsSuccess_iso7816_61xx() {
        // Status code 61 XX berarti perintah sukses dieksekusi dan terdapat XX byte respon tersedia
        val response6118 = byteArrayOf(0x61.toByte(), 0x18)
        assertTrue(ApduCommands.isSuccess(response6118))

        val responsePayloadWith6120 = byteArrayOf(0x01, 0x02, 0x61.toByte(), 0x20)
        assertTrue(ApduCommands.isSuccess(responsePayloadWith6120))
    }

    @Test
    fun testMandiriCommands() {
        assertEquals("00A4040008A000000003000000", ApduCommands.toHexString(ApduCommands.MANDIRI_SELECT_AID))
        assertEquals("00B000001D", ApduCommands.toHexString(ApduCommands.MANDIRI_READ_INFO))
        assertEquals("00B300003F", ApduCommands.toHexString(ApduCommands.MANDIRI_READ_INFO_RECORD))
        assertEquals("00B0000004", ApduCommands.toHexString(ApduCommands.MANDIRI_READ_BALANCE))
        assertEquals("00B500000A", ApduCommands.toHexString(ApduCommands.MANDIRI_READ_BALANCE_SV))
        assertEquals("00B200001E", ApduCommands.toHexString(ApduCommands.MANDIRI_READ_HISTORY_G1))
        assertEquals("00D1000000", ApduCommands.toHexString(ApduCommands.MANDIRI_READ_HISTORY_G2))
    }

    @Test
    fun testTapcashCommands() {
        assertEquals("00A4040008A000424E49100001", ApduCommands.toHexString(ApduCommands.TAPCASH_SELECT_AID_ISO))
        assertEquals("905A00000399999900", ApduCommands.toHexString(ApduCommands.TAPCASH_SELECT_APP))
        assertEquals("90BD0000070100000029000000", ApduCommands.toHexString(ApduCommands.TAPCASH_READ_FILE))
        assertEquals("9032030001", ApduCommands.toHexString(ApduCommands.TAPCASH_READ_HISTORY_HEADER))
    }

    @Test
    fun testBrizziCommands() {
        assertEquals("905A00000301000000", ApduCommands.toHexString(ApduCommands.BRIZZI_SELECT_APP))
        assertEquals("90BD0000070100000016000000", ApduCommands.toHexString(ApduCommands.BRIZZI_READ_INFO))
        assertEquals("906C0000000000", ApduCommands.toHexString(ApduCommands.BRIZZI_READ_BALANCE_GET_VALUE))
        assertEquals("90BB0000070100000000000000", ApduCommands.toHexString(ApduCommands.BRIZZI_READ_HISTORY_INIT))
        assertEquals("90AF000000", ApduCommands.toHexString(ApduCommands.BRIZZI_READ_HISTORY_NEXT))
    }

    @Test
    fun testFlazzHistoryCommands() {
        assertEquals("00B0810000", ApduCommands.toHexString(ApduCommands.FLAZZ_CHECK_HISTORY))
        assertEquals("0084000008", ApduCommands.toHexString(ApduCommands.FLAZZ_GET_CHALLENGE))
        assertEquals("00B084003C", ApduCommands.toHexString(ApduCommands.FLAZZ_READ_RECORD_SFI4))
        assertEquals("00B085003C", ApduCommands.toHexString(ApduCommands.FLAZZ_READ_RECORD_SFI5))
    }

    @Test
    fun testJakcardCommands() {
        assertEquals("00A4040008A0000005714E4A43", ApduCommands.toHexString(ApduCommands.JAKCARD_SELECT_APP))
        assertEquals("904C000004", ApduCommands.toHexString(ApduCommands.JAKCARD_READ_BALANCE))
    }

    @Test
    fun testMegacashCommands() {
        assertEquals("905A00000300880000", ApduCommands.toHexString(ApduCommands.MEGACASH_SELECT_APP))
        assertEquals("90BD000007010000000A000000", ApduCommands.toHexString(ApduCommands.MEGACASH_READ_INFO))
        assertEquals("906C0000000000", ApduCommands.toHexString(ApduCommands.MEGACASH_READ_BALANCE))
    }

    @Test
    fun testFlazzCommands() {
        // Authentic Flazz Gen 2 AID: 11 bytes A0000000180F0000018001
        assertEquals("00A404000BA0000000180F0000018001", ApduCommands.toHexString(ApduCommands.FLAZZ_SELECT_AID))
        // Select File EF 0200
        assertEquals("00A40100020200", ApduCommands.toHexString(ApduCommands.FLAZZ_SELECT_FILE))
        // Read Record
        assertEquals("00B081008E", ApduCommands.toHexString(ApduCommands.FLAZZ_READ_RECORD))
        // Get Balance
        assertEquals("803200030400000000", ApduCommands.toHexString(ApduCommands.FLAZZ_GET_BALANCE))

        // Fallback AIDs
        assertEquals("00A4040008A000000018434154", ApduCommands.toHexString(ApduCommands.FLAZZ_SELECT_AID_CAT))
        assertEquals("00A4040007A0000000031010", ApduCommands.toHexString(ApduCommands.FLAZZ_SELECT_AID_PBOC))
    }

    @Test
    fun testIsSuccess_errorCodes() {
        val error1 = byteArrayOf(0x6A.toByte(), 0x82.toByte())
        assertFalse(ApduCommands.isSuccess(error1))

        val error2 = byteArrayOf(0x90.toByte(), 0x01)
        assertFalse(ApduCommands.isSuccess(error2))

        val singleByte = byteArrayOf(0x90.toByte())
        assertFalse(ApduCommands.isSuccess(singleByte))

        assertFalse(ApduCommands.isSuccess(null))
    }

    @Test
    fun testToHexString() {
        val bytes = byteArrayOf(0x0A, 0xFF.toByte(), 0x00, 0x3B)
        assertEquals("0AFF003B", ApduCommands.toHexString(bytes))
        assertEquals("FF00", ApduCommands.toHexString(bytes, 1, 2))
    }
}

