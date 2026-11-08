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

