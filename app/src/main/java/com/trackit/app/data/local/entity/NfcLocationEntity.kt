package com.trackit.app.data.local.entity

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey

@Keep
@Entity(tableName = "nfc_locations")
data class NfcLocationEntity(
    @PrimaryKey
    val locationId: String,
    val locationName: String,
    val isUserCustom: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)
