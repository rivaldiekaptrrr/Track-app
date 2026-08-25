package com.trackit.app.data.repository

import com.trackit.app.data.local.dao.WeddingSeserahanDao
import com.trackit.app.data.local.entity.WeddingSeserahanEntity
import com.trackit.app.util.SyncManager
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeddingSeserahanRepository @Inject constructor(
    private val dao: WeddingSeserahanDao,
    private val syncManager: SyncManager
) {
    fun getAllByProfile(profileId: String): Flow<List<WeddingSeserahanEntity>> = dao.getAllByProfile(profileId)
    fun getByDirection(profileId: String, direction: String): Flow<List<WeddingSeserahanEntity>> = dao.getByDirection(profileId, direction)
    suspend fun insert(item: WeddingSeserahanEntity) { dao.insert(item); syncManager.pushWeddingSeserahan(item) }
    suspend fun insertAll(items: List<WeddingSeserahanEntity>) { dao.insertAll(items); items.forEach { syncManager.pushWeddingSeserahan(it) } }
    suspend fun update(item: WeddingSeserahanEntity) { dao.update(item); syncManager.pushWeddingSeserahan(item) }
    suspend fun delete(item: WeddingSeserahanEntity) { dao.delete(item); syncManager.deleteWeddingSeserahan(item) }
}
