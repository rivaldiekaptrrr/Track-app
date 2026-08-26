package com.trackit.app.data.repository

import com.trackit.app.data.local.dao.CategoryDao
import com.trackit.app.data.local.entity.CategoryEntity
import com.trackit.app.util.SyncManager
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepository @Inject constructor(
    private val categoryDao: CategoryDao,
    private val syncManager: SyncManager
) {
    fun getAllCategories(profileId: Long): Flow<List<CategoryEntity>> =
        categoryDao.getAllCategories(profileId)

    suspend fun getById(id: String): CategoryEntity? =
        categoryDao.getById(id)

    suspend fun getCategoryByIdSync(id: String): CategoryEntity? =
        categoryDao.getById(id)

    suspend fun insert(category: CategoryEntity): String {
        categoryDao.insert(category)
        syncManager.pushCategory(category)
        return category.id
    }

    suspend fun insertAll(categories: List<CategoryEntity>) {
        categoryDao.insertAll(categories)
        categories.forEach { syncManager.pushCategory(it) }
    }

    suspend fun update(category: CategoryEntity) {
        categoryDao.update(category)
        syncManager.pushCategory(category)
    }

    suspend fun delete(category: CategoryEntity) {
        categoryDao.delete(category)
        syncManager.deleteCategory(category)
    }

    suspend fun getCount(): Int =
        categoryDao.getCount()

    suspend fun getCountForProfile(profileId: Long): Int =
        categoryDao.getCountForProfile(profileId)
}
