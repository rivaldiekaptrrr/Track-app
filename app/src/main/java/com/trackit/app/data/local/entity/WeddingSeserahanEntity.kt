package com.trackit.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "wedding_seserahan",
    foreignKeys = [ForeignKey(
        entity = WeddingProfileEntity::class,
        parentColumns = ["id"], childColumns = ["weddingProfileId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("weddingProfileId")]
)
data class WeddingSeserahanEntity(
    @PrimaryKey val itemId: String = UUID.randomUUID().toString(),
    val weddingProfileId: String,
    // SESERAHAN_CPP (CPP→CPW), BALASAN_CPW (CPW→CPP), MAHAR
    val direction: String,
    val itemName: String,
    val quantity: Int = 1,
    val estimatedPrice: Double = 0.0,
    // BELUM_BELI, DIBELI, WRAPPING, SIAP
    val status: String = "BELUM_BELI",
    val notes: String? = null,
    val sortOrder: Int = 0
)
