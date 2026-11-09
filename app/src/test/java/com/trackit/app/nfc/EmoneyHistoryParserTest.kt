package com.trackit.app.nfc

import com.trackit.app.nfc.model.NfcTransactionType
import com.trackit.app.nfc.parser.EmoneyHistoryParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EmoneyHistoryParserTest {

    @Test
    fun testParseFlazzHistory() {
        // Blok Flazz 120 hex chars (60 bytes)
        // 0..4: 0400 (Payment)
        // 4..12: padding 8 char
        // 12..18: 000FA0 (Rp 4.000)
        // 18..60: padding 42 char
        // 60..76: ASCII "STASIUN " dalam hex -> 5354415349554E20 (16 char)
        // 76..84: timestamp detik hex -> 54000000 (8 char)
        // 84..120: padding 36 char
        val block = StringBuilder()
        block.append("0400")                           // 4
        block.append("00000000")                       // 8 -> 12
        block.append("000FA0")                         // 6 -> 18
        block.append("0".repeat(42))                   // 42 -> 60
        block.append("5354415349554E20")               // 16 -> 76 ("STASIUN ")
        block.append("54000000")                       // 8 -> 84
        block.append("0".repeat(36))                   // 36 -> 120

        val list = EmoneyHistoryParser.parseFlazzHistory(block.toString())
        assertEquals(1, list.size)
        val tx = list[0]
        assertEquals(4000L, tx.amount)
        assertEquals(NfcTransactionType.PAYMENT, tx.type)
        assertEquals("STASIUN", tx.locationId)
        assertTrue(tx.dateFormatted.isNotBlank())
        assertTrue(tx.timeFormatted.isNotBlank())
    }

    @Test
    fun testParseFlazzGen2History() {
        // Blok Flazz Gen 2: 64 hex chars (32 bytes)
        // 0..2: 04 (Payment)
        // 2..8: raw 24-bit amount -> 16777216 - 4000 = 16773216 = 0xFFF060
        // 8..16: timestamp sec hex -> 54000000
        // 16..28: padding 12 char
        // 28..44: ASCII "0012A4F1" dalam hex -> 3030313241344631 (16 char)
        // 44..50: sisa saldo hex -> 000064
        // 50..64: padding 14 char
        val block = "04" + "FFF060" + "54000000" + "0".repeat(12) + "3030313241344631" + "000064" + "0".repeat(14)
        assertEquals(64, block.length)

        val list = EmoneyHistoryParser.parseFlazzGen2History(block)
        assertEquals(1, list.size)
        val tx = list[0]
        assertEquals(4000L, tx.amount)
        assertEquals(NfcTransactionType.PAYMENT, tx.type)
        assertEquals("0012A4F1", tx.locationId)
        assertTrue(tx.dateFormatted.isNotBlank())
        assertTrue(tx.timeFormatted.isNotBlank())
    }

    @Test
    fun testParseMandiriG2History() {
        // Blok Mandiri G2: 48 hex chars
        // 0..12: ddMMyyHHmmss -> 091126083000 (09 Nov 2026 08:30:00)
        // 12..20: Location ID -> 0012A4F1
        // 20..28: padding 8 char
        // 28..32: procCode -> 0001 (Payment)
        // 32..40: amount little-endian -> A0860100 (100.000)
        // 40..48: balanceAfter little-endian -> E0930400 (300.000)
        val block = "091126083000" + "0012A4F1" + "00000000" + "0001" + "A0860100" + "E0930400"
        assertEquals(48, block.length)

        val list = EmoneyHistoryParser.parseMandiriG2History(block)
        assertEquals(1, list.size)
        val tx = list[0]
        assertEquals(100000L, tx.amount)
        assertEquals(300000L, tx.balanceAfter)
        assertEquals("0012A4F1", tx.locationId)
        assertEquals(NfcTransactionType.PAYMENT, tx.type)
        assertEquals("08:30", tx.timeFormatted)
    }

    @Test
    fun testParseTapcashHistory() {
        // 16 bytes per record
        // [0]: 1 (PAYMENT)
        // [1..3]: 0x00, 0x0F, 0xA0 (4000)
        // [4..7]: 0x35, 0x00, 0x00, 0x00
        val bytes = ByteArray(16)
        bytes[0] = 1
        bytes[1] = 0x00
        bytes[2] = 0x0F
        bytes[3] = 0xA0.toByte()
        bytes[4] = 0x35
        bytes[5] = 0x00
        bytes[6] = 0x00
        bytes[7] = 0x00

        val list = EmoneyHistoryParser.parseTapcashHistory(bytes)
        assertEquals(1, list.size)
        val tx = list[0]
        assertEquals(4000L, tx.amount)
        assertEquals(NfcTransactionType.PAYMENT, tx.type)
    }

    @Test
    fun testParseBrizziHistory() {
        // Blok Brizzi 64 hex chars
        // 0..8: amount little-endian -> A00F0000 (4.000)
        // 8..32: padding 24 char
        // 32..38: tanggal ddMMyy -> 091126
        // 38..42: jam HHmm -> 0830
        // 42..44: padding 2 char
        // 44..46: procCode -> 01 (Payment)
        // 46..64: padding 18 char
        val block = "A00F0000" + "0".repeat(24) + "091126" + "0830" + "00" + "01" + "0".repeat(18)
        assertEquals(64, block.length)

        val list = EmoneyHistoryParser.parseBrizziHistory(block)
        assertEquals(1, list.size)
        val tx = list[0]
        assertEquals(4000L, tx.amount)
        assertEquals(NfcTransactionType.PAYMENT, tx.type)
        assertEquals("08:30", tx.timeFormatted)
    }
}
