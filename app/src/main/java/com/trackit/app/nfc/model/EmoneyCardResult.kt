package com.trackit.app.nfc.model

data class EmoneyCardResult(
    val cardType: EmoneyCardType,
    val cardNumber: String,
    val balance: Long,
    val isChipG2: Boolean = false,
    val readTimestamp: Long = System.currentTimeMillis()
) {
    /**
     * Memformat nomor kartu dengan masking: 6032 •••• •••• 1234
     */
    fun formattedCardNumber(): String {
        val clean = cardNumber.replace(Regex("[^0-9A-Fa-f]"), "")
        if (clean.length < 8) return cardNumber.ifEmpty { "•••• •••• •••• ••••" }
        val prefix = clean.take(4)
        val suffix = clean.takeLast(4)
        return "$prefix •••• •••• $suffix"
    }
}

