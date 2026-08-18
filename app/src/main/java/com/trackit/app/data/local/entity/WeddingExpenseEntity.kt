package com.trackit.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "wedding_expenses",
    foreignKeys = [ForeignKey(
        entity = WeddingProfileEntity::class,
        parentColumns = ["id"], childColumns = ["weddingProfileId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("weddingProfileId")]
)
data class WeddingExpenseEntity(
    @PrimaryKey val expenseId: String = UUID.randomUUID().toString(),
    val weddingProfileId: String,
    // VENUE, CATERING, DECOR, MUA, DOKUMENTASI, SESERAHAN, UNDANGAN, LAINNYA
    val category: String,
    val title: String,
    val totalEstimated: Double,
    val totalPaid: Double = 0.0,
    // TABUNGAN_CPP, TABUNGAN_CPW, ORTU_CPP, ORTU_CPW, BERSAMA
    val paidBySource: String = "BERSAMA",
    // UNPAID, PARTIAL_DP, FULLY_PAID
    val paymentStatus: String = "UNPAID",
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
