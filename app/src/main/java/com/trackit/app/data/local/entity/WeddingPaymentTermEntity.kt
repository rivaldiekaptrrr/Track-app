package com.trackit.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "wedding_payment_terms",
    foreignKeys = [ForeignKey(
        entity = WeddingExpenseEntity::class,
        parentColumns = ["expenseId"], childColumns = ["expenseId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("expenseId")]
)
data class WeddingPaymentTermEntity(
    @PrimaryKey val termId: String = UUID.randomUUID().toString(),
    val expenseId: String,
    val termName: String, // "DP 1", "DP 2", "Pelunasan"
    val amount: Double,
    val dueDate: Long,
    val isPaid: Boolean = false,
    val paidDate: Long? = null
)
