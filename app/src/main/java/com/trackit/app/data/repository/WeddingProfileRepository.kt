package com.trackit.app.data.repository

import com.trackit.app.data.local.dao.WeddingProfileDao
import com.trackit.app.data.local.entity.WeddingProfileEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeddingProfileRepository @Inject constructor(
    private val dao: WeddingProfileDao
) {
    fun getById(id: String): Flow<WeddingProfileEntity?> = dao.getById(id)
    suspend fun insert(profile: WeddingProfileEntity): Long = dao.insert(profile)
    suspend fun update(profile: WeddingProfileEntity) = dao.update(profile)
    suspend fun delete(profile: WeddingProfileEntity) = dao.delete(profile)
}
