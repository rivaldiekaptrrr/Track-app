package com.trackit.app.data.local.dao

import androidx.room.*
import com.trackit.app.data.local.entity.WeddingGuestEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WeddingGuestDao {
    @Query("SELECT * FROM wedding_guests WHERE weddingProfileId = :profileId ORDER BY guestName ASC")
    fun getAllByProfile(profileId: String): Flow<List<WeddingGuestEntity>>

    @Query("SELECT COUNT(*) FROM wedding_guests WHERE weddingProfileId = :profileId")
    fun getTotalCount(profileId: String): Flow<Int>

    @Query("SELECT SUM(estimatedPax) FROM wedding_guests WHERE weddingProfileId = :profileId")
    fun getTotalPax(profileId: String): Flow<Int?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(guest: WeddingGuestEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(guests: List<WeddingGuestEntity>)

    @Update
    suspend fun update(guest: WeddingGuestEntity)

    @Delete
    suspend fun delete(guest: WeddingGuestEntity)

    @Query("UPDATE wedding_guests SET groupAllocation = :newGroup WHERE weddingProfileId = :profileId AND groupAllocation = :oldGroup")
    suspend fun renameGroup(profileId: String, oldGroup: String, newGroup: String)
}
