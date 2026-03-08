package com.trackit.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budget_settings")
data class BudgetSettingEntity(
    @PrimaryKey
    val id: Int = 1, // singleton row
    val monthlyBudget: Double = 0.0
)
