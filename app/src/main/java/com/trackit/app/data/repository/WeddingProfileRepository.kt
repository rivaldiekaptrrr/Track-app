package com.trackit.app.data.repository

import com.trackit.app.data.local.dao.WeddingProfileDao
import com.trackit.app.data.local.entity.WeddingProfileEntity
import com.trackit.app.util.SyncManager
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeddingProfileRepository @Inject constructor(
    private val dao: WeddingProfileDao,
    private val syncManager: SyncManager
) {
    fun getById(id: String): Flow<WeddingProfileEntity?> = dao.getById(id)
    suspend fun insert(profile: WeddingProfileEntity): Long { val r = dao.insert(profile); syncManager.pushWeddingProfile(profile); return r }
    suspend fun update(profile: WeddingProfileEntity) { dao.update(profile); syncManager.pushWeddingProfile(profile) }
    suspend fun delete(profile: WeddingProfileEntity) { dao.delete(profile); syncManager.deleteWeddingProfile(profile) }
}
