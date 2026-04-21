package com.trackit.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budget_settings")
data class BudgetSettingEntity(
    @PrimaryKey
    val profileId: Long = 1,
    val monthlyBudget: Double = 0.0
)
