package com.trackit.app.nfc.model

enum class NfcTransactionType {
    PAYMENT,
    TOPUP,
    UNKNOWN
}

data class NfcCardTransaction(
    val type: NfcTransactionType,
    val amount: Long,
    val balanceAfter: Long? = null,
    val timestamp: Long,
    val dateFormatted: String,
    val timeFormatted: String,
    val locationId: String,
    val rawDescription: String? = null
)
