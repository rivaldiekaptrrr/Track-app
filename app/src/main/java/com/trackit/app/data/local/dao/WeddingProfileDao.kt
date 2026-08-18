package com.trackit.app.data.local.dao

import androidx.room.*
import com.trackit.app.data.local.entity.WeddingProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WeddingProfileDao {
    @Query("SELECT * FROM wedding_profiles WHERE id = :id")
    fun getById(id: String): Flow<WeddingProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: WeddingProfileEntity): Long

    @Update
    suspend fun update(profile: WeddingProfileEntity)

    @Delete
    suspend fun delete(profile: WeddingProfileEntity)
}
