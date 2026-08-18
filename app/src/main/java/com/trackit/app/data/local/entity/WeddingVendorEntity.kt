package com.trackit.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "wedding_vendors",
    foreignKeys = [ForeignKey(
        entity = WeddingProfileEntity::class,
        parentColumns = ["id"], childColumns = ["weddingProfileId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("weddingProfileId")]
)
data class WeddingVendorEntity(
    @PrimaryKey val vendorId: String = UUID.randomUUID().toString(),
    val weddingProfileId: String,
    // VENUE, CATERING, DECOR, MUA, DOKUMENTASI, MUSIK, WO, SOUVENIR, LAINNYA
    val category: String,
    val name: String,
    val picName: String? = null,        // Nama contact person
    val phoneNumber: String? = null,
    val instagramHandle: String? = null,
    val contractValue: Double = 0.0,
    val notes: String? = null,
    // PROSPEK, TANDA_JADI, KONTRAK, SELESAI
    val status: String = "PROSPEK",
    val createdAt: Long = System.currentTimeMillis()
)
