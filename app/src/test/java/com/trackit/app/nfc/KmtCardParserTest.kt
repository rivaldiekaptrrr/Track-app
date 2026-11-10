package com.trackit.app.nfc

import com.trackit.app.nfc.model.NfcTransactionType
import com.trackit.app.nfc.parser.KmtCardParser
import com.trackit.app.nfc.parser.KmtStationData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class KmtCardParserTest {

    @Test
    fun testParseCardNumber() {
        val asciiCardNumber = "1001567890123456".toByteArray(Charsets.US_ASCII)
        val result = KmtCardParser.parseCardNumber(asciiCardNumber)
        assertEquals("1001567890123456", result)
    }

    @Test
    fun testParseBalance_littleEndian() {
        // Saldo Rp 50.000 = 0x0000C350
        // Little endian: 0x50, 0xC3, 0x00, 0x00
        val block1 = byteArrayOf(
            0x50.toByte(), 0xC3.toByte(), 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00
        )
        val balance = KmtCardParser.parseBalance(block1)
        assertEquals(50000L, balance)
    }

    @Test
    fun testParseBalance_largeBalance() {
        // Saldo Rp 1.500.000 = 0x0016E360
        // Little endian: 0x60, 0xE3, 0x16, 0x00
        val block1 = byteArrayOf(
            0x60.toByte(), 0xE3.toByte(), 0x16.toByte(), 0x00,
            0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00
        )
        val balance = KmtCardParser.parseBalance(block1)
        assertEquals(1500000L, balance)
    }

    @Test
    fun testBuildReadCommand() {
        val idm = byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08)
        val cmd = KmtCardParser.buildReadCommand(idm, 0x90B7, 2)

        assertEquals(16, cmd.size)
        assertEquals(16.toByte(), cmd[0]) // Length
        assertEquals(0x06.toByte(), cmd[1]) // Read Without Encryption command
        for (i in 0 until 8) {
            assertEquals(idm[i], cmd[2 + i])
        }
        assertEquals(0x01.toByte(), cmd[10]) // 1 service
        assertEquals(0xB7.toByte(), cmd[11]) // Service Code LSB
        assertEquals(0x90.toByte(), cmd[12]) // Service Code MSB
        assertEquals(0x01.toByte(), cmd[13]) // 1 block
        assertEquals(0x80.toByte(), cmd[14]) // 2-byte format, service 0
        assertEquals(2.toByte(), cmd[15])    // Block number
    }

    @Test
    fun testExtractBlockData_success() {
        val fakePayload = ByteArray(16) { (it + 1).toByte() }
        val fakeResponse = ByteArray(29)
        fakeResponse[0] = 29.toByte()
        fakeResponse[1] = 0x07.toByte() // Response command
        fakeResponse[10] = 0x00.toByte() // Status Flag 1 (0 = Success)
        fakeResponse[11] = 0x00.toByte() // Status Flag 2
        fakeResponse[12] = 0x01.toByte() // Block count
        System.arraycopy(fakePayload, 0, fakeResponse, 13, 16)

        val extracted = KmtCardParser.extractBlockData(fakeResponse)
        assertNotNull(extracted)
        assertEquals(16, extracted!!.size)
        assertEquals(1.toByte(), extracted[0])
        assertEquals(16.toByte(), extracted[15])
    }

    @Test
    fun testExtractBlockData_errorStatus() {
        val fakeResponse = ByteArray(29)
        fakeResponse[0] = 29.toByte()
        fakeResponse[1] = 0x07.toByte()
        fakeResponse[10] = 0xFF.toByte() // Status Flag 1 error

        val extracted = KmtCardParser.extractBlockData(fakeResponse)
        assertNull(extracted)
    }

    @Test
    fun testParseHistoryBlock_paymentAtManggarai() {
        val block = ByteArray(16)
        // Detik: 1000000 detik = 0x000F4240 (Big Endian)
        block[0] = 0x00
        block[1] = 0x0F
        block[2] = 0x42
        block[3] = 0x40

        // Nominal: Rp 3.000 = 0x00000BB8 (Big Endian)
        block[4] = 0x00
        block[5] = 0x00
        block[6] = 0x0B
        block[7] = 0xB8.toByte()

        // Kode Stasiun: Manggarai (279 = 0x0117)
        block[8] = 0x01
        block[9] = 0x17

        // Tipe: 1 = Payment
        block[12] = 0x01

        val tx = KmtCardParser.parseHistoryBlock(block)
        assertNotNull(tx)
        assertEquals(NfcTransactionType.PAYMENT, tx!!.type)
        assertEquals(3000L, tx.amount)
        assertEquals("KMT-279", tx.locationId)
        assertEquals("Stasiun KRL Manggarai", tx.rawDescription)
        assertEquals(KmtCardParser.KMT_BASE_EPOCH_MS + (1000000L * 1000L), tx.timestamp)
    }

    @Test
    fun testParseHistoryBlock_topUp() {
        val block = ByteArray(16)
        // Nominal: Rp 50.000 = 0x0000C350 (Big Endian)
        block[4] = 0x00
        block[5] = 0x00
        block[6] = 0xC3.toByte()
        block[7] = 0x50

        // Tipe: 0 = Top Up
        block[12] = 0x00

        val tx = KmtCardParser.parseHistoryBlock(block)
        assertNotNull(tx)
        assertEquals(NfcTransactionType.TOPUP, tx!!.type)
        assertEquals(50000L, tx.amount)
    }

    @Test
    fun testParseHistoryBlock_emptyBlock() {
        val emptyBlock = ByteArray(16)
        val tx = KmtCardParser.parseHistoryBlock(emptyBlock)
        assertNull(tx)
    }

    @Test
    fun testKmtStationData() {
        assertEquals("Stasiun KRL Tanah Abang", KmtStationData.getStationName(1))
        assertEquals("Stasiun KRL Tanah Abang", KmtStationData.getStationName(329))
        assertEquals("Stasiun KRL Bogor", KmtStationData.getStationName(257))
        assertEquals("Stasiun KRL Manggarai", KmtStationData.getStationName(279))
        assertEquals("Stasiun KRL Tangerang", KmtStationData.getStationName(306))
        assertEquals("Stasiun KRL Tangerang", KmtStationData.getStationName(313))
        assertEquals("Stasiun KRL Cikarang", KmtStationData.getStationName(376))
        assertNull(KmtStationData.getStationName(9999))
    }
}

