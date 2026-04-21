package com.trackit.app.data.local.dao

import androidx.room.*
import com.trackit.app.data.local.entity.BudgetSettingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetSettingDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(setting: BudgetSettingEntity)

    @Query("SELECT * FROM budget_settings WHERE profileId = :profileId")
    fun getBudgetSetting(profileId: Long): Flow<BudgetSettingEntity?>

    @Query("SELECT * FROM budget_settings WHERE profileId = :profileId")
    suspend fun getBudgetSettingSync(profileId: Long): BudgetSettingEntity?

    @Query("UPDATE budget_settings SET monthlyBudget = :budget WHERE profileId = :profileId")
    suspend fun updateBudget(profileId: Long, budget: Double)
}
