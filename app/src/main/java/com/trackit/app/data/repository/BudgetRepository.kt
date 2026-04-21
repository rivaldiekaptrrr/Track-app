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
    fun getBudgetSetting(profileId: Long): Flow<BudgetSettingEntity?> =
        budgetSettingDao.getBudgetSetting(profileId)

    suspend fun getBudgetSettingSync(profileId: Long): BudgetSettingEntity? =
        budgetSettingDao.getBudgetSettingSync(profileId)

    suspend fun saveBudget(profileId: Long, budget: Double) {
        val existing = budgetSettingDao.getBudgetSettingSync(profileId)
        if (existing != null) {
            budgetSettingDao.updateBudget(profileId, budget)
        } else {
            budgetSettingDao.insert(BudgetSettingEntity(profileId = profileId, monthlyBudget = budget))
        }
    }
}
