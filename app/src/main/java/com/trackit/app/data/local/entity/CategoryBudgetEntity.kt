package com.trackit.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "category_budgets",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class CategoryBudgetEntity(
    @PrimaryKey
    val categoryId: String,
    val amount: Double,
    @ColumnInfo(defaultValue = "0.9")
    val alertPercentage: Float = 0.9f,
    @ColumnInfo(defaultValue = "''")
    val lastWarningMonth: String = "", // Format: "YYYY-MM"
    @ColumnInfo(defaultValue = "1")
    val profileId: Long = 1
)
