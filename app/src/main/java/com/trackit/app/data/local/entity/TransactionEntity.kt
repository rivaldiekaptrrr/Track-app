package com.trackit.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["categoryId"])]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,
    val description: String,
    val categoryId: Long?,
    val date: Long, // epoch millis
    val createdAt: Long = System.currentTimeMillis(),
    val isRecurring: Boolean = false,
    val recurringType: String? = null, // "DAILY", "WEEKLY", "MONTHLY"
    val recurringDayOfMonth: Int? = null,
    @ColumnInfo(defaultValue = "EXPENSE")
    val type: String = "EXPENSE" // "EXPENSE" or "INCOME"
)
