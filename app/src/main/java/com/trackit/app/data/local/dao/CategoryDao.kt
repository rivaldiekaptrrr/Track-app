package com.trackit.app.data.local.dao

import androidx.room.*
import com.trackit.app.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Update
    suspend fun update(category: CategoryEntity)

    @Delete
    suspend fun delete(category: CategoryEntity)

    @Query("SELECT * FROM categories WHERE profileId = :profileId ORDER BY name ASC")
    fun getAllCategories(profileId: Long): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: String): CategoryEntity?

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun getCount(): Int

    @Query("SELECT COUNT(*) FROM categories WHERE profileId = :profileId")
    suspend fun getCountForProfile(profileId: Long): Int

    @Query("SELECT * FROM categories WHERE name = :name AND profileId = :profileId LIMIT 1")
    suspend fun getByNameAndProfile(name: String, profileId: Long): CategoryEntity?

    @Query("SELECT * FROM categories")
    suspend fun getAllCategoriesSync(): List<CategoryEntity>
}
