package com.trackit.app.data.local.dao

import androidx.room.*
import com.trackit.app.data.local.entity.CategoryBudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryBudgetDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(budget: CategoryBudgetEntity)

    @Query("SELECT * FROM category_budgets WHERE profileId = :profileId")
    fun getAllBudgets(profileId: Long): Flow<List<CategoryBudgetEntity>>

    @Query("SELECT * FROM category_budgets WHERE categoryId = :categoryId AND profileId = :profileId LIMIT 1")
    suspend fun getBudgetByCategorySync(categoryId: String, profileId: Long): CategoryBudgetEntity?

    @Query("SELECT * FROM category_budgets WHERE categoryId = :categoryId AND profileId = :profileId LIMIT 1")
    fun getBudgetByCategory(categoryId: String, profileId: Long): Flow<CategoryBudgetEntity?>

    @Query("UPDATE category_budgets SET lastWarningMonth = :month WHERE categoryId = :categoryId AND profileId = :profileId")
    suspend fun updateLastWarningMonth(categoryId: String, profileId: Long, month: String)

    @Delete
    suspend fun delete(budget: CategoryBudgetEntity)
}
