package com.trackit.app.nfc

import com.trackit.app.data.local.entity.CategoryEntity
import com.trackit.app.nfc.categorization.NfcKeywordMatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NfcKeywordMatcherTest {

    @Test
    fun testExtractSmartKeyword_stasiun() {
        assertEquals("Stasiun", NfcKeywordMatcher.extractSmartKeyword("Stasiun KRL Tangerang"))
        assertEquals("Stasiun", NfcKeywordMatcher.extractSmartKeyword("stasiun manggarai"))
    }

    @Test
    fun testExtractSmartKeyword_tol() {
        assertEquals("Tol", NfcKeywordMatcher.extractSmartKeyword("Gerbang Tol Pasteur"))
        assertEquals("Tol", NfcKeywordMatcher.extractSmartKeyword("Tol Jagorawi"))
    }

    @Test
    fun testExtractSmartKeyword_halte() {
        assertEquals("Halte", NfcKeywordMatcher.extractSmartKeyword("Halte Transjakarta Tosari"))
    }

    @Test
    fun testExtractSmartKeyword_fallbackFirstWord() {
        assertEquals("Parkir", NfcKeywordMatcher.extractSmartKeyword("Parkir Mall Grand Indonesia"))
        assertEquals("Kantin", NfcKeywordMatcher.extractSmartKeyword("Kantin Pusat"))
    }

    @Test
    fun testMatchCategory_foundInCustomKeywords() {
        val catTransport = CategoryEntity(
            id = "cat_transport",
            name = "Transportasi",
            iconName = "directions_car",
            colorHex = "#123456",
            customKeywords = "kereta, bensin, stasiun, grab, gojek"
        )
        val catFood = CategoryEntity(
            id = "cat_food",
            name = "Makanan",
            iconName = "restaurant",
            colorHex = "#654321",
            customKeywords = "nasi, kopi"
        )

        val matched = NfcKeywordMatcher.matchCategory(
            locationName = "Stasiun KRL Tangerang",
            categories = listOf(catFood, catTransport)
        )

        assertNotNull(matched)
        assertEquals("cat_transport", matched?.id)
        assertEquals("Transportasi", matched?.name)
    }

    @Test
    fun testMatchCategory_notFound() {
        val catTransport = CategoryEntity(
            id = "cat_transport",
            name = "Transportasi",
            iconName = "directions_car",
            colorHex = "#123456",
            customKeywords = "kereta, bensin"
        )

        val matched = NfcKeywordMatcher.matchCategory(
            locationName = "Gerbang Tol Pasteur",
            categories = listOf(catTransport)
        )

        assertNull(matched)
    }

    @Test
    fun testAppendKeywordToCategory() {
        val catTransport = CategoryEntity(
            id = "cat_transport",
            name = "Transportasi",
            iconName = "directions_car",
            colorHex = "#123456",
            customKeywords = "kereta, bensin"
        )

        val updated = NfcKeywordMatcher.appendKeywordToCategory(catTransport, "Stasiun")
        assertEquals("kereta, bensin, Stasiun", updated.customKeywords)

        // Idempotent jika kata kunci sudah ada (case-insensitive)
        val notDuplicated = NfcKeywordMatcher.appendKeywordToCategory(updated, "stasiun")
        assertEquals("kereta, bensin, Stasiun", notDuplicated.customKeywords)
    }
}

