package com.trackit.app.data.local.dao

import androidx.room.*
import com.trackit.app.data.local.entity.WeddingSeserahanEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WeddingSeserahanDao {
    @Query("SELECT * FROM wedding_seserahan WHERE weddingProfileId = :profileId ORDER BY direction ASC, sortOrder ASC")
    fun getAllByProfile(profileId: String): Flow<List<WeddingSeserahanEntity>>

    @Query("SELECT * FROM wedding_seserahan WHERE weddingProfileId = :profileId AND direction = :direction ORDER BY sortOrder ASC")
    fun getByDirection(profileId: String, direction: String): Flow<List<WeddingSeserahanEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: WeddingSeserahanEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<WeddingSeserahanEntity>)

    @Update
    suspend fun update(item: WeddingSeserahanEntity)

    @Delete
    suspend fun delete(item: WeddingSeserahanEntity)
}
