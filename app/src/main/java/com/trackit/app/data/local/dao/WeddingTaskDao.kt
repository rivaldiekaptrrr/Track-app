package com.trackit.app.data.local.dao

import androidx.room.*
import com.trackit.app.data.local.entity.WeddingTaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WeddingTaskDao {
    @Query("SELECT * FROM wedding_tasks WHERE weddingProfileId = :profileId ORDER BY phaseMonth DESC, sortOrder ASC")
    fun getAllByProfile(profileId: String): Flow<List<WeddingTaskEntity>>

    @Query("SELECT * FROM wedding_tasks WHERE weddingProfileId = :profileId AND isCompleted = 0 ORDER BY dueDate ASC LIMIT 5")
    fun getUpcomingTasks(profileId: String): Flow<List<WeddingTaskEntity>>

    @Query("SELECT COUNT(*) FROM wedding_tasks WHERE weddingProfileId = :profileId")
    fun getTotalCount(profileId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM wedding_tasks WHERE weddingProfileId = :profileId AND isCompleted = 1")
    fun getCompletedCount(profileId: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: WeddingTaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<WeddingTaskEntity>)

    @Update
    suspend fun update(task: WeddingTaskEntity)

    @Delete
    suspend fun delete(task: WeddingTaskEntity)

    @Query("DELETE FROM wedding_tasks WHERE weddingProfileId = :profileId")
    suspend fun deleteAllByProfile(profileId: String)
}
