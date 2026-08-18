package com.trackit.app.data.local.dao

import androidx.room.*
import com.trackit.app.data.local.entity.WeddingExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WeddingExpenseDao {
    @Query("SELECT * FROM wedding_expenses WHERE weddingProfileId = :profileId ORDER BY createdAt DESC")
    fun getAllByProfile(profileId: String): Flow<List<WeddingExpenseEntity>>

    @Query("SELECT SUM(totalEstimated) FROM wedding_expenses WHERE weddingProfileId = :profileId")
    fun getTotalEstimated(profileId: String): Flow<Double?>

    @Query("SELECT SUM(totalPaid) FROM wedding_expenses WHERE weddingProfileId = :profileId")
    fun getTotalPaid(profileId: String): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expense: WeddingExpenseEntity)

    @Update
    suspend fun update(expense: WeddingExpenseEntity)

    @Delete
    suspend fun delete(expense: WeddingExpenseEntity)
}
