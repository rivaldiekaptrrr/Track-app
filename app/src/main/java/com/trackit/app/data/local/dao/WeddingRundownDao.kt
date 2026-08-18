package com.trackit.app.data.local.dao

import androidx.room.*
import com.trackit.app.data.local.entity.WeddingEventEntity
import com.trackit.app.data.local.entity.WeddingRundownItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WeddingEventDao {
    @Query("SELECT * FROM wedding_events WHERE weddingProfileId = :profileId ORDER BY sortOrder ASC, eventDate ASC")
    fun getAllByProfile(profileId: String): Flow<List<WeddingEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: WeddingEventEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<WeddingEventEntity>)

    @Update
    suspend fun update(event: WeddingEventEntity)

    @Delete
    suspend fun delete(event: WeddingEventEntity)
}

@Dao
interface WeddingRundownItemDao {
    @Query("SELECT * FROM wedding_rundown_items WHERE eventId = :eventId ORDER BY sortOrder ASC, timeStart ASC")
    fun getByEvent(eventId: String): Flow<List<WeddingRundownItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: WeddingRundownItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<WeddingRundownItemEntity>)

    @Update
    suspend fun update(item: WeddingRundownItemEntity)

    @Delete
    suspend fun delete(item: WeddingRundownItemEntity)

    @Query("DELETE FROM wedding_rundown_items WHERE eventId = :eventId")
    suspend fun deleteAllByEvent(eventId: String)
}
