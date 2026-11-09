package com.trackit.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.trackit.app.data.local.entity.NfcLocationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NfcLocationDao {

    @Query("SELECT * FROM nfc_locations WHERE locationId = :locationId LIMIT 1")
    suspend fun getLocationById(locationId: String): NfcLocationEntity?

    @Query("SELECT * FROM nfc_locations WHERE locationId = :locationId LIMIT 1")
    fun observeLocationById(locationId: String): Flow<NfcLocationEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entity: NfcLocationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<NfcLocationEntity>)
}
