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

