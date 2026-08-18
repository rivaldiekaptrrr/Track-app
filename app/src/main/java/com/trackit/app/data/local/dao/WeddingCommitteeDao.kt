package com.trackit.app.data.local.dao

import androidx.room.*
import com.trackit.app.data.local.entity.WeddingCommitteeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WeddingCommitteeDao {
    @Query("SELECT * FROM wedding_committee WHERE weddingProfileId = :profileId ORDER BY side ASC, sortOrder ASC")
    fun getAllByProfile(profileId: String): Flow<List<WeddingCommitteeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(member: WeddingCommitteeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(members: List<WeddingCommitteeEntity>)

    @Update
    suspend fun update(member: WeddingCommitteeEntity)

    @Delete
    suspend fun delete(member: WeddingCommitteeEntity)
}
