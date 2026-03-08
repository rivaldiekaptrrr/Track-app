package com.trackit.app.data.local.dao

import androidx.room.*
import com.trackit.app.data.local.entity.BudgetSettingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetSettingDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(setting: BudgetSettingEntity)

    @Query("SELECT * FROM budget_settings WHERE id = 1")
    fun getBudgetSetting(): Flow<BudgetSettingEntity?>

    @Query("SELECT * FROM budget_settings WHERE id = 1")
    suspend fun getBudgetSettingSync(): BudgetSettingEntity?

    @Query("UPDATE budget_settings SET monthlyBudget = :budget WHERE id = 1")
    suspend fun updateBudget(budget: Double)
}
