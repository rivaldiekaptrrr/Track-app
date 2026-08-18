package com.trackit.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "wedding_guests",
    foreignKeys = [ForeignKey(
        entity = WeddingProfileEntity::class,
        parentColumns = ["id"], childColumns = ["weddingProfileId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("weddingProfileId")]
)
data class WeddingGuestEntity(
    @PrimaryKey val guestId: String = UUID.randomUUID().toString(),
    val weddingProfileId: String,
    val guestName: String,
    val phoneNumber: String? = null,
    // KELUARGA_CPP, KELUARGA_CPW, TEMAN_CPP, TEMAN_CPW, VIP
    val groupAllocation: String = "TEMAN_CPP",
    // AKAD, RESEPSI, KEDUANYA
    val sessionTarget: String = "KEDUANYA",
    val estimatedPax: Int = 2,
    // PENDING, ATTENDING, DECLINED
    val rsvpStatus: String = "PENDING"
)
