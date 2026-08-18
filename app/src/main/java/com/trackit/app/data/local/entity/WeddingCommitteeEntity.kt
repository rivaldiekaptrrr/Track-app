package com.trackit.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "wedding_committee",
    foreignKeys = [ForeignKey(
        entity = WeddingProfileEntity::class,
        parentColumns = ["id"], childColumns = ["weddingProfileId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("weddingProfileId")]
)
data class WeddingCommitteeEntity(
    @PrimaryKey val memberId: String = UUID.randomUUID().toString(),
    val weddingProfileId: String,
    val memberName: String,
    // Peran: Saksi, Sambutan, Doa, Meja Kado, Among Tamu, Suhut, Dongan Tubu, dll
    val role: String,
    // KELUARGA_CPP, KELUARGA_CPW, TEMAN_CPP, TEMAN_CPW
    val side: String = "KELUARGA_CPP",
    val phoneNumber: String? = null,
    // Seragam / kain
    val uniformDescription: String? = null, // Warna/model seragam
    val fabricMeters: Double = 0.0,         // Jatah kain (meter)
    // BELUM_DIBAGI, SEDANG_JAHIT, SIAP_PAKAI
    val uniformStatus: String = "BELUM_DIBAGI",
    val sortOrder: Int = 0
)
