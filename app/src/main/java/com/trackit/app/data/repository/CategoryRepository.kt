package com.trackit.app.data.repository

import com.trackit.app.data.local.dao.CategoryDao
import com.trackit.app.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepository @Inject constructor(
    private val categoryDao: CategoryDao
) {
    fun getAllCategories(profileId: Long): Flow<List<CategoryEntity>> =
        categoryDao.getAllCategories(profileId)

    suspend fun getById(id: Long): CategoryEntity? =
        categoryDao.getById(id)

    suspend fun insert(category: CategoryEntity): Long =
        categoryDao.insert(category)

    suspend fun insertAll(categories: List<CategoryEntity>) =
        categoryDao.insertAll(categories)

    suspend fun update(category: CategoryEntity) =
        categoryDao.update(category)

    suspend fun delete(category: CategoryEntity) =
        categoryDao.delete(category)

    suspend fun getCount(): Int =
        categoryDao.getCount()

    suspend fun getCountForProfile(profileId: Long): Int =
        categoryDao.getCountForProfile(profileId)
}
