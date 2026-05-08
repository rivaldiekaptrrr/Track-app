package com.trackit.app.data.repository

import com.trackit.app.data.local.dao.CategoryBudgetDao
import com.trackit.app.data.local.entity.CategoryBudgetEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryBudgetRepository @Inject constructor(
    private val categoryBudgetDao: CategoryBudgetDao
) {
    fun getAllBudgets(profileId: Long): Flow<List<CategoryBudgetEntity>> {
        return categoryBudgetDao.getAllBudgets(profileId)
    }

    suspend fun getBudgetByCategorySync(categoryId: Long, profileId: Long): CategoryBudgetEntity? {
        return categoryBudgetDao.getBudgetByCategorySync(categoryId, profileId)
    }

    fun getBudgetByCategory(categoryId: Long, profileId: Long): Flow<CategoryBudgetEntity?> {
        return categoryBudgetDao.getBudgetByCategory(categoryId, profileId)
    }

    suspend fun saveBudget(budget: CategoryBudgetEntity) {
        categoryBudgetDao.insert(budget)
    }

    suspend fun deleteBudget(budget: CategoryBudgetEntity) {
        categoryBudgetDao.delete(budget)
    }
    
    suspend fun updateLastWarningMonth(categoryId: Long, profileId: Long, month: String) {
        categoryBudgetDao.updateLastWarningMonth(categoryId, profileId, month)
    }
}
