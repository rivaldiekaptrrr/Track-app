package com.trackit.app.nfc

import com.trackit.app.nfc.model.EmoneyCardResult
import com.trackit.app.nfc.model.EmoneyCardType
import org.junit.Assert.assertEquals
import org.junit.Test

class EmoneyCardResultTest {

    @Test
    fun testFormattedCardNumber_with16DigitNumber() {
        val card = EmoneyCardResult(
            cardType = EmoneyCardType.MANDIRI_EMONEY,
            cardNumber = "6032123456789821",
            balance = 150000L
        )

        assertEquals("6032 •••• •••• 9821", card.formattedCardNumber())
    }

    @Test
    fun testFormattedCardNumber_withShortNumber() {
        val card = EmoneyCardResult(
            cardType = EmoneyCardType.BCA_FLAZZ,
            cardNumber = "1234",
            balance = 50000L
        )

        assertEquals("1234", card.formattedCardNumber())
    }

    @Test
    fun testFormattedCardNumber_withEmptyNumber() {
        val card = EmoneyCardResult(
            cardType = EmoneyCardType.MEGA_CASH,
            cardNumber = "",
            balance = 25000L
        )

        assertEquals("•••• •••• •••• ••••", card.formattedCardNumber())
    }

    @Test
    fun testCardTypeProperties() {
        assertEquals("Mandiri e-Money", EmoneyCardType.MANDIRI_EMONEY.displayName)
        assertEquals("Bank Mandiri", EmoneyCardType.MANDIRI_EMONEY.bankName)
        assertEquals("BCA Flazz", EmoneyCardType.BCA_FLAZZ.displayName)
        assertEquals("BNI TapCash", EmoneyCardType.BNI_TAPCASH.displayName)
        assertEquals("BRI Brizzi", EmoneyCardType.BRI_BRIZZI.displayName)
        assertEquals("JakCard", EmoneyCardType.DKI_JAKCARD.displayName)
    }
}

