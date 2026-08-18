package com.trackit.app.data.local.dao

import androidx.room.*
import com.trackit.app.data.local.entity.WeddingDocumentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WeddingDocumentDao {
    @Query("SELECT * FROM wedding_documents WHERE weddingProfileId = :profileId ORDER BY sortOrder ASC")
    fun getAllByProfile(profileId: String): Flow<List<WeddingDocumentEntity>>

    @Query("SELECT COUNT(*) FROM wedding_documents WHERE weddingProfileId = :profileId")
    fun getTotalCount(profileId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM wedding_documents WHERE weddingProfileId = :profileId AND isCompleted = 1")
    fun getCompletedCount(profileId: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(doc: WeddingDocumentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(docs: List<WeddingDocumentEntity>)

    @Update
    suspend fun update(doc: WeddingDocumentEntity)

    @Delete
    suspend fun delete(doc: WeddingDocumentEntity)
}
