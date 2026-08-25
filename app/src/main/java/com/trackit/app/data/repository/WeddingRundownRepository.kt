package com.trackit.app.data.repository

import com.trackit.app.data.local.dao.WeddingEventDao
import com.trackit.app.data.local.dao.WeddingRundownItemDao
import com.trackit.app.data.local.entity.WeddingEventEntity
import com.trackit.app.data.local.entity.WeddingRundownItemEntity
import com.trackit.app.util.SyncManager
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeddingEventRepository @Inject constructor(
    private val dao: WeddingEventDao,
    private val syncManager: SyncManager
) {
    fun getAllByProfile(profileId: String): Flow<List<WeddingEventEntity>> = dao.getAllByProfile(profileId)
    suspend fun insert(event: WeddingEventEntity) { dao.insert(event); syncManager.pushWeddingEvent(event) }
    suspend fun insertAll(events: List<WeddingEventEntity>) { dao.insertAll(events); events.forEach { syncManager.pushWeddingEvent(it) } }
    suspend fun update(event: WeddingEventEntity) { dao.update(event); syncManager.pushWeddingEvent(event) }
    suspend fun delete(event: WeddingEventEntity) { dao.delete(event); syncManager.deleteWeddingEvent(event) }
}

@Singleton
class WeddingRundownRepository @Inject constructor(
    private val dao: WeddingRundownItemDao,
    private val syncManager: SyncManager
) {
    fun getByEvent(eventId: String): Flow<List<WeddingRundownItemEntity>> = dao.getByEvent(eventId)
    suspend fun insert(item: WeddingRundownItemEntity) { dao.insert(item); syncManager.pushWeddingRundownItem(item) }
    suspend fun insertAll(items: List<WeddingRundownItemEntity>) { dao.insertAll(items); items.forEach { syncManager.pushWeddingRundownItem(it) } }
    suspend fun update(item: WeddingRundownItemEntity) { dao.update(item); syncManager.pushWeddingRundownItem(item) }
    suspend fun delete(item: WeddingRundownItemEntity) { dao.delete(item); syncManager.deleteWeddingRundownItem(item) }
    suspend fun deleteAllByEvent(eventId: String) = dao.deleteAllByEvent(eventId)
}
