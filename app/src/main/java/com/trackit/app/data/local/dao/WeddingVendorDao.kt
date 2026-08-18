package com.trackit.app.data.local.dao

import androidx.room.*
import com.trackit.app.data.local.entity.WeddingVendorEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WeddingVendorDao {
    @Query("SELECT * FROM wedding_vendors WHERE weddingProfileId = :profileId ORDER BY category ASC, name ASC")
    fun getAllByProfile(profileId: String): Flow<List<WeddingVendorEntity>>

    @Query("SELECT * FROM wedding_vendors WHERE weddingProfileId = :profileId AND category = :category ORDER BY name ASC")
    fun getByCategory(profileId: String, category: String): Flow<List<WeddingVendorEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vendor: WeddingVendorEntity)

    @Update
    suspend fun update(vendor: WeddingVendorEntity)

    @Delete
    suspend fun delete(vendor: WeddingVendorEntity)
}
