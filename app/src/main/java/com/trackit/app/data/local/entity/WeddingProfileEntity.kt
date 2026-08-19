package com.trackit.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "wedding_profiles")
data class WeddingProfileEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val groomName: String,
    val brideName: String,
    val weddingDate: Long,
    val totalBudgetCap: Double = 0.0,
    // ISLAM (KUA) / NON_ISLAM (Dukcapil)
    val religionType: String = "ISLAM",
    // ISLAM, KRISTEN, KATOLIK, HINDU, BUDDHA, KONGHUCU, LAINNYA
    val religionDetail: String? = null,
    // JAWA, SUNDA, BATAK, MINANG, BUGIS_MAKASSAR, BALI, BETAWI, TIONGHOA, MODERN, LAINNYA
    val culturalPresetGroom: String? = null,
    val culturalPresetBride: String? = null,
    val quote: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
