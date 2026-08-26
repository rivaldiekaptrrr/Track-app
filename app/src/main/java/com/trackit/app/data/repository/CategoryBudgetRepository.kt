package com.trackit.app.data.repository

import com.trackit.app.data.local.dao.CategoryBudgetDao
import com.trackit.app.data.local.entity.CategoryBudgetEntity
import com.trackit.app.util.SyncManager
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryBudgetRepository @Inject constructor(
    private val categoryBudgetDao: CategoryBudgetDao,
    private val syncManager: SyncManager
) {
    fun getAllBudgets(profileId: Long): Flow<List<CategoryBudgetEntity>> {
        return categoryBudgetDao.getAllBudgets(profileId)
    }

    suspend fun getBudgetByCategorySync(categoryId: String, profileId: Long): CategoryBudgetEntity? {
        return categoryBudgetDao.getBudgetByCategorySync(categoryId, profileId)
    }

    fun getBudgetByCategory(categoryId: String, profileId: Long): Flow<CategoryBudgetEntity?> {
        return categoryBudgetDao.getBudgetByCategory(categoryId, profileId)
    }

    suspend fun saveBudget(budget: CategoryBudgetEntity) {
        categoryBudgetDao.insert(budget)
        syncManager.pushCategoryBudget(budget)
    }

    suspend fun deleteBudget(budget: CategoryBudgetEntity) {
        categoryBudgetDao.delete(budget)
        syncManager.deleteCategoryBudget(budget)
    }

    suspend fun updateLastWarningMonth(categoryId: String, profileId: Long, month: String) {
        categoryBudgetDao.updateLastWarningMonth(categoryId, profileId, month)
    }
}
