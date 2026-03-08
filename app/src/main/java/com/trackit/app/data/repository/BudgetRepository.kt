package com.trackit.app.data.repository

import com.trackit.app.data.local.dao.BudgetSettingDao
import com.trackit.app.data.local.entity.BudgetSettingEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BudgetRepository @Inject constructor(
    private val budgetSettingDao: BudgetSettingDao
) {
    fun getBudgetSetting(): Flow<BudgetSettingEntity?> =
        budgetSettingDao.getBudgetSetting()

    suspend fun getBudgetSettingSync(): BudgetSettingEntity? =
        budgetSettingDao.getBudgetSettingSync()

    suspend fun saveBudget(budget: Double) {
        val existing = budgetSettingDao.getBudgetSettingSync()
        if (existing != null) {
            budgetSettingDao.updateBudget(budget)
        } else {
            budgetSettingDao.insert(BudgetSettingEntity(monthlyBudget = budget))
        }
    }
}
